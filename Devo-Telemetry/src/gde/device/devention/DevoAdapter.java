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

    Copyright (c) 2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.devention;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.DataAccess;
import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.Record.DataType;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.exception.DataInconsitsentException;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;

/**
 * Devo telemetry data adapter device implementation class
 */
public class DevoAdapter extends DeviceConfiguration implements IDevice {
	final static Logger			log					= Logger.getLogger(DevoAdapter.class.getName());
	final static Properties	properties	= new Properties();

	final DataExplorer			application;
	final Channels					channels;

	/**
	 * @param xmlFileName
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public DevoAdapter(String xmlFileName) throws FileNotFoundException, JAXBException {
		super(xmlFileName);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.devention.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT3303), Messages.getString(MessageIds.GDE_MSGT3303));
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
		}
		readProperties();
	}

	/**
	 * @param xmlFileName
	 * @param tmpUnmarshaller
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public DevoAdapter(String xmlFileName, Unmarshaller tmpUnmarshaller) throws FileNotFoundException, JAXBException {
		super(xmlFileName, tmpUnmarshaller);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.devention.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT3303), Messages.getString(MessageIds.GDE_MSGT3303));
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
		}
		readProperties();
	}

	/**
	 * @param deviceConfig
	 */
	public DevoAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.devention.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT3303), Messages.getString(MessageIds.GDE_MSGT3303));
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
		}
		readProperties();
	}

	/**
	 * read special properties to enable configuration to specific GPX extent values
	 */
	private void readProperties() {
		DataAccess.getInstance().checkMappingFileAndCreate(this.getClass(), "DevoMeasurementMappings.xml");
		try (InputStream stream = DataAccess.getInstance().getMappingInputStream("DevoMeasurementMappings.xml")) {
			DevoAdapter.properties.loadFromXML(stream);
		} catch (Exception e) {
			String preopertyFilePath = Settings.MAPPINGS_DIR_NAME + GDE.STRING_FILE_SEPARATOR_UNIX + "DevoMeasurementMappings.xml"; //$NON-NLS-1$
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE3300, new String[] { preopertyFilePath }));
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
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT3304, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT3304));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					DevoAdapter.log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					importCsvFiles();
				}
			});
		}
	}

	/**
	 * update the file export menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileExportMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZDAbsoluteItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT3311));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "convertKMZ3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT3312));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT3313));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});
		}
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// ...
		return lov2osdMap;
	}

	/**
	 * convert record LogView config data to GDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber
	 * @return converted configuration data
	 */
	@Override
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device
	 */
	@Override
	public int getLovDataByteSize() {
		return 0; // sometimes first 4 bytes give the length of data + 4 bytes for number
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		//noop due to previous parsed CSV data
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
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.size()];
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
		DevoAdapter.log.log(java.util.logging.Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString()); //$NON-NLS-1$

		for (int i = 0; i < recordDataSize; i++) {
			DevoAdapter.log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			for (int j = 0; j < points.length; j++) {
				points[j] = (((convertBuffer[0 + (j * 4)] & 0xff) << 24) + ((convertBuffer[1 + (j * 4)] & 0xff) << 16) + ((convertBuffer[2 + (j * 4)] & 0xff) << 8)
						+ ((convertBuffer[3 + (j * 4)] & 0xff) << 0));
			}

			if (recordSet.isTimeStepConstant())
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
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
	@Override
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		// noop
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
				dataTableRow[index + 1] = record.getFormattedTableValue(rowIndex);
				++index;
			}
		}
		catch (RuntimeException e) {
			DevoAdapter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
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

		double newValue = 0;
		switch (record.getDataType()) {
		case GPS_LATITUDE:
		case GPS_LONGITUDE:
//			int grad = (int) (value / 1000);
//			int minuten = (int) (value - (grad * 1000)) / 10;
//			double seconds = (value - (grad * 1000.0) - (minuten * 10.0)) * 10;
//			newValue = grad + minuten / 60.0 + seconds / 3600.0;
			int grad = ((int) (value / 1000));
			double minuten = (value - (grad * 1000.0)) / 10.0;
			newValue = grad + minuten / 60.0;
			break;

		default:
			newValue = (value - reduction) * factor + offset;
			break;
		}
		DevoAdapter.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
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

		double newValue = 0;
		switch (record.getDataType()) {
		case GPS_LATITUDE:
		case GPS_LONGITUDE:
			int grad = (int) value;
			double minuten = (value - grad * 1.0) * 60.0;
			newValue = (grad + minuten / 100.0) * 1000.0;
			break;

		default:
			newValue = (value - offset) / factor + reduction;
			break;
		}
		DevoAdapter.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		Record record;
		String[] measurementNames = recordSet.getRecordNames();

		//clean sync properties
		for (int i = 0; i < measurementNames.length; i++) {
			if (this.getMeasurement(channelConfigNumber, i).getProperty(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value()) != null) {
				Iterator<PropertyType> iterator = this.getMeasurement(channelConfigNumber, i).getProperty().iterator();
				while (iterator.hasNext()) {
					PropertyType propertyType = iterator.next();
					if (propertyType.getName().equals(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value())) {
						log.log(Level.FINE, "remove synch from " + measurementNames[i]);
						iterator.remove();
						continue;
					}
				}
			}
		}

		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			if (DevoAdapter.log.isLoggable(java.util.logging.Level.FINE)) DevoAdapter.log.log(java.util.logging.Level.FINE, record.getName() + " = " + measurementNames[i]); //$NON-NLS-1$

			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData());
				if (DevoAdapter.log.isLoggable(java.util.logging.Level.FINE))
					DevoAdapter.log.log(java.util.logging.Level.FINE, i + " " + record.getName() + " hasReasonableData = " + record.hasReasonableData()); //$NON-NLS-1$ //$NON-NLS-2$
			}

			if (record.isActive() && record.isDisplayable()) {
				if (DevoAdapter.log.isLoggable(java.util.logging.Level.FINE)) DevoAdapter.log.log(java.util.logging.Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		if (DevoAdapter.log.isLoggable(java.util.logging.Level.FINER)) DevoAdapter.log.log(java.util.logging.Level.FINER, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);

		for (int i = 0; i < measurementNames.length; i++) {
			for (int j = i; j < measurementNames.length; j++) {
				String[] nameParts = measurementNames[j].split(GDE.STRING_BLANK);
				if (nameParts.length > 1 && measurementNames[i].split(GDE.STRING_BLANK)[0].equals(nameParts[0]) && i != j
						&& this.getMeasruementProperty(channelConfigNumber, j, MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value()) == null && recordSet.get(i).getUnit().equals(recordSet.get(j).getUnit())
						&& recordSet.get(j).getDataType() == Record.DataType.DEFAULT) {
					log.log(Level.FINE, "do synch " + measurementNames[j] + " to " + measurementNames[i]);
					this.setMeasurementPropertyValue(channelConfigNumber, j, MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value(), DataTypes.INTEGER, i);
				}
			}
		}
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		this.application.updateStatisticsData();
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION };
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data
	 */
	@Override
	public void open_closeCommPort() {
		importCsvFiles();
	}

	/**
	 * import a CSV file, also called "OpenFormat" file
	 */
	public void importCsvFiles() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT3300));

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					DevoAdapter.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						DevoAdapter.log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								RecordSet activeRecordSet = CSVReaderWriter.read(DevoAdapter.this.getDataBlockSeparator().value().charAt(0), selectedImportFile, DevoAdapter.this.getRecordSetStemNameReplacement());
								DevoAdapter.this.updateVisibilityStatus(activeRecordSet, true);
								activeRecordSet.descriptionAppendFilename(selectedImportFile);
							}
							catch (Throwable e) {
								DevoAdapter.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					DevoAdapter.this.application.setPortConnected(false);
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
				for (int i = 0; i < activeRecordSet.size(); i++) {
					Record record = activeRecordSet.get(i);
					if (record.getDataType().equals(Record.DataType.GPS_LATITUDE) || record.getDataType().equals(Record.DataType.GPS_LONGITUDE)) {
						containsGPSdata = record.hasReasonableData();
						break;
					}
				}
			}
		}
		return containsGPSdata;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		RecordSet actualRecordSet = this.application.getActiveRecordSet();
		if (actualRecordSet != null) {
			if (this.kmzMeasurementOrdinal == null) // keep usage as initial supposed and use speed measurement ordinal
				return actualRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_SPEED);
		}
		return this.kmzMeasurementOrdinal != null ? this.kmzMeasurementOrdinal : -1;
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
				exportFileName = new FileHandler().exportFileKMZ(activeRecordSet.getRecordOrdinalOfDataType(Record.DataType.GPS_LONGITUDE), activeRecordSet.getRecordOrdinalOfDataType(Record.DataType.GPS_LATITUDE),
						activeRecordSet.getRecordOrdinalOfDataType(Record.DataType.GPS_ALTITUDE), additionalMeasurementOrdinal, findRecordByUnit(activeRecordSet, "m/s"), findRecordByUnit(activeRecordSet, "km"), -1, true,
						isExportTmpDir);
			}
		}
		return exportFileName;
	}

	private int findRecordByUnit(RecordSet recordSet, String unit) {
		for (int i = 0; i < recordSet.size(); i++) {
			Record record = recordSet.get(i);
			if (record.getUnit().equalsIgnoreCase(unit)) return record.getOrdinal();
		}
		return -1;
	}


	/**
	 * @param record
	 * @return true if if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
		return record.getDataType() == DataType.GPS_LATITUDE || record.getDataType() == DataType.GPS_LONGITUDE;
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	public void export2KMZ3D(int type) {
		RecordSet activeRecordSet = application.getActiveRecordSet();
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT3310), activeRecordSet.getRecordOrdinalOfDataType(Record.DataType.GPS_LONGITUDE),
				activeRecordSet.getRecordOrdinalOfDataType(Record.DataType.GPS_LATITUDE), activeRecordSet.getRecordOrdinalOfDataType(Record.DataType.GPS_ALTITUDE), findRecordByUnit(activeRecordSet, "km/h"),
				findRecordByUnit(activeRecordSet, "m/s"), findRecordByUnit(activeRecordSet, "km"), -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}
}
