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

    Copyright (c) 2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DesktopPropertyType;
import gde.device.DesktopPropertyTypes;
import gde.device.DeviceConfiguration;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.device.graupner.polaron.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * Graupner Ultramat base class
 * @author Winfried Brügmann
 */
public abstract class Polaron extends DeviceConfiguration implements IDevice {
	final static Logger	log	= Logger.getLogger(Polaron.class.getName());

	public enum GraupnerDeviceType {
		//0=unknown, 1=PolaronEx, 2=PolaronAcDcEQ, 3=PolaronAcDc, 4=PolaronPro, 5=PolaronSports, 6=Gemini2030iEvo, 7=Absolute1807, 8=PolronEX1400, 9=UltraDuoPlus80
		unknown, PolaronEx, PolaronACDC_EQ, PolaronACDC, PolaronPro, PolaronSports, Gemini2030iEvo, Absolute1807, PolaronEx1400, UltraDuoPlus80
	};

	protected String[]								PROCESSING_MODE;
	protected String[]								CHARGE_TYPE;
	protected String[]								DISCHARGE_TYPE;
	protected String[]								ERROR_TYPE;

	protected static final int			OPERATIONS_MODE_LINK_DISCHARGE	= 4;
	protected static final int			OPERATIONS_MODE_LINK_CHARGE			= 6;
	protected static final int			OPERATIONS_MODE_ERROR						= 6;
	protected static final int			OPERATIONS_MODE_NONE						= 0;

	protected String									firmware												= GDE.STRING_MINUS;
	protected PolaronGathererThread		dataGatherThread;

