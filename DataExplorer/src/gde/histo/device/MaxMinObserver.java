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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gde.log.Logger;

/**
 * Provide information about the max and min value sets based on the point sets delivered.
 * @author Thomas Eickert (USER)
 */
public class MaxMinObserver {
	private final static String	$CLASS_NAME			= MaxMinObserver.class.getName();
	private final static Logger	log							= Logger.getLogger($CLASS_NAME);

	protected final int[]				points;
	protected final int[]				minPoints;
	protected final int[]				maxPoints;

	// keep the information which value was identified as a new max / min value
	List<Integer>								thisMaxIndices	= new ArrayList<>();
	List<Integer>								lastMaxIndices	= new ArrayList<>();
	List<Integer>								thisMinIndices	= new ArrayList<>();
	List<Integer>								lastMinIndices	= new ArrayList<>();

	/**
	 * true if the last points item has min/max values at positions where the current item has not
	 */
	private boolean							isLastAutonomousExtremum;
	/**
	 * true if the current candidate has changed the max or min value set
	 */
	private boolean							isThisChanged;
	public MaxMinObserver(int[] points, int[] newMaxPoints, int[] newMinPoints) {
		this.points = points;
		this.maxPoints = new int[newMaxPoints.length];
		this.minPoints = new int[newMinPoints.length];
		setMaxMinPoints(newMaxPoints, newMinPoints);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("max ").append(Arrays.toString(this.maxPoints)).append("\n");
		sb.append("min ").append(Arrays.toString(this.minPoints)).append("\n");
		sb.append("maxIndices ").append(Arrays.toString(this.lastMaxIndices.toArray())).append("\n");
		sb.append("minIndices ").append(Arrays.toString(this.lastMinIndices.toArray())).append("\n");
		sb.append(String.format("isThisChanged=%b isLastAutonomousExtremum=%b", this.isThisChanged, this.isLastAutonomousExtremum));
		return sb.toString();
	}

	/**
	 * Set the lastAutonomousExtremum property.
	 * @param timeStep_ms
	 * @param lastPoints
	 * @return true if a new max or min value was found in the point set
	 */
	public boolean update(long timeStep_ms, int[] lastPoints) {
		boolean isLastChanged = this.isThisChanged;
		this.isThisChanged = false;
		this.isLastAutonomousExtremum = false;
		{ // swap identifier lists
			if (isLastChanged) {
				this.lastMaxIndices = this.thisMaxIndices;
				this.lastMinIndices = this.thisMinIndices;
				this.thisMaxIndices = new ArrayList<>();
				this.thisMinIndices = new ArrayList<>();
			}
		}
		for (int i = 0; i < this.points.length; i++) {
			int point = this.points[i];
			if (point > this.maxPoints[i]) {
				this.thisMaxIndices.add(i);
				this.maxPoints[i] = point;
				this.isThisChanged = true;
			} else if (point < this.minPoints[i]) {
				this.thisMinIndices.add(i);
				this.minPoints[i] = point;
				this.isThisChanged = true;
			}
		}
		log.finer(() -> String.format("timeStep_ms=%,12d newMinMax=%b %s", timeStep_ms, this.isThisChanged, Arrays.toString(this.points)));
		if (isLastChanged && this.isThisChanged) setLastAutonomousExtremum(timeStep_ms, lastPoints);
		return this.isThisChanged;
	}

	/**
	 * Checks the new max/min values found in the last point set.
	 * An autonomous extremum is a new extremum which does not occur in the current points.
	 * In this case we need the last point set for oversampling in order not to miss an extremum value.
	 * @param timeStep_ms
	 * @param lastPoints
	 */
	private void setLastAutonomousExtremum(long timeStep_ms, int[] lastPoints) {
		{
			List<Integer> autonomousIndices = new ArrayList<>();
			for (int j = 0; j < this.lastMaxIndices.size(); j++) {
				int i = this.lastMaxIndices.get(j);
				if (this.points[i] < lastPoints[i]) autonomousIndices.add(i);
			}
			if (!autonomousIndices.isEmpty()) {
				this.isLastAutonomousExtremum = true;
				log.finer(() -> String.format("%,12d  autonomous last max found %s from %s", timeStep_ms, Arrays.toString(autonomousIndices.toArray()), Arrays.toString(this.lastMaxIndices.toArray())));
			}
		}
		{
			List<Integer> autonomousIndices = new ArrayList<>();
			for (int j = 0; j < this.lastMinIndices.size(); j++) {
				int i = this.lastMinIndices.get(j);
				if (this.points[i] > lastPoints[i]) autonomousIndices.add(i);
			}
			if (!autonomousIndices.isEmpty()) {
				this.isLastAutonomousExtremum = true;
				log.finer(() -> String.format("%,12d  autonomous last min found %s from %s", timeStep_ms, Arrays.toString(autonomousIndices.toArray()), Arrays.toString(this.lastMinIndices.toArray())));
			}
		}
	}

	public void setMaxMinPoints(int[] newMaxPoints, int[] newMinPoints) {
		if (this.maxPoints.length != newMaxPoints.length || this.minPoints.length != newMinPoints.length) {
			throw new UnsupportedOperationException();
		} else {
			// the max/min points shall identify the samples with hold these max/min values - so they must diverge a bit (+-1)
			for (int i = 0; i < newMaxPoints.length; i++) {
				int value = newMaxPoints[i];
				this.maxPoints[i] = value == Integer.MIN_VALUE ? value : value - 1;
			}
			for (int i = 0; i < newMinPoints.length; i++) {
				int value = newMinPoints[i];
				this.minPoints[i] = value == Integer.MAX_VALUE ? value : value + 1;
			}
		}
	}

	public void resetMaxMinPoints() {
		Arrays.fill(this.maxPoints, Integer.MIN_VALUE);
		Arrays.fill(this.minPoints, Integer.MAX_VALUE);
	}

	public boolean isLastAutonomousExtremum() {
		return this.isLastAutonomousExtremum;
	}
}
