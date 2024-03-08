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
package gde.device.powerbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.InputTypes;
import gde.device.MeasurementType;
import gde.device.powerbox.TelemetryData.TelemetrySensor;
import gde.device.powerbox.TelemetryData.TelemetryVar;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.exception.MissMatchDeviceException;
import gde.exception.NotSupportedFileFormatException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;

/**
 * Class to read Jeti sensor sensorData using the TelemetryData class provided by Martin Falticko
 * @author Winfried Brügmann
 */
public class CoreDataReader {
	static Logger													log							= Logger.getLogger(CoreDataReader.class.getName());

	static String													lineSep					= GDE.LINE_SEPARATOR;
	static DecimalFormat									df3							= new DecimalFormat("0.000");												//$NON-NLS-1$
	static StringBuffer										sb;

	final static DataExplorer							application			= DataExplorer.getInstance();
	final static Channels									channels				= Channels.getInstance();

	final static TreeSet<TelemetrySensor>	sensorData			= new TreeSet<TelemetrySensor>();
	static long														startTimeStamp	= 0;

	/**
	 * read the selected CSV file and parse
	 * @param filePath
	 * @param device
	 * @param recordNameExtend
	 * @param channelConfigNumber
	 * @param isRaw
	 * @return record set created
	 * @throws NotSupportedFileFormatException
	 * @throws MissMatchDeviceException
	 * @throws IOException
	 * @throws DataInconsitsentException
	 * @throws DataTypeException
	 */
	public static RecordSet read(String filePath, CoreAdapter device, String recordNameExtend, Integer channelConfigNumber, boolean isRaw)
			throws NotSupportedFileFormatException, IOException, DataInconsitsentException, DataTypeException {
		RecordSet recordSet = null;
		Channel activeChannel = null;
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new File(filePath).lastModified()); //$NON-NLS-1$
		boolean isOutdated = false;
		int lineNumber = 0;
		int activeChannelConfigNumber = 1; // at least each device needs to have one channelConfig to place record sets
		String recordSetNameExtend = device.getRecordSetStemNameReplacement();
		double time_ms = 0;

		String appVer = ""; //$NON-NLS-1$
		String tcFwVer = ""; //$NON-NLS-1$
		String scFwVer = ""; //$NON-NLS-1$
		String modelName = ""; //$NON-NLS-1$
		boolean isSensorTable = false;
		boolean isStartLogEntries = false;
		long startTime_ms = 0;
		int[] points = null;
		long lastEntryTime_ms = 0;

