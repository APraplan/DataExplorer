/**
 *
 */
package gde.device.graupner;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

import gde.Analyzer;
import gde.GDE;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.device.graupner.HoTTAdapter.PickerParameters;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

/**
 * @author brueg
 */
public class HoTTlogReader extends HoTTbinReader {
	final static Logger	log					= Logger.getLogger(HoTTlogReader.class.getName());
	protected static boolean 			isHoTTAdapter2 = false;
	protected static int[]				points;
	protected static RcvLogParser rcvLogParser;
	protected static ChnLogParser chnLogParser;
	protected static VarLogParser varLogParser;
	protected static GpsLogParser gpsLogParser;
	protected static GamLogParser gamLogParser;
	protected static EamLogParser eamLogParser;
	protected static EscLogParser escLogParser;
	protected static EscLogParser esc2LogParser;
	protected static EscLogParser esc3LogParser;
	protected static EscLogParser esc4LogParser;

	/**
	 * read complete file data and display the first found record set
	 *
	 * @param filePath
	 * @throws Exception
	 */
	public static synchronized void read(String filePath, PickerParameters newPickerParameters) throws Exception {
		final String $METHOD_NAME = "read";
		HoTTlogReader.pickerParameters = newPickerParameters;
		HashMap<String, String> fileInfoHeader = getFileInfo(new File(filePath), newPickerParameters);
		HoTTlogReader.detectedSensors = Sensor.getSetFromDetected(fileInfoHeader.get(HoTTAdapter.DETECTED_SENSOR));

		final File file = new File(fileInfoHeader.get(HoTTAdapter.FILE_PATH));
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		HoTTAdapter device = (HoTTAdapter) HoTTlogReader.application.getActiveDevice();
		HoTTlogReader.isHoTTAdapter2 = HoTTlogReader.application.getActiveDevice() instanceof HoTTAdapter2;
		int recordSetNumber = HoTTbinReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		HoTTbinReader.recordSetReceiver = null; // 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=UminRx
		HoTTbinReader.recordSetChannel = null; // 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16 19=PowerOff 20=BattLow 21=Reset 22=Warning
		HoTTbinReader.recordSetGAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinReader.recordSetEAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Altitude, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2, 27=Revolution
		HoTTbinReader.recordSetVario = null; // 0=RXSQ, 1=Altitude, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx 7=Event 8=accX 9=accY 10=accZ 11=reserved 12=version
		HoTTbinReader.recordSetGPS = null; // 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripLength, 10=VoltageRx, 11=TemperatureRx 12=satellites 13=GPS-fix 14=EventGPS 15=HomeDirection 16=Roll 17=Pitch 18=Yaw 19=GyroX 20=GyroY 21=GyroZ 22=Vibration 23=Version	
		HoTTbinReader.recordSetESC = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
		HoTTbinReader.recordSetESC2 = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
		HoTTbinReader.recordSetESC3 = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
		HoTTbinReader.recordSetESC4 = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
		long[] timeSteps_ms = new long[] {0};
		int numberLogChannels = Integer.valueOf(fileInfoHeader.get("LOG NOB CHANNEL"));
		boolean isASCII = fileInfoHeader.get("LOG TYPE").contains("ASCII");
		int rawDataBlockSize = Integer.parseInt(fileInfoHeader.get(HoTTAdapter.RAW_LOG_SIZE));
		int asciiDataBlockSize = Integer.parseInt(fileInfoHeader.get(HoTTAdapter.ASCII_LOG_SIZE));
		HoTTbinReader.dataBlockSize = isASCII ? asciiDataBlockSize : rawDataBlockSize;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTlogReader.rcvLogParser = (RcvLogParser) Sensor.RECEIVER.createLogParser(HoTTbinReader.pickerParameters, new int[10], timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader.varLogParser = (VarLogParser) Sensor.VARIO.createLogParser(HoTTbinReader.pickerParameters, new int[13], timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader.gpsLogParser = (GpsLogParser) Sensor.GPS.createLogParser(HoTTbinReader.pickerParameters, new int[24], timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader.gamLogParser = (GamLogParser) Sensor.GAM.createLogParser(HoTTbinReader.pickerParameters, new int[26], timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader.eamLogParser = (EamLogParser) Sensor.EAM.createLogParser(HoTTbinReader.pickerParameters, new int[31], timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader.escLogParser = (EscLogParser) Sensor.ESC.createLogParser(HoTTbinReader.pickerParameters, new int[30], timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader.esc2LogParser = (EscLogParser) Sensor.ESC.createLogParser(HoTTbinReader.pickerParameters, new int[30], timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader.esc3LogParser = (EscLogParser) Sensor.ESC.createLogParser(HoTTbinReader.pickerParameters, new int[30], timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader.esc4LogParser = (EscLogParser) Sensor.ESC.createLogParser(HoTTbinReader.pickerParameters, new int[30], timeSteps_ms, buf, numberLogChannels);
		int logTimeStep_ms = 1000/Integer.valueOf(fileInfoHeader.get("COUNTER").split("/")[1].split(GDE.STRING_BLANK)[0]);
		HoTTbinReader.isTextModusSignaled = false;
		boolean isVarioDetected = false;
		boolean isGPSdetected = false;
		boolean isESCdetected = false, isESC2detected = false,  isESC3detected = false,  isESC4detected = false;
		int logDataOffset = Integer.valueOf(fileInfoHeader.get("LOG DATA OFFSET"));
		long numberDatablocks = Long.parseLong(fileInfoHeader.get(HoTTAdapter.LOG_COUNT));
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(fileInfoHeader.get("LOG START TIME"), HoTTbinReader.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks));
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);
		long sequenceNumber = 0, sequenceDelta = 0;

		try {
			HoTTbinReader.recordSets.clear();
			// receiver data are always contained
			// check if recordSetReceiver initialized, transmitter and receiver
			// data always present, but not in the same data rate and signals
			channel = HoTTbinReader.channels.get(1);
			String newFileDescription = HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()	: date;
			if (channel.getFileDescription().length() <= newFileDescription.length() || (HoTTbinReader.application.isObjectoriented() && !channel.getFileDescription().contains(HoTTbinReader.application.getObjectKey())))
				channel.setFileDescription(newFileDescription);
			recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
			HoTTbinReader.recordSetReceiver = RecordSet.createRecordSet(recordSetName, device, 1, true, true, true);
			channel.put(recordSetName, HoTTbinReader.recordSetReceiver);
			HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), HoTTbinReader.recordSetReceiver);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (HoTTbinReader.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, false);
			}
			// recordSetReceiver initialized and ready to add data
			// channel data are always contained
			if (pickerParameters.isChannelsChannelEnabled) {
				// check if recordSetChannel initialized, transmitter and
				// receiver data always present, but not in the same data rate
				// and signals
				channel = HoTTbinReader.channels.get(6);
				channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
						? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
				recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.CHANNEL.value() + recordSetNameExtend;
				HoTTbinReader.recordSetChannel = RecordSet.createRecordSet(recordSetName, device, 6, true, true, true);
				numberLogChannels = HoTTbinReader.recordSetChannel.size() == 23 ? 16 : numberLogChannels;
				HoTTlogReader.chnLogParser = (ChnLogParser) Sensor.CHANNEL.createLogParser(HoTTbinReader.pickerParameters, new int[HoTTbinReader.recordSetChannel.size()], timeSteps_ms, buf, numberLogChannels);
				channel.put(recordSetName, HoTTbinReader.recordSetChannel);
				HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.CHANNEL.value(), HoTTbinReader.recordSetChannel);
				tmpRecordSet = channel.get(recordSetName);
				tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
				tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
				if (HoTTbinReader.application.getMenuToolBar() != null) {
					channel.applyTemplate(recordSetName, false);
				}
				// recordSetChannel initialized and ready to add data
			}

			log.log(Level.INFO, fileInfoHeader.toString());
			// read all the data blocks from the file and parse
			data_in.skip(logDataOffset);
			int i = 0;
			for (; i < numberDatablocks; i++) { //skip log entries before transmitter active
				data_in.read(HoTTbinReader.buf);
				if (isASCII) { //convert ASCII log data to hex
					HoTTlogReader.convertAscii2Raw(rawDataBlockSize, HoTTbinReader.buf);
				}
				log.log(Level.INFO, "raw block data   " + StringHelper.byte2Hex2CharString(HoTTlogReader2.buf, 30));
				sequenceDelta = sequenceDelta == 0 ? 1 : DataParser.getUInt32(HoTTlogReader2.buf, 0) - sequenceNumber;
				timeSteps_ms[BinParser.TIMESTEP_INDEX] += logTimeStep_ms * sequenceDelta;// add default time step given by log msec
				sequenceNumber = DataParser.getUInt32(HoTTlogReader2.buf, 0);
				//log.logp(Level.OFF, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, rawDataBlockSize));
				if (HoTTbinReader.buf[8] == 0 || HoTTbinReader.buf[9] == 0 || HoTTbinReader.buf[24] == 0x1F) { // tx, rx, rx sensitivity data
					continue;
				}
				break;
			}
			
			for (; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (log.isLoggable(Level.FINE)) {
					if (isASCII)
						log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, new String(HoTTbinReader.buf));
					else 
						log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (isASCII) { //convert ASCII log data to hex
					convertAscii2Raw(rawDataBlockSize, HoTTbinReader.buf);
				}
				
				if (HoTTbinReader.buf[24] == 0x1F) {//rx sensitivity data
					if (log.isLoggable(Level.INFO))
						log.log(Level.INFO, "sensitivity data " + StringHelper.byte2Hex2CharString(HoTTlogReader2.buf, 30));
					sequenceDelta = DataParser.getUInt32(HoTTlogReader2.buf, 0) - sequenceNumber;
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += logTimeStep_ms * sequenceDelta;// add default time step given by log msec
					sequenceNumber = DataParser.getUInt32(HoTTlogReader2.buf, 0);
					continue; //skip rx sensitivity data
				}
				
				if (HoTTbinReader.buf[8] != 0 && HoTTbinReader.buf[9] != 0) { //buf 8, 9, tx,rx, rx sensitivity data
					if (log.isLoggable(Level.INFO)) {
						//log.log(Level.INFO, String.format("Sensor %02X", HoTTbinReader.buf[26]));
						log.log(Level.INFO, "sensor data      " + StringHelper.byte2Hex2CharString(HoTTlogReader2.buf, 30));
					}

					HoTTlogReader.rcvLogParser.trackPackageLoss(true);

					// create and fill sensor specific data record sets
						if (log.isLoggable(Level.FINEST)) {
							log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME,
									StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));
						}

						// fill receiver data
						HoTTlogReader.parseAddReceiver(HoTTbinReader.buf);

						if (pickerParameters.isChannelsChannelEnabled) {
							// 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16 19=PowerOff 20=BattLow 21=Reset 22=Warning
							HoTTlogReader.parseAddChannel(HoTTbinReader.buf);
						}

						switch ((byte) (HoTTbinReader.buf[26] & 0xFF)) { //actual sensor
						case HoTTAdapter.ANSWER_SENSOR_VARIO_19200:
								// check if recordSetVario initialized, transmitter and receiver data always
								// present, but not in the same data rate as signals
								if (HoTTbinReader.recordSetVario == null) {
									channel = HoTTbinReader.channels.get(2);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
									HoTTbinReader.recordSetVario = RecordSet.createRecordSet(recordSetName, device, 2, true, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetVario);
									HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.VARIO.value(), HoTTbinReader.recordSetVario);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								// recordSetVario initialized and ready to add data
								HoTTlogReader.parseAddVario(HoTTbinReader.buf);
								if (!isVarioDetected) {
									HoTTAdapter.updateVarioTypeDependent((HoTTbinReader.buf[65] & 0xFF), device, HoTTbinReader.recordSetVario);
									isVarioDetected = true;								
								}
								break;

						case HoTTAdapter.ANSWER_SENSOR_GPS_19200:
								// check if recordSetReceiver initialized, transmitter and receiver
								// data always present, but not in the same data rate as signals
								if (HoTTbinReader.recordSetGPS == null) {
									channel = HoTTbinReader.channels.get(3);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
									HoTTbinReader.recordSetGPS = RecordSet.createRecordSet(recordSetName, device, 3, true, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetGPS);
									HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.GPS.value(), HoTTbinReader.recordSetGPS);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								// recordSetGPS initialized and ready to add data
								HoTTlogReader.parseAddGPS(HoTTbinReader.buf, timeStep_ms);
								if (!isGPSdetected) {
									startTimeStamp_ms = HoTTAdapter.updateGpsTypeDependent((HoTTbinReader.buf[65] & 0xFF), device, HoTTbinReader.recordSetGPS, startTimeStamp_ms);
									isGPSdetected = true;								
								}
								break;

						case HoTTAdapter.ANSWER_SENSOR_GENERAL_19200:
								// check if recordSetGeneral initialized, transmitter and receiver
								// data always present, but not in the same data rate as signals
								if (HoTTbinReader.recordSetGAM == null) {
									channel = HoTTbinReader.channels.get(4);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GAM.value() + recordSetNameExtend;
									HoTTbinReader.recordSetGAM = RecordSet.createRecordSet(recordSetName, device, 4, true, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetGAM);
									HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.GAM.value(), HoTTbinReader.recordSetGAM);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								// recordSetGeneral initialized and ready to add data
								HoTTlogReader.parseAddGAM(HoTTbinReader.buf);
								break;

						case HoTTAdapter.ANSWER_SENSOR_ELECTRIC_19200:
								// check if recordSetGeneral initialized, transmitter and receiver
								// data always present, but not in the same data rate as signals
								if (HoTTbinReader.recordSetEAM == null) {
									channel = HoTTbinReader.channels.get(5);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.EAM.value() + recordSetNameExtend;
									HoTTbinReader.recordSetEAM = RecordSet.createRecordSet(recordSetName, device, 5, true, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetEAM);
									HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.EAM.value(), HoTTbinReader.recordSetEAM);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								// recordSetElectric initialized and ready to add data
								HoTTlogReader.parseAddEAM(HoTTbinReader.buf);
								break;

						case HoTTAdapter.ANSWER_SENSOR_MOTOR_DRIVER_19200:
								// check if recordSetGeneral initialized, transmitter and receiver
								// data always present, but not in the same data rate as signals
								if (HoTTbinReader.recordSetESC == null) {
									channel = HoTTbinReader.channels.get(7);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ESC.value() + recordSetNameExtend;
									HoTTbinReader.recordSetESC = RecordSet.createRecordSet(recordSetName, device, 7, true, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetESC);
									HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.ESC.value(), HoTTbinReader.recordSetESC);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								// recordSetElectric initialized and ready to add data
								HoTTlogReader.parseAddESC(HoTTbinReader.buf);

								if (!isESCdetected) {
									HoTTAdapter.updateEscTypeDependent((HoTTbinReader.buf[65] & 0xFF), device, HoTTbinReader.recordSetESC);
									isESCdetected = true;
								}
								break;

						case HoTTAdapter.ANSWER_SENSOR_ESC2_19200:
								// check if recordSetGeneral initialized, transmitter and receiver
								// data always present, but not in the same data rate as signals
								if (HoTTbinReader.recordSetESC2 == null) {
									channel = HoTTbinReader.channels.get(8);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ESC2.value() + recordSetNameExtend;
									HoTTbinReader.recordSetESC2 = RecordSet.createRecordSet(recordSetName, device, 8, true, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetESC2);
									HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.ESC2.value(), HoTTbinReader.recordSetESC2);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								// recordSetElectric initialized and ready to add data
								HoTTlogReader.parseAddESC2(HoTTbinReader.buf);

								if (!isESC2detected) {
									HoTTAdapter.updateEscTypeDependent((HoTTbinReader.buf[65] & 0xFF), device, HoTTbinReader.recordSetESC2);
									isESC2detected = true;
								}
								break;

						case HoTTAdapter.ANSWER_SENSOR_ESC3_19200:
								// check if recordSetGeneral initialized, transmitter and receiver
								// data always present, but not in the same data rate as signals
								if (HoTTbinReader.recordSetESC3 == null) {
									channel = HoTTbinReader.channels.get(9);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ESC3.value() + recordSetNameExtend;
									HoTTbinReader.recordSetESC3 = RecordSet.createRecordSet(recordSetName, device, 9, true, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetESC3);
									HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.ESC3.value(), HoTTbinReader.recordSetESC3);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								// recordSetESC initialized and ready to add data
								HoTTlogReader.parseAddESC3(HoTTbinReader.buf);

								if (!isESC3detected) {
									HoTTAdapter.updateEscTypeDependent((HoTTbinReader.buf[65] & 0xFF), device, HoTTbinReader.recordSetESC3);
									isESC3detected = true;
								}
								break;

						case HoTTAdapter.ANSWER_SENSOR_ESC4_19200:
								// check if recordSetGeneral initialized, transmitter and receiver
								// data always present, but not in the same data rate as signals
								if (HoTTbinReader.recordSetESC4 == null) {
									channel = HoTTbinReader.channels.get(10);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ESC4.value() + recordSetNameExtend;
									HoTTbinReader.recordSetESC4 = RecordSet.createRecordSet(recordSetName, device, 10, true, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetESC4);
									HoTTbinReader.recordSets.put(HoTTAdapter.Sensor.ESC4.value(), HoTTbinReader.recordSetESC4);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								// recordSetESC initialized and ready to add data
								HoTTlogReader.parseAddESC4(HoTTbinReader.buf);

								if (!isESC4detected) {
									HoTTAdapter.updateEscTypeDependent((HoTTbinReader.buf[65] & 0xFF), device, HoTTbinReader.recordSetESC4);
									isESC4detected = true;
								}
								break;
						}

						sequenceDelta = DataParser.getUInt32(HoTTlogReader2.buf, 0) - sequenceNumber;
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += logTimeStep_ms * sequenceDelta;// add default time step given by log msec
						sequenceNumber = DataParser.getUInt32(HoTTlogReader2.buf, 0);

						HoTTbinReader.isJustParsed = !HoTTlogReader.rcvLogParser.updateLossStatistics();
						
						if (i % progressIndicator == 0)
							GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
					}
					else { // Rx == 0
						if (HoTTlogReader.log.isLoggable(Level.FINE)) HoTTlogReader.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

						HoTTlogReader.rcvLogParser.trackPackageLoss(false);

						if (pickerParameters.isChannelsChannelEnabled && HoTTlogReader.recordSetReceiver.getRecordDataSize(true) > 0) {
							// fill receiver data
							HoTTlogReader.parseAddReceiverRxTxOnly(HoTTbinReader.buf);
							HoTTlogReader.parseAddChannel(HoTTbinReader.buf);
						}

						sequenceDelta = DataParser.getUInt32(HoTTlogReader2.buf, 0) - sequenceNumber;
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += logTimeStep_ms * sequenceDelta;// add default time step given by log msec
						sequenceNumber = DataParser.getUInt32(HoTTlogReader2.buf, 0);
					}
				//} //isTextModus
				//else if (!HoTTbinReader.isTextModusSignaled) {
				//	HoTTbinReader.isTextModusSignaled = true;
				//	HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				//}
			}
			HoTTlogReader.rcvLogParser.finalUpdateLossStatistics();
			String packageLossPercentage = HoTTbinReader.recordSetReceiver.getRecordDataSize(true) > 0
					? String.format("%.1f", HoTTlogReader.rcvLogParser.getLostPackages().percentage) 
					: "100";
			if (HoTTbinReader.pickerParameters.isChannelsChannelEnabled)
				HoTTbinReader.detectedSensors.add(Sensor.CHANNEL);
			HoTTbinReader.recordSetReceiver.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { HoTTlogReader.rcvLogParser.getLossTotal(), HoTTlogReader.rcvLogParser.getLostPackages().lossTotal, packageLossPercentage, HoTTlogReader.rcvLogParser.getLostPackages().getStatistics() })
					+ String.format(" - Sensor: %s", HoTTlogReader.detectedSensors.toString()));
			HoTTbinReader.log.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + HoTTlogReader.rcvLogParser.getLostPackages().lossTotal); //$NON-NLS-1$
			HoTTbinReader.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " //$NON-NLS-1$
					+ StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$

