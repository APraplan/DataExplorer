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

    Copyright (c) 2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.junsi.modbus;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.usb.UsbException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channels;
import gde.device.DeviceDialog;
import gde.device.junsi.MessageIds;
import gde.device.junsi.iChargerUsb;
import gde.device.junsi.iChargerX6;
import gde.device.junsi.modbus.ChargerUsbPort.ModBusErrorCode;
import gde.exception.TimeOutException;
import gde.io.DataParser;
import gde.log.Level;
import gde.log.LogFormatter;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.ParameterConfigControl;
import gde.ui.SWTResourceManager;
import gde.utils.WaitTimer;

public class ChargerDialog extends DeviceDialog {
	final static Logger								log													= Logger.getLogger(ChargerDialog.class.getName());
	static Handler										logHandler;
	static Logger											rootLogger;
	
	//protected Shell			dialogShell; //required to use WindowBuilder, even if declared in parent class


	final iChargerUsb									device;
	final static Channels							channels										= Channels.getInstance();
	final static Settings							settings										= Settings.getInstance();
	private ChargerUsbPort						usbPort											= null;
	private boolean										isPortOpenedByDialog				= false;
	private ChargerMemory							selectedProgramMemory				= null;
	private ChargerMemory							copiedProgramMemory					= null;
	private int												lastSelectedProgramMemoryIndex;
	private int												lastSelectedComboIndex = 0;
	byte[]														memoryHeadIndex;

	private CCombo										combo;
	private int												comboHeight									= GDE.IS_LINUX ? 24 : GDE.IS_MAC ? 20 : 22;
	private boolean										isComboSetEdit							= false;
	private Button										btnCopy, btnEdit, btnWrite, btnDelete, btnSystemSave;
	private Button										btnCharge, btnStorage, btnDischarge, btnCycle, btnBalance, btnPower, btnStop;
	private Group											grpProgramMemory, grpBalancerSettings, grpAdvancedRestoreSettings, grpChargeSaftySettings, grpDischargeSaftySettings, grpRunProgram;
	private CTabFolder								tabFolderProgrMem;
	private CTabItem									tbtmCharge, tbtmDischarge, tbtmStorage, tbtmCycle, tbtmOption, tbtmPower;
	private Composite									memoryComposite, chargeComposite, dischargeComposite, storageComposite, cycleComposite, optionComposite, powerComposite, sysComposite;
	private CLabel										powerLabel;
	private Group 										grpTemperature, grpFans, grpBeepTone, grpLcdScreen, grpChargeDischargePower;
	private Group 										grpInputDischargePowerLimits, grpInputPowerLimits, grpRegInputPowerLimits, grpLanguage;
	private Group 										grpDuoInputPowerLimits, grpDcInputPowerLimits, grpBatInputPowerLimits, grpDuoRegInputPowerLimits;
	private Group											regToChannelSettings;
	private CLabel										regToInputWarningLable;
	
	private ParameterConfigControl[]	memoryParameters						= new ParameterConfigControl[53];																																				
	private ParameterConfigControl[]	systemParameters						= new ParameterConfigControl[50];																																				
	private final String							cellTypeNames;
	private final String[]						cellTypeNamesArray;
	private int[]											memoryValues								= new int[memoryParameters.length];																		//values to be used for modifying sliders
	private int[]											systemValues								= new int[systemParameters.length];																		//values to be used for modifying sliders
	private final boolean							isDuo;
	private final boolean							isDx;
	final Listener										memoryParameterChangeListener;
	final Listener										systemParameterChangeListener;
	private ChargerInfo								systemInfo = null;
	private ChargerSystem							systemSettings = null;
	private boolean										isRetryReadMem = false;

	final static short								REG_INPUT_INFO_START				= 0x0000;
	
	final static short								REG_INPUT_CH1_START					= 0x0100;
	final static short								REG_INPUT_CH1_NREGS					= 0x0100;
	final static short								REG_INPUT_CH2_START					= (0x0100 + ChargerDialog.REG_INPUT_CH1_START);
	final static short								REG_INPUT_CH2_NREGS					= ChargerDialog.REG_INPUT_CH1_NREGS;

	final static short								REG_INPUT_STATUS_CH1				= REG_INPUT_CH1_START;
	final static short								REG_INPUT_STATUS_CH2				= REG_INPUT_CH2_START;

	final static short								REG_HOLDING_CTRL_START			= (short) 0x8000;
	final static short								REG_HOLDING_SYS_START				= (short) 0x8400;
	final static short								REG_HOLDING_MEM_HEAD_START	= (short) 0x8800;
	final static short								REG_HOLDING_MEM_START				= (short) 0x8c00;

	final static short								VALUE_ORDER_KEY							= 0x55aa;
//	private Group  grpSaveLoadConfig;
//	private Button btnSaveActualConf;
//	private Button btnRestoreSavedConf;
//	private Button btnLoadDefaultSysConf;

	enum Order {
		ORDER_STOP, ORDER_RUN, ORDER_MODIFY, ORDER_WRITE_SYS, ORDER_WRITE_MEM_HEAD, ORDER_WRITE_MEM, ORDER_TRANS_LOG_ON, ORDER_TRANS_LOG_OFF, ORDER_MSGBOX_YES, ORDER_MSGBOX_NO;
	};

	enum OperationLi {
		Charge, Storage, Discharge, Cycle, Balance;
	}

	enum OperationNiPb {
		Charge, Discharge, Cycle;
	}

	enum Register {
		REG_SEL_OP(ChargerDialog.REG_HOLDING_CTRL_START), REG_SEL_MEM((short) (ChargerDialog.REG_HOLDING_CTRL_START + 1)), REG_SEL_CHANNEL(
				(short) (ChargerDialog.REG_HOLDING_CTRL_START + 2)), REG_ORDER_KEY((short) (ChargerDialog.REG_HOLDING_CTRL_START + 3)), REG_ORDER(
						(short) (ChargerDialog.REG_HOLDING_CTRL_START + 4)), REG_CURRENT((short) (ChargerDialog.REG_HOLDING_CTRL_START + 5)), REG_VOLT((short) (ChargerDialog.REG_HOLDING_CTRL_START + 6));

		short													value;
		public final static Register	VALUES[]	= values();

		Register(short newValue) {
			this.value = newValue;
		}
	};

