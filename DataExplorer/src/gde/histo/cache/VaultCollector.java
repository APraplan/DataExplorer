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

    Copyright (c) 2017,2018,2019 Thomas Eickert
****************************************************************************************/

package gde.histo.cache;

import java.nio.file.Path;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import gde.Analyzer;
import gde.DataAccess;
import gde.GDE;
import gde.config.Settings;
import gde.data.IRecord;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IChannelItem;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.ScoreLabelTypes;
import gde.device.SettlementType;
import gde.device.StatisticsType;
import gde.device.TrailTypes;
import gde.device.TransitionAmountType;
import gde.device.TransitionCalculusType;
import gde.device.TransitionFigureType;
import gde.histo.datasources.HistoSet;
import gde.histo.datasources.SourceDataSet;
import gde.histo.device.IHistoDevice;
import gde.histo.settlements.AmountEvaluator;
import gde.histo.settlements.CalculusEvaluator;
import gde.histo.settlements.FigureEvaluator;
import gde.histo.settlements.SettlementRecord;
import gde.histo.transitions.GroupTransitions;
import gde.histo.transitions.Transition;
import gde.histo.transitions.TransitionCollector;
import gde.histo.transitions.TransitionTableMapper.SettlementRecords;
import gde.histo.utils.UniversalQuantile;
import gde.log.Logger;

/**
 * Supports the data collection for a serializable histo vault.
 * @author Thomas Eickert
 */
public final class VaultCollector {
	final private static String	$CLASS_NAME	= VaultCollector.class.getName();
	final private static Logger	log					= Logger.getLogger($CLASS_NAME);

	private enum Coding {
		BITS {
			@Override
			public UniversalQuantile<Integer> toIndexQuantile(IRecord record, Settings settings) {
				List<Integer> bitIndexes = record.getValues().stream().map(p -> p / 1000) // todo supports a maximum of 22 bits only
						.flatMap(v -> BitSet.valueOf(new long[] { v }).stream().boxed()) //
						.collect(Collectors.toList());
				UniversalQuantile<Integer> quantile = new UniversalQuantile<>(bitIndexes, true, false, false, settings);
				log.finer(() -> quantile.toString());
				return quantile;
			}

			@Override
			public UniversalQuantile<? extends Number> toValueQuantile(IRecord record, Settings settings) {
				List<Integer> intValues = record.getValues().stream().map(p -> p / 1000) //
						.collect(Collectors.toList());
				UniversalQuantile<Integer> quantile = new UniversalQuantile<>(intValues, true, false, false, settings);
				return quantile;
			}
		},
		TOKENS {
			@Override
			public UniversalQuantile<Integer> toIndexQuantile(IRecord record, Settings settings) {
				List<Integer> bitIndexes = record.getValues().stream().map(p -> p / 1000) //
						.collect(Collectors.toList());
				UniversalQuantile<Integer> quantile = new UniversalQuantile<>(bitIndexes, true, false, false, settings);
				log.finer(() -> quantile.toString());
				return quantile;
			}

			@Override
			public UniversalQuantile<? extends Number> toValueQuantile(IRecord record, Settings settings) {
				Function<Integer, Integer> indexToSetMapper = i -> {
					if (i <= 0) return 1; // setting the bits(0) prevents zero values from getting lost

					// take inputs from { 1 .. 32 } corresponding to { A .. Z plus some extra chars }
					// convert into an int value, i.e. calculate the power of two
					int idx = i & 0x1F; // todo supports a maximum of 31 tokens only
					return idx > 0 ? 1 << idx : 0; // 1-based token numbers ('A' maps to bits(1), bits(0) used for zero detection)
				};

				List<Integer> intValues = record.getValues().stream().map(p -> p / 1000) //
						.map(indexToSetMapper) //
						.collect(Collectors.toList());
				UniversalQuantile<Integer> quantile = new UniversalQuantile<>(intValues, true, false, false, settings);
				log.finer(() -> quantile.toString());
				return quantile;
			}
		},
		POINT {
			@Override
			public UniversalQuantile<Integer> toIndexQuantile(IRecord record, Settings settings) {
				throw new UnsupportedOperationException();
			}

			@Override
			public UniversalQuantile<? extends Number> toValueQuantile(IRecord record, Settings settings) {
				boolean removeCastaways = true;
				UniversalQuantile<Double> quantile = new UniversalQuantile<>(record.getTranslatedValues(), true, removeCastaways, false, settings);
				return quantile;
			}
		};

