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
import java.util.logging.Logger;

/**
 * Ultramat devices serial port implementation
 * @author Winfried Brügmann
 */
public class PolaronSerialPort extends DeviceCommPort {
	final static String	$CLASS_NAME										= PolaronSerialPort.class.getName();
	final static Logger	log														= Logger.getLogger(PolaronSerialPort.$CLASS_NAME);

	final static byte[] QUERY_DATA										= new byte[] {0x00, (byte) 0x99};
	final static byte[]	RESET													= new byte[] { DeviceCommPort.FF, 0x43, 0x30, 0x30, 0x30, 0x30, 0x30, 0x44, 0x33, DeviceCommPort.CR };	//1000 1111

	final static int		xferErrorLimit								= 15;

	byte								cntUp									= 0x00;
	byte								cntDown								= (byte) 0xFF;
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
	public PolaronSerialPort(Polaron currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
//		switch (currentDevice.getDeviceTypeIdentifier()) {
//		case UltraDuoPlus40:
//			PolaronSerialPort.SIZE_CHANNEL_1_SETUP = 19; // additional supply input 2 voltage, current
//			PolaronSerialPort.SIZE_MEMORY_SETUP = 27;
//			break;
//			
//		case UltraDuoPlus45:
//			PolaronSerialPort.SIZE_CHANNEL_1_SETUP = 19; // additional supply input 2 voltage, current
//			PolaronSerialPort.SIZE_MEMORY_TRACE = 5;
//			break;
//
//		case UltraDuoPlus50:
//			PolaronSerialPort.SIZE_CHANNEL_1_SETUP = 16;
//			PolaronSerialPort.SIZE_MEMORY_TRACE = 5;
//			PolaronSerialPort.SIZE_MEMORY_SETUP = 27;
//			break;
//			
//		case UltraDuoPlus60:
//		default:
//			PolaronSerialPort.SIZE_CHANNEL_1_SETUP = 16;
//			break;
//		}
	}

	/**
	 * constructor for testing purpose
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public PolaronSerialPort(DeviceConfiguration deviceConfiguration) {
		super(deviceConfiguration);
	}

	/**
	 * prepare simple command for sending
	 * @param cmd
	 * @return
	 */
	private byte[] prepareCmdBytes(byte[] cmd) {
		return prepareCmdBytes(cmd, GDE.STRING_EMPTY);
	}

