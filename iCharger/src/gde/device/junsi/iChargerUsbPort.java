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
package gde.device.junsi;

import gde.comm.DeviceCommPort;
import gde.comm.IDeviceCommPort;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.io.DataParser;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

import java.io.IOException;
import java.util.logging.Logger;

import javax.usb.UsbClaimException;
import javax.usb.UsbException;
import javax.usb.UsbInterface;

import org.usb4java.DeviceHandle;

/**
 * @author Winfried Brügmann
 */
public class iChargerUsbPort extends DeviceCommPort implements IDeviceCommPort {
	final static String				$CLASS_NAME	= iChargerUsbPort.class.getName();
	final static Logger				log					= Logger.getLogger($CLASS_NAME);

	// The communication timeout in milliseconds, 500ms is the smallest interval
	public final static long	TIMEOUT_MS	= 1200;
	protected long						timeout_ms	= 1200;

	protected DeviceHandle		libUsbHandle;

	protected final byte			interfaceId;
	protected final byte			endpointIn;
	protected final byte			endpointOut;

	final int									dataSize;
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public iChargerUsbPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
		this.dataSize = this.deviceConfig.getDataBlockSize(InputTypes.SERIAL_IO);
		this.interfaceId = this.device.getUsbInterface();
		this.endpointIn = this.device.getUsbEndpointIn();
		this.endpointOut = this.device.getUsbEndpointOut();
	}

	public void openUsbPort() throws UsbClaimException, UsbException {
		this.libUsbHandle = this.openLibUsbPort(this.device);
	}

	public void closeUsbPort(boolean cacheSelectedUsbDevice) throws UsbClaimException, UsbException {
		if (this.libUsbHandle != null) {
			this.closeLibUsbPort(this.libUsbHandle, cacheSelectedUsbDevice);
			this.libUsbHandle = null;
		}
		else this.closeLibUsbPort(null, false);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData(final UsbInterface iface) throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
		byte[] data = new byte[Math.abs(this.dataSize)];

		try {
			this.read(iface, this.endpointOut, data, (int)timeout_ms);
			
			if (log.isLoggable(Level.FINE)) {
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, DataParser.parse2Int(data, 3)/1000/60 + " min " + (DataParser.parse2Int(data, 3)/1000)%60 + " sec run time");
			}
		}
		catch (Exception e) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
				if (e instanceof RuntimeException) throw e;
		}
		return data;
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @param libUsbHandle the valid device handle to communicate through USB-HID 
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
		byte[] data = new byte[Math.abs(this.dataSize)];

		this.read(this.libUsbHandle, this.endpointOut, data, timeout_ms);
		
		if (log.isLoggable(Level.FINE)) {
			log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
			log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, DataParser.getUInt32(data, 3)/1000/60 + " min " + (DataParser.getUInt32(data, 3)/1000)%60 + " sec run time");
		}
		return data;
	}
	
	public long getTimeOut_ms() {
		return this.timeout_ms;
	}
	
	public void setTimeOut_ms(long newTimeout_ms) {
		log.finer(() -> String.format("set new timeout_ms = %d", newTimeout_ms));
		this.timeout_ms = newTimeout_ms;
	}
}