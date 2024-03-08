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

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.widgets.FileDialog;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.data.Channel;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.StatisticsType;
import gde.device.graupner.hott.MessageIds;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.FileUtils;
import gde.utils.ObjectKeyCompliance;
import gde.utils.WaitTimer;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class HoTTAdapterD extends HoTTAdapter2 implements IDevice {
	final static Logger									log														= Logger.getLogger(HoTTAdapterD.class.getName());

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public HoTTAdapterD(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		if (this.application.getMenuToolBar() != null) {
			String toolTipText = HoTTAdapter.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}

		setPickerParameters();
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public HoTTAdapterD(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		if (this.application.getMenuToolBar() != null) {
			String toolTipText = HoTTAdapter.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}

		setPickerParameters();
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
					boolean isInitialSwitched = false;
					HoTTAdapterD.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						if (!selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_BIN) && !selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
							log.log(Level.WARNING, String.format("skip selectedImportFile %s since it has not a supported file ending", selectedImportFile));
						}
						log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > MIN_FILENAME_LENGTH) {
							//String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));

							String directoryName = ObjectKeyCompliance.getUpcomingObjectKey(Paths.get(selectedImportFile));
							if (!directoryName.isEmpty()) ObjectKeyCompliance.createObjectKey(directoryName);

							try {
								// use a copy of the picker parameters to avoid changes by the reader
								if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
									HoTTbinReaderD.read(selectedImportFile, new PickerParameters(HoTTAdapterD.this.pickerParameters));
								}
								else if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
									HoTTlogReaderD.read(selectedImportFile, new PickerParameters(HoTTAdapterD.this.pickerParameters));
								}
								if (!isInitialSwitched) {
									Channel activeChannel = HoTTAdapterD.this.application.getActiveChannel();
									HoTTbinReaderD.channels.switchChannel(activeChannel.getName());
									if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
										activeChannel.switchRecordSet(HoTTbinReaderD.recordSet.getName());
									}
									else if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
										activeChannel.switchRecordSet(HoTTlogReaderD.recordSet.getName());
									}
									isInitialSwitched = true;
								}
								else {
									HoTTAdapterD.this.makeInActiveDisplayable(HoTTbinReaderD.recordSet);
								}
								WaitTimer.delay(500);
							}
							catch (Exception e) {
								log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally  {
					HoTTAdapterD.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
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
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=Ch17 ... 118=Ch32, 119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
		// ESC1
		// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
		// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
		// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
		// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC

		// 239=Test 00 240=Test 01.. 251=Test 12
		final int latOrdinal = 20, lonOrdinal = 21;
		if (record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal) { //15=Latitude, 16=Longitude
			int grad = ((int) (value / 1000));
			double minuten = (value - (grad * 1000.0)) / 10.0;
			newValue = grad + minuten / 60.0;
		}
		else if (record.getOrdinal() >= 87 && record.getOrdinal() <= 118 && value != 0.) {
			if (this.pickerParameters.isChannelPercentEnabled) {
				if (!record.getUnit().equals("%")) record.setUnit("%");
				factor = 0.250;
				reduction = 1500.0;
				newValue = (value - reduction) * factor + 0.001;
			}
			else {
				if (!record.getUnit().equals("µsec")) record.setUnit("µsec");
				newValue = (value - reduction) * factor;
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
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=Ch17 ... 118=Ch32, 119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
		// ESC1
		// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
		// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
		// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
		// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC

		// 239=Test 00 240=Test 01.. 251=Test 12
		final int latOrdinal = 20, lonOrdinal = 21;
		if (record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal) { // 20=Latitude, 21=Longitude
			int grad = (int) value;
			double minuten = (value - grad * 1.0) * 60.0;
			newValue = (grad + minuten / 100.0) * 1000.0;
		}
		else if (record.getOrdinal() >= 87 && record.getOrdinal() <= 118 && value != 0.) {
			if (this.pickerParameters.isChannelPercentEnabled) {
				if (!record.getUnit().equals("%")) record.setUnit("%");
				factor = 0.250;
				reduction = 1500.0;
				newValue = value / factor + reduction - 0.001;
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


	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				int ordinal = record.getOrdinal();
				// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
				// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
				// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
				// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
				// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
				// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
				// 57=LowestCellNumber, 58=Pressure, 59=Event G
				// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
				// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
				// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=Ch17 ... 118=Ch32, 119=PowerOff, 120=BatterieLow, 121=Reset, 122=warning
				// ESC1
				// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
				// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
				// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
				// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC

				// 239=Test 00 240=Test 01.. 251=Test 12
				if (ordinal >= 0 && ordinal <= 5) {
					dataTableRow[index + 1] = String.format("%.0f", (record.realGet(rowIndex) / 1000.0)); //$NON-NLS-1$
				}
				else if (ordinal == 122 && record.getUnit().equals(GDE.STRING_EMPTY)) { //Warning
					dataTableRow[index + 1] = record.realGet(rowIndex) == 0
							? GDE.STRING_EMPTY
									: String.format("'%c'", ((record.realGet(rowIndex) / 1000)+64));
				}
				//RCE Sparrow 33=GPS hh:mm 34=GPS sss.SSS 
				else if (ordinal == 33 && record.getUnit().endsWith("HH:mm:ss.SSS")) { 
					dataTableRow[index + 1] = HoTTAdapter.getFormattedTime(record.realGet(rowIndex));
				}
				else if (ordinal == 34 && record.getUnit().endsWith("yy-MM-dd")) {
					dataTableRow[index + 1] = HoTTAdapter.getFormattedDate(record.realGet(rowIndex)/10);
				}
				else {
					dataTableRow[index + 1] = record.getFormattedTableValue(rowIndex);
				}
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * update the record set ESC dependent record meta data
	 * @param version detected in byte buffer
	 * @param device HoTTAdapter
	 * @param tmpRecordSet the record set to be updated
	 */
	protected static void updateEscTypeDependent(int version, IDevice device, RecordSet tmpRecordSet, int numESC) {
		int channelConfigNumber = tmpRecordSet.getChannelConfigNumber();
		int offsetNumberESC = (numESC - 1) * 29; 
		int channelOffset = 36 + offsetNumberESC;
		if (version == 3) { //YGE
			// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
			// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
			tmpRecordSet.get(100 + channelOffset).setName(device.getMeasurementReplacement("speed") + " M" + numESC);
			tmpRecordSet.get(100 + channelOffset).setUnit("km/h");
			device.getMeasurement(channelConfigNumber, 100 + channelOffset).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(101 + channelOffset).setName(device.getMeasurementReplacement("speed") + " M" + numESC + "_max");
			tmpRecordSet.get(101 + channelOffset).setUnit("km/h");
			tmpRecordSet.get(102 + channelOffset).setName("PWM M" + numESC);
			tmpRecordSet.get(102 + channelOffset).setUnit("%");
			tmpRecordSet.get(103 + channelOffset).setName(device.getMeasurementReplacement("throttle") + " M" + numESC);
			tmpRecordSet.get(103 + channelOffset).setUnit("%");
			tmpRecordSet.get(104 + channelOffset).setName(device.getMeasurementReplacement("voltage_bec") + " M" + numESC);
			tmpRecordSet.get(104 + channelOffset).setUnit("V");
			tmpRecordSet.get(104 + channelOffset).setFactor(0.1);
			device.getMeasurement(channelConfigNumber, 104 + channelOffset).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(104 + channelOffset).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 6); //$NON-NLS-1$
			tmpRecordSet.get(105 + channelOffset).setName(device.getMeasurementReplacement("voltage_bec") + " M" + numESC + "_min");
			tmpRecordSet.get(105 + channelOffset).setUnit("V");
			tmpRecordSet.get(105 + channelOffset).setFactor(0.1);
			tmpRecordSet.get(105 + channelOffset).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 6); //$NON-NLS-1$
			tmpRecordSet.get(106 + channelOffset).setName(device.getMeasurementReplacement("current_bec") + " M" + numESC);
			tmpRecordSet.get(106 + channelOffset).setUnit("A");
			tmpRecordSet.get(106 + channelOffset).setFactor(0.1);
			device.getMeasurement(channelConfigNumber, 106 + channelOffset).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(107 + channelOffset).setName(device.getMeasurementReplacement("temperature_bec") + " M" + numESC);
			tmpRecordSet.get(107 + channelOffset).setUnit("°C");
			tmpRecordSet.get(107 + channelOffset).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 7); //$NON-NLS-1$
			device.getMeasurement(channelConfigNumber, 107 + channelOffset).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(108 + channelOffset).setName(device.getMeasurementReplacement("temperature_capacitor") + " M" + numESC);
			tmpRecordSet.get(108 + channelOffset).setUnit("°C");
			tmpRecordSet.get(108 + channelOffset).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 7); //$NON-NLS-1$
			device.getMeasurement(channelConfigNumber, 108 + channelOffset).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(109 + channelOffset).setName(device.getMeasurementReplacement("timing") + " M" + numESC);
			tmpRecordSet.get(109 + channelOffset).setUnit("°");
			tmpRecordSet.get(110 + channelOffset).setName(device.getMeasurementReplacement("temperature") + " M" + numESC + "_3");
			tmpRecordSet.get(110 + channelOffset).setUnit("°C");
			tmpRecordSet.get(110 + channelOffset).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 7); //$NON-NLS-1$
			device.getMeasurement(channelConfigNumber, 110 + channelOffset).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(111 + channelOffset).setName(device.getMeasurementReplacement("gear") + " M" + numESC);
			tmpRecordSet.get(111 + channelOffset).setUnit("");
			tmpRecordSet.get(112 + channelOffset).setName("YGEGenExt M" + numESC);
			tmpRecordSet.get(112 + channelOffset).setUnit("");
			tmpRecordSet.get(113 + channelOffset).setName("MotStatEscNr M" + numESC);
			tmpRecordSet.get(113 + channelOffset).setUnit("#");
		}
		else if (version >= 128) { //CS-Electronics 
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
			// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
			// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
			// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
			// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
			// 57=LowestCellNumber, 58=Pressure, 59=Event G
			// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
			// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
			// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
			// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
			// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
			// 120=AirSpeed 121=AirSpeed_max 122=PWM 123=Throttle 124=VoltagePump 125=VoltagePump_min 126=Flow 127=Fuel 128=Power 
			// 129=Thrust 130=TemperaturePump 131=EngineStat 132=spare 133=spare 134=spare 135=version
			
			// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
			// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M
			tmpRecordSet.get(87 + channelOffset).setName(device.getMeasurementReplacement("voltage") + " ECU" + numESC);
			tmpRecordSet.get(88 + channelOffset).setName(device.getMeasurementReplacement("current") + " ECU" + numESC);
			tmpRecordSet.get(89 + channelOffset).setName(device.getMeasurementReplacement("capacity") + " ECU" + numESC);
			tmpRecordSet.get(90 + channelOffset).setName(device.getMeasurementReplacement("power") + " ECU" + numESC);
			tmpRecordSet.get(91 + channelOffset).setName(device.getMeasurementReplacement("revolution") + " ECU" + numESC);
			tmpRecordSet.get(92 + channelOffset).setName(device.getMeasurementReplacement("temperature") + " EGT" + numESC + " 1");
			tmpRecordSet.get(93 + channelOffset).setName(device.getMeasurementReplacement("temperature") + " EGT" + numESC + " 2");
			tmpRecordSet.get(94 + channelOffset).setName(device.getMeasurementReplacement("voltage") + " ECU" + numESC + "_min");
			tmpRecordSet.get(95 + channelOffset).setName(device.getMeasurementReplacement("current") + " ECU" + numESC + "_max");
			tmpRecordSet.get(97 + channelOffset).setName(device.getMeasurementReplacement("temperature") + " EGT" + numESC + " 1_max");
			tmpRecordSet.get(98 + channelOffset).setName(device.getMeasurementReplacement("temperature") + " EGT" + numESC + " 2_max");
			tmpRecordSet.get(99 + channelOffset).setName(device.getMeasurementReplacement("event") + " ECU" + numESC);			

			tmpRecordSet.get(100 + channelOffset).setName(device.getMeasurementReplacement("air_speed") + " M" + numESC);
			tmpRecordSet.get(100 + channelOffset).setUnit("km/h");
			device.getMeasurement(channelConfigNumber, 100 + channelOffset).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(101 + channelOffset).setName(device.getMeasurementReplacement("air_speed") + " M" + numESC + "_max");
			tmpRecordSet.get(101 + channelOffset).setUnit("km/h");
			tmpRecordSet.get(102 + channelOffset).setName("PWM M");
			tmpRecordSet.get(102 + channelOffset).setUnit("%");
			tmpRecordSet.get(103 + channelOffset).setName(device.getMeasurementReplacement("throttle") + " M" + numESC);
			tmpRecordSet.get(103 + channelOffset).setUnit("%");
			tmpRecordSet.get(104 + channelOffset).setName(device.getMeasurementReplacement("voltage_pump") + " M" + numESC);
			tmpRecordSet.get(104 + channelOffset).setUnit("V");
			tmpRecordSet.get(104 + channelOffset).setFactor(0.1);
			device.getMeasurement(channelConfigNumber, 104 + channelOffset).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(105 + channelOffset).setName(device.getMeasurementReplacement("voltage_pump") + " M" + numESC + "_min");
			tmpRecordSet.get(105 + channelOffset).setUnit("V");
			tmpRecordSet.get(105 + channelOffset).setFactor(0.1);
			tmpRecordSet.get(106 + channelOffset).setName(device.getMeasurementReplacement("flow") + " M" + numESC);
			tmpRecordSet.get(106 + channelOffset).setUnit("ml/min");
			device.getMeasurement(channelConfigNumber, 106 + channelOffset).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(107 + channelOffset).setName(device.getMeasurementReplacement("fuel") + " M" + numESC);
			tmpRecordSet.get(107 + channelOffset).setUnit("ml");
			device.getMeasurement(channelConfigNumber, 107 + channelOffset).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(108 + channelOffset).setName(device.getMeasurementReplacement("power") + " M" + numESC);
			tmpRecordSet.get(108 + channelOffset).setUnit("W");
			device.getMeasurement(channelConfigNumber, 108 + channelOffset).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(109 + channelOffset).setName(device.getMeasurementReplacement("thrust") + " M" + numESC);
			tmpRecordSet.get(109 + channelOffset).setUnit("N");
			tmpRecordSet.get(110 + channelOffset).setName(device.getMeasurementReplacement("temperature_pump") + " M" + numESC);
			tmpRecordSet.get(110 + channelOffset).setUnit("°C");
			device.getMeasurement(channelConfigNumber, 110 + channelOffset).setStatistics(StatisticsType.fromString("min=true max=true avg=true sigma=false"));
			tmpRecordSet.get(111 + channelOffset).setName(device.getMeasurementReplacement("engine") + " M" + numESC);
			tmpRecordSet.get(111 + channelOffset).setUnit("");
		}
	}

}