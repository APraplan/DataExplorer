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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import gde.GDE;
import gde.device.graupner.HoTTAdapter.PickerParameters;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.device.graupner.HoTTlogReader.ChnLogParser;
import gde.device.graupner.HoTTlogReader.EamLogParser;
import gde.device.graupner.HoTTlogReader.EscLogParser;
import gde.device.graupner.HoTTlogReader.GamLogParser;
import gde.device.graupner.HoTTlogReader.GpsLogParser;
import gde.device.graupner.HoTTlogReader.LogParser;
import gde.device.graupner.HoTTlogReader.RcvLogParser;
import gde.device.graupner.HoTTlogReader.VarLogParser;
import gde.exception.DataInconsitsentException;
import gde.exception.ThrowableUtils;
import gde.histo.device.UniversalSampler;
import gde.log.Level;
import gde.utils.StringHelper;

/**
 * Read Graupner HoTT binary data for history analysis.
 * Collect data in a recordset and fill the vault collector.
 * Read measurements from multiple sensors for one single channel.
 * For small files (around 1 minute) no measurements are added to the recordset.
 * Support sampling to maximize the throughput.
 */
public class HoTTlogHistoReader2 extends HoTTlogHistoReader {
	private static final String	$CLASS_NAME									= HoTTlogHistoReader2.class.getName();
	private static final Logger	log													= Logger.getLogger(HoTTlogHistoReader2.$CLASS_NAME);

	/**
	 * the high number of measurement records increases the probability for excessive max/min values
	 */
	private static final int		INITIALIZE_SAMPLING_FACTOR	= 1;

	public HoTTlogHistoReader2(PickerParameters pickerParameters, HashMap<String, String> newInfoHeader) {
		super(pickerParameters, INITIALIZE_SAMPLING_FACTOR, newInfoHeader);
		HoTTlogReader2.isHoTTAdapter2 = analyzer.getActiveDevice() instanceof HoTTAdapter2;
	}

