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
package gde.device.opentx;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
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
	static StringBuffer				sb;

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

		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
			line = reader.readLine();
			String[] headerData = line.split(GDE.STRING_EMPTY + separator);
			if (headerData.length > 2 || headerData.length < 1) { //not according to exported file with device and channelCinfig, assume actual selected device and first channel configuration
				String tmpHeaderLine = CSVReaderWriter.application.getActiveDevice().getName() + separator + CSVReaderWriter.channels.getActiveChannel().getChannelConfigKey();
				headerData = tmpHeaderLine.split(GDE.STRING_EMPTY + separator);
			}

			for (int i = 0; i < headerData.length; i++) {
				if (i == 0) header.put(GDE.DEVICE_NAME, headerData[i].split("\\r")[0].trim());
				if (i == 1) header.put(GDE.CHANNEL_CONFIG_NAME, headerData[i].split(" ")[0].split("\\r")[0].trim());
			}
			CSVReaderWriter.log.log(Level.FINE, GDE.DEVICE_NAME + header.get(GDE.DEVICE_NAME));
			CSVReaderWriter.log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + (header.get(GDE.CHANNEL_CONFIG_NAME) != null ? header.get(GDE.CHANNEL_CONFIG_NAME) : "")); //$NON-NLS-1$

			while (!(line.startsWith("Date,Time")) && ((line = reader.readLine()) != null)) {
				// read until Date,Time,SWR,RSSI,A1,A2,A3,A4,GPS Date,GPS Time,Long,Lat,Course,GPS Speed(kts),GPS Alt,Baro Alt(m),Vertical Speed,Air Speed(kts),Temp1,Temp2,RPM,Fuel,Cell volts,Cell 1,Cell 2,Cell 3,Cell 4,Cell 5,Cell 6,Cell 7,Cell 8,Cell 9,Cell 10,Cell 11,Cell 12,Current,Consumption,Vfas,AccelX,AccelY,AccelZ,Rud,Ele,Thr,Ail,S1,S2,S3,LS,RS,SA,SB,SC,SD,SE,SF,SG,SH
			}
			header.put(GDE.CSV_DATA_HEADER, line);
			CSVReaderWriter.log.log(Level.FINE, GDE.CSV_DATA_HEADER + (header.get(GDE.CSV_DATA_HEADER) != null ? header.get(GDE.CSV_DATA_HEADER) : "")); //$NON-NLS-1$
		}
		catch (FileNotFoundException e) {
			CSVReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
			throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGW0011, new Object[] { filePath }));
		}
		catch (IOException e) {
			CSVReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.GDE_MSGW0012, new Object[] { filePath }));
		}
		finally {
			if (reader != null) reader.close();
		}

		return header;
	}

	/**
	 * evaluate channel/configuration and check units for absolute data
	 * <ul>
	 * <li>if the channel/configuration does not match device the first channel/configuration of the device will chosen
	 * <li>if units of absolute data will not match a warning dialog will show all red measurements keys with expected units
	 * </ul>
	 * @param header
	 */
	public static HashMap<String, String> evaluateType(char separator, HashMap<String, String> header, DeviceConfiguration deviceConfig) {
		//int index = 0, innerIndex = 0;
		//int countRaw = 0, countAbs = 0;
		StringBuilder sb_measurements = new StringBuilder();
		StringBuilder sb_units = new StringBuilder();

		String headerLine = header.get(GDE.CSV_DATA_HEADER);

		// Spannung;Strom;Ladung; < Spannung;Strom;Ladung;Leistung;Energie;
		String[] headerLineArray = headerLine.split(GDE.STRING_EMPTY + separator);
		header.put(GDE.CSV_DATA_TYPE, GDE.CSV_DATA_TYPE_RAW);
		CSVReaderWriter.log.log(Level.FINE, GDE.CSV_DATA_TYPE + header.get(GDE.CSV_DATA_TYPE));

		String channelConfig = header.get(GDE.CHANNEL_CONFIG_NAME);
		int channelNumber = CSVReaderWriter.channels.getChannelNumber(channelConfig);
		if (channelConfig != null && !CSVReaderWriter.channels.getActiveChannel().getChannelConfigKey().equals(channelConfig) && channelNumber >= 1 && channelNumber <= deviceConfig.getChannelCount()) {
			CSVReaderWriter.channels.setActiveChannelNumber(channelNumber);
		}
		else { // unknown channel configuration using active one
			channelConfig = CSVReaderWriter.channels.getActiveChannel().getChannelConfigKey();
			CSVReaderWriter.channels.setActiveChannelNumber(CSVReaderWriter.channels.getActiveChannelNumber());
			channelNumber = CSVReaderWriter.channels.getActiveChannelNumber();
		}
		header.put(GDE.CHANNEL_CONFIG_NAME, channelConfig);
		header.put(GDE.CHANNEL_CONFIG_NUMBER, "" + CSVReaderWriter.channels.getActiveChannelNumber());
		CSVReaderWriter.log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + header.get(GDE.CHANNEL_CONFIG_NUMBER) + " : " + header.get(GDE.CHANNEL_CONFIG_NAME));

		List<String> ignoreIndexList = new ArrayList<String>();
		for (int i = 2, j=2; i < headerLineArray.length; i++,j++) {
			if (headerLineArray[i].contains("LSW")) { //GPS Time, GPS Date, LSW hex value
				ignoreIndexList.add(GDE.STRING_EMPTY + j);
				header.put(GDE.CSV_DATA_IGNORE_INDEX, StringHelper.listToString(ignoreIndexList, separator));
				continue;
			}
			String mappedMeasurement = headerLineArray[i].contains("(") 
					? String.format("%s [%s]", headerLineArray[i].substring(0, headerLineArray[i].indexOf("(")), headerLineArray[i].substring(headerLineArray[i].indexOf("(")+1, headerLineArray[i].length()-1))
					:	OpenTxAdapter.properties.getProperty(headerLineArray[i].contains("(") ? headerLineArray[i].substring(0, headerLineArray[i].indexOf("(")) : headerLineArray[i]);
			if (mappedMeasurement == null) {
					mappedMeasurement = String.format("%s [-]", headerLineArray[i]);
			}
			//check if the mapped measurement name is already in use
			int count = 1;
			for (String measurement : sb_measurements.toString().split(GDE.STRING_SEMICOLON)) {
				if (mappedMeasurement.split("\\[|]")[0].trim().equals(measurement)) {
					mappedMeasurement = String.format("%s %d [%s]",
							mappedMeasurement.split("\\[|]")[0].trim().indexOf(GDE.CHAR_BLANK) > 0 ? mappedMeasurement.substring(0, mappedMeasurement.indexOf(GDE.CHAR_BLANK))
									: mappedMeasurement.split("\\[|]")[0].trim(), count++, mappedMeasurement.split("\\[|]")[1]);
					continue;
				}
			}
			CSVReaderWriter.log.log(Level.INFO, "corrected mappedMeasurement = " + mappedMeasurement);

			String[] inHeaderMeasurement = mappedMeasurement.trim().split("\\[|]"); //$NON-NLS-1$
			String inMeasurement = inHeaderMeasurement.length >= 1 ? inHeaderMeasurement[0].trim() : Settings.EMPTY;
			String inUnit = inHeaderMeasurement.length == 2 ? inHeaderMeasurement[1].trim() : Settings.EMPTY;
			//check for OpenTx GPS coordinates
			if (inMeasurement.equals("GPS")) {
				sb_measurements.append(inMeasurement+".latitude").append(GDE.STRING_SEMICOLON);
				sb_units.append("°").append(GDE.STRING_SEMICOLON);
				sb_measurements.append(inMeasurement+".longitude").append(GDE.STRING_SEMICOLON);
				sb_units.append("°").append(GDE.STRING_SEMICOLON);
				++j;
			}
			else if ((inMeasurement.startsWith("GPS") && inMeasurement.toLowerCase().contains("zeit")) && !(sb_measurements.toString().toLowerCase().contains("gps datum") || sb_measurements.toString().toLowerCase().contains("gps time"))) {
				mappedMeasurement = OpenTxAdapter.properties.getProperty("GPS.date");
				if (mappedMeasurement != null) {
					sb_measurements.append(mappedMeasurement.split("\\[|]")[0].trim()).append(GDE.STRING_SEMICOLON);
					sb_units.append(mappedMeasurement.split("\\[|]")[1].trim()).append(GDE.STRING_SEMICOLON);
				}
				else {
					sb_measurements.append("GPS.date").append(GDE.STRING_SEMICOLON);
					sb_units.append("yy-mm-dd").append(GDE.STRING_SEMICOLON);
				}
				mappedMeasurement = OpenTxAdapter.properties.getProperty("GPS.time");
				if (mappedMeasurement != null) {
					sb_measurements.append(mappedMeasurement.split("\\[|]")[0].trim()).append(GDE.STRING_SEMICOLON);
					sb_units.append(mappedMeasurement.split("\\[|]")[1].trim()).append(GDE.STRING_SEMICOLON);
				}
				else {
					sb_measurements.append("GPS.time").append(GDE.STRING_SEMICOLON);
					sb_units.append("HH:mm:ss").append(GDE.STRING_SEMICOLON);
				}
			}
			else if (inMeasurement.toLowerCase().startsWith("date")) {
				mappedMeasurement = OpenTxAdapter.properties.getProperty("GPS.date");
				if (mappedMeasurement != null) {
					sb_measurements.append(mappedMeasurement.split("\\[|]")[0].trim()).append(GDE.STRING_SEMICOLON);
					sb_units.append(mappedMeasurement.split("\\[|]")[1].trim()).append(GDE.STRING_SEMICOLON);
				}
				else {
					sb_measurements.append("GPS.date").append(GDE.STRING_SEMICOLON);
					sb_units.append("yy-mm-dd").append(GDE.STRING_SEMICOLON);
				}
				mappedMeasurement = OpenTxAdapter.properties.getProperty("GPS.time");
				if (mappedMeasurement != null) {
					sb_measurements.append(mappedMeasurement.split("\\[|]")[0].trim()).append(GDE.STRING_SEMICOLON);
					sb_units.append(mappedMeasurement.split("\\[|]")[1].trim()).append(GDE.STRING_SEMICOLON);
				}
				else {
					sb_measurements.append("GPS.time").append(GDE.STRING_SEMICOLON);
					sb_units.append("HH:mm:ss").append(GDE.STRING_SEMICOLON);
				}
			}
			else {
				sb_measurements.append(inMeasurement).append(GDE.STRING_SEMICOLON);
				sb_units.append(inUnit).append(GDE.STRING_SEMICOLON);
			}
		}
		header.put(GDE.CSV_DATA_HEADER_MEASUREMENTS, sb_measurements.toString());
		header.put(GDE.CSV_DATA_HEADER_UNITS, sb_units.toString());

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
		long inputFileSize = new File(filePath).length();
		int progressLineLength = Math.abs(CSVReaderWriter.application.getActiveDevice().getDataBlockSize(InputTypes.FILE_IO));
		BufferedReader reader; // to read the data
		IDevice device = CSVReaderWriter.application.getActiveDevice();
		Channel activeChannel = null;
		Vector<String> createdRecordSets = new Vector<>();

		try {
			HashMap<String, String> fileHeader = CSVReaderWriter.getHeader(separator, filePath);
			activeChannel = CSVReaderWriter.channels.get(CSVReaderWriter.channels.getChannelNumber(fileHeader.get(GDE.CHANNEL_CONFIG_NAME)));
			activeChannel = activeChannel == null ? CSVReaderWriter.channels.getActiveChannel() : activeChannel;

			if (activeChannel != null) {
				GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0134) + filePath);
				GDE.getUiNotification().setProgress(0);
				long time_ms = 0;

				// check for device name and channel or configuration in first line
				if (!CSVReaderWriter.application.getActiveDevice().getName().equals(fileHeader.get(GDE.DEVICE_NAME))) {
					MissMatchDeviceException e = new MissMatchDeviceException(Messages.getString(MessageIds.GDE_MSGW0013, new Object[] { fileHeader.get(GDE.DEVICE_NAME) })); // mismatch device name
					CSVReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
					throw e;
				}

				fileHeader = CSVReaderWriter.evaluateType(separator, fileHeader, (DeviceConfiguration) device);
				CSVReaderWriter.log.log(Level.FINE, "device name check ok, channel/configuration ok"); //$NON-NLS-1$

				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
				int lineNumber = 0;
				while (!(line.startsWith("Date,Time")) && ((line = reader.readLine()) != null)) {
					// read until Date,Time,SWR,RSSI,A1,A2,A3,A4,GPS Date,GPS Time,Long,Lat,Course,GPS Speed(kts),GPS Alt,Baro Alt(m),Vertical Speed,Air Speed(kts),Temp1,Temp2,RPM,Fuel,Cell volts,Cell 1,Cell 2,Cell 3,Cell 4,Cell 5,Cell 6,Cell 7,Cell 8,Cell 9,Cell 10,Cell 11,Cell 12,Current,Consumption,Vfas,AccelX,AccelY,AccelZ,Rud,Ele,Thr,Ail,S1,S2,S3,LS,RS,SA,SB,SC,SD,SE,SF,SG,SH
					++lineNumber;
				}

				if (GDE.isWithUi()) {
					CSVReaderWriter.channels.switchChannel(activeChannel.getNumber(), GDE.STRING_EMPTY);
					CSVReaderWriter.application.getMenuToolBar().updateChannelSelector();
					activeChannel = CSVReaderWriter.channels.getActiveChannel();
				}

				String[] tmpRecordNames = fileHeader.get(GDE.CSV_DATA_HEADER_MEASUREMENTS).split(GDE.STRING_SEMICOLON);
				String[] tmpRecordUnits = fileHeader.get(GDE.CSV_DATA_HEADER_UNITS).split(GDE.STRING_SEMICOLON);
				String[] tmpRecordSymbols = new String[tmpRecordNames.length];
				for (int i = 0; i < tmpRecordNames.length; i++) {
					tmpRecordSymbols[i] = GDE.STRING_EMPTY;
				}
				recordSet = createRecordSet(recordSetNameExtend, device, activeChannel, tmpRecordNames, tmpRecordUnits, tmpRecordSymbols);
				String recordSetName = recordSet.getName(); // cut length
				createdRecordSets.add(recordSetName);

				//find GPS related records and try to assign data type
				for (int i = 0; i < recordSet.size(); i++) {
					Record record = recordSet.get(i);
					if (record.getName().toLowerCase().contains("gps") && (record.getName().toLowerCase().contains("speed") || record.getName().toLowerCase().contains("geschw")) || record.getName().startsWith("GSpd"))
						record.setDataType(Record.DataType.GPS_SPEED);
					else if ((record.getName().toLowerCase().contains("gps") && (record.getName().toLowerCase().contains("alt") || record.getName().toLowerCase().contains("höhe"))) || record.getName().startsWith("Alt") || record.getName().startsWith("GAlt"))
						record.setDataType(Record.DataType.GPS_ALTITUDE);
					else if ((record.getUnit().contains("°") && record.getUnit().contains("'") && (record.getName().toLowerCase().contains("long") || record.getName().toLowerCase().contains("länge")))
							|| (record.getName().toLowerCase().contains("gps") && (record.getName().toLowerCase().contains("long") || record.getName().toLowerCase().contains("länge")))
							|| record.getName().toLowerCase().contains("ngengrad")) {
						record.setDataType(Record.DataType.GPS_LONGITUDE);
						if (record.getUnit().equals(GDE.STRING_DASH)) record.setUnit("°");
					}
					else if (record.getUnit().contains("°") && record.getUnit().contains("'") && (record.getName().toLowerCase().contains("lat") || record.getName().toLowerCase().contains("breit"))
							|| (record.getName().toLowerCase().contains("gps") && (record.getName().toLowerCase().contains("lat") || record.getName().toLowerCase().contains("breit")))
							|| record.getName().toLowerCase().contains("breitengrad")) {
						record.setDataType(Record.DataType.GPS_LATITUDE);
						if (record.getUnit().equals(GDE.STRING_DASH)) record.setUnit("°");
					}
					else if ((record.getName().toLowerCase().contains("gps") && (record.getName().toLowerCase().contains("date") || record.getName().toLowerCase().contains("time") || record.getName().toLowerCase().contains("datum") || record.getName().toLowerCase().contains("zeit")))
							|| record.getName().toLowerCase().contains("date")) {
						record.setDataType(Record.DataType.GPS_TIME);
					}

					MeasurementType measurement = device.getMeasurement(activeChannel.getNumber(), i);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, tmpRecordNames[i]);
					measurement.setName(tmpRecordNames[i]);
					measurement.setSymbol(tmpRecordSymbols[i]);
					measurement.setUnit(tmpRecordUnits[i]);
				}

				long startTimeStamp = 0;
				long lastTimeStamp = 0;

				// now get all data   0; 14,780;  0,598;  1,000;  8,838;  0,002
				String[] updateRecordNames = recordSet.getRecordNames();
				int[] points = new int[updateRecordNames.length];
				while ((line = reader.readLine()) != null) {
					++lineNumber;
					if (line.startsWith("#")) {
						if (recordSet.getRecordSetDescription().endsWith(GDE.LINE_SEPARATOR))
							recordSet.setRecordSetDescription(recordSet.getRecordSetDescription() + line.substring(1) + GDE.LINE_SEPARATOR);
						else
							recordSet.setRecordSetDescription(recordSet.getRecordSetDescription() + line.replace('#', GDE.CHAR_BLANK) + GDE.LINE_SEPARATOR);
						continue;
					}
					String[] dataStr = line.replace(",,", ",0.0 0.0,").replace(", ", ",").split(" |"+separator); //replace(", ", ",") to allow GPS coords ..,48.474413, 11.477986,.. split(" |"+separator) to take ..,48.132850 11.720808,..
					String data = dataStr[0].trim();
					int year = Integer.parseInt(data.substring(0, 4));
					int month = Integer.parseInt(data.substring(5, 7));
					int day = Integer.parseInt(data.substring(8, 10));

					data = dataStr[1].trim();
					int hour = Integer.parseInt(data.substring(0, 2));
					int minute = Integer.parseInt(data.substring(3, 5));
					int second = Integer.parseInt(data.substring(6, 8));
					int millis = Integer.parseInt(data.substring(9, 12));

					Calendar calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
					long timeStamp = calendar.getTimeInMillis() + millis;

					if (lastTimeStamp < timeStamp) {
						if (timeStamp - lastTimeStamp > 5000) {
							if (recordSet.get(0).size() > 100) {
								if (lastTimeStamp > 0)
									log.log(Level.WARNING,  String.format("time differenze = %s msec, actual number record entries = %d", CSVReaderWriter.getFormatedTimeDiff(startTimeStamp, lastTimeStamp),  (recordSet.get(0).size())));
								recordSet.setSaved(true);
								activeChannel.put(recordSetName, recordSet);
								activeChannel.setActiveRecordSet(recordSetName);
								activeChannel.applyTemplate(recordSetName, true);
								device.updateVisibilityStatus(recordSet, true);
								//if (GDE.isWithUi()) activeChannel.switchRecordSet(recordSetName);
							}
							else {
								log.log(Level.WARNING,  String.format("time differenze = %d msec, actual number record entries = %d (< 100 -> remove)", (timeStamp - lastTimeStamp),  (recordSet.get(0).size())));
							}

							recordSet = createRecordSet(recordSetNameExtend, device, activeChannel, tmpRecordNames, tmpRecordUnits, tmpRecordSymbols);
							recordSetName = recordSet.getName(); // cut length
							lastTimeStamp = startTimeStamp = 0;
							if (!createdRecordSets.contains(recordSetName))
								createdRecordSets.add(recordSetName);
						}

						time_ms = lastTimeStamp == 0 ? 0 : time_ms + (timeStamp - lastTimeStamp);
						lastTimeStamp = timeStamp;
						if (startTimeStamp == 0) {
							startTimeStamp = timeStamp;
							recordSet.setRecordSetDescription(
									device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp));
							recordSet.setStartTimeStamp(startTimeStamp);
							recordSet.descriptionAppendFilename(filePath);
							activeChannel.setFileDescription((new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp)).substring(0, 10) + activeChannel.getFileDescription().substring(10));
						}
						
						//find GPS related records and try to assign data type
						for (int i = 0; i < recordSet.size(); i++) {
							Record record = recordSet.get(i);
							if (record.getName().toLowerCase().contains("gps") && (record.getName().toLowerCase().contains("speed") || record.getName().toLowerCase().contains("geschw")) || record.getName().startsWith("GSpd"))
								record.setDataType(Record.DataType.GPS_SPEED);
							else if ((record.getName().toLowerCase().contains("gps") && (record.getName().toLowerCase().contains("alt") || record.getName().toLowerCase().contains("höhe"))) || record.getName().startsWith("Alt") || record.getName().startsWith("GAlt"))
								record.setDataType(Record.DataType.GPS_ALTITUDE);
							else if ((record.getUnit().contains("°") && record.getUnit().contains("'") && (record.getName().toLowerCase().contains("long") || record.getName().toLowerCase().contains("länge")))
									|| (record.getName().toLowerCase().contains("gps") && (record.getName().toLowerCase().contains("long") || record.getName().toLowerCase().contains("länge")))
									|| record.getName().toLowerCase().contains("gengrad")) {
								record.setDataType(Record.DataType.GPS_LONGITUDE);
								if (record.getUnit().equals(GDE.STRING_DASH)) record.setUnit("°");
							}
							else if (record.getUnit().contains("°") && record.getUnit().contains("'") && (record.getName().toLowerCase().contains("lat") || record.getName().toLowerCase().contains("breit"))
									|| (record.getName().toLowerCase().contains("gps") && (record.getName().toLowerCase().contains("lat") || record.getName().toLowerCase().contains("breit")))
									|| record.getName().toLowerCase().contains("tengrad")) {
								record.setDataType(Record.DataType.GPS_LATITUDE);
								if (record.getUnit().equals(GDE.STRING_DASH)) record.setUnit("°");
							}
							else if ((record.getName().toLowerCase().contains("gps") && (record.getName().toLowerCase().contains("date") || record.getName().toLowerCase().contains("time") || record.getName().toLowerCase().contains("datum") || record.getName().toLowerCase().contains("zeit")))
									|| record.getName().toLowerCase().contains("date")) {
								record.setDataType(Record.DataType.GPS_TIME);
							}

							if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " - " + record.getDataType());
							MeasurementType measurement = device.getMeasurement(activeChannel.getNumber(), i);
							measurement.setName(tmpRecordNames[i]);
							measurement.setSymbol(tmpRecordSymbols[i]);
							measurement.setUnit(tmpRecordUnits[i]);
						}
					}
					else
						continue;

					List<String> skipIndexList = fileHeader.get(GDE.CSV_DATA_IGNORE_INDEX) != null ? StringHelper.stringToList(fileHeader.get(GDE.CSV_DATA_IGNORE_INDEX), separator) : new ArrayList<String>();
					for (int i = 0, j = 0; i < updateRecordNames.length; i++, j++) { // only iterate over record names found in file
						while (skipIndexList.size() > 0 && j + 2 == Integer.parseInt(skipIndexList.get(0))) {
							skipIndexList = skipIndexList.subList(1, skipIndexList.size());
							++j;
						}
						if ((j+2) < dataStr.length) {
							try {
								data = dataStr[j + 2].trim().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY);
								//log.log(Level.OFF, String.format("%s i=%d j=%d %s", recordSet.get(i).getName(), i, j, data));
							}
							catch (Exception e) {
								data = "0";
								CSVReaderWriter.log.log(Level.WARNING, String.format("Check line = %s position %d", line, j));
							}
							switch (recordSet.get(i).getDataType()) {
							case GPS_LONGITUDE:
							case GPS_LATITUDE:
								if (data.contains(GDE.STRING_MINUS))
									points[i] = 0;
								else { //normalize GPS coordinate to enable %9.6f
									data = data.replace("E", GDE.STRING_EMPTY).replace('W', GDE.CHAR_DASH).replace("N", GDE.STRING_EMPTY).replace('S', GDE.CHAR_DASH).replace(GDE.STRING_COLON, GDE.STRING_EMPTY);
									if (recordSet.get(i).getUnit().endsWith("'"))
										data = data.replace(GDE.STRING_DOT, GDE.STRING_EMPTY);
									else {
										if (data.length() != 0)
											data = String.format(Locale.ENGLISH, "%.6f", Double.parseDouble(data)).replace(GDE.STRING_DOT, GDE.STRING_EMPTY);
										else
											data = "0.0";
									}
									try {
										points[i] = Double.valueOf(data).intValue();
									}
									catch (NumberFormatException e) {
										log.log(Level.WARNING, e.getMessage(), e);
									}
								}
								break;

							case GPS_TIME:
								try {
									if (data.contains(GDE.STRING_DASH) && data.length() == 10) { //Date
										data = data.trim().replace(GDE.STRING_DASH, GDE.STRING_EMPTY);
										points[i] = Integer.parseInt(data.substring(2)) * 1000;
									}
									else if (data.contains(GDE.STRING_COLON) && data.length() == 8) { // Time
										data = data.trim().replace(GDE.STRING_COLON, GDE.STRING_EMPTY);;
										points[i] = Integer.parseInt(data) * 1000;
									}
									else
										points[i] = 0;
								}
								catch (NumberFormatException e) {
									points[i] = 0;
								}
								break;
								
							default:
								try {
									double value = Double.valueOf(data);
									if (value <= Integer.MIN_VALUE / 1000 || value >= Integer.MAX_VALUE / 1000)
										log.log(Level.WARNING, String.format("Check line %d, found misterious value %s", lineNumber, data));
									else
										points[i] = (int) (value * 1000.0);

								}
								catch (NumberFormatException e) {
										points[i] = 0;
								}
								break;
							}
						}
					}
					try {
						recordSet.addPoints(points, time_ms);
					}
					catch (DataInconsitsentException e) {
						DataExplorer.getInstance().openMessageDialogAsync(e.getMessage() + "\n" + StringHelper.arrayToString(updateRecordNames));
						throw e;
					}

					progressLineLength = progressLineLength > line.length() ? progressLineLength : line.length();
					int progress = progressLineLength > 0 && inputFileSize > 0 ? (int) (lineNumber * 100 / (inputFileSize / progressLineLength)) : 50;
					if (progress <= 90 && progress > GDE.getUiNotification().getProgressPercentage() && progress % 10 == 0) {
						GDE.getUiNotification().setProgress(progress);
						try {
							Thread.sleep(2);
						}
						catch (Exception e) {
							// ignore
						}
					}
				}

				if (recordSet.get(0).size() > 100) {
					recordSet.setSaved(true);
					activeChannel.put(recordSetName, recordSet);
					activeChannel.setActiveRecordSet(recordSetName);
					activeChannel.applyTemplate(recordSetName, true);
					device.updateVisibilityStatus(recordSet, true);
				}
				else {
					activeChannel.remove(recordSetName);
					if (createdRecordSets.contains(recordSetName))
						createdRecordSets.remove(recordSetName);
					log.log(Level.WARNING, "recordSet removed, number of measurements " + recordSet.get(0).size() + " < 100");
				}

				if (GDE.isWithUi() && createdRecordSets.size() > 0) 
					activeChannel.switchRecordSet(createdRecordSets.firstElement()); //recordSetName);

				reader.close();
				reader = null;
			}
		}
		catch (UnsupportedEncodingException e) {
			CSVReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
			throw new UnsupportedEncodingException(Messages.getString(MessageIds.GDE_MSGW0010));
		}
		catch (FileNotFoundException e) {
			CSVReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
			throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGW0011, new Object[] { filePath }));
		}
		catch (IOException e) {
			CSVReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.GDE_MSGW0012, new Object[] { filePath }));
		}
		finally {
			GDE.getUiNotification().setProgress(100);
			GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
			if (GDE.isWithUi()) {
				CSVReaderWriter.application.getMenuToolBar().updateChannelSelector();
				CSVReaderWriter.application.getMenuToolBar().updateRecordSetSelectCombo();
			}
		}

		return recordSet;
	}

	/**
	 * create a new recordSet - time gap occured
	 * @param recordSetNameExtend
	 * @param device
	 * @param activeChannel
	 * @param tmpRecordNames
	 * @param tmpRecordUnits
	 * @param tmpRecordSymbols
	 * @return
	 */
	private static RecordSet createRecordSet(String recordSetNameExtend, IDevice device, Channel activeChannel, String[] tmpRecordNames, String[] tmpRecordUnits, String[] tmpRecordSymbols) {
		String recordSetName = (activeChannel.size() + 1) + recordSetNameExtend;
		RecordSet recordSet = RecordSet.createRecordSet(recordSetName, device, activeChannel.getNumber(), tmpRecordNames, tmpRecordSymbols, tmpRecordUnits, device.getTimeStep_ms(), true, true, true);
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
			CSVReaderWriter.sb = new StringBuffer();
			RecordSet recordSet = Channels.getInstance().getActiveChannel().get(recordSetKey);
			IDevice device = DataExplorer.getInstance().getActiveDevice();
			// write device name , manufacturer, and serial port string
			CSVReaderWriter.sb.append(device.getName()).append(separator).append(recordSet.getChannelConfigName()).append(CSVReaderWriter.lineSep);
			writer.write(CSVReaderWriter.sb.toString());
			CSVReaderWriter.log.log(Level.FINE, "written header line = " + CSVReaderWriter.sb.toString()); //$NON-NLS-1$

			CSVReaderWriter.sb = new StringBuffer();
			CSVReaderWriter.sb.append(Messages.getString(MessageIds.GDE_MSGT0137)).append(separator); // Spannung [V];Strom [A];Ladung [Ah];Leistung [W];Energie [Wh]";
			// write the measurements signature
			for (int i = 0; i < recordSet.size(); i++) {
				MeasurementType measurement = device.getMeasurement(recordSet.getChannelConfigNumber(), i);
				Record record = recordSet.get(i);
				CSVReaderWriter.log.log(Level.FINEST, "append " + record.getName()); //$NON-NLS-1$
				if (isRaw) {
					if (!measurement.isCalculation()) { // only use active records for writing raw data
						CSVReaderWriter.sb.append(record.getName()).append(" [---]").append(separator); //$NON-NLS-1$
						CSVReaderWriter.log.log(Level.FINEST, "append " + record.getName()); //$NON-NLS-1$
					}
				}
				else {
					CSVReaderWriter.sb.append(record.getName()).append(" [").append(record.getUnit()).append(']').append(separator); //$NON-NLS-1$
					CSVReaderWriter.log.log(Level.FINEST, "append " + record.getName()); //$NON-NLS-1$
				}
			}
			CSVReaderWriter.sb.deleteCharAt(CSVReaderWriter.sb.length() - 1).append(CSVReaderWriter.lineSep);
			CSVReaderWriter.log.log(Level.FINER, "header line = " + CSVReaderWriter.sb.toString()); //$NON-NLS-1$
			writer.write(CSVReaderWriter.sb.toString());

			// write data
			long startTime = new Date(recordSet.getTime(0)).getTime();
			int recordEntries = recordSet.getRecordDataSize(true);
			int progressCycle = 0;
			GDE.getUiNotification().setProgress(progressCycle);
			for (int i = 0; i < recordEntries; i++) {
				CSVReaderWriter.sb = new StringBuffer();
				String[] row = recordSet.getExportRow(i, true);

				// add time entry
				CSVReaderWriter.sb.append(row[0].replace('.', decimalSeparator)).append(separator).append(GDE.STRING_BLANK);
				// add data entries
				for (int j = 0; j < recordSet.size(); j++) {
					MeasurementType measurement = device.getMeasurement(recordSet.getChannelConfigNumber(), j);
					if (isRaw) { // do not change any values
						if (!measurement.isCalculation()) if (recordSet.isRaw())
							CSVReaderWriter.sb.append(row[j + 1].replace('.', decimalSeparator)).append(separator);
						else
							CSVReaderWriter.sb.append(row[j + 1].replace('.', decimalSeparator)).append(separator);
					}
					else
						// translate according device and measurement unit
						CSVReaderWriter.sb.append(row[j + 1].replace('.', decimalSeparator)).append(separator);
				}
				CSVReaderWriter.sb.deleteCharAt(CSVReaderWriter.sb.length() - 1).append(CSVReaderWriter.lineSep);
				writer.write(CSVReaderWriter.sb.toString());
				if (i % 50 == 0) GDE.getUiNotification().setProgress(((++progressCycle * 5000) / recordEntries));
				if (CSVReaderWriter.log.isLoggable(Level.FINE)) CSVReaderWriter.log.log(Level.FINE, "data line = " + CSVReaderWriter.sb.toString()); //$NON-NLS-1$
			}
			CSVReaderWriter.sb = null;
			CSVReaderWriter.log.log(Level.TIME, "CSV file = " + filePath + " erfolgreich geschieben" //$NON-NLS-1$ //$NON-NLS-2$
					+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$

			writer.flush();
			writer.close();
			writer = null;
			//recordSet.setSaved(true);
			GDE.getUiNotification().setProgress(100);
		}
		catch (IOException e) {
			CSVReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0006, new Object[] { GDE.FILE_ENDING_CSV, filePath, e.getMessage() }));
		}
		catch (Exception e) {
			CSVReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
		finally {
			GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
		}

	}
	
  public static String getFormatedTimeDiff(long startTime_ms, long endTime_ms) {
    long difference = (endTime_ms - startTime_ms) / 1000L;
    long hours = difference / 3600;
    difference %= 3600;
    long minutes = difference / 60;
    difference %= 60;
    long seconds = difference;

    return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds);
}

}
