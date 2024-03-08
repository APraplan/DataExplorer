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

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

import gde.log.Level;
import gde.utils.LocalizedDateTime;
import gde.utils.LocalizedDateTime.DateTimePattern;
import gde.utils.LocalizedDateTime.DurationPattern;

/**
 * TimeSteps class handles all the time steps of a record set or a record of part of compare set
 * @author Winfried Brügmann
 */
public class TimeSteps extends Vector<Long> {
	final static String												$CLASS_NAME				= RecordSet.class.getName();
	final static long													serialVersionUID	= 26031957;
	final static Logger												log								= Logger.getLogger(RecordSet.class.getName());

	final boolean															isConstant;																											// true if the time step is constant and the consumed time is a number of measurement points * timeStep_ms
	long																			startTimeStamp_ms	= 0;

	/**
	 * Constructs a new TimeSteps class, a give time step greater than 0 signals that the time step is constant between measurement points
	 * This class should hide time step calculations for constant or individual time steps of a device.
	 * @param newTimeStep_ms
	 */
	public TimeSteps(double newTimeStep_ms) {
		super(newTimeStep_ms < 0 ? 555 : 5);
		this.isConstant = newTimeStep_ms > 0;
		if (this.isConstant) super.add(Double.valueOf(newTimeStep_ms * 10).longValue());
		this.startTimeStamp_ms = new Date().getTime();
	}

	/**
	 * Constructs a new TimeSteps class, a give time step greater than 0 signals that the time step is constant between measurement points
	 * This class should hide time step calculations for constant or individual time steps of a device.
	 * @param newTimeStep_ms
	 * @param initialCapacity
	 */
	public TimeSteps(double newTimeStep_ms, int initialCapacity) {
		super(newTimeStep_ms < 0 ? initialCapacity : 5);
		this.isConstant = newTimeStep_ms > 0;
		if (this.isConstant) super.add(Double.valueOf(newTimeStep_ms * 10).longValue());
		this.startTimeStamp_ms = new Date().getTime();
	}

	/**
	 * copy constructor
	 */
	private TimeSteps(TimeSteps toBeClonedTimeSteps) {
		super(toBeClonedTimeSteps);
		this.isConstant = toBeClonedTimeSteps.isConstant;
		this.startTimeStamp_ms = toBeClonedTimeSteps.startTimeStamp_ms;
	}

	/**
	 * copy constructor
	 */
	private TimeSteps(TimeSteps toBeClonedTimeSteps, int index, boolean isFromBegin) {
		super(toBeClonedTimeSteps);
		this.clear();
		if (!(this.isConstant = toBeClonedTimeSteps.isConstant)) {
			this.startTimeStamp_ms = isFromBegin ? toBeClonedTimeSteps.startTimeStamp_ms + toBeClonedTimeSteps.get(index) / 10 : toBeClonedTimeSteps.startTimeStamp_ms;
			if (isFromBegin) {
				long cutOffVal = toBeClonedTimeSteps.get(index);
				for (int i = index; i < toBeClonedTimeSteps.elementCount; i++) {
					super.add(toBeClonedTimeSteps.get(i) - cutOffVal);
				}
			}
			else {
				for (int i = 0; i < index; i++) {
					this.add(toBeClonedTimeSteps.get(i));
				}
			}
		}
		else {
			this.startTimeStamp_ms = toBeClonedTimeSteps.startTimeStamp_ms;
			this.add(toBeClonedTimeSteps.get(0));
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.toString());
	}

	/**
	 * overwritten clone method
	 */
	@Override
	public synchronized TimeSteps clone() {
		super.clone();
		return new TimeSteps(this);
	}

	/**
	 * clone method re-writes time steps
	 * - if isFromBegin == true, the given index is the index where the record starts after this operation
	 * - if isFromBegin == false, the given index represents the last data point index of the records.
	 */
	public synchronized TimeSteps clone(int index, boolean isFromBegin) {
		return new TimeSteps(this, index, isFromBegin);
	}

	/**
	 * query the delta time in msec between two index positions
	 * @param indexStart
	 * @param indexEnd
	 * @return delta time in msec
	 */
	public double getDeltaTime(int indexStart, int indexEnd) {
		if (indexStart < 0) {
			log.log(Level.WARNING, "indexStart < 0 " + indexStart); //$NON-NLS-1$
			indexStart = 0;
		}
		if (indexEnd > this.elementCount - 1) {
			log.log(Level.WARNING, "indexEnd > this.elementCount - 1 " + indexEnd); //$NON-NLS-1$
			indexEnd = this.elementCount - 1;
		}
		indexEnd = indexEnd > this.elementCount - 1 ? this.elementCount - 1 : indexEnd;
		return (this.get(indexEnd) - this.get(indexStart)) / 10.0;
	}

