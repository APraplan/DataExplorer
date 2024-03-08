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
    
    Copyright (c) 2023 Winfried Bruegmann
****************************************************************************************/
package gde.device.skyrc;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import gde.device.DeviceConfiguration;
import gde.device.bantam.eStationBC6;

/**
 * SkyRC iMax B6 device class which is 100% eStation BC6
 * @author Winfried Brügmann
 */
public class IMaxB6 extends eStationBC6 {

	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public IMaxB6(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * @param deviceConfig
	 */
	public IMaxB6(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

}
