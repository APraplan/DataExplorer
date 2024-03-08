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
package gde.device.tttronix;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.comm.IDeviceCommPort;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.Checksum;
import gde.utils.StringHelper;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * QC-Copter or QuadroConrtol serial port implementation
 * @author Winfried Brügmann
 */
public class QcCopterSerialPort extends DeviceCommPort implements IDeviceCommPort {
	final static String $CLASS_NAME = QcCopterSerialPort.class.getName();
	final static Logger	log	= Logger.getLogger($CLASS_NAME);
	
	boolean isInSync = false;
	
	final public static byte STX = 0x02;
	final public static byte ETX = 0x03;
	final public static byte FF = '\f';
	final int  dataSize;
	final int  terminalDataSize = 345; // configuration menu string
	int retrys = 0; // re-try temporary error counter
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public QcCopterSerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
		this.dataSize = this.deviceConfig.getDataBlockSize(InputTypes.SERIAL_IO);
	}
	
	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
		byte[] data = new byte[this.dataSize];

		try {
			data = this.read(data, 1000);
			
			//check data for valid content
			if (containsSTX(data)) {
				//synchronize received data to begin of sent data 
				while (data[0] != STX) { 
					this.isInSync = false;
					for (int i = 1; i < data.length; i++) {
						if(data[i] == STX){
							System.arraycopy(data, i, data, 0, this.dataSize-i);
							byte []tmpdata = new byte[i];
							tmpdata = this.read(tmpdata, 1000);
							System.arraycopy(tmpdata, 0, data, this.dataSize-i, i);
							this.isInSync = true;
							log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
							break; //sync
						}
					}
					if(this.isInSync)
						break;
				}

				if (log.isLoggable(Level.FINER)) {
					StringBuilder sb = new StringBuilder();
					sb.append(StringHelper.byte2Hex2CharString(data, data.length));
					log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, sb.toString());
				}
				
				if (data.length != this.dataSize || !isChecksumOK(data)) {
					this.addXferError();
					this.retrys++;
					log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "=====> checksum error occured, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
					if (this.retrys > 3) { //retry or skip in case of xfer errors
						this.retrys = 0;
						throw new SerialPortException(Messages.getString(MessageIds.GDE_MSGT1904));
					}
					data = getData();
				}
				this.retrys = 0;
			}
			else { // only flight simulation data contains STX, so this must be configuration menu string
				throw new TimeOutException("received no flight simulation data!");
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
	 * check if received data contains STX
	 * @return true if one byte of data represent STX else false
	 */
	boolean containsSTX(byte[] data) {
		boolean isContained = false;
		for (int i = 0; i < data.length; i++) {
			if (data[i] == STX) {
				isContained = true;
				break;
			}
		}
		return isContained;
	}
	
	/**
	 * check if received data contains form feed 
	 * @return true if one byte of data represent FF else false
	 */
	boolean containsFF(byte[] data) {
		boolean isContained = false;
		for (int i = 0; i < data.length; i++) {
			if (data[i] == FF) {
				isContained = true;
				break;
			}
		}
		return isContained;
	}

	/**
	 * receive terminal data using stable counter which sends data back to gatherer thread while dialog is open
	 * @return String containing gathered data 
	 * @throws IOException
	 */
	public synchronized String getTerminalData() throws Exception {
		final String $METHOD_NAME = "getTerminalData"; //$NON-NLS-1$
		int timeout_ms = 1000;
		byte[] data = null;

		try {
			wait4Bytes(timeout_ms);
			data = this.read(new byte[this.getAvailableBytes()], timeout_ms);

			if (log.isLoggable(Level.FINER)) {
				StringBuilder sb = new StringBuilder();
				for (byte b : data) {
					sb.append(String.format("%c", (char)b)); //$NON-NLS-1$
				}
				log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}
		}
		catch (Exception e) {
			log.logp(Level.SEVERE, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			throw e;
		}
		return (data != null && data.length > 0) ? new String(data) : GDE.STRING_EMPTY;
	}

	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private boolean isChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK"; //$NON-NLS-1$
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, (Checksum.ADD(buffer, 1, 57)) + "; " + ( (((buffer[58]&0xFF) - 94) << 6) | (((buffer[59]&0xFF) - 94) & 0x3F) ) ); //$NON-NLS-1$
		return Checksum.ADD(buffer, 1, 57) == ((((buffer[58]&0xFF) - 94) << 6) | (((buffer[59]&0xFF) - 94) & 0x3F));
	}
}
