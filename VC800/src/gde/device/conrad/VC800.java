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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.conrad;

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

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

/**
 * VC8XX device class
 * @author Winfried Brügmann
 */
public class VC800 extends DeviceConfiguration implements IDevice {
	final static Logger						log	= Logger.getLogger(VC800.class.getName());
	
	public final static String		INPUT_TYPE					= "input_type"; //$NON-NLS-1$
	public final static String		INPUT_SYMBOL				= "input_symbol"; //$NON-NLS-1$
	public final static String		INPUT_UNIT					= "input_unit"; //$NON-NLS-1$

	final DataExplorer	application;
	final VC800SerialPort					serialPort;
	final VC800Dialog							dialog;

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public VC800(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.conrad.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new VC800SerialPort(this, this.application);
		this.dialog = new VC800Dialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public VC800(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.conrad.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new VC800SerialPort(this, this.application);
		this.dialog = new VC800Dialog(this.application.getShell(), this);
		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
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
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGW0022));
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 20;  // sometimes first 4 bytes give the length of data + 4 bytes for number
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
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {		
		int lovDataSize = this.getLovDataByteSize();
		int deviceDataBufferSize = 8;
		byte[] convertBuffer = new byte[deviceDataBufferSize];
		int[] points = new int[this.getNumberOfMeasurements(1)];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		for (int i = 0; i < recordDataSize; i++) { 
			System.arraycopy(dataBuffer, i*lovDataSize, convertBuffer, 0, deviceDataBufferSize);
			recordSet.addPoints(convertDataBytes(points, convertBuffer));
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
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
		
		if (log.isLoggable(Level.FINER)) {
			StringBuilder sb = new StringBuilder();
			for (byte b : dataBuffer) {
				sb.append(String.format("%02x", b)).append(" "); //$NON-NLS-1$ //$NON-NLS-2$
			}
			log.log(Level.FINER, sb.toString());
		}
		points[0] =  1000 * getDigit(((dataBuffer[1] & 0x07) << 4) | (dataBuffer[2] & 0x0f));
    points[0] += 100 * getDigit(((dataBuffer[3] & 0x07) << 4) | (dataBuffer[4] & 0x0f));
    points[0] += 10 * getDigit(((dataBuffer[5] & 0x07) << 4) | (dataBuffer[6] & 0x0f));
    points[0] += 1 * getDigit(((dataBuffer[7] & 0x07) << 4) | (dataBuffer[8] & 0x0f));
		log.log(Level.FINEST, "digits = " + points[0]); //$NON-NLS-1$

    if 			((dataBuffer[3] & 0x08) > 0) points[0] *= 1;		// /= 1000.0;
    else if ((dataBuffer[5] & 0x08) > 0) points[0] *= 10 ; 	// /= 100.0;
    else if ((dataBuffer[7] & 0x08) > 0) points[0] *= 100; 	// /= 10.0;
    else																 points[0] *= 1000; // /= 1.0
    
    if ((dataBuffer[1] & 0x08) > 0) points[0] *= -1;  

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
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.size()];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		Vector<Integer> timeStamps = new Vector<Integer>(1,1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		int timeStampBufferSize = 0;
		if(!recordSet.isTimeStepConstant()) {
			timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
			byte[] timeStampBuffer = new byte[timeStampBufferSize];
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8) + ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*2500)/recordDataSize), sThreadId);
			}
		}
		log.log(Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString());
		
		for (int i = 0; i < recordDataSize; i++) {
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize+timeStampBufferSize);
			System.arraycopy(dataBuffer, i*dataBufferSize+timeStampBufferSize, convertBuffer, 0, dataBufferSize);
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));

			if(recordSet.isTimeStepConstant()) 
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i)/10.0);
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*2500)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
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
	 * get digit from display
	 * @param select
	 * @return digit value
	 */
	public int getDigit(int select) {
		int digit = 0;
		switch (select) {
		case 0x7d:
			digit = 0;
			break;
		case 0x05:
			digit = 1;
			break;
		case 0x5b:
			digit = 2;
			break;
		case 0x1f:
			digit = 3;
			break;
		case 0x27:
			digit = 4;
			break;
		case 0x3e:
			digit = 5;
			break;
		case 0x7e:
			digit = 6;
			break;
		case 0x15:
			digit = 7;
			break;
		case 0x7f:
			digit = 8;
			break;
		case 0x3f:
			digit = 9;
			break;
		}

		return digit;
	}
	
	/**
	 * query battery voltage level
	 * @param buffer
	 * @return true if battey voltage level detected as low
	 */
	public boolean isBatteryLevelLow(byte[] buffer) {
		return (buffer[12] & 0x01) != 1;
	}

	/**
	 * get measurement info (type, symbol, unit)
	 * @param buffer
	 * @return measurement unit as string
	 */
	public HashMap<String, String> getMeasurementInfo(byte[] buffer, HashMap<String, String> measurementInfo) {
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "  buffer : " + StringHelper.byte2Hex2CharString(buffer, buffer.length));
		}
		String unit = ""; //$NON-NLS-1$
		if 			((buffer[9] & 0x02) > 0)	unit = "k"; //$NON-NLS-1$
		else if ((buffer[9] & 0x04) > 0)	unit = "n"; //$NON-NLS-1$
		else if ((buffer[9] & 0x08) > 0)	unit = "µ"; //$NON-NLS-1$
		else if ((buffer[10] & 0x02) > 0)	unit = "M"; //$NON-NLS-1$
		else if ((buffer[10] & 0x08) > 0)	unit = "m"; //$NON-NLS-1$
		else if ((buffer[10] & 0x04) > 0) unit = "%"; //$NON-NLS-1$

		if ((buffer[11] & 0x04) > 0)			unit += "Ω"; //$NON-NLS-1$
		else if ((buffer[11] & 0x08) > 0)	unit += "F"; //$NON-NLS-1$
		else if ((buffer[12] & 0x02) > 0)	unit += "Hz"; //$NON-NLS-1$
		else if ((buffer[12] & 0x04) > 0)	unit += "V"; //$NON-NLS-1$
		else if ((buffer[12] & 0x08) > 0)	unit += "A"; //$NON-NLS-1$
		//else if ((buffer[13] & 0x01) > 0) unit += "°C"; //$NON-NLS-1$
		else 															unit += "°C"; //$NON-NLS-1$
		
		measurementInfo.put(VC800.INPUT_UNIT, unit);
		
		String typeSymbol = Messages.getString(MessageIds.GDE_MSGT1500);
		if 			(unit.endsWith("V")) 		typeSymbol = Messages.getString(MessageIds.GDE_MSGT1501); //$NON-NLS-1$
		else if (unit.endsWith("A")) 		typeSymbol = Messages.getString(MessageIds.GDE_MSGT1503); //$NON-NLS-1$
		else if (unit.endsWith("Ω")) 		typeSymbol = Messages.getString(MessageIds.GDE_MSGT1504); //$NON-NLS-1$
		else if (unit.endsWith("F")) 		typeSymbol = Messages.getString(MessageIds.GDE_MSGT1505); //$NON-NLS-1$
		else if (unit.endsWith("Hz")) 	typeSymbol = Messages.getString(MessageIds.GDE_MSGT1506); //$NON-NLS-1$
		else if (unit.endsWith("°C")) 	typeSymbol = Messages.getString(MessageIds.GDE_MSGT1507); //$NON-NLS-1$
		
		try {
			measurementInfo.put(VC800.INPUT_TYPE, typeSymbol.split(" ")[0]); //$NON-NLS-1$
			measurementInfo.put(VC800.INPUT_SYMBOL, typeSymbol.split(" ")[1]); //$NON-NLS-1$
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
		String mode = Messages.getString(MessageIds.GDE_MSGT1510);
		if ((buffer[0] & 0x02) > 0) mode = Messages.getString(MessageIds.GDE_MSGT1511);
		
		if ((buffer[12] & 0x0c) > 0 && (buffer[0] & 0x04) > 0) mode += Messages.getString(MessageIds.GDE_MSGT1512);
		else if ((buffer[12] & 0x0c) > 0 && (buffer[0] & 0x08) > 0) mode += Messages.getString(MessageIds.GDE_MSGT1513);
		else if ((buffer[9] & 0x01) > 0)	mode += Messages.getString(MessageIds.GDE_MSGT1514);
		else if ((buffer[11] & 0x04) > 0)	mode += Messages.getString(MessageIds.GDE_MSGT1515);
		else if ((buffer[11] & 0x08) > 0)	mode += Messages.getString(MessageIds.GDE_MSGT1516);
		else if ((buffer[12] & 0x02) > 0)	mode += Messages.getString(MessageIds.GDE_MSGT1517);
		else if ((buffer[13] & 0x01) > 0) mode += Messages.getString(MessageIds.GDE_MSGT1518);
		
		return mode;
	}
	
	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double translateValue(Record record, double value) {
		// 0=Spannung oder Strom oder ..
		double newValue = value; // no factor, offset, reduction or other supported
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		// 0=Spannung oder Strom oder ..
		double newValue = value; // no factor, offset, reduction or other supported
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		recordSet.setAllVisibleAndDisplayable();
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		log.log(Level.FINE, "no update required for " + recordSet.getName()); //$NON-NLS-1$
	}

	/**
	 * @return the dialog
	 */
	@Override
	public VC800Dialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the serialPort
	 */
	@Override
	public VC800SerialPort getCommunicationPort() {
		return this.serialPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] {IDevice.OFFSET, IDevice.FACTOR};
	}
	
	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
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
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage()}));
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
