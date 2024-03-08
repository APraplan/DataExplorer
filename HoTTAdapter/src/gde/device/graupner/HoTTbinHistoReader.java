
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
    					2016,2017,2018,2019 Thomas Eickert
 ****************************************************************************************/
package gde.device.graupner;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

import com.sun.istack.Nullable;

import gde.Analyzer;
import gde.GDE;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.ScoreLabelTypes;
import gde.device.graupner.HoTTAdapter.PickerParameters;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.device.graupner.HoTTbinReader.BinParser;
import gde.device.graupner.HoTTbinReader.BufCopier;
import gde.device.graupner.HoTTbinReader.ChnBinParser;
import gde.device.graupner.HoTTbinReader.EamBinParser;
import gde.device.graupner.HoTTbinReader.EscBinParser;
import gde.device.graupner.HoTTbinReader.GamBinParser;
import gde.device.graupner.HoTTbinReader.GpsBinParser;
import gde.device.graupner.HoTTbinReader.InfoParser;
import gde.device.graupner.HoTTbinReader.RcvBinParser;
import gde.device.graupner.HoTTbinReader.SdLogFormat;
import gde.device.graupner.HoTTbinReader.SdLogInputStream;
import gde.device.graupner.HoTTbinReader.VarBinParser;
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
 * Read measurements for one single channel which holds a maximum of one sensor.
 * For small files (around 1 minute) no measurements are added to the recordset.
 * Support sampling to maximize the throughput.
 * @author Thomas Eickert
 */
public class HoTTbinHistoReader {
	private static final String	$CLASS_NAME					= HoTTbinHistoReader.class.getName();
	private static final Logger	log									= Logger.getLogger(HoTTbinHistoReader.$CLASS_NAME);

	/**
	 * HoTT logs data rate defined by the channel log
	 */
	protected final static int	RECORD_TIMESPAN_MS	= 10;
	protected final static int	DATA_BLOCK_SIZE			= 64;

	@FunctionalInterface
	interface Procedure {
		void invoke();
	}

	protected final PickerParameters	pickerParameters;
	protected final Analyzer					analyzer;
	protected final boolean						isChannelsChannelEnabled;
	protected final int								initializeSamplingFactor;
	protected final boolean						isFilterEnabled;
	protected final boolean						isFilterTextModus;
	protected final int 							altitudeClimbSensorSelection;
	protected final Procedure					initTimer, readTimer, reviewTimer, addTimer, pickTimer, finishTimer;

	protected long										nanoTime;
	protected long										currentTime, initiateTime, readTime, reviewTime, addTime, pickTime, finishTime, lastTime;
	/**
	 * The detected sensors including the receiver but without 'channel'
	 */
	protected EnumSet<Sensor>					detectedSensors;
	protected RecordSet								tmpRecordSet;
	protected VaultCollector					truss;

	public HoTTbinHistoReader(PickerParameters pickerParameters) {
		this(pickerParameters, pickerParameters.analyzer.getActiveChannel().getNumber() == Sensor.CHANNEL.getChannelNumber(), 1);
	}

