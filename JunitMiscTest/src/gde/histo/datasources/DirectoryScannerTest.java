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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import gde.histo.base.NonUiTestCase;
import gde.histo.datasources.HistoSet.RebuildStep;
import gde.utils.FileUtils;
import gde.utils.ObjectKeyCompliance;

/**
 *
 * @author Thomas Eickert (USER)
 */
class DirectoryScannerTest extends NonUiTestCase {
	private final static String	$CLASS_NAME	= DirectoryScannerTest.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	/**
	 * Directory listener detects file create, modification or delete.
	 * For osd files and native files (if supported by the device).
	 * Only if settings is_source_file_listener_active.
	 */
	@ParameterizedTest
	@CsvSource({ //
			"HoTTAdapter, true, false", //
			"HoTTAdapter, true, true", //
			"UltraTrioPlus14, false, false", //
			"UltraTrioPlus14, false, true" })
	void testWatchDir(String deviceName, boolean isBinSupported, boolean searchDataPathImports) {
		settings.setSearchDataPathImports(searchDataPathImports);

		Stream.of(true, false).forEach(sourceFileListenerActive -> {
			settings.setSourceFileListenerActive(sourceFileListenerActive);

			setDeviceChannelObject(deviceName, 1, "TopSky");

			DirectoryScanner directoryScanner = new DirectoryScanner(analyzer);
			try {
				RebuildStep realRebuildStep = directoryScanner.isValidated(RebuildStep.F_FILE_CHECK);
				log.log(Level.FINE, directoryScanner.getSourceFolders().getDecoratedPathsCsv());
				assertEquals("rebuildStep after first scan    : ", RebuildStep.B_HISTOVAULTS, realRebuildStep);
				assertEquals("rebuildStep 2nd scan w/o change : ", RebuildStep.F_FILE_CHECK, directoryScanner.isValidated(RebuildStep.F_FILE_CHECK));

				Path path = directoryScanner.getSourceFolders().values().stream().flatMap(Collection::stream).findFirst().orElse(Paths.get(""));
				List<File> fileListing = FileUtils.getFileListing(path.toFile(), 1);
				// copy to extension osd because this is the only one captured by WatchDir with all user settings
				File fileCopy = fileListing.get(0).toPath().getParent().resolve("UnitTest.osd").toFile();
				FileUtils.copyFile(fileListing.get(0), fileCopy);
				FileUtils.deleteFile(fileCopy.getPath());

				Thread.sleep(WatchDir.DELAY * 11 / 10); // wait 10% longer than the DELAY

				// this is the actual test for watching osd files
				assertEquals("rebuildStep", (boolean) sourceFileListenerActive, RebuildStep.B_HISTOVAULTS == directoryScanner.isValidated(RebuildStep.F_FILE_CHECK));

				// copy to extension osd because this is the only one captured by WatchDir with all user settings
				File fileCopyBin = fileListing.get(0).toPath().getParent().resolve("UnitTest.bin").toFile();
				FileUtils.copyFile(fileListing.get(0), fileCopyBin);
				FileUtils.deleteFile(fileCopyBin.getPath());

				Thread.sleep(WatchDir.DELAY * 11 / 10); // wait 10% longer than the DELAY

				// this is the actual test for watching bin files
				boolean supportsBinListener = searchDataPathImports && isBinSupported && sourceFileListenerActive;
				assertEquals("rebuildStep", supportsBinListener, RebuildStep.B_HISTOVAULTS == directoryScanner.isValidated(RebuildStep.F_FILE_CHECK));
			} catch (Exception e) {
				e.printStackTrace();
				fail("directory not accessible");
			}
			DirectoryScanner.closeWatchDir();
		});
	}

