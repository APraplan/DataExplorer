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
    
    Copyright (c) 2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.wb;

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
import gde.data.Record.DataType;
import gde.data.RecordSet;
import gde.device.DataBlockType;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.io.CSVSerialDataReaderWriter;
import gde.io.DataParser;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.StringHelper;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class CSV2SerialAdapter extends DeviceConfiguration implements IDevice {
	final static Logger							log					= Logger.getLogger(CSV2SerialAdapter.class.getName());

	final DataExplorer							application;
	final CSV2SerialAdapterDialog		dialog;
	protected final CSV2SerialPort	serialPort;
	protected final Channels				channels;
	protected GathererThread				gathererThread;

	protected boolean								isFileIO		= false;
	protected boolean								isSerialIO	= false;

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public CSV2SerialAdapter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.wb.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.dialog = new CSV2SerialAdapterDialog(this.application.getShell(), this);
		this.serialPort = new CSV2SerialPort(this, this.application);
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) {
			for (DataBlockType.Format format : this.getDataBlockType().getFormat()) {
				if (!isSerialIO) isSerialIO = format.getInputType() == InputTypes.SERIAL_IO;
				if (!isFileIO) isFileIO = format.getInputType() == InputTypes.FILE_IO;
			}
			if (isSerialIO) { //InputTypes.SERIAL_IO has higher relevance  
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT1706), Messages.getString(MessageIds.GDE_MSGT1705));
			}
			else { //InputTypes.FILE_IO
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT1703), Messages.getString(MessageIds.GDE_MSGT1703));
			}
			if (isFileIO) updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public CSV2SerialAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.wb.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.dialog = new CSV2SerialAdapterDialog(this.application.getShell(), this);
		this.serialPort = new CSV2SerialPort(this, this.application);
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) {
			for (DataBlockType.Format format : this.getDataBlockType().getFormat()) {
				if (!isSerialIO) isSerialIO = format.getInputType() == InputTypes.SERIAL_IO;
				if (!isFileIO) isFileIO = format.getInputType() == InputTypes.FILE_IO;
			}
			if (isSerialIO) { //InputTypes.SERIAL_IO has higher relevance  
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT1706), Messages.getString(MessageIds.GDE_MSGT1705));
			}
			else { //InputTypes.FILE_IO
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT1703), Messages.getString(MessageIds.GDE_MSGT1703));
			}
			if (isFileIO) updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
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
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 69; // sometimes first 4 bytes give the length of data + 4 bytes for number
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
		// prepare the serial CSV data parser
		DataParser data = new DataParser(this.getDataBlockTimeUnitFactor(), this.getDataBlockLeader(), this.getDataBlockSeparator().value(), this.getDataBlockCheckSumType(),
				Math.abs(this.getDataBlockSize(InputTypes.FILE_IO)));
		int[] startLength = new int[] { 0, 0 };
		byte[] lineBuffer = null;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		try {
			for (int i = 0; i < recordDataSize; i++) {
				setDataLineStartAndLength(dataBuffer, startLength);
				lineBuffer = new byte[startLength[1]];
				System.arraycopy(dataBuffer, startLength[0], lineBuffer, 0, startLength[1]);
				//0=Empfänger-Spannung 1=Höhe 2=Motor-Strom 3=Motor-Spannung 4=Motorakku-Kapazität 5=Geschwindigkeit 6=Temperatur 7=GPS-Länge 8=GPS-Breite 9=GPS-Höhe 10=Steigen 11=ServoImpuls
				data.parse(new String(lineBuffer), i);

				recordSet.addNoneCalculationRecordsPoints(data.getValues(), data.getTime_ms());

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}
			this.updateVisibilityStatus(recordSet, true);
			if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		}
		catch (Exception e) {
			String msg = e.getMessage() + Messages.getString(gde.messages.MessageIds.GDE_MSGW0543);
			log.log(Level.WARNING, msg, e);
			application.openMessageDialog(msg);
			if (doUpdateProgressBar) this.application.setProgress(0, sThreadId);
		}
	}

	/**
	 * set data line end points - this method will be called within getConvertedLovDataBytes only and requires to set startPos and crlfPos to zero before first call
	 * - data line start is defined with '$ ;'
	 * - end position is defined with '0d0a' (CRLF)
	 * @param dataBuffer
	 * @param startPos
	 * @param crlfPos
	 */
	private void setDataLineStartAndLength(byte[] dataBuffer, int[] refStartLength) {
		int startPos = refStartLength[0] + refStartLength[1];

		for (; startPos < dataBuffer.length; ++startPos) {
			if (dataBuffer[startPos] == 0x24) {
				if (dataBuffer[startPos + 2] == 0x31 || dataBuffer[startPos + 3] == 0x31) break; // "$ ;" or "$  ;" (record set number two digits
			}
		}
		int crlfPos = refStartLength[0] = startPos;

		for (; crlfPos < dataBuffer.length; ++crlfPos) {
			if (dataBuffer[crlfPos] == 0x0D) if (dataBuffer[crlfPos + 1] == 0X0A) break; //0d0a (CRLF)
		}
		refStartLength[1] = crlfPos - startPos;
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
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
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.size()];
		int timeStampBufferSize = 0;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		Vector<Integer> timeStamps = new Vector<Integer>(1, 1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		if (!recordSet.isTimeStepConstant()) {
			timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
			byte[] timeStampBuffer = new byte[timeStampBufferSize];
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8)
						+ ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
			}
		}
		log.log(Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString()); //$NON-NLS-1$

		for (int i = 0; i < recordDataSize; i++) {
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			//0=Empfänger-Spannung 1=Höhe 2=Motor-Strom 3=Motor-Spannung 4=Motorakku-Kapazität 5=Geschwindigkeit 6=Temperatur 7=GPS-Länge 8=GPS-Breite 9=GPS-Höhe 10=Steigen 11=ServoImpuls
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
	 * @param record
	 * @return true if if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
		return record.getDataType() == DataType.GPS_LATITUDE || record.getDataType() == DataType.GPS_LONGITUDE;
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				switch (record.getDataType()) {
				case DATE_TIME:
					dataTableRow[index + 1] = StringHelper.getFormatedTime(record.getUnit(), record.realGet(rowIndex));
					dataTableRow[index + 1] = dataTableRow[index + 1].substring(0, dataTableRow[index + 1].indexOf(GDE.CHAR_COMMA) + 2);
					break;

				default:
					dataTableRow[index + 1] = record.getFormattedTableValue(rowIndex);
					break;
				}
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
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

		double newValue = 0;
		switch (record.getDataType()) {
		case GPS_LATITUDE:
		case GPS_LONGITUDE:
			if (record.getUnit().contains("°") && record.getUnit().contains("'")) {
				int grad = ((int) (value / 1000));
				double minuten = (value - (grad * 1000.0)) / 10.0;
				newValue = grad + minuten / 60.0;
			}
			else { // assume degree only
				newValue = value / 1000.0;
			}
			break;

		case DATE_TIME:
			newValue = 0;
			break;

		default:
			newValue = (value - reduction) * factor + offset;
			break;
		}

		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

		double newValue = 0;
		switch (record.getDataType()) {
		case GPS_LATITUDE:
		case GPS_LONGITUDE:
			if (record.getUnit().contains("°") && record.getUnit().contains("'")) {
				int grad = (int) value;
				double minuten = (value - grad * 1.0) * 60.0;
				newValue = (grad + minuten / 100.0) * 1000.0;
			}
			else { // assume degree only
				newValue = value * 1000.0;
			}
			break;

		case DATE_TIME:
			newValue = 0;
			break;

		default:
			newValue = (value - offset) / factor + reduction;
			break;
		}

		log.log(Level.FINER, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		Record record;
		MeasurementType measurement;
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		String[] measurementNames = this.getMeasurementNames(channelConfigNumber);
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			measurement = this.getMeasurement(channelConfigNumber, i);
			log.log(Level.FINE, record.getName() + " = " + measurementNames[i]); //$NON-NLS-1$

			// update active state and displayable state if configuration switched with other names
			if (record.isActive() != measurement.isActive()) {
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				log.log(Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData() && measurement.isActive());
				log.log(Level.FINE, record.getName() + " ! hasReasonableData "); //$NON-NLS-1$ 
			}

			if (record.isActive() && record.isDisplayable()) {
				log.log(Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		log.log(Level.TIME, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//add implementation where data point are calculated
		//do not forget to make record displayable -> record.setDisplayable(true);

		//set record data types if required and/or given with the device properties
		String[] recordNames = recordSet.getRecordNames();
		for (int i = 0; i < recordNames.length; ++i) {
			MeasurementType measurement = this.getMeasurement(recordSet.getChannelConfigNumber(), i);
			PropertyType dataTypeProperty = measurement.getProperty(MeasurementPropertyTypes.DATA_TYPE.value());
			if (dataTypeProperty != null) {
				switch (Record.DataType.fromValue(dataTypeProperty.getValue())) {
				case GPS_ALTITUDE:
				case GPS_LATITUDE:
				case GPS_LONGITUDE:
				case DATE_TIME:
					recordSet.get(recordNames[i]).setDataType(Record.DataType.fromValue(dataTypeProperty.getValue()));
					break;

				default:
					break;
				}
			}
		}
		this.application.updateStatisticsData();
	}

	/**
	 * @return the dialog
	 */
	@Override
	public CSV2SerialAdapterDialog getDialog() {
		return this.dialog;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION };
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	public void open_closeCommPort() {
		if (this.isSerialIO) {
			if (this.serialPort != null) {
				if (!this.serialPort.isConnected()) {
					try {
						Channel activChannel = Channels.getInstance().getActiveChannel();
						if (activChannel != null) {
							this.gathererThread = new GathererThread(this.application, this, this.serialPort, activChannel.getNumber());
							try {
								if (this.serialPort.isConnected()) {
									this.gathererThread.start();
								}
							}
							catch (RuntimeException e) {
								log.log(Level.SEVERE, e.getMessage(), e);
							}
							catch (Throwable e) {
								log.log(Level.SEVERE, e.getMessage(), e);
							}
						}
					}
					catch (SerialPortException e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
					}
					catch (ApplicationConfigurationException e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
						this.application.getDeviceSelectionDialog().open();
					}
					catch (Throwable e) {
						log.log(Level.SEVERE, e.getMessage(), e);
					}
				}
				else {
					if (this.gathererThread != null) {
						this.gathererThread.stopDataGatheringThread(false, null);
					}
					this.serialPort.close();
				}
			}
		}
		else { //InputTypes.FILE_IO
			importCsvFiles();
		}
	}

	/**
	 * import a CSV file, also called "OpenFormat" file
	 */
	public void importCsvFiles() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT1700));

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					CSV2SerialAdapter.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								String recordNameExtend;
								try {
									recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
									Integer.valueOf(recordNameExtend);
								}
								catch (Exception e) {
									try {
										recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 3, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
										Integer.valueOf(recordNameExtend);
									}
									catch (Exception e1) {
										recordNameExtend = GDE.STRING_EMPTY;
									}
								}
								Integer channelConfigNumber = dialog != null && !dialog.isDisposed() ? dialog.getTabFolderSelectionIndex() + 1 : null;
								CSVSerialDataReaderWriter.read(selectedImportFile, CSV2SerialAdapter.this, recordNameExtend, channelConfigNumber,
										new DataParser(CSV2SerialAdapter.this.getDataBlockTimeUnitFactor(), CSV2SerialAdapter.this.getDataBlockLeader(), CSV2SerialAdapter.this.getDataBlockSeparator().value(),
												CSV2SerialAdapter.this.getDataBlockCheckSumType(), CSV2SerialAdapter.this.getDataBlockSize(InputTypes.FILE_IO)));
							}
							catch (Throwable e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					CSV2SerialAdapter.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
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
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT1704, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT1704));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					if (!isSerialIO)
						open_closeCommPort();
					else
						importCsvFiles();
				}
			});
		}
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
		//check file contained record properties which are not contained in actual configuration
		String[] recordNames = recordSet.getRecordNames();
		Vector<String> cleanedRecordNames = new Vector<String>();
		if ((recordNames.length - fileRecordsProperties.length) > 0) {
			for (String recordProps : fileRecordsProperties) {
				cleanedRecordNames.add(StringHelper.splitString(recordProps, Record.DELIMITER, Record.propertyKeys).get(Record.propertyKeys[0]));
			}
			recordNames = cleanedRecordNames.toArray(new String[1]);
			//correct recordSet with cleaned record names
			recordSet.clear();
			for (int j = 0; j < recordNames.length; j++) {
				MeasurementType measurement = this.getMeasurement(recordSet.getChannelConfigNumber(), j);
				recordSet.addRecordName(recordNames[j]);
				recordSet.put(recordNames[j],
						new Record(this, j, recordNames[j], measurement.getSymbol(), measurement.getUnit(), measurement.isActive(), measurement.getStatistics(), measurement.getProperty(), 5));
			}
		}
		return recordNames;
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
				//0=Empfänger-Spannung 1=Höhe 2=Motor-Strom 3=Motor-Spannung 4=Motorakku-Kapazität 5=Geschwindigkeit 6=Temperatur 7=GPS-Länge 8=GPS-Breite 9=GPS-Höhe 10=GPS-Geschwindigkeit 11=Steigen 12=ServoImpuls
				//13=tripLength 14=distance 15=azimuth 16=directionStart
				exportFileName = new FileHandler().exportFileKMZ(activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LONGITUDE), activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LATITUDE),
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_ALTITUDE), activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_SPEED),
						activeRecordSet.findRecordOrdinalByUnit(new String[] { "m/s" }), //climb
						activeRecordSet.findRecordOrdinalByUnit(new String[] { "km" }), //distance 
						-1, //azimuth
						true, isExportTmpDir);
			}
		}
		return exportFileName;
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE | DeviceConfiguration.HEIGHT_CLAMPTOGROUND
	 */
	public void export2KMZ3D(int type) {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && activeRecordSet.containsGPSdata()) {
				new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT1710), activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LONGITUDE),
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LATITUDE), activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_ALTITUDE),
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_SPEED), activeRecordSet.findRecordOrdinalByUnit(new String[] { "m/s" }), //climb
						activeRecordSet.findRecordOrdinalByUnit(new String[] { "km" }), //distance 
						-1, //azimuth
						type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
			}
		}
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
				//GPGGA	0=latitude 1=longitude  2=altitudeAbs 
				containsGPSdata = activeRecordSet.containsGPSdata();
				if (!containsGPSdata) {
					containsGPSdata = (activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LONGITUDE) >= 0) && (activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LATITUDE) >= 0);
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
		//GPGGA	0=latitude 1=longitude  2=altitudeAbs 3=numSatelites
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && this.isActualRecordSetWithGpsData()) {
				int recordOrdinal = activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_SPEED);
				return recordOrdinal >= 0 ? recordOrdinal : activeRecordSet.findRecordOrdinalByUnit(new String[] { "km/h", "kph" }); //speed;
			}
		}
		return -1;
	}

	/**
	 * update the file menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileMenu(final Menu exportMenue) {
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
					MenuItem convertKMZ3DRelativeItem;
					MenuItem convertKMZ3DAbsoluteItem;

					new MenuItem(exportMenue, SWT.SEPARATOR);
					convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
					convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT1711));
					convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
						@Override
						public void handleEvent(Event e) {
							log.log(java.util.logging.Level.FINEST, "convertKLM3DRelativeItem action performed! " + e); //$NON-NLS-1$
							export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
						}
					});
					convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
					convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT1712));
					convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
						@Override
						public void handleEvent(Event e) {
							log.log(java.util.logging.Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
							export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
						}
					});
					convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
					convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT1713));
					convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
						@Override
						public void handleEvent(Event e) {
							log.log(java.util.logging.Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
							export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
						}
					});
				}
			}
		});
	}
}
