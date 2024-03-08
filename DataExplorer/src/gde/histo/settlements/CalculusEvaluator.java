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

package gde.histo.settlements;

import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.device.CalculusTypes;
import gde.device.ChannelPropertyType;
import gde.device.ChannelPropertyTypes;
import gde.device.ChannelType;
import gde.device.DeltaBasisTypes;
import gde.device.IDevice;
import gde.device.LevelingTypes;
import gde.device.TransitionCalculusType;
import gde.histo.datasources.HistoSet;
import gde.histo.transitions.Transition;
import gde.histo.utils.SingleResponseRegression;
import gde.histo.utils.SingleResponseRegression.RegressionType;
import gde.histo.utils.UniversalQuantile;
import gde.log.Logger;

/**
 * Collect settlement data for the trail recordset and subordinate objects.
 * @author Thomas Eickert (USER)
 */
public final class CalculusEvaluator {
	private final static String	$CLASS_NAME	= CalculusEvaluator.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * The delta level is the difference of the levels in the reference / recovery phase compared to the threshold phase.
	 * @author Thomas Eickert (USER)
	 */
	private static class DeltaLevelCalculator {

		private final DeltaBasisTypes	deltaBasis;
		private final RecordGroup			tmpRecordGroup;
		private final Transition			transition;
		private final Analyzer				analyzer;

		public DeltaLevelCalculator(DeltaBasisTypes deltaBasis, RecordGroup tmpRecordGroup, Transition transition, Analyzer analyzer) {
			this.deltaBasis = deltaBasis;
			this.tmpRecordGroup = tmpRecordGroup;
			this.transition = transition;
			this.analyzer = analyzer;
		}

		/**
		 * @return the level delta based on the first value of the phases
		 */
		public double calcFirst() {
			final double deltaValue;
			double referenceExtremum = 0.;
			double thresholdExtremum = 0.;
			double recoveryExtremum = 0.;
			for (int j = this.transition.getReferenceStartIndex(); j < this.transition.getReferenceEndIndex() + 1; j++) {
				Double aggregatedValue = this.tmpRecordGroup.getReal(j);
				if (aggregatedValue != null) {
					referenceExtremum = aggregatedValue;
					break;
				}
			}
			// one additional time step before and after in order to cope with potential measurement latencies
			for (int j = this.transition.getThresholdStartIndex() - 1; j < this.transition.getThresholdEndIndex() + 1 + 1; j++) {
				Double aggregatedValue = this.tmpRecordGroup.getReal(j);
				if (aggregatedValue != null) {
					thresholdExtremum = aggregatedValue;
					break;
				}
			}
			if (this.transition.getRecoveryStartIndex() > 0) {
				for (int j = this.transition.getRecoveryStartIndex(); j < this.transition.getRecoveryEndIndex() + 1; j++) {
					Double aggregatedValue = this.tmpRecordGroup.getReal(j);
					if (aggregatedValue != null) {
						recoveryExtremum = aggregatedValue;
						break;
					}
				}
			}

			deltaValue = calcDeltaValue(isPositiveTransition(), referenceExtremum, thresholdExtremum, recoveryExtremum);
			return deltaValue;
		}

		/**
		 * @return the level delta based on the last value of the phases
		 */
		public double calcLast() {
			final double deltaValue;
			double referenceExtremum = 0.;
			double thresholdExtremum = 0.;
			double recoveryExtremum = 0.;
			for (int j = this.transition.getReferenceEndIndex(); j >= this.transition.getReferenceStartIndex(); j--) {
				Double aggregatedValue = this.tmpRecordGroup.getReal(j);
				if (aggregatedValue != null) {
					referenceExtremum = aggregatedValue;
					break;
				}
			}
			// one additional time step before and after in order to cope with potential measurement latencies
			for (int j = this.transition.getThresholdEndIndex() + 1; j >= this.transition.getThresholdStartIndex() - 1; j--) {
				Double aggregatedValue = this.tmpRecordGroup.getReal(j);
				if (aggregatedValue != null) {
					thresholdExtremum = aggregatedValue;
					break;
				}
			}
			if (this.transition.getRecoveryStartIndex() > 0) {
				for (int j = this.transition.getRecoveryEndIndex(); j >= this.transition.getRecoveryStartIndex(); j--) {
					Double aggregatedValue = this.tmpRecordGroup.getReal(j);
					if (aggregatedValue != null) {
						recoveryExtremum = aggregatedValue;
						break;
					}
				}
			}

			deltaValue = calcDeltaValue(isPositiveTransition(), referenceExtremum, thresholdExtremum, recoveryExtremum);
			return deltaValue;
		}

