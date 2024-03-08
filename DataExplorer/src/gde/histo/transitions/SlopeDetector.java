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

import static gde.histo.transitions.AbstractDetector.TriggerState.TRIGGERED;
import static gde.histo.transitions.AbstractDetector.TriggerState.WAITING;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;

import gde.GDE;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.TransitionType;
import gde.histo.transitions.GroupTransitions.TransitionChronicle;
import gde.log.Logger;

/**
 * Analyze a record for transitions defined in the device channel settings.
 * @author Thomas Eickert (USER)
 */
public final class SlopeDetector extends AbstractDetector {
	@SuppressWarnings("hiding")
	final static String			$CLASS_NAME	= SlopeDetector.class.getName();
	@SuppressWarnings("hiding")
	final static Logger			log					= Logger.getLogger($CLASS_NAME);

	private final RecordSet	recordSet;

	public SlopeDetector(RecordSet recordSet) {
		super(recordSet.getAnalyzer());
		this.recordSet = recordSet;
	}

	/**
	 * Apply transitionType definitions to the measurement data.
	 * Add all the transitions identified.
	 * Measurements with null values are ignored.
	 * @param record
	 * @param transitionType
	 * @return all transitions with the key thresholdStartTimestamp_ms
	 */
	public TransitionChronicle findTransitions(Record record, TransitionType transitionType) {
		this.triggerState = WAITING;
		initializeDeques(record, transitionType);

		return findSlopeTransitions(record, transitionType);
	}

	/**
	 * @param record
	 * @param transitionType
	 * @return all transitions with the key thresholdStartTimestamp_ms
	 */
	private TransitionChronicle findSlopeTransitions(Record record, TransitionType transitionType) {
		TransitionChronicle transitions = new TransitionChronicle();
		IDevice device = analyzer.getActiveDevice();

		LevelChecker levelChecker = new LevelChecker(record, transitionType);
		for (int i = 0; i < record.realSize(); i++) {
			if (record.elementAt(i) == null) break;
			long timeStamp_100ns = (long) (record.getTime_ms(i) * 10.);
			double translatedValue = device.translateValue(record, record.elementAt(i) / 1000.);
			switch (this.triggerState) {
			case WAITING:
				this.previousTriggerState = this.triggerState;
				boolean isThresholdLevel = levelChecker.isBeyondThresholdLevel(translatedValue);
				// check if the current value is beyond the threshold and if the majority of the reference values did not pass the threshold
				if (isThresholdLevel && !this.referenceDeque.isAddableInTimePeriod(timeStamp_100ns) && !levelChecker.isBeyondThresholdLevel(this.referenceDeque.getSecurityValue())) {
					this.triggerState = TRIGGERED;
					this.thresholdDeque.initialize(i);
					this.thresholdDeque.addLast(translatedValue, timeStamp_100ns);
					log.finer(() -> Integer.toString(transitionType.getTransitionId()) + this);
				} else {
					this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
				}
				break;
			case TRIGGERED:
				this.previousTriggerState = this.triggerState;
				boolean isPersistentTrigger = levelChecker.isBeyondThresholdLevel(translatedValue);
				if (!this.thresholdDeque.isAddableInTimePeriod(timeStamp_100ns)) {
					// final check if the majority of the reference values did not pass the threshold and if the majority of the threshold values passed the
					// threshold
					if (!levelChecker.isBeyondThresholdLevel(this.referenceDeque.getSecurityValue())) {
						this.triggerState = WAITING;
						log.finer(() -> Integer.toString(transitionType.getTransitionId()) + this);
						Transition transition = new Transition(this.referenceDeque.startIndex, this.referenceDeque.size(), this.thresholdDeque.startIndex,
								this.thresholdDeque.size(), record, transitionType);
						transitions.put(transition.getThresholdStartTimeStamp_ms(), transition);
						log.log(FINE, GDE.STRING_GREATER, transition);
						log.finer(() -> Integer.toString(transitionType.getTransitionId()) + this);
						this.thresholdDeque.clear();
						this.referenceDeque.initialize(i);
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					} else {
						log.warning(() -> String.format("%d trigger security check provoked a fallback %s: translatedValue=%f  thresholdAverage=%f", //$NON-NLS-1$
								transitionType.getTransitionId(), this.thresholdDeque.getFormatedDuration(this.thresholdDeque.size() - 1), translatedValue, this.thresholdDeque.getAverageValue()));
						this.triggerState = WAITING;
						log.log(FINER, Integer.toString(transitionType.getTransitionId()), this);
						this.referenceDeque.initialize(i - this.thresholdDeque.size());
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
						this.thresholdDeque.clear();
					}
				} else if (!isPersistentTrigger) { // threshold must conform perfectly to the threshold requirements which are level and time
					this.triggerState = WAITING;
					log.log(FINE, " !isPersistentTrigger " + transitionType.getTransitionId(), this); //$NON-NLS-1$
					this.referenceDeque.initialize(i - this.thresholdDeque.size());
					this.referenceDeque.addLastByMoving(this.thresholdDeque);
					this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
				} else {
					this.thresholdDeque.addLast(translatedValue, timeStamp_100ns); // go on and try to confirm the trigger
				}
				break;
			case RECOVERING: // is not relevant for slope
				break;
			} // end switch
		} // end for
		// do not take the last transition (differs from peak / pulse) because there is no recovery time
		return transitions;
	}

	/**
	 * @param transitionRecord
	 * @param transitionType
	 */
	private void initializeDeques(Record transitionRecord, TransitionType transitionType) {
		final int referenceDequeSize = (int) (transitionType.getReferenceTimeMsec() / this.recordSet.getAverageTimeStep_ms());
		final int thresholdDequeSize = (int) (transitionType.getThresholdTimeMsec() / this.recordSet.getAverageTimeStep_ms());

		this.referenceDeque = new SettlementDeque(referenceDequeSize, transitionType.isGreater(), transitionType.getReferenceTimeMsec() * 10);
		this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
		this.recoveryDeque = new SettlementDeque(1, true, 1); // initialize an empty deque just for avoiding a null reference
		this.referenceDeque.initialize(0);

		int descriptionCutPoint = transitionRecord.getParent().getDescription().indexOf("\r"); //$NON-NLS-1$
		if (descriptionCutPoint < 0) descriptionCutPoint = 11;
		log.log(FINEST, transitionType.getTransitionId() + "  " + transitionRecord.getParent().getName() + " " + " " //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				+ transitionRecord.getParent().getDescription().substring(0, descriptionCutPoint) + String.format(" %s initialized: referenceDequeSize=%d  thresholdDequeSize=%d", transitionRecord.getName(), referenceDequeSize, thresholdDequeSize)); //$NON-NLS-1$
	}

}
