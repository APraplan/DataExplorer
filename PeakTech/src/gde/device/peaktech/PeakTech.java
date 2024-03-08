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

    Copyright (c) 2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.peaktech;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.DataInconsitsentException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * UT device class
 * @author Winfried Brügmann
 */
public abstract class PeakTech extends DeviceConfiguration implements IDevice {
	final static Logger					log						= Logger.getLogger(PeakTech.class.getName());

	public final static String	INPUT_TYPE		= "input_type";																//$NON-NLS-1$
	public final static String	INPUT_SYMBOL	= "input_symbol";															//$NON-NLS-1$
	public final static String	INPUT_UNIT		= "input_unit";																//$NON-NLS-1$

	final DataExplorer					application;
	final PeakTechSerialPort		serialPort;
	final PeakTechDialog				dialog = null;

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public PeakTech(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.peaktech.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new PeakTechSerialPort(this, this.application);
		//this.dialog = new PeakTechDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public PeakTech(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.peaktech.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new PeakTechSerialPort(this, this.application);
		//this.dialog = new PeakTechDialog(this.application.getShell(), this);
		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		//this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGW0022));
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
		//this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGW0022));
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device
	 */
	@Override
	public int getLovDataByteSize() {
		return 20; // sometimes first 4 bytes give the length of data + 4 bytes for number
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal business to user
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 */
	@Override
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int lovDataSize = this.getLovDataByteSize();
		int deviceDataBufferSize = 8;
		byte[] convertBuffer = new byte[deviceDataBufferSize];
		int[] points = new int[this.getNumberOfMeasurements(1)];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			System.arraycopy(dataBuffer, i * lovDataSize, convertBuffer, 0, deviceDataBufferSize);
			recordSet.addPoints(convertDataBytes(points, convertBuffer));

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	abstract public int[] convertDataBytes(int[] points, byte[] dataBuffer);

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal business to user
	 * @param recordSet
	 * @param dataBuffer
	 * @param doUpdateProgressBar
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

		int timeStampBufferSize = 0;
		if (!recordSet.isTimeStepConstant()) {
			timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
			byte[] timeStampBuffer = new byte[timeStampBufferSize];
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8)
						+ ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
			}
		}
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString());

		for (int i = 0; i < recordDataSize; i++) {
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize);
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);
			points[0] = (((convertBuffer[0] & 0xff) << 24) + ((convertBuffer[1] & 0xff) << 16) + ((convertBuffer[2] & 0xff) << 8) + ((convertBuffer[3] & 0xff) << 0));

			if (recordSet.isTimeStepConstant())
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i) / 10.0);
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			dataTableRow[1] = recordSet.get(0).getDecimalFormat().format(recordSet.get(0).realGet(rowIndex) / 1000.0);
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * query battery voltage level
	 * @param buffer
	 * @return true if battey voltage level detected as low
	 */
	public boolean isBatteryLevelLow(byte[] buffer) {
		return (buffer[7] & 0x02) != 1;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		// 0=Spannung oder Strom oder ..
		double newValue = value; // no factor, offset, reduction or other supported
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		// 0=Spannung oder Strom oder ..
		double newValue = value; // no factor, offset, reduction or other supported
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		recordSet.setAllVisibleAndDisplayable();
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		log.log(Level.FINE, "no update required for " + recordSet.getName()); //$NON-NLS-1$
	}

	/**
	 * @return the dialog
	 */
	@Override
	public PeakTechDialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the usbPort
	 */
	@Override
	public PeakTechSerialPort getCommunicationPort() {
		return this.serialPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR };
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	abstract public void open_closeCommPort();

	/**
	 * get measurement info (type, symbol, unit)
	 * @param buffer
	 * @return measurement unit as string
	 */
	abstract public HashMap<String, String> getMeasurementInfo(byte[] buffer, HashMap<String, String> measurementInfo);	

	/**
	 * get the measurement mode
	 * @param buffer
	 * @return the measurement mode key
	 */
	abstract public String getMode(byte[] buffer);

}
