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
    
    Copyright (c) 2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.device.DeviceConfiguration;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

/**
 * Graupner Polaron AcDc base class
 * @author Winfried Brügmann
 */
public class PolaronACDC extends Polaron {

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public PolaronACDC(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public PolaronACDC(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

	/**
	 * query the device identifier to differentiate between different device implementations
	 * @return 	0=unknown, 1=PolaronEx, 2=PolaronAcDcEQ, 3=PolaronAcDc, 4=PolaronPro, 5=PolaronSports
	 */
	@Override
	public GraupnerDeviceType getDeviceTypeIdentifier() {
		return GraupnerDeviceType.PolaronACDC;
	}
}
