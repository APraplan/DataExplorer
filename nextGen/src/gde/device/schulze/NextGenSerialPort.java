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
package gde.device.schulze;

import java.io.IOException;
import java.util.logging.Logger;

import gde.comm.DeviceCommPort;
import gde.comm.IDeviceCommPort;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.Checksum;
import gde.utils.StringHelper;

/**
 * nextGeneration serial port implementation
 * @author Winfried Brügmann
 */
public class NextGenSerialPort extends DeviceCommPort implements IDeviceCommPort {
	final static String	$CLASS_NAME			= NextGenSerialPort.class.getName();
	final static Logger	log							= Logger.getLogger(NextGenSerialPort.$CLASS_NAME);

	final byte					startByte1 = '1';
	final byte					startByte2 = '2';
	final byte					startByteTrailer = ':';
	final byte					endByte;
	final byte[]				tmpByte					= new byte[1];
	final int						timeout;
	final int						stableIndex;
	final int						tmpDataLength;

	byte[]							answer;
	byte[]							tmpData;
	byte[]							data						= new byte[] { 0x00 };
	long								time						= 0;
	boolean							isDataReceived	= false;
	int									index						= 0;
	int									retryCounter		= 0;

	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public NextGenSerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
		this.tmpDataLength = Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO));
		this.endByte = (byte) this.device.getDataBlockSeparator().value().charAt(0);
		this.timeout = this.device.getDeviceConfiguration().getReadTimeOut();
		this.stableIndex = this.device.getDeviceConfiguration().getReadStableIndex();
		this.isDataReceived = false;
		this.index = 0;
		this.tmpData = new byte[0];
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData";
		int startIndex;
		this.index = 0;

		try {
			//receive data while needed
			this.isDataReceived = false;
			readNewData();
			log.log(Level.OFF, "'" + new String(answer) + "'");

			//find start index
			while (this.answer.length >= this.tmpDataLength && this.index < this.answer.length-1 && (this.answer[this.index] != this.startByte1 || this.answer[this.index] != this.startByte2) && this.answer[this.index+1] != this.startByteTrailer)
				++this.index;

			if (this.index < this.answer.length) {
				startIndex = this.index;
				++this.index;
				this.tmpData = new byte[0];
			}
			else { //startIndex not found, read new data
				this.isDataReceived = false;
				this.index = 0;
				return getData();
			}

			//find end index
			findDataEnd(startIndex);
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				NextGenSerialPort.log.logp(Level.SEVERE, NextGenSerialPort.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		this.data[0] = 0x32;
		log.log(Level.OFF, StringHelper.byte2Hex2CharString(this.data, this.data.length));
		log.log(Level.OFF, "'" + new String(this.data) + "'");
		return this.data;
	}

	/**
	 * recursive find the end of data, normal exit is not at the end of the method
	 * @param startIndex
	 * @throws IOException
	 * @throws TimeOutException
	 */
	protected byte[] findDataEnd(int startIndex) throws IOException, TimeOutException {
		final String $METHOD_NAME = "findDataEnd";
		int endIndex;
		
		//log.log(Level.INFO, StringHelper.byte2Hex2CharString(this.answer, this.answer.length));

		if (answer.length - startIndex >=  Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO))) {
			this.index =  Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO));
			while (this.index < this.answer.length && !(this.answer[this.index] == this.endByte))
				--this.index;
			++this.index;
		}
		else 
			this.index = startIndex + 24; //Ni 1: 3488:13614: 2007:L 41
		
		
		if (this.index < this.answer.length && (this.tmpData.length + this.index - startIndex) > 8) {
			endIndex = this.index;
			this.data = new byte[this.tmpData.length + endIndex - startIndex];
			//System.out.println(startIndex + " - " + this.tmpData.length + " - " + endIndex);
			System.arraycopy(this.tmpData, 0, this.data, 0, this.tmpData.length);
			System.arraycopy(this.answer, startIndex, this.data, this.tmpData.length, endIndex - startIndex);
			//log.log(Level.INFO, StringHelper.byte2Hex2CharString(this.data, this.data.length));

			if (NextGenSerialPort.log.isLoggable(Level.FINER)) {
				StringBuilder sb = new StringBuilder().append("'");
				for (byte b : this.data) {
					sb.append((char) b);
				}
				while (sb.length() > 5 && (sb.charAt(sb.length() - 1) == '\n' || sb.charAt(sb.length() - 1) == '\r'))
					sb.deleteCharAt(sb.length() - 1);
				sb.append("'");
				NextGenSerialPort.log.logp(Level.FINER, NextGenSerialPort.$CLASS_NAME, $METHOD_NAME, sb.toString());
			}
			return this.data;
		}
		//endIndex not found, save temporary data, read new data
		if (this.retryCounter > 10)
			throw new TimeOutException("no data received");
		
		++this.retryCounter;
		this.data = new byte[this.tmpData.length];
		System.arraycopy(this.tmpData, 0, this.data, 0, this.data.length);

		this.tmpData = new byte[this.answer.length - startIndex + this.data.length];
		System.arraycopy(this.data, 0, this.tmpData, 0, this.data.length);
		System.arraycopy(this.answer, startIndex, this.tmpData, this.data.length, this.answer.length - startIndex);

		this.isDataReceived = false;
		readNewData();
		findDataEnd(this.index = 0);
		
		this.retryCounter = 0; //reset retryCounter counter
		return this.data;
	}

	/**
	 * receive data only if needed, a receive buffer may hold more than one getData() result
	 * @throws IOException
	 * @throws TimeOutException
	 */
	protected void readNewData() throws IOException, TimeOutException {
		if (!this.isDataReceived) {
			this.answer = new byte[this.tmpDataLength];
			this.answer = this.read(this.answer, this.timeout, this.stableIndex);
			this.isDataReceived = true;
		}
	}

	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	protected boolean isChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK";
		boolean isOK = false;
		int check_sum = Checksum.XOR(buffer, buffer.length - 4);
		if (Integer.parseInt(String.format("%c%c", buffer[buffer.length - 4], buffer[buffer.length - 3])) == check_sum) isOK = true;
		if (NextGenSerialPort.log.isLoggable(Level.FINER)) NextGenSerialPort.log.logp(Level.FINER, NextGenSerialPort.$CLASS_NAME, $METHOD_NAME, "Check_sum = " + isOK); //$NON-NLS-1$
		return isOK;
	}

}
