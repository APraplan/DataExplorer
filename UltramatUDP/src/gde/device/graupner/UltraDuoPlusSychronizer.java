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
    
    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.device.graupner.UltraDuoPlusType.ChannelData1;
import gde.device.graupner.UltraDuoPlusType.ChannelData2;
import gde.device.graupner.UltraDuoPlusType.MotorRunData;
import gde.device.graupner.UltraDuoPlusType.TireHeaterData;
import gde.device.graupner.Ultramat.GraupnerDeviceType;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.utils.StringHelper;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class UltraDuoPlusSychronizer extends Thread {
	final static Logger	log	= Logger.getLogger(UltraDuoPlusSychronizer.class.getName());

	public static enum SYNC_TYPE {
		READ, WRITE;
	}

	final UltraDuoPlusDialog	dialog;
	final UltramatSerialPort	serialPort;
	final UltraDuoPlusType		ultraDuoPlusSetup;
	final SYNC_TYPE						syncType;
	final long								startTime;

	public UltraDuoPlusSychronizer(UltraDuoPlusDialog useDialog, UltramatSerialPort useSerialPort, UltraDuoPlusType useUltraDuoPlusSetup, SYNC_TYPE useSyncType) {
		super(UltraDuoPlusSychronizer.class.getSimpleName() + GDE.STRING_MAC_APP_BASE_PATH + useSyncType.toString());
		this.dialog = useDialog;
		this.serialPort = useSerialPort;
		this.ultraDuoPlusSetup = useUltraDuoPlusSetup;
		this.syncType = useSyncType;
		this.startTime = new Date().getTime();
	}

	/**
	 * execute creation or update of UltraDuoPlus cache
	 */
	@Override
	public void run() {
		try {
			switch (this.syncType) {
			case READ:
				syncRead();
				break;
			case WRITE:
				syncWrite();
				break;
			}
		}
		catch (IOException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		catch (TimeOutException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * synchronizes cached ultraDuoPlusSetup
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	private void syncRead() throws IOException, TimeOutException, SerialPortException {
		if (this.serialPort.isConnected()) {
			if (this.dialog != null) this.dialog.setBackupRetoreButtons(false);

			//read memory names first
			List<MemoryType> cellMemories = this.ultraDuoPlusSetup.getMemory();
			Iterator<MemoryType> iterator = cellMemories.iterator();
			for (int i = 1; iterator.hasNext(); ++i) {
				MemoryType cellMemory = iterator.next();
				if (!cellMemory.isSynced()) {
					cellMemory.setName(this.serialPort.readMemoryName(i));
					cellMemory.setSynced(true);
				}
				log.log(java.util.logging.Level.FINE, "read memory name " + i + " time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - this.startTime))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

			//read basic setup data
			if (this.ultraDuoPlusSetup.getChannelData1() == null || !this.ultraDuoPlusSetup.getChannelData1().isSynced()) {
				ChannelData1 channelData1 = new ObjectFactory().createUltraDuoPlusTypeChannelData1();
				channelData1.setValue(this.serialPort.readChannelData(1));
				this.ultraDuoPlusSetup.setChannelData1(channelData1);
			}
			if (this.dialog != null && this.dialog.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus40 && this.dialog.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45
					&& (this.ultraDuoPlusSetup.getChannelData2() == null || !this.ultraDuoPlusSetup.getChannelData2().isSynced())) {
				ChannelData2 channelData2 = new ObjectFactory().createUltraDuoPlusTypeChannelData2();
				channelData2.setValue(this.serialPort.readChannelData(2));
				this.ultraDuoPlusSetup.setChannelData2(channelData2);
			}

			log.log(java.util.logging.Level.FINE, "read basics time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - this.startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			//read all memory setup data
			iterator = cellMemories.iterator();
			for (int i = 1; iterator.hasNext(); ++i) {
				MemoryType cellMemory = iterator.next();
				if (!cellMemory.getSetupData().isSynced()) {
					MemoryType.SetupData setupData = cellMemory.getSetupData();
					setupData.setValue(this.serialPort.readMemorySetup(i));
					cellMemory.setSetupData(setupData);
					setupData.setSynced(true);
				}
				log.log(java.util.logging.Level.FINE, "read memory setup" + i + " time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - this.startTime))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

			//start optional entries, temporary comment out to reduce serial I/O
			//memory step charge data
			iterator = cellMemories.iterator();
			for (int i = 1; iterator.hasNext(); ++i) {
				MemoryType cellMemory = iterator.next();
				if (cellMemory.getStepChargeData() == null || !cellMemory.getStepChargeData().isSynced()) {
					MemoryType.StepChargeData stepChargeData = cellMemory.getStepChargeData();
					stepChargeData = stepChargeData == null ? new ObjectFactory().createMemoryTypeStepChargeData() : stepChargeData;
					stepChargeData.setValue(this.serialPort.readMemoryStepChargeSetup(i));
					cellMemory.setStepChargeData(stepChargeData);
					stepChargeData.setSynced(true);
				}
				log.log(java.util.logging.Level.FINE, "read memory step charge data " + i + " time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - this.startTime))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
	
			//		//memory trace data
			//		iterator = cellMemories.iterator();
			//		for (int i = 1; iterator.hasNext(); ++i) {
			//			MemoryType cellMemory = iterator.next();
			//			if (cellMemory.getTraceData() == null || !cellMemory.getTraceData().isSynced()) {
			//				MemoryType.TraceData traceData = cellMemory.getTraceData();
			//				traceData = traceData == null ? new ObjectFactory().createMemoryTypeTraceData() : traceData;
			//				traceData.setValue(this.serialPort.readMemoryTrace(i));
			//				cellMemory.setTraceData(traceData);
			//				traceData.setSynced(true);
			//			}
			//			log.log(Level.TIME, "read memory trace data " + i + " time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - this.startTime))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			//		}
			//
			//		//memory cycle data
			//		iterator = cellMemories.iterator();
			//		for (int i = 1; iterator.hasNext(); ++i) {
			//			MemoryType cellMemory = iterator.next();
			//			if (cellMemory.getCycleData() == null || !cellMemory.getCycleData().isSynced()) {
			//				MemoryType.CycleData cycleData = cellMemory.getCycleData();
			//				cycleData = cycleData == null ? new ObjectFactory().createMemoryTypeCycleData() : cycleData;
			//				cycleData.setValue(this.serialPort.readMemoryCycle(i));
			//				cellMemory.setCycleData(cycleData);
			//				cycleData.setSynced(true);
			//			}
			//			log.log(Level.TIME, "read memory cycle data " + i + " time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - this.startTime))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			//		}
			//
			//		//tire heater setup data
			//		if (this.ultraDuoPlusSetup.getTireHeaterData() == null || this.ultraDuoPlusSetup.getTireHeaterData().size() == 0) {
			//			for (int i = 0; i < 2; i++) {
			//				this.ultraDuoPlusSetup.getTireHeaterData().add(new ObjectFactory().createUltraDuoPlusTypeTireHeaterData());
			//			}
			//		}
			//		Iterator<TireHeaterData> tireIterator = this.ultraDuoPlusSetup.getTireHeaterData().iterator();
			//		for (int i = 0; tireIterator.hasNext(); ++i) {
			//			TireHeaterData tireHeaterData = tireIterator.next();
			//			if (!tireHeaterData.isSynced()) {
			//				tireHeaterData.setValue(new String(this.serialPort.readConfigData(UltramatSerialPort.READ_TIRE_HEATER, UltramatSerialPort.SIZE_TIRE_HEATER_SETUP * 4 + 7, i + 1)).substring(1,
			//						UltramatSerialPort.SIZE_TIRE_HEATER_SETUP * 4 + 1));
			//			}
			//		}
			//
			//		//motor run setup data
			//		if (this.ultraDuoPlusSetup.getMotorRunData() == null || this.ultraDuoPlusSetup.getMotorRunData().size() == 0) {
			//			for (int i = 0; i < 2; i++) {
			//				this.ultraDuoPlusSetup.getMotorRunData().add(new ObjectFactory().createUltraDuoPlusTypeMotorRunData());
			//			}
			//		}
			//		Iterator<MotorRunData> motorIterator = this.ultraDuoPlusSetup.getMotorRunData().iterator();
			//		for (int i = 0; motorIterator.hasNext(); ++i) {
			//			MotorRunData motorRunData = motorIterator.next();
			//			if (!motorRunData.isSynced()) {
			//				motorRunData.setValue(new String(this.serialPort.readConfigData(UltramatSerialPort.READ_MOTOR_RUN, UltramatSerialPort.SIZE_MOTOR_RUN_SETUP * 4 + 7, i + 1)).substring(1,
			//						UltramatSerialPort.SIZE_MOTOR_RUN_SETUP * 4 + 1));
			//			}
			//		}

			log.log(java.util.logging.Level.FINE, "read complete memory setup time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - this.startTime))); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialog != null) this.dialog.setBackupRetoreButtons(true);
		}
	}

	/**
	 * synchronizes cached ultraDuoPlusSetup
	 * @throws IOException
	 * @throws TimeOutException
	 */
	private void syncWrite() throws IOException, TimeOutException {
		if (this.serialPort.isConnected()) {
			if (this.dialog != null) this.dialog.setBackupRetoreButtons(false);

			//device Identifier name
			if (this.ultraDuoPlusSetup.isChanged()) {
				this.serialPort.writeConfigData(UltramatSerialPort.WRITE_DEVICE_IDENTIFIER_NAME, this.ultraDuoPlusSetup.getIdentifierName().getBytes(), 0);
				this.ultraDuoPlusSetup.changed = null;
			}

			//basic setup data, write channel 1 alway to sync the time
			this.serialPort.writeConfigData(UltramatSerialPort.WRITE_CHANNEL_SETUP, this.ultraDuoPlusSetup.getChannelData1().getValue().getBytes(), 1);
			this.ultraDuoPlusSetup.getChannelData1().changed = null;

			if (this.dialog != null && this.dialog.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus40 &&  this.dialog.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 && (this.ultraDuoPlusSetup.getChannelData2().isChanged())) {
				this.serialPort.writeConfigData(UltramatSerialPort.WRITE_CHANNEL_SETUP, this.ultraDuoPlusSetup.getChannelData2().getValue().getBytes(), 2);
				this.ultraDuoPlusSetup.getChannelData2().changed = null;
			}

			//battery memory
			List<MemoryType> cellMemories = this.ultraDuoPlusSetup.getMemory();
			Iterator<MemoryType> iterator = cellMemories.iterator();
			for (int i = 0; iterator.hasNext(); ++i) {
				MemoryType cellMemory = iterator.next();
				//memory name
				if (cellMemory.isChanged()) {
					this.serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_NAME, cellMemory.getName().getBytes(), i + 1);
					this.ultraDuoPlusSetup.getMemory().get(i).changed = null;
				}
				//memory setup data
				if (cellMemory.getSetupData().isChanged()) {
					this.serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_SETUP, cellMemory.getSetupData().getValue().getBytes(), i + 1);
					this.ultraDuoPlusSetup.getMemory().get(i).getSetupData().changed = null;
				}
				//memory step charge data
				if (cellMemory.getStepChargeData() != null && cellMemory.getStepChargeData().isChanged()) {
					this.serialPort.writeConfigData(UltramatSerialPort.WRITE_STEP_CHARGE_SETUP, cellMemory.getStepChargeData().getValue().getBytes(), i + 1);
					this.ultraDuoPlusSetup.getMemory().get(i).getStepChargeData().changed = null;
				}
				//memory trace data
				if (cellMemory.getTraceData() != null && cellMemory.getTraceData().isChanged()) {
					this.serialPort.writeConfigData(UltramatSerialPort.WRITE_TRACE_DATA, cellMemory.getTraceData().getValue().getBytes(), i + 1);
					this.ultraDuoPlusSetup.getMemory().get(i).getTraceData().changed = null;
				}
				//memory cycle data
				if (cellMemory.getCycleData() != null && cellMemory.getCycleData().isChanged()) {
					this.serialPort.writeConfigData(UltramatSerialPort.WRITE_CYCLE_DATA, cellMemory.getCycleData().getValue().getBytes(), i + 1);
					this.ultraDuoPlusSetup.getMemory().get(i).getCycleData().changed = null;
				}
			}

			Iterator<TireHeaterData> tireIterator = this.ultraDuoPlusSetup.getTireHeaterData().iterator();
			for (int i = 0; tireIterator.hasNext(); ++i) {
				TireHeaterData tireHeaterData = tireIterator.next();
				if (tireHeaterData != null && tireHeaterData.isChanged()) {
					this.serialPort.writeConfigData(UltramatSerialPort.WRITE_TIRE_HEATER, tireHeaterData.getValue().getBytes(), i + 1);
					this.ultraDuoPlusSetup.getTireHeaterData().get(i).changed = null;
				}
			}
			Iterator<MotorRunData> motorIterator = this.ultraDuoPlusSetup.getMotorRunData().iterator();
			for (int i = 0; motorIterator.hasNext(); ++i) {
				MotorRunData motorRunData = motorIterator.next();
				if (motorRunData != null && motorRunData.isChanged()) {
					this.serialPort.writeConfigData(UltramatSerialPort.WRITE_MOTOR_RUN, motorRunData.getValue().getBytes(), i + 1);
					this.ultraDuoPlusSetup.getMotorRunData().get(i).changed = null;
				}
			}

			log.log(java.util.logging.Level.FINE, "complete update (write) time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - this.startTime))); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialog != null) this.dialog.setBackupRetoreButtons(true);
		}
	}
}
