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
    
    Copyright (c) 2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.elprog;

import java.io.IOException;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.InputTypes;
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
 * @author brueg
 */
public class PulsarGathererThread extends Thread {
	final static String			$CLASS_NAME						= PulsarGathererThread.class.getName();
	final static Logger			log										= Logger.getLogger(PulsarGathererThread.class.getName());
	final static int				TIME_STEP_DEFAULT			= 1000;
	final static int				WAIT_TIME_RETRYS			= 3600;																										// 3600 * 1 sec

	final DataExplorer			application;
	final PulsarSerialPort	serialPort;
	final Pulsar3						device;
	final Channels					channels;
	final Channel							channel;
	final int									channelNumber;

	String									recordSetKey					= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
	int											retryCounter					= PulsarGathererThread.WAIT_TIME_RETRYS;									//3600 * 1 sec = 60 Min
	boolean									isCollectDataStopped	= false;
	boolean									isProgrammExecuting	= false;
	boolean									isPortOpenedByLiveGatherer	= false;
	boolean									isContinuousRecordSet	= Settings.getInstance().isContinuousRecordSet();
	int											lastNumberDisplayableRecords	= 0;

	/**
	 * 
	 */
	public PulsarGathererThread(DataExplorer currentApplication, Pulsar3 useDevice, PulsarSerialPort useSerialPort, int channelConfigNumber) throws ApplicationConfigurationException, SerialPortException {
		super("dataGatherer"); //$NON-NLS-1$
		this.application = currentApplication;
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.channels = Channels.getInstance();
		this.channelNumber = channelConfigNumber;
		this.channel = this.channels.get(this.channelNumber);

		if (!this.serialPort.isConnected()) {
			this.serialPort.open();
			try {
				this.serialPort.cleanInputStream();
			}
			catch (IOException e) {
				// ignore
			}
			this.isPortOpenedByLiveGatherer = true;
		}
		this.setPriority(Thread.MAX_PRIORITY);
	}

