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
    
    Copyright (c) 2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.spektrum;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.Analyzer;
import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.resource.DeviceXmlResource;
import gde.exception.DataInconsitsentException;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.GPSHelper;
import gde.utils.ObjectKeyCompliance;

public class SpektrumAdapter extends DeviceConfiguration implements IDevice {
	
	final static Logger		log																= Logger.getLogger(SpektrumAdapter.class.getName());

	
	final static Analyzer 						analyser 							= Analyzer.getInstance();	
	final static DataExplorer					application						=	DataExplorer.getInstance();
	final Settings										settings;
	final static Channels							channels							= Channels.getInstance();


	public SpektrumAdapter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		//Messages.setDeviceResourceBundle("gde.device.graupner.hott.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.settings = Settings.getInstance();
		if (SpektrumAdapter.application.getMenuToolBar() != null) {
			String toolTipText = SpektrumAdapter.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(SpektrumAdapter.application.getMenuBar().getExportMenu());
			updateFileImportMenu(SpektrumAdapter.application.getMenuBar().getImportMenu());
		}
	}

	public SpektrumAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		//Messages.setDeviceResourceBundle("gde.device.graupner.hott.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.settings = Settings.getInstance();
		if (SpektrumAdapter.application.getMenuToolBar() != null) {
			String toolTipText = SpektrumAdapter.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(SpektrumAdapter.application.getMenuBar().getExportMenu());
			updateFileImportMenu(SpektrumAdapter.application.getMenuBar().getImportMenu());
		}
	}

	/**
	 * @return the tooltip text for the import menu bar button
	 */
	public static String getImportToolTip() {
		DeviceConfiguration hoTTConfiguration = Analyzer.getInstance().getDeviceConfigurations().get("HoTTAdapter");
		String fileExtentions = hoTTConfiguration != null ? hoTTConfiguration.getDataBlockPreferredFileExtention() : GDE.STRING_QUESTION_MARK;
		return Messages.getString(gde.messages.MessageIds.GDE_MSGT0964, new Object[] { fileExtentions });
	}

	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	@Override
	public String exportFile(String fileEndingType, boolean isExport2TmpDir) {
		String exportFileName = GDE.STRING_EMPTY;
		Channel activeChannel = SpektrumAdapter.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && fileEndingType.contains(GDE.FILE_ENDING_KMZ)) {
				//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
				//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
				//Vario 12=Altitude V, 13=Climb V
				//Altitude	14=Altitude A
				//AltitudeZero 15=Altitude Offset
				//Voltage 16=Voltage V
				//Current 17=Current C
				//Temperature 18=Temperature T
				//AirSpeed 19=AirSpeed
				//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
				//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
				//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
				//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
				//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
				//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
				//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
				final int latOrdinal = 21, lonOrdinal = 22, altOrdinal = 20, climbOrdinal = 13, tripOrdinal = 28;
				final int additionalMeasurementOrdinal = this.getGPS2KMZMeasurementOrdinal();
				exportFileName = new FileHandler().exportFileKMZ(lonOrdinal, latOrdinal, altOrdinal, additionalMeasurementOrdinal, climbOrdinal, tripOrdinal, -1, true, isExport2TmpDir);
			}
		}
		return exportFileName;
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	public void export2KMZ3D(int type) {
		//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
		//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
		//Vario 12=Altitude V, 13=Climb V
		//Altitude	14=Altitude A
		//AltitudeZero 15=Altitude Offset
		//Voltage 16=Voltage V
		//Current 17=Current C
		//Temperature 18=Temperature T
		//AirSpeed 19=AirSpeed
		//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
		//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
		//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
		//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
		//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
		//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
		//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
		//21=Latitude, 22=Longitude, 20=Height, 13=Altitude, 23=Speed, 13=Climb, -1=TripLength, -1=Azimuth
	new FileHandler().exportFileKMZ(Messages.getString(gde.messages.MessageIds.GDE_MSGT0963), 21, 22, 20, 23, 13, -1, -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}

	/**
	 * update the file export menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileExportMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZDAbsoluteItem;
		// MenuItem convertGPXItem;
		// MenuItem convertGPXGarminItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0965));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					SpektrumAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZ3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0966));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					SpektrumAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0967));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					SpektrumAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});
		}
	}

	/**
	 * update the file import menu by adding new entry to import device specific files
	 * @param importMenue
	 */
	public void updateFileImportMenu(Menu importMenue) {
		MenuItem importDeviceLogItem;

		if (importMenue.getItem(importMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {
			new MenuItem(importMenue, SWT.SEPARATOR);

			importDeviceLogItem = new MenuItem(importMenue, SWT.PUSH);
			String[] messageParams = new String[GDE.MOD1.length + 1];
			System.arraycopy(GDE.MOD1, 0, messageParams, 1, GDE.MOD1.length);
			messageParams[0] = this.getDeviceConfiguration().getDataBlockPreferredFileExtention();
			importDeviceLogItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0962, messageParams));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(gde.messages.MessageIds.GDE_MSGT0962));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					SpektrumAdapter.log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					importDeviceData();
				}
			});
		}
	}

	/**
	 * import device specific *.bin data files
	 */
	protected void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(gde.messages.MessageIds.GDE_MSGT0961), "LogData"); //$NON-NLS-1$

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					SpektrumAdapter.application.setPortConnected(true);

					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						if (!selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_TLM) && !selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
							log.log(Level.WARNING, String.format("skip selectedImportFile %s since it has not a supported file ending", selectedImportFile));
						}
						SpektrumAdapter.log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > MIN_FILENAME_LENGTH) {
							// String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));

							String directoryName = ObjectKeyCompliance.getUpcomingObjectKey(Paths.get(selectedImportFile));
							if (!directoryName.isEmpty()) ObjectKeyCompliance.createObjectKey(directoryName);

							try {
								if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_TLM)) {
									TlmReader.read(selectedImportFile);
								}
							} catch (Exception e) {
								SpektrumAdapter.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
							}
						}
					}
				} finally {
					SpektrumAdapter.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		return null;
	}

	@Override
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		return null;
	}

	@Override
	public int getLovDataByteSize() {
		return 0;
	}

	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		return points;
	}

	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		int[] points = new int[recordSet.getNoneCalculationRecordNames().length];
					String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 1;
		if (doUpdateProgressBar) SpektrumAdapter.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		int index = 0;
		for (int i = 0; i < recordDataSize; i++) {
			index = i * dataBufferSize + timeStampBufferSize;
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + index); //$NON-NLS-1$

			for (int j = 0; j < points.length; j++) {
				points[j] = (((dataBuffer[0 + (j * 4) + index] & 0xff) << 24) + ((dataBuffer[1 + (j * 4) + index] & 0xff) << 16) + ((dataBuffer[2 + (j * 4) + index] & 0xff) << 8)
						+ ((dataBuffer[3 + (j * 4) + index] & 0xff) << 0));
			}

			recordSet.addNoneCalculationRecordsPoints(points,
					(((dataBuffer[0 + (i * 4)] & 0xff) << 24) + ((dataBuffer[1 + (i * 4)] & 0xff) << 16) + ((dataBuffer[2 + (i * 4)] & 0xff) << 8) + ((dataBuffer[3 + (i * 4)] & 0xff) << 0)) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) SpektrumAdapter.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) SpektrumAdapter.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
		}

	@Override
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				int ordinal = record.getOrdinal();
				//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
				//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
				//Vario 12=Altitude V, 13=Climb V
				//Altitude	14=Altitude A
				//AltitudeZero 15=Altitude Offset
				//Voltage 16=Voltage V
				//Current 17=Current C
				//Temperature 18=Temperature T
				//AirSpeed 19=AirSpeed
				//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
				//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
				//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
				//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
				//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
				//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
				//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
				if (ordinal >= 3 && ordinal <= 10) {
					dataTableRow[index + 1] = String.format("%.0f", (record.realGet(rowIndex) / 1000.0)); //$NON-NLS-1$
				}
				else {
					dataTableRow[index + 1] = record.getFormattedTableValue(rowIndex);
				}
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * query if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 * @param record
	 * @return
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
		//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
		final int latOrdinal = 21, lonOrdinal = 22;
		return record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal;
	}

	/**
	 * query if the actual record set of this device contains GPS data to enable KML export to enable google earth visualization
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public boolean isActualRecordSetWithGpsData() {
		boolean containsGPSdata = false;
		Channel activeChannel = SpektrumAdapter.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
				final int latOrdinal = 21, lonOrdinal = 22;
				containsGPSdata = activeRecordSet.get(latOrdinal).hasReasonableData() && activeRecordSet.get(lonOrdinal).hasReasonableData();
			}
		}
		return containsGPSdata;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
		//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
		//Vario 12=Altitude V, 13=Climb V
		//Altitude	14=Altitude A
		//AltitudeZero 15=Altitude Offset
		//Voltage 16=Voltage V
		//Current 17=Current C
		//Temperature 18=Temperature T
		//AirSpeed 19=AirSpeed
		//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
		//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
		//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
		//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
		//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
		//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
		//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
		if (kmzMeasurementOrdinal == null) // keep usage as initial supposed and use speed measurement ordinal
			return 23;

		return kmzMeasurementOrdinal;
	}

	@Override
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double newValue = 0;
		//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
		//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
		//Vario 12=Altitude V, 13=Climb V
		//Altitude	14=Altitude A
		//AltitudeZero 15=Altitude Offset
		//Voltage 16=Voltage V
		//Current 17=Current C
		//Temperature 18=Temperature T
		//AirSpeed 19=AirSpeed
		//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
		//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
		//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
		//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
		//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
		//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
		//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
		final int latOrdinal = 21, lonOrdinal = 22;
		if (record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal) { //15=Latitude, 16=Longitude
			int grad = ((int) (value / 1000));
			double minuten = (value - (grad * 1000.0)) / 10.0;
			newValue = grad + minuten / 60.0;
		}
		else {
			newValue = value * factor;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	@Override
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double newValue = 0;
		//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
		//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
		//Vario 12=Altitude V, 13=Climb V
		//Altitude	14=Altitude A
		//AltitudeZero 15=Altitude Offset
		//Voltage 16=Voltage V
		//Current 17=Current C
		//Temperature 18=Temperature T
		//AirSpeed 19=AirSpeed
		//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
		//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
		//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
		//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
		//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
		//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
		//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
		final int latOrdinal = 21, lonOrdinal = 22;
		if (record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal) { // 15=Latitude, 16=Longitude
			int grad = (int) value;
			double minuten = (value - grad * 1.0) * 60.0;
			newValue = (grad + minuten / 100.0) * 1000.0;
		}
		else {
			newValue = value / factor;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}
	

	/**
	 * function to calculate values for inactive records, data not readable from device
	 */
	public void calculateInactiveRecords(RecordSet recordSet) {
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		// 15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		// 24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6,
		// 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage,
		// 43=LowestCellNumber, 44=Pressure, 45=Event G
		// 46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14,
		// 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		// 73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max,
		// 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M

		// 73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
		// 93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max,
		// 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M
		final int latOrdinal = 15, lonOrdinal = 16, altOrdinal = 10, tripOrdinal = 20;
		Record recordLatitude = recordSet.get(latOrdinal);
		Record recordLongitude = recordSet.get(lonOrdinal);
		Record recordAlitude = recordSet.get(altOrdinal);
		if (recordLatitude.hasReasonableData() && recordLongitude.hasReasonableData() && recordAlitude.hasReasonableData()) { // 13=Latitude,
																																																													// 14=Longitude 9=Height
			int recordSize = recordLatitude.realSize();
			int startAltitude = recordAlitude.get(8); // using this as start point might be sense less if the GPS data has no 3D-fix
			// check GPS latitude and longitude
			int indexGPS = 0;
			int i = 0;
			for (; i < recordSize; ++i) {
				if (recordLatitude.get(i) != 0 && recordLongitude.get(i) != 0) {
					indexGPS = i;
					++i;
					break;
				}
			}
			startAltitude = recordAlitude.get(indexGPS); // set initial altitude to enable absolute altitude calculation

			GPSHelper.calculateTripLength(this, recordSet, latOrdinal, lonOrdinal, altOrdinal, startAltitude, tripOrdinal);
			// GPSHelper.calculateLabs(this, recordSet, latOrdinal, lonOrdinal, distOrdinal, tripOrdinal, 15);
		}
	}

	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		if (recordSet != null) {
			//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
			//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
			//Vario 12=Altitude V, 13=Climb V
			//Altitude	14=Altitude A
			//AltitudeZero 15=Altitude Offset
			//Voltage 16=Voltage V
			//Current 17=Current C
			//Temperature 18=Temperature T
			//AirSpeed 19=AirSpeed
			//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
			//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
			//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
			//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
			//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
			//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
			//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
			final int latOrdinal = 21, lonOrdinal = 22, altOrdinal = 20, tripOrdinal = 28;
			GPSHelper.calculateTripLength(this, recordSet, latOrdinal, lonOrdinal, altOrdinal, 0, tripOrdinal);

			recordSet.syncScaleOfSyncableRecords();
			this.updateVisibilityStatus(recordSet, true);
			SpektrumAdapter.application.updateStatisticsData();
		}
	}

	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		boolean configChanged = this.isChangePropery();
		Record record;

		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			if (SpektrumAdapter.log.isLoggable(java.util.logging.Level.FINE))
				SpektrumAdapter.log.log(java.util.logging.Level.FINE, record.getName() + " = " + DeviceXmlResource.getInstance().getReplacement(this.getMeasurementNames(channelConfigNumber)[i])); //$NON-NLS-1$

			MeasurementType measurement = this.getMeasurement(channelConfigNumber, i);
			if (record.isActive() && record.isActive() != measurement.isActive()) { //corrected values from older OSD might be overwritten p.e. VoltageRx_min
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				if (SpektrumAdapter.log.isLoggable(java.util.logging.Level.FINE)) SpektrumAdapter.log.log(java.util.logging.Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (includeReasonableDataCheck && !(record.getOrdinal() == 9 || record.getOrdinal() == 10)) {
				//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
				record.setDisplayable(measurement.isActive() && record.hasReasonableData());
				if (SpektrumAdapter.log.isLoggable(java.util.logging.Level.FINE)) SpektrumAdapter.log.log(java.util.logging.Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$
			}

			if (record.isActive() && record.isDisplayable()) {
				++displayableCounter;
				if (SpektrumAdapter.log.isLoggable(java.util.logging.Level.FINE)) SpektrumAdapter.log.log(java.util.logging.Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
			}
		}
		if (SpektrumAdapter.log.isLoggable(java.util.logging.Level.FINE)) SpektrumAdapter.log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
		this.setChangePropery(configChanged); // reset configuration change indicator to previous value, do not vote automatic configuration change at all
	}

	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] {FACTOR};
	}

	@Override
	public void open_closeCommPort() {
		importDeviceData();
	}

}
