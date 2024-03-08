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

    Copyright (c) 2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.renschler;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.PropertyType;
import gde.exception.DataInconsitsentException;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.CalculationThread;
import gde.utils.FileUtils;
import gde.utils.LinearRegression;
import gde.utils.QuasiLinearRegression;

/**
 * Picolario2 device main implementation class
 * @author Winfried Brügmann
 */
public class Picolario2 extends Picolario {

	/**
	 * @param iniFile
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public Picolario2(String iniFile) throws FileNotFoundException, JAXBException {
		super(iniFile);
		this.dialog = new Picolario2Dialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT1253), Messages.getString(MessageIds.GDE_MSGT1253));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}


	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Picolario2(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.dialog = new Picolario2Dialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT1253), Messages.getString(MessageIds.GDE_MSGT1253));
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
		//0=Altitude, 1=Pressure, 2=VoltageRx, 3=Climb, 4=Voltage, 5=Current, 5=Capacity, 7=Power 8=Revolution 9=Temperature, 10=Latitude, 11=Longitude, 12=Altitude GPS, 13=Speed (GPS)
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
		int timeStampBufferSize = 0;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		Vector<Integer> timeStamps = new Vector<Integer>(1,1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		if(!recordSet.isTimeStepConstant()) {
			timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
			byte[] timeStampBuffer = new byte[timeStampBufferSize];
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8) + ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
			}
		}
		log.log(Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString()); //$NON-NLS-1$

		for (int i = 0; i < recordDataSize; i++) {
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize+timeStampBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i*dataBufferSize+timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			//0=Altitude, 1=Pressure, 2=VoltageRx, 3=Climb, 4=Voltage, 5=Current, 5=Capacity, 7=Power 8=Revolution 9=Temperature, 10=Latitude, 11=Longitude, 12=Altitude GPS, 13=Speed (GPS)
			for (int j=0,k=0; j < points.length; j++) {
				switch (j) {
				case 3://3=Climb
				case 6://6=Capacity
				case 7://7=Power
					//0=Altitude, 1=Pressure, 2=VoltageRx, 3=Climb, 4=Voltage, 5=Current, 6=Capacity, 7=Power 8=Revolution 9=Temperature, 10=Latitude, 11=Longitude, 12=Altitude GPS, 13=Speed (GPS)
					break;

				default:
					points[j] = (((convertBuffer[0 + (k * 4)] & 0xff) << 24) + ((convertBuffer[1 + (k * 4)] & 0xff) << 16) + ((convertBuffer[2 + (k * 4)] & 0xff) << 8) + ((convertBuffer[3 + (k * 4)] & 0xff) << 0));
					++k;
					break;
				}
			}

			if(recordSet.isTimeStepConstant())
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i)/10.0);


			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
		}
		this.makeInActiveDisplayable(recordSet);
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
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
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required

				//0=Altitude, 1=Pressure, 2=VoltageRx, 3=Climb, 4=Voltage, 5=Current, 5=Capacity, 7=Power 8=Revolution 9=Temperature, 10=Latitude, 11=Longitude, 12=Altitude GPS, 13=Speed (GPS)
				switch (record.getOrdinal()) {
				case 0: //Höhe/Height
					PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
					boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
					property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
					boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

					if (subtractFirst) {
						reduction = record.getFirst()/1000.0;
					}
					else if (subtractLast) {
						reduction = record.getLast()/1000.0;
					}
					else {
						reduction = 0;
					}
					break;
				case 3: //Steigung/Slope
					factor = recordSet.get(3).getFactor(); // 1=height
					break;
				}

				dataTableRow[index + 1] = record.getDecimalFormat().format((offset + ((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * function to translate measured value from a device to values represented (((value - reduction) * factor) + offset - firstLastAdaption)
	 * @return double with the adapted value
	 */
	@Override
	public double translateValue(Record record, double value) {
		final String $METHOD_NAME = "translateValue()";
		log.log(Level.FINEST, String.format("input value for %s - %f", record.getName(), value)); //$NON-NLS-1$

		String recordKey = "?"; //$NON-NLS-1$
		double newValue = 0.0;
		try {
			//0=Altitude, 1=Pressure, 2=VoltageRx, 3=Climb, 4=Voltage, 5=Current, 5=Capacity, 7=Power 8=Revolution 9=Temperature, 10=Latitude, 11=Longitude, 12=Altitude GPS, 13=Speed (GPS)
			recordKey = record.getName();
			double offset = record.getOffset(); // != 0 if curve has an defined offset
			double reduction = record.getReduction();
			double factor = record.getFactor(); // != 1 if a unit translation is required

			switch (record.getOrdinal()) {
			case 0: // 0=height calculation need special procedure
				PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
				boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
				property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
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
					log.log(Level.SEVERE, record.getAbstractParent().getName() + " " + record.getName() + " " + e.getMessage() + " " + $CLASS_NAME + "." + $METHOD_NAME);
				}
				break;

			case 3: // 3=slope calculation needs height factor for calculation
				factor = this.getMeasurementFactor(record.getParent().getChannelConfigNumber(), 0); // 0=height
				break;
			}

			newValue = offset + (value - reduction) * factor;
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		log.log(Level.FINER, String.format("value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue)); //$NON-NLS-1$
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented (((value - offset + firstLastAdaption)/factor) + reduction)
	 * @return double with the adapted value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		log.log(Level.FINEST, String.format("input value for %s - %f", record.getName(), value)); //$NON-NLS-1$
		final String $METHOD_NAME = "reverseTranslateValue()";

		//0=Altitude, 1=Pressure, 2=VoltageRx, 3=Climb, 4=Voltage, 5=Current, 5=Capacity, 7=Power 8=Revolution 9=Temperature, 10=Latitude, 11=Longitude, 12=Altitude GPS, 13=Speed (GPS)
		String recordKey = record.getName();
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double reduction = record.getReduction();
		double factor = record.getFactor(); // != 1 if a unit translation is required

		switch (record.getOrdinal()) {
		case 0: // 0=height calculation need special procedure
			PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
			boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
			property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
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
				log.log(Level.SEVERE, record.getAbstractParent().getName() + " " + record.getName() + " " + e.getMessage() + " " + $CLASS_NAME + "." + $METHOD_NAME);
			}
			break;

		case 3: // 3=slope calculation needs height factor for calculation
			factor = this.getMeasurementFactor(record.getParent().getChannelConfigNumber(), 0); // 1=height
			break;
		}
		double newValue = (value - offset) / factor + reduction;

		log.log(Level.FINER, String.format("new value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue)); //$NON-NLS-1$
		return newValue;
	}

	/**
	 * function to calculate values for inactive and to be calculated records
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during capturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isRaw() && recordSet.isRecalculation()) {
			//0=Altitude, 1=Pressure, 2=VoltageRx, 3=Climb, 4=Voltage, 5=Current, 5=Capacity, 7=Power 8=Revolution 9=Temperature, 10=Latitude, 11=Longitude, 12=Altitude GPS, 13=Speed (GPS)
			// calculate the values required
			Record slopeRecord = recordSet.get(3);//3=Steigrate
			slopeRecord.setDisplayable(false);
			PropertyType property = slopeRecord.getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
			int regressionInterval = property != null ? Integer.valueOf(property.getValue()) : 10;
			property = slopeRecord.getProperty(CalculationThread.REGRESSION_TYPE);
			if (property == null || property.getValue().equals(CalculationThread.REGRESSION_TYPE_CURVE))
				this.calculationThread = new QuasiLinearRegression(recordSet, recordSet.get(0).getName(), slopeRecord.getName(), regressionInterval);
			else
				this.calculationThread = new LinearRegression(recordSet, recordSet.get(0).getName(), slopeRecord.getName(), regressionInterval);

			try {
				this.calculationThread.start();
			}
			catch (RuntimeException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
		this.updateVisibilityStatus(recordSet, true);
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
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT1254));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT1255));
			convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT1256));
			convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
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
		//0=Altitude, 1=Pressure, 2=VoltageRx, 3=Climb, 4=Voltage, 5=Current, 5=Capacity, 7=Power 8=Revolution 9=Temperature, 10=Latitude, 11=Longitude, 12=Altitude GPS, 13=Speed (GPS)
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT1252), 11, 10, 12, 13, 3, -1, -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
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
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT1257, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT1257));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					open_closeCommPort();
				}
			});
		}
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		//0=Altitude, 1=Pressure, 2=VoltageRx, 3=Climb, 4=Voltage, 5=Current, 5=Capacity, 7=Power 8=Revolution 9=Temperature, 10=Latitude, 11=Longitude, 12=Altitude GPS, 13=Speed (GPS)
		return -1;
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data
	 */
	@Override
	public void open_closeCommPort() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT1251));

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					Picolario2.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						Picolario2.log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								Integer channelConfigNumber = Picolario2.this.dialog != null && !Picolario2.this.dialog.isDisposed() ? ((Picolario2Dialog)Picolario2.this.dialog).getTabFolderSelectionIndex() + 1 : null;
								String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));
								Picolario2LogReader.read(selectedImportFile, Picolario2.this, recordNameExtend, channelConfigNumber);
							}
							catch (Throwable e) {
								Picolario2.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					Picolario2.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
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
		//0=Altitude, 1=Pressure, 2=VoltageRx, 3=Climb, 4=Voltage, 5=Current, 5=Capacity, 7=Power 8=Revolution 9=Temperature, 10=Latitude, 11=Longitude, 12=Altitude GPS, 13=Speed (GPS)
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			log.log(java.util.logging.Level.FINE, record.getName() + " = " + this.getMeasurementNameReplacement(recordSet.getChannelConfigNumber(), i)); //$NON-NLS-1$

			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData());
				log.log(java.util.logging.Level.FINE, record.getName() + " hasReasonableData = " + record.hasReasonableData()); //$NON-NLS-1$
			}

			if (record.isActive() && record.isDisplayable()) {
				log.log(java.util.logging.Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		log.log(Level.FINER, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
	}
}
