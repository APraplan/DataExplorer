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
    
    Copyright (c) 2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.junsi;

import java.io.FileNotFoundException;
import java.util.Vector;

import javax.usb.UsbClaimException;
import javax.usb.UsbException;
import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DataBlockType;
import gde.device.DeviceConfiguration;
import gde.device.FormatTypes;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.device.MeasurementType;
import gde.device.junsi.modbus.ChargerDialog;
import gde.device.junsi.modbus.ChargerUsbPort;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.io.CSVSerialDataReaderWriter;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.FileUtils;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

/**
 * iCharger base class for USB communicating iCharger devices
 */
public abstract class iChargerUsb extends iCharger implements IDevice {

	protected final ChargerUsbPort	usbPort;
	protected boolean								isFileIO		= false;
	protected boolean								isSerialIO	= false;
	protected UsbGathererThread			usbGathererThread;
	protected ChargerDialog					dialog;

	protected String 								batteryType_1 = GDE.STRING_QUESTION_MARK;
	protected String 								batteryType_2 = GDE.STRING_QUESTION_MARK;

	protected static int lastTime1_ms = 0;
	protected static int lastTime2_ms = 0;
	protected static double energySum1 = 0.0;
	protected static double energySum2 = 0.0;

		
	public enum BatteryTypesDuo {
		BT_UNKNOWN("?"), BT_LIPO("LiPo"), BT_LIIO("LiIo"), BT_LIFE("LiFe"), BT_NIMH("NiMH"), BT_NICD("NiCd"), BT_PB("PB"), BT_NIZN("NiZn"), BT_LIHV("LiHV"), BT_UNKNOWN_("?");

		private String value;
		
		private BatteryTypesDuo(String newValue) {
			value = newValue;
		}
		
		protected String getName() {
			return value;
		}
		
		public static BatteryTypesDuo[] VALUES = values();
		
