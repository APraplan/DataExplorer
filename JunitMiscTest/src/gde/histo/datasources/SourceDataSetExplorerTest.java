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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import gde.histo.base.NonUiTestCase;
import gde.histo.datasources.HistoSet.RebuildStep;

/**
 *
 * @author Thomas Eickert (USER)
 */
class SourceDataSetExplorerTest extends NonUiTestCase {
	private final static String	$CLASS_NAME	= SourceDataSetExplorerTest.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	/**
	 * Take folders from DirectoryScanner, determine files and create trusses.
	 * For osd files and native files (if supported by the device).
	 */
	@ParameterizedTest
	@CsvSource({ //
			"HoTTAdapter, 4, true", //
			"UltraTrioPlus14, 6, true", //
			"UltraTrioPlus14, 2, false" })
	public void testDefineTrusses(String deviceName, int numberOfDirectories, boolean isChannelMix) {
		settings.setChannelMix(isChannelMix);
		setDeviceChannelObject(deviceName, 3, "");

		DirectoryScanner directoryScanner = new DirectoryScanner(analyzer);
		try {
			directoryScanner.isValidated(RebuildStep.F_FILE_CHECK);
			log.log(Level.FINE, directoryScanner.getSourceFolders().getDecoratedPathsCsv());
			SourceDataSetExplorer sourceDataSetExplorer = new SourceDataSetExplorer(analyzer, directoryScanner.getSourceFolders(), false);
			boolean reReadFiles = !directoryScanner.isChannelChangeOnly();
			sourceDataSetExplorer.screen4Trusses(directoryScanner.getSourceFolders().getMap(), reReadFiles);

			// this is the actual test
			Assertions.assertEquals(numberOfDirectories, sourceDataSetExplorer.getTrusses().size(), "number of trusses");
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("directory not accessible");
		}
	}

	/**
	 * Take folders from DirectoryScanner, determine files and create trusses.
	 * Channel mix is supported for loaders with identical channel configurations.
	 */
	@ParameterizedTest
	@CsvSource({ //
			"HoTTAdapter, 4", //
			"UltraTrioPlus14, 6" })
	public void testDefineTrussesWithChannelMix(String deviceName, int numberOfDirectories) {
		settings.setChannelMix(true);

		setDeviceChannelObject(deviceName, 3, "");

		DirectoryScanner directoryScanner = new DirectoryScanner(analyzer);
		try {
			directoryScanner.isValidated(RebuildStep.F_FILE_CHECK);
			SourceDataSetExplorer sourceDataSetExplorer = new SourceDataSetExplorer(analyzer, directoryScanner.getSourceFolders(), false);
			boolean reReadFiles = !directoryScanner.isChannelChangeOnly();
			sourceDataSetExplorer.screen4Trusses(directoryScanner.getSourceFolders().getMap(), reReadFiles);

			// this is the actual test
			Assertions.assertEquals(numberOfDirectories, sourceDataSetExplorer.getTrusses().size(), "number of trusses");
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("directory not accessible");
		}
	}
}
