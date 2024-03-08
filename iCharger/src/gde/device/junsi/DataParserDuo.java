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
    
    Copyright (c) 2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.junsi;

import java.util.logging.Level;
import java.util.logging.Logger;

import gde.GDE;
import gde.device.CheckSumTypes;
import gde.device.FormatTypes;
import gde.device.InputTypes;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.io.DataParser;
import gde.messages.MessageIds;
import gde.messages.Messages;

/**
 * @author brueg
 *
 */
public class DataParserDuo extends DataParser {
	static Logger			log										= Logger.getLogger(DataParser.class.getName());

	protected final int offset;
	/**
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType
	 * @param useDataSize
	 */
	public DataParserDuo(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, int useDataSize, int offset) {
		super(useTimeFactor, useLeaderChar, useSeparator, useCheckSumType, useDataSize);
		this.offset = offset;
	}

	/**
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType
	 * @param useCheckSumFormatType
	 * @param useDataSize
	 * @param useDataFormatType
	 * @param doMultiply1000
	 */
	public DataParserDuo(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, FormatTypes useCheckSumFormatType, int useDataSize, FormatTypes useDataFormatType,
			boolean doMultiply1000, int offset) {
		super(useTimeFactor, useLeaderChar, useSeparator, useCheckSumType, useCheckSumFormatType, useDataSize, useDataFormatType, doMultiply1000);
		this.offset = offset;
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
		this.channelConfigNumber = Integer.parseInt(strValue);

		strValue = strValues[1].trim();
		this.state = Integer.parseInt(strValue);
		
		switch (this.state) {
		default: 
			strValue = strValues[2].trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT);
			strValue = strValue.length() > 0 ? strValue : "0";
			if (this.start_time_ms == Integer.MIN_VALUE) {
				this.start_time_ms = (int) (Double.parseDouble(strValue) * this.timeFactor); // Seconds * 1000 = msec
			}
			else {
				this.time_ms = (int) (Double.parseDouble(strValue) * this.timeFactor) - this.start_time_ms; // Seconds * 1000 = msec			
			}

			for (int i = offset; i < Math.abs(device.getDataBlockSize(InputTypes.FILE_IO))+offset; i++) {
				strValue = strValues[i + 3].trim();
				try {
					double tmpValue = strValue.length() > 0 ? Double.parseDouble(strValue.trim()) : 0.0;
					if (this.isMultiply1000 && tmpValue < Integer.MAX_VALUE / 1000 && tmpValue > Integer.MIN_VALUE / 1000)
						this.values[i-offset] = (int) (tmpValue * 1000); // enable 3 positions after decimal place
					else // needs special processing within IDevice.translateValue(), IDevice.reverseTranslateValue()
					if (tmpValue < Integer.MAX_VALUE || tmpValue > Integer.MIN_VALUE) {
						this.values[i-offset] = (int) tmpValue;
					}
					else {
						this.values[i-offset] = (int) (tmpValue / 1000);
					}
				}
				catch (NumberFormatException e) {
					this.values[i-offset] = 0;
				}
			}
			break;
			
		case 128: //0x80 BattRi
			int valueOffset = ((iChargerUsb)this.device).getNoneCalculationMeasurementNames(1, ((iChargerUsb)this.device).getMeasurementNames(1)).length - ((iChargerUsb)this.device).getNumberOfLithiumCells() - 2;
			strValue = strValues[2].trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT);
			strValue = strValue.length() > 0 ? strValue : "0";
			if (this.start_time_ms == Integer.MIN_VALUE) {
				this.start_time_ms = (int) (Double.parseDouble(strValue) * this.timeFactor); // Seconds * 1000 = msec
			}
			else {
				this.time_ms = (int) (Double.parseDouble(strValue) * this.timeFactor) - this.start_time_ms; // Seconds * 1000 = msec		
			}

			for (int i = 0; i < this.valueSize-valueOffset; i++) {
				strValue = strValues[i + 1 + offset].trim();
				try {
					double tmpValue = strValue.length() > 0 ? Double.parseDouble(strValue.trim()) : 0.0;
					this.values[i + valueOffset] = (int) tmpValue;
				}
				catch (NumberFormatException e) {
					this.values[i + valueOffset] = 0;
				}
			}
			break;
					
		case 130: //0x82 Batt capacity/cell skip
			break;
		}


		//check time reset to force a new data set creation
		if (this.device.getTimeStep_ms() < 0 && this.time_ms <= 0 && this.isTimeResetEnabled) {
			this.recordSetNumberOffset += ++this.timeResetCounter;
			this.isTimeResetEnabled = false;
		}

		if (this.checkSumType != null) {
			if (!isChecksumOK(inputLine, Integer.parseInt(strValues[strValues.length - 1].trim(), 16))) {
				DevicePropertiesInconsistenceException e = new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0049, new Object[] { strValues[strValues.length - 1].trim(), String.format("%X", calcChecksum(inputLine)) }));
				log.log(Level.WARNING, e.getMessage(), e);
				throw e;
			}
		}
	}

}
