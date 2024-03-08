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
package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.messages.Messages;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

/**
 * Graupner Ultra Duo Plus 60 base class
 * @author Winfried Brügmann
 */
@Deprecated
public class UltramatUDP extends UltraDuoPlus60 {
	final static Logger	logg	= Logger.getLogger(UltramatUDP.class.getName());

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public UltramatUDP(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202),
				Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGT2204), Messages.getString(MessageIds.GDE_MSGT2205), Messages.getString(MessageIds.GDE_MSGT2206),
				Messages.getString(MessageIds.GDE_MSGT2207), Messages.getString(MessageIds.GDE_MSGT2208), Messages.getString(MessageIds.GDE_MSGT2209) };
		this.CHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212),
				Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2215), Messages.getString(MessageIds.GDE_MSGT2216),
				Messages.getString(MessageIds.GDE_MSGT2217), Messages.getString(MessageIds.GDE_MSGT2218), Messages.getString(MessageIds.GDE_MSGT2219), Messages.getString(MessageIds.GDE_MSGT2220),
				Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222) };
		this.DISCHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212),
				Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2223), Messages.getString(MessageIds.GDE_MSGT2224), Messages.getString(MessageIds.GDE_MSGT2220),
				Messages.getString(MessageIds.GDE_MSGT2222) };
		this.DELAY_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2225), Messages.getString(MessageIds.GDE_MSGT2226), Messages.getString(MessageIds.GDE_MSGT2227),
				Messages.getString(MessageIds.GDE_MSGT2228) };
		this.CURRENT_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2229), Messages.getString(MessageIds.GDE_MSGT2230), Messages.getString(MessageIds.GDE_MSGT2231),
				Messages.getString(MessageIds.GDE_MSGT2232), Messages.getString(MessageIds.GDE_MSGT2233), Messages.getString(MessageIds.GDE_MSGT2234), Messages.getString(MessageIds.GDE_MSGT2235),
				Messages.getString(MessageIds.GDE_MSGT2236), Messages.getString(MessageIds.GDE_MSGT2237) };

		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public UltramatUDP(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202),
				Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGT2204), Messages.getString(MessageIds.GDE_MSGT2205), Messages.getString(MessageIds.GDE_MSGT2206),
				Messages.getString(MessageIds.GDE_MSGT2207), Messages.getString(MessageIds.GDE_MSGT2208), Messages.getString(MessageIds.GDE_MSGT2209) };
		this.CHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212),
				Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2215), Messages.getString(MessageIds.GDE_MSGT2216),
				Messages.getString(MessageIds.GDE_MSGT2217), Messages.getString(MessageIds.GDE_MSGT2218), Messages.getString(MessageIds.GDE_MSGT2219), Messages.getString(MessageIds.GDE_MSGT2220),
				Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222) };
		this.DISCHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212),
				Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2223), Messages.getString(MessageIds.GDE_MSGT2224), Messages.getString(MessageIds.GDE_MSGT2220),
				Messages.getString(MessageIds.GDE_MSGT2222) };
		this.DELAY_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2225), Messages.getString(MessageIds.GDE_MSGT2226), Messages.getString(MessageIds.GDE_MSGT2227),
				Messages.getString(MessageIds.GDE_MSGT2228) };
		this.CURRENT_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2229), Messages.getString(MessageIds.GDE_MSGT2230), Messages.getString(MessageIds.GDE_MSGT2231),
				Messages.getString(MessageIds.GDE_MSGT2232), Messages.getString(MessageIds.GDE_MSGT2233), Messages.getString(MessageIds.GDE_MSGT2234), Messages.getString(MessageIds.GDE_MSGT2235),
				Messages.getString(MessageIds.GDE_MSGT2236), Messages.getString(MessageIds.GDE_MSGT2237) };

		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}
}
