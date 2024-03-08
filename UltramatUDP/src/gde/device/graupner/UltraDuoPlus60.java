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

    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DesktopPropertyType;
import gde.device.DesktopPropertyTypes;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.exception.DataInconsitsentException;
import gde.messages.Messages;

/**
 * Graupner Ultra Duo Plus 60 base class
 * @author Winfried Brügmann
 */
public class UltraDuoPlus60 extends Ultramat {
	final static Logger	logger	= Logger.getLogger(UltraDuoPlus60.class.getName());

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public UltraDuoPlus60(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202),
				Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGT2204), Messages.getString(MessageIds.GDE_MSGT2205), Messages.getString(MessageIds.GDE_MSGT2206),
				Messages.getString(MessageIds.GDE_MSGT2207), Messages.getString(MessageIds.GDE_MSGT2208), Messages.getString(MessageIds.GDE_MSGT2209) };
		this.CHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212),
				Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2215), Messages.getString(MessageIds.GDE_MSGT2216),
				Messages.getString(MessageIds.GDE_MSGT2217), Messages.getString(MessageIds.GDE_MSGT2218), Messages.getString(MessageIds.GDE_MSGT2219), Messages.getString(MessageIds.GDE_MSGT2220),
				Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222) };
		this.DISCHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212),
				Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2223), Messages.getString(MessageIds.GDE_MSGT2224), Messages.getString(MessageIds.GDE_MSGT2220),
				Messages.getString(MessageIds.GDE_MSGT2222) };
		this.DELAY_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2225), Messages.getString(MessageIds.GDE_MSGT2226), Messages.getString(MessageIds.GDE_MSGT2227),
				Messages.getString(MessageIds.GDE_MSGT2228) };
		this.CURRENT_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2229), Messages.getString(MessageIds.GDE_MSGT2230), Messages.getString(MessageIds.GDE_MSGT2231),
				Messages.getString(MessageIds.GDE_MSGT2232), Messages.getString(MessageIds.GDE_MSGT2233), Messages.getString(MessageIds.GDE_MSGT2234), Messages.getString(MessageIds.GDE_MSGT2235),
				Messages.getString(MessageIds.GDE_MSGT2236), Messages.getString(MessageIds.GDE_MSGT2237) };

		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public UltraDuoPlus60(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202),
				Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGT2204), Messages.getString(MessageIds.GDE_MSGT2205), Messages.getString(MessageIds.GDE_MSGT2206),
				Messages.getString(MessageIds.GDE_MSGT2207), Messages.getString(MessageIds.GDE_MSGT2208), Messages.getString(MessageIds.GDE_MSGT2209) };
		this.CHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212),
				Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2215), Messages.getString(MessageIds.GDE_MSGT2216),
				Messages.getString(MessageIds.GDE_MSGT2217), Messages.getString(MessageIds.GDE_MSGT2218), Messages.getString(MessageIds.GDE_MSGT2219), Messages.getString(MessageIds.GDE_MSGT2220),
				Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222) };
		this.DISCHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212),
				Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2223), Messages.getString(MessageIds.GDE_MSGT2224), Messages.getString(MessageIds.GDE_MSGT2220),
				Messages.getString(MessageIds.GDE_MSGT2222) };
		this.DELAY_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2225), Messages.getString(MessageIds.GDE_MSGT2226), Messages.getString(MessageIds.GDE_MSGT2227),
				Messages.getString(MessageIds.GDE_MSGT2228) };
		this.CURRENT_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2229), Messages.getString(MessageIds.GDE_MSGT2230), Messages.getString(MessageIds.GDE_MSGT2231),
				Messages.getString(MessageIds.GDE_MSGT2232), Messages.getString(MessageIds.GDE_MSGT2233), Messages.getString(MessageIds.GDE_MSGT2234), Messages.getString(MessageIds.GDE_MSGT2235),
				Messages.getString(MessageIds.GDE_MSGT2236), Messages.getString(MessageIds.GDE_MSGT2237) };

		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device
	 */
	@Override
	public int getLovDataByteSize() {
		return 150;
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
		int deviceDataBufferSize2 = deviceDataBufferSize / 2;
		int channel2Offset = deviceDataBufferSize2 - 5;
		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigNumber())];
		int offset = 4;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		int outputChannel = recordSet.getChannelConfigNumber();

		if (dataBuffer[offset] == 0x0C) {
			byte[] convertBuffer = new byte[deviceDataBufferSize];
			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

			for (int i = 0; i < recordDataSize; i++) {
				if (outputChannel == 1)
					System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize2);
				else if (outputChannel == 2)
					System.arraycopy(dataBuffer, channel2Offset + offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize2);
				else if (outputChannel == 3) System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize);
				recordSet.addPoints(convertDataBytes(points, convertBuffer));

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}

			recordSet.setTimeStep_ms(this.getAverageTimeStep_ms() != null ? this.getAverageTimeStep_ms() : 1000); // no average time available, use a hard coded one
		}

		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
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
				// 0=Spannung 1=Spannung1 2=Spannung2 3=Strom 4=Strom1 5=Strom2 6=Ladung 7=Ladung1 8=Ladung2 9=Leistung 10=Leistung1 11=Leistung2 12=Energie 13=Energie1 14=Energie2 15=BatteryTemperature1 16=BatteryTemperature2 17=VersorgungsSpg1 18=Balance
				points[1] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[21], (char) dataBuffer[22], (char) dataBuffer[23], (char) dataBuffer[24]), 16);
				points[2] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[85], (char) dataBuffer[86], (char) dataBuffer[87], (char) dataBuffer[88]), 16);
				points[0] = points[1] + points[2];
				points[4] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[25], (char) dataBuffer[26], (char) dataBuffer[27], (char) dataBuffer[28]), 16);
				points[5] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[89], (char) dataBuffer[90], (char) dataBuffer[91], (char) dataBuffer[92]), 16);
				points[3] = points[4] + points[5];
				points[7] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[29], (char) dataBuffer[30], (char) dataBuffer[31], (char) dataBuffer[32]), 16);
				points[8] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[93], (char) dataBuffer[94], (char) dataBuffer[95], (char) dataBuffer[96]), 16);
				points[6] = points[7] + points[8];
				points[10] = Double.valueOf(points[1] * points[4] / 1000.0).intValue(); // power U*I [W]
				points[11] = Double.valueOf(points[2] * points[5] / 1000.0).intValue(); // power U*I [W]
				points[9] = points[10] + points[11];
				points[13] = Double.valueOf(points[0] * points[2] / 1000.0).intValue(); // energy U*C [Wh]
				points[14] = Double.valueOf(points[0] * points[2] / 1000.0).intValue(); // energy U*C [Wh]
				points[12] = points[13] + points[14];
				points[15] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[33], (char) dataBuffer[34], (char) dataBuffer[35], (char) dataBuffer[36]), 16);
				String sign = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[37], (char) dataBuffer[38]);
				if (sign != null && sign.length() > 0 && Integer.parseInt(sign) == 0) points[15] = -1 * points[15];
				points[16] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[97], (char) dataBuffer[98], (char) dataBuffer[99], (char) dataBuffer[100]), 16);
				sign = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[101], (char) dataBuffer[102]);
				if (sign != null && sign.length() > 0 && Integer.parseInt(sign) == 0) points[16] = -1 * points[16];
				points[17] = (Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[11], (char) dataBuffer[12], (char) dataBuffer[13], (char) dataBuffer[14]), 16)
						+ Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[75], (char) dataBuffer[76], (char) dataBuffer[77], (char) dataBuffer[78]), 16)) >>> 1;
				points[18] = 0;

				// 19=SpannungZelle1 20=SpannungZelle2 21=SpannungZelle3 22=SpannungZelle4 23=SpannungZelle5 24=SpannungZelle6 25=SpannungZelle7
				for (int i = 0, j = 0; i < 7; ++i, j += 4) {
					points[i + 19] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[41 + j], (char) dataBuffer[42 + j], (char) dataBuffer[43 + j], (char) dataBuffer[44 + j]),	16);
					if (points[i + 19] > 0) {
						maxVotage = points[i + 19] > maxVotage ? points[i + 19] : maxVotage;
						minVotage = points[i + 19] < minVotage ? points[i + 19] : minVotage;
					}
				}
				// 26=SpannungZelle8 27=SpannungZelle9 28=SpannungZelle10 29=SpannungZelle11 30=SpannungZelle12 31=SpannungZelle13 32=SpannungZelle14
				for (int i = 0, j = 0; i < 7; ++i, j += 4) {
					points[i + 26] = Integer.parseInt(
							String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[105 + j], (char) dataBuffer[106 + j], (char) dataBuffer[107 + j], (char) dataBuffer[108 + j]), 16);
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
				// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance
				points[0] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[21], (char) dataBuffer[22], (char) dataBuffer[23], (char) dataBuffer[24]), 16);
				points[1] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[25], (char) dataBuffer[26], (char) dataBuffer[27], (char) dataBuffer[28]), 16);
				points[2] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[29], (char) dataBuffer[30], (char) dataBuffer[31], (char) dataBuffer[32]), 16);
				points[3] = Double.valueOf(points[0] * points[1] / 1000.0).intValue(); // power U*I [W]
				points[4] = Double.valueOf(points[0] * points[2] / 1000.0).intValue(); // energy U*C [Wh]
				points[5] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[33], (char) dataBuffer[34], (char) dataBuffer[35], (char) dataBuffer[36]), 16);
				String sign = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[37], (char) dataBuffer[38]);
				if (sign != null && sign.length() > 0 && Integer.parseInt(sign) == 0) points[5] = -1 * points[5];
				points[6] = Integer.parseInt(String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[11], (char) dataBuffer[12], (char) dataBuffer[13], (char) dataBuffer[14]), 16);
				points[7] = 0;
				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7
				for (int i = 0, j = 0; i < points.length - 8; ++i, j += 4) {
					points[i + 8] = Integer.parseInt(
							String.format(DeviceCommPort.FORMAT_4_CHAR, (char) dataBuffer[41 + j], (char) dataBuffer[42 + j], (char) dataBuffer[43 + j], (char) dataBuffer[44 + j]), 16);
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
				logger.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize); //$NON-NLS-1$
				System.arraycopy(dataBuffer, i * dataBufferSize, convertBuffer, 0, dataBufferSize);
				// 0=Spannung 1=Spannung1 2=Spannung2 3=Strom 4=Strom1 5=Strom2 6=Ladung 7=Ladung1 8=Ladung2 9=Leistung 10=Leistung1 11=Leistung2 12=Energie 13=Energie1 14=Energie2 15=BatteryTemperature1 16=BatteryTemperature2 17=VersorgungsSpg1 18=Balance
				points[1] = (((convertBuffer[0] & 0xff) << 24) + ((convertBuffer[1] & 0xff) << 16) + ((convertBuffer[2] & 0xff) << 8) + ((convertBuffer[3] & 0xff) << 0));
				points[2] = (((convertBuffer[4] & 0xff) << 24) + ((convertBuffer[5] & 0xff) << 16) + ((convertBuffer[6] & 0xff) << 8) + ((convertBuffer[7] & 0xff) << 0));
				points[0] = points[1] + points[2];
				points[4] = (((convertBuffer[8] & 0xff) << 24) + ((convertBuffer[9] & 0xff) << 16) + ((convertBuffer[10] & 0xff) << 8) + ((convertBuffer[11] & 0xff) << 0));
				points[5] = (((convertBuffer[12] & 0xff) << 24) + ((convertBuffer[13] & 0xff) << 16) + ((convertBuffer[14] & 0xff) << 8) + ((convertBuffer[15] & 0xff) << 0));
				points[3] = points[4] + points[5];
				points[7] = (((convertBuffer[16] & 0xff) << 24) + ((convertBuffer[17] & 0xff) << 16) + ((convertBuffer[18] & 0xff) << 8) + ((convertBuffer[19] & 0xff) << 0));
				points[8] = (((convertBuffer[20] & 0xff) << 24) + ((convertBuffer[21] & 0xff) << 16) + ((convertBuffer[22] & 0xff) << 8) + ((convertBuffer[23] & 0xff) << 0));
				points[6] = points[7] + points[8];
				points[9] = Double.valueOf(points[0] / 1000.0 * points[3]).intValue(); // power U*I [W]
				points[10] = Double.valueOf(points[1] / 1000.0 * points[4]).intValue(); // power U*I [W]
				points[11] = Double.valueOf(points[2] / 1000.0 * points[5]).intValue(); // power U*I [W]
				points[12] = Double.valueOf(points[0] / 1000.0 * points[6]).intValue(); // energy U*C [Wh]
				points[13] = Double.valueOf(points[0] / 1000.0 * points[7]).intValue(); // energy U*C [Wh]
				points[14] = Double.valueOf(points[0] / 1000.0 * points[8]).intValue(); // energy U*C [Wh]
				points[15] = (((convertBuffer[24] & 0xff) << 24) + ((convertBuffer[25] & 0xff) << 16) + ((convertBuffer[26] & 0xff) << 8) + ((convertBuffer[27] & 0xff) << 0));
				points[16] = (((convertBuffer[28] & 0xff) << 24) + ((convertBuffer[29] & 0xff) << 16) + ((convertBuffer[30] & 0xff) << 8) + ((convertBuffer[31] & 0xff) << 0));
				points[17] = (((convertBuffer[32] & 0xff) << 24) + ((convertBuffer[33] & 0xff) << 16) + ((convertBuffer[34] & 0xff) << 8) + ((convertBuffer[35] & 0xff) << 0));
				points[18] = 0;

				// 19=SpannungZelle1 20=SpannungZelle2 21=SpannungZelle3 22=SpannungZelle4 23=SpannungZelle5 24=SpannungZelle6 25=SpannungZelle7
				// 26=SpannungZelle8 27=SpannungZelle9 28=SpannungZelle10 29=SpannungZelle11 30=SpannungZelle12 31=SpannungZelle13 32=SpannungZelle14
				for (int j = 0, k = 0; j < points.length - 20; ++j, k += GDE.SIZE_BYTES_INTEGER) {
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
				logger.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize); //$NON-NLS-1$
				System.arraycopy(dataBuffer, i * dataBufferSize, convertBuffer, 0, dataBufferSize);
				// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance
				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
				points[0] = (((convertBuffer[0] & 0xff) << 24) + ((convertBuffer[1] & 0xff) << 16) + ((convertBuffer[2] & 0xff) << 8) + ((convertBuffer[3] & 0xff) << 0));
				points[1] = (((convertBuffer[4] & 0xff) << 24) + ((convertBuffer[5] & 0xff) << 16) + ((convertBuffer[6] & 0xff) << 8) + ((convertBuffer[7] & 0xff) << 0));
				points[2] = (((convertBuffer[8] & 0xff) << 24) + ((convertBuffer[9] & 0xff) << 16) + ((convertBuffer[10] & 0xff) << 8) + ((convertBuffer[11] & 0xff) << 0));
				points[3] = Double.valueOf(points[0] / 1000.0 * points[1]).intValue(); // power U*I [W]
				points[4] = Double.valueOf(points[0] / 1000.0 * points[2]).intValue(); // energy U*C [Wh]
				points[5] = (((convertBuffer[12] & 0xff) << 24) + ((convertBuffer[13] & 0xff) << 16) + ((convertBuffer[14] & 0xff) << 8) + ((convertBuffer[15] & 0xff) << 0));
				points[6] = (((convertBuffer[16] & 0xff) << 24) + ((convertBuffer[17] & 0xff) << 16) + ((convertBuffer[18] & 0xff) << 8) + ((convertBuffer[19] & 0xff) << 0));
				points[7] = 0;

				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
				for (int j = 0, k = 0; j < points.length - 8; ++j, k += GDE.SIZE_BYTES_INTEGER) {
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
		recordSet.setAllDisplayable();
		int numCells = recordSet.getChannelConfigNumber() < 3 ? 7 : 14;
		for (int i = recordSet.size() - numCells - 1; i < recordSet.size(); ++i) {
			Record record = recordSet.get(i);
			record.setDisplayable(record.getOrdinal() <= 5 || record.hasReasonableData());
			log.log(java.util.logging.Level.FINER, record.getName() + " setDisplayable=" + (record.getOrdinal() <= 5 || record.hasReasonableData())); //$NON-NLS-1$

			if (record.isActive() && record.isDisplayable()) {
				++displayableCounter;
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			for (int i = 0; i < recordSet.size(); i++) {
				Record record = recordSet.get(i);
				logger.log(Level.FINE, record.getName() + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		recordSet.setConfiguredDisplayable(displayableCounter);
	}

	/**
	 * query the device identifier to differentiate between different device implementations
	 * @return 	-1=Ultramat16 1=Ultramat50, 2=Ultramat40, 3=UltramatTrio14, 4=Ultramat45, 5=Ultramat60, 6=Ultramat16S
	 */
	@Override
	public GraupnerDeviceType getDeviceTypeIdentifier() {
		return GraupnerDeviceType.UltraDuoPlus60;
	}

	/**
	 * query if the target measurement reference ordinal used by the given desktop type
	 * @return the target measurement reference ordinal, -1 if reference ordinal not set
	 */
	@Override
	public int getDesktopTargetReferenceOrdinal(DesktopPropertyTypes desktopPropertyType) {
		DesktopPropertyType property = this.getDesktopProperty(desktopPropertyType);
		if (this.channels.getActiveChannelNumber() == 3) {
			return property != null ? property.getTargetReferenceOrdinal() + 11 : -1;
		}
		return property != null ? property.getTargetReferenceOrdinal() : -1;
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public int[] getCellVoltageOrdinals() {
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
		// 16=SpannungZelle8 17=SpannungZelle9 18=SpannungZelle10 19=SpannungZelle11 20=SpannungZelle12 21=SpannungZelle13 22=SpannungZelle14

		// LINK
		// 0=Spannung 1=Spannung1 2=Spannung2 3=Strom 4=Strom1 5=Strom2 6=Ladung 7=Ladung1 8=Ladung2 9=Leistung 10=Leistung1 11=Leistung2 12=Energie 13=Energie1 14=Energie2 15=BatteryTemperature1 16=BatteryTemperature2 17=VersorgungsSpg1 18=Balance
		// 19=SpannungZelle1 20=SpannungZelle2 21=SpannungZelle3 22=SpannungZelle4 23=SpannungZelle5 24=SpannungZelle6 25=SpannungZelle7
		// 26=SpannungZelle8 27=SpannungZelle9 28=SpannungZelle10 29=SpannungZelle11 30=SpannungZelle12 31=SpannungZelle13 32=SpannungZelle14
		return new int[] { 0, this.channels.getActiveChannelNumber() == 3 ? 6 : 2 };
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
			String operationModeOut1 = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[15], (char) dataBuffer[16]);
			if (logger.isLoggable(java.util.logging.Level.FINE)) {
				logger.log(java.util.logging.Level.FINE,
						"operationModeOut1 = " + (operationModeOut1 != null && operationModeOut1.length() > 0 ? this.USAGE_MODE[Integer.parseInt(operationModeOut1, 16)] : operationModeOut1)); //$NON-NLS-1$
			}
			if (this.settings.isReduceChargeDischarge())
				return operationModeOut1 != null && operationModeOut1.length() == 2 && (operationModeOut1.equals("01") || operationModeOut1.equals("02") || (this.settings.isContinuousRecordSet() && operationModeOut1.equals("03")));
			return operationModeOut1 != null && operationModeOut1.length() == 2 && !(operationModeOut1.equals(Ultramat.OPERATIONS_MODE_NONE) || operationModeOut1.equals(Ultramat.OPERATIONS_MODE_ERROR));
		}
		else if (outletNum == 2) {
			String operationModeOut2 = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[79], (char) dataBuffer[80]);
			if (logger.isLoggable(java.util.logging.Level.FINE)) {
				logger.log(java.util.logging.Level.FINE,
						"operationModeOut2 = " + (operationModeOut2 != null && operationModeOut2.length() > 0 ? this.USAGE_MODE[Integer.parseInt(operationModeOut2, 16)] : operationModeOut2)); //$NON-NLS-1$
			}
			if (this.settings.isReduceChargeDischarge())
				return operationModeOut2 != null && operationModeOut2.length() == 2 && (operationModeOut2.equals("01") || operationModeOut2.equals("02") || (this.settings.isContinuousRecordSet() && operationModeOut2.equals("03")));
			return operationModeOut2 != null && operationModeOut2.length() == 2 && !(operationModeOut2.equals(Ultramat.OPERATIONS_MODE_NONE) || operationModeOut2.equals(Ultramat.OPERATIONS_MODE_ERROR));
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
		String operationMode = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[15], (char) dataBuffer[16]);
		return operationMode != null && operationMode.length() > 0 ? Integer.parseInt(operationMode, 16) : 0;
	}

	/**
	 * query the charge mode, main modes are automatic/normal/CVCC, make sure the data buffer contains at index 15,16 the processing modes, type at index 17,18
	 * @param dataBuffer
	 * @return string of charge mode
	 */
	@Override
	public String getProcessingType(byte[] dataBuffer) {
		String type = GDE.STRING_EMPTY;
		String operationMode = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[15], (char) dataBuffer[16]);
		int opMode = operationMode != null && operationMode.length() > 0 ? Integer.parseInt(operationMode, 16) : 0;
		String operationType = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[17], (char) dataBuffer[18]);
		switch (opMode) {
		case 1: //charge
			type = operationType != null && operationType.length() > 0 ? this.CHARGE_MODE[Integer.parseInt(operationType, 16)] : GDE.STRING_EMPTY;
			break;
		case 2: //discharge
			type = operationType != null && operationType.length() > 0 ? this.DISCHARGE_MODE[Integer.parseInt(operationType, 16)] : GDE.STRING_EMPTY;
			break;
		case 3: //delay
			type = operationType != null && operationType.length() > 0 ? this.DELAY_MODE[Integer.parseInt(operationType, 16)] : GDE.STRING_EMPTY;
			break;
		case 5: //last active
			type = operationType != null && operationType.length() > 0 ? this.CURRENT_MODE[Integer.parseInt(operationType, 16)] : GDE.STRING_EMPTY;
			break;
		case 6: //error
			GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGE2218) + String.format("%02d", Integer.parseInt(operationType, 16)), SWT.COLOR_RED); //$NON-NLS-1$
			break;
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
		String operationMode1 = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[15], (char) dataBuffer[16]);
		String operationMode2 = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[79], (char) dataBuffer[80]);
		String operationType1 = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[17], (char) dataBuffer[18]);
		String operationType2 = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[81], (char) dataBuffer[82]);
		return operationMode1.equals(operationMode2) && operationType1.equals(operationType2)
				&& ((operationMode1.equals("01") && operationType1.equals(Ultramat.OPERATIONS_MODE_LINK_CHARGE))
						|| (operationMode1.equals("02") && operationType1.equals(Ultramat.OPERATIONS_MODE_LINK_DISCHARGE)));
	}

	/**
	 * query the battery memory number of the given outlet channel
	 * @param outletNum
	 * @param dataBuffer
	 * @return
	 */
	@Override
	public int getBatteryMemoryNumber(int outletNum, byte[] dataBuffer) {
		String batteryMemoryNumber = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[5], (char) dataBuffer[6]);
		if (outletNum == 2) {
			try {
				batteryMemoryNumber = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[69], (char) dataBuffer[70]);
			}
			catch (Exception e) {
				// ignore and use values from outlet channel 1 (data buffer will be copied)
			}
		}
		return Integer.parseInt(batteryMemoryNumber, 16);
	}

	/**
	 * query the cycle number of the given outlet channel
	 * @param outletNum
	 * @param dataBuffer
	 * @return
	 */
	@Override
	public int getCycleNumber(int outletNum, byte[] dataBuffer) {
		String cycleNumber = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[19], (char) dataBuffer[20]);
		if (outletNum == 2) {
			try {
				cycleNumber = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[83], (char) dataBuffer[84]);
			}
			catch (Exception e) {
				// ignore and use values from outlet channel 1 (data buffer will be copied)
			}
		}
		return Integer.parseInt(cycleNumber, 16);
	}

	/**
	 * set the temperature unit right after creating a record set according to the used oultet channel
	 * @param channelNumber
	 * @param recordSet
	 * @param dataBuffer
	 */
	@Override
	public void setTemperatureUnit(int channelNumber, RecordSet recordSet, byte[] dataBuffer) {
		String unit = String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[39], (char) dataBuffer[40]);
		if (unit != null && unit.length() > 0) if (channelNumber == 3) {
			if (Integer.parseInt(unit) == 0) {
				this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 15, DeviceConfiguration.UNIT_DEGREE_CELSIUS);
				this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 16, DeviceConfiguration.UNIT_DEGREE_CELSIUS);
			}
			else if (Integer.parseInt(unit) == 1) {
				this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 15, DeviceConfiguration.UNIT_DEGREE_FAHRENHEIT);
				this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 16, DeviceConfiguration.UNIT_DEGREE_FAHRENHEIT);
			}
		}
		else {
			if (Integer.parseInt(unit) == 0)
				this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 5, DeviceConfiguration.UNIT_DEGREE_CELSIUS);
			else if (Integer.parseInt(unit) == 1) this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 5, DeviceConfiguration.UNIT_DEGREE_FAHRENHEIT);
		}
	}

	/**
	 * This function allows to register a custom CTabItem to the main application tab folder to display device
	 * specific curve calculated from point combinations or other specific dialog
	 * As default the function should return null which stands for no device custom tab item.
	 */
	@Override
	public CTabItem getUtilityDeviceTabItem() {
		return this.application.getUtilGraphicsWindow(Messages.getString(MessageIds.GDE_MSGT2340));
	}
}
