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

package gde.histo.datasources;

import static gde.histo.datasources.VaultPicker.LoadProgress.CACHED;
import static gde.histo.datasources.VaultPicker.LoadProgress.DONE;
import static gde.histo.datasources.VaultPicker.LoadProgress.INITIALIZED;
import static gde.histo.datasources.VaultPicker.LoadProgress.MATCHED;
import static gde.histo.datasources.VaultPicker.LoadProgress.PATHS_VERIFIED;
import static gde.histo.datasources.VaultPicker.LoadProgress.RECORDED;
import static gde.histo.datasources.VaultPicker.LoadProgress.RESTORED;
import static gde.histo.datasources.VaultPicker.LoadProgress.SCANNED;
import static gde.histo.datasources.VaultPicker.LoadProgress.STARTED;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.sun.istack.Nullable;

import gde.Analyzer;
import gde.GDE;
import gde.GDE.UiNotification;
import gde.device.ScoreLabelTypes;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.exception.ThrowableUtils;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.VaultCollector;
import gde.histo.cache.VaultReaderWriter;
import gde.histo.datasources.HistoSet.RebuildStep;
import gde.histo.exclusions.ExclusionData;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Logger;

/**
 * Convert the source files into a trail recordSet required for the history tabs.
 * @author Thomas Eickert (USER)
 */
public final class VaultPicker {
	private static final String							$CLASS_NAME					= VaultPicker.class.getName();
	private static final Logger							log									= Logger.getLogger($CLASS_NAME);

	/**
	 * Estimated processing time ratio between reading from files and reading from cache
	 */
	private static final int								CACHE_BENEFIT				= 10;

	private static final DuplicateHandling	DUPLICATE_HANDLING	= DuplicateHandling.DISCARD;