		/**
		 * @return the level delta based on the mid value of the phases
		 */
		public double calcMid() {
			final double deltaValue;
			double referenceExtremum = 0.;
			double thresholdExtremum = 0.;
			double recoveryExtremum = 0.;
			if (this.transition.getReferenceSize() > 1) {
				int midIndex = (this.transition.getReferenceStartIndex() + this.transition.getReferenceEndIndex()) / 2;
				for (int i = midIndex, j = midIndex + 1; i <= this.transition.getReferenceEndIndex() && j >= this.transition.getReferenceStartIndex();
						i++, j--) {
					Double aggregatedValue = this.tmpRecordGroup.getReal(i);
					if (aggregatedValue != null) {
						referenceExtremum = aggregatedValue;
						break;
					}
					aggregatedValue = this.tmpRecordGroup.getReal(j);
					if (aggregatedValue != null) {
						referenceExtremum = aggregatedValue;
						break;
					}
				}
			} else {
				referenceExtremum = this.tmpRecordGroup.getReal(this.transition.getReferenceStartIndex());
			}
			if (this.transition.getThresholdSize() > 1) {
				int midIndex = (this.transition.getThresholdStartIndex() + this.transition.getThresholdEndIndex()) / 2;
				for (int i = midIndex, j = midIndex; i <= this.transition.getThresholdEndIndex() && j >= this.transition.getThresholdStartIndex(); j--) {
					Double aggregatedValue = this.tmpRecordGroup.getReal(i);
					if (aggregatedValue != null) {
						thresholdExtremum = aggregatedValue;
						break;
					}
					aggregatedValue = this.tmpRecordGroup.getReal(j);
					if (aggregatedValue != null) {
						thresholdExtremum = aggregatedValue;
						break;
					}
				}
			} else {
				thresholdExtremum = this.tmpRecordGroup.getReal(this.transition.getThresholdStartIndex());
			}
			if (this.transition.getRecoveryStartIndex() > 0) {
				if (this.transition.getRecoverySize() > 1) {
					int midIndex = (this.transition.getRecoveryStartIndex() + this.transition.getRecoveryEndIndex()) / 2;
					for (int i = midIndex, j = midIndex; i <= this.transition.getRecoveryEndIndex() && j >= this.transition.getRecoveryStartIndex(); j--) {
						Double aggregatedValue = this.tmpRecordGroup.getReal(i);
						if (aggregatedValue != null) {
							recoveryExtremum = aggregatedValue;
							break;
						}
						aggregatedValue = this.tmpRecordGroup.getReal(j);
						if (aggregatedValue != null) {
							recoveryExtremum = aggregatedValue;
							break;
						}
					}
				} else {
					recoveryExtremum = this.tmpRecordGroup.getReal(this.transition.getRecoveryStartIndex());
				}
			}

			deltaValue = calcDeltaValue(isPositiveTransition(), referenceExtremum, thresholdExtremum, recoveryExtremum);
			return deltaValue;
		}

