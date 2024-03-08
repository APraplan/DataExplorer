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

    Copyright (c) 2016,2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.histo.io;

import static java.util.logging.Level.FINEST;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import gde.Analyzer;
import gde.GDE;
import gde.data.AbstractRecordSet;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.ChannelTypes;
import gde.device.IDevice;
import gde.device.ScoreLabelTypes;
import gde.exception.DataInconsitsentException;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.VaultCollector;
import gde.histo.device.IHistoDevice;
import gde.histo.recordings.TrailRecordSet;
import gde.io.OsdReaderWriter;
import gde.log.Level;
import gde.log.Logger;
import gde.utils.FileUtils;
import gde.utils.StringHelper;

/**
 * Read the DataExplorer file format into histo recordsets.
 * @author Thomas Eickert
 */
public final class HistoOsdReaderWriter extends OsdReaderWriter {
	final private static String	$CLASS_NAME	= HistoOsdReaderWriter.class.getName();
	final private static Logger	log					= Logger.getLogger($CLASS_NAME);

	private class HeaderParser {

		private final HashMap<String, String>	header;

		// lazy parsing
		private List<RecordSetParser>					osdRecordSets;

		HeaderParser(HashMap<String, String> header) throws FileNotFoundException, IOException, NotSupportedFileFormatException {
			this.header = header;
		}

		int getRecordSetSize() {
			return Integer.parseInt(header.get(GDE.RECORD_SET_SIZE).trim());
		}

		int getFileVersion() {
			return (int) getDataExplorerVersion();
		}

		double getDataExplorerVersion() {
			// OpenSerialData version : 1
			return header.containsKey(GDE.DATA_EXPLORER_FILE_VERSION) ? Double.parseDouble(header.get(GDE.DATA_EXPLORER_FILE_VERSION)) : 1.;
		}

		String getLogObjectKey() {
			return header.containsKey(GDE.OBJECT_KEY) ? header.get(GDE.OBJECT_KEY) : GDE.STRING_EMPTY;
		}

		String getDeviceName() {
			return header.get(GDE.DEVICE_NAME);
		}

		@SuppressWarnings("unused")
		ChannelTypes getChannelConfigType() {
			return ChannelTypes.valueOf(header.get(GDE.CHANNEL_CONFIG_TYPE).trim());
		}

		HashMap<String, String> getHeader() {
			return this.header;
		}

		List<RecordSetParser> getOsdRecordSets() {
			if (osdRecordSets == null) setOsdRecordSets();
			return osdRecordSets;
		}

		private void setOsdRecordSets() {
			osdRecordSets = new ArrayList<>();
			for (int i = 0; i < getRecordSetSize(); i++) {
				String line = GDE.RECORD_SET_NAME + GDE.STRING_EMPTY + getHeader().get("" + (i + 1) + GDE.STRING_BLANK + GDE.RECORD_SET_NAME);
				RecordSetParser osdRecordSet = new RecordSetParser(this, getRecordSetProperties(line));
				osdRecordSets.add(osdRecordSet);
			}
		}

		/**
		 * @return zero or the osd header creation timestamp
		 */
		long getCreationTimeStamp_ms() {
			if (header.containsKey(GDE.CREATION_TIME_STAMP)) {
				try {
					return Instant.parse(header.get(GDE.CREATION_TIME_STAMP)).toEpochMilli();
				} catch (DateTimeParseException e) {}

				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd" + ' ' + " HH:mm:ss"); //$NON-NLS-1$ //$NON-NLS-2$
				try {
					return simpleDateFormat.parse(header.get(GDE.CREATION_TIME_STAMP)).getTime();
				} catch (Exception ex) { // ParseException ex) {
					throw new RuntimeException(ex);
				}
			} else {
				return 0;
			}
		}
	}

	private static class RecordSetParser {

		private final HeaderParser						osdHeader;
		private final HashMap<String, String>	recordSetInfo;
		private final HashMap<String, String>	recordSetProps;

		// lazy parsing
		private List<RecordParser>						osdRecords;

		RecordSetParser(HeaderParser osdHeader, HashMap<String, String> recordSetInfo) {
			this.osdHeader = osdHeader;
			this.recordSetInfo = recordSetInfo;

			String recordSetProperties = recordSetInfo.get(GDE.RECORD_SET_PROPERTIES);
			recordSetProps = StringHelper.splitString(recordSetProperties, Record.DELIMITER, RecordSet.propertyKeys);
		}

