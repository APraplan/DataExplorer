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

    Copyright (c) 2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.ardupilot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.StatisticsType;
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
 * @author brueg
 *
 */
public class ArduPilotLogReader {
	static Logger							log					= Logger.getLogger(ArduPilotLogReader.class.getName());

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();
	final static String 			pattern 		= "[^0-9].";
	
	static int 								realUsedMeasurementCount;
	static IDevice 						device = ArduPilotLogReader.application.getActiveDevice();

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
		
//    # output header info
//    xml.write("<header>\n")
//    xml.write("  <logfile>"   + escape(self.logfile) + "</logfile>\n")
//    xml.write("  <sizekb>"    + escape(repr(self.logdata.filesizeKB)) + "</sizekb>\n")
//    xml.write("  <sizelines>" + escape(repr(self.logdata.lineCount)) + "</sizelines>\n")
//    xml.write("  <duration>"  + escape(str(datetime.timedelta(seconds=self.logdata.durationSecs))) + "</duration>\n")
//    xml.write("  <vehicletype>" + escape(self.logdata.vehicleTypeString) + "</vehicletype>\n")
//    if self.logdata.vehicleType == VehicleType.Copter and self.logdata.getCopterType():
//        xml.write("  <coptertype>"  + escape(self.logdata.getCopterType()) + "</coptertype>\n")
//    xml.write("  <firmwareversion>" + escape(self.logdata.firmwareVersion) + "</firmwareversion>\n")
//    xml.write("  <firmwarehash>" + escape(self.logdata.firmwareHash) + "</firmwarehash>\n")
//    xml.write("  <hardwaretype>" + escape(self.logdata.hardwareType) + "</hardwaretype>\n")
//    xml.write("  <freemem>" + escape(repr(self.logdata.freeRAM)) + "</freemem>\n")
//    xml.write("  <skippedlines>" + escape(repr(self.logdata.skippedLines)) + "</skippedlines>\n")
//    xml.write("</header>\n")

		header.put("logfile", filePath.substring(filePath.lastIndexOf(GDE.FILE_SEPARATOR) + 1));
		File logFile = new File(filePath);
		header.put("sizekb", "" + (logFile.isFile() ? logFile.length()/1000 : 0));
		
		
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
			int lineCount = 0;
			while (((line = reader.readLine()) != null)) {
				++lineCount;
				
				if (line.startsWith("Ardu")) { 
					header.put("vehicletype", line.split(" ")[0]);	
					header.put("firmwareversion", line.split(" ")[1]);	
				}
				else if (line.startsWith("Free")) {
					header.put("freemem", line.split(" ")[2]);	
				}
			}
			header.put("sizelines", "" + lineCount);

			header.put(GDE.DEVICE_NAME, application.getActiveDevice().getName());
			
