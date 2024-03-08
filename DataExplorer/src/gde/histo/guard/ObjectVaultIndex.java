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

    Copyright (c) 2018,2019 Thomas Eickert
****************************************************************************************/

package gde.histo.guard;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import gde.Analyzer;
import gde.DataAccess;
import gde.GDE;
import gde.config.Settings;
import gde.device.IDevice;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.HistoVault;
import gde.histo.cache.VaultReaderWriter;
import gde.histo.device.IHistoDevice;
import gde.histo.exclusions.ExclusionData;
import gde.log.Level;
import gde.log.Logger;

/**
 * Supports an index based access to the vaults.
 * Rebuilding the index only takes vaults for the current property settings.
 * @author Thomas Eickert (USER)
 */
public final class ObjectVaultIndex {
	private static final String	$CLASS_NAME	= ObjectVaultIndex.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * File name for the 'deviceoriented' indices.
	 */
	private static final String	INDEX_FILENAME_DEVICEORIENTED = GDE.STRING_UNDER_BAR;

	/**
	 * Support custom filters for vault index attributes.
	 * Provides selection result caching.
	 * Use the equals method to decide if objects represent the same query.
	 */
	static class DetailSelector implements Function<ObjectVaultIndexEntry, Boolean> {

		private final String																		filterText;
		private final Function<ObjectVaultIndexEntry, Boolean>	filterFunction;

		@Override
		public Boolean apply(ObjectVaultIndexEntry t) {
			return filterFunction.apply(t);
		}

		static DetailSelector createDummyFilter() {
			return new DetailSelector(GDE.STRING_EMPTY, t -> true);
		}

		static DetailSelector createDeviceNameFilter(Collection<String> deviceNames) {
			ArrayList<String> names = new ArrayList<String>(deviceNames);
			Collections.sort(names);
			return new DetailSelector("deviceName:" + names.toString(), t -> names.contains(t.vaultDeviceName));
		}

		static DetailSelector createFunctionFilter(Function<ObjectVaultIndexEntry, Boolean> function) {
			return new DetailSelector("function:" + function.toString(), function);
		}

