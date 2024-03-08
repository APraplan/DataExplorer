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
package gde.device.jeti;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
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
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.exception.MissMatchDeviceException;
import gde.exception.NotSupportedFileFormatException;
import gde.io.CSVSerialDataReaderWriter;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.TimeLine;
import cz.vutbr.fit.gja.proj.utils.TelemetryData;
import cz.vutbr.fit.gja.proj.utils.TelemetryData.TelemetrySensor;

/**
 * Class to read Jeti sensor data using the TelemetryData class provided by Martin Falticko
 * @author Winfried Brügmann
 */
public class JetiDataReader {
	static Logger										log					= Logger.getLogger(CSVSerialDataReaderWriter.class.getName());

	static String										lineSep			= GDE.LINE_SEPARATOR;
	static DecimalFormat						df3					= new DecimalFormat("0.000");																	//$NON-NLS-1$
	static StringBuffer							sb;

	final static DataExplorer				application	= DataExplorer.getInstance();
	final static Channels						channels		= Channels.getInstance();

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
	public static RecordSet read(String filePath, JetiAdapter device, String recordNameExtend, Integer channelConfigNumber, boolean isRaw) throws NotSupportedFileFormatException, IOException,
			DataInconsitsentException, DataTypeException {
		RecordSet recordSet = null;
		Channel activeChannel = null;
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new File(filePath).lastModified()); //$NON-NLS-1$
		boolean isOutdated = false, isStartIndexGPS = false;
		boolean isActualTime = false, isActualDate = false;
		int ordinalTimeRecord = -1, ordinalDateRecord = -1;
		int lineNumber = 0;
		int activeChannelConfigNumber = 1; // at least each device needs to have one channelConfig to place record sets
		String recordSetNameExtend = device.getRecordSetStemNameReplacement();
		double time_ms = 0, timeStep_ms = 0;

		try {
			if (channelConfigNumber == null)
				activeChannel = JetiDataReader.channels.getActiveChannel();
			else
				activeChannel = JetiDataReader.channels.get(channelConfigNumber);

			if (activeChannel != null) {
				GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0594) + filePath);
				GDE.getUiNotification().setProgress(0);
				activeChannelConfigNumber = activeChannel.getNumber();

				if (GDE.isWithUi()) {
					JetiDataReader.channels.switchChannel(activeChannel.getNumber(), GDE.STRING_EMPTY);
					JetiDataReader.application.getMenuToolBar().updateChannelSelector();
					activeChannel = JetiDataReader.channels.getActiveChannel();
				}
				String recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$

