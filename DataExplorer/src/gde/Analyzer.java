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

    Copyright (c) 2018 Thomas Eickert
****************************************************************************************/

package gde;

import java.io.IOException;
import java.util.function.Supplier;

import com.sun.istack.Nullable;

import gde.config.DeviceConfigurations;
import gde.config.ExportService;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.log.Level;
import gde.log.Logger;

/**
 * Kernel for analyzing logging data.
 * Loads the active flagged device configurations.
 * References the settings and the roaming data access.
 * Tracks the active device with its channels and the active channel.
 * @author Thomas Eickert (USER)
 */
public abstract class Analyzer implements Cloneable {
	private static final String						$CLASS_NAME				= Analyzer.class.getName();
	private static final Logger						log								= Logger.getLogger($CLASS_NAME);

	protected static Analyzer							analyzer;
	protected static Supplier<Analyzer>		analyzerBuilder		= null;
	protected static Supplier<DataAccess>	dataAccessBuilder	= null;

	public static void setBuilders(Supplier<Analyzer> analyzerBuilder, Supplier<DataAccess> dataAccessBuilder) {
		Analyzer.analyzerBuilder = analyzerBuilder;
		Analyzer.dataAccessBuilder = dataAccessBuilder;
	}

	public static boolean isWithBuilders() {
		return analyzerBuilder != null;
	}

	public static Analyzer getInstance() {
		if (analyzer == null) {
			if (analyzerBuilder != null) {
				analyzer = analyzerBuilder.get();
			} else {
				if (!GDE.isWithUi()) {
					analyzer = new TestAnalyzer();
				} else {
					analyzer = new Explorer();
				}
			}
			// synchronize now to avoid a performance penalty in case of frequent getInstance calls
			synchronized (analyzer) {
				analyzer.startDeviceConfigurationsThread();
			}
		}
		return analyzer;
	}

	protected final Settings							settings;
	protected final DataAccess						dataAccess;
	protected final DeviceConfigurations	deviceConfigurations;

	protected Thread											deviceConfigurationsThread;
	protected IDevice											activeDevice	= null;
	protected Channels										channels			= null;

	protected Analyzer() {
		this.settings = Settings.getInstance();
		if (dataAccessBuilder != null) {
			this.dataAccess = dataAccessBuilder.get();
		} else if (!GDE.isWithUi()) {
			this.dataAccess = DataAccess.getInstance();
		} else {
			this.dataAccess = DataAccess.getInstance();
		}

		this.deviceConfigurations = new DeviceConfigurations();
	}

	public void startDeviceConfigurationsThread() {
		this.deviceConfigurationsThread = new Thread("loadDeviceConfigurations") {
			@Override
			public void run() {
				log.log(Level.FINE, "deviceConfigurationsThread    started");
				Analyzer.this.deviceConfigurations.initialize(Analyzer.this);
			}
		};
		this.deviceConfigurationsThread.start();
	}

	/**
	 * Hybrid singleton copy constructor.
	 * Caution:
	 * <li>NO deep copy for the device configurations and the active device
	 * <li>the active device does NOT copy the device specific objects (e.g. HoTTAdapter fields)
	 */
	protected Analyzer(Analyzer that) {
		this.settings = new Settings(that.settings);
		this.dataAccess = that.dataAccess.clone();

		if (that.activeDevice == null) {
			this.activeDevice = null;
			this.channels = null;
		} else {
			// inhomogeneous clone: All device configuration objects remain the same (NOT cloned), whereas native device objects are created anew !!!
			this.activeDevice = that.activeDevice.getDeviceConfiguration().getAsDevice();
			this.channels = new Channels(that.channels);
			this.channels.setupChannels(that); // todo better to add the channel put statements to the copy constructor
		}

		// shallow copy
		if (that.deviceConfigurations == null) {
			throw new UnsupportedOperationException("clone is requested before the configurations are loaded");
		} else {
			this.deviceConfigurations = that.deviceConfigurations;
		}
		// define thread object for other methods checking its progress
		this.deviceConfigurationsThread = that.deviceConfigurationsThread;
		// do not start because we only do a shallow copy for performance reasons
		// this.deviceConfigurationsThread.start();
	}

