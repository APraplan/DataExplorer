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

import static java.util.logging.Level.INFO;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import gde.Analyzer;
import gde.GDE;
import gde.histo.cache.VaultCollector;
import gde.histo.datasources.SourceFolders.DirectoryType;
import gde.histo.exclusions.ExclusionData;
import gde.log.Logger;

/**
 * Read the log file listings from the source folders list.
 * Avoid rebuilding if a choice of basic criteria did not change.
 * @author Thomas Eickert (USER)
 */
public class SourceDataSetExplorer {
	private static final String				$CLASS_NAME				= SourceDataSetExplorer.class.getName();
	private static final Logger				log								= Logger.getLogger($CLASS_NAME);

	protected final Analyzer			analyzer;
	protected final SourceFolders	sourceFolders;

	protected Consumer<String>		signaler	= s -> {};

	private final LongAdder						nonWorkableCount	= new LongAdder();
	private final List<Path>					excludedFiles			= new ArrayList<>();
	private final List<SourceDataSet>	sourceDataSets		= new ArrayList<>();

	private List<VaultCollector>			trusses						= new ArrayList<>();

	/**
	 * @param isStatusMessagesActive true for status messages during file system access (advisable in case file system latencies)
	 */
	public SourceDataSetExplorer(Analyzer analyzer, SourceFolders sourceFolders, boolean isStatusMessagesActive) {
		this.analyzer = analyzer;
		this.sourceFolders = sourceFolders;

		setStatusMessages(isStatusMessagesActive);
	}

	/**
	 * Explore the source files matching the validated extensions.
	 * Use file name extension lists and ignore file lists to determine the files required for the data access.
	 */
	private void listFiles(Map<Path, Set<DirectoryType>> pathsWithPermissions) {
		sourceDataSets.clear();
		nonWorkableCount.reset();
		excludedFiles.clear();

		int subDirectoryLevelMax = analyzer.getSettings().getSubDirectoryLevelMax();
		for (Entry<Path, Set<DirectoryType>> entry : pathsWithPermissions.entrySet()) {
			try {
				sourceDataSets.addAll(getFileListing(entry.getKey(), subDirectoryLevelMax, entry.getValue()));
			} catch (FileNotFoundException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * Recursively walk a directory tree.
	 * @param rootDirectory
	 * @param recursionDepth specifies the depth of recursion cycles (0 means no recursion)
	 * @param directoryTypes which the returned files must match (file extension)
	 * @return the unsorted files matching the {@code extensions} and not discarded as per exclusion list
	 */
	private List<SourceDataSet> getFileListing(Path rootDirectory, int recursionDepth, Set<DirectoryType> directoryTypes) throws FileNotFoundException {
		signaler.accept("get file names     " + rootDirectory.toString());
		List<SourceDataSet> sourceDataSets = new ArrayList<>();
		ExclusionData exclusionData = new ExclusionData(new DirectoryScanner(analyzer).getActiveFolder(), analyzer.getDataAccess());
		boolean isSuppressMode = analyzer.getSettings().isSuppressMode();
		for (String fileName : analyzer.getDataAccess().getSourceFolderList(rootDirectory).toArray(String[]::new)) {
			Path filePath = rootDirectory.resolve(fileName);
			if (analyzer.getDataAccess().existsSourceFile(filePath)) {
				SourceDataSet originFile = SourceDataSet.createSourceDataSet(filePath, analyzer);
				if (originFile != null && originFile.isWorkableFile(directoryTypes, sourceFolders)) {
					if (!isSuppressMode || !exclusionData.isExcluded(filePath.getFileName().toString())) {
						sourceDataSets.add(originFile);
					} else {
						excludedFiles.add(filePath);
						log.log(INFO, "file is excluded              ", filePath);
					}
				} else {
					nonWorkableCount.increment();
				}
			} else if (recursionDepth > 0) { // recursive walk by calling itself
				List<SourceDataSet> deeperList = getFileListing(filePath, recursionDepth - 1, directoryTypes);
				sourceDataSets.addAll(deeperList);
			}
		}
		signaler.accept("");
		return sourceDataSets;
	}

	/**
	 * @return the number of files which have been discarded during the last read operation due to an invalid file format
	 */
	public int getNonWorkableCount() {
		return nonWorkableCount.intValue();
	}

	/**
	 * @return the files which have been discarded during the last read operation based on the exclusion lists
	 */
	public List<Path> getExcludedFiles() {
		return excludedFiles;
	}

	/**
	 * @return the log files from all validated directories matching the validated extensions
	 */
	public List<SourceDataSet> getSourceFiles() {
		return sourceDataSets;
	}

	/**
	 * Collect the trusses in the osd files and in the native files (if supported by the device) which comply with the {@code trussCriteria)}
	 * @param doListFiles true gets the files from the file system whereas false uses the files list from the last call
	 */
	public void screen4Trusses(Map<Path, Set<DirectoryType>> pathsWithPermissions, boolean doListFiles) {
		// a channel change without any additional criterion change can use the existent list of files for reading the trusses (performance)
		if (doListFiles) listFiles(pathsWithPermissions);

		VaultChecker vaultChecker = new VaultChecker(analyzer);
		trusses = sourceDataSets.parallelStream() //
				.peek(d -> signaler.accept("get file properties    " + d.getActualFile().toString())) //
				.flatMap(d -> d.getSelectedTrusses(vaultChecker)) //
				.collect(Collectors.toList());
		signaler.accept("");
	}

	/**
	 * @param isActive true activates status messages during file system access which is advisable in case of high file system latency
	 */
	public void setStatusMessages(boolean isActive) {
		if (isActive)
			signaler = s -> GDE.getUiNotification().setStatusMessage(s);
		else
			signaler = s -> {};
	}

	public List<VaultCollector> getTrusses() {
		return this.trusses;
	}
}
