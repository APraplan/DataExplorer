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

import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.io.DataParser;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

/**
 * Graupner Polaron Ex base class
 * @author Winfried Brügmann
 */
public class PolaronEx1400 extends Polaron {

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public PolaronEx1400(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public PolaronEx1400(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

	/**
	 * query the device identifier to differentiate between different device implementations
	 * @return 	0=unknown, 1=PolaronEx, 2=PolaronAcDcEQ, 3=PolaronAcDc, 4=PolaronPro, 5=PolaronSports
	 */
	@Override
	public GraupnerDeviceType getDeviceTypeIdentifier() {
		return GraupnerDeviceType.PolaronEx1400;
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		int deviceDataBufferSize = Math.abs(this.getDataBlockSize(InputTypes.SERIAL_IO));

		if (deviceDataBufferSize == dataBuffer.length && this.isLinkedMode(dataBuffer)) {
			try {
				final int offset2 = 129-11;
				//0=VersorgungsSpg1 1=Spannung 2=Spannung1 3=Spannung2 4=Strom 5=Strom1 6=Strom2 7=Ladung 8=Ladung1 9=Ladung2 10=Leistung 11=Leistung1 12=Leistung2 13=Energie 14=Energie1 15=Energie2 16=BatteryTemperature1 17=BatteryTemperature2 18=Balance 
				points[0] = DataParser.parse2Short(dataBuffer, 11);

				points[2] = DataParser.parse2Short(dataBuffer, 31);
				points[3] = DataParser.parse2Short(dataBuffer, 31 + offset2);
				points[1] = points[2] + points[3];

				points[5] = DataParser.parse2Short(dataBuffer, 33);
				points[6] = DataParser.parse2Short(dataBuffer, 33 + offset2);
				points[4] = points[5] + points[6];

				points[8] = DataParser.parse2Short(dataBuffer, 35);
				points[9] = DataParser.parse2Short(dataBuffer, 35 + offset2);
				points[7] = points[8] + points[9];

				points[11] = Double.valueOf(points[2] * points[5] / 1000.0).intValue(); // power U*I [W]
				points[12] = Double.valueOf(points[3] * points[6] / 1000.0).intValue(); // power U*I [W]
				points[10] = points[11] + points[12];

				points[14] = Double.valueOf(points[2] * points[8] / 1000.0).intValue(); // energy U*C [Wh]
				points[15] = Double.valueOf(points[3] * points[9] / 1000.0).intValue(); // energy U*C [Wh]
				points[13] = points[14] + points[15];

				points[16] = DataParser.parse2Short(dataBuffer, 37);
				if (DataParser.parse2Short(dataBuffer, 39) == 0) points[16] = -1 * points[16];
				points[17] = DataParser.parse2Short(dataBuffer, 37 + offset2);
				if (DataParser.parse2Short(dataBuffer, 39) == 0) points[17] = -1 * points[17];
				points[18] = 0;

				// 19=SpannungZelle1 20=SpannungZelle2 21=SpannungZelle3 22=SpannungZelle4 23=SpannungZelle5 24=SpannungZelle6 25=SpannungZelle7  26=SpannungZelle8 
				for (int i = 0, j = 0; i < 8; ++i, j += 2) {
					points[i + 19] = DataParser.parse2Short(dataBuffer, j + 45);
					if (points[i + 19] > 0) {
						maxVotage = points[i + 19] > maxVotage ? points[i + 19] : maxVotage;
						minVotage = points[i + 19] < minVotage ? points[i + 19] : minVotage;
					}
				}
				// 27=SpannungZelle9 28=SpannungZelle10 29=SpannungZelle11 30=SpannungZelle12 31=SpannungZelle13 32=SpannungZelle14 33=SpannungZelle15 34=SpannungZelle16 
				for (int i = 0, j = 0; i < 8; ++i, j += 2) {
					points[i + 27] = DataParser.parse2Short(dataBuffer, j + 45 + offset2);
					if (points[i + 27] > 0) {
						maxVotage = points[i + 27] > maxVotage ? points[i + 27] : maxVotage;
						minVotage = points[i + 27] < minVotage ? points[i + 27] : minVotage;
					}
				}
				//calculate balance on the fly
				points[18] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

				// 35=BatteryRi
				points[35] = DataParser.parse2Short(dataBuffer, 95) + DataParser.parse2Short(dataBuffer, 95 + offset2);
				// 36=CellRi1 37=CellRi2 38=CellRi3 39=CellRi4 40=CellRi5 41=CellRi6 42=CellRi7 43=CellRi8
				for (int i = 0, j = 0; i < 8; ++i, j += 2) {
					points[i + 36] = DataParser.parse2Short(dataBuffer, j + 61);
				}
				// 44=CellRi1 45=CellRi2 46=CellRi3 47=CellRi4 48=CellRi5 49=CellRi6 50=CellRi7 51=CellRi8
				for (int i = 0, j = 0; i < 8; ++i, j += 2) {
					points[i + 44] = DataParser.parse2Short(dataBuffer, j + 61 + offset2);
				}
			}
			catch (NumberFormatException e) {
				Polaron.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			}
		}
		else {
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

				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7 15=SpannungZelle8
				for (int i = 0, j = 0; i < 8; ++i, j += 2) {
					points[i + 8] = DataParser.parse2Short(dataBuffer, j + 45);
					if (points[i + 8] > 0) {
						maxVotage = points[i + 8] > maxVotage ? points[i + 8] : maxVotage;
						minVotage = points[i + 8] < minVotage ? points[i + 8] : minVotage;
					}
				}
				//calculate balance on the fly
				points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

				// 16=BatteryRi
				points[16] = DataParser.parse2Short(dataBuffer, 95);
				// 17=CellRi1 18=CellRi2 19=CellRi3 20=CellRi4 21=CellRi5 22=CellRi6 23=CellRi7 24=CellRi8
				for (int i = 0, j = 0; i < 8; ++i, j += 2) {
					points[i + 17] = DataParser.parse2Short(dataBuffer, j + 61);
				}
			}
			catch (Exception e) {
				Polaron.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			}
		}
		return points;
	}

	/**
	 * check if one of the outlet channels are in processing mode
	 * @param outletNum 1 or 2
	 * @param dataBuffer
	 * @return true if channel 1 or 2 is active 
	 */
	@Override
	public boolean isProcessing(int outletNum, byte[] dataBuffer) {
		if (outletNum == 1) {
			int processingModeOut1 = getProcessingMode(dataBuffer);
			processingModeOut1 = processingModeOut1 > 9 ? 0 : processingModeOut1;
			if (Polaron.log.isLoggable(java.util.logging.Level.FINE)) {
				Polaron.log.log(java.util.logging.Level.FINE, "processingModeOut1 = " + this.PROCESSING_MODE[processingModeOut1]); //$NON-NLS-1$
			}
			return !(processingModeOut1 == Polaron.OPERATIONS_MODE_NONE || processingModeOut1 == Polaron.OPERATIONS_MODE_ERROR);
		}
		else if (outletNum == 2) {
			int processingModeOut2 = DataParser.parse2Short(dataBuffer, 131);
			processingModeOut2 = processingModeOut2 > 9 ? 0 : processingModeOut2;
			if (Polaron.log.isLoggable(java.util.logging.Level.FINE)) {
				Polaron.log.log(java.util.logging.Level.FINE, "processingModeOut2 = " + this.PROCESSING_MODE[processingModeOut2]); //$NON-NLS-1$
			}
			return !(processingModeOut2 == Polaron.OPERATIONS_MODE_NONE || processingModeOut2 == Polaron.OPERATIONS_MODE_ERROR);
		}
		else
			return false;
	}

	/**
	 * query if outlets are linked together to charge identical batteries in parallel
	 * @param dataBuffer
	 * @return true | false
	 */
	@Override
	public boolean isLinkedMode(byte[] dataBuffer) {
		int processingMode1 = DataParser.parse2Short(dataBuffer, 13);
		int processingType1 = DataParser.parse2Short(dataBuffer, 15);
		int processingMode2 = DataParser.parse2Short(dataBuffer, 131);
		int processingType2 = DataParser.parse2Short(dataBuffer, 133);
		int numCells = DataParser.parse2Short(dataBuffer, 43);
		return (processingMode1 == 7 && processingMode2 == 0 && numCells > 8)
				|| (processingMode1 == processingMode2 && processingType1 == processingType2 && ((processingMode1 == 1 && processingType1 == 6) || (processingMode1 == 2 && processingType2 == 4)));
	}

}
