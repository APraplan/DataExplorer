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
package gde.device.logview;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.mozilla.universalchardet.ReaderFactory;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.ChannelType;
import gde.device.ChannelTypes;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.device.MeasurementType;
import gde.device.StatisticsType;
import gde.device.resource.DeviceXmlResource;
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
public class ZeroReaderWriter {
	static Logger							log					= Logger.getLogger(ZeroReaderWriter.class.getName());

	static String							lineSep			= GDE.LINE_SEPARATOR;
	static DecimalFormat			df3					= new DecimalFormat("0.000");												//$NON-NLS-1$
	static StringBuffer				sb;

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();
	
 	final static String DATA_SET_NUMBER = "data-set-number";
	final static String DATA_SET_NAME = "data-set-name";
	final static String MEASUREMENT_NAME_UNIT_SYMBOLS = "name-unit-symbol";
	final static String VALUE_FORMAT = "value-format";
	final static String LOG_MESSAGE = "log-message";
	final static String VALUE_FACTORS = "value-factors";
	final static String VALUE_OFFSETS = "value-offsets";
	final static String VALUE_REDUCTIONS = "value-reductions";
	final static String RESET_TIME_MODE = "reset-time-mode";
	final static String TIME_GIVEN_INDEX = "time-given-index";
	final static String TIME_GIVEN_FORMAT = "time-given-format";
	final static String TIME_GIVEN_MS = "time-given-ms";
	final static String TIME_GIVEN_MS_LAST = "time-given-ms-last";
	final static String TIME_GIVEN_MS_START = "time-given-ms-start";
	final static String TIME_MODE_INCREMENT = "time-mode-increment";
	final static String TIME_MODE_INCREMENT_START = "time-mode-increment-start";
	final static String TIME_MODE_INCREMENT_MS = "time-mode-increment-ms";
	

	/**
	 * @param headers the map in map structure, if needed a new header map get initialized
	 * @param line the line to parse for header data
	 * @return header map according data set number
	 */
	private static HashMap<String, String> getHeaderDataSet(HashMap<Integer, HashMap<String, String>> headers, String line) {
		int channelConfigNumber = line.substring(2, 3).matches("([0-9])") ? Integer.parseInt(line.substring(2, 3)) : 1;
		if (headers.get(channelConfigNumber) == null) headers.put(channelConfigNumber, new HashMap<String, String>());
		HashMap<String, String> header = headers.get(channelConfigNumber);
		return header;
	}


