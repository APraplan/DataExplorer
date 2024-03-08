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
package gde.device.skyrc;

import gde.comm.DeviceCommPort;
import gde.comm.IDeviceCommPort;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.Checksum;
import gde.utils.StringHelper;

import java.io.IOException;
import java.util.logging.Logger;

import javax.usb.UsbInterface;

/**
 * Q200 universal serial port implementation
 * @author Winfried Bruegmann
 */
public class D100UsbPort extends DeviceCommPort implements IDeviceCommPort {
	final static String $CLASS_NAME = D100UsbPort.class.getName();
	final static Logger	log	= Logger.getLogger($CLASS_NAME);
		
  // The vendor ID SKYRC products are actually not registered.
  protected static final short VENDOR_ID = 0x0000;
  // The product ID SKYRC Q200 
  protected static final short PRODUCT_ID = 0x0001;
  // The SKYRC Q200 interface address
  protected static final byte INTERFACE_ID = 0x01;
  // The SKYRC Q200 input end point
  protected static final byte IN_ENDPOINT = (byte) 0x81;
  // The SKYRC Q200 output end point
  protected static final byte OUT_ENDPOINT = 0x01;

  // The communication timeout in milliseconds. */
  protected static final int TIMEOUT = 1000;
  
  protected final byte interfaceId;
  protected final byte endpointIn;
  protected final byte endpointOut;

//	public enum StartProcessing {
//		CHANNEL_A(new byte[]{0x0f, 0x03, 0x5F, 0x01, 0x60}), 
//		CHANNEL_B(new byte[]{0x0f, 0x03, 0x5F, 0x02, 0x61});
//		private byte[]	value;
//
//		private StartProcessing(byte[] v) {
//			this.value = v;
//		}
//
//		public byte[] value() {
//			return this.value;
//		}
//
//		public void setValue(final byte[] newValue) {
//			this.value = newValue;
//		}
//	};
//
//	public enum StopProcessing {
//		CHANNEL_A(new byte[]{0x0f, 0x03, (byte) 0xFE, 0x01, (byte) 0xFF}), 
//		CHANNEL_B(new byte[]{0x0f, 0x03, (byte) 0xFE, 0x02, (byte) 0x01});
//		private byte[]	value;
//
//		private StopProcessing(byte[] v) {
//			this.value = v;
//		}
//
//		public byte[] value() {
//			return this.value;
//		}
//
//		public void setValue(final byte[] newValue) {
//			this.value = newValue;
//		}
//	};
//
//	public enum ModifySystemSetting {
//		CHANNEL_A(new byte[]{0x0f, 0x03, (byte) 0x11, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00}), 
//		CHANNEL_B(new byte[]{0x0f, 0x03, (byte) 0x11, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00}); 
//		private byte[]	value;
//
//		private ModifySystemSetting(byte[] v) {
//			this.value = v;
//		}
//
//		public byte[] value() {
//			return this.value;
//		}
//
//		public void setValue(final byte[] newValue) {
//			this.value = newValue;
//		}
//	};

	public enum QuerySystemSetting {
		CHANNEL_A(new byte[]{0x0f, 0x04, 0x5A, 0x00, 0x00, 0x5A, (byte) 0xFF, (byte) 0xFF}),
		CHANNEL_B(new byte[]{0x0f, 0x04, 0x5A, 0x00, 0x01, 0x5B, (byte) 0xFF, (byte) 0xFF});
		private byte[]	value;

		private QuerySystemSetting(byte[] v) {
			this.value = v;
		}

		public byte[] value() {
			return this.value;
		}

		public void setValue(final byte[] newValue) {
			this.value = newValue;
		}
	};

	public enum QueryChannelData {
		CHANNEL_A(new byte[]{0x0f, 0x04, 0x5f, 0x00, 0x00, 0x5F, (byte) 0xFF, (byte) 0xFF}), 
		CHANNEL_B(new byte[]{0x0f, 0x04, 0x5f, 0x00, 0x01, 0x60, (byte) 0xFF, (byte) 0xFF});
		private byte[]	value;

		private QueryChannelData(byte[] v) {
			this.value = v;
		}

		public byte[] value() {
			return this.value;
		}

