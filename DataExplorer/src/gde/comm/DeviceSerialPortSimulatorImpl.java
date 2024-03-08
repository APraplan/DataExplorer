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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
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
import org.eclipse.swt.widgets.FileDialog;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsbException;

import gde.GDE;
import gde.config.Settings;
import gde.device.FormatTypes;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.ApplicationConfigurationException;
import gde.exception.FailedQueryException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.io.DataParser;
import gde.io.LogViewReader;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;
import gnu.io.SerialPort;

/**
 * class to simulate serial port where bytes read form input file
 */
public class DeviceSerialPortSimulatorImpl implements IDeviceCommPort {
	final static String	$CLASS_NAME					= DeviceSerialPortSimulatorImpl.class.getName();
	final static Logger	log									= Logger.getLogger($CLASS_NAME);

	DataInputStream			data_in;
	BufferedReader			txt_in;
	int									xferErrors					= 0;
	int									timeoutErrors				= 0;
	boolean							isConnected					= false;
	String 							fileType 						= GDE.FILE_ENDING_STAR_LOV;

	final IDevice				device;
	final DataExplorer	application;
	final Settings			settings;
	final int						sleepTime_ms;
	final boolean				isTimeStepConstant;

	/**
	 * constructor to create a communications port simulation instance
	 * @param currentDevice
	 * @param currentApplication
	 * @param timeStep_ms
	 */
	public DeviceSerialPortSimulatorImpl(IDevice currentDevice, DataExplorer currentApplication, boolean isTimeStepConstant, int timeStep_ms) {
		this.device = currentDevice;
		this.application = currentApplication;
		this.settings = Settings.getInstance();
		this.sleepTime_ms = timeStep_ms;
		this.isTimeStepConstant = isTimeStepConstant;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#open()
	 */
	public SerialPort open() throws ApplicationConfigurationException, SerialPortException {
		try {
			if (this.application != null) {
				GDE.display.syncExec(new Runnable() {
					public void run() {
						String path;
						if (application.isObjectoriented()) {
							path = application.getObjectFilePath();
						}
						else {
							String devicePath = application.getActiveDevice() != null ? GDE.STRING_FILE_SEPARATOR_UNIX + application.getActiveDevice().getName() : GDE.STRING_EMPTY;
							path = application.getActiveDevice() != null ? settings.getDataFilePath() + devicePath + GDE.STRING_FILE_SEPARATOR_UNIX : settings.getDataFilePath();
							if (!FileUtils.checkDirectoryAndCreate(path)) {
								if (!FileUtils.checkDirectoryExist(path)) 
									application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0056, new Object[] { path }));
								else
									application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0012, new Object[] { path }));
							}
						}
						FileDialog openFileDialog = application.openFileOpenDialog("Open File used as simulation input", new String[] { GDE.FILE_ENDING_STAR_LOV, GDE.FILE_ENDING_STAR_TXT,
								GDE.FILE_ENDING_STAR_LOG }, path, null, SWT.SINGLE);
						if (openFileDialog.getFileName().length() > 4) {
							String openFilePath = (openFileDialog.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + openFileDialog.getFileName()).replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);

							try {
								if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_OSD)) {
									fileType = GDE.FILE_ENDING_STAR_OSD;
									//add implementation to use *.osd files as simulation data input
								}
								else if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_LOV)) {
									fileType = GDE.FILE_ENDING_STAR_LOV;
									data_in = new DataInputStream(new FileInputStream(new File(openFilePath)));
									LogViewReader.readHeader(data_in);
									switch (application.getActiveDevice().getName()) {
									case "Robbe PowerPeak IV":
										data_in.skip(6); //data start offset
										break;
									default:
										break;
									}
								}
								else if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_TXT)) {
									fileType = GDE.FILE_ENDING_STAR_TXT;
									txt_in = new BufferedReader(new InputStreamReader(new FileInputStream(openFilePath), "ISO-8859-1")); //$NON-NLS-1$
									txt_in.read();
								}
								else if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
									fileType = GDE.FILE_ENDING_STAR_LOG;
									txt_in = new BufferedReader(new InputStreamReader(new FileInputStream(openFilePath), "ISO-8859-1")); //$NON-NLS-1$
								}
								else
									application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0008) + openFilePath);
							}
							catch (Exception e) {
								log.log(Level.SEVERE, e.getMessage(), e);
							}
						}
						isConnected = data_in != null || txt_in != null;
						application.setPortConnected(isConnected);
					}
				});
			}
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#close()
	 */
	public void close() {
		try {
			if (data_in != null) {
				data_in.close(); 
				data_in = null;
			}
			if (txt_in != null) {
				txt_in.close(); 
				txt_in = null;
			}
			if (this.isConnected) {
				this.isConnected = false;
			}
			if (this.application != null) this.application.setPortConnected(false);
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#read(byte[], int)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec) throws IOException, TimeOutException {
		final String $METHOD_NAME = "read"; //$NON-NLS-1$
		try {
			wait4Bytes(timeout_msec);
		}
		catch (InterruptedException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		if (this.isConnected) {
			if (data_in != null && this.fileType.equals(GDE.FILE_ENDING_STAR_LOV)) {
				if (data_in.read(readBuffer) > 0) {
					//System.out.println(StringHelper.byte2Hex2CharString(readBuffer, readBuffer.length));
					int size2Read = this.device.getLovDataByteSize() - Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO));
					if (data_in.read(new byte[size2Read]) != size2Read) {
						log.log(Level.WARNING, "expected byte size to  read does not macht really red size of bytes !");
					}
					if (this.device.getName().toLowerCase().contains("4010duo")) {
						byte[] tmpBuffer = new byte[ Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO))];
						System.arraycopy(readBuffer, 5, tmpBuffer, 0, tmpBuffer.length-5);
						System.arraycopy(tmpBuffer, 0, readBuffer, 0, tmpBuffer.length);						
					}
				}
				else {
					readBuffer = new byte[0];
					this.close();
				}
			}
			else if (txt_in != null) {
				if (this.fileType.equals(GDE.FILE_ENDING_STAR_TXT)) {
					StringBuffer sb = new StringBuffer();
					int value;

					sb.append('\f');
					while ((value = txt_in.read()) != -1 && value != '\f')
						sb.append((char) value);

					if (sb.length() > 1)
						readBuffer = sb.toString().getBytes();
					else
						this.close();
				}
				else if (this.fileType.equals(GDE.FILE_ENDING_STAR_LOG)) {
					String line;
					if ((line = txt_in.readLine()) != null) {
						while(!line.contains("WARNING") && !(line.contains("Read") && line.length() > line.indexOf("Read") + 15) && (line = txt_in.readLine()) != null) ;
						
						//System.out.println(line);
						if(line != null && line.contains("Read")) {
							boolean isQCdata = false;
							line = line.substring(line.indexOf("Read") + 15);
							StringTokenizer token = new StringTokenizer(line);
							StringBuffer sb = new StringBuffer();
							while (token.hasMoreElements()) {
								String nextByte = token.nextToken();
								if (nextByte.equals("02")) isQCdata = true;
								sb.append(nextByte);
							}
							//System.out.println(sb.toString());
							if (isQCdata) {
								//sync with QC data
								sb.delete(0, sb.indexOf("02"));
								if (sb.length() > readBuffer.length*2) {
									sb.delete(readBuffer.length*2, sb.length()-1);
								}
								else if (sb.length() < readBuffer.length*2) {
									sb.append(StringHelper.byte2Hex2CharString(this.read(new byte[readBuffer.length - sb.length()/2], 1000), (readBuffer.length - sb.length()/2)));
								}
							}
							byte[] tmpData = StringHelper.convert2ByteArray(sb.toString());
							System.arraycopy(tmpData, 0, readBuffer, 0, tmpData.length <= readBuffer.length ? tmpData.length : readBuffer.length);
							if (log.isLoggable(Level.FINE)) {
								log.logp(java.util.logging.Level.FINER, DeviceSerialPortSimulatorImpl.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(readBuffer));
								log.logp(Level.FINE, DeviceSerialPortSimulatorImpl.$CLASS_NAME, $METHOD_NAME, "  Read : " + StringHelper.byte2Hex2CharString(readBuffer, readBuffer.length));
							}
						}
						else { // WARNING, assume time out
							if (log.isLoggable(Level.TIME)) log.logp(Level.TIME, $CLASS_NAME, "read()", "delay " + timeout_msec);
							WaitTimer.delay(timeout_msec);
							throw new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { "*", timeout_msec })); //$NON-NLS-1$ 
						}
					}
					else {
						this.close();
						throw new EOFException();
					} 
				}
			}
		}
		return readBuffer;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#read(byte[], int, boolean)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, boolean checkFailedQuery) throws IOException, FailedQueryException, TimeOutException {
		byte[] resultBuffer = new byte[0];
		try {
			wait4Bytes(1000);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (this.isConnected) {
			if (this.fileType.equals(GDE.FILE_ENDING_STAR_TXT)) {
				String line;
				if ((line = txt_in.readLine()) != null) {
					while (!line.contains("[<]") && (line = txt_in.readLine()) != null)
						;

					//System.out.println(line);
					if (line != null && line.contains("[<]")) {
						line = line.substring(line.indexOf("[<]") + 19);
						StringTokenizer token = new StringTokenizer(line);
						StringBuffer sb = new StringBuffer();
						while (token.hasMoreElements()) {
							sb.append(token.nextToken());
						}
						//System.out.println(sb.toString());
						resultBuffer = StringHelper.convert2ByteArray(sb.toString());
						System.arraycopy(resultBuffer, 0, readBuffer, 0, resultBuffer.length < readBuffer.length ? resultBuffer.length : readBuffer.length);
					}
					else { // WARNING, assume time out
						if (log.isLoggable(Level.TIME)) log.logp(Level.TIME, $CLASS_NAME, "read()", "delay " + timeout_msec);
						WaitTimer.delay(timeout_msec);
						throw new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { "*", timeout_msec })); //$NON-NLS-1$ 
					}
				}
				else
					this.close();
			}
		}
		return readBuffer;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#read(byte[], int, int)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex) throws IOException, TimeOutException {
		byte[] resultBuffer = new byte[0];
		byte leader = (byte) this.device.getDataBlockLeader().charAt(0);
		byte[] lineEnding = this.device.getDataBlockEnding();
		boolean isNextGen = this.device.getName().startsWith("next");
		try {
			waitForStableReceiveBuffer(readBuffer.length, timeout_msec, 100);
		}
		catch (InterruptedException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		if (this.isConnected) {
			if (data_in != null && this.fileType.equals(GDE.FILE_ENDING_STAR_LOV)) {
				Vector<Byte> tmpVector = new Vector<Byte>();
				byte tmpByte, lastByte = 0x00;
				try {
					boolean isOF = false;
					
					if (isNextGen) {
						while ((tmpByte = data_in.readByte()) != 0xff) {
							if (!isOF) {
								isOF = tmpByte == ':' && (lastByte == '1' || lastByte == '2'); 
							}
							tmpVector.add(tmpByte);
							if (isOF && tmpByte == lineEnding[1] && lastByte == lineEnding[0]) 
								break;
							lastByte = tmpByte;
						}						
					} else {
						while ((tmpByte = data_in.readByte()) != 0xff) {
							if (!isOF)isOF = tmpByte == leader; 
							tmpVector.add(tmpByte);
							if (isOF && tmpByte == lineEnding[1] && lastByte == lineEnding[0]) 
								break;
							lastByte = tmpByte;
						}
					}
					
					int size2Read = this.device.getLovDataByteSize() - Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO));
					byte[] tmpBuffer = new byte[size2Read > 0 ? size2Read : 0];
					if (data_in.read(tmpBuffer) != size2Read) {
						//end of file reached
						this.close();
					}
					else {
						for (byte tmp : tmpBuffer)
							tmpVector.add(tmp);
					}
				}
				catch (Exception e) {
					this.close();
				}

				if (this.device.getDataBlockLeader().length() > 0) {
					if (isNextGen)
						while (tmpVector.size() > 2 && (tmpVector.get(0) != '1' || tmpVector.get(0) != '2') && tmpVector.get(1) != ':')
							tmpVector.remove(0);
					else
						while (tmpVector.size() > 2 && tmpVector.get(0) != leader)
							tmpVector.remove(0);
				}
				if (tmpVector.size() != readBuffer.length) {
					readBuffer = new byte[tmpVector.size()];
					for (int i = 0; i < readBuffer.length; i++) {
						readBuffer[i] = tmpVector.get(i);
					}
				}
				else {
					for (int i = 0; i < readBuffer.length; i++) {
						readBuffer[i] = tmpVector.get(i);
					}
				}
			}
			else if (txt_in != null) {
				if (this.fileType.equals(GDE.FILE_ENDING_STAR_TXT)) {
					String line;
					while ((line = txt_in.readLine()) != null) {
						if (line.length() >= device.getDataBlockSize(FormatTypes.BYTE)) {
							readBuffer = (line+"\r\n").getBytes();
							break;
						}	
						this.close();
					}
				}
				else if (this.fileType.equals(GDE.FILE_ENDING_STAR_LOG)) {
					String line;
					if ((line = txt_in.readLine()) != null) {
						//while(!line.contains("WARNING") && !line.contains("Read  data:") && (line = txt_in.readLine()) != null) ; //trace.log
						while(!line.contains("IRP_MJ_READ") && !line.contains("SUCCESS	Length") && (line = txt_in.readLine()) != null) ; // portmon.log
						
						//System.out.println(line);
						//if(line != null && line.contains("Read  data:")) { //trace.log
						if(line != null && line.contains("IRP_MJ_READ") && line.contains("SUCCESS	Length")) { //portmon.log
							boolean isDataEnd = false;
							String lastByte = "00";
							StringBuffer sb = new StringBuffer();
							while (!isDataEnd && line != null) {
								line = line.substring(line.indexOf("SUCCESS	Length") + 19);
								StringTokenizer token = new StringTokenizer(line);
								while (token.hasMoreElements() && !isDataEnd) {
									String nextByte = token.nextToken();
									if (lastByte.equals("0D") && nextByte.equals("0A")) 
										isDataEnd = true;
									lastByte = nextByte;
									sb.append(nextByte);
								}
								if (!isDataEnd) {
									line = txt_in.readLine();
									while(!line.contains("IRP_MJ_READ") && !line.contains("SUCCESS	Length") && (line = txt_in.readLine()) != null) ; // portmon.log
								}
							}
							//System.out.println(sb.toString());
//							if (isDataEnd) {
//								//sync with QC data
//								sb.delete(0, sb.indexOf("02"));
//								if (sb.length() > readBuffer.length*2) {
//									sb.delete(readBuffer.length*2, sb.length()-1);
//								}
//								else if (sb.length() < readBuffer.length*2) {
//									sb.append(StringHelper.byte2Hex2CharString(this.read(new byte[readBuffer.length - sb.length()/2], 1000), readBuffer.length - sb.length()/2));
//								}
//							}
							System.out.println(StringHelper.byte2CharString(sb.toString().getBytes(), sb.length()));
							readBuffer = StringHelper.convert2ByteArray(sb.toString());
						}
						else { // WARNING, assume time out
							if (log.isLoggable(Level.TIME)) log.logp(Level.TIME, $CLASS_NAME, "read()", "delay " + timeout_msec);
							WaitTimer.delay(timeout_msec);
							throw new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { "*", timeout_msec })); //$NON-NLS-1$ 
						}
					}
					else
						this.close();
				}
			}
		}
		else 
			throw new IOException("Connection closed by EOF"); //$NON-NLS-1$ 
			
		byte[] resultData = resultBuffer.length <= readBuffer.length ? readBuffer : resultBuffer;
		//log.log(Level.OFF, StringHelper.byte2Hex2CharString(resultData, resultData.length));
		return resultData;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#read(byte[], int, java.util.Vector)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, Vector<Long> waitTimes) throws IOException, TimeOutException {
		try {
			wait4Bytes(readBuffer.length, timeout_msec);
		}
		catch (IOException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		if (this.isConnected) {
			if (data_in != null && this.fileType.equals(GDE.FILE_ENDING_STAR_LOV)) {
				if (data_in.read(readBuffer) > 0) {
					int size2Read = this.device.getLovDataByteSize() - Math.abs(this.device.getDataBlockSize(InputTypes.FILE_IO));
					if (data_in.read(new byte[size2Read]) != size2Read) {
						log.log(Level.WARNING, "expected byte size to  read does not macht really red size of bytes !");
					}
				}
				else {
					readBuffer = new byte[0];
					this.close();
				}
			}
			else if (txt_in != null) {
				if (this.fileType.equals(GDE.FILE_ENDING_STAR_TXT)) {
					StringBuffer sb = new StringBuffer();
					int value;

					sb.append('\f');
					while ((value = txt_in.read()) != -1 && value != '\f')
						sb.append((char) value);

					if (sb.length() > 1)
						readBuffer = sb.toString().getBytes();
					else
						this.close();
				}
				else if (this.fileType.equals(GDE.FILE_ENDING_STAR_LOG)) {
					String line;
					if ((line = txt_in.readLine()) != null) {
						while(!line.contains("WARNING") && !line.contains("Read  data:") && (line = txt_in.readLine()) != null) ;
						
						//System.out.println(line);
						if(line != null && line.contains("Read  data:")) {
							boolean isQCdata = false;
							line = line.substring(line.indexOf("Read  data:") + 12);
							StringTokenizer token = new StringTokenizer(line);
							StringBuffer sb = new StringBuffer();
							while (token.hasMoreElements()) {
								String nextByte = token.nextToken();
								if (nextByte.equals("02")) isQCdata = true;
								sb.append(nextByte);
							}
							//System.out.println(sb.toString());
							if (isQCdata) {
								//sync with QC data
								sb.delete(0, sb.indexOf("02"));
								if (sb.length() > readBuffer.length*2) {
									sb.delete(readBuffer.length*2, sb.length()-1);
								}
								else if (sb.length() < readBuffer.length*2) {
									sb.append(StringHelper.byte2Hex2CharString(this.read(new byte[readBuffer.length - sb.length()/2], 1000), readBuffer.length - sb.length()/2));
								}
							}
							readBuffer = StringHelper.convert2ByteArray(sb.toString());
						}
						else { // WARNING, assume time out
							if (log.isLoggable(Level.TIME)) log.logp(Level.TIME, $CLASS_NAME, "read()", "delay " + timeout_msec);
							WaitTimer.delay(timeout_msec);
							throw new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { "*", timeout_msec })); //$NON-NLS-1$ 
						}
					}
					else
						this.close();
				}
			}
		}
		return readBuffer;
	}

	/**
	 * write bytes to serial port output stream, cleans receive buffer if available byes prior to send data 
	 * @param writeBuffer writes size of writeBuffer to output stream
	 * @throws IOException
	 */
	public void write(byte[] writeBuffer) throws IOException {
		//log.log(Level.WARNING, "write() not supported in simulation");
	}

	/**
	 * write bytes to serial port output stream, each byte individual with the given time gap in msec
	 * cleans receive buffer if available byes prior to send data 
	 * @param writeBuffer writes size of writeBuffer to output stream
	 * @throws IOException
	 */
	public synchronized void write(byte[] writeBuffer, long gap_ms) throws IOException {
		log.log(Level.WARNING, "write() not supported in simulation");
	}

	/**
	 * cleanup the input stream if there are bytes available
	 * @return number of bytes in receive buffer which get removed
	 * @throws IOException
	 */
	public int cleanInputStream() throws IOException {
		log.log(Level.WARNING, "cleanInputStream() not supported in simulation");
		return 0;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#wait4Bytes(int)
	 */
	public long wait4Bytes(int timeout_msec) throws InterruptedException, TimeOutException, IOException {
		WaitTimer.delay(getWaitTime());
		return 0;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#wait4Bytes(int, int)
	 */
	public int wait4Bytes(int numBytes, int timeout_msec) throws IOException {
		WaitTimer.delay(getWaitTime());
		return numBytes;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#waitForStableReceiveBuffer(int, int, int)
	 */
	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex) throws InterruptedException, TimeOutException, IOException {
		WaitTimer.delay(getWaitTime());
		return expectedBytes;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#waitForStableReceiveBuffer(int, int, int)
	 */
	public int getAvailableBytes() throws IOException {
		return this.isConnected ? 20 : 0;
	}

	/**
	 * query if the port is already open
	 * @return
	 */
	public boolean isConnected() {
		return this.isConnected;
	}

	/**
	 * @return number of transfer errors occur
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
	 * @return number of timeout errors occur
	 */
	public int getTimeoutErrors() {
		return this.timeoutErrors;
	}

	/**
	 * add up timeout errors
	 */
	public void addTimeoutError() {
		this.timeoutErrors++;
	}

	/**
	 * check if a configured serial port string matches actual available ports
	 * @param newSerialPortStr
	 * @return true if given port string matches one of the available once
	 */
	public boolean isMatchAvailablePorts(String newSerialPortStr) {
		return true;
	}

	/**
	 * @return calculated sleep time in milli seconds
	 */
	int getWaitTime() {
		int sleepTime = 0;
		Random randomGenerator = new Random(new Date().getTime());
    if (this.isTimeStepConstant) {
    	sleepTime = this.sleepTime_ms;
    }
    else {
			while (sleepTime < this.sleepTime_ms/2 || sleepTime > this.sleepTime_ms*2) {
				sleepTime = randomGenerator.nextInt(this.sleepTime_ms);
			}
		}
		log.log(Level.TIME, "sleepTime : " + sleepTime + " ms");
		return sleepTime;
	}

	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex, int minCountBytes) throws IOException, TimeOutException {
		return null;
	}

	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex, int minCount) throws InterruptedException, TimeOutException, IOException {
		return 0;
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
		try {
			return (UsbInterface) this.open();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
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
		try {
			if (this.application != null) {
				GDE.display.syncExec(new Runnable() {
					public void run() {
						String path;
						if (application.isObjectoriented()) {
							path = application.getObjectFilePath();
						}
						else {
							String devicePath = application.getActiveDevice() != null ? GDE.STRING_FILE_SEPARATOR_UNIX + application.getActiveDevice().getName() : GDE.STRING_EMPTY;
							path = application.getActiveDevice() != null ? settings.getDataFilePath() + devicePath + GDE.STRING_FILE_SEPARATOR_UNIX : settings.getDataFilePath();
							if (!FileUtils.checkDirectoryAndCreate(path)) {
								if (!FileUtils.checkDirectoryExist(path)) 
									application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0056, new Object[] { path }));
								else
									application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0012, new Object[] { path }));
							}
						}
						FileDialog openFileDialog = application.openFileOpenDialog("Open File used as simulation input", new String[] { GDE.FILE_ENDING_STAR_LOV, GDE.FILE_ENDING_STAR_TXT,
								GDE.FILE_ENDING_STAR_LOG }, path, null, SWT.SINGLE);
						if (openFileDialog.getFileName().length() > 4) {
							String openFilePath = (openFileDialog.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + openFileDialog.getFileName()).replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);

							try {
								if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_OSD)) {
									fileType = GDE.FILE_ENDING_STAR_OSD;
									//add implementation to use *.osd files as simulation data input
								}
								else if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_LOV)) {
									fileType = GDE.FILE_ENDING_STAR_LOV;
									data_in = new DataInputStream(new FileInputStream(new File(openFilePath)));
									LogViewReader.readHeader(data_in);
								}
								else if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_TXT)) {
									fileType = GDE.FILE_ENDING_STAR_TXT;
									txt_in = new BufferedReader(new InputStreamReader(new FileInputStream(openFilePath), "ISO-8859-1")); //$NON-NLS-1$
									txt_in.read();
								}
								else if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
									fileType = GDE.FILE_ENDING_STAR_LOG;
									txt_in = new BufferedReader(new InputStreamReader(new FileInputStream(openFilePath), "ISO-8859-1")); //$NON-NLS-1$
								}
								else
									application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0008) + openFilePath);
							}
							catch (Exception e) {
								log.log(Level.SEVERE, e.getMessage(), e);
							}
						}
						isConnected = data_in != null || txt_in != null;
						application.setPortConnected(isConnected);
					}
				});
			}
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}

	/**
	 * release or close the given interface
	 * @param usbInterface
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public void closeUsbPort(final UsbInterface usbInterface) throws UsbClaimException, UsbException {
		this.close();
	}

	/**
	 * release or close the given lib usb handle
	 * @param libUsbDeviceHanlde
	 * @param cacheSelectedUsbDevice true| false
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public void closeLibUsbPort(final DeviceHandle libUsbDeviceHanlde, boolean cacheSelectedUsbDevice) throws LibUsbException, UsbException {
		this.close();
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
	public int read(final UsbInterface iface, final byte endpointAddress, byte[] data) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException {
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
		try {
			return (read(data, timeout_msec)).length;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
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
   * @return The number of bytes red
   * @throws IllegalStateException while handle not initialized
   * @throws TimeOutException while data transmission failed
   */
  public int read(final DeviceHandle handle, final byte inEndpoint, final byte[] data, final long timeout_ms) throws IllegalStateException, TimeOutException {
		try {
			wait4Bytes((int) timeout_ms);
		}
		catch (InterruptedException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		catch (TimeOutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte[] readBuffer = new byte[data.length];
		if (this.isConnected) {
			try {
				if (data_in != null && this.fileType.equals(GDE.FILE_ENDING_STAR_LOV)) {
					if (data_in.read(readBuffer) > 0) {
						int size2Read = this.device.getLovDataByteSize() - Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO));
						if (data_in.read(new byte[size2Read]) != size2Read) {
							log.log(Level.WARNING, "expected byte size to  read does not macht really red size of bytes !");
						}
						if (this.device.getName().toLowerCase().contains("icharger")) {
							byte[] tmpBuffer = new byte[ Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO))];
							System.arraycopy(readBuffer, 5, tmpBuffer, 0, tmpBuffer.length-5);
							System.arraycopy(tmpBuffer, 0, readBuffer, 0, tmpBuffer.length);	
							int timeStamp = DataParser.parse2Int(readBuffer, 3);
							//System.out.println("timeStamp = " + timeStamp);
							if (timeStamp % 5000 == 0) {
								System.out.println(StringHelper.byte2Hex2CharString(readBuffer));
								System.out.println("inject IR data");
								byte[] irBuffer = new byte[64 - 7];
								irBuffer[7 - 7] = (byte) 0x80;
								irBuffer[8 - 7] = (byte) 0xF4; //pack  ri
								irBuffer[9 - 7] = 0x01; //pack  ri
								irBuffer[10 - 7] = 0x65;//cell1 ri
								irBuffer[12 - 7] = 0x64;//cell2 ri
								irBuffer[14 - 7] = 0x63;//cell3 ri
								irBuffer[16 - 7] = 0x63;//cell4 ri
								irBuffer[18 - 7] = 0x65;//cell5 ri
								System.arraycopy(irBuffer, 0, readBuffer, 7, 64 - 7);
								System.out.println(StringHelper.byte2Hex2CharString(readBuffer));
							}
							//2C 10 01 90D00300 01 01 00 C7006C86394C850000008A0100003C0F3A0F3B0F3C0F330F000000000000000000000000000000000000000000000000000000000000
							//2C 10 01 90D00300 80 3200 0A00 0900 0A00 0A00 0B00 0000 000000000000000000000000000000000000000000000000000000000000000000000000000000000
						}
					}
					else {
						readBuffer = new byte[0];
						this.close();
					}
				}
			} catch (Exception e) {
				
			}
		}
		System.arraycopy(readBuffer, 0, data, 0, data.length);
		return readBuffer.length;
	}

}
