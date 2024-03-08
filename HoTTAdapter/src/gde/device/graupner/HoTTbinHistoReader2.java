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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.graupner.HoTTAdapter.PickerParameters;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.device.graupner.HoTTbinReader.BinParser;
import gde.device.graupner.HoTTbinReader.BufCopier;
import gde.device.graupner.HoTTbinReader.InfoParser;
import gde.device.graupner.HoTTbinReader2.RcvBinParser;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.ThrowableUtils;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.VaultCollector;
import gde.histo.device.UniversalSampler;
import gde.io.DataParser;
import gde.log.Level;
import gde.utils.StringHelper;

/**
 * Read Graupner HoTT binary data for history analysis.
 * Collect data in a recordset and fill the vault collector.
 * Read measurements from multiple sensors for one single channel.
 * For small files (around 1 minute) no measurements are added to the recordset.
 * Support sampling to maximize the throughput.
 */
public class HoTTbinHistoReader2 extends HoTTbinHistoReader {
	private static final String	$CLASS_NAME									= HoTTbinHistoReader2.class.getName();
	private static final Logger	log													= Logger.getLogger(HoTTbinHistoReader2.$CLASS_NAME);

	/**
	 * the high number of measurement records increases the probability for excessive max/min values
	 */
	private static final int		INITIALIZE_SAMPLING_FACTOR	= 3;

	public HoTTbinHistoReader2(PickerParameters pickerParameters) {
		super(pickerParameters, pickerParameters.analyzer.getActiveChannel().getNumber() == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER, INITIALIZE_SAMPLING_FACTOR);
	}