		public void setValue(final byte[] newValue) {
			this.value = newValue;
		}
	};

	public enum QueryOperationData {
		CHANNEL_A(new byte[]{0x0f, 0x04, 0x55, 0x00, 0x00, 0x55, (byte) 0xFF, (byte) 0xFF}),
		CHANNEL_B(new byte[]{0x0f, 0x04, 0x55, 0x00, 0x01, 0x56, (byte) 0xFF, (byte) 0xFF});
		private byte[]	value;

		private QueryOperationData(byte[] v) {
			this.value = v;
		}

		public byte[] value() {
			return this.value;
		}

		public void setValue(final byte[] newValue) {
			this.value = newValue;
		}
	};

	public enum QuerySystemInfo {
		CHANNEL_A(new byte[]{0x0f, 0x04, 0x57, 0x00, 0x00, 0x57, (byte) 0xFF, (byte) 0xFF}),
		CHANNEL_B(new byte[]{0x0f, 0x04, 0x57, 0x00, 0x01, 0x58, (byte) 0xFF, (byte) 0xFF});
		private byte[]	value;

		private QuerySystemInfo(byte[] v) {
			this.value = v;
		}

		public byte[] value() {
			return this.value;
		}

		public void setValue(final byte[] newValue) {
			this.value = newValue;
		}
	};
		
	final int  dataSize;
	int retrys = 1; // re-try temporary error counter
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public D100UsbPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
		this.dataSize = this.deviceConfig.getDataBlockSize(InputTypes.SERIAL_IO);
		this.interfaceId = this.device.getUsbInterface();
		this.endpointIn = this.device.getUsbEndpointIn();
		this.endpointOut = this.device.getUsbEndpointOut();
		