	/**
	 * query time at an index position
	 * @param index
	 * @return time fit to index
	 */
	public double getTime_ms(int index) {
		synchronized (this) {
			return this.isConstant ? (index == 0 ? 0.0 : this.get(0) / 10.0 * index) : (index < 0 ? this.firstElement() : index > elementCount - 1 ? this.lastElement() / 10.0 : this.get(index) / 10.0);
		}
	}

	/**
	 * @param formatPattern
	 * @param index
	 * @param isAbsolute true adds the start timestamp to the timestep value
	 * @return the absolute time at the index position as 25 char formated string
	 */
	public String getFormattedTime(DateTimePattern formatPattern, int index) {
		return String.format("%25s", LocalizedDateTime.getFormatedTime(formatPattern, this.startTimeStamp_ms + (long) getTime_ms(index))); //$NON-NLS-1$
	}

	/**
	 * @param formatPattern
	 * @param index
	 * @return the time duration value at the index position as 25 char formated string
	 */
	public String getFormattedDuration(DurationPattern formatPattern, int index) {
		return String.format("%25s", LocalizedDateTime.getFormatedDuration(formatPattern, (long) this.getTime_ms(index))); //$NON-NLS-1$
	}

	/**
	 * add a new time step without conversion overhead.
	 * @param time_100ns in 0.1 ms (divide by 10 to get ms)
	 * @return true if add was successful
	 */
	public synchronized boolean addRaw(long time_100ns) {
		synchronized (this) {
			return this.isConstant ? true : super.add(time_100ns);
		}
	}

	/**
	 * add a new time step
	 * @param value_ms in ms
	 * @return true if add was successful
	 */
	public synchronized boolean add(double value_ms) {
		synchronized (this) {
			return this.isConstant ? true : super.add((long) (value_ms * 10));
		}
	}

	/**
	 * @return the const. time step in msec
	 */
	public double getAverageTimeStep_ms() {
		try {
			return this.isConstant ? this.getTime_ms(1) : (double) this.lastElement() / elementCount / 10.0;
		}
		catch (Exception e) {
			// a redraw event where the record set has no records
			return 0.0;
		}
	}

	/**
	 * @return the minimum time step (timespan) in msec
	 */
	public double getMinimumTimeStep_ms() {
		if (this.isConstant) {
			return (long) this.getTime_ms(1);
		}
		else if (this.size() == 0) {
			return 0;
		}
		else {
			long minValue = this.get(0) - 0;
			for (int i = 1; i < this.elementCount; i++) {
				long diff = this.get(i) - this.get(i - 1);
				if (minValue > diff) minValue = diff;
			}
			log.log(Level.FINE, String.format("min=%d  avg=%f", minValue / 10, getAverageTimeStep_ms())); //$NON-NLS-1$
			return minValue / 10.;
		}
	}

	/**
	 * @return the maximum time step (timespan) in msec
	 */
	public double getMaximumTimeStep_ms() {
		if (this.isConstant) {
			return (long) this.getTime_ms(1);
		}
		else if (this.size() == 0) {
			return 0;
		}
		else {
			long maxValue = this.get(0) - 0;
			for (int i = 1; i < this.elementCount; i++) {
				long diff = this.get(i) - this.get(i - 1);
				if (maxValue < diff) maxValue = diff;
			}
			log.log(Level.FINE, String.format("max=%d  avg=%f", maxValue / 10, getAverageTimeStep_ms())); //$NON-NLS-1$
			return maxValue / 10.;
		}
	}

