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

    Copyright (c) 2018,2019 Thomas Eickert
****************************************************************************************/

package gde.histo.guard;

import static gde.histo.utils.ElementaryQuantile.BoxplotItems.LOWER_WHISKER;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.LQT;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE1;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE3;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.UPPER_WHISKER;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.UQT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gde.config.Settings;
import gde.device.IChannelItem;
import gde.device.ScoreGroupType;
import gde.device.ScoreType;
import gde.device.TrailTypes;
import gde.histo.cache.HistoVault;
import gde.histo.datasources.HistoSet;
import gde.histo.guard.Reminder.ReminderType;
import gde.histo.recordings.TrailSelector;
import gde.histo.utils.ElementaryQuantile;
import gde.histo.utils.UniversalQuantile;
import gde.log.Level;
import gde.log.Logger;

/**
 * Create reminders from vaults.
 * Supports measurements, settlements and scores.
 * @author Thomas Eickert (USER)
 */
public final class Guardian {
	private static final String	$CLASS_NAME	= Guardian.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Determine the reminders of the category which is the most significant.
	 * This means: The result holds far outliers OR close outliers in case no far outliers are present.
	 * @param indexedVaults in start timestamp reverse order (for reminderCount)
	 * @param logLimit is the maximum number of the most recent logs which is checked for reminders
	 * @return the array of reminder objects which may hold null values
	 */
	public static Reminder[] defineMinMaxReminder(HistoVault[] indexedVaults, IChannelItem channelItem, TrailSelector trailSelector, int logLimit, Settings settings) {
		double[][] minMaxQuantiles = Guardian.defineExtremumQuantiles(indexedVaults, channelItem, trailSelector.getExtremumTrailsOrdinals(), settings);
		if (minMaxQuantiles.length == 0) return new Reminder[] { null, null };

		int[] extremumIndices = trailSelector.getExtremumTrailsIndices();
		String[] extremumText = trailSelector.getExtremumTrailsTexts();

		int reminderLevel = settings.getReminderLevel();
		if (reminderLevel == -1) return new Reminder[] { null, null };
		Reminder minReminder = null;
		Reminder maxReminder = null;
		double minWhiskerLimit = minMaxQuantiles[0][QUARTILE1.ordinal()];
		double maxWhiskerLimit = minMaxQuantiles[1][QUARTILE3.ordinal()];
		double closeMinOutlierLimit = minMaxQuantiles[0][LOWER_WHISKER.ordinal()];
		double closeMaxOutlierLimit = minMaxQuantiles[1][UPPER_WHISKER.ordinal()];
		double farMinOutlierLimit = minMaxQuantiles[0][QUARTILE1.ordinal()] - 3. * 2. * minMaxQuantiles[0][LQT.ordinal()];
		double farMaxOutlierLimit = minMaxQuantiles[1][QUARTILE3.ordinal()] + 3. * 2. * minMaxQuantiles[1][UQT.ordinal()];

		int actualLimit = logLimit >= 0 && logLimit < indexedVaults.length ? logLimit : indexedVaults.length;
		for (int i = 0; i < actualLimit; i++) {
			HistoVault vault = indexedVaults[i];
			Integer[] minMaxTrailPoints = Guardian.getExtremumTrailPoints(vault, channelItem, trailSelector);
			if (minMaxTrailPoints[0] == null || minMaxTrailPoints[1] == null) continue;

			double tmpMinValue = HistoSet.decodeVaultValue(channelItem, minMaxTrailPoints[0] / 1000.0);
			if (HistoSet.fuzzyCompare(tmpMinValue, farMinOutlierLimit) < 0) {
				if (minReminder == null || minReminder.getReminderType() == ReminderType.CLOSE || minReminder.getReminderType() == ReminderType.WHISKER) {
					// discard lower category reminders
					minReminder = new Reminder(ReminderType.FAR, farMinOutlierLimit, closeMinOutlierLimit, extremumIndices[0], extremumText[0]);
				}
				minReminder.add(tmpMinValue, i);
			} else if (ReminderType.CLOSE.isIncluded(reminderLevel) && HistoSet.fuzzyCompare(tmpMinValue, closeMinOutlierLimit) < 0) {
				if (minReminder == null || minReminder.getReminderType() == ReminderType.WHISKER) {
					minReminder = new Reminder(ReminderType.CLOSE, farMinOutlierLimit, closeMinOutlierLimit, extremumIndices[0], extremumText[0]);
					minReminder.add(tmpMinValue, i);
				} else if (minReminder.getReminderType() == ReminderType.CLOSE) {
					minReminder.add(tmpMinValue, i);
				} else {
					; // discard close reminder if far reminders are already present
				}
			} else if (ReminderType.WHISKER.isIncluded(reminderLevel) && HistoSet.fuzzyCompare(tmpMinValue, minWhiskerLimit) < 0) {
				if (minReminder == null) {
					minReminder = new Reminder(ReminderType.WHISKER, farMinOutlierLimit, closeMinOutlierLimit, extremumIndices[0], extremumText[0]);
					minReminder.add(tmpMinValue, i);
				} else if (minReminder.getReminderType() == ReminderType.WHISKER) {
					minReminder.add(tmpMinValue, i);
				} else {
					; // discard reminder if far or close reminders are already present
				}
			}
			double tmpMaxValue = HistoSet.decodeVaultValue(channelItem, minMaxTrailPoints[1] / 1000.);
			if (HistoSet.fuzzyCompare(tmpMaxValue, farMaxOutlierLimit) > 0) {
				if (maxReminder == null || maxReminder.getReminderType() == ReminderType.CLOSE || maxReminder.getReminderType() == ReminderType.WHISKER) {
					maxReminder = new Reminder(ReminderType.FAR, farMaxOutlierLimit, closeMaxOutlierLimit, extremumIndices[1], extremumText[1]);
				}
				maxReminder.add(tmpMaxValue, i);
			} else if (ReminderType.CLOSE.isIncluded(reminderLevel) && HistoSet.fuzzyCompare(tmpMaxValue, closeMaxOutlierLimit) > 0) {
				if (maxReminder == null || maxReminder.getReminderType() == ReminderType.WHISKER) {
					maxReminder = new Reminder(ReminderType.CLOSE, farMaxOutlierLimit, closeMaxOutlierLimit, extremumIndices[1], extremumText[1]);
					maxReminder.add(tmpMaxValue, i);
				} else if (maxReminder.getReminderType() == ReminderType.CLOSE) {
					maxReminder.add(tmpMaxValue, i);
				}
			} else if (ReminderType.WHISKER.isIncluded(reminderLevel) && HistoSet.fuzzyCompare(tmpMaxValue, maxWhiskerLimit) > 0) {
				if (maxReminder == null) {
					maxReminder = new Reminder(ReminderType.WHISKER, farMaxOutlierLimit, closeMaxOutlierLimit, extremumIndices[1], extremumText[1]);
					maxReminder.add(tmpMaxValue, i);
				} else if (maxReminder.getReminderType() == ReminderType.WHISKER) {
					maxReminder.add(tmpMaxValue, i);
				}
			}
		}
		Reminder[] result = new Reminder[] { minReminder, maxReminder };
		return result;
	}