			log.log(Level.FINE, GDE.DEVICE_NAME + header.get(GDE.DEVICE_NAME));
			log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + (header.get(GDE.CHANNEL_CONFIG_NAME) != null ? header.get(GDE.CHANNEL_CONFIG_NAME) : "")); //$NON-NLS-1$
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGW0011, new Object[] { filePath }));
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.GDE_MSGW0012, new Object[] { filePath }));
		}
		finally {
			if (reader != null) reader.close();
		}

		return header;
	}

	/**
	 * read the first two line of CSV file and prepare a map with all available information
	 * @param separator
	 * @param filePath
	 * @return map with GDE.DEVICE_NAME,GDE.CSV_DATA_HEADER,[GDE.CHANNEL_CONFIG_NAME]
	 * @throws NotSupportedFileFormatException
	 * @throws IOException
	 */
	public static LinkedHashMap<String, LogFMT> getSensorsMeasurements(String separator, String filePath) throws NotSupportedFileFormatException, IOException {
		String line = GDE.STRING_STAR;
		BufferedReader reader = null; // to read the data
		LinkedHashMap<String, LogFMT> logFmts = new LinkedHashMap<String, LogFMT>();
		
		//FMT, 128, 89, FMT, BBnNZ, Type,Length,Name,Format
		//FMT, 130, 35, GPS, BIBcLLeeEe, Status,Time,NSats,HDop,Lat,Lng,RelAlt,Alt,Spd,GCrs
		//	
	  //  see "struct GPS_State" and "Log_Write_GPS":
	  //	#define GPS_LABELS "TimeUS,Status,GMS,GWk,NSats,HDop,Lat,Lng,Alt,Spd,GCrs,VZ,U"
	  //	#define GPS_FMT    "QBIHBcLLefffB"
	  //	#define GPS_UNITS  "s---SmDUmnhn-"
	  //	#define GPS_MULTS  "F---0BGGB000-"
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
			while (((line = reader.readLine()) != null)) {				
				if (line.startsWith("FMT")) { //formating specification
					String[] tmpFMT = line.split(", ");
					switch (ArduPilot.LogEntry.getLogEntry(tmpFMT[3])) { // make sure only enumerated member of LogEntry get used
					case ACC:
					case ARSP:
					case BARO:
					case CURR: 
					case CURR_CELL: 
					case ESC:
					case GPA:
					case GYR:
					case IMT:
					case IMU:
					case ISBD:
					case ISBH:
					case MAG:
					case PID:
					case QUAT:
					case GPS:
						log.log(Level.INFO, String.format("%4s\t%12s\t%s", tmpFMT[3], tmpFMT[4], tmpFMT[5]));
						logFmts.put(tmpFMT[3], new LogFMT(line, separator));
						break;
					default: //skip, since not part of implemented log entry enum
						continue;
					}
				}
			}
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGW0011, new Object[] { filePath }));
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.GDE_MSGW0012, new Object[] { filePath }));
		}
		finally {
			if (reader != null) reader.close();
		}

		return logFmts;
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
		int progressLineLength = Math.abs(ArduPilotLogReader.application.getActiveDevice().getDataBlockSize(InputTypes.FILE_IO));
		BufferedReader reader = null; // to read the data
		
		Channel activeChannel = null;
		ArduPilotLogReader.realUsedMeasurementCount = 0;

		try {
			HashMap<String, String> fileHeader = ArduPilotLogReader.getHeader(separator, filePath);
			LinkedHashMap<String, LogFMT> logEntries = ArduPilotLogReader.getSensorsMeasurements(""+separator, filePath);
			activeChannel = ArduPilotLogReader.channels.get(ArduPilotLogReader.channels.getChannelNumber(fileHeader.get(GDE.CHANNEL_CONFIG_NAME)));
			activeChannel = activeChannel == null ? ArduPilotLogReader.channels.getActiveChannel() : activeChannel;

			if (activeChannel != null) {
				GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0134) + filePath);
				GDE.getUiNotification().setProgress(0);

				// check for device name and channel or configuration in first line
				if (!ArduPilotLogReader.application.getActiveDevice().getName().equals(fileHeader.get(GDE.DEVICE_NAME))) {
					MissMatchDeviceException e = new MissMatchDeviceException(Messages.getString(MessageIds.GDE_MSGW0013, new Object[] { fileHeader.get(GDE.DEVICE_NAME) })); // mismatch device name
					log.log(Level.SEVERE, e.getMessage(), e);
					throw e;
				}

				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
				while (((line = reader.readLine()) != null) && line.matches(ArduPilotLogReader.pattern)) {
					// read until line does not starts with numbers
				}

				if (GDE.isWithUi()) {
					ArduPilotLogReader.channels.switchChannel(activeChannel.getNumber(), GDE.STRING_EMPTY);
					ArduPilotLogReader.application.getMenuToolBar().updateChannelSelector();
					activeChannel = ArduPilotLogReader.channels.getActiveChannel();
				}

				int existingNumberMeasurements = device.getDeviceConfiguration().getMeasurementNames(activeChannel.getNumber()).length;

				String recordSetName = (activeChannel.size() + 1) + recordSetNameExtend;
				List<String> tmpRecordNames = new ArrayList<>();
				List<String> tmpRecordSymbols = new ArrayList<>();
				List<String> tmpRecordUnits = new ArrayList<>();
				List<Double> tmpRecordFactors = new ArrayList<>();
				int measurementOrdinal = 0;
				for (LogFMT logEntryFormat : logEntries.values()) {
					switch (ArduPilot.LogEntry.getLogEntry(logEntryFormat.name)) { // make sure only enumerated member of LogEntry get used
					case ACC:
					case ARSP:
					case BARO:
					case CURR: 
					case CURR_CELL: 
					case ESC:
					case GPA:
					case GYR:
					case IMT:
					case IMU:
					case ISBD:
					case ISBH:
					case MAG:
					case PID:
					case QUAT:
					case GPS:
						log.log(Level.FINE, logEntryFormat.name);
						tmpRecordNames.addAll(logEntryFormat.getMeasurementNames());
						List<String> symbols = logEntryFormat.getSymbols();
						tmpRecordSymbols.addAll(symbols);
						List<String> units = logEntryFormat.getUnits();
						tmpRecordUnits.addAll(units);
						List<Double> factors = logEntryFormat.getMults();
						tmpRecordFactors.addAll(factors);
						int index = 0;
						for (String measurementName : logEntryFormat.getMeasurementNames()) {
							ArduPilotLogReader.setupMeasurement(activeChannel.getNumber(), measurementOrdinal++, measurementName, symbols.get(index), units.get(index), true, factors.get(index), true);
							++index;
						}
						break;
					default: //skip, since not part of implemented log entry enum
						break;
					}
				}
				if (tmpRecordNames.size() != existingNumberMeasurements) {
					for (int i = tmpRecordNames.size(); i < existingNumberMeasurements; i++) {
						ArduPilotLogReader.device.removeMeasurementFromChannel(activeChannel.getNumber(), ArduPilotLogReader.device.getMeasurement(activeChannel.getNumber(), tmpRecordNames.size()));
					}
				}
				
				recordSet = RecordSet.createRecordSet(recordSetName, device, activeChannel.getNumber(), tmpRecordNames.toArray(new String[1]), tmpRecordSymbols.toArray(new String[1]), tmpRecordUnits.toArray(new String[1]), device.getTimeStep_ms(), true, true, true);
				recordSetName = recordSet.getName(); // cut length

				//find GPS related records and try to assign data type
				for (int i = 0; i < recordSet.size(); i++) {
					Record record = recordSet.get(i);
					log.log(Level.FINE, String.format("%s - %s", record.getName(), record.getUnit()));
					if (record.getUnit().equals("m/s") && record.getName().toLowerCase().contains("gps_spd") )
						record.setDataType(Record.DataType.GPS_SPEED);
					else if (record.getUnit().equals("m/s") && record.getName().toLowerCase().contains("airspeed") )
						record.setDataType(Record.DataType.AIR_SPEED);
					else if (record.getUnit().equals("m") && record.getName().toLowerCase().contains("gps_alt") )
						record.setDataType(Record.DataType.GPS_ALTITUDE);
					else if (record.getUnit().contains("°") && record.getName().toLowerCase().contains("gps_lng") )
						record.setDataType(Record.DataType.GPS_LONGITUDE);
					else if (record.getUnit().contains("°") && record.getName().toLowerCase().contains("gps_lat") )
						record.setDataType(Record.DataType.GPS_LATITUDE);
				}

				//correct data and start time
				boolean isFileBasedTimeStamp = true;
				long startTimeStamp = (long) (new File(filePath).lastModified() - recordSet.getMaxTime_ms());
				recordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129)
						+ new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp));
				//preliminary start time stamp, might be replaced by first GPS time stamp
				recordSet.setStartTimeStamp(startTimeStamp);
				activeChannel.setFileDescription((new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp)).substring(0, 10) + activeChannel.getFileDescription().substring(10));

				long lastTimeStamp = 0;

				// now get all data   0; 14,780;  0,598;  1,000;  8,838;  0,002
				String[] updateRecordNames = recordSet.getRecordNames();
				int[] points = new int[updateRecordNames.length];
				if (points.length != recordSet.realSize()) {
					throw new DataInconsitsentException(String.format("mismatch recordSet size to detected point size\n%s \n%s", StringHelper.arrayToString(recordSet.getRecordNames()), StringHelper.arrayToString(updateRecordNames)));
				}
				
				boolean isNewTimeStamp = false;
				int lineNumber = 0;
				while ((line = reader.readLine()) != null) {
					++lineNumber;
					switch (ArduPilot.LogEntry.getLogEntry(line.split(", ")[0])) { // make sure only enumerated member of LogEntry get used
					case ACC:
					case ARSP:
					case BARO:
					case CURR: 
					case CURR_CELL: 
					case ESC:
					case GPA:
					case GYR:
					case IMT:
					case IMU:
					case ISBD:
					case ISBH:
					case MAG:
					case PID:
					case QUAT:
					case GPS: //GPS, 3, 594438201, 6, 4.68, 44.0290459, -77.7367640, 3.13, 91.56, 0.00, 0.00				
						log.log(Level.FINE, line);
						String sensor = line.split(", ")[0];
						LogFMT logEntry = logEntries.get(sensor);
						int index = 0;
						for (String name : logEntry.getMeasurementNames()) {
							int entryOrdinal = recordSet.get(name).getOrdinal();
							//FMT, 130, 35, GPS, BIBcLLeeEe, Status,Time,NSats,HDop,Lat,Lng,RelAlt,Alt,Spd,GCrs
							//log.log(Level.FINE, String.format("name: %s, ordinal: %d, fmt: %s, value: %s", name, entryOrdinal, logEntry.getFmt(index), line.split(", ")[index+1]));
	
							if (name.toLowerCase().contains("time")) {
								long tmpTimeStamp = Long.parseLong(line.split(", ")[1+index]) *1000;
								//log.log(Level.FINE, new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(tmpTimeStamp));
								if (isFileBasedTimeStamp && lastTimeStamp == 0) {
									startTimeStamp = lastTimeStamp = tmpTimeStamp;
									recordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129)	+ new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp));
									recordSet.setStartTimeStamp(startTimeStamp);
									activeChannel.setFileDescription((new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp)).substring(0, 10) + activeChannel.getFileDescription().substring(10));
									isNewTimeStamp = true;
								} else if (lastTimeStamp < tmpTimeStamp) {
									lastTimeStamp = tmpTimeStamp;
									isNewTimeStamp = true;
								}
								log.log(Level.FINE, new SimpleDateFormat("mm:ss.SSS").format((lastTimeStamp - startTimeStamp)/1000));
								points[entryOrdinal] = 0; //skip time entry (int) (lastTimeStamp - startTimeStamp);																
							} else {
								
								try {
									points[entryOrdinal] =  Integer.parseInt(""+ArduPilot.parseValue(logEntry.getFmt(index), line.split(", ")[1+index]));
								}
								catch (NumberFormatException e) {
									log.log(Level.WARNING, String.format("%s line %d -> NumberFormatException for %s", filePath, lineNumber, e.getMessage()));
								}								
							}
							++index;
						}
						if (isNewTimeStamp) {
							recordSet.addPoints(points, (lastTimeStamp - startTimeStamp)/1000.0); //time_ms
							isNewTimeStamp = false;
						}
						break;

					default:
						continue;
					}


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
				device.updateVisibilityStatus(recordSet, true);
				if (GDE.isWithUi()) activeChannel.switchRecordSet(recordSetName);

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
			throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGW0011, new Object[] { filePath }));
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.GDE_MSGW0012, new Object[] { filePath }));
		}
		finally {
			if (reader != null) reader.close();
			GDE.getUiNotification().setProgress(100);
			GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
			if (GDE.isWithUi()) {
				ArduPilotLogReader.application.getMenuToolBar().updateChannelSelector();
				ArduPilotLogReader.application.getMenuToolBar().updateRecordSetSelectCombo();
			}
		}

		return recordSet;
	}

	/**
	 * setup GPS related measurement, this gets called while preparing records
	 * @param dataType
	 * @param channelConfig
	 * @param measurementOrdinal
	 * @param name
	 * @param unit
	 * @param isActive
	 * @param factor
	 * @param offset
	 * @param isClearStatistics
	 */
	public static void setupMeasurement(final gde.data.Record.DataType dataType, final int channelConfig, final int measurementOrdinal, String name, String symbol, String unit, boolean isActive, double factor,
			boolean isClearStatistics) {
		++ArduPilotLogReader.realUsedMeasurementCount;
		MeasurementType gdeMeasurement = ArduPilotLogReader.device.getMeasurement(channelConfig, measurementOrdinal);
		if (!name.equals(gdeMeasurement.getName())) {
			gdeMeasurement.setName(name);
			gdeMeasurement.setStatistics(null);//delete statistics with trigger, ....
		}
		gdeMeasurement.removeProperties();
		gdeMeasurement.setSymbol(symbol);
		gdeMeasurement.setUnit(unit);
		gdeMeasurement.setActive(isActive);
		gdeMeasurement.setFactor(factor);

		switch (dataType) {
		case GPS_LATITUDE:
			PropertyType tmpPropertyType = new PropertyType();
			tmpPropertyType.setName(gde.data.Record.DataType.GPS_LATITUDE.value());
			tmpPropertyType.setType(DataTypes.STRING);
			tmpPropertyType.setValue(gde.data.Record.DataType.GPS_LATITUDE.value());
			gdeMeasurement.getProperty().add(tmpPropertyType);
			break;
		case GPS_LONGITUDE:
			tmpPropertyType = new PropertyType();
			tmpPropertyType.setName(gde.data.Record.DataType.GPS_LONGITUDE.value());
			tmpPropertyType.setType(DataTypes.STRING);
			tmpPropertyType.setValue(gde.data.Record.DataType.GPS_LONGITUDE.value());
			gdeMeasurement.getProperty().add(tmpPropertyType);
			break;
		case GPS_ALTITUDE:
			tmpPropertyType = new PropertyType();
			tmpPropertyType.setName(gde.data.Record.DataType.GPS_ALTITUDE.value());
			tmpPropertyType.setType(DataTypes.STRING);
			tmpPropertyType.setValue(gde.data.Record.DataType.GPS_ALTITUDE.value());
			gdeMeasurement.getProperty().add(tmpPropertyType);
			break;
		case GPS_AZIMUTH:
			tmpPropertyType = new PropertyType();
			tmpPropertyType.setName(gde.data.Record.DataType.GPS_AZIMUTH.value());
			tmpPropertyType.setType(DataTypes.STRING);
			tmpPropertyType.setValue(gde.data.Record.DataType.GPS_AZIMUTH.value());
			gdeMeasurement.getProperty().add(tmpPropertyType);
			break;
		case GPS_SPEED:
			tmpPropertyType = new PropertyType();
			tmpPropertyType.setName(gde.data.Record.DataType.GPS_SPEED.value());
			tmpPropertyType.setType(DataTypes.STRING);
			tmpPropertyType.setValue(gde.data.Record.DataType.GPS_SPEED.value());
			gdeMeasurement.getProperty().add(tmpPropertyType);
			break;

		default:
			break;
		}

//		if (WeatronicAdapter.properties.get(name) != null) { //scale_sync_ref_ordinal
//			String[] measurementNames = LogReader.device.getMeasurementNamesReplacements(channelConfig);
//			int syncOrdinal = -1;
//			String syncName = (String) WeatronicAdapter.properties.get(name);
//			for (int i = 0; i < measurementNames.length; i++) {
//				if (measurementNames[i].equals(syncName)) {
//					syncOrdinal = i;
//					break;
//				}
//			}
//
//			if (syncOrdinal >= 0) {
//				PropertyType tmpPropertyType = new PropertyType();
//				tmpPropertyType.setName(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
//				tmpPropertyType.setType(DataTypes.INTEGER);
//				tmpPropertyType.setValue(syncOrdinal);
//				gdeMeasurement.getProperty().add(tmpPropertyType);
//			}
//		}

		if (isClearStatistics) {
			gdeMeasurement.setStatistics(null);
		}
		else {
			StatisticsType newStatisticsType = gdeMeasurement.getStatistics();
			if (newStatisticsType == null) {
				newStatisticsType = new StatisticsType();
			}
			newStatisticsType.setMin(true);
			newStatisticsType.setMax(true);
			newStatisticsType.setAvg(true);
			newStatisticsType.setSigma(true);
			gdeMeasurement.setStatistics(newStatisticsType);
		}
	}

	/**
	 * setup measurement, this gets called while preparing records
	 * @param channelConfig
	 * @param measurementOrdinal
	 * @param measurement
	 * @param isClearStatistics
	 */
	private static void setupMeasurement(final int channelConfig, final int measurementOrdinal, String name, String symbol, String unit, boolean isActive, double factor,
			boolean isClearStatistics) {
		++ArduPilotLogReader.realUsedMeasurementCount;
		MeasurementType gdeMeasurement = ArduPilotLogReader.device.getMeasurement(channelConfig, measurementOrdinal);
		if (!name.equals(gdeMeasurement.getName())) {
			gdeMeasurement.setName(name.length() == 0 ? ("???_" + measurementOrdinal) : name);
			gdeMeasurement.setStatistics(null);//delete statistics with trigger, ....
		}
		gdeMeasurement.removeProperties();
		gdeMeasurement.setSymbol(symbol);
		gdeMeasurement.setUnit(unit);
		gdeMeasurement.setActive(true);
		if (factor != 0.0 && factor != 1.0)
		gdeMeasurement.setFactor(factor);

//		if (WeatronicAdapter.properties.get(measurement.getName()) != null) { //scale_sync_ref_ordinal
//			String[] measurementNames = LogReader.device.getMeasurementNamesReplacements(channelConfig);
//			int syncOrdinal = -1;
//			String syncName = (String) WeatronicAdapter.properties.get(measurement.getName());
//			for (int i = 0; i < measurementNames.length; i++) {
//				if (measurementNames[i].equals(syncName)) {
//					syncOrdinal = i;
//					break;
//				}
//			}
//
//			if (syncOrdinal >= 0) {
//				PropertyType tmpPropertyType = new PropertyType();
//				tmpPropertyType.setName(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
//				tmpPropertyType.setType(DataTypes.INTEGER);
//				tmpPropertyType.setValue(syncOrdinal);
//				gdeMeasurement.getProperty().add(tmpPropertyType);
//			}
//		}

		if (isClearStatistics) {
			gdeMeasurement.setStatistics(null);
		}
		else {
			StatisticsType newStatisticsType = gdeMeasurement.getStatistics();
			if (newStatisticsType == null) {
				newStatisticsType = new StatisticsType();
			}
			newStatisticsType.setMin(true);
			newStatisticsType.setMax(true);
			newStatisticsType.setAvg(true);
			newStatisticsType.setSigma(true);
			gdeMeasurement.setStatistics(newStatisticsType);
		}
	}

}
