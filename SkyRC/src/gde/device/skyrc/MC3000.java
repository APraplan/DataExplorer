/**************************************************************************************
  	This file is part of DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.skyrc;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

import javax.usb.UsbClaimException;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.resource.DeviceXmlResource;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

/**
 * Class to implement SKYRC MC3000 device
 * @author Winfried Brügmann
 */
public class MC3000 extends DeviceConfiguration implements IDevice {
	final static Logger		log									= Logger.getLogger(MC3000.class.getName());

	final DataExplorer		application;
	final Settings				settings;
	final MC3000Dialog		dialog;
	final MC3000UsbPort		usbPort;

	protected String[]		STATUS_MODE;
	protected String[]		USAGE_MODE_LI;
	protected String[]		USAGE_MODE_NI;
	protected String[]		USAGE_MODE_ZN;
	protected String[]		USAGE_MODE_PB;
	protected String[]		BATTERY_TYPE;
	final static String[]	cellTypeNames				= { "LiIon", "LiFe", "LiIo4.35", "NiMH", "NiCd", "NiZn", "Eneloop", "RAM", "LTO" };
	final static String[]	cycleModeNames			= { "C>D", "C>D>C", "D>C", "D>C>D" };
	final static String[]	operationModeLi			= { "Charge", "Refresh", "Storage", "Discharge", "Cycle" };
	final static String[]	operationModeNi			= { "Charge", "Refresh", "Break_in", "Discharge", "Cycle" };
	final static String[]	operationModeZnRAM	= { "Charge", "Refresh", "Discharge", "Cycle" };
	final static String[]	cellModelNames111		= { "Std AA", "Lite AAA", "Lite AAA", "Std AAA", "Std AAA", "Pro/XX AAA", "Pro/XX AAA", "Lite AA", "Std AA", "Plus AA", "Pro/XX AA", "Std C", "Std C", "Std D", "OFF"												};
	final static int[]		cellModelCapacity111= { 0, 700, 720, 900, 960, 1000, 1080, 1200, 2000, 2400, 3000, 3800, 3840, 7200 };
	final static String[]	cellModelNames112		= { "Std AA", "Lite AAA", "Lite AAA", "Std AAA", "Std AAA", "Pro/XX AAA", "Pro/XX AAA", "Lite AA", "Lite AA", "Std AA", "Std AA", "Plus AA", "Plus AA", "Pro/XX AA", "Pro/XX AA", "Std C", "Std C", "Std D", "Std D", "OFF"												};
	final static int[]		cellModelCapacity112= {  0,        600,        720,        800,       960,       900,          1080,         1000,      1200,      2000,     2400,     2200,      2640,      2500,        3000,        3200,    3840,    6000,    7200      };
	final static String[]	trickleTimeValues		= { "OFF", "End", "Rest" };

	protected class SystemSettings {
		byte		currentSlotNumber;
		byte		slot1programmNumber;
		byte		slot2programmNumber;
		byte		slot3programmNumber;
		byte		slot4programmNumber;
		byte		userInterfaceMode;
		byte		temperatureUnit;
		byte		beepTone;
		boolean	isHideLiFe;
		boolean	isHideLiIon435;
		boolean	isHideEneloop;
		boolean	isHideNiZn;
		byte		LCDoffTime;
		byte		minVoltage;
		byte[]	machineId	= new byte[16];
		boolean	isHideRAM;

		public SystemSettings(final byte[] buffer) {
			this.currentSlotNumber = buffer[2];
			this.slot1programmNumber = buffer[3];
			this.slot2programmNumber = buffer[4];
			this.slot3programmNumber = buffer[5];
			this.slot4programmNumber = buffer[6];
			this.userInterfaceMode = buffer[7];
			this.temperatureUnit = buffer[8];
			this.beepTone = buffer[9];
			this.isHideLiFe = buffer[10] == 0x01;
			this.isHideLiIon435 = buffer[11] == 0x01;
			this.isHideEneloop = buffer[12] == 0x01;
			this.isHideNiZn = buffer[13] == 0x01;
			this.LCDoffTime = buffer[14];
			this.minVoltage = buffer[15];
			for (int i = 0; i < 15; i++) {
				this.machineId[i] = buffer[i + 16];
			}
			this.isHideRAM = buffer[32] == 0x01;
			//System.out.println(new String(machineId));
		}

		public String getFirmwareVersion() {
			return String.format("Firmware : %d.%02d", this.machineId[11], this.machineId[12]);
		}

		public int getFirmwareVersionAsInt() {
			return Integer.valueOf(String.format("%d%02d", this.machineId[11], this.machineId[12])).intValue();
		}

		public String getHardwareVersion() {
			return String.format("Hardware : %d.%d", this.machineId[13]/10, this.machineId[13]%10);
		}

		public int getHardwareVersionAsInt() {
			return this.machineId[13];
		}

		public byte getCurrentSlotNumber() {
			return this.currentSlotNumber;
		}

		public void setCurrentSlotNumber(byte currentSlotNumber) {
			this.currentSlotNumber = currentSlotNumber;
		}

		public byte getUserInterfaceMode() {
			return this.userInterfaceMode;
		}

		public void setUserInterfaceMode(byte userInterfaceMode) {
			this.userInterfaceMode = userInterfaceMode;
		}

		public byte getTemperatureUnit() {
			return this.temperatureUnit;
		}

		public void setTemperatureUnit(byte temperatureUnit) {
			this.temperatureUnit = temperatureUnit;
		}

		public byte getBeepTone() {
			return this.beepTone;
		}

		public boolean isHideLiFe() {
			return this.isHideLiFe;
		}

		public void setHideLiFe(boolean isHideLiFe) {
			this.isHideLiFe = isHideLiFe;
		}

		public boolean isHideLiIon435() {
			return this.isHideLiIon435;
		}

		public void setHideLiIon435(boolean isHideLiIon435) {
			this.isHideLiIon435 = isHideLiIon435;
		}

		public boolean isHideEneloop() {
			return this.isHideEneloop;
		}

		public boolean isHideNiZn() {
			return this.isHideNiZn;
		}

		public boolean isHideRAM() {
			return this.isHideRAM;
		}

		public byte getLCDoffTime() {
			return this.LCDoffTime;
		}

		public void setLCDoffTime(byte lCDoffTime) {
			this.LCDoffTime = lCDoffTime;
		}

		public byte getMinVoltage() {
			return this.minVoltage;
		}

		public void setMinVoltage(byte minVoltage) {
			this.minVoltage = minVoltage;
		}

		public byte getSlot1programmNumber() {
			return (byte) (this.slot1programmNumber + 1);
		}

		public byte getSlot2programmNumber() {
			return (byte) (this.slot2programmNumber + 1);
		}

		public byte getSlot3programmNumber() {
			return (byte) (this.slot3programmNumber + 1);
		}

		public byte getSlot4programmNumber() {
			return (byte) (this.slot4programmNumber + 1);
		}
	}

	SystemSettings	systemSettings;

	protected class SlotSettings {
		byte[]	slotBuffer	= new byte[64];
		int			firmwareVersion;
		byte		slotNumber;
		byte		busyTag;
		byte		batteryType;
		byte		operationMode;
		byte[]	capacity;
		byte[]	chargeCurrent;
		byte[]	dischargeCurrent;
		byte[]	dischargeCutVoltage;
		byte[]	chargeEndVoltage;
		byte[]	dischargeReduceCurrent;
		byte[]	chargeEndCurrent;
		byte		numberCycle;
		byte		chargeRestingTime;
		byte		cycleMode;
		byte		peakSenseVoltage;					//Ni cells only
		byte		trickleCurrent;
		byte[]	restartVoltage;
		byte		cutTemperature;
		byte[]	cutTime;
		byte		temperatureUnit;
		//FW 1.12
		byte		dischargeRestingTime;
		byte		trickleTime;

		public SlotSettings(final byte[] buffer, final int firmware) throws Exception {
			if (MC3000UsbPort.calculateCheckSum(buffer) == buffer[buffer.length - 1]) {
				System.arraycopy(buffer, 0, this.slotBuffer, 0, buffer.length);
				this.firmwareVersion = firmware;
				this.slotNumber = buffer[1];
				this.busyTag = buffer[2];
				this.batteryType = buffer[3];
				this.operationMode = buffer[4];
				this.capacity = new byte[] { buffer[5], buffer[6] };
				this.chargeCurrent = new byte[] { buffer[7], buffer[8] };
				this.dischargeCurrent = new byte[] { buffer[9], buffer[10] };
				this.dischargeCutVoltage = new byte[] { buffer[11], buffer[12] };
				this.chargeEndVoltage = new byte[] { buffer[13], buffer[14] };
				this.chargeEndCurrent = new byte[] { buffer[15], buffer[16] };
				this.dischargeReduceCurrent = new byte[] { buffer[17], buffer[18] };
				this.numberCycle = buffer[19];
				this.chargeRestingTime = buffer[20];
				this.cycleMode = buffer[21];
				this.peakSenseVoltage = buffer[22];
				this.trickleCurrent = buffer[23];
				this.restartVoltage = new byte[] { buffer[24], buffer[25] };
				this.cutTemperature = buffer[26];
				this.cutTime = new byte[] { buffer[27], buffer[28] };
				this.temperatureUnit = buffer[29];
				if (this.firmwareVersion > 111) {
					this.trickleTime = buffer[30];
					this.dischargeRestingTime = buffer[31];
				}
				else {
					this.trickleTime = 0;
					this.dischargeRestingTime = 0;
				}
				MC3000.log.log(java.util.logging.Level.FINE, this.toString());
			}
			else {
				MC3000.log.log(java.util.logging.Level.SEVERE, Messages.getString(MessageIds.GDE_MSGE3600));
				WaitTimer.delay(500);
				MC3000.this.application.openMessageDialogAsync(MC3000.this.application.getShell(), Messages.getString(MessageIds.GDE_MSGE3600));
				throw new Exception(Messages.getString(MessageIds.GDE_MSGE3600));
			}
		}