		public static String[] getValues() {
			StringBuilder sb = new StringBuilder();
			for (BatteryTypesDuo bt : BatteryTypesDuo.values()) 
				sb.append(bt.value).append(GDE.CHAR_CSV_SEPARATOR);
			return sb.toString().split(GDE.STRING_CSV_SEPARATOR);
		}
	};
	
	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public iChargerUsb(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.junsi.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.BATTERIE_TYPES = BatteryTypesDuo.getValues(); 
 
		if (this.application.getMenuToolBar() != null) {
			for (DataBlockType.Format format : this.getDataBlockType().getFormat()) {
				if (!isSerialIO) isSerialIO = format.getInputType() == InputTypes.SERIAL_IO;
				if (!isFileIO) isFileIO = format.getInputType() == InputTypes.FILE_IO;
			}
			if (isSerialIO) { //InputTypes.SERIAL_IO has higher relevance  
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT2606), Messages.getString(MessageIds.GDE_MSGT2605));
			} else { //InputTypes.FILE_IO
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2603), Messages.getString(MessageIds.GDE_MSGT2603));
			}
			if (isFileIO)
				updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
		
		this.usbPort = new ChargerUsbPort(this, this.application);
		this.dialog = new ChargerDialog(this.application.getShell(), this);
	}

	/**
	 * @param deviceConfig
	 */
	public iChargerUsb(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.junsi.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.BATTERIE_TYPES = BatteryTypesDuo.getValues(); 
 
		if (this.application.getMenuToolBar() != null) {
			for (DataBlockType.Format format : this.getDataBlockType().getFormat()) {
				if (!isSerialIO) isSerialIO = format.getInputType() == InputTypes.SERIAL_IO;
				if (!isFileIO) isFileIO = format.getInputType() == InputTypes.FILE_IO;
			}
			if (isSerialIO) { //InputTypes.SERIAL_IO has higher relevance  
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT2606), Messages.getString(MessageIds.GDE_MSGT2605));
			} else { //InputTypes.FILE_IO
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2603), Messages.getString(MessageIds.GDE_MSGT2603));
			}
			if (isFileIO)
				updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
		
		this.usbPort = new ChargerUsbPort(this, this.application);
		this.dialog = new ChargerDialog(this.application.getShell(), this);
	}

	public ChargerUsbPort getUsbPort() { return this.usbPort; };
	
	/**
	 * @return the device specific dialog instance
	 */
	@Override
	public ChargerDialog getDialog() {
		return this.dialog;
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	@Override
	public int getLovDataByteSize() {
		return 64+8+5;  
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
		byte[] convertBuffer = new byte[deviceDataBufferSize];

		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			//prepare convert buffer for conversion
			System.arraycopy(dataBuffer, offset+5, convertBuffer, 0, deviceDataBufferSize);
			int actualTime_ms = DataParser.parse2Int(convertBuffer, 3);
			recordSet.addPoints(convertDataBytes(points, convertBuffer), actualTime_ms);
			offset += lovDataSize;

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}

		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}
	
	/**
	 * update the file import menu by adding new entry to import device specific files
	 * @param importMenue
	 */
	public void updateFileImportMenu(Menu importMenue) {
		MenuItem importDeviceLogItem;

		if (importMenue.getItem(importMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {			
			new MenuItem(importMenue, SWT.SEPARATOR);

			importDeviceLogItem = new MenuItem(importMenue, SWT.PUSH);
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT2604, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT2604));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					if (!isSerialIO) open_closeCommPort();
					else importDeviceData();
				}
			});
		}
	}
	
	public boolean isDataGathererActive() {
		return this.usbGathererThread != null && this.usbGathererThread.isAlive();
	}
	
	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	@Override	
	public void open_closeCommPort() {
		switch (application.getMenuBar().getSerialPortIconSet()) {
		case DeviceCommPort.ICON_SET_IMPORT_CLOSE:
			importDeviceData();
			break;
			
		case DeviceCommPort.ICON_SET_START_STOP:
			if (this.usbPort != null) {
				if (!this.usbPort.isConnected() || !this.isDataGathererActive()) { 
					try {
						Channel activChannel = Channels.getInstance().getActiveChannel();
						if (activChannel != null) {
							this.usbGathererThread = new UsbGathererThread(this.application, this, this.usbPort, activChannel.getNumber());
							try {
								if (this.usbGathererThread != null && this.usbPort.isConnected()) {
									WaitTimer.delay(100);
									this.usbGathererThread.start();
								}
							}
							catch (Throwable e) {
								log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
							}
						}
					}
					catch (UsbClaimException e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0051, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
						try {
							if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
						}
						catch (UsbException ex) {
							log.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
						}
					}
					catch (UsbException e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						this.application.openMessageDialog(e.getMessage());
						try {
							if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
						}
						catch (UsbException ex) {
							log.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
						}
					}
					catch (ApplicationConfigurationException e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
						this.application.getDeviceSelectionDialog().open();
					}
					catch (Throwable e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					}
				}
				else {
					if (this.usbGathererThread != null) {
						this.usbGathererThread.stopDataGatheringThread(false, null);
						//this.usbGathererThread = null;
					}
					try {
						WaitTimer.delay(1000);
						if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(false);
					}
					catch (UsbException e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					}
				}
			}
			break;
		}
	}

	/**
	 * import device specific *.bin data files
	 */
	public void importDeviceData() {
		if (!this.application.getDeviceSelectionDialog().checkDataSaved()) 
			return;

		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT2600));

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								CSVSerialDataReaderWriter.read(selectedImportFile, iChargerUsb.this, GDE.STRING_EMPTY, 1, 
										new  DataParserDuo(1, getDataBlockLeader(), getDataBlockSeparator().value(), null, null, 
												 getNoneCalculationMeasurementNames(1, getMeasurementNames(1)).length, 
												 getDataBlockFormat(InputTypes.FILE_IO), false, 2));
							}
							catch (Throwable e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * query the batteries type BATTERY_TYPE 1=LiPo 2=LiIo 3=LiFe 4=NiMH 5=NiCd 6=Pb 7=NiZn
	 * @param databuffer
	 * @return
	 * @throws DataInconsitsentException 
	 */
	public String getBatteryType(final byte[] databuffer) throws DataInconsitsentException {
		try {
			return this.BATTERIE_TYPES[databuffer[8]];
		}
		catch (Exception e) {
			throw new DataInconsitsentException("could not detect battery type !");
		}
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
		int actualTime_ms = DataParser.parse2Int(dataBuffer, 3);
		double timeStep_h = 1.0 / 3600.0; //default time step
		if (dataBuffer[2] == 0x01) { //channel 1
			if (actualTime_ms < lastTime1_ms)
				lastTime1_ms = 0;
			timeStep_h = (actualTime_ms - lastTime1_ms) / 1000.0 / 3600.0;
			lastTime1_ms = actualTime_ms;
		} else { //channel 2
			if (actualTime_ms < lastTime2_ms)
				lastTime2_ms = 0;
			timeStep_h = (actualTime_ms - lastTime2_ms) / 1000.0 / 3600.0;
			lastTime2_ms = actualTime_ms;
		}

		//2C 10 01 F8 7D 07 00 02 01 00 E5 FF 04 87 2B 3C CF FF FF FF 64 01 00 00 02 0F 0B 0F 0E 0F 00 0F 00 00 00 00 00 00 00 00 00 00 00
	  //                                    34564 15403             35,6        3842  3851  3854  3840
		switch (dataBuffer[7]) {
		default: //LOG_NORMAL = 0, LOG_CHARGE = 1, LOG_DISCHARGE = 2, LOG_MONITOR = 3, LOG_WASTE = 4, LOG_PRECHARGE = 5, LOG_OVER = 6,
			//LOG_ERR = 7, LOG_TRICKLE = 8, LOG_ONLYBAL = 9, LOG_INFO = 11, LOG_DIGITPOWER = 30,
			//byte[0] length; byte[1] type, byte[2] channel, byte[3..6]ushort timeStamp, byte[7] logState, byte[8] battType, byte[9] cycleCount
			//short current, ushort Vin, ushort Vbat, int capacity, short tempInt, short tempExt, int Vcell1, int Vcell2, ....
			switch (dataBuffer[2]) {
			default:
			case 1: //channel 1
				try {
					batteryType_1 = this.getBatteryType(dataBuffer);
				}
				catch (DataInconsitsentException e) {
					batteryType_1 = GDE.STRING_QUESTION_MARK;
				}
				break;
			case 2: //channel 2
				try {
					batteryType_2 = this.getBatteryType(dataBuffer);
				}
				catch (DataInconsitsentException e) {
					batteryType_2 = GDE.STRING_QUESTION_MARK;
				}
				break;
			}
			//0=Current 1=SupplyVoltage. 2=Voltage
			points[0] = DataParser.parse2Short(dataBuffer, 10);
			points[1] = DataParser.parse2UnsignedShort(dataBuffer, 12);
			points[2] = DataParser.parse2UnsignedShort(dataBuffer, 14);
			//3=Capacity 4=Power 5=Energy
			points[3] = DataParser.parse2Int(dataBuffer, 16);
			points[4] = Math.abs(points[0] * points[2] / 100); 	// power U*I [W]
			if (dataBuffer[2] == 0x01) { //channel 1
				if (points[3] != 0) {
					energySum1 += points[4] * timeStep_h; //energy = energy + (timeDelta * power)
					points[5] = Double.valueOf(energySum1).intValue();
				} else {
					points[5] = 0; 		
					energySum1 = 0.0;
				}
			} else { //channel 2
				if (points[3] != 0) {
					energySum2 += points[4] * timeStep_h; //energy = energy + (timeDelta * power)
					points[5] = Double.valueOf(energySum2).intValue();
				} else {
					points[5] = 0; 		
					energySum2 = 0.0;
				}
			}
			//6=Temp.intern 7=Temp.extern
			points[6] = DataParser.parse2Short(dataBuffer, 20);
			points[7] = DataParser.parse2Short(dataBuffer, 22);
			//9=VoltageCell1 10=VoltageCell2 11=VoltageCell3 12=VoltageCell4 13=VoltageCell5 14=VoltageCell6 ... VoltageCell[numberOfLithiumCells]
			for (int i = 0,j=0; i < this.getNumberOfLithiumCells(); i++,j+=2) {
				points[i+9] = DataParser.parse2Short(dataBuffer, 24+j);
				if (points[i + 9] > 0) {
					maxVotage = points[i + 9] > maxVotage ? points[i + 9] : maxVotage;
					minVotage = points[i + 9] < minVotage ? points[i + 9] : minVotage;
				}
			}
			//8=Balance
			points[8] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
			break;
			
		case (byte) 0x80: //LOG_EX_IR = 0x80
			//byte[0] length; byte[1] type, byte[2] channel, byte[3..6]ushort timeStamp, byte[7] logState, 
			//short[8,9] PackIR, short[8,9] SumCellIR, short[10,11] CellIR1, .... CellIR[CELL_MAX]
			int offset = 9 + this.getNumberOfLithiumCells();
			points[offset++] = DataParser.parse2UnsignedShort(dataBuffer, 8); //BatteryRi 

			//log.log(Level.OFF, StringHelper.byte2Hex2CharString(dataBuffer, dataBuffer.length));
			switch (dataBuffer[2] == 2 ? batteryType_2 : batteryType_1) {
			case "LiPo":
			case "LiIo":
			case "LiFe":
			case "LiHv":
			case "LTO":
			case "NiZn":
				points[offset++] = DataParser.parse2UnsignedShort(dataBuffer, 10); //CellRiSum
		
				//CellRi1, CellRi2, ... CellRi(numberOfLithiumCells)
				for (int i = 0,j=0; i < this.getNumberOfLithiumCells(); i++,j+=2) {
					points[i+offset] = DataParser.parse2UnsignedShort(dataBuffer, 12+j);
				}
				break;
			default:
			case "NiMH":
			case "NiCd":
			case "PB":
				break;
			}
			break;
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
		int numberOfNoneCalculationRecords = recordSet.getNoneCalculationRecordNames().length;
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * numberOfNoneCalculationRecords;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.size()];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		double energy = 0.0;
		double timeStep_h = 1.0 / 3600.0; //use default time step 1000 ms
		double lastTime_ms = 0, actualTime_ms = 0;
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		int riRecordOffset = 9 + this.getNumberOfLithiumCells();
		boolean isVariableIntervalTime = !recordSet.isTimeStepConstant();
		//System.out.println("isVariableIntervalTime = " + isVariableIntervalTime);

		if (isVariableIntervalTime) {
			int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
			int index = 0;
			for (int i = 0; i < recordDataSize; i++) {
				index = i * dataBufferSize + timeStampBufferSize;
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + index); //$NON-NLS-1$
				System.arraycopy(dataBuffer, index, convertBuffer, 0, dataBufferSize);
				
				//0=Current 1=SupplyVoltage 2=Voltage 3=Capacity 
				points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff)));
				points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff)));
				points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff)));
				points[3] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff)));
				//4=Power 5=Energy
				points[4] = Math.abs(Double.valueOf((points[0] / 100.0) * (points[2])).intValue()); // power U*I [W]
				//6=Temp.intern 7=Temp.extern
				points[6] = (((convertBuffer[16]&0xff) << 24) + ((convertBuffer[17]&0xff) << 16) + ((convertBuffer[18]&0xff) << 8) + ((convertBuffer[19]&0xff)));
				points[7] = (((convertBuffer[20]&0xff) << 24) + ((convertBuffer[21]&0xff) << 16) + ((convertBuffer[22]&0xff) << 8) + ((convertBuffer[23]&0xff)));
	
				int maxVotage = Integer.MIN_VALUE;
				int minVotage = Integer.MAX_VALUE;
				int k = 0;
				//7=VoltageCell1 8=VoltageCell2 9=VoltageCell3 10=VoltageCell4 11=VoltageCell5 12=VoltageCell6 ...... NumberOfLithiumCells
				for (int j=0; j < this.getNumberOfLithiumCells(); ++j, k+=GDE.SIZE_BYTES_INTEGER) {
					points[j + 9] = (((convertBuffer[k+24]&0xff) << 24) + ((convertBuffer[k+25]&0xff) << 16) + ((convertBuffer[k+26]&0xff) << 8) + ((convertBuffer[k+27]&0xff)));
					//System.out.println(String.format("points[%d] k=%d..%d <- %d", (j+9), k+24, k+27, points[j+9]));
					if (points[j + 9] > 0) {
						maxVotage = points[j + 9] > maxVotage ? points[j + 9] : maxVotage;
						minVotage = points[j + 9] < minVotage ? points[j + 9] : minVotage;
					}
				}
				//8=Balance calculate balance on the fly
				points[8] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
				//System.out.println("----");
				
				//BatteryRi, CellRi∑, CellRi1...
				if (recordSet.size() == 9 + this.getNumberOfLithiumCells() * 2 + 2) {
					for (int j=0; j < this.getNumberOfLithiumCells() + 2 && j+riRecordOffset < recordSet.size(); ++j, k+=GDE.SIZE_BYTES_INTEGER) {
						points[j+riRecordOffset] = (((convertBuffer[k+24]&0xff) << 24) + ((convertBuffer[k+25]&0xff) << 16) + ((convertBuffer[k+26]&0xff) << 8) + ((convertBuffer[k+27]&0xff)));
						//System.out.println(String.format("points[%d] k=%d..%d <- %d", (j+riRecordOffset), (k+24), (k+27), points[j+riRecordOffset]));
					}
				}
				
				actualTime_ms = ((((long)(dataBuffer[0 + (i * 4)] & 0xff)) << 24) + (((long)(dataBuffer[1 + (i * 4)] & 0xff)) << 16) + (((long)(dataBuffer[2 + (i * 4)] & 0xff)) << 8) + ((long)(dataBuffer[3 + (i * 4)] & 0xff))) / 10.0;
				
				//workaround while time stamp is reset, use constant time step as before
				if (actualTime_ms < lastTime_ms) {
					actualTime_ms = lastTime_ms + (recordSet.getTime_ms(recordSet.getRecordDataSize(true) - 2) - recordSet.getTime_ms(recordSet.getRecordDataSize(true) - 1));
				}
				
				if (points[3] == 0) //reset energy, only required for continuous recording, requires at least a short delay between charge and discharge
					energy = 0;
				
				timeStep_h = (actualTime_ms - lastTime_ms) / 1000.0 / 3600.0;
				if (i != 0)
					energy += points[4] * timeStep_h;  //energy = energy + (timeDelta * power)
				points[5] = Double.valueOf(energy).intValue();
				recordSet.addPoints(points, actualTime_ms);
				lastTime_ms = actualTime_ms;
	
				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*2500)/recordDataSize), sThreadId);				
			}
		} 
		else { //older recorded files doesn't have variable log interval
			for (int i = 0; i < recordDataSize; i++) {
				log.log(Level.FINER, i + " i*dataBufferSize = " + i*dataBufferSize); //$NON-NLS-1$
				System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);
	
				//0=Current. 1=SupplyVoltage 2=Voltage 3=Capacity 
				points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff)));
				points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff)));
				points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff)));
				points[3] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff)));
				//4=Power 5=Energy
				points[4] = Double.valueOf((points[0] / 100.0) * (points[2])).intValue(); // power U*I [W]
				if (i != 0)
					energy += points[4] * timeStep_h; 						//energy = energy + (timeDelta * power)
				points[5] = Double.valueOf(energy).intValue(); 
				//6=Temp.intern 7=Temp.extern
				points[6] = (((convertBuffer[16]&0xff) << 24) + ((convertBuffer[17]&0xff) << 16) + ((convertBuffer[18]&0xff) << 8) + ((convertBuffer[19]&0xff)));
				points[7] = (((convertBuffer[20]&0xff) << 24) + ((convertBuffer[21]&0xff) << 16) + ((convertBuffer[22]&0xff) << 8) + ((convertBuffer[23]&0xff)));
	
				int maxVotage = Integer.MIN_VALUE;
				int minVotage = Integer.MAX_VALUE;
				int k=0;
				//7=VoltageCell1 8=VoltageCell2 9=VoltageCell3 10=VoltageCell4 11=VoltageCell5 12=VoltageCell6 ...... NumberOfLithiumCells
				for (int j=0; j < this.getNumberOfLithiumCells(); ++j, k+=GDE.SIZE_BYTES_INTEGER) {
					points[j + 9] = (((convertBuffer[k+24]&0xff) << 24) + ((convertBuffer[k+25]&0xff) << 16) + ((convertBuffer[k+26]&0xff) << 8) + ((convertBuffer[k+27]&0xff)));
					//System.out.println(String.format("points[%d] k=%d..%d <- %d", (j+9), k+24, k+27, points[j+9]));
					if (points[j + 9] > 0) {
						maxVotage = points[j + 9] > maxVotage ? points[j + 9] : maxVotage;
						minVotage = points[j + 9] < minVotage ? points[j + 9] : minVotage;
					}
				}
				//8=Balance calculate balance on the fly
				points[8] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
				
				//BatteryRi, CellRi∑, CellRi1...
				if (recordSet.size() == 9 + this.getNumberOfLithiumCells() * 2 + 2) {
					for (int j=0; j < this.getNumberOfLithiumCells() + 2 && j+riRecordOffset < recordSet.size(); ++j, k+=GDE.SIZE_BYTES_INTEGER) {
						points[j+riRecordOffset] = (((convertBuffer[k+24]&0xff) << 24) + ((convertBuffer[k+25]&0xff) << 16) + ((convertBuffer[k+26]&0xff) << 8) + ((convertBuffer[k+27]&0xff)));
						//System.out.println(String.format("points[%d] k=%d..%d <- %d", (j+riRecordOffset), (k+24), (k+27), points[j+riRecordOffset]));
					}
				}
	
				recordSet.addPoints(points);
	
				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*2500)/recordDataSize), sThreadId);
			}
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * check and adapt stored measurement properties against actual record set records which gets created by device properties XML
	 * - calculated measurements could be later on added to the device properties XML
	 * - devices with battery cell voltage does not need to all the cell curves which does not contain measurement values
	 * @param fileRecordsProperties - all the record describing properties stored in the file
	 * @param recordSet - the record sets with its measurements build up with its measurements from device properties XML
	 * @return string array of measurement names which match the ordinal of the record set requirements to restore file record properties
	 */
	@Override
	public String[] crossCheckMeasurements(String[] fileRecordsProperties, RecordSet recordSet) {
		String[] recordKeys = recordSet.getRecordNames();
		Vector<String> cleanedRecordNames = new Vector<String>();
		
		if ((recordKeys.length - fileRecordsProperties.length) == 1) { // CellRi ∑
			recordSet.remove(recordKeys[9 + this.getNumberOfLithiumCells() + 1]);
			recordKeys = recordSet.getRecordNames();
		}
		else if ((recordKeys.length - fileRecordsProperties.length) > 0) { // CellRi ∑
			int i = 0;
			for (; i < fileRecordsProperties.length; ++i) {
				cleanedRecordNames.add(recordKeys[i]);
			}
			// cleanup recordSet
			for (; i < recordKeys.length; ++i) {
				recordSet.remove(recordKeys[i]);
			}
			recordKeys = cleanedRecordNames.toArray(new String[1]);
		}
		return recordKeys;
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
		Record recordPower = recordSet.get(4);
		Record recordEnergy = recordSet.get(5);
		if (recordSet.isRaw() && !recordPower.hasReasonableData() && !recordEnergy.hasReasonableData()) {
			// calculate the values required
			try {
				//0=Current 1=SupplyVoltage. 2=Voltage 3=Capacity 4=Power 5=Energy 6=Temp.intern 7=Temp.extern 8=Balance
				//9=VoltageCell1 10=VoltageCell2 11=VoltageCell3 12=VoltageCell4 13=VoltageCell5 14=VoltageCell6 12=VoltageCell6 ...... NumberOfLithiumCells
				int displayableCounter = 0;

				Record recordCurrent = recordSet.get(0);
				Record recordVoltage = recordSet.get(2);
				Record recordCapacity = recordSet.get(3);
				Record recordBalance = recordSet.get(8);

				recordPower.clear();
				recordEnergy.clear();
				recordBalance.clear();
				double energy = 0.0;
				double lastTime_ms = 0.0;

				for (int i = 0; i < recordCurrent.size(); i++) {
					//4=Power 5=Energy
					int power = Math.abs(Double.valueOf(recordVoltage.get(i) * recordCurrent.get(i) / 100.0).intValue());
					recordPower.add(power); // power U*I [W]
          if (recordCapacity.get(i) == 0)
            energy = 0.0;
					if (i != 0) {
						double timeStep_h = (recordSet.getTime_ms(i) - lastTime_ms) / 1000.0 / 3600.0;
						energy += power * timeStep_h;  //energy = energy + (timeDelta * power)
					}
					recordEnergy.add(Double.valueOf(energy).intValue());
					lastTime_ms = recordSet.getTime_ms(i);

					int maxVotage = Integer.MIN_VALUE;
					int minVotage = Integer.MAX_VALUE;
					for (int j = 0; j < this.getNumberOfLithiumCells(); j++) {
						Record  selectedRecord = recordSet.get(j + 9);
						if (selectedRecord.size() > i) {
							int value = selectedRecord.get(i);
							if (value > 0) {
								maxVotage = value > maxVotage ? value : maxVotage;
								minVotage = value < minVotage ? value : minVotage;
							}
						}
					}
					//8=Balance
					recordBalance.add(maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0);

				}

				// check if measurements isActive == false and set to isDisplayable == false
				for (int i = 0; i < recordSet.size(); i++) {
					Record record = recordSet.get(i);
					if (record.isActive() && record.hasReasonableData()) {
						++displayableCounter;
					}
				}

				log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
				recordSet.setConfiguredDisplayable(displayableCounter);

				if (this.channels.getActiveChannel().getActiveRecordSet() == null || recordSet.getName().equals(this.channels.getActiveChannel().getActiveRecordSet().getName())) {
					this.application.updateGraphicsWindow();
				}
			}
			catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		this.updateVisibilityStatus(recordSet, true);
	}

	/**
	 * method to get the sorted active or in active record names as string array
	 * - records which does not have inactive or active flag are calculated from active or inactive
	 * - all records not calculated may have the active status and must be stored
	 * @param channelConfigNumber
	 * @param validMeasurementNames based on the current or any previous configuration
	 * @return String[] containing record names
	 */
	@Override
	public String[] getNoneCalculationMeasurementNames(int channelConfigNumber, String[] validMeasurementNames) {
		final Vector<String> tmpCalculationRecords = new Vector<String>();
		final String[] deviceMeasurements = this.getMeasurementNames(channelConfigNumber);
		int deviceDataBlockSize = Math.abs(this.getDataBlockSize(FormatTypes.VALUE)) + getNumberOfLithiumCells() + 2;
		deviceDataBlockSize = this.getDataBlockSize(FormatTypes.VALUE) <= 0 ? deviceMeasurements.length : deviceDataBlockSize;
		// record names may not match device measurements, but device measurements might be more then existing records
		for (int i = 0; i < deviceMeasurements.length && i < validMeasurementNames.length; ++i) {
			final MeasurementType measurement = this.getMeasurement(channelConfigNumber, i);
			if (!measurement.isCalculation()) { // active or inactive
				tmpCalculationRecords.add(validMeasurementNames[i]);
			}
			// else
			// System.out.println(measurement.getName());
		}
		// assume attached records are calculations like DataVario
		while (tmpCalculationRecords.size() > deviceDataBlockSize) {
			tmpCalculationRecords.remove(deviceDataBlockSize);
		}
		return tmpCalculationRecords.toArray(new String[0]);
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public int[] getCellVoltageOrdinals() {
		//0=Current 1=SupplyVoltage. 2=Voltage 3=Capacity 4=Power 5=Energy 6=Temp.intern 7=Temp.extern 8=Balance
		//9=VoltageCell1 10=VoltageCell2 11=VoltageCell3 12=VoltageCell4 13=VoltageCell5 14=VoltageCell6 12=VoltageCell6 ...... NumberOfLithiumCells
		return new int[] {2, 3};
	}
	
	public long getTimeStamp(final byte[] buffer) {
		log.finest(() -> StringHelper.byte2Hex2CharString(buffer, 3, 4));
		return DataParser.getUInt32(buffer, 3);
	}
	
	public void resetEnergySum(int channelnumber) {
		if (channelnumber == 1)
			energySum1 = 0.0;
		else 
			energySum2 = 0.0;
	}
	
	/**
	 * @return the minimal input voltage
	 */
	public abstract int getDcInputVoltMin();
	
	/**
	 * @return the maximal input voltage
	 */
	public abstract int getDcInputVoltMax();
	
	/**
	 * @return the maximal input current
	 */
	public abstract int getDcInputCurrentMax();
	
	/**
	 * @return the minimal regenerative input voltage
	 */
	public abstract int getRegInputVoltMin();
	
	/**
	 * @return the maximal regenerative input voltage
	 */
	public abstract int getRegInputVoltMax();
	
	/**
	 * @return the maximal charge current for channel (duo -> async)
	 */
	public abstract int getChargeCurrentMax4Channel();
	
	/**
	 * @return the maximal charge current for channels in sync
	 */
	public abstract int getChargeCurrentMaxSyncChannels();
	
	/**
	 * @return the maximal charge power of device
	 */
	public abstract int getDeviceChargePowerMax();
	
	/**
	 * @return the maximal charge power
	 */
	public abstract int[] getChargePowerMax();
	
	/**
	 * @return the maximal discharge current
	 */
	public abstract int[] getDischargePowerMax();		
	
	/**
	 * @return the min/max regenerative channel voltage, factor 1000
	 */
	public abstract int[] getRegChannelVoltageLimits();
	
	/**
	 * @return the min/max regenerative channel current, factor 100
	 */
	public abstract int[] getRegChannelCurrentLimits();
	
	/**
	 * @return the max voltage adjustable for digital power supply voltage, factor 10
	 */
	public abstract int getDigitalPowerVoltageMax();
	
}
