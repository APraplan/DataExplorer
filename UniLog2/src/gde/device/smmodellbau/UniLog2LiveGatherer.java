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
package gde.device.smmodellbau;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.smmodellbau.unilog2.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.TimeLine;

/**
 * Thread implementation to gather data from UniLog device
 * @author Winfied Brügmann
 */
public class UniLog2LiveGatherer extends Thread {
	final static String			$CLASS_NAME									= UniLog2LiveGatherer.class.getName();
	final static Logger			log													= Logger.getLogger(UniLog2LiveGatherer.class.getName());
	final static String			CELL_VOLTAGES_UL2						= "[f[1mUL2live 0052\n" + 
			"[0m*  Zeit  0:00:52\n" + 
			" 0.00V1   0.00V2\n" + 
			" 0.00V3   0.00V4\n" + 
			" 0.00V5   0.00V6\n" + 
			"A1        ----`C\n" + 
			"A2        ----`C\n" + 
			"A3        ----`C\n";

	final DataExplorer			application;
	final UniLog2SerialPort	serialPort;
	final UniLog2						device;
	final UniLog2Dialog			dialog;
	final Channels					channels;
	final Channel						channel;
	final Integer						channelNumber;
	int											timeStep_ms;
	boolean									isPortOpenedByLiveGatherer	= false;
	final int[]							time_ms											= { 1000 / 4, 1000 / 4, 1000 / 4, 1000 / 2, 1000, 2000, 5000, 10000 };
	boolean 								isSwitchedRecordSet 				= false;
	boolean									isGatheredRecordSetVisible	= true;


	// offsets and factors are constant over thread live time
	final HashMap<String, Double> calcValues = new HashMap<String, Double>();

	/**
	 * @throws Exception 
	 */
	public UniLog2LiveGatherer(DataExplorer currentApplication, UniLog2 useDevice, UniLog2SerialPort useSerialPort, UniLog2Dialog useDialog) throws Exception {
		super("liveDataGatherer");
		this.application = currentApplication;
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.dialog = useDialog;
		this.channels = Channels.getInstance();
		this.channelNumber = this.application.getActiveChannelNumber();
		this.channel = this.channels.get(this.channelNumber);
	}

