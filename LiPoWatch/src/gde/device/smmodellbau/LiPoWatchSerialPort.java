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
import gde.exception.CheckSumMissmatchException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.Checksum;
import gde.utils.WaitTimer;

import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;

/**
 * LiPoWatch serial port implementation
 * @author Winfried Brügmann
 */
public class LiPoWatchSerialPort extends DeviceCommPort {
	final static Logger	log	= Logger.getLogger(LiPoWatchSerialPort.class.getName());

	public final static String 		NUMBER_RECORD						= "number_record"; 	//$NON-NLS-1$
	public final static String 		TIME_MILLI_SEC					= "time_ms"; 				//$NON-NLS-1$
	
	final static byte[]		COMMAND_QUERY_STATE			= { 0x54 };		// 'T' query LiPoWatch state
	final static byte[]		COMMAND_RESET						= { 0x72 };		// 'r' reset LiPoWatch to repeat data send (from the begin)
	final static byte[]		COMMAND_READ_DATA				= { 0x6C };		// 'l' LiPoWatch read request, answer is one data set (telegram)
	final static byte[]		COMMAND_REPEAT					= { 0x77 };		// 'w' repeat data transmission, LiPoWatch re-send same data set (telegram)
	final static byte[]		COMMAND_DELETE					= { (byte) 0xC0, 0x04, 0x01, 0x03, 0x08 };
	final static byte[]		COMMAND_QUERY_CONFIG		= { (byte) 0xC0, 0x04, 0x01, 0x01, 0x06 };
	final static byte[]		COMMAND_LIVE_VALUES			= { 0x76 };		// 'v' query LiPoWatch live values
	final static byte[]		COMMAND_START_LOGGING		= { 0x53 };		// 'S' start logging data
	final static byte[]		COMMAND_STOP_LOGGING		= { 0x73 };		// 's' stop logging data
	final static byte[]		COMMAND_BEGIN_XFER			= { (byte) 0xC0 };	// begin data transfer
	final static byte[]		COMMAND_END_XFER				= { 0x45 };					// 'E' end data transfer

	final static byte[]		COMMAND_PREPARE_DELETE			= { 0x78, 0x79, 0x31 };					// "xy1"
	final static byte[]		COMMAND_PREPARE_SET_CONFIG	= { 0x78, 0x79, (byte) 0xA7 };	// "xyz"
	
	final static byte			DATA_STATE_WAITING			= 0x57;		// 'W' LiPoWatch connected, needs some time to organize flash
	final static byte			DATA_STATE_READY				= 0x46;		// 'F' LiPoWatch ready to receive command
	final static byte			DATA_STATE_OK						= 0x6A;		// 'j' operation successful ended

	final static int			DATA_LENGTH_BYTES				= 47;			
	
	boolean 							isLoggingActive 				= false;
	boolean 							isTransmitFinished			= false;
	
