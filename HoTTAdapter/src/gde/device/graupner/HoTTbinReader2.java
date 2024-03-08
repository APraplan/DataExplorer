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
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.device.IDevice;
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
public class HoTTbinReader2 extends HoTTbinReader {
	final static Logger	log	= Logger.getLogger(HoTTbinReader2.class.getName());
	static int[]				points;
	static RecordSet		recordSet;

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception
	 */
	public static synchronized void read(String filePath, PickerParameters newPickerParameters) throws Exception {
		HoTTbinReader2.pickerParameters = newPickerParameters;
		HashMap<String, String> header = getFileInfo(new File(filePath), newPickerParameters);
		HoTTbinReader2.detectedSensors = Sensor.getSetFromDetected(header.get(HoTTAdapter.DETECTED_SENSOR));
		
		//set picker parameter setting sensor for altitude/climb usage (0=auto, 1=VARIO, 2=GPS, 3=GAM, 4=EAM)
		HoTTbinReader2.setAltitudeClimbPickeParameter(HoTTbinReader2.pickerParameters, HoTTbinReader2.detectedSensors);

		if (HoTTbinReader2.detectedSensors.size() <= 2) {
			HoTTbinReader2.isReceiverOnly = HoTTbinReader2.detectedSensors.size() == 1;
			readSingle(new File(header.get(HoTTAdapter.FILE_PATH)), header);
		} else
			readMultiple(new File(header.get(HoTTAdapter.FILE_PATH)), header);
	}

