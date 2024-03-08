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
package gde.device.bantam;

import gde.comm.DeviceCommPort;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.Checksum;
import gde.utils.StringHelper;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * eStation serial port implementation
 * @author Winfried Brügmann
 */
public class EStationSerialPort extends DeviceCommPort {
	final static String $CLASS_NAME = EStationSerialPort.class.getName();
	final static Logger	log	= Logger.getLogger($CLASS_NAME);
	
	boolean isInSync = false;
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public EStationSerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData";
		byte[] data = new byte[Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO))];
		byte[] answer = new byte[] {0x00};

		try {
			answer = new byte[data.length];
			answer = this.read(data, 5000);
			// synchronize received data to DeviceSerialPortImpl.FF of sent data 
			while (answer.length > 0 && answer[0] != 0x7b) {
				this.isInSync = false;
				for (int i = 1; i < answer.length; i++) {
					if (answer[i] == 0x7b) {
						System.arraycopy(answer, i, data, 0, data.length - i);
						answer = new byte[i];
						answer = this.read(answer, 3000);
						System.arraycopy(answer, 0, data, data.length - i, i);
						this.isInSync = true;
						if (log.isLoggable(Level.FINE)) log.logp(java.util.logging.Level.FINE, EStationSerialPort.$CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				if (this.isInSync) break;
				answer = new byte[data.length];
				answer = this.read(answer, 3000);
			}
			StringBuilder sb;
			if (log.isLoggable(Level.FINE)) {
				sb = new StringBuilder();
				for (byte b : data) {
					sb.append(String.format("0x%02x, ", b)); //$NON-NLS-1$
				}
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}
			
			if (data.length > 0 && !isChecksumOK(data)) {
				this.addXferError();
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "=====> checksum error occured, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				data = getData();
			}
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(data, (byte) 0x80, 1, data.length-4));
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(Level.SEVERE, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		return data;
	}

	/**
	 * check ckeck sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private boolean isChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK";
		boolean isOK = false;
		switch (buffer.length) {
		default:
		case 76:
			int check_sum = Checksum.ADD(buffer, 1, 72);
			if (((check_sum & 0xF0) >> 4) + 0x30 == (buffer[73]&0xFF+0x80) && (check_sum & 0x00F) + 0x30 == (buffer[74]&0xFF))
				isOK = true;
			break;
		case 112:
			check_sum = Checksum.ADD(buffer, 1, buffer.length-4);
			if (((check_sum & 0xF0) >> 4) + 0x30 == (buffer[buffer.length-3]&0xFF+0x80) && (check_sum & 0x00F) + 0x30 == (buffer[buffer.length-2]&0xFF))
				isOK = true;
			break;
		}
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "Check_sum = " + isOK); //$NON-NLS-1$
		return isOK;
	}

}
