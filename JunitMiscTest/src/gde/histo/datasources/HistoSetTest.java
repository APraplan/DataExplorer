/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
									2016,2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.histo.datasources;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import gde.DataAccess;
import gde.DataAccess.LocalAccess;
import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.histo.base.NonUiTestCase;
import gde.histo.datasources.HistoSet.RebuildStep;
import gde.utils.FileUtils;
import gde.utils.ObjectKeyCompliance;

public class HistoSetTest extends NonUiTestCase {
	private final static String	$CLASS_NAME	= HistoSetTest.class.getName();
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
	 * ET 09.2018: elapsed times for DataFilesTestSamples/DataExplorer/_ET_Exzerpt with  182 files resulting in   406 vaults  @26 object keys and isZippedCache=false
	 *   run 1  w/o vault cache 192 sec (8,5 MiB cache JSON)
	 *   run 2 with vault cache 111 sec (8,5 MiB cache JSON)
	 * ET 09.2018: elapsed times for DataFilesTestSamples/DataExplorer/_ET_Exzerpt with  182 files resulting in   406 vaults  @26 object keys and isZippedCache=false
	 *   run 1  w/o vault cache 196 sec (12,0 MiB cache)
	 *   run 2 with vault cache 102 sec (12,0 MiB cache)
	 * ET 08.2018: elapsed times for DataFilesTestSamples/DataExplorer/_ET_Exzerpt with  182 files resulting in   491 vaults  @26 object keys and isZippedCache=false
	 *   run 1  w/o vault cache 237 sec (16,0 MiB cache)
	 *   run 2 with vault cache 100 sec (16,0 MiB cache)
	 * ET 04.2017: elapsed times for DataFilesTestSamples/DataExplorer/_ET_Exzerpt with 171 files resulting in 1.645 vaults @26 object keys and isZippedCache=false/true
	 * - run 1 w/o vault cache 298/309 sec (15,4/1,8 MiB cache)
	 * - run 2 with vault cache 165/161 sec (15,4/1,8 MiB cache)
	 * ET 04.2017: elapsed times for DataFilesTestSamples/DataExplorer/_ET_Exzerpt with 171 files resulting in 926 vaults @26 object keys and isZippedCache=false/true (910 vault w/o duplicates)
	 * - run 1 w/o vault cache 372 sec (17,2 MiB cache)
	 * - run 2 with vault cache 231 sec (17,2 MiB cache)
	 */
	@Tag("performance")
	@Test
	public void testHistoSet4All() {
		this.settings.setSearchDataPathImports(true);

		System.out.println(String.format("Max Memory=%,11d   Total Memory=%,11d   Free Memory=%,11d", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().totalMemory(), Runtime.getRuntime().freeMemory()));
		TreeMap<String, Exception> failures = new TreeMap<String, Exception>();

		assertTrue(FileUtils.checkDirectoryExist(settings.getDataFilePath()));

		// take the non-device directories in the data path as object keys
		Set<String> objectKeys = ObjectKeyCompliance.defineObjectKeyCandidates(analyzer.getDeviceConfigurations().getAllConfigurations());
		objectKeys.add(""); // empty string for 'device oriented'
		this.settings.setObjectList(objectKeys.toArray(new String[0]), 0);

		HistoSet histoSet = new HistoSet();

		List<Long> elapsed_sec = new ArrayList<>();
		long totalVaultsCount = 0;
		((LocalAccess) DataAccess.getInstance()).resetHistoCache();
		for (int i = 0; i < 2; i++) { // run twice for cache effects measuring
			long nanoTime = System.nanoTime();
			try {
				totalVaultsCount = 0;
				for (Entry<String, DeviceConfiguration> deviceEntry : analyzer.getDeviceConfigurations().getAllConfigurations().entrySet()) {
					IDevice device = setDevice(deviceEntry.getKey());
					if (device != null) {
						System.out.println(device.getName());
						for (String objectKey : objectKeys) {
							for (int j = 1; j <= device.getChannelCount(); j++) {
								this.analyzer.setArena(device, j, objectKey);

								histoSet.rebuild4Screening(RebuildStep.A_HISTOSET);
								int tmpSize = histoSet.getTrailRecordSet().getTimeStepSize();
								if (tmpSize > 0) {
									System.out.println(String.format("%40.44s channelNumber=%2d  histoSetSize==%,11d", //
											objectKey, j, tmpSize));
								}
								totalVaultsCount += tmpSize;
							}
						}
					}
				}
				elapsed_sec.add(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - nanoTime));
			} catch (Exception e) {
				e.printStackTrace();
				fail(e.toString());
			}
		}