	int 									reveiceErrors 					= 0;

	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public LiPoWatchSerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @param dialog to update displays at device dialog like progress bar, ...
	 * @return map containing gathered data - this can individual specified per device
	 * @throws Exception
	 */
	public Vector<Vector<byte[]>> getData(LiPoWatchDialog dialog) throws Exception {
		boolean isPortOpenedByMe = false;
		Vector<Vector<byte[]>> dataCollection = new Vector<Vector<byte[]>>();
		int numberLess4measurements = 0;
		
		byte[] readBuffer;
		
		try {
			log.log(Level.FINE, "start"); //$NON-NLS-1$
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}
			this.reveiceErrors = 0;

			// check data ready for read operation
			if (this.waitDataReady()) {
				// query configuration to have actual values -> get number of entries to calculate percentage and progress bar
				readBuffer = this.readConfiguration();
				int memoryLeft = ((readBuffer[8] & 0xFF) << 24) + ((readBuffer[7] & 0xFF) << 16) + ((readBuffer[6] & 0xFF) << 8) + (readBuffer[5] & 0xFF);
				int memoryUsed = memoryLeft;
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "memoryUsed = " + memoryLeft); //$NON-NLS-1$
				
				// reset data and prepare for read
				this.write(COMMAND_RESET);

				dialog.setReadDataProgressBar(0);
				Vector<byte[]> telegrams = new Vector<byte[]>();
				int numberRecordSet = 1;
				int redCounter = 0;
				int memoryRed = 0;
				int dataLength = 7;
				int dataSetType = 1;
				
				while ((memoryLeft-=(dataLength-7)) > 0) {
					readBuffer = readSingleTelegramm();

					dataLength = (readBuffer[0] & 0x7F); 
					dataSetType = (readBuffer[9] & 0x0F);
					if (dataSetType == 0) { // normal data set type
						
						int time_ms =  (((readBuffer[4] & 0xFF) << 24) + ((readBuffer[3] & 0xFF) << 16) + ((readBuffer[2] & 0xFF) << 8) + (readBuffer[1] & 0xFF));

						// number record set
						if(numberRecordSet == ((readBuffer[10] & 0xFF) + 1)) {
							telegrams.add(readBuffer);
						}
						else {
							//telegrams.size() > 4 min + max + 2 data points
							if (telegrams.size() > 4) {
								dataCollection.add(telegrams); //$NON-NLS-1$
								if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "dataCollection.put = " + numberRecordSet ); //$NON-NLS-1$					
							}
							else 
								++numberLess4measurements;
								
							numberRecordSet = ((readBuffer[10] & 0xFF) + 1);
							telegrams = new Vector<byte[]>();
							telegrams.add(readBuffer);
						}
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "numberRecordSet = " + numberRecordSet + " time_ms = " + time_ms + " memoryLeft = " + memoryLeft); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$						
						
						
						memoryRed+=(dataLength-7);
						++redCounter;

						//"Gelesene Datensätze/Werte: " & Datensatznummer & "/" & Werte_gelesen & " von " & Speichernummer & " (" & CInt(CLng(Werte_gelesen) * 100 / Speichernummer) & "%)" ' & " (" & Fehlersumme & ")"
						if ((redCounter % 5) == 0) dialog.updateDataGatherProgress(memoryRed, numberRecordSet, this.reveiceErrors, numberLess4measurements, memoryUsed);

						if (this.isTransmitFinished) {
							log.log(Level.WARNING, "transmission stopped by user"); //$NON-NLS-1$
							break;
						}
					}
					else { // no data telegram received
						memoryRed+=(dataLength-7);
					}
				}
				if (telegrams.size() > 4) {
					dataCollection.add(telegrams);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "dataCollection.put = " + numberRecordSet ); //$NON-NLS-1$					
				}
				else 
					++numberLess4measurements;
				
				dialog.updateDataGatherProgress(memoryUsed, numberRecordSet, this.reveiceErrors, numberLess4measurements, memoryUsed);
			}
			else
				throw new IOException(Messages.getString(gde.messages.MessageIds.GDE_MSGE0026));
			
			log.log(Level.FINE, "end"); //$NON-NLS-1$
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			if(isPortOpenedByMe) this.close();
			log.log(Level.FINE, "stop"); //$NON-NLS-1$
		}
		return dataCollection;
	}

	/**
	 * read a single telegram to enable live view of measurements
	 * @return raw byte array of received data
	 * @throws Exception
	 */
	public byte[] readSingleTelegramm() throws Exception {
		byte[] tmp1ReadBuffer = new byte[1], tmp2ReadBuffer, readBuffer = new byte[1];
		int length = 0;
		
		try {
			this.write(COMMAND_READ_DATA);
			this.read(tmp1ReadBuffer, 2000);			
			length = (tmp1ReadBuffer[0] & 0x7F);    // höchstes Bit steht für Einstellungen, sonst Daten
			tmp2ReadBuffer = new byte[length-1];
			this.read(tmp2ReadBuffer, 4000);		
			readBuffer = new byte[length];
			readBuffer[0] = tmp1ReadBuffer[0];
			System.arraycopy(tmp2ReadBuffer, 0, readBuffer, 1, length-1);
			
			if (!isChecksumOK(readBuffer)) {
				readBuffer = readRetry(tmp1ReadBuffer);
			}
		}
		catch (Throwable e) {
			readBuffer = readRetry(tmp1ReadBuffer);
		}
		
		return readBuffer;
	}

	/**
	 * read re-try function, will send COMMAND_REPEAT and re-read the last telegram
	 * @param readBuffer
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws CheckSumMissmatchException
	 */
	byte[] readRetry(byte[] tmp1ReadBuffer) throws IOException, TimeOutException, CheckSumMissmatchException {
		byte[] tmp2ReadBuffer;
		byte[] readBuffer;
		int length;
		// give it another try
		++this.reveiceErrors;
		this.write(COMMAND_REPEAT);
		log.log(Level.WARNING, "errors = " + this.reveiceErrors); //$NON-NLS-1$
		this.read(tmp1ReadBuffer, 2000);			
		length = (tmp1ReadBuffer[0] & 0x7F);    // höchstes Bit steht für Einstellungen, sonst Daten
		tmp2ReadBuffer = new byte[length-1];
		this.read(tmp2ReadBuffer, 4000);		
		readBuffer = new byte[length];
		readBuffer[0] = tmp1ReadBuffer[0];
		System.arraycopy(tmp2ReadBuffer, 0, readBuffer, 1, length-1);
		verifyChecksum(readBuffer); // throws exception if checksum miss match
		return readBuffer;
	}
	
	/**
	 * wait while LiPoWatch answers with data by a given retry count, the wait time between retries is 250 ms
	 * @param retrys number time 250 ms is maximum time
	 * @return true, if data can received after the adjusted time period
	 * @throws Exception
	 */
	public boolean wait4LiveData(int retrys) throws Exception {
		boolean isLiveDataAvailable = false;
		if (this.isConnected()) {
			this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));
			while (this.getAvailableBytes() < 10 && retrys-- > 0) {
				this.write(COMMAND_LIVE_VALUES);
				WaitTimer.delay(250);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "retryLimit = " + retrys); //$NON-NLS-1$
			}
			// read data bytes to clear buffer
			byte[] tmp1ReadBuffer = new byte[1];
			this.read(tmp1ReadBuffer, 1000);
			int length = (tmp1ReadBuffer[0] & 0x7F);    // höchstes Bit steht für Einstellungen, sonst Daten
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "length = " + length); //$NON-NLS-1$
			this.read(new byte[(tmp1ReadBuffer[0] & 0x7F)-1], 1000);
			isLiveDataAvailable = true;
			
			this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
		}
		else
			throw new Exception(Messages.getString(gde.messages.MessageIds.GDE_MSGE0031));
		
		return isLiveDataAvailable;
	}
	
	/**
	 * enable live data view, timer loop must gather the data which also handles open/close operations
	 * @return byte array with red data
	 * @throws Exception
	 */
	public byte[] queryLiveData() throws Exception {
		byte[] tmp1ReadBuffer = new byte[1], tmp2ReadBuffer, readBuffer;
		int length = 0;
		
		if (this.isConnected()) {
			try {
				this.write(COMMAND_LIVE_VALUES);
				this.read(tmp1ReadBuffer, 2000);				
				length = (tmp1ReadBuffer[0] & 0x7F);    // höchstes Bit steht für Einstellungen, sonst Daten
				tmp2ReadBuffer = new byte[length-1];
				this.read(tmp2ReadBuffer, 4000);			
				readBuffer = new byte[length];
				readBuffer[0] = tmp1ReadBuffer[0];
				System.arraycopy(tmp2ReadBuffer, 0, readBuffer, 1, length-1);

				// give it another try
				if (!isChecksumOK(readBuffer)) {
					this.write(COMMAND_LIVE_VALUES);
					this.read(tmp1ReadBuffer, 2000);				
					length = (tmp1ReadBuffer[0] & 0x7F);    // höchstes Bit steht für Einstellungen, sonst Daten
					tmp2ReadBuffer = new byte[length-1];
					this.read(tmp2ReadBuffer, 4000);			
					readBuffer = new byte[length];
					readBuffer[0] = tmp1ReadBuffer[0];
					System.arraycopy(tmp2ReadBuffer, 0, readBuffer, 1, length-1);
					verifyChecksum(readBuffer); // throws exception if checksum miss match
				}
			}
			catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
				throw e;
			}
		}
		else
			throw new Exception(Messages.getString(gde.messages.MessageIds.GDE_MSGE0031));

		return readBuffer;
	}
	
	/**
	 * start logging of LiPoWatch, open/close port operations must be handled outside
	 * @return true if logging is enabled
	 * @throws Exception
	 */
	public boolean startLogging() throws Exception {
		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
				checkConnectionStatus();
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
	 * stop logging activity of LiPoWatch, open/close port operations must be handled outside
	 * @return true if logging is disabled
	 * @throws Exception
	 */
	public boolean stopLogging() throws Exception {
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
	 * clears the flash memory of LiPoWatch
	 * @return true for successful operation
	 * @throws Exception
	 */
	public synchronized boolean clearMemory() throws Exception {
		boolean success = false;
		try {
			if (!this.isConnected()) { // port may not used by other
				this.open();
				// check data ready for read operation
				if (this.waitDataReady()) {
					this.write(COMMAND_DELETE);
					byte[] readBuffer = new byte[1];
					readBuffer = this.read(readBuffer, 5000);
					if (readBuffer[0] != DATA_STATE_OK) success = true;

				}
				else
					throw new IOException(Messages.getString(gde.messages.MessageIds.GDE_MSGE0032));
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			this.close();
		}
		return success;
	}
	/**
	 * set LiPoWatch configuration with new values
	 * @param updateBuffer - byte array to be written
	 * @return true | false for state of the operation
	 * @throws Exception
	 */
	public synchronized boolean setConfiguration(byte[] updateBuffer) throws Exception {
		boolean success = false;
		boolean isPortOpenedByMe = false;
		
		try {
			if(!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}
			// check device connected
			if (this.checkConnectionStatus()) {
				// check data ready for read operation
				if (this.checkDataReady()) {

					this.write(updateBuffer);
					byte[] readBuffer = new byte[1];
					readBuffer = this.read(readBuffer, 5000);
					if (readBuffer[0] == DATA_STATE_OK) success = true;
					//this.write(COMMAND_END_XFER);
					
				}
				else
					throw new IOException(Messages.getString(gde.messages.MessageIds.GDE_MSGE0032));
			}
			else
				throw new IOException(Messages.getString(gde.messages.MessageIds.GDE_MSGE0033));
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			if(isPortOpenedByMe)this.close();
		}
		return success;
	}
	
	/**
	 * query the configuration information from LiPoWatch
	 * @return byte array containing the configuration information 
	 * @throws Exception
	 */
	public byte[] readConfiguration() throws Exception {
		byte[] readBuffer = new byte[DATA_LENGTH_BYTES + 2];
		boolean isPortOpenedByMe = false;
		try {
			if(!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}

			// check device connected
			if (this.checkConnectionStatus()) {
				// check data ready for read operation
				if (this.waitDataReady()) {
					
					this.write(COMMAND_QUERY_CONFIG);				
					this.read(readBuffer, 4000);
					verifyChecksum(readBuffer); // valid data set -> set values
					
				}
				else
					throw new IOException(Messages.getString(gde.messages.MessageIds.GDE_MSGE0033));
			}
			else
				throw new IOException(Messages.getString(gde.messages.MessageIds.GDE_MSGE0032));
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			//if (this.isConnected()) this.write(COMMAND_END_XFER);
			if(isPortOpenedByMe) this.close();
		}
		return readBuffer;
	}

	/**
	 * query if LiPoWatch is connected and capable for communication
	 * @return true/false
	 * @throws IOException
	 * @throws TimeOutException 
	 */
	public synchronized boolean checkConnectionStatus() throws IOException, TimeOutException {
		boolean isConnect = false;
		int counter = 50;

		while (!isConnect && counter-- > 0) {
			this.write(COMMAND_QUERY_STATE);
			byte[] buffer = new byte[1];
			WaitTimer.delay(100);
			buffer = this.read(buffer, 2000);
			if (buffer[0] == DATA_STATE_WAITING || buffer[0] == DATA_STATE_READY) {
				isConnect = true;
			}
		}
		return isConnect;
	}

	/**
	 * check if LiPoWatch is capable to send data
	 * @return true if device is ready to gather data telegrams
	 * @throws Exception
	 */
	public boolean checkDataReady() throws Exception {
		boolean isReady = false;
		int counter = 50;

		while (!isReady && counter-- > 0) {
			this.write(COMMAND_QUERY_STATE);
			WaitTimer.delay(100);
			byte[] buffer = new byte[1];
			buffer = this.read(buffer, 2000);
			if (buffer[0] == DATA_STATE_READY) {
				isReady = true;
			}
		}
		
		return isReady;
	}

	/**
	 * loop while writing status request until data state signaled as ready
	 * @return true if LiPoWatch signals data ready for transmission
	 * @throws Exception
	 */
	public boolean waitDataReady() throws Exception {
		boolean isReady = false;
		
		//isReady = this.checkConnectionStatus();
		isReady = this.checkDataReady();

		return isReady;
	}

	/**
	 * verify the check sum
	 * @param readBuffer
	 * @throws CheckSumMissmatchException 
	 */
	private void verifyChecksum(byte[] readBuffer) throws CheckSumMissmatchException {
		int checkSum = 0;
		int checkSumData = 0;
		int length = (readBuffer[0] & 0x7F);
		checkSum = Checksum.ADD(readBuffer, 2) + 1;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "checkSum = " + checkSum); //$NON-NLS-1$
		checkSumData = ((readBuffer[length - 2] & 0xFF) << 8) + (readBuffer[length - 1] & 0xFF);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "checkSumData = " + checkSumData); //$NON-NLS-1$
		
		if (checkSum != checkSumData)
			throw new CheckSumMissmatchException(Messages.getString(gde.messages.MessageIds.GDE_MSGE0034, new Object[] { checkSum, checkSumData } ));
	}
	
	/**
	 * verify the check sum
	 * @param readBuffer
	 * @return true for checksum match
	 * @throws CheckSumMissmatchException 
	 */
	private boolean isChecksumOK(byte[] readBuffer) {
		int checkSum = 0;
		int checkSumLast2Bytes = 0;
		checkSum = Checksum.ADD(readBuffer, 2) + 1;
		checkSumLast2Bytes = ((readBuffer[readBuffer.length - 2] & 0xFF) << 8) + (readBuffer[readBuffer.length - 1] & 0xFF);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "checkSum = " + checkSum + " checkSumLast2Bytes = " + checkSumLast2Bytes); //$NON-NLS-1$ //$NON-NLS-2$

		return (checkSum == checkSumLast2Bytes);
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
