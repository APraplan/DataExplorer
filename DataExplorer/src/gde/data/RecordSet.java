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

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;

import gde.Analyzer;
import gde.Explorer;
import gde.GDE;
import gde.TestAnalyzer;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.TriggerType;
import gde.device.resource.DeviceXmlResource;
import gde.exception.DataInconsitsentException;
import gde.histo.transitions.GroupTransitions;
import gde.histo.transitions.TransitionCollector;
import gde.histo.transitions.TransitionTableMapper;
import gde.io.LogViewReader;
import gde.io.OsdReaderWriter;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.tab.GraphicsWindow.GraphicsType;
import gde.utils.CalculationThread;
import gde.utils.CellVoltageValues;
import gde.utils.LocalizedDateTime.DateTimePattern;
import gde.utils.LocalizedDateTime.DurationPattern;
import gde.utils.StringHelper;
import gde.utils.TimeLine;

/**
 * RecordSet class holds all the data records for the configured measurement of a device
 * @author Winfried Brügmann
 */
public final class RecordSet extends AbstractRecordSet {
	final static String						$CLASS_NAME										= RecordSet.class.getName();
	final static long							serialVersionUID							= 26031957;
	final static Logger						log														= Logger.getLogger(RecordSet.class.getName());

	/**
	 * start point of data hierarchy.
	 */
	protected final Channels			channels;
	protected final IDevice				device;
	protected final Channel				parent;

	String[]											noneCalculationRecords				= new String[0];																																																				//records/measurements which are active or inactive
	int														noneCalculationRecordsCount		= -1;																																																										//cached count or number of noneCalculationRecords, -1 means not initialized
	boolean												isSaved												= false;																																																								//indicates if the record set is saved to file
	boolean												isRaw													= false;																																																								//indicates imported file with raw data, no translation at all
	boolean												isFromFile										= false;																																																								//indicates that this record set was created by loading data from file
	boolean												isRecalculation								= true;																																																									//indicates record is modified and need re-calculation
	int														fileDataSize									= 0;																					// number of integer values per record
	int														fileDataBytes									= 0;																																																										//number of bytes containing all records data
	long													fileDataPointer								= 0;																																																										//file pointer where the data of this record begins
	int														xScaleStep										= 0;																																																										// steps in x direction to draw the curves, normally 1
	Rectangle											drawAreaBounds;

	protected double														maxValue												= Integer.MIN_VALUE;
	protected double														minValue												= Integer.MAX_VALUE;

	boolean												isUtilitySet									= false;
	double												maxTime												= 0.0;																																																									//compare set -> each record will have its own timeSteps_ms,
	// so the biggest record in view point of time will define the time scale

	// zooming
	int														zoomLevel											= 0;																					// 0 == not zoomed
	boolean												isZoomMode										= false;
	boolean												isScopeMode										= false;
	int														scopeModeOffset;																														// defines the offset in record pixel
	int														scopeModeSize;																																																																				// defines the number of record pixels to be displayed

	// boolean isSyncRecordSelected = false;
	// public static final String SYNC_RECORD_SELECTED = "Syncable_record_selected";

	public static final String[]	propertyKeys									= new String[] { TIME_STEP_MS,								//
			START_TIME_STAMP, VALUE_GRID_RECORD_ORDINAL,																													//
			VALUE_GRID_RECORD, TIME_GRID_TYPE, TIME_GRID_LINE_STYLE, TIME_GRID_COLOR,															//
			VALUE_GRID_TYPE, VALUE_GRID_LINE_STYLE,																																//
			VALUE_GRID_COLOR, SMOOTH_AT_CURRENT_DROP, SMOOTH_VOLTAGE_CURVE, VOLTAGE_LIMITS };

	int														configuredDisplayable					= 0;																																																										// number of record which must be displayable before table calculation begins

	public final static String		UNSAVED_REASON_GRAPHICS				= Messages.getString(MessageIds.GDE_MSGT0130);
	public final static String		UNSAVED_REASON_DATA						= Messages.getString(MessageIds.GDE_MSGT0131);
	public final static String		UNSAVED_REASON_CONFIGURATION	= Messages.getString(MessageIds.GDE_MSGT0132);
	public final static String		UNSAVED_REASON_COMMENT				= Messages.getString(MessageIds.GDE_MSGT0611);

	private final Analyzer				analyzer;

	Vector<String>								unsaveReasons									= new Vector<String>();
	int														changeCounter									= 0;																					// indicates change in general

	private GroupTransitions			histoTransitions;
	private String								recordKeyMeasurement					= GDE.STRING_EMPTY;

	/**
	 * record set data buffers according the size of given names array, where
	 * the name is the key to access the data buffer
	 * @param channelNumber the channel number to be used (supports channel mix)
	 * @param recordSetName
	 * @param measurementNames array of the device supported measurement names
	 * @param isRawValue specified if dependent values has been calculated
	 * @param isFromFileValue specifies if the data are red from file and if not modified don't need to be saved
	 */
	private RecordSet(Analyzer analyzer, int channelNumber, String recordSetName, String[] measurementNames, boolean isRawValue, boolean isFromFileValue) {
		super(analyzer.getActiveDevice(), recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH), //
				measurementNames, new TimeSteps(analyzer.getActiveDevice().getTimeStep_ms(), INITIAL_RECORD_CAPACITY));
		this.analyzer = analyzer;
		this.channels = analyzer.getChannels();
		this.device = analyzer.getActiveDevice();

