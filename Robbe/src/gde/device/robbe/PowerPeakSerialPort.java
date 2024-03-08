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
package gde.device.robbe;

import java.io.IOException;
import java.util.logging.Logger;

import gde.comm.DeviceCommPort;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.ui.DataExplorer;
import gde.utils.Checksum;
import gde.utils.StringHelper;

/**
 * PowerPeak devices serial port implementation
 * @author Winfried Brügmann
 */
public class PowerPeakSerialPort extends DeviceCommPort {
	final static String	$CLASS_NAME										= PowerPeakSerialPort.class.getName();
	final static Logger	log														= Logger.getLogger(PowerPeakSerialPort.$CLASS_NAME);

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
	public PowerPeakSerialPort(PowerPeak currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
	}

	/**
	 * constructor for testing purpose
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public PowerPeakSerialPort(DeviceConfiguration deviceConfiguration) {
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
			log.logp(java.util.logging.Level.INFO, PowerPeakSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.convert2CharString(answer));

			// synchronize received data to DeviceCommPort.FF of sent data 
			while (answer[0] != DeviceCommPort.FF) {
				this.isInSync = false;
				for (int i = 1; i < answer.length; i++) {
					if (answer[i] == DeviceCommPort.FF) {
						System.arraycopy(answer, i, data, 0, data.length - i);
						answer = new byte[i];
						answer = this.read(answer, 1000);
						log.logp(java.util.logging.Level.INFO, PowerPeakSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.convert2CharString(answer));
						System.arraycopy(answer, 0, data, data.length - i, i);
						this.isInSync = true;
						log.logp(java.util.logging.Level.INFO, PowerPeakSerialPort.$CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				if (this.isInSync) break;

				this.addXferError();
				log.logp(java.util.logging.Level.WARNING, PowerPeakSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> unable to synchronize received data, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > PowerPeakSerialPort.xferErrorLimit) throw new SerialPortException("Number of transfer error exceed the acceptable limit of " + PowerPeakSerialPort.xferErrorLimit); //$NON-NLS-1$
				//this.write(PowerPeakSerialPort.RESET);
				answer = new byte[data.length];
				answer = this.read(answer, 3000);
			}

			if (checkBeginEndSignature && !(data[0] == DeviceCommPort.FF && data[data.length - 1] == DeviceCommPort.CR)) {
				this.addXferError();
				log.logp(java.util.logging.Level.WARNING, PowerPeakSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> data start or end does not match, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > PowerPeakSerialPort.xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + PowerPeakSerialPort.xferErrorLimit); //$NON-NLS-1$
				//this.write(PowerPeakSerialPort.RESET);
				data = getData(true);
			}

			if (checkBeginEndSignature && !isChecksumOK(data)) {
				this.addXferError();
				log.logp(java.util.logging.Level.WARNING, PowerPeakSerialPort.$CLASS_NAME, $METHOD_NAME, "=====> checksum error occured, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > PowerPeakSerialPort.xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + PowerPeakSerialPort.xferErrorLimit); //$NON-NLS-1$
				//this.write(PowerPeakSerialPort.RESET);
				data = getData(true);
			}
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(java.util.logging.Level.SEVERE, PowerPeakSerialPort.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		return data;
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
				log.logp(java.util.logging.Level.WARNING, PowerPeakSerialPort.$CLASS_NAME, $METHOD_NAME,
						"check sum missmatch detected, calculates check_sum = " + check_sum + "; delta to data contained delta = " //$NON-NLS-1$ //$NON-NLS-2$
								+ (buffer_check_sum - check_sum));
				log.logp(java.util.logging.Level.WARNING, PowerPeakSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.convert2CharString(buffer));
				this.isDataMissmatchWarningWritten = true;
				this.dataCheckSumOffset = buffer_check_sum - check_sum;
				isOK = true;
			}
		}
		return isOK;
	}
}