	/**
	 * thread execution method containing receive loop
	 */
	@Override
	public void run() {
		final String $METHOD_NAME = "run"; //$NON-NLS-1$
		RecordSet recordSet = null;
		int[] points = new int[this.device.getNumberOfMeasurements(this.channelNumber)];
		boolean isProgrammExecuting = false;
		long measurementCount = 0;
		final byte					startByte = (byte) this.device.getDataBlockLeader().charAt(0);
		final byte					startByteRi = '!';
		int maxBufferSize = this.device.getDataBlockSize(InputTypes.SERIAL_IO);
		byte[] tmpBuffer = null;
		byte[] dataBuffer = new byte[158];
		byte[] dataBufferRi = new byte[64];
		String processName = GDE.STRING_EMPTY;
		long lastCycleTime = 0;

		this.isCollectDataStopped = false;
		log.logp(Level.FINE, PulsarGathererThread.$CLASS_NAME, $METHOD_NAME, "====> entry initial time step ms = " + this.device.getTimeStep_ms()); //$NON-NLS-1$
		lastCycleTime = System.nanoTime()/1000000;
		while (!this.isCollectDataStopped) {
			try {
				// get data from device
				tmpBuffer = this.serialPort.getData();
				log.log(Level.INFO, startByte + " " + new String(tmpBuffer));
				
				if (tmpBuffer.length <= maxBufferSize) {
					//find line of Ri starting with ! only while more than 160 bytes received
					int index = 0;
					while (tmpBuffer.length > 160 && index < tmpBuffer.length && tmpBuffer[index] != startByteRi)
						++index;
					if (index > 159 && tmpBuffer[index] == startByteRi && (tmpBuffer.length - index) == 66) { //! line with Ri values exist
						System.arraycopy(tmpBuffer, index, dataBufferRi, 0, dataBufferRi.length);
						log.log(Level.INFO, startByteRi + " " + new String(dataBufferRi));
					} 
				}
				System.arraycopy(tmpBuffer, 0, dataBuffer, 0, dataBuffer.length);

				// check if device is ready for data capturing
				// device sends at the end of transmission $ENDBULK;64
				try {
						try {
							processName = this.device.getProcessName(dataBuffer);
							isProgrammExecuting = this.device.isProcessing(dataBuffer);
							if (log.isLoggable(Level.FINE))
								log.log(Level.FINE, String.format("isProgramExecuting = %s, processName = %s", isProgrammExecuting, processName));
						}
						catch (Exception e) {
							log.log(Level.WARNING, String .format("Error in getProcessName evaluating '%s'", StringHelper.byte2CharString(dataBuffer, dataBuffer.length)));
							continue;
						}
					if (log.isLoggable(Level.FINE))
						log.logp(Level.FINE, PulsarGathererThread.$CLASS_NAME, $METHOD_NAME, "processing mode = " + processName); //$NON-NLS-1$
				}
				catch (final Exception e) {
					StringBuilder sb = new StringBuilder();
					for (byte b : dataBuffer) {
						sb.append((char) b);
					}
					while (sb.length() > 5 && (sb.charAt(sb.length() - 1) == '\n' || sb.charAt(sb.length() - 1) == '\r'))
						sb.deleteCharAt(sb.length() - 1);
					log.log(Level.WARNING, sb.toString());
					log.log(Level.WARNING, e.getMessage(), e);
					continue;
				}

				if (isProgrammExecuting) {
					// check state change waiting to discharge to charge
					// check if a record set matching for re-use is available and prepare a new if required
					if (this.channel.size() == 0 || recordSet == null || (processName.length() > 3 && !this.recordSetKey.endsWith(" " + processName))) { //$NON-NLS-1$
						this.application.setStatusMessage(GDE.STRING_EMPTY);
						setRetryCounter(PulsarGathererThread.WAIT_TIME_RETRYS); // 120 * 2500 = 3000 sec
						// record set does not exist or is outdated, build a new name and create
						this.recordSetKey = this.channel.getNextRecordSetNumber() + ") " + processName; //$NON-NLS-1$
						this.channel.put(this.recordSetKey, RecordSet.createRecordSet(this.recordSetKey, this.application.getActiveDevice(), this.channel.getNumber(), true, false, true));
						if (log.isLoggable(Level.FINE))
							log.logp(Level.FINE, PulsarGathererThread.$CLASS_NAME, $METHOD_NAME, this.recordSetKey + " created for channel " + this.channel.getName()); //$NON-NLS-1$
						if (this.channel.getActiveRecordSet() == null) this.channel.setActiveRecordSet(this.recordSetKey);
						recordSet = this.channel.get(this.recordSetKey);
						this.channel.applyTemplateBasics(this.recordSetKey);
						// switch the active record set if the current record set is child of active channel
						// for iCharge its always the case since we have only one channel
						if (this.channel.getName().equals(this.channels.getActiveChannel().getName())) {
							this.channels.getActiveChannel().switchRecordSet(this.recordSetKey);
						}
						measurementCount = 0;
					}

					if (measurementCount > 0) {// prepare the data for adding to record set
						if (dataBufferRi != null && dataBufferRi[0] == startByteRi)
							points = this.device.convertDataBytes(points, dataBufferRi);
					 recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer));//constant time step
					}
					
					++measurementCount;

					if (recordSet != null) {
						if (measurementCount > 0 && measurementCount % 2 == 0) {
							this.device.updateVisibilityStatus(recordSet, true);
							recordSet.syncScaleOfSyncableRecords();
						}
						RecordSet activeRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
						if (activeRecordSet != null && recordSet.size() > 0 && recordSet.isChildOfActiveChannel() && recordSet.equals(activeRecordSet)) {
							PulsarGathererThread.this.application.updateAllTabs(false, this.lastNumberDisplayableRecords != recordSet.getConfiguredDisplayable());
							this.lastNumberDisplayableRecords = recordSet.getConfiguredDisplayable();
						}
					}
					this.application.setStatusMessage(GDE.STRING_EMPTY);
				}
				else { // no program is executing, wait for 180 seconds max. for actions
					this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI3900));
					if (log.isLoggable(Level.FINE))
						log.logp(Level.FINE, PulsarGathererThread.$CLASS_NAME, $METHOD_NAME, "wait for device activation"); //$NON-NLS-1$

