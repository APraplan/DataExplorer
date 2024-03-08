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

    Copyright (c) 2018 Thomas Eickert
****************************************************************************************/

package gde.histo.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import gde.Analyzer;
import gde.DataAccess;
import gde.GDE;
import gde.TestAnalyzer;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.log.LogFormatter;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import junit.framework.TestCase;

/**
 * Provide the DataExplorer settings without integrated UI.
 * @author Thomas Eickert (USER)
 */
public class NonUiTestCase extends TestCase {
	protected Logger									rootLogger;

	protected HashMap<String, String>	legacyDeviceNames	= new HashMap<String, String>(2);

	protected enum DataSource {
		SETTINGS {
			@Override
			public Path getDataPath(String subPath) {
				return Paths.get(Settings.getInstance().getDataFilePath()).resolve(subPath);
			}
		},
		TESTDATA {
			@Override
			public Path getDataPath(String subPath) {
				String srcDataPath = getLoaderPath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
				if (srcDataPath.endsWith("bin/")) { // running inside eclipse
					srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf(GDE.NAME_LONG)) + "DataFilesTestSamples/" + GDE.NAME_LONG;
				} else if (srcDataPath.indexOf("classes") > -1) { // ET running inside eclipse in Debug mode
					srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf(GDE.NAME_LONG)) + "DataFilesTestSamples/" + GDE.NAME_LONG;
				} else {
					srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf("build")) + "DataFilesTestSamples/" + GDE.NAME_LONG;
				}
				// return Paths.get(srcDataPath).resolve(subPath); Error because of leading slash:
				// /C:/Users/USER/git/dataexplorer/DataFilesTestSamples/DataExplorer // this.dataPath = Paths.get(srcDataPath).resolve(subPath).toFile();
				return (new File(srcDataPath)).toPath().resolve(subPath);
			}
		},
		INDIVIDUAL {
			@Override
			public Path getDataPath(String subPath) {
				return Paths.get(subPath);
			}
		};

		public abstract Path getDataPath(String string);
	}

	protected final TestAnalyzer	analyzer		= (TestAnalyzer) Analyzer.getInstance();

	protected final Settings			settings		= Settings.getInstance();
	protected final DataAccess		dataAccess	= DataAccess.getInstance();
	protected final String				tmpDir			= System.getProperty("java.io.tmpdir").endsWith(GDE.FILE_SEPARATOR)		//
			? System.getProperty("java.io.tmpdir") : System.getProperty("java.io.tmpdir") + GDE.FILE_SEPARATOR;

	Handler												ch					= new ConsoleHandler();
	LogFormatter									lf					= new LogFormatter();

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.rootLogger = Logger.getLogger("");
		// clean up all handlers from outside
		Handler[] handlers = this.rootLogger.getHandlers();
		for (Handler handler : handlers) {
			this.rootLogger.removeHandler(handler);
		}
		this.rootLogger.setLevel(Level.WARNING); // applies to test method logging
		this.rootLogger.addHandler(this.ch);
		this.ch.setFormatter(this.lf);
		this.ch.setLevel(Level.FINER); // applies to console logging in the classes to be tested

		Thread.currentThread().setContextClassLoader(GDE.getClassLoader());

		this.initialize();

		// add this two renamed device plug-ins to the list of legacy devices
		this.legacyDeviceNames.put("GPSLogger", "GPS-Logger");
		this.legacyDeviceNames.put("QuadroControl", "QC-Copter");
		this.legacyDeviceNames.put("PichlerP60", "PichlerP60 50W");
	}

	/**
	 * Goes through the existing device properties files and set active flagged devices into active devices list.
	 */
	public void initialize() throws FileNotFoundException {
		this.settings.setDataFilePath(DataSource.TESTDATA.getDataPath("_ET_Exzerpt").toString());

		// set histo settings now because SearchImportPath might influence the object list
		DataExplorer.getInstance().setHisto(true);
		setHistoSettings();

		String deviceoriented = Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0];
		this.settings.setObjectList(settings.getObjectList(), Arrays.asList(settings.getObjectList()).indexOf(deviceoriented));

		this.settings.setPartialDataTable(false);
		this.settings.setTimeFormat("relativ");
	}

	/**
	 *
	 */
	protected void setHistoSettings() {
		// wait until schema is setup
		this.settings.joinXsdThread();

		// the next lines only hold settings which do not control the GUI appearance
		this.settings.setSearchDataPathImports(true);
		this.settings.setChannelMix(false);
		this.settings.setSamplingTimespan_ms("2"); // this index corresponds to 1 sec
		this.settings.setIgnoreLogObjectKey(true);
		this.settings.setRetrospectMonths("240"); // this is the current maximum value
		this.settings.setXmlCache(true);
		this.settings.setZippedCache(false);
		this.settings.setAbsoluteTransitionLevel("999"); // results in default value
		this.settings.setAbsoluteTransitionLevel("999"); // results in default value
		this.settings.setSuppressMode(false);
		this.settings.setSubDirectoryLevelMax("5");
		this.settings.setCanonicalQuantiles(false);
		this.settings.setSymmetricToleranceInterval(false);
		this.settings.setOutlierToleranceSpread("99999");
		this.settings.setSourceFileListenerActive(false);
		this.settings.setDataFoldersCsv("");
		this.settings.setImportFoldersCsv("");
		this.settings.setMirrorSourceFoldersCsv("");
		System.out.println("done");
	}

	/**
	 * @return the path where the class GDE gets loaded
	 */
	protected static String getLoaderPath() {
		return GDE.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	}

	/**
	 * Emulate user GUI settings required for history tabs.
	 * @param fileDeviceName
	 * @param activeChannelNumber
	 * @param activeObjectKey is an object key which exists in the settings or an empty string for deviceoriented
	 */
	protected void setDeviceChannelObject(String fileDeviceName, int activeChannelNumber, String activeObjectKey) {
		// device : from setDevice
		if (this.legacyDeviceNames.get(fileDeviceName) != null) fileDeviceName = this.legacyDeviceNames.get(fileDeviceName);
		if (fileDeviceName.toLowerCase().contains("charger308duo") || fileDeviceName.toLowerCase().contains("charger308duo")) {
			System.out.println("skip fileDeviceName=" + fileDeviceName);
		}
		DeviceConfiguration deviceConfig = analyzer.getDeviceConfigurations().get(fileDeviceName);
		if (deviceConfig == null) throw new UnsupportedOperationException("deviceConfig == null");

		this.analyzer.setArena(deviceConfig.getAsDevice(), activeChannelNumber, activeObjectKey);
	}

}
