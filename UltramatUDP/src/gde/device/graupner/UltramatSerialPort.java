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
package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.Checksum;
import gde.utils.StringHelper;

import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Ultramat devices serial port implementation
 * @author Winfried Brügmann
 */
public class UltramatSerialPort extends DeviceCommPort {
	final static String	$CLASS_NAME										= UltramatSerialPort.class.getName();
	final static Logger	log														= Logger.getLogger(UltramatSerialPort.$CLASS_NAME);

	final static byte[]	RESET_CONFIG									= new byte[] { DeviceCommPort.FF, 0x41, 0x37, 0x30, 0x30, 0x30, 0x30, 0x44, 0x38, DeviceCommPort.CR };	//1000 1111
	final static byte[]	RESET													= new byte[] { DeviceCommPort.FF, 0x43, 0x30, 0x30, 0x30, 0x30, 0x30, 0x44, 0x33, DeviceCommPort.CR };	//1000 1111
	final static byte[]	READ_MEMORY_NAME							= new byte[] { DeviceCommPort.FF, '8', '0', '0', '0', '0', '0', '0', '0', DeviceCommPort.CR };					//1000 0000
	final static byte[]	WRITE_MEMORY_NAME							= new byte[] { DeviceCommPort.FF, '0', '0' };																																//0000 0000
	final static byte[]	READ_MEMORY_SETUP							= new byte[] { DeviceCommPort.FF, '8', '1', '0', '0', '0', '0', '0', '0', DeviceCommPort.CR };					//1000 0001
	final static byte[]	WRITE_MEMORY_SETUP						= new byte[] { DeviceCommPort.FF, '0', '1' };																																//0000 0001
	final static byte[]	READ_MEMORY_STEP_CHARGE_SETUP	= new byte[] { DeviceCommPort.FF, '8', '2', '0', '0', '0', '0', '0', '0', DeviceCommPort.CR };					//1000 0010
	final static byte[]	WRITE_STEP_CHARGE_SETUP				= new byte[] { DeviceCommPort.FF, '0', '2' };																																//0000 0010
	final static byte[]	READ_MEMORY_CYCLE_DATA				= new byte[] { DeviceCommPort.FF, '8', '3', '0', '0', '0', '0', '0', '0', DeviceCommPort.CR };					//1000 0011
	final static byte[]	WRITE_CYCLE_DATA							= new byte[] { DeviceCommPort.FF, '0', '3' };																																//0000 0011
	final static byte[]	READ_MEMORY_TRACE_DATA				= new byte[] { DeviceCommPort.FF, '8', '4', '0', '0', '0', '0', '0', '0', DeviceCommPort.CR };					//1000 0100
	final static byte[]	WRITE_TRACE_DATA							= new byte[] { DeviceCommPort.FF, '0', '4' };																																//0000 0100
	final static byte[]	READ_TIRE_HEATER							= new byte[] { DeviceCommPort.FF, '8', '5', '0', '0', '0', '0', '0', '0', DeviceCommPort.CR };					//1000 0101
	final static byte[]	WRITE_TIRE_HEATER							= new byte[] { DeviceCommPort.FF, '0', '5' };																																//0000 0101
	final static byte[]	READ_MOTOR_RUN								= new byte[] { DeviceCommPort.FF, '8', '6', '0', '0', '0', '0', '0', '0', DeviceCommPort.CR };					//1000 0110
	final static byte[]	WRITE_MOTOR_RUN								= new byte[] { DeviceCommPort.FF, '0', '6' };																																//0000 0110
	final static byte[]	READ_CHANNEL_SETUP						= new byte[] { DeviceCommPort.FF, '8', '7', '0', '0', '0', '0', '0', '0', DeviceCommPort.CR };					//1000 0111
	final static byte[]	WRITE_CHANNEL_SETUP						= new byte[] { DeviceCommPort.FF, '0', '7' };																																//0000 0111
	final static byte[]	READ_DEVICE_IDENTIFIER_NAME		= new byte[] { DeviceCommPort.FF, '8', '8', '0', '0', '0', '0', '0', '0', DeviceCommPort.CR };					//1000 1000
	final static byte[]	WRITE_DEVICE_IDENTIFIER_NAME	= new byte[] { DeviceCommPort.FF, '0', '8' };																																//0000 1000
	final static byte[]	READ_GRAPHICS_DATA						= new byte[] { DeviceCommPort.FF, '8', '9', '0', '0', '0', '0', '0', '0', DeviceCommPort.CR };					//1000 1001
	final static byte[]	WRITE_GRAPHICS_DATA						= new byte[] { DeviceCommPort.FF, '0', '9' };																																//0000 1001

