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
public class HoTTlogReaderD extends HoTTlogReader2 {
	final static Logger	log					= Logger.getLogger(HoTTlogReaderD.class.getName());
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
		HoTTlogReaderD.pickerParameters = newPickerParameters;
		HashMap<String, String> fileInfoHeader = getFileInfo(new File(filePath), newPickerParameters);
		HoTTlogReaderD.detectedSensors = Sensor.getSetFromDetected(fileInfoHeader.get(HoTTAdapter.DETECTED_SENSOR));

		final File file = new File(fileInfoHeader.get(HoTTAdapter.FILE_PATH));
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		HoTTAdapter2 device = (HoTTAdapter2) HoTTlogReaderD.application.getActiveDevice();
		HoTTlogReaderD.isHoTTAdapter2 = HoTTlogReaderD.application.getActiveDevice() instanceof HoTTAdapter2;
		int recordSetNumber = HoTTlogReaderD.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		int channelNumber = 1; //fix channel
		boolean isReceiverData = false;
		boolean isVarioData = false;
		boolean isGPSData = false;
		boolean isGeneralData = false;
		boolean isElectricData = false;
		boolean isESCData = false, isESC2Data = false, isESC3Data = false, isESC4Data = false;
		HoTTlogReaderD.recordSet = null;
		HoTTlogReaderD.isJustMigrated = false;
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=Ch17 ... 118=Ch32, 119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
		// ESC1
		// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
		// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
		// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
		// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
		//ESC2
		// 152=VoltageM, 153=CurrentM, 154=CapacityM, 155=PowerM, 156=RevolutionM, 157=TemperatureM 1, 158=TemperatureM 2 159=Voltage_min, 160=Current_max,
		// 161=Revolution_max, 162=Temperature1_max, 163=Temperature2_max 164=Event M
		// 165=Speed 166=Speed_max 167=PWM 168=Throttle 169=VoltageBEC 170=VoltageBEC_min 171=CurrentBEC 172=TemperatureBEC 173=TemperatureCap 
		// 174=Timing(empty) 175=Temperature_aux 176=Gear 177=YGEGenExt 178=MotStatEscNr 179=misc ESC_15 180=VersionESC
		//ESC3
		// 181=VoltageM2, 182=CurrentM, 183=CapacityM, 184=PowerM, 185=RevolutionM, 186=TemperatureM 1, 187=TemperatureM 2 188=Voltage_min, 189=Current_max,
		// 190=Revolution_max, 191=Temperature1_max, 192=Temperature2_max 193=Event M
		// 194=Speed 195=Speed_max 196=PWM 197=Throttle 198=VoltageBEC 199=VoltageBEC_min 200=CurrentBEC 201=TemperatureBEC 202=TemperatureCap 
		// 203=Timing(empty) 204=Temperature_aux 205=Gear 206=YGEGenExt 207=MotStatEscNr 208=misc ESC_15 209=VersionESC
		//ESC4
		// 210=VoltageM3, 211=CurrentM, 212=CapacityM, 213=PowerM, 214=RevolutionM, 215=TemperatureM 1, 216=TemperatureM 2 217=Voltage_min, 218=Current_max,
		// 219=Revolution_max, 220=Temperature1_max, 221=Temperature2_max 222=Event M
		// 223=Speed 224=Speed_max 225=PWM 226=Throttle 227=VoltageBEC 228=VoltageBEC_min 229=CurrentBEC 230=TemperatureBEC 231=TemperatureCap 
		// 232=Timing(empty) 233=Temperature_aux 234=Gear 235=YGEGenExt 236=MotStatEscNr 237=misc ESC_15 238=VersionESC