		/**
		 * copy constructor
		 * @param slotSettings
		 */
		public SlotSettings(final SlotSettings slotSettings) {
			System.arraycopy(slotSettings.getBuffer(), 0, this.slotBuffer, 0, slotSettings.getBuffer().length);
			this.firmwareVersion = slotSettings.firmwareVersion;
			this.slotNumber = slotSettings.slotNumber;
			this.busyTag = slotSettings.busyTag;
			this.batteryType = slotSettings.batteryType;
			this.operationMode = slotSettings.operationMode;
			this.capacity = slotSettings.capacity.clone();
			this.chargeCurrent = slotSettings.chargeCurrent.clone();
			this.dischargeCurrent = slotSettings.dischargeCurrent.clone();
			this.dischargeCutVoltage = slotSettings.dischargeCutVoltage.clone();
			this.chargeEndVoltage = slotSettings.chargeEndVoltage.clone();
			this.dischargeReduceCurrent = slotSettings.dischargeReduceCurrent.clone();
			this.chargeEndCurrent = slotSettings.chargeEndCurrent.clone();
			this.numberCycle = slotSettings.numberCycle;
			this.chargeRestingTime = slotSettings.chargeRestingTime;
			this.cycleMode = slotSettings.cycleMode;
			this.peakSenseVoltage = slotSettings.peakSenseVoltage;
			this.trickleCurrent = slotSettings.trickleCurrent;
			this.restartVoltage = slotSettings.restartVoltage.clone();
			this.cutTemperature = slotSettings.cutTemperature;
			this.cutTime = slotSettings.cutTime.clone();
			this.temperatureUnit = slotSettings.temperatureUnit;
			if (this.firmwareVersion > 111) {
				this.trickleTime = slotSettings.trickleTime;
				this.dischargeRestingTime = slotSettings.dischargeRestingTime;
			}

			if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, this.toString());
		}

		public boolean isBusy() {
			return this.busyTag == 0x01;
		}

		@Override
		public String toString() {
			if (this.firmwareVersion <= 111) //FW > 1.12 missing D.RESTING TRICKLE TIME
				return String.format(
							" slot#=%02d busy=%b BATT TYPE=%02d MODE=%02d CAPACITY=%04d C.CURRENT=%04d D.CURRENT=%04d CUT VOLT=%04d TARGET VOLT=%04d D.REDUCE=%04d TERMINATION=%04d CYCLE COUNT=%02d C.RESTING=%02d CYCLE MODE=%d DELTA PEAK=%02d TRICKLE C=%02d RESTART VOLT=%03d CUT TEMP=%02d CUT TIME=%03d temeratureUnit=%d",
							this.slotNumber, this.busyTag == 0x01, this.batteryType, this.operationMode, getCapacity(), getChargeCurrent(), getDischargeCurrent(), getDischargeCutVoltage(), getChargeEndVoltage(),
							getDischargeReduceCurrent(), getChargeEndCurrent(), this.numberCycle, this.chargeRestingTime & 0xFF, this.cycleMode, this.peakSenseVoltage, this.trickleCurrent, getRestartVoltage(),
							this.cutTemperature, getCutTime(), this.temperatureUnit);
			
				return String.format(
						" slot#=%02d busy=%b BATT TYPE=%02d MODE=%02d CAPACITY=%04d C.CURRENT=%04d D.CURRENT=%04d CUT VOLT=%04d TARGET VOLT=%04d D.REDUCE=%04d TERMINATION=%04d CYCLE COUNT=%02d C.RESTING=%02d D.RESTING=%02d CYCLE MODE=%d DELTA PEAK=%02d TRICKLE C=%02d TRICKLE TIME=%s RESTART VOLT=%03d CUT TEMP=%02d CUT TIME=%03d temeratureUnit=%d",
						this.slotNumber, this.busyTag == 0x01, this.batteryType, this.operationMode, getCapacity(), getChargeCurrent(), getDischargeCurrent(), getDischargeCutVoltage(), getChargeEndVoltage(),
						getDischargeReduceCurrent(), getChargeEndCurrent(), this.numberCycle, this.chargeRestingTime & 0xFF, this.dischargeRestingTime & 0xFF, this.cycleMode, this.peakSenseVoltage, this.trickleCurrent, this.trickleTime == 0 ? "OFF" : this.trickleTime == 1 ? "END" : "Rest", getRestartVoltage(),
						this.cutTemperature, getCutTime(), this.temperatureUnit);
		}