		Collection<String> validLogExtentions = this.settings.getSearchDataPathImports() ? analyzer.getDeviceConfigurations().getValidLogExtentions()
				: Arrays.asList(new String[] { GDE.FILE_ENDING_DOT_OSD });
		long logFilesCount = getLogFilesCount(new File(settings.getDataFilePath()), 99, validLogExtentions);
		long cacheSize = analyzer.getDataAccess().getCacheSize();
		System.out.println(String.format("* elapsed times for %s with%,5d files resulting in%,6d vaults  @%d object keys and isZippedCache=%b", //
				settings.getDataFilePath(), logFilesCount, totalVaultsCount, objectKeys.size(), settings.isZippedCache()));
		for (int j = 0; j < elapsed_sec.size(); j++) {
			System.out.println(String.format("*   run %d %s vault cache %d sec (%.1f MiB cache)", //
					j + 1, j == 0 ? " w/o" : "with", elapsed_sec.get(j), cacheSize / 1024. / 1024.));
		}

		StringBuilder sb = new StringBuilder();
		for (Entry<String, Exception> failure : failures.entrySet()) {
			sb.append(failure).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * Fills the histoset by reading all logs into all devices and all channels.
	 * uses cache data in a 2nd internal loop.
	 * ET 12.2016: Win PC elapsed times for DataFilesTestSamples\DataExplorer with 144 files resulting in 223 vaults (using a zipped cache):
	 * - 1st run w/o vault cache 72 sec
	 * - 2nd run with vault cache 23 sec
	 * ET 10.2017: Win PC elapsed times for DataFilesTestSamples\DataExplorer with 291 files resulting in 691 vaults (using a UN/zipped cache)
	 * including 73 bin files:
	 * - 1st run w/o vault cache 230/225 sec (20,7 / 2,49 MiB cache)
	 * - 2nd run with vault cache 56/56 sec
	 * ET 03.2018: Win PC elapsed times for DataFilesTestSamples\DataExplorer\_ET_Exzerpt with 139 files resulting in 382 vaults (using a
	 * UN/zipped cache) including 58 bin files:
	 * - 1st run w/o vault cache 320/297 sec (25,1 / 3,16 MiB cache)
	 * - 2nd run with vault cache 64/65 sec
	 * ET 03.2018: Win PC elapsed times for DataFilesTestSamples\DataExplorer\_ET_Exzerpt with 139 files resulting in 382 vaults (using a
	 * UN/zipped cache) including 58 bin files:
	 * - 1st run w/o vault cache 238/290 sec (13,3 / 1,62 MiB cache)
	 * - 2nd run with vault cache 65/68 sec
	 * ET 04.2018: Win PC elapsed times for DataFilesTestSamples\DataExplorer\_ET_Exzerpt with 175 files resulting in 475 vaults (using a
	 * UN/zipped cache) including 58 bin files: @46 object keys
	 * - 1st run w/o vault cache 559/? sec (15,1 / ? MiB cache)
	 * - 2nd run with vault cache 406/? sec
	 * ET 04.2018: Win PC elapsed times for DataFilesTestSamples/DataExplorer/_ET_Exzerpt with 175 files resulting in 475 vaults @26 object keys
	 * and isZippedCache=false
	 * - run 1 w/o vault cache 174 sec (15,1 MiB cache)
	 * - run 2 with vault cache 38 sec (15,1 MiB cache)
	 */
	@Deprecated
	public void testBuildHistoSet4TestSamples() {
	}

	/**
	 * HoTTAdapter only for all channels.
	 * ET 05.2018: elapsed times for C:/Users/USER/git/dataexplorer/DataFilesTestSamples/DataExplorer/_ET_Exzerpt with 174 files resulting in
	 * 129 vaults @25 object keys and isZippedCache=false
	 * - run 1 w/o vault cache 43 sec (3,1 MiB cache)
	 * - run 2 with vault cache 6 sec (3,1 MiB cache)
	 * HoTTAdapter2 elapsed times for C:/Users/USER/git/dataexplorer/DataFilesTestSamples/DataExplorer/_ET_Exzerpt with 174 files resulting in
	 * 50 vaults @25 object keys and isZippedCache=false
	 * run 1 w/o vault cache 18 sec (1,8 MiB cache)
	 * run 2 with vault cache 7 sec (1,8 MiB cache)
	 */
	@Tag("performance")
	@ParameterizedTest
	@CsvSource({ //
			"HoTTAdapter, 129", //
			"HoTTAdapter2, 222" })
	public void testHistoSet4HoTTAdapter(String deviceName, int numberOfVaults) {
		this.settings.setSearchDataPathImports(true);

		System.out.println(String.format("Max Memory=%,11d   Total Memory=%,11d   Free Memory=%,11d", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().totalMemory(), Runtime.getRuntime().freeMemory()));
		TreeMap<String, Exception> failures = new TreeMap<String, Exception>();

		assertTrue(FileUtils.checkDirectoryExist(settings.getDataFilePath()));

		// take the non-device directories in the data path as object keys
		Set<String> objectKeys = ObjectKeyCompliance.defineObjectKeyCandidates(analyzer.getDeviceConfigurations().getAllConfigurations());
		objectKeys.add(""); // empty string for 'device oriented'
		this.settings.setObjectList(objectKeys.toArray(new String[0]), 0);

		HistoSet histoSet = new HistoSet();

		List<Long> elapsed_sec = new ArrayList<>();
		long totalVaultsCount = 0;
		((LocalAccess) DataAccess.getInstance()).resetHistoCache();
		for (int i = 0; i < 2; i++) { // run twice for cache effects measuring
			long nanoTime = System.nanoTime();
			try {
				totalVaultsCount = 0;
				IDevice device = setDevice(deviceName);
				if (device != null) {
					System.out.println(device.getName());
					for (String objectKey : objectKeys) {
						for (int j = 1; j <= device.getChannelCount(); j++) {
							this.analyzer.setArena(device, j, objectKey);

							histoSet.rebuild4Screening(RebuildStep.A_HISTOSET);
							int tmpSize = histoSet.getTrailRecordSet().getTimeStepSize();
							if (tmpSize > 0) {
								System.out.println(String.format("%40.44s channelNumber=%2d  histoSetSize==%,11d", //
										objectKey, j, tmpSize));
							}
							totalVaultsCount += tmpSize;
						}
					}
				}
				elapsed_sec.add(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - nanoTime));
			} catch (Exception e) {
				e.printStackTrace();
				fail(e.toString());
			}
		}

		Collection<String> validLogExtentions = this.settings.getSearchDataPathImports() ? analyzer.getDeviceConfigurations().getValidLogExtentions()
				: Arrays.asList(new String[] { GDE.FILE_ENDING_DOT_OSD });
		long logFilesCount = getLogFilesCount(new File(settings.getDataFilePath()), 99, validLogExtentions);
		long cacheSize = analyzer.getDataAccess().getCacheSize();
		System.out.println(String.format("* " + deviceName + " elapsed times for %s with%,5d files resulting in%,6d vaults  @%d object keys and isZippedCache=%b", //
				settings.getDataFilePath(), logFilesCount, totalVaultsCount, objectKeys.size(), settings.isZippedCache()));
		for (int j = 0; j < elapsed_sec.size(); j++) {
			System.out.println(String.format("*   run %d %s vault cache %d sec (%.1f MiB cache)", //
					j + 1, j == 0 ? " w/o" : "with", elapsed_sec.get(j), cacheSize / 1024. / 1024.));
		}

		StringBuilder sb = new StringBuilder();
		for (Entry<String, Exception> failure : failures.entrySet()) {
			sb.append(failure).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	public void buildHistoSet4OneHoTTObject(String deviceName, String objectKey) {
		this.settings.setSearchDataPathImports(true);

		assertTrue(FileUtils.checkDirectoryExist(settings.getDataFilePath()));

		// take the non-device directories in the data path as object keys
		Set<String> objectKeys = ObjectKeyCompliance.defineObjectKeyCandidates(analyzer.getDeviceConfigurations().getAllConfigurations());
		objectKeys.add(""); // empty string for 'device oriented'
		this.settings.setObjectList(objectKeys.toArray(new String[0]), 0);

		HistoSet histoSet = new HistoSet();

		try {
			IDevice device = setDevice(deviceName);
			for (int j = 1; j <= device.getChannelCount(); j++) {
				this.analyzer.setArena(device, j, objectKey);
				histoSet.rebuild4Screening(RebuildStep.A_HISTOSET);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	/**
	 * @param fileDeviceName
	 * @return
	 */
	private IDevice setDevice(String fileDeviceName) {
		String deviceName = fileDeviceName;
		if (this.legacyDeviceNames.get(fileDeviceName) != null) deviceName = this.legacyDeviceNames.get(fileDeviceName);
		if (deviceName.toLowerCase().contains("hottviewer") || deviceName.toLowerCase().contains("mpu")) return null; // iCharger308DUO
																																																									// gde.device.UsbPortType missing
		DeviceConfiguration deviceConfig = analyzer.getDeviceConfigurations().get(deviceName);
		IDevice device = null;
		if (deviceConfig != null) {
			try {
				device = deviceConfig.getAsDevice();
			} catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		return device;
	}

	/**
	 * Recursively walk a directory tree.
	 * @param rootDirectory
	 * @param recursionDepth specifies the depth of recursion cycles (0 means no recursion)
	 * @param extensions are the lowercase file extensions (e.g. '.bin')
	 * @return the number of files matching the {@code extensions}
	 */
	public static long getLogFilesCount(File rootDirectory, int recursionDepth, Collection<String> extensions) {
		long numberOfFiles = 0;
		if (rootDirectory.isDirectory() && rootDirectory.canRead()) {
			try (Stream<Path> paths = Files.walk(rootDirectory.toPath(), recursionDepth)) {
				numberOfFiles = paths.filter(p -> extensions.stream().anyMatch(p.toString().toLowerCase()::endsWith)) //
						.count();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return numberOfFiles;
	}

}
