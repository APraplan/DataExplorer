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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import gde.GDE;
import gde.device.IDevice;
import gde.log.Level;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.CellVoltageValues;
import gde.utils.StringHelper;

/**
 * Hold records for a channel of a device and the time steps.
 * Support synchronizing the y axis scales.
 * @author Thomas Eickert (USER)
 */
public abstract class AbstractRecordSet extends LinkedHashMap<String, AbstractRecord> {
	protected final static String											$CLASS_NAME							= AbstractRecordSet.class.getName();
	protected final static long												serialVersionUID				= 26031957;
	protected final static Logger											log											= Logger.getLogger($CLASS_NAME);

	public static final int														MAX_NAME_LENGTH					= 40;

	protected static final int												INITIAL_RECORD_CAPACITY	= 55;
	protected static final String											DESCRIPTION_TEXT_LEAD		= Messages.getString(MessageIds.GDE_MSGT0129);

	/**
	 * Collection of records where scales might be synchronized.
	 */
	protected SyncedRecords<? extends AbstractRecord>	scaleSyncedRecords			= new SyncedRecords<>(2);

	public static class SyncedRecords<T extends AbstractRecord> extends HashMap<Integer, Vector<T>> {
		private final static long	serialVersionUID	= -1231656159005000097L;
		@SuppressWarnings("hiding")
		private final Logger			log								= Logger.getLogger(SyncedRecords.class.getName());

		protected SyncedRecords(int initialCapacity) {
			super(initialCapacity);
		}

		/**
		 * @param recordName the record key to be used for the query
		 * @return true if syncable records contains queryRecordKey
		 */
		boolean isOneOfSyncableRecord(String recordName) {
			return getSyncMasterRecordOrdinal(recordName) >= 0;
		}

		/**
		 * @param recordName the record key to be used for the query
		 * @return the synchronization master record name or -1 if not found
		 */
		int getSyncMasterRecordOrdinal(String recordName) {
			for (Integer syncRecordOrdinal : this.keySet()) {
				if (this.isRecordContained(syncRecordOrdinal, recordName)) {
					return syncRecordOrdinal;
				}
			}
			return -1;
		}

		/**
		 * @param syncMasterRecordOrdinal
		 * @param recordName
		 * @return true if the scaleSyncedRecords vector contains the given record not using equivalent entries, like the Vector.contains() method
		 */
		boolean isRecordContained(int syncMasterRecordOrdinal, String recordName) {
			final String $METHOD_NAME = "isRecordContained";
			boolean isContained = false;
			synchronized (this) {
				if (this.get(syncMasterRecordOrdinal) != null) {
					for (AbstractRecord tempRecord : this.get(syncMasterRecordOrdinal)) {
						if (log.isLoggable(Level.FINER))
							log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "compare " + tempRecord.getName() + " with " + recordName);
						if (tempRecord.getName().equals(recordName)) {
							isContained = true;
							break;
						}
					}
				}
			}
			return isContained;
		}

