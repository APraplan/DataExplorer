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
package gde.device.conrad;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.util.HashMap;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;


/**
 * Thread implementation to gather data from UniLog device
 * @author Winfied Brügmann
 */
public class GathererThread extends Thread {

	final static String				$CLASS_NAME									= GathererThread.class.getName();
	final static Logger				log													= Logger.getLogger(GathererThread.class.getName());

	DataExplorer		application;
	final VC800SerialPort			serialPort;
	final VC800								device;
	final VC800Dialog					dialog;
	final Channels						channels;
	final Channel							channel;
	final int									channelNumber;
	String										recordSetKey								= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272);
	boolean										isPortOpenedByLiveGatherer	= false;
	boolean										isSwitchedRecordSet					= false;
	boolean										isGatheredRecordSetVisible	= true;

	final static int					WAIT_TIME_RETRYS						= 600;
	int												retryCounter								= GathererThread.WAIT_TIME_RETRYS;	// 600 * 1 sec timeout = 600 sec
	final long								cycleTime_ms;
	boolean										isCollectDataStopped				= false;

	/**
	 * data gathere thread definition 
	 * @throws SerialPortException 
	 * @throws ApplicationConfigurationException 
	 * @throws Exception 
	 */
	public GathererThread(DataExplorer currentApplication, VC800 useDevice, VC800SerialPort useSerialPort, int channelConfigNumber, VC800Dialog useDialog)
			throws ApplicationConfigurationException, SerialPortException {
		super("liveDataGatherer");
		this.application = currentApplication;
		this.device = useDevice;
		this.dialog = useDialog;
		this.serialPort = useSerialPort;
		this.channels = Channels.getInstance();
		this.channelNumber = channelConfigNumber;
		this.channel = this.channels.get(this.channelNumber);
		this.recordSetKey = this.device.getRecordSetStemNameReplacement();

		if (!this.serialPort.isConnected()) {
			this.serialPort.open();
			this.isPortOpenedByLiveGatherer = true;
		}
		this.setPriority(Thread.MAX_PRIORITY);
		
		this.cycleTime_ms = (long) this.device.getTimeStep_ms();
	}

	@Override
	public void run() {
		final String $METHOD_NAME = "run"; //$NON-NLS-1$

		RecordSet recordSet = null;
		int[] points = new int[this.device.getNumberOfMeasurements(this.channelNumber)];
		final HashMap<String, String>	configData = this.dialog.getConfigData();
		long startCycleTime = 0;
		long tmpCycleTime = 0;
		long measurementCount = 0;
		byte[] dataBuffer = null;
		String m_unit = "", old_unit = ""; //$NON-NLS-1$ //$NON-NLS-2$

		this.isCollectDataStopped = false;
		log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "====> entry " + "initial time step ms = " + this.device.getTimeStep_ms()); //$NON-NLS-1$ //$NON-NLS-2$
		this.application.setStatusMessage(""); //$NON-NLS-1$

		try {
			this.serialPort.getData();
		}
		catch (Exception e1) {
			// ignore here
		}
		long delayTime, lastTimeStamp = 0;
		while (!this.isCollectDataStopped) {
			try {
				// get data from device
				dataBuffer = this.serialPort.getData();

				this.device.getMeasurementInfo(dataBuffer, configData);
				m_unit = configData.get(VC800.INPUT_UNIT);
				String processName = this.device.getMode(dataBuffer);
				log.log(Level.FINE, "unit = " + m_unit + " mode = " + processName); //$NON-NLS-1$ //$NON-NLS-2$
				
				if (m_unit.length() > 0) {
					// check if a record set matching for re-use is available and prepare a new if required
					if (this.channel.size() == 0 || recordSet == null || !this.recordSetKey.endsWith(" " + processName) || !m_unit.equals(old_unit)) { //$NON-NLS-1$
						this.application.setStatusMessage(""); //$NON-NLS-1$
						setRetryCounter(GathererThread.WAIT_TIME_RETRYS); // 600 * receive timeout sec timeout  600 sec
						// record set does not exist or is outdated, build a new name and create
						this.recordSetKey = this.channel.getNextRecordSetNumber() + ") " + processName; //$NON-NLS-1$
						this.channel.put(this.recordSetKey, RecordSet.createRecordSet(this.recordSetKey, this.application.getActiveDevice(), channel.getNumber(), true, false, true));
						this.channel.applyTemplateBasics(this.recordSetKey);
						log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, this.recordSetKey + " created for channel " + this.channel.getName()); //$NON-NLS-1$
						if (this.channel.getActiveRecordSet() == null) this.channel.setActiveRecordSet(this.recordSetKey);
						recordSet = this.channel.get(this.recordSetKey);
						recordSet.setAllDisplayable();
						Record record = recordSet.get(recordSet.getFirstRecordName());
						record.setUnit(m_unit);
						record.setSymbol(configData.get(VC800.INPUT_SYMBOL));
						record.setName(configData.get(VC800.INPUT_TYPE));
						this.channel.applyTemplate(this.recordSetKey, false);
						// switch the active record set if the current record set is child of active channel
						// for eStation its always the case since we have only one channel
						if (this.channel.getName().equals(this.channels.getActiveChannel().getName())) {
							this.channels.getActiveChannel().switchRecordSet(this.recordSetKey);
						}
						recordSet.updateVisibleAndDisplayableRecordsForTable();
						measurementCount = 0;
						old_unit = m_unit;
					}
					// prepare the data for adding to record set
					tmpCycleTime = System.nanoTime()/1000000;
					if (measurementCount == 0) {
						measurementCount++;
						startCycleTime = tmpCycleTime;
					}
					
					if (cycleTime_ms > 1000 || lastTimeStamp == 0) {
						if ((tmpCycleTime - lastTimeStamp) > (cycleTime_ms-150)) {
							recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer), (tmpCycleTime - startCycleTime));
							
							delayTime = lastTimeStamp == 0 ? 0 : (tmpCycleTime - startCycleTime)-(measurementCount++ * cycleTime_ms);
							log.logp(Level.TIME, $CLASS_NAME, $METHOD_NAME, "deltaTime = " + (lastTimeStamp == 0 ? 0 : ((tmpCycleTime - startCycleTime)/(measurementCount-1))) + ", delayTime = " + delayTime);
							lastTimeStamp = tmpCycleTime - delayTime;
						}
					}
					else {
						recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer), (tmpCycleTime - startCycleTime));
					}

					if (recordSet.isChildOfActiveChannel() && recordSet.equals(this.channels.getActiveChannel().getActiveRecordSet())) {
						GathererThread.this.application.updateAllTabs(false);
					}
				}
			}
			catch (DataInconsitsentException e) {
				log.log(Level.SEVERE, Messages.getString(gde.messages.MessageIds.GDE_MSGE0036, new Object[] { this.getClass().getSimpleName(), $METHOD_NAME }));
				postCleanup();
			}
			catch (Throwable e) {
				log.logp(Level.WARNING, GathererThread.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
				if (e instanceof TimeOutException) {
					this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI1500));
					log.logp(Level.WARNING, GathererThread.$CLASS_NAME, $METHOD_NAME, "wait for device activation ... " + getRetryCounter()); //$NON-NLS-1$
					if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						log.log(Level.WARNING, "device activation timeout", e); //$NON-NLS-1$
						this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI1501), SWT.COLOR_RED);
						stopDataGatheringThread(false);
					}
				}
				// program end or unexpected exception occurred, stop data gathering to enable save data by user
				else {
					stopDataGatheringThread(true);
				}
			}
		}
		configData.clear();
		log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "======> exit"); //$NON-NLS-1$
	}

	/**
	 * stop the data gathering and check if reasonable data in record set to finalize or clear
	 * @param enableEndMessage
	 */
	void stopDataGatheringThread(boolean enableEndMessage) {
		this.isCollectDataStopped = true;

		if (this.serialPort != null && this.serialPort.getXferErrors() > 0) {
			log.log(Level.WARNING, "During complete data transfer " + this.serialPort.getXferErrors() + " number of errors occured!"); //$NON-NLS-1$ //$NON-NLS-2$
			this.application.setStatusMessage(Messages.getString(gde.messages.MessageIds.GDE_MSGE0026), SWT.COLOR_RED);						
		}
		if (this.serialPort != null && this.serialPort.isConnected() && this.isPortOpenedByLiveGatherer == true && this.serialPort.isConnected()) {
			this.serialPort.close();
		}

		RecordSet recordSet = this.channel.get(this.recordSetKey);
		if (recordSet != null && recordSet.getRecordDataSize(true) > 5) { // some other exception while program execution, record set has data points
			finalizeRecordSet(false);
			if (enableEndMessage) this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(MessageIds.GDE_MSGI1501));
		}
		else {
			postCleanup();
		}
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 */
	void finalizeRecordSet(boolean doClosePort) {
		if (doClosePort && this.isPortOpenedByLiveGatherer && this.serialPort.isConnected()) this.serialPort.close();

		RecordSet tmpRecordSet = this.channel.get(this.recordSetKey);
		if (tmpRecordSet != null) {
			this.device.updateVisibilityStatus(tmpRecordSet, false);
			this.device.makeInActiveDisplayable(tmpRecordSet);
			this.application.updateStatisticsData();
			this.application.updateDataTable(this.recordSetKey, false);
		}
	}

	/**
	 * cleanup all allocated resources and display the message
	 */
	void postCleanup() {
		if (this.channel.get(this.recordSetKey) != null) {
			this.channel.get(this.recordSetKey).clear();
			this.channel.remove(this.recordSetKey);
			if (Thread.currentThread().getId() == this.application.getThreadId()) {
				this.application.getMenuToolBar().updateRecordSetSelectCombo();
				this.application.updateStatisticsData();
				this.application.updateDataTable(this.recordSetKey, true);
				this.device.getDialog().resetButtons();
			}
			else {
				final String useRecordSetKey = this.recordSetKey;
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						GathererThread.this.application.getMenuToolBar().updateRecordSetSelectCombo();
						GathererThread.this.application.updateStatisticsData();
						GathererThread.this.application.updateDataTable(useRecordSetKey, true);
						GathererThread.this.device.getDialog().resetButtons();
					}
				});
			}
		}
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
