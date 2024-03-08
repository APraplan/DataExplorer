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

    Copyright (c) 2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.unitrend;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * UT device class
 * @author Winfried Brügmann
 */
public class UniTrend extends DeviceConfiguration implements IDevice {
	final static Logger					log						= Logger.getLogger(UniTrend.class.getName());

	public final static String	INPUT_TYPE		= "input_type";																//$NON-NLS-1$
	public final static String	INPUT_SYMBOL	= "input_symbol";															//$NON-NLS-1$
	public final static String	INPUT_UNIT		= "input_unit";																//$NON-NLS-1$

	final DataExplorer					application;
	final UniTrendSerialPort		serialPort;
	final UniTrendDialog				dialog;

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public UniTrend(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.unitrend.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new UniTrendSerialPort(this, this.application);
		this.dialog = new UniTrendDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public UniTrend(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.unitrend.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new UniTrendSerialPort(this, this.application);
		this.dialog = new UniTrendDialog(this.application.getShell(), this);
		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGW0022));
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
		this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGW0022));
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
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {

		if (log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i <= 5; i++) {
				sb.append(String.format("%02x", dataBuffer[i])).append(" "); //$NON-NLS-1$ //$NON-NLS-2$
			}
			log.log(Level.FINE, sb.toString());
		}
		points[0] = Integer.valueOf(String.format("%c%c%c%c%c", dataBuffer[1], dataBuffer[2], dataBuffer[3], dataBuffer[4], dataBuffer[5])).intValue();
		switch (dataBuffer[6]) {
		default:
		case 59: //voltage
			switch (dataBuffer[0]) {
			case 48:
				points[0] /= 10;
				break;
			case 50:
				points[0] *= 10;
				break;
			case 51:
				points[0] *= 100;
				break;
			case 52:
				points[0] *= 10;
				break;
			case 49:
			default:
				break;
			}
			break;
		case 51: //resistance
			switch (dataBuffer[0]) {
			case 48:
				points[0] *= 10;
				break;
			case 50:
				break;
			case 51:
				points[0] *= 10;
				break;
			case 52:
				points[0] /= 10;
				break;
			case 53:
				break;
			case 54:
				points[0] /= 100000;
				break;
			case 49:
				points[0] /= 10;
			default:
				break;
			}
			break;
		case 53: //resistance - pieps
			break;
		case 49: //resistance - diode
			points[0] /= 10;
			break;
		case 54: //capacity
			switch (dataBuffer[0]) {
			case 49:
			case 52:
			case 55:
				points[0] *= 10;
				break;
			case 50:
			case 53:
				points[0] /= 10;
				break;
			default:
				break;
			}
			break;
		case 50: //frequency
			break;
		case 61: //current
			switch (dataBuffer[0]) {
			default:
			case 48:
				points[0] *= 10;
				break;
			case 49:
				points[0] *= 100;
				break;
			}
			break;
		case 63: //current
			switch (dataBuffer[0]) {
			default:
			case 48:
				points[0] /= 1;
				break;
			case 49:
				points[0] *= 10;
				break;
			}
			break;
		case 48: //current
			switch (dataBuffer[0]) {
			default:
			case 48:
				points[0] /= 1;
				break;
			}
			break;
		}

		if ((dataBuffer[7] & 0x04) > 0) points[0] *= -1;

		return points;
	}

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
	 * get measurement info (type, symbol, unit)
	 * @param buffer
	 * @return measurement unit as string
	 */
	public HashMap<String, String> getMeasurementInfo(byte[] buffer, HashMap<String, String> measurementInfo) {
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "buffer : " + StringHelper.byte2Hex4CharString(buffer, buffer.length));
			log.log(Level.FINE, "Schalterstellung (Byte[6]): " + buffer[6]);
			log.log(Level.FINE, "Bereich          (Byte[0]): " + buffer[0]);
			log.log(Level.FINE, "Kopplung        (Byte[10]): " + buffer[10]);
		}
		String unit = ""; //$NON-NLS-1$
		switch (buffer[6]) {
		default:
		case 59: //voltage
			switch (buffer[0]) {
			case 52:
				unit = "mV";
				break;
			default:
				unit = "V";
				break;
			}
			switch (buffer[10]) {
			case 57:
				unit = "%";
				break;
			case 59:
				unit = "Hz";
				break;
			default:
				break;
			}
			break;
		case 51: //resistance
			switch (buffer[0]) {
			case 49:
			case 50:
			case 51:
				unit = "kΩ";
				break;
			case 52:
			case 53:
			case 54:
				unit = "MΩ";
				break;
			case 48:
			default:
				unit = "Ω";
				break;
			}
			break;
		case 53: //resistance - pieps
			unit = "Ω";
			break;
		case 49: //resistance - diode
			unit = "V";
			break;
		case 54: //capacity
			switch (buffer[0]) {
			default:
			case 48:
			case 49:
				unit = "nF";
				break;
			case 50:
			case 51:
			case 52:
				unit = "µF";
				break;
			case 53:
			case 54:
			case 55:
				unit = "mF";
				break;
			}
			break;
		case 50: //frequency
			switch (buffer[0]) {
			default:
			case 48:
			case 49:
				unit = "Hz";
				break;
			case 51:
			case 52:
				unit = "kHz";
				break;
			case 53:
			case 54:
			case 55:
				unit = "MHz";
				break;
			}
			break;
		case 61: //current
			unit = "µA";
			break;
		case 63: //current
			unit = "mA";
			break;
		case 48: //current
			unit = "A";
			break;
		}

		measurementInfo.put(UniTrend.INPUT_UNIT, unit);

		String typeSymbol = Messages.getString(MessageIds.GDE_MSGT1500);
		if (unit.contains("V")) //$NON-NLS-1$
			typeSymbol = Messages.getString(MessageIds.GDE_MSGT1501);
		else if (unit.endsWith("A")) //$NON-NLS-1$
			typeSymbol = Messages.getString(MessageIds.GDE_MSGT1503);
		else if (unit.endsWith("Ω")) //$NON-NLS-1$
			typeSymbol = Messages.getString(MessageIds.GDE_MSGT1504);
		else if (unit.endsWith("F")) //$NON-NLS-1$
			typeSymbol = Messages.getString(MessageIds.GDE_MSGT1505);
		else if (unit.endsWith("Hz")) //$NON-NLS-1$
			typeSymbol = Messages.getString(MessageIds.GDE_MSGT1506);
		else if (unit.endsWith("°C")) //$NON-NLS-1$
			typeSymbol = Messages.getString(MessageIds.GDE_MSGT1507);
		else if (unit.endsWith("%")) typeSymbol = Messages.getString(MessageIds.GDE_MSGT1537); //$NON-NLS-1$

		try {
			measurementInfo.put(UniTrend.INPUT_TYPE, typeSymbol.split(" ")[0]); //$NON-NLS-1$
			measurementInfo.put(UniTrend.INPUT_SYMBOL, typeSymbol.split(" ")[1]); //$NON-NLS-1$
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
		}

		return measurementInfo;
	}

	/**
	 * get the measurement mode
	 * @param buffer
	 * @return the measurement mode key
	 */
	public String getMode(byte[] buffer) {
		String mode;
		if ((buffer[10] & 0x02) > 0)
			mode = Messages.getString(MessageIds.GDE_MSGT1511);
		else
			mode = Messages.getString(MessageIds.GDE_MSGT1510);

		if ((buffer[10] & 0x08) > 0)
			mode += Messages.getString(MessageIds.GDE_MSGT1512); //AC
		else if ((buffer[10] & 0x04) > 0) mode += Messages.getString(MessageIds.GDE_MSGT1513); // DC

		return mode;
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
	public UniTrendDialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the serialPort
	 */
	@Override
	public UniTrendSerialPort getCommunicationPort() {
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
	@Override
	public void open_closeCommPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.getDialog().dataGatherThread = new GathererThread(this.application, this, this.serialPort, activChannel.getNumber(), this.getDialog());
						try {
							if (this.serialPort.isConnected()) {
								this.getDialog().dataGatherThread.start();
							}
						}
						catch (RuntimeException e) {
							log.log(Level.WARNING, e.getMessage(), e);
						}
						if (this.getDialog().boundsComposite != null && !this.getDialog().isDisposed()) this.getDialog().boundsComposite.redraw();
					}
				}
				catch (SerialPortException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(),
							Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
				}
				catch (ApplicationConfigurationException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
			}
			else {
				if (this.getDialog().dataGatherThread != null) {
					this.getDialog().dataGatherThread.stopDataGatheringThread(false);
				}
				if (this.getDialog().boundsComposite != null && !this.getDialog().isDisposed()) this.getDialog().boundsComposite.redraw();
				this.serialPort.close();
			}
		}
	}
}
