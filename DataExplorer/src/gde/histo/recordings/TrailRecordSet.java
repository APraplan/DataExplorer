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

import static java.util.logging.Level.FINER;
import static java.util.logging.Level.WARNING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import gde.Analyzer;
import gde.GDE;
import gde.data.AbstractRecord;
import gde.data.AbstractRecordSet;
import gde.data.Record;
import gde.data.Record.DataType;
import gde.data.RecordSet;
import gde.data.TimeSteps;
import gde.device.DeviceConfiguration;
import gde.device.IChannelItem;
import gde.device.IDevice;
import gde.device.ScoreLabelTypes;
import gde.device.TrailTypes;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.HistoVault;
import gde.histo.config.HistoGraphicsTemplate;
import gde.histo.datasources.HistoSet;
import gde.histo.device.ChannelItems;
import gde.histo.gpslocations.GpsCluster;
import gde.histo.recordings.TrailDataTags.DataTag;
import gde.histo.ui.AbstractChartComposite.AbstractChartData;
import gde.histo.ui.GraphicsComposite;
import gde.histo.ui.HistoExplorer;
import gde.histo.ui.HistoSummaryWindow;
import gde.histo.ui.SummaryComposite.SummaryLayout;
import gde.histo.utils.GpsCoordinate;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * Hold histo trail records for the configured measurements of a device supplemented by settlements and scores.
 * The display sequence is the linked hashmap sequence whereas the ordinals refer to the sequence of measurements + settlements +
 * scoregroups.
 * @author Thomas Eickert
 */
public final class TrailRecordSet extends AbstractRecordSet {
	@SuppressWarnings("hiding")
	private static final String	$CLASS_NAME					= TrailRecordSet.class.getName();
	private static final long		serialVersionUID		= -1580283867987273535L;
	@SuppressWarnings("hiding")
	private static final Logger	log									= Logger.getLogger($CLASS_NAME);

	public static final String	BASE_NAME_SEPARATOR	= " | ";

	/**
	 * Collect input data for the trail recordset and subordinate objects.
	 * Support initial collection and collections after user input (e.g. trail type selection).
	 * @author Thomas Eickert (USER)
	 */
	final class RecordingsCollector {
		@SuppressWarnings("hiding")
		private final Logger log = Logger.getLogger(RecordingsCollector.class.getName());

		/**
		 * Set time steps for the trail recordset and the data points for all trail records.
		 * Every record takes the selected trail type / score data from the history vault and populates its data.
		 */
		void addVaultsToRecordSet() {
			for (HistoVault histoVault : pickedVaults.indexedVaults) {
				int duration_mm = histoVault.getScorePoint(ScoreLabelTypes.DURATION_MM.ordinal());
				durations_mm.add(duration_mm);
				if (!timeStep_ms.addRaw(histoVault.getLogStartTimestamp_ms() * 10)) {
					log.warning(() -> String.format("Duplicate recordSet  startTimeStamp %,d  %s", histoVault.getLogStartTimestamp_ms(), ((ExtendedVault) histoVault).getLoadFilePath())); //$NON-NLS-1$
				}
				dataTags.add((ExtendedVault) histoVault);
			}
		}

		/**
		 * Add GPS location tagging information based on the latitude / longitude median.
		 * Support asynchronous geocode fetches from the internet.
		 */
		void setGpsLocationsTags() {
			// locate the GPS coordinates records
			TrailRecord latitudeRecord = null, longitudeRecord = null;
			for (TrailRecord trailRecord : TrailRecordSet.this.getValues()) {
				if (trailRecord.getDataType() == DataType.GPS_LATITUDE)
					latitudeRecord = trailRecord;
				else if (trailRecord.getDataType() == DataType.GPS_LONGITUDE) longitudeRecord = trailRecord;
				if (latitudeRecord != null && longitudeRecord != null) break;
			}

			if (latitudeRecord != null && longitudeRecord != null) {
				// provide GPS coordinates for clustering which is the prerequisite for adding the location to dataGpsLocations
				GpsCluster gpsCluster = pickedVaults.defineGpsAverages(latitudeRecord, longitudeRecord);
				// populate the GPS locations list for subsequently filling the histo table
				if (gpsCluster.parallelStream().filter(Objects::nonNull).count() > 0) {
					Thread gpsLocationsThread = new Thread((Runnable) () -> setGpsLocationTags(gpsCluster), "setGpsLocationTags"); //$NON-NLS-1$
					try {
						gpsLocationsThread.start();
					} catch (RuntimeException e) {
						log.log(WARNING, e.getMessage(), e);
					}
				}
			}
		}