		/**
		 * @param isToolTip true will suppress fixed OFFs
		 * @return tool tip text 
		 */
		public String toString4Tip(final boolean isToolTip, final byte systemTemperatureUnit) {
			StringBuilder sb = new StringBuilder();
			//BATT TYPE
			sb.append(Messages.getString(MessageIds.GDE_MSGT3682)).append(MC3000.cellTypeNames[this.batteryType]).append(GDE.LINE_SEPARATOR);
			//MODE
			appendOperationMode(sb.append(Messages.getString(MessageIds.GDE_MSGT3683)));
			sb.append(GDE.LINE_SEPARATOR);
			//CAPACITY
			appendCapacityModelNominal(sb);
			//C.CURRENT:
			//battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
			//Mode LI battery： 		0=CHARGE 1=REFRESH 2=STORAGE   3=DISCHARGE 4=CYCLE
			//Mode Ni battery:		0=CHARGE 1=REFRESH 2=BREAK_IN  3=DISCHARGE 4=CYCLE
			//Mode Zn battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
			//Mode RAM battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
			if ((((this.batteryType <= 4 || this.batteryType == 6 || this.batteryType == 8) && this.operationMode != 3) //!discharge Li -> Ni + Eneloop
					|| ((this.batteryType == 5 || this.batteryType == 7) && this.operationMode != 2)) //!discharge NiZn, RAM
					&& this.getChargeCurrent() != 0) {
				sb.append(String.format(Locale.ENGLISH, "%-14s %4.2fA", Messages.getString(MessageIds.GDE_MSGT3661), this.getChargeCurrent() / 1000.0)).append(GDE.LINE_SEPARATOR);
			}
			else if (!isToolTip) {
				sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3661))).append(GDE.LINE_SEPARATOR);
			}
			//D.CURRENT:
			if (this.operationMode != 0 && this.getDischargeCurrent() != 0) { //!charge
				sb.append(String.format(Locale.ENGLISH, "%-13s -%4.2fA", Messages.getString(MessageIds.GDE_MSGT3662), this.getDischargeCurrent() / 1000.0)).append(GDE.LINE_SEPARATOR);
			}
			else if (!isToolTip) {
				sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3662))).append(GDE.LINE_SEPARATOR);
			}
			//C.RESTING:
			if (((this.batteryType <= 4 || this.batteryType == 6 || this.batteryType == 8) && this.operationMode != 3) //!discharge Li -> Ni + Eneloop
					|| ((this.batteryType == 5 || this.batteryType == 7) && this.operationMode != 2)) { //!discharge NiZn, RAM
				sb.append(String.format(Locale.ENGLISH, "%-14s %dmin", Messages.getString(MessageIds.GDE_MSGT3663), this.chargeRestingTime & 0xFF)).append(GDE.LINE_SEPARATOR);
			}
			else if (!isToolTip) {
				sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3663))).append(GDE.LINE_SEPARATOR);
			}
			//D.RESTING:			
			if (this.operationMode != 0) { //!charge
				if (this.firmwareVersion > 111)
					sb.append(String.format(Locale.ENGLISH, "%-14s %dmin", Messages.getString(MessageIds.GDE_MSGT3664), this.dischargeRestingTime & 0xFF)).append(GDE.LINE_SEPARATOR);
				else
					sb.append(String.format(Locale.ENGLISH, "%-14s %dmin (FW <=1.11 = %s)", Messages.getString(MessageIds.GDE_MSGT3664), this.chargeRestingTime & 0xFF, Messages.getString(MessageIds.GDE_MSGT3663))).append(GDE.LINE_SEPARATOR);
			}
			else if (!isToolTip) {
				sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3664), this.dischargeRestingTime & 0xFF)).append(GDE.LINE_SEPARATOR);
			}
			//CYCLE COUNT://CYCLE MODE:
			appendCycleCountCycleMode(sb, isToolTip);
			//TARGET VOLT:
			//battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM 8:LiTo
			//Mode LI battery： 		0=CHARGE 1=REFRESH 2=STORAGE   3=DISCHARGE 4=CYCLE
			//Mode Ni battery:		0=CHARGE 1=REFRESH 2=BREAK_IN  3=DISCHARGE 4=CYCLE
			//Mode Zn battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
			//Mode RAM battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
			if (((this.batteryType <= 2 || this.batteryType == 8) && this.operationMode != 3) //!discharge Li
					|| ((this.batteryType == 3 || this.batteryType == 4 || this.batteryType == 6) && (this.operationMode != 2 && this.operationMode != 3))//!break_in !discharge Ni + Eneloop
					|| ((this.batteryType == 5 || this.batteryType == 7) && this.operationMode != 2)) { //!discharge NiZn, RAM
				sb.append(String.format(Locale.ENGLISH, "%-14s %4.2fV", Messages.getString(MessageIds.GDE_MSGT3667), this.getChargeEndVoltage() / 1000.0)).append(GDE.LINE_SEPARATOR);
			}
			else if (!isToolTip) {
				sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3667))).append(GDE.LINE_SEPARATOR);
			}
			//TERMINATION:
			appendCcCvTerminationCurrent(sb, isToolTip);
			//DELTA PEAK://TRICKLE C.://TRICKLE TIME:
			appendNiProcessEndDetectionValues(sb, isToolTip);
			//RESTART VOLT:
			appendRestartVoltage(sb, isToolTip);
			//D.REDUCE:
			appendDischargeReductionCurrent(sb, isToolTip);
			//CUT VOLT:
			if (this.operationMode == 0 || (this.operationMode == 2 && (this.batteryType < 3 || this.batteryType == 8))) {// charge or Li storage
				if (!isToolTip) sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3671))).append(GDE.LINE_SEPARATOR);
			}
			else {
				sb.append(String.format(Locale.ENGLISH, "%-14s %4.2fV", Messages.getString(MessageIds.GDE_MSGT3671), this.getDischargeCutVoltage() / 1000.0)).append(GDE.LINE_SEPARATOR);
			}

			//CUT TEMP:
			switch (systemTemperatureUnit) {
			case 1: //°F
				if (this.getCutTemperature(systemTemperatureUnit) >= 68) {
					sb.append(String.format(Locale.ENGLISH, "%-14s %d%s", Messages.getString(MessageIds.GDE_MSGT3672), this.getCutTemperature(systemTemperatureUnit), systemTemperatureUnit == 0 ? "°C" : "°F"));
				} else {
					sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3672)));
				}
				break;

			case 0: //°C
			default:
				if (this.getCutTemperature(systemTemperatureUnit) >= 20) {
					sb.append(String.format(Locale.ENGLISH, "%-14s %d%s", Messages.getString(MessageIds.GDE_MSGT3672), this.getCutTemperature(systemTemperatureUnit), systemTemperatureUnit == 0 ? "°C" : "°F"));
				} else {
					sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3672)));
				}
				break;
			}

			//CUT TIME:			
			if ((this.batteryType == 3 || this.batteryType == 4 || this.batteryType == 6) && this.operationMode == 2) {// NiMH, NiCd, Eneloop & break_in
				if (!isToolTip) sb.append(GDE.LINE_SEPARATOR).append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3673)));

			}
			else {
				if (this.getCutTime() == 0)
					sb.append(GDE.LINE_SEPARATOR).append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3673)));
				else
					sb.append(GDE.LINE_SEPARATOR).append(String.format(Locale.ENGLISH, "%-14s %dmin", Messages.getString(MessageIds.GDE_MSGT3673), this.getCutTime()));
			}
			return sb.toString();
		}

		private void appendDischargeReductionCurrent(final StringBuilder sb, final boolean isToolTip) {
			//battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
			//Mode LI battery： 		0=CHARGE 1=REFRESH 2=STORAGE   3=DISCHARGE 4=CYCLE
			//Mode Ni battery:		0=CHARGE 1=REFRESH 2=BREAK_IN  3=DISCHARGE 4=CYCLE
			//Mode Zn battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
			//Mode RAM battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
			switch (this.batteryType) {
			default:
			case 0://LiIon
			case 1://LiFe
			case 2://LiHV
				switch (this.operationMode) {
				case 0://charge
					if (!isToolTip)
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3670))).append(GDE.LINE_SEPARATOR);
					break;
				default://refresh, storage, discharge cycle
					if (this.getDischargeReduceCurrent() == 0)  
						sb.append(String.format(Locale.ENGLISH, "%-13s -Zero", Messages.getString(MessageIds.GDE_MSGT3670))).append(GDE.LINE_SEPARATOR);
					else if (this.getDischargeReduceCurrent() >= this.getDischargeCurrent())  
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3670))).append(GDE.LINE_SEPARATOR);
	 				else  
						sb.append(String.format(Locale.ENGLISH, "%-13s -%3.2fA", Messages.getString(MessageIds.GDE_MSGT3670), this.getDischargeReduceCurrent() / 1000.0)).append(GDE.LINE_SEPARATOR);
					break;
				}
 				break;

			case 3://NiMH
			case 4://NiCd
			case 6://Eneloop
				switch (this.operationMode) {
				case 0://charge
				case 2://break_in 
					if (!isToolTip) sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3670))).append(GDE.LINE_SEPARATOR);
					break;
				default://discharge, refresh, cycle
					if (this.getDischargeReduceCurrent() == 0)
						sb.append(String.format(Locale.ENGLISH, "%-13s -Zero", Messages.getString(MessageIds.GDE_MSGT3670))).append(GDE.LINE_SEPARATOR);
					else if (this.getDischargeReduceCurrent() >= this.getDischargeCurrent())
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3670))).append(GDE.LINE_SEPARATOR);
					else
						sb.append(String.format(Locale.ENGLISH, "%-13s -%3.2fA", Messages.getString(MessageIds.GDE_MSGT3670), this.getDischargeReduceCurrent() / 1000.0)).append(GDE.LINE_SEPARATOR);
					break;
				}
				break;

			case 5://NiZn
			case 7://RAM
				switch (this.operationMode) {
				case 0://charge 
					if (!isToolTip) sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3670))).append(GDE.LINE_SEPARATOR);
					break;
				default://discharge, refresh, cycle
					if (this.getDischargeReduceCurrent() == 0)
						sb.append(String.format(Locale.ENGLISH, "%-13s -Zero", Messages.getString(MessageIds.GDE_MSGT3670))).append(GDE.LINE_SEPARATOR);
					else if (this.getDischargeReduceCurrent() >= this.getDischargeCurrent())
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3670))).append(GDE.LINE_SEPARATOR);
					else
						sb.append(String.format(Locale.ENGLISH, "%-13s -%3.2fA", Messages.getString(MessageIds.GDE_MSGT3670), this.getDischargeReduceCurrent() / 1000.0)).append(GDE.LINE_SEPARATOR);
					break;
				}
				break;
			}
		}

		private void appendRestartVoltage(final StringBuilder sb, final boolean isToolTip) {
			//battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
			//Mode LI battery： 		0=CHARGE 1=REFRESH 2=STORAGE   3=DISCHARGE 4=CYCLE
			//Mode Ni battery:		0=CHARGE 1=REFRESH 2=BREAK_IN  3=DISCHARGE 4=CYCLE
			//Mode Zn battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
			//Mode RAM battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
			switch (this.batteryType) {
			default:
			case 0://LiIon
			case 1://LiFe
			case 2://LiHV
			case 3://NiMH
			case 4://NiCd
			case 6://Eneloop
			case 8://LiTo
				switch (this.operationMode) {
				case 2://storage, break_in 
				case 3://discharge
					if (!isToolTip) sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3669))).append(GDE.LINE_SEPARATOR);
					break;
				default://charge, refresh, cycle
					if (this.getRestartVoltage() != 0)
						sb.append(String.format(Locale.ENGLISH, "%-14s %4.2fV", Messages.getString(MessageIds.GDE_MSGT3669), this.getRestartVoltage() / 1000.0)).append(GDE.LINE_SEPARATOR);
					else
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3669))).append(GDE.LINE_SEPARATOR);
					break;
				}
				break;

			case 5://NiZn
			case 7://RAM
				switch (this.operationMode) {
				case 2://discharge 
					if (!isToolTip) sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3669))).append(GDE.LINE_SEPARATOR);
					break;
				default://charge, refresh, cycle
					if (this.getRestartVoltage() != 0)
						sb.append(String.format(Locale.ENGLISH, "%-14s %4.2fV", Messages.getString(MessageIds.GDE_MSGT3669), this.getRestartVoltage() / 1000.0)).append(GDE.LINE_SEPARATOR);
					else
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3669))).append(GDE.LINE_SEPARATOR);
					break;
				}
				break;
			}
		}

		private void appendNiProcessEndDetectionValues(final StringBuilder sb, final boolean isToolTip) {
			//battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
			//Mode Ni battery:		0=CHARGE 1=REFRESH 2=BREAK_IN  3=DISCHARGE 4=CYCLE
			switch (this.batteryType) {
			case 0://LiIon
			case 1://LiFe
			case 2://LiHV
			case 5://NiZn
			case 7://RAM
			case 8://LiTo
				//Li batteries use termination current as end detection
				break;

			default:
			case 3://NiMH
			case 4://NiCd
			case 6://Eneloop
				switch (this.operationMode) {
				case 2://break_in 
				case 3://discharge
					if (!isToolTip) {
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3674))).append(GDE.LINE_SEPARATOR);
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3675))).append(GDE.LINE_SEPARATOR);
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3676))).append(GDE.LINE_SEPARATOR);
					}
					break;
				default://charge, refresh, cycle
					if (this.peakSenseVoltage == 0)
						sb.append(String.format(Locale.ENGLISH, "%-14s 0dV", Messages.getString(MessageIds.GDE_MSGT3674))).append(GDE.LINE_SEPARATOR);
					else
						sb.append(String.format(Locale.ENGLISH, "%-14s %dmV", Messages.getString(MessageIds.GDE_MSGT3674), this.peakSenseVoltage)).append(GDE.LINE_SEPARATOR);

					if (this.trickleCurrent != 0)
						sb.append(String.format(Locale.ENGLISH, "%-14s %dmA", Messages.getString(MessageIds.GDE_MSGT3675), this.trickleCurrent * 10)).append(GDE.LINE_SEPARATOR);
					else
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3675))).append(GDE.LINE_SEPARATOR);

					if (this.firmwareVersion > 111)
						sb.append(String.format(Locale.ENGLISH, "%-14s %s", Messages.getString(MessageIds.GDE_MSGT3676), MC3000.trickleTimeValues[this.trickleTime])).append(GDE.LINE_SEPARATOR);
					else
						sb.append(String.format(Locale.ENGLISH, "%-14s ?? FW <=1.11", Messages.getString(MessageIds.GDE_MSGT3676))).append(GDE.LINE_SEPARATOR);
					break;
				}
				break;
			}
		}

		private void appendCcCvTerminationCurrent(final StringBuilder sb, final boolean isToolTip) {
			//battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
			//Mode LI battery： 		0=CHARGE 1=REFRESH 2=STORAGE   3=DISCHARGE 4=CYCLE
			//Mode Zn battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
			//Mode RAM battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
			switch (this.batteryType) {
			case 3://NiMH
			case 4://NiCd
			case 6://Eneloop
				//no termination current with Ni batterie type, using delta peak voltage drop
				break;

			default:
			case 0://LiIon
			case 1://LiFe
			case 2://LiHV
			case 8://LiTo
				switch (this.operationMode) {
				case 3://discharge
					if (!isToolTip)
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3668))).append(GDE.LINE_SEPARATOR);
					break;
				case 1://refresh
				case 4://cycle
				default://charge, storage  {Zero|current range|OFF}
					if (this.getChargeEndCurrent() == 0)
						sb.append(String.format(Locale.ENGLISH, "%-14s Zero", Messages.getString(MessageIds.GDE_MSGT3668))).append(GDE.LINE_SEPARATOR);
					else if (this.getChargeEndCurrent() >= this.getChargeCurrent())
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3668))).append(GDE.LINE_SEPARATOR);
					else
						sb.append(String.format(Locale.ENGLISH, "%-14s %4.2fA", Messages.getString(MessageIds.GDE_MSGT3668), this.getChargeEndCurrent() / 1000.0)).append(GDE.LINE_SEPARATOR);
					break;
				}
				break;

			case 5://NiZn
			case 7://RAM
				switch (this.operationMode) {
				case 2://discharge
					if (!isToolTip) 
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3668))).append(GDE.LINE_SEPARATOR);
					break;
				case 1://refresh
				case 3://cycle
				default://charge
					if (this.getChargeEndCurrent() == 0)
						sb.append(String.format(Locale.ENGLISH, "%-14s Zero", Messages.getString(MessageIds.GDE_MSGT3668))).append(GDE.LINE_SEPARATOR);
					else if (this.getChargeEndCurrent() >= this.getChargeCurrent())
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3668))).append(GDE.LINE_SEPARATOR);
					else
						sb.append(String.format(Locale.ENGLISH, "%-14s %4.2fA", Messages.getString(MessageIds.GDE_MSGT3668), this.getChargeEndCurrent() / 1000.0)).append(GDE.LINE_SEPARATOR);
					break;
				}
				break;
			}
		}

		private void appendCycleCountCycleMode(final StringBuilder sb, final boolean isToolTip) {
			//battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
			//Mode LI battery： 		0=CHARGE 1=REFRESH 2=STORAGE   3=DISCHARGE 4=CYCLE
			//Mode Ni battery:		0=CHARGE 1=REFRESH 2=BREAK_IN  3=DISCHARGE 4=CYCLE
			//Mode Zn battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
			//Mode RAM battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
			switch (this.batteryType) {
			default:
			case 0://LiIon
			case 1://LiFe
			case 2://LiHV
			case 8://LiTo
				switch (this.operationMode) {
				case 1://refresh
					sb.append(String.format(Locale.ENGLISH, "%-14s %d", Messages.getString(MessageIds.GDE_MSGT3665), 1)).append(GDE.LINE_SEPARATOR);
					sb.append(String.format(Locale.ENGLISH, "%-14s %s", Messages.getString(MessageIds.GDE_MSGT3666), MC3000.cycleModeNames[1])).append(GDE.LINE_SEPARATOR);
					break;
				case 4://cycle
					sb.append(String.format(Locale.ENGLISH, "%-14s %d", Messages.getString(MessageIds.GDE_MSGT3665), this.numberCycle)).append(GDE.LINE_SEPARATOR);
					sb.append(String.format(Locale.ENGLISH, "%-14s %s", Messages.getString(MessageIds.GDE_MSGT3666), this.getCycleModeString())).append(GDE.LINE_SEPARATOR);
					break;
				default://charge, discharge, storage
					if (!isToolTip) {
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3665))).append(GDE.LINE_SEPARATOR);
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3666), this.getCycleModeString())).append(GDE.LINE_SEPARATOR);
					}
					break;
				}
				break;

			case 3://NiMH
			case 4://NiCd
			case 6://Eneloop
				switch (this.operationMode) {
				case 1://refresh
					sb.append(String.format(Locale.ENGLISH, "%-14s %d", Messages.getString(MessageIds.GDE_MSGT3665), 1)).append(GDE.LINE_SEPARATOR);
					sb.append(String.format(Locale.ENGLISH, "%-14s %s", Messages.getString(MessageIds.GDE_MSGT3666), MC3000.cycleModeNames[1])).append(GDE.LINE_SEPARATOR);
					break;
				case 2://break_in C>D>C | D>C>D 
					if (!isToolTip) sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3665))).append(GDE.LINE_SEPARATOR);
					sb.append(String.format(Locale.ENGLISH, "%-14s %s", Messages.getString(MessageIds.GDE_MSGT3666), this.cycleMode == 0 ? MC3000.cycleModeNames[1] : MC3000.cycleModeNames[3])).append(
							GDE.LINE_SEPARATOR);
					break;
				case 4://cycle
					sb.append(String.format(Locale.ENGLISH, "%-14s %d", Messages.getString(MessageIds.GDE_MSGT3665), this.numberCycle)).append(GDE.LINE_SEPARATOR);
					sb.append(String.format(Locale.ENGLISH, "%-14s %s", Messages.getString(MessageIds.GDE_MSGT3666), this.getCycleModeString())).append(GDE.LINE_SEPARATOR);
					break;
				default://charge, discharge
					if (!isToolTip) {
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3665))).append(GDE.LINE_SEPARATOR);
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3666), this.getCycleModeString())).append(GDE.LINE_SEPARATOR);
					}
					break;
				}
				break;

			case 5://NiZn
			case 7://RAM
				switch (this.operationMode) {
				case 1://refresh
					sb.append(String.format(Locale.ENGLISH, "%-14s %d", Messages.getString(MessageIds.GDE_MSGT3665), 1)).append(GDE.LINE_SEPARATOR);
					sb.append(String.format(Locale.ENGLISH, "%-14s %s", Messages.getString(MessageIds.GDE_MSGT3666), MC3000.cycleModeNames[1])).append(GDE.LINE_SEPARATOR);
					break;
				case 3://cycle
					sb.append(String.format(Locale.ENGLISH, "%-14s %d", Messages.getString(MessageIds.GDE_MSGT3665), this.numberCycle)).append(GDE.LINE_SEPARATOR);
					sb.append(String.format(Locale.ENGLISH, "%-14s %s", Messages.getString(MessageIds.GDE_MSGT3666), this.getCycleModeString())).append(GDE.LINE_SEPARATOR);
					break;
				default://charge, discharge
					if (!isToolTip) {
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3665))).append(GDE.LINE_SEPARATOR);
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3666), this.getCycleModeString())).append(GDE.LINE_SEPARATOR);
					}
					break;
				}
				break;
			}
		}

		private void appendCapacityModelNominal(StringBuilder sb) {
			//battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
			switch (this.batteryType) {
			default:
			case 0://LiIon
			case 1://LiFe
			case 2://LiHV
			case 5://NiZn
			case 7://RAM
			case 8://LiTo
				//Capacity
				if (getCapacity() == 0)
					sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3660))).append(GDE.LINE_SEPARATOR);
				else
					sb.append(String.format(Locale.ENGLISH, "%-14s %dmAh", Messages.getString(MessageIds.GDE_MSGT3660), getCapacity())).append(GDE.LINE_SEPARATOR);
				break;

			case 3://NiMH
			case 4://NiCd
				//Capacity - Nominal (braek_in)
				switch (this.operationMode) {
				case 2://break_in
					sb.append(String.format(Locale.ENGLISH, "%-14s %dmAh", Messages.getString(MessageIds.GDE_MSGT3685), getCapacity())).append(GDE.LINE_SEPARATOR);
					break;
				default:
					if (getCapacity() == 0)
						sb.append(String.format(Locale.ENGLISH, "%-14s OFF", Messages.getString(MessageIds.GDE_MSGT3660))).append(GDE.LINE_SEPARATOR);
					else
						sb.append(String.format(Locale.ENGLISH, "%-14s %dmAh", Messages.getString(MessageIds.GDE_MSGT3660), getCapacity())).append(GDE.LINE_SEPARATOR);
					break;
				}
				break;

			case 6://Eneloop
				//Model
				int i = 0;
				if (this.firmwareVersion <= 111) {
					for (; this.batteryType == 6 && i < MC3000.cellModelCapacity111.length; i++)
						if (this.getCapacity() == MC3000.cellModelCapacity111[i]) break;
					sb.append(String.format(Locale.ENGLISH, "%-14s %s", Messages.getString(MessageIds.GDE_MSGT3684), MC3000.cellModelNames111[i])).append(GDE.LINE_SEPARATOR);
				}
				else {
					for (; i < MC3000.cellModelCapacity112.length; i++)
						if (this.getCapacity() == MC3000.cellModelCapacity112[i]) break;
					sb.append(String.format(Locale.ENGLISH, "%-14s %s", Messages.getString(MessageIds.GDE_MSGT3684), MC3000.cellModelNames112[i])).append(GDE.LINE_SEPARATOR);
				}
				break;
			}
		}

		/**
		 * "NiMH — Cycle — -1.50/3.00A — N=4"
		 * "LiIon — Storage — 3.75V"
		 * "Eneloop — Break_in — Std AA"
		 * "NiCd — Break_in — 2400mAh"
		 * "LiFe — Charge — 2.50A"
		 * "RAM — Discharge — -0.75A"
		 * "LiIo4.35 — Refresh — -1.50/3.00A"
		 * "NiZn — Cycle — -1.50/3.00A — N=2"
		 * @return string for display
		 */
		public String toString4View() {

			StringBuilder sb = new StringBuilder();
			//add battery type "LiIo","LiFe","LiHV","NiMH","NiCd","NiZn","Eneloop","RAM"
			sb.append(MC3000.cellTypeNames[this.batteryType]).append(GDE.STRING_BLANK).append(GDE.STRING_BLANK);
			//add operation mode
			appendOperationMode(sb);
			sb.append(GDE.STRING_BLANK).append(GDE.STRING_BLANK);

			switch (this.batteryType) {
			case 0: //LiIo
			case 1: //LiFe
			case 2: //LiHV
			case 3: //NiMH
			case 4: //NiCd
			case 6: //Eneloop
			case 8: //LiTo
				switch (this.operationMode) {
				case 0: //CHARGE
					sb.append(String.format(Locale.ENGLISH, "%4.2fA", this.getChargeCurrent() / 1000.0));
					break;
				case 1: //REFRESH
					sb.append(String.format(Locale.ENGLISH, "-%4.2f/%4.2fA", this.getDischargeCurrent() / 1000.0, this.getChargeCurrent() / 1000.0));
					break;
				case 2: //STORAGE or BREAKIN
					switch (this.batteryType) {
					case 6: //Eneloop
						int i = 0;
						if (this.firmwareVersion <= 111) {
							for (; i < MC3000.cellModelCapacity111.length; i++) {
								if (this.getCapacity() == MC3000.cellModelCapacity111[i]) {
									sb.append(MC3000.cellModelNames111[i]);
									break;
								}
							}
						}
						else {
							for (; i < MC3000.cellModelCapacity112.length; i++) {
								if (this.getCapacity() == MC3000.cellModelCapacity112[i]) {
									sb.append(MC3000.cellModelNames112[i]);
									break;
								}
							}
						}
						break;
					case 0: //LiIo
					case 1: //LiFe
					case 2: //LiHV
					case 8: //LiTo
						sb.append(String.format(Locale.ENGLISH, "%3.2fV", this.getChargeEndVoltage() / 1000.0));
						break;
					case 3: //NiMH
					case 4: //NiCd
					default:
						sb.append(String.format(Locale.ENGLISH, "%dmAh", this.getCapacity()));
						break;
					}
					break;
				case 3: //DISCHARGE
					sb.append(String.format(Locale.ENGLISH, "-%4.2fA", this.getDischargeCurrent() / 1000.0));
					break;
				case 4: //CYCLE
					sb.append(String.format(Locale.ENGLISH, "-%4.2f/%4.2fA", this.getDischargeCurrent() / 1000.0, this.getChargeCurrent() / 1000.0));
					sb.append(String.format("  N=%d (%s)", this.numberCycle, this.getCycleModeString()));
					break;
				default:
					break;
				}
				break;
			//battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
			//Mode LI battery： 		0=CHARGE 1=REFRESH 2=STORAGE   3=DISCHARGE 4=CYCLE
			//Mode Ni battery:		0=CHARGE 1=REFRESH 2=BREAK_IN  3=DISCHARGE 4=CYCLE
			//Mode Zn battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
			//Mode RAM battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE

			case 5: //NiZn
			case 7: //RAM
			default:
				switch (this.operationMode) {
				case 0: //CHARGE
					sb.append(String.format(Locale.ENGLISH, "%4.2fA", this.getChargeCurrent() / 1000.0));
					break;
				case 1: //REFRESH
					sb.append(String.format(Locale.ENGLISH, "-%4.2f/%4.2fA", this.getDischargeCurrent() / 1000.0, this.getChargeCurrent() / 1000.0));
					break;
				case 2: //DISCHARGE
					sb.append(String.format(Locale.ENGLISH, "-%4.2fA", this.getDischargeCurrent() / 1000.0));
					break;
				case 3: //CYCLE
					sb.append(String.format(Locale.ENGLISH, "-%4.2f/%4.2fA", this.getDischargeCurrent() / 1000.0, this.getChargeCurrent() / 1000.0));
					sb.append(String.format("  N=%d (%s)", this.numberCycle, this.getCycleModeString()));
					break;
				default:
					break;
				}
				break;
			}
			return sb.toString().trim();
		}

		private void appendOperationMode(StringBuilder sb) {
			//battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
			switch (this.batteryType) {
			default:
			case 0://LiIon
			case 1://LiFe
			case 2://LiHV
			case 8://LiTo
				//Capacity
				//Mode LI battery： 		0=CHARGE 1=REFRESH 2=STORAGE   3=DISCHARGE 4=CYCLE
				sb.append(MC3000.operationModeLi[this.operationMode]);
				break;

			case 5://NiZn
			case 7://RAM
				//Capacity
				//Mode Zn/RAM battery:0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
				sb.append(MC3000.operationModeZnRAM[this.operationMode]);
				break;

			case 3://NiMH
			case 4://NiCd
				//Capacity - Nominal (braek_in)
				//Mode Ni battery:		0=CHARGE 1=REFRESH 2=BREAK_IN  3=DISCHARGE 4=CYCLE
				sb.append(MC3000.operationModeNi[this.operationMode]);
				break;

			case 6://Eneloop
				//Model
				//Mode Ni battery:		0=CHARGE 1=REFRESH 2=BREAK_IN  3=DISCHARGE 4=CYCLE
				sb.append(MC3000.operationModeNi[this.operationMode]);
				break;
			}
		}

		public byte[] getBuffer() {
			return this.slotBuffer;
		}

		public byte[] getBuffer(final byte newSlotNumber, final int firmwareAsNumber, final byte systemTemperatureUnit) {
			byte[] reducedBuffer = new byte[64];
			if (firmwareAsNumber <= 111) {
				reducedBuffer[0] = 0x0F;
				reducedBuffer[1] = 0x1D; //29
				reducedBuffer[2] = 0x11;
				reducedBuffer[3] = 0x00;
				reducedBuffer[4] = newSlotNumber;
				reducedBuffer[5] = this.batteryType;
				reducedBuffer[6] = (byte) (this.getCapacity() / 100);
				reducedBuffer[7] = this.operationMode;
				System.arraycopy(this.slotBuffer, 7, reducedBuffer, 8, 17);//charge current to trickle current
				reducedBuffer[25] = this.cutTemperature;
				System.arraycopy(this.slotBuffer, 27, reducedBuffer, 26, 2);//cut time
				System.arraycopy(this.slotBuffer, 24, reducedBuffer, 28, 2);//restart voltage
				reducedBuffer[30] = MC3000UsbPort.calculateCheckSum(reducedBuffer, reducedBuffer[1]);
				reducedBuffer[31] = (byte) 0xFF;
				reducedBuffer[32] = (byte) 0xFF;
				if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) {
					MC3000.log.log(java.util.logging.Level.FINE, StringHelper.byte2Hex2CharString(reducedBuffer, 64));
					MC3000.log
							.log(
									java.util.logging.Level.FINE,
									String
											.format(
													"slot#=%02d BATT TYPE=%02d MODE=%02d CAPACITY=%04d C.CURRENT=%04d D.CURRENT=%04d D.REDUCE=%04d TARGET VOLT=%04d D.REDUCE=%04d TERMINATION=%04d CYCLE COUNT=%02d C.RESTING=%02d CYCLE MODE=%d DELTA PEAK=%02d TRICKLE C=%02d CUT TEMP=%02d CUT TIME=%03d RESTART VOLT=%04d",
													reducedBuffer[4], reducedBuffer[5], this.operationMode, (reducedBuffer[6] & 0xFF) * 100, getChargeCurrent(), getDischargeCurrent(), getDischargeReduceCurrent(),
													getChargeEndVoltage(), getDischargeReduceCurrent(), getChargeEndCurrent(), this.numberCycle, this.chargeRestingTime & 0xFF, this.cycleMode, this.peakSenseVoltage,
													this.trickleCurrent, this.cutTemperature, DataParser.parse2UnsignedShort(reducedBuffer[27], reducedBuffer[26]),
													DataParser.parse2UnsignedShort(reducedBuffer[29], reducedBuffer[28])));
				}

				//D.RESTING: TRICKLE TIME: CUT VOLT: missing in FW <= 1.11
			}
			else {
				reducedBuffer[0] = 0x0F;
				reducedBuffer[1] = 0x20; //32
				reducedBuffer[2] = 0x11;
				reducedBuffer[3] = 0x00;
				reducedBuffer[4] = newSlotNumber;
				reducedBuffer[5] = this.batteryType;
				System.arraycopy(this.slotBuffer, 5, reducedBuffer, 6, 2);//capacity
				reducedBuffer[8] = this.operationMode;
				System.arraycopy(this.slotBuffer, 7, reducedBuffer, 9, 14);//C.CURRRENT to C.RESTING
				reducedBuffer[23] = this.dischargeRestingTime;//D.RESTING
				reducedBuffer[24] = this.cycleMode;
				reducedBuffer[25] = this.peakSenseVoltage;
				reducedBuffer[26] = this.trickleCurrent;
				reducedBuffer[27] = this.trickleTime;
				reducedBuffer[28] = (byte) this.getCutTemperature(systemTemperatureUnit);
				System.arraycopy(this.slotBuffer, 27, reducedBuffer, 29, 2);//cut time
				System.arraycopy(this.slotBuffer, 24, reducedBuffer, 31, 2);//restart voltage
				reducedBuffer[33] = MC3000UsbPort.calculateCheckSum(reducedBuffer, reducedBuffer[1]);
				reducedBuffer[34] = (byte) 0xFF;
				reducedBuffer[35] = (byte) 0xFF;
				if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) {
					MC3000.log.log(java.util.logging.Level.FINE, StringHelper.byte2Hex2CharString(reducedBuffer, 64));
					MC3000.log
							.log(
									java.util.logging.Level.FINE,
									String
											.format(
													"slot#=%02d BATT TYPE=%02d MODE=%02d CAPACITY=%04d C.CURRENT=%04d D.CURRENT=%04d D.REDUCE=%04d TARGET VOLT=%04d D.REDUCE=%04d TERMINATION=%04d CYCLE COUNT=%02d C.RESTING=%02d CYCLE MODE=%d DELTA PEAK=%02d TRICKLE C=%02d CUT TEMP=%02d CUT TIME=%03d RESTART VOLT=%04d",
													reducedBuffer[4], reducedBuffer[5], this.operationMode, (reducedBuffer[6] & 0xFF) * 100, getChargeCurrent(), getDischargeCurrent(), getDischargeReduceCurrent(),
													getChargeEndVoltage(), getDischargeReduceCurrent(), getChargeEndCurrent(), this.numberCycle, this.chargeRestingTime & 0xFF, this.cycleMode, this.peakSenseVoltage,
													this.trickleCurrent, this.getCutTemperature(systemTemperatureUnit), DataParser.parse2UnsignedShort(reducedBuffer[27], reducedBuffer[26]),
													DataParser.parse2UnsignedShort(reducedBuffer[29], reducedBuffer[28])));
				}

				//D.RESTING: TRICKLE TIME: CUT VOLT: missing in FW <= 1.11
			}
			return reducedBuffer;
		}

		public void setSlotNumber(final byte newSlotNumber) {
			this.slotNumber = newSlotNumber;
			this.slotBuffer[1] = newSlotNumber;
			this.setCheckSum(MC3000UsbPort.calculateCheckSum(this.slotBuffer));
		}

		public int getSlotNumber() {
			return this.slotNumber;
		}

		public byte getBatteryType() {
			return this.batteryType;
		}

		public byte getOperatinoMode() {
			return this.operationMode;
		}

		public int getCapacity() {
			return DataParser.parse2UnsignedShort(this.capacity[1], this.capacity[0]);
		}

		public int getChargeCurrent() {
			return DataParser.parse2UnsignedShort(this.chargeCurrent[1], this.chargeCurrent[0]);
		}

		public int getDischargeCurrent() {
			return DataParser.parse2UnsignedShort(this.dischargeCurrent[1], this.dischargeCurrent[0]);
		}

		public int getDischargeCutVoltage() {
			return DataParser.parse2UnsignedShort(this.dischargeCutVoltage[1], this.dischargeCutVoltage[0]);
		}

		public int getChargeEndVoltage() {
			return DataParser.parse2UnsignedShort(this.chargeEndVoltage[1], this.chargeEndVoltage[0]);
		}

		public int getDischargeReduceCurrent() {
			return DataParser.parse2UnsignedShort(this.dischargeReduceCurrent[1], this.dischargeReduceCurrent[0]);
		}

		public int getChargeEndCurrent() {
			return DataParser.parse2UnsignedShort(this.chargeEndCurrent[1], this.chargeEndCurrent[0]);
		}

		public short getNumberCycle() {
			return this.numberCycle;
		}

		public short getChargeRestingTime() {
			return (short) (this.chargeRestingTime & 0xFF);
		}

		public byte getCycleMode() {
			return this.cycleMode;
		}

		public String getCycleModeString() {
			return MC3000.cycleModeNames[this.cycleMode];
		}

		public short getEndDeltaVoltage() {
			return (short) (this.peakSenseVoltage & 0xFF);
		}

		public short getTrickleCurrent() {
			return (short) (this.trickleCurrent & 0xFF);
		}