		/**
		 * Define sync groups according device properties.
		 * Set axis end values, number format and scale position.
		 * Support settlements.
		 */
		public void initSyncedScales(AbstractRecordSet recordSet) {
			clear();

			for (int i = 0; i < recordSet.size(); i++) {
				@SuppressWarnings("unchecked")
				T tmpRecord = (T) recordSet.get(i);
				int syncMasterRecordOrdinal = tmpRecord.getSyncMasterRecordOrdinal();
				if (syncMasterRecordOrdinal >= 0) {
					@SuppressWarnings("unchecked")
					T syncMasterRecord = (T) recordSet.get(syncMasterRecordOrdinal);
					if (get(syncMasterRecordOrdinal) == null) {
						put(syncMasterRecordOrdinal, new Vector<T>());
						get(syncMasterRecordOrdinal).add(syncMasterRecord);
						syncMasterRecord.setSyncMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
					}
					if (!isRecordContained(syncMasterRecordOrdinal, tmpRecord.getName())) {
						if (Math.abs(i - syncMasterRecordOrdinal) >= get(syncMasterRecordOrdinal).size())
							get(syncMasterRecordOrdinal).add(tmpRecord);
						else
							// sort while add
							get(syncMasterRecordOrdinal).add(Math.abs(i - syncMasterRecordOrdinal), tmpRecord);

						if (log.isLoggable(Level.FINER)) log.finer(() -> "add " + tmpRecord.getName()); //$NON-NLS-1$
					}
				}
			}
			for (int i = 0; i < recordSet.size(); i++) {
				int syncMasterRecordOrdinal = recordSet.get(i).getSyncMasterRecordOrdinal();
				if (syncMasterRecordOrdinal >= 0) {
					AbstractRecord syncMasterRecord = recordSet.get(syncMasterRecordOrdinal);
					this.syncMasterSlaveRecords(syncMasterRecord, Record.TYPE_AXIS_END_VALUES);
					this.syncMasterSlaveRecords(syncMasterRecord, Record.TYPE_AXIS_NUMBER_FORMAT);
					this.syncMasterSlaveRecords(syncMasterRecord, Record.TYPE_AXIS_SCALE_POSITION);
				}
			}
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				for (Integer syncRecordOrdinal : this.keySet()) {
					sb.append(GDE.STRING_NEW_LINE).append(syncRecordOrdinal).append(GDE.STRING_COLON);
					for (AbstractRecord tmpRecord : get(syncRecordOrdinal)) {
						sb.append(tmpRecord.getName()).append(GDE.STRING_SEMICOLON);
					}
				}
				log.log(Level.FINE, sb.toString());
			}
		}

		/**
		 * Synchronize scale properties of master and slave scale synchronized records.
		 */
		void syncMasterSlaveRecords(AbstractRecord syncInputRecord, int type) {
			for (Integer syncRecordOrdinal : this.keySet()) {
				if (this.isRecordContained(syncRecordOrdinal, syncInputRecord.getName())) {
					switch (type) {
					case Record.TYPE_AXIS_END_VALUES:
						boolean tmpIsRoundout = syncInputRecord.isRoundOut();
						boolean tmpIsStartpointZero = syncInputRecord.isStartpointZero();
						boolean tmpIsStartEndDefined = syncInputRecord.isStartEndDefined();
						double minScaleValue = syncInputRecord.getMinScaleValue();
						double maxScaleValue = syncInputRecord.getMaxScaleValue();
						log.log(Level.FINER, "", this.get(syncRecordOrdinal).size() + " " + this.get(syncRecordOrdinal).stream().map(r -> r.getName()).collect(Collectors.joining(",")));
						for (AbstractRecord tmpRecord : this.get(syncRecordOrdinal)) {
							synchronized (tmpRecord) {
								tmpRecord.setRoundOut(tmpIsRoundout);
								tmpRecord.setStartpointZero(tmpIsStartpointZero);
								tmpRecord.setStartEndDefined(tmpIsStartEndDefined);
								tmpRecord.setMinScaleValue(minScaleValue);
								tmpRecord.setMaxScaleValue(maxScaleValue);
							}
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%d %s minScaleValue=%.2f maxScaleValue=%.2f isStartEndDefined=%b", //
									syncRecordOrdinal, tmpRecord.getName(), tmpRecord.getMinScaleValue(), tmpRecord.getMaxScaleValue(), tmpIsStartEndDefined));
						}
						break;
					case Record.TYPE_AXIS_NUMBER_FORMAT:
						DecimalFormat tmpDf = syncInputRecord.getRealDf();
						int numberFormat = syncInputRecord.getNumberFormat();
						for (AbstractRecord tmpRecord : this.get(syncRecordOrdinal)) {
							synchronized (tmpRecord) {
								tmpRecord.setRealDf((DecimalFormat) tmpDf.clone());
								tmpRecord.setNumberFormatDirect(numberFormat); //avoid recursion in compare set, replacement for record.numberformat=newValue
							}
						}
						break;
					case Record.TYPE_AXIS_SCALE_POSITION:
						boolean tmpIsPositionLeft = syncInputRecord.isPositionLeft();
						for (AbstractRecord tmpRecord : this.get(syncRecordOrdinal)) {
							synchronized (tmpRecord) {
								tmpRecord.setPositionLeft(tmpIsPositionLeft);
							}
						}
						break;
					}
				}
			}
		}

		/**
		 * @return true if one of the syncable records is visible
		 */
		boolean isOneSyncableVisible() {
			for (Integer syncRecordOrdinal : this.keySet()) {
				for (AbstractRecord tmpRecord : this.get(syncRecordOrdinal)) {
					if (tmpRecord != null && tmpRecord.isVisible()) {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * @param syncMasterOrdinal the record name of the sync master record
		 * @return true if one of the syncable records is visible
		 */
		boolean isOneSyncableVisible(int syncMasterOrdinal) {
			for (AbstractRecord tmpRecord : this.get(syncMasterOrdinal)) {
				if (tmpRecord != null && tmpRecord.isVisible() && tmpRecord.isDisplayable()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return "SyncedRecords [isOneSyncableVisible()=" + this.isOneSyncableVisible() + ", size()=" + this.size() + ", keySet()=" + this.keySet() + "]";
		}

	}

	/**
	 * 1)Flugaufzeichnung, 2)Laden, 3)Entladen, ..
	 */
	protected String														name;
	protected TimeSteps													timeStep_ms;

	protected String														header										= null;
	/**
	 * Spannung, Strom, ..
	 */
	protected String[]													recordNames;
	protected String														description								= GDE.STRING_EMPTY;

	protected boolean														hasDisplayableData				= false;
	// current drop, shadow point vector to mark data points capable to be smoothed
	protected boolean														isSmoothAtCurrentDrop			= false;
	protected Vector<Integer[]>									currentDropShadow					= new Vector<>(0);
	public static final String									SMOOTH_AT_CURRENT_DROP		= "RecordSet_smoothAtCurrentDrop";					//$NON-NLS-1$

	protected boolean														isSmoothVoltageCurve			= false;
	public static final String									SMOOTH_VOLTAGE_CURVE			= "RecordSet_smoothVoltageCurve";						//$NON-NLS-1$

	int[]																				voltageLimits							= CellVoltageValues.getVoltageLimits();																																									// voltage limits for LiXx cells, initial LiPo
	public static final String									VOLTAGE_LIMITS						= "RecordSet_voltageLimits";									// each main tickmark //$NON-NLS-1$

	/**
	 * display in data table.
	 */
	protected Vector<? extends AbstractRecord>	visibleAndDisplayableRecords;
	/**
	 * display in curve selector.
	 */
	protected Vector<? extends AbstractRecord>	displayRecords;

	public static final String									TIME_STEP_MS							= "timeStep_ms";														//$NON-NLS-1$
	public static final String									START_TIME_STAMP					= "startTimeStamp";													//$NON-NLS-1$
	protected static final String								TIME											= "time";																		//$NON-NLS-1$
	protected static final String								TIME_GRID_TYPE						= "RecordSet_timeGridType";									//$NON-NLS-1$
	protected static final String								TIME_GRID_COLOR						= "RecordSet_timeGridColor";								//$NON-NLS-1$
	protected static final String								TIME_GRID_LINE_STYLE			= "RecordSet_timeGridLineStyle";						//$NON-NLS-1$
	/**
	 * no time grid
	 */
	public static final int											TIME_GRID_NONE						= 0;
	/**
	 * each main tickmark
	 */
	public static final int											TIME_GRID_MAIN						= 1;
	/**
	 * each mod60 tickmark
	 */
	public static final int											TIME_GRID_MOD60						= 2;
	protected int																timeGridType							= TIME_GRID_NONE;
	/**
	 * contains the time grid position, updated from TimeLine.drawTickMarks
	 */
	protected Vector<Integer>										timeGrid									= new Vector<Integer>();
	protected Color															timeGridColor							= DataExplorer.getInstance().COLOR_GREY;
	protected int																timeGridLineStyle					= SWT.LINE_DOT;

	public static final String									SMART_STATISTICS					= "RecordSet_smartStatistics";							// histo: Quantiles
	protected static final String								CHART_WEIGHT							= "RecordSet_chartWeight";									// histo: weight of the charts
																																																										// (graphics or summary)
	@Deprecated
	protected static final String								VALUE_GRID_RECORD					= "RecordSet_horizontalGridRecord";					//$NON-NLS-1$
	protected static final String								VALUE_GRID_RECORD_ORDINAL	= "RecordSet_horizontalGridRecordOrdinal";	//$NON-NLS-1$
	protected static final String								VALUE_GRID_RECORD_NAME		= "RecordSet_horizontalGridRecordName";			// histo: replace ordinal
	protected static final String								VALUE_GRID_TYPE						= "RecordSet_horizontalGridType";						//$NON-NLS-1$
	protected static final String								VALUE_GRID_COLOR					= "RecordSet_horizontalGridColor";					//$NON-NLS-1$
	protected static final String								VALUE_GRID_LINE_STYLE			= "RecordSet_horizontalGridLineStyle";			//$NON-NLS-1$
	public static final int											VALUE_GRID_NONE						= 0;
	public static final int											VALUE_GRID_EVERY					= 1;
	public static final int											VALUE_GRID_SECOND					= 2;
	protected int																valueGridType							= VALUE_GRID_NONE;
	/**
	 * contains the time grid position, updated from TimeLine.drawTickMarks
	 */
	protected Vector<Integer>										valueGrid									= new Vector<Integer>();
	protected Color															valueGridColor						= DataExplorer.getInstance().COLOR_GREY;
	protected int																valueGridLineStyle				= SWT.LINE_DASH;
	/**
	 * recordNames[horizontalGridRecord]
	 */
	protected int																valueGridRecordOrdinal		= -1;
	protected String														valueGridRecordName				= "";																				// histo: replace ordinal

	// for compare set x min/max and y max (time) might be different
	boolean																			isCompareSet							= false;

	/**
	 * Special record set data buffers according the size of given names array, where the name is the key to access the data buffer used to hold
	 * compare able records (compare set).
	 * @param useDevice the device
	 * @param newName for the records like "1) Laden"
	 * @param recordNames
	 */
	protected AbstractRecordSet(IDevice useDevice, String newName, String[] recordNames) {
		super();
		this.name = newName;
		this.recordNames = recordNames;
	}

	/**
	 * Record set data buffers according the size of given names array, where the name is the key to access the data buffer.
	 * @param useDevice
	 * @param channelNumber the channel number to be used
	 * @param newName for the records like "1) Laden"
	 * @param measurementNames array of the device supported measurement names
	 * @param newTimeSteps
	 */
	protected AbstractRecordSet(IDevice useDevice, String newName, String[] measurementNames, TimeSteps newTimeSteps) {
		super(measurementNames.length);
		this.name = newName.length() <= AbstractRecordSet.MAX_NAME_LENGTH ? newName : newName.substring(0, 30);

		this.recordNames = measurementNames.clone();
		this.timeStep_ms = newTimeSteps;
		this.description = (useDevice != null ? useDevice.getName() + GDE.STRING_MESSAGE_CONCAT
				: GDE.STRING_EMPTY) + DESCRIPTION_TEXT_LEAD + StringHelper.getDateAndTime();
	}

	/**
	 * Copy constructor - used to copy a record set to another channel/configuration, where the configuration coming from the device properties
	 * file.
	 * @param recordSet
	 */
	protected AbstractRecordSet(AbstractRecordSet recordSet) {
		super(recordSet);
		this.name = recordSet.name;
	}

	/**
	 * Copy constructor - used to copy a record set during graphics cut mode.
	 * @param recordSet
	 * @param recordNames
	 */
	protected AbstractRecordSet(AbstractRecordSet recordSet, String[] recordNames) {
		super(recordSet);
		this.name = recordSet.name.length() < MAX_NAME_LENGTH ? recordSet.name + GDE.STRING_UNDER_BAR
				: recordSet.name.substring(0, MAX_NAME_LENGTH - 1) + GDE.STRING_UNDER_BAR;

		this.recordNames = recordNames;
	}

	/**
	 * Method to get the sorted record names as array, use it for logging or debugging purpose only.
	 * Sorted according list in the device configuration (XML) file.
	 * @return the record names
	 */
	public String[] getRecordNames() {
		return this.recordNames.clone();
	}

	/**
	 * Update the collection of visible and displayable records in this record set for table view
	 */
	public abstract void updateVisibleAndDisplayableRecordsForTable();

	/**
	 * @return visible and displayable records (p.e. to build the partial data table)
	 */
	public Vector<? extends AbstractRecord> getVisibleAndDisplayableRecords() {
		return this.visibleAndDisplayableRecords;
	}

	/**
	 * @return all records for display
	 */
	public Vector<? extends AbstractRecord> getDisplayRecords() {
		return this.displayRecords;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String newName) {
		this.name = newName;
	}

	/**
	 * Print record names array into the log.
	 * @param recordNames
	 */
	protected static void printRecordNames(String methodName, String[] recordNames) {
		StringBuilder sb = new StringBuilder();
		for (String recordName : recordNames) {
			sb.append(recordName).append(GDE.STRING_MESSAGE_CONCAT);
		}
		sb.delete(sb.length() - 3, sb.length());
		if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, methodName, sb.toString());
	}

	/**
	 * @param recordKey
	 * @param isLeft
	 * @return the scale axis position as function of available axis at the given side
	 */
	public int getAxisPosition(String recordKey, boolean isLeft) {
		int value = -1;
		if (isLeft) {
			for (String recordName : this.recordNames) {
				AbstractRecord tmpRecord = this.get(recordName);
				if (tmpRecord.isPositionLeft() && tmpRecord.isScaleVisible()) ++value;
				if (recordName.equals(recordKey)) break;
			}
		} else {
			for (String recordName : this.recordNames) {
				log.log(Level.FINER, "record name = " + recordName); //$NON-NLS-1$
				AbstractRecord tmpRecord = this.get(recordName);
				if (!tmpRecord.isPositionLeft() && tmpRecord.isScaleVisible()) ++value;
				if (recordName.equals(recordKey)) break;
			}
		}
		return value;
	}

	/**
	 * @param recordOrdinal
	 * @return the record based on ordinal
	 */
	public AbstractRecord get(int recordOrdinal) {
		try {
			return this.get(this.recordNames[recordOrdinal]);
		} catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
			return super.size() > 0 ? this.get(0) : null;
		}
	}

	/**
	 * @param index
	 * @return the time value
	 */
	public double getTime_ms(int index) {
		return this.timeStep_ms.getTime_ms(index);
	}

	/**
	 * @return the maximum time of this record set, which should correspondence to the last entry in timeSteps
	 */
	public double getMaxTime_ms() {
		return this.timeStep_ms == null ? 0.0 : this.timeStep_ms.isConstant ? this.timeStep_ms.getMaxTime_ms() * (this.get(0).size() - 1)
				: this.timeStep_ms.getMaxTime_ms();
	}

	/**
	 * @return the channel/configuration number
	 */
	public abstract int getChannelConfigNumber();

		/**
	 * @return the horizontalGridRecord ordinal
	 */
	public abstract int getValueGridRecordOrdinal();

	/**
	 * @param newValueGridRecordOrdinal of the horizontal grid record name to set
	 */
	public void setValueGridRecordOrdinal(int newValueGridRecordOrdinal) {
		int tmpOrdinal = newValueGridRecordOrdinal;
		if (tmpOrdinal >= this.size()) tmpOrdinal = 0;
		this.valueGridRecordOrdinal = this.scaleSyncedRecords.isOneOfSyncableRecord(this.get(tmpOrdinal).getName())
				? this.scaleSyncedRecords.getSyncMasterRecordOrdinal(this.get(tmpOrdinal).getName()) : tmpOrdinal;
	}

	/**
	 * @return the horizontalGridType
	 */
	public int getValueGridType() {
		return this.valueGridType;
	}

	/**
	 * @param newValueGridType the horizontalGridType to set
	 */
	public void setValueGridType(int newValueGridType) {
		this.valueGridType = newValueGridType;
	}

	/**
	 * @return the horizontalGrid
	 */
	public Vector<Integer> getValueGrid() {
		return this.valueGrid;
	}

	/**
	 * @param newValueGrid the horizontalGrid to set
	 */
	public void setValueGrid(Vector<Integer> newValueGrid) {
		this.valueGrid = new Vector<Integer>(newValueGrid);
	}

	/**
	 * @return the horizontalGridColor
	 */
	public Color getValueGridColor() {
		return this.valueGridColor;
	}

	/**
	 * @param newHorizontalGridColor the horizontalGridColor to set
	 */
	public void setValueGridColor(Color newHorizontalGridColor) {
		this.valueGridColor = newHorizontalGridColor;
	}

	/**
	 * @return the horizontalGridLineStyle
	 */
	public int getValueGridLineStyle() {
		return this.valueGridLineStyle;
	}

	/**
	 * @param newValueGridLineStyle the horizontalGridLineStyle to set
	 */
	public void setValueGridLineStyle(int newValueGridLineStyle) {
		this.valueGridLineStyle = newValueGridLineStyle;
	}

	/**
	 * @return the device
	 */
	public abstract IDevice getDevice();

	/**
	 * query boolean value to enable curve smoothing due to current drop
	 */
	public boolean isSmoothAtCurrentDrop() {
		return this.isSmoothAtCurrentDrop;
	}

	/**
	 * set boolean value to enable curve smoothing due to current drop
	 * @param enable
	 */
	public void setSmoothAtCurrentDrop(boolean enable) {
		this.isSmoothAtCurrentDrop = enable;
	}

	/**
	 * query boolean value to enable curve smoothing due to pulsed voltage curve
	 */
	public boolean isSmoothVoltageCurve() {
		return this.isSmoothVoltageCurve;
	}

	/**
	 * set boolean value to enable curve smoothing due to pulsed voltage curve
	 * @param enable
	 */
	public void setSmoothVoltageCurve(boolean enable) {
		this.isSmoothVoltageCurve = enable;
	}

	/**
	 * Synchronize scales according device properties.
	 */
	public void syncScaleOfSyncableRecords() {
		this.scaleSyncedRecords.initSyncedScales(this);
	}

	/**
	 * Synchronize scale properties of master and slave scale synchronized records.
	 */
	public void syncMasterSlaveRecords(AbstractRecord syncInputRecord, int type) {
		this.scaleSyncedRecords.syncMasterSlaveRecords(syncInputRecord, type);
	}

	/**
	 * @param syncMasterOrdinal the record name of the sync master record
	 * @return true if one of the syncable records is visible
	 */
	public boolean isOneSyncableVisible(int syncMasterOrdinal) {
		return this.scaleSyncedRecords.isOneSyncableVisible(syncMasterOrdinal);
	}

	/**
	 * @return the Vector containing the slave records sync by the master name
	 */
	public abstract Vector<? extends AbstractRecord> getScaleSyncedRecords(int syncMasterRecordOrdinal);

	public int realSize() {
		return super.size();
	}

	/**
	 * @param recordName the record key to be used for the query
	 * @return true if syncable records contains queryRecordKey
	 */
	public boolean isOneOfSyncableRecord(String recordName) {
		return this.scaleSyncedRecords.isOneOfSyncableRecord(recordName);
	}

	/**
	 * @param recordName the record key to be used for the query
	 * @return the synchronization master record name or -1 if not found
	 */
	public int getSyncMasterRecordOrdinal(String recordName) {
		return this.scaleSyncedRecords.getSyncMasterRecordOrdinal(recordName);
	}

	public String getDescription() {
		return this.description;
	}

	@Override
	public String toString() {
		return "AbstractRecordSet [realSize=" + this.realSize() + ", hasDisplayableData=" + this.hasDisplayableData + ", visibleAndDisplayableRecordSize=" + this.visibleAndDisplayableRecords.size() + ", displayRecordSize=" + this.displayRecords.size() + "]";
	}

	/**
	 * @return the isCompareSet
	 */
	public boolean isCompareSet() {
		return this.isCompareSet;
	}

}
