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

import gde.device.IDevice;
import gde.device.junsi.MessageIds;
import gde.device.junsi.iCharger308DUO;
import gde.device.junsi.iCharger4010DUO;
import gde.device.junsi.iCharger406DUO;
import gde.device.junsi.iCharger4512DUO;
import gde.device.junsi.iCharger456DUO;
import gde.device.junsi.iCharger458DUO;
import gde.device.junsi.iChargerDX12;
import gde.device.junsi.iChargerDX6;
import gde.device.junsi.iChargerDX8;
import gde.device.junsi.iChargerS6;
import gde.device.junsi.iChargerUsb.BatteryTypesDuo;
import gde.device.junsi.iChargerX12;
import gde.device.junsi.iChargerX6;
import gde.device.junsi.iChargerX6.BatteryTypesX;
import gde.device.junsi.iChargerX8;
import gde.io.DataParser;
import gde.messages.Messages;
import gde.utils.StringHelper;

public class ChargerMemory {
	
	public enum BalancerStart {
		BAL_START_CV("CV"), BAL_START_CV_100("CV-0.1V"), BAL_START_CV_200("CV-0.2V"), BAL_START_CV_300("CV-0.3V"), BAL_START_CV_400("CV-0.4V"), BAL_START_CV_500("CV-0.5V"), 
		BAL_START_CV_600("CV-0.6V"), BAL_START_CV_700("CV-0.7V"), BAL_START_CV_800("CV-0.8V"), BAL_START_CV_900("CV-0.9V"), BAL_START_CV_1000("CV-1.0V"), BAL_START_ALWAY("Always");

		String value;

		private BalancerStart(String setValue) {
			this.value = setValue;
		}

		public static String[] VALUES = getValues();

		private static String[] getValues() {
			List<String> list = new ArrayList<String>();
			for (BalancerStart element : values()) {
				list.add(element.value);
			}
			return list.toArray(new String[0]);
		}
	};

	public enum BalancerLiSetup {
		END_CURRENT_OFF_BAL_DETECT_ON("OFF-ON"), END_CURRENT_ON_BAL_DETECT_OFF("ON-OFF"), END_CURRENT_OR_BAL_DETECT("OR"), END_CURRENT_AND_BAL_DETECT("AND");
		String value;

		private BalancerLiSetup(String setValue) {
			this.value = setValue;
		}

		public static String[] VALUES = getValues();

		private static String[] getValues() {
			List<String> list = new ArrayList<String>();
			for (BalancerLiSetup element : values()) {
				list.add(element.value);
			}
			return list.toArray(new String[0]);
		}
	}
	
	public enum BalancerSpeed {
		BAL_SPEED_SLOW("slow"), BAL_SPEED_NORMAL("normal"), BAL_SPEED_FAST("fast"), BAL_SPEED_USER("user");
		
		String value;
		private BalancerSpeed(String setValue) {
			this.value = setValue; 
		}
		public static String[] VALUES = getValues();

		private static String[] getValues() {
			List<String> list = new ArrayList<String>();
			for (BalancerSpeed element : values()) {
				list.add(element.value);
			}
			return list.toArray(new String[0]);
		}
	};

	public enum LiMode {
		LI_MODE_C_BAL("balancer"), 
		LI_MODE_C_NOTBAL("off");
		
		String value;
		private LiMode(String setValue) {
			this.value = setValue; 
		}
		public static String[] VALUES = getValues();

		private static String[] getValues() {
			List<String> list = new ArrayList<String>();
			for (LiMode element : values()) {
				list.add(element.value);
			}
			return list.toArray(new String[0]);
		}
	};

	public enum NiMode {
		NI_MODE_C_NORMAL("normal"), NI_MODE_C_REFLEX("reflex");
		
		String value;
		private NiMode(String setValue) {
			this.value = setValue; 
		}
		public static String[] VALUES = getValues();

		private static String[] getValues() {
			List<String> list = new ArrayList<String>();
			for (NiMode element : values()) {
				list.add(element.value);
			}
			return list.toArray(new String[0]);
		}
	};

	public enum PbMode {
		PB_MODE_C_NORMAL("normal"), PB_MODE_C_REFLEX("reflex");
		
		String value;
		private PbMode(String setValue) {
			this.value = setValue; 
		}
		public static String[] VALUES = getValues();

		private static String[] getValues() {
			List<String> list = new ArrayList<String>();
			for (PbMode element : values()) {
				list.add(element.value);
			}
			return list.toArray(new String[0]);
		}
	};

	enum CycleMode {
		CYCLE_MODE_C2D("C->D"), CYCLE_MODE_D2C("D->C"), 
		CYCLE_MODE_C2D2C("C->D->C"), CYCLE_MODE_D2C2D("D->C->D"), CYCLE_MODE_C2D2STO("C->D->S"), CYCLE_MODE_D2C2STO("D->C->S");
		
		String value;
		private CycleMode(String setValue) {
			this.value = setValue; 
		}
		public static String[] VALUES = getValues();

		private static String[] getValues() {
			List<String> list = new ArrayList<String>();
			for (CycleMode element : values()) {
				list.add(element.value);
			}
			return list.toArray(new String[0]);
		}
	};

	enum DischargeMode {
		REG_DCHG_OFF, REG_DCHG_INPUT, REG_DCHG_CH, REG_DCHG_AUTO,
	};

	enum ChannelType {
		REG_CH_TYPE_RES, REG_CH_TYPE_BAT,
	};

	public static final short	CELL_MAX										= 10;
//	public static final short	BT_MAX											= 10;													//TODO
//	public static final short	HW_NI_CELLS_MAX							= 10;													//TODO
//	public static final short	HW_PB_CELLS_MAX							= 10;													//TODO
//	public static final short	SET_CURRENT_MAX							= 20;													//TODO
//	public static final short	SET_ALL_CURRENT_MAX					= 20;													//TODO

	public static final short	NI_ZERO_VOLT								= 500;

	public static final short	CAP_MIN											= 0;
	public static final int		CAP_MAX											= 999900;
	public static final short	CAP_STEP										= 100;
	public static final short	CAP_DEFAULT									= 0;

	public static final short	RUN_COUNT_MIN								= 0;
	public static final short	RUN_COUNT_MAX								= 999;
	public static final short	RUN_COUNT_STEP							= 1;
	public static final short	RUN_COUNT_DEFAULT						= 0;

	public static final short	LOG_INTERVAL_MIN						= 5;
	public static final short	LOG_INTERVAL_MAX						= 600;
	public static final short	LOG_INTERVAL_STEP						= 5;
	public static final short	LOG_INTERVAL_DEFAULT				= 10;

	public static final short	BT_TYPE_MIN									= 0;
//	public static final short	BT_TYPE_MAX									= (BT_MAX - 1);
	public static final short	BT_TYPE_STEP								= 1;
	public static final short	BT_TYPE_DEFAULT							= 0;

	public static final short	LI_CELLS_MIN								= 0;
	public static final short	LI_CELLS_MAX								= CELL_MAX;
	public static final short	LI_CELLS_DEFAULT						= 0;

	public static final short	NI_CELLS_MIN								= 0;
//	public static final short	NI_CELLS_MAX								= HW_NI_CELLS_MAX;
	public static final short	NI_CELLS_DEFAULT						= 0;

	public static final short	PB_CELLS_MIN								= 1;
//	public static final short	PB_CELLS_MAX								= HW_PB_CELLS_MAX;
	public static final short	PB_CELLS_DEFAULT						= 6;

	public static final short	RESTORE_VOLT_MIN						= 500;
	public static final short	RESTORE_VOLT_MAX						= 2500;
	public static final short	RESTORE_VOLT_DEFAULT				= 1000;

	public static final short	RESTORE_TIME_MIN						= 1;
	public static final short	RESTORE_TIME_MAX						= 5;
	public static final short	RESTORE_TIME_DEFAULT				= 3;

	public static final short	RESTORE_CURRENT_MIN					= 2;
	public static final short	RESTORE_CURRENT_MAX					= 50;
	public static final short	RESTORE_CURRENT_DEFAULT			= 10;

	//#ifdef NIZN
	public static final short	NIZN_CELLS_MIN							= 0;
	public static final short	NIZN_CELLS_MAX							= CELL_MAX;
	public static final short	NIZN_CELLS_DEFAULT					= 0;

	public static final short	NIZN_CHG_MIN								= 1200;
	public static final short	NIZN_CHG_MAX								= 2000;
	public static final short	NIZN_CHG_DEFAULT						= 1900;

	public static final short	NIZN_DCHG_MIN								= 900;
	public static final short	NIZN_DCHG_MAX								= 1600;
	public static final short	NIZN_DCHG_DEFAULT						= 1100;

	public static final short	NIZN_STD										= 1650;
	public static final short	NIZN_MIN										= RESTORE_VOLT_MIN;
	public static final short	NIZN_MAX										= (NIZN_CHG_MAX + 50);
	//#endif

	//#ifdef LIHV
	public static final short	LIHV_CHG_MIN								= 3900;
	public static final short	LIHV_CHG_MAX								= 4450;
	public static final short	LIHV_CHG_DEFAULT						= 4350;

	public static final short	LIHV_STO_MIN								= 3700;
	public static final short	LIHV_STO_MAX								= 3900;
	public static final short	LIHV_STO_DEFAULT						= 3850;

	public static final short	LIHV_DCHG_MIN								= 3000;
	public static final short	LIHV_DCHG_MAX								= 4100;
	public static final short	LIHV_DCHG_DEFAULT						= 3500;

	public static final short	LIHV_STD										= 3700;
	public static final short	LIHV_MIN										= RESTORE_VOLT_MIN;
	public static final short	LIHV_MAX										= (LIHV_CHG_MAX + 50);
	//#endif

	//#ifdef LTO
	public static final short	LTO_CHG_MIN								= 2400;
	public static final short	LTO_CHG_MAX								= 3100;
	public static final short	LTO_CHG_DEFAULT						= 2850;

	public static final short	LTO_STO_MIN								= 2200;
	public static final short	LTO_STO_MAX								= 2600;
	public static final short	LTO_STO_DEFAULT						= 2400;

	public static final short	LTO_DCHG_MIN								= 1500;
	public static final short	LTO_DCHG_MAX								= 2900;
	public static final short	LTO_DCHG_DEFAULT						= 1800;

	public static final short	LTO_STD										= 3700;
	public static final short	LTO_MIN										= RESTORE_VOLT_MIN;
	public static final short	LTO_MAX										= (LTO_CHG_MAX + 50);
	//#endif

	//public static final short LITYPE_FE		=	2;
	public static final short	LI_BAL_VOLT_MIN							= 3000;
	public static final short	LI_BAL_VOLT_MAX							= 4200;
	public static final short	LI_BAL_VOLT_DEFAULT					= 3500;

	public static final short	LI_BAL_DIFF_MIN							= 1;
	public static final short	LI_BAL_DIFF_MAX							= 10;
	public static final short	LI_BAL_DIFF_DEFAULT					= 5;

	public static final short	LI_BAL_SETPOINT_MIN					= 1;
	public static final short	LI_BAL_SETPOINT_MAX					= 50;
	public static final short	LI_BAL_SETPOINT_DEFAULT			= 5;

	public static final short	LI_BAL_DELAY_MIN						= 0;
	public static final short	LI_BAL_DELAY_MAX						= 20;
	public static final short	LI_BAL_DELAY_DEFAULT				= 1;

	public static final short	LI_BAL_OVER_MIN							= 0;
	public static final short	LI_BAL_OVER_MAX							= 10;
	public static final short	LI_BAL_OVER_DEFAULT					= 0;

	public static final short	LIFE_CHG_MIN								= 3300;
	public static final short	LIFE_CHG_MAX								= 3800;
	public static final short	LIFE_CHG_DEFAULT						= 3600;

	public static final short	LIFE_STO_MIN								= 3100;
	public static final short	LIFE_STO_MAX								= 3400;
	public static final short	LIFE_STO_DEFAULT						= 3300;

	public static final short	LIFE_DCHG_MIN								= 2000;
	public static final short	LIFE_DCHG_MAX								= 3500;
	public static final short	LIFE_DCHG_DEFAULT						= 2500;