		/**
		 * determine channel number in file and access the device configuration.
		 */
		Channel getChannel(Channels channels) {
			Channel channel = null;
			String channelConfig = recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME);
				// String channelConfigKey;
				// Channel currentChannel = channels.get(channels.getChannelNumber(channelConfig));
				// 1.st try channelConfiguration not found
				try { // get channel last digit and use as channel config ordinal : 'Channel/Configuration Name: 1 : Ausgang 1'
					channel = channels.get(Integer.parseInt(channelConfig.substring(channelConfig.length() - 1)));
					// channelConfigKey = channel.getChannelConfigKey();
				}
				catch (NumberFormatException e) {
					// ignore and keep channel as null
				}
				catch (NullPointerException e) {
					// ignore and keep channel as null
				}
				if (channel == null) { // 2.nd try channelConfiguration not found
					try { // try to get channel startsWith configuration name : 'Channel/Configuration Name: 1 : Receiver'
						channel = channels.get(Integer.parseInt(channelConfig.split(GDE.STRING_BLANK)[0]));
						// channelConfigKey = channel.getChannelConfigKey();
					}
					catch (NullPointerException | NumberFormatException e) {
						// ignore and keep channel as null
					}
				}
				if (channel == null) { // 3.rd try channelConfiguration not found
					// ET 20161121 reactivated for '2008_04-05_ASW27_Motor_Test_Akku_Vergleich.osd' and similar files
					// do not rely on channel nomenclature
					// "3 : Motor"
					String channelConfigKey;
					channelConfigKey = channelConfig.contains(GDE.STRING_COLON) ? channelConfig.split(GDE.STRING_COLON)[1].trim() : channelConfig.trim();
					// "Motor 3"
					channelConfigKey = channelConfigKey.contains(GDE.STRING_BLANK) ? channelConfigKey.split(GDE.STRING_BLANK)[0].trim() : channelConfigKey.trim();
					// "Motor"
					channel = channels.get(channels.getChannelNumber(channelConfigKey));
				}
				return channel;
		}

		@SuppressWarnings("unused")
		String getName() {
			return recordSetInfo.get(GDE.RECORD_SET_NAME);
		}

