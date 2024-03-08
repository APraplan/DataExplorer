/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.ardupilot;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.Record.DataType;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.DataInconsitsentException;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;

/**
 * @author brueg
 *
 */
public class ArduPilot extends DeviceConfiguration implements IDevice {
	final static Logger			log					= Logger.getLogger(ArduPilot.class.getName());

	protected final DataExplorer				application;
	protected final Channels						channels;
	protected final Settings 						settings = Settings.getInstance();

	/**
	 * defining all log structures according to LogStructure.h
	 */
	public enum LogEntry {
		ACC, ARSP, BARO, CURR, CURR_CELL, ESC, GPA, GPS, GYR, IMT, IMU, ISBD, ISBH, MAG, PID, QUAT, UNKNOWN;
		
		public static final LogEntry	VALUES[]	= values();	
		
		public static LogEntry getLogEntry(String logEntry) {
			for (LogEntry entry : VALUES) {
				if (entry.name().equals(logEntry))
					return entry;
			}
			return UNKNOWN;
		}
	}
	
	/**
		#define ACC_LABELS "TimeUS,SampleUS,AccX,AccY,AccZ"
		#define ACC_FMT   "QQfff"
		#define ACC_UNITS "ssnnn"
		#define ACC_MULTS "FF000"
	 */
	public enum ACC {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		SampleUS("Q" , "s", "F"),
		Sample("Q" , "s", "F"),
		AccX("B" , "-", "-"),
		AccY("I" , "-", "-"),
		AccZ("H" , "-", "-");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final ACC	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private ACC(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
				
		public static String getFmt(String entry) {
			for (ACC acc : VALUES) {
				if (acc.name().equals(entry))
					return acc.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (ACC acc : VALUES) {
				if (acc.name().equals(entry))
					return acc.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (ACC acc : VALUES) {
				if (acc.name().equals(entry))
					return ArduPilot.getUnit(acc.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (ACC acc : VALUES) {
				if (acc.name().equals(entry))
					return  ArduPilot.getMults(acc.mults);
			}
			return 1;
		}
	}

	/**
		#define BARO_LABELS "TimeUS,Alt,Press,Temp,CRt,SMS,Offset,GndTemp"
		#define BARO_FMT   "QffcfIff"
		#define BARO_UNITS "smPOnsmO"
		#define BARO_MULTS "F00B0C?0"
	 */
	public enum BARO {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		Alt("f" , "m", "0"),
		Press("f" , "P", "0"),
		Temp("c" , "O", "B"),
		CRt("f" , "n", "0"),
		SMS("I" , "s", "C"),
		Offset("f" , "m", "?"),
		GndTemp("f" , "O", "0");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final BARO	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private BARO(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
			
		public static String getFmt(String entry) {
			for (BARO baro : VALUES) {
				if (baro.name().equals(entry))
					return baro.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (BARO baro : VALUES) {
				if (baro.name().equals(entry))
					return baro.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (BARO baro : VALUES) {
				if (baro.name().equals(entry))
					return ArduPilot.getUnit(baro.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (BARO baro : VALUES) {
				if (baro.name().equals(entry))
					return  ArduPilot.getMults(baro.mults);
			}
			return 1;
		}
	}

	/**
		#define ESC_LABELS "TimeUS,RPM,Volt,Curr,Temp,CTot"
		#define ESC_FMT   "QeCCcH"
		#define ESC_UNITS "sqvAO-"
		#define ESC_MULTS "FBBBB-"
	 */
	public enum ESC {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		RPM("e" , "q", "B"),
		Volt("C" , "v", "B"),
		Curr("C" , "A", "B"),
		Temp("c" , "O", "B"),
		CTot("H" , "-", "-");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final ESC	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private ESC(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (ESC esc : VALUES) {
				if (esc.name().equals(entry))
					return esc.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (ESC esc : VALUES) {
				if (esc.name().equals(entry))
					return esc.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (ESC esc : VALUES) {
				if (esc.name().equals(entry))
					return ArduPilot.getUnit(esc.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (ESC esc : VALUES) {
				if (esc.name().equals(entry))
					return  ArduPilot.getMults(esc.mults);
			}
			return 1;
		}
	}

	/**
		#define GPA_LABELS "TimeUS,VDop,HAcc,VAcc,SAcc,VV,SMS,Delta"
		#define GPA_FMT   "QCCCCBIH"
		#define GPA_UNITS "smmmn-ss"
		#define GPA_MULTS "FBBBB-CF"
	 */
	public enum GPA {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		VDop("C" , "m", "B"),
		HAcc("C" , "m", "B"),
		VAcc("C" , "m", "B"),
		SAcc("C" , "n", "B"),
		VV("B" , "-", "-"),
		SMS("I" , "s", "C"),
		Delta("H" , "s", "F");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final GPA	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private GPA(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (GPA gpa : VALUES) {
				if (gpa.name().equals(entry))
					return gpa.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (GPA gpa : VALUES) {
				if (gpa.name().equals(entry))
					return gpa.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (GPA gpa : VALUES) {
				if (gpa.name().equals(entry))
					return ArduPilot.getUnit(gpa.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (GPA gpa : VALUES) {
				if (gpa.name().equals(entry))
					return  ArduPilot.getMults(gpa.mults);
			}
			return 1;
		}
	}

	/**
	  *	#define GPS_LABELS "TimeUS,Status,GMS,GWk,NSats,HDop,Lat,Lng,Alt,Spd,GCrs,VZ,U"
	  *	#define GPS_FMT    "QBIHBcLLefffB"
	  *	#define GPS_UNITS  "s---SmDUmnhn-"
	  *	#define GPS_MULTS  "F---0BGGB000-"
	 */
	public enum GPS {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		Status("B" , "-", "-"),
		GMS("I" , "-", "-"),
		GWk("H" , "-", "-"),
		NSats("B" , "S", "0"),
		HDop("c" , "m", "B"),
		Lat("L" , "D", "G"),
		Lng("L" , "U", "G"),
		Alt("e" , "m", "B"),
		RelAlt("e" , "m", "B"),
		Spd("f" , "n", "0"),
		GCrs("f" , "h", "0"),
		VZ("f" , "n", "0"),
		U("B" , "-", "-");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final GPS	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private GPS(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (GPS gps : VALUES) {
				if (gps.name().equals(entry))
					return gps.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (GPS gps : VALUES) {
				if (gps.name().equals(entry))
					return gps.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (GPS gps : VALUES) {
				if (gps.name().equals(entry))
					return ArduPilot.getUnit(gps.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (GPS gps : VALUES) {
				if (gps.name().equals(entry))
					return  ArduPilot.getMults(gps.mults);
			}
			return 1;
		}
	}

	/**
		#define GYR_LABELS "TimeUS,SampleUS,GyrX,GyrY,GyrZ"
		#define GYR_FMT    "QQfff"
		#define GYR_UNITS  "ssEEE"
		#define GYR_MULTS  "FF000"
	 */
	public enum GYR {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		SampleUS("Q" , "s", "F"),
		Sample("Q" , "s", "F"),
		GyrX("f" , "E", "0"),
		GyrY("f" , "E", "0"),
		GyrZ("f" , "E", "0");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final GYR	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private GYR(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (GYR gyr : VALUES) {
				if (gyr.name().equals(entry))
					return gyr.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (GYR gyr : VALUES) {
				if (gyr.name().equals(entry))
					return gyr.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (GYR gyr : VALUES) {
				if (gyr.name().equals(entry))
					return ArduPilot.getUnit(gyr.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (GYR gyr : VALUES) {
				if (gyr.name().equals(entry))
					return  ArduPilot.getMults(gyr.mults);
			}
			return 1;
		}
	}

	/**
		#define IMT_LABELS "TimeUS,DelT,DelvT,DelaT,DelAX,DelAY,DelAZ,DelVX,DelVY,DelVZ"
		#define IMT_FMT    "Qfffffffff"
		#define IMT_UNITS  "ssssrrrnnn"
		#define IMT_MULTS  "FF00000000"
	 */
	public enum IMT {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		DelT("f" , "s", "F"),
		DelvT("f" , "s", "0"),
		DelaT("f" , "s", "0"),
		DelAX("f" , "r", "0"),
		DelAY("f" , "r", "0"),
		DelAZ("f" , "r", "0"),
		DelVX("f" , "n", "0"),
		DelVY("f" , "n", "0"),
		DelVZ("f" , "n", "0");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final IMT	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private IMT(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (IMT imt : VALUES) {
				if (imt.name().equals(entry))
					return imt.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (IMT imt : VALUES) {
				if (imt.name().equals(entry))
					return imt.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (IMT imt : VALUES) {
				if (imt.name().equals(entry))
					return ArduPilot.getUnit(imt.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (IMT imt : VALUES) {
				if (imt.name().equals(entry))
					return  ArduPilot.getMults(imt.mults);
			}
			return 1;
		}
	}

	/**
		#define ISBH_LABELS "TimeUS,N,type,instance,mul,smp_cnt,SampleUS,smp_rate"
		#define ISBH_FMT    "QHBBHHQf"
		#define ISBH_UNITS  "s-----sz"
		#define ISBH_MULTS  "F-----F-"
	 */
	public enum ISBH {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		N("H" , "-", "-"),
		type("B" , "-", "-"),
		instance("B" , "-", "-"),
		mul("H" , "-", "-"),
		smp_cnt("H" , "-", "-"),
		SampleUS("Q" , "s", "F"),
		Sample("Q" , "s", "F"),
		smp_rate("f" , "z", "-");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final ISBH	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private ISBH(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (ISBH isbh : VALUES) {
				if (isbh.name().equals(entry))
					return isbh.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (ISBH isbh : VALUES) {
				if (isbh.name().equals(entry))
					return isbh.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (ISBH isbh : VALUES) {
				if (isbh.name().equals(entry))
					return ArduPilot.getUnit(isbh.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (ISBH isbh : VALUES) {
				if (isbh.name().equals(entry))
					return  ArduPilot.getMults(isbh.mults);
			}
			return 1;
		}
	}

	/**
		#define ISBD_LABELS "TimeUS,N,seqno,x,y,z"
		#define ISBD_FMT    "QHHaaa"
		#define ISBD_UNITS  "s--ooo"
		#define ISBD_MULTS  "F--???"
	 */
	public enum ISBD {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		N("H" , "-", "-"),
		seqno("H" , "-", "-"),
		x("a" , "o", "?"),
		y("a" , "o", "?"),
		z("a" , "o", "?");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final ISBD	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private ISBD(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (ISBD isbd : VALUES) {
				if (isbd.name().equals(entry))
					return isbd.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (ISBD isbd : VALUES) {
				if (isbd.name().equals(entry))
					return isbd.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (ISBD isbd : VALUES) {
				if (isbd.name().equals(entry))
					return ArduPilot.getUnit(isbd.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (ISBD isbd : VALUES) {
				if (isbd.name().equals(entry))
					return  ArduPilot.getMults(isbd.mults);
			}
			return 1;
		}
	}

	/**
		#define IMU_LABELS "TimeUS,GyrX,GyrY,GyrZ,AccX,AccY,AccZ,EG,EA,T,GH,AH,GHz,AHz"
		#define IMU_FMT   "QffffffIIfBBHH"
		#define IMU_UNITS "sEEEooo--O--zz"
		#define IMU_MULTS "F000000-----00"
	 */
	public enum IMU {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		GyrX("f" , "E", "0"),
		GyrY("f" , "E", "0"),
		GyrZ("f" , "E", "0"),
		AccX("f" , "o", "0"),
		AccY("f" , "o", "0"),
		AccZ("f" , "o", "0"),
		EG("I" , "-", "-"),
		EA("I" , "-", "-"),
		T("f" , "O", "-"),
		GH("B" , "-", "-"),
		AH("B" , "-", "-"),
		GHz("H" , "z", "0"),
		AHz("H" , "z", "0");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final IMU	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private IMU(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (IMU imu : VALUES) {
				if (imu.name().equals(entry))
					return imu.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (IMU imu : VALUES) {
				if (imu.name().equals(entry))
					return imu.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (IMU imu : VALUES) {
				if (imu.name().equals(entry))
					return ArduPilot.getUnit(imu.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (IMU imu : VALUES) {
				if (imu.name().equals(entry))
					return  ArduPilot.getMults(imu.mults);
			}
			return 1;
		}
	}

	/**
		#define MAG_LABELS "TimeUS,MagX,MagY,MagZ,OfsX,OfsY,OfsZ,MOfsX,MOfsY,MOfsZ,Health,S"
		#define MAG_FMT   "QhhhhhhhhhBI"
		#define MAG_UNITS "sGGGGGGGGG-s"
		#define MAG_MULTS "FCCCCCCCCC-F"
	 */
	public enum MAG {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		MagX("h" , "G", "C"),
		MagY("h" , "G", "C"),
		MagZ("h" , "G", "C"),
		OfsX("h" , "G", "C"),
		OfsY("h" , "G", "C"),
		OfsZ("h" , "G", "C"),
		MOfsX("h" , "G", "C"),
		MOfsY("h" , "G", "C"),
		MOfsZ("h" , "G", "C"),
		Health("B" , "-", "-"),
		S("I" , "s", "F");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final MAG	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private MAG(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (MAG mag : VALUES) {
				if (mag.name().equals(entry))
					return mag.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (MAG mag : VALUES) {
				if (mag.name().equals(entry))
					return mag.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (MAG mag : VALUES) {
				if (mag.name().equals(entry))
					return ArduPilot.getUnit(mag.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (MAG mag : VALUES) {
				if (mag.name().equals(entry))
					return  ArduPilot.getMults(mag.mults);
			}
			return 1;
		}
	}

	/**
		#define PID_LABELS "TimeUS,Des,Act,P,I,D,FF"
		#define PID_FMT    "Qffffff"
		#define PID_UNITS  "s------"
		#define PID_MULTS  "F------"
	 */
	public enum PID {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		Des("f" , "-", "-"),
		Act("f" , "-", "-"),
		P("f" , "-", "-"),
		I("f" , "-", "-"),
		D("f" , "-", "-"),
		FF("f" , "-", "-");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final PID	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private PID(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (PID pid : VALUES) {
				if (pid.name().equals(entry))
					return pid.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (PID pid : VALUES) {
				if (pid.name().equals(entry))
					return pid.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (PID pid : VALUES) {
				if (pid.name().equals(entry))
					return ArduPilot.getUnit(pid.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (PID pid : VALUES) {
				if (pid.name().equals(entry))
					return  ArduPilot.getMults(pid.mults);
			}
			return 1;
		}
	}

	/**
		#define QUAT_LABELS "TimeUS,Q1,Q2,Q3,Q4"
		#define QUAT_FMT    "Qffff"
		#define QUAT_UNITS  "s????"
		#define QUAT_MULTS  "F????"
	 */
	public enum QUAT {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		Q1("f" , "?", "?"),
		Q2("f" , "?", "?"),
		Q3("f" , "?", "?"),
		Q4("f" , "?", "?");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final QUAT	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private QUAT(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (QUAT quad : VALUES) {
				if (quad.name().equals(entry))
					return quad.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (QUAT quat : VALUES) {
				if (quat.name().equals(entry))
					return quat.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (QUAT quat : VALUES) {
				if (quat.name().equals(entry))
					return ArduPilot.getUnit(quat.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (QUAT quat : VALUES) {
				if (quat.name().equals(entry))
					return  ArduPilot.getMults(quat.mults);
			}
			return 1;
		}
	}

	/**
		#define CURR_LABELS "TimeUS,Volt,VoltR,Curr,CurrTot,EnrgTot,Temp,Res"
		#define CURR_FMT    "Qfffffcf"
		#define CURR_UNITS  "svvA?JOw"
		#define CURR_MULTS  "F000?/?0"
	 */
	public enum CURR {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		Volt("f" , "v", "0"),
		VoltR("f" , "v", "0"),
		Curr("f" , "A", "0"),
		CurrTot("f" , "?", "?"),
		EnrgTot("f" , "J", "/"),
		Temp("c" , "O", "?"),
		Res("f" , "w", "0");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final CURR	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private CURR(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (CURR curr : VALUES) {
				if (curr.name().equals(entry))
					return curr.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (CURR curr : VALUES) {
				if (curr.name().equals(entry))
					return curr.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (CURR curr : VALUES) {
				if (curr.name().equals(entry))
					return ArduPilot.getUnit(curr.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (CURR curr : VALUES) {
				if (curr.name().equals(entry))
					return  ArduPilot.getMults(curr.mults);
			}
			return 1;
		}
	}

	/**
		#define CURR_CELL_LABELS "TimeUS,Volt,V1,V2,V3,V4,V5,V6,V7,V8,V9,V10"
		#define CURR_CELL_FMT    "QfHHHHHHHHHH"
		#define CURR_CELL_UNITS  "svvvvvvvvvvv"
		#define CURR_CELL_MULTS  "F00000000000"
	 */
	public enum CURR_CELL {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		Volt("f" , "v", "0"),
		V1("H" , "v", "0"),
		V2("H" , "v", "0"),
		V3("H" , "v", "0"),
		V4("H" , "v", "0"),
		V5("H" , "v", "0"),
		V6("H" , "v", "0"),
		V7("H" , "v", "0"),
		V8("H" , "v", "0"),
		V9("H" , "v", "0"),
		V10("H" , "v", "0");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final CURR_CELL	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private CURR_CELL(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (CURR_CELL cell : VALUES) {
				if (cell.name().equals(entry))
					return cell.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (CURR_CELL cell : VALUES) {
				if (cell.name().equals(entry))
					return cell.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (CURR_CELL cell : VALUES) {
				if (cell.name().equals(entry))
					return ArduPilot.getUnit(cell.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (CURR_CELL cell : VALUES) {
				if (cell.name().equals(entry))
					return  ArduPilot.getMults(cell.mults);
			}
			return 1;
		}
	}

	/**
		#define ARSP_LABELS "TimeUS,Airspeed,DiffPress,Temp,RawPress,Offset,U,Health,Primary"
		#define ARSP_FMT "QffcffBBB"
		#define ARSP_UNITS "snPOPP---"
		#define ARSP_MULTS "F00B00---"
	 */
	public enum ARSP {
		TimeUS("Q" , "s", "F"),
		Time("Q" , "s", "F"),
		Airspeed("f" , "n", "0"),
		DiffPress("f" , "P", "0"),
		Temp("f" , "O", "B"),
		RawPress("f" , "P", "0"),
		Offset("f" , "P", "0"),
		U("B" , "-", "-"),
		Health("B" , "-", "-"),
		Primary("B" , "-", "-");

		private final String				fmt;
		private final String				unit;
		private final String				mults;
		public static final ARSP	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private ARSP(String _fmt, String _unit, String _mults) {
			this.fmt = _fmt;
			this.unit = _unit;
			this.mults = _mults;
		}
		
		public static String getFmt(String entry) {
			for (ARSP arsp : VALUES) {
				if (arsp.name().equals(entry))
					return arsp.fmt;
			}
			return "i"; //uint32_t
		}

		public static String getSymbol(String entry) {
			for (ARSP arsp : VALUES) {
				if (arsp.name().equals(entry))
					return arsp.unit;
			}
			return GDE.STRING_EMPTY;
		}

		public static String getUnit(String entry) {
			for (ARSP arsp : VALUES) {
				if (arsp.name().equals(entry))
					return ArduPilot.getUnit(arsp.unit);
			}
			return GDE.STRING_EMPTY;
		}

		public static double getMults(String entry) {
			for (ARSP arsp : VALUES) {
				if (arsp.name().equals(entry))
					return  ArduPilot.getMults(arsp.mults);
			}
			return 1;
		}
	}

	/**
	 * Format characters in the format string for binary log messages
	 */
	public static Object parseValue(String fmt, String value) throws NumberFormatException {
		switch (fmt.charAt(0)) {
		  case 'a'://int16_t[32]
		  	short[] parsedValue = new short[32];
		  	int index = 0;
		  	for (byte b: value.getBytes()) {
		  		parsedValue[index++] = b;
		  	}
		  	return parsedValue;
		  case 'b'://int8_t
		  case 'B'://uint8_t
		  	return Byte.valueOf(value) * 1000;
		  case 'h'://int16_t
		  case 'H'://uint16_t
		  	return Short.valueOf(value) * 1000;
		  case 'i'://int32_t
		  case 'I'://uint32_t
		  	return Integer.valueOf(value) * 1000;
		  case 'f'://float
		  	return Float.valueOf(value).intValue() * 1000;
		  case 'd'://double
		  	return Double.valueOf(value).intValue() * 1000;
		  case 'n'://char[4]
		  	char[] char4 = new char[4];
		  	for (int i = 0; i < char4.length; i++) {
		  		char4[i] = (char) value.getBytes()[i];
				}
		  	return char4;
		  case 'N'://char[16]
		  	char[] char16 = new char[16];
		  	for (int i = 0; i < char16.length; i++) {
		  		char16[i] = (char) value.getBytes()[i];
				}
		  	return char16;
		  case 'Z'://char[64]
		  	char[] char64 = new char[64];
		  	for (int i = 0; i < char64.length; i++) {
		  		char64[i] = (char) value.getBytes()[i];
				}
		  	return char64;
		  case 'c'://int16_t * 100
		  case 'C'://uint16_t * 100
		  	return Short.valueOf(value.replace(GDE.STRING_DOT, GDE.STRING_EMPTY)) * 1000;
		  case 'e'://int32_t * 100
		  case 'E'://uint32_t * 100
		  	return Integer.valueOf(value.replace(GDE.STRING_DOT, GDE.STRING_EMPTY)) * 1000;
		  default:
		  case 'L'://int32_t latitude/longitude
		  	return Integer.valueOf(value.replace(GDE.STRING_DOT, GDE.STRING_EMPTY));
		  case 'M'://uint8_t flight mode
		  	return Byte.valueOf(value) * 1000;
		  case 'q'://int64_t
		  case 'Q'://uint64_t
		  	return Long.valueOf(value) * 1000;
		}
	}
	
	public static String getUnit(String s) {
		switch (s.charAt(0)) {
		case '-': return "";							// no units e.g. Pi, or a string
		case '?': return "UNKNOWN";				// Units which haven't been worked out yet....
		case 'A': return "A";							// Ampere
		case 'd': return "°";							// of the angular variety, -180 to 180
		case 'b': return "B";							// bytes
		case 'k': return "°/s";						// degrees per second. Degrees are NOT SI, but is some situations more user-friendly than radians
		case 'D': return "°";							// degrees of latitude
		case 'e': return "°/s/s";       	// degrees per second per second. Degrees are NOT SI, but is some situations more user-friendly than radians
		case 'E': return "rad/s";         // radians per second
		case 'G': return "Gauss";         // Gauss is not an SI unit, but 1 tesla = 10000 gauss so a simple replacement is not possible here
		case 'h': return "°";    					// 0.? to 359.?
		case 'i': return "A.s";           // Ampere second
		case 'J': return "W.s";           // Joule (Watt second)
		//case 'l': return "l";          	// litres
		case 'L': return "rad/s/s";       // radians per second per second
		case 'm': return "m";             // metres
		case 'n': return "m/s";           // metres per second
		//case 'N': return "N";          	// Newton
		case 'o': return "m/s/s";         // metres per second per second
		case 'O': return "°C";          	// degrees Celsius. Not SI, but Kelvin is too cumbersome for most users
		case '%': return "%";             // percent
		case 'S': return "satellites";    // number of satellites
		case 's': return "s";             // seconds
		case 'q': return "rpm";           // rounds per minute. Not SI, but sometimes more intuitive than Hertz
		case 'r': return "rad";           // radians
		case 'U': return "°";  						// degrees of longitude
		case 'u': return "ppm";           // pulses per minute
		case 'Y': return "us";          	// pulse width modulation in microseconds
		case 'v': return "V";             // Volt
		case 'P': return "Pa";            // Pascal
		case 'w': return "Ω";           	// Ohm
		case 'z': return "Hz";            // Hertz
		default: return GDE.STRING_EMPTY;
		}
	}
	
	public static double getMults(String s) {
		switch (s.charAt(0)) {
		case '-': return 0;				// no multiplier e.g. a string
	  case '?': return 1;       // multipliers which haven't been worked out yet....
	  // <leave a gap here, just in case....>
    case '2': return 1e2;
    case '1': return 1e1;
    case '0': return 1e0;
    case 'A': return 1e-1;
    case 'B': return 1e-2;
    case 'C': return 1e-3;
    case 'D': return 1e-4;
    case 'E': return 1e-5;
    case 'F': return 1e-6;
    case 'G': return 1e-7;
    // <leave a gap here, just in case....>
	  case '!': return 3.6; 		// (ampere*second => milliampere*hour) and (km/h => m/s)
	  case '/': return 3600; 		// (ampere*second => ampere*hour)
	  default: return 0;
		}
	}

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public ArduPilot(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.ardupilot.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT4000), Messages.getString(MessageIds.GDE_MSGT4000));
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public ArduPilot(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.ardupilot.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT4000), Messages.getString(MessageIds.GDE_MSGT4000));
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
		}
	}

	/**
	 * update the file import menu by adding new entry to import device specific files
	 * @param importMenue
	 */
	public void updateFileImportMenu(Menu importMenue) {
		MenuItem importDeviceLogItem;

		if (importMenue.getItem(importMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {
			new MenuItem(importMenue, SWT.SEPARATOR);

			importDeviceLogItem = new MenuItem(importMenue, SWT.PUSH);
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT4005, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT4005));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					importLogFiles();
				}
			});
		}
	}

	/**
	 * update the file export menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileExportMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZDAbsoluteItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT4002));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "convertKMZ3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT4003));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT4004));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});
		}
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	public void export2KMZ3D(int type) {
		RecordSet activeRecordSet = application.getActiveRecordSet();
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT4001), 
				findRecordByType(activeRecordSet, Record.DataType.GPS_LONGITUDE),
				findRecordByType(activeRecordSet, Record.DataType.GPS_LATITUDE), 
				findRecordByType(activeRecordSet, Record.DataType.GPS_ALTITUDE), 
				findRecordByUnit(activeRecordSet, "km/h"),
				findRecordByUnit(activeRecordSet, "m/s"), 
				findRecordByUnit(activeRecordSet, "km"), -1, 
				type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}

	private int findRecordByUnit(RecordSet recordSet, String unit) {
		for (int i = 0; i < recordSet.size(); i++) {
			Record record = recordSet.get(i);
			if (record.getUnit().equalsIgnoreCase(unit)) return record.getOrdinal();
		}
		return -1;
	}

	private int findRecordByType(RecordSet recordSet, Record.DataType dataType) {
		for (int i = 0; i < recordSet.size(); i++) {
			Record record = recordSet.get(i);
			if (record.getDataType().equals(dataType) && record.hasReasonableData()) return record.getOrdinal();
		}
		return -1;
	}

	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	@Override
	public String exportFile(String fileEndingType, boolean isExportTmpDir) {
		String exportFileName = GDE.STRING_EMPTY;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && fileEndingType.contains(GDE.FILE_ENDING_KMZ)) {
				exportFileName = new FileHandler().exportFileKMZ(
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LONGITUDE), 
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_LATITUDE),
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_ALTITUDE), 
						activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_SPEED),
						-1, //climb
						-1, //distance 
						-1, //azimuth
						true, isExportTmpDir);
			}
		}
		return exportFileName;
	}

	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// ArduPilot unknown to LogView
		return null;
	}

	@Override
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ArduPilot unknown to LogView
		return null;
	}

	@Override
	public int getLovDataByteSize() {
		// ArduPilot unknown to LogView
		return 0;
	}

	@Override
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		// unknown to LogView
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
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
		int[] points = new int[recordSet.getNoneCalculationRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 1;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		int index = 0;
		for (int i = 0; i < recordDataSize; i++) {
			index = i * dataBufferSize + timeStampBufferSize;
			if (log.isLoggable(java.util.logging.Level.FINER)) log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + index); //$NON-NLS-1$

			for (int j = 0; j < points.length; j++) {
				points[j] = (((dataBuffer[0 + (j * 4) + index] & 0xff) << 24) + ((dataBuffer[1 + (j * 4) + index] & 0xff) << 16) + ((dataBuffer[2 + (j * 4) + index] & 0xff) << 8) + ((dataBuffer[3 + (j * 4)
						+ index] & 0xff) << 0));
			}

			recordSet.addPoints(points,
					(((dataBuffer[0 + (i * 4)] & 0xff) << 24) + ((dataBuffer[1 + (i * 4)] & 0xff) << 16) + ((dataBuffer[2 + (i * 4)] & 0xff) << 8) + ((dataBuffer[3 + (i * 4)] & 0xff) << 0)) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}
	
	/**
	 * check and adapt stored measurement properties against actual record set records which gets created by device properties XML
	 * - calculated measurements could be later on added to the device properties XML
	 * - devices with battery cell voltage does not need to all the cell curves which does not contain measurement values
	 * @param fileRecordsProperties - all the record describing properties stored in the file
	 * @param recordSet - the record sets with its measurements build up with its measurements from device properties XML
	 * @return string array of measurement names which match the ordinal of the record set requirements to restore file record properties
	 */
	@Override
	public String[] crossCheckMeasurements(String[] fileRecordsProperties, RecordSet recordSet) {
		//return unchanged key set, reordSet was build using fileRecordsProperties keys
		return recordSet.getRecordNames();
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
				double factor = record.getFactor(); // != 1 if a unit translation is required
				DataType dataType = record.getDataType();
				if (dataType == Record.DataType.GPS_LATITUDE || dataType == Record.DataType.GPS_LONGITUDE) {
					dataTableRow[index + 1] = String.format("%09.6f", record.realGet(rowIndex) * factor); //$NON-NLS-1$
				}
				else {
					dataTableRow[index + 1] = record.getDecimalFormat().format((offset + (record.realGet(rowIndex) / 1000.0) * factor));
				}
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
		DataType dataType = record.getDataType();
		double newValue;
		if (dataType == Record.DataType.GPS_LATITUDE || dataType == Record.DataType.GPS_LONGITUDE)
			newValue = 1000 * value * factor;
		else
			newValue = value * factor;
		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		DataType dataType = record.getDataType();
		double newValue;
		if (dataType == Record.DataType.GPS_LATITUDE || dataType == Record.DataType.GPS_LONGITUDE)
			newValue = 1000 * value / factor;
		else
			newValue = value / factor;
		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				int displayableCounter = 0;

				// check if measurements isActive == false and set to isDisplayable == false
				for (String measurementKey : recordSet.keySet()) {
					Record record = recordSet.get(measurementKey);

					if (record.isActive() && (record.getOrdinal() <= 6 || record.hasReasonableData())) {
						++displayableCounter;
					}
				}

				log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
				recordSet.setConfiguredDisplayable(displayableCounter);

				if (recordSet.getName().equals(this.channels.getActiveChannel().getActiveRecordSet().getName())) {
					this.application.updateGraphicsWindow();
				}
			}
			catch (RuntimeException e) {
				log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			}
		}
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

		recordSet.setAllDisplayable();
		for (int i = 0; i < recordSet.realSize(); ++i) {
			Record record = recordSet.get(i);
			record.setDisplayable(record.hasReasonableData());
			if (log.isLoggable(java.util.logging.Level.FINER)) log.log(java.util.logging.Level.FINER, record.getName() + " setDisplayable=" + record.hasReasonableData());
		}

		if (log.isLoggable(java.util.logging.Level.FINE)) {
			for (int i = 0; i < recordSet.size(); i++) {
				Record record = recordSet.get(i);
				log.log(java.util.logging.Level.FINE, record.getName() + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable());
			}
		}
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.FACTOR, Record.DataType.GPS_LATITUDE.value(), Record.DataType.GPS_LONGITUDE.value(), Record.DataType.GPS_ALTITUDE.value(),
				Record.DataType.GPS_AZIMUTH.value(), Record.DataType.GPS_SPEED.value(), "statistics" };
	}

	/**
	 * query if the measurements get build up dynamically while reading (import) the data
	 * the implementation must create measurementType while reading the import data,
	 * refer to Weatronic-Telemetry implementation DataHeader
	 * @return true
	 */
	@Override
	public boolean isVariableMeasurementSize() {
		return true;
	}

	/**
	 * method to get the sorted active or in active record names as string array
	 * - records which does not have inactive or active flag are calculated from active or inactive
	 * - all records not calculated may have the active status and must be stored
	 * @param channelConfigNumber
	 * @param validMeasurementNames based on the current or any previous configuration
	 * @return String[] containing record names
	 */
	@Override
	public String[] getNoneCalculationMeasurementNames(int channelConfigNumber, String[] validMeasurementNames) {
		return validMeasurementNames;
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	@Override
	public void open_closeCommPort() {
		this.importLogFiles();
	}

	/**
	 * import a CSV file, also called "OpenFormat" file
	 */
	public void importLogFiles() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT4000));

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					ArduPilot.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								RecordSet activeRecordSet = ArduPilotLogReader.read(ArduPilot.this.getDataBlockSeparator().value().charAt(0), selectedImportFile, ArduPilot.this.getRecordSetStemNameReplacement());
								ArduPilot.this.updateVisibilityStatus(activeRecordSet, true);
								activeRecordSet.descriptionAppendFilename(selectedImportFile);
							}
							catch (Throwable e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					ArduPilot.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * @param record
	 * @return true if if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
		return record.getDataType() == DataType.GPS_LATITUDE || record.getDataType() == DataType.GPS_LONGITUDE;
	}

	/**
	 * query if the actual record set of this device contains GPS data to enable KML export to enable google earth visualization 
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public boolean isActualRecordSetWithGpsData() {
		boolean containsGPSdata = false;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				containsGPSdata = activeRecordSet.containsGPSdata();
			}
		}

		return containsGPSdata;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		//GPGGA	0=latitude 1=longitude  2=altitudeAbs 3=numSatelites
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && this.isActualRecordSetWithGpsData()) {
				int recordOrdinal = activeRecordSet.getRecordOrdinalOfType(Record.DataType.GPS_SPEED);
				return recordOrdinal >= 0 ? recordOrdinal : activeRecordSet.findRecordOrdinalByUnit(new String[] { "km/h", "kph" }); //speed;
			}
		}
		return -1;
	}
}
