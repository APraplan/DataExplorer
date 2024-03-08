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

    Copyright (c) 2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.junsi;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.device.DeviceConfiguration;

/**
 * Junsi iCharger X6 device class
 * @author Winfried Brügmann
 */
public class iChargerX6 extends iChargerUsb {

	public enum BatteryTypesX { //is different to iChargerUSB.BatteryTypesDuo
		BT_UNKNOWN("?"), BT_LIPO("LiPo"), BT_LIIO("LiIo"), BT_LIFE("LiFe"), BT_LIHV("LiHV"), BT_LTO("LTO"), BT_NIMH("NiMH"), BT_NICD("NiCd"), BT_NIZN("NiZn"), BT_PB("PB"), BT_POWER("Power"), BT_USER("User"), BT_UNKNOWN_("?");

		private String value;
		
		private BatteryTypesX(String newValue) {
			value = newValue;
		}
		
		protected String getName() {
			return value;
		}
		
		public static BatteryTypesX[] VALUES = values();
		
		public static String[] getValues() {
			StringBuilder sb = new StringBuilder();
			for (BatteryTypesX bt : BatteryTypesX.values()) 
				sb.append(bt.value).append(GDE.CHAR_CSV_SEPARATOR);
			return sb.toString().split(GDE.STRING_CSV_SEPARATOR);
		}
	};
		
	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public iChargerX6(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		this.BATTERIE_TYPES = BatteryTypesX.getValues(); 
	}

	/**
	 * @param deviceConfig
	 */
	public iChargerX6(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.BATTERIE_TYPES = BatteryTypesX.getValues(); 
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
		return 70; //*0.1V
	}
	
	/**
	 * @return the maximal input voltage
	 */
	@Override
	public int getDcInputVoltMax() {
		return 310; //*0.1 V
	}
	
	/**
	 * @return the maximal input current
	 */
	@Override
	public int getDcInputCurrentMax() {
		return 350; //*0.1 A
	}
	
	/**
	 * @return the minimal regenerative input voltage
	 */
	@Override
	public int getRegInputVoltMin() {
		return 70; //*0.1V
	}
	
	/**
	 * @return the maximal regenerative input voltage
	 */
	@Override
	public int getRegInputVoltMax() {
		return 310; //*0.1 V
	}

	/**
	 * @return the maximal charge current for channel
	 */
	@Override
	public int getChargeCurrentMax4Channel() {
		return 300; //*0.1 A
	}
	
	/**
	 * @return the maximal charge current for channels in sync
	 */
	public int getChargeCurrentMaxSyncChannels() { return 0;};
	
	/**
	 * @return the maximal charge power of device
	 */
	public int getDeviceChargePowerMax() {
		return 800;
	}

	/**
	 * @return the maximal charge power
	 */
	@Override
	public int[] getChargePowerMax() {
		return new int[] {800, 0};
	}

	/**
	 * @return the maximal discharge current
	 */
	@Override
	public int[] getDischargePowerMax() {
		return new int[] {30, 0};
	}
	
	/**
	 * @return the min/max regenerative channel voltage, factor 100
	 */
	@Override
	public int[] getRegChannelVoltageLimits() {
		return new int[] {0, 0};
	}

	/**
	 * @return the min/max regenerative channel current, factor 100
	 */
	@Override
	public int[] getRegChannelCurrentLimits() {
		return new int[] {0, 0};
	}
	
	/**
	 * @return the max voltage adjustable for digital power supply voltage, factor 10
	 */
	public int getDigitalPowerVoltageMax() {
		return 265;
	}
}
