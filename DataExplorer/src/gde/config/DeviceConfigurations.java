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

    Copyright (c) 2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann, Thomas Eickert
****************************************************************************************/

package gde.config;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.xml.sax.SAXParseException;

import gde.Analyzer;
import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.StringHelper;

/**
 * Provide device lists from used flagged devices and the selected active device.
 * Holds only devices with the usage flag in the device configuration xml file.
 * Not threadsafe due to device shallow copies and JAXB un-/marshallers.
 * @author Thomas Eickert (USER)
 */
public final class DeviceConfigurations {
	private static final String													$CLASS_NAME									= DeviceConfigurations.class.getName();
	private static final Logger													log													= Logger.getLogger($CLASS_NAME);

	private final TreeMap<String, DeviceConfiguration>	deviceConfigs;

	private DeviceConfiguration													selectedActiveDeviceConfig	= null;

	public DeviceConfigurations() {
		this.deviceConfigs = new TreeMap<String, DeviceConfiguration>(String.CASE_INSENSITIVE_ORDER);
	}

	/**
	 * @return the device names of devices with the usage flag in the device configuration xml file
	 */
	public Set<String> deviceNames() {
		return this.deviceConfigs.keySet();
	}

	/**
	 * @return the size of devices with the usage flag in the device configuration xml file
	 */
	public int size() {
		return this.deviceConfigs.size();
	}

	/**
	 * @return the key name at given index
	 */
	public String get(int index) {
		return this.deviceConfigs.keySet().toArray(new String[0])[index];
	}
	
	/**
	 * @return the index position for given name
	 */
	public int indexOf(String deviceKey) {
		int index = -1;
		for (String key : this.deviceConfigs.keySet()) {
			++index;
			if (key.equals(deviceKey)) return index;
		}
		return -1;
	}

