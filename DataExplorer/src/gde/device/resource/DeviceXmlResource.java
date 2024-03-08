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
    
    Copyright (c) 2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.resource;

import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import gde.config.Settings;
import gde.log.Level;

/**
 * singleton class to encapsulate ResourceBundle with some helper methods
 * allowed resource bundle keys may have several underscore characters in its name, type_lap_time, 
 * where the last underscore may an separator character type_outlet_1
 * as separator normally the last occurrence blank character is use "type_outlet 1", 
 * 
 * Key for replacement resource string must not have more than one extension separated by several separator characters!
 * 
 * Use this class to find replacements of keys used in device property XML files without changing the internal XML representation itself. 
 * If the internal XML representation get changed by user or usage it will be saved while closing the application and replaced keys may persist.
 */
public class DeviceXmlResource {
	private final static Logger				log									= Logger.getLogger(DeviceXmlResource.class.getName());

	private static DeviceXmlResource	instance						= null;

	private static ResourceBundle			deviceXmlResources	= null;

	/**
	 * placeholder constructor
	 */
	private DeviceXmlResource() {
	}
	
	/**
	 * @return instance of singleton DeviceXmlResource
	 */
	public static DeviceXmlResource getInstance() {
		if (instance == null) {
			deviceXmlResources = PropertyResourceBundle.getBundle("gde.device.resource.devicexmlresources", Settings.getInstance().getLocale());
			instance = new DeviceXmlResource();
		}
		return instance;
	}
	
	/**
	 * query key to find a valid replacement, use this method to circumvent exception handling of getString()
	 * @param key
	 * @return true|false
	 */
	public boolean isKey(final String key) {
		try {
			deviceXmlResources.getString(key);
		}
		catch (MissingResourceException e) {
			return false;
		}
		return true;
	}

	public String getReplacement(final String key) {
		String replacement = key;

		if (!this.isKey(key)) { //check if key is available which is hopefully the most case
			if (key.length() > 0 && !(Character.isUpperCase(key.charAt(0)) || Character.isDigit(key.charAt(0)))) { //check for first character upper case or decimal digit number which is never a key -> no replacement
				String subKey = key.indexOf(") ") >= 0 ? key.substring(key.indexOf(") ") + 2) : key; //") record_set_name_key"
				if (!this.isKey(subKey)) {
					subKey = subKey.indexOf(' ') >= 3 ? subKey.substring(0, subKey.indexOf(' ')) : subKey; //"voltage S1", "label_cell_voltage 2 EAM"
					if (!this.isKey(subKey)) {
						subKey = subKey.lastIndexOf('_') >= 3 ? subKey.substring(0, subKey.lastIndexOf('_')) : subKey;//"voltage_S1"
						if (!this.isKey(subKey)) {
							subKey = subKey.lastIndexOf('(') >= 3 ? subKey.substring(0, subKey.lastIndexOf('(')) : subKey;//"altitude(relative)"
							if (!this.isKey(subKey)) {
								log.log(Level.WARNING, String.format("Finding a replacement for key = '%s' failed, using key as replacement value", key));
							}
							else { //replacement of key + "(relative)" -> altitude(relative)
								replacement = String.format("%s%s", deviceXmlResources.getString(subKey), key.substring(key.lastIndexOf('(')));
							}
						}
						else { //replacement of key + "_1" -> type_outlet_1
							replacement = String.format("%s%s", deviceXmlResources.getString(subKey), key.substring(key.lastIndexOf('_')));
						}
					}
					else { //replacement of key + " 1" -> "type_outlet 1"
						replacement = key.indexOf(") ") >= 0 ? String.format("%s%s%s", key.substring(0, key.indexOf(") ") + 2), deviceXmlResources.getString(subKey), key.substring(key.lastIndexOf(' ')))
								: String.format("%s%s", deviceXmlResources.getString(subKey), key.substring(key.lastIndexOf(' ')));
					}
				}
				else { //replacement of key + ") " -> "1) state_data_recording"
					replacement = String.format("%s%s", key.substring(0, key.indexOf(") ") + 2), deviceXmlResources.getString(subKey));
				}
			}
		}
		else { //direct replacement, voltage, current,....
			replacement = deviceXmlResources.getString(key);
		}

		return replacement;
	}
	
	public String[] getReplacements(final String[] keys) {
		String[] replacements = new String[keys.length];
		for (int i = 0; i < replacements.length; i++) {
			replacements[i] = this.getReplacement(keys[i]);
		}
		return replacements;
	}

	/**
	 * enable reloading resources after a language switch
	 */
	public static void reloadResources() {
		deviceXmlResources = PropertyResourceBundle.getBundle("gde.device.resource.devicexmlresources", Settings.getInstance().getLocale());
	}

}
