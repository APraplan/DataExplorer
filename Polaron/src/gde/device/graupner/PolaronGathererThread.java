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
package gde.device.graupner;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.InputTypes;
import gde.device.graupner.polaron.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

import java.util.logging.Logger;

/**
 * Thread implementation to gather data from eStation device
 * @author Winfied Brügmann
 */
public class PolaronGathererThread extends Thread {
	final static String			$CLASS_NAME						= PolaronGathererThread.class.getName();
	final static Logger			log										= Logger.getLogger(PolaronGathererThread.class.getName());
	final static int				TIME_STEP_DEFAULT			= 1000;
	final static int				WAIT_TIME_RETRYS			= 3600;																										// 3600 * 1 sec

	final DataExplorer			application;
	final PolaronSerialPort	serialPort;
	final Polaron						device;
	final Channels					channels;

	String									recordSetKey1					= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
	String									recordSetKey2					= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
	String									recordSetKey3					= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
	int											retryCounter					= PolaronGathererThread.WAIT_TIME_RETRYS;									//3600 * 1 sec = 60 Min
	boolean									isCollectDataStopped	= false;

	boolean									isProgrammExecuting1	= false;
	boolean									isProgrammExecuting2	= false;
	boolean									isProgrammExecuting3	= false;
	boolean[]								isAlerted4Finish			= { false, false, false, false };
	boolean									isLinkedMode					= false;
	boolean									isContinuousRecordSet	= Settings.getInstance().isContinuousRecordSet();
	int											lastNumberDisplayableRecords	= 0;

	/**
	 * data gatherer thread definition 
	 * @throws SerialPortException 
	 * @throws ApplicationConfigurationException 
	 * @throws Exception 
	 */
	public PolaronGathererThread() throws ApplicationConfigurationException, SerialPortException {
		super("dataGatherer"); //$NON-NLS-1$
		this.application = DataExplorer.getInstance();
		this.device = (Polaron) this.application.getActiveDevice();
		this.serialPort = this.device.getCommunicationPort();
		this.channels = Channels.getInstance();

		this.setPriority(Thread.MAX_PRIORITY);
	}