		//make sure commands have enough bytes to fill endpoint buffer
		byte[] tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(QueryOperationData.CHANNEL_A.value(), 0, tmpData, 0, QueryOperationData.CHANNEL_A.value().length);
		QueryOperationData.CHANNEL_A.setValue(tmpData);
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(QueryOperationData.CHANNEL_B.value(), 0, tmpData, 0, QueryOperationData.CHANNEL_B.value().length);
		QueryOperationData.CHANNEL_B.setValue(tmpData);
		
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(QueryChannelData.CHANNEL_A.value(), 0, tmpData, 0, QueryChannelData.CHANNEL_A.value().length);
		QueryChannelData.CHANNEL_A.setValue(tmpData);
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(QueryChannelData.CHANNEL_B.value(), 0, tmpData, 0, QueryChannelData.CHANNEL_B.value().length);
		QueryChannelData.CHANNEL_B.setValue(tmpData);
		
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(QuerySystemInfo.CHANNEL_A.value(), 0, tmpData, 0, QuerySystemInfo.CHANNEL_A.value().length);
		QuerySystemInfo.CHANNEL_A.setValue(tmpData);
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(QuerySystemInfo.CHANNEL_B.value(), 0, tmpData, 0, QuerySystemInfo.CHANNEL_B.value().length);
		QuerySystemInfo.CHANNEL_B.setValue(tmpData);
		
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(QuerySystemSetting.CHANNEL_A.value(), 0, tmpData, 0, QuerySystemSetting.CHANNEL_A.value().length);
		QuerySystemSetting.CHANNEL_A.setValue(tmpData);
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(QuerySystemSetting.CHANNEL_B.value(), 0, tmpData, 0, QuerySystemSetting.CHANNEL_B.value().length);
		QuerySystemSetting.CHANNEL_B.setValue(tmpData);
	}
	
	/**
	 * query the actual system info
	 * @param usbInterface
	 * @param systemInfoCmd
	 * @return
	 * @throws Exception
	 */
	public synchronized byte[] getSystemInfo(final UsbInterface usbInterface, final byte[] systemInfoCmd) throws Exception {
		final String $METHOD_NAME = "getSystemInfo"; //$NON-NLS-1$
		byte[] data = new byte[Math.abs(this.dataSize)];
		UsbInterface iface = null;
		boolean isPortOpenedByCall = false;

		try {
			if (usbInterface == null) {
				iface = this.openUsbPort(this.device);
				isPortOpenedByCall = true;
			}
			iface = usbInterface == null ? this.openUsbPort(this.device) : usbInterface;
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(systemInfoCmd, systemInfoCmd.length));
			//if (log.isLoggable(Level.OFF)) log.logp(Level.OFF, $CLASS_NAME, $METHOD_NAME, String.format("Checksum = 0x%02X", systemInfoCmd[systemInfoCmd[1]+1]));
			this.write(iface, this.endpointIn, systemInfoCmd);					
			try {
				Thread.sleep(10);
			}
			catch (Exception e) {
				// ignore
			}
			this.read(iface, this.endpointOut, data);
			
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
			
			if (!this.isChecksumOK(data) && this.retrys-- <= 0) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, String.format("Error: Checksum = 0x%02X != 0x%02X", calculateCheckSum(data, data[1]-1), data[data[1]+2]));
				return this.getSystemInfo(iface, systemInfoCmd);
			}
		}
		catch (Exception e) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
		finally {
				if (isPortOpenedByCall) this.closeUsbPort(iface);
		}
		this.retrys = 1;
		return data;
	}
	
	/**
	 * query the actual system info
	 * @param usbInterface
	 * @param systemSettingCmd
	 * @return
	 * @throws Exception
	 */
	public synchronized byte[] getSystemSetting(final UsbInterface usbInterface, final byte[] systemSettingCmd) throws Exception {
		final String $METHOD_NAME = "getSystemSetting"; //$NON-NLS-1$
		byte[] data = new byte[Math.abs(this.dataSize)];
		UsbInterface iface = null;
		boolean isPortOpenedByCall = false;

		try {
			if (usbInterface == null) {
				iface = this.openUsbPort(this.device);
				isPortOpenedByCall = true;
			}
			iface = usbInterface == null ? this.openUsbPort(this.device) : usbInterface;
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(systemSettingCmd, systemSettingCmd.length));
			//if (log.isLoggable(Level.OFF)) log.logp(Level.OFF, $CLASS_NAME, $METHOD_NAME, String.format("Checksum = 0x%02X", systemSettingCmd[systemSettingCmd[1]+1]));
			this.write(iface, this.endpointIn, systemSettingCmd);					
			try {
				Thread.sleep(10);
			}
			catch (Exception e) {
				// ignore
			}
			this.read(iface, this.endpointOut, data);
			
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
			
			if (!this.isChecksumOK(data) && this.retrys-- <= 0) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, String.format("Error: Checksum = 0x%02X != 0x%02X", calculateCheckSum(data, data[1]-1), data[data[1]+2]));
				return this.getSystemInfo(iface, systemSettingCmd);
			}
		}
		catch (Exception e) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
		finally {
				if (isPortOpenedByCall) this.closeUsbPort(iface);
		}
		this.retrys = 1;
		return data;	
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData(final UsbInterface iface, final byte[] request) throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
		byte[] data = new byte[Math.abs(this.dataSize)];

		try {
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(request, request.length));
			this.write(iface, this.endpointIn, request);			
			try {
				Thread.sleep(10);
			}
			catch (Exception e) {
				// ignore
			}
			this.read(iface, this.endpointOut, data);
			
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
			//log.logp(Level.OFF, $CLASS_NAME, $METHOD_NAME, "Length = " + data[1]);
			
			if (!this.isChecksumOK(data) && this.retrys-- <= 0) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, String.format("Error: Checksum = 0x%02X == 0x%02X", calculateCheckSum(data, data[1]-1), data[data[1]+2]));
				return this.getData(iface, request);
			}
		}
		catch (Exception e) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
		this.retrys = 1;
		return data;
	}

	public static byte calculateCheckSum(byte[] buffer, final int length) {
		return (byte) (Checksum.ADD(buffer, 2, length)%256);
	}

	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private boolean isChecksumOK(final byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK"; //$NON-NLS-1$
		final byte chkSum = calculateCheckSum(buffer, buffer[1]);
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME,String.format("Checksum = 0x%02X == 0x%02X", chkSum, buffer[buffer[1]+1])); //$NON-NLS-1$
		return chkSum == buffer[buffer[1]+1];
	}
}