	/**
	 * @return the extended tukey tolerance arrays for the min/max trails or for score groups w/o min/max scores take the first score
	 */
	public static double[][] defineExtremumQuantiles(HistoVault[] vaults, IChannelItem channelItem, int[] extremumOrdinals, Settings settings) {
		List<Double> decodedMinimums = new ArrayList<>();
		List<Double> decodedMaximums = new ArrayList<>();
		for (HistoVault v : vaults) {
			Integer point = channelItem.getVaultPoint(v, extremumOrdinals[0]);
			if (point == null) continue;
			decodedMinimums.add(HistoSet.decodeVaultValue(channelItem, point / 1000.));
			point = channelItem.getVaultPoint(v, extremumOrdinals[1]);
			if (point == null) continue;
			decodedMaximums.add(HistoSet.decodeVaultValue(channelItem, point / 1000.));
		}
		ElementaryQuantile<Double> minQuantile = new ElementaryQuantile<>(decodedMinimums, true, settings);
		ElementaryQuantile<Double> maxQuantile = new ElementaryQuantile<>(decodedMaximums, true, settings);

		double[][] result;
		if (!decodedMinimums.isEmpty() && !decodedMaximums.isEmpty()) {
			result = new double[][] { minQuantile.getTukeyWithQuartileTolerances(), maxQuantile.getTukeyWithQuartileTolerances() };
		} else {
			result = new double[0][0];
		}
		return result;
	}