		/**
		 * @return the level delta based on the average value of the phases
		 */
		public double calcAvg() {
			final double deltaValue;
			double referenceExtremum = 0.;
			double thresholdExtremum = 0.;
			double recoveryExtremum = 0.;
			double value = 0.;
			int skipCount = 0;
			for (int j = this.transition.getReferenceStartIndex(); j < this.transition.getReferenceEndIndex() + 1; j++) {
				Double aggregatedValue = this.tmpRecordGroup.getReal(j);
				if (aggregatedValue != null)
					value += (aggregatedValue - value) / (j - this.transition.getReferenceStartIndex() + 1);
				else
					skipCount++;
			}
			referenceExtremum = value / (this.transition.getReferenceSize() - skipCount);
			value = 0.;
			skipCount = 0;
			// one additional time step before and after in order to cope with potential measurement latencies
			for (int j = this.transition.getThresholdStartIndex() - 1; j < this.transition.getThresholdEndIndex() + 1 + 1; j++) {
				Double aggregatedValue = this.tmpRecordGroup.getReal(j);
				if (aggregatedValue != null)
					value += (aggregatedValue - value) / (j - this.transition.getThresholdStartIndex() + 1);
				else
					skipCount++;
			}
			thresholdExtremum = value / (this.transition.getThresholdSize() - skipCount);
			if (this.transition.getRecoveryStartIndex() > 0) {
				value = 0.;
				skipCount = 0;
				for (int j = this.transition.getRecoveryStartIndex(); j < this.transition.getRecoveryEndIndex() + 1; j++) {
					Double aggregatedValue = this.tmpRecordGroup.getReal(j);
					if (aggregatedValue != null)
						value += (aggregatedValue - value) / (j - this.transition.getRecoveryStartIndex() + 1);
					else
						skipCount++;
				}
				recoveryExtremum = value / (this.transition.getRecoverySize() - skipCount);
			}

			deltaValue = calcDeltaValue(isPositiveTransition(), referenceExtremum, thresholdExtremum, recoveryExtremum);
			return deltaValue;
		}

		/**
		 * @return the level delta based on the extremum value of the phases
		 */
		public double calcMinMax() {
			final double deltaValue;
			double referenceExtremum = 0.;
			double thresholdExtremum = 0.;
			double recoveryExtremum = 0.;
			// determine the direction of the peak or pulse or slope
			final boolean isPositiveDirection = isPositiveTransition();

			if (isPositiveDirection) {
				referenceExtremum = Double.MAX_VALUE;
				for (int j = this.transition.getReferenceStartIndex(); j < this.transition.getReferenceEndIndex() + 1; j++) {
					Double aggregatedValue = this.tmpRecordGroup.getReal(j);
					if (aggregatedValue != null && aggregatedValue < referenceExtremum) referenceExtremum = aggregatedValue;
				}
				thresholdExtremum = -Double.MAX_VALUE;
				// one additional time step before and after in order to cope with potential measurement latencies
				for (int j = this.transition.getThresholdStartIndex() - 1; j < this.transition.getThresholdEndIndex() + 1 + 1; j++) {
					Double aggregatedValue = this.tmpRecordGroup.getReal(j);
					if (aggregatedValue != null && aggregatedValue > thresholdExtremum) thresholdExtremum = aggregatedValue;
				}
				if (this.transition.getRecoveryStartIndex() > 0) {
					recoveryExtremum = Double.MAX_VALUE;
					for (int j = this.transition.getRecoveryStartIndex(); j < this.transition.getRecoveryEndIndex() + 1; j++) {
						Double aggregatedValue = this.tmpRecordGroup.getReal(j);
						if (aggregatedValue != null && aggregatedValue < recoveryExtremum) recoveryExtremum = aggregatedValue;
					}
				}
			} else {
				referenceExtremum = -Double.MAX_VALUE;
				for (int j = this.transition.getReferenceStartIndex(); j < this.transition.getReferenceEndIndex() + 1; j++) {
					Double aggregatedValue = this.tmpRecordGroup.getReal(j);
					if (aggregatedValue != null && aggregatedValue > referenceExtremum) referenceExtremum = aggregatedValue;
				}
				thresholdExtremum = Double.MAX_VALUE;
				// one additional time step before and after in order to cope with potential measurement latencies
				for (int j = this.transition.getThresholdStartIndex() - 1; j < this.transition.getThresholdEndIndex() + 1 + 1; j++) {
					Double aggregatedValue = this.tmpRecordGroup.getReal(j);
					if (aggregatedValue != null && aggregatedValue < thresholdExtremum) thresholdExtremum = aggregatedValue;
				}
				if (this.transition.getRecoveryStartIndex() > 0) {
					recoveryExtremum = -Double.MAX_VALUE;
					for (int j = this.transition.getRecoveryStartIndex(); j < this.transition.getRecoveryEndIndex() + 1; j++) {
						Double aggregatedValue = this.tmpRecordGroup.getReal(j);
						if (aggregatedValue != null && aggregatedValue > recoveryExtremum) recoveryExtremum = aggregatedValue;
					}
				}
			}
			deltaValue = calcDeltaValue(isPositiveDirection, referenceExtremum, thresholdExtremum, recoveryExtremum);
			return deltaValue;
		}

