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
    
    Copyright (c) 2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.comm.DeviceCommPort;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.ui.DataExplorer;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * JLog serial port implementation
 * @author Winfried Brügmann
 */
public class JLog2SerialPort extends DeviceCommPort {
	final static String $CLASS_NAME = JLog2SerialPort.class.getName();
	final static Logger	log	= Logger.getLogger($CLASS_NAME);
	
	boolean isInSync = false;
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public JLog2SerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
		byte[] data = new byte[] {0x00};
		byte[] tmpData = new byte[Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO))];
		byte[] answer = new byte[] {0x00};

		try {
			answer = new byte[tmpData.length];
			answer = this.read(answer, 1000, 5, tmpData.length/3);
			
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				for (byte b : answer) {
					sb.append((char)b);
				}
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}

			// synchronize received data to begin of sent data 
			while (answer[0] != '$') {
				if (this.isInterruptedByUser) return tmpData;
				//re-read data
				this.cleanInputStream();
				tmpData = getData();
			}
			//data in sync now
			System.arraycopy(answer, 0, tmpData, 0, answer.length);
			data = new byte[getArrayLengthByCheckEnding(tmpData)];
			System.arraycopy(tmpData, 0, data, 0, data.length);

			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				for (byte b : data) {
					sb.append((char)b);
				}
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}
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
	 * @param tmpData
	 * @return
	 */
	public int getArrayLengthByCheckEnding(byte[] tmpData) {
		//real answer might be shorter as the maximum of 150 bytes
		int lenght = tmpData.length;
		while (lenght > 10 && !(tmpData[lenght-1] == 0x0A && tmpData[lenght-2] == 0x0D))
			--lenght;
		return lenght;
	}
}