	/**
	 * read log data according to version 0
	 * @param file
	 * @param data_in
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	static void readSingle(File file, HashMap<String, String> header) throws IOException, DataInconsitsentException {
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		IDevice device = HoTTbinReader2.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader2.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		int channelNumber = HoTTbinReader2.pickerParameters.analyzer.getActiveChannel().getNumber();
		device.getMeasurementFactor(channelNumber, 12);
		boolean isReceiverData = false;
		boolean isSensorData = false;
		boolean isVarioDetected = false;
		boolean isGPSdetected = false;
		boolean isESCdetected = false;
		boolean[] isResetMinMax = new boolean[] {false, false, false, false, false}; //ESC, EAM, GAM, GPS, Vario
		HoTTbinReader2.recordSet = null;
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
		HoTTbinReader2.points = new int[device.getNumberOfMeasurements(channelNumber)];
		HoTTbinReader2.pointsGAM = HoTTbinReader2.pointsEAM = HoTTbinReader2.pointsESC = HoTTbinReader2.pointsVario = HoTTbinReader2.pointsGPS = HoTTbinReader2.points;
		HoTTbinReader2.dataBlockSize = 64;
		HoTTbinReader2.buf = new byte[HoTTbinReader2.dataBlockSize];
		HoTTbinReader2.buf0 = new byte[30];
		HoTTbinReader2.buf1 = new byte[30];
		HoTTbinReader2.buf2 = new byte[30];
		HoTTbinReader2.buf3 = new byte[30];
		HoTTbinReader2.buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		long[] timeSteps_ms = new long[] { 0 };
		HoTTbinReader2.rcvBinParser = Sensor.RECEIVER.createBinParser2(HoTTbinReader2.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf });
		HoTTbinReader2.chnBinParser = Sensor.CHANNEL.createBinParser2(HoTTbinReader2.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf });
		HoTTbinReader2.varBinParser = Sensor.VARIO.createBinParser2(HoTTbinReader2.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader2.gpsBinParser = Sensor.GPS.createBinParser2(HoTTbinReader2.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader2.gamBinParser = Sensor.GAM.createBinParser2(HoTTbinReader2.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader2.eamBinParser = Sensor.EAM.createBinParser2(HoTTbinReader2.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader2.escBinParser = Sensor.ESC.createBinParser2(HoTTbinReader2.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader2.isTextModusSignaled = false;
		boolean isSdLogFormat = Boolean.parseBoolean(header.get(HoTTAdapter.SD_FORMAT));
		long numberDatablocks = isSdLogFormat ? fileSize - HoTTbinReaderX.headerSize - HoTTbinReaderX.footerSize : fileSize / HoTTbinReader2.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader2.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks);
		numberDatablocks = HoTTbinReader2.isReceiverOnly && channelNumber != HoTTAdapter2.CHANNELS_CHANNEL_NUMBER ? numberDatablocks / 10 : numberDatablocks;
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTbinReader2.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);
		if (isSdLogFormat) data_in.skip(HoTTbinReaderX.headerSize);

		try {
			// check if recordSet initialized, transmitter and receiver data always present, but not in the same data rate and signals
			channel = HoTTbinReader2.channels.get(channelNumber);
			String newFileDescription = HoTTbinReader2.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader2.application.getObjectKey()	: date;
			if (channel.getFileDescription().length() <= newFileDescription.length() || (HoTTbinReader2.application.isObjectoriented() && !channel.getFileDescription().contains(HoTTbinReader2.application.getObjectKey())))
				channel.setFileDescription(newFileDescription);
			recordSetName = recordSetNumber + device.getRecordSetStemNameReplacement() + recordSetNameExtend;
			HoTTbinReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
			channel.put(recordSetName, HoTTbinReader2.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			// recordSet initialized and ready to add data

			// read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader2.buf);
				if (HoTTbinReader2.log.isLoggable(Level.FINE) && i % 10 == 0) {
					HoTTbinReader2.log.log(Level.FINE, StringHelper.fourDigitsRunningNumber(HoTTbinReader2.buf.length));
					HoTTbinReader2.log.log(Level.FINE, StringHelper.byte2Hex4CharString(HoTTbinReader2.buf, HoTTbinReader2.buf.length));
				}

				if (!HoTTbinReader2.pickerParameters.isFilterTextModus || (HoTTbinReader2.buf[6] & 0x01) == 0) { // switch into text modus
					if (HoTTbinReader2.buf[33] >= 0 && HoTTbinReader2.buf[33] <= 4 && HoTTbinReader2.buf[3] != 0 && HoTTbinReader2.buf[4] != 0) { // buf 3, 4, tx,rx
						if (HoTTbinReader2.log.isLoggable(Level.INFO))
							HoTTbinReader2.log.log(Level.INFO, String.format("Sensor %x Blocknummer : %d", HoTTbinReader2.buf[7], HoTTbinReader2.buf[33]));

						((RcvBinParser) HoTTbinReader2.rcvBinParser).trackPackageLoss(true);
						
						if (HoTTbinReader2.log.isLoggable(Level.FINER)) HoTTbinReader2.log.log(Level.FINER, StringHelper.byte2Hex2CharString(new byte[] {
								HoTTbinReader2.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader2.buf[7], false));

						// fill receiver data
						if (HoTTbinReader2.buf[33] == 0 && (HoTTbinReader2.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader2.buf, 40) >= 0) {
							HoTTbinReader2.rcvBinParser.parse();
							isReceiverData = true;
						}
						if (channelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) {
							HoTTbinReader2.chnBinParser.parse(); // Channels
						}

						// fill data block 0 receiver voltage an temperature
						if (buf[33] == 0) {
							bufCopier.copyToBuffer();
						}
						if (HoTTbinReader2.isReceiverOnly && channelNumber != HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) { // reduce data rate for receiver to 0.1 sec
							for (int j = 0; j < 9; j++) { //skip 9 data packets
								data_in.read(HoTTbinReader2.buf);
								timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
							}
							isSensorData = true;
						}

						// create and fill sensor specific data record sets
						switch ((byte) (HoTTbinReader2.buf[7] & 0xFF)) {
						case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
						case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
							if (detectedSensors.contains(Sensor.VARIO)) {
								bufCopier.copyToVarioBuffer();
								if (bufCopier.is4BuffersFull()) {
									HoTTbinReader2.varBinParser.parse();

									if (!isVarioDetected) {
										HoTTAdapter2.updateVarioTypeDependent((HoTTbinReader2.buf4[9] & 0xFF), device, HoTTbinReader2.recordSet);
										isVarioDetected = true;								
									}
									
									bufCopier.clearBuffers();
									isSensorData = true;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_GPS_115200:
						case HoTTAdapter.SENSOR_TYPE_GPS_19200:
							if (detectedSensors.contains(Sensor.GPS)) {
								bufCopier.copyToFreeBuffer();
								if (bufCopier.is4BuffersFull()) {
									HoTTbinReader2.gpsBinParser.parse();
									
									if (!isGPSdetected) {
										if (isReasonableData(buf4) && HoTTbinReader2.recordSet.get(33).size() > 0 && HoTTbinReader2.recordSet.get(33).get(HoTTbinReader2.recordSet.get(33).size()-1) != 0) {
											HoTTAdapter2.updateGpsTypeDependent((buf4[9] & 0xFF), device, HoTTbinReader2.recordSet, (HoTTbinReader2.recordSet.get(33).size()-1) * 5);
											isGPSdetected = true;
										}
									}
									
									bufCopier.clearBuffers();
									isSensorData = true;
									// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
									// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
									if (!isResetMinMax[3] && HoTTbinReader2.points[27] == 3000 && HoTTbinReader2.points[20] != 0 && HoTTbinReader2.points[21] != 0) {
										for (int j=20; j<38; ++j) {
											tmpRecordSet.get(j).setMinMax(HoTTbinReader2.points[j], HoTTbinReader2.points[j]);
										}
										isResetMinMax[3] = true;
									}
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
						case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
							if (detectedSensors.contains(Sensor.GAM)) {
								bufCopier.copyToFreeBuffer();
								if (bufCopier.is4BuffersFull()) {
									HoTTbinReader2.gamBinParser.parse();
									bufCopier.clearBuffers();
									isSensorData = true;
									// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
									// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
									// 57=LowestCellNumber, 58=Pressure, 59=Event G
									if (!isResetMinMax[2] && HoTTbinReader2.points[38] != 0) {
										for (int j=38; j<60; ++j) {
											tmpRecordSet.get(j).setMinMax(HoTTbinReader2.points[j], HoTTbinReader2.points[j]);
										}
										isResetMinMax[2] = true;
									}
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
							if (detectedSensors.contains(Sensor.EAM)) {
								bufCopier.copyToFreeBuffer();
								if (bufCopier.is4BuffersFull()) {
									HoTTbinReader2.eamBinParser.parse();
									bufCopier.clearBuffers();
									isSensorData = true;
									// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
									// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
									if (!isResetMinMax[1] && HoTTbinReader2.points[60] != 0) {
										for (int j=60; j<87; ++j) {
											tmpRecordSet.get(j).setMinMax(HoTTbinReader2.points[j], HoTTbinReader2.points[j]);
										}
										isResetMinMax[1] = true;
									}
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
							if (detectedSensors.contains(Sensor.ESC)) {
								bufCopier.copyToFreeBuffer();
								if (bufCopier.is4BuffersFull()) {
									HoTTbinReader2.escBinParser.parse();
									
									if (!isESCdetected) {
										HoTTAdapter2.updateEscTypeDependent((HoTTbinReader2.buf4[9] & 0xFF), device, HoTTbinReader2.recordSet, 1);
										isESCdetected = true;								
									}

									bufCopier.clearBuffers();
									isSensorData = true;
									if (((EscBinParser) HoTTbinReader2.escBinParser).isChannelsChannel()) {
										// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
										// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
										// 120=Speed 121=Speed_max 122=PWM 123=Throttle 124=VoltageBEC 125=VoltageBEC_max 125=CurrentBEC 127=TemperatureBEC 128=TemperatureCap 
										// 129=Timing(empty) 130=Temperature_aux 131=Gear 132=YGEGenExt 133=MotStatEscNr 134=misc ESC_15 135=VersionESC
										if (!isResetMinMax[0] && HoTTbinReader2.points[107] != 0) {
											for (int j=107; j<136; ++j) {
												tmpRecordSet.get(j).setMinMax(HoTTbinReader2.points[j], HoTTbinReader2.points[j]);
											}
											isResetMinMax[0] = true;
										}
									} else {
										// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
										// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M
										// 100=Speed 101=Speed_max 102=PWM 103=Throttle 104=VoltageBEC 105=VoltageBEC_max 106=CurrentBEC 107=TemperatureBEC 108=TemperatureCap 
										// 109=Timing(empty) 110=Temperature_aux 111=Gear 112=YGEGenExt 113=MotStatEscNr 114=misc ESC_15 115=VersionESC
										if (!isResetMinMax[0] && HoTTbinReader2.points[87] != 0) {
											for (int j=87; j<116; ++j) {
												tmpRecordSet.get(j).setMinMax(HoTTbinReader2.points[j], HoTTbinReader2.points[j]);
											}
											isResetMinMax[0] = true;
										}
									}
								}
							}
							break;
						}

						if (isSensorData) {
							((RcvBinParser) HoTTbinReader2.rcvBinParser).updateLossStatistics();
						}

						if (isSensorData || (isReceiverData && tmpRecordSet.get(0).realSize() > 0)) {
							tmpRecordSet.addPoints(HoTTbinReader2.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
							isSensorData = isReceiverData = false;
						} else if (channelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) {
							tmpRecordSet.addPoints(HoTTbinReader2.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						}

						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10; // add default time step from device of 10 msec
						
						if (i % progressIndicator == 0) GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
					} else { // skip empty block, but add time step
						if (HoTTbinReader2.log.isLoggable(Level.FINE)) HoTTbinReader2.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");
						
						((RcvBinParser) HoTTbinReader2.rcvBinParser).trackPackageLoss(false);

						if (channelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) {
							HoTTbinReader2.chnBinParser.parse(); // Channels
							tmpRecordSet.addPoints(HoTTbinReader2.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						}
						
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
					}
				} else if (!HoTTbinReader2.isTextModusSignaled) {
					HoTTbinReader2.isTextModusSignaled = true;
					HoTTbinReader2.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			((RcvBinParser) HoTTbinReader2.rcvBinParser).finalUpdateLossStatistics();
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0
					? String.format("%.1f", ((RcvBinParser) HoTTbinReader2.rcvBinParser).getLostPackages().percentage)
					: "100";
			if (channelNumber == 4)
				HoTTbinReader2.detectedSensors.add(Sensor.CHANNEL);
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] {	((RcvBinParser) HoTTbinReader2.rcvBinParser).getLossTotal(), ((RcvBinParser) HoTTbinReader2.rcvBinParser).getLostPackages().lossTotal, packageLossPercentage, ((RcvBinParser) HoTTbinReader2.rcvBinParser).getLostPackages().getStatistics() }) 
					+ String.format(" - Sensor: %s", HoTTbinReader2.detectedSensors.toString())
					+ (HoTTAdapter2.isAltClimbSensor(HoTTbinReader2.detectedSensors)
							? String.format(" - %s = %s", Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGT2419), Sensor.fromOrdinal(pickerParameters.altitudeClimbSensorSelection).name())
									: ""));

			HoTTbinReader2.log.log(Level.WARNING, "skipped number receiver data due to package loss = " + ((RcvBinParser) HoTTbinReader2.rcvBinParser).getLostPackages().lossTotal); //$NON-NLS-1$
			HoTTbinReader2.log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (GDE.isWithUi()) {
				GDE.getUiNotification().setProgress(99);
				device.updateVisibilityStatus(tmpRecordSet, true);
				channel.applyTemplate(recordSetName, false);

				// write filename after import to record description
				tmpRecordSet.descriptionAppendFilename(file.getName());

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				GDE.getUiNotification().setProgress(100);
			}
		} finally {
			data_in.close();
			data_in = null;
		}
	}


	/**
	 * read log data according to version 0
	 * @param file
	 * @param data_in
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	static void readMultiple(File file, HashMap<String, String> header) throws IOException, DataInconsitsentException {
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		IDevice device = HoTTbinReader2.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader2.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		int channelNumber = HoTTbinReader2.pickerParameters.analyzer.getActiveChannel().getNumber();
		boolean isReceiverData = false;
		HoTTbinReader2.recordSet = null;
		boolean isJustMigrated = false;
		boolean isVarioDetected = false;
		boolean isGPSdetected = false;
		boolean isESCdetected = false;
		boolean[] isResetMinMax = new boolean[] {false, false, false, false, false}; //ESC, EAM, GAM, GPS, Vario
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
		HoTTbinReader2.points = new int[device.getNumberOfMeasurements(channelNumber)];
		HoTTbinReader2.pointsGAM = new int[HoTTbinReader2.points.length];
		HoTTbinReader2.pointsEAM = new int[HoTTbinReader2.points.length];
		HoTTbinReader2.pointsESC = new int[HoTTbinReader2.points.length];
		HoTTbinReader2.pointsVario = new int[HoTTbinReader2.points.length];
		HoTTbinReader2.pointsVario[2] = 100000;
		HoTTbinReader2.pointsGPS = new int[HoTTbinReader2.points.length];
		HoTTbinReader2.dataBlockSize = 64;
		HoTTbinReader2.buf = new byte[HoTTbinReader2.dataBlockSize];
		HoTTbinReader2.buf0 = new byte[30];
		HoTTbinReader2.buf1 = new byte[30];
		HoTTbinReader2.buf2 = new byte[30];
		HoTTbinReader2.buf3 = new byte[30];
		HoTTbinReader2.buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		long[] timeSteps_ms = new long[] { 0 };
		// parse in situ for receiver and channel
		HoTTbinReader2.rcvBinParser = Sensor.RECEIVER.createBinParser2(HoTTbinReader2.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf });
		HoTTbinReader2.chnBinParser = Sensor.CHANNEL.createBinParser2(HoTTbinReader2.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf });
		// use parser points objects
		HoTTbinReader2.varBinParser = Sensor.VARIO.createBinParser2(HoTTbinReader2.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader2.gpsBinParser = Sensor.GPS.createBinParser2(HoTTbinReader2.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader2.gamBinParser = Sensor.GAM.createBinParser2(HoTTbinReader2.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader2.eamBinParser = Sensor.EAM.createBinParser2(HoTTbinReader2.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader2.escBinParser = Sensor.ESC.createBinParser2(HoTTbinReader2.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGAM = 0, logCountEAM = 0, logCountESC = 0;
		EnumSet<Sensor> migrationJobs = EnumSet.noneOf(Sensor.class);
		boolean isSdLogFormat = Boolean.parseBoolean(header.get(HoTTAdapter.SD_FORMAT));
		long numberDatablocks = isSdLogFormat ? fileSize - HoTTbinReaderX.headerSize - HoTTbinReaderX.footerSize : fileSize / HoTTbinReader2.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader2.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTbinReader2.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);
		if (isSdLogFormat) data_in.skip(HoTTbinReaderX.headerSize);

		try {
			// receiver data are always contained
			channel = HoTTbinReader2.channels.get(channelNumber);
			String newFileDescription = HoTTbinReader2.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader2.application.getObjectKey()	: date;
			if (channel.getFileDescription().length() <= newFileDescription.length() || (HoTTbinReader2.application.isObjectoriented() && !channel.getFileDescription().contains(HoTTbinReader2.application.getObjectKey())))
				channel.setFileDescription(newFileDescription);
			recordSetName = recordSetNumber + device.getRecordSetStemNameReplacement() + recordSetNameExtend;
			HoTTbinReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
			channel.put(recordSetName, HoTTbinReader2.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			// recordSet initialized and ready to add data

			// read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader2.buf);
				if (HoTTbinReader2.log.isLoggable(Level.FINEST)) {
					HoTTbinReader2.log.log(Level.FINEST, StringHelper.byte2Hex4CharString(HoTTbinReader2.buf, HoTTbinReader2.buf.length));
				}

				if (!HoTTbinReader2.pickerParameters.isFilterTextModus || (HoTTbinReader2.buf[6] & 0x01) == 0) { // switch into text modus
					if (HoTTbinReader2.buf[33] >= 0 && HoTTbinReader2.buf[33] <= 4 && HoTTbinReader2.buf[3] != 0 && HoTTbinReader2.buf[4] != 0) { // buf 3, 4, tx,rx
						if (HoTTbinReader2.log.isLoggable(Level.INFO))
							HoTTbinReader2.log.log(Level.INFO, String.format("Sensor %x Blocknummer : %d", HoTTbinReader2.buf[7], HoTTbinReader2.buf[33]));

						((RcvBinParser) HoTTbinReader2.rcvBinParser).trackPackageLoss(true);
						
						if (HoTTbinReader2.log.isLoggable(Level.FINEST)) HoTTbinReader2.log.log(Level.FINEST, StringHelper.byte2Hex2CharString(new byte[] {
								HoTTbinReader2.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader2.buf[7], false));

						// fill receiver data
						if (HoTTbinReader2.buf[33] == 0 && (HoTTbinReader2.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader2.buf, 40) >= 0) {
							HoTTbinReader2.rcvBinParser.parse();
							isReceiverData = true;
						}
						if (channelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) {
							HoTTbinReader2.chnBinParser.parse();
						}

						if (actualSensor == -1)
							lastSensor = actualSensor = (byte) (HoTTbinReader2.buf[7] & 0xFF);
						else
							actualSensor = (byte) (HoTTbinReader2.buf[7] & 0xFF);

						if (actualSensor != lastSensor) {
							if (logCountVario >= 5 || logCountGPS >= 5 || logCountGAM >= 5 || logCountEAM >= 5 || logCountESC >= 5) {
								switch (lastSensor) {
								case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
								case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
									if (detectedSensors.contains(Sensor.VARIO)) {
										if (migrationJobs.contains(Sensor.VARIO) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReader2.varBinParser.parse();
										migrationJobs.add(Sensor.VARIO);
										
										if (!isVarioDetected) {
											HoTTAdapter2.updateVarioTypeDependent((HoTTbinReader2.buf4[9] & 0xFF), device, HoTTbinReader2.recordSet);
											isVarioDetected = true;								
										}
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GPS_115200:
								case HoTTAdapter.SENSOR_TYPE_GPS_19200:
									if (detectedSensors.contains(Sensor.GPS)) {
										if (migrationJobs.contains(Sensor.GPS) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReader2.gpsBinParser.parse();
										migrationJobs.add(Sensor.GPS);
										
										if (!isGPSdetected) {
											if (isReasonableData(buf4) && HoTTbinReader2.recordSet.get(33).size() > 0 && HoTTbinReader2.recordSet.get(33).get(HoTTbinReader2.recordSet.get(33).size()-1) != 0) {
												HoTTAdapter2.updateGpsTypeDependent((buf4[9] & 0xFF), device, HoTTbinReader2.recordSet, (HoTTbinReader2.recordSet.get(33).size()-1) * 5);
												isGPSdetected = true;
											}
										}
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
								case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
									if (detectedSensors.contains(Sensor.GAM)) {
										if (migrationJobs.contains(Sensor.GAM) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReader2.gamBinParser.parse();
										migrationJobs.add(Sensor.GAM);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
									if (detectedSensors.contains(Sensor.EAM)) {
										if (migrationJobs.contains(Sensor.EAM) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReader2.eamBinParser.parse();
										migrationJobs.add(Sensor.EAM);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
									if (detectedSensors.contains(Sensor.ESC)) {
										if (migrationJobs.contains(Sensor.ESC) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReader2.escBinParser.parse();
										migrationJobs.add(Sensor.ESC);
										
										if (!isESCdetected) {
											HoTTAdapter2.updateEscTypeDependent((HoTTbinReader2.buf4[9] & 0xFF), device, HoTTbinReader2.recordSet, 1);
											isESCdetected = true;								
										}
									}
									break;
								}

								if (HoTTbinReader2.log.isLoggable(Level.FINE)) HoTTbinReader2.log.log(Level.FINE, "isReceiverData " + isReceiverData + " migrationJobs " + migrationJobs);
							}

							if (HoTTbinReader2.log.isLoggable(Level.FINE))
								HoTTbinReader2.log.log(Level.FINE, "logCountVario = " + logCountVario + " logCountGPS = " + logCountGPS + " logCountGeneral = " + logCountGAM + " logCountElectric = " + logCountEAM);
							lastSensor = actualSensor;
							logCountVario = logCountGPS = logCountGAM = logCountEAM = logCountESC = 0;
						}

						switch (lastSensor) {
						case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
						case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
							++logCountVario;
							break;
						case HoTTAdapter.SENSOR_TYPE_GPS_115200:
						case HoTTAdapter.SENSOR_TYPE_GPS_19200:
							++logCountGPS;
							break;
						case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
						case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
							++logCountGAM;
							break;
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
							++logCountEAM;
							break;
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
							++logCountESC;
							break;
						}

						if (isJustMigrated) {
							((RcvBinParser) HoTTbinReader2.rcvBinParser).updateLossStatistics();
						}

						if (isReceiverData && (logCountVario > 0 || logCountGPS > 0 || logCountGAM > 0 || logCountEAM > 0 || logCountESC > 0)) {
							tmpRecordSet.addPoints(HoTTbinReader2.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
							isReceiverData = false;
						}
						else if (channelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER && !isJustMigrated) {
							tmpRecordSet.addPoints(HoTTbinReader2.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						}
						isJustMigrated = false;

						bufCopier.copyToBuffer();
						
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;// add default time step from log record of 10 msec

						if (i % progressIndicator == 0) GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
					} else { // skip empty block, but add time step
						if (HoTTbinReader2.log.isLoggable(Level.FINE)) HoTTbinReader2.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");
						
						((RcvBinParser) HoTTbinReader2.rcvBinParser).trackPackageLoss(false);
						
						if (channelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) {
							HoTTbinReader2.chnBinParser.parse();
							tmpRecordSet.addPoints(HoTTbinReader2.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						}
						
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
					}
				} else if (!HoTTbinReader2.isTextModusSignaled) {
					HoTTbinReader2.isTextModusSignaled = true;
					HoTTbinReader2.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			// if (HoTTbinReader2.oldProtocolCount > 2) {
			// application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2405, new Object[] {
			// HoTTbinReader2.oldProtocolCount }));
			// }
			((RcvBinParser) HoTTbinReader2.rcvBinParser).finalUpdateLossStatistics();
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0
					? String.format("%.1f", ((RcvBinParser) HoTTbinReader2.rcvBinParser).getLostPackages().percentage)
					: "100";
			if (channelNumber == 4)
				HoTTbinReader2.detectedSensors.add(Sensor.CHANNEL);
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] {	((RcvBinParser) HoTTbinReader2.rcvBinParser).getLossTotal(), ((RcvBinParser) HoTTbinReader2.rcvBinParser).getLostPackages().lossTotal, packageLossPercentage, ((RcvBinParser) HoTTbinReader2.rcvBinParser).getLostPackages().getStatistics() }) 
					+ String.format(" - Sensor: %s", HoTTbinReader2.detectedSensors.toString())
					+ (HoTTAdapter2.isAltClimbSensor(HoTTbinReader2.detectedSensors)
							? String.format(" - %s = %s", Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGT2419), Sensor.fromOrdinal(pickerParameters.altitudeClimbSensorSelection).name())
									: ""));
			HoTTbinReader2.log.log(Level.WARNING, "skipped number receiver data due to package loss = " + ((RcvBinParser) HoTTbinReader2.rcvBinParser).getLossTotal()); //$NON-NLS-1$
			HoTTbinReader2.log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (menuToolBar != null) {
				GDE.getUiNotification().setProgress(99);
				device.makeInActiveDisplayable(tmpRecordSet);
				device.updateVisibilityStatus(tmpRecordSet, true);
				channel.applyTemplate(recordSetName, false);

				// write filename after import to record description
				tmpRecordSet.descriptionAppendFilename(file.getName());

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				GDE.getUiNotification().setProgress(100);
			}
		} finally {
			data_in.close();
			data_in = null;
		}
	}

	/**
	 * Migrate sensor measurement values in the correct priority and add to record set.
	 * Receiver data are always updated.
	 */
	public static void migrateAddPoints(RecordSet tmpRecordSet, EnumSet<Sensor> migrationJobs, long timeStep_ms, boolean[] isResetMinMax) throws DataInconsitsentException {
		if (migrationJobs.contains(Sensor.EAM)) {
			HoTTbinReader2.eamBinParser.migratePoints(HoTTbinReader2.points);
			// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
			// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
			if (!isResetMinMax[1] && HoTTbinReader2.points[60] != 0) {
				for (int i=60; i<87; ++i) {
					tmpRecordSet.get(i).setMinMax(HoTTbinReader2.points[i], HoTTbinReader2.points[i]);
				}
				isResetMinMax[1] = true;
			}
		}
		if (migrationJobs.contains(Sensor.GAM)) {
			HoTTbinReader2.gamBinParser.migratePoints(HoTTbinReader2.points);
			// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
			// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
			// 57=LowestCellNumber, 58=Pressure, 59=Event G
			if (!isResetMinMax[2] && HoTTbinReader2.points[38] != 0) {
				for (int i=38; i<59; ++i) {
					tmpRecordSet.get(i).setMinMax(HoTTbinReader2.points[i], HoTTbinReader2.points[i]);
				}
				isResetMinMax[2] = true;
			}
		}
		if (migrationJobs.contains(Sensor.GPS)) {
			HoTTbinReader2.gpsBinParser.migratePoints(HoTTbinReader2.points);
			// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
			// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
			if (!isResetMinMax[3] && HoTTbinReader2.points[27] >= 3000  && HoTTbinReader2.points[20] != 0 && HoTTbinReader2.points[21] != 0) {
				for (int i=20; i<38; ++i) {
					tmpRecordSet.get(i).setMinMax(HoTTbinReader2.points[i], HoTTbinReader2.points[i]);
				}
				isResetMinMax[3] = true;
			}
		}
		if (migrationJobs.contains(Sensor.VARIO)) {
			HoTTbinReader2.varBinParser.migratePoints(HoTTbinReader2.points);
		}
		if (migrationJobs.contains(Sensor.ESC)) {
			HoTTbinReader2.escBinParser.migratePoints(HoTTbinReader2.points);
			if (((EscBinParser) HoTTbinReader2.escBinParser).isChannelsChannel()) {
				// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
				// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
				// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
				// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
				if (!isResetMinMax[0] && HoTTbinReader2.points[123] != 0) {
					for (int i=123; i<152; ++i) {
						tmpRecordSet.get(i).setMinMax(HoTTbinReader2.points[i], HoTTbinReader2.points[i]);
					}
					isResetMinMax[0] = true;
				}
			} else {
				// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
				// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M
				// 100=Speed 101=Speed_max 102=PWM 103=Throttle 104=VoltageBEC 105=VoltageBEC_max 106=CurrentBEC 107=TemperatureBEC 108=TemperatureCap 
				// 109=Timing(empty) 110=Temperature_aux 111=Gear 112=YGEGenExt 113=MotStatEscNr 114=misc ESC_15 115=VersionESC
				if (!isResetMinMax[0] && HoTTbinReader2.points[87] != 0) {
					for (int i=87; i<114; ++i) {
						tmpRecordSet.get(i).setMinMax(HoTTbinReader2.points[i], HoTTbinReader2.points[i]);
					}
					isResetMinMax[0] = true;
				}
			}
		}
		migrationJobs.clear();

		HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, timeStep_ms);
	}

	public static class RcvBinParser extends BinParser {
		private int							tmpVoltageRx			= 0;
		private int							tmpTemperatureRx	= 0;

		private int							consecutiveLossCounter	= 0;

		private PackageLoss			lostPackages			= new PackageLoss();

		protected final byte[]	_buf;

		protected RcvBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
			throw new UnsupportedOperationException("use in situ parsing");
		}

		protected RcvBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.RECEIVER);
			_buf = buffers[0];
			if (buffers.length != 1) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		/**
		 * parse the buffered data from buffer and add points to record set
		 */
		@Override
		protected boolean parse() {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			this.tmpVoltageRx = (this._buf[35] & 0xFF);
			this.tmpTemperatureRx = (this._buf[36] & 0xFF);
			this.points[1] = (this._buf[38] & 0xFF) * 1000;
			this.points[3] = DataParser.parse2Short(this._buf, 40) * 1000;
			if (isPointsValid()) {
				this.points[2] = (convertRxDbm2Strength(this._buf[4] & 0xFF)) * 1000;
				this.points[4] = (this._buf[3] & 0xFF) * -1000;
				this.points[5] = (this._buf[4] & 0xFF) * -1000;
				this.points[6] = this.tmpVoltageRx * 1000;
				this.points[7] = (this.tmpTemperatureRx - 20) * 1000;
				this.points[8] = (this._buf[39] & 0xFF) * 1000;
				if ((this._buf[32] & 0x40) > 0 || (this._buf[32] & 0x25) > 0 && this.tmpTemperatureRx >= 70) // T = 70 - 20 = 50 lowest temperature warning
					this.points[9] = (this._buf[32] & 0x65) * 1000; // warning E,V,T only
				else
					this.points[9] = 0;
				return true;
			}
			if ((this._buf[32] & 0x40) > 0 || (this._buf[32] & 0x25) > 0 && this.tmpTemperatureRx >= 70) // T = 70 - 20 = 50 lowest temperature warning
				this.points[9] = (this._buf[32] & 0x65) * 1000; // warning E,V,T only
			else
				this.points[9] = 0;
			return false;
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

		public PackageLoss getLostPackages() {
			return this.lostPackages;
		}

		private boolean isPointsValid() {
			// WB !this.pickerParameters.isFilterEnabled || this.tmpVoltageRx > -1 && this.tmpVoltageRx < 100 && this.tmpTemperatureRx < 120; from HoTTbinReader2
			return !this.pickerParameters.isFilterEnabled || this.tmpVoltageRx > -1 && this.tmpVoltageRx < 100 && this.tmpTemperatureRx < 100;
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
			return super.toString() + "  [countPackageLoss=" + this.lostPackages.lossTotal + ", consecutiveLossCounter=" + this.consecutiveLossCounter + "]";
		}

	}

	public static class VarBinParser extends BinParser {
		private int	tmpHeight		= 0;
		private int	tmpClimb10	= 0;
		
		protected VarBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers, Sensor sensor) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.VARIO);
		}

		protected VarBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
		}

		protected VarBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.VARIO);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
			points[2] = 100000;
		}

		@Override
		protected boolean parse() {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			//10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
			this.tmpHeight = DataParser.parse2Short(this._buf1, 2) - 500;
			this.tmpClimb10 = DataParser.parse2UnsignedShort(this._buf2, 2) - 30000;
			if (isPointsValid()) {
				if (this.pickerParameters.altitudeClimbSensorSelection == 1) { //sensor selection GPS (auto, Vario, GPS, GAM, EAM)
					this.points[10] = this.tmpHeight * 1000;
					// pointsVarioMax = DataParser.parse2Short(buf1, 4) * 1000;
					// pointsVarioMin = DataParser.parse2Short(buf1, 6) * 1000;
					this.points[11] = (DataParser.parse2UnsignedShort(this._buf1, 8) - 30000) * 10;
					this.points[12] = (DataParser.parse2UnsignedShort(this._buf2, 0) - 30000) * 10;
					this.points[13] = this.tmpClimb10 * 10;
				}
				this.points[14] = (this._buf1[1] & 0x3F) * 1000; // inverse event
				
				if ((_buf4[9] & 0xFF) > 100 && (_buf4[9] & 0xFF) < 120) { //SM MicroVario starts with FW version 1.00 -> 100
					try {
						this.points[15] = Integer.parseInt(String.format(Locale.ENGLISH, "%c%c%c%c%c0", _buf2[4], _buf2[5], _buf2[6], _buf2[8], _buf2[9]).trim());
						this.points[16] = Integer.parseInt(String.format(Locale.ENGLISH, "%c%c%c%c%c0", _buf3[1], _buf3[2], _buf3[3], _buf3[5], _buf3[6]).trim());
						this.points[17] = Integer.parseInt(String.format(Locale.ENGLISH, "%c%c%c%c%c0", _buf3[8], _buf3[9], _buf4[0], _buf4[2], _buf4[3]).trim());
					}
					catch (NumberFormatException e) {
						byte[] tmpArray = new byte[21];
						System.arraycopy(_buf2, 4, tmpArray, 0, 6);
						System.arraycopy(_buf3, 0, tmpArray, 6, 10);
						System.arraycopy(_buf4, 0, tmpArray, 16, 5);
						log.log(Level.WARNING, "'" + new String(tmpArray) + "'");
					}
					this.points[18] = (_buf4[8] & 0xFF) * 1000; //AirSpeed/2
					this.points[19] = (_buf4[9] & 0xFF) * 1000; //SM MicroVario starts with FW version 1.00 -> 100
				}
				return true;
			}
			else 
				System.out.println();
			this.points[14] = (this._buf1[1] & 0x3F) * 1000; // inverse event
			return isPointsValid();
		}

		private boolean isPointsValid() {
			return !this.pickerParameters.isFilterEnabled || (this.tmpHeight >= -490 && this.tmpHeight < 5000);
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if (this.points[10] != 0 || this.points[11] != 0 || this.points[12] != 0 || this.points[13] != 0) {
				for (int j = 10; j < 20; j++) {
					targetPoints[j] = this.points[j];
				}
			}
		}

	}

	public static class GpsBinParser extends BinParser {
		private int			tmpDate								= 0;
		private int			tmpTime								= 0;
		private int			tmpHeight							= 0;
		private int			tmpClimb1							= 0;
		private int			tmpClimb3							= 0;
		private int			tmpVelocity						= 0;
		private int			tmpLatitude						= 0;
		private int			tmpLatitudeDelta			= 0;
		private int			tmpLongitude					= 0;
		private int			tmpLongitudeDelta			= 0;
		private double	latitudeTolerance			= 1;
		private long		lastLatitudeTimeStep	= 0;
		private double	longitudeTolerance		= 1;
		private long		lastLongitudeTimeStep	= 0;

		protected GpsBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
		}

		protected GpsBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.GPS);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
			// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
			// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
			this.tmpHeight = DataParser.parse2Short(this._buf2, 8) - 500;
			this.tmpClimb1 = (DataParser.parse2UnsignedShort(this._buf3, 0) - 30000);
			this.tmpClimb3 = (this._buf3[2] & 0xFF) - 120;
			this.tmpVelocity = DataParser.parse2UnsignedShort(this._buf1, 4) * 1000;
			if (isPointsValid()) {
				this.points[22] = this.pickerParameters.isFilterEnabled && this.tmpVelocity > 500000 ? this.points[22] : this.tmpVelocity;

				this.tmpLatitude = DataParser.parse2UnsignedShort(this._buf1, 7) * 10000 + DataParser.parse2UnsignedShort(this._buf1[9], this._buf2[0]);
				this.tmpLatitude = this._buf1[6] == 1 ? -1 * this.tmpLatitude : this.tmpLatitude;
				this.tmpLatitudeDelta = Math.abs(this.tmpLatitude - this.points[20]);
				this.tmpLatitudeDelta = this.tmpLatitudeDelta > 400000 ? this.tmpLatitudeDelta - 400000 : this.tmpLatitudeDelta;
				this.latitudeTolerance = (this.points[22] / 1000.0) * (this.getTimeStep_ms() - this.lastLatitudeTimeStep) / this.pickerParameters.latitudeToleranceFactor;
				this.latitudeTolerance = this.latitudeTolerance > 0 ? this.latitudeTolerance : 25;

				if (!this.pickerParameters.isFilterEnabled || this.points[20] == 0 || this.tmpLatitudeDelta <= this.latitudeTolerance) {
					this.lastLatitudeTimeStep = this.getTimeStep_ms();
					this.points[20] = this.tmpLatitude;
				} else {
					if (HoTTbinReader2.log.isLoggable(Level.INFO))
						HoTTbinReader2.log.log(Level.INFO, StringHelper.getFormatedTime("HH:mm:ss:SSS", this.getTimeStep_ms() - GDE.ONE_HOUR_MS) + " Lat " + this.tmpLatitude + " - " + this.tmpLatitudeDelta+ " - " + this.latitudeTolerance);
				}

				this.tmpLongitude = DataParser.parse2UnsignedShort(this._buf2, 2) * 10000 + DataParser.parse2UnsignedShort(this._buf2, 4);
				this.tmpLongitude = this._buf2[1] == 1 ? -1 * this.tmpLongitude : this.tmpLongitude;
				this.tmpLongitudeDelta = Math.abs(this.tmpLongitude - this.points[21]);
				this.tmpLongitudeDelta = this.tmpLongitudeDelta > 400000 ? this.tmpLongitudeDelta - 400000 : this.tmpLongitudeDelta;
				this.longitudeTolerance = (this.points[22] / 1000.0) * (this.getTimeStep_ms() - this.lastLongitudeTimeStep) / this.pickerParameters.longitudeToleranceFactor;
				this.longitudeTolerance = this.longitudeTolerance > 0 ? this.longitudeTolerance : 45;

				if (!this.pickerParameters.isFilterEnabled || this.points[21] == 0 || this.tmpLongitudeDelta <= this.longitudeTolerance) {
					this.lastLongitudeTimeStep = this.getTimeStep_ms();
					this.points[21] = this.tmpLongitude;
				} else {
					if (HoTTbinReader2.log.isLoggable(Level.INFO))
						HoTTbinReader2.log.log(Level.INFO, StringHelper.getFormatedTime("HH:mm:ss:SSS", this.getTimeStep_ms() - GDE.ONE_HOUR_MS) + " Long " + this.tmpLongitude + " - " + this.tmpLongitudeDelta + " - " + this.longitudeTolerance);
				}

				if (this.pickerParameters.altitudeClimbSensorSelection == 2) { //sensor selection GPS (auto, Vario, GPS, GAM, EAM)
					this.points[10] = this.tmpHeight * 1000;
					this.points[11] = this.tmpClimb1 * 10;
					this.points[12] = this.tmpClimb3 * 1000;
				}
				this.points[23] = DataParser.parse2UnsignedShort(this._buf2, 6) * 1000;
				this.points[24] = (this._buf1[3] & 0xFF) * 1000;
				this.points[25] = 0;
				this.points[26] = (this._buf3[3] & 0xFF) * 1000;
				
				switch (_buf3[4]) { //sat-fix
				case '-':
					this.points[27] = 0;
					break;
				case '2':
					this.points[27] = 2000;
					break;
				case '3':
					this.points[27] = 3000;
					break;
				case 'D':
					this.points[27] = 4000;
					break;
				default:
					try {
						this.points[27] = Integer.valueOf(String.format("%c", 0xFF & _buf3[4])) * 1000;
					}
					catch (NumberFormatException e1) {
						this.points[27] = 1000;
					}
					break;
				}
				this.points[28] = (this._buf1[1] & 0x0F) * 1000; // inverse event
				this.points[29] = (_buf3[5] & 0xFF) * 1000;
				// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=ENL 37=Version	
				if ((_buf4[9] & 0xFF) > 100) { //SM GPS-Logger
					//30=servoPulse 31=AirSpeed 32=n/a 33=GyroX 34=GyroY 35=GyroZ 36=ENL 37=Version	
					this.points[30] = _buf3[6] * 1000; 
					this.points[31] = DataParser.parse2UnsignedShort(_buf3, 7) * 1000;
					this.points[33] = DataParser.parse2Short(_buf3[9], _buf4[0]) * 1000;
					this.points[34] = DataParser.parse2Short(_buf4, 1) * 1000;
					this.points[35] = DataParser.parse2Short(_buf4, 3) * 1000;
					this.points[36] = (_buf4[5] & 0xFF) * 1000;
				}
				else if ((_buf4[9] & 0xFF) == 4) { //RCE Electronics Sparrow
					//30=servoPulse 31=fixed 32=Voltage 33=GPS time 34=GPS date 35=MSL Altitude 37=ENL 37=Version	
					this.points[30] = _buf4[4] * 1000; 
					this.points[31] = 0; 
					this.points[32] = _buf3[8] * 100; 
					
					if (this.points[27] > 0) {  //Sat-Fix
						tmpTime = _buf3[9] * 10000000 + _buf4[0] * 100000 + _buf4[1] * 1000 + _buf4[2] * 10;//HH:mm:ss.SSS
						if (tmpTime < this.points[33]) 
							log.log(Level.WARNING, String.format("near time: %s %s", StringHelper.getFormatedTime("HH:mm:ss.SSS", this.timeSteps_ms[TIMESTEP_INDEX] - GDE.ONE_HOUR_MS), HoTTAdapter.getFormattedTime(tmpTime)));
						this.points[33] = tmpTime;

						tmpDate = ((_buf4[5] - 48) * 1000000 + (_buf4[7] - 48) * 10000 + (_buf4[6] - 48) * 100) * 10;//yy-MM-dd
						if (tmpDate < this.points[34]) 
							log.log(Level.WARNING, String.format("near time: %s Sat-Fix %d #Sats %d %s - %c %c %c", StringHelper.getFormatedTime("HH:mm:ss.SSS", this.timeSteps_ms[TIMESTEP_INDEX] - GDE.ONE_HOUR_MS),  this.points[27]/1000,  this.points[26]/1000, HoTTAdapter.getFormattedTime(this.points[33]), _buf4[5]&0xff, _buf4[7]&0xff, _buf4[6]&0xff));
						this.points[34] = tmpDate;

						this.points[35] = (DataParser.parse2Short(_buf3, 6) - 500) * 1000; //TODO remove offset 500 after correction
					}
					else this.points[33] = this.points[34] = this.points[35] = 0;
					
					this.points[36] = (_buf4[3] & 0xFF) * 1000;
					//log.log(Level.INFO, StringHelper.byte2Hex2CharString(_buf4, _buf4.length));
				}
				else if ((_buf4[9] & 0xFF) == 0 || (_buf4[9] & 0xFF) == 1) { //Graupner GPS need workaround to distinguish between different Graupner GPS version #0
					int version = this.points[37] == 1000 || (_buf3[6] != 0 && _buf3[7] != 0 && _buf3[8] != 0) ? 1 : 0;
						
					if (version == 0) { //#0=GPS 33600
						//25=Roll 26=Pitch 27=Yaw 28=GPS time 29=free 30=MSL Altitude 31=Vibration 32=Version	
						this.points[30] = _buf3[6] * 1000; //0
						this.points[31] = _buf3[7] * 1000; //0
						this.points[32] = _buf3[8] * 1000; //0
						this.points[33] = _buf3[9] * 10000000 + _buf4[0] * 100000 + _buf4[1] * 1000 + _buf4[2]*10;//HH:mm:ss.SSS
						this.points[34] = 0;
						this.points[35] = DataParser.parse2Short(_buf4, 3) * 1000;
						this.points[36] = (_buf4[5] & 0xFF) * 1000; //0
					}
					else { //#1= 33602/S8437
						//16=velN NED north velocity mm/s 17=n/a 18=sAcc Speed accuracy estimate cm/s
						this.points[30] = DataParser.parse2Short(_buf3, 6) * 1000;
						this.points[31] = 0;
						this.points[32] = _buf3[8] * 1000; 
						//19=GPS time 20=free 21=velE NED east velocity mm/s
						this.points[33] = _buf3[9] * 10000000 + _buf4[0] * 100000 + _buf4[1] * 1000 + _buf4[2]*10;//HH:mm:ss.SSS
						this.points[34] = 0;
						this.points[35] = DataParser.parse2Short(_buf4, 3) * 1000;
						//22=hAcc Horizontal accuracy estimate HDOP 			
						//61=Vibration 62-64=freeChars 65=Version
						this.points[36] = (_buf4[5] & 0xFF) * 1000;
					}
				}
				else { //unknown GPS
					//16=Roll 17=Pitch 18=Yaw 19=GPS time1 20=GPS time2 21=AltitudeMSL 22=Vibration
					this.points[30] = _buf3[6] * 1000; 
					this.points[31] = _buf3[7] * 1000; 
					this.points[32] = _buf3[8] * 1000; 
					this.points[33] = DataParser.parse2Short(_buf3[9], _buf4[0]) * 1000;
					this.points[34] = DataParser.parse2Short(_buf4, 1) * 1000;
					this.points[35] = DataParser.parse2Short(_buf4, 3) * 1000;
					this.points[36] = (_buf4[5] & 0xFF) * 1000;
				}
				this.points[37] = (_buf4[9] & 0xFF) * 1000;
				return true;
			}
			this.points[28] = (this._buf1[1] & 0x0F) * 1000; // inverse event
			return false;
		}

		private boolean isPointsValid() {
			return !this.pickerParameters.isFilterEnabled || (this.tmpClimb1 > -20000 && this.tmpClimb3 > -90 && this.tmpHeight >= -490 && this.tmpHeight < 4500);
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if ((targetPoints[10] != 0 && this.points[10] != 0) || this.points[10] != 0) {
				// 10=Altitude, 11=Climb 1, 12=Climb 3
				for (int j = 10; j < 13; j++) {
					targetPoints[j] = this.points[j];
				}
			}
			// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
			// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
			for (int k = 20; k < 38; k++) {
				targetPoints[k] = this.points[k];
			}
		}

	}

	public static class GamBinParser extends BinParser {
		private int	tmpHeight		= 0;
		private int	tmpClimb3		= 0;
		private int	tmpVoltage1	= 0;
		private int	tmpVoltage2	= 0;
		private int	tmpCapacity	= 0;

		private int	parseCount	= 0;

		protected GamBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
		}

		protected GamBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.GAM);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			this.tmpHeight = DataParser.parse2Short(this._buf3, 0) - 500;
			this.tmpClimb3 = (this._buf3[4] & 0xFF) - 120;
			this.tmpVoltage1 = DataParser.parse2Short(this._buf1[9], this._buf2[0]);
			this.tmpVoltage2 = DataParser.parse2Short(this._buf2, 1);
			this.tmpCapacity = DataParser.parse2Short(this._buf3[9], this._buf4[0]);
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
			// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
			// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
			// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
			// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
			// 57=LowestCellNumber, 58=Pressure, 59=Event G
			if (isPointsValid()) {
				int maxVotage = Integer.MIN_VALUE;
				int minVotage = Integer.MAX_VALUE;
				this.points[38] = DataParser.parse2Short(this._buf3, 7) * 1000;
				this.points[39] = DataParser.parse2Short(this._buf3, 5) * 1000;
				if (!this.pickerParameters.isFilterEnabled || this.parseCount <= 20
						|| (this.tmpCapacity != 0 && Math.abs(this.tmpCapacity) <= (this.points[40] / 1000 + this.points[38] / 1000 * this.points[39] / 1000 / 2500 + 2))) {
					this.points[40] = this.tmpCapacity * 1000;
				} else {
					if (HoTTbinReader2.log.isLoggable(Level.FINE))
						HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", this.getTimeStep_ms()) + " - " + this.tmpCapacity + " - " + (this.points[26] / 1000) + " + " + (this.points[24] / 1000 * this.points[25] / 1000 / 2500 + 2));
				}
				this.points[41] = Double.valueOf(this.points[38] / 1000.0 * this.points[39]).intValue();
				// cell voltage
				for (int j = 0; j < 6; j++) {
					this.points[j + 43] = (this._buf1[3 + j] & 0xFF) * 1000;
					if (this.points[j + 43] > 0) {
						maxVotage = this.points[j + 43] > maxVotage ? this.points[j + 43] : maxVotage;
						minVotage = this.points[j + 43] < minVotage ? this.points[j + 43] : minVotage;
					}
				}
				this.points[42] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
				this.points[49] = DataParser.parse2UnsignedShort(this._buf2, 8) * 1000;
				this.points[50] = DataParser.parse2Short(this._buf2, 6) * 1000;
				if (this.pickerParameters.altitudeClimbSensorSelection == 3) { //sensor selection GPS (auto, Vario, GPS, GAM, EAM)
					this.points[10] = this.tmpHeight * 1000;
					this.points[11] = (DataParser.parse2UnsignedShort(this._buf3, 2) - 30000) * 10;
					this.points[12] = this.tmpClimb3 * 1000;
				}
				this.points[51] = this.tmpVoltage1 * 100;
				this.points[52] = this.tmpVoltage2 * 100;
				this.points[53] = ((this._buf2[3] & 0xFF) - 20) * 1000;
				this.points[54] = ((this._buf2[4] & 0xFF) - 20) * 1000;
				this.points[55] = DataParser.parse2UnsignedShort(this._buf4, 1) * 1000; // Speed [km/h
				this.points[56] = (this._buf4[3] & 0xFF) * 1000; // lowest cell voltage 124 = 2.48 V
				this.points[57] = (this._buf4[4] & 0xFF) * 1000; // cell number lowest cell voltage
				this.points[58] = (this._buf4[8] & 0xFF) * 1000; // Pressure
				this.points[59] = ((this._buf1[1] & 0xFF) + ((this._buf1[2] & 0x7F) << 8)) * 1000; // inverse event
				++this.parseCount;
				return true;
			}
			this.points[59] = ((this._buf1[1] & 0xFF) + ((this._buf1[2] & 0x7F) << 8)) * 1000; // inverse event
			++this.parseCount;
			return false;
		}

		private boolean isPointsValid() {
			// WB !this.pickerParameters.isFilterEnabled || (this.tmpClimb3 > 30 && this.tmpHeight > 10 && this.tmpHeight < 5000 && Math.abs(this.tmpVoltage1) < 600 && Math.abs(this.tmpVoltage2) < 600); from HoTTbinReader2
			return !this.pickerParameters.isFilterEnabled || (this.tmpClimb3 > -90 && this.tmpHeight >= -490 && this.tmpHeight < 5000 && Math.abs(this.tmpVoltage1) < 600 && Math.abs(this.tmpVoltage2) < 600);
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if ((targetPoints[10] != 0 && this.points[10] != 0) || this.points[10] != 0) { 
				// 10=Altitude, 11=Climb 1, 12=Climb 3
				for (int j = 10; j < 13; j++) {
					targetPoints[j] = this.points[j];
				}
			}
			// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
			// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
			// 57=LowestCellNumber, 58=Pressure, 59=Event G
			for (int k = 38; k < 60; k++) {
				targetPoints[k] = this.points[k];
			}
		}

	}

	public static class EamBinParser extends BinParser {
		private int	tmpHeight		= 0;
		private int	tmpClimb3		= 0;
		private int	tmpVoltage1	= 0;
		private int	tmpVoltage2	= 0;
		private int	tmpCapacity	= 0;

		private int	parseCount	= 0;

		protected EamBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
		}

		protected EamBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.EAM);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
			// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
			// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
			// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
			// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
			// 57=LowestCellNumber, 58=Pressure, 59=Event G
			// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
			// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
			this.tmpHeight = DataParser.parse2Short(this._buf3, 3) - 500;
			this.tmpClimb3 = (this._buf4[3] & 0xFF) - 120;
			this.tmpVoltage1 = DataParser.parse2Short(this._buf2, 7);
			this.tmpVoltage2 = DataParser.parse2Short(this._buf2[9], this._buf3[0]);
			this.tmpCapacity = DataParser.parse2Short(this._buf3[9], this._buf4[0]);
			if (isPointsValid()) {
				int maxVotage = Integer.MIN_VALUE;
				int minVotage = Integer.MAX_VALUE;
				this.points[60] = DataParser.parse2Short(this._buf3, 7) * 1000;
				this.points[61] = DataParser.parse2Short(this._buf3, 5) * 1000;
				if (!this.pickerParameters.isFilterEnabled || this.parseCount <= 20
						|| Math.abs(this.tmpCapacity) <= (this.points[62] / 1000 + this.points[60] / 1000 * this.points[61] / 1000 / 2500 + 2)) {
					this.points[62] = this.tmpCapacity * 1000;
				} else {
					if (HoTTbinReader2.log.isLoggable(Level.FINE))
						HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", this.getTimeStep_ms()) + " - " + this.tmpCapacity + " - " + (this.points[48] / 1000) + " + " + (this.points[46] / 1000 * this.points[47] / 1000 / 2500 + 2));
				}
				this.points[63] = Double.valueOf(this.points[60] / 1000.0 * this.points[61]).intValue(); // power U*I [W];
				for (int j = 0; j < 7; j++) {
					this.points[j + 65] = (this._buf1[3 + j] & 0xFF) * 1000;
					if (this.points[j + 65] > 0) {
						maxVotage = this.points[j + 65] > maxVotage ? this.points[j + 65] : maxVotage;
						minVotage = this.points[j + 65] < minVotage ? this.points[j + 65] : minVotage;
					}
				}
				for (int j = 0; j < 7; j++) {
					this.points[j + 72] = (this._buf2[j] & 0xFF) * 1000;
					if (this.points[j + 72] > 0) {
						maxVotage = this.points[j + 72] > maxVotage ? this.points[j + 72] : maxVotage;
						minVotage = this.points[j + 72] < minVotage ? this.points[j + 72] : minVotage;
					}
				}
				// calculate balance on the fly
				this.points[64] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
				if (this.pickerParameters.altitudeClimbSensorSelection == 4) { //sensor selection GPS (auto, Vario, GPS, GAM, EAM)
					this.points[10] = this.tmpHeight * 1000;
					this.points[11] = (DataParser.parse2UnsignedShort(this._buf4, 1) - 30000) * 10;
					this.points[12] = this.tmpClimb3 * 1000;
				}
				this.points[79] = this.tmpVoltage1 * 100;
				this.points[80] = this.tmpVoltage2 * 100;
				this.points[81] = ((this._buf3[1] & 0xFF) - 20) * 1000;
				this.points[82] = ((this._buf3[2] & 0xFF) - 20) * 1000;
				this.points[83] = DataParser.parse2UnsignedShort(this._buf4, 4) * 1000;
				this.points[84] = ((this._buf4[6] & 0xFF) * 60 + (this._buf4[7] & 0xFF)) * 1000; 	// motor time
				this.points[85] = DataParser.parse2Short(this._buf4, 8) * 1000; 									// speed
				this.points[86] = ((this._buf1[1] & 0xFF) + ((this._buf1[2] & 0x7F) << 8)) * 1000;// inverse event
				++this.parseCount;
				return true;
			}
			this.points[86] = ((this._buf1[1] & 0xFF) + ((this._buf1[2] & 0x7F) << 8)) * 1000; // inverse event
			return false;
		}

		private boolean isPointsValid() {
			// WB !this.pickerParameters.isFilterEnabled || (this.tmpClimb3 > 30 && this.tmpHeight > 10 && this.tmpHeight < 5000 && Math.abs(this.tmpVoltage1) < 600 && Math.abs(this.tmpVoltage2) < 600); from HoTTbinReader2
			return !this.pickerParameters.isFilterEnabled || (this.tmpClimb3 > -90 && this.tmpHeight >= -490 && this.tmpHeight < 5000 && Math.abs(this.tmpVoltage1) < 600 && Math.abs(this.tmpVoltage2) < 600);
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if ((targetPoints[10] != 0 && this.points[10] != 0) || this.points[10] != 0) {
				// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
				for (int j = 10; j < 13; j++) {
					targetPoints[j] = this.points[j];
				}
			}
			// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
			// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
			for (int k = 60; k < 87; k++) {
				targetPoints[k] = this.points[k];
			}
		}

	}

	public static class ChnBinParser extends BinParser {
		protected final byte[] _buf;

		protected ChnBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
			throw new UnsupportedOperationException("use in situ parsing");
		}

		protected ChnBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.CHANNEL);
			_buf = buffers[0];
			if (buffers.length != 1) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=Ch17 ... 118=Ch32  119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
			this.points[4] = (this._buf[3] & 0xFF) * -1000;
			this.points[5] = (this._buf[4] & 0xFF) * -1000;

			this.points[87] = (DataParser.parse2UnsignedShort(this._buf, 8) / 2) * 1000;
			this.points[88] = (DataParser.parse2UnsignedShort(this._buf, 10) / 2) * 1000;
			this.points[89] = (DataParser.parse2UnsignedShort(this._buf, 12) / 2) * 1000;
			this.points[90] = (DataParser.parse2UnsignedShort(this._buf, 14) / 2) * 1000;
			this.points[91] = (DataParser.parse2UnsignedShort(this._buf, 16) / 2) * 1000;
			this.points[92] = (DataParser.parse2UnsignedShort(this._buf, 18) / 2) * 1000;
			this.points[93] = (DataParser.parse2UnsignedShort(this._buf, 20) / 2) * 1000;
			this.points[94] = (DataParser.parse2UnsignedShort(this._buf, 22) / 2) * 1000;

			if (this._buf[5] == 0x00) { // channel 9-12
				this.points[95] = (DataParser.parse2UnsignedShort(this._buf, 24) / 2) * 1000;
				this.points[96] = (DataParser.parse2UnsignedShort(this._buf, 26) / 2) * 1000;
				this.points[97] = (DataParser.parse2UnsignedShort(this._buf, 28) / 2) * 1000;
				this.points[98] = (DataParser.parse2UnsignedShort(this._buf, 30) / 2) * 1000;
				if (this.points[99] == 0) {
					this.points[99] = 1500 * 1000;
					this.points[100] = 1500 * 1000;
					this.points[101] = 1500 * 1000;
					this.points[102] = 1500 * 1000;
				}
			} else { // channel 13-16
				this.points[99] = (DataParser.parse2UnsignedShort(this._buf, 24) / 2) * 1000;
				this.points[100] = (DataParser.parse2UnsignedShort(this._buf, 26) / 2) * 1000;
				this.points[101] = (DataParser.parse2UnsignedShort(this._buf, 28) / 2) * 1000;
				this.points[102] = (DataParser.parse2UnsignedShort(this._buf, 30) / 2) * 1000;
				if (this.points[95] == 0) {
					this.points[95] = 1500 * 1000;
					this.points[96] = 1500 * 1000;
					this.points[97] = 1500 * 1000;
					this.points[98] = 1500 * 1000;
				}
			}
			// events 119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
			this.points[119] = (this._buf[50] & 0x01) * 100000;
			this.points[120] = (this._buf[50] & 0x02) * 50000;
			this.points[121] = (this._buf[50] & 0x04) * 25000;
			if (this._buf[37] > 0 && this._buf[37] < 27)
				this.points[122] = this._buf[32] * 1000; // warning
			else
				this.points[122] = 0;
			return true;
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			for (int j = 4; j < 7; j++) {
				targetPoints[j] = this.points[j];
			}
			// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 
			for (int j = 87; j < 87+16; j++) {
				targetPoints[j] = this.points[j];
			}
			// points.length = 136 -> 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
			// points.length = 152 -> 119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
			throw new UnsupportedOperationException("use in situ parsing");
		}

	}

	public static class EscBinParser extends BinParser {
		private final boolean	isChannelsChannel;

		private int						tmpTemperatureFet	= 0;
		private int						tmpVoltage				= 0;
		private int						tmpCurrent				= 0;
		private int						tmpRevolution			= 0;
		private int						tmpCapacity				= 0;

		private int						parseCount				= 0;
		
		protected boolean isChannelsChannel() { return this.isChannelsChannel; }

		protected EscBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
		}

		protected EscBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.ESC);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
			this.isChannelsChannel = this.pickerParameters.analyzer.getActiveChannel().getNumber() == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER;
		}

		@Override
		protected boolean parse() {
			this.tmpVoltage = DataParser.parse2Short(this._buf1, 3);
			this.tmpCurrent = DataParser.parse2Short(this._buf2, 1);
			this.tmpCapacity = DataParser.parse2Short(this._buf1, 7);
			this.tmpRevolution = DataParser.parse2UnsignedShort(this._buf2, 5);
			this.tmpTemperatureFet = (this._buf1[9] & 0xFF) - 20;
			if (this.isChannelsChannel) {
				// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
				// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
				// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
				// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
				if (isPointsValid()) {
					this.points[123] = this.tmpVoltage * 1000;
					this.points[124] = this.tmpCurrent * 1000;
					this.points[126] = Double.valueOf(this.points[107] / 1000.0 * this.points[108]).intValue();
					if (!this.pickerParameters.isFilterEnabled || this.parseCount <= 20
							|| (this.tmpCapacity != 0 && Math.abs(this.tmpCapacity) <= (this.points[109] / 1000 + this.tmpVoltage * this.tmpCurrent / 2500 + 2))) {
						this.points[125] = this.tmpCapacity * 1000;
					} else {
						if (this.tmpCapacity != 0 && HoTTbinReader2.log.isLoggable(Level.FINE))
							HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", this.getTimeStep_ms()) + " - " + this.tmpCapacity + " - " + (this.points[104] / 1000) + " + " + (this.tmpVoltage * this.tmpCurrent / 2500 + 2));
					}
					this.points[127] = this.tmpRevolution * 1000;
					this.points[128] = this.tmpTemperatureFet * 1000;

					this.points[129] = ((this._buf2[9] & 0xFF) - 20) * 1000;
					this.points[130] = DataParser.parse2Short(this._buf1, 5) * 1000;
					this.points[131] = DataParser.parse2Short(this._buf2, 3) * 1000;
					this.points[132] = DataParser.parse2UnsignedShort(this._buf2, 7) * 1000;
					this.points[133] = ((this._buf2[0] & 0xFF) - 20) * 1000;
					this.points[134] = ((this._buf3[0] & 0xFF) - 20) * 1000;
					this.points[135] = (this._buf1[1] & 0xFF) * 1000; // inverse event
					
					if ((_buf4[9] & 0xFF) == 3) { //Extended YGE protocol 				
						// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
						// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
						this.points[136] = DataParser.parse2Short(_buf3, 1) * 1000; //Speed
						this.points[137] = DataParser.parse2Short(_buf3, 3) * 1000; //Speed max
						this.points[138] = (_buf3[5] & 0xFF) * 1000; 								//PWM
						this.points[139] = (_buf3[6] & 0xFF) * 1000; 								//Throttle
						this.points[140] = (_buf3[7] & 0xFF) * 1000; 								//BEC Voltage
						this.points[141] = (_buf3[8] & 0xFF) * 1000; 								//BEC Voltage min
						this.points[142] = DataParser.parse2UnsignedShort(_buf3[9], _buf4[0]) * 1000; 	//BEC Current
						this.points[143] = ((_buf4[1] & 0xFF) - 20) * 1000; 				//BEC Temperature
						this.points[144] = ((_buf4[2] & 0xFF) - 20) * 1000; 				//Capacity Temperature
						this.points[145] = (_buf4[3] & 0xFF) * 1000; 								//Timing
						this.points[146] = ((_buf4[4] & 0xFF) - 20) * 1000; 				//Aux Temperature
						this.points[147] = DataParser.parse2Short(_buf4, 5) * 1000; //Gear
						this.points[146] = (_buf4[7] & 0xFF) * 1000; 								//YGEGenExt
						this.points[149] = (_buf4[8] & 0xFF) * 1000; 								//MotStatEscNr
						this.points[150] = 0; 																			//spare
						this.points[151] = (_buf4[9] & 0xFF) * 1000; 								//Version ESC
					}
					else if ((_buf4[9] & 0xFF) >= 128) { //Extended CS-Electronics
						//136=AirSpeed 137=AirSpeed_max 138=PWM 139=Throttle 140=VoltagePump 141=VoltagePump_min 142=Flow 143=Fuel 144=Power 
						//145=Thrust 146=TemperaturePump 147=EngineStat 148=spare 149=spare 150=spare 151=version
						this.points[136] = DataParser.parse2Short(_buf3, 1) * 1000; 	//AirSpeed
						this.points[137] = DataParser.parse2Short(_buf3, 3) * 1000; 	//AirSpeed max
						this.points[138] = (_buf3[5] & 0xFF) * 1000; 									//PWM
						this.points[139] = (_buf3[6] & 0xFF) * 1000; 									//Throttle
						this.points[140] = (_buf3[7] & 0xFF) * 1000; 									//Pump Voltage
						this.points[141] = (_buf3[8] & 0xFF) * 1000; 									//Pump Voltage min
						this.points[142] = DataParser.parse2UnsignedShort(_buf3[9], _buf4[0]) * 1000;	//Flow
						this.points[143] = DataParser.parse2UnsignedShort(_buf4, 1) * 1000;						//Fuel ml
						this.points[144] = DataParser.parse2UnsignedShort(_buf4, 3) * 1000; 					//Power Wh
						this.points[145] = DataParser.parse2UnsignedShort(_buf4, 5) * 1000; 					//Thrust
						this.points[146] = ((_buf4[7] & 0xFF) - 20) * 1000; 					//Pump Temperature
						this.points[147] = (_buf4[8] & 0xFF) * 1000; 									//Engine run
						this.points[148] = 0; 																				//spare
						this.points[149] = 0; 																				//spare
						this.points[150] = 0; 																				//spare
						this.points[151] = (_buf4[9] & 0xFF) * 1000; 									//Version ESC			
					}
					return true;
				}
				this.points[135] = (this._buf1[1] & 0xFF) * 1000; // inverse event
				return false;
			} else {
				// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
				// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M
				// 100=Speed 101=Speed_max 102=PWM 103=Throttle 104=VoltageBEC 105=VoltageBEC_max 106=CurrentBEC 107=TemperatureBEC 108=TemperatureCap 
				// 109=Timing(empty) 110=Temperature_aux 111=Gear 112=YGEGenExt 113=MotStatEscNr 114=misc ESC_15 115=VersionESC
				if (isPointsValid()) {
					this.points[87] = this.tmpVoltage * 1000;
					this.points[88] = this.tmpCurrent * 1000;
					this.points[90] = Double.valueOf(this.points[87] / 1000.0 * this.points[88]).intValue();
					if (!this.pickerParameters.isFilterEnabled || this.parseCount <= 20
							|| (this.tmpCapacity != 0 && Math.abs(this.tmpCapacity) <= (this.points[89] / 1000 + this.tmpVoltage * this.tmpCurrent / 2500 + 2))) {
						this.points[89] = this.tmpCapacity * 1000;
					} else {
						if (this.tmpCapacity != 0 && HoTTbinReader2.log.isLoggable(Level.FINE))
							HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", this.getTimeStep_ms()) + " - " + this.tmpCapacity + " - " + (this.points[84] / 1000) + " + " + (this.tmpVoltage * this.tmpCurrent / 2500 + 2));
					}
					this.points[91] = this.tmpRevolution * 1000;
					this.points[92] = this.tmpTemperatureFet * 1000;

					this.points[93] = ((this._buf2[9] & 0xFF) - 20) * 1000;
					this.points[94] = DataParser.parse2Short(this._buf1, 5) * 1000;
					this.points[95] = DataParser.parse2Short(this._buf2, 3) * 1000;
					this.points[96] = DataParser.parse2UnsignedShort(this._buf2, 7) * 1000;
					this.points[97] = ((this._buf2[0] & 0xFF) - 20) * 1000;
					this.points[98] = ((this._buf3[0] & 0xFF) - 20) * 1000;
					this.points[99] = (this._buf1[1] & 0xFF) * 1000; // inverse event
					
					if ((_buf4[9] & 0xFF) == 3) { //Extended YGE protocol 				
						// 100=Speed 101=Speed_max 102=PWM 103=Throttle 104=VoltageBEC 105=VoltageBEC_max 106=CurrentBEC 107=TemperatureBEC 108=TemperatureCap 
						// 109=Timing(empty) 110=Temperature_aux 111=Gear 112=YGEGenExt 113=MotStatEscNr 114=VersionESC
						this.points[100] = DataParser.parse2Short(_buf3, 1) * 1000; //Speed
						this.points[101] = DataParser.parse2Short(_buf3, 3) * 1000; //Speed max
						this.points[102] = (_buf3[5] & 0xFF) * 1000; 								//PWM
						this.points[103] = (_buf3[6] & 0xFF) * 1000; 								//Throttle
						this.points[104] = (_buf3[7] & 0xFF) * 1000; 								//BEC Voltage
						this.points[105] = (_buf3[8] & 0xFF) * 1000; 								//BEC Voltage min
						this.points[106] = DataParser.parse2UnsignedShort(_buf3[9], _buf4[0]) * 1000; 	//BEC Current
						this.points[107] = ((_buf4[1] & 0xFF) - 20) * 1000; 				//BEC Temperature
						this.points[108] = ((_buf4[2] & 0xFF) - 20) * 1000; 				//Capacity Temperature
						this.points[109] = (_buf4[3] & 0xFF) * 1000; 								//Timing
						this.points[110] = ((_buf4[4] & 0xFF) - 20) * 1000; 				//Aux Temperature
						this.points[111] = DataParser.parse2Short(_buf4, 5) * 1000; //Gear
						this.points[112] = (_buf4[7] & 0xFF) * 1000; 								//YGEGenExt
						this.points[113] = (_buf4[8] & 0xFF) * 1000; 								//MotStatEscNr
						this.points[114] = 0;																				//spare
						this.points[115] = (_buf4[9] & 0xFF) * 1000; 								//Version ESC
					}
					else if ((_buf4[9] & 0xFF) >= 128) { //Extended CS-Electronics
						//100=AirSpeed 101=AirSpeed_max 102=PWM 103=Throttle 104=VoltagePump 105=VoltagePump_min 106=Flow 107=Fuel 108=Power 
						//109=Thrust 110=TemperaturePump 111=EngineStat 112=spare 113=spare 114=spare 115=version
						this.points[100] = DataParser.parse2Short(_buf3, 1) * 1000; 	//AirSpeed
						this.points[101] = DataParser.parse2Short(_buf3, 3) * 1000; 	//AirSpeed max
						this.points[102] = (_buf3[5] & 0xFF) * 1000; 									//PWM
						this.points[103] = (_buf3[6] & 0xFF) * 1000; 									//Throttle
						this.points[104] = (_buf3[7] & 0xFF) * 1000; 									//Pump Voltage
						this.points[105] = (_buf3[8] & 0xFF) * 1000; 									//Pump Voltage min
						this.points[106] = DataParser.parse2UnsignedShort(_buf3[9], _buf4[0]) * 1000;	//Flow
						this.points[107] = DataParser.parse2UnsignedShort(_buf4, 1) * 1000;						//Fuel ml
						this.points[108] = DataParser.parse2UnsignedShort(_buf4, 3) * 1000; 					//Power Wh
						this.points[109] = DataParser.parse2UnsignedShort(_buf4, 5) * 1000; 					//Thrust
						this.points[110] = ((_buf4[7] & 0xFF) - 20) * 1000; 					//Pump Temperature
						this.points[111] = (_buf4[8] & 0xFF) * 1000; 									//Engine run
						this.points[112] = 0; 																				//spare
						this.points[113] = 0; 																				//spare
						this.points[114] = 0; 																				//spare
						this.points[115] = (_buf4[9] & 0xFF) * 1000; 									//Version ESC			
					}

					++this.parseCount;
					return true;
				}
				this.points[99] = (this._buf1[1] & 0xFF) * 1000; // inverse event
				++this.parseCount;
				return false;
			}
		}

		private boolean isPointsValid() {
			if (this.isChannelsChannel) {
				return !this.pickerParameters.isFilterEnabled
						|| this.tmpVoltage > 0 && this.tmpVoltage < 1000 && this.tmpCurrent < 4000 && this.tmpCurrent > -10 && this.tmpRevolution > -1
						&& this.tmpRevolution < 20000 && !(this.points[112] != 0 && this.points[112] / 1000 - this.tmpTemperatureFet > 20);
			} else {
				return !this.pickerParameters.isFilterEnabled
						|| this.tmpVoltage > 0 && this.tmpVoltage < 1000 && this.tmpCurrent < 4000 && this.tmpCurrent > -10 && this.tmpRevolution > -1
						&& this.tmpRevolution < 20000 && !(this.points[92] != 0 && this.points[92] / 1000 - this.tmpTemperatureFet > 20);
			}
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if (this.isChannelsChannel) {
				// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
				// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
				// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
				// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
				for (int j = 123; j < 123+29; j++) {
					targetPoints[j] = this.points[j];
				}
			} else {
				// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
				// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M
				// 100=Speed 101=Speed_max 102=PWM 103=Throttle 104=VoltageBEC 105=VoltageBEC_max 106=CurrentBEC 107=TemperatureBEC 108=TemperatureCap 
				// 109=Timing(empty) 110=Temperature_aux 111=Gear 112=YGEGenExt 113=MotStatEscNr 114=VersionESC
				for (int j = 87; j < 87+29; j++) {
					targetPoints[j] = this.points[j];
				}
			}
		}
	}
}
