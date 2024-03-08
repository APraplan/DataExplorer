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

package gde.histo.datasources;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.istack.Nullable;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.device.IDevice;
import gde.exception.ThrowableUtils;
import gde.histo.device.IHistoDevice;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;

/**
 * Detect valid sub paths.
 * @author Thomas Eickert (USER)
 */
public class SourceFolders {
	private static final String	$CLASS_NAME	= SourceFolders.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Data sources supported by the history including osd link files.
	 */
	public enum DirectoryType {
		DATA {
			@Override
			public Path getBasePath(String dataFilePath) {
				return dataFilePath == null || dataFilePath.isEmpty() ? null : Paths.get(dataFilePath);
			}

			@Override
			public Path getDeviceSubPath(IDevice device) {
				return Paths.get(device.getDeviceConfiguration().getPureDeviceName(device.getName()));
			}

			@Override
			public List<String> getDataSetExtentions(IDevice device, Settings settings) {
				List<String> extentions = new ArrayList<>();
				extentions.add(GDE.FILE_ENDING_DOT_OSD);
				if (device instanceof IHistoDevice && settings.getSearchDataPathImports()) {
					extentions.addAll(((IHistoDevice) device).getSupportedImportExtentions());
				}
				return extentions;
			}

			@Override
			public Stream<Path> getExternalBaseDirs(Settings settings) {
				try {
					return Arrays.stream(settings.getDataFoldersCsv().split(GDE.STRING_CSV_SEPARATOR)) //
							.map(String::trim).filter(s -> !s.isEmpty()).map(Paths::get);
				} catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					return Stream.empty();
				}
			}

			@Override
			public boolean isActive(IDevice device, Settings settings) {
				// the DataSetPath is always a valid path
				return true;
			}
		},
		IMPORT {
			@Override
			@Nullable
			public Path getBasePath(String dataFilePath) {
				return null;
			}

			@Override
			public Path getDeviceSubPath(IDevice device) {
				return null; // native files are not segregated by devices
			}

			@Override
			public List<String> getDataSetExtentions(IDevice device, Settings settings) {
				if (device instanceof IHistoDevice)
					return ((IHistoDevice) device).getSupportedImportExtentions();
				else
					return new ArrayList<>();
			}

			@Override
			public Stream<Path> getExternalBaseDirs(Settings settings) {
				try {
					return Arrays.stream(settings.getImportFoldersCsv().split(GDE.STRING_CSV_SEPARATOR)) //
							.map(String::trim).filter(s -> !s.isEmpty()).map(Paths::get);
				} catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					return Stream.empty();
				}
			}

