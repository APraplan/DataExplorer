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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.usb.UsbClaimException;
import javax.usb.UsbDevice;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotClaimedException;

import org.eclipse.swt.SWT;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsbException;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortPacketListener;

import gde.GDE;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.exception.FailedQueryException;
import gde.exception.NoSuchPortException;
import gde.exception.PortInUseException;
import gde.exception.ReadWriteOutOfSyncException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

/**
 * DeviceSerialPort is the abstract class of the serial port implementation as parent for a device specific serial port implementation
 * @author Winfried Brügmann
 */
public class DeviceJavaSerialCommPortImpl implements IDeviceCommPort, SerialPortPacketListener {
	final static String										$CLASS_NAME								= DeviceJavaSerialCommPortImpl.class.getName();
	final static Logger										log												= Logger.getLogger(DeviceJavaSerialCommPortImpl.$CLASS_NAME);

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

	final protected DeviceConfiguration		deviceConfig;
	final protected DataExplorer					application;
	final Settings												settings;
	protected SerialPort									serialPort								= null;
	protected int													xferErrors								= 0;
	protected int													queryErrors								= 0;
	protected int													timeoutErrors							= 0;

	boolean																isConnected								= false;
	String																serialPortStr							= GDE.STRING_EMPTY;
	Thread																closeThread;

	InputStream														inputStream								= null;
	OutputStream													outputStream							= null;

	/**
	 * normal constructor to be used within DataExplorer
	 * @param currentDeviceConfig
	 * @param currentApplication
	 */
	public DeviceJavaSerialCommPortImpl(DeviceConfiguration currentDeviceConfig, DataExplorer currentApplication) {
		this.deviceConfig = currentDeviceConfig;
		this.application = currentApplication;
		this.settings = Settings.getInstance();
	}
	
	/**
	 * constructor for test purpose only, do not use within DataExplorer
	 */
	public DeviceJavaSerialCommPortImpl() {
		this.deviceConfig = null;
		this.application = null;
		this.settings = null;
	}

	/**
	 * updates the given vector with actual available according black/white list configuration
	 * @param doAvialabilityCheck
	 * @param portBlackList
	 * @param portWhiteList
	 */
	public synchronized static TreeMap<String, String> listConfiguredSerialPorts(final boolean doAvialabilityCheck, final String portBlackList, final Vector<String> portWhiteList) {
		final String $METHOD_NAME = "listConfiguredSerialPorts"; //$NON-NLS-1$
		if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "entry"); //$NON-NLS-1$