				//now get all data   $1;1;0; 14780;  598;  1000;  8838;  0002
				//$recordSetNumber;stateNumber;timeStepSeconds;firstIntValue;secondIntValue;.....;checkSumIntValue;
				int measurementSize = device.getNumberOfMeasurements(activeChannelConfigNumber);
				int dataBlockSize = device.getDataBlockSize(InputTypes.FILE_IO); // measurements size must not match data block size, there are some measurements which are result of calculation
				JetiDataReader.log.log(Level.FINE, "measurementSize = " + measurementSize + "; dataBlockSize = " + dataBlockSize); //$NON-NLS-1$ //$NON-NLS-2$
				if (measurementSize < Math.abs(dataBlockSize)) throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0041, new String[] { filePath }));
				StringBuilder reverseChannelStatistics = new StringBuilder();
				
				TelemetryData data = new TelemetryData();
				if (data.loadData(filePath)) {
					TreeSet<TelemetrySensor> recordSetData = data.getData();
					if (JetiDataReader.log.isLoggable(Level.FINE)) JetiDataReader.log.log(Level.FINE, "Modell name = " + data.getModelName());
					if (JetiDataReader.application.getMenuBar() != null) device.matchModelNameObjectKey(data.getModelName());

					//find best fit number of values to time step
					int maxHit = 0, numValues = 0;
					Map<Integer, Integer> valuesMap = new HashMap<Integer, Integer>();
					for (TelemetrySensor telemetrySensor : recordSetData) {
						boolean isTimeStepEvaluated = false;
						//System.out.println(telemetrySensor.getName() + " - ");
						for (TelemetryData.TelemetryVar dataVar : telemetrySensor.getVariables()) {
							int dataItemsSize = dataVar.getItems().size();
							//System.out.println(dataVar.getName() + " data items size = " + dataItemsSize);
							if (dataVar.getItems().size() > 3) { //Alarm has 3 values start time, alarm time , end time
								if (valuesMap.containsKey(dataItemsSize) && dataItemsSize > 1) {
									valuesMap.put(dataItemsSize, valuesMap.get(dataItemsSize) + 1);
									maxHit = Math.max(maxHit, valuesMap.get(dataItemsSize));
									if (!isTimeStepEvaluated && dataVar.getTimeSteps().size() > 0) {
										if (telemetrySensor.getName().length() <= 8 )
											reverseChannelStatistics.append(String.format(Locale.getDefault(), "%s\t\tmin %.3fsec avg %.3fsec max %.3fsec at %s", telemetrySensor.getName(), dataVar.getTimeSteps().getMinValue()/1000., dataVar.getTimeSteps().getAvgValue()/1000., dataVar.getTimeSteps().getMaxValue()/1000., TimeLine.getFomatedTimeWithUnit(dataVar.getTimeSteps().getMaxValueTimeStamp()))).append(GDE.CHAR_NEW_LINE);
										else 
											reverseChannelStatistics.append(String.format(Locale.getDefault(), "%s\tmin %.3fsec avg %.3fsec max %.3fsec at %s", telemetrySensor.getName(), dataVar.getTimeSteps().getMinValue()/1000., dataVar.getTimeSteps().getAvgValue()/1000., dataVar.getTimeSteps().getMaxValue()/1000., TimeLine.getFomatedTimeWithUnit(dataVar.getTimeSteps().getMaxValueTimeStamp()))).append(GDE.CHAR_NEW_LINE);
										JetiDataReader.log.log(Level.INFO, String.format(Locale.getDefault(), "%10s: min %.3fsec avg %.3fsec max %.3fsec at %s", telemetrySensor.getName(), dataVar.getTimeSteps().getMinValue()/1000., dataVar.getTimeSteps().getAvgValue()/1000., dataVar.getTimeSteps().getMaxValue()/1000., TimeLine.getFomatedTimeWithUnit(dataVar.getTimeSteps().getMaxValueTimeStamp())));
										isTimeStepEvaluated = true;
									}
								}
								else
									valuesMap.put(dataItemsSize, 1);
							}
							if (JetiDataReader.log.isLoggable(Level.FINE)) JetiDataReader.log.log(Level.FINE, String.format("%10s [%s]", dataVar.getName(), dataVar.getUnit()));
						}
					}
					Integer[] occurrence = valuesMap.values().toArray(new Integer[1]);
					Arrays.sort(occurrence);
					for (Integer key : valuesMap.keySet()) {
						if (valuesMap.get(key) == occurrence[occurrence.length - 1]) {
							numValues = key;
							break;
						}
					}
					if (JetiDataReader.log.isLoggable(Level.FINE)) JetiDataReader.log.log(Level.FINE, "best fit # values = " + numValues);
					timeStep_ms = data.getMaxTimestamp() * 1000.0 / numValues;
					if (JetiDataReader.log.isLoggable(Level.FINE)) JetiDataReader.log.log(Level.FINE, String.format("best fit timeStep_ms = %.1f", timeStep_ms));

					try {
						recordSetNameExtend = device.getRecordSetStateNameReplacement(1); // state name
						if (recordNameExtend.length() > 0) {
							recordSetNameExtend = recordSetNameExtend + GDE.STRING_BLANK + GDE.STRING_LEFT_BRACKET + recordNameExtend + GDE.STRING_RIGHT_BRACKET;
						}
					}
					catch (Exception e) {
						throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0044, new Object[] { 1, filePath, device.getPropertiesFileName() }));
					}

					//prepare new record set now
					recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$

					//adapt record names and units to current telemetry sensors
					int index = 0;
					Vector<String> vecRecordNames = new Vector<String>();
					Map<Integer, Record.DataType> mapRecordType = new HashMap<Integer, Record.DataType>();
					//add record exclude Tx
					for (TelemetrySensor telemetrySensor : recordSetData) {
						if (telemetrySensor.getId() != 0) {
							boolean isActualgps = false;
							for (TelemetryData.TelemetryVar dataVar : telemetrySensor.getVariables()) {
								String newRecordName = dataVar.getName().trim();
								if (telemetrySensor.getName().startsWith("Rx")) {
									newRecordName = String.format("%s %s", newRecordName.split(" Rx")[0], telemetrySensor.getName());
								}
								while (vecRecordNames.contains(newRecordName)) { //check for duplicated record names and update to make unique
									newRecordName = String.format("%s %s", newRecordName, telemetrySensor.getName());
								}
								vecRecordNames.add(newRecordName);
								if (JetiDataReader.log.isLoggable(Level.FINE)) 
									JetiDataReader.log.log(Level.FINE, String.format("add new record = %s [%s]", newRecordName, dataVar.getUnit()));

								device.setMeasurementName(activeChannelConfigNumber, index, dataVar.getName());
								device.setMeasurementUnit(activeChannelConfigNumber, index, dataVar.getUnit());
								if (dataVar.getType() == (TelemetryData.T_GPS) && (dataVar.getDecimals() & 1) == 0) {
									isActualgps = true;
									mapRecordType.put(index, Record.DataType.GPS_LATITUDE);
								}
								else if (dataVar.getType() == (TelemetryData.T_GPS) && (dataVar.getDecimals() & 1) == 1) {
									isActualgps = true;
									mapRecordType.put(index, Record.DataType.GPS_LONGITUDE);
								}
								else if (dataVar.getType() == (TelemetryData.T_TIME) && (dataVar.getDecimals() & 1) == 0) {
									mapRecordType.put(index, Record.DataType.GPS_TIME);
									isActualTime = true;
									ordinalTimeRecord = index;
								}
								else if (dataVar.getType() == (TelemetryData.T_TIME) && (dataVar.getDecimals() & 1) == 1) {
									mapRecordType.put(index, Record.DataType.DATE_TIME);
									isActualDate = true;
									ordinalDateRecord = index;
								}
								else if (isActualgps && dataVar.getUnit().contains("°") && dataVar.getParam() == 10) {
									mapRecordType.put(index, Record.DataType.GPS_AZIMUTH);
								}
								else if ((dataVar.getName().toLowerCase().contains("hoehe") || dataVar.getName().toLowerCase().contains("höhe") || dataVar.getName().toLowerCase().contains("height") || dataVar
										.getName().toLowerCase().contains("alt"))
										&& dataVar.getUnit().equals("m")) //dataVar.getParam()==4
								{
									mapRecordType.put(index, Record.DataType.GPS_ALTITUDE);
								}
								else if ((dataVar.getName().toLowerCase().contains("speed") || dataVar.getName().toLowerCase().contains("geschw")) && (dataVar.getUnit().equals("km/h") || dataVar.getUnit().equals("kmh") || dataVar.getUnit().equals("kph") || dataVar.getUnit().equals("m/s"))) {
									mapRecordType.put(index, Record.DataType.GPS_SPEED);
								}
								if (JetiDataReader.log.isLoggable(Level.FINE)) JetiDataReader.log.log(Level.FINE, "param = " + dataVar.getParam());
								++index;
							}
						}
					}
					//append Tx, alarms and events
					for (TelemetrySensor telemetrySensor : recordSetData) {
						if (telemetrySensor.getId() == 0) {
							for (TelemetryData.TelemetryVar dataVar : telemetrySensor.getVariables()) {
								String newRecordName = dataVar.getName().trim();
								while (vecRecordNames.contains(newRecordName)) { //check for duplicated record names and update to make unique
									newRecordName = newRecordName + "'";
								}
								vecRecordNames.add(newRecordName);
								if (JetiDataReader.log.isLoggable(Level.FINE)) JetiDataReader.log.log(Level.FINE, "add new record name = " + newRecordName);

								device.setMeasurementName(activeChannelConfigNumber, index, dataVar.getName());
								device.setMeasurementUnit(activeChannelConfigNumber, index, dataVar.getUnit());

								++index;
							}
						}
					}

					//build up the record set with variable number of records just fit the sensor data
					String[] recordNames = vecRecordNames.toArray(new String[0]);
					String[] recordSymbols = new String[recordNames.length];
					String[] recordUnits = new String[recordNames.length];
					for (int i = 0; i < recordNames.length; i++) {
						MeasurementType measurement = device.getMeasurement(activeChannelConfigNumber, i);
						recordSymbols[i] = measurement.getSymbol();
						recordUnits[i] = measurement.getUnit();
					}
					recordSet = RecordSet.createRecordSet(recordSetName, device, activeChannelConfigNumber, recordNames, recordSymbols, recordUnits, device.getTimeStep_ms(), isRaw, true, true);
					//set record data type which are not default
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
					
					index = 0;
					if (JetiDataReader.log.isLoggable(Level.FINE))
						JetiDataReader.log.log(Level.FINE, device.getNumberOfMeasurements(activeChannelConfigNumber) + " - " + recordSet.size());
					int[] points = new int[recordNames.length];
					for (int i = 0; i < numValues; i++) {
						for (TelemetrySensor telemetrySensor : recordSetData) {
							if (telemetrySensor.getId() != 0) {
								for (TelemetryData.TelemetryVar dataVar : telemetrySensor.getVariables()) {
									//System.out.print(String.format("%s ", dataVar.getName()));
									if (dataVar.getType() == TelemetryData.T_GPS && (isStartIndexGPS || (isStartIndexGPS = dataVar.getNextIntFrom(time_ms) > 0))) {
										points[index++] = (int) (dataVar.getDoubleAt(time_ms) * 1000000);
									}
									else	
										points[index++] = dataVar.getIntValueAt(time_ms);									
									//System.out.print(String.format("%3.2f ", (points[index-1]/1000.0)));
								}
							}
						}
						for (TelemetrySensor telemetrySensor : recordSetData) {
							if (telemetrySensor.getId() == 0) { // Warnings, Events, ..
								for (TelemetryData.TelemetryVar dataVar : telemetrySensor.getVariables()) {
									//System.out.print(String.format("%s \n", dataVar.getName()));
									points[index++] = dataVar.getIntValueAt(time_ms);									
									//System.out.print(String.format("%3.2f ", (points[index-1]/1000.0)));
								}
							}
						}
						recordSet.addPoints(points, time_ms);
						time_ms += timeStep_ms;
						index = 0;
						//System.out.println();
					}
					GDE.getUiNotification().setProgress(100);
						//if (application.getStatusBar().getMessage().length > 0)
						//	isAlarmMEssageDisplayed = true;

					activeChannel.setActiveRecordSet(recordSetName);
					activeChannel.applyTemplate(recordSetName, true);
					device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
					if (!isOutdated) {
						long startTimeStamp = (long) (new File(filePath).lastModified() - activeChannel.get(recordSetName).getMaxTime_ms());
						if (isActualTime && ordinalTimeRecord >= 0 
								&& activeChannel.get(recordSetName).get(ordinalTimeRecord).size() > 1 
								&& activeChannel.get(recordSetName).get(ordinalTimeRecord).get(0) != 0) {
							int timeValue = activeChannel.get(recordSetName).get(ordinalTimeRecord).get(0) / 1000;
							int tmpHH = timeValue/10000;
							int tmpMM = timeValue/100 - tmpHH*100;
							int tmpSS = timeValue - tmpMM*100 - tmpHH*10000;

							Calendar cal = new GregorianCalendar();
							cal.setTimeInMillis(startTimeStamp);
							cal.set(Calendar.HOUR_OF_DAY, tmpHH);
							cal.set(Calendar.MINUTE, tmpMM);
							cal.set(Calendar.SECOND, tmpSS);
							startTimeStamp = cal.getTimeInMillis();						
						}
						if (isActualDate && ordinalDateRecord >= 0 
								&& activeChannel.get(recordSetName).get(ordinalDateRecord).size() > 11 
								&& activeChannel.get(recordSetName).get(ordinalDateRecord).get(0) != 0) {
							int dateValue = activeChannel.get(recordSetName).get(ordinalDateRecord).get(10) / 1000;
							int tmpYY = dateValue/10000;
							int tmpMM = dateValue/100 - tmpYY*100;
							int tmpDD = dateValue - tmpMM*100 - tmpYY*10000;

							Calendar cal = new GregorianCalendar();
							cal.setTimeInMillis(startTimeStamp);
							cal.set(Calendar.YEAR, tmpYY + 2000);
							cal.set(Calendar.MONTH, tmpMM - 1);
							cal.set(Calendar.DAY_OF_MONTH, tmpDD);
							startTimeStamp = cal.getTimeInMillis();						
						}
						activeChannel.get(recordSetName).setRecordSetDescription(
								device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp));
						activeChannel.get(recordSetName).setStartTimeStamp(startTimeStamp);
						activeChannel.setFileDescription(dateTime.substring(0, 10) + activeChannel.getFileDescription().substring(10));
					}
					else {
						activeChannel.get(recordSetName).setRecordSetDescription(
								device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new Date())); //$NON-NLS-1$
					}
					if (reverseChannelStatistics.length() > 3)//remove new line char
						reverseChannelStatistics.deleteCharAt(reverseChannelStatistics.length()-1);
					//write filename after import to record description
					activeChannel.get(recordSetName).descriptionAppendFilename(filePath.substring(filePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX) + 1));
					if (data.getModelName().length() > 2)
						activeChannel.get(recordSetName).setRecordSetDescription(Messages.getString(gde.device.jeti.MessageIds.GDE_MSGT2914,
								new String[]{activeChannel.get(recordSetName).getRecordSetDescription(), data.getModelName()}));
					for (String statistics : reverseChannelStatistics.toString().split(GDE.STRING_NEW_LINE)) {
						activeChannel.get(recordSetName).setRecordSetDescription(activeChannel.get(recordSetName).getRecordSetDescription() 
								+ Messages.getString(gde.device.jeti.MessageIds.GDE_MSGT2915, new String[]{GDE.STRING_NEW_LINE, statistics}));
					}
							
					activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
					activeChannel.get(recordSetName).updateVisibleAndDisplayableRecordsForTable();
					if (GDE.isWithUi()) activeChannel.switchRecordSet(recordSetName);
				}
			}
		}
		catch (Exception e) {
			// check if previous records are available and needs to be displayed
			if (activeChannel != null && activeChannel.size() > 0) {
				String recordSetName = activeChannel.getFirstRecordSetName();
				activeChannel.setActiveRecordSet(recordSetName);
				device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
				if (GDE.isWithUi()) activeChannel.switchRecordSet(recordSetName);
			}
			// now display the error message
			String msg = filePath + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGE0045, new Object[] { e.getMessage(), lineNumber });
			JetiDataReader.log.log(Level.WARNING, msg, e);
			JetiDataReader.application.openMessageDialog(msg);
		}

		return recordSet;
	}
}
