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
    
    Copyright (c) 2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.junsi;

import gde.GDE;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.log.Level;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

public class iCharger3010B extends iCharger {

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public iCharger3010B(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public iCharger3010B(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
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
		int deviceDataBufferSize = this.getLovDataByteSize();
		int[] points = new int[this.getNumberOfMeasurements(1)];
		int offset = 0;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		byte[] convertBuffer = new byte[deviceDataBufferSize];

		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			lovDataSize = deviceDataBufferSize/3;
			//prepare convert buffer for conversion
			System.arraycopy(dataBuffer, offset, convertBuffer, 0, deviceDataBufferSize/3);
			for (int j = deviceDataBufferSize/3; j < deviceDataBufferSize; j++) { //start at minimum length of data buffer 
				convertBuffer[j] = dataBuffer[offset+j];
				++lovDataSize;
				if (dataBuffer[offset+j] == 0x0A && dataBuffer[offset+j-1] == 0x0D)
					break;
			}

			recordSet.addPoints(convertDataBytes(points, convertBuffer));
			offset += lovDataSize+8;

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}

		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {		
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		// prepare the serial CSV data parser
		DataParser data = new  DataParser(this.getDataBlockTimeUnitFactor(), this.getDataBlockLeader(), this.getDataBlockSeparator().value(), null, null, Math.abs(this.getDataBlockSize(InputTypes.FILE_IO)), this.getDataBlockFormat(InputTypes.SERIAL_IO), false);
		int[] startLength = new int[] {0,0};
		byte[] lineBuffer = null;
		double timeStep_h = 2.0 / 3600.0;
				
		try {
			setDataLineStartAndLength(dataBuffer, startLength);
			lineBuffer = new byte[startLength[1]];
			System.arraycopy(dataBuffer, startLength[0], lineBuffer, 0, startLength[1]);
			data.parse(new String(lineBuffer), 1);
			int[] values = data.getValues();
//			System.out.println(StringHelper.arrayToString(values));
//			13703, 32911, 219, 3704, 3679, 3691, 3710, 3640, 3606, 3573, 3569, 3586, 0, 333, 200, 30, 0, 
//			13683, 32933, 220, 3708, 3682, 3694, 3712, 3642, 3609, 3576, 3571, 3591, 0, 333, 198, 31, 0, 
//			13683, 32951, 220, 3709, 3682, 3696, 3712, 3643, 3610, 3581, 3574, 3593, 0, 333, 200, 33, 0, 

			int cellOffset = this.getNumberOfLithiumCells() + 2;
			
			//0=VersorgungsSpg. 1=Spannung 2=Strom 3=Ladung 
			points[0] = values[0];
			points[1] = values[1];
			points[2] = values[2];			
			points[3] = values[cellOffset + 3] * 1000;
			//4=Leistung 5=Energie
			points[4] = Double.valueOf(points[1] * points[2] / 100.0).intValue(); 							// power U*I [W]
			if (points[3] != 0) {
				energySum += points[4] * timeStep_h; //energy = energy + (timeDelta * power)
				points[5] = Double.valueOf(energySum).intValue();
			} else {
				points[5] = 0;
				energySum = 0.0;
			}
			//6=Temp.intern 7=Temp.extern 
			points[6] = values[cellOffset + 1];
			points[7] = values[cellOffset + 2];
			//9=SpannungZelle1 10=SpannungZelle2 11=SpannungZelle3 12=SpannungZelle4 13=SpannungZelle5 14=SpannungZelle6 ... 18=SpannungZelle10
			for (int i = 0; i < this.getNumberOfLithiumCells(); i++) {
				points[i+9] = values[i+3];
				if (points[i + 9] > 0) {
					maxVotage = points[i + 9] > maxVotage ? points[i + 9] : maxVotage;
					minVotage = points[i + 9] < minVotage ? points[i + 9] : minVotage;
				}
			}
			//8=Balance
			points[8] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
		}
		return points;
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
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.size()];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		double energy = 0.0;
		double timeStep_h = 2.0 / 3600.0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		for (int i = 0; i < recordDataSize; i++) {
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);
			
			//0=VersorgungsSpg. 1=Spannung 2=Strom 3=Ladung  
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			points[3] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
			//4=Leistung 5=Energie
			points[4] = Double.valueOf((points[1] / 1000.0) * (points[2] / 1000.0) * 10000).intValue();	// power U*I [W]
			if (i != 0) {
				energy += points[4] * timeStep_h; //energy = energy + (timeDelta * power)
			}
			points[5] = Double.valueOf(energy).intValue();
			//6=Temp.intern 7=Temp.extern 
			points[6] = (((convertBuffer[16]&0xff) << 24) + ((convertBuffer[17]&0xff) << 16) + ((convertBuffer[18]&0xff) << 8) + ((convertBuffer[19]&0xff) << 0));
			points[7] = (((convertBuffer[20]&0xff) << 24) + ((convertBuffer[21]&0xff) << 16) + ((convertBuffer[22]&0xff) << 8) + ((convertBuffer[23]&0xff) << 0));

			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			//7=SpannungZelle1 8=SpannungZelle2 9=SpannungZelle3 10=SpannungZelle4 11=SpannungZelle5 12=SpannungZelle6
			for (int j=0, k=0; j<10; ++j, k+=GDE.SIZE_BYTES_INTEGER) {
				//log_base.info("cell " + (i+1) + " points[" + (i+8) + "]  = Integer.valueOf((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
				//log.log(Level.OFF, j + " k+19 = " + (k+19));
				points[j + 9] = (((convertBuffer[k+24]&0xff) << 24) + ((convertBuffer[k+25]&0xff) << 16) + ((convertBuffer[k+26]&0xff) << 8) + ((convertBuffer[k+27]&0xff) << 0));
				if (points[j + 9] > 0) {
					maxVotage = points[j + 9] > maxVotage ? points[j + 9] : maxVotage;
					minVotage = points[j + 9] < minVotage ? points[j + 9] : minVotage;
				}
			}
			//calculate balance on the fly
			points[8] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

			recordSet.addPoints(points);
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*2500)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}
	
	/**
	 * query number of Lithium cells of this charger device
	 * @return
	 */
	@Override
	public int getNumberOfLithiumCells() {
		return 10;
	}
}