	/**
	* main method to test this dialog inside a dialogShell
	*/
	public static void main(String[] args) {
		try {
			initLogger();
			//iChargerUsb device = new iCharger4010DUO("c:\\Users\\Winfried\\AppData\\Roaming\\DataExplorer\\Devices\\iCharger406DUO.xml"); //$NON-NLS-1$
			iChargerUsb device = new iChargerX6("c:\\Users\\Winfried\\AppData\\Roaming\\DataExplorer\\Devices\\iCharger S6.xml"); //$NON-NLS-1$
			boolean isDuo = device.getName().endsWith("DUO"); //DUO = true; X6 = false
			boolean isDx = device.getName().contains(" DX");
			ChargerUsbPort usbPort = new ChargerUsbPort(device, null);
			if (!usbPort.isConnected()) usbPort.openUsbPort();

			if (usbPort.isConnected()) {
				//Read system info data
				short sizeInfo = (short) ((ChargerInfo.getSize() + 1) / 2);
				byte[] infoBuffer = new byte[sizeInfo * 2];
				usbPort.masterRead((byte) 1, ChargerDialog.REG_INPUT_INFO_START, sizeInfo, infoBuffer);
				ChargerInfo systemInfo = new ChargerInfo(infoBuffer);
				if (ChargerDialog.log.isLoggable(Level.INFO)) 
					ChargerDialog.log.log(Level.INFO, systemInfo.toString());

				//Read system setup data
				//MasterRead(0,REG_HOLDING_SYS_START,(sizeof(SYSTEM)+1)/2,(BYTE *)&System)
				short sizeSystem = (short) (((systemInfo != null ? systemInfo.getSystemMemoryLength() : ChargerSystem.getSize(isDuo, isDx)) + 1) / 2);
				byte[] systemBuffer = new byte[sizeSystem * 2];
				usbPort.masterRead((byte) 0, ChargerDialog.REG_HOLDING_SYS_START, sizeSystem, systemBuffer);
				ChargerDialog.log.log(Level.INFO, new ChargerSystem(systemBuffer, isDuo, isDx).toString(isDuo, isDx));
				
				//repair X6 while looping soft boot after writing wrong system memory using S6 configuration
//				if (usbPort.isConnected()) usbPort.closeUsbPort();
//				
//				byte[] temp = new byte[4];
//				temp[0] = (byte) (ChargerDialog.VALUE_ORDER_KEY & 0xFF);
//				temp[1] = (byte) (ChargerDialog.VALUE_ORDER_KEY >> 8);
//				temp[2] = (byte) Order.ORDER_WRITE_SYS.ordinal();
//				
//				byte[] temp2 = new byte[2];
//
//				boolean sysNotWritten = true;
//				do {
//					while (!usbPort.isConnected())
//						try {
//							usbPort.openUsbPort();
//						}
//						catch (Exception e) {
//							// ignore
//						}
//					usbPort.masterWrite(REG_HOLDING_SYS_START, sizeSystem, systemBuffer);
//					usbPort.masterWrite(Register.REG_ORDER_KEY.value, (short) 2, temp);
//					usbPort.masterWrite(Register.REG_ORDER_KEY.value, (short) 1, temp2);
//					sysNotWritten = false;
//				}
//				while (sysNotWritten);

				//Read memory structure of original and added/modified program memories
				//MasterWrite(REG_HOLDING_MEM_HEAD_START,(sizeof(MEM_HEAD)+1)/2,(BYTE *)&MemHead)
				short sizeMemHead = (short) ((ChargerMemoryHead.getSize(isDuo || isDx) + 1) / 2);
				byte[] memHeadBuffer = new byte[sizeMemHead * 2];
				usbPort.masterRead((byte) 0, ChargerDialog.REG_HOLDING_MEM_HEAD_START, sizeMemHead, memHeadBuffer);
				ChargerMemoryHead memHead = new ChargerMemoryHead(memHeadBuffer, isDuo || isDx);
				ChargerDialog.log.log(Level.INFO, memHead.toString());

				for (int i = 0; i < memHead.getCount(); ++i) {
					//Read charger program memory after write index selection
					//MasterWrite(REG_SEL_MEM,1,(BYTE *)&Index) != MB_EOK
					byte[] index = new byte[2];
					index[0] = memHead.getIndex()[i];
					ChargerDialog.log.log(Level.INFO, String.format("select mem index %d", DataParser.parse2Short(index[0], index[1]))); //$NON-NLS-1$
					usbPort.masterWrite(Register.REG_SEL_MEM.value, (short) 1, index);

					//MasterRead(0,REG_HOLDING_MEM_START,(sizeof(MEMORY)+1)/2,(BYTE *)&Memory) == MB_EOK
					short sizeMemory = (short) (((systemInfo != null ? systemInfo.getProgramMemoryLength() : ChargerMemory.getSize(isDuo)) + 1) / 2);
					byte[] memoryBuffer = new byte[sizeMemory * 2];
					usbPort.masterRead((byte) 0, ChargerDialog.REG_HOLDING_MEM_START, sizeMemory, memoryBuffer);
					ChargerDialog.log.log(Level.INFO, String.format("%02d %s", memHead.getIndex()[i], new ChargerMemory(memoryBuffer, isDuo).getUseFlagAndName(isDuo))); //$NON-NLS-1$
					ChargerDialog.log.log(Level.INFO, new ChargerMemory(memoryBuffer, isDuo).toString(isDuo));
				}
			}
			if (usbPort.isConnected()) usbPort.closeUsbPort(false);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ChargerInfo readInfo() {
		ChargerInfo chargerInfo = null;
		try {
			//Read system info data
			short sizeInfo = (short) ((ChargerInfo.getSize() + 1) / 2);
			byte[] infoBuffer = new byte[sizeInfo * 2];
			if (ModBusErrorCode.MB_EOK != this.usbPort.masterRead((byte) 1, ChargerDialog.REG_INPUT_INFO_START, sizeInfo, infoBuffer))
				throw new IllegalStateException();
			chargerInfo = new ChargerInfo(infoBuffer);
			if (ChargerDialog.log.isLoggable(Level.INFO)) 
				ChargerDialog.log.log(Level.INFO, chargerInfo.toString());
		}
		catch (IllegalStateException | TimeOutException e) {
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (RuntimeException rte) {
			ChargerDialog.log.log(Level.SEVERE, rte.getMessage(), rte);
		}
		return chargerInfo;
	}

	public ChargerStatus readStatus(int channelNumber) {
		ChargerStatus chargerStatus = null;
		try {
			//Read system info data
			short sizeStatus = (short) ((ChargerStatus.getSize() + 1) / 2);
			byte[] statusBuffer = new byte[sizeStatus * 2];
			if (channelNumber == 2)
				if (ModBusErrorCode.MB_EOK != this.usbPort.masterRead((byte) 1, ChargerDialog.REG_INPUT_STATUS_CH2, sizeStatus, statusBuffer))
					throw new IllegalStateException();
			else
				if (ModBusErrorCode.MB_EOK != this.usbPort.masterRead((byte) 1, ChargerDialog.REG_INPUT_STATUS_CH1, sizeStatus, statusBuffer))
					throw new IllegalStateException();
			
			chargerStatus = new ChargerStatus(statusBuffer);
			if (ChargerDialog.log.isLoggable(Level.INFO)) 
				ChargerDialog.log.log(Level.INFO, chargerStatus.toString());
		}
		catch (IllegalStateException | TimeOutException e) {
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (RuntimeException rte) {
			ChargerDialog.log.log(Level.SEVERE, rte.getMessage(), rte);
		}
		return chargerStatus;
	}

	public void readSystem(final boolean isDuo, final boolean isDx) {
		try {
			//Read system setup data
			//MasterRead(0,REG_HOLDING_SYS_START,(sizeof(SYSTEM)+1)/2,(BYTE *)&System)
			short sizeSystem = (short) (((this.systemInfo != null && this.systemInfo.getDeviceID() != 0 ? this.systemInfo.getSystemMemoryLength() : ChargerSystem.getSize(isDuo, isDx)) + 1) / 2);
			byte[] systemBuffer = new byte[sizeSystem * 2];
			if (ModBusErrorCode.MB_EOK != this.usbPort.masterRead((byte) 0, ChargerDialog.REG_HOLDING_SYS_START, sizeSystem, systemBuffer))
				throw new IllegalStateException();				
			this.systemSettings = new ChargerSystem(systemBuffer, isDuo, isDx);
			this.systemValues = this.systemSettings.getSystemValues(this.systemValues);
			if (ChargerDialog.log.isLoggable(Level.INFO)) ChargerDialog.log.log(Level.INFO, new ChargerSystem(systemBuffer, isDuo, isDx).toString(isDuo, isDx));
		}
		catch (IllegalStateException | TimeOutException e) {
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (RuntimeException rte) {
			ChargerDialog.log.log(Level.SEVERE, rte.getMessage(), rte);
		}
	}

	/**
	 * default constructor initialize all variables required
	 * @param parent dialogShell
	 * @param useDevice device specific class implementation
	 */
	public ChargerDialog(Shell parent, iChargerUsb useDevice) {
		super(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		this.device = useDevice;
		this.usbPort = ((iChargerUsb) this.device).getUsbPort();
		setText(this.device.getName());
		this.isDuo = this.device.getName().toLowerCase().endsWith("duo") && !this.device.getName().toLowerCase().contains("45") ? true : false; //$NON-NLS-1$
		this.isDx = device.getName().toLowerCase().contains(" dx") || this.device.getName().toLowerCase().endsWith("duo") && this.device.getName().toLowerCase().contains("45")? true : false; //$NON-NLS-1$
		String[] tmpNamesArray = this.isDuo ? iChargerUsb.BatteryTypesDuo.getValues() : iChargerX6.BatteryTypesX.getValues();
		this.cellTypeNamesArray = new String[tmpNamesArray.length - (this.isDuo ? 2 : 4)];
		System.arraycopy(tmpNamesArray, 1, this.cellTypeNamesArray, 0, this.cellTypeNamesArray.length);
		this.cellTypeNames = String.join(", ", this.cellTypeNamesArray); //$NON-NLS-1$
		this.memoryParameterChangeListener = addProgramMemoryChangedListener();
		this.systemParameterChangeListener = addSystemValuesChangedListener();
		this.memoryHeadIndex = new byte[ChargerMemoryHead.getMaxListIndex(isDuo)];
	}

	private Listener addProgramMemoryChangedListener() {
		return new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if (ChargerDialog.this.selectedProgramMemory != null && ChargerDialog.this.selectedProgramMemory.getUseFlag() == (short) 0x55aa) {
					ChargerDialog.this.btnWrite.setEnabled(true);

					ChargerDialog.this.btnCharge.setEnabled(false);
					ChargerDialog.this.btnStorage.setEnabled(false);
					ChargerDialog.this.btnDischarge.setEnabled(false);
					ChargerDialog.this.btnCycle.setEnabled(false);
					ChargerDialog.this.btnBalance.setEnabled(false);
					ChargerDialog.this.btnPower.setEnabled(false);
					ChargerDialog.this.btnStop.setEnabled(false);

					String changedProgramMemoryText = ChargerDialog.this.combo.getText().trim();
					if (changedProgramMemoryText.contains(" - ")) { //$NON-NLS-1$
						changedProgramMemoryText = changedProgramMemoryText.substring(changedProgramMemoryText.lastIndexOf(" - ") + 3); //$NON-NLS-1$
					}
					ChargerDialog.this.combo.setText(ChargerDialog.this.lastSelectedProgramMemoryIndex + Messages.getString(MessageIds.GDE_MSGT2625) + changedProgramMemoryText);
					ChargerDialog.this.combo.setForeground(ChargerDialog.this.application.COLOR_RED);

					switch (evt.index) {
					case 0: //battery type, disabled can be changed by selection in drop down only
					default:
						break;
					case 1: //cell count
						if (ChargerDialog.this.isDuo) {
							switch (ChargerDialog.this.selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,NiMH,Nicd,Pb,NiZn,LiHV
							case 0: //LiPo
							case 1: //LiIo
							case 2: //LiFe
							case 7: //LiHV
								ChargerDialog.this.selectedProgramMemory.setLiCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							case 3: //NiMH
							case 4: //NiCd
								ChargerDialog.this.selectedProgramMemory.setNiCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							case 6: //NiZn
								ChargerDialog.this.selectedProgramMemory.setNiZnCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							case 5: //Pb
								ChargerDialog.this.selectedProgramMemory.setPbCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							default: //unknown
								break;
							}
						}
						else {
							switch (ChargerDialog.this.selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,LiHV,LTO, NiMH,Nicd,NiZn,Pb
							case 0: //LiPo
							case 1: //LiIo
							case 2: //LiFe
							case 3: //LiHV
							case 4: //LTO
								ChargerDialog.this.selectedProgramMemory.setLiCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							case 7: //NiZn
								ChargerDialog.this.selectedProgramMemory.setNiZnCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							case 5: //NiMH
							case 6: //NiCd
								ChargerDialog.this.selectedProgramMemory.setNiCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							case 8: //Pb
								ChargerDialog.this.selectedProgramMemory.setPbCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							default: //Power
							}
						}
						break;
					case 2: //capacity
						ChargerDialog.this.selectedProgramMemory.setCapacity(ChargerDialog.this.memoryValues[2]);
						break;
					case 3: // charge parameter current
						ChargerDialog.this.selectedProgramMemory.setChargeCurrent((short) (ChargerDialog.this.memoryValues[3]));
						break;
					case 4: // charge parameter modus normal,balance,external,reflex
						if (ChargerDialog.this.isDuo) {
							switch (ChargerDialog.this.selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,NiMH,Nicd,Pb,NiZn,LiHV
							case 0: //LiPo
							case 1: //LiIo
							case 2: //LiFe
							case 6: //NiZn
							case 7: //LiHV
								ChargerDialog.this.selectedProgramMemory.setLiModeC((byte) ChargerDialog.this.memoryValues[4]);
								break;
							case 3: //NiMH
							case 4: //NiCd
								ChargerDialog.this.selectedProgramMemory.setNiModeC((byte) ChargerDialog.this.memoryValues[4]);
								break;
							case 5: //Pb
								ChargerDialog.this.selectedProgramMemory.setPbModeC((byte) ChargerDialog.this.memoryValues[4]);
								break;
							default: //unknown
								break;
							}
						}
						else {
							switch (ChargerDialog.this.selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,LiHV,LTO, NiMH,Nicd,NiZn,Pb
							case 0: //LiPo
							case 1: //LiIo
							case 2: //LiFe
							case 3: //LiHV
							case 4: //LTO
							case 7: //NiZn
								ChargerDialog.this.selectedProgramMemory.setLiModeC((byte) ChargerDialog.this.memoryValues[4]);
								break;
							case 5: //NiMH
							case 6: //NiCd
								ChargerDialog.this.selectedProgramMemory.setNiModeC((byte) ChargerDialog.this.memoryValues[4]);
								break;
							case 8: //Pb
								ChargerDialog.this.selectedProgramMemory.setPbModeC((byte) ChargerDialog.this.memoryValues[4]);
								break;
							default: //Power
							}
						}
						break;
					case 5: // charge parameter balancer Li Setup
						ChargerDialog.this.selectedProgramMemory.setLiBalEndMode((byte) ChargerDialog.this.memoryValues[5]); //end current OFF, detect balancer ON
						break;
					case 6: // charge parameter end current
						ChargerDialog.this.selectedProgramMemory.setEndCharge((short) ChargerDialog.this.memoryValues[6]);
						break;
					case 7: // charge parameter cell voltage
						if (ChargerDialog.this.isDuo) {
							switch (ChargerDialog.this.selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,NiMH,Nicd,Pb,NiZn,LiHV
							case 0: //LiPo
								ChargerDialog.this.selectedProgramMemory.setLiPoChgCellVolt((short) ChargerDialog.this.memoryValues[7]);
								break;
							case 1: //LiIo
								ChargerDialog.this.selectedProgramMemory.setLiIoChgCellVolt((short) ChargerDialog.this.memoryValues[7]);
								break;
							case 2: //LiFe
								ChargerDialog.this.selectedProgramMemory.setLiFeChgCellVolt((short) ChargerDialog.this.memoryValues[7]);
								break;
							case 6: //NiZn
								ChargerDialog.this.selectedProgramMemory.setNiZnChgCellVolt((short) ChargerDialog.this.memoryValues[7]);
								break;
							case 5: //Pb
								ChargerDialog.this.selectedProgramMemory.setPbChgCellVolt((short) ChargerDialog.this.memoryValues[7]);
								break;
							case 7: //LiHV
							case 3: //NiMH
							case 4: //NiCd
							default:
								break;
							}
						}
						else {
							switch (ChargerDialog.this.selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,LiHV,LTO, NiMH,Nicd,NiZn,Pb
							case 0: //LiPo
								ChargerDialog.this.selectedProgramMemory.setLiPoChgCellVolt((short) ChargerDialog.this.memoryValues[7]);
								break;
							case 1: //LiIo
								ChargerDialog.this.selectedProgramMemory.setLiIoChgCellVolt((short) ChargerDialog.this.memoryValues[7]);
								break;
							case 2: //LiFe
								ChargerDialog.this.selectedProgramMemory.setLiFeChgCellVolt((short) ChargerDialog.this.memoryValues[7]);
								break;
							case 7: //NiZn
								ChargerDialog.this.selectedProgramMemory.setNiZnChgCellVolt((short) ChargerDialog.this.memoryValues[7]);
								break;
							case 8: //Pb
								ChargerDialog.this.selectedProgramMemory.setPbChgCellVolt((short) ChargerDialog.this.memoryValues[7]);
								break;
							case 3: //LiHV
							case 4: //LTO
							case 5: //NiMH
							case 6: //NiCd
							default:
							}
						}
						break;
					case 8: // charge cut temperature
						ChargerDialog.this.selectedProgramMemory.setSafetyTempC((short) (ChargerDialog.this.memoryValues[8] * 10));
						break;
					case 9: // charge max charge
						ChargerDialog.this.selectedProgramMemory.setSafetyCapC((short) ChargerDialog.this.memoryValues[9]);
						break;
					case 10: // charge parameter balancer difference
						ChargerDialog.this.selectedProgramMemory.setSafetyTimeC((short) ChargerDialog.this.memoryValues[10]);
						break;
					case 11: // charge parameter balancer
						ChargerDialog.this.selectedProgramMemory.setBalSpeed((byte) ChargerDialog.this.memoryValues[11]);
						break;
					case 12: // charge parameter balancer start
						ChargerDialog.this.selectedProgramMemory.setBalStartMode((byte) ChargerDialog.this.memoryValues[12]);
						break;
					case 13: // charge parameter balancer difference
						ChargerDialog.this.selectedProgramMemory.setBalDiff((byte) ChargerDialog.this.memoryValues[13]);
						break;
					case 14: // charge parameter balancer target
						ChargerDialog.this.selectedProgramMemory.setBalSetPoint((byte) ChargerDialog.this.memoryValues[14]);
						break;
					case 15: // charge parameter balancer over charge
						ChargerDialog.this.selectedProgramMemory.setBalOverPoint((byte) ChargerDialog.this.memoryValues[15]);
						break;
					case 16: // charge parameter balancer delay
						ChargerDialog.this.selectedProgramMemory.setBalDelay((byte) ChargerDialog.this.memoryValues[16]);
						break;
					//discharge begin
					case 17: // discharge parameter current
						ChargerDialog.this.selectedProgramMemory.setDischargeCurrent((short) (ChargerDialog.this.memoryValues[17]));
						break;
					case 19: // discharge end current
						ChargerDialog.this.selectedProgramMemory.setEndDischarge((short) ChargerDialog.this.memoryValues[19]);
						break;
					case 20: // regenerative mode
						ChargerDialog.this.selectedProgramMemory.setRegDchgMode((short) ChargerDialog.this.memoryValues[20]);
						if (ChargerDialog.this.regToInputWarningLable != null) {
							boolean isToInput = ChargerDialog.this.memoryValues[20] == 1;
							ChargerDialog.this.regToInputWarningLable.setEnabled(isToInput);
							ChargerDialog.this.regToInputWarningLable.setForeground(isToInput ? SWTResourceManager.getColor(SWT.COLOR_RED) : SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
						}
						if (ChargerDialog.this.regToChannelSettings != null) {
							boolean isToChannel = ChargerDialog.this.memoryValues[20] == 2;
							ChargerDialog.this.regToChannelSettings.setEnabled(isToChannel);
							ChargerDialog.this.memoryParameters[50].setEnabled(isToChannel);
							boolean isResOrBulb = ChargerDialog.this.memoryValues[50] == 0;
							ChargerDialog.this.memoryParameters[51].setEnabled(isToChannel && isResOrBulb);
							ChargerDialog.this.memoryParameters[52].setEnabled(isToChannel && isResOrBulb);
						}
						break;
					case 18: // discharge parameter cell voltage
					case 21: // discharge extra
					case 22: // discharge balancer
						if (ChargerDialog.this.isDuo) {
							switch (ChargerDialog.this.selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,NiMH,Nicd,Pb,NiZn,LiHV
							case 0: //LiPo
								ChargerDialog.this.selectedProgramMemory.setLiPoDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 1: //LiIo
								ChargerDialog.this.selectedProgramMemory.setLiIoDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 2: //LiFe
								ChargerDialog.this.selectedProgramMemory.setLiFeDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 3: //NiMH
							case 4: //NiCd
								ChargerDialog.this.selectedProgramMemory.setNiDischargeVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setNiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 5: //Pb
								ChargerDialog.this.selectedProgramMemory.setPbDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setPbModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 6: //NiZn
								ChargerDialog.this.selectedProgramMemory.setNiZnDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 7: //LiHV
								ChargerDialog.this.selectedProgramMemory.setLiHVDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							default:
								break;
							}
						}
						else {
							switch (ChargerDialog.this.selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,LiHV,LTO, NiMH,Nicd,NiZn,Pb
							case 0: //LiPo
								ChargerDialog.this.selectedProgramMemory.setLiPoDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 1: //LiIo
								ChargerDialog.this.selectedProgramMemory.setLiIoDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 2: //LiFe
								ChargerDialog.this.selectedProgramMemory.setLiFeDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 3: //LiHV
								ChargerDialog.this.selectedProgramMemory.setLiHVDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 4: //LTO
								ChargerDialog.this.selectedProgramMemory.setLtoDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 5: //NiMH
							case 6: //NiCd
								ChargerDialog.this.selectedProgramMemory.setNiDischargeVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setNiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 7: //NiZn
								ChargerDialog.this.selectedProgramMemory.setNiZnDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 8: //Pb
								ChargerDialog.this.selectedProgramMemory.setPbDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								ChargerDialog.this.selectedProgramMemory.setPbModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							default:
								break;
							}
						}
						break;
					case 23: // discharge parameter cut temperature
						ChargerDialog.this.selectedProgramMemory.setSafetyTempD((short) (ChargerDialog.this.memoryValues[23] * 10));
						break;
					case 24: // discharge parameter max discharge capacity
						ChargerDialog.this.selectedProgramMemory.setSafetyCapD((short) ChargerDialog.this.memoryValues[24]);
						break;
					case 25: // discharge parameter safety timer
						ChargerDialog.this.selectedProgramMemory.setSafetyTimeD((short) ChargerDialog.this.memoryValues[25]);
						break;
					case 26: // Ni charge voltage drop
						ChargerDialog.this.selectedProgramMemory.setNiPeak((short) ChargerDialog.this.memoryValues[26]);
						break;
					case 27: // Ni charge voltage drop delay
						ChargerDialog.this.selectedProgramMemory.setNiPeakDelay((short) ChargerDialog.this.memoryValues[27]);
						break;
					case 28: // Ni charge allow 0V
						ChargerDialog.this.selectedProgramMemory.setNiZeroEnable((short) ChargerDialog.this.memoryValues[28]);
						break;
					case 29: // Ni trickle charge enable
						ChargerDialog.this.selectedProgramMemory.setNiTrickleEnable((short) ChargerDialog.this.memoryValues[29]);
						break;
					case 30: // Ni charge trickle current
						ChargerDialog.this.selectedProgramMemory.setNiTrickleCurrent((short) ChargerDialog.this.memoryValues[30]);
						break;
					case 31: // Ni charge trickle timeout
						ChargerDialog.this.selectedProgramMemory.setNiTrickleTime((short) ChargerDialog.this.memoryValues[31]);
						break;

					case 32: // charge restore lowest voltage
						ChargerDialog.this.selectedProgramMemory.setRestoreVolt((short) ChargerDialog.this.memoryValues[32]);
						break;
					case 33: // charge restore charge time
						ChargerDialog.this.selectedProgramMemory.setRestoreTime((short) ChargerDialog.this.memoryValues[33]);
						break;
					case 34: // charge restore charge current
						ChargerDialog.this.selectedProgramMemory.setRestoreCurent((short) ChargerDialog.this.memoryValues[34]);
						break;
					case 35: // charge keep charging after done
						ChargerDialog.this.selectedProgramMemory.setKeepChargeEnable((byte) ChargerDialog.this.memoryValues[35]);
						break;

					case 36: // storage parameter cell voltage (Li battery type only)
						if (ChargerDialog.this.isDuo) {
							switch (ChargerDialog.this.selectedProgramMemory.getType()) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
							case 0: //LiPo
								ChargerDialog.this.selectedProgramMemory.setLiPoStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 1: //LiIo
								ChargerDialog.this.selectedProgramMemory.setLiLoStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 2: //LiFe
								ChargerDialog.this.selectedProgramMemory.setLiFeStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 7: //LiHV
								ChargerDialog.this.selectedProgramMemory.setLiHVStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 3: //NiMH
							case 4: //NiCd
							case 5: //Pb
							case 6: //NiZn
							default:
								break;
							}
						}
						else {
							switch (ChargerDialog.this.selectedProgramMemory.getType()) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
							case 0: //LiPo
								ChargerDialog.this.selectedProgramMemory.setLiPoStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 1: //LiIo
								ChargerDialog.this.selectedProgramMemory.setLiLoStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 2: //LiFe
								ChargerDialog.this.selectedProgramMemory.setLiFeStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 3: //LiHV
								ChargerDialog.this.selectedProgramMemory.setLiHVStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 4: //LTO
								ChargerDialog.this.selectedProgramMemory.setLtoStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 5: //NiMH
							case 6: //NiCd
							case 7: //NiZn
							case 8: //Pb
							default:
								break;
							}
						}
						break;
					case 37: // storage parameter compensation
						ChargerDialog.this.selectedProgramMemory.setStoCompensation((short) ChargerDialog.this.memoryValues[37]);
						break;
					case 38: // storage acceleration
						ChargerDialog.this.selectedProgramMemory.setFastSto((byte) ChargerDialog.this.memoryValues[38]);
						break;

					case 39: // cycle parameter mode CHG->DCHG
						ChargerDialog.this.selectedProgramMemory.setCycleMode((byte) ChargerDialog.this.memoryValues[39]);
						break;
					case 40: // cycle count
						ChargerDialog.this.selectedProgramMemory.setCycleCount((short) ChargerDialog.this.memoryValues[40]);
						break;
					case 41: // cycle delay
						ChargerDialog.this.selectedProgramMemory.setCycleDelay((short) ChargerDialog.this.memoryValues[41]);
						break;

					case 42: // power voltage
						ChargerDialog.this.selectedProgramMemory.setDigitPowerVolt((short) (ChargerDialog.this.memoryValues[42] * 100));
						break;
					case 43: // power current
						ChargerDialog.this.selectedProgramMemory.setDigitPowerCurrent((short) (ChargerDialog.this.memoryValues[43] * 10));
						break;
					case 44: // power option lock
					case 45: // power option auto start
					case 46: // power option live update
						ChargerDialog.this.selectedProgramMemory.setDigitPowerSet((byte) (ChargerDialog.this.memoryValues[44] 
								+ (ChargerDialog.this.memoryValues[45] << 1) + (ChargerDialog.this.memoryValues[46] << 2)));
						break;

					case 47: // channel mode asynchronous | synchronous DUO only
						ChargerDialog.this.selectedProgramMemory.setChannelMode((byte) ChargerDialog.this.memoryValues[47]);
						log.log(Level.OFF, "channel async/sync = " + ChargerDialog.this.memoryValues[47]);
						break;
					case 48: // log interval
						ChargerDialog.this.selectedProgramMemory.setLogInterval((short) (ChargerDialog.this.memoryValues[48] - ChargerDialog.this.memoryValues[48]%5));
						break;
					case 49: // power option auto start
						ChargerDialog.this.selectedProgramMemory.setSaveToSD((byte) ChargerDialog.this.memoryValues[49]);
						break;
						
					case 50: //regenerative channel mode
						ChargerDialog.this.selectedProgramMemory.setRegChMode((byte) ChargerDialog.this.memoryValues[50]);
						if (ChargerDialog.this.memoryParameters[51] != null && ChargerDialog.this.memoryParameters[52] != null) {
							ChargerDialog.this.memoryParameters[51].setEnabled(ChargerDialog.this.memoryValues[50] == 0);
							ChargerDialog.this.memoryParameters[52].setEnabled(ChargerDialog.this.memoryValues[50] == 0);
						}
						break;
					case 51: //regenerative to channel voltage limit
						ChargerDialog.this.selectedProgramMemory.setRegChVolt((short) (ChargerDialog.this.memoryValues[51] * 100));
						break;
					case 52: //regenerative to channel current limit
						ChargerDialog.this.selectedProgramMemory.setRegChCurrent((short) (ChargerDialog.this.memoryValues[52]));
						break;

					}
					ChargerDialog.this.memoryValues = ChargerDialog.this.selectedProgramMemory.getMemoryValues(ChargerDialog.this.memoryValues, ChargerDialog.this.isDuo);
					updateMemoryParameterControls();
				}
			}
		};
	}
	

	private Listener addSystemValuesChangedListener() {
		return new Listener() {
			@Override
			public void handleEvent(Event evt) {
				btnSystemSave.setEnabled(true);
				switch (evt.index) {
				case 0: //temperature unit
					systemSettings.setTempUnit((short) systemValues[0]);
					break;
				case 1: //shut down temperature
					systemSettings.setTempStop((short) systemValues[1]);
					break;
				case 2: //power reduction delta
					systemSettings.setTempReduce((short) systemValues[2]);
					break;
				case 3: //cooling fan on temperature
					systemSettings.setTempFansOn((short) systemValues[3]);
					break;
				case 4: //cooling fan off delay time
					systemSettings.setFansOffDelay((short) systemValues[4]);
					break;
					
				case 5://5 Beep volume buttons
				case 6: //6 Beep volume tip
				case 7://7 Beep volume alarm
				case 8://8 Beep volume end
					short[] beepEnable = new short[4];
					beepEnable[0] = (short) (systemValues[5] == 0 ? 0 : 1);
					beepEnable[1] = (short) (systemValues[6] == 0 ? 0 : 1);
					beepEnable[2] = (short) (systemValues[7] == 0 ? 0 : 1);
					beepEnable[3] = (short) (systemValues[8] == 0 ? 0 : 1);
					systemSettings.setBeepEnable(beepEnable);
					short[] beepVolume = new short[4];
					beepVolume[0] = (short) (systemValues[5] == 0 ? systemSettings.getBeepVOL()[0] : systemValues[5]);
					beepVolume[1] = (short) (systemValues[6] == 0 ? systemSettings.getBeepVOL()[1] : systemValues[6]);
					beepVolume[2] = (short) (systemValues[7] == 0 ? systemSettings.getBeepVOL()[2] : systemValues[7]);
					beepVolume[3] = (short) (systemValues[8] == 0 ? systemSettings.getBeepVOL()[3] : systemValues[8]);
					systemSettings.setBeepVOL(beepVolume);
				case 9: //9 Beep type (only modifying beepType[3])
					short[] beepType = new short[4];
					beepType[3] = (short) systemValues[9];
					systemSettings.setBeepType(beepType); 

				case 10: //LCD brightness
				case 11: //LCD contrast
					systemSettings.setLightValue((short) systemValues[10]);
					systemSettings.setLcdContraste((short) systemValues[11]);
					break;
					
					//DUO only section
				case 12://12 Charge power channel 1
				case 13://13 Charge power channel 2
					systemSettings.setChargePower(new short[] {(short) systemValues[12], (short) systemValues[13]});
					break;
				case 14://14 Charge power channel 1
				case 15://15 Charge power channel 2
					systemSettings.setDischargePower(new short[] {(short) systemValues[14], (short) systemValues[15]});
					break;
				case 16:
					systemSettings.setProPower((short) systemValues[16]); //16 Power distribution
					break;
					
				case 17: //16 Power distribution, DUO 0=DC, 1=Bat, X 0-3
					systemSettings.setSelInputSource((short) systemValues[17]); //17 source input select
					break;

					//DX/X/S only section
				case 18: //18 discharge power Limit
					systemSettings.setxDischargePower((short) systemValues[18]); 
					break;
				case 19: //19 inputLowVolt
					systemSettings.xInputSources[systemValues[17]].setInputLowVolt((short) systemValues[19]);
					break;
				case 20: //20 inputCurrentLimit
					systemSettings.xInputSources[systemValues[17]].setInputCurrentLimit((short) systemValues[20]);
					break;
				case 21: //21 Charge power 
					systemSettings.xInputSources[systemValues[17]].setChargePower((short) systemValues[21]);
					break;
				case 22: //22 regEnable
					systemSettings.xInputSources[systemValues[17]].setRegEnable((short) systemValues[22]);
					if (grpRegInputPowerLimits != null)
						grpRegInputPowerLimits.setEnabled(systemValues[22] == 1);
					break;
				case 23: //23 regVoltLimit
					systemSettings.xInputSources[systemValues[17]].setRegVoltLimit((short) systemValues[23]);
					break;
				case 24: //24 regCurrentLimit
					systemSettings.xInputSources[systemValues[17]].setRegCurrentLimit((short) systemValues[24]);
					break;
				case 25: //25 regPowerLimit
					systemSettings.xInputSources[systemValues[17]].setRegPowerLimit((short) systemValues[25]);
					break;
				case 26: //26 regCapLimit
					systemSettings.xInputSources[systemValues[17]].setRegCapLimit(systemValues[26]);
					break;
				case 27: //language 0=en 1=de 2=ch
					systemSettings.setSelectLanguage((short) systemValues[27]);
					break;
					
				case 28: 	//28 duo dcInputLowVolt 
					systemSettings.setDcInputLowVolt((short) systemValues[28]);
					break;
				case 29: 	//29 duo dcInputCurrentLimit 
					systemSettings.setDcInputCurrentLimit((short) systemValues[29]);
					break;
				case 30: 	//30 duo batInputLowVolt
					systemSettings.setBatInputLowVolt((short) systemValues[30]);
					break;
				case 31: 	//31 duo batInputCurrentLimit 
					systemSettings.setBatInputCurrentLimit((short) systemValues[31]);
					break;
				case 32:	//32 duo regEnable
					systemSettings.setRegEnable((short) systemValues[32]);
					if (grpDuoRegInputPowerLimits != null)
						grpDuoRegInputPowerLimits.setEnabled(systemValues[32] == 1);
					break;
				case 33:	//33 duo regVoltLimit
					systemSettings.setRegVoltLimit((short) systemValues[33]);
					break;
				case 34:	//34 duo regCurrentLimit
					systemSettings.setRegCurrentLimit((short) systemValues[34]);
					break;
				case 35:	//35 duo regCapLimit
					systemSettings.setRegCapacityLimit(systemValues[35] * 100);
					break;
				case 36:	//36 DX antiSparkEnable
					systemSettings.setAntiSparkEnabl((short) systemValues[36]);
					break;
				default:
					break;
				}
				systemValues = ChargerDialog.this.systemSettings.getSystemValues(ChargerDialog.this.systemValues);
				updateSystemParameterControls();
			}
		};
	}


	private static void initLogger() {
		ChargerDialog.logHandler = new ConsoleHandler();
		ChargerDialog.logHandler.setFormatter(new LogFormatter());
		ChargerDialog.logHandler.setLevel(Level.INFO);
		ChargerDialog.rootLogger = Logger.getLogger(GDE.STRING_EMPTY);
		// clean up all handlers from outside
		Handler[] handlers = ChargerDialog.rootLogger.getHandlers();
		for (Handler handler : handlers) {
			ChargerDialog.rootLogger.removeHandler(handler);
		}
		ChargerDialog.rootLogger.setLevel(Level.ALL);
		ChargerDialog.rootLogger.addHandler(ChargerDialog.logHandler);
	}

	/**
	 * read the program memory structure and initialize build in program memory 0 LiPo
	 * @return string array to fill drop down box
	 */
	private String[] readProgramMemories() {
		List<String> programMemories = new ArrayList<String>();
		try {
			if (this.usbPort != null && this.usbPort.isConnected()) {
				//Read memory structure of original and added/modified program memories
				short sizeMemHead = (short) ((ChargerMemoryHead.getSize(this.isDuo || this.isDx) + 1) / 2);
				byte[] memHeadBuffer = new byte[sizeMemHead * 2];
				this.usbPort.masterRead((byte) 0, ChargerDialog.REG_HOLDING_MEM_HEAD_START, sizeMemHead, memHeadBuffer);
				ChargerMemoryHead memHead = new ChargerMemoryHead(memHeadBuffer, this.isDuo || this.isDx);
				if (ChargerDialog.log.isLoggable(Level.INFO)) 
					ChargerDialog.log.log(Level.INFO, memHead.toString());

				int i = 0;
				byte[] index = new byte[2];
				short sizeMemory = (short) (((this.systemInfo != null && this.systemInfo.getDeviceID() != 0 ? this.systemInfo.getProgramMemoryLength() : ChargerMemory.getSize(this.isDuo)) + 1) / 2);
				byte[] memoryBuffer;
				this.memoryHeadIndex = memHead.getIndex();

				for (; i < memHead.getCount(); ++i) {
					//Read charger program memory after writing selected index
					//index[0] = (byte) i; //order number
					index[0] = this.memoryHeadIndex[i]; //order logical
					if (ChargerDialog.log.isLoggable(Level.INFO))
						ChargerDialog.log.log(Level.INFO, String.format("select mem index %d", DataParser.parse2Short(index[0], index[1]))); //$NON-NLS-1$
					this.usbPort.masterWrite(Register.REG_SEL_MEM.value, (short) 1, index);

					//Read selected charger program memory
					memoryBuffer = new byte[sizeMemory * 2];
					this.usbPort.masterRead((byte) 0, ChargerDialog.REG_HOLDING_MEM_START, sizeMemory, memoryBuffer);
					programMemories.add(getFormatedListEntry(i, new ChargerMemory(memoryBuffer, this.isDuo))); //order logical //$NON-NLS-1$
					if (ChargerDialog.log.isLoggable(Level.INFO)) 
						ChargerDialog.log.log(Level.INFO, programMemories.get(programMemories.size() - 1));
				}
				initProgramMemory(0);
				return programMemories.size() == 0 ? new String[0] : programMemories.toArray(new String[1]);
			}
		}
		catch (IllegalStateException e) {
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialogAsync(ChargerDialog.this.getDialogShell(), e.getMessage());
		}
		catch (TimeOutException te) {
			if(!this.isRetryReadMem) {
				ChargerDialog.log.log(Level.WARNING, te.getMessage(), te);
				WaitTimer.delay(1000);
				this.isRetryReadMem = true;
				return readProgramMemories();
			}
			ChargerDialog.log.log(Level.SEVERE, te.getMessage(), te);
			this.application.openMessageDialogAsync(ChargerDialog.this.getDialogShell(), te.getMessage());
		}
		catch (RuntimeException rte) {
			ChargerDialog.log.log(Level.SEVERE, rte.getMessage(), rte);
		}
		return new String[0];
	}

	/**
	 * @param memHeadIndex
	 * @param programMemory
	 * @return a formated string for program memory
	 */
	private String getFormatedListEntry(final int memHeadIndex, final ChargerMemory programMemory) {
		return String.format("%02d - %s", this.memoryHeadIndex[memHeadIndex], programMemory.getUseFlagAndName(isDuo));
	}

	/**
	 * initialize program memory with the given index and set selectedProgram memory local variable
	 * @param selectedProgramMemoryIndex the index of the selected program memory according memory structure
	 * @return byte array containing the program memory
	 */
	private byte[] initProgramMemory(int selectedProgramMemoryIndex) {
		short sizeMemory = (short) (((this.systemInfo != null && this.systemInfo.getDeviceID() != 0 ? this.systemInfo.getProgramMemoryLength() : ChargerMemory.getSize(this.isDuo)) + 1) / 2);
		byte[] memoryBuffer = new byte[sizeMemory * 2];
		if (ChargerDialog.log.isLoggable(Level.FINER)) ChargerDialog.log.log(Level.FINER, "read using memory buffer length " + memoryBuffer.length); //$NON-NLS-1$
		byte[] index = new byte[2];
		index[0] = (byte) (selectedProgramMemoryIndex & 0xFF);

		try {
			this.usbPort.masterWrite(Register.REG_SEL_MEM.value, (short) 1, index);
			
			this.usbPort.masterRead((byte) 0, ChargerDialog.REG_HOLDING_MEM_START, sizeMemory, memoryBuffer);
			this.selectedProgramMemory = new ChargerMemory(memoryBuffer, this.isDuo);
			if (ChargerDialog.log.isLoggable(Level.INFO)) ChargerDialog.log.log(Level.INFO, "selectedProgramMemory = " + this.selectedProgramMemory.toString(this.isDuo));
		}
		catch (IllegalStateException | TimeOutException e) {
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (RuntimeException rte) {
			ChargerDialog.log.log(Level.SEVERE, rte.getMessage(), rte);
		}
		return memoryBuffer;
	}

	/**
	 * write modified program memory to selected program memory index of memory head structure
	 * this method will update the useFlag to 0x55AA to differentiate to build in memories
	 * @param selectedProgramMemoryIndex the index of the selected program memory according memory head structure
	 * @param modifiedProgramMemory the modified program memory class
	 * @param useFlag 0x0000 for BUILD IN or 0x55aa for CUSTOM
	 */
	private void writeProgramMemory(final int selectedProgramMemoryIndex, ChargerMemory modifiedProgramMemory, short useFlag) {
		short sizeMemory = (short) (((this.systemInfo != null && this.systemInfo.getDeviceID() != 0 ? this.systemInfo.getProgramMemoryLength() : ChargerMemory.getSize(this.isDuo)) + 1) / 2);
		byte[] index = new byte[2];
		index[0] = (byte) (selectedProgramMemoryIndex & 0xFF);

		if ((this.isDuo && selectedProgramMemoryIndex < 7) || (!this.isDuo && selectedProgramMemoryIndex < 10)) {
			ChargerDialog.log.log(Level.SEVERE, String.format(Messages.getString(MessageIds.GDE_MSGT2621), selectedProgramMemoryIndex));
			this.application.openMessageDialog(this.dialogShell, Messages.getString(MessageIds.GDE_MSGT2622));
			return;
		}

		try {
			this.usbPort.masterWrite(Register.REG_SEL_MEM.value, (short) 1, index);

			if (modifiedProgramMemory.getUseFlag() != useFlag) //replace BUILD_IN to CUSTOM
				modifiedProgramMemory.setUseFlag(useFlag);
			if (ChargerDialog.log.isLoggable(Level.INFO)) {
				ChargerDialog.log.log(Level.INFO, String.format("Program memory name = %s", new String(modifiedProgramMemory.getName()).trim())); //$NON-NLS-1$
				ChargerDialog.log.log(Level.INFO, String.format("Program memory useFlag = 0x%04X", modifiedProgramMemory.getUseFlag())); //$NON-NLS-1$
				ChargerDialog.log.log(Level.INFO, String.format("write memory buffer index  = %d", selectedProgramMemoryIndex)); //$NON-NLS-1$
			}
			this.usbPort.masterWrite(ChargerDialog.REG_HOLDING_MEM_START, sizeMemory, modifiedProgramMemory.getAsByteArray(this.isDuo));

			transOrder((byte) Order.ORDER_WRITE_MEM.ordinal());
		}
		catch (IllegalStateException | TimeOutException e) {
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (RuntimeException rte) {
			ChargerDialog.log.log(Level.SEVERE, rte.getMessage(), rte);
		}
	}

	/**
	 * write modified system memory to device
	 * this method will update the useFlag to 0x55AA to differentiate to build in memories
	 * @param modifiedSystemMemory the modified program memory class
	 */
	private boolean writeSystemMemory(boolean isDuo) {
		short sizeSystem = (short) (((systemInfo != null ? systemInfo.getSystemMemoryLength() : ChargerSystem.getSize(isDuo, isDx)) + 1) / 2);

		try {
			if (systemSettings != null) {
				if (ChargerDialog.log.isLoggable(Level.FINE)) ChargerDialog.log.log(Level.FINE, new ChargerSystem(systemSettings.getAsByteArray(isDuo, isDx), isDuo, isDx).toString(isDuo, isDx));
				//MasterWrite(REG_HOLDING_SYS_START,(sizeof(SYSTEM)+1)/2,(BYTE *)&System)
				this.usbPort.masterWrite(REG_HOLDING_SYS_START, sizeSystem, systemSettings.getAsByteArray(isDuo, isDx));
				//TransOrder(ORDER_WRITE_SYS)
				transOrder((byte) Order.ORDER_WRITE_SYS.ordinal());
			}
		}
		catch (IllegalStateException | TimeOutException e) {
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (RuntimeException rte) {
			ChargerDialog.log.log(Level.SEVERE, rte.getMessage(), rte);
		}
		return true;
	}

	/**
	 * add an entry to memory head structure
	 * @param batTypeName defines the index device dependent where to add a new program memory
	 * @return the selected program memory index of the memory head structure
	 */
	private short addEntryMemoryHead(String batTypeName) {
		short newHeadIndex = -1;
		try {
			if (this.usbPort != null && this.usbPort.isConnected()) {
				//Read memory structure of original and added/modified program memories
				short sizeMemHead = (short) ((ChargerMemoryHead.getSize(this.isDuo || this.isDx) + 1) / 2);
				byte[] memHeadBuffer = new byte[sizeMemHead * 2];
				this.usbPort.masterRead((byte) 0, ChargerDialog.REG_HOLDING_MEM_HEAD_START, sizeMemHead, memHeadBuffer);
				ChargerMemoryHead memHead = new ChargerMemoryHead(memHeadBuffer, this.isDuo || this.isDx);
				if (ChargerDialog.log.isLoggable(Level.INFO)) ChargerDialog.log.log(Level.INFO, String.format("before modification: %s", memHead.toString())); //$NON-NLS-1$
				
				if (memHead.getCount() >= ChargerMemoryHead.getMaxListIndex(this.isDuo || this.isDx))
					throw new IndexOutOfBoundsException();

				newHeadIndex = memHead.getNextFreeIndex();
				byte[] updatedIndex = memHead.addIndexAfter(((byte) (((iChargerUsb) this.device).getBatTypeIndex(batTypeName) - 1)), newHeadIndex);
				memHead.setIndex(updatedIndex);
				//0, 17, 16, 15, 14, 13, 12, 10, 1, 2, 11, 3, 4, 5, 6, 7, 8, 9,
				memHead.setCount((short) (memHead.getCount() + 1));
				if (ChargerDialog.log.isLoggable(Level.INFO)) ChargerDialog.log.log(Level.INFO, String.format("after modification: %s", memHead.toString())); //$NON-NLS-1$

				//write the updated memory head structure to device
				this.usbPort.masterWrite(ChargerDialog.REG_HOLDING_MEM_HEAD_START, sizeMemHead, memHead.getAsByteArray(this.isDuo || this.isDx));
				transOrder((byte) Order.ORDER_WRITE_MEM_HEAD.ordinal());
			}
		}
		catch (IllegalStateException | TimeOutException e) {
			if (e instanceof UsbException) {
				this.application.openMessageDialogAsync(ChargerDialog.this.getDialogShell(), e.getMessage());
			}
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (IndexOutOfBoundsException ex) {
			ChargerDialog.log.log(Level.SEVERE, ex.getMessage(), ex);
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE2601));
		}
		return newHeadIndex;
	}

	/**
	 * remove given index from program memory head structure
	 * @param removeProgramMemoryIndex the index of the selected program memory according memory structure
	 */
	private void removeMemoryHead(byte removeProgramMemoryIndex) {
		try {
			if (this.usbPort != null && this.usbPort.isConnected()) {
				//Read memory structure of original and added/modified program memories
				short sizeMemHead = (short) ((ChargerMemoryHead.getSize(this.isDuo || this.isDx) + 1) / 2);
				byte[] memHeadBuffer = new byte[sizeMemHead * 2];
				this.usbPort.masterRead((byte) 0, ChargerDialog.REG_HOLDING_MEM_HEAD_START, sizeMemHead, memHeadBuffer);
				ChargerMemoryHead memHead = new ChargerMemoryHead(memHeadBuffer, this.isDuo || this.isDx);
				if (ChargerDialog.log.isLoggable(Level.INFO)) ChargerDialog.log.log(Level.INFO, String.format("before modification: count %d; index.length %d", memHead.getCount(), memHead.getIndex().length)); //$NON-NLS-1$
				if (ChargerDialog.log.isLoggable(Level.INFO)) ChargerDialog.log.log(Level.INFO, String.format("before modification: %s", memHead.toString())); //$NON-NLS-1$

				memHead.setIndex(memHead.removeIndex(removeProgramMemoryIndex));
				memHead.setCount((short) (memHead.getCount() - 1));
				if (ChargerDialog.log.isLoggable(Level.INFO)) ChargerDialog.log.log(Level.INFO, String.format("after modification: count %d; index.length %d", memHead.getCount(), memHead.getIndex().length)); //$NON-NLS-1$
				if (ChargerDialog.log.isLoggable(Level.INFO)) ChargerDialog.log.log(Level.INFO, String.format("after modification: %s", memHead.toString())); //$NON-NLS-1$

				//write the updated memory head structure to device
				this.usbPort.masterWrite(ChargerDialog.REG_HOLDING_MEM_HEAD_START, sizeMemHead, memHead.getAsByteArray(this.isDuo || this.isDx));
				transOrder((byte) Order.ORDER_WRITE_MEM_HEAD.ordinal());
			}
		}
		catch (IllegalStateException | TimeOutException ex) {
			if (ex instanceof UsbException) {
				this.application.openMessageDialogAsync(ChargerDialog.this.getDialogShell(), ex.getMessage());
			}
			ChargerDialog.log.log(Level.SEVERE, ex.getMessage(), ex);
		}
		catch (RuntimeException rte) {
			ChargerDialog.log.log(Level.SEVERE, rte.getMessage(), rte);
		}
	}

	/**
	 * confirm the previous written data
	 * @param order order key
	 * @throws IllegalStateException
	 * @throws TimeOutException
	 */
	private void transOrder(byte order) throws IllegalStateException, TimeOutException {
		byte[] temp = new byte[4];
		temp[0] = (byte) (ChargerDialog.VALUE_ORDER_KEY & 0xFF);
		temp[1] = (byte) (ChargerDialog.VALUE_ORDER_KEY >> 8);
		temp[2] = order;
		ModBusErrorCode mbErrorCode = this.usbPort.masterWrite(Register.REG_ORDER_KEY.value, (short) (temp.length / 2), temp);
		if (mbErrorCode !=  ModBusErrorCode.MB_EOK)
			throw new IllegalStateException(mbErrorCode.toString());

		temp = new byte[2];
		mbErrorCode = this.usbPort.masterWrite(Register.REG_ORDER_KEY.value, (short) (temp.length / 2), temp);
		if (mbErrorCode !=  ModBusErrorCode.MB_EOK)
			throw new IllegalStateException(mbErrorCode.toString());
	}

	/**
	 * start a program execution at device
	 * @param programType type of program to execute enum {charge, storage, discharge, cycle, balance}
	 * @param channel number of channel enum {channel1, channel2}
	 * @param programMemoryIndex the index of the selected program memory according memory structure
	 */
	private void startProgramExecution(byte programType, byte channel, byte programMemoryIndex) {
		try {
			if (this.usbPort != null && this.usbPort.isConnected()) {
				byte[] runOrderBuf = new byte[10];
				runOrderBuf[0] = programType;// enum {charge, storage, discharge, cycle, balance};
				runOrderBuf[2] = programMemoryIndex;
				runOrderBuf[4] = channel;
				runOrderBuf[6] = (byte) (ChargerDialog.VALUE_ORDER_KEY & 0xFF);
				runOrderBuf[7] = (byte) (ChargerDialog.VALUE_ORDER_KEY >> 8);
				runOrderBuf[8] = (byte) Order.ORDER_RUN.ordinal();
				this.usbPort.masterWrite(Register.REG_SEL_OP.value, (short) (runOrderBuf.length / 2), runOrderBuf);
			}
		}
		catch (IllegalStateException | TimeOutException e) {
			if (e instanceof UsbException) {
				this.application.openMessageDialogAsync(ChargerDialog.this.getDialogShell(), e.getMessage());
			}
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (RuntimeException rte) {
			ChargerDialog.log.log(Level.SEVERE, rte.getMessage(), rte);
		}
	}

	/**
	 * stop a program memory execution
	 * @param channel number of channel enum {channel1, channel2}
	 */
	private void stopProgramExecution(byte channel) {
		try {
			boolean isConnectedByStop = false;
			if (this.usbPort != null) {
				if (!this.usbPort.isConnected()) {
					this.usbPort.openUsbPort();
					isConnectedByStop = true;
					WaitTimer.delay(500);
				}
				byte[] runOrderBuf = new byte[6];
				runOrderBuf[0] = channel;
				runOrderBuf[2] = (byte) (ChargerDialog.VALUE_ORDER_KEY & 0xFF);
				runOrderBuf[3] = (byte) (ChargerDialog.VALUE_ORDER_KEY >> 8);
				runOrderBuf[4] = (byte) Order.ORDER_STOP.ordinal();

				this.usbPort.masterWrite(Register.REG_SEL_CHANNEL.value, (short) (runOrderBuf.length / 2), runOrderBuf);

				if (isConnectedByStop) this.usbPort.closeUsbPort(true);
			}
		}
		catch (UsbException | IllegalStateException | TimeOutException e) {
			if (e instanceof UsbException) {
				this.application.openMessageDialogAsync(ChargerDialog.this.getDialogShell(), e.getMessage());
			}
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (RuntimeException rte) {
			ChargerDialog.log.log(Level.SEVERE, rte.getMessage(), rte);
		}
	}

	/**
	 * stop log transmission, log transmission and modbus communication needs synchronization
	 * @param channel number of channel enum {channel1, channel2}
	 */
	private void stopLogTransmission() {
		try {
			boolean isConnectedByStop = false;
			if (this.usbPort != null) {
				if (!this.usbPort.isConnected()) {
					this.usbPort.openUsbPort();
					isConnectedByStop = true;
					WaitTimer.delay(500);
				}
				byte[] temp = new byte[4];
				temp[0] = (byte) (ChargerDialog.VALUE_ORDER_KEY & 0xFF);
				temp[1] = (byte) (ChargerDialog.VALUE_ORDER_KEY >> 8);
				temp[2] = (byte) Order.ORDER_TRANS_LOG_OFF.ordinal();
				if (ModBusErrorCode.MB_EOK != this.usbPort.masterWrite(Register.REG_ORDER_KEY.value, (short) (temp.length / 2), temp))
					throw new IllegalStateException();

				temp = new byte[2];
				if (ModBusErrorCode.MB_EOK != this.usbPort.masterWrite(Register.REG_ORDER_KEY.value, (short) (temp.length / 2), temp))
					throw new IllegalStateException();

				if (isConnectedByStop) this.usbPort.closeUsbPort(true);
			}
		}
		catch (UsbException | IllegalStateException | TimeOutException e) {
			if (e instanceof UsbException) {
				this.application.openMessageDialogAsync(ChargerDialog.this.getDialogShell(), e.getMessage());
			}
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (RuntimeException rte) {
			ChargerDialog.log.log(Level.SEVERE, rte.getMessage(), rte);
		}
	}

	/**
	 * stop log transmission, log transmission and modbus communication needs synchronization
	 */
	public void startLogTransmission() {
		try {
			boolean isConnectedByStop = false;
			if (this.usbPort != null) {
				if (!this.usbPort.isConnected()) {
					this.usbPort.openUsbPort();
					isConnectedByStop = true;
					WaitTimer.delay(500);
				}
				byte[] temp = new byte[4];
				temp[0] = (byte) (ChargerDialog.VALUE_ORDER_KEY & 0xFF);
				temp[1] = (byte) (ChargerDialog.VALUE_ORDER_KEY >> 8);
				temp[2] = (byte) Order.ORDER_TRANS_LOG_ON.ordinal();
				if (ModBusErrorCode.MB_EOK != this.usbPort.masterWrite(Register.REG_ORDER_KEY.value, (short) (temp.length / 2), temp))
						throw new IllegalStateException();

				temp = new byte[2];
				if (ModBusErrorCode.MB_EOK != this.usbPort.masterWrite(Register.REG_ORDER_KEY.value, (short) (temp.length / 2), temp))
						throw new IllegalStateException();

				if (isConnectedByStop) this.usbPort.closeUsbPort(true);
			}
		}
		catch (UsbException | IllegalStateException | TimeOutException e) {
			if (e instanceof UsbException) {
				this.application.openMessageDialogAsync(ChargerDialog.this.getDialogShell(), e.getMessage());
			}
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (RuntimeException rte) {
			ChargerDialog.log.log(Level.SEVERE, rte.getMessage(), rte);
		}
	}

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ChargerDialog(Shell parent, int style) {
		super(parent, style);
		this.device = (iChargerUsb) DataExplorer.getInstance().getActiveDevice();
		this.usbPort = ((iChargerUsb) this.device).getUsbPort();
		setText(this.device.getName());
		this.isDuo = this.device.getName().toLowerCase().endsWith("duo") && !this.device.getName().toLowerCase().contains("45") ? true : false; //$NON-NLS-1$
		this.isDx = device.getName().toLowerCase().contains(" dx") || this.device.getName().toLowerCase().endsWith("duo") && this.device.getName().toLowerCase().contains("45")? true : false; //$NON-NLS-1$
		String[] tmpNamesArray = this.isDuo ? iChargerUsb.BatteryTypesDuo.getValues() : iChargerX6.BatteryTypesX.getValues();
		this.cellTypeNamesArray = new String[tmpNamesArray.length - (this.isDuo ? 2 : 4)];
		System.arraycopy(tmpNamesArray, 1, this.cellTypeNamesArray, 0, this.cellTypeNamesArray.length);
		this.cellTypeNames = String.join(", ", this.cellTypeNamesArray); //$NON-NLS-1$
		this.memoryParameterChangeListener = addProgramMemoryChangedListener();
		this.systemParameterChangeListener = addSystemValuesChangedListener();
		this.memoryHeadIndex = new byte[ChargerMemoryHead.getMaxListIndex(isDuo)];
	}

	/**
	 * Open the dialog.
	 */
	@Override
	public void open() {
		if (SWT.CANCEL == this.application.openOkCancelMessageDialog(Messages.getString(MessageIds.GDE_MSGI2602))) return;
		try {
			if (this.usbPort != null && !this.usbPort.isConnected()) {
				this.usbPort.openUsbPort();
				this.isPortOpenedByDialog = true;
				WaitTimer.delay(500);
			}
			else {
				this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGW2602, new String[] { this.device.getName() }));
				return;
			}
		}
		catch (UsbException e) {
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialogAsync(Messages.getString(gde.messages.MessageIds.GDE_MSGE0051, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
			return;
		}
		try {
			this.usbPort.getData();
		}
		catch (Exception e1) {
			//ChargerDialog.log.log(Level.WARNING, e1.getMessage(), e1);
		}
		try {
			this.usbPort.getData(); //sync logging and modbus query
			WaitTimer.delay(100);
		}
		catch (Exception e) {
			//ChargerDialog.log.log(Level.WARNING, e.getMessage(), e);
		}
		this.stopLogTransmission();
		WaitTimer.delay(100);
		this.systemInfo = this.readInfo();
		if (this.systemInfo == null || this.systemInfo.getDeviceID() == 0) {
			log.log(Level.SEVERE, "Read system info failed");
			this.application.openMessageDialogAsync(ChargerDialog.this.getDialogShell(), Messages.getString(MessageIds.GDE_MSGW2602, new String[] { this.device.getName() }));
			try {
				this.usbPort.closeUsbPort(true);
			}
			catch (UsbException e) {
				ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
			}
			return;
		}
		else if (this.systemInfo != null && (this.systemInfo.getStatus() & 0x02) != 0) {
			log.log(Level.SEVERE, this.systemInfo.getStatusString());
			this.application.openMessageDialogAsync(ChargerDialog.this.getDialogShell(), Messages.getString(MessageIds.GDE_MSGE2603));
			try {
				this.usbPort.closeUsbPort(true);
			}
			catch (UsbException e) {
				ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
			}
			return;
		}
		createContents();
		this.dialogShell.setLocation(300, 50);
		this.dialogShell.open();
		if (((iChargerUsb) this.device).isDataGathererActive()) {
			this.btnCharge.setEnabled(false);
			this.btnStorage.setEnabled(false);
			this.btnDischarge.setEnabled(false);
			this.btnCycle.setEnabled(false);
			this.btnBalance.setEnabled(false);
			this.btnStop.setEnabled(true);
			this.grpProgramMemory.setEnabled(false);
		}
		this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent evt) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
				fadeOutAplhaBlending(evt, getDialogShell().getClientArea(), 10, 10, 10, 15);
			}
			@Override
			public void mouseHover(MouseEvent evt) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
			}
			@Override
			public void mouseExit(MouseEvent evt) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
				fadeInAlpaBlending(evt, getDialogShell().getClientArea(), 5, 5, -5, 5);
			}
		});
		this.dialogShell.layout();
		this.dialogShell.setSize(800, 750);
		Display display = getParent().getDisplay();
		while (!this.dialogShell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		if (!((iChargerUsb) this.device).isDataGathererActive() && this.isPortOpenedByDialog && this.usbPort != null && this.usbPort.isConnected()) {
			try {
				if (!((iChargerUsb) this.device).isDataGathererActive()) {
					this.startLogTransmission();
					this.usbPort.closeUsbPort(true);
					this.isPortOpenedByDialog = false;
				}
			}
			catch (UsbException e) {
				ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
		this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

		if (Settings.getInstance().isDeviceDialogsModal())
			this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		else if (Settings.getInstance().isDeviceDialogsOnTop())
			this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
		else
			this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);		
		this.dialogShell.setSize(800, 750);
		
		SWTResourceManager.registerResourceUser(this.dialogShell);
		this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
		this.dialogShell.setText(getText());
		this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
		if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);
		FillLayout mainTabFolderLayout = new FillLayout();
		mainTabFolderLayout.marginHeight = 5;
		mainTabFolderLayout.marginWidth = 5;
		this.dialogShell.setLayout(mainTabFolderLayout);
		
		CTabFolder mainTabFolder = new CTabFolder(dialogShell, SWT.NONE);
		mainTabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		mainTabFolder.setSimple(false);
		mainTabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (mainTabFolder.getSelectionIndex() == 1) {
					readSystem(isDuo, isDx);
					updateSystemParameterControls();
				}
			}
		});
		
		CTabItem tbtmProgramMemory = new CTabItem(mainTabFolder, SWT.NONE);
		tbtmProgramMemory.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		tbtmProgramMemory.setText(Messages.getString(MessageIds.GDE_MSGI2607)); //$NON-NLS-1$
		Composite mainMemoryComposite = new Composite(mainTabFolder, SWT.NONE);
		tbtmProgramMemory.setControl(mainMemoryComposite);
		mainMemoryComposite.setLayout(new FormLayout());

		this.grpProgramMemory = new Group(mainMemoryComposite, SWT.NONE);
		this.grpProgramMemory.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.grpProgramMemory.setText(Messages.getString(MessageIds.GDE_MSGT2623));
		RowLayout rl_grpMemory = new RowLayout(SWT.HORIZONTAL);
		rl_grpMemory.justify = true;
		rl_grpMemory.fill = GDE.IS_WINDOWS | GDE.IS_OS_ARCH_ARM;
		this.grpProgramMemory.setLayout(rl_grpMemory);
		FormData fd_grpMemory = new FormData();
		fd_grpMemory.top = new FormAttachment(0, 10);
		fd_grpMemory.right = new FormAttachment(100, -10);
		fd_grpMemory.bottom = new FormAttachment(0, 75);
		fd_grpMemory.left = new FormAttachment(0, 10);
		this.grpProgramMemory.setLayoutData(fd_grpMemory);

		this.combo = new CCombo(this.grpProgramMemory, SWT.BORDER);
		this.combo.setLayoutData(new RowData(350, this.comboHeight));
		this.combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
		this.combo.setItems(((iChargerUsb) this.device).isDataGathererActive() ? new String[] { Messages.getString(MessageIds.GDE_MSGT2624) } : this.readProgramMemories());
		this.combo.select(0);
		this.combo.setBackground(this.application.COLOR_WHITE);
		this.combo.setEditable(false);
		this.combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				ChargerDialog.this.combo.setForeground(ChargerDialog.this.application.COLOR_BLACK);
				if (ChargerDialog.this.combo.getSelectionIndex() >= 0 && ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()] >= 0) {
					initProgramMemory(ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()]);
					ChargerDialog.this.lastSelectedComboIndex = ChargerDialog.this.combo.getSelectionIndex();
					ChargerDialog.this.lastSelectedProgramMemoryIndex = ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.lastSelectedComboIndex];
					if (ChargerDialog.this.selectedProgramMemory != null) {
						ChargerDialog.this.memoryValues = ChargerDialog.this.selectedProgramMemory.getMemoryValues(ChargerDialog.this.memoryValues, ChargerDialog.this.isDuo);
						updateMemoryParameterControls();
					}
				}
				if (ChargerDialog.this.combo.getText().contains("BUILD IN")) { //$NON-NLS-1$
					ChargerDialog.this.btnCopy.setEnabled(true);
					ChargerDialog.this.btnDelete.setEnabled(false);
					if (log.isLoggable(Level.FINEST)) 
						ChargerDialog.this.btnDelete.setEnabled(true); //enable deletion of memory created by error
				}
				else {
					ChargerDialog.this.btnCopy.setEnabled(false);
					ChargerDialog.this.btnDelete.setEnabled(true);
				}
				//btnEdit.setEnabled(true); //enable line to fix corrupted memory
				ChargerDialog.this.btnEdit.setEnabled(ChargerDialog.this.combo.getText().contains("CUSTOM")); //$NON-NLS-1$

				if ((ChargerDialog.this.isDuo && ChargerDialog.this.memoryValues[0] < 8) || ChargerDialog.this.memoryValues[0] < 9) { // X devices up to Power
					ChargerDialog.this.btnCharge.setEnabled(true);
					ChargerDialog.this.btnStorage.setEnabled(false);
					ChargerDialog.this.btnDischarge.setEnabled(true);
					ChargerDialog.this.btnCycle.setEnabled(true);
					ChargerDialog.this.btnBalance.setEnabled(false);
					ChargerDialog.this.btnPower.setEnabled(false);
					ChargerDialog.this.btnStop.setEnabled(false);
					if (ChargerDialog.this.memoryValues[0] >= 0 && ChargerDialog.this.cellTypeNamesArray[ChargerDialog.this.memoryValues[0]].startsWith("L")) { //$NON-NLS-1$
						ChargerDialog.this.btnStorage.setEnabled(true);
						ChargerDialog.this.btnBalance.setEnabled(true);
					}
				}
				else if (!ChargerDialog.this.isDuo && ChargerDialog.this.memoryValues[0] == 9) { // X devices power type memory
					ChargerDialog.this.btnCharge.setEnabled(false);
					ChargerDialog.this.btnStorage.setEnabled(false);
					ChargerDialog.this.btnDischarge.setEnabled(false);
					ChargerDialog.this.btnCycle.setEnabled(false);
					ChargerDialog.this.btnBalance.setEnabled(false);
					ChargerDialog.this.btnPower.setEnabled(true);
					ChargerDialog.this.btnStop.setEnabled(false);
				}
			}
		});
		this.combo.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent evt) {
				if (evt.character == SWT.CR) {
					ChargerDialog.this.combo.setEditable(false);
					String changedProgramMemoryText = ChargerDialog.this.combo.getText().trim();
					if (changedProgramMemoryText.contains(" - ")) { //$NON-NLS-1$
						changedProgramMemoryText = changedProgramMemoryText.substring(changedProgramMemoryText.lastIndexOf(" - ") + 3).trim(); //$NON-NLS-1$
					}
					else if (changedProgramMemoryText.contains("CUSTOM")) { //$NON-NLS-1$
						changedProgramMemoryText = changedProgramMemoryText.substring(changedProgramMemoryText.lastIndexOf("CUSTOM") + 6).trim(); //$NON-NLS-1$
					}
					ChargerDialog.this.combo.setText(ChargerDialog.this.lastSelectedProgramMemoryIndex + Messages.getString(MessageIds.GDE_MSGT2625) + changedProgramMemoryText);
					ChargerDialog.this.combo.setForeground(ChargerDialog.this.application.COLOR_RED);
					if (ChargerDialog.this.selectedProgramMemory != null) {
						ChargerDialog.this.selectedProgramMemory.setName(changedProgramMemoryText);
					}
					ChargerDialog.this.btnEdit.setEnabled(false);
					ChargerDialog.this.btnWrite.setEnabled(true);
				}
			}
		});

		this.btnCopy = new Button(this.grpProgramMemory, SWT.NONE);
		this.btnCopy.setLayoutData(new RowData(70, this.comboHeight));
		this.btnCopy.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.btnCopy.setText(Messages.getString(MessageIds.GDE_MSGT2626));
		this.btnCopy.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2627));
		this.btnCopy.setEnabled(false);
		this.btnCopy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ChargerDialog.this.selectedProgramMemory != null) {
					ChargerDialog.this.combo.setForeground(ChargerDialog.this.application.COLOR_RED);
					ChargerDialog.this.copiedProgramMemory = new ChargerMemory(ChargerDialog.this.selectedProgramMemory, ChargerDialog.this.isDuo);
					String batteryType = new String(ChargerDialog.this.copiedProgramMemory.getName()).trim();
					ChargerDialog.this.copiedProgramMemory.setName(batteryType + Messages.getString(MessageIds.GDE_MSGT2620));
					short newIndex = addEntryMemoryHead(batteryType);
					if (ChargerDialog.log.isLoggable(Level.INFO)) ChargerDialog.log.log(Level.INFO, String.format("memoryHead use new index: %s", newIndex)); //$NON-NLS-1$
					if (newIndex > 0) {
						writeProgramMemory(newIndex, ChargerDialog.this.copiedProgramMemory, (short) 0x55aa);
						ChargerDialog.this.copiedProgramMemory = null;
						ChargerDialog.this.combo.setForeground(ChargerDialog.this.application.COLOR_BLACK);
						ChargerDialog.this.combo.setItems(readProgramMemories());
						ChargerDialog.this.combo.select(ChargerDialog.this.lastSelectedComboIndex + 1);
						ChargerDialog.this.combo.notifyListeners(SWT.Selection, new Event());
					} 
				}
			}
		});

		this.btnEdit = new Button(this.grpProgramMemory, SWT.NONE);
		this.btnEdit.setLayoutData(new RowData(70, this.comboHeight));
		this.btnEdit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.btnEdit.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2630));
		this.btnEdit.setText(Messages.getString(MessageIds.GDE_MSGT2631));
		this.btnEdit.setEnabled(false);
		this.btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ChargerDialog.this.combo.setEditable(true);
				ChargerDialog.this.isComboSetEdit = true;
			}
		});

		this.btnWrite = new Button(this.grpProgramMemory, SWT.NONE);
		this.btnWrite.setLayoutData(new RowData(70, this.comboHeight));
		this.btnWrite.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.btnWrite.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2632));
		this.btnWrite.setText(Messages.getString(MessageIds.GDE_MSGT2633));
		this.btnWrite.setEnabled(false);
		this.btnWrite.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ChargerDialog.this.isComboSetEdit) {
					Event evt = new Event();
					evt.character = SWT.CR;
					ChargerDialog.this.combo.notifyListeners(SWT.KeyDown, evt);
					ChargerDialog.this.isComboSetEdit = false;
					WaitTimer.delay(400);
				}
				if (ChargerDialog.this.selectedProgramMemory != null)
					writeProgramMemory(ChargerDialog.this.lastSelectedProgramMemoryIndex, ChargerDialog.this.selectedProgramMemory, (short) 0x55aa);
				ChargerDialog.this.combo.setForeground(ChargerDialog.this.application.COLOR_BLACK);
				String updatedItemText = getFormatedListEntry(ChargerDialog.this.lastSelectedComboIndex, ChargerDialog.this.selectedProgramMemory);
				String[] items = ChargerDialog.this.combo.getItems();
				items[ChargerDialog.this.lastSelectedComboIndex] = updatedItemText;
				ChargerDialog.this.combo.setItems(items);
				ChargerDialog.this.combo.setText(updatedItemText);
				//reduce combo change and not required read write operations
				//ChargerDialog.this.combo.setItems(readProgramMemories());
				//ChargerDialog.this.combo.select(0);
				//ChargerDialog.this.combo.notifyListeners(SWT.Selection, new Event());
				ChargerDialog.this.btnWrite.setEnabled(false);
			}
		});

		this.btnDelete = new Button(this.grpProgramMemory, SWT.NONE);
		this.btnDelete.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.btnDelete.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2634));
		this.btnDelete.setLayoutData(new RowData(70, this.comboHeight));
		this.btnDelete.setText(Messages.getString(MessageIds.GDE_MSGT2635));
		this.btnDelete.setEnabled(false);
		this.btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeMemoryHead((byte) ChargerDialog.this.lastSelectedProgramMemoryIndex);
				ChargerDialog.this.combo.setForeground(ChargerDialog.this.application.COLOR_BLACK);
				ChargerDialog.this.combo.setItems(readProgramMemories());
				ChargerDialog.this.combo.select(0);
				ChargerDialog.this.combo.notifyListeners(SWT.Selection, new Event());
			}
		});
		
		if (this.selectedProgramMemory != null) this.memoryValues = this.selectedProgramMemory.getMemoryValues(this.memoryValues, this.isDuo);

		this.memoryComposite = new Composite(mainMemoryComposite, SWT.BORDER);
		this.memoryComposite.setLayout(new FillLayout(SWT.VERTICAL));
		FormData fdMainComposite = new FormData();
		fdMainComposite.top = new FormAttachment(0, 80);
		fdMainComposite.bottom = new FormAttachment(0, (this.isDuo || this.isDx ? 210 : 190));
		fdMainComposite.right = new FormAttachment(100, -10);
		fdMainComposite.left = new FormAttachment(0, 10);
		this.memoryComposite.setLayoutData(fdMainComposite);
		this.memoryComposite.setBackground(this.application.COLOR_CANVAS_YELLOW);
		createBaseBatteryParameters();

		this.tabFolderProgrMem = new CTabFolder(mainMemoryComposite, SWT.BORDER);
		tabFolderProgrMem.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		FormData fdTabFolderProgrMem = new FormData();
		fdTabFolderProgrMem.top = new FormAttachment(this.memoryComposite, 6);
		fdTabFolderProgrMem.left = new FormAttachment(0, 10);
		fdTabFolderProgrMem.right = new FormAttachment(100, -10);
		this.tabFolderProgrMem.setLayoutData(fdTabFolderProgrMem);
		this.tabFolderProgrMem.setSimple(false);

		createChargeTabItem();
		createDischargeTabItem();
		createStorageTabItem();
		createCycleTabItem();
		createOptionTabItem();
		//createPowerTabItem();
		this.tabFolderProgrMem.setSelection(0);
		updateMemoryParameterControls();

		this.grpRunProgram = new Group(mainMemoryComposite, SWT.NONE);
		fdTabFolderProgrMem.bottom = new FormAttachment(this.grpRunProgram, -5);
		this.grpRunProgram.setText(Messages.getString(MessageIds.GDE_MSGT2685));
		RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
		rowLayout.justify = true;
		rowLayout.fill = GDE.IS_WINDOWS | GDE.IS_OS_ARCH_ARM;
		this.grpRunProgram.setLayout(rowLayout);
		FormData fd_grpRunProgram = new FormData();
		fd_grpRunProgram.left = new FormAttachment(0, 10);
		fd_grpRunProgram.right = new FormAttachment(100, -120);
		fd_grpRunProgram.top = new FormAttachment(100, -65);
		fd_grpRunProgram.bottom = new FormAttachment(100, -5);
		this.grpRunProgram.setLayoutData(fd_grpRunProgram);
		this.grpRunProgram.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2686));

		this.btnCharge = new Button(this.grpRunProgram, SWT.NONE);
		this.btnCharge.setLayoutData(new RowData(85, this.comboHeight));
		this.btnCharge.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.btnCharge.setText(Messages.getString(MessageIds.GDE_MSGT2687));
		this.btnCharge.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()] >= 0) {
					startProgramExecution((byte) OperationLi.Charge.ordinal(), (byte) (ChargerDialog.this.application.getActiveChannelNumber() - 1),
							ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()]);
					WaitTimer.delay(400);
					ChargerDialog.this.systemInfo = ChargerDialog.this.readInfo();
					if (ChargerDialog.this.systemInfo != null && (ChargerDialog.this.systemInfo.getStatus() & 0x02) != 0) {
						log.log(Level.WARNING, ChargerDialog.this.systemInfo.getStatusString());
						ChargerDialog.this.application.openMessageDialog(ChargerDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGE2604));
					}
					else {
						ChargerDialog.this.startLogTransmission();
						ChargerDialog.this.btnCharge.setEnabled(false);
						ChargerDialog.this.btnStorage.setEnabled(false);
						ChargerDialog.this.btnDischarge.setEnabled(false);
						ChargerDialog.this.btnCycle.setEnabled(false);
						ChargerDialog.this.btnBalance.setEnabled(false);
						ChargerDialog.this.btnStop.setEnabled(true);
						ChargerDialog.this.grpProgramMemory.setEnabled(false);
						ChargerDialog.this.memoryComposite.setEnabled(false);
						removeAllListeners();
						ChargerDialog.this.device.open_closeCommPort();
					} 
				}
			}
		});

		this.btnStorage = new Button(this.grpRunProgram, SWT.NONE);
		this.btnStorage.setLayoutData(new RowData(85, this.comboHeight));
		this.btnStorage.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.btnStorage.setText(Messages.getString(MessageIds.GDE_MSGT2688));
		this.btnStorage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()] >= 0) {
					startProgramExecution((byte) OperationLi.Storage.ordinal(), (byte) (ChargerDialog.this.application.getActiveChannelNumber() - 1),
							ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()]);
					WaitTimer.delay(400);
					ChargerDialog.this.systemInfo = ChargerDialog.this.readInfo();
					if (ChargerDialog.this.systemInfo != null && (ChargerDialog.this.systemInfo.getStatus() & 0x02) != 0) {
						log.log(Level.WARNING, ChargerDialog.this.systemInfo.getStatusString());
						ChargerDialog.this.application.openMessageDialog(ChargerDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGE2604));
					}
					else {
						ChargerDialog.this.startLogTransmission();
						ChargerDialog.this.btnCharge.setEnabled(false);
						ChargerDialog.this.btnStorage.setEnabled(false);
						ChargerDialog.this.btnDischarge.setEnabled(false);
						ChargerDialog.this.btnCycle.setEnabled(false);
						ChargerDialog.this.btnBalance.setEnabled(false);
						ChargerDialog.this.btnStop.setEnabled(true);
						ChargerDialog.this.grpProgramMemory.setEnabled(false);
						ChargerDialog.this.memoryComposite.setEnabled(false);
						removeAllListeners();
						ChargerDialog.this.device.open_closeCommPort();
					} 
				}
			}
		});

		this.btnDischarge = new Button(this.grpRunProgram, SWT.NONE);
		this.btnDischarge.setLayoutData(new RowData(85, this.comboHeight));
		this.btnDischarge.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.btnDischarge.setText(Messages.getString(MessageIds.GDE_MSGT2689));
		this.btnDischarge.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()] >= 0) {
					byte operationIndex = ChargerDialog.this.cellTypeNamesArray[ChargerDialog.this.memoryValues[0]].startsWith("L") 
							? (byte) OperationLi.Discharge.ordinal() : (byte) OperationNiPb.Discharge.ordinal();
					startProgramExecution(operationIndex, (byte) (ChargerDialog.this.application.getActiveChannelNumber() - 1),
							ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()]);
					WaitTimer.delay(400);
					if ((ChargerDialog.this.systemInfo.getStatus() & 0x02) != 0) {
						log.log(Level.WARNING, ChargerDialog.this.systemInfo.getStatusString());
						ChargerDialog.this.application.openMessageDialog(ChargerDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGE2604));
					}
					else {
						ChargerDialog.this.startLogTransmission();
						ChargerDialog.this.btnCharge.setEnabled(false);
						ChargerDialog.this.btnStorage.setEnabled(false);
						ChargerDialog.this.btnDischarge.setEnabled(false);
						ChargerDialog.this.btnCycle.setEnabled(false);
						ChargerDialog.this.btnBalance.setEnabled(false);
						ChargerDialog.this.btnStop.setEnabled(true);
						ChargerDialog.this.grpProgramMemory.setEnabled(false);
						ChargerDialog.this.memoryComposite.setEnabled(false);
						removeAllListeners();
						ChargerDialog.this.device.open_closeCommPort();
					}
				}
			}
		});

		this.btnCycle = new Button(this.grpRunProgram, SWT.NONE);
		this.btnCycle.setLayoutData(new RowData(85, this.comboHeight));
		this.btnCycle.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.btnCycle.setText(Messages.getString(MessageIds.GDE_MSGT2690));
		this.btnCycle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()] >= 0) {
					byte operationIndex = ChargerDialog.this.cellTypeNamesArray[ChargerDialog.this.memoryValues[0]].startsWith("L") 
							? (byte) OperationLi.Cycle.ordinal() : (byte) OperationNiPb.Cycle.ordinal();
					startProgramExecution(operationIndex, (byte) (ChargerDialog.this.application.getActiveChannelNumber() - 1),
							ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()]);
					WaitTimer.delay(400);
					ChargerDialog.this.systemInfo = ChargerDialog.this.readInfo();
					if (ChargerDialog.this.systemInfo != null && (ChargerDialog.this.systemInfo.getStatus() & 0x02) != 0) {
						log.log(Level.WARNING, ChargerDialog.this.systemInfo.getStatusString());
						ChargerDialog.this.application.openMessageDialog(ChargerDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGE2604));
					}
					else {
						ChargerDialog.this.startLogTransmission();
						ChargerDialog.this.btnCharge.setEnabled(false);
						ChargerDialog.this.btnStorage.setEnabled(false);
						ChargerDialog.this.btnDischarge.setEnabled(false);
						ChargerDialog.this.btnCycle.setEnabled(false);
						ChargerDialog.this.btnBalance.setEnabled(false);
						ChargerDialog.this.btnStop.setEnabled(true);
						ChargerDialog.this.grpProgramMemory.setEnabled(false);
						ChargerDialog.this.memoryComposite.setEnabled(false);
						removeAllListeners();
						ChargerDialog.this.device.open_closeCommPort();
					}
				}
			}
		});

		this.btnBalance = new Button(this.grpRunProgram, SWT.NONE);
		this.btnBalance.setLayoutData(new RowData(85, this.comboHeight));
		this.btnBalance.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.btnBalance.setText(Messages.getString(MessageIds.GDE_MSGT2691));
		this.btnBalance.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()] >= 0) {
					startProgramExecution((byte) OperationLi.Balance.ordinal(), (byte) (ChargerDialog.this.application.getActiveChannelNumber() - 1),
							ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()]);
					WaitTimer.delay(400);
					ChargerDialog.this.systemInfo = ChargerDialog.this.readInfo();
					if (ChargerDialog.this.systemInfo != null && (ChargerDialog.this.systemInfo.getStatus() & 0x02) != 0) {
						log.log(Level.WARNING, ChargerDialog.this.systemInfo.getStatusString());
						ChargerDialog.this.application.openMessageDialog(ChargerDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGE2604));
					}
					else {
						ChargerDialog.this.startLogTransmission();
						ChargerDialog.this.btnCharge.setEnabled(false);
						ChargerDialog.this.btnStorage.setEnabled(false);
						ChargerDialog.this.btnDischarge.setEnabled(false);
						ChargerDialog.this.btnCycle.setEnabled(false);
						ChargerDialog.this.btnBalance.setEnabled(false);
						ChargerDialog.this.btnStop.setEnabled(true);
						ChargerDialog.this.grpProgramMemory.setEnabled(false);
						ChargerDialog.this.memoryComposite.setEnabled(false);
						removeAllListeners();
						ChargerDialog.this.device.open_closeCommPort();
					}
				}
			}
		});

		this.btnPower = new Button(this.grpRunProgram, SWT.NONE);
		this.btnPower.setLayoutData(new RowData(85, this.comboHeight));
		this.btnPower.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.btnPower.setText("Power"); //$NON-NLS-1$
		this.btnPower.setEnabled(false);
		this.btnPower.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()] >= 0) {
					//set select to run operations = 0(Power), Select MEMORY, Select CHANNEL=0, then send Order RUN command
					startProgramExecution((byte) 0, (byte) 0, ChargerDialog.this.memoryHeadIndex[ChargerDialog.this.combo.getSelectionIndex()]);
					WaitTimer.delay(400);
					ChargerDialog.this.systemInfo = ChargerDialog.this.readInfo();
					if (ChargerDialog.this.systemInfo != null && (ChargerDialog.this.systemInfo.getStatus() & 0x02) != 0) {
						log.log(Level.WARNING, ChargerDialog.this.systemInfo.getStatusString());
						ChargerDialog.this.application.openMessageDialog(ChargerDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGE2604));
					}
					else {
						ChargerDialog.this.startLogTransmission();
						ChargerDialog.this.btnCharge.setEnabled(false);
						ChargerDialog.this.btnStorage.setEnabled(false);
						ChargerDialog.this.btnDischarge.setEnabled(false);
						ChargerDialog.this.btnCycle.setEnabled(false);
						ChargerDialog.this.btnBalance.setEnabled(false);
						ChargerDialog.this.btnPower.setEnabled(false);
						ChargerDialog.this.btnStop.setEnabled(true);
						ChargerDialog.this.grpProgramMemory.setEnabled(false);
						ChargerDialog.this.memoryComposite.setEnabled(false);
						removeAllListeners();
						ChargerDialog.this.device.open_closeCommPort();
					}
				}
			}
		});

		this.btnStop = new Button(this.grpRunProgram, SWT.NONE);
		this.btnStop.setLayoutData(new RowData(85, this.comboHeight));
		this.btnStop.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.btnStop.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2692));
		this.btnStop.setText(Messages.getString(MessageIds.GDE_MSGT2693));
		this.btnStop.setEnabled(false);
		this.btnStop.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ChargerDialog.this.device.open_closeCommPort();
				try {
					ChargerDialog.this.usbPort.getData(); //sync logging and modbus query
					WaitTimer.delay(100);
				}
				catch (Exception ex) {
					//ignore 
				}
				ChargerDialog.this.stopProgramExecution((byte) (ChargerDialog.this.application.getActiveChannelNumber() - 1));
				ChargerDialog.this.dialogShell.dispose();
			}
		});

		Button btnHelp = new Button(mainMemoryComposite, SWT.NONE);
		FormData fd_btnHelp = new FormData();
		fd_btnHelp.width = GDE.IS_MAC ? 50 : 40;
		fd_btnHelp.height = GDE.IS_MAC ? 33 : 30;
		fd_btnHelp.bottom = new FormAttachment(100, -35);
		fd_btnHelp.right = new FormAttachment(100, -40);
		btnHelp.setLayoutData(fd_btnHelp);
		btnHelp.setImage(SWTResourceManager.getImage("gde/resource/QuestionHot.gif"));
		btnHelp.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2628)); //$NON-NLS-1$
		btnHelp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				ChargerDialog.this.application.openHelpDialog("iCharger", "HelpInfo.html");  //$NON-NLS-1$
			}
		});

		Button btnClose = new Button(mainMemoryComposite, SWT.NONE);
		FormData fd_btnCancel = new FormData();
		fd_btnCancel.height = GDE.IS_MAC ? 33 : 30;
		fd_btnCancel.bottom = new FormAttachment(100, -5);
		fd_btnCancel.right = new FormAttachment(100, -10);
		fd_btnCancel.left = new FormAttachment(100, -110);
		btnClose.setLayoutData(fd_btnCancel);
		btnClose.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		btnClose.setText(Messages.getString(MessageIds.GDE_MSGT2694));
		btnClose.setToolTipText(Messages.getString(MessageIds.GDE_MSGI2610)); //$NON-NLS-1$
		btnClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				ChargerDialog.this.dialogShell.dispose();
			}
		});
		
		
		CTabItem tbtmSystem = new CTabItem(mainTabFolder, SWT.NONE);
		tbtmSystem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		tbtmSystem.setText(Messages.getString(MessageIds.GDE_MSGI2605)); //$NON-NLS-1$
		Composite mainSystemComposite = new Composite(mainTabFolder, SWT.NONE);
		tbtmSystem.setControl(mainSystemComposite);
		mainSystemComposite.setLayout(new FormLayout());
	
		ScrolledComposite sysScrolledComposite = new ScrolledComposite(mainSystemComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		FormData fd_scrolledComposite = new FormData();
		fd_scrolledComposite.top = new FormAttachment(0);
		fd_scrolledComposite.bottom = new FormAttachment(100, -60);
		fd_scrolledComposite.right = new FormAttachment(100);
		fd_scrolledComposite.left = new FormAttachment(0);
		sysScrolledComposite.setLayoutData(fd_scrolledComposite);
		sysScrolledComposite.setLayout(new FillLayout());
		sysComposite = new Composite(sysScrolledComposite, SWT.NONE);
		sysScrolledComposite.setExpandHorizontal(true);
		//sysScrolledComposite.setExpandVertical(true);
		sysScrolledComposite.setContent(sysComposite);
		sysScrolledComposite.setMinSize(sysComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sysComposite.setSize(800, GDE.IS_WINDOWS ? 1150 : 1300);
		sysComposite.setLayout(new RowLayout(SWT.VERTICAL));
		
		grpTemperature = new Group(sysComposite, SWT.NONE);
		grpTemperature.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		grpTemperature.setText(Messages.getString(MessageIds.GDE_MSGI2603)); //$NON-NLS-1$
		grpTemperature.setLayout(new RowLayout(SWT.VERTICAL));
		//0 Unit
		this.systemParameters[0] = new ParameterConfigControl(this.grpTemperature, this.systemValues, 0, 
				Messages.getString(MessageIds.GDE_MSGI2604), 175,  //$NON-NLS-1$
				"Celsius, Fahrenheit", 280, //$NON-NLS-1$
				new String[] { "°C", "°F" }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		//1 shut down temperature
		this.systemParameters[1] = new ParameterConfigControl(this.grpTemperature, this.systemValues, 1, "%3.1f",  //$NON-NLS-1$
				Messages.getString(MessageIds.GDE_MSGI2606), 175,  //$NON-NLS-1$
				"65°C - 80°C", 280, //$NON-NLS-1$
				true, 50, 200, 650, 800, -650, false);
		//2 power reduce delta
		this.systemParameters[2] = new ParameterConfigControl(this.grpTemperature, this.systemValues, 2, "%3.1f",  //$NON-NLS-1$
				Messages.getString(MessageIds.GDE_MSGI2608), 175,  //$NON-NLS-1$
				"5°C - 20°C", 280, //$NON-NLS-1$
				true, 50, 200, 50, 200, -50, false);
		grpTemperature.addListener(SWT.Selection, this.systemParameterChangeListener);
		grpTemperature.layout();

		grpFans = new Group(sysComposite, SWT.NONE);
		grpFans.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		grpFans.setText(Messages.getString(MessageIds.GDE_MSGI2609)); //$NON-NLS-1$
		grpFans.setLayout(new RowLayout(SWT.VERTICAL));
		//3 Cooling fan on temperature
		this.systemParameters[3] = new ParameterConfigControl(this.grpFans, this.systemValues, 3, "%3.1f",  //$NON-NLS-1$
				Messages.getString(MessageIds.GDE_MSGI2611), 175,  //$NON-NLS-1$
				"30°C - 50°C", 280, //$NON-NLS-1$
				true, 50, 200, 300, 500, -300, false);
		//4 Cooling fan off delay time
		this.systemParameters[4] = new ParameterConfigControl(this.grpFans, this.systemValues, 4, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2612), 175,  //$NON-NLS-1$
				"0 Min - 10 Min", 280, //$NON-NLS-1$
				true, 50, 200, 0, 10, 0, false);
		grpFans.addListener(SWT.Selection, this.systemParameterChangeListener);
		grpFans.layout();
		
		grpBeepTone = new Group(sysComposite, SWT.NONE);
		grpBeepTone.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		grpBeepTone.setText(Messages.getString(MessageIds.GDE_MSGI2613)); //$NON-NLS-1$
		grpBeepTone.setLayout(new RowLayout(SWT.VERTICAL));
		//5 Beep volume buttons
		this.systemParameters[5] = new ParameterConfigControl(this.grpBeepTone, this.systemValues, 5, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2614), 175,  //$NON-NLS-1$
				"(off)0 - 10", 280, //$NON-NLS-1$
				true, 50, 200, 0, 10, 0, false);
		//6 Beep volume tip
		this.systemParameters[6] = new ParameterConfigControl(this.grpBeepTone, this.systemValues, 6, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2615), 175,  //$NON-NLS-1$
				"(off)0 - 10", 280, //$NON-NLS-1$
				true, 50, 200, 0, 10, 0, false);
		//7 Beep volume alarm
		this.systemParameters[7] = new ParameterConfigControl(this.grpBeepTone, this.systemValues, 7, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2616), 175,  //$NON-NLS-1$
				"(off)0 - 10", 280, //$NON-NLS-1$
				true, 50, 200, 0, 10, 0, false);
		//8 Beep volume end
		this.systemParameters[8] = new ParameterConfigControl(this.grpBeepTone, this.systemValues, 8, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2617), 175,  //$NON-NLS-1$
				"(off)0 - 10", 280, //$NON-NLS-1$
				true, 50, 200, 0, 10, 0, false);
		//9 Beep type
		this.systemParameters[9] = new ParameterConfigControl(this.grpBeepTone, this.systemValues, 9, 
				Messages.getString(MessageIds.GDE_MSGI2618), 175,  //$NON-NLS-1$
				"5x, 30s, 3min, "+ Messages.getString(MessageIds.GDE_MSGI2620), 280, //$NON-NLS-1$
				new String[] { "5x", "30S", "3Min", Messages.getString(MessageIds.GDE_MSGI2620) }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		grpBeepTone.addListener(SWT.Selection, this.systemParameterChangeListener);
		grpBeepTone.layout();
		
		grpLcdScreen = new Group(sysComposite, SWT.NONE);
		grpLcdScreen.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		grpLcdScreen.setText(Messages.getString(MessageIds.GDE_MSGI2621)); //$NON-NLS-1$
		grpLcdScreen.setLayout(new RowLayout(SWT.VERTICAL));
		//10 LCD brightness
		this.systemParameters[10] = new ParameterConfigControl(this.grpLcdScreen, this.systemValues, 10, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2622), 175,  //$NON-NLS-1$
				"0 - 32", 280, //$NON-NLS-1$
				true, 50, 200, 0, 32, 0, false);
		//11 LCD contrast
		this.systemParameters[11] = new ParameterConfigControl(this.grpLcdScreen, this.systemValues, 11, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2623), 175,  //$NON-NLS-1$
				"0 - 32", 280, //$NON-NLS-1$
				true, 50, 200, 0, 32, 0, false);
		grpLcdScreen.addListener(SWT.Selection, this.systemParameterChangeListener);
		grpLcdScreen.layout();
		
		grpLanguage = new Group(sysComposite, SWT.NONE);
		grpLanguage.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		grpLanguage.setText("Language");
		grpLanguage.setLayout(new RowLayout(SWT.VERTICAL));
		//27 language 0=en 1=de
		this.systemParameters[27] = new ParameterConfigControl(this.grpLanguage, this.systemValues, 27, 
				"Language", 175, //$NON-NLS-1$
				"English, Deutsch, Chinese", 280, //$NON-NLS-1$
				new String[] { "en", "de", "zh" }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		grpLanguage.addListener(SWT.Selection, systemParameterChangeListener);
		grpLanguage.layout();

//		grpSaveLoadConfig = new Group(sysComposite, SWT.NONE);
//		grpSaveLoadConfig.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//		grpSaveLoadConfig.setText("Save, Restore & Load Configuration");
//		grpSaveLoadConfig.setLayout(new RowLayout(SWT.VERTICAL));
//		
//		btnSaveActualConf = new Button(grpSaveLoadConfig, SWT.NONE);
//		btnSaveActualConf.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//		btnSaveActualConf.setText("Save Actual System Configuration");
//		btnSaveActualConf.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//			}
//		});
//		
//		btnRestoreSavedConf = new Button(grpSaveLoadConfig, SWT.NONE);
//		btnRestoreSavedConf.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//		btnRestoreSavedConf.setText("Restore Saved System Configuration");
//		btnRestoreSavedConf.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//			}
//		});
//		
//		btnLoadDefaultSysConf = new Button(grpSaveLoadConfig, SWT.NONE);
//		btnLoadDefaultSysConf.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//		btnLoadDefaultSysConf.setText("Load Default System Configuration");
//		btnLoadDefaultSysConf.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//			}
//		});
//		grpSaveLoadConfig.layout();
		
		//createGrpInputDischargePowerLimits(); //X/S/DX
		//createGrpChargeDischargePower();//dual channel devices only		
		
		btnSystemSave = new Button(mainSystemComposite, SWT.NONE);
		FormData fd_btnSave = new FormData();
		fd_btnSave.bottom = new FormAttachment(100, -10);
		fd_btnSave.left = new FormAttachment(0, 100);
		fd_btnSave.right = new FormAttachment(0, 300);
		btnSystemSave.setLayoutData(fd_btnSave);
		btnSystemSave.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		btnSystemSave.setText(Messages.getString(MessageIds.GDE_MSGI2624)); //$NON-NLS-1$
		btnSystemSave.setToolTipText(Messages.getString(MessageIds.GDE_MSGI2625)); //$NON-NLS-1$
		btnSystemSave.setEnabled(false);
		btnSystemSave.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
						btnSystemSave.setEnabled(!writeSystemMemory(isDuo));
				}
				catch (Exception e1) {
					log.log(Level.SEVERE, e1.getMessage(), e);
				}
			}
		});
		
		Button btnSystemClose = new Button(mainSystemComposite, SWT.NONE);
		FormData fd_btnSystemClose = new FormData();
		fd_btnSystemClose.bottom = new FormAttachment(100, -10);
		fd_btnSystemClose.left = new FormAttachment(100, -300);
		fd_btnSystemClose.right = new FormAttachment(100, -100);
		btnSystemClose.setLayoutData(fd_btnSystemClose);
		btnSystemClose.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		btnSystemClose.setText(Messages.getString(MessageIds.GDE_MSGT2694));
		btnSystemClose.setToolTipText(Messages.getString(MessageIds.GDE_MSGI2610)); //$NON-NLS-1$
		btnSystemClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ChargerDialog.this.dialogShell.dispose();
			}
		});

		updateSystemParameterControls();
	}
	
	private void createGrpDuoInputPowerLimits() {
		grpDuoInputPowerLimits = new Group(sysComposite, SWT.NONE);
		grpDuoInputPowerLimits.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		grpDuoInputPowerLimits.setText(Messages.getString(MessageIds.GDE_MSGI2626)); //$NON-NLS-1$
		grpDuoInputPowerLimits.setLayout(new RowLayout(SWT.VERTICAL));
		grpDuoInputPowerLimits.setBackground(application.COLOR_CANVAS_YELLOW);
		//17 input select
		this.systemParameters[17] = new ParameterConfigControl(this.grpDuoInputPowerLimits, this.systemValues, 17, GDE.STRING_EMPTY, 175,
				Messages.getString(MessageIds.GDE_MSGI2647), 280, new String[] { "DC", "Bat" }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		grpDuoInputPowerLimits.addListener(SWT.Selection, systemParameterChangeListener);
	
		grpDcInputPowerLimits = new Group(grpDuoInputPowerLimits, SWT.NONE);
		grpDcInputPowerLimits.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		grpDcInputPowerLimits.setText(Messages.getString(MessageIds.GDE_MSGI2635)); //$NON-NLS-1$
		grpDcInputPowerLimits.setLayout(new RowLayout(SWT.VERTICAL));
		//28 duo dcInputLowVolt
		this.systemParameters[28] = new ParameterConfigControl(this.grpDcInputPowerLimits, this.systemValues, 28, "%3.1f", 
				Messages.getString(MessageIds.GDE_MSGI2631), 175,  //$NON-NLS-1$
				String.format("%dV - %dV", device.getDcInputVoltMin()/10, device.getDcInputVoltMax()/10), 280, //$NON-NLS-1$
				true, 50, 200, device.getDcInputVoltMin(), device.getDcInputVoltMax(), -1*device.getDcInputVoltMin()/10, false);
		//29 duo dcInputCurrentLimit
		this.systemParameters[29] = new ParameterConfigControl(this.grpDcInputPowerLimits, this.systemValues, 29, "%3.1f", 
				Messages.getString(MessageIds.GDE_MSGI2632), 175,  //$NON-NLS-1$
				String.format("1A - %dA", device.getDcInputCurrentMax()/10), 280, //$NON-NLS-1$
				true, 50, 200, 10, device.getDcInputCurrentMax(), -1, false);
		grpDcInputPowerLimits.addListener(SWT.Selection, systemParameterChangeListener);
				
		grpBatInputPowerLimits = new Group(grpDuoInputPowerLimits, SWT.NONE);
		grpBatInputPowerLimits.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		grpBatInputPowerLimits.setText(Messages.getString(MessageIds.GDE_MSGI2636)); //$NON-NLS-1$
		grpBatInputPowerLimits.setLayout(new RowLayout(SWT.VERTICAL));
		//30 duo batInputLowVolt
		this.systemParameters[30] = new ParameterConfigControl(this.grpBatInputPowerLimits, this.systemValues, 30, "%3.1f", 
				Messages.getString(MessageIds.GDE_MSGI2631), 175,  //$NON-NLS-1$
				String.format("%dV - %dV", device.getDcInputVoltMin()/10, device.getDcInputVoltMax()/10), 280, //$NON-NLS-1$
				true, 50, 200, device.getDcInputVoltMin(), device.getDcInputVoltMax(), -1*device.getDcInputVoltMin()/10, false);
		//31 duo batInputCurrentLimit 
		this.systemParameters[31] = new ParameterConfigControl(this.grpBatInputPowerLimits, this.systemValues, 31, "%3.1f", 
				Messages.getString(MessageIds.GDE_MSGI2632), 175,  //$NON-NLS-1$
				String.format("1A - %dA", device.getDcInputCurrentMax()/10), 280, //$NON-NLS-1$
				true, 50, 200, 10, device.getDcInputCurrentMax(), -1, false);
		//32 duo regEnable
		this.systemParameters[32] = new ParameterConfigControl(this.grpBatInputPowerLimits, this.systemValues, 32, 
				Messages.getString(MessageIds.GDE_MSGT2674), 175, 
				"off, on", 280, //$NON-NLS-1$
				new String[] { "off", "on" }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		
		grpBatInputPowerLimits.addListener(SWT.Selection, systemParameterChangeListener);

		grpDuoRegInputPowerLimits = new Group(grpBatInputPowerLimits, SWT.NONE);
		grpDuoRegInputPowerLimits.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		grpDuoRegInputPowerLimits.setText(Messages.getString(MessageIds.GDE_MSGI2634)); //$NON-NLS-1$
		grpDuoRegInputPowerLimits.setLayout(new RowLayout(SWT.VERTICAL));
		grpDuoRegInputPowerLimits.setBackground(application.COLOR_GREY);
		grpDuoRegInputPowerLimits.setEnabled(systemValues[32] == 1);
		//33 duo regVoltLimit
		this.systemParameters[33] = new ParameterConfigControl(this.grpDuoRegInputPowerLimits, this.systemValues, 33, "%3.1f", 
				Messages.getString(MessageIds.GDE_MSGI2631), 175,  //$NON-NLS-1$
				String.format("%dV - %dV", device.getRegInputVoltMin()/10, device.getRegInputVoltMax()/10) , 280, //$NON-NLS-1$
				true, 50, 200, device.getRegInputVoltMin(), device.getRegInputVoltMax(), -1*device.getRegInputVoltMin()/10, false);
		//34 duo regCurrentLimit
		this.systemParameters[34] = new ParameterConfigControl(this.grpDuoRegInputPowerLimits, this.systemValues, 34, "%3.1f", 
				Messages.getString(MessageIds.GDE_MSGI2632), 175,  //$NON-NLS-1$
				String.format("1A - %dA", device.getDcInputCurrentMax()/10), 280, //$NON-NLS-1$
				true, 50, 200, 10, device.getDcInputCurrentMax(), -1, false);
		//35 duo regCapacityLimit
		this.systemParameters[35] = new ParameterConfigControl(this.grpDuoRegInputPowerLimits, this.systemValues, 35, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2638), 175,  //$NON-NLS-1$
				"(ingnore)0Ah - 999900Ah", 280, //$NON-NLS-1$
				true, 50, 200, 0, 9999, 0, false);
		
		grpDuoRegInputPowerLimits.addListener(SWT.Selection, systemParameterChangeListener);

		grpDuoInputPowerLimits.layout();
	}

	private void createGrpInputDischargePowerLimits() {
		grpInputDischargePowerLimits = new Group(sysComposite, SWT.NONE);
		grpInputDischargePowerLimits.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		grpInputDischargePowerLimits.setText(Messages.getString(MessageIds.GDE_MSGI2627)); //$NON-NLS-1$
		grpInputDischargePowerLimits.setLayout(new RowLayout(SWT.VERTICAL));
		grpInputDischargePowerLimits.setBackground(application.COLOR_CANVAS_YELLOW);
		//17 input select
		this.systemParameters[17] = new ParameterConfigControl(this.grpInputDischargePowerLimits, this.systemValues, 17, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2628), 175,  //$NON-NLS-1$
				"0 - 3", 280, //$NON-NLS-1$
				true, 50, 200, 0, 3, 0, false);
		if (!isDx) {
			//18 discharge power Limit
			this.systemParameters[18] = new ParameterConfigControl(this.grpInputDischargePowerLimits, this.systemValues, 18, GDE.STRING_EMPTY, 
					Messages.getString(MessageIds.GDE_MSGI2629), 175,  //$NON-NLS-1$
					String.format("5W - %dW", device.getDischargePowerMax()[0]), 280, //$NON-NLS-1$
					true, 50, 200, 5, device.getDischargePowerMax()[0], -5, false);
		}
		grpInputDischargePowerLimits.addListener(SWT.Selection, systemParameterChangeListener);
		
		grpInputPowerLimits = new Group(grpInputDischargePowerLimits, SWT.NONE);
		grpInputPowerLimits.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		grpInputPowerLimits.setText(Messages.getString(MessageIds.GDE_MSGI2630)); //$NON-NLS-1$
		grpInputPowerLimits.setLayout(new RowLayout(SWT.VERTICAL));
		//19 inputLowVolt
		this.systemParameters[19] = new ParameterConfigControl(this.grpInputPowerLimits, this.systemValues, 19, "%3.1f", 
				Messages.getString(MessageIds.GDE_MSGI2631), 175,  //$NON-NLS-1$
				String.format("%dV - %dV", device.getDcInputVoltMin()/10, device.getDcInputVoltMax()/10), 280, //$NON-NLS-1$
				true, 50, 200, device.getDcInputVoltMin(), device.getDcInputVoltMax(), -1*device.getDcInputVoltMin()/10, false);
		//20 inputCurrentLimit
		this.systemParameters[20] = new ParameterConfigControl(this.grpInputPowerLimits, this.systemValues, 20, "%3.1f", 
				Messages.getString(MessageIds.GDE_MSGI2632), 175,  //$NON-NLS-1$
				String.format("1A - %dA", device.getDcInputCurrentMax()/10), 280, //$NON-NLS-1$
				true, 50, 200, 10, device.getDcInputCurrentMax(), -1, false);
		//21 Charge power 
		this.systemParameters[21] = new ParameterConfigControl(this.grpInputPowerLimits, this.systemValues, 21, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2633), 175,  //$NON-NLS-1$
				String.format("5W - %dW", device.getDeviceChargePowerMax()), 280, //$NON-NLS-1$
				true, 50, 200, 5, device.getDeviceChargePowerMax(), -5, false);
		//22 regEnable
		this.systemParameters[22] = new ParameterConfigControl(this.grpInputPowerLimits, this.systemValues, 22, 
				Messages.getString(MessageIds.GDE_MSGT2674), 175, 
				"off, on", 280, //$NON-NLS-1$
				new String[] { "off", "on" }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		grpInputPowerLimits.addListener(SWT.Selection, systemParameterChangeListener);
		
		grpRegInputPowerLimits = new Group(grpInputPowerLimits, SWT.NONE);
		grpRegInputPowerLimits.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		grpRegInputPowerLimits.setText(Messages.getString(MessageIds.GDE_MSGI2634)); //$NON-NLS-1$
		grpRegInputPowerLimits.setLayout(new RowLayout(SWT.VERTICAL));
		grpRegInputPowerLimits.setBackground(application.COLOR_GREY);
		grpRegInputPowerLimits.setEnabled(systemValues[22] == 1);
		//23 regVoltLimit
		this.systemParameters[23] = new ParameterConfigControl(this.grpRegInputPowerLimits, this.systemValues, 23, "%3.1f", 
				Messages.getString(MessageIds.GDE_MSGI2631), 175,  //$NON-NLS-1$
				String.format("%dV - %dV", device.getRegInputVoltMin()/10, device.getRegInputVoltMax()/10) , 280, //$NON-NLS-1$
				true, 50, 200, device.getRegInputVoltMin(), device.getRegInputVoltMax(), -1*device.getRegInputVoltMin()/10, false);
		//24 regCurrentLimit
		this.systemParameters[24] = new ParameterConfigControl(this.grpRegInputPowerLimits, this.systemValues, 24, "%3.1f", 
				Messages.getString(MessageIds.GDE_MSGI2632), 175,  //$NON-NLS-1$
				String.format("1A - %dA", device.getDcInputCurrentMax()/10), 280, //$NON-NLS-1$
				true, 50, 200, 10, device.getDcInputCurrentMax(), -1, false);
		//25 regPowerLimit
		this.systemParameters[25] = new ParameterConfigControl(this.grpRegInputPowerLimits, this.systemValues, 25, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2633), 175,  //$NON-NLS-1$
				String.format("5W - %dW", device.getChargePowerMax()[0]), 280, //$NON-NLS-1$
				true, 50, 200, 5, device.getChargePowerMax()[0], -5, false);
		//26 regCapLimit
		this.systemParameters[26] = new ParameterConfigControl(this.grpRegInputPowerLimits, this.systemValues, 26, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2638), 175,  //$NON-NLS-1$
				"(ignore)0Ah - 999900Ah", 280, //$NON-NLS-1$
				true, 50, 200, 0, 999900, 0, false);
		grpRegInputPowerLimits.addListener(SWT.Selection, systemParameterChangeListener);
		grpInputDischargePowerLimits.layout();
	}

	private void createGrpChargeDischargePower() {
		grpChargeDischargePower = new Group(sysComposite, SWT.NONE);
		grpChargeDischargePower.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		grpChargeDischargePower.setText(Messages.getString(MessageIds.GDE_MSGI2639)); //$NON-NLS-1$
		grpChargeDischargePower.setLayout(new RowLayout(SWT.VERTICAL));
		//12 Charge power channel 1
		this.systemParameters[12] = new ParameterConfigControl(this.grpChargeDischargePower, this.systemValues, 12, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2640, new String[] {Messages.getString(MessageIds.GDE_MSGI2645).split(",")[1]}), 175, 
				String.format("5W - %dW", device.getChargePowerMax()[0]), 280, //$NON-NLS-1$
				true, 50, 200, 5, device.getChargePowerMax()[0], -5, false);
		//14 Discharge power channel 1
		this.systemParameters[14] = new ParameterConfigControl(this.grpChargeDischargePower, this.systemValues, 14, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2642, new String[] {Messages.getString(MessageIds.GDE_MSGI2645).split(",")[1]}), 175,  //$NON-NLS-1$
				String.format("5W - %dW", device.getDischargePowerMax()[0]), 280, //$NON-NLS-1$
				true, 50, 200, 5, device.getDischargePowerMax()[0], -5, false);
		//16 Power distribution
		this.systemParameters[16] = new ParameterConfigControl(this.grpChargeDischargePower, this.systemValues, 16, 
				Messages.getString(MessageIds.GDE_MSGI2644), 175,
				Messages.getString(MessageIds.GDE_MSGI2645), 280,
				Messages.getString(MessageIds.GDE_MSGI2645).split(","), 50, 200); //$NON-NLS-1$
		//13 Charge power channel 2
		this.systemParameters[13] = new ParameterConfigControl(this.grpChargeDischargePower, this.systemValues, 13, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2640, new String[] {Messages.getString(MessageIds.GDE_MSGI2645).split(",")[2]}), 175, 
				String.format("5W - %dW", device.getChargePowerMax()[1]), 280, //$NON-NLS-1$
				true, 50, 200, 5, device.getChargePowerMax()[1], -5, false);
		//15 Discharge power channel 2
		this.systemParameters[15] = new ParameterConfigControl(this.grpChargeDischargePower, this.systemValues, 15, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGI2642, new String[] {Messages.getString(MessageIds.GDE_MSGI2645).split(",")[2]}), 175, 
				String.format("5W - %dW", device.getDischargePowerMax()[1]), 280, //$NON-NLS-1$
				true, 50, 200, 5, device.getDischargePowerMax()[1], -5, false);
		if (isDx) {
			//36 DX antiSparkEnable
			this.systemParameters[36] = new ParameterConfigControl(this.grpChargeDischargePower, this.systemValues, 36, Messages.getString(MessageIds.GDE_MSGT2678), 
					175, "off, on", 280, //$NON-NLS-1$
					new String[] { "off", "on" }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		}

		grpChargeDischargePower.addListener(SWT.Selection, this.systemParameterChangeListener);
		grpChargeDischargePower.layout();
	}
	
	private void updateSystemParameterControls() {		
		if (isDuo) {
			if (grpInputDischargePowerLimits != null && !grpInputDischargePowerLimits.isDisposed()) {
				grpInputDischargePowerLimits.removeListener(SWT.Selection, systemParameterChangeListener);
				grpInputPowerLimits.removeListener(SWT.Selection, systemParameterChangeListener);
				grpRegInputPowerLimits.removeListener(SWT.Selection, systemParameterChangeListener);
				grpInputDischargePowerLimits.dispose();
			}
			if (grpDuoInputPowerLimits == null || grpDuoInputPowerLimits.isDisposed()) {
				createGrpDuoInputPowerLimits();
			}
			if (grpChargeDischargePower == null || grpChargeDischargePower.isDisposed()) {
				createGrpChargeDischargePower();
			}
		}
		else if (isDx) {
			if (grpDuoInputPowerLimits != null && !grpDuoInputPowerLimits.isDisposed()) {
				grpDuoInputPowerLimits.removeListener(SWT.Selection, systemParameterChangeListener);
				grpDcInputPowerLimits.removeListener(SWT.Selection, systemParameterChangeListener);
				grpBatInputPowerLimits.removeListener(SWT.Selection, systemParameterChangeListener);
				grpDuoRegInputPowerLimits.removeListener(SWT.Selection, systemParameterChangeListener);
				grpDuoInputPowerLimits.dispose();
			}

			if (grpInputDischargePowerLimits == null || grpInputDischargePowerLimits.isDisposed()) {
				createGrpInputDischargePowerLimits();
			}
			if (grpChargeDischargePower == null || grpChargeDischargePower.isDisposed()) {
				createGrpChargeDischargePower();
			}		
		}
		else {
			if (grpDuoInputPowerLimits != null && !grpDuoInputPowerLimits.isDisposed()) {
				grpDuoInputPowerLimits.removeListener(SWT.Selection, systemParameterChangeListener);
				grpDcInputPowerLimits.removeListener(SWT.Selection, systemParameterChangeListener);
				grpBatInputPowerLimits.removeListener(SWT.Selection, systemParameterChangeListener);
				grpDuoRegInputPowerLimits.removeListener(SWT.Selection, systemParameterChangeListener);
				grpDuoInputPowerLimits.dispose();
			}

			if (grpChargeDischargePower != null && !grpChargeDischargePower.isDisposed()) {
				grpChargeDischargePower.removeListener(SWT.Selection, systemParameterChangeListener);
				grpChargeDischargePower.dispose();
			}
			if (grpInputDischargePowerLimits == null || grpInputDischargePowerLimits.isDisposed()) {
				createGrpInputDischargePowerLimits();
			}
		}
		//update parameter controls
		for (int i = 0; i < this.systemParameters.length; i++) {
			if (this.systemParameters[i] != null) {
				this.systemParameters[i].setSliderSelection(this.systemValues[i]);
			}
		}		
	}


	private void createBaseBatteryParameters() {
		//battery type
		this.memoryParameters[0] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 0, Messages.getString(MessageIds.GDE_MSGT2636), 175, this.cellTypeNames, 280,
				this.cellTypeNamesArray, 50, 200);
		this.memoryParameters[0].getSlider().setVisible(false);
		//number cells
		int maxNumberCells = ((iChargerUsb) this.device).getNumberOfLithiumCells();
		this.memoryParameters[1] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 1, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2637), 175, "0(auto) - " + maxNumberCells, //$NON-NLS-1$
				280, false, 50, 200, 0, maxNumberCells);
		//battery capacity
		this.memoryParameters[2] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 2, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2638), 175, "0(auto) ~ 999900 mAh", 280, //$NON-NLS-1$
				true, 50, 200, 0, 999900, 0, true);
		if (this.isDuo || this.isDx) {
			//channel mode asynchronous | synchronous - DUO & DX only
			this.memoryParameters[47] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 47, Messages.getString(MessageIds.GDE_MSGI2649), 175, //$NON-NLS-1$
					"asynchronous, synchronous", 280, //$NON-NLS-1$
					new String[] { "async", "sync" }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		}
		this.memoryComposite.layout();
		this.memoryComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
	}

	private void createChargeTabItem() {
		this.tbtmCharge = new CTabItem(this.tabFolderProgrMem, SWT.NONE);
		this.tbtmCharge.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.tbtmCharge.setText(Messages.getString(MessageIds.GDE_MSGT2639));
		ScrolledComposite scrolledChargeComposite = new ScrolledComposite(this.tabFolderProgrMem, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		this.tbtmCharge.setControl(scrolledChargeComposite);
		scrolledChargeComposite.setExpandHorizontal(true);
		scrolledChargeComposite.setLayout(new FillLayout(SWT.VERTICAL));
		this.chargeComposite = new Composite(scrolledChargeComposite, SWT.NONE);
		this.chargeComposite.setLayout(new RowLayout(SWT.VERTICAL));
		scrolledChargeComposite.setContent(this.chargeComposite);
		scrolledChargeComposite.setMinSize(this.chargeComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		this.chargeComposite.setSize(750, GDE.IS_WINDOWS ? 880 : 1050);
		//charge parameter current
		this.memoryParameters[3] = new ParameterConfigControl(this.chargeComposite, this.memoryValues, 3, "%4.2f", Messages.getString(MessageIds.GDE_MSGT2640), 175, 
				String.format("0.05 ~ %d A", device.getChargeCurrentMax4Channel()/10), 280, //$NON-NLS-1$
				true, 50, 200, 5, device.getChargeCurrentMax4Channel()*10+10, 0, false);
		//charge parameter modus normal,balance,external,reflex
		this.memoryParameters[4] = new ParameterConfigControl(this.chargeComposite, this.memoryValues, 4, Messages.getString(MessageIds.GDE_MSGT2641), 175, String.join(", ", ChargerMemory.LiMode.VALUES), //$NON-NLS-1$
				280, ChargerMemory.LiMode.VALUES, 50, 200);
		//charge parameter balancer modus 
		this.memoryParameters[5] = new ParameterConfigControl(this.chargeComposite, this.memoryValues, 5, Messages.getString(MessageIds.GDE_MSGT2642), 175,
				String.join(", ", ChargerMemory.BalancerLiSetup.VALUES), 280, //$NON-NLS-1$
				ChargerMemory.BalancerLiSetup.VALUES, 50, 200);
		//charge parameter end current, Li balancer end current, PB charge end current
		this.memoryParameters[6] = new ParameterConfigControl(this.chargeComposite, this.memoryValues, 6, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2643), 175, "10% - 50%", 280, //$NON-NLS-1$
				true, 50, 200, 10, 50, -10, false);
		if (this.memoryValues[5] == 0 && this.memoryValues[4] == 0) this.memoryParameters[6].setEnabled(false);
		//charge parameter cell voltage
		this.memoryParameters[7] = new ParameterConfigControl(this.chargeComposite, this.memoryValues, 7, "%4.3f", Messages.getString(MessageIds.GDE_MSGT2644), 175, "4.000 V - 4.200 V", 280, //$NON-NLS-1$
				true, 50, 200, 4000, 4350, 4200, -4000, false);

		//Ni charge voltage drop
		this.memoryParameters[26] = new ParameterConfigControl(this.chargeComposite, this.memoryValues, 26, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2645), 175, "1 mV - 20 mV", 280, //$NON-NLS-1$
				true, 50, 200, 1, 20, -1, false);
		//Ni charge voltage drop delay
		this.memoryParameters[27] = new ParameterConfigControl(this.chargeComposite, this.memoryValues, 27, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2646), 175, "0 Min - 20 Min", 280, //$NON-NLS-1$
				true, 50, 200, 0, 20, 0, false);
		//Ni charge allow 0V
		this.memoryParameters[28] = new ParameterConfigControl(this.chargeComposite, this.memoryValues, 28, Messages.getString(MessageIds.GDE_MSGT2647), 175, Messages.getString(MessageIds.GDE_MSGT2648),
				280, new String[] { Messages.getString(MessageIds.GDE_MSGT2649), Messages.getString(MessageIds.GDE_MSGT2650) }, 50, 200);
		//Ni trickle charge enable
		this.memoryParameters[29] = new ParameterConfigControl(this.chargeComposite, this.memoryValues, 29, Messages.getString(MessageIds.GDE_MSGT2651), 175, "off, on", 280, //$NON-NLS-1$
				new String[] { "off", "on" }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		//Ni charge trickle current
		this.memoryParameters[30] = new ParameterConfigControl(this.chargeComposite, this.memoryValues, 30, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2652), 175, "20mA - 1000mA", 280, //$NON-NLS-1$
				true, 50, 200, 20, 1000, -20, false);
		//Ni charge trickle timeout
		this.memoryParameters[31] = new ParameterConfigControl(this.chargeComposite, this.memoryValues, 31, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2653), 175, "1 Min - 999 Min", 280, //$NON-NLS-1$
				true, 50, 200, 1, 999, -1, false);
		this.chargeComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);

		this.grpChargeSaftySettings = new Group(this.chargeComposite, SWT.NONE);
		this.grpChargeSaftySettings.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.grpChargeSaftySettings.setText(Messages.getString(MessageIds.GDE_MSGT2654));
		this.grpChargeSaftySettings.setBackground(this.application.COLOR_WHITE);
		this.grpChargeSaftySettings.setLayout(new RowLayout(SWT.VERTICAL));
		//charge parameter balancer
		this.memoryParameters[8] = new ParameterConfigControl(this.grpChargeSaftySettings, this.memoryValues, 8, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2655), 175, "20°C - 80°C", 280, //$NON-NLS-1$
				true, 50, 200, 20, 80, -20, false);
		//charge parameter balancer start
		this.memoryParameters[9] = new ParameterConfigControl(this.grpChargeSaftySettings, this.memoryValues, 9, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2656), 175, "50% - 200%", 280, //$NON-NLS-1$
				true, 50, 200, 50, 200, -50, false);
		//charge parameter balancer difference
		this.memoryParameters[10] = new ParameterConfigControl(this.grpChargeSaftySettings, this.memoryValues, 10, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2657), 175, "0(OFF) - 9999 Min", //$NON-NLS-1$
				280, true, 50, 200, 0, 9999, 0, false);
		this.grpChargeSaftySettings.addListener(SWT.Selection, this.memoryParameterChangeListener);

		this.grpBalancerSettings = new Group(this.chargeComposite, SWT.NONE);
		this.grpBalancerSettings.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.grpBalancerSettings.setText(Messages.getString(MessageIds.GDE_MSGT2658));
		this.grpBalancerSettings.setLayout(new RowLayout(SWT.VERTICAL));
		//charge parameter balancer
		this.memoryParameters[11] = new ParameterConfigControl(this.grpBalancerSettings, this.memoryValues, 11, Messages.getString(MessageIds.GDE_MSGT2659), 175,
				String.join(", ", ChargerMemory.BalancerSpeed.VALUES), 280, //$NON-NLS-1$
				ChargerMemory.BalancerSpeed.VALUES, 50, 200);
		//charge parameter balancer start
		this.memoryParameters[12] = new ParameterConfigControl(this.grpBalancerSettings, this.memoryValues, 12, Messages.getString(MessageIds.GDE_MSGT2660), 175,
				String.join(", ", ChargerMemory.BalancerStart.VALUES), 280, //$NON-NLS-1$
				ChargerMemory.BalancerStart.VALUES, 50, 200);
		//charge parameter balancer difference
		this.memoryParameters[13] = new ParameterConfigControl(this.grpBalancerSettings, this.memoryValues, 13, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2661), 175, "1 mV - 10 mV", 280, //$NON-NLS-1$
				true, 50, 200, 1, 10, -1, false);
		//charge parameter balancer target
		this.memoryParameters[14] = new ParameterConfigControl(this.grpBalancerSettings, this.memoryValues, 14, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2662), 175, "1 mV - 50 mV", 280, //$NON-NLS-1$
				true, 50, 200, 1, 50, -1, false);
		//charge parameter balancer over charge
		this.memoryParameters[15] = new ParameterConfigControl(this.grpBalancerSettings, this.memoryValues, 15, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2663), 175, "0 mV - 50 mV", 280, //$NON-NLS-1$
				true, 50, 200, 0, 50, 0, false);
		//charge parameter balancer delay
		this.memoryParameters[16] = new ParameterConfigControl(this.grpBalancerSettings, this.memoryValues, 16, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2664), 175, "0 Min - 20 Min", 280, //$NON-NLS-1$
				true, 50, 200, 0, 20, 0, false);
		this.grpBalancerSettings.addListener(SWT.Selection, this.memoryParameterChangeListener);

		//Li, NiZn, Pb only (NiMH, NiCd has charge at 0V enablement)
		this.grpAdvancedRestoreSettings = new Group(this.chargeComposite, SWT.NONE);
		this.grpAdvancedRestoreSettings.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.grpAdvancedRestoreSettings.setText(Messages.getString(MessageIds.GDE_MSGT2665));
		this.grpAdvancedRestoreSettings.setLayout(new RowLayout(SWT.VERTICAL));
		//charge restore lowest voltage
		this.memoryParameters[32] = new ParameterConfigControl(this.grpAdvancedRestoreSettings, this.memoryValues, 32, "%4.3f", Messages.getString(MessageIds.GDE_MSGT2666), 175,
				"0.500 - 2.500 V", 280, //$NON-NLS-1$
				true, 50, 200, 500, 2500, -500, false);
		//charge restore charge time
		this.memoryParameters[33] = new ParameterConfigControl(this.grpAdvancedRestoreSettings, this.memoryValues, 33, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2667), 175, "1 Min - 5 Min", //$NON-NLS-1$
				280, true, 50, 200, 1, 5, -1, false);
		//charge restore charge current
		this.memoryParameters[34] = new ParameterConfigControl(this.grpAdvancedRestoreSettings, this.memoryValues, 34, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2668), 175, "20 mA - 500 mA", //$NON-NLS-1$
				280, true, 50, 200, 20, 500, -20, false);
		//charge keep charging after done
		this.memoryParameters[35] = new ParameterConfigControl(this.grpAdvancedRestoreSettings, this.memoryValues, 35, Messages.getString(MessageIds.GDE_MSGT2669), 175, "off, on", 280, //$NON-NLS-1$
				new String[] { "off", "on" }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$

		this.grpAdvancedRestoreSettings.addListener(SWT.Selection, this.memoryParameterChangeListener);
		this.chargeComposite.layout();
	}

	private void createDischargeTabItem() {
		this.tbtmDischarge = new CTabItem(this.tabFolderProgrMem, SWT.NONE);
		this.tbtmDischarge.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.tbtmDischarge.setText(Messages.getString(MessageIds.GDE_MSGT2670));
		ScrolledComposite scrolledDischargeComposite = new ScrolledComposite(this.tabFolderProgrMem, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		this.tbtmDischarge.setControl(scrolledDischargeComposite);
		scrolledDischargeComposite.setExpandHorizontal(true);
		scrolledDischargeComposite.setLayout(new FillLayout(SWT.VERTICAL));
		this.dischargeComposite = new Composite(scrolledDischargeComposite, SWT.NONE);
		this.dischargeComposite.setLayout(new RowLayout(SWT.VERTICAL));
		scrolledDischargeComposite.setContent(this.dischargeComposite);
		scrolledDischargeComposite.setMinSize(this.dischargeComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		this.dischargeComposite.setSize(750, GDE.IS_WINDOWS ? 480 : 580);
		//discharge parameter current
		this.memoryParameters[17] = new ParameterConfigControl(this.dischargeComposite, this.memoryValues, 17, "%4.2f", Messages.getString(MessageIds.GDE_MSGT2671), 175, 
				String.format("0.05 ~ %d A", device.getChargeCurrentMax4Channel()/10), 280, //$NON-NLS-1$
				true, 50, 200, 5, device.getChargeCurrentMax4Channel()*10+10, 0, false);
		//discharge parameter cell voltage
		this.memoryParameters[18] = new ParameterConfigControl(this.dischargeComposite, this.memoryValues, 18, "%4.3f", Messages.getString(MessageIds.GDE_MSGT2672), 175, "3.000 - 4.100 V", 280, //$NON-NLS-1$
				true, 50, 200, 3000, 4100, -3000, false);
		//discharge end current
		this.memoryParameters[19] = new ParameterConfigControl(this.dischargeComposite, this.memoryValues, 19, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2673), 175, "1% - 100%", 280, //$NON-NLS-1$
				true, 50, 200, 1, 100, -1, false);
		//discharge extra
		this.memoryParameters[21] = new ParameterConfigControl(this.dischargeComposite, this.memoryValues, 21, Messages.getString(MessageIds.GDE_MSGT2675), 175, Messages.getString(MessageIds.GDE_MSGT2648), 280, //$NON-NLS-1$
				new String[] { Messages.getString(MessageIds.GDE_MSGT2649), Messages.getString(MessageIds.GDE_MSGT2650) }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		//discharge balancer
		this.memoryParameters[22] = new ParameterConfigControl(this.dischargeComposite, this.memoryValues, 22, Messages.getString(MessageIds.GDE_MSGT2676), 175, Messages.getString(MessageIds.GDE_MSGT2648), 280, //$NON-NLS-1$
				new String[] { Messages.getString(MessageIds.GDE_MSGT2649), Messages.getString(MessageIds.GDE_MSGT2650) }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		this.dischargeComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
		//discharge safety settings
		this.grpDischargeSaftySettings = new Group(this.dischargeComposite, SWT.NONE);
		this.grpDischargeSaftySettings.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.grpDischargeSaftySettings.setText(Messages.getString(MessageIds.GDE_MSGT2654));
		this.grpDischargeSaftySettings.setBackground(this.application.COLOR_WHITE);
		this.grpDischargeSaftySettings.setLayout(new RowLayout(SWT.VERTICAL));
		//discharge parameter cut temperature
		this.memoryParameters[23] = new ParameterConfigControl(this.grpDischargeSaftySettings, this.memoryValues, 23, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2655), 175, "20°C - 80°C", //$NON-NLS-1$
				280, true, 50, 200, 20, 80, -20, false);
		//discharge parameter max discharge capacity
		this.memoryParameters[24] = new ParameterConfigControl(this.grpDischargeSaftySettings, this.memoryValues, 24, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2656), 175, "50% - 200%", 280, //$NON-NLS-1$
				true, 50, 200, 50, 200, -50, false);
		//discharge parameter safety timer
		this.memoryParameters[25] = new ParameterConfigControl(this.grpDischargeSaftySettings, this.memoryValues, 25, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2657), 175,
				"0(OFF) - 9999 Min", 280, //$NON-NLS-1$
				true, 50, 200, 0, 9999, 0, false);
		this.grpDischargeSaftySettings.addListener(SWT.Selection, this.memoryParameterChangeListener);
		//20 regenerative mode off, to input, to channel (duo, dx)
		if (isDuo || isDx )
			this.memoryParameters[20] = new ParameterConfigControl(this.dischargeComposite, this.memoryValues, 20, Messages.getString(MessageIds.GDE_MSGT2674), 175, 
					String.format("%s, %s, %s", Messages.getString(MessageIds.GDE_MSGT2649), Messages.getString(MessageIds.GDE_MSGI2652), Messages.getString(MessageIds.GDE_MSGI2653)), 280, 				
					new String[] { Messages.getString(MessageIds.GDE_MSGT2649), Messages.getString(MessageIds.GDE_MSGI2654), Messages.getString(MessageIds.GDE_MSGI2655) }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		else
			this.memoryParameters[20] = new ParameterConfigControl(this.dischargeComposite, this.memoryValues, 20, Messages.getString(MessageIds.GDE_MSGT2674), 175, 
					String.format("%s, %s", Messages.getString(MessageIds.GDE_MSGT2649), Messages.getString(MessageIds.GDE_MSGI2652)), 280, 				
					new String[] { Messages.getString(MessageIds.GDE_MSGT2649), Messages.getString(MessageIds.GDE_MSGI2654) }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		//regenerative to input
		this.regToInputWarningLable = new CLabel(this.dischargeComposite, SWT.NONE);
		this.regToInputWarningLable.setLayoutData(new RowData(750, this.comboHeight));
		this.regToInputWarningLable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.regToInputWarningLable.setText(Messages.getString(MessageIds.GDE_MSGE2602));
		boolean isToInput = this.memoryValues[20] == 1;
		this.regToInputWarningLable.setForeground(isToInput ? SWTResourceManager.getColor(SWT.COLOR_RED) : SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		this.regToInputWarningLable.setEnabled(isToInput);
		//regenerative to channel/output
		this.regToChannelSettings = new Group(this.dischargeComposite, SWT.NONE);
		this.regToChannelSettings.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.regToChannelSettings.setText(Messages.getString(MessageIds.GDE_MSGI2656));
		this.regToChannelSettings.setBackground(this.application.COLOR_WHITE);
		this.regToChannelSettings.setLayout(new RowLayout(SWT.VERTICAL));
		boolean isToChannel = this.memoryValues[20] == 2;
		this.regToChannelSettings.setEnabled(isToChannel);
		//regenerative channel join
		this.memoryParameters[50] = new ParameterConfigControl(this.regToChannelSettings, this.memoryValues, 50, Messages.getString(MessageIds.GDE_MSGI2657), 175, Messages.getString(MessageIds.GDE_MSGI2658), 280, //$NON-NLS-1$
				Messages.getString(MessageIds.GDE_MSGI2658).split(", "), 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		this.memoryParameters[50].setEnabled(isToChannel);
		//regenerative channel voltage limit
		this.memoryParameters[51] = new ParameterConfigControl(this.regToChannelSettings, this.memoryValues, 51, "%.1f", Messages.getString(MessageIds.GDE_MSGI2659), 175, 
				String.format("%.1fV - %.1fV", this.device.getRegChannelVoltageLimits()[0]/1000., this.device.getRegChannelVoltageLimits()[1]/1000.), 280, //$NON-NLS-1$
				true, 50, 200, this.device.getRegChannelVoltageLimits()[0]/100, this.device.getRegChannelVoltageLimits()[1]/100, 0, false);
		this.memoryParameters[51].setEnabled(isToChannel);
		//regenerative channel current limit
		this.memoryParameters[52] = new ParameterConfigControl(this.regToChannelSettings, this.memoryValues, 52, "%.2f", Messages.getString(MessageIds.GDE_MSGI2660), 175,
				String.format("%.2fA - %.2fA", this.device.getRegChannelCurrentLimits()[0]/100., this.device.getRegChannelCurrentLimits()[1]/100.), 280, //$NON-NLS-1$
				true, 50, 200, this.device.getRegChannelCurrentLimits()[0], this.device.getRegChannelCurrentLimits()[1], 0, false);
		this.memoryParameters[52].setEnabled(isToChannel);
		this.regToChannelSettings.addListener(SWT.Selection, this.memoryParameterChangeListener);
	}

	private void createStorageTabItem() {
		this.tbtmStorage = new CTabItem(this.tabFolderProgrMem, SWT.NONE);
		this.tbtmStorage.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.tbtmStorage.setText(Messages.getString(MessageIds.GDE_MSGT2695));
		this.storageComposite = new Composite(this.tabFolderProgrMem, SWT.NONE);
		this.tbtmStorage.setControl(this.storageComposite);
		this.storageComposite.setLayout(new RowLayout(SWT.VERTICAL));
		//storage parameter cell voltage (Li battery type only)
		this.memoryParameters[36] = new ParameterConfigControl(this.storageComposite, this.memoryValues, 36, "%4.3f", Messages.getString(MessageIds.GDE_MSGT2696), 175, "3.700 - 3.900 V", 280, //$NON-NLS-1$
				true, 50, 200, 3700, 3900, -3700, false);
		//storage parameter compensation
		this.memoryParameters[37] = new ParameterConfigControl(this.storageComposite, this.memoryValues, 37, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2697), 175, "0mV - 200mV", 280, //$NON-NLS-1$
				true, 50, 200, 0, 200, 0, false);
		//storage acceleration
		this.memoryParameters[38] = new ParameterConfigControl(this.storageComposite, this.memoryValues, 38, Messages.getString(MessageIds.GDE_MSGT2698), 175, 
				Messages.getString(MessageIds.GDE_MSGT2648), 280, //$NON-NLS-1$
				new String[] { Messages.getString(MessageIds.GDE_MSGT2649), Messages.getString(MessageIds.GDE_MSGT2650) }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		this.storageComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
	}

	private void createCycleTabItem() {
		this.tbtmCycle = new CTabItem(this.tabFolderProgrMem, SWT.NONE);
		this.tbtmCycle.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.tbtmCycle.setText(Messages.getString(MessageIds.GDE_MSGT2681));
		this.cycleComposite = new Composite(this.tabFolderProgrMem, SWT.NONE);
		this.tbtmCycle.setControl(this.cycleComposite);
		this.cycleComposite.setLayout(new RowLayout(SWT.VERTICAL));
		//cycle parameter mode CHG->DCHG
		this.memoryParameters[39] = new ParameterConfigControl(this.cycleComposite, this.memoryValues, 39, Messages.getString(MessageIds.GDE_MSGT2682), 175,
				String.join(", ", ChargerMemory.CycleMode.VALUES), 280, //$NON-NLS-1$
				ChargerMemory.CycleMode.VALUES, 50, 200);
		//cycle count
		this.memoryParameters[40] = new ParameterConfigControl(this.cycleComposite, this.memoryValues, 40, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2683), 175, "1 - 99", 280, //$NON-NLS-1$
				true, 50, 200, 1, 99, -1, false);
		//cycle delay
		this.memoryParameters[41] = new ParameterConfigControl(this.cycleComposite, this.memoryValues, 41, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2684), 175, "1 Min - 999 Min", 280, //$NON-NLS-1$
				true, 50, 200, 1, 999, -1, false);
		this.cycleComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
	}

	private void createPowerTabItem() {
		this.tbtmPower = new CTabItem(this.tabFolderProgrMem, SWT.NONE);
		this.tbtmPower.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.tbtmPower.setText(Messages.getString(MessageIds.GDE_MSGT2677));
		this.powerComposite = new Composite(this.tabFolderProgrMem, SWT.NONE);
		this.tbtmPower.setControl(this.powerComposite);
		this.powerComposite.setLayout(new RowLayout(SWT.VERTICAL));
		//power voltage
		this.memoryParameters[42] = new ParameterConfigControl(this.powerComposite, this.memoryValues, 42, "%3.1f", //$NON-NLS-1$
				"Voltage", 175, //$NON-NLS-1$
				String.format("2.0 ~ %.1f V", device.getDigitalPowerVoltageMax()/10.), 280, //$NON-NLS-1$
				true, 50, 200, 20, device.getDigitalPowerVoltageMax(), -20, false);
		//power current
		this.memoryParameters[43] = new ParameterConfigControl(this.powerComposite, this.memoryValues, 43, "%3.1f", //$NON-NLS-1$
				"Current", 175, //$NON-NLS-1$
				String.format("1.0 ~ %.1f A", device.getChargeCurrentMax4Channel()/10.), 280, //$NON-NLS-1$
				true, 50, 200, 10, device.getChargeCurrentMax4Channel(), -10, false);
		//power option lock
		this.memoryParameters[44] = new ParameterConfigControl(this.powerComposite, this.memoryValues, 44, "Lock", 175, //$NON-NLS-1$
				"off, on", 280, //$NON-NLS-1$
				new String[] { "off", "on" }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		//power option auto start
		this.memoryParameters[45] = new ParameterConfigControl(this.powerComposite, this.memoryValues, 45, "Auto start", 175, //$NON-NLS-1$
				"off, on", 280, //$NON-NLS-1$
				new String[] { "off", "on" }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		//power option live update
		this.memoryParameters[46] = new ParameterConfigControl(this.powerComposite, this.memoryValues, 46, "Live update", 175, //$NON-NLS-1$
				"off, on", 280, //$NON-NLS-1$
				new String[] { "off", "on" }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		this.powerComposite.layout();
		this.powerComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
	}

	private void createOptionTabItem() {
		this.tbtmOption = new CTabItem(this.tabFolderProgrMem, SWT.NONE);
		this.tbtmOption.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.tbtmOption.setText(Messages.getString(MessageIds.GDE_MSGI2648)); //$NON-NLS-1$
		this.optionComposite = new Composite(this.tabFolderProgrMem, SWT.NONE);
		this.tbtmOption.setControl(this.optionComposite);
		this.optionComposite.setLayout(new RowLayout(SWT.VERTICAL));
		//log interval
		this.memoryParameters[48] = new ParameterConfigControl(this.optionComposite, this.memoryValues, 48, "%3.1f", //$NON-NLS-1$
				Messages.getString(MessageIds.GDE_MSGI2650), 175, //$NON-NLS-1$
				"0.5 - 60.0 Sec", 280, //$NON-NLS-1$
				true, 50, 200, 5, 600, -5, false);
		//power option auto start 
		this.memoryParameters[49] = new ParameterConfigControl(this.optionComposite, this.memoryValues, 49, Messages.getString(MessageIds.GDE_MSGI2651), 175, //$NON-NLS-1$
				 Messages.getString(MessageIds.GDE_MSGT2648), 280, //$NON-NLS-1$
				 new String[] { Messages.getString(MessageIds.GDE_MSGT2649), Messages.getString(MessageIds.GDE_MSGT2650) }, 50, 200); //$NON-NLS-1$ //$NON-NLS-2$
		this.optionComposite.layout();
		this.optionComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
	}

	private void updateMemoryParameterControls() {

		try {
			if ((this.isDuo && this.memoryValues[0] < 8) || this.memoryValues[0] < 9) { // X devices up to Power
				int maxNumberCells = this.cellTypeNamesArray[this.memoryValues[0]].startsWith("L") 
						? ((iChargerUsb) this.device).getNumberOfLithiumCells() //$NON-NLS-1$
						: this.cellTypeNamesArray[this.memoryValues[0]].startsWith("N") ? ChargerMemory.getMaxCellsNi(this.device) //$NON-NLS-1$
								: this.cellTypeNamesArray[this.memoryValues[0]].startsWith("P") ? ChargerMemory.getMaxCellsPb(this.device) : 0; //$NON-NLS-1$

				if (this.tbtmPower != null && !this.tbtmPower.isDisposed()) {
					this.powerComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
					for (int i = 42; i < 47; ++i)
						this.memoryParameters[i].dispose();
					this.powerComposite.dispose();
					this.tbtmPower.dispose();
				}
				if (this.powerLabel != null && !this.powerLabel.isDisposed()) {
					this.powerLabel.dispose();
					createBaseBatteryParameters();
				}
				if (this.tbtmCharge == null || this.tbtmCharge.isDisposed()) {
					createChargeTabItem();
					this.grpRunProgram.setEnabled(true);
					this.tabFolderProgrMem.setSelection(0);
				}
				if (this.tbtmDischarge == null || this.tbtmDischarge.isDisposed()) createDischargeTabItem();
				if (this.tbtmCycle == null || this.tbtmCycle.isDisposed()) createCycleTabItem();
				if (this.tbtmOption == null || this.tbtmOption.isDisposed()) createOptionTabItem();

				this.memoryParameters[1].updateValueRange("0(auto) - " + maxNumberCells, 0, maxNumberCells, 0); //$NON-NLS-1$
				//battery type dependent updates
				this.memoryParameters[5].setEnabled(true);
				this.memoryParameters[6].setEnabled(this.memoryValues[5] != 0 || this.memoryValues[4] != 0);
				this.memoryParameters[6].updateValueRange("10% - 50%", 10, 50, 50, -10); //$NON-NLS-1$
				this.memoryParameters[7].setEnabled(true);
				this.memoryParameters[7].getSlider().setEnabled(true);
				this.grpBalancerSettings.setEnabled(true);
				for (int i = 11; i < 17; ++i)
					this.memoryParameters[i].setEnabled(true);
				if (this.isDuo) {
					switch (this.memoryValues[0]) { //battery type LiPo,LiLo,LiFe,NiMH,Nicd,Pb,NiZn,LiHV
					case 0: //LiPo
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.LiMode.VALUES);
						this.memoryParameters[7].updateValueRange("4.2V (3.850 - 4.350V)", 3850, 4350, 4200, -3850); //$NON-NLS-1$
						break;
					case 1: //LiIo
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.LiMode.VALUES);
						this.memoryParameters[7].updateValueRange("4.1V (3.750 - 4.350V)", 3750, 4350, 4100, -3750); //$NON-NLS-1$
						break;
					case 2: //LiFe
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.LiMode.VALUES);
						this.memoryParameters[7].updateValueRange("3.3V (3.300 - 3.800 V", 3300, 3800, 3600, -3300); //$NON-NLS-1$
						break;
					case 6: //NiZn
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.NiMode.VALUES);
						this.memoryParameters[7].updateValueRange("1.9V (1.200 - 2.100 V", 1200, 2100, 2000, -1200); //$NON-NLS-1$
						break;
					case 7: //LiHV
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.LiMode.VALUES);
						this.memoryParameters[7].updateValueRange("4.35V (3.900 - 4.450V)", 3900, 4450, 4350, -3900); //$NON-NLS-1$
						break;
					case 3: //NiMH
					case 4: //NiCd
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.NiMode.VALUES);
						this.memoryParameters[5].setEnabled(false);
						this.memoryParameters[7].setEnabled(false);
						this.grpBalancerSettings.setEnabled(false);
						for (int i = 11; i < 17; ++i)
							this.memoryParameters[i].setEnabled(false);
						break;
					case 5: //Pb
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.PbMode.VALUES);
						this.memoryParameters[5].setEnabled(false);
						this.memoryParameters[6].setEnabled(true);
						this.memoryParameters[6].updateValueRange("1% - 50%", 1, 50, 50, -1); //$NON-NLS-1$
						this.memoryParameters[7].updateValueRange("2.4V (2.2 - 2.6V)", ChargerMemory.PB_CHG_MIN, ChargerMemory.PB_CHG_MAX, ChargerMemory.PB_CHG_DEFAULT, -1*ChargerMemory.PB_CHG_MIN); //$NON-NLS-1$
						this.grpBalancerSettings.setEnabled(false);
						for (int i = 11; i < 17; ++i)
							this.memoryParameters[i].setEnabled(false);
						break;
					default: //unknown
						this.memoryParameters[7].setEnabled(false);
						this.grpBalancerSettings.setEnabled(false);
						break;
					}
				}
				else {
					switch (this.memoryValues[0]) { //battery type LiPo,LiLo,LiFe,LiHV,LTO, NiMH,Nicd,NiZn,Pb
					case 0: //LiPo
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.LiMode.VALUES);
						this.memoryParameters[7].updateValueRange("4.2V (3.850 - 4.350V)", 3850, 4350, 4200, -3850); //$NON-NLS-1$
						break;
					case 1: //LiIo
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.LiMode.VALUES);
						this.memoryParameters[7].updateValueRange("4.1V (3.750 - 4.350V)", 3750, 4350, 4100, -3750); //$NON-NLS-1$
						break;
					case 2: //LiFe
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.LiMode.VALUES);
						this.memoryParameters[7].updateValueRange("3.3V (3.300 - 3.800 V", 3300, 3800, 3600, -3300); //$NON-NLS-1$
						break;
					case 3: //LiHV
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.LiMode.VALUES);
						this.memoryParameters[7].updateValueRange("4.35V (3.900 - 4.450V)", 3900, 4450, 4350, -3900); //$NON-NLS-1$
						break;
					case 4: //LTO
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.LiMode.VALUES);
						this.memoryParameters[7].updateValueRange("2.85V (2.400 - 3.100 V", 2400, 3100, 2850, -2400); //$NON-NLS-1$
						break;
					case 7: //NiZn
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.NiMode.VALUES);
						this.memoryParameters[7].updateValueRange("1.9V (1.200 - 2.100 V", 1200, 2100, 2000, -1200); //$NON-NLS-1$
						break;
					case 5: //NiMH
					case 6: //NiCd
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.NiMode.VALUES);
						this.memoryParameters[5].setEnabled(false);
						this.memoryParameters[7].setEnabled(false);
						this.grpBalancerSettings.setEnabled(false);
						for (int i = 11; i < 17; ++i)
							this.memoryParameters[i].setEnabled(false);
						break;
					case 8: //Pb
						this.memoryParameters[4].updateTextFieldValues(ChargerMemory.PbMode.VALUES);
						this.memoryParameters[5].setEnabled(false);
						this.memoryParameters[6].setEnabled(true);
						this.memoryParameters[6].updateValueRange("1% - 50%", 1, 50, 50, -1); //$NON-NLS-1$
						this.memoryParameters[7].updateValueRange("2.4V (2.200 - 2.600V)", 2000, 2600, 2400, -2000); //$NON-NLS-1$
						this.grpBalancerSettings.setEnabled(false);
						for (int i = 11; i < 17; ++i)
							this.memoryParameters[i].setEnabled(false);
						break;
					default: //Power
						this.memoryParameters[7].setEnabled(false);
						this.grpBalancerSettings.setEnabled(false);
						for (int i = 11; i < 17; ++i)
							this.memoryParameters[i].setEnabled(false);
					}
				}
				this.memoryParameters[13].setEnabled(this.memoryValues[11] == 3);
				this.memoryParameters[14].setEnabled(this.memoryValues[11] == 3);
				this.memoryParameters[15].setEnabled(this.memoryValues[11] == 3);
				this.memoryParameters[16].setEnabled(this.memoryValues[11] == 3);

				//18 discharge parameter cell voltage is battery type dependent
				//21 discharge extra is battery type dependent
				//22 discharge balancer is battery type dependent
				this.memoryParameters[18].updateNameLabel(Messages.getString(MessageIds.GDE_MSGT2672));
				if (this.isDuo) {
					switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
					case 0: //LiPo
						this.memoryParameters[18].updateValueRange("3.5V (3.000 - 4.100V)", 3000, 4100, -3000); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(true);
						this.memoryParameters[22].setEnabled(true);
						break;
					case 1: //LiIo
						this.memoryParameters[18].updateValueRange("3.5V (2.500 - 4.000V)", 2500, 4000, -2500); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(true);
						this.memoryParameters[22].setEnabled(true);
						break;
					case 2: //LiFe
						this.memoryParameters[18].updateValueRange("2.5V (2.000 - 3.500V)", 2000, 3500, -2000); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(true);
						this.memoryParameters[22].setEnabled(true);
						break;
					case 3: //NiMH
					case 4: //NiCd
						this.memoryParameters[18].updateNameLabel(Messages.getString(MessageIds.GDE_MSGT2619));
						this.memoryParameters[18].updateValueRange("0.100 - 35.000V", 100, 35000, -100); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(false);
						this.memoryParameters[22].setEnabled(false);
						break;
					case 5: //Pb
						this.memoryParameters[18].updateValueRange("1.8V (1.500 - 2.400V)", 1500, 2400, -1500); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(false);
						this.memoryParameters[22].setEnabled(false);
						break;
					case 6: //NiZn
						this.memoryParameters[18].updateValueRange("1.1V (0.900 - 1.600V)", 900, 1600, -900); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(true);
						this.memoryParameters[22].setEnabled(true);
						break;
					case 7: //LiHV
						this.memoryParameters[18].updateValueRange("3.6V (3.000 - 4.250V)", 3000, 4250, -3000); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(true);
						this.memoryParameters[22].setEnabled(true);
						break;
					default:
						break;
					}
				}
				else {
					switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
					case 0: //LiPo
						this.memoryParameters[18].updateValueRange("3.5V (3.000 - 4.100V)", 3000, 4100, -3000); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(true);
						this.memoryParameters[22].setEnabled(true);
						break;
					case 1: //LiIo
						this.memoryParameters[18].updateValueRange("3.5V (2.500 - 4.000V)", 2500, 4000, -2500); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(true);
						this.memoryParameters[22].setEnabled(true);
						break;
					case 2: //LiFe
						this.memoryParameters[18].updateValueRange("2.5V (2.000 - 3.500V)", 2000, 3500, -2000); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(true);
						this.memoryParameters[22].setEnabled(true);
						break;
					case 3: //LiHV
						this.memoryParameters[18].updateValueRange("3.6V (3.000 - 4.250V)", 3000, 4250, -3000); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(true);
						this.memoryParameters[22].setEnabled(true);
						break;
					case 4: //LTO
						this.memoryParameters[18].updateValueRange("1.8V (1.500 - 2.900V)", 1500, 2900, -1500); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(true);
						this.memoryParameters[22].setEnabled(true);
						break;
					case 5: //NiMH
					case 6: //NiCd
						this.memoryParameters[18].updateNameLabel(Messages.getString(MessageIds.GDE_MSGT2619));
						this.memoryParameters[18].updateValueRange("0.100 - 35.000 V", 100, 35000, -100); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(false);
						this.memoryParameters[22].setEnabled(false);
						break;
					case 7: //NiZn
						this.memoryParameters[18].updateValueRange("1.1V (0.900 - 1.600V)", 900, 1600, -900); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(true);
						this.memoryParameters[22].setEnabled(true);
						break;
					case 8: //Pb
						this.memoryParameters[18].updateValueRange("1.8V (1.500 - 2.400V)", 1500, 2400, -1500); //$NON-NLS-1$
						this.memoryParameters[21].setEnabled(false);
						this.memoryParameters[22].setEnabled(false);
						break;
					default:
						break;
					}
				}
				//23 discharge parameter cut temperature
				//24 discharge parameter max discharge capacity
				//25 discharge parameter safety timer

				//26 Ni charge voltage drop
				//27 Ni charge voltage drop delay
				//28 Ni charge allow 0V
				//29 Ni trickle charge enable
				//30 Ni charge trickle current
				//31 Ni charge trickle timeout
				if (this.isDuo) {
					switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
					case 0: //LiPo
					case 1: //LiIo
					case 2: //LiFe
					case 5: //Pb
					case 6: //NiZn
					case 7: //LiHV
					default:
						for (int i = 26; i < 32; ++i)
							this.memoryParameters[i].setEnabled(false);
						break;
					case 3: //NiMH
					case 4: //NiCd
						for (int i = 26; i < 32; ++i)
							this.memoryParameters[i].setEnabled(true);
						break;
					}
				}
				else {
					switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
					case 0: //LiPo
					case 1: //LiIo
					case 2: //LiFe
					case 3: //LiHV
					case 4: //LTO
					case 7: //NiZn
					case 8: //Pb
					default:
						for (int i = 26; i < 32; ++i)
							this.memoryParameters[i].setEnabled(false);
						break;
					case 5: //NiMH
					case 6: //NiCd
						for (int i = 26; i < 32; ++i)
							this.memoryParameters[i].setEnabled(true);
						break;
					}
				}

				//Li, NiZn, Pb only (NiMH, NiCd has charge at 0V enable)
				//32 charge restore lowest voltage
				//33 charge restore charge time
				//34 charge restore charge current
				//35 charge keep charging after done
				if (this.isDuo) {
					switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
					case 0: //LiPo
					case 1: //LiIo
					case 2: //LiFe
					case 5: //Pb
					case 6: //NiZn
					case 7: //LiHV
					default:
						for (int i = 32; i < 36; ++i)
							this.memoryParameters[i].setEnabled(true);
						break;
					case 3: //NiMH
					case 4: //NiCd
						for (int i = 32; i < 36; ++i)
							this.memoryParameters[i].setEnabled(false);
						break;
					}
				}
				else {
					switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
					case 0: //LiPo
					case 1: //LiIo
					case 2: //LiFe
					case 3: //LiHV
					case 4: //LTO
					case 7: //NiZn
					case 8: //Pb
					default:
						for (int i = 32; i < 36; ++i)
							this.memoryParameters[i].setEnabled(true);
						break;
					case 5: //NiMH
					case 6: //NiCd
						for (int i = 32; i < 36; ++i)
							this.memoryParameters[i].setEnabled(false);
						break;
					}
				}

				//36 storage parameter cell voltage (Li battery type only)
				//37 storage parameter compensation
				//38 storage acceleration
				if (this.isDuo) {
					switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
					case 0: //LiPo
					case 1: //LiIo
					case 2: //LiFe
					case 7: //LiHV
					default:
						if (this.tbtmStorage == null || this.tbtmStorage.isDisposed()) {
							createStorageTabItem();
						}
						switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe
						case 0: //LiPo
							this.memoryParameters[36].updateValueRange("3.85V (3.700 - 3.900V)", 3700, 3900, -3700); //$NON-NLS-1$
							break;
						case 1: //LiIo
							this.memoryParameters[36].updateValueRange("3.75V (3.600 - 3.800V)", 3600, 3800, -3600); //$NON-NLS-1$
							break;
						case 2: //LiFe
							this.memoryParameters[36].updateValueRange("3.3V (3.100 - 3.400V)", 3100, 3400, -3100); //$NON-NLS-1$
							break;
						case 7: //LiHV
							this.memoryParameters[36].updateValueRange("3.9V (3.750 - 4.100V)", 3750, 4100, -3750); //$NON-NLS-1$
							break;
						default:
							break;
						}
						break;
					case 3: //NiMH
					case 4: //NiCd
					case 5: //Pb
					case 6: //NiZn
						if (this.tbtmStorage != null && !this.tbtmStorage.isDisposed()) {
							this.storageComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
							for (int i = 36; i < 39; ++i)
								this.memoryParameters[i].dispose();
							this.storageComposite.dispose();
							this.tbtmStorage.dispose();
						}
						break;
					}
				}
				else {
					switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
					case 0: //LiPo
					case 1: //LiIo
					case 2: //LiFe
					case 3: //LiHV
					case 4: //LTO
						if (this.tbtmStorage == null || this.tbtmStorage.isDisposed()) {
							createStorageTabItem();
						}
						switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe
						case 0: //LiPo
							this.memoryParameters[36].updateValueRange("3.85V (3.700 - 3.900V)", 3700, 3900, -3700); //$NON-NLS-1$
							break;
						case 1: //LiIo
							this.memoryParameters[36].updateValueRange("3.75V (3.600 - 3.800V)", 3600, 3800, -3600); //$NON-NLS-1$
							break;
						case 2: //LiFe
							this.memoryParameters[36].updateValueRange("3.3V (3.100 - 3.400V)", 3100, 3400, -3100); //$NON-NLS-1$
							break;
						case 3: //LiHV
							this.memoryParameters[36].updateValueRange("3.9V (3.750 - 4.100V)", 3750, 4100, -3750); //$NON-NLS-1$
							break;
						case 4: //LTO
							this.memoryParameters[36].updateValueRange("2.5V (2.200 - 2.600V)", 2200, 2600, -2200); //$NON-NLS-1$
							break;
						default:
							break;
						}
						break;
					default:
					case 5: //NiMH
					case 6: //NiCd
					case 7: //NiZn
					case 8: //Pb
						if (this.tbtmStorage != null && !this.tbtmStorage.isDisposed()) {
							this.storageComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
							for (int i = 36; i < 39; ++i)
								this.memoryParameters[i].dispose();
							this.storageComposite.dispose();
							this.tbtmStorage.dispose();
						}
						break;
					}
				}
				if (isDuo || isDx) {//47 channel mode asynchronous | synchronous DUO/DX only
					if (this.memoryParameters[47] != null) {
						this.memoryParameters[47].setEnabled(true);
						if (this.regToInputWarningLable != null) {
							boolean isToInput = this.memoryValues[20] == 1;
							this.regToInputWarningLable.setEnabled(isToInput);
							this.regToInputWarningLable.setForeground(isToInput ? SWTResourceManager.getColor(SWT.COLOR_RED) : SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
						}
						if (this.regToChannelSettings != null) {
							boolean isToChannel = this.memoryValues[20] == 2;
							this.regToChannelSettings.setEnabled(isToChannel);
							this.memoryParameters[50].setEnabled(isToChannel);
							boolean isResOrBulb = this.memoryValues[50] == 0;
							this.memoryParameters[51].setEnabled(isToChannel && isResOrBulb);
							this.memoryParameters[52].setEnabled(isToChannel && isResOrBulb);
						}
						
						//update charge/discharge max current channel async/sync
						if (this.memoryValues[47] == 1) {//single channel or channels async
							this.memoryParameters[3].updateValueRange(String.format("0.05 ~ %d A", device.getChargeCurrentMaxSyncChannels()/10), 5, device.getChargeCurrentMaxSyncChannels()*10+10); //$NON-NLS-1$
							this.memoryParameters[17].updateValueRange(String.format("0.05 ~ %d A", device.getChargeCurrentMaxSyncChannels()/10), 5, device.getChargeCurrentMaxSyncChannels()*10+10); //$NON-NLS-1$
						}
						else {//duo device channels in sync
							this.memoryParameters[3].updateValueRange(String.format("0.05 ~ %d A", device.getChargeCurrentMax4Channel()/10), 5, device.getChargeCurrentMax4Channel()*10+10); //$NON-NLS-1$
							this.memoryParameters[17].updateValueRange(String.format("0.05 ~ %d A", device.getChargeCurrentMax4Channel()/10), 5, device.getChargeCurrentMax4Channel()*10+10); //$NON-NLS-1$
						}
					}
				}
				else {
					if (this.memoryParameters[47] != null)
						this.memoryParameters[47].setEnabled(false);
					if (this.regToInputWarningLable != null) {
						boolean isToInput = this.memoryValues[20] == 1;
						this.regToInputWarningLable.setEnabled(isToInput);
						this.regToInputWarningLable.setForeground(isToInput ? SWTResourceManager.getColor(SWT.COLOR_RED) : SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
					}
					if (this.regToChannelSettings != null) {
						boolean isToChannel = false;
						this.regToChannelSettings.setEnabled(isToChannel);
						this.memoryParameters[50].setEnabled(isToChannel);
						this.memoryParameters[51].setEnabled(isToChannel);
						this.memoryParameters[52].setEnabled(isToChannel);
					}
				}
			}
			else if (!this.isDuo && this.memoryValues[0] == 9) { // X devices power type memory
				this.memoryComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
				for (int i = 0; i < 42; ++i)
					if (this.memoryParameters[i] != null) this.memoryParameters[i].dispose();

				if (this.powerLabel == null || this.powerLabel.isDisposed()) {
					this.powerLabel = new CLabel(this.memoryComposite, SWT.CENTER);
					this.powerLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 15, SWT.BOLD));
					this.powerLabel.setBackground(this.application.COLOR_CANVAS_YELLOW);
					this.powerLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.powerLabel.setText(Messages.getString(MessageIds.GDE_MSGT2677));
					this.memoryComposite.layout();
				}

				if (this.tbtmCharge != null && !this.tbtmCharge.isDisposed()) {
					this.grpBalancerSettings.removeListener(SWT.Selection, this.memoryParameterChangeListener);
					this.grpBalancerSettings.dispose();
					this.grpAdvancedRestoreSettings.removeListener(SWT.Selection, this.memoryParameterChangeListener);
					this.grpAdvancedRestoreSettings.dispose();
					this.grpChargeSaftySettings.removeListener(SWT.Selection, this.memoryParameterChangeListener);
					this.grpChargeSaftySettings.dispose();
					this.chargeComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
					this.chargeComposite.dispose();
					this.tbtmCharge.dispose();
				}
				if (this.tbtmDischarge != null && !this.tbtmDischarge.isDisposed()) {
					this.dischargeComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
					this.dischargeComposite.dispose();
					this.tbtmDischarge.dispose();
				}
				if (this.tbtmStorage != null && !this.tbtmStorage.isDisposed()) {
					this.storageComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
					this.storageComposite.dispose();
					this.tbtmStorage.dispose();
				}
				if (this.tbtmCycle != null && !this.tbtmCycle.isDisposed()) {
					this.cycleComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
					this.cycleComposite.dispose();
					this.tbtmCycle.dispose();
				}
				if (this.tbtmOption != null && !this.tbtmOption.isDisposed()) {
					this.optionComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
					this.optionComposite.dispose();
					this.tbtmOption.dispose();
				}
				if (this.tbtmPower == null || this.tbtmPower.isDisposed()) {
					createPowerTabItem();
					this.tabFolderProgrMem.setSelection(0);
					if (this.memoryParameters[47] != null)
						this.memoryParameters[47].dispose();
				}
			}

			//update parameter controls
			for (int i = 0; i < this.memoryParameters.length; i++) {
				if (this.memoryParameters[i] != null) {
					this.memoryParameters[i].setSliderSelection(this.memoryValues[i]);
				}
			}
		}
		catch (RuntimeException e) {
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialogAsync(ChargerDialog.this.getDialogShell(), e.getMessage());
		}
	}

	void addAllListeners() {
		if (this.storageComposite != null && !this.storageComposite.isDisposed()) this.storageComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.memoryComposite != null && !this.memoryComposite.isDisposed()) this.memoryComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.chargeComposite != null && !this.chargeComposite.isDisposed()) this.chargeComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.grpChargeSaftySettings != null && !this.grpChargeSaftySettings.isDisposed()) this.grpChargeSaftySettings.addListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.grpBalancerSettings != null && !this.grpBalancerSettings.isDisposed()) this.grpBalancerSettings.addListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.grpAdvancedRestoreSettings != null && !this.grpAdvancedRestoreSettings.isDisposed()) this.grpAdvancedRestoreSettings.addListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.dischargeComposite != null && !this.dischargeComposite.isDisposed()) this.dischargeComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.powerComposite != null && !this.powerComposite.isDisposed()) this.powerComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
	}

	void removeAllListeners() {
		if (this.storageComposite != null && !this.storageComposite.isDisposed()) this.storageComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.memoryComposite != null && !this.memoryComposite.isDisposed()) this.memoryComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.chargeComposite != null && !this.chargeComposite.isDisposed()) this.chargeComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.grpChargeSaftySettings != null && !this.grpChargeSaftySettings.isDisposed()) this.grpChargeSaftySettings.removeListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.grpBalancerSettings != null && !this.grpBalancerSettings.isDisposed()) this.grpBalancerSettings.removeListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.grpAdvancedRestoreSettings != null && !this.grpAdvancedRestoreSettings.isDisposed()) this.grpAdvancedRestoreSettings.removeListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.dischargeComposite != null && !this.dischargeComposite.isDisposed()) this.dischargeComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
		if (this.powerComposite != null && !this.powerComposite.isDisposed()) this.powerComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
	}
}