	/**
	 * prepare command with string parameter for sending
	 * @param cmd
	 * @param body
	 * @return
	 */
	private byte[] prepareCmdBytes(byte[] cmd, String body) {
		byte[] b = new byte[body.length() == 0 ? body.length() + 9 : body.length() + 10];
		b[0] = 0x00;
		if (this.cntUp == 0xFE || this.cntDown == 0x01) {
			this.cntUp = 0x00;
			this.cntDown = (byte) 0xFF;
		}
		b[1] = this.cntUp += 0x01;
		b[2] = this.cntDown -= 0x01;
		b[3] = (byte) (body.length() == 0 ? (body.length() & 0xFF) : ((body.length() + 1) & 0xFF));
		b[4] = 0x00;
		b[5] = cmd[0];
		b[6] = cmd[1];
		int i = 7;
		for (; i < body.length() + 7; ++i) {
			b[i] = (byte) (body.getBytes()[i - 7] & 0xFF);
		}
		if (body.length() > 0) b[i++] = 0x00;
		short crc16 = Checksum.CRC16CCITT(b, 3, (body.length() == 0 ? body.length() + 4 : body.length() + 5));
		b[i++] = (byte) (crc16 & 0x00FF);
		b[i++] = (byte) ((crc16 & 0xFF00) >> 8);

		if (PolaronSerialPort.log.isLoggable(Level.FINER)) PolaronSerialPort.log.log(Level.FINER, StringHelper.byte2Hex2CharString(b, b.length));
		return b;
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
		byte[] data = new byte[Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO))];
		byte[] answer = new byte[] { 0x00 };

		try {
			this.write(prepareCmdBytes(QUERY_DATA));
			
			answer = new byte[data.length];
			answer = this.read(data, 2000);
			if (PolaronSerialPort.log.isLoggable(Level.FINER)) log.logp(Level.FINER, PolaronSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(answer, answer.length));

			if (answer.length == 0 || answer[answer.length-1] == 0x00 && answer[answer.length-2] == 0x00) {
				throw new TimeOutException("no data");
			}
			
			if (!isCheckSumOK(3, data)) {
				this.addXferError();
				log.logp(Level.WARNING, PolaronSerialPort.$CLASS_NAME, $METHOD_NAME, "=====> checksum error occured, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > PolaronSerialPort.xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + PolaronSerialPort.xferErrorLimit); //$NON-NLS-1$
			}
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(Level.SEVERE, PolaronSerialPort.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		return data;
	}

	/**
	 * method to gather data from device Polaron Sports (follow old Graupner serial handshake paradigma)
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
						if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, PolaronSerialPort.$CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				if (this.isInSync) break;

				this.addXferError();
				log.logp(Level.WARNING, PolaronSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> unable to synchronize received data, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > PolaronSerialPort.xferErrorLimit) throw new SerialPortException("Number of transfer error exceed the acceptable limit of " + PolaronSerialPort.xferErrorLimit); //$NON-NLS-1$
				this.write(PolaronSerialPort.RESET);
				answer = new byte[data.length];
				answer = this.read(answer, 3000);
			}
			if (PolaronSerialPort.log.isLoggable(Level.FINE)) log.logp(Level.FINE, PolaronSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.convert2CharString(data));

			if (checkBeginEndSignature && !(data[0] == DeviceCommPort.FF && data[data.length - 1] == DeviceCommPort.CR)) {
				this.addXferError();
				log.logp(Level.WARNING, PolaronSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> data start or end does not match, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > PolaronSerialPort.xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + PolaronSerialPort.xferErrorLimit); //$NON-NLS-1$
				this.write(PolaronSerialPort.RESET);
				data = getData(true);
			}

			if (checkBeginEndSignature && !isChecksumOK(data)) {
				this.addXferError();
				log.logp(Level.WARNING, PolaronSerialPort.$CLASS_NAME, $METHOD_NAME, "=====> checksum error occured, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > PolaronSerialPort.xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + PolaronSerialPort.xferErrorLimit); //$NON-NLS-1$
				this.write(PolaronSerialPort.RESET);
				data = getData(true);
			}
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(Level.SEVERE, PolaronSerialPort.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		return data;
	}

	/**
	 * query checksum OK
	 * @param startIndex
	 * @param bytes
	 * @return true|false
	 */
	public boolean isCheckSumOK(int startIndex, byte[] bytes) {
		final String $METHOD_NAME = "isCheckSumOK";
		short checksum = Checksum.CRC16CCITT(bytes, startIndex, bytes.length - 2 - startIndex);
		if (PolaronSerialPort.log.isLoggable(Level.FINE))
			PolaronSerialPort.log.logp(Level.FINE, PolaronSerialPort.$CLASS_NAME, $METHOD_NAME,
					String.format("checksum: %b - %04X", ((checksum & 0xFF00) >> 8) == (bytes[bytes.length - 1] & 0xFF) && (checksum & 0x00FF) == (bytes[bytes.length - 2] & 0xFF), checksum));
		return ((checksum & 0xFF00) >> 8) == (bytes[bytes.length - 1] & 0xFF) && (checksum & 0x00FF) == (bytes[bytes.length - 2] & 0xFF);
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
				log.logp(Level.WARNING, PolaronSerialPort.$CLASS_NAME, $METHOD_NAME,
						"check sum missmatch detected, calculates check_sum = " + check_sum + "; delta to data contained delta = " //$NON-NLS-1$ //$NON-NLS-2$
								+ (buffer_check_sum - check_sum));
				log.logp(Level.WARNING, PolaronSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.convert2CharString(buffer));
				this.isDataMissmatchWarningWritten = true;
				this.dataCheckSumOffset = buffer_check_sum - check_sum;
				isOK = true;
			}
		}
		return isOK;
	}
}
