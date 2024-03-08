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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import gde.Analyzer;
import gde.GDE;
import gde.data.Channel;
import gde.device.IDevice;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.datasources.HistoSet.RebuildStep;
import gde.histo.datasources.SourceFolders.DirectoryType;
import gde.log.Logger;

/**
 * Access to history source directories.
 * @author Thomas Eickert (USER)
 */
public final class DirectoryScanner {
	private static final String	$CLASS_NAME						= DirectoryScanner.class.getName();
	private static final Logger	log										= Logger.getLogger($CLASS_NAME);

	/**
	 * Average elapsed time per identified folder during folder scan.</br>
	 * local drive: 13,5 to 16 ms/folder </br>
	 * but the full data drive scan may take up to 400 ms.
	 */
	private static final int		SLOW_FOLDER_LIMIT_MS	= 222;

	private static WatchDir			watchDir;
	private static Thread				watchDirThread;

	/**
	 * Extended predicate which supports IOException and NotSupportedFileFormatException.
	 */
	@FunctionalInterface
	interface CheckedPredicate<U> {
		boolean test(U u) throws IOException, NotSupportedFileFormatException;
	}

	/**
	 * @return the data folder residing in the top level working directory (data path + object key resp device)
	 */
	public static Path getActiveFolder4Ui() {
		return new DirectoryScanner(Analyzer.getInstance()).getActiveFolder(); // ok
	}

	/**
	 * @return the data folder residing in the top level working directory (data path + object key resp device)
	 */
	public Path getActiveFolder() {
		IDevice device = analyzer.getActiveDevice();
		String activeObjectKey = analyzer.getSettings().getActiveObjectKey();
		String subPathData = activeObjectKey.isEmpty() ? device.getDeviceConfiguration().getPureDeviceName(device.getName()) : activeObjectKey;
		return Paths.get(analyzer.getSettings().getDataFilePath()).resolve(subPathData);
	}

	/**
	 * Build the source folders list from the source directories.
	 * Avoid rebuilding if a selection of basic criteria did not change.
	 */
	private static final class SourceFoldersBuilder {
		@SuppressWarnings("hiding")
		private static final Logger						log											= Logger.getLogger(SourceFoldersBuilder.class.getName());

		private final EnumSet<DirectoryType>	validatedDirectoryTypes	= EnumSet.noneOf(DirectoryType.class);

		private final Analyzer								analyzer;

		private IDevice												validatedDevice					= null;
		private Channel												validatedChannel				= null;
		private String												validatedObjectKey			= GDE.STRING_EMPTY;

		private SourceFolders									sourceFolders						= null;

		/**
		 * File access speed indicator. True is slow file system access.
		 * Simple algorithm.
		 */
		private boolean												isSlowFolderAccess			= false;
		private boolean												isMajorChange						= false;
		private boolean												isChannelChangeOnly			= false;

		public SourceFoldersBuilder(Analyzer analyzer) {
			this.analyzer = analyzer;
			this.sourceFolders = new SourceFolders(analyzer);
		}

		/**
		 * Re- initializes the class.
		 */
		public void initialize() {
			this.validatedDirectoryTypes.clear();

			this.validatedDevice = null;
			this.validatedObjectKey = GDE.STRING_EMPTY;

			this.isSlowFolderAccess = false;
			this.isMajorChange = false;
			this.isChannelChangeOnly = false;
		}

		/**
		 * Determine if the criteria for determining the source log files have changed.
		 * Please note that not all criteria are checked:
		 * Thus changing any criteria like data file path or various histo settings must trigger a histo reset {@link RebuildStep#A_HISTOSET}).
		 * @param rebuildStep defines which steps during histo data collection are skipped
		 * @return true if the criteria have changed since the last invocation of the directory scanner
		 */
		public boolean validateAndBuild(RebuildStep rebuildStep) throws IOException, NotSupportedFileFormatException {
			IDevice lastDevice = validatedDevice;
			Channel lastChannel = validatedChannel;
			EnumSet<DirectoryType> lastDirectoryTypes = EnumSet.copyOf(validatedDirectoryTypes);
			String lastObjectKey = validatedObjectKey;

			boolean isFirstCall = lastDevice == null;
			isMajorChange = rebuildStep == RebuildStep.A_HISTOSET || isFirstCall;

			validatedDevice = analyzer.getActiveDevice();
			boolean isNewDevice = lastDevice != null && validatedDevice != null ? !lastDevice.getName().equals(validatedDevice.getName())
					: validatedDevice != null;
			isMajorChange = isMajorChange || isNewDevice;

			// the import extentions do not have any influence on the validated folders list

			validatedDirectoryTypes.clear();
			validatedDirectoryTypes.addAll(DirectoryType.getValidDirectoryTypes(validatedDevice, analyzer.getSettings()));
			isMajorChange = isMajorChange || !lastDirectoryTypes.equals(validatedDirectoryTypes);

			validatedObjectKey = analyzer.getSettings().getActiveObjectKey();
			isMajorChange = isMajorChange || !lastObjectKey.equals(validatedObjectKey);

			// avoid costly directory scan and building directory file event listeners (WatchDir)
			if (isMajorChange) {
				long nanoTime = System.nanoTime();
				Consumer<String> signaler = isSlowFolderAccess ? s -> GDE.getUiNotification().setStatusMessage(s) : s -> {};
				sourceFolders.defineDirectories(signaler);
				long foldersCount = sourceFolders.values().stream().mapToInt(Collection::size).sum();
				long elapsed_ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime);
				isSlowFolderAccess = elapsed_ms / (foldersCount + 3) > SLOW_FOLDER_LIMIT_MS; // +3 for initial overhead (data path)
				log.fine(() -> "slowFolderAccess=" + isSlowFolderAccess + "  numberOfFolders=" + foldersCount + " in " + elapsed_ms + " [ms]");
			}

