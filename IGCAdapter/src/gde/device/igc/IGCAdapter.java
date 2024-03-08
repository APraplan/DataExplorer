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
    
    Copyright (c) 2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.igc;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
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
import gde.exception.DataInconsitsentException;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.CalculationThread;
import gde.utils.FileUtils;
import gde.utils.GPSHelper;
import gde.utils.LinearRegression;
import gde.utils.QuasiLinearRegression;
import gde.utils.StringHelper;

/**
 * IGC adapter class to read and verify ICG files
 * @author Winfried Bruegmann
 */
public class IGCAdapter extends DeviceConfiguration implements IDevice {
	final static Logger						log	= Logger.getLogger(IGCAdapter.class.getName());
	
	protected final DataExplorer				application;
	protected final Channels						channels;

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public IGCAdapter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.igc.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT1754), Messages.getString(MessageIds.GDE_MSGT1754));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public IGCAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.igc.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT1754), Messages.getString(MessageIds.GDE_MSGT1754));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// no device specific mapping required
		return lov2osdMap;
	}

	/**
	 * convert record LogView config data to GDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber 
	 * @return converted configuration data
	 */
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return -1;  
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * this method is more usable for real logger, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		//device specific implementation required
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {		
		//device specific implementation required
		return points;
	}

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal business to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		int[] points = new int[recordSet.getNoneCalculationRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 1;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		int index = 0;

		for (int i = 0; i < recordDataSize; i++) {
			index = i * dataBufferSize + timeStampBufferSize;
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + index); //$NON-NLS-1$

			for (int j = 0; j < points.length; j++) {
				points[j] = (((dataBuffer[0 + (j * 4) + index] & 0xff) << 24) + ((dataBuffer[1 + (j * 4) + index] & 0xff) << 16) + ((dataBuffer[2 + (j * 4) + index] & 0xff) << 8) + ((dataBuffer[3 + (j * 4) + index] & 0xff) << 0));
			}

			recordSet.addNoneCalculationRecordsPoints(points, 
						(((dataBuffer[0 + (i * 4)] & 0xff) << 24) + ((dataBuffer[1 + (i * 4)] & 0xff) << 16) + ((dataBuffer[2 + (i * 4)] & 0xff) << 8)	+ ((dataBuffer[3 + (i * 4)] & 0xff) << 0)) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * @param record
	 * @return true if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
		return record.getOrdinal() == 0 || record.getOrdinal() == 1; // 0=GPS-latitude 1=GPS-longitude 
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				//0=latitude 1=longitude  2=altitudeAbs 3=height
				if (record.getOrdinal() > 1) {
					dataTableRow[index + 1] = record.getFormattedTableValue(rowIndex);
				}
				else {
					int grad = record.realGet(rowIndex) / 1000000;
					double minuten = record.realGet(rowIndex) % 1000000 / 10000.0;
					dataTableRow[index + 1] = String.format("%02d %07.4f", grad, minuten); //$NON-NLS-1$
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
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
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

		//0=latitude 1=longitude  2=altitudeAbs 3=height
		double newValue = 0;
		if (record.getOrdinal() == 0 || record.getOrdinal() == 1) { // 0=GPS-latitude 1=GPS-longitude 
			int grad = ((int) (value / 1000));
			double minuten = (value - (grad * 1000.0)) / 10.0;
			newValue = grad + minuten / 60.0;
		}
		else {
			newValue = (value - reduction) * factor + offset;
		}
		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
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

		//0=latitude 1=longitude  2=altitudeAbs 3=height
		double newValue = 0;
		if (record.getOrdinal() == 0 || record.getOrdinal() == 1) { // 0=GPS-latitude 1=GPS-longitude 
			int grad = (int) value;
			double minuten = (value - grad * 1.0) * 60.0;
			newValue = (grad + minuten / 100.0) * 1000.0;
		}
		else {
			newValue = (value - offset) / factor + reduction;
		}
		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview 
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 * at least an update of the graphics window should be included at the end of this method
	 */
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int displayableCounter = 0;
		Record record;
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			log.log(java.util.logging.Level.FINER, record.getName() + " = " + this.getMeasurementNameReplacement(recordSet.getChannelConfigNumber(), i)); //$NON-NLS-1$

			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData());
				log.log(java.util.logging.Level.FINER, record.getName() + " hasReasonableData = " + record.hasReasonableData()); //$NON-NLS-1$ 
			}

			if (record.isActive() && record.isDisplayable()) {
				log.log(java.util.logging.Level.FINER, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		log.log(Level.FINER, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during capturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isRaw() && recordSet.isRecalculation()) {
			// 0=Longitude, 1=Latitude, 2=Altitude(baro), 3=Altitude(GPS), 4=Climb, 5=Speed
			// calculate the values required		
			Record slopeRecord = recordSet.get(4);//4=Climb
			slopeRecord.setDisplayable(false);
			PropertyType property = slopeRecord.getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
			int regressionInterval = property != null ? Integer.valueOf(property.getValue()) : 10;
			property = slopeRecord.getProperty(CalculationThread.REGRESSION_TYPE);
			if (property == null || property.getValue().equals(CalculationThread.REGRESSION_TYPE_CURVE))
				this.calculationThread = new QuasiLinearRegression(recordSet, recordSet.get(2).getName(), slopeRecord.getName(), regressionInterval);
			else
				this.calculationThread = new LinearRegression(recordSet, recordSet.get(2).getName(), slopeRecord.getName(), regressionInterval);

			try {
				this.calculationThread.start();
			}
			catch (RuntimeException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
			if (recordSet.realSize() >= 6)
				GPSHelper.calculateSpeed3D(this, recordSet, 1, 0, 2, 5);
		}
		this.updateVisibilityStatus(recordSet, true);
		this.application.updateStatisticsData();
	}	

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	public void open_closeCommPort() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT1750));

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					IGCAdapter.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$
						
						if (fd.getFileName().length() > 4) {
							try {
								//distinguish between short and long IGC file name
								int fileNameLength = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX)+1, selectedImportFile.lastIndexOf(GDE.CHAR_DOT)).length();
								String  recordNameExtend;
								if (fileNameLength == 8) { //short name 
									recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT)-1, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
									try {
										Integer.parseInt(recordNameExtend);
									}
									catch (Exception e) { // A to Z will cause an exception
										recordNameExtend = GDE.STRING_EMPTY + (recordNameExtend.getBytes()[0] - 54);
									}
								}
								else { 
									if (tmpFileName.contains(GDE.STRING_DASH))
										recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DASH)+1, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
									else
										recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT)-1, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
								}
								IGCReaderWriter.read(selectedImportFile, IGCAdapter.this, recordNameExtend, 1);
							}
							catch (Throwable e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					IGCAdapter.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * update the file menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZ3DAbsoluteItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT1755));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT1756));
			convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT1757));
			convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});
		}
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE | DeviceConfiguration.HEIGHT_CLAMPTOGROUND
	 */
	public void export2KMZ3D(int type) {
		//0=latitude 1=longitude  2=altitudeAbs 3=height
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT1753), 1, 0, 2, -1, -1, -1, -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
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
				//0=latitude 1=longitude  2=altitudeAbs 3=height
				containsGPSdata = activeRecordSet.get(0).hasReasonableData() && activeRecordSet.get(1).hasReasonableData() && activeRecordSet.get(3).hasReasonableData();
			}
		}
		return containsGPSdata;
	}

	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	@Override
	public String exportFile(String fileEndingType, boolean isExportTmpDir) {
		String exportFileName = GDE.STRING_EMPTY;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && fileEndingType.contains(GDE.FILE_ENDING_KMZ)) {
				final int additionalMeasurementOrdinal = this.getGPS2KMZMeasurementOrdinal();
				//0=latitude 1=longitude  2=altitude 3=altitudeGPS 4=climb 5=speed
				exportFileName = new FileHandler().exportFileKMZ(1, 0, 2, additionalMeasurementOrdinal, 4, -1, -1, true, isExportTmpDir);
			}
		}
		return exportFileName;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		//0=latitude 1=longitude  2=altitude 3=altitudeGPS 4=climb 5=speed
		if (kmzMeasurementOrdinal == null) // keep usage as initial supposed and use speed measurement ordinal
			return 5;

		return kmzMeasurementOrdinal;
	}
		
	/**
	 * @return the translated latitude and longitude to IGC latitude {DDMMmmmN/S, DDDMMmmmE/W} for GPS devices only
	 */
	@Override
	public String translateGPS2IGC(RecordSet recordSet, int index, char fixValidity, int startAltitude, int offsetAltitude) {
		//GPGGA	0=latitude 1=longitude  2=altitudeAbs 3=numSatelites
		Record recordLatitude = recordSet.get(0);
		Record recordLongitude = recordSet.get(1);
		Record baroAlitude = recordSet.get(2);
		Record gpsAlitude = recordSet.get(3);
		
		return String.format("%02d%05d%s%03d%05d%s%c%05d%05d", 																																														//$NON-NLS-1$
				recordLatitude.get(index) / 1000000, Double.valueOf(recordLatitude.get(index) % 1000000 / 10.0 + 0.5).intValue(), recordLatitude.get(index) > 0 ? "N" : "S",//$NON-NLS-1$ //$NON-NLS-2$
				recordLongitude.get(index) / 1000000, Double.valueOf(recordLongitude.get(index) % 1000000 / 10.0 + 0.5).intValue(), recordLongitude.get(index) > 0 ? "E" : "W",//$NON-NLS-1$ //$NON-NLS-2$
				fixValidity, Double.valueOf(baroAlitude.get(index) / 10000.0 + startAltitude + offsetAltitude).intValue(), Double.valueOf(gpsAlitude.get(index) / 1000.0 + offsetAltitude).intValue());
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
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT1758, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT1758));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					open_closeCommPort();
				}
			});
		}
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[0];
	}

	/**
	 * check and adapt stored measurement properties against actual record set records which gets created by device properties XML
	 * - calculated measurements could be later on added to the device properties XML
	 * - devices with battery cell voltage does not need to all the cell curves which does not contain measurement values
	 * @param fileRecordsProperties - all the record describing properties stored in the file
	 * @param recordSet - the record sets with its measurements build up with its measurements from device properties XML
	 * @return string array of measurement names which match the ordinal of the record set requirements to restore file record properties
	 */
	@Override
	public String[] crossCheckMeasurements(String[] fileRecordsProperties, RecordSet recordSet) {
		ArrayList<String> recordKeys = new ArrayList<String>(Arrays.asList(recordSet.getRecordNames()));
		//check if measurement Speed is already contained
		boolean containsSpeed = false;
		for (String recordProperty : fileRecordsProperties) {
			String[] tmp = StringHelper.splitString(recordProperty, Record.DELIMITER, "_name=");
			containsSpeed = tmp[0].equals("Speed") || tmp[0].startsWith("Geschw");
			if (containsSpeed)
				break;
		}
		if (!containsSpeed) {
			recordKeys.remove(5);
		}
		final Vector<String> cleanedRecordNames = new Vector<>();
		if ((recordKeys.size() - fileRecordsProperties.length) > 0) {
			int i = 0;
			for (; i < fileRecordsProperties.length; ++i) {
				cleanedRecordNames.add(recordKeys.get(i));
			}
			//cleanup recordSet
			for (; i < recordKeys.size(); ++i) {
				recordSet.remove(recordKeys.get(i));
			}
			return cleanedRecordNames.toArray(new String[1]);
		}
		return recordKeys.toArray(new String[1]);
	}
}

