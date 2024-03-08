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
package gde.device.schulze;

import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.PropertyType;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.DevicePropertiesInconsistenceException;
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
public class GathererThread extends Thread {
	final static String				$CLASS_NAME									= GathererThread.class.getName();
	final static Logger				log													= Logger.getLogger(GathererThread.class.getName());
	final static int					WAIT_TIME_RETRYS						= 3600;																											// 3600 * 1 sec

	final DataExplorer				application;
	final NextGenSerialPort		serialPort;
	final NextGen8						device;
	final Channels						channels;
	final DataParserNext			parser;

	String										recordSetKey1								= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
	String										recordSetKey2								= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
	int												retryCounter								= GathererThread.WAIT_TIME_RETRYS;													//3600 * 1 sec = 60 Min
	boolean										isCollectDataStopped				= false;

	int												channelNumber;
	Channel										channel;
	int												stateNumber									= 1;
	boolean										isProgrammExecuting1				= false;
	boolean										isProgrammExecuting2				= false;
	boolean[]									isAlerted4Finish						= {false, false, false, false};
	boolean 									isContinuousRecordSet 			= Settings.getInstance().isContinuousRecordSet();

	/**
	 * data gatherer thread definition 
	 * @throws SerialPortException 
	 * @throws ApplicationConfigurationException 
	 * @throws Exception 
	 */
	public GathererThread(DataExplorer currentApplication, NextGen8 useDevice, NextGenSerialPort useSerialPort) throws ApplicationConfigurationException, SerialPortException {
		super("dataGatherer"); //$NON-NLS-1$
		this.application = currentApplication;
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.channels = Channels.getInstance();
		this.parser = new DataParserNext(this.device, this.device.getDataBlockTimeUnitFactor(), this.device.getDataBlockLeader(), this.device.getDataBlockSeparator().value(), this.device.getDataBlockCheckSumType(), 14, 0); 

		if (!this.serialPort.isConnected()) {
			this.serialPort.open();
		}
		this.setPriority(Thread.MAX_PRIORITY);
	}

	@Override
	public void run() {
		final String $METHOD_NAME = "run"; //$NON-NLS-1$
		RecordSet recordSet1 = null;
		RecordSet recordSet2 = null;
		int[] points1 = new int[this.device.getNumberOfMeasurements(1)];
		int[] points2 = new int[this.device.getNumberOfMeasurements(2)];
		byte[] dataBuffer = null;

		this.isProgrammExecuting1 = false;
		this.isProgrammExecuting2 = false;
		Object[] ch1, ch2;

		this.isCollectDataStopped = false;
		log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "====> entry initial time step ms = " + this.device.getTimeStep_ms()); //$NON-NLS-1$

