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
package gde.device.renschler;

import java.util.Vector;
import gde.log.Level;
import java.util.logging.Logger;

import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.PropertyType;
import gde.exception.DataInconsitsentException;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.CalculationThread;
import gde.utils.QuasiLinearRegression;

/**
 * Thread implementation to gather data from Picolariolog device
 * @author Winfied Brügmann
 */
public class DataGathererThread extends Thread {
	final static Logger				log							= Logger.getLogger(DataGathererThread.class.getName());

	DataExplorer		application;
	String[]									datagramNumbers;
	final String							RECORD_SET_NAME	= Messages.getString(MessageIds.GDE_MSGT1220);
	final String							configKey;
	final PicolarioSerialPort	serialPort;
	final PicolarioDialog			dialog;
	final Picolario						device;
	CalculationThread					calculationThread;
	boolean										threadStop			= false;

	/**
	 * 
	 */
	public DataGathererThread(DataExplorer currentApplication, Picolario currentDevice, PicolarioSerialPort currentSerialPort, String[] useDatagramNumbers) {
		super("dataGatherer");
		this.application = currentApplication;
		this.device = currentDevice;
		this.serialPort = currentSerialPort;
		this.dialog = (PicolarioDialog) currentDevice.getDialog();
		this.datagramNumbers = useDatagramNumbers;
		this.configKey = currentDevice.getChannelNameReplacement(1);
	}

	/**
	 * method implements the data gathering from device and load data into the records
	 * if more than one available record set is created the status bar shows the status how many record sets are done
	 * this gives not a real feeling since the record sets may have big differences in number of available telegrams
	 */
	@Override
	public void run() {
		boolean isPortOpenedByMe = false;
		try {
			DataGathererThread.log.log(Level.FINE, "entry data gatherer"); //$NON-NLS-1$
			Channel channel = Channels.getInstance().getActiveChannel();
			String[] measurements = this.device.getMeasurementNamesReplacements(channel.getNumber()); // 0=Spannung, 1=Höhe, 2=Steigrate
			String recordSetKey;

			this.dialog.resetDataSetsLabel();
			if (!this.serialPort.isConnected()) {
				this.serialPort.open();
				isPortOpenedByMe = true;
			}

			for (int j = 0; j < this.datagramNumbers.length && !this.threadStop; ++j) {
				this.dialog.resetTelegramLabel();
				this.dialog.setAlreadyRedDataSets(this.datagramNumbers[j]);
				Vector<byte[]> data = this.serialPort.getData(Integer.valueOf(this.datagramNumbers[j]).intValue(), this.device);
				recordSetKey = channel.getNextRecordSetNumber() + this.RECORD_SET_NAME;
				channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.application.getActiveDevice(), channel.getNumber(), true, false, true));
				DataGathererThread.log.log(Level.FINE, recordSetKey + " created"); //$NON-NLS-1$
				if (channel.getActiveRecordSet() == null) Channels.getInstance().getActiveChannel().setActiveRecordSet(recordSetKey);
				RecordSet recordSet = channel.get(recordSetKey);

				byte[] dataBuffer = new byte[recordSet.size()];
				int[] points = new int[this.device.getNumberOfMeasurements(1)];
				
				for (byte[] reveivedBuffer : data) { // 31 or x*3 + 1
					for (int i = 0; i < reveivedBuffer.length/3; ++i) {  // three bytes per datapoint
						System.arraycopy(reveivedBuffer, i*3, dataBuffer, 0, 3);
						recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer));
					}
				}
				
				// start slope calculation
				PropertyType property = recordSet.get(measurements[2]).getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
				int regressionInterval = property != null ? Integer.valueOf(property.getValue()) : 4;
				this.calculationThread = new QuasiLinearRegression(recordSet, measurements[1], measurements[2], regressionInterval);
				try {
					this.calculationThread.start();
				}
				catch (RuntimeException e) {
					log.log(Level.WARNING, e.getMessage(), e);
				}

				this.application.getMenuToolBar().addRecordSetName(recordSetKey);
				if (channel.getRecordSetNames().length <= 1 || this.dialog.isDoSwtichRecordSet()) channel.switchRecordSet(recordSetKey);

				// update the progress bar reading one after the other only
				channel.get(recordSetKey).setAllDisplayable();
				channel.applyTemplate(recordSetKey, true);
			}// end for
			this.dialog.enableReadButtons();
			DataGathererThread.log.log(Level.FINE, "exit data gatherer"); //$NON-NLS-1$

		}
		catch (DataInconsitsentException e) {
			DataGathererThread.log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
		}
		catch (Exception e) {
			DataGathererThread.log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
		}
		finally {
			if (isPortOpenedByMe) this.serialPort.close();
			this.dialog.resetButtons();
		}
	} // end of run()

	public void setThreadStop(boolean enable) {
		this.threadStop = enable;
		this.serialPort.setTransmitFinished(true);
	}

}
