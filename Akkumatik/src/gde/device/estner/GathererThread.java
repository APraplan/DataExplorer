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
    
    Copyright (c) 2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.estner;

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

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Thread implementation to gather data from Akkumatik device
 * @author Winfied Brügmann
 */
public class GathererThread extends Thread {
	final static String				$CLASS_NAME									= GathererThread.class.getName();
	final static Logger				log													= Logger.getLogger(GathererThread.class.getName());
	final static int					WAIT_TIME_RETRYS						= 3600;

	final DataExplorer				application;
	final AkkumatikSerialPort	serialPort;
	final Akkumatik						device;
	final Channels						channels;
	Channel										channel;
	final int									channelNumber;

	String										recordSetKey1								= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272);
	String										recordSetKey2								= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272);
	boolean										isPortOpenedByLiveGatherer	= false;
	int												statusNumberCh1							= 0;
	int												statusNUmberCh2						= 0;
	int												retryCounter								= GathererThread.WAIT_TIME_RETRYS;
	boolean										isCollectDataStopped				= false;
	int												lastNumberDisplayableRecords1	= 0;
	int												lastNumberDisplayableRecords2	= 0;

	/**
	 * data gatherer thread definition 
	 * @throws SerialPortException 
	 * @throws ApplicationConfigurationException 
	 * @throws Exception 
	 */
	public GathererThread(DataExplorer currentApplication, Akkumatik useDevice, AkkumatikSerialPort useSerialPort, int channelConfigNumber) throws ApplicationConfigurationException, SerialPortException {
		super("dataGatherer");
		this.application = currentApplication;
		this.device = useDevice;
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
		RecordSet recordSet1 = null, recordSet2 = null;
		int[] points1 = new int[this.device.getNumberOfMeasurements(this.channelNumber)];
		int[] points2 = new int[this.device.getNumberOfMeasurements(this.channelNumber)];
		boolean isProgrammExecuting1 = false, isProgrammExecuting2 = false;
		long startCycleTime1 = 0, startCycleTime2 = 0;
		long timeStep1 = 0, timeStep2 = 0;
		long lastCycleTime1 = 0, lastCycleTime2 = 0;
		boolean isCycleMode1 = false, isCycleMode2 = false;
		String cycleCount1 = GDE.STRING_EMPTY, cycleCount2 = GDE.STRING_EMPTY;
		String[] data = null;

		this.isCollectDataStopped = false;
		GathererThread.log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "====> entry initial time step ms = " + this.device.getTimeStep_ms()); //$NON-NLS-1$
		try {
			this.serialPort.cleanInputStream();
		}
		catch (IOException e1) {
			// ignore
		}
		
		//long atualTime_ms = 0;
		while (!this.isCollectDataStopped) {
			//atualTime_ms = System.currentTimeMillis();
			try {
				// get data from device
				data = this.serialPort.getDataArray(this.serialPort.getData());
				
				//check if dialog prepared data to write
				byte[] data2Write = AkkumatikDialog.getData2Write();
				if (data2Write != null && data2Write.length > 0) {
					this.serialPort.write(data2Write);
				}

				// check if device is ready for data capturing, discharge or charge allowed only
				final String processName = this.device.isCycleMode(data) 
						? data[4].startsWith(GDE.STRING_MINUS) ?  Messages.getString(MessageIds.GDE_MSGT3401) :  Messages.getString(MessageIds.GDE_MSGT3400)
						: Akkumatik.PROCESS_MODE[this.device.getProcessingMode(data)];
				final String processType = this.device.getProcessingPhase(data) == 10 ? Messages.getString(MessageIds.GDE_MSGT3420) : Akkumatik.CHARGE_MODE[this.device.getProcessingType(data)];

				switch (data != null ? Integer.valueOf(data[0]) : 0) { // device outlet 1 or 2
				case 1:
					isProgrammExecuting1 = this.device.isProcessing(data);
					isCycleMode1 = this.device.isCycleMode(data);
					cycleCount1 = GDE.STRING_EMPTY + (isCycleMode1 ? "#" + this.device.getNumberOfCycle(data) : GDE.STRING_BLANK);
					GathererThread.log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME,
							String.format("1: isProcessing = %b process mode = %s isCycleMode = %b(%s) process type = %s", isProgrammExecuting1, processName, isCycleMode1, cycleCount1, processType));

					if (isProgrammExecuting1) {
						this.channel = this.channels.get(1);
						// check state change waiting to discharge to charge
						// check if a record set matching for re-use is available and prepare a new if required
						if (this.channel.size() == 0 || recordSet1 == null
								|| !(this.recordSetKey1.contains(processName) && (cycleCount1.length() > 0 ? this.recordSetKey1.contains(cycleCount1) : true) && this.recordSetKey1.endsWith(processType))) {
							this.application.setStatusMessage(""); //$NON-NLS-1$
							// record set does not exist or is outdated, build a new name and create
							int akkuType = this.device.getAccuCellType(data);
							this.recordSetKey1 = this.channel.getNextRecordSetNumber() + GDE.STRING_RIGHT_PARENTHESIS_BLANK + processName + GDE.STRING_BLANK_LEFT_BRACKET + Akkumatik.ACCU_TYPES[akkuType];
							if (isCycleMode1) this.recordSetKey1 = this.recordSetKey1 + GDE.STRING_BLANK + Messages.getString(MessageIds.GDE_MSGT3421, new Object[] { cycleCount1 });
							this.recordSetKey1 = this.recordSetKey1 + GDE.STRING_RIGHT_BRACKET;
							if (processType.length() > 0) this.recordSetKey1 = this.recordSetKey1 + GDE.STRING_MESSAGE_CONCAT + processType;
							GathererThread.log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "Create record set with name = " + this.recordSetKey1);
							this.channel.put(this.recordSetKey1, RecordSet.createRecordSet(this.recordSetKey1, this.application.getActiveDevice(), this.channel.getNumber(), true, false, true));
							GathererThread.log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, this.recordSetKey1 + " created for channel " + this.channel.getName()); //$NON-NLS-1$
							if (this.channel.getActiveRecordSet() == null) this.channel.setActiveRecordSet(this.recordSetKey1);
							recordSet1 = this.channel.get(this.recordSetKey1);
							this.channel.applyTemplateBasics(this.recordSetKey1);
							// switch the active record set if the current record set is child of active channel
							// for Akkumatik its always the case since we have only one channel
							if (this.channel.getName().equals(this.channels.getActiveChannel().getName())) {
								this.channels.getActiveChannel().switchRecordSet(this.recordSetKey1);
							}
							startCycleTime1 = this.device.getProcessingTime(data);
							recordSet1.setAllDisplayable();
							this.channels.switchChannel(this.channel.getName());
							this.channel.switchRecordSet(this.recordSetKey1);
							lastCycleTime1 = 0;
						}

						// prepare the data for adding to record set
						recordSet1 = this.channel.get(this.recordSetKey1);

						timeStep1 = this.device.getProcessingTime(data) - startCycleTime1;
						if (lastCycleTime1 < timeStep1 ) {
							recordSet1.addPoints(this.device.convertDataBytes(points1, data), timeStep1);
							if (log.isLoggable(Level.TIME)) GathererThread.log.logp(Level.TIME, GathererThread.$CLASS_NAME, $METHOD_NAME, "time = " + TimeLine.getFomatedTimeWithUnit(startCycleTime1 + this.device.getProcessingTime(data))); //$NON-NLS-1$
							lastCycleTime1 = timeStep1;
						}
						if (recordSet1 != null && recordSet1.get(0).realSize() < 3 || recordSet1 != null && recordSet1.get(0).realSize() % 10 == 0) {
							this.device.updateVisibilityStatus(recordSet1, true);
						}
						RecordSet activeRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
						if (activeRecordSet != null && recordSet1 != null && recordSet1.size() > 0 && recordSet1.isChildOfActiveChannel() && recordSet1.equals(activeRecordSet)) {
							GathererThread.this.application.updateAllTabs(false, this.lastNumberDisplayableRecords1 != recordSet1.getConfiguredDisplayable());
							this.lastNumberDisplayableRecords1 = recordSet1.getConfiguredDisplayable();
						}
						this.statusNumberCh1 = 0;
					}
					this.statusNumberCh1 = this.device.getNumberStatus(data);
					break;

				case 2:
					isProgrammExecuting2 = this.device.isProcessing(data);
					isCycleMode2 = this.device.isCycleMode(data);
					cycleCount2 = GDE.STRING_EMPTY + (isCycleMode2 ? "#" + this.device.getNumberOfCycle(data) : GDE.STRING_BLANK);
					GathererThread.log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME,
							String.format("2: isProcessing = %b process mode = %s isCycleMode = %b(%s) process type = %s", isProgrammExecuting2, processName, isCycleMode2, cycleCount2, processType));

					if (isProgrammExecuting2) {
						this.channel = this.channels.get(2);
						// check state change waiting to discharge to charge
						// check if a record set matching for re-use is available and prepare a new if required
						if (this.channel.size() == 0 || recordSet2 == null
								|| !(this.recordSetKey2.contains(processName) && (cycleCount2.length() > 0 ? this.recordSetKey2.contains(cycleCount2) : true) && this.recordSetKey2.endsWith(processType))) {
							this.application.setStatusMessage(""); //$NON-NLS-1$
							// record set does not exist or is outdated, build a new name and create
							int akkuType = this.device.getAccuCellType(data);
							this.recordSetKey2 = this.channel.getNextRecordSetNumber() + GDE.STRING_RIGHT_PARENTHESIS_BLANK + processName + GDE.STRING_BLANK_LEFT_BRACKET + Akkumatik.ACCU_TYPES[akkuType];
							if (isCycleMode2) this.recordSetKey2 = this.recordSetKey2 + GDE.STRING_BLANK + Messages.getString(MessageIds.GDE_MSGT3421, new Object[] { cycleCount2 });
							this.recordSetKey2 = this.recordSetKey2 + GDE.STRING_RIGHT_BRACKET;
							if (processType.length() > 0) this.recordSetKey2 = this.recordSetKey2 + GDE.STRING_MESSAGE_CONCAT + processType;
							GathererThread.log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "Create record set with name = " + this.recordSetKey2);
							this.channel.put(this.recordSetKey2, RecordSet.createRecordSet(this.recordSetKey2, this.application.getActiveDevice(), this.channel.getNumber(), true, false, true));
							GathererThread.log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, this.recordSetKey2 + " created for channel " + this.channel.getName()); //$NON-NLS-1$
							if (this.channel.getActiveRecordSet() == null) this.channel.setActiveRecordSet(this.recordSetKey2);
							recordSet2 = this.channel.get(this.recordSetKey2);
							this.channel.applyTemplateBasics(this.recordSetKey2);
							// switch the active record set if the current record set is child of active channel
							// for Akkumatik its always the case since we have only one channel
							if (this.channel.getName().equals(this.channels.getActiveChannel().getName())) {
								this.channels.getActiveChannel().switchRecordSet(this.recordSetKey2);
							}
							startCycleTime2 = this.device.getProcessingTime(data);
							recordSet2.setAllDisplayable();
							this.channels.switchChannel(this.channel.getName());
							this.channel.switchRecordSet(this.recordSetKey2);
							lastCycleTime2 = 0;
						}

						// prepare the data for adding to record set
						recordSet2 = this.channel.get(this.recordSetKey2);

						timeStep2 = this.device.getProcessingTime(data) - startCycleTime2;
						if (lastCycleTime2 < timeStep2 ) {
							recordSet2.addPoints(this.device.convertDataBytes(points2, data), timeStep2);
							if (log.isLoggable(Level.TIME)) GathererThread.log.logp(Level.TIME, GathererThread.$CLASS_NAME, $METHOD_NAME, "time = " + TimeLine.getFomatedTimeWithUnit(startCycleTime2 + this.device.getProcessingTime(data))); //$NON-NLS-1$
							lastCycleTime2 = timeStep2;
						}

						if (recordSet2 != null && recordSet2.get(0).realSize() < 3 || recordSet2 != null && recordSet2.get(0).realSize() % 10 == 0) {
							this.device.updateVisibilityStatus(recordSet2, true);
						}
						RecordSet activeRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
						if (activeRecordSet != null && recordSet2 != null && recordSet2.size() > 0 && recordSet2.isChildOfActiveChannel() && recordSet2.equals(activeRecordSet)) {
							GathererThread.this.application.updateAllTabs(false, this.lastNumberDisplayableRecords2 != recordSet2.getConfiguredDisplayable());
							this.lastNumberDisplayableRecords2 = recordSet2.getConfiguredDisplayable();
						}
						this.statusNUmberCh2 = 0;
					}
					this.statusNUmberCh2 = this.device.getNumberStatus(data);
					break;

				default:
					GathererThread.log.logp(Level.WARNING, GathererThread.$CLASS_NAME, $METHOD_NAME, "Create record set with name = nothing executing ?");
					break;
				}


				if (!isProgrammExecuting1 || !isProgrammExecuting2)
					setProcessingStatus(statusNumberCh1, statusNUmberCh2);

				if (!isProgrammExecuting1 && !isProgrammExecuting2) { // no Akkumatik program is executing, wait for 180 seconds max. for actions
					//this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI3400));
					GathererThread.log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "wait for Akkumatik activation"); //$NON-NLS-1$

					if (recordSet1 != null && recordSet1.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(false);
						isProgrammExecuting1 = isProgrammExecuting2 = false;
						recordSet1 = null;
					}
					if (recordSet2 != null && recordSet2.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(false);
						isProgrammExecuting1 = isProgrammExecuting2 = false;
						recordSet2 = null;
					}
				}
			}
			catch (DataInconsitsentException e) {
				String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0036, new Object[] { this.getClass().getSimpleName(), $METHOD_NAME });
				cleanup(message);
			}
			catch (Throwable e) {
				// this case will be reached while NiXx Akku discharge/charge/discharge cycle
				if (e instanceof TimeOutException /* && isCycleMode && dryTimeCycleCount > 0 */) {
					finalizeRecordSet(false);
					GathererThread.log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "(dry time) waiting..."); //$NON-NLS-1$
					this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI3401));
					recordSet1 = recordSet2 = null;
					//--dryTimeCycleCount;
				}
				// this case will be reached while Akkumatik program is started, checked and the check not asap committed, stop pressed
				else if (e instanceof TimeOutException && !isProgrammExecuting1 && !isProgrammExecuting2) {
					this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI3400));
					GathererThread.log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "wait for Akkumatik activation ..."); //$NON-NLS-1$
					if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						GathererThread.log.log(java.util.logging.Level.FINE, "Akkumatik activation timeout"); //$NON-NLS-1$
						this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGW3400));
						stopDataGatheringThread(false, null);
					}
				}
				// program end or unexpected exception occurred, stop data gathering to enable save data by user
				else {
					GathererThread.log.log(java.util.logging.Level.FINE, "Akkumatik program end detected"); //$NON-NLS-1$
					stopDataGatheringThread(true, e);
				}
			}
			//wait for one second is over
			//WaitTimer.delay(atualTime_ms + 500 - System.currentTimeMillis());
		}
		this.application.setStatusMessage(""); //$NON-NLS-1$
		GathererThread.log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "======> exit"); //$NON-NLS-1$
	}

	/**
	 * stop the data gathering and check if reasonable data in record set to finalize or clear
	 * @param enableEndMessage
	 * @param throwable
	 */
	void stopDataGatheringThread(boolean enableEndMessage, Throwable throwable) {
		final String $METHOD_NAME = "stopDataGatheringThread"; //$NON-NLS-1$

		if (throwable != null) {
			GathererThread.log.logp(java.util.logging.Level.WARNING, GathererThread.$CLASS_NAME, $METHOD_NAME, throwable.getMessage(), throwable);
		}

		this.isCollectDataStopped = true;

		if (this.serialPort != null && this.serialPort.getXferErrors() > 0) {
			GathererThread.log.log(java.util.logging.Level.WARNING, "During complete data transfer " + this.serialPort.getXferErrors() + " number of errors occured!"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (this.serialPort != null && this.serialPort.isConnected() && this.isPortOpenedByLiveGatherer == true && this.serialPort.isConnected()) {
			this.serialPort.close();
		}

		RecordSet recordSet1 = this.channel.get(this.recordSetKey1);
		RecordSet recordSet2 = this.channel.get(this.recordSetKey2);
		if ((recordSet2 != null && recordSet2.getRecordDataSize(true) > 5) || (recordSet1 != null && recordSet1.getRecordDataSize(true) > 5)) { // some other exception while program execution, record set has data points
			finalizeRecordSet(false);
			if (enableEndMessage) this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGW3401));
		}
		else {
			if (throwable != null) {
				cleanup(Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { throwable.getClass().getSimpleName(), throwable.getMessage() }) + "Datenaufnahme beendet");
			}
			else {
				if (enableEndMessage) cleanup(Messages.getString(gde.messages.MessageIds.GDE_MSGE0026) + Messages.getString(MessageIds.GDE_MSGW3401));
			}
		}
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 */
	void finalizeRecordSet(boolean doClosePort) {
		if (doClosePort && this.isPortOpenedByLiveGatherer && this.serialPort.isConnected()) this.serialPort.close();

		RecordSet tmpRecordSet = this.channel.get(this.recordSetKey1);
		if (tmpRecordSet != null) {
			this.device.updateVisibilityStatus(tmpRecordSet, true);
			this.device.makeInActiveDisplayable(tmpRecordSet);
			this.application.updateStatisticsData();
			this.application.updateDataTable(this.recordSetKey1, false);

			this.device.setAverageTimeStep_ms(tmpRecordSet.getAverageTimeStep_ms());
			GathererThread.log.log(java.util.logging.Level.FINE, "set average time step msec = " + this.device.getAverageTimeStep_ms());
		}
	}

	/**
	 * cleanup all allocated resources and display the message
	 * @param this.recordSetKey
	 * @param message
	 * @param e
	 */
	void cleanup(final String message) {
		if (this.channel.get(this.recordSetKey1) != null) {
			this.channel.get(this.recordSetKey1).clear();
			this.channel.remove(this.recordSetKey1);
			if (Thread.currentThread().getId() == this.application.getThreadId()) {
				this.application.getMenuToolBar().updateRecordSetSelectCombo();
				this.application.updateStatisticsData();
				this.application.updateDataTable(this.recordSetKey1, true);
				this.application.openMessageDialog(message);
			}
			else {
				final String useRecordSetKey = this.recordSetKey1;
				GDE.display.asyncExec(new Runnable() {
					@Override
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
	
	/**
	 * build a status message covering both gathered stati
	 * @param statusCh1
	 * @param statusCh2
	 */
	void setProcessingStatus(int statusCh1, int statusCh2) {	
		this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT3466, new String[] {getStatusMessage(statusCh1), getStatusMessage(statusCh2)}));
	}
	
	/**
	 * @param status
	 * @return corresponding message
	 */
	String getStatusMessage(int status) {
		String statusMsg = GDE.STRING_EMPTY;
		switch (status) {
		case 50: //AKKU STOP
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3450);
			break;
			
		case 51: //AKKU VOLL
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3451);
			break;
			
		case 52: //AKKU LEER
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3452);
			break;
			
		case 54: //FEHLER TIMEOUT
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3454);
			break;
			
		case 55: //FEHLER LADE-MENGE
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3455);
			break;
			
		case 56: //FEHLER AKKU ZU HEISS
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3456);
			break;
			
		case 57: //FEHLER VERSORGUNGSSPANNUNG
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3457);
			break;
			
		case 58: //FEHLER AKKUSPANNUNG
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3458);
			break;
			
		case 59: //FEHLER ZELLENSPANNUNG
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3459);
			break;
			
		case 60: //FEHLER ALARMEINGANG
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3460);
			break;
			
		case 61: //FEHLER STROMREGLER
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3461);
			break;
			
		case 62: //FEHLER POLUNG/KURZSCHLUSS
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3462);
			break;
			
		case 63: //FEHLER REGELFENSTER
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3463);
			break;
			
		case 64: //FEHLER MESSFENSTER
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3464);
			break;
			
		case 65: //FEHLER TEMPERATUR
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3465);
			break;
			
		case 66: //FEHLER TEMPSENS
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3466);
			break;
			
		case 67: //FEHLER HARDWARE
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3467);
			break;
			
		default:
			statusMsg = Messages.getString(MessageIds.GDE_MSGI3468);
			break;
		}
		
		return statusMsg;
	}
}
