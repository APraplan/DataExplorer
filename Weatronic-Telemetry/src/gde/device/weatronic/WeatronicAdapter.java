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
package gde.device.weatronic;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.widgets.FileDialog;

import gde.DataAccess;
import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.Record.DataType;
import gde.data.RecordSet;
import gde.device.ChannelPropertyTypes;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.DataInconsitsentException;
import gde.io.FileHandler;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.WaitTimer;

/**
 * PowerLab8 base device class
 * @author Winfried Brügmann
 */
public class WeatronicAdapter extends DeviceConfiguration implements IDevice {
	final static Logger						log							= Logger.getLogger(WeatronicAdapter.class.getName());
	final static Properties				properties			= new Properties();

	final DataExplorer						application;
	final Channels								channels;
	final Settings								settings;
	final WeatronicAdapterDialog	dialog;

	static boolean								isChannelFilter	= true;
	static boolean								isUtcFilter			= true;
	static boolean								isStatusFilter	= true;

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public WeatronicAdapter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.weatronic.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
		this.dialog = new WeatronicAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		WeatronicAdapter.isChannelFilter = this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL) == null
				|| this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL).getValue().equals(GDE.STRING_EMPTY) ? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL)
				.getValue()) : true;
		WeatronicAdapter.isStatusFilter = this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER) == null
				|| this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER).getValue().equals(GDE.STRING_EMPTY) ? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER)
				.getValue()) : true;
		WeatronicAdapter.isUtcFilter = this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE) == null || this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE).getValue().equals(GDE.STRING_EMPTY) ? Boolean
				.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE).getValue()) : true;
		readProperties();
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public WeatronicAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.weatronic.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
		this.dialog = new WeatronicAdapterDialog(this.application.getShell(), this);
		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		WeatronicAdapter.isChannelFilter = this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL) == null ? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL)
				.getValue()) : true;
		WeatronicAdapter.isStatusFilter = this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER) == null ? Boolean
				.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER).getValue()) : true;
		WeatronicAdapter.isUtcFilter = this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE) == null ? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE).getValue()) : true;
		readProperties();
	}

	/**
	 * read special properties to enable configuration to specific GPX extent values
	 */
	private void readProperties() {
		DataAccess.getInstance().checkMappingFileAndCreate(this.getClass(), "WeatronicSynchronizationMappings.xml");
		try (InputStream stream = DataAccess.getInstance().getMappingInputStream("WeatronicSynchronizationMappings.xml")) {
			WeatronicAdapter.properties.loadFromXML(stream);
		} catch (Exception e) {
			String preopertyFilePath = Settings.MAPPINGS_DIR_NAME + GDE.STRING_FILE_SEPARATOR_UNIX + "WeatronicSynchronizationMappings.xml"; //$NON-NLS-1$
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE3700, new String[] { preopertyFilePath }));
		}
	}

	/**
	 * @param isUtcFilterEnabled the isUtcFilter to set
	 */
	public static synchronized void setUtcFilter(boolean isUtcFilterEnabled) {
		WeatronicAdapter.isUtcFilter = isUtcFilterEnabled;
	}

	/**
	 * @param isStatusFilterEnabled the isStatusFilter to set
	 */
	public static synchronized void setStatusFilter(boolean isStatusFilterEnabled) {
		WeatronicAdapter.isStatusFilter = isStatusFilterEnabled;
	}

	/**
	 * @param isChannelFilterEnabled the isChannelFilter to set
	 */
	public static synchronized void setChannelFilter(boolean isChannelFilterEnabled) {
		WeatronicAdapter.isChannelFilter = isChannelFilterEnabled;
	}

	/**
	 * @return the dialog
	 */
	@Override
	public WeatronicAdapterDialog getDialog() {
		return this.dialog;
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
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
		int[] points = new int[recordSet.getNoneCalculationRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 1;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		int index = 0;
		for (int i = 0; i < recordDataSize; i++) {
			index = i * dataBufferSize + timeStampBufferSize;
			if (WeatronicAdapter.log.isLoggable(java.util.logging.Level.FINER)) WeatronicAdapter.log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + index); //$NON-NLS-1$

			for (int j = 0; j < points.length; j++) {
				points[j] = (((dataBuffer[0 + (j * 4) + index] & 0xff) << 24) + ((dataBuffer[1 + (j * 4) + index] & 0xff) << 16) + ((dataBuffer[2 + (j * 4) + index] & 0xff) << 8) + ((dataBuffer[3 + (j * 4)
						+ index] & 0xff) << 0));
			}

			recordSet.addPoints(points,
					(((dataBuffer[0 + (i * 4)] & 0xff) << 24) + ((dataBuffer[1 + (i * 4)] & 0xff) << 16) + ((dataBuffer[2 + (i * 4)] & 0xff) << 8) + ((dataBuffer[3 + (i * 4)] & 0xff) << 0)) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
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
		//return unchanged key set, reordSet was build using fileRecordsProperties keys
		return recordSet.getRecordNames();
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
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double factor = record.getFactor(); // != 1 if a unit translation is required
				DataType dataType = record.getDataType();
				if (dataType == Record.DataType.GPS_LATITUDE || dataType == Record.DataType.GPS_LONGITUDE) {
					dataTableRow[index + 1] = String.format("%09.6f", record.realGet(rowIndex) * factor); //$NON-NLS-1$
				}
				else {
					dataTableRow[index + 1] = record.getDecimalFormat().format((offset + (record.realGet(rowIndex) / 1000.0) * factor));
				}
				++index;
			}
		}
		catch (RuntimeException e) {
			WeatronicAdapter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required
		DataType dataType = record.getDataType();
		double newValue;
		if (dataType == Record.DataType.GPS_LATITUDE || dataType == Record.DataType.GPS_LONGITUDE)
			newValue = 1000 * value * factor + offset;
		else
			newValue = value * factor + offset;
		WeatronicAdapter.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required
		DataType dataType = record.getDataType();
		double newValue;
		if (dataType == Record.DataType.GPS_LATITUDE || dataType == Record.DataType.GPS_LONGITUDE)
			newValue = 1000 * value / factor - offset;
		else
			newValue = value / factor - offset;
		WeatronicAdapter.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

		recordSet.setAllDisplayable();
		for (int i = 0; i < recordSet.realSize(); ++i) {
			Record record = recordSet.get(i);
			record.setDisplayable(record.hasReasonableData());
			if (WeatronicAdapter.log.isLoggable(java.util.logging.Level.FINER)) WeatronicAdapter.log.log(java.util.logging.Level.FINER, record.getName() + " setDisplayable=" + record.hasReasonableData());
		}

		if (WeatronicAdapter.log.isLoggable(java.util.logging.Level.FINE)) {
			for (int i = 0; i < recordSet.size(); i++) {
				Record record = recordSet.get(i);
				WeatronicAdapter.log.log(java.util.logging.Level.FINE, record.getName() + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable());
			}
		}
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				int displayableCounter = 0;

				// check if measurements isActive == false and set to isDisplayable == false
				for (String measurementKey : recordSet.keySet()) {
					Record record = recordSet.get(measurementKey);

					if (record.isActive() && (record.getOrdinal() <= 6 || record.hasReasonableData())) {
						++displayableCounter;
					}
				}

				WeatronicAdapter.log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
				recordSet.setConfiguredDisplayable(displayableCounter);

				if (recordSet.getName().equals(this.channels.getActiveChannel().getActiveRecordSet().getName())) {
					this.application.updateGraphicsWindow();
				}
			}
			catch (RuntimeException e) {
				WeatronicAdapter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, Record.DataType.GPS_LATITUDE.value(), Record.DataType.GPS_LONGITUDE.value(), Record.DataType.GPS_ALTITUDE.value(),
				Record.DataType.GPS_AZIMUTH.value(), Record.DataType.GPS_SPEED.value(), "statistics" };
	}

	/**
	 * query if the measurements get build up dynamically while reading (import) the data
	 * the implementation must create measurementType while reading the import data,
	 * refer to Weatronic-Telemetry implementation DataHeader
	 * @return true
	 */
	@Override
	public boolean isVariableMeasurementSize() {
		return true;
	}

	/**
	 * method to get the sorted active or in active record names as string array
	 * - records which does not have inactive or active flag are calculated from active or inactive
	 * - all records not calculated may have the active status and must be stored
	 * @param channelConfigNumber
	 * @param validMeasurementNames based on the current or any previous configuration
	 * @return String[] containing record names
	 */
	@Override
	public String[] getNoneCalculationMeasurementNames(int channelConfigNumber, String[] validMeasurementNames) {
		return validMeasurementNames;
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	@Override
	public void open_closeCommPort() {
		importDeviceData();
	}

	/**
	 * import device specific *.bin data files
	 */
	protected void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT3700), "LogData");

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					WeatronicAdapter.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						if (!selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
							if (selectedImportFile.contains(GDE.STRING_DOT)) {
								selectedImportFile = selectedImportFile.substring(0, selectedImportFile.indexOf(GDE.CHAR_DOT));
							}
							selectedImportFile = selectedImportFile + GDE.FILE_ENDING_DOT_LOG;
						}
						WeatronicAdapter.log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							Integer channelConfigNumber = WeatronicAdapter.this.application.getActiveChannelNumber();
							channelConfigNumber = channelConfigNumber == null ? 1 : channelConfigNumber;
							//String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
							try {
								LogReader.read(selectedImportFile, channelConfigNumber); //, WeatronicAdapter.this, GDE.STRING_EMPTY, channelConfigNumber);
								WaitTimer.delay(500);
							}
							catch (Exception e) {
								WeatronicAdapter.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					WeatronicAdapter.this.application.setPortConnected(false);
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
			int latOrdinal = -1, longOrdinal = -1;
			if (activeRecordSet != null) {
				for (int i = 0; i < activeRecordSet.size(); i++) {
					Record record = activeRecordSet.get(i);
					Record.DataType datatype = record.getDataType();
					switch (datatype) {
					case GPS_LATITUDE:
						if (record.getName().startsWith("Rx")) latOrdinal = record.getOrdinal();
						break;
					case GPS_LONGITUDE:
						if (record.getName().startsWith("Rx")) longOrdinal = record.getOrdinal();
						break;

					default:
						break;
					}
				}
				if (latOrdinal != -1 && longOrdinal != -1) containsGPSdata = activeRecordSet.get(latOrdinal).hasReasonableData() && activeRecordSet.get(longOrdinal).hasReasonableData();
			}
		}
		return containsGPSdata;
	}

	/**
	 * query if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 * @param record
	 * @return
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
		return (record.getDataType() == DataType.GPS_LATITUDE || record.getDataType() == DataType.GPS_LONGITUDE) && record.getName().startsWith("Rx");
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		Channel activeChannel = this.channels.getActiveChannel();
		if (this.kmzMeasurementOrdinal == null) // keep usage as initial supposed and use speed measurement ordinal
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				for (int i = 0; i < activeRecordSet.size(); i++) {
					Record record = activeRecordSet.get(i);
					Record.DataType datatype = record.getDataType();
					switch (datatype) {
					case GPS_SPEED:
						if (record.getName().startsWith("Rx"))
							return record.getOrdinal();
						break;
					default:
						break;
					}
				}
			}
		}
		return this.kmzMeasurementOrdinal != null ? this.kmzMeasurementOrdinal : -1;
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	public void export2KMZ3D(int type) {
		int latOrdinal = -1, longOrdinal = -1, altOrdinal = -1, climbOrdinal = -1, speedOrdinal = -1, tripOrdinal = -1;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				for (int i = 0; i < activeRecordSet.size(); i++) {
					Record record = activeRecordSet.get(i);
					Record.DataType datatype = record.getDataType();
					switch (datatype) {
					case GPS_LATITUDE:
						if (record.getName().startsWith("Rx")) latOrdinal = record.getOrdinal();
						break;
					case GPS_LONGITUDE:
						if (record.getName().startsWith("Rx")) longOrdinal = record.getOrdinal();
						break;
					case GPS_ALTITUDE:
						if (record.getName().startsWith("Rx")) altOrdinal = record.getOrdinal();
						break;
					case GPS_SPEED:
						if (record.getName().startsWith("Rx")) speedOrdinal = record.getOrdinal();
						break;

					default:
						break;
					}
				}
			}
		}
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT3703), longOrdinal, latOrdinal, altOrdinal, speedOrdinal, climbOrdinal, tripOrdinal, -1,
				type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}

	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	@Override
	public String exportFile(String fileEndingType, boolean isExport2TmpDir) {
		String exportFileName = GDE.STRING_EMPTY;
		int latOrdinal = -1, longOrdinal = -1, altOrdinal = -1, climbOrdinal = -1, tripOrdinal = -1;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			final int additionalMeasurementOrdinal = this.getGPS2KMZMeasurementOrdinal();
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && fileEndingType.contains(GDE.FILE_ENDING_KMZ)) {
				for (int i = 0; i < activeRecordSet.size(); i++) {
					Record record = activeRecordSet.get(i);
					Record.DataType datatype = record.getDataType();
					switch (datatype) {
					case GPS_LATITUDE:
						if (record.getName().startsWith("Rx")) latOrdinal = record.getOrdinal();
						break;
					case GPS_LONGITUDE:
						if (record.getName().startsWith("Rx")) longOrdinal = record.getOrdinal();
						break;
					case GPS_ALTITUDE:
						if (record.getName().startsWith("Rx")) altOrdinal = record.getOrdinal();
						break;
					default:
						break;
					}
				}
				exportFileName = new FileHandler().exportFileKMZ(longOrdinal, latOrdinal, altOrdinal, additionalMeasurementOrdinal, climbOrdinal, tripOrdinal, -1, true, isExport2TmpDir);
			}
		}
		return exportFileName;
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
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		return;
	}
}