	public static final short	LIFE_STD										= 3300;
	public static final short	LIFE_MIN										= RESTORE_VOLT_MIN;
	public static final short	LIFE_MAX										= (LIFE_CHG_MAX + 50);

	public static final short	LIFE_ADJ_STEP								= 1;

	//public static final short LITYPE_LO	=	1;
	public static final short	LIIO_CHG_MIN								= 3750;
	public static final short	LIIO_CHG_MAX								= 4350;
	public static final short	LIIO_CHG_DEFAULT						= 4100;

	public static final short	LIIO_STO_MIN								= 3600;
	public static final short	LIIO_STO_MAX								= 3800;
	public static final short	LIIO_STO_DEFAULT						= 3750;

	public static final short	LIIO_DCHG_MIN								= 2500;
	public static final short	LIIO_DCHG_MAX								= 4000;
	public static final short	LIIO_DCHG_DEFAULT						= 3500;

	public static final short	LIIO_STD										= 3600;
	public static final short	LIIO_MIN										= RESTORE_VOLT_MIN;
	public static final short	LIIO_MAX										= (LIIO_CHG_MAX + 50);

	public static final short	LIIO_ADJ_STEP								= 1;

	public static final short	LIPO_CHG_MIN								= 3850;
	public static final short	LIPO_CHG_MAX								= 4350;
	public static final short	LIPO_CHG_DEFAULT						= 4200;

	public static final short	LIPO_STO_MIN								= 3700;
	public static final short	LIPO_STO_MAX								= 3900;
	public static final short	LIPO_STO_DEFAULT						= 3850;

	public static final short	LI_STO_COMP_MIN							= 0;
	public static final short	LI_STO_COMP_MAX							= 200;
	public static final short	LI_STO_COMP_DEFAULT					= 10;

	public static final short	LIPO_DCHG_MIN								= 3000;
	public static final short	LIPO_DCHG_MAX								= 4100;
	public static final short	LIPO_DCHG_DEFAULT						= 3500;

	public static final short	LIPO_STD										= 3700;
	public static final short	LIPO_MIN										= RESTORE_VOLT_MIN;
	public static final short	LIPO_MAX										= (LIPO_CHG_MAX + 50);

	public static final short	LIPO_ADJ_STEP								= 1;

	public static final short	NI_STD											= 1200;

	public static final short	PB_STD											= 2000;

	public static final short	PB_CHG_MIN									= 2000;
	public static final short	PB_CHG_MAX									= 2600;
	public static final short	PB_CHG_DEFAULT							= 2400;

	public static final short	PB_FLOAT_MIN								= 2200;
	public static final short	PB_FLOAT_MAX								= 2400;
	public static final short	PB_FLOAT_DEFAULT						= 2300;

	public static final short	PB_DCHG_MIN									= 1500;
	public static final short	PB_DCHG_MAX									= 2400;
	public static final short	PB_DCHG_DEFAULT							= 1800;

	public static final short	PB_ADJ_STEP									= 1;

	public static final short	MEM_EMPTY										= (short) 0xffff;
	public static final short	MEM_USED										= 0x55aa;
	public static final short	MEM_FIXED										= 0x0000;
	public static final short	MEM_SIZE										= 256;

	public static final short	CYCLE_COUNT_MIN							= 1;
	public static final short	CYCLE_COUNT_MAX							= 99;
	public static final short	CYCLE_COUNT_DEFAULT					= 3;

	public static final short	CYCLE_TIME_MIN							= 0;
	public static final short	CYCLE_TIME_MAX							= 9999;
	public static final short	CYCLE_TIME_DEFAULT					= 3;

	public static final short	SAFETY_TEMP_MIN							= 200;
	public static final short	SAFETY_TEMP_MAX							= 800;
	public static final short	SAFETY_TEMP_DEFAULT					= 450;

	public static final short	SAFETY_CAP_MIN							= 50;
	public static final short	SAFETY_CAP_MAX							= 200;
	public static final short	SAFETY_CAP_DEFAULT					= 120;

	public static final short	SAFETY_TIME_MIN							= 0;
	public static final short	SAFETY_TIME_MAX							= 9999;
	public static final short	SAFETY_TIME_DEFAULT					= 0;

	public static final short	END_CURRENT_C_MIN						= 1;
	public static final short	END_CURRENT_C_MAX						= 50;
	public static final short	END_CURRENT_C_DEFAULT				= 10;

	public static final short	END_CURRENT_D_MIN						= 1;
	public static final short	END_CURRENT_D_MAX						= 100;
	public static final short	END_CURRENT_D_DEFAULT				= 50;

	public static final short	END_CURRENT_STO_MAX					= 10;

	public static final short	CURRENT_MIN									= 5;
//	public static final short	CURRENT_MAX									= (SET_CURRENT_MAX);
	public static final short	CURRENT_DEFAULT							= 200;
//	public static final short	CURRENT_SYN_MAX							= SET_ALL_CURRENT_MAX;

	public static final short	VOLT_D_MIN									= 100;
	public static final short	VOLT_D_MAX									= (short) 40000;
	public static final short	VOLT_D_DEFAULT							= 10;

	public static final short	NI_PEAK_SENS_MIN						= 1;
	public static final short	NI_PEAK_SENS_MAX						= 20;
	public static final short	NIMH_PEAK_SENS_DEFAULT			= 3;
	public static final short	NICD_PEAK_SENS_DEFAULT			= 5;

	public static final short	NI_PEAK_DELAY_MIN						= 0;
	public static final short	NI_PEAK_DELAY_MAX						= 20;
	public static final short	NI_PEAK_DELAY_DEFAULT				= 3;

	public static final short	NI_TRICKLE_CURRENT_MIN			= 2;
	public static final short	NI_TRICKLE_CURRENT_MAX			= 100;
	public static final short	NI_TRICKLE_CURRENT_DEFAULT	= 5;

	public static final short	NI_TRICKLE_TIME_MIN					= 1;
	public static final short	NI_TRICKLE_TIME_MAX					= 999;
	public static final short	NI_TRICKLE_TIME_DEFAULT			= 5;

	public static final short	BAL_DIFF_SLOW								= 3;
	public static final short	BAL_POINT_SLOW							= 3;
	public static final short	BAL_OVER_SLOW								= 0;
	public static final short	BAL_DELAY_SLOW							= 2;

	public static final short	BAL_DIFF_NORMAL							= LI_BAL_DIFF_DEFAULT;
	public static final short	BAL_POINT_NORMAL						= LI_BAL_SETPOINT_DEFAULT;
	public static final short	BAL_OVER_NORMAL							= LI_BAL_OVER_DEFAULT;
	public static final short	BAL_DELAY_NORMAL						= LI_BAL_DELAY_DEFAULT;

	public static final short	BAL_DIFF_FAST								= 8;
	public static final short	BAL_POINT_FAST							= 8;
	public static final short	BAL_OVER_FAST								= 5;
	public static final short	BAL_DELAY_FAST							= 0;

	public static final short	REG_CH_VOLT_DEFAULT					= 12000;
	public static final short	REG_CH_CURRENT_DEFAULT			= 100;

	public static final short	MEM_NAME_LEN								= 37;

	short											useFlag;
	byte[]										name												= new byte[MEM_NAME_LEN + 1];
	int												capacity;
	byte											autoSave;
	byte											liBalEndMode;
	byte											lockFlag;
	byte[]										lockPWD											= new byte[6];
	short											opEnable;																									//Charge(bit0) ,Storage(bit2) ,Discharge(bit3) ,Cycle(bit4) ,OnlyBalance(bit5) 
	//55
	byte											channelMode;																							//CH1|CH2,CH1&CH2,CH1,CH2	
	byte											saveToSD;
	short											logInterval;
	short											runCounter;
	//61
	byte											type;																											//LiPo,LiLo,LiFe,NiMH,Nicd,Pb
	byte											liCell;
	byte											niCell;
	byte											pbCell;

	byte											liModeC;																									//Normal,Balance
	byte											liModeD;																									//Normal,Balance,External
	byte											niModeC;																									//Normal,REFLEX
	byte											niModeD;
	byte											pbModeC;																									//Normal,REFLEX
	byte											pbModeD;

	byte											balSpeed;
	byte											balStartMode;
	short											balStartVolt;
	byte											balDiff;
	byte											balOverPoint;
	byte											balSetPoint;
	byte											balDelay;
	//79
	byte											keepChargeEnable;

	short											liPoChgCellVolt;
	short											liIoChgCellVolt;
	short											liFeChgCellVolt;

	short											liPoStoCellVolt;
	short											liLoStoCellVolt;
	short											liFeStoCellVolt;
	//92
	short											liPoDchgCellVolt;
	short											liIoDchgCellVolt;
	short											liFeDchgCellVolt;
	//98
	short											chargeCurrent;
	short											dischargeCurrent;

	short											endCharge;
	short											endDischarge;
	short											regDchgMode;

	short											niPeak;
	short											niPeakDelay;

	short											niTrickleEnable;
	short											niTrickleCurrent;
	short											niTrickleTime;

	short											niZeroEnable;

	short											niDischargeVolt;
	short											pbChgCellVolt;
	short											pbDchgCellVolt;
	short											pbFloatEnable;
	short											pbFloatCellVolt;
	//130
	short											restoreVolt;
	short											restoreTime;
	short											restoreCurent;

	short											cycleCount;
	short											cycleDelay;
	byte											cycleMode;
	//141
	short											safetyTimeC;
	short											safetyCapC;
	short											safetyTempC;
	short											safetyTimeD;
	short											safetyCapD;
	short											safetyTempD;
	//153
	//#ifdef REG_CH
	byte											regChMode;
	short											regChVolt;
	short											regChCurrent;
	//#endif
	//158
	//#ifdef FAST_STO
	byte											fastSto;
	short											stoCompensation;
	//#endif
	//161
	//#ifdef NIZN
	short											niZnChgCellVolt;
	short											niZnDchgCellVolt;
	byte											niZnCell;
	//#endif

	//add type
	short											liHVChgCellVolt;																					//LIHV charge cell voltage
	short											liHVStoCellVolt;																					//LIHV storage cell voltage
	short											liHVDchgCellVolt;																					//LIHV discharge cell voltage
	//#ifdef TYPE_USER	
	short											ltoChgCellVolt;																						//LTO charge cell voltage
	short											ltoStoCellVolt;																						//LTO storage cell voltage
	short											ltoDchgCellVolt;																					//LTO discharge cell voltage
	//#endif
	//#ifdef TYPE_USER	
	short											userChgCellVolt;																					//LTO charge cell voltage
	short											userStoCellVolt;																					//LTO charge cell voltage
	short											userDchgCellVolt;																					//LTO charge cell voltage
	byte											userCell;																									//user defined cell
	//#endif
	//#ifdef DIGIT_POWER
	short											digitPowerVolt;
	short											digitPowerCurrent;
	short											digitPowerSet;																						//bit0--LOCK  bit1--?? bit2--??
	//#endif
	byte											dump;

