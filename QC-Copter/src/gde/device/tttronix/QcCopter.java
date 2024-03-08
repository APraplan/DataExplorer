/**************************************************************************************
  	This file is part of DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.tttronix;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.device.MeasurementType;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.Checksum;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

/**
 * Class to implement QC-Copter device
 * @author Winfried Brügmann
 */
public class QcCopter extends DeviceConfiguration implements IDevice {
	final static Logger				log	= Logger.getLogger(QcCopter.class.getName());

	final DataExplorer				application;
	final QcCopterDialog			dialog;
	final QcCopterSerialPort	serialPort;

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public QcCopter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.tttronix.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new QcCopterSerialPort(this, this.application);
		this.dialog = new QcCopterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public QcCopter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.tttronix.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new QcCopterSerialPort(this, this.application);
		this.dialog = new QcCopterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		}
	}

	/**
	 * @return the dialog
	 */
	@Override
	public QcCopterDialog getDialog() {
		return this.dialog;
	}

	/**
	 * load the mapping exist between lov file configuration keys and gde keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// ...
		return lov2osdMap;
	}

	/**
	 * convert record LogView config data to gde config keys into records section
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
		return 86; //sometimes first 4 bytes give the length of data + 4 bytes for number
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
		boolean configChanged = this.isChangePropery();
		Record record;
		MeasurementType measurement;
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		String[] measurementNames = this.getMeasurementNames(channelConfigNumber);
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			measurement = this.getMeasurement(channelConfigNumber, i);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " = " + measurementNames[i]); //$NON-NLS-1$

			// update active state and displayable state if configuration switched with other names
			if (record.isActive() != measurement.isActive()) {
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData() && measurement.isActive());
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$ 
			}

			if (record.isActive() && record.isDisplayable()) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
		this.setChangePropery(configChanged); //reset configuration change indicator to previous value, do not vote automatic configuration change at all
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

		//for the moment there are no calculations necessary
		//String[] recordNames = recordSet.getRecordNames();
		//for (int i=0; i<recordNames.length; ++i) {
		//	MeasurementType measurement = this.getMeasurement(recordSet.getChannelConfigNumber(), i);
		//	if (measurement.isCalculation()) {
		//		log.log(Level.FINE, "do calculation for " + recordNames[i]); //$NON-NLS-1$
		//	}
		//}
	}
	
	/**
	 * @return the device communication port
	 */
	@Override
	public QcCopterSerialPort getCommunicationPort() {
		return this.serialPort;
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
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.getDialog().dataGatherThread = new GathererThread(this.application, this, this.serialPort, activChannel.getNumber(), this.getDialog());
						try {
							if (this.getDialog().dataGatherThread != null && this.serialPort.isConnected()) {
								this.getDialog().dataGatherThread.start();
							}
						}
						catch (RuntimeException e) {
							log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
						catch (Throwable e) {
							log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
						//if (this.getDialog().boundsComposite != null && !this.getDialog().isDisposed()) this.getDialog().boundsComposite.redraw();
					}
				}
				catch (SerialPortException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(),
							Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
				}
				catch (ApplicationConfigurationException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (Throwable e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
				}
			}
			else {
				if (this.getDialog().dataGatherThread != null) {
					this.getDialog().dataGatherThread.stopDataGatheringThread(false, null);
				}
				//if (this.getDialog().boundsComposite != null && !this.getDialog().isDisposed()) this.getDialog().boundsComposite.redraw();
				this.serialPort.close();
			}
		}
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
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int deviceDataBufferSize = Math.abs(this.getDataBlockSize(InputTypes.SERIAL_IO));;
		int[] points = new int[this.getNumberOfMeasurements(1)];
		int offset = 0;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		byte[] convertBuffer = new byte[deviceDataBufferSize];
		double lastDateTime = 0, sumTimeDelta = 0, deltaTime = 0;

		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			//prepare databuffer for conversion
			System.arraycopy(dataBuffer, offset, convertBuffer, 0, deviceDataBufferSize);
			//recordSet.addPoints(convertDataBytes(points, convertBuffer));
			offset += lovDataSize;

			//prepare time calculation while individual time steps gets recorded
			byte[] timeBuffer = new byte[lovDataSize - deviceDataBufferSize];
			System.arraycopy(dataBuffer, offset - timeBuffer.length, timeBuffer, 0, timeBuffer.length);
			long dateTime = (long) (lastDateTime + 11650);
			try {
				dateTime = Long.parseLong((new String(timeBuffer).trim() + "0000000000").substring(6, 16)); //10 digits //$NON-NLS-1$
			}
			catch (NumberFormatException e) {
				// ignore
			}
			deltaTime = lastDateTime == 0 ? 0 : (dateTime - lastDateTime) / 116.5;
			log.log(java.util.logging.Level.FINE, String.format("%d; %4.1fd ms - %d : %s", i, deltaTime, dateTime, new String(timeBuffer).trim())); //$NON-NLS-1$
			sumTimeDelta += deltaTime;
			lastDateTime = dateTime;

			recordSet.addPoints(convertDataBytes(points, convertBuffer), sumTimeDelta);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}

		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		for (int i = 0, j = 0; j < points.length; ++i, ++j) {
			//DBx_0 = 94 + [ A7 A6 A5 A4 A3 A2 ]
			//DBx_1 = 94 + [ A1 A0 B7 B6 B5 ]
			//DBx_2 = 94 + [ B4 B3 B2 B1 B0 ]
			int DBx_1 = (dataBuffer[1 + i * 3] & 0xFF) - 94;
			int DBx_2 = (dataBuffer[1 + i * 3 + 1] & 0xFF) - 94;
			int DBx_3 = (dataBuffer[1 + i * 3 + 2] & 0xFF) - 94;
			log.log(java.util.logging.Level.FINE, i + "; " + j + "; " + (1 + i * 3) + "; " + (1 + i * 3 + 1) + "; " + (1 + i * 3 + 2)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			log.log(java.util.logging.Level.FINE, i + "; " + j + ": " + DBx_1 + "; " + DBx_2 + "; " + DBx_3); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			if (i <= 10 || i > 12) {
				points[j] = ((DBx_2 & 0x0007) << 13) | ((DBx_3 & 0x001F) << 8) | ((DBx_1 & 0x003F) << 2) | ((DBx_2 & 0x0018) >> 3);

				if (i != 10 && (points[j] & 0x00008000) > 0) // i==10 battery voltage uint16
					points[j] = (0xFFFF0000 | points[j]);

				points[j] *= 1000;
			}
			else { // motor uint8		
				points[j] = ((DBx_1 & 0x003F) << 2) | ((DBx_2 & 0x0018) >> 3);
				points[j] *= 1000;
				++j;
				points[j] = ((DBx_2 & 0x0007) << 5) | (DBx_3 & 0x001F);
				points[j] *= 1000;
			}
		}

		log.log(java.util.logging.Level.FINER, "CheckSum = " + (Checksum.ADD(dataBuffer, 1, 57)) + " = " + ((((dataBuffer[58] & 0xFF) - 94) << 6) | (((dataBuffer[59] & 0xFF) - 94) & 0x3F))); //$NON-NLS-1$ //$NON-NLS-2$
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

		// 0=Roll Integral, 1=Roll Winkel, 2=Nick Integral, 3=Nick Winkel, 4=Gyro Roll, 5=Gyro Nick, 6=Gyro Gier, 7=ACC Roll, 8=ACC Nick, 9=ACC Z, 
		//10=Akkuspannung, 11=Motor VL, 12=Motor VR, 13=Motor HL, 14=Motor HR, 15=User-Daten 0, 16=User-Daten 1, 17=User-Daten 2, 18=User-Daten 3, 19=User-Daten 4, 20=User-Daten 5
		for (int i = 0; i < recordDataSize; i++) {
			log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			for (int j = 0; j < points.length; j++) {
				points[j] = (((convertBuffer[j * 4] & 0xff) << 24) + ((convertBuffer[1 + (j * 4)] & 0xff) << 16) + ((convertBuffer[2 + (j * 4)] & 0xff) << 8) + ((convertBuffer[3 + (j * 4)] & 0xff) << 0));
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
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				dataTableRow[index + 1] = record.getDecimalFormat().format((offset + ((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
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

		double newValue = (value - reduction) * factor + offset;

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

		double newValue = (value - offset) / factor + reduction;

		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}
}
