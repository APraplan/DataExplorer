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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.bantam;

import java.io.FileNotFoundException;
import java.util.Vector;

import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.exception.DataInconsitsentException;
import gde.log.Level;
import gde.utils.StringHelper;

/**
 * eStation BC6 device class
 * @author Winfried Brügmann
 */
public class eStationBC6 extends eStation {

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public eStationBC6(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		this.dialog = new EStationDialog(this.application.getShell(), this);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public eStationBC6(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.dialog = new EStationDialog(this.application.getShell(), this);
	}

	/**
	 * @return the dialog
	 */
	@Override
	public EStationDialog getDialog() {
		return this.dialog;
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
		
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, StringHelper.byte2FourDigitsIntegerString(dataBuffer, (byte)0x80, 1, dataBuffer.length-2));
		
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=VersorgungsSpg. 6=Balance
		points[0] = Integer.valueOf((((dataBuffer[35] & 0xFF)-0x80)*100 + ((dataBuffer[36] & 0xFF)-0x80))*10);  //35,36   feed-back voltage
		points[1] = Integer.valueOf((((dataBuffer[33] & 0xFF)-0x80)*100 + ((dataBuffer[34] & 0xFF)-0x80))*10);  //33,34   feed-back current : 0=0.0A,900=9.00A
		points[2] = Integer.valueOf((((dataBuffer[43] & 0xFF)-0x80)*100 + ((dataBuffer[44] & 0xFF)-0x80))*1000);  //43,44  cq_capa_dis;  : charged capacity
		points[3] = Double.valueOf((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
		points[4] = Double.valueOf((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
		points[5] = Integer.valueOf((((dataBuffer[41] & 0xFF)-0x80)*100 + ((dataBuffer[42] & 0xFF)-0x80))*10);  //41,42  fd_in_12v;    : input voltage(00.00V 30.00V)
		points[6] = 0;

		// 7=SpannungZelle1 7=SpannungZelle2 9=SpannungZelle3 10=SpannungZelle4 11=SpannungZelle5 12=SpannungZelle6
		for (int i=0, j=0; i<points.length - 7; ++i, j+=2) {
			points[i + 7]  = Integer.valueOf((((dataBuffer[j+45] & 0xFF)-0x80)*100 + ((dataBuffer[j+46] & 0xFF)-0x80))*10);  //45,46 CELL_420v[1];
			if (points[i + 7] > 0) {
				maxVotage = points[i + 7] > maxVotage ? points[i + 7] : maxVotage;
				minVotage = points[i + 7] < minVotage ? points[i + 7] : minVotage;
			}
		}
		//calculate balance on the fly
		points[6] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

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
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		Vector<Integer> timeStamps = new Vector<Integer>(1,1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		int timeStampBufferSize = 0;
		if(!recordSet.isTimeStepConstant()) {
			timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
			byte[] timeStampBuffer = new byte[timeStampBufferSize];
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8) + ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*2500)/recordDataSize), sThreadId);
			}
		}
		log.log(Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString());
		
		for (int i = 0; i < recordDataSize; i++) {
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize+timeStampBufferSize);
			System.arraycopy(dataBuffer, i*dataBufferSize+timeStampBufferSize, convertBuffer, 0, dataBufferSize);
			
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=VersorgungsSpg. 6=Balance
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			points[3] = Double.valueOf((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
			points[4] = Double.valueOf((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
			points[5] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
			points[6] = 0;
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;

			// 7=SpannungZelle1 7=SpannungZelle2 9=SpannungZelle3 10=SpannungZelle4 11=SpannungZelle5 12=SpannungZelle6
			for (int j=0, k=0; j<points.length - 7; ++j, k+=GDE.SIZE_BYTES_INTEGER) {
				//log_base.info("cell " + (i+1) + " points[" + (i+8) + "]  = Integer.valueOf((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
				points[j + 7] = (((convertBuffer[k+16]&0xff) << 24) + ((convertBuffer[k+17]&0xff) << 16) + ((convertBuffer[k+18]&0xff) << 8) + ((convertBuffer[k+19]&0xff) << 0));
				if (points[j + 7] > 0) {
					maxVotage = points[j + 7] > maxVotage ? points[j + 7] : maxVotage;
					minVotage = points[j + 7] < minVotage ? points[j + 7] : minVotage;
				}
			}
			//calculate balance on the fly
			points[6] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
			
			if(recordSet.isTimeStepConstant()) 
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i)/10.0);
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*2500)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
		recordSet.getDevice().updateVisibilityStatus(recordSet, true);
	}
}
