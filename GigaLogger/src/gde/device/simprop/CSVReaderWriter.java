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

    Copyright (c) 2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.simprop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.device.MeasurementType;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.MissMatchDeviceException;
import gde.exception.NotSupportedFileFormatException;
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
	static Logger							log					= Logger.getLogger(CSVReaderWriter.class.getName());

	static String							lineSep			= GDE.LINE_SEPARATOR;
	static DecimalFormat			df3					= new DecimalFormat("0.000");												//$NON-NLS-1$

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();

	/**
	 * read the first two line of CSV file and prepare a map with all available information
	 * @param separator
	 * @param filePath
	 * @return map with GDE.DEVICE_NAME,GDE.CSV_DATA_HEADER,[GDE.CHANNEL_CONFIG_NAME]
	 * @throws NotSupportedFileFormatException
	 * @throws IOException
	 */
	public static HashMap<String, String> getHeader(char separator, String filePath) throws NotSupportedFileFormatException, IOException {
		String line = GDE.STRING_STAR;
		BufferedReader reader = null; // to read the data
		HashMap<String, String> header = new HashMap<String, String>();
		IDevice device = CSVReaderWriter.application.getActiveDevice();

		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
			header.put(GDE.DEVICE_NAME, CSVReaderWriter.application.getActiveDevice().getName().trim());
			header.put(GDE.CHANNEL_CONFIG_NAME, CSVReaderWriter.channels.getActiveChannel().getChannelConfigKey().trim());

			line = reader.readLine();
			String[] headerDataMeasurements = line.split(GDE.STRING_EMPTY + separator);

			StringBuilder sb = new StringBuilder();
			String[] measurements = device.getMeasurementNamesReplacements(application.getActiveChannelNumber());
			for (int i = 1, j = 0; i < headerDataMeasurements.length; i++) {
				if (headerDataMeasurements[i].trim().length() > 3) {
					if (!headerDataMeasurements[i].trim().equals(measurements[j]) && headerDataMeasurements[i].startsWith("MSB")) {
						sb.append(measurements[j]).append(separator);
						sb.append(measurements[j] + "_alarm").append(separator);
					}
					else {
						sb.append(headerDataMeasurements[i].trim()).append(separator);
						sb.append(headerDataMeasurements[i].trim() + "_alarm").append(separator);
					}
					j+=2;
				}
			}
			header.put(GDE.CSV_DATA_HEADER_MEASUREMENTS, sb.toString());

			line = reader.readLine();
			String[] headerDataUnits = line.split(GDE.STRING_EMPTY + separator);

			sb = new StringBuilder();
			for (int i = 2; i < headerDataUnits.length; i+=3) {
				sb.append(headerDataUnits[i].trim().length() >= 1 ? headerDataUnits[i].trim() : GDE.STRING_BLANK).append(separator);
				sb.append(headerDataUnits[i].trim().length() >= 1 ? headerDataUnits[i].trim() : GDE.STRING_BLANK).append(separator);
			}
			header.put(GDE.CSV_DATA_HEADER_UNITS, sb.toString());

			CSVReaderWriter.log.log(java.util.logging.Level.FINE, GDE.DEVICE_NAME + header.get(GDE.DEVICE_NAME));
			CSVReaderWriter.log.log(java.util.logging.Level.FINE, GDE.CHANNEL_CONFIG_NAME + (header.get(GDE.CHANNEL_CONFIG_NAME) != null ? header.get(GDE.CHANNEL_CONFIG_NAME) : "")); //$NON-NLS-1$
			CSVReaderWriter.log.log(java.util.logging.Level.FINE, GDE.CSV_DATA_HEADER_MEASUREMENTS + header.get(GDE.CSV_DATA_HEADER_MEASUREMENTS));
			CSVReaderWriter.log.log(java.util.logging.Level.FINE, GDE.CSV_DATA_HEADER_UNITS + header.get(GDE.CSV_DATA_HEADER_UNITS));
		}
		catch (FileNotFoundException e) {
			CSVReaderWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new FileNotFoundException(Messages.getString(gde.messages.MessageIds.GDE_MSGW0011, new Object[] { filePath }));
		}
		catch (IOException e) {
			CSVReaderWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(gde.messages.MessageIds.GDE_MSGW0012, new Object[] { filePath }));
		}
		finally {
			if (reader != null) reader.close();
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
	public static RecordSet read(char separator, String filePath, String recordSetNameExtend) throws NotSupportedFileFormatException, MissMatchDeviceException, IOException, DataInconsitsentException,
			DataTypeException {
		String line = GDE.STRING_STAR;
		RecordSet recordSet = null;
		int progressLineLength = Math.abs(CSVReaderWriter.application.getActiveDevice().getDataBlockSize(InputTypes.FILE_IO));
		long inputFileSize = new File(filePath).length();
		BufferedReader reader; // to read the data
		IDevice device = CSVReaderWriter.application.getActiveDevice();
		Channel activeChannel = null;
		boolean isParsingError = false, isParsingErrorLine = false;

		try {
			HashMap<String, String> fileHeader = CSVReaderWriter.getHeader(separator, filePath);
			activeChannel = CSVReaderWriter.channels.get(CSVReaderWriter.channels.getChannelNumber(fileHeader.get(GDE.CHANNEL_CONFIG_NAME)));
			activeChannel = activeChannel == null ? CSVReaderWriter.channels.getActiveChannel() : activeChannel;

			if (activeChannel != null) {
				GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0134) + filePath);
				GDE.getUiNotification().setProgress(0);
				int time_ms = 0;

				// check for device name and channel or configuration in first line
				if (!CSVReaderWriter.application.getActiveDevice().getName().equals(fileHeader.get(GDE.DEVICE_NAME))) {
					MissMatchDeviceException e = new MissMatchDeviceException(Messages.getString(MessageIds.GDE_MSGW0013, new Object[] { fileHeader.get(GDE.DEVICE_NAME) })); // mismatch device name
					CSVReaderWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					throw e;
				}

				CSVReaderWriter.log.log(java.util.logging.Level.FINE, "device name check ok, channel/configuration ok"); //$NON-NLS-1$

				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
				reader.readLine();					// Std:Min:Sek;MSB A00;    ; ;MSB A01;    ; ;MSB A02;

				if (GDE.isWithUi()) {
					CSVReaderWriter.channels.switchChannel(activeChannel.getNumber(), GDE.STRING_EMPTY);
					CSVReaderWriter.application.getMenuToolBar().updateChannelSelector();
					activeChannel = CSVReaderWriter.channels.getActiveChannel();
				}

				String recordSetName = (activeChannel.size() + 1) + recordSetNameExtend;
				String[] tmpRecordNames = fileHeader.get(GDE.CSV_DATA_HEADER_MEASUREMENTS).split(GDE.STRING_SEMICOLON);
				String[] tmpRecordUnits = fileHeader.get(GDE.CSV_DATA_HEADER_UNITS).split(GDE.STRING_SEMICOLON);
				String[] recordNames = new String[tmpRecordNames.length];
				String[] recordSymbols = new String[recordNames.length];
				String[] recordUnits = new String[recordNames.length];
				for (int i = 0; i < tmpRecordNames.length; i++) {
					MeasurementType measurement = device.getMeasurement(activeChannel.getNumber(), i);
					recordNames[i] = tmpRecordNames[i];
					recordSymbols[i] = measurement.getSymbol();
					recordUnits[i] = tmpRecordUnits[i];
				}
				recordSet = RecordSet.createRecordSet(recordSetName, device, activeChannel.getNumber(), recordNames, recordSymbols, recordUnits, device.getTimeStep_ms(), true, true, true);
				recordSetName = recordSet.getName(); // cut length

				//find GPS related records and try to assign data type
				for (int i = 0; i < recordSet.size(); i++) {
					Record record = recordSet.get(i);
					if (record.getName().startsWith("GPS")) {
						if (record.getName().contains("Lon")) {
							record.setDataType(Record.DataType.GPS_LONGITUDE);
							record.setUnit("°");
						}
						if (record.getName().contains("Lat")) {
							record.setDataType(Record.DataType.GPS_LATITUDE);
							record.setUnit("°");
						}
						if (record.getName().startsWith("GPS H") || record.getName().startsWith("GPS Alt")) record.setDataType(Record.DataType.GPS_ALTITUDE);
						if (record.getName().startsWith("GPS Ges") || record.getName().startsWith("GPS Spe") || record.getName().startsWith("GPS Vit")) record.setDataType(Record.DataType.GPS_SPEED);
					}
				}

				//correct data and start time
				long startTimeStamp = (long) (new File(filePath).lastModified() - recordSet.getMaxTime_ms());
				recordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129)
						+ new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp));
				recordSet.setStartTimeStamp(startTimeStamp);
				activeChannel.setFileDescription((new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp)).substring(0, 10) + activeChannel.getFileDescription().substring(10));

				// make all records displayable while absolute data
				for (String recordKey : recordNames) {
					recordSet.get(recordKey).setDisplayable(true); // all data available
				}
				Calendar date = new GregorianCalendar();
				date.setTime(new Date(recordSet.getStartTimeStamp()));
				int year = date.get(Calendar.YEAR), month = date.get(Calendar.MONTH), day = date.get(Calendar.DAY_OF_MONTH);
				long lastTimeStamp = 0;

				// now get all data   0; 14,780;  0,598;  1,000;  8,838;  0,002
				String[] updateRecordNames = recordNames;
				int[] points = new int[updateRecordNames.length];
				int lineNumber = 1;
				while ((line = reader.readLine()) != null) {
					++lineNumber;
					isParsingErrorLine = false;
					String[] dataStr = line.split(GDE.STRING_EMPTY + separator);
					String data = dataStr[0].trim().replace(',', '.');
					String data_alarm = dataStr[0].trim().replace(',', '.');
					if (data.contains(GDE.STRING_COLON)) {
						int hour = Integer.parseInt(data.substring(0, 3));
						int minute = Integer.parseInt(data.substring(4, 6));
						int second = Integer.parseInt(data.substring(7, 9));
						GregorianCalendar calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
						long timeStamp = calendar.getTimeInMillis() + (data.contains(GDE.STRING_DOT) ? Integer.parseInt(data.substring(data.lastIndexOf(GDE.CHAR_DOT) + 1)) : 0);

						if (lastTimeStamp < timeStamp) {
							time_ms = (int) (lastTimeStamp == 0 ? 0 : time_ms + (timeStamp - lastTimeStamp));
							lastTimeStamp = timeStamp;
							if (startTimeStamp == 0) startTimeStamp = timeStamp;
						}
						else
							continue;
					}
					else
						// decimal time value
						time_ms = Double.valueOf(Double.valueOf(data).doubleValue() * 1000).intValue();

					for (int i=0, j=1; i < updateRecordNames.length; i+=2,j+=3) { // only iterate over record names found in file
						try {
							//System.out.println(dataStr[j]);
							data = dataStr[j].trim().replace(',', '.').replace(GDE.STRING_BLANK, GDE.STRING_EMPTY);
							data_alarm = dataStr[j+2].trim().replace(',', '.').replace(GDE.STRING_BLANK, GDE.STRING_EMPTY);
							//System.out.println(dataStr[j+2]);
						}
						catch (Exception e) {
							data = "0";
							data_alarm = "-";
							CSVReaderWriter.log.log(java.util.logging.Level.WARNING, String.format("Check line = %s", line));
						}
						switch (recordSet.get(i).getDataType()) {
						case GPS_LONGITUDE:
						case GPS_LATITUDE:
							try {
								if (data.length() > 0) {
									points[i] = Double.valueOf(Double.valueOf(data) * 1000000.0).intValue();
								}
							}
							catch (NumberFormatException e) {
								if (!isParsingErrorLine) CSVReaderWriter.log.log(java.util.logging.Level.WARNING, Messages.getString(gde.device.simprop.MessageIds.GDE_MSGW3500, new Object[] { lineNumber, line }));
								GDE.getUiNotification().setStatusMessage(Messages.getString(gde.device.simprop.MessageIds.GDE_MSGW3500, new Object[] { lineNumber, line }), SWT.COLOR_RED);
								isParsingError = isParsingErrorLine = true;
							}
							break;

						default:
							try {
								if (data.length() > 0) {
									points[i] = Double.valueOf(Double.valueOf(data) * 1000.0).intValue();
								}
								if (data_alarm.length() > 0 && !data_alarm.equals("-")) {
									points[i+1] = Double.valueOf(Double.valueOf(data) * 1000.0).intValue();
								}
							}
							catch (NumberFormatException e) {
								if (!isParsingErrorLine) CSVReaderWriter.log.log(java.util.logging.Level.WARNING, Messages.getString(gde.device.simprop.MessageIds.GDE_MSGW3500, new Object[] { lineNumber, line }));
								GDE.getUiNotification().setStatusMessage(Messages.getString(gde.device.simprop.MessageIds.GDE_MSGW3500, new Object[] { lineNumber, line }), SWT.COLOR_RED);
								isParsingError = isParsingErrorLine = true;
							}
							break;
						}
					}
					recordSet.addPoints(points, time_ms);

					progressLineLength = progressLineLength > line.length() ? progressLineLength : line.length();
					int progress = (int) (lineNumber*100/(inputFileSize/progressLineLength));
					if (progress <= 90 && progress > GDE.getUiNotification().getProgressPercentage() && progress % 10 == 0) 	{
						GDE.getUiNotification().setProgress(progress);
						try {
							Thread.sleep(2);
						}
						catch (Exception e) {
							// ignore
						}
					}
				}

				recordSet.setSaved(true);

				activeChannel.put(recordSetName, recordSet);
				activeChannel.setActiveRecordSet(recordSetName);
				activeChannel.applyTemplate(recordSetName, true);
				recordSet.updateVisibleAndDisplayableRecordsForTable();
				if (GDE.isWithUi()) activeChannel.switchRecordSet(recordSetName);
				//activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records

				reader.close();
				reader = null;
			}
		}
		catch (UnsupportedEncodingException e) {
			CSVReaderWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new UnsupportedEncodingException(Messages.getString(MessageIds.GDE_MSGW0010));
		}
		catch (FileNotFoundException e) {
			CSVReaderWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGW0011, new Object[] { filePath }));
		}
		catch (IOException e) {
			CSVReaderWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.GDE_MSGW0012, new Object[] { filePath }));
		}
		finally {
			GDE.getUiNotification().setProgress(100);
			GDE.getUiNotification().setStatusMessage(isParsingError ? Messages.getString(gde.device.simprop.MessageIds.GDE_MSGW3501) : GDE.STRING_EMPTY, SWT.COLOR_RED);
			if (GDE.isWithUi()) {
				CSVReaderWriter.application.getMenuToolBar().updateChannelSelector();
				CSVReaderWriter.application.getMenuToolBar().updateRecordSetSelectCombo();
			}
		}

		return recordSet;
	}

	/**
	 * write data CVS file
	 * @throws Exception
	 */
	public static void write(char separator, String recordSetKey, String filePath, boolean isRaw) throws Exception {
		BufferedWriter writer;

		try {
			GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138, new String[] { GDE.FILE_ENDING_CSV, filePath }));
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
			char decimalSeparator = Settings.getInstance().getDecimalSeparator();

			CSVReaderWriter.df3.setGroupingUsed(false);
			StringBuffer sb = new StringBuffer();
			RecordSet recordSet = Channels.getInstance().getActiveChannel().get(recordSetKey);
			IDevice device = DataExplorer.getInstance().getActiveDevice();
			// write device name , manufacturer, and serial port string
			sb.append(device.getName()).append(separator).append(recordSet.getChannelConfigName()).append(CSVReaderWriter.lineSep);
			writer.write(sb.toString());
			CSVReaderWriter.log.log(java.util.logging.Level.FINE, "written header line = " + sb.toString()); //$NON-NLS-1$

			sb = new StringBuffer();
			sb.append(Messages.getString(MessageIds.GDE_MSGT0137)).append(separator); // Spannung [V];Strom [A];Ladung [Ah];Leistung [W];Energie [Wh]";
			// write the measurements signature
			for (int i = 0; i < recordSet.size(); i++) {
				MeasurementType measurement = device.getMeasurement(recordSet.getChannelConfigNumber(), i);
				Record record = recordSet.get(i);
				CSVReaderWriter.log.log(java.util.logging.Level.FINEST, "append " + record.getName()); //$NON-NLS-1$
				if (isRaw) {
					if (!measurement.isCalculation()) { // only use active records for writing raw data
						sb.append(record.getName()).append(" [---]").append(separator); //$NON-NLS-1$
						CSVReaderWriter.log.log(java.util.logging.Level.FINEST, "append " + record.getName()); //$NON-NLS-1$
					}
				}
				else {
					sb.append(record.getName()).append(" [").append(record.getUnit()).append(']').append(separator); //$NON-NLS-1$
					CSVReaderWriter.log.log(java.util.logging.Level.FINEST, "append " + record.getName()); //$NON-NLS-1$
				}
			}
			sb.deleteCharAt(sb.length() - 1).append(CSVReaderWriter.lineSep);
			CSVReaderWriter.log.log(java.util.logging.Level.FINER, "header line = " + sb.toString()); //$NON-NLS-1$
			writer.write(sb.toString());

			// write data
			long startTime = new Date(recordSet.getTime(0)).getTime();
			int recordEntries = recordSet.getRecordDataSize(true);
			int progressCycle = 0;
			GDE.getUiNotification().setProgress(progressCycle);
			for (int i = 0; i < recordEntries; i++) {
				sb = new StringBuffer();
				String[] row = recordSet.getExportRow(i, true);

				// add time entry
				sb.append(row[0].replace('.', decimalSeparator)).append(separator).append(GDE.STRING_BLANK);
				// add data entries
				for (int j = 0; j < recordSet.size(); j++) {
					MeasurementType measurement = device.getMeasurement(recordSet.getChannelConfigNumber(), j);
					if (isRaw) { // do not change any values
						if (!measurement.isCalculation()) if (recordSet.isRaw())
							sb.append(row[j + 1].replace('.', decimalSeparator)).append(separator);
						else
							sb.append(row[j + 1].replace('.', decimalSeparator)).append(separator);
					}
					else
						// translate according device and measurement unit
						sb.append(row[j + 1].replace('.', decimalSeparator)).append(separator);
				}
				sb.deleteCharAt(sb.length() - 1).append(CSVReaderWriter.lineSep);
				writer.write(sb.toString());
				if (i % 50 == 0) GDE.getUiNotification().setProgress(((++progressCycle * 5000) / recordEntries));
				if (CSVReaderWriter.log.isLoggable(java.util.logging.Level.FINE)) CSVReaderWriter.log.log(java.util.logging.Level.FINE, "data line = " + sb.toString()); //$NON-NLS-1$
			}
			sb = null;
			CSVReaderWriter.log.log(Level.TIME, "CSV file = " + filePath + " erfolgreich geschieben" //$NON-NLS-1$ //$NON-NLS-2$
					+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$

			writer.flush();
			writer.close();
			writer = null;
			//recordSet.setSaved(true);
			GDE.getUiNotification().setProgress(100);
		}
		catch (IOException e) {
			CSVReaderWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0006, new Object[] { GDE.FILE_ENDING_CSV, filePath, e.getMessage() }));
		}
		catch (Exception e) {
			CSVReaderWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
		finally {
			GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
		}

	}

}