		// 239=Test 00 240=Test 01.. 251=Test 12
		HoTTlogReaderD.points = new int[device.getNumberOfMeasurements(channelNumber)];
		long[] timeSteps_ms = new long[] {0};
		int numberLogChannels = Integer.valueOf(fileInfoHeader.get("LOG NOB CHANNEL"));
		boolean isASCII = fileInfoHeader.get("LOG TYPE").contains("ASCII");
		int rawDataBlockSize = Integer.parseInt(fileInfoHeader.get(HoTTAdapter.RAW_LOG_SIZE));
		int asciiDataBlockSize = Integer.parseInt(fileInfoHeader.get(HoTTAdapter.ASCII_LOG_SIZE));
    int[] valuesRec = new int[10];
    int[] valuesChn = new int[39];
    int[] valuesVar = new int[27];
    int[] valuesGPS = new int[24];
    int[] valuesGAM = new int[26];
    int[] valuesEAM = new int[31];
    int[] valuesESC = new int[30];
    int[] valuesESC2 = new int[30];
    int[] valuesESC3 = new int[30];
    int[] valuesESC4 = new int[30];
		HoTTlogReaderD.dataBlockSize = isASCII ? asciiDataBlockSize : rawDataBlockSize;
		HoTTlogReaderD.buf = new byte[HoTTlogReaderD.dataBlockSize];
		HoTTlogReaderD.rcvLogParser = (RcvLogParser) Sensor.RECEIVER.createLogParser(HoTTlogReaderD.pickerParameters, valuesRec, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReaderD.chnLogParser = (ChnLogParser) Sensor.CHANNEL.createLogParser(HoTTlogReaderD.pickerParameters, valuesChn, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReaderD.varLogParser = (VarLogParserD) Sensor.VARIO.createLogParserD(HoTTlogReaderD.pickerParameters, valuesVar, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReaderD.gpsLogParser = (GpsLogParser) Sensor.GPS.createLogParser(HoTTlogReaderD.pickerParameters, valuesGPS, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReaderD.gamLogParser = (GamLogParser) Sensor.GAM.createLogParser(HoTTlogReaderD.pickerParameters, valuesGAM, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReaderD.eamLogParser = (EamLogParser) Sensor.EAM.createLogParser(HoTTlogReaderD.pickerParameters, valuesEAM, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReaderD.escLogParser = (EscLogParser) Sensor.ESC.createLogParser(HoTTlogReaderD.pickerParameters, valuesESC, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReaderD.esc2LogParser = (EscLogParser) Sensor.ESC2.createLogParser(HoTTlogReaderD.pickerParameters, valuesESC2, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReaderD.esc3LogParser = (EscLogParser) Sensor.ESC3.createLogParser(HoTTlogReaderD.pickerParameters, valuesESC3, timeSteps_ms, buf, numberLogChannels);
		HoTTlogReaderD.esc4LogParser = (EscLogParser) Sensor.ESC4.createLogParser(HoTTlogReaderD.pickerParameters, valuesESC4, timeSteps_ms, buf, numberLogChannels);
		int logTimeStep_ms = 1000/Integer.valueOf(fileInfoHeader.get("COUNTER").split("/")[1].split(GDE.STRING_BLANK)[0]);
		boolean isVarioDetected = false;
		boolean isGPSdetected = false;
		boolean isESCdetected = false, isESC2detected = false, isESC3detected = false, isESC4detected = false;
		int logDataOffset = Integer.valueOf(fileInfoHeader.get("LOG DATA OFFSET"));
		long numberDatablocks = Long.parseLong(fileInfoHeader.get(HoTTAdapter.LOG_COUNT));
		long startTimeStamp_ms = HoTTlogReaderD.getStartTimeStamp(fileInfoHeader.get("LOG START TIME"), HoTTlogReaderD.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks));
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTlogReaderD.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);

		try {
			//receiver data are always contained
			channel = HoTTlogReaderD.channels.get(channelNumber);
			String newFileDescription = HoTTlogReaderD.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTlogReaderD.application.getObjectKey()	: date;
			if (channel.getFileDescription().length() <= newFileDescription.length() || (HoTTlogReaderD.application.isObjectoriented() && !channel.getFileDescription().contains(HoTTlogReaderD.application.getObjectKey())))
				channel.setFileDescription(newFileDescription);
			recordSetName = recordSetNumber + device.getRecordSetStemNameReplacement() + recordSetNameExtend;
			HoTTlogReaderD.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
			channel.put(recordSetName, HoTTlogReaderD.recordSet);
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
				data_in.read(HoTTlogReaderD.buf);
				if (isASCII) { //convert ASCII log data to hex
					HoTTlogReader.convertAscii2Raw(rawDataBlockSize, HoTTlogReaderD.buf);
				}
				//log.logp(Level.OFF, HoTTlogReaderD.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTlogReaderD.buf, rawDataBlockSize));
				log.log(Level.INFO, "raw block data   " + StringHelper.byte2Hex2CharString(HoTTlogReaderD.buf, 30));
				sequenceDelta = sequenceDelta == 0 ? 1 : DataParser.getUInt32(HoTTlogReaderD.buf, 0) - sequenceNumber;
				timeSteps_ms[BinParser.TIMESTEP_INDEX] += logTimeStep_ms * sequenceDelta;// add default time step given by log msec
				sequenceNumber = DataParser.getUInt32(HoTTlogReaderD.buf, 0);

				if (HoTTlogReaderD.buf[8] == 0 || HoTTlogReaderD.buf[9] == 0 || HoTTlogReaderD.buf[24] == 0x1F) { // tx, rx, rx sensitivity data
					continue;
				}
				break;
			}
			
			for (; i < numberDatablocks; i++) {
				data_in.read(HoTTlogReaderD.buf);
				if (log.isLoggable(Level.FINE)) {
					if (isASCII)
						log.logp(Level.FINE, HoTTlogReaderD.$CLASS_NAME, $METHOD_NAME, new String(HoTTlogReaderD.buf));
					else
						log.logp(Level.FINE, HoTTlogReaderD.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTlogReaderD.buf, HoTTlogReaderD.buf.length));
				}

				if (isASCII) { //convert ASCII log data to hex
					HoTTlogReader.convertAscii2Raw(rawDataBlockSize, HoTTlogReaderD.buf);
				}

				//Ph(D)[4], Evt1(H)[5], Evt2(D)[6], Fch(D)[7], TXdBm(-D)[8], RXdBm(-D)[9], RfRcvRatio(D)[10], TrnRcvRatio(D)[11]
				//STATUS : Ph(D)[4], Evt1(H)[5], Evt2(D)[6], Fch(D)[7], TXdBm(-D)[8], RXdBm(-D)[9], RfRcvRatio(D)[10], TrnRcvRatio(D)[11]
				//S.INFOR : DEV(D)[22], CH(D)[23], SID(H)[24], WARN(H)[25]
				//if (!HoTTAdapter.isFilterTextModus || (HoTTlogReaderD.buf[6] & 0x01) == 0) { //switch into text modus
				if (HoTTlogReaderD.buf[24] == 0x1F) {//rx sensitivity data
					if (log.isLoggable(Level.INFO))
						log.log(Level.INFO, "sensitivity data " + StringHelper.byte2Hex2CharString(HoTTlogReaderD.buf, HoTTlogReaderD.buf.length));
					sequenceDelta = DataParser.getUInt32(HoTTlogReaderD.buf, 0) - sequenceNumber;
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += logTimeStep_ms * sequenceDelta;// add default time step given by log msec
					sequenceNumber = DataParser.getUInt32(HoTTlogReaderD.buf, 0);
					continue; //skip rx sensitivity data
				}
				
				if (HoTTlogReaderD.buf[8] != 0 && HoTTlogReaderD.buf[9] != 0) { //buf 8, 9, tx,rx, rx sensitivity data
					if (log.isLoggable(Level.INFO)) {
						log.log(Level.INFO, "sensor data      " + StringHelper.byte2Hex2CharString(HoTTlogReaderD.buf, HoTTlogReaderD.buf.length));
					}
						
					HoTTlogReaderD.rcvLogParser.trackPackageLoss(true);

					//create and fill sensor specific data record sets
					if (log.isLoggable(Level.FINEST)) {
						log.logp(Level.FINEST, HoTTlogReaderD.$CLASS_NAME, $METHOD_NAME,
								StringHelper.byte2Hex2CharString(new byte[] { HoTTlogReaderD.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTlogReaderD.buf[7], false));
					}

					//fill receiver data
					//in 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					isReceiverData = HoTTlogReaderD.rcvLogParser.parse();
					System.arraycopy(valuesRec, 0, HoTTlogReaderD.points, 0, 10); //migrate/copy receiver points

					HoTTlogReaderD.chnLogParser.parse();
					//in 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16 19=PowerOff 20=BattLow 21=Reset 22=Warning
					//out 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserv
					System.arraycopy(valuesChn, 3, HoTTlogReaderD.points, 87, 36); //copy channel data and events, warning

					switch ((byte) (HoTTlogReaderD.buf[26] & 0xFF)) { //actual sensor
					case HoTTAdapter.ANSWER_SENSOR_VARIO_19200:
						isVarioData = HoTTlogReaderD.varLogParser.parse();
						if (isVarioData && isReceiverData) {
							migrateAddPoints(HoTTlogReaderD.varLogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isESCData, isESC2Data, isESC3Data, isESC4Data, channelNumber, valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
							isReceiverData = false;

							if (!isVarioDetected) {
								HoTTAdapter2.updateVarioTypeDependent((HoTTlogReaderD.buf[65] & 0xFF), device, HoTTlogReaderD.recordSet);
								isVarioDetected = true;
							}
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_GPS_19200:
						isGPSData = HoTTlogReaderD.gpsLogParser.parse();
						if (isGPSData && isReceiverData) {
							migrateAddPoints(HoTTlogReaderD.gpsLogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isESCData, isESC2Data, isESC3Data, isESC4Data, channelNumber, valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
							isReceiverData = false;

							if (!isGPSdetected) {
								HoTTAdapter2.updateGpsTypeDependent((HoTTlogReaderD.buf[65] & 0xFF), device, HoTTlogReaderD.recordSet, 0);
								isGPSdetected = true;
							}
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_GENERAL_19200:
						isGeneralData = HoTTlogReaderD.gamLogParser.parse();
						if (isGeneralData && isReceiverData) {
							migrateAddPoints(HoTTlogReaderD.gamLogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isESCData, isESC2Data, isESC3Data, isESC4Data, channelNumber, valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
							isReceiverData = false;
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_ELECTRIC_19200:
						isElectricData = HoTTlogReaderD.eamLogParser.parse();
						if (isElectricData && isReceiverData) {
							migrateAddPoints(HoTTlogReaderD.eamLogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isESCData, isESC2Data, isESC3Data, isESC4Data, channelNumber, valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
							isReceiverData = false;
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_MOTOR_DRIVER_19200:
						isESCData = HoTTlogReaderD.escLogParser.parse(HoTTlogReaderD.recordSet, HoTTlogReaderD.escLogParser.getTimeStep_ms());
						if (isESCData && isReceiverData) {
							migrateAddPoints(HoTTlogReaderD.escLogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isESCData, isESC2Data, isESC3Data, isESC4Data, channelNumber, valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
							isReceiverData = false;

							if (!isESCdetected) {
								HoTTAdapterD.updateEscTypeDependent((HoTTlogReaderD.buf[65] & 0xFF), device, HoTTlogReaderD.recordSet, 1);
								isESCdetected = true;
							}
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_ESC2_19200:
						isESC2Data = HoTTlogReaderD.esc2LogParser.parse(HoTTlogReaderD.recordSet, HoTTlogReaderD.esc2LogParser.getTimeStep_ms());
						if (isESC2Data && isReceiverData) {
							migrateAddPoints(HoTTlogReaderD.esc2LogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isESCData, isESC2Data, isESC3Data, isESC4Data, channelNumber,
									valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
							isReceiverData = false;

							if (!isESC2detected) {
								HoTTAdapterD.updateEscTypeDependent((HoTTlogReaderD.buf[65] & 0xFF), device, HoTTlogReaderD.recordSet, 2);
								HoTTlogReaderD.detectedSensors.add(Sensor.ESC2);
								isESC2detected = true;
							}
						} 
						break;
					case HoTTAdapter.ANSWER_SENSOR_ESC3_19200:
						isESC3Data = HoTTlogReaderD.esc3LogParser.parse(HoTTlogReaderD.recordSet, HoTTlogReaderD.esc3LogParser.getTimeStep_ms());
						if (isESC3Data && isReceiverData) {
							migrateAddPoints(HoTTlogReaderD.esc3LogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isESCData, isESC2Data, isESC3Data, isESC4Data, channelNumber,
									valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
							isReceiverData = false;

							if (!isESC3detected) {
								HoTTAdapterD.updateEscTypeDependent((HoTTlogReaderD.buf[65] & 0xFF), device, HoTTlogReaderD.recordSet, 3);
								HoTTlogReaderD.detectedSensors.add(Sensor.ESC3);
								isESC3detected = true;
							}
						} 
						break;
					case HoTTAdapter.ANSWER_SENSOR_ESC4_19200:
						isESC4Data = HoTTlogReaderD.esc4LogParser.parse(HoTTlogReaderD.recordSet, HoTTlogReaderD.esc4LogParser.getTimeStep_ms());
						if (isESC4Data && isReceiverData) {
							migrateAddPoints(HoTTlogReaderD.esc3LogParser.getTimeStep_ms(), isVarioData, isGPSData, isGeneralData, isElectricData, isESCData, isESC2Data, isESC3Data, isESC4Data, channelNumber,
									valuesVar, valuesGPS, valuesGAM, valuesEAM, valuesESC, valuesESC2, valuesESC3, valuesESC4);
							isReceiverData = false;

							if (!isESC4detected) {
								HoTTAdapterD.updateEscTypeDependent((HoTTlogReaderD.buf[65] & 0xFF), device, HoTTlogReaderD.recordSet, 4);
								HoTTlogReaderD.detectedSensors.add(Sensor.ESC4);
								isESC4detected = true;
							}
						} 
						break;
					case 0x1F: //receiver sensitive data
					default:
						break;
					}

					if (isReceiverData) { //this will only be true if no other sensor is connected
						HoTTlogReaderD.recordSet.addPoints(HoTTlogReaderD.points, HoTTlogReaderD.rcvLogParser.getTimeStep_ms());
						isReceiverData = false;
					}
					else if (!HoTTlogReaderD.isJustMigrated) { //this will only be true if no other sensor is connected
						HoTTlogReaderD.recordSet.addPoints(HoTTlogReaderD.points, HoTTlogReaderD.chnLogParser.getTimeStep_ms());
					}

					sequenceDelta = DataParser.getUInt32(HoTTlogReaderD.buf, 0) - sequenceNumber;
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += logTimeStep_ms * sequenceDelta;// add default time step given by log msec
					sequenceNumber = DataParser.getUInt32(HoTTlogReaderD.buf, 0);

					HoTTlogReaderD.isJustMigrated = !HoTTlogReaderD.rcvLogParser.updateLossStatistics();
					HoTTlogReaderD.isJustMigrated = false;

					if (i % progressIndicator == 0) GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
				}
				else { //skip empty block, but add time step
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found rx=0 dBm");
					if (log.isLoggable(Level.INFO)) {
						log.log(Level.INFO, "sensor data Tx=Rx=0 " + StringHelper.byte2Hex2CharString(HoTTlogReaderD.buf, HoTTlogReaderD.buf.length));
					}

					HoTTlogReaderD.rcvLogParser.trackPackageLoss(false);
					//fill receiver data
					isReceiverData = HoTTlogReaderD.rcvLogParser.parse();
					System.arraycopy(valuesRec, 4, HoTTlogReaderD.points, 4, 2); //Rx and Tx values only, keep other data since invalid

					HoTTlogReaderD.chnLogParser.parse();
					System.arraycopy(valuesChn, 3, HoTTlogReaderD.points, 87, 20); //copy channel data and events, warning
					
					HoTTlogReaderD.recordSet.addPoints(HoTTlogReaderD.points, HoTTlogReaderD.chnLogParser.getTimeStep_ms());

					sequenceDelta = DataParser.getUInt32(HoTTlogReaderD.buf, 0) - sequenceNumber;
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += logTimeStep_ms * sequenceDelta;// add default time step given by log msec
					sequenceNumber = DataParser.getUInt32(HoTTlogReaderD.buf, 0);
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
			HoTTlogReaderD.rcvLogParser.finalUpdateLossStatistics();
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0 
					? String.format("%.1f", HoTTlogReaderD.rcvLogParser.getLostPackages().percentage) 
					: "100";
			HoTTlogReaderD.detectedSensors.add(Sensor.CHANNEL);
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { HoTTlogReaderD.rcvLogParser.getLossTotal(), HoTTlogReader.rcvLogParser.getLostPackages().lossTotal, packageLossPercentage, HoTTlogReader.rcvLogParser.getLostPackages().getStatistics() })
					+ String.format(" - Sensor: %s", HoTTlogReaderD.detectedSensors.toString())
					+ (altitudeClimbSensorSelection != null && (detectedSensors.contains(Sensor.fromOrdinal(pickerParameters.altitudeClimbSensorSelection)) || detectedSensors.contains(altitudeClimbSensorSelection))
							? String.format(" - %s = %s", Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGT2419), altitudeClimbSensorSelection)
							: ""));
			log.logp(Level.WARNING, HoTTlogReaderD.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + HoTTlogReaderD.rcvLogParser.getLostPackages().lossTotal); //$NON-NLS-1$
			log.logp(Level.TIME, HoTTlogReaderD.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (GDE.isWithUi()) {
				GDE.getUiNotification().setProgress(99);
				device.makeInActiveDisplayable(HoTTlogReaderD.recordSet);
				device.updateVisibilityStatus(HoTTlogReaderD.recordSet, true);
				channel.applyTemplate(recordSetName, false);

				//write filename after import to record description
				HoTTlogReaderD.recordSet.descriptionAppendFilename(file.getName());

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

	public static class VarLogParserD extends VarLogParser {

		protected VarLogParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[] buffer) {
			super(pickerParameters, points, timeSteps_ms, buffer);
		}

		@Override
		protected boolean parse() {
			super.parse();

			//if ((_buf[40] & 0xFF) == 0xFF) { // gyro receiver
			// 239=Test 00 240=Test 01.. 251=Test 12
				for (int i = 0, j = 0; i < 13; i++, j += 2) {
					this.points[i + 14] = DataParser.parse2Short(buf, 40 + j) * 1000;
				}
			//}
			if (log.isLoggable(Level.FINER)) {
				printSensorValues(buf, this.points, 21);
			}
			return true;
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
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=Ch17 ... 118=Ch32, 119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
		//ESC1
		// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
		// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
		// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
		// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
		//ESC2
		// 152=VoltageM, 153=CurrentM, 154=CapacityM, 155=PowerM, 156=RevolutionM, 157=TemperatureM 1, 158=TemperatureM 2 159=Voltage_min, 160=Current_max,
		// 161=Revolution_max, 162=Temperature1_max, 163=Temperature2_max 164=Event M
		// 165=Speed 166=Speed_max 167=PWM 168=Throttle 169=VoltageBEC 170=VoltageBEC_min 171=CurrentBEC 172=TemperatureBEC 173=TemperatureCap 
		// 174=Timing(empty) 175=Temperature_aux 176=Gear 177=YGEGenExt 178=MotStatEscNr 179=misc ESC_15 180=VersionESC
		//ESC3
		// 181=VoltageM2, 182=CurrentM, 183=CapacityM, 184=PowerM, 185=RevolutionM, 186=TemperatureM 1, 187=TemperatureM 2 188=Voltage_min, 189=Current_max,
		// 190=Revolution_max, 191=Temperature1_max, 192=Temperature2_max 193=Event M
		// 194=Speed 195=Speed_max 196=PWM 197=Throttle 198=VoltageBEC 199=VoltageBEC_min 200=CurrentBEC 201=TemperatureBEC 202=TemperatureCap 
		// 203=Timing(empty) 204=Temperature_aux 205=Gear 206=YGEGenExt 207=MotStatEscNr 208=misc ESC_15 209=VersionESC
		//ESC4
		// 210=VoltageM3, 211=CurrentM, 212=CapacityM, 213=PowerM, 214=RevolutionM, 215=TemperatureM 1, 216=TemperatureM 2 217=Voltage_min, 218=Current_max,
		// 219=Revolution_max, 220=Temperature1_max, 221=Temperature2_max 222=Event M
		// 223=Speed 224=Speed_max 225=PWM 226=Throttle 227=VoltageBEC 228=VoltageBEC_min 229=CurrentBEC 230=TemperatureBEC 231=TemperatureCap 
		// 232=Timing(empty) 233=Temperature_aux 234=Gear 235=YGEGenExt 236=MotStatEscNr 237=misc ESC_15 238=VersionESC

		// 239=Test 00 240=Test 01.. 251=Test 12

		//in 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14,
		//in 20=Altitude, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 27=RPM 28=MotorTime 29=Speed 30=Event
		if (isElectricData) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3
			for (int j = 0; !isVarioData && !isGPSData && !isGeneralData && j < 3; j++) { //0=altitude 1=climb1 2=climb3
				HoTTlogReaderD.points[j + 10] = valuesEAM[j + 20];
			}

			//out 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
			System.arraycopy(valuesEAM, 1, HoTTlogReaderD.points, 60, 19);
			//out 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
			System.arraycopy(valuesEAM, 23, HoTTlogReaderD.points, 79, 8);
		}
		//in 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6,
		//in 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		//in 21=Speed, 22=LowestCellVoltage, 23=LowestCellNumber, 24=Pressure, 24=Event
		if (isGeneralData) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3
			for (int k = 0; !isVarioData && !isGPSData && k < 3; k++) {
				HoTTlogReaderD.points[k + 10] = valuesGAM[k + 13];
			}
			//out 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6, 49=Revolution G, 
			for (int j = 0; j < 12; j++) {
				HoTTlogReaderD.points[j + 38] = valuesGAM[j + 1];
			}
			//out 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
			for (int j = 0; !isVarioData && !isGPSData && j < 10; j++) {
				HoTTlogReaderD.points[j + 50] = valuesGAM[j + 16];
			}
		}
		//in 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripLength, 10=VoltageRx, 11=TemperatureRx 12=satellites 13=GPS-fix 14=EventGPS
		if (isGPSData) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3
			for (int j = 0; !isVarioData && j < 3; j++) { //0=altitude 1=climb1 2=climb3
				HoTTlogReaderD.points[j + 10] = valuesGPS[j + 3];
			}
			//out 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 
			HoTTlogReaderD.points[20] = valuesGPS[1];
			HoTTlogReaderD.points[21] = valuesGPS[2];
			for (int k = 0; k < 4; k++) {
				HoTTlogReaderD.points[k + 22] = valuesGPS[k + 6];
			}
			//out 26=NumSatellites 27=GPS-Fix 28=EventGPS
			for (int k = 0; k < 3; k++) {
				HoTTlogReaderD.points[k + 26] = valuesGPS[k + 12];
			}
			//out 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
			for (int k = 0; k < 9; k++) {
				HoTTlogReaderD.points[k + 29] = valuesGPS[k + 15];
			}
		}
		//in 0=RXSQ, 1=Altitude, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx 7=Event 
		//in 8=accX 9=accY 10=accZ 11=reserved 12=version	if (isVarioData) {
		if (isVarioData) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
			for (int j = 0; j < 4; j++) {
				HoTTlogReaderD.points[j + 10] = valuesVario[j + 1];
			}
			HoTTlogReaderD.points[14] = valuesVario[7];

			//out 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
			for (int j = 0; j < 5; j++) {
				HoTTlogReaderD.points[j + 15] = valuesVario[j + 8];
			}

			//out 239=Test 00 240=Test 01.. 251=Test 12
			for (int j = 0; j < 13; j++) {
				HoTTlogReaderD.points[j + 239] = valuesVario[j + 14];
			}
		}
		//in 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature1, 7=Temperature2
		//in 8=Voltage_min, 9=Current_max, 10=Revolution_max, 11=Temperature1_max, 12=Temperature2_max 13=Event
		//in 14=Speed 15=Speed_max 16=PWM 17=Throttle 18=VoltageBEC 19=VoltageBEC_min 20=CurrentBEC 21=TemperatureBEC 22=TemperatureCap 
		//in 23=Timing(empty) 24=Temperature_aux 25=Gear 26=YGEGenExt 27=MotStatEscNr 28=misc ESC_15 29=VersionESC
		if (isEscData) {
			// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
			// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
			// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
			// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
			for (int j = 0; j < 29; j++) {
				HoTTlogReaderD.points[j + 123] = valuesESC[j + 1];
			}
		}
		if (isEsc2Data) {
			// 152=VoltageM, 153=CurrentM, 154=CapacityM, 155=PowerM, 156=RevolutionM, 157=TemperatureM 1, 158=TemperatureM 2 159=Voltage_min, 160=Current_max,
			// 161=Revolution_max, 162=Temperature1_max, 163=Temperature2_max 164=Event M
			// 165=Speed 166=Speed_max 167=PWM 168=Throttle 169=VoltageBEC 170=VoltageBEC_min 171=CurrentBEC 172=TemperatureBEC 173=TemperatureCap 
			// 174=Timing(empty) 175=Temperature_aux 176=Gear 177=YGEGenExt 178=MotStatEscNr 179=misc ESC_15 180=VersionESC
			for (int j = 0; j < 29; j++) {
				HoTTlogReaderD.points[j + 152] = valuesESC2[j + 1];
			}
		}
		if (isEsc3Data) {
			// 181=VoltageM2, 182=CurrentM, 183=CapacityM, 184=PowerM, 185=RevolutionM, 186=TemperatureM 1, 187=TemperatureM 2 188=Voltage_min, 189=Current_max,
			// 190=Revolution_max, 191=Temperature1_max, 192=Temperature2_max 193=Event M
			// 194=Speed 195=Speed_max 196=PWM 197=Throttle 198=VoltageBEC 199=VoltageBEC_min 200=CurrentBEC 201=TemperatureBEC 202=TemperatureCap 
			// 203=Timing(empty) 204=Temperature_aux 205=Gear 206=YGEGenExt 207=MotStatEscNr 208=misc ESC_15 209=VersionESC
			for (int j = 0; j < 29; j++) {
				HoTTlogReaderD.points[j + 181] = valuesESC3[j + 1];
			}
		}
		if (isEsc4Data) {
			// 210=VoltageM3, 211=CurrentM, 212=CapacityM, 213=PowerM, 214=RevolutionM, 215=TemperatureM 1, 216=TemperatureM 2 217=Voltage_min, 218=Current_max,
			// 219=Revolution_max, 220=Temperature1_max, 221=Temperature2_max 222=Event M
			// 223=Speed 224=Speed_max 225=PWM 226=Throttle 227=VoltageBEC 228=VoltageBEC_min 229=CurrentBEC 230=TemperatureBEC 231=TemperatureCap 
			// 232=Timing(empty) 233=Temperature_aux 234=Gear 235=YGEGenExt 236=MotStatEscNr 237=misc ESC_15 238=VersionESC
			for (int j = 0; j < 29; j++) {
				HoTTlogReaderD.points[j + 210] = valuesESC4[j + 1];
			}
		}

		//add altitude and climb values from selected sensor
		//log.log(Level.OFF, String.format("pickerParameters.altitudeClimbSensorSelection = %s", pickerParameters.altitudeClimbSensorSelection));
		switch (Sensor.VALUES[HoTTlogReaderD.pickerParameters.altitudeClimbSensorSelection]) {
		case VARIO:
			//8=Altitude, 9=Climb 1, 10=Climb 3, 11=Climb 10
			if (isVarioData) for (int j = 0; j < 4; j++) {
				HoTTlogReaderD.points[j + 10] = valuesVario[j + 1];
			}
			break;
		case GPS:
			//8=Altitude, 9=Climb 1, 10=Climb 3
			if (isGPSData) for (int j = 0; j < 3; j++) { //0=altitude 1=climb1 2=climb3
				HoTTlogReaderD.points[j + 10] = valuesGPS[j + 3];
			}
			HoTTlogReaderD.points[11] = 0;
			break;
		case GAM:
			//8=Altitude, 9=Climb 1, 10=Climb 3
			if (isGeneralData) for (int j = 0; j < 3; j++) {
				HoTTlogReaderD.points[j + 10] = valuesGAM[j + 13];
			}
			HoTTlogReaderD.points[11] = 0;
			break;
		case EAM:
			//8=Altitude, 9=Climb 1, 10=Climb 3
			if (isElectricData) for (int j = 0; j < 3; j++) { //0=altitude 1=climb1 2=climb3
				HoTTlogReaderD.points[j + 10] = valuesEAM[j + 20];
			}
			HoTTlogReaderD.points[11] = 0;
			break;
		default:
			break;
		}

		HoTTlogReaderD.recordSet.addPoints(HoTTlogReaderD.points, timeStep_ms);
		HoTTlogReaderD.isJustMigrated = true;
	}
}