//		/**
//		 * get cut temperature from buffer in °C 
//		 * @param systemtemperatureUnit
//		 * @return
//		 */
//		public byte getCutTemperatureInCelsius() {
//			if (this.temperatureUnit == 0) //buffer temperature is °C
//				return (byte) (this.cutTemperature & 0xFF);
//
//			//buffer temperature unit is °F
//			return (byte) (((this.cutTemperature - 32)*5/9) & 0xFF);
//		}

		/**
		 * get cut temperature from buffer according system cut temperature setting
		 * @param systemtemperatureUnit
		 * @return
		 */
		public short getCutTemperature(final byte systemtemperatureUnit) {
			switch (systemtemperatureUnit) {
			case 1://°F = °C × 9/5 + 32
				if (this.temperatureUnit == 0) //buffer temperature unit == °C
					return (short) (((this.cutTemperature&0xFF)*1.8) + 32);

				//buffer temperature unit is °F
				return (short) (this.cutTemperature & 0xFF);
				

			case 0://°C = (°F − 32) * 5/9
			default:
				if (this.temperatureUnit == 0) //buffer temperature unit == °C
					return (short) (this.cutTemperature & 0xFF);

				//buffer temperature unit is °F
				return (short) (((this.cutTemperature&0xFF) - 32)/1.8);
			}
		}

		public int getCutTime() {
			return DataParser.parse2UnsignedShort(this.cutTime[1], this.cutTime[0]);
		}

		public int getRestartVoltage() {
			return DataParser.parse2UnsignedShort(this.restartVoltage[1], this.restartVoltage[0]);
		}

		public void setCheckSum(final byte newChecksum) {
			this.slotBuffer[this.slotBuffer.length - 1] = newChecksum;
		}
	}

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public MC3000(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.skyrc.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.STATUS_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT3600), Messages.getString(MessageIds.GDE_MSGT3601), Messages.getString(MessageIds.GDE_MSGT3602),
				Messages.getString(MessageIds.GDE_MSGT3603), Messages.getString(MessageIds.GDE_MSGT3604), Messages.getString(MessageIds.GDE_MSGT3605) };
		this.USAGE_MODE_LI = new String[] { Messages.getString(MessageIds.GDE_MSGT3610), Messages.getString(MessageIds.GDE_MSGT3611), Messages.getString(MessageIds.GDE_MSGT3612),
				Messages.getString(MessageIds.GDE_MSGT3613), Messages.getString(MessageIds.GDE_MSGT3614) };
		this.USAGE_MODE_NI = new String[] { Messages.getString(MessageIds.GDE_MSGT3620), Messages.getString(MessageIds.GDE_MSGT3621), Messages.getString(MessageIds.GDE_MSGT3622),
				Messages.getString(MessageIds.GDE_MSGT3623), Messages.getString(MessageIds.GDE_MSGT3624) };
		this.USAGE_MODE_ZN = new String[] { Messages.getString(MessageIds.GDE_MSGT3620), Messages.getString(MessageIds.GDE_MSGT3621), Messages.getString(MessageIds.GDE_MSGT3623),
				Messages.getString(MessageIds.GDE_MSGT3624), Messages.getString(MessageIds.GDE_MSGT3624) };
		this.BATTERY_TYPE = new String[] { Messages.getString(MessageIds.GDE_MSGT3640), Messages.getString(MessageIds.GDE_MSGT3641), Messages.getString(MessageIds.GDE_MSGT3642),
				Messages.getString(MessageIds.GDE_MSGT3643), Messages.getString(MessageIds.GDE_MSGT3644), Messages.getString(MessageIds.GDE_MSGT3645), Messages.getString(MessageIds.GDE_MSGT3646),
				Messages.getString(MessageIds.GDE_MSGT3647), Messages.getString(MessageIds.GDE_MSGT3686) };

		this.application = DataExplorer.getInstance();
		this.settings = Settings.getInstance();
		this.usbPort = new MC3000UsbPort(this, this.application);
		this.dialog = new MC3000Dialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public MC3000(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.skyrc.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.STATUS_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT3600), Messages.getString(MessageIds.GDE_MSGT3601), Messages.getString(MessageIds.GDE_MSGT3602),
				Messages.getString(MessageIds.GDE_MSGT3603), Messages.getString(MessageIds.GDE_MSGT3604), Messages.getString(MessageIds.GDE_MSGT3605) };
		this.USAGE_MODE_LI = new String[] { Messages.getString(MessageIds.GDE_MSGT3610), Messages.getString(MessageIds.GDE_MSGT3611), Messages.getString(MessageIds.GDE_MSGT3612),
				Messages.getString(MessageIds.GDE_MSGT3613), Messages.getString(MessageIds.GDE_MSGT3614) };
		this.USAGE_MODE_NI = new String[] { Messages.getString(MessageIds.GDE_MSGT3620), Messages.getString(MessageIds.GDE_MSGT3621), Messages.getString(MessageIds.GDE_MSGT3622),
				Messages.getString(MessageIds.GDE_MSGT3623), Messages.getString(MessageIds.GDE_MSGT3624) };
		this.USAGE_MODE_ZN = new String[] { Messages.getString(MessageIds.GDE_MSGT3620), Messages.getString(MessageIds.GDE_MSGT3621), Messages.getString(MessageIds.GDE_MSGT3623),
				Messages.getString(MessageIds.GDE_MSGT3624), Messages.getString(MessageIds.GDE_MSGT3624) };
		this.BATTERY_TYPE = new String[] { Messages.getString(MessageIds.GDE_MSGT3640), Messages.getString(MessageIds.GDE_MSGT3641), Messages.getString(MessageIds.GDE_MSGT3642),
				Messages.getString(MessageIds.GDE_MSGT3643), Messages.getString(MessageIds.GDE_MSGT3644), Messages.getString(MessageIds.GDE_MSGT3645), Messages.getString(MessageIds.GDE_MSGT3646),
				Messages.getString(MessageIds.GDE_MSGT3647), Messages.getString(MessageIds.GDE_MSGT3686) };

		this.application = DataExplorer.getInstance();
		this.settings = Settings.getInstance();
		this.usbPort = new MC3000UsbPort(this, this.application);
		this.dialog = new MC3000Dialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		}
	}

	/**
	 * @return the dialog
	 */
	public DeviceDialog getDialog() {
		return this.dialog;
	}

	/**
	 * load the mapping exist between lov file configuration keys and gde keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// ...
		return lov2osdMap;
	}

	/**
	 * convert record LogView config data to gde config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber 
	 * @return converted configuration data
	 */
	@Override
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	@Override
	public int getLovDataByteSize() {
		return 86; //sometimes first 4 bytes give the length of data + 4 bytes for number
	}

	/**
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview 
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 * at least an update of the graphics window should be included at the end of this method
	 */
	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		boolean configChanged = this.isChangePropery();
		Record record;
		MeasurementType measurement;
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			measurement = this.getMeasurement(channelConfigNumber, i);
			if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, record.getName() + " = " + DeviceXmlResource.getInstance().getReplacement(this.getMeasurementNames(channelConfigNumber)[i])); //$NON-NLS-1$

			// update active state and displayable state if configuration switched with other names
			if (record.isActive() && record.isActive() != measurement.isActive()) {
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData() && measurement.isActive());
				if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$ 
			}

			if (record.isActive() && record.isDisplayable()) {
				if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
		this.setChangePropery(configChanged); //reset configuration change indicator to previous value, do not vote automatic configuration change at all
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//no implementation required
	}

	/**
	 * @return the device communication port
	 */
	@Override
	public MC3000UsbPort getCommunicationPort() {
		return this.usbPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION };
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	@Override
	public void open_closeCommPort() {
		if (this.usbPort != null) {
			if (!this.usbPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						((MC3000Dialog)this.getDialog()).dataGatherThread = new MC3000GathererThread(this.application, this, this.usbPort, activChannel.getNumber(), (MC3000Dialog) this.getDialog());
						try {
							if (((MC3000Dialog)this.getDialog()).dataGatherThread != null && this.usbPort.isConnected()) {
								this.systemSettings = new MC3000.SystemSettings(this.usbPort.getSystemSettings(((MC3000Dialog)this.getDialog()).dataGatherThread.getUsbInterface()));
								//WaitTimer.delay(100);
								//this.usbPort.startProcessing(this.getDialog().dataGatherThread.getUsbInterface());
								WaitTimer.delay(100);
								((MC3000Dialog)this.getDialog()).dataGatherThread.start();
							}
							else {
								this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
							}
						}
						catch (Throwable e) {
							MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
							//if (this.getDialog().dataGatherThread != null) this.usbPort.stopProcessing(this.getDialog().dataGatherThread.getUsbInterface());
						}
					}
				}
				catch (UsbClaimException e) {
					MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(),
							Messages.getString(gde.messages.MessageIds.GDE_MSGE0051, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
					try {
						if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
					}
					catch (UsbException ex) {
						MC3000.log.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
					}
				}
				catch (UsbException e) {
					MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					//if (this.getDialog().dataGatherThread != null) this.usbPort.stopProcessing(this.getDialog().dataGatherThread.getUsbInterface());
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0050));
					try {
						if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
					}
					catch (UsbException ex) {
						MC3000.log.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
					}
				}
				catch (ApplicationConfigurationException e) {
					MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					//if (this.getDialog().dataGatherThread != null) this.usbPort.stopProcessing(this.getDialog().dataGatherThread.getUsbInterface());
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (Throwable e) {
					MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					//if (this.getDialog().dataGatherThread != null) this.usbPort.stopProcessing(this.getDialog().dataGatherThread.getUsbInterface());
				}
			}
			else {
				if (((MC3000Dialog)this.getDialog()).dataGatherThread != null) {
					//this.usbPort.stopProcessing(this.getDialog().dataGatherThread.getUsbInterface());
					((MC3000Dialog)this.getDialog()).dataGatherThread.stopDataGatheringThread(false, null);
				}
				//if (this.getDialog().boundsComposite != null && !this.getDialog().isDisposed()) this.getDialog().boundsComposite.redraw();
				try {
					WaitTimer.delay(1000);
					if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
				}
				catch (UsbException e) {
					MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * this method is more usable for real logger, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	@Override
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		//not implemented
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		int chargeCorrection = (this.getProcessingType(dataBuffer) == 3 || ((this.getProcessingType(dataBuffer) == 1 || this.getProcessingType(dataBuffer) == 2 || this.getProcessingType(dataBuffer) == 4) && this
				.getProcessingStatus(dataBuffer) == 2)) ? -1 : 1;
		//0=Voltage 1=Current 2=Capacity 3=power 4=Energy 5=Temperature 6=Resistence
		points[0] = DataParser.parse2Short(dataBuffer[9], dataBuffer[8]) * 1000;
		points[1] = DataParser.parse2Short(dataBuffer[11], dataBuffer[10]) * 1000 * chargeCorrection;
		points[2] = DataParser.parse2Short(dataBuffer[13], dataBuffer[12]) * 1000;
		if (this.systemSettings != null && this.systemSettings.getFirmwareVersionAsInt() <= 105) {
			points[3] = Double.valueOf(points[0] / 1000.0 * points[1] / 1000.0 * chargeCorrection).intValue(); // power U*I [W]
			switch (dataBuffer[1]) {
			case 0: //add up energy
				points[4] += Double.valueOf((points[0] / 1000.0 * points[1] / 1000.0 * chargeCorrection) / 3600.0 + 0.5).intValue();
				break;
			case 1: // reset energy
				points[4] = 0;
				if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, "reset Energy");
				break;
			default: // keep energy untouched
			case -1: // keep energy untouched
				points[4] = points[4];
				if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, "untouche Energy");
				break;
			}
		}
		else {//firmware 1.05+ Energy and power comes direct from the device			
			points[3] = DataParser.parse2Short(dataBuffer[23], dataBuffer[22]) * 1000;
			points[4] = DataParser.parse2Short(dataBuffer[21], dataBuffer[20]) * 1000;
		}
		if (this.systemSettings != null && this.systemSettings.getFirmwareVersionAsInt() >= 111) {
			points[2] += dataBuffer[24] * 100; //capacity decimal
		}
		points[5] = DataParser.parse2Short(dataBuffer[15], dataBuffer[14]) * 1000;
		points[6] = DataParser.parse2Short(dataBuffer[17], dataBuffer[16]) * 1000;
		points[7] = DataParser.parse2Short(dataBuffer[19], dataBuffer[18]) * 1000;
		
		if (this.systemSettings != null && this.systemSettings.getFirmwareVersionAsInt() >= 114) {
			points[8] = DataParser.parse2Short(dataBuffer[26], dataBuffer[25]) * 1000;
		}

		return points;
	}

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal business to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.size()];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			MC3000.log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize, convertBuffer, 0, dataBufferSize);

			for (int j = 0, k = 0; j < points.length; j++, k += 4) {
				//0=Voltage 1=Current 2=Capacity 3=Power 4=Energy 5=Teperature 6=Resistance 7=SysTemperature
				points[j] = (((convertBuffer[k] & 0xff) << 24) + ((convertBuffer[k + 1] & 0xff) << 16) + ((convertBuffer[k + 2] & 0xff) << 8) + ((convertBuffer[k + 3] & 0xff) << 0));
			}
			recordSet.addPoints(points);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				dataTableRow[index + 1] = record.getDecimalFormat().format((offset + ((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
				++index;
			}
		}
		catch (RuntimeException e) {
			MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required

		double newValue = (value - reduction) * factor + offset;

		MC3000.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required

		double newValue = (value - offset) / factor + reduction;

		MC3000.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * query device for specific smoothing index
	 * 0 do nothing at all
	 * 1 current drops just a single peak
	 * 2 current drop more or equal than 2 measurements 
	 */
	@Override
	public int getCurrentSmoothIndex() {
		return 1;
	}

	/**
	 * check if one of the outlet channels are in processing mode
	 * STATUS:     0=standby 1=charge 2=discharge 3=resting 4=finish 0x80--0xff：error code
	 * @param outletNum
	 * @param dataBuffer
	 * @return true if channel 1 is active 
	 */
	public boolean isProcessing(final int outletNum, final byte[] dataBuffer) {
		if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, "isProcessing = " + dataBuffer[5]);

		//firmware 1.05+ power and energy comes direct from device
		//		if (this.resetEnergy[outletNum-1] != dataBuffer[5] || dataBuffer[5] > 2)
		//			if (dataBuffer[5] > 2)
		//				dataBuffer[1] = -1; //keep energy
		//			else
		//				dataBuffer[1] = 1;  //reset energy
		//		else
		//			dataBuffer[1] = 0;	//add up energy
		//		
		//		this.resetEnergy[outletNum-1] = dataBuffer[5];

		if (this.settings.isReduceChargeDischarge() && !this.isContinuousRecordSet()) 
			return dataBuffer[5] > 0 && dataBuffer[5] < 3;
		return dataBuffer[5] > 0 && dataBuffer[5] < 4;
	}

	/**
	 * STATUS:     0=standby 1=charge 2=discharge 3=resting 4=finish 0x80--0xff：error code
	 * @param dataBuffer
	 * @return
	 */
	public int getProcessingStatus(final byte[] dataBuffer) {
		return dataBuffer[5];
	}

	/**
	 * STATUS:     0=standby 1=charge 2=discharge 3=resting 4=finish 0x80--0xff：error code
	 * @param dataBuffer
	 * @return
	 */
	public boolean isProcessingStatusStandByOrFinished(final byte[] dataBuffer) {
		return dataBuffer[5] == 0 || dataBuffer[5] == 4;
	}

	/**
	 * STATUS:     0=standby 1=charge 2=discharge 3=resting 4=finish 0x80--0xff：error code
	 * @param dataBuffer
	 * @return
	 */
	public String getProcessingStatusName(final byte[] dataBuffer) {
		return this.isContinuousRecordSet() ? Messages.getString(MessageIds.GDE_MSGT3606) : this.STATUS_MODE[dataBuffer[5]];
	}

	
	/**
	 * @return true if setting is configured to continuous record set data gathering
	 */
	protected boolean isContinuousRecordSet() {
		return this.settings.isContinuousRecordSet() || this.application.getActiveDevice().getName().contains("-Set");
	}

	/**
	 * battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
	 * @param dataBuffer
	 * @return
	 */
	public String getProcessingBatteryTypeName(final byte[] dataBuffer) {
		return this.BATTERY_TYPE[dataBuffer[2]];
	}

	/**
	 * query the processing mode, main modes are charge/discharge, make sure the data buffer contains at index 15,16 the processing modes
	 * Mode LI battery： 	0=CHARGE 1=REFRESH 2=STORAGE 3=DISCHARGE 4=CYCLE
	 * Mode Ni battery:		0=CHARGE 1=REFRESH 2=PAUSE     3=DISCHARGE 4=CYCLE
	 * Mode Zn battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
	 * Mode RAM battery:	0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
	 * @param dataBuffer 
	 * @return
	 */
	public int getProcessingType(final byte[] dataBuffer) {
		return dataBuffer[3];
	}

	/**
	 * query battery type
	 * battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
	 * @param dataBuffer
	 * @return
	 */
	public int getBatteryType(byte[] dataBuffer) {
		return dataBuffer[2];
	}

	/**
	 * query the processing type name
	 * @param dataBuffer
	 * @return string of mode
	 */
	public String getProcessingTypeName(byte[] dataBuffer) {
		String processTypeName = GDE.STRING_EMPTY;
		//Mode LI battery： 		0=CHARGE 1=REFRESH 2=STORAGE 	 3=DISCHARGE 4=CYCLE
		//Mode Ni battery:		0=CHARGE 1=REFRESH 2=PAUSE     3=DISCHARGE 4=CYCLE
		//Mode Zn battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
		//Mode RAM battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE

		// battery type:     0:LiIon       1:LiFe        2:LiHV        3:NiMH        4:NiCd        5:NiZn        6:Eneloop
		switch (this.getBatteryType(dataBuffer)) {
		case 0: //LiIon
		case 1: //LiFe
		case 2: //LiHv
			processTypeName = this.USAGE_MODE_LI[this.getProcessingType(dataBuffer)];
			break;
		case 3: //NiMH
		case 4: //NiCD
		case 6: //Eneloop
			processTypeName = this.USAGE_MODE_NI[this.getProcessingType(dataBuffer)];
			break;
		case 5: //NiZn
		case 7: //RAM
			processTypeName = this.USAGE_MODE_ZN[this.getProcessingType(dataBuffer)];
			break;
		}
		return this.isContinuousRecordSet() ? Messages.getString(MessageIds.GDE_MSGT3606) : processTypeName;
	}

	/**
	 * query the cycle number of the given outlet channel
	 * @param outletNum
	 * @param dataBuffer
	 * @return
	 */
	public int getCycleNumber(int outletNum, byte[] dataBuffer) {
		return dataBuffer[4];
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public int[] getCellVoltageOrdinals() {
		//0=Voltage 1=Current 2=Capacity 3=power 4=Energy 5=Temperature 6=Resistance

		// Combi
		//0=Voltage 1=Voltage 2=Voltage 3=Voltage 4=Current 5=Current 6=Current 7=Current 8=Capacity 9=Capacity 10=Capacity 11=Capacity
		//12=Temperature 13=Temperature 14=Temperature 15=Temperature 16=Resistance 17=Resistance 18=Resistance 19=Resistance
		switch (Channels.getInstance().getActiveChannelNumber()) {
		case 1:
		case 2:
		case 3:
		case 4:
			return new int[] { 0, 2 };
		case 5:
		default:
			return new int[] { -1, -1 };
		}
	}

	/**
	 * @return string containing firmware : major.minor
	 */
	public String getHardwareString() {
		return this.systemSettings.getHardwareVersion();
	}

	/**
	 * @return string containing firmware : major.minor
	 */
	public String getFirmwareString() {
		return this.systemSettings.getFirmwareVersion();
	}

	/**
	 * @return string containing system temperatur unit
	 */
	public String getTemperatureUnit() {
		return this.systemSettings.getTemperatureUnit() == 0 ? "°C" : "°F";
	}

	/**
	 * @param slotNumber
	 * @param usbInterface
	 * @return slot related program/memory number
	 */
	public byte getBatteryMemoryNumber(final int slotNumber, UsbInterface usbInterface) {
		try {
			this.systemSettings = new SystemSettings(this.usbPort.getSystemSettings(usbInterface));
		}
		catch (Exception e) {
			return 0x00;
		}
		switch (slotNumber) {
		case 1:
			return this.systemSettings.getSlot1programmNumber();
		case 2:
			return this.systemSettings.getSlot2programmNumber();
		case 3:
			return this.systemSettings.getSlot3programmNumber();
		case 4:
			return this.systemSettings.getSlot4programmNumber();
		default:
			return 0x00;
		}
	}
	
	/**
	 * implementation returning device name which is the default directory name to store OSD files as well to search for
	 * @return the preferred directory to search and store for device specific files
	 */
	public String getFileBaseDir() {
		return this.getClass().getSimpleName();
	}
}
