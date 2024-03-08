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
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.ChannelTypes;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.exception.MissMatchDeviceException;
import gde.exception.NotSupportedFileFormatException;
import gde.io.NMEAParser.NMEA;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * Class to read and write comma separated value files which simulates serial data
 * one data line consist of $1;1;0; 14780;  598;  1000;  8838;.....;0002;
 * where $channelConfigNumber; stateNumber; timeStepSeconds; firstIntValue; secondIntValue; .....;checkSumIntValue;
 * All properties around the textual data in this line has to be specified in DataBlockType (type=TEXT, size number of values, separator=;, ...), refer to DeviceProperties_XY.XSD
 * @author Winfried Brügmann
 */
public class CSVSerialDataReaderWriter {
	static Logger					log			= Logger.getLogger(CSVSerialDataReaderWriter.class.getName());

	static String					lineSep	= GDE.LINE_SEPARATOR;
	static DecimalFormat	df3			= new DecimalFormat("0.000"); //$NON-NLS-1$
	static StringBuffer		sb;

	final static DataExplorer				application	= DataExplorer.getInstance();
	final static Channels						channels		= Channels.getInstance();

	/**
	 * read the selected CSV file and parse
	 * @param filePath
	 * @param device
	 * @param recordNameExtend
	 * @param channelConfigNumber
	 * @param data pre-initialized parser
	 * @return record set created
	 * @throws NotSupportedFileFormatException
	 * @throws MissMatchDeviceException
	 * @throws IOException
	 * @throws DataInconsitsentException
	 * @throws DataTypeException
	 */
	public static RecordSet read(String filePath, IDevice device, String recordNameExtend, Integer channelConfigNumber, IDataParser data) throws NotSupportedFileFormatException, IOException, DataInconsitsentException, DataTypeException {
		String line = GDE.STRING_STAR;
		BufferedReader reader; // to read the data
		Channel activeChannel = null;
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new File(filePath).lastModified()); //$NON-NLS-1$
		long inputFileSize = new File(filePath).length();
		int progressLineLength = Math.abs(device.getDataBlockSize(InputTypes.FILE_IO));
		boolean isOutdated = false;
		int lineNumber = 0;
		int activeChannelConfigNumber = 1; // at least each device needs to have one channelConfig to place record sets
		String recordSetNameExtend = device.getRecordSetStemNameReplacement();
		RecordSet channelRecordSet = null;
		int lastRecordSetNumberOffset = 0;
		Vector<RecordSet> createdRecordSets = new Vector<RecordSet>(1);
		StringBuilder inputLineBuffer = new StringBuilder();
		boolean isRedirect2Channel1 = data.isRedirectChannel1();
		int endIndex = 0;
		int dataBlockNumber = 1;
		String firmwareHardwareDescription = new String();
		long lastRecordEndTimeStamp_ms = 0;
		boolean isDataConsistentChecked = false;

		try {
			if (channelConfigNumber == null)
				activeChannel = channels.getActiveChannel();
			else
				activeChannel = channels.get(channelConfigNumber);
			activeChannelConfigNumber = channels.getActiveChannelNumber();

			if (activeChannel != null) {
				GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0594) + filePath);
				GDE.getUiNotification().setProgress(0);
				activeChannelConfigNumber = activeChannel.getNumber();

				if (device.recordSetNumberFollowChannel() && activeChannel.size() != 0) {
					application.getDeviceSelectionDialog().setupDataChannels(device);
				}

				String recordSetName = (activeChannel.size() + 1) + recordSetNameExtend; //temporary name will be replaced while recordSet creation below

