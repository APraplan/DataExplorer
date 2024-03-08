/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.comm;

import java.io.IOException;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.usb.UsbClaimException;
import javax.usb.UsbDevice;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotClaimedException;

import org.usb4java.DeviceHandle;

import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.exception.FailedQueryException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.ui.DataExplorer;

/**
 * @author brueg
 *
 */
public class DeviceCommPort implements IDeviceCommPort {
	final static String 				$CLASS_NAME 			= DeviceCommPort.class.getName();

	final protected Settings							settings;
	final protected IDevice								device;
	final protected DeviceConfiguration		deviceConfig;
	final protected DataExplorer					application;
	final protected IDeviceCommPort				port;

	public static final int ICON_SET_OPEN_CLOSE = 0;
	public static final int ICON_SET_START_STOP = 1;
	public static final int ICON_SET_IMPORT_CLOSE = 2;

	public final static byte							FF												= 0x0C;
	public final static byte							CR												= 0x0D;
	public final static byte							ACK												= 0x06;
	public final static byte							NAK												= 0x15;
	public static final String						STRING_NAK								= "<NAK>";
	public static final String						STRING_ACK								= "<ACK>";
	public static final String						STRING_CR									= "<CR>";
	public static final String						STRING_FF									= "<FF>";
	public static final String						FORMAT_2_CHAR							= "%c%c";																																												//2 char to string formating
	public static final String						FORMAT_4_CHAR							= "%c%c%c%c";																																										//4 char to string formating
	public static final String						FORMAT_16_CHAR						= "%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c";
	public static final String[]					STRING_ARRAY_BAUDE_RATES	= new String[] { "2400", "4800", "7200", "9600", "14400", "19200", "28800", "38400", "57600", "115200" , "128000", "230400"};	//$NON-NLS-1$
	
	public boolean				isInterruptedByUser	= false;	

	static IDeviceCommPort staticPort = null;