			if (GDE.isWithUi()) {
				for (RecordSet recordSet : HoTTbinReader.recordSets.values()) {
					device.makeInActiveDisplayable(recordSet);
					device.updateVisibilityStatus(recordSet, true);

					// write filename after import to record description
					recordSet.descriptionAppendFilename(file.getName());
				}

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				GDE.getUiNotification().setProgress(100);
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}
	}

	/**
	 * convert ASCII log data to raw, binary format to keep parser identically
	 * @param rawDataBlockSize
	 */
	protected static synchronized void convertAscii2Raw(int rawDataBlockSize, byte[] buffer) {
		String[] splitInput = new String(buffer).split("\\|");
		byte[] rawBuf = new byte[rawDataBlockSize];
		if (splitInput.length != 7 || (splitInput.length > 1 && splitInput[0].length() != 8)) {
			log.log(Level.WARNING, "invalid string input '" + new String(buffer) + "'");
			return;
		}
			
		if (log.isLoggable(Level.FINER))
			for (String part : splitInput)
				log.log(Level.FINER, "'" + part + "'");
		int index = 4;
		for (String statuscByte : splitInput[1].split(","))
			rawBuf[index++] = (byte)Integer.parseInt(statuscByte.trim());

		for (String recByte : splitInput[2].split(","))
			rawBuf[index++] = (byte)Integer.parseInt(recByte.trim(), 16);

		for (String sensorInfoByte : splitInput[3].split(","))
			rawBuf[index++] = (byte)Integer.parseInt(sensorInfoByte.trim(), 16);

		for (String sensorByte : splitInput[4].split(","))
			rawBuf[index++] = (byte)Integer.parseInt(sensorByte.trim(), 16);

		for (String channelByte : splitInput[5].split(",")) {
			String ch = String.format("%04X", Integer.parseInt(channelByte.trim()));
			rawBuf[index++] = (byte)Integer.parseInt(ch.trim().substring(2), 16);
			rawBuf[index++] =(byte)Integer.parseInt(ch.trim().substring(0, 2), 16);
		}
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, StringHelper.byte2Hex4CharString(rawBuf, rawBuf.length));
		System.arraycopy(rawBuf, 0, buffer, 0, rawBuf.length);
	}

	/**
	 * print values evaluated by parsing sensor bytes
	 * @param buffer
	 * @param values
	 * @param endIndex
	 */
	protected static void printSensorValues(byte[] buffer, int[] values, int endIndex) {
		log.log(Level.FINER, StringHelper.byte2Hex2CharString(buffer, 26, 38));
		StringBuilder sb = new StringBuilder().append(String.format("Sensor = 0x%X", buffer[26]));
		sb.append(String.format(" %d", values[endIndex-1] / 1000));
		for (int i = 0; i < endIndex; i++) {
			sb.append(String.format(" %6.3f", values[i] / 1000.0));
		}
		log.log(Level.FINER, sb.toString());
	}	

	/**
	 * Use for HoTTbinReader and HoTTbinHistoReader only (not for HoTTbinReaderD / X and derivates).
	 * @author brueg
	 */
	public abstract static class LogParser {

		public static final int						TIMESTEP_INDEX	= 0;

		@SuppressWarnings("hiding")
		protected final PickerParameters	pickerParameters;
		protected final int[]							points;
		protected final long[]						timeSteps_ms;
		protected final Sensor						sensor;

		protected byte[]									buf;

		/**
		 * Takes the parsing input objects in order to avoid parsing method parameters for better performance.
		 * @param pickerParameters is the parameter object for the current thread
		 * @param points parsed from the input buffers
		 * @param timeSteps_ms is the wrapper object holding the current timestep
		 * @param buffers are the required input buffers for parsing (the first dimension corresponds to the buffers count)
		 * @param sensor associated with this parser (pls note that the receiver / channel is also a sensor)
		 */
		protected LogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, Sensor sensor) {
			this.pickerParameters = pickerParameters;
			this.points = points;
			this.timeSteps_ms = timeSteps_ms;
			this.sensor = sensor;
			this.buf = buffer;
		}

		/**
		 * Parse the buffered data.
		 * @return true if the core points are valid
		 */
		protected abstract boolean parse();

		protected long getTimeStep_ms() {
			return this.timeSteps_ms[TIMESTEP_INDEX];
		}

		/**
		 * @return the sensor associated with this parser (pls note that the receiver / channel is also a sensor)
		 */
		public Sensor getSensor() {
			return this.sensor;
		}

		public int[] getPoints() {
			return this.points;
		}

		public void migratePoints(int[] targetPoints) {
			throw new UnsupportedOperationException("required for HoTTbinReader2 only");
		}

		@Override
		public String toString() {
			final int maxLen = 11;
			return "LogParser [sensor=" + this.sensor + ", timeStep_ms=" + this.getTimeStep_ms() + ", points=" + (this.points != null
					? Arrays.toString(Arrays.copyOf(this.points, Math.min(this.points.length, maxLen)))
					: null) + "]";
		}
	}


	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddReceiver(byte[] _buf) throws DataInconsitsentException {
		HoTTlogReader.rcvLogParser.parse();
		HoTTlogReader.recordSetReceiver.addPoints(HoTTlogReader.rcvLogParser.getPoints(), HoTTlogReader.rcvLogParser.getTimeStep_ms());
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddReceiverRxTxOnly(byte[] _buf) throws DataInconsitsentException {
		int[] tmpPoints = new int[10];
		System.arraycopy(HoTTlogReader.rcvLogParser.points, 0, tmpPoints, 0, 10);
		HoTTlogReader.rcvLogParser.parseTxRxOnly();
		System.arraycopy(HoTTlogReader.rcvLogParser.getPoints(), 4, tmpPoints, 4, 2); //Rx and Tx values only, keep other data since invalid
		HoTTlogReader.recordSetReceiver.addPoints(tmpPoints, HoTTlogReader.rcvLogParser.getTimeStep_ms());
	}

	public static class RcvLogParser extends LogParser {
		private int																	tmpVoltageRx			= 0;
		private int																	tmpTemperatureRx	= 0;

		private int																	consecutiveLossCounter	= 0; 	//number of lost packages since the last invalid packages

		private PackageLoss													lostPackages = new PackageLoss();

		protected final byte[]											buf;

		protected RcvLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer) {
			super(pickerParameters, points, timeSteps_ms, buffer, Sensor.RECEIVER);
			this.buf = buffer;
		}

		protected void parseTxRxOnly() {
			this.points[4] = buf[8] * -1000;
			this.points[5] = buf[9] * -1000;			
		}
		
		@Override
		protected boolean parse() {
			//log.log(Level.OFF, StringHelper.byte2Hex2CharString(buf, 12, 10));
			//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			//data bytes: 12=RX-S-STR 13=voltageRx 14=TemperatureRx 15=Rx-DBM 16=RX-S-QUA 17=voltageRx_min 18=VPack_low_byte 19=VPack_high_byte 20=Tx-DBM 21=?
			this.tmpVoltageRx = (buf[13] & 0xFF);
			this.tmpTemperatureRx = (buf[14] & 0xFF);
			if (isPointsValid()) {
				this.points[1] = (buf[16] & 0xFF) * 1000;
				this.points[2] = (convertRxDbm2Strength(buf[9] & 0xFF)) * 1000;
				this.points[3] = DataParser.parse2Short(buf, 18) * 1000;
				this.points[4] = buf[8] * -1000;
				this.points[5] = buf[9] * -1000;
				this.points[6] = (buf[13] & 0xFF) * 1000;
				this.points[7] = ((buf[14] & 0xFF) - 20) * 1000;
				this.points[8] = (buf[17] & 0xFF) * 1000;
				if ((buf[25] & 0x40) > 0 || (buf[25] & 0x20) > 0 && HoTTbinReader.tmpTemperatureRx >= 70) //T = 70 - 20 = 50 lowest temperature warning
					this.points[9] = (buf[25] & 0x60) * 1000; //warning V,T only
				else
					this.points[9] = 0;
			}
			if (log.isLoggable(Level.FINE)) {
				//data bytes: 8=TXdBm(-D), 9=RXdBm(-D)
				StringBuilder sb = new StringBuilder().append(String.format("Tx-dbm = -%d Rx-dbm = -%d", buf[8], buf[9]));
				for (int i = 0; i < 10; i++) {
					sb.append(String.format(" %6.3f", this.points[i] / 1000.0));
				}
				log.log(Level.FINE, sb.toString());
			}
			return true;
		}

		/**
		 * @param isAvailable true if the package is not lost
		 */
		public void trackPackageLoss(boolean isAvailable) {
			if (isAvailable) {
				this.pickerParameters.reverseChannelPackageLossCounter.add(1);
				this.points[0] = this.pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;
			} else {
				this.pickerParameters.reverseChannelPackageLossCounter.add(0);
				this.points[0] = this.pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;

				++this.consecutiveLossCounter;
			}
			++this.lostPackages.numberTrackedSamples;
		}

		/**
		 * @return true if the lost packages count is transferred into the loss statistics
		 */
		public boolean updateLossStatistics() {
			if (this.consecutiveLossCounter > 0) {
				this.lostPackages.add(this.consecutiveLossCounter);
				this.consecutiveLossCounter = 0;
				return true;
			} else {
				return false;
			}
		}

		/**
		 * update packets loss statistics before reading statistics values
		 */
		public void finalUpdateLossStatistics() {
			this.lostPackages.percentage = this.lostPackages.lossTotal * 100. / (this.lostPackages.numberTrackedSamples - this.consecutiveLossCounter);
			log.log(Level.INFO, String.format("lostPackages = (%d) %d of %d percentage = %3.1f", this.lostPackages.lossTotal + this.consecutiveLossCounter,
					this.lostPackages.lossTotal, this.lostPackages.numberTrackedSamples, this.lostPackages.percentage ));
		}

		/**
		 * @return the total number of lost packages (is summed up while reading the log)
		 */
		public int getLossTotal() {
			return this.lostPackages.lossTotal + this.consecutiveLossCounter;
		}

		/**
		 * @return the total number of lost packages (is summed up while reading the log)
		 */
		public int getConsecutiveLossCounter() {
			return this.consecutiveLossCounter;
		}

		public PackageLoss getLostPackages() {
			return this.lostPackages;
		}
		
		private boolean isPointsValid() {
			return !pickerParameters.isFilterEnabled || this.tmpVoltageRx > -1 && this.tmpVoltageRx < 100 && this.tmpTemperatureRx < 120;
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			for (int j = 0; j < 10; j++) {
				targetPoints[j] = this.points[j];
			}
			throw new UnsupportedOperationException("use in situ parsing");
		}

		@Override
		public String toString() {
			return super.toString() + "  [lossTotal=" + this.lostPackages.lossTotal + ", consecutiveLossCounter=" + this.consecutiveLossCounter + "]";
		}

	}
	
	/**
	 * parse the buffered data from buffer and add points to record set
	 *
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddVario(byte[] _buf) throws DataInconsitsentException {
		if (HoTTlogReader.varLogParser.parse()) {
			HoTTlogReader.recordSetVario.addPoints(HoTTlogReader.varLogParser.getPoints(), HoTTlogReader.varLogParser.getTimeStep_ms());
		}
		HoTTlogReader.isJustParsed = true;
	}

	public static class VarLogParser extends LogParser {
		private int			tmpHeight							= 0;
		private int			tmpClimb10						= 0;

		protected VarLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer) {
			super(pickerParameters, points, timeSteps_ms, buffer, Sensor.VARIO);
			this.buf = buffer;
			this.points[2] = 100000; //valuesVario[2] = 100000;
		}

		@Override
		protected boolean parse() {
		//log.log(Level.OFF, StringHelper.byte2Hex4CharString(buf, buf.length));
		//0=RXSQ, 1=Altitude, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx 7=EventVario
		//10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//sensor byte: 26=sensor byte
		//27=inverseBits 28,29=altitude 30,31=altitude_max 32,33=altitude_min 34,35=climb1 36,37=climb3 38,39=climb10
		this.points[0] = (buf[16] & 0xFF) * 1000;
		this.tmpHeight = HoTTlogReader.isHoTTAdapter2 ? DataParser.parse2Short(buf, 28) - 500 : DataParser.parse2Short(buf, 28);
		this.points[1] = this.tmpHeight * 1000;
		//pointsVarioMax = DataParser.parse2Short(buf1, 30) * 1000;
		//pointsVarioMin = DataParser.parse2Short(buf1, 32) * 1000;
		this.points[2] = (DataParser.parse2UnsignedShort(buf, 34) - 30000) * 10;
		this.tmpClimb10 = HoTTlogReader.isHoTTAdapter2 ? (DataParser.parse2UnsignedShort(buf , 38) - 30000) * 10 : DataParser.parse2UnsignedShort(buf , 38) * 1000;
		this.points[3] = HoTTlogReader.isHoTTAdapter2 ? (DataParser.parse2UnsignedShort(buf, 36) - 30000) * 10 : DataParser.parse2UnsignedShort(buf, 36) * 1000;
		this.points[4] = this.tmpClimb10;
		this.points[5] = (buf[13] & 0xFF) * 1000;				//voltageRx
		this.points[6] = (buf[14] & 0xFF) * 1000;				//temperaturRx
		this.points[7] = (buf[27] & 0x3F) * 1000; 			//inverse event
		
		if ((this.buf[65] & 0xFF) > 100 && (this.buf[65] & 0xFF) < 120) { //SM MicroVario starts with FW version 1.00 -> 100
			try {
				this.points[8] = Integer.parseInt(String.format(Locale.ENGLISH, "%c%c%c%c%c0", buf[40], buf[41], buf[42], buf[44], buf[45]).trim());
				this.points[9] = Integer.parseInt(String.format(Locale.ENGLISH, "%c%c%c%c%c0", buf[47], buf[48], buf[49], buf[51], buf[52]).trim());
				this.points[10] = Integer.parseInt(String.format(Locale.ENGLISH, "%c%c%c%c%c0", buf[54], buf[55], buf[56], buf[58], buf[59]).trim());
			}
			catch (NumberFormatException e) {
				byte[] tmpArray = new byte[21];
				System.arraycopy(buf, 40, tmpArray, 0, tmpArray.length);
				log.log(Level.WARNING, new String(tmpArray));
			}
			this.points[11] = (buf[64] & 0xFF) * 1000; //AirSpped/2
			this.points[12] = (buf[65] & 0xFF) * 1000; //SM MicroVario starts with FW version 1.00 -> 100
		}

		if (log.isLoggable(Level.FINER)) {
			printSensorValues(buf, this.points, 8);
		}
		return true;
		}

		/**
		 * point value migration used by HoTTlogHistoReader2
		 */
		@Override
		public void migratePoints(int[] targetPoints) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10
			if (pickerParameters.altitudeClimbSensorSelection == Sensor.VARIO.ordinal())
				System.arraycopy(this.points, 1, targetPoints, 10, 4);
			
			//out 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
			System.arraycopy(this.points, 7, targetPoints, 14, 6);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 3 and add points to record set
	 *
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddGPS(byte[] _buf, long timeStep_ms) throws DataInconsitsentException {
		System.arraycopy(_buf, 0, buf, 0, _buf.length);
		HoTTlogReader.gpsLogParser.timeStep_ms = timeStep_ms;
		if (HoTTlogReader.gpsLogParser.parse()) {
			HoTTlogReader.recordSetGPS.addPoints(HoTTlogReader.gpsLogParser.getPoints(), HoTTlogReader.gpsLogParser.getTimeStep_ms());
		}
		HoTTlogReader.isJustParsed = true;
	}

	public static class GpsLogParser extends LogParser {
		
		private long		timeStep_ms						= 0;
		private int			tmpHeight							= 0;
		private int			tmpClimb1							= 0;
		private int			tmpClimb3							= 0;
		private int			tmpVelocity						= 0;
		private int			tmpLatitude						= 0;
		private int			tmpLongitude					= 0;

		protected GpsLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer) {
			super(pickerParameters, points, timeSteps_ms, buffer, Sensor.GPS);
			this.buf = buffer;
		}

		@Override
		protected boolean parse() {
			//log.log(Level.OFF, StringHelper.byte2Hex4CharString(buf, buf.length));
		  //0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripLength, 10=VoltageRx, 11=TemperatureRx 12=satellites 13=GPS-fix 14=EventGPS 
			//15=HomeDirection 16=Roll 17=Pitch 18=Yaw 19=GyroX 20=GyroY 21=GyroZ 22=Vibration 23=Version	
			//sensor byte: 26=sensor byte
			//27,28=InverseBits 29=moveDirection 30,31=speed 32,33,34,35,36=latitude 37,38,39,40,41=longitude 42,43=distanceStart 44,45=altitude
			//46,47=climb1 48=climb3 49=#satellites 50=GPS-Fix 51=homeDirection 52=Roll 53=Pitch 54=Yaw 55,56=GyroX 57,58=GyroY 59,60=GyroZ
			//61=Vibration 62-65=freeChars 66=Version
			this.points[0] = (buf[16] & 0xFF) * 1000;
			this.tmpHeight = HoTTlogReader.isHoTTAdapter2 ? DataParser.parse2Short(buf, 44) - 500 : DataParser.parse2Short(buf, 44);
			this.tmpClimb1 = HoTTlogReader.isHoTTAdapter2 ? (DataParser.parse2UnsignedShort(buf, 46) - 30000) : DataParser.parse2UnsignedShort(buf, 46);
			this.tmpClimb3 = HoTTlogReader.isHoTTAdapter2 ? (buf[48] & 0xFF) - 120 : (buf[48] & 0xFF);
			this.tmpVelocity = DataParser.parse2UnsignedShort(buf, 30) * 1000;
			this.points[6] = this.tmpVelocity;
			this.tmpLatitude = DataParser.parse2UnsignedShort(buf, 33) * 10000 + DataParser.parse2UnsignedShort(buf, 35);
			this.tmpLatitude =  buf[32] == 1 ? -1 * this.tmpLatitude : this.tmpLatitude;
			this.points[1] = this.tmpLatitude;
			this.tmpLongitude = DataParser.parse2UnsignedShort(buf, 38) * 10000 + DataParser.parse2UnsignedShort(buf, 40);
			this.tmpLongitude =  buf[37] == 1 ? -1 * this.tmpLongitude : this.tmpLongitude;
			this.points[2] = this.tmpLongitude;
			this.points[3] = this.tmpHeight * 1000;		//altitude
			this.points[4] = HoTTlogReader.isHoTTAdapter2 ? this.tmpClimb1 * 10 : this.tmpClimb1 * 1000;			//climb1
			this.points[5] = this.tmpClimb3 * 1000;		//climb3
			this.points[7] = DataParser.parse2UnsignedShort(buf, 42) * 1000;
			this.points[8] = (buf[51] & 0xFF) * 1000;
			this.points[9] = 0; 																//trip length
			this.points[10] = (buf[13] & 0xFF) * 1000;				//voltageRx
			this.points[11] = (buf[14] & 0xFF) * 1000;				//temperaturRx
			this.points[12] = (buf[49] & 0xFF) * 1000;
			switch (buf[50]) { //sat-fix
			case '-':
				this.points[13] = 0;
				break;
			case '2':
				this.points[13] = 2000;
				break;
			case '3':
				this.points[13] = 3000;
				break;
			case 'D':
				this.points[13] = 4000;
				break;
			default:
				try {
					this.points[13] = Integer.valueOf(String.format("%c", 0xFF & buf[50])) * 1000;
				}
				catch (NumberFormatException e1) {
					this.points[13] = 1000;
				}
				break;
			}
			//this.points[14] = DataParser.parse2Short(buf, 27) * 1000; //inverse event including byte 2 valid GPS data
			this.points[14] = (buf[28] & 0xFF) * 1000; //inverse event
			//51=homeDirection 52=Roll 53=Pitch 54=Yaw
			this.points[15] = (buf[51] & 0xFF) * 1000; //15=HomeDirection		
			if ((buf[65] & 0xFF) > 100) { //SM GPS-Logger
				//52=Roll 53=Pitch 54=Yaw
				//16=ServoPulse 17=AirSpeeed 18=n/a
				this.points[16] = buf[52] * 1000;
				this.points[17] = DataParser.parse2UnsignedShort(buf, 53) * 1000;
				//19=GyroX 20=GyroY 21=GyroZ 	
				//55,56=GyroX 57,58=GyroY 59,60=GyroZ
				this.points[19] = DataParser.parse2Short(buf, 55) * 1000;
				this.points[20] = DataParser.parse2Short(buf, 57) * 1000;
				this.points[21] = DataParser.parse2Short(buf, 59) * 1000;
				//22=ENL 			
				//61=Vibration 62-64=freeChars 65=Version
				this.points[22] = (buf[61] & 0xFF) * 1000;
			}
			else if ((buf[65] & 0xFF) == 4) { //RCE Sparrow
				//16=servoPulse 17=fixed 18=Voltage 19=GPS hh:mm 20=GPS sss.SSS 21=MSL Altitude 22=ENL 23=Version	
				this.points[16] = buf[60] * 1000;
				this.points[17] = 0;
				this.points[18] = buf[54] * 100; 
				//19=GPS hh:mm:sss.SSS 20=GPS sss.SSS 21=MSL Altitude 	
				//55,56=GPS hh:mm 57,58=GPS sss.SSS 59,60=MSL Altitude
				if (this.points[13] > 0) { //Sat-Fix
					int tmpTime = buf[55] * 10000000 + buf[56] * 100000 + buf[57] * 1000 + buf[58]*10;//HH:mm:ss.SSS
					if (tmpTime < this.points[19])
						log.log(Level.WARNING, String.format("near time: %s %s", StringHelper.getFormatedTime("HH:mm:ss.SSS", this.timeStep_ms - GDE.ONE_HOUR_MS), HoTTAdapter.getFormattedTime(tmpTime)));
					this.points[19] = tmpTime;
					int tmpDate = ((buf[61]-48) * 1000000 + (buf[63]-48) * 10000 + (buf[62]-48) * 100) * 10;//yy-MM-dd
					if (tmpDate < 0)
						log.log(Level.WARNING, String.format("near time: %s Sat-Fix %d #Sats %d %s - %c %c %c", StringHelper.getFormatedTime("HH:mm:ss.SSS",  this.timeStep_ms - GDE.ONE_HOUR_MS), this.points[13]/1000,  this.points[12]/1000, HoTTAdapter.getFormattedTime(this.points[19]), buf[61]&0xff, buf[63]&0xff, buf[62]&0xff));
					this.points[20] = tmpDate;
				}
				this.points[21] = (DataParser.parse2Short(buf, 52) - 500) * 1000; //TODO remove offset 500 after correction
				//22=Vibration 			
				//61=Vibration 62-64=freeChars 65=Version
				this.points[22] = (buf[59] & 0xFF) * 1000;
			}
			else if ((buf[65] & 0xFF) == 0 || (buf[65] & 0xFF) == 1) { //Graupner GPS need workaround to distinguish between different Graupner GPS version #0
				int version = this.points[23] == 1000 || (buf[52] != 0 && buf[53] != 0 && buf[54] != 0) ? 1 : 0;
					
				if (version == 0) { //#0=GPS 33600
					//16=Roll 17=Pitch 18=Yaw
					this.points[16] = buf[52] * 1000;
					this.points[17] = buf[53] * 1000;
					this.points[18] = buf[54] * 1000; 
					//19=GPS hh:mm 20=GPS sss.SSS 21=MSL Altitude 	
					//55,56=GPS hh:mm 57,58=GPS sss.SSS 59,60=MSL Altitude
					this.points[19] = buf[55] * 10000000 + buf[56] * 100000 + buf[57] * 1000 + buf[58]*10;//HH:mm:ss.SSS
					this.points[20] = 0;
					this.points[21] = DataParser.parse2Short(buf, 59) * 1000;
					//22=Vibration 			
					//61=Vibration 62-64=freeChars 65=Version
					this.points[22] = (buf[61] & 0xFF) * 1000;
				}
				else { //#1= 33602/S8437
					//16=velN NED north velocity mm/s 17=n/a 18=sAcc Speed accuracy estimate cm/s
					this.points[16] = DataParser.parse2Short(buf, 52) * 1000;
					this.points[17] = 0;
					this.points[18] = buf[54] * 1000; 
					//19=GPS hh:mm 20=GPS sss.SSS 21=velE NED east velocity mm/s
					//55,56=GPS hh:mm 57,58=GPS sss.SSS 59,60=velocityEast
					this.points[19] = buf[55] * 10000000 + buf[56] * 100000 + buf[57] * 1000 + buf[58]*10;//HH:mm:ss.SSS
					this.points[20] = 0;
					this.points[21] = DataParser.parse2Short(buf, 59) * 1000;
					//22=hAcc Horizontal accuracy estimate HDOP 			
					//61=Vibration 62-64=freeChars 65=Version
					this.points[22] = (buf[61] & 0xFF) * 1000;
				}
			}
			else { //unknown GPS
				//16=Roll 17=Pitch 18=Yaw 19=GPS time1 20=GPS time2 21=AltitudeMSL 22=Vibration
				this.points[16] = buf[52] * 1000;
				this.points[17] = buf[53] * 1000;
				this.points[18] = buf[54] * 1000; 
				this.points[19] = DataParser.parse2Short(buf, 55) * 1000;
				this.points[20] = DataParser.parse2Short(buf, 57) * 1000;
				this.points[21] = DataParser.parse2Short(buf, 59) * 1000;
				this.points[22] = (buf[61] & 0xFF) * 1000;
			}
			//three char
			//23=Version
			this.points[23] = (buf[65] & 0xFF) * 1000;

			if (log.isLoggable(Level.FINER)) {
				printSensorValues(buf, this.points, 23);
			}
			return true;
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if (pickerParameters.altitudeClimbSensorSelection == Sensor.GPS.ordinal()) {
				// 10=Altitude, 11=Climb 1, 12=Climb 3
				System.arraycopy(this.points, 3, targetPoints, 10, 3);
			}
			//out 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 
			targetPoints[20] = points[1];
			targetPoints[21] = points[2];
			for (int k = 0; k < 4; k++) {
				targetPoints[k + 22] = points[k + 6];
			}
			//out 26=NumSatellites 27=GPS-Fix 28=EventGPS
			for (int k = 0; k < 3; k++) {
				targetPoints[k + 26] = points[k + 12];
			}
			//out 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
			for (int k = 0; k < 9; k++) {
				targetPoints[k + 29] = points[k + 15];
			}
		}
	}
	
	/**
	 * parse the buffered data from buffer and add points to record set
	 *
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddGAM(byte[] _buf) throws DataInconsitsentException {
		if (HoTTlogReader.gamLogParser.parse()) {
			HoTTlogReader.recordSetGAM.addPoints(HoTTlogReader.gamLogParser.getPoints(), HoTTlogReader.gamLogParser.getTimeStep_ms());
		}
		HoTTlogReader.isJustParsed = true;
	}

	public static class GamLogParser extends LogParser {
		
		private int		tmpHeight		= 0;
		private int		tmpClimb3		= 0;
		private int		tmpVoltage1	= 0;
		private int		tmpVoltage2	= 0;
		private int		tmpCapacity	= 0;

		protected GamLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer) {
			super(pickerParameters, points, timeSteps_ms, buffer, Sensor.GAM);
			this.buf = buffer;
		}

		@Override
		protected boolean parse() {
			// 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance,
			// 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6,
			// 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel,
			// 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
			// 21=Speed, 22=LowestCellVoltage, 23=LowestCellNumber, 24=Pressure, 24=Event
			this.points[0] = (buf[16] & 0xFF) * 1000;
			this.tmpHeight = HoTTlogReader.isHoTTAdapter2 ? DataParser.parse2Short(buf, 46) - 500 : DataParser.parse2Short(buf, 46);
			this.tmpClimb3 = HoTTlogReader.isHoTTAdapter2 ? (buf[50] & 0xFF) - 120 : (buf[50] & 0xFF);
			this.tmpVoltage1 = DataParser.parse2Short(buf, 35);
			this.tmpVoltage2 = DataParser.parse2Short(buf, 37);
			this.tmpCapacity = DataParser.parse2Short(buf, 55);
			//sensor byte: 26=sensor byte
			//27,28=InverseBits 29=cell1, 30=cell2 31=cell3 32=cell4 33=cell5 34=cell6 35,36=voltage1 37,38=voltage2 39=temperature1 40=temperature2
			//41=? 42,43=fuel 44,45=rpm 46,47=altitude 48,49=climb1 50=climb3 51,52=current 53,54=voltage 55,56=capacity 57,58=speed
			//59=cellVoltage_min 60=#cellVoltage_min 61,62=rpm2 63=#error 64=pressure 65=version
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			this.points[1] = DataParser.parse2Short(buf, 53) * 1000;
			this.points[2] = DataParser.parse2Short(buf, 51) * 1000;
			this.points[3] = this.tmpCapacity * 1000;
			this.points[4] = Double.valueOf(this.points[1] / 1000.0 * this.points[2]).intValue();
			for (int j = 0; j < 6; j++) { //cell voltages
				this.points[j + 6] = (buf[j + 29] & 0xFF) * 1000;
				if (this.points[j + 5] > 0) {
					maxVotage = this.points[j + 6] > maxVotage ? this.points[j + 6] : maxVotage;
					minVotage = this.points[j + 6] < minVotage ? this.points[j + 6] : minVotage;
				}
			}
			this.points[5] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0; //balance
			this.points[12] = DataParser.parse2UnsignedShort(buf, 44) * 1000;
			this.points[13] = this.tmpHeight * 1000;
			this.points[14] = HoTTlogReader.isHoTTAdapter2 ? (DataParser.parse2UnsignedShort(buf, 48) - 30000) * 10 : (DataParser.parse2UnsignedShort(buf, 48) * 1000);
			this.points[15] = this.tmpClimb3 * 1000;
			this.points[16] = DataParser.parse2Short(buf, 42) * 1000;
			this.points[17] = this.tmpVoltage1 * 100;
			this.points[18] = this.tmpVoltage2 * 100;
			this.points[19] = ((buf[39] & 0xFF) - 20) * 1000;
			this.points[20] = ((buf[40] & 0xFF) - 20) * 1000;
			this.points[21] = DataParser.parse2UnsignedShort(buf, 57) * 1000; //Speed [km/h
			this.points[22] = (buf[59] & 0xFF) * 1000; //lowest cell voltage 124 = 2.48 V
			this.points[23] = (buf[60] & 0xFF) * 1000; //cell number lowest cell voltage
			this.points[24] = (buf[64] & 0xFF) * 1000; //Pressure
			this.points[25] = ((buf[27] & 0xFF) + ((buf[28] & 0x7F) << 8)) * 1000; //inverse event

			if (log.isLoggable(Level.FINER)) {
				printSensorValues(buf, this.points, 26);
			}
			return true;
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if (pickerParameters.altitudeClimbSensorSelection == Sensor.GAM.ordinal()) {
				//out 10=Altitude, 11=Climb 1, 12=Climb 3
				for (int k = 0; k < 3; k++) {
					targetPoints[k + 10] = points[k + 13];
				}
			}
			//out 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6, 49=Revolution G, 
			for (int j = 0; j < 12; j++) {
				targetPoints[j + 38] = points[j + 1];
			}
			//out 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
			for (int j = 0; j < 10; j++) {
				targetPoints[j + 50] = points[j + 16];
			}
		}
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 *
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddEAM(byte[] _buf) throws DataInconsitsentException {
		if (HoTTlogReader.eamLogParser.parse()) {
			HoTTlogReader.recordSetEAM.addPoints(HoTTlogReader.eamLogParser.getPoints(), HoTTlogReader.eamLogParser.getTimeStep_ms());
		}
		HoTTlogReader.isJustParsed = true;
	}

	public static class EamLogParser extends LogParser {
		protected int tmpHeight;
		protected int tmpClimb3;
		protected int tmpVoltage1;
		protected int tmpVoltage2;
		protected int tmpCapacity;

		protected EamLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer) {
			super(pickerParameters, points, timeSteps_ms, buffer, Sensor.EAM);
			this.buf = buffer;
		}

		@Override
		protected boolean parse() {
			//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14,
			//20=Altitude, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 27=RPM 28=MotorTime 29=Speed 30=Event
			//sensor byte: 26=sensor byte
			//27,28=InverseBits 29=cell1, 30=cell2 31=cell3 32=cell4 33=cell5 34=cell6 35=cell7 36=cell8 37=cell9 38=cell10 39=cell11 40=cell12 41=cell13 42=cell14
			//43,44=voltage1 45,46=voltage2 47=temperature1 48=temperature2 49,50=altitude 51,52=current 53,54=voltage 55,56=capacity 57,58=climb1 59=climb3
			//60,61=rpm 62,63=runtime>3A 64,65=speed
			this.points[0] = (buf[16] & 0xFF) * 1000;
			this.tmpHeight = HoTTlogReader.isHoTTAdapter2 ? DataParser.parse2Short(buf, 49) - 500 : DataParser.parse2Short(buf, 49);
			this.tmpClimb3 = HoTTlogReader.isHoTTAdapter2 ? (buf[59] & 0xFF) - 120 : (buf[59] & 0xFF);
			this.tmpVoltage1 = DataParser.parse2Short(buf, 43);
			this.tmpVoltage2 = DataParser.parse2Short(buf, 45);
			this.tmpCapacity = DataParser.parse2Short(buf, 55);
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			this.points[1] = DataParser.parse2Short(buf, 53) * 1000;
			this.points[2] = DataParser.parse2Short(buf, 51) * 1000;
			this.points[3] = this.tmpCapacity * 1000;
			this.points[4] = Double.valueOf(this.points[1] / 1000.0 * this.points[2]).intValue(); // power U*I [W];
			for (int j = 0; j < 14; j++) { //cell voltages
				this.points[j + 6] = (buf[j + 29] & 0xFF) * 1000;
				if (this.points[j + 6] > 0) {
					maxVotage = this.points[j + 6] > maxVotage ? this.points[j + 6] : maxVotage;
					minVotage = this.points[j + 6] < minVotage ? this.points[j + 6] : minVotage;
				}
			}
			this.points[5] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0; //balance
			this.points[20] = this.tmpHeight * 1000;
			this.points[21] = HoTTlogReader.isHoTTAdapter2 ? (DataParser.parse2UnsignedShort(buf, 57) - 30000) * 10 : DataParser.parse2UnsignedShort(buf, 57) * 1000;
			this.points[22] = this.tmpClimb3 * 1000;
			this.points[23] = this.tmpVoltage1 * 100;
			this.points[24] = this.tmpVoltage2 * 100;
			this.points[25] = ((buf[47] & 0xFF) - 20) * 1000;
			this.points[26] = ((buf[48] & 0xFF) - 20) * 1000;
			this.points[27] = DataParser.parse2UnsignedShort(buf, 60) * 1000;
			this.points[28] = ((buf[62] & 0xFF) * 60 + (buf[63] & 0xFF)) * 1000; // motor time
			this.points[29] = DataParser.parse2Short(buf, 64) * 1000; // speed
			this.points[30] = ((buf[27] & 0xFF) + ((buf[28] & 0x7F) << 8)) * 1000; //inverse event

			if (log.isLoggable(Level.FINER)) {
				printSensorValues(buf, this.points, 31);
			}
			return true;
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3
			if (pickerParameters.altitudeClimbSensorSelection == Sensor.EAM.ordinal()) {
				for (int j = 0; j < 3; j++) { //0=altitude 1=climb1 2=climb3
					targetPoints[j + 10] = points[j + 20];
				}
			}
			//out 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
			System.arraycopy(this.points, 1, targetPoints, 60, 19);
			//out 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
			System.arraycopy(this.points, 23, targetPoints, 79, 8);
		}
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 *
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddESC(byte[] _buf) throws DataInconsitsentException {
		if (HoTTlogReader.escLogParser.parse(HoTTlogReader.recordSetESC, HoTTlogReader.escLogParser.getTimeStep_ms())) {
			HoTTlogReader.recordSetESC.addPoints(HoTTlogReader.escLogParser.getPoints(), HoTTlogReader.escLogParser.getTimeStep_ms());
		}
		HoTTlogReader.isJustParsed = true;
	}

	public static class EscLogParser extends LogParser {
		protected int tmpVoltage;
		protected int tmpCurrent;
		protected int tmpCapacity;
		protected int tmpRevolution;
		protected int tmpTemperatureFet;
		protected final boolean	isChannelsChannel;

		protected EscLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer) {
			super(pickerParameters, points, timeSteps_ms, buffer, Sensor.ESC);
			this.buf = buffer;
			this.isChannelsChannel = Analyzer.getInstance().getActiveChannel().getNumber() == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER;
		}
		
		protected EscLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, Sensor sensor) {
			super(pickerParameters, points, timeSteps_ms, buffer, sensor);
			this.buf = buffer;
			this.isChannelsChannel = Analyzer.getInstance().getActiveChannel().getNumber() == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER;
		}

		@Override
		protected boolean parse() {
			return false;
		}
		
		protected boolean parse(RecordSet recordSet, long timeStep_ms) {
			//log.log(Level.OFF, StringHelper.byte2Hex4CharString(buf, buf.length));
			//0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature1, 7=Temperature2
			//8=Voltage_min, 9=Current_max, 10=Revolution_max, 11=Temperature1_max, 12=Temperature2_max 13=Event
			// 14=Speed 15=Speed_max 16=PWM 17=Throttle 18=VoltageBEC 19=VoltageBEC_max 20=CurrentBEC 21=TemperatureBEC 22=TemperatureBEC_max 
			// 23=Timing(empty) 24=Temperature_aux 25=Gear 26=YGEGenExt 27=MotStatEscNr 28=spare 29=VersionESC
			//sensor byte: 26=sensor byte
			//27,28=InverseBits 29,30=voltageIn 31,32=voltageIn_min 33,34=capacity 35=temperature1 36=temperature1_max 37,38=current 39,40=current_max
			//41,42=rpm 43,44=rpm_max 45=temperature2 46=temperature2_max
			//47,48=speed 49,50=speed_max 51=PWM 52=Throttle 53=VoltageBEC 54=VoltageBEC_max 55,56=CurrentBEC 57=TemperatureBEC 58=TemperatureBEC_max
			//59=Timing(empty) 60=Temperature_aux 61,62=Gear 63=YGEGenExt 64=MotStatEscNr 65=misc ESC_15 66=VersionESC
			this.points[0] = (buf[16] & 0xFF) * 1000;
			this.tmpVoltage = DataParser.parse2Short(buf, 29);
			this.tmpCurrent = DataParser.parse2Short(buf, 37);
			this.tmpCapacity = DataParser.parse2Short(buf, 33);
			this.tmpRevolution = DataParser.parse2UnsignedShort(buf, 41);
			this.tmpTemperatureFet = (buf[35] & 0xFF) - 20;
			//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M
			if (!pickerParameters.isFilterEnabled || this.tmpVoltage > 0 && this.tmpVoltage < 1000 && this.tmpCurrent < 4000 && this.tmpCurrent > -10
					&& this.tmpRevolution > -1 && this.tmpRevolution < 20000
					&& !(this.points[6] != 0 && this.points[6] / 1000 - this.tmpTemperatureFet > 20)) {
				this.points[1] = this.tmpVoltage * 1000;
				this.points[2] = this.tmpCurrent * 1000;
				if (!pickerParameters.isFilterEnabled || recordSet.getRecordDataSize(true) <= 20
						|| (this.tmpCapacity != 0 && Math.abs(this.tmpCapacity) <= (this.points[3] / 1000 + this.tmpVoltage * this.tmpCurrent / 2500 + 2))) {
					this.points[3] = this.tmpCapacity * 1000;
				}
				else {
					if (this.tmpCapacity != 0)
						HoTTlogReader2.log.log(Level.WARNING, StringHelper.getFormatedTime("mm:ss.SSS", timeStep_ms) + " - " + this.tmpCapacity + " - "
							+ (this.points[3] / 1000) + " + " + (this.tmpVoltage * this.tmpCurrent / 2500 + 2));
				}
				this.points[4] = Double.valueOf(this.points[1] / 1000.0 * this.points[2]).intValue();
				this.points[5] = this.tmpRevolution * 1000;
				this.points[6] = this.tmpTemperatureFet * 1000;
				this.points[7] = ((buf[45] & 0xFF) - 20) * 1000;
				this.points[8] = DataParser.parse2Short(buf, 31) * 1000;
				this.points[9] = DataParser.parse2Short(buf, 39) * 1000;
				this.points[10] = DataParser.parse2UnsignedShort(buf, 43) * 1000;
				this.points[11] = ((buf[36] & 0xFF) - 20) * 1000;
				this.points[12] = ((buf[46] & 0xFF) - 20) * 1000;
			}
			this.points[13] = ((buf[27] & 0xFF) + ((buf[28] & 0x7F) << 8)) * 1000; //inverse event
			
			if ((buf[65] & 0xFF) == 3) { //Extended YGE protocol 				
				//14=Speed 15=Speed_max 16=PWM 17=Throttle 18=VoltageBEC 19=VoltageBEC_max 20=CurrentBEC 21=TemperatureBEC 22=TemperatureBEC_max 
				//23=Timing(empty) 24=Temperature_aux 25=Gear 26=YGEGenExt 27=MotStatEscNr 28=VersionESC
				//47,48=speed 49,50=speed_max 51=PWM 52=Throttle 53=VoltageBEC 54=VoltageBEC_max 55,56=CurrentBEC 57=TemperatureBEC 58=TemperatureBEC_max
				//59=Timing(empty) 60=Temperature_aux 61,62=Gear 63=YGEGenExt 64=MotStatEscNr 65=VersionESC
				this.points[14] = DataParser.parse2Short(buf, 47) * 1000; //Speed
				this.points[15] = DataParser.parse2Short(buf, 49) * 1000; //Speed max
				this.points[16] = (buf[51] & 0xFF) * 1000; 								//PWM
				this.points[17] = (buf[52] & 0xFF) * 1000; 								//Throttle
				this.points[18] = (buf[53] & 0xFF) * 1000; 								//BEC Voltage
				this.points[19] = (buf[54] & 0xFF) * 1000; 								//BEC Voltage min
				this.points[20] = DataParser.parse2UnsignedShort(buf, 55) * 1000; 	//BEC Current
				this.points[21] = ((buf[57] & 0xFF) - 20) * 1000; 				//BEC Temperature
				this.points[22] = ((buf[58] & 0xFF) - 20) * 1000; 				//Capacity Temperature
				this.points[23] = (buf[59] & 0xFF) * 1000; 								//Timing
				this.points[24] = ((buf[60] & 0xFF) - 20) * 1000; 				//Aux Temperature
				this.points[25] = DataParser.parse2Short(buf, 61) * 1000; //Gear
				this.points[26] = (buf[63] & 0xFF) * 1000; 								//YGEGenExt
				this.points[27] = (buf[64] & 0xFF) * 1000; 								//MotStatEscNr
				this.points[28] = 0; 																			//spare
				this.points[29] = (buf[65] & 0xFF) * 1000; 								//Version ESC
			}
			else if ((buf[65] & 0xFF) >= 128) { //Extended CB-Electronics
				//14=AirSpeed 15=AirSpeed_max 16=PWM 17=Throttle 18=VoltagePump 19=VoltagePump_min 20=Flow 21=Fuel 22=Power 
				//23=Thrust 24=TemperaturePump 25=EngineStat 26=spare 27=spare 28=spare 29=version
				//47,48=speed 49,50=speed_max 51=PWM 52=Throttle 53=VoltageECU 54=VoltageBEC_max 55,56=CurrentBEC 57=TemperatureBEC 58=TemperatureBEC_max
				//59=Timing(empty) 60=Temperature_aux 61,62=Gear 63=YGEGenExt 64=MotStatEscNr 65=VersionESC
				this.points[14] = DataParser.parse2Short(buf, 47) * 1000; //AirSpeed
				this.points[15] = DataParser.parse2Short(buf, 49) * 1000; //AirSpeed max
				this.points[16] = (buf[51] & 0xFF) * 1000; 								//PWM
				this.points[17] = (buf[52] & 0xFF) * 1000; 								//Throttle
				this.points[18] = (buf[53] & 0xFF) * 1000; 								//Pump Voltage
				this.points[19] = (buf[54] & 0xFF) * 1000; 								//Pump Voltage min
				this.points[20] = DataParser.parse2UnsignedShort(buf, 55) * 1000; 	//Flow
				this.points[21] = DataParser.parse2UnsignedShort(buf, 57) * 1000;		//Fuel ml
				this.points[22] = DataParser.parse2UnsignedShort(buf, 59) * 1000; 	//Power Wh
				this.points[23] = DataParser.parse2UnsignedShort(buf, 61) * 1000;   //Thrust
				this.points[24] = ((buf[63] & 0xFF) - 20) * 1000; 				//Pump Temperature
				this.points[25] = (buf[64] & 0xFF) * 1000; 								//Engine run
				this.points[26] = 0; 																			//spare
				this.points[27] = 0; 																			//spare
				this.points[28] = 0; 																			//spare
				this.points[29] = (buf[65] & 0xFF) * 1000; 								//Version ESC			
			}

			//enable binary output for enhanced ESC data
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, StringHelper.byte2Hex2CharString(buf, buf.length));
			
			if (log.isLoggable(Level.FINER)) {
				printSensorValues(buf, this.points, 14);
			}
			return true;
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if (this.isChannelsChannel)
				//out 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max, 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
				//out 120=Speed 121=Speed_max 122=PWM 123=Throttle 124=VoltageBEC 125=VoltageBEC_min 125=CurrentBEC 127=TemperatureBEC 128=TemperatureCap 
				//out 129=Timing(empty) 130=Temperature_aux 131=Gear 132=YGEGenExt 133=MotStatEscNr 134=misc ESC_15 135=VersionESC
				for (int j = 0; j < 29; j++) {
					targetPoints[j + 107] = this.points[j + 1];
				}
			else
				//out 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max, 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M
				//out 100=Speed 101=Speed_max 102=PWM 103=Throttle 104=VoltageBEC 105=VoltageBEC_min 106=CurrentBEC 107=TemperatureBEC 108=TemperatureCap 
				//out 109=Timing(empty) 110=Temperature_aux 111=Gear 112=YGEGenExt 113=MotStatEscNr 114=misc ESC_15 115=VersionESC
				for (int j = 0; j < 29; j++) {
					targetPoints[j + 87] = this.points[j + 1];
				}
		}

	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 *
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddESC2(byte[] _buf) throws DataInconsitsentException {
		if (HoTTlogReader.esc2LogParser.parse(HoTTlogReader.recordSetESC2, HoTTlogReader.esc2LogParser.getTimeStep_ms())) {
			HoTTlogReader.recordSetESC2.addPoints(HoTTlogReader.esc2LogParser.getPoints(), HoTTlogReader.esc2LogParser.getTimeStep_ms());
		}
		HoTTlogReader.isJustParsed = true;
	}

	public static class Esc2LogParser extends EscLogParser {

		protected Esc2LogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer) {
			super(pickerParameters, points, timeSteps_ms, buffer, Sensor.ESC2);
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if (this.isChannelsChannel)
				//out 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max, 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
				//out 120=Speed 121=Speed_max 122=PWM 123=Throttle 124=VoltageBEC 125=VoltageBEC_min 125=CurrentBEC 127=TemperatureBEC 128=TemperatureCap 
				//out 129=Timing(empty) 130=Temperature_aux 131=Gear 132=YGEGenExt 133=MotStatEscNr 134=misc ESC_15 135=VersionESC
				for (int j = 0; j < 29; j++) {
					targetPoints[j + 107] = this.points[j + 1];
				}
			else
				// 116=VoltageM, 117=CurrentM, 118=CapacityM, 119=PowerM, 120=RevolutionM, 121=TemperatureM 1, 122=TemperatureM 2 123=Voltage_min, 124=Current_max,
				// 125=Revolution_max, 126=Temperature1_max, 127=Temperature2_max 128=Event M
				// 129=Speed 130=Speed_max 131=PWM 132=Throttle 133=VoltageBEC 134=VoltageBEC_min 135=CurrentBEC 136=TemperatureBEC 137=TemperatureCap 
				// 138=Timing(empty) 139=Temperature_aux 140=Gear 141=YGEGenExt 142=MotStatEscNr 143=misc ESC_15 144=VersionESC
				for (int j = 0; j < 29; j++) {
					targetPoints[j + 116] = this.points[j + 1];
				}
		}

	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 *
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddESC3(byte[] _buf) throws DataInconsitsentException {
		if (HoTTlogReader.esc3LogParser.parse(HoTTlogReader.recordSetESC3, HoTTlogReader.esc3LogParser.getTimeStep_ms())) {
			HoTTlogReader.recordSetESC3.addPoints(HoTTlogReader.esc3LogParser.getPoints(), HoTTlogReader.esc3LogParser.getTimeStep_ms());
		}
		HoTTlogReader.isJustParsed = true;
	}

	public static class Esc3LogParser extends EscLogParser {

		protected Esc3LogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer) {
			super(pickerParameters, points, timeSteps_ms, buffer, Sensor.ESC3);
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if (this.isChannelsChannel)
				//out 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max, 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
				//out 120=Speed 121=Speed_max 122=PWM 123=Throttle 124=VoltageBEC 125=VoltageBEC_min 125=CurrentBEC 127=TemperatureBEC 128=TemperatureCap 
				//out 129=Timing(empty) 130=Temperature_aux 131=Gear 132=YGEGenExt 133=MotStatEscNr 134=misc ESC_15 135=VersionESC
				for (int j = 0; j < 29; j++) {
					targetPoints[j + 107] = this.points[j + 1];
				}
			else
				// 145=VoltageM2, 146=CurrentM, 147=CapacityM, 148=PowerM, 149=RevolutionM, 150=TemperatureM 1, 151=TemperatureM 2 152=Voltage_min, 153=Current_max,
				// 154=Revolution_max, 155=Temperature1_max, 156=Temperature2_max 157=Event M
				// 158=Speed 159=Speed_max 160=PWM 161=Throttle 162=VoltageBEC 163=VoltageBEC_min 164=CurrentBEC 165=TemperatureBEC 166=TemperatureCap 
				// 167=Timing(empty) 168=Temperature_aux 169=Gear 170=YGEGenExt 171=MotStatEscNr 172=misc ESC_15 173=VersionESC
				for (int j = 0; j < 29; j++) {
					targetPoints[j + 145] = this.points[j + 1];
				}
		}

	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 *
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddESC4(byte[] _buf) throws DataInconsitsentException {
		if (HoTTlogReader.esc4LogParser.parse(HoTTlogReader.recordSetESC4, HoTTlogReader.esc4LogParser.getTimeStep_ms())) {
			HoTTlogReader.recordSetESC4.addPoints(HoTTlogReader.esc4LogParser.getPoints(), HoTTlogReader.esc4LogParser.getTimeStep_ms());
		}
		HoTTlogReader.isJustParsed = true;
	}

	public static class Esc4LogParser extends EscLogParser {

		protected Esc4LogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer) {
			super(pickerParameters, points, timeSteps_ms, buffer, Sensor.ESC4);
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if (this.isChannelsChannel)
				//out 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max, 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
				//out 120=Speed 121=Speed_max 122=PWM 123=Throttle 124=VoltageBEC 125=VoltageBEC_min 125=CurrentBEC 127=TemperatureBEC 128=TemperatureCap 
				//out 129=Timing(empty) 130=Temperature_aux 131=Gear 132=YGEGenExt 133=MotStatEscNr 134=misc ESC_15 135=VersionESC
				for (int j = 0; j < 29; j++) {
					targetPoints[j + 107] = this.points[j + 1];
				}
			else
				// 174=VoltageM3, 175=CurrentM, 176=CapacityM, 177=PowerM, 178=RevolutionM, 179=TemperatureM 1, 180=TemperatureM 2 181=Voltage_min, 182=Current_max,
				// 183=Revolution_max, 184=Temperature1_max, 185=Temperature2_max 186=Event M
				// 187=Speed 188=Speed_max 189=PWM 190=Throttle 191=VoltageBEC 192=VoltageBEC_min 193=CurrentBEC 194=TemperatureBEC 195=TemperatureCap 
				for (int j = 0; j < 29; j++) {
					targetPoints[j + 174] = this.points[j + 1];
				}
		}

	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 *
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddChannel(byte[] _buf) throws DataInconsitsentException {
		HoTTlogReader.chnLogParser.parse();
		HoTTlogReader.recordSetChannel.addPoints(HoTTlogReader.chnLogParser.getPoints(), HoTTlogReader.chnLogParser.getTimeStep_ms());
	}

	public static class ChnLogParser extends LogParser {
		protected final int	numberUsedChannels;

		protected ChnLogParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer, int numberUsedChannels) {
			super(pickerParameters, points, timeSteps_ms, buffer, Sensor.CHANNEL);
			this.buf = buffer;
			this.numberUsedChannels = numberUsedChannels;
		}

		@Override
		protected boolean parse() {

			//0=FreCh, 1=Tx, 2=Rx, 3=Ch01, 4=Ch02 .. 18=Ch16 19=Ch17 .. 34=Ch32
			this.points[0] = buf[7] * -1000;
			this.points[1] = buf[8] * -1000;
			this.points[2] = buf[9] * -1000;

			for (int i = 0,j = 0; i < numberUsedChannels; i++,j+=2) {
				this.points[i + 3] = (DataParser.parse2UnsignedShort(buf, (66 + j)) / 2) * 1000;
			}
			//Ph(D)[4], Evt1(H)[5], Evt2(D)[6], Fch(D)[7], TXdBm(-D)[8], RXdBm(-D)[9], RfRcvRatio(D)[10], TrnRcvRatio(D)[11]
			//STATUS : Ph(D)[4], Evt1(H)[5], Evt2(D)[6], Fch(D)[7], TXdBm(-D)[8], RXdBm(-D)[9], RfRcvRatio(D)[10], TrnRcvRatio(D)[11]
			//S.INFOR : DEV(D)[22], CH(D)[23], SID(H)[24], WARN(H)[25]
			//remove evaluation of transmitter event and warning to avoid end user confusion
			//this.points[19] = (buf[5] & 0x01) * 100000; 	//power off
			//this.points[20] = (buf[1] & 0x01) * 50000;		//batt low
			//this.points[21] = (buf[5] & 0x04) * 25000;		//reset
			//if (buf[25] > 0) {
			//	this.points[22] = (buf[25] & 0x7F) * 1000;		//warning
			//}
			//else
			//	this.points[22] = 0;
			
			return true;
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			for (int j = 4; j < 7; j++) {
				targetPoints[j] = this.points[j];
			}
			// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
			for (int j = 87; j < 107; j++) {
				targetPoints[j] = this.points[j];
			}
			throw new UnsupportedOperationException("use in situ parsing");
		}
	}

}