		public abstract UniversalQuantile<Integer> toIndexQuantile(IRecord record, Settings settings);

		public abstract UniversalQuantile<? extends Number> toValueQuantile(IRecord record, Settings settings);
	}

	private final Analyzer	analyzer;

	private ExtendedVault		vault;
	private SourceDataSet		sourceDataSet;

	/**
	 * Use this for import files.
	 * @param objectDirectory validated object key
	 * @param sourcePath is the bin origin file (not a link file)
	 * @param fileVersion is the version of the log origin file
	 * @param logRecordSetSize is the number of recordsets in the log origin file
	 * @param logRecordsetBaseName the base name without recordset number
	 * @param providesReaderSettings true is a file reader capable to deliver different measurement values based on device settings
	 */
	public VaultCollector(String objectDirectory, Path sourcePath, int fileVersion, int logRecordSetSize, String logRecordsetBaseName, //
			Analyzer analyzer, boolean providesReaderSettings) {
		this(objectDirectory, sourcePath, fileVersion, logRecordSetSize, 0, logRecordsetBaseName, analyzer.getActiveDevice().getName(), analyzer, //
				analyzer.getDataAccess().getSourceLastModified(sourcePath), analyzer.getActiveChannel().getNumber(), objectDirectory, providesReaderSettings);
	}

	/**
	 * Use this for log files which know about the device for recording (e.g. osd files).
	 * @param objectDirectory validated object key
	 * @param sourcePath is the log origin file (not a link file)
	 * @param fileVersion is the version of the log origin file
	 * @param logRecordSetSize is the number of recordsets in the log origin file
	 * @param logRecordSetOrdinal identifies multiple recordsets in one single file (0-based)
	 * @param logRecordsetBaseName the base name without recordset number
	 * @param logDeviceName
	 * @param logStartTimestamp_ms of the log or recordset
	 * @param logChannelNumber may differ from UI settings in case of channel mix
	 * @param logObjectKey may differ from UI settings (empty in OSD files, validated parent path for bin files)
	 */
	public VaultCollector(Analyzer analyzer, String objectDirectory, Path sourcePath, int fileVersion, int logRecordSetSize, int logRecordSetOrdinal,
			String logRecordsetBaseName, String logDeviceName, long logStartTimestamp_ms, int logChannelNumber, String logObjectKey) {
		this(objectDirectory, sourcePath, fileVersion, logRecordSetSize, logRecordSetOrdinal, logRecordsetBaseName, logDeviceName, analyzer, //
				logStartTimestamp_ms, logChannelNumber, objectDirectory, false);
	}

	private VaultCollector(String objectDirectory, Path sourcePath, int fileVersion, int logRecordSetSize, int logRecordSetOrdinal,
			String logRecordsetBaseName, String logDeviceName, Analyzer analyzer, //
			long logStartTimestamp_ms, int logChannelNumber, String logObjectKey, boolean providesReaderSettings) {
		this.analyzer = analyzer;
		String readerSettings = providesReaderSettings && analyzer.getActiveDevice() instanceof IHistoDevice
				? ((IHistoDevice) analyzer.getActiveDevice()).getReaderSettingsCsv() : GDE.STRING_EMPTY;
		DataAccess dataAccess = analyzer.getDataAccess();
		this.vault = new ExtendedVault(objectDirectory, sourcePath, dataAccess.getSourceLastModified(sourcePath), dataAccess.getSourceLength(sourcePath),
				fileVersion, logRecordSetSize, logRecordSetOrdinal, logRecordsetBaseName, logDeviceName, analyzer, //
				logStartTimestamp_ms, logChannelNumber, logObjectKey, readerSettings);
	}

	@Override
	public String toString() {
		return String.format("logChannelNumber=%d  logRecordSetOrdinal=%d  logObjectKey=%s  startTimestamp=%s  %s", vault.getLogChannelNumber(), vault.getLogRecordSetOrdinal(), vault.getLogObjectKey(), vault.getStartTimeStampFormatted(), vault.getLoadFilePath());
	}

