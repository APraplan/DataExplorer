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
    
    Copyright (c) 2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.wb;

import gde.GDE;
import gde.data.Record;
import gde.device.CheckSumTypes;
import gde.device.FormatTypes;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.io.DataParser;
import gde.messages.MessageIds;
import gde.messages.Messages;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * implements special requirements for AkkuMonitor data
 */
public class AkkuMonitorParser extends DataParser {
	private static final Logger	log												= Logger.getLogger(AkkuMonitorParser.class.getName());

	/**
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType
	 * @param useDataSize
	 */
	public AkkuMonitorParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, int useDataSize) {
		super(useTimeFactor, useLeaderChar, useSeparator, useCheckSumType, useDataSize);
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
	public AkkuMonitorParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, FormatTypes useCheckSumFormatType, int useDataSize,
			FormatTypes useDataFormatType, boolean doMultiply1000) {
		super(useTimeFactor, useLeaderChar, useSeparator, useCheckSumType, useCheckSumFormatType, useDataSize, useDataFormatType, doMultiply1000);
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

		strValue = strValues[2].trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT);
		strValue = strValue.length() > 0 ? strValue : "0";
		if (this.start_time_ms == Integer.MIN_VALUE) {
			this.start_time_ms = (int) (Double.parseDouble(strValue) * this.timeFactor); // Seconds * 1000 = msec
		}
		else {
			this.time_ms = (int) (Double.parseDouble(strValue) * this.timeFactor) - this.start_time_ms; // Seconds * 1000 = msec			
		}

		for (int i = 0; i < this.valueSize; i++) {
			strValue = strValues[i + 3].trim();
			PropertyType property = device.getMeasurement(this.channelConfigNumber, i).getProperty(MeasurementPropertyTypes.DATA_TYPE.value());
			try {
				if (property == null) { //no special data type
					double tmpValue = strValue.length() > 0 ? Double.parseDouble(strValue.trim()) : 0.0;
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
				else {
					log.log(Level.FINE, Record.DataType.fromValue(property.getValue()).toString());
					switch (Record.DataType.fromValue(property.getValue())) {
					case GPS_LATITUDE:
					case GPS_LONGITUDE:
						this.values[i] = Integer.parseInt(strValue.replace(GDE.STRING_DOT, GDE.STRING_EMPTY)); //degree minutes 4 decimals
						break;
					case GPS_ALTITUDE:
						this.values[i] = Integer.parseInt(strValue) * 1000; 
						break;
					case DATE_TIME:
						//System.out.println(strValue);
						String[] tmpValues = strValue.split(GDE.STRING_COLON);
						if (tmpValues.length == 2) { //: contained
							this.values[i] = tmpValues[0].trim().length() > 0 ? Integer.parseInt(tmpValues[0].trim()) * 60 * 1000 : 0; //mm:ss.S
							if (tmpValues[1].indexOf(GDE.CHAR_DOT) >= 0) {
								tmpValues[0] = tmpValues[1].substring(0, tmpValues[1].indexOf(GDE.CHAR_DOT));
								tmpValues[1] = tmpValues[1].substring(tmpValues[1].indexOf(GDE.CHAR_DOT)+1);
								this.values[i] += tmpValues[0].trim().length() > 0 ? Integer.parseInt(tmpValues[0].trim()) * 1000 : 0; //mm:ss.S
								this.values[i] += tmpValues[1].trim().length() > 0 ? Integer.parseInt(tmpValues[1].trim()) * 100 : 0; //mm:ss.S
							}
							else {
								this.values[i] += Integer.parseInt(tmpValues[1].substring(0, 2).trim()) * 1000;
							}
						}
						else if (strValue.contains(GDE.STRING_DOT)) { // dot contained
							tmpValues = new String[2];
							tmpValues[0] = strValue.substring(0, strValue.indexOf(GDE.CHAR_DOT));
							tmpValues[1] = strValue.substring(strValue.indexOf(GDE.CHAR_DOT)+1);
							this.values[i] += tmpValues[0].trim().length() > 0 ? Integer.parseInt(tmpValues[0].trim()) * 60 * 1000 : 0; //mm:ss.S
							this.values[i] += tmpValues[1].trim().length() > 0 ? Integer.parseInt(tmpValues[1].trim()) * 1000 : 0; //mm:ss.S
						}
						else {
							this.values[i] = tmpValues[0].trim().length() > 0 ? Integer.parseInt(tmpValues[0].trim()) * 60 * 1000 : 0; //mm:ss.S
						}
						break;
					default:
						break;
					}
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
				log.log(Level.WARNING, e.getMessage(), e);
				throw e;
			}
		}
	}

}