		/**
		 * Populate the GPS locations list if there are any GPS locations in this recordset.
		 * Trigger refilling the histo table.
		 * @param gpsCluster holds the GPS coordinates and the assignment to clusters; null coordinates are allowed
		 */
		private void setGpsLocationTags(GpsCluster gpsCluster) {
			long nanoTime = System.nanoTime();
			gpsCluster.setClusters(analyzer.getSettings().getGpsLocationRadius());
			if (gpsCluster.size() > 0) {
				List<String> gpsLocations = gpsCluster.defineGpsLocations(analyzer.getSettings().getGpsLocationRadius(), analyzer.getDataAccess());
				// fill the data tags only if there is at least one GPS coordinate
				if (gpsLocations.parallelStream().filter(s -> !s.isEmpty()).count() > 0) {
					getDataTags().add(gpsLocations);
				}
				// refresh the histo table which might already have been painted without the GPS coordinates
				if (getDataTags().getDataGpsLocations().size() > 0) {
					if (GDE.isWithUi()) DataExplorer.getInstance().getPresentHistoExplorer().updateHistoTableWindow(false);
					log.finer(() -> "fill in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + " ms!  GPS locations size=" + gpsCluster.getAssignedClusters().values().size()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}

		/**
		 * Inform displayable trail records about the trail types which are allowed, set trail selection list and current trailType / score.
		 */
		void defineTrailTypes() {
			String[] trailRecordNames = getRecordNames();
			for (String trailRecordName : trailRecordNames) {
				TrailRecord trailRecord = get(trailRecordName);
				trailRecord.trailSelector.setApplicableTrails();
			}
		}

	}

	/**
	 * Keeps a shallow copy of the startTimeStamp vault map.
	 */
	public final class PickedVaults {

		/**
		 * Data source for this recordset.
		 * Key is recordSet startTimeStamp in reverse order.
		 */
		private final TreeMap<Long, List<ExtendedVault>>	initialVaults	= new TreeMap<>(Collections.reverseOrder());
		/**
		 * Reference array for accessing vaults by index.
		 * Same set of vaults as {@link PickedVaults#initialVaults}.
		 * RecordSet startTimeStamp reverse order.
		 */
		private final HistoVault[]												indexedVaults;

		/**
		 * @param initialVaults in startTimeStamp in reverse order for creating a new map
		 */
		public PickedVaults(TreeMap<Long, List<ExtendedVault>> initialVaults) {
			this.initialVaults.putAll(initialVaults);
			Stream<ExtendedVault> map1 = this.initialVaults.values().stream().flatMap(List::stream).distinct();
			indexedVaults = map1.toArray(ExtendedVault[]::new);
		}

		/**
		 * @return a GPS location coordinates list
		 */
		GpsCluster defineGpsAverages(TrailRecord latitudeRecord, TrailRecord longitudeRecord) {
			GpsCluster gpsCluster = new GpsCluster();
			// provide GPS coordinates for clustering which is the prerequisite for adding the location to dataGpsLocations
			for (HistoVault histoVault : indexedVaults) {
				Integer latitudePoint = histoVault.getMeasurementPoint(latitudeRecord.getOrdinal(), TrailTypes.Q2.ordinal());
				Integer longitudePoint = histoVault.getMeasurementPoint(longitudeRecord.getOrdinal(), TrailTypes.Q2.ordinal());

				if (latitudePoint != null && longitudePoint != null) {
					gpsCluster.add(new GpsCoordinate(HistoSet.decodeVaultValue(latitudeRecord.channelItem, latitudePoint / 1000.),
							HistoSet.decodeVaultValue(longitudeRecord.channelItem, longitudePoint / 1000.)));
				} else {
					gpsCluster.add(null); // this keeps the sequence in parallel with the vaults sequence
				}
			}
			return gpsCluster;
		}

		@Override
		public String toString() {
			return "[initialVaults=" + this.initialVaults.size() + ", indexedVaults=" + this.indexedVaults.length + "]";
		}

	}

	private final Analyzer				analyzer;

	private final HistoExplorer		presentHistoExplorer	= DataExplorer.getInstance().getPresentHistoExplorer();
	private final List<Integer>		durations_mm					= new ArrayList<Integer>(INITIAL_RECORD_CAPACITY);

	private final TrailDataTags		dataTags;

	private PickedVaults					pickedVaults;

	/**
	 * Holds the view configuration.
	 */
	private HistoGraphicsTemplate	template;

	/**
	 * Hold trail records for measurements, settlements and scores.
	 * @param recordNames
	 * @param timeSteps
	 */
	private TrailRecordSet(Analyzer analyzer, String[] recordNames, TimeSteps timeSteps) {
		super(analyzer.getActiveDevice(), analyzer.getActiveDevice().getName() + GDE.STRING_UNDER_BAR + analyzer.getActiveChannel().getNumber(), //
				recordNames, timeSteps);
		this.analyzer = analyzer;
		this.dataTags = new TrailDataTags();
		this.template = HistoGraphicsTemplate.createGraphicsTemplate(analyzer);
		this.template.load();

		this.visibleAndDisplayableRecords = new Vector<TrailRecord>();
		this.displayRecords = new Vector<TrailRecord>();
		log.fine(() -> " TrailRecordSet(IDevice, int, RecordSet"); //$NON-NLS-1$
	}

	@FunctionalInterface
	public interface ChannelItemAction {
		void accept(TrailRecordSet trailRecordSet, Integer myIndex, IChannelItem channelItem);
	}

	/**
	 * Create a trail record set containing records according the channel configuration which is loaded from device properties file.
	 * The trail records' display sequence (= LinkedHashMap sequence) supports pinning score / settlement records at the top
	 * based on device xml settings.
	 * @return a trail record set containing all trail records (empty) as specified
	 */
	public static synchronized TrailRecordSet createRecordSet(Analyzer analyzer) {
		DeviceConfiguration configuration = analyzer.getActiveDevice().getDeviceConfiguration();

		TimeSteps timeSteps = new TimeSteps(-1, INITIAL_RECORD_CAPACITY);

		String[] names = configuration.getMeasurementSettlementScoregroupNames(analyzer.getActiveChannel().getNumber());
		TrailRecordSet newTrailRecordSet = new TrailRecordSet(analyzer, names, timeSteps);

		BiConsumer<Integer, IChannelItem> measurementAction = (idx, itm) -> {
			TrailRecord tmpRecord = new MeasurementTrail(idx, itm, newTrailRecordSet, INITIAL_RECORD_CAPACITY);
			newTrailRecordSet.put(itm.getName(), tmpRecord);
			tmpRecord.setColorDefaultsAndPosition(idx);
			log.fine(() -> "added measurement record for " + itm.getName() + " - " + idx);
		};
		BiConsumer<Integer, IChannelItem> settlementAction = (idx, itm) -> {
			TrailRecord tmpRecord = new SettlementTrail(idx, itm, newTrailRecordSet, INITIAL_RECORD_CAPACITY);
			newTrailRecordSet.put(itm.getName(), tmpRecord);
			tmpRecord.setColorDefaultsAndPosition(idx);
			log.fine(() -> "added settlement record for " + itm.getName() + " - " + idx);
		};
		BiConsumer<Integer, IChannelItem> scoreGroupAction = (idx, itm) -> {
			TrailRecord tmpRecord = new ScoregroupTrail(idx, itm, newTrailRecordSet, itm.getProperty().size());
			newTrailRecordSet.put(itm.getName(), tmpRecord);
			tmpRecord.setColorDefaultsAndPosition(idx);
			log.fine(() -> "added scoregroup record for " + itm.getName() + " - " + idx);
		};

		ChannelItems channelItems = new ChannelItems(analyzer);
		channelItems.processItems(measurementAction, settlementAction, scoreGroupAction);
		newTrailRecordSet.get(0).setRGB("0,0,0"); // top score group entry, set color to black

		return newTrailRecordSet;
	}

	/**
	 * Rebuild the record based on a new trail selection.
	 */
	public void refillRecord(TrailRecord record, int trailTextIndex) {
		record.setSelectedTrail(trailTextIndex);
		record.clear();
		record.initializeFromVaults(this.pickedVaults.initialVaults);
	}

	/**
	 * Refill data contents and keep the template data including trails.
	 * @param newPickedVaults
	 */
	public void refillFromVaults(TreeMap<Long, List<ExtendedVault>> newPickedVaults) {
		this.pickedVaults = new PickedVaults(newPickedVaults);

		cleanup();
		this.pickedVaults = new PickedVaults(newPickedVaults);
		RecordingsCollector collector = new RecordingsCollector();
		// re-define the valid trail types because the smart statistics setting might have changed
		collector.defineTrailTypes();
		collector.addVaultsToRecordSet();

		for (String recordName : recordNames) {
			TrailRecord trailRecord = get(recordName);
			trailRecord.clear();
			// Apply the data source information (= comboBox setting) from the graphics template definition to a record set.
			trailRecord.setSelectedTrail();
			trailRecord.initializeFromVaults(pickedVaults.initialVaults);
		}
		log.finer(() -> "refilled trailRecord size = " + get(0).size());
		collector.setGpsLocationsTags();
	}

	/**
	 * Build data contents after building the records list.
	 * @param newPickedVaults
	 */
	public void initializeFromVaults(TreeMap<Long, List<ExtendedVault>> newPickedVaults) {
		this.pickedVaults = new PickedVaults(newPickedVaults);

		cleanup();
		RecordingsCollector collector = new RecordingsCollector();
		collector.defineTrailTypes();
		collector.addVaultsToRecordSet();

		for (String recordName : recordNames) {
			TrailRecord trailRecord = get(recordName);
			trailRecord.clear();
			// Apply the data source information (= comboBox setting) from the graphics template definition to a record set.
			int trailTextOrdinal = template != null && template.isAvailable()
					? Integer.parseInt(template.getRecordProperty(recordName, Record.TRAIL_TEXT_ORDINAL, "-1")) : -1;
			trailRecord.setSelectedTrail(trailTextOrdinal);
			trailRecord.initializeFromVaults(pickedVaults.initialVaults);
		}
		log.finer(() -> "initial trailRecord size = " + get(0).size());
		collector.setGpsLocationsTags();
	}

	public void initializeTrailSelectors() {
		for (String recordName : recordNames) {
			TrailRecord trailRecord = get(recordName);
			trailRecord.setTrailSelector();
		}
	}

	/**
	 * @param recordOrdinal
	 * @return the record based on ordinal
	 */
	@Override
	public TrailRecord get(int recordOrdinal) {
		return (TrailRecord) super.get(recordOrdinal);
	}

	@Override
	public TrailRecord get(Object recordName) {
		return (TrailRecord) super.get(recordName);
	}

	@Override
	public TrailRecord put(String recordName, AbstractRecord record) {
		return (TrailRecord) super.put(recordName, record);
	}

	@Override
	@Deprecated // use getValueGridRecordName() or isValueGridRecord(..) instead
	public int getValueGridRecordOrdinal() {
		return Arrays.asList(recordNames).indexOf(getValueGridRecordName());
	}

	@Override
	@Deprecated // use setValueGridRecordName() instead
	public void setValueGridRecordOrdinal(int newValueGridRecordOrdinal) {
		setValueGridRecordName(recordNames[newValueGridRecordOrdinal]);
	}

	public boolean isValueGridRecord(TrailRecord record) {
		return this.valueGridRecordName.equals(record.getName());
	}

	public String getValueGridRecordName() {
		return this.valueGridRecordName;
	}

	/**
	 * @param newValueGridRecordName of the horizontal grid record name to set
	 */
	public void setValueGridRecordName(String newValueGridRecordName) {
		String tmpName = newValueGridRecordName;
		if (!this.keySet().contains(newValueGridRecordName)) tmpName = recordNames[0];
		this.valueGridRecordName = this.isOneOfSyncableRecord(tmpName) ? recordNames[this.getSyncMasterRecordOrdinal(tmpName)] : tmpName;
	}

	/**
	 * Synchronize scales according device properties.
	 * Support settlements.
	 */
	@Override
	public void syncScaleOfSyncableRecords() {
		this.scaleSyncedRecords.initSyncedScales(this);
	}

	/**
	 * Update referenced records to enable drawing of the curve, set min/max.
	 * Set the sync max/min values for visible records inclusive referenced suite records.
	 * Update the scale values from sync record if visible.
	 * @param graphicsComposite
	 */
	public synchronized void updateSyncGraphicsScale(GraphicsComposite graphicsComposite) {
		for (TrailRecord actualRecord : getVisibleAndDisplayableRecords()) {
			log.finer(() -> "set scale base value " + actualRecord.getName() + " isScaleSynced=" + actualRecord.isScaleSynced()); //$NON-NLS-1$
			graphicsComposite.getChartData(actualRecord).setSyncMaxMinValue();
		}

		for (Map.Entry<Integer, Vector<TrailRecord>> syncRecordsEntry : this.getScaleSyncedRecords().entrySet()) {
			boolean isAffected = false;
			int tmpMin = Integer.MAX_VALUE;
			int tmpMax = Integer.MIN_VALUE;
			for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
				if (syncRecord.isVisible() && syncRecord.isDisplayable()) {
					isAffected = true;
					tmpMin = Math.min(tmpMin, graphicsComposite.getChartData(syncRecord).getSyncMinValue());
					tmpMax = Math.max(tmpMax, graphicsComposite.getChartData(syncRecord).getSyncMaxValue());
					if (log.isLoggable(FINER)) log.log(FINER, syncRecord.getName() + " tmpMin  = " + tmpMin / 1000.0 + "; tmpMax  = " + tmpMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			// now we have the max/min values over all sync records of the current sync group
			for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
				graphicsComposite.getChartData(syncRecord).setSyncMinMax(tmpMin, tmpMax);
			}

			if (isAffected && log.isLoggable(FINER)) {
				log.log(FINER, this.get((int) syncRecordsEntry.getKey()).getSyncMasterName() + "; syncMin = " + tmpMin / 1000.0 + "; syncMax = " + tmpMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * Update the display records to enable drawing of the summary graphics.
	 * Needs not to check suite records because the summary max/min values comprise all suite members.
	 * @param summaryData holds all data required for painting with key recordName
	 */
	public synchronized void updateSyncSummaryScale(AbstractChartData summaryData) {
		int recencyLimit = analyzer.getSettings().getReminderCount();
		for (TrailRecord actualRecord : getDisplayRecords()) {
			SummaryLayout summary = (SummaryLayout) summaryData.get(actualRecord.getName());
			summary.clear();
			summary.setSyncMinMax(recencyLimit);
			log.finer(() -> actualRecord.getName() + "   summaryMin = " + summary.getSyncMin() + "  summaryMax=" + summary.getSyncMax());
		}

		// update the min/max values for synced records
		for (java.util.Map.Entry<Integer, Vector<TrailRecord>> syncRecordsEntry : this.getScaleSyncedRecords().entrySet()) {
			boolean isAffected = false;
			double tmpMin = Double.MAX_VALUE;
			double tmpMax = -Double.MAX_VALUE;
			for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
				SummaryLayout summary = (SummaryLayout) summaryData.get(syncRecord.getName());
				// exclude records with special trails from synchronizing
				if (syncRecord.getTrailSelector().isOddRangeTrail()) continue;

				if (summary.isSyncMinMaxDefined()) {
					isAffected = true;
					tmpMin = Math.min(tmpMin, summary.getSyncMin());
					tmpMax = Math.max(tmpMax, summary.getSyncMax());
					if (log.isLoggable(FINER)) log.log(FINER, syncRecord.getName() + " tmpMin  = " + tmpMin + "; tmpMax  = " + tmpMax);
				}
			}

			// now we have the max/min values over all sync records of the current sync group
			if (tmpMin == Double.MAX_VALUE || tmpMax == -Double.MAX_VALUE) {
				for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
					if (syncRecord.getTrailSelector().isOddRangeTrail()) continue;
					SummaryLayout summary = (SummaryLayout) summaryData.get(syncRecord.getName());
					summary.resetSyncMinMax();
				}
			} else {
				for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
					if (syncRecord.getTrailSelector().isOddRangeTrail()) continue;
					SummaryLayout summary = (SummaryLayout) summaryData.get(syncRecord.getName());
					summary.setSyncMinMax(tmpMin, tmpMax);
				}
			}
			if (isAffected && log.isLoggable(FINER)) {
				log.log(FINER, this.get((int) syncRecordsEntry.getKey()).getSyncMasterName() + "; syncMin = " + tmpMin + "; syncMax = " + tmpMax);
			}
		}
	}

	/**
	 * Update the displayable record information in this record set.
	 */
	public void setDisplayable() {
		for (TrailRecord record : this.getValues()) {
			record.setDisplayable();
		}
	}

	/**
	 * Update the collections of displayable records in this record set.
	 * The sort order conforms to the record insertion order.
	 */
	@Override
	public synchronized void updateVisibleAndDisplayableRecordsForTable() {
		this.visibleAndDisplayableRecords.removeAllElements();
		this.displayRecords.removeAllElements();

		// get by insertion order
		for (Map.Entry<String, AbstractRecord> entry : this.entrySet()) {
			final TrailRecord record = (TrailRecord) entry.getValue();
			if (record.isDisplayable()) {
				getDisplayRecords().add(record);
				if (record.isVisible()) // only selected records get displayed
					getVisibleAndDisplayableRecords().add(record);
			}
		}
	}

	/**
	 * @return visible and display able records (p.e. to build the partial data table)
	 */
	@SuppressWarnings("unchecked")
	public Vector<TrailRecord> getVisibleAndDisplayableRecordsForTable() {
		return (Vector<TrailRecord>) (analyzer.getSettings().isPartialDataTable() ? this.visibleAndDisplayableRecords : this.displayRecords);
	}

	/**
	 * @return visible and displayable records (p.e. to build the partial data table)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Vector<TrailRecord> getVisibleAndDisplayableRecords() {
		return (Vector<TrailRecord>) this.visibleAndDisplayableRecords;
	}

	/**
	 * @return all records for display
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Vector<TrailRecord> getDisplayRecords() {
		return (Vector<TrailRecord>) this.displayRecords;
	}

	/**
	 * Reverts adding vaults data to the recordset.
	 * Keeps initial capacities.
	 * Does not clear the records or any fields in the recordSet or in timeStep.
	 */
	public void cleanup() {
		super.timeStep_ms.clear();
		this.durations_mm.clear();
		this.dataTags.clear();
	}

	/**
	 * Method to get the sorted record names as array for display purpose.
	 * Sorted according display requirement, grid record first, syncMasterRecords second, all remaining.
	 * @return all measurement records and settlement / score records based on display settings
	 */
	public synchronized TrailRecord[] getRecordsSortedForDisplay() {
		Vector<TrailRecord> resultRecords = new Vector<>();
		// add the record with horizontal grid
		for (TrailRecord record : this.getDisplayRecords()) {
			if (isValueGridRecord(record)) resultRecords.add(record);
		}
		// add the scaleSyncMaster records to draw scale of this records first which sets the min/max display values
		for (TrailRecord record : this.getDisplayRecords()) {
			if (!isValueGridRecord(record)) {
				if (record.isScaleSyncMaster()) {
					resultRecords.add(record);
				} else if (record.isScaleSynced() && record.getSyncMasterRecordOrdinal() >= 0
						&& !resultRecords.stream().anyMatch(x -> x.getOrdinal() == record.getSyncMasterRecordOrdinal())
						&& record.getParent().isOneSyncableVisible(record.getSyncMasterRecordOrdinal())) {
					resultRecords.add(record.getParent().get(record.getSyncMasterRecordOrdinal()));
				}
			}
		}
		// add all others
		for (TrailRecord record : this.getDisplayRecords()) {
			if (!isValueGridRecord(record) && !record.isScaleSyncMaster()) resultRecords.add(record);
		}

		return resultRecords.toArray(new TrailRecord[resultRecords.size()]);
	}

	/**
	 * Save the histo graphics definition into a histo template file.
	 */
	public void saveTemplate() {
		for (TrailRecord record : this.getValues()) {
			record.saveTemplate();
		}
		// curve grid
		Color color = getValueGridColor();
		String rgb = color.getRGB().red + GDE.STRING_COMMA + color.getRGB().green + GDE.STRING_COMMA + color.getRGB().blue;
		template.setProperty(RecordSet.VALUE_GRID_COLOR, rgb);
		template.setProperty(RecordSet.VALUE_GRID_LINE_STYLE, "" + getValueGridLineStyle());
		template.setProperty(RecordSet.VALUE_GRID_TYPE, "" + getValueGridType());

		if (get(getValueGridRecordName()) != null) {
			template.setProperty(RecordSet.VALUE_GRID_RECORD_NAME, getValueGridRecordName());
		}

		template.setProperty(AbstractRecordSet.SMART_STATISTICS, "" + isSmartStatistics());
		int[] chartWeights = presentHistoExplorer.getHistoSummaryTabItem().getChartWeights();
		for (int i = 0; i < chartWeights.length; i++) {
			template.setProperty(AbstractRecordSet.CHART_WEIGHT + i, "" + chartWeights[i]);
		}
		template.setCommentSuffix(name + " " + description);
		template.store();
		log.fine(() -> "creating histo graphics template file in " + template.getTargetFileSubPath());
	}

	/**
	 * Apply the graphics template definition to a record set.
	 * @param doUpdateVisibilityStatus example: if the histo data do not hold data for this record it makes no sense to display the curve.
	 */
	public void applyTemplate(boolean doUpdateVisibilityStatus) {
		if (template != null && template.isAvailable()) {
			for (TrailRecord record : getValues()) {
				record.applyTemplate();
			}
			{
				String color = template.getProperty(AbstractRecordSet.VALUE_GRID_COLOR, "128,128,128");
				int r, g, b;
				r = Integer.parseInt(color.split(GDE.STRING_COMMA)[0].trim());
				g = Integer.parseInt(color.split(GDE.STRING_COMMA)[1].trim());
				b = Integer.parseInt(color.split(GDE.STRING_COMMA)[2].trim());
				setValueGridColor(SWTResourceManager.getColor(r, g, b));
				setValueGridLineStyle(Integer.parseInt(template.getProperty(AbstractRecordSet.VALUE_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)));
				setValueGridType(Integer.parseInt(template.getProperty(AbstractRecordSet.VALUE_GRID_TYPE, "0")));

				// default use first visible
				String gridDefaultRecordName = this.getValues().stream().filter(TrailRecord::isVisible).findFirst().orElse(get(0)).getName();
				String gridRecordName = template.getProperty(AbstractRecordSet.VALUE_GRID_RECORD_NAME, gridDefaultRecordName);
				TrailRecord gridRecord = get(gridRecordName);
				setValueGridRecordName(gridRecord != null && gridRecord.isVisible() ? gridRecordName : gridDefaultRecordName);
			}
			setSmartStatistics(Boolean.parseBoolean(template.getProperty(AbstractRecordSet.SMART_STATISTICS, "true")));
			if (GDE.isWithUi()) presentHistoExplorer.getHistoSummaryTabItem().setChartWeights(getChartWeights());
			log.fine(() -> "applied histo graphics template file " + template.getTargetFileSubPath());

			if (doUpdateVisibilityStatus) {
				setDisplayable();
				updateVisibleAndDisplayableRecordsForTable();
			}
		}
	}

	/**
	 * @return boolean true if the history analysis contains quantile values instead of legacy statistics
	 */
	public boolean isSmartStatistics() {
		return Boolean.parseBoolean(template.getProperty(AbstractRecordSet.SMART_STATISTICS, "true"));
	}

	/**
	 * Set true if the history analysis contains quantile values instead of legacy statistics
	 */
	public void setSmartStatistics(boolean isActive) {
		template.setProperty(AbstractRecordSet.SMART_STATISTICS, "" + isActive);
		if (GDE.isWithUi()) DataExplorer.getInstance().getPresentHistoExplorer().updateHistoMenuItems();
	}

	/**
	 * @return the template chart weights for graphs with multiple charts
	 */
	public int[] getChartWeights() {
		int[] chartWeights;
		if (isSmartStatistics()) { // only smart statistics supports multiple charts
			chartWeights = HistoSummaryWindow.DEFAULT_CHART_WEIGHTS.clone();
			for (int i = 0; i < chartWeights.length; i++) {
				chartWeights[i] = Integer.parseInt(template.getProperty(AbstractRecordSet.CHART_WEIGHT + i, "" + HistoSummaryWindow.DEFAULT_CHART_WEIGHTS[i]));
			}
		} else {
			chartWeights = HistoSummaryWindow.DEFAULT_CHART_WEIGHTS;
		}
		return chartWeights;
	}

	public HistoGraphicsTemplate getTemplate() {
		return this.template;
	}

	/**
	 * @return the number of timesteps (equals the size of the trailrecords /suiterecords)
	 */
	public int getTimeStepSize() {
		return this.timeStep_ms.size();
	}

	/**
	 * @param timestamp_ms
	 * @return the position of the timestep which is the closest to the timestamp
	 */
	public int getIndex(long timestamp_ms) {
		return this.timeStep_ms.getBestIndex(timestamp_ms, Comparator.reverseOrder());
	}

	/**
	 * @return individual durations for all trails
	 */
	public List<Integer> getDurations_mm() {
		return this.durations_mm;
	}

	public TrailDataTags getDataTags() {
		return this.dataTags;
	}

	public String getDataTagText(int index, DataTag dataTag) {
		return this.dataTags.getText(index, dataTag);
	}

	public long getTopTimeStamp_ms() {
		try {
			return this.timeStep_ms.firstElement() / 10;
		}
		catch (Exception e) {
			return 0;
		}
	}

	public long getLowestTimeStamp_ms() {
		try {
			return this.timeStep_ms.lastElement() / 10;
		}
		catch (Exception e) {
			return 0;
		}
	}

	public long getDisplayTimeStamp_ms(int index) {
		if (this.analyzer.getSettings().isXAxisReversed()) {
			return this.timeStep_ms.get(index) / 10;
		} else {
			return this.timeStep_ms.get(this.timeStep_ms.size() - 1 - index) / 10;
		}
	}

	/**
	 * @return the device
	 */
	@Override
	public IDevice getDevice() {
		return this.analyzer.getActiveDevice();
	}

	@Override
	public int getChannelConfigNumber() {
		return this.analyzer.getActiveChannel().getNumber();
	}

	/**
	 * @return the channel (or) configuration name
	 */
	public String getChannelConfigName() {
		return this.analyzer.getActiveChannel().getName();
	}

	@SuppressWarnings("unchecked")
	private SyncedRecords<TrailRecord> getScaleSyncedRecords() {
		return (SyncedRecords<TrailRecord>) this.scaleSyncedRecords;
	}

	/**
	 * @return the Vector containing the slave records sync by the master name
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Vector<TrailRecord> getScaleSyncedRecords(int syncMasterRecordOrdinal) {
		return (Vector<TrailRecord>) this.scaleSyncedRecords.get(syncMasterRecordOrdinal);
	}

	/**
	 * @return the vault at the timestep index position
	 */
	public ExtendedVault getVault(int index) {
		return (ExtendedVault) this.pickedVaults.indexedVaults[index];
	}

	@SuppressWarnings("unchecked")
	public Collection<TrailRecord> getValues() {
		return (Collection<TrailRecord>) (Collection<?>) values();
	}

	/**
	 * @return the vaults stream sorted by start timestamp for multiple use
	 */
	public HistoVault[] getIndexedVaults() {
		return pickedVaults.indexedVaults;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

}