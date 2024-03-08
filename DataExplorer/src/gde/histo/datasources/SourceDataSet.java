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

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;

import com.sun.istack.Nullable;

import gde.Analyzer;
import gde.GDE;
import gde.config.DeviceConfigurations;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.VaultCollector;
import gde.histo.datasources.SourceFolders.DirectoryType;
import gde.histo.device.IHistoDevice;
import gde.histo.io.HistoOsdReaderWriter;
import gde.histo.utils.PathUtils;
import gde.io.FileHandler;
import gde.log.Logger;

/**
 * Exploring and checking source data sets (log files) on a file and vault attribute level.
 * @author Thomas Eickert (USER)
 */
public abstract class SourceDataSet {
	private static final String	$CLASS_NAME	= SourceDataSet.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

		protected final Path				filePath;
		protected final Analyzer		analyzer;


		/**
		 * @return true if the current device supports both directory type and file extension
		 */
		public boolean isWorkableFile(Set<DirectoryType> directoryTypes, SourceFolders sourceFolders) {
			return new VaultChecker(analyzer).isWorkableDataSet(filePath, directoryTypes, sourceFolders);
		}

		/**
		 * Determining the path is less costly than making a file from a path.
		 */
		protected SourceDataSet(Path filePath, Analyzer analyzer) {
			this.filePath = filePath;
			this.analyzer = analyzer;
		}

		/**
		 * @returns true is a file reader capable to deliver different measurement values based on device settings
		 */
		public abstract boolean providesReaderSettings();

		/**
		 * Adds support for link files pointing to data files.
		 * Deletes a link file if the data file does not exist.
		 * @return the data file (which differs in case of a link file)
		 */
		public abstract Path getActualFile();

		/**
		 * @param desiredRecordSetName
		 */
		public abstract void loadFile(Path tmpPath, String desiredRecordSetName);

		/**
		 * @return the path to the link file if the actual file does not hold the data
		 */
		public abstract String getLinkPath();

		/**
		 * @param deviceConfigurations
		 * @return the file directory name if it is not a device directory or an empty string
		 */
		protected String getObjectKey() {
			String dirName = filePath.getParent().getFileName().toString();
			DeviceConfigurations deviceConfigurations = analyzer.getDeviceConfigurations();
			return !deviceConfigurations.contains(dirName) ? dirName : GDE.STRING_EMPTY;
		}

		/**
		 * @return the trusses or an empty stream
		 */
		public abstract Stream<VaultCollector> getSelectedTrusses(VaultChecker vaultChecker);

		/**
		 * Promote trusses into vaults by reading the source file.
		 * @param trusses lists the requested vaults
		 */
		public abstract void readVaults4Ui(Path sourcePath, List<VaultCollector> trusses);

		/**
		 * @param desiredRecordSetName is a valid recordsetName or empty
		 * @return true if the file was opened or imported
		 */
		public boolean load(String desiredRecordSetName) {
			if (!analyzer.getDataAccess().existsSourceFile(filePath)) return false;

			loadFile(filePath, desiredRecordSetName);
			return true;
		}

		public Path getPath() {
			return this.filePath;
		}

