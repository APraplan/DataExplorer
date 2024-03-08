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

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.log.Level;

/**
 * Graupner HoTT device class with Mikrokopter adaptation, GPS coordinates in decimal degrees instead of degree decimal mintes
 * @author Winfried Brügmann
 */
public class HoTTAdapterM extends HoTTAdapter {
	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public HoTTAdapterM(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public HoTTAdapterM(DeviceConfiguration deviceConfig) {
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
				//0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb, 5=Velocity, 6=Distance, 7=Direction, 8=TripDistance, 9=VoltageRx, 10=TemperatureRx
				if ((ordinal == 1 || ordinal == 2) && record.getParent().getChannelConfigNumber() == 3) { // 1=GPS-longitude 2=GPS-latitude
					dataTableRow[index + 1] = String.format("%02.7f", record.realGet(rowIndex) / 1000000.0); //$NON-NLS-1$
				}
				//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx
				else if (ordinal >= 0 && ordinal <= 5 && record.getParent().getChannelConfigNumber() == 1) { //Receiver
					dataTableRow[index + 1] = String.format("%.0f", (record.realGet(rowIndex) / 1000.0));
				}
				else {
					dataTableRow[index + 1] = record.getDecimalFormat().format((offset + ((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
				}
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
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

		if (record.getAbstractParent().getChannelConfigNumber() == 3 && (record.getOrdinal() == 1 || record.getOrdinal() == 2)) { // 1=GPS-longitude 2=GPS-latitude
			//0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			newValue = value / 1000.0;
		}
		else if (record.getAbstractParent().getChannelConfigNumber() == 6 && (record.getOrdinal() >= 3 && record.getOrdinal() <= 18)) {
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

		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

		if ((record.getOrdinal() == 1 || record.getOrdinal() == 2) && record.getAbstractParent().getChannelConfigNumber() == 3) { // 1=GPS-longitude 2=GPS-latitude  )
			//0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			newValue = value * 1000.0;
		}
		else if (record.getAbstractParent().getChannelConfigNumber() == 6 && (record.getOrdinal() >= 3 && record.getOrdinal() <= 18)) {
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

		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}
}