	/**
	 * main constructor to create program memory instance from received data
	 * @param memoryBuffer filled by Modbus communication
	 */
	public ChargerMemory(final byte[] memoryBuffer, final boolean isDuo) {
		if (memoryBuffer != null && ((isDuo && memoryBuffer.length >= 167) || memoryBuffer.length >= 192))  {
			this.useFlag = DataParser.parse2Short(memoryBuffer[0], memoryBuffer[1]);
			System.arraycopy(memoryBuffer, 2, this.name, 0, this.name.length);
			this.capacity = DataParser.intFromBytes(memoryBuffer[MEM_NAME_LEN + 1 + 5], memoryBuffer[MEM_NAME_LEN + 1 + 4], memoryBuffer[MEM_NAME_LEN + 1 + 3], memoryBuffer[MEM_NAME_LEN + 1 + 2]);
			this.autoSave = memoryBuffer[44];
			this.liBalEndMode = memoryBuffer[45];
			this.lockFlag = memoryBuffer[46];
			System.arraycopy(memoryBuffer, 47, this.lockPWD, 0, lockPWD.length);
			this.opEnable = DataParser.parse2Short(memoryBuffer[53], memoryBuffer[54]); //Charge(bit0) ,Storage(bit2) ,Discharge(bit3) ,Cycle(bit4) ,OnlyBalance(bit5) 
			this.channelMode = memoryBuffer[55]; //CH1|CH2,CH1&CH2,CH1,CH2	
			this.saveToSD = memoryBuffer[56];
			this.logInterval = DataParser.parse2Short(memoryBuffer[57], memoryBuffer[58]);
			this.runCounter = DataParser.parse2Short(memoryBuffer[59], memoryBuffer[60]);
			this.type = memoryBuffer[61]; //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
			this.liCell = memoryBuffer[62];
			this.niCell = memoryBuffer[63];
			this.pbCell = memoryBuffer[64];
			this.liModeC = memoryBuffer[65]; //Normal,Balance
			this.liModeD = memoryBuffer[66]; //Normal,Balance,External
			this.niModeC = memoryBuffer[67]; //Normal,REFLEX
			this.niModeD = memoryBuffer[68];
			this.pbModeC = memoryBuffer[69]; //Normal,REFLEX
			this.pbModeD = memoryBuffer[70];
			this.balSpeed = memoryBuffer[71];
			this.balStartMode = memoryBuffer[72];
			this.balStartVolt = DataParser.parse2Short(memoryBuffer[73], memoryBuffer[74]);
			this.balDiff = memoryBuffer[75];
			this.balOverPoint = memoryBuffer[76];
			this.balSetPoint = memoryBuffer[77];
			this.balDelay = memoryBuffer[78];
			this.keepChargeEnable = memoryBuffer[79];
			this.liPoChgCellVolt = DataParser.parse2Short(memoryBuffer[80], memoryBuffer[81]);
			this.liIoChgCellVolt = DataParser.parse2Short(memoryBuffer[82], memoryBuffer[83]);
			this.liFeChgCellVolt = DataParser.parse2Short(memoryBuffer[84], memoryBuffer[85]);
			this.liPoStoCellVolt = DataParser.parse2Short(memoryBuffer[86], memoryBuffer[87]);
			this.liLoStoCellVolt = DataParser.parse2Short(memoryBuffer[88], memoryBuffer[89]);
			this.liFeStoCellVolt = DataParser.parse2Short(memoryBuffer[90], memoryBuffer[91]);
			this.liPoDchgCellVolt = DataParser.parse2Short(memoryBuffer[92], memoryBuffer[93]);
			this.liIoDchgCellVolt = DataParser.parse2Short(memoryBuffer[94], memoryBuffer[95]);
			this.liFeDchgCellVolt = DataParser.parse2Short(memoryBuffer[96], memoryBuffer[97]);
			this.chargeCurrent = DataParser.parse2Short(memoryBuffer[98], memoryBuffer[99]);
			this.dischargeCurrent = DataParser.parse2Short(memoryBuffer[100], memoryBuffer[101]);
			this.endCharge = DataParser.parse2Short(memoryBuffer[102], memoryBuffer[103]);
			this.endDischarge = DataParser.parse2Short(memoryBuffer[104], memoryBuffer[105]);
			this.regDchgMode = DataParser.parse2Short(memoryBuffer[106], memoryBuffer[107]);
			this.niPeak = DataParser.parse2Short(memoryBuffer[108], memoryBuffer[109]);
			this.niPeakDelay = DataParser.parse2Short(memoryBuffer[110], memoryBuffer[111]);
			this.niTrickleEnable = DataParser.parse2Short(memoryBuffer[112], memoryBuffer[113]);
			this.niTrickleCurrent = DataParser.parse2Short(memoryBuffer[114], memoryBuffer[115]);
			this.niTrickleTime = DataParser.parse2Short(memoryBuffer[116], memoryBuffer[117]);
			this.niZeroEnable = DataParser.parse2Short(memoryBuffer[118], memoryBuffer[119]);
			this.niDischargeVolt = DataParser.parse2Short(memoryBuffer[120], memoryBuffer[121]);
			this.pbChgCellVolt = DataParser.parse2Short(memoryBuffer[122], memoryBuffer[123]);
			this.pbDchgCellVolt = DataParser.parse2Short(memoryBuffer[124], memoryBuffer[125]);
			this.pbFloatEnable = DataParser.parse2Short(memoryBuffer[126], memoryBuffer[127]);
			this.pbFloatCellVolt = DataParser.parse2Short(memoryBuffer[128], memoryBuffer[129]);
			this.restoreVolt = DataParser.parse2Short(memoryBuffer[130], memoryBuffer[131]);
			this.restoreTime = DataParser.parse2Short(memoryBuffer[132], memoryBuffer[133]);
			this.restoreCurent = DataParser.parse2Short(memoryBuffer[134], memoryBuffer[135]);
			this.cycleCount = DataParser.parse2Short(memoryBuffer[136], memoryBuffer[137]);
			this.cycleDelay = DataParser.parse2Short(memoryBuffer[138], memoryBuffer[139]);
			this.cycleMode = memoryBuffer[140];
			this.safetyTimeC = DataParser.parse2Short(memoryBuffer[141], memoryBuffer[142]);
			this.safetyCapC = DataParser.parse2Short(memoryBuffer[143], memoryBuffer[144]);
			this.safetyTempC = DataParser.parse2Short(memoryBuffer[145], memoryBuffer[146]);
			this.safetyTimeD = DataParser.parse2Short(memoryBuffer[147], memoryBuffer[148]);
			this.safetyCapD = DataParser.parse2Short(memoryBuffer[149], memoryBuffer[150]);
			this.safetyTempD = DataParser.parse2Short(memoryBuffer[151], memoryBuffer[152]);
			this.regChMode = memoryBuffer[153];
			this.regChVolt = DataParser.parse2Short(memoryBuffer[154], memoryBuffer[155]);
			this.regChCurrent = DataParser.parse2Short(memoryBuffer[156], memoryBuffer[157]);
			this.fastSto = memoryBuffer[158];
			this.stoCompensation = DataParser.parse2Short(memoryBuffer[159], memoryBuffer[160]);
			this.niZnChgCellVolt = DataParser.parse2Short(memoryBuffer[161], memoryBuffer[162]);
			this.niZnDchgCellVolt = DataParser.parse2Short(memoryBuffer[163], memoryBuffer[164]);
			this.niZnCell = memoryBuffer[165];
			this.liHVChgCellVolt = DataParser.parse2Short(memoryBuffer[166], memoryBuffer[167]); //LIHV charge cell voltage
			this.liHVStoCellVolt = DataParser.parse2Short(memoryBuffer[168], memoryBuffer[169]); //LIHV storage cell voltage
			this.liHVDchgCellVolt = DataParser.parse2Short(memoryBuffer[170], memoryBuffer[171]); //LIHV discharge cell voltage
			if (isDuo) { //without LTO, User, Power		
				this.dump = memoryBuffer[172];
				return;
			}
			this.ltoChgCellVolt = DataParser.parse2Short(memoryBuffer[172], memoryBuffer[173]); //LTO charge cell voltage
			this.ltoStoCellVolt = DataParser.parse2Short(memoryBuffer[174], memoryBuffer[175]); //LTO storage cell voltage
			this.ltoDchgCellVolt = DataParser.parse2Short(memoryBuffer[176], memoryBuffer[177]); //LTO discharge cell voltage
			this.userChgCellVolt = DataParser.parse2Short(memoryBuffer[178], memoryBuffer[179]); //LTO charge cell voltage
			this.userStoCellVolt = DataParser.parse2Short(memoryBuffer[180], memoryBuffer[181]); //LTO charge cell voltage
			this.userDchgCellVolt = DataParser.parse2Short(memoryBuffer[182], memoryBuffer[183]); //LTO charge cell voltage
			this.userCell = memoryBuffer[184]; //user defined cell
			this.digitPowerVolt = DataParser.parse2Short(memoryBuffer[185], memoryBuffer[186]);
			this.digitPowerCurrent = DataParser.parse2Short(memoryBuffer[187], memoryBuffer[188]);
			this.digitPowerSet = DataParser.parse2Short(memoryBuffer[189], memoryBuffer[190]); //bit0--LOCK  bit1--?? bit2--??
			this.dump = memoryBuffer[191];
		}
	}
//	
//	/**
//	 * constructor with default values
//	 */
//	public ModbusMemory() {
//		this.useFlag = MEM_USED;
//		this.name[0] = 0;
//		this.capacity = CAP_DEFAULT;
//		this.autoSave = 0;
//		this.liBalEndMode = 0;
//		this.lockFlag = (byte) 0xff;
//		this.lockPWD = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };
//		this.opEnable = (short) 0xffff; //Charge(bit0) ,Storage(bit2) ,Discharge(bit3) ,Cycle(bit4) ,OnlyBalance(bit5) 
//
//		this.channelMode = 0; //CH1|CH2,CH1&CH2,CH1,CH2	
//		this.saveToSD = 1;
//		this.logInterval = LOG_INTERVAL_DEFAULT;
//		this.runCounter = RUN_COUNT_DEFAULT;
//
//		this.type = BT_TYPE_DEFAULT; //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
//		this.liCell = LI_CELLS_DEFAULT;
//		this.niCell = NI_CELLS_DEFAULT;
//		this.pbCell = PB_CELLS_DEFAULT;
//
//		this.liModeC = (byte) LiMode.LI_MODE_C_BAL.ordinal(); //Normal,Balance
//		this.liModeD = 0; //Normal,Balance,External
//		this.niModeC = (byte) NiMode.NI_MODE_C_NORMAL.ordinal(); //Normal,REFLEX
//		this.niModeD = 0;
//		this.pbModeC = 0; //Normal,REFLEX
//		this.pbModeD = 0;
//
//		this.balSpeed = (byte) BalancerSpeed.BAL_SPEED_NORMAL.ordinal();
//		this.balStartMode = (byte) BalancerStart.BAL_START_CV_200.ordinal();
//		this.balStartVolt = LI_BAL_VOLT_DEFAULT;
//		this.balDiff = LI_BAL_DIFF_DEFAULT;
//		this.balOverPoint = LI_BAL_OVER_DEFAULT;
//		this.balSetPoint = LI_BAL_SETPOINT_DEFAULT;
//		this.balDelay = LI_BAL_DELAY_DEFAULT;
//
//		this.keepChargeEnable = 0;
//
//		this.liPoChgCellVolt = LIPO_CHG_DEFAULT;
//		this.liLoChgCellVolt = LIIO_CHG_DEFAULT;
//		this.liFeChgCellVolt = LIFE_CHG_DEFAULT;
//
//		this.liPoStoCellVolt = LIPO_STO_DEFAULT;
//		this.liLoStoCellVolt = LIIO_STO_DEFAULT;
//		this.liFeStoCellVolt = LIFE_STO_DEFAULT;
//
//		this.liPoDchgCellVolt = LIPO_DCHG_DEFAULT;
//		this.liLoDchgCellVolt = LIIO_DCHG_DEFAULT;
//		this.liFeDchgCellVolt = LIFE_DCHG_DEFAULT;
//
//		this.chargeCurrent = CURRENT_DEFAULT;
//		this.dischargeCurrent = CURRENT_DEFAULT;
//
//		this.endCharge = END_CURRENT_C_DEFAULT;
//		this.endDischarge = END_CURRENT_D_DEFAULT;
//		this.regDchgMode = (short) DischargeMode.REG_DCHG_OFF.ordinal();
//
//		this.niPeak = NIMH_PEAK_SENS_DEFAULT;
//		this.niPeakDelay = NI_PEAK_DELAY_DEFAULT;
//
//		this.niTrickleEnable = 0;
//		this.niTrickleCurrent = NI_TRICKLE_CURRENT_DEFAULT;
//		this.niTrickleTime = NI_TRICKLE_TIME_DEFAULT;
//
//		this.niZeroEnable = 0;
//
//		this.niDischargeVolt = VOLT_D_DEFAULT;
//		this.pbChgCellVolt = PB_CHG_DEFAULT;
//		this.pbDchgCellVolt = PB_DCHG_DEFAULT;
//		this.pbFloatEnable = 0;
//		this.pbFloatCellVolt = PB_FLOAT_DEFAULT;
//
//		this.restoreVolt = RESTORE_VOLT_DEFAULT;
//		this.restoreTime = RESTORE_TIME_DEFAULT;
//		this.restoreCurent = RESTORE_CURRENT_DEFAULT;
//
//		this.cycleCount = CYCLE_COUNT_DEFAULT;
//		this.cycleDelay = CYCLE_TIME_DEFAULT;
//		this.cycleMode = (byte) CycleMode.CYCLE_MODE_C2D.ordinal();
//
//		this.safetyTimeC = SAFETY_TIME_DEFAULT;
//		this.safetyCapC = SAFETY_CAP_DEFAULT;
//		this.safetyTempC = SAFETY_TEMP_DEFAULT;
//		this.safetyTimeD = SAFETY_TIME_DEFAULT;
//		this.safetyCapD = SAFETY_CAP_DEFAULT;
//		this.safetyTempD = SAFETY_TEMP_DEFAULT;
//
//		this.regChMode = (byte) ChannelType.REG_CH_TYPE_RES.ordinal();
//		this.regChVolt = REG_CH_VOLT_DEFAULT;
//		this.regChCurrent = REG_CH_CURRENT_DEFAULT;
//
//		this.fastSto = 1;
//		this.stoCompensation = LI_STO_COMP_DEFAULT;
//
//		this.niZnChgCellVolt = NIZN_CHG_DEFAULT;
//		this.niZnDchgCellVolt = NIZN_DCHG_DEFAULT;
//		this.niZnCell = NIZN_CELLS_DEFAULT;
//	
//	short											liHVChgCellVolt;																					//LIHV charge cell voltage
//	short											liHVStoCellVolt;																					//LIHV storage cell voltage
//	short											liHVDchgCellVolt;																					//LIHV discharge cell voltage
//	//#ifdef TYPE_USER	
//	short											ltoChgCellVolt;																						//LTO charge cell voltage
//	short											ltoStoCellVolt;																						//LTO storage cell voltage
//	short											ltoDchgCellVolt;																					//LTO discharge cell voltage
//	//#endif
//	//#ifdef TYPE_USER	
//	short											userChgCellVolt;																					//LTO charge cell voltage
//	short											userStoCellVolt;																					//LTO charge cell voltage
//	short											userDchgCellVolt;																					//LTO charge cell voltage
//	byte											userCell;																									//user defined cell
//	//#endif
//	//#ifdef DIGIT_POWER
//	short											digitPowerVolt;
//	short											digitPowerCurrent;
//	short											digitPowerSet;																						//bit0--LOCK  bit1--?? bit2--??
//
//		this.dump = (byte) 0xff;
//	}
	
