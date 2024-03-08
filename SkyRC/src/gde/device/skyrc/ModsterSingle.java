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
import java.util.logging.Logger;

import javax.usb.UsbClaimException;
import javax.usb.UsbException;
import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.messages.Messages;
import gde.utils.WaitTimer;

/**
 * implementation to be used for Modster type charger with 1 channel SkyRC OEM product
 */
public class ModsterSingle extends Modster implements IDevice {
	final static Logger				log					= Logger.getLogger(ModsterSingle.class.getName());
	ModsterUsbPort						usbPort;
	ModsterSingleGathererThread	dataGatherThread;

	/**
	 * @param xmlFileName
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public ModsterSingle(String xmlFileName) throws FileNotFoundException, JAXBException {
		super(xmlFileName);
		this.usbPort = new ModsterUsbPort(this, this.application);
	}

	/**
	 * @param deviceConfig
	 */
	public ModsterSingle(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.usbPort = new ModsterUsbPort(this, this.application);
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	@Override
	public void open_closeCommPort() {
		if (this.usbPort != null) {
			if (!this.usbPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.dataGatherThread = new ModsterSingleGathererThread(this.application, this, this.usbPort, activChannel.getNumber(), this.getDialog());
						try {
							if (this.dataGatherThread != null && this.usbPort.isConnected()) {
								this.systemInfo[0] = new Modster.SystemInfo(this.usbPort.getSystemInfo(this.dataGatherThread.getUsbInterface(), IMaxB6RDX1UsbPort.QuerySystemInfo.CHANNEL_A.value()));
								this.systemSetting[0] = new Modster.SystemSetting(this.usbPort.getSystemSetting(this.dataGatherThread.getUsbInterface(), IMaxB6RDX1UsbPort.QuerySystemSetting.CHANNEL_A.value()));

								WaitTimer.delay(100);
								this.dataGatherThread.start();
							}
						}
						catch (Throwable e) {
							log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
					}
				}
				catch (UsbClaimException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(),
							Messages.getString(gde.messages.MessageIds.GDE_MSGE0051, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
					try {
						if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
					}
					catch (UsbException ex) {
						log.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
					}
				}
				catch (UsbException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0050));
					try {
						if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
					}
					catch (UsbException ex) {
						log.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
					}
				}
				catch (ApplicationConfigurationException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (Throwable e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
				}
			}
			else {
				if (this.dataGatherThread != null) {
					this.dataGatherThread.stopDataGatheringThread(false, null);
				}
				//if (this.boundsComposite != null && !this.isDisposed()) this.boundsComposite.redraw();
				try {
					WaitTimer.delay(1000);
					if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
				}
				catch (UsbException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}
}
