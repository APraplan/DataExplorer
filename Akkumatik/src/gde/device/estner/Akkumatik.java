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
package gde.device.estner;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.io.LogViewReader;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * Akkumatik base device class
 * @author Winfried Brügmann
 */
public class Akkumatik extends DeviceConfiguration implements IDevice {
	final static Logger									log								= Logger.getLogger(Akkumatik.class.getName());

	public static String[]				PROCESS_MODE_LI = new String[8];
	public static String[]				PROCESS_MODE_NI = new String[7];
	public static String[]				PROCESS_MODE_PB = new String[7];
	public static String[]				PROCESS_MODE;
	public static String[]				ACCU_TYPES;
	public static String[]				CHARGE_MODE;
	public static String[]				CHARGE_MODE_LI;
	public static String[]				CHARGE_MODE_NI;
	public static String[]				CHARGE_MODE_PB;
	public static String[]				PROCESS_MODE_CH2;

	protected final DataExplorer				application;
	protected final AkkumatikDialog			dialog;
	protected final AkkumatikSerialPort	serialPort;
	protected final Channels						channels;
	protected GathererThread						dataGatherThread	= null;
	protected final Settings 						settings = Settings.getInstance();

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public Akkumatik(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.estner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		PROCESS_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT3400), Messages.getString(MessageIds.GDE_MSGT3401), Messages.getString(MessageIds.GDE_MSGT3402),
				Messages.getString(MessageIds.GDE_MSGT3403), Messages.getString(MessageIds.GDE_MSGT3404), Messages.getString(MessageIds.GDE_MSGT3405), Messages.getString(MessageIds.GDE_MSGT3406), Messages.getString(MessageIds.GDE_MSGT3407) };
		ACCU_TYPES = new String[] { Messages.getString(MessageIds.GDE_MSGT3430), Messages.getString(MessageIds.GDE_MSGT3431), Messages.getString(MessageIds.GDE_MSGT3432),
				Messages.getString(MessageIds.GDE_MSGT3433), Messages.getString(MessageIds.GDE_MSGT3434), Messages.getString(MessageIds.GDE_MSGT3435), Messages.getString(MessageIds.GDE_MSGT3436),
				Messages.getString(MessageIds.GDE_MSGT3437) };
		CHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT3410), Messages.getString(MessageIds.GDE_MSGT3411), Messages.getString(MessageIds.GDE_MSGT3412),
				Messages.getString(MessageIds.GDE_MSGT3413) };

		System.arraycopy(PROCESS_MODE, 0, PROCESS_MODE_LI, 0, PROCESS_MODE.length);
		System.arraycopy(PROCESS_MODE, 0, PROCESS_MODE_NI, 0, PROCESS_MODE.length - 1);
		System.arraycopy(PROCESS_MODE, 0, PROCESS_MODE_PB, 0, PROCESS_MODE.length - 1);
		PROCESS_MODE_CH2 = new String[] { PROCESS_MODE[0] };
		
		CHARGE_MODE_LI = new String[] { Messages.getString(MessageIds.GDE_MSGT3410), Messages.getString(MessageIds.GDE_MSGT3413) };
		CHARGE_MODE_NI = new String[] { Messages.getString(MessageIds.GDE_MSGT3410), Messages.getString(MessageIds.GDE_MSGT3411), Messages.getString(MessageIds.GDE_MSGT3412) };
		CHARGE_MODE_PB = new String[] { Messages.getString(MessageIds.GDE_MSGT3410) };

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.serialPort = new AkkumatikSerialPort(this, this.application);
		this.dialog = new AkkumatikDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Akkumatik(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.estner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		PROCESS_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT3400), Messages.getString(MessageIds.GDE_MSGT3401), Messages.getString(MessageIds.GDE_MSGT3402),
				Messages.getString(MessageIds.GDE_MSGT3403), Messages.getString(MessageIds.GDE_MSGT3404), Messages.getString(MessageIds.GDE_MSGT3405), Messages.getString(MessageIds.GDE_MSGT3406), Messages.getString(MessageIds.GDE_MSGT3407) };
		ACCU_TYPES = new String[] { Messages.getString(MessageIds.GDE_MSGT3430), Messages.getString(MessageIds.GDE_MSGT3431), Messages.getString(MessageIds.GDE_MSGT3432),
				Messages.getString(MessageIds.GDE_MSGT3433), Messages.getString(MessageIds.GDE_MSGT3434), Messages.getString(MessageIds.GDE_MSGT3435), Messages.getString(MessageIds.GDE_MSGT3436),
				Messages.getString(MessageIds.GDE_MSGT3437) };
		CHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT3410), Messages.getString(MessageIds.GDE_MSGT3411), Messages.getString(MessageIds.GDE_MSGT3412),
				Messages.getString(MessageIds.GDE_MSGT3413) };

		System.arraycopy(PROCESS_MODE, 0, PROCESS_MODE_LI, 0, PROCESS_MODE.length);
		System.arraycopy(PROCESS_MODE, 0, PROCESS_MODE_NI, 0, PROCESS_MODE.length - 1);
		System.arraycopy(PROCESS_MODE, 0, PROCESS_MODE_PB, 0, PROCESS_MODE.length - 1);
		PROCESS_MODE_CH2 = new String[] { PROCESS_MODE[0] };
		
		CHARGE_MODE_LI = new String[] { Messages.getString(MessageIds.GDE_MSGT3410), Messages.getString(MessageIds.GDE_MSGT3413) };
		CHARGE_MODE_NI = new String[] { Messages.getString(MessageIds.GDE_MSGT3410), Messages.getString(MessageIds.GDE_MSGT3411), Messages.getString(MessageIds.GDE_MSGT3412) };
		CHARGE_MODE_PB = new String[] { Messages.getString(MessageIds.GDE_MSGT3410) };

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.serialPort = new AkkumatikSerialPort(this, this.application);
		this.dialog = new AkkumatikDialog(this.application.getShell(), this);
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
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device
	 */
	@Override
	public int getLovDataByteSize() {
		return 81;
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
		int deviceDataBufferSize = Math.abs(this.getDataBlockSize(InputTypes.SERIAL_IO));
		int[] points = new int[this.getNumberOfMeasurements(1)];
		int offset = 0;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		long startCycleTime = 0, timeStep = 0, lastCycleTime = -1;

		// 0=Voltage 4=Current 2=Capacity 3=Power 4=Energy 5=SupplyVoltage 6=Resistance 7=Temperature 8=TemperatureInt 9=Balance
		// 10=CellVoltage1 11=CellVoltage2 12=CellVoltage3 13=CellVoltage4 14=CellVoltage5 15=CellVoltage6
		// 16=CellVoltage7 17=CellVoltage8 18=CellVoltage9 19=CellVoltage10 20=CellVoltage11 21=CellVoltage12
		// none constant time steps
		byte[] sizeBuffer = new byte[4];
		byte[] convertBuffer = new byte[deviceDataBufferSize];

		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		for (int i = 0; i < recordDataSize-1; i++) {
			System.arraycopy(dataBuffer, offset, sizeBuffer, 0, sizeBuffer.length);
			lovDataSize = 4 + LogViewReader.parse2Int(sizeBuffer);
			System.arraycopy(dataBuffer, offset + 4, convertBuffer, 0, lovDataSize);
			if (log.isLoggable(java.util.logging.Level.FINE)) {
				log.log(java.util.logging.Level.FINE, "  Read : " + StringHelper.byte2Hex2CharString(convertBuffer, convertBuffer.length));
				log.log(java.util.logging.Level.FINE, "0123456789|123456789|123456789|123456789|123456789|123456789|123456789|123456789");
				log.log(java.util.logging.Level.FINE, new String(convertBuffer));
			}
			String [] data = new String(convertBuffer).split(new String(new byte[] {(byte) 0xFF}));
			if (startCycleTime == 0) startCycleTime = this.getProcessingTime(data);
			timeStep = this.getProcessingTime(data) - startCycleTime;
			if (lastCycleTime < timeStep) {
				recordSet.addPoints(convertDataBytes(points, data), timeStep);
				lastCycleTime = timeStep;
			}
			offset += lovDataSize;

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}

		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer string array with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, String[] dataBuffer) {
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;

		// 0=Voltage 4=Current 2=Capacity 3=Power 4=Energy 5=SupplyVoltage 6=Resistance 7=Temperature 8=TemperatureInt 9=Balance
		points[0] = Integer.valueOf(dataBuffer[2]);
		points[1] = Integer.valueOf(dataBuffer[3].replace(GDE.STRING_PLUS, GDE.STRING_EMPTY));
		points[2] = Integer.valueOf(dataBuffer[4].replace(GDE.STRING_PLUS, GDE.STRING_EMPTY)) * 1000;
		points[3] = Double.valueOf(points[0] * points[1] / 1000.0).intValue(); // power U*I [W]
		points[4] = Double.valueOf(points[0] / 1000.0 * points[2] / 1000.0).intValue(); // energy U*C [mWh]
		points[5] = Integer.valueOf(dataBuffer[5]);
		points[6] = Integer.valueOf(dataBuffer[6]) * 1000;
		points[7] = Integer.valueOf(dataBuffer[7]) * 1000;
		points[8] = Integer.valueOf(dataBuffer[17]) * 1000;
		points[9] = 0;

		//System.out.println("dataBuffer.length = " + dataBuffer.length);
		if ( this.getAccuCellType(dataBuffer) > 3) { //Akkutyp (0=NICD, 1=NIMH, 2=BLEI, 3=BGEL, 4=LIIO, 5=LIPO, 6=LiFe, 7=IUxx)
			final int numCells = this.getNumberOfLithiumCells(dataBuffer);
			if (numCells > 1 && dataBuffer.length >= (19 + numCells)) {
				// 10=CellVoltage1 11=CellVoltage2 12=CellVoltage3 13=CellVoltage4 14=CellVoltage5 15=CellVoltage6 16=CellVoltage7 17=CellVoltage8
				for (int i = 0; i < numCells; ++i) {
					points[i + 10] = Integer.valueOf(dataBuffer[i + 18]);
					if (points[i + 10] > 0) {
						maxVotage = points[i + 10] > maxVotage ? points[i + 10] : maxVotage;
						minVotage = points[i + 10] < minVotage ? points[i + 10] : minVotage;
					}
				}
				//calculate balance on the fly
				points[9] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 1000;
			}
		}
		return points;
	}

	/**
	 * query if the PowerLab8 executes discharge > charge > discharge cycles
	 * Program (0= LADE, 1= ENTL, 2= E+L, 3= L+E, 4= (L)E+L, 5= (E)L+E, 6= SENDER
	 */
	boolean isCycleMode(String[] dataBuffer) {
		return this.getProcessingMode(dataBuffer) >= 2 && this.getProcessingMode(dataBuffer) <= 5;
	}

	/**
	 * getNumberOfCycle for NiCd and NiMh, for LiXx it  will return 0
	 * accuCellType -> Lithium=1, NiMH=2, NiCd=3, Pb=4
	 * @param dataBuffer
	 * @return cycle count
	 */
	public int getNumberOfCycle(String[] dataBuffer) {
		return Integer.valueOf(dataBuffer[10]); //dataBuffer[50] - 48;
	}

	/**
	 * query the number of Lithium cells if any (after processing is stopped a status word replace cell count) 
	 * @param specificData
	 * @return cell count if any
	 */
	public int getNumberOfLithiumCells(String[] dataBuffer) {
		return Integer.valueOf(dataBuffer[8]); 
	}

	/**
	 * query the number of Lithium cells if any (after processing is stopped a status word replace cell count) 
	 * @param specificData
	 * @return Status number
	 */
	public int getNumberStatus(String[] dataBuffer) {
		return Integer.valueOf(dataBuffer[8]); 
	}

	/**
	 * get battery cell type
	 * @param dataBuffer
	 * @return Akkutype (0=NICD, 1=NIMH, 2=BLEI, 3=BGEL, 4=LIIO, 5=LIPO, 6=LiFe, 7=IUxx)
	 */
	public int getAccuCellType(String[] dataBuffer) {
		return Integer.valueOf(dataBuffer[12]);// dataBuffer[54] - 48;
	}

	/**
	 * check if processing
	 * Ladephase (0=stop, 1...n, siehe Bedienungsanleitung)
	 * @param dataBuffer
	 * @return
	 */
	public boolean isProcessing(String[] dataBuffer) {
		if (this.settings.isReduceChargeDischarge())
			return this.getProcessingPhase(dataBuffer) != 0 && this.getProcessingPhase(dataBuffer) != 10;
		return this.getProcessingPhase(dataBuffer) != 0;
	}
	
	/**
	 * get processing mode
	 * (0= LADE, 1= ENTL, 2= E+L, 3= L+E, 4= (L)E+L, 5= (E)L+E, 6= SENDER, LAGERN wird mit 0 oder 1 gemeldet)
	 * @param dataBuffer
	 * @return
	 */
	public int getProcessingMode(String[] dataBuffer) {
		return Integer.valueOf(dataBuffer[13]); //dataBuffer[56] - 48;
	}

	/**
	 * get processing phase
	 * 0=Stop; 10=Pause
	 * @param dataBuffer
	 * @return
	 */
	public int getProcessingPhase(String[] dataBuffer) {
		return Integer.valueOf(dataBuffer[9]); //(dataBuffer[47] - 48) * 10 + (dataBuffer[48] - 48);
	}

	/**
	 * get processing type
	 * Ladeart (0=Normal, 1=Puls, 2=Reflex, 3=Fast)
	 * @param dataBuffer
	 * @return
	 */
	public int getProcessingType(String[] dataBuffer) {
		return Integer.valueOf(dataBuffer[14]); //dataBuffer[58] - 48;
	}

	/**
	 * get processing time
	 * (00:00:11)
	 * @param dataBuffer
	 * @return
	 */
	public long getProcessingTime(String[] dataBuffer) {
		final String[] time = dataBuffer[1].split(":");
		return (Integer.valueOf(time[0]) * 3600 + Integer.valueOf(time[1]) * 60 + Integer.valueOf(time[2])) * 1000;
		//(((dataBuffer[2] - 48) * 10 + (dataBuffer[3] - 48)) * 3600 + ((dataBuffer[5] - 48) * 10 + (dataBuffer[6] - 48)) * 60 + ((dataBuffer[8] - 48) * 10 + (dataBuffer[9] - 48))) * 1000;
	}

	/**
	 * get processing time
	 * (00:00:11)
	 * @param dataBuffer
	 * @return
	 */
	public long getProcessingTime(byte[] dataBuffer) {
		return (((dataBuffer[2] - 48) * 10 + (dataBuffer[3] - 48)) * 3600 + ((dataBuffer[5] - 48) * 10 + (dataBuffer[6] - 48)) * 60 + ((dataBuffer[8] - 48) * 10 + (dataBuffer[9] - 48))) * 1000;
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
		Vector<Integer> timeStamps = new Vector<Integer>(1, 1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		// 0=Voltage 4=Current 2=Capacity 3=Power 4=Energy 5=SupplyVoltage 6=Resistance 7=Temperature 8=TemperatureInt 9=Balance
		// 10=CellVoltage1 11=CellVoltage2 12=CellVoltage3 13=CellVoltage4 14=CellVoltage5 15=CellVoltage6
		// 16=CellVoltage7 17=CellVoltage8 18=CellVoltage9 19=CellVoltage10 20=CellVoltage11 21=CellVoltage12

		int timeStampBufferSize = 0;
		if (!recordSet.isTimeStepConstant()) {
			timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
			byte[] timeStampBuffer = new byte[timeStampBufferSize];
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8)
						+ ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
			}
		}
		Akkumatik.log.log(java.util.logging.Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString());

		for (int i = 0; i < recordDataSize; i++) {
			Akkumatik.log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize);
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			// 0=Voltage 4=Current 2=Capacity 3=Power 4=Energy 5=SupplyVoltage 6=Resistance 7=Temperature 8=TemperatureInt 9=Balance
			// 10=CellVoltage1 11=CellVoltage2 12=CellVoltage3 13=CellVoltage4 14=CellVoltage5 15=CellVoltage6
			// 16=CellVoltage7 17=CellVoltage8 18=CellVoltage9 19=CellVoltage10 20=CellVoltage11 21=CellVoltage12
			points[0] = (((convertBuffer[0] & 0xff) << 24) + ((convertBuffer[1] & 0xff) << 16) + ((convertBuffer[2] & 0xff) << 8) + ((convertBuffer[3] & 0xff) << 0));
			points[1] = (((convertBuffer[4] & 0xff) << 24) + ((convertBuffer[5] & 0xff) << 16) + ((convertBuffer[6] & 0xff) << 8) + ((convertBuffer[7] & 0xff) << 0));
			points[2] = (((convertBuffer[8] & 0xff) << 24) + ((convertBuffer[9] & 0xff) << 16) + ((convertBuffer[10] & 0xff) << 8) + ((convertBuffer[11] & 0xff) << 0));
			points[3] = Double.valueOf((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); // power U*I [W]
			points[4] = Double.valueOf((points[0] / 1000.0) * (points[2] / 1000.0)).intValue(); // energy U*C [mWh]
			points[5] = (((convertBuffer[12] & 0xff) << 24) + ((convertBuffer[13] & 0xff) << 16) + ((convertBuffer[14] & 0xff) << 8) + ((convertBuffer[15] & 0xff) << 0));
			points[6] = (((convertBuffer[16] & 0xff) << 24) + ((convertBuffer[17] & 0xff) << 16) + ((convertBuffer[18] & 0xff) << 8) + ((convertBuffer[19] & 0xff) << 0));
			points[7] = (((convertBuffer[20] & 0xff) << 24) + ((convertBuffer[21] & 0xff) << 16) + ((convertBuffer[22] & 0xff) << 8) + ((convertBuffer[23] & 0xff) << 0));
			points[8] = (((convertBuffer[24] & 0xff) << 24) + ((convertBuffer[25] & 0xff) << 16) + ((convertBuffer[26] & 0xff) << 8) + ((convertBuffer[27] & 0xff) << 0));
			points[9] = 0;
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;

			// 10=CellVoltage1 11=CellVoltage2 12=CellVoltage3 13=CellVoltage4 14=CellVoltage5 15=CellVoltage6
			// 16=CellVoltage7 17=CellVoltage8 18=CellVoltage9 19=CellVoltage10 20=CellVoltage11 21=CellVoltage12
			for (int j = 0, k = 0; j < 12; ++j, k += GDE.SIZE_BYTES_INTEGER) {
				points[j + 10] = (((convertBuffer[k + 28] & 0xff) << 24) + ((convertBuffer[k + 29] & 0xff) << 16) + ((convertBuffer[k + 30] & 0xff) << 8) + ((convertBuffer[k + 31] & 0xff) << 0));
				if (points[j + 10] > 0) {
					maxVotage = points[j + 10] > maxVotage ? points[j + 10] : maxVotage;
					minVotage = points[j + 10] < minVotage ? points[j + 10] : minVotage;
				}
			}
			//calculate balance on the fly
			points[9] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 1000;

			if (recordSet.isTimeStepConstant())
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		// 0=Voltage 4=Current 2=Capacity 3=Power 4=Energy 5=SupplyVoltage 6=Resistance 7=Temperature 8=TemperatureInt 9=Balance
		// 10=CellVoltage1 11=CellVoltage2 12=CellVoltage3 13=CellVoltage4 14=CellVoltage5 15=CellVoltage6
		// 16=CellVoltage7 17=CellVoltage8 18=CellVoltage9 19=CellVoltage10 20=CellVoltage11 21=CellVoltage12
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				double factor = record.getFactor(); // != 1 if a unit translation is required
				if (record.getOrdinal() > 8 && record.getUnit().equals("V")) //cell voltage
					dataTableRow[index + 1] = String.format("%.3f", ((record.realGet(rowIndex) / 1000.0) * factor));
				else
					dataTableRow[index + 1] = record.getDecimalFormat().format(((record.realGet(rowIndex) / 1000.0) * factor));
				++index;
			}
		}
		catch (RuntimeException e) {
			Akkumatik.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
		// 0=Voltage 4=Current 2=Capacity 3=Power 4=Energy 5=SupplyVoltage 6=Resistance 7=Temperature 8=TemperatureInt 9=Balance
		// 10=CellVoltage1 11=CellVoltage2 12=CellVoltage3 13=CellVoltage4 14=CellVoltage5 15=CellVoltage6
		// 16=CellVoltage7 17=CellVoltage8 18=CellVoltage9 19=CellVoltage10 20=CellVoltage11 21=CellVoltage12
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value * factor + offset;
		Akkumatik.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		// 0=Voltage 4=Current 2=Capacity 3=Power 4=Energy 5=SupplyVoltage 6=Resistance 7=Temperature 8=TemperatureInt 9=Balance
		// 10=CellVoltage1 11=CellVoltage2 12=CellVoltage3 13=CellVoltage4 14=CellVoltage5 15=CellVoltage6
		// 16=CellVoltage7 17=CellVoltage8 18=CellVoltage9 19=CellVoltage10 20=CellVoltage11 21=CellVoltage12
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value / factor - offset;
		Akkumatik.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		// 0=Voltage 4=Current 2=Capacity 3=Power 4=Energy 5=SupplyVoltage 6=Resistance 7=Temperature 8=TemperatureInt 9=Balance
		// 10=CellVoltage1 11=CellVoltage2 12=CellVoltage3 13=CellVoltage4 14=CellVoltage5 15=CellVoltage6
		// 16=CellVoltage7 17=CellVoltage8 18=CellVoltage9 19=CellVoltage10 20=CellVoltage11 21=CellVoltage12
		recordSet.setAllDisplayable();
		for (String recordKey : recordSet.getNoneCalculationRecordNames()) {
			recordSet.get(recordKey).setActive(true);
		}
		for (int i = 0; i < recordSet.size(); ++i) {
			Record record = recordSet.get(i);
			record.setDisplayable(record.hasReasonableData());
			if (Akkumatik.log.isLoggable(java.util.logging.Level.FINE)) Akkumatik.log.log(java.util.logging.Level.FINE, record.getName() + " setDisplayable=" + record.hasReasonableData());

			if (record.isActive() && record.isDisplayable()) {
				++displayableCounter;
			}
		}

		if (Akkumatik.log.isLoggable(java.util.logging.Level.FINE)) {
			for (int i = 0; i < recordSet.size(); i++) {
				Record record = recordSet.get(i);
				Akkumatik.log.log(java.util.logging.Level.FINE, record.getName() + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable());
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
				// 0=Voltage 4=Current 2=Capacity 3=Power 4=Energy 5=SupplyVoltage 6=Resistance 7=Temperature 8=TemperatureInt 9=Balance
				// 10=CellVoltage1 11=CellVoltage2 12=CellVoltage3 13=CellVoltage4 14=CellVoltage5 15=CellVoltage6
				// 16=CellVoltage7 17=CellVoltage8 18=CellVoltage9 19=CellVoltage10 20=CellVoltage11 21=CellVoltage12
				int displayableCounter = 0;

				// check if measurements isActive == false and set to isDisplayable == false
				for (String measurementKey : recordSet.keySet()) {
					Record record = recordSet.get(measurementKey);

					if (record.isActive() && (record.getOrdinal() <= 6 || record.hasReasonableData())) {
						++displayableCounter;
					}
				}

				//3=Leistung
				++displayableCounter;
				//4=Energie
				++displayableCounter;

				Akkumatik.log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
				recordSet.setConfiguredDisplayable(displayableCounter);

				if (recordSet.getName().equals(this.channels.getActiveChannel().getActiveRecordSet().getName())) {
					this.application.updateGraphicsWindow();
				}
			}
			catch (RuntimeException e) {
				Akkumatik.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * @return the serialPort
	 */
	@Override
	public AkkumatikSerialPort getCommunicationPort() {
		return this.serialPort;
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
	 * @return the dialog
	 */
	@Override
	public DeviceDialog getDialog() {
		return this.dialog;
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	@Override
	public void open_closeCommPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.dataGatherThread = new GathererThread(this.application, this, this.serialPort, activChannel.getNumber());
						try {
							if (this.serialPort.isConnected()) {
								this.dataGatherThread.start();
							}
						}
						catch (RuntimeException e) {
							Akkumatik.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
						catch (Throwable e) {
							Akkumatik.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
					}
				}
				catch (SerialPortException e) {
					Akkumatik.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
				}
				catch (ApplicationConfigurationException e) {
					Akkumatik.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (Throwable e) {
					Akkumatik.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
				}
			}
			else {
				if (this.dataGatherThread != null) {
					try {
						this.dataGatherThread.stopDataGatheringThread(false, null);
						this.dataGatherThread = null;
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
		// 0=Voltage 4=Current 2=Capacity 3=Power 4=Energy 5=SupplyVoltage 6=Resistance 7=Temperature 8=TemperatureInt 9=Balance
		// 10=CellVoltage1 11=CellVoltage2 12=CellVoltage3 13=CellVoltage4 14=CellVoltage5 15=CellVoltage6
		// 16=CellVoltage7 17=CellVoltage8 18=CellVoltage9 19=CellVoltage10 20=CellVoltage11 21=CellVoltage12
		return new int[] { 0, 2 };
	}

	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		return null;
	}

	/**
	 * query device for specific smoothing index
	 * 0 do nothing at all
	 * 1 current drops just a single peak
	 * 2 current drop more or equal than 2 measurements
	 */
	@Override
	public int	getCurrentSmoothIndex() {
		return 1;
	}
	
	/**
	 * @return reference to gatherer thread
	 */
	public GathererThread getDataGathererThread() {
		return this.dataGatherThread;
	}
}
