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
    
    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
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
    
    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.exception.DataInconsitsentException;
import gde.messages.Messages;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

/**
 * Graupner Ultramat 16  (copy of Ultramat16S)
 * @author Winfried Brügmann
 */
public class Ultramat16 extends Ultramat16S {
	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public Ultramat16(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202),
				Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGI2206), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2207),
				Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2206), 
				Messages.getString(MessageIds.GDE_MSGT2222), Messages.getString(MessageIds.GDE_MSGT2222)};

		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		this.dialog = null;
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Ultramat16(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202),
				Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGI2206), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2207),
				Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2206), 
				Messages.getString(MessageIds.GDE_MSGT2222), Messages.getString(MessageIds.GDE_MSGT2222)};

		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		this.dialog = null;
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	@Override
	public int getLovDataByteSize() {
		return 60;
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * this method is more usable for real logger, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	@Override
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int deviceDataBufferSize = Math.abs(this.getDataBlockSize(InputTypes.SERIAL_IO));
		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigNumber())];
		int offset = 0;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();

		if (dataBuffer[offset] == 0x0C) {
			byte[] convertBuffer = new byte[deviceDataBufferSize];
			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

			for (int i = 0; i < recordDataSize; i++) {
				System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize);
				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
				recordSet.addPoints(convertDataBytes(points, convertBuffer));
			}

			recordSet.setTimeStep_ms(this.getAverageTimeStep_ms() != null ? this.getAverageTimeStep_ms() : 1000);
		}

		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
	}
}
