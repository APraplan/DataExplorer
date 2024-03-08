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

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.smmodellbau.unilog.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.CalculationThread;
import gde.utils.LinearRegression;
import gde.utils.QuasiLinearRegression;
import gde.utils.StringHelper;
import gde.exception.NoSuchPortException;

/**
 * UniLog default device implementation, just copied from Sample project
 * @author Winfried Bruegmann
 */
public class UniLog extends DeviceConfiguration implements IDevice {
	final static Logger						log	= Logger.getLogger(UniLog.class.getName());

	public static final String	LOV_N_100_W = "n100W="; //$NON-NLS-1$
	public static final String	LOV_CURRENT_OFFSET = "Stromoffset="; //$NON-NLS-1$
	public static final String	LOV_NUMBER_MOTOR = "AnzahlMotoren="; //$NON-NLS-1$
	public static final String	LOV_NUMBER_CELLS = "AnzahlZellen="; //$NON-NLS-1$
	public static final String	LOV_RPM2_FACTOR = "DrehzahlFaktor="; //$NON-NLS-1$
	public static final String	LOV_CURRENT_INVERT = "StromInvertieren="; //$NON-NLS-1$
	public static final String	LOV_RPM_CHECKED = "RpmChecked="; //$NON-NLS-1$
	public static final String	LOV_A1_CHECKED = "A1Checked="; //$NON-NLS-1$
	public static final String	LOV_A2_CHECKED = "A2Checked="; //$NON-NLS-1$
	public static final String	LOV_A3_CHECKED = "A3Checked="; //$NON-NLS-1$
	public static final String	LOV_RPM_NAME = "RpmName="; //$NON-NLS-1$
	public static final String	LOV_A1_NAME = "A1Name="; //$NON-NLS-1$
	public static final String	LOV_A2_NAME = "A2Name="; //$NON-NLS-1$
	public static final String	LOV_A3_NAME = "A3Name="; //$NON-NLS-1$
	public static final String	LOV_RPM_OFFSET = "RpmOffset="; //$NON-NLS-1$
	public static final String	LOV_A1_OFFSET = "A1Offset="; //$NON-NLS-1$
	public static final String	LOV_A2_OFFSET = "A2Offset="; //$NON-NLS-1$
	public static final String	LOV_A3_OFFSET = "A3Offset="; //$NON-NLS-1$
	public static final String	LOV_RPM_FACTOR = "RpmFaktor="; //$NON-NLS-1$
	public static final String	LOV_A1_FACTOR = "A1Faktor="; //$NON-NLS-1$
	public static final String	LOV_A2_FACTOR = "A2Faktor="; //$NON-NLS-1$
	public static final String	LOV_A3_FACTOR = "A3Faktor="; //$NON-NLS-1$
	public static final String	LOV_RPM_UNIT = "RpmEinheit="; //$NON-NLS-1$
	public static final String	LOV_A1_UNIT = "A1Einheit="; //$NON-NLS-1$
	public static final String	LOV_A2_UNIT = " A2Einheit="; //$NON-NLS-1$
	public static final String	LOV_A3_UNIT = " A3Einheit="; //$NON-NLS-1$

//	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG	= new String[] { LOV_N_100_W, LOV_NUMBER_CELLS, LOV_RPM_CHECKED,
//		LOV_A1_CHECKED, LOV_A2_CHECKED, LOV_A3_CHECKED, LOV_RPM_NAME, LOV_A1_NAME, LOV_A2_NAME, LOV_A3_NAME, LOV_RPM_OFFSET,
//		LOV_A1_OFFSET, LOV_A2_OFFSET, LOV_A3_OFFSET, LOV_RPM_FACTOR, LOV_A1_FACTOR, LOV_A2_FACTOR, LOV_A3_FACTOR,
//		LOV_RPM_UNIT, LOV_A1_UNIT, LOV_A1_UNIT, LOV_A2_UNIT, LOV_A3_UNIT,
//		LOV_CURRENT_OFFSET, LOV_NUMBER_MOTOR, LOV_RPM2_FACTOR, LOV_CURRENT_INVERT,
//	};

	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_2		= new String[] { 	// 2=current
		LOV_CURRENT_OFFSET, LOV_CURRENT_INVERT };
	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_6		= new String[] { 	// 6=votagePerCell
		LOV_NUMBER_CELLS };
	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_7		= new String[] {	// 7=revolutionSpeed
		LOV_RPM_CHECKED, LOV_RPM_NAME, LOV_RPM_UNIT, LOV_RPM_FACTOR, LOV_RPM_OFFSET, LOV_RPM2_FACTOR, LOV_NUMBER_MOTOR };
	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_8		= new String[] { 	// 8=efficiency
		LOV_N_100_W };																																																																																																																									//A1 Modus -> 0==Temperatur, 1==Millivolt, 2=Speed 250, 3=Speed 400
	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_11	= new String[] { 	//11=a1Value
		LOV_A1_CHECKED, LOV_A1_NAME, LOV_A1_UNIT, LOV_A1_OFFSET, LOV_A1_FACTOR	};
	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_12	= new String[] { 	//12=a2Value
		LOV_A2_CHECKED, LOV_A2_NAME, LOV_A2_UNIT, LOV_A2_OFFSET, LOV_A2_FACTOR };
	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_13	= new String[] { 	//13=a3Value
		LOV_A3_CHECKED, LOV_A3_NAME, LOV_A3_UNIT, LOV_A3_OFFSET, LOV_A3_FACTOR };

