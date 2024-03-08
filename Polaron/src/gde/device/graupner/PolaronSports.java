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

import java.io.FileNotFoundException;
import java.util.Locale;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.device.graupner.polaron.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;

/**
 * Graupner Polaron Sport base class
 * @author Winfried Brügmann
 */
public class PolaronSports extends Polaron {

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public PolaronSports(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		this.CHARGE_TYPE = new String[] {
				Messages.getString(MessageIds.GDE_MSGT3110), //NiXx Automatic
				Messages.getString(MessageIds.GDE_MSGT3111), //LiXy Automatic
				Messages.getString(MessageIds.GDE_MSGT3112), //Normal
				Messages.getString(MessageIds.GDE_MSGT3113), //Linear
				Messages.getString(MessageIds.GDE_MSGT3118), //CV-CC
				Messages.getString(MessageIds.GDE_MSGT3120), //N-Storage
				Messages.getString(MessageIds.GDE_MSGT3119), //CV-LINK
				Messages.getString(MessageIds.GDE_MSGT3116), //reflex
				Messages.getString(MessageIds.GDE_MSGT3121), //CV-Fast
				Messages.getString(MessageIds.GDE_MSGT3117), //re-peak
				Messages.getString(MessageIds.GDE_MSGT3118), //CV-CC
				Messages.getString(MessageIds.GDE_MSGT3119), //CV-LINK
				Messages.getString(MessageIds.GDE_MSGT3120), //N-Storage
				Messages.getString(MessageIds.GDE_MSGT3122) };//QLagern
		this.DISCHARGE_TYPE = new String[] {
				Messages.getString(MessageIds.GDE_MSGT3110), //NiXx Automatic
				Messages.getString(MessageIds.GDE_MSGT3111), //LiXy Automatic
				Messages.getString(MessageIds.GDE_MSGT3112), //Normal
				Messages.getString(MessageIds.GDE_MSGT3113), //Linear
				Messages.getString(MessageIds.GDE_MSGT3118), //CV-CC
				Messages.getString(MessageIds.GDE_MSGT3124), //LINK
				Messages.getString(MessageIds.GDE_MSGT3120), //N-Storage
				Messages.getString(MessageIds.GDE_MSGT3122) };//Q-Storage
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public PolaronSports(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.CHARGE_TYPE = new String[] {
				Messages.getString(MessageIds.GDE_MSGT3110), //NiXx Automatic
				Messages.getString(MessageIds.GDE_MSGT3111), //LiXy Automatic
				Messages.getString(MessageIds.GDE_MSGT3112), //Normal
				Messages.getString(MessageIds.GDE_MSGT3113), //Linear
				Messages.getString(MessageIds.GDE_MSGT3118), //CV-CC
				Messages.getString(MessageIds.GDE_MSGT3120), //N-Storage
				Messages.getString(MessageIds.GDE_MSGT3119), //CV-LINK
				Messages.getString(MessageIds.GDE_MSGT3116), //reflex
				Messages.getString(MessageIds.GDE_MSGT3121), //CV-Fast
				Messages.getString(MessageIds.GDE_MSGT3117), //re-peak
				Messages.getString(MessageIds.GDE_MSGT3118), //CV-CC
				Messages.getString(MessageIds.GDE_MSGT3119), //CV-LINK
				Messages.getString(MessageIds.GDE_MSGT3120), //N-Storage
				Messages.getString(MessageIds.GDE_MSGT3122) };//QLagern
		this.DISCHARGE_TYPE = new String[] {
				Messages.getString(MessageIds.GDE_MSGT3110), //NiXx Automatic
				Messages.getString(MessageIds.GDE_MSGT3111), //LiXy Automatic
				Messages.getString(MessageIds.GDE_MSGT3112), //Normal
				Messages.getString(MessageIds.GDE_MSGT3113), //Linear
				Messages.getString(MessageIds.GDE_MSGT3124), //LINK
				Messages.getString(MessageIds.GDE_MSGT3120), //N-Storage
				Messages.getString(MessageIds.GDE_MSGT3122) };//Q-Storage
	}

	/**
	 * query the device identifier to differentiate between different device implementations
	 * @return 	0=unknown, 1=PolaronEx, 2=PolaronAcDcEQ, 3=PolaronAcDc, 4=PolaronPro, 5=PolaronSports
	 */
	@Override
	public GraupnerDeviceType getDeviceTypeIdentifier() {
		return GraupnerDeviceType.PolaronSports;
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public int[] getCellVoltageOrdinals() {
		// 0=VersorgungsSpg 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=BatteryTemperature 7=Balance
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7
		// LINK
		// 0=VersorgungsSpg1 1=Spannung 2=Spannung1 3=Spannung2 4=Strom 5=Strom1 6=Strom2 7=Ladung 8=Ladung1 9=Ladung2 10=Leistung 11=Leistung1 12=Leistung2 13=Energie 14=Energie1 15=Energie2 16=BatteryTemperature1 17=BatteryTemperature2
		// 18=Balance 19=SpannungZelle1 20=SpannungZelle2 21=SpannungZelle3 22=SpannungZelle4 23=SpannungZelle5 24=SpannungZelle6 25=SpannungZelle7
		// 26=SpannungZelle8 27=SpannungZelle9 28=SpannungZelle10 29=SpannungZelle11 30=SpannungZelle12 31=SpannungZelle13 32=SpannungZelle14
		return new int[] { 1, this.channels.getActiveChannelNumber() == 3 ? 7 : 3 };
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
				// 0=VersorgungsSpg1 1=Spannung 2=Spannung1 3=Spannung2 4=Strom 5=Strom1 6=Strom2 7=Ladung 8=Ladung1 9=Ladung2 10=Leistung 11=Leistung1 12=Leistung2 13=Energie 14=Energie1 15=Energie2 16=BatteryTemperature1 17=BatteryTemperature2 18=Balance
				points[0] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[9], (char) dataBuffer[10], (char) dataBuffer[11], (char) dataBuffer[12]), 16);

				points[2] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[25], (char) dataBuffer[26], (char) dataBuffer[27], (char) dataBuffer[28]), 16);
				points[3] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[93], (char) dataBuffer[94], (char) dataBuffer[95], (char) dataBuffer[96]), 16);
				points[1] = points[2] + points[3];

				points[5] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[29], (char) dataBuffer[30], (char) dataBuffer[31], (char) dataBuffer[32]), 16);
				points[6] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[97], (char) dataBuffer[98], (char) dataBuffer[99], (char) dataBuffer[100]), 16);
				points[4] = points[5] + points[6];

				points[8] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[33], (char) dataBuffer[34], (char) dataBuffer[35], (char) dataBuffer[36]), 16);
				points[9] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[101], (char) dataBuffer[102], (char) dataBuffer[103], (char) dataBuffer[104]), 16);
				points[7] = points[8] + points[9];

				points[11] = Double.valueOf(points[2] * points[5] / 1000.0).intValue(); // power U*I [W]
				points[12] = Double.valueOf(points[3] * points[6] / 1000.0).intValue(); // power U*I [W]
				points[10] = points[11] + points[12];

				points[14] = Double.valueOf(points[2] * points[8] / 1000.0).intValue(); // energy U*C [Wh]
				points[15] = Double.valueOf(points[3] * points[9] / 1000.0).intValue(); // energy U*C [Wh]
				points[13] = points[14] + points[15];

				points[16] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[37], (char) dataBuffer[38], (char) dataBuffer[39], (char) dataBuffer[40]), 16);
				String sign = String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[41], (char) dataBuffer[42], (char) dataBuffer[43], (char) dataBuffer[44]);
				if (sign != null && sign.length() > 0 && Integer.parseInt(sign) == 0) points[16] = -1 * points[16];

				points[17] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[105], (char) dataBuffer[106], (char) dataBuffer[107], (char) dataBuffer[108]), 16);
				sign = String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[109], (char) dataBuffer[110], (char) dataBuffer[111], (char) dataBuffer[112]);
				if (sign != null && sign.length() > 0 && Integer.parseInt(sign) == 0) points[17] = -1 * points[17];
				points[18] = 0;

				// 19=SpannungZelle1 20=SpannungZelle2 21=SpannungZelle3 22=SpannungZelle4 23=SpannungZelle5 24=SpannungZelle6 25=SpannungZelle7
				for (int i = 0, j = 0; i < 7; ++i, j += 4) {
					points[i + 19] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[49 + j], (char) dataBuffer[50 + j], (char) dataBuffer[51 + j], (char) dataBuffer[52 + j]),	16);
					if (points[i + 19] > 0) {
						maxVotage = points[i + 19] > maxVotage ? points[i + 19] : maxVotage;
						minVotage = points[i + 19] < minVotage ? points[i + 19] : minVotage;
					}
				}
				// 26=SpannungZelle8 27=SpannungZelle9 28=SpannungZelle10 29=SpannungZelle11 30=SpannungZelle12 31=SpannungZelle13 32=SpannungZelle14
				for (int i = 0, j = 0; i < 7; ++i, j += 4) {
					points[i + 26] = Integer.parseInt(
							String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[117 + j], (char) dataBuffer[118 + j], (char) dataBuffer[119 + j], (char) dataBuffer[120 + j]), 16);
					if (points[i + 26] > 0) {
						maxVotage = points[i + 26] > maxVotage ? points[i + 26] : maxVotage;
						minVotage = points[i + 26] < minVotage ? points[i + 26] : minVotage;
					}
				}
			}
			catch (NumberFormatException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
			//calculate balance on the fly
			points[18] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
		}
		else {
			try {
				// 0=VersorgungsSpg 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=BatteryTemperature 7=Balance 8=SpannungZelle1 9=SpannungZelle2...
				points[0] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[9], (char) dataBuffer[10], (char) dataBuffer[11], (char) dataBuffer[12]), 16);
				points[1] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[25], (char) dataBuffer[26], (char) dataBuffer[27], (char) dataBuffer[28]), 16);
				points[2] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[29], (char) dataBuffer[30], (char) dataBuffer[31], (char) dataBuffer[32]), 16);
				points[3] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[33], (char) dataBuffer[34], (char) dataBuffer[35], (char) dataBuffer[36]), 16);
				points[4] = Double.valueOf(points[1] * points[2] / 1000.0).intValue(); // power U*I [W]
				points[5] = Double.valueOf(points[1] * points[3] / 1000.0).intValue(); // energy U*C [Wh]
				points[6] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[37], (char) dataBuffer[38], (char) dataBuffer[39], (char) dataBuffer[40]), 16);
				String sign = String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[41], (char) dataBuffer[42], (char) dataBuffer[43], (char) dataBuffer[44]);
				if (sign != null && sign.length() > 0 && Integer.parseInt(sign) == 0) points[6] = -1 * points[6];
				points[7] = 0;

				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7
				for (int i = 0, j = 0; i < 7; ++i, j += 4) {
					points[i + 8] = Integer
							.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[49 + j], (char) dataBuffer[50 + j], (char) dataBuffer[51 + j], (char) dataBuffer[52 + j]), 16);
					if (points[i + 8] > 0) {
						maxVotage = points[i + 8] > maxVotage ? points[i + 8] : maxVotage;
						minVotage = points[i + 8] < minVotage ? points[i + 8] : minVotage;
					}
				}
			}
			catch (Exception e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
			//calculate balance on the fly
			points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
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

		if (recordSet.getChannelConfigNumber() == 3) { //LINK
			for (int i = 0; i < recordDataSize; i++) {
				int maxVotage = Integer.MIN_VALUE;
				int minVotage = Integer.MAX_VALUE;
				Polaron.log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize); //$NON-NLS-1$
				System.arraycopy(dataBuffer, i * dataBufferSize, convertBuffer, 0, dataBufferSize);
				//0=VersorgungsSpg1 1=Spannung 2=Spannung1 3=Spannung2 4=Strom 5=Strom1 6=Strom2 7=Ladung 8=Ladung1 9=Ladung2 10=Leistung 11=Leistung1 12=Leistung2 13=Energie 14=Energie1 15=Energie2 16=BatteryTemperature1 17=BatteryTemperature2 18=Balance
				points[0] = (((convertBuffer[0] & 0xff) << 24) + ((convertBuffer[1] & 0xff) << 16) + ((convertBuffer[2] & 0xff) << 8) + ((convertBuffer[3] & 0xff) << 0));

				points[2] = (((convertBuffer[4] & 0xff) << 24) + ((convertBuffer[5] & 0xff) << 16) + ((convertBuffer[6] & 0xff) << 8) + ((convertBuffer[7] & 0xff) << 0));
				points[3] = (((convertBuffer[8] & 0xff) << 24) + ((convertBuffer[9] & 0xff) << 16) + ((convertBuffer[10] & 0xff) << 8) + ((convertBuffer[11] & 0xff) << 0));
				points[1] = points[2] + points[3];

				points[5] = (((convertBuffer[12] & 0xff) << 24) + ((convertBuffer[13] & 0xff) << 16) + ((convertBuffer[14] & 0xff) << 8) + ((convertBuffer[15] & 0xff) << 0));
				points[6] = (((convertBuffer[16] & 0xff) << 24) + ((convertBuffer[17] & 0xff) << 16) + ((convertBuffer[18] & 0xff) << 8) + ((convertBuffer[19] & 0xff) << 0));
				points[4] = points[5] + points[6];

				points[8] = (((convertBuffer[20] & 0xff) << 24) + ((convertBuffer[21] & 0xff) << 16) + ((convertBuffer[22] & 0xff) << 8) + ((convertBuffer[23] & 0xff) << 0));
				points[9] = (((convertBuffer[24] & 0xff) << 24) + ((convertBuffer[25] & 0xff) << 16) + ((convertBuffer[26] & 0xff) << 8) + ((convertBuffer[27] & 0xff) << 0));
				points[7] = points[8] + points[9];

				points[10] = Double.valueOf(points[1] / 1000.0 * points[4]).intValue(); // power U*I [W]
				points[11] = Double.valueOf(points[2] / 1000.0 * points[5]).intValue(); // power U*I [W]
				points[12] = Double.valueOf(points[3] / 1000.0 * points[6]).intValue(); // power U*I [W]
				points[13] = Double.valueOf(points[1] / 1000.0 * points[7]).intValue(); // energy U*C [Wh]
				points[14] = Double.valueOf(points[1] / 1000.0 * points[8]).intValue(); // energy U*C [Wh]
				points[15] = Double.valueOf(points[1] / 1000.0 * points[9]).intValue(); // energy U*C [Wh]

				points[16] = (((convertBuffer[28] & 0xff) << 24) + ((convertBuffer[29] & 0xff) << 16) + ((convertBuffer[30] & 0xff) << 8) + ((convertBuffer[31] & 0xff) << 0));
				points[17] = (((convertBuffer[32] & 0xff) << 24) + ((convertBuffer[33] & 0xff) << 16) + ((convertBuffer[34] & 0xff) << 8) + ((convertBuffer[35] & 0xff) << 0));
				points[18] = 0;

				// 19=SpannungZelle1 20=SpannungZelle2 21=SpannungZelle3 22=SpannungZelle4 23=SpannungZelle5 24=SpannungZelle6 25=SpannungZelle7
				// 26=SpannungZelle8 27=SpannungZelle9 28=SpannungZelle10 29=SpannungZelle11 30=SpannungZelle12 31=SpannungZelle13 32=SpannungZelle14
				for (int j = 0, k = 0; j < 14; ++j, k += GDE.SIZE_BYTES_INTEGER) {
					points[j + 19] = (((convertBuffer[k + 36] & 0xff) << 24) + ((convertBuffer[k + 37] & 0xff) << 16) + ((convertBuffer[k + 38] & 0xff) << 8) + ((convertBuffer[k + 39] & 0xff) << 0));
					if (points[j + 19] > 0) {
						maxVotage = points[j + 19] > maxVotage ? points[j + 19] : maxVotage;
						minVotage = points[j + 19] < minVotage ? points[j + 19] : minVotage;
					}
				}

				//calculate balance on the fly
				points[18] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

				recordSet.addPoints(points);

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
			}
		}
		else {
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
				for (int j = 0, k = 0; j < 7; ++j, k += GDE.SIZE_BYTES_INTEGER) {
					points[j + 8] = (((convertBuffer[k + 20] & 0xff) << 24) + ((convertBuffer[k + 21] & 0xff) << 16) + ((convertBuffer[k + 22] & 0xff) << 8) + ((convertBuffer[k + 23] & 0xff) << 0));
					if (points[j + 8] > 0) {
						maxVotage = points[j + 8] > maxVotage ? points[j + 8] : maxVotage;
						minVotage = points[j + 8] < minVotage ? points[j + 8] : minVotage;
					}
				}
				//calculate balance on the fly
				points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

				recordSet.addPoints(points);

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
			}
		}

		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
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
			String tmpProcessing = String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[81], (char) dataBuffer[82], (char) dataBuffer[83], (char) dataBuffer[84]);
			int processingModeOut2 = tmpProcessing != null && tmpProcessing.length() > 0 ? Integer.parseInt(tmpProcessing, 16) : 0;
			if (Polaron.log.isLoggable(java.util.logging.Level.FINE)) {
				Polaron.log.log(java.util.logging.Level.FINE, "processingModeOut2 = " + this.PROCESSING_MODE[processingModeOut2]); //$NON-NLS-1$
			}
			return !(processingModeOut2 == Polaron.OPERATIONS_MODE_NONE || processingModeOut2 == Polaron.OPERATIONS_MODE_ERROR);
		}
		else
			return false;
	}

	/**
	 * query the processing mode, main modes are charge/discharge, make sure the data buffer contains at index 15,16 the processing modes
	 * @param dataBuffer
	 * @return 0 = no processing, 1 = charge, 2 = discharge, 3 = delay, 4 = pause, 5 = current operation finished, 6 = error, 7 = balancer, 8 = tire heater, 9 = motor
	 */
	@Override
	public int getProcessingMode(byte[] dataBuffer) {
		String processingMode = String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[13], (char) dataBuffer[14], (char) dataBuffer[15], (char) dataBuffer[16]);
		return processingMode != null && processingMode.length() > 0 ? Integer.parseInt(processingMode, 16) : 0;
	}

	/**
	 * query the charge mode, main modes are automatic/normal/CVCC, make sure the data buffer contains at index 15,16 the processing modes, type at index 17,18
	 * @param dataBuffer
	 * @return string of charge mode
	 */
	@Override
	public String getProcessingType(byte[] dataBuffer) {
		String type = GDE.STRING_EMPTY;
		int processingMode = this.getProcessingMode(dataBuffer);
		String processingType = String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[17], (char) dataBuffer[18], (char) dataBuffer[19], (char) dataBuffer[20]);
		switch (processingMode) {
		case 1: //charge
			type = processingType != null && processingType.length() > 0 ? this.CHARGE_TYPE[Integer.parseInt(processingType, 16)] : GDE.STRING_EMPTY;
			break;
		case 2: //discharge
			type = processingType != null && processingType.length() > 0 ? this.DISCHARGE_TYPE[Integer.parseInt(processingType, 16)] : GDE.STRING_EMPTY;
			break;
//		case 3: //delay
//			type = processingType != null && processingType.length() > 0 ? this.DELAY_MODE[Integer.parseInt(processingType, 16)] : GDE.STRING_EMPTY;
//			break;
//		case 5: //last active
//			type = processingType != null && processingType.length() > 0 ? this.CURRENT_MODE[Integer.parseInt(processingType, 16)] : GDE.STRING_EMPTY;
//			break;
		case 6: //error
			GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGE3118) + String.format("%02d", Integer.parseInt(processingType, 16)), SWT.COLOR_RED); //$NON-NLS-1$
			break;
		}
		if (log.isLoggable(Level.FINE)) {
			//operation: 0=no processing 1=charge 2=discharge 3=delay 4=auto balance 5=error
			log.log(Level.FINE, "processingMode=" + processingMode + " processingType=" + type);
		}
		return type;
	}

	/**
	 * query if outlets are linked together to charge identical batteries in parallel
	 * @param dataBuffer
	 * @return true | false
	 */
	@Override
	public boolean isLinkedMode(byte[] dataBuffer) {
		int operationMode1 = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[13], (char) dataBuffer[14], (char) dataBuffer[15], (char) dataBuffer[16]), 16);
		int operationMode2 = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[81], (char) dataBuffer[82], (char) dataBuffer[83], (char) dataBuffer[84]), 16);
		int operationType1 = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[17], (char) dataBuffer[18], (char) dataBuffer[19], (char) dataBuffer[20]), 16);
		int operationType2 = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[85], (char) dataBuffer[86], (char) dataBuffer[87], (char) dataBuffer[88]), 16);
		return operationMode1 == operationMode2 && operationType1 == operationType2
				&& ((operationMode1 == 1 && operationType1 == Polaron.OPERATIONS_MODE_LINK_CHARGE))
						|| (operationMode1 == 2 && operationType1 == Polaron.OPERATIONS_MODE_LINK_DISCHARGE);
	}

	/**
	 * query the product code 0=Ultramat50, 1=Ultramat40, 2=Ultramat14Trio, 3=Ultramat18, 4=ultramat45, 5=Ultramat60, 6=Ultramat80
	 * @param dataBuffer
	 * @return v2.0
	 */
	@Override
	public int getProductCode(byte[] dataBuffer) {
		//0=unknown, 1=PolaronEx, 2=PolaronAcDcEQ, 3=PolaronAcDc, 4=PolaronPro, 5=PolaronSports
		return Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[5], (char) dataBuffer[6], (char) dataBuffer[7], (char) dataBuffer[8]), 16);
	}

	/**
	 * query the firmware version
	 * @param dataBuffer
	 * @return v2.0
	 */
	@Override
	public String getFirmwareVersion(byte[] dataBuffer) {
		return String.format(Locale.ENGLISH, "%.3f", (Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[1], (char) dataBuffer[2], (char) dataBuffer[3], (char) dataBuffer[4]), 16) / 1000.0)); //$NON-NLS-1$
	}

	/**
	 * set the temperature unit right after creating a record set according to the used oultet channel
	 * @param channelNumber
	 * @param recordSet
	 * @param dataBuffer
	 */
	@Override
	public void setTemperatureUnit(int channelNumber, RecordSet recordSet, byte[] dataBuffer) {
		int unit = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[45], (char) dataBuffer[46], (char) dataBuffer[47], (char) dataBuffer[48]), 16);
		if (unit == 0) {
			this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 6, DeviceConfiguration.UNIT_DEGREE_CELSIUS);
			this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 6, DeviceConfiguration.UNIT_DEGREE_CELSIUS);
		}
		else if (unit == 1) {
			this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 6, DeviceConfiguration.UNIT_DEGREE_FAHRENHEIT);
			this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 6, DeviceConfiguration.UNIT_DEGREE_FAHRENHEIT);
		}
	}

	/**
	 * query the cycle number of the given outlet channel
	 * @param outletNum
	 * @param dataBuffer
	 * @return
	 */
	@Override
	public int getCycleNumber(int outletNum, byte[] dataBuffer) {
		String cycleNumber = String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[21], (char) dataBuffer[22], (char) dataBuffer[23], (char) dataBuffer[24]);
		if (outletNum == 2) {
			try {
				cycleNumber = String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[89], (char) dataBuffer[90], (char) dataBuffer[91], (char) dataBuffer[92]);
			}
			catch (Exception e) {
				// ignore and use values from outlet channel 1 (data buffer will be copied)
			}
		}
		return Integer.parseInt(cycleNumber, 16);
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	@Override
	public void open_closeCommPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					this.serialPort.open();
					this.serialPort.write(PolaronSerialPort.RESET);
					try {
						byte[] dataBuffer = this.serialPort.getData(true);
						this.firmware = this.getFirmwareVersion(dataBuffer);
						//check if device fits this.device.getProductCode(dataBuffer)
						if (!this.getDeviceTypeIdentifier().name().equals(this.getClass().getSimpleName()) && this.getDeviceTypeIdentifier().ordinal() != this.getProductCode(dataBuffer)) {
							int answer = this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGW3102, new String[] {GraupnerDeviceType.values()[this.getProductCode(dataBuffer)].toString()}));
							if (answer == SWT.YES) {
								this.application.getDeviceSelectionDialog().setupDevice(GraupnerDeviceType.values()[this.getProductCode(dataBuffer)].toString());
								this.serialPort.close();
								return;
							}
						}
					}
					catch (SerialPortException e) {
						if (this.serialPort.isConnected()) this.serialPort.write(PolaronSerialPort.RESET);
						throw e;
					}
					catch (TimeOutException e) {
						if (this.serialPort.isConnected()) this.serialPort.write(PolaronSerialPort.RESET);
						throw new SerialPortException(e.getMessage());
					}
					catch (Exception e) {
						if (this.serialPort.isConnected()) this.serialPort.write(PolaronSerialPort.RESET);
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						throw e;
					}

					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.dataGatherThread = new PolaronGathererThread();
						try {
							if (this.serialPort.isConnected()) {
								this.dataGatherThread.start();
							}
						}
						catch (RuntimeException e) {
							log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
						catch (Throwable e) {
							log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
					}
				}
				catch (SerialPortException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.serialPort.close();
					this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (ApplicationConfigurationException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.serialPort.close();
					this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (Throwable e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.serialPort.close();
				}
			}
			else {
				if (this.dataGatherThread != null) {
					try {
						this.dataGatherThread.stopDataGatheringThread(false, null);
					}
					catch (Exception e) {
						// ignore, while stopping no exception will be thrown
					}
				}
				this.serialPort.close();
			}
		}
	}
}