				DataInputStream binReader    = new DataInputStream(new FileInputStream(new File(filePath)));
				byte[] buffer = new byte[1024];
				byte[] lineEnding = device.getDataBlockEnding();
				boolean lineEndingOcurred = false;
				int chars = binReader.read(buffer);
				for (int i = 0; i < chars; i++) {
					if (buffer[i] == lineEnding[0] && i < chars-lineEnding.length-1 && buffer[i+lineEnding.length+1] != '$'
						&& (lineEnding.length > 1 ? buffer[i+1] == lineEnding[1] : true)) {
						lineEndingOcurred = true;
					}
				}
				binReader.close();
				if (!lineEndingOcurred) throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0042, new Object[] {chars, filePath}));

				long lastTimeStamp = 0;
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$

				if (device.getStateType() == null)
					throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0043, new Object[] {device.getPropertiesFileName()}));

				while ((line = reader.readLine()) != null) {
					++lineNumber;
					if (line.startsWith(device.getDataBlockLeader())) {
						//execute special device related parsing operations
						if (line.startsWith(device.getDataBlockLeader() + NMEA.SETUP.name()) || line.startsWith(device.getDataBlockLeader() + NMEA.GPGGA.name())) {
							data.parse(line, lineNumber);
							continue;
						}
						else if (line.trim().endsWith(device.getDataBlockSeparator().value())) {
							log.log(Level.WARNING, filePath + " - skipped " + lineNumber + ", ends with " + device.getDataBlockSeparator().value()); //$NON-NLS-1$
							continue;
						}
					}
					else {//skip all lines which does not match the configured data block leader
						if (line.startsWith("@Model")) {//iCharger model and hardware/firmware level
							//System.out.println("!" + line.substring(1) + "! " + line.substring(1).length());
							//System.out.println("!" + line.substring(1).trim() + "! " + line.substring(1).trim().length());
							//attention, special character has influence to file pointer calculation while writing OSD file
							firmwareHardwareDescription = line.substring(1).trim().replace(GDE.CHAR_COLON, GDE.CHAR_BLANK); //this line contains some special character, remove it using trim()
						} else
							log.log(Level.WARNING, filePath + " - skipped " + lineNumber + ", it does not start with " + device.getDataBlockLeader()); //$NON-NLS-1$
						continue;
					}

					if (isRedirect2Channel1) {
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "number of values inputLineBuffer " + inputLineBuffer.toString().split(device.getDataBlockSeparator().value()).length);
						if (line.startsWith("$1")) { //start new $1 segment
							int numValues = dataBlockNumber == 2 ? 58 : dataBlockNumber == 3 ? 75 : Math.abs(device.getDataBlockSize(InputTypes.FILE_IO)) - 25;
							if (inputLineBuffer.toString().split(device.getDataBlockSeparator().value()).length > numValues) {
								data.parse(inputLineBuffer.toString(), lineNumber);
								endIndex = line.lastIndexOf(GDE.CHAR_SEMICOLON, line.lastIndexOf(GDE.CHAR_SEMICOLON) - 3);
								inputLineBuffer = new StringBuilder().append(line.substring(0, endIndex));
							}
							else {
								endIndex = line.lastIndexOf(GDE.CHAR_SEMICOLON, line.lastIndexOf(GDE.CHAR_SEMICOLON) - 3);
								inputLineBuffer = new StringBuilder().append(line.substring(0, endIndex));
								continue;
							}
						}
						else {
							try {
								dataBlockNumber = line.charAt(1)-48;
							}
							catch (Exception e) {
								dataBlockNumber = 1;
							}
							endIndex = line.lastIndexOf(GDE.CHAR_SEMICOLON);
							inputLineBuffer.append(line.substring(line.indexOf(device.getDataBlockSeparator().value(), 6), endIndex));
							continue;
						}
					}
					else { //normal behavior
						data.parse(line, lineNumber);
					}

					try {
						if (data.getChannelConfigNumber() > device.getChannelCount())
							continue; //skip data if not configured

						activeChannelConfigNumber = device.recordSetNumberFollowChannel() ? data.getChannelConfigNumber() : activeChannelConfigNumber;					
						activeChannel = channels.get(activeChannelConfigNumber);
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, device.getChannelCount() + " - data for channel = " + activeChannelConfigNumber + " state = " + data.getState());
						
						if (!isDataConsistentChecked) {
							//$1;1;0; 14780;  598;  1000;  8838;  0002
							//$channelConfigNumber;stateNumber;timeStepSeconds;firstIntValue;secondIntValue;.....;checkSumIntValue;
							int measurementSize = device.getNumberOfMeasurements(activeChannelConfigNumber);
							int dataBlockSize = device.getDataBlockSize(InputTypes.FILE_IO); // measurements size must not match data block size, there are some measurements which are result of calculation
							if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "measurementSize = " + measurementSize + "; dataBlockSize = " + dataBlockSize);  //$NON-NLS-1$ //$NON-NLS-2$
							if (dataBlockSize < 0 && measurementSize > Math.abs(dataBlockSize))
								throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0041, new String[] {filePath}));
							isDataConsistentChecked = true;
						}

						//enable adding iCharger Ri values
						boolean isJunsiChargerSpecialState = (device.getName().startsWith("iCharger") && data.getState() >= 128);
						recordSetNameExtend = isJunsiChargerSpecialState  
								? recordSetNameExtend 
								: device.getRecordSetStateNameReplacement(data.getState());
						if (recordNameExtend.length() > 0 && !isJunsiChargerSpecialState) {
							recordSetNameExtend = recordSetNameExtend + GDE.STRING_BLANK + GDE.STRING_LEFT_BRACKET + recordNameExtend + GDE.STRING_RIGHT_BRACKET;
						}
						channelRecordSet = activeChannel.get(device.recordSetNumberFollowChannel() && activeChannel.getType() == ChannelTypes.TYPE_CONFIG ? activeChannel.getLastActiveRecordSetName() : recordSetName);
						recordSetNameExtend = recordSetNameExtend.length() <= RecordSet.MAX_NAME_LENGTH - 4 /* '99) '*/ ? recordSetNameExtend : recordSetNameExtend.substring(0, RecordSet.MAX_NAME_LENGTH - 5) + GDE.STRING_RIGHT_BRACKET;
					}
					catch (Exception e) {
						throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0044, new Object[] {data.getState(), filePath, device.getPropertiesFileName()}));
					}

					// check if a record set matching for re-use is available and prepare a new if required
					if (activeChannel.size() == 0 || channelRecordSet == null || !recordSetName.endsWith(GDE.STRING_BLANK + recordSetNameExtend) || lastRecordSetNumberOffset != data.getRecordSetNumberOffset()) {
						//record set does not exist or is outdated, build a new name and create, in case of ChannelTypes.TYPE_CONFIG try sync with channel number
						if (lastRecordSetNumberOffset != data.getRecordSetNumberOffset() && channelRecordSet != null) {
							if (channelRecordSet.get(0).size() < 3) {
								channelRecordSet = activeChannel.get(recordSetName);
								activeChannel.remove(recordSetName);
								createdRecordSets.remove(channelRecordSet);
								log.log(Level.WARNING, filePath + " - remove record set with < 3 data points, last line number = " + (lineNumber - 1)); //$NON-NLS-1$
								activeChannel.put(recordSetName, RecordSet.createRecordSet(recordSetName, application.getActiveDevice(), activeChannelConfigNumber, true, false, true));
								createdRecordSets.add(activeChannel.get(recordSetName));
								recordSetName = channelRecordSet.getName(); // cut/correct length

								if (activeChannel.getType() == ChannelTypes.TYPE_CONFIG)
									activeChannel.applyTemplate(recordSetName, false);
								else
									activeChannel.applyTemplateBasics(recordSetName);
							}
							else {
								channelRecordSet.checkAllDisplayable(); // raw import needs calculation of passive records
								if (activeChannel.getType() == ChannelTypes.TYPE_CONFIG)
									activeChannel.applyTemplate(recordSetName, false);
								else
									activeChannel.applyTemplateBasics(recordSetName);
								device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
							}
						}
						else {
							int recordNumber = device.recordSetNumberFollowChannel() && activeChannel.getType() == ChannelTypes.TYPE_CONFIG ? activeChannel.getNextRecordSetNumber(activeChannelConfigNumber) : activeChannel.getNextRecordSetNumber();
							recordSetName = recordNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + recordSetNameExtend;
							recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH-1) + GDE.STRING_RIGHT_BRACKET;

							activeChannel.put(recordSetName, RecordSet.createRecordSet(recordSetName, application.getActiveDevice(), activeChannelConfigNumber, true, false, true));
							if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordSetName + " created for channel " + activeChannel.getName()); //$NON-NLS-1$

							if (activeChannel.getType() == ChannelTypes.TYPE_CONFIG)
								activeChannel.applyTemplate(recordSetName, false);
							else
								activeChannel.applyTemplateBasics(recordSetName);

							activeChannel.setActiveRecordSet(recordSetName);
							channelRecordSet = activeChannel.get(recordSetName);
							createdRecordSets.add(channelRecordSet);
							recordSetName = channelRecordSet.getName(); // cut/correct length
							if (firmwareHardwareDescription.length() > 10 && !activeChannel.getFileDescription().contains(firmwareHardwareDescription))
								activeChannel.setFileDescription(activeChannel.getFileDescription() + GDE.STRING_MESSAGE_CONCAT + firmwareHardwareDescription);
							device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
							lastRecordEndTimeStamp_ms = data.getTime_ms();
						}


						try {
							isOutdated = Integer.parseInt(dateTime.split(GDE.STRING_DASH)[0]) <= 2000;
						}
						catch (Exception e) {
							// ignore and state as not outdated
						}

						// make all records displayable while absolute data
						for (String recordKey : device.getMeasurementNamesReplacements(activeChannelConfigNumber)) {
							channelRecordSet.get(recordKey).setDisplayable(true); // all data available
						}

						lastRecordSetNumberOffset = data.getRecordSetNumberOffset();
					}
					//add data only if
					if (data.getTime_ms() - lastTimeStamp >= 0) {
						//System.out.println("timeStep_ms = " + (data.getTime_ms() - lastRecordEndTimeStamp_ms));
						channelRecordSet.addNoneCalculationRecordsPoints(data.getValues(), data.getTime_ms() - lastRecordEndTimeStamp_ms);
						data.setTimeResetEnabled(true);
						lastTimeStamp = data.getTime_ms();
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
				GDE.getUiNotification().setProgress(100);

				Iterator<RecordSet> iterator = createdRecordSets.iterator();
				while (iterator.hasNext()) {
					RecordSet tmpRecordSet = iterator.next();
					if (tmpRecordSet.get(0).realSize() < 3) {
						channels.get(tmpRecordSet.getChannelConfigNumber()).remove(recordSetName);
						log.log(Level.WARNING, filePath + " - remove record set " + tmpRecordSet.getName() + " with < 3 data points, last line number = " + (lineNumber - 1)); //$NON-NLS-1$
						iterator.remove();
					}
					else {
						if (!isOutdated) {
							long startTimeStamp = (long) (new File(filePath).lastModified() - tmpRecordSet.getMaxTime_ms());
							tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT	+ Messages.getString(MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp));
							tmpRecordSet.setStartTimeStamp(startTimeStamp);
							String initialFileDescription = dateTime.substring(0, 10) + activeChannel.getFileDescription().substring(10);
							activeChannel.setFileDescription(initialFileDescription);
							if (activeChannel.getActiveRecordSet() != null && activeChannel.getActiveRecordSet().getDevice().useChannelWithSyncedDescription())
								activeChannel.syncFileDescription(activeChannel, initialFileDescription);
						}
						else {
							tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT	+ Messages.getString(MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new Date())); //$NON-NLS-1$
						}
						tmpRecordSet.checkAllDisplayable(); // raw import needs calculation of passive records
						device.updateVisibilityStatus(tmpRecordSet, true);
					}
					//write filename after import to record description
					tmpRecordSet.descriptionAppendFilename(filePath.substring(filePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX)+1));
					if (firmwareHardwareDescription.length() > 10 && !tmpRecordSet.getDescription().contains(firmwareHardwareDescription))
						tmpRecordSet.setRecordSetDescription(tmpRecordSet.getDescription()+ GDE.STRING_NEW_LINE + firmwareHardwareDescription);
				}

				if (GDE.isWithUi()) {
					if (createdRecordSets.size() == 1) {
						channels.switchChannel(activeChannelConfigNumber, createdRecordSets.firstElement().getName());
					}
					else if (createdRecordSets.size() > 1 && !device.recordSetNumberFollowChannel()) {
						channels.switchChannel(activeChannelConfigNumber, createdRecordSets.lastElement().getName());
					}
					else {
						channels.switchChannel(activeChannelConfigNumber, GDE.STRING_EMPTY);
					}
				}

				reader.close();
				reader = null;
			}
		}
		catch (FileNotFoundException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			application.openMessageDialog(e.getMessage());
		}
		catch (IOException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			application.openMessageDialog(e.getMessage());
		}
		catch (Exception e) {
			// check if previous records are available and needs to be displayed
			if (activeChannel != null && activeChannel.size() > 0) {
				String recordSetName = activeChannel.getFirstRecordSetName();
				activeChannel.setActiveRecordSet(recordSetName);
				device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
				if (GDE.isWithUi())
					if (createdRecordSets.size() == 0) {
						channels.switchChannel(1, GDE.STRING_EMPTY);
					}
					else if (createdRecordSets.size() == 1) {
						channels.switchChannel(activeChannelConfigNumber, createdRecordSets.firstElement().getName());
					}
					else {
						channels.switchChannel(activeChannelConfigNumber, createdRecordSets.lastElement().getName());
					}
			}
			// now display the error message
			String msg = filePath + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGE0045, new Object[] {e.getMessage(), lineNumber});
			log.log(Level.WARNING, msg, e);
			application.openMessageDialog(msg);
		}
		finally {
			GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
			GDE.getUiNotification().setProgress(100);
		}

		return channelRecordSet;
	}

