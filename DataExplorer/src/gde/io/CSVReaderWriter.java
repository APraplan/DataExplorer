/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.MissMatchDeviceException;
import gde.exception.NotSupportedFileFormatException;
import gde.exception.UnitCompareException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * Class to read and write comma separated value files
 * @author Winfried Brügmann
 */
public class CSVReaderWriter {
	static Logger					log			= Logger.getLogger(CSVReaderWriter.class.getName());

	static String					lineSep	= GDE.LINE_SEPARATOR;
	static DecimalFormat	df3			= new DecimalFormat("0.000"); //$NON-NLS-1$
	static StringBuffer		sb;

	final static DataExplorer						application	= DataExplorer.getInstance();
	final static Channels								channels		= Channels.getInstance();

	/**
	 * read the first two line of CSV file and prepare a map with all available information
	 * @param separator
	 * @param filePath
	 * @return map with GDE.DEVICE_NAME,GDE.CSV_DATA_HEADER,[GDE.CHANNEL_CONFIG_NAME]
	 * @throws NotSupportedFileFormatException
	 * @throws IOException
	 */
	public static HashMap<String, String> getHeader(char separator, String filePath) throws NotSupportedFileFormatException, IOException {
		String					line		= GDE.STRING_STAR;
		BufferedReader reader = null; // to read the data
		HashMap<String, String> header = new HashMap<String, String>();

		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
			line = reader.readLine();
			String[] headerData = line.split(GDE.STRING_EMPTY + separator);
			if (headerData.length > 2) {
  			NotSupportedFileFormatException e = new NotSupportedFileFormatException("File does have required header information \"device_name[separator<channel/config_name>]\"");
  			log.log(Level.SEVERE, e.getMessage(), e);
  			throw e;
			}
			for (int i = 0; i < headerData.length; i++) {
				if (i == 0) header.put(GDE.DEVICE_NAME, headerData[i].split("\\r")[0].trim());
				if (i == 1) header.put(GDE.CHANNEL_CONFIG_NAME, headerData[i].split(" ")[0].split("\\r")[0].trim());
			}
			while (((line = reader.readLine()) != null) && !(line.contains("[") && line.contains("]"))) {
				// read until Zeit [sec];Spannung [---];Höhe [---]
				// 						Zeit [s];Spannung [V];Strom [A];Ladung [mAh];Leistung [W];Energie [Wh]
			}
    	header.put(GDE.CSV_DATA_HEADER, line);

  		if (header.size() >= 1) {
  			log.log(Level.FINE, GDE.DEVICE_NAME + header.get(GDE.DEVICE_NAME));
 				log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + (header.get(GDE.CHANNEL_CONFIG_NAME) != null ? header.get(GDE.CHANNEL_CONFIG_NAME) : "" )); //$NON-NLS-1$
 				log.log(Level.FINE, GDE.CSV_DATA_HEADER + (header.get(GDE.CSV_DATA_HEADER) != null ? header.get(GDE.CSV_DATA_HEADER) : "")); //$NON-NLS-1$
  		}