	/**
	 * Goes through the existing XML files and set active flagged devices into active devices list.
	 * Fills the DeviceConfigurations list.
	 */
	public synchronized void initialize(Analyzer analyzer) {
		String activeDeviceName = analyzer.getSettings().getActiveDevice();
		Objects.requireNonNull(activeDeviceName);

		this.deviceConfigs.clear();
		for (String fileName : analyzer.getDataAccess().getDeviceFolderList()) {
			// loop through all device properties XML and check if device used
			add(analyzer, activeDeviceName, fileName, !analyzer.getSettings().isDevicePropertiesUpdated);
		}
		
		//active device configurations collected, now synchronize settings device_use accordingly
		this.synchronizeDeviceUse();
		
		log.log(Level.TIME, "device init time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime)));
	}

	/**
	 * synchronize device use list with device configurations, keep independent from remove to enable user to fix problem during device instantiation
	 * to support update from older DataExplorer versions add devices to list which have use flag
	 */
	public void synchronizeDeviceUse() {
		String deviceUseCsv = Settings.getInstance().getDeviceUseCsv();
		if (!deviceUseCsv.isEmpty()) {
			for (String deviceName : deviceUseCsv.split(GDE.STRING_CSV_SEPARATOR)) {
				if (!deviceName.isEmpty() && !this.deviceConfigs.containsKey(deviceName.substring(0, deviceName.lastIndexOf(GDE.CHAR_STAR)))) {
					log.log(Level.INFO, String.format("remove %s from device_use list", deviceName));
					Settings.getInstance().removeDeviceUse(deviceName.substring(0, deviceName.lastIndexOf(GDE.CHAR_STAR)));
				}
			}
		} else { //device_use list empty or does not exist
			for (String deviceKey : this.deviceConfigs.keySet()) {
				if (this.deviceConfigs.get(deviceKey).isUsed())
					Settings.getInstance().addDeviceUse(this.deviceConfigs.get(deviceKey).getName(), 1);
			}
		}
	}

	/**
	 * add device configuration independent of usage flag
	 * @param analyzer
	 * @param activeDeviceName
	 * @param fileName
	 */
	public void add(Analyzer analyzer, String activeDeviceName, String fileName, boolean checkUsedFlag) {
		try {
			if (fileName.endsWith(GDE.FILE_ENDING_DOT_XML)) {
				String deviceKey = fileName.substring(0, fileName.length() - 4);
				DeviceConfiguration devConfig = new DeviceConfiguration(Paths.get(Settings.DEVICE_PROPERTIES_DIR_NAME, fileName), analyzer);
				if (checkUsedFlag && devConfig.isUsed() || !checkUsedFlag) {
					if (devConfig.getName().equals(activeDeviceName) && devConfig.isUsed()) { // define the active device after re-start
						selectedActiveDeviceConfig = devConfig;
					}
					// store all device configurations in a map
					String keyString;
					if (devConfig.getName() != null)
						keyString = devConfig.getName();
					else {
						devConfig.setName(deviceKey);
						keyString = deviceKey;
					}
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, deviceKey + GDE.STRING_MESSAGE_CONCAT + keyString);
					this.deviceConfigs.put(keyString, devConfig);
				}
			}
		} catch (JAXBException e) {
			log.log(Level.WARNING, fileName, e);
			if (e.getLinkedException() instanceof SAXParseException) {
				SAXParseException spe = (SAXParseException) e.getLinkedException();
				GDE.setInitError(Messages.getString(MessageIds.GDE_MSGW0038, new String[] {
						fileName, spe.getLocalizedMessage() }));
			}
		} catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	public TreeMap<String, DeviceConfiguration> getAllConfigurations() { // todo replace with getters / putters etc
		return this.deviceConfigs;
	}

	public boolean contains(String deviceName) {
		return this.deviceConfigs.containsKey(deviceName);
	}

	public DeviceConfiguration get(String deviceName) {
		return this.deviceConfigs.get(deviceName);
	}

	public DeviceConfiguration put(String deviceName, DeviceConfiguration tmpDeviceConfiguration) {
		return this.deviceConfigs.put(deviceName, tmpDeviceConfiguration);
	}

	public DeviceConfiguration remove(String deviceName) {
		return this.deviceConfigs.remove(deviceName);
	}

	/**
	 * @return null or a device selected by name
	 */
	public DeviceConfiguration getSelectedActiveDeviceConfig() {
		return this.selectedActiveDeviceConfig;
	}

	/**
	 * @return the supported lowercase file extensions (e.g. '.bin') or an empty set
	 */
	public Set<String> getValidLogExtentions() {
		Set<String> logExtensions = getImportExtentions();
		logExtensions.add(GDE.FILE_ENDING_DOT_OSD);
		return logExtensions;
	}

	/**
	 * @return the supported lowercase file extensions (e.g. '.bin') or an empty set
	 */
	public Set<String> getImportExtentions() {
		Set<String> extentions = this.deviceConfigs.values().parallelStream() //
				.map(c -> Arrays.asList(c.getDataBlockPreferredFileExtention().split(GDE.REGEX_FILE_EXTENTION_SEPARATION))).flatMap(Collection::stream) //
				.map(s -> s.substring(s.lastIndexOf(GDE.CHAR_DOT))).map(e -> e.toLowerCase()) //
				.collect(Collectors.toSet());
		return extentions;
	}

	/**
	 * @return a map with key device name and device
	 */
	public HashMap<String, IDevice> getAsDevices(Collection<String> deviceNames) {
		HashMap<String, IDevice> existingDevices = new HashMap<>();
		for (String deviceName : deviceNames) {
			existingDevices.put(deviceName, deviceConfigs.get(deviceName).getAsDevice());
		}
		log.log(Level.FINE, "Selected      size=", existingDevices.size());
		return existingDevices;
	}

	/**
	 * @param deviceName is the name entry in the device configuration xml file
	 * @return the cashed device configuration
	 */
	public DeviceConfiguration getConfiguration(String deviceName) {
		return this.deviceConfigs.get(deviceName);
	}

	public String toString() {
		return this.deviceConfigs.keySet().toString();
	}
}
