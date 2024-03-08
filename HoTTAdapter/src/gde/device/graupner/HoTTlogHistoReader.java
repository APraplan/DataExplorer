
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
import gde.device.graupner.HoTTlogReader.ChnLogParser;
import gde.device.graupner.HoTTlogReader.EamLogParser;
import gde.device.graupner.HoTTlogReader.EscLogParser;
import gde.device.graupner.HoTTlogReader.GamLogParser;
import gde.device.graupner.HoTTlogReader.GpsLogParser;
import gde.device.graupner.HoTTlogReader.LogParser;
import gde.device.graupner.HoTTlogReader.RcvLogParser;
import gde.device.graupner.HoTTlogReader.VarLogParser;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.ThrowableUtils;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.VaultCollector;
import gde.histo.device.UniversalSampler;
import gde.log.Level;
import gde.utils.StringHelper;

/**
 * Read Graupner HoTT binary data for history analysis.
 * Collect data in a recordset and fill the vault collector.
 * Read measurements for one single channel which holds a maximum of one sensor.
 * For small files (around 1 minute) no measurements are added to the recordset.
 * Support sampling to maximize the throughput.
 * @author Winfried Brügmann
 */
public class HoTTlogHistoReader {
	private static final String	$CLASS_NAME					= HoTTlogHistoReader.class.getName();
	private static final Logger	log									= Logger.getLogger(HoTTlogHistoReader.$CLASS_NAME);

	/**
	 * HoTT logs data rate defined by the channel log
	 */
	protected final static int	RECORD_TIMESPAN_MS	= 50;

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
	protected final int								dataBlockSize;
	protected final int								rawDataBlockSize;
	protected final int								logDataOffset;
	protected final long							logEntryCount;
	protected final int								numberUsedChannels;
	protected final boolean						isASCII;

	protected long										nanoTime;
	protected long										currentTime, initiateTime, readTime, reviewTime, addTime, pickTime, finishTime, lastTime;
	/**
	 * The detected sensors including the receiver but without 'channel'
	 */
	protected EnumSet<Sensor>					detectedSensors;
	protected RecordSet								tmpRecordSet;
	protected VaultCollector					truss;
	protected HashMap<String, String> infoHeader;

	public HoTTlogHistoReader(PickerParameters pickerParameters, HashMap<String, String> newInfoHeader) {
		this(pickerParameters, pickerParameters.analyzer.getActiveChannel().getNumber() == Sensor.CHANNEL.getChannelNumber(), 1, newInfoHeader);
	}

	public HoTTlogHistoReader(PickerParameters pickerParameters, int initializeSamplingFactor, HashMap<String, String> newInfoHeader) {
		this(pickerParameters, pickerParameters.analyzer.getActiveChannel().getNumber() == Sensor.CHANNEL.getChannelNumber(), initializeSamplingFactor, newInfoHeader);
	}

