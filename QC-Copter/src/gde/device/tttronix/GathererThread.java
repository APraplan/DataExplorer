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
package gde.device.tttronix;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.TimeLine;

import java.util.logging.Logger;

/**
 * Thread implementation to gather data from eStation device
 * @author Winfied Brügmann
 */
public class GathererThread extends Thread {
	final static String				$CLASS_NAME									= GathererThread.class.getName();
	final static Logger				log													= Logger.getLogger(GathererThread.class.getName());

	final DataExplorer				application;
	final QcCopterSerialPort	serialPort;
	final QcCopter						device;
	final QcCopterDialog			dialog;
	final Channels						channels;
	final Channel							channel;
	final int									channelNumber;

	String										recordSetKey								= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272);
	boolean										isPortOpenedByLiveGatherer	= false;
	boolean										isGatheredRecordSetVisible	= true;
	boolean										isCollectDataStopped				= false;

	/**
	 * data gatherer thread definition 
	 * @throws SerialPortException 
	 * @throws ApplicationConfigurationException 
	 * @throws Exception 
	 */
	public GathererThread(DataExplorer currentApplication, QcCopter useDevice, QcCopterSerialPort useSerialPort, int channelConfigNumber, QcCopterDialog useDialog)
			throws ApplicationConfigurationException, SerialPortException {
		super("dataGatherer");
		this.application = currentApplication;
		this.device = useDevice;
		this.dialog = useDialog;
		this.serialPort = useSerialPort;
		this.channels = Channels.getInstance();
		this.channelNumber = channelConfigNumber;
		this.channel = this.channels.get(this.channelNumber);

		if (!this.serialPort.isConnected()) {
			this.serialPort.open();
			this.isPortOpenedByLiveGatherer = true;
		}
		this.setPriority(Thread.MAX_PRIORITY);
	}

	@Override
	public void run() {
		final String $METHOD_NAME = "run"; //$NON-NLS-1$
		RecordSet recordSet = null;
		int[] points = new int[this.device.getNumberOfMeasurements(this.channelNumber)];
		long startCycleTime = 0;
		long tmpCycleTime = 0;
		long measurementCount = 0;
		byte[] dataBuffer = null;
		
		this.isCollectDataStopped = false;
		if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "====> entry initial time step ms = " + this.device.getTimeStep_ms()); //$NON-NLS-1$

		StringBuffer terminalText = new StringBuffer();
		while (!this.isCollectDataStopped && this.serialPort.isConnected()) {
			try {
				// check if device is ready for data capturing or terminal open
				// in case of time outs wait for 180 seconds max. for actions
				if (this.dialog != null && !this.dialog.isDisposed() && this.dialog.getTabFolderSelectionIndex() == 0) {					
					String text = this.serialPort.getTerminalData();
					if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, text);
					if (text.length() > 0 && !text.equals(GDE.STRING_EMPTY)) {
						if (this.serialPort.containsSTX(text.getBytes())) {
							terminalText = new StringBuffer();
							terminalText.append(Messages.getString(MessageIds.GDE_MSGI1903)).append(text);
							this.dialog.setTerminalText(terminalText.toString());
						}
						else if (this.serialPort.containsFF(text.getBytes())) {
							terminalText = new StringBuffer();
							if(text.length() > 1)
								terminalText.append(text.substring(text.indexOf(QcCopterSerialPort.FF) + 1));
						}
						else {
							terminalText.append(text);
						}
						if (terminalText.length() > 60) this.dialog.setTerminalText(terminalText.toString());
						else log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, text);
					}
					else {
						stopDataGatheringThread(true, null);
					}
				}
				else { //if (this.dialog != null && this.dialog.isDisposed()) {
					//get flight simulation data from device
					dataBuffer = this.serialPort.getData();

					// check if a record set matching for re-use is available and prepare a new if required
					if (this.channel.size() == 0 || recordSet == null || !this.recordSetKey.endsWith(this.device.getRecordSetStemNameReplacement())) { //$NON-NLS-1$
						this.application.setStatusMessage(""); //$NON-NLS-1$
						// record set does not exist or is outdated, build a new name and create
						this.recordSetKey = this.channel.getNextRecordSetNumber() + this.device.getRecordSetStemNameReplacement(); //$NON-NLS-1$ 
						this.channel.put(this.recordSetKey, RecordSet.createRecordSet(this.recordSetKey, this.application.getActiveDevice(), this.channel.getNumber(), true, false, true));
						this.channel.applyTemplateBasics(this.recordSetKey);
						log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, this.recordSetKey + " created for channel " + this.channel.getName()); //$NON-NLS-1$
						if (this.channel.getActiveRecordSet() == null) this.channel.setActiveRecordSet(this.recordSetKey);
						recordSet = this.channel.get(this.recordSetKey);
						recordSet.setAllDisplayable();
						this.device.updateVisibilityStatus(recordSet, false);
						// switch the active record set if the current record set is child of active channel
						this.channels.getActiveChannel().switchRecordSet(this.recordSetKey);
					
						measurementCount = 0;
						startCycleTime = 0;
					}

					// prepare the data for adding to record set
					this.isGatheredRecordSetVisible = this.recordSetKey.equals(this.channels.getActiveChannel().getActiveRecordSet().getName());
					tmpCycleTime = System.nanoTime() / 1000000;
					if (measurementCount++ == 0) {
						startCycleTime = tmpCycleTime;
					}
					else if (measurementCount % 10 == 0) {
						this.device.updateVisibilityStatus(recordSet, true);
					}
					recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer), (tmpCycleTime - startCycleTime));
					if (log.isLoggable(Level.TIME)) log.logp(Level.TIME, GathererThread.$CLASS_NAME, $METHOD_NAME, "time = " + TimeLine.getFomatedTimeWithUnit(tmpCycleTime - startCycleTime)); //$NON-NLS-1$

					if (recordSet.isChildOfActiveChannel() && recordSet.equals(this.channels.getActiveChannel().getActiveRecordSet())) {
						GathererThread.this.application.updateAllTabs(false);
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
					this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI1900));
					if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, Messages.getString(MessageIds.GDE_MSGI1900));
				}
				// program end or unexpected exception occurred, stop data gathering to enable save data by user
				else {
					log.log(Level.FINE, "data gathering end detected"); //$NON-NLS-1$
					stopDataGatheringThread(true, e);
				}
			}
		}
		this.application.setStatusMessage(""); //$NON-NLS-1$
		if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "======> exit"); //$NON-NLS-1$
		
		if(!this.isCollectDataStopped) {
			this.stopDataGatheringThread(true, null);
		}
	}

	/**
	 * stop the data gathering and check if reasonable data in record set to finalize or clear
	 * @param enableEndMessage
	 * @param throwable
	 */
	void stopDataGatheringThread(boolean enableEndMessage, Throwable throwable) {
		final String $METHOD_NAME = "stopDataGatheringThread"; //$NON-NLS-1$

		if (throwable != null) {
			log.logp(Level.WARNING, GathererThread.$CLASS_NAME, $METHOD_NAME, throwable.getMessage(), throwable);
		}

		this.isCollectDataStopped = true;

		if (this.serialPort != null && this.serialPort.getXferErrors() > 0) {
			log.log(Level.WARNING, "During complete data transfer " + this.serialPort.getXferErrors() + " number of errors occured!"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (this.serialPort != null && this.serialPort.isConnected() && this.isPortOpenedByLiveGatherer == true && this.serialPort.isConnected()) {
			this.serialPort.close();
		}
		if (this.dialog != null && !this.dialog.isDisposed()) {
			this.dialog.checkPortStatus();
		}

		RecordSet recordSet = this.channel.get(this.recordSetKey);
		if (recordSet != null && recordSet.getRecordDataSize(true) > 5) { // some other exception while program execution, record set has data points
			finalizeRecordSet(false);
			if (enableEndMessage) this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(MessageIds.GDE_MSGT1903));
		}
		else {
			if (throwable != null) {
				cleanup(Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { throwable.getClass().getSimpleName(), throwable.getMessage() }) + Messages.getString(MessageIds.GDE_MSGT1902));
			}
			else {
				if (enableEndMessage) cleanup(Messages.getString(gde.messages.MessageIds.GDE_MSGE0026) + Messages.getString(MessageIds.GDE_MSGT1902));
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
			this.device.updateVisibilityStatus(tmpRecordSet, false);
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
				this.application.openMessageDialog(GathererThread.this.dialog.getDialogShell(), message);
				//this.device.getDialog().resetButtons();
			}
			else {
				final String useRecordSetKey = this.recordSetKey;
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						GathererThread.this.application.getMenuToolBar().updateRecordSetSelectCombo();
						GathererThread.this.application.updateStatisticsData();
						GathererThread.this.application.updateDataTable(useRecordSetKey, true);
						GathererThread.this.application.openMessageDialog(GathererThread.this.dialog.getDialogShell(), message);
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
}