	public final static String		A1_FACTOR									= "a1_"+IDevice.FACTOR; //$NON-NLS-1$
	public final static String		A1_OFFSET									= "a1_"+IDevice.OFFSET; //$NON-NLS-1$
	public final static String		A2_FACTOR									= "a2_"+IDevice.FACTOR; //$NON-NLS-1$
	public final static String		A2_OFFSET									= "a2_"+IDevice.OFFSET; //$NON-NLS-1$
	public final static String		A3_FACTOR									= "a3_"+IDevice.FACTOR; //$NON-NLS-1$
	public final static String		A3_OFFSET									= "a3_"+IDevice.OFFSET; //$NON-NLS-1$

	public final static String		NUMBER_CELLS							= MeasurementPropertyTypes.NUMBER_CELLS.value();
	public final static String		PROP_N_100_W							= MeasurementPropertyTypes.PROP_N_100_W.value();

	public final static String		IS_INVERT_CURRENT					= MeasurementPropertyTypes.IS_INVERT_CURRENT.value();
	public final static String		CURRENT_OFFSET						= IDevice.OFFSET;

	public final static String		NUMBER_MOTOR							= MeasurementPropertyTypes.NUMBER_MOTOR.value();
	public final static String		REVOLUTION_FACTOR					= MeasurementPropertyTypes.REVOLUTION_FACTOR.value();
	public final static String		RPM_FACTOR								= IDevice.FACTOR;
	public final static String		DO_SUBTRACT_FIRST					= MeasurementPropertyTypes.DO_SUBTRACT_FIRST.value();
	public final static String		DO_SUBTRACT_LAST					= MeasurementPropertyTypes.DO_SUBTRACT_LAST.value();

	public final static String		FIRMEWARE_VERSION					= "Firmware"; //$NON-NLS-1$
	public final static String		SERIAL_NUMBER							= "S/N"; //$NON-NLS-1$

	final DataExplorer	application;
	final UniLogSerialPort				serialPort;
	final UniLogDialog						dialog;

