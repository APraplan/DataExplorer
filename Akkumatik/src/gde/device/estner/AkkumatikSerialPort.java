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
package gde.device.estner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * PowerLab8 serial port implementation
 * @author Winfried Brügmann
 */
public class AkkumatikSerialPort extends DeviceCommPort {
	final static String	$CLASS_NAME		= AkkumatikSerialPort.class.getName();
	final static Logger	log						= Logger.getLogger(AkkumatikSerialPort.$CLASS_NAME);

	final int						timeout;
	final int						stableIndex;
	final int						maxRetryCount	= 25;
	int									retryCount;

	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public AkkumatikSerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
		this.timeout = this.device.getDeviceConfiguration().getReadTimeOut();
		this.stableIndex = this.device.getDeviceConfiguration().getReadStableIndex();
		this.retryCount = 0;
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * Checking Charger Status:
	 * 1. Send Ram0 to request a status packet
	 * 2. Verify the CRC checksum to confirm the received packet is valid.
	 * 3. Gather the following important information from the packet (done in gatherer thread)
	 * 	- Cell Voltages
	 * 	- Mode
	 * 	- Preset Number
	 * 	- Charge/Discharge Complete (from Status Flags)
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData";
		byte[] data = new byte[Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO))];

		try {
			data = this.read(data, this.timeout, this.stableIndex);

			if (AkkumatikSerialPort.log.isLoggable(java.util.logging.Level.FINE)) {
				AkkumatikSerialPort.log.logp(java.util.logging.Level.FINER, AkkumatikSerialPort.$CLASS_NAME, $METHOD_NAME, "0123456789|123456789|123456789|123456789|123456789|123456789|123456789|123456789");
				AkkumatikSerialPort.log.logp(java.util.logging.Level.FINE, AkkumatikSerialPort.$CLASS_NAME, $METHOD_NAME, new String(data));
			}
			if ((data.length < 69 || data.length > 129) || data[4] != 58 || data[7] != 58
					|| !(data[0] == 49 || data[0] == 50) || data[1] != -1 || data[data.length - 1] != 0x0A || data[data.length - 2] != 0x0D) {
				if (data.length != 5) { //"A126 "
					AkkumatikSerialPort.log.logp(java.util.logging.Level.WARNING, AkkumatikSerialPort.$CLASS_NAME, $METHOD_NAME, "Serial comm error, data = " + new String(data));
					++this.retryCount;
					if (this.retryCount > this.maxRetryCount) {
						final String msg = "Errors during serial communication, maximum of retries exceeded!";
						this.retryCount = 0;
						AkkumatikSerialPort.log.logp(java.util.logging.Level.WARNING, AkkumatikSerialPort.$CLASS_NAME, $METHOD_NAME, msg);
					}
					this.cleanInputStream();
				}
				else {
					AkkumatikSerialPort.log.logp(java.util.logging.Level.INFO, AkkumatikSerialPort.$CLASS_NAME, $METHOD_NAME, "Firmware '" + new String(data) + "'");
				}
				data = getData();
			}
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				AkkumatikSerialPort.log.logp(java.util.logging.Level.SEVERE, AkkumatikSerialPort.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		this.retryCount = 0;
		return data;
	}

	/**
	 * converts the data byte buffer in a String array with mostly readable content
	 * @param buffer
	 * @return
	 */
	public String[] getDataArray(byte[] buffer) {
		if (log.isLoggable(java.util.logging.Level.FINE)) {
			log.log(java.util.logging.Level.FINE, "  Read : " + StringHelper.byte2Hex2CharString(buffer, buffer.length));
			log.log(java.util.logging.Level.FINE, "0123456789|123456789|123456789|123456789|123456789|123456789|123456789|123456789");
			log.log(java.util.logging.Level.FINE, "splitter = " + new String(new byte[] {(byte) 0xFF}) + " - " + new String(buffer));
		}
		String[] dataArray = new String(buffer).split(new String(new byte[] {(byte) 0xFF}));
		if (dataArray.length < 15) {
			log.log(java.util.logging.Level.SEVERE, "array size = " + dataArray.length + " splitter = " + new String(new byte[] {(byte) 0xFF}) + " - " + new String(buffer));
			log.log(java.util.logging.Level.SEVERE, "splitter = " + new String(new byte[] {(byte) 0xFF}) + " - " + new String(buffer));
		}
		if (dataArray.length < 15) {
			for (int i = 0; i<buffer.length; ++i) {
				if (buffer[i] == 0xFF) buffer[i] = 0x21;
			}
			log.log(java.util.logging.Level.WARNING, "array size = " + dataArray.length + " splitter = " + new String(new byte[] {(byte) 0x21}) + " - " + new String(buffer));
			dataArray = new String(buffer).split(new String(new byte[] {(byte) 0x21}));
		}
		return dataArray;
	}
	
	/**
	 * @param cmd values as space separated string
	 * @return byte array following UM-Akkumatik specification
	 */
  public static byte[] getBytes2Write(String cmd) {
//  <Channel>1</Channel>
//  <AccuTyp>0</AccuTyp>
//  <CurrentMode>2</CurrentMode>
//  <Amount>0</Amount>
//  <Capacity>1000</Capacity>
//  <CellCount>34</CellCount>
//  <Program>2</Program>
//  <Cycle>0</Cycle>
//  <ChargeMode>0</ChargeMode>
//  <ChargeStopMode>0</ChargeStopMode>
//  <ChargeCurrent>100</ChargeCurrent>
//  <DisChargeCurrent>100</DisChargeCurrent>
  	log.log(Level.INFO, "<STX> " + cmd + " <CS> <ETX>");
  	List<Byte> bytes2Write = new ArrayList<>();
  	for (String token : cmd.split(GDE.STRING_BLANK))
  		for (Byte b : token.getBytes())
  			bytes2Write.add(b);   	
  	
    bytes2Write.add(0, (byte) 0x02);
  	bytes2Write.add((byte) 0x40); //dummy checksum
  	bytes2Write.add((byte) 0x03);
  	
  	byte[] bytes = new byte[bytes2Write.size()];
  	for (int i = 0; i < bytes2Write.size(); ++i)
  		bytes[i] = bytes2Write.get(i);
  	bytes[bytes.length-2] = AkkumatikSerialPort.getChecksum(bytes);
  	
  	log.log(Level.INFO, StringHelper.byte2Hex2CharString(bytes, bytes.length));
  	return bytes;
  }

	/**
	 * calculate checksum
	 * @param data byte array starting with 0x02 (STX), ends with 0x03 (ETX), checksum is placed at data.length-2
	 * @return checksum byte
	 */
	public static byte getChecksum(byte[] data) {
		byte chksum = 0;
		if (data[0] == 0x02 && data[data.length-1] == 0x03) {
			for (int i=0; i<data.length-2; ++i)
				chksum ^= (data[i] & 0x0F);//xor
			chksum |= 0x40;
		}
		return chksum;
	}
	
	
	public static void main(String[] args) {
		String[] lines = { "310002000203000000003200320000000000", // C   
				"310006000200000000006400640000000100", // E
				"3105020002000400>803>803>80300000100", // E
				"3105020002000400=007=007>80300000100", // E 

				"3105020002000400=107=007>80300000100", // D
				"3105020002000400=207=007>80300000100", // G
				"3105020002000400=307=007>80300000100", // F
				"3105020002000400=407=007>80300000100", // A
				"3105020002000400=507=007>80300000100", // @
				"3105020002000400=607=007>80300000100", // C
				"3105020002000400=707=007>80300000100", // B
				"3105020002000400=807=007>80300000100", // M
				"3105020002000400=907=007>80300000100", // L
				"3105020002000400=:07=007>80300000100", // O
				"3105020002000400=;07=007>80300000100", // N
				"3105020002000400=<07=007>80300000100", // I
				"3105020002000400==07=007>80300000100", // H
				"3105020002000400=>07=007>80300000100", // K
				"3105020002000400=?07=007>80300000100", // J
				"3105020002000400>007=007>80300000100", // F
				"3105020002000400>107=007>80300000100", // G
				"3105020002000400>207=007>80300000100", // D
				"3105020002000400>307=007>80300000100", // E 
				"310006000200000000006400640000000100", // E nicd,0,0,konst,100,ladem,100,sender,1,fest,0
				"3105020002000400>803>803>80300000100", // E li37,1000,4,konst,lmenge,1000,e+l,1,fest,0
				"3105020002000400=007=007>80300000100", // E li37,2000,4,konst,lmenge,2000,e+l,1,fest,0

				"3105020002000400=107=007>80300000100", // D li37,2001,4,konst,lmenge,2000,e+l,1,fest,0
				"3105020002000400=207=007>80300000100", // G li37,2002
				"3105020002000400=307=007>80300000100", // F li37,2003
				"3105020002000400=407=007>80300000100", // A li37,2004
				"3105020002000400=507=007>80300000100", // @ li37,2005
				"3105020002000400=607=007>80300000100", // C li37,2006
				"3105020002000400=707=007>80300000100", // B
				"3105020002000400=807=007>80300000100", // M
				"3105020002000400=907=007>80300000100", // L li37,2009

				"3105020002000400=:07=007>80300000100", // O li37,2010
				"3105020002000400=;07=007>80300000100", // N li37,2011
				"3105020002000400=<07=007>80300000100", // I
				"3105020002000400==07=007>80300000100", // H
				"3105020002000400=>07=007>80300000100", // K
				"3105020002000400=?07=007>80300000100", // J
				"3105020002000400>007=007>80300000100", // F
				"3105020002000400>107=007>80300000100", // G
				"3105020002000400>207=007>80300000100", // D li37,2018
				"3105020002000400>307=007>80300000100", // E li37,2019  was macht diese Zeile zu E?

				"3106020002000400=007=007>80300000100", // F liFe,2000,4,konst,lmenge,2000,e+l,1,fest,0
				"3106020002000400=107=007>80300000100", // G liFe,2001
				"3106020002000400=207=007>80300000100", // D liFe,2002
				"3106020002000400=307=007>80300000100", // E liFe,2003
				"3106020002000400=407=007>80300000100", // B liFe,2004
				"3106020002000400=507=007>80300000100", // C
				"3106020002000400=607=007>80300000100", // @
				"3106020002000400=707=007>80300000100", // A
				"3106020002000400=807=007>80300000100", // N
				"3106020002000400=907=007>80300000100", // O
				"3106020002000400=:07=007>80300000100", // L liFe,2010
				"3106020002000400=;07=007>80300000100", // M
				"3106020002000400=<07=007>80300000100", // J
				"3106020002000400==07=007>80300000100", // K
				"3106020002000400=>07=007>80300000100", // H
				"3106020002000400=?07=007>80300000100", // I
				"3106020002000400>007=007>80300000100", // E
				"3106020002000400>107=007>80300000100", // D
				"3106020002000400>207=007>80300000100", // G
				"3106020002000400>307=007>80300000100", // F liFe,2019

				"3106020002000400;80;=007>80300000100", // D liFe,3000
				"3106020002000400;90;=007>80300000100", // E
				"3106020002000400;:0;=007>80300000100", // F
				"3106020002000400;;0;=007>80300000100", // G
				"3106020002000400;<0;=007>80300000100", // @
				"3106020002000400;=0;=007>80300000100", // A
				"3106020002000400;>0;=007>80300000100", // B
				"3106020002000400;?0;=007>80300000100", // C
				"3106020002000400<00;=007>80300000100", // K
				"3106020002000400<10;=007>80300000100", // J
				"3106020002000400<20;=007>80300000100", // I liFe,3010

				"3101020002030400=007=007>80300000100", // B NiMh,2000
				"3101020002030400=107=007>80300000100", // C
				"3101020002030400=207=007>80300000100", // @
				"3101020002030400=307=007>80300000100", // A
				"3101020002030400=407=007>80300000100", // F
				"3101020002030400=507=007>80300000100", // G
				"3101020002030400=607=007>80300000100", // D
				"3101020002030400=707=007>80300000100", // E
				"3101020002030400=807=007>80300000100", // J
				"3101020002030400=907=007>80300000100", // K
				"3101020002030400=:07=007>80300000100", // H NiMh,2010

				"30", // A FW
				"44", // B Start_1
				"48", // Start_2
				"41", // Stop_1
				"42"  // Stop_2
		};

		AkkumatikDialog.initLogger();

		for (String line : lines) {
			byte[] bytes = getBytes2Write(line);
			bytes[bytes.length-2] = getChecksum(bytes);
			log.log(Level.OFF, new String(bytes));
			log.log(Level.OFF, StringHelper.byte2Hex2CharString(bytes, bytes.length));
			log.log(Level.OFF, String.format("calculated checksum = %c (0x%02X)", bytes[bytes.length-2], bytes[bytes.length-2]));
		}
	}

}
