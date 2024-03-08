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

    Copyright (c) 2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.skyrc;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.usb.UsbClaimException;
import javax.usb.UsbException;
import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.WaitTimer;

/**
 * Class to implement SKYRC iMax B6 device
 * @author Winfried Bruegmann
 */
public class IMaxB6USB extends MC3000 implements IDevice {
	final static Logger				log					= Logger.getLogger(IMaxB6USB.class.getName());
	final protected ChargerDialog				dialog;
	ImaxB6GathererThread			dataGatherThread;
	IMaxB6RDX1UsbPort					usbPort;

	int[]								resetEnergy		= new int[] { 5, 5 };
	double[]						energy				= new double[] { 0, 0 };
	protected String[]	USAGE_MODE_DJI;

	protected class SystemInfo {
		byte		productId;
		byte[]	productModel	= new byte[6];
		byte 		isUpgradeSupported;
		byte 		isSoftwareEncrytion;
		byte		customerId;
		byte[]	firmwareVersion = new byte[2];
		byte[]	hardwareVersion = new byte[2];

		public SystemInfo(final byte[] buffer) {
			this.productId = buffer[4];
			for (int i = 0; i < productModel.length; i++) {
				this.productModel[i] = buffer[i + 5];
			}
			this.isUpgradeSupported = buffer[11];
			this.isSoftwareEncrytion = buffer[12];
			this.customerId = buffer[13];
			System.arraycopy(buffer, 16, this.firmwareVersion, 0, 2);
			System.arraycopy(buffer, 18, this.hardwareVersion, 0, 2);
			
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, String.format("productId=0x%02x productModel=%s, isUpgradeSupported=%b isSoftwareEncrytion=%b customerId=0x%02x firmwareVersion=%s hardwareVersion=%s", 
						this.getProductId(), this.getProductModel(), this.isUpgradeSupported(), this.isSoftwareEncryption(), this.getCustomerId(), this.getFirmwareVersion(), this.getHardwareVersion()));
			}
		}

		public byte getProductId() {
			return this.productId;
		}
		
		public String getProductModel() {
			byte[] tmpMachineID = new byte[6];
			System.arraycopy(this.productModel, 0, tmpMachineID, 0, 6);
			return String.format("%s", new String(tmpMachineID));
		}
		
		public boolean isUpgradeSupported() {
			return this.isUpgradeSupported == 0x01;
		}
		
		public boolean isSoftwareEncryption() {
			return this.isSoftwareEncrytion == 0x01;
		}
		
		public byte getCustomerId() {
			return this.customerId;
		}

		public String getFirmwareVersion() {
			return String.format("Firmware: %d.%02d", this.firmwareVersion[0], this.firmwareVersion[1]);
		}

		public int getFirmwareVersionAsInt() {
			return Integer.valueOf(String.format("%d%02d", this.firmwareVersion[0], this.firmwareVersion[1])).intValue();
		}

		public String getHardwareVersion() {
			return String.format("Hardware: %d.%02d", this.hardwareVersion[0], this.hardwareVersion[1]);
		}

		public int getHardwareVersionAsInt() {
			return Integer.valueOf(String.format("%d%02d", this.hardwareVersion[0], this.hardwareVersion[1])).intValue();
		}
	}
	
	protected class SystemSetting {

		byte		productId;
		byte		restingTimeNiMH;
		byte		isProtectionTime;
		byte[]	protectionTime	= new byte[2];
		byte		isCapacityLimit;
		byte[]	capacityLimit	= new byte[2];
		byte		isKeySound;
		byte		isSystemSound;
		byte[]	voltageLow = new byte[2];
		byte		isTemperaturLimit;
		byte[]	voltage = new byte[2];
		byte[]	voltage1 = new byte[2];
		byte[]	voltage2 = new byte[2];
		byte[]	voltage3 = new byte[2];
		byte[]	voltage4 = new byte[2];
		byte[]	voltage5 = new byte[2];
		byte[]	voltage6 = new byte[2];
		
			
		public SystemSetting(final byte[] buffer) {
			this.productId = buffer[3];
			this.restingTimeNiMH = buffer[4];
			this.isProtectionTime = buffer[5];
			System.arraycopy(buffer, 6, this.protectionTime, 0, 2);
			this.isCapacityLimit = buffer[8];
			System.arraycopy(buffer, 9, this.capacityLimit, 0, 2);			
			this.isKeySound = buffer[11];
			this.isSystemSound = buffer[12];
			System.arraycopy(buffer, 13, this.voltageLow, 0, 2);
			this.isTemperaturLimit = buffer[17];
			System.arraycopy(buffer, 18, this.voltage, 0, 2);
			System.arraycopy(buffer, 20, this.voltage1, 0, 2);
			System.arraycopy(buffer, 22, this.voltage2, 0, 2);
			System.arraycopy(buffer, 24, this.voltage3, 0, 2);
			System.arraycopy(buffer, 26, this.voltage4, 0, 2);
			System.arraycopy(buffer, 28, this.voltage5, 0, 2);
			System.arraycopy(buffer, 30, this.voltage6, 0, 2);			
			
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, String.format(
					"productId=0x%02x restingTimeNiMH=%d isPrtotectionTime=%b protectionTime=%d isCapacityLimit=%b capacityLimit=%d isKeySound=%b isSystemSound=%b voltageLow=%d isTemperaturLimit=%b", 
					this.getProductId(), this.getRestingTimeNiMH(), this.isProtectionTime(), this.getProtectionTime(), this.isCapacityLimit(), this.getCapacityLimit(), this.isKeySound(), this.isSystemSound(), this.getVoltageLow(), this.isTemperaturLimit()));
				log.log(Level.FINER, String.format(
						"voltage=%d, voltage1=%d voltage2=%d voltage3=%d voltage4=%d voltage5=%d voltage6=%d",
						this.getVoltage(), this.getVoltage1(), this.getVoltage2(), this.getVoltage3(), this.getVoltage4(), this.getVoltage5(), this.getVoltage6()));
			}
		}

		public byte getProductId() {
			return productId;
		}

		public boolean isTimeLimiting() {
			return isProtectionTime == 0x01;
		}

		public short getTimeLimit() {
			return DataParser.parse2Short(protectionTime[1], protectionTime[0]);
		}

		public boolean isCapacityLimit() {
			return isCapacityLimit == 0x01;
		}

		public short getCapacityLimit() {
			return DataParser.parse2Short(capacityLimit[1], capacityLimit[0]);
		}

		public boolean isKeySound() {
			return isKeySound == 0x01;
		}

		public boolean isSystemSound() {
			return isSystemSound == 0x01;
		}

		public byte getRestingTimeNiMH() {
			return restingTimeNiMH;
		}

		public boolean isProtectionTime() {
			return isProtectionTime == 0x01;
		}

		public short getProtectionTime() {
			return DataParser.parse2Short(this.protectionTime, 0);
		}

		public short getVoltageLow() {
			return DataParser.parse2Short(this.voltageLow[1], this.voltageLow[0]);
		}

		public boolean isTemperaturLimit() {
			return isTemperaturLimit == 0x01;
		}

		public short getVoltage() {
			return DataParser.parse2Short(this.voltage[1], this.voltage[0]);
		}

		public short getVoltage1() {
			return DataParser.parse2Short(this.voltage1[1], this.voltage1[0]);
		}

		public short getVoltage2() {
			return DataParser.parse2Short(this.voltage2[1], this.voltage2[0]);
		}

		public short getVoltage3() {
			return DataParser.parse2Short(this.voltage3[1], this.voltage3[0]);
		}

		public short getVoltage4() {
			return DataParser.parse2Short(this.voltage4[1], this.voltage4[0]);
		}

		public short getVoltage5() {
			return DataParser.parse2Short(this.voltage5[1], this.voltage5[0]);
		}

		public short getVoltage6() {
			return DataParser.parse2Short(this.voltage6[1], this.voltage6[0]);
		}
	}


	SystemInfo[]	systemInfo = new SystemInfo[1];
	SystemSetting[]	systemSetting = new SystemSetting[1];

	/**
	 * @param xmlFileName
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public IMaxB6USB(String xmlFileName) throws FileNotFoundException, JAXBException {
		super(xmlFileName);
	
		this.STATUS_MODE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3600), 	//Standby
				Messages.getString(MessageIds.GDE_MSGT3601) + //Charge	0x01
				GDE.STRING_COLON +
				Messages.getString(MessageIds.GDE_MSGT3602),	//Discharge
				Messages.getString(MessageIds.GDE_MSGT3600), 	//Standby 0x02
				Messages.getString(MessageIds.GDE_MSGT3604), 	//Finish	0x03
				Messages.getString(MessageIds.GDE_MSGT3605) };//Error		0x04

		//LI battery： 	0：CHARGE, 1：DISCHARGE, 2：STORAGE, 3：FAST CHG, 4：BALANCE
		this.USAGE_MODE_LI = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3617), //CHARGE
				Messages.getString(MessageIds.GDE_MSGT3613), //DISCHARGE
				Messages.getString(MessageIds.GDE_MSGT3612), //STORAGE
				Messages.getString(MessageIds.GDE_MSGT3615), //FAST CHG
				Messages.getString(MessageIds.GDE_MSGT3616)};//BALANCE

		//Ni battery:		0=CHARGE 1=AUTO_CHARGE 2=DISCHARGE 3=RE_PEAK 4=CYCLE
		this.USAGE_MODE_NI = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3617), 
				Messages.getString(MessageIds.GDE_MSGT3625), 
				Messages.getString(MessageIds.GDE_MSGT3623),
				Messages.getString(MessageIds.GDE_MSGT3626), 
				Messages.getString(MessageIds.GDE_MSGT3624) };

		//PB battery:		0=CHARGE 1=DISCHARGE
		this.USAGE_MODE_PB = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3620), 
				Messages.getString(MessageIds.GDE_MSGT3623) };

		//DJI battery:		0=CHARGE 1=Storage
		this.USAGE_MODE_DJI = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3617), //CHARGE
				Messages.getString(MessageIds.GDE_MSGT3612)};//STORAGE

		//battery type:  0:LiPo 1:LiIo 2:LiFe 3:LiHv 4:NiMH 5:NiCd 6:PB 7: DJI Mavic
		this.BATTERY_TYPE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3649), 
				Messages.getString(MessageIds.GDE_MSGT3640),
				Messages.getString(MessageIds.GDE_MSGT3641), 
				Messages.getString(MessageIds.GDE_MSGT3642), 
				Messages.getString(MessageIds.GDE_MSGT3643), 
				Messages.getString(MessageIds.GDE_MSGT3644), 
				Messages.getString(MessageIds.GDE_MSGT3648), 
				Messages.getString(MessageIds.GDE_MSGT3687) };

		this.usbPort = new IMaxB6RDX1UsbPort(this, this.application);
		this.dialog = new ChargerDialog(this.application.getShell(), this);
	}

	/**
	 * @param deviceConfig
	 */
	public IMaxB6USB(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		
		this.STATUS_MODE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3600), 	//Standby
				Messages.getString(MessageIds.GDE_MSGT3601) + //Charge	0x01
				GDE.STRING_COLON +
				Messages.getString(MessageIds.GDE_MSGT3602),	//Discharge
				Messages.getString(MessageIds.GDE_MSGT3600), 	//Standby 0x02
				Messages.getString(MessageIds.GDE_MSGT3604), 	//Finish	0x03
				Messages.getString(MessageIds.GDE_MSGT3605) };//Error		0x04
		
		//LI battery： 	0：CHARGE, 1：DISCHARGE, 2：STORAGE, 3：FAST CHG, 4：BALANCE
		this.USAGE_MODE_LI = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3617), //CHARGE
				Messages.getString(MessageIds.GDE_MSGT3613), //DISCHARGE
				Messages.getString(MessageIds.GDE_MSGT3612), //STORAGE
				Messages.getString(MessageIds.GDE_MSGT3615), //FAST CHG
				Messages.getString(MessageIds.GDE_MSGT3616)};//BALANCE

		//Ni battery:		0=CHARGE 1=AUTO_CHARGE 2=DISCHARGE 3=RE_PEAK 4=CYCLE
		this.USAGE_MODE_NI = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3617), 
				Messages.getString(MessageIds.GDE_MSGT3625), 
				Messages.getString(MessageIds.GDE_MSGT3623),
				Messages.getString(MessageIds.GDE_MSGT3626), 
				Messages.getString(MessageIds.GDE_MSGT3624) };

		//PB battery:		0=CHARGE 1=DISCHARGE
		this.USAGE_MODE_PB = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3620), 
				Messages.getString(MessageIds.GDE_MSGT3623) };

		//DJI battery:		0=CHARGE 1=Storage
		this.USAGE_MODE_DJI = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3617), //CHARGE
				Messages.getString(MessageIds.GDE_MSGT3612)};//STORAGE

		//battery type:  0:LiPo 1:LiIo 2:LiFe 3:LiHv 4:NiMH 5:NiCd 6:PB 7: DJI Mavic
		this.BATTERY_TYPE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3649), 
				Messages.getString(MessageIds.GDE_MSGT3640),
				Messages.getString(MessageIds.GDE_MSGT3641), 
				Messages.getString(MessageIds.GDE_MSGT3642), 
				Messages.getString(MessageIds.GDE_MSGT3643), 
				Messages.getString(MessageIds.GDE_MSGT3644), 
				Messages.getString(MessageIds.GDE_MSGT3648), 
				Messages.getString(MessageIds.GDE_MSGT3687) };

		this.usbPort = new IMaxB6RDX1UsbPort(this, this.application);
		this.dialog = new ChargerDialog(this.application.getShell(), this);
	}

	/**
	 * @return the dialog
	 */
	public DeviceDialog getDialog() {
		return this.dialog;
	}