	/**
	 * constructor using properties file
	 * @param deviceProperties
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public UniLog(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.smmodellbau.unilog.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = this.application != null ? new UniLogSerialPort(this, this.application) : new UniLogSerialPort(this, null);
		this.dialog = this.application != null ? new UniLogDialog(this.application.getShell(), this) : new UniLogDialog(new Shell(Display.getDefault()), this);
		if (this.application != null && this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT1376), Messages.getString(MessageIds.GDE_MSGT1375));
		UniLogSerialPort.TIME_OUT_MS = this.getReadTimeOut();
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 * @throws NoSuchPortException
	 */
	public UniLog(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.smmodellbau.unilog.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = this.application != null ? new UniLogSerialPort(this, this.application) : new UniLogSerialPort(this, null);
		this.dialog = this.application != null ? new UniLogDialog(this.application.getShell(), this) : new UniLogDialog(new Shell(Display.getDefault()), this);
		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT1376), Messages.getString(MessageIds.GDE_MSGT1375));
		UniLogSerialPort.TIME_OUT_MS = this.getReadTimeOut();
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {

		lov2osdMap.put(LOV_CURRENT_OFFSET, CURRENT_OFFSET + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_CURRENT_INVERT, IS_INVERT_CURRENT	+ "=_" + "BOOLEAN"); //$NON-NLS-1$ //$NON-NLS-2$

		lov2osdMap.put(LOV_NUMBER_CELLS, NUMBER_CELLS + "=_" + "INTEGER"); //$NON-NLS-1$ //$NON-NLS-2$

		lov2osdMap.put(LOV_RPM_CHECKED, 	Record.IS_ACTIVE	+ "=_" + "BOOLEAN"); //$NON-NLS-1$ //$NON-NLS-2$
		//lov2osdMap.put(LOV_RPM_NAME, 		Record.NAME);
		//lov2osdMap.put(LOV_RPM_UNIT, 		Record.UNIT);
		lov2osdMap.put(LOV_RPM_OFFSET, 		IDevice.OFFSET 		+ "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_RPM_FACTOR, 		IDevice.FACTOR 		+ "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_NUMBER_MOTOR, 	NUMBER_MOTOR);
		lov2osdMap.put(LOV_RPM2_FACTOR, 	REVOLUTION_FACTOR + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_N_100_W, 			PROP_N_100_W			+ "=_" + "INTEGER"); //$NON-NLS-1$ //$NON-NLS-2$

		lov2osdMap.put(LOV_A1_CHECKED, 		Record.IS_ACTIVE	+ "=_" + "BOOLEAN"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A2_CHECKED, 		Record.IS_ACTIVE	+ "=_" + "BOOLEAN"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A3_CHECKED, 		Record.IS_ACTIVE	+ "=_" + "BOOLEAN"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A1_NAME, 			Record.NAME);
		lov2osdMap.put(LOV_A2_NAME, 			Record.NAME);
		lov2osdMap.put(LOV_A3_NAME, 			Record.NAME);
		lov2osdMap.put(LOV_A1_UNIT, 			Record.UNIT);
		lov2osdMap.put(LOV_A2_UNIT, 			Record.UNIT);
		lov2osdMap.put(LOV_A3_UNIT, 			Record.UNIT);
		lov2osdMap.put(LOV_A1_OFFSET, 		IDevice.OFFSET + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A2_OFFSET, 		IDevice.OFFSET + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A3_OFFSET, 		IDevice.OFFSET + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A1_FACTOR, 		IDevice.FACTOR + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A2_FACTOR, 		IDevice.FACTOR + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A3_FACTOR, 		IDevice.FACTOR + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$

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
		String recordSetInfo = GDE.STRING_EMPTY;
		for (int j = 0; j < this.getNumberOfMeasurements(channelNumber); j++) {
			StringBuilder recordConfigData = new StringBuilder();
			if (j == 2) {// 6=votage LOV_CONFIG_DATA_KEYS_UNILOG_2
				HashMap<String, String> configData = StringHelper.splitString(header.get(GDE.LOV_CONFIG_DATA), GDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_2);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_2) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			else if (j == 6) {// 6=votagePerCell LOV_CONFIG_DATA_KEYS_UNILOG_6
				HashMap<String, String> configData = StringHelper.splitString(header.get(GDE.LOV_CONFIG_DATA), GDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_6);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_6) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			else if (j == 7) { // 7=revolutionSpeed LOV_CONFIG_DATA_KEYS_UNILOG_7
				HashMap<String, String> configData = StringHelper.splitString(header.get(GDE.LOV_CONFIG_DATA), GDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_7);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_7) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			else if (j == 8) {// 8=efficiency LOV_CONFIG_DATA_KEYS_UNILOG_8
				HashMap<String, String> configData = StringHelper.splitString(header.get(GDE.LOV_CONFIG_DATA), GDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_8);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_8) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			else if (j == 11) {//11=a1Value LOV_CONFIG_DATA_KEYS_UNILOG_11
				HashMap<String, String> configData = StringHelper.splitString(header.get(GDE.LOV_CONFIG_DATA), GDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_11);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_11) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			else if (j == 12) {//12=a2Value LOV_CONFIG_DATA_KEYS_UNILOG_12
				HashMap<String, String> configData = StringHelper.splitString(header.get(GDE.LOV_CONFIG_DATA), GDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_12);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_12) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			else if (j == 13) {//13=a3Value LOV_CONFIG_DATA_KEYS_UNILOG_13
				HashMap<String, String> configData = StringHelper.splitString(header.get(GDE.LOV_CONFIG_DATA), GDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_13);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_13) {
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
	@Override
	public int getLovDataByteSize() {
		return 32;
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
	@Override
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int timeStep_ms = 0;
		int size = this.getLovDataByteSize();
		byte[] readBuffer = new byte[size];
		int[] points = new int[this.getNumberOfMeasurements(1)];
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 2; i < recordDataSize; i++) { // skip UniLog min/max line

			System.arraycopy(dataBuffer, i*size, readBuffer, 0, size);

			// time milli seconds
			if (timeStep_ms == 0) { // set time step for this record set
				timeStep_ms = timeStep_ms + ((readBuffer[3] & 0xFF) << 24) + ((readBuffer[2] & 0xFF) << 16) + ((readBuffer[1] & 0xFF) << 8) + (readBuffer[0] & 0xFF);
				if (timeStep_ms != 0) {
					recordSet.setTimeStep_ms(timeStep_ms);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "timeStep_ms = " + timeStep_ms); //$NON-NLS-1$
				}
			}
			recordSet.addPoints(convertDataBytes(points, readBuffer));

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
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		StringBuilder sb = new StringBuilder();
		String lineSep = GDE.LINE_SEPARATOR;
		int tmpValue = 0;

		// voltageReceiver *** power/drive *** group
		tmpValue = (((dataBuffer[7] & 0xFF) << 8) + (dataBuffer[6] & 0xFF)) & 0x0FFF;
		points[0] = (tmpValue * 10); //0=voltageReceiver
		if (log.isLoggable(Level.FINE)) sb.append("voltageReceiver [V] = " + points[0]).append(lineSep); //$NON-NLS-1$

		// voltage *** power/drive *** group
		tmpValue = (((dataBuffer[9] & 0xFF) << 8) + (dataBuffer[8] & 0xFF));
		if (tmpValue > 32768) tmpValue = tmpValue - 65536;
		points[1] = (tmpValue * 10); //1=voltage
		if (log.isLoggable(Level.FINE)) sb.append("voltage [V] = " + points[1]).append(lineSep); //$NON-NLS-1$

		// current *** power/drive *** group - asymmetric for 400 A sensor
		tmpValue = (((dataBuffer[11] & 0xFF) << 8) + (dataBuffer[10] & 0xFF));
		tmpValue = tmpValue <= 55536 ? tmpValue : (tmpValue - 65536);
		points[2] = tmpValue * 10; //2=current [A]
		if (log.isLoggable(Level.FINE)) sb.append("current [A] = " + points[2]).append(lineSep); //$NON-NLS-1$

		//capacity = cycleCount > 0 ? capacity + ((points[2] * timeStep_ms * 1.0) / 3600) : 0.0;
		//points[3] = capacity.intValue(); //3=capacity [Ah]
		points[3] = 0; //3=capacity [mAh]

		//points[4] = Double.valueOf(1.0 * points[1] * points[2] / 1000).intValue(); //4=power [W]
		points[4] = 0; //4=power [W]

		//points[5] = Double.valueOf(1.0 * points[1] * points[3] / 1000000).intValue(); //5=energy [Wh]
		points[5] = 0; //5=energy [Wh]

		//points[6] = points[1] / numberCells; //6=votagePerCell
		points[6] = 0; //6=votagePerCellS

		// revolution speed *** power/drive *** group
		tmpValue = (((dataBuffer[13] & 0xFF) << 8) + (dataBuffer[12] & 0xFF));
		if (tmpValue > 50000) tmpValue = (tmpValue - 50000) * 10 + 50000;
		points[7] = (tmpValue * 1000); //7=revolutionSpeed
		if (log.isLoggable(Level.FINE)) sb.append("revolution speed [1/min] = " + points[7]).append(lineSep); //$NON-NLS-1$

		//double motorPower = (points[7]*100.0)/prop100WValue;
		//double eta = points[4] > motorPower ? (motorPower*100.0)/points[4] : 0;
		//points[8] = Double.valueOf(eta * 1000).intValue();//8=efficiency
		points[8] = 0; //8=efficiency

		// height *** power/drive *** group
		tmpValue = (((dataBuffer[15] & 0xFF) << 8) + (dataBuffer[14] & 0xFF)) + 20000;
		if (tmpValue > 32768) tmpValue = tmpValue - 65536;
		points[9] = (tmpValue * 100); //9=height
		if (log.isLoggable(Level.FINE)) sb.append("height [m] = " + points[9]).append(lineSep); //$NON-NLS-1$

		points[10] = 0; //10=slope

		// a1Modus -> 0==Temperatur, 1==Millivolt, 2=Speed 250, 3=Speed 400
		int a1Modus = (dataBuffer[7] & 0xF0) >> 4; // 11110000
		a1Modus = a1Modus > 3 ? 3 : a1Modus;
		tmpValue = (((dataBuffer[17] & 0xFF) << 8) + (dataBuffer[16] & 0xFF));
		if (tmpValue > 32768) tmpValue = tmpValue - 65536;
		points[11] = Integer.valueOf(tmpValue * 100); //11=a1Value
		if (log.isLoggable(Level.FINE)) {
			sb.append("a1Modus = " + a1Modus).append(lineSep); //$NON-NLS-1$
			if (a1Modus == 0)
				sb.append("temperatur [°C] = " + points[12]).append(lineSep); //$NON-NLS-1$
			else if (a1Modus == 1)
				sb.append("voltage [mV] = " + points[12]).append(lineSep); //$NON-NLS-1$
			else if (a1Modus == 2)
				sb.append("speed 250 [km/h] = " + points[12]).append(lineSep); //$NON-NLS-1$
			else if (a1Modus == 3)
				sb.append("speed 400 [km/h] = " + points[12]).append(lineSep); //$NON-NLS-1$
		}

		// A2 Modus == 0 -> temperature; A2 Modus == 1 -> impulse time length
		// A2 Modus == 2 -> mills voltage; A2 Modus == 3 -> capacity mAh
		int a2Modus = (dataBuffer[4] & 0x30) >> 4; // 00110000
		if (a2Modus == 0 || a2Modus == 2) {
			tmpValue = (((dataBuffer[19] & 0xEF) << 8) + (dataBuffer[18] & 0xFF));
			tmpValue = tmpValue > 32768 ? (tmpValue - 65536) : tmpValue;
			points[12] = Integer.valueOf(tmpValue * 100); //12=a2Value
		}
		else if (a2Modus == 1 || a2Modus == 3) {
			tmpValue = (((dataBuffer[19] & 0xFF) << 8) + (dataBuffer[18] & 0xFF));
			points[12] = Integer.valueOf(tmpValue * 1000); //12=a2Value
		}
		if (log.isLoggable(Level.FINE)) {
			sb.append("a2Modus = " + a2Modus).append(lineSep); //$NON-NLS-1$
			if (a2Modus == 0)
				sb.append("tempreature [°C] = " + points[12]).append(lineSep); //$NON-NLS-1$
			else if (a2Modus == 1)
				sb.append("impulseTime [us] = " + points[12]).append(lineSep); //$NON-NLS-1$
			else if (a2Modus == 2)
				sb.append("voltage [mV] = " + points[12]).append(lineSep); //$NON-NLS-1$
			else if (a2Modus == 3)
				sb.append("capacity [mAh] = " + points[12]).append(lineSep); //$NON-NLS-1$
		}

		// A3 Modus == 0 -> external temperature sensor; A3 Modus == 1 -> internal temperature
		// A3 Modus == 2 -> external temperature sensor; A3 Modus == 3 -> internal temperature
		int a3Modus = (dataBuffer[4] & 0xC0) >> 6; // 11000000
		tmpValue = (((dataBuffer[21] & 0xEF) << 8) + (dataBuffer[20] & 0xFF));
		if (a3Modus == 0 || a3Modus == 1 || a3Modus == 3) {
			if (tmpValue > 32768) tmpValue = tmpValue - 65536;
			points[13] = Integer.valueOf(tmpValue * 100).intValue(); //13=a3Value
		}
		else if (a3Modus == 2) {
			points[13] = Integer.valueOf(tmpValue * 100).intValue(); //13=a3Value
		}
		if (log.isLoggable(Level.FINE)) {
			sb.append("a3Modus = " + a3Modus).append(lineSep); //$NON-NLS-1$
			if (a3Modus == 0)
				sb.append("external Temperature [°C] = " + points[13]).append(lineSep); //$NON-NLS-1$
			else if (a3Modus == 1)
				sb.append("internal Temperature [°C] = " + points[13]).append(lineSep); //$NON-NLS-1$
			else if (a3Modus == 2)
				sb.append("energy [Wmin] = " + points[13]).append(lineSep); //$NON-NLS-1$
			else if (a3Modus == 3)
				sb.append("voltage [mV] = " + points[13]).append(lineSep); //$NON-NLS-1$
			else
				sb.append("a3Value = " + points[13]).append(lineSep); //$NON-NLS-1$
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, sb.toString());
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
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
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
			}
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString());
			recordSet.setTimeStep_ms(timeStamps.lastElement()/(double)(timeStamps.size()-1)/10.0); //UniLog has constant time step, even if the XML says -1
		}

		for (int i = 0; i < recordDataSize; i++) {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize+timeStampBufferSize);
			System.arraycopy(dataBuffer, i*dataBufferSize+timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			//points[3] = capacity.intValue(); //3=capacity [Ah]
			//points[4] = Double.valueOf(1.0 * points[1] * points[2] / 1000).intValue(); //4=power [W]
			//points[5] = Double.valueOf(1.0 * points[1] * points[3] / 1000000).intValue(); //5=energy [Wh]
			//points[6] = points[1] / numberCells; //6=votagePerCell
			points[7] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
			//points[8] = Double.valueOf(eta * 1000).intValue();//8=efficiency
			points[9] = (((convertBuffer[16]&0xff) << 24) + ((convertBuffer[17]&0xff) << 16) + ((convertBuffer[18]&0xff) << 8) + ((convertBuffer[19]&0xff) << 0));
			//points[10] = slope
			points[11] = (((convertBuffer[20]&0xff) << 24) + ((convertBuffer[21]&0xff) << 16) + ((convertBuffer[22]&0xff) << 8) + ((convertBuffer[23]&0xff) << 0));
			points[12] = (((convertBuffer[24]&0xff) << 24) + ((convertBuffer[25]&0xff) << 16) + ((convertBuffer[26]&0xff) << 8) + ((convertBuffer[27]&0xff) << 0));
			points[13] = (((convertBuffer[28]&0xff) << 24) + ((convertBuffer[29]&0xff) << 16) + ((convertBuffer[30]&0xff) << 8) + ((convertBuffer[31]&0xff) << 0));

			recordSet.addPoints(points);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
		}
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
				double currentOffset = 0;
				double rpmFactor = 1;
				double numberMotor = 1;
				PropertyType property = null;

				switch (record.getOrdinal()) {
				case 0: //voltageReceiver
				case 1: //voltage
					break;
				case 2: //current
					property = record.getProperty(UniLog.CURRENT_OFFSET);
					currentOffset = property != null ? Double.valueOf(property.getValue()).doubleValue() : 0;
					//newValues = value + currentOffset;
					break;
				case 3: //capacity
				case 4: //power
				case 5: //energy
				case 6: //votagePerCell
					break;
				case 7: //revolutionSpeed
					property = record.getProperty(UniLog.RPM_FACTOR);
					rpmFactor = property != null ? Double.valueOf(property.getValue()).doubleValue() : 1.0;
					property = record.getProperty(UniLog.NUMBER_MOTOR);
					numberMotor = property != null ? Double.valueOf(property.getValue()).doubleValue() : 1.0;
					//newValues = value * rpmFactor / numberMotor;
					break;
				case 9: //height
					property = record.getProperty(UniLog.DO_SUBTRACT_FIRST);
					boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
					property = record.getProperty(UniLog.DO_SUBTRACT_LAST);
					boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

					try {
						if (subtractFirst) {
							reduction = record.getFirst()/1000.0;
						}
						else if (subtractLast) {
							reduction = record.getLast()/1000.0;
						}
					}
					catch (Throwable e) {
						log.log(Level.SEVERE, record.getParent().getName() + " " + record.getName() + " " + e.getMessage());
					}
					break;
				case 8: //efficiency
				case 10: //slope
				case 11: //a1Value
				case 12: //a2Value
				case 13: //a3Value
					break;
				default:
					log.log(Level.WARNING, "exceed known record names"); //$NON-NLS-1$
					break;
				}

				dataTableRow[index+1] = record.getDecimalFormat().format((offset + (((record.realGet(rowIndex)/1000.0 + currentOffset) * rpmFactor / numberMotor) - reduction) * factor));
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}
	
	/**
	 * function to prepare a row of record set for export while translating available measurement values.
	 * @return pointer to filled data table row with formated values
	 */
	public String[] prepareRawExportRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			String[] recordNames = recordSet.getRecordNames();
			for (int index = 1, j = 0; index < dataTableRow.length && j < recordNames.length; j++) { //do not touch index 0 with time entry
				final Record record = recordSet.get(recordNames[j]);
				MeasurementType  measurement = this.getMeasurement(recordSet.getChannelConfigNumber(), record.getOrdinal());
				if (!measurement.isCalculation()) {	// only use active records for writing raw data
					dataTableRow[index] = String.format("%d", record.realGet(rowIndex));
					++index;
				}
			}
		} catch (RuntimeException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
		double newValue = value;

		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		PropertyType property = null;
		switch (record.getOrdinal()) {
		case 2: //2=current [A]
			newValue = value + record.getOffset();
			break;

		case 7: //7=revolutionSpeed [1/min]
			property = record.getProperty(UniLog.NUMBER_MOTOR);
			double numberMotor = property != null ? Double.valueOf(property.getValue()).doubleValue() : 1.0;
			newValue = value * record.getFactor() / numberMotor;
			break;

		case 9: //9=height [m]
			property = record.getProperty(UniLog.DO_SUBTRACT_FIRST);
			boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
			property = record.getProperty(UniLog.DO_SUBTRACT_LAST);
			boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

			try {
				if (subtractFirst) {
					newValue = value - record.getFirst()/1000.0;
				}
				else if (subtractLast) {
					newValue = value - record.getLast()/1000.0;
				}
			}
			catch (Throwable e) {
				log.log(Level.SEVERE, record.getAbstractParent().getName() + " " + record.getName() + " " + e.getMessage());
			}
			break;

		case 11: //11=a1Value
		case 12: //12=a2Value
		case 13: //13=a3Value
			newValue = record.getFactor() * value + record.getOffset();
			break;

		default:
			break;
		}

		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		double newValue = value;

		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		PropertyType property = null;
		switch (record.getOrdinal()) {
		case 2: //2=current [A]:
			newValue = value - record.getOffset();
			break;

		case 7: //7=revolutionSpeed [1/min]
			property = record.getProperty(UniLog.NUMBER_MOTOR);
			double numberMotor = property != null ? Double.valueOf(property.getValue()).doubleValue() : 1.0;
			newValue = value * numberMotor / record.getFactor();
			break;

		case 9: //9=height [m]
			property = record.getProperty(UniLog.DO_SUBTRACT_FIRST);
			boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
			property = record.getProperty(UniLog.DO_SUBTRACT_LAST);
			boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

			try {
				if (subtractFirst) {
					newValue = value + record.getFirst()/1000.0;
				}
				else if (subtractLast) {
					newValue = value + record.getLast()/1000.0;
				}
			}
			catch (Throwable e) {
				log.log(Level.SEVERE, record.getAbstractParent().getName() + " " + record.getName() + " " + e.getMessage());
			}
			break;

		case 11: //11=a1Value
		case 12: //12=a2Value
		case 13: //13=a3Value
			newValue = (value - record.getOffset()) / record.getFactor();
			break;

		default:
			break;
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
	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		Record record;
		MeasurementType measurement;
		boolean configChanged = this.isChangePropery();
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		String[] measurementNames = this.getMeasurementNames(channelConfigNumber);
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			measurement = this.getMeasurement(channelConfigNumber, i);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " = " + measurementNames[i]); //$NON-NLS-1$

			// update active state and displayable state if configuration switched with other names
			if (includeReasonableDataCheck) {
				boolean state = record.hasReasonableData();
				log.log(Level.TIME, record.getName() + " hasReasonableData " + state); //$NON-NLS-1$
				record.setActive(state);
				//record.setVisible(state);
				record.setDisplayable(state);
			}
			else if (record.isActive() != measurement.isActive()) {
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		if (!includeReasonableDataCheck) {
			// updateStateCurrentDependent
			boolean enabled = recordSet.get(2).isActive();
			// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
			recordSet.get(3).setDisplayable(enabled);
			// updateStateVoltageAndCurrentDependent
			enabled = recordSet.get(1).isActive() && recordSet.get(2).isActive();
			// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
			recordSet.get(4).setDisplayable(enabled);
			recordSet.get(5).setDisplayable(enabled);
			recordSet.get(6).setDisplayable(enabled);
			// updateStateVoltageCurrentRevolutionDependent
			enabled = recordSet.get(1).isActive() && recordSet.get(2).isActive() && recordSet.get(7).isActive();
			// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
			recordSet.get(8).setDisplayable(enabled);
			// updateHeightDependent
			// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
			recordSet.get(10).setDisplayable(recordSet.get(9).isActive());
		}
		this.setChangePropery(configChanged); //reset configuration change indicator to previous value, do not vote automatic configuration change at all
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//do not forget to make record displayable -> record.setDisplayable(true);
		if (recordSet.isRaw()) {
			// calculate the values required
			Record record;
			// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
			int displayableCounter = 0;

			// check if measurements isActive == false and set to isDisplayable == false
			for (int i = 0; i < recordSet.size(); i++) {
				Record tmpRecord = recordSet.get(i);
				if (tmpRecord.isActive() && tmpRecord.isDisplayable()) {
					if (log.isLoggable(Level.FINE))
						log.log(Level.FINE, "add to displayable counter: " + tmpRecord.getName());
					++displayableCounter;
				}
			}

			record = recordSet.get(3);// 3=capacity [Ah]
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "start data calculation for record = " + record.getName()); //$NON-NLS-1$
			record.setDisplayable(false);
			record.clear();
			Record recordCurrent = recordSet.get(2); // 2=current
			double timeStep_ms = recordCurrent.getAverageTimeStep_ms(); // timeStep_ms
			Double capacity = 0.0;
			for (int i = 0; i < recordCurrent.size(); i++) {
				capacity = i > 0 ? capacity + ((recordCurrent.get(i) * timeStep_ms) / 3600) : 0.0;
				record.add(capacity.intValue());
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "adding value = " + record.get(i)); //$NON-NLS-1$
			}
			if (recordCurrent.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			record = recordSet.get(4); //4=power
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "start data calculation for record = " + record.getName()); //$NON-NLS-1$
			record.setDisplayable(false);
			record.clear();
			Record recordVoltage = recordSet.get(1); // 1=voltage
			recordCurrent = recordSet.get(2); // 2=current
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(Double.valueOf(1.0 * recordVoltage.get(i) * recordCurrent.get(i) / 1000.0).intValue());
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "adding value = " + record.get(i)); //$NON-NLS-1$
			}
			if (recordVoltage.isDisplayable() && recordCurrent.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			record = recordSet.get(5); //5=energy
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "start data calculation for record = " + record.getName()); //$NON-NLS-1$
			record.setDisplayable(false);
			record.clear();
			recordVoltage = recordSet.get(1); // 1=voltage
			recordCurrent = recordSet.get(2); // 2=current
			timeStep_ms = recordVoltage.getAverageTimeStep_ms(); // timeStep_ms
			Double power = 0.0;
			for (int i = 0; i < recordVoltage.size(); i++) {
				power = i > 0 ? power + ((recordVoltage.get(i) / 1000.0) * (recordCurrent.get(i) / 1000.0) * (timeStep_ms / 3600.0)) : 0.0;
				record.add(power.intValue());
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "adding value = " + record.get(i)); //$NON-NLS-1$
			}
			if (recordVoltage.isDisplayable() && recordCurrent.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			record = recordSet.get(6);// 6=votagePerCell
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "start data calculation for record = " + record.getName()); //$NON-NLS-1$
			record.setDisplayable(false);
			record.clear();
			recordVoltage = recordSet.get(1); // 1=voltage
			PropertyType property = record.getProperty(UniLog.NUMBER_CELLS);
			int numberCells = property != null ? Integer.valueOf(property.getValue()) : 4;
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(Double.valueOf(recordVoltage.get(i) / (double)numberCells).intValue());
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "adding value = " + record.get(i)); //$NON-NLS-1$
			}
			if (recordVoltage.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			record = recordSet.get(8); // 8=efficiency
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "start data calculation for record = " + record.getName()); //$NON-NLS-1$
			record.setDisplayable(false);
			record.clear();
			Record recordRevolution = recordSet.get(7); // 7=revolutionSpeed
			property = recordRevolution.getProperty(UniLog.RPM_FACTOR);
			double rpmFactor = property != null ? Double.valueOf(property.getValue()).doubleValue() : 1.0;
			property = recordRevolution.getProperty(UniLog.NUMBER_MOTOR);
			double numberMotor = property != null ? Double.valueOf(property.getValue()).doubleValue() : 1.0;
			Record recordPower = recordSet.get(4); // 4=power [w]
			property = record.getProperty(UniLog.PROP_N_100_W);
			int prop_n100W = property != null ? Integer.valueOf(property.getValue()) : 10000;
			recordCurrent = recordSet.get(2); // 2=current
			Record recordA1 = recordSet.get(11); // 11=A1 -> torque
			double eta = 0.;
			for (int i = 0; i < recordRevolution.size(); i++) {
				if (i > 1 && recordRevolution.get(i) > 100000 && recordCurrent.get(i) > 3000) { //100 1/min && 3A
					if (prop_n100W == 99999) { // A1 -> torque
						eta = (2 * Math.PI * (this.translateValue(recordA1, recordA1.get(i) / 1000.)) * (recordRevolution.get(i) / 1000.)) / ((recordVoltage.get(i) / 1000.) * (recordCurrent.get(i) / 1000.) * 60.);
						log.log(Level.INFO, String.format("(2 * %4.3f * %3.1fNcm * %5.0frpm) / (%3.1fV * %3.1fA * 60 * 100) = %4.2f", Math.PI, (this.translateValue(recordA1, recordA1.get(i) / 1000.)), (recordRevolution.get(i) / 1000.), (recordVoltage.get(i) / 1000.), (recordCurrent.get(i) / 1000.), eta));
					}
					else {
						double motorPower = Math.pow(((recordRevolution.get(i) * rpmFactor / numberMotor) / 1000.0 * 4.64) / prop_n100W, 3) * 1000.0;
						eta = motorPower * 100.0 / recordPower.get(i);
						eta = eta > 100 ? record.lastElement() / 1000.0 : eta < 0 ? 0 : eta;
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("current=%5.1f; recordRevolution=%5.0f; recordPower=%6.2f; motorPower=%6.2f eta=%5.1f", recordCurrent.get(i) / 1000.0,
								recordRevolution.get(i) / 1000.0, recordPower.get(i) / 1000.0, motorPower / 1000.0, eta));
					}
					record.add(Double.valueOf(eta * 1000).intValue());
				}
				else
					record.add(0);
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "adding value = " + record.get(i)); //$NON-NLS-1$
			}
			if (recordRevolution.isDisplayable() && recordPower.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			boolean isNoSlopeCalculationStarted = true;
			record = recordSet.get(10);// 10=slope
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "start data calculation for record = " + record.getName()); //$NON-NLS-1$
			record.setDisplayable(false);
			record.clear();
			property = record.getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
			int regressionInterval = property != null ? Integer.valueOf(property.getValue()) : 4;
			property = record.getProperty(CalculationThread.REGRESSION_TYPE);
			if (property == null || property.getValue().equals(CalculationThread.REGRESSION_TYPE_CURVE))
				this.calculationThread = new QuasiLinearRegression(recordSet, recordSet.get(9).getName(), recordSet.get(10).getName(), regressionInterval);
			else
				this.calculationThread = new LinearRegression(recordSet,recordSet.get(9).getName(), recordSet.get(10).getName(), regressionInterval);

			try {
				this.calculationThread.start();
			}
			catch (RuntimeException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
			if (recordSet.get(9).isDisplayable()) {
				//record.setDisplayable(true); // set within calculation thread
				isNoSlopeCalculationStarted = false;
				++displayableCounter;
			}

			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
			recordSet.setConfiguredDisplayable(displayableCounter);

			if (isNoSlopeCalculationStarted) this.application.updateGraphicsWindow();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "finished data calculation for record = " + record.getName()); //$NON-NLS-1$
		}
	}

	/**
	 * @return the dialog
	 */
	@Override
	public UniLogDialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the serialPort
	 */
	@Override
	public UniLogSerialPort getCommunicationPort() {
		return this.serialPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] {	IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION,
				NUMBER_CELLS, PROP_N_100_W,
				CalculationThread.REGRESSION_INTERVAL_SEC, CalculationThread.REGRESSION_TYPE};
	}

	/**
	 * enhance initial record set comment device specific
	 * UniLog has serial number and a firmeware version
	 * @param recordSet
	 */
	public void updateInitialRecordSetComment(RecordSet recordSet) {
		recordSet.setRecordSetDescription(String.format("%s; \n%s : %s; %s : %s; ",  //$NON-NLS-1$
				recordSet.getRecordSetDescription(), SERIAL_NUMBER, this.getDialog().serialNumber, FIRMEWARE_VERSION, this.getDialog().unilogVersion));
	}

	/**
	 * invert data of current curve
	 */
	public void invertRecordData(Record record) {
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (int i=0; i<record.realSize(); ++i) {
			int value = record.get(i) * -1;
			record.set(i, value);
			if (i != 0) {
				if (value > max) max = value;
				if (value < min) min = value;
			}
			else {
				min = max = value;
			}
		}
		record.setMinMax(min, max);
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
						this.getDialog().liveThread = new UniLogLiveGatherer(this.application, this, this.serialPort, activChannel.getNumber(), this.getDialog());
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
					this.getDialog().setButtonStateLiveGatherer(true);
					this.application.getDeviceSelectionDialog().open();
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
	 * get the analog modus of A1, A2 and A3 to update the analog measurements of the given channel configuration
	 * @param dataBuffer
	 * @param channelConfigKey
	 */
	public void updateMeasurementByAnalogModi(byte[] dataBuffer, final int channelConfigKey) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "visit updateMeasurementByAnalogModi");
		// a1Modus -> 0==Temperature, 1==Millivolt, 2=Speed 250, 3=Speed 400
		int a1Modus = (dataBuffer[7] & 0xF0) >> 4; // 11110000
				a1Modus = a1Modus > 3 ? 3 : a1Modus;

		// A2 Modus == 0 -> temperature; A2 Modus == 1 -> impulse time length
		// A2 Modus == 2 -> mills voltage; A2 Modus == 3 -> capacity mAh
		int a2Modus = (dataBuffer[4] & 0x30) >> 4; // 00110000

		// A3 Modus == 0 -> external temperature sensor; A3 Modus == 1 -> internal temperature
		// A3 Modus == 2 -> energy in Wmin; A3 Modus == 3 -> mills voltage
		int a3Modus = (dataBuffer[4] & 0xC0) >> 6; // 11000000

		if (log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder();
			sb.append("a1Modus = " + a1Modus).append(GDE.LINE_SEPARATOR); //$NON-NLS-1$
			sb.append("a2Modus = " + a2Modus).append(GDE.LINE_SEPARATOR); //$NON-NLS-1$
			sb.append("a3Modus = " + a3Modus).append(GDE.LINE_SEPARATOR); //$NON-NLS-1$
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, sb.toString());
		}

		MeasurementType measurement = this.getMeasurement(channelConfigKey, 11); // 11=A1
		measurement.setName(UniLogDialog.A1_MODUS_NAMES[a1Modus].trim());
		measurement.setUnit(UniLogDialog.A1_MODUS_UNITS[a1Modus].trim());

		measurement = this.getMeasurement(channelConfigKey, 12); // 12=A2
		measurement.setName(UniLogDialog.A2_MODUS_NAMES[a2Modus].trim());
		measurement.setUnit(UniLogDialog.A2_MODUS_UNITS[a2Modus].trim());

		measurement = this.getMeasurement(channelConfigKey, 13); // 13=A3
		measurement.setName(UniLogDialog.A3_MODUS_NAMES[a3Modus].trim());
		measurement.setUnit(UniLogDialog.A3_MODUS_UNITS[a3Modus].trim());
	}
}
