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
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.GDE;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.exception.DataInconsitsentException;
import gde.log.Level;
import gde.utils.StringHelper;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;

import javax.xml.bind.JAXBException;

/**
 * GPS-Logger2 device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class GPSLogger2 extends GPSLogger {
	//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
	//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track;
	//SMGPS2 15=AccelerationX 16=AccelerationY 17=AccelerationZ 18=ENL 19=Impulse
	//CH1-UniLog
	//Unilog 20=voltage_UL 21=current_UL 22=power_UL 23=revolution_UL 24=voltageRx_UL 25=altitude_UL 26=a1_UL 27=a2_UL 27=a3_UL;
	//M-LINK 28=valAdd00 29=valAdd01 30=valAdd02 31=valAdd03 32=valAdd04 33=valAdd05 34=valAdd06 35=valAdd07 36=valAdd08 37=valAdd09 38=valAdd10 39=valAdd11 40=valAdd12 41=valAdd13 42=valAdd14;
	//CH2-UniLog2
	//Unilog2 20=voltage_UL 21=current_UL2 22=capacity_UL2 23=power_UL2 24=energy_UL2 25=balance_UL 26=cellVoltage1 27=cellVolt2_ul 28=cellVolltage3_UL 29=cellVoltage4_UL 30=cellVoltage5_UL 31=cellVoltage6_UL 32=revolution_UL 33=a1_UL 34=a2_UL 35=a3_UL 36=temp_UL;
	//M-LINK 37=valAdd00 38=valAdd01 39=valAdd02 40=valAdd03 41=valAdd04 42=valAdd05 43=valAdd06 44=valAdd07 45=valAdd08 46=valAdd09 47=valAdd10 48=valAdd11 49=valAdd12 50=valAdd13 51=valAdd14;
	//begin FW1.26 GDE 3.4.9
	//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
	//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
	//SMGPS2 17=AccelerationX 18=AccelerationY 19=AccelerationZ 20=ENL 21=Impulse 22=AirSpeed 23=pressure static 24=pressure TEK 25=climb TEK
	//CH1-UniLog
	//Unilog 26=voltage_UL 27=current_UL 28=power_UL 29=revolution_UL 30=voltageRx_UL 31=altitude_UL 32=a1_UL 33=a2_UL 34=a3_UL;
	//M-LINK 35=valAdd00 36=valAdd01 37=valAdd02 38=valAdd03 39=valAdd04 40=valAdd05 41=valAdd06 42=valAdd07 43=valAdd08 44=valAdd09 45=valAdd10 46=valAdd11 47=valAdd12 48=valAdd13 49=valAdd14;
	//CH2-UniLog2
	//Unilog2 26=voltage_UL 27=current_UL2 28=capacity_UL2 29=power_UL2 30=energy_UL2 31=balance_UL 32=cellVoltage1 33=cellVolt2_ul 34=cellVolltage3_UL 35=cellVoltage4_UL 36=cellVoltage5_UL 37=cellVoltage6_UL 38=revolution_UL 39=a1_UL 40=a2_UL 41=a3_UL 42=temp_UL;
	//M-LINK 43=valAdd00 44=valAdd01 45=valAdd02 46=valAdd03 47=valAdd04 48=valAdd05 49=valAdd06 50=valAdd07 51=valAdd08 52=valAdd09 53=valAdd10 54=valAdd11 55=valAdd12 56=valAdd13 57=valAdd14;

	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public GPSLogger2(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * @param deviceConfig
	 */
	public GPSLogger2(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

	/**
	 * check and adapt stored measurement properties against actual record set records which gets created by device properties XML
	 * - calculated measurements could be later on added to the device properties XML
	 * - devices with battery cell voltage does not need to all the cell curves which does not contain measurement values
	 * @param fileRecordsProperties - all the record describing properties stored in the file
	 * @param recordSet - the record sets with its measurements build up with its measurements from device properties XML
	 * @return string array of measurement names which match the ordinal of the record set requirements to restore file record properties
	 */
	public String[] crossCheckMeasurements(String[] fileRecordsProperties, RecordSet recordSet) {
		// check for device file contained record properties which are not contained in actual configuration
		// firmware < 126 Ch1=42 measurements, CH2=51 measurements
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track;
		//SMGPS2 15=AccelerationX 16=AccelerationY 17=AccelerationZ 18=ENL 19=Impulse
		//CH1-UniLog
		//Unilog 20=voltage_UL 21=current_UL 22=power_UL 23=revolution_UL 24=voltageRx_UL 25=altitude_UL 26=a1_UL 27=a2_UL 28=a3_UL;
		//M-LINK 29=valAdd00 30=valAdd01 31=valAdd02 32=valAdd03 33=valAdd04 34=valAdd05 35=valAdd06 36=valAdd07 37=valAdd08 38=valAdd09 39=valAdd10 40=valAdd11 41=valAdd12 42=valAdd13 43=valAdd14;
		//CH2-UniLog2
		//Unilog2 20=voltage_UL 21=current_UL2 22=capacity_UL2 23=power_UL2 24=energy_UL2 25=balance_UL 26=cellVoltage1 27=cellVolt2_ul 28=cellVolltage3_UL 29=cellVoltage4_UL 30=cellVoltage5_UL 31=cellVoltage6_UL 32=revolution_UL 33=a1_UL 34=a2_UL 35=a3_UL 36=temp_UL;
		//M-LINK 37=valAdd00 38=valAdd01 39=valAdd02 40=valAdd03 41=valAdd04 42=valAdd05 43=valAdd06 44=valAdd07 45=valAdd08 46=valAdd09 47=valAdd10 48=valAdd11 49=valAdd12 50=valAdd13 51=valAdd14;
		//GDE 3.4.9 begin FW1.26 Ch1=49 measurements, Ch2=57 measurements
		//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
		//SMGPS2 17=AccelerationX 18=AccelerationY 19=AccelerationZ 20=ENL 21=Impulse 22=AirSpeed 23=pressure static 24=pressure TEK 25=climb TEK
		//CH1-UniLog
		//Unilog 26=voltage_UL 27=current_UL 28=power_UL 29=revolution_UL 30=voltageRx_UL 31=altitude_UL 32=a1_UL 33=a2_UL 34=a3_UL;
		//M-LINK 35=valAdd00 36=valAdd01 37=valAdd02 38=valAdd03 39=valAdd04 40=valAdd05 41=valAdd06 42=valAdd07 43=valAdd08 44=valAdd09 45=valAdd10 46=valAdd11 47=valAdd12 48=valAdd13 49=valAdd14;
		//CH2-UniLog2
		//Unilog2 26=voltage_UL 27=current_UL2 28=capacity_UL2 29=power_UL2 30=energy_UL2 31=balance_UL 32=cellVoltage1 33=cellVolt2_ul 34=cellVolltage3_UL 35=cellVoltage4_UL 36=cellVoltage5_UL 37=cellVoltage6_UL 38=revolution_UL 39=a1_UL 40=a2_UL 41=a3_UL 42=temp_UL;
		//M-LINK 43=valAdd00 44=valAdd01 45=valAdd02 46=valAdd03 47=valAdd04 48=valAdd05 49=valAdd06 50=valAdd07 51=valAdd08 52=valAdd09 53=valAdd10 54=valAdd11 55=valAdd12 56=valAdd13 57=valAdd14;

		String[] recordKeys = recordSet.getRecordNames();

		StringBuilder sb = new StringBuilder().append(GDE.LINE_SEPARATOR);

		Vector<String> cleanedRecordNames = new Vector<String>();
		//incoming filePropertiesRecordNames may mismatch recordKeyNames, but addNoneCalculation will use original name
		Vector<String> noneCalculationRecordNames = new Vector<String>();

		try {
			switch (fileRecordsProperties.length) {
			case 20: //Android GPS-Logger2/3 < 1.5.2
			case 29: //Android GPS-Logger2/3_UL < 1.5.2
			case 37: //Android GPS-Logger2/3_UL2 < 1.5.2
			case 26: //Android GPS-Logger2/3 >=1.5.2
			case 35: //Android GPS-Logger2/3_UL >=1.5.2
			case 43: //Android GPS-Logger2/3_UL2 >=1.5.2
				return super.crossCheckMeasurements(fileRecordsProperties, recordSet);
				
			default: //GDE handling
				if (fileRecordsProperties.length != recordKeys.length) {
					//GDE 3.4.9 firmware < 126 Ch1=44 measurements, CH2=52 measurements
					//begin FW1.26 Ch1=50 measurements, Ch2=58 measurements
					//SMGPS  added 15=GlideRatio 16=SpeedGlideRatio;
					//SMGPS2 added 22=AirSpeed 23=pressure static 24=pressure TEK 25=climb TEK
					for (int i = 0, j = 0; i < recordKeys.length; i++) {
						switch (i) {
						case 15: //GlideRatio
						case 16: //SpeedGlideRatio
						case 22: //AirSpeed
						case 23: //pressure static
						case 24: //pressure TEK
						case 25: //climb TEK
							sb.append(String.format("%02d added measurement set to isCalculation=true -> %s\n", i, recordKeys[i]));
							recordSet.get(i).setActive(null);
							break;
						default:
							if (j < fileRecordsProperties.length) {
								HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
								sb.append(String.format("%02d %19s match %19s isAvtive = %s\n", i, recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
								cleanedRecordNames.add(recordKeys[i]);
								noneCalculationRecordNames.add(recordProps.get(Record.NAME));
								if (fileRecordsProperties[j].contains("_isActive=false")) recordSet.get(i).setActive(false);
								++j;
							}
							else {//some Android saved record sets contain less fileRecordsProperties, mark rest as calculation
								sb.append(String.format("%02d added measurement set to isCalculation=true -> %s\n", i, recordKeys[i]));
								recordSet.get(i).setActive(null);
							}
							break;
							}
						}
					}
					else { //already adapted record set stored
						for (int i = 0; i < recordKeys.length; i++) {
							if (!fileRecordsProperties[i].contains("_isActive")) {
								sb.append(String.format("%02d added measurement set to isCalculation=true -> %s\n", i, recordKeys[i]));
								recordSet.get(i).setActive(null);
								cleanedRecordNames.add(recordKeys[i]);
							}
							else {
								HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[i], Record.DELIMITER, Record.propertyKeys);
								sb.append(String.format("%02d %19s match %19s isAvtive = %s\n", i, recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
								cleanedRecordNames.add(recordKeys[i]);
								noneCalculationRecordNames.add(recordProps.get(Record.NAME));
								if (fileRecordsProperties[i].contains("_isActive=false")) recordSet.get(i).setActive(false);
							}
						}
					}
					break;
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, String.format("recordKey to fileRecordsProperties mismatch, check:\n %s \nfileRecordsProperties.length = %d recordKeys.length = %d %s", e.getMessage(),
					fileRecordsProperties.length, recordKeys.length, sb.toString()));
		}

		recordKeys = cleanedRecordNames.toArray(new String[1]);
		//incoming filePropertiesRecordNames may mismatch recordKeyNames, but addNoneCalculation will use original incoming name
		recordSet.setNoneCalculationRecordNames(noneCalculationRecordNames.toArray(new String[1]));

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, sb.toString());

		return recordKeys;
	}

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal business to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.getNoneCalculationRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		Vector<Integer> timeStamps = new Vector<Integer>(1, 1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		byte[] timeStampBuffer = new byte[timeStampBufferSize];
		if (!recordSet.isTimeStepConstant()) {
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8)
						+ ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
			}
		}
		log.log(java.util.logging.Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString()); //$NON-NLS-1$

		for (int i = 0; i < recordDataSize; i++) {
			log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
			//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
			//SMGPS2 17=AccelerationX 18=AccelerationY 19=AccelerationZ 20=ENL 21=Impulse 22=AirSpeed 23=pressure static 24=pressure TEK 25=climb TEK
			//CH1-UniLog
			//Unilog 26=voltage_UL 27=current_UL 28=power_UL 29=revolution_UL 30=voltageRx_UL 31=altitude_UL 32=a1_UL 33=a2_UL 34=a3_UL;
			//M-LINK 35=valAdd00 36=valAdd01 37=valAdd02 38=valAdd03 39=valAdd04 40=valAdd05 41=valAdd06 42=valAdd07 43=valAdd08 44=valAdd09 45=valAdd10 46=valAdd11 47=valAdd12 48=valAdd13 49=valAdd14;
			//CH2-UniLog2
			//Unilog2 26=voltage_UL 27=current_UL2 28=capacity_UL2 29=power_UL2 30=energy_UL2 31=balance_UL 32=cellVoltage1 33=cellVolt2_ul 34=cellVolltage3_UL 35=cellVoltage4_UL 36=cellVoltage5_UL 37=cellVoltage6_UL 38=revolution_UL 39=a1_UL 40=a2_UL 41=a3_UL 42=temp_UL;
			//M-LINK 43=valAdd00 44=valAdd01 45=valAdd02 46=valAdd03 47=valAdd04 48=valAdd05 49=valAdd06 50=valAdd07 51=valAdd08 52=valAdd09 53=valAdd10 54=valAdd11 55=valAdd12 56=valAdd13 57=valAdd14;
		for (int j = 0; j < points.length; j++) {
				points[j] = (((convertBuffer[0 + (j * 4)] & 0xff) << 24) + ((convertBuffer[1 + (j * 4)] & 0xff) << 16) + ((convertBuffer[2 + (j * 4)] & 0xff) << 8) + ((convertBuffer[3 + (j * 4)] & 0xff) << 0));
			}

			if (recordSet.isTimeStepConstant())
				recordSet.addNoneCalculationRecordsPoints(points);
			else
				recordSet.addNoneCalculationRecordsPoints(points, timeStamps.get(i) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		this.updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
	}

}
