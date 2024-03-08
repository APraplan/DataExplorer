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
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.jeti;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.StringTokenizer;
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
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.exception.DataInconsitsentException;
import gde.io.FileHandler;
import gde.io.LogViewReader;
import gde.io.NMEAParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.StringHelper;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class JetiAdapter extends DeviceConfiguration implements IDevice {
	final static Logger			log	= Logger.getLogger(JetiAdapter.class.getName());

	final DataExplorer			application;
	final Channels					channels;
	final JetiAdapterDialog	dialog;

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public JetiAdapter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.jeti.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.dialog = new JetiAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2904), Messages.getString(MessageIds.GDE_MSGT2904));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public JetiAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.jeti.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.dialog = new JetiAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2904), Messages.getString(MessageIds.GDE_MSGT2904));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
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
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		// prepare the serial CSV data parser
		NMEAParser data = new NMEAParser(this.getDataBlockLeader(), this.getDataBlockSeparator().value(), this.getDataBlockCheckSumType(), Math.abs(this.getDataBlockSize(InputTypes.FILE_IO)), this,
				this.channels.getActiveChannelNumber(), this.getUTCdelta());
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		byte[] lineBuffer;
		byte[] subLengthBytes;
		int subLenght;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		try {
			int lastLength = 0;
			for (int i = 0; i < recordDataSize; i++) {
				subLengthBytes = new byte[4];
				System.arraycopy(dataBuffer, lastLength, subLengthBytes, 0, 4);
				subLenght = LogViewReader.parse2Int(subLengthBytes) - 8;
				lineBuffer = new byte[subLenght];
				System.arraycopy(dataBuffer, 4 + lastLength, lineBuffer, 0, subLenght);
				String textInput = new String(lineBuffer, "ISO-8859-1");
				StringTokenizer st = new StringTokenizer(textInput);
				Vector<String> vec = new Vector<String>();
				while (st.hasMoreTokens())
					vec.add(st.nextToken("\r\n"));
				data.parse(vec, vec.size());
				lastLength += (subLenght + 12);

				recordSet.addNoneCalculationRecordsPoints(data.getValues(), data.getTime_ms());

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}
			this.updateVisibilityStatus(recordSet, true);
			if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		}
		catch (Exception e) {
			String msg = e.getMessage() + Messages.getString(gde.messages.MessageIds.GDE_MSGW0543);
			log.log(Level.WARNING, msg, e);
			this.application.openMessageDialog(msg);
			if (doUpdateProgressBar) this.application.setProgress(0, sThreadId);
		}
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
		log.log(Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString()); //$NON-NLS-1$

		for (int i = 0; i < recordDataSize; i++) {
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			for (int j = 0; j < points.length; j++) {
				points[j] = (((convertBuffer[0 + (j * 4)] & 0xff) << 24) + ((convertBuffer[1 + (j * 4)] & 0xff) << 16) + ((convertBuffer[2 + (j * 4)] & 0xff) << 8) + ((convertBuffer[3 + (j * 4)] & 0xff) << 0));
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
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {

				switch (record.getDataType()) {
				case GPS_LATITUDE:
				case GPS_LONGITUDE:
					dataTableRow[index + 1] = String.format("%.6f", (record.get(rowIndex) / 1000000.0));
					break;
					
					case GPS_TIME:
						dataTableRow[index + 1] = JetiAdapter.getFormattedTime(record.realGet(rowIndex));
						break;
					case DATE_TIME:
						dataTableRow[index + 1] = JetiAdapter.getFormattedDate(record.realGet(rowIndex));
						break;

				case DEFAULT:
				default:
					double offset = record.getOffset(); // != 0 if curve has an defined offset
					double reduction = record.getReduction();
					double factor = record.getFactor(); // != 1 if a unit translation is required
					dataTableRow[index + 1] = record.getDecimalFormat().format((offset + ((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
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
	 * @param timeValue
	 * @return formatted time HH:mm:ss
	 */
	public static String getFormattedTime(final int timeValue) {
		int tmpHH = timeValue/10000000;
		int tmpMM = timeValue/100000 - tmpHH*100;
		int tmpSS = timeValue/1000 - tmpMM*100 - tmpHH*10000;
		return String.format("%02d:%02d:%02d", tmpHH, tmpMM, tmpSS); //$NON-NLS-1$;
	}

	/**
	 * @param dateValue
	 * @return formatted date yy-MM-dd
	 */
	public static String getFormattedDate(int dateValue) {
		int tmpYY = dateValue/10000000;
		int tmpMM = dateValue/100000 - tmpYY*100;
		int tmpDD = dateValue/1000 - tmpMM*100 - tmpYY*10000;
		return String.format("%02d-%02d-%02d", tmpYY, tmpMM, tmpDD); //$NON-NLS-1$
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		double newValue = 0;

		switch (record.getDataType()) {
		case GPS_LATITUDE:
		case GPS_LONGITUDE:
			//		int grad = ((int)(value / 1000));
			//		double minuten = (value - (grad*1000.0))/10.0;
			//		newValue = grad + minuten/60.0;
			newValue = value / 1000.0;
			break;

		case DEFAULT:
		default:
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
			newValue = (value - reduction) * factor + offset;
			break;
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		double newValue = 0;

		switch (record.getDataType()) {
		case GPS_LATITUDE:
		case GPS_LONGITUDE:
			//		int grad = (int)value;
			//		double minuten =  (value - grad*1.0) * 60.0;
			//		newValue = (grad + minuten/100.0)*1000.0;
			newValue = value * 1000.0;
			break;

		case DEFAULT:
		default:
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
			newValue = (value - offset) / factor + reduction;
			break;
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		int displayableCounter = 0;
		Record record;
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, record.getName() + " = " + this.getMeasurementNameReplacement(recordSet.getChannelConfigNumber(), i)); //$NON-NLS-1$

			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData());
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, record.getName() + " hasReasonableData = " + record.hasReasonableData()); //$NON-NLS-1$ 
			}

			if (record.isActive() && record.isDisplayable()) {
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
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
	 * @return the dialog
	 */
	@Override
	public JetiAdapterDialog getDialog() {
		return this.dialog;
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
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT2900), new String[] { GDE.FILE_ENDING_STAR_LOG, GDE.FILE_ENDING_STAR_JML,
				GDE.FILE_ENDING_STAR_STAR });

		Thread reader = new Thread("reader") {
			@Override
			public void run() {
				try {
					JetiAdapter.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								Integer channelConfigNumber = JetiAdapter.this.dialog != null && !JetiAdapter.this.dialog.isDisposed() ? JetiAdapter.this.dialog.getTabFolderSelectionIndex() + 1 : null;
								String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX) + 1, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
								JetiDataReader.read(selectedImportFile, JetiAdapter.this, recordNameExtend, channelConfigNumber, true);
							}
							catch (Throwable e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					JetiAdapter.this.application.setPortConnected(false);
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
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT2905));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "convertKLM3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2906));
			convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2907));
			convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
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
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && isActualRecordSetWithGpsData()) {
				new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT2903), 
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LONGITUDE),
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LATITUDE), 
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_ALTITUDE), 
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_SPEED), 
						activeRecordSet.findRecordOrdinalByUnit(new String[] {"m/s"}),					//climb
						activeRecordSet.findRecordOrdinalByUnit(new String[] {"km"}),						//distance 
						-1, 																																		//azimuth
						type == DeviceConfiguration.HEIGHT_RELATIVE, 
						type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
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
				if (!containsGPSdata) {
					containsGPSdata = (activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LONGITUDE) >= 0) && (activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LATITUDE) >= 0);
				}
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
			if (activeRecordSet != null && fileEndingType.contains(GDE.FILE_ENDING_KMZ) && this.isActualRecordSetWithGpsData()) {
				final int additionalMeasurementOrdinal = this.getGPS2KMZMeasurementOrdinal();
				exportFileName = new FileHandler().exportFileKMZ(
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LONGITUDE), 
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LATITUDE),
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_ALTITUDE), 
						additionalMeasurementOrdinal, 
						activeRecordSet.findRecordOrdinalByUnit(new String[] {"m/s"}),					//climb
						activeRecordSet.findRecordOrdinalByUnit(new String[] {"km"}),						//distance 
						-1, 																																		//azimuth
						true, isExportTmpDir);
			}
		}
		return exportFileName;
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
				if (this.kmzMeasurementOrdinal == null) // keep usage as initial supposed and use speed measurement ordinal
					return activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_SPEED); 

				return this.kmzMeasurementOrdinal != null ? this.kmzMeasurementOrdinal : -1;
			}
		}
		return -1;
	}

	/**
	 * @return the translated latitude and longitude to IGC latitude {DDMMmmmN/S, DDDMMmmmE/W} for GPS devices only
	 */
	@Override
	public String translateGPS2IGC(RecordSet recordSet, int index, char fixValidity, int startAltitude, int offsetAltitude) {
		String bSentence = "error";
		RecordSet activeRecordSet = this.application.getActiveRecordSet();
		if (activeRecordSet != null) {
			Record recordLatitude = recordSet.get(activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LATITUDE));
			Record recordLongitude = recordSet.get(activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LONGITUDE));
			Record baroAlitude = recordSet.get(activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_ALTITUDE));
			Record gpsAlitude = recordSet.get(activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_ALTITUDE));
			
			//		int grad = ((int)(value / 1000));
			//		double minuten = (value - (grad*1000.0))/10.0;
			//		newValue = grad + minuten/60.0;
			
			//		int grad = (int)value;
			//		double minuten =  (value - grad*1.0) * 60.0;
			//		newValue = (grad + minuten/100.0)*1000.0;

			bSentence = String.format("%02d%05d%s%03d%05d%s%c%05d%05d", //$NON-NLS-1$
					recordLatitude.get(index)  / 1000000, Double.valueOf( recordLatitude.get(index) % 1000000 / 100.0 * 6.0).intValue(), recordLatitude.get(index) > 0 ? "N" : "S",//$NON-NLS-1$
					recordLongitude.get(index) / 1000000, Double.valueOf(recordLongitude.get(index) % 1000000 / 100.0 * 6.0).intValue(), recordLongitude.get(index) > 0 ? "E" : "W",//$NON-NLS-1$
					fixValidity, Double.valueOf(baroAlitude.get(index) / 1000.0 + startAltitude + offsetAltitude).intValue(), Double.valueOf(gpsAlitude.get(index) / 1000.0 + offsetAltitude).intValue());
		}
		return bSentence;
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
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT2908, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT2908));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					open_closeCommPort();
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
	 * find best match of memory name with object key and select, if no match no object key will be changed
	 * @param batteryMemoryName
	 * @return
	 */
	public void matchModelNameObjectKey(String modelName) {
		Object[] tmpResult = null;
		for (String tmpObjectKey : this.application.getObjectKeys()) {
			if (tmpObjectKey.equals(modelName)) {
				tmpResult = new Object[] { tmpObjectKey, 100 };
				break;
			}
			String[] nameParts = modelName.split(" |-|_"); //$NON-NLS-1$
			int hitCount = 0;
			for (String namePart : nameParts) {
				if (namePart.length() > 1 && tmpObjectKey.contains(namePart)) ++hitCount;
			}
			if (hitCount > 0) {
				if (tmpResult == null || hitCount > (Integer) tmpResult[1]) {
					tmpResult = new Object[] { tmpObjectKey, hitCount };
					log.log(Level.FINE, "result updated = " + tmpObjectKey + " hitCount = " + hitCount); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		if (tmpResult != null) {
			this.application.selectObjectKey((String) tmpResult[0]);
		}
	}
}