	/**
	 * @param inputStream for retrieving the file info and for loading the log data
	 * @param newTruss which is promoted to a full vault object if the file has a minimum length.
	 */
	@Override
	public void read(Supplier<InputStream> inputStream, VaultCollector newTruss) throws IOException, DataTypeException, DataInconsitsentException {
		if (newTruss.getVault().getLogFileLength() <= HoTTbinReader.NUMBER_LOG_RECORDS_MIN * HoTTbinHistoReader.DATA_BLOCK_SIZE) return;

		nanoTime = System.nanoTime();
		initiateTime = readTime = reviewTime = addTime = pickTime = finishTime = 0;
		lastTime = System.nanoTime();

		truss = newTruss;
		IDevice device = analyzer.getActiveDevice();
		ExtendedVault vault = truss.getVault();
		long numberDatablocks = vault.getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE;
		tmpRecordSet = RecordSet.createRecordSet(vault.getLogRecordsetBaseName(), analyzer, analyzer.getActiveChannel().getNumber(), true, true, false);
		tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(vault.getLoadFileAsPath().getFileName().toString(), vault.getLogFileLastModified(), numberDatablocks));
		tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", tmpRecordSet.getStartTimeStamp())); //$NON-NLS-1$
		tmpRecordSet.descriptionAppendFilename(vault.getLoadFileAsPath().getFileName().toString());
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, " recordSetBaseName=" + vault.getLogRecordsetBaseName());

		HashMap<String, String> header = null;
		try (BufferedInputStream info_in = new BufferedInputStream(inputStream.get())) {
			header = new InfoParser((s) -> {}).getFileInfo(info_in, vault.getLoadFilePath(), vault.getLogFileLength());
			if (header == null || header.isEmpty()) return;

			detectedSensors = Sensor.getSetFromDetected(header.get(HoTTAdapter.DETECTED_SENSOR));
			
			//set picker parameter setting sensor for altitude/climb usage (0=auto, 1=VARIO, 2=GPS, 3=GAM, 4=EAM)
			HoTTbinReader.setAltitudeClimbPickeParameter(pickerParameters, detectedSensors);

			read(inputStream, Boolean.parseBoolean(header.get(HoTTAdapter.SD_FORMAT)));
		} catch (DataTypeException e) {
			log.log(Level.WARNING, String.format("%s  %s", e.getMessage(), vault.getLoadFilePath()));
		} catch (InvalidObjectException e) {
			// so any anther exception is propagated to the caller
		}
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
	protected void readSingle(InputStream data_in, int initializeBlocks, UniversalSampler histoRandomSample) throws DataInconsitsentException, IOException {
		HoTTAdapter2 device = (HoTTAdapter2) analyzer.getActiveDevice();
		boolean isReceiverData = false;
		boolean isSensorData = false;
		boolean[] isResetMinMax = new boolean[] {false, false, false, false, false}; //ESC, EAM, GAM, GPS, Vario
		int[]	points = histoRandomSample.getPoints();
		byte[] buf = new byte[HoTTbinHistoReader.DATA_BLOCK_SIZE];
		byte[] buf0 = new byte[30];
		byte[] buf1 = new byte[30];
		byte[] buf2 = new byte[30];
		byte[] buf3 = new byte[30];
		byte[] buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		long[] timeSteps_ms = new long[] { 0 };
		boolean isTextModusSignaled = false;

		BinParser rcvBinParser = null, chnBinParser = null, varBinParser = null, gpsBinParser = null, gamBinParser = null, eamBinParser = null, escBinParser = null;
		rcvBinParser = Sensor.RECEIVER.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		if (isChannelsChannelEnabled) {
			chnBinParser = Sensor.CHANNEL.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		}
		if (detectedSensors.contains(Sensor.VARIO)) {
			varBinParser = Sensor.VARIO.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		}
		if (detectedSensors.contains(Sensor.GPS)) {
			gpsBinParser = Sensor.GPS.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		}
		if (detectedSensors.contains(Sensor.GAM)) {
			gamBinParser = Sensor.GAM.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		}
		if (detectedSensors.contains(Sensor.EAM)) {
			eamBinParser = Sensor.EAM.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		}
		if (detectedSensors.contains(Sensor.ESC)) {
			escBinParser = Sensor.ESC.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		}

		Procedure pointsAdder = initializeBlocks <= 0 //
				? () -> {
					readTimer.invoke();
					boolean isValidSample = histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);
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
				} : () -> histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);

		// read all the data blocks from the file, parse only for the active channel
		boolean doFullRead = initializeBlocks <= 0;
		boolean doDataSkip = detectedSensors.size() == 1 && !isChannelsChannelEnabled;
		int datablocksLimit = (doFullRead ? (int) truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE : initializeBlocks) / (doDataSkip ? 10 : 1);
		for (int i = 0; i < datablocksLimit; i++) {
			data_in.read(buf);
			if (log.isLoggable(Level.FINE) && i % 10 == 0) {
				log.log(Level.FINE, StringHelper.fourDigitsRunningNumber(buf.length));
				log.log(Level.FINE, StringHelper.byte2Hex4CharString(buf, buf.length));
			}

			if (!isFilterTextModus || (buf[6] & 0x01) == 0) { //switch into text modus
				if (buf[33] >= 0 && buf[33] <= 4 && buf[3] != 0 && buf[4] != 0) { //buf 3, 4, tx,rx
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", buf[7], buf[33]));

					((RcvBinParser) rcvBinParser).trackPackageLoss(true);
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, StringHelper.byte2Hex2CharString(new byte[] { buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(buf[7], false));

					//fill receiver data
					if (buf[33] == 0 && (buf[38] & 0x80) != 128 && DataParser.parse2Short(buf, 40) >= 0) {
						rcvBinParser.parse();
						isReceiverData = true;
					}
					if (chnBinParser != null) {
						chnBinParser.parse();
					}

					//fill data block 0 receiver voltage an temperature
					if (buf[33] == 0) {
						bufCopier.copyToBuffer();
					}
					if (detectedSensors.size() == 1 && chnBinParser == null) { //reduce data rate for receiver to 0.1 sec
						for (int j = 0; j < 9; j++) {
							data_in.read(buf);
							timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
						}
						isSensorData = true;
					}

					//create and fill sensor specific data record sets
					switch ((byte) (buf[7] & 0xFF)) {
					case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
					case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
						if (varBinParser != null) {
							bufCopier.copyToVarioBuffer();
							if (bufCopier.is4BuffersFull()) {
								varBinParser.parse();
								bufCopier.clearBuffers();
								isSensorData = true;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GPS_115200:
					case HoTTAdapter.SENSOR_TYPE_GPS_19200:
						if (gpsBinParser != null) {
							bufCopier.copyToFreeBuffer();
							if (bufCopier.is4BuffersFull()) {
								gpsBinParser.parse();
								bufCopier.clearBuffers();
								isSensorData = true;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
					case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
						if (gamBinParser != null) {
							bufCopier.copyToFreeBuffer();
							if (bufCopier.is4BuffersFull()) {
								gamBinParser.parse();
								bufCopier.clearBuffers();
								isSensorData = true;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
						if (eamBinParser != null) {
							bufCopier.copyToFreeBuffer();
							if (bufCopier.is4BuffersFull()) {
								eamBinParser.parse();
								bufCopier.clearBuffers();
								isSensorData = true;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
						if (escBinParser != null) {
							bufCopier.copyToFreeBuffer();
							if (bufCopier.is4BuffersFull()) {
								escBinParser.parse();
								bufCopier.clearBuffers();
								isSensorData = true;
							}
						}
						break;
					}

					if (isSensorData) {
						((RcvBinParser) rcvBinParser).updateLossStatistics();
					}

					if (isSensorData || (isReceiverData && this.tmpRecordSet.get(0).realSize() > 0)) {
						pointsAdder.invoke();
						isSensorData = isReceiverData = false;
					} else if (chnBinParser != null) {
						pointsAdder.invoke();
					}

					timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10; // add default time step from device of 10 msec
				} else { //skip empty block, but add time step
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

					((RcvBinParser) rcvBinParser).trackPackageLoss(false);

					if (chnBinParser != null) {
						chnBinParser.parse();
						pointsAdder.invoke();
					}
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
					//reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
					//buf1 = buf2 = buf3 = buf4 = null;
				}
			} else if (!isTextModusSignaled) {
				isTextModusSignaled = true;
			}
		}
		if (doFullRead) {
			((RcvBinParser) rcvBinParser).finalUpdateLossStatistics();
			PackageLoss lostPackages  = ((RcvBinParser) rcvBinParser).getLostPackages();
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

	/**
	 * read log data according to version 0 either in initialize mode for learning min/max values or in fully functional read mode.
	 * reads only sample records and allocates only one single recordset, so HoTTAdapter.isChannelsChannelEnabled does not take effect.
	 * no progress bar support and no channel data modifications.
	 * @param data_in
	 * @param initializeBlocks if this number is greater than zero, the min/max values are initialized
	 * @param histoRandomSample is the random sampler which might use the minMax values from a previous run and thus reduces oversampling
	 */
	@Override
	protected void readMultiple(InputStream data_in, int initializeBlocks, UniversalSampler histoRandomSample) throws IOException, DataInconsitsentException {
		HoTTAdapter2 device = (HoTTAdapter2) analyzer.getActiveDevice();
		boolean isReceiverData = false;
		boolean isJustMigrated = false;
		boolean[] isResetMinMax = new boolean[] {false, false, false, false, false}; //ESC, EAM, GAM, GPS, Vario
		int[]	points = histoRandomSample.getPoints();
		byte[] buf = new byte[HoTTbinHistoReader.DATA_BLOCK_SIZE];
		byte[] buf0 = new byte[30];
		byte[] buf1 = new byte[30];
		byte[] buf2 = new byte[30];
		byte[] buf3 = new byte[30];
		byte[] buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGAM = 0, logCountEAM = 0, logCountESC = 0;
		long[] timeSteps_ms = new long[] { 0 };
		boolean isTextModusSignaled = false;

		BinParser rcvBinParser, chnBinParser, varBinParser, gpsBinParser, gamBinParser, eamBinParser, escBinParser;
		// parse in situ for receiver and channel
		rcvBinParser = Sensor.RECEIVER.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
    if (isChannelsChannelEnabled) {
    	chnBinParser = Sensor.CHANNEL.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		} else chnBinParser = null;
		// use parser points objects
		if (detectedSensors.contains(Sensor.VARIO)) {
			varBinParser = Sensor.VARIO.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else varBinParser = null;
		if (detectedSensors.contains(Sensor.GPS)) {
			gpsBinParser = Sensor.GPS.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else gpsBinParser = null;
		if (detectedSensors.contains(Sensor.GAM)) {
			gamBinParser = Sensor.GAM.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else gamBinParser = null;
		if (detectedSensors.contains(Sensor.EAM)) {
			eamBinParser = Sensor.EAM.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else eamBinParser = null;
		if (detectedSensors.contains(Sensor.ESC)) {
			escBinParser = Sensor.ESC.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else escBinParser = null;

		Set<BinParser> migrationJobs = new HashSet<>();
		@SuppressWarnings("null")
		Procedure migrator = () -> {
			// the sequence of the next statements is crucial, eg. for vario data
			if (migrationJobs.contains(eamBinParser)) eamBinParser.migratePoints(points);
			if (migrationJobs.contains(gamBinParser)) gamBinParser.migratePoints(points);
			if (migrationJobs.contains(gpsBinParser)) gpsBinParser.migratePoints(points);
			if (migrationJobs.contains(varBinParser)) varBinParser.migratePoints(points);
			if (migrationJobs.contains(escBinParser)) escBinParser.migratePoints(points);
			migrationJobs.clear();
		};

		Procedure pointsAdder = initializeBlocks <= 0 //
				? () -> {
					readTimer.invoke();
					boolean isValidSample = histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);
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
				} : () -> histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);
		initTimer.invoke();

		// read all the data blocks from the file, parse only for the active channel
		boolean doFullRead = initializeBlocks <= 0;
		int initializeBlockLimit = initializeBlocks > 0 ? initializeBlocks : Integer.MAX_VALUE;
		for (int i = 0; i < initializeBlockLimit && i < truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE; i++) {
			data_in.read(buf);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, StringHelper.byte2Hex4CharString(buf, buf.length));

			if (!isFilterTextModus || (buf[6] & 0x01) == 0) { //switch into text modus
				if (buf[33] >= 0 && buf[33] <= 4 && buf[3] != 0 && buf[4] != 0) { //buf 3, 4, tx,rx
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", buf[7], buf[33]));

					((RcvBinParser) rcvBinParser).trackPackageLoss(true);
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, StringHelper.byte2Hex2CharString(new byte[] { buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(buf[7], false));

					//fill receiver data
					if (buf[33] == 0 && (buf[38] & 0x80) != 128 && DataParser.parse2Short(buf, 40) >= 0) {
						rcvBinParser.parse();
						isReceiverData = true;
					}
					if (chnBinParser != null) {
						chnBinParser.parse();
					}

					if (actualSensor == -1)
						lastSensor = actualSensor = (byte) (buf[7] & 0xFF);
					else
						actualSensor = (byte) (buf[7] & 0xFF);

					if (actualSensor != lastSensor) {
						if (logCountVario >= 5 || logCountGPS >= 5 || logCountGAM >= 5 || logCountEAM >= 5 || logCountESC >= 5) {
							//							System.out.println();
							switch (lastSensor) {
							case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
							case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
								if (varBinParser != null) {
									if (migrationJobs.contains(varBinParser) && isReceiverData) {
										migrator.invoke();
										isJustMigrated = true;
										isReceiverData = false;
										pointsAdder.invoke();
									}
									varBinParser.parse();
									migrationJobs.add(varBinParser);
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GPS_115200:
							case HoTTAdapter.SENSOR_TYPE_GPS_19200:
								if (gpsBinParser != null) {
									if (migrationJobs.contains(gpsBinParser) && isReceiverData) {
										migrator.invoke();
										isJustMigrated = true;
										isReceiverData = false;
										pointsAdder.invoke();
									}
									gpsBinParser.parse();
									migrationJobs.add(gpsBinParser);
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
							case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
								if (gamBinParser != null) {
									if (migrationJobs.contains(gamBinParser) && isReceiverData) {
										migrator.invoke();
										isJustMigrated = true;
										isReceiverData = false;
										pointsAdder.invoke();
									}
									gamBinParser.parse();
									migrationJobs.add(gamBinParser);
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
								if (eamBinParser != null)  {
									if (migrationJobs.contains(eamBinParser) && isReceiverData) {
										migrator.invoke();
										isJustMigrated = true;
										isReceiverData = false;
										pointsAdder.invoke();
									}
									eamBinParser.parse();
									migrationJobs.add(eamBinParser);
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
								if (escBinParser != null)  {
									if (migrationJobs.contains(escBinParser) && isReceiverData) {
										migrator.invoke();
										isJustMigrated = true;
										isReceiverData = false;
										pointsAdder.invoke();
									}
									escBinParser.parse();
									migrationJobs.add(escBinParser);
								}
								break;
							}

							if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isReceiverData " + isReceiverData + " migrationJobs " + migrationJobs);
						}

						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "logCountVario = " + logCountVario + " logCountGPS = " + logCountGPS + " logCountGeneral = " + logCountGAM + " logCountElectric = " + logCountEAM);
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
						((RcvBinParser) rcvBinParser).updateLossStatistics();
					}

					if (isReceiverData && (logCountVario > 0 || logCountGPS > 0 || logCountGAM > 0 || logCountEAM > 0 || logCountESC > 0)) {
						pointsAdder.invoke();
						isReceiverData = false;
					} else if (chnBinParser != null && !isJustMigrated) {
						pointsAdder.invoke();
					}
					isJustMigrated = false;

					bufCopier.copyToBuffer();
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;// add default time step from log record of 10 msec
				} else { //skip empty block, but add time step
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

					((RcvBinParser) rcvBinParser).trackPackageLoss(false);
					if (chnBinParser != null) {
						chnBinParser.parse();
						pointsAdder.invoke();
					}
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
				}
			} else if (!isTextModusSignaled) {
				isTextModusSignaled = true;
			}
		}

		if (doFullRead) {
			((RcvBinParser) rcvBinParser).finalUpdateLossStatistics();
			PackageLoss lostPackages  = ((RcvBinParser) rcvBinParser).getLostPackages();
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
