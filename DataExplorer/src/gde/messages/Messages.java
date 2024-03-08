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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.messages;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import gde.GDE;
import gde.log.Level;
import java.util.logging.Logger;

import gde.config.Settings;


public class Messages {
	final static Logger						log						= Logger.getLogger(Messages.class.getName());

	static final String					BUNDLE_NAME			= "gde.messages.messages";		//$NON-NLS-1$

	static ResourceBundle	mainResourceBundle		= ResourceBundle.getBundle(BUNDLE_NAME, Settings.getInstance().getLocale());
	static ResourceBundle	deviceResourceBundle	= ResourceBundle.getBundle(BUNDLE_NAME, Settings.getInstance().getLocale());

	private Messages() {
		//ignore
	}

//	/**
//	 * testing message class
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		System.out.println(Messages.getString(MessageIds.GDE_MSGI0001));
//		System.out.println(Messages.getString(MessageIds.GDE_MSGI0002, new String[] {"UniLog"}));
//		System.out.println(Messages.getString(MessageIds.GDE_MSGI0003, new Object[] {"UniLog", 5}));
//		System.out.println(Messages.getString(MessageIds.GDE_MSGI0003, new Object[] {"UniLog"}));
//	}
	
	/**
	 * example usage: application.openMessageDialog(Messages.getString(MessageIds.GDE_MSG001, new Object{"hallo", "world"));
	 * @param key
	 * @param params as object array
	 * @return the message as string with unlined parameters
	 */
	public static String getString(String key, Object[] params) {
		try {
			String result;
			if (Integer.valueOf(key.substring(key.length()-4)) <= GDE.NUMBER_RANGE_MAX_GDE) {
				result = mainResourceBundle.getString(key);
			}
			else {
				result = deviceResourceBundle.getString(key);
			}
			String[] array = result.split("[{}]");
			StringBuilder sb = new StringBuilder();
			if (array.length > 1) {
				for (int i = 0, j = 0; i < array.length; i++) {
					if (i != 0 && i % 2 != 0)
						sb.append(params.length >= (j + 1) ? params[j++] : "?");
					else
						sb.append(array[i]);
				}
				result = sb.toString();
			}
			
			return result;
		}
		catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
	
	/**
	 * example usage: application.openMessageDialog(Messages.getString(MessageIds.GDE_MSG001));
	 * @param key
	 * @return the string matching the given key
	 */
	public static String getString(String key) {
		try {
			if (Integer.valueOf(key.substring(key.length()-4)) <= GDE.NUMBER_RANGE_MAX_GDE) {
				return mainResourceBundle.getString(key);
			}
			return deviceResourceBundle.getString(key);
		}
		catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	/**
	 * method to modify the resource bundle force using other then the system local
	 * @param newLocale the local to set,  Locale.GERMANY, Locale.ENGLISH, ...
	 */
	public static void setMainResourceBundleLocale(Locale newLocale) {
		Messages.mainResourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, newLocale);
	}

	/**
	 * set the device specific resource bundle
	 * @param newBundleName the deviceResourceBundle to be used
	 */
	public static void setDeviceResourceBundle(String newBundleName, Locale newLocale, ClassLoader loader) {
		try {
			Messages.deviceResourceBundle = ResourceBundle.getBundle(newBundleName, newLocale, loader);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, newBundleName, e);
		}
	}
	
	/**
	 * query the accelerator character to have it language dependent
	 * @param key resource bundle constant key
	 * @return accelerator character
	 */
	public static char getAcceleratorChar(String key) {
		try {
			String resourceString;
			if (Integer.valueOf(key.substring(key.length()-4)) <= GDE.NUMBER_RANGE_MAX_GDE) {
				resourceString = mainResourceBundle.getString(key);
			}
			else {
				resourceString = deviceResourceBundle.getString(key);
			}
			return resourceString.charAt(resourceString.length()-1);
		}
		catch (MissingResourceException e) {
			return '?';
		}
	}
	
	/**
	 * enable reloading resources after a language switch
	 */
	public static void reloadResources() {
		mainResourceBundle		= ResourceBundle.getBundle(BUNDLE_NAME, Settings.getInstance().getLocale());
		deviceResourceBundle	= ResourceBundle.getBundle(BUNDLE_NAME, Settings.getInstance().getLocale());
	}
}