//
//	/**
//	 * @return the device name -> return not supported device
//	 */
//	public String getName() {
//		return this.systemInfo[0] != null && this.systemInfo[0].machineId.toString().startsWith("100089") ? "D100" : "D100 V2";
//	}

	/**
	 * load the mapping exist between lov file configuration keys and gde keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
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
		return 86; //sometimes first 4 bytes give the length of data + 4 bytes for number
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
		//not implemented
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		int maxVoltage = Integer.MIN_VALUE;
		int minVoltage = Integer.MAX_VALUE;
		//0=Voltage 1=Current 2=Capacity 3=Power 4=Energy 5=Temperature Ext  6=Temperature Int 7=Resistance
		points[0] = DataParser.parse2UnsignedShort(dataBuffer[10], dataBuffer[9]);
		points[1] = DataParser.parse2Short(dataBuffer[12], dataBuffer[11]);
		points[2] = DataParser.parse2Short(dataBuffer[6], dataBuffer[5]) * 1000;
		points[3] = Double.valueOf(points[0] / 1000.0 * points[1]).intValue(); // power U*I [W]
		//databuffer[0] injected energy handling flag
		switch (dataBuffer[1]) { //injected energy handling flag
		case 0: //add up energy
			energy[0] += points[0] / 1000.0 * points[1] / 3600.0;
			points[4] = Double.valueOf(energy[0]).intValue();
			if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, "add up Energy");
			break;
		case 1: // reset energy
			energy[0] = 0.0;
			points[4] = 0;
			points[4] = 0;
			if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, "reset Energy");
			break;
		default: // keep energy untouched
		case -1: // keep energy untouched
			points[4] = points[4];
			if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, "untouche Energy");
			break;
		}
		//0=Voltage 1=Current 2=Capacity 3=Power 4=Energy 5=Temperature Ext  6=Temperature Int 7=Resistance
		points[5] = dataBuffer[13] * 1000;
		points[6] = dataBuffer[14] * 1000;
		points[7] = dataBuffer[15] * 1000;
		
		if (dataBuffer[0] <= 3) { // exclude Ni PB batteries
			//9=CellVoltage1....14=CellVoltage6
			int j = 0;
			for (int i = 9; i < points.length; i++, j += 2) {
				if (dataBuffer[j + 17] > 0x05) { // filter none used cell 
					points[i] = DataParser.parse2Short(dataBuffer[j + 18], dataBuffer[j + 17]);
					maxVoltage = points[i] > maxVoltage ? points[i] : maxVoltage;
					minVoltage = points[i] < minVoltage ? points[i] : minVoltage;
				}
				else
					points[i] = 0;
			}
			//8=Balance
			points[8] = 1000 * (maxVoltage != Integer.MIN_VALUE && minVoltage != Integer.MAX_VALUE ? maxVoltage - minVoltage : 0);
		}
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
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize, convertBuffer, 0, dataBufferSize);

			for (int j = 0, k = 0; j < points.length; j++, k += 4) {
				//0=Voltage 1=Current 2=Capacity 3=Power 4=Energy 5=TempExt  6=TempInt 7=Resistance 8=Balance 9=CellVoltage1....14=CellVoltage6
				points[j] = (((convertBuffer[k] & 0xff) << 24) + ((convertBuffer[k + 1] & 0xff) << 16) + ((convertBuffer[k + 2] & 0xff) << 8) + ((convertBuffer[k + 3] & 0xff) << 0));
			}
			recordSet.addPoints(points);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
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
		//0=Voltage 1=Current 2=Capacity 3=Power 4=Energy 5=TempExt  6=TempInt 7=Resistance 8=Balance 9=CellVoltage1....14=CellVoltage6
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
	@Override
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
	@Override
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required

		double newValue = (value - offset) / factor + reduction;

		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * query device for specific smoothing index
	 * 0 do nothing at all
	 * 1 current drops just a single peak
	 * 2 current drop more or equal than 2 measurements 
	 */
	@Override
	public int getCurrentSmoothIndex() {
		return 1;
	}

	/**
	 * check if one of the outlet channels are in processing type
	 * types: 1=working 2=standby 3=finish 4=error 
	 * 0: Standby, 1: Charging, 2: discharge, 3: cycle charge rest time, 4: charge / discharge is complete, >4: error
	 * @param outletNum
	 * @param dataBuffer
	 * @return true if channel # is active 
	 */
	public boolean isProcessing(final int outletNum, final byte[] channelBuffer, final byte[] dataBuffer) {
		if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, outletNum + " isProcessing = " + (dataBuffer == null ? channelBuffer[4] == 0x01 : dataBuffer[4] >= 0x01));
		if (dataBuffer == null) // initial processing type query
			return channelBuffer[4] == 0x01;
		return (channelBuffer[5] == 4 || channelBuffer[5] == 5) && dataBuffer[17] == 0x02 && !this.isContinuousRecordSet() && this.settings.isReduceChargeDischarge()
				? false 
				: dataBuffer[4] == 0x01;
	}
	
	/**
	 * STATUS:     0=standby 1=charge 2=discharge 3=resting 4=finish 0x80--0xff：error code
	 * @param dataBuffer
	 * @return
	 */
	public String getProcessingStatusName(final byte[] dataBuffer) {
		return this.isContinuousRecordSet() ? Messages.getString(MessageIds.GDE_MSGT3606) : this.STATUS_MODE[dataBuffer[4]];
	}

	/**
	 * battery type:  0:LiPo 1:LiIo 2:LiFe 3:LiHv 4:NiMH 5:NiCd 6:PB
	 * @param dataBuffer
	 * @return
	 */
	public String getProcessingBatteryTypeName(final byte[] channelBuffer) {
		return this.BATTERY_TYPE[channelBuffer[5]];
	}

	/**
	 * query the processing mode, main modes are charge/discharge, make sure the data buffer contains at index 15,16 the processing modes
	 * LI battery： 		0=BALACE-CHARGE 1=CHARGE 2=DISCHARGE 3=STORAGE 4=FAST-CHARGE
	 * Ni battery:		0=CHARGE 1=AUTO_CHARGE 2=DISCHARGE 3=RE_PEAK 4=CYCLE
	 * Pb battery:		0=CHARGE 1=DISCHARGE
	 * @param dataBuffer 
	 * @return
	 */
	public int getProcessingType(final byte[] channelBuffer) {
		return channelBuffer[7];
	}

	/**
	 * query battery type
	 * battery type:  0:LiPo 1:LiFe 2:LiIo 3:LiHv 4:NiMH 5:NiCd 6:PB
	 * @param dataBuffer
	 * @return
	 */
	public int getBatteryType(byte[] channelBuffer) {
		return channelBuffer[5];
	}

	/**
	 * query the processing type name
	 * @param channelBuffer
	 * @return string of mode
	 */
	public String getProcessingTypeName(final byte[] channelBuffer) {
		String processTypeName = GDE.STRING_EMPTY;
		//LI battery： 	0：CHARGE, 1：DISCHARGE, 2：STORAGE, 3：FAST CHG, 4：BALANCE
		//Ni battery:		0=CHARGE 1=AUTO_CHARGE 2=DISCHARGE 3=RE_PEAK 4=CYCLE
		//Pb battery:		0=CHARGE 1=DISCHARGE

		//battery type:  0:LiPo 1:LiFe 2:LiLo 3:LiHv 4:NiMH 5:NiCd 6:PB 
		switch (this.getBatteryType(channelBuffer)) {
		case 0: //LiPo
		case 1: //LiFe
		case 2: //LiIo
		case 3: //LiHV
			processTypeName = this.USAGE_MODE_LI[channelBuffer[7]];
			break;
		case 4: //NiCd
		case 5: //NiMH
			processTypeName = this.USAGE_MODE_NI[channelBuffer[7]];
			break;
		case 6: //PB
			processTypeName = this.USAGE_MODE_PB[channelBuffer[7]];
			break;
		}
		return this.isContinuousRecordSet() ? Messages.getString(MessageIds.GDE_MSGT3606) : processTypeName;
	}

	/**
	 * query the cycle number of the given outlet channel
	 * @param dataBuffer
	 * @return
	 */
	public int getCycleNumber(final byte[] dataBuffer) {
		return dataBuffer[18] + 1;
	}

	/**
	 * query the sub process type if in cycle
	 * 1=charge 2=pause 3=discharge
	 * @param dataBuffer
	 * @return
	 */
	public int getProcessSubType(final byte[] channelBuffer, final byte[] dataBuffer) {
		if (channelBuffer[5] == 4 || channelBuffer[5] == 5) 
			return dataBuffer[17];
		return this.getProcessingType(channelBuffer);
	}

	/**
	 * query the sub process name if in cycle
	 * LI battery： 		0=BALACE-CHARGE 1=CHARGE 2=DISCHARGE 3=STORAGE 4=FAST-CHARGE
	 * Ni battery:		0=CHARGE 1=AUTO_CHARGE 2=DISCHARGE 3=RE_PEAK 4=CYCLE
	 * Pb battery:		0=CHARGE 1=DISCHARGE
	 * battery type:  0:LiPo 1:LiIo 2:LiFe 3:LiHv 4:NiMH 5:NiCd 6:PB
	 * @param dataBuffer
	 * @return
	 */
	public String getProcessSubTypeName(final byte[] channelBuffer, final byte[] dataBuffer) {
		switch (this.getBatteryType(channelBuffer)) {
		default:
			return GDE.STRING_EMPTY;
			
		case 4: //NiMH
		case 5: //NiCd
			if (this.isContinuousRecordSet() || this.getProcessingType(channelBuffer) != 4) 
				return GDE.STRING_EMPTY;			
			break;
		}
		
		switch (dataBuffer[17]) {
		default:
		case 1: //charge
			return Messages.getString(MessageIds.GDE_MSGT3620);
		case 2: //pause
			return Messages.getString(MessageIds.GDE_MSGT3627);
		case 3: //discharge
			return Messages.getString(MessageIds.GDE_MSGT3623);
		}
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public int[] getCellVoltageOrdinals() {
		//0=Voltage 1=Current 2=Capacity 3=Power 4=Energy 5=TempExt  6=TempInt 7=Resistance 8=Balance 9=CellVoltage1....14=CellVoltage6
		return new int[] { 0, 2 };
	}
	
	/**
	 * @param channelBuffer
	 * @return number of configured cells
	 */
	public int getNumberCells(final byte[] channelBuffer) {
		return channelBuffer[6];
	}

	/**
	 * @return string containing firmware : major.minor
	 */
	public String getProductIdString() {
		return String.format("%2x", this.systemInfo[0].getProductId());
	}

	/**
	 * @return string containing firmware : major.minor
	 */
	public String getHarwareString() {
		return this.systemInfo[0].getHardwareVersion();
	}

	/**
	 * @return string containing firmware : major.minor
	 */
	public String getFirmwareString() {
		return this.systemInfo[0].getFirmwareVersion();
	}

//	/**
//	 * @return string containing system temperature unit
//	 */
//	public String getTemperatureUnit(final int productId) {
//		return this.systemSetting[productId-1].getTempeartureUnit();
//	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//no implementation required
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
		boolean configChanged = this.isChangePropery();
		Record record;
		MeasurementType measurement;
		//0=Voltage 1=Current 2=Capacity 3=Power 4=Energy 5=TempExt  6=TempInt 7=Resistance 8=Balance 9=CellVoltage1....14=CellVoltage6
		String[] measurementNames = this.getMeasurementNames(channelConfigNumber);
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			measurement = this.getMeasurement(channelConfigNumber, i);
			if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, record.getName() + " = " + measurementNames[i]); //$NON-NLS-1$

			// update active state and displayable state if configuration switched with other names
			if (record.isActive() && record.isActive() != measurement.isActive()) {
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData() && measurement.isActive());
				if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$ 
			}

			if (record.isActive() && record.isDisplayable()) {
				if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
		this.setChangePropery(configChanged); //reset configuration change indicator to previous value, do not vote automatic configuration change at all
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
		if (this.usbPort != null) {
			if (!this.usbPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.dataGatherThread = new ImaxB6GathererThread(this.application, this, this.usbPort, activChannel.getNumber(), this.getDialog());
						try {
							if (this.dataGatherThread != null && this.usbPort.isConnected()) {
								this.systemInfo[0] = new IMaxB6USB.SystemInfo(this.usbPort.getSystemInfo(this.dataGatherThread.getUsbInterface(), IMaxB6RDX1UsbPort.QuerySystemInfo.CHANNEL_A.value()));
								this.systemSetting[0] = new IMaxB6USB.SystemSetting(this.usbPort.getSystemSetting(this.dataGatherThread.getUsbInterface(), IMaxB6RDX1UsbPort.QuerySystemSetting.CHANNEL_A.value()));

								WaitTimer.delay(100);
								this.dataGatherThread.start();
							}
						}
						catch (Throwable e) {
							log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
					}
				}
				catch (UsbClaimException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(),
							Messages.getString(gde.messages.MessageIds.GDE_MSGE0051, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
					try {
						if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
					}
					catch (UsbException ex) {
						log.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
					}
				}
				catch (UsbException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0050));
					try {
						if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
					}
					catch (UsbException ex) {
						log.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
					}
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
				if (this.dataGatherThread != null) {
					this.dataGatherThread.stopDataGatheringThread(false, null);
				}
				//if (this.boundsComposite != null && !this.isDisposed()) this.boundsComposite.redraw();
				try {
					WaitTimer.delay(1000);
					if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
				}
				catch (UsbException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}
	
	/**
	 * implementation returning device name which is the default directory name to store OSD files as well to search for
	 * @return the preferred directory to search and store for device specific files
	 */
	public String getFileBaseDir() {
		return this.getName();
	}
}