	/**
	 * @return the standard deviation of the time steps (timespans)in msec
	 */
	public double getSigmaTimeStep_ms() {
		if (this.isConstant) {
			return 0;
		}
		else if (this.size() <= 1) {
			return 0;
		}
		else {
			long baseTimeSpan = (long) (getAverageTimeStep_ms());
			double sqSum = (this.get(0) - 0 - baseTimeSpan) * (this.get(0) - 0 - baseTimeSpan);
			for (int i = 1; i < this.elementCount; i++) {
				long diff = this.get(i) - this.get(i - 1);
				sqSum += (diff - baseTimeSpan) * (diff - baseTimeSpan);
			}
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("avg=%f  sigma=%f", getAverageTimeStep_ms(), Math.sqrt(sqSum / (this.elementCount - 1)) / 10)); //$NON-NLS-1$
			return Math.sqrt(sqSum / (this.elementCount - 1)) / 10.;
		}
	}

	/**
	 * @return the isConstant true if time step is a constant value between measurement points
	 */
	public boolean isConstant() {
		return isConstant;
	}

	/**
	 * @return the maximum time relative to the displayable area
	 */
	public double getMaxTime_ms() {
		double maxTime = 0.0;
		if (isConstant) {
			maxTime = this.get(0) / 10.0;
		}
		else {
			maxTime = this.elementCount > 1 ? this.lastElement() / 10.0 : 0.0;
		}
		//return isConstant ? this.get(0)*this.parent.realSize()/10.0 : this.lastElement()/10.0;
		return maxTime;
	}

	/**
	 * Find the indexes in this time vector where the given time value is placed
	 * In case of the given time in in between two available measurement points both bounding indexes are returned,
	 * only in case where the given time matches an existing entry both indexes are equal.
	 * In cases where the returned indexes are not equal the related point x/y has to be interpolated.
	 * @param time_ms
	 * @return two indexes around given time
	 */
	public int[] findBoundingIndexes(double time_ms) {
		//log.log(Level.INFO, "time_ms = " + time_ms);
		int index1 = 0, index2 = 0;
		if (time_ms > 0 && elementCount > 0) {
			if (isConstant) {
				double position = time_ms / (this.get(0) / 10.0);
				index1 = (int) position;
				index2 = (int) (position + 0.5);
			}
			else {
				int value = Double.valueOf(time_ms * 10.0).intValue();
				for (; index2 < elementCount - 1; index2++) {
					if (value == get(index2)) {
						index1 = index2;
						break;
					}
					else if (value < get(index2)) {
						index1 = index2 > 0 ? index2 - 1 : 0;
						break;
					}
					index1 = index2 + 1;
				}
			}
		}
		//log.log(Level.INFO, index1 + " - " + index2);
		return new int[] { index1, index2 };
	}

	/**
	 * find the index closest to given time in msec
	 * @param time_ms
	 * @return index closest to given time
	 */
	public int findBestIndex(double time_ms) {
		int index = 0;
		if (time_ms > 0) {
			double position = 0.0;
			if (this.isConstant) {
				position = time_ms / (this.get(0) / 10.0);
				index = (int) (position + 0.5);
			}
			else {
				index = (int) (time_ms / (this.lastElement() / (double) (this.elementCount - 1) / 10.0) / 2.0);
				int value = Double.valueOf(time_ms * 10.0).intValue();
				for (; index < elementCount; index++) {
					if (value <= this.get(index)) break;
				}
				if (index + 1 <= this.elementCount && (index > 0 && value <= (this.get(index) + this.get(index - 1)) / 2)) index = index - 1;
			}
		}
		//log.log(Level.INFO, "index=" + index);
		return index;
	}

	/**
	 * @param time_ms
	 * @param comparator reflects the sort order of the timesteps, e.g. Comparator.naturalOrder(),  Comparator.reverseOrder()
	 * @return the index closest to the given time or 0 if the list is empty
	 */
	public synchronized int getBestIndex(double time_ms, Comparator<Long> comparator) {
		int index = 0;
		if (!this.isEmpty()) {
			if (this.isConstant) {
				if (time_ms > 0) {
					double position = time_ms / (this.get(0) / 10.0);
					index = (int) (position + 0.5);
				}
			}
			else {
				// determine index
				long value_ms = (long) (time_ms * 10.0);
				int result = Collections.binarySearch(this, value_ms, comparator);
				if (result >= 0)
					index = result;
				else {
					index = Math.abs(result + 2);
					// the index is now the lower bound and we want the next index if the value is beyond the mid of the distance to the next value
					if (index < size() - 1 && comparator.compare(value_ms, (this.get(index + 1) + this.get(index)) / 2) > 0) index++;
				}
			}
		}
		// log.log(Level.INFO, "index=" + index);
		return index;
	}

	/**
	 * set absolute start and end time of this record set
	 * @param newStartTimeStamp_ms
	 */
	public void setStartTimeStamp(long newStartTimeStamp_ms) {
		this.startTimeStamp_ms = newStartTimeStamp_ms;
	}

	/**
	 * return the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by the start date time of the dependent record set
	 * @return
	 */
	public long getStartTimeStamp() {
		return this.startTimeStamp_ms;
	}

}
