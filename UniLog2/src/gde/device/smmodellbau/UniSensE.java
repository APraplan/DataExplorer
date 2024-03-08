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

    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.StringTokenizer;
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
import gde.device.InputTypes;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.io.LogViewReader;
import gde.io.NMEAParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class UniSensE extends UniLog2 implements IDevice {
	final static Logger	log										= Logger.getLogger(UniSensE.class.getName());

	final static String			SM_UNILOG_2_INI				= "SM UniLog 2.ini";													//$NON-NLS-1$
	final static String			SM_UNILOG_2_INI_PATH	= "SM UniLog 2 setup";												//$NON-NLS-1$
	final static String			SM_UNILOG_2_DIR_STUB	= "UniLog";																		//$NON-NLS-1$
	static String						selectedSetupFilePath;																							//path to setup ini file

	final DataExplorer			application;
	final Channels					channels;
	final UniLog2Dialog			dialog;
	final UniLog2SerialPort	serialPort;
	UniLog2LiveGatherer			gathererThread;

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public UniSensE(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.smmodellbau.unilog2.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = this.application != null ? new UniLog2SerialPort(this, this.application) : new UniLog2SerialPort(this, null);
		this.channels = Channels.getInstance();
		this.dialog = new UniLog2Dialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) 
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		UniLog2SerialPort.TIME_OUT_MS = this.getReadTimeOut();
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public UniSensE(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.smmodellbau.unilog2.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = this.application != null ? new UniLog2SerialPort(this, this.application) : new UniLog2SerialPort(this, null);
		this.channels = Channels.getInstance();
		this.dialog = new UniLog2Dialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) 
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		UniLog2SerialPort.TIME_OUT_MS = this.getReadTimeOut();
	}

	/**
	 * @return the serialPort
	 */
	@Override
	public UniLog2SerialPort getCommunicationPort() {
		return this.serialPort;
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
				//System.out.println((subLenght+8));
				lineBuffer = new byte[subLenght];
				System.arraycopy(dataBuffer, 4 + lastLength, lineBuffer, 0, subLenght);
				String textInput = new String(lineBuffer, "ISO-8859-1"); //$NON-NLS-1$
				//System.out.println(textInput);
				StringTokenizer st = new StringTokenizer(textInput);
				Vector<String> vec = new Vector<String>();
				while (st.hasMoreTokens())
					vec.add(st.nextToken("\r\n")); //$NON-NLS-1$
				//0=VoltageRx, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Energy, 6=CellBalance, 7=CellVoltage1, 8=CellVoltage2, 9=CellVoltage3,
				//10=CellVoltage4, 11=CellVoltage5, 12=CellVoltage6, 13=Revolution, 14=Efficiency, 15=Altitude, 16=Climb, 17=ValueA1, 18=ValueA2, 19=ValueA3,
				//20=AirPressure, 21=InternTemperature, 22=ServoImpuls In, 23=ServoImpuls Out,
				//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
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
	 * @param dataBuffer byte array with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		//noop due to previous parsed CSV data
		return points;
	}

	/**
	 * convert the device live data string to data point values, no calculation will take place here, see translateValue reverseTranslateValue
	 * @param points pointer to integer array to be filled with converted data
	 * @param stringBuffer the data to be parsed
	 */
	public int[] convertLiveData(int[] points, String stringBuffer) {
		String[] dataArray = StringHelper.splitString(stringBuffer, "\r", "\n");
		for (int i=2; i < dataArray.length; i++) {
			//0=VoltageRx, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Energy, 6=CellBalance, 7=CellVoltage1, 8=CellVoltage2, 9=CellVoltage3,
			//10=CellVoltage4, 11=CellVoltage5, 12=CellVoltage6, 13=Revolution, 14=Efficiency, 15=Altitude, 16=Climb, 17=ValueA1, 18=ValueA2, 19=ValueA3,
			//20=AirPressure, 21=InternTemperature, 22=ServoImpuls In, 23=ServoImpuls Out,
			try {
				switch (i) {
				case 2: // 0.00A    -3.4m
					points[2] = (int) (Double.parseDouble(dataArray[i].split("A")[0].trim()) * 1000.0);
					points[15] = (int) (Double.parseDouble(dataArray[i].split("A")[1].split("m")[0].trim()) * 1000.0);
					break;
				case 3: // 16.23V  +0.1m/s
					points[1] = (int) (Double.parseDouble(dataArray[i].split("V")[0].trim()) * 1000.0);
					points[16] = (int) (Double.parseDouble(dataArray[i].split("V")[1].split("m")[0].trim()) * 1000.0);
					break;
				case 4: // 0W      0rpm
					points[4] = (int) (Double.parseDouble(dataArray[i].split("W")[0].trim()) * 1000.0);
					points[13] = (int) (Double.parseDouble(dataArray[i].split("W")[1].split("r")[0].trim()) * 1000.0);
					break;
				case 5: // 0mAh 0.00VRx
					points[3] = (int) (Double.parseDouble(dataArray[i].split("m")[0].trim()) * 1000.0);
					points[0] = (int) (Double.parseDouble(dataArray[i].split("h")[1].split("V")[0].trim()) * 1000.0);
					break;
				case 6: // 0Wmin
					points[5] = (int) (Double.parseDouble(dataArray[i].split("W")[0].trim()) * 1000.0);
					break;
				case 7: // 0us ->    |0us
					points[22] = (int) (Double.parseDouble(dataArray[i].split("us")[0].trim()) * 1000.0);
					points[23] = (int) (Double.parseDouble(dataArray[i].split("us")[1].replace("|", "").substring(3).trim()) * 1000.0);
					break;
				case 10: // 4.04V1   4.05V2
					points[7] = (int) (Double.parseDouble(dataArray[i].split("V1")[0].trim()) * 1000.0);
					points[8] = (int) (Double.parseDouble(dataArray[i].split("V1")[1].split("V")[0].trim()) * 1000.0);
					break;
				case 11: // 4.05V3   4.08V4
					points[9] = (int) (Double.parseDouble(dataArray[i].split("V3")[0].trim()) * 1000.0);
					points[10] = (int) (Double.parseDouble(dataArray[i].split("V3")[1].split("V")[0].trim()) * 1000.0);
					break;
				case 12: // 0.00V5   0.00V6
					points[11] = (int) (Double.parseDouble(dataArray[i].split("V5")[0].trim()) * 1000.0);
					points[12] = (int) (Double.parseDouble(dataArray[i].split("V5")[1].split("V")[0].trim()) * 1000.0);
					break;
				case 13: // A1         0.6mV
					String tmpValueA1 =dataArray[i].substring(2).trim();
					tmpValueA1 = tmpValueA1.contains("|") ? tmpValueA1.substring(0, tmpValueA1.indexOf('|')) + tmpValueA1.substring(tmpValueA1.indexOf('|')+1) : tmpValueA1;
					if(tmpValueA1.contains("mV"))
						points[17] = (int) (Double.parseDouble(tmpValueA1.split("mV")[0].trim()) * 1000.0);
					else if(tmpValueA1.contains("`C"))
						points[17] = (int) (Double.parseDouble(tmpValueA1.split("`C")[0].trim()) * 1000.0);
					else if(tmpValueA1.contains("km/h"))
						points[17] = (int) (Double.parseDouble(tmpValueA1.split("km/h")[0].trim()) * 1000.0);
					break;
				case 14: // A2         0.6mV
					String tmpValueA2 =dataArray[i].substring(2).trim();
					tmpValueA2 = tmpValueA2.contains("|") ? tmpValueA2.substring(0, tmpValueA2.indexOf('|')) + tmpValueA2.substring(tmpValueA2.indexOf('|')+1) : tmpValueA2;
					if(tmpValueA2.contains("mV"))
						points[18] = (int) (Double.parseDouble(tmpValueA2.split("mV")[0].trim()) * 1000.0);
					else if(tmpValueA2.contains("`C"))
						points[18] = (int) (Double.parseDouble(tmpValueA2.split("`C")[0].trim()) * 1000.0);
					else if(tmpValueA2.contains("km/h"))
						points[18] = (int) (Double.parseDouble(tmpValueA2.split("km/h")[0].trim()) * 1000.0);
					break;
				case 15: // A3         0.6mV
					String tmpValueA3 =dataArray[i].substring(2).trim();
					tmpValueA3 = tmpValueA3.contains("|") ? tmpValueA3.substring(0, tmpValueA3.indexOf('|')) + tmpValueA3.substring(tmpValueA3.indexOf('|')+1) : tmpValueA3;
					if(tmpValueA3.contains("mV"))
						points[19] = (int) (Double.parseDouble(tmpValueA3.split("mV")[0].trim()) * 1000.0);
					else if(tmpValueA3.contains("`C"))
						points[19] = (int) (Double.parseDouble(tmpValueA3.split("`C")[0].trim()) * 1000.0);
					else if(tmpValueA3.contains("km/h"))
						points[19] = (int) (Double.parseDouble(tmpValueA3.split("km/h")[0].trim()) * 1000.0);
					break;
				case 18: // Druck  970.74hPa
					points[20] = (int) (Double.parseDouble(dataArray[i].substring(dataArray[i].lastIndexOf(" "), dataArray[i].length()-3).trim()) * 1000.0);
					break;
				case 19: // intern    32.1`C
					points[21] = (int) (Double.parseDouble(dataArray[i].substring(dataArray[i].lastIndexOf(" "), dataArray[i].length()-2).trim()) * 1000.0);
					break;
				}
			}
			catch (RuntimeException e) {
				//ignore;
			}
		}

		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		for (int i = 7; i <= 12; i++) {
			if (points[i] > 0) {
				maxVotage = points[i] > maxVotage ? points[i] : maxVotage;
				minVotage = points[i] < minVotage ? points[i] : minVotage;
			}
		}
		points[6] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 1000;

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
		int[] points = new int[recordSet.getNoneCalculationRecordNames().length];
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

			//0=VoltageRx, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Energy, 6=CellBalance, 7=CellVoltage1, 8=CellVoltage2, 9=CellVoltage3,
			//10=CellVoltage4, 11=CellVoltage5, 12=CellVoltage6, 13=Revolution, 14=Efficiency, 15=Altitude, 16=Climb, 17=ValueA1, 18=ValueA2, 19=ValueA3,
			//20=AirPressure, 21=InternTemperature, 22=ServoImpuls In, 23=ServoImpuls Out,
			//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
			for (int j = 0; j < points.length; j++) {
				points[j] = (((convertBuffer[0 + (j * 4)] & 0xff) << 24) + ((convertBuffer[1 + (j * 4)] & 0xff) << 16) + ((convertBuffer[2 + (j * 4)] & 0xff) << 8) + ((convertBuffer[3 + (j * 4)] & 0xff) << 0));
			}

			if (recordSet.isTimeStepConstant())
				recordSet.addNoneCalculationRecordsPoints(points);
			else
				recordSet.addNoneCalculationRecordsPoints(points, timeStamps.get(i) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
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
				//0=VoltageRx, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Energy, 6=CellBalance, 7=CellVoltage1, 8=CellVoltage2, 9=CellVoltage3,
				//10=CellVoltage4, 11=CellVoltage5, 12=CellVoltage6, 13=Revolution, 14=Efficiency, 15=Altitude, 16=Climb, 17=ValueA1, 18=ValueA2, 19=ValueA3,
				//20=AirPressure, 21=InternTemperature, 22=ServoImpuls In, 23=ServoImpuls Out,
				//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
				if (record.getOrdinal() == 15) { //15=Altitude
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
						log.log(Level.SEVERE, record.getParent().getName() + " " + record.getName() + " " + e.getMessage());
					}
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
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required

		//0=VoltageRx, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Energy, 6=CellBalance, 7=CellVoltage1, 8=CellVoltage2, 9=CellVoltage3,
		//10=CellVoltage4, 11=CellVoltage5, 12=CellVoltage6, 13=Revolution, 14=Efficiency, 15=Altitude, 16=Climb, 17=ValueA1, 18=ValueA2, 19=ValueA3,
		//20=AirPressure, 21=InternTemperature, 22=ServoImpuls In, 23=ServoImpuls Out,
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
		if (record.getOrdinal() == 15) { //15=Altitude
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
				log.log(Level.SEVERE, record.getAbstractParent().getName() + " " + record.getName() + " " + e.getMessage());
			}
		}

		double newValue = (value - reduction) * factor + offset;

		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required

		//0=VoltageRx, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Energy, 6=CellBalance, 7=CellVoltage1, 8=CellVoltage2, 9=CellVoltage3,
		//10=CellVoltage4, 11=CellVoltage5, 12=CellVoltage6, 13=Revolution, 14=Efficiency, 15=Altitude, 16=Climb, 17=ValueA1, 18=ValueA2, 19=ValueA3,
		//20=AirPressure, 21=InternTemperature, 22=ServoImpuls In, 23=ServoImpuls Out,
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
		if (record.getOrdinal() == 15) { //15=Altitude
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
				log.log(Level.SEVERE, record.getAbstractParent().getName() + " " + record.getName() + " " + e.getMessage());
			}
		}

		double newValue = (value - offset) / factor + reduction;

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
	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		Record record;
		MeasurementType measurement;
		//0=VoltageRx, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Energy, 6=CellBalance, 7=CellVoltage1, 8=CellVoltage2, 9=CellVoltage3,
		//10=CellVoltage4, 11=CellVoltage5, 12=CellVoltage6, 13=Revolution, 14=Efficiency, 15=Altitude, 16=Climb, 17=ValueA1, 18=ValueA2, 19=ValueA3,
		//20=AirPressure, 21=InternTemperature, 22=ServoImpuls In, 23=ServoImpuls Out,
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
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
		//do not forget to make record displayable -> record.setDisplayable(true);
		// calculate the values required
		//0=VoltageRx, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Energy, 6=CellBalance, 7=CellVoltage1, 8=CellVoltage2, 9=CellVoltage3,
		//10=CellVoltage4, 11=CellVoltage5, 12=CellVoltage6, 13=Revolution, 14=Efficiency, 15=Altitude, 16=Climb, 17=ValueA1, 18=ValueA2, 19=ValueA3,
		//20=AirPressure, 21=InternTemperature, 22=ServoImpuls In, 23=ServoImpuls Out,
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;

		Record record = recordSet.get(14); // 14=efficiency
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "start data calculation for record = " + record.getName()); //$NON-NLS-1$
		Record recordRevolution = recordSet.get(13); // 13=revolution
		Record recordPower = recordSet.get(4); // 4=Power [w]
		PropertyType property = record.getProperty(MeasurementPropertyTypes.PROP_N_100_W.value());
		int prop_n100W = property != null ? Integer.valueOf(property.getValue()) : 10000;
		property = recordRevolution.getProperty(MeasurementPropertyTypes.NUMBER_MOTOR.value());
		double numberMotor = property != null ? Double.valueOf(property.getValue()).doubleValue() : 1.0;
		Record recordCurrent = recordSet.get(2); // 2=Current
		for (int i = 0; i < recordRevolution.size(); i++) {
			if (i > 1 && recordRevolution.get(i)> 100000 && recordCurrent.get(i) > 3000) { //100 1/min && 3A
				double motorPower = Math.pow(((recordRevolution.get(i) / numberMotor) / 1000.0 * 4.64) / prop_n100W, 3) * 1000.0;
				double eta = motorPower * 100.0 / recordPower.get(i);
				eta = eta > 100 && i > 1 ? record.get(i-1)/1000.0 : eta < 0 ? 0 : eta;
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, String.format("current=%5.1f; recordRevolution=%5.0f; recordPower=%6.2f; motorPower=%6.2f eta=%5.1f", recordCurrent.get(i)/1000.0, recordRevolution.get(i)/1000.0, recordPower.get(i)/1000.0, motorPower/1000.0, eta));
				record.set(i, Double.valueOf(eta * 1000).intValue());
				record.setDisplayable(true);
			}
			else record.set(i, 0);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "adding value = " + record.get(i)); //$NON-NLS-1$
		}
		this.application.updateStatisticsData();
	}

	/**
	 * @return the dialog
	 */
	@Override
	public UniLog2Dialog getDialog() {
		return null; //suppress opening dialog even if initialized 
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
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.gathererThread = new UniLog2LiveGatherer(application, this, serialPort, dialog);
						this.gathererThread.start();
					}
				}
				catch (SerialPortException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage()}));
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
				if (this.gathererThread != null && this.gathererThread.isAlive()) {
					this.gathererThread.stopDataGathering();
					this.gathererThread.interrupt();
				}
				WaitTimer.delay(1000);
				this.serialPort.close();
			}
		}
	}
}