		this.parent = this.channels.get(channelNumber);
		this.isRaw = isRawValue;
		this.isFromFile = isFromFileValue;
		this.visibleAndDisplayableRecords = new Vector<Record>();
		this.displayRecords = new Vector<Record>();
		this.scaleSyncedRecords = new SyncedRecords<Record>(2);
	}

	/**
	 * special record set data buffers according the size of given names array, where
	 * the name is the key to access the data buffer used to hold compare able records (compare set)
	 * @param newChannelName the channel name or configuration name
	 * @param newName for the records like "1) Laden"
	 * @param newTimeStep_ms time in msec of device measures points
	 * @param graphicsType
	 */
	public RecordSet(Analyzer analyzer, String newChannelName, String newName, double newTimeStep_ms, GraphicsType graphicsType) {
		super(analyzer.getActiveDevice(), newName.length() <= RecordSet.MAX_NAME_LENGTH ? newName : newName.substring(0, 30), new String[0]);
		this.analyzer = analyzer;
		this.channels = null;
		this.device = analyzer.getActiveDevice();

		this.parent = null;
		// this.timeStep_ms = new TimeSteps(this.get(0), newTimeStep_ms);
		this.isRaw = true;
		this.isCompareSet = GraphicsType.COMPARE == graphicsType;
		this.isUtilitySet = GraphicsType.UTIL == graphicsType;
		this.visibleAndDisplayableRecords = new Vector<Record>();
		this.displayRecords = new Vector<Record>();
		this.scaleSyncedRecords = new SyncedRecords<Record>(2);
	}

	/**
	 * copy constructor - used to copy a record set to another channel/configuration,
	 * where the configuration coming from the device properties file
	 * @param recordSet
	 * @param channelConfigurationNumber
	 */
	private RecordSet(RecordSet recordSet, int channelConfigurationNumber) {
		super(recordSet);
		this.analyzer = recordSet.analyzer.getReplica();
		this.channels = recordSet.channels;
		this.device = recordSet.device;

		this.parent = this.channels.get(channelConfigurationNumber);

		// check if there is a miss match of measurement names and correction required
		String[] oldRecordNames = recordSet.recordNames;
		if (log.isLoggable(Level.FINER)) {
			StringBuilder sb = new StringBuilder();
			for (String string : oldRecordNames) {
				sb.append(string).append(" "); //$NON-NLS-1$
			}
			log.log(Level.FINER, "oldRecordNames = " + sb.toString()); //$NON-NLS-1$
		}
		String[] newRecordNames = this.device.getMeasurementNamesReplacements(channelConfigurationNumber);
		if (log.isLoggable(Level.FINER)) {
			StringBuilder sb = new StringBuilder();
			for (String string : newRecordNames) {
				sb.append(string).append(" "); //$NON-NLS-1$
			}
			log.log(Level.FINER, "newRecordNames = " + sb.toString()); //$NON-NLS-1$
		}
		// copy records while exchange record name (may change to other language!)
		for (int i = 0; i < newRecordNames.length; i++) {
			if (!oldRecordNames[i].equals(newRecordNames[i])) {
				// add the old record with new name
				this.put(newRecordNames[i], this.get(oldRecordNames[i]).clone(newRecordNames[i]));
				// remove the old record
				this.remove(oldRecordNames[i]);
			}
		}
		this.recordNames = newRecordNames.clone();

		// update child records to new channel or configuration key and to the new parent
		for (int i = 0; i < this.recordNames.length; ++i) {
			Record tmpRecord = this.get(this.recordNames[i]);
			tmpRecord.setParent(this);

			tmpRecord.statistics = this.device.getMeasurementStatistic(channelConfigurationNumber, i);
			if (tmpRecord.statistics == null) log.log(Level.WARNING, "tmpRecord.statistics == null"); //$NON-NLS-1$
			TriggerType tmpTrigger = tmpRecord.statistics != null ? tmpRecord.statistics.getTrigger() : null;
			tmpRecord.triggerIsGreater = tmpTrigger != null ? tmpTrigger.isGreater() : null;
			tmpRecord.triggerLevel = tmpTrigger != null ? tmpTrigger.getLevel() : null;
			tmpRecord.minTriggerTimeSec = tmpTrigger != null ? tmpTrigger.getMinTimeSec() : null;

			// copy record properties if -> record properties available == name equal
			if (recordSet.get(this.recordNames[i]) != null)
				tmpRecord.setProperties(recordSet.get(this.recordNames[i]).getProperties());
			else
				tmpRecord.setProperties(this.device.getProperties(channelConfigurationNumber, i));
		}

		this.timeStep_ms = recordSet.timeStep_ms.clone(0, true);
		this.description = recordSet.description;
		this.isSaved = false;
		this.isRaw = recordSet.isRaw;
		this.isFromFile = recordSet.isFromFile;
		this.drawAreaBounds = recordSet.drawAreaBounds;

		this.isCompareSet = recordSet.isCompareSet;

		// this.maxSize = recordSet.maxSize;
		this.maxTime = recordSet.maxTime;
		this.maxValue = recordSet.maxValue;
		this.minValue = recordSet.minValue;

		this.zoomLevel = recordSet.zoomLevel;
		this.isZoomMode = recordSet.isZoomMode;

		this.recordKeyMeasurement = recordSet.recordKeyMeasurement;

		this.timeGridType = recordSet.timeGridType;
		this.timeGrid = new Vector<Integer>(recordSet.timeGrid);
		this.timeGridColor = recordSet.timeGridColor;
		this.timeGridLineStyle = recordSet.timeGridLineStyle;

		this.valueGridType = recordSet.valueGridType;
		this.valueGrid = new Vector<Integer>(recordSet.valueGrid);
		this.valueGridColor = recordSet.valueGridColor;
		this.valueGridLineStyle = recordSet.valueGridLineStyle;
		this.valueGridRecordOrdinal = recordSet.valueGridRecordOrdinal;

		this.configuredDisplayable = recordSet.configuredDisplayable;

		this.visibleAndDisplayableRecords = new Vector<Record>();
		this.displayRecords = new Vector<Record>();
		this.scaleSyncedRecords = new SyncedRecords<Record>(this.recordNames.length);
		this.syncScaleOfSyncableRecords();
	}

	/**
	 * clone method used to move record sets to other configuration or channel
	 * @param channelConfiguationNumber
	 */
	public RecordSet clone(int channelConfiguationNumber) {
		return new RecordSet(this, channelConfiguationNumber);
	}

	/**
	 * copy constructor - used to copy a record set during graphics cut mode.
	 * @param recordSet
	 * @param dataIndex
	 * @param isFromBegin
	 */
	private RecordSet(RecordSet recordSet, int dataIndex, boolean isFromBegin) {
		super(recordSet, DeviceXmlResource.getInstance().getReplacements(recordSet.recordNames.clone())); // copy record names without possible syncableName
		this.analyzer = recordSet.analyzer.getReplica();
		this.channels = recordSet.channels;
		this.device = recordSet.device;

		this.parent = recordSet.parent;

		// update child records
		for (String recordKey : this.recordNames) {
			this.put(recordKey, this.get(recordKey).clone(dataIndex, isFromBegin));
		}

		if (recordSet.timeStep_ms != null) { // time step vector must be updated as well
			this.timeStep_ms = recordSet.timeStep_ms.clone(dataIndex, isFromBegin);
		}

		if (isFromBegin && recordSet.timeStep_ms != null && this.timeStep_ms != null) {
			String[] splitDescription = recordSet.description.split(StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss", recordSet.timeStep_ms.getStartTimeStamp()));
			if (splitDescription.length > 1)
				this.description = splitDescription[0] + StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss", this.timeStep_ms.getStartTimeStamp()) + splitDescription[1];
			else if (splitDescription.length > 0)
				this.description = splitDescription[0] + StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss", this.timeStep_ms.getStartTimeStamp());
			else
				this.description = recordSet.description;
		} else {
			this.description = recordSet.description;
		}
		this.isSaved = false;
		this.isRaw = recordSet.isRaw;
		this.isFromFile = recordSet.isFromFile;
		this.drawAreaBounds = recordSet.drawAreaBounds;

		this.isCompareSet = recordSet.isCompareSet;

		// this.maxSize = recordSet.maxSize;
		this.maxTime = recordSet.maxTime;
		this.maxValue = recordSet.maxValue;
		this.minValue = recordSet.minValue;

		this.zoomLevel = 0;
		this.isZoomMode = false;

		this.recordKeyMeasurement = recordSet.recordKeyMeasurement;

		this.timeGridType = recordSet.timeGridType;
		this.timeGrid = new Vector<Integer>(recordSet.timeGrid);
		this.timeGridColor = recordSet.timeGridColor;
		this.timeGridLineStyle = recordSet.timeGridLineStyle;

		this.valueGridType = recordSet.valueGridType;
		this.valueGrid = new Vector<Integer>(recordSet.valueGrid);
		this.valueGridColor = recordSet.valueGridColor;
		this.valueGridLineStyle = recordSet.valueGridLineStyle;
		this.valueGridRecordOrdinal = recordSet.valueGridRecordOrdinal;

		this.configuredDisplayable = recordSet.configuredDisplayable;

		this.visibleAndDisplayableRecords = new Vector<Record>();
		this.displayRecords = new Vector<Record>();
		this.scaleSyncedRecords = new SyncedRecords<Record>(this.recordNames.length);

		this.device.updateVisibilityStatus(this, false);
	}

	/**
	 * clone method re-writes data points of all records of this record set
	 * - if isFromBegin == true, the given index is the index where the record starts after this operation
	 * - if isFromBegin == false, the given index represents the last data point index of the records.
	 * @param dataIndex
	 * @param isFromBegin
	 * @return new created record set
	 */
	public RecordSet clone(int dataIndex, boolean isFromBegin) {
		return new RecordSet(this, dataIndex, isFromBegin);
	}

	/**
	 * @param recordOrdinal
	 * @return the record based on ordinal
	 */
	@Override
	public Record get(int recordOrdinal) {
		return (Record) super.get(recordOrdinal);
	}

	@Override
	public Record get(Object recordName) {
		return (Record) super.get(recordName);
	}

	/**
	 * remove the record as well as the key
	 */
	public void remove(String recordKey) {
		super.remove(recordKey);
		if (this.recordNames != null) this.removeRecordName(recordKey);
	}

	/**
	 * check all records of this record set are displayable
	 * @return true/false
	 */
	public boolean checkAllRecordsDisplayable() {
		int displayableRecordEntries = 0;
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isDisplayable()) ++displayableRecordEntries;
			log.log(Level.FINER, recordKey + " isDiplayable = " + this.get(recordKey).isDisplayable()); //$NON-NLS-1$
		}

		int targetDisplayable = this.configuredDisplayable == 0 ? this.size() : this.configuredDisplayable;
		log.log(Level.FINE, "targetDisplayable = " + targetDisplayable + " - displayableRecordEntries = " + displayableRecordEntries); //$NON-NLS-1$ //$NON-NLS-2$

		return displayableRecordEntries >= targetDisplayable;
	}

	/**
	 * method to add a series of points to the associated records
	 * @param points as int[], where the length must fit records.size()
	 * @throws DataInconsitsentException
	 */
	public synchronized void addPoints(int[] points) throws DataInconsitsentException {
		final String $METHOD_NAME = "addPoints"; //$NON-NLS-1$
		if (points.length == this.size()) {
			for (int i = 0; i < points.length; i++) {
				this.get(this.recordNames[i]).add(points[i]);
			}
			if (log.isLoggable(Level.FINEST)) {
				StringBuilder sb = new StringBuilder();
				for (int point : points) {
					sb.append(point).append(GDE.STRING_BLANK);
				}
				log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}
		} else
			throw new DataInconsitsentException(Messages.getString(MessageIds.GDE_MSGE0035, new Object[] { this.getClass().getSimpleName(), $METHOD_NAME, points.length, this.size() })); // $NON-NLS-1$

		this.hasDisplayableData = true;
	}

	/**
	 * method to add a series of points to the associated records
	 * @param points as int[], where the length must fit records.size()
	 * @param time_ms
	 * @throws DataInconsitsentException
	 */
	public synchronized void addPoints(int[] points, double time_ms) throws DataInconsitsentException {
		this.timeStep_ms.add(time_ms);
		this.addPoints(points);
	}

	/**
	 * method to add a series of points to none calculation records (records active or inactive)
	 * @param points as int[], where the length must fit records.size()
	 * @throws DataInconsitsentException
	 */
	public synchronized void addNoneCalculationRecordsPoints(int[] points) throws DataInconsitsentException {
		final String $METHOD_NAME = "addNoneCalculationRecordsPoints"; //$NON-NLS-1$
		if (points.length <= this.getNoneCalculationRecordNames().length) {
			for (int i = 0; i < points.length; i++) {
				try {
					this.get(this.noneCalculationRecords[i]).add(points[i]);
				} catch (Exception e) {
					log.log(Level.SEVERE, String.format("Record %s not found, matching recordName %s", this.noneCalculationRecords[i], get(i).name));
				}
			}
			if (log.isLoggable(Level.FINEST)) {
				StringBuilder sb = new StringBuilder();
				for (int point : points) {
					sb.append(point).append(GDE.STRING_BLANK);
				}
				log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}
		} else
			throw new DataInconsitsentException(Messages.getString(MessageIds.GDE_MSGE0036, new Object[] { this.getClass().getSimpleName(), $METHOD_NAME }));

		this.hasDisplayableData = true;
	}

	/**
	 * method to add a series of points to none calculation records (records active or inactive)
	 * @param points as int[], where the length must fit records.size()
	 * @param time_ms
	 * @throws DataInconsitsentException
	 */
	public synchronized void addNoneCalculationRecordsPoints(int[] points, double time_ms) throws DataInconsitsentException {
		this.timeStep_ms.add(time_ms);
		this.addNoneCalculationRecordsPoints(points);
	}

	/**
	 * Uses cached transitions if the record data size has not changed (no gathering mode).
	 * @return the transitions identified for this recordset
	 */
	public GroupTransitions getHistoTransitions() {
		boolean isOutdated = this.histoTransitions != null && this.histoTransitions.isGatheringMode(this);
		if (this.histoTransitions == null || isOutdated) {
			this.histoTransitions = new TransitionCollector(this).defineTransitions(this.channels.getActiveChannelNumber());
		}
		return this.histoTransitions;
	}

	/**
	 * get all calculated and formated data table points of a given index.
	 * Add transition columns and list the valid transition ID in the transition timestamp rows.
	 * @param index of the data points
	 * @param isAbsolute false shows timesteps starting with zero
	 * @return formatted values as string array including time
	 */
	public String[] getDataTableRow(int index, boolean isAbsolute) {
		index = this.get(0).getIndex(index);
		String[] dataTableRow = new String[this.size() + 1]; // add time column
		dataTableRow[0] = this.getFormatedTime_sec(index, isAbsolute);
		this.device.prepareDataTableRow(this, dataTableRow, index);

		if (this.analyzer.getSettings().isHistoActive() && this.analyzer.getSettings().isDataTableTransitions()) {
			String[] rowWithSettlements = new TransitionTableMapper(this, this.analyzer).defineRowWithSettlements(index, dataTableRow);
			return rowWithSettlements;
		} else
			return dataTableRow;
	}

	/**
	 * get all calculated and formated data points of a given index
	 * @param index of the data points
	 * @return formatted values as string array including time
	 */
	public String[] getExportRow(int index, boolean isAbsolute) {
		index = this.get(0).getIndex(index);
		String[] exportRow = new String[this.size() + 1]; // add time column
		exportRow[0] = this.getFormatedTime_sec(index, isAbsolute);
		return this.device.prepareExportRow(this, exportRow, index);
	}

	/**
	 * get all none calculated and formated data points of a given index
	 * @param index of the data points
	 * @return raw values as string array including time
	 */
	public String[] getRawExportRow(int index, boolean isAbsolute) {
		index = this.get(0).getIndex(index);
		String[] exportRow = new String[this.size() + 1]; // add time column
		exportRow[0] = this.getFormatedTime_sec(index, isAbsolute);
		return this.device.prepareRawExportRow(this, exportRow, index);
	}

	/**
	 * @return a valid time step in msec for record sets from devices with constant time step between measurement points !
	 *         For devices with none constant time step between measurement points it returns the average value.
	 *         Do not use for calculation, use for logging purpose only.
	 */
	public double getAverageTimeStep_ms() {
		return this.timeStep_ms != null ? this.timeStep_ms.getAverageTimeStep_ms() : this.get(0).timeStep_ms != null ? this.get(0).timeStep_ms.getAverageTimeStep_ms() : -1.0;
	}

	/**
	 * @return the minimum time step (timespan) in msec
	 */
	public double getMinimumTimeStep_ms() {
		return this.timeStep_ms != null ? this.timeStep_ms.getMinimumTimeStep_ms() : this.get(0).timeStep_ms != null ? this.get(0).timeStep_ms.getMinimumTimeStep_ms() : -1.0;
	}

	/**
	 * @return the maximum time step (timespan) in msec
	 */
	public double getMaximumTimeStep_ms() {
		return this.timeStep_ms != null ? this.timeStep_ms.getMaximumTimeStep_ms() : this.get(0).timeStep_ms != null ? this.get(0).timeStep_ms.getMaximumTimeStep_ms() : -1.0;
	}

	/**
	 * @return the standard deviation of the time steps (timespans) in msec
	 */
	public double getSigmaTimeStep_ms() {
		return this.timeStep_ms != null ? this.timeStep_ms.getSigmaTimeStep_ms() : this.get(0).timeStep_ms != null ? this.get(0).timeStep_ms.getSigmaTimeStep_ms() : -1.0;
	}

	/**
	 * @return the isConstant true if time step is a constant value between measurement points
	 */
	public boolean isTimeStepConstant() {
		return this.timeStep_ms.isConstant;
	}

	/**
	 * @param index
	 * @param isAbsolute true forces the formatting to "yyyy-mm-dd HH:mm:ss.SSS"
	 * @return the timeSteps_ms formatted relative (e.g. "mm:ss.SSS") or absolute
	 */
	public String getFormatedTime_sec(int index, boolean isAbsolute) {
		if (isAbsolute) {
			return this.timeStep_ms.getFormattedTime(DateTimePattern.yyyyMMdd_HHmmssSSS, index);
		} else if (this.getMaxTime_ms() > GDE.ONE_HOUR_MS * 24. * 365. * 11.) {
			return this.timeStep_ms.getFormattedTime(DateTimePattern.yyyyMMdd_HHmmssSSS, index);
		} else {
			final DurationPattern formatString;
			if (this.getMaxTime_ms() <= GDE.ONE_HOUR_MS)
				formatString = DurationPattern.mm_ss_SSS;
			else if (this.getMaxTime_ms() <= GDE.ONE_HOUR_MS * 24.)
				formatString = DurationPattern.HH_mm_ss_SSS;
			else if (this.getMaxTime_ms() <= GDE.ONE_HOUR_MS * 24. * 30.)
				formatString = DurationPattern.dd_HH_mm_ss_SSS;
			else if (this.getMaxTime_ms() <= GDE.ONE_HOUR_MS * 24. * 365.)
				formatString = DurationPattern.MM_dd_HH_mm_ss_SSS;
			else
				formatString = DurationPattern.yy_MM_dd_HH_mm_ss_SSS;
			return this.timeStep_ms.getFormattedDuration(formatString, index);
		}
	}

	/**
	 * @return the time information for index given
	 */
	public long getTime(int index) {
		return this.timeStep_ms.get(index);
	}

	/**
	 * add a new time step to the time steps vector
	 * @param timeValue
	 */
	public void addTimeStep_ms(double timeValue) {
		this.timeStep_ms.add(timeValue);
	}

	/**
	 * method to get the sorted record names as array for display purpose
	 * sorted according display requirement, grid record first, syncMasterRecords second, all remaining
	 * @return Record[] containing records
	 */
	public Record[] getRecordsSortedForDisplay() {
		Vector<Record> displayRecords = new Vector<Record>();
		// add the record with horizontal grid
		for (Record record : this.getValues()) {
			if (record.size() > 0 && record.ordinal == this.valueGridRecordOrdinal) displayRecords.add(record);
		}
		// add the scaleSyncMaster records to draw scale of this records first which sets the min/max display values
		for (int i = 0; i < this.size(); ++i) {
			final Record record = this.get(i);
			if (record.size() > 0 && record.ordinal != this.valueGridRecordOrdinal && record.isScaleSyncMaster()) displayRecords.add(record);
		}
		// add all others
		for (int i = 0; i < this.size(); ++i) {
			final Record record = this.get(i);
			if (record.size() > 0 && record.ordinal != this.valueGridRecordOrdinal && !record.isScaleSyncMaster()) displayRecords.add(record);
		}

		return displayRecords.toArray(new Record[0]);
	}

	/**
	 * update the collection of visible and displayable records in this record set for table view
	 */
	@Override
	public void updateVisibleAndDisplayableRecordsForTable() {
		this.visibleAndDisplayableRecords.removeAllElements();
		this.displayRecords.removeAllElements();
		for (int i = 0; i < this.size(); ++i) {
			final Record record = this.get(i);
			if (record.isVisible && record.isDisplayable) getVisibleAndDisplayableRecords().add(record);
			getDisplayRecords().add(record);
		}
	}

	/**
	 * @return visible and display able records (p.e. to build the partial data table)
	 */
	@SuppressWarnings("unchecked")
	public Vector<Record> getVisibleAndDisplayableRecordsForTable() {
		return (Vector<Record>) (this.analyzer.getSettings().isPartialDataTable() ? this.visibleAndDisplayableRecords : this.displayRecords);
	}

	/**
	 * @return visible and displayable records (p.e. to build the partial data table)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Vector<Record> getVisibleAndDisplayableRecords() {
		return (Vector<Record>) this.visibleAndDisplayableRecords;
	}

	/**
	 * @return all records for display
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Vector<Record> getDisplayRecords() {
		return (Vector<Record>) this.displayRecords;
	}

	/**
	 * method to add an new record name
	 */
	public void addRecordName(String newRecordName) {
		String[] newRecordNames = new String[this.recordNames.length + 1];
		System.arraycopy(this.recordNames, 0, newRecordNames, 0, this.recordNames.length);
		newRecordNames[this.recordNames.length] = newRecordName;
		this.recordNames = newRecordNames;
	}

	/**
	 * method to remove a record name
	 */
	public void removeRecordName(String deleteRecordName) {
		Vector<String> newRecordNames = new Vector<String>();
		for (String recordKey : this.recordNames) {
			if (!recordKey.equals(deleteRecordName)) newRecordNames.add(recordKey);
		}
		this.recordNames = newRecordNames.toArray(new String[0]);
	}

	/**
	 * replace a record name with a new one
	 * @param oldRecord
	 * @param newRecordName
	 */
	public void replaceRecordName(Record oldRecord, String newRecordName) {
		// replace in recordNames array
		for (int i = 0; i < this.recordNames.length; i++) {
			if (this.recordNames[i].equals(oldRecord.name)) this.recordNames[i] = newRecordName;
		}
		if (this.get(newRecordName) == null) { // record may be created previously
			this.put(newRecordName, oldRecord.clone(newRecordName));
			this.remove(oldRecord.name);
		}
	}

	/**
	 * method to get the sorted record active names which are visible as string array
	 * @return String[] containing record names
	 */
	public String[] getDisplayableAndVisibleRecordNames() {
		Vector<String> displayableAndVisibleRecords = new Vector<String>();
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isDisplayable && this.get(recordKey).isVisible) displayableAndVisibleRecords.add(recordKey);
		}
		return displayableAndVisibleRecords.toArray(new String[0]);
	}

	/**
	 * method to get the sorted record active names which are visible as string array
	 * @return String[] containing record names
	 */
	public String[] getVisibleRecordNames() {
		Vector<String> visibleRecords = new Vector<String>();
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isVisible()) visibleRecords.add(recordKey);
		}
		return visibleRecords.toArray(new String[0]);
	}

	/**
	 * query number of visible and displayable records
	 * @return number of records visible and displayable (makeInActiveDisplayable)
	 */
	public int getNumberOfVisibleAndDisplayableRecords() {
		int visibleAndDisplayable = 0;
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isVisible && this.get(recordKey).isDisplayable) visibleAndDisplayable++;
		}
		return visibleAndDisplayable;
	}

	/**
	 * query number of displayable records
	 * @return number of records displayable (makeInActiveDisplayable)
	 */
	public int getNumberOfDisplayableRecords() {
		int displayable = 0;
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isDisplayable) displayable++;
		}
		return displayable;
	}

	/**
	 * method to get the sorted record displayable names as string array
	 * @return String[] containing record names
	 */
	public String[] getDisplayableRecordNames() {
		Vector<String> displayableRecords = new Vector<String>();
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isDisplayable()) displayableRecords.add(recordKey);
		}
		return displayableRecords.toArray(new String[0]);
	}

	/**
	 * method to get the sorted record active names as string array
	 * @return String[] containing record names
	 */
	public String[] getActiveRecordNames() {
		Vector<String> activeRecords = new Vector<String>();
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isActive()) activeRecords.add(recordKey);
		}
		return activeRecords.toArray(new String[0]);
	}

	/**
	 * method to set the sorted active or in active record names as string array
	 * - records which does not have inactive or active flag are calculated from active or inactive
	 * - all records not calculated may have the active status and must be stored
	 * - set this during IDevice.crossCheckMeasurements() to cache this and avoid recalculation based on initial device measurements
	 */
	public void setNoneCalculationRecordNames(final String[] recordNames) {
		this.noneCalculationRecords = recordNames;
	}

	/**
	 * method to get the sorted active or in active record names as string array
	 * - records which does not have inactive or active flag are calculated from active or inactive
	 * - all records not calculated may have the active status and must be stored
	 * @return String[] containing record names
	 */
	public String[] getNoneCalculationRecordNames() {
		if (this.noneCalculationRecords.length == 0) {
			this.noneCalculationRecords = this.device.getNoneCalculationMeasurementNames(this.parent.number, this.recordNames);
		}
		return this.noneCalculationRecords;
	}

	/**
	 * clear the record set compare view
	 */
	@Override
	public void clear() {
		super.clear();
		this.recordNames = new String[0];
		this.timeStep_ms = null;
		// this.maxSize = 0;
		this.maxTime = 0.0;
		this.maxValue = -20000;
		this.minValue = 20000;
		this.resetZoomAndMeasurement();
	}

	/**
	 * clears the data points in all records and in timeStep.
	 * reduce initial capacity to zero.
	 * does not clear any fields in the recordSet, in the records or in timeStep.
	 */
	public void cleanup() {
		// this.histoSettlements.clear();
		// this.histoScoregroups.clear();
		// this.visibleAndDisplayableRecords.clear();
		// this.visibleAndDisplayableRecords.trimToSize();
		// this.allRecords.clear();
		// this.allRecords.trimToSize();
		// this.scaleSyncedRecords.clear();

		this.timeStep_ms.clear();
		this.timeStep_ms.trimToSize();
		for (Record record : this.getValues()) {
			record.clear();
			record.trimToSize();
		}
		// clear(); because cleanup is intended to reduce the heap size this clear is not required
	}

	/**
	 * this method with param adjustObjectKey=true is 100% identical to the deprecated createRecordSet method
	 * method to create a record set with given name "1) Laden" containing records according the device channel/configuration
	 * which are loaded from device properties file
	 * @param recordSetName the name of the record set
	 * @param device the instance of the device
	 * @param channelConfigNumber (number of the outlet or configuration)
	 * @param isRaw defines if the data needs translation using device specific properties
	 * @param isFromFile defines if a configuration change must be recorded to signal changes
	 * @param adjustObjectKey defines if the channel's object key is updated by the settings objects key
	 * @return a record set containing all records (empty) as specified
	 */
	public static RecordSet createRecordSet(String recordSetName, IDevice device, int channelConfigNumber, boolean isRaw, boolean isFromFile, boolean adjustObjectKey) {
		Analyzer analyzer = Analyzer.getInstance();
		if (!(analyzer instanceof Explorer || analyzer instanceof TestAnalyzer)) throw new UnsupportedOperationException("for UI-based tests only, not for histo tests");
		return createRecordSet(recordSetName, analyzer, channelConfigNumber, isRaw, isFromFile, adjustObjectKey);
	}

	/**
	 * Use this constructor if a threadsafe operation is required.
	 * @param channelNumber the channel number to be used (supports channel mix)
	 * @param isRaw defines if the data needs translation using device specific properties
	 * @param isFromFile defines if a configuration change must be recorded to signal changes
	 * @param adjustObjectKey defines if the channel's object key is updated by the settings objects key
	 * @return a record set containing all records (empty) as specified
	 */
	public static RecordSet createRecordSet(String recordSetName, Analyzer analyzer, int channelNumber, boolean isRaw, boolean isFromFile, boolean adjustObjectKey) {
		String shortRecordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);

		IDevice device = analyzer.getActiveDevice();
		String[] recordNames = device.getMeasurementNamesReplacements(channelNumber);
		if (recordNames.length == 0) { // simple check for valid device and record names, as fall back use the config from the first channel/configuration
			recordNames = device.getMeasurementNamesReplacements(channelNumber = 1);
		}

		String[] recordSymbols = new String[recordNames.length];
		String[] recordUnits = new String[recordNames.length];
		for (int i = 0; i < recordNames.length; i++) {
			MeasurementType measurement = device.getMeasurement(channelNumber, i);
			recordSymbols[i] = measurement.getSymbol();
			recordUnits[i] = measurement.getUnit();
		}
		return createRecordSet(shortRecordSetName, analyzer, channelNumber, recordNames, recordSymbols, recordUnits, isRaw, isFromFile, adjustObjectKey);
	}

	/**
	 * this method with param adjustObjectKey=true is 100% identical to the deprecated createRecordSet method
	 * method to create a record set with given name "1) Laden" containing records according the given record names, symbols and units.
	 * active status as well as statistics and properties are used from device properties.
	 * @param recordSetName the name of the record set
	 * @param device the instance of the device
	 * @param channelConfigNumber (name of the outlet or configuration)
	 * @param recordNames array of names to be used for created records
	 * @param recordSymbols array of symbols to be used for created records
	 * @param recordUnits array of units to be used for created records
	 * @param timeStep_ms
	 * @param isRaw defines if the data needs translation using device specific properties
	 * @param isFromFile defines if a configuration change must be recorded to signal changes
	 * @param adjustObjectKey defines if the channel's object key is updated by the settings objects key
	 * @return a record set containing all records (empty) as specified
	 */
	public static RecordSet createRecordSet(String recordSetName, IDevice device, int channelConfigNumber, String[] recordNames, String[] recordSymbols,
			String[] recordUnits, double newTimeStep_ms, boolean isRaw, boolean isFromFile, boolean adjustObjectKey) {
		Analyzer analyzer = Analyzer.getInstance();
		if (!(analyzer instanceof Explorer || analyzer instanceof TestAnalyzer)) throw new UnsupportedOperationException("for UI-based tests only, not for histo tests");
		return createRecordSet(recordSetName, analyzer, channelConfigNumber, recordNames, recordSymbols, recordUnits, isRaw, isFromFile, adjustObjectKey);
	}

	/**
	 * Use this constructor if a threadsafe operation is required.
	 * @param channelNumber the channel number to be used (supports channel mix)
	 * @param isRaw defines if the data needs translation using device specific properties
	 * @param isFromFile defines if a configuration change must be recorded to signal changes
	 * @param adjustObjectKey defines if the channel's object key is updated by the settings objects key
	 * @return a record set containing all records (empty) as specified
	 */
	public static RecordSet createRecordSet(String recordSetName, Analyzer analyzer, int channelNumber, String[] recordNames, String[] recordSymbols, String[] recordUnits,
			boolean isRaw, boolean isFromFile, boolean adjustObjectKey) {
		RecordSet newRecordSet = new RecordSet(analyzer, channelNumber, recordSetName, recordNames, isRaw, isFromFile);
		if (log.isLoggable(Level.FINE)) printRecordNames("createRecordSet() " + newRecordSet.name + " - ", newRecordSet.getRecordNames()); //$NON-NLS-1$ //$NON-NLS-2$

		IDevice device = analyzer.getActiveDevice();
		for (int i = 0; i < recordNames.length; i++) {
			MeasurementType measurement = device.getMeasurement(channelNumber, i);
			Record tmpRecord = new Record(device, i, recordNames[i], recordSymbols[i], recordUnits[i], measurement.isActive(), measurement.getStatistics(), measurement.getProperty(),
					INITIAL_RECORD_CAPACITY);
			tmpRecord.setColorDefaultsAndPosition(i);
			newRecordSet.put(recordNames[i], tmpRecord);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added record for " + recordNames[i] + " - " + newRecordSet.size()); //$NON-NLS-1$
		}

		if (adjustObjectKey) {
			// check and update object key
			String activeObjectKey = analyzer.getSettings().getActiveObjectKey();
			if (!activeObjectKey.isEmpty()) {
				Channel activeChannel = analyzer.getActiveChannel();
				if (activeChannel != null && !activeChannel.getObjectKey().equals(activeObjectKey)) {
					activeChannel.setObjectKey(activeObjectKey);
				}
			}
		}
		newRecordSet.syncScaleOfSyncableRecords();
		return newRecordSet;
	}

	/**
	 * calculate number of records with axis position is left
	 */
	public int getNumberVisibleWithAxisPosLeft() {
		int value = 0;
		for (String recordKey : this.recordNames) {
			Record record = this.get(recordKey);
			if (record.isVisible && record.isDisplayable && record.isPositionLeft) ++value;
		}
		return value;
	}

	/**
	 * @return the device
	 */
	@Override
	public IDevice getDevice() {
		return this.device != null ? this.device : this.analyzer.getActiveDevice();
	}

	/**
	 * @return the channel/configuration number
	 */
	@Override
	public int getChannelConfigNumber() {
		return this.parent != null ? this.parent.number : 1; // compare set does not have a parent
	}

	/**
	 * @return the channel (or) configuration name
	 */
	public String getChannelConfigName() {
		return this.parent != null ? this.parent.channelConfigName : GDE.STRING_EMPTY;
	}

	/**
	 * check if this record set is one of the just active channel in UI
	 */
	public boolean isChildOfActiveChannel() {
		boolean isChild = false;
		RecordSet uiRecordSet = this.channels.getActiveChannel().get(this.name);
		if (uiRecordSet == this) isChild = true;
		return isChild;
	}

	/**
	 * overwrites default HashMap method
	 */
	@Override
	public Record put(String key, AbstractRecord record) {
		super.put(key, record);
		Record newRecord = this.get(key);
		// for compare set record following properties has to be checked at the point where
		newRecord.keyName = key;
		newRecord.parent = this;

		// add key to recordNames[] in case of TYPE_COMPARE_SET, keep ordinal to enable translate value
		if (this.isCompareSet || this.isUtilitySet) {
			this.addRecordName(key);
			newRecord.name = key;
			newRecord.ordinal = this.size() - 1;

			if (this.realSize() > 1) {
				// keep the color of first added record
				newRecord.setColorDefaultsAndPosition(this.realSize());
				// synchronize scale format
				newRecord.numberFormat = this.get(0).numberFormat;
				newRecord.df = (DecimalFormat) this.get(0).df.clone();
			} else {
				newRecord.setNumberFormat(-1);
			}
			// set all scales of compare set to left
			newRecord.isPositionLeft = true;
		}

		return newRecord;
	}

	/**
	 * Set the time step in msec for record sets of devices with constant time step with a positive value.
	 * A negative value signals none-constant time steps for this record set.
	 * This method has to be called BEFORE adding data points and will set the time step if called the first time only,
	 * if timeStep_ms needs to be changed for some reason use setNewTimeStep_ms
	 * @param newTimeStep_ms the timeStep_ms to set
	 */
	public void setTimeStep_ms(double newTimeStep_ms) {
		this.timeStep_ms = this.timeStep_ms == null || this.timeStep_ms.size() == 0 ? new TimeSteps(newTimeStep_ms) : this.timeStep_ms;
	}

	/**
	 * Set the time step in msec for record sets of devices with constant time step with a positive value.
	 * A negative value signals none-constant time steps for this record set.
	 * @param newTimeStep_ms the timeStep_ms to set
	 */
	public void setNewTimeStep_ms(double newTimeStep_ms) {
		this.timeStep_ms = new TimeSteps(newTimeStep_ms);
	}

	/**
	 * @return the isSaved
	 */
	public boolean isSaved() {
		return this.isSaved;
	}

	/**
	 * @param newValue the isSaved to set
	 */
	public void setSaved(boolean newValue) {
		if (newValue) this.unsaveReasons = new Vector<String>();
		this.isSaved = newValue;
	}

	/**
	 * set data unsaved with a given reason RecordSet.UNSAVED_REASON_*
	 * no influence for compare set
	 * @param unsavedReason
	 */
	public void setUnsaved(String unsavedReason) {
		if (!this.isCompareSet) {
			this.changeCounter++;
			this.isSaved = false;
			if (!this.unsaveReasons.contains(unsavedReason)) {
				this.unsaveReasons.add(unsavedReason);
			}
		}
	}

	/**
	 * @return the isRaw
	 */
	public boolean isRaw() {
		return this.isRaw;
	}

	/**
	 * @return the recordSetDescription
	 */
	public String getRecordSetDescription() {
		return this.description;
	}

	/**
	 * @param newRecordSetDescription the recordSetDescription to set
	 */
	public void setRecordSetDescription(String newRecordSetDescription) {
		this.setUnsaved(RecordSet.UNSAVED_REASON_COMMENT);
		this.description = newRecordSetDescription;
	}

	/**
	 * append import file name to description text
	 */
	public void descriptionAppendFilename(String fileName) {
		String tmpDescription = this.getRecordSetDescription();
		if (this.description.contains(GDE.STRING_NEW_LINE)) {
			this.description = this.description.substring(0, this.description.indexOf(GDE.CHAR_NEW_LINE)).trim() + Messages.getString(MessageIds.GDE_MSGT0681, new String[] { fileName })
					+ this.description.substring(tmpDescription.indexOf(GDE.CHAR_NEW_LINE));
		} else {
			this.description = this.description.trim() + Messages.getString(MessageIds.GDE_MSGT0681, new String[] { fileName });
		}
	}

	/**
	 * check if all records from this record set are displayable, starts calculation if required by calling makeInActiveDisplayable()
	 */
	public void checkAllDisplayable() {
		this.analyzer.getActiveDevice().makeInActiveDisplayable(this);
		this.isRecalculation = false;

		// start a low prio tread to load other record set data
		Thread dataLoadThread = new Thread(new Runnable() {
			@Override
			public void run() {
				CalculationThread ct = RecordSet.this.device.getCalculationThread();
				try {
					Thread.sleep(1000);
					while (ct != null && ct.isAlive()) {
						log.log(Level.FINER, "CalculationThread isAlive"); //$NON-NLS-1$
						Thread.sleep(1000);
					}
				} catch (InterruptedException e) {
					// ignore
				}
				Channel activeChannel = RecordSet.this.channels.getActiveChannel();
				if (activeChannel != null && activeChannel.getFullQualifiedFileName() != null) activeChannel.checkAndLoadData();
			}
		}, "DataLoadCheck");
		try {
			dataLoadThread.setPriority(Thread.NORM_PRIORITY - 2);
			dataLoadThread.start();
		} catch (RuntimeException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * set all records from this record to displayable
	 */
	public void setAllDisplayable() {
		for (String recordKey : this.recordNames) {
			this.get(recordKey).setDisplayable(true);
		}
	}

	/**
	 * force enable all records to be displayed and active if none calculation, this is used for unknown data import or unknown configurations
	 */
	public void setAllVisibleAndDisplayable() {
		for (String recordKey : this.recordNames) {
			Record record = this.get(recordKey);
			record.setVisible(true);
			record.setDisplayable(true);
		}
		for (String recordKey : this.getNoneCalculationRecordNames()) {
			this.get(recordKey).setActive(true);
		}
	}

	/**
	 * @return the isFromFile, this flag indicates to call the make allInActiveDisplayable
	 *         - the handling might be different if data captured directly from device
	 */
	public boolean isFromFile() {
		return this.isFromFile;
	}

	/**
	 * query the size of record set child record
	 * - normal record set will return the size of the data vector of first active in recordNames
	 * - zoomed set will return size of zoomOffset + zoomWith
	 * @return the size of data point to calculate the time unit
	 */
	public int getRecordDataSize(boolean isReal) {
		int size = 0;
		if (isReal) {
			for (String recordKey : this.recordNames) {
				if (get(recordKey).isActive()) {
					size = get(recordKey).realSize();
					break;
				}
			}
		} else {
			for (String recordKey : this.recordNames) {
				if (get(recordKey).isActive()) {
					size = get(recordKey).size();
					break;
				}
			}
		}
		return size;
	}

	/**
	 * @return the maxValue
	 */
	public double getMaxValue() {
		return this.maxValue;
	}

	/**
	 * @param newMaxValue the maxValue to set
	 */
	public void setMaxValue(double newMaxValue) {
		this.maxValue = newMaxValue;
	}

	/**
	 * @return the minValue
	 */
	public double getMinValue() {
		return this.minValue;
	}

	/**
	 * @param newMinValue the minValue to set
	 */
	public void setMinValue(double newMinValue) {
		this.minValue = newMinValue;
	}

	/**
	 * set maximum time in msec relating to the record defining the time scale of a compare set
	 * @param newMaxTime the maxSize to set
	 */
	public void setCompareSetMaxScaleTime_ms(double newMaxTime) {
		this.maxTime = newMaxTime;
	}

	/**
	 * get maximum time in msec relating to the record defining the time scale of a compare set
	 */
	public double getCompareSetMaxScaleTime_ms() {
		return this.maxTime;
	}

	/**
	 * @return the isZoomMode
	 */
	public boolean isZoomMode() {
		return this.isZoomMode;
	}

	/**
	 * set the mouse tracker in graphics window active for zoom window selection
	 * @param zoomModeEnabled the isZoomMode to set
	 */
	public void setZoomMode(boolean zoomModeEnabled) {
		if (!zoomModeEnabled) {
			this.resetMeasurement();
			if (this.recordNames.length != 0) { // check existence of records, a compare set may have no records
				// iterate children and reset min/max values
				for (Record record : this.getValues()) {
					record.zoomOffset = 0;
					record.zoomTimeOffset = 0.0;
					record.drawTimeWidth = record.getMaxTime_ms();
					// log.log(Level.INFO, this.name + "this.getMaxTime_ms() = " + record.drawTimeWidth);
					record.minZoomScaleValue = record.minScaleValue;
					record.maxZoomScaleValue = record.maxScaleValue;
					log.log(Level.FINER, this.name + " zoomTimeOffset " + TimeLine.getFomatedTimeWithUnit(record.zoomTimeOffset) + " drawTimeWidth " + TimeLine.getFomatedTimeWithUnit(record.drawTimeWidth)); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} else {
			if (!this.isZoomMode) {
				for (Record record : this.getValues()) {
					record.minZoomScaleValue = record.minScaleValue;
					record.maxZoomScaleValue = record.maxScaleValue;
				}
			}
		}
		this.isZoomMode = zoomModeEnabled;
		this.isScopeMode = false;
	}

	/**
	 * reset the record set in viewpoint of measurement and zooming
	 */
	public void resetZoomAndMeasurement() {
		this.setZoomMode(false);
		this.setMeasurementMode(this.recordKeyMeasurement, false);
		this.setDeltaMeasurementMode(this.recordKeyMeasurement, false);
	}

	/**
	 * set the zoom bounds from display to child records as record point offset and size
	 * @param newDisplayZoomBounds - where the start point offset is x,y and the area is width, height
	 */
	public void setDisplayZoomBounds(Rectangle newDisplayZoomBounds) {
		// iterate children
		for (Record record : this.getValues()) {
			record.setZoomBounds(newDisplayZoomBounds);
		}
	}

	/**
	 * set the scope size to record set to enable to display only the last size records point (scope mode)
	 * recordZoomOffset must be calculated each graphics refresh
	 * @param newScopeSize number of points shown
	 */
	public void setScopeSizeRecordPoints(int newScopeSize) {
		this.isScopeMode = true;
		this.scopeModeSize = newScopeSize;
	}

	/**
	 * query if the record set is in scope mode
	 */
	public boolean isScopeMode() {
		return this.isScopeMode;
	}

	/**
	 * query actual scope mode offset in record points
	 * @return scopeModeOffset
	 */
	public int getScopeModeOffset() {
		return this.scopeModeOffset;
	}

	/**
	 * set a new scope mode offset in record points
	 * @param newScopeModeOffset
	 */
	public void setScopeModeOffset(int newScopeModeOffset) {
		this.scopeModeOffset = newScopeModeOffset;
	}

	/**
	 * @return the scope mode size in record points
	 */
	public int getScopeModeSize() {
		return this.scopeModeSize;
	}

	/**
	 * @return the display start time in msec
	 */
	public double getStartTime() {
		double startTime = 0;
		if (this.isZoomMode) {
			startTime = (this.analyzer.getSettings() != null && this.analyzer.getSettings().isTimeFormatAbsolute() && this.timeStep_ms != null) ? this.timeStep_ms.startTimeStamp_ms + this.get(0).zoomTimeOffset
					: this.get(0).zoomTimeOffset;
		} else if (this.isScopeMode) {
			startTime = (this.analyzer.getSettings() != null && this.analyzer.getSettings().isTimeFormatAbsolute() && this.timeStep_ms != null)
					? this.timeStep_ms.startTimeStamp_ms + this.timeStep_ms.getTime_ms(this.scopeModeOffset + 1)
					: this.timeStep_ms.getTime_ms(this.scopeModeOffset + 1);
		}
		return startTime;
	}

	/**
	 * @return the isPanMode, panning is only possible in zoom mode
	 */
	public boolean isPanMode() {
		return this.isZoomMode;
	}

	public void shift(int xPercent, int yPercent) {
		// iterate children and set min/max values
		for (Record record : this.getValues()) {
			double xShift_ms = record.drawTimeWidth * xPercent / 100;
			if (record.zoomTimeOffset + xShift_ms <= 0) {
				record.zoomOffset = 0;
				record.zoomTimeOffset = 0.0;
			} else if (record.zoomTimeOffset + record.drawTimeWidth + xShift_ms > record.getMaxTime_ms()) {
				record.zoomTimeOffset = record.getMaxTime_ms() - record.drawTimeWidth;
				record.zoomOffset = record.findBestIndex(record.zoomTimeOffset);
			} else {
				record.zoomTimeOffset = record.zoomTimeOffset + xShift_ms;
				record.zoomOffset = record.findBestIndex(record.zoomTimeOffset);
			}

			double yShift = (record.getMaxScaleValue() - record.getMinScaleValue()) * yPercent / 100;
			record.minZoomScaleValue = record.getMinScaleValue() + yShift;
			record.maxZoomScaleValue = record.getMaxScaleValue() + yShift;
		}
	}

	/**
	 * @return the gridType
	 */
	public int getTimeGridType() {
		return this.timeGridType;
	}

	/**
	 * @param gridType the gridType to set
	 */
	public void setTimeGridType(int gridType) {
		this.timeGridType = gridType;
	}

	/**
	 * @return the timeGrid
	 */
	public Vector<Integer> getTimeGrid() {
		return this.timeGrid;
	}

	/**
	 * @param newTimeGrid the timeGrid to set
	 */
	public void setTimeGrid(Vector<Integer> newTimeGrid) {
		this.timeGrid = new Vector<Integer>(newTimeGrid);
	}

	/**
	 * @return the colorTimeGrid
	 */
	public Color getColorTimeGrid() {
		return this.timeGridColor;
	}

	/**
	 * @param colorTimeGrid the colorTimeGrid to set
	 */
	public void setTimeGridColor(Color colorTimeGrid) {
		this.timeGridColor = colorTimeGrid;
	}

	/**
	 * @return the lineStyleTimeGrid
	 */
	public int getLineStyleTimeGrid() {
		return this.timeGridLineStyle;
	}

	/**
	 * @param lineStyleTimeGrid the lineStyleTimeGrid to set
	 */
	public void setTimeGridLineStyle(int lineStyleTimeGrid) {
		this.timeGridLineStyle = lineStyleTimeGrid;
	}

	/**
	 * @return the configuredDisplayable
	 */
	public int getConfiguredDisplayable() {
		return this.configuredDisplayable;
	}

	/**
	 * @param newConfiguredDisplayableNumber the configuredDisplayable to set
	 */
	public void setConfiguredDisplayable(int newConfiguredDisplayableNumber) {
		log.log(Level.FINE, "configuredDisplayable = " + newConfiguredDisplayableNumber); //$NON-NLS-1$
		this.configuredDisplayable = newConfiguredDisplayableNumber;
	}

	/**
	 * get the record name to decide for horizontal grid lines
	 * @return the horizontalGridRecord
	 */
	@Deprecated
	public String getValueGridRecordName() {
		String gridRecordName = this.valueGridRecordOrdinal == -1 || this.valueGridRecordOrdinal > this.getRecordNames().length - 1 ? GDE.STRING_DASH
				: this.getRecordNames()[this.valueGridRecordOrdinal];
		boolean isOneOfSycableAndOneOfSynableVisible = this.valueGridRecordOrdinal >= 0 && this.isOneOfSyncableRecord(this.get(this.valueGridRecordOrdinal).getName())
				&& this.isOneSyncableVisible(this.getSyncMasterRecordOrdinal(gridRecordName));
		if (this.get(gridRecordName) != null && !isOneOfSycableAndOneOfSynableVisible && !(this.get(gridRecordName).isVisible && this.get(gridRecordName).isDisplayable)) {
			gridRecordName = this.getFirstRecordName();
			log.log(Level.FINE, "gridRecordName = " + gridRecordName); //$NON-NLS-1$
		}
		if (this.isCompareSet) {
			gridRecordName = this.realSize() == 0 ? GDE.STRING_DASH : this.getFirstRecordName();
			log.log(Level.FINE, "gridRecordName = " + gridRecordName); //$NON-NLS-1$
		}
		return gridRecordName;
	}

	/**
	 * @return the horizontalGridRecord ordinal
	 */
	@Override
	public int getValueGridRecordOrdinal() {
		return this.isCompareSet ? this.get(0).ordinal : this.valueGridRecordOrdinal;
	}

	/**
	 * @return the isRecalculation
	 */
	public boolean isRecalculation() {
		return this.isRecalculation;
	}

	public String getFirstRecordName() {
		return this.recordNames[0];
	}

	/**
	 * set if a recalculation of depending calculated records are required
	 */
	public void setRecalculationRequired() {
		this.isRecalculation = true;
		for (int i = 0; i < this.device.getNumberOfMeasurements(this.parent.number) && i < this.size(); ++i) {
			if (this.device.getMeasurement(this.parent.number, i).isCalculation()) {
				this.get(i).resetMinMax();
			}
			this.get(i).resetStatiticCalculationBase();
		}
	}

	/**
	 * get all record properties in serialized form
	 * @return serializedRecordProperties
	 */
	public String getSerializeProperties() {
		StringBuilder sb = new StringBuilder();

		sb.append(TIME_STEP_MS).append(GDE.STRING_EQUAL).append(this.timeStep_ms.isConstant ? this.getAverageTimeStep_ms() : -1).append(Record.DELIMITER);
		sb.append(START_TIME_STAMP).append(GDE.STRING_EQUAL).append(this.timeStep_ms.getStartTimeStamp()).append(Record.DELIMITER);

		sb.append(TIME_GRID_TYPE).append(GDE.STRING_EQUAL).append(this.timeGridType).append(Record.DELIMITER);
		sb.append(TIME_GRID_LINE_STYLE).append(GDE.STRING_EQUAL).append(this.timeGridLineStyle).append(Record.DELIMITER);
		sb.append(TIME_GRID_COLOR).append(GDE.STRING_EQUAL).append(this.timeGridColor.getRed()).append(GDE.STRING_COMMA).append(this.timeGridColor.getGreen()).append(GDE.STRING_COMMA).append(this.timeGridColor.getBlue()).append(Record.DELIMITER);

		sb.append(VALUE_GRID_RECORD_ORDINAL).append(GDE.STRING_EQUAL).append(this.valueGridRecordOrdinal).append(Record.DELIMITER);
		sb.append(VALUE_GRID_TYPE).append(GDE.STRING_EQUAL).append(this.valueGridType).append(Record.DELIMITER);
		sb.append(VALUE_GRID_LINE_STYLE).append(GDE.STRING_EQUAL).append(this.valueGridLineStyle).append(Record.DELIMITER);
		sb.append(VALUE_GRID_COLOR).append(GDE.STRING_EQUAL).append(this.valueGridColor.getRed()).append(GDE.STRING_COMMA).append(this.valueGridColor.getGreen()).append(GDE.STRING_COMMA)
				.append(this.valueGridColor.getBlue()).append(Record.DELIMITER);

		sb.append(SMOOTH_AT_CURRENT_DROP).append(GDE.STRING_EQUAL).append(this.isSmoothAtCurrentDrop).append(Record.DELIMITER);
		sb.append(SMOOTH_VOLTAGE_CURVE).append(GDE.STRING_EQUAL).append(this.isSmoothVoltageCurve).append(Record.DELIMITER);

		sb.append(VOLTAGE_LIMITS).append(GDE.STRING_EQUAL);
		for (int value : this.voltageLimits) {
			sb.append(value).append(GDE.STRING_COMMA);
		}
		sb.deleteCharAt(sb.length() - 1);

		return sb.toString().endsWith(Record.DELIMITER) ? sb.substring(0, sb.lastIndexOf(Record.DELIMITER)) : sb.toString();
	}

	/**
	 * set all record properties by given serialized form
	 * @param serializedRecordSetProperties
	 */
	public void setDeserializedProperties(String serializedRecordSetProperties) {
		HashMap<String, String> recordSetProps = StringHelper.splitString(serializedRecordSetProperties, Record.DELIMITER, RecordSet.propertyKeys);
		String tmpValue = null;
		try {
			tmpValue = recordSetProps.get(TIME_STEP_MS);
			if (tmpValue != null && tmpValue.length() > 0) this.timeStep_ms = new TimeSteps(Double.parseDouble(tmpValue.trim()));
			tmpValue = recordSetProps.get(START_TIME_STAMP);
			if (tmpValue != null && tmpValue.length() > 0)
				this.timeStep_ms.setStartTimeStamp(Long.parseLong(tmpValue));
			else {
				String recordSetDescription = this.getRecordSetDescription();
				Pattern datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
				Matcher dateMatcher = datePattern.matcher(recordSetDescription);
				Pattern timePattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}");
				Matcher timeMatcher = timePattern.matcher(recordSetDescription);
				if (dateMatcher.find() && timeMatcher.find()) {
					String date = dateMatcher.group();
					String time = timeMatcher.group();
					log.logp(Level.FINE, $CLASS_NAME, "setDeserializedProperties", date + " " + time);

					String[] strValueDate = date.split(GDE.STRING_DASH);
					int year = Integer.parseInt(strValueDate[0]);
					int month = Integer.parseInt(strValueDate[1]);
					int day = Integer.parseInt(strValueDate[2]);

					String[] strValueTime = time.split(GDE.STRING_COLON);
					int hour = Integer.parseInt(strValueTime[0]);
					int minute = Integer.parseInt(strValueTime[1]);
					int second = Integer.parseInt(strValueTime[2]);

					GregorianCalendar calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
					this.timeStep_ms.setStartTimeStamp(calendar.getTimeInMillis());
				}
			}

			tmpValue = recordSetProps.get(TIME_GRID_TYPE);
			if (tmpValue != null && tmpValue.length() > 0) this.timeGridType = Integer.valueOf(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(TIME_GRID_LINE_STYLE);
			if (tmpValue != null && tmpValue.length() > 0) this.timeGridLineStyle = Integer.valueOf(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(TIME_GRID_COLOR);
			if (tmpValue != null && tmpValue.length() > 5) this.timeGridColor = SWTResourceManager.getColor(Integer.valueOf(tmpValue.split(GDE.STRING_COMMA)[0]),
					Integer.valueOf(tmpValue.split(GDE.STRING_COMMA)[1]), Integer.valueOf(tmpValue.split(GDE.STRING_COMMA)[2]));

			// begin depreciated
			tmpValue = recordSetProps.get(VALUE_GRID_RECORD);
			if (tmpValue != null && tmpValue.length() > 0) {
				for (String recordKey : this.recordNames) {
					if (recordKey.equals(tmpValue)) {
						this.valueGridRecordOrdinal = this.get(recordKey).getOrdinal();
						break;
					}
				}
			}
			// end depreciated
			tmpValue = recordSetProps.get(VALUE_GRID_RECORD_ORDINAL);
			if (tmpValue != null && tmpValue.length() > 0) {
				try {
					this.valueGridRecordOrdinal = Integer.valueOf(tmpValue.trim());
				} catch (Exception e) {
					this.valueGridRecordOrdinal = -1;
				}
			}
			tmpValue = recordSetProps.get(VALUE_GRID_TYPE);
			if (tmpValue != null && tmpValue.length() > 0) this.valueGridType = Integer.valueOf(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(VALUE_GRID_LINE_STYLE);
			if (tmpValue != null && tmpValue.length() > 0) this.valueGridLineStyle = Integer.valueOf(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(VALUE_GRID_COLOR);
			if (tmpValue != null && tmpValue.length() > 5) this.valueGridColor = SWTResourceManager.getColor(Integer.valueOf(tmpValue.split(GDE.STRING_COMMA)[0]),
					Integer.valueOf(tmpValue.split(GDE.STRING_COMMA)[1]), Integer.valueOf(tmpValue.split(GDE.STRING_COMMA)[2]));

			tmpValue = recordSetProps.get(SMOOTH_AT_CURRENT_DROP);
			if (tmpValue != null && tmpValue.length() > 0) this.isSmoothAtCurrentDrop = Boolean.valueOf(tmpValue.trim()).booleanValue();
			tmpValue = recordSetProps.get(SMOOTH_VOLTAGE_CURVE);
			if (tmpValue != null && tmpValue.length() > 0) this.isSmoothVoltageCurve = Boolean.valueOf(tmpValue.trim()).booleanValue();

			tmpValue = recordSetProps.get(VOLTAGE_LIMITS);
			if (tmpValue != null && tmpValue.length() > 0) {
				String[] strVoltageValues = tmpValue.trim().split(GDE.STRING_COMMA);
				for (int i = 0; i < strVoltageValues.length && i < this.voltageLimits.length; i++) {
					this.voltageLimits[i] = Integer.valueOf(strVoltageValues[i].trim());
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE0002) + GDE.STRING_NEW_LINE + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
	}

	/**
	 * query if the record set is zoomed and the zoomed data extract starts at first data point
	 * @return true if zoom is active and starts at left edge of curve
	 */
	public boolean isCutLeftEdgeEnabled() {
		try {
			return DataExplorer.getInstance().isRecordSetVisible(GraphicsType.NORMAL) && this.isZoomMode && (this.get(0).zoomTimeOffset == 0);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * query if the record set is zoomed and the zoomed data extract ends at last data point
	 * @return true if zoom is active and starts at right edge of curve
	 */
	public boolean isCutRightEdgeEnabled() {
		Record tmpRecord;
		try {
			tmpRecord = this.get(0);
		} catch (Exception e) {
			return false;
		}
		return DataExplorer.getInstance().isRecordSetVisible(GraphicsType.NORMAL) && this.isZoomMode && (tmpRecord.zoomTimeOffset + tmpRecord.drawTimeWidth >= tmpRecord.getMaxTime_ms());
	}

	/**
	 * @return the unsaveReasons
	 */
	public Vector<String> getUnsaveReasons() {
		return this.unsaveReasons;
	}

	/**
	 * @return the changeCounter
	 */
	public int getChangeCounter() {
		return this.changeCounter;
	}

	/**
	 * synchronize scales according device properties
	 */
	@Override
	public void syncScaleOfSyncableRecords() {
		if (this.isCompareSet) return;
		this.scaleSyncedRecords.initSyncedScales(this);
	}

	/**
	 * update the scale values from sync record if visible
	 * and update referenced records to enable drawing of curve, set min/max
	 */
	public void updateSyncRecordScale() {
		for (int syncRecordOrdinal : this.scaleSyncedRecords.keySet()) {
			boolean isAffected = false;
			int tmpSyncMin = Integer.MAX_VALUE;
			int tmpSyncMax = Integer.MIN_VALUE;
			for (Record tmpRecord : this.getScaleSyncedRecords().get(syncRecordOrdinal)) {
				synchronized (tmpRecord) {
					if (tmpRecord.isVisible && tmpRecord.isDisplayable) {
						isAffected = true;
						int tmpMin = Double.valueOf(tmpRecord.getMinValue() * tmpRecord.syncMasterFactor).intValue();
						int tmpMax = Double.valueOf(tmpRecord.getMaxValue() * tmpRecord.syncMasterFactor).intValue();
						if (tmpMin != 0 || tmpMax != 0) {
							if (log.isLoggable(Level.FINE)) log.log(Level.FINE, tmpRecord.name + " tmpMin  = " + tmpMin / 1000.0 + "; tmpMax  = " + tmpMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
							if (tmpMin < tmpSyncMin) tmpSyncMin = tmpMin;
							if (tmpMax > tmpSyncMax) tmpSyncMax = tmpMax;
						}
					}
				}
			}
			for (Record tmpRecord : this.getScaleSyncedRecords().get(syncRecordOrdinal)) {
				synchronized (tmpRecord) {
					if (this.isScopeMode) {
						tmpRecord.scopeMin = tmpSyncMin;
						tmpRecord.scopeMax = tmpSyncMax;
					} else {
						tmpRecord.syncMinValue = tmpSyncMin;
						tmpRecord.syncMaxValue = tmpSyncMax;
					}
				}
			}
			if (syncRecordOrdinal < this.realSize()) {
				Record syncRecord = this.get(syncRecordOrdinal);
				synchronized (syncRecord) {
					if (this.isScopeMode) {
						syncRecord.scopeMin = tmpSyncMin;
						syncRecord.scopeMax = tmpSyncMax;
					} else {
						syncRecord.syncMinValue = tmpSyncMin;
						syncRecord.syncMaxValue = tmpSyncMax;
					}
				}
			} else {
				log.log(Level.WARNING, String.format("Check device XML regarding <property name=\"scale_sync_ref_ordinal\" value=\"%d\" type=\"Integer\" />", syncRecordOrdinal));
			}

			if (isAffected && log.isLoggable(Level.FINE)) log.log(Level.FINE, this.get(syncRecordOrdinal).getSyncMasterName() + "; syncMin = " + tmpSyncMin / 1000.0 + "; syncMax = " + tmpSyncMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@SuppressWarnings("unchecked")
	private SyncedRecords<Record> getScaleSyncedRecords() {
		return (SyncedRecords<Record>) this.scaleSyncedRecords;
	}

	/**
	 * @return the Vector containing the slave records sync by the master name
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Vector<Record> getScaleSyncedRecords(int syncMasterRecordOrdinal) {
		return (Vector<Record>) this.scaleSyncedRecords.get(syncMasterRecordOrdinal);
	}

	/**
	 * @return the fileDataSize, number of integer values per record
	 */
	public int getFileDataSize() {
		return this.fileDataSize;
	}

	/**
	 * @return the fileDataPointer, file pointer where the data of this record begins
	 */
	public long getFileDataPointer() {
		return this.fileDataPointer;
	}

	/**
	 * @return the fileDataBytes, number of bytes containing all records data
	 */
	public int getFileDataBytesSize() {
		return this.fileDataBytes;
	}

	/**
	 * @param newFileDataPointer the file data pointer to set, seek point to read data
	 * @param newFileRecordDataSize the file record size to set
	 * @param newFileRecordSetDataBytes the file data bytes to set
	 */
	public void setFileDataPointerAndSize(long newFileDataPointer, int newFileRecordDataSize, int newFileRecordSetDataBytes) {
		this.fileDataPointer = newFileDataPointer;
		this.fileDataSize = newFileRecordDataSize;
		this.fileDataBytes = newFileRecordSetDataBytes;
	}

	/**
	 * @return the hasDisplayableData
	 */
	public boolean hasDisplayableData() {
		return this.hasDisplayableData;
	}

	/**
	 * @param enable the hasDisplayableData to set
	 */
	public void setDisplayableData(boolean enable) {
		this.hasDisplayableData = enable;
	}

	/**
	 * load data from file
	 */
	public void loadFileData(String fullQualifiedFileName, boolean doShowProgress) {
		try {
			if (this.fileDataSize != 0 && this.fileDataPointer != 0) {
				if (fullQualifiedFileName.endsWith(GDE.FILE_ENDING_OSD))
					OsdReaderWriter.readRecordSetsData(this, fullQualifiedFileName, doShowProgress);
				else if (fullQualifiedFileName.endsWith(GDE.FILE_ENDING_LOV)) LogViewReader.readRecordSetsData(this, fullQualifiedFileName, doShowProgress);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialog(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
	}

	/**
	 * @return the xScale
	 */
	public int getXScale() {
		return this.xScaleStep;
	}

	/**
	 * @param value the x scale step to set
	 */
	public void setXScale(int value) {
		this.xScaleStep = value;
	}

	/**
	 * @return the voltageLimits for LiXx cells if enabled
	 */
	public int[] getVoltageLimits() {
		return this.voltageLimits;
	}

	/**
	 * set the voltage limits to set for battery cells if enabled
	 */
	public void setVoltageLimits() {
		this.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
		this.voltageLimits = CellVoltageValues.getVoltageLimits();
	}

	/**
	 * @param enable the isScopeMode
	 */
	public void setScopeMode(boolean enable) {
		this.isScopeMode = enable;
		if (enable) {
			// iterate children and set min/max values
			for (String recordKey : this.recordNames) {
				// StringBuilder sb = new StringBuilder();
				Record record = this.get(recordKey);
				if (record.isVisible && record.isDisplayable) {
					int min = 0, max = 0, value;
					for (int i = this.scopeModeOffset; i < record.realSize(); i++) {
						value = record.realGet(i);
						if (i == this.scopeModeOffset)
							min = max = value;
						else {
							if (value > max)
								max = value;
							else if (value < min) min = value;
						}
						// sb.append(value).append(", ");
					}
					// log.log(Level.INFO, sb.toString());
					log.log(Level.FINE, record.getName() + ": scopeMin = " + min / 1000.0 + "; scopeMax = " + max / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
					record.setScopeMinMax(min, max);
				}
			}
		}
	}

	/**
	 * set absolute start and end time of this record set
	 * @param startTimeStamp
	 */
	public void setStartTimeStamp(long startTimeStamp) {
		if (this.timeStep_ms != null) {
			this.timeStep_ms.setStartTimeStamp(startTimeStamp);
		} else
			log.log(Level.WARNING, "time step vector is null !");
	}

	/**
	 * @return the record set start time stamp
	 */
	public long getStartTimeStamp() {
		return this.timeStep_ms != null ? this.timeStep_ms.getStartTimeStamp() : new Date().getTime();
	}

	/**
	 * @return true if this record set contains GPS data type records which enable related calculations and KML/KMZ export
	 */
	public boolean containsGPSdata() {
		int sumGpsRelatedRecords = 0;
		for (Record record : this.getValues()) {
			if (record.dataType == Record.DataType.GPS_LATITUDE || record.dataType == Record.DataType.GPS_LONGITUDE || record.dataType == Record.DataType.GPS_ALTITUDE) ++sumGpsRelatedRecords;
		}
		return sumGpsRelatedRecords >= 3;
	}

	/**
	 * query record with specified Record.DataType
	 * @return record ordinal or -1 if not found
	 */
	public int getRecordOrdinalOfType(Record.DataType dataType) {
		for (Record record : this.getValues()) {
			if (record.dataType == dataType) {
				if (record.hasReasonableData()) return record.ordinal;
			}
		}
		return -1;
	}

	/**
	 * query record with specified Record.DataType
	 * @return record ordinal or -1 if not found
	 */
	public int getRecordOrdinalOfDataType(Record.DataType dataType) {
		for (Record record : this.getValues()) {
			if (record.dataType == dataType) {
				return record.ordinal;
			}
		}
		return -1;
	}

	/**
	 * find the first occurrence of given unit samples and return the record ordinal
	 * @param units string array of unit
	 * @return record ordinal or -1 if not found
	 */
	public int findRecordOrdinalByUnit(String[] units) {
		for (Record record : this.getValues()) {
			for (String unit : units) {
				if (record.getUnit().toLowerCase().contains(unit.toLowerCase()) && record.hasReasonableData()) return record.getOrdinal();
			}
		}
		return -1;
	}

	/**
	 * find the first occurrence of given unit samples and return the record ordinal if the value range is within the given boundaries
	 * @param units string array of unit
	 * @param minBoundaryValue lower value boundary
	 * @param maxBoundaryValue upper value boundary
	 * @return record ordinal or -1 if not found
	 */
	public int findRecordOrdinalByUnit(String[] units, int minBoundaryValue, int maxBoundaryValue) {
		for (Record record : this.getValues()) {
			if (record.getMinValue() / 1000 >= minBoundaryValue && record.getMaxValue() / 1000 <= maxBoundaryValue) for (String unit : units) {
				if (record.getUnit().toLowerCase().contains(unit.toLowerCase()) && record.hasReasonableData()) return record.getOrdinal();
			}
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	public Collection<Record> getValues() {
		return (Collection<Record>) (Collection<?>) values();
	}

	/**
	 * @param recordKey the key which record should be measured
	 * @param enabled the boolean value to set
	 */
	public void setMeasurementMode(String recordKey, boolean enabled) {
		Record record = this.get(recordKey);
		if (record != null) {
			record.setMeasurementMode(enabled);
			if (enabled) {
				Record oldRecord = this.get(this.recordKeyMeasurement);
				if (oldRecord != null && !oldRecord.equals(record)) {
					oldRecord.setMeasurementMode(false);
					oldRecord.setDeltaMeasurementMode(false);
					oldRecord.setAvgMedianMeasurementMode(false);
				}
				this.recordKeyMeasurement = recordKey;
				record.setDeltaMeasurementMode(false);
				record.setAvgMedianMeasurementMode(false);
			}
		}
	}

	/**
	 * @param recordKey the key which record should be measured
	 * @param enabled the boolean value to set
	 */
	public void setDeltaMeasurementMode(String recordKey, boolean enabled) {
		Record record = this.get(recordKey);
		if (record != null) {
			record.setDeltaMeasurementMode(enabled);
			if (enabled) {
				Record oldRecord = this.get(this.recordKeyMeasurement);
				if (oldRecord != null && !oldRecord.equals(record)) {
					oldRecord.setMeasurementMode(false);
					oldRecord.setDeltaMeasurementMode(false);
					oldRecord.setAvgMedianMeasurementMode(false);
				}
				this.recordKeyMeasurement = recordKey;
				record.setMeasurementMode(false);
				record.setAvgMedianMeasurementMode(false);
			}
		}
	}

	/**
	 * @param recordKey the key which record should be measured
	 * @param enabled the boolean value to set
	 */
	public void setAvgMedianMeasurementMode(String recordKey, boolean enabled) {
		Record record = this.get(recordKey);
		if (record != null) {
			record.setAvgMedianMeasurementMode(enabled);
			if (enabled) {
				Record oldRecord = this.get(this.recordKeyMeasurement);
				if (oldRecord != null && !oldRecord.equals(record)) {
					oldRecord.setMeasurementMode(false);
					oldRecord.setDeltaMeasurementMode(false);
					oldRecord.setAvgMedianMeasurementMode(false);
				}
				this.recordKeyMeasurement = recordKey;
				record.setMeasurementMode(false);
				record.setDeltaMeasurementMode(false);
			}
		}
	}

	/**
	 * Clear measurement modes if any.
	 */
	public void clearMeasurementModes() {
		Record record = this.get(this.recordKeyMeasurement);
		if (record != null) {
			record.setMeasurementMode(false);
			record.setDeltaMeasurementMode(false);
			record.setAvgMedianMeasurementMode(false);
		}
	}

	/**
	 * @param recordKey the key which record should be measured
	 * @return the isMeasurementMode
	 */
	public boolean isMeasurementMode(String recordKey) {
		return this.get(recordKey) != null ? this.get(recordKey).isMeasurementMode() : false;
	}

	/**
	 * @param recordKey the key which record should be measured
	 * @return the isDeltaMeasurementMode
	 */
	public boolean isDeltaMeasurementMode(String recordKey) {
		return this.get(recordKey) != null ? this.get(recordKey).isDeltaMeasurementMode() : false;
	}

	/**
	 * @param recordKey the key which record should be measured
	 * @return the isAvgMedianMeasurementMode (average & median calculation)
	 */
	public boolean isAvgMedianMeasurementMode(String recordKey) {
		return this.get(recordKey) != null ? this.get(recordKey).isAvgMedianMeasurementMode() : false;
	}

	/**
	 * Reset the record set in viewpoint of measurement.
	 */
	public void resetMeasurement() {
		this.setMeasurementMode(this.recordKeyMeasurement, false);
		this.setDeltaMeasurementMode(this.recordKeyMeasurement, false);
		this.setAvgMedianMeasurementMode(this.recordKeyMeasurement, false);
	}

	/**
	 * @return the recordKeyMeasurement
	 */
	public String getRecordKeyMeasurement() {
		return this.recordKeyMeasurement;
	}

	/**
	 * @return the curveBounds, this is the area where curves are drawn
	 */
	public Rectangle getDrawAreaBounds() {
		return this.drawAreaBounds;
	}

	/**
	 * Define the area where curves are drawn (clipping, image).
	 * @param newDrawAreaBounds the curveBounds to set
	 */
	public void setDrawAreaBounds(Rectangle newDrawAreaBounds) {
		this.drawAreaBounds = newDrawAreaBounds;
		log.finest(() -> "drawAreaBounds = " + this.drawAreaBounds); //$NON-NLS-1$
	}

	public Analyzer getAnalyzer() {
		return this.analyzer;
	}
}