	/**
	 * @return the roaming data sources support
	 */
	public DataAccess getDataAccess() {
		return this.dataAccess;
	}

	public DeviceConfigurations getDeviceConfigurations() {
		if (this.deviceConfigurationsThread.isAlive()) {
			joinDeviceConfigurationsThread();
		}
		return this.deviceConfigurations;
	}

	@Nullable
	public Channels getChannels() {
		return this.channels;
	}

	@Nullable
	public Channel getActiveChannel() {
		return this.channels != null ? this.channels.getActiveChannel() : null;
	}

	@Nullable
	public IDevice getActiveDevice() {
		return this.activeDevice;
	}

	/**
	 * Do not clean or rebuild the channels.
	 */
	public void setActiveDevice(IDevice device) {
		this.activeDevice = device;
	}

	/**
	 * Do not clean or rebuild the channels.
	 */
	public void setActiveDevice(String deviceName) {
		setActiveDevice(deviceConfigurations.get(deviceName).getAsDevice());
	}

	public void setChannelNumber(int channelNumber) {
		this.channels.setActiveChannelNumber(channelNumber);
	}

	/**
	 * @return the settings clone
	 */
	public Settings getSettings() {
		return this.settings;
	}

	@Override
	@Deprecated // use getReplica instead
	public Analyzer clone() {
		throw new UnsupportedOperationException();
	};

	/**
	 * Support multiple threads with different analyzer instances.
	 * Use this if analyzer updates are not required or apply to the current thread only.
	 * Be aware of the cloning performance impact.
	 * @return the shallow / deep clone instance ({@link gde.Analyzer#Analyzer(Analyzer)})
	 */
	public abstract Analyzer getReplica();

	public void joinDeviceConfigurationsThread() {
		try {
			while (this.deviceConfigurationsThread == null) {
				Thread.sleep(22);
			}
			this.deviceConfigurationsThread.join();
		} catch (InterruptedException e) {
			log.log(Level.SEVERE, "deviceConfigurationsThread.run()", e);
		}
	}

	@Override
	public String toString() {
		String deviceName = this.activeDevice != null ? this.activeDevice.getName() : "null";
		int channelsSize = this.channels != null ? this.channels.size() : 0;
		int channelNumber = this.getActiveChannel() != null ? this.getActiveChannel().getNumber() : -1;
		return "Analyzer [activeDevice=" + deviceName + ", deviceConfigurationsSize=" + this.deviceConfigurations.getAllConfigurations().size() //
				+ ", channelNumber=" + channelNumber + ", objectKey=" + this.getSettings().getActiveObjectKey() + ", channelsSize=" + channelsSize + "]";
	}

	/**
	 * Set the basic analysis parameters.
	 * Do not use with the integrated UI.
	 */
	public void setArena(String fileDeviceName, int channelNumber, String objectKey) {
		try {
			DeviceConfiguration deviceConfig = getDeviceConfigurations().get(fileDeviceName);
			if (deviceConfig == null) {
				ExportService service = this.settings.getDeviceServices().get(fileDeviceName);
				getSettings().extractDevicePropertiesAndTemplates(service.getJarFile(), fileDeviceName);
				this.deviceConfigurations.add(this, fileDeviceName, fileDeviceName + GDE.FILE_ENDING_DOT_XML, false);
				deviceConfig = this.deviceConfigurations.get(fileDeviceName);
			}

			log.log(Level.FINE, fileDeviceName, deviceConfig);
			IDevice asDevice = deviceConfig.getAsDevice();
			setArena(asDevice, channelNumber, objectKey);
		} catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * Set the basic analysis parameters.
	 * Do not use with the integrated UI.
	 */
	public void setArena(IDevice device, int channelNumber, String objectKey) {
		this.settings.setActiveObjectKey(objectKey);

		log.fine(() -> device + "  " + this.activeDevice);
		if (!device.equals(this.activeDevice)) {
			// device :
			this.activeDevice = device;
			this.setActiveDevice(device);

			if (this.channels == null) {
				this.channels = Channels.createChannels();
				channels.setupChannels(this);
			} else {
				this.channels.setupChannels(this);
			}
		}

		// channel :
		this.channels.setActiveChannelNumber(channelNumber);
	}

}
