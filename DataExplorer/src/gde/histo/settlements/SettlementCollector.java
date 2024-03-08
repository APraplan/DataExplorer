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

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.data.Record;
import gde.device.AmountTypes;
import gde.device.CalculusTypes;
import gde.device.ChannelPropertyType;
import gde.device.ChannelPropertyTypes;
import gde.device.ChannelType;
import gde.device.DeltaBasisTypes;
import gde.device.EvaluationType;
import gde.device.FigureTypes;
import gde.device.IDevice;
import gde.device.LevelingTypes;
import gde.device.TransitionAmountType;
import gde.device.TransitionCalculusType;
import gde.device.TransitionFigureType;
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
@Deprecated
public final class SettlementCollector {
	private final static String	$CLASS_NAME	= SettlementCollector.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	private final Analyzer			analyzer;
	private final Settings			settings;

	public SettlementCollector(Analyzer analyzer) {
		this.analyzer = analyzer;
		this.settings = analyzer.getSettings();
	}

	/**
	 * Take histo transitions which are applicable and fetch trigger time steps and records from the parent.
	 * Add settlement points according the evaluation rules.
	 * @param transitions holds transitions identified for this settlement
	 */
	public synchronized void addFromTransitions(SettlementRecord histoSettlement, Collection<Transition> transitions) {
		EvaluationType evaluation = histoSettlement.getSettlement().getEvaluation();
		for (Transition transition : transitions) {
			if (evaluation.getTransitionFigure() != null) {
				addFigure(histoSettlement, transition);
			} else if (evaluation.getTransitionAmount() != null) {
				addAmount(histoSettlement, transition);
			} else if (evaluation.getTransitionCalculus() != null) {
				addCalculus(histoSettlement, transition);
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}

	/**
	 * Figure out the result value and add result value.
	 * The timeSum and timeStep value has the unit seconds with 3 decimal places.
	 * @param transition
	 */
	private void addFigure(SettlementRecord histoSettlement, Transition transition) {
		TransitionFigureType transitionFigure = histoSettlement.getSettlement().getEvaluation().getTransitionFigure();
		log.log(FINE, GDE.STRING_GREATER, transitionFigure);
		final int reverseTranslatedResult;
		if (transitionFigure.getFigureType() == FigureTypes.COUNT) {
			reverseTranslatedResult = transition.getThresholdSize() * 1000; // all internal values are multiplied by 1000
		} else if (transitionFigure.getFigureType() == FigureTypes.TIME_SUM_SEC) {
			reverseTranslatedResult = (int) (histoSettlement.getParent().getTime_ms(transition.getThresholdEndIndex() + 1) - histoSettlement.getParent().getTime_ms(transition.getThresholdStartIndex()));
		} else if (transitionFigure.getFigureType() == FigureTypes.TIME_STEP_SEC) {
			reverseTranslatedResult = (int) histoSettlement.getParent().getTime_ms(transition.getThresholdStartIndex());
		} else {
			reverseTranslatedResult = 0;
			throw new UnsupportedOperationException();
		}
		// add to settlement record
		histoSettlement.add(reverseTranslatedResult, transition);
		log.fine(() -> String.format("%s: timeStamp_ms=%d  reverseTranslatedResult=%d  figureType=%s", histoSettlement.getName(), (int) histoSettlement.getParent().getTime_ms(transition.getThresholdEndIndex() + 1) //$NON-NLS-1$
				, reverseTranslatedResult, transitionFigure.getFigureType()));
	}

	/**
	 * Walk forward from the time step when the trigger has fired and amount the extremum values for the calculation.
	 * Add single result value.
	 * @param transition
	 */
	private void addAmount(SettlementRecord histoSettlement, Transition transition) {
		IDevice device = analyzer.getActiveDevice();
		TransitionAmountType transitionAmount = histoSettlement.getSettlement().getEvaluation().getTransitionAmount();
		log.log(FINE, GDE.STRING_GREATER, transitionAmount);
		final Record record = histoSettlement.getParent().get(histoSettlement.getParent().getRecordNames()[transitionAmount.getRefOrdinal()]);
		log.fine(() -> record.getName() + " values   " + record.subList(transition.getReferenceStartIndex(), transition.getThresholdEndIndex() + 1)); //$NON-NLS-1$
		final int reverseTranslatedResult;
		if (transitionAmount.getAmountType() == AmountTypes.MIN) {
			double min = Double.MAX_VALUE;
			for (int j = transition.getThresholdStartIndex(); j < transition.getThresholdEndIndex() + 1; j++)
				if (record.elementAt(j) != null) min = Math.min(min, transitionAmount.isUnsigned()
						? Math.abs(device.translateValue(record, record.elementAt(j) / 1000.)) : device.translateValue(record, record.elementAt(j) / 1000.));
			reverseTranslatedResult = (int) (histoSettlement.reverseTranslateValue(min) * 1000.);
		} else if (transitionAmount.getAmountType() == AmountTypes.MAX) {
			double max = transitionAmount.isUnsigned() ? 0. : -Double.MAX_VALUE;
			for (int j = transition.getThresholdStartIndex(); j < transition.getThresholdEndIndex() + 1; j++)
				if (record.elementAt(j) != null) max = Math.max(max, transitionAmount.isUnsigned()
						? Math.abs(device.translateValue(record, record.elementAt(j) / 1000.)) : device.translateValue(record, record.elementAt(j) / 1000.));
			reverseTranslatedResult = (int) (histoSettlement.reverseTranslateValue(max) * 1000.);
		} else if (transitionAmount.getAmountType() == AmountTypes.AVG) {
			double avg = 0., value = 0.;
			int skipCount = 0;
			for (int j = transition.getThresholdStartIndex(); j < transition.getThresholdEndIndex() + 1; j++)
				if (record.elementAt(j) != null) {
					value = transitionAmount.isUnsigned() ? Math.abs(device.translateValue(record, record.elementAt(j) / 1000.))
							: device.translateValue(record, record.elementAt(j) / 1000.);
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.getThresholdStartIndex() - skipCount + 1);
				} else
					skipCount++;
			reverseTranslatedResult = (int) (histoSettlement.reverseTranslateValue(avg) * 1000.);
		} else if (transitionAmount.getAmountType() == AmountTypes.SIGMA) {
			// https://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods
			double avg = 0., q = 0., value = 0.;
			int skipCount = 0;
			for (int j = transition.getThresholdStartIndex(); j < transition.getThresholdEndIndex() + 1; j++)
				if (record.elementAt(j) != null) {
					value = transitionAmount.isUnsigned() ? Math.abs(device.translateValue(record, record.elementAt(j) / 1000.))
							: device.translateValue(record, record.elementAt(j) / 1000.);
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.getThresholdStartIndex() - skipCount + 1);
					q += deltaAvg * (value - avg);
					if (log.isLoggable(FINE)) log.log(FINE, String.format("value=%f  deltaAvg=%f  q=%f", value, deltaAvg, q)); //$NON-NLS-1$
				} else
					skipCount++;
			reverseTranslatedResult = (int) (histoSettlement.reverseTranslateValue(Math.sqrt(q / (transition.getThresholdSize() - skipCount - 1))) * 1000.);
		} else {
			reverseTranslatedResult = 0;
			throw new UnsupportedOperationException();
		}
		// add to settlement record
		histoSettlement.add(reverseTranslatedResult, transition);
		log.fine(() -> String.format("%s: timeStamp_ms=%d  reverseTranslatedResult=%d  amountType=%s", //$NON-NLS-1$
				histoSettlement.getName(), (int) histoSettlement.getParent().getTime_ms(transition.getThresholdEndIndex() + 1), reverseTranslatedResult, transitionAmount.getAmountType()));
	}