	/**
	 * read log data according to version 0 either in initialize mode for learning min/max values or in fully functional read mode.
	 * reads only sample records and allocates only one single record set.
	 * no progress bar support and no channel data modifications.
	 * @param data_in
	 * @param initializeBlocks if this number is greater than zero, the min/max values are initialized
	 * @param histoRandomSample is the random sampler which might use the minMax values from a previous run and thus reduces oversampling
	 */
	@Override
	protected void read(InputStream data_in, int initializeBlocks, UniversalSampler histoRandomSample) throws DataInconsitsentException, IOException {
		HoTTAdapter2 device = (HoTTAdapter2) analyzer.getActiveDevice();
		boolean isChannelsEnabled = analyzer.getActiveChannel().getNumber() == 4;
		boolean isReceiverData = false;
		boolean isVarioData = false;
		boolean isGPSData = false;
		boolean isGeneralData = false;
		boolean isElectricData = false;
		boolean isMotorDriverData = false;
		boolean[] isResetMinMax = new boolean[] {false, false, false, false, false}; //ESC, EAM, GAM, GPS, Vario
		int[]	points = histoRandomSample.getPoints();
		byte[] buf = new byte[this.dataBlockSize];
		long[] timeSteps_ms = new long[] { 0 };
		boolean isTextModusSignaled = false;
		int[] valuesRec = new int[10];
		int[] valuesChn = new int[23];
		int[] valuesVar = new int[13];
		int[] valuesGPS = new int[24];
		int[] valuesGAM = new int[26];
		int[] valuesEAM = new int[31];
		int[] valuesESC = new int[30];

		//set picker parameter setting sensor for altitude/climb usage (0=auto, 1=VARIO, 2=GPS, 3=GAM, 4=EAM)
		HoTTbinReader.setAltitudeClimbPickeParameter(pickerParameters, detectedSensors);
		RcvLogParser rcvLogParser = (RcvLogParser) Sensor.RECEIVER.createLogParser(pickerParameters, valuesRec, timeSteps_ms, buf, this.numberUsedChannels);
		ChnLogParser chnLogParser = (ChnLogParser) Sensor.CHANNEL.createLogParser(pickerParameters, valuesChn, timeSteps_ms, buf, this.numberUsedChannels);
		VarLogParser varLogParser = (VarLogParser) Sensor.VARIO.createLogParser(pickerParameters, valuesVar, timeSteps_ms, buf, this.numberUsedChannels);
		GpsLogParser gpsLogParser = (GpsLogParser) Sensor.GPS.createLogParser(pickerParameters, valuesGPS, timeSteps_ms, buf, this.numberUsedChannels);
		GamLogParser gamLogParser = (GamLogParser) Sensor.GAM.createLogParser(pickerParameters, valuesGAM, timeSteps_ms, buf, this.numberUsedChannels);
		EamLogParser eamLogParser = (EamLogParser) Sensor.EAM.createLogParser(pickerParameters, valuesEAM, timeSteps_ms, buf, this.numberUsedChannels);
		EscLogParser escLogParser = (EscLogParser) Sensor.ESC.createLogParser(pickerParameters, valuesESC, timeSteps_ms, buf, this.numberUsedChannels);
		boolean isJustMigrated = false;
		
		Set<LogParser> migrationJobs = new HashSet<>();
		@SuppressWarnings("null")
		Procedure migrator = () -> {
			// the sequence of the next statements is crucial, eg. for vario data
			if (migrationJobs.contains(eamLogParser)) eamLogParser.migratePoints(points);
			if (migrationJobs.contains(gamLogParser)) gamLogParser.migratePoints(points);
			if (migrationJobs.contains(gpsLogParser)) gpsLogParser.migratePoints(points);
			if (migrationJobs.contains(varLogParser)) varLogParser.migratePoints(points);
			if (migrationJobs.contains(escLogParser)) escLogParser.migratePoints(points);
			migrationJobs.clear();
		};

		Procedure pointsAdder = initializeBlocks <= 0 //
				? () -> {
					readTimer.invoke();
					boolean isValidSample = histoRandomSample.capturePoints(timeSteps_ms[LogParser.TIMESTEP_INDEX]);
					reviewTimer.invoke();
					if (isValidSample) {
						try {
							int[] histoRandomSamplePoints = histoRandomSample.getSamplePoints();
							
							// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
							if (!isResetMinMax[4]) {
								for (int j=10; j<19; ++j) {
									tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
								}
								isResetMinMax[4] = true;
							}
							// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
							// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
							if (!isResetMinMax[3] && histoRandomSamplePoints[27] >= 3000  && histoRandomSamplePoints[20] != 0 && histoRandomSamplePoints[21] != 0) {
								for (int j=20; j<37; ++j) {
									tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
								}
								isResetMinMax[3] = true;
							}
							// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
							// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
							// 57=LowestCellNumber, 58=Pressure, 59=Event G
							if (!isResetMinMax[2] && histoRandomSamplePoints[38] != 0) {
								for (int j=38; j<59; ++j) {
									tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
								}
								isResetMinMax[2] = true;
							}
							// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
							// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
							// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
							// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M
							if (!isResetMinMax[1] && histoRandomSamplePoints[60] != 0) {
								for (int j=60; j<99; ++j) {
									tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
								}
								isResetMinMax[1] = true;
							}
							if (isChannelsChannelEnabled) {
								// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
								// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
								// 120=Speed 121=Speed_max 122=PWM 123=Throttle 124=VoltageBEC 125=VoltageBEC_max 125=CurrentBEC 127=TemperatureBEC 128=TemperatureCap 
								// 129=Timing(empty) 130=Temperature_aux 131=Gear 132=YGEGenExt 133=MotStatEscNr 134=misc ESC_15 135=VersionESC
								if (!isResetMinMax[0] && histoRandomSamplePoints[107] != 0) {
									for (int j=107; j<135; ++j) {
										tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
									}
									isResetMinMax[0] = true;
								}
							} else {
								// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
								// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M
								// 100=Speed 101=Speed_max 102=PWM 103=Throttle 104=VoltageBEC 105=VoltageBEC_max 106=CurrentBEC 107=TemperatureBEC 108=TemperatureCap 
								// 109=Timing(empty) 110=Temperature_aux 111=Gear 112=YGEGenExt 113=MotStatEscNr 114=misc ESC_15 115=VersionESC
								if (!isResetMinMax[0] && histoRandomSamplePoints[87] != 0) {
									for (int j=87; j<115; ++j) {
										tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
									}
									isResetMinMax[0] = true;
								}
							}

							tmpRecordSet.addPoints(histoRandomSamplePoints, histoRandomSample.getSampleTimeStep_ms());
						} catch (DataInconsitsentException e) {
							throw ThrowableUtils.rethrow(e);
						}
						addTimer.invoke();
						pickTimer.invoke();
					}
				} : () -> histoRandomSample.capturePoints(timeSteps_ms[LogParser.TIMESTEP_INDEX]);

		// read all the data blocks from the file, parse only for the active channel
		boolean doFullRead = initializeBlocks <= 0;
		//boolean doDataSkip = detectedSensors.size() == 1 && !isChannelsEnabled;
		int datablocksLimit = doFullRead ? (int) this.logEntryCount : initializeBlocks;
		int i = 0;
		for (; i < datablocksLimit; i++) { //skip log entries before transmitter active
			if (buf.length != data_in.read(buf))
				log.log(Level.WARNING, "reading buf failed 1");
			if (isASCII) { //convert ASCII log data to hex
				HoTTlogReader.convertAscii2Raw(this.rawDataBlockSize, buf);
			}
			//log.logp(Level.OFF, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(buf, rawDataBlockSize));
			if (buf[8] == 0 || buf[9] == 0 || buf[24] == 0x1F) { // tx, rx, rx sensitivity data
				continue;
			}
			break;
		}
		
		for (; i < datablocksLimit; i++) {
			if (buf.length != data_in.read(buf))
				log.log(Level.WARNING, "reading buf failed 2");
			if (log.isLoggable(Level.FINE)) {
				if (isASCII)
					log.log(Level.FINE, new String(buf));
				else 
					log.log(Level.FINE, StringHelper.byte2Hex4CharString(buf, buf.length));
			}

			if (isASCII) { //convert ASCII log data to hex
				HoTTlogReader.convertAscii2Raw(rawDataBlockSize, buf);
			}
			
			//Ph(D)[4], Evt1(H)[5], Evt2(D)[6], Fch(D)[7], TXdBm(-D)[8], RXdBm(-D)[9], RfRcvRatio(D)[10], TrnRcvRatio(D)[11]
			//STATUS : Ph(D)[4], Evt1(H)[5], Evt2(D)[6], Fch(D)[7], TXdBm(-D)[8], RXdBm(-D)[9], RfRcvRatio(D)[10], TrnRcvRatio(D)[11]
			//S.INFOR : DEV(D)[22], CH(D)[23], SID(H)[24], WARN(H)[25]
			if (buf[8] != 0 && buf[9] != 0) { //buf 8, 9, tx,rx, rx sensitivity data
				if (buf[24] != 0x1F) {//rx sensitivity data
					if (log.isLoggable(Level.INFO)) {
						log.log(Level.INFO, String.format("Sensor %02X", buf[26]));
					}
				}
				rcvLogParser.trackPackageLoss(true);

				//create and fill sensor specific data record sets
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							StringHelper.byte2Hex2CharString(new byte[] { buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(buf[7], false));
				}

				//fill receiver data
				if (buf[24] != 0x1F) { //receiver sensitive data
					isReceiverData = rcvLogParser.parse();
					System.arraycopy(valuesRec, 0, points, 0, 10); //copy receiver points
				}

				if (isChannelsEnabled) {
					chnLogParser.parse();
					//in 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16 19=PowerOff 20=BattLow 21=Reset 22=Warning
					//out 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
					System.arraycopy(valuesChn, 3, points, 87, 20); //copy channel data and events, warning
				}
				
				switch ((byte) (buf[26] & 0xFF)) { //actual sensor
				case HoTTAdapter.ANSWER_SENSOR_VARIO_19200:
					isVarioData = varLogParser.parse();
					if (isVarioData && isReceiverData) {
						migrationJobs.add(varLogParser);
						migrator.invoke();
						isJustMigrated = true;
						isReceiverData = false;
					}
					break;
					
				case HoTTAdapter.ANSWER_SENSOR_GPS_19200:
					isGPSData = gpsLogParser.parse();
					if (isGPSData && isReceiverData) {
						migrationJobs.add(gpsLogParser);
						migrator.invoke();
						isJustMigrated = true;
						isReceiverData = false;
					}
					break;
				case HoTTAdapter.ANSWER_SENSOR_GENERAL_19200:
					isGeneralData = gamLogParser.parse();
					if (isGeneralData && isReceiverData) {
						migrationJobs.add(gamLogParser);
						migrator.invoke();
						isJustMigrated = true;
						isReceiverData = false;
					}
					break;
					
				case HoTTAdapter.ANSWER_SENSOR_ELECTRIC_19200:
					isElectricData = eamLogParser.parse();
					if (isElectricData && isReceiverData) {
						migrationJobs.add(eamLogParser);
						migrator.invoke();
						isJustMigrated = true;
						isReceiverData = false;
					}
					break;
					
				case HoTTAdapter.ANSWER_SENSOR_MOTOR_DRIVER_19200:
					isMotorDriverData = escLogParser.parse(tmpRecordSet, escLogParser.getTimeStep_ms());
					if (isMotorDriverData && isReceiverData) {
						migrationJobs.add(escLogParser);
						migrator.invoke();
						isJustMigrated = true;
						isReceiverData = false;
					}
					break;
					
				case 0x1F: //receiver sensitive data
				default:
					break;
				}

				if (isReceiverData) { //this will only be true if no other sensor is connected
					pointsAdder.invoke();
					isReceiverData = false;
				}
				else if (isChannelsEnabled && !isJustMigrated) { //this will only be true if no other sensor is connected and channel 4
					pointsAdder.invoke();
				}
				/*
				if (doDataSkip) {
					for (int j = 0; j < 9; j++) {
						data_in.read(buf);
						timeSteps_ms[LogParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;
					}
				}
				*/
				timeSteps_ms[LogParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;
				
				if (isJustMigrated) {
					pointsAdder.invoke();
				}
				isJustMigrated = !rcvLogParser.updateLossStatistics();
				isJustMigrated = false;
			}
			else { //skip empty block, but add time step
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found tx=rx=0 dBm");
					
					rcvLogParser.trackPackageLoss(false);

				if (isChannelsEnabled) {
					chnLogParser.parse();
					//in 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16 19=PowerOff 20=BattLow 21=Reset 22=Warning
					//out 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
					System.arraycopy(valuesChn, 3, points, 87, 20); //copy channel data and events, warning
					pointsAdder.invoke();
				}
				
				timeSteps_ms[LogParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;
			}
		}
		if (doFullRead) {
			rcvLogParser.finalUpdateLossStatistics();
			PackageLoss lostPackages  = rcvLogParser.getLostPackages();
			Integer[] scores = getScores(lostPackages, histoRandomSample,  truss.getVault());
			device.calculateInactiveRecords(tmpRecordSet);
			device.updateVisibilityStatus(tmpRecordSet, true);
			truss.promoteTruss(tmpRecordSet, scores);
			finishTimer.invoke();
			writeFinalLog(isTextModusSignaled, lostPackages, histoRandomSample, truss.getVault());
			// reduce memory consumption in advance to the garbage collection
			tmpRecordSet.cleanup();
		}
		log.log(Level.FINER, " > ends <  doFullRead=", doFullRead); //$NON-NLS-1$
	}
}