	/**
	 * Multiple object directories are identified.
	 */
	@ParameterizedTest
	@CsvSource({ //
			"'', 1", //
			"FS14, 1", //
			"TopSky, 2" })
	void testObjectDirs(String objectKey, int numberOfDirectories) {
		setDeviceChannelObject("HoTTAdapter", 1, objectKey);

		DirectoryScanner directoryScanner = new DirectoryScanner(analyzer);
		try {
			directoryScanner.isValidated(RebuildStep.F_FILE_CHECK);
			log.log(Level.FINE, directoryScanner.getSourceFolders().getDecoratedPathsCsv());

			// this is the actual test
			assertEquals("number of directories", numberOfDirectories, directoryScanner.getSourceFolders().getMap().size());
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("directory not accessible");
		}
	}

	/**
	 * Supplement object directories are detected.
	 */
	@ParameterizedTest
	@CsvSource({ //
			"parentFolder, 3", //
			"noDataFolder, 2" })
	void testSupplementDirs(String testCase, int numberOfDirectories) {
		String dataFolderPath = testCase.equals("parentFolder") ? DataSource.TESTDATA.getDataPath("").toString() : "";

		settings.setDataFoldersCsv(dataFolderPath);

		setDeviceChannelObject("HoTTAdapter", 1, "TopSky");

		DirectoryScanner directoryScanner = new DirectoryScanner(analyzer);
		try {
			directoryScanner.isValidated(RebuildStep.F_FILE_CHECK);
			log.log(Level.FINE, directoryScanner.getSourceFolders().getDecoratedPathsCsv());
			assertEquals("source folders size    : ", numberOfDirectories, directoryScanner.getSourceFolders().getMap().size());
		} catch (Exception e) {
			e.printStackTrace();
			fail("directory not accessible");
		}
	}

	/**
	 * Performance meter.
	 * 04.2017: 270 sec = 151+119 sec (349 sec = 192+157 sec with new instance for each channel) @ 46 objects * 85 devices
	 * 04.2017: 119 sec = 70+49 sec @ 25 objects * 85 devices
	 * 04.2017: 136 sec = 77+59 sec @ 24 objects * 85 devices
	 */
	@Tag("performance")
	@Test
	void testElapsed() {
		settings.setSearchDataPathImports(true);
		// take the non-device directories in the data path as object keys
		Set<String> objectKeys = ObjectKeyCompliance.defineObjectKeyCandidates(analyzer.getDeviceConfigurations().getAllConfigurations());
		objectKeys.add(""); // empty string for 'device oriented'
		this.settings.setObjectList(objectKeys.toArray(new String[0]), 0);

		List<Long> elapsed_sec = new ArrayList<>();
		Stream.of(true, false).forEach(b -> {
			long nanoTime = System.nanoTime();
			settings.setSourceFileListenerActive(b);

			analyzer.getDeviceConfigurations().getAllConfigurations().entrySet().stream().forEach(device -> {
				for (String objectKey : objectKeys) {
					// take the same instance of the scanner in order to get the performance optimization for channel switching
					DirectoryScanner directoryScanner = new DirectoryScanner(analyzer);
					for (int i = 1; i <= device.getValue().getChannelCount(); i++) {
						setDeviceChannelObject(device.getKey(), i, objectKey);
						try {
							directoryScanner.isValidated(RebuildStep.F_FILE_CHECK);
							log.log(Level.FINE, directoryScanner.getSourceFolders().getDecoratedPathsCsv());
						} catch (Exception e) {
							e.printStackTrace();
							fail("directory not accessible");
						}
					}
				}
			});

			DirectoryScanner.closeWatchDir();
			elapsed_sec.add(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - nanoTime));
		});
		log.log(Level.OFF, String.format("%d sec = %d+%d sec @%3d objects *%3d devices", //
				elapsed_sec.get(0) + elapsed_sec.get(1), elapsed_sec.get(0), elapsed_sec.get(1), ObjectKeyCompliance.defineObjectKeyCandidates(analyzer.getDeviceConfigurations().getAllConfigurations()).size(), analyzer.getDeviceConfigurations().getAllConfigurations().size()));
	}
}