	/**
	 * @param pickerParameters
	 * @param isChannelsChannelEnabled true activates the channel measurements
	 * @param initializeSamplingFactor increases the number of blocks for max/min evaluation and reduces the oversampling
	 */
	protected HoTTbinHistoReader(PickerParameters pickerParameters, boolean isChannelsChannelEnabled, int initializeSamplingFactor) {
		this.pickerParameters = new PickerParameters(pickerParameters);
		this.pickerParameters.isFilterEnabled = true;

		this.analyzer = pickerParameters.analyzer;
		this.isChannelsChannelEnabled = isChannelsChannelEnabled;
		this.initializeSamplingFactor = initializeSamplingFactor;
		this.isFilterEnabled = true;
		this.isFilterTextModus = true;
		this.altitudeClimbSensorSelection = 0; //auto
		this.detectedSensors = EnumSet.noneOf(Sensor.class);
		if (log.isLoggable(Level.TIME)) {
			initTimer = () -> {
				currentTime = System.nanoTime();
				initiateTime += currentTime - lastTime;
				lastTime = currentTime;
			};
			readTimer = () -> {
				currentTime = System.nanoTime();
				readTime += currentTime - lastTime;
				lastTime = currentTime;
			};
			reviewTimer = () -> {
				currentTime = System.nanoTime();
				reviewTime += currentTime - lastTime;
				lastTime = currentTime;
			};
			addTimer = () -> {
				currentTime = System.nanoTime();
				addTime += currentTime - lastTime;
				lastTime = currentTime;
			};
			pickTimer = () -> {
				currentTime = System.nanoTime();
				pickTime += currentTime - lastTime;
				lastTime = currentTime;
			};
			finishTimer = () -> {
				currentTime = System.nanoTime();
				finishTime += currentTime - lastTime;
				lastTime = currentTime;
			};
		} else {
			initTimer = readTimer = reviewTimer = addTimer = pickTimer = finishTimer = () -> {
			};
		}
	}