	/**
	 * Make a full vault from the truss.
	 * @param recordSet
	 * @param scorePoints
	 */
	public void promoteTruss(RecordSet recordSet, Integer[] scorePoints) {
		if (recordSet.getRecordDataSize(true) > 0) {
			this.vault.logStartTimestamp_ms = recordSet.getStartTimeStamp();

			boolean isSampled = scorePoints[ScoreLabelTypes.TOTAL_READINGS.ordinal()] != null && scorePoints[ScoreLabelTypes.TOTAL_READINGS.ordinal()] > recordSet.getRecordDataSize(true);
			setMeasurementPoints(recordSet, isSampled);

			{
				GroupTransitions transitions = new TransitionCollector(recordSet).defineTransitions(this.vault.logChannelNumber);
				SettlementRecords histoSettlements = determineSettlements(recordSet, transitions);
				setSettlements(histoSettlements);
			}

			scorePoints[ScoreLabelTypes.DURATION_MM.ordinal()] = (int) (recordSet.getMaxTime_ms() / 60000. * 1000. + .5);
			scorePoints[ScoreLabelTypes.AVERAGE_TIME_STEP_MS.ordinal()] = (int) (recordSet.getAverageTimeStep_ms() * 1000.);
			scorePoints[ScoreLabelTypes.MAXIMUM_TIME_STEP_MS.ordinal()] = (int) (recordSet.getMaximumTimeStep_ms() * 1000.);
			scorePoints[ScoreLabelTypes.MINIMUM_TIME_STEP_MS.ordinal()] = (int) (recordSet.getMinimumTimeStep_ms() * 1000.);
			scorePoints[ScoreLabelTypes.SIGMA_TIME_STEP_MS.ordinal()] = (int) (recordSet.getSigmaTimeStep_ms() * 1000.);
			scorePoints[ScoreLabelTypes.SAMPLED_READINGS.ordinal()] = recordSet.getRecordDataSize(true);

			setScorePoints(scorePoints);
		}
	}

	/**
	 * Add the measurements points from the records to the vault.
	 * @param recordSet
	 * @param isSampled true if the recordset does not hold all measurement points
	 */
	private void setMeasurementPoints(RecordSet recordSet, boolean isSampled) {
		List<MeasurementType> channelMeasurements = analyzer.getActiveDevice().getChannelMeasuremts(this.vault.getLogChannelNumber());
		for (int i = 0; i < recordSet.getRecordNames().length; i++) {
			MeasurementType measurementType = channelMeasurements.get(i);
			Record record = recordSet.get(recordSet.getRecordNames()[i]);

			CompartmentType entryPoints = new CompartmentType(i, measurementType.getName(), DataTypes.fromDataType(record.getDataType()));
			this.vault.getMeasurements().put(i, entryPoints);
			entryPoints.setTrails(new HashMap<Integer, PointType>());

			if (!record.hasReasonableData()) {
				log.fine(() -> String.format("no reasonable data for measurementType.getName()=%s %s", measurementType.getName(), this.vault.getLoadFilePath()));
			} else {
				if (measurementType.getStatistics() != null) { // this creates trail types mismatch : && record.hasReasonableData()) {
					setTrailStatisticsPoints(entryPoints, record, recordSet);
				}
				setTrailPoints(entryPoints, record, isSampled);
			}
			log.finer(() -> record.getName() + " data " + entryPoints);
		}
	}