	private enum DuplicateHandling {
		KEEP {
			@Override
			TrussJobs getTrussJobs(List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException {
				TrussJobs resultMap = new TrussJobs();
				for (VaultCollector truss : trusses) {
					resultMap.add(truss);
				}
				return resultMap;
			}
		},

		DISCARD {
			@Override
			TrussJobs getTrussJobs(List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException {
				TrussJobs resultMap = new TrussJobs();
				final Set<ExtendedVault> trusses4StartTimes = new HashSet<>();

				for (VaultCollector truss : trusses) {
					// add the truss to the set to find out if it is a duplicate according to the equals method criteria
					if (!trusses4StartTimes.add(truss.getVault())) {
						log.log(WARNING, "duplicate vault was discarded: ", truss);
						continue;
					}
					resultMap.add(truss);
				}
				return resultMap;
			}
		};

		/**
		 * Determine the vaults which are required for the data access.
		 * @return trussJobs with the actual path (not the link file path) and a map of vault skeletons (trusses)
		 */
		abstract TrussJobs getTrussJobs(List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException;

	}

	private final DirectoryScanner										directoryScanner;
	private final SourceDataSetExplorer								sourceDataSetExplorer;
	private final Analyzer														analyzer;

	/**
	 * Sorted by recordSet startTimeStamp in reverse order; each timestamp may hold multiple vaults.
	 */
	private final TreeMap<Long, List<ExtendedVault>>	pickedVaults			= new TreeMap<>(Collections.reverseOrder());

	/**
	 * Excluded vaults via ignore lists
	 */
	private List<ExtendedVault>												suppressedVaults	= new ArrayList<>();
	/**
	 * Size of all the histo files which have been read to build the histo recordsets
	 */
	private long																			recordSetBytesSum	= 0;
	/**
	 * Total time for rebuilding the HistoSet in micorseconds
	 */
	private int																				elapsedTime_us		= 0;

	/**
	 * Histo vault data transformed in a recordset format
	 */
	private TrailRecordSet														trailRecordSet		= null;

	public enum LoadProgress {
		STARTED(2), INITIALIZED(5), PATHS_VERIFIED(7), SCANNED(11), MATCHED(22), RESTORED(50), LOADED(80), CACHED(97), RECORDED(99), DONE(100);
		public int endPercentage;

		private LoadProgress(int endPercentage) {
			this.endPercentage = endPercentage;
		}
	}

	/**
	 * Holds trusses (vault skeletons) which are bound for reading into a full vault.
	 * Multimap implementation.
	 */
	public static final class TrussJobs {

		/**
		 * @return the jobs determined from the paths after duplicates elimination
		 */
		public static TrussJobs createTrussJobs(List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException {
			TrussJobs trussJobs;
			long nanoTime = System.nanoTime();
			// step: build the workload map consisting of the cache key and the file path
			trussJobs = DUPLICATE_HANDLING.getTrussJobs(trusses);
			int jobSize = trussJobs.values().parallelStream().mapToInt(List::size).sum();
			if (jobSize > 0) {
				long micros = NANOSECONDS.toMicros(System.nanoTime() - nanoTime);
				log.finer(() -> format("%,5d trusses    job check          time=%,6d [ms] :: per second:%5d", //
						jobSize, micros / 1000, trusses.size() * 1000000 / micros));
			}
			return trussJobs;
		}

		private Map<Path, List<VaultCollector>> trussJobs = new HashMap<>();

		public void clear() {
			trussJobs.clear();
		}

		public List<VaultCollector> get(Path path) {
			return trussJobs.get(path);
		}

		public synchronized boolean remove(VaultCollector value) {
			Path path = value.getVault().getLoadFileAsPath();
			List<VaultCollector> trussList = trussJobs.get(path);
			if (trussList == null) {
				return false;
			} else {
				boolean removed = trussList.remove(value);
				if (trussList.isEmpty()) trussJobs.remove(path);
				return removed;
			}
		}

		private List<VaultCollector> put(Path path, List<VaultCollector> trussList) {
			return trussJobs.put(path, trussList);
		}

		public synchronized void add(VaultCollector truss) {
			Path path = truss.getVault().getLoadFileAsPath();
			List<VaultCollector> list = get(path);
			if (list == null) put(path, list = new ArrayList<>());

			list.add(truss);
		}

		public Set<Entry<Path, List<VaultCollector>>> entrySet() {
			return trussJobs.entrySet();
		}

		public Collection<List<VaultCollector>> values() {
			return trussJobs.values();
		}

		public HashMap<String, VaultCollector> getAsVaultNameMap() {
			HashMap<String, VaultCollector> vaultNames = new HashMap<>();
			for (Entry<Path, List<VaultCollector>> jobEntry : trussJobs.entrySet()) {
				for (VaultCollector vaultCollector : jobEntry.getValue()) {
					VaultCollector oldVaultCollector = vaultNames.put(vaultCollector.getVault().getVaultName(), vaultCollector);
					if (oldVaultCollector != null) {
						log.log(Level.WARNING, "duplicate vaultName " + vaultCollector.getVault().getVaultName(), oldVaultCollector.getVault().getLoadFilePath());
					}
				}
			}
			return vaultNames;
		}

		public int size() {
			return trussJobs.size();
		}

		@Override
		public String toString() {
			final int maxLen = 10;
			return "TrussJobs [trussJobs=" + (this.trussJobs != null ? this.toString(this.trussJobs.entrySet(), maxLen) : null) + "]";
		}

		private String toString(Collection<?> collection, int maxLen) {
			StringBuilder builder = new StringBuilder();
			builder.append("[");
			int i = 0;
			for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
				if (i > 0) builder.append(", ");
				builder.append(iterator.next());
			}
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * Supports progress bars in loops and a simple progress bar setting.
	 */
	public static final class ProgressManager {

		private final UiNotification	presenter			= GDE.getUiNotification();

		private double								progressCycle	= 0.;
		private int										progressStart	= 0;
		private int										stepSize			= 0;
		private int										counter				= 0;

		public ProgressManager() {
		}

		public ProgressManager(int newEndPercentage, int newTotalCount, int newStepSize) {
			reInit(newEndPercentage, newTotalCount, newStepSize);
		}

		/**
		 * To be called prior to looping with doCount.
		 * @param loadProgress is the step to be accomplished
		 * @param newTotalCount
		 * @param newStepSize defines the number of counts which are required before the bar is updated again (performance)
		 */
		public void reInit(LoadProgress loadProgress, int newTotalCount, int newStepSize) {
			reInit(loadProgress.endPercentage, newTotalCount, newStepSize);
		}

		/**
		 * To be called prior to looping with doCount.
		 * @param newEndPercentage is reached after the total number of counts
		 * @param newTotalCount
		 * @param newStepSize defines the number of counts which are required before the bar is updated again (performance)
		 */
		public void reInit(int newEndPercentage, int newTotalCount, int newStepSize) {
			progressStart = presenter.getProgressPercentage();
			stepSize = newStepSize;
			counter = 0;
			if (newTotalCount == 0) {
				presenter.setProgress(newEndPercentage);
				progressCycle = 0.;
			} else {
				presenter.setProgress(progressStart);
				progressCycle = (newEndPercentage - progressStart) / (double) newTotalCount;
			}
		}

		/**
		 * Method option for calling in every loop.
		 * @param increment is the value added to the counter
		 */
		public void countInLoop(int increment) {
			int newCounter = counter + increment;
			if (progressCycle <= 0)
				throw new UnsupportedOperationException();
			else if (newCounter % stepSize == 0) {
				counter = newCounter;
				presenter.setProgress((int) (newCounter * stepSize * progressCycle + progressStart));
			} else
				; // progress is set if stepSize is reached
		}

		/**
		 * Set the progress bar percentage.
		 * @param percentage value (reset for 0 or bigger than 99)
		 */
		public void set(int percentage) {
			presenter.setProgress(percentage);
		}

		/**
		 * Set the progress bar percentage.
		 * @param loadProgress
		 */
		public void set(LoadProgress loadProgress) {
			presenter.setProgress(loadProgress.endPercentage);
		}
	}

	public VaultPicker() {
		this.analyzer = Analyzer.getInstance(); // ok
		this.directoryScanner = new DirectoryScanner(this.analyzer);
		this.sourceDataSetExplorer = new SourceDataSetExplorer(analyzer, this.directoryScanner.getSourceFolders(), false);
	}

	@Override
	public String toString() {
		String result = format("totalTrussesCount=%,d  availableVaultsCount=%,d recordSetBytesSum=%,d elapsedTime_ms=%,d", //
				getTrussesCount(), getTimeStepSize(), recordSetBytesSum, getElapsedTime_ms());
		return result;
	}

	/**
	 * Re- initializes the class.
	 */
	public void initialize() {
		// deep clear in order to reduce memory consumption prior to garbage collection
		for (List<ExtendedVault> timestampHistoVaults : pickedVaults.values()) {
			timestampHistoVaults.clear();
		}
		pickedVaults.clear();
		suppressedVaults.clear();

		recordSetBytesSum = 0;
		elapsedTime_us = 0;

		trailRecordSet = null;
		log.log(FINER, "done", this);
	}

	/**
	 * Determine the valid histo files and read the vaults from the log files or the cache.
	 * Use the vaults to populate the trail recordset.
	 * @param rebuildStep is the step type where the processing starts. The previous job types are omitted.
	 *          May be augmented if the device, channel or object was modified.
	 * @return true if the trail recordSet was rebuilt
	 */
	public synchronized boolean rebuild4Screening(RebuildStep rebuildStep) throws FileNotFoundException, IOException, NotSupportedFileFormatException,
			DataInconsitsentException, DataTypeException {
		RebuildStep realRebuildStep = rebuildStep; // the rebuild step might be augmented during the screening procedure

		Optional<ProgressManager> progress = GDE.isWithUi() ? Optional.of(new ProgressManager()) : Optional.empty();
		try {
			long startNanoTime = System.nanoTime();
			log.log(FINER, GDE.STRING_GREATER, rebuildStep);
			progress.ifPresent((p) -> p.set(STARTED));

			if (realRebuildStep.isEqualOrBiggerThan(RebuildStep.A_HISTOSET)) {
				directoryScanner.initialize();
				log.fine(() -> format("directoryScanner      initialize"));
			}
			progress.ifPresent((p) -> p.set(INITIALIZED));

			if (realRebuildStep.isEqualOrBiggerThan(RebuildStep.F_FILE_CHECK)) {
				realRebuildStep = directoryScanner.isValidated(rebuildStep);
				log.time(() -> format("  %3d file paths verified           time=%,6d [ms]", directoryScanner.getValidatedFoldersCount(), NANOSECONDS.toMillis(System.nanoTime() - startNanoTime + 500000)));
			}
			progress.ifPresent((p) -> p.set(PATHS_VERIFIED));

			if (realRebuildStep.isEqualOrBiggerThan(RebuildStep.B_HISTOVAULTS)) {
				initialize();
				{
					sourceDataSetExplorer.setStatusMessages(directoryScanner.isSlowFolderAccess());
					boolean reReadFiles = !directoryScanner.isChannelChangeOnly();
					sourceDataSetExplorer.screen4Trusses(directoryScanner.getSourceFolders().getMap(), reReadFiles);

					long millis = NANOSECONDS.toMillis(System.nanoTime() - startNanoTime);
					if (millis > 0)
						log.time(() -> format("%,5d trusses    select folders     time=%,6d [ms] :: per second:%5d", sourceDataSetExplorer.getTrusses().size(), millis, sourceDataSetExplorer.getTrusses().size() * 1000 / millis));
				}
				progress.ifPresent((p) -> p.set(SCANNED));

				if (!sourceDataSetExplorer.getTrusses().isEmpty()) {
					TrussJobs trussJobs;
					{ // step: read from paths and identify duplicates
						trussJobs = TrussJobs.createTrussJobs(sourceDataSetExplorer.getTrusses());
						progress.ifPresent((p) -> p.set(MATCHED));
					}
					{// step: put cached vaults into the histoSet map and reduce workload map
						int jobSize = trussJobs.values().parallelStream().mapToInt(List::size).sum();
						progress.ifPresent((p) -> p.reInit(RESTORED, jobSize, CACHE_BENEFIT));
						loadVaultsFromCache(trussJobs, progress);
						int totalEstimatedEffort = pickedVaults.size() + CACHE_BENEFIT * (jobSize - pickedVaults.size());
						double timeQuotaDone = pickedVaults.size() / totalEstimatedEffort;
						int progressPercentageDone = (int) ((CACHED.endPercentage - MATCHED.endPercentage) * timeQuotaDone);
						progress.ifPresent((
								p) -> p.set(Math.max(GDE.getUiNotification().getProgressPercentage(), MATCHED.endPercentage + progressPercentageDone)));
					}
					final long recordSetBytesCachedSum = recordSetBytesSum;
					{// step: transform log files from workload map into vaults and put them into the histoSet map
						long nanoTime = System.nanoTime();
						loadVaultsFromFiles(trussJobs, progress);
						long loadCount = trussJobs.values().parallelStream().mapToInt(Collection::size).sum();
						if (loadCount > 0) {
							long micros = NANOSECONDS.toMicros(System.nanoTime() - nanoTime);
							log.time(() -> format("%,5d recordsets create from files  time=%,6d [ms] :: per second:%5d :: Rate=%,6d MiB/s", loadCount, micros / 1000, loadCount * 1000000 / micros, (int) ((this.recordSetBytesSum - recordSetBytesCachedSum) / 1.024 / 1.024 / micros)));
						}
					}
					{// step: save vaults in the file system
						long nanoTime = System.nanoTime(), cacheSize_B = 0;
						long loadCount = trussJobs.values().parallelStream().mapToInt(Collection::size).sum();
						if (loadCount > 0) {
							cacheSize_B = analyzer.getDataAccess().getCacheSize();
							new VaultReaderWriter(analyzer, Optional.empty()).storeInCaches(trussJobs);
							if (analyzer.getDataAccess().getCacheSize() - cacheSize_B > 0) {
								long micros = NANOSECONDS.toMicros(System.nanoTime() - nanoTime);
								log.time(() -> format("%,5d recordsets store in cache     time=%,6d [ms] :: per second:%5d :: Rate=%,6d MiB/s", loadCount, micros / 1000, loadCount * 1000000 / micros, (int) ((this.recordSetBytesSum - recordSetBytesCachedSum) / 1.024 / 1.024 / micros)));
							}
						}
					}
				}
			}
			progress.ifPresent((p) -> p.set(CACHED));

			if (analyzer.getSettings().isSuppressMode()) suppressedVaults.addAll(removeSuppressedHistoVaults());

			if (realRebuildStep.isEqualOrBiggerThan(RebuildStep.C_TRAILRECORDSET)) {
				long nanoTime = System.nanoTime();
				trailRecordSet = TrailRecordSet.createRecordSet(analyzer);
				trailRecordSet.initializeFromVaults(pickedVaults);
				trailRecordSet.applyTemplate(true); // needs reasonable data
				if (recordSetBytesSum > 0) {
					long micros = NANOSECONDS.toMicros(System.nanoTime() - nanoTime);
					log.time(() -> format("%,5d timeSteps new TrailRecordSet  time=%,6d [ms] :: per second:%5d", //
							pickedVaults.size(), micros / 1000, pickedVaults.size() > 0 ? pickedVaults.size() * 1000000 / micros : 0));
				}
			} else if (realRebuildStep.isEqualOrBiggerThan(RebuildStep.D_TRAIL_DATA)) { // keeps the template in contrast to the logic above
				long nanoTime = System.nanoTime();
				trailRecordSet.initializeTrailSelectors();
				trailRecordSet.refillFromVaults(pickedVaults);
				long micros = NANOSECONDS.toMicros(System.nanoTime() - nanoTime);
				log.time(() -> format("%,5d timeSteps  to TrailRecordSet  time=%,6d [ms] :: per second:%5d", //
						pickedVaults.size(), micros / 1000, pickedVaults.size() > 0 ? pickedVaults.size() * 1000000 / micros : 0));
			}
			progress.ifPresent((p) -> p.set(RECORDED));

			this.elapsedTime_us = (int) ((System.nanoTime() - startNanoTime + 500000) / 1000);
			log.time(() -> format("%,5d timeSteps  total              time=%,6d [ms] :: per second:%5d :: Rate=%,6d MiB/s", this.pickedVaults.size(), elapsedTime_us / 1000, this.pickedVaults.size() * 1000000 / this.elapsedTime_us, (int) (this.recordSetBytesSum / 1.024 / 1.024 / this.elapsedTime_us)));
		} catch (IOException | UncheckedIOException e) {
			ThrowableUtils.rethrow(e);
		} catch (Exception e) {
			log.log(SEVERE, e.getMessage(), e);
		} finally {
			progress.ifPresent((p) -> p.set(DONE));
		}
		return realRebuildStep.isEqualOrBiggerThan(RebuildStep.D_TRAIL_DATA);
	}

	/**
	 * @param trussJobs is the list of trusses which must be read
	 */
	private void loadVaultsFromFiles(TrussJobs trussJobs, Optional<ProgressManager> progress) {
		int remainingJobSize = trussJobs.values().parallelStream().mapToInt(List::size).sum();
		if (remainingJobSize <= 0) return;

		progress.ifPresent((p) -> p.reInit(CACHED, remainingJobSize, 1));
		VaultReaderWriter vaultReaderWriter = new VaultReaderWriter(analyzer, Optional.empty());
		trussJobs.entrySet().parallelStream().forEach(trussJobsEntry -> {
			vaultReaderWriter.loadFromFile(trussJobsEntry.getKey(), trussJobsEntry.getValue());
			for (VaultCollector vaultCollector : trussJobsEntry.getValue()) {
				ExtendedVault histoVault = vaultCollector.getVault();
				if (!histoVault.isTruss()) {
					putVault(histoVault);
					recordSetBytesSum += histoVault.getScorePoint(ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal());
				} else {
					log.info(() -> format("vault has no log data %,7d kiB %s", histoVault.getLogFileLength() / 1024, histoVault.getLoadFilePath()));
				}
			}
			progress.ifPresent((p) -> p.countInLoop(trussJobsEntry.getValue().size()));
		});
	}

	/**
	 * @param trussJobs is the job list which is worked on and reduced for each vault found in the cache
	 */
	private void loadVaultsFromCache(TrussJobs trussJobs, Optional<ProgressManager> progress) throws IOException {
		long nanoTime = System.nanoTime();
		long hitCount = VaultReaderWriter.getInnerCacheHitCount();
		int tmpHistoSetsSize = pickedVaults.size();
		VaultReaderWriter vaultReaderWriter = new VaultReaderWriter(analyzer, progress);
		for (ExtendedVault histoVault : vaultReaderWriter.loadFromCaches(trussJobs)) {
			if (!histoVault.isTruss()) {
				putVault(histoVault);
				recordSetBytesSum += histoVault.getScorePoint(ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal());
			} else {
				log.info(() -> format("vault has no log data %,7d kiB %s", histoVault.getLogFileLength() / 1024, histoVault.getLoadFilePath()));
			}
		}
		int loadCount = pickedVaults.size() - tmpHistoSetsSize;
		if (loadCount > 0) {
			long micros = NANOSECONDS.toMicros(System.nanoTime() - nanoTime);
			log.time(() -> format("%,5d/%,5d      vaults cached/hit  time=%,6d [ms] :: per second:%5d :: Rate=%,6d MiB/s", loadCount, VaultReaderWriter.getInnerCacheHitCount() - hitCount, micros / 1000, loadCount * 1000000 / micros, (int) (this.recordSetBytesSum / 1.024 / 1.024 / micros)));
		}
	}

	/**
	 * Check the histo vaults list for suppressed vaults in order to cope with additional suppressions by the user.
	 * Use ignore lists (for recordSets only) to determine the vaults which are required for the data access.
	 * @return the suppressed vaults which have been detected in the vaults list
	 */
	private List<ExtendedVault> removeSuppressedHistoVaults() {
		long totalSize = pickedVaults.values().parallelStream().mapToInt(Collection::size).sum();
		ExclusionData exclusionData = new ExclusionData(new DirectoryScanner(analyzer).getActiveFolder(), analyzer.getDataAccess());
		List<ExtendedVault> removed = pickedVaults.values().parallelStream().flatMap(Collection::stream) //
				.filter(v -> exclusionData.isExcluded(v.getLoadFileName(), v.getLogRecordsetBaseName())) //
				.collect(Collectors.toList());
		for (ExtendedVault vault : removed) {
			log.info(() -> format("discarded as per exclusion list   %s %s   %s", //
					vault.getLoadFilePath(), vault.getLogRecordsetBaseName(), vault.getStartTimeStampFormatted()));
			List<ExtendedVault> vaultList = pickedVaults.get(vault.getLogStartTimestamp_ms());
			vaultList.remove(vault);
			if (vaultList.isEmpty()) pickedVaults.remove(vault.getLogStartTimestamp_ms());
		}
		log.fine(() -> format("%04d total trusses --- %04d excluded trusses", totalSize, removed.size()));
		return removed;
	}

	/**
	 * @param histoVault
	 */
	private void putVault(ExtendedVault histoVault) {
		List<ExtendedVault> timeStampHistoVaults = pickedVaults.get(histoVault.getLogStartTimestamp_ms());
		if (timeStampHistoVaults == null) {
			pickedVaults.put(histoVault.getLogStartTimestamp_ms(), timeStampHistoVaults = new ArrayList<ExtendedVault>());
			timeStampHistoVaults.add(histoVault);
			log.finer(() -> format("added   startTimeStamp=%s  %s  logRecordSetOrdinal=%d  logChannelNumber=%d  %s", histoVault.getStartTimeStampFormatted(), histoVault.getVaultFileName(), histoVault.getLogRecordSetOrdinal(), histoVault.getLogChannelNumber(), histoVault.getLoadFilePath()));
		}
	}

	/**
	 * @return the total number of trusses identified in the files
	 */
	public int getTrussesCount() {
		return sourceDataSetExplorer.getTrusses().size();
	}

	/**
	 * @return the number of timestamp entries
	 */
	public int getTimeStepSize() {
		return getTrailRecordSet() != null ? getTrailRecordSet().getTimeStepSize() : 0;
	}

	public int getElapsedTime_ms() {
		return this.elapsedTime_us / 1000;
	}

	@Nullable // i.e. if rebuild thread is not finished
	public TrailRecordSet getTrailRecordSet() {
		return this.trailRecordSet;
	}

	public long getRecordSetBytesSum() {
		return this.recordSetBytesSum;
	}

	public List<ExtendedVault> getSuppressedTrusses() {
		return this.suppressedVaults;
	}

	public List<Path> getExcludedFiles() {
		return this.sourceDataSetExplorer.getExcludedFiles();
	}

	public SourceFolders getSourceFolders() {
		return this.directoryScanner.getSourceFolders();
	}

	public int getDirectoryFilesCount() {
		return sourceDataSetExplorer.getSourceFiles().size() + sourceDataSetExplorer.getNonWorkableCount() + sourceDataSetExplorer.getExcludedFiles().size();
	}

	public int getReadFilesCount() {
		return this.sourceDataSetExplorer.getSourceFiles().size();
	}

	/**
	 * @return the number of files which have log data for the object, device, analysis timespan etc.
	 */
	public int getMatchingFilesCount() {
		return (int) sourceDataSetExplorer.getTrusses().parallelStream() //
				.map(VaultCollector::getVault).map(ExtendedVault::getLoadFileAsPath).count();
	}

}