	/**
	 * @param inputStream for retrieving the file info and for loading the log data
	 * @param newTruss which is promoted to a full vault object if the file has a minimum length.
	 */
	public void read(Supplier<InputStream> inputStream, VaultCollector newTruss) throws IOException, DataTypeException, DataInconsitsentException {
		if (newTruss.getVault().getLogFileLength() <= HoTTbinReader.NUMBER_LOG_RECORDS_MIN * HoTTbinHistoReader.DATA_BLOCK_SIZE ) return;

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
			header = new InfoParser((s) -> {} ).getFileInfo(info_in, vault.getLoadFilePath(), vault.getLogFileLength());
			if (header == null || header.isEmpty()) return;

			detectedSensors = Sensor.getSetFromDetected(header.get(HoTTAdapter.DETECTED_SENSOR));
			if (!Sensor.getChannelNumbers(detectedSensors).contains(truss.getVault().getVaultChannelNumber())) return;

			read(inputStream, Boolean.parseBoolean(header.get(HoTTAdapter.SD_FORMAT)));
		} catch (DataTypeException e) {
			log.log(Level.WARNING, String.format("%s  %s", e.getMessage(), vault.getLoadFilePath()));
		} catch (InvalidObjectException e) {
			// so any anther exception is propagated to the caller
		}
	}

	/**
	 * read file.
	 * provide data in a tmpRecordSet.
	 */
	protected void read(Supplier<InputStream> inputStream, boolean isSdFormat) throws IOException, DataTypeException, DataInconsitsentException {
		try (BufferedInputStream in = new BufferedInputStream(inputStream.get()); //
				InputStream data_in = isSdFormat //
						? new SdLogInputStream(in, truss.getVault().getLogFileLength(), new SdLogFormat(HoTTbinReaderX.headerSize, HoTTbinReaderX.footerSize, 64)) //
						: in; ) {
			int initializeBlockLimit = initializeSamplingFactor * (HoTTbinReader.LOG_RECORD_SCAN_START + HoTTbinReader.NUMBER_LOG_RECORDS_TO_SCAN);
			int readLimitMark = initializeBlockLimit * HoTTbinHistoReader.DATA_BLOCK_SIZE + 1;
			if (detectedSensors.size() <= 2) {
				data_in.mark(readLimitMark); // reduces # of overscan records from 57 to 23 (to 38 with 1500 blocks)
				int activeChannelNumber = analyzer.getActiveChannel().getNumber();
				int[]	points = new int[analyzer.getActiveDevice().getNumberOfMeasurements(activeChannelNumber)];
				UniversalSampler initSampler = UniversalSampler.createSampler(activeChannelNumber, points, HoTTbinHistoReader.RECORD_TIMESPAN_MS, analyzer);
				readSingle(data_in, initializeBlockLimit, initSampler);
				data_in.reset();
				UniversalSampler sampler = UniversalSampler.createSampler(activeChannelNumber, initSampler.getMaxPoints(), initSampler.getMinPoints(), HoTTbinHistoReader.RECORD_TIMESPAN_MS, analyzer);
				readSingle(data_in, -1, sampler);
			} else {
				data_in.mark(readLimitMark);
				int activeChannelNumber = analyzer.getActiveChannel().getNumber();
				int[]	points = new int[analyzer.getActiveDevice().getNumberOfMeasurements(activeChannelNumber)];
				UniversalSampler initSampler = UniversalSampler.createSampler(activeChannelNumber, points, HoTTbinHistoReader.RECORD_TIMESPAN_MS, analyzer);
				readMultiple(data_in, initializeBlockLimit, initSampler);
				data_in.reset();
				UniversalSampler sampler = UniversalSampler.createSampler(activeChannelNumber, initSampler.getMaxPoints(), initSampler.getMinPoints(), HoTTbinHistoReader.RECORD_TIMESPAN_MS, analyzer);
				readMultiple(data_in, -1, sampler);
			}
		}
	}

	/**
	 * read log data according to version 0.
	 * allocates only one single recordset for the active channel, so HoTTAdapter.isChannelsChannelEnabled does not take any effect.
	 * no progress bar support and no channel data modifications.
	 */
	protected void readSingle(InputStream data_in, int initializeBlocks, UniversalSampler histoRandomSample) throws DataInconsitsentException, IOException {
		int[]	points = histoRandomSample.getPoints();
		byte[] buf = new byte[HoTTbinHistoReader.DATA_BLOCK_SIZE];
		byte[] buf0 = new byte[30];
		byte[] buf1 = new byte[30];
		byte[] buf2 = new byte[30];
		byte[] buf3 = new byte[30];
		byte[] buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		long[] timeSteps_ms = new long[] { 0 };
		boolean isSensorData = false;
		boolean	isTextModusSignaled	= false;
		BinParser binParser;
		int activeChannelNumber = analyzer.getActiveChannel().getNumber();
		if (activeChannelNumber == Sensor.RECEIVER.getChannelNumber()) {
			binParser = Sensor.RECEIVER.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		} else if (activeChannelNumber == Sensor.CHANNEL.getChannelNumber()) {
			binParser = Sensor.CHANNEL.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		} else if (activeChannelNumber == Sensor.VARIO.getChannelNumber()) {
			binParser = Sensor.VARIO.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else if (activeChannelNumber == Sensor.GPS.getChannelNumber()) {
			binParser = Sensor.GPS.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else if (activeChannelNumber == Sensor.GAM.getChannelNumber()) {
			binParser = Sensor.GAM.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else if (activeChannelNumber == Sensor.EAM.getChannelNumber()) {
			binParser = Sensor.EAM.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else if (activeChannelNumber == Sensor.ESC.getChannelNumber()) {
			binParser = Sensor.ESC.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else {
			throw new UnsupportedOperationException();
		}

		Procedure pointsAdder = initializeBlocks <= 0 //
				? () -> {
					readTimer.invoke();
					boolean isValidSample = histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);
					reviewTimer.invoke();
					if (isValidSample) {
						try {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
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
		boolean doDataSkip = detectedSensors.size() == 1 && !isChannelsChannelEnabled;
		int datablocksLimit = (doFullRead ? (int) truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE : initializeBlocks) / (doDataSkip ? 10 : 1);
		for (int i = 0; i < datablocksLimit; i++) {
			// if (log.isLoggable(Level.TIME))
			// log.log(Level.TIME, String.format("markpos: %,9d i: %,9d ", data_in.markpos, i));
			data_in.read(buf);
			if (log.isLoggable(Level.FINE) && i % 10 == 0) {
				log.log(Level.FINE, StringHelper.fourDigitsRunningNumber(buf.length));
				log.log(Level.FINE, StringHelper.byte2Hex4CharString(buf, buf.length));
			}

			if (!this.isFilterTextModus || (buf[6] & 0x01) == 0) { // switch into text modus
				if (buf[33] >= 0 && buf[33] <= 4 && buf[3] != 0 && buf[4] != 0) { // buf 3, 4, tx,rx
					if (log.isLoggable(Level.FINE))
						log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", buf[7], buf[33])); //$NON-NLS-1$
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, StringHelper.byte2Hex2CharString(new byte[] { buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(buf[7], false));
					if (binParser instanceof RcvBinParser) {
						((RcvBinParser) binParser).trackPackageLoss(true);
						if (buf[33] == 0 && (buf[38] & 0x80) != 128 && DataParser.parse2Short(buf, 40) >= 0) {
							if (binParser.parse()) pointsAdder.invoke();
						}
					} else if (binParser instanceof ChnBinParser && binParser.parse()) {
						pointsAdder.invoke();
					}
					if (doDataSkip) {
						for (int j = 0; j < 9; j++) {
							data_in.read(buf);
							timeSteps_ms[BinParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;
						}
						isSensorData = true;
					}
					// fill data block 0 receiver voltage and temperature
					if (buf[33] == 0) {
						bufCopier.copyToBuffer();
					}

					timeSteps_ms[BinParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;

					// log.log(Level.OFF, "sensor type ID = " + StringHelper.byte2Hex2CharString(new byte[] {(byte) (HoTTbinHistoReader.buf[7] & 0xFF)}, 1));
					switch ((byte) (buf[7] & 0xFF)) {
					case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
					case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
						if (detectedSensors.contains(Sensor.VARIO)) {
							bufCopier.copyToVarioBuffer();
							if (bufCopier.is4BuffersFull()) {
								if (binParser instanceof VarBinParser && binParser.parse()) pointsAdder.invoke();
								isSensorData = true;
								bufCopier.clearBuffers();
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GPS_115200:
					case HoTTAdapter.SENSOR_TYPE_GPS_19200:
						if (detectedSensors.contains(Sensor.GPS)) {
							bufCopier.copyToFreeBuffer();
							if (bufCopier.is4BuffersFull()) {
								if (binParser instanceof GpsBinParser && binParser.parse()) pointsAdder.invoke();
								isSensorData = true;
								bufCopier.clearBuffers();
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
					case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
						if (detectedSensors.contains(Sensor.GAM)) {
							bufCopier.copyToFreeBuffer();
							if (bufCopier.is4BuffersFull()) {
								if (binParser instanceof GamBinParser && binParser.parse()) pointsAdder.invoke();
								isSensorData = true;
								bufCopier.clearBuffers();
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
						if (detectedSensors.contains(Sensor.EAM)) {
							bufCopier.copyToFreeBuffer();
							if (bufCopier.is4BuffersFull()) {
								if (binParser instanceof EamBinParser && binParser.parse()) pointsAdder.invoke();
								isSensorData = true;
								bufCopier.clearBuffers();
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
						if (detectedSensors.contains(Sensor.ESC)) {
							bufCopier.copyToFreeBuffer();
							if (bufCopier.is4BuffersFull()) {
								if (binParser instanceof EscBinParser && binParser.parse()) pointsAdder.invoke();
								isSensorData = true;
								bufCopier.clearBuffers();
							}
						}
						break;
					}

					if (isSensorData) {
						if (binParser instanceof RcvBinParser)
							isSensorData = !((RcvBinParser) binParser).updateLossStatistics();
						else
							isSensorData = false;
					}
				} else { // skip empty block, but add time step
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found tx=rx=0 dBm"); //$NON-NLS-1$
					if (binParser instanceof RcvBinParser) {
						((RcvBinParser) binParser).trackPackageLoss(false);
					}
					if (binParser instanceof ChnBinParser && binParser.parse()) {
						pointsAdder.invoke();
					}
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;
				}
			} else if (!isTextModusSignaled) {
				isTextModusSignaled = true;
			}
		}
		if (doFullRead) {
			PackageLoss lostPackages = null;
			if (binParser instanceof RcvBinParser) {
				((RcvBinParser) binParser).finalUpdateLossStatistics();
				lostPackages = ((RcvBinParser) binParser).getLostPackages();
			}
			Integer[] scores = getScores(lostPackages, histoRandomSample,  truss.getVault());
			HoTTAdapter device = (HoTTAdapter) analyzer.getActiveDevice();
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
	 * reads only sample records and allocates only one single recordset for the active channel, so HoTTAdapter.isChannelsChannelEnabled does not take effect.
	 * no progress bar support and no channel data modifications.
	 * @param initializeBlocks if this number is greater than zero, the min/max values are initialized
	 * @param histoRandomSample is the random sampler which might use the minMax values from a previous run and thus reduces oversampling
	 */
	protected void readMultiple(InputStream data_in, int initializeBlocks, UniversalSampler histoRandomSample) throws IOException, DataInconsitsentException {
		int[]	points = histoRandomSample.getPoints();
		byte[] buf = new byte[HoTTbinHistoReader.DATA_BLOCK_SIZE];
		byte[] buf0 = new byte[30];
		byte[] buf1 = new byte[30];
		byte[] buf2 = new byte[30];
		byte[] buf3 = new byte[30];
		byte[] buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		long[] timeSteps_ms = new long[] { 0 };
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGeneral = 0, logCountElectric = 0, logCountSpeedControl = 0;
		boolean isSensorData = false;
		boolean	isTextModusSignaled	= false;
		BinParser binParser;
		int activeChannelNumber = analyzer.getActiveChannel().getNumber();
		if (activeChannelNumber == Sensor.RECEIVER.getChannelNumber()) {
			binParser = Sensor.RECEIVER.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		} else if (activeChannelNumber == Sensor.CHANNEL.getChannelNumber()) {
			binParser = Sensor.CHANNEL.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		} else if (activeChannelNumber == Sensor.VARIO.getChannelNumber() && detectedSensors.contains(Sensor.VARIO)) {
			binParser = Sensor.VARIO.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else if (activeChannelNumber == Sensor.GPS.getChannelNumber() && detectedSensors.contains(Sensor.GPS)) {
			binParser = Sensor.GPS.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else if (activeChannelNumber == Sensor.GAM.getChannelNumber() && detectedSensors.contains(Sensor.GAM)) {
			binParser = Sensor.GAM.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else if (activeChannelNumber == Sensor.EAM.getChannelNumber() && detectedSensors.contains(Sensor.EAM)) {
			binParser = Sensor.EAM.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else if (activeChannelNumber == Sensor.ESC.getChannelNumber() && detectedSensors.contains(Sensor.ESC)) {
			binParser = Sensor.ESC.createBinParser(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else {
			throw new UnsupportedOperationException();
		}

		Procedure pointsAdder = initializeBlocks <= 0 //
				? () -> {
					readTimer.invoke();
					boolean isValidSample = histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);
					reviewTimer.invoke();
					if (isValidSample) {
						try {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
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
		int datablocksLimit = (doFullRead ? (int) truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE : initializeBlocks);
		for (int i = 0; i < datablocksLimit; i++) {
			data_in.read(buf);
			if (log.isLoggable(Level.FINEST) && i % 10 == 0) {
				log.log(Level.FINEST, StringHelper.fourDigitsRunningNumber(buf.length));
				log.log(Level.FINEST, StringHelper.byte2Hex4CharString(buf, buf.length));
			}

			if (!this.isFilterTextModus || (buf[6] & 0x01) == 0) { // switch into text modus
				if (buf[33] >= 0 && buf[33] <= 4 && buf[3] != 0 && buf[4] != 0) { // buf 3, 4, tx,rx
					if (log.isLoggable(Level.FINE))
						log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", buf[7], buf[33])); //$NON-NLS-1$
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, StringHelper.byte2Hex2CharString(new byte[] { buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(buf[7], false));

					if (binParser instanceof RcvBinParser) {
						((RcvBinParser) binParser).trackPackageLoss(true);
						if (buf[33] == 0 && (buf[38] & 0x80) != 128 && DataParser.parse2Short(buf, 40) >= 0) {
							if (binParser.parse()) pointsAdder.invoke();
						}
					} else if (binParser instanceof ChnBinParser && binParser.parse()) {
						pointsAdder.invoke();
					}

					timeSteps_ms[BinParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;

					// detect sensor switch
					if (actualSensor == -1)
						lastSensor = actualSensor = (byte) (buf[7] & 0xFF);
					else
						actualSensor = (byte) (buf[7] & 0xFF);

					if (actualSensor != lastSensor) {
						// write data just after sensor switch
						if (logCountVario >= 5 || logCountGPS >= 5 || logCountGeneral >= 5 || logCountElectric >= 5 || logCountSpeedControl >= 5) {
							switch (lastSensor) {
							case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
							case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
								if (detectedSensors.contains(Sensor.VARIO)) {
									if (binParser instanceof VarBinParser && binParser.parse()) pointsAdder.invoke();
									isSensorData = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GPS_115200:
							case HoTTAdapter.SENSOR_TYPE_GPS_19200:
								if (detectedSensors.contains(Sensor.GPS)) {
									if (binParser instanceof GpsBinParser && binParser.parse()) pointsAdder.invoke();
									isSensorData = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
							case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
								if (detectedSensors.contains(Sensor.GAM)) {
									if (binParser instanceof GamBinParser && binParser.parse()) pointsAdder.invoke();
									isSensorData = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
								if (detectedSensors.contains(Sensor.EAM)) {
									if (binParser instanceof EamBinParser && binParser.parse()) pointsAdder.invoke();
									isSensorData = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
								if (detectedSensors.contains(Sensor.ESC)) {
									if (binParser instanceof EscBinParser && binParser.parse()) pointsAdder.invoke();
									isSensorData = true;
								}
								break;
							}
						}

						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "logCountVario = " + logCountVario //$NON-NLS-1$
								+ " logCountGPS = " + logCountGPS + " logCountGeneral = " + logCountGeneral + " logCountElectric = " + logCountElectric + " logCountMotorDriver = " + logCountSpeedControl); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						lastSensor = actualSensor;
						logCountVario = logCountGPS = logCountGeneral = logCountElectric = logCountSpeedControl = 0;
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
						++logCountGeneral;
						break;
					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
						++logCountElectric;
						break;
					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
						++logCountSpeedControl;
						break;
					}

					bufCopier.copyToBuffer();
					if (isSensorData) {
						if (binParser instanceof RcvBinParser)
							isSensorData = !((RcvBinParser) binParser).updateLossStatistics();
						else
							isSensorData = false;
					}
				} else { // tx,rx == 0
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found tx=rx=0 dBm"); //$NON-NLS-1$
					if (binParser instanceof RcvBinParser) {
						((RcvBinParser) binParser).trackPackageLoss(false);
					}
					if (binParser instanceof ChnBinParser && binParser.parse()) {
						pointsAdder.invoke();
					}
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;
				}
			} else if (!isTextModusSignaled) {
				isTextModusSignaled = true;
			}
		}
		if (doFullRead) {
			if (binParser instanceof RcvBinParser)
				((RcvBinParser) binParser).finalUpdateLossStatistics();
			PackageLoss lostPackages  = binParser instanceof RcvBinParser ? ((RcvBinParser) binParser).getLostPackages() : null;
			Integer[] scores = getScores(lostPackages, histoRandomSample,  truss.getVault());
			HoTTAdapter device = (HoTTAdapter) analyzer.getActiveDevice();
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
	 * @param lostPackages might be null if the reader did not collect the lost packages statistics
	 * @return the scores array based on the score label type ordinal number
	 */
	protected Integer[] getScores(@Nullable PackageLoss lostPackages, UniversalSampler histoRandomSample, ExtendedVault vault) {
		int lossTotal = lostPackages == null ? 0 : lostPackages.lossTotal;
		final Integer[] scores = new Integer[ScoreLabelTypes.VALUES.length];
		// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
		// scores for duration and timestep values are filled in by the HistoVault
		scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = histoRandomSample.getReadingCount();
		scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = (int) vault.getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE;
		scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = lossTotal;
		if (lostPackages != null) {
			scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = (int) (lostPackages.percentage * 10000.);
			scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = (int) lostPackages.getAvgValue() * RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = lostPackages.getMaxValue() * RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = lostPackages.getMinValue() * RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = (int) lostPackages.getSigmaValue() * RECORD_TIMESPAN_MS * 1000;
		} else {
			scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = 0;
		}
		BitSet activeSensors = Sensor.getSensors(detectedSensors);
		scores[ScoreLabelTypes.SENSORS.ordinal()] = (int) activeSensors.toLongArray()[0]; // todo only 32 sensor types supported
		scores[ScoreLabelTypes.SENSOR_VARIO.ordinal()] = detectedSensors.contains(Sensor.VARIO) ? 1000 : 0;
		scores[ScoreLabelTypes.SENSOR_GPS.ordinal()] = detectedSensors.contains(Sensor.GPS) ? 1000 : 0;
		scores[ScoreLabelTypes.SENSOR_GAM.ordinal()] = detectedSensors.contains(Sensor.GAM) ? 1000 : 0;
		scores[ScoreLabelTypes.SENSOR_EAM.ordinal()] = detectedSensors.contains(Sensor.EAM) ? 1000 : 0;
		scores[ScoreLabelTypes.SENSOR_ESC.ordinal()] = detectedSensors.contains(Sensor.ESC) ? 1000 : 0;
		scores[ScoreLabelTypes.SENSOR_COUNT.ordinal()] = (detectedSensors.size() - 1) * 1000; // exclude receiver
		scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = (int) 4.0 * 1000; // V4 with and without container
		scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = 0;
		scores[ScoreLabelTypes.LOG_FILE_VERSION.ordinal()] = 0;
		scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = histoRandomSample.getReadingCount() * HoTTbinHistoReader.DATA_BLOCK_SIZE;
		scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) vault.getLogFileLength();
		scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = (detectedSensors.size() + 1) * 1000; // +1 for channel
		scores[ScoreLabelTypes.ELAPSED_HISTO_RECORD_SET_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime); // do not multiply by 1000 as usual, this is the conversion from microseconds to ms
		return scores;
	}

	/**
	 * @param lostPackages might be null if the reader did not collect the lost packages statistics
	 */
	protected void writeFinalLog(boolean isTextModusSignaled, @Nullable PackageLoss lostPackages, UniversalSampler histoRandomSample,
			ExtendedVault vault) {
		int lossTotal = lostPackages == null ? 0 : lostPackages.lossTotal;
		if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("%s > packages:%,9d  readings:%,9d  sampled:%,9d  overSampled:%4d", //
				tmpRecordSet.getChannelConfigName(), vault.getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE, histoRandomSample.getReadingCount(), tmpRecordSet.getRecordDataSize(true), histoRandomSample.getOverSamplingCount()));
		if (log.isLoggable(Level.TIME))
			log.log(Level.TIME, String.format("initiateTime: %,7d  readTime: %,7d  reviewTime: %,7d  addTime: %,7d  pickTime: %,7d  finishTime: %,7d", //
					TimeUnit.NANOSECONDS.toMillis(initiateTime), // $NON-NLS-1$
					TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime), TimeUnit.NANOSECONDS.toMillis(finishTime)));
		if (lostPackages != null) {
			if (tmpRecordSet.getMaxTime_ms() > 0) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", //
						lossTotal, (int) (lossTotal / tmpRecordSet.getMaxTime_ms() * 1000. * RECORD_TIMESPAN_MS), vault.getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE, lostPackages.getMaxValue() * 10, (int) lostPackages.getAvgValue() * 10));
			} else {
				log.log(Level.WARNING, String.format("RecordSet with unidentified data.  fileLength=%,11d   isTextModusSignaled=%b   %s", //
						vault.getLogFileLength(), isTextModusSignaled, vault.getLoadFilePath())); // $NON-NLS-1$
			}
		}
	}

}