		/**
		 * Compose the record set extend to give capability to identify source of this record set
		 * @return
		 */
		protected String getRecordSetExtend() {
			String fileName = filePath.getFileName().toString();
			String recordSetNameExtend = GDE.STRING_EMPTY;
			if (fileName.contains(GDE.STRING_UNDER_BAR)) {
				try {
					Integer.parseInt(fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)));
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
				} catch (Exception e) {
					if (fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)).length() <= 8)
						recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
				}
			} else {
				try {
					Integer.parseInt(fileName.substring(0, 4));
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, 4) + GDE.STRING_RIGHT_BRACKET;
				} catch (Exception e) {
					if (fileName.substring(0, fileName.length()).length() <= 8 + 4)
						recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.length() - 4) + GDE.STRING_RIGHT_BRACKET;
				}
			}
			return recordSetNameExtend;
		}

		@Override
		public String toString() {
			return this.filePath.toString();
		}

	private static final class OsdDataSet extends SourceDataSet {
		@SuppressWarnings("hiding")
		private static final Logger	log										= Logger.getLogger(OsdDataSet.class.getName());

		/** Is the real source file because {@source file} might be link file */
		protected Path							actualFile						= null;
		private boolean							isActualFileVerified	= false;

		protected OsdDataSet(Path filePath, Analyzer analyzer) {
			super(filePath, analyzer);
		}

		@Override
		public Stream<VaultCollector> getSelectedTrusses(VaultChecker vaultChecker) {
			return getTrusses().stream().filter(t -> vaultChecker.matchDeviceChannelObjectAndStart(t.getVault()));
		}

		private List<VaultCollector> getTrusses() {
			List<VaultCollector> trusses = new ArrayList<>();
			if (getActualFile() != null) {
				String objectDirectory = getObjectKey();
				try (InputStream sourceInputStream = analyzer.getDataAccess().getSourceInputStream(getActualFile());) {
					trusses = HistoOsdReaderWriter.readTrusses(sourceInputStream, getActualFile(), objectDirectory, analyzer);
				} catch (Exception e) {
					// link file points to non existent file
					log.log(Level.SEVERE,  getActualFile() + GDE.STRING_MESSAGE_CONCAT + e.getMessage(), e);
				}
				for (VaultCollector truss : trusses) {
					truss.getVault().setLoadLinkPath(Paths.get(getLinkPath()));
					truss.getVault().setLogLinkPath(getLinkPath());
					truss.setSourceDataSet(this);
				}
			}
			return trusses;
		}

		@Override
		public void readVaults4Ui(Path sourcePath, List<VaultCollector> trusses) {
			try (InputStream sourceInputStream = analyzer.getDataAccess().getSourceInputStream(sourcePath);) {
				HistoOsdReaderWriter.readVaults(sourceInputStream, trusses, analyzer);
			} catch (Exception e) {
				log.log(Level.WARNING, String.format("invalid file format: %s  channelNumber=%d  %s", //
						analyzer.getActiveDevice().getName(), analyzer.getActiveChannel().getNumber(), sourcePath));
			}
		}

		@Override
		public boolean providesReaderSettings() {
			return false;
		}

		@Override
		@Nullable
		public Path getActualFile() {
			if (actualFile == null && !isActualFileVerified) {
				setActualFile();
			}
			return actualFile;
		}

		protected void setActualFile() {
			actualFile = analyzer.getDataAccess().getActualSourceFile(filePath);
			isActualFileVerified = true;
		}

		/**
		 * @return the path to the link file if the actual file does not hold the data or an empty string otherwise
		 */
		@Override
		public String getLinkPath() {
			String linkPath = !filePath.toAbsolutePath().equals(getActualFile().toAbsolutePath()) ? filePath.toString() : GDE.STRING_EMPTY;
			return linkPath;
		}

		@Override
		public void loadFile(Path tmpPath, String desiredRecordSetName) {
			new FileHandler().openOsdFile(tmpPath.toString(), desiredRecordSetName);
		}

	}

	private static final class ImportDataSet extends SourceDataSet {
		@SuppressWarnings("hiding")
		private static final Logger log = Logger.getLogger(ImportDataSet.class.getName());

		protected ImportDataSet(Path filePath, Analyzer analyzer) {
			super(filePath, analyzer);
		}

		@Override
		public Stream<VaultCollector> getSelectedTrusses(VaultChecker vaultChecker) {
			return getTrusses().stream().filter(t -> vaultChecker.matchObjectAndStart(t.getVault()));
		}

		private List<VaultCollector> getTrusses() {
			String objectDirectory = getObjectKey();
			String recordSetBaseName = analyzer.getActiveChannel().getChannelConfigKey() + getRecordSetExtend();
			VaultCollector truss = new VaultCollector(objectDirectory, filePath, 0, analyzer.getChannels().size(), recordSetBaseName,
					analyzer, providesReaderSettings());
			truss.setSourceDataSet(this);
			return new ArrayList<>(Arrays.asList(truss));
		}

		@Override
		public void readVaults4Ui(Path sourcePath, List<VaultCollector> trusses) {
			if (trusses.isEmpty()) throw new IllegalArgumentException("at least one trusses entry is required");
			List<ExtendedVault> histoVaults = new ArrayList<>();

			String loadFilePath = trusses.get(0).getVault().getLoadFilePath();
			for (VaultCollector truss : trusses) {
				if (truss.getVault().getLoadFilePath().equals(loadFilePath)) {
					Supplier<InputStream> inputStream = () -> analyzer.getDataAccess().getSourceInputStream(sourcePath);
					try {
						((IHistoDevice) analyzer.getActiveDevice()).getRecordSetFromImportFile(inputStream, truss, analyzer);
					} catch (Exception e) {
						log.log(Level.WARNING, String.format("invalid file format: %s  channelNumber=%d  %s", //
								analyzer.getActiveDevice().getName(), analyzer.getActiveChannel().getNumber(), sourcePath));
					}
					histoVaults.add(truss.getVault());
				} else
					throw new UnsupportedOperationException("all trusses must carry the same logFilePath");
			}
		}

		@Override
		public boolean providesReaderSettings() {
			return true;
		}

		@Override
		public Path getActualFile() {
			return filePath;
		}

		@Override
		public String getLinkPath() {
			return GDE.STRING_EMPTY;
		}

		@Override
		public void loadFile(Path tmpPath, String desiredRecordSetName) {
			((IHistoDevice) analyzer.getActiveDevice()).importDeviceData(tmpPath);
		}
	}

	/**
	 * @param filePath is a file path
	 * @return null if the file is not supported
	 */
	@Nullable
	public static SourceDataSet createSourceDataSet(Path filePath, Analyzer analyzer) {
		log.log(Level.FINE, "started");
		String extention = PathUtils.getFileExtention(filePath);
		if (extention.equals(GDE.FILE_ENDING_DOT_OSD))
			return new OsdDataSet(filePath, analyzer);
		else if (analyzer.getActiveDevice() instanceof IHistoDevice) {
			List<String> importExtentions = ((IHistoDevice) analyzer.getActiveDevice()).getSupportedImportExtentions();
			if (importExtentions.contains(extention)) {
				return new ImportDataSet(filePath, analyzer); // todo implement logDataSet and native HottLogReader
			}
		}
		return null;
	}

}