			// the channel selection does not have any influence on the validated folders list
			validatedChannel = analyzer.getActiveChannel();
			isChannelChangeOnly = !isMajorChange && RebuildStep.B_HISTOVAULTS.isEqualOrBiggerThan(rebuildStep) && !lastChannel.equals(validatedChannel);
			isMajorChange = isMajorChange || !lastChannel.equals(validatedChannel);
			return true;
		}

		/**
		 * @return true if the last build detected a full change
		 */
		public boolean isMajorChange() {
			return this.isMajorChange;
		}

	}

	private final SourceFoldersBuilder					sourceFoldersBuilder;
	private final Analyzer											analyzer;
	private final CheckedPredicate<RebuildStep>	sourceFileValidator;

	public DirectoryScanner(Analyzer analyzer) {
		this.analyzer = analyzer;
		this.sourceFoldersBuilder = new SourceFoldersBuilder(analyzer);
		// define if the log paths are checked for any changed / new / deleted files
		if (analyzer.getSettings().isSourceFileListenerActive()) {
			this.sourceFileValidator = (RebuildStep r) -> validateDirectoryFiles(r);
		} else {
			this.sourceFileValidator = (RebuildStep r) -> validateDirectoryPaths(r);
		}

		initialize();
	}

	/**
	 * Re- initializes the class.
	 */
	public void initialize() {
		sourceFoldersBuilder.initialize();
	}

	/**
	 * Re- Initializes the source log paths watcher.
	 */
	private void initializeWatchDir(List<Path> sourceLogPaths) {
		closeWatchDir();

		SourceFolders sourceFolders = sourceFoldersBuilder.sourceFolders;
		Predicate<Path> workableFileDecider = new Predicate<Path>() {
			@Override
			public boolean test(Path filePath) {
				Set<DirectoryType> directoryTypes = sourceFolders.getMap().entrySet().stream() //
						.filter(e -> filePath.startsWith(e.getKey())) //
						.map(e -> e.getValue()).flatMap(Set::stream)//
						.collect(Collectors.toSet());
				SourceDataSet sourceDataSet = SourceDataSet.createSourceDataSet(filePath, analyzer);
				return sourceDataSet != null && sourceDataSet.isWorkableFile(directoryTypes, sourceFolders);
			}
		};

		try {
			boolean recursive = true;
			watchDir = new WatchDir(sourceLogPaths, recursive, workableFileDecider);
			watchDirThread = new Thread(watchDir::processEvents, "watchDir");
			try {
				watchDirThread.start();
				log.log(Level.FINEST, "watchDir thread started");
			} catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		} catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 *
	 */
	public static void closeWatchDir() {
		if (watchDirThread != null) {
			try {
				watchDirThread.interrupt();
				watchDirThread.join();
			} catch (InterruptedException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * Use alternatively a path or files checker function which conforms to the DE settings.
	 * @return the rebuild step conforming to the input value and the validation scan results
	 */
	public RebuildStep isValidated(RebuildStep rebuildStep) throws IOException, NotSupportedFileFormatException {
		if (sourceFileValidator.test(rebuildStep))
			return rebuildStep;
		else if (rebuildStep.isEqualOrBiggerThan(RebuildStep.B_HISTOVAULTS))
			return rebuildStep;
		else
			return RebuildStep.B_HISTOVAULTS;
	}

	/**
	 * @param rebuildStep defines which steps during histo data collection are skipped
	 * @return true if the list of source log directories has not changed and the directories did not send any events.
	 */
	private boolean validateDirectoryFiles(RebuildStep rebuildStep) throws IOException, NotSupportedFileFormatException {
		sourceFoldersBuilder.validateAndBuild(rebuildStep);
		boolean isValid = !sourceFoldersBuilder.isMajorChange();

		if (watchDir == null || !isValid) {
			initializeWatchDir(sourceFoldersBuilder.sourceFolders.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
		}
		log.log(Level.FINER, "hasChangedLogFiles=", watchDir.hasChangedLogFiles());
		return isValid && !watchDir.hasChangedLogFilesThenReset();
	}

	/**
	 * @param rebuildStep defines which steps during histo data collection are skipped
	 * @return true if the list of source log directories has not changed
	 */
	private boolean validateDirectoryPaths(RebuildStep rebuildStep) throws IOException, NotSupportedFileFormatException {
		sourceFoldersBuilder.validateAndBuild(rebuildStep);
		return !sourceFoldersBuilder.isMajorChange();
	}

	public int getValidatedFoldersCount() {
		if (sourceFoldersBuilder.sourceFolders != null) {
			return sourceFoldersBuilder.sourceFolders.getFoldersCount();
		} else {
			return 0;
		}
	}

	public SourceFolders getSourceFolders() {
		return sourceFoldersBuilder.sourceFolders;
	}

	public boolean isSlowFolderAccess() {
		return sourceFoldersBuilder.isSlowFolderAccess;
	}

	/**
	 * @return true if nothing else except the channel has changed since the last directory scan
	 */
	public boolean isChannelChangeOnly() {
		return sourceFoldersBuilder.isChannelChangeOnly;
	}

}
