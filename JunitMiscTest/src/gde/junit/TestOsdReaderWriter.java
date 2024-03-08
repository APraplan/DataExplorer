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
****************************************************************************************/
package gde.junit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import gde.GDE;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.io.CSVReaderWriter;
import gde.io.OsdReaderWriter;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;

public class TestOsdReaderWriter extends TestSuperClass {

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * test reading OSD files %TEMP%\Write_1_OSD and writes OSD files to %TEMP%\Write_2_OSD
	 * all files must identical except time stamp
	 */
	public final void testOsdReaderCsvWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			if (!new File(this.tmpDir1).exists())
				throw new FileNotFoundException(this.tmpDir1);

			List<File> files = FileUtils.getFileListing(new File(this.tmpDir1), 1);

			for (File file : files) {
				String filePath = file.getAbsolutePath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
				if (filePath.toLowerCase().endsWith(".osd")) {
					try {
						if (filePath.equals(OperatingSystemHelper.getLinkContainedFilePath(filePath))) {
							HashMap<String, String> fileHeader = OsdReaderWriter.getHeader(file.getAbsolutePath());
							String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
							if (fileDeviceName.contains("Weatronic"))
								continue; //TODO fix Weatronic CSV export
							System.out.println(fileDeviceName + " working with : " + file);
							DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
							IDevice device = this.getInstanceOfDevice(deviceConfig);
							this.analyzer.setActiveDevice(device);

							setupDataChannels(device);

							OsdReaderWriter.read(file.getAbsolutePath());

							Channel activeChannel = this.channels.getActiveChannel();
							activeChannel.setFileName(file.getAbsolutePath());
							activeChannel.setFileDescription(fileHeader.get(GDE.FILE_COMMENT));
							activeChannel.setSaved(true);
							activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

							for (String recordSetName : activeChannel.getRecordSetNames()) {
								RecordSet recordSet = activeChannel.get(recordSetName);
								if (recordSet != null) {
									activeChannel.setActiveRecordSet(recordSet);
									activeChannel.getActiveRecordSet().updateVisibleAndDisplayableRecordsForTable();
									//device.makeInActiveDisplayable(recordSet);
									drawCurves(recordSet, 1024, 768);
									
									if (recordSet.getDisplayRecords().size() > 0 && recordSet.getRecordDataSize(true) > 2 && (recordSet.getTime_ms(0) > recordSet.getMaxTime_ms() || recordSet.getAverageTimeStep_ms() <= 0))
										throw new Exception("time error reading recordSet " + recordSet.getName());
								}
							}

							if (activeChannel.getActiveRecordSet() != null) {
								String absolutFilePath = this.tmpDir1 + file.getName();
								if (absolutFilePath.contains("_lov")) //exclude LogView files
									continue;
								absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_abs.csv";
								System.out.println("writing as   : " + absolutFilePath);
								CSVReaderWriter.write(';', activeChannel.getActiveRecordSet().getName(), absolutFilePath, false, "UTF-8");
								absolutFilePath = this.tmpDir1 + file.getName();
								absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_raw.csv";
								System.out.println("writing as   : " + absolutFilePath);
								CSVReaderWriter.write(';', activeChannel.getActiveRecordSet().getName(), absolutFilePath, true, "ISO-8859-1");
							}
						}
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading OSD files from directories used by GDE application and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures,
	 * the written files might different due to code updates (add/change properties)
	 */
	public final void testOsdReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				String filePath = file.getAbsolutePath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
				if (filePath.toLowerCase().endsWith(".osd") && !filePath.contains("Av4ms_FV_x69")) {
					try {
						if (filePath.equals(OperatingSystemHelper.getLinkContainedFilePath(filePath))) {
							HashMap<String, String> fileHeader = OsdReaderWriter.getHeader(file.getAbsolutePath());
							String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
							if(this.legacyDeviceNames.get(fileDeviceName) != null)
								fileDeviceName = this.legacyDeviceNames.get(fileDeviceName);
							if (fileDeviceName.toLowerCase().contains("hottviewer") || fileDeviceName.toLowerCase().contains("mpu") || fileDeviceName.contains("HoTTAdapter3") || fileDeviceName.contains("HoTTAdapterD"))
								continue;
							
							fileDeviceName = fileDeviceName.contains("(") ? fileDeviceName.split(" ")[0] : fileDeviceName; //(GPS-Logger (UL)
							DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
							if (deviceConfig == null) 
								continue;
							
							IDevice device = this.getInstanceOfDevice(deviceConfig);
							this.analyzer.setActiveDevice(device);
							System.out.println(fileDeviceName + ": working with : " + file);

							setupDataChannels(device);

							OsdReaderWriter.read(file.getAbsolutePath());

							Channel activeChannel = this.channels.getActiveChannel();
							activeChannel.setFileName(file.getAbsolutePath());
							activeChannel.setFileDescription(fileHeader.get(GDE.FILE_COMMENT));
							activeChannel.setSaved(true);
							activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

							for (String recordSetName : activeChannel.getRecordSetNames()) {
								RecordSet recordSet = activeChannel.get(recordSetName);
								if (recordSet != null) {
									if (!recordSet.hasDisplayableData()) recordSet.loadFileData(activeChannel.getFullQualifiedFileName(), false);
									activeChannel.setActiveRecordSet(recordSet);
									if (fileDeviceName.startsWith("HoTTAdapter2")) {
										device.makeInActiveDisplayable(recordSet);
										//if (recordSet.get(8) != null) //8=VoltageRxMin
										//	System.out.println(recordSet.get(8).getName() + " isActive = " + recordSet.get(8).isActive());
									}
									drawCurves(recordSet, 1024, 768);
									
									if (recordSet.getDisplayRecords().size() > 0 && recordSet.getRecordDataSize(true) > 2 && (recordSet.getTime_ms(0) > recordSet.getMaxTime_ms() || recordSet.getAverageTimeStep_ms() <= 0))
										throw new Exception("time error reading recordSet " + recordSet.getName());
								}
							}

							if (!new File(this.tmpDir1).exists())
								throw new FileNotFoundException(this.tmpDir1);

							String absolutFilePath = this.tmpDir1 + file.getName();
							System.out.println("writing as   : " + absolutFilePath);
							OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
						}
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading OSD files %TEMP%\Write_1_OSD and writes OSD files to %TEMP%\Write_2_OSD
	 * all files must identical except time stamp
	 */
	public final void testReaderWriterOsd() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			if (!new File(this.tmpDir1).exists())
				throw new FileNotFoundException(this.tmpDir1);

			List<File> files = FileUtils.getFileListing(new File(this.tmpDir1), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".osd")) {
//					if (!file.getAbsolutePath().contains("2013-04-13_Cappuccino_BT_Vergleich_PLoss"))
//						continue;
					try {
						HashMap<String, String> fileHeader = OsdReaderWriter.getHeader(file.getAbsolutePath());
						String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
						if (fileDeviceName.toLowerCase().contains("gpx"))
							continue;
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);
						System.out.println(fileDeviceName + ": working with : " + file);

						setupDataChannels(device);

						OsdReaderWriter.read(file.getAbsolutePath());

						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(fileHeader.get(GDE.FILE_COMMENT));
						activeChannel.setSaved(true);
						activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

						for (String recordSetName : activeChannel.getRecordSetNames()) {
							RecordSet recordSet = activeChannel.get(recordSetName);
							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								if (fileDeviceName.startsWith("HoTTAdapter2")) {
									device.makeInActiveDisplayable(recordSet);
									if (recordSet.get(8) != null) //8=VoltageRxMin
										System.out.println(recordSet.get(8).getName() + " isActive = " + recordSet.get(8).isActive());
								}
								drawCurves(recordSet, 1024, 768);
								
								if (recordSet.getDisplayRecords().size() > 0 && recordSet.getRecordDataSize(true) > 2 && (recordSet.getTime_ms(0) > recordSet.getMaxTime_ms() || recordSet.getAverageTimeStep_ms() <= 0))
									throw new Exception("time error reading recordSet " + recordSet.getName());
							}
						}

						new File(this.tmpDir2).mkdirs();
						String absolutFilePath = this.tmpDir2 + file.getName();
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * Test reading OSD example files headers.
	 */
	public final void testInputStreamOsdReader() {
		this.setDataPath(); // set the dataPath variable
		Path compressedOsd = this.dataPath.toPath().resolve("_ET_Exzerpt/HoTTAdapter2/2016-08-22_Sharon Pro E all configs.osd");
		Path uncompressedOsd = this.dataPath.toPath().resolve("_ET_Exzerpt/UltraTrioPlus14/2015-06-12_4NiMH_E_Laden.osd");

		try (InputStream compressedInputStream = new FileInputStream(compressedOsd.toFile()); //
				InputStream uncompressedInputStream = new FileInputStream(uncompressedOsd.toFile());) {
			HashMap<String, String> compressedHeader = OsdReaderWriter.getHeader(compressedInputStream);
			assertFalse("no header entries found for " + compressedOsd.toString(), compressedHeader.isEmpty());

			HashMap<String, String> uncompressedHeader = OsdReaderWriter.getHeader(uncompressedInputStream);
			assertFalse("no header entries found for " + uncompressedOsd.toString(), uncompressedHeader.isEmpty());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