	final protected static TreeMap<String, String>	availablePorts						= new TreeMap<String, String>();																																					//available port vector used by all application dialogs
	final protected static TreeMap<Integer, String>	windowsPorts							= new TreeMap<Integer, String>();
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public DeviceCommPort(IDevice currentDevice, DataExplorer currentApplication) {
		this.device = currentDevice;
		this.deviceConfig = currentDevice.getDeviceConfiguration();
		this.application = currentApplication;
		this.settings = Settings.getInstance();
		if (Boolean.parseBoolean(System.getProperty("GDE_IS_SIMULATION"))) {
			this.port = new DeviceSerialPortSimulatorImpl(this.device, this.application, this.device.getTimeStep_ms() > 0, (this.device.getTimeStep_ms() < 0 
						? (System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC") != null ? Integer.parseInt(System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC")) : 100) 
						: (System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC") != null ? Integer.parseInt(System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC")) : (int)this.device.getTimeStep_ms())));
		}
		else  if (this.deviceConfig.getSerialPortType() != null) {
			if (this.settings.isRXTXcommToBeUsed()) //RXTXcomm
				this.port = new DeviceSerialPortImpl(this.deviceConfig, this.application);
			else //jSerialCommPort: 
				this.port = new DeviceJavaSerialCommPortImpl(this.deviceConfig, this.application);
		}
		else if (this.deviceConfig.getUsbPortType() != null) { // USB device
			this.port = new DeviceUsbPortImpl(this.deviceConfig, this.application);
		}
		else this.port = null;
		
		DeviceCommPort.staticPort = this.port;
	}
	
	/**
	 * constructor to enable serial port tests without complete device construction
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public DeviceCommPort(DeviceConfiguration deviceConfiguration) {
		this.device = null;
		this.deviceConfig = deviceConfiguration;
		this.application = null;
		this.settings = Settings.getInstance();
		if (Boolean.parseBoolean(System.getProperty("GDE_IS_SIMULATION"))) {
			this.port = new DeviceSerialPortSimulatorImpl(null, this.application, this.deviceConfig.getTimeStep_ms() > 0, (this.deviceConfig.getTimeStep_ms() < 0 
						? (System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC") != null ? Integer.parseInt(System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC")) : 100) 
						: (System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC") != null ? Integer.parseInt(System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC")) : (int)this.deviceConfig.getTimeStep_ms())));
		}
		else  if (this.deviceConfig.getSerialPortType() != null) {
			//RXTXcomm usage: this.port = new DeviceSerialPortImpl(this.deviceConfig, this.application);
			//jSerialCommPort: 
			this.port = new DeviceJavaSerialCommPortImpl(this.deviceConfig, this.application);
		}
		else if (this.deviceConfig.getUsbPortType() != null) { // USB device
			this.port = new DeviceUsbPortImpl(this.deviceConfig, this.application);
		}
		else this.port = null;

		DeviceCommPort.staticPort = this.port;
	}

	/**
	 * updates the given vector with actual available according black/white list configuration
	 * @param doAvialabilityCheck
	 * @param portBlackList
	 * @param portWhiteList
	 */
	public static TreeMap<String, String> listConfiguredSerialPorts(final boolean doAvialabilityCheck, final String portBlackList, final Vector<String> portWhiteList) {
		if (DeviceCommPort.staticPort != null && DeviceCommPort.staticPort instanceof DeviceSerialPortImpl) //RXTXcomm
			return DeviceSerialPortImpl.listConfiguredSerialPorts(doAvialabilityCheck, portBlackList, portWhiteList);
		else if (DeviceCommPort.staticPort != null && DeviceCommPort.staticPort instanceof DeviceJavaSerialCommPortImpl) //JSerialCommPort: 
			return DeviceJavaSerialCommPortImpl.listConfiguredSerialPorts(doAvialabilityCheck, portBlackList, portWhiteList);
		else {
			DeviceCommPort.availablePorts.clear();
			return DeviceCommPort.availablePorts;
		}
	}


	/**
	 * @return available serial port list
	 */
	public static String[] prepareSerialPortList() {
		if (DeviceCommPort.staticPort != null && DeviceCommPort.staticPort instanceof DeviceSerialPortImpl) //RXTXcomm
			return DeviceSerialPortImpl.prepareSerialPortList();
		else if (DeviceCommPort.staticPort != null && DeviceCommPort.staticPort instanceof DeviceJavaSerialCommPortImpl) //JSerialCommPort: 
			return DeviceJavaSerialCommPortImpl.prepareSerialPortList();
		else
			return new String[0];
	}
	
	/**
	 * @return the available serial port names
	 */
	public static TreeMap<String, String> getAvailableports() {
		return DeviceCommPort.availablePorts;
	}

	/**
	 * @return the windows ports
	 */
	public static TreeMap<Integer, String> getWindowsPorts() {
		return DeviceCommPort.windowsPorts;
	}


	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#open()
	 */
	public Object open() throws ApplicationConfigurationException, SerialPortException {
		return this.port.open();
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#close()
	 */
	public void close() {
		if (this.port != null)
			this.port.close();	
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#read(byte[], int)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec) throws IOException, TimeOutException {
		return this.port.read(readBuffer, timeout_msec);
	}

	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * @param readBuffer
	 * @param timeout_msec
	 * @param checkFailedQuery
	 * @return the red byte array
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized byte[] read(byte[] readBuffer, int timeout_msec, boolean checkFailedQuery) throws IOException, FailedQueryException, TimeOutException {
		return this.port.read(readBuffer, timeout_msec, checkFailedQuery);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#read(byte[], int, int)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex) throws IOException, TimeOutException {
		return this.port.read(readBuffer, timeout_msec, stableIndex);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#read(byte[], int, int)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex, int minCountBytes) throws IOException, TimeOutException {
		return this.port.read(readBuffer, timeout_msec, stableIndex, minCountBytes);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#read(byte[], int, java.util.Vector)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, Vector<Long> waitTimes) throws IOException, TimeOutException {
		return this.port.read(readBuffer, timeout_msec, waitTimes);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#write(byte[])
	 */
	public void write(byte[] writeBuffer) throws IOException {
		this.port.write(writeBuffer);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#write(byte[])
	 */
	public void write(byte[] writeBuffer, long gap_ms) throws IOException {
		this.port.write(writeBuffer, gap_ms);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#write(byte[])
	 */
	public int cleanInputStream() throws IOException {
		return this.port.cleanInputStream();
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#wait4Bytes(int)
	 */
	public long wait4Bytes(int timeout_msec) throws InterruptedException, TimeOutException, IOException {
		return this.port.wait4Bytes(timeout_msec);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#wait4Bytes(int, int)
	 */
	public int wait4Bytes(int numBytes, int timeout_msec) throws IOException {
		return this.port.wait4Bytes(numBytes, timeout_msec);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#waitForStableReceiveBuffer(int, int, int)
	 */
	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex) throws InterruptedException, TimeOutException, IOException {
		return this.port.waitForStableReceiveBuffer(expectedBytes, timeout_msec, stableIndex);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#waitForStableReceiveBuffer(int, int, int)
	 */
	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex, int minCount) throws InterruptedException, TimeOutException, IOException {
		return this.port.waitForStableReceiveBuffer(expectedBytes, timeout_msec, stableIndex, minCount);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#isConnected()
	 */
	public boolean isConnected() {
		return this.port != null ? this.port.isConnected() : false;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#getXferErrors()
	 */
	public int getXferErrors() {
		return this.port.getXferErrors();
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#addXferError()
	 */
	public void addXferError() {
		this.port.addXferError();
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#getXferErrors()
	 */
	public int getTimeoutErrors() {
		return this.port.getTimeoutErrors();
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#addXferError()
	 */
	public void addTimeoutError() {
		this.port.addTimeoutError();
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#isMatchAvailablePorts(java.lang.String)
	 */
	public boolean isMatchAvailablePorts(String newSerialPortStr) {
		return this.port.isMatchAvailablePorts(newSerialPortStr);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#waitForStableReceiveBuffer(int, int, int)
	 */
	public int getAvailableBytes() throws IOException {
		return this.port.getAvailableBytes();
	}

	/**
	 * @param setInterruptedByUser the isInterruptedByUser to set
	 */
	public synchronized void setInterruptedByUser(boolean setInterruptedByUser) {
		this.isInterruptedByUser = setInterruptedByUser;
	}
	
	/////// USB interface starts here
  /**
   * find USB devices identified by vendor ID and product ID
   * @param vendorId
   * @param productId
   * @return
   * @throws UsbException
   */
	public Set<UsbDevice> findUsbDevices(final short vendorId, final short productId) throws UsbException {
		return this.port.findUsbDevices(vendorId, productId);
	}

	/**
	 * find USB devices starting from hub (root hub)
	 * @param hub
	 * @param vendorId
	 * @param productId
	 * @return
	 */
	public Set<UsbDevice> findDevices(UsbHub hub, short vendorId, short productId) {
		return this.port.findDevices(hub, vendorId, productId);
	}

	/**
	 * dump required information for a USB device with known product ID and
	 * vendor ID
	 * @param vendorId
	 * @param productId
	 * @throws UsbException
	 */
	public void dumpUsbDevices(final short vendorId, final short productId) throws UsbException {
		this.port.dumpUsbDevices(vendorId, productId);
	}
	
	/**
	 * claim USB interface with given number which correlates to open a USB port
	 * @param IDevice the actual device in use
	 * @return
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public UsbInterface openUsbPort(final IDevice activeDevice) throws UsbClaimException, UsbException {
		return this.port.openUsbPort(activeDevice);
	}
	
	/**
	 * claim USB interface with given number which correlates to open a USB port
	 * @param IDevice the actual device in use
	 * @return
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public DeviceHandle openLibUsbPort(final IDevice activeDevice) throws UsbClaimException, UsbException {
		return this.port.openLibUsbPort(activeDevice);
	}

	/**
	 * release or close the given interface
	 * @param usbInterface
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public void closeUsbPort(final UsbInterface usbInterface) throws UsbClaimException, UsbException {
		this.port.closeUsbPort(usbInterface);
	}

	/**
	 * release or close the given interface
	 * @param libUsbHandle
	 * @param cacheSelectedUsbDevice true| false
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public void closeLibUsbPort(final DeviceHandle libUsbHandle, boolean cacheSelectedUsbDevice) throws UsbClaimException, UsbException {
		this.port.closeLibUsbPort(libUsbHandle, cacheSelectedUsbDevice);
	}
	
	/**
	 * write a byte array of data using the given interface and its end point address
	 * @param iface
	 * @param endpointAddress
	 * @param data
	 * @return number of bytes sent
	 * @throws UsbNotActiveException
	 * @throws UsbNotClaimedException
	 * @throws UsbDisconnectedException
	 * @throws UsbException
	 */
	public int write(final UsbInterface iface, final byte endpointAddress, final byte[] data) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException {
		return this.port.write(iface, endpointAddress, data);
	}

	/**
	 * read a byte array of data using the given interface and its end point address
	 * @param iface
	 * @param endpointAddress
	 * @param data receive buffer
	 * @return number of bytes received
	 * @throws UsbNotActiveException
	 * @throws UsbNotClaimedException
	 * @throws UsbDisconnectedException
	 * @throws UsbException
	 */
	public int read(final UsbInterface iface, final byte endpointAddress, final byte[] data) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException {
		return this.port.read(iface, endpointAddress, data);
	}

	/**
	 * read a byte array of data using the given interface and its end point address
	 * @param iface
	 * @param endpointAddress
	 * @param data receive buffer
	 * @param timeout_msec
	 * @return number of bytes received
	 * @throws UsbNotActiveException
	 * @throws UsbNotClaimedException
	 * @throws UsbDisconnectedException
	 * @throws UsbException
	 */
	public int read(final UsbInterface iface, final byte endpointAddress, final byte[] data, final int timeout_msec) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException {
		return this.port.read(iface, endpointAddress, data, timeout_msec);
	}
	
  /**
   * Writes some data byte array to the device.
   * @param handle The device handle.
   * @param outEndpoint The end point address
   * @param data the byte array for data with length as size to be send 
   * @param timeout_ms the time out in milli seconds
   * @throws IllegalStateException while handle not initialized
   * @throws TimeOutException while data transmission failed
   */
  public void write(final DeviceHandle handle, final byte outEndpoint, final byte[] data, final long timeout_ms) throws IllegalStateException, TimeOutException {
  	this.port.write(handle, outEndpoint, data, timeout_ms);
  } 

  /**
   * Reads some data with length from the device
   * @param handle The device handle.
   * @param inEndpoint The end point address
   * @param data the byte array for data with length as size to be received 
   * @param timeout_ms the time out in milli seconds
   * @return The number of bytes red
   * @throws IllegalStateException while handle not initialized
   * @throws TimeOutException while data transmission failed
   */
  public int read(final DeviceHandle handle, final byte inEndpoint, final byte[] data, final long timeout_ms) throws IllegalStateException, TimeOutException {
  	return this.port.read(handle, inEndpoint, data, timeout_ms);
  }

}