		/**
		 * Use the create methods instead.
		 */
		private DetailSelector(String filterText, Function<ObjectVaultIndexEntry, Boolean> filterFunction) {
			this.filterText = filterText;
			this.filterFunction = filterFunction;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.filterText == null) ? 0 : this.filterText.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			DetailSelector other = (DetailSelector) obj;
			if (this.filterText == null) {
				if (other.filterText != null) return false;
			} else if (!this.filterText.equals(other.filterText)) return false;
			return true;
		}

	}

	/**
	 * The keys for accessing vaults are the directory name and the file name.
	 */
	public static class VaultKeyPair extends SimpleImmutableEntry<String, String> {
		private static final long serialVersionUID = 8937538273315451095L;

		/**
		 * @param directoryName is a valid cache folder entry (directory or zip file)
		 * @param vaultName is the SHA1 name of the vault
		 */
		public VaultKeyPair(String directoryName, String vaultName) {
			super(directoryName, vaultName);
		}
	}

	/**
	 * Index directory caching.
	 */
	static class ObjectVaultMap {
		@SuppressWarnings("hiding")
		private static final Logger							log	= Logger.getLogger(ObjectVaultMap.class.getName());

		private final DataAccess								dataAccess;
		private final Settings									settings;
		/**
		 * Key is the objectKey, the list holds the vault index attributes.
		 * todo simplify to List<ObjectVaultIndexEntry> in case a high speed object key selection is not required
		 */
		private final Map<String, List<String>>	indexMap;

		/**
		 * Use this for retrieving the full index from the vault index entries.
		 */
		public ObjectVaultMap(DataAccess dataAccess, Settings settings) {
			this.dataAccess = dataAccess;
			this.settings = settings;
			this.indexMap = loadIndex(s -> true);
		}

		/**
		 * Use this for retrieving a partial index to reduce vault index access.
		 * @param objectKeys specifies the user's object key selection
		 */
		public ObjectVaultMap(String[] objectKeys, DataAccess dataAccess, Settings settings) {
			this.dataAccess = dataAccess;
			this.settings = settings;
			List<String> keys = Arrays.asList(objectKeys);
			this.indexMap = loadIndex(s -> keys.contains(s));
		}

		/**
		 * Recreate the fleet index directory.
		 */
		public static void storeIndex(Map<String, List<String>> newIndexMap, DataAccess dataAccess) {
			newIndexMap.forEach((objectKey, list) -> {
				try (OutputStream outputStream = dataAccess.getFleetOutputStream(Paths.get(objectKey)); //
						Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));) {
					for (String str : list) {
						writer.write(str + GDE.STRING_NEW_LINE);
					}
				} catch (IOException e) {
					log.log(Level.SEVERE, "write", e);
				}
			});
			log.off(() -> "objects: " + newIndexMap.keySet().toString());
			log.off(() -> "objects count: " + newIndexMap.size() + "  total vaults: " + newIndexMap.values().stream().mapToLong(l -> l.size()).sum());
		}

		/**
		 * @param objectSelector specifies the user's object key selection
		 * @return the object key index portion from the vault index entries
		 */
		private Map<String, List<String>> loadIndex(Function<String, Boolean> objectSelector) {
			Map<String, List<String>> objectIndexes = new HashMap<>();
			for (String fileName : dataAccess.getFleetFileNames(objectSelector)) {
				List<String> indexList = new ArrayList<>();
				try (InputStream inputStream = dataAccess.getFleetInputStream(Paths.get(fileName)); //
						BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));) {
					String line;
					while ((line = br.readLine()) != null) {
						if (!line.isEmpty()) {
							indexList.add(line);
						}
					}
				} catch (Exception e) {
					log.log(Level.SEVERE, "read", e);
				}
				String objectKey = fileName.equals(INDEX_FILENAME_DEVICEORIENTED) ? GDE.STRING_EMPTY : fileName;
				objectIndexes.put(objectKey, indexList);
			}
			log.off(() -> "objects: " + objectIndexes.keySet().toString());
			log.off(() -> "objects count: " + objectIndexes.size() + "  total vaults: " + objectIndexes.values().stream().mapToLong(l -> l.size()).sum());
			return objectIndexes;
		}

		public Set<String> getObjectKeys() {
			Set<String> objectStrings = indexMap.keySet();
			if (objectStrings.remove(INDEX_FILENAME_DEVICEORIENTED)) objectStrings.add("");
			return objectStrings;
		}

		/**
		 * @param objectKeys are the primary selection parameters
		 * @param detailSelector specifies the selected vault index entries
		 * @return the vault indices
		 */
		public Set<ObjectVaultIndexEntry> getVaultIndexes(String[] objectKeys, DetailSelector detailSelector) {
			if (objectKeys.length == 1) {
				return getVaultIndexes(objectKeys[0], detailSelector);
			} else {
				return getVaultIndexes(s -> Arrays.asList(objectKeys).contains(s), detailSelector);
			}
		}

		/**
		 * @param objectKey is the primary selection parameter
		 * @param detailSelector specifies the selected vault index entries
		 */
		public Set<ObjectVaultIndexEntry> getVaultIndexes(String objectKey, DetailSelector detailSelector) {
			Set<ObjectVaultIndexEntry> indices = new HashSet<>();
			Path activeFolder = Paths.get(settings.getDataFilePath()).resolve(objectKey);
			ExclusionData exclusionData = new ExclusionData(activeFolder, dataAccess);
			boolean isSuppressMode = settings.isSuppressMode();
			for (Entry<String, List<String>> e : indexMap.entrySet()) {
				if (!objectKey.equals(e.getKey())) continue;
				for (String indexCsv : e.getValue()) {
					ObjectVaultIndexEntry idx = new ObjectVaultIndexEntry(e.getKey(), indexCsv);
					if (!detailSelector.apply(idx)) continue;
					if (!isSuppressMode || !exclusionData.isExcluded(idx.getLogFileName())) indices.add(idx);
				}
			}
			return indices;
		}

		/**
		 * @param objectSelector specifies the object key selection
		 * @param detailSelector specifies the selected vault index entries
		 * @return the vault indices
		 */
		public Set<ObjectVaultIndexEntry> getVaultIndexes(Function<String, Boolean> objectSelector, DetailSelector detailSelector) {
			Path dataFilePath = Paths.get(settings.getDataFilePath());
			Function<ObjectVaultIndexEntry, Boolean> isExcludedSelector = idx -> {
				Path activeFolder = dataFilePath.resolve(idx.vaultObjectKey);
				ExclusionData exclusionData = new ExclusionData(activeFolder, dataAccess);
				return exclusionData.isExcluded(idx.getLogFileName());
			};

			Set<ObjectVaultIndexEntry> indices = new HashSet<>();
			boolean isSuppressMode = settings.isSuppressMode();
			for (Entry<String, List<String>> e : indexMap.entrySet()) {
				if (!objectSelector.apply(e.getKey())) continue;
				for (String indexCsv : e.getValue()) {
					ObjectVaultIndexEntry idx = new ObjectVaultIndexEntry(e.getKey(), indexCsv);
					if (!detailSelector.apply(idx)) continue;
					if (!isSuppressMode || !isExcludedSelector.apply(idx)) indices.add(idx);
				}
			}
			return indices;
		}

		public boolean isEmpty() {
			return indexMap.isEmpty();
		}
	}

	/**
	 * Represents an index item.
	 */
	static class ObjectVaultIndexEntry {
		@SuppressWarnings("hiding")
		private static final Logger	log	= Logger.getLogger(ObjectVaultIndexEntry.class.getName());

		String											vaultObjectKey;

		String											vaultName;
		String											vaultDirectory;
		String											vaultReaderSettingsCsv;
		long												vaultCreatedMs;
		String											vaultDataExplorerVersion;
		String											vaultDeviceKey;
		String											vaultDeviceName;
		int													vaultChannelNumber;
		long												vaultSamplingTimespanMs;

		long												logFileLastModified;
		long												logFileLength;
		int													logRecordSetOrdinal;
		String											logRecordsetBaseName;
		int													logChannelNumber;
		long												logStartTimestampMs;
		String											logFilePath;

		static Function<HistoVault, ObjectVaultIndexEntry> objectKeyExtractor() {
			return v -> {
				ObjectVaultIndexEntry idx = new ObjectVaultIndexEntry();
				idx.vaultObjectKey = v.getVaultObjectKey();

				idx.vaultName = v.getVaultName();
				idx.vaultDirectory = v.getVaultDirectory();
				idx.vaultReaderSettingsCsv = v.getVaultReaderSettings();
				idx.vaultCreatedMs = v.getVaultCreated_ms();
				idx.vaultDataExplorerVersion = v.getVaultDataExplorerVersion();
				idx.vaultDeviceKey = v.getVaultDeviceKey();
				idx.vaultDeviceName = v.getVaultDeviceName();
				idx.vaultChannelNumber = v.getVaultChannelNumber();
				idx.vaultSamplingTimespanMs = v.getVaultSamplingTimespan_ms();

				idx.logFileLastModified = v.getLogFileLastModified();
				idx.logFileLength = v.getLogFileLength();
				idx.logRecordSetOrdinal = v.getLogRecordSetOrdinal();
				idx.logRecordsetBaseName = v.getLogRecordsetBaseName();
				idx.logChannelNumber = v.getLogChannelNumber();
				idx.logStartTimestampMs = v.getLogStartTimestamp_ms();
				idx.logFilePath = v.getLogFilePath();
				return idx;
			};
		}

		private ObjectVaultIndexEntry() {
		}

		/**
		 * @param fieldsCsv holds the objectKey enclosed in quotes plus additional attributes
		 */
		public ObjectVaultIndexEntry(String fieldsCsv) {
			this(fieldsCsv.split(GDE.STRING_CSV_SEPARATOR, 2));
		}

		private ObjectVaultIndexEntry(String[] splitFields) {
			this(splitFields[0].replace(GDE.STRING_CSV_QUOTE, ""), splitFields[1]);
		}

		/**
		 * @param attributesCsv holds the additional attributes (without the objectKey)
		 */
		public ObjectVaultIndexEntry(String objectKey, String attributesCsv) {
			this.vaultObjectKey = objectKey;

			log.log(Level.FINEST, attributesCsv);
			String[] values = attributesCsv.split(GDE.STRING_CSV_SEPARATOR);
			this.vaultName = values[0];
			this.vaultDirectory = values[1];
			this.vaultReaderSettingsCsv = values[2].replace(GDE.CHAR_UNDER_BAR, GDE.CHAR_CSV_SEPARATOR);
			this.vaultCreatedMs = Long.parseLong(values[3]);
			this.vaultDataExplorerVersion = values[4];
			this.vaultDeviceKey = values[5];
			this.vaultDeviceName = values[6];
			this.vaultChannelNumber = Integer.parseInt(values[7]);
			this.vaultSamplingTimespanMs = Long.parseLong(values[8]);

			this.logFileLastModified = Long.parseLong(values[9]);
			this.logFileLength = Long.parseLong(values[10]);
			this.logRecordSetOrdinal = Integer.parseInt(values[11]);
			this.logRecordsetBaseName = values[12];
			this.logChannelNumber = Integer.parseInt(values[13]);
			this.logStartTimestampMs = Long.parseLong(values[14]);
			this.logFilePath = values[15];
		}

		@Override
		public String toString() {
			String d = GDE.STRING_CSV_SEPARATOR;
			String readerSettings = this.vaultReaderSettingsCsv.replace(GDE.CHAR_CSV_SEPARATOR, GDE.CHAR_UNDER_BAR);
			return GDE.STRING_CSV_QUOTE + this.vaultObjectKey + GDE.STRING_CSV_QUOTE + d + this.vaultName + d + this.vaultDirectory + d + readerSettings + d + this.vaultCreatedMs //
					+ d + this.vaultDataExplorerVersion + d + this.vaultDeviceKey + d + this.vaultDeviceName + d + this.vaultChannelNumber + d + this.vaultSamplingTimespanMs //
					+ d + this.logFileLastModified + d + this.logFileLength + d + this.logRecordSetOrdinal + d + this.logRecordsetBaseName //
					+ d + this.logChannelNumber + d + this.logStartTimestampMs + d + this.logFilePath;
		}

		public String getLogFileName() {
			return Paths.get(this.logFilePath).getFileName().toString();
		}
	}

	/**
	 * Read the vaults for all devices based on the settings.
	 * Build and store the fleet index files.
	 */
	public static void rebuild(Analyzer analyzer) {
		List<String> cacheDirectoryNames = defineCacheDirectoryNames(analyzer);
		Map<String, List<String>> objectKeyIndexMap = readIndex(cacheDirectoryNames, analyzer);

		analyzer.getDataAccess().deleteFleetObjects();
		ObjectVaultMap.storeIndex(objectKeyIndexMap, analyzer.getDataAccess());
		log.off(() -> "Fleet directory      size=" + analyzer.getDataAccess().getFleetFileNames(s -> true).size());
	}

	/**
	 * @return the current vault directory names based on settings etc.
	 */
	private static List<String> defineCacheDirectoryNames(String deviceName, Analyzer analyzer) {
		List<String> cacheDirectoryNames = new ArrayList<>();
		analyzer.setActiveDevice(deviceName);
		IDevice device = analyzer.getActiveDevice();
		List<String> validReaderSettings = new ArrayList<>();
		validReaderSettings.add(GDE.STRING_EMPTY);
		if (device instanceof IHistoDevice) validReaderSettings.add(((IHistoDevice) device).getReaderSettingsCsv());

		for (String vaultReaderSettings : validReaderSettings) {
			for (int j = 1; j <= device.getChannelCount(); j++) {
				String directoryName = ExtendedVault.getVaultDirectoryName(analyzer.getActiveDevice(), analyzer.getSettings(), j, vaultReaderSettings);
				cacheDirectoryNames.add(directoryName);
			}
		}
		log.off(() -> "Cache directory      size=" + cacheDirectoryNames.size() + " for device " + deviceName);
		return cacheDirectoryNames;
	}

	/**
	 * @return the current vault directory names based on settings for all devices.
	 */
	private static List<String> defineCacheDirectoryNames(Analyzer analyzer) {
		List<String> cacheDirectoryNames = new ArrayList<>();
		Collection<String> deviceNames = analyzer.getDeviceConfigurations().getAllConfigurations().keySet();
		for (String deviceName : deviceNames) {
			cacheDirectoryNames.addAll(defineCacheDirectoryNames(deviceName, analyzer));
		}
		log.off(() -> "cache directories: " + cacheDirectoryNames.size() + "  from deviceNames: " + deviceNames.size());
		return cacheDirectoryNames;
	}

	/**
	 * @param cacheDirectoryNames are the vault directories to be indexed (is a subset due to obsolete directories)
	 * @return the lists of vault index entries for all objects (key is objectKey)
	 */
	private static Map<String, List<String>> readIndex(List<String> cacheDirectoryNames, Analyzer analyzer) {
		Map<String, List<String>> objectKeyMap = new HashMap<>();
		try {
			for (String directoryName : cacheDirectoryNames) {
				if (analyzer.getDataAccess().existsCacheDirectory(directoryName)) {
					List<Entry<String, String>> vaultIndices = new VaultReaderWriter(analyzer, Optional.empty()).readVaultsIndices(directoryName);
					for (Entry<String, String> e : vaultIndices) {
						String objectKey = e.getKey().isEmpty() ? INDEX_FILENAME_DEVICEORIENTED : e.getKey();
						if (objectKeyMap.get(objectKey) == null) {
							objectKeyMap.put(objectKey, new ArrayList<>());
						}
						objectKeyMap.get(objectKey).add(e.getValue());
					}
				}
			}
		} catch (IOException e) {
			log.log(Level.SEVERE, "read", e);
			return objectKeyMap;
		}
		log.off(() -> "objectKeys       selected=" + objectKeyMap.keySet().size() + "  " + new TreeSet<String>(objectKeyMap.keySet()).toString());
		log.off(() -> "vaults           selected=" + objectKeyMap.values().parallelStream().mapToInt(l -> l.size()).sum());
		return objectKeyMap;
	}

	private final ObjectVaultMap	objectVaultMap;

	/**
	 * Use this for full index access.
	 */
	public ObjectVaultIndex(DataAccess dataAccess, Settings settings) {
		this.objectVaultMap = new ObjectVaultMap(dataAccess, settings);
		log.log(Level.INFO, "vault key index loaded");
	}

	/**
	 * @param objectKeys for selecting a vault index subset which is used for all methods
	 */
	public ObjectVaultIndex(String[] objectKeys, DataAccess dataAccess, Settings settings) {
		this.objectVaultMap = new ObjectVaultMap(objectKeys, dataAccess, settings);
		log.log(Level.INFO, "vault key index loaded for", objectKeys);
	}

	/**
	 * @return the all device names from the index without device validation
	 */
	public Set<String> selectDeviceNames() {
		Set<ObjectVaultIndexEntry> vaultIndexes = objectVaultMap.getVaultIndexes(s -> true, DetailSelector.createDummyFilter());
		Set<String> deviceNames = new HashSet<>();
		for (ObjectVaultIndexEntry idx : vaultIndexes) {
			deviceNames.add(idx.vaultDeviceName);
		}
		return deviceNames;
	}

	/**
	 * @return the device names from the index corresponding to the selection criteria without device validation
	 */
	public Set<String> selectDeviceNames(String[] objectKeys, DetailSelector detailSelector) {
		Set<ObjectVaultIndexEntry> vaultIndexes = objectVaultMap.getVaultIndexes(objectKeys, detailSelector);
		Set<String> deviceNames = new HashSet<>();
		for (ObjectVaultIndexEntry idx : vaultIndexes) {
			deviceNames.add(idx.vaultDeviceName);
		}
		return deviceNames;
	}

	/**
	 * @param detailSelector specifies the selected vault index entries
	 * @return the vault key entries consisting of directory name and vault name
	 */
	public Set<VaultKeyPair> selectVaultKeys(DetailSelector detailSelector) {
		Function<String, Boolean> objectSelector = s -> true;
		Set<VaultKeyPair> result = new HashSet<>();
		for (ObjectVaultIndexEntry idx : objectVaultMap.getVaultIndexes(objectSelector, detailSelector)) {
			result.add(new VaultKeyPair(idx.vaultDirectory, idx.vaultName));
		}
		log.off(() -> "Selected      size=" + result.size());
		return result;
	}

	/**
	 * @param objectKeys are the primary selection parameters
	 * @param detailSelector specifies the selected vault index entries
	 * @return the vault key entries consisting of directory name and vault name
	 */
	public Set<VaultKeyPair> selectVaultKeys(String[] objectKeys, DetailSelector detailSelector) {
		Set<VaultKeyPair> result = new HashSet<>();
		for (ObjectVaultIndexEntry idx : objectVaultMap.getVaultIndexes(objectKeys, detailSelector)) {
			result.add(new VaultKeyPair(idx.vaultDirectory, idx.vaultName));
		}
		log.off(() -> "Selected      size=" + result.size());
		return result;
	}

	/**
	 * @param objectKey is the primary selection parameter
	 * @param detailSelector specifies the selected vault index entries
	 * @param classifier specifies the group key values
	 * @return the map with the key acc. to the classifier and the keys for accessing the vaults
	 */
	public TreeMap<String, List<VaultKeyPair>> selectGroupedVaultKeys(String objectKey, DetailSelector detailSelector,
			Function<ObjectVaultIndexEntry, String> classifier) {
		TreeMap<String, List<VaultKeyPair>> result = new TreeMap<>();
		Set<ObjectVaultIndexEntry> indexEntries = objectVaultMap.getVaultIndexes(new String[] { objectKey }, detailSelector);
		for (ObjectVaultIndexEntry idx : indexEntries) {
			String key = classifier.apply(idx);
			if (result.get(key) == null) result.put(key, new ArrayList<>());
			result.get(key).add(new VaultKeyPair(idx.vaultDirectory, idx.vaultName));
		}
		log.log(Level.INFO, "Size   ", result.size());
		return result;
	}

	/**
	 * @param objectSelector specifies the user's object key selection
	 * @return the object key entries in the vault index based on the current settings
	 */
	public Set<String> selectObjectKeys(Function<String, Boolean> objectSelector) {
		return objectVaultMap.getObjectKeys().stream().filter((s) -> objectSelector.apply(s)).collect(Collectors.toSet());
	}

	/**
	 * @return the object key entries in the vault index based on the current settings
	 */
	public Set<String> selectIndexedObjectKeys() {
		return objectVaultMap.getObjectKeys();
	}

	public boolean isEmpty() {
		return objectVaultMap.isEmpty();
	}
}