	/**
	 * @return the min/maxValues from the most recent logs
	 */
	public static double[] defineStandardMinMax(Stream<HistoVault> vaults, IChannelItem channelItem) {
		double[] decodedMinMaxValues = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE };
		vaults.forEach(v -> {
			Integer tmpPoint;
			if ((tmpPoint = channelItem.getVaultPoint(v, TrailTypes.MIN.ordinal())) != null)
				decodedMinMaxValues[0] = Math.min(decodedMinMaxValues[0], HistoSet.decodeVaultValue(channelItem, tmpPoint / 1000.));
			if ((tmpPoint = channelItem.getVaultPoint(v, TrailTypes.MAX.ordinal())) != null)
				decodedMinMaxValues[1] = Math.max(decodedMinMaxValues[1], HistoSet.decodeVaultValue(channelItem, tmpPoint / 1000.));
		});
		double[] result;
		if (decodedMinMaxValues[0] == Double.MAX_VALUE || decodedMinMaxValues[1] == -Double.MAX_VALUE) {
			result = new double[] { 0., 0. };
		} else {
			result = decodedMinMaxValues;
		}
		return result;
	}

	/**
	 * @return the lower/upper values based on q0/q4
	 */
	public static double[] defineStandardExtrema(List<HistoVault> vaults, IChannelItem channelItem, Settings settings) {
		List<Double> decodedMinValues = new ArrayList<>();
		List<Double> decodedLowValues = new ArrayList<>();
		List<Double> decodedHighValues = new ArrayList<>();
		List<Double> decodedMaxValues = new ArrayList<>();
		for (HistoVault v : vaults) {
			Integer tmpPoint;
			if ((tmpPoint = channelItem.getVaultPoint(v, TrailTypes.MIN.ordinal())) != null)
				decodedMinValues.add(HistoSet.decodeVaultValue(channelItem, tmpPoint / 1000.));
			if ((tmpPoint = channelItem.getVaultPoint(v, TrailTypes.Q0.ordinal())) != null)
				decodedLowValues.add(HistoSet.decodeVaultValue(channelItem, tmpPoint / 1000.));
			if ((tmpPoint = channelItem.getVaultPoint(v, TrailTypes.Q4.ordinal())) != null)
				decodedHighValues.add(HistoSet.decodeVaultValue(channelItem, tmpPoint / 1000.));
			if ((tmpPoint = channelItem.getVaultPoint(v, TrailTypes.MAX.ordinal())) != null)
				decodedMaxValues.add(HistoSet.decodeVaultValue(channelItem, tmpPoint / 1000.));
		}
		;
		double[] result;
		if (decodedLowValues.isEmpty() || decodedHighValues.isEmpty()) {
			result = new double[] { 0., 0. };
		} else {
			result = getExtrema(channelItem.getName(), decodedLowValues, decodedHighValues, settings);

			// corrections in cases when the whiskers are not within the scale
			double lowerWhisker = new ElementaryQuantile<>(decodedMinValues, true, settings).getQuantileLowerWhisker();
			if (lowerWhisker < result[0]) {
				result[0] = Math.min(lowerWhisker, result[0]);
				log.log(Level.FINER, "lower corrected to ", lowerWhisker);
			}
			double upperWhisker = new ElementaryQuantile<>(decodedMaxValues, true, settings).getQuantileUpperWhisker();
			if (upperWhisker > result[1]) {
				result[1] = Math.max(upperWhisker, result[1]);
				log.log(Level.FINER, "upper corrected to ", upperWhisker);
			}
		}
		return result;
	}

	/**
	 * @return the lower/upper values from the most recent logs for trails with a different number range than the measurement values
	 */
	public static double[] defineAlienMinMax(Stream<HistoVault> vaults, IChannelItem channelItem, int trailOrdinal) {
		Stream<Integer> alienPoints = vaults.map(v -> channelItem.getVaultPoint(v, trailOrdinal));
		DoubleSummaryStatistics decodedAliens = alienPoints.filter(Objects::nonNull).map(i -> HistoSet.decodeVaultValue(channelItem, i / 1000.))//
				.collect(Collectors.summarizingDouble((Double::doubleValue)));
		double[] result;
		if (decodedAliens.getCount() == 0) {
			result = new double[] { 0., 0. };
		} else {
			result = new double[] { decodedAliens.getMin(), decodedAliens.getMax() };
		}
		return result;
	}

	/**
	 * @return the lower/upper values for trails with a different number range than the measurement values (e.g. SD, counters)
	 */
	public static double[] defineAlienExtrema(List<HistoVault> vaults, IChannelItem channelItem, int trailOrdinal, Settings settings) {
		Stream<Integer> alienPoints = vaults.stream().map(v -> channelItem.getVaultPoint(v, trailOrdinal));
		List<Double> decodedAliens = alienPoints.filter(Objects::nonNull).map(i -> HistoSet.decodeVaultValue(channelItem, i / 1000.)).collect(Collectors.toList());
		double[] result;
		if (decodedAliens.isEmpty()) {
			result = new double[] { 0., 0. };
		} else {
			result = getExtrema(channelItem.getName(), decodedAliens, decodedAliens, settings);
		}
		return result;
	}

	/**
	 * @return the min/maxValues from the most recent logs and all scoregroup members
	 */
	public static double[] defineScoreMinMax(Stream<HistoVault> vaults, ScoreGroupType scoregroup) {
		double[] decodedMinMaxValues = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE };

		List<Integer> scoreOrdinals = scoregroup.getScore().stream().map(ScoreType::getTrailOrdinal) //
				.collect(Collectors.toList());
		vaults.forEach(v -> {
			// determine the min and max of all score entries in the score group of this vault
			Stream<Integer> scoregroupPoints = scoreOrdinals.stream().map(t -> scoregroup.getVaultPoint(v, t));
			DoubleSummaryStatistics stats = scoregroupPoints.map(i -> HistoSet.decodeVaultValue(scoregroup, i / 1000.)) //
					.collect(Collectors.summarizingDouble(Double::doubleValue));
			decodedMinMaxValues[0] = Math.min(decodedMinMaxValues[0], stats.getMin());
			decodedMinMaxValues[1] = Math.max(decodedMinMaxValues[1], stats.getMax());
		});
		double[] result;
		if (decodedMinMaxValues[0] == Double.MAX_VALUE || decodedMinMaxValues[1] == -Double.MAX_VALUE) {
			result = new double[] { 0., 0. };
		} else {
			result = decodedMinMaxValues;
		}
		return result;
	}

	/**
	 * @return the lower/upper values from all scoregroup members
	 */
	public static double[] defineScoreExtrema(List<HistoVault> vaults, ScoreGroupType scoregroup, Settings settings) {
		List<Double> decodedLowValues = new ArrayList<>();
		List<Double> decodedHighValues = new ArrayList<>();

		List<Integer> scoreOrdinals = scoregroup.getScore().stream().map(ScoreType::getTrailOrdinal) //
				.collect(Collectors.toList());
		for (HistoVault v : vaults) {
			// determine the min and max of all score entries in the score group of this vault
			Stream<Integer> scoregroupPoints = scoreOrdinals.stream().map(t -> scoregroup.getVaultPoint(v, t));
			DoubleSummaryStatistics stats = scoregroupPoints.map(i -> HistoSet.decodeVaultValue(scoregroup, i / 1000.)) //
					.collect(Collectors.summarizingDouble(Double::doubleValue));
			decodedLowValues.add(stats.getMin());
			decodedHighValues.add(stats.getMax());
		}
		;
		log.finer(() -> scoregroup.getName() + "  decodedMinimums=" + Arrays.toString(decodedLowValues.toArray()) + "  decodedMaximums=" + Arrays.toString(decodedHighValues.toArray()));

		double[] extrema = getExtrema(scoregroup.getName(), decodedLowValues, decodedHighValues, settings);
		return extrema;
	}

	/**
	 * @return the points for the q0/q4 respective min/max trails; for score groups w/o min/max scores take the first score
	 */
	private static Integer[] getExtremumTrailPoints(HistoVault vault, IChannelItem channelItem, TrailSelector trailSelector) {
		int[] extremumOrdinals = trailSelector.getExtremumTrailsOrdinals();
		return new Integer[] { channelItem.getVaultPoint(vault, extremumOrdinals[0]), channelItem.getVaultPoint(vault, extremumOrdinals[1]) };
	}

	private static double[] getExtrema(String recordName, List<Double> decodedLowValues, List<Double> decodedHighValues, Settings settings) {
		ElementaryQuantile<Double> minQuantile = new ElementaryQuantile<>(decodedLowValues, true, settings);
		ElementaryQuantile<Double> maxQuantile = new ElementaryQuantile<>(decodedHighValues, true, settings);
		int scaleSpread = settings.getSummaryScaleSpread();
		double scaleMin = minQuantile.getExtremumFromRange(UniversalQuantile.INTER_QUARTILE_SIGMA_FACTOR, -scaleSpread);
		double scaleMax = maxQuantile.getExtremumFromRange(UniversalQuantile.INTER_QUARTILE_SIGMA_FACTOR, scaleSpread);
		double[] result = new double[] { Math.min(minQuantile.getQuantileLowerWhisker(), scaleMin),
				Math.max(maxQuantile.getQuantileUpperWhisker(), scaleMax) };
		log.finer(() -> recordName + " Quantile.Size=" + decodedLowValues.size() + "/" + decodedHighValues.size() + Arrays.toString(result));
		return result;
	}

}
