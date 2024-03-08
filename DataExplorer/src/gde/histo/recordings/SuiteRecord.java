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

package gde.histo.recordings;

import java.util.Vector;

import gde.log.Logger;

/**
 * Data points of one measurement or line or curve.
 * Member of a trail record suite.
 * @author Thomas Eickert (USER)
 */
public final class SuiteRecord extends Vector<Integer> {
	private final static String	$CLASS_NAME				= SuiteRecord.class.getName();
	private static final long		serialVersionUID	= 8757759753520551985L;
	private final static Logger	log								= Logger.getLogger($CLASS_NAME);

	private final int						trailOrdinal;

	private int									maxRecordValue		= Integer.MIN_VALUE;						// max value of the curve
	private int									minRecordValue		= Integer.MAX_VALUE;						// min value of the curve

	public SuiteRecord(int newTrailOrdinal, int initialCapacity) {
		super(initialCapacity);
		this.trailOrdinal = newTrailOrdinal;
	}

	/**
	 * Add a data point to the record and set minimum and maximum.
	 * @param point
	 */
	@Override
	public synchronized void addElement(Integer point) {
		if (point == null) {
			if (this.isEmpty()) {
				this.maxRecordValue = Integer.MIN_VALUE;
				this.minRecordValue = Integer.MAX_VALUE;
			}
		} else {
			if (this.isEmpty())
				this.minRecordValue = this.maxRecordValue = point;
			else {
				if (point > this.maxRecordValue) this.maxRecordValue = point;
				if (point < this.minRecordValue) this.minRecordValue = point;
			}
		}
		super.addElement(point);
		log.finer(() -> this.trailOrdinal + " adding point = " + point); //$NON-NLS-1$
		log.finest(() -> this.trailOrdinal + " minValue = " + this.minRecordValue + " maxValue = " + this.maxRecordValue); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public int getTrailOrdinal() {
		return this.trailOrdinal;
	}

	public int getMaxRecordValue() {
		return this.maxRecordValue;
	}

	public int getMinRecordValue() {
		return this.minRecordValue;
	}

}