		String getBaseName() {
			return recordSetInfo.get(GDE.RECORD_SET_NAME) + TrailRecordSet.BASE_NAME_SEPARATOR + recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME);
		}

		long getDataPointer() {
			return Long.parseLong(recordSetInfo.get(GDE.RECORD_SET_DATA_POINTER));
		}

		int getRecordDataSize() {
			return Integer.parseInt(recordSetInfo.get(GDE.RECORD_DATA_SIZE));
		}

		String getComment() {
			return recordSetInfo.get(GDE.RECORD_SET_COMMENT);
		}

		Long getStartTimestamp_ms() {
			return recordSetProps.containsKey(AbstractRecordSet.START_TIME_STAMP) && !recordSetProps.get(AbstractRecordSet.START_TIME_STAMP).isEmpty()
					? Long.parseLong(recordSetProps.get(AbstractRecordSet.START_TIME_STAMP).trim()) : null;
		}

		/**
		 * Search the recordset comment for the sensor signature in the format '[RECEIVER,GAM,VARIO]'.
		 * @return a csv list of names of the sensors or an empty string if parsing was not successful
		 */
		String getSensorSignature() {
			String recordSetComment = getComment();
			int idx1 = recordSetComment.indexOf(GDE.CHAR_LEFT_BRACKET);
			int idx2 = recordSetComment.indexOf(GDE.CHAR_RIGHT_BRACKET);
			if (idx1 > 0 && idx2 > idx1) {
				return recordSetComment.substring(idx1 + 1, idx2);
			} else {
				return "";
			}
		}

		/**
		 * @return null if parsing was not successful.
		 */
		Double getFirmware() {
			String recordSetComment = getComment();
			final String label = "Firmware"; //$NON-NLS-1$
			int idx1 = recordSetComment.indexOf(label);
			// int idx2 = recordSetComment.indexOf(GDE.CHAR_RIGHT_BRACKET);
			if (idx1 > 0) {
				try {
					String firmwareVersion = recordSetComment.substring(idx1 + label.length(), recordSetComment.length());
					// change comma to dot and take digits, dots and signs only
					return Double.parseDouble(firmwareVersion.replace(',', '.').replaceAll("[^0-9.]", ""));
				} catch (Exception e) {
					return null;
				}
			} else {
				return null;
			}
		}

		/**
		 * Search the recordset comment for package loss data in the format '\n<GDE_MSGI2404> = 1154 ~ 13,6 %'.
		 * @return packagesLostCounter, packagesLostRatio, minPackagesLost_sec, maxPackagesLost_sec, avgPackagesLost_sec,
		 *         sigmaPackagesLost_sec. zero if parsing was not successful.
		 */
		double[] getPackageLoss() {
			String recordSetComment = getComment();
			NumberFormat doubleFormat = NumberFormat.getInstance(Locale.getDefault());
			double[] values = new double[6];
			// GDE_MSGI2404=\nVerlorene Rückkanalpakete = {0} ~ {1} % ({2})
			// "min=%.2f sec; max=%.2f sec; avg=%.2f sec; sigma=%.2f sec"
			int idx0 = recordSetComment.indexOf(GDE.CHAR_NEW_LINE);
			int idx1 = recordSetComment.indexOf(GDE.CHAR_EQUAL) + 1;
				try {
				//old formating = 689 ~ 14,2 % new formating = (1638) 1272 ~2,1 % 
				boolean isNewFormat = recordSetComment.indexOf(GDE.CHAR_LEFT_PARENTHESIS) == idx1 + 1;
				idx1 = isNewFormat ? recordSetComment.indexOf(GDE.CHAR_BLANK, idx1+2) : idx1;
				int idx2 = isNewFormat ? recordSetComment.indexOf("~", idx1) - 1 : recordSetComment.indexOf("~", idx1); //$NON-NLS-1$
				int idx3 = recordSetComment.indexOf("%", idx2); //$NON-NLS-1$
				String numLostPackages = recordSetComment.substring(idx1+1, idx2).trim();
				String percentLostPackages = recordSetComment.substring(idx2 + 2, idx3 - 1).trim();
				if (idx0 > 0 && idx1 > idx0 && idx2 > idx1 && idx3 > idx2) {
						values[0] = Double.parseDouble(numLostPackages); // integer
						values[1] = doubleFormat.parse(percentLostPackages).doubleValue();
						String[] times = (recordSetComment.substring(idx3 + 1)).split("="); //$NON-NLS-1$
						if (times.length > 1) { // time statistics is available
							values[2] = doubleFormat.parse(times[1].substring(0, times[1].indexOf(" "))).doubleValue(); //$NON-NLS-1$
							values[3] = doubleFormat.parse(times[2].substring(0, times[2].indexOf(" "))).doubleValue(); //$NON-NLS-1$
							values[4] = doubleFormat.parse(times[3].substring(0, times[3].indexOf(" "))).doubleValue(); //$NON-NLS-1$
							values[5] = doubleFormat.parse(times[4].substring(0, times[4].indexOf(" "))).doubleValue(); //$NON-NLS-1$
						}
				}
			} catch (RuntimeException | ParseException e) {
				log.log(Level.WARNING, recordSetComment +  "\n" + recordSetComment.substring(idx1));
				log.log(Level.WARNING, e.getMessage());
			}
			return values;
		}

		/**
		 * @return start timestamp from recordset properties, alternatively take RecordedTS from RecordSetComment, alternatively
		 *         FileCreatedTS
		 */
		long getEnhancedStartTimestamp_ms() {
			Long startTimestamp_ms = getStartTimestamp_ms();

			// try 1: osd version >= 3 only --- V 1 and 2 do not carry a recordset start timestamp
			if (startTimestamp_ms == null) { // copied from gde.data.RecordSet.setDeserializedProperties(String)
				try {
					// try 2: check if the original time stamp in the recordset comment is available
					String[] parts = getComment().split(" ");
					String date = "";
					String time = "";
					for (String stringPart : parts) {
						Pattern datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}"); //$NON-NLS-1$
						Matcher dateMatcher = datePattern.matcher(stringPart);
						Pattern timePattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}"); //$NON-NLS-1$
						Matcher timeMatcher = timePattern.matcher(stringPart);
						if (dateMatcher.find()) {
							date = dateMatcher.group();
						}
						if (timeMatcher.find()) {
							time = timeMatcher.group();
						}
					}
					if (!date.isEmpty()) {
						String[] strValueDate = date.split(GDE.STRING_DASH);
						int year = Integer.parseInt(strValueDate[0]);
						int month = Integer.parseInt(strValueDate[1]);
						int day = Integer.parseInt(strValueDate[2]);

						int hour = 0;
						int minute = 0;
						int second = 0;
						if (!time.isEmpty()) {
							String[] strValueTime = time.split(GDE.STRING_COLON);
							hour = Integer.parseInt(strValueTime[0]);
							minute = Integer.parseInt(strValueTime[1]);
							second = Integer.parseInt(strValueTime[2]);
						}
						GregorianCalendar calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
						startTimestamp_ms = calendar.getTimeInMillis();
					}
				} catch (Exception e) {
					// ignore and keep startTimestamp_ms = 0
				}
			}
			// try 3: take the osd file creation timestamp
			if (startTimestamp_ms == null || startTimestamp_ms == 0) {
				startTimestamp_ms = osdHeader.getCreationTimeStamp_ms();
			}
			log.log(FINEST, "startTimestamp_ms=", startTimestamp_ms); //$NON-NLS-1$
			return startTimestamp_ms;
		}

		/**
		 * @return the record names (measurement names at the time of osd creation)
		 */
		String[] getRecordKeys() {
			List<RecordParser> records = getOsdRecords();
			String[] recordKeys = new String[records.size()];
			for (int i = 0; i < records.size(); i++) {
				RecordParser osdRecord = records.get(i);
				recordKeys[i] = osdRecord.getName();
			}
			return recordKeys;
		}

		List<RecordParser> getOsdRecords() {
			if (osdRecords == null) setOsdRecords();
			return osdRecords;
		}

		private void setOsdRecords() {
			osdRecords = new ArrayList<>();
			String[] recordsProperties = StringHelper.splitString(recordSetInfo.get(GDE.RECORDS_PROPERTIES), Record.END_MARKER, GDE.RECORDS_PROPERTIES);
			for (String recordPropertiesInfo : recordsProperties) {
				RecordParser osdRecord = new RecordParser(recordPropertiesInfo);
				osdRecords.add(osdRecord);
			}
		}

		HeaderParser getOsdHeader() {
			return this.osdHeader;
		}
	}

	private static class RecordParser {

		private final HashMap<String, String> recordProperties;

		RecordParser(String recordPropertiesInfo) {
			recordProperties = StringHelper.splitString(recordPropertiesInfo, Record.DELIMITER, Record.propertyKeys);
		}

		int getMinValue() {
			return Integer.parseInt(recordProperties.get(Record.MIN_VALUE).trim());
		}

		int getMaxValue() {
			return Integer.parseInt(recordProperties.get(Record.MAX_VALUE).trim());
		}

		String getName() {
			return recordProperties.get(Record.NAME);
		}
	}

	private enum PointsType {
		MAX, MIN
	};

	/**
	 * Identify an enhanced value for the start timestamp.
	 * @param fileStream is a zipped or standard filestream
	 * @param sourcePath is the path corresponding to the fileStream and is put into the trusses
	 * @param objectDirectory holds the validated object key from the parent directory or is empty
	 * @return the vaults skeletons created from the recordsets (for valid channels in the current device only)
	 */
	public static List<VaultCollector> readTrusses(InputStream fileStream, Path sourcePath, String objectDirectory, Analyzer analyzer) throws IOException,
			NotSupportedFileFormatException {
		List<VaultCollector> trusses = new ArrayList<>();
		final HeaderParser osdHeader;

		try (DataInputStream data_in = new DataInputStream(FileUtils.wrapIfZipStream(fileStream))) { // closes the inputStream also
			HashMap<String, String> header = readHeader(sourcePath.toString(), data_in);
			osdHeader = new HistoOsdReaderWriter().new HeaderParser(header);
		}

		final List<RecordSetParser> osdRecordSets = osdHeader.getOsdRecordSets();
		for (int i = 0; i < osdRecordSets.size(); ++i) {
			RecordSetParser osdRecordSet = osdRecordSets.get(i);
			Channel channel = osdRecordSet.getChannel(analyzer.getChannels());
			if (channel != null) {
				VaultCollector vaultCollector = new VaultCollector(analyzer, objectDirectory, sourcePath, osdHeader.getFileVersion(), osdRecordSets.size(), i,
						osdRecordSet.getBaseName(), osdHeader.getDeviceName(), osdRecordSet.getEnhancedStartTimestamp_ms(), channel.getNumber(), osdHeader.getLogObjectKey());
				trusses.add(vaultCollector);
			}
		}
		log.fine(() -> " " + trusses.size() + " identified in " + sourcePath); //$NON-NLS-1$
		return trusses;
	}

	/**
	 * Read histo record sets from one single file.
	 * @param fileStream
	 * @param trusses holds a non-empty subset of the recordsets in the file
	 * @return histo vault list (may contain trusses without measurements, settlements and scores)
	 */
	public static List<ExtendedVault> readVaults(InputStream fileStream, List<VaultCollector> trusses, Analyzer analyzer) throws IOException,
			NotSupportedFileFormatException, DataInconsitsentException {
		if (trusses.isEmpty()) throw new IllegalArgumentException("at least one trusses entry is required");
		List<ExtendedVault> histoVaults = new ArrayList<>();

		// build list of requested recordset ordinals
		List<Integer> recordSetOrdinals = trusses.stream().mapToInt(a -> a.getVault().getLogRecordSetOrdinal()).boxed().collect(Collectors.toList());

		InputStream inputStream = FileUtils.wrapIfZipStream(fileStream);
		try (DataInputStream data_in = new DataInputStream(inputStream)) { // closes the inputStream also
			final HashMap<String, String> header = readHeader(trusses.get(0).getVault().getLoadFilePath(), data_in);
			final HeaderParser osdHeader = new HistoOsdReaderWriter().new HeaderParser(header);

			long startTimeNs = System.nanoTime();
			long unreadDataPointer = -1;
			for (int i = 0; i < osdHeader.getRecordSetSize(); i++) {
				RecordSetParser osdRecordSet = osdHeader.getOsdRecordSets().get(i);

				int trussesIndex = recordSetOrdinals.indexOf(i);
				if (trussesIndex < 0) {
					// defer reading any unused recordsets until a recordset is actually required
					if (unreadDataPointer < 0) unreadDataPointer = osdRecordSet.getDataPointer();
				} else {
					long elapsedStart_ns = System.nanoTime();
					unreadDataPointer = skipUnreadData(data_in, unreadDataPointer, osdRecordSet.getDataPointer());

					final VaultCollector vaultCollector = trusses.get(trussesIndex);

					RecordSet histoRecordSet = constructRecordSet(data_in, osdRecordSet, vaultCollector.getVault(), analyzer);

					final Integer[] scores = determineScores(osdRecordSet, elapsedStart_ns, histoRecordSet.getFileDataBytesSize(), vaultCollector.getVault().getLogFileLength(), analyzer.getActiveDevice());
					vaultCollector.promoteTruss(histoRecordSet, scores);

					histoVaults.add(vaultCollector.getVault());

					log.finer(() -> String.format("|%s|  startTimeStamp=%s    recordDataSize=%,d  recordSetDataPointer=%,d", //$NON-NLS-1$
							osdRecordSet.getChannel(analyzer.getChannels()).getName(), vaultCollector.getVault().getStartTimeStampFormatted(), osdRecordSet.getRecordDataSize(), osdRecordSet.getDataPointer()));
				}
			}
			log.fine(() -> String.format("%d of%3d recordsets in%,7d ms  recordSetOrdinals=%s from %s", //$NON-NLS-1$
					histoVaults.size(), osdHeader.getRecordSetSize(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNs), recordSetOrdinals.toString(), trusses.get(0).getVault().getLoadFilePath()));
		}
		return histoVaults;
	}

	/**
	 * @param data_in
	 * @param unreadDataPointer points to the stream position which has not been read yet
	 * @param recordSetDataPointer points to the stream position which shall be read next
	 * @return the new value for the unreadDataPointer
	 */
	private static long skipUnreadData(DataInputStream data_in, long unreadDataPointer, long recordSetDataPointer) throws IOException, EOFException {
		if (unreadDataPointer > -1) {
			// make up all deferred readings in one single step
			long toSkip = recordSetDataPointer - unreadDataPointer;
			while (toSkip > 0) {
				// The skip method may, for a variety of reasons, end up skipping over some smaller number of bytes, possibly 0.
				// The actual number of bytes skipped is returned.
				toSkip -= data_in.skip(toSkip);
				if (toSkip > 0) {
					log.log(Level.INFO, String.format("recordSetDataPointer=%,9d toSkip=%,9d", recordSetDataPointer, toSkip)); //$NON-NLS-1$
					if (data_in.available() == 0) throw new EOFException("recordDataSize / recordSetDataPointer do not match the actual file size"); //$NON-NLS-1$
				}
			}
		}
		return -1;
	}

	/**
	 * Read recordset data from the log file raw data.
	 * Take meta data data from the log file recordset header.
	 * @param data_in
	 * @param osdRecordSet holds recordset meta data from the file header
	 * @param truss is the vault skeleton with the base data for the new recordset
	 * @param analyzer
	 * @return the new recordset fully populated
	 */
	private static RecordSet constructRecordSet(DataInputStream data_in, RecordSetParser osdRecordSet, ExtendedVault truss, Analyzer analyzer) throws IOException,
			DataInconsitsentException {
		RecordSet histoRecordSet = buildRecordSet(truss.getLogRecordsetBaseName(), truss.getLogChannelNumber(), osdRecordSet.recordSetInfo, false, analyzer);

		{// setDeserializedProperties does not take all possible solutions to set the timestamp
			long enhancedStartTimestamp_ms = osdRecordSet.getEnhancedStartTimestamp_ms();
			if (histoRecordSet.getStartTimeStamp() != enhancedStartTimestamp_ms) {
				log.info(() -> String.format("startTimeStamp rectified %,d  %,d", //$NON-NLS-1$
						enhancedStartTimestamp_ms, histoRecordSet.getStartTimeStamp()));
			}
			histoRecordSet.setStartTimeStamp(enhancedStartTimestamp_ms);
		}

		String[] noneCalculationMeasurementNames = histoRecordSet.getNoneCalculationRecordNames();
		int numberRecordAndTimeStamp = noneCalculationMeasurementNames.length + (histoRecordSet.isTimeStepConstant() ? 0 : 1);
		final int recordSetDataBytes = GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * osdRecordSet.getRecordDataSize();
		final int recordDataSize = osdRecordSet.getRecordDataSize();

		histoRecordSet.setFileDataPointerAndSize(osdRecordSet.getDataPointer(), recordDataSize, recordSetDataBytes);
		log.fine(() -> String.format("%s recordDataSize=%,d  recordSetDataPointer=%,d  numberRecordAndTimeStamp=%,d", //$NON-NLS-1$
				osdRecordSet.getChannel(analyzer.getChannels()).getName(), recordDataSize, osdRecordSet.getDataPointer(), numberRecordAndTimeStamp));

		final byte[] buffer = new byte[recordSetDataBytes];
		data_in.readFully(buffer);

		if (histoRecordSet.getDevice() instanceof IHistoDevice) {
			Map<PointsType, int[]> extrema = getExtremumValues(osdRecordSet, noneCalculationMeasurementNames);
			((IHistoDevice) histoRecordSet.getDevice()).addDataBufferAsRawDataPoints(histoRecordSet, buffer, recordDataSize, extrema.get(PointsType.MAX), extrema.get(PointsType.MIN), analyzer);
		} else {
			histoRecordSet.getDevice().addDataBufferAsRawDataPoints(histoRecordSet, buffer, recordDataSize, false);
		}
		return histoRecordSet;
	}

	/**
	 * @param osdRecordSet
	 * @param elapsedStart_ns is the nanotime when the recordset evaluation was started
	 * @param recordSetDataBytes is the number of bytes in the recordset data
	 * @param fileLength is the number of bytes in the log file
	 * @param iDevice
	 * @return the scores (may contain null values to be filled in later)
	 */
	private static Integer[] determineScores(RecordSetParser osdRecordSet, long elapsedStart_ns, int recordSetDataBytes, long fileLength, IDevice device) {
		final HeaderParser osdHeader = osdRecordSet.getOsdHeader();
		final Double logDataVersion = osdRecordSet.getFirmware();
		final Integer[] scores = new Integer[ScoreLabelTypes.VALUES.length];

		// values are multiplied by 1000 as this is the convention for internal values
		// in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
		// scores for duration and timestep values are filled in by the HistoRecordSet
		scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = osdRecordSet.getRecordDataSize();
		if (device instanceof IHistoDevice) {
			double[] packagesLost = new double[6];
			try {
				packagesLost = osdRecordSet.getPackageLoss();
			}
			catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
			BitSet activeSensors = ((IHistoDevice) device).getActiveSensors(osdRecordSet.getSensorSignature());
			// recalculating the following scores from raw data would be feasible
			scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = packagesLost[1] != 0 ? (int) (packagesLost[0] * 100. / packagesLost[1]) : 0;
			scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = (int) packagesLost[0];
			scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = (int) (packagesLost[1] * 10. * 1000.); // % -> %o
			scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = (int) (packagesLost[4] * 1000000.); // sec -> ms
			scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = (int) (packagesLost[3] * 1000000.); // sec -> ms
			scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = (int) (packagesLost[2] * 1000000.); // sec -> ms
			scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = (int) (packagesLost[5] * 1000000.); // sec -> ms
			scores[ScoreLabelTypes.SENSORS.ordinal()] = (int) activeSensors.toLongArray()[0]; // todo only 31 sensor types supported
			scores[ScoreLabelTypes.SENSOR_VARIO.ordinal()] = activeSensors.get(1) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GPS.ordinal()] = activeSensors.get(2) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GAM.ordinal()] = activeSensors.get(3) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_EAM.ordinal()] = activeSensors.get(4) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_ESC.ordinal()] = activeSensors.get(5) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_COUNT.ordinal()] = (activeSensors.cardinality() - 1) * 1000; // exclude receiver
		} else {
			scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = 0;
			scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = 0;
			scores[ScoreLabelTypes.SENSORS.ordinal()] = 0;
			scores[ScoreLabelTypes.SENSOR_VARIO.ordinal()] = 0;
			scores[ScoreLabelTypes.SENSOR_GPS.ordinal()] = 0;
			scores[ScoreLabelTypes.SENSOR_GAM.ordinal()] = 0;
			scores[ScoreLabelTypes.SENSOR_EAM.ordinal()] = 0;
			scores[ScoreLabelTypes.SENSOR_ESC.ordinal()] = 0;
			scores[ScoreLabelTypes.SENSOR_COUNT.ordinal()] = 0;
		}
		scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = logDataVersion != null ? (int) (logDataVersion * 1000.) : 0; // Firmware
		scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = (int) (osdHeader.getDataExplorerVersion() * 1000.); // from file
		scores[ScoreLabelTypes.LOG_FILE_VERSION.ordinal()] = osdHeader.getFileVersion() * 1000; // from file
		scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = recordSetDataBytes;
		scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) fileLength;
		scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = osdHeader.getRecordSetSize() * 1000;
		// do not multiply by 1000 as usual, this is the conversion from microseconds to ms
		scores[ScoreLabelTypes.ELAPSED_HISTO_RECORD_SET_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - elapsedStart_ns);

		return scores;
	}

	/**
	 * @return the max / min values for all records
	 */
	private static Map<PointsType, int[]> getExtremumValues(RecordSetParser osdRecordSet, String[] noneCalculationMeasurementNames) {
		int[] maxPoints = new int[noneCalculationMeasurementNames.length];
		int[] minPoints = new int[noneCalculationMeasurementNames.length];
		List<RecordParser> osdRecords = osdRecordSet.getOsdRecords();
		String[] recordKeys = osdRecordSet.getRecordKeys();
		List<String> noneCalculationMeasurementNameList = Arrays.asList(noneCalculationMeasurementNames);
		for (int j = 0; j < recordKeys.length; j++) {
			RecordParser osdRecord = osdRecords.get(j);
			int index = noneCalculationMeasurementNameList.indexOf(recordKeys[j]);
			if (index > -1) {
				maxPoints[index] = osdRecord.getMaxValue();
				minPoints[index] = osdRecord.getMinValue();
			}
		}
		Map<PointsType, int[]> extrema = new HashMap<>();
		extrema.put(PointsType.MAX, maxPoints);
		extrema.put(PointsType.MIN, minPoints);
		return extrema;
	}

}