			@Override
			public boolean isActive(IDevice device, Settings settings) {
				log.finest(() -> " IMPORT : Extensions.isEmpty=" + getDataSetExtentions(device, settings).isEmpty() + " ExternalFolders.exist=" + getExternalBaseDirs(settings).anyMatch(e -> true));
				if (getDataSetExtentions(device, settings).isEmpty()) {
					return false;
				} else {
					return getExternalBaseDirs(settings).anyMatch(e -> true);
				}
			};
		};

		/**
		 * Use this instead of values() to avoid repeatedly cloning actions.
		 */
		public static final DirectoryType[] VALUES = values();

		public static EnumSet<DirectoryType> getValidDirectoryTypes(IDevice device, Settings settings) {
			EnumSet<DirectoryType> directoryTypes = EnumSet.noneOf(DirectoryType.class);
			for (DirectoryType directoryType : VALUES) {
				if (directoryType.isActive(device, settings)) directoryTypes.add(directoryType);
			}
			return directoryTypes;
		}

		/**
		 * @param dataFilePath
		 * @return the current directory path independent from object / device
		 */
		@Nullable
		public abstract Path getBasePath(String dataFilePath);

		/**
		 * @return the sub path for finding files assigned to the device
		 */
		@Nullable
		public abstract Path getDeviceSubPath(IDevice device);

		/**
		 * @return the supported file extensions (e.g. '.bin') or an empty list
		 */
		public abstract List<String> getDataSetExtentions(IDevice device, Settings settings);

		/**
		 * @return true if the prerequisites for the directory type are fulfilled
		 */
		public abstract boolean isActive(IDevice device, Settings settings);

		public abstract Stream<Path> getExternalBaseDirs(Settings settings);
	}

	/**
	 * Designed to hold all folder paths feeding the file scanning steps.
	 * Supports an equality check comparing the current paths to an older path map.
	 */
	private final Map<DirectoryType, Set<Path>>	folders	=															// formatting
			new EnumMap<DirectoryType, Set<Path>>(DirectoryType.class) {
				private static final long serialVersionUID = -8624409377603884008L;

				/**
				 * @return true if the keys are equal and the sets hold the same path strings
				 */
				@Override
				public boolean equals(Object obj) {
					if (this == obj) return true;
					if (obj == null) return false;
					if (getClass() != obj.getClass()) return false;
					@SuppressWarnings("unchecked")																						// reason is anonymous class
					Map<DirectoryType, Set<Path>> that = (Map<DirectoryType, Set<Path>>) obj;
					boolean hasSameDirectoryTypes = keySet().equals(that.keySet());
					if (hasSameDirectoryTypes) {
						boolean isEqual = true;
						for (Entry<DirectoryType, Set<Path>> entry : entrySet()) {
							Set<Path> thisSet = entry.getValue();
							Set<Path> thatSet = that.get(entry.getKey());
							isEqual &= thisSet.equals(thatSet);
							if (!isEqual) break;
						}
						log.log(Level.FINEST, "isEqual=", isEqual);
						return isEqual;
					} else {
						return false;
					}
				}
			};

	private final Analyzer											analyzer;

	public SourceFolders(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	/**
	 * Determine the valid directory paths from all log sources.
	 * The result depends on the active device and object.
	 * @param signaler is a status message setter
	 */
	public void defineDirectories(Consumer<String> signaler) {
		folders.clear();
		IDevice device = analyzer.getActiveDevice();
		Settings settings = analyzer.getSettings();
		for (DirectoryType directoryType : DirectoryType.getValidDirectoryTypes(device, settings)) {
			signaler.accept("find object folders for " + directoryType);
			Set<Path> currentPaths = defineCurrentPaths(directoryType);
			signaler.accept("");
			if (!settings.getActiveObjectKey().isEmpty()) {
				Set<Path> externalObjectPaths = directoryType.getExternalBaseDirs(analyzer.getSettings()) //
						.map(p -> defineObjectPaths(p)).flatMap(Set::stream).collect(Collectors.toSet());
				currentPaths.addAll(externalObjectPaths);
				log.log(Level.FINE, directoryType.toString(), externalObjectPaths);
			}
			log.log(Level.FINE, directoryType.toString(), currentPaths);
			folders.put(directoryType, currentPaths);
		}
	}

	@SuppressWarnings("unused")
	private void removeDoubleDirectories() {
		// no, because the same directory might contribute different file types to the screening
	}

	/**
	 * Determine the valid directory paths from the currently defined working / import directory.
	 * The result depends on the active device and object.
	 * @return the list with 0 .. n entries
	 */
	private Set<Path> defineCurrentPaths(DirectoryType directoryType) {
		Set<Path> newPaths = new HashSet<>();

		Path basePath = directoryType.getBasePath(analyzer.getSettings().getDataFilePath());
		if (basePath == null) {
			// an unavailable path results in no files found
		} else {
			if (analyzer.getSettings().getActiveObjectKey().isEmpty()) {
				IDevice device = analyzer.getActiveDevice();
				Path rootPath = directoryType.getDeviceSubPath(device) != null ? basePath.resolve(directoryType.getDeviceSubPath(device)) : basePath;
				newPaths.add(rootPath);
			} else {
				newPaths = defineObjectPaths(basePath);
			}
		}
		return newPaths;
	}

	private Set<Path> defineObjectPaths(Path basePath) {
		Set<Path> paths = new HashSet<Path>();
		Stream<String> objectKeys = Stream.of(analyzer.getSettings().getActiveObjectKey());
		try (Stream<Path> objectPaths = analyzer.getDataAccess().getSourceFolders(basePath, objectKeys)) {
			paths = objectPaths.collect(Collectors.toSet());
		} catch (IOException | UncheckedIOException e) {
			log.log(Level.SEVERE, e.getMessage(), " is not accessible : " + e.getClass());
			if (e.getMessage().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX).contains(analyzer.getSettings().getDataFilePath())) {
				ThrowableUtils.rethrow(e);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		return paths;
	}

	public Set<Entry<DirectoryType, Set<Path>>> entrySet() {
		return folders.entrySet();
	}

	public Collection<Set<Path>> values() {
		return folders.values();
	}

	public Map<Path, Set<DirectoryType>> getMap() { // todo change folders to Map<Path, Set<DirectoryType>>
		Map<Path, Set<DirectoryType>> directoryTypesMap = new HashMap<>();
		for (Entry<DirectoryType, Set<Path>> entry : folders.entrySet()) {
			for (Path path : entry.getValue()) {
				Set<DirectoryType> set = directoryTypesMap.get(path);
				if (set == null) {
					set = EnumSet.noneOf(DirectoryType.class);
					directoryTypesMap.put(path, set);
				}
				set.add(entry.getKey());
			}
		}
		return directoryTypesMap;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int hashCode = 1;
		hashCode = prime * hashCode + ((this.folders == null) ? 0 : this.folders.hashCode());
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SourceFolders other = (SourceFolders) obj;
		if (this.folders == null) {
			if (other.folders != null) return false;
		} else if (!this.folders.equals(other.folders)) return false;
		return true;
	}

	@Override
	public String toString() {
		return "" + this.folders;
	}

	public int getFoldersCount() {
		return values().parallelStream().mapToInt(Collection::size).sum();
	}

	/**
	 * @return the rightmost folder names, e.g. 'FS14 | MiniEllipse'
	 */
	public String getTruncatedFileNamesCsv() {
		String ellipsisText = Messages.getString(MessageIds.GDE_MSGT0864);
		return folders.values().stream().flatMap(Collection::stream).map(Path::getFileName).map(Path::toString) //
				.map(s -> s.length() > 22 ? s.substring(0, 22) + ellipsisText : s) //
				.distinct().collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR));
	}

	/**
	 * @return the full paths with prefix directory type, e.g. 'DATA: E:\User\Logs\FS14'
	 */
	public String getDecoratedPathsCsv() {
		List<String> directoryTypeTexts = new ArrayList<>();
		for (Entry<DirectoryType, Set<Path>> directoryEntry : folders.entrySet()) {
			String text = directoryEntry.getValue().stream().map(Path::toString) //
					.map(p -> directoryEntry.getKey().toString() + GDE.STRING_BLANK_COLON_BLANK + p) //
					.collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR));
			directoryTypeTexts.add(text);
		}
		return directoryTypeTexts.stream().collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR));
	}

	public boolean isMatchingPath(Path sourceFile) {
		int nameCountMax = analyzer.getSettings().getSubDirectoryLevelMax() + 1;
		Map<Path, Set<DirectoryType>> pathMap = getMap();
		for (Path path : pathMap.keySet()) {
			try {
				if (path.relativize(sourceFile).getNameCount() <= nameCountMax) return true;
			} catch (Exception e) {
				// path hat no common starter
			}
		}
		return false;
	}

}
