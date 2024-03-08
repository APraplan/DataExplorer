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

import gde.GDE;
import gde.data.AbstractRecordSet.SyncedRecords;
import gde.data.Record.DataType;
import gde.device.IDevice;
import gde.device.StatisticsType;
import gde.log.Level;
import gde.log.Logger;

/**
 * Supports the GDE kernel record class and the history trail record class.
 * @author Thomas Eickert (USER)
 */
public abstract class CommonRecord extends AbstractRecord {
	private static final String	$CLASS_NAME				= CommonRecord.class.getName();
	private static final long		serialVersionUID	= 26031957;
	private static final Logger	log								= Logger.getLogger($CLASS_NAME);

	/**
	 * for each measurement point in compare set, where time step of measurement points might be individual
	 */
	TimeSteps										timeStep_ms				= null;
	protected IDevice						device;
	/**
	 * is referencing the source position of the record ordinal relative to the initial
	 * device measurement configuration and used to find specific properties
	 */
	protected int								ordinal;

	protected AbstractRecordSet	parent;

	// core fields
	protected String						name;																							// measurement name Höhe
	String											unit;																							// unit [m]
	String											symbol;																						// symbol h
	String											description				= GDE.STRING_BLANK;							// only set if copied into compare set
	protected Boolean						isActive;
	DataType										dataType					= Record.DataType.DEFAULT;
	StatisticsType							statistics				= null;

	protected int								maxValue					= 0;														// max value of the curve
	protected int								minValue					= 0;														// min value of the curve

	int													avgValue					= Integer.MIN_VALUE;						// avarage value (avg = sum(xi)/n)
	int													sigmaValue				= Integer.MIN_VALUE;						// sigma value of data, according a set trigger level if any

