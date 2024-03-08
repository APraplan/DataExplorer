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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.ChannelPropertyTypes;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.device.graupner.hott.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

/**
 * Thread implementation to gather data from HoTTAdapter device
 * @author Winfied Brügmann
 */
public class HoTTAdapterLiveGatherer extends Thread {
	final static Logger						log													= Logger.getLogger(HoTTAdapterLiveGatherer.class.getName());

	final DataExplorer						application;
	final HoTTAdapterSerialPort		serialPort;
	final HoTTAdapter							device;
	final HoTTAdapterDialog				dialog;
	final Channels								channels;
	Channel												channel;
	int														timeStep_ms									= 1000;
	boolean												isPortOpenedByLiveGatherer	= false;
	boolean												isSwitchedRecordSet					= false;
	boolean												isGatheredRecordSetVisible	= true;
	byte													sensorType									= (byte) 0x80;
	byte[]												dataBuffer;

	// offsets and factors are constant over thread live time
	final HashMap<String, Double>									calcValues									= new HashMap<String, Double>();

	protected static final Map<String, RecordSet>	recordSets									= new HashMap<String, RecordSet>();

	protected static final boolean								isSensorType[]							= { false, false, false, false, false, false };

	/**
	 * @param pickerParameters
	 * @throws Exception
	 */
	public HoTTAdapterLiveGatherer(DataExplorer currentApplication, HoTTAdapter useDevice, HoTTAdapterSerialPort useSerialPort, HoTTAdapterDialog useDialog) throws Exception {
		super("liveDataGatherer"); //$NON-NLS-1$
		this.application = currentApplication;
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.dialog = useDialog;
		this.channels = Channels.getInstance();
	}