	protected final Settings					settings												= Settings.getInstance();
	protected final DataExplorer			application;
	protected final PolaronSerialPort	serialPort;
	protected final Channels					channels;

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public Polaron(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.polaron.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.PROCESSING_MODE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3100), //0 no operation
				Messages.getString(MessageIds.GDE_MSGT3101), //1 charge
				Messages.getString(MessageIds.GDE_MSGT3102), //2 discharge
				Messages.getString(MessageIds.GDE_MSGT3103), //3 pause
				Messages.getString(MessageIds.GDE_MSGT3100), //4 no operation
				Messages.getString(MessageIds.GDE_MSGT3105), //5 Auto-Balancing
				Messages.getString(MessageIds.GDE_MSGT3106), //6 Error
				Messages.getString(MessageIds.GDE_MSGT3107), //7 Balancing
				Messages.getString(MessageIds.GDE_MSGT3122), //8 storage
				Messages.getString(MessageIds.GDE_MSGT3109), //9 Motor
				Messages.getString(MessageIds.GDE_MSGT3100), //10 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //11 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //12 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //13 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //14 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //15 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //16 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //17 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //18 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100)};//19 Unknown
		this.CHARGE_TYPE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3110), //0 NiXx Automatic
				Messages.getString(MessageIds.GDE_MSGT3111), //1 LiXy Automatic
				Messages.getString(MessageIds.GDE_MSGT3112), //2 Normal
				Messages.getString(MessageIds.GDE_MSGT3113), //3 Linear
				Messages.getString(MessageIds.GDE_MSGT3118), //4 charge CV-CC
				Messages.getString(MessageIds.GDE_MSGT3120), //5 N-Storage
				Messages.getString(MessageIds.GDE_MSGT3119), //6 CV-LINK
				Messages.getString(MessageIds.GDE_MSGT3116), //7 reflex
				Messages.getString(MessageIds.GDE_MSGT3121), //8 CV-Fast
				Messages.getString(MessageIds.GDE_MSGT3117), //9 re-peak
				Messages.getString(MessageIds.GDE_MSGT3118), //10 CV-CC
				Messages.getString(MessageIds.GDE_MSGT3119), //11 CV-LINK
				Messages.getString(MessageIds.GDE_MSGT3120), //12 N-Storage
				Messages.getString(MessageIds.GDE_MSGT3122) };//13 QLagern
		this.DISCHARGE_TYPE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3110), //0 NiXx Automatic
				Messages.getString(MessageIds.GDE_MSGT3111), //1 LiXy Automatic
				Messages.getString(MessageIds.GDE_MSGT3112), //2 Normal
				Messages.getString(MessageIds.GDE_MSGT3113), //3 Linear
				Messages.getString(MessageIds.GDE_MSGT3124), //4 LINK
				Messages.getString(MessageIds.GDE_MSGT3118), //5 CV-CC
				Messages.getString(MessageIds.GDE_MSGT3124), //6 LINK
				Messages.getString(MessageIds.GDE_MSGT3120), //7 N-Storage
				Messages.getString(MessageIds.GDE_MSGT3122) };//8 Q-Storage

		this.application = DataExplorer.getInstance();
		this.serialPort = new PolaronSerialPort(this, this.application);
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Polaron(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.polaron.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.PROCESSING_MODE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3100), //0 no operation
				Messages.getString(MessageIds.GDE_MSGT3101), //1 charge
				Messages.getString(MessageIds.GDE_MSGT3102), //2 discharge
				Messages.getString(MessageIds.GDE_MSGT3103), //3 pause
				Messages.getString(MessageIds.GDE_MSGT3100), //4 no operation
				Messages.getString(MessageIds.GDE_MSGT3105), //5 Auto-Balancing
				Messages.getString(MessageIds.GDE_MSGT3106), //6 Error
				Messages.getString(MessageIds.GDE_MSGT3107), //7 Balancing
				Messages.getString(MessageIds.GDE_MSGT3122), //8 storage
				Messages.getString(MessageIds.GDE_MSGT3109), //9 Motor
				Messages.getString(MessageIds.GDE_MSGT3100), //10 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //11 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //12 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //13 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //14 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //15 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //16 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //17 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100), //18 Unknown
				Messages.getString(MessageIds.GDE_MSGT3100)};//19 Unknown
		this.CHARGE_TYPE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3110), //0 NiXx Automatic
				Messages.getString(MessageIds.GDE_MSGT3111), //1 LiXy Automatic
				Messages.getString(MessageIds.GDE_MSGT3112), //2 Normal
				Messages.getString(MessageIds.GDE_MSGT3113), //3 Linear
				Messages.getString(MessageIds.GDE_MSGT3118), //4 charge CV-CC
				Messages.getString(MessageIds.GDE_MSGT3120), //5 N-Storage
				Messages.getString(MessageIds.GDE_MSGT3119), //6 CV-LINK
				Messages.getString(MessageIds.GDE_MSGT3116), //7 reflex
				Messages.getString(MessageIds.GDE_MSGT3121), //8 CV-Fast
				Messages.getString(MessageIds.GDE_MSGT3117), //9 re-peak
				Messages.getString(MessageIds.GDE_MSGT3118), //10 CV-CC
				Messages.getString(MessageIds.GDE_MSGT3119), //11 CV-LINK
				Messages.getString(MessageIds.GDE_MSGT3120), //12 N-Storage
				Messages.getString(MessageIds.GDE_MSGT3122) };//13 QLagern
		this.DISCHARGE_TYPE = new String[] { 
				Messages.getString(MessageIds.GDE_MSGT3110), //0 NiXx Automatic
				Messages.getString(MessageIds.GDE_MSGT3111), //1 LiXy Automatic
				Messages.getString(MessageIds.GDE_MSGT3112), //2 Normal
				Messages.getString(MessageIds.GDE_MSGT3113), //3 Linear
				Messages.getString(MessageIds.GDE_MSGT3124), //4 LINK
				Messages.getString(MessageIds.GDE_MSGT3118), //5 CV-CC
				Messages.getString(MessageIds.GDE_MSGT3124), //6 LINK
				Messages.getString(MessageIds.GDE_MSGT3120), //7 N-Storage
				Messages.getString(MessageIds.GDE_MSGT3122) };//8 Q-Storage

		this.application = DataExplorer.getInstance();
		this.serialPort = new PolaronSerialPort(this, this.application);
		this.channels = Channels.getInstance();
		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// no device specific mapping required
		return lov2osdMap;
	}

	/**
	 * convert record LogView config data to GDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber
	 * @return converted configuration data
	 */
	@Override
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return GDE.STRING_EMPTY;
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device
	 */
	@Override
	public int getLovDataByteSize() {
		return 132;
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * this method is more usable for real log, where data can be stored and converted in one block
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
		int channel2Offset = deviceDataBufferSize2 - 4;
		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigNumber())];
		int offset = 0;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		int outputChannel = recordSet.getChannelConfigNumber();

		if (dataBuffer[offset] == 0x0C) {
			byte[] convertBuffer = new byte[deviceDataBufferSize];
			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

			for (int i = 0; i < recordDataSize; i++) {
				if (outputChannel == 1)
					System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize2);
				else if (outputChannel == 2) System.arraycopy(dataBuffer, channel2Offset + offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize2);

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
				final int offset2 = 114;
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

				// 19=SpannungZelle1 20=SpannungZelle2 21=SpannungZelle3 22=SpannungZelle4 23=SpannungZelle5 24=SpannungZelle6 25=SpannungZelle7
				for (int i = 0, j = 0; i < 7; ++i, j += 2) {
					points[i + 19] = DataParser.parse2Short(dataBuffer, j + 45);
					if (points[i + 19] > 0) {
						maxVotage = points[i + 19] > maxVotage ? points[i + 19] : maxVotage;
						minVotage = points[i + 19] < minVotage ? points[i + 19] : minVotage;
					}
				}
				// 26=SpannungZelle8 27=SpannungZelle9 28=SpannungZelle10 29=SpannungZelle11 30=SpannungZelle12 31=SpannungZelle13 32=SpannungZelle14
				for (int i = 0, j = 0; i < 7; ++i, j += 2) {
					points[i + 26] = DataParser.parse2Short(dataBuffer, j + 45 + offset2);
					if (points[i + 26] > 0) {
						maxVotage = points[i + 26] > maxVotage ? points[i + 26] : maxVotage;
						minVotage = points[i + 26] < minVotage ? points[i + 26] : minVotage;
					}
				}
				//calculate balance on the fly
				points[18] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

				// 33=BatteryRi
				points[33] = DataParser.parse2Short(dataBuffer, 91) + DataParser.parse2Short(dataBuffer, 91 + offset2);
				// 34=CellRi1 35=CellRi2 36=CellRi3 37=CellRi4 38=CellRi5 39=CellRi6 40=CellRi7
				for (int i = 0, j = 0; i < 7; ++i, j += 2) {
					points[i + 34] = DataParser.parse2Short(dataBuffer, j + 59);
				}
				// 41=CellRi1 42=CellRi2 43=CellRi3 44=CellRi4 45=CellRi5 46=CellRi6 47=CellRi7
				for (int i = 0, j = 0; i < 7; ++i, j += 2) {
					points[i + 41] = DataParser.parse2Short(dataBuffer, j + 59 + offset2);
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

				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7
				for (int i = 0, j = 0; i < 7; ++i, j += 2) {
					points[i + 8] = DataParser.parse2Short(dataBuffer, j + 45);
					if (points[i + 8] > 0) {
						maxVotage = points[i + 8] > maxVotage ? points[i + 8] : maxVotage;
						minVotage = points[i + 8] < minVotage ? points[i + 8] : minVotage;
					}
				}
				//calculate balance on the fly
				points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

				// 15=BatteryRi
				points[15] = DataParser.parse2Short(dataBuffer, 91);
				// 16=CellRi1 17=CellRi2 18=CellRi3 19=CellRi4 20=CellRi5 21=CellRi6 22=CellRi7
				for (int i = 0, j = 0; i < 7; ++i, j += 2) {
					points[i + 16] = DataParser.parse2Short(dataBuffer, j + 59);
				}
			}
			catch (Exception e) {
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

				// 33=BatteryRi
				points[33] = (((convertBuffer[92] & 0xff) << 24) + ((convertBuffer[93] & 0xff) << 16) + ((convertBuffer[94] & 0xff) << 8) + ((convertBuffer[95] & 0xff) << 0));
				// 34=CellRi1 35=CellRi2 36=CellRi3 37=CellRi4 38=CellRi5 39=CellRi6 40=CellRi7
				// 41=CellRi1 42=CellRi2 43=CellRi3 44=CellRi4 45=CellRi5 46=CellRi6 47=CellRi7
				for (int j = 0, k = 0; j < 14; ++j, k += GDE.SIZE_BYTES_INTEGER) {
					points[j + 34] = (((convertBuffer[k + 96] & 0xff) << 24) + ((convertBuffer[k + 97] & 0xff) << 16) + ((convertBuffer[k + 98] & 0xff) << 8) + ((convertBuffer[k + 99] & 0xff) << 0));
				}

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

				// 15=BatteryRi 16=CellRi1 17=CellRi2 18=CellRi3 19=CellRi4 20=CellRi5 21=CellRi6 22=CellRi7
				for (int j = 0, k = 0; j < 8; ++j, k += GDE.SIZE_BYTES_INTEGER) {
					points[j + 15] = (((convertBuffer[k + 48] & 0xff) << 24) + ((convertBuffer[k + 49] & 0xff) << 16) + ((convertBuffer[k + 50] & 0xff) << 8) + ((convertBuffer[k + 51] & 0xff) << 0));
				}

				recordSet.addPoints(points);

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
			}
		}

		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
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
				double factor = record.getFactor(); // != 1 if a unit translation is required
				dataTableRow[index + 1] = record.getDecimalFormat().format(((record.realGet(rowIndex) / 1000.0) * factor));
				++index;
			}
		}
		catch (RuntimeException e) {
			Polaron.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
		// 0=VersorgungsSpg 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=BatteryTemperature 7=Balance
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7
		// 15=BatteryRi 16=CellRi1 17=CellRi2 18=CellRi3 19=CellRi4 20=CellRi5 21=CellRi6 22=CellRi7
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value * factor + offset;
		Polaron.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		// 0=VersorgungsSpg 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=BatteryTemperature 7=Balance
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7
		// 15=BatteryRi 16=CellRi1 17=CellRi2 18=CellRi3 19=CellRi4 20=CellRi5 21=CellRi6 22=CellRi7
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value / factor - offset;
		Polaron.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
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
		for (int i = 0; i < recordSet.size(); i++) {
			Record record = recordSet.get(i);
			record.setDisplayable(record.getOrdinal() <= 5 || record.hasReasonableData());
			if (log.isLoggable(Level.FINER))
				Polaron.log.log(Level.FINER, record.getName() + " setDisplayable=" + (record.getOrdinal() <= 5 || record.hasReasonableData())); //$NON-NLS-1$

			if (record.isActive() && record.isDisplayable()) {
				++displayableCounter;
			}
		}

		if (log.isLoggable(Level.FINE)) {
			for (int i = 0; i < recordSet.size(); i++) {
				Record record = recordSet.get(i);
				log.log(Level.FINE, record.getName() + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		recordSet.setConfiguredDisplayable(displayableCounter);
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {

		// since there are live measurement points only the calculation will take place directly after switch all to displayable
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				// 0=VersorgungsSpg 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=BatteryTemperature 7=Balance
				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7
				// 15=BatteryRi 16=CellRi1 17=CellRi2 18=CellRi3 19=CellRi4 20=CellRi5 21=CellRi6 22=CellRi7
				int displayableCounter = 0;

				// check if measurements isActive == false and set to isDisplayable == false
				for (String measurementKey : recordSet.keySet()) {
					Record record = recordSet.get(measurementKey);

					if (record.isActive() && (record.getOrdinal() <= 5 || record.hasReasonableData())) {
						++displayableCounter;
					}
				}

				Polaron.log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
				recordSet.setConfiguredDisplayable(displayableCounter);

				if (recordSet.getName().equals(this.channels.getActiveChannel().getActiveRecordSet().getName())) {
					this.application.updateGraphicsWindow();
				}
			}
			catch (RuntimeException e) {
				Polaron.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * @return the serialPort
	 */
	@Override
	public PolaronSerialPort getCommunicationPort() {
		return this.serialPort;
	}

	/**
	 * @return the device specific dialog instance
	 */
	@Override
	public DeviceDialog getDialog() {
		return null;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR };
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
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.dataGatherThread = new PolaronGathererThread();
						try {
							if (this.serialPort.isConnected()) {
								this.dataGatherThread.start();
							}
						}
						catch (RuntimeException e) {
							Polaron.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
						catch (Throwable e) {
							Polaron.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
					}
				}
				catch (SerialPortException e) {
					Polaron.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.serialPort.close();
					this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (ApplicationConfigurationException e) {
					Polaron.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.serialPort.close();
					this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (Throwable e) {
					Polaron.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public int[] getCellVoltageOrdinals() {
		// 0=VersorgungsSpg 1=Spannung 2=Strom 3=Ladung 4=Leistung 5=Energie 6=BatteryTemperature 7=Balance
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7
		// 15=BatteryRi 16=CellRi1 17=CellRi2 18=CellRi3 19=CellRi4 20=CellRi5 21=CellRi6 22=CellRi7
		// LINK
		// 0=VersorgungsSpg1 1=Spannung 2=Spannung1 3=Spannung2 4=Strom 5=Strom1 6=Strom2 7=Ladung 8=Ladung1 9=Ladung2 10=Leistung 11=Leistung1 12=Leistung2 13=Energie 14=Energie1 15=Energie2 16=BatteryTemperature1 17=BatteryTemperature2
		// 18=Balance 19=SpannungZelle1 20=SpannungZelle2 21=SpannungZelle3 22=SpannungZelle4 23=SpannungZelle5 24=SpannungZelle6 25=SpannungZelle7
		// 26=SpannungZelle8 27=SpannungZelle9 28=SpannungZelle10 29=SpannungZelle11 30=SpannungZelle12 31=SpannungZelle13 32=SpannungZelle14
		// 33=BatterieRi 34=CellRi1 35=CellRi2 36=CellRi3 37=CellRi4 38=CellRi5 39=CellRi6 40=CellRi7
		// 41=CellRi1 42=CellRi2 43=CellRi3 44=CellRi4 45=CellRi5 46=CellRi6 47=CellRi7
		return new int[] { 1, this.channels.getActiveChannelNumber() == 3 ? 7 : 3 };
	}

	/**
	 * check if one of the outlet channels are in processing mode
	 * @param outletNum 1 or 2
	 * @param dataBuffer
	 * @return true if channel 1 or 2 is active
	 */
	public boolean isProcessing(int outletNum, byte[] dataBuffer) {
		if (outletNum == 1) {
			int processingModeOut1 = getProcessingMode(dataBuffer);
			processingModeOut1 = processingModeOut1 > 9 ? 0 : processingModeOut1;
			if (Polaron.log.isLoggable(java.util.logging.Level.FINE)) {
				Polaron.log.log(java.util.logging.Level.FINE, "processingModeOut1 = " + this.PROCESSING_MODE[processingModeOut1]); //$NON-NLS-1$
			}
			return !(processingModeOut1 == Polaron.OPERATIONS_MODE_NONE || processingModeOut1 == Polaron.OPERATIONS_MODE_ERROR );
		}
		else if (outletNum == 2) {
			int processingModeOut2 = DataParser.parse2Short(dataBuffer, 127);
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
	 * query the processing mode, main modes are charge/discharge, make sure the data buffer contains at index 15,16 the processing modes
	 * @param dataBuffer
	 * @return 0 = no processing, 1 = charge, 2 = discharge, 3 = delay, 4 = pause, 5 = current operation finished, 6 = error, 7 = balancer, 8 = tire heater, 9 = motor
	 */
	public int getProcessingMode(byte[] dataBuffer) {
		return DataParser.parse2Short(dataBuffer, 13);
	}

	/**
	 * query the charge mode, main modes are automatic/normal/CVCC, make sure the data buffer contains at index 15,16 the processing modes, type at index 17,18
	 * @param dataBuffer
	 * @return string of charge mode
	 */
	public String getProcessingType(byte[] dataBuffer) {
		int processingMode = getProcessingMode(dataBuffer);
		int processingType = DataParser.parse2Short(dataBuffer, 15);
		String type = GDE.STRING_EMPTY;
		switch (processingMode) {
		case 1: //charge
			type = this.CHARGE_TYPE[processingType];
			break;
		case 2: //discharge
			type = this.DISCHARGE_TYPE[processingType];
			break;
		case 6: //error
			GDE.getUiNotification().setStatusMessage(Messages.getString(MessageIds.GDE_MSGE3118) + String.format("%02d", processingType), SWT.COLOR_RED); //$NON-NLS-1$
			break;
		}
		if (Polaron.log.isLoggable(java.util.logging.Level.FINE)) {
			//operation: 0=no processing 1=charge 2=discharge 3=delay 4=auto balance 5=error
			Polaron.log.log(java.util.logging.Level.FINE, "processingMode=" + this.PROCESSING_MODE[processingMode] + " processingType=" + type);
		}
		return type;
	}

	/**
	 * query the cycle number of the given outlet channel
	 * @param outletNum
	 * @param dataBuffer
	 * @return
	 */
	public int getCycleNumber(int outletNum, byte[] dataBuffer) {
//		int cycleNumber = DataParser.parse2Short(dataBuffer, 17);
//		if (outletNum == 2) {
//			cycleNumber = DataParser.parse2Short(dataBuffer, 131);
//		}
		return DataParser.parse2Short(dataBuffer, 17);
	}

	/**
	 * query if outlets are linked together to charge identical batteries in parallel
	 * @param dataBuffer
	 * @return true | false
	 */
	public boolean isLinkedMode(byte[] dataBuffer) {
		int processingMode1 = DataParser.parse2Short(dataBuffer, 13);
		int processingType1 = DataParser.parse2Short(dataBuffer, 15);
		int processingMode2 = DataParser.parse2Short(dataBuffer, 127);
		int processingType2 = DataParser.parse2Short(dataBuffer, 129);
		int numCells = DataParser.parse2Short(dataBuffer, 43);
		return (processingMode1 == 7 && processingMode2 == 0 && numCells > 7)
				|| (processingMode1 == processingMode2 && processingType1 == processingType2 && ((processingMode1 == 1 && (processingType1 == 6 || processingType1 == 11)) || (processingMode1 == 2 && (processingType1 == 4 || processingType1 == 6))));
				//																																											charge										CV-LINK                CV-LINK                  discharge							    LINK 											LINK
	}

	/**
	 * set the temperature unit right after creating a record set according to the used oultet channel
	 * @param channelNumber
	 * @param recordSet
	 * @param dataBuffer
	 */
	public void setTemperatureUnit(int channelNumber, RecordSet recordSet, byte[] dataBuffer) {
		int unit = DataParser.parse2Short(dataBuffer, 41);
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
	 * query the device identifier to differentiate between different device implementations
	 * @return 0=unknown, 1=PolaronEx, 2=PolaronAcDcEQ, 3=PolaronAcDc, 4=PolaronPro, 5=PolaronSports
	 */
	public abstract GraupnerDeviceType getDeviceTypeIdentifier();

	/**
	 * query the firmware version
	 * @param dataBuffer
	 * @return 1.337
	 */
	public String getFirmwareVersion(byte[] dataBuffer) {
		return String.format(Locale.ENGLISH, "%.3f", DataParser.parse2Short(dataBuffer, 7) / 1000.0);
	}

	/**
	 * query the product code 0=unknown, 1=PolaronEx, 2=PolaronAcDcEQ, 3=PolaronAcDc, 4=PolaronPro, 5=PolaronSports
	 * @param dataBuffer
	 * @return
	 */
	public int getProductCode(byte[] dataBuffer) {
		//0=unknown, 1=PolaronEx, 2=PolaronAcDcEQ, 3=PolaronAcDc, 4=PolaronPro, 5=PolaronSports
		return DataParser.parse2Short(dataBuffer, 5);
	}

	/**
	 * query the battery memory number of the given outlet channel
	 * @param outletNum
	 * @param dataBuffer
	 * @return
	 */
	public int getBatteryMemoryNumber(int outletNum, byte[] dataBuffer) {
		return -1;
	}

	/**
	 * query device for specific smoothing index
	 * 0 do nothing at all
	 * 1 current drops just a single peak
	 * 2 current drop more or equal than 2 measurements
	 */
	@Override
	public int	getCurrentSmoothIndex() {
		return 2;
	}
}
