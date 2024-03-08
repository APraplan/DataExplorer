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
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.exception.DataInconsitsentException;
import gde.io.LogViewReader;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.StringHelper;

/**
 * eStation BC6 80W device class
 * @author Winfried Brügmann
 */
public class eStationBC680W extends eStation {

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public eStationBC680W(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		this.dialog = new EStationDialog(this.application.getShell(), this);
		this.ACCU_TYPES = new String[] {
				Messages.getString(MessageIds.GDE_MSGT1403), //0=Lithium
				GDE.STRING_EMPTY,
				GDE.STRING_EMPTY,
				GDE.STRING_EMPTY,
				Messages.getString(MessageIds.GDE_MSGT1404), //4=NiMH
				Messages.getString(MessageIds.GDE_MSGT1405), //5=NiCd
				Messages.getString(MessageIds.GDE_MSGT1406), //6=Pb
				"Save","Load"};
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public eStationBC680W(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.dialog = new EStationDialog(this.application.getShell(), this);
		this.ACCU_TYPES = new String[] {
				Messages.getString(MessageIds.GDE_MSGT1403), //0=Lithium
				GDE.STRING_EMPTY,
				GDE.STRING_EMPTY,
				GDE.STRING_EMPTY,
				Messages.getString(MessageIds.GDE_MSGT1404), //4=NiMH
				Messages.getString(MessageIds.GDE_MSGT1405), //5=NiCd
				Messages.getString(MessageIds.GDE_MSGT1406), //6=Pb
				"Save","Load"};
	}

	/**
	 * get global device configuration values
	 * @param configData
	 * @param dataBuffer
	 */
	@Override
	public HashMap<String, String> getConfigurationValues(HashMap<String, String> configData, byte[] dataBuffer) {
		configData.put(eStation.CONFIG_EXT_TEMP_CUT_OFF, "0");
		configData.put(eStation.CONFIG_WAIT_TIME, "0");
		configData.put(eStation.CONFIG_IN_VOLTAGE_CUT_OFF, "0");
		configData.put(eStation.CONFIG_SAFETY_TIME, "0");
		configData.put(eStation.CONFIG_SET_CAPASITY, "0");
		if(getProcessingMode(dataBuffer) != 0) {
			configData.put(eStation.CONFIG_BATTERY_TYPE, this.ACCU_TYPES[this.getAccuCellType(dataBuffer)]);
			configData.put(eStation.CONFIG_PROCESSING_TIME, ""+((dataBuffer[9] & 0xFF - 0x80)*100 + (dataBuffer[10] & 0xFF - 0x80))); //$NON-NLS-1$
		}
		if (log.isLoggable(Level.FINE)) {
			for (Entry<String, String> entry : configData.entrySet()) {
				log.log(Level.FINE, entry.getKey() + " = " + entry.getValue()); //$NON-NLS-1$
			}
		}
		return configData;
	}

	/**
	 * @return the dialog
	 */
	@Override
	public EStationDialog getDialog() {
		return this.dialog;
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device
	 */
	@Override
	public int getLovDataByteSize() {
		return 120;
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
		int deviceDataBufferSize = Math.abs(this.getDataBlockSize(InputTypes.SERIAL_IO)); // const.
		int[] points = new int[this.getNumberOfMeasurements(1)];
		int offset = 0;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		long lastDateTime = 0, sumTimeDelta = 0, deltaTime = 0;

		if (dataBuffer[0] == 0x7B) {
			byte[] convertBuffer = new byte[deviceDataBufferSize];
			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

			for (int i = 0; i < recordDataSize; i++) {
				System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize);
				recordSet.addPoints(convertDataBytes(points, convertBuffer));

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}

			recordSet.setTimeStep_ms(this.getAverageTimeStep_ms() != null ? this.getAverageTimeStep_ms() : 1478); // no average time available, use a hard coded one
		}
		else { // none constant time steps
			byte[] sizeBuffer = new byte[4];
			byte[] convertBuffer = new byte[deviceDataBufferSize];

			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
			for (int i = 0; i < recordDataSize; i++) {
				System.arraycopy(dataBuffer, offset, sizeBuffer, 0, 4);
				lovDataSize = 4 + LogViewReader.parse2Int(sizeBuffer);
				System.arraycopy(dataBuffer, offset + 4, convertBuffer, 0, deviceDataBufferSize);
				recordSet.addPoints(convertDataBytes(points, convertBuffer));
				offset += lovDataSize;

				StringBuilder sb = new StringBuilder();
				byte[] timeBuffer = new byte[lovDataSize - deviceDataBufferSize - 4];
				//sb.append(timeBuffer.length).append(" - ");
				System.arraycopy(dataBuffer, offset - timeBuffer.length, timeBuffer, 0, timeBuffer.length);
				String timeStamp = new String(timeBuffer).substring(0, timeBuffer.length-8)+"0000000000";
				long dateTime = Long.parseLong(timeStamp.substring(6,17));
				log.log(Level.FINEST, timeStamp + " " + timeStamp.substring(6,17) + " " + dateTime);
				sb.append(dateTime);
				//System.arraycopy(dataBuffer, offset - 4, sizeBuffer, 0, 4);
				//sb.append(" ? ").append(LogViewReader.parse2Int(sizeBuffer));
				deltaTime = lastDateTime == 0 ? 0 : (dateTime - lastDateTime)/1000 - 217; // value 217 is a compromis manual selected
				sb.append(" - ").append(deltaTime);
				sb.append(" - ").append(sumTimeDelta += deltaTime);
				log.log(Level.FINER, sb.toString());
				lastDateTime = dateTime;

				recordSet.addTimeStep_ms(sumTimeDelta);

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}
//			recordSet.setTimeStep_ms((double)sumTimeDelta/recordDataSize);
//			log.log(Level.FINE, sumTimeDelta/recordDataSize + " " + sumTimeDelta);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
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

		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=VersorgungsSpg. 7=Balance
		points[0] = Integer.valueOf((((dataBuffer[15] & 0xFF)-0x80)*100 + ((dataBuffer[16] & 0xFF)-0x80))*10);
		points[1] = Integer.valueOf((((dataBuffer[11] & 0xFF)-0x80)*100 + ((dataBuffer[12] & 0xFF)-0x80))*10);
		points[2] = Integer.valueOf((((dataBuffer[7] & 0xFF)-0x80)*100 + ((dataBuffer[8] & 0xFF)-0x80))*1000);
		points[3] = Double.valueOf((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
		points[4] = Double.valueOf((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
		points[5] = Integer.valueOf((((dataBuffer[13] & 0xFF)-0x80)*100 + ((dataBuffer[14] & 0xFF)-0x80))*10);
		points[6] = Integer.valueOf((((dataBuffer[4] & 0xFF)-0x80)*100 + ((dataBuffer[5] & 0xFF)-0x80))*10);
		points[7] = 0;

		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
		for (int i=0, j=0; i<6; ++i, j+=2) {
			points[i + 8]  = Integer.valueOf((((dataBuffer[j+19] & 0xFF)-0x80)*100 + ((dataBuffer[j+20] & 0xFF)-0x80))*10);  //45,46 CELL_420v[1];
			if (points[i + 8] > 0) {
				maxVotage = points[i + 8] > maxVotage ? points[i + 8] : maxVotage;
				minVotage = points[i + 8] < minVotage ? points[i + 8] : minVotage;
			}
		}
		//calculate balance on the fly
		points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

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

			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=VersorgungsSpg. 7=Balance
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			points[3] = Double.valueOf((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
			points[4] = Double.valueOf((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
			points[5] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
			points[6] = (((convertBuffer[16]&0xff) << 24) + ((convertBuffer[17]&0xff) << 16) + ((convertBuffer[18]&0xff) << 8) + ((convertBuffer[19]&0xff) << 0));
			points[7] = 0;
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;

			// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
			for (int j=0, k=0; j<6; ++j, k+=GDE.SIZE_BYTES_INTEGER) {
				//log_base.info("cell " + (i+1) + " points[" + (i+8) + "]  = Integer.valueOf((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
				points[j + 8] = (((convertBuffer[k+20]&0xff) << 24) + ((convertBuffer[k+21]&0xff) << 16) + ((convertBuffer[k+22]&0xff) << 8) + ((convertBuffer[k+23]&0xff) << 0));
				if (points[j + 8] > 0) {
					maxVotage = points[j + 8] > maxVotage ? points[j + 8] : maxVotage;
					minVotage = points[j + 8] < minVotage ? points[j + 8] : minVotage;
				}
			}
			//calculate balance on the fly
			points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

			if(recordSet.isTimeStepConstant())
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i)/10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*2500)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * query if the eStation executes discharge > charge > discharge cycles
	 */
	@Override
	boolean isCycleMode(byte[] dataBuffer) {
		return (((dataBuffer[8] & 0xFF)-0x80) & 0x10) > 0;
	}

	/**
	 * getNumberOfCycle for NiCd and NiMh, for LiXx it  will return 0
	 * accuCellType -> Lithium=1, NiMH=2, NiCd=3, Pb=4
	 * @param dataBuffer
	 * @return cycle count
	 */
	@Override
	public int getNumberOfCycle(byte[] dataBuffer) {
		return 0;
	}

	/**
	 * @param dataBuffer
	 * @return for 0= LiPo, 4=NiMH, 5=NiCd, 6=Pb, 7=Save, 8=Load
	 */
	@Override
	public int getAccuCellType(byte[] dataBuffer) {
		return (dataBuffer[2] & 0xFF)- 0x80; //0= LiPo, 4=NiMH, 5=NiCd, 6=Pb, 7=Save, 8=Load
	}

	/**
	 * @param dataBuffer
	 * @return for Lithium=1, NiMH=2, NiCd=3, Pb=4
	 */
	@Override
	public boolean isProcessing(byte[] dataBuffer) {
		return ((dataBuffer[1] & 0xFF)- 0x80) > 0; //processing >= 1; stop = 0
	}

	/**
	 * @param dataBuffer [lenght 76 bytes]
	 * @return 0 = no processing, 1 = discharge, 2 = charge
	 */
	@Override
	public int getProcessingMode(byte[] dataBuffer) {
		int modeIndex = (dataBuffer[1] & 0xFF) - 0x80; // processing >= 1, stop=0
		if(modeIndex != 0) {
			modeIndex = (dataBuffer[1] & 0x0F) == 0x02 ? 2 : 1;
		}
		return modeIndex;
	}

	/**
	 * @param dataBuffer [length 112 bytes]
	 * @return processing time in seconds
	 */
	@Override
	public int getProcessingTime(byte[] dataBuffer) {
		return  ((dataBuffer[9] & 0xFF - 0x80)*100 + (dataBuffer[10] & 0xFF - 0x80));
	}

	/**
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 * at least an update of the graphics window should be included at the end of this method
	 */
	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int displayableCounter = 0;
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=VersorgungsSpg. 7=Balance
		recordSet.setAllDisplayable();
		for (String recordKey : recordSet.getNoneCalculationRecordNames()) {
			recordSet.get(recordKey).setActive(true);
		}
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
		for (int i=5; i<recordSet.size(); ++i) {
				Record record = recordSet.get(i);
				record.setDisplayable(record.hasReasonableData());
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, record.getName() + " setDisplayable=" + record.hasReasonableData());

				if (record.isActive() && record.isDisplayable()) {
					++displayableCounter;
				}
		}

		if (log.isLoggable(Level.FINE)) {
			for (int i = 0; i < recordSet.size(); i++) {
				Record record = recordSet.get(i);
				log.log(Level.FINE, record.getName() + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable());
			}
		}
		recordSet.setConfiguredDisplayable(displayableCounter);
	}

}
