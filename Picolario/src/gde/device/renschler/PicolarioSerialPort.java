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
package gde.device.renschler;

import gde.comm.DeviceCommPort;
import gde.device.IDevice;
import gde.exception.ReadWriteOutOfSyncException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.Checksum;
import gde.utils.WaitTimer;
import gde.exception.NoSuchPortException;

import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Serial communication implementation class for the Renschler Picolariolog device
 * @author Winfried Brügmann
 */
public class PicolarioSerialPort extends DeviceCommPort {
	final static Logger	log											= Logger.getLogger(PicolarioSerialPort.class.getName());

	boolean							isTransmitFinished			= false;

	// Datentyp Kommando Beschreibung
	final byte					readNumberRecordSets[]	= new byte[] { (byte) 0xAA, (byte) 0xAA };
	final byte					readRecordSets[]				= new byte[] { (byte) 0xAA, (byte) 0x00 };

	/**
	 * PicolarioSerialPort constructor
	 * @param currentDeviceConfig
	 * @param currentApplication
	 * @throws NoSuchPortException
	 */
	public PicolarioSerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
	}

	/**
	 * ask the Picolario about the number of available record sets
	 * @return number of record sets available
	 * @throws Exception 
	 */
	public int readNumberAvailableRecordSets() throws Exception {
		int recordSets = 0;
		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}

			this.write(this.readNumberRecordSets);
			WaitTimer.delay(30);
			this.write(this.readNumberRecordSets);

			byte[] answer = new byte[4];
			answer = this.read(answer, 2000);

			if (answer[0] != this.readNumberRecordSets[0] && answer[2] != this.readNumberRecordSets[0])
				throw new IOException(Messages.getString(MessageIds.GDE_MSGE1201));

			recordSets = (answer[1] & 0xFF);
		}
		catch (Exception e) {
			this.close();
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			if (isPortOpenedByMe) this.close();
		}
		log.log(Level.FINE, "number available record sets = " + recordSets); //$NON-NLS-1$
		return recordSets;
	}

	/**
	 * method to receive full set of Picolario data
	 * @param datagramNumber
	 * @return hash map containing gathered data points (voltage and height in separate vector)
	 * @throws Exception 
	 */
	public Vector<byte[]> getData(int datagramNumber, Picolario picolario) throws Exception {
		Vector<byte[]> dataBuffer = new Vector<byte[]>(100);
		byte[] readBuffer;
		int numberRed = 0;
		byte[] readRecordSetsWithNumber = new byte[] { this.readRecordSets[0], (byte) datagramNumber, this.readRecordSets[0], (byte) datagramNumber };
		
		try {
			this.write(readRecordSetsWithNumber);
			this.isTransmitFinished = false;

			WaitTimer.delay(256); // give picolario time to prepare data

			while (!this.isTransmitFinished) {

				((PicolarioDialog)picolario.getDialog()).setAlreadyRedText(numberRed++);
				readBuffer = new byte[31];
				readBuffer = read(readBuffer, 2000, 100); // throws timeout exception
			
				if (readBuffer.length != 0) {
					
					if (checkGoodPackage(readBuffer, 31, 3)) {
						// append data to data container
						dataBuffer.add(readBuffer);

						//acknowledge request next
						this.write(new byte[] { readBuffer[readBuffer.length - 1], readBuffer[readBuffer.length - 1] });
						// update the dialog
						
					}
					else {
						// write wrong checksum to repeat data package receive cycle
						log.log(Level.WARNING, "write wrong checksum required"); //$NON-NLS-1$
						byte wrongChecksum = readBuffer[readBuffer.length - 1];
						byte[] requestAgain = new byte[] { wrongChecksum, wrongChecksum };
						this.write(requestAgain);
						numberRed--;
					}
					this.isTransmitFinished = checkTransmissionFinished(3000);
				}
				//}
				else {
					this.isTransmitFinished = checkTransmissionFinished(3000);
				}
			} // end while receive loop
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		return dataBuffer;
	}

	/**
	 * function check transmission finished
	 * @return false if there are available bytes
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private boolean checkTransmissionFinished(int timeout_msec) throws InterruptedException, IOException {
		int sleepTime = 8;
		int timeCounter = timeout_msec / sleepTime;
		int availableBytes = 0;

		WaitTimer.delay(sleepTime);
		while (0 == (availableBytes = this.getAvailableBytes()) && timeCounter-- > 0) {
			WaitTimer.delay(sleepTime);
		}
		return availableBytes == 0 || this.isTransmitFinished;
	}

	/**
	 * function check good package, set isGoodPackage throws read/write out of sync exception
	 * @throws ReadWriteOutOfSyncException 
	 */
	private boolean checkGoodPackage(byte[] readBuffer, int maxBytes, int modulo) {
		boolean isGoodPackage = false;
		if (readBuffer.length == maxBytes)
			isGoodPackage = true;
		else if (readBuffer.length < maxBytes && (readBuffer.length - 1) % modulo == 0)
			isGoodPackage = true;
		else
			isGoodPackage = verifyChecksum(readBuffer);

		return isGoodPackage;
	}

	/**
	 * method to check telegram trailing checksum
	 * @param readBuffer byte array
	 * @return boolean value comparing last value of the byte array with the XOR checksum
	 */
	private boolean verifyChecksum(final byte[] readBuffer) {
		return readBuffer[readBuffer.length - 1] == Checksum.XOR(readBuffer);
	}

	/**
	 * @param enabled the isTransmitFinished to set
	 */
	public void setTransmitFinished(boolean enabled) {
		this.isTransmitFinished = enabled;
	}
}
