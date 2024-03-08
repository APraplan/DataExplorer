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
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.comm;

import java.io.IOException;
import java.util.Set;
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

import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.exception.FailedQueryException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;

/**
 * interface to device serial port to enable overloading with different implementations
 */
public interface IDeviceCommPort {

	/**
	 * opens the serial port specified in device configuration or settings (global)
	 * @return reference to instance of serialPort
	 * @throws ApplicationConfigurationException
	 * @throws SerialPortException
	 */
	public Object open() throws ApplicationConfigurationException, SerialPortException;
	
	/**
	 * function to close the serial port
	 * this is done within a tread since the port can't close if it stays open for a long time period ??
	 */
	public void close();
	
	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * @param readBuffer
	 * @param timeout_msec
	 * @return the red byte array
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec) throws IOException, TimeOutException;

	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * @param readBuffer
	 * @param timeout_msec
	 * @param checkFailedQuery
	 * @return the red byte array
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, boolean checkFailedQuery) throws IOException, FailedQueryException, TimeOutException;

	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * if the readBuffer kan not be filled a stable counter will be active where a number of retries can be specified
	 * @param readBuffer with the size expected bytes
	 * @param timeout_msec
	 * @param stableIndex a number of cycles to treat as telegram transmission finished
	 * @return the reference of the given byte array, byte array meight be adapted to received size
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex) throws IOException, TimeOutException;

	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * if the readBuffer can not be filled a stable counter will be active where a number of retries can be specified
	 * @param readBuffer with the size expected bytes
	 * @param timeout_msec
	 * @param stableIndex a number of cycles to treat as telegram transmission finished
	 * @param minCountBytes minimum count of bytes to be received, even if stable
	 * @return the reference of the given byte array, byte array might be adapted to received size
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex, int minCountBytes) throws IOException, TimeOutException;

	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * the reference to the wait time vector will add the actual wait time to have the read buffer ready to read the given number of bytes
	 * @param readBuffer
	 * @param timeout_msec
	 * @param waitTimes
	 * @return the red byte array
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, Vector<Long> waitTimes) throws IOException, TimeOutException;


	/**
	 * write bytes to serial port output stream, cleans receive buffer if available byes prior to send data 
	 * @param writeBuffer writes size of writeBuffer to output stream
	 * @throws IOException
	 */
	public void write(byte[] writeBuffer) throws IOException;

	/**
	 * write bytes to serial port output stream, each byte individual with the given time gap in msec
	 * cleans receive buffer if available byes prior to send data 
	 * @param writeBuffer writes size of writeBuffer to output stream
	 * @throws IOException
	 */
	public void write(byte[] writeBuffer, long gap_ms) throws IOException;

	/**
	 * cleanup the input stream if there are bytes available
	 * @return number of bytes in receive buffer which get removed
	 * @throws IOException
	 */
	public int cleanInputStream() throws IOException;

	/**
	 * function check for available bytes on receive buffer
	 * @return System.currentTimeMillis() if data available within time out, else an exception
	 * @throws InterruptedException 
	 * @throws TimeOutException 
	 * @throws IOException 
	 */
	public long wait4Bytes(int timeout_msec) throws InterruptedException, TimeOutException, IOException;

	/**
	 * waits until receive buffer is filled with the number of expected bytes while checking inputStream
	 * @param numBytes
	 * @param timeout_msec
	 * @return number of bytes in receive buffer
	 * @throws TimeOutException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public int wait4Bytes(int numBytes, int timeout_msec) throws IOException;

	/**
	 * waits until receive buffer is filled with number of expected bytes or does not change anymore in stableIndex cycles * 10 msec
	 * @param expectedBytes
	 * @param timeout_msec in milli seconds, this is the maximum time this process will wait for stable byte count or maxBytes
	 * @param stableIndex cycle count times 10 msec to be treat as stable
	 * @return number of bytes in receive buffer
	 * @throws InterruptedException 
	 * @throws TimeOutException 
	 * @throws IOException 
	 */
	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex) throws InterruptedException, TimeOutException, IOException;

	/**
	 * waits until receive buffer is filled with number of expected bytes or does not change anymore in stableIndex cycles * 10 msec
	 * @param expectedBytes
	 * @param timeout_msec in milli seconds, this is the maximum time this process will wait for stable byte count or maxBytes
	 * @param stableIndex cycle count times 10 msec to be treat as stable
	 * @param minCount minimum number of bytes, even if stable
	 * @return number of bytes in receive buffer
	 * @throws InterruptedException 
	 * @throws TimeOutException 
	 * @throws IOException 
	 */
	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex, int minCount) throws InterruptedException, TimeOutException, IOException;

	/**
	 * query if the port is already open
	 * @return
	 */
	public boolean isConnected();

	/**
	 * @return number of transfer errors occur
	 */
	public int getXferErrors();

	/**
	 * add up transfer errors
	 */
	public void addXferError();

	/**
	 * @return number of query errors occur
	 */
	public int getTimeoutErrors();

	/**
	 * add up transfer errors
	 */
	public void addTimeoutError();

	/**
	 * check if a configured serial port string matches actual available ports
	 * @param newSerialPortStr
	 * @return true if given port string matches one of the available once
	 */
	public boolean isMatchAvailablePorts(String newSerialPortStr);

	/**
	 * check available bytes on input stream
	 * @return number of bytes available on inputstream
	 * @throws IOException
	 */
	public int getAvailableBytes() throws IOException;
	
	/////// USB interface starts here
  /**
   * find USB devices identified by vendor ID and product ID
   * @param vendorId
   * @param productId
   * @return
   * @throws UsbException
   */
	public Set<UsbDevice> findUsbDevices(final short vendorId, final short productId) throws UsbException;

	/**
	 * find USB devices starting from hub (root hub)
	 * @param hub
	 * @param vendorId
	 * @param productId
	 * @return
	 */
	public Set<UsbDevice> findDevices(UsbHub hub, short vendorId, short productId);

	/**
	 * dump required information for a USB device with known product ID and
	 * vendor ID
	 * @param vendorId
	 * @param productId
	 * @throws UsbException
	 */
	public void dumpUsbDevices(final short vendorId, final short productId) throws UsbException;

	
	/**
	 * claim USB interface with given number which correlates to open a USB port
	 * @param IDevice the actual device in use
	 * @return
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public UsbInterface openUsbPort(final IDevice activeDevice) throws UsbClaimException, UsbException;
	
	/**
	 * claim USB interface with given number which correlates to open a USB port
	 * @param IDevice the actual device in use
	 * @return
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public DeviceHandle openLibUsbPort(final IDevice activeDevice) throws UsbClaimException, UsbException;

	/**
	 * release or close the given interface
	 * @param usbInterface
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public void closeUsbPort(final UsbInterface usbInterface) throws UsbClaimException, UsbException;

	/**
	 * release or close the given interface
	 * @param usbInterface
	 * @param cacheSelectedUsbDevice true| false
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public void closeLibUsbPort(final DeviceHandle libUsbHandle, boolean cacheSelectedUsbDevice) throws UsbClaimException, UsbException;

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
	public int write(final UsbInterface iface, final byte endpointAddress, final byte[] data) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException;

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
	public int read(final UsbInterface iface, final byte endpointAddress, byte[] data) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException;

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
	public int read(final UsbInterface iface, final byte endpointAddress, byte[] data, final int timeout_msec) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException;

	
  /**
   * Writes some data byte array to the device.
   * @param handle The device handle.
   * @param outEndpoint The end point address
   * @param data the byte array for data with length as size to be send 
   * @param timeout_ms the time out in milli seconds
   * @throws IllegalStateException while handle not initialized
   * @throws TimeOutException while data transmission failed
   */
  public void write(final DeviceHandle handle, final byte outEndpoint, final byte[] data, final long timeout_ms) throws IllegalStateException, TimeOutException;

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
  public int read(final DeviceHandle handle, final byte inEndpoint, final byte[] data, final long timeout_ms) throws IllegalStateException, TimeOutException;

}