  		else {
  			NotSupportedFileFormatException e = new NotSupportedFileFormatException("File does have required header information \"device_name[separator<channel/config_name>]\"");
  			log.log(Level.SEVERE, e.getMessage(), e);
  			throw e;
  		}
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGW0011, new Object[] {filePath}));
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.GDE_MSGW0012, new Object[] {filePath}));
		}
		finally {
			if (reader != null) 		reader.close();
		}

		return header;
	}

	/**
	 * evaluate channel/configuartion and check units for absolute data
	 * <ul>
	 * <li>if the channel/configuration does not match device the first channel/configuration of the device will choosen
	 * <li>if units of absolute data will not match a warning dialog will show all red measurements kesy with expected units
	 * </ul>
	 * @param header
	 */
	public static HashMap<String, String> evaluateType(char separator, HashMap<String, String> header, DeviceConfiguration deviceConfig) {
		int index = 0, innerIndex = 0;
		int countRaw = 0, countAbs = 0;
		StringBuilder sb_measurements = new StringBuilder();
		StringBuilder sb_units = new StringBuilder();
		String subString;
		boolean isRaw = false;

		String headerLine = header.get(GDE.CSV_DATA_HEADER);
		while ((index = headerLine.indexOf('[', index+1)) != -1) {
			if ((innerIndex = headerLine.indexOf(']', index+1)) != -1) {
				subString = headerLine.substring(index+1, innerIndex);
				if (subString.split("-").length == 0) {
					++countRaw;
				}
				else																			 {
					++countAbs;
				}
			}
			++index;
		}

		// Spannung;Strom;Ladung; < Spannung;Strom;Ladung;Leistung;Energie;
		String[] headerLineArray = headerLine.split(GDE.STRING_EMPTY+separator);
		if(countAbs <= 1 && countRaw > 0 || (headerLineArray.length - 1)  < deviceConfig.getNumberOfMeasurements(1)) {
			header.put(GDE.CSV_DATA_TYPE, GDE.CSV_DATA_TYPE_RAW);
			isRaw = true;
		}
		else {
			header.put(GDE.CSV_DATA_TYPE, GDE.CSV_DATA_TYPE_ABS);
			isRaw = false;
		}
		log.log(Level.FINE, GDE.CSV_DATA_TYPE + header.get(GDE.CSV_DATA_TYPE));

		String channelConfig = header.get(GDE.CHANNEL_CONFIG_NAME);
		int channelNumber = channels.getChannelNumber(channelConfig);
		if (channelConfig != null
				&& !channels.getActiveChannel().getChannelConfigKey().equals(channelConfig)
				&& channelNumber >= 1 && channelNumber <= deviceConfig.getChannelCount() ) {
			channels.setActiveChannelNumber(channelNumber);
		}
		else { // unknown channel configuration using active one
			channelConfig = channels.getActiveChannel().getChannelConfigKey();
			channels.setActiveChannelNumber(channels.getActiveChannelNumber());
			channelNumber = channels.getActiveChannelNumber();
		}
		header.put(GDE.CHANNEL_CONFIG_NAME, channelConfig);
		header.put(GDE.CHANNEL_CONFIG_NUMBER, ""+channels.getActiveChannelNumber());
		log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + header.get(GDE.CHANNEL_CONFIG_NUMBER) + " : " + header.get(GDE.CHANNEL_CONFIG_NAME));

		int match = 0; // check match of the measurement units, relevant for absolute csv data only
		StringBuilder unitCompare = new StringBuilder().append(lineSep);
		for (int i = 1; i < headerLineArray.length; i++) {
			String expectUnit = deviceConfig.getMeasurementUnit(channelNumber, (i - 1));
			String[] inHeaderMeasurement = headerLineArray[i].trim().split("\\[|]"); //$NON-NLS-1$
			String inMeasurement = inHeaderMeasurement.length >= 1 ? inHeaderMeasurement[0].trim() : Settings.EMPTY;
			String inUnit = inHeaderMeasurement.length == 2 ? inHeaderMeasurement[1].trim() : Settings.EMPTY;
			sb_measurements.append(inMeasurement).append(GDE.STRING_SEMICOLON);
			sb_units.append(inUnit).append(GDE.STRING_SEMICOLON);
			unitCompare.append(String.format("%-30s \t%s \n", inMeasurement, Messages.getString(MessageIds.GDE_MSGT0136, new Object[]{inUnit, expectUnit})));
			if (inUnit.equals(expectUnit) || inUnit.equals(Settings.EMPTY)) ++match;
		}
		header.put(GDE.CSV_DATA_HEADER_MEASUREMENTS, sb_measurements.toString());
		header.put(GDE.CSV_DATA_HEADER_UNITS, sb_units.toString());

		if (!isRaw) {
			log.log(Level.FINE, unitCompare.toString());
			if (match != headerLineArray.length - 1) {
				UnitCompareException e = new UnitCompareException(Messages.getString(MessageIds.GDE_MSGW0015, new Object[] {unitCompare.toString()})); // mismatch data header units
				log.log(Level.WARNING, e.getMessage());
				application.openMessageDialogAsync(e.getMessage());
			}
		}

		return header;
	}

	/**
	 * read the selected CSV file
	 * @return record set created
	 * @throws NotSupportedFileFormatException
	 * @throws MissMatchDeviceException
	 * @throws IOException
	 * @throws DataInconsitsentException
	 * @throws DataTypeException
	 */
	public static RecordSet read(char separator, String filePath, String recordSetNameExtend, boolean isRaw) throws NotSupportedFileFormatException, MissMatchDeviceException, IOException, DataInconsitsentException, DataTypeException {
		String line = GDE.STRING_STAR;
		RecordSet recordSet = null;
		BufferedReader reader; // to read the data
		IDevice device = application.getActiveDevice();
		Channel activeChannel = null;

		try {
			HashMap<String, String> fileHeader = CSVReaderWriter.getHeader(separator, filePath);
			activeChannel = channels.get(channels.getChannelNumber(fileHeader.get(GDE.CHANNEL_CONFIG_NAME)));
			activeChannel = activeChannel == null ? channels.getActiveChannel() : activeChannel;

			if (activeChannel != null) {
				GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0134) + filePath);
				long time_ms = 0;

				// check for device name and channel or configuration in first line
				if (!application.getActiveDevice().getName().equals(fileHeader.get(GDE.DEVICE_NAME))) {
					MissMatchDeviceException e = new MissMatchDeviceException(Messages.getString(MessageIds.GDE_MSGW0013, new Object[] {fileHeader.get(GDE.DEVICE_NAME)})); // mismatch device name
					log.log(Level.SEVERE, e.getMessage(), e);
					throw e;
				}

				fileHeader = CSVReaderWriter.evaluateType(separator, fileHeader, (DeviceConfiguration)device);
				if (isRaw != fileHeader.get(GDE.CSV_DATA_TYPE).equals(GDE.CSV_DATA_TYPE_RAW)) {
					throw new DataTypeException(Messages.getString(MessageIds.GDE_MSGW0014));
				}
				log.log(Level.FINE, "device name check ok, channel/configuration ok"); //$NON-NLS-1$

				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
				while (!(((line = reader.readLine())!= null) && line.contains("[") && line.contains("]"))) {
					// read until Zeit [sec];Spannung [---];Höhe [---]
					// 						Zeit [s];Spannung [V];Strom [A];Ladung [mAh];Leistung [W];Energie [Wh]
				}

				if (GDE.isWithUi()) {
					channels.switchChannel(activeChannel.getNumber(), GDE.STRING_EMPTY);
					application.getMenuToolBar().updateChannelSelector();
					activeChannel = channels.getActiveChannel();
				}

				String recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$
				String[] tmpRecordNames	=	fileHeader.get(GDE.CSV_DATA_HEADER_MEASUREMENTS).split(GDE.STRING_SEMICOLON);
				String[] tmpRecordUnits = fileHeader.get(GDE.CSV_DATA_HEADER_UNITS).split(GDE.STRING_SEMICOLON);
				String[] recordNames = isRaw ? device.getMeasurementNamesReplacements(activeChannel.getNumber()) : new String[tmpRecordNames.length];
				String[] recordSymbols = new String[recordNames.length];
				String[] recordUnits = new String[recordNames.length];
				for (int i=0, j=0; isRaw ? i < recordNames.length : i < tmpRecordNames.length; i++) {
					MeasurementType measurement = device.getMeasurement(activeChannel.getNumber(), i);
					if (isRaw) {
						if (!measurement.isCalculation()) {
							recordNames[i] = tmpRecordNames[j];
							recordSymbols[i] = measurement.getSymbol();
							recordUnits[i] = measurement.getUnit();
							++j;
						}
						else {
							recordSymbols[i] = measurement.getSymbol();
							recordUnits[i] = measurement.getUnit();
						}
					}
					else {
							recordNames[i] = tmpRecordNames[i];
							recordSymbols[i] = measurement.getSymbol();
							recordUnits[i] = tmpRecordUnits[i];
					}
				}
				recordSet = RecordSet.createRecordSet(recordSetName, device, activeChannel.getNumber(), recordNames, recordSymbols, recordUnits, device.getTimeStep_ms(), isRaw, true, true);
				recordSetName = recordSet.getName(); // cut length

				// make all records displayable while absolute data
				if (!isRaw) { // absolute
					for (String recordKey : recordNames) {
						recordSet.get(recordKey).setDisplayable(true); // all data available
					}
				}
				Date												date = null;
				int													year=0, month=0, day=0;
				long startTimeStamp=0, lastTimeStamp=0;

				// now get all data   0; 14,780;  0,598;  1,000;  8,838;  0,002
				String[] updateRecordNames = isRaw ? recordSet.getNoneCalculationRecordNames() : recordNames;
				int[] points = new int[updateRecordNames.length];
				while ((line = reader.readLine()) != null) {
					String[] dataStr = line.split(GDE.STRING_EMPTY + separator);
					String data = dataStr[0].trim().replace(',', '.');
					if (data.contains(GDE.STRING_BLANK)) { //absolute time YYYY-MM-DD HH:mm:ss:SSS
						if (date == null) {
							year = Integer.parseInt(data.substring(0,4));
							month = Integer.parseInt(data.substring(5, 7));
							day = Integer.parseInt(data.substring(8, 10));
						}
						data = data.split(GDE.STRING_BLANK)[1];
						int hour = Integer.parseInt(data.substring(0, 2));
						int minute = Integer.parseInt(data.substring(3, 5));
						int second = Integer.parseInt(data.substring(6, 8));
						GregorianCalendar calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
						long timeStamp = calendar.getTimeInMillis() + (data.contains(GDE.STRING_DOT) ? Integer.parseInt(data.substring(data.lastIndexOf(GDE.CHAR_DOT) + 1)) : 0);

						if (lastTimeStamp < timeStamp) {
							time_ms = lastTimeStamp == 0 ? 0 : time_ms + (timeStamp - lastTimeStamp);
							lastTimeStamp = timeStamp;
							date = calendar.getTime();
							if (startTimeStamp == 0) startTimeStamp = timeStamp;
						}
						else
							continue;
					}
					else { // relative time HH:mm:ss:SSS
						if (startTimeStamp == 0) startTimeStamp = new Date().getTime();
						if (data.length() == 9) { //00:00.000
							int minute = Integer.parseInt(data.substring(0, 2));
							int second = Integer.parseInt(data.substring(3, 5));
							time_ms = minute*60*1000 + second*1000 + Integer.parseInt(data.substring(data.lastIndexOf(GDE.CHAR_DOT) + 1));
						}
						else if (data.length() == 12) { //00:00:00.000)
							int hour = Integer.parseInt(data.substring(0, 2));
							int minute = Integer.parseInt(data.substring(3, 5));
							int second = Integer.parseInt(data.substring(6, 8));
							time_ms = hour*60*60*1000 + minute*60*1000 + second*1000 + Integer.parseInt(data.substring(data.lastIndexOf(GDE.CHAR_DOT) + 1));
						}
					}
					for (int i = 0; i < updateRecordNames.length; i++) { // only iterate over record names found in file
						data = dataStr[i + 1].trim().replace(',', '.').replace(GDE.STRING_BLANK, GDE.STRING_EMPTY);
						points[i] = (int) (Double.valueOf(data).doubleValue() * 1000);
					}
					if (isRaw) 	recordSet.addNoneCalculationRecordsPoints(points, time_ms);
					else 				recordSet.addPoints(points, time_ms);
				}

				recordSet.setSaved(true);

				activeChannel.put(recordSetName, recordSet);
				activeChannel.setActiveRecordSet(recordSetName);
				if (isRaw)
					activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
				activeChannel.applyTemplate(recordSetName, true);
				if (GDE.isWithUi()) {
					activeChannel.switchRecordSet(recordSetName);
					application.updateAllTabs(true, true);
				}

				reader.close();
				reader = null;
			}
		}
		catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new UnsupportedEncodingException(Messages.getString(MessageIds.GDE_MSGW0010));
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGW0011, new Object[] {filePath}));
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.GDE_MSGW0012, new Object[] {filePath}));
		}
		finally {
			GDE.getUiNotification().setProgress(100);
			GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
			if (GDE.isWithUi()) {
				application.getMenuToolBar().updateChannelSelector();
				application.getMenuToolBar().updateRecordSetSelectCombo();
			}
		}

		return recordSet;
	}

	/**
	 * write data CVS file
	 * @throws Exception
	 */
	public static void write(char separator, String recordSetKey, String filePath, boolean isRaw, final String encoding) throws Exception {
		BufferedWriter writer;

		try {
			GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138, new String[] {GDE.FILE_ENDING_CSV, filePath}));
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), encoding));
			char decimalSeparator = Settings.getInstance().getDecimalSeparator();
			boolean isTimeFormatAbsolute = Settings.getInstance().isTimeFormatAbsolute();

			df3.setGroupingUsed(false);
			sb = new StringBuffer();
			RecordSet recordSet = Channels.getInstance().getActiveChannel().get(recordSetKey);
			IDevice device = DataExplorer.getInstance().getActiveDevice();
			// write device name , manufacturer, and serial port string
			sb.append(device.getName()).append(separator).append(recordSet.getChannelConfigName()).append(lineSep);
			writer.write(sb.toString());
			log.log(Level.FINE, "written header line = " + sb.toString());  //$NON-NLS-1$

			sb = new StringBuffer();
			sb.append(Messages.getString(isTimeFormatAbsolute ? MessageIds.GDE_MSGT0436 : MessageIds.GDE_MSGT0137)).append(separator); // Spannung [V];Strom [A];Ladung [Ah];Leistung [W];Energie [Wh]";
			// write the measurements signature
			int i = 0;
			if (isRaw) {
				String[] recordNames = recordSet.getRecordNames();
				for (int j = 0; j < recordNames.length; j++) {
					final Record record = recordSet.get(recordNames[j]);
					MeasurementType  measurement = device.getMeasurement(recordSet.getChannelConfigNumber(), record.getOrdinal());
					log.log(Level.FINEST, "append " + record.getName()); //$NON-NLS-1$
					if (!measurement.isCalculation()) {	// only use active records for writing raw data
						sb.append(record.getName()).append(" [---]").append(separator);	 //$NON-NLS-1$
						log.log(Level.FINEST, "append " + record.getName()); //$NON-NLS-1$
					}
				}
				++i;
			}
			else {
				for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
					log.log(Level.FINER, "append " + record.getName()); //$NON-NLS-1$
					sb.append(record.getName()).append(" [").append(record.getUnit()).append(']').append(separator);	 //$NON-NLS-1$
					++i;
				}
			}
			sb.deleteCharAt(sb.length() - 1).append(lineSep);
			log.log(Level.FINER, "header line = " + sb.toString()); //$NON-NLS-1$
			writer.write(sb.toString());

			// write data
			long startTime;
			try {
				startTime = new Date(recordSet.getTime(0)).getTime();
			}
			catch (Exception e) {
				startTime = new Date().getTime();
			}
			int recordEntries = recordSet.getRecordDataSize(false);
			int progressCycle = 0;
			GDE.getUiNotification().setProgress(progressCycle);

			for (i = 0; i < recordEntries; i++) {
				sb = new StringBuffer();
				if (isRaw) { // do not change any values
					String[] row = recordSet.getRawExportRow(i, isTimeFormatAbsolute);
					row[0].replace(',', decimalSeparator);
					for (String value : row) {
						if (value != null)
							sb.append(value.trim());		
						sb.append(separator);
					}
				}
				else {
					String[] row = recordSet.getDataTableRow(i, isTimeFormatAbsolute);
					char currentDecimalSeparator = Character.valueOf(recordSet.get(0).getDecimalFormat().getDecimalFormatSymbols().getDecimalSeparator());
					char currentrGoupingSeparator = Character.valueOf(recordSet.get(0).getDecimalFormat().getDecimalFormatSymbols().getGroupingSeparator());
	
					// add time entry
					sb.append(row[0].replace(',', decimalSeparator).trim()).append(separator);
					// add data entries
					int j = 0;
					for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
						log.log(Level.FINEST, "append " + record.getName()); //$NON-NLS-1$
						// translate according device and measurement unit
						sb.append(row[j + 1].replace(currentrGoupingSeparator, GDE.CHAR_BLANK).replace(currentDecimalSeparator, decimalSeparator)).append(separator);
						++j;
					}
				}
				sb.deleteCharAt(sb.length() - 1).append(lineSep);
				writer.write(sb.toString());
				if (i % 50 == 0) GDE.getUiNotification().setProgress(((++progressCycle*5000)/recordEntries));
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "data line = " + sb.toString()); //$NON-NLS-1$
			}
			sb = null;
			log.log(Level.TIME, "CSV file = " + filePath + " erfolgreich geschieben"  //$NON-NLS-1$ //$NON-NLS-2$
					+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$

			writer.flush();
			writer.close();
			writer = null;
			//recordSet.setSaved(true);
			GDE.getUiNotification().setProgress(100);
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0006, new Object[]{ GDE.FILE_ENDING_CSV, filePath, e.getMessage() }));
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
		finally {
			GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
		}

	}

}