	/**
	 * this constructor will create an vector to hold data points in case the initial capacity is > 0
	 * @param newDevice
	 * @param newOrdinal
	 * @param newName
	 * @param newUnit
	 * @param newSymbol
	 * @param isActiveValue
	 * @param newStatistic
	 * @param initialCapacity
	 */
	public CommonRecord(IDevice newDevice, int newOrdinal, String newName, String newSymbol, String newUnit, boolean isActiveValue,
			StatisticsType newStatistic, int initialCapacity) {
		super(initialCapacity);
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, newName + " Record(IDevice, int, String, String, String, boolean, StatisticsType, List<PropertyType>, int)"); //$NON-NLS-1$
		this.device = newDevice;
		this.ordinal = newOrdinal;
		this.name = newName;
		this.symbol = newSymbol;
		this.unit = newUnit;
		this.isActive = isActiveValue;
		this.statistics = newStatistic;
	}

	/**
	 * copy constructor
	 */
	protected CommonRecord(CommonRecord record) {
		super(record);
	}

	/**
	 * add a data point to the record data, checks for minimum and maximum to define display range
	 * @param point
	 */
	public synchronized boolean add(Integer point, double useTimeStep_ms) {
		if (this.timeStep_ms != null) this.timeStep_ms.add(useTimeStep_ms);
		return this.add(point);
	}

	@Override
	public abstract boolean add(Integer point);

	@Override
	public synchronized Integer set(int index, Integer point) {
		final String $METHOD_NAME = "set"; //$NON-NLS-1$
		if (super.size() == 0) {
			this.minValue = this.maxValue = point;
		} else {
			if (point > this.maxValue)
				this.maxValue = point;
			else if (point < this.minValue) this.minValue = point;
		}
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, this.name + " setting point = " + point); //$NON-NLS-1$
		if (log.isLoggable(Level.FINEST))
			log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, this.name + " minValue = " + this.minValue + " maxValue = " + this.maxValue); //$NON-NLS-1$ //$NON-NLS-2$
		return super.set(index, point);
	}

	@Override
	public int getOrdinal() {
		return this.ordinal;
	}

	public void setOrdinal(int newOrdinal) {
		this.ordinal = newOrdinal;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public String getSyncMasterName() {
		StringBuilder sb = new StringBuilder().append(this.name.split(GDE.STRING_BLANK)[0]);
		SyncedRecords<? extends AbstractRecord> syncedRecords = this.getAbstractParent().scaleSyncedRecords;
		if (syncedRecords.get(this.ordinal) != null && syncedRecords.get(this.ordinal).firstElement().getName().split(GDE.STRING_BLANK).length > 1) {
			sb.append(GDE.STRING_BLANK);
			String[] splitName = syncedRecords.get(this.ordinal).firstElement().getName().split(GDE.STRING_BLANK);
			sb.append(splitName.length > 1 ? splitName[1] : GDE.STRING_STAR);
			sb.append(GDE.STRING_DOT);
			sb.append(GDE.STRING_DOT);
			String trailer = GDE.STRING_STAR;
			for (AbstractRecord tmpRecord : syncedRecords.get(this.ordinal)) {
				if (tmpRecord.isDisplayable() && tmpRecord.size() > 1) trailer = tmpRecord.getName();
			}
			sb.append(trailer.split(GDE.STRING_BLANK).length > 1 ? trailer.split(GDE.STRING_BLANK)[1] : GDE.STRING_STAR);
		} else {
			sb.append(GDE.STRING_MESSAGE_CONCAT).append(syncedRecords.get(this.ordinal).lastElement().getName());
		}
		return sb.toString();
	}

	public String getUnit() {
		return this.unit;
	}

	public void setUnit(String newUnit) {
		this.unit = newUnit;
	}

	public String getSymbol() {
		return this.symbol;
	}

	public void setSymbol(String newSymbol) {
		this.symbol = newSymbol;
	}

	public abstract int getMaxValue();

	public abstract int getMinValue();

	public int getRealMaxValue() {
		return this.maxValue;
	}

	public int getRealMinValue() {
		return this.minValue;
	}

	/**
	 * return the 'best fit' number of measurement points in dependency of zoomMode or scopeMode
	 */
	@SuppressWarnings("sync-override")
	@Override
	public abstract int size();

	/**
	 * time calculation needs always the real size of the record
	 * @return real vector size
	 */
	public int realSize() {
		return super.size();
	}

	public Integer getFirst() {
		return super.size() > 0 ? super.get(0) : 0;
	}

	public Integer getLast() {
		return super.size() > 0 ? super.get(super.size() - 1) : 0;
	}

	/**
	 * ET: throws NullPointerException if super.get(index) is null.
	 * in debugging mode, however, the expression 'super.size() != 0 ? super.get(index) : 0' evaluates to null which is correct.
	 * could not clarify the reason for the exception <<<
	 * @param index
	 */
	public Integer realGet(int index) {
		try {
			return super.size() != 0 ? super.get(index) : 0;
		} catch (ArrayIndexOutOfBoundsException e) {
			log.log(Level.WARNING, String.format("%s - %20s: size = %d - indesx = %d", this.parent.name, this.name, super.size(), index));
			return super.size() != 0 ? super.get(index - 1) : 0;
		}
	}

	@Override
	public AbstractRecordSet getAbstractParent() {
		return this.parent;
	}

	/**
	 * @return the isActive
	 */
	public boolean isActive() {
		return this.isActive == null || super.size() == 0 ? false : this.isActive;
	}

	/**
	 * set isActive value
	 */
	public void setActive(Boolean newValue) {
		this.isActive = newValue;
	}

	/**
	 * @return if record data represented by calculation
	 */
	public boolean isCalculation() {
		return this.isActive == null ? true : false;
	}

	/**
	 * return the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by the start date time of this record (set)
	 * @return time stamp of the date and time when the record starts
	 */
	public long getStartTimeStamp() {
		return this.timeStep_ms == null ? this.parent.timeStep_ms.getStartTimeStamp() : this.timeStep_ms.getStartTimeStamp();
	}

	/**
	 * query time step time in mills seconds at index
	 * @return time step in msec
	 */
	public abstract double getTime_ms(int index);

	/**
	 * query time step time in mills seconds at index
	 * @return time step in msec
	 */
	public double getLastTime_ms() {
		return this.timeStep_ms == null ? this.parent.timeStep_ms.lastElement() / 10.0 : this.timeStep_ms.lastElement() / 10.0;
	}

	/**
	 * set the time step in milli seconds, this property is hold local to be independent
	 * @param timeStep_ms the timeStep_ms to set
	 */
	void setTimeStep_ms(double newTimeStep_ms) {
		this.timeStep_ms = new TimeSteps(newTimeStep_ms);
	}

	/**
	 * @return the maximum time of this record, which should correspondence to the last entry in timeSteps
	 */
	public double getMaxTime_ms() {
		return this.timeStep_ms == null ? this.parent.getMaxTime_ms() : this.timeStep_ms.isConstant
				? this.timeStep_ms.getMaxTime_ms() * (this.elementCount - 1) : this.timeStep_ms.getMaxTime_ms();
	}

	/**
	 * Find the indexes in this time vector where the given time value is placed
	 * In case of the given time in in between two available measurement points both bounding indexes are returned,
	 * only in case where the given time matches an existing entry both indexes are equal.
	 * In cases where the returned indexes are not equal the related point x/y has to be interpolated.
	 * @param time_ms
	 * @return two index values around the given time
	 */
	protected int[] findBoundingIndexes(double time_ms) {
		int[] indexs = this.timeStep_ms == null ? this.parent.timeStep_ms.findBoundingIndexes(time_ms) : this.timeStep_ms.findBoundingIndexes(time_ms);
		if (this.elementCount > 0) {
			indexs[0] = indexs[0] > this.elementCount - 1 ? this.elementCount - 1 : indexs[0];
			indexs[1] = indexs[1] > this.elementCount - 1 ? this.elementCount - 1 : indexs[1];
		}
		return indexs;
	}

	/**
	 * @param finalValue is the value to be displayed (without applying a factor or GPS coordinates fraction correction)
	 * @return the translated and decimal formatted value
	 */
	@Override
	public abstract String getFormattedScaleValue(double finalValue);

	/**
	 * reset the min-max-values to enable new settings after re-calculation
	 */
	public void resetMinMax() {
		this.maxValue = 0;
		this.minValue = 0;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.name);
	}

	/**
	 * set new the min-max-values after external re-calculation
	 */
	public void setMinMax(int newMin, int newMax) {
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "setMinMax :  " + newMin + "," + newMax); //$NON-NLS-1$ //$NON-NLS-2$
		this.maxValue = newMax;
		this.minValue = newMin;
	}

	/**
	 * @return the avgValue
	 */
	public int getAvgValue() {
		this.setAvgValue();
		return this.avgValue;
	}

	/**
	 * calculates the avgValue by discarding nulls and zeroes
	 */
	public void setAvgValue() {
		synchronized (this) {
			if (super.size() > 0) {
				long sum = 0;
				int zeroCount = 0;
				for (Integer xi : this) { // ET loops over all elements of the vector
					if (xi != null && xi != 0) {
						sum += xi;
					} else {
						zeroCount++;
					}
				}
				this.avgValue = (super.size() - zeroCount) != 0 ? Long.valueOf(sum / (super.size() - zeroCount)).intValue() : 0; // ET realSize corresponds to
																																																													// the looped elements
			}
		}
	}

	/**
	 * @return the sigmaValue by discarding nulls and zeroes
	 */
	public int getSigmaValue() {
		this.setSigmaValue();
		return this.sigmaValue;
	}

	/**
	 * calculates the sigmaValue by discarding nulls and zeroes
	 */
	public void setSigmaValue() {
		synchronized (this) {
			if (super.size() > 0) {
				double average = this.getAvgValue() / 1000.0;
				double sumPoweredValues = 0;
				int zeroCount = 0;
				for (Integer xi : this) { // ET loops over all elements of the vector
					if (xi != null && xi != 0) { // sigma is based on the same population as avg
						sumPoweredValues += Math.pow(xi / 1000.0 - average, 2);
					} else {
						zeroCount++;
					}
				}
				this.sigmaValue = (super.size() - zeroCount - 1) != 0
						? Double.valueOf(Math.sqrt(sumPoweredValues / (super.size() - zeroCount - 1)) * 1000).intValue() : 0;
				// ET realSize corresponds to the looped elements
			}
		}
	}

	/**
	 * query if the record display scale is synced with an other record
	 * @return the isScaleSynced
	 */
	public boolean isScaleSynced() {
		return this.getAbstractParent().isOneOfSyncableRecord(this.name);
	}

	/**
	 * @return true if the record represents a scale synchronize master record
	 */
	public boolean isScaleSyncMaster() {
		return this.getAbstractParent().scaleSyncedRecords.containsKey(this.ordinal);
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return true if the record contained reasonable date which can be displayed
	 */
	public abstract boolean hasReasonableData();

	@Override
	public String toString() {
		return "CommonRecord [ordinal=" + this.ordinal + ", name=" + this.name +  ", realSize=" + this.realSize() + ", isActive=" + this.isActive + ", dataType=" + this.dataType + ", maxValue=" + this.maxValue + ", minValue=" + this.minValue + ", avgValue=" + this.avgValue + ", sigmaValue=" + this.sigmaValue + "]";
	}

}
