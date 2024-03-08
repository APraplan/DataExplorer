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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.device.ardupilot.ArduPilotLogReader;
import gde.device.gpx.GPXDataReaderWriter;
import gde.device.graupner.GeniusWizardLogReader;
import gde.device.graupner.HoTTAdapter;
import gde.device.graupner.HoTTbinReader;
import gde.device.graupner.HoTTbinReader2;
import gde.device.graupner.HoTTlogReader;
import gde.device.graupner.HoTTlogReader2;
import gde.device.igc.IGCReaderWriter;
import gde.device.jeti.JetiAdapter;
import gde.device.jeti.JetiDataReader;
import gde.device.junsi.DataParserDuo;
import gde.device.junsi.iChargerUsb;
import gde.device.logview.ZeroReaderWriter;
import gde.device.skyrc.GplLogReader;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedException;
import gde.exception.NotSupportedFileFormatException;
import gde.io.CSVReaderWriter;
import gde.io.CSVSerialDataReaderWriter;
import gde.io.DataParser;
import gde.io.LogViewReader;
import gde.io.NMEAReaderWriter;
import gde.io.OsdReaderWriter;
import gde.utils.FileUtils;
import gde.utils.StringHelper;

public class TestFileReaderOsdWriter extends TestSuperClass {

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		Settings.getInstance().setPartialDataTable(true);
	}

	/**
	 * test reading CSV files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testCsvReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".csv")
						&& !(file.getPath().toLowerCase().contains("csv2serialadapter")
								|| file.getPath().toLowerCase().contains("gigalogger")
								|| file.getPath().toLowerCase().contains("tesla")
								|| file.getPath().toLowerCase().contains("snipe")
								|| file.getPath().toLowerCase().contains("mc3000")
								|| file.getPath().toLowerCase().contains("space pro")
								|| file.getPath().toLowerCase().contains("asw")
								|| file.getPath().toLowerCase().contains("ash")
								|| file.getPath().toLowerCase().contains("spektrum")
								|| file.getPath().toLowerCase().contains("av4ms")
								|| file.getPath().toLowerCase().contains("futaba")
								|| file.getPath().toLowerCase().contains("iisi")
								|| file.getPath().toLowerCase().contains("opentx")
								|| file.getPath().toLowerCase().contains("devo")
								|| file.getPath().toLowerCase().contains("akkumonitor")
								|| file.getPath().toLowerCase().contains("flightrecorder"))) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO-8859-1")); //$NON-NLS-1$
						String line = reader.readLine();
						boolean isCVS2SerialFormat = line.startsWith(deviceConfig.getDataBlockLeader()) && line.contains(deviceConfig.getDataBlockSeparator().value());
						reader.close();

						if (!isCVS2SerialFormat) {
							HashMap<String, String> fileHeader = CSVReaderWriter.getHeader(';', file.getAbsolutePath());
							String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
							deviceConfig = this.deviceConfigurations.get(fileDeviceName);
							if (deviceConfig == null) throw new NotSupportedException("device = " + fileDeviceName + " is not supported or in list of active devices");
							IDevice device = this.getInstanceOfDevice(deviceConfig);
							this.analyzer.setActiveDevice(device);

							setupDataChannels(device);
							fileHeader = CSVReaderWriter.evaluateType(';', fileHeader, deviceConfig);

							int channelConfigNumber = this.channels.getChannelNumber(fileHeader.get(GDE.CHANNEL_CONFIG_NAME));
							if (channelConfigNumber > device.getChannelCount()) {
								channelConfigNumber = 1;
								fileHeader.put(GDE.CHANNEL_CONFIG_NAME, this.channels.get(1).getChannelConfigKey());
							}
							this.channels.setActiveChannelNumber(channelConfigNumber);
							Channel activeChannel = this.channels.getActiveChannel();
							activeChannel.setFileName(file.getAbsolutePath());
							activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
							activeChannel.setSaved(true);

							RecordSet recordSet = CSVReaderWriter.read(';', file.getAbsolutePath(), "csv test", fileHeader.get(GDE.CSV_DATA_TYPE).equals(GDE.CSV_DATA_TYPE_RAW));

							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								activeChannel.applyTemplate(recordSet.getName(), true);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}
						else { // CSV2SerialAdapter file
							IDevice device = this.getInstanceOfDevice(deviceConfig);
							this.analyzer.setActiveDevice(device);

							setupDataChannels(device);

							this.channels.setActiveChannelNumber(1);
							Channel activeChannel = this.channels.getActiveChannel();
							activeChannel.setFileName(file.getAbsolutePath());
							activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
							activeChannel.setSaved(true);

							CSVSerialDataReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1,
									new DataParser(device.getDataBlockTimeUnitFactor(),
											device.getDataBlockLeader(), device.getDataBlockSeparator().value(),
											device.getDataBlockCheckSumType(), device.getDataBlockSize(InputTypes.FILE_IO)));
							RecordSet recordSet = activeChannel.getActiveRecordSet();

							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								activeChannel.applyTemplate(recordSet.getName(), true);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_cvs.osd";
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
	 * test reading CSV Futaba Telemetry files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testFutabaCsvReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".csv") && file.getPath().toLowerCase().contains("futaba")) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = "Futaba-Telemetry";
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
						activeChannel.setSaved(true);

						RecordSet recordSet = gde.device.robbe.CSVReaderWriter.read(';', file.getAbsolutePath(), "csv test");

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_cvs.osd";
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
	 * test reading Junsi iCharger *.txt log files files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testJunsiCsvReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".txt") && file.getPath().toLowerCase().contains("icharger")) {
					System.out.println("working with : " + file);

					try {
						String absolutePath = file.getAbsolutePath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
						String deviceName = absolutePath.substring(absolutePath.indexOf("iCharger"), absolutePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX));
						System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) 
							throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
						activeChannel.setSaved(true);

						RecordSet recordSet = (device instanceof iChargerUsb) 
								?	CSVSerialDataReaderWriter.read(file.getAbsolutePath(), device, "test txt import", 1, 
										new  DataParserDuo(1, device.getDataBlockLeader(), device.getDataBlockSeparator().value(), null, null, 
												device.getNoneCalculationMeasurementNames(1, device.getMeasurementNames(1)).length, 
												device.getDataBlockFormat(InputTypes.FILE_IO), false, 2))							
								: CSVSerialDataReaderWriter.read(file.getAbsolutePath(), device, "test txt import", 1, 
										new  DataParserDuo(1, device.getDataBlockLeader(), device.getDataBlockSeparator().value(), null, null, 
												Math.abs(device.getDataBlockSize(InputTypes.FILE_IO)), 
												device.getDataBlockFormat(InputTypes.FILE_IO), false, 2));


						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_cvs.osd";
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
	 * test reading log files following the LogView OpenFormat Zero and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testLogViewZeroCsvReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".txt") && file.getPath().toLowerCase().contains("zero")) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = "ZeroAdapter";
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) 
							throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
						activeChannel.setSaved(true);

						RecordSet recordSet = ZeroReaderWriter.read(device.getDataBlockSeparator().value().charAt(0), 
								file.getAbsolutePath(), device.getRecordSetStemNameReplacement());


						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_cvs.osd";
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
	 * test reading CSV generic import files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testCsvImportOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".csv") 
						&& (file.getPath().toLowerCase().contains("tesla") || file.getPath().toLowerCase().contains("spektrum") || file.getPath().toLowerCase().contains("snipe"))) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().toLowerCase().contains("tesla") ? "Tesla" : file.getPath().toLowerCase().contains("spektrum") ? "SpektrumCSV" : "Snipe";
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
						activeChannel.setSaved(true);

						RecordSet recordSet = gde.device.csv.CSVReaderWriter.read(deviceConfig.getDataBlockSeparator().value().charAt(0), file.getAbsolutePath(), "csv test");

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_cvs.osd";
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
	 * test reading CSV OpenTx Telemetry files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testOpenTxCsvReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".csv") && file.getPath().toLowerCase().contains("opentx")) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = "OpenTx-Telemetry";
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
						activeChannel.setSaved(true);

						RecordSet recordSet = gde.device.opentx.CSVReaderWriter.read(device.getDataBlockSeparator().value().charAt(0), file.getAbsolutePath(), "csv test");
						device.updateVisibilityStatus(recordSet, true);

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_cvs.osd";
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
	 * test reading CSV Devo Telemetry files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testDevoCsvReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".csv") && file.getPath().toLowerCase().contains("devo")) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = "Devo-Telemetry";
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
						activeChannel.setSaved(true);

						RecordSet recordSet = gde.device.devention.CSVReaderWriter.read(',', file.getAbsolutePath(), "csv test");

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_cvs.osd";
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
	 * test reading CSV Futaba Telemetry files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testIISI_CockpitCsvReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".csv") && file.getPath().toLowerCase().contains("iisi")) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = "IISI-Cockpit V2";
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
						activeChannel.setSaved(true);

						RecordSet recordSet = gde.device.isler.CSVReaderWriter.read(';', file.getAbsolutePath(), "csv test");

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_cvs.osd";
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
	 * test reading NMEA files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testNmeaReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".nmea")
						&& !(file.getPath().toLowerCase().contains("cappuccino")
						|| file.getPath().toLowerCase().contains("t-rex")
						|| file.getPath().toLowerCase().contains("space pro")
						|| file.getPath().toLowerCase().contains("asw")
						|| file.getPath().toLowerCase().contains("mue")
						|| file.getPath().toLowerCase().contains("foka")
						|| file.getPath().toLowerCase().contains("/gps-logger/") //GPS Logger 1 has binary setup sentence
						|| file.getPath().toLowerCase().contains("ash"))) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						// GPS-Logger and similar file
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from NMEA file");
						activeChannel.setSaved(true);

						NMEAReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1);
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_nmea.osd";
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
	 * test reading GPS-Logger 1 NMEA files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testGPSLogger1ReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".nmea")
						&& (file.getPath().toLowerCase().contains("/gps-logger/"))) { //GPS Logger 1 has binary setup sentence
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						// GPS-Logger and similar file
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from NMEA file");
						activeChannel.setSaved(true);

						NMEAReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1);
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_nmea.osd";
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
	 * test reading GPS-Logger2 NMEA files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testGPSLogger2ReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".nmea")
						&& (file.getPath().toLowerCase().contains("/gps-logger2/"))) { 
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						// GPS-Logger and similar file
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from NMEA file");
						activeChannel.setSaved(true);

						NMEAReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1);
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_nmea.osd";
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
	 * test reading GPS-Logger3 NMEA files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testGPSLogger3ReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".nmea")
						&& (file.getPath().toLowerCase().contains("/gps-logger3/"))) { 
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						// GPS-Logger and similar file
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from NMEA file");
						activeChannel.setSaved(true);

						NMEAReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1);
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_nmea.osd";
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
	 * test reading GSM-015 GNSS Speed Meterr 3gpl files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testSkyRC_GPSLoggerReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".3gpl")
						&& (file.getPath().toLowerCase().contains("/skyrc_dynamite_gps-logger/"))) { 
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = "GSM-015 GNSS Speed Meter";
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						// GPS-Logger and similar file
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from 3gpl file");
						activeChannel.setSaved(true);

						RecordSet recordSet = GplLogReader.read(file.getAbsolutePath(), device, "RecordSet", 1);

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 5) + "_3gpl.osd";
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
	 * test reading Dynamite Passport GPS Speed Meter 3gpl files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testDynamite_GPSLoggerReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".3gpl")
						&& (file.getPath().toLowerCase().contains("/skyrc_dynamite_gps-logger/"))) { 
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = "Dynamite Passport GPS Speed Meter";
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						// GPS-Logger and similar file
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from 3gpl file");
						activeChannel.setSaved(true);

						RecordSet recordSet = GplLogReader.read(file.getAbsolutePath(), device, "RecordSet", 1);

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 5) + "_3gpl.osd";
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
	 * test reading UniLog2 NMEA files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testUniLog2NmeaReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(new File(this.dataPath.getAbsolutePath() + "/UniLog2/"), 0);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".txt")) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						// GPS-Logger and similar file
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from TXT file");
						activeChannel.setSaved(true);

						NMEAReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1);
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_txt.osd";
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
	 * test reading UniLog2 NMEA files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testMpxFlightRecorderNmeaReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(new File(this.dataPath.getAbsolutePath() + "/FlightRecorder/"), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".csv")) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						// GPS-Logger and similar file
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from TXT file");
						activeChannel.setSaved(true);

						CSVSerialDataReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1,
								new DataParser(device.getDataBlockTimeUnitFactor(),
										device.getDataBlockLeader(), device.getDataBlockSeparator().value(),
										device.getDataBlockCheckSumType(), device.getDataBlockSize(InputTypes.FILE_IO)));
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_csv.osd";
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
	 * test reading LOV files from LogView application directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testLovReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			LogViewReader.putDeviceMap("pulsar 3", "Pulsar3"); //add Pulsar3 since it adds its entry by plugin jar
			LogViewReader.putDeviceMap("junsi icharger 106b+", "iCharger106B"); //add iCharger106b since its adding entry by plugin
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".lov")
						&& !(file.getPath().toLowerCase().contains("spektrum"))
						&& !(file.getPath().toLowerCase().contains("duo"))) {
					System.out.println("working with : " + file);
					try {
						HashMap<String, String> fileHeader = LogViewReader.getHeader(file.getAbsolutePath());
						String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
						System.out.println("working with deviceName = " + fileDeviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						LogViewReader.read(file.getAbsolutePath());

						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(fileHeader.get(GDE.FILE_COMMENT));
						activeChannel.setSaved(true);
						//activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

						for (String recordSetName : activeChannel.getRecordSetNames()) {
							//System.out.println("start drawing curves : " + recordSetName);
							RecordSet recordSet = activeChannel.get(recordSetName);
							if (recordSet != null) {
								if (!recordSet.hasDisplayableData()) recordSet.loadFileData(activeChannel.getFullQualifiedFileName(), false);
								//System.out.println("loaded FileData : " + recordSetName);
								activeChannel.setActiveRecordSet(recordSet);
								activeChannel.applyTemplate(recordSetName, true);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_lov.osd";
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
	 * test reading GPX XML files from various application directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testGPXReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".gpx")) {
					System.out.println("working with : " + file);
					try {
						String deviceName = "GPXAdapter";
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						Channel activeChannel = this.channels.getActiveChannel();
						GPXDataReaderWriter.read(file.getAbsolutePath(), device, GDE.STRING_DOLLAR, activeChannel.getNumber());

						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setSaved(true);
						//activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_gpx.osd";
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
	}

	/**
	 * test reading GPX XML files from various application directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testIGCReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".igc")) {
					System.out.println("working with : " + file);
					try {
						String deviceName = "IGCAdapter";
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						Channel activeChannel = this.channels.getActiveChannel();
						IGCReaderWriter.read(file.getAbsolutePath(), device, GDE.STRING_DOLLAR, activeChannel.getNumber());

						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setSaved(true);
						//activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_igc.osd";
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
	 * test reading CSV files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testCSVAdapterWriterOsd() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			if (!new File(this.tmpDir1).exists())
				throw new FileNotFoundException(this.tmpDir1);

			List<File> files = FileUtils.getFileListing(new File(this.tmpDir1), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".csv") && file.getName().toLowerCase().startsWith("log")) {
					try {
						HashMap<String, String> fileHeader = CSVReaderWriter.getHeader(';',file.getAbsolutePath());
						String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);
						System.out.println(fileDeviceName + ": working with : " + file);

						setupDataChannels(device);

						CSVReaderWriter.read(';', file.getAbsolutePath(), ") Record", file.getAbsolutePath().endsWith("_raw.csv"));

						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(fileHeader.get(GDE.FILE_COMMENT));
						activeChannel.setSaved(true);
						activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

						for (String recordSetName : activeChannel.getRecordSetNames()) {
							RecordSet recordSet = activeChannel.get(recordSetName);
							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
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
	 * test reading CSV(.txt) JLog2 files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testJlog2TxtReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			this.setDataPath(); //set the dataPath variable
			List<File> files = FileUtils.getFileListing(new File(this.dataPath.getAbsolutePath() + "/JLog2/"), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".txt") && !file.getAbsolutePath().toLowerCase().contains("config")
						&& !file.getAbsolutePath().toLowerCase().contains("version") && !file.getAbsolutePath().toLowerCase().contains("info")) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO-8859-1")); //$NON-NLS-1$
						String line = reader.readLine();
						boolean isCVS2SerialFormat = line.startsWith(deviceConfig.getDataBlockLeader()) && line.contains(deviceConfig.getDataBlockSeparator().value());
						reader.close();

						if (!isCVS2SerialFormat) {
							HashMap<String, String> fileHeader = CSVReaderWriter.getHeader(';', file.getAbsolutePath());
							String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
							deviceConfig = this.deviceConfigurations.get(fileDeviceName);
							if (deviceConfig == null) throw new NotSupportedException("device = " + fileDeviceName + " is not supported or in list of active devices");
							IDevice device = this.getInstanceOfDevice(deviceConfig);
							this.analyzer.setActiveDevice(device);

							setupDataChannels(device);

							fileHeader = CSVReaderWriter.evaluateType(';', fileHeader, deviceConfig);

							int channelConfigNumber = this.channels.getChannelNumber(fileHeader.get(GDE.CHANNEL_CONFIG_NAME));
							if (channelConfigNumber > device.getChannelCount()) {
								channelConfigNumber = 1;
								fileHeader.put(GDE.CHANNEL_CONFIG_NAME, this.channels.get(1).getChannelConfigKey());
							}
							this.channels.setActiveChannelNumber(channelConfigNumber);
							Channel activeChannel = this.channels.getActiveChannel();
							activeChannel.setFileName(file.getAbsolutePath());
							activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
							activeChannel.setSaved(true);

							RecordSet recordSet = CSVReaderWriter.read(';', file.getAbsolutePath(), "csv test", fileHeader.get(GDE.CSV_DATA_TYPE).equals(GDE.CSV_DATA_TYPE_RAW));

							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								activeChannel.applyTemplate(recordSet.getName(), true);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}
						else { // CSV2SerialAdapter file
							IDevice device = this.getInstanceOfDevice(deviceConfig);
							this.analyzer.setActiveDevice(device);

							setupDataChannels(device);

							this.channels.setActiveChannelNumber(1);
							Channel activeChannel = this.channels.getActiveChannel();
							activeChannel.setFileName(file.getAbsolutePath());
							activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
							activeChannel.setSaved(true);

							CSVSerialDataReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1,
									new DataParser(device.getDataBlockTimeUnitFactor(),
											device.getDataBlockLeader(), device.getDataBlockSeparator().value(),
											device.getDataBlockCheckSumType(), device.getDataBlockSize(InputTypes.FILE_IO)));
							RecordSet recordSet = activeChannel.getActiveRecordSet();

							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								activeChannel.applyTemplate(recordSet.getName(), true);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_txt.osd";
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
	 * test reading CSV(.txt) S32 files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testS32TxtReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			this.setDataPath(); //set the dataPath variable
			List<File> files = FileUtils.getFileListing(new File(this.dataPath.getAbsolutePath() + "/S32/"), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".txt") && !file.getAbsolutePath().toLowerCase().contains("config")
						&& !file.getAbsolutePath().toLowerCase().contains("version") && !file.getAbsolutePath().toLowerCase().contains("info")) {
					System.out.println("working with : " + file);

					//System.out.println("file.getPath() = " + file.getPath());
					String deviceName = "S32";
					deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
					//System.out.println("deviceName = " + deviceName);
					DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
					if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						// CSV2SerialAdapter file
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
						activeChannel.setSaved(true);

						CSVSerialDataReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1,
								new DataParser(device.getDataBlockTimeUnitFactor(),
										device.getDataBlockLeader(), device.getDataBlockSeparator().value(),
										device.getDataBlockCheckSumType(), device.getDataBlockSize(InputTypes.FILE_IO)));
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_txt.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
				}
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}
		catch (IOException e) {
			e.printStackTrace();
			fail(e.toString());
		}
		catch (NotSupportedFileFormatException e) {
			e.printStackTrace();
		}
		catch (DataInconsitsentException e) {
			e.printStackTrace();
		}
		catch (DataTypeException e) {
			e.printStackTrace();
		}
		catch (NotSupportedException e) {
			e.printStackTrace();
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading CSV(.txt) S32 files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testS32_2TxtReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			this.setDataPath(); //set the dataPath variable
			List<File> files = FileUtils.getFileListing(new File(this.dataPath.getAbsolutePath() + "/S32/"), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".txt") && !file.getAbsolutePath().toLowerCase().contains("config")
						&& !file.getAbsolutePath().toLowerCase().contains("version") && !file.getAbsolutePath().toLowerCase().contains("info")) {
					System.out.println("working with : " + file);

					//System.out.println("file.getPath() = " + file.getPath());
					String deviceName = "S32_2";
					deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
					//System.out.println("deviceName = " + deviceName);
					DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
					if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						// CSV2SerialAdapter file
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
						activeChannel.setSaved(true);

						CSVSerialDataReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1,
								new DataParser(device.getDataBlockTimeUnitFactor(),
										device.getDataBlockLeader(), device.getDataBlockSeparator().value(),
										device.getDataBlockCheckSumType(), device.getDataBlockSize(InputTypes.FILE_IO)));
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_txt.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
				}
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}
		catch (IOException e) {
			e.printStackTrace();
			fail(e.toString());
		}
		catch (NotSupportedFileFormatException e) {
			e.printStackTrace();
		}
		catch (DataInconsitsentException e) {
			e.printStackTrace();
		}
		catch (DataTypeException e) {
			e.printStackTrace();
		}
		catch (NotSupportedException e) {
			e.printStackTrace();
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading CSV(.txt) JLog2 files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testKosmikDatReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			this.setDataPath(); //set the dataPath variable
			List<File> files = FileUtils.getFileListing(new File(this.dataPath.getAbsolutePath() + "/Kosmik/"), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".dat")) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(2);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
						activeChannel.setSaved(true);

						CSVSerialDataReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 2,
								new DataParser(device.getDataBlockTimeUnitFactor(),
										device.getDataBlockLeader(), device.getDataBlockSeparator().value(),
										device.getDataBlockCheckSumType(), device.getDataBlockSize(InputTypes.FILE_IO)));
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_dat.osd";
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
	 * test reading Graupner HoTT bin log files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testHoTTAdapterBinReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			String binDir = this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "HoTTAdapter" + GDE.FILE_SEPARATOR;
			List<File> files = FileUtils.getFileListing(new File(binDir), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".bin")) {
					System.out.println("working with : " + file);
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from bin log file");
						activeChannel.setSaved(true);

						HoTTbinReader.read(file.getAbsolutePath(), ((HoTTAdapter) device).getPickerParameters());
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_bin.osd";
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
	 * test reading Graupner HoTT bin log files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testHoTTAdapter2BinReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			String binDir = this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "HoTTAdapter" + GDE.FILE_SEPARATOR;
			List<File> files = FileUtils.getFileListing(new File(binDir), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".bin")) {
					System.out.println("working with : " + file);
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR)) + "2";
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from bin log file");
						activeChannel.setSaved(true);

						HoTTbinReader2.read(file.getAbsolutePath(), ((HoTTAdapter) device).getPickerParameters());
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_2bin.osd";
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
	 * test reading Graupner HoTT log files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testHoTTAdapterLogReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			boolean isHoTTAdapterProblemDirectory = new File(this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "HoTTAdapter_Problem").exists();
			String logDir = isHoTTAdapterProblemDirectory
				? this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "HoTTAdapter_Problem" + GDE.FILE_SEPARATOR
				: this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "HoTTAdapter" + GDE.FILE_SEPARATOR ;
			List<File> files = FileUtils.getFileListing(new File(logDir), 2);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".log")) {
					System.out.println("working with : " + file);
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = "HoTTAdapter";
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from bin log file");
						activeChannel.setSaved(true);

						try {
							HoTTlogReader.read(file.getAbsolutePath(), ((HoTTAdapter) device).getPickerParameters());
						}
						catch (DataTypeException e) {
							// ignore not supported log files
							System.out.println("====>>>> " + e.getMessage());
							continue;
						}
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_log.osd";
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
	 * test reading Graupner HoTT log files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testHoTTAdapter2LogReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			boolean isHoTTAdapterProblemDirectory = new File(this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "HoTTAdapter_Problem").exists();
			String logDir = isHoTTAdapterProblemDirectory
				? this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "HoTTAdapter_Problem" + GDE.FILE_SEPARATOR
				: this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "HoTTAdapter" + GDE.FILE_SEPARATOR ;
			List<File> files = FileUtils.getFileListing(new File(logDir), 2);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".log")) {
					System.out.println("working with : " + file);
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = "HoTTAdapter2";
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from bin log file");
						activeChannel.setSaved(true);

						try {
							HoTTlogReader2.read(file.getAbsolutePath(), ((HoTTAdapter) device).getPickerParameters());
						}
						catch (DataTypeException e) {
							// ignore not supported log files
							System.out.println("====>>>> " + e.getMessage());
							continue;
						}
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_2log.osd";
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
	 * test reading Graupner Genius Wizard log files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testGeniusWizardLogReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			String logDir = this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "GeniusWizard" + GDE.FILE_SEPARATOR;
			List<File> files = FileUtils.getFileListing(new File(logDir), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".log")) {
					System.out.println("working with : " + file);
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = "GeniusWizard";
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from log file");
						activeChannel.setSaved(true);

						try {
							GeniusWizardLogReader.read(file.getAbsolutePath());
						}
						catch (DataTypeException e) {
							// ignore not supported log files
							System.out.println("====>>>> " + e.getMessage());
							continue;
						}
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							device.updateVisibilityStatus(recordSet, true);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_bin.osd";
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
	 * test reading ArduPilot log files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testArduPilotLogReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			File logDir = new File(this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "ArduPilot");
			boolean isArduPilotDirectory = logDir.exists() && logDir.isDirectory();
			if (!isArduPilotDirectory) 
				return;
			List<File> files = FileUtils.getFileListing(logDir, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".log")) {
					System.out.println("working with : " + file);
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = "ArduPilot";
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from bin log file");
						activeChannel.setSaved(true);

						try {
							RecordSet activeRecordSet = ArduPilotLogReader.read(device.getDataBlockSeparator().value().charAt(0), file.getAbsolutePath(), device.getRecordSetStemNameReplacement());
							device.updateVisibilityStatus(activeRecordSet, true);
							activeRecordSet.descriptionAppendFilename(file.getAbsolutePath());
						}
						catch (DataTypeException e) {
							// ignore not supported log files
							System.out.println("====>>>> " + e.getMessage());
							continue;
						}
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_bin.osd";
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
	 * test reading Jeti *.log log files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 */
	public final void testJetiAdapterReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			this.setDataPath(); //set the dataPath variable
			List<File> files = FileUtils.getFileListing(new File(this.dataPath.getAbsolutePath() + "/JetiAdapter/"), 0);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG) || file.getAbsolutePath().toLowerCase().endsWith(GDE.FILE_ENDING_DOT_JML)) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1 + deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from LOG file");
						activeChannel.setSaved(true);

						JetiDataReader.read(file.getAbsolutePath(), (JetiAdapter) device, "RecordSet", 1, true);
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_log.osd";
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
	 * test reading Weatronic Telemetry Log files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testWeatronicLogReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".log") && file.getPath().toLowerCase().contains("weatronic")) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = "Weatronic-Telemetry";
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from LOG file");
						activeChannel.setSaved(true);

						RecordSet recordSet = gde.device.weatronic.LogReader.read(file.getAbsolutePath(), activeChannel.getNumber());

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_log.osd";
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
	 * test reading Weatronic Telemetry Log files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testSpektrumTLMReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".tlm") && file.getPath().toLowerCase().contains("spektrum")) {
					System.out.println("working with : " + file);

					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = "Spektrum-Telemetry";
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.analyzer.setActiveDevice(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from LOG file");
						activeChannel.setSaved(true);

						RecordSet recordSet = gde.device.spektrum.TlmReader.read(file.getAbsolutePath());

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						if (!new File(this.tmpDir1).exists())
							throw new FileNotFoundException(this.tmpDir1);

						String absolutFilePath = this.tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_tlm.osd";
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
}