	/**
	 * Walk forward from the time step when the trigger has fired and collect the extremum values for the calculation.
	 * Add single result value.
	 * @param transition
	 */
	private void addCalculus(SettlementRecord histoSettlement, Transition transition) {
		TransitionCalculusType calculus = histoSettlement.getSettlement().getEvaluation().getTransitionCalculus();
		log.log(FINEST, GDE.STRING_GREATER, calculus);
		final ChannelType logChannel = analyzer.getActiveDevice().getDeviceConfiguration().getChannel(histoSettlement.getLogChannelNumber());
		final RecordGroup recordGroup = new RecordGroup(histoSettlement, logChannel.getReferenceGroupById(calculus.getReferenceGroupId()), analyzer);
		if (recordGroup.hasReasonableData()) {
			final int reverseTranslatedResult;
			if (calculus.getCalculusType() == CalculusTypes.DELTA) {
				final double deltaValue = calculateLevelDelta(recordGroup, calculus.getDeltaBasis(), calculus.getLeveling(), transition);
				reverseTranslatedResult = (int) (histoSettlement.reverseTranslateValue(calculus.isUnsigned() ? Math.abs(deltaValue) : deltaValue) * 1000.);
			} else if (calculus.getCalculusType() == CalculusTypes.DELTA_PERMILLE) {
				final double deltaValue = calculateLevelDelta(recordGroup, calculus.getDeltaBasis(), calculus.getLeveling(), transition);
				reverseTranslatedResult = (int) (histoSettlement.reverseTranslateValue(calculus.isUnsigned() ? Math.abs(deltaValue)
						: deltaValue) * 1000. * 1000.);
			} else if (calculus.getCalculusType() == CalculusTypes.RELATIVE_DELTA_PERCENT) {
				final double relativeDeltaValue = calculateLevelDelta(recordGroup, calculus.getDeltaBasis(), calculus.getLeveling(), transition) / (recordGroup.getRealMax() - recordGroup.getRealMin());
				reverseTranslatedResult = (int) (calculus.isUnsigned() ? Math.abs(relativeDeltaValue) * 1000. * 100. : relativeDeltaValue * 1000. * 100.); // all
																																																																										// internal
																																																																										// values
																																																																										// are
																																																																										// multiplied
																																																																										// by
																																																																										// 1000
			} else if (calculus.getCalculusType() == CalculusTypes.RATIO || calculus.getCalculusType() == CalculusTypes.RATIO_PERMILLE) {
				final double denominator = calculateLevelDelta(recordGroup, calculus.getDeltaBasis(), calculus.getLeveling(), transition);
				final RecordGroup divisorRecordGroup = new RecordGroup(histoSettlement,
						logChannel.getReferenceGroupById(calculus.getReferenceGroupIdDivisor()), analyzer);
				if (!divisorRecordGroup.hasReasonableData()) {
					return;
				}

				final double divisor = calculateLevelDelta(divisorRecordGroup, calculus.getDeltaBasis(), calculus.getDivisorLeveling(), transition);
				log.finer(() -> recordGroup.getComment() + " denominator " + denominator + " divisor " + divisor); //$NON-NLS-1$
				if (calculus.getCalculusType() == CalculusTypes.RATIO) {
					reverseTranslatedResult = (int) (calculus.isUnsigned() ? Math.abs(denominator / divisor * 1000.) : denominator / divisor * 1000.);
					// all internal values are multiplied by 1000
				} else {
					reverseTranslatedResult = (int) (calculus.isUnsigned() ? Math.abs(denominator / divisor * 1000. * 1000.)
							: denominator / divisor * 1000. * 1000.);
					// all internal values are multiplied by 1000
				}
			} else {
				reverseTranslatedResult = 0;
				throw new UnsupportedOperationException();
			}
			// add to settlement record --- no recordgroup zero ratios which often occur for discharge logs from UDP60
			boolean isNeglectableRatioValue = recordGroup.getSize() > 1 && reverseTranslatedResult == 0.0 && (calculus.getCalculusType() == CalculusTypes.RATIO || calculus.getCalculusType() == CalculusTypes.RATIO_PERMILLE);
			if (!isNeglectableRatioValue) histoSettlement.add(reverseTranslatedResult, transition);
			log.fine(() -> String.format("%s: timeStamp_ms=%d  reverseTranslatedResult=%d  calcType=%s", histoSettlement.getName(), (int) histoSettlement.getParent().getTime_ms(transition.getThresholdEndIndex() + 1) //$NON-NLS-1$
					, reverseTranslatedResult, calculus.getCalculusType()));
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
	private double calculateLevelDelta(RecordGroup recordGroup, DeltaBasisTypes deltaBasis, LevelingTypes leveling, Transition transition) {
		double deltaValue = 0.;
		IDevice device = analyzer.getActiveDevice();

		// determine the direction of the peak or pulse or slope
		final boolean isPositiveDirection = isPositiveTransition(recordGroup, transition);

		double referenceExtremum = 0.;
		double thresholdExtremum = 0.;
		double recoveryExtremum = 0.;
		if (leveling == LevelingTypes.FIRST) {
			for (int j = transition.getReferenceStartIndex(); j < transition.getReferenceEndIndex() + 1; j++) {
				Double aggregatedValue = recordGroup.getReal(j);
				if (aggregatedValue != null) {
					referenceExtremum = aggregatedValue;
					break;
				}
			}
			// one additional time step before and after in order to cope with potential measurement latencies
			for (int j = transition.getThresholdStartIndex() - 1; j < transition.getThresholdEndIndex() + 1 + 1; j++) {
				Double aggregatedValue = recordGroup.getReal(j);
				if (aggregatedValue != null) {
					thresholdExtremum = aggregatedValue;
					break;
				}
			}
			if (transition.getRecoveryStartIndex() > 0) {
				for (int j = transition.getRecoveryStartIndex(); j < transition.getRecoveryEndIndex() + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null) {
						recoveryExtremum = aggregatedValue;
						break;
					}
				}
			}
		} else if (leveling == LevelingTypes.LAST) {
			for (int j = transition.getReferenceEndIndex(); j >= transition.getReferenceStartIndex(); j--) {
				Double aggregatedValue = recordGroup.getReal(j);
				if (aggregatedValue != null) {
					referenceExtremum = aggregatedValue;
					break;
				}
			}
			// one additional time step before and after in order to cope with potential measurement latencies
			for (int j = transition.getThresholdEndIndex() + 1; j >= transition.getThresholdStartIndex() - 1; j--) {
				Double aggregatedValue = recordGroup.getReal(j);
				if (aggregatedValue != null) {
					thresholdExtremum = aggregatedValue;
					break;
				}
			}
			if (transition.getRecoveryStartIndex() > 0) {
				for (int j = transition.getRecoveryEndIndex(); j >= transition.getRecoveryStartIndex(); j--) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null) {
						recoveryExtremum = aggregatedValue;
						break;
					}
				}
			}
		} else if (leveling == LevelingTypes.MID) {
			if (transition.getReferenceSize() > 1) {
				int midIndex = (transition.getReferenceStartIndex() + transition.getReferenceEndIndex()) / 2;
				for (int i = midIndex, j = midIndex + 1; i <= transition.getReferenceEndIndex() && j >= transition.getReferenceStartIndex(); i++, j--) {
					Double aggregatedValue = recordGroup.getReal(i);
					if (aggregatedValue != null) {
						referenceExtremum = aggregatedValue;
						break;
					}
					aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null) {
						referenceExtremum = aggregatedValue;
						break;
					}
				}
			} else {
				referenceExtremum = recordGroup.getReal(transition.getReferenceStartIndex());
			}
			if (transition.getThresholdSize() > 1) {
				int midIndex = (transition.getThresholdStartIndex() + transition.getThresholdEndIndex()) / 2;
				for (int i = midIndex, j = midIndex; i <= transition.getThresholdEndIndex() && j >= transition.getThresholdStartIndex(); j--) {
					Double aggregatedValue = recordGroup.getReal(i);
					if (aggregatedValue != null) {
						thresholdExtremum = aggregatedValue;
						break;
					}
					aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null) {
						thresholdExtremum = aggregatedValue;
						break;
					}
				}
			} else {
				thresholdExtremum = recordGroup.getReal(transition.getThresholdStartIndex());
			}
			if (transition.getRecoveryStartIndex() > 0) {
				if (transition.getRecoverySize() > 1) {
					int midIndex = (transition.getRecoveryStartIndex() + transition.getRecoveryEndIndex()) / 2;
					for (int i = midIndex, j = midIndex; i <= transition.getRecoveryEndIndex() && j >= transition.getRecoveryStartIndex(); j--) {
						Double aggregatedValue = recordGroup.getReal(i);
						if (aggregatedValue != null) {
							recoveryExtremum = aggregatedValue;
							break;
						}
						aggregatedValue = recordGroup.getReal(j);
						if (aggregatedValue != null) {
							recoveryExtremum = aggregatedValue;
							break;
						}
					}
				} else {
					recoveryExtremum = recordGroup.getReal(transition.getRecoveryStartIndex());
				}
			}
		} else if (leveling == LevelingTypes.AVG) {
			double value = 0.;
			int skipCount = 0;
			for (int j = transition.getReferenceStartIndex(); j < transition.getReferenceEndIndex() + 1; j++) {
				Double aggregatedValue = recordGroup.getReal(j);
				if (aggregatedValue != null)
					value += (aggregatedValue - value) / (j - transition.getReferenceStartIndex() + 1);
				else
					skipCount++;
			}
			referenceExtremum = value / (transition.getReferenceSize() - skipCount);
			value = 0.;
			skipCount = 0;
			// one additional time step before and after in order to cope with potential measurement latencies
			for (int j = transition.getThresholdStartIndex() - 1; j < transition.getThresholdEndIndex() + 1 + 1; j++) {
				Double aggregatedValue = recordGroup.getReal(j);
				if (aggregatedValue != null)
					value += (aggregatedValue - value) / (j - transition.getThresholdStartIndex() + 1);
				else
					skipCount++;
			}
			thresholdExtremum = value / (transition.getThresholdSize() - skipCount);
			if (transition.getRecoveryStartIndex() > 0) {
				value = 0.;
				skipCount = 0;
				for (int j = transition.getRecoveryStartIndex(); j < transition.getRecoveryEndIndex() + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null)
						value += (aggregatedValue - value) / (j - transition.getRecoveryStartIndex() + 1);
					else
						skipCount++;
				}
				recoveryExtremum = value / (transition.getRecoverySize() - skipCount);
			}
		} else if (leveling == LevelingTypes.MINMAX) {
			if (isPositiveDirection) {
				referenceExtremum = Double.MAX_VALUE;
				for (int j = transition.getReferenceStartIndex(); j < transition.getReferenceEndIndex() + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null && aggregatedValue < referenceExtremum) referenceExtremum = aggregatedValue;
				}
				thresholdExtremum = -Double.MAX_VALUE;
				// one additional time step before and after in order to cope with potential measurement latencies
				for (int j = transition.getThresholdStartIndex() - 1; j < transition.getThresholdEndIndex() + 1 + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null && aggregatedValue > thresholdExtremum) thresholdExtremum = aggregatedValue;
				}
				if (transition.getRecoveryStartIndex() > 0) {
					recoveryExtremum = Double.MAX_VALUE;
					for (int j = transition.getRecoveryStartIndex(); j < transition.getRecoveryEndIndex() + 1; j++) {
						Double aggregatedValue = recordGroup.getReal(j);
						if (aggregatedValue != null && aggregatedValue < recoveryExtremum) recoveryExtremum = aggregatedValue;
					}
				}
			} else {
				referenceExtremum = -Double.MAX_VALUE;
				for (int j = transition.getReferenceStartIndex(); j < transition.getReferenceEndIndex() + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null && aggregatedValue > referenceExtremum) referenceExtremum = aggregatedValue;
				}
				thresholdExtremum = Double.MAX_VALUE;
				// one additional time step before and after in order to cope with potential measurement latencies
				for (int j = transition.getThresholdStartIndex() - 1; j < transition.getThresholdEndIndex() + 1 + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null && aggregatedValue < thresholdExtremum) thresholdExtremum = aggregatedValue;
				}
				if (transition.getRecoveryStartIndex() > 0) {
					recoveryExtremum = -Double.MAX_VALUE;
					for (int j = transition.getRecoveryStartIndex(); j < transition.getRecoveryEndIndex() + 1; j++) {
						Double aggregatedValue = recordGroup.getReal(j);
						if (aggregatedValue != null && aggregatedValue > recoveryExtremum) recoveryExtremum = aggregatedValue;
					}
				}
			}
		} else if (leveling == LevelingTypes.SMOOTH_MINMAX) {
			final ChannelPropertyType channelProperty = device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_SIGMA);
			final double sigmaFactor = channelProperty.getValue() != null && !channelProperty.getValue().isEmpty()
					? Double.parseDouble(channelProperty.getValue()) : HistoSet.OUTLIER_SIGMA_DEFAULT;
			final ChannelPropertyType channelProperty2 = device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_RANGE_FACTOR);
			final double outlierFactor = channelProperty2.getValue() != null && !channelProperty2.getValue().isEmpty()
					? Double.parseDouble(channelProperty2.getValue()) : HistoSet.OUTLIER_RANGE_FACTOR_DEFAULT;
			final double probabilityCutPoint = !isPositiveDirection ? 1. - settings.getMinmaxQuantileDistance() : settings.getMinmaxQuantileDistance();
			{
				List<Double> values = new ArrayList<Double>();
				for (int j = transition.getReferenceStartIndex(); j < transition.getReferenceEndIndex() + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null) values.add(aggregatedValue);
				}
				UniversalQuantile<Double> tmpQuantile = new UniversalQuantile<>(values, true, sigmaFactor, outlierFactor, settings);
				referenceExtremum = tmpQuantile.getQuantile(probabilityCutPoint);
				log.fine(() -> "reference " + Arrays.toString(values.toArray()));
			}
			{
				List<Double> values = new ArrayList<Double>();
				// one additional time step before and after in order to cope with potential measurement latencies
				for (int j = transition.getThresholdStartIndex() - 1; j < transition.getThresholdEndIndex() + 1 + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null) values.add(aggregatedValue);
				}
				UniversalQuantile<Double> tmpQuantile = new UniversalQuantile<>(values, true, sigmaFactor, outlierFactor, settings);
				referenceExtremum = tmpQuantile.getQuantile(probabilityCutPoint);
				thresholdExtremum = tmpQuantile.getQuantile(isPositiveDirection ? 1. - settings.getMinmaxQuantileDistance()
						: settings.getMinmaxQuantileDistance());
				log.fine(() -> "threshold " + Arrays.toString(values.toArray()));
			}
			{
				if (transition.getRecoveryStartIndex() > 0) {
					List<Double> values = new ArrayList<Double>();
					for (int j = transition.getRecoveryStartIndex(); j < transition.getRecoveryEndIndex() + 1; j++) {
						Double aggregatedValue = recordGroup.getReal(j);
						if (aggregatedValue != null) values.add(aggregatedValue);
					}
					UniversalQuantile<Double> tmpQuantile = new UniversalQuantile<>(values, true, sigmaFactor, outlierFactor, settings);
					referenceExtremum = tmpQuantile.getQuantile(probabilityCutPoint);
					recoveryExtremum = tmpQuantile.getQuantile(probabilityCutPoint);
					log.fine(() -> "recovery " + Arrays.toString(values.toArray()));
				}
			}
		} else {
			throw new UnsupportedOperationException();
		}

		if (transition.isSlope() || deltaBasis == null || deltaBasis == DeltaBasisTypes.REFERENCE)
			deltaValue = thresholdExtremum - referenceExtremum;
		else if (deltaBasis == DeltaBasisTypes.RECOVERY)
			deltaValue = thresholdExtremum - recoveryExtremum;
		else if (deltaBasis == DeltaBasisTypes.BOTH_AVG)
			deltaValue = thresholdExtremum - (referenceExtremum + recoveryExtremum) / 2.;
		else if (deltaBasis == DeltaBasisTypes.INNER)
			deltaValue = isPositiveDirection ? thresholdExtremum - Math.max(referenceExtremum, recoveryExtremum)
					: thresholdExtremum - Math.min(referenceExtremum, recoveryExtremum);
		else if (deltaBasis == DeltaBasisTypes.OUTER)
			deltaValue = isPositiveDirection ? thresholdExtremum - Math.min(referenceExtremum, recoveryExtremum)
					: thresholdExtremum - Math.max(referenceExtremum, recoveryExtremum);
		else
			throw new UnsupportedOperationException();

		if (log.isLoggable(FINE))
			log.log(FINE, String.format("%s %s referenceExtremum=%f  thresholdExtremum=%f  recoveryExtremum=%f  deltaValue=%f  @ isBasedOnRecovery=%s" //$NON-NLS-1$
					, recordGroup.getComment(), leveling.value(), referenceExtremum, thresholdExtremum, recoveryExtremum, deltaValue, deltaBasis));
		return deltaValue;
	}

	/**
	 * @param recordGroup
	 * @param transition
	 * @return true if the transition has a positive peak / pulse or a positive slope
	 */
	private static boolean isPositiveTransition(RecordGroup recordGroup, Transition transition) {
		final boolean isPositiveDirection;
		{
			if (transition.isSlope()) {
				int fromIndex = transition.getReferenceStartIndex();
				int toIndex = transition.getThresholdEndIndex() + 1;
				SingleResponseRegression<Double> regression = new SingleResponseRegression<>(recordGroup.getSubPoints(fromIndex, toIndex),
						RegressionType.LINEAR);
				isPositiveDirection = regression.getSlope() > 0;
			} else {
				int fromIndex = transition.getReferenceStartIndex();
				int toIndex = transition.getRecoveryEndIndex() + 1;
				SingleResponseRegression<Double> regression = new SingleResponseRegression<>(recordGroup.getSubPoints(fromIndex, toIndex),
						RegressionType.QUADRATIC);
				isPositiveDirection = regression.getGamma() < 0;
			}
			if (log.isLoggable(FINER)) log.log(FINER, "direction: ", isPositiveDirection);
		}
		return isPositiveDirection;
	}

}