		/**
		 * Smoothing includes removing outliers and determining a level value based on a setting quantile cut point.
		 * @return the level delta based on the smoothed extremum value of the phases
		 */
		public double calcSmoothMinMax() {
			final double deltaValue;
			double referenceExtremum = 0.;
			double thresholdExtremum = 0.;
			double recoveryExtremum = 0.;
			// determine the direction of the peak or pulse or slope
			final boolean isPositiveDirection = isPositiveTransition();

			final IDevice device = analyzer.getActiveDevice();
			final Settings settings = analyzer.getSettings();
			final ChannelPropertyType channelProperty = device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_SIGMA);
			final double sigmaFactor = channelProperty.getValue() != null && !channelProperty.getValue().isEmpty()
					? Double.parseDouble(channelProperty.getValue()) : HistoSet.OUTLIER_SIGMA_DEFAULT;
			final ChannelPropertyType channelProperty2 = device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_RANGE_FACTOR);
			final double outlierFactor = channelProperty2.getValue() != null && !channelProperty2.getValue().isEmpty()
					? Double.parseDouble(channelProperty2.getValue()) : HistoSet.OUTLIER_RANGE_FACTOR_DEFAULT;
			final double probabilityCutPoint = !isPositiveDirection ? 1. - settings.getMinmaxQuantileDistance() : settings.getMinmaxQuantileDistance();
			{
				List<Double> values = new ArrayList<Double>();
				for (int j = this.transition.getReferenceStartIndex(); j < this.transition.getReferenceEndIndex() + 1; j++) {
					Double aggregatedValue = this.tmpRecordGroup.getReal(j);
					if (aggregatedValue != null) values.add(aggregatedValue);
				}
				UniversalQuantile<Double> tmpQuantile = new UniversalQuantile<>(values, true, sigmaFactor, outlierFactor, settings);
				referenceExtremum = tmpQuantile.getQuantile(probabilityCutPoint);
				log.fine(() -> "reference " + Arrays.toString(values.toArray()));
			}
			{
				List<Double> values = new ArrayList<Double>();
				// one additional time step before and after in order to cope with potential measurement latencies
				for (int j = this.transition.getThresholdStartIndex() - 1; j < this.transition.getThresholdEndIndex() + 1 + 1; j++) {
					Double aggregatedValue = this.tmpRecordGroup.getReal(j);
					if (aggregatedValue != null) values.add(aggregatedValue);
				}
				UniversalQuantile<Double> tmpQuantile = new UniversalQuantile<>(values, true, sigmaFactor, outlierFactor, settings);
				thresholdExtremum = tmpQuantile.getQuantile(isPositiveDirection ? 1. - settings.getMinmaxQuantileDistance()
						: settings.getMinmaxQuantileDistance());
				log.fine(() -> "threshold " + Arrays.toString(values.toArray()));
			}
			{
				if (this.transition.getRecoveryStartIndex() > 0) {
					List<Double> values = new ArrayList<Double>();
					for (int j = this.transition.getRecoveryStartIndex(); j < this.transition.getRecoveryEndIndex() + 1; j++) {
						Double aggregatedValue = this.tmpRecordGroup.getReal(j);
						if (aggregatedValue != null) values.add(aggregatedValue);
					}
					UniversalQuantile<Double> tmpQuantile = new UniversalQuantile<>(values, true, sigmaFactor, outlierFactor, settings);
					recoveryExtremum = tmpQuantile.getQuantile(probabilityCutPoint);
					log.fine(() -> "recovery " + Arrays.toString(values.toArray()));
				}
			}
			deltaValue = calcDeltaValue(isPositiveDirection, referenceExtremum, thresholdExtremum, recoveryExtremum);
			return deltaValue;
		}

		/**
		 * @param isPositiveDirection
		 * @param referenceExtremum
		 * @param thresholdExtremum
		 * @param recoveryExtremum
		 * @return the delta value of the reference / recovery phase and the threshold phase
		 */
		private double calcDeltaValue(boolean isPositiveDirection, double referenceExtremum, double thresholdExtremum, double recoveryExtremum) {
			final double deltaValue;

			if (this.transition.isSlope() || this.deltaBasis == null || this.deltaBasis == DeltaBasisTypes.REFERENCE)
				deltaValue = thresholdExtremum - referenceExtremum;
			else if (this.deltaBasis == DeltaBasisTypes.RECOVERY)
				deltaValue = thresholdExtremum - recoveryExtremum;
			else if (this.deltaBasis == DeltaBasisTypes.BOTH_AVG)
				deltaValue = thresholdExtremum - (referenceExtremum + recoveryExtremum) / 2.;
			else if (this.deltaBasis == DeltaBasisTypes.INNER)
				deltaValue = isPositiveDirection ? thresholdExtremum - Math.max(referenceExtremum, recoveryExtremum)
						: thresholdExtremum - Math.min(referenceExtremum, recoveryExtremum);
			else if (this.deltaBasis == DeltaBasisTypes.OUTER)
				deltaValue = isPositiveDirection ? thresholdExtremum - Math.min(referenceExtremum, recoveryExtremum)
						: thresholdExtremum - Math.max(referenceExtremum, recoveryExtremum);
			else
				throw new UnsupportedOperationException();

			log.fine(() -> String.format("referenceExtremum=%f  thresholdExtremum=%f  recoveryExtremum=%f  deltaValue=%f  @ isBasedOnRecovery=%s" //$NON-NLS-1$
					, referenceExtremum, thresholdExtremum, recoveryExtremum, deltaValue, this.deltaBasis));
			return deltaValue;
		}

		/**
		 * @param recordGroup
		 * @param transition
		 * @return true if the transition has a positive peak / pulse or a positive slope
		 */
		private boolean isPositiveTransition() {
			final boolean isPositiveDirection;
			{
				if (this.transition.isSlope()) {
					int fromIndex = this.transition.getReferenceStartIndex();
					int toIndex = this.transition.getThresholdEndIndex() + 1;
					SingleResponseRegression<Double> regression = new SingleResponseRegression<>(this.tmpRecordGroup.getSubPoints(fromIndex, toIndex),
							RegressionType.LINEAR);
					isPositiveDirection = regression.getSlope() > 0;
				} else {
					int fromIndex = this.transition.getReferenceStartIndex();
					int toIndex = this.transition.getRecoveryEndIndex() + 1;
					SingleResponseRegression<Double> regression = new SingleResponseRegression<>(this.tmpRecordGroup.getSubPoints(fromIndex, toIndex),
							RegressionType.QUADRATIC);
					isPositiveDirection = regression.getGamma() < 0;
				}
				log.log(FINER, "direction: ", isPositiveDirection);
			}
			return isPositiveDirection;
		}

	}

	private final SettlementRecord				histoSettlement;
	private final Analyzer								analyzer;
	private final TransitionCalculusType	calculus;
	private final ChannelType							logChannel;
	private final RecordGroup							recordGroup;

	public CalculusEvaluator(SettlementRecord newHistoSettlement, Analyzer analyzer) {
		this.histoSettlement = newHistoSettlement;
		this.analyzer = analyzer;
		this.calculus = newHistoSettlement.getSettlement().getEvaluation().getTransitionCalculus();
		this.logChannel = analyzer.getActiveDevice().getDeviceConfiguration().getChannel(newHistoSettlement.getLogChannelNumber());
		this.recordGroup = new RecordGroup(newHistoSettlement, this.logChannel.getReferenceGroupById(this.calculus.getReferenceGroupId()), analyzer);
		log.log(FINEST, GDE.STRING_GREATER, this.calculus);
	}

	/**
	 * Walk forward from the time step when the trigger has fired and collect the extremum values for the calculation.
	 * Add single result value.
	 * @param transition
	 */
	public void addFromTransition(Transition transition) {
		if (this.recordGroup.hasReasonableData()) {
			final int reverseTranslatedResult;
			final CalculusTypes calculusType = this.calculus.getCalculusType();
			if (calculusType == CalculusTypes.DELTA) {
				double deltaValue = calculateLevelDelta(this.recordGroup, this.calculus.getLeveling(), transition);
				reverseTranslatedResult = (int) (this.histoSettlement.reverseTranslateValue(this.calculus.isUnsigned() ? Math.abs(deltaValue)
						: deltaValue) * 1000.);
			} else if (calculusType == CalculusTypes.DELTA_PERMILLE) {
				double deltaValue = calculateLevelDelta(this.recordGroup, this.calculus.getLeveling(), transition);
				reverseTranslatedResult = (int) (this.histoSettlement.reverseTranslateValue(this.calculus.isUnsigned() ? Math.abs(deltaValue)
						: deltaValue) * 1000. * 1000.);
			} else if (calculusType == CalculusTypes.RELATIVE_DELTA_PERCENT) {
				double relativeDeltaValue = calculateLevelDelta(this.recordGroup, this.calculus.getLeveling(), transition) / (this.recordGroup.getRealMax() - this.recordGroup.getRealMin());
				reverseTranslatedResult = (int) (this.calculus.isUnsigned() ? Math.abs(relativeDeltaValue) * 1000. * 100.
						: relativeDeltaValue * 1000. * 100.); // all internal values are multiplied by 1000
			} else if (calculusType == CalculusTypes.RATIO || calculusType == CalculusTypes.RATIO_PERMILLE) {
				double denominator = calculateLevelDelta(this.recordGroup, this.calculus.getLeveling(), transition);
				RecordGroup divisorRecordGroup = new RecordGroup(this.histoSettlement,
						this.logChannel.getReferenceGroupById(this.calculus.getReferenceGroupIdDivisor()), analyzer);
				if (!divisorRecordGroup.hasReasonableData()) {
					return;
				}

				double divisor = calculateLevelDelta(divisorRecordGroup, this.calculus.getDivisorLeveling(), transition);
				log.finer(() -> this.recordGroup.getComment() + " denominator " + denominator + " divisor " + divisor); //$NON-NLS-1$
				if (calculusType == CalculusTypes.RATIO) {
					reverseTranslatedResult = (int) (this.calculus.isUnsigned() ? Math.abs(denominator / divisor * 1000.) : denominator / divisor * 1000.);
					// all internal values are multiplied by 1000
				} else {
					reverseTranslatedResult = (int) (this.calculus.isUnsigned() ? Math.abs(denominator / divisor * 1000. * 1000.)
							: denominator / divisor * 1000. * 1000.); // all internal values are multiplied by 1000
				}
			} else {
				reverseTranslatedResult = 0;
				throw new UnsupportedOperationException();
			}

			this.histoSettlement.add(reverseTranslatedResult, transition);
			log.fine(() -> String.format("%s: timeStamp_ms=%d  reverseTranslatedResult=%d  calcType=%s", this.histoSettlement.getName(), //$NON-NLS-1$
					(int) this.histoSettlement.getParent().getTime_ms(transition.getThresholdEndIndex() + 1), reverseTranslatedResult, calculusType));
		}
	}

	/**
	 * Walk through the measurement record and calculates the difference between the threshold level value and the base level value (reference
	 * or recovery).
	 * Skip null measurement values.
	 * @param recordGroup holds the measurement points
	 * @param leveling rule for determining the level value from the device configuration
	 * @param transition holds the transition properties which are used to access the measurement data
	 * @return peak spread value as translatedValue
	 */
	private double calculateLevelDelta(RecordGroup tmpRecordGroup, LevelingTypes leveling, Transition transition) {
		final double deltaValue;

		DeltaLevelCalculator deltaLevelCalculator = new DeltaLevelCalculator(this.calculus.getDeltaBasis(), tmpRecordGroup, transition, analyzer);

		if (leveling == LevelingTypes.FIRST) {
			deltaValue = deltaLevelCalculator.calcFirst();
		} else if (leveling == LevelingTypes.LAST) {
			deltaValue = deltaLevelCalculator.calcLast();
		} else if (leveling == LevelingTypes.MID) {
			deltaValue = deltaLevelCalculator.calcMid();
		} else if (leveling == LevelingTypes.AVG) {
			deltaValue = deltaLevelCalculator.calcAvg();
		} else if (leveling == LevelingTypes.MINMAX) {
			deltaValue = deltaLevelCalculator.calcMinMax();
		} else if (leveling == LevelingTypes.SMOOTH_MINMAX) {
			deltaValue = deltaLevelCalculator.calcSmoothMinMax();
		} else {
			throw new UnsupportedOperationException();
		}

		return deltaValue;
	}

}
