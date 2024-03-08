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
    
    Copyright (c) 2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

/**
 * Graupner HoTT device class with Mikrokopter adaptation, GPS coordinates in decimal degrees instead of degree decimal mintes
 * @author Winfried Brügmann
 */
public class HoTTAdapter2M extends HoTTAdapter2 {

	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public HoTTAdapter2M(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * @param deviceConfig
	 */
	public HoTTAdapter2M(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
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
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				int ordinal = record.getOrdinal();
				//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
				//10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
				//15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
				//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
				//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
				//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M

				//73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
				//93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M

				if (ordinal == 15 || ordinal == 16) { //15=Latitude, 16=Longitude 
					dataTableRow[index + 1] = String.format("%02.7f", record.realGet(rowIndex) / 1000000.0); //$NON-NLS-1$
				}
				else if (ordinal >= 0 && ordinal <= 5){
					dataTableRow[index + 1] = String.format("%.0f",(record.realGet(rowIndex) / 1000.0));
				}
				else {
					dataTableRow[index + 1] = record.getDecimalFormat().format((offset + ((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
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

		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M

		//73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
		//93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M

		if (record.getOrdinal() == 15 || record.getOrdinal() == 16) { //15=Latitude, 16=Longitude
			newValue = value / 1000.0;
		}
		else if (record.getAbstractParent().getChannelConfigNumber() == 4 && (record.getOrdinal() >= 73 && record.getOrdinal() <= 88)) {
			if (this.pickerParameters.isChannelPercentEnabled) {
				if (!record.getUnit().equals("%")) record.setUnit("%");
				factor = 0.250;
				reduction = 1500.0;
				newValue = (value - reduction) * factor;
			}
			else {
				if (!record.getUnit().equals("µsec")) record.setUnit("µsec");
				newValue = (value - reduction) * factor;
			}
		}
		else {
			newValue = (value - reduction) * factor + offset;
		}

		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M

		//73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
		//93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M

		if (record.getOrdinal() == 15 || record.getOrdinal() == 16) { // 15=Latitude, 16=Longitude
			newValue = value * 1000.0;
		}
		else if (record.getAbstractParent().getChannelConfigNumber() == 6 && (record.getOrdinal() >= 73 && record.getOrdinal() <= 88)) {
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

		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

}