	/**
	 * copy constructor copy from existing program memory
	 * @param that the program memory to take over for the new instance
	 */
	public ChargerMemory(final ChargerMemory that, final boolean isDuo) {
		this.useFlag = that.useFlag;
		System.arraycopy(that.name, 0, this.name, 0, that.name.length);
		this.capacity = that.capacity;
		this.autoSave = that.autoSave;
		this.liBalEndMode = that.liBalEndMode;
		this.lockFlag = that.lockFlag;
		System.arraycopy(that.lockPWD, 0, this.lockPWD, 0, that.lockPWD.length);
		this.opEnable = that.opEnable; //Charge(bit0) ,Storage(bit2) ,Discharge(bit3) ,Cycle(bit4) ,OnlyBalance(bit5) 

		this.channelMode = that.channelMode; //CH1|CH2,CH1&CH2,CH1,CH2	
		this.saveToSD = that.saveToSD;
		this.logInterval = that.logInterval;
		this.runCounter = that.runCounter;

		this.type = that.type; //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
		this.liCell = that.liCell;
		this.niCell = that.niCell;
		this.pbCell = that.pbCell;

		this.liModeC = that.liModeC; //Normal,Balance
		this.liModeD = that.liModeD; //Normal,Balance,External
		this.niModeC = that.niModeC; //Normal,REFLEX
		this.niModeD = that.niModeD;
		this.pbModeC = that.pbModeC; //Normal,REFLEX
		this.pbModeD = that.pbModeD;

		this.balSpeed = that.balSpeed;
		this.balStartMode = that.balStartMode;
		this.balStartVolt = that.balStartVolt;
		this.balDiff = that.balDiff;
		this.balOverPoint = that.balOverPoint;
		this.balSetPoint = that.balSetPoint;
		this.balDelay = that.balDelay;

		this.keepChargeEnable = that.keepChargeEnable;

		this.liPoChgCellVolt = that.liPoChgCellVolt;
		this.liIoChgCellVolt = that.liIoChgCellVolt;
		this.liFeChgCellVolt = that.liFeChgCellVolt;

		this.liPoStoCellVolt = that.liPoStoCellVolt;
		this.liLoStoCellVolt = that.liLoStoCellVolt;
		this.liFeStoCellVolt = that.liFeStoCellVolt;

		this.liPoDchgCellVolt = that.liPoDchgCellVolt;
		this.liIoDchgCellVolt = that.liIoDchgCellVolt;
		this.liFeDchgCellVolt = that.liFeDchgCellVolt;

		this.chargeCurrent = that.chargeCurrent;
		this.dischargeCurrent = that.dischargeCurrent;

		this.endCharge = that.endCharge;
		this.endDischarge = that.endDischarge;
		this.regDchgMode = that.regDchgMode;

		this.niPeak = that.niPeak;
		this.niPeakDelay = that.niTrickleEnable;

		this.niTrickleEnable = that.niTrickleEnable;
		this.niTrickleCurrent = that.niTrickleCurrent;
		this.niTrickleTime = that.niTrickleTime;

		this.niZeroEnable = that.niZeroEnable;

		this.niDischargeVolt = that.niDischargeVolt;
		this.pbChgCellVolt = that.pbChgCellVolt;
		this.pbDchgCellVolt = that.pbDchgCellVolt;
		this.pbFloatEnable = that.pbFloatEnable;
		this.pbFloatCellVolt = that.pbFloatCellVolt;

		this.restoreVolt = that.restoreVolt;
		this.restoreTime = that.restoreTime;
		this.restoreCurent = that.restoreCurent;

		this.cycleCount = that.cycleCount;
		this.cycleDelay = that.cycleDelay;
		this.cycleMode = that.cycleMode;

		this.safetyTimeC = that.safetyTimeC;
		this.safetyCapC = that.safetyCapC;
		this.safetyTempC = that.safetyTempC;
		this.safetyTimeD = that.safetyTimeD;
		this.safetyCapD = that.safetyCapD;
		this.safetyTempD = that.safetyTempD;

		this.regChMode = that.regChMode;
		this.regChVolt = that.regChVolt;
		this.regChCurrent = that.regChCurrent;

		this.fastSto = that.fastSto;
		this.stoCompensation = that.stoCompensation;

		this.niZnChgCellVolt = that.niZnChgCellVolt;
		this.niZnDchgCellVolt = that.niZnDchgCellVolt;
		this.niZnCell = that.niZnCell;

		this.liHVChgCellVolt = that.liHVChgCellVolt; //LIHV charge cell voltage
		this.liHVStoCellVolt = that.liHVStoCellVolt; //LIHV storage cell voltage
		this.liHVDchgCellVolt = that.liHVDchgCellVolt; //LIHV discharge cell voltage

		if (!isDuo) {
			this.ltoChgCellVolt = that.ltoChgCellVolt; //LTO charge cell voltage
			this.ltoStoCellVolt = that.ltoStoCellVolt; //LTO storage cell voltage
			this.ltoDchgCellVolt = that.ltoStoCellVolt; //LTO discharge cell voltage
	
			this.userChgCellVolt = that.userChgCellVolt; //LTO charge cell voltage
			this.userStoCellVolt = that.userStoCellVolt; //LTO charge cell voltage
			this.userDchgCellVolt = that.userDchgCellVolt; //LTO charge cell voltage
			this.userCell = that.userCell; //user defined cell
	
			this.digitPowerVolt = that.digitPowerVolt;
			this.digitPowerCurrent = that.digitPowerCurrent;
			this.digitPowerSet = that.digitPowerSet; //bit0--LOCK  bit1--?? bit2--??
		}

		this.dump = that.dump;
	}

