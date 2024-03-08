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
package gde.device.junsi;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.log.Level;

/**
 * Junsi CellLog 8S device class
 * @author Winfried Brügmann
 */
public class CellLog8S extends iCharger {

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public CellLog8S(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public CellLog8S(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device
	 */
	@Override
	public int getLovDataByteSize() {
		return 78;
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
			for (int j = deviceDataBufferSize/3; j < deviceDataBufferSize && (offset+j < dataBuffer.length); j++) { //start at minimum length of data buffer
				convertBuffer[j] = dataBuffer[offset+j];
				++lovDataSize;
				if (dataBuffer[offset+j] == 0x0A && dataBuffer[offset+j-1] == 0x0D)
					break;
			}

			if ( i > 1) //first measurement seams always contain only zeros as values
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

		try {
			setDataLineStartAndLength(dataBuffer, startLength);
			if (startLength[1]<18)
				return points;
			lineBuffer = new byte[startLength[1]];
			System.arraycopy(dataBuffer, startLength[0], lineBuffer, 0, startLength[1]);
			data.parse(new String(lineBuffer), 1);
			int[] values = data.getValues();

			//0=Spannung
			points[0] = values[8];
			//2=SpannungZelle1 3=SpannungZelle2 4=SpannungZelle3 5=SpannungZelle4 6=SpannungZelle5 7=SpannungZelle6 8=SpannungZelle7 9=SpannungZelle8
			for (int i = 0; i < 8; i++) {
				points[i+2] = values[i];
				if (points[i + 2] > 0) {
					maxVotage = points[i + 2] > maxVotage ? points[i + 2] : maxVotage;
					minVotage = points[i + 2] < minVotage ? points[i + 2] : minVotage;
				}
			}
			//1=Balance
			points[1] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
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
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);

			//0==Spannung
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));

			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			//2=SpannungZelle1 3=SpannungZelle2 4=SpannungZelle3 5=SpannungZelle4 6=SpannungZelle5 7=SpannungZelle6 8=SpannungZelle7 9=SpannungZelle8
			for (int j=0, k=0; j<8; ++j, k+=GDE.SIZE_BYTES_INTEGER) {
				//log_base.info("cell " + (i+1) + " points[" + (i+8) + "]  = Integer.valueOf((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
				//log.log(Level.OFF, j + " k+19 = " + (k+19));
				points[j + 2] = (((convertBuffer[k+4]&0xff) << 24) + ((convertBuffer[k+5]&0xff) << 16) + ((convertBuffer[k+6]&0xff) << 8) + ((convertBuffer[k+7]&0xff) << 0));
				if (points[j + 2] > 0) {
					maxVotage = points[j + 2] > maxVotage ? points[j + 2] : maxVotage;
					minVotage = points[j + 2] < minVotage ? points[j + 2] : minVotage;
				}
			}
			//calculate balance on the fly
			points[1] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

			recordSet.addPoints(points);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*2500)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
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

		//0=Spannung 1=Balance 2=SpannungZelle1 3=SpannungZelle2 4=SpannungZelle3 5=SpannungZelle4 6=SpannungZelle5 7=SpannungZelle6 8=SpannungZelle7 9=SpannungZelle8
		recordSet.setAllDisplayable();
		for (int i=2; i<recordSet.size(); ++i) {
				Record record = recordSet.get(i);
				record.setDisplayable(record.hasReasonableData());
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, record.getName() + " setDisplayable=" + record.hasReasonableData()); //$NON-NLS-1$
		}

		if (log.isLoggable(Level.FINE)) {
			for (int i = 0; i < recordSet.size(); i++) {
				Record record = recordSet.get(i);
				log.log(Level.FINE, record.getName() + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}

	/**
	 * query number of Lithium cells of this charger device
	 * @return
	 */
	@Override
	public int getNumberOfLithiumCells() {
		return 8;
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public int[] getCellVoltageOrdinals() {
		//0=Spannung 1=Balance 2=SpannungZelle1 3=SpannungZelle2 4=SpannungZelle3 5=SpannungZelle4 6=SpannungZelle5 7=SpannungZelle6 8=SpannungZelle7 9=SpannungZelle8
		return new int[] {0, -1};
	}
}