	/**
	 * @param pickerParameters
	 * @param isChannelsChannelEnabled true activates the channel measurements
	 * @param initializeSamplingFactor increases the number of blocks for max/min evaluation and reduces the oversampling
	 */
	protected HoTTlogHistoReader(PickerParameters pickerParameters, boolean isChannelsChannelEnabled, int initializeSamplingFactor, HashMap<String, String> newInfoHeader) {
		this.pickerParameters = new PickerParameters(pickerParameters);
		this.pickerParameters.isFilterEnabled = true;

		this.analyzer = pickerParameters.analyzer;
		this.isChannelsChannelEnabled = isChannelsChannelEnabled;
		this.initializeSamplingFactor = initializeSamplingFactor;
		this.isFilterEnabled = true;
		this.isFilterTextModus = true;
		this.altitudeClimbSensorSelection = 0; //auto
		this.infoHeader = newInfoHeader;
		this.isASCII = infoHeader.get("LOG TYPE").contains("ASCII");
		this.detectedSensors = Sensor.getSetFromDetected(infoHeader.get(HoTTAdapter.DETECTED_SENSOR));
		this.dataBlockSize = Integer.parseInt(infoHeader.get(HoTTAdapter.DATA_BLOCK_SIZE));
		this.rawDataBlockSize = Integer.parseInt(infoHeader.get(HoTTAdapter.RAW_LOG_SIZE));
		this.logDataOffset = Integer.parseInt(infoHeader.get(HoTTAdapter.LOG_DATA_OFFSET));
		this.logEntryCount = Long.parseLong(infoHeader.get(HoTTAdapter.LOG_COUNT));
		this.numberUsedChannels = Integer.parseInt(infoHeader.get("LOG NOB CHANNEL"));
		if (log.isLoggable(Level.INFO))
			log.log(Level.INFO, String.format("%s isASCII = %b dataBlockSize = %d rawDataBlockSize = %d logDataOffset =  %d", infoHeader.get(HoTTAdapter.FILE_PATH).substring(infoHeader.get(HoTTAdapter.FILE_PATH).lastIndexOf(GDE.STRING_FILE_SEPARATOR_UNIX)), this.isASCII, this.dataBlockSize, this.rawDataBlockSize, this.logDataOffset));
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

		nanoTime = System.nanoTime();
		initiateTime = readTime = reviewTime = addTime = pickTime = finishTime = 0;
		lastTime = System.nanoTime();

		truss = newTruss;
		IDevice device = analyzer.getActiveDevice();
		ExtendedVault vault = truss.getVault();
		long numberDatablocks = Long.parseLong(infoHeader.get(HoTTAdapter.LOG_COUNT));
		tmpRecordSet = RecordSet.createRecordSet(vault.getLogRecordsetBaseName(), analyzer, analyzer.getActiveChannel().getNumber(), true, true, false);
		tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(infoHeader.get("LOG START TIME"), HoTTbinReader.getStartTimeStamp(vault.getLoadFileAsPath().getFileName().toString(), vault.getLogFileLastModified(), numberDatablocks)));
		tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", tmpRecordSet.getStartTimeStamp())); //$NON-NLS-1$
		tmpRecordSet.descriptionAppendFilename(vault.getLoadFileAsPath().getFileName().toString());
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, " recordSetBaseName=" + vault.getLogRecordsetBaseName());

		try {
			read(inputStream);
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
	protected void read(Supplier<InputStream> inputStream) throws IOException, DataTypeException, DataInconsitsentException {
		try (BufferedInputStream in = new BufferedInputStream(inputStream.get()); //
				InputStream data_in = in; ) {
			if (logDataOffset != data_in.skip(logDataOffset)) {
				log.log(Level.WARNING, "skipping logDataOffset failed");
			}
			int initializeBlockLimit = initializeSamplingFactor * HoTTbinReader.NUMBER_LOG_RECORDS_TO_SCAN / 5;
			int readLimitMark = (initializeBlockLimit + 1) * dataBlockSize + 1;
			data_in.mark(readLimitMark); // reduces # of overscan records
			int activeChannelNumber = analyzer.getActiveChannel().getNumber();
			int[]	points = new int[analyzer.getActiveDevice().getNumberOfMeasurements(activeChannelNumber)];
			UniversalSampler initSampler = UniversalSampler.createSampler(activeChannelNumber, points, HoTTlogHistoReader.RECORD_TIMESPAN_MS, analyzer);
			read(data_in, initializeBlockLimit, initSampler);
			data_in.reset();
			UniversalSampler sampler = UniversalSampler.createSampler(activeChannelNumber, initSampler.getMaxPoints(), initSampler.getMinPoints(), HoTTlogHistoReader.RECORD_TIMESPAN_MS, analyzer);
			read(data_in, -1, sampler);
		}
	}

	/**
	 * read log data
	 * allocates only one single recordset for the active channel, so HoTTAdapter.isChannelsChannelEnabled does not take any effect.
	 * no progress bar support and no channel data modifications.
	 */
	protected void read(InputStream data_in, int initializeBlocks, UniversalSampler histoRandomSample) throws DataInconsitsentException, IOException {
		int[]	points = histoRandomSample.getPoints();
		byte[] buf = new byte[this.dataBlockSize];
		long[] timeSteps_ms = new long[] { 0 };
		boolean	isTextModusSignaled	= false;
		LogParser logParser;
		int activeChannelNumber = analyzer.getActiveChannel().getNumber();
		if (activeChannelNumber == Sensor.RECEIVER.getChannelNumber()) {
			logParser = Sensor.RECEIVER.createLogParser(pickerParameters, points, timeSteps_ms, buf, numberUsedChannels);
		} else if (activeChannelNumber == Sensor.CHANNEL.getChannelNumber()) {
			logParser = Sensor.CHANNEL.createLogParser(pickerParameters, points, timeSteps_ms, buf, numberUsedChannels);
		} else if (activeChannelNumber == Sensor.VARIO.getChannelNumber()) {
			logParser = Sensor.VARIO.createLogParser(pickerParameters, points, timeSteps_ms, buf, numberUsedChannels);
		} else if (activeChannelNumber == Sensor.GPS.getChannelNumber()) {
			logParser = Sensor.GPS.createLogParser(pickerParameters, points, timeSteps_ms, buf, numberUsedChannels);
		} else if (activeChannelNumber == Sensor.GAM.getChannelNumber()) {
			logParser = Sensor.GAM.createLogParser(pickerParameters, points, timeSteps_ms, buf, numberUsedChannels);
		} else if (activeChannelNumber == Sensor.EAM.getChannelNumber()) {
			logParser = Sensor.EAM.createLogParser(pickerParameters, points, timeSteps_ms, buf, numberUsedChannels);
		} else if (activeChannelNumber == Sensor.ESC.getChannelNumber()) {
			logParser = Sensor.ESC.createLogParser(pickerParameters, points, timeSteps_ms, buf, numberUsedChannels);
		} else {
			throw new UnsupportedOperationException();
		}

		Procedure pointsAdder = initializeBlocks <= 0 //
				? () -> {
					readTimer.invoke();
					boolean isValidSample = histoRandomSample.capturePoints(timeSteps_ms[LogParser.TIMESTEP_INDEX]);
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
				} : () -> histoRandomSample.capturePoints(timeSteps_ms[LogParser.TIMESTEP_INDEX]);
		initTimer.invoke();

		// read all the data blocks from the file, parse only for the active channel
		boolean doFullRead = initializeBlocks <= 0;
		//boolean doDataSkip = detectedSensors.size() == 1 && !isChannelsChannelEnabled;
		int datablocksLimit = doFullRead ? (int) this.logEntryCount : initializeBlocks;
		int i = 0;
		for (; i < datablocksLimit; i++) { //skip log entries before transmitter active
			if (buf.length != data_in.read(buf))
				log.log(Level.WARNING, "reading buf failed 1");
			if (isASCII) { //convert ASCII log data to hex
				HoTTlogReader.convertAscii2Raw(this.rawDataBlockSize, buf);
			}
			//log.logp(Level.OFF, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, rawDataBlockSize));
			if (buf[8] == 0 || buf[9] == 0 || buf[24] == 0x1F) { // tx, rx, rx sensitivity data
				continue;
			}
			break;
		}
		
		for (; i < datablocksLimit; i++) {
			// if (log.isLoggable(Level.TIME))
			// log.log(Level.TIME, String.format("markpos: %,9d i: %,9d ", data_in.markpos, i));
			if (buf.length != data_in.read(buf)) 
				log.log(Level.WARNING, "reading buf failed 2");
			if (i % 10 == 0 && log.isLoggable(Level.FINE)) {
				if (isASCII)
					log.log(Level.FINE, new String(buf));
				else
					log.log(Level.FINE, StringHelper.byte2Hex4CharString(buf, buf.length));
			}

			if (this.isASCII) { //convert ASCII log data to hex
				HoTTlogReader.convertAscii2Raw(this.rawDataBlockSize, buf);
			}

			//if (!this.isFilterTextModus || (buf[6] & 0x01) == 0) { // switch into text modus
			if (buf[8] != 0 && buf[9] != 0) { //buf 8, 9, tx,rx, rx sensitivity data
				if (buf[24] != 0x1F) {//rx sensitivity data
					if (log.isLoggable(Level.INFO)) {
						log.log(Level.INFO, String.format("Sensor %02X", buf[26]));
					}

					if (logParser instanceof RcvLogParser) {
						((RcvLogParser) logParser).trackPackageLoss(true);
						logParser.parse();
						pointsAdder.invoke();
						((RcvLogParser) logParser).updateLossStatistics();
					}
					else if (logParser instanceof ChnLogParser) {
						logParser.parse();
						pointsAdder.invoke();
					}
					else if (logParser instanceof VarLogParser && buf[26] == HoTTAdapter.ANSWER_SENSOR_VARIO_19200) {
						logParser.parse();
						pointsAdder.invoke();
					}
					else if (logParser instanceof GpsLogParser && buf[26] == HoTTAdapter.ANSWER_SENSOR_GPS_19200) {
						logParser.parse();
						pointsAdder.invoke();
					}
					else if (logParser instanceof GamLogParser && buf[26] == HoTTAdapter.ANSWER_SENSOR_GENERAL_19200) {
						logParser.parse();
						pointsAdder.invoke();
					}
					else if (logParser instanceof EamLogParser && buf[26] == HoTTAdapter.ANSWER_SENSOR_ELECTRIC_19200) {
						logParser.parse();
						pointsAdder.invoke();
					}
					else if (logParser instanceof EscLogParser && buf[26] == HoTTAdapter.ANSWER_SENSOR_MOTOR_DRIVER_19200) {
						((EscLogParser)logParser).parse(tmpRecordSet, logParser.getTimeStep_ms());
						pointsAdder.invoke();
					}
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

			}
			else { // skip empty block, but add time step
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found tx=rx=0 dBm"); //$NON-NLS-1$
				if (logParser instanceof RcvLogParser) {
					((RcvLogParser) logParser).trackPackageLoss(false);
				}
				if (logParser instanceof ChnLogParser && logParser.parse()) {
					pointsAdder.invoke();
				}
				timeSteps_ms[LogParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;
			}
		}
		if (doFullRead) {
			PackageLoss lostPackages  = null;
			if (logParser instanceof RcvLogParser) {
				((RcvLogParser) logParser).finalUpdateLossStatistics();
				lostPackages  = ((RcvLogParser) logParser).getLostPackages();
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
	 * @param lostPackages might be null if the reader did not collect the lost packages statistics
	 * @return the scores array based on the score label type ordinal number
	 */
	protected Integer[] getScores(@Nullable PackageLoss lostPackages, UniversalSampler histoRandomSample, ExtendedVault vault) {
		int lossTotal = lostPackages == null ? 0 : lostPackages.lossTotal;
		final Integer[] scores = new Integer[ScoreLabelTypes.VALUES.length];
		// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
		// scores for duration and timestep values are filled in by the HistoVault
		scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = histoRandomSample.getReadingCount();
		scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = (int) (lostPackages == null ? vault.getLogFileLength() / this.dataBlockSize : lostPackages.numberTrackedSamples) * 1000;
		scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = lossTotal * 1000;
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
		scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = histoRandomSample.getReadingCount() * this.dataBlockSize;
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
				tmpRecordSet.getChannelConfigName(), vault.getLogFileLength() / this.dataBlockSize, histoRandomSample.getReadingCount(), tmpRecordSet.getRecordDataSize(true), histoRandomSample.getOverSamplingCount()));
		if (log.isLoggable(Level.TIME))
			log.log(Level.TIME, String.format("initiateTime: %,7d  readTime: %,7d  reviewTime: %,7d  addTime: %,7d  pickTime: %,7d  finishTime: %,7d", //
					TimeUnit.NANOSECONDS.toMillis(initiateTime), // $NON-NLS-1$
					TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime), TimeUnit.NANOSECONDS.toMillis(finishTime)));
		if (lostPackages != null) {
			if (tmpRecordSet.getMaxTime_ms() > 0) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", //
						lossTotal, (int) (lossTotal / tmpRecordSet.getMaxTime_ms() * 1000. * RECORD_TIMESPAN_MS), vault.getLogFileLength() / this.dataBlockSize, lostPackages.getMaxValue() * 10, (int) lostPackages.getAvgValue() * 10));
			} else {
				log.log(Level.WARNING, String.format("RecordSet with unidentified data.  fileLength=%,11d   isTextModusSignaled=%b   %s", //
						vault.getLogFileLength(), isTextModusSignaled, vault.getLoadFilePath())); // $NON-NLS-1$
			}
		}
	}

}

