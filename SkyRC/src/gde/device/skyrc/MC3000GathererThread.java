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
package gde.device.skyrc;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.ChannelTypes;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.WaitTimer;

import java.util.logging.Logger;

import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.usb.UsbNotClaimedException;

/**
 * Thread implementation to gather data from eStation device
 * @author Winfied Brügmann
 */
public class MC3000GathererThread extends Thread {
	protected static final int	USB_QUERY_DELAY	= GDE.IS_WINDOWS ? 70 : 160;
	final static String	$CLASS_NAME									= MC3000GathererThread.class.getName();
	final static Logger	log													= Logger.getLogger(MC3000GathererThread.class.getName());
	final static int		WAIT_TIME_RETRYS_END_SEC		= 3600;		// 3600 * 1 sec = 60 Minutes
	final static int		WAIT_TIME_RETRYS_MAX_SEC		= (240 + 1) * 60; // maximal evaluated resting time plus 1 Min

	final DataExplorer	application;
	final Settings			settings;
	final MC3000UsbPort	usbPort;
	final MC3000				device;
	final MC3000Dialog	dialog;
	final Channels			channels;
	final Channel				channel;
	final int						channelNumber;

	String							recordSetKey								= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272);
	boolean							isPortOpenedByLiveGatherer	= false;
	boolean							isGatheredRecordSetVisible	= true;
	boolean							isCollectDataStopped				= false;
	UsbInterface				usbInterface								= null;
	boolean							isProgrammExecuting1				= false;
	boolean							isProgrammExecuting2				= false;
	boolean							isProgrammExecuting3				= false;
	boolean							isProgrammExecuting4				= false;
	boolean[]						isAlerted4Finish						= { false, false, false, false };
	int									retryCounterRest_sec				= MC3000GathererThread.WAIT_TIME_RETRYS_MAX_SEC;	//maximal evaluated resting time plus 1 Min
	int									retryCounterEnd_sec					= MC3000GathererThread.WAIT_TIME_RETRYS_END_SEC;	//60/4 = 15 Min

	/**
	 * data gatherer thread definition 
	 * @throws SerialPortException 
	 * @throws ApplicationConfigurationException 
	 * @throws UsbException 
	 * @throws Exception 
	 */
	public MC3000GathererThread(DataExplorer currentApplication, MC3000 useDevice, MC3000UsbPort useSerialPort, int channelConfigNumber, MC3000Dialog useDialog) throws ApplicationConfigurationException,
			UsbDisconnectedException, UsbException {
		super("dataGatherer");
		this.application = currentApplication;
		this.settings = Settings.getInstance();
		this.device = useDevice;
		this.dialog = useDialog;
		this.usbPort = useSerialPort;
		this.channels = Channels.getInstance();
		this.channelNumber = channelConfigNumber;
		this.channel = this.channels.get(this.channelNumber);

		if (!this.usbPort.isConnected()) {
			this.usbInterface = this.usbPort.openUsbPort(this.device);
			this.isPortOpenedByLiveGatherer = true;
		}
		this.setPriority(Thread.MAX_PRIORITY);
	}

	@Override
	public void run() {
		try {
			final String $METHOD_NAME = "run"; //$NON-NLS-1$
			RecordSet recordSet1 = null;
			RecordSet recordSet2 = null;
			RecordSet recordSet3 = null;
			RecordSet recordSet4 = null;
			RecordSet recordSet5 = null;
			int[] points1 = new int[this.device.getNumberOfMeasurements(1)];
			int[] points2 = new int[this.device.getNumberOfMeasurements(2)];
			int[] points3 = new int[this.device.getNumberOfMeasurements(3)];
			int[] points4 = new int[this.device.getNumberOfMeasurements(4)];
			int[] points5 = new int[this.device.getNumberOfMeasurements(5)];
			int lastEnergie1, lastEnergie2, lastEnergie3, lastEnergie4;

			this.isProgrammExecuting1 = false;
			this.isProgrammExecuting2 = false;
			this.isProgrammExecuting3 = false;
			this.isProgrammExecuting4 = false;

			long lastCycleTime = 0;
			byte[] dataBuffer1 = null;
			byte[] dataBuffer2 = null;
			byte[] dataBuffer3 = null;
			byte[] dataBuffer4 = null;
			Object[] ch1, ch2, ch3, ch4;
			String recordSetKey1 = Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
			String recordSetKey2 = Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
			String recordSetKey3 = Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
			String recordSetKey4 = Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
			String recordSetKey5 = Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization

			this.isCollectDataStopped = false;
			if (MC3000GathererThread.log.isLoggable(Level.FINE))
				MC3000GathererThread.log.logp(Level.FINE, MC3000GathererThread.$CLASS_NAME, $METHOD_NAME, "====> entry initial time step ms = " + this.device.getTimeStep_ms()); //$NON-NLS-1$

			lastCycleTime = System.nanoTime()/1000000;
			while (!this.isCollectDataStopped && this.usbPort.isConnected()) {
				try {
					// check if device is ready for data capturing or terminal open
					// in case of time outs wait for 180 seconds max. for actions
					if (this.dialog != null && !this.dialog.isDisposed() && this.dialog.getTabFolderSelectionIndex() == 0) {
						System.out.println();
					}
					else { //if (this.dialog != null && this.dialog.isDisposed()) {
						if (this.application != null) {
							this.application.setSerialTxOn();
							this.application.setSerialRxOn();
						}
						//get data from device for all4 slots
						if (this.usbPort.isConnected()) dataBuffer1 = this.usbPort.getData(this.usbInterface, MC3000UsbPort.TakeMtuData.SLOT_0.value());
						WaitTimer.delay(USB_QUERY_DELAY);
						if (this.usbPort.isConnected()) dataBuffer2 = this.usbPort.getData(this.usbInterface, MC3000UsbPort.TakeMtuData.SLOT_1.value());
						if (this.application != null) this.application.setSerialTxOff();
						WaitTimer.delay(USB_QUERY_DELAY);
						if (this.usbPort.isConnected()) dataBuffer3 = this.usbPort.getData(this.usbInterface, MC3000UsbPort.TakeMtuData.SLOT_2.value());
						WaitTimer.delay(USB_QUERY_DELAY);
						if (this.usbPort.isConnected()) dataBuffer4 = this.usbPort.getData(this.usbInterface, MC3000UsbPort.TakeMtuData.SLOT_3.value());
						if (this.application != null) this.application.setSerialRxOff();

						this.isProgrammExecuting1 = this.device.isProcessing(1, dataBuffer1);
						this.isProgrammExecuting2 = this.device.isProcessing(2, dataBuffer2);
						this.isProgrammExecuting3 = this.device.isProcessing(3, dataBuffer3);
						this.isProgrammExecuting4 = this.device.isProcessing(4, dataBuffer4);

						// check if device is ready for data capturing, discharge or charge allowed only
						// else wait for 180 seconds max. for actions
						if (this.isProgrammExecuting1 || this.isProgrammExecuting2 || this.isProgrammExecuting3 || this.isProgrammExecuting4) {
							lastEnergie1 = points1[4];
							points1 = new int[this.device.getNumberOfMeasurements(1)];
							
							lastEnergie2 = points2[4];
							points2 = new int[this.device.getNumberOfMeasurements(2)];
							
							lastEnergie3 = points3[4];
							points3 = new int[this.device.getNumberOfMeasurements(3)];
							
							lastEnergie4 = points4[4];
							points4 = new int[this.device.getNumberOfMeasurements(4)];


							if (this.isProgrammExecuting1) { // checks for processes active includes check state change waiting to discharge to charge
								points1[4] = lastEnergie1;
								ch1 = processDataChannel(1, recordSet1, recordSetKey1, dataBuffer1, points1);
								recordSet1 = (RecordSet) ch1[0];
								recordSetKey1 = (String) ch1[1];
							}
							if (this.isProgrammExecuting2) { // checks for processes active includes check state change waiting to discharge to charge
								points2[4] = lastEnergie2;
								ch2 = processDataChannel(2, recordSet2, recordSetKey2, dataBuffer2, points2);
								recordSet2 = (RecordSet) ch2[0];
								recordSetKey2 = (String) ch2[1];
							}
							if (this.isProgrammExecuting3) { // checks for processes active includes check state change waiting to discharge to charge
								points3[4] = lastEnergie3;
								ch3 = processDataChannel(3, recordSet3, recordSetKey3, dataBuffer3, points3);
								recordSet3 = (RecordSet) ch3[0];
								recordSetKey3 = (String) ch3[1];
							}
							if (this.isProgrammExecuting4) { // checks for processes active includes check state change waiting to discharge to charge
								points4[4] = lastEnergie4;
								ch4 = processDataChannel(4, recordSet4, recordSetKey4, dataBuffer4, points4);
								recordSet4 = (RecordSet) ch4[0];
								recordSetKey4 = (String) ch4[1];
							}

							if (points5.length != 0 && (this.isProgrammExecuting1 || this.isProgrammExecuting2 || this.isProgrammExecuting3 || this.isProgrammExecuting4)) {
								//build combination of all the data to display it as curve compare
								points5[0] = points1[0];
								points5[1] = points2[0];
								points5[2] = points3[0];
								points5[3] = points4[0];
								points5[4] = points1[1];
								points5[5] = points2[1];
								points5[6] = points3[1];
								points5[7] = points4[1];
								points5[8] = points1[2];
								points5[9] = points2[2];
								points5[10] = points3[2];
								points5[11] = points4[2];
								points5[12] = points1[5];
								points5[13] = points2[5];
								points5[14] = points3[5];
								points5[15] = points4[5];
								points5[16] = points1[6];
								points5[17] = points2[6];
								points5[18] = points3[6];
								points5[19] = points4[6];
								
								//add system temperature using one of the processing slots
								if (this.isProgrammExecuting1)
									points5[20] = points1[7];
								else if (this.isProgrammExecuting2)
									points5[20] = points2[7];
								else if (this.isProgrammExecuting3)
									points5[20] = points3[7];
								else if (this.isProgrammExecuting4)
									points5[20] = points4[7];
								
								points5[21] = points1[8];
								points5[22] = points2[8];
								points5[23] = points3[8];
								points5[24] = points4[8];
								
								String processName = Messages.getString(MessageIds.GDE_MSGT3630);
								Channel slotChannel = this.channels.get(5);
								if (slotChannel != null) {
									// check if a record set matching for re-use is available and prepare a new if required
									if (recordSet5 == null || !recordSetKey5.contains(processName)) {
										this.application.setStatusMessage(""); //$NON-NLS-1$

										// record set does not exist or is out dated, build a new name and create
										recordSetKey5 = slotChannel.getNextRecordSetNumber() + GDE.STRING_RIGHT_PARENTHESIS_BLANK + processName;
										recordSetKey5 = recordSetKey5.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetKey5 : recordSetKey5.substring(0, RecordSet.MAX_NAME_LENGTH);

										slotChannel.put(recordSetKey5, RecordSet.createRecordSet(recordSetKey5, this.application.getActiveDevice(), slotChannel.getNumber(), true, false, true));
										if (slotChannel.getType() == ChannelTypes.TYPE_CONFIG) 
											slotChannel.applyTemplate(recordSetKey5, false);
										else
											slotChannel.applyTemplateBasics(recordSetKey5);
										MC3000GathererThread.log.logp(Level.FINE, MC3000GathererThread.$CLASS_NAME, $METHOD_NAME, recordSetKey5 + " created for channel " + slotChannel.getName()); //$NON-NLS-1$
										recordSet5 = slotChannel.get(recordSetKey5);
										recordSet5.setAllDisplayable();
										String description = recordSet5.getRecordSetDescription();
										recordSet5.setRecordSetDescription(description + GDE.LINE_SEPARATOR + this.device.getHardwareString() + GDE.STRING_BLANK + this.device.getFirmwareString());
									}
								}
								if (recordSet5 != null) recordSet5.addPoints(points5);
								
								if (recordSet5 != null && (recordSet5.get(0).realSize() < 3 || recordSet5.get(0).realSize() % 10 == 0)) {
									this.device.updateVisibilityStatus(recordSet5, true);
								}
							}
							
							MC3000GathererThread.this.application.updateAllTabs(false);
							this.application.setStatusMessage(GDE.STRING_EMPTY);
							
							//check for all processing finished and stop gathering after 15 min
							if (this.device.isProcessingStatusStandByOrFinished(dataBuffer1) && this.device.isProcessingStatusStandByOrFinished(dataBuffer2) && this.device.isProcessingStatusStandByOrFinished(dataBuffer3) && this.device.isProcessingStatusStandByOrFinished(dataBuffer4)) {
								if (0 >= (retryCounterEnd_sec -= 4)) {
									log.log(Level.INFO, "finish device activation timeout"); //$NON-NLS-1$
									this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI3601));
									stopDataGatheringThread(false, null);
								}
							}
							else {
								this.retryCounterRest_sec	= MC3000GathererThread.WAIT_TIME_RETRYS_MAX_SEC;
								this.retryCounterEnd_sec	= MC3000GathererThread.WAIT_TIME_RETRYS_END_SEC;
							}
						}
						else {
							this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI3600));
							log.logp(Level.FINE, MC3000GathererThread.$CLASS_NAME, $METHOD_NAME, "wait for device activation ..."); //$NON-NLS-1$

							if (0 >= (retryCounterRest_sec -= 1)) {
								log.log(Level.INFO, "device activation timeout"); //$NON-NLS-1$
								this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI3601));
								stopDataGatheringThread(false, null);
							}
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
						this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI3600));
						if (MC3000GathererThread.log.isLoggable(Level.FINE))
							MC3000GathererThread.log.logp(Level.FINE, MC3000GathererThread.$CLASS_NAME, $METHOD_NAME, Messages.getString(MessageIds.GDE_MSGI3600));
					}
					else if (e instanceof UsbNotClaimedException) { //USB error detected, p.e. disconnect
						stopDataGatheringThread(false, e);
					}
					else if (e instanceof UsbException) { //USB error detected, p.e. disconnect
						this.application.setStatusMessage(Messages.getString(gde.messages.MessageIds.GDE_MSGE0050));
						stopDataGatheringThread(false, e);
					}
					// program end or unexpected exception occurred, stop data gathering to enable save data by user
					else {
						MC3000GathererThread.log.log(Level.FINE, "data gathering end detected"); //$NON-NLS-1$
						stopDataGatheringThread(true, e);
					}
				}

				//force data collection every second
				lastCycleTime += 1000;
				long delay = lastCycleTime - (System.nanoTime()/1000000);
				if (delay > 0 ) WaitTimer.delay(delay);
				if (log.isLoggable(Level.TIME)) log.log(Level.TIME, String.format("delay = %d", delay)); //$NON-NLS-1$
			}
			this.application.setStatusMessage(""); //$NON-NLS-1$
			if (MC3000GathererThread.log.isLoggable(Level.FINE)) MC3000GathererThread.log.logp(Level.FINE, MC3000GathererThread.$CLASS_NAME, $METHOD_NAME, "======> exit"); //$NON-NLS-1$

			if (!this.isCollectDataStopped) {
				this.stopDataGatheringThread(false, null);
			}
		}
		finally {
			try {
				if (this.usbInterface != null) {
					this.device.usbPort.closeUsbPort(this.usbInterface);
					MC3000GathererThread.log.log(Level.FINE, "USB interface closed");
				}
			}
			catch (UsbException e) {
				MC3000GathererThread.log.log(Level.SEVERE, e.getMessage(), e);
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
		//Mode LI battery： 		0=CHARGE 1=REFRESH 2=STORAGE   3=DISCHARGE 4=CYCLE
		//Mode Ni battery:		0=CHARGE 1=REFRESH 2=BREAK_IN  3=DISCHARGE 4=CYCLE
		//Mode Zn battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
		//Mode RAM battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
		int processModeNumber = this.device.getProcessingType(dataBuffer);
		String processTypeName = this.device.getProcessingTypeName(dataBuffer);
		//STATUS:     0=standby 1=charge 2=discharge 3=resting 4=finish 0x80--0xff：error code
		String processStatusName = this.device.getProcessingStatusName(dataBuffer);
		String processBatteryType = this.device.getProcessingBatteryTypeName(dataBuffer);
		if (MC3000GathererThread.log.isLoggable(Level.FINE)) {
			MC3000GathererThread.log.log(Level.FINE, number + " : processTypeName = " + processTypeName + " - processStatusName = " + processStatusName);
		}
		Channel slotChannel = this.channels.get(number);
		if (slotChannel != null) {
			// check if a record set matching for re-use is available and prepare a new if required
			if (recordSet == null || !processRecordSetKey.contains(processTypeName) || !processRecordSetKey.contains(processStatusName)) {
				this.application.setStatusMessage(""); //$NON-NLS-1$

				// record set does not exist or is out dated, build a new name and create
				StringBuilder extend = new StringBuilder();
				if (!this.device.isContinuousRecordSet()) {
					//Mode LI battery： 		0=CHARGE 1=REFRESH 2=STORAGE   3=DISCHARGE 4=CYCLE
					//Mode Ni battery:		0=CHARGE 1=REFRESH 2=BREAK_IN  3=DISCHARGE 4=CYCLE
					//Mode Zn battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
					//Mode RAM battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
					//battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
					final int batterieType = this.device.getBatteryType(dataBuffer);
					if (((batterieType == 5 || batterieType == 7) && processModeNumber == 3) //NiZn, RAM -> Cycle
							|| (batterieType != 5 && batterieType != 7  && processModeNumber == 4)) { //LiIon, LiFe, LiHV, NiMH, NiCd, Eneloop -> CYCLE
						int cycleNumber = this.device.getCycleNumber(number, dataBuffer);

						if (cycleNumber >= 0) {
							extend.append(GDE.STRING_LEFT_BRACKET).append(processStatusName).append(GDE.STRING_COLON).append(cycleNumber).append(GDE.STRING_RIGHT_BRACKET);
						}
					}
					else if (processModeNumber == 1 || (batterieType != 5 && batterieType != 7  && processModeNumber == 2)) { //1=Refresh || Li storage || Ni break-in
						extend.append(GDE.STRING_LEFT_BRACKET).append(processStatusName).append(GDE.STRING_RIGHT_BRACKET);
					}
				}
				processRecordSetKey = String.format("%d) %s - %s %s", slotChannel.getNextRecordSetNumber(), processBatteryType, processTypeName, extend.toString());
				if (MC3000GathererThread.log.isLoggable(Level.FINE)) {
					MC3000GathererThread.log.log(Level.FINE, number + " : processRecordSetKey = " + processRecordSetKey);
				}
				processRecordSetKey = processRecordSetKey.length() <= RecordSet.MAX_NAME_LENGTH ? processRecordSetKey : processRecordSetKey.substring(0, RecordSet.MAX_NAME_LENGTH);

				slotChannel.put(processRecordSetKey, RecordSet.createRecordSet(processRecordSetKey, this.application.getActiveDevice(), slotChannel.getNumber(), true, false, true));

				if (slotChannel.getType() == ChannelTypes.TYPE_CONFIG) 
					slotChannel.applyTemplate(processRecordSetKey, false);
				else
					slotChannel.applyTemplateBasics(processRecordSetKey);

				MC3000GathererThread.log.logp(Level.FINE, MC3000GathererThread.$CLASS_NAME, $METHOD_NAME, processRecordSetKey + " created for channel " + slotChannel.getName()); //$NON-NLS-1$
				recordSet = slotChannel.get(processRecordSetKey);
				recordSet.setAllDisplayable();
				recordSet.get(5).setUnit(device.getTemperatureUnit());
				recordSet.get(7).setUnit(device.getTemperatureUnit());
				//channel.applyTemplate(recordSetKey, false);
				// switch the active record set if the current record set is child of active channel
				this.channels.switchChannel(slotChannel.getNumber(), processRecordSetKey);
				slotChannel.switchRecordSet(processRecordSetKey);
				String description = String.format("%s%s%s %s; Memory # %02d", 
						recordSet.getRecordSetDescription(), GDE.LINE_SEPARATOR, this.device.getHardwareString(), this.device.getFirmwareString(), this.device.getBatteryMemoryNumber(number, this.usbInterface)); //$NON-NLS-1$
				recordSet.setRecordSetDescription(description);
			}

			// STATUS:     0=standby 1=charge 2=discharge 3=resting 4=finish 0x80--0xff：error code
			if (this.settings.isReduceChargeDischarge() && !this.device.isContinuousRecordSet() && this.device.getProcessingStatus(dataBuffer) == 3) { //pause will not be recorded
				result[0] = recordSet;
				result[1] = processRecordSetKey;
				return result;
			}

			recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer));

			if (recordSet.get(0).realSize() < 3 || recordSet.get(0).realSize() % 10 == 0) {
				this.device.updateVisibilityStatus(recordSet, true);
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
			MC3000GathererThread.log.logp(Level.WARNING, MC3000GathererThread.$CLASS_NAME, $METHOD_NAME, throwable.getMessage(), throwable);
		}

		this.isCollectDataStopped = true;

		if (this.usbPort != null && this.usbPort.getXferErrors() > 0) {
			MC3000GathererThread.log.log(Level.WARNING, "During complete data transfer " + this.usbPort.getXferErrors() + " number of errors occured!"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (this.usbPort != null && this.usbPort.isConnected() && this.isPortOpenedByLiveGatherer == true && this.usbPort.isConnected()) {
			try {
				this.usbPort.closeUsbPort(null);
			}
			catch (UsbException e) {
				MC3000GathererThread.log.log(Level.WARNING, e.getMessage(), e);
			}
		}
//		if (this.dialog != null && !this.dialog.isDisposed()) {
//			this.dialog.checkPortStatus();
//		}

		RecordSet recordSet = this.channel.get(this.recordSetKey);
		if (recordSet != null && recordSet.getRecordDataSize(true) > 5) { // some other exception while program execution, record set has data points
			finalizeRecordSet(false);
			if (enableEndMessage) this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(MessageIds.GDE_MSGT3603));
		}
		else {
			if (throwable != null) {
				cleanup(Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { throwable.getClass().getSimpleName(), throwable.getMessage() }) + Messages.getString(MessageIds.GDE_MSGT3602));
			}
			else {
				if (enableEndMessage) cleanup(Messages.getString(gde.messages.MessageIds.GDE_MSGE0026) + Messages.getString(MessageIds.GDE_MSGT3602));
			}
		}
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 */
	void finalizeRecordSet(boolean doClosePort) {
		if (doClosePort && this.isPortOpenedByLiveGatherer && this.usbPort.isConnected()) try {
			this.usbPort.closeUsbPort(null);
		}
		catch (UsbException e) {
			MC3000GathererThread.log.log(Level.WARNING, e.getMessage(), e);
		}

		RecordSet tmpRecordSet = this.channel.get(this.recordSetKey);
		if (tmpRecordSet != null) {
			this.device.updateVisibilityStatus(tmpRecordSet, false);
			this.device.makeInActiveDisplayable(tmpRecordSet);
			this.application.updateStatisticsData();
			this.application.updateDataTable(this.recordSetKey, false);

			this.device.setAverageTimeStep_ms(tmpRecordSet.getAverageTimeStep_ms());
			MC3000GathererThread.log.log(Level.TIME, "set average time step msec = " + this.device.getAverageTimeStep_ms()); //$NON-NLS-1$
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
				this.application.openMessageDialog(MC3000GathererThread.this.dialog.getDialogShell(), message);
				//this.device.getDialog().resetButtons();
			}
			else {
				final String useRecordSetKey = this.recordSetKey;
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						MC3000GathererThread.this.application.getMenuToolBar().updateRecordSetSelectCombo();
						MC3000GathererThread.this.application.updateStatisticsData();
						MC3000GathererThread.this.application.updateDataTable(useRecordSetKey, true);
						MC3000GathererThread.this.application.openMessageDialog(MC3000GathererThread.this.dialog.getDialogShell(), message);
						//GathererThread.this.device.getDialog().resetButtons();
					}
				});
			}
		}
		else
			this.application.openMessageDialog(this.dialog.getDialogShell(), message);
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
	
	public UsbInterface getUsbInterface() {
		return this.usbInterface;
	}
}
