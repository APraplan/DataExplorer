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
package gde.device.graupner;

import gde.GDE;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

/**
 * Graupner Polaron Pro base class
 * @author Winfried Brügmann
 */
public class PolaronPro extends Polaron {

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public PolaronPro(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public PolaronPro(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

	/**
	 * query the device identifier to differentiate between different device implementations
	 * @return 	0=unknown, 1=PolaronEx, 2=PolaronAcDcEQ, 3=PolaronAcDc, 4=PolaronPro, 5=PolaronSports
	 */
	@Override
	public GraupnerDeviceType getDeviceTypeIdentifier() {
		return GraupnerDeviceType.PolaronPro;
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		int maxVoltage = Integer.MIN_VALUE;
		int minVoltage = Integer.MAX_VALUE;
		int deviceDataBufferSize = Math.abs(this.getDataBlockSize(InputTypes.SERIAL_IO));

		if (deviceDataBufferSize == dataBuffer.length) { //outlet/channel 1
			try {
				// 0=VersorgungsSpg 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=BatteryTemperature 7=Balance 
				points[0] = DataParser.parse2Short(dataBuffer, 11);
				points[1] = DataParser.parse2Short(dataBuffer, 31);
				points[2] = DataParser.parse2Short(dataBuffer, 33);
				points[3] = DataParser.parse2Short(dataBuffer, 35);
				points[4] = Double.valueOf(points[1] * points[2] / 1000.0).intValue(); // power U*I [W]
				points[5] = Double.valueOf(points[1] * points[3] / 1000.0).intValue(); // energy U*C [Wh]
				points[6] = DataParser.parse2Short(dataBuffer, 37);
				if (DataParser.parse2Short(dataBuffer, 39) == 0) points[6] = -1 * points[6];
				points[7] = 0;

				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7
				for (int i = 0, j = 0; i < 7; ++i, j += 2) {
					points[i + 8] = DataParser.parse2Short(dataBuffer, j + 45);
					if (points[i + 8] > 0) {
						maxVoltage = points[i + 8] > maxVoltage ? points[i + 8] : maxVoltage;
						minVoltage = points[i + 8] < minVoltage ? points[i + 8] : minVoltage;
					}
				}
				// 15=SpannungZelle8 16=SpannungZelle9 17=SpannungZelle10 18=SpannungZelle11 19=SpannungZelle12 20=SpannungZelle13 21=SpannungZelle14
				for (int i = 0, j = 0; i < 7; ++i, j += 2) {
					points[i + 15] = DataParser.parse2Short(dataBuffer, j + 159);
					if (points[i + 15] > 0) {
						maxVoltage = points[i + 15] > maxVoltage ? points[i + 15] : maxVoltage;
						minVoltage = points[i + 15] < minVoltage ? points[i + 15] : minVoltage;
					}
				}
				//calculate balance on the fly
				points[7] = maxVoltage != Integer.MIN_VALUE && minVoltage != Integer.MAX_VALUE ? maxVoltage - minVoltage : 0;

				// 22=BatteryRi
				points[22] = DataParser.parse2Short(dataBuffer, 91);
				
				// 23=CellRi1 24=CellRi2 25=CellRi3 26=CellRi4 27=CellRi5 28=CellRi6 29=CellRi7
				for (int i = 0, j = 0; i < 7; ++i, j += 2) {
					points[i + 23] = DataParser.parse2Short(dataBuffer, j + 59);
				}
				// 30=CellRi8 31=CellRi9 32=CellRi10 33=CellRi11 34=CellRi12 35=CellRi13 36=CellRi14
				for (int i = 0, j = 0; i < 7; ++i, j += 2) {
					points[i + 30] = DataParser.parse2Short(dataBuffer, j + 173);
				}
			}
			catch (Exception e) {
				Polaron.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			}
		}
		else {
			try {
				// 0=VersorgungsSpg 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=BatteryTemperature 7=BatteryRi
				points[0] = DataParser.parse2Short(dataBuffer, 11);
				points[1] = DataParser.parse2Short(dataBuffer, 31);
				points[2] = DataParser.parse2Short(dataBuffer, 33);
				points[3] = DataParser.parse2Short(dataBuffer, 35);
				points[4] = Double.valueOf(points[0] * points[1] / 1000.0).intValue(); // power U*I [W]
				points[5] = Double.valueOf(points[0] * points[2] / 1000.0).intValue(); // energy U*C [Wh]
				points[6] = DataParser.parse2Short(dataBuffer, 37);
				if (DataParser.parse2Short(dataBuffer, 39) == 0) points[6] = -1 * points[6];
				points[7] = DataParser.parse2Short(dataBuffer, 91);
			}
			catch (NumberFormatException e) {
				Polaron.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			}
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

		if (recordSet.getChannelConfigNumber() == 1) {
			for (int i = 0; i < recordDataSize; i++) {
				int maxVotage = Integer.MIN_VALUE;
				int minVotage = Integer.MAX_VALUE;
				Polaron.log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize); //$NON-NLS-1$
				System.arraycopy(dataBuffer, i * dataBufferSize, convertBuffer, 0, dataBufferSize);
				// 0=VersorgungsSpg 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=BatteryTemperature 7=Balance 
				points[0] = (((convertBuffer[0] & 0xff) << 24) + ((convertBuffer[1] & 0xff) << 16) + ((convertBuffer[2] & 0xff) << 8) + ((convertBuffer[3] & 0xff) << 0));
				points[1] = (((convertBuffer[4] & 0xff) << 24) + ((convertBuffer[5] & 0xff) << 16) + ((convertBuffer[6] & 0xff) << 8) + ((convertBuffer[7] & 0xff) << 0));
				points[2] = (((convertBuffer[8] & 0xff) << 24) + ((convertBuffer[9] & 0xff) << 16) + ((convertBuffer[10] & 0xff) << 8) + ((convertBuffer[11] & 0xff) << 0));
				points[3] = (((convertBuffer[12] & 0xff) << 24) + ((convertBuffer[13] & 0xff) << 16) + ((convertBuffer[14] & 0xff) << 8) + ((convertBuffer[15] & 0xff) << 0));
				points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W]
				points[5] = Double.valueOf(points[1] / 1000.0 * points[3]).intValue(); // energy U*C [Wh]
				points[6] = (((convertBuffer[16] & 0xff) << 24) + ((convertBuffer[17] & 0xff) << 16) + ((convertBuffer[18] & 0xff) << 8) + ((convertBuffer[19] & 0xff) << 0));
				points[7] = 0;

				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7
				// 15=SpannungZelle8 16=SpannungZelle8 17=SpannungZelle10 18=SpannungZelle11 19=SpannungZelle12 20=SpannungZelle13 21=SpannungZelle14
				for (int j = 0, k = 0; j < 14; ++j, k += GDE.SIZE_BYTES_INTEGER) {
					points[j + 8] = (((convertBuffer[k + 20] & 0xff) << 24) + ((convertBuffer[k + 21] & 0xff) << 16) + ((convertBuffer[k + 22] & 0xff) << 8) + ((convertBuffer[k + 23] & 0xff) << 0));
					if (points[j + 8] > 0) {
						maxVotage = points[j + 8] > maxVotage ? points[j + 8] : maxVotage;
						minVotage = points[j + 8] < minVotage ? points[j + 8] : minVotage;
					}
				}
				//calculate balance on the fly
				points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

				points[22] = DataParser.parse2Short(dataBuffer, 91);
				// 22=BatteryRi 23=CellRi1 24=CellRi2 25=CellRi3 26=CellRi4 27=CellRi5 28=CellRi6 29=CellRi7
				// 30=CellRi8 31=CellRi9 32=CellRi10 33=CellRi11 34=CellRi12 35=CellRi13 36=CellRi14
				for (int j = 0, k = 0; j < 8; ++j, k += GDE.SIZE_BYTES_INTEGER) {
					points[j + 15] = (((convertBuffer[k + 48] & 0xff) << 24) + ((convertBuffer[k + 49] & 0xff) << 16) + ((convertBuffer[k + 50] & 0xff) << 8) + ((convertBuffer[k + 51] & 0xff) << 0));
				}

				recordSet.addPoints(points);

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
			}
		}
		else {
			for (int i = 0; i < recordDataSize; i++) {
				Polaron.log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize); //$NON-NLS-1$
				System.arraycopy(dataBuffer, i * dataBufferSize, convertBuffer, 0, dataBufferSize);
				// 0=VersorgungsSpg 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=BatteryTemperature 7=BatteryRi
				points[0] = (((convertBuffer[0] & 0xff) << 24) + ((convertBuffer[1] & 0xff) << 16) + ((convertBuffer[2] & 0xff) << 8) + ((convertBuffer[3] & 0xff) << 0));
				points[1] = (((convertBuffer[4] & 0xff) << 24) + ((convertBuffer[5] & 0xff) << 16) + ((convertBuffer[6] & 0xff) << 8) + ((convertBuffer[7] & 0xff) << 0));
				points[2] = (((convertBuffer[8] & 0xff) << 24) + ((convertBuffer[9] & 0xff) << 16) + ((convertBuffer[10] & 0xff) << 8) + ((convertBuffer[11] & 0xff) << 0));
				points[3] = (((convertBuffer[12] & 0xff) << 24) + ((convertBuffer[13] & 0xff) << 16) + ((convertBuffer[14] & 0xff) << 8) + ((convertBuffer[15] & 0xff) << 0));
				points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W]
				points[5] = Double.valueOf(points[1] / 1000.0 * points[3]).intValue(); // energy U*C [Wh]
				points[6] = (((convertBuffer[16] & 0xff) << 24) + ((convertBuffer[17] & 0xff) << 16) + ((convertBuffer[18] & 0xff) << 8) + ((convertBuffer[19] & 0xff) << 0));
				points[7] = (((convertBuffer[20] & 0xff) << 24) + ((convertBuffer[21] & 0xff) << 16) + ((convertBuffer[22] & 0xff) << 8) + ((convertBuffer[23] & 0xff) << 0));

				recordSet.addPoints(points);

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
			}
		}

		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public int[] getCellVoltageOrdinals() {
		// outlet 1
		// 0=VersorgungsSpg 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=BatteryTemperature 7=Balance 
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7
		// 15=SpannungZelle8 16=SpannungZelle8 17=SpannungZelle10 18=SpannungZelle11 19=SpannungZelle12 20=SpannungZelle13 21=SpannungZelle14
		// 22=BatteryRi 23=CellRi1 24=CellRi2 25=CellRi3 26=CellRi4 27=CellRi5 28=CellRi6 29=CellRi7
		// 30=CellRi8 31=CellRi9 32=CellRi10 33=CellRi11 34=CellRi12 35=CellRi13 36=CellRi14
		// outlet 2
		// 0=VersorgungsSpg 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=BatteryTemperature 7=BatteryRi
		return new int[] { 1, 3 };
	}

	/**
	 * query if outlets are linked together to charge identical batteries in parallel
	 * @param dataBuffer
	 * @return true | false
	 */
	@Override
	public boolean isLinkedMode(byte[] dataBuffer) {
		return false; //no linked mode all balancer connectors are part of outlet 1
	}
}
