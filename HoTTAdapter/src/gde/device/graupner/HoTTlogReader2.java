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

    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.logging.Logger;

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
 * Class to read Graupner HoTT binary data as saved on SD-Cards
 * @author Winfried Brügmann
 */
public class HoTTlogReader2 extends HoTTlogReader {
	final static Logger	log							= Logger.getLogger(HoTTlogReader2.class.getName());
	static int[]				points;
	static RecordSet		recordSet;
	static boolean			isJustMigrated	= false;

	/**
	* read log data according to version 0
	* @param filePath
	* @param newPickerParameters
	* @throws IOException
	* @throws DataInconsitsentException
	*/
	public static synchronized void read(String filePath, PickerParameters newPickerParameters) throws Exception {
		final String $METHOD_NAME = "read";
		HoTTlogReader2.pickerParameters = newPickerParameters;
		HashMap<String, String> fileInfoHeader = getFileInfo(new File(filePath), newPickerParameters);
		HoTTlogReader2.detectedSensors = Sensor.getSetFromDetected(fileInfoHeader.get(HoTTAdapter.DETECTED_SENSOR));

		final File file = new File(fileInfoHeader.get(HoTTAdapter.FILE_PATH));
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		HoTTAdapter2 device = (HoTTAdapter2) HoTTlogReader2.application.getActiveDevice();
		HoTTlogReader2.isHoTTAdapter2 = HoTTlogReader2.application.getActiveDevice() instanceof HoTTAdapter2;
		int recordSetNumber = HoTTlogReader2.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		int channelNumber = HoTTlogReader2.pickerParameters.analyzer.getActiveChannel().getNumber();
		boolean isReceiverData = false;
		boolean isVarioData = false;
		boolean isGPSData = false;
		boolean isGeneralData = false;
		boolean isElectricData = false;
		boolean isEscData = false, isEsc2Data = false,  isEsc3Data = false,  isEsc4Data = false;
		HoTTlogReader2.recordSet = null;
		HoTTlogReader2.isJustMigrated = false;
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// ESC wo channels
		// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
		// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M
		// 100=Speed 101=Speed_max 102=PWM 103=Throttle 104=VoltageBEC 105=VoltageBEC_max 106=CurrentBEC 107=TemperatureBEC 108=TemperatureCap 
		// 109=Timing(empty) 110=Temperature_aux 111=Gear 112=YGEGenExt 113=MotStatEscNr 114=misc ESC_15 115=VersionESC

		// Channels
		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=Ch17 ... 118=Ch32 119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
		// ESC
		// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
		// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
		// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
		// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
		HoTTlogReader2.points = new int[device.getNumberOfMeasurements(channelNumber)];
		long[] timeSteps_ms = new long[] {0};
		int numberLogChannels = Integer.valueOf(fileInfoHeader.get("LOG NOB CHANNEL"));
		boolean isASCII = fileInfoHeader.get("LOG TYPE").contains("ASCII");
		int rawDataBlockSize = Integer.parseInt(fileInfoHeader.get(HoTTAdapter.RAW_LOG_SIZE));
		int asciiDataBlockSize = Integer.parseInt(fileInfoHeader.get(HoTTAdapter.ASCII_LOG_SIZE));
		HoTTlogReader2.dataBlockSize = isASCII ? asciiDataBlockSize : rawDataBlockSize;
		HoTTlogReader2.buf = new byte[HoTTlogReader2.dataBlockSize];
		int[] valuesRec = new int[10];
		int[] valuesChn = new int[39];
		int[] valuesVar = new int[13];
		int[] valuesGPS = new int[24];
		int[] valuesGAM = new int[26];
		int[] valuesEAM = new int[31];
		int[] valuesESC = new int[30];
		int[] valuesESC2 = new int[30];
		int[] valuesESC3 = new int[30];
		int[] valuesESC4 = new int[30];
		HoTTlogReader2.rcvLogParser = (RcvLogParser) Sensor.RECEIVER.createLogParser(HoTTlogReader2.pickerParameters, valuesRec, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader2.chnLogParser = (ChnLogParser) Sensor.CHANNEL.createLogParser(HoTTlogReader2.pickerParameters, valuesChn, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader2.varLogParser = (VarLogParser) Sensor.VARIO.createLogParser(HoTTlogReader2.pickerParameters, valuesVar, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader2.gpsLogParser = (GpsLogParser) Sensor.GPS.createLogParser(HoTTlogReader2.pickerParameters, valuesGPS, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader2.gamLogParser = (GamLogParser) Sensor.GAM.createLogParser(HoTTlogReader2.pickerParameters, valuesGAM, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader2.eamLogParser = (EamLogParser) Sensor.EAM.createLogParser(HoTTlogReader2.pickerParameters, valuesEAM, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader2.escLogParser = (EscLogParser) Sensor.ESC.createLogParser(HoTTlogReader2.pickerParameters, valuesESC, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader2.esc2LogParser = (Esc2LogParser) Sensor.ESC2.createLogParser(HoTTlogReader2.pickerParameters, valuesESC2, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader2.esc3LogParser = (Esc3LogParser) Sensor.ESC3.createLogParser(HoTTlogReader2.pickerParameters, valuesESC3, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReader2.esc4LogParser = (Esc4LogParser) Sensor.ESC4.createLogParser(HoTTlogReader2.pickerParameters, valuesESC4, timeSteps_ms, buf, numberLogChannels);
		int logTimeStep_ms = 1000/Integer.valueOf(fileInfoHeader.get("COUNTER").split("/")[1].split(GDE.STRING_BLANK)[0]);
		boolean isVarioDetected = false;
		boolean isGPSdetected = false;
		boolean isESCdetected = false, isESC2detected = false,  isESC3detected = false,  isESC4detected = false;
		int logDataOffset = Integer.valueOf(fileInfoHeader.get("LOG DATA OFFSET"));
		long numberDatablocks = Long.parseLong(fileInfoHeader.get(HoTTAdapter.LOG_COUNT));
		long startTimeStamp_ms = HoTTlogReader2.getStartTimeStamp(fileInfoHeader.get("LOG START TIME"), HoTTlogReader2.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks));
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTlogReader2.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);

		try {
			//receiver data are always contained
			channel = HoTTlogReader2.channels.get(channelNumber);
			String newFileDescription = HoTTlogReader2.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTlogReader2.application.getObjectKey() : date;
			if (channel.getFileDescription().length() <= newFileDescription.length() || (HoTTlogReader2.application.isObjectoriented() && !channel.getFileDescription().contains(HoTTlogReader2.application.getObjectKey())))
				channel.setFileDescription(newFileDescription);
			recordSetName = recordSetNumber + device.getRecordSetStemNameReplacement() + recordSetNameExtend;
			HoTTlogReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
			channel.put(recordSetName, HoTTlogReader2.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			long sequenceNumber = 0, sequenceDelta = 0;
			//recordSet initialized and ready to add data

			log.log(Level.INFO, fileInfoHeader.toString());
			//read all the data blocks from the file and parse
			data_in.skip(logDataOffset);
			int i = 0;
			for (; i < numberDatablocks; i++) { //skip log entries before transmitter active
				data_in.read(HoTTlogReader2.buf);
				if (isASCII) { //convert ASCII log data to hex
					HoTTlogReader.convertAscii2Raw(rawDataBlockSize, HoTTlogReader2.buf);
				}
				//log.logp(Level.OFF, HoTTlogReader2.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTlogReader2.buf, rawDataBlockSize));
				//log.log(Level.OFF, String.format("HoTTlogReader2.buf[8] == 0x%02X HoTTlogReader2.buf[9] == 0x%02X HoTTlogReader2.buf[24] == 0x%02X", HoTTlogReader2.buf[8], HoTTlogReader2.buf[9], HoTTlogReader2.buf[24]));
				log.log(Level.INFO, "raw block data   " + StringHelper.byte2Hex2CharString(HoTTlogReader2.buf, 30));
				sequenceDelta = sequenceDelta == 0 ? 1 : DataParser.getUInt32(HoTTlogReader2.buf, 0) - sequenceNumber;
				timeSteps_ms[BinParser.TIMESTEP_INDEX] += logTimeStep_ms * sequenceDelta;// add default time step given by log msec
				sequenceNumber = DataParser.getUInt32(HoTTlogReader2.buf, 0);

				if (HoTTlogReader2.buf[8] == 0 || HoTTlogReader2.buf[9] == 0 || HoTTlogReader2.buf[24] == 0x1F) { // tx, rx, rx sensitivity data
					continue;
				}
				break;
			}
			for (; i < numberDatablocks; i++) {
				if (data_in.read(HoTTlogReader2.buf) < dataBlockSize) {
					log.log(Level.OFF, "numberDataBlocks = " + (i-1));
					break;
				}
				if (log.isLoggable(Level.FINE)) {
					if (isASCII)
						log.logp(Level.FINE, HoTTlogReader2.$CLASS_NAME, $METHOD_NAME, new String(HoTTlogReader2.buf));
					else if (HoTTlogReader2.buf[24] != 0x1F)
						log.logp(Level.FINE, HoTTlogReader2.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTlogReader2.buf, HoTTlogReader2.buf.length));
				}

				if (isASCII) { //convert ASCII log data to hex
					HoTTlogReader.convertAscii2Raw(rawDataBlockSize, HoTTlogReader2.buf);
				}
				
				//Ph(D)[4], Evt1(H)[5], Evt2(D)[6], Fch(D)[7], TXdBm(-D)[8], RXdBm(-D)[9], RfRcvRatio(D)[10], TrnRcvRatio(D)[11]
				//STATUS : Ph(D)[4], Evt1(H)[5], Evt2(D)[6], Fch(D)[7], TXdBm(-D)[8], RXdBm(-D)[9], RfRcvRatio(D)[10], TrnRcvRatio(D)[11]
				//S.INFOR : DEV(D)[22], CH(D)[23], SID(H)[24], WARN(H)[25]
				//if (!HoTTAdapter.isFilterTextModus || (HoTTlogReader2.buf[6] & 0x01) == 0) { //switch into text modus
				//log.log(Level.OFF, String.format("HoTTlogReader2.buf[8] == 0x%02X HoTTlogReader2.buf[9] == 0x%02X HoTTlogReader2.buf[24] == 0x%02X", HoTTlogReader2.buf[8], HoTTlogReader2.buf[9], HoTTlogReader2.buf[24]));
				if (HoTTlogReader2.buf[24] == 0x1F) {//!rx sensitivity data
					if (log.isLoggable(Level.INFO))
						log.log(Level.INFO, "sensitivity data " + StringHelper.byte2Hex2CharString(HoTTlogReader2.buf, 30));
					sequenceDelta = DataParser.getUInt32(HoTTlogReader2.buf, 0) - sequenceNumber;
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += logTimeStep_ms * sequenceDelta;// add default time step given by log msec
					sequenceNumber = DataParser.getUInt32(HoTTlogReader2.buf, 0);
					continue; //skip rx sensitivity data
				}

				if (HoTTlogReader2.buf[8] != 0 && HoTTlogReader2.buf[9] != 0) { //buf 8, 9, tx,rx, rx sensitivity data
					if (log.isLoggable(Level.INFO)) {
						//log.log(Level.INFO, String.format("Sensor %02X", HoTTlogReader2.buf[26]));
						log.log(Level.INFO, "sensor data      " + StringHelper.byte2Hex2CharString(HoTTlogReader2.buf, 30));
					}

					HoTTlogReader2.rcvLogParser.trackPackageLoss(true);

					//create and fill sensor specific data record sets
					//fill receiver data
					isReceiverData = HoTTlogReader2.rcvLogParser.parse();
					System.arraycopy(valuesRec, 0, HoTTlogReader2.points, 0, 10); //copy receiver points

					if (channelNumber == 4) {
						HoTTlogReader2.chnLogParser.parse();
						//in 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16 19=PowerOff 20=BattLow 21=Reset 22=Warning
						//out 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
						System.arraycopy(valuesChn, 3, HoTTlogReader2.points, 87, 36); //copy channel data and events, warning
					}

					switch ((byte) (HoTTlogReader2.buf[26] & 0xFF)) { //actual sensor
					case HoTTAdapter.ANSWER_SENSOR_VARIO_19200:
						isVarioData = HoTTlogReader2.varLogParser.parse();
						if (isVarioData && isReceiverData) {
							migrateAddPoints(HoTTlogReader2.varLogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isEscData, isEsc2Data, isEsc3Data, isEsc4Data, channelNumber,
									valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
							isReceiverData = false;

							if (!isVarioDetected) {
								HoTTAdapter2.updateVarioTypeDependent((HoTTlogReader2.buf[65] & 0xFF), device, HoTTlogReader2.recordSet);
								isVarioDetected = true;
							}
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_GPS_19200:
						isGPSData = HoTTlogReader2.gpsLogParser.parse();
						if (isGPSData && isReceiverData) {
							migrateAddPoints(HoTTlogReader2.gpsLogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isEscData, isEsc2Data, isEsc3Data, isEsc4Data, channelNumber,
									valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
							isReceiverData = false;

							if (!isGPSdetected) {
								HoTTAdapter2.updateGpsTypeDependent((HoTTlogReader2.buf[65] & 0xFF), device, HoTTlogReader2.recordSet, 0);
								isGPSdetected = true;
							}
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_GENERAL_19200:
						isGeneralData = HoTTlogReader2.gamLogParser.parse();
						if (isGeneralData && isReceiverData) {
							migrateAddPoints(HoTTlogReader2.gamLogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isEscData, isEsc2Data, isEsc3Data, isEsc4Data, channelNumber,
									valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
							isReceiverData = false;
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_ELECTRIC_19200:
						isElectricData = HoTTlogReader2.eamLogParser.parse();
						if (isElectricData && isReceiverData) {
							migrateAddPoints(HoTTlogReader2.eamLogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isEscData, isEsc2Data, isEsc3Data, isEsc4Data, channelNumber,
									valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
							isReceiverData = false;
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_MOTOR_DRIVER_19200:
						isEscData = HoTTlogReader2.escLogParser.parse(HoTTlogReader2.recordSet, HoTTlogReader2.escLogParser.getTimeStep_ms());
						if (isEscData && isReceiverData) {
							migrateAddPoints(HoTTlogReader2.escLogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isEscData, isEsc2Data, isEsc3Data, isEsc4Data, channelNumber,
									valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
							isReceiverData = false;

							if (!isESCdetected) {
								HoTTAdapter2.updateEscTypeDependent((HoTTlogReader2.buf[65] & 0xFF), device, HoTTlogReader2.recordSet, 1);
								isESCdetected = true;
							}
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_ESC2_19200:
						if (channelNumber == 6) {
							isEsc2Data = HoTTlogReader2.esc2LogParser.parse(HoTTlogReader2.recordSet, HoTTlogReader2.esc2LogParser.getTimeStep_ms());
							if (isEsc2Data && isReceiverData) {
								migrateAddPoints(HoTTlogReader2.esc2LogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isEscData, isEsc2Data, isEsc3Data, isEsc4Data, channelNumber,
										valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
								isReceiverData = false;

								if (!isESC2detected) {
									HoTTAdapter2.updateEscTypeDependent((HoTTlogReader2.buf[65] & 0xFF), device, HoTTlogReader2.recordSet, 2);
									HoTTlogReader2.detectedSensors.add(Sensor.ESC2);
									isESC2detected = true;
								}
							}
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_ESC3_19200:
						if (channelNumber == 6) {
							isEsc3Data = HoTTlogReader2.esc3LogParser.parse(HoTTlogReader2.recordSet, HoTTlogReader2.esc3LogParser.getTimeStep_ms());
							if (isEsc3Data && isReceiverData) {
								migrateAddPoints(HoTTlogReader2.esc3LogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isEscData, isEsc2Data, isEsc3Data, isEsc4Data, channelNumber,
										valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
								isReceiverData = false;

								if (!isESC3detected) {
									HoTTAdapter2.updateEscTypeDependent((HoTTlogReader2.buf[65] & 0xFF), device, HoTTlogReader2.recordSet, 3);
									HoTTlogReader2.detectedSensors.add(Sensor.ESC3);
									isESC3detected = true;
								}
							}
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_ESC4_19200:
						if (channelNumber == 6) {
							isEsc4Data = HoTTlogReader2.esc4LogParser.parse(HoTTlogReader2.recordSet, HoTTlogReader2.esc4LogParser.getTimeStep_ms());
							if (isEsc4Data && isReceiverData) {
								migrateAddPoints(HoTTlogReader2.esc4LogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isEscData, isEsc2Data, isEsc3Data, isEsc4Data, channelNumber,
										valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
								isReceiverData = false;

								if (!isESC4detected) {
									HoTTAdapter2.updateEscTypeDependent((HoTTlogReader2.buf[65] & 0xFF), device, HoTTlogReader2.recordSet, 4);
									HoTTlogReader2.detectedSensors.add(Sensor.ESC4);
									isESC4detected = true;
								}
							}
						}
						break;
					case 0x1F: //receiver sensitive data
					default:
						break;
					}

					if (isReceiverData) { //this will only be true if no other sensor is connected
						HoTTlogReader2.recordSet.addPoints(HoTTlogReader2.points, HoTTlogReader2.rcvLogParser.getTimeStep_ms());
						isReceiverData = false;
					}
					else if (channelNumber == 4 && !HoTTlogReader2.isJustMigrated) { //this will only be true if no other sensor is connected and channel 4
						HoTTlogReader2.recordSet.addPoints(HoTTlogReader2.points, HoTTlogReader2.chnLogParser.getTimeStep_ms());
					}

					sequenceDelta = DataParser.getUInt32(HoTTlogReader2.buf, 0) - sequenceNumber;
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += logTimeStep_ms * sequenceDelta;// add default time step given by log msec
					sequenceNumber = DataParser.getUInt32(HoTTlogReader2.buf, 0);

					HoTTlogReader2.isJustMigrated = !HoTTlogReader2.rcvLogParser.updateLossStatistics();
					HoTTlogReader2.isJustMigrated = false;

					if (i % progressIndicator == 0) GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
				}
				else { //skip empty block, but add time step
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found rx=0 dBm");

					HoTTlogReader2.rcvLogParser.trackPackageLoss(false);

					if (channelNumber == 4 && HoTTlogReader2.recordSet.getRecordDataSize(true) > 0) {
						//fill receiver data
						isReceiverData = HoTTlogReader2.rcvLogParser.parse();
						System.arraycopy(valuesRec, 4, HoTTlogReader2.points, 4, 2); //Rx and Tx values only, keep other data since invalid

						HoTTlogReader2.chnLogParser.parse();
						//in 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16 19=PowerOff 20=BattLow 21=Reset 22=Warning
						//out 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
						System.arraycopy(valuesChn, 3, HoTTlogReader2.points, 87, 20); //copy channel data and events, warning
						HoTTlogReader2.recordSet.addPoints(HoTTlogReader2.points, HoTTlogReader2.chnLogParser.getTimeStep_ms());
					}

					sequenceDelta = DataParser.getUInt32(HoTTlogReader2.buf, 0) - sequenceNumber;
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += logTimeStep_ms * sequenceDelta;// add default time step given by log msec
					sequenceNumber = DataParser.getUInt32(HoTTlogReader2.buf, 0);
				}
			}

			Sensor altitudeClimbSensorSelection = pickerParameters.altitudeClimbSensorSelection == 0 ? null : Sensor.fromOrdinal(pickerParameters.altitudeClimbSensorSelection);
			if (pickerParameters.altitudeClimbSensorSelection == 0 || !detectedSensors.contains(Sensor.fromOrdinal(pickerParameters.altitudeClimbSensorSelection))) { //auto
				if (isElectricData && !isVarioData && !isGPSData && !isGeneralData)
					altitudeClimbSensorSelection = Sensor.EAM;
				else if (isGeneralData && !isVarioData && !isGPSData)
					altitudeClimbSensorSelection = Sensor.GAM;
				else if (isGPSData && !isVarioData)
					altitudeClimbSensorSelection = Sensor.GPS;
				else if (isVarioData)
					altitudeClimbSensorSelection = Sensor.VARIO;
			}
			HoTTlogReader2.rcvLogParser.finalUpdateLossStatistics();
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0 
					? String.format("%.1f", HoTTlogReader2.rcvLogParser.getLostPackages().percentage) 
					: "100";
			if (channelNumber == 4) 
				HoTTlogReader2.detectedSensors.add(Sensor.CHANNEL);
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { HoTTlogReader2.rcvLogParser.getLossTotal(), HoTTlogReader.rcvLogParser.getLostPackages().lossTotal, packageLossPercentage, HoTTlogReader.rcvLogParser.getLostPackages().getStatistics() })
					+ String.format(" - Sensor: %s", HoTTlogReader2.detectedSensors.toString())
					+ (altitudeClimbSensorSelection != null && (detectedSensors.contains(Sensor.fromOrdinal(pickerParameters.altitudeClimbSensorSelection)) || detectedSensors.contains(altitudeClimbSensorSelection))
							? String.format(" - %s = %s", Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGT2419), altitudeClimbSensorSelection)
							: ""));
			log.logp(Level.WARNING, HoTTlogReader2.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + HoTTlogReader2.rcvLogParser.getLostPackages().lossTotal); //$NON-NLS-1$
			log.logp(Level.TIME, HoTTlogReader2.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (GDE.isWithUi()) {
				GDE.getUiNotification().setProgress(99);
				device.makeInActiveDisplayable(HoTTlogReader2.recordSet);
				device.updateVisibilityStatus(HoTTlogReader2.recordSet, true);
				channel.applyTemplate(recordSetName, false);

				//write filename after import to record description
				HoTTlogReader2.recordSet.descriptionAppendFilename(file.getName());

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
	 * migrate sensor measurement values and add to record set, receiver data are always updated
	 * @param isVarioData
	 * @param isGPSData
	 * @param isGeneralData
	 * @param isElectricData
	 * @param isEscData
	 * @param channelNumber
	 * @throws DataInconsitsentException
	 */
	public static void migrateAddPoints(long timeStep_ms, boolean isVarioData, boolean isGPSData, boolean isGeneralData, boolean isElectricData, 
			boolean isEscData, boolean isEsc2Data, boolean isEsc3Data, boolean isEsc4Data, int channelNumber,
			int[] valuesVario, int[] valuesGPS, int[] valuesGAM, int[] valuesEAM, int[] valuesESC, int[] valuesESC2, int[] valuesESC3, int[] valuesESC4)
			throws DataInconsitsentException {
		//receiver data gets integrated each cycle
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6, 49=Revolution G, 
		// 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		//ESC		
		// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
		// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M
		// 100=Speed 101=Speed_max 102=PWM 103=Throttle 104=VoltageBEC 105=VoltageBEC_min 106=CurrentBEC 107=TemperatureBEC 108=TemperatureCap 
		// 109=Timing(empty) 110=Temperature_aux 111=Gear 112=YGEGenExt 113=MotStatEscNr 114=misc ESC_15 115=VersionESC
		//ESC2
		// 116=VoltageM, 117=CurrentM, 118=CapacityM, 119=PowerM, 120=RevolutionM, 121=TemperatureM 1, 122=TemperatureM 2 123=Voltage_min, 124=Current_max,
		// 125=Revolution_max, 126=Temperature1_max, 127=Temperature2_max 128=Event M
		// 129=Speed 130=Speed_max 131=PWM 132=Throttle 133=VoltageBEC 134=VoltageBEC_min 135=CurrentBEC 136=TemperatureBEC 137=TemperatureCap 
		// 138=Timing(empty) 139=Temperature_aux 140=Gear 141=YGEGenExt 142=MotStatEscNr 143=misc ESC_15 144=VersionESC
		//ESC3
		// 145=VoltageM2, 146=CurrentM, 147=CapacityM, 148=PowerM, 149=RevolutionM, 150=TemperatureM 1, 151=TemperatureM 2 152=Voltage_min, 153=Current_max,
		// 154=Revolution_max, 155=Temperature1_max, 156=Temperature2_max 157=Event M
		// 158=Speed 159=Speed_max 160=PWM 161=Throttle 162=VoltageBEC 163=VoltageBEC_min 164=CurrentBEC 165=TemperatureBEC 166=TemperatureCap 
		// 167=Timing(empty) 168=Temperature_aux 169=Gear 170=YGEGenExt 171=MotStatEscNr 172=misc ESC_15 173=VersionESC
		//ESC4
		// 174=VoltageM3, 175=CurrentM, 176=CapacityM, 177=PowerM, 178=RevolutionM, 179=TemperatureM 1, 180=TemperatureM 2 181=Voltage_min, 182=Current_max,
		// 183=Revolution_max, 184=Temperature1_max, 185=Temperature2_max 186=Event M
		// 187=Speed 188=Speed_max 189=PWM 190=Throttle 191=VoltageBEC 192=VoltageBEC_min 193=CurrentBEC 194=TemperatureBEC 195=TemperatureCap 
		// 196=Timing(empty) 197=Temperature_aux 198=Gear 199=YGEGenExt 200=MotStatEscNr 201=misc ESC_15 202=VersionESC

		// Channels
		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=Ch17 ... 118=Ch32
		// points.length = 136 -> 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
		// points.length = 152 -> 119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
		// ESC
		// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
		// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
		// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
		// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC

		//in 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14,
		//in 20=Altitude, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 27=RPM 28=MotorTime 29=Speed 30=Event
		if (isElectricData) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3
			for (int j = 0; !isVarioData && !isGPSData && !isGeneralData && j < 3; j++) { //0=altitude 1=climb1 2=climb3
				HoTTlogReader2.points[j + 10] = valuesEAM[j + 20];
			}
			//out 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
			System.arraycopy(valuesEAM, 1, HoTTlogReader2.points, 60, 19);
			//out 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
			System.arraycopy(valuesEAM, 23, HoTTlogReader2.points, 79, 8);
		}
		//in 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6,
		//in 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		//in 21=Speed, 22=LowestCellVoltage, 23=LowestCellNumber, 24=Pressure, 24=Event
		if (isGeneralData) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3
			for (int k = 0; !isVarioData && !isGPSData && k < 3; k++) {
				HoTTlogReader2.points[k + 10] = valuesGAM[k + 13];
			}
			//out 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6, 49=Revolution G, 
			for (int j = 0; j < 12; j++) {
				HoTTlogReader2.points[j + 38] = valuesGAM[j + 1];
			}
			//out 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
			for (int j = 0; j < 10; j++) {
				HoTTlogReader2.points[j + 50] = valuesGAM[j + 16];
			}
		}
		//in 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripLength, 10=VoltageRx, 11=TemperatureRx 12=satellites 13=GPS-fix 14=EventGPS
		if (isGPSData) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3
			for (int j = 0; !isVarioData && j < 3; j++) { //0=altitude 1=climb1 2=climb3
				HoTTlogReader2.points[j + 10] = valuesGPS[j + 3];
			}
			//out 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 
			HoTTlogReader2.points[20] = valuesGPS[1];
			HoTTlogReader2.points[21] = valuesGPS[2];
			for (int k = 0; k < 4; k++) {
				HoTTlogReader2.points[k + 22] = valuesGPS[k + 6];
			}
			//out 26=NumSatellites 27=GPS-Fix 28=EventGPS
			for (int k = 0; k < 3; k++) {
				HoTTlogReader2.points[k + 26] = valuesGPS[k + 12];
			}
			//out 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
			for (int k = 0; k < 9; k++) {
				HoTTlogReader2.points[k + 29] = valuesGPS[k + 15];
			}
		}
		//in 0=RXSQ, 1=Altitude, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx 7=Event 
		//in 8=accX 9=accY 10=accZ 11=reserved 12=version	if (isVarioData) {
		if (isVarioData) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
			for (int j = 0; j < 4; j++) {
				HoTTlogReader2.points[j + 10] = valuesVario[j + 1];
			}
			HoTTlogReader2.points[14] = valuesVario[7];

			//out 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
			for (int j = 0; j < 5; j++) {
				HoTTlogReader2.points[j + 15] = valuesVario[j + 8];
			}
		}
		//in 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature1, 7=Temperature2
		//in 8=Voltage_min, 9=Current_max, 10=Revolution_max, 11=Temperature1_max, 12=Temperature2_max 13=Event
		//in 14=Speed 15=Speed_max 16=PWM 17=Throttle 18=VoltageBEC 19=VoltageBEC_min 20=CurrentBEC 21=TemperatureBEC 22=TemperatureCap 
		//in 23=Timing(empty) 24=Temperature_aux 25=Gear 26=YGEGenExt 27=MotStatEscNr 28=misc ESC_15 29=VersionESC
		if (isEscData) {
			if (channelNumber == 4)
				//out 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
				//out 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
				//out 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
				//out 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
				for (int j = 0; j < 29; j++) {
					HoTTlogReader2.points[j + 123] = valuesESC[j + 1];
				}
			else
				//out 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max, 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M
				//out 100=Speed 101=Speed_max 102=PWM 103=Throttle 104=VoltageBEC 105=VoltageBEC_min 106=CurrentBEC 107=TemperatureBEC 108=TemperatureCap 
				//out 109=Timing(empty) 110=Temperature_aux 111=Gear 112=YGEGenExt 113=MotStatEscNr 114=misc ESC_15 115=VersionESC
				for (int j = 0; j < 29; j++) {
					HoTTlogReader2.points[j + 87] = valuesESC[j + 1];
				}
		}
		if (isEsc2Data) {
			if (channelNumber != 4)
				// 116=VoltageM, 117=CurrentM, 118=CapacityM, 119=PowerM, 120=RevolutionM, 121=TemperatureM 1, 122=TemperatureM 2 123=Voltage_min, 124=Current_max,
				// 125=Revolution_max, 126=Temperature1_max, 127=Temperature2_max 128=Event M
				// 129=Speed 130=Speed_max 131=PWM 132=Throttle 133=VoltageBEC 134=VoltageBEC_min 135=CurrentBEC 136=TemperatureBEC 137=TemperatureCap 
				// 138=Timing(empty) 139=Temperature_aux 140=Gear 141=YGEGenExt 142=MotStatEscNr 143=misc ESC_15 144=VersionESC
				for (int j = 0; j < 29; j++) {
					HoTTlogReader2.points[j + 116] = valuesESC2[j + 1];
				}
		}
		if (isEsc3Data) {
			if (channelNumber != 4)
				// 145=VoltageM2, 146=CurrentM, 147=CapacityM, 148=PowerM, 149=RevolutionM, 150=TemperatureM 1, 151=TemperatureM 2 152=Voltage_min, 153=Current_max,
				// 154=Revolution_max, 155=Temperature1_max, 156=Temperature2_max 157=Event M
				// 158=Speed 159=Speed_max 160=PWM 161=Throttle 162=VoltageBEC 163=VoltageBEC_min 164=CurrentBEC 165=TemperatureBEC 166=TemperatureCap 
				// 167=Timing(empty) 168=Temperature_aux 169=Gear 170=YGEGenExt 171=MotStatEscNr 172=misc ESC_15 173=VersionESC
				for (int j = 0; j < 29; j++) {
					HoTTlogReader2.points[j + 145] = valuesESC3[j + 1];
				}
		}
		if (isEsc4Data) {
			if (channelNumber != 4)
				// 174=VoltageM3, 175=CurrentM, 176=CapacityM, 177=PowerM, 178=RevolutionM, 179=TemperatureM 1, 180=TemperatureM 2 181=Voltage_min, 182=Current_max,
				// 183=Revolution_max, 184=Temperature1_max, 185=Temperature2_max 186=Event M
				// 187=Speed 188=Speed_max 189=PWM 190=Throttle 191=VoltageBEC 192=VoltageBEC_min 193=CurrentBEC 194=TemperatureBEC 195=TemperatureCap 
				for (int j = 0; j < 29; j++) {
					HoTTlogReader2.points[j + 174] = valuesESC4[j + 1];
				}
		}

		//add altitude and climb values from selected sensor
		switch (Sensor.VALUES[HoTTlogReader2.pickerParameters.altitudeClimbSensorSelection]) {
		case VARIO:
			//8=Altitude, 9=Climb 1, 10=Climb 3, 11=Climb 10
			if (isVarioData)
				for (int j = 0; j < 4; j++) {
				HoTTlogReader2.points[j + 10] = valuesVario[j + 1];
			}
			break;
		case GPS:
			//8=Altitude, 9=Climb 1, 10=Climb 3
			if (isGPSData)
				for (int j = 0; j < 3; j++) { //0=altitude 1=climb1 2=climb3
				HoTTlogReader2.points[j + 10] = valuesGPS[j + 3];
			}
			HoTTlogReader2.points[11] = 0;
			break;
		case GAM:
			//8=Altitude, 9=Climb 1, 10=Climb 3
			if (isGeneralData)
				for (int j = 0; j < 3; j++) {
				HoTTlogReader2.points[j + 10] = valuesGAM[j + 13];
			}
			HoTTlogReader2.points[11] = 0;
			break;
		case EAM:
			//8=Altitude, 9=Climb 1, 10=Climb 3
			if (isElectricData)
				for (int j = 0; j < 3; j++) { //0=altitude 1=climb1 2=climb3
				HoTTlogReader2.points[j + 10] = valuesEAM[j + 20];
			}
			HoTTlogReader2.points[11] = 0;
			break;
		default:
			break;
		}

		HoTTlogReader2.recordSet.addPoints(HoTTlogReader2.points, timeStep_ms);
		HoTTlogReader2.isJustMigrated = true;
	}
}
