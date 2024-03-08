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
    
    Copyright (c) 2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.skyrc;

import java.util.logging.Logger;

import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.usb.UsbNotClaimedException;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.ChannelTypes;
import gde.device.DeviceDialog;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.WaitTimer;

/**
 * Thread implementation to gather data from eStation device
 * @author Winfied Brügmann
 */
public class HitecX1RedGathererThread extends Thread {
	protected static final int	USB_QUERY_DELAY	= GDE.IS_WINDOWS ? 70 : 160;
	final static String	$CLASS_NAME									= HitecX1RedGathererThread.class.getName();
	final static Logger	log													= Logger.getLogger(HitecX1RedGathererThread.class.getName());
	final static int		WAIT_TIME_RETRYS						= 3600;		// 3600 * 1 sec = 60 Minutes

	final DataExplorer			application;
	final Settings					settings;
	final IMaxB6RDX1UsbPort	usbPort;
	final HitecX1Red				device;
	final DeviceDialog			dialog;
	final Channels					channels;
	final Channel						channel;
	final int								channelNumber;

	String							recordSetKey								= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272);
	boolean							isPortOpenedByLiveGatherer	= false;
	boolean							isGatheredRecordSetVisible	= true;
	boolean							isCollectDataStopped				= false;
	UsbInterface				usbInterface								= null;
	boolean							isProgrammExecuting1				= false;
	boolean[]						isAlerted4Finish						= { false, false, false, false };
	int									retryCounter								= HitecX1RedGathererThread.WAIT_TIME_RETRYS;	//60 Min

	/**
	 * data gatherer thread definition 
	 * @throws SerialPortException 
	 * @throws ApplicationConfigurationException 
	 * @throws UsbException 
	 * @throws Exception 
	 */
	public HitecX1RedGathererThread(DataExplorer currentApplication, HitecX1Red useDevice, IMaxB6RDX1UsbPort useSerialPort, int channelConfigNumber, DeviceDialog useDialog) throws ApplicationConfigurationException,
			UsbDisconnectedException, UsbException {
		super("dataGatherer");
		this.application = currentApplication;
		this.settings = Settings.getInstance();
		this.device = useDevice;
		this.device.resetEnergy		= new int[] { 5, 5, 5, 5 };
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
			int[] points1 = new int[this.device.getNumberOfMeasurements(1)];
			int lastEnergie1;

			this.isProgrammExecuting1 = false;

			long lastCycleTime = 0;
			byte[] dataBuffer1 = null;
			byte[] channelBuffer1 = null;
			Object[] ch1;
			String recordSetKey1 = Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization

			this.isCollectDataStopped = false;
			if (HitecX1RedGathererThread.log.isLoggable(Level.FINE))
				HitecX1RedGathererThread.log.logp(Level.FINE, HitecX1RedGathererThread.$CLASS_NAME, $METHOD_NAME, "====> entry initial time step ms = " + this.device.getTimeStep_ms()); //$NON-NLS-1$

			lastCycleTime = System.nanoTime()/1000000;
			while (!this.isCollectDataStopped && this.usbPort.isConnected()) {
				try {
					if (this.application != null) this.application.setSerialTxOn();
					if (this.application != null) this.application.setSerialRxOn();
					//get data from device for all4 slots
					if (this.usbPort.isConnected()) 
						if (this.isProgrammExecuting1) 	{
							dataBuffer1 = this.usbPort.getData(this.usbInterface, IMaxB6RDX1UsbPort.QueryOperationData.CHANNEL_A.value());
							this.isProgrammExecuting1 = this.device.isProcessing(1, channelBuffer1, dataBuffer1);
						}
						else {
							dataBuffer1 = null;
							channelBuffer1 = this.usbPort.getData(this.usbInterface, IMaxB6RDX1UsbPort.QueryChannelData.CHANNEL_A.value());
							this.isProgrammExecuting1 = this.device.isProcessing(1, channelBuffer1, dataBuffer1);
						}
					WaitTimer.delay(USB_QUERY_DELAY);
					if (this.application != null) this.application.setSerialTxOff();

					WaitTimer.delay(USB_QUERY_DELAY);
					if (this.application != null) this.application.setSerialRxOff();

					// check if device is ready for data capturing, discharge or charge allowed only
					if (this.isProgrammExecuting1) {
						lastEnergie1 = points1[4];
						points1 = new int[this.device.getNumberOfMeasurements(1)];

						if (this.isProgrammExecuting1 && channelBuffer1 != null && dataBuffer1 != null) { // checks for processes active includes check state change waiting to discharge to charge
							points1[4] = lastEnergie1;
							ch1 = processDataChannel(1, recordSet1, recordSetKey1, channelBuffer1, dataBuffer1, points1);
							recordSet1 = (RecordSet) ch1[0];
							recordSetKey1 = (String) ch1[1];
						}
						
						this.application.setStatusMessage(GDE.STRING_EMPTY);						
					}
					else {
						this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI3600));
						log.logp(Level.FINE, HitecX1RedGathererThread.$CLASS_NAME, $METHOD_NAME, "wait for device activation ..."); //$NON-NLS-1$

						if (0 >= (retryCounter -= 1)) {
							log.log(Level.FINE, "device activation timeout"); //$NON-NLS-1$
							this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI3601));
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
						this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI3600));
						if (HitecX1RedGathererThread.log.isLoggable(Level.FINE))
							HitecX1RedGathererThread.log.logp(Level.FINE, HitecX1RedGathererThread.$CLASS_NAME, $METHOD_NAME, Messages.getString(MessageIds.GDE_MSGI3600));
					}
					else if (e instanceof UsbNotClaimedException) { //USB error detected, p.e. disconnect
						this.application.setStatusMessage(Messages.getString(gde.messages.MessageIds.GDE_MSGE0051, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
						stopDataGatheringThread(false, e);
					}
					else if (e instanceof UsbDisconnectedException) { //USB error detected, p.e. disconnect
						this.application.setStatusMessage(Messages.getString(gde.messages.MessageIds.GDE_MSGE0051, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
						stopDataGatheringThread(false, e);
					}
					else if (e instanceof UsbException) { //USB error detected, p.e. disconnect
						this.application.setStatusMessage(Messages.getString(gde.messages.MessageIds.GDE_MSGE0050));
						stopDataGatheringThread(false, e);
					}
					// program end or unexpected exception occurred, stop data gathering to enable save data by user
					else {
						HitecX1RedGathererThread.log.log(Level.FINE, "data gathering end detected"); //$NON-NLS-1$
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
			if (HitecX1RedGathererThread.log.isLoggable(Level.FINE)) HitecX1RedGathererThread.log.logp(Level.FINE, HitecX1RedGathererThread.$CLASS_NAME, $METHOD_NAME, "======> exit"); //$NON-NLS-1$

			if (!this.isCollectDataStopped) {
				this.stopDataGatheringThread(false, null);
			}
		}
		finally {
			try {
				if (this.usbInterface != null) {
					this.device.usbPort.closeUsbPort(this.usbInterface);
					HitecX1RedGathererThread.log.log(Level.FINE, "USB interface closed");
				}
			}
			catch (UsbException e) {
				HitecX1RedGathererThread.log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * process a outlet channel
	 * @param number
	 * @param recordSet
	 * @param processRecordSetKey
	 * @param channelBuffer contains battery type and processing type
	 * @param dataBuffer
	 * @param points
	 * @return object array with updated values {recordSet, recordSetKey}
	 * @throws DataInconsitsentException
	 */
	private Object[] processDataChannel(final int number, RecordSet recordSet, String processRecordSetKey, final byte[] channelBuffer, final byte[] dataBuffer, final int[] points) throws DataInconsitsentException {
		final String $METHOD_NAME = "processOutlet"; //$NON-NLS-1$
		Object[] result = new Object[2];
		//LI battery： 	0=BALACE-CHARGE 1=CHARGE 2=DISCHARGE 3=STORAGE 4=FAST-CHARGE
		//Ni battery:		0=CHARGE 1=AUTO_CHARGE 2=DISCHARGE 3=RE_PEAK 4=CYCLE
		//Pb battery:		0=CHARGE 1=DISCHARGE
		String processTypeName = this.device.getProcessingTypeName(channelBuffer);
		String processSubTypeName = this.device.getProcessSubTypeName(channelBuffer, dataBuffer); //Ni cycle
		String processBatteryTypeName = this.device.getProcessingBatteryTypeName(channelBuffer);

		//firmware 1.07 energy needs to be calculated
		int processSubType = device.getProcessSubType(channelBuffer, dataBuffer);
		if (this.device.resetEnergy[number-1] != processSubType || processSubType == 2)
			if (processSubType == 2)
				dataBuffer[0] = -1; //keep energy
			else
				dataBuffer[0] = 1;  //reset energy
		else
			dataBuffer[0] = 0;	//add up energy		
		this.device.resetEnergy[number-1] = processSubType;

		if (HitecX1RedGathererThread.log.isLoggable(Level.FINE)) 
			HitecX1RedGathererThread.log.log(Level.FINE, String.format("process = %s ; subProcess = %s", processTypeName, processSubTypeName));
		
		Channel channel = this.channels.get(number);
		if (channel != null) {
			// check if a record set matching for re-use is available and prepare a new if required
			if (recordSet == null || !processRecordSetKey.contains(processTypeName) || !processRecordSetKey.contains(processSubTypeName)) {
				this.application.setStatusMessage(GDE.STRING_EMPTY); 

				// record set does not exist or is out dated, build a new name and create
				StringBuilder extend = new StringBuilder();
				if (!this.device.isContinuousRecordSet()) {
					//LI battery： 	0=BALACE-CHARGE 1=CHARGE 2=DISCHARGE 3=STORAGE 4=FAST-CHARGE
					//Ni battery:		0=CHARGE 1=AUTO_CHARGE 2=DISCHARGE 3=RE_PEAK 4=CYCLE
					//Pb battery:		0=CHARGE 1=DISCHARGE
					//battery type:  0:LiPo 1:LiFe 2:LiIo 3:LiHv 4:NiMH 5:NiCd 6:PB
					final int batterieType = this.device.getBatteryType(channelBuffer);
					final int processingType = this.device.getProcessingType(channelBuffer);
					if ((batterieType == 4 || batterieType == 5) && processingType == 4) { //NiMH, NiCd -> Cycle
						int cycleNumber = this.device.getCycleNumber(dataBuffer);

						if (cycleNumber >= 0) {
							extend.append(GDE.STRING_LEFT_BRACKET).append(processSubTypeName).append(GDE.STRING_COLON).append(cycleNumber).append(GDE.STRING_RIGHT_BRACKET);
						}
					}
				}
				processRecordSetKey = String.format("%d) %s - %s %s", channel.getNextRecordSetNumber(), processBatteryTypeName, processTypeName, extend.toString());
				if (HitecX1RedGathererThread.log.isLoggable(Level.FINE)) {
					HitecX1RedGathererThread.log.log(Level.FINE, number + " : processRecordSetKey = " + processRecordSetKey);
				}
				processRecordSetKey = processRecordSetKey.length() <= RecordSet.MAX_NAME_LENGTH ? processRecordSetKey : processRecordSetKey.substring(0, RecordSet.MAX_NAME_LENGTH);

				channel.put(processRecordSetKey, RecordSet.createRecordSet(processRecordSetKey, this.application.getActiveDevice(), channel.getNumber(), true, false, true));

				if (channel.getType() == ChannelTypes.TYPE_CONFIG) 
					channel.applyTemplate(processRecordSetKey, false);
				else
					channel.applyTemplateBasics(processRecordSetKey);

				if (log.isLoggable(Level.FINE))
					log.logp(Level.FINE, HitecX1RedGathererThread.$CLASS_NAME, $METHOD_NAME, processRecordSetKey + " created for channel " + channel.getName()); //$NON-NLS-1$
				recordSet = channel.get(processRecordSetKey);
				recordSet.setAllDisplayable();
//				recordSet.get(5).setUnit(device.getTemperatureUnit(number));
//				recordSet.get(6).setUnit(device.getTemperatureUnit(number));
				//channel.applyTemplate(recordSetKey, false);
				// switch the active record set if the current record set is child of active channel
				this.channels.switchChannel(channel.getNumber(), processRecordSetKey);
				channel.switchRecordSet(processRecordSetKey);
				String description = String.format("%s%s%s %s;", 
						recordSet.getRecordSetDescription(), GDE.LINE_SEPARATOR, this.device.getHarwareString(), this.device.getFirmwareString()); //$NON-NLS-1$
				recordSet.setRecordSetDescription(description);

				//firmware 1.09 energy needs to be calculated -> creating a new record set will reset energy
				dataBuffer[0] = 1;  //reset energy
			}

			recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer));

			HitecX1RedGathererThread.this.application.updateAllTabs(false);

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
			HitecX1RedGathererThread.log.logp(Level.WARNING, HitecX1RedGathererThread.$CLASS_NAME, $METHOD_NAME, throwable.getMessage(), throwable);
		}

		this.isCollectDataStopped = true;

		if (this.usbPort != null && this.usbPort.getXferErrors() > 0) {
			HitecX1RedGathererThread.log.log(Level.WARNING, "During complete data transfer " + this.usbPort.getXferErrors() + " number of errors occured!"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (this.usbPort != null && this.usbPort.isConnected() && this.isPortOpenedByLiveGatherer == true && this.usbPort.isConnected()) {
			try {
				this.usbPort.closeUsbPort(null);
			}
			catch (UsbException e) {
				HitecX1RedGathererThread.log.log(Level.WARNING, e.getMessage(), e);
			}
		}
//		if (this.dialog != null && !this.dialog.isDisposed()) {
//			this.dialog.checkPortStatus();
//		}

		RecordSet recordSet = this.channel.get(this.recordSetKey);
		if (recordSet != null && recordSet.getRecordDataSize(true) > 5) { // some other exception while program execution, record set has data points
			finalizeRecordSet(false);
			if (enableEndMessage) this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGT3603));
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
			HitecX1RedGathererThread.log.log(Level.WARNING, e.getMessage(), e);
		}

		RecordSet tmpRecordSet = this.channel.get(this.recordSetKey);
		if (tmpRecordSet != null) {
			this.device.updateVisibilityStatus(tmpRecordSet, false);
			this.device.makeInActiveDisplayable(tmpRecordSet);
			this.application.updateStatisticsData();
			this.application.updateDataTable(this.recordSetKey, false);

			this.device.setAverageTimeStep_ms(tmpRecordSet.getAverageTimeStep_ms());
			HitecX1RedGathererThread.log.log(Level.TIME, "set average time step msec = " + this.device.getAverageTimeStep_ms()); //$NON-NLS-1$
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
				this.application.openMessageDialogAsync(message);
				//this.device.getDialog().resetButtons();
			}
			else {
				final String useRecordSetKey = this.recordSetKey;
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						HitecX1RedGathererThread.this.application.getMenuToolBar().updateRecordSetSelectCombo();
						HitecX1RedGathererThread.this.application.updateStatisticsData();
						HitecX1RedGathererThread.this.application.updateDataTable(useRecordSetKey, true);
						HitecX1RedGathererThread.this.application.openMessageDialogAsync(message);
						//GathererThread.this.device.getDialog().resetButtons();
					}
				});
			}
		}
		else
			this.application.openMessageDialogAsync(message);
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