					if (recordSet != null && recordSet.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(false);
						isProgrammExecuting = false;
						recordSet = null;
						setRetryCounter(PulsarGathererThread.WAIT_TIME_RETRYS); // 120 * 2500 = 3000 sec
						this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGT3907));
					}
					else if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "iCharge activation timeout"); //$NON-NLS-1$
						this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGW3900));
						stopDataGatheringThread(false, null);
					}
				}
			}
			catch (DataInconsitsentException e) {
				String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0036, new Object[] { this.getClass().getSimpleName(), $METHOD_NAME });
				cleanup(message);
			}
			catch (Throwable e) {
				// this case will be reached while NiXx Akku discharge/charge/discharge cycle
				if (e instanceof TimeOutException && isProgrammExecuting) {
					//tolerate timeout, just update status message
					this.application.setStatusMessage(">>>> serial port timeout <<<<");
				}
				// this case will be reached while program is started, checked and the check not asap committed, stop pressed
				else if (e instanceof TimeOutException && !isProgrammExecuting) {
					this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI3900));
					if (log.isLoggable(Level.FINE))
						log.logp(Level.FINE, PulsarGathererThread.$CLASS_NAME, $METHOD_NAME, "wait for device activation ..."); //$NON-NLS-1$
					if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "device activation timeout"); //$NON-NLS-1$
						this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGW3900));
						stopDataGatheringThread(false, null);
					}
				}
				// program end or unexpected exception occurred, stop data gathering to enable save data by user
				else {
					log.log(Level.SEVERE, e.getMessage(), e);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "program end detected"); //$NON-NLS-1$
					stopDataGatheringThread(true, e);
				}
			}
			//force data collection every second
			lastCycleTime += 5000;
			long delay = lastCycleTime - (System.nanoTime()/1000000);
			if (delay > 0 ) WaitTimer.delay(delay);
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("delay = %d", delay)); //$NON-NLS-1$
		}
		this.application.setStatusMessage(GDE.STRING_EMPTY);
		if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, PulsarGathererThread.$CLASS_NAME, $METHOD_NAME, "======> exit"); //$NON-NLS-1$
	}

	/**
	 * stop the data gathering and check if reasonable data in record set to finalize or clear
	 * @param enableEndMessage
	 * @param throwable
	 */
	void stopDataGatheringThread(boolean enableEndMessage, Throwable throwable) {
		final String $METHOD_NAME = "stopDataGatheringThread"; //$NON-NLS-1$

		if (throwable != null) {
			log.logp(Level.WARNING, PulsarGathererThread.$CLASS_NAME, $METHOD_NAME, throwable.getMessage(), throwable);
		}

		this.isCollectDataStopped = true;

		if (this.serialPort != null && this.serialPort.getXferErrors() > 0) {
			log.log(Level.WARNING, "During complete data transfer " + this.serialPort.getXferErrors() + " number of errors occured!"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (this.serialPort != null && this.serialPort.isConnected() && this.isPortOpenedByLiveGatherer == true && this.serialPort.isConnected()) {
			this.serialPort.close();
		}
		log.log(Level.INFO, $METHOD_NAME+" - this.isCollectDataStopped=" + this.isCollectDataStopped);
		log.log(Level.INFO, $METHOD_NAME+" - this.serialPortisConnected=" + this.serialPort.isConnected());

		RecordSet recordSet = this.channel.get(this.recordSetKey);
		log.log(Level.INFO, $METHOD_NAME+" - " + (recordSet != null && recordSet.getRecordDataSize(true) > 5));
		if (recordSet != null && recordSet.getRecordDataSize(true) > 5) { // some other exception while program execution, record set has data points
			finalizeRecordSet(false);
			if (enableEndMessage) this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGT3907));
		}
		else {
			if (throwable != null) {
				cleanup(Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { throwable.getClass().getSimpleName(), throwable.getMessage() }) + Messages.getString(MessageIds.GDE_MSGT3908));
			}
			else {
				if (enableEndMessage) cleanup(Messages.getString(gde.messages.MessageIds.GDE_MSGE0026) + Messages.getString(MessageIds.GDE_MSGT3908));
			}
		}
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 */
	void finalizeRecordSet(boolean doClosePort) {
		if (doClosePort && this.isPortOpenedByLiveGatherer && this.serialPort.isConnected()) this.serialPort.close();

		RecordSet tmpRecordSet = this.channel.get(this.recordSetKey);
		if (tmpRecordSet != null) {
			this.device.updateVisibilityStatus(tmpRecordSet, true);
			this.device.makeInActiveDisplayable(tmpRecordSet);
			this.application.updateStatisticsData();
			this.application.updateDataTable(this.recordSetKey, false);

			this.device.setAverageTimeStep_ms(tmpRecordSet.getAverageTimeStep_ms());
			log.log(Level.TIME, "set average time step msec = " + this.device.getAverageTimeStep_ms()); //$NON-NLS-1$
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
			}
			else {
				final String useRecordSetKey = this.recordSetKey;
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						PulsarGathererThread.this.application.getMenuToolBar().updateRecordSetSelectCombo();
						PulsarGathererThread.this.application.updateStatisticsData();
						PulsarGathererThread.this.application.updateDataTable(useRecordSetKey, true);
						PulsarGathererThread.this.application.openMessageDialog(message);
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

	/**
	 * @return the retryCounter
	 */
	int getRetryCounter() {
		return this.retryCounter;
	}

	/**
	 * @param newRetryCounter the retryCounter to set
	 */
	int setRetryCounter(int newRetryCounter) {
		return this.retryCounter = newRetryCounter;
	}

}
