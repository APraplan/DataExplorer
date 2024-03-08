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

    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
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
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
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
import gde.utils.StringHelper;

/**
 * Class to read and write comma separated value files which simulates serial data
 * one data line consist of $1;1;0; 14780;  598;  1000;  8838;.....;0002;
 * where $recordSetNumber; stateNumber; timeStepSeconds; firstIntValue; secondIntValue; .....;checkSumIntValue;
 * All properties around the textual data in this line has to be specified in DataBlockType (type=TEXT, size number of values, separator=;, ...), refer to DeviceProperties_XY.XSD
 * @author Winfried Brügmann
 */
public class NMEAReaderWriter {
	static Logger										log					= Logger.getLogger(NMEAReaderWriter.class.getName());

	static String										lineSep			= GDE.LINE_SEPARATOR;
	static DecimalFormat						df3					= new DecimalFormat("0.000");													//$NON-NLS-1$
	static StringBuffer							sb;
	static String										tmpSetupString;

	final static DataExplorer				application	= DataExplorer.getInstance();
	final static Channels						channels		= Channels.getInstance();

	/**
	 * read the selected NMEA file and read/parse
	 * @param filePath
	 * @param device
	 * @param recordNameExtend
	 * @param channelConfigNumber
	 * @return record set created
	 * @throws NotSupportedFileFormatException
	 * @throws MissMatchDeviceException
	 * @throws IOException
	 * @throws DataInconsitsentException
	 * @throws DataTypeException
	 */
	public static RecordSet read(String filePath, IDevice device, String recordNameExtend, Integer channelConfigNumber) throws NotSupportedFileFormatException, IOException, DataInconsitsentException,
			DataTypeException {
		long startTime = System.nanoTime() / 1000000;
		String line = GDE.STRING_STAR;
		RecordSet recordSet = null;
		String recordSetName = GDE.STRING_EMPTY;
		BufferedReader reader; // to read the data
		Channel activeChannel = null;
		int lineNumber = 1;
		int activeChannelConfigNumber = 1; // at least each device needs to have one channelConfig to place record sets
		String recordSetNameExtend = device.getRecordSetStemNameReplacement();
		long timeStamp = -1;
		int lastProgress = 0;
		File inputFile = new File(filePath);
		GDE.getUiNotification().setProgress(0);

		try {
			if (channelConfigNumber == null)
				activeChannel = NMEAReaderWriter.channels.getActiveChannel();
			else
				activeChannel = NMEAReaderWriter.channels.get(channelConfigNumber);

			if (activeChannel != null) {
				GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0594) + filePath);
				activeChannelConfigNumber = activeChannel.getNumber();

				if (GDE.isWithUi()) {
					NMEAReaderWriter.channels.switchChannel(activeChannelConfigNumber, GDE.STRING_EMPTY);
					NMEAReaderWriter.application.getMenuToolBar().updateChannelSelector();
					activeChannel = NMEAReaderWriter.channels.getActiveChannel();
				}
				recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$
				int measurementSize = device.getNumberOfMeasurements(activeChannelConfigNumber);
				int dataBlockSize = Math.abs(device.getDataBlockSize(InputTypes.FILE_IO)); // measurements size must not match data block size, there are some measurements which are result of calculation
				log.log(java.util.logging.Level.FINE, "measurementSize = " + measurementSize + "; dataBlockSize = " + dataBlockSize); //$NON-NLS-1$ //$NON-NLS-2$
				if (measurementSize < dataBlockSize) {
					dataBlockSize = measurementSize;
				}
				NMEAParser data = new NMEAParser(device.getDataBlockLeader(), device.getDataBlockSeparator().value(), device.getDataBlockCheckSumType(), dataBlockSize, device, activeChannelConfigNumber, device.getUTCdelta());

				long approximateLines = inputFile.length()/65; //average approximately 70 bytes per line
				DataInputStream binReader = new DataInputStream(new FileInputStream(inputFile));
				byte[] buffer = new byte[1024];
				byte[] lineEnding = device.getDataBlockEnding();
				boolean lineEndingOcurred = false;
				int chars = binReader.read(buffer);
				for (int i = 0; i < chars; i++) {
					if (buffer[i] == lineEnding[0] && i < chars - lineEnding.length - 1 && buffer[i + lineEnding.length + 1] != '$' && (lineEnding.length > 1 ? buffer[i + 1] == lineEnding[1] : true)) {
						lineEndingOcurred = true;
					}
				}
				binReader.close();
				if (!lineEndingOcurred) throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0042, new Object[] { chars, filePath }));
				tmpSetupString = new String(buffer);
				if (tmpSetupString.indexOf(NMEAParser.NMEA.SETUP.toString(), 1) > -1 && tmpSetupString.indexOf(NMEAParser.NMEA.SETUP.toString(), 1) < 3) {
					try {
						//SETUP for GPS-Logger firmware <=1.00
						data.timeOffsetUTC = (short) ((buffer[7+7] << 8) + (buffer[7+6] & 0x00FF));
						data.timeOffsetUTC = data.timeOffsetUTC > 12 ? 12 : data.timeOffsetUTC < -12 ? -12 : data.timeOffsetUTC;
					}
					catch (Exception e) {
						log.log(Level.WARNING, "failed interpreting binary data, time offset to UTC not set!"); //$NON-NLS-1$
					}
				}

				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
				Vector<String> lines = new Vector<String>();
				//skip SM GPS-Logger setup sentence
				while ((line = reader.readLine()) != null &&
						(line.startsWith(device.getDataBlockLeader() + NMEA.SETUP.name())
						|| line.startsWith(device.getDataBlockLeader() + NMEA.GPSSETUP.name())
						|| line.startsWith(device.getDataBlockLeader() + NMEA.UL2SETUP.name())
						|| !line.startsWith(device.getDataBlockLeader())
						|| !data.isSupportedSentence(line.substring(1, line.indexOf(device.getDataBlockSeparator().value(), 2))))) {
					if (line != null && (line.startsWith(device.getDataBlockLeader() + NMEA.SETUP.name())
							|| line.startsWith(device.getDataBlockLeader() + NMEA.GPSSETUP.name())
							|| line.startsWith(device.getDataBlockLeader() + NMEA.UL2SETUP.name()))) {
						Vector<String> setupLine = new Vector<String>();
						setupLine.add(line);
						data.parse(setupLine, lineNumber);
					}
					else if (line != null && line.startsWith(device.getDataBlockLeader()) && !data.isSupportedSentence(line.substring(1, line.indexOf(device.getDataBlockSeparator().value(), 2)))) {
						log.log(Level.WARNING, filePath + " line number " + lineNumber + " does not starts with supported NMEA sentence" + line.substring(1, 6) + " !"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					else {
						log.log(Level.WARNING, filePath + " line number " + lineNumber + " does not starts with " + device.getDataBlockLeader() + " !"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					++lineNumber;
				}

				String signature = line.substring(0, 6);
				log.log(Level.FINE, "sync with signature: " + signature); //$NON-NLS-1$

				--lineNumber; // correct do to do-while
				do {
					line = line.trim();
					if (line.length() > 7) {
						approximateLines = inputFile.length() / line.length();
					}
					++lineNumber;
					if (line.length() > 7 && line.startsWith(device.getDataBlockLeader())) {
						log.log(java.util.logging.Level.FINER, line);
						lines.add(line);
					}
					else {
						log.log(Level.WARNING, "line number " + lineNumber + " line length to short or missing " + device.getDataBlockLeader() + " !"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}

					log.log(Level.FINER, "signature = " + signature); //$NON-NLS-1$
					//UniLog2 use like NMEA sentence, but does not have different sentences like a real NMEA GGA, VTC, GGA
					while (!device.getName().equals("UniLog2") && (line = reader.readLine()) != null) {
						line = line.trim();
						++lineNumber;
						if (line.startsWith(signature)) break;

						if (line.length() > 7 && line.startsWith(device.getDataBlockLeader())) {
							log.log(java.util.logging.Level.FINER, line);
							lines.add(line);
						}
						else {
							log.log(Level.WARNING, filePath + " line number " + lineNumber + " line length to short or missing " + device.getDataBlockLeader() + " !"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}
					data.parse(lines, lineNumber);
					int progress = (int) (lineNumber*100/approximateLines);
					if (progress > lastProgress && progress % 5 == 0) {
						GDE.getUiNotification().setProgress(progress);
						lastProgress = progress;
						//System.out.println(progress);
					}
					//start over with new line and signature
					lines.clear();
					if (line != null && line.length() > 7) {
						lines.add(line);
						signature = line.substring(0, 6);
					}

					if (device.getStateType() == null)
						throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0043, new Object[] { device.getPropertiesFileName() }));

					try {
						recordSetNameExtend = device.getRecordSetStateNameReplacement(1); // state name
						if (recordNameExtend.length() > 0) {
							recordSetNameExtend = recordSetNameExtend + GDE.STRING_BLANK + GDE.STRING_LEFT_BRACKET + recordNameExtend + GDE.STRING_RIGHT_BRACKET;
						}
					}
					catch (Exception e) {
						throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0044, new Object[] { 0, filePath, device.getPropertiesFileName() }));
					}

					//detect states where a new record set has to be created
					if (recordSet == null || !recordSet.getName().contains(recordSetNameExtend)) {
						//prepare new record set now
						recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$

						recordSet = RecordSet.createRecordSet(recordSetName, device, activeChannel.getNumber(), true, true, true);
						recordSetName = recordSet.getName(); // cut/correct length
						String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(data.getDate() == null ? new Date() : data.getDate()); //$NON-NLS-1$
						boolean isOutdated = false;
						try {
							isOutdated = Integer.parseInt(dateTime.split(GDE.STRING_DASH)[0]) <= 2000;
						}
						catch (Exception e) {
							// ignore and state as not outdated
						}
						if (!isOutdated) {
							String description = device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime;
							if(!data.deviceSerialNumber.equals(GDE.STRING_EMPTY)) description = description + GDE.LINE_SEPARATOR + "S/N : " + data.deviceSerialNumber; //$NON-NLS-1$
							if(!data.firmwareVersion.equals(GDE.STRING_EMPTY)) description = description.contains(GDE.LINE_SEPARATOR) ? description + "; Firmware  : " + data.firmwareVersion : GDE.LINE_SEPARATOR + "Firmware  : " + data.firmwareVersion; //$NON-NLS-1$ //$NON-NLS-2$
							recordSet.setRecordSetDescription(description);
							activeChannel.setFileDescription(dateTime.substring(0, 10) + (activeChannel.getFileDescription().length() < 11 ? "" : activeChannel.getFileDescription().substring(10)));
						}
						else {
							recordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new Date())); //$NON-NLS-1$
						}

						activeChannel.put(recordSetName, recordSet);
					}

					if (timeStamp < data.time_ms) {
						recordSet.addNoneCalculationRecordsPoints(data.values, data.time_ms);
						
						if (log.isLoggable(Level.FINE)) 
							log.log(Level.FINE, "add " + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss.SSS").format(data.time_ms)); //$NON-NLS-1$);
					}
					timeStamp = data.time_ms;
				}
				while ((line = reader.readLine()) != null);

				activeChannel.setActiveRecordSet(recordSetName);
				activeChannel.get(recordSetName).setRecordSetDescription(activeChannel.get(recordSetName).getRecordSetDescription() + GDE.STRING_BLANK + data.getComment());
				activeChannel.get(recordSetName).setStartTimeStamp(data.getStartTimeStamp());
				activeChannel.applyTemplate(recordSetName, true);
				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
				device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
				activeChannel.get(recordSetName).updateVisibleAndDisplayableRecordsForTable();
				if (GDE.isWithUi()) activeChannel.switchRecordSet(recordSetName);

				reader.close();
				reader = null;

				//write filename after import to record description
				activeChannel.get(recordSetName).descriptionAppendFilename(filePath.substring(filePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX)+1));
				if (data.airPressures.getMaxEntryCount() > 0) {
					log.log(Level.INFO, data.airPressures.listValues());
					StringBuilder sb = new StringBuilder();
						sb.append(String.format("\n%s - offset = %.3f (%2d); count = %4d, %4d, %4d; avgAirSpeed = %3.1f, %3.1f, %3.1f; avgPressure ∆TEK = %3.2f, %3.2f, %3.2f",
								filePath.substring(filePath.lastIndexOf(GDE.FILE_SEPARATOR) + 1), data.airPressures.getAvgDiffMaxCount() / 1000., data.airPressures.getMaxEntryCount(),
								data.vcAirSpeed40.getCount(), data.vcAirSpeedDp1.getCount(), data.vcAirSpeed70.getCount(),
								data.vcAirSpeed40.getAvgValuesA() / 1000., data.vcAirSpeedDp1.getAvgValuesA() / 1000., data.vcAirSpeed70.getAvgValuesA() / 1000.,
								data.vcAirSpeed40.getAvgValuesB() / 1000., data.vcAirSpeedDp1.getAvgValuesB() / 1000., data.vcAirSpeed70.getAvgValuesB() / 1000.));
						log.log(Level.INFO, sb.toString());
					if (sb.length() > 5 && sb.toString().contains(GDE.FILE_ENDING_NMEA)) activeChannel.get(recordSetName)
							.setRecordSetDescription(activeChannel.get(recordSetName).getRecordSetDescription() + sb.toString().substring(sb.toString().indexOf(GDE.FILE_ENDING_NMEA) + 5));
				}
			}
		}
		catch (FileNotFoundException e) {
			log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			if (GDE.isWithUi()) {
				NMEAReaderWriter.application.openMessageDialog(e.getMessage());
			}
		}
		catch (IOException e) {
			log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			if (GDE.isWithUi()) {
				NMEAReaderWriter.application.openMessageDialog(e.getMessage());
			}
		}
		catch (Exception e) {
			// check if previous records are available and needs to be displayed
			if (activeChannel != null && activeChannel.size() > 0) {
				recordSetName = activeChannel.getFirstRecordSetName();
				activeChannel.setActiveRecordSet(recordSetName);
				device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
				if (GDE.isWithUi()) activeChannel.switchRecordSet(recordSetName);
			}
			// now display the error message
			String msg = filePath + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGE0045, new Object[] { e.getMessage(), lineNumber });
			log.log(java.util.logging.Level.WARNING, msg, e);
			if (GDE.isWithUi()) {
				NMEAReaderWriter.application.openMessageDialog(msg);
			}
		}
		finally {
			GDE.getUiNotification().setProgress(100);
			GDE.getUiNotification().setStatusMessage(GDE.STRING_EMPTY);
		}

		if (log.isLoggable(Level.TIME))
			log.log(Level.TIME,  String.format("read time = %s", StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime)))); //$NON-NLS-1$
		return recordSet;
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