	public String toString(final boolean isDuo) {
		StringBuilder sb = new StringBuilder();
		sb.append("Memory :").append("\n");
		sb.append(String.format("useFlag \t\t= 0x%04X", useFlag)).append("\n");
		sb.append(String.format("name \t\t\t= \"%s\"", new String(name).trim())).append("\n");
		sb.append(String.format("capacity \t\t= %d", capacity)).append("\n");
		sb.append(String.format("autoSave \t\t= %d", autoSave)).append("\n");
		sb.append(String.format("liBalEndMode \t\t= %d", liBalEndMode)).append("\n");
		sb.append(String.format("lockFlag \t\t= %d", lockFlag&0xFF)).append("\n");
		sb.append(String.format("lockPWD \t\t= \"%s\"", new String(lockPWD))).append("\n");
		sb.append(String.format("opEnable \t\t= %s%s", StringHelper.printBinary((byte)(opEnable >> 8), false), StringHelper.printBinary((byte)(opEnable & 0xFF), false))).append("\n");

		sb.append(String.format("channelMode \t\t= %d", channelMode)).append("\n"); //CH1|CH2,CH1&CH2,CH1,CH2	
		sb.append(String.format("saveToSD \t\t= %d", saveToSD)).append("\n");
		sb.append(String.format("logInterval \t\t= %d", logInterval)).append("\n");
		sb.append(String.format("runCounter \t\t= %d", runCounter)).append("\n");

		sb.append(String.format("type \t\t\t= %d", type)).append("\n"); //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
		sb.append(String.format("liCell \t\t\t= %d", liCell)).append("\n");
		sb.append(String.format("niCell \t\t\t= %d", niCell)).append("\n");
		sb.append(String.format("pbCell \t\t\t= %d", pbCell)).append("\n");

		sb.append(String.format("liModeC \t\t= %d", liModeC)).append("\n"); //Normal,Balance
		sb.append(String.format("liModeD \t\t= %d", liModeD)).append("\n"); //Normal,Balance,External
		sb.append(String.format("niModeC \t\t= %d", niModeC)).append("\n"); //Normal,REFLEX
		sb.append(String.format("niModeD \t\t= %d", niModeD)).append("\n");
		sb.append(String.format("pbModeC \t\t= %d", pbModeC)).append("\n"); //Normal,REFLEX
		sb.append(String.format("pbModeD \t\t= %d", pbModeD)).append("\n");

		sb.append(String.format("balSpeed \t\t= %d", balSpeed)).append("\n");
		sb.append(String.format("balStartMode \t\t= %d", balStartMode&0xFF)).append("\n");
		sb.append(String.format("balStartVolt \t\t= %d", balStartVolt)).append("\n");
		sb.append(String.format("balDiff \t\t= %d", balDiff)).append("\n");
		sb.append(String.format("balOverPoint \t\t= %d", balOverPoint)).append("\n");
		sb.append(String.format("balSetPoint \t\t= %d", balSetPoint)).append("\n");
		sb.append(String.format("balDelay \t\t= %d", balDelay)).append("\n");

		sb.append(String.format("keepChargeEnable \t= %d", keepChargeEnable)).append("\n");

		sb.append(String.format("liPoChgCellVolt \t= %d", liPoChgCellVolt)).append("\n");
		sb.append(String.format("liLoChgCellVolt \t= %d", liIoChgCellVolt)).append("\n");
		sb.append(String.format("liFeChgCellVolt \t= %d", liFeChgCellVolt)).append("\n");

		sb.append(String.format("liPoStoCellVolt \t= %d", liPoStoCellVolt)).append("\n");
		sb.append(String.format("liLoStoCellVolt \t= %d", liLoStoCellVolt)).append("\n");;
		sb.append(String.format("liFeStoCellVolt \t= %d", liFeStoCellVolt)).append("\n");

		sb.append(String.format("liPoDchgCellVolt \t= %d", liPoDchgCellVolt)).append("\n");
		sb.append(String.format("liLoDchgCellVolt \t= %d", liIoDchgCellVolt)).append("\n");
		sb.append(String.format("liFeDchgCellVolt \t= %d", liFeDchgCellVolt)).append("\n");

		sb.append(String.format("chargeCurrent \t\t= %d", chargeCurrent)).append("\n");
		sb.append(String.format("dischargeCurrent \t= %d", dischargeCurrent)).append("\n");

		sb.append(String.format("endCharge \t\t= %d", endCharge)).append("\n");
		sb.append(String.format("endDischarge \t\t= %d", endDischarge)).append("\n");
		sb.append(String.format("regDchgMode \t\t= %d", regDchgMode)).append("\n");

		sb.append(String.format("niPeak \t\t\t= %d", niPeak)).append("\n");
		sb.append(String.format("niPeakDelay \t\t= %d", niPeakDelay)).append("\n");

		sb.append(String.format("niTrickleEnable \t= %d", niTrickleEnable)).append("\n");
		sb.append(String.format("niTrickleCurrent \t= %d", niTrickleCurrent)).append("\n");
		sb.append(String.format("niTrickleTime \t\t= %d", niTrickleTime)).append("\n");

		sb.append(String.format("niZeroEnable \t\t= %d", niZeroEnable)).append("\n");

		sb.append(String.format("niDischargeVolt \t= %d", niDischargeVolt)).append("\n");
		sb.append(String.format("pbChgCellVolt \t\t= %d", pbChgCellVolt)).append("\n");
		sb.append(String.format("pbDchgCellVolt \t\t= %d", pbDchgCellVolt)).append("\n");
		sb.append(String.format("pbFloatEnable \t\t= %d", pbFloatEnable)).append("\n");
		sb.append(String.format("pbFloatCellVolt \t= %d", pbFloatCellVolt)).append("\n");

		sb.append(String.format("restoreVolt \t\t= %d", restoreVolt)).append("\n");
		sb.append(String.format("restoreTime \t\t= %d", restoreTime)).append("\n");
		sb.append(String.format("restoreCurent \t\t= %d", restoreCurent)).append("\n");

		sb.append(String.format("cycleCount \t\t= %d", cycleCount)).append("\n");
		sb.append(String.format("cycleDelay \t\t= %d", cycleDelay)).append("\n");
		sb.append(String.format("cycleMode \t\t= %d", cycleMode)).append("\n");

		sb.append(String.format("safetyTimeC \t\t= %d", safetyTimeC)).append("\n");
		sb.append(String.format("safetyCapC \t\t= %d", safetyCapC)).append("\n");
		sb.append(String.format("safetyTempC \t\t= %d", safetyTempC)).append("\n");
		sb.append(String.format("safetyTimeD \t\t= %d", safetyTimeD)).append("\n");
		sb.append(String.format("safetyCapD \t\t= %d", safetyCapD)).append("\n");
		sb.append(String.format("safetyTempD \t\t= %d", safetyTempD)).append("\n");

		if (isDuo) {
			sb.append(String.format("regChMode \t\t= %d", regChMode)).append("\n");
			sb.append(String.format("regChVolt \t\t= %d", regChVolt)).append("\n");
			sb.append(String.format("regChCurrent \t\t= %d", regChCurrent)).append("\n");
		}

		sb.append(String.format("fastSto \t\t= %d", fastSto)).append("\n");
		sb.append(String.format("stoCompensation \t= %d", stoCompensation)).append("\n");

		sb.append(String.format("niZnChgCellVolt \t= %d", niZnChgCellVolt)).append("\n");
		sb.append(String.format("niZnDchgCellVolt \t= %d", niZnDchgCellVolt)).append("\n");
		sb.append(String.format("niZnCell \t\t= %d", niZnCell)).append("\n");
		
		sb.append(String.format("liHVChgCellVolt \t= %d", liHVChgCellVolt)).append("\n");
		sb.append(String.format("liHVStoCellVolt \t= %d", liHVStoCellVolt)).append("\n");
		sb.append(String.format("liHVDchgCellVolt \t= %d", liHVDchgCellVolt)).append("\n");

		if (!isDuo) {
			sb.append(String.format("ltoChgCellVolt \t\t= %d", ltoChgCellVolt)).append("\n");
			sb.append(String.format("ltoStoCellVolt \t\t= %d", ltoStoCellVolt)).append("\n");
			sb.append(String.format("ltoDchgCellVolt \t= %d", ltoDchgCellVolt)).append("\n");
			
			sb.append(String.format("userChgCellVolt \t= %d", userChgCellVolt)).append("\n");
			sb.append(String.format("userStoCellVolt \t= %d", userStoCellVolt)).append("\n");
			sb.append(String.format("userDchgCellVolt \t= %d", userDchgCellVolt)).append("\n");
			sb.append(String.format("userCell \t\t= %d", userCell)).append("\n");
			
			sb.append(String.format("digitPowerVolt \t\t= %d", digitPowerVolt)).append("\n");
			sb.append(String.format("digitPowerCurrent \t= %d", digitPowerCurrent)).append("\n");
			sb.append(String.format("digitPowerSet \t\t= %s %s", StringHelper.printBinary((byte) (digitPowerSet >> 8), false), StringHelper.printBinary((byte) (digitPowerSet &0x0F), false))).append("\n");
		}
		sb.append(String.format("dump \t\t\t= %s", StringHelper.printBinary(dump, false))).append("\n");
		
		return sb.toString();
	}
	
	/**
	 * HW_NI_CELLS_MAX  4010duo=18; 406duo,X6,S6=12;  308duo,X8=15;  X12=20
	 * @param device
	 * @return number of maximal supported cells Pb chemistry
	 */
	public static int getMaxCellsNi(IDevice device) {
		if (device instanceof iChargerX12) {
			return (int) (20 * 2 / 1.2);
		}
		else if (device instanceof iChargerX8 || device instanceof iCharger308DUO || device instanceof iChargerDX8) {
			return (int) (15 * 2 / 1.2);
		}
		else if (device instanceof iChargerX6 || device instanceof iChargerS6 || device instanceof iCharger406DUO || device instanceof iChargerDX6) {
			return (int) (12 * 2 / 1.2);
		}
		else if (device instanceof iCharger4010DUO) {
			return (int) (18 * 2 / 1.2);
		}
		return 0;
	}
	
