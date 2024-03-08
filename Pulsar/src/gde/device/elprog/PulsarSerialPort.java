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
    
    Copyright (c) 2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.elprog;

import java.io.IOException;
import java.util.logging.Logger;

import gde.comm.DeviceCommPort;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.ui.DataExplorer;

/**
 * @author brueg
 *
 */
public class PulsarSerialPort extends DeviceCommPort {
	final static String	$CLASS_NAME			= PulsarSerialPort.class.getName();
	final static Logger	log							= Logger.getLogger(PulsarSerialPort.$CLASS_NAME);

	final byte					startByte;
	final byte					endByte;
	final byte					endByte_1;
	final byte[]				tmpByte					= new byte[1];
	final int						timeout;
	final int						stableIndex;
	final int						tmpDataLength;

	byte[]							answer;
	byte[]							tmpData;
	byte[]							data						= new byte[] { 0x00 };

	long								time						= 0;
	int									index						= 0;
	boolean							isEndByte_1			= false;

	/**
	 * @param currentDevice
	 * @param currentApplication
	 */
	public PulsarSerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
		this.startByte = (byte) this.device.getDataBlockLeader().charAt(0);
		this.endByte = this.device.getDataBlockEnding()[this.device.getDataBlockEnding().length - 1];
		this.endByte_1 = this.device.getDataBlockEnding().length == 2 ? this.device.getDataBlockEnding()[0] : 0x00;
		this.tmpDataLength = Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO));
		this.timeout = this.device.getDeviceConfiguration().getReadTimeOut();
		this.stableIndex = this.device.getDeviceConfiguration().getReadStableIndex();
		this.index = 0;
		this.tmpData = new byte[0];
	}

	/**
	 * @param deviceConfiguration
	 */
	public PulsarSerialPort(DeviceConfiguration deviceConfiguration) {
		super(deviceConfiguration);
		this.startByte = (byte) this.device.getDataBlockLeader().charAt(0);
		this.endByte = this.device.getDataBlockEnding()[this.device.getDataBlockEnding().length - 1];
		this.endByte_1 = this.device.getDataBlockEnding().length == 2 ? this.device.getDataBlockEnding()[0] : 0x00;
		this.tmpDataLength = Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO));
		this.timeout = this.device.getDeviceConfiguration().getReadTimeOut();
		this.stableIndex = this.device.getDeviceConfiguration().getReadStableIndex();
		this.index = 0;
		this.tmpData = new byte[0];
	}


	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
		this.index = 0;

		try {
			//receive data while needed
			this.answer = new byte[this.tmpDataLength];
			this.answer = this.read(this.answer, this.timeout, this.stableIndex);
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, new String(this.answer));
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(Level.SEVERE, PulsarSerialPort.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		return this.answer;
	}

	/**
	 * recursive find the end of data, normal exit is not at the end of the method
	 * @param startIndex
	 * @throws IOException
	 * @throws TimeOutException
	 */
	protected byte[] findDataEnd(int startIndex) throws IOException, TimeOutException {
		final String $METHOD_NAME = "findDataEnd"; //$NON-NLS-1$
		int endIndex = 0;
		while (this.index < this.answer.length && !((this.endByte_1 != 0x00 || this.answer[this.index - 1] == this.endByte_1 || this.isEndByte_1 == true) && this.answer[this.index] == this.endByte))
			++this.index;

		if (this.endByte_1 != 0x00 || this.answer[this.index - 1] == this.endByte_1) this.isEndByte_1 = true;

		if (this.index < this.answer.length && (this.tmpData.length + this.index - startIndex) > 8) {
			endIndex = this.index;
			this.isEndByte_1 = false;
			this.data = new byte[this.tmpData.length + endIndex - startIndex];
			//System.out.println(startIndex + " - " + this.tmpData.length + " - " + endIndex);
			System.arraycopy(this.tmpData, 0, this.data, 0, this.tmpData.length);
			System.arraycopy(this.answer, startIndex, this.data, this.tmpData.length, endIndex - startIndex);
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				for (byte b : this.data) {
					sb.append((char) b);
				}
				while (sb.length() > 5 && (sb.charAt(sb.length() - 1) == '\n' || sb.charAt(sb.length() - 1) == '\r'))
					sb.deleteCharAt(sb.length() - 1);
				if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, PulsarSerialPort.$CLASS_NAME, $METHOD_NAME, sb.toString());
			}
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "1" + new String(this.data));
			return this.data;
		}
		//endIndex not found, save temporary data, read new data
		this.data = new byte[this.tmpData.length];
		System.arraycopy(this.tmpData, 0, this.data, 0, this.data.length);

		this.tmpData = new byte[this.answer.length - startIndex + this.data.length];
		System.arraycopy(this.data, 0, this.tmpData, 0, this.data.length);
		System.arraycopy(this.answer, startIndex, this.tmpData, this.data.length, this.answer.length - startIndex);

		readNewData();
		findDataEnd(this.index = 0);

		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "2" + new String(this.data));
		return this.data;
	}

	/**
	 * receive data only if needed, a receive buffer may hold more than one getData() result
	 * @throws IOException
	 * @throws TimeOutException
	 */
	protected void readNewData() throws IOException, TimeOutException {
		this.answer = new byte[this.tmpDataLength];
		this.answer = this.read(this.answer, this.timeout, this.stableIndex);
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, new String(this.answer));
	}

	/**
	 * query the size of data starting at the end, requires an empty data buffer
	 * @return
	 */
	protected int getArrayLengthByCheckEnding() {
		//real answer might be shorter as the maximum of 150 bytes
		int lenght = this.tmpData.length;
		while (lenght > 0 && !((this.endByte_1 != 0x00 || this.tmpData[lenght - 2] == this.endByte_1) && this.tmpData[lenght - 1] == this.endByte))
			--lenght;
		if (log.isLoggable(Level.FINE))
			log.logp(Level.FINE, PulsarSerialPort.$CLASS_NAME, "getArrayLengthByCheckEnding", "array length = " + lenght); //$NON-NLS-1$ //$NON-NLS-2$
		return lenght;
	}

}
