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

    Copyright (c) 2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.io.FileNotFoundException;
import java.nio.file.Paths;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.widgets.FileDialog;

import gde.GDE;
import gde.data.Channel;
import gde.data.Record;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.graupner.hott.MessageIds;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.FileUtils;
import gde.utils.ObjectKeyCompliance;
import gde.utils.WaitTimer;

/**
 * Graupner HoTT base class for X-8N transmitter saved log files
 */
public class HoTTAdapterX extends HoTTAdapter implements IDevice {

	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public HoTTAdapterX(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * @param deviceConfig
	 */
	public HoTTAdapterX(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

	/**
	 * import device specific *.bin data files
	 */
	@Override
	protected void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT2400), "LogData");

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					HoTTAdapterX.this.application.setPortConnected(true);
					boolean isInitialSwitched = false;

					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						if (!selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
							if (selectedImportFile.contains(GDE.STRING_DOT)) {
								selectedImportFile = selectedImportFile.substring(0, selectedImportFile.indexOf(GDE.CHAR_DOT));
							}
							selectedImportFile = selectedImportFile + GDE.FILE_ENDING_DOT_BIN;
						}
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > MIN_FILENAME_LENGTH) {
							//String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));

							String directoryName = ObjectKeyCompliance.getUpcomingObjectKey(Paths.get(selectedImportFile));
							if (!directoryName.isEmpty()) ObjectKeyCompliance.createObjectKey(directoryName);

							try {
								// use a copy of the picker parameters to avoid changes by the reader
								HoTTbinReaderX.read(selectedImportFile, new PickerParameters(HoTTAdapterX.this.pickerParameters)); //, HoTTAdapter.this, GDE.STRING_EMPTY, channelConfigNumber);
								if (!isInitialSwitched) {
									Channel receiverChannel = HoTTAdapterX.this.channels.get(1);
									HoTTbinReader.channels.switchChannel(receiverChannel.getName());
									isInitialSwitched = true;
								}
								WaitTimer.delay(500);
							}
							catch (Exception e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally  {
					HoTTAdapterX.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		int tmpVoltage, tmpCurrent, tmpRevolution, tmpPackageLoss, tmpVoltageRx, tmpTemperatureRx;

		switch (this.serialPort.protocolType) {
		case TYPE_19200_V4:
			switch (dataBuffer[1]) {
			case HoTTAdapter2.SENSOR_TYPE_RECEIVER_19200:
				if (dataBuffer.length == 17) {
					//0=Rx->Tx-PLoss, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
					//9=SpannungM, 10=SpannungM_min, 11=CurrentM, 12=CurrentM_max, 13=CapacityM, 14=PowerM, 15=RevolutionM, 16=RevolutionM_max
					//17=Temperature, 18=Temperature_max, 19=TemperatureM, 20=TemperatureM_max, 21=Speed, 22=Speed_max
					//23=VoltageExt, 24=VoltageExt_min, 25=TemperatureExt, 26=TemperatureExt_max
					tmpPackageLoss = DataParser.parse2Short(dataBuffer, 11);
					tmpVoltageRx = (dataBuffer[6] & 0xFF);
					tmpTemperatureRx = (dataBuffer[7] & 0xFF) - 20;
					if (!this.pickerParameters.isFilterEnabled || tmpPackageLoss > -1 && tmpVoltageRx > -1 && tmpVoltageRx < 100 && tmpTemperatureRx < 120) {
						points[0] = 0; // seams not part of live data ?? (dataBuffer[15] & 0xFF) * 1000;
						points[1] = (dataBuffer[9] & 0xFF) * 1000;
						points[2] = (dataBuffer[5] & 0xFF) * 1000;
						points[3] = tmpPackageLoss * 1000;
						points[4] = (dataBuffer[13] & 0xFF) * 1000;
						points[5] = (dataBuffer[8] & 0xFF) * 1000;
						points[6] = tmpVoltageRx * 1000;
						points[7] = tmpTemperatureRx * 1000;
						points[8] = (dataBuffer[10] & 0xFF) * 1000;
						//23 VoltageExt not part of serial protocol
						//24 VoltageExt_min not part of serial protocol
						//25 TemperatureExt not part of serial protocol
						//26 TemperatureExt_max not part of serial protocol

					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
				//0=Rx->Tx-PLoss, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
				//9=SpannungM, 10=SpannungM_min, 11=CurrentM, 12=CurrentM_max, 13=CapacityM, 14=PowerM, 15=RevolutionM, 16=RevolutionM_max
				//17=Temperature, 18=Temperature_max, 19=TemperatureM, 20=TemperatureM_max, 21=Speed, 22=Speed_max
				//23=VoltageExt, 24=VoltageExt_min, 25=TemperatureExt, 26=TemperatureExt_max
				if (dataBuffer.length == 57) {
					tmpVoltage = DataParser.parse2Short(dataBuffer, 17);
					tmpCurrent = DataParser.parse2Short(dataBuffer, 21);
					tmpRevolution = DataParser.parse2Short(dataBuffer, 25);
					if (!this.pickerParameters.isFilterEnabled || tmpVoltage > -1 && tmpVoltage < 1000 && tmpCurrent < 2550 && tmpRevolution > -1 && tmpRevolution < 20000) {
						points[9] = tmpVoltage * 1000;
						points[10] = DataParser.parse2Short(dataBuffer, 19) * 1000;
						points[11] = tmpCurrent * 1000;
						points[12] = DataParser.parse2Short(dataBuffer, 23) * 1000;
						points[13] = DataParser.parse2Short(dataBuffer, 29) * 1000;
						points[14] = Double.valueOf(points[9] / 1000.0 * points[11]).intValue(); // power U*I [W];
						points[15] = tmpRevolution * 1000;
						points[16] = DataParser.parse2Short(dataBuffer, 27) * 1000;
						points[17] = 0;
						points[18] = 0;
						points[19] = DataParser.parse2Short(dataBuffer, 33) * 1000;
						points[20] = DataParser.parse2Short(dataBuffer, 35) * 1000;
						points[21] = 0;
						points[22] = 0;
					}
				}
				break;
			}
			break;

		default:
		case TYPE_115200:
			switch (dataBuffer[0]) {
			case HoTTAdapter2.SENSOR_TYPE_RECEIVER_115200:
				if (dataBuffer.length >= 21) {
					//0=Rx->Tx-PLoss, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
					//9=SpannungM, 10=SpannungM_min, 11=CurrentM, 12=CurrentM_max, 13=CapacityM, 14=PowerM, 15=RevolutionM, 16=RevolutionM_max
					//17=Temperature, 18=Temperature_max, 19=TemperatureM, 20=TemperatureM_max, 21=Speed, 22=Speed_max
					//23=VoltageExt, 24=VoltageExt_min, 25=TemperatureExt, 26=TemperatureExt_max
					tmpPackageLoss = DataParser.parse2Short(dataBuffer, 12);
					tmpVoltageRx = dataBuffer[15] & 0xFF;
					tmpTemperatureRx = DataParser.parse2Short(dataBuffer, 10);
					if (!this.pickerParameters.isFilterEnabled || tmpPackageLoss > -1 && tmpVoltageRx > -1 && tmpVoltageRx < 100 && tmpTemperatureRx < 100) {
						this.pickerParameters.reverseChannelPackageLossCounter.add((dataBuffer[5] & 0xFF) == 0 && (dataBuffer[4] & 0xFF) == 0 ? 0 : 1);
						points[0] = this.pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;//(dataBuffer[16] & 0xFF) * 1000;
						points[1] = (dataBuffer[17] & 0xFF) * 1000;
						points[2] = (dataBuffer[14] & 0xFF) * 1000;
						points[3] = tmpPackageLoss * 1000;
						points[4] = (dataBuffer[5] & 0xFF) * 1000;
						points[5] = (dataBuffer[4] & 0xFF) * 1000;
						points[6] = tmpVoltageRx * 1000;
						points[7] = tmpTemperatureRx * 1000;
						points[8] = (dataBuffer[18] & 0xFF) * 1000;
						//23 VoltageExt not part of serial protocol
						//24 VoltageExt_min not part of serial protocol
						//25 TemperatureExt not part of serial protocol
						//26 TemperatureExt_max not part of serial protocol
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
				//0=Rx->Tx-PLoss, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
				//9=SpannungM, 10=SpannungM_min, 11=CurrentM, 12=CurrentM_max, 13=CapacityM, 14=PowerM, 15=RevolutionM, 16=RevolutionM_max
				//17=Temperature, 18=Temperature_max, 19=TemperatureM, 20=TemperatureM_max, 21=Speed, 22=Speed_max
				//23=VoltageExt, 24=VoltageExt_min, 25=TemperatureExt, 26=TemperatureExt_max
				if (dataBuffer.length >= 34) {
					tmpVoltage = DataParser.parse2Short(dataBuffer, 10);
					tmpCurrent = DataParser.parse2Short(dataBuffer, 14);
					tmpRevolution = DataParser.parse2Short(dataBuffer, 18);
					if (!this.pickerParameters.isFilterEnabled || tmpVoltage > -1 && tmpVoltage < 1000 && tmpCurrent < 2550 && tmpRevolution > -1 && tmpRevolution < 20000) {
						points[9] = tmpVoltage * 1000;
						points[10] = DataParser.parse2Short(dataBuffer, 12) * 1000;
						points[11] = tmpCurrent * 1000;
						points[12] = DataParser.parse2Short(dataBuffer, 16) * 1000;
						points[13] = DataParser.parse2Short(dataBuffer, 22) * 1000;
						points[14] = Double.valueOf(points[9] / 1000.0 * points[11]).intValue(); // power U*I [W];
						points[15] = tmpRevolution * 1000;
						points[16] = DataParser.parse2Short(dataBuffer, 20) * 1000;
						points[17] = 0;
						points[18] = 0;
						points[19] = DataParser.parse2Short(dataBuffer, 24) * 1000 * 10;
						points[20] = DataParser.parse2Short(dataBuffer, 26) * 1000 * 10;
						points[21] = 0;
						points[22] = 0;
					}
				}
				break;
			}
			break;
		}
		return points;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		double newValue = 0;

		if (record.getAbstractParent().getChannelConfigNumber() == 2 && (record.getOrdinal() >= 3 && record.getOrdinal() <= 10)) {
			if (this.pickerParameters.isChannelPercentEnabled) {
				if (!record.getUnit().equals("%")) record.setUnit("%");
				factor = 0.250;
				reduction = 1500.0;
				newValue = (value - reduction) * factor;
			}
			else {
				if (!record.getUnit().equals("µsec")) record.setUnit("µsec");
				newValue = (value - reduction) * factor + offset;
			}
		}
		else {
			newValue = (value - reduction) * factor + offset;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		double newValue = 0;

		if (record.getAbstractParent().getChannelConfigNumber() == 2 && (record.getOrdinal() >= 3 && record.getOrdinal() <= 10)) {
			if (this.pickerParameters.isChannelPercentEnabled) {
				if (!record.getUnit().equals("%")) record.setUnit("%");
				factor = 0.250;
				reduction = 1500.0;
				newValue = value / factor + reduction;
			}
			else {
				if (!record.getUnit().equals("µsec")) record.setUnit("µsec");
				newValue = (value - reduction) * factor;
			}
		}
		else {
			newValue = (value - offset) / factor + reduction;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

}