	/**
	 * HW_PB_CELLS_MAX  4010duo=18; 406duo,X6,S6=12;  308duo,X8=15;  X12=20
	 * @param device
	 * @return number of maximal supported cells Pb chemistry
	 */
	public static int getMaxCellsPb(IDevice device) {
		if (device instanceof iChargerX6 || device instanceof iChargerS6 || device instanceof iCharger406DUO || device instanceof iChargerDX6 || device instanceof iCharger456DUO) {
			return 12;
		}
		else if (device instanceof iChargerX8 || device instanceof iCharger308DUO || device instanceof iChargerDX8 || device instanceof iCharger458DUO) {
			return 15;
		}
		else if (device instanceof iCharger4010DUO) {
			return 18;
		}
		else if (device instanceof iChargerX12 || device instanceof iChargerDX12 || device instanceof iCharger4512DUO) {
			return 20;
		}
		return 0;
	}
	
		
	public byte[] getAsByteArray(final boolean isDuo) {
		byte[] memoryBuffer = new byte[(ChargerMemory.size + 1) / 2 * 2];
		
		memoryBuffer[0] = (byte) (this.useFlag & 0xFF);
		memoryBuffer[1] = (byte) (this.useFlag >> 8);
		System.arraycopy(this.name, 0, memoryBuffer, 2, this.name.length);
		memoryBuffer[MEM_NAME_LEN+1+5] = (byte) (this.capacity >> 24);
		memoryBuffer[MEM_NAME_LEN+1+4] = (byte) ((this.capacity & 0x00FF0000) >> 16);
		memoryBuffer[MEM_NAME_LEN+1+3] = (byte) ((this.capacity & 0x0000FF00) >> 8);
		memoryBuffer[MEM_NAME_LEN+1+2] = (byte) (this.capacity & 0xFF);
		memoryBuffer[44] = this.autoSave;
		memoryBuffer[45] = this.liBalEndMode;
		memoryBuffer[46] = this.lockFlag;
		System.arraycopy(this.lockPWD, 0, memoryBuffer, 47, lockPWD.length);
		memoryBuffer[54] = (byte) (this.opEnable >> 8);
		memoryBuffer[53] = (byte) (this.opEnable & 0xFF);
		memoryBuffer[55] = this.channelMode; //CH1|CH2,CH1&CH2,CH1,CH2	
		memoryBuffer[56] = this.saveToSD;
		memoryBuffer[58] = (byte) (this.logInterval >> 8);
		memoryBuffer[57] = (byte) (this.logInterval & 0xFF);
		memoryBuffer[60] = (byte) (this.runCounter >> 8);
		memoryBuffer[59] = (byte) (this.runCounter & 0xFF);

		memoryBuffer[61] = this.type; //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
		memoryBuffer[62] = this.liCell;
		memoryBuffer[63] = this.niCell;
		memoryBuffer[64] = this.pbCell;

		memoryBuffer[65] = this.liModeC; //Normal,Balance
		memoryBuffer[66] = this.liModeD; //Normal,Balance,External
		memoryBuffer[67] = this.niModeC; //Normal,REFLEX
		memoryBuffer[68] = this.niModeD;
		memoryBuffer[69] = this.pbModeC; //Normal,REFLEX
		memoryBuffer[70] = this.pbModeD;

		memoryBuffer[71] = this.balSpeed;
		memoryBuffer[72] = this.balStartMode;
		memoryBuffer[74] = (byte) (this.balStartVolt >> 8);
		memoryBuffer[73] = (byte) (this.balStartVolt & 0xFF);
		memoryBuffer[75] = this.balDiff;
		memoryBuffer[76] = this.balOverPoint;
		memoryBuffer[77] = this.balSetPoint;
		memoryBuffer[78] = this.balDelay;

		memoryBuffer[79] = this.keepChargeEnable;

		memoryBuffer[81] = (byte) (this.liPoChgCellVolt >> 8);
		memoryBuffer[80] = (byte) (this.liPoChgCellVolt & 0xFF);
		memoryBuffer[83] = (byte) (this.liIoChgCellVolt >> 8);
		memoryBuffer[82] = (byte) (this.liIoChgCellVolt & 0xFF);
		memoryBuffer[85] = (byte) (this.liFeChgCellVolt >> 8);
		memoryBuffer[84] = (byte) (this.liFeChgCellVolt & 0xFF);

		memoryBuffer[87] = (byte) (this.liPoStoCellVolt >> 8);
		memoryBuffer[86] = (byte) (this.liPoStoCellVolt & 0xFF);
		memoryBuffer[89] = (byte) (this.liLoStoCellVolt >> 8);
		memoryBuffer[88] = (byte) (this.liLoStoCellVolt & 0xFF);
		memoryBuffer[91] = (byte) (this.liFeStoCellVolt >> 8);
		memoryBuffer[90] = (byte) (this.liFeStoCellVolt & 0xFF);

		memoryBuffer[93] = (byte) (this.liPoDchgCellVolt >> 8);
		memoryBuffer[92] = (byte) (this.liPoDchgCellVolt & 0xFF);
		memoryBuffer[95] = (byte) (this.liIoDchgCellVolt >> 8);
		memoryBuffer[94] = (byte) (this.liIoDchgCellVolt & 0xFF);
		memoryBuffer[97] = (byte) (this.liFeDchgCellVolt >> 8);
		memoryBuffer[96] = (byte) (this.liFeDchgCellVolt & 0xFF);

		memoryBuffer[99] = (byte) (this.chargeCurrent >> 8);
		memoryBuffer[98] = (byte) (this.chargeCurrent & 0xFF);
		memoryBuffer[101] = (byte) (this.dischargeCurrent >> 8);
		memoryBuffer[100] = (byte) (this.dischargeCurrent & 0xFF);

		memoryBuffer[103] = (byte) (this.endCharge >> 8);
		memoryBuffer[102] = (byte) (this.endCharge & 0xFF);
		memoryBuffer[105] = (byte) (this.endDischarge >> 8);
		memoryBuffer[104] = (byte) (this.endDischarge & 0xFF);
		memoryBuffer[107] = (byte) (this.regDchgMode >> 8);
		memoryBuffer[106] = (byte) (this.regDchgMode & 0xFF);

		memoryBuffer[109] = (byte) (this.niPeak >> 8);
		memoryBuffer[108] = (byte) (this.niPeak & 0xFF);
		memoryBuffer[111] = (byte) (this.niPeakDelay >> 8);
		memoryBuffer[110] = (byte) (this.niPeakDelay & 0xFF);

		memoryBuffer[113] = (byte) (this.niTrickleEnable >> 8);
		memoryBuffer[112] = (byte) (this.niTrickleEnable & 0xFF);
		memoryBuffer[115] = (byte) (this.niTrickleCurrent >> 8);
		memoryBuffer[114] = (byte) (this.niTrickleCurrent & 0xFF);
		memoryBuffer[117] = (byte) (this.niTrickleTime >> 8);
		memoryBuffer[116] = (byte) (this.niTrickleTime & 0xFF);

		memoryBuffer[119] = (byte) (this.niZeroEnable >> 8);
		memoryBuffer[118] = (byte) (this.niZeroEnable & 0xFF);

		memoryBuffer[121] = (byte) (this.niDischargeVolt >> 8);
		memoryBuffer[120] = (byte) (this.niDischargeVolt & 0xFF);
		memoryBuffer[123] = (byte) (this.pbChgCellVolt >> 8);
		memoryBuffer[122] = (byte) (this.pbChgCellVolt & 0xFF);
		memoryBuffer[125] = (byte) (this.pbDchgCellVolt >> 8);
		memoryBuffer[124] = (byte) (this.pbDchgCellVolt & 0xFF);
		memoryBuffer[127] = (byte) (this.pbFloatEnable >> 8);
		memoryBuffer[126] = (byte) (this.pbFloatEnable & 0xFF);
		memoryBuffer[129] = (byte) (this.pbFloatCellVolt >> 8);
		memoryBuffer[128] = (byte) (this.pbFloatCellVolt & 0xFF);

		memoryBuffer[131] = (byte) (this.restoreVolt >> 8);
		memoryBuffer[130] = (byte) (this.restoreVolt & 0xFF);
		memoryBuffer[133] = (byte) (this.restoreTime >> 8);
		memoryBuffer[132] = (byte) (this.restoreTime & 0xFF);
		memoryBuffer[135] = (byte) (this.restoreCurent >> 8);
		memoryBuffer[134] = (byte) (this.restoreCurent & 0xFF);

		memoryBuffer[137] = (byte) (this.cycleCount >> 8);
		memoryBuffer[136] = (byte) (this.cycleCount & 0xFF);
		memoryBuffer[139] = (byte) (this.cycleDelay >> 8);
		memoryBuffer[138] = (byte) (this.cycleDelay & 0xFF);
		memoryBuffer[140] = this.cycleMode;

		memoryBuffer[142] = (byte) (this.safetyTimeC >> 8);
		memoryBuffer[141] = (byte) (this.safetyTimeC & 0xFF);
		memoryBuffer[144] = (byte) (this.safetyCapC >> 8);
		memoryBuffer[143] = (byte) (this.safetyCapC & 0xFF);
		memoryBuffer[146] = (byte) (this.safetyTempC >> 8);
		memoryBuffer[145] = (byte) (this.safetyTempC & 0xFF);
		memoryBuffer[148] = (byte) (this.safetyTimeD >> 8);
		memoryBuffer[147] = (byte) (this.safetyTimeD & 0xFF);
		memoryBuffer[150] = (byte) (this.safetyCapD >> 8);
		memoryBuffer[149] = (byte) (this.safetyCapD & 0xFF);
		memoryBuffer[152] = (byte) (this.safetyTempD >> 8);
		memoryBuffer[151] = (byte) (this.safetyTempD & 0xFF);

		memoryBuffer[153] = this.regChMode;
		memoryBuffer[155] = (byte) (this.regChVolt >> 8);
		memoryBuffer[154] = (byte) (this.regChVolt & 0xFF);
		memoryBuffer[157] = (byte) (this.regChCurrent >> 8);
		memoryBuffer[156] = (byte) (this.regChCurrent & 0xFF);

		memoryBuffer[158] = this.fastSto;
		memoryBuffer[160] = (byte) (this.stoCompensation >> 8);
		memoryBuffer[159] = (byte) (this.stoCompensation & 0xFF);

		memoryBuffer[162] = (byte) (this.niZnChgCellVolt >> 8);
		memoryBuffer[161] = (byte) (this.niZnChgCellVolt & 0xFF);
		memoryBuffer[164] = (byte) (this.niZnDchgCellVolt >> 8);
		memoryBuffer[163] = (byte) (this.niZnDchgCellVolt & 0xFF);
		memoryBuffer[165] = this.niZnCell;

		memoryBuffer[167] = (byte) (this.liHVChgCellVolt >> 8);
		memoryBuffer[166] = (byte) (this.liHVChgCellVolt & 0xFF);
		memoryBuffer[169] = (byte) (this.liHVStoCellVolt >> 8);
		memoryBuffer[168] = (byte) (this.liHVStoCellVolt & 0xFF);
		memoryBuffer[171] = (byte) (this.liHVDchgCellVolt >> 8);
		memoryBuffer[170] = (byte) (this.liHVDchgCellVolt & 0xFF);

		if (isDuo) { //without LTO, User, Power
			memoryBuffer[172] = this.dump;
			return memoryBuffer;
		}
		
		memoryBuffer[173] = (byte) (this.ltoChgCellVolt >> 8);
		memoryBuffer[172] = (byte) (this.ltoChgCellVolt & 0xFF);
		memoryBuffer[175] = (byte) (this.ltoStoCellVolt >> 8);
		memoryBuffer[174] = (byte) (this.ltoStoCellVolt & 0xFF);
		memoryBuffer[177] = (byte) (this.ltoDchgCellVolt >> 8);
		memoryBuffer[176] = (byte) (this.ltoDchgCellVolt & 0xFF);

		memoryBuffer[179] = (byte) (this.userChgCellVolt >> 8);
		memoryBuffer[178] = (byte) (this.userChgCellVolt & 0xFF);
		memoryBuffer[181] = (byte) (this.userStoCellVolt >> 8);
		memoryBuffer[180] = (byte) (this.userStoCellVolt & 0xFF);
		memoryBuffer[183] = (byte) (this.userDchgCellVolt >> 8);
		memoryBuffer[182] = (byte) (this.userDchgCellVolt & 0xFF);
		memoryBuffer[184] = this.userCell; //user defined cell

		memoryBuffer[186] = (byte) (this.digitPowerVolt >> 8);
		memoryBuffer[185] = (byte) (this.digitPowerVolt & 0xFF);
		memoryBuffer[188] = (byte) (this.digitPowerCurrent >> 8);
		memoryBuffer[187] = (byte) (this.digitPowerCurrent & 0xFF);
		memoryBuffer[190] = (byte) (this.digitPowerSet >> 8);
		memoryBuffer[189] = (byte) (this.digitPowerSet & 0xFF);

		memoryBuffer[191] = this.dump;
		return memoryBuffer;
	}
	