		try {
			DeviceCommPort.availablePorts.clear();

			if (portWhiteList.size() > 0) { // check ports from the white list only
				for (String serialPortDescriptor : portWhiteList) {
					SerialPort commPortIdentifier = null;
					for (SerialPort commPort : SerialPort.getCommPorts()) {
						if (commPort.getSystemPortName().equals(serialPortDescriptor)) {
							commPortIdentifier = commPort;
							break;
						}
					}
					try {
						if (commPortIdentifier != null) {
							if (doAvialabilityCheck) {
								boolean isOpen = commPortIdentifier.openPort(1);
								if (isOpen)
									commPortIdentifier.closePort();
								else if (commPortIdentifier.getDescriptivePortName().toLowerCase().contains("virtual")) {
									commPortIdentifier.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
									commPortIdentifier.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 1000);
									commPortIdentifier.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
									commPortIdentifier.disablePortConfiguration();
									isOpen = commPortIdentifier.openPort(1);
									if (isOpen)
										commPortIdentifier.closePort();
									else
										throw new NoSuchPortException();
								}
								else
									throw new NoSuchPortException();
							}
							DeviceCommPort.availablePorts.put(serialPortDescriptor, serialPortDescriptor + GDE.STRING_MESSAGE_CONCAT + commPortIdentifier.getDescriptivePortName());
							if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "Found available port: " + serialPortDescriptor); //$NON-NLS-1$
						}
					}
					catch (Exception e) {
						if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "Found port, but in use: " + serialPortDescriptor); //$NON-NLS-1$
					}
				}
			}
			else { // find all available serial ports, check against black list
				String serialPortStr;
				for (SerialPort commPort : SerialPort.getCommPorts()) { // initializes serial port
					serialPortStr = commPort.getSystemPortName();
					if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "Found available port: " + serialPortStr + ": " + commPort.getDescriptivePortName() + " - " + commPort.getPortDescription()); //$NON-NLS-1$
					if (!portBlackList.contains(serialPortStr)) {
						try {
							if (doAvialabilityCheck) {
								boolean isOpen = commPort.openPort(1);
								if (isOpen) {
									commPort.closePort();
								} 
								else if (commPort.getDescriptivePortName().toLowerCase().contains("virtual")) { //STM32 virtual
									commPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
									commPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 1000);
									commPort.setComPortParameters(115200, 8,  SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
									commPort.disablePortConfiguration();
									isOpen = commPort.openPort(1);
									if (isOpen)
										commPort.closePort();
									else
										throw new NoSuchPortException();
								}
								else new NoSuchPortException();
							}
							DeviceCommPort.availablePorts.put(serialPortStr, commPort.getSystemPortName() + GDE.STRING_MESSAGE_CONCAT + commPort.getDescriptivePortName());
							if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "Found available port: " + serialPortStr); //$NON-NLS-1$
						}
						catch (Exception e) {
							if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "Found port, but in use: " + serialPortStr); //$NON-NLS-1$
						}
					}
				}
			}
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder().append("Available serial Ports : "); //$NON-NLS-1$
				for (String comPort : DeviceCommPort.availablePorts.values()) {
					sb.append(comPort).append(" "); //$NON-NLS-1$
				}
				if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, sb.toString());
			}
			
			// Windows COM1, COM2 -> COM20
			// GNU/Linux /dev/ttyS0, /dev/ttyS1, /dev/ttyUSB0, /dev/ttyUSB1
			// MAC /dev/tty.WT12-BT-MX20 /dev/tty.GraupnerHoTTBT32
			if (GDE.IS_MAC) {
				Iterator<String> iterator = DeviceCommPort.availablePorts.keySet().iterator();
				while (iterator.hasNext()) {
					if (iterator.next().contains("cu."))
						iterator.remove();
				}
			}
		}
		catch (Throwable t) {
			log.log(Level.WARNING, t.getMessage(), t);
		}

		if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "exit"); //$NON-NLS-1$
		return DeviceCommPort.availablePorts;
	}

	/**
	 * @return available serial port list
	 */
	public static String[] prepareSerialPortList() {
		String[] serialPortList = new String[DeviceCommPort.availablePorts.size()];
		String[] tmpSerialPortList = DeviceCommPort.availablePorts.values().toArray(new String[DeviceCommPort.availablePorts.size()]);
		for (int i = 0; i < tmpSerialPortList.length; i++) {
			serialPortList[i] = GDE.STRING_BLANK + tmpSerialPortList[i];
		}
		return serialPortList;
	}

	/**
	 * check if a configured serial port string matches actual available ports
	 * @param newSerialPortStr
	 * @return true if given port string matches one of the available once
	 */
	public boolean isMatchAvailablePorts(String newSerialPortStr) {
		boolean match = false;
		if (DeviceCommPort.availablePorts.size() == 0 || (this.serialPortStr != null && !DeviceCommPort.availablePorts.keySet().contains(this.serialPortStr))) {
			listConfiguredSerialPorts(false, this.settings.isSerialPortBlackListEnabled() ? this.settings.getSerialPortBlackList() : GDE.STRING_EMPTY,
					this.settings.isSerialPortWhiteListEnabled() ? this.settings.getSerialPortWhiteList() : new Vector<String>());
		}
		for (String availablePort : DeviceCommPort.availablePorts.keySet()) {
			if (availablePort.equals(newSerialPortStr)) {
				match = true;
				break;
			}
		}
		return match;
	}

	/**
	 * opens the serial port specified in device configuration or settings (global)
	 * @return reference to instance of serialPort
	 * @throws ApplicationConfigurationException
	 * @throws SerialPortException
	 */
	public SerialPort open() throws ApplicationConfigurationException, SerialPortException {
		final String $METHOD_NAME = "open"; //$NON-NLS-1$
		this.xferErrors = this.timeoutErrors = 0;
		// Initialize serial port
		try {
			this.serialPortStr = this.deviceConfig.getPort();
			// check if the serial port which is selected can be used
			if (this.serialPortStr == null || this.serialPortStr.length() < 4 || !isMatchAvailablePorts(this.serialPortStr)) {
				if (DeviceCommPort.availablePorts.size() == 1 && (this.serialPortStr != null && !isMatchAvailablePorts(this.serialPortStr))) {
					if (SWT.YES == this.application.openYesNoMessageDialogSync(Messages.getString(MessageIds.GDE_MSGE0010) + GDE.LINE_SEPARATOR
							+ Messages.getString(MessageIds.GDE_MSGT0194, new String[] { this.serialPortStr = DeviceCommPort.availablePorts.firstKey() }))) {
						this.serialPortStr = DeviceCommPort.availablePorts.firstKey();
						this.deviceConfig.setPort(this.serialPortStr);
						this.deviceConfig.storeDeviceProperties();
						this.application.updateTitleBar();
					}
					else {
						throw new ApplicationConfigurationException(Messages.getString(MessageIds.GDE_MSGE0010));
					}
				}
				else {
					throw new ApplicationConfigurationException(Messages.getString(MessageIds.GDE_MSGE0010));
				}
			}
			log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME,
				String.format("serialPortString = %s; baudeRate = %d; dataBits = %s; stopBits = %s; parity = %s; flowControlMode = %s; RTS = %s; DTR = %s", this.serialPortStr, this.deviceConfig.getBaudeRate(), this.deviceConfig.getDataBits(), this.deviceConfig.getStopBits(), this.deviceConfig.getParity(), this.deviceConfig.getFlowCtrlMode(), this.deviceConfig.isRTS(), this.deviceConfig.isDTR())); //$NON-NLS-1$

			for (SerialPort commPort : SerialPort.getCommPorts()) {
				if (commPort.getSystemPortName().equals(this.serialPortStr)) {
					this.serialPort = commPort;
					break;
				}
			}
			//this.serialPort = SerialPort.getCommPort(this.serialPortStr);
			boolean isVirtual = GDE.IS_WINDOWS && this.serialPort.getDescriptivePortName().toLowerCase().contains("virtual");
			if (isVirtual) {
				this.serialPort.setFlowControl(this.deviceConfig.getFlowCtrlMode());
				this.serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 1000);
				this.serialPort.setComPortParameters(115200, this.deviceConfig.getDataBits().ordinal() + 5,  this.deviceConfig.getStopBits().ordinal() + 1, this.deviceConfig.getParity().ordinal());
				this.serialPort.disablePortConfiguration();
			}
			if (this.serialPort.openPort()) {
				if (!isVirtual) { // set port parameters
					this.serialPort.setComPortParameters(this.deviceConfig.getBaudeRate(), this.deviceConfig.getDataBits().ordinal() + 5,  this.deviceConfig.getStopBits().ordinal() + 1, this.deviceConfig.getParity().ordinal());
					this.serialPort.setFlowControl(this.deviceConfig.getFlowCtrlMode());
					if (this.deviceConfig.isRTS()) this.serialPort.setRTS();
					else this.serialPort.clearRTS();
					if (this.deviceConfig.isDTR()) this.serialPort.setDTR();
					else this.serialPort.clearDTR();
				}
				// init in and out stream for writing and reading
				this.inputStream = this.serialPort.getInputStream();
				this.outputStream = this.serialPort.getOutputStream();
				this.isConnected = true;
				if (this.application != null) this.application.setPortConnected(true);
			} else {
				throw new PortInUseException();
			}
		}
		catch (ApplicationConfigurationException e) {
			log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			if (this.serialPort != null) this.serialPort.closePort();
			throw e;
		}
		catch (PortInUseException e) {
			SerialPortException en = new SerialPortException(e.getMessage());
			log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, en.getMessage(), en);
			if (this.serialPort != null) this.serialPort.closePort();
			throw en;
		}
		catch (Throwable e) {
			SerialPortException en = new SerialPortException(e.getMessage());
			log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, en.getMessage(), en);
			if (this.serialPort != null) this.serialPort.closePort();
			throw en;
		}
		return this.serialPort;
	}

	/**
	 * write bytes to serial port output stream, cleans receive buffer if available byes prior to send data 
	 * @param writeBuffer writes size of writeBuffer to output stream
	 * @throws IOException
	 */
	public synchronized void write(byte[] writeBuffer) throws IOException {
		final String $METHOD_NAME = "write"; //$NON-NLS-1$

		try {
			if (this.application != null) this.application.setSerialTxOn();
			cleanInputStream();

			this.outputStream.write(writeBuffer);
			if (GDE.IS_LINUX && GDE.IS_ARCH_DATA_MODEL_64) {
				this.outputStream.flush();
			}

			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "Write : " + StringHelper.byte2Hex2CharString(writeBuffer, writeBuffer.length));
		}
		catch (IOException e) {
			log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			throw e;
		}
		finally {
			if (this.application != null) this.application.setSerialTxOff();
		}
	}

	/**
	 * write bytes to serial port output stream, each byte individual with the given time gap in msec
	 * cleans receive buffer if available byes prior to send data 
	 * @param writeBuffer writes size of writeBuffer to output stream
	 * @throws IOException
	 */
	public synchronized void write(byte[] writeBuffer, long gap_ms) throws IOException {
		final String $METHOD_NAME = "write"; //$NON-NLS-1$

		try {
			if (this.application != null) this.application.setSerialTxOn();
			cleanInputStream();

			for (int i = 0; i < writeBuffer.length; i++) {
				this.outputStream.write(writeBuffer[i]);
				WaitTimer.delay(gap_ms);
			}
			if (GDE.IS_LINUX && GDE.IS_ARCH_DATA_MODEL_64) {
				this.outputStream.flush();
			}

			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "Write : " + StringHelper.byte2Hex2CharString(writeBuffer, writeBuffer.length));
		}
		catch (IOException e) {
			log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			throw e;
		}
		finally {
			if (this.application != null) this.application.setSerialTxOff();
		}
	}

	/**
	 * cleanup the input stream if there are bytes available
	 * @return number of bytes in receive buffer which get removed
	 * @throws IOException
	 */
	public int cleanInputStream() throws IOException {
		final String $METHOD_NAME = "cleanInputStream"; //$NON-NLS-1$
		int num = 0;
		if ((num = this.inputStream.available()) != 0) {
			this.inputStream.read(new byte[num]);
			log.logp(Level.WARNING, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "clean inputStream left bytes -> " + num); //$NON-NLS-1$
		}
		return num;
	}

	/**
	 * event handler method handles only events as previous registered
	 * - activate the DATA_AVAILABLE notifier to read available data -> dataAvailable = true;
	 * - activate the OUTPUT_BUFFER_EMPTY notifier -> dataAvailable = false;
	 */
	public void serialEvent(SerialPortEvent event) {
		final String $METHOD_NAME = "serialEvent"; //$NON-NLS-1$


		byte[] newData = event.getReceivedData();
		if (newData.length > 0) {
			//this.dataAvailable = false;
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "OUTPUT_BUFFER_EMPTY"); //$NON-NLS-1$
		} else {
			//this.dataAvailable = true;
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "DATA_AVAILABLE"); //$NON-NLS-1$
		}
	}

	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * @param readBuffer
	 * @param timeout_msec
	 * @return the red byte array
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized byte[] read(byte[] readBuffer, int timeout_msec) throws IOException, TimeOutException {
		final String $METHOD_NAME = "read"; //$NON-NLS-1$
		int sleepTime = 2 ; // ms
		int bytes = readBuffer.length;
		int readBytes = 0;
		int timeOutCounter = timeout_msec / (sleepTime + 18); //18 ms read blocking time

		try {
			if (this.application != null) this.application.setSerialRxOn();
			wait4Bytes(bytes, timeout_msec - (timeout_msec / 5));


			while (bytes != readBytes && timeOutCounter-- > 0) {
				if (this.inputStream.available() > 0) {
					readBytes += this.inputStream.read(readBuffer, 0 + readBytes, bytes - readBytes);
				}
				if (bytes != readBytes) {
					WaitTimer.delay(sleepTime);
				}

				//this.dataAvailable = false;
				if (timeOutCounter <= 0) {
					TimeOutException e = new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { bytes, timeout_msec }));
					log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
					log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "  Read : " + StringHelper.byte2Hex2CharString(readBuffer, readBytes));
					throw e;
				}
			}

			if (log.isLoggable(Level.FINE)) {
				log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "  Read : " + StringHelper.byte2Hex2CharString(readBuffer, readBytes));
			}
		}
		catch (IOException e) {
			log.logp(Level.WARNING, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			throw e;
		}
		finally {
			if (this.application != null) this.application.setSerialRxOff();
		}
		return readBuffer;
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
		final String $METHOD_NAME = "read"; //$NON-NLS-1$
		int sleepTime = 10; // ms
		int bytes = readBuffer.length;
		int readBytes = 0;
		int timeOutCounter = timeout_msec / (sleepTime + 18); //18 ms read blocking time

		try {
			if (this.application != null) this.application.setSerialRxOn();
			WaitTimer.delay(2);

			//loop inputStream and read available bytes
			while (bytes != readBytes && timeOutCounter-- > 0) {
				if (this.inputStream.available() > 0) {
					readBytes += this.inputStream.read(readBuffer, 0 + readBytes, bytes - readBytes);
				}
				if (bytes != readBytes) {
					WaitTimer.delay(sleepTime);
				}

//				if (timeOutCounter/4 <= 0 && readBytes == 0) {
//					FailedQueryException e = new FailedQueryException(Messages.getString(MessageIds.GDE_MSGE0012, new Object[] { timeout_msec/4 }));
//					log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage());
//					throw e;
//				}
//				else 
					if (timeOutCounter <= 0) {
					TimeOutException e = new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { bytes, timeout_msec }));
					log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
					log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "  Read : " + StringHelper.byte2Hex2CharString(readBuffer, readBytes));
					throw e;
				}
			}

			if (log.isLoggable(Level.FINE)) {
				log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "  Read : " + StringHelper.byte2Hex2CharString(readBuffer, readBytes));
			}
		}
		catch (IOException e) {
			log.logp(Level.WARNING, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			throw e;
		}
		finally {
			if (this.application != null) this.application.setSerialRxOff();
		}
		return readBuffer;
	}

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
	public synchronized byte[] read(byte[] readBuffer, int timeout_msec, Vector<Long> waitTimes) throws IOException, TimeOutException {
		final String $METHOD_NAME = "read"; //$NON-NLS-1$
		int sleepTime = 4; // ms
		int bytes = readBuffer.length;
		int readBytes = 0;
		int timeOutCounter = timeout_msec / sleepTime;

		try {
			if (this.application != null) this.application.setSerialRxOn();
			long startTime_ms = new Date().getTime();
			wait4Bytes(timeout_msec);

			while (bytes != readBytes && timeOutCounter-- > 0) {
				readBytes += this.inputStream.read(readBuffer, readBytes, bytes - readBytes);
				if (bytes != readBytes) {
					WaitTimer.delay(sleepTime); //run synchronous do not use start() here
				}
			}
			//this.dataAvailable = false;
			if (timeOutCounter <= 0) {
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { bytes, timeout_msec }));
				log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
				log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "  Read : " + StringHelper.byte2Hex2CharString(readBuffer, readBytes));
				throw e;
			}

			long ms = (new Date().getTime()) - startTime_ms;
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "waitTime = " + ms); //$NON-NLS-1$
			waitTimes.add(ms);

			log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "  Read : " + StringHelper.byte2Hex2CharString(readBuffer, readBytes));
		}
		catch (IOException e) {
			log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			throw e;
		}
		catch (InterruptedException e) {
			log.logp(Level.WARNING, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
		finally {
			if (this.application != null) this.application.setSerialRxOff();
		}
		return readBuffer;
	}

	/**
	 * function check for available bytes on receive buffer
	 * @return System.currentTimeMillis() if data available within time out, else an exception
	 * @throws InterruptedException 
	 * @throws TimeOutException 
	 * @throws IOException 
	 */
	public long wait4Bytes(int timeout_msec) throws InterruptedException, TimeOutException, IOException {
		final String $METHOD_NAME = "wait4Bytes"; //$NON-NLS-1$
		int sleepTime = 1;
		int timeOutCounter = timeout_msec / sleepTime;

		while (0 == this.inputStream.available()) {
			WaitTimer.delay(sleepTime);

			if (timeOutCounter-- <= 0) {
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { "*", timeout_msec })); //$NON-NLS-1$ 
				log.logp(Level.WARNING, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
				throw e;
			}
		}
		return System.currentTimeMillis();
	}

	/**
	 * waits until receive buffer is filled with the number of expected bytes while checking inputStream
	 * @param numBytes
	 * @param timeout_msec
	 * @return number of bytes in receive buffer
	 * @throws TimeOutException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public int wait4Bytes(int numBytes, int timeout_msec) throws IOException {
		final String $METHOD_NAME = "wait4Bytes"; //$NON-NLS-1$
		int sleepTime = 1; // msec
		int timeOutCounter = timeout_msec / sleepTime;
		int resBytes = 0;

		while ((resBytes = this.inputStream.available()) < numBytes) {
			WaitTimer.delay(sleepTime);

			timeOutCounter--;
			//if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, "time out counter = " + counter);
			if (timeOutCounter <= 0) {
				log.logp(Level.WARNING, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, String.format("only %d of %d Bytes are available in %d msec", resBytes, numBytes, timeout_msec));
				break;
			}
		}

		return resBytes;
	}

	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * if the readBuffer can not be filled a stable counter will be active where a number of retries can be specified
	 * @param readBuffer with the size expected bytes
	 * @param timeout_msec
	 * @param stableIndex a number of cycles to treat as telegram transmission finished
	 * @return the reference of the given byte array, byte array meight be adapted to received size
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex) throws IOException, TimeOutException {
		final String $METHOD_NAME = "read"; //$NON-NLS-1$
		int sleepTime = 4; // ms
		int numAvailableBytes = readBuffer.length;
		int readBytes = 0;
		int timeOutCounter = timeout_msec / sleepTime;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "entry");
		if (stableIndex >= timeOutCounter) {
			log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, Messages.getString(MessageIds.GDE_MSGE0013));
		}

		try {
			if (this.application != null) this.application.setSerialRxOn();

			numAvailableBytes = waitForStableReceiveBuffer(numAvailableBytes, timeout_msec, stableIndex);
			//adapt readBuffer, available bytes more than expected
			if (numAvailableBytes > readBuffer.length) 
				readBuffer = new byte[numAvailableBytes];

			while (readBytes < numAvailableBytes && timeOutCounter-- > 0) {
				readBytes += this.inputStream.read(readBuffer, 0 + readBytes, numAvailableBytes - readBytes);

				if (numAvailableBytes != readBytes) {
					WaitTimer.delay(sleepTime);
				}
			}
			//this.dataAvailable = false;
			if (timeOutCounter <= 0) {
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { numAvailableBytes, timeout_msec }));
				log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
				log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "  Read : " + StringHelper.byte2Hex2CharString(readBuffer, readBytes));
				throw e;
			}

			// resize the data buffer to real red data 
			if (readBytes < readBuffer.length) {
				byte[] tmpBuffer = new byte[readBytes];
				System.arraycopy(readBuffer, 0, tmpBuffer, 0, readBytes);
				readBuffer = tmpBuffer;
			}

			if (log.isLoggable(Level.FINE)) 
				log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "  Read : " + StringHelper.byte2Hex2CharString(readBuffer, readBytes));

		}
		catch (IndexOutOfBoundsException e) {
			log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			throw e;
		}
		catch (IOException e) {
			log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			throw e;
		}
		catch (InterruptedException e) {
			log.logp(Level.WARNING, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
		finally {
			if (this.application != null) this.application.setSerialRxOff();
		}
		return readBuffer;
	}

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
	public synchronized byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex, int minCountBytes) throws IOException, TimeOutException {
		final String $METHOD_NAME = "read"; //$NON-NLS-1$
		int sleepTime = 4; // ms
		int expectedBytes = readBuffer.length;
		int readBytes = 0;
		int timeOutCounter = timeout_msec / sleepTime;
		if (stableIndex >= timeOutCounter) {
			log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, Messages.getString(MessageIds.GDE_MSGE0013));
		}

		try {
			if (this.application != null) this.application.setSerialRxOn();

			expectedBytes = waitForStableReceiveBuffer(expectedBytes, timeout_msec, stableIndex, minCountBytes);

			while (readBytes < expectedBytes && timeOutCounter-- > 0) {
				readBytes += this.inputStream.read(readBuffer, 0 + readBytes, expectedBytes - readBytes);

				if (expectedBytes != readBytes) {
					WaitTimer.delay(sleepTime);
				}
			}
			//this.dataAvailable = false;
			if (timeOutCounter <= 0) {
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { expectedBytes, timeout_msec }));
				log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
				log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "  Read : " + StringHelper.byte2Hex2CharString(readBuffer, readBytes));
				throw e;
			}

			// resize the data buffer to real red data 
			if (readBytes < readBuffer.length) {
				byte[] tmpBuffer = new byte[readBytes];
				System.arraycopy(readBuffer, 0, tmpBuffer, 0, readBytes);
				readBuffer = tmpBuffer;
			}

			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "  Read : " + StringHelper.byte2Hex2CharString(readBuffer, readBytes));

		}
		catch (IndexOutOfBoundsException e) {
			log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			throw e;
		}
		catch (IOException e) {
			log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			throw e;
		}
		catch (InterruptedException e) {
			log.logp(Level.WARNING, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
		finally {
			if (this.application != null) this.application.setSerialRxOff();
		}
		return readBuffer;
	}

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
	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex) throws InterruptedException, TimeOutException, IOException {
		final String $METHOD_NAME = "waitForStableReceiveBuffer"; //$NON-NLS-1$
		int sleepTime = 1; // ms
		int timeOutCounter = timeout_msec / sleepTime;
		int stableCounter = stableIndex;
		boolean isStable = false;
		boolean isTimedOut = false;

		// availableBytes are updated by event handler
		int byteCounter = 0, numBytesAvailable = 0;
		while (byteCounter < expectedBytes && !isStable && !isTimedOut) {
			WaitTimer.delay(sleepTime);

			if (byteCounter == (numBytesAvailable = this.inputStream.available()) && byteCounter > 0) {
				if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "stableCounter = " + stableCounter + " byteCounter = " + byteCounter); //$NON-NLS-1$ //$NON-NLS-2$
				--stableCounter;
			}
			else 
				stableCounter = stableIndex;

			if (stableCounter == 0) isStable = true;

			byteCounter = numBytesAvailable;

			--timeOutCounter;

			if (timeOutCounter == 0) {
				log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, String.format("byteCounter = %d numBytesAvailable = %d", byteCounter, numBytesAvailable));
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { expectedBytes, timeout_msec }));
				throw e;
			}

		} // end while
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "byteCounter = " + byteCounter + " timeOutCounter = " + timeOutCounter); //$NON-NLS-1$ //$NON-NLS-2$
		return byteCounter;
	}

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
	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex, int minCount) throws InterruptedException, TimeOutException, IOException {
		final String $METHOD_NAME = "waitForStableReceiveBuffer"; //$NON-NLS-1$
		int sleepTime = 1; // ms
		int timeOutCounter = timeout_msec / sleepTime;
		int stableCounter = stableIndex;
		boolean isStable = false;
		boolean isTimedOut = false;

		// availableBytes are updated by event handler
		int byteCounter = 0, numBytesAvailable = 0;
		while (byteCounter < expectedBytes && !isStable && !isTimedOut) {
			WaitTimer.delay(sleepTime);

			if (byteCounter == (numBytesAvailable = this.inputStream.available()) && byteCounter > minCount) {
				if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "stableCounter = " + stableCounter + " byteCounter = " + byteCounter); //$NON-NLS-1$ //$NON-NLS-2$
				--stableCounter;
			}
			else 
				stableCounter = stableIndex;

			if (stableCounter == 0) isStable = true;

			byteCounter = numBytesAvailable;

			--timeOutCounter;

			if (timeOutCounter == 0) {
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { expectedBytes, timeout_msec }));
				log.logp(Level.SEVERE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
				throw e;
			}

		} // end while
		log.logp(Level.FINE, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "byteCounter = " + byteCounter + " timeOutCounter = " + timeOutCounter); //$NON-NLS-1$ //$NON-NLS-2$
		return byteCounter;
	}

	/**
	 * function check for left bytes on receive buffer -> called to check wait for stable bytes missed
	 * @throws ReadWriteOutOfSyncException 
	 * @throws IOException 
	 */
	public void checkForLeftBytes() throws ReadWriteOutOfSyncException, IOException {
		final String $METHOD_NAME = "checkForLeftBytes"; //$NON-NLS-1$
		//check available bytes in receive buffer == 0
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "inputStream available bytes = " + this.inputStream.available()); //$NON-NLS-1$
		if (this.inputStream.available() != 0) throw new ReadWriteOutOfSyncException(Messages.getString(MessageIds.GDE_MSGE0014));
	}

	/**
	 * function to close the serial port
	 * this is done within a tread since the port can't close if it stays open for a long time period ??
	 */
	public synchronized void close() {
		final String $METHOD_NAME = "close"; //$NON-NLS-1$
		if (this.isConnected && DeviceJavaSerialCommPortImpl.this.serialPort != null) {
			DeviceJavaSerialCommPortImpl.this.isConnected = false;
			this.closeThread = new Thread("closePort") {
				@Override
				public void run() {
					log.logp(Level.CONFIG, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "entry"); //$NON-NLS-1$
					try {
						Thread.sleep(5);
						byte[] buf = new byte[getInputStream().available()];
						if (buf.length > 0) getInputStream().read(buf);
						getOutputStream().flush();
						Thread.sleep(5);
					}
					catch (Throwable e) {
						log.logp(Level.WARNING, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
					}
					log.logp(Level.CONFIG, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "before close"); //$NON-NLS-1$
					DeviceJavaSerialCommPortImpl.this.serialPort.closePort();
					log.logp(Level.CONFIG, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "after close"); //$NON-NLS-1$
					DeviceJavaSerialCommPortImpl.this.isConnected = false;
					if (DeviceJavaSerialCommPortImpl.this.application != null) DeviceJavaSerialCommPortImpl.this.application.setPortConnected(false);
					log.logp(Level.CONFIG, DeviceJavaSerialCommPortImpl.$CLASS_NAME, $METHOD_NAME, "exit"); //$NON-NLS-1$
				}
			};
			try {
				this.closeThread.start();
			}
			catch (RuntimeException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	/**
	 * check available bytes on input stream
	 * @return number of bytes available on input stream
	 * @throws IOException
	 */
	public int getAvailableBytes() throws IOException {
		return this.inputStream.available();
	}

	public InputStream getInputStream() {
		return this.inputStream;
	}

	public OutputStream getOutputStream() {
		return this.outputStream;
	}

	public boolean isConnected() {
		return this.isConnected;
	}

	/**
	 * @return the serialPortStr
	 */
	public String getSerialPortStr() {
		return this.serialPortStr == null ? this.deviceConfig.getPort() : this.serialPortStr;
	}

	/**
	 * @return number of transfer errors
	 */
	public int getXferErrors() {
		return this.xferErrors;
	}

	/**
	 * add up transfer errors
	 */
	public void addXferError() {
		this.xferErrors++;
	}

	/**
	 * add up timeout errors
	 */
	public void addTimeoutError() {
		this.timeoutErrors++;
	}

	/**
	 * @return number of timeout errors 
	 */
	public int getTimeoutErrors() {
		return this.timeoutErrors;
	}
	
	/**
	 * main method to test this class
	 * @param args
	 */
	public static void main(String[] args) {
		Logger logger = Logger.getLogger(GDE.STRING_EMPTY);
		logger.setLevel(Level.OFF);

		DeviceJavaSerialCommPortImpl impl = new DeviceJavaSerialCommPortImpl();
		byte[] buffer = new byte[1024];
		try {

			if (args.length > 0 && args[0].startsWith("COM")) {
				//open begin
				impl.serialPort = SerialPort.getCommPort(args[0].trim());
				impl.serialPort.openPort(100);
				if (impl.serialPort.isOpen()) {
					// set port parameters
					impl.serialPort.setComPortParameters(19200, 8, 1, 0);
					impl.serialPort.setFlowControl(0);
					// init in and out stream for writing and reading
					impl.inputStream = impl.serialPort.getInputStream();
					impl.outputStream = impl.serialPort.getOutputStream();
					impl.isConnected = true;
					//open end
					if (args.length > 1 && args[1].equals("sender")) {
						//			for (int baud = 2400; baud <= 115200; baud+=100) {
						//				System.out.println(baud);

						for (int i = 1; i < 126; i++) {
							impl.write(new byte[] { (byte) i });
						}

						impl.wait4Bytes(1000);
						int numBytes = 0, redBytes = 0;
						while ((numBytes = impl.inputStream.available()) != 0) {
							byte[] readBuffer = new byte[numBytes];
							impl.read(readBuffer, 1000);
							System.arraycopy(readBuffer, 0, buffer, redBytes, numBytes);
							redBytes += numBytes;
						}
						System.out.println(StringHelper.byte2CharString(buffer, redBytes));
						//close begin
						impl.close();
						//close end
						//			}
					}
					else { //receiver
						while (true) {
							impl.wait4Bytes(1000);
							int numBytes = 0, redBytes = 0;
							while ((numBytes = impl.inputStream.available()) != 0) {
								byte[] readBuffer = new byte[numBytes];
								impl.read(readBuffer, 1000);
								System.arraycopy(readBuffer, 0, buffer, redBytes, numBytes);
								redBytes += numBytes;
							}
							System.out.println(StringHelper.byte2CharString(buffer, redBytes));
						}
					}
				}
			}
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
		finally {
			//close begin
			impl.close();
			//close end
		}
	}

	/////// USB interface starts here
  /**
   * find USB device to be identified by vendor ID and product ID
   * @param vendorId
   * @param productId
   * @return
   * @throws UsbException
   */
	public Set<UsbDevice> findUsbDevices(final short vendorId, final short productId) throws UsbException {
		return null;
	}

	/**
	 * find USB device starting from hub (root hub)
	 * @param hub
	 * @param vendorId
	 * @param productId
	 * @return
	 */
	public Set<UsbDevice> findDevices(UsbHub hub, short vendorId, short productId) {
		return null;
	}

	/**
	 * dump required information for a USB device with known product ID and
	 * vendor ID
	 * @param vendorId
	 * @param productId
	 * @throws UsbException
	 */
	public void dumpUsbDevices(final short vendorId, final short productId) throws UsbException {
		//no explicit return result
	}
	
	/**
	 * claim USB interface with given number which correlates to open a USB port
	 * @param IDevice the actual device in use
	 * @return
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public UsbInterface openUsbPort(final IDevice activeDevice) throws UsbClaimException, UsbException {
		return null;
	}
	
	/**
	 * claim USB interface with given number which correlates to open a USB port
	 * @param IDevice the actual device in use
	 * @return
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public DeviceHandle openLibUsbPort(final IDevice activeDevice) throws LibUsbException, UsbException {
		return null;
	}

	/**
	 * release or close the given interface
	 * @param usbInterface
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public void closeUsbPort(final UsbInterface usbInterface) throws UsbClaimException, UsbException {
		//no explicit return result
	}

	/**
	 * release or close the given lib usb handle
	 * @param libUsbDeviceHanlde
	 * @param cacheSelectedUsbDevice true| false
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public void closeLibUsbPort(final DeviceHandle libUsbDeviceHanlde, boolean cacheSelectedUsbDevice) throws LibUsbException, UsbException {
		//no explicit return result
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
		return 0;
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
		return 0;
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
		return 0;
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
  	return;
  } 

  /**
   * Reads some data with length from the device
   * @param handle The device handle.
   * @param inEndpoint The end point address
   * @param data the byte array for data with length as size to be received 
   * @param timeout_ms the time out in milli seconds
   * @throws IllegalStateException while handle not initialized
   * @throws TimeOutException while data transmission failed
   */
  public int read(final DeviceHandle handle, final byte inEndpoint, final byte[] data, final long timeout_ms) throws IllegalStateException, TimeOutException {
  	return 0;
  }

	@Override
	public int getListeningEvents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPacketSize() {
		// TODO Auto-generated method stub
		return 0;
	}

}