	@Override
	public void run() {
		final String $METHOD_NAME = "run"; //$NON-NLS-1$
		int measurementCount = 0;
		try {
			if (!this.serialPort.isConnected()) {
				this.serialPort.open();
				this.isPortOpenedByLiveGatherer = true;
				//WaitTimer.delay(1000);
			}
		}
		catch (SerialPortException e) {
			this.application.openMessageDialogAsync(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage()}));
			return;
		}
		catch (ApplicationConfigurationException e) {
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
			//WaitTimer.delay(200);
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					UniLog2LiveGatherer.this.application.getDeviceSelectionDialog().open();
				}
			});
			return;
		}
		catch (Throwable t) {
			log.log(Level.SEVERE, t.getMessage(), t);
			this.application.openMessageDialogAsync(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { t.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + t.getMessage()}));
			return;
		}

		this.channels.switchChannel(this.channel.getName());

		// prepare timed data gatherer thread
		final String recordSetKey = this.channel.getNextRecordSetNumber() + this.device.getRecordSetStemNameReplacement();

		this.channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.device, this.channelNumber, true, false, true));
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordSetKey + " created for channel " + this.channel.getName()); //$NON-NLS-1$
		final RecordSet recordSet = this.channel.get(recordSetKey);
		this.channel.applyTemplateBasics(recordSetKey);
		final int[] points = new int[recordSet.size()];
		final UniLog2 usedDevice = this.device;

		try {
			this.serialPort.checkDataReady();
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() }) + System.getProperty("line.separator")
					+ Messages.getString(MessageIds.GDE_MSGW2500);
			cleanup(recordSetKey, message);
			return;
		}
		this.serialPort.isInterruptedByUser = false;
		String[] liveAnswers = new String[3];
		long startCycleTime = 0;
		long tmpCycleTime = 0;

		try {
			while (!this.serialPort.isInterruptedByUser) {
				try {
					// prepare the data for adding to record set
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "recordSetKey = " + recordSetKey + " channelKonfigKey = " + recordSet.getChannelConfigName()); //$NON-NLS-1$ //$NON-NLS-2$

					// build the point array according curves from record set
					liveAnswers = new String[3];
					while (liveAnswers[0] == null || liveAnswers[1] == null || liveAnswers[2] == null) {
						String liveAnswer = UniLog2LiveGatherer.this.serialPort.queryLiveData();
						if(liveAnswer.length() < 100) 
							break;
						if (liveAnswer.contains("UniSens")) liveAnswers[1] = CELL_VOLTAGES_UL2;
						
						if (liveAnswer.contains("rpm") && liveAnswer.contains("VRx")) {
							liveAnswers[0] = liveAnswer;
						}
						else if (liveAnswer.contains("V1") && liveAnswer.contains("V6")) {
							liveAnswers[1] = liveAnswer;
						}
						else if (liveAnswer.contains("hPa") && liveAnswer.contains("intern")) {
							liveAnswers[2] = liveAnswer;
						}
						else if (this.serialPort.isInterruptedByUser)
							break;
					}

					tmpCycleTime = System.nanoTime()/1000000;
					if (startCycleTime == 0) startCycleTime = tmpCycleTime;
					recordSet.addPoints(usedDevice.convertLiveData(points, (new StringBuilder().append(liveAnswers[0]).append(liveAnswers[1]).append(liveAnswers[2])).toString()), (tmpCycleTime - startCycleTime));
					log.logp(Level.TIME, $CLASS_NAME, $METHOD_NAME, "time = " + TimeLine.getFomatedTimeWithUnit(tmpCycleTime - startCycleTime)); //$NON-NLS-1$

					// switch the active record set if the current record set is child of active channel
					if (!UniLog2LiveGatherer.this.isSwitchedRecordSet && UniLog2LiveGatherer.this.channel.getName().equals(UniLog2LiveGatherer.this.channels.getActiveChannel().getName())) {
						UniLog2LiveGatherer.this.channel.applyTemplateBasics(recordSetKey);
						UniLog2LiveGatherer.this.application.getMenuToolBar().addRecordSetName(recordSetKey);
						UniLog2LiveGatherer.this.channels.getActiveChannel().switchRecordSet(recordSetKey);
						UniLog2LiveGatherer.this.isSwitchedRecordSet = true;
					}

					if (++measurementCount % 5 == 0) {
						UniLog2LiveGatherer.this.device.updateVisibilityStatus(recordSet, true);
					}
					if (recordSet.isChildOfActiveChannel() && recordSet.equals(UniLog2LiveGatherer.this.channels.getActiveChannel().getActiveRecordSet())) {
						UniLog2LiveGatherer.this.application.updateAllTabs(false);
					}
				}
				catch (DataInconsitsentException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() });
					cleanup(recordSetKey, message);
					return;
				}
				catch (TimeOutException e) {
					log.log(Level.WARNING, e.getMessage(), e);
					try {
						this.serialPort.checkDataReady();
					}
					catch (Exception e1) {
						log.log(Level.WARNING, e1.getMessage(), e1);
					}
				}
				catch (IOException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() })
							+ System.getProperty("line.separator") + Messages.getString(MessageIds.GDE_MSGW2500); //$NON-NLS-1$ 
					this.application.openMessageDialog(this.dialog.getDialogShell(), message);
					return;
				}
				catch (Throwable e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					String message = e.getClass().getSimpleName() + " - " + e.getMessage(); //$NON-NLS-1$ 
					this.application.openMessageDialog(this.dialog.getDialogShell(), message);
					return;
				}
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "======> exit"); //$NON-NLS-1$
			}
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		finally {
			cleanup(recordSetKey, null);
			if (this.device.getDialog() != null) {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						UniLog2LiveGatherer.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2504), Messages.getString(MessageIds.GDE_MSGT2504));
					}
				});
			}
		}
	}

	/**
	 * cleanup all allocated resources and display the message
	 * @param recordSetKey
	 * @param message
	 */
	private void cleanup(final String recordSetKey, String message) {
		this.serialPort.isInterruptedByUser = true;
		if (this.serialPort.isConnected()) 
			try {
				this.serialPort.write(UniLog2SerialPort.COMMAND_STOP_LOGGING);
			}
			catch (IOException e) {
				// ignore;
			}
		if (this.isPortOpenedByLiveGatherer) {
			this.serialPort.close();
		}
		
		if (this.channel.get(recordSetKey) != null) {
			if (message != null) { 
				this.channel.get(recordSetKey).clear();
				this.channel.remove(recordSetKey);
			}
			this.application.getMenuToolBar().updateRecordSetSelectCombo();
			this.application.updateStatisticsData();
			this.application.updateDataTable(recordSetKey, true);
			if (UniLog2LiveGatherer.this.device.getDialog() != null) {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						UniLog2LiveGatherer.this.device.getDialog().resetButtons();
					}
				});
			}
			if (message != null && message.length() > 5) {
				this.application.openMessageDialog(this.dialog.getDialogShell(), message);
			}
		}
	}

	/**
	 * stop data gathering
	 */
	public synchronized void stopDataGathering() {
		this.serialPort.isInterruptedByUser = true;
	}
}