	public int[] getMemoryValues(final int[] values, final boolean isDuo) {
		
		values[0] = this.getType(); //0 battery type
		
		if (isDuo) {
			switch (this.getType()) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
			case 0: //LiPo
			case 1: //LiIo
			case 2: //LiFe
			case 7: //LiHV
				values[1] = this.getLiCell(); //1 cell count
				break;
			case 3: //NiMH
			case 4: //NiCd
				values[1] = this.getNiCell(); //1 cell count
				break;
			case 5: //Pb
				values[1] = this.getPbCell(); //1 cell count
				break;
			case 6: //NiZn
				values[1] = this.getNiZnCell(); //1 cell count
				break;
			default: //unknown
				values[1] = 0; //1 cell count
				break;
			}
		}
		else {
			switch (this.getType()) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
			case 0: //LiPo
			case 1: //LiIo
			case 2: //LiFe
			case 3: //LiHV
			case 4: //LTO
				values[1] = this.getLiCell(); //1 cell count
				break;
			case 5: //NiMH
			case 6: //NiCd
				values[1] = this.getNiCell(); //1 cell count
				break;
			case 7: //NiZn
				values[1] = this.getNiZnCell(); //1 cell count
				break;
			case 8: //Pb
				values[1] = this.getPbCell(); //1 cell count
				break;
			default: //Power
				values[1] = 0; //1 cell count
				break;
			}
		}
		
		values[2] = this.getCapacity(); //2 capacity
		
		//3 charge parameter current
		values[3] = this.getChargeCurrent();
		//5 charge parameter balancer Li Setup
		values[5] = this.getLiBalEndMode(); //end current OFF, detect balancer ON,...
		//6 charge parameter end current
		values[6] = this.getEndCharge();
		//4 charge parameter modus normal,balance,external,reflex 
		//7 charge parameter cell voltage
		if (isDuo) {
			switch (this.getType()) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
			case 0: //LiPo
				values[4] = this.getLiModeC(); 
				values[7] = this.getLiPoChgCellVolt(); 
				break;
			case 1: //LiIo
				values[4] = this.getLiModeC(); 
				values[7] = this.getLiIoChgCellVolt(); 
				break;
			case 2: //LiFe
				values[4] = this.getLiModeC(); 
				values[7] = this.getLiFeChgCellVolt(); 
				break;
			case 3: //NiMH
			case 4: //NiCd
				values[4] = this.getNiModeC(); 
				values[7] = 0; 
				break;
			case 5: //Pb
				values[4] = this.getPbModeC(); 
				values[7] = this.getPbChgCellVolt();
				break;
			case 6: //NiZn
				values[4] = this.getNiModeC(); 
				values[7] = this.getNiZnChgCellVolt();
				break;
			case 7: //LiHV
				values[4] = this.getLiModeC(); 
				values[7] = this.getLiHVChgCellVolt(); 
				break;
			default: 
				values[7] = 0; 
				break;
			}
		}
		else {
			switch (this.getType()) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
			case 0: //LiPo
				values[4] = this.getLiModeC(); 
				values[7] = this.getLiPoChgCellVolt(); 
				break;
			case 1: //LiIo
				values[4] = this.getLiModeC(); 
				values[7] = this.getLiIoChgCellVolt(); 
				break;
			case 2: //LiFe
				values[4] = this.getLiModeC(); 
				values[7] = this.getLiFeChgCellVolt(); 
				break;
			case 3: //LiHV
				values[4] = this.getLiModeC(); 
				values[7] = this.getLiHVChgCellVolt();
				break;
			case 4: //LTO
				values[4] = this.getLiModeC(); 
				values[7] = this.getLtoChgCellVolt();
				break;
			case 5: //NiMH
			case 6: //NiCd
				values[4] = this.getNiModeC(); 
				values[7] = 0; 
				break;
			case 7: //NiZn
				values[4] = this.getNiModeC(); 
				values[7] = this.getNiZnChgCellVolt(); 
				break;
			case 8: //Pb
				values[4] = this.getPbModeC(); 
				values[7] = this.getPbChgCellVolt();
				break;
			default: 
				values[7] = 0; 
				break;
			}
		}
		
		//8 charge safety temp cut
		values[8] = this.getSafetyTempC() / 10;
		//9 charge max capacity
		values[9] = this.getSafetyCapC();
		//10 charge max time
		values[10] = this.getSafetyTimeC();

		//11 charge parameter balancer
		values[11] = this.getBalSpeed();
		//12 charge parameter balancer start
		values[12] = this.getBalStartMode();
		//13 charge parameter balancer difference
		values[13] = this.getBalDiff();
		//14 charge parameter balancer target
		values[14] = this.getBalSetPoint();
		//15 charge parameter balancer over charge
		values[15] = this.getBalOverPoint();
		//16 charge parameter balancer delay
		values[16] = this.getBalDelay();

		//17 discharge parameter current
		values[17] = this.getDischargeCurrent();
		//19 discharge end current
		values[19] = this.getEndDischarge();
		//20 regenerative mode 
		values[20] = this.getRegDchgMode();
		//18 discharge parameter cell voltage
		//21 discharge extra
		//22 discharge balancer
		if (isDuo) {
			switch (this.getType()) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
			case 0: //LiPo
				values[18] = this.getLiPoDchgCellVolt();
				values[21] = this.getLiModeD() & 0x01;
				values[22] = this.getLiModeD() >> 1;
				break;
			case 1: //LiIo
				values[18] = this.getLiIoDchgCellVolt();
				values[21] = this.getLiModeD() & 0x01;
				values[22] = this.getLiModeD() >> 1;
				break;
			case 2: //LiFe
				values[18] = this.getLiFeDchgCellVolt();
				values[21] = this.getLiModeD() & 0x01;
				values[22] = this.getLiModeD() >> 1;
				break;
			case 3: //NiMH
			case 4: //NiCd
				values[18] = this.getNiDischargeVolt();
				values[21] = this.getNiModeD() & 0x01;
				values[22] = this.getNiModeD() >> 1;
				break;
			case 5: //Pb
				values[18] = this.getPbDchgCellVolt();
				values[21] = this.getPbModeD() & 0x01;
				values[22] = this.getPbModeD() >> 1;
				break;
			case 6: //NiZn
				values[18] = this.getNiZnDchgCellVolt();
				values[21] = this.getLiModeD() & 0x01;
				values[22] = this.getLiModeD() >> 1;
				break;
			case 7: //LiHV
				values[18] = this.getLiHVDchgCellVolt();
				values[21] = this.getLiModeD() & 0x01;
				values[22] = this.getLiModeD() >> 1;
				break;
			default: 
				values[18] = 0;
				values[21] = 0;
				values[22] = 0;
				break;
			}
		}
		else {
			switch (this.getType()) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
			case 0: //LiPo
				values[18] = this.getLiPoDchgCellVolt();
				values[21] = this.getLiModeD() & 0x01;
				values[22] = this.getLiModeD() >> 1;
				break;
			case 1: //LiIo
				values[18] = this.getLiIoDchgCellVolt();
				values[21] = this.getLiModeD() & 0x01;
				values[22] = this.getLiModeD() >> 1;
				break;
			case 2: //LiFe
				values[18] = this.getLiFeDchgCellVolt();
				values[21] = this.getLiModeD() & 0x01;
				values[22] = this.getLiModeD() >> 1;
				break;
			case 3: //LiHV
				values[18] = this.getLiHVDchgCellVolt();
				values[21] = this.getLiModeD() & 0x01;
				values[22] = this.getLiModeD() >> 1;
				break;
			case 4: //LTO
				values[18] = this.getLtoDchgCellVolt();
				values[21] = this.getLiModeD() & 0x01;
				values[22] = this.getLiModeD() >> 1;
				break;
			case 5: //NiMH
			case 6: //NiCd
				values[18] = this.getNiDischargeVolt();
				values[21] = this.getNiModeD() & 0x01;
				values[22] = this.getNiModeD() >> 1;
				break;
			case 7: //NiZn
				values[18] = this.getNiZnDchgCellVolt();
				values[21] = this.getLiModeD() & 0x01;
				values[22] = this.getLiModeD() >> 1;
				break;
			case 8: //Pb
				values[18] = this.getPbDchgCellVolt();
				values[21] = this.getPbModeD() & 0x01;
				values[22] = this.getPbModeD() >> 1;
				break;
			default: 
				values[18] = 0;
				values[21] = 0;
				values[22] = 0;
				break;
			}
		}
		//23 discharge parameter cut temperature
		values[23] = this.getSafetyTempD() / 10;
		//24 discharge parameter max discharge capacity
		values[24] = this.getSafetyCapD();
		//25 discharge parameter safety timer
		values[25] = this.getSafetyTimeD();

		//26 Ni charge voltage drop
		values[26] = this.getNiPeak();
		//27 Ni charge voltage drop delay
		values[27] = this.getNiPeakDelay();
		//28 Ni charge allow 0V 
		values[28] = this.getNiZeroEnable();
		//29 Ni trickle charge enable 
		values[29] = this.getNiTrickleEnable();
		//30 Ni charge trickle current
		values[30] = this.getNiTrickleCurrent();
		//31 Ni charge trickle timeout
		values[31] = this.getNiTrickleTime();

		//32 charge restore lowest voltage
		values[32] = this.getRestoreVolt();
		//33 charge restore charge time
		values[33] = this.getRestoreTime();
		//34 charge restore charge current
		values[34] = this.getRestoreCurent();
		//35 charge keep charging after done
		values[35] = this.getKeepChargeEnable();

		//36 storage parameter cell voltage (Li battery type only)
		if (isDuo) {
			switch (this.getType()) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
			case 0: //LiPo
				values[36] = this.getLiPoStoCellVolt();
				break;
			case 1: //LiIo
				values[36] = this.getLiLoStoCellVolt();
				break;
			case 2: //LiFe
				values[36] = this.getLiFeStoCellVolt();
				break;
			case 7: //LiHV
				values[36] = this.getLiHVStoCellVolt();
				break;
			case 3: //NiMH
			case 4: //NiCd
			case 5: //Pb
			case 6: //NiZn
			default: 
				values[36] = 0;
				break;
			}
		}
		else {
			switch (this.getType()) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
			case 0: //LiPo
				values[36] = this.getLiPoStoCellVolt();
				break;
			case 1: //LiIo
				values[36] = this.getLiLoStoCellVolt();
				break;
			case 2: //LiFe
				values[36] = this.getLiFeStoCellVolt();
				break;
			case 3: //LiHV
				values[36] = this.getLiHVStoCellVolt();
				break;
			case 4: //LTO
				values[36] = this.getLtoStoCellVolt();
				break;
			case 5: //NiMH
			case 6: //NiCd
			case 7: //NiZn
			case 8: //Pb
			default: 
				values[36] = 0;
				break;
			}
		}
		//37 storage parameter compensation
		values[37] = this.getStoCompensation();
		//38 storage acceleration
		values[38] = this.getFastSto();

		//39 cycle parameter mode CHG->DCHG
		values[39] = this.getCycleMode();
		//40 cycle count
		values[40] = this.getCycleCount();
		//41 cycle delay
		values[41] = this.getCycleDelay();

		//42 power voltage
		values[42] = this.getDigitPowerVolt() / 100;		
		//43 power current
		values[43] = this.getDigitPowerCurrent() / 10;		
		//44 power option lock
		values[44] = this.getDigitPowerSet() & 0x01;		
		//45 power option auto start
		values[45] = this.getDigitPowerSet() & 0x02;	
		//46 power option live update
		values[46] = this.getDigitPowerSet() & 0x04;

		//47 channel mode asynchronous | synchronous DUO only
		values[47] = this.getChannelMode();
		//48 log interval
		values[48] = this.getLogInterval();
		//49 power option auto start
		values[49] = this.getSaveToSD();
		
		//regenerative channel mode
		values[50] = this.getRegChMode();
		//regenerative to channel voltage limit
		values[51] = this.getRegChVolt() / 100;
		//regenerative to channel current limit
		values[52] = this.getRegChCurrent();

		return values;
	}

	public String getUseFlagAndName(boolean isDuo) {
		StringBuilder sb = new StringBuilder();
		if (this.name[0] == 0) {
			String replaceDeviceCopiedName = isDuo ? BatteryTypesDuo.getValues()[1 + this.getType()] : BatteryTypesX.getValues()[1 + this.getType()];
			replaceDeviceCopiedName += Messages.getString(MessageIds.GDE_MSGT2620);
			sb.append(String.format("%-7s - %s", useFlag == MEM_USED ? "CUSTOM" : "BUILD IN", replaceDeviceCopiedName));
		}
		else
			sb.append(String.format("%-7s - %s", useFlag == MEM_USED ? "CUSTOM" : "BUILD IN", new String(name).trim()));
		return sb.toString();
	}

	
	static int size = 167 + 25; //size of data bytes for values + values for LiHV, LTO, User, Power

	public static int getSize(final boolean isDuo) {
		return isDuo ? 167 + 6 : 167 + 25;
	}

	public short getUseFlag() {
		return useFlag;
	}

	public void setUseFlag(short useFlag) {
		this.useFlag = useFlag;
	}

	public byte[] getName() {
		return name;
	}

	public void setName(String newName) {
		newName = newName.length() > MEM_NAME_LEN ? newName.substring(0, MEM_NAME_LEN) : newName;
		this.name = new byte[MEM_NAME_LEN + 1];
		System.arraycopy(newName.getBytes(), 0, this.name, 0, newName.length());
	}
	

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public byte getAutoSave() {
		return autoSave;
	}

	public void setAutoSave(byte autoSave) {
		this.autoSave = autoSave;
	}

	public byte getLiBalEndMode() {
		return liBalEndMode;
	}

	public void setLiBalEndMode(byte liBalEndMode) {
		this.liBalEndMode = liBalEndMode;
	}

	public byte getLockFlag() {
		return lockFlag;
	}

	public void setLockFlag(byte lockFlag) {
		this.lockFlag = lockFlag;
	}

	public byte[] getLockPWD() {
		return lockPWD;
	}

	public void setLockPWD(byte[] lockPWD) {
		this.lockPWD = lockPWD;
	}

	public short getOpEnable() {
		return opEnable;
	}

	public void setOpEnable(short opEnable) {
		this.opEnable = opEnable;
	}

	public byte getChannelMode() {
		return channelMode;
	}

	public void setChannelMode(byte channelMode) {
		this.channelMode = channelMode;
	}

	public byte getSaveToSD() {
		return saveToSD;
	}

	public void setSaveToSD(byte saveToSD) {
		this.saveToSD = saveToSD;
	}

	public short getLogInterval() {
		return logInterval;
	}

	public void setLogInterval(short logInterval) {
		this.logInterval = logInterval;
	}

	public short getRunCounter() {
		return runCounter;
	}

	public void setRunCounter(short runCounter) {
		this.runCounter = runCounter;
	}

	public byte getType() {
		return type;
	}

	public void setType(byte type) {
		this.type = type;
	}

	public byte getLiCell() {
		return liCell;
	}

	public void setLiCell(byte liCell) {
		this.liCell = liCell;
	}

	public byte getNiCell() {
		return niCell;
	}

	public void setNiCell(byte niCell) {
		this.niCell = niCell;
	}

	public byte getPbCell() {
		return pbCell;
	}

	public void setPbCell(byte pbCell) {
		this.pbCell = pbCell;
	}

	public byte getLiModeC() {
		return liModeC;
	}

	public void setLiModeC(byte liModeC) {
		this.liModeC = liModeC;
	}

	public byte getLiModeD() {
		return liModeD;
	}

	public void setLiModeD(byte liModeD) {
		this.liModeD = liModeD;
	}

	public byte getNiModeC() {
		return niModeC;
	}

	public void setNiModeC(byte niModeC) {
		this.niModeC = niModeC;
	}

	public byte getNiModeD() {
		return niModeD;
	}

	public void setNiModeD(byte niModeD) {
		this.niModeD = niModeD;
	}

	public byte getPbModeC() {
		return pbModeC;
	}

	public void setPbModeC(byte pbModeC) {
		this.pbModeC = pbModeC;
	}

	public byte getPbModeD() {
		return pbModeD;
	}

	public void setPbModeD(byte pbModeD) {
		this.pbModeD = pbModeD;
	}

	public byte getBalSpeed() {
		return balSpeed;
	}

	public void setBalSpeed(byte balSpeed) {
		this.balSpeed = balSpeed;
	}

	public byte getBalStartMode() {
		return balStartMode;
	}

	public void setBalStartMode(byte balStartMode) {
		this.balStartMode = balStartMode;
	}

	public short getBalStartVolt() {
		return balStartVolt;
	}

	public void setBalStartVolt(short balStartVolt) {
		this.balStartVolt = balStartVolt;
	}

	public byte getBalDiff() {
		return balDiff;
	}

	public void setBalDiff(byte balDiff) {
		this.balDiff = balDiff;
	}

	public byte getBalOverPoint() {
		return balOverPoint;
	}

	public void setBalOverPoint(byte balOverPoint) {
		this.balOverPoint = balOverPoint;
	}

	public byte getBalSetPoint() {
		return balSetPoint;
	}

	public void setBalSetPoint(byte balSetPoint) {
		this.balSetPoint = balSetPoint;
	}

	public byte getBalDelay() {
		return balDelay;
	}

	public void setBalDelay(byte balDelay) {
		this.balDelay = balDelay;
	}

	public byte getKeepChargeEnable() {
		return keepChargeEnable;
	}

	public void setKeepChargeEnable(byte keepChargeEnable) {
		this.keepChargeEnable = keepChargeEnable;
	}

	public short getLiPoChgCellVolt() {
		return liPoChgCellVolt;
	}

	public void setLiPoChgCellVolt(short liPoChgCellVolt) {
		this.liPoChgCellVolt = liPoChgCellVolt;
	}

	public short getLiIoChgCellVolt() {
		return liIoChgCellVolt;
	}

	public void setLiIoChgCellVolt(short liLoChgCellVolt) {
		this.liIoChgCellVolt = liLoChgCellVolt;
	}

	public short getLiFeChgCellVolt() {
		return liFeChgCellVolt;
	}

	public void setLiFeChgCellVolt(short liFeChgCellVolt) {
		this.liFeChgCellVolt = liFeChgCellVolt;
	}

	public short getLiPoStoCellVolt() {
		return liPoStoCellVolt;
	}

	public void setLiPoStoCellVolt(short liPoStoCellVolt) {
		this.liPoStoCellVolt = liPoStoCellVolt;
	}

	public short getLiLoStoCellVolt() {
		return liLoStoCellVolt;
	}

	public void setLiLoStoCellVolt(short liLoStoCellVolt) {
		this.liLoStoCellVolt = liLoStoCellVolt;
	}

	public short getLiFeStoCellVolt() {
		return liFeStoCellVolt;
	}

	public void setLiFeStoCellVolt(short liFeStoCellVolt) {
		this.liFeStoCellVolt = liFeStoCellVolt;
	}

	public short getLiPoDchgCellVolt() {
		return liPoDchgCellVolt;
	}

	public void setLiPoDchgCellVolt(short liPoDchgCellVolt) {
		this.liPoDchgCellVolt = liPoDchgCellVolt;
	}

	public short getLiIoDchgCellVolt() {
		return liIoDchgCellVolt;
	}

	public void setLiIoDchgCellVolt(short liLoDchgCellVolt) {
		this.liIoDchgCellVolt = liLoDchgCellVolt;
	}

	public short getLiFeDchgCellVolt() {
		return liFeDchgCellVolt;
	}

	public void setLiFeDchgCellVolt(short liFeDchgCellVolt) {
		this.liFeDchgCellVolt = liFeDchgCellVolt;
	}

	public short getChargeCurrent() {
		return chargeCurrent;
	}

	public void setChargeCurrent(short chargeCurrent) {
		this.chargeCurrent = chargeCurrent;
	}

	public short getDischargeCurrent() {
		return dischargeCurrent;
	}

	public void setDischargeCurrent(short dischargeCurrent) {
		this.dischargeCurrent = dischargeCurrent;
	}

	public short getEndCharge() {
		return endCharge;
	}

	public void setEndCharge(short endCharge) {
		this.endCharge = endCharge;
	}

	public short getEndDischarge() {
		return endDischarge;
	}

	public void setEndDischarge(short endDischarge) {
		this.endDischarge = endDischarge;
	}

	public short getRegDchgMode() {
		return regDchgMode;
	}

	public void setRegDchgMode(short regDchgMode) {
		this.regDchgMode = regDchgMode;
	}

	public short getNiPeak() {
		return niPeak;
	}

	public void setNiPeak(short niPeak) {
		this.niPeak = niPeak;
	}

	public short getNiPeakDelay() {
		return niPeakDelay;
	}

	public void setNiPeakDelay(short niPeakDelay) {
		this.niPeakDelay = niPeakDelay;
	}

	public short getNiTrickleEnable() {
		return niTrickleEnable;
	}

	public void setNiTrickleEnable(short niTrickleEnable) {
		this.niTrickleEnable = niTrickleEnable;
	}

	public short getNiTrickleCurrent() {
		return niTrickleCurrent;
	}

	public void setNiTrickleCurrent(short niTrickleCurrent) {
		this.niTrickleCurrent = niTrickleCurrent;
	}

	public short getNiTrickleTime() {
		return niTrickleTime;
	}

	public void setNiTrickleTime(short niTrickleTime) {
		this.niTrickleTime = niTrickleTime;
	}

	public short getNiZeroEnable() {
		return niZeroEnable;
	}

	public void setNiZeroEnable(short niZeroEnable) {
		this.niZeroEnable = niZeroEnable;
	}

	public short getNiDischargeVolt() {
		return niDischargeVolt;
	}

	public void setNiDischargeVolt(short niDischargeVolt) {
		this.niDischargeVolt = niDischargeVolt;
	}

	public short getPbChgCellVolt() {
		return pbChgCellVolt;
	}

	public void setPbChgCellVolt(short pbChgCellVolt) {
		this.pbChgCellVolt = pbChgCellVolt;
	}

	public short getPbDchgCellVolt() {
		return pbDchgCellVolt;
	}

	public void setPbDchgCellVolt(short pbDchgCellVolt) {
		this.pbDchgCellVolt = pbDchgCellVolt;
	}

	public short getPbFloatEnable() {
		return pbFloatEnable;
	}

	public void setPbFloatEnable(short pbFloatEnable) {
		this.pbFloatEnable = pbFloatEnable;
	}

	public short getPbFloatCellVolt() {
		return pbFloatCellVolt;
	}

	public void setPbFloatCellVolt(short pbFloatCellVolt) {
		this.pbFloatCellVolt = pbFloatCellVolt;
	}

	public short getRestoreVolt() {
		return restoreVolt;
	}

	public void setRestoreVolt(short restoreVolt) {
		this.restoreVolt = restoreVolt;
	}

	public short getRestoreTime() {
		return restoreTime;
	}

	public void setRestoreTime(short restoreTime) {
		this.restoreTime = restoreTime;
	}

	public short getRestoreCurent() {
		return restoreCurent;
	}

	public void setRestoreCurent(short restoreCurent) {
		this.restoreCurent = restoreCurent;
	}

	public short getCycleCount() {
		return cycleCount;
	}

	public void setCycleCount(short cycleCount) {
		this.cycleCount = cycleCount;
	}

	public short getCycleDelay() {
		return cycleDelay;
	}

	public void setCycleDelay(short cycleDelay) {
		this.cycleDelay = cycleDelay;
	}

	public byte getCycleMode() {
		return cycleMode;
	}

	public void setCycleMode(byte cycleMode) {
		this.cycleMode = cycleMode;
	}

	public short getSafetyTimeC() {
		return safetyTimeC;
	}

	public void setSafetyTimeC(short safetyTimeC) {
		this.safetyTimeC = safetyTimeC;
	}

	public short getSafetyCapC() {
		return safetyCapC;
	}

	public void setSafetyCapC(short safetyCapC) {
		this.safetyCapC = safetyCapC;
	}

	public short getSafetyTempC() {
		return safetyTempC;
	}

	public void setSafetyTempC(short safetyTempC) {
		this.safetyTempC = safetyTempC;
	}

	public short getSafetyTimeD() {
		return safetyTimeD;
	}

	public void setSafetyTimeD(short safetyTimeD) {
		this.safetyTimeD = safetyTimeD;
	}

	public short getSafetyCapD() {
		return safetyCapD;
	}

	public void setSafetyCapD(short safetyCapD) {
		this.safetyCapD = safetyCapD;
	}

	public short getSafetyTempD() {
		return safetyTempD;
	}

	public void setSafetyTempD(short safetyTempD) {
		this.safetyTempD = safetyTempD;
	}

	public byte getRegChMode() {
		return regChMode;
	}

	public void setRegChMode(byte regChMode) {
		this.regChMode = regChMode;
	}

	public short getRegChVolt() {
		return regChVolt;
	}

	public void setRegChVolt(short regChVolt) {
		this.regChVolt = regChVolt;
	}

	public short getRegChCurrent() {
		return regChCurrent;
	}

	public void setRegChCurrent(short regChCurrent) {
		this.regChCurrent = regChCurrent;
	}

	public byte getFastSto() {
		return fastSto;
	}

	public void setFastSto(byte fastSto) {
		this.fastSto = fastSto;
	}

	public short getStoCompensation() {
		return stoCompensation;
	}

	public void setStoCompensation(short stoCompensation) {
		this.stoCompensation = stoCompensation;
	}

	public short getNiZnChgCellVolt() {
		return niZnChgCellVolt;
	}

	public void setNiZnChgCellVolt(short niZnChgCellVolt) {
		this.niZnChgCellVolt = niZnChgCellVolt;
	}

	public short getNiZnDchgCellVolt() {
		return niZnDchgCellVolt;
	}

	public void setNiZnDchgCellVolt(short niZnDchgCellVolt) {
		this.niZnDchgCellVolt = niZnDchgCellVolt;
	}

	public byte getNiZnCell() {
		return niZnCell;
	}

	public void setNiZnCell(byte niZnCell) {
		this.niZnCell = niZnCell;
	}

	public short getLiHVChgCellVolt() {
		return liHVChgCellVolt;
	}

	public void setLiHVChgCellVolt(short liHVChgCellVolt) {
		this.liHVChgCellVolt = liHVChgCellVolt;
	}

	public short getLiHVStoCellVolt() {
		return liHVStoCellVolt;
	}

	public void setLiHVStoCellVolt(short liHVStoCellVolt) {
		this.liHVStoCellVolt = liHVStoCellVolt;
	}

	public short getLiHVDchgCellVolt() {
		return liHVDchgCellVolt;
	}

	public void setLiHVDchgCellVolt(short liHVDchgCellVolt) {
		this.liHVDchgCellVolt = liHVDchgCellVolt;
	}

	public short getLtoChgCellVolt() {
		return ltoChgCellVolt;
	}

	public void setLtoChgCellVolt(short ltoChgCellVolt) {
		this.ltoChgCellVolt = ltoChgCellVolt;
	}

	public short getLtoStoCellVolt() {
		return ltoStoCellVolt;
	}

	public void setLtoStoCellVolt(short ltoStoCellVolt) {
		this.ltoStoCellVolt = ltoStoCellVolt;
	}

	public short getLtoDchgCellVolt() {
		return ltoDchgCellVolt;
	}

	public void setLtoDchgCellVolt(short ltoDchgCellVolt) {
		this.ltoDchgCellVolt = ltoDchgCellVolt;
	}

	public short getUserChgCellVolt() {
		return userChgCellVolt;
	}

	public void setUserChgCellVolt(short userChgCellVolt) {
		this.userChgCellVolt = userChgCellVolt;
	}

	public short getUserStoCellVolt() {
		return userStoCellVolt;
	}

	public void setUserStoCellVolt(short userStoCellVolt) {
		this.userStoCellVolt = userStoCellVolt;
	}

	public short getUserDchgCellVolt() {
		return userDchgCellVolt;
	}

	public void setUserDchgCellVolt(short userDchgCellVolt) {
		this.userDchgCellVolt = userDchgCellVolt;
	}

	public byte getUserCell() {
		return userCell;
	}

	public void setUserCell(byte userCell) {
		this.userCell = userCell;
	}

	public int getDigitPowerVolt() {
		return Short.toUnsignedInt(this.digitPowerVolt);
	}

	public void setDigitPowerVolt(short digitPowerVolt) {
		this.digitPowerVolt = digitPowerVolt;
	}

	public short getDigitPowerCurrent() {
		return digitPowerCurrent;
	}

	public void setDigitPowerCurrent(short digitPowerCurrent) {
		this.digitPowerCurrent = digitPowerCurrent;
	}

	public short getDigitPowerSet() {
		return digitPowerSet;
	}

	public void setDigitPowerSet(short digitPowerSet) {
		this.digitPowerSet = digitPowerSet;
	}

	public byte getDump() {
		return dump;
	}

	public void setDump(byte dump) {
		this.dump = dump;
	}

}
