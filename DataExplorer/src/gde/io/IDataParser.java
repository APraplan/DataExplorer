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
    
    Copyright (c) 2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import gde.exception.DevicePropertiesInconsistenceException;

import java.util.Date;

/**
 * class to define interface to a data parser
 */
public interface IDataParser {

	/**
	 * @return the values
	 */
	public int[] getValues();
	
	/**
	 * @return the time
	 */
	public long getTime_ms();
	/**
	 * @return the date
	 */
	public Date getDate();
	/**
	 * @return the comment
	 */
	public String getComment();

	/**
	 * @return the startTimeStamp
	 */
	public long getStartTimeStamp();

	/**
	 * @return the lastTimeStamp
	 */
	public long getLastTimeStamp();
	
	/**
	 * @return the actual state number
	 */
	public int getState();
	
	/**
	 * @return the recordSetNumberOffset
	 */
	public int getRecordSetNumberOffset();

	/**
	 * @param isTimeResetPrepared the isTimeResetPrepared to set
	 */
	public void setTimeResetEnabled(boolean isTimeResetPrepared);
	
	/**
	 * parse the input line string
	 * @param inputLine
	 * @throws DevicePropertiesInconsistenceException
	 * @throws Exception
	 */
	public void parse(String inputLine, int lineNum) throws DevicePropertiesInconsistenceException, Exception;

	/**
	 * default parse method for $1, 1, 0, 14780, 0,598, 1,000, 8,838, 22 like lines
	 * @param inputLine
	 * @param strValues
	 * @throws DevicePropertiesInconsistenceException
	 */
	public void parse(String inputLine, String[] strValues) throws DevicePropertiesInconsistenceException;
	
	/**
	 * @return the channel/config number to locate the parsed data
	 */
	public int getChannelConfigNumber();
	

	/**
	 * @return true if all $2, $3 should be redirected to channel 1
	 */
	public boolean isRedirectChannel1();

}