	@Override
	public void run() {
		final String $METHOD_NAME = "run"; //$NON-NLS-1$
		RecordSet recordSet1 = null;
		RecordSet recordSet2 = null;
		RecordSet recordSet3 = null;
		int[] points1 = new int[this.device.getNumberOfMeasurements(1)];
		int[] points2 = new int[this.device.getNumberOfMeasurements(2)];
		int[] points3 = new int[this.device.getNumberOfMeasurements(3)];
		byte[] dataBuffer = null;

		this.isProgrammExecuting1 = false;
		this.isProgrammExecuting2 = false;
		this.isProgrammExecuting3 = false;
		Object[] ch1, ch2, ch3;

		this.isCollectDataStopped = false;
		PolaronGathererThread.log.logp(Level.FINE, PolaronGathererThread.$CLASS_NAME, $METHOD_NAME, "====> entry initial time step ms = " + this.device.getTimeStep_ms()); //$NON-NLS-1$

		long atualTime_ms = 0;
		while (!this.isCollectDataStopped) {
			atualTime_ms = System.currentTimeMillis();
			try {
				switch (this.device.getDeviceTypeIdentifier()) {
				case PolaronSports:
					dataBuffer = this.serialPort.getData(true); // get data from device
					break;

				default:
					dataBuffer = this.serialPort.getData(); // get data from device
					break;
				}

				switch (this.device.getDeviceTypeIdentifier()) {
				default:
					this.isProgrammExecuting3 = this.device.isLinkedMode(dataBuffer);
					if (!this.isProgrammExecuting3 && !this.isLinkedMode) { // outlet channel 1+2 combined 
						this.isProgrammExecuting1 = this.device.isProcessing(1, dataBuffer);
						this.isProgrammExecuting2 = this.device.isProcessing(2, dataBuffer);
					}
					break;
				case PolaronEx1400:
					this.isProgrammExecuting3 = this.device.isLinkedMode(dataBuffer);
					if (!this.isProgrammExecuting3 && !this.isLinkedMode) { // outlet channel 1+2 combined 
						this.isProgrammExecuting1 = ((PolaronEx1400)this.device).isProcessing(1, dataBuffer);
						this.isProgrammExecuting2 = ((PolaronEx1400)this.device).isProcessing(2, dataBuffer);
					}
					break;
				case PolaronSports:
					this.isProgrammExecuting1 = ((PolaronSports)this.device).isProcessing(1, dataBuffer);
					this.isProgrammExecuting2 = ((PolaronSports)this.device).isProcessing(2, dataBuffer);
					this.isProgrammExecuting3 = false;
					break;
				}

				// check if device is ready for data capturing, discharge or charge allowed only
				// else wait for 180 seconds max. for actions
				if (this.isProgrammExecuting1 || this.isProgrammExecuting2 || this.isProgrammExecuting3) {
					switch (this.device.getDeviceTypeIdentifier()) {
					case PolaronACDC:
					case PolaronPro:
					case PolaronEx:
					case PolaronEx1400:
					case PolaronSports:
						if (this.isProgrammExecuting3) { // checks for processes active includes check state change waiting to discharge to charge
							this.isLinkedMode = true;
							ch3 = processDataChannel(3, recordSet3, this.recordSetKey3, dataBuffer, points3);
							recordSet3 = (RecordSet) ch3[0];
							this.recordSetKey3 = (String) ch3[1];
						}
						else {
							if (this.isProgrammExecuting1) { // checks for processes active includes check state change waiting to discharge to charge
								ch1 = processDataChannel(1, recordSet1, this.recordSetKey1, dataBuffer, points1);
								recordSet1 = (RecordSet) ch1[0];
								this.recordSetKey1 = (String) ch1[1];
							}
							if (this.isProgrammExecuting2) { // checks for processes active includes check state change waiting to discharge to charge
								byte[] buffer = new byte[Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO)) / 2 + 11];
								switch (this.device.getDeviceTypeIdentifier()) {
								case PolaronSports:
									System.arraycopy(dataBuffer, 0, buffer, 0, 9);//header, application, product code
									System.arraycopy(dataBuffer, 77, buffer, 9, dataBuffer.length-77); 
									if (PolaronGathererThread.log.isLoggable(Level.FINE))log.logp(Level.FINE, PolaronGathererThread.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(buffer, buffer.length));
									break;

								case PolaronEx:
									System.arraycopy(dataBuffer, 0, buffer, 0, 11);//header, application, product code
									System.arraycopy(dataBuffer, 125, buffer, 11, buffer.length-11); //160
									if (PolaronGathererThread.log.isLoggable(Level.FINE))log.logp(Level.FINE, PolaronGathererThread.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(buffer, buffer.length));
									break;

								case PolaronACDC:
								case PolaronEx1400:
									System.arraycopy(dataBuffer, 0, buffer, 0, 11);//header, application, product code
									System.arraycopy(dataBuffer, 129, buffer, 11, 130);
									if (PolaronGathererThread.log.isLoggable(Level.FINE))log.logp(Level.FINE, PolaronGathererThread.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(buffer, buffer.length));
									break;

								default:
									System.arraycopy(dataBuffer, buffer.length - 9 - 30, buffer, 0, buffer.length);
									System.arraycopy(dataBuffer, 0, buffer, 0, 11);//header, application, product code
									//log.log(Level.OFF, StringHelper.byte2Hex2CharString(dataBuffer, dataBuffer.length));
									//log.log(Level.OFF, StringHelper.byte2Hex2CharString(buffer, buffer.length));
									break;
								}
								ch2 = processDataChannel(2, recordSet2, this.recordSetKey2, buffer, points2);
								recordSet2 = (RecordSet) ch2[0];
								this.recordSetKey2 = (String) ch2[1];
							}
						}
						break;

					default:
						break;

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
					if (!this.isProgrammExecuting3 && recordSet3 != null && recordSet3.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(recordSet3.getName());
						this.isProgrammExecuting3 = false;
						recordSet3 = null;
					}
				}
				else { // no program is executing, wait for 180 seconds max. for actions
					this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI3100));
					PolaronGathererThread.log.logp(Level.FINER, PolaronGathererThread.$CLASS_NAME, $METHOD_NAME, "wait for device activation"); //$NON-NLS-1$

					if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						PolaronGathererThread.log.log(Level.FINE, "device activation timeout"); //$NON-NLS-1$
						this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI3103));
						stopDataGatheringThread(false, null);
					}

					//finalize record sets
					int processNumber = this.device.getProcessingMode(dataBuffer);
					if (recordSet1 != null && recordSet1.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(recordSet1.getName());
						this.isProgrammExecuting1 = false;
						recordSet1 = null;
						//signal processing finished, auto balancing devices like UltraDuoPlus already signaled
						//0=no processing 1=charge 2=discharge 3=delay 4=auto balance 5=error (only UltraDuoPlus devices do auto balancing when processing has been finished)
						//0=no processing 1=charge 2=discharge 3=pause 4=finished 		5=error 6=balance 11=store charge 12=store discharge
						if (processNumber == 4) {
							this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI3104, new Object[] { 1 }));
						}
					}
					if (recordSet2 != null && recordSet2.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(recordSet2.getName());
						this.isProgrammExecuting2 = false;
						recordSet2 = null;
						//signal processing finished, auto balancing devices like UltraDuoPlus already signaled
						//0=no processing 1=charge 2=discharge 3=delay 4=auto balance 5=error (only UltraDuoPlus devices do auto balancing when processing has been finished)
						//0=no processing 1=charge 2=discharge 3=pause 4=finished 		5=error 6=balance 11=store charge 12=store discharge
						if (processNumber == 4) {
							this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI3104, new Object[] { 2 }));
						}
					}
					if (recordSet3 != null && recordSet3.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(recordSet3.getName());
						this.isProgrammExecuting3 = false;
						recordSet3 = null;
						//signal processing finished, auto balancing devices like UltraDuoPlus already signaled
						//0=no processing 1=charge 2=discharge 3=delay 4=auto balance 5=error (only UltraDuoPlus devices do auto balancing when processing has been finished)
						//0=no processing 1=charge 2=discharge 3=pause 4=finished 		5=error 6=balance 11=store charge 12=store discharge
						if (processNumber == 4) {
							this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI3104, new Object[] { 3 }));
						}
					}
				}

			}
			catch (DataInconsitsentException e) {
				String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0036, new Object[] { this.getClass().getSimpleName(), $METHOD_NAME });
				if (recordSet1 != null) cleanup(recordSet1.getName(), message);
				if (recordSet2 != null) cleanup(recordSet2.getName(), message);
				if (recordSet3 != null) cleanup(recordSet3.getName(), message);
				this.stopDataGatheringThread(false, e);
			}
			catch (Throwable e) {
				// this case will be reached while NiXx Akku discharge/charge/discharge cycle
				if (e instanceof TimeOutException) {
					if (recordSet1 != null) {
						finalizeRecordSet(recordSet1.getName());
						//recordSet1 = null;
						WaitTimer.delay(1000);
					}
					if (recordSet2 != null) {
						finalizeRecordSet(recordSet2.getName());
						//recordSet2 = null;
						WaitTimer.delay(1000);
					}
					if (recordSet3 != null) {
						finalizeRecordSet(recordSet3.getName());
						//recordSet3 = null;
						WaitTimer.delay(1000);
					}
				}
				// this case will be reached while program is started, checked and the check not asap committed, stop pressed
				else if (e instanceof TimeOutException && !(this.isProgrammExecuting1 || this.isProgrammExecuting2)) {
					this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI3100));
					PolaronGathererThread.log.logp(Level.FINE, PolaronGathererThread.$CLASS_NAME, $METHOD_NAME, "wait for device activation ..."); //$NON-NLS-1$
					if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						PolaronGathererThread.log.log(Level.FINE, "device activation timeout"); //$NON-NLS-1$
						this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI3100));
						stopDataGatheringThread(false, null);
					}
					//wait for one second is over
					WaitTimer.delay(atualTime_ms + 1000 - System.currentTimeMillis());
				}
				// program end or unexpected exception occurred, stop data gathering to enable save data by user
				else {
					PolaronGathererThread.log.log(Level.FINE, "program end detected"); //$NON-NLS-1$
					stopDataGatheringThread(true, e);
				}
			}
			//wait for one second is over
			WaitTimer.delay(atualTime_ms + 1000 - System.currentTimeMillis());
		}
		this.application.setStatusMessage(""); //$NON-NLS-1$
		PolaronGathererThread.log.logp(Level.FINE, PolaronGathererThread.$CLASS_NAME, $METHOD_NAME, "======> exit"); //$NON-NLS-1$
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
	 */
	private Object[] processDataChannel(int number, RecordSet recordSet, String recordSetKey, byte[] dataBuffer, int[] points) throws DataInconsitsentException {
		final String $METHOD_NAME = "processOutlet"; //$NON-NLS-1$
		Object[] result = new Object[2];
		if (PolaronSerialPort.log.isLoggable(Level.FINER)) {
			log.logp(Level.FINER, PolaronSerialPort.$CLASS_NAME, $METHOD_NAME, number + " - " + StringHelper.byte2Hex2CharString(dataBuffer, dataBuffer.length));
		}
		// 0=no processing 1=charge 2=discharge 3=delay 4=auto balance 5=error
		int processNumber = this.device.getProcessingMode(dataBuffer);
		String processName = this.isContinuousRecordSet ? Messages.getString(MessageIds.GDE_MSGT3126) : this.device.PROCESSING_MODE[processNumber];
		if (PolaronGathererThread.log.isLoggable(Level.FINER)) {
			PolaronGathererThread.log.log(Level.FINER, "processName = " + processName + " " + processNumber);
		}
		Channel channel = this.channels.get(number);
		if (channel != null) {
			// check if a record set matching for re-use is available and prepare a new if required
			if (recordSet == null || !recordSetKey.contains(processName)) {
				this.application.setStatusMessage(""); //$NON-NLS-1$
				setRetryCounter(PolaronGathererThread.WAIT_TIME_RETRYS); // reset to 180 sec

				// record set does not exist or is out dated, build a new name and create
				StringBuilder extend = new StringBuilder();
				if (processNumber < 5) {// 0=no processing 1=charge 2=discharge 3=delay 4=auto balance 5=error
					String processingType = this.device.getProcessingType(dataBuffer);
					int cycleNumber = this.device.getCycleNumber(number, dataBuffer);

					if (processingType.length() > 3 || cycleNumber > 0) extend.append(GDE.STRING_BLANK_LEFT_BRACKET);
					if (processingType.length() > 3) extend.append(processingType);

					if (cycleNumber > 0) {
						if (processingType.equals(Messages.getString(MessageIds.GDE_MSGT3125))) {
							extend.append(GDE.STRING_COLON).append(cycleNumber);
						}
						else {
							if (processingType.length() > 0) extend.append(GDE.STRING_MESSAGE_CONCAT);
							extend.append(Messages.getString(MessageIds.GDE_MSGT3125)).append(GDE.STRING_COLON).append(cycleNumber);
						}
					}
					if (processingType.length() > 3 || cycleNumber > 0) extend.append(GDE.STRING_RIGHT_BRACKET);
				}
				recordSetKey = channel.getNextRecordSetNumber() + GDE.STRING_RIGHT_PARENTHESIS_BLANK + (this.isContinuousRecordSet ? processName : processName + extend.toString());
				recordSetKey = recordSetKey.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetKey : recordSetKey.substring(0, RecordSet.MAX_NAME_LENGTH);

				channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.application.getActiveDevice(), channel.getNumber(), true, false, true));
				channel.applyTemplateBasics(recordSetKey);
				PolaronGathererThread.log.logp(Level.FINE, PolaronGathererThread.$CLASS_NAME, $METHOD_NAME, recordSetKey + " created for channel " + channel.getName()); //$NON-NLS-1$
				recordSet = channel.get(recordSetKey);
				this.device.setTemperatureUnit(number, recordSet, dataBuffer); //°C or °F
				recordSet.setAllDisplayable();
				//channel.applyTemplate(recordSetKey, false);
				// switch the active record set if the current record set is child of active channel
				this.channels.switchChannel(channel.getNumber(), recordSetKey);
				channel.switchRecordSet(recordSetKey);
				String description = recordSet.getRecordSetDescription() + GDE.LINE_SEPARATOR + "Firmware  : " + this.device.getFirmwareVersion(dataBuffer) //$NON-NLS-1$
						+ (this.device.getBatteryMemoryNumber(number, dataBuffer) >= 1 ? "; Memory #" + this.device.getBatteryMemoryNumber(number, dataBuffer) : GDE.STRING_EMPTY); //$NON-NLS-1$
				recordSet.setRecordSetDescription(description);

				// 0=no processing 1=charge 2=discharge 3=delay 4=auto balance 5=error (only UltraDuoPlus devices do auto balancing when processing has been finished)
				// 0=no processing 1=charge 2=discharge 3=pause 4=finished 		 5=error 6=balance 11=store charge 12=store discharge
				if (processNumber == 4 && !this.isAlerted4Finish[number]) {
					this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI3104, new Object[] { number }));
					this.isAlerted4Finish[number] = true;
				}
				else {
					this.isAlerted4Finish[number] = false;
				}
			}

			if (this.isContinuousRecordSet && processNumber == 3) {
				result[0] = recordSet;
				result[1] = recordSetKey;
				return result;
			}

			recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer));

			RecordSet activeRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
			if (activeRecordSet != null && recordSet.size() > 0 && recordSet.isChildOfActiveChannel() && recordSet.equals(activeRecordSet)) {
				PolaronGathererThread.this.application.updateAllTabs(false, this.lastNumberDisplayableRecords != activeRecordSet.getConfiguredDisplayable());
				this.lastNumberDisplayableRecords = activeRecordSet.getConfiguredDisplayable();
			}

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
			PolaronGathererThread.log.logp(Level.WARNING, PolaronGathererThread.$CLASS_NAME, $METHOD_NAME, throwable.getMessage(), throwable);
		}

		this.isCollectDataStopped = true;

		if (this.serialPort != null) {
			if (this.serialPort.getXferErrors() > 0) {
				PolaronGathererThread.log.log(Level.WARNING, "During complete data transfer " + this.serialPort.getXferErrors() + " number of errors occured!"); //$NON-NLS-1$ //$NON-NLS-2$
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

			PolaronGathererThread.log.log(Level.TIME, "set average time step msec = " + this.device.getAverageTimeStep_ms()); //$NON-NLS-1$
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
					@Override
					public void run() {
						PolaronGathererThread.this.application.getMenuToolBar().updateRecordSetSelectCombo();
						PolaronGathererThread.this.application.updateStatisticsData();
						PolaronGathererThread.this.application.updateDataTable(useRecordSetKey, true);
						PolaronGathererThread.this.application.openMessageDialog(message);
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
