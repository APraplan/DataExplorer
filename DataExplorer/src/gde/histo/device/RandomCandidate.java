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

package gde.histo.device;

import java.util.Arrays;
import java.util.Random;

import gde.log.Logger;


/**
 * A sample candidate chosen by a randomized selection.
 * @author Thomas Eickert (USER)
 */
public final class RandomCandidate extends Candidate {
	private final static String		$CLASS_NAME				= RandomCandidate.class.getName();
	private final static Logger		log								= Logger.getLogger($CLASS_NAME);

	private final int							samplingTimespan_ms;
	private final MaxMinObserver	maxMinObserver;

	private int										nextStartTimeStamp_ms;															// the next sampling period start time (is always in the future)
	private int										nextSamplingTimeStamp_ms;														// the sampling time in the current sampling period
	private boolean								isSampleTimePassed;
	private int										timeSpanSamplingCount;
	private boolean								isNewTimeSpan;
	private int										overSamplingCount	= 0;															// timeSpan counter

	private boolean								isMinMax					= false;
	private boolean								isRandomCandidate	= false;

	private enum Action {
		/**
		 * predecessor points are not used as a sample
		 */
		DISCARD,
		/**
		 * predecessor points are a mandatory sample
		 */
		FORCE_RELEASE,
		/**
		 * points are a candidate for release but may be discarded to compensate an oversampling action;
		 * predecessor points must NOT be minmax points
		 */
		COMPENSATE_RELEASE
	}

	/**
	 * @param points is the array of measurement points holding the current values corresponding to the timestamp
	 * @param newSamplingTimespan_ms
	 * @param newMaxMinObserver
	 */
	public RandomCandidate(int[] points, int newSamplingTimespan_ms, MaxMinObserver newMaxMinObserver) {
		super(points);
		this.samplingTimespan_ms = newSamplingTimespan_ms;
		this.maxMinObserver = newMaxMinObserver;
	}