	/**
	 * update header information corresponding to channel and record set
	 * @param headers initialized surrounding map
	 * @param separator to be used to separate entries
	 * @param line already trimmed identified as header line
	 * @return updated map in map structure 
	 */
	public static HashMap<Integer, HashMap<String, String>> updateHeaders(final HashMap<Integer, HashMap<String, String>> headers, final char separator, final String line)
			throws NotSupportedFileFormatException, IOException {
		/*
		LogView Studio Documentation
		General Information for the Header Settings
		Header Settings are valid as long as you don't change them. You can set the Columns once and send multiple datasets. All datasets will use the same Columns. Same for Factor, Offsets and Timesettings.
		
		$N<CHN>$;DatasetName
		Set the Dataset Name in LogView Studio and indicates the start of a new data set.
		If you only send $N$; LogView Studio will use a Default dataset name.
		Note: This header is mandatory for a new dataset!
		
		$C<CHN>$;Temp 1 [°C,T];Voltage[V];Current
		Set the columns which will be received in the data part. Each column is separated by a ; (semicolon).
		As you can see the Column name can be supplemented by one or two values in [].
		The first value is the Unit and the second value is the Symbol. You can also use only one additional value. In that case you have to transmit the Unit.
		It's also possible to send only the Column name.
		Note: The Column count must match the data item count !
		
		$V<CHN>$;VALUE
		You can set the value format for the data lines. Valid Values are HEX and DOUBLE.
		If you don't set this value the Default is DOUBLE.
		 
		$L<CHN>$;Level;LogText
		You can send Log Messages to LogView Studio. They appear in the LogView Logging system. You can use this feature to send some Debug information.
		Level can be set to the following values:
		E = Error
		W = Warning
		V = Verbose
		If you only set the LogText the Default Level is Verbose.
		 
		$F<CHN>$;Factor1;Factor2;....
		$O<CHN>$;Offset1;Offset2;....
		$S<CHN>$;SumOffset1;SumOffset2;.....
		LogView can calculate the value based on three values : Factor, Offset, Sum Offset (GDE.REDUCTION)
		The formula for this is : y = (x + Offset) * Factor + SumOffset
		Note : Factor, Offset and SumOffset must have the same count as the data items count!
		
		$R<CHN>$
		Reset the Time Settings to Time Mode Receivedate.
		 
		$T<CHN>$;1;yyMMdd
		Set the Time Settings to Time Mode TimeGiven.
		The first parameter (1) indicates the Time position in the data line (value starts with Index 0 !)
		The second parameter sets the given DateTime Format.
		 
		$I<CHN>$;1000;28042013_120005123
		Set the Time Setting to Time Mode Increment.
		The first parameter is the increment in Milliseconds.
		The second parameter is optional and indicates the Start Date Time. The Format is : DDMMYYYY_HHMMSSFFF (FFF = Milliseconds).
		*/

		if (line.startsWith("$N")) { //$N<CHN>$;DatasetName
			HashMap<String, String> recordSetHeader = ZeroReaderWriter.getHeaderDataSet(headers, line);
			
			int channelConfigNumber = line.substring(2, 3).matches("([0-9])") ? Integer.parseInt(line.substring(2, 3)) : 1;
			if (channels.get(channelConfigNumber) == null) {
				if (recordSetHeader.get(DATA_SET_NAME) == null) {
					recordSetHeader.put(DATA_SET_NUMBER, "1");
				}
				else {
					recordSetHeader.put(DATA_SET_NUMBER, GDE.STRING_EMPTY + (Integer.parseInt(recordSetHeader.get(DATA_SET_NUMBER)) + 1));
				}
			}
			else {
				recordSetHeader.put(DATA_SET_NUMBER, GDE.STRING_EMPTY + (channels.get(channelConfigNumber).size() + 1));
			}
			recordSetHeader.put(DATA_SET_NAME, line.substring(line.indexOf(separator, 2) + 1));
		}
		else if (line.startsWith("$C")) { //$C<CHN>$;Temp 1 [°C,T];Voltage[V];Current
			ZeroReaderWriter.getHeaderDataSet(headers, line).put(MEASUREMENT_NAME_UNIT_SYMBOLS, line.substring(line.indexOf(separator, 2) + 1));
		}
		else if (line.startsWith("$V")) { //$V<CHN>$;HEX|DOUBLE (default DOUBLE)
			ZeroReaderWriter.getHeaderDataSet(headers, line).put(VALUE_FORMAT, line.substring(line.indexOf(separator, 2) + 1));
		}
		else if (line.startsWith("$L")) { //$L<CHN>$;LogLevel;LogText
			ZeroReaderWriter.getHeaderDataSet(headers, line).put(LOG_MESSAGE, ZeroReaderWriter.getHeaderDataSet(headers, line).get(LOG_MESSAGE) 
					+ line.substring(line.indexOf(separator, 2) + 1) + GDE.STRING_NEW_LINE);
		}
		else if (line.startsWith("$F")) { //$F<CHN>$;Factor1;Factor2;....
			ZeroReaderWriter.getHeaderDataSet(headers, line).put(VALUE_FACTORS, line.substring(line.indexOf(separator, 2) + 1));
		}
		else if (line.startsWith("$O")) { //$O<CHN>$;Offset1;Offset2;....
			ZeroReaderWriter.getHeaderDataSet(headers, line).put(VALUE_OFFSETS, line.substring(line.indexOf(separator, 2) + 1));
		}
		else if (line.startsWith("$S")) { //$S<CHN>$;SumOffset1;SumOffset2;.....
			ZeroReaderWriter.getHeaderDataSet(headers, line).put(VALUE_REDUCTIONS, line.substring(line.indexOf(separator, 2) + 1));
		}
		else if (line.startsWith("$R")) { //$R<CHN>$ 	Reset the Time Settings to Time Mode Receive date.
			ZeroReaderWriter.getHeaderDataSet(headers, line).put(RESET_TIME_MODE, line.substring(line.indexOf(separator, 2) + 1));
		}
		else if (line.startsWith("$T")) { //$T<CHN>$;1;yyMMdd  Time Mode TimeGiven.
			String[] timeGiventDefinition = line.substring(line.indexOf(separator, 2) + 1).split(GDE.STRING_EMPTY + separator);
			if (timeGiventDefinition.length >= 1) 
				ZeroReaderWriter.getHeaderDataSet(headers, line).put(TIME_GIVEN_INDEX, timeGiventDefinition[0]);
			if (timeGiventDefinition.length >= 2) {
				ZeroReaderWriter.getHeaderDataSet(headers, line).put(TIME_GIVEN_FORMAT, timeGiventDefinition[1]);
			}
		}
		else if (line.startsWith("$I")) { //$I<CHN>$;1000;28042013_120005123 Time Mode Increment.
			String[] timeModeIncrementDefinition = line.substring(line.indexOf(separator, 2) + 1).split(GDE.STRING_EMPTY + separator);
			if (timeModeIncrementDefinition.length >= 1) 
				ZeroReaderWriter.getHeaderDataSet(headers, line).put(TIME_MODE_INCREMENT, timeModeIncrementDefinition[0]);
			if (timeModeIncrementDefinition.length >= 2) {
				int day = Integer.parseInt(timeModeIncrementDefinition[1].substring(0, 1));
				int month = Integer.parseInt(timeModeIncrementDefinition[1].substring(2, 3));
				int year = Integer.parseInt(timeModeIncrementDefinition[1].substring(4, 7));
				int hour = Integer.parseInt(timeModeIncrementDefinition[1].substring(9, 10));
				int minute = Integer.parseInt(timeModeIncrementDefinition[1].substring(11, 12));
				int second = Integer.parseInt(timeModeIncrementDefinition[1].substring(13, 14));
				int msecond = Integer.parseInt(timeModeIncrementDefinition[1].substring(15, 17));
				GregorianCalendar calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
				ZeroReaderWriter.getHeaderDataSet(headers, line).put(TIME_MODE_INCREMENT_START, GDE.STRING_EMPTY + (calendar.getTimeInMillis() + msecond));
			}
		}

		return headers;
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
		int progressLineLength = Math.abs(ZeroReaderWriter.application.getActiveDevice().getDataBlockSize(InputTypes.FILE_IO));
		BufferedReader reader = null;
		IDevice device = ZeroReaderWriter.application.getActiveDevice();
		Channel activeChannel = null;
		HashMap<Integer, HashMap<String, String>> headers = new HashMap<Integer, HashMap<String, String>>();

		try {
			HashMap<String, String> dataSetHeader;
			boolean isNewHeaderEntry = false; 
			int channelConfigNumber = 1; //default
			int recordSetNumber = 0;
			int lineNumber = 0;
			long time_ms = 0;
			int dataStartOffset = 1;

			reader = ReaderFactory.createBufferedReader(new java.io.File(filePath));

			while (((line = reader.readLine()) != null)) {
				++lineNumber;
				line = line.trim();
				if (!line.startsWith("$")) {
					continue;
				} else if (line.matches("\\$([A-Z])\\$(.*)") || line.matches("\\$([A-Z])([1-6])\\$(.*)")) { //header line
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "header: " + line);
					ZeroReaderWriter.updateHeaders(headers, separator, line);
					isNewHeaderEntry = true;
				}
				else if (!line.matches("\\$(.*)\\$(.*)") || line.matches("\\$([1-6])\\$(.*)")) { //data line
					channelConfigNumber = 1; //default
					dataStartOffset = 1; //line without channel number $11.98;11.63;0
					if (line.matches("\\$([1-6])\\$(.*)")) { //allow a max of 6 channelConfigs
						channelConfigNumber = line.substring(1, 2).matches("([0-9])") ? Integer.parseInt(line.substring(1, 2)) : 1;
						dataStartOffset = 4; //line with channel number $2$;12.00;11.63;0
					}
					int recordSetOrdinal = Integer.parseInt(headers.get(channelConfigNumber).get(DATA_SET_NUMBER));
					String recordSetKey = recordSetOrdinal + GDE.STRING_RIGHT_PARENTHESIS_BLANK + headers.get(channelConfigNumber).get(DATA_SET_NAME);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "channel = " + channelConfigNumber + "; recordSetName  = " + recordSetKey);
					if (isNewHeaderEntry 
							&& ((channels.get(channelConfigNumber) != null && channels.get(channelConfigNumber).get(recordSetKey) == null) 	//new recordSet
							|| channels.get(channelConfigNumber) == null)) {																																//new channel
						dataSetHeader = headers.get(channelConfigNumber);
						recordSetNumber = Integer.parseInt(headers.get(channelConfigNumber).get(DATA_SET_NUMBER));
						dataSetHeader.put(TIME_MODE_INCREMENT_MS, null); //reset incremental time
						dataSetHeader.put(TIME_GIVEN_MS_LAST, null); //reset time given 

						//create recordSet and update corresponding measurementType
						activeChannel = ZeroReaderWriter.channels.get(channelConfigNumber);
						if (activeChannel == null) {
							int tmpChannelConfigNumber = ZeroReaderWriter.channels.size() + 1;
							while (ZeroReaderWriter.channels.get(tmpChannelConfigNumber) == null && tmpChannelConfigNumber <= channelConfigNumber) {
								ZeroReaderWriter.channels.put(tmpChannelConfigNumber,
										new Channel(Analyzer.getInstance(), DeviceXmlResource.getInstance().getReplacement("type_config_" + tmpChannelConfigNumber), ChannelTypes.TYPE_CONFIG));
								activeChannel = ZeroReaderWriter.channels.get(tmpChannelConfigNumber);
								//update device XML
								device.getDeviceConfiguration().addChannelType(new ChannelType());
								device.getDeviceConfiguration().setChannelName("type_config_" + tmpChannelConfigNumber, tmpChannelConfigNumber);
								device.getDeviceConfiguration().setChannelTypes(ChannelTypes.TYPE_CONFIG, tmpChannelConfigNumber);
								device.getDeviceConfiguration().addMeasurement2Channel(tmpChannelConfigNumber, new MeasurementType());
								++tmpChannelConfigNumber;
							}
						}

						if (activeChannel != null) {
							GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0134) + filePath);
							GDE.getUiNotification().setProgress(0);

							recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + dataSetHeader.get(DATA_SET_NAME);
							String[] tmpMeasurementNameUnitSymbols = dataSetHeader.get(MEASUREMENT_NAME_UNIT_SYMBOLS).split(GDE.STRING_EMPTY + separator);
							List<String> tmpRecordNames = new ArrayList<>();
							List<String> tmpRecordUnits = new ArrayList<>();
							List<String> tmpRecordSymbols = new ArrayList<>();
							for (String tmpMeasurementNameUnitSymbol : tmpMeasurementNameUnitSymbols) {
								String[] measurementNameUnitSymbol = tmpMeasurementNameUnitSymbol.split("\\[|\\]|\\,");
								tmpRecordNames.add(measurementNameUnitSymbol[0]);
								tmpRecordUnits.add(measurementNameUnitSymbol.length > 1 ? measurementNameUnitSymbol[1] : GDE.STRING_BLANK);
								tmpRecordSymbols.add(measurementNameUnitSymbol.length > 2 ? measurementNameUnitSymbol[2] : GDE.STRING_BLANK);
							}

							List<String> tmpMeasurementFactors = dataSetHeader.get(VALUE_FACTORS) != null ? Arrays.asList(dataSetHeader.get(VALUE_FACTORS).split(GDE.STRING_EMPTY + separator))
									: new ArrayList<>(tmpRecordNames.size());
							List<String> tmpMeasurementOffsets = dataSetHeader.get(VALUE_OFFSETS) != null ? Arrays.asList(dataSetHeader.get(VALUE_OFFSETS).split(GDE.STRING_EMPTY + separator))
									: new ArrayList<>(tmpRecordNames.size());
							List<String> tmpMeasurementReductions = dataSetHeader.get(VALUE_REDUCTIONS) != null ? Arrays.asList(dataSetHeader.get(VALUE_REDUCTIONS).split(GDE.STRING_EMPTY + separator))
									: new ArrayList<>(tmpRecordNames.size());

							//reduce number of measurement types in configuration if count is higher as needed
							int existingNumberMeasurementTypes = device.getDeviceConfiguration().getMeasurementNames(channelConfigNumber).length;
							for (int i = tmpRecordNames.size(); i < existingNumberMeasurementTypes; i++) {
								device.removeMeasurementFromChannel(channelConfigNumber, device.getMeasurement(channelConfigNumber, tmpRecordNames.size()));
							}

							for (int i = 0; i < tmpRecordNames.size(); i++) {
								MeasurementType measurement = device.getMeasurement(channelConfigNumber, i);
								if (log.isLoggable(Level.FINE)) log.log(Level.FINE, tmpRecordNames.get(i));
								measurement.setName(tmpRecordNames.get(i));
								measurement.setUnit(tmpRecordUnits.get(i));
								measurement.setSymbol(tmpRecordSymbols.get(i));
								measurement.setActive(true);
								measurement.setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
								if (tmpMeasurementFactors.size() > i && tmpMeasurementFactors.get(i).matches("([0-9])"))
									measurement.setFactor(Double.parseDouble(tmpMeasurementFactors.get(i)));
								else
									measurement.setFactor(1.0);
								if (tmpMeasurementOffsets.size() > i && tmpMeasurementOffsets.get(i).matches("([0-9])"))
									measurement.setOffset(Double.parseDouble(tmpMeasurementOffsets.get(i)));
								else
									measurement.setOffset(0.0);
								if (tmpMeasurementReductions.size() > i && tmpMeasurementReductions.get(i).matches("([0-9])"))
									measurement.setReduction(Double.parseDouble(tmpMeasurementReductions.get(i)));
								else
									measurement.setReduction(0.0);
							}
							device.setChangePropery(true);
							device.storeDeviceProperties();
							
							recordSet = RecordSet.createRecordSet(recordSetKey, device, channelConfigNumber, tmpRecordNames.toArray(new String[0]), tmpRecordSymbols.toArray(new String[0]),
									tmpRecordUnits.toArray(new String[0]), device.getTimeStep_ms(), true, true, true);
							recordSetKey = recordSet.getName(); // cut length

							//find GPS related records and try to assign data type
							for (int i = 0; i < recordSet.size(); i++) {
								Record record = recordSet.get(i);
								if (record.getUnit().equals("km/h"))
									record.setDataType(Record.DataType.GPS_SPEED);
								else if (record.getUnit().equals("m") && (record.getName().toLowerCase().contains("alti") || record.getName().toLowerCase().contains("höhe")))
									record.setDataType(Record.DataType.GPS_ALTITUDE);
								else if (record.getUnit().contains("°") && record.getUnit().contains("'") && (record.getName().toLowerCase().contains("long") || record.getName().toLowerCase().contains("länge")))
									record.setDataType(Record.DataType.GPS_LONGITUDE);
								else if (record.getUnit().contains("°") && record.getUnit().contains("'") && (record.getName().toLowerCase().contains("lat") || record.getName().toLowerCase().contains("breit")))
									record.setDataType(Record.DataType.GPS_LATITUDE);
							}
							activeChannel.put(recordSetKey, recordSet);
						}
						//at this point empty record sets are prepared and synchronized with device measurement types
						//received data line must fit into one of the exiting recordSets
						isNewHeaderEntry = false;
					}
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("channel: %d, recordSet %d, data line: %s", channelConfigNumber, recordSetNumber, line));
					dataSetHeader = headers.get(channelConfigNumber);
					recordSetNumber = Integer.parseInt(headers.get(channelConfigNumber).get(DATA_SET_NUMBER));
					recordSetKey = recordSetOrdinal + GDE.STRING_RIGHT_PARENTHESIS_BLANK + headers.get(channelConfigNumber).get(DATA_SET_NAME);
					recordSet = channels.get(channelConfigNumber).get(recordSetKey);

					String[] updateRecordNames = recordSet.getRecordNames();
					int[] points = new int[updateRecordNames.length];
					line = line.substring(dataStartOffset);

					String[] dataStringArray = line.split(GDE.STRING_EMPTY + separator);
					String data = dataStringArray[0].trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT);

					int indexTimeGiven = dataSetHeader.get(TIME_GIVEN_INDEX) != null ? Integer.parseInt(dataSetHeader.get(TIME_GIVEN_INDEX)) : -1;
					for (int i = 0, j = 0; j < updateRecordNames.length && i < dataStringArray.length; ++i, ++j) { 
						// only iterate over record names found in file, skip adding point value while time given index
						try {
							data = dataStringArray[i].trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT).replace(GDE.STRING_BLANK, GDE.STRING_EMPTY);
							
							if (indexTimeGiven == i) { //time given each line at index
								String formatString = dataSetHeader.get(TIME_GIVEN_FORMAT).replace(GDE.STRING_BLANK, GDE.STRING_EMPTY); //ddHHmmss, yyyyMMddHHmmss.f					
								int year = new GregorianCalendar().get(Calendar.YEAR);
								int subIndex = 0;
								if (formatString.toLowerCase().contains("yyyy")) {
									subIndex = formatString.indexOf("yyyy");
									year = Integer.parseInt(data.substring(subIndex, 4));
								}
								int month = new GregorianCalendar().get(Calendar.MONTH);
								if (formatString.contains("MM")) {
									subIndex = formatString.indexOf("MM");
									month = Integer.parseInt(data.substring(subIndex, subIndex+2)) - 1;
								}
								int day = new GregorianCalendar().get(Calendar.DAY_OF_MONTH);
								if (formatString.contains("dd")) {
									subIndex = formatString.indexOf("dd");
									day = Integer.parseInt(data.substring(subIndex, subIndex+2));
								}
								int hour = new GregorianCalendar().get(Calendar.HOUR_OF_DAY);
								if (formatString.contains("HH")) {
									subIndex = formatString.indexOf("HH");
									hour = Integer.parseInt(data.substring(subIndex, subIndex+2));
								}
								else if (formatString.contains("hh")) {
									subIndex = formatString.indexOf("hh");
									hour = Integer.parseInt(data.substring(subIndex, subIndex+2));
								}
								int minute = new GregorianCalendar().get(Calendar.MINUTE);
								if (formatString.contains("mm")) {
									subIndex = formatString.indexOf("mm");
									minute = Integer.parseInt(data.substring(subIndex, subIndex+2));
								}
								int second = new GregorianCalendar().get(Calendar.SECOND);
								if (formatString.contains("ss")) {
									subIndex = formatString.indexOf("ss");
									second = Integer.parseInt(data.substring(subIndex, subIndex+2));
								}
								GregorianCalendar calendar = new GregorianCalendar(year, month, day, hour, minute, second);
								//log.log(Level.OFF, ""+ calendar.getTimeInMillis());
								int milliseconds = 0;
								if (formatString.contains(".f") || formatString.contains(".S")) {
									subIndex = formatString.indexOf(".") + 1;
									milliseconds = Integer.parseInt(data.substring(subIndex, data.length())) * 100;
								}
								//log.log(Level.OFF, year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + "." + milliseconds);
								dataSetHeader.put(TIME_GIVEN_MS, GDE.STRING_EMPTY + (calendar.getTimeInMillis() + milliseconds));
								//log.log(Level.OFF, dataSetHeader.get(TIME_GIVEN_MS));
								if (dataSetHeader.get(TIME_GIVEN_MS_START) == null) dataSetHeader.put(TIME_GIVEN_MS_START, GDE.STRING_EMPTY + (calendar.getTimeInMillis() + milliseconds));
								--j; 
								continue;
							}
						}
						catch (Exception e) {
							data = "0";
							ZeroReaderWriter.log.log(Level.WARNING, String.format("Check line = %s", line));
						}
						switch (recordSet.get(j).getDataType()) {
						case GPS_LONGITUDE:
						case GPS_LATITUDE:
							try {
								points[j] = Double.valueOf(data.replace("E", GDE.STRING_EMPTY).replace('W', GDE.CHAR_DASH).replace("N", GDE.STRING_EMPTY).replace('S', GDE.CHAR_DASH)
										.replace(GDE.STRING_COLON, GDE.STRING_EMPTY).replace(GDE.STRING_DOT, GDE.STRING_EMPTY)).intValue();
							}
							catch (NumberFormatException e1) {
								points[j] = 0; //GPS coordinate does not exist "---"
							}
							break;

						default:
							try {
								if (dataSetHeader.get(VALUE_FORMAT) == null || dataSetHeader.get(VALUE_FORMAT).equals("DOUBLE"))
									points[j] = (int) (Double.parseDouble(data) * 1000.0);
								else
									points[j] = Integer.parseInt(data, 16) * 1000;
//									switch (data.length()) {
//									case 1: //A -> 0x0A
//									case 2: //A0 -> 0xA0
//									case 3: //FFF -> 0xFFF
//										points[j] = Integer.parseInt(data, 16) * 1000;
//										break;
//									case 4: //A01B
//										points[j] = DataParser.parse2Int(data.getBytes(), 0) * 1000;
//										break;
//									case 8: //A01B 12C3
//										points[j] = DataParser.parse2Int(data.getBytes(), 0) * 1000;
//										break;
//									}
							}
							catch (NumberFormatException e) {
								//points[i] = 0;  //ignore and keep last value
							}
							break;
						}
					}
					if (dataSetHeader.get(TIME_MODE_INCREMENT) != null)
						time_ms = dataSetHeader.get(TIME_MODE_INCREMENT_MS) != null ? Long.parseLong(dataSetHeader.get(TIME_MODE_INCREMENT_MS)) : 0;
					else if (dataSetHeader.get(TIME_GIVEN_INDEX) != null)
						time_ms = dataSetHeader.get(TIME_GIVEN_MS_LAST) != null ? Long.parseLong(dataSetHeader.get(TIME_GIVEN_MS)) - Long.parseLong(dataSetHeader.get(TIME_GIVEN_MS_LAST)) : 0;

					recordSet.addPoints(points, time_ms);
					
					if (dataSetHeader.get(TIME_MODE_INCREMENT) != null)	
						dataSetHeader.put(TIME_MODE_INCREMENT_MS, GDE.STRING_EMPTY + (time_ms += Long.parseLong(dataSetHeader.get(TIME_MODE_INCREMENT))));
					else if (dataSetHeader.get(TIME_GIVEN_INDEX) != null)
						dataSetHeader.put(TIME_GIVEN_MS_LAST, GDE.STRING_EMPTY + (Long.parseLong(dataSetHeader.get(TIME_GIVEN_MS)) - time_ms));

					progressLineLength = progressLineLength > line.length() ? progressLineLength : line.length();
					int progress = (int) (lineNumber * 100 / (inputFileSize / progressLineLength));
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
			}
			if (reader != null) {
				reader.close();
			}

			//correct data and start time
			for (Channel channel : Channels.getInstance().values()) {
				dataSetHeader = headers.get(channel.getNumber());
				for (RecordSet updateRecordSet : channel.values()) {
					long startTimeStamp = dataSetHeader.get(TIME_GIVEN_MS_START) != null
								? Long.parseLong(dataSetHeader.get(TIME_GIVEN_MS_START))
								: (long) (new File(filePath).lastModified() - updateRecordSet.getMaxTime_ms());
					updateRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp));
					updateRecordSet.setStartTimeStamp(startTimeStamp);
					channel.setFileDescription((new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp)).substring(0, 10) + channel.getFileDescription().substring(10));
					channel.setActiveRecordSet(updateRecordSet.getName());
					device.updateVisibilityStatus(updateRecordSet, true);
					channel.applyTemplate(updateRecordSet.getName(), true);
					updateRecordSet.setSaved(true);
				}			
			}
			
			if (GDE.isWithUi()) {
				ZeroReaderWriter.channels.switchChannel(channelConfigNumber, GDE.STRING_EMPTY);
				ZeroReaderWriter.application.getMenuToolBar().updateChannelSelector();
				activeChannel = ZeroReaderWriter.channels.getActiveChannel();
				activeChannel.switchRecordSet(recordSet.getName());
			}
		}
		catch (UnsupportedEncodingException e) {
			ZeroReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
			throw new UnsupportedEncodingException(Messages.getString(MessageIds.GDE_MSGW0010));
		}
		catch (FileNotFoundException e) {
			ZeroReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
			throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGW0011, new Object[] { filePath }));
		}
		catch (IOException e) {
			ZeroReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.GDE_MSGW0012, new Object[] { filePath }));
		}
		catch (Throwable e) {
			ZeroReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.GDE_MSGW0012, new Object[] { filePath }));
		}
		finally {
			GDE.getUiNotification().setProgress(100);
			GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
			if (GDE.isWithUi()) {
				ZeroReaderWriter.application.getMenuToolBar().updateChannelSelector();
				ZeroReaderWriter.application.getMenuToolBar().updateRecordSetSelectCombo();
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
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8")); //$NON-NLS-1$
			char decimalSeparator = Settings.getInstance().getDecimalSeparator();

			ZeroReaderWriter.df3.setGroupingUsed(false);
			ZeroReaderWriter.sb = new StringBuffer();
			RecordSet recordSet = Channels.getInstance().getActiveChannel().get(recordSetKey);
			IDevice device = DataExplorer.getInstance().getActiveDevice();
			// write device name , manufacturer, and serial port string
			ZeroReaderWriter.sb.append(device.getName()).append(separator).append(recordSet.getChannelConfigName()).append(ZeroReaderWriter.lineSep);
			writer.write(ZeroReaderWriter.sb.toString());
			ZeroReaderWriter.log.log(Level.FINE, "written header line = " + ZeroReaderWriter.sb.toString()); //$NON-NLS-1$

			ZeroReaderWriter.sb = new StringBuffer();
			ZeroReaderWriter.sb.append(Messages.getString(MessageIds.GDE_MSGT0137)).append(separator); // Spannung [V];Strom [A];Ladung [Ah];Leistung [W];Energie [Wh]";
			// write the measurements signature
			for (int i = 0; i < recordSet.size(); i++) {
				MeasurementType measurement = device.getMeasurement(recordSet.getChannelConfigNumber(), i);
				Record record = recordSet.get(i);
				ZeroReaderWriter.log.log(Level.FINEST, "append " + record.getName()); //$NON-NLS-1$
				if (isRaw) {
					if (!measurement.isCalculation()) { // only use active records for writing raw data
						ZeroReaderWriter.sb.append(record.getName()).append(" [---]").append(separator); //$NON-NLS-1$
						ZeroReaderWriter.log.log(Level.FINEST, "append " + record.getName()); //$NON-NLS-1$
					}
				}
				else {
					ZeroReaderWriter.sb.append(record.getName()).append(" [").append(record.getUnit()).append(']').append(separator); //$NON-NLS-1$
					ZeroReaderWriter.log.log(Level.FINEST, "append " + record.getName()); //$NON-NLS-1$
				}
			}
			ZeroReaderWriter.sb.deleteCharAt(ZeroReaderWriter.sb.length() - 1).append(ZeroReaderWriter.lineSep);
			ZeroReaderWriter.log.log(Level.FINER, "header line = " + ZeroReaderWriter.sb.toString()); //$NON-NLS-1$
			writer.write(ZeroReaderWriter.sb.toString());

			// write data
			long startTime = new Date(recordSet.getTime(0)).getTime();
			int recordEntries = recordSet.getRecordDataSize(true);
			int progressCycle = 0;
			GDE.getUiNotification().setProgress(progressCycle);
			for (int i = 0; i < recordEntries; i++) {
				ZeroReaderWriter.sb = new StringBuffer();
				String[] row = recordSet.getExportRow(i, true);

				// add time entry
				ZeroReaderWriter.sb.append(row[0].replace('.', decimalSeparator)).append(separator).append(GDE.STRING_BLANK);
				// add data entries
				for (int j = 0; j < recordSet.size(); j++) {
					MeasurementType measurement = device.getMeasurement(recordSet.getChannelConfigNumber(), j);
					if (isRaw) { // do not change any values
						if (!measurement.isCalculation()) if (recordSet.isRaw())
							ZeroReaderWriter.sb.append(row[j + 1].replace('.', decimalSeparator)).append(separator);
						else
							ZeroReaderWriter.sb.append(row[j + 1].replace('.', decimalSeparator)).append(separator);
					}
					else
						// translate according device and measurement unit
						ZeroReaderWriter.sb.append(row[j + 1].replace('.', decimalSeparator)).append(separator);
				}
				ZeroReaderWriter.sb.deleteCharAt(ZeroReaderWriter.sb.length() - 1).append(ZeroReaderWriter.lineSep);
				writer.write(ZeroReaderWriter.sb.toString());
				if (i % 50 == 0) GDE.getUiNotification().setProgress(((++progressCycle * 5000) / recordEntries));
				if (ZeroReaderWriter.log.isLoggable(Level.FINE)) ZeroReaderWriter.log.log(Level.FINE, "data line = " + ZeroReaderWriter.sb.toString()); //$NON-NLS-1$
			}
			ZeroReaderWriter.sb = null;
			ZeroReaderWriter.log.log(Level.TIME, "CSV file = " + filePath + " erfolgreich geschieben" //$NON-NLS-1$ //$NON-NLS-2$
					+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$

			writer.flush();
			writer.close();
			writer = null;
			//recordSet.setSaved(true);
			GDE.getUiNotification().setProgress(100);
		}
		catch (IOException e) {
			ZeroReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0006, new Object[] { GDE.FILE_ENDING_CSV, filePath, e.getMessage() }));
		}
		catch (Exception e) {
			ZeroReaderWriter.log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
		finally {
			GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
		}

	}

}