	@Override
	public void run() {
		int recordSetNumber = this.channels.get(1).maxSize() + 1;
		RecordSet recordSetReceiver = null, recordSetVario = null, recordSetGPS = null, recordSetGeneral = null, recordSetElectric = null, recordSetMotorDriver = null, recordSetChannels = null;
		int[] pointsReceiver = null, pointsVario = null, pointsGPS = null, pointsGeneral = null, pointsElectric = null, pointsMotorDriver = null, pointsChannels = null;
		HoTTAdapterLiveGatherer.recordSets.clear();
		StringBuilder sb = new StringBuilder();
		EnumSet<Sensor> detectedSensors = Sensor.getSetFromSignature("Receiver");
		boolean isChannelsActivated = this.serialPort.getProtocolType().equals("115200") && Boolean.parseBoolean(this.device.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL).getValue());
		try {
			if (!this.serialPort.isConnected()) {
				this.serialPort.open();
				this.isPortOpenedByLiveGatherer = true;
				this.serialPort.cleanInputStream();
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
				HoTTAdapterLiveGatherer.log.log(Level.WARNING, "HoTTAdapter.IS_SLAVE_MODE = " + HoTTAdapter.IS_SLAVE_MODE);
			}
			else {
				HoTTAdapter.IS_SLAVE_MODE = false;
				HoTTAdapterLiveGatherer.log.log(Level.FINE, "HoTTAdapter.IS_SLAVE_MODE = " + HoTTAdapter.IS_SLAVE_MODE);

				for (int i = 0; i < 10
						&& (HoTTAdapterLiveGatherer.isSensorType[0] == false && HoTTAdapterLiveGatherer.isSensorType[1] == false && HoTTAdapterLiveGatherer.isSensorType[2] == false && HoTTAdapterLiveGatherer.isSensorType[3] == false
								&& HoTTAdapterLiveGatherer.isSensorType[4] == false && HoTTAdapterLiveGatherer.isSensorType[5] == false); i++) {
					try {
						detectSensorType();
					}
					catch (Exception e) {
						HoTTAdapterLiveGatherer.log.log(Level.WARNING, e.getMessage(), e);
					}
				}
				try {
					detectSensorType();
				}
				catch (Exception e) {
					HoTTAdapterLiveGatherer.log.log(Level.WARNING, e.getMessage(), e);
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
								tmpSensorType[0] ? "Receiver" : tmpSensorType[1] ? "VARIO" : tmpSensorType[2] ? "GPS" : tmpSensorType[3] ? "GAM" 
										: tmpSensorType[4] ? "EAM" : tmpSensorType[5] ? "ESC" : "");
						tmpSensorType[i] = false;
					}
				}
				if (isChannelsActivated)
					sb.append(GDE.STRING_COMMA_BLANK).append(Sensor.CHANNEL);
				detectedSensors = Sensor.getSetFromSignature(sb.toString().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY));
				if (HoTTAdapterLiveGatherer.log.isLoggable(Level.TIME))
					HoTTAdapterLiveGatherer.log.log(Level.TIME, sb.toString() + ", detecting sensor type takes " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
				this.application.setStatusMessage("["+sb.toString()+"]");

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
					HoTTAdapterLiveGatherer.this.application.getDeviceSelectionDialog().open();
				}
			});
			return;
		}
		catch (Throwable t) {
			this.serialPort.close();
			HoTTAdapterLiveGatherer.log.log(Level.SEVERE, t.getMessage(), t);
			this.dialog.resetButtons();
			this.application.openMessageDialogAsync(this.dialog.getDialogShell(),
					Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { t.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + t.getMessage() }));
			return;
		}

		this.channel = this.application.getActiveChannel();
		String recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + "live" + GDE.STRING_RIGHT_BRACKET;
		String recordSetKey = this.channel.getNextRecordSetNumber() + this.device.getRecordSetStemNameReplacement();

		//receiver is always - might be empty in slave modus
		this.channel = this.channels.get(1);
		recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
		recordSetReceiver = RecordSet.createRecordSet(recordSetKey, this.device, 1, true, true, true);
		this.channel.put(recordSetKey, recordSetReceiver);
		HoTTAdapterLiveGatherer.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), recordSetReceiver);
		this.channel.applyTemplate(recordSetKey, true);
		pointsReceiver = new int[recordSetReceiver.size()];
		//vario
		if (detectedSensors.contains(Sensor.VARIO)) {
			this.channel = this.channels.get(2);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
			recordSetVario = RecordSet.createRecordSet(recordSetKey, this.device, 2, true, true, true);
			this.channel.put(recordSetKey, recordSetVario);
			HoTTAdapterLiveGatherer.recordSets.put(HoTTAdapter.Sensor.VARIO.value(), recordSetVario);
			this.channel.applyTemplate(recordSetKey, true);
			pointsVario = new int[recordSetVario.size()];
		}
		//GPS
		if (detectedSensors.contains(Sensor.GPS)) {
			this.channel = this.channels.get(3);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
			recordSetGPS = RecordSet.createRecordSet(recordSetKey, this.device, 3, true, true, true);
			this.channel.put(recordSetKey, recordSetGPS);
			HoTTAdapterLiveGatherer.recordSets.put(HoTTAdapter.Sensor.GPS.value(), recordSetGPS);
			this.channel.applyTemplate(recordSetKey, true);
			pointsGPS = new int[recordSetGPS.size()];
		}
		//General
		if (detectedSensors.contains(Sensor.GAM)) {
			this.channel = this.channels.get(4);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GAM.value() + recordSetNameExtend;
			recordSetGeneral = RecordSet.createRecordSet(recordSetKey, this.device, 4, true, true, true);
			this.channel.put(recordSetKey, recordSetGeneral);
			HoTTAdapterLiveGatherer.recordSets.put(HoTTAdapter.Sensor.GAM.value(), recordSetGeneral);
			this.channel.applyTemplate(recordSetKey, true);
			pointsGeneral = new int[recordSetGeneral.size()];
		}
		//Electric
		if (detectedSensors.contains(Sensor.EAM)) {
			this.channel = this.channels.get(5);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.EAM.value() + recordSetNameExtend;
			recordSetElectric = RecordSet.createRecordSet(recordSetKey, this.device, 5, true, true, true);
			this.channel.put(recordSetKey, recordSetElectric);
			HoTTAdapterLiveGatherer.recordSets.put(HoTTAdapter.Sensor.EAM.value(), recordSetElectric);
			this.channel.applyTemplate(recordSetKey, true);
			pointsElectric = new int[recordSetElectric.size()];
		}
		//Channels
		if (isChannelsActivated) {
			this.channel = this.channels.get(6);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.CHANNEL.value() + recordSetNameExtend;
			recordSetChannels = RecordSet.createRecordSet(recordSetKey, this.device, 6, true, true, true);
			this.channel.put(recordSetKey, recordSetChannels);
			HoTTAdapterLiveGatherer.recordSets.put(HoTTAdapter.Sensor.CHANNEL.value(), recordSetChannels);
			this.channel.applyTemplate(recordSetKey, true);
			pointsChannels = new int[recordSetChannels.size()];
			detectedSensors.add(Sensor.CHANNEL);
		}
		//SpeedControl
		if (detectedSensors.contains(Sensor.ESC)) {
			this.channel = this.channels.get(7);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ESC.value() + recordSetNameExtend;
			recordSetMotorDriver = RecordSet.createRecordSet(recordSetKey, this.device, 7, true, true, true);
			this.channel.put(recordSetKey, recordSetMotorDriver);
			HoTTAdapterLiveGatherer.recordSets.put(HoTTAdapter.Sensor.ESC.value(), recordSetMotorDriver);
			this.channel.applyTemplate(recordSetKey, true);
			pointsMotorDriver = new int[recordSetMotorDriver.size()];
		}
		this.application.getMenuToolBar().updateChannelSelector();
		this.application.getMenuToolBar().updateRecordSetSelectCombo();
		int activeChannelNumber = this.channels.getActiveChannelNumber();

		//0=isReceiver, 1=isVario, 2=isGPS, 3=isGeneral, 4=isElectric 5=Channels 6=ESC
		if (HoTTAdapterLiveGatherer.isSensorType[0] && activeChannelNumber == 1) {
			this.dialog.selectTab(1);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
		}
		else if (HoTTAdapterLiveGatherer.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()] && activeChannelNumber == 2) {
			this.dialog.selectTab(2);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
		}
		else if (HoTTAdapterLiveGatherer.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()] && activeChannelNumber == 3) {
			this.dialog.selectTab(3);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
		}
		else if (HoTTAdapterLiveGatherer.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()] && activeChannelNumber == 4) {
			this.dialog.selectTab(4);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GAM.value() + recordSetNameExtend;
		}
		else if (HoTTAdapterLiveGatherer.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()] && activeChannelNumber == 5) {
			this.dialog.selectTab(5);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.EAM.value() + recordSetNameExtend;
		}
		else if (isChannelsActivated && activeChannelNumber == 6) {
			this.dialog.selectTab(6);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.CHANNEL.value() + recordSetNameExtend;
		}
		else if (HoTTAdapterLiveGatherer.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()] && activeChannelNumber == 7) {
			this.dialog.selectTab(7);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ESC.value() + recordSetNameExtend;
		}
		this.channel.switchRecordSet(recordSetKey);
		this.channels.switchChannel(this.channel.getName());
		this.application.setStatusMessage(String.format("Sensor: %s", detectedSensors.toString()));
		recordSetReceiver.setRecordSetDescription(recordSetReceiver.getRecordSetDescription() 
				+ String.format(" - %s Baud", this.serialPort.getProtocolType())
				+ String.format(" - Sensor: %s", detectedSensors.toString()));

		boolean isGPSdetected = false;
		Vector<Integer> queryRing = new Vector<Integer>();
		for (int i = 1; i < HoTTAdapterLiveGatherer.isSensorType.length; ++i) {
			if (HoTTAdapterLiveGatherer.isSensorType[i]) queryRing.add(i);
		}
		long measurementCount = 0;
		int lastNumberDisplayableRecords = 0;
		final long startTime = System.nanoTime() / 1000000;
		while (!this.serialPort.isInterruptedByUser) {
			if (HoTTAdapterLiveGatherer.log.isLoggable(Level.FINE)) HoTTAdapterLiveGatherer.log.log(Level.FINE, "====> entry"); //$NON-NLS-1$
			try {
				// prepare the data and add to record set
				// build the point array according curves from record set
				switch (HoTTAdapterLiveGatherer.this.serialPort.protocolType) {
				case TYPE_19200_V3:
				case TYPE_19200_V4:
					if (HoTTAdapter.IS_SLAVE_MODE) {
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_19200); //receiver has shortest data array
						this.dataBuffer = HoTTAdapterLiveGatherer.this.serialPort.getData(false);
						while (!checkContainsDataBegin(this.dataBuffer))
							this.dataBuffer = HoTTAdapterLiveGatherer.this.serialPort.getData(false);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						if (checkSignature(this.dataBuffer, HoTTAdapter.SENSOR_TYPE_RECEIVER_19200)) {
							recordSetReceiver.addPoints(this.device.convertDataBytes(pointsReceiver, HoTTAdapterLiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
							switchRecordSetDisplay(HoTTAdapter.Sensor.RECEIVER, recordSetNumber, recordSetNameExtend);
						}
						else if (checkSignature(this.dataBuffer, HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200);
							recordSetElectric.addPoints(this.device.convertDataBytes(pointsElectric, HoTTAdapterLiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
							switchRecordSetDisplay(HoTTAdapter.Sensor.EAM, recordSetNumber, recordSetNameExtend);
						}
						else if (checkSignature(this.dataBuffer, HoTTAdapter.SENSOR_TYPE_GENERAL_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_19200);
							recordSetGeneral.addPoints(this.device.convertDataBytes(pointsGeneral, HoTTAdapterLiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
							switchRecordSetDisplay(HoTTAdapter.Sensor.GAM, recordSetNumber, recordSetNameExtend);
						}
						else if (checkSignature(this.dataBuffer, HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200);
							recordSetMotorDriver.addPoints(this.device.convertDataBytes(pointsMotorDriver, HoTTAdapterLiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
							switchRecordSetDisplay(HoTTAdapter.Sensor.ESC, recordSetNumber, recordSetNameExtend);
						}
						else if (checkSignature(this.dataBuffer, HoTTAdapter.SENSOR_TYPE_GPS_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_19200);
							recordSetGPS.addPoints(this.device.convertDataBytes(pointsGPS, HoTTAdapterLiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
							switchRecordSetDisplay(HoTTAdapter.Sensor.GPS, recordSetNumber, recordSetNameExtend);
						}
						else if (checkSignature(this.dataBuffer, HoTTAdapter.SENSOR_TYPE_VARIO_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_19200);
							recordSetVario.addPoints(this.device.convertDataBytes(pointsVario, HoTTAdapterLiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
							switchRecordSetDisplay(HoTTAdapter.Sensor.VARIO, recordSetNumber, recordSetNameExtend);
						}

					}
					else {
						//query receiver only in case other sensors doesn't get detected
						if (HoTTAdapterLiveGatherer.isSensorType[0] == true && !(HoTTAdapterLiveGatherer.isSensorType[1] || HoTTAdapterLiveGatherer.isSensorType[2] || HoTTAdapterLiveGatherer.isSensorType[3] || HoTTAdapterLiveGatherer.isSensorType[4] || HoTTAdapterLiveGatherer.isSensorType[5])) {
							try {
								//always gather receiver data first, anserRx are used to fill RXSQ, VoltageRx and TemperatureRx
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_19200);
								HoTTAdapterLiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								recordSetReceiver.addPoints(this.device.convertDataBytes(pointsReceiver, HoTTAdapterLiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						if (queryRing.size() > 0 && queryRing.firstElement() == 4) {
							try {
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200);
								HoTTAdapterLiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								byte[] data = HoTTAdapterLiveGatherer.this.serialPort.getData(true);
								recordSetElectric.addPoints(this.device.convertDataBytes(pointsElectric, data), System.nanoTime() / 1000000 - startTime);
								data[1] = HoTTAdapter.SENSOR_TYPE_RECEIVER_19200;
								recordSetReceiver.addPoints(this.device.convertDataBytes(pointsReceiver, data), System.nanoTime() / 1000000 - startTime);
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						if (queryRing.size() > 0 && queryRing.firstElement() == 3) {
							try {
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_19200);
								HoTTAdapterLiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								byte[] data = HoTTAdapterLiveGatherer.this.serialPort.getData(true);
								recordSetGeneral.addPoints(this.device.convertDataBytes(pointsGeneral, data), System.nanoTime() / 1000000 - startTime);
								data[1] = HoTTAdapter.SENSOR_TYPE_RECEIVER_19200;
								recordSetReceiver.addPoints(this.device.convertDataBytes(pointsReceiver, data), System.nanoTime() / 1000000 - startTime);
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						if (queryRing.size() > 0 && queryRing.firstElement() == 5) {
							try {
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200);
								HoTTAdapterLiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								byte[] data = HoTTAdapterLiveGatherer.this.serialPort.getData(true);
								recordSetMotorDriver.addPoints(this.device.convertDataBytes(pointsMotorDriver, data), System.nanoTime() / 1000000 - startTime);
								data[1] = HoTTAdapter.SENSOR_TYPE_RECEIVER_19200;
								recordSetReceiver.addPoints(this.device.convertDataBytes(pointsReceiver, data), System.nanoTime() / 1000000 - startTime);
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						if (queryRing.size() > 0 && queryRing.firstElement() == 2) {
							try {
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_19200);
								HoTTAdapterLiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								byte[] data = HoTTAdapterLiveGatherer.this.serialPort.getData(true);
								recordSetGPS.addPoints(this.device.convertDataBytes(pointsGPS, data), System.nanoTime() / 1000000 - startTime);
								data[1] = HoTTAdapter.SENSOR_TYPE_RECEIVER_19200;
								recordSetReceiver.addPoints(this.device.convertDataBytes(pointsReceiver, data), System.nanoTime() / 1000000 - startTime);
								
								if (!isGPSdetected) {
									HoTTAdapter.updateGpsTypeDependent(pointsGPS[23]/1000, device, recordSetGPS, -1); //-1 -> suppress time stamp update
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
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_19200);
								HoTTAdapterLiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
								byte[] data = HoTTAdapterLiveGatherer.this.serialPort.getData(true);
								recordSetVario.addPoints(this.device.convertDataBytes(pointsVario, data), System.nanoTime() / 1000000 - startTime);
								data[1] = HoTTAdapter.SENSOR_TYPE_RECEIVER_19200;
								recordSetReceiver.addPoints(this.device.convertDataBytes(pointsReceiver, data), System.nanoTime() / 1000000 - startTime);
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
					}
					break;
				case TYPE_115200:
					if (!HoTTAdapter.IS_SLAVE_MODE || HoTTAdapterLiveGatherer.isSensorType[0] == true) {
						try {
							//always gather receiver data first, anserRx are used to fill RXSQ, VoltageRx and TemperatureRx
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_115200);
							for (int i = 0; i < 2 && !this.serialPort.isCheckSumOK(4, (this.dataBuffer = this.serialPort.getData())); ++i) {
								Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
							}
							this.serialPort.getDataDBM(true, this.dataBuffer);
							recordSetReceiver.addPoints(this.device.convertDataBytes(pointsReceiver, this.dataBuffer), System.nanoTime() / 1000000 - startTime);
							Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						}
						catch (TimeOutException e) {
							// ignore and go ahead gathering sensor data
							this.serialPort.addTimeoutError();
						}
						if (isChannelsActivated) {
							try {
								this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_SERVO_POSITION_115200);
								for (int i = 0; i < 2 && !this.serialPort.isCheckSumOK(4, (this.dataBuffer = this.serialPort.getData())); ++i) {
									Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
								}
								recordSetChannels.addPoints(this.device.convertDataBytes(pointsChannels, this.dataBuffer), System.nanoTime() / 1000000 - startTime);
								Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						/* switch change detection
						try {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_CONTROL_1_115200);
							for (int i = 0; i < 5 && !this.serialPort.isCheckSumOK(4, (this.dataBuffer = this.serialPort.getData())); ++i) {
								Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
							}
							System.out.println("Control positions 1 : ");
							for (int i = 161; i < this.dataBuffer.length - 2; i++) { //start 161
								System.out.print(StringHelper.printBinary(this.dataBuffer[i], false));
							}
							System.out.println();
							String swS = String.format("S%02d changed!", setSwitchS());
							if (!swS.substring(1, 3).equals("00")) System.out.println(swS);
						
							for (int i = 0, j = 1; i < 2; i++) {
								for (int k = 1; j <= (i + 1) * 8; j++, k *= 2) {
									System.out.print(String.format("S%02d : %d; ", j, ((this.dataBuffer[i + 161] & k) != 0 ? 1 : 0)));
								}
							}
							System.out.println();
							for (int i = 0, j = 1; i < 1; i++) {
								for (int k = 1; j <= (i + 1) * 8; j++, k *= 2) {
									System.out.print(String.format("G%02d : %d; ", j, ((this.dataBuffer[i + 169] & k) != 0 ? 1 : 0)));
								}
							}
							System.out.println();
							String swG = String.format("G%02d changed!", setSwitchG());
							if (!swG.substring(1, 3).equals("00")) System.out.println(swG);
						
							for (int i = 0, j = 1; i < 1; i++) {
								for (int k = 1; j <= (i + 1) * 8; j++, k *= 2) {
									System.out.print(String.format("L%02d : %d; ", j, ((this.dataBuffer[i + 173] & k) != 0 ? 1 : 0)));
								}
							}
							System.out.println();
							if (setSwitchL() > 0) System.out.println(String.format("L%02d changed!", setSwitchL()));
						}
						catch (Exception e) {
							e.printStackTrace();
						}
						*/
						/* transmitter voltage detection
						try {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_CONTROL_2_115200);
							for (int i = 0; i < 5 && !this.serialPort.isCheckSumOK(4, (this.dataBuffer = this.serialPort.getData())); ++i) {
								Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
							}
						}
						catch (Exception e) {
							e.printStackTrace();
						}
						System.out.println("Control positions 2 : ");
						System.out.println(StringHelper.byte2Hex2CharString(this.dataBuffer, this.dataBuffer.length - 2));
						if (24 < this.dataBuffer.length - 1) System.out.println(String.format("transmitter voltage %.2f V", this.dataBuffer[24] / 10.0f));
						
						if (26 < this.dataBuffer.length - 1) {
							System.out.println(StringHelper.printBinary(this.dataBuffer[26], false));
						}
						*/
					}
					if (queryRing.size() > 0 && queryRing.firstElement() == 4) {
						try {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200);
							for (int i = 0; i < 2 && !this.serialPort.isCheckSumOK(4, (this.dataBuffer = this.serialPort.getData())); ++i) {
								Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
							}
							this.serialPort.getDataDBM(false, this.dataBuffer);
							recordSetElectric.addPoints(this.device.convertDataBytes(pointsElectric, this.dataBuffer), System.nanoTime() / 1000000 - startTime);
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
							this.serialPort.getDataDBM(false, this.dataBuffer);
							recordSetGeneral.addPoints(this.device.convertDataBytes(pointsGeneral, this.dataBuffer), System.nanoTime() / 1000000 - startTime);
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
							this.serialPort.getDataDBM(false, this.dataBuffer);
							recordSetMotorDriver.addPoints(this.device.convertDataBytes(pointsMotorDriver, this.dataBuffer), System.nanoTime() / 1000000 - startTime);
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
							this.serialPort.getDataDBM(false, this.dataBuffer);
							recordSetGPS.addPoints(this.device.convertDataBytes(pointsGPS, this.dataBuffer), System.nanoTime() / 1000000 - startTime);
							
							if (!isGPSdetected) {
								HoTTAdapter.updateGpsTypeDependent(pointsGPS[23]/1000, device, recordSetGPS, -1); //-1 -> suppress time stamp update
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
							for (int i = 0; i < 2 && !this.serialPort.isCheckSumOK(4, (this.dataBuffer = this.serialPort.getData())); ++i) {
								Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
							}
							this.serialPort.getDataDBM(false, this.dataBuffer);
							recordSetVario.addPoints(this.device.convertDataBytes(pointsVario, this.dataBuffer), System.nanoTime() / 1000000 - startTime);
						}
						catch (TimeOutException e) {
							// ignore and go ahead gathering sensor data
							this.serialPort.addTimeoutError();
						}
					}
					break;
				}
				if (queryRing.size() > 1) queryRing.add(queryRing.remove(0));

				if (++measurementCount % 5 == 0) {
					for (RecordSet recordSet : HoTTAdapterLiveGatherer.recordSets.values()) {
						this.device.updateVisibilityStatus(recordSet, true);
					}
				}
				RecordSet activeRecordSet = this.application.getActiveRecordSet();
				if (activeRecordSet != null && activeRecordSet.size() > 0 && activeRecordSet.isChildOfActiveChannel() && activeRecordSet.equals(activeRecordSet)) {
					HoTTAdapterLiveGatherer.this.application.updateAllTabs(false, lastNumberDisplayableRecords != activeRecordSet.getConfiguredDisplayable());
					lastNumberDisplayableRecords = activeRecordSet.getConfiguredDisplayable();
				}

				if (this.serialPort.getTimeoutErrors() > 2 && this.serialPort.getTimeoutErrors() % 10 == 0) {
					this.application.setStatusMessage(
							Messages.getString(gde.messages.MessageIds.GDE_MSGW0045, new Object[] { "TimeOutException", this.serialPort.getTimeoutErrors() + "; xferErrors = " + this.serialPort.getXferErrors() }),
							SWT.COLOR_RED);
				}
			}
			catch (DataInconsitsentException e) {
				HoTTAdapterLiveGatherer.log.log(Level.SEVERE, e.getMessage(), e);
				String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() });
				for (RecordSet recordSet : HoTTAdapterLiveGatherer.recordSets.values()) {
					cleanup(recordSet.getName(), message);
				}
			}
			catch (TimeOutException e) {
				HoTTAdapterLiveGatherer.log.log(Level.WARNING, e.getMessage());
				this.serialPort.addTimeoutError();
				this.application.setStatusMessage(
						Messages.getString(gde.messages.MessageIds.GDE_MSGW0045,
								new Object[] { e.getClass().getSimpleName(), this.serialPort.getTimeoutErrors() + "; xferErrors = " + this.serialPort.getXferErrors() }), SWT.COLOR_RED);
				WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS); //give time to settle
			}
			catch (IOException | SerialPortException e) {
				HoTTAdapterLiveGatherer.log.log(Level.WARNING, e.getMessage());
				this.application.openMessageDialogAsync(Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
				for (RecordSet recordSet : HoTTAdapterLiveGatherer.recordSets.values()) {
					finalizeRecordSet(recordSet);
				}
			}
			catch (Throwable e) {
				HoTTAdapterLiveGatherer.log.log(Level.SEVERE, e.getMessage(), e);
				this.application.openMessageDialogAsync(e.getClass().getSimpleName() + " - " + e.getMessage()); //$NON-NLS-1$
				for (RecordSet recordSet : HoTTAdapterLiveGatherer.recordSets.values()) {
					finalizeRecordSet(recordSet);
				}
			}
			WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS); //make sure we have such a pause while receiving data even we have
		}
		for (RecordSet recordSet : HoTTAdapterLiveGatherer.recordSets.values()) {
			finalizeRecordSet(recordSet);
		}
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				String toolTipText = HoTTAdapter.getImportToolTip();
				HoTTAdapterLiveGatherer.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			}
		});
		if (HoTTAdapterLiveGatherer.log.isLoggable(Level.FINE)) HoTTAdapterLiveGatherer.log.log(Level.FINE, "exit"); //$NON-NLS-1$
	}

	/**
	 * switch display to active sensor
	 * @param sensor
	 * @param recordSetNumber
	 * @param recordSetNameExtend
	 *
	 */
	public void switchRecordSetDisplay(HoTTAdapter.Sensor sensor, int recordSetNumber, String recordSetNameExtend) {
		if (this.channels.getActiveChannelNumber() != sensor.ordinal() + 1) {
			this.dialog.selectTab(sensor.ordinal() + 1);
			this.channel.switchRecordSet(recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + sensor.value() + recordSetNameExtend);
		}
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 * @param recordSet
	 */
	public void finalizeRecordSet(RecordSet recordSet) {
		this.serialPort.isInterruptedByUser = true;
		this.serialPort.close();
		this.device.updateVisibilityStatus(recordSet, false);
		this.device.makeInActiveDisplayable(recordSet);
		this.application.updateStatisticsData();
		this.application.updateDataTable(recordSet.getName(), false);
		this.device.getDialog().resetButtons();
	}

	/**
	 * cleanup all allocated resources and display the message
	 * @param recordSetKey
	 * @param message
	 */
	void cleanup(final String recordSetKey, String message) {
		this.serialPort.isInterruptedByUser = true;
		this.serialPort.close();
		this.channel.get(recordSetKey).clear();
		this.channel.switchRecordSet(recordSetKey);
		this.channel.remove(recordSetKey);
		this.application.getMenuToolBar().updateRecordSetSelectCombo();
		this.application.updateStatisticsData();
		this.application.updateDataTable(recordSetKey, true);
		this.device.getDialog().resetButtons();
		if (message != null && message.length() > 5) {
			this.application.openMessageDialog(this.dialog.getDialogShell(), message);
		}
	}

	/**
	 * detect Sensor data, retrieves data only on not detected sensors driven by boolean[] isSensorType
	 * @param isSensorType
	 * @return points integer array of data points, to enable 3 decimal digits value is multiplied by 1000
	 * @throws Exception
	 */
	void detectSensorType() throws Exception {

		switch (HoTTAdapterLiveGatherer.this.serialPort.protocolType) {
		case TYPE_19200_V3:
			if (!this.serialPort.isInterruptedByUser) {
				if (!HoTTAdapterLiveGatherer.isSensorType[1]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ Vario");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_19200);
						this.serialPort.getData(false);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						this.dataBuffer = this.serialPort.getData(true);
						HoTTAdapterLiveGatherer.isSensorType[1] = (DataParser.parse2Short(this.dataBuffer, 16) != 0 || this.dataBuffer[22] != 0);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						//ignore and go ahead detecting sensors
					}
				}
				if (!HoTTAdapterLiveGatherer.isSensorType[2]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ GPS");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_19200);
						this.serialPort.getData(false);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						this.dataBuffer = this.serialPort.getData(true);
						HoTTAdapterLiveGatherer.isSensorType[2] = (this.dataBuffer[37] == 0x01 || (this.dataBuffer[20] != 0 && this.dataBuffer[21] != 0 && this.dataBuffer[25] != 0 && this.dataBuffer[26] != 0));
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						//ignore and go ahead detecting sensors
					}
				}
				if (!HoTTAdapterLiveGatherer.isSensorType[3]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ General");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_19200);
						this.serialPort.getData(false);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						this.dataBuffer = this.serialPort.getData(true);
						HoTTAdapterLiveGatherer.isSensorType[3] = (DataParser.parse2Short(this.dataBuffer, 40) != 0);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						//ignore and go ahead detecting sensors
					}
				}
				if (!HoTTAdapterLiveGatherer.isSensorType[4]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ Electric");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200);
						this.serialPort.getData(false);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						this.dataBuffer = this.serialPort.getData(true);
						HoTTAdapterLiveGatherer.isSensorType[4] = (DataParser.parse2Short(this.dataBuffer, 40) != 0);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						//ignore and go ahead detecting sensors
					}
				}
				if (HoTTAdapterLiveGatherer.isSensorType[1] || HoTTAdapterLiveGatherer.isSensorType[2] || HoTTAdapterLiveGatherer.isSensorType[3] || HoTTAdapterLiveGatherer.isSensorType[4]) HoTTAdapterLiveGatherer.isSensorType[0] = true;
				if (!HoTTAdapterLiveGatherer.isSensorType[0]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ Receiver");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_19200);
						this.serialPort.getData(false);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						this.dataBuffer = this.serialPort.getData(true);
						HoTTAdapterLiveGatherer.isSensorType[0] = (this.dataBuffer[9] != 0 && this.dataBuffer[6] != 0);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						//ignore and go ahead detecting sensors
					}
				}
			}
			break;

		case TYPE_19200_V4:
			if (!this.serialPort.isInterruptedByUser) {
				//V4 has multi sensor capability which might need more queries for stable result
				if (!HoTTAdapterLiveGatherer.isSensorType[1]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ Vario");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_19200);
						this.serialPort.getData(false);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS*3);
						this.serialPort.getData(true);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS*3);
						this.serialPort.getData(true);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS*3);
						HoTTAdapterLiveGatherer.isSensorType[1] = (this.serialPort.getData(true)[15] == HoTTAdapter.ANSWER_SENSOR_VARIO_19200);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS*3);
					}
					catch (Exception e) {
						//ignore and go ahead detecting sensors
					}
				}
				if (!HoTTAdapterLiveGatherer.isSensorType[2]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ GPS");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_19200);
//						int dataLength = 0, dataLengthLast = 0;
//						while (dataLengthLast != (dataLength = this.serialPort.getDataSize())) {
//							System.out.println("dataLength = " + dataLength);
//							dataLengthLast = dataLength;
//						}
						this.serialPort.getData(false);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						this.serialPort.getData(true);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						this.serialPort.getData(true);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapterLiveGatherer.isSensorType[2] = (this.serialPort.getData(true)[15] == HoTTAdapter.ANSWER_SENSOR_GPS_19200);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						//ignore and go ahead detecting sensors
					}
				}
				if (!HoTTAdapterLiveGatherer.isSensorType[3]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ General");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_19200);
						this.serialPort.getData(false);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						this.serialPort.getData(true);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						this.serialPort.getData(true);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapterLiveGatherer.isSensorType[3] = (this.serialPort.getData(true)[15] == HoTTAdapter.ANSWER_SENSOR_GENERAL_19200);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						//ignore and go ahead detecting sensors
					}
				}
				if (!HoTTAdapterLiveGatherer.isSensorType[4]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ Electric");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200);
						this.serialPort.getData(false);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						this.serialPort.getData(true);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						this.serialPort.getData(true);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapterLiveGatherer.isSensorType[4] = (this.serialPort.getData(true)[15] == HoTTAdapter.ANSWER_SENSOR_ELECTRIC_19200);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						//ignore and go ahead detecting sensors
					}
				}
				if (!HoTTAdapterLiveGatherer.isSensorType[5]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ SpeedControler");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200);
						this.serialPort.getData(false);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS*3);
						this.serialPort.getData(true);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS*3);
						this.serialPort.getData(true);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS*3);
						HoTTAdapterLiveGatherer.isSensorType[5] = (this.serialPort.getData(true)[15] == HoTTAdapter.ANSWER_SENSOR_MOTOR_DRIVER_19200);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS*5);
					}
					catch (Exception e) {
						//ignore and go ahead detecting sensors
					}
				}
				if (HoTTAdapterLiveGatherer.isSensorType[1] || HoTTAdapterLiveGatherer.isSensorType[2] || HoTTAdapterLiveGatherer.isSensorType[3] || HoTTAdapterLiveGatherer.isSensorType[4] || HoTTAdapterLiveGatherer.isSensorType[5])
					HoTTAdapterLiveGatherer.isSensorType[0] = true;
				if (!HoTTAdapterLiveGatherer.isSensorType[0]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ Receiver");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_19200);
						this.serialPort.getData(false);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS*5);
						this.serialPort.getData(true);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS*5);
						this.serialPort.getData(true);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS*5);
						byte[] buffer = this.serialPort.getData(true);
						HoTTAdapterLiveGatherer.isSensorType[0] = (buffer[2] == 0x7C && buffer[15] == 0x7D);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS*5);
					}
					catch (Exception e) {
						//ignore and go ahead detecting sensors
					}
				}
			}
			break;

		case TYPE_115200:
			if (!this.serialPort.isInterruptedByUser) {
				//V4 has multi sensor capability which might need more queries for stable result
				if (!HoTTAdapterLiveGatherer.isSensorType[0]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ Receiver");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_115200);
						for (int i = 0; i < 10; ++i) {
							try {
								if (this.serialPort.isCheckSumOK(4, this.dataBuffer = this.serialPort.getData())) break;
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
							}
							catch (final Exception e) {
								WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS * 2);
								// ignore, go ahead with data gathering
							}
						}
						HoTTAdapterLiveGatherer.isSensorType[0] = (dataBuffer[17] != 0 && dataBuffer[15] != 0);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (final Exception e) {
						// ignore, go ahead with data gathering
					}
				}
				if (!HoTTAdapterLiveGatherer.isSensorType[5]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ SpeedControler");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200);
						for (int i = 0; i < 5 && !this.serialPort.isCheckSumOK(4, this.dataBuffer = this.serialPort.getData()); ++i) {
							Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						}
						//this.dataBuffer = this.serialPort.getData();
						//Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapterLiveGatherer.isSensorType[5] = DataParser.parse2Short(this.dataBuffer, 22) != 0 || DataParser.parse2Short(this.dataBuffer, 10) != 0;
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage());
					}
				}
				if (!HoTTAdapterLiveGatherer.isSensorType[4]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ Electric");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200);
						for (int i = 0; i < 5 && !this.serialPort.isCheckSumOK(4, this.dataBuffer = this.serialPort.getData()); ++i) {
							Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						}
						//this.dataBuffer = this.serialPort.getData();
						//Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapterLiveGatherer.isSensorType[4] = DataParser.parse2Short(this.dataBuffer, 50) != 0 || DataParser.parse2Short(this.dataBuffer, 42) != 0 || DataParser.parse2Short(this.dataBuffer, 44) != 0;
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						//ignore and go ahead detecting sensors
					}
				}
				if (!HoTTAdapterLiveGatherer.isSensorType[3]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ General");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_115200);
						for (int i = 0; i < 5 && !this.serialPort.isCheckSumOK(4, this.dataBuffer = this.serialPort.getData()); ++i) {
							Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						}
						//this.dataBuffer = this.serialPort.getData();
						//Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapterLiveGatherer.isSensorType[3] = DataParser.parse2Short(this.dataBuffer, 36) != 0 || DataParser.parse2Short(this.dataBuffer, 26) != 0 || DataParser.parse2Short(this.dataBuffer, 28) != 0;
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage());
					}
				}
				if (!HoTTAdapterLiveGatherer.isSensorType[2]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ GPS");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_115200);
						for (int i = 0; i < 5 && !this.serialPort.isCheckSumOK(4, this.dataBuffer = this.serialPort.getData()); ++i) {
							Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						}
						//this.dataBuffer = this.serialPort.getData();
						//Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapterLiveGatherer.isSensorType[2] = (this.dataBuffer[31] != 0 || (this.dataBuffer[16] != 0 && this.dataBuffer[17] != 0 && this.dataBuffer[20] != 0 && this.dataBuffer[21] != 0));
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage());
					}
				}
				if (!HoTTAdapterLiveGatherer.isSensorType[1]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ Vario");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_115200);
						for (int i = 0; i < 5 && !this.serialPort.isCheckSumOK(4, this.dataBuffer = this.serialPort.getData()); ++i) {
							Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						}
						//this.dataBuffer = this.serialPort.getData();
						//Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapterLiveGatherer.isSensorType[1] = (DataParser.parse2Short(dataBuffer, 10) != 0 || dataBuffer[16] != 0);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage());
					}
				}
				if (HoTTAdapterLiveGatherer.isSensorType[1] || HoTTAdapterLiveGatherer.isSensorType[2] || HoTTAdapterLiveGatherer.isSensorType[3] || HoTTAdapterLiveGatherer.isSensorType[4] || HoTTAdapterLiveGatherer.isSensorType[5])
					HoTTAdapterLiveGatherer.isSensorType[0] = true;
				if (!HoTTAdapterLiveGatherer.isSensorType[0]) {
					try {
						HoTTAdapterLiveGatherer.log.log(Level.FINE, "------------ Receiver");
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_115200);
						for (int i = 0; i < 5 && !this.serialPort.isCheckSumOK(4, this.dataBuffer = this.serialPort.getData()); ++i) {
							Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						}
						//this.dataBuffer = this.serialPort.getData();
						//Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapterLiveGatherer.isSensorType[0] = (dataBuffer[17] != 0 && dataBuffer[15] != 0);
						Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage());
					}
				}
			}
			break;
		}
	}

	/**
	 * check for begin and end signature in data buffer 19200 V3 protocol
	 * @param dataBuffer0
	 * @param dataBuffer1
	 * @return
	 */
	protected boolean checkSignature(byte[] dataBuffer0, byte[] dataBuffer1) {
		boolean isDataSignature = false;
		byte[] tmpBuffer = new byte[dataBuffer0.length * 2];
		System.arraycopy(dataBuffer0, 0, tmpBuffer, 0, dataBuffer0.length);
		System.arraycopy(dataBuffer1, 0, tmpBuffer, dataBuffer0.length, dataBuffer1.length);

		for (int i = 0; i < tmpBuffer.length; i++) {
			if (tmpBuffer[i] == HoTTAdapterSerialPort.DATA_BEGIN) {
				int endIndex = i + dataBuffer0.length - 4;
				isDataSignature = endIndex < tmpBuffer.length && tmpBuffer[endIndex] == HoTTAdapterSerialPort.DATA_END;
				break;
			}
		}
		return isDataSignature;
	}

	/**
	 * check for begin and sensor signature in data buffer 19200 V4 protocol
	 * @param _dataBuffer
	 * @param checkSensorType
	 * @return
	 */
	protected boolean checkSignature(byte[] _dataBuffer, byte checkSensorType) {
		boolean isDataSignature = false;
		for (int i = 1; i < _dataBuffer.length; i++) {
			if (_dataBuffer[i] == HoTTAdapterSerialPort.DATA_BEGIN) {
				isDataSignature = _dataBuffer[i - 1] == checkSensorType;
				break;
			}
		}
		return isDataSignature;
	}

	/**
	 * check for begin and copy signature at begin of array
	 * @param _dataBuffer
	 * @param checkSensorType
	 * @return
	 */
	protected boolean checkContainsDataBegin(byte[] _dataBuffer) {
		boolean isDataBegin = false;
		for (int i = 1; i < _dataBuffer.length; i++) {
			if (_dataBuffer[i] == HoTTAdapterSerialPort.DATA_BEGIN) {
				System.arraycopy(_dataBuffer, i - 1, _dataBuffer, 0, 2); //correct data to fasten checkSignature
				isDataBegin = true;
				break;
			}
		}
		return isDataBegin;
	}

	/**
	 * set switches and alert change
	 * @return > 0 signals the changed switch number
	 */
	protected int setSwitchS() {
		//set switches and alert change
		for (int i = 0, j = 1; i < 2; i++) {
			for (int k = 1; j <= (i + 1) * 8; j++, k *= 2) {
				if (HoTTAdapter.isSwitchS[j - 1] != ((this.dataBuffer[i + 161] & k) != 0)) {
					HoTTAdapter.isSwitchS[j - 1] = (this.dataBuffer[i + 161] & k) != 0;
					return j;
				}
			}
		}
		return 0;
	}

	/**
	 * set switches and alert change
	 * @return > 0 signals the changed switch number
	 */
	protected int setSwitchG() {
		for (int i = 0, j = 1; i < 1; i++) {
			for (int k = 1; j <= (i + 1) * 8; j++, k *= 2) {
				if (HoTTAdapter.isSwitchG[j - 1] != ((this.dataBuffer[i + 169] & k) != 0)) {
					HoTTAdapter.isSwitchG[j - 1] = (this.dataBuffer[i + 169] & k) != 0;
					return j;
				}
			}
		}
		return 0;
	}

	/**
	 * set logical switches and alert change
	 * @return > 0 signals the changed switch number
	 */
	protected int setSwitchL() {
		for (int i = 0, j = 1; i < 1; i++) {
			for (int k = 1; j <= (i + 1) * 8; j++, k *= 2) {
				if (HoTTAdapter.isSwitchL[j - 1] != ((this.dataBuffer[i + 173] & k) != 0)) {
					HoTTAdapter.isSwitchL[j - 1] = (this.dataBuffer[i + 173] & k) != 0;
					return j;
				}
			}
		}
		return 0;
	}
}