	static int					SIZE_MEMORY_SETUP							= 28;
	final static int		SIZE_MEMORY_STEP_CHARGE_SETUP	= 20;
	static int					SIZE_MEMORY_TRACE							= 6;
	static int					SIZE_MEMORY_CYCLE							= 121;
	static int					SIZE_CHANNEL_1_SETUP					= 16;
	final static int		SIZE_CHANNEL_2_SETUP					= 4;
	final static int		SIZE_TIRE_HEATER_SETUP				= 8;
	final static int		SIZE_MOTOR_RUN_SETUP					= 17;

	final static int		xferErrorLimit								= 15;

	boolean							isInSync											= false;
	boolean							isDataMissmatchWarningWritten	= false;
	int									dataCheckSumOffset						= 0;
	boolean							isCmdMissmatchWarningWritten	= false;
	int									cmdCheckSumOffset							= 0;

	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public UltramatSerialPort(Ultramat currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
		switch (currentDevice.getDeviceTypeIdentifier()) {
		case UltraDuoPlus40:
			UltramatSerialPort.SIZE_CHANNEL_1_SETUP = 19; // additional supply input 2 voltage, current
			UltramatSerialPort.SIZE_MEMORY_SETUP = 27;
			break;
			
		case UltraDuoPlus45:
			UltramatSerialPort.SIZE_CHANNEL_1_SETUP = 19; // additional supply input 2 voltage, current
			UltramatSerialPort.SIZE_MEMORY_TRACE = 5;
			break;

		case UltraDuoPlus50:
			UltramatSerialPort.SIZE_CHANNEL_1_SETUP = 16;
			UltramatSerialPort.SIZE_MEMORY_TRACE = 5;
			UltramatSerialPort.SIZE_MEMORY_SETUP = 27;
			break;
			
		case UltraDuoPlus60:
		default:
			UltramatSerialPort.SIZE_CHANNEL_1_SETUP = 16;
			break;
		}
	}

