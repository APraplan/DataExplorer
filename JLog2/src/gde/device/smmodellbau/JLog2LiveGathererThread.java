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
    
    Copyright (c) 2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.smmodellbau.jlog2.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.WaitTimer;

import java.util.logging.Logger;

/**
 * Thread implementation to gather data from eStation device
 * @author Winfied Brügmann
 */
public class JLog2LiveGathererThread extends Thread {
	final static String		$CLASS_NAME				= JLog2LiveGathererThread.class.getName();
	final static Logger		log								= Logger.getLogger(JLog2LiveGathererThread.class.getName());
	final static int			WAIT_TIME_RETRYS	= 36;

	final DataExplorer		application;
	final JLog2SerialPort	serialPort;
	final JLog2						device;
	final Channels				channels;
	final Channel					channel;
	final int							channelNumber;
	final JLog2Dialog			dialog;

	String								recordSetKey			= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272);

	/**
	 * data gatherer thread definition 
	 * @throws SerialPortException 
	 * @throws ApplicationConfigurationException 
	 * @throws Exception 
	 */
	public JLog2LiveGathererThread(DataExplorer currentApplication, JLog2 useDevice, JLog2SerialPort useSerialPort, int channelConfigNumber, JLog2Dialog useDialog) throws ApplicationConfigurationException {
		super("dataGatherer"); //$NON-NLS-1$
		this.application = currentApplication;
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.channels = Channels.getInstance();
		this.channelNumber = channelConfigNumber;
		this.channel = this.channels.get(this.channelNumber);
		this.dialog = useDialog;

		this.setPriority(Thread.MAX_PRIORITY);
	}

	@Override
	public void run() {
		final String $METHOD_NAME = "run"; //$NON-NLS-1$
		RecordSet recordSet = null;
		int[] points = new int[this.device.getNumberOfMeasurements(this.channelNumber)];
		long measurementCount = 0;
		byte[] dataBuffer = null;

		this.serialPort.isInterruptedByUser = false;
		JLog2LiveGathererThread.log.logp(java.util.logging.Level.FINE, JLog2LiveGathererThread.$CLASS_NAME, $METHOD_NAME, "====> entry initial time step ms = " + this.device.getTimeStep_ms()); //$NON-NLS-1$

		try {
			if (!this.serialPort.isConnected()) {
				this.serialPort.open();
				WaitTimer.delay(1000);
			}
			// check if device is ready for data capturing, discharge or charge allowed only
			while (!this.serialPort.isInterruptedByUser && this.serialPort.cleanInputStream() <= 0)
				WaitTimer.delay(1000);
		}
		catch (Exception e) {
			JLog2LiveGathererThread.log.logp(java.util.logging.Level.SEVERE, JLog2LiveGathererThread.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}

		try {
			long startTime = System.nanoTime() / 1000000;
			while (!this.serialPort.isInterruptedByUser) {
				try {
					// get data from device
					dataBuffer = this.serialPort.getData();

					// else wait for 180 seconds max. for actions
					String processName = this.device.getProcessName(dataBuffer);

					if (this.channel.size() == 0 || recordSet == null || !this.recordSetKey.endsWith(" " + processName)) { //$NON-NLS-1$
						this.application.setStatusMessage(""); //$NON-NLS-1$
						this.recordSetKey = this.channel.getNextRecordSetNumber() + ") " + processName; //$NON-NLS-1$
						this.channel.put(this.recordSetKey, RecordSet.createRecordSet(this.recordSetKey, this.application.getActiveDevice(), this.channel.getNumber(), true, false, true));
						JLog2LiveGathererThread.log.logp(java.util.logging.Level.FINE, JLog2LiveGathererThread.$CLASS_NAME, $METHOD_NAME, this.recordSetKey + " created for channel " + this.channel.getName()); //$NON-NLS-1$
						if (this.channel.getActiveRecordSet() == null) this.channel.setActiveRecordSet(this.recordSetKey);
						recordSet = this.channel.get(this.recordSetKey);
						this.channel.applyTemplateBasics(this.recordSetKey);
						// switch the active record set if the current record set is child of active channel
						// for eStation its always the case since we have only one channel
						if (this.channel.getName().equals(this.channels.getActiveChannel().getName())) {
							this.channels.getActiveChannel().switchRecordSet(this.recordSetKey);
						}
						measurementCount = 0;
					}

					// prepare the data for adding to record set
					recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer), System.nanoTime() / 1000000 - startTime);

					if (recordSet.size() > 0 && recordSet.isChildOfActiveChannel() && recordSet.equals(this.channels.getActiveChannel().getActiveRecordSet())) {
						JLog2LiveGathererThread.this.application.updateAllTabs(false);
					}
					++measurementCount;

					if (measurementCount > 0 && measurementCount % 10 == 0) {
						this.device.updateVisibilityStatus(recordSet, true);
					}
				}
				catch (DataInconsitsentException e) {
					String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0036, new Object[] { this.getClass().getSimpleName(), $METHOD_NAME });
					cleanup(message);
				}
				catch (Throwable e) {
					JLog2LiveGathererThread.log.log(java.util.logging.Level.FINE, "JLog2 program end detected"); //$NON-NLS-1$
					finalizeRecordSet();
				}
			}
		}
		finally {
			if (this.serialPort.isConnected()) this.serialPort.close();
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					JLog2LiveGathererThread.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2804), Messages.getString(MessageIds.GDE_MSGT2804));
					if (JLog2LiveGathererThread.this.dialog != null && !JLog2LiveGathererThread.this.dialog.isDisposed()) {
						JLog2LiveGathererThread.this.dialog.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2805));
					}
				}
			});
			this.application.setStatusMessage(""); //$NON-NLS-1$
			JLog2LiveGathererThread.log.logp(java.util.logging.Level.FINE, JLog2LiveGathererThread.$CLASS_NAME, $METHOD_NAME, "======> exit"); //$NON-NLS-1$
		}
	}

	/**
	 * stop data gathering
	 */
	public synchronized void stopDataGathering() {
		this.serialPort.isInterruptedByUser = true;
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 */
	void finalizeRecordSet() {
		stopDataGathering();
		if (this.serialPort.isConnected()) this.serialPort.close();

		RecordSet tmpRecordSet = this.channel.get(this.recordSetKey);
		if (tmpRecordSet != null) {
			this.device.updateVisibilityStatus(tmpRecordSet, true);
			this.device.makeInActiveDisplayable(tmpRecordSet);
			this.application.updateStatisticsData();
			this.application.updateDataTable(this.recordSetKey, false);

			this.device.setAverageTimeStep_ms(tmpRecordSet.getAverageTimeStep_ms());
			JLog2LiveGathererThread.log.log(Level.TIME, "set average time step msec = " + this.device.getAverageTimeStep_ms()); //$NON-NLS-1$
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
					public void run() {
						JLog2LiveGathererThread.this.application.getMenuToolBar().updateRecordSetSelectCombo();
						JLog2LiveGathererThread.this.application.updateStatisticsData();
						JLog2LiveGathererThread.this.application.updateDataTable(useRecordSetKey, true);
						JLog2LiveGathererThread.this.application.openMessageDialog(message);
					}
				});
			}
		}
		else
			this.application.openMessageDialog(message);
	}
}
