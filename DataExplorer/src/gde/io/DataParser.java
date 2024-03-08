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
package gde.io;

import gde.GDE;
import gde.device.CheckSumTypes;
import gde.device.FormatTypes;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.Checksum;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to parse comma separated input line from a comma separated textual line which simulates serial data 
 * one data line consist of $1;1;0; 14780;  598;  1000;  8838;.....;0002;
 * where $recordSetNumber; stateNumber; timeStepSeconds; firstIntValue; secondIntValue; .....;checkSumIntValue;
 * All properties around the textual data in this line has to be specified in DataBlockType (type=TEXT, size number of values, separator=;, ...), refer to DeviceProperties_XY.XSD
 * @author Winfried Brügmann
 */
public class DataParser extends NMEAParser implements IDataParser {
	static Logger			log										= Logger.getLogger(DataParser.class.getName());

	protected int								start_time_ms					= Integer.MIN_VALUE;
	protected int								valueSize;

	protected final int					timeFactor;
	protected final FormatTypes	checkSumFormatType;
	protected final FormatTypes	dataFormatType;
	protected final boolean			isMultiply1000;
	protected final boolean			isRedirectChannel1;

	/**
	 * constructor to initialize required configuration parameter
	 * assuming checkSumFormatType == FormatTypes.TEXT to checksum is build using contained values
	 * dataFormatType == FormatTypes.TEXT where dataBlockSize specifies the number of contained values (file input)
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType if null, no checksum calculation will occur
	 * @param useDataSize
	 */
	public DataParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, int useDataSize) {
		super(useLeaderChar, useSeparator, useCheckSumType, useDataSize, DataExplorer.getInstance().getActiveDevice(), DataExplorer.getInstance().getActiveChannelNumber(), (short) 0);
		this.timeFactor = useTimeFactor;
		this.checkSumFormatType = FormatTypes.BINARY; //checksum is build using contained values
		this.dataFormatType = FormatTypes.VALUE; //dataBlockSize specifies the number of contained values
		this.isMultiply1000 = true;
		this.isRedirectChannel1 = false;
	}

	/**
	 * constructor to initialize required configuration parameter
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType 
	 * @param useCheckSumFormatType if null, no checksum calculation will occur
	 * @param useDataSize
	 * @param useDataFormatType FormatTypes.TEXT where dataBlockSize specifies the number of contained values, FormatTypes.BINARY where dataBlockSize specifies the number of bytes received via seral connection
	 * @param doMultiply1000 some transferred values are already in a format required to enable 3 digits 
	 */
	public DataParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, FormatTypes useCheckSumFormatType, int useDataSize, FormatTypes useDataFormatType,
			boolean doMultiply1000) {
		super(useLeaderChar, useSeparator, useCheckSumType, useDataSize, DataExplorer.getInstance().getActiveDevice(), DataExplorer.getInstance().getActiveChannelNumber(), (short) 0);
		this.timeFactor = useTimeFactor;
		this.checkSumFormatType = useCheckSumFormatType;
		this.dataFormatType = useDataFormatType; //dataBlockSize specifies the number of contained values if FormatTypes.TEXT else FormatTypes.BINARY the contained byte size
		this.isMultiply1000 = doMultiply1000;
		this.isRedirectChannel1 = false;
	}

	/**
	 * @return true if all $2, $3 should be redirected to channel 1
	 */
	@Override
	public boolean isRedirectChannel1() {
		return this.isRedirectChannel1;
	}

	/**
	 * constructor to initialize required configuration parameter
	 * assuming checkSumFormatType == FormatTypes.TEXT to checksum is build using contained values
	 * dataFormatType == FormatTypes.TEXT where dataBlockSize specifies the number of contained values (file input)
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType if null, no checksum calculation will occur
	 * @param useDataSize
	 */
	public DataParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, int useDataSize, boolean redirect2Channel1) {
		super(useLeaderChar, useSeparator, useCheckSumType, useDataSize, DataExplorer.getInstance().getActiveDevice(), DataExplorer.getInstance().getActiveChannelNumber(), (short) 0);
		this.timeFactor = useTimeFactor;
		this.checkSumFormatType = FormatTypes.BINARY; //checksum is build using contained values
		this.dataFormatType = FormatTypes.VALUE; //dataBlockSize specifies the number of contained values
		this.isMultiply1000 = true;
		this.isRedirectChannel1 = redirect2Channel1;
	}

	@Override
	public void parse(String inputLine, int lineNum) throws DevicePropertiesInconsistenceException, Exception {
		try {
			String[] strValues = inputLine.split(this.separator); // {$1, 1, 0, 14780, 0,598, 1,000, 8,838, 22}
			try {
				this.channelConfigNumber = this.isRedirectChannel1 ? 1 : Integer.parseInt(strValues[0].substring(1).trim());
				this.valueSize = this.dataFormatType != null && this.dataFormatType == FormatTypes.BINARY 
						? strValues.length - 4 
						: this.dataFormatType != null && this.dataFormatType == FormatTypes.VALUE	&& this.dataBlockSize != 0 
							? Math.abs(this.dataBlockSize) > this.device.getNumberOfMeasurements(this.channelConfigNumber) 
									? this.device.getNumberOfMeasurements(this.channelConfigNumber)
									: Math.abs(this.dataBlockSize)
							: strValues.length - 4;
				//enable iCharger Ri values
				if (!device.getName().startsWith("iCharger")) this.values = new int[this.valueSize];
				DataParser.log.log(Level.FINER, "parser inputLine = " + inputLine); //$NON-NLS-1$

				parse(inputLine, strValues);
			}
			catch (RuntimeException e) {
				//Multiplex FlightRecorder
				super.parse(inputLine, lineNum);
			}
		}
		catch (NumberFormatException e) {
			DataParser.log.log(Level.WARNING, e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * default parse method for $1, 1, 0, 14780, 0,598, 1,000, 8,838, 22 like lines
	 * @param inputLine
	 * @param strValues
	 * @throws DevicePropertiesInconsistenceException
	 */
	@Override
	public void parse(String inputLine, String[] strValues) throws DevicePropertiesInconsistenceException {
		String strValue = strValues[0].trim().substring(1);
		this.channelConfigNumber = isRedirectChannel1 ? 1 : Integer.parseInt(strValue);

		strValue = strValues[1].trim();
		this.state = Integer.parseInt(strValue);

		strValue = strValues[2].trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT);
		strValue = strValue.length() > 0 ? strValue : "0";
		if (this.start_time_ms == Integer.MIN_VALUE) {
			this.start_time_ms = (int) (Double.parseDouble(strValue) * this.timeFactor); // Seconds * 1000 = msec
		}
		else {
			if (this.deviceName.startsWith("JLog") && this.time_ms >= 3276700) {
				//JLog2 workaround maximum logging time of 54 minutes
				this.time_ms = this.time_ms + 100;
			}
			else
				this.time_ms = (int) (Double.parseDouble(strValue) * this.timeFactor) - this.start_time_ms; // Seconds * 1000 = msec			
		}

		for (int i = 0; i < this.valueSize && i < strValues.length - 4; i++) {
			strValue = strValues[i + 3].trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT);
			try {
				double tmpValue = strValue.length() > 0 ? Double.parseDouble(strValue) : 0.0;
				if (this.isMultiply1000 && tmpValue < Integer.MAX_VALUE / 1000 && tmpValue > Integer.MIN_VALUE / 1000)
					this.values[i] = (int) (tmpValue * 1000); // enable 3 positions after decimal place
				else // needs special processing within IDevice.translateValue(), IDevice.reverseTranslateValue()
				if (tmpValue < Integer.MAX_VALUE || tmpValue > Integer.MIN_VALUE) {
					this.values[i] = (int) tmpValue;
				}
				else {
					this.values[i] = (int) (tmpValue / 1000);
				}
			}
			catch (NumberFormatException e) {
				this.values[i] = 0;
			}
		}

		//check time reset to force a new data set creation
		if (this.device.getTimeStep_ms() < 0 && this.time_ms <= 0 && this.isTimeResetEnabled) {
			this.recordSetNumberOffset += ++this.timeResetCounter;
			this.isTimeResetEnabled = false;
		}

		if (this.checkSumType != null) {
			if (!isChecksumOK(inputLine, Integer.parseInt(strValues[strValues.length - 1].trim(), 16))) {
				DevicePropertiesInconsistenceException e = new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0049, new Object[] { strValues[strValues.length - 1].trim(), String.format("%X", calcChecksum(inputLine)) }));
				DataParser.log.log(Level.WARNING, e.getMessage(), e);
				throw e;
			}
		}
	}

	/**
	 * check the checksum with configured checksum algorithm
	 * @param inputLine
	 * @param tmpCheckSum
	 * @return
	 */
	public boolean isChecksumOK(String inputLine, int tmpCheckSum) {
		return tmpCheckSum == calcChecksum(inputLine);
	}

	/**
	 * calculate the checksum with configured checksum algorithm
	 * @param inputLine
	 * @return checksum
	 */
	public int calcChecksum(String inputLine) {
		int checksum = 0;
		switch (this.checkSumType) {
		case ADD:
			switch (this.checkSumFormatType) {
			case VALUE:
				checksum = Checksum.ADD(this.values, 0, this.valueSize);
				break;
			case BINARY:
			default:
				checksum = Checksum.ADD(inputLine.substring(0, inputLine.lastIndexOf(this.separator) + 1).getBytes());
				break;
			}
			break;
		case XOR:
			switch (this.checkSumFormatType) {
			case VALUE:
				checksum = Checksum.XOR(this.values, 0, this.valueSize);
				break;
			case BINARY:
			default:
				checksum = Checksum.XOR(inputLine.substring(0, inputLine.lastIndexOf(this.separator) + 1).getBytes());
				break;
			}
			break;
		case OR:
			switch (this.checkSumFormatType) {
			case VALUE:
				checksum = Checksum.OR(this.values, 0, this.valueSize);
				break;
			case BINARY:
			default:
				checksum = Checksum.OR(inputLine.substring(0, inputLine.lastIndexOf(this.separator) + 1).getBytes());
				break;
			}
			break;
		case AND:
			switch (this.checkSumFormatType) {
			case VALUE:
				checksum = Checksum.AND(this.values, 0, this.valueSize);
				break;
			case BINARY:
			default:
				checksum = Checksum.AND(inputLine.substring(0, inputLine.lastIndexOf(this.separator) + 1).getBytes());
				break;
			}
			break;
		}
		return checksum;
	}
	
	/*
	Copyright 2007 Creare Inc.

	Licensed under the Apache License, Version 2.0 (the "License"); 
	you may not use this file except in compliance with the License. 
	You may obtain a copy of the License at 

	https://www.apache.org/licenses/LICENSE-2.0 

	Unless required by applicable law or agreed to in writing, software 
	distributed under the License is distributed on an "AS IS" BASIS, 
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
	See the License for the specific language governing permissions and 
	limitations under the License.
  // byte2Double method - extracts doubles from byte array
	*/
  public static final double[] byte2Double(byte[] inData, boolean byteSwap) {
    int j = 0, upper, lower;
    int length = inData.length / 8;
    double[] outData = new double[length];
    if (!byteSwap)
      for (int i = 0; i < length; i++) {
        j = i * 8;
        upper = (((inData[j] & 0xff) << 24)
            + ((inData[j + 1] & 0xff) << 16)
            + ((inData[j + 2] & 0xff) << 8) + ((inData[j + 3] & 0xff) << 0));
        lower = (((inData[j + 4] & 0xff) << 24)
            + ((inData[j + 5] & 0xff) << 16)
            + ((inData[j + 6] & 0xff) << 8) + ((inData[j + 7] & 0xff) << 0));
        outData[i] = Double.longBitsToDouble((((long) upper) << 32)
            + (lower & 0xffffffffl));
      }
    else
      for (int i = 0; i < length; i++) {
        j = i * 8;
        upper = (((inData[j + 7] & 0xff) << 24)
            + ((inData[j + 6] & 0xff) << 16)
            + ((inData[j + 5] & 0xff) << 8) + ((inData[j + 4] & 0xff) << 0));
        lower = (((inData[j + 3] & 0xff) << 24)
            + ((inData[j + 2] & 0xff) << 16)
            + ((inData[j + 1] & 0xff) << 8) + ((inData[j] & 0xff) << 0));
        outData[i] = Double.longBitsToDouble((((long) upper) << 32)
            + (lower & 0xffffffffl));
      }

    return outData;
  }


  public static long getLong(byte[] array, int offset) {
    return
      ((long)(array[offset]   & 0xff) << 56) |
      ((long)(array[offset+1] & 0xff) << 48) |
      ((long)(array[offset+2] & 0xff) << 40) |
      ((long)(array[offset+3] & 0xff) << 32) |
      ((long)(array[offset+4] & 0xff) << 24) |
      ((long)(array[offset+5] & 0xff) << 16) |
      ((long)(array[offset+6] & 0xff) << 8) |
      ((array[offset+7] & 0xff));
  }


	/**
	 * parse 8 byte of a data buffer to integer value
	 * @param buffer
	 * @param startIndex index of low byte
	 */
	public static long parse2Long(byte[] buffer, int startIndex) {
		return (((long)(buffer[startIndex + 7] & 0xff) << 56) 
				| ((long)(buffer[startIndex + 6] & 0xff) << 48) 
				| ((long)(buffer[startIndex + 5] & 0xff) << 40) 
				| ((long)(buffer[startIndex + 4] & 0xff) << 32) 
				|	((long)(buffer[startIndex + 3] & 0xff) << 24) 
				| ((long)(buffer[startIndex + 2] & 0xff) << 16) 
				| ((long)(buffer[startIndex + 1] & 0xff) << 8) 
				| (buffer[startIndex] & 0xff));
	}
	
	public static long getUInt32(byte[] buffer, int startIndex) {
    long value = buffer[0+startIndex] & 0xFF;
    value |= (buffer[1+startIndex] << 8) & 0xFFFF;
    value |= (buffer[2+startIndex] << 16) & 0xFFFFFF;
    value |= (buffer[3+startIndex] << 24) & 0xFFFFFFFF;
    return value;
}

	public static int getInt(byte[] array, int offset) {
    return
      ((array[offset]   & 0xff) << 24) |
      ((array[offset+1] & 0xff) << 16) |
      ((array[offset+2] & 0xff) << 8) |
       (array[offset+3] & 0xff);
  }
	
	/**
	 * parse 4 byte of a data buffer to integer value
	 * @param buffer
	 * @param startIndex index of low byte
	 */
	public static int parse2Int(byte[] buffer, int startIndex) {
		return (((buffer[startIndex + 3] & 0xff) << 24) | ((buffer[startIndex + 2] & 0xff) << 16) | ((buffer[startIndex + 1] & 0xff) << 8) | (buffer[startIndex] & 0xff));
	}

	/**
	 * parse 2 byte of a data buffer to short integer value, buffer byte sequence low byte high byte
	 * @param buffer
	 * @param startIndex index of low byte 
	 */
	public static short parse2Short(byte[] buffer, int startIndex) {
		return (short) (((buffer[startIndex + 1] & 0xff) << 8) | (buffer[startIndex] & 0xff));
	}

	/**
	 * parse 2 byte of a data buffer to integer value, buffer byte sequence low byte high byte
	 * @param buffer
	 * @param startIndex index of low byte 
	 */
	public static int parse2UnsignedShort(byte[] buffer, int startIndex) {
		return ((buffer[startIndex + 1] & 0xff) << 8) | (buffer[startIndex] & 0xff);
	}

	/**
	 * @param b1 byte
	 * @param b2 byte
	 * @param b3 byte
	 * @param b4 byte
	 * @return the integer value whose byte representation is the given 4 bytes in big-endian order
	 */
  public static int intFromBytes(byte b1, byte b2, byte b3, byte b4) {
    return b1 << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8 | (b4 & 0xFF);
  }

	/**
	 * @param b1 byte
	 * @param b2 byte
	 * @return the short value whose byte representation is the given 2 bytes in big-endian order
	 */
  public static short shortFromBytes(byte b1, byte b2) {
    return (short) ((b1 << 8) | (b2 & 0xFF));
  }

	/**
	 * parse high and low byte to short integer value
	 * @param low byte
	 * @param high byte
	 */
	public static short parse2Short(byte low, byte high) {
		return (short) (((high & 0xFF) << 8) | (low & 0xFF));
	}

	/**
	 * parse high and low byte to short integer value
	 * @param low byte
	 * @param high byte
	 */
	public static int parse2UnsignedShort(byte low, byte high) {
		return ((high & 0xFF) << 8) | (low & 0xFF);
	}
}
