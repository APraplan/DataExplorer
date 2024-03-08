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
package gde.device.junsi;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import gde.device.DeviceConfiguration;

/**
 * Junsi iCharger 406DUO device class
 * @author Winfried Brügmann
 */
public class iCharger406DUO extends iChargerUsb {

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public iCharger406DUO(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public iCharger406DUO(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

	/**
	 * query number of Lithium cells of this charger device
	 * @return
	 */
	@Override
	public int getNumberOfLithiumCells() {
		return 6;
	}
	
	/**
	 * @return the minimal input voltage
	 */
	@Override
	public int getDcInputVoltMin() {
		return 100; //*0.1V
	}
	
	/**
	 * @return the maximal input voltage
	 */
	@Override
	public int getDcInputVoltMax() {
		return 290; //*0.1 V
	}
	
	/**
	 * @return the maximal input current
	 */
	@Override
	public int getDcInputCurrentMax() {
		return 600; //*0.1 A
	}
	
	/**
	 * @return the minimal regenerative input voltage
	 */
	@Override
	public int getRegInputVoltMin() {
		return 100; //*0.1V
	}
	
	/**
	 * @return the maximal regenerative input voltage
	 */
	@Override
	public int getRegInputVoltMax() {
		return 300; //*0.1 V
	}

	/**
	 * @return the maximal charge current for each channel (async)
	 */
	@Override
	public int getChargeCurrentMax4Channel() {
		return 400; //*0.1 A
	}
	
	/**
	 * @return the maximal charge current for channels in sync
	 */
	public int getChargeCurrentMaxSyncChannels() {
		return 700; //*0.1 A
	}
	
	/**
	 * @return the maximal charge power of device
	 */
	public int getDeviceChargePowerMax() {
		return 1000;
	}

	/**
	 * @return the maximal charge power
	 */
	@Override
	public int[] getChargePowerMax() {
		return new int[] {1000, 1000};
	}

	/**
	 * @return the maximal discharge current
	 */
	@Override
	public int[] getDischargePowerMax() {
		return new int[] {80, 80};
	}
	
	/**
	 * @return the min/max regenerative channel voltage, factor 1000
	 */
	@Override
	public int[] getRegChannelVoltageLimits() {
		return new int[] {200, 27000};
	}

	/**
	 * @return the min/max regenerative channel current, factor 100
	 */
	@Override
	public int[] getRegChannelCurrentLimits() {
		return new int[] {5, 4000};
	}
	
	/**
	 * @return the max voltage adjustable for digital power supply voltage, factor 10
	 */
	public int getDigitalPowerVoltageMax() {
		return 0;
	}
}
