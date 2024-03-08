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
package gde.device.smmodellbau;

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
import gde.device.PropertyType;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.CalculationThread;
import gde.utils.StringHelper;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class LiPoWatch extends DeviceConfiguration implements IDevice {
	final static Logger						log															= Logger.getLogger(LiPoWatch.class.getName());

	public static final String		LOV_A1_CHECKED									= "A1Checked=";																																																						//$NON-NLS-1$
	public static final String		LOV_A1_NAME											= "A1Name=";																																																								//$NON-NLS-1$
	public static final String		LOV_A1_OFFSET										= "A1Offset=";																																																							//$NON-NLS-1$
	public static final String		LOV_A1_FACTOR										= "A1Faktor=";																																																							//$NON-NLS-1$
	public static final String		LOV_A1_UNIT											= "A1Einheit=";																																																						//$NON-NLS-1$

	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_11	= new String[] { //11=a1Value
																																LiPoWatch.LOV_A1_CHECKED, LiPoWatch.LOV_A1_NAME, LiPoWatch.LOV_A1_UNIT, LiPoWatch.LOV_A1_OFFSET, LiPoWatch.LOV_A1_FACTOR };

	public final static String		A1_FACTOR												= "a1_" + IDevice.FACTOR;																																																	//$NON-NLS-1$
	public final static String		A1_OFFSET												= "a1_" + IDevice.OFFSET;																																																	//$NON-NLS-1$

	public final static String		FIRMEWARE_VERSION								= "Firmware";																																																							//$NON-NLS-1$
	public final static String		SERIAL_NUMBER										= "S/N";																																																										//$NON-NLS-1$

	final DataExplorer	application;
	final LiPoWatchSerialPort			serialPort;
	final LiPoWatchDialog					dialog;

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public LiPoWatch(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.smmodellbau.lipowatch.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new LiPoWatchSerialPort(this, this.application);
		this.dialog = new LiPoWatchDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public LiPoWatch(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.smmodellbau.lipowatch.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new LiPoWatchSerialPort(this, this.application);
		this.dialog = new LiPoWatchDialog(this.application.getShell(), this);
		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {

		lov2osdMap.put(LiPoWatch.LOV_A1_CHECKED, Record.IS_ACTIVE + "=_" + "BOOLEAN"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LiPoWatch.LOV_A1_NAME, Record.NAME);
		lov2osdMap.put(LiPoWatch.LOV_A1_UNIT, Record.UNIT);
		lov2osdMap.put(LiPoWatch.LOV_A1_OFFSET, IDevice.OFFSET + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LiPoWatch.LOV_A1_FACTOR, IDevice.FACTOR + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$

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
		String recordSetInfo = GDE.STRING_EMPTY;
		for (int j = 0; j < this.getNumberOfMeasurements(1); j++) {
			StringBuilder recordConfigData = new StringBuilder();
			if (j == 18) {//11=a1Value LOV_CONFIG_DATA_KEYS_UNILOG_11
				HashMap<String, String> configData = StringHelper.splitString(header.get(GDE.LOV_CONFIG_DATA), GDE.DATA_DELIMITER, LiPoWatch.LOV_CONFIG_DATA_KEYS_UNILOG_11);
				for (String lovKey : LiPoWatch.LOV_CONFIG_DATA_KEYS_UNILOG_11) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			recordSetInfo = recordSetInfo + GDE.RECORDS_PROPERTIES + recordConfigData.toString() + Record.END_MARKER;
		}

		return recordSetInfo;
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device
	 */
	public int getLovDataByteSize() {
		return 37;
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException
	 */
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int[] points = new int[this.getNumberOfMeasurements(1)];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		int offset = 0; //offset data pointer while non constant data length
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
//			length = (tmp1ReadBuffer[0] & 0x7F);
//			tmp2ReadBuffer = new byte[length-1];
//			this.read(tmp2ReadBuffer, 4000);
//			readBuffer = new byte[length];
//			readBuffer[0] = tmp1ReadBuffer[0];
//			System.arraycopy(tmp2ReadBuffer, 0, readBuffer, 1, length-1);
			int length = (dataBuffer[offset] & 0x7F);
			byte[] readBuffer = new byte[length];

			System.arraycopy(dataBuffer, offset+4, readBuffer, 0, length);
			recordSet.addPoints(convertDataBytes(points, readBuffer));
			offset += (length+4);

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
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		StringBuilder sb = new StringBuilder();
		int tmpValue = 0;
		int totalVotage = 0;
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature , 4=Balance, 5=cell voltage, 6=cell voltage, 7=cell voltage, ....

		//Servoimpuls_in
		points[1] = (((dataBuffer[6] & 0xFF) << 8) + (dataBuffer[5] & 0xFF) & 0xFFF0) / 16 * 1000;
		if (LiPoWatch.log.isLoggable(Level.FINE)) sb.append("(1)" + points[16]).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		//Servoimpuls_out
		points[2] = (((dataBuffer[8] & 0xFF) << 8) + (dataBuffer[7] & 0xFF) & 0xFFF0) / 16 * 1000;
		if (LiPoWatch.log.isLoggable(Level.FINE)) sb.append("(2)" + points[17]).append("; "); //$NON-NLS-1$ //$NON-NLS-2$

		//Temp_intern
		tmpValue = ((dataBuffer[14] & 0xFF) << 8) + (dataBuffer[13] & 0xFF);
		points[3] = tmpValue <= 32768 ? tmpValue * 10 : (tmpValue - 65536) * 10;
		if (LiPoWatch.log.isLoggable(Level.FINE)) sb.append("(3)" + points[18]).append("; "); //$NON-NLS-1$ //$NON-NLS-2$

		// number cells (first measurement might be wrong, so use avarage if possible)
		int numberCells = (dataBuffer[5] & 0x0F);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "numberCells = " + numberCells); //$NON-NLS-1$
		// read cell voltage values
		int i;
		for (i = 0; i < numberCells; i++) {
			tmpValue = ((dataBuffer[2 * i + 16] & 0xFF) << 8) + (dataBuffer[2 * i + 15] & 0xFF);
			points[i + 5] = (tmpValue <= 32786 ? tmpValue * 2 : (tmpValue - 65536) * 2); //cell voltage
			if (LiPoWatch.log.isLoggable(Level.FINE)) sb.append("(" + (i + 4) + ")" + points[1]).append("; "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			totalVotage += points[i + 5];
			maxVotage = points[i + 5] > maxVotage ? points[i + 5] : maxVotage;
			minVotage = points[i + 5] < minVotage ? points[i + 5] : minVotage;
		}
		//measurement modus absolute/relative
		boolean isRelative = ((dataBuffer[9] & 0xF0) >> 4) == 1;
		points[0] = isRelative ? totalVotage : points[i + 4]; // total battery voltage
		if (LiPoWatch.log.isLoggable(Level.FINE)) sb.insert(0, "(0)" + points[0] + "; "); //$NON-NLS-1$ //$NON-NLS-2$

		points[4] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
		if (LiPoWatch.log.isLoggable(Level.FINE)) sb.append("(" + (i + 4) + ")" + points[1]).append("; "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, sb.toString());
		return points;
	}

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal busy to user
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
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			System.arraycopy(dataBuffer, i * dataBufferSize, convertBuffer, 0, dataBufferSize);

			// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=Balance, 5=cell voltage, 6=cell voltage, 7=cell voltage, ....
			points[0] = (((convertBuffer[0] & 0xff) << 24) + ((convertBuffer[1] & 0xff) << 16) + ((convertBuffer[2] & 0xff) << 8) + ((convertBuffer[3] & 0xff) << 0));
			points[1] = (((convertBuffer[4] & 0xff) << 24) + ((convertBuffer[5] & 0xff) << 16) + ((convertBuffer[6] & 0xff) << 8) + ((convertBuffer[7] & 0xff) << 0));
			points[2] = (((convertBuffer[8] & 0xff) << 24) + ((convertBuffer[9] & 0xff) << 16) + ((convertBuffer[10] & 0xff) << 8) + ((convertBuffer[11] & 0xff) << 0));
			points[3] = (((convertBuffer[12] & 0xff) << 24) + ((convertBuffer[13] & 0xff) << 16) + ((convertBuffer[14] & 0xff) << 8) + ((convertBuffer[15] & 0xff) << 0));
			points[4] = 0;

			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			for (int j = 0; j < points.length - 5; j++) {
				points[j + 5] = (((convertBuffer[16 + (j * 4)] & 0xff) << 24) + ((convertBuffer[17 + (j * 4)] & 0xff) << 16) + ((convertBuffer[18 + (j * 4)] & 0xff) << 8) + ((convertBuffer[19 + (j * 4)] & 0xff) << 0));
				if (points[j + 5] > 0) {
					maxVotage = points[j + 5] > maxVotage ? points[j + 5] : maxVotage;
					minVotage = points[j + 5] < minVotage ? points[j + 5] : minVotage;
				}
			}
			//calculate balance on the fly
			points[4] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
			recordSet.addPoints(points);

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
			double offset = 0.0;
			double factor = 1.0;
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				switch (record.getOrdinal()) {
				case 3: //3=temperature analog outlet
					offset = record.getOffset(); // != 0 if curve has an defined offset
					factor = record.getFactor(); // != 1 if a unit translation is required
					dataTableRow[index + 1] = record.getDecimalFormat().format((offset + record.realGet(rowIndex) / 1000.0) * factor);
					break;
				case 4: //4=Balance [mV]
					offset = record.getOffset();
					factor = record.getFactor();
					dataTableRow[index + 1] = record.getDecimalFormat().format((offset + record.realGet(rowIndex) / 1000.0) * factor);
					break;
				default:
					if(index > 4 && record.getUnit().equals("V")) //cell voltage BC6 no temperature measurements
						dataTableRow[index + 1] = String.format("%.3f", (record.realGet(rowIndex) / 1000.0));
					else
						dataTableRow[index + 1] = record.getDecimalFormat().format(record.realGet(rowIndex) / 1000.0);
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
		double newValue = value;

		// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=Balance, 5=cell voltage, 6=cell voltage, 7=cell voltage, ....
		PropertyType property = null;
		if (record.getOrdinal() == 3) {//3=temperature [°C]
			property = record.getProperty(LiPoWatch.A1_FACTOR);
			double factor = property != null ? Double.valueOf(property.getValue()).doubleValue() : 1.0;
			property = record.getProperty(LiPoWatch.A1_FACTOR);
			double offset = property != null ? Double.valueOf(property.getValue()).doubleValue() : 0.0;
			newValue = value * factor + offset;
		}
		else if (record.getOrdinal() == 4) {//4=Balance [mV]
			double factor =  record.getFactor();
			double offset = record.getOffset();
			newValue = value * factor + offset;
		}

		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		double newValue = value;

		// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=Balance, 5=cell voltage, 6=cell voltage, 7=cell voltage, ....
		PropertyType property = null;
		if (record.getOrdinal() == 3) {//3=temperature [°C]
			property = record.getProperty(LiPoWatch.A1_FACTOR);
			double factor = property != null ? Double.valueOf(property.getValue()).doubleValue() : 1.0;
			property = record.getProperty(LiPoWatch.A1_FACTOR);
			double offset = property != null ? Double.valueOf(property.getValue()).doubleValue() : 0.0;
			newValue = (value - offset) / factor;
		}
		else if (record.getOrdinal() == 4) {//4=Balance [mV]
			double factor =  record.getFactor();
			double offset = record.getOffset();
			newValue = (value - offset) / factor;
		}

		return newValue;
	}

	/**
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 */
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int displayableCounter = 0;

		for (int i = 0; i < recordSet.size(); ++i) {
			Record record = recordSet.get(i);
			boolean hasReasonableData = record.hasReasonableData();
			//record.setVisible(record.isActive() && hasReasonableData);
			//if (log.isLoggable(Level.FINER)) log.log(Level.FINER, record.getName() + ".setVisible = " + hasReasonableData);
			record.setDisplayable(hasReasonableData);
			if (hasReasonableData) ++displayableCounter;
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, record.getName() + " setDisplayable=" + hasReasonableData); //$NON-NLS-1$
		}
		recordSet.setConfiguredDisplayable(displayableCounter);

		if (LiPoWatch.log.isLoggable(Level.FINE)) {
			for (int i = 0; i < recordSet.size(); i++) {
				Record record = recordSet.get(i);
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, record.getName() + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		int displayableCounter = 0;

		for (int i = 0; i < recordSet.size(); ++i) {
			Record record = recordSet.get(i);
			if (record.isActive() && record.isDisplayable()) {
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
	}

	/**
	 * @return the dialog
	 */
	@Override
	public LiPoWatchDialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the serialPort
	 */
	@Override
	public LiPoWatchSerialPort getCommunicationPort() {
		return this.serialPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION, CalculationThread.REGRESSION_INTERVAL_SEC, CalculationThread.REGRESSION_TYPE };
	}

	/**
	 * enhance initial record set comment device specific
	 * LiPoWatch has serial number and a firmeware version
	 * @param recordSet
	 */
	public void updateInitialRecordSetComment(RecordSet recordSet) {
		recordSet.setRecordSetDescription(String.format("%s; \n%s : %s; %s : %s; ", //$NON-NLS-1$
				recordSet.getRecordSetDescription(), LiPoWatch.SERIAL_NUMBER, this.getDialog().serialNumber, LiPoWatch.FIRMEWARE_VERSION, this.getDialog().lipoWatchVersion));
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
						this.getDialog().liveThread = new LiPoWatchLiveGatherer(this.application, this, this.serialPort, this.getDialog());
						this.getDialog().setButtonStateLiveGatherer(false);
						this.getDialog().liveThread.start();
					}
				}
				catch (SerialPortException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage()}));
					this.getDialog().setButtonStateLiveGatherer(true);
				}
				catch (ApplicationConfigurationException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
					this.getDialog().setButtonStateLiveGatherer(true);
				}
				catch (Throwable t) {
					log.log(Level.SEVERE, t.getMessage(), t);
					this.getDialog().setButtonStateLiveGatherer(true);
				}
			}
			else {
				if (this.getDialog().liveThread != null) {
					this.getDialog().liveThread.stopTimerThread();
				}
				if (this.getDialog().gatherThread != null) {
					this.getDialog().gatherThread.setThreadStop();
				}
				this.serialPort.close();
				this.getDialog().setButtonStateLiveGatherer(true);
			}
		}
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public int[] getCellVoltageOrdinals() {
		// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=cell voltage, 5=cell voltage, 6=cell voltage, ....
		return new int[] { 0, 3 };
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
		//check for LiPoWatch file contained record properties for containing balance curve
		String[] recordKeys = recordSet.getRecordNames();
		Vector<String> cleanedRecordNames = new Vector<String>();
		if (!this.containsBalance(fileRecordsProperties)) {
			for (String tmpRecordName : recordKeys) {
				if (!tmpRecordName.toLowerCase().contains("balance")) {
					cleanedRecordNames.add(tmpRecordName);
				}
			}
			recordKeys = cleanedRecordNames.toArray(new String[1]);
		}

		// check if the file content fits measurements form device properties XML which was used to create the record set and remove not required cell voltage measurements
		if (fileRecordsProperties.length != recordKeys.length) {
			for (int j = fileRecordsProperties.length; j < recordKeys.length; j++) {
				recordSet.remove(recordKeys[j]);
				log.log(Level.FINER, "removed record " + recordKeys[j]);
			}
			//update recordKeys to reflect change
			recordKeys = this.crossCheckMeasurements(fileRecordsProperties, recordSet);
		}
		return recordKeys;
	}

	/**
	 * check if a record named Balance is contained
	 * @param recordsProperties
	 * @return
	 */
	private boolean containsBalance(String[] recordsProperties) {
		boolean isContained = false;
		for (String recordProperties : recordsProperties) {
			isContained = recordProperties.toLowerCase().indexOf("balance", 0) > -1;
			if (isContained) break;
		}
		return isContained;
	}
}
