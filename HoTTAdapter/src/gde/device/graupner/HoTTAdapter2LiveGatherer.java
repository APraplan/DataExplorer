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

import java.io.IOException;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.data.RecordSet;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.device.graupner.hott.MessageIds;
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
 * Thread implementation to gather data from HoTTAdapter device
 * @author Winfied Brügmann
 */
public class HoTTAdapter2LiveGatherer extends HoTTAdapterLiveGatherer {
	final static Logger	logger	= Logger.getLogger(HoTTAdapter2LiveGatherer.class.getName());

	public HoTTAdapter2LiveGatherer(DataExplorer currentApplication, HoTTAdapter useDevice, HoTTAdapterSerialPort useSerialPort, HoTTAdapterDialog useDialog) throws Exception {
		super(currentApplication, useDevice, useSerialPort, useDialog);
	}

	@Override
	public void run() {
		RecordSet recordSet = null;
		int[] points = null;
		HoTTAdapterLiveGatherer.recordSets.clear();
		boolean isChannelsActive = this.serialPort.getProtocolType().equals("115200") && (this.device.getName().equals("HoTTAdapterD") || (this.device.getName().startsWith("HoTTAdapter2") && this.channels.getActiveChannelNumber() == 4));
		StringBuilder sb = new StringBuilder();
		try {
			if (!this.serialPort.isConnected()) {
				this.serialPort.open();
				this.isPortOpenedByLiveGatherer = true;
			}
			this.serialPort.isInterruptedByUser = false;
			long startTime = new Date().getTime();
			//detect sensor type running a max of 3 times getting the sensor data
			for (int i = 0; i < HoTTAdapterLiveGatherer.isSensorType.length; i++) {
				HoTTAdapterLiveGatherer.isSensorType[i] = false;
			}

			//detect master slave mode, only in slave mode data are written to receive buffer without query
			WaitTimer.delay(1000);
			if (this.serialPort.cleanInputStream() > 2) {
				HoTTAdapter.IS_SLAVE_MODE = true;
				HoTTAdapter2LiveGatherer.logger.log(Level.WARNING, "HoTTAdapter2.IS_SLAVE_MODE = " + HoTTAdapter.IS_SLAVE_MODE);
			}
			else {
				HoTTAdapter.IS_SLAVE_MODE = false;
				HoTTAdapter2LiveGatherer.logger.log(Level.FINE, "HoTTAdapter2.IS_SLAVE_MODE = " + HoTTAdapter.IS_SLAVE_MODE);

				for (int i = 0; i < 3
						&& (HoTTAdapterLiveGatherer.isSensorType[0] == false && HoTTAdapterLiveGatherer.isSensorType[1] == false && HoTTAdapterLiveGatherer.isSensorType[2] == false && HoTTAdapterLiveGatherer.isSensorType[3] == false
								&& HoTTAdapterLiveGatherer.isSensorType[4] == false && HoTTAdapterLiveGatherer.isSensorType[5] == false); i++) {
					try {
						detectSensorType();
					}
					catch (Exception e) {
						HoTTAdapter2LiveGatherer.logger.log(Level.WARNING, e.getMessage(), e);
					}
				}
				try {
					detectSensorType();
				}
				catch (Exception e) {
					HoTTAdapter2LiveGatherer.logger.log(Level.WARNING, e.getMessage(), e);
				}

				boolean[] tmpSensorType = HoTTAdapterLiveGatherer.isSensorType.clone();
				//0=isReceiver, 1=isVario, 2=isGPS, 3=isGeneral, 4=isElectric 5=isESC
				for (int i = 0; i < HoTTAdapterLiveGatherer.isSensorType.length; ++i) {
					if (sb.length() == 0 && tmpSensorType[i]) {
						sb.append("Receiver");
						tmpSensorType[i] = false;
					}
					else if (tmpSensorType[i]) {
						sb.append(GDE.STRING_COMMA_BLANK).append(
								tmpSensorType[0] ? "Receiver" : tmpSensorType[1] ? "VARIO" : tmpSensorType[2] ? "GPS" : tmpSensorType[3] ? "GENERAL" 
										: tmpSensorType[4] ? "ELECTRIC" : tmpSensorType[5] ? "AIR_ESC" : "");
						tmpSensorType[i] = false;
					}
				}
				if (HoTTAdapterLiveGatherer.log.isLoggable(Level.TIME))
					HoTTAdapterLiveGatherer.log.log(Level.TIME, sb.toString() + ", detecting sensor type takes " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));

				HoTTbinReader2.detectedSensors = Sensor.getSetFromDetected(sb.toString().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY));
				if (isChannelsActive)
					HoTTbinReader2.detectedSensors.add(Sensor.CHANNEL);
				this.application.setStatusMessage(HoTTbinReader2.detectedSensors.toString());


				//no sensor type detected, seams only receiver is connected
				if (HoTTAdapterLiveGatherer.isSensorType[1] == false && HoTTAdapterLiveGatherer.isSensorType[2] == false && HoTTAdapterLiveGatherer.isSensorType[3] == false && HoTTAdapterLiveGatherer.isSensorType[4] == false
						&& HoTTAdapterLiveGatherer.isSensorType[5] == false) {
					this.serialPort.setSensorType(this.serialPort.protocolType.ordinal() < 2 ? HoTTAdapter.SENSOR_TYPE_RECEIVER_19200 : HoTTAdapter.SENSOR_TYPE_RECEIVER_115200);
					if (HoTTAdapterLiveGatherer.isSensorType[0] == false) this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(MessageIds.GDE_MSGW2400));
				}
			}
		}
		catch (SerialPortException e) {
			this.serialPort.close();
			this.dialog.resetButtons();
			this.application.openMessageDialogAsync(this.dialog.getDialogShell(),
					Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
			return;
		}
		catch (ApplicationConfigurationException e) {
			this.serialPort.close();
			this.dialog.resetButtons();
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					HoTTAdapter2LiveGatherer.this.application.getDeviceSelectionDialog().open();
				}
			});
			return;
		}
		catch (Throwable t) {
			this.serialPort.close();
			HoTTAdapter2LiveGatherer.logger.log(Level.SEVERE, t.getMessage(), t);
			this.dialog.resetButtons();
			this.application.openMessageDialogAsync(this.dialog.getDialogShell(),
					Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { t.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + t.getMessage() }));
			return;
		}

		//set picker parameter setting sensor for altitude/climb usage (0=auto, 1=VARIO, 2=GPS, 3=GAM, 4=EAM)
		HoTTbinReader.setAltitudeClimbPickeParameter(this.device.pickerParameters, HoTTbinReader2.detectedSensors);

		this.channel = this.application.getActiveChannel();
		String recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + "live" + GDE.STRING_RIGHT_BRACKET;
		String recordSetKey = this.channel.getNextRecordSetNumber() + this.device.getRecordSetStemNameReplacement() + recordSetNameExtend;

		this.dialog.selectTab(this.device.getLastChannelNumber());
		this.channel = this.channels.get(this.device.getLastChannelNumber());
		recordSet = RecordSet.createRecordSet(recordSetKey, this.device, this.device.getLastChannelNumber(), true, true, true);
		this.channel.put(recordSetKey, recordSet);
		HoTTAdapterLiveGatherer.recordSets.put(HoTTAdapter2.Sensor.RECEIVER.value(), recordSet);
		this.channel.applyTemplate(recordSetKey, true);
		points = new int[recordSet.size()];

		this.application.getMenuToolBar().updateChannelSelector();
		this.application.getMenuToolBar().updateRecordSetSelectCombo();
		this.channels.switchChannel(this.channel.getName());
		this.channel.switchRecordSet(recordSetKey);
		this.application.setStatusMessage(HoTTbinReader2.detectedSensors.toString());
		recordSet.setRecordSetDescription(recordSet.getRecordSetDescription()
				+ String.format(" - %s Baud", this.serialPort.getProtocolType())
				+ String.format(" - Sensor: %s", HoTTbinReader2.detectedSensors.toString())
				+ (HoTTAdapter2.isAltClimbSensor(HoTTbinReader2.detectedSensors)
						? String.format(" - %s = %s", Messages.getString(MessageIds.GDE_MSGT2419), Sensor.fromOrdinal(this.device.pickerParameters.altitudeClimbSensorSelection).name())
								: ""));

		boolean isGPSdetected = false;
		Vector<Integer> queryRing = new Vector<Integer>();
		for (int i = 1; i < HoTTAdapterLiveGatherer.isSensorType.length; ++i) {
			if (HoTTAdapterLiveGatherer.isSensorType[i]) queryRing.add(i);
		}
		long measurementCount = 0;
		int lastNumberDisplayableRecords = 0;
		final long startTime = System.nanoTime() / 1000000;
		while (!this.serialPort.isInterruptedByUser) {
			if (HoTTAdapter2LiveGatherer.logger.isLoggable(Level.FINE)) HoTTAdapter2LiveGatherer.logger.log(Level.FINE, "====> entry"); //$NON-NLS-1$
			try {
				// prepare the data and add to record set
				// build the point array according curves from record set
				switch (HoTTAdapter2LiveGatherer.this.serialPort.protocolType) {
				case TYPE_19200_V3:
				case TYPE_19200_V4:
					if (HoTTAdapter.IS_SLAVE_MODE) {
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_19200); //receiver has shortest data array
						this.dataBuffer = HoTTAdapter2LiveGatherer.this.serialPort.getData(false);
						while (!this.serialPort.isInterruptedByUser && !checkContainsDataBegin(this.dataBuffer)) {
							WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
							this.dataBuffer = HoTTAdapter2LiveGatherer.this.serialPort.getData(false);
						}
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapter2LiveGatherer.this.serialPort.getData(false);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						if (checkSignature(this.dataBuffer, HoTTAdapter.SENSOR_TYPE_RECEIVER_19200)) {
							recordSet.addPoints(this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
						}
						else if (checkSignature(this.dataBuffer, HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200);
							recordSet.addPoints(this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
						}
						else if (checkSignature(this.dataBuffer, HoTTAdapter.SENSOR_TYPE_GENERAL_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_19200);
							recordSet.addPoints(this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
						}
						else if (checkSignature(this.dataBuffer, HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200);
							recordSet.addPoints(this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
						}
						else if (checkSignature(this.dataBuffer, HoTTAdapter.SENSOR_TYPE_GPS_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_19200);
							recordSet.addPoints(this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
						}
						else if (checkSignature(this.dataBuffer, HoTTAdapter.SENSOR_TYPE_VARIO_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_19200);
							recordSet.addPoints(this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
						}
					}
					else {
						//query receiver only in case other sensors doesn't get detected
						if (HoTTAdapterLiveGatherer.isSensorType[0] == true && !(HoTTAdapterLiveGatherer.isSensorType[1] || HoTTAdapterLiveGatherer.isSensorType[2] || HoTTAdapterLiveGatherer.isSensorType[3] || HoTTAdapterLiveGatherer.isSensorType[4] || HoTTAdapterLiveGatherer.isSensorType[5])) {
							try {
								//always gather receiver data first, anserRx are used to fill RXSQ, VoltageRx and TemperatureRx
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_19200);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(false);
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true));
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						if (queryRing.size() > 0 && queryRing.firstElement() == 4) {
							try {
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(false);
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true));
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						if (queryRing.size() > 0 && queryRing.firstElement() == 3) {
							try {
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_19200);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true));
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						if (queryRing.size() > 0 && queryRing.firstElement() == 5) {
							try {
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true));
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						if (queryRing.size() > 0 && queryRing.firstElement() == 2) {
							try {
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_19200);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								this.device.convertDataBytes(points, this.dataBuffer = HoTTAdapter2LiveGatherer.this.serialPort.getData(true));
								
								if (!isGPSdetected) {
									if (!isGPSdetected) {
										HoTTAdapter2.updateGpsTypeDependent(points[37]/1000, device, recordSet, -1); //-1 -> suppress time stamp update
										isGPSdetected = true;
									}
									isGPSdetected = true;
								}

							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						if (queryRing.size() > 0 && queryRing.firstElement() == 1) {
							try {
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_19200);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true));
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
					}
					break;
				case TYPE_115200:
					if (HoTTAdapterLiveGatherer.isSensorType[0]) {
						try {
							//always gather receiver data first, anserRx are used to fill RXSQ, VoltageRx and TemperatureRx
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_115200);
							for (int i = 0; i < 2 && !this.serialPort.isCheckSumOK(4, (this.dataBuffer = this.serialPort.getData())); ++i) {
								Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
							}
							this.serialPort.getDataDBM(true, this.dataBuffer);
							this.device.convertDataBytes(points, this.dataBuffer);
							Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						}
						catch (TimeOutException e) {
							// ignore and go ahead gathering sensor data
							this.serialPort.addTimeoutError();
						}
						if (isChannelsActive) {
							try {
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_SERVO_POSITION_115200);
								for (int i = 0; i < 2 && !this.serialPort.isCheckSumOK(4, (this.dataBuffer = this.serialPort.getData())); ++i) {
									Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
								}
								this.device.convertDataBytes(points, this.dataBuffer);
								Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
					}
					if (queryRing.size() > 0 && queryRing.firstElement() == 4) {
						try {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200);
							for (int i = 0; i < 2 && !this.serialPort.isCheckSumOK(4, (this.dataBuffer = this.serialPort.getData())); ++i) {
								Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
							}
							this.device.convertDataBytes(points, this.dataBuffer);
						}
						catch (TimeOutException e) {
							// ignore and go ahead gathering sensor data
							this.serialPort.addTimeoutError();
						}
					}
					if (queryRing.size() > 0 && queryRing.firstElement() == 3) {
						try {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_115200);
							for (int i = 0; i < 2 && !this.serialPort.isCheckSumOK(4, (this.dataBuffer = this.serialPort.getData())); ++i) {
								Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
							}
							this.device.convertDataBytes(points, this.dataBuffer);
						}
						catch (TimeOutException e) {
							// ignore and go ahead gathering sensor data
							this.serialPort.addTimeoutError();
						}
					}
					if (queryRing.size() > 0 && queryRing.firstElement() == 5) {
						try {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200);
							for (int i = 0; i < 2 && !this.serialPort.isCheckSumOK(4, (this.dataBuffer = this.serialPort.getData())); ++i) {
								Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
							}
							this.device.convertDataBytes(points, this.dataBuffer);
						}
						catch (TimeOutException e) {
							// ignore and go ahead gathering sensor data
							this.serialPort.addTimeoutError();
						}
					}
					if (queryRing.size() > 0 && queryRing.firstElement() == 2) {
						try {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_115200);
							for (int i = 0; i < 2 && !this.serialPort.isCheckSumOK(4, (this.dataBuffer = this.serialPort.getData())); ++i) {
								Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
							}
							this.device.convertDataBytes(points, this.dataBuffer);
							
							if (!isGPSdetected) {
								HoTTAdapter2.updateGpsTypeDependent(points[37]/1000, device, recordSet, -1); //-1 -> suppress time stamp update
								isGPSdetected = true;
							}
						}
						catch (TimeOutException e) {
							// ignore and go ahead gathering sensor data
							this.serialPort.addTimeoutError();
						}
					}
					if (queryRing.size() > 0 && queryRing.firstElement() == 1) {
						try {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_115200);
							for (int i = 0; i < 5 && !this.serialPort.isCheckSumOK(4, (this.dataBuffer = this.serialPort.getData())); ++i) {
								Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
							}
							this.device.convertDataBytes(points, this.dataBuffer);
						}
						catch (TimeOutException e) {
							// ignore and go ahead gathering sensor data
							this.serialPort.addTimeoutError();
						}
					}
					break;
				}
				recordSet.addPoints(points, System.nanoTime() / 1000000 - startTime);

				if (queryRing.size() > 1) queryRing.add(queryRing.remove(0));

				if (++measurementCount % 5 == 0) {
					for (RecordSet tmpRecordSet : HoTTAdapterLiveGatherer.recordSets.values()) {
						this.device.updateVisibilityStatus(tmpRecordSet, true);
					}
				}
				
				RecordSet activeRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
				if (activeRecordSet != null && recordSet.size() > 0 && recordSet.isChildOfActiveChannel() && recordSet.equals(activeRecordSet)) {
					HoTTAdapter2LiveGatherer.this.application.updateAllTabs(false,  lastNumberDisplayableRecords != recordSet.getConfiguredDisplayable());
					lastNumberDisplayableRecords = recordSet.getConfiguredDisplayable();
				}

				if (this.serialPort.getTimeoutErrors() > 2 && this.serialPort.getTimeoutErrors() % 10 == 0) {
					this.application.setStatusMessage(
							Messages.getString(gde.messages.MessageIds.GDE_MSGW0045, new Object[] { "TimeOutException", this.serialPort.getTimeoutErrors() + "; xferErrors = " + this.serialPort.getXferErrors() }),
							SWT.COLOR_RED);
				}
			}
			catch (DataInconsitsentException e) {
				HoTTAdapter2LiveGatherer.logger.log(Level.SEVERE, e.getMessage(), e);
				String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() });
				for (RecordSet tmpRecordSet : HoTTAdapterLiveGatherer.recordSets.values()) {
					cleanup(tmpRecordSet.getName(), message);
				}
			}
			catch (TimeOutException e) {
				HoTTAdapter2LiveGatherer.logger.log(Level.WARNING, e.getMessage());
				this.serialPort.addTimeoutError();
				this.application.setStatusMessage(
						Messages.getString(gde.messages.MessageIds.GDE_MSGW0045,
								new Object[] { e.getClass().getSimpleName(), this.serialPort.getTimeoutErrors() + "; xferErrors = " + this.serialPort.getXferErrors() }), SWT.COLOR_RED);
				WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS); //give time to settle
			}
			catch (IOException | SerialPortException e) {
				HoTTAdapter2LiveGatherer.logger.log(Level.WARNING, e.getMessage());
				this.application.openMessageDialogAsync(Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
				for (RecordSet tmpRecordSet : HoTTAdapterLiveGatherer.recordSets.values()) {
					finalizeRecordSet(tmpRecordSet);
				}
			}
			catch (Throwable e) {
				HoTTAdapter2LiveGatherer.logger.log(Level.SEVERE, e.getMessage(), e);
				this.application.openMessageDialogAsync(e.getClass().getSimpleName() + " - " + e.getMessage()); //$NON-NLS-1$
				for (RecordSet tmpRecordSet : HoTTAdapterLiveGatherer.recordSets.values()) {
					finalizeRecordSet(tmpRecordSet);
				}
			}
			WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS); //make sure we have such a pause while receiving data even we have
		}
		for (RecordSet tmpRecordSet : HoTTAdapterLiveGatherer.recordSets.values()) {
			finalizeRecordSet(tmpRecordSet);
		}
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				String toolTipText = HoTTAdapter.getImportToolTip();
				HoTTAdapter2LiveGatherer.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			}
		});
		if (HoTTAdapter2LiveGatherer.logger.isLoggable(Level.FINE)) HoTTAdapter2LiveGatherer.logger.log(Level.FINE, "exit"); //$NON-NLS-1$
	}
}