	/**
	 * @param recordSet
	 * @param transitions
	 * @return the calculated settlements calculated from transitions (key is the name of the settlementType)
	 */
	private SettlementRecords determineSettlements(RecordSet recordSet, GroupTransitions transitions) {
		SettlementRecords histoSettlements = new SettlementRecords();

		for (SettlementType settlementType : analyzer.getActiveDevice().getDeviceConfiguration().getChannel(this.vault.logChannelNumber).getSettlements().values()) {
			if (settlementType.getEvaluation() != null) {
				SettlementRecord record = new SettlementRecord(settlementType, recordSet, this.vault.logChannelNumber);
				histoSettlements.put(settlementType.getName(), record);
				if (transitions.isEmpty()) continue;

				final TransitionFigureType transitionFigureType = settlementType.getEvaluation().getTransitionFigure();
				final TransitionAmountType transitionAmountType = settlementType.getEvaluation().getTransitionAmount();
				final TransitionCalculusType calculationType = settlementType.getEvaluation().getTransitionCalculus();
				if (transitionFigureType != null) {
					FigureEvaluator evaluator = new FigureEvaluator(record);
					for (Transition transition : transitions.get(transitionFigureType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
				} else if (transitionAmountType != null) {
					AmountEvaluator evaluator = new AmountEvaluator(record, analyzer);
					for (Transition transition : transitions.get(transitionAmountType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
				} else if (calculationType != null) {
					CalculusEvaluator evaluator = new CalculusEvaluator(record, analyzer);
					for (Transition transition : transitions.get(calculationType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
				} else {
					throw new UnsupportedOperationException();
				}
			}
		}
		return histoSettlements;
	}

	/**
	 * Set the list of point values for trail types marked as 'real'.
	 * @param entryPoints is the target object
	 * @param record
	 * @param recordSet
	 */
	private void setTrailStatisticsPoints(CompartmentType entryPoints, Record record, RecordSet recordSet) {
		IDevice device = analyzer.getActiveDevice();
		StatisticsType measurementStatistics = device.getChannelMeasuremts(this.vault.getLogChannelNumber()).get(record.getOrdinal()).getStatistics();

		int triggerRefOrdinal = -1;
		if (measurementStatistics == null) {//corrupt file, probably OSD included, need to be sorted out
			log.log(Level.SEVERE, "===>> check corrupt file related to: " + recordSet.getDescription());
		}
		if (measurementStatistics.getTriggerRefOrdinal() != null) {
			int tmpOrdinal = measurementStatistics.getTriggerRefOrdinal().intValue();
			// if (record.isDisplayable()) { the record may be displayable later --- but note that calculating trigger ranges requires displayable true
			triggerRefOrdinal = tmpOrdinal;
			// }
		}
		boolean isTriggerLevel = measurementStatistics.getTrigger() != null;

		if (measurementStatistics.isAvg()) {
			if (isTriggerLevel) {
				int dd = record.getAvgValueTriggered() != Integer.MIN_VALUE ? record.getAvgValueTriggered() : 0;
				entryPoints.addPoint(TrailTypes.REAL_AVG, transmuteValue(record, dd));
			} else if (triggerRefOrdinal < 0) {
				entryPoints.addPoint(TrailTypes.REAL_AVG, transmuteValue(record, record.getAvgValue()));
			} else if (record.getAvgValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) {
				entryPoints.addPoint(TrailTypes.REAL_AVG, transmuteValue(record, record.getAvgValueTriggered(triggerRefOrdinal)));
			}
		} else {
			entryPoints.addPoint(TrailTypes.REAL_AVG, transmuteValue(record, record.getAvgValue()));
		}

		if (measurementStatistics.isMax()) {
			if (isTriggerLevel) {
				entryPoints.addPoint(TrailTypes.REAL_MAX, transmuteValue(record, record.getMaxValueTriggered()));
			} else if (triggerRefOrdinal < 0) {
				entryPoints.addPoint(TrailTypes.REAL_MAX, transmuteValue(record, record.getRealMaxValue()));
			} else if (record.getMaxValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) {
				entryPoints.addPoint(TrailTypes.REAL_MAX, transmuteValue(record, record.getMaxValueTriggered(triggerRefOrdinal)));
			}
		} else {
			entryPoints.addPoint(TrailTypes.REAL_MAX, transmuteValue(record, record.getRealMaxValue()));
		}

		if (measurementStatistics.isMin()) {
			if (isTriggerLevel) {
				entryPoints.addPoint(TrailTypes.REAL_MIN, transmuteValue(record, record.getMinValueTriggered()));
			} else if (triggerRefOrdinal < 0) {
				entryPoints.addPoint(TrailTypes.REAL_MIN, transmuteValue(record, record.getRealMinValue()));
			} else if (record.getMinValueTriggered(triggerRefOrdinal) != Integer.MAX_VALUE) {
				entryPoints.addPoint(TrailTypes.REAL_MIN, transmuteValue(record, record.getMinValueTriggered(triggerRefOrdinal)));
			}
		} else {
			entryPoints.addPoint(TrailTypes.REAL_MIN, transmuteValue(record, record.getRealMinValue()));
		}

		if (measurementStatistics.isSigma()) {
			if (isTriggerLevel) {
				entryPoints.addPoint(TrailTypes.REAL_SD, transmuteDelta(record, record.getSigmaValueTriggered()));
			} else if (triggerRefOrdinal < 0) {
				entryPoints.addPoint(TrailTypes.REAL_SD, transmuteDelta(record, record.getSigmaValue()));
			} else if (record.getSigmaValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) {
				entryPoints.addPoint(TrailTypes.REAL_SD, transmuteDelta(record, record.getSigmaValueTriggered(triggerRefOrdinal)));
			}
		} else {
			entryPoints.addPoint(TrailTypes.REAL_SD, transmuteDelta(record, record.getSigmaValue()));
		}

		Integer refOrdinal = measurementStatistics.getSumByTriggerRefOrdinal();
		Integer refSecondaryOrdinal = measurementStatistics.getSumBySecondaryTriggerRefOrdinal();
		if (refOrdinal != null) {
			int referencedSumTriggeredRange = refSecondaryOrdinal != null ? record.getSumTriggeredRange(refOrdinal, refSecondaryOrdinal) : record.getSumTriggeredRange(refOrdinal);
			double summarizedValue = device.translateDeltaValue(record, referencedSumTriggeredRange / 1000.);
			log.finer(() -> String.format("%s -> summarizedValue = %d", record.getName(), (int) summarizedValue));
			if (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1 && summarizedValue > 0.) {
				// Warning: Sum values do not sum correctly in case of offset != 0. Reason is summing up the offset multiple times.
				if (isTriggerLevel) {
					entryPoints.addPoint(TrailTypes.REAL_SUM_TRIGGERED, transmuteDelta(record, record.getSumTriggeredRange()));
				} else {
					entryPoints.addPoint(TrailTypes.REAL_SUM_TRIGGERED, transmuteDelta(record, referencedSumTriggeredRange));
				}
			}

			if (summarizedValue > 0.) { // while summarized value is zero it does not make sense to calculate ratio
				Integer ratioRefOrdinal = measurementStatistics.getRatioRefOrdinal();
				if (ratioRefOrdinal != null && measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1) {
					Record referencedRecord = recordSet.get(ratioRefOrdinal.intValue());
					if (referencedRecord != null) {
						final double summarizedReferencedValue = device.translateDeltaValue(referencedRecord, referencedRecord.getSumTriggeredRange(refOrdinal) / 1000.0);
						log.finer(() -> String.format("summarizedReferencedValue = %d, ratioRefOrdinal = %d", (int) summarizedReferencedValue, ratioRefOrdinal));
						if (summarizedReferencedValue > 0.) {
							double ratio = summarizedReferencedValue / summarizedValue;
							// multiply by 1000 -> all ratios are internally stored multiplied by thousand
							entryPoints.addPoint(TrailTypes.REAL_MAX_RATIO_TRIGGERED, transmuteScalar(record, (int) (ratio * 1000.)));
						}
					}
				}
			}
		}

		String sumTriggerTimeText = measurementStatistics.getSumTriggerTimeText();
		if (measurementStatistics.getTrigger() != null && sumTriggerTimeText != null && sumTriggerTimeText.length() > 1) {
			// omitting the multiplication by 1000 results in seconds
			entryPoints.addPoint(TrailTypes.REAL_TIME_SUM_TRIGGERED, transmuteScalar(record, record.getTimeSumTriggeredRange_ms()));
		}
		if (measurementStatistics.isCountByTrigger() != null) {
			int countValue = record.getTriggerRanges() != null ? record.getTriggerRanges().size() : 0;
			// all counters are internally stored multiplied by thousand
			entryPoints.addPoint(TrailTypes.REAL_COUNT_TRIGGERED, transmuteScalar(record, countValue * 1000));
		}
	}

	/**
	 * Provides an encoded histo vault value which can be decoded by applying the factor, offset and reduction.
	 * @return a record value (point) transformed into the device independent histo vault value (point)
	 */
	private int transmuteValue(Record record, int value) {
		return encodeValue(record.getChannelItem(), analyzer.getActiveDevice().translateValue(record, value / 1000.));
	}

	/**
	 * Provides an encoded histo vault value which can be decoded by applying the factor, offset and reduction.
	 * @return a record delta value transformed into the device independent histo vault value
	 */
	private int transmuteDelta(Record record, int deltaValue) {
		return encodeValue(record.getChannelItem(), analyzer.getActiveDevice().translateDeltaValue(record, deltaValue / 1000.));
	}

	/**
	 * Provides an encoded histo vault value which can be decoded by applying the factor, offset and reduction.
	 * @return a scalar (e.g. ratio) transformed into the device independent histo vault value
	 */
	private int transmuteScalar(Record record, int scalar) {
		return encodeValue(record.getChannelItem(), scalar / 1000.);
	}

	/**
	 * Set the list of point values for standard trail types.
	 * @param entryPoints is the target object
	 * @param record
	 * @param isSampled true indicates that the record values are a sample from the original values
	 */
	private void setTrailPoints(CompartmentType entryPoints, Record record, boolean isSampled) {
		IChannelItem channelItem = record.getChannelItem();

		Function<Double, Integer> encoder = value -> encodeValue(channelItem, value);

		UniversalQuantile<? extends Number> quantile;
		final int rawMax, rawBitwiseOr;
		if (channelItem.isBits()) {
			UniversalQuantile<? extends Number> rawQuantile = Coding.BITS.toValueQuantile(record, analyzer.getSettings());
			rawMax = (int) Math.round(rawQuantile.getQuartile4());
			rawBitwiseOr = rawQuantile.getOrFigure();
			log.finer(() -> "isBits " + rawQuantile.getQuartile4() + "   " + rawQuantile.getOrFigure());
			quantile = Coding.BITS.toIndexQuantile(record, analyzer.getSettings());
		} else if (channelItem.isTokens()) {
			UniversalQuantile<? extends Number> rawQuantile = Coding.TOKENS.toValueQuantile(record, analyzer.getSettings());
			rawMax = (int) Math.round(rawQuantile.getQuartile4());
			rawBitwiseOr = rawQuantile.getOrFigure();
			log.finer(() -> "isToken " + rawQuantile.getQuartile4() + "   " + rawQuantile.getOrFigure());
			quantile = Coding.TOKENS.toIndexQuantile(record, analyzer.getSettings());
		} else {
			rawMax = rawBitwiseOr = 0;
			quantile = Coding.POINT.toValueQuantile(record, analyzer.getSettings());
			if (!quantile.getOutliers().isEmpty()) {
				IntStream outliers = quantile.getOutliers().stream().distinct().mapToInt(v -> encodeValue(channelItem, v.doubleValue()));
				String outliersCsv = outliers.boxed().map(String::valueOf).collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR));
				entryPoints.setOutlierPoints(outliersCsv);
			}
			if (!quantile.getConstantScraps().isEmpty()) {
				IntStream scraps = quantile.getConstantScraps().stream().distinct().mapToInt(v -> encoder.apply(v.doubleValue()));
				String scrapsCsv = scraps.boxed().map(String::valueOf).collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR));
				entryPoints.setScrappedPoints(scrapsCsv);
			}
		}
		// raw points without multiplying by 1000 in order to not loose the bits 22 to 31
		entryPoints.addPoint(TrailTypes.RAW_BITS, rawBitwiseOr);
		entryPoints.addPoint(TrailTypes.RAW_MAX, rawMax);

		entryPoints.addPoint(TrailTypes.REAL_FIRST, transmuteValue(record, record.elementAt(0)));
		entryPoints.addPoint(TrailTypes.REAL_LAST, transmuteValue(record, record.elementAt(record.realSize() - 1)));

		entryPoints.addPoint(TrailTypes.AVG, encoder.apply(quantile.getAvgFigure()));
		entryPoints.addPoint(TrailTypes.MAX, encoder.apply(quantile.getPopulationMaxFigure()));
		entryPoints.addPoint(TrailTypes.MIN, encoder.apply(quantile.getPopulationMinFigure()));
		entryPoints.addPoint(TrailTypes.SD, encoder.apply(quantile.getSigmaFigure()));
		entryPoints.addPoint(TrailTypes.Q0, encoder.apply(quantile.getQuartile0()));
		entryPoints.addPoint(TrailTypes.Q1, encoder.apply(quantile.getQuartile1()));
		entryPoints.addPoint(TrailTypes.Q2, encoder.apply(quantile.getQuartile2()));
		entryPoints.addPoint(TrailTypes.Q3, encoder.apply(quantile.getQuartile3()));
		entryPoints.addPoint(TrailTypes.Q4, encoder.apply(quantile.getQuartile4()));
		entryPoints.addPoint(TrailTypes.Q_25_PERMILLE, encoder.apply(quantile.getQuantile(.025)));
		entryPoints.addPoint(TrailTypes.Q_975_PERMILLE, encoder.apply(quantile.getQuantile(.975)));
		entryPoints.addPoint(TrailTypes.Q_LOWER_WHISKER, encoder.apply(quantile.getQuantileLowerWhisker()));
		entryPoints.addPoint(TrailTypes.Q_UPPER_WHISKER, encoder.apply(quantile.getQuantileUpperWhisker()));

		entryPoints.addPoint(TrailTypes.FIRST, encoder.apply(quantile.getFirstFigure()));
		entryPoints.addPoint(TrailTypes.LAST, encoder.apply(quantile.getLastFigure()));
		// trigger trail types sum are not supported for measurements
		entryPoints.addPoint(TrailTypes.SUM, 0);
		entryPoints.addPoint(TrailTypes.COUNT, encoder.apply((double) quantile.getSize()));

		// all counters are internally stored multiplied by thousand
		entryPoints.addPoint(TrailTypes.REAL_COUNT, encoder.apply((double) record.realSize()));
	}

	/**
	 * Add the settlements points from the histo settlements to the vault.
	 * @param histoSettlements
	 */
	private void setSettlements(SettlementRecords histoSettlements) {
		for (Entry<String, SettlementRecord> entry : histoSettlements.entrySet()) {
			SettlementRecord record = entry.getValue();
			SettlementType settlementType = record.getSettlement();
			CompartmentType entryPoints = new CompartmentType(settlementType.getSettlementId(), settlementType.getName(), DataTypes.DEFAULT);
			this.vault.getSettlements().put(settlementType.getSettlementId(), entryPoints);
			entryPoints.setTrails(new HashMap<Integer, PointType>());

			if (record.realSize() != 0 && record.hasReasonableData()) {
				entryPoints.addPoint(TrailTypes.REAL_FIRST, record.elementAt(0));
				entryPoints.addPoint(TrailTypes.REAL_LAST, record.elementAt(record.realSize() - 1));

				Function<Double, Integer> encoder = value -> encodeValue(settlementType, value);

				UniversalQuantile<? extends Number> quantile;
				if (settlementType.isBits()) {
					quantile = Coding.BITS.toIndexQuantile(record, analyzer.getSettings());
				} else if (settlementType.isTokens()) {
					quantile = Coding.TOKENS.toIndexQuantile(record, analyzer.getSettings());
				} else {
					quantile = Coding.POINT.toValueQuantile(record, analyzer.getSettings());
					if (!quantile.getOutliers().isEmpty()) {
						IntStream outliers = quantile.getOutliers().stream().distinct().mapToInt(v -> encoder.apply(v.doubleValue()));
						String outliersCsv = outliers.boxed().map(String::valueOf).collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR));
						entryPoints.setOutlierPoints(outliersCsv);
					}
					if (!quantile.getConstantScraps().isEmpty()) {
						IntStream scraps = quantile.getConstantScraps().stream().distinct().mapToInt(v -> encoder.apply(v.doubleValue()));
						String scrapsCsv = scraps.boxed().map(String::valueOf).collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR));
						entryPoints.setScrappedPoints(scrapsCsv);
					}
				}

				entryPoints.addPoint(TrailTypes.RAW_BITS, quantile.getOrFigure());

				entryPoints.addPoint(TrailTypes.REAL_AVG, encoder.apply(quantile.getAvgFigure()));
				entryPoints.addPoint(TrailTypes.REAL_MAX, encoder.apply(quantile.getPopulationMaxFigure()));
				entryPoints.addPoint(TrailTypes.REAL_MIN, encoder.apply(quantile.getPopulationMinFigure()));
				entryPoints.addPoint(TrailTypes.REAL_SD, encoder.apply(quantile.getSigmaFigure()));
				entryPoints.addPoint(TrailTypes.REAL_SUM, encoder.apply(quantile.getSumFigure()));
				entryPoints.addPoint(TrailTypes.REAL_COUNT, encoder.apply((double) quantile.getSize()));

				entryPoints.addPoint(TrailTypes.AVG, encoder.apply(quantile.getAvgFigure()));
				entryPoints.addPoint(TrailTypes.MAX, encoder.apply(quantile.getPopulationMaxFigure()));
				entryPoints.addPoint(TrailTypes.MIN, encoder.apply(quantile.getPopulationMinFigure()));
				entryPoints.addPoint(TrailTypes.SD, encoder.apply(quantile.getSigmaFigure()));
				entryPoints.addPoint(TrailTypes.Q0, encoder.apply(quantile.getQuartile0()));
				entryPoints.addPoint(TrailTypes.Q1, encoder.apply(quantile.getQuartile1()));
				entryPoints.addPoint(TrailTypes.Q2, encoder.apply(quantile.getQuartile2()));
				entryPoints.addPoint(TrailTypes.Q3, encoder.apply(quantile.getQuartile3()));
				entryPoints.addPoint(TrailTypes.Q4, encoder.apply(quantile.getQuartile4()));
				entryPoints.addPoint(TrailTypes.Q_25_PERMILLE, encoder.apply(quantile.getQuantile(.025)));
				entryPoints.addPoint(TrailTypes.Q_975_PERMILLE, encoder.apply(quantile.getQuantile(.975)));
				entryPoints.addPoint(TrailTypes.Q_LOWER_WHISKER, encoder.apply(quantile.getQuantileLowerWhisker()));
				entryPoints.addPoint(TrailTypes.Q_UPPER_WHISKER, encoder.apply(quantile.getQuantileUpperWhisker()));

				entryPoints.addPoint(TrailTypes.FIRST, encoder.apply(quantile.getFirstFigure()));
				entryPoints.addPoint(TrailTypes.LAST, encoder.apply(quantile.getLastFigure()));
				entryPoints.addPoint(TrailTypes.SUM, encoder.apply(quantile.getSumFigure()));
				entryPoints.addPoint(TrailTypes.COUNT, encoder.apply((double) quantile.getSize()));
			}
			log.finer(() -> record.getName() + " data " + this.vault.getSettlements());
		}
	}

	/**
	 * Add the score points to the vault.
	 * @param scorePoints
	 */
	private void setScorePoints(Integer[] scorePoints) {

		for (ScoreLabelTypes scoreLabelTypes : EnumSet.allOf(ScoreLabelTypes.class)) {
			Integer scoreValue = scorePoints[scoreLabelTypes.ordinal()];
			if (scoreValue != null) {
				PointType pointType = new PointType(scoreLabelTypes.ordinal(), scoreLabelTypes.toString(), scoreValue);
				this.vault.getScores().put(scoreLabelTypes.ordinal(), pointType);
			}
		}
		log.fine(() -> "scores " + this.vault.getScores());
	}

	private int encodeValue(IChannelItem channelItem, double value) {
		return (int) (HistoSet.encodeVaultValue(channelItem, value) * 1000.);
	}

	public ExtendedVault getVault() {
		return this.vault;
	}

	public void setSourceDataSet(SourceDataSet sourceDataSet) {
		this.sourceDataSet = sourceDataSet;
	}

	public SourceDataSet getSourceDataSet() {
		return this.sourceDataSet;
	}

}