		while (!this.isCollectDataStopped) {
			try {
				dataBuffer = this.serialPort.getData(); // get data from device
				this.parser.parse(new String(dataBuffer), 42);
				
				this.channelNumber = this.parser.getChannelConfigNumber();
				this.stateNumber = this.parser.getState(); 
				if (log.isLoggable(Level.INFO)) log.logp(Level.INFO, GathererThread.$CLASS_NAME, $METHOD_NAME,	device.getChannelCount() + " - data for channel = " + channelNumber + " state = " + stateNumber);

				if (this.channelNumber == 1)
					this.isProgrammExecuting1 = this.stateNumber > 0;
				else 
					this.isProgrammExecuting2 = this.stateNumber > 0;



				// check if device is ready for data capturing, discharge or charge allowed only
				// else wait for 180 seconds max. for actions
				if (this.isProgrammExecuting1 || this.isProgrammExecuting2) {
					if (this.isProgrammExecuting1) { // checks for processes active includes check state change waiting to discharge to charge
						ch1 = processDataChannel(1, recordSet1, this.recordSetKey1, dataBuffer, points1);
						recordSet1 = (RecordSet) ch1[0];
						this.recordSetKey1 = (String) ch1[1];
					}
					if (this.isProgrammExecuting2) { // checks for processes active includes check state change waiting to discharge to charge
						ch2 = processDataChannel(2, recordSet2, this.recordSetKey2, dataBuffer, points2);
						recordSet2 = (RecordSet) ch2[0];
						this.recordSetKey2 = (String) ch2[1];
					}
					
					//finalize record sets for devices with > 1 outlet channel while one is still processing
					if (!this.isProgrammExecuting1 && recordSet1 != null && recordSet1.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(recordSet1.getName());
						this.isProgrammExecuting1 = false;
						recordSet1 = null;
					}
					if (!this.isProgrammExecuting2 && recordSet2 != null && recordSet2.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(recordSet2.getName());
						this.isProgrammExecuting2 = false;
						recordSet2 = null;
					}
				}
				else { // no program is executing, wait for 180 seconds max. for actions
					this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI1702));
					log.logp(java.util.logging.Level.FINER, GathererThread.$CLASS_NAME, $METHOD_NAME, "wait for device activation"); //$NON-NLS-1$
					
					if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						log.log(java.util.logging.Level.FINE, "device activation timeout"); //$NON-NLS-1$
						this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGW1700));
						stopDataGatheringThread(false, null);
					}
					
					//finalize record sets
					if (recordSet1 != null && recordSet1.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(recordSet1.getName());
						this.isProgrammExecuting1 = false;
						recordSet1 = null;
					}
					if (recordSet2 != null && recordSet2.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(recordSet2.getName());
						this.isProgrammExecuting2 = false;
						recordSet2 = null;
					}
				}

			}
			catch (DataInconsitsentException e) {
				String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0036, new Object[] { this.getClass().getSimpleName(), $METHOD_NAME });
				if (recordSet1 != null) cleanup(recordSet1.getName(), message);
				if (recordSet2 != null) cleanup(recordSet2.getName(), message);
				this.stopDataGatheringThread(false, e);
			}
			catch (Throwable e) {
				// this case will be reached while NiXx Akku discharge/charge/discharge cycle
				if (e instanceof TimeOutException) {
					if (recordSet1 != null) {
						finalizeRecordSet(recordSet1.getName());
						recordSet1 = null;
						WaitTimer.delay(500); //assume data will be send every 500 ms per channel
					}
					if (recordSet2 != null) {
						finalizeRecordSet(recordSet2.getName());
						recordSet2 = null;
						WaitTimer.delay(500); //assume data will be send every 500 ms per channel
					}
				}
				// this case will be reached while program is started, checked and the check not asap committed, stop pressed
				else if (e instanceof TimeOutException && !(this.isProgrammExecuting1 || this.isProgrammExecuting2)) {
					this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI1702));
					log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "wait for device activation ..."); //$NON-NLS-1$
					if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						log.log(java.util.logging.Level.FINE, "device activation timeout"); //$NON-NLS-1$
						this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGW1700));
						stopDataGatheringThread(false, null);
					}
				}
				// program end or unexpected exception occurred, stop data gathering to enable save data by user
				else {
					log.log(java.util.logging.Level.FINE, "program end detected"); //$NON-NLS-1$
					stopDataGatheringThread(true, e);
				}
			}
		}
		this.application.setStatusMessage(""); //$NON-NLS-1$
		log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "======> exit"); //$NON-NLS-1$
	}

	/**
	 * process a outlet channel
	 * @param number
	 * @param recordSet
	 * @param recordSetKey
	 * @param dataBuffer
	 * @param points
	 * @return object array with updated values {recordSet, recordSetKey}
	 * @throws DataInconsitsentException
	 * @throws DevicePropertiesInconsistenceException 
	 */
	private synchronized Object[] processDataChannel(int number, RecordSet recordSet, String recordSetKey, byte[] dataBuffer, int[] points)
			throws DataInconsitsentException, DevicePropertiesInconsistenceException {
		final String $METHOD_NAME = "processOutlet"; //$NON-NLS-1$
		Object[] result = new Object[2];
		
		String processName = this.device.getRecordSetStateNameReplacement(0);
		PropertyType stateProperty = this.device.getStateProperty(this.stateNumber);
		if (stateProperty != null) 
			processName = this.device.getRecordSetStateNameReplacement(this.stateNumber);
		else 
			throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGW1702, new Object[] {this.stateNumber}));
		
		// 0=no processing 1=charge 2=discharge 3=delay 4=auto balance 5=error
		int processNumber = this.stateNumber;
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "processName = " + processName + " " + processNumber);
		}
		Channel channel = this.channels.get(number);
		if (channel != null) {
			// check if a record set matching for re-use is available and prepare a new if required
			if (recordSet == null || !recordSetKey.contains(processName)) {
				this.application.setStatusMessage(""); //$NON-NLS-1$
				setRetryCounter(GathererThread.WAIT_TIME_RETRYS); // reset to 180 sec

				// record set does not exist or is out dated, build a new name and create
				recordSetKey = channel.getNextRecordSetNumber() + GDE.STRING_RIGHT_PARENTHESIS_BLANK + (this.isContinuousRecordSet ? processName : processName);
				recordSetKey = recordSetKey.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetKey : recordSetKey.substring(0, RecordSet.MAX_NAME_LENGTH);

				channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.application.getActiveDevice(), channel.getNumber(), true, false, true));
				channel.applyTemplateBasics(recordSetKey);
				log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, recordSetKey + " created for channel " + channel.getName()); //$NON-NLS-1$
				recordSet = channel.get(recordSetKey);
				recordSet.setAllDisplayable();
				//channel.applyTemplate(recordSetKey, false);
				// switch the active record set if the current record set is child of active channel
				this.channels.switchChannel(channel.getNumber(), recordSetKey);
				channel.switchRecordSet(recordSetKey);
