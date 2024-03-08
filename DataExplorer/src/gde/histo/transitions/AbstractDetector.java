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

package gde.histo.transitions;

import static java.util.logging.Level.FINEST;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import gde.Analyzer;
import gde.GDE;
import gde.data.Record;
import gde.device.TransitionClassTypes;
import gde.device.TransitionType;
import gde.device.TransitionValueTypes;
import gde.log.Logger;
import gde.utils.StringHelper;

/**
 * Analyze a record for transitions defined in the device channel settings.
 * @author Thomas Eickert (USER)
 */
public class AbstractDetector {
	final static String				$CLASS_NAME	= AbstractDetector.class.getName();
	final static Logger				log					= Logger.getLogger($CLASS_NAME);

	protected SettlementDeque	referenceDeque;
	protected SettlementDeque	thresholdDeque;
	protected SettlementDeque	recoveryDeque;

	protected TriggerState		previousTriggerState;
	protected TriggerState		triggerState;

	protected enum TriggerState {
		/**
		 * check measurements until the reference duration is satisfied and the measurement value goes beyond the trigger threshold
		 */
		WAITING,
		/**
		 * threshold conditions (level and duration) apply to all threshold measurements.
		 * for peaks this phase includes a level hysteresis within the threshold duration: the measurement value must not exceed the recovery level.
		 */
		TRIGGERED,
		/**
		 * check measurements until the recovery duration is satisfied and the measurement value stays within the recovery threshold
		 */
		RECOVERING;
	}

	/**
	 * Provide the extremum value of the deque for reference purposes and the information if the deque has sufficient elements for the deque
	 * time period.
	 * Ensure the maximum deque time period by removing elements from the front of the deque.
	 * Expect elements to be added in chronological order.
	 * Do not support null measurement values.
	 * Resizable array implementation of the Deque interface.
	 * @author Thomas Eickert
	 */
	protected class SettlementDeque extends ArrayDeque<Double> {

		private static final long	serialVersionUID	= 915484098600135376L;

		private static final int	CAPACITY_SURPLUS	= 11;									// plus 11 elements ensures sufficient elements for most cases

		private boolean						isMinimumExtremum;
		private long							timePeriod_100ns;

		protected double					extremeValue;
		protected int							startIndex;

		private ArrayDeque<Long>	timeStampDeque;
		private List<Double>			sortedValues;

		/**
		 * Be sure to call initialize before using the deque.
		 * @param numElements
		 * @param isMinimumExtremum true if the extreme value shall determine the minimum of all values since initialize / clear
		 * @param timePeriod_100ns
		 */
		public SettlementDeque(int numElements, boolean isMinimumExtremum, long timePeriod_100ns) {
			super(numElements + SettlementDeque.CAPACITY_SURPLUS);
			this.isMinimumExtremum = isMinimumExtremum;
			this.timePeriod_100ns = timePeriod_100ns;
			this.timeStampDeque = new ArrayDeque<Long>(numElements);
			this.sortedValues = null;
			clear();
		}

		@Override
		@Deprecated // does not add the timestamp value
		public boolean add(Double translatedValue) {
			throw new UnsupportedOperationException();
		}