	@Override
	@Deprecated
	public void processSample(int[] newPoints, long newTimeStep_ms) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("timeStep_ms=%,d isSampleTimePassed=%b isRandomCandidate=%b", this.sampleTimeStep_ms, this.isSampleTimePassed, this.isRandomCandidate));
		sb.append("\n").append(String.format("nextStartTimeStamp_ms=%,d nextSamplingTimeStamp_ms=%,d isMinMax=%b timeSpanSamplingCount=%d", this.nextStartTimeStamp_ms, this.nextSamplingTimeStamp_ms, this.isMinMax, this.timeSpanSamplingCount));
		if (this.samplePoints != null)
			sb.append("\n").append(Arrays.toString(this.samplePoints));
		else
			sb.append("\n").append("no points");
		return sb.toString();
	}

	/**
	 * @param newPoints
	 * @param newTimeStep_ms
	 * @param previousCandidate
	 * @param rand
	 * @return true if the candidate is a valid sample
	 */
	public boolean processPoints(long newTimeStep_ms, RandomCandidate previousCandidate, Random rand) {
		initialize(newTimeStep_ms, previousCandidate, rand);

		boolean isValidSample = isValidSample(previousCandidate);

		if (isValidSample) {
			if (previousCandidate.nextStartTimeStamp_ms < this.nextStartTimeStamp_ms) // sampling timespan group has changed
				++previousCandidate.timeSpanSamplingCount; // sample belongs to the last timespan
			else
				++this.timeSpanSamplingCount;
		}

		log.finer(() -> String.format("%,12d isValidSample=%b", previousCandidate.sampleTimeStep_ms, isValidSample));
		return isValidSample;
	}

	/**
	 * Initialize a sample candidate based on the last candidate data.
	 * Decide if a new sampling period starts.
	 * @param newPoints
	 * @param newTimeStep_ms
	 * @param previousCandidate
	 * @param rand
	 */
	private void initialize(long newTimeStep_ms, RandomCandidate previousCandidate, Random rand) {
		if (newTimeStep_ms >= previousCandidate.nextStartTimeStamp_ms) { // lastTimeStep was the last in the current sampling timespan
			this.overSamplingCount = this.timeSpanSamplingCount > 0 ? this.timeSpanSamplingCount - 1 : 0;
			this.isNewTimeSpan = true;

			this.nextStartTimeStamp_ms = previousCandidate.nextStartTimeStamp_ms + this.samplingTimespan_ms;
			this.nextSamplingTimeStamp_ms = previousCandidate.nextStartTimeStamp_ms + rand.nextInt(this.samplingTimespan_ms);
			this.isSampleTimePassed = false;
			this.timeSpanSamplingCount = 0;
			log.finer(() -> "******************** " + String.format("timeStep_ms=%d nextSamplingTimeStamp_ms=%d", newTimeStep_ms, this.nextSamplingTimeStamp_ms));

			this.isRandomCandidate = newTimeStep_ms >= this.nextSamplingTimeStamp_ms;
			this.isMinMax = this.maxMinObserver.update(newTimeStep_ms, previousCandidate.samplePoints);

			// the last sampling time span might not have a sample in case of a very late next sampling time stamp
			if (previousCandidate.timeSpanSamplingCount == 0 && previousCandidate.nextStartTimeStamp_ms != 0) { // never take the very first record
				previousCandidate.isRandomCandidate = true;
			}
		} else {
			this.overSamplingCount = previousCandidate.overSamplingCount;
			this.isNewTimeSpan = false;

			this.nextStartTimeStamp_ms = previousCandidate.nextStartTimeStamp_ms;
			this.nextSamplingTimeStamp_ms = previousCandidate.nextSamplingTimeStamp_ms;
			this.isSampleTimePassed = previousCandidate.isSampleTimePassed || previousCandidate.isRandomCandidate;
			this.timeSpanSamplingCount = previousCandidate.timeSpanSamplingCount;

			this.isRandomCandidate = !this.isSampleTimePassed && newTimeStep_ms >= this.nextSamplingTimeStamp_ms;
			this.isMinMax = this.maxMinObserver.update(newTimeStep_ms, previousCandidate.samplePoints);
		}

		this.sampleTimeStep_ms = newTimeStep_ms;
		if (this.isMinMax || this.timeSpanSamplingCount == 0) { // only in these cases the candidate might be promoted to a valid sample
			// copying makes the object immutable in term of the value set
			this.samplePoints = Arrays.copyOf(points, points.length);
		} else {
			// bypass copying the points for performance improvement
			this.samplePoints = null;
		}

		log.finer(() -> String.format("timeStep_ms=%,12d isSampleTimePassed=%b isRandomCandidate=%b", newTimeStep_ms, this.isSampleTimePassed, this.isRandomCandidate));
	}

	/**
	 * @param lastItem
	 * @return the activity to be performed with the values of the last time step
	 */
	private boolean isValidSample(RandomCandidate lastItem) {
		Action predecessorAction;
		if (lastItem.isMinMax && lastItem.isRandomCandidate) {
			if (this.isMinMax)
				if (this.isRandomCandidate)
					predecessorAction = Action.FORCE_RELEASE; // {}
				else {
					if (this.maxMinObserver.isLastAutonomousExtremum())
						predecessorAction = Action.FORCE_RELEASE;
					else
						predecessorAction = Action.DISCARD;
				}
			else
				predecessorAction = Action.FORCE_RELEASE; // {}
		} else if (lastItem.isMinMax && !lastItem.isRandomCandidate) {
			if (this.isMinMax)
				if (this.isRandomCandidate)
					predecessorAction = Action.DISCARD;
				else {
					if (this.maxMinObserver.isLastAutonomousExtremum())
						predecessorAction = Action.FORCE_RELEASE;
					else
						predecessorAction = Action.DISCARD;
				}
			else
				predecessorAction = Action.FORCE_RELEASE; // OVERSAMPLING;
		} else if (!lastItem.isMinMax && lastItem.isRandomCandidate) {
			if (this.isMinMax)
				if (this.isRandomCandidate)
					predecessorAction = Action.COMPENSATE_RELEASE; // ()
				else
					predecessorAction = Action.DISCARD;
			else
				predecessorAction = Action.COMPENSATE_RELEASE; // ()
		} else if (!lastItem.isMinMax && !lastItem.isRandomCandidate) {
			predecessorAction = Action.DISCARD;
		} else {
			throw new UnsupportedOperationException("is never reached");
		}
		log.finer(() -> String.format("action=%-11s %,12d isLastMinMax=%b isLastRandomCandidate=%b isMinMax=%b isRandomCandidate=%b", predecessorAction.toString(), lastItem.sampleTimeStep_ms, lastItem.isMinMax, lastItem.isRandomCandidate, this.isMinMax, this.isRandomCandidate));

		return predecessorAction == Action.FORCE_RELEASE || predecessorAction == Action.COMPENSATE_RELEASE && lastItem.timeSpanSamplingCount <= 0;
	}

	/**
	 * @return the number of oversamplings in the past sampling timespan (group change)
	 */
	public int getOverSamplingCount() {
		return this.overSamplingCount;
	}

	/**
	 * @return true if a new sampling timespan has just started (group change)
	 */
	public boolean isNewTimeSpan() {
		return this.isNewTimeSpan;
	}

}