		try {
			if (channelConfigNumber == null)
				activeChannel = CoreDataReader.channels.getActiveChannel();
			else
				activeChannel = CoreDataReader.channels.get(channelConfigNumber);

			if (activeChannel != null) {
				GDE.getUiNotification().setStatusMessage(gde.messages.Messages.getString(gde.messages.MessageIds.GDE_MSGT0594) + filePath);
				GDE.getUiNotification().setProgress(0);
				activeChannelConfigNumber = activeChannel.getNumber();

				if (GDE.isWithUi()) {
					CoreDataReader.channels.switchChannel(activeChannel.getNumber(), GDE.STRING_EMPTY);
					CoreDataReader.application.getMenuToolBar().updateChannelSelector();
					activeChannel = CoreDataReader.channels.getActiveChannel();
				}
				String recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$

				int measurementSize = device.getNumberOfMeasurements(activeChannelConfigNumber);
				int dataBlockSize = device.getDataBlockSize(InputTypes.FILE_IO); // measurements size must not match sensorData block size, there are some measurements which are result of calculation
				CoreDataReader.log.log(Level.FINE, "measurementSize = " + measurementSize + "; dataBlockSize = " + dataBlockSize); //$NON-NLS-1$ //$NON-NLS-2$
				if (measurementSize < Math.abs(dataBlockSize))
					throw new DevicePropertiesInconsistenceException(gde.messages.Messages.getString(gde.messages.MessageIds.GDE_MSGE0041, new String[] { filePath }));

				CoreDataReader.sensorData.clear();
				int line = 0;
				try {
					//find all depending log files
					String searchPath = filePath.substring(0, filePath.lastIndexOf(GDE.FILE_SEPARATOR));
					String filter = filePath.substring(filePath.lastIndexOf(GDE.FILE_SEPARATOR) + 1, filePath.lastIndexOf(GDE.CHAR_UNDER_BAR) - 3);
					List<File> logFiles = FileUtils.getFileListing(new File(searchPath), 0, filter);
					int numberLogFiles = logFiles.size();
					int numberProcessedLogFiles = 1;
					for (File logFile : logFiles) {
						log.log(Level.INFO, logFile.getAbsolutePath());
						line = 0;

						FileInputStream fis = new FileInputStream(logFile);
						InputStreamReader in = new InputStreamReader(fis, "ISO-8859-1"); //$NON-NLS-1$
						BufferedReader br = new BufferedReader(in);

						String strLine;
						while ((strLine = br.readLine()) != null) {
							line++;
							strLine = strLine.trim();
							/*First character - commentar?
							#AppVer=1.95
							#TCfwVer=1.21
							#SCfwVer=1.4
							#Model=X-Perience Pro@0x5F33C7FF
							#SensorsTable
							*/
							if (strLine.length() == 0 || strLine.startsWith("#")) { //$NON-NLS-1$
								if (strLine.startsWith("#AppVer"))
									appVer = strLine.substring(8).trim();
								else if (strLine.startsWith("#TCfwVer"))
									tcFwVer = strLine.substring(9).trim();
								else if (strLine.startsWith("#SCfwVer"))
									scFwVer = strLine.substring(9).trim();
								else if (strLine.startsWith("#Model=")) {
									modelName = strLine.substring(7, strLine.indexOf('@')).trim();
								}
								else if (strLine.startsWith("#SensorsTable")) {
									isSensorTable = true;
									isStartLogEntries = false;
									log.log(Level.INFO, "SensorsTable");
								}
								else if (strLine.startsWith("#Time")) {
									if (isSensorTable) {
										//all sensors and variables evaluated, recordSet can be build
										recordSetNameExtend = device.getRecordSetStateNameReplacement(1); // state name
										if (recordNameExtend.length() > 0) {
											recordSetNameExtend = recordSetNameExtend + GDE.STRING_BLANK + GDE.STRING_LEFT_BRACKET + recordNameExtend + GDE.STRING_RIGHT_BRACKET;
										}

										//prepare new record set now
										recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$
										//adapt record names and units to current telemetry sensors
										int index = 0;
										Vector<String> vecRecordNames = new Vector<String>();
										Map<Integer, Record.DataType> mapRecordType = new HashMap<Integer, Record.DataType>();
										//add record exclude Tx
										for (TelemetrySensor telemetrySensor : CoreDataReader.sensorData) {
											boolean isActualgps = false;
											for (TelemetryData.TelemetryVar dataVar : telemetrySensor.getVariables()) {
												String newRecordName = dataVar.getName().trim();
												while (vecRecordNames.contains(newRecordName)) { //check for duplicated record names and update to make unique
													newRecordName = String.format("%s %s", newRecordName, telemetrySensor.getName());
													dataVar.setName(newRecordName);
												}
												vecRecordNames.add(newRecordName);
												if (CoreDataReader.log.isLoggable(Level.INFO)) CoreDataReader.log.log(Level.INFO, String.format("add new record = %s [%s]", newRecordName, dataVar.getUnit()));

												if (dataVar.getDataType() == TelemetryData.T_GPS) {
													if (dataVar.getName().toLowerCase().startsWith("lon") || dataVar.getName().toLowerCase().startsWith("län")) {
														isActualgps = true;
														mapRecordType.put(index, Record.DataType.GPS_LATITUDE);
														dataVar.unit = "°";
													}
													else if (dataVar.getName().toLowerCase().startsWith("lat") || dataVar.getName().toLowerCase().startsWith("breit")) {
														isActualgps = true;
														mapRecordType.put(index, Record.DataType.GPS_LONGITUDE);
														dataVar.unit = "°";
													}
												}
												else if (isActualgps && dataVar.getUnit().contains("°") && dataVar.getParam() == 10) {
													mapRecordType.put(index, Record.DataType.GPS_AZIMUTH);
												}
												else if ((dataVar.getName().toLowerCase().endsWith("hoehe") || dataVar.getName().toLowerCase().contains("höhe") || dataVar.getName().toLowerCase().contains("height")
														|| dataVar.getName().toLowerCase().contains("alt")) && dataVar.getUnit().equals("m")) //dataVar.getParam()==4
												{
													mapRecordType.put(index, Record.DataType.GPS_ALTITUDE);
												}
												else if ((dataVar.getName().toLowerCase().contains("speed") || dataVar.getName().toLowerCase().contains("geschw"))
														&& (dataVar.getUnit().equals("km/h") || dataVar.getUnit().equals("kmh") || dataVar.getUnit().equals("kph") || dataVar.getUnit().equals("m/s"))) {
													mapRecordType.put(index, Record.DataType.GPS_SPEED);
												}
												if (CoreDataReader.log.isLoggable(Level.FINE)) CoreDataReader.log.log(Level.FINE, "param = " + dataVar.getParam());
												device.setMeasurementName(activeChannelConfigNumber, index, dataVar.getName());
												device.setMeasurementUnit(activeChannelConfigNumber, index, dataVar.getUnit());
												++index;
											}
										}
										//build up the record set with variable number of records just fit the sensor sensorData
										String[] recordNames = vecRecordNames.toArray(new String[0]);
										String[] recordSymbols = new String[recordNames.length];
										String[] recordUnits = new String[recordNames.length];
										for (int i = 0; i < recordNames.length; i++) {
											MeasurementType measurement = device.getMeasurement(activeChannelConfigNumber, i);
											recordSymbols[i] = measurement.getSymbol();
											recordUnits[i] = measurement.getUnit();
										}
										recordSet = RecordSet.createRecordSet(recordSetName, device, activeChannelConfigNumber, recordNames, recordSymbols, recordUnits, device.getTimeStep_ms(), isRaw, true, true);
										//set record sensorData type which are not default
										for (Entry<Integer, Record.DataType> entry : mapRecordType.entrySet()) {
											recordSet.get(entry.getKey().intValue()).setDataType(entry.getValue());
										}
										recordSetName = recordSet.getName(); // cut/correct length of recordSetName
										//correct time if needed
										try {
											isOutdated = Integer.parseInt(dateTime.split(GDE.STRING_DASH)[0]) <= 2000;
										}
										catch (Exception e) {
											// ignore and state as not outdated
										}
										activeChannel.put(recordSetName, recordSet);
										if (CoreDataReader.log.isLoggable(Level.FINE)) CoreDataReader.log.log(Level.FINE, device.getNumberOfMeasurements(activeChannelConfigNumber) + " - " + recordSet.size());
										points = new int[recordNames.length];
									}

									try {
										startTime_ms = Long.parseLong(strLine.substring(8).trim(), 16) * 1000;
										isSensorTable = false;
										isStartLogEntries = true;
										//log.log(Level.INFO, "Time = " + StringHelper.getFormatedTime("YYYY-MM-dd hh:mm:ss.SSS", startTime_ms));
									}
									catch (Exception e) {
										log.log(Level.SEVERE, logFile.getName() + " line # " + line + " failed parsing " + strLine.substring(8));
									}
								}
								continue;
							}

							if (isSensorTable) //patch time stamp 0 to enable adding sensor
								strLine = "000000000;" + strLine;
							else if (isStartLogEntries && strLine.startsWith(":")) {
								try {
									long entryTime_ms = startTime_ms + Integer.parseInt(strLine.substring(1, 3)) * 1000;
									strLine = String.format("%09d;%s", entryTime_ms, strLine.substring(4));
									if (entryTime_ms > lastEntryTime_ms) {
										//log.log(Level.INFO, "addPoints");
										recordSet.addPoints(points, time_ms);
										if (lastEntryTime_ms != 0) time_ms += entryTime_ms - lastEntryTime_ms;
										lastEntryTime_ms = entryTime_ms;
									}
								}
								catch (Exception e) {
									log.log(Level.SEVERE, logFile.getName() + " line # " + line + " failed parsing " + strLine);
								}

							}
							//log.log(Level.INFO, strLine);

							List<String> array = new ArrayList<String>();
							array.addAll(Arrays.asList(strLine.replace("|", ";").split(";")));
							if (array != null && array.size() > 0) {
								if (array.size() == 4) { //only sensors/variables may have 4 entries in array while missing a unit 
									//log.log(Level.WARNING, String.format("Sensor sensorData unknown! - %s", array));
									array.add(GDE.STRING_EMPTY);
									continue;
								}
								//if (!array.get(0).equals("000000000")) //print sensor measurements
								//	log.log(Level.INFO, array.toString());
								parseLineParams(array.toArray(new String[5]), recordSet, points, logFile.getName(), line);
							}
						}
						in.close();

						int progress = (int) (numberProcessedLogFiles++ * 100 / numberLogFiles);
						if (progress <= 90 && progress > GDE.getUiNotification().getProgressPercentage()) {
							GDE.getUiNotification().setProgress(progress);
						}
					}
				}
				catch (Exception e) {//Catch exception if any
					TelemetryData.log.log(Level.SEVERE, e.getMessage(), e);
					DataExplorer.getInstance().openMessageDialogAsync(gde.messages.Messages.getString(MessageIds.GDE_MSGE2952, new String[] { filePath, String.valueOf(line) }));
				}

				GDE.getUiNotification().setProgress(99);

				activeChannel.setActiveRecordSet(recordSetName);
				activeChannel.applyTemplate(recordSetName, true);
				device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
				if (!isOutdated) {
					long startTimeStamp = (long) (new File(filePath).lastModified() - activeChannel.get(recordSetName).getMaxTime_ms());
					activeChannel.get(recordSetName).setRecordSetDescription(
							device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(gde.messages.MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp));
					activeChannel.get(recordSetName).setStartTimeStamp(startTimeStamp);
					activeChannel.setFileDescription(dateTime.substring(0, 10) + activeChannel.getFileDescription().substring(10));
				}
				else {
					activeChannel.get(recordSetName).setRecordSetDescription(
							device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(gde.messages.MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new Date())); //$NON-NLS-1$
				}
				//write filename after import to record description
				activeChannel.get(recordSetName).descriptionAppendFilename(filePath.substring(filePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX) + 1));
				if (modelName.length() > 2) activeChannel.get(recordSetName)
						.setRecordSetDescription(Messages.getString(gde.device.powerbox.MessageIds.GDE_MSGT2964, new String[] { activeChannel.get(recordSetName).getRecordSetDescription(), modelName }));

				activeChannel.get(recordSetName).setRecordSetDescription(activeChannel.get(recordSetName).getRecordSetDescription() + GDE.STRING_NEW_LINE +
						"#AppVer=" + appVer + " #TCfwVer=" + tcFwVer + " #SCfwVer=" + scFwVer);
				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
				activeChannel.get(recordSetName).updateVisibleAndDisplayableRecordsForTable();
				GDE.getUiNotification().setProgress(100);
				if (GDE.isWithUi()) activeChannel.switchRecordSet(recordSetName);
			}
		}
		catch (Exception e) {
			// check if previous records are available and needs to be displayed
			if (activeChannel != null && activeChannel.size() > 0) {
				String recordSetName = activeChannel.getFirstRecordSetName();
				activeChannel.setActiveRecordSet(recordSetName);
				device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
				GDE.getUiNotification().setProgress(100);
				if (GDE.isWithUi()) activeChannel.switchRecordSet(recordSetName);
			}
			// now display the error message
			String msg = filePath + GDE.STRING_MESSAGE_CONCAT + gde.messages.Messages.getString(gde.messages.MessageIds.GDE_MSGE0045, new Object[] { e.getMessage(), lineNumber });
			CoreDataReader.log.log(Level.WARNING, msg, e);
			CoreDataReader.application.openMessageDialog(msg);
		}
		//		finally {
		//			if (application.getStatusBar() != null) {
		//				application.setStatusMessage(GDE.STRING_EMPTY);
		//			}
		//		}

		return recordSet;
	}

	/**
	 * parse string array into parameter
	 */
	static void parseLineParams(String params[], RecordSet recordSet, int[] points, String fileName, int lineNumber) {
		final int ST_TIME = 0;
		final int ST_DEVICE_ID = 1;
		final int ST_PARAM_NUM = 2;
		final int ST_START_INDEX = 3;
		final int ST_DECIMALS = 4;
		final int ST_VALUES = 5;
		final int ST_SENSOR = 6;
		final int ST_LABEL = 7;
		final int ST_UNIT = 8;

		int state = ST_TIME;
		long timestamp = 0;
		long deviceId = 0;
		int paramId = 0;
		int dataType = 0;
		int decimals = 0;
		String sensor = ""; //$NON-NLS-1$
		String label = ""; //$NON-NLS-1$
		String unit = ""; //$NON-NLS-1$
		if (params == null) {
			return;
		}
		for (String param : params) {
			try {
				switch (state) {
				case ST_TIME:
					timestamp = Long.parseLong(param);
					state = ST_DEVICE_ID;
					break;
				case ST_DEVICE_ID:
					//device id sometimes ;42020    1; LiVa or | 0   0| Alarm: Cap..
					try {
						deviceId = Long.parseLong(!(param.startsWith(" ") && param.endsWith("0")) ? param.replace(' ', '0') : param);
					}
					catch (Exception e) {
						log.log(Level.WARNING, "skip | param = " + param);
					}
					state = ST_PARAM_NUM;
					break;
				case ST_PARAM_NUM:
					if (param != null) {
						paramId = Integer.parseInt(param.trim());
						if (timestamp == 0) {
							state = ST_SENSOR;
						}
						else {
							state = ST_START_INDEX;
						}
					}
					break;
				case ST_SENSOR:
					sensor = param;
					//Insert a new sensor and exit the queue
					if (timestamp == 0 && paramId == 0) {
						if (TelemetryData.log.isLoggable(Level.INFO)) TelemetryData.log.log(Level.INFO, "adding sensor " + sensor);
						TelemetrySensor telemetrySensor = new TelemetrySensor(deviceId, sensor);
						CoreDataReader.sensorData.add(telemetrySensor);
					}
					//call the parameter label
					state = ST_LABEL;
					break;
				case ST_LABEL:
					label = param.trim();
					//call the parameter unit
					state = ST_UNIT;
					break;
				case ST_UNIT:
					unit = param;
					//call the parameter decimals
					state = ST_DECIMALS;
					break;
				case ST_DECIMALS:
					decimals = Integer.parseInt(param);
					TelemetryVar var = new TelemetryVar(paramId, label, unit, decimals);
					if (label.toLowerCase().startsWith("lon") || label.toLowerCase().startsWith("län") || label.toLowerCase().startsWith("lat") || label.toLowerCase().startsWith("brei"))
						var.setDataType( TelemetryData.T_GPS);
					TelemetrySensor s = CoreDataReader.getSensor(deviceId);
					if (s != null) {
						if (TelemetryData.log.isLoggable(Level.FINER)) TelemetryData.log.log(Level.FINER, String.format("%s %03d add variable %s[%s] ID=%d", s.getName(), deviceId, var.name, unit, paramId));
						s.addVariable(var);
					}
					//no function
					return;
				case ST_START_INDEX:
					state = ST_VALUES;
					break;
				case ST_VALUES:
					int val = 0;
					try {
						if (param.startsWith("0x")) {
							switch (param.length()) {
							case 10: //GPS coordinate
								//log.log(Level.INFO, paramId + " param = " + param);
								dataType = TelemetryData.T_GPS;
								val = Integer.parseInt(param.substring(2), 16) / 10; //make GPS coordinates homogeneous for GDE
								break;

							case 6: //GPS other value
							default:
								dataType = TelemetryData.T_DATA16;
								val = Integer.valueOf(param.substring(2), 16).shortValue();
								break;
							}
						}
						else {
							//paramId += 1;				//skip for first GOE coordinate
							state = ST_VALUES;
							break;
						}
						TelemetryVar sensorVar = CoreDataReader.getSensor(deviceId).getVar(paramId);
						if (sensorVar != null) {
							int pontsIndex = recordSet.get(sensorVar.name).getOrdinal();
							//log.log(Level.INFO, String.format("TelemetryData: deviceId=%03d, paramId=%02d, value=%d, decimals=%d timeStamp=%d ordinal=%d", deviceId, paramId, val, sensorVar.decimals, timestamp, pontsIndex));	
							if (dataType == TelemetryData.T_GPS)
								points[pontsIndex] = val;
							else
								points[pontsIndex] = val * 1000 / (int) Math.pow(10, sensorVar.decimals);
						}
					}
					catch (Exception e) {
						log.log(Level.SEVERE, fileName + " line # " + lineNumber + " failed parsing " + param);
					}
					if (startTimeStamp == 0) {
						if (TelemetryData.log.isLoggable(Level.FINER)) TelemetryData.log.log(Level.FINER, "set startTimeStamp = " + timestamp);
						startTimeStamp = timestamp;
					}
					paramId += dataType == TelemetryData.T_GPS ? 2 : 1;
					state = ST_VALUES;
					break;
				}
			}
			catch (NumberFormatException e) {
				log.log(Level.SEVERE, fileName + " line # " + lineNumber + " failed parsing " + param);
			}
		}
	}

	/**
	 * @return the sensor according to the given ID
	 */
	private static TelemetrySensor getSensor(long id) {
		for (TelemetrySensor s : CoreDataReader.sensorData) {
			if (s.id == id) {
				return s;
			}
		}
		return null;
	}

}
