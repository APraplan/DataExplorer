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
package gde.device.junsi;

import java.util.Arrays;
import java.util.logging.Logger;

import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbNotClaimedException;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.InputTypes;
import gde.device.junsi.modbus.ChargerInfo;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

/**
 * Thread implementation to gather data from eStation device
 * @author Winfied Brügmann
 */
public class UsbGathererThread extends Thread {
	protected static final int	USB_QUERY_DELAY	= GDE.IS_WINDOWS ? 70 : 160;
	final static String	$CLASS_NAME									= UsbGathererThread.class.getName();
	final static Logger	log													= Logger.getLogger(UsbGathererThread.class.getName());
	final static int		WAIT_TIME_RETRYS						= 1900;		// 1900 * 1 sec = 30 Minutes

	final DataExplorer					application;
	final Settings							settings;
	final iChargerUsbPort				usbPort;
	final iChargerUsb						device;
	final Channels							channels;
	final Channel								channel;
	final int										channelNumber;

	String							recordSetKey								= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272);
	boolean							isPortOpenedByLiveGatherer	= false;
	boolean							isGatheredRecordSetVisible	= true;
	boolean							isCollectDataStopped				= false;
	boolean							isProgrammExecuting1				= false;
	boolean							isProgrammExecuting2				= false;
	boolean							isProgrammExecuting3				= false;
	boolean							isProgrammExecuting4				= false;
	boolean[]						isAlerted4Finish						= { false, false, false, false };
	int									retryCounter								= UsbGathererThread.WAIT_TIME_RETRYS;	//30 Min
	long								lastTimeStamp_01_ms						= 0;
	long 								lastRecordEndTimeStamp_01_ms 	= 0;
	long								lastTimeStamp_02_ms						= 0;
	long 								lastRecordEndTimeStamp_02_ms 	= 0;
	ChargerInfo 				chargerInfo 									= null;
	int									lastNumberDisplayableRecords	= 0;


	/**
	 * data gatherer thread definition 
	 * @throws SerialPortException 
	 * @throws ApplicationConfigurationException 
	 * @throws UsbException 
	 * @throws Exception 
	 */
	public UsbGathererThread(DataExplorer currentApplication, iChargerUsb useDevice, iChargerUsbPort useSerialPort, int channelConfigNumber) throws ApplicationConfigurationException,
			UsbDisconnectedException, UsbException {
		super("dataGatherer");
		this.application = currentApplication;
		this.settings = Settings.getInstance();
		this.device = useDevice;
		this.usbPort = useSerialPort;
		this.channels = Channels.getInstance();
		this.channelNumber = channelConfigNumber;
		this.channel = this.channels.get(this.channelNumber);
		if (this.usbPort!= null) this.usbPort.setTimeOut_ms(iChargerUsbPort.TIMEOUT_MS); //reset timeout to default

		if (!this.usbPort.isConnected()) {
			this.usbPort.openUsbPort();
			this.isPortOpenedByLiveGatherer = true;
		}
		this.setPriority(Thread.MAX_PRIORITY);
		
		try {
			if (this.device.getDialog() != null) {
				try {
					this.usbPort.getData(); //sync logging and modbus query
					WaitTimer.delay(100);
				}
				catch (TimeOutException e) {
					//ignore 
				}
				this.device.getDialog().startLogTransmission();
				this.chargerInfo = this.device.getDialog().readInfo();
				if (this.chargerInfo != null)
					log.log(Level.INFO, this.device.getName() + GDE.STRING_MESSAGE_CONCAT + this.chargerInfo.getSystemInfo(this.device.getName().toLowerCase().endsWith("duo") ? true : false));
				else 
					log.log(Level.WARNING, this.device.getName() + GDE.STRING_MESSAGE_CONCAT + "failed to initialize chargerInfo");
			}
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	@Override
	public void run() {
		try {
			final String $METHOD_NAME = "run"; //$NON-NLS-1$
			RecordSet recordSet1 = null;
			RecordSet recordSet2 = null;
			int[] points1 = new int[this.device.getNumberOfMeasurements(1)];
			int[] points2 = new int[this.device.getNumberOfMeasurements(2)];
			int lastEnergie1 = 0, lastEnergie2 = 0;

			this.isProgrammExecuting1 = false;
			this.isProgrammExecuting2 = false;

			byte[] dataBuffer1 = new byte[this.device.getDataBlockSize(InputTypes.SERIAL_IO)];
			byte[] dataBuffer2 = new byte[this.device.getDataBlockSize(InputTypes.SERIAL_IO)];
			byte[] dataBuffer = null;
			Object[] ch1, ch2;
			String recordSetKey1 = Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
			String recordSetKey2 = Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization

			this.isCollectDataStopped = false;
			if (UsbGathererThread.log.isLoggable(Level.FINE))
				UsbGathererThread.log.logp(Level.FINE, UsbGathererThread.$CLASS_NAME, $METHOD_NAME, "====> entry initial time step ms = " + this.device.getTimeStep_ms()); //$NON-NLS-1$

			while (!this.isCollectDataStopped && this.usbPort.isConnected()) {
				try {
					if (this.application != null) {
						this.application.setPortConnected(true);
						//this.application.setSerialTxOn();
						this.application.setSerialRxOn();
						//this.application.setSerialTxOff();
					}
					dataBuffer = this.usbPort.getData();
					if (this.application != null) this.application.setSerialRxOff();

					this.isProgrammExecuting1 = dataBuffer[2] == 0x01; //output channel 1
					this.isProgrammExecuting2 = dataBuffer[2] == 0x02 ;//output channel 2
					
					if (this.isProgrammExecuting1) 
						System.arraycopy(dataBuffer, 0, dataBuffer1, 0, dataBuffer1.length);
					if (this.isProgrammExecuting2) 
						System.arraycopy(dataBuffer, 0, dataBuffer2, 0, dataBuffer2.length);

					// check if device is ready for data capturing, charge,discharge or pause only
					if (this.isProgrammExecuting1 || this.isProgrammExecuting2) {
						lastEnergie1 = points1[5];
						//points1 = new int[this.device.getNumberOfMeasurements(1)]; //move initialization of point array to processDataChannel for new recordSet created.
						
						if (this.isProgrammExecuting2) {
							lastEnergie2 = points2[5];
							//points2 = new int[this.device.getNumberOfMeasurements(2)]; //move initialization of point array to processDataChannel for new recordSet created.
						}

						if (this.isProgrammExecuting1) { // checks for processes active includes check state change waiting to discharge to charge
							points1[5] = lastEnergie1;
							ch1 = processDataChannel(1, recordSet1, recordSetKey1, dataBuffer1, points1);
							recordSet1 = (RecordSet) ch1[0];
							recordSetKey1 = (String) ch1[1];
						}
						if (this.isProgrammExecuting2) { // checks for processes active includes check state change waiting to discharge to charge
							points2[5] = lastEnergie2;
							ch2 = processDataChannel(2, recordSet2, recordSetKey2, dataBuffer2, points2);
							recordSet2 = (RecordSet) ch2[0];
							recordSetKey2 = (String) ch2[1];
						}
						
						this.application.setStatusMessage(GDE.STRING_EMPTY);
						
						this.retryCounter	= UsbGathererThread.WAIT_TIME_RETRYS;	//30 Min
					}
					else {
						this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI2600));
						log.logp(Level.FINE, UsbGathererThread.$CLASS_NAME, $METHOD_NAME, "wait for device activation ..."); //$NON-NLS-1$
						WaitTimer.delay(1000);

						if (0 >= (this.retryCounter -= 1)) {
							log.log(Level.FINE, "device activation timeout"); //$NON-NLS-1$
							this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGW2600));
							stopDataGatheringThread(false, null);
						}
					}
				}
				catch (DataInconsitsentException e) {
					String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0036, new Object[] { this.getClass().getSimpleName(), $METHOD_NAME });
					cleanup(message);
				}
				catch (Throwable e) {
					// this case will be reached while data gathering enabled, but no data will be received
					if (e instanceof TimeOutException) {
						if (this.usbPort!= null) this.usbPort.setTimeOut_ms(iChargerUsbPort.TIMEOUT_MS); //reset timeout to default
						this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI2600));
						if (UsbGathererThread.log.isLoggable(Level.FINE))
							UsbGathererThread.log.logp(Level.FINE, UsbGathererThread.$CLASS_NAME, $METHOD_NAME, Messages.getString(MessageIds.GDE_MSGI2600));
					}
					else if (e instanceof UsbNotClaimedException) { //USB error detected, p.e. disconnect
						this.application.setStatusMessage(Messages.getString(gde.messages.MessageIds.GDE_MSGE0051, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
						stopDataGatheringThread(false, e);
					}
					else if (e instanceof UsbDisconnectedException) { //USB error detected, p.e. disconnect
						UsbGathererThread.log.log(Level.SEVERE, e.getMessage(), e);
						this.application.setStatusMessage(e.getMessage(), SWT.COLOR_RED);
						try {
								this.usbPort.closeUsbPort(true);
								UsbGathererThread.log.log(Level.FINE, "USB interface closed");
						}
						catch (UsbException eClose) {
							UsbGathererThread.log.log(Level.SEVERE, eClose.getMessage(), eClose);
						}

						for (int i = 0; i < 5; i++) {
							try {
								WaitTimer.delay(500);
								if (!this.usbPort.isConnected()) {
									UsbGathererThread.log.log(Level.WARNING, "USB error recovery, reopen USB port");
									this.usbPort.openUsbPort();
									this.isPortOpenedByLiveGatherer = true;
								}
							}
							catch (UsbException eOpen) {
								UsbGathererThread.log.log(Level.SEVERE, eOpen.getMessage(), eOpen);
							}
						}
					}
					else if (e instanceof UsbException) { //USB error detected, p.e. disconnect
						this.application.setStatusMessage(Messages.getString(gde.messages.MessageIds.GDE_MSGE0050));
						stopDataGatheringThread(false, e);
					}
					else if (e instanceof IllegalStateException) { //USB error detected, p.e. disconnect
						UsbGathererThread.log.log(Level.SEVERE, e.getMessage(), e);
						this.application.setStatusMessage(e.getMessage(), SWT.COLOR_RED);
						try {
								this.usbPort.closeUsbPort(true);
								UsbGathererThread.log.log(Level.FINE, "USB interface closed");
						}
						catch (UsbException eClose) {
							UsbGathererThread.log.log(Level.SEVERE, eClose.getMessage(), eClose);
						}

						for (int i = 0; i < 5; i++) {
							try {
								WaitTimer.delay(500);
								if (!this.usbPort.isConnected()) {
									UsbGathererThread.log.log(Level.WARNING, "USB error recovery, reopen USB port");
									this.usbPort.openUsbPort();
									this.isPortOpenedByLiveGatherer = true;
								}
							}
							catch (UsbException eOpen) {
								UsbGathererThread.log.log(Level.SEVERE, eOpen.getMessage(), eOpen);
							}
						}
					}
					// program end or unexpected exception occurred, stop data gathering to enable save data by user
					else {
						UsbGathererThread.log.log(Level.FINE, "data gathering end detected"); //$NON-NLS-1$
						stopDataGatheringThread(true, e);
					}
				}
			}
			this.application.setStatusMessage(""); //$NON-NLS-1$
			if (UsbGathererThread.log.isLoggable(Level.FINE)) UsbGathererThread.log.logp(Level.FINE, UsbGathererThread.$CLASS_NAME, $METHOD_NAME, "======> exit"); //$NON-NLS-1$

			if (!this.isCollectDataStopped) {
				this.stopDataGatheringThread(true, null);
			}
		}
		finally {
			try {
					this.usbPort.closeUsbPort(false);
					UsbGathererThread.log.log(Level.FINE, "USB interface closed");
			}
			catch (UsbException e) {
				UsbGathererThread.log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * process a outlet channel
	 * @param number
	 * @param recordSet
	 * @param processRecordSetKey
	 * @param dataBuffer
	 * @param points
	 * @return object array with updated values {recordSet, recordSetKey}
	 * @throws DataInconsitsentException
	 */
	private Object[] processDataChannel(final int number, RecordSet recordSet, String processRecordSetKey, final byte[] dataBuffer, final int[] points) throws DataInconsitsentException {
		final String $METHOD_NAME = "processOutlet"; //$NON-NLS-1$
		Object[] result = new Object[2];
		boolean isReduceChargeDischarge = this.settings.isReduceChargeDischarge();
		boolean isContinuousRecordSet = this.settings.isContinuousRecordSet();
 
		if ((dataBuffer[7] & 0xFF) == 0x80) { //LOG_EX_IR = 0x80 only integrate Ri values
			if (UsbGathererThread.log.isLoggable(Level.FINE)) 
				UsbGathererThread.log.log(Level.FINE, String.format("channel:%d integrate Ri values", number));
			this.device.convertDataBytes(points, dataBuffer);
			result[0] = recordSet;
			result[1] = processRecordSetKey;
			return result;
		} 
		else if ((dataBuffer[7] & 0xFF) > 0x80) { //other iCharger special states like cell capacity 0x82
			log.log(Level.WARNING, "iCharger special message - " + StringHelper.byte2Hex2CharString(dataBuffer, dataBuffer.length));
			result[0] = recordSet;
			result[1] = processRecordSetKey;
			return result;
		}
		else if (isReduceChargeDischarge && dataBuffer[7] == 4) { //skip Mode: 4=PAUSE
			result[0] = recordSet;
			result[1] = processRecordSetKey;
			return result;
		} 
		
		//BATTERY_TYPE 1=LiPo 2=LiIo 3=LiFe 4=NiMH 5=NiCd 6=Pb 7=NiZn
		String batteryType = this.device.getBatteryType(dataBuffer);
		//Mode： 		1=CHARGE 2=DISCHARGE 4=PAUSE 8=TrickleCurrent 9=Balancing
		int processModeNumber = dataBuffer[7];
		String processTypeName = isContinuousRecordSet ? Messages.getString(MessageIds.GDE_MSGT2618) : this.device.getRecordSetStateNameReplacement(processModeNumber);
		//STATUS:     0=normal !0=cycle
		String processStatusName = !isContinuousRecordSet && dataBuffer[9] != 0 ? Messages.getString(MessageIds.GDE_MSGT2610) : GDE.STRING_EMPTY;
		if (UsbGathererThread.log.isLoggable(Level.FINE)) {
			UsbGathererThread.log.log(Level.FINE, String.format("channel:%d %s %s %s", number, batteryType, processTypeName, processStatusName).trim());
		}
		
		Channel outputChannel = this.channels.get(number);
		if (outputChannel != null) {
			long deltaTimeStamp_ms = this.device.getTimeStamp(dataBuffer) - (number == 2 ? this.lastTimeStamp_02_ms : this.lastTimeStamp_01_ms);
			// check if a record set matching for re-use is available and prepare a new if required, case continuous gathering check for timeStamp since starting a new process will reset time
			if (recordSet == null || (deltaTimeStamp_ms <= 0 && this.device.getTimeStamp(dataBuffer) == 0) || !processRecordSetKey.contains(batteryType)  || !processRecordSetKey.contains(processTypeName) || !processRecordSetKey.contains(processStatusName)) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "timestampDifference " + deltaTimeStamp_ms);
				this.application.setStatusMessage(""); //$NON-NLS-1$

				// record set does not exist or is out dated, build a new name and create
				StringBuilder extend = new StringBuilder();
				if (!isContinuousRecordSet) {
					//Mode： 		1=CHARGE 2=DISCHARGE 4=PAUSE
					if (dataBuffer[9] != 0) { //CYCLE
						int cycleNumber = dataBuffer[9];

						if (cycleNumber >= 0) {
							extend.append(GDE.STRING_BLANK_LEFT_BRACKET).append(processStatusName).append(GDE.STRING_COLON).append(cycleNumber).append(GDE.STRING_RIGHT_BRACKET);
						}
					}
				}
				device.resetEnergySum(number); //reset integrated energy due to switch over capacity and energy 
				processRecordSetKey = String.format("%d) %s - %s %s", outputChannel.getNextRecordSetNumber(), batteryType, processTypeName, extend.toString()).trim();
				processRecordSetKey = processRecordSetKey.length() <= RecordSet.MAX_NAME_LENGTH ? processRecordSetKey : processRecordSetKey.substring(0, RecordSet.MAX_NAME_LENGTH);

				outputChannel.put(processRecordSetKey, RecordSet.createRecordSet(processRecordSetKey, this.application.getActiveDevice(), outputChannel.getNumber(), true, false, true));
				outputChannel.applyTemplateBasics(processRecordSetKey);
				UsbGathererThread.log.logp(Level.FINE, UsbGathererThread.$CLASS_NAME, $METHOD_NAME, processRecordSetKey + " created for channel " + outputChannel.getName()); //$NON-NLS-1$
				recordSet = outputChannel.get(processRecordSetKey);
				if (this.chargerInfo != null)
					recordSet.setRecordSetDescription(String.format("%s\n%s", recordSet.getDescription(), this.chargerInfo.getSystemInfo(this.device.getName().toLowerCase().endsWith("duo") ? true : false)));
				recordSet.setAllDisplayable();
				channel.applyTemplate(recordSetKey, false);
				// switch the active record set if the current record set is child of active channel
				this.channels.switchChannel(outputChannel.getNumber(), processRecordSetKey);
				outputChannel.switchRecordSet(processRecordSetKey);
				Arrays.fill(points, 0); //clear points array for the new recordSet created
				switch (number) {
				default:
				case 1:
					this.lastRecordEndTimeStamp_01_ms = this.device.getTimeStamp(dataBuffer);
					this.lastTimeStamp_01_ms = 0;
					break;
				case 2:
					this.lastRecordEndTimeStamp_02_ms = this.device.getTimeStamp(dataBuffer);
					this.lastTimeStamp_02_ms = 0;
					break;
				}
			}

			deltaTimeStamp_ms = this.device.getTimeStamp(dataBuffer) - (number == 2 ? this.lastRecordEndTimeStamp_02_ms : this.lastRecordEndTimeStamp_01_ms);
			if (deltaTimeStamp_ms > 0)
				recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer), deltaTimeStamp_ms);
			
			long intervalTime_ms = deltaTimeStamp_ms - (number == 2 ? this.lastTimeStamp_02_ms : this.lastTimeStamp_01_ms);
			if (UsbGathererThread.log.isLoggable(Level.INFO))
				UsbGathererThread.log.log(Level.INFO, "TimeOut = " + this.usbPort.getTimeOut_ms() + " deltaTimeStamp_ms = " + deltaTimeStamp_ms + " intervalTime_ms = " + intervalTime_ms + " lastTimeStamp_01_ms = " + lastTimeStamp_01_ms);
			if (this.usbPort.getTimeOut_ms() != (intervalTime_ms + 200))
				this.usbPort.setTimeOut_ms(intervalTime_ms + 200);
			
			switch (number) {
			default:
			case 1:
				this.lastTimeStamp_01_ms = deltaTimeStamp_ms;
				break;
			case 2:
				this.lastTimeStamp_02_ms = deltaTimeStamp_ms;
				break;
			}

			if (recordSet.get(0).realSize() < 3 || recordSet.get(0).realSize() % 10 == 0) {
				this.device.updateVisibilityStatus(recordSet, true);
			}

			RecordSet activeRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
			if (activeRecordSet != null && recordSet.size() > 0 && recordSet.isChildOfActiveChannel() && recordSet.equals(activeRecordSet)) {
				UsbGathererThread.this.application.updateAllTabs(false, this.lastNumberDisplayableRecords != activeRecordSet.getConfiguredDisplayable());
				this.lastNumberDisplayableRecords = activeRecordSet.getConfiguredDisplayable();
			}
		}
		result[0] = recordSet;
		result[1] = processRecordSetKey;
		return result;
	}

	/**
	 * stop the data gathering and check if reasonable data in record set to finalize or clear
	 * @param enableEndMessage
	 * @param throwable
	 */
	void stopDataGatheringThread(boolean enableEndMessage, Throwable throwable) {
		final String $METHOD_NAME = "stopDataGatheringThread"; //$NON-NLS-1$

		if (throwable != null) {
			UsbGathererThread.log.logp(Level.WARNING, UsbGathererThread.$CLASS_NAME, $METHOD_NAME, throwable.getMessage(), throwable);
		}

		this.isCollectDataStopped = true;
		WaitTimer.delay(this.usbPort.getTimeOut_ms());

		if (this.usbPort != null && this.usbPort.getXferErrors() > 0) {
			UsbGathererThread.log.log(Level.WARNING, "During complete data transfer " + this.usbPort.getXferErrors() + " number of errors occured!"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (this.usbPort != null && this.usbPort.isConnected() && this.isPortOpenedByLiveGatherer == true && this.usbPort.isConnected()) {
			try {
				this.usbPort.closeUsbPort(false);
			}
			catch (UsbException e) {
				UsbGathererThread.log.log(Level.WARNING, e.getMessage(), e);
			}
		}

		RecordSet recordSet = this.channel.get(this.recordSetKey);
		if (recordSet != null && recordSet.getRecordDataSize(true) > 5) { // some other exception while program execution, record set has data points
			finalizeRecordSet(false);
			if (enableEndMessage) this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGT2603));
		}
		else {
			if (enableEndMessage && throwable != null) cleanup(throwable.getMessage());
			else if (throwable != null) cleanup(Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { throwable.getClass().getSimpleName(), throwable.getMessage() }));
			else if (enableEndMessage) cleanup(Messages.getString(gde.messages.MessageIds.GDE_MSGE0026));
		}
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 */
	void finalizeRecordSet(boolean doClosePort) {
		if (doClosePort && this.isPortOpenedByLiveGatherer && this.usbPort.isConnected()) try {
			this.usbPort.closeUsbPort(false);
		}
		catch (UsbException e) {
			UsbGathererThread.log.log(Level.WARNING, e.getMessage(), e);
		}

		RecordSet tmpRecordSet = this.channel.get(this.recordSetKey);
		if (tmpRecordSet != null) {
			this.device.updateVisibilityStatus(tmpRecordSet, false);
			this.device.makeInActiveDisplayable(tmpRecordSet);
			this.application.updateStatisticsData();
			this.application.updateDataTable(this.recordSetKey, false);

			this.device.setAverageTimeStep_ms(tmpRecordSet.getAverageTimeStep_ms());
			UsbGathererThread.log.log(Level.TIME, "set average time step msec = " + this.device.getAverageTimeStep_ms()); //$NON-NLS-1$
		}
	}

	/**
	 * cleanup all allocated resources and display the message
	 * @param this.recordSetKey
	 * @param message
	 * @param e
	 */
	void cleanup(final String message) {
		if (this.channel.get(this.recordSetKey) != null) {
			this.channel.get(this.recordSetKey).clear();
			this.channel.remove(this.recordSetKey);
			if (Thread.currentThread().getId() == this.application.getThreadId()) {
				this.application.getMenuToolBar().updateRecordSetSelectCombo();
				this.application.updateStatisticsData();
				this.application.updateDataTable(this.recordSetKey, true);
				this.application.openMessageDialog(message);
				//this.device.getDialog().resetButtons();
			}
			else {
				final String useRecordSetKey = this.recordSetKey;
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						UsbGathererThread.this.application.getMenuToolBar().updateRecordSetSelectCombo();
						UsbGathererThread.this.application.updateStatisticsData();
						UsbGathererThread.this.application.updateDataTable(useRecordSetKey, true);
						UsbGathererThread.this.application.openMessageDialog(message);
						//GathererThread.this.device.getDialog().resetButtons();
					}
				});
			}
		}
		else
			this.application.openMessageDialog(message);
	}

	/**
	 * @param enabled the isCollectDataStopped to set
	 */
	void setCollectDataStopped(boolean enabled) {
		this.isCollectDataStopped = enabled;
	}

	/**
	 * @return the isCollectDataStopped
	 */
	boolean isCollectDataStopped() {
		return this.isCollectDataStopped;
	}
}
