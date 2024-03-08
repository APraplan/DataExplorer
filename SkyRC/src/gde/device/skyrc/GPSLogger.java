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

    Copyright (c) 2022 Winfried Bruegmann
****************************************************************************************/
package gde.device.skyrc;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.device.resource.DeviceXmlResource;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.GPSHelper;

/**
 * @author brueg
 * SKYRC GSM-015 Logger bzw. Dynamite DYN4403 GPS Logger
 */
public class GPSLogger extends DeviceConfiguration implements IDevice {
	final static Logger		log								= Logger.getLogger(GPSLogger.class.getName());
	final DataExplorer		application;
	final Channels				channels;

	/**
	 * @param xmlFileName
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public GPSLogger(String xmlFileName) throws FileNotFoundException, JAXBException {
		super(xmlFileName);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.skyrc.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGI3664), Messages.getString(MessageIds.GDE_MSGI3664));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}

	/**
	 * @param deviceConfig
	 */
	public GPSLogger(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		Messages.setDeviceResourceBundle("gde.device.skyrc.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGI3664), Messages.getString(MessageIds.GDE_MSGI3664));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
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
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGI3668, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGI3668));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					open_closeCommPort();
				}
			});
		}
	}


	/**
	 * update the file menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZ3DAbsoluteItem;
		MenuItem convertGPXItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGI3665));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGI3666));
			convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGI3667));
			convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});

			convertGPXItem = new MenuItem(exportMenue, SWT.PUSH);
			convertGPXItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0728));
			convertGPXItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertGPXItem action performed! " + e); //$NON-NLS-1$
					export2GPX(false);
				}
			});
		}
	}
	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE | DeviceConfiguration.HEIGHT_CLAMPTOGROUND
	 */
	public void export2KMZ3D(int type) {
		//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGI3663), 2, 3, 1, 0, -1, -1, -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE | DeviceConfiguration.HEIGHT_CLAMPTOGROUND
	 */
	public void export2GPX(final boolean isGarminExtension) {
		//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 
		new FileHandler().exportFileGPX(Messages.getString(gde.messages.MessageIds.GDE_MSGT0730), 3, 2, 1, 0, -1, -1, -1, -1, new int[0]);
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
		//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 4=trip
		//log.log(Level.OFF, StringHelper.byte2Hex2CharString(dataBuffer));
		points[0] = DataParser.intFromBytes((byte)0, dataBuffer[0], dataBuffer[1], dataBuffer[2]) * 1000;
		points[1] = DataParser.intFromBytes((byte)0, dataBuffer[3], dataBuffer[4], dataBuffer[5]) * 1000;
		points[2] = dataBuffer[7] * 1000000 + DataParser.intFromBytes((byte)0, dataBuffer[8], dataBuffer[9], dataBuffer[10])/10;
		points[2] = dataBuffer[6] == 1 ? points[2] * -1 : points[2];
		points[3] = dataBuffer[12] * 1000000 + DataParser.intFromBytes((byte)0, dataBuffer[13], dataBuffer[14], dataBuffer[15])/10;
		points[3] = dataBuffer[11] == 1 ? points[3] * -1 : points[3];
		return points;
	}

	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.getNoneCalculationRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		Vector<Integer> timeStamps = new Vector<Integer>(1, 1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		byte[] timeStampBuffer = new byte[timeStampBufferSize];
		if (!recordSet.isTimeStepConstant()) {
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8)
						+ ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
			}
		}
		log.log(java.util.logging.Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString()); //$NON-NLS-1$

		for (int i = 0; i < recordDataSize; i++) {
			log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 
			for (int j = 0; j < points.length; j++) {
				points[j] = (((convertBuffer[0 + (j * 4)] & 0xff) << 24) + ((convertBuffer[1 + (j * 4)] & 0xff) << 16) + ((convertBuffer[2 + (j * 4)] & 0xff) << 8) + ((convertBuffer[3 + (j * 4)] & 0xff) << 0));
			}

			if (recordSet.isTimeStepConstant())
				recordSet.addNoneCalculationRecordsPoints(points);
			else
				recordSet.addNoneCalculationRecordsPoints(points, timeStamps.get(i) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		this.makeInActiveDisplayable(recordSet);
	}

	@Override
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		// nothing to do 
	}

	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {

		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 
				if (record.getOrdinal() == 2 || record.getOrdinal() == 3) {
					dataTableRow[index + 1] = String.format("%.6f", (record.get(rowIndex) / 1000000.0));
				}
				else {
					dataTableRow[index + 1] = record.getDecimalFormat().format((offset + ((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
				}
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
		}

	@Override
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 
		if (record.getOrdinal() == 1) { 
			PropertyType property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_FIRST.value());
			boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
			property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_LAST.value());
			boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

			try {
				if (subtractFirst) {
					reduction = record.getFirst() / 1000.0;
				}
				else if (subtractLast) {
					reduction = record.getLast() / 1000.0;
				}
			}
			catch (Throwable e) {
				reduction = 0;
			}
		}

		double newValue = 0;
		if (record.getOrdinal() == 2 || record.getOrdinal() == 3) { // 3=GPS-latitude 2=GPS-longitude 
			newValue = value / 1000.0;
		}
		else {
			newValue = (value - reduction) * factor + offset;
		}
		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	@Override
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 
		if (record.getOrdinal() == 1) { 
			PropertyType property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_FIRST.value());
			boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
			property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_LAST.value());
			boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

			try {
				if (subtractFirst) {
					reduction = record.getFirst() / 1000.0;
				}
				else if (subtractLast) {
					reduction = record.getLast() / 1000.0;
				}
			}
			catch (Throwable e) {
				reduction = 0;
			}
		}

		double newValue = 0;
		if (record.getOrdinal() == 2 || record.getOrdinal() == 3) { // 3=GPS-latitude 2=GPS-longitude 
			newValue = value * 1000.0;
		}
		else {
			newValue = (value - offset) / factor + reduction;
		}
		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 4=trip
		Record recordLatitude = recordSet.get(3);
		Record recordLongitude = recordSet.get(2);
		Record recordAlitude = recordSet.get(1);
		if (recordLatitude.hasReasonableData() && recordLongitude.hasReasonableData() && recordAlitude.hasReasonableData()) {
//			int recordSize = recordLatitude.realSize();
//			int startAltitude = recordAlitude.get(0); // using this as start point might be sense less if the GPS data has no 3D-fix
//			// check GPS latitude and longitude
//			int indexGPS = 0;
//			int i = 0;
//			for (; i < recordSize; ++i) {
//				if (recordLatitude.get(i) != 0 && recordLongitude.get(i) != 0) {
//					indexGPS = i;
//					++i;
//					break;
//				}
//			}
//			startAltitude = recordAlitude.get(indexGPS); // set initial altitude to enable absolute altitude calculation
//
//			GPSHelper.calculateTripLength(this, recordSet, 3, 2, 1, startAltitude, 4); //calculate 3D
			GPSHelper.calculateTripLength2D(this, recordSet, 3, 2, 4); //calculate 2D
		}

		this.updateVisibilityStatus(recordSet, true);
		this.application.updateStatisticsData();
	}

	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		Record record;

		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			if (log.isLoggable(java.util.logging.Level.FINE))
				log.log(java.util.logging.Level.FINE, record.getName() + " = " + DeviceXmlResource.getInstance().getReplacement(this.getMeasurementNames(channelConfigNumber)[i])); //$NON-NLS-1$

			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData());
				if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$
			}

			if (record.isDisplayable()) {
				++displayableCounter;
				if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
			}
		}
		if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
	}

	@Override
	public String[] getUsedPropertyKeys() {
		//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION };
	}

	@Override
	public void open_closeCommPort() {
		//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 

		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGI3662));

		Thread reader = new Thread("reader"){
			@Override
			public void run() {
				try {
					GPSLogger.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						if (!selectedImportFile.toLowerCase().endsWith("*.3gpl")) {
							if (selectedImportFile.contains(GDE.STRING_DOT)) {
								selectedImportFile = selectedImportFile.substring(0, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
							}
							selectedImportFile = selectedImportFile + ".3gpl";
						}
						log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 5) {
							try {
								Integer channelConfigNumber = application.getActiveChannelNumber(); 
								String recordNameExtend = GDE.STRING_EMPTY;
								RecordSet recordSet = GplLogReader.read(selectedImportFile, GPSLogger.this, recordNameExtend, channelConfigNumber);
								if (recordSet != null) {
									recordSet.get(1).setDataType(Record.DataType.GPS_ALTITUDE);
									recordSet.get(2).setDataType(Record.DataType.GPS_LONGITUDE);
									recordSet.get(3).setDataType(Record.DataType.GPS_LATITUDE);
								}
								else
									application.openMessageDialogAsync("Check " + selectedImportFile);
							}
							catch (Exception e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					GPSLogger.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * query if the actual record set of this device contains GPS data to enable KML export to enable google earth visualization 
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public boolean isActualRecordSetWithGpsData() {
		boolean containsGPSdata = false;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 
				containsGPSdata = activeRecordSet.get(2).hasReasonableData() && activeRecordSet.get(3).hasReasonableData();
			}
		}
		return containsGPSdata;
	}

	/**
	 * @param record
	 * @return true if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
		if (record.getOrdinal() == 2 || record.getOrdinal() == 3) { 
			//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 
			return true;
		}
		return false;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 
		if (this.kmzMeasurementOrdinal == null) // keep usage as initial supposed and use speed measurement ordinal
			return 0;

		return this.kmzMeasurementOrdinal;
	}

	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	@Override
	public String exportFile(String fileEndingType, boolean isExportTmpDir) {
		//GPS 0=velocity 1=altitudeGPS 2=longitude 3=latitude 
		String exportFileName = GDE.STRING_EMPTY;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && fileEndingType.contains(GDE.FILE_ENDING_KMZ) && this.isActualRecordSetWithGpsData()) {
				final int additionalMeasurementOrdinal = this.getGPS2KMZMeasurementOrdinal();
				int ordinalLongitude = activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LONGITUDE);
				ordinalLongitude = ordinalLongitude == -1 ? activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LONGITUDE_DEGREE) : ordinalLongitude;
				int ordinalLatitude = activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LATITUDE);
				ordinalLatitude = ordinalLatitude == -1 ? activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LATITUDE_DEGREE) : ordinalLongitude;

				exportFileName = new FileHandler().exportFileKMZ(
						2, 
						3,
						1, 
						additionalMeasurementOrdinal, 
						-1,					//climb
						-1,					//distance 
						-1, 																																		//azimuth
						true, isExportTmpDir);
			}
		}
		return exportFileName;
	}

}