//	/**
//	 * write data CVS file
//	 * @throws Exception
//	 */
//	public static void write(char separator, String recordSetKey, String filePath, boolean isRaw) throws Exception {
//		BufferedWriter writer;
//		String sThreadId = String.format("%06d", Thread.currentThread().getId());
//
//		try {
//			if (application.getStatusBar() != null) application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138, new String[] {GDE.FILE_ENDING_CSV, filePath}));
//			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
//			char decimalSeparator = Settings.getInstance().getDecimalSeparator();
//
//			df3.setGroupingUsed(false);
//			sb = new StringBuffer();
//			RecordSet recordSet = Channels.getInstance().getActiveChannel().get(recordSetKey);
//			IDevice device = DataExplorer.getInstance().getActiveDevice();
//			// write device name , manufacturer, and serial port string
//			sb.append(device.getName()).append(separator).append(recordSet.getChannelConfigName()).append(lineSep);
//			writer.write(sb.toString());
//			log.log(Level.FINE, "written header line = " + sb.toString());  //$NON-NLS-1$
//
//			sb = new StringBuffer();
//			sb.append(Messages.getString(MessageIds.GDE_MSGT0137)).append(separator); // Spannung [V];Strom [A];Ladung [Ah];Leistung [W];Energie [Wh]"; //$NON-NLS-1$
//			// write the measurements signature
//			String[] recordNames = recordSet.getRecordNames();
//			for (int i = 0; i < recordNames.length; i++) {
//				MeasurementType  measurement = device.getMeasurement(recordSet.getChannelConfigNumber(), i);
//				Record record = recordSet.get(recordNames[i]);
//				log.log(Level.FINEST, "append " + recordNames[i]); //$NON-NLS-1$
//				if (isRaw) {
//					if (!measurement.isCalculation()) {	// only use active records for writing raw data
//						sb.append(recordNames[i]).append(" [---]").append(separator);	 //$NON-NLS-1$
//						log.log(Level.FINEST, "append " + recordNames[i]); //$NON-NLS-1$
//					}
//				}
//				else {
//					sb.append(recordNames[i]).append(" [").append(record.getUnit()).append(']').append(separator);	 //$NON-NLS-1$
//					log.log(Level.FINEST, "append " + recordNames[i]); //$NON-NLS-1$
//				}
//			}
//			sb.deleteCharAt(sb.length() - 1).append(lineSep);
//			log.log(Level.FINER, "header line = " + sb.toString()); //$NON-NLS-1$
//			writer.write(sb.toString());
//
//			// write data
//			long startTime = new Date().getTime();
//			int recordEntries = recordSet.getRecordDataSize(true);
//			int progressCycle = 0;
//			if (application.getStatusBar() != null) application.setProgress(progressCycle, sThreadId);
//			for (int i = 0; i < recordEntries; i++) {
//				sb = new StringBuffer();
//				// add time entry
//				sb.append((df3.format(Double.valueOf(i * recordSet.getTimeStep_ms() / 1000.0))).replace('.', decimalSeparator)).append(separator).append(' ');
//				// add data entries
//				for (int j = 0; j < recordNames.length; j++) {
//					Record record = recordSet.getRecord(recordNames[j]);
//					if (record == null)
//						throw new Exception(Messages.getString(MessageIds.GDE_MSGE0005, new Object[]{recordNames[j], recordSet.getChannelConfigName()}));
//
//					MeasurementType measurement = device.getMeasurement(recordSet.getChannelConfigNumber(), j);
//					if (isRaw) { // do not change any values
//						if (!measurement.isCalculation())
//							if (record.getParent().isRaw())
//								sb.append(df3.format((record.get(i)/1000.0)).replace('.', decimalSeparator)).append(separator);
//							else
//								sb.append(df3.format(device.reverseTranslateValue(record, record.get(i)/1000.0)).replace('.', decimalSeparator)).append(separator);
//					}
//					else
//						// translate according device and measurement unit
//						sb.append(df3.format(device.translateValue(record, record.get(i)/1000.0)).replace('.', decimalSeparator)).append(separator);
//				}
//				sb.deleteCharAt(sb.length() - 1).append(lineSep);
//				writer.write(sb.toString());
//				if (application.getStatusBar() != null && i % 50 == 0) application.setProgress(((++progressCycle*5000)/recordEntries), sThreadId);
//				log.log(Level.FINE, "data line = " + sb.toString()); //$NON-NLS-1$
//			}
//			sb = null;
//			log.log(Level.FINE, "CSV file = " + filePath + " erfolgreich geschieben"  //$NON-NLS-1$ //$NON-NLS-2$
//					+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$
//
//			writer.flush();
//			writer.close();
//			writer = null;
//			//recordSet.setSaved(true);
//			if (application.getStatusBar() != null) application.setProgress(100, sThreadId);
//		}
//		catch (IOException e) {
//			log.log(Level.SEVERE, e.getMessage(), e);
//			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0006, new Object[]{ GDE.FILE_ENDING_CSV, filePath, e.getMessage() }));
//		}
//		catch (Exception e) {
//			log.log(Level.SEVERE, e.getMessage(), e);
//			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
//		}
//		finally {
//			if (application.getStatusBar() != null) application.setStatusMessage(GDE.STRING_EMPTY);
//		}
//
//	}

}