//				String description = recordSet.getRecordSetDescription() + GDE.LINE_SEPARATOR 
//					+ "Firmware  : " + this.device.firmware //$NON-NLS-1$
//					+ (this.device.getBatteryMemoryNumber(number, dataBuffer) >= 1 ? "; Memory #" + this.device.getBatteryMemoryNumber(number, dataBuffer) : GDE.STRING_EMPTY); //$NON-NLS-1$
//				recordSet.setRecordSetDescription(description);				
			}

			recordSet.addPoints(this.parser.getValues());

			GathererThread.this.application.updateAllTabs(false);

			if (recordSet.get(0).realSize() < 3 || recordSet.get(0).realSize() % 10 == 0) {
				this.device.updateVisibilityStatus(recordSet, true);
			}
		}
		result[0] = recordSet;
		result[1] = recordSetKey;
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
			log.logp(java.util.logging.Level.WARNING, GathererThread.$CLASS_NAME, $METHOD_NAME, throwable.getMessage(), throwable);
		}

		this.isCollectDataStopped = true;

		if (this.serialPort != null) {
			if (this.serialPort.getXferErrors() > 0) {
				log.log(java.util.logging.Level.WARNING, "During complete data transfer " + this.serialPort.getXferErrors() + " number of errors occured!"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			this.serialPort.close();
		}
	}

	/**
	 * set isDisplayable according channel configuration and calculate slope
	 */
	void finalizeRecordSet(String recordSetKey) {

		RecordSet tmpRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
		if (tmpRecordSet != null) {
			this.device.updateVisibilityStatus(tmpRecordSet, true);
			this.device.makeInActiveDisplayable(tmpRecordSet);
			this.application.updateStatisticsData();
			this.application.updateDataTable(recordSetKey, false);

			log.log(Level.TIME, "set average time step msec = " + this.device.getAverageTimeStep_ms()); //$NON-NLS-1$
		}
	}

	/**
	 * cleanup all allocated resources and display the message
	 * @param this.recordSetKey
	 * @param message
	 * @param e
	 */
	void cleanup(String recordSetKey, final String message) {
		RecordSet activeRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
		if (activeRecordSet != null) {
			activeRecordSet.clear();
			this.channels.getActiveChannel().remove(recordSetKey);
			if (Thread.currentThread().getId() == this.application.getThreadId()) {
				this.application.getMenuToolBar().updateRecordSetSelectCombo();
				this.application.updateStatisticsData();
				this.application.updateDataTable(recordSetKey, true);
				this.application.openMessageDialog(message);
			}
			else {
				final String useRecordSetKey = recordSetKey;
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						GathererThread.this.application.getMenuToolBar().updateRecordSetSelectCombo();
						GathererThread.this.application.updateStatisticsData();
						GathererThread.this.application.updateDataTable(useRecordSetKey, true);
						GathererThread.this.application.openMessageDialog(message);
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
