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

import gde.device.DeviceConfiguration;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

/**
 * @author brueg
 *
 */
public class Herkules_III_Telemetrie extends CSV2SerialAdapter {

	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public Herkules_III_Telemetrie(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * @param deviceConfig
	 */
	public Herkules_III_Telemetrie(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

	/**
	 * query if the record set numbering should follow channel configuration numbering
	 * @return true where devices does not distinguish between channels (for example Av4ms_FV_762)
	 */
	@Override
	public boolean recordSetNumberFollowChannel() {
		return true;
	}

	/**
	 * query if the channel in use has dependency with each other to initiate file description synchronization 
	 * @return true for devices with one source of data distributed over record sets (HoTTAdapter, Av4ms_FV_762)
	 */
	@Override
	public boolean useChannelWithSyncedDescription() { return true; }
}
