/**************************************************************************************
  	This file is part of DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.skyrc;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.messages.Messages;

/**
 * implementation to be used for Modster type charger SkyRC OEM product
 */
public class Modster extends HitecX1Red  implements IDevice {

	/**
	 * @param xmlFileName
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public Modster(String xmlFileName) throws FileNotFoundException, JAXBException {
		super(xmlFileName);
	
		this.STATUS_MODE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3600), 	//Standby
				Messages.getString(MessageIds.GDE_MSGT3601) + //Charge	0x01
				GDE.STRING_COLON +
				Messages.getString(MessageIds.GDE_MSGT3602),	//Discharge
				Messages.getString(MessageIds.GDE_MSGT3600), 	//Standby 0x02
				Messages.getString(MessageIds.GDE_MSGT3604), 	//Finish	0x03
				Messages.getString(MessageIds.GDE_MSGT3605) };//Error		0x04

		//LI battery： 	0：CHARGE, 1：FAST CHG, 2：STORAGE, 3：DISCHARGE, 4：BALANCE
		this.USAGE_MODE_LI = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3617), //CHARGE
				Messages.getString(MessageIds.GDE_MSGT3615), //FAST CHG
				Messages.getString(MessageIds.GDE_MSGT3612), //STORAGE
				Messages.getString(MessageIds.GDE_MSGT3613), //DISCHARGE
				Messages.getString(MessageIds.GDE_MSGT3616)};//BALANCE

		//Ni battery:		0=CHARGE 1=DISCHARGE 2=AUTO_CHARGE 3=RE_PEAK 4=CYCLE
		this.USAGE_MODE_NI = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3617), //CHARGE
				Messages.getString(MessageIds.GDE_MSGT3623), //DISCHARGE
				Messages.getString(MessageIds.GDE_MSGT3625), //AUTO_CHARGE
				Messages.getString(MessageIds.GDE_MSGT3626), //RE_PEAK
				Messages.getString(MessageIds.GDE_MSGT3624) };//CYCLE

		//PB battery:		0=CHARGE 1=DISCHARGE
		this.USAGE_MODE_PB = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3620), 
				Messages.getString(MessageIds.GDE_MSGT3623) };

		//battery type:  0:LiPo 1:LiFe 2:LiLo 3:LiHv 4:NiCd 5:NiMH 6:PB
		this.BATTERY_TYPE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3649), 
				Messages.getString(MessageIds.GDE_MSGT3641), 
				Messages.getString(MessageIds.GDE_MSGT3640),
				Messages.getString(MessageIds.GDE_MSGT3642), 
				Messages.getString(MessageIds.GDE_MSGT3644), 
				Messages.getString(MessageIds.GDE_MSGT3643), 
				Messages.getString(MessageIds.GDE_MSGT3648) };
	}

	/**
	 * @param deviceConfig
	 */
	public Modster(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		
		this.STATUS_MODE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3600), 	//Standby
				Messages.getString(MessageIds.GDE_MSGT3601) + //Charge	0x01
				GDE.STRING_COLON +
				Messages.getString(MessageIds.GDE_MSGT3602),	//Discharge
				Messages.getString(MessageIds.GDE_MSGT3600), 	//Standby 0x02
				Messages.getString(MessageIds.GDE_MSGT3604), 	//Finish	0x03
				Messages.getString(MessageIds.GDE_MSGT3605) };//Error		0x04
		
		//LI battery： 	0：CHARGE, 1：FAST CHG, 2：STORAGE, 3：DISCHARGE, 4：BALANCE
		this.USAGE_MODE_LI = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3617), //CHARGE
				Messages.getString(MessageIds.GDE_MSGT3615), //FAST CHG
				Messages.getString(MessageIds.GDE_MSGT3612), //STORAGE
				Messages.getString(MessageIds.GDE_MSGT3613), //DISCHARGE
				Messages.getString(MessageIds.GDE_MSGT3616)};//BALANCE

		//Ni battery:		0=CHARGE 1=DISCHARGE 2=AUTO_CHARGE 3=RE_PEAK 4=CYCLE
		this.USAGE_MODE_NI = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3617), //CHARGE
				Messages.getString(MessageIds.GDE_MSGT3623), //DISCHARGE
				Messages.getString(MessageIds.GDE_MSGT3625), //AUTO_CHARGE
				Messages.getString(MessageIds.GDE_MSGT3626), //RE_PEAK
				Messages.getString(MessageIds.GDE_MSGT3624) };//CYCLE

		//PB battery:		0=CHARGE 1=DISCHARGE
		this.USAGE_MODE_PB = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3620), 
				Messages.getString(MessageIds.GDE_MSGT3623) };

		//battery type:  0:LiPo 1:LiFe 2:LiLo 3:LiHv 4:NiCd 5:NiMH 6:PB
		this.BATTERY_TYPE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3649), 
				Messages.getString(MessageIds.GDE_MSGT3641), 
				Messages.getString(MessageIds.GDE_MSGT3640),
				Messages.getString(MessageIds.GDE_MSGT3642), 
				Messages.getString(MessageIds.GDE_MSGT3644), 
				Messages.getString(MessageIds.GDE_MSGT3643), 
				Messages.getString(MessageIds.GDE_MSGT3648)};
	}

}
