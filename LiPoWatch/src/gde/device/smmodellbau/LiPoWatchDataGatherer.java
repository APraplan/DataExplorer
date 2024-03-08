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

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.smmodellbau.lipowatch.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.CalculationThread;
import gde.utils.WaitTimer;

import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Thread implementation to gather data from LiPoWatch device
 * @author Winfied Brügmann
 */
public class LiPoWatchDataGatherer extends Thread {
	final static Logger			log							= Logger.getLogger(LiPoWatchDataGatherer.class.getName());

	final DataExplorer				application;
	final LiPoWatchSerialPort	serialPort;
	final LiPoWatchDialog			dialog;
	final LiPoWatch						device;
	final Integer							channelNumber;
	final String							configKey;
	CalculationThread					calculationThread;

	/**
	 * 
	 */
	public LiPoWatchDataGatherer(DataExplorer currrentApplication, LiPoWatch useDevice, LiPoWatchSerialPort useSerialPort) {
		super("dataGatherer");
		this.application = currrentApplication;
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.dialog = useDevice.getDialog();
		this.channelNumber = 1; //$NON-NLS-1$
		this.configKey = this.device.getChannelNameReplacement(this.channelNumber);
}

	/**
	 * method implements the data gathering from device and load data into the records
	 * if more than one available record set is created the status bar shows the status how many record sets are done
	 * this gives not a real feeling since the record sets may have big differences in number of available telegrams
	 */
	@Override
	public void run() {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "entry data gatherer : " + this.channelNumber + " : " + this.configKey); //$NON-NLS-1$ //$NON-NLS-2$
		Channel channel = Channels.getInstance().get(this.channelNumber);
		String recordSetKey = null;
		RecordSet recordSet = null;
		boolean isPortOpenedByMe = false;

		try {
			if(!this.serialPort.isConnected()) {
				this.serialPort.open();
				isPortOpenedByMe = true;
				WaitTimer.delay(100);
			}
			
			//update the config display of the dialog to enable comment enichments
			final byte[] configBuffer = this.serialPort.readConfiguration();
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					LiPoWatchDataGatherer.this.dialog.updateConfigurationValues(configBuffer);
				}
			});

			this.dialog.resetDataSetsLabel();
			this.serialPort.setTransmitFinished(false);
			Vector<Vector<byte[]>> dataCollection = this.serialPort.getData(this.dialog);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "back from gathering data"); //$NON-NLS-1$

			// iterate over telegram entries to build the record set
			for (Vector<byte[]> telegrams : dataCollection) {
				recordSetKey = channel.getNextRecordSetNumber() + this.device.getRecordSetStemNameReplacement();
				
				// check analog modus and update channel/configuration
				//this.device.updateMeasurementByAnalogModi(telegrams.get(3), this.configKey);
				
				channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.application.getActiveDevice(), channel.getNumber(), true, false, true));
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordSetKey + " created"); //$NON-NLS-1$

				recordSet = channel.get(recordSetKey); // record set where the data is added
				this.device.updateInitialRecordSetComment(recordSet);
				channel.applyTemplateBasics(recordSetKey);
				
				int[] points = new int[recordSet.realSize()];
				byte[] dataBuffer;
				for (int j = 1; j < telegrams.size(); j++) {
					dataBuffer = telegrams.get(j);
					recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer));
				}

				//reduce receodSet to really available number of cells, calculate average over first 10 measurements since I got different number of cells
				int numCells = 0;
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "number of measurements = " +  telegrams.size());
				for (int j = 0; j < 10 && j < telegrams.size(); j++) {
					dataBuffer = telegrams.get(j);
					numCells += (dataBuffer[5] & 0x0F);
				}
				int numberRecords = numCells/10 + 5; // number cells + total battery voltage + servo impuls in + servio impuls out + temperature + balance
				String[] recordNames = recordSet.getRecordNames();
				for (int j = numberRecords; j < this.device.getNumberOfMeasurements(1) && j < recordNames.length-1; j++) {
					recordSet.remove(recordNames[j]);
				}
				recordSet.syncScaleOfSyncableRecords();

				if (channel.getActiveRecordSet() == null) {
					Channels.getInstance().switchChannel(this.channelNumber, recordSetKey);
					channel.switchRecordSet(recordSetKey);
				}
				this.device.makeInActiveDisplayable(recordSet);
				this.device.updateVisibilityStatus(recordSet, true);
				this.application.updateStatisticsData();
				this.application.updateDataTable(recordSetKey, false);
			}
			// make all record set names visible in selection combo
			this.application.getMenuToolBar().updateRecordSetSelectCombo();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "exit data gatherer"); //$NON-NLS-1$

		}
		catch (DataInconsitsentException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
		}
		catch (ApplicationConfigurationException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), e.getClass().getSimpleName() + " - " + e.getMessage()); //$NON-NLS-1$
		}
		catch (TimeOutException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() } )
			+ System.getProperty("line.separator") + Messages.getString(MessageIds.GDE_MSGW1602)); //$NON-NLS-1$
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() } )
			+ System.getProperty("line.separator") + Messages.getString(MessageIds.GDE_MSGW1602)); //$NON-NLS-1$
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), e.getClass().getSimpleName() + " - " + e.getMessage());
		}
		finally {
			this.dialog.resetButtons();
			if(isPortOpenedByMe) this.serialPort.close();
		}
	} // end of run()

	public void setThreadStop() {
		try {
			WaitTimer.delay(5);
			this.serialPort.setTransmitFinished(true);
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
		}
	}
}
