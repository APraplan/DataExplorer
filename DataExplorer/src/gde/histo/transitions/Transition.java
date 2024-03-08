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


import gde.GDE;
import gde.data.Record;
import gde.device.TransitionClassTypes;
import gde.device.TransitionType;
import gde.log.Logger;
import gde.utils.StringHelper;

/**
 * Hold the data for the transition section identified in the data record.
 * The reference, threshold and recovery sections do not overlap.
 * @author Thomas Eickert
 */
public final class Transition {
	final static String						$CLASS_NAME	= Transition.class.getName();
	final static Logger						log					= Logger.getLogger($CLASS_NAME);

	final int											referenceSize;
	final int											thresholdSize;
	final int											recoverySize;
	private final Record					transitionRecord;
	private final TransitionType	transitionType;

	final int											startIndex;
	final int											referenceStartIndex;
	final int											referenceEndIndex;
	final int											thresholdStartIndex;
	final int											thresholdEndIndex;
	final int											recoveryStartIndex;
	final int											recoveryEndIndex;

	/**
	 * Transitions without recovery phase.
	 * @param startIndex is the position where the search started after the previous transition
	 * @param referenceSize
	 * @param thresholdStartIndex
	 * @param thresholdSize
	 * @param transitionRecord
	 * @param transitionType
	 */
	public Transition(int startIndex, int referenceSize, int thresholdStartIndex, int thresholdSize, Record transitionRecord, TransitionType transitionType) {
		this.startIndex = startIndex;
		this.referenceStartIndex = thresholdStartIndex - referenceSize;
		this.referenceSize = referenceSize;
		this.referenceEndIndex = thresholdStartIndex - 1;
		this.thresholdStartIndex = thresholdStartIndex;
		this.thresholdSize = thresholdSize;
		this.transitionRecord = transitionRecord;
		this.transitionType = transitionType;

		this.thresholdEndIndex = thresholdStartIndex + thresholdSize - 1;
		this.recoveryStartIndex = -1;
		this.recoverySize = 0;
		this.recoveryEndIndex = -1;
	}

	/**
	 * Transitions with recovery phase.
	 * @param startIndex is the position where the search started after the previous transition
	 * @param referenceSize
	 * @param thresholdStartIndex
	 * @param thresholdSize
	 * @param recoveryStartIndex is less than zero in case of missing recovery phase
	 * @param recoverySize
	 * @param transitionRecord
	 * @param transitionType
	 */
	public Transition(int startIndex, int referenceSize, int thresholdStartIndex, int thresholdSize, int recoveryStartIndex, int recoverySize, Record transitionRecord, TransitionType transitionType) {
		this.startIndex = startIndex;
		this.referenceStartIndex = thresholdStartIndex - referenceSize;
		this.referenceSize = referenceSize;
		this.referenceEndIndex = thresholdStartIndex - 1;
		this.thresholdStartIndex = thresholdStartIndex;
		this.thresholdSize = thresholdSize;
		this.transitionRecord = transitionRecord;
		this.transitionType = transitionType;

		this.thresholdEndIndex = recoveryStartIndex - 1;
		this.recoveryStartIndex = recoveryStartIndex;
		this.recoverySize = recoverySize;
		this.recoveryEndIndex = recoveryStartIndex + recoverySize - 1;
	}

	public double getTimeStamp_ms(int index) {
		return this.transitionRecord.getTime_ms(index);
	}

	public long getReferenceStartTimeStamp_ms() {
		return (long) this.transitionRecord.getTime_ms(this.referenceStartIndex);
	}

	public long getThresholdStartTimeStamp_ms() {
		return (long) this.transitionRecord.getTime_ms(this.thresholdStartIndex);
	}

	public long getThresholdEndTimeStamp_ms() {
		return (long) this.transitionRecord.getTime_ms(this.thresholdEndIndex);
	}

	/**
	 * @return HH:mm:ss.SSS
	 */
	public String getFormatedDuration(int index) {
		return StringHelper.getFormatedDuration("HH:mm:ss.SSS", (long) this.transitionRecord.getParent().getTime_ms(index)); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.transitionType.getTransitionId()).append(GDE.STRING_BLANK).append(GDE.STRING_BLANK);
		sb.append("threshold=").append(getFormatedDuration(this.thresholdStartIndex)).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$
		sb.append("thresholdStartIndex/size=").append(this.thresholdStartIndex).append(GDE.STRING_OR).append(this.thresholdSize).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$
		sb.append("isPeak=").append(isPeak()).append(GDE.STRING_COMMA_BLANK).append("isSlope=").append(isSlope()).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("referenceStartIndex/size=").append(this.referenceStartIndex).append(GDE.STRING_OR).append(this.referenceSize).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$
		sb.append("reference=").append(getFormatedDuration(this.referenceStartIndex)); //$NON-NLS-1$
		if (this.recoveryStartIndex > 0) {
			sb.append(GDE.STRING_COMMA_BLANK);
			sb.append("recovery=").append(getFormatedDuration(this.recoveryStartIndex)).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$
			sb.append("recoveryStartIndex/size=").append(this.recoveryStartIndex).append(GDE.STRING_OR).append(this.recoverySize).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$
		}
		return sb.toString();
	}

	public boolean isSlope() {
		return this.transitionType.getClassType() == TransitionClassTypes.SLOPE;
	}

	public boolean isPeak() {
		return this.transitionType.getClassType() == TransitionClassTypes.PEAK;
	}

	public boolean isPulse() {
		return this.transitionType.getClassType() == TransitionClassTypes.PULSE;
	}

	public TransitionType getTransitionType() {
		return this.transitionType;
	}

	public int getReferenceStartIndex() {
		return this.referenceStartIndex;
	}

	public int getReferenceEndIndex() {
		return this.referenceEndIndex;
	}

	public int getThresholdStartIndex() {
		return this.thresholdStartIndex;
	}

	public int getThresholdEndIndex() {
		return this.thresholdEndIndex;
	}

	public int getRecoveryStartIndex() {
		return this.recoveryStartIndex;
	}

	public int getRecoveryEndIndex() {
		return this.recoveryEndIndex;
	}

	public int getReferenceSize() {
		return this.referenceSize;
	}

	public int getThresholdSize() {
		return this.thresholdSize;
	}

	public int getRecoverySize() {
		return this.recoverySize;
	}

}