		public void addFirst(double translatedValue, long timeStamp_100ns) {
			super.addFirst(translatedValue);
			this.extremeValue = this.isMinimumExtremum ? Math.min(this.extremeValue, translatedValue) : Math.max(this.extremeValue, translatedValue);
			this.sortedValues = null;
			this.timeStampDeque.addFirst(timeStamp_100ns);
			ensureTimePeriod();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.ArrayDeque#addLast(java.lang.Object)
		 *
		 */
		public void addLast(double translatedValue, long timeStamp_100ns) {
			super.addLast(translatedValue);
			this.extremeValue = this.isMinimumExtremum ? Math.min(this.extremeValue, translatedValue) : Math.max(this.extremeValue, translatedValue);
			this.sortedValues = null;
			this.timeStampDeque.addLast(timeStamp_100ns);
			ensureTimePeriod();
		}

		@Override
		@Deprecated // does not add the timestamp value
		public void addLast(Double translatedValue) {
			throw new UnsupportedOperationException();
		}

		@Override // reInitializes the deque properties
		public void clear() {
			super.clear();
			this.timeStampDeque.clear();
			this.extremeValue = this.isMinimumExtremum ? Double.MAX_VALUE : -Double.MAX_VALUE;
			// leave untouched because of initialize() this.startIndex = -1;
			this.sortedValues = null;
		}

		/**
		 * @param newStartIndex is the source record index at the initialize call
		 */
		public void initialize(int newStartIndex) {
			clear();
			this.startIndex = newStartIndex;
		}

		/**
		 * Refreshes all properties.
		 * Use this method after removing elements from this deque.
		 */
		public void reInitialize(int removedCount) {
			if (this.isEmpty())
				clear();
			else {
				this.startIndex -= removedCount;
				this.sortedValues = null;
				if (this.isMinimumExtremum) {
					this.extremeValue = Double.MAX_VALUE;
					for (Double value : this) {
						this.extremeValue = Math.min(this.extremeValue, value.doubleValue());
					}
				} else {
					this.extremeValue = -Double.MAX_VALUE;
					for (Double value : this) {
						this.extremeValue = Math.max(this.extremeValue, value.doubleValue());
					}
				}
			}
		}

		@Override
		@Deprecated // would require reInitializing the deque properties
		public SettlementDeque clone() {
			throw new UnsupportedOperationException();
		}

		@Override
		@Deprecated // would require reInitializing the deque properties
		public boolean offer(Double translatedValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		@Deprecated // would require reInitializing the deque properties
		public boolean offerFirst(Double translatedValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		@Deprecated // would require reInitializing the deque properties
		public boolean offerLast(Double translatedValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		@Deprecated // would require reInitializing the deque properties
		public Double poll() {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.ArrayDeque#pollFirst()
		 */
		@Override // reInitializes the deque properties
		public Double pollFirst() {
			Double removedItem = super.pollFirst();
			this.timeStampDeque.pollFirst();
			setExtremeValue(removedItem);
			this.sortedValues = null;
			return removedItem;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.ArrayDeque#pollLast()
		 */
		@Override // reInitializes the deque properties
		public Double pollLast() {
			Double removedItem = super.pollLast();
			this.timeStampDeque.pollLast();
			setExtremeValue(removedItem);
			this.sortedValues = null;
			return removedItem;
		}

		@Override
		@Deprecated // would require reInitializing the deque properties
		public void push(Double translatedValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		@Deprecated // would require reInitializing the deque properties
		public Double pop() {
			throw new UnsupportedOperationException();
		}

		@Override
		@Deprecated // would require reInitializing the deque properties
		public Double remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		@Deprecated // would require reInitializing the deque properties
		public Double removeFirst() {
			// use pollFirst()
			throw new UnsupportedOperationException();
		}

		@Override
		@Deprecated // would require reInitializing the deque properties
		public Double removeLast() {
			// use pollLast()
			throw new UnsupportedOperationException();
		}

		/**
		 * Moves all deque entries and leaves a cleared deque.
		 * @param settlementDeque
		 */
		public void addLastByMoving(SettlementDeque settlementDeque) {
			Iterator<Long> iteratorTimeStamp = settlementDeque.timeStampDeque.iterator();
			for (Double value : settlementDeque) {
				this.addLast(value, iteratorTimeStamp.next());
			}
			settlementDeque.clear();
		}

		/**
		 * Move entries until the deque time period is exceeded.
		 * Leave a reduced settlementDeque.
		 * @param settlementDeque
		 * @return the number of deque entries moved
		 */
		public int tryAddLastByMoving(SettlementDeque settlementDeque) {
			int movedCount = 0;
			Iterator<Double> iterator = settlementDeque.iterator();
			for (Iterator<Long> iteratorTimeStamp = settlementDeque.timeStampDeque.iterator(); iterator.hasNext();) {
				final long nextTimeStamp_100ns = iteratorTimeStamp.next();
				if (!this.isAddableInTimePeriod(nextTimeStamp_100ns)) break;

				addLast(iterator.next(), nextTimeStamp_100ns);
				iterator.remove();
				iteratorTimeStamp.remove();
				movedCount++;
			}
			if (movedCount > 0) settlementDeque.reInitialize(movedCount);
			return movedCount;
		}

		/**
		 * @param timeStamp_100ns
		 * @return true if the timestamp will fit in the deque without exceeding the deque's time period.
		 */
		public boolean isAddableInTimePeriod(long timeStamp_100ns) {
			return !this.timeStampDeque.isEmpty() ? timeStamp_100ns - this.timeStampDeque.peekFirst() <= this.timePeriod_100ns : true;
		}

		private void ensureTimePeriod() {
			if (!this.timeStampDeque.isEmpty()) {
				for (int i = 0; i < this.timeStampDeque.size(); i++) {
					if (getDuration_ms() > this.timePeriod_100ns) {
						this.pollFirst(); // polls timestampDeque also
					} else {
						break;
					}
				}
			}
		}

		public long getDuration_ms() {
			return !this.timeStampDeque.isEmpty() ? this.timeStampDeque.peekLast() - this.timeStampDeque.peekFirst() : 0;
		}

		public List<Double> getSortedValues() {
			if (this.sortedValues == null) {
				this.sortedValues = new ArrayList<>(this);
				Collections.sort(this.sortedValues);
			}
			return this.sortedValues;
		}

		/**
		 * Please note: requires a sort over the full deque size.
		 * @param probabilityCutPoint as value between 0 and 1
		 * @return the quantileValue of the current deque contents
		 */
		public double getQuantileValue(double probabilityCutPoint) {
			// IS_SAMPLE
			final int realSize = this.getSortedValues().size();
			if (realSize > 0) {
				if (probabilityCutPoint >= 1. / (realSize + 1) && probabilityCutPoint < (double) realSize / (realSize + 1)) {
					double position = (realSize + 1) * probabilityCutPoint;
					return this.getSortedValues().get((int) position - 1) + (position - (int) position) * (this.getSortedValues().get((int) position) - this.getSortedValues().get((int) position - 1));
				} else if (probabilityCutPoint < 1. / (realSize + 1))
					return this.getSortedValues().get(0);
				else
					return this.getSortedValues().get(realSize - 1);
			} else
				throw new UnsupportedOperationException();
		}

		/**
		 * Please note: requires a sort over the full deque size.
		 * @return NaN for empty deque or a leveled extremum value for comparisons, i.e. a more stable value than the extremum value
		 */
		public double getBenchmarkValue() {
			if (minmaxQuantileDistance == 0.) {
				// bypass deque sort
				return this.extremeValue;
			} else {
				return this.isMinimumExtremum ? this.getQuantileValue(minmaxQuantileDistance) : this.getQuantileValue(1. - minmaxQuantileDistance);
			}
		}

		/**
		 * please note: requires a sort over the full deque size.
		 * @return NaN for empty deque or a leveled translated value for security comparisons which shall ensure that the majority of the values did
		 *         NOT pass a comparison level
		 */
		public double getSecurityValue() {
			if (this.isEmpty()) {
				return Double.NaN;
			} else {
				return this.isMinimumExtremum ? this.getQuantileValue(1. - minmaxQuantileDistance) : this.getQuantileValue(minmaxQuantileDistance);
			}
		}

		/**
		 * Please note: performs a loop over the full deque size.
		 * @return the averageValue of the current deque contents
		 */
		public double getAverageValue() {
			double averageValue = 0.;
			int i = 0;
			for (double translatedValue : this) {
				averageValue += (translatedValue - averageValue) / ++i;
			}
			log.log(FINEST, "averageValue=", averageValue); //$NON-NLS-1$
			return averageValue;
		}

		private void setExtremeValue(double removedValue) {
			if (this.isEmpty()) {
				this.extremeValue = this.isMinimumExtremum ? Double.MAX_VALUE : -Double.MAX_VALUE;
			} else if (removedValue == this.extremeValue) {
				// avoid the calculation of a new extreme value if the neighbor value is identical
				if (removedValue != this.peekFirst()) {
					log.finest(() -> String.format("removedValue=%f extremeValue=%f", removedValue, this.extremeValue)); //$NON-NLS-1$
					this.extremeValue = this.isMinimumExtremum ? getQuantileValue(0.) : getQuantileValue(1.);
					log.finest(() -> String.format("extremeValue=%f", this.extremeValue)); //$NON-NLS-1$
				}
			}
		}

		public List<Double> getTranslatedValues() {
			return Arrays.asList(this.toArray(new Double[0]));
		}

		public List<Long> getTimestamps_100ns() {
			return Arrays.asList(this.timeStampDeque.toArray(new Long[0]));
		}

		public String getFormatedDuration(int index) {
			return StringHelper.getFormatedDuration("HH:mm:ss.SSS", this.getTimestamps_100ns().get(index) / 10); //$NON-NLS-1$
		}

		/**
		 * @return the isMinimumExtremum
		 */
		public boolean isMinimumExtremum() {
			return this.isMinimumExtremum;
		}

		/**
		 * @return the timePeriod_100ns
		 */
		public long getTimePeriod_100ns() {
			return this.timePeriod_100ns;
		}

		/**
		 * @return the extremeValue of the current deque contents
		 */
		public double getExtremeValue() {
			return this.extremeValue;
		}

		/**
		 * @return the startIndex
		 */
		public int getStartIndex() {
			return this.startIndex;
		}

	}

	/**
	 * Check for transitions.
	 * @author Thomas Eickert
	 */
	protected class LevelChecker {
		final private TransitionType	transitionType;

		final private double					absoluteDelta;						// ensures for DELTA_FACTOR transitions a minimum delta between the translated value and
																														// the base value derived from the reference period
		final private double					translatedThresholdValue;
		final private double					translatedRecoveryValue;
		final private boolean					isDeltaFactor;
		final private boolean					isDeltaValue;

		/**
		 * @param transitionRecord
		 * @param transitionType
		 */
		LevelChecker(Record transitionRecord, TransitionType transitionType) {
			this.transitionType = transitionType;

			this.isDeltaFactor = this.transitionType.getValueType() == TransitionValueTypes.DELTA_FACTOR;
			this.isDeltaValue = this.transitionType.getValueType() == TransitionValueTypes.DELTA_VALUE;

			double translatedMaxValue = analyzer.getActiveDevice().translateValue(transitionRecord, transitionRecord.getMaxValue() / 1000.);
			double translatedMinValue = analyzer.getActiveDevice().translateValue(transitionRecord, transitionRecord.getMinValue() / 1000.);

			this.absoluteDelta = analyzer.getSettings().getAbsoluteTransitionLevel() * (translatedMaxValue - translatedMinValue);
			this.translatedThresholdValue = transitionType.getThresholdValue();
			this.translatedRecoveryValue = transitionType.getClassType() != TransitionClassTypes.SLOPE
					? transitionType.getRecoveryValue().orElseThrow(() -> new UnsupportedOperationException(
							"recovery value. transitionID=" + transitionType.getTransitionId())) : 0; //$NON-NLS-1$
		}

		/**
		 * @param translatedValue
		 * @return true if level exceeded (based on isGreater property)
		 */
		boolean isBeyondThresholdLevel(double translatedValue) {
			final boolean isBeyond;
			if (AbstractDetector.this.referenceDeque.isEmpty()) {
				isBeyond = false;
			} else if (this.isDeltaFactor) {
				final double baseValue = AbstractDetector.this.referenceDeque.getBenchmarkValue();
				if (this.transitionType.isGreater())
					isBeyond = translatedValue > baseValue + this.absoluteDelta && translatedValue > baseValue + this.transitionType.getThresholdValue() * Math.abs(baseValue);
				else
					isBeyond = translatedValue < baseValue - this.absoluteDelta && translatedValue < baseValue + this.transitionType.getThresholdValue() * Math.abs(baseValue);
			} else if (this.isDeltaValue) {
				final double baseValue = AbstractDetector.this.referenceDeque.getBenchmarkValue();
				if (this.transitionType.isGreater())
					isBeyond = translatedValue > baseValue + this.translatedThresholdValue;
				else
					isBeyond = translatedValue < baseValue + this.translatedThresholdValue;
			} else if (this.transitionType.getValueType() == TransitionValueTypes.UPPER_THRESHOLD)
				isBeyond = translatedValue > this.translatedThresholdValue;
			else if (this.transitionType.getValueType() == TransitionValueTypes.LOWER_THRESHOLD)
				isBeyond = translatedValue < this.translatedThresholdValue;
			else
				throw new UnsupportedOperationException();
			return isBeyond;
		}

		/**
		 * @param translatedValue
		 * @return true if level exceeded (based on isGreater property)
		 */
		boolean isBeyondRecoveryLevel(double translatedValue) {
			final boolean isBeyond;
			if (this.isDeltaFactor) {
				final double baseValue = AbstractDetector.this.referenceDeque.getBenchmarkValue();
				isBeyond = this.transitionType.isGreater() ? translatedValue < baseValue + this.transitionType.getRecoveryValue().orElse(null) * baseValue
						: translatedValue > baseValue + this.transitionType.getRecoveryValue().orElse(null) * baseValue;
			} else if (this.isDeltaValue) {
				final double baseValue = AbstractDetector.this.referenceDeque.getBenchmarkValue();
				isBeyond = this.transitionType.isGreater() ? translatedValue < baseValue + this.translatedRecoveryValue
						: translatedValue > baseValue + this.translatedRecoveryValue;
			} else {
				isBeyond = this.transitionType.isGreater() ? translatedValue < this.translatedRecoveryValue : translatedValue > this.translatedRecoveryValue;
			}
			return isBeyond;
		}
	}

	protected final Analyzer	analyzer;
	/**
	 * Cache settings value due to frequent access.
	 */
	protected final double		minmaxQuantileDistance;

	public AbstractDetector(Analyzer analyzer) {
		this.analyzer = analyzer;
		this.minmaxQuantileDistance = analyzer.getSettings().getMinmaxQuantileDistance();
	}

	@Override
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.referenceDeque != null && this.thresholdDeque != null && this.recoveryDeque != null && this.recoveryDeque != null && this.previousTriggerState != null) {
			sb.append(String.format("%s > %s ", this.previousTriggerState, this.triggerState)); //$NON-NLS-1$
			sb.append(this.previousTriggerState != null && this.previousTriggerState == TriggerState.WAITING && !this.thresholdDeque.isEmpty()
					? this.referenceDeque.getFormatedDuration(this.referenceDeque.size() - 1)
					: this.previousTriggerState == TriggerState.TRIGGERED && !this.thresholdDeque.isEmpty()
							? this.thresholdDeque.getFormatedDuration(this.thresholdDeque.size() - 1)
							: this.previousTriggerState == TriggerState.RECOVERING && !this.recoveryDeque.isEmpty()
									? this.recoveryDeque.getFormatedDuration(this.recoveryDeque.size() - 1) : "null"); //$NON-NLS-1$
			sb.append(String.format(": referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", this.referenceDeque.size(), this.thresholdDeque.size(), this.recoveryDeque.size())) //$NON-NLS-1$
					.append(GDE.STRING_NEW_LINE);
			sb.append(String.format("%38s reference bench=%.1f extreme=", "", this.referenceDeque.getBenchmarkValue()) + this.referenceDeque.getExtremeValue() + this.referenceDeque.getTranslatedValues()); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(GDE.STRING_BLANK_AT_BLANK + this.referenceDeque.getTimestamps_100ns()).append(GDE.STRING_NEW_LINE);
			sb.append(String.format("%38s threshold bench=%.1f extreme=", "", this.thresholdDeque.getBenchmarkValue()) + this.thresholdDeque.getExtremeValue() + this.thresholdDeque.getTranslatedValues()); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(GDE.STRING_BLANK_AT_BLANK + this.thresholdDeque.getTimestamps_100ns());
			if (!this.recoveryDeque.isEmpty()) {
				sb.append(GDE.STRING_NEW_LINE);
				sb.append(String.format("%38s recovery  bench=%.1f extreme=", "", this.recoveryDeque.getBenchmarkValue()) + this.recoveryDeque.getExtremeValue() + this.recoveryDeque.getTranslatedValues()); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(GDE.STRING_BLANK_AT_BLANK + this.recoveryDeque.getTimestamps_100ns());
			}
		}
		return sb.toString();
	}

}