	/**
	 * constructor for testing purpose
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public UltramatSerialPort(DeviceConfiguration deviceConfiguration) {
		super(deviceConfiguration);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData(boolean checkBeginEndSignature) throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
		byte[] data = new byte[Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO))];
		byte[] answer = new byte[] { 0x00 };

		try {

			answer = new byte[data.length];
			answer = this.read(data, 3000);
			// synchronize received data to DeviceCommPort.FF of sent data 
			while (answer[0] != DeviceCommPort.FF) {
				this.isInSync = false;
				for (int i = 1; i < answer.length; i++) {
					if (answer[i] == DeviceCommPort.FF) {
						System.arraycopy(answer, i, data, 0, data.length - i);
						answer = new byte[i];
						answer = this.read(answer, 1000);
						System.arraycopy(answer, 0, data, data.length - i, i);
						this.isInSync = true;
						log.logp(java.util.logging.Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				if (this.isInSync) break;

				this.addXferError();
				log.logp(java.util.logging.Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> unable to synchronize received data, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > UltramatSerialPort.xferErrorLimit) throw new SerialPortException("Number of transfer error exceed the acceptable limit of " + UltramatSerialPort.xferErrorLimit); //$NON-NLS-1$
				this.write(UltramatSerialPort.RESET);
				answer = new byte[data.length];
				answer = this.read(answer, 3000);
			}
			log.logp(java.util.logging.Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.convert2CharString(data));

			if (checkBeginEndSignature && !(data[0] == DeviceCommPort.FF && data[data.length - 1] == DeviceCommPort.CR)) {
				this.addXferError();
				log.logp(java.util.logging.Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> data start or end does not match, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > UltramatSerialPort.xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + UltramatSerialPort.xferErrorLimit); //$NON-NLS-1$
				this.write(UltramatSerialPort.RESET);
				data = getData(true);
			}

			if (checkBeginEndSignature && !isChecksumOK(data)) {
				this.addXferError();
				log.logp(java.util.logging.Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "=====> checksum error occured, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > UltramatSerialPort.xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + UltramatSerialPort.xferErrorLimit); //$NON-NLS-1$
				this.write(UltramatSerialPort.RESET);
				data = getData(true);
			}
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(java.util.logging.Level.SEVERE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		return data;
	}

	/**
	 * read the device user name
	 * @return string configured in device
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readDeviceUserName() throws IOException, TimeOutException, SerialPortException {
		byte[] answer = this.readConfigData(UltramatSerialPort.READ_DEVICE_IDENTIFIER_NAME, 23, 2);
		return String.format(DeviceCommPort.FORMAT_16_CHAR, answer[1], answer[2], answer[3], answer[4], answer[5], answer[6], answer[7], answer[8], answer[9], answer[10], answer[11], answer[12],
				answer[13], answer[14], answer[15], answer[16]);
	}

	/**
	 * read the outlet channel setup data
	 * @param channelNumber
	 * @return values integer array
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readChannelData(int channelNumber) throws IOException, TimeOutException, SerialPortException {
		if (channelNumber == 1) {
			return new String(this.readConfigData(UltramatSerialPort.READ_CHANNEL_SETUP, UltramatSerialPort.SIZE_CHANNEL_1_SETUP * 4 + 7, channelNumber)).substring(1,
					UltramatSerialPort.SIZE_CHANNEL_1_SETUP * 4 + 1);
		}
		else if (channelNumber == 2) {
			return new String(this.readConfigData(UltramatSerialPort.READ_CHANNEL_SETUP, UltramatSerialPort.SIZE_CHANNEL_2_SETUP * 4 + 7, channelNumber)).substring(1,
					UltramatSerialPort.SIZE_CHANNEL_2_SETUP * 4 + 1);
		}
		return GDE.STRING_EMPTY;
	}

	/**
	 * read the name of memory with given number 0 - 39 for memory 01 - 40
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readMemoryName(int number) throws IOException, TimeOutException, SerialPortException {
		byte[] answer = this.readConfigData(UltramatSerialPort.READ_MEMORY_NAME, 23, number);
		for (int i = 1; i < answer.length-1; i++) {
			if (answer[i] < 31 || answer[i] > 126) answer[i] = ' ';
		}
		return String.format(DeviceCommPort.FORMAT_16_CHAR, answer[1], answer[2], answer[3], answer[4], answer[5], answer[6], answer[7], answer[8], answer[9], answer[10], answer[11], answer[12],
				answer[13], answer[14], answer[15], answer[16]);
	}

	/**
	 * read the memory setup values for given memory number
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readMemorySetup(int number) throws IOException, TimeOutException, SerialPortException {
		switch (((Ultramat)this.device).getDeviceTypeIdentifier()) {
		case UltraDuoPlus40:
		case UltraDuoPlus50: //no checksum !
			return new String(this.readConfigData(UltramatSerialPort.READ_MEMORY_SETUP, UltramatSerialPort.SIZE_MEMORY_SETUP * 4 + 3, number)).substring(1, UltramatSerialPort.SIZE_MEMORY_SETUP * 4 + 1);
			
		case UltraDuoPlus45:
		case UltraDuoPlus60:
		default:
			return new String(this.readConfigData(UltramatSerialPort.READ_MEMORY_SETUP, UltramatSerialPort.SIZE_MEMORY_SETUP * 4 + 7, number)).substring(1, UltramatSerialPort.SIZE_MEMORY_SETUP * 4 + 1);
		}
	}

	/**
	 * read the memory trace data for given memory number
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readMemoryTrace(int number) throws IOException, TimeOutException, SerialPortException {
		return new String(this.readConfigData(UltramatSerialPort.READ_MEMORY_TRACE_DATA, UltramatSerialPort.SIZE_MEMORY_TRACE * 4 + 7, number)).substring(1, UltramatSerialPort.SIZE_MEMORY_TRACE * 4 + 1);
	}

	/**
	 * read the memory cycle data for given memory number
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readMemoryCycle(int number) throws IOException, TimeOutException, SerialPortException {
		return new String(this.readConfigData(UltramatSerialPort.READ_MEMORY_CYCLE_DATA, UltramatSerialPort.SIZE_MEMORY_CYCLE * 4 + 7, number)).substring(1, UltramatSerialPort.SIZE_MEMORY_CYCLE * 4 + 1);
	}

	/**
	 * read the memory step charge data for given memory number
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readMemoryStepChargeSetup(int number) throws IOException, TimeOutException, SerialPortException {
		return new String(this.readConfigData(UltramatSerialPort.READ_MEMORY_STEP_CHARGE_SETUP, UltramatSerialPort.SIZE_MEMORY_STEP_CHARGE_SETUP * 4 + 7, number)).substring(1,
				UltramatSerialPort.SIZE_MEMORY_STEP_CHARGE_SETUP * 4 + 1);
	}

	/**
	 * read configuration data according given type and expected data size for a given index (channel, memory number, ...)
	 * @param type
	 * @param expectedDataSize
	 * @param index
	 * @return byte array containing the requested configuration data
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized byte[] readConfigData(byte[] type, int expectedDataSize, int index) throws IOException, TimeOutException, SerialPortException {
		final String $METHOD_NAME = "readConfigData"; //$NON-NLS-1$
		log.logp(java.util.logging.Level.FINEST, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "entry"); //$NON-NLS-1$
		byte[] readBuffer = new byte[expectedDataSize];

		if (this.isConnected()) {
			byte[] writeBuffer = type;
			byte[] num = String.format("%02X", index).getBytes(); //$NON-NLS-1$
			System.arraycopy(num, 0, writeBuffer, 3, 2);
			byte[] checkSum = getChecksum(writeBuffer);
			System.arraycopy(checkSum, 0, writeBuffer, 5, 4);
			log.logp(java.util.logging.Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "write = " + StringHelper.convert2CharString(writeBuffer)); //$NON-NLS-1$

			this.write(writeBuffer);
			byte[] answer = this.read(readBuffer, 3000);
			while (answer[0] != DeviceCommPort.FF) {
				this.isInSync = false;
				for (int i = 1; i < answer.length; i++) {
					if (answer[i] == DeviceCommPort.FF) {
						System.arraycopy(answer, i, readBuffer, 0, readBuffer.length - i);
						answer = new byte[i];
						answer = this.read(answer, 1000);
						System.arraycopy(answer, 0, readBuffer, readBuffer.length - i, i);
						this.isInSync = true;
						log.logp(java.util.logging.Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				if (this.isInSync) break;

				this.addXferError();
				log.logp(java.util.logging.Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> unable to synchronize received data, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > UltramatSerialPort.xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + UltramatSerialPort.xferErrorLimit); //$NON-NLS-1$
				this.write(UltramatSerialPort.RESET);
				answer = new byte[expectedDataSize];
				answer = this.read(answer, 3000);
			}
			if (!(readBuffer[0] == DeviceCommPort.FF && readBuffer[readBuffer.length - 1] == DeviceCommPort.ACK && isCommandChecksumOK(readBuffer))) {
				this.addXferError();
				log.logp(java.util.logging.Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> data start or end does not match, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > UltramatSerialPort.xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + UltramatSerialPort.xferErrorLimit); //$NON-NLS-1$
				this.write(UltramatSerialPort.RESET);
				readBuffer = readConfigData(type, expectedDataSize, index);
			}
			log.logp(java.util.logging.Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "readBuffer = " + StringHelper.convert2CharString(readBuffer)); //$NON-NLS-1$
		}
		return readBuffer;
	}

	/**
	 * read last available graphics data for the given  channel number
	 * @param graphicsRecordSetData pointer to the graphics data organized in a array[3][]
	 * @param channelNumber 1 | 2 supported by device only
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized void readGraphicsData(byte[][] graphicsRecordSetData, int channelNumber, UltraDuoPlusDialog dialog) throws IOException, TimeOutException, SerialPortException {
		final String $METHOD_NAME = "readConfigData"; //$NON-NLS-1$
		log.logp(java.util.logging.Level.FINEST, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "entry"); //$NON-NLS-1$
		Vector<Byte> graphicsData = new Vector<Byte>();
		int redPoints = 0;
		int numBytes = 1;
		dialog.setGraphicsDataReadProgress(redPoints);

		if (this.isConnected()) {
			for (int i = 0; i < 3; i++) {//voltage=0, current=1, temperature=2 
				byte[] writeBuffer = UltramatSerialPort.READ_GRAPHICS_DATA;
				byte[] num = String.format("%d%X", i, channelNumber).getBytes(); //$NON-NLS-1$
				System.arraycopy(num, 0, writeBuffer, 3, 2);
				byte[] checkSum = getChecksum(writeBuffer);
				System.arraycopy(checkSum, 0, writeBuffer, 5, 4);
				log.logp(java.util.logging.Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "write = " + StringHelper.convert2CharString(writeBuffer)); //$NON-NLS-1$
				this.write(writeBuffer);

				byte[] readBuffer = new byte[9];
				byte[] answer = this.read(readBuffer, 3000);
				int numOfPoints = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, answer[1], answer[2], answer[3], answer[4]), 16);
				int timeStep_sec = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, answer[5], answer[6], answer[7], answer[8]), 16);
				numBytes = numOfPoints * 3 * 4 + 3 * 9 + 5;
				for (byte b : answer) {
					graphicsData.add(b);
				}
				redPoints += 9;
				dialog.setGraphicsDataReadProgress(redPoints * 100 / numBytes);
				log.logp(java.util.logging.Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, String.format("numOfPoints = %d; timeStep_sec = %d", numOfPoints, timeStep_sec)); //$NON-NLS-1$ 
				readBuffer = new byte[numOfPoints]; // 4 byte per point + chksum + 0xD0
				for (int j = 0; j < 4; j++) {
					answer = this.read(readBuffer, 5000);
					for (byte b : answer) {
						graphicsData.add(b);
					}
					redPoints += numOfPoints;
					dialog.setGraphicsDataReadProgress(redPoints * 100 / numBytes);
				}
				readBuffer = new byte[1]; // checksum + 0x0D
				while (readBuffer[0] != DeviceCommPort.ACK) {
					answer = this.read(readBuffer, 1000);
					graphicsData.add(answer[0]);
					redPoints += 1;
					dialog.setGraphicsDataReadProgress(redPoints * 100 / numBytes);
				}
				readBuffer = new byte[graphicsData.size()];
				for (int j = 0; j < readBuffer.length; j++) {
					readBuffer[j] = graphicsData.elementAt(j);
				}
				log.logp(java.util.logging.Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "readBuffer = " + StringHelper.convert2CharString(readBuffer)); //$NON-NLS-1$
				graphicsRecordSetData[i] = readBuffer.clone();
				graphicsData.clear();
			}
			dialog.setGraphicsDataReadProgress(redPoints * 100 / numBytes);
		}
	}

	/**
	 * write configuration data according given type and expected data size for a given index (channel, memory number, ...)
	 * @param type
	 * @param configData
	 * @param index
	 * @throws IOException if writing configuration data failed
	 * @throws TimeOutException
	 */
	public synchronized void writeConfigData(byte[] type, byte[] configData, int index) throws IOException, TimeOutException {
		final String $METHOD_NAME = "writeConfigData"; //$NON-NLS-1$
		log.logp(java.util.logging.Level.FINEST, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "entry"); //$NON-NLS-1$
		byte[] writeBuffer = new byte[configData.length + 10]; //0x0C, type, index, data..., checksum, 0x0D

		if (this.isConnected()) {
			System.arraycopy(type, 0, writeBuffer, 0, 3);
			writeBuffer[writeBuffer.length - 1] = DeviceCommPort.CR;
			byte[] num = String.format("%02X", index).getBytes(); //$NON-NLS-1$
			System.arraycopy(num, 0, writeBuffer, 3, 2);
			System.arraycopy(configData, 0, writeBuffer, 5, configData.length);
			byte[] checkSum = getChecksum(writeBuffer);
			System.arraycopy(checkSum, 0, writeBuffer, writeBuffer.length - 5, 4);
			log.logp(java.util.logging.Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "answer = " + StringHelper.convert2CharString(writeBuffer)); //$NON-NLS-1$

			this.write(writeBuffer);
			byte[] answer = this.read(new byte[1], 3000);
			log.logp(java.util.logging.Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "answer = " + StringHelper.convert2CharString(answer)); //$NON-NLS-1$
			if ((answer[0] == DeviceCommPort.NAK)) {
				log.log(java.util.logging.Level.WARNING, "Writing UltraDuoPlus configuration type (" + new String(type) + ") data failed!"); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException("Writing UltraDuoPlus configuration type (" + new String(type) + ") data failed!"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private synchronized boolean isChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK"; //$NON-NLS-1$
		boolean isOK = false;
		int length = buffer.length;
		int check_sum = Checksum.ADD(buffer, 1, length - 6);
		int buffer_check_sum = Integer.parseInt(
				String.format(DeviceCommPort.FORMAT_4_CHAR, (char) buffer[length - 5], (char) buffer[length - 4], (char) buffer[length - 3], (char) buffer[length - 2]), 16);
		if (check_sum == buffer_check_sum || check_sum == (buffer_check_sum - this.dataCheckSumOffset))
			isOK = true;
		else {
			//some devices has a constant checksum offset by firmware error, calculate this offset first time the mismatch occurs and tolerate the calculated offset afterwards
			if (!this.isDataMissmatchWarningWritten) {
				log.logp(java.util.logging.Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME,
						"check sum missmatch detected, calculates check_sum = " + check_sum + "; delta to data contained delta = " //$NON-NLS-1$ //$NON-NLS-2$
								+ (buffer_check_sum - check_sum));
				log.logp(java.util.logging.Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.convert2CharString(buffer));
				this.isDataMissmatchWarningWritten = true;
				this.dataCheckSumOffset = buffer_check_sum - check_sum;
				isOK = true;
			}
		}
		return isOK;
	}

	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private synchronized boolean isCommandChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK"; //$NON-NLS-1$
		boolean isOK = false;
		int length = buffer.length;
		int check_sum = Checksum.ADD(buffer, 1, length - 7);
		int buffer_check_sum = Integer.parseInt(
				String.format(DeviceCommPort.FORMAT_4_CHAR, (char) buffer[length - 6], (char) buffer[length - 5], (char) buffer[length - 4], (char) buffer[length - 3]), 16);
		if (check_sum == buffer_check_sum || check_sum == (buffer_check_sum - this.cmdCheckSumOffset))
			isOK = true;
		else {
			//some devices has a constant checksum offset by firmware error, calculate this offset first time the mismatch occurs and tolerate the calculated offset afterwards
			if (!this.isDataMissmatchWarningWritten) {
				log.logp(java.util.logging.Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME,
						"check sum missmatch detected, calculates check_sum = " + check_sum + "; delta to data contained delta = " //$NON-NLS-1$ //$NON-NLS-2$
								+ (buffer_check_sum - check_sum));
				log.logp(java.util.logging.Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.convert2CharString(buffer));
				this.isDataMissmatchWarningWritten = true;
				this.dataCheckSumOffset = buffer_check_sum - check_sum;
				isOK = true;
			}
		}
		return isOK;
	}

	/**
	 * calculate the checksum of the buffer and translate it to required format
	 * @param buffer
	 * @return
	 */
	private synchronized byte[] getChecksum(byte[] buffer) {
		final String $METHOD_NAME = "getChecksum"; //$NON-NLS-1$
		String check_sum = String.format("%04X", Checksum.ADD(buffer, 1, buffer.length - 6)); //$NON-NLS-1$
		log.logp(java.util.logging.Level.FINER, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "Check_sum char[]= " + check_sum); //$NON-NLS-1$
		return check_sum.getBytes();
	}

	/**
	 * query memory cycle data, time stamp, capacity, voltage, resistance for charge and discharge
	 * @param memoryNumber
	 * @return
	 * @throws TimeOutException 
	 * @throws IOException 
	 * @throws SerialPortException 
	 */
	public synchronized Vector<byte[]> readMemoryCycleData(int memoryNumber) throws SerialPortException, IOException, TimeOutException {
		Vector<byte[]> result = new Vector<byte[]>();
		String memoryCycleData = readMemoryCycle(memoryNumber);
		for (int i = 0; i < 11; i++) {
			int startIndex = i * (11 * 4);
			int endIndex = (i + 1) * (11 * 4);
			result.add(memoryCycleData.substring(startIndex, endIndex).getBytes());
		}
		return result;
	}

	/**
	 * query memory cycle data, time stamp, capacity, voltage, resistance for charge and discharge
	 * @param memoryNumber
	 * @param cyclesData
	 * @throws TimeOutException 
	 * @throws IOException 
	 * @throws SerialPortException 
	 */
	public synchronized void writeMemoryCycleData(int memoryNumber, Vector<byte[]> cyclesData) throws SerialPortException, IOException, TimeOutException {
		StringBuilder sb = new StringBuilder();
		for (byte[] bytes : cyclesData) {
			sb.append(new String(bytes));
		}
		log.log(Level.FINER, sb.toString());
		
		//clear config data
		//sb = new StringBuilder().append("000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
		//05 FlugAkku 3200 A original data
		//sb = new StringBuilder().append("00030026000B00040011050141C2040C00213F880000000B0004000B00040018017341D905910000000000000009002D000B0005000709BF417E03B50000000000000008002B000C00050009084341DC04B4000B0000000000000032000F00050012001A41F0000000000000000000090015000C0005001D0A2E4194044E00000000000000100004000B000600040ADF41960566000F0022001D00120034000B0006000C07D0418004740000000000000003000E000E0006000D094241E206A30000000000000000000000000000000000000000000000000000000000210088000000000000000000000000000000000000");
		//sb = new StringBuilder().append("00030026000B00040011050141C2040C00213F88000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
		//05 FlugAkku 3200 A corrected data
		//sb = new StringBuilder().append("00030026000B00040011050141C2040C00213F880000000B0004000B00040018017341D905910000000000000009002D000B0005000709BF417E03B500000000000000130013000b0005000a084341DC04B4000B0000000000000000000000000001001A41F0000000000000000000130028000b0005001e0A2E4194044E00000000000000100004000B000600040ADF41960566000F0022001D00120034000B0006000C07D041800474000000000000000d0022000b00060010094241E206A30000000000000000000000000000000000000000000000000000000000210088000000000000000000000000000000000000");
		this.writeConfigData(UltramatSerialPort.WRITE_CYCLE_DATA, sb.toString().getBytes(), memoryNumber);
	}
}
