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
package gde.device.conrad;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import gde.device.DeviceConfiguration;

/**
 * VC820 device class
 * @author Winfried Brügmann
 */
public class VC820 extends VC800 {
	
	/**
	 * constructor using properties file
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public VC820(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public VC820(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}
}
