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
package gde.device.smmodellbau;

import gde.comm.DeviceCommPort;
import gde.device.IDevice;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;
import gde.exception.NoSuchPortException;

import java.util.logging.Logger;

/**
 * UniLog serial port implementation class, just copied from Sample projectS
 * @author Winfried Brügmann
 */
public class UniLog2SerialPort extends DeviceCommPort {
	final static Logger					log											= Logger.getLogger(UniLog2SerialPort.class.getName());

	public final static String	NUMBER_RECORD					= "number_record";								//$NON-NLS-1$
	public final static String	TIME_MILLI_SEC				= "time_ms";											//$NON-NLS-1$
	public final static String	A_MODUS_1_2_3					= "aModus_1_2_3";									//$NON-NLS-1$

	final static byte[]					COMMAND_QUERY_STATE		= { 0x74 };												// 't' query UniLog state
	final static byte[]					COMMAND_RESET					= { 0x1B, 0x5B, 0x44 };
	final static byte[]					COMMAND_LIVE_VALUES		= { 0x1B, 0x5B, 0x00, 0x30 };
	final static byte[]					COMMAND_START_LOGGING	= { 0x1B, 0x5B, 0x43 };
	final static byte[]					COMMAND_STOP_LOGGING	= { 0x1B, 0x5B, 0x44 };

	final static byte						DATA_STATE_READY			= 0x47;														// 'G' UniLog ready to receive command

	static int									DATA_LENGTH_BYTES			= 24;
	static int									TIME_OUT_MS						= 2000;

	boolean											isLoggingActive				= false;
	boolean											isTransmitFinished		= false;

	int													reveiceErrors					= 0;

	/**
	 * constructor of default implementation
	 * @param actualDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 * @throws NoSuchPortException
	 */
	public UniLog2SerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
	}
	
	/**
	 * enable live data view, timer loop must gather the data which also handles open/close operations
	 * @return byte array with red data
	 * @throws Exception
	 */
	public synchronized String queryLiveData() throws Exception {
		byte[] readBuffer = new byte[DATA_LENGTH_BYTES];
		byte[] readBufferTmp = new byte[38];
		
		if (this.isConnected()) {
			try {
				this.write(COMMAND_START_LOGGING);
				this.write(COMMAND_LIVE_VALUES);
				this.read(readBufferTmp, UniLog2SerialPort.TIME_OUT_MS);
				System.arraycopy(readBufferTmp, 0, readBuffer, 0, 38);
				this.read(readBufferTmp, UniLog2SerialPort.TIME_OUT_MS);
				System.arraycopy(readBufferTmp, 0, readBuffer, 38, 38);
				this.read(readBufferTmp, UniLog2SerialPort.TIME_OUT_MS);
				System.arraycopy(readBufferTmp, 0, readBuffer, 76, 38);
				this.read(readBufferTmp, UniLog2SerialPort.TIME_OUT_MS);
				System.arraycopy(readBufferTmp, 0, readBuffer, 114, 38);

				//log.log(Level.FINE, StringHelper.byte2Hex2CharString(readBuffer));
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, StringHelper.byte2CharString(readBuffer, readBuffer.length));
			}
			catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
				throw e;
			}
		}
		else
			throw new Exception(Messages.getString(gde.messages.MessageIds.GDE_MSGE0031));

		return StringHelper.byte2CharString(readBuffer, readBuffer.length);
	}
	
	/**
	 * start logging of UniLog, open/close port operations must be handled outside
	 * @return true if logging is enabled
	 * @throws Exception
	 */
	public boolean startLogging() throws Exception {
		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
				WaitTimer.delay(2000);
			}

			this.write(COMMAND_START_LOGGING);
			this.isLoggingActive = true;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.close();
			throw e;
		}
		finally {
			if (isPortOpenedByMe) this.close();
		}
		return this.isLoggingActive;
	}
	
	/**
	 * stop logging activity of UniLog, open/close port operations must be handled outside
	 * @return true if logging is disabled
	 * @throws Exception
	 */
	public synchronized boolean stopLogging() throws Exception {
		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
				//waitDataReady();
			}

			this.write(COMMAND_STOP_LOGGING);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.close();
			throw e;
		}
		finally {
			if (isPortOpenedByMe) this.close();
		}
		return !this.isLoggingActive;
	}

	/**
	 * check if UniLog2 is capable to send data
	 * @return true if device is ready to gather data telegrams
	 * @throws Exception
	 */
	public synchronized boolean checkDataReady() throws Exception {
		boolean isReady = false;
		int counter = 20;

		while (!isReady && counter-- > 0) {
			this.write(COMMAND_QUERY_STATE);
			byte[] buffer = new byte[1];
			buffer = this.read(buffer, UniLog2SerialPort.TIME_OUT_MS * 2);
			if (buffer[0] == DATA_STATE_READY) {
				isReady = true;
			}
		}
		
		if (isReady) {
			byte[] buffer = new byte[50];
			this.read(buffer, UniLog2SerialPort.TIME_OUT_MS * 2);
			this.read(buffer, UniLog2SerialPort.TIME_OUT_MS * 2);
			buffer = new byte[this.waitForStableReceiveBuffer(50, 1000, 50)];
			this.read(buffer, 2000);
			//log.log(Level.FINE, "###" + StringHelper.convert2CharString(buffer));
		}
		
		for (int i = 0; i < 3; i++) {
			this.write(UniLog2SerialPort.COMMAND_RESET);
			byte[] buffer = new byte[50];
			this.read(buffer, UniLog2SerialPort.TIME_OUT_MS * 2);
			this.read(buffer, UniLog2SerialPort.TIME_OUT_MS * 2);
			buffer = new byte[this.waitForStableReceiveBuffer(50, 1000, 50)];
			this.read(buffer, UniLog2SerialPort.TIME_OUT_MS * 2);
			//log.log(Level.FINE, "***" + StringHelper.convert2CharString(buffer));
		}
		
//		this.write(UniLog2SerialPort.COMMAND_START_LOGGING);
//		byte[] buffer = new byte[40];
//		this.read(buffer, 1000);
//		this.read(buffer, 1000);
//		this.read(buffer, 1000);
//		buffer = new byte[1];
//		Vector<Byte> bytes = new Vector<Byte>();
//		while (true) {
//			this.read(buffer, 250);
//			bytes.add(buffer[0]);
//			if (bytes.size() > 10 && bytes.get(bytes.size()-3) == 0x0D && bytes.get(bytes.size()-2) == 0x0A && buffer[0] == 0x00) 
//				break;
//		}
//		DATA_LENGTH_BYTES = 120 + bytes.size() - 4;
//		log.log(Level.FINE, "datasize = " + DATA_LENGTH_BYTES);
			
		DATA_LENGTH_BYTES = 152;
		return isReady;
	}

	/**
	 * @param isFinished the isTransmitFinished to set, used within getData only
	 */
	public void setTransmitFinished(boolean isFinished) {
		this.isTransmitFinished = isFinished;
	}

	/**
	 * @return the isTransmitFinished, used within getData only
	 */
	public boolean isTransmitFinished() {
		return this.isTransmitFinished;
	}
}
