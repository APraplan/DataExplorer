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

package gde.data;

import java.text.DecimalFormat;
import java.util.Vector;

import gde.data.Record.DataType;

/**
 * Supports all classes which access both the Records class and the TrailRecords class.
 * @author Thomas Eickert (USER)
 */
public abstract class AbstractRecord extends Vector<Integer> {
	private static final long serialVersionUID = 3212164037419263272L;

	protected AbstractRecord() {
		super();
	}

	protected AbstractRecord(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * copy constructor
	 */
	protected AbstractRecord(AbstractRecord record) {
		super(record);
	}

	public abstract double getFactor();

	public abstract double getOffset();

	public abstract double getReduction();

	/**
	 * @return true if the channel item holds bits, e.g. events
	 */
	public abstract boolean isBits();

	/**
	 * @return true if the channel item holds tokens, e.g. warnings
	 */
	public abstract boolean isTokens();

	public abstract int getOrdinal();

	public abstract String getName();

	public abstract DataType getDataType();

	public abstract boolean isVisible();

	public abstract void setVisible(boolean enabled);

	public abstract boolean isPositionLeft();

	public abstract void setPositionLeft(boolean enabled);

	/**
	 * @return the CSV value (e.g. 0,0,0 for black)
	 */
	public abstract String getRGB();

	/**
	 * @param rgb is the CSV value (e.g. 0,0,0 for black)
	 */
	public abstract void setRGB(String newColor);

	public abstract boolean isRoundOut();

	public abstract void setRoundOut(boolean enabled);

	public abstract boolean isStartpointZero();

	public abstract void setStartpointZero(boolean enabled);

	public abstract void setStartEndDefined(boolean enabled);

	public abstract void setMaxScaleValue(double newMaxScaleValue);

	public abstract void setMinScaleValue(double newMinScaleValue);

	public abstract boolean isStartEndDefined();

	/**
	 * sets the min-max values as displayed 4.0 - 200.5
	 * @param enabled
	 * @param newMinScaleValue
	 * @param newMaxScaleValue
	 */
	public abstract void setStartEndDefined(boolean enabled, double newMinScaleValue, double newMaxScaleValue);

	public abstract int getLineWidth();

	public abstract void setLineWidth(int newLineWidth);

	public abstract int getLineStyle();

	public abstract void setLineStyle(int newLineStyle);

	public abstract int getNumberFormat();

	public abstract void setNumberFormat(int newNumberFormat);

	/**
	 * set instance value directly without logic
	 * @param newNumberFormat
	 */
	public abstract void setNumberFormatDirect(int newNumberFormat);

	public abstract AbstractRecordSet getAbstractParent();

	public abstract boolean isDisplayable();

	public abstract double getMaxScaleValue();

	public abstract double getMinScaleValue();

	/**
	 * @param finalValue is the value to be displayed (without applying a factor or GPS coordinates fraction correction)
	 * @return the translated and decimal formatted value
	 */
	public abstract String getFormattedScaleValue(double finalValue);

	/**
	 * @return true if the record is the scale sync master and is visible
	 */
	public abstract boolean isScaleVisible();

	/**
	 * @return the decimal format of this record (without looking for synced records)
	 */
	public abstract DecimalFormat getRealDf();

	/**
	 * @param realDf is the decimal format of this record (without updating synced records)
	 */
	public abstract void setRealDf(DecimalFormat realDf);

	/**
	 * Determine the property references.
	 * @return the ordinal number of the sync master record or -1 if the record scale is not synchronized
	 */
	public abstract int getSyncMasterRecordOrdinal();

	public abstract void setSyncMinMax(int newMin, int newMax);

}
