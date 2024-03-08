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

    Copyright (c) 2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.GDE;
import gde.device.smmodellbau.jlog2.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * this class provides capability to read, update and create JLog2 configuration and manifest it in CONFIG.txt file
 */
public class JLog2Configuration extends Composite {
	final static Logger						log										= Logger.getLogger(JLog2Configuration.class.getName());

	private CCombo								jlogVersionCombo, jlogFirmwareCombo;

	private Group									mainConfigGroup;
	private CLabel								jlcUrlLabel, logModeLabel, flagsLabel;
	private Button								jlcDownloadButton, jlcForumButton, resetButton;
	private Button								directRatioButton, extRpmButton, logStopButton, highPulsWidthButton, gearSelectionButton;
	private CLabel								dotLabel, sysModeLabel, gearRatioLabel, baudrateLabel, motorPolsLabel;
	private CCombo								jlogConfigurationCombo, logModeCombo, sysModeCombo, baudrateCombo;
	private CCombo								motorShuntCombo, motorPolsCombo, directGearRatioMajorCombo, directGearRatioDecimalsCombo;
	private CCombo								gearMainWheelDecimalsCombo, gearMainWheelCombo, gearPinionWheelCombo;
	private CLabel								mainConfigLabel, gearMainWheelLabel, percentLabel, motorShuntLabel;
	private Text									mainExplanationText;

	private Group									alarmGroup;
	private CLabel								uBecDipDetectLabel, fetTempMaxLabel, voltageLabel, mAhLabel, uBatMinAlarmLabel, capacityAlarmLabel;
	private CLabel								temperaureLabel, temperaure1Label;
	private CCombo								fetTempMaxCombo, voltageBatteryAlarmDecimalsCombo, voltageBatteryAlarmCombo, capacityAlarmCombo;
	private Button								alarmsClearButton, uBecDipDetectButton, ext1smallerButton, ext2smallerButton, ext3smallerButton, ext4smallerButton, ext5smallerButton;
	private Button								speedSensorButton;
	private CLabel								speedSensorLabel;
	private CCombo								hv2BecCombo;
	private CLabel								hv2BecLabel;
	private CCombo								extern1Combo, extern2Combo, extern3Combo, extern4Combo, extern5Combo;
	private CLabel								ext1Label, ext2Label, ext3Label, ext4Label, ext5Label;

	private Group									optionalGroup;
	private Composite							optionalStuff;
	private Button								sensorAdapterButton, motorButton, brushLessButton;
	private CCombo								alarmLinesCombo, telemetryCombo, pulsPerRevolutionSensorCombo, tempSensorTypeCombo, subDevicesCombo, line1signalTypeCombo;
	private CLabel								telemetryLabel, rpmSensorLabel, tempSensorTypeLabel, subDevicesLabel, line1signalTypeLabel, alarmLinesLabel, mpxAddessesLabel;
	private MpxAddressComposite[]	mpxAddresses					= new MpxAddressComposite[16];

	final DataExplorer						application;
	final JLog2Dialog							dialog;
	final JLog2										device;
	final String[]								jlogFirmware					= new String[] { "3.2.2", "4.0.0" };//$NON-NLS-1$																																																																				//$NON-NLS-1$ //$NON-NLS-2$
	final String[]								jlogConfigurations322	= new String[] {
			"---- normal ----", //$NON-NLS-1$
			Messages.getString(MessageIds.GDE_MSGT2894), Messages.getString(MessageIds.GDE_MSGI2878), Messages.getString(MessageIds.GDE_MSGI2879), Messages.getString(MessageIds.GDE_MSGI2880),
			Messages.getString(MessageIds.GDE_MSGI2881), Messages.getString(MessageIds.GDE_MSGI2882), Messages.getString(MessageIds.GDE_MSGT2817), Messages.getString(MessageIds.GDE_MSGT2818),
			Messages.getString(MessageIds.GDE_MSGT2819), Messages.getString(MessageIds.GDE_MSGT2820), Messages.getString(MessageIds.GDE_MSGT2821), Messages.getString(MessageIds.GDE_MSGT2822),
			Messages.getString(MessageIds.GDE_MSGT2823), Messages.getString(MessageIds.GDE_MSGT2824), Messages.getString(MessageIds.GDE_MSGT2825), Messages.getString(MessageIds.GDE_MSGT2831),
			Messages.getString(MessageIds.GDE_MSGT2837), Messages.getString(MessageIds.GDE_MSGT2843), Messages.getString(MessageIds.GDE_MSGT2846), Messages.getString(MessageIds.GDE_MSGT2852),
			Messages.getString(MessageIds.GDE_MSGT2856), Messages.getString(MessageIds.GDE_MSGT2862) };
	final String[]								jlogConfigurations400	= new String[] { " none selected", //$NON-NLS-1$
			" Jive basic", //$NON-NLS-1$ //R60: Kontronik JIVE with base telemetry: OpenFormat live stream, Unidisplay, JETI v1 and MPX telemetry
			" Jive JETI EX", //$NON-NLS-1$ //R61: Kontronik JIVE with JETI EX telemetry.  Also supported: OpenFormat live stream, Unidisplay and JETI v1 telemetry
			" Jive HoTT v4", //$NON-NLS-1$ //R62: Kontronik JIVE with HoTTv4 telemetry.  Also supported: OpenFormat live stream and JETI v1 telemetry
			" Jive Futaba SBUS2", //$NON-NLS-1$ //R63: Kontronik JIVE with Futaba(S.BUS2) telemetry.  Also supported: OpenFormat live stream, JETI v1 and MPX telemetry
			" Jive HiTec", //$NON-NLS-1$ //R64: Kontronik JIVE with HiTec telemetry.  (HiTec only!) Interface JSend required for pull up resistors to +3,3 V to enalbel Optima-Rx detection
			" Jive Spektrum", //$NON-NLS-1$ //R65: Kontronik JIVE with SPEKTRUM telemetry.  (SPEKTRUM only!) JSPEK Interface to TM1000 required and adapter to connect K4-1, K4-2 to 2 x 3 Pin JR
			" Castle Creations Jeti EX", //$NON-NLS-1$ //R71: Castle Creations ESC with JETI EX telemetry.  Also supported: OpenFormat live stream, Unidisplay, JETI v1 and MPX telemetry
			" Castle Creations HoTT v4", //$NON-NLS-1$ //R72: Castle Creations ESC with HoTTv4 telemetry.  Also supported: OpenFormat live stream, Unidisplay, JETI v1 and MPX telemetry
			" Castle Creations Futaba SBUS2", //$NON-NLS-1$ //R73: Castle Creations ESC with Futaba(S.BUS2) telemetry.  Also supported: OpenFormat live stream, Unidisplay, JETI v1 and MPX
			" Castle Creations HiTec", //$NON-NLS-1$ //R74: Castle Creations ESC with HiTec telemetry.  (HiTec only!) Interface JLog-COM<->CC/Rx-throttle required and JSend required for pull up resistors to +3,3 V to enalbel Optima-Rx detection
			" Castle Creations Spektrum", //$NON-NLS-1$ //R75: Castle Creations ESC with SPEKTRUM telemetry.  (SPEKTRUM only!)   Interface JLog-COM<->CC/Rx-throttle required and  JSPEK Interface to TM1000
			" C200 current sensor HoTT V4", //$NON-NLS-1$ //R81: Current sensor C200 with HoTTv4 telemetry.  Also supported: OpenFormat live stream, Unidisplay, JETI v1 and MPX telemetry
			" C200 current sensor Futaba SBUS2", //$NON-NLS-1$ //R82: Current sensor C200 with Futaba(S.BUS2) telemetry.  Also supported: OpenFormat live stream, Unidisplay, JETI v1 and MPX
																											};

	final String[]								baudrates							= new String[] { "JIVE", "2400", "4800", "9600", "38400", "57600", "115200", "CMT" };																																											//$NON-NLS-1$
	final String[]								sysModes							= new String[] { "NEWLOG", "SEQLOG" };																																																																			//$NON-NLS-1$ //$NON-NLS-2$
	final String[]								logModes							= new String[] { "(0) OF/LV", "(2) SER", "(8) JLV" };																																																											//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	final String[]								motorPols							= new String[] {
			"2", "4", "6", "8", "10", "12", "14", "16", "18", "20", "22", "24", "26", "38", "30", "32", "34", "36", "38", "40", "42", "44", "46", "48" };																																							//$NON-NLS-1$
	final String[]								currentShuntAdjust		= new String[121];
	final String[]								zeroTo9								= new String[10];																																																																														;
	final String[]								zeroTo99							= new String[100];																																																																													;
	final String[]								zeroTo50							= new String[51];
	final String[]								oneTo50								= new String[50];
	final String[]								eightTo30							= new String[23];
	final String[]								eightTo255						= new String[248];
	final String[]								zeroTo127							= new String[127];																																																																													;
	final String[]								zeroTo25500						= new String[256];
	final String[]								zeroAlarms						= new String[] { "0" };																																																																										//$NON-NLS-1$
	final String[]								oneAlarms							= new String[] { "0", "1" };																																																																								//$NON-NLS-1$
	final String[]								greaterOneAlarms			= new String[] { "0", "1", "2" };																																																																					//$NON-NLS-1$
	final String									comOutputString				= " ----------- , FTDI livestream, JETI, MPX, Unidisplay";																																																									//$NON-NLS-1$
	final StringBuilder						comOutput							= new StringBuilder().append(this.comOutputString);
	final String									hottOutput						= ", HoTT";																																							//$NON-NLS-1$
	//firmware 4.0.0
	final String[]								comOutputBase					= new String[] {" ----------- "," FTDI livestream"," Unidisplay"," MPX"," JETI v1"};		//$NON-NLS-1$
	final String[]								comOutputJetiEx				= new String[] {" ----------- "," FTDI livestream"," JETI EX"};													//$NON-NLS-1$
	final String[]								comOutputHoTTv4				= new String[] {" ----------- "," FTDI livestream"," JETI v1"," HoTT v4"};							//$NON-NLS-1$
	final String[]								comOutputFutabaSbus2	= new String[] {" ----------- "," FTDI livestream"," JETI v1"," MPX"," S.BUS2"};				//$NON-NLS-1$
	final String[]								comOutputHiTec				= new String[] {" HiTec"};																															//$NON-NLS-1$
	final String[]								comOutputSpektrum			= new String[] {" Spektrum"};																														//$NON-NLS-1$

	/**
	 * this inner class holds the configuration and contains all logic to insert and update entries
	 * missing part is the sub device section since there are only the special configurations which contains the required bit masks
	 */
	public abstract class Configuration {

		String[]	config;
		int version = 400;

		Configuration(String newConfiguration, int newVersion) {
			String[] tmpConfig = newConfiguration.split(GDE.STRING_COMMA);
			this.config = new String[tmpConfig.length];
			for (int i = 0; i < tmpConfig.length; i++) {
				this.config[i] = tmpConfig[i];
			}
			this.version = newVersion;
		}

		public void update(String updatedConfig) {
			String[] tmpConfig = updatedConfig.split(GDE.STRING_COMMA);
			this.config = new String[tmpConfig.length];
			for (int i = 0; i < tmpConfig.length; i++) {
				this.config[i] = tmpConfig[i];
			}
			JLog2Configuration.log.log(Level.FINE, getConfiguration());
		}

		public int get(int index) {
			return Integer.valueOf(this.config[index]);
		}

		public String getConfiguration() {
			StringBuilder sb = new StringBuilder();
			for (String element : this.config) {
				sb.append(element).append(GDE.STRING_COMMA);
			}
			return sb.delete(sb.length() - 1, sb.length()).toString();
		}

		public void setBaudRate(int baudeRateIndex) {
			String baudeRate = "9600"; //$NON-NLS-1$
			switch (baudeRateIndex) {
			case 1:
				baudeRate = "2400"; //$NON-NLS-1$
				break;
			case 2:
				baudeRate = "4800"; //$NON-NLS-1$
				break;
			case 3:
				baudeRate = "9600"; //$NON-NLS-1$
				break;
			case 4:
			case 7: //CMT
				baudeRate = "38400"; //$NON-NLS-1$
				break;
			case 5:
				baudeRate = "57600"; //$NON-NLS-1$
				break;
			case 6:
				baudeRate = "115200"; //$NON-NLS-1$
				break;
			case 0:
			default:
				baudeRate = "9600"; //$NON-NLS-1$
				break;
			}
			this.config[0] = baudeRate;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		/**
		 * generic setter
		 * @param index
		 * @param value
		 */
		public void set(int index, int value) {
			this.config[index] = GDE.STRING_EMPTY + value;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setSysMode(int sysMode) { //0=NEWLOG; 1=SEQLOG
			this.config[1] = GDE.STRING_EMPTY + sysMode;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMotorCalibration(String calibration) {
			int value = Integer.valueOf(calibration.trim());
			if ((value & 0x80) == 0)
				this.config[1] = GDE.STRING_EMPTY + ((value << 1) + (Integer.valueOf(this.config[1]) & 0x0001));
			else
				this.config[1] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[1]) & 0x0001) - ((value << 1) - 128));
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setLogMode(int logMode) { //0:CSV=0, 1:SER=2, 2:JLV=8
			switch (logMode) {
			case 0:
			default:
				this.config[2] = GDE.STRING_EMPTY + (Integer.valueOf(this.config[2]) & 0x00F4);
				break;
			case 1:
				this.config[2] = GDE.STRING_EMPTY + (2 + (Integer.valueOf(this.config[2]) & 0x00F4));
				break;
			case 2:
				this.config[2] = GDE.STRING_EMPTY + (8 + (Integer.valueOf(this.config[2]) & 0x00F4));
				break;
			}
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMotorPols(String numMotorPols) {
			this.config[3] = numMotorPols;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setGearDirectConfig(boolean isConfig) {
			this.config[4] = GDE.STRING_EMPTY + (isConfig ? 2 : 1);
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setGearRatioMinor(String gearRatioMajor) {
			this.config[5] = gearRatioMajor;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setGearRatioDecimals(String gearRatioDecimals) {
			this.config[6] = GDE.STRING_EMPTY + Integer.valueOf(gearRatioDecimals);
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setGearPinionWheel(String gearPinionWheel) {
			this.config[7] = gearPinionWheel;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setGearMajorWheel(String gearMainWheel) {
			this.config[8] = gearMainWheel;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setGearMajorWheelDecimals(String gearMainWheelDecimals) {
			this.config[9] = gearMainWheelDecimals;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setLogStop(int logStop) {
			this.config[10] = GDE.STRING_EMPTY + ((logStop & 0x0001) + (Integer.valueOf(this.config[10]) & 0xFFFE));
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setHighPwmWarning(int highPwmWarning) {
			this.config[10] = GDE.STRING_EMPTY + ((highPwmWarning & 0x0002) + (this.get(10) & 0xFFFD));
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setLogStopExtRpmSensor(int extRpmSensorEffect) {
			this.config[10] = GDE.STRING_EMPTY + ((extRpmSensorEffect & 0x0004) + (Integer.valueOf(this.config[10]) & 0xFFFB));
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setReset(String reset) {
			this.config[11] = reset;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setCapacityAlarm(int capacity) {
			this.config[12] = GDE.STRING_EMPTY + capacity;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setBatteryAlarmMajor(String alarmVoltageMajor) {
			this.config[13] = alarmVoltageMajor;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setBatteryAlarmDecimals(String alarmVoltageMinor) {
			this.config[14] = alarmVoltageMinor;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setPaMaxTempAlarm(String pamaxTemperature) {
			this.config[15] = pamaxTemperature;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setBecDip(boolean isUbecDip) {
			this.config[16] = GDE.STRING_EMPTY + (isUbecDip ? 1 : 0);
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setExtTemp1(String extTemp1) {
			this.config[17] = extTemp1;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setExtTemp2(String extTemp2) {
			this.config[18] = extTemp2;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setExtTemp3(String extTemp3) {
			this.config[19] = extTemp3;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setExtTemp4(String extTemp4) {
			this.config[20] = extTemp4;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setExtTemp5(String extTemp5) {
			this.config[21] = extTemp5;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setExtTemp1LowerThan(int ltExtTemp1) {
			this.config[22] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[22]) & 0x00FE) + (ltExtTemp1 & 0x01));
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setExtTemp2LowerThan(int ltExtTemp2) {
			this.config[22] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[22]) & 0x00FD) + (ltExtTemp2 << 1 & 0x02));
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setExtTemp3LowerThan(int ltExtTemp3) {
			this.config[22] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[22]) & 0x00FB) + (ltExtTemp3 << 2 & 0x04));
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setExtTemp4LowerThan(int ltExtTemp4) {
			this.config[22] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[22]) & 0x00F7) + (ltExtTemp4 << 3 & 0x08));
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setExtTemp5LowerThan(int ltExtTemp5) {
			this.config[22] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[22]) & 0x00EF) + (ltExtTemp5 << 4 & 0x10));
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setNumberAlarmLines(int numAddressLines) {
			this.config[23] = GDE.STRING_EMPTY + numAddressLines;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setTemperaturSensorType(int tempSensorType) {
			this.config[24] = GDE.STRING_EMPTY + tempSensorType;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void set25Type(int type) { //this is unclear, set only in combination with special configurations, may be JLog2.5
			this.config[25] = GDE.STRING_EMPTY + type;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setTelemetryType(int type) {
			this.config[26] = GDE.STRING_EMPTY + (type > 1 ? type - 1 : 0);
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setPulsePerRevolution(int rpmSensorPulsePerRevolution) {
			this.config[27] = rpmSensorPulsePerRevolution >= 1 ? "1" : "0"; //$NON-NLS-1$ //$NON-NLS-2$
			this.config[28] = rpmSensorPulsePerRevolution >= 1 ? GDE.STRING_EMPTY + rpmSensorPulsePerRevolution : "0"; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setIsMotor(boolean isMotor) {
			this.config[29] = isMotor ? GDE.STRING_EMPTY + (Integer.valueOf(this.config[29]) | 0x40) : GDE.STRING_EMPTY + (Integer.valueOf(this.config[29]) & 0xFFBF);
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setIsBrushlessMotor(boolean isBrushLessMotor, int numMotorPols) {
			this.config[29] = isBrushLessMotor ? GDE.STRING_EMPTY + (0x80 + numMotorPols) : GDE.STRING_EMPTY + (Integer.valueOf(this.config[29]) & 0xFF7F);
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress0(int mpxSensorAddress) {
			this.config[30] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress1(int mpxSensorAddress) {
			this.config[31] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress2(int mpxSensorAddress) {
			this.config[32] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress3(int mpxSensorAddress) {
			this.config[33] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress4(int mpxSensorAddress) {
			this.config[34] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress5(int mpxSensorAddress) {
			this.config[35] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress6(int mpxSensorAddress) {
			this.config[36] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress7(int mpxSensorAddress) {
			this.config[37] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress8(int mpxSensorAddress) {
			this.config[38] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress9(int mpxSensorAddress) {
			this.config[39] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress10(int mpxSensorAddress) {
			this.config[40] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress11(int mpxSensorAddress) {
			this.config[41] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress12(int mpxSensorAddress) {
			this.config[42] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress13(int mpxSensorAddress) {
			this.config[43] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress14(int mpxSensorAddress) {
			this.config[44] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setMpxSensorAddress15(int mpxSensorAddress) {
			this.config[45] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress; //$NON-NLS-1$
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setLine1signalType(int signalType) {
			this.config[46] = GDE.STRING_EMPTY + (signalType & 0x00E0);
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setTelemetryBaudrateType(int type) {
			this.config[46] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[46]) & 0xFFF0) + (type & 0x000F));
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setSubDeviceType(int type) {
			this.config[46] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[46]) & 0xFFF0) + (type & 0x000F));
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void set47Type(int type) { //Duftmarke, set only in combination with special configurations
			this.config[47] = GDE.STRING_EMPTY + type;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public void setHV2BecVoltage(int slectionIndex) {
			if (this.config.length < 49) {
				String[] tmpConfig = new String[49];
				for (int i = 0; i < this.config.length; i++) {
					tmpConfig[i] = this.config[i];
				}
				this.config = tmpConfig;
			}
			this.config[48] = GDE.STRING_EMPTY + slectionIndex;
			JLog2Configuration.log.log(Level.FINER, getConfiguration());
		}

		public abstract void switchConfig();
	}

	public class Configuration322 extends Configuration {

		final static String	normal	= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,2,3,4,5,6,7,8,12,13,16,16,16,14,9,10,11,36,0";							//$NON-NLS-1$
		final String				HSS			= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,16,9,0,0,6,2,3,4,5,6,7,8,12,13,16,16,16,14,9,10,11,36,200";					//$NON-NLS-1$
		final String				HSSG2		= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,17,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,36,201";	//$NON-NLS-1$
		final String				HSST		= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,18,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,36,202";	//$NON-NLS-1$
		final String				BH			= "9600,0,128,6,2,1,0,10,10,0,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,19,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,36,210,0"; //$NON-NLS-1$
		final String				BM			= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,19,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,36,211,0"; //$NON-NLS-1$
		final String				BHSS		= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,20,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,36,212,0"; //$NON-NLS-1$
		final String				BHSST		= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,20,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,36,213,0"; //$NON-NLS-1$
		final String				G				= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,36,10";		//$NON-NLS-1$
		final String				S				= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,6,4,5,6,7,8,9,10,16,16,16,16,16,14,11,12,13,36,15";					//$NON-NLS-1$
		final String				L				= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,6,2,3,4,5,6,7,8,16,16,16,16,16,12,9,10,11,36,40";						//$NON-NLS-1$
		final String				T				= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6,9,0,0,6,2,3,4,5,6,7,8,16,16,16,16,16,12,9,10,11,36,110";						//$NON-NLS-1$
		final String				GT			= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,36,111";		//$NON-NLS-1$
		final String				V				= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,36,1";			//$NON-NLS-1$
		final String				VG			= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,36,23";		//$NON-NLS-1$
		final String				VS			= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,6,4,5,6,7,8,9,10,16,14,16,16,16,16,11,12,13,36,16";					//$NON-NLS-1$
		final String				BID			= "9600,0,192,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,4,5,6,7,8,9,10,16,14,16,16,16,16,11,12,13,36,2";						//$NON-NLS-1$
		final String				BIDG		= "9600,0,192,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,36,20";		//$NON-NLS-1$
		final String				BIDS		= "9600,0,192,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,6,4,5,6,7,8,9,10,16,16,16,16,16,14,11,12,13,36,25";					//$NON-NLS-1$
		final String				A				= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,0,0,6,4,5,6,7,8,9,10,16,16,16,16,16,14,11,12,13,36,5";						//$NON-NLS-1$
		final String				AV			= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,0,0,6,4,5,6,7,8,9,10,16,16,16,16,16,14,11,12,13,36,6";						//$NON-NLS-1$
		final String				B				= "0,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,3,0,0,6,4,5,6,7,8,9,10,16,16,16,16,16,14,11,12,13,132,3";							//$NON-NLS-1$
		final String				BV			= "0,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,3,0,0,6,4,5,6,7,8,9,10,16,16,16,16,16,14,11,12,13,132,4";							//$NON-NLS-1$
		final String				P1			= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,4,5,6,7,8,9,10,16,16,16,16,16,14,11,12,13,36,91";					//$NON-NLS-1$
		final String				P2			= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,4,5,6,7,8,9,10,16,16,16,16,16,14,11,12,13,36,92";					//$NON-NLS-1$
		final String				AVP1		= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,0,0,6,4,5,6,7,8,9,10,16,16,16,16,16,14,11,12,13,36,180";					//$NON-NLS-1$
		final String				AP2			= "9600,0,128,6,2,1,0,10,10,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,0,0,6,4,5,6,7,8,9,10,16,16,16,16,16,14,11,12,13,36,170";					//$NON-NLS-1$

		Configuration322() {
			super(Configuration322.normal, 322);
		}

		Configuration322(String newConfiguration) {
			super(newConfiguration, 322);
		}

		@Override
		public void switchConfig() {
			//0=normal, 1=HSS, 2=HSSG2, 3=HSST, 4=BH, 5=BM, 6=BHSS, 7=BHSST, 8=G, 9=S 10=L,
			//11=T, 12=GT, 13=V, 14=VG, 15=VS, 16=BID, 17=BIDG, 18=BIDS, 19=A, 20=AV, 21=B, 22=BV, 23=P1, 24=P2, 25=AVP1, 26=AP2
			switch (JLog2Configuration.this.jlogConfigurationCombo.getSelectionIndex()) {
			default:
			case 0:
				JLog2Configuration.this.configuration.update(Configuration322.normal);
				break;
			case 1:
				JLog2Configuration.this.configuration.update(this.HSS);
				break;
			case 2:
				JLog2Configuration.this.configuration.update(this.HSSG2);
				break;
			case 3:
				JLog2Configuration.this.configuration.update(this.HSST);
				break;
			case 4:
				JLog2Configuration.this.configuration.update(this.BH);
				break;
			case 5:
				JLog2Configuration.this.configuration.update(this.BM);
				break;
			case 6:
				JLog2Configuration.this.configuration.update(this.BHSS);
				break;
			case 7:
				JLog2Configuration.this.configuration.update(this.BHSST);
				break;
			case 8:
				JLog2Configuration.this.configuration.update(this.G);
				break;
			case 9:
				JLog2Configuration.this.configuration.update(this.S);
				break;
			case 10:
				JLog2Configuration.this.configuration.update(this.L);
				break;
			//11=T, 12=GT, 13=V, 14=VG, 15=VS, 16=BID, 17=BIDG, 18=BIDS, 19=A, 20=AV,
			case 11:
				JLog2Configuration.this.configuration.update(this.T);
				break;
			case 12:
				JLog2Configuration.this.configuration.update(this.GT);
				break;
			case 13:
				JLog2Configuration.this.configuration.update(this.V);
				break;
			case 14:
				JLog2Configuration.this.configuration.update(this.VG);
				break;
			case 15:
				JLog2Configuration.this.configuration.update(this.VS);
				break;
			case 16:
				JLog2Configuration.this.configuration.update(this.BID);
				break;
			case 17:
				JLog2Configuration.this.configuration.update(this.BIDG);
				break;
			case 18:
				JLog2Configuration.this.configuration.update(this.BIDS);
				break;
			case 19:
				JLog2Configuration.this.configuration.update(this.A);
				break;
			case 20:
				JLog2Configuration.this.configuration.update(this.AV);
				break;
			//21=B, 22=BV, 23=P1, 24=P2, 25=AVP1, 26=AP2
			case 21:
				JLog2Configuration.this.configuration.update(this.B);
				break;
			case 22:
				JLog2Configuration.this.configuration.update(this.BV);
				break;
			case 23:
				JLog2Configuration.this.configuration.update(this.P1);
				break;
			case 24:
				JLog2Configuration.this.configuration.update(this.P2);
				break;
			case 25:
				JLog2Configuration.this.configuration.update(this.AVP1);
				break;
			case 26:
				JLog2Configuration.this.configuration.update(this.AP2);
				break;
			}
		}
	}

	public class Configuration400 extends Configuration {
		final static String	noneSelected			= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132";				//$NON-NLS-1$
		final String				jiveBasic					= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132,60,0,0";	//$NON-NLS-1$
		final String				jiveJetiEx				= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132,61,0,0";	//$NON-NLS-1$
		final String				jiveHoTTv4				= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132,62,0,0";	//$NON-NLS-1$
		final String				jiveFutabaSbus2		= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132,63,0,0";	//$NON-NLS-1$
		final String				jiveHiTec					= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132,64";			//$NON-NLS-1$
		final String				jiveSpektrum			= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132,65";			//$NON-NLS-1$
		final String				castleJetiEx			= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132,71";			//$NON-NLS-1$
		final String				castleHoTTv4			= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132,72";			//$NON-NLS-1$
		final String				castleFutabaSbus2	= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132,73";			//$NON-NLS-1$
		final String				castleHiTec				= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132,74";			//$NON-NLS-1$
		final String				castleSpektrum		= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132,75";			//$NON-NLS-1$
		final String				c200HoTTv4				= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132,81";			//$NON-NLS-1$
		final String				c200FutabaSbus		= "9600,0,128,6,2,1,0,10,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,6,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,132,82";			//$NON-NLS-1$

		Configuration400() {
			super(Configuration400.noneSelected, 400);
		}

		Configuration400(String newConfiguration) {
			super(newConfiguration,400);
		}

		@Override
		public void switchConfig() {
			//R60: Kontronik JIVE with base telemetry: OpenFormat live stream, Unidisplay, JETI v1 and MPX telemetry
			//R61: Kontronik JIVE with JETI EX telemetry.  Also supported: OpenFormat live stream, Unidisplay and JETI v1 telemetry
			//R62: Kontronik JIVE with HoTTv4 telemetry.  Also supported: OpenFormat live stream and JETI v1 telemetry
			//R63: Kontronik JIVE with Futaba(S.BUS2) telemetry.  Also supported: OpenFormat live stream, JETI v1 and MPX telemetry
			//R64: Kontronik JIVE with HiTec telemetry.  (HiTec only!) Interface JSend required for pull up resistors to +3,3 V to enalbel Optima-Rx detection
			//R65: Kontronik JIVE with SPEKTRUM telemetry.  (SPEKTRUM only!) JSPEK Interface to TM1000 required and adapter to connect K4-1, K4-2 to 2 x 3 Pin JR
			//R71: Castle Creations ESC with JETI EX telemetry.  Also supported: OpenFormat live stream, Unidisplay, JETI v1 and MPX telemetry
			//R72: Castle Creations ESC with HoTTv4 telemetry.  Also supported: OpenFormat live stream, Unidisplay, JETI v1 and MPX telemetry
			//R73: Castle Creations ESC with Futaba(S.BUS2) telemetry.  Also supported: OpenFormat live stream, Unidisplay, JETI v1 and MPX
			//R74: Castle Creations ESC with HiTec telemetry.  (HiTec only!) Interface JLog-COM<->CC/Rx-throttle required and JSend required for pull up resistors to +3,3 V to enalbel Optima-Rx detection
			//R75: Castle Creations ESC with SPEKTRUM telemetry.  (SPEKTRUM only!)   Interface JLog-COM<->CC/Rx-throttle required and  JSPEK Interface to TM1000
			//R81: Current sensor C200 with HoTTv4 telemetry.  Also supported: OpenFormat live stream, Unidisplay, JETI v1 and MPX telemetry
			//R82: Current sensor C200 with Futaba(S.BUS2) telemetry.  Also supported: OpenFormat live stream, Unidisplay, JETI v1 and MPX
			switch (JLog2Configuration.this.jlogConfigurationCombo.getSelectionIndex()) {
			default:
			case 0:
				JLog2Configuration.this.configuration.update(Configuration400.noneSelected);
				break;
			case 1:
				JLog2Configuration.this.configuration.update(this.jiveBasic);
				break;
			case 2:
				JLog2Configuration.this.configuration.update(this.jiveJetiEx);
				break;
			case 3:
				JLog2Configuration.this.configuration.update(this.jiveHoTTv4);
				break;
			case 4:
				JLog2Configuration.this.configuration.update(this.jiveFutabaSbus2);
				break;
			case 5:
				JLog2Configuration.this.configuration.update(this.jiveHiTec);
				break;
			case 6:
				JLog2Configuration.this.configuration.update(this.jiveSpektrum);
				break;
			case 7:
				JLog2Configuration.this.configuration.update(this.castleJetiEx);
				break;
			case 8:
				JLog2Configuration.this.configuration.update(this.castleHoTTv4);
				break;
			case 9:
				JLog2Configuration.this.configuration.update(this.castleFutabaSbus2);
				break;
			case 10:
				JLog2Configuration.this.configuration.update(this.castleHiTec);
				break;
			case 11:
				JLog2Configuration.this.configuration.update(this.castleSpektrum);
				break;
			case 12:
				JLog2Configuration.this.configuration.update(this.c200HoTTv4);
				break;
			case 13:
				JLog2Configuration.this.configuration.update(this.c200FutabaSbus);
				break;
			}
		}
	}

	/**
	 * this UI composite encapsulate Multiplex address configuration drop down configuration
	 */
	public class MpxAddressComposite extends Composite {

		private CCombo	mpxAddressCombo;
		private CLabel	mpxAddressLabel;

		public MpxAddressComposite(Composite parent, int style, final String labelText, final Configuration configuration, final int index) {
			super(parent, style);
			RowLayout mpxAddressCompositeLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
			RowData mpxAddressCompositeLData = new RowData();
			mpxAddressCompositeLData.width = GDE.IS_LINUX ? 105 : 95;
			mpxAddressCompositeLData.height = 22;
			this.setLayoutData(mpxAddressCompositeLData);
			this.setLayout(mpxAddressCompositeLayout);
			{
				this.mpxAddressLabel = new CLabel(this, SWT.RIGHT);
				RowData mpxAddressLabelLData = new RowData();
				mpxAddressLabelLData.width = 50;
				mpxAddressLabelLData.height = 20;
				this.mpxAddressLabel.setLayoutData(mpxAddressLabelLData);
				this.mpxAddressLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.mpxAddressLabel.setText(labelText);
			}
			{
				RowData mpxAddressComboLData = new RowData();
				mpxAddressComboLData.width = GDE.IS_LINUX ? 45 : 35;
				mpxAddressComboLData.height = 16;
				this.mpxAddressCombo = new CCombo(this, SWT.BORDER);
				this.mpxAddressCombo.setLayoutData(mpxAddressComboLData);
				this.mpxAddressCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.mpxAddressCombo.setItems(new String[] { " 0", " 1", " 2", " 3", " 4", " 5", " 6", " 7", " 8", " 9", " 10", " 11", " 12", " 13", " 14", " 15", " --" }); //$NON-NLS-1$
				this.mpxAddressCombo.select(16);
				this.mpxAddressCombo.setVisibleItemCount(10);
				this.mpxAddressCombo.setEnabled(false);
				this.mpxAddressCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						JLog2Configuration.log.log(Level.FINEST, "mpxAddressCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						int address = MpxAddressComposite.this.mpxAddressCombo.getSelectionIndex();
						for (int i = 0; i < 16; i++) {
							if (JLog2Configuration.log.isLoggable(Level.FINER)) JLog2Configuration.log.log(Level.FINER, i + " != " + index + "; " + configuration.get(i + 30) + " == " + address);
							if (i != index && configuration.get(i + 30) == address) {
								JLog2Configuration.this.application.openMessageDialog(JLog2Configuration.this.dialog.getDialogShell(), Messages.getString(MessageIds.GDE_MSGW2803));
								MpxAddressComposite.this.mpxAddressCombo.select(16);
								address = 16;
								break;
							}
						}
						switch (index) {
						case 0:
							configuration.setMpxSensorAddress0(address);
							break;
						case 1:
							configuration.setMpxSensorAddress1(address);
							break;
						case 2:
							configuration.setMpxSensorAddress2(address);
							break;
						case 3:
							configuration.setMpxSensorAddress3(address);
							break;
						case 4:
							configuration.setMpxSensorAddress4(address);
							break;
						case 5:
							configuration.setMpxSensorAddress5(address);
							break;
						case 6:
							configuration.setMpxSensorAddress6(address);
							break;
						case 7:
							configuration.setMpxSensorAddress7(address);
							break;
						case 8:
							configuration.setMpxSensorAddress8(address);
							break;
						case 9:
							configuration.setMpxSensorAddress9(address);
							break;
						case 10:
							configuration.setMpxSensorAddress10(address);
							break;
						case 11:
							configuration.setMpxSensorAddress11(address);
							break;
						case 12:
							configuration.setMpxSensorAddress12(address);
							break;
						case 13:
							configuration.setMpxSensorAddress13(address);
							break;
						case 14:
							configuration.setMpxSensorAddress14(address);
							break;
						case 15:
							configuration.setMpxSensorAddress15(address);
							break;
						}
						enableSaveSettings();
					}
				});
			}
		}

		public void setSelection(int addressIndex) {
			this.mpxAddressCombo.select(addressIndex);
		}
	}

	Configuration	configuration;

	public void loadConfiuration(String configString, int version) {
		if (version == 322) { //firmware 3.2.2
			this.jlogFirmwareCombo.select(0);
			this.jlogConfigurationCombo.setItems(this.jlogConfigurations322);
			this.configuration = new Configuration322(configString);
			this.configuration.update(configString);
		}
		else { //firmware 4.0.0
			this.jlogFirmwareCombo.select(1);
			this.jlogConfigurationCombo.setItems(this.jlogConfigurations400);
			this.configuration = new Configuration400(configString);
			this.configuration.update(configString);
		}
		this.jlogConfigurationCombo.select(0);
		initialyzeGUI(this.configuration, true);
	}

	/**
	* Auto-generated main method to display this
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public static void main(String[] args) {
		showGUI();
	}

	/**
	* Auto-generated method to display this
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public static void showGUI() {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		JLog2Configuration inst = new JLog2Configuration(shell, SWT.NULL, null, null);
		Point size = inst.getSize();
		shell.setLayout(new FillLayout());
		shell.layout();
		if (size.x == 0 && size.y == 0) {
			inst.pack();
			shell.pack();
		}
		else {
			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
			shell.setSize(shellBounds.width, shellBounds.height);
		}
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
	}

	public JLog2Configuration(Composite parent, int style, JLog2Dialog useDialog, JLog2 useDevice) {
		super(parent, style);
		this.application = DataExplorer.getInstance();
		this.dialog = useDialog;
		this.device = useDevice;

		for (int i = 0; i < this.currentShuntAdjust.length; i++) {
			this.currentShuntAdjust[i] = GDE.STRING_EMPTY + (i - 60);
		}
		for (int i = 0; i < this.zeroTo9.length; i++) {
			this.zeroTo9[i] = GDE.STRING_EMPTY + i;
		}
		for (int i = 0; i < this.zeroTo99.length; i++) {
			this.zeroTo99[i] = (i < 10 ? "0" : GDE.STRING_EMPTY) + i; //$NON-NLS-1$
		}
		for (int i = 0; i < this.zeroTo50.length; i++) {
			this.zeroTo50[i] = GDE.STRING_EMPTY + i;
		}
		for (int i = 0; i < this.oneTo50.length; i++) {
			this.oneTo50[i] = GDE.STRING_EMPTY + (i + 1);
		}
		for (int i = 0; i < this.eightTo30.length; i++) {
			this.eightTo30[i] = GDE.STRING_EMPTY + (i + 8);
		}
		for (int i = 0; i < this.eightTo255.length; i++) {
			this.eightTo255[i] = GDE.STRING_EMPTY + (i + 8);
		}
		for (int i = 0; i < this.zeroTo127.length; i++) {
			this.zeroTo127[i] = GDE.STRING_EMPTY + i;
		}
		for (int i = 0; i < this.zeroTo25500.length; i++) {
			this.zeroTo25500[i] = GDE.STRING_EMPTY + (i * 100);
		}

		this.configuration = new Configuration400();
		initGUI();
	}

	public void enableSaveSettings() {
		this.dialog.isConfigChanged = true;
		this.dialog.liveGathererButton.setEnabled(true);
	}

	private void initGUI() {
		try {
			this.dialog.isConfigChanged = false;
			this.setLayout(new FormLayout());
			this.setSize(675, 600);
			{
				this.mainConfigGroup = new Group(this, SWT.NONE);
				RowLayout mainConfigGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.mainConfigGroup.setLayout(mainConfigGroupLayout);
				FormData mainConfigGroupLData = new FormData();
				mainConfigGroupLData.width = 650;
				mainConfigGroupLData.left = new FormAttachment(0, 1000, 7);
				mainConfigGroupLData.top = new FormAttachment(0, 1000, 3);
				mainConfigGroupLData.right = new FormAttachment(1000, 1000, -7);
				mainConfigGroupLData.height = 280;
				this.mainConfigGroup.setLayoutData(mainConfigGroupLData);
				this.mainConfigGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 2 : 0), SWT.NORMAL));
				this.mainConfigGroup.setText(Messages.getString(MessageIds.GDE_MSGT2828));
				{
					this.jlcUrlLabel = new CLabel(this.mainConfigGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData jlcUrlLabelLData = new RowData();
					jlcUrlLabelLData.width = 154;
					jlcUrlLabelLData.height = 25;
					this.jlcUrlLabel.setLayoutData(jlcUrlLabelLData);
					this.jlcUrlLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.jlcUrlLabel.setText(Messages.getString(MessageIds.GDE_MSGT2829));
				}
				{
					this.jlcDownloadButton = new Button(this.mainConfigGroup, SWT.PUSH | SWT.CENTER);
					RowData jlcDownloadButtonLData = new RowData();
					jlcDownloadButtonLData.width = 114;
					jlcDownloadButtonLData.height = 25;
					this.jlcDownloadButton.setLayoutData(jlcDownloadButtonLData);
					this.jlcDownloadButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.jlcDownloadButton.setText("Homepage"); //$NON-NLS-1$
					this.jlcDownloadButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "jlcDownloadButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.application.openWebBrowser("https://jlog.hacknet.eu/jlog/");//$NON-NLS-1$
						}
					});
				}
				{
					this.jlcForumButton = new Button(this.mainConfigGroup, SWT.PUSH | SWT.CENTER);
					RowData jlcForumButtonLData = new RowData();
					jlcForumButtonLData.width = 114;
					jlcForumButtonLData.height = 25;
					this.jlcForumButton.setLayoutData(jlcForumButtonLData);
					this.jlcForumButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.jlcForumButton.setText("Forum"); //$NON-NLS-1$
					this.jlcForumButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "jlcForumButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.application.openWebBrowser("https://jlog.hacknet.eu/jlog/forum/");//$NON-NLS-1$
						}
					});
				}
				{
					new Label(this.mainConfigGroup, SWT.NONE).setLayoutData(new RowData(this.getClientArea().width - (GDE.IS_MAC ? 35 : GDE.IS_LINUX ? 0 : 45), 5));
				}
				{
					this.mainConfigLabel = new CLabel(this.mainConfigGroup, SWT.NONE);
					this.mainConfigLabel.setLayoutData(new RowData(80, 20));
					this.mainConfigLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.mainConfigLabel.setText(Messages.getString(MessageIds.GDE_MSGT2830));
				}
				{
					this.jlogVersionCombo = new CCombo(this.mainConfigGroup, SWT.BORDER);
					this.jlogVersionCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.jlogVersionCombo.setText("JLog2"); //$NON-NLS-1$
					this.jlogVersionCombo.setEditable(false);
					this.jlogVersionCombo.setBackground(SWTResourceManager.getColor(255, 128, 0));
					this.jlogVersionCombo.setLayoutData(new RowData(60, 17));
					this.jlogVersionCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "jlogVersionCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2824));
						}
					});
				}
				{
					this.jlogFirmwareCombo = new CCombo(this.mainConfigGroup, SWT.BORDER);
					this.jlogFirmwareCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.jlogFirmwareCombo.setItems(this.jlogFirmware);
					this.jlogFirmwareCombo.setBackground(SWTResourceManager.getColor(255, 128, 0));
					this.jlogFirmwareCombo.setLayoutData(new RowData(60, 17));
					this.jlogFirmwareCombo.setEditable(false);
					this.jlogFirmwareCombo.select(1);
					this.jlogFirmwareCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "cCombo1.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2825));
						}
					});
				}
				{
					this.jlogConfigurationCombo = new CCombo(this.mainConfigGroup, SWT.BORDER);
					this.jlogConfigurationCombo.setLayoutData(new RowData(420, 17));
					this.jlogConfigurationCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.jlogConfigurationCombo.setItems(this.jlogConfigurations400);
					this.jlogConfigurationCombo.setVisibleItemCount(10);
					this.jlogConfigurationCombo.setBackground(SWTResourceManager.getColor(255, 128, 0));
					this.jlogConfigurationCombo.setEditable(false);
					this.jlogConfigurationCombo.select(0);
					this.jlogConfigurationCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "jlogConfigurationCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2826));
						}
					});
					this.jlogConfigurationCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "jlogConfigurationCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (JLog2Configuration.this.configuration.version == 322 && JLog2Configuration.this.jlogConfigurationCombo.getSelectionIndex() > 0)
								JLog2Configuration.this.application.openMessageDialogAsync(JLog2Configuration.this.dialog.getDialogShell(), Messages.getString(MessageIds.GDE_MSGI2826));
							JLog2Configuration.this.configuration.switchConfig();
							JLog2Configuration.this.initialyzeGUI(JLog2Configuration.this.configuration, true);
							enableSaveSettings();
						}
					});
				}
				{
					//new Label(mainConfigGroup, SWT.NONE).setLayoutData(new RowData(this.getClientArea().width - 30, 5));
				}
				{
					this.baudrateLabel = new CLabel(this.mainConfigGroup, SWT.NONE);
					RowData baudrateLabelLData = new RowData();
					baudrateLabelLData.width = 105;
					baudrateLabelLData.height = 20;
					this.baudrateLabel.setLayoutData(baudrateLabelLData);
					this.baudrateLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.baudrateLabel.setText(Messages.getString(MessageIds.GDE_MSGT2835));
				}
				{
					this.baudrateCombo = new CCombo(this.mainConfigGroup, SWT.BORDER);
					RowData baudrateComboLData = new RowData();
					baudrateComboLData.width = 99;
					baudrateComboLData.height = 17;
					this.baudrateCombo.setLayoutData(baudrateComboLData);
					this.baudrateCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.baudrateCombo.setItems(this.baudrates);
					this.baudrateCombo.setEditable(false);
					this.baudrateCombo.setVisibleItemCount(10);
					this.baudrateCombo.select(0);
					this.baudrateCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "baudrateCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setBaudRate(JLog2Configuration.this.baudrateCombo.getSelectionIndex());
							enableSaveSettings();
						}
					});
					this.baudrateCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "baudrateCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2827));
						}
					});
				}
				{
					this.gearRatioLabel = new CLabel(this.mainConfigGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData gearRatioLabelLData = new RowData();
					gearRatioLabelLData.width = 395;
					gearRatioLabelLData.height = 20;
					this.gearRatioLabel.setLayoutData(gearRatioLabelLData);
					this.gearRatioLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.gearRatioLabel.setText(Messages.getString(MessageIds.GDE_MSGT2839));
				}
				{
					this.sysModeLabel = new CLabel(this.mainConfigGroup, SWT.NONE);
					RowData sysModeLabelLData = new RowData();
					sysModeLabelLData.width = 105;
					sysModeLabelLData.height = 20;
					this.sysModeLabel.setLayoutData(sysModeLabelLData);
					this.sysModeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.sysModeLabel.setText("SYSmode"); //$NON-NLS-1$
				}
				{
					this.sysModeCombo = new CCombo(this.mainConfigGroup, SWT.BORDER);
					RowData sysModeComboLData = new RowData();
					sysModeComboLData.width = 99;
					sysModeComboLData.height = 17;
					this.sysModeCombo.setLayoutData(sysModeComboLData);
					this.sysModeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.sysModeCombo.setItems(this.sysModes);
					this.sysModeCombo.setEditable(false);
					this.sysModeCombo.select(0);
					this.sysModeCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "sysModeCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2828));
						}
					});
					this.sysModeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "sysModeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setSysMode(JLog2Configuration.this.sysModeCombo.getSelectionIndex());
							enableSaveSettings();
						}
					});
				}
				{
					new Label(this.mainConfigGroup, SWT.NONE).setLayoutData(new RowData(19, 19));
				}
				{
					this.directRatioButton = new Button(this.mainConfigGroup, SWT.CHECK | SWT.RIGHT);
					RowData directRatioButtonLData = new RowData();
					directRatioButtonLData.width = 73;
					directRatioButtonLData.height = 21;
					this.directRatioButton.setLayoutData(directRatioButtonLData);
					this.directRatioButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.directRatioButton.setText(Messages.getString(MessageIds.GDE_MSGT2841));
					this.directRatioButton.setSelection(true);
					this.directRatioButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "directRatioButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2829));
						}
					});
					this.directRatioButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "directRatioButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.gearSelectionButton.setSelection(!JLog2Configuration.this.directRatioButton.getSelection());
							JLog2Configuration.this.configuration.setGearDirectConfig(JLog2Configuration.this.directRatioButton.getSelection());
							JLog2Configuration.this.directGearRatioMajorCombo.setEnabled(JLog2Configuration.this.directRatioButton.getSelection());
							JLog2Configuration.this.directGearRatioDecimalsCombo.setEnabled(JLog2Configuration.this.directRatioButton.getSelection());
							JLog2Configuration.this.gearPinionWheelCombo.setEnabled(!JLog2Configuration.this.directRatioButton.getSelection());
							JLog2Configuration.this.gearMainWheelCombo.setEnabled(!JLog2Configuration.this.directRatioButton.getSelection());
							JLog2Configuration.this.gearMainWheelDecimalsCombo.setEnabled(!JLog2Configuration.this.directRatioButton.getSelection());
							enableSaveSettings();
						}
					});
				}
				{
					this.directGearRatioMajorCombo = new CCombo(this.mainConfigGroup, SWT.BORDER);
					RowData directRatioComboLData = new RowData();
					directRatioComboLData.width = GDE.IS_LINUX ? 55 : 45;
					directRatioComboLData.height = 17;
					this.directGearRatioMajorCombo.setLayoutData(directRatioComboLData);
					this.directGearRatioMajorCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.directGearRatioMajorCombo.setItems(this.oneTo50);
					this.directGearRatioMajorCombo.setVisibleItemCount(10);
					this.directGearRatioMajorCombo.setEditable(false);
					this.directGearRatioMajorCombo.select(0);
					this.directGearRatioMajorCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "directRatioCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setGearRatioMinor(JLog2Configuration.this.directGearRatioMajorCombo.getText());
							enableSaveSettings();
						}
					});
					this.directGearRatioMajorCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "directRatioCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2830));
						}
					});
				}
				{
					this.dotLabel = new CLabel(this.mainConfigGroup, SWT.NONE);
					RowData dotLabelLData = new RowData();
					dotLabelLData.width = 8;
					dotLabelLData.height = 20;
					this.dotLabel.setLayoutData(dotLabelLData);
					this.dotLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.dotLabel.setText("."); //$NON-NLS-1$
				}
				{
					this.directGearRatioDecimalsCombo = new CCombo(this.mainConfigGroup, SWT.BORDER);
					RowData gearRatioMinorComboLData = new RowData();
					gearRatioMinorComboLData.width = GDE.IS_LINUX ? 55 : 45;
					gearRatioMinorComboLData.height = 17;
					this.directGearRatioDecimalsCombo.setLayoutData(gearRatioMinorComboLData);
					this.directGearRatioDecimalsCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.directGearRatioDecimalsCombo.setItems(this.zeroTo99);
					this.directGearRatioDecimalsCombo.setVisibleItemCount(10);
					this.directGearRatioDecimalsCombo.setEditable(false);
					this.directGearRatioDecimalsCombo.select(0);
					this.directGearRatioDecimalsCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "gearRatioMinorCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2831));
						}
					});
					this.directGearRatioDecimalsCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "gearRatioMinorCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setGearRatioDecimals(JLog2Configuration.this.directGearRatioDecimalsCombo.getText());
							enableSaveSettings();
						}
					});
				}
				{
					new Label(this.mainConfigGroup, SWT.NONE).setLayoutData(new RowData(GDE.IS_LINUX ? 190 : 150, 19));
				}
				{
					this.logModeLabel = new CLabel(this.mainConfigGroup, SWT.NONE);
					RowData logModeLabelLData = new RowData();
					logModeLabelLData.width = 105;
					logModeLabelLData.height = 20;
					this.logModeLabel.setLayoutData(logModeLabelLData);
					this.logModeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.logModeLabel.setText("LOGmode"); //$NON-NLS-1$
				}
				{
					this.logModeCombo = new CCombo(this.mainConfigGroup, SWT.BORDER);
					RowData logModeComboLData = new RowData();
					logModeComboLData.width = 99;
					logModeComboLData.height = 17;
					this.logModeCombo.setLayoutData(logModeComboLData);
					this.logModeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.logModeCombo.setItems(this.logModes);
					this.logModeCombo.setEditable(false);
					this.logModeCombo.select(0);
					this.logModeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "logModeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setLogMode(JLog2Configuration.this.logModeCombo.getSelectionIndex());
							if (JLog2Configuration.this.logModeCombo.getSelectionIndex() != 1) {
								enableAll(true);
								initialyzeGUI(JLog2Configuration.this.configuration, true);
							}
							else {
								enableAll(false);
							}
							enableSaveSettings();
						}
					});
					this.logModeCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "logModeCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2832));
						}
					});
				}
				{
					new Label(this.mainConfigGroup, SWT.NONE).setLayoutData(new RowData(19, 19));
				}
				{
					this.gearSelectionButton = new Button(this.mainConfigGroup, SWT.CHECK | SWT.RIGHT);
					RowData gearSelectionButtonLData = new RowData();
					gearSelectionButtonLData.width = 73;
					gearSelectionButtonLData.height = 21;
					this.gearSelectionButton.setLayoutData(gearSelectionButtonLData);
					this.gearSelectionButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.gearSelectionButton.setText(Messages.getString(MessageIds.GDE_MSGT2848));
					this.gearSelectionButton.setSelection(false);
					this.gearSelectionButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "gearSelectionButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2833));
						}
					});
					this.gearSelectionButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "gearSelectionButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.directRatioButton.setSelection(!JLog2Configuration.this.gearSelectionButton.getSelection());
							JLog2Configuration.this.configuration.setGearDirectConfig(JLog2Configuration.this.directRatioButton.getSelection());
							JLog2Configuration.this.directGearRatioMajorCombo.setEnabled(!JLog2Configuration.this.gearSelectionButton.getSelection());
							JLog2Configuration.this.directGearRatioDecimalsCombo.setEnabled(!JLog2Configuration.this.gearSelectionButton.getSelection());
							JLog2Configuration.this.gearPinionWheelCombo.setEnabled(JLog2Configuration.this.gearSelectionButton.getSelection());
							JLog2Configuration.this.gearMainWheelCombo.setEnabled(JLog2Configuration.this.gearSelectionButton.getSelection());
							JLog2Configuration.this.gearMainWheelDecimalsCombo.setEnabled(JLog2Configuration.this.gearSelectionButton.getSelection());
							updateGearRatio();
							enableSaveSettings();
						}
					});
				}
				{
					this.gearPinionWheelCombo = new CCombo(this.mainConfigGroup, SWT.BORDER);
					RowData pinionComboLData = new RowData();
					pinionComboLData.width = GDE.IS_LINUX ? 55 : 45;
					pinionComboLData.height = 17;
					this.gearPinionWheelCombo.setLayoutData(pinionComboLData);
					this.gearPinionWheelCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.gearPinionWheelCombo.setItems(this.eightTo30);
					this.gearPinionWheelCombo.setVisibleItemCount(10);
					this.gearPinionWheelCombo.setEnabled(false);
					this.gearPinionWheelCombo.setEditable(false);
					this.gearPinionWheelCombo.select(2);
					this.gearPinionWheelCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "pinionCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setGearPinionWheel(JLog2Configuration.this.gearPinionWheelCombo.getText());
							updateGearRatio();
							enableSaveSettings();
						}
					});
					this.gearPinionWheelCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "pinionCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2834));
						}
					});
				}
				{
					this.gearMainWheelLabel = new CLabel(this.mainConfigGroup, SWT.RIGHT);
					RowData mainGearLabelLData = new RowData();
					mainGearLabelLData.width = 80;
					mainGearLabelLData.height = 20;
					this.gearMainWheelLabel.setLayoutData(mainGearLabelLData);
					this.gearMainWheelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.gearMainWheelLabel.setText(Messages.getString(MessageIds.GDE_MSGT2851));
				}
				{
					this.gearMainWheelCombo = new CCombo(this.mainConfigGroup, SWT.BORDER);
					this.gearMainWheelCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.gearMainWheelCombo.setItems(this.eightTo255);
					this.gearMainWheelCombo.setVisibleItemCount(10);
					RowData mainGearComboLData = new RowData();
					mainGearComboLData.width = GDE.IS_LINUX ? 55 : 45;
					mainGearComboLData.height = 17;
					this.gearMainWheelCombo.setLayoutData(mainGearComboLData);
					this.gearMainWheelCombo.setEnabled(false);
					this.gearMainWheelCombo.setEditable(false);
					this.gearMainWheelCombo.select(2);
					this.gearMainWheelCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "mainGearCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2835));
						}
					});
					this.gearMainWheelCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "mainGearCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setGearMajorWheel(JLog2Configuration.this.gearMainWheelCombo.getText());
							updateGearRatio();
							enableSaveSettings();
						}
					});
				}
				{
					this.dotLabel = new CLabel(this.mainConfigGroup, SWT.NONE);
					this.dotLabel.setLayoutData(new RowData(8, 20));
					this.dotLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.dotLabel.setText("."); //$NON-NLS-1$
				}
				{
					this.gearMainWheelDecimalsCombo = new CCombo(this.mainConfigGroup, SWT.BORDER);
					this.gearMainWheelDecimalsCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.gearMainWheelDecimalsCombo.setItems(this.zeroTo99);
					this.gearMainWheelDecimalsCombo.setVisibleItemCount(10);
					RowData secondgearComboLData = new RowData();
					secondgearComboLData.width = GDE.IS_LINUX ? 55 : 45;
					secondgearComboLData.height = 17;
					this.gearMainWheelDecimalsCombo.setLayoutData(secondgearComboLData);
					this.gearMainWheelDecimalsCombo.setEnabled(false);
					this.gearMainWheelDecimalsCombo.setEditable(false);
					this.gearMainWheelDecimalsCombo.select(0);
					this.gearMainWheelDecimalsCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "secondgearCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setGearMajorWheelDecimals(JLog2Configuration.this.gearMainWheelDecimalsCombo.getText());
							updateGearRatio();
							enableSaveSettings();
						}
					});
					this.gearMainWheelDecimalsCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "secondgearCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2836));
						}
					});
				}
				{
					new Label(this.mainConfigGroup, SWT.NONE).setLayoutData(new RowData(GDE.IS_LINUX ? 85 : 75, 19));
				}
				{
					this.motorPolsLabel = new CLabel(this.mainConfigGroup, SWT.NONE);
					RowData motoPolsLabelLData = new RowData();
					motoPolsLabelLData.width = 105;
					motoPolsLabelLData.height = 20;
					this.motorPolsLabel.setLayoutData(motoPolsLabelLData);
					this.motorPolsLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.motorPolsLabel.setText(Messages.getString(MessageIds.GDE_MSGT2854));
				}
				{
					this.motorPolsCombo = new CCombo(this.mainConfigGroup, SWT.BORDER);
					RowData motorPolsComboLData = new RowData();
					motorPolsComboLData.width = 99;
					motorPolsComboLData.height = 17;
					this.motorPolsCombo.setLayoutData(motorPolsComboLData);
					this.motorPolsCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.motorPolsCombo.setItems(this.motorPols);
					this.motorPolsCombo.setVisibleItemCount(10);
					this.motorPolsCombo.setEditable(false);
					this.motorPolsCombo.select(6);
					this.motorPolsCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "motorPolsCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2837));
						}
					});
					this.motorPolsCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "motorPolsCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setMotorPols(JLog2Configuration.this.motorPolsCombo.getText());
							enableSaveSettings();
						}
					});
				}
				{
					this.motorShuntLabel = new CLabel(this.mainConfigGroup, SWT.RIGHT);
					RowData motorShuntLabelLData = new RowData();
					motorShuntLabelLData.width = 231;
					motorShuntLabelLData.height = 20;
					this.motorShuntLabel.setLayoutData(motorShuntLabelLData);
					this.motorShuntLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.motorShuntLabel.setText(Messages.getString(MessageIds.GDE_MSGT2826));
				}
				{
					this.motorShuntCombo = new CCombo(this.mainConfigGroup, SWT.BORDER);
					RowData motorShuntComboLData = new RowData();
					motorShuntComboLData.width = GDE.IS_LINUX ? 55 : 45;
					motorShuntComboLData.height = 17;
					this.motorShuntCombo.setLayoutData(motorShuntComboLData);
					this.motorShuntCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.motorShuntCombo.setItems(this.currentShuntAdjust);
					this.motorShuntCombo.setVisibleItemCount(10);
					this.motorShuntCombo.setEditable(false);
					this.motorShuntCombo.select(60);
					this.motorShuntCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "motorShuntCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setMotorCalibration(JLog2Configuration.this.motorShuntCombo.getText());
							enableSaveSettings();
						}
					});
					this.motorShuntCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "motorShuntCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2839));
						}
					});
				}
				{
					this.percentLabel = new CLabel(this.mainConfigGroup, SWT.NONE);
					this.percentLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.percentLabel.setText("[%]"); //$NON-NLS-1$
				}
				{
					this.mainExplanationText = new Text(this.mainConfigGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP);
					RowData mainExplanationTextLData = new RowData();
					mainExplanationTextLData.width = this.getClientArea().width - (GDE.IS_MAC ? 35 : GDE.IS_LINUX ? 0 : 45);
					mainExplanationTextLData.height = 80;
					this.mainExplanationText.setLayoutData(mainExplanationTextLData);
					this.mainExplanationText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2801));
					this.mainExplanationText.setEditable(false);
					this.mainExplanationText.setBackground(SWTResourceManager.getColor(255, 255, 128));
				}
				{
					this.flagsLabel = new CLabel(this.mainConfigGroup, SWT.NONE);
					RowData flagsLabelLData = new RowData();
					flagsLabelLData.width = 93;
					flagsLabelLData.height = 20;
					this.flagsLabel.setLayoutData(flagsLabelLData);
					this.flagsLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.flagsLabel.setText(Messages.getString(MessageIds.GDE_MSGT2860));
				}
				{
					this.resetButton = new Button(this.mainConfigGroup, SWT.CHECK | SWT.LEFT);
					RowData rstButtonLData = new RowData();
					rstButtonLData.width = 130;
					rstButtonLData.height = 23;
					this.resetButton.setLayoutData(rstButtonLData);
					this.resetButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.resetButton.setText(Messages.getString(MessageIds.GDE_MSGT2861));
					this.resetButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.resetButton.setForeground(SWTResourceManager.getColor(255, 0, 0));
					this.resetButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "rstButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2802));
						}
					});
					this.resetButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "rstButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setReset(JLog2Configuration.this.resetButton.getSelection() ? "1" : "0"); //$NON-NLS-1$ //$NON-NLS-2$
							enableSaveSettings();
						}
					});
				}
				{
					this.highPulsWidthButton = new Button(this.mainConfigGroup, SWT.CHECK | SWT.LEFT);
					RowData hpwButtonLData = new RowData();
					hpwButtonLData.width = 130;
					hpwButtonLData.height = 23;
					this.highPulsWidthButton.setLayoutData(hpwButtonLData);
					this.highPulsWidthButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.highPulsWidthButton.setText(Messages.getString(MessageIds.GDE_MSGT2863));
					this.highPulsWidthButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "hpwButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setHighPwmWarning(JLog2Configuration.this.highPulsWidthButton.getSelection() ? 2 : 0);
							enableSaveSettings();
						}
					});
					this.highPulsWidthButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "hpwButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2803));
						}
					});
				}
				{
					this.logStopButton = new Button(this.mainConfigGroup, SWT.CHECK | SWT.LEFT);
					RowData logStopButtonLData = new RowData();
					logStopButtonLData.width = 130;
					logStopButtonLData.height = 23;
					this.logStopButton.setLayoutData(logStopButtonLData);
					this.logStopButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.logStopButton.setText(Messages.getString(MessageIds.GDE_MSGT2864));
					this.logStopButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "logStopButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setLogStop(JLog2Configuration.this.logStopButton.getSelection() ? 1 : 0);
							enableSaveSettings();
						}
					});
					this.logStopButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "logStopButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2804));
						}
					});
				}
				{
					this.extRpmButton = new Button(this.mainConfigGroup, SWT.CHECK | SWT.LEFT);
					RowData extRpmButtonLData = new RowData();
					extRpmButtonLData.width = 130;
					extRpmButtonLData.height = 23;
					this.extRpmButton.setLayoutData(extRpmButtonLData);
					this.extRpmButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.extRpmButton.setText(Messages.getString(MessageIds.GDE_MSGT2836));
					this.extRpmButton.setEnabled(false);
					this.extRpmButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "extRpmButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setLogStopExtRpmSensor(JLog2Configuration.this.extRpmButton.getSelection() ? 4 : 0);
							enableSaveSettings();
						}
					});
					this.extRpmButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "extRpmButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2805));
						}
					});
				}
			}
			{
				this.alarmGroup = new Group(this, SWT.NONE);
				RowLayout alarmGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.alarmGroup.setLayout(alarmGroupLayout);
				FormData alarmGroupLData = new FormData();
				alarmGroupLData.left = new FormAttachment(0, 1000, 5);
				alarmGroupLData.top = new FormAttachment(0, 1000, 305);
				alarmGroupLData.width = GDE.IS_LINUX ? 265 : 235;
				alarmGroupLData.height = 255;
				this.alarmGroup.setLayoutData(alarmGroupLData);
				this.alarmGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 2 : 0), SWT.NORMAL));
				this.alarmGroup.setText(Messages.getString(MessageIds.GDE_MSGT2866));
				{
					this.alarmsClearButton = new Button(this.alarmGroup, SWT.PUSH | SWT.CENTER);
					RowData alarmsClearButtonLData = new RowData();
					alarmsClearButtonLData.width = 200;
					alarmsClearButtonLData.height = 25;
					this.alarmsClearButton.setLayoutData(alarmsClearButtonLData);
					this.alarmsClearButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmsClearButton.setText(Messages.getString(MessageIds.GDE_MSGT2867));
					this.alarmsClearButton.setEnabled(false);
					this.alarmsClearButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "alarmsClearButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							clearAlarms();
							enableSaveSettings();
						}
					});
					this.alarmsClearButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "alarmsClearButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2806));
						}
					});
				}
				{
					this.uBecDipDetectLabel = new CLabel(this.alarmGroup, SWT.NONE);
					RowData capacityAlarmLabelLData = new RowData();
					capacityAlarmLabelLData.width = 106;
					capacityAlarmLabelLData.height = 20;
					this.uBecDipDetectLabel.setLayoutData(capacityAlarmLabelLData);
					this.uBecDipDetectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.uBecDipDetectLabel.setText(Messages.getString(MessageIds.GDE_MSGT2868));
				}
				{
					this.uBecDipDetectButton = new Button(this.alarmGroup, SWT.CHECK | SWT.CENTER);
					RowData uBecDipDetectButtonLData = new RowData();
					uBecDipDetectButtonLData.width = 106;
					uBecDipDetectButtonLData.height = 20;
					this.uBecDipDetectButton.setLayoutData(uBecDipDetectButtonLData);
					this.uBecDipDetectButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.uBecDipDetectButton.setText("> 500 [mV]"); //$NON-NLS-1$
					this.uBecDipDetectButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "uBecDipDetectButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2807));
						}
					});
					this.uBecDipDetectButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "uBecDipDetectButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setBecDip(JLog2Configuration.this.uBecDipDetectButton.getSelection());
							checkNumberAlarms();
							checkClearButtonState();
							enableSaveSettings();
						}
					});
				}
				{
					this.capacityAlarmLabel = new CLabel(this.alarmGroup, SWT.NONE);
					RowData capacityAlarmLabelLData = new RowData();
					capacityAlarmLabelLData.width = 106;
					capacityAlarmLabelLData.height = 20;
					this.capacityAlarmLabel.setLayoutData(capacityAlarmLabelLData);
					this.capacityAlarmLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityAlarmLabel.setText(Messages.getString(MessageIds.GDE_MSGT2869));
				}
				{
					this.capacityAlarmCombo = new CCombo(this.alarmGroup, SWT.BORDER);
					RowData capacityAlarmComboLData = new RowData();
					capacityAlarmComboLData.width = GDE.IS_LINUX ? 77 : 57;
					capacityAlarmComboLData.height = 17;
					this.capacityAlarmCombo.setLayoutData(capacityAlarmComboLData);
					this.capacityAlarmCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityAlarmCombo.setItems(this.zeroTo25500);
					this.capacityAlarmCombo.setVisibleItemCount(10);
					this.capacityAlarmCombo.setEditable(false);
					this.capacityAlarmCombo.select(0);
					this.capacityAlarmCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "capacityAlarmCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setCapacityAlarm(Integer.parseInt(JLog2Configuration.this.capacityAlarmCombo.getText().trim()) / 100);
							checkNumberAlarms();
							checkClearButtonState();
							enableSaveSettings();
						}
					});
					this.capacityAlarmCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "capacityAlarmCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2808));
						}
					});
				}
				{
					this.mAhLabel = new CLabel(this.alarmGroup, SWT.NONE);
					RowData mAhLabelLData = new RowData();
					mAhLabelLData.width = 41;
					mAhLabelLData.height = 20;
					this.mAhLabel.setLayoutData(mAhLabelLData);
					this.mAhLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.mAhLabel.setText("[mAh]"); //$NON-NLS-1$
				}
				{
					this.uBatMinAlarmLabel = new CLabel(this.alarmGroup, SWT.NONE);
					RowData uBatMinAlarmLabelLData = new RowData();
					uBatMinAlarmLabelLData.width = 106;
					uBatMinAlarmLabelLData.height = 20;
					this.uBatMinAlarmLabel.setLayoutData(uBatMinAlarmLabelLData);
					this.uBatMinAlarmLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.uBatMinAlarmLabel.setText(Messages.getString(MessageIds.GDE_MSGT2871));
				}
				{
					this.voltageBatteryAlarmCombo = new CCombo(this.alarmGroup, SWT.BORDER);
					RowData voltagebatteryMinCombo1LData = new RowData();
					voltagebatteryMinCombo1LData.width = GDE.IS_LINUX ? 45 : 35;
					voltagebatteryMinCombo1LData.height = 17;
					this.voltageBatteryAlarmCombo.setLayoutData(voltagebatteryMinCombo1LData);
					this.voltageBatteryAlarmCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageBatteryAlarmCombo.setItems(this.zeroTo50);
					this.voltageBatteryAlarmCombo.setVisibleItemCount(10);
					this.voltageBatteryAlarmCombo.setEditable(false);
					this.voltageBatteryAlarmCombo.select(0);
					this.voltageBatteryAlarmCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "voltagebatteryMinCombo1.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2809));
						}
					});
					this.voltageBatteryAlarmCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "voltagebatteryMinCombo1.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setBatteryAlarmMajor(JLog2Configuration.this.voltageBatteryAlarmCombo.getText().trim());
							checkNumberAlarms();
							checkClearButtonState();
							enableSaveSettings();
						}
					});
				}
				{
					this.dotLabel = new CLabel(this.alarmGroup, SWT.NONE);
					this.dotLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.dotLabel.setText("."); //$NON-NLS-1$
					RowData RALData = new RowData();
					RALData.width = 8;
					RALData.height = 20;
					this.dotLabel.setLayoutData(RALData);
				}
				{
					this.voltageBatteryAlarmDecimalsCombo = new CCombo(this.alarmGroup, SWT.BORDER);
					RowData voltagebatteryAlarmMaxCombo2LData = new RowData();
					voltagebatteryAlarmMaxCombo2LData.width = GDE.IS_LINUX ? 45 : 35;
					voltagebatteryAlarmMaxCombo2LData.height = 17;
					this.voltageBatteryAlarmDecimalsCombo.setLayoutData(voltagebatteryAlarmMaxCombo2LData);
					this.voltageBatteryAlarmDecimalsCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageBatteryAlarmDecimalsCombo.setItems(this.zeroTo9);
					this.voltageBatteryAlarmDecimalsCombo.setVisibleItemCount(10);
					this.voltageBatteryAlarmDecimalsCombo.setEditable(false);
					this.voltageBatteryAlarmDecimalsCombo.select(0);
					this.voltageBatteryAlarmDecimalsCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "voltagebatteryAlarmMaxCombo2.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setBatteryAlarmDecimals(JLog2Configuration.this.voltageBatteryAlarmDecimalsCombo.getText().trim());
							checkNumberAlarms();
							checkClearButtonState();
							enableSaveSettings();
						}
					});
					this.voltageBatteryAlarmDecimalsCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "voltagebatteryAlarmMaxCombo2.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2810));
						}
					});
				}
				{
					this.voltageLabel = new CLabel(this.alarmGroup, SWT.NONE);
					RowData voltageLabelLData = new RowData();
					voltageLabelLData.width = 23;
					voltageLabelLData.height = 19;
					this.voltageLabel.setLayoutData(voltageLabelLData);
					this.voltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageLabel.setText("[V]"); //$NON-NLS-1$
				}
				{
					this.fetTempMaxLabel = new CLabel(this.alarmGroup, SWT.NONE);
					RowData paTempMaxLabelLData = new RowData();
					paTempMaxLabelLData.width = 106;
					paTempMaxLabelLData.height = 20;
					this.fetTempMaxLabel.setLayoutData(paTempMaxLabelLData);
					this.fetTempMaxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.fetTempMaxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2873));
				}
				{
					this.fetTempMaxCombo = new CCombo(this.alarmGroup, SWT.BORDER);
					RowData paTempMaxComboLData = new RowData();
					paTempMaxComboLData.width = GDE.IS_LINUX ? 55 : 45;
					paTempMaxComboLData.height = 17;
					this.fetTempMaxCombo.setLayoutData(paTempMaxComboLData);
					this.fetTempMaxCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.fetTempMaxCombo.setItems(this.zeroTo127);
					this.fetTempMaxCombo.setVisibleItemCount(10);
					this.fetTempMaxCombo.setEditable(false);
					this.fetTempMaxCombo.select(0);
					this.fetTempMaxCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "paTempMaxCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2811));
						}
					});
					this.fetTempMaxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "paTempMaxCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setPaMaxTempAlarm(JLog2Configuration.this.fetTempMaxCombo.getText().trim());
							checkNumberAlarms();
							checkClearButtonState();
							enableSaveSettings();
						}
					});
				}
				{
					this.temperaureLabel = new CLabel(this.alarmGroup, SWT.NONE);
					RowData temperaureLabelLData = new RowData();
					temperaureLabelLData.width = 27;
					temperaureLabelLData.height = 19;
					this.temperaureLabel.setLayoutData(temperaureLabelLData);
					this.temperaureLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.temperaureLabel.setText("[°C]"); //$NON-NLS-1$
				}
				{
					this.ext1Label = new CLabel(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext1LabelLData = new RowData();
					ext1LabelLData.width = 106;
					ext1LabelLData.height = 20;
					this.ext1Label.setLayoutData(ext1LabelLData);
					this.ext1Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.ext1Label.setText(Messages.getString(MessageIds.GDE_MSGT2874));
				}
				{
					this.extern1Combo = new CCombo(this.alarmGroup, SWT.BORDER);
					RowData extern1ComboLData = new RowData();
					extern1ComboLData.width = GDE.IS_LINUX ? 55 : 45;
					extern1ComboLData.height = 17;
					this.extern1Combo.setLayoutData(extern1ComboLData);
					this.extern1Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.extern1Combo.setItems(this.zeroTo127);
					this.extern1Combo.setVisibleItemCount(10);
					this.extern1Combo.setEnabled(false);
					this.extern1Combo.setEditable(false);
					this.extern1Combo.select(0);
					this.extern1Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "extern1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setExtTemp1(JLog2Configuration.this.extern1Combo.getText().trim());
							if (JLog2Configuration.this.extern1Combo.getSelectionIndex() > 0)
								JLog2Configuration.this.configuration.setExtTemp1LowerThan(JLog2Configuration.this.ext1smallerButton.getSelection() ? 1 : 0);
							else
								JLog2Configuration.this.configuration.setExtTemp1LowerThan(0);
							checkNumberAlarms();
							checkClearButtonState();
							enableSaveSettings();
						}
					});
					this.extern1Combo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "extern1Combo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2812));
						}
					});
				}
				{
					this.temperaure1Label = new CLabel(this.alarmGroup, SWT.NONE);
					this.temperaure1Label.setLayoutData(new RowData(27, 19));
					this.temperaure1Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.temperaure1Label.setText("[°C]"); //$NON-NLS-1$
				}
				{
					this.ext1smallerButton = new Button(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext1smallerButtonLData = new RowData();
					ext1smallerButtonLData.width = GDE.IS_LINUX ? 45 : 32;
					ext1smallerButtonLData.height = 20;
					this.ext1smallerButton.setLayoutData(ext1smallerButtonLData);
					this.ext1smallerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.ext1smallerButton.setText(" <"); //$NON-NLS-1$
					this.ext1smallerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.ext1smallerButton.setEnabled(false);
					this.ext1smallerButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "ext1smallerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (JLog2Configuration.this.extern1Combo.getSelectionIndex() > 0)
								JLog2Configuration.this.configuration.setExtTemp1LowerThan(JLog2Configuration.this.ext1smallerButton.getSelection() ? 1 : 0);
							checkClearButtonState();
							enableSaveSettings();
						}
					});
					this.ext1smallerButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "ext1smallerButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2812));
						}
					});
				}
				{
					this.ext2Label = new CLabel(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext2LabelLData = new RowData();
					ext2LabelLData.width = 106;
					ext2LabelLData.height = 20;
					this.ext2Label.setLayoutData(ext2LabelLData);
					this.ext2Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.ext2Label.setText(Messages.getString(MessageIds.GDE_MSGT2875));
				}
				{
					this.extern2Combo = new CCombo(this.alarmGroup, SWT.BORDER);
					RowData extern2ComboLData = new RowData();
					extern2ComboLData.width = GDE.IS_LINUX ? 55 : 45;
					extern2ComboLData.height = 17;
					this.extern2Combo.setLayoutData(extern2ComboLData);
					this.extern2Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.extern2Combo.setItems(this.zeroTo127);
					this.extern2Combo.setVisibleItemCount(10);
					this.extern2Combo.setEnabled(false);
					this.extern2Combo.setEditable(false);
					this.extern2Combo.select(0);
					this.extern2Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "extern2Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setExtTemp2(JLog2Configuration.this.extern2Combo.getText().trim());
							if (JLog2Configuration.this.extern2Combo.getSelectionIndex() > 0)
								JLog2Configuration.this.configuration.setExtTemp2LowerThan(JLog2Configuration.this.ext2smallerButton.getSelection() ? 1 : 0);
							else
								JLog2Configuration.this.configuration.setExtTemp2LowerThan(0);
							checkNumberAlarms();
							checkClearButtonState();
							enableSaveSettings();
						}
					});
					this.extern2Combo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "extern2Combo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2812));
						}
					});
				}
				{
					this.temperaureLabel = new CLabel(this.alarmGroup, SWT.NONE);
					this.temperaureLabel.setLayoutData(new RowData(27, 19));
					this.temperaureLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.temperaureLabel.setText("[°C]"); //$NON-NLS-1$
				}
				{
					this.ext2smallerButton = new Button(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext2smallerButtonLData = new RowData();
					ext2smallerButtonLData.width = GDE.IS_LINUX ? 45 : 32;
					ext2smallerButtonLData.height = 20;
					this.ext2smallerButton.setLayoutData(ext2smallerButtonLData);
					this.ext2smallerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.ext2smallerButton.setText(" <"); //$NON-NLS-1$
					this.ext2smallerButton.setEnabled(false);
					this.ext2smallerButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "ext2smallerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (JLog2Configuration.this.extern2Combo.getSelectionIndex() > 0)
								JLog2Configuration.this.configuration.setExtTemp2LowerThan(JLog2Configuration.this.ext2smallerButton.getSelection() ? 1 : 0);
							checkClearButtonState();
							enableSaveSettings();
						}
					});
					this.ext2smallerButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "ext2smallerButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2812));
						}
					});
				}
				{
					this.ext3Label = new CLabel(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext3LabelLData = new RowData();
					ext3LabelLData.width = 106;
					ext3LabelLData.height = 20;
					this.ext3Label.setLayoutData(ext3LabelLData);
					this.ext3Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.ext3Label.setText(Messages.getString(MessageIds.GDE_MSGT2876));
				}
				{
					this.extern3Combo = new CCombo(this.alarmGroup, SWT.BORDER);
					RowData extern3ComboLData = new RowData();
					extern3ComboLData.width = GDE.IS_LINUX ? 55 : 45;
					extern3ComboLData.height = 17;
					this.extern3Combo.setLayoutData(extern3ComboLData);
					this.extern3Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.extern3Combo.setItems(this.zeroTo127);
					this.extern3Combo.setVisibleItemCount(10);
					this.extern3Combo.setEnabled(false);
					this.extern3Combo.setEditable(false);
					this.extern3Combo.select(0);
					this.extern3Combo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "extern3Combo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2812));
						}
					});
					this.extern3Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "extern3Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setExtTemp3(JLog2Configuration.this.extern3Combo.getText().trim());
							if (JLog2Configuration.this.extern3Combo.getSelectionIndex() > 0)
								JLog2Configuration.this.configuration.setExtTemp3LowerThan(JLog2Configuration.this.ext3smallerButton.getSelection() ? 1 : 0);
							else
								JLog2Configuration.this.configuration.setExtTemp3LowerThan(0);
							checkNumberAlarms();
							checkClearButtonState();
							enableSaveSettings();
						}
					});
				}
				{
					this.temperaureLabel = new CLabel(this.alarmGroup, SWT.NONE);
					this.temperaureLabel.setLayoutData(new RowData(27, 19));
					this.temperaureLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.temperaureLabel.setText("[°C]"); //$NON-NLS-1$
				}
				{
					this.ext3smallerButton = new Button(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext3smallerButtonLData = new RowData();
					ext3smallerButtonLData.width = GDE.IS_LINUX ? 45 : 32;
					ext3smallerButtonLData.height = 20;
					this.ext3smallerButton.setLayoutData(ext3smallerButtonLData);
					this.ext3smallerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.ext3smallerButton.setText(" <"); //$NON-NLS-1$
					this.ext3smallerButton.setEnabled(false);
					this.ext3smallerButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "ext3smallerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (JLog2Configuration.this.extern3Combo.getSelectionIndex() > 0)
								JLog2Configuration.this.configuration.setExtTemp3LowerThan(JLog2Configuration.this.ext3smallerButton.getSelection() ? 1 : 0);
							checkClearButtonState();
							enableSaveSettings();
						}
					});
					this.ext3smallerButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "ext3smallerButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2812));
						}
					});
				}
				{
					this.ext4Label = new CLabel(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext4LabelLData = new RowData();
					ext4LabelLData.width = 106;
					ext4LabelLData.height = 20;
					this.ext4Label.setLayoutData(ext4LabelLData);
					this.ext4Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.ext4Label.setText(Messages.getString(MessageIds.GDE_MSGT2879));
				}
				{
					this.extern4Combo = new CCombo(this.alarmGroup, SWT.BORDER);
					RowData extern4ComboLData = new RowData();
					extern4ComboLData.width = GDE.IS_LINUX ? 55 : 45;
					extern4ComboLData.height = 17;
					this.extern4Combo.setLayoutData(extern4ComboLData);
					this.extern4Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.extern4Combo.setItems(this.zeroTo127);
					this.extern4Combo.setVisibleItemCount(10);
					this.extern4Combo.setEnabled(false);
					this.extern4Combo.setEditable(false);
					this.extern4Combo.select(0);
					this.extern4Combo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "extern4Combo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2812));
						}
					});
					this.extern4Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "extern4Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setExtTemp4(JLog2Configuration.this.extern4Combo.getText().trim());
							if (JLog2Configuration.this.extern4Combo.getSelectionIndex() > 0)
								JLog2Configuration.this.configuration.setExtTemp4LowerThan(JLog2Configuration.this.ext4smallerButton.getSelection() ? 1 : 0);
							else
								JLog2Configuration.this.configuration.setExtTemp4LowerThan(0);
							checkNumberAlarms();
							checkClearButtonState();
							enableSaveSettings();
						}
					});
				}
				{
					this.temperaureLabel = new CLabel(this.alarmGroup, SWT.NONE);
					this.temperaureLabel.setLayoutData(new RowData(27, 19));
					this.temperaureLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.temperaureLabel.setText("[°C]"); //$NON-NLS-1$
				}
				{
					this.ext4smallerButton = new Button(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext4smallerButtonLData = new RowData();
					ext4smallerButtonLData.width = GDE.IS_LINUX ? 45 : 32;
					ext4smallerButtonLData.height = 20;
					this.ext4smallerButton.setLayoutData(ext4smallerButtonLData);
					this.ext4smallerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.ext4smallerButton.setText(" <"); //$NON-NLS-1$
					this.ext4smallerButton.setEnabled(false);
					this.ext4smallerButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "ext4smallerButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2812));
						}
					});
					this.ext4smallerButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "ext4smallerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (JLog2Configuration.this.extern4Combo.getSelectionIndex() > 0)
								JLog2Configuration.this.configuration.setExtTemp4LowerThan(JLog2Configuration.this.ext4smallerButton.getSelection() ? 1 : 0);
							checkClearButtonState();
							enableSaveSettings();
						}
					});
				}
				{
					this.ext5Label = new CLabel(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext5LabelLData = new RowData();
					ext5LabelLData.width = 106;
					ext5LabelLData.height = 20;
					this.ext5Label.setLayoutData(ext5LabelLData);
					this.ext5Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.ext5Label.setText(Messages.getString(MessageIds.GDE_MSGT2880));
				}
				{
					this.extern5Combo = new CCombo(this.alarmGroup, SWT.BORDER);
					RowData extern5ComboLData = new RowData();
					extern5ComboLData.width = GDE.IS_LINUX ? 55 : 45;
					extern5ComboLData.height = 17;
					this.extern5Combo.setLayoutData(extern5ComboLData);
					this.extern5Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.extern5Combo.setItems(this.zeroTo127);
					this.extern5Combo.setVisibleItemCount(10);
					this.extern5Combo.setEnabled(false);
					this.extern5Combo.setEditable(false);
					this.extern5Combo.select(0);
					this.extern5Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "extern5Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setExtTemp5(JLog2Configuration.this.extern5Combo.getText().trim());
							if (JLog2Configuration.this.extern5Combo.getSelectionIndex() > 0)
								JLog2Configuration.this.configuration.setExtTemp5LowerThan(JLog2Configuration.this.ext5smallerButton.getSelection() ? 1 : 0);
							else
								JLog2Configuration.this.configuration.setExtTemp5LowerThan(0);
							checkNumberAlarms();
							checkClearButtonState();
							enableSaveSettings();
						}
					});
					this.extern5Combo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "extern5Combo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2812));
						}
					});
				}
				{
					this.temperaureLabel = new CLabel(this.alarmGroup, SWT.NONE);
					this.temperaureLabel.setLayoutData(new RowData(27, 19));
					this.temperaureLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.temperaureLabel.setText("[°C]"); //$NON-NLS-1$
				}
				{
					this.ext5smallerButton = new Button(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext5smallerButtonLData = new RowData();
					ext5smallerButtonLData.width = GDE.IS_LINUX ? 45 : 32;
					ext5smallerButtonLData.height = 20;
					this.ext5smallerButton.setLayoutData(ext5smallerButtonLData);
					this.ext5smallerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.ext5smallerButton.setText(" <"); //$NON-NLS-1$
					this.ext5smallerButton.setEnabled(false);
					this.ext5smallerButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "ext5smallerButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2812));
						}
					});
					this.ext5smallerButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "ext5smallerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (JLog2Configuration.this.extern5Combo.getSelectionIndex() > 0)
								JLog2Configuration.this.configuration.setExtTemp5LowerThan(JLog2Configuration.this.ext5smallerButton.getSelection() ? 1 : 0);
							checkClearButtonState();
							enableSaveSettings();
						}
					});
				}
			}
			{
				this.optionalGroup = new Group(this, SWT.NONE);
				RowLayout optionalGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.optionalGroup.setLayout(optionalGroupLayout);
				FormData optionalGroupLData = new FormData();
				optionalGroupLData.top = new FormAttachment(0, 1000, 305);
				optionalGroupLData.right = new FormAttachment(1000, 1000, -7);
				optionalGroupLData.width = GDE.IS_LINUX ? 440 : 400;
				optionalGroupLData.height = 255;
				this.optionalGroup.setLayoutData(optionalGroupLData);
				this.optionalGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 2 : 0), SWT.NORMAL));
				this.optionalGroup.setText(Messages.getString(MessageIds.GDE_MSGT2881));
				{
					this.alarmLinesLabel = new CLabel(this.optionalGroup, SWT.CENTER);
					RowData alarmLinesButtonLData = new RowData();
					alarmLinesButtonLData.width = 115;
					alarmLinesButtonLData.height = 20;
					this.alarmLinesLabel.setLayoutData(alarmLinesButtonLData);
					this.alarmLinesLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmLinesLabel.setText(Messages.getString(MessageIds.GDE_MSGT2882));
				}
				{
					this.alarmLinesCombo = new CCombo(this.optionalGroup, SWT.BORDER);
					RowData alarmLinesComboLData = new RowData();
					alarmLinesComboLData.width = GDE.IS_LINUX ? 45 : 32;
					alarmLinesComboLData.height = 17;
					this.alarmLinesCombo.setLayoutData(alarmLinesComboLData);
					this.alarmLinesCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmLinesCombo.setItems(this.zeroAlarms);
					this.alarmLinesCombo.setEditable(false);
					this.alarmLinesCombo.select(0);
					this.alarmLinesCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "alarmLinesCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (JLog2Configuration.this.alarmLinesCombo.getSelectionIndex() >= 1) {
								JLog2Configuration.this.alarmLinesLabel.setForeground(DataExplorer.getInstance().COLOR_RED);
							}
							else {
								JLog2Configuration.this.alarmLinesLabel.setForeground(DataExplorer.getInstance().COLOR_BLACK);
							}
							checkAdapterRequired();
							JLog2Configuration.this.configuration.setNumberAlarmLines(JLog2Configuration.this.alarmLinesCombo.getSelectionIndex());
							enableSaveSettings();
						}
					});
					this.alarmLinesCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "alarmLinesCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2813));
						}
					});
				}
				{
					this.line1signalTypeLabel = new CLabel(this.optionalGroup, SWT.RIGHT);
					RowData Line1signalLabelLData = new RowData();
					Line1signalLabelLData.width = 128;
					Line1signalLabelLData.height = 20;
					this.line1signalTypeLabel.setLayoutData(Line1signalLabelLData);
					this.line1signalTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.line1signalTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT2883));
				}
				{
					this.line1signalTypeCombo = new CCombo(this.optionalGroup, SWT.BORDER);
					RowData line1signalComboLData = new RowData();
					line1signalComboLData.width = GDE.IS_LINUX ? 85 : 75;
					line1signalComboLData.height = 17;
					this.line1signalTypeCombo.setLayoutData(line1signalComboLData);
					this.line1signalTypeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.line1signalTypeCombo.setItems(new String[] { "switched", "flash", "interval", "Morse" }); //$NON-NLS-1$
					this.line1signalTypeCombo.setEditable(false);
					this.line1signalTypeCombo.select(0);
					this.line1signalTypeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "line1signalCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							switch (JLog2Configuration.this.line1signalTypeCombo.getSelectionIndex()) {
							case 0: //switch
								JLog2Configuration.this.configuration.setLine1signalType(0);
								break;
							case 1: //interval
								JLog2Configuration.this.configuration.setLine1signalType(64);
								break;
							case 2: //flash
								JLog2Configuration.this.configuration.setLine1signalType(32);
								break;
							case 3: //Morse
								JLog2Configuration.this.configuration.setLine1signalType(128);
								break;
							}
							enableSaveSettings();
						}
					});
					this.line1signalTypeCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "line1signalCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2814));
						}
					});
				}
				{
					this.subDevicesLabel = new CLabel(this.optionalGroup, SWT.CENTER);
					RowData subDevicesButtonLData = new RowData();
					subDevicesButtonLData.width = 115;
					subDevicesButtonLData.height = 20;
					this.subDevicesLabel.setLayoutData(subDevicesButtonLData);
					this.subDevicesLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.subDevicesLabel.setText(Messages.getString(MessageIds.GDE_MSGT2884));
					this.subDevicesLabel.setEnabled(false);
				}
				{
					this.subDevicesCombo = new CCombo(this.optionalGroup, SWT.BORDER);
					this.subDevicesCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.subDevicesCombo.setText(Messages.getString(MessageIds.GDE_MSGT2885));
					this.subDevicesCombo.setEditable(false);
					RowData subDevicesComboLData = new RowData();
					subDevicesComboLData.width = GDE.IS_LINUX ? 75 : 65;
					subDevicesComboLData.height = 17;
					this.subDevicesCombo.setLayoutData(subDevicesComboLData);
					this.subDevicesCombo.setEnabled(false);
				}
				{
					this.tempSensorTypeLabel = new CLabel(this.optionalGroup, SWT.RIGHT);
					RowData tempSensorTypeButtonLData = new RowData();
					tempSensorTypeButtonLData.width = 95;
					tempSensorTypeButtonLData.height = 20;
					this.tempSensorTypeLabel.setLayoutData(tempSensorTypeButtonLData);
					this.tempSensorTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tempSensorTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT2886));
				}
				{
					this.tempSensorTypeCombo = new CCombo(this.optionalGroup, SWT.BORDER);
					RowData tempSensorTypeComboLData = new RowData();
					tempSensorTypeComboLData.width = GDE.IS_LINUX ? 85 : 75;
					tempSensorTypeComboLData.height = 17;
					this.tempSensorTypeCombo.setLayoutData(tempSensorTypeComboLData);
					this.tempSensorTypeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tempSensorTypeCombo.setItems(new String[] { " --- ", "analog", "digital" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					this.tempSensorTypeCombo.setEditable(false);
					this.tempSensorTypeCombo.select(0);
					this.tempSensorTypeCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "tempSensorTypeCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2815));
						}
					});
					this.tempSensorTypeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "tempSensorTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							setTemperaturSensorType(JLog2Configuration.this.tempSensorTypeCombo.getSelectionIndex());
							JLog2Configuration.this.configuration.setTemperaturSensorType(JLog2Configuration.this.tempSensorTypeCombo.getSelectionIndex());
							checkNumberAlarms();
							enableSaveSettings();
						}
					});
				}
				{
					this.rpmSensorLabel = new CLabel(this.optionalGroup, SWT.CENTER);
					RowData rpmSensorButtonLData = new RowData();
					rpmSensorButtonLData.width = 115;
					rpmSensorButtonLData.height = 20;
					this.rpmSensorLabel.setLayoutData(rpmSensorButtonLData);
					this.rpmSensorLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.rpmSensorLabel.setText(Messages.getString(MessageIds.GDE_MSGT2887));
				}
				{
					this.pulsPerRevolutionSensorCombo = new CCombo(this.optionalGroup, SWT.BORDER);
					RowData rpmSensorComboLData = new RowData();
					rpmSensorComboLData.width = GDE.IS_LINUX ? 75 : 65;
					rpmSensorComboLData.height = 17;
					this.pulsPerRevolutionSensorCombo.setLayoutData(rpmSensorComboLData);
					this.pulsPerRevolutionSensorCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.pulsPerRevolutionSensorCombo.setItems(this.zeroTo127);
					this.pulsPerRevolutionSensorCombo.setEditable(false);
					this.pulsPerRevolutionSensorCombo.select(0);
					this.pulsPerRevolutionSensorCombo.setVisibleItemCount(10);
					this.pulsPerRevolutionSensorCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "rpmSensorCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (JLog2Configuration.this.brushLessButton.getSelection()) {
								JLog2Configuration.this.configuration.setIsBrushlessMotor(true, JLog2Configuration.this.pulsPerRevolutionSensorCombo.getSelectionIndex() * 2 + 2);
							}
							else {
								JLog2Configuration.this.configuration.setPulsePerRevolution(JLog2Configuration.this.pulsPerRevolutionSensorCombo.getSelectionIndex());
							}
							if (JLog2Configuration.this.pulsPerRevolutionSensorCombo.getSelectionIndex() < 1 && !JLog2Configuration.this.brushLessButton.getSelection()) {
								JLog2Configuration.this.motorButton.setSelection(false);
								JLog2Configuration.this.configuration.setIsMotor(false);
								JLog2Configuration.this.motorButton.setEnabled(false);
								JLog2Configuration.this.extRpmButton.setEnabled(false);
							}
							else if (JLog2Configuration.this.motorButton.getSelection() && Integer.parseInt(JLog2Configuration.this.pulsPerRevolutionSensorCombo.getText().trim()) >= 2) {
								JLog2Configuration.this.brushLessButton.setEnabled(true);
							}
							else {
								JLog2Configuration.this.motorButton.setEnabled(true);
								JLog2Configuration.this.extRpmButton.setEnabled(true);
							}
							checkAdapterRequired();
							enableSaveSettings();
						}
					});
					this.pulsPerRevolutionSensorCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "rpmSensorCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							if (JLog2Configuration.this.brushLessButton.getSelection())
								JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2816));
							else
								JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2817));
						}
					});
				}
				{
					new Label(this.optionalGroup, SWT.NONE).setLayoutData(new RowData(9, 19));
				}
				{
					this.motorButton = new Button(this.optionalGroup, SWT.CHECK | SWT.CENTER);
					RowData motorButtonLData = new RowData();
					motorButtonLData.width = 80;
					motorButtonLData.height = 20;
					this.motorButton.setLayoutData(motorButtonLData);
					this.motorButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.motorButton.setText(Messages.getString(MessageIds.GDE_MSGT2888));
					this.motorButton.setEnabled(false);
					this.motorButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "motorButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2818));
						}
					});
					this.motorButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "motorButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setIsMotor(JLog2Configuration.this.motorButton.getSelection());
							if (JLog2Configuration.this.motorButton.getSelection() && Integer.parseInt(JLog2Configuration.this.pulsPerRevolutionSensorCombo.getText().trim()) >= 2) {
								JLog2Configuration.this.brushLessButton.setEnabled(true);
							}
							else {
								JLog2Configuration.this.brushLessButton.setEnabled(false);
							}
							enableSaveSettings();
						}
					});
				}
				{
					this.brushLessButton = new Button(this.optionalGroup, SWT.CHECK | SWT.CENTER);
					RowData brushLessButtonLData = new RowData();
					brushLessButtonLData.width = 100;
					brushLessButtonLData.height = 20;
					this.brushLessButton.setLayoutData(brushLessButtonLData);
					this.brushLessButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.brushLessButton.setText(Messages.getString(MessageIds.GDE_MSGT2889));
					this.brushLessButton.setEnabled(false);
					this.brushLessButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "brushLessButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.configuration.setIsBrushlessMotor(JLog2Configuration.this.brushLessButton.getSelection(), JLog2Configuration.this.motorPolsCombo.getSelectionIndex() * 2 + 2);
							if (JLog2Configuration.this.brushLessButton.getSelection()) {
								JLog2Configuration.this.pulsPerRevolutionSensorCombo.setItems(JLog2Configuration.this.motorPols);
								JLog2Configuration.this.pulsPerRevolutionSensorCombo.select(JLog2Configuration.this.motorPolsCombo.getSelectionIndex());
								JLog2Configuration.this.rpmSensorLabel.setText(Messages.getString(MessageIds.GDE_MSGT2890));
								JLog2Configuration.this.rpmSensorLabel.setForeground(DataExplorer.getInstance().COLOR_RED);
								JLog2Configuration.this.configuration.setPulsePerRevolution(1);
							}
							else {
								JLog2Configuration.this.pulsPerRevolutionSensorCombo.setItems(JLog2Configuration.this.zeroTo127);
								JLog2Configuration.this.pulsPerRevolutionSensorCombo.select(1);
								JLog2Configuration.this.rpmSensorLabel.setText(Messages.getString(MessageIds.GDE_MSGT2891));
								JLog2Configuration.this.rpmSensorLabel.setForeground(DataExplorer.getInstance().COLOR_BLACK);
								JLog2Configuration.this.configuration.setPulsePerRevolution(1);
							}
							enableSaveSettings();
						}
					});
					this.brushLessButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "brushLessButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2819));
						}
					});
				}
				{
					this.telemetryLabel = new CLabel(this.optionalGroup, SWT.CENTER);
					RowData telemetryButtonLData = new RowData();
					telemetryButtonLData.width = 115;
					telemetryButtonLData.height = 20;
					this.telemetryLabel.setLayoutData(telemetryButtonLData);
					this.telemetryLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.telemetryLabel.setText(Messages.getString(MessageIds.GDE_MSGT2892));
				}
				{
					this.telemetryCombo = new CCombo(this.optionalGroup, SWT.BORDER);
					RowData telemetryComboLData = new RowData();
					telemetryComboLData.width = GDE.IS_LINUX ? 123 : 113;
					telemetryComboLData.height = 17;
					this.telemetryCombo.setLayoutData(telemetryComboLData);
					this.telemetryCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.telemetryCombo.setItems(this.comOutputString.split(GDE.STRING_COMMA));
					this.telemetryCombo.setEditable(false);
					this.telemetryCombo.select(0);
					this.telemetryCombo.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "telemetryCombo.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2820));
						}
					});
					this.telemetryCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "telemetryCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.application.openMessageDialogAsync(JLog2Configuration.this.dialog.getDialogShell(), Messages.getString(MessageIds.GDE_MSGI2820));
							enableMpxAddressSelection(false);
							JLog2Configuration.this.configuration.setTelemetryType(JLog2Configuration.this.telemetryCombo.getSelectionIndex());
							switch (JLog2Configuration.this.telemetryCombo.getSelectionIndex()) {
							case 0: //none
								JLog2Configuration.this.configuration.setTelemetryBaudrateType(0);
								break;
							case 1: //FTDI live stream
								JLog2Configuration.this.configuration.setTelemetryBaudrateType(4);
								break;
							//case 2: //Unidisplay
							//	configuration.setTelemetryBaudrateType(0);
							//	break;
							case 3: //MPX
								if (JLog2Configuration.this.telemetryCombo.getText().contains("MPX"))
									enableMpxAddressSelection(true);
								break;
							//case 4: //Jeti v1
							//	configuration.setTelemetryBaudrateType(0);
							//	break;
							}
							enableSaveSettings();
						}
					});
				}
				{
					this.sensorAdapterButton = new Button(this.optionalGroup, SWT.PUSH | SWT.CENTER);
					RowData sensorAdapterButtonLData = new RowData();
					sensorAdapterButtonLData.width = 145;
					sensorAdapterButtonLData.height = 26;
					this.sensorAdapterButton.setLayoutData(sensorAdapterButtonLData);
					this.sensorAdapterButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.sensorAdapterButton.setText(Messages.getString(MessageIds.GDE_MSGT2893));
					this.sensorAdapterButton.setEnabled(false);
					this.sensorAdapterButton.addMouseMoveListener(new MouseMoveListener() {
						@Override
						public void mouseMove(MouseEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "sensorAdapterButton.mouseMove, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2821));
						}
					});
					this.sensorAdapterButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Configuration.log.log(Level.FINEST, "sensorAdapterButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Configuration.this.application.openHelpDialog(JLog2Configuration.this.device.getName(), "HelpInfo.html", true); //$NON-NLS-1$
						}
					});
				}
				{
					this.optionalStuff = new Composite(this.optionalGroup, SWT.NONE);
					RowLayout optionalStuffLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					this.optionalStuff.setLayout(optionalStuffLayout);
					this.optionalStuff.setLayoutData(new RowData(385, 25)); //this.getClientArea().width - (GDE.IS_MAC ? 35 : GDE.IS_LINUX ? 0 : 45)
					{
						this.hv2BecLabel = new CLabel(this.optionalStuff, SWT.RIGHT);
						RowData hv2BecLabelLData = new RowData();
						hv2BecLabelLData.width = 75;
						hv2BecLabelLData.height = 20;
						this.hv2BecLabel.setLayoutData(hv2BecLabelLData);
						this.hv2BecLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.hv2BecLabel.setText("HV²BEC [V]"); //$NON-NLS-1$
					}
					{
						this.hv2BecCombo = new CCombo(this.optionalStuff, SWT.BORDER);
						this.hv2BecCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.hv2BecCombo.setItems(new String[] { "6.0", "6.5", "7.0", "7.5", "8.0", "8.5" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
						this.hv2BecCombo.setLayoutData(new RowData((GDE.IS_LINUX ? 70 : 60), 17));
						this.hv2BecCombo.setVisible(false);
						this.hv2BecCombo.setEditable(false);
						this.hv2BecCombo.select(0);
						this.hv2BecCombo.addMouseWheelListener(new MouseWheelListener() {
							@Override
							public void mouseScrolled(MouseEvent evt) {
								JLog2Configuration.log.log(Level.FINEST, "hv2BecCombo.mouseScrolled, event=" + evt); //$NON-NLS-1$
								JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2822));
							}
						});
						this.hv2BecCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								JLog2Configuration.log.log(Level.FINEST, "hv2BecCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								JLog2Configuration.this.configuration.setHV2BecVoltage(JLog2Configuration.this.hv2BecCombo.getSelectionIndex());
								enableSaveSettings();
							}
						});
					}
					{
						this.speedSensorLabel = new CLabel(this.optionalStuff, SWT.RIGHT);
						RowData speedSensorLabelLData = new RowData();
						speedSensorLabelLData.width = 140;
						speedSensorLabelLData.height = 20;
						this.speedSensorLabel.setLayoutData(speedSensorLabelLData);
						this.speedSensorLabel.setText(Messages.getString(MessageIds.GDE_MSGT2895));
						this.speedSensorLabel.setVisible(false);
					}
					{
						this.speedSensorButton = new Button(this.optionalStuff, SWT.CHECK | SWT.LEFT);
						RowData speedSensorButtonLData = new RowData();
						speedSensorButtonLData.width = 90;
						speedSensorButtonLData.height = 20;
						this.speedSensorButton.setLayoutData(speedSensorButtonLData);
						this.speedSensorButton.setText(Messages.getString(MessageIds.GDE_MSGT2896));
						this.speedSensorButton.setVisible(false);
						this.speedSensorButton.addMouseMoveListener(new MouseMoveListener() {
							@Override
							public void mouseMove(MouseEvent evt) {
								JLog2Configuration.log.log(Level.FINEST, "speedSensorButton.mouseMove, event=" + evt); //$NON-NLS-1$
								JLog2Configuration.this.mainExplanationText.setText(Messages.getString(MessageIds.GDE_MSGI2823));
							}
						});
						this.speedSensorButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								JLog2Configuration.log.log(Level.FINEST, "speedSensorButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								configuration.setHV2BecVoltage(speedSensorButton.getSelection() ? 1 : 0);
							}
						});
					}
				}
				{
					this.mpxAddessesLabel = new CLabel(this.optionalGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData mpxAddessesLabelLData = new RowData();
					mpxAddessesLabelLData.width = 384;
					mpxAddessesLabelLData.height = 17;
					this.mpxAddessesLabel.setLayoutData(mpxAddessesLabelLData);
					this.mpxAddessesLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.mpxAddessesLabel.setText(Messages.getString(MessageIds.GDE_MSGT2897));

					this.mpxAddresses[0] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGT2898), this.configuration, 0);
					this.mpxAddresses[1] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGT2899), this.configuration, 1);
					this.mpxAddresses[2] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2860), this.configuration, 2);
					this.mpxAddresses[3] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2861), this.configuration, 3);
					this.mpxAddresses[4] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2862), this.configuration, 4);
					this.mpxAddresses[5] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2863), this.configuration, 5);
					this.mpxAddresses[6] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2864), this.configuration, 6);
					this.mpxAddresses[7] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2865), this.configuration, 7);
					this.mpxAddresses[8] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2866), this.configuration, 8);
					this.mpxAddresses[9] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2867), this.configuration, 9);
					this.mpxAddresses[10] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2868), this.configuration, 10);
					this.mpxAddresses[11] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2869), this.configuration, 11);
					this.mpxAddresses[12] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2870), this.configuration, 12);
					this.mpxAddresses[13] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2871), this.configuration, 13);
					this.mpxAddresses[14] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2872), this.configuration, 14);
					this.mpxAddresses[15] = new MpxAddressComposite(this.optionalGroup, SWT.NONE, Messages.getString(MessageIds.GDE_MSGI2873), this.configuration, 15);
				}
			}
			this.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initialyzeGUI(Configuration config, boolean isInitialLoad) {
		if (isInitialLoad) {
			setBaudrate(config.get(0));
			this.sysModeCombo.select(config.get(1) & 0x01);
			setMotorShuntCalibration(config.get(1));
			this.motorPolsCombo.select(config.get(3) > 0 ? (config.get(3) / 2 - 1) : 0);
			setGearRatio(config);

			this.resetButton.setSelection(config.get(11) == 1);
			this.logStopButton.setSelection((config.get(10) & 0x01) == 1);
			this.highPulsWidthButton.setSelection((config.get(10) & 0x02) == 2);
			this.extRpmButton.setSelection((config.get(10) & 0x04) == 4);

			this.uBecDipDetectButton.setSelection((config.get(16) & 0x01) == 1);
			this.capacityAlarmCombo.select(config.get(12));
			this.voltageBatteryAlarmCombo.select(config.get(13));
			this.voltageBatteryAlarmDecimalsCombo.select(config.get(14));
			this.fetTempMaxCombo.select(config.get(15));
		}
		this.extern1Combo.select(config.get(17));
		this.extern2Combo.select(config.get(18));
		this.extern3Combo.select(config.get(19));
		this.extern4Combo.select(config.get(20));
		this.extern5Combo.select(config.get(21));
		this.ext1smallerButton.setSelection((config.get(22) & 0x0001) != 0);
		this.ext2smallerButton.setSelection((config.get(22) & 0x0002) != 0);
		this.ext3smallerButton.setSelection((config.get(22) & 0x0004) != 0);
		this.ext4smallerButton.setSelection((config.get(22) & 0x0008) != 0);
		this.ext5smallerButton.setSelection((config.get(22) & 0x0010) != 0);

		this.alarmLinesCombo.select(config.get(23));
		if (config.get(23) > 0) this.alarmLinesLabel.setForeground(this.application.COLOR_RED);
		setLine1SignalType(config.get(46));
		setTemperaturSensorType(config.get(24));
		this.telemetryCombo.select(config.get(26) <= 3 ? config.get(26) + 1 : 0); // 0=none 1=FTDI 2=JETI 3=MPX 4=Unidisplay
		this.pulsPerRevolutionSensorCombo.select(config.get(27));
		if (this.pulsPerRevolutionSensorCombo.getSelectionIndex() > 0) this.extRpmButton.setEnabled(true);
		setPulsPerRevolutionSensor(config.get(28));
		setIsMotorButton((config.get(29) & 0x0004) > 0 && config.get(28) > 0);
		this.extRpmButton.setEnabled((config.get(29) & 0x0004) > 0 && config.get(28) > 0);
		setIsBrushlessMotor((config.get(29) & 0x0080) > 0 && (config.get(29) & 0x0004) > 0 && config.get(28) > 0);
		this.mpxAddresses[0].mpxAddressCombo.select(config.get(30));
		this.mpxAddresses[1].mpxAddressCombo.select(config.get(31));
		this.mpxAddresses[2].mpxAddressCombo.select(config.get(32));
		this.mpxAddresses[3].mpxAddressCombo.select(config.get(33));
		this.mpxAddresses[4].mpxAddressCombo.select(config.get(34));
		this.mpxAddresses[5].mpxAddressCombo.select(config.get(35));
		this.mpxAddresses[6].mpxAddressCombo.select(config.get(36));
		this.mpxAddresses[7].mpxAddressCombo.select(config.get(37));
		this.mpxAddresses[8].mpxAddressCombo.select(config.get(38));
		this.mpxAddresses[9].mpxAddressCombo.select(config.get(39));
		this.mpxAddresses[10].mpxAddressCombo.select(config.get(40));
		this.mpxAddresses[11].mpxAddressCombo.select(config.get(41));
		this.mpxAddresses[12].mpxAddressCombo.select(config.get(42));
		this.mpxAddresses[13].mpxAddressCombo.select(config.get(43));
		this.mpxAddresses[14].mpxAddressCombo.select(config.get(44));
		this.mpxAddresses[15].mpxAddressCombo.select(config.get(45));
		enableMpxAddressSelection(false);
		//setTelemetryLivedata(config.get(46) & 0xFFF0);
		if (isInitialLoad) {
			setLogMode(config.get(2)); //enable/disables lots of combos
		}
		try {
			updateSubDevices(config.get(47));
		}
		catch (Exception e) {
			//ignore, not relevant for this configuration
		}
		checkClearButtonState();
	}

	/**
	 * @param isBrushless
	 */
	private void setIsBrushlessMotor(boolean isBrushless) {
		this.brushLessButton.setSelection(isBrushless);
		if (isBrushless) {
			this.pulsPerRevolutionSensorCombo.setItems(this.motorPols);
			this.pulsPerRevolutionSensorCombo.select(this.motorPolsCombo.getSelectionIndex());
			this.rpmSensorLabel.setText(Messages.getString(MessageIds.GDE_MSGI2874));
			this.rpmSensorLabel.setForeground(this.application.COLOR_RED);
		}
		else {
			int selectionIndex = this.pulsPerRevolutionSensorCombo.getSelectionIndex();
			this.pulsPerRevolutionSensorCombo.setItems(this.zeroTo127);
			if (selectionIndex > 0) this.pulsPerRevolutionSensorCombo.select(1);
			else this.pulsPerRevolutionSensorCombo.select(0);
			this.rpmSensorLabel.setText(Messages.getString(MessageIds.GDE_MSGI2875));
			this.rpmSensorLabel.setForeground(this.application.COLOR_BLACK);
		}
	}

	/**
	 * @param isMotor
	 */
	private void setIsMotorButton(boolean isMotor) {
		this.motorButton.setSelection(isMotor);
		if (isMotor) {
			this.brushLessButton.setEnabled(true);
		}
		else {
			this.brushLessButton.setEnabled(false);
		}
	}

	/**
	 * @param value
	 */
	private void setPulsPerRevolutionSensor(int value) {
		this.pulsPerRevolutionSensorCombo.select(value);
		if (this.pulsPerRevolutionSensorCombo.getSelectionIndex() < 1) {
			this.motorButton.setEnabled(false);
		}
		else {
			this.motorButton.setEnabled(true);
		}
	}

	/**
	 * @param value
	 */
	private void setTemperaturSensorType(int value) {
		this.tempSensorTypeCombo.select(value);
		switch (value) {
		case 1:
			enableExternTempeartureAll(false);
			enableExternTempearture1(true);
			break;
		case 2:
			enableExternTempeartureAll(true);
			this.tempSensorTypeLabel.setForeground(this.application.COLOR_RED);
			break;
		default:
			enableExternTempeartureAll(false);
			this.tempSensorTypeLabel.setForeground(this.application.COLOR_BLACK);
			break;
		}
	}

	/**
	 * @param value
	 */
	private void setLine1SignalType(int value) {
		switch (value & 0xE0) {
		default:
		case 0: //switch
			this.line1signalTypeCombo.select(0);
			break;
		case 32: //flash
			this.line1signalTypeCombo.select(1);
			break;
		case 64: //interval
			this.line1signalTypeCombo.select(2);
			break;
		case 128: //Morse
			this.line1signalTypeCombo.select(3);
			break;
		}
	}

	/**
	 * @param config
	 */
	private void setGearRatio(Configuration config) {
		if (config.get(4) == 1) {
			this.directRatioButton.setSelection(false);
			this.directGearRatioMajorCombo.setEnabled(false);
			this.directGearRatioDecimalsCombo.setEnabled(false);
			this.gearSelectionButton.setSelection(true);
			this.gearPinionWheelCombo.setEnabled(true);
			this.gearMainWheelCombo.setEnabled(true);
			this.gearMainWheelDecimalsCombo.setEnabled(true);
		}
		else {
			this.directRatioButton.setSelection(true);
			this.directGearRatioMajorCombo.setEnabled(true);
			this.directGearRatioDecimalsCombo.setEnabled(true);
			this.gearSelectionButton.setSelection(false);
			this.gearPinionWheelCombo.setEnabled(false);
			this.gearMainWheelCombo.setEnabled(false);
			this.gearMainWheelDecimalsCombo.setEnabled(false);
		}
		this.directGearRatioMajorCombo.select(config.get(5) - 1);
		this.directGearRatioDecimalsCombo.select(config.get(6) > 0 ? config.get(6) : 0);
		this.gearPinionWheelCombo.select(config.get(7) - 8);
		this.gearMainWheelCombo.select(config.get(8) - 8);
		this.gearMainWheelDecimalsCombo.select(config.get(9) > 0 ? config.get(9) : 0);
	}

	/**
	 * @param value
	 */
	private void setMotorShuntCalibration(int value) {
		if ((value & 0x80) == 0)
			this.motorShuntCombo.select((value & 0x7E) / 2 + 60);
		else
			this.motorShuntCombo.select(60 - (value & 0x7E) / 2);
	}

	/**
	 * set log mode combo according to configuration value
	 * @param value
	 */
	private void setLogMode(int value) {
		switch (value & 0x0B) {
		case 0:
		default:
			this.logModeCombo.select(0);
			break;
		case 2:
			this.logModeCombo.select(1);
			enableAll(false);
			break;
		case 8:
			this.logModeCombo.select(2);
			break;
		}
	}

	private void enableAll(boolean isEnabled) {
		this.directRatioButton.setEnabled(isEnabled);
		this.logStopButton.setEnabled(isEnabled);
		this.highPulsWidthButton.setEnabled(isEnabled);
		this.gearSelectionButton.setEnabled(isEnabled);
		this.motorPolsCombo.setEnabled(isEnabled);
		this.directGearRatioMajorCombo.setEnabled(isEnabled);
		this.directGearRatioDecimalsCombo.setEnabled(isEnabled);
		this.alarmsClearButton.setEnabled(isEnabled);
		this.uBecDipDetectButton.setEnabled(isEnabled);
		this.fetTempMaxCombo.setEnabled(isEnabled);
		this.voltageBatteryAlarmDecimalsCombo.setEnabled(isEnabled);
		this.voltageBatteryAlarmCombo.setEnabled(isEnabled);
		this.capacityAlarmCombo.setEnabled(isEnabled);
		if (!isEnabled) {
			this.extRpmButton.setEnabled(false);
			enableExternTempeartureAll(false);
			enableMpxAddressSelection(false);
		}
		this.sensorAdapterButton.setEnabled(isEnabled);
		this.motorButton.setEnabled(isEnabled);
		this.brushLessButton.setEnabled(isEnabled);
		this.alarmLinesCombo.setEnabled(isEnabled);
		this.telemetryCombo.setEnabled(isEnabled);
		this.pulsPerRevolutionSensorCombo.setEnabled(isEnabled);
		this.tempSensorTypeCombo.setEnabled(isEnabled);
		this.subDevicesCombo.setEnabled(isEnabled);
		this.line1signalTypeCombo.setEnabled(isEnabled);

	}

	/**
	 * set baud rate combo according to configuration value
	 * @param value of config[0]
	 */
	private void setBaudrate(int value) {
		switch (value) {
		case 2400:
			this.baudrateCombo.select(1);
			break;
		case 4800:
			this.baudrateCombo.select(2);
			break;
		case 9600: //JIVE
			this.baudrateCombo.select(0);
			//baudrateCombo.select(3);
			break;
		case 38400: //CMT
			this.baudrateCombo.select(4);
			//baudrateCombo.select(7);
			break;
		case 57600:
			this.baudrateCombo.select(5);
			break;
		case 115200:
			this.baudrateCombo.select(6);
			break;
		}
	}

	private void checkNumberAlarms() {
		int numAlarms = 0;
		boolean isTemperatureSensorType = this.tempSensorTypeCombo.getSelectionIndex() > 0;

//		if (this.uBecDipDetectButton.getSelection()) ++numAlarms;
//		if (this.capacityAlarmCombo.getSelectionIndex() > 0) ++numAlarms;
//		if (this.voltageBatteryAlarmCombo.getSelectionIndex() > 0) ++numAlarms;
//		if (this.voltageBatteryAlarmDecimalsCombo.getSelectionIndex() > 0) ++numAlarms;
//		if (this.fetTempMaxCombo.getSelectionIndex() > 0) ++numAlarms;
		if (this.extern1Combo.getSelectionIndex() > 0) ++numAlarms;
		if (this.ext1smallerButton.getSelection()) ++numAlarms;
		if (this.extern2Combo.getSelectionIndex() > 0) ++numAlarms;
		if (this.ext2smallerButton.getSelection()) ++numAlarms;
		if (this.extern3Combo.getSelectionIndex() > 0) ++numAlarms;
		if (this.ext3smallerButton.getSelection()) ++numAlarms;
		if (this.extern4Combo.getSelectionIndex() > 0) ++numAlarms;
		if (this.ext4smallerButton.getSelection()) ++numAlarms;
		if (this.extern5Combo.getSelectionIndex() > 0) ++numAlarms;
		if (this.ext5smallerButton.getSelection()) ++numAlarms;

		int selectionIndex = this.alarmLinesCombo.getSelectionIndex();
		if (numAlarms == 1) {
			this.alarmLinesCombo.setItems(this.oneAlarms);
			if (selectionIndex > 1) {
				selectionIndex = 1;
				this.configuration.setNumberAlarmLines(1);
			}
			this.alarmLinesCombo.select(selectionIndex);
		}
		else if (isTemperatureSensorType && numAlarms > 1) {
			this.alarmLinesCombo.setItems(this.oneAlarms);
			if (selectionIndex > 1) {
				selectionIndex = 1;
				this.configuration.setNumberAlarmLines(1);
			}
			this.alarmLinesCombo.select(selectionIndex);
		}
		else if (numAlarms > 1) {
			this.alarmLinesCombo.setItems(this.greaterOneAlarms);
			this.alarmLinesCombo.select(2);
		}
		else {
			this.alarmLinesCombo.setItems(this.zeroAlarms);
			this.alarmLinesCombo.select(0);
			this.configuration.setNumberAlarmLines(0);
			this.alarmLinesLabel.setForeground(this.application.COLOR_BLACK);
			this.sensorAdapterButton.setBackground(this.application.COLOR_LIGHT_GREY);
			this.sensorAdapterButton.setEnabled(false);
		}
		checkAdapterRequired();
	}

	private void checkClearButtonState() {
		if (this.uBecDipDetectButton.getSelection() || this.capacityAlarmCombo.getSelectionIndex() > 0 || this.voltageBatteryAlarmCombo.getSelectionIndex() > 0
				|| this.voltageBatteryAlarmDecimalsCombo.getSelectionIndex() > 0 || this.fetTempMaxCombo.getSelectionIndex() > 0 || this.extern1Combo.getSelectionIndex() > 0
				|| this.ext1smallerButton.getSelection() || this.extern2Combo.getSelectionIndex() > 0 || this.ext2smallerButton.getSelection() || this.extern3Combo.getSelectionIndex() > 0
				|| this.ext3smallerButton.getSelection() || this.extern4Combo.getSelectionIndex() > 0 || this.ext4smallerButton.getSelection() || this.extern5Combo.getSelectionIndex() > 0
				|| this.ext5smallerButton.getSelection()) {
			this.alarmsClearButton.setBackground(this.application.COLOR_RED);
			this.alarmsClearButton.setEnabled(true);
		}
		else {
			this.alarmsClearButton.setBackground(this.application.COLOR_LIGHT_GREY);
			this.alarmsClearButton.setEnabled(false);
		}
		checkNumberAlarms();
	}

	private void clearAlarms() {
		this.uBecDipDetectButton.setSelection(false);
		this.configuration.setBecDip(false);
		this.capacityAlarmCombo.select(0);
		this.configuration.setCapacityAlarm(0);
		this.voltageBatteryAlarmDecimalsCombo.select(0);
		this.configuration.setBatteryAlarmMajor("0"); //$NON-NLS-1$
		this.voltageBatteryAlarmCombo.select(0);
		this.configuration.setBatteryAlarmDecimals("0"); //$NON-NLS-1$
		this.fetTempMaxCombo.select(0);
		this.configuration.setPaMaxTempAlarm("0"); //$NON-NLS-1$
		this.extern1Combo.select(0);
		this.ext1smallerButton.setSelection(false);
		this.configuration.setExtTemp1("0"); //$NON-NLS-1$
		this.configuration.setExtTemp1LowerThan(0);
		this.configuration.setExtTemp2("0"); //$NON-NLS-1$
		this.configuration.setExtTemp2LowerThan(0);
		this.configuration.setExtTemp3("0"); //$NON-NLS-1$
		this.configuration.setExtTemp3LowerThan(0);
		this.configuration.setExtTemp4("0"); //$NON-NLS-1$
		this.configuration.setExtTemp4LowerThan(0);
		this.configuration.setExtTemp5("0"); //$NON-NLS-1$
		this.configuration.setExtTemp5LowerThan(0);

		this.alarmLinesLabel.setForeground(this.application.COLOR_BLACK);
		this.alarmLinesCombo.select(0);

		checkClearButtonState();
	}

	private void enableExternTempearture1(boolean isEnabled) {
		this.extern1Combo.setEnabled(isEnabled);
		this.ext1smallerButton.setEnabled(isEnabled);
		this.configuration.setExtTemp1("0"); //$NON-NLS-1$
		this.configuration.setExtTemp1LowerThan(0);
	}

	private void enableExternTempeartureAll(boolean isEnabled) {
		this.extern1Combo.setEnabled(isEnabled);
		this.ext1smallerButton.setEnabled(isEnabled);
		this.extern2Combo.setEnabled(isEnabled);
		this.ext2smallerButton.setEnabled(isEnabled);
		this.extern3Combo.setEnabled(isEnabled);
		this.ext3smallerButton.setEnabled(isEnabled);
		this.extern4Combo.setEnabled(isEnabled);
		this.ext4smallerButton.setEnabled(isEnabled);
		this.extern5Combo.setEnabled(isEnabled);
		this.ext5smallerButton.setEnabled(isEnabled);
		this.configuration.setExtTemp2("0"); //$NON-NLS-1$
		this.configuration.setExtTemp2LowerThan(0);
		this.configuration.setExtTemp3("0"); //$NON-NLS-1$
		this.configuration.setExtTemp3LowerThan(0);
		this.configuration.setExtTemp4("0"); //$NON-NLS-1$
		this.configuration.setExtTemp4LowerThan(0);
		this.configuration.setExtTemp5("0"); //$NON-NLS-1$
		this.configuration.setExtTemp5LowerThan(0);
	}

	private void updateSubDevices(int value) {
		this.subDevicesLabel.setForeground(this.application.COLOR_BLACK);
		this.subDevicesCombo.setText(Messages.getString(MessageIds.GDE_MSGT2865));
		this.sensorAdapterButton.setEnabled(false);
		this.sensorAdapterButton.setBackground(this.application.COLOR_LIGHT_GREY);
		this.telemetryCombo.setEnabled(true);
		this.alarmLinesCombo.setEnabled(true);
		this.line1signalTypeCombo.setEnabled(true);
		this.alarmLinesLabel.setForeground(this.application.COLOR_BLACK);
		this.tempSensorTypeCombo.setEnabled(true);
		this.pulsPerRevolutionSensorCombo.setEnabled(true);
		this.ext1Label.setText(Messages.getString(MessageIds.GDE_MSGT2874));
		this.temperaure1Label.setText("[°C]"); //$NON-NLS-1$
		this.alarmLinesCombo.setItems(this.zeroAlarms);
		this.alarmLinesCombo.select(0);
		this.hv2BecLabel.setVisible(false);
		this.hv2BecCombo.setVisible(false);
		this.speedSensorLabel.setVisible(false);
		this.speedSensorButton.setVisible(false);
		if (this.configuration.version == 322) {
			if (this.comOutput.lastIndexOf(this.hottOutput) > 0)
				this.comOutput.substring(0, this.comOutput.lastIndexOf(this.hottOutput));
			this.telemetryCombo.setItems(this.comOutputString.split(GDE.STRING_COMMA));

			//0=normal, 1=HSS, 2=HSSG2, 3=HSST, 4=BH, 5=BM, 6=BHSS, 7=BHSST, 8=G, 9=S 10=L,
			//11=T, 12=GT, 13=V, 14=VG, 15=VS, 16=BID, 17=BIDG, 18=BIDS, 19=A, 20=AV, 21=B, 22=BV, 23=P1, 24=P2, 25=AVP1, 26=AP2
			switch (value) {
			case 1: //V (V4T0)
				this.jlogConfigurationCombo.select(13);
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				enableExternTempearture1(true);
				this.ext1Label.setText(Messages.getString(MessageIds.GDE_MSGI2877));
				this.temperaure1Label.setText("[V]"); //$NON-NLS-1$
				break;
			case 2: //BID
				this.jlogConfigurationCombo.select(16);
				this.subDevicesLabel.setForeground(this.application.COLOR_RED);
				this.subDevicesCombo.setText("BID"); //$NON-NLS-1$
				this.alarmLinesCombo.setEnabled(false);
				this.line1signalTypeCombo.setEnabled(false);
				this.pulsPerRevolutionSensorCombo.setEnabled(false);
				this.tempSensorTypeCombo.setEnabled(false);
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				break;
			case 92: //P2 (Phase puls K4-2)
				this.jlogConfigurationCombo.select(24);
				this.subDevicesLabel.setForeground(this.application.COLOR_RED);
				this.subDevicesCombo.setText("BID"); //$NON-NLS-1$
				this.alarmLinesCombo.setEnabled(false);
				this.line1signalTypeCombo.setEnabled(false);
				this.pulsPerRevolutionSensorCombo.setEnabled(false);
				this.tempSensorTypeCombo.setEnabled(false);
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				break;
			case 3: //B (JTX base)
				this.jlogConfigurationCombo.select(21);
				this.alarmLinesCombo.setItems(this.greaterOneAlarms);
				this.alarmLinesCombo.select(2);
				this.alarmLinesCombo.setEnabled(false);
				this.alarmLinesLabel.setForeground(this.application.COLOR_RED);
				this.line1signalTypeCombo.setEnabled(false);
				this.tempSensorTypeCombo.setEnabled(false);
				this.pulsPerRevolutionSensorCombo.setEnabled(false);
				this.telemetryCombo.select(4);
				enableExternTempeartureAll(true);
				break;
			case 4: //BV (JTX base V4T0)
				this.jlogConfigurationCombo.select(22);
				this.alarmLinesCombo.setItems(this.greaterOneAlarms);
				this.alarmLinesCombo.select(2);
				this.alarmLinesCombo.setEnabled(false);
				this.alarmLinesLabel.setForeground(this.application.COLOR_RED);
				this.line1signalTypeCombo.setEnabled(false);
				this.tempSensorTypeCombo.setEnabled(false);
				this.pulsPerRevolutionSensorCombo.setEnabled(false);
				this.telemetryCombo.select(4);
				enableExternTempeartureAll(true);
				this.ext1Label.setText(Messages.getString(MessageIds.GDE_MSGI2877));
				this.temperaure1Label.setText("[V]"); //$NON-NLS-1$
				break;
			case 5: //A (JTX Air)
				this.jlogConfigurationCombo.select(19);
				this.telemetryCombo.setEnabled(false);
				this.telemetryCombo.setText("JTX"); //$NON-NLS-1$
				break;
			case 6: //AV (JTX Air V4T0)
				this.jlogConfigurationCombo.select(20);
				this.telemetryCombo.setEnabled(false);
				this.telemetryCombo.setText("JTX"); //$NON-NLS-1$
				enableExternTempearture1(true);
				this.ext1Label.setText(Messages.getString(MessageIds.GDE_MSGI2877));
				this.temperaure1Label.setText("[V]"); //$NON-NLS-1$
				break;
			case 10: //G (GPS single)
				this.jlogConfigurationCombo.select(8);
				this.subDevicesCombo.setText("none"); //$NON-NLS-1$
				this.telemetryCombo.setEnabled(false);
				this.telemetryCombo.setText("MBS"); //$NON-NLS-1$
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				break;
			case 11: //HoTT
				//this.jlogConfigurationCombo.select(10); // no special configuration or firmaware required
				this.subDevicesCombo.setText("none"); //$NON-NLS-1$
				this.telemetryCombo.setItems(this.comOutput.append(this.hottOutput).toString().split(GDE.STRING_COMMA));
				this.telemetryCombo.select(5);
				break;
			case 15: //S (GPS mpx)
				this.jlogConfigurationCombo.select(9);
				this.subDevicesCombo.setText("none"); //$NON-NLS-1$
				this.telemetryCombo.setEnabled(false);
				this.telemetryCombo.setText("MPX"); //$NON-NLS-1$
				enableMpxAddressSelection(true);
				break;
			case 16: //VS (V4T0 GPS mpx)
				this.jlogConfigurationCombo.select(15);
				this.subDevicesCombo.setText("none"); //$NON-NLS-1$
				this.telemetryCombo.setEnabled(false);
				this.telemetryCombo.setText("MPX"); //$NON-NLS-1$
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				enableExternTempearture1(true);
				this.ext1Label.setText(Messages.getString(MessageIds.GDE_MSGI2877));
				this.temperaure1Label.setText("[V]"); //$NON-NLS-1$
				break;
			case 20: //BIDG (GPS single)
				this.jlogConfigurationCombo.select(17);
				this.subDevicesLabel.setForeground(this.application.COLOR_RED);
				this.subDevicesCombo.setText("BID"); //$NON-NLS-1$
				this.telemetryCombo.setEnabled(false);
				this.telemetryCombo.setText("MBS"); //$NON-NLS-1$
				this.alarmLinesCombo.setEnabled(false);
				this.line1signalTypeCombo.setEnabled(false);
				this.pulsPerRevolutionSensorCombo.setEnabled(false);
				this.tempSensorTypeCombo.setEnabled(false);
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				break;
			case 23: //VG (V4T0 GPS single)
				this.jlogConfigurationCombo.select(14);
				this.subDevicesCombo.setText("none"); //$NON-NLS-1$
				this.telemetryCombo.setEnabled(false);
				this.telemetryCombo.setText("MBS"); //$NON-NLS-1$
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				enableExternTempearture1(true);
				this.ext1Label.setText(Messages.getString(MessageIds.GDE_MSGI2877));
				this.temperaure1Label.setText("[V]"); //$NON-NLS-1$
				break;
			case 25: //BIDS (GPS mpx)
				this.jlogConfigurationCombo.select(18);
				this.subDevicesLabel.setForeground(this.application.COLOR_RED);
				this.subDevicesCombo.setText("BID"); //$NON-NLS-1$
				this.telemetryCombo.setEnabled(false);
				this.telemetryCombo.setText("MPX"); //$NON-NLS-1$
				this.alarmLinesCombo.setEnabled(false);
				this.line1signalTypeCombo.setEnabled(false);
				this.pulsPerRevolutionSensorCombo.setEnabled(false);
				this.tempSensorTypeCombo.setEnabled(false);
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				enableMpxAddressSelection(true);
				break;
			case 40: //L (GPS mpx LQI)
				this.jlogConfigurationCombo.select(10);
				this.subDevicesCombo.setText("none"); //$NON-NLS-1$
				this.telemetryCombo.setEnabled(false);
				this.telemetryCombo.setText("MPX"); //$NON-NLS-1$
				break;
			case 91: //P1 (Phase puls K4-1)
				this.jlogConfigurationCombo.select(23);
				this.subDevicesLabel.setForeground(this.application.COLOR_RED);
				break;
			case 110: //T (Prandtl probe)
				this.jlogConfigurationCombo.select(11);
				this.subDevicesCombo.setText("none"); //$NON-NLS-1$
				this.speedSensorLabel.setVisible(true);
				this.speedSensorButton.setVisible(true);
				break;
			case 111: //GT (Prandtl probe + GPS)
				this.jlogConfigurationCombo.select(12);
				this.subDevicesCombo.setText("none"); //$NON-NLS-1$
				this.telemetryCombo.setEnabled(false);
				this.telemetryCombo.setText("MBS"); //$NON-NLS-1$
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				this.speedSensorLabel.setVisible(true);
				this.speedSensorButton.setVisible(true);
				break;
			case 200: //HTS-SS (HiTec telemetry)
				this.jlogConfigurationCombo.select(1);
				this.subDevicesLabel.setForeground(this.application.COLOR_RED);
				this.subDevicesCombo.setText("HTS-SS"); //$NON-NLS-1$
				this.alarmLinesCombo.setEnabled(false);
				this.line1signalTypeCombo.setEnabled(false);
				this.pulsPerRevolutionSensorCombo.setEnabled(false);
				this.tempSensorTypeCombo.setEnabled(false);
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				break;
			case 201: //HTS-SS + GPS-Logger (no HiTec telemetry)
				this.jlogConfigurationCombo.select(2);
				this.subDevicesLabel.setForeground(this.application.COLOR_RED);
				this.subDevicesCombo.setText("HTS-SS"); //$NON-NLS-1$
				this.alarmLinesCombo.setEnabled(false);
				this.line1signalTypeCombo.setEnabled(false);
				this.pulsPerRevolutionSensorCombo.setEnabled(false);
				this.tempSensorTypeCombo.setEnabled(false);
				this.telemetryCombo.setEnabled(false);
				this.telemetryCombo.setText("MBS"); //$NON-NLS-1$
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				break;
			case 202: //HTS-SS + temperature sensor (no HiTec telemetry)
				this.jlogConfigurationCombo.select(3);
				this.subDevicesLabel.setForeground(this.application.COLOR_RED);
				this.subDevicesCombo.setText("HTS-SS"); //$NON-NLS-1$
				this.alarmLinesCombo.setEnabled(false);
				this.line1signalTypeCombo.setEnabled(false);
				this.pulsPerRevolutionSensorCombo.setEnabled(false);
				this.tempSensorTypeCombo.select(0);
				this.tempSensorTypeCombo.setEnabled(false);
				this.telemetryCombo.setEnabled(false);
				this.telemetryCombo.setText("digital COM"); //$NON-NLS-1$
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				break;
			case 210: //BH (HV²BEC no MPX telemetry)
				this.jlogConfigurationCombo.select(4);
				this.subDevicesLabel.setForeground(this.application.COLOR_RED);
				this.subDevicesCombo.setText("HV²BEC"); //$NON-NLS-1$
				this.ext1Label.setText("HV²BEC Temp"); //$NON-NLS-1$
				this.alarmLinesCombo.setEnabled(false);
				this.line1signalTypeCombo.setEnabled(false);
				this.pulsPerRevolutionSensorCombo.setEnabled(false);
				this.tempSensorTypeCombo.select(0);
				this.tempSensorTypeCombo.setEnabled(false);
				enableExternTempearture1(true);
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				this.hv2BecLabel.setVisible(true);
				this.hv2BecCombo.setVisible(true);
				break;
			case 211: //BM (HV²BEC no HoTT telemetry)
				this.jlogConfigurationCombo.select(5);
				this.subDevicesLabel.setForeground(this.application.COLOR_RED);
				this.subDevicesCombo.setText("HV²BEC"); //$NON-NLS-1$
				this.ext1Label.setText("HV²BEC Temp"); //$NON-NLS-1$
				this.alarmLinesCombo.setEnabled(false);
				this.line1signalTypeCombo.setEnabled(false);
				this.pulsPerRevolutionSensorCombo.setEnabled(false);
				this.tempSensorTypeCombo.select(0);
				this.tempSensorTypeCombo.setEnabled(false);
				enableExternTempearture1(true);
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				this.hv2BecLabel.setVisible(true);
				this.hv2BecCombo.setVisible(true);
				break;
			case 212: //HV²BEC/HTSS (HiTec telemetry)
				this.jlogConfigurationCombo.select(6);
				this.subDevicesLabel.setForeground(this.application.COLOR_RED);
				this.subDevicesCombo.setText("HV²BEC/HTSS"); //$NON-NLS-1$
				this.ext1Label.setText("HV²BEC Temp"); //$NON-NLS-1$
				this.alarmLinesCombo.setEnabled(false);
				this.line1signalTypeCombo.setEnabled(false);
				this.pulsPerRevolutionSensorCombo.setEnabled(false);
				this.tempSensorTypeCombo.select(0);
				this.tempSensorTypeCombo.setEnabled(false);
				enableExternTempearture1(true);
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				this.hv2BecLabel.setVisible(true);
				this.hv2BecCombo.setVisible(true);
				break;
			case 213: //BHSST (HV²BEC + HiTec telemetry + digital temperature sensors at COM)
				this.jlogConfigurationCombo.select(7);
				this.subDevicesLabel.setForeground(this.application.COLOR_RED);
				this.subDevicesCombo.setText("HV²BEC/HTSS"); //$NON-NLS-1$
				this.ext1Label.setText("HV²BEC Temp"); //$NON-NLS-1$
				this.alarmLinesCombo.setEnabled(false);
				this.line1signalTypeCombo.setEnabled(false);
				this.pulsPerRevolutionSensorCombo.setEnabled(false);
				this.telemetryCombo.setEnabled(false);
				this.telemetryCombo.setText("digital COM"); //$NON-NLS-1$
				this.tempSensorTypeCombo.select(0);
				this.tempSensorTypeCombo.setEnabled(false);
				this.enableExternTempearture1(true);
				this.sensorAdapterButton.setEnabled(true);
				this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
				this.hv2BecLabel.setVisible(true);
				this.hv2BecCombo.setVisible(true);
				break;
			}
		}
		else { //version 4.0.0
			switch (value) {
			default:
			case 60:
				this.jlogConfigurationCombo.select(1);
				this.telemetryCombo.setItems(this.comOutputBase);
				this.telemetryCombo.select(0);
				break;
			case 61: //Jive Jeti EX
			case 71: //Castle Jeti EX
				this.jlogConfigurationCombo.select(value == 61 ? 2 : 7);
				this.telemetryCombo.setItems(this.comOutputJetiEx);
				this.telemetryCombo.select(2);
				break;
			case 62: //Jive HoTT v4
			case 72: //Castle HoTT v4
			case 81: //C200 HoTT v4
				this.jlogConfigurationCombo.select(value == 62 ? 3 : value == 72 ? 8 : 12);
				this.telemetryCombo.setItems(this.comOutputHoTTv4);
				this.telemetryCombo.select(3);
				break;
			case 63: //Jive Futaba S.BUS2
			case 73: //Castle Futaba S.BUS2
			case 82: //C200 Futaba S.BUS2
				this.jlogConfigurationCombo.select(value == 63 ? 4 : value == 73 ? 9 : 13);
				this.telemetryCombo.setItems(this.comOutputFutabaSbus2);
				this.telemetryCombo.select(4);
				break;
			case 64: //Jive HiTec
			case 74: //Castle HiTec
				this.jlogConfigurationCombo.select(value == 64 ? 5 : 10);
				this.telemetryCombo.setItems(this.comOutputHiTec);
				this.telemetryCombo.select(0);
				break;
			case 65: //Jive Spektrum
			case 75: //Castle Spektrum
				this.jlogConfigurationCombo.select(value == 65 ? 6 : 11);
				this.telemetryCombo.setItems(this.comOutputSpektrum);
				this.telemetryCombo.select(0);
				break;
			}

		}
		JLog2Configuration.log.log(Level.FINE, "telemetryCombo selection index = " + this.telemetryCombo.getSelectionIndex() + " jlogConfigurationCombo selection index = " + this.jlogConfigurationCombo.getSelectionIndex()); //$NON-NLS-1$ //$NON-NLS-2$
		JLog2Configuration.log.log(Level.FINER, StringHelper.printBinary((byte) (value & 0xFF), false));
	}

	private void enableMpxAddressSelection(boolean isEnabled) {
		for (MpxAddressComposite tmpMpxAddress : this.mpxAddresses) {
			tmpMpxAddress.mpxAddressCombo.setEnabled(isEnabled);
		}
	}

	/**
	 *
	 */
	private void updateGearRatio() {
		double gearRatio = Double.valueOf(this.gearMainWheelCombo.getText().trim() + GDE.STRING_DOT + this.gearMainWheelDecimalsCombo.getText().trim())
				/ Double.valueOf(this.gearPinionWheelCombo.getText().trim());
		this.directGearRatioMajorCombo.select((int) gearRatio - 1);
		this.directGearRatioDecimalsCombo.select((int) (gearRatio % 1 * 100));
		this.configuration.setGearRatioMinor(GDE.STRING_EMPTY + (int) gearRatio);
		this.configuration.setGearRatioDecimals(GDE.STRING_EMPTY + (int) (gearRatio % 1 * 100));
	}

	/**
	 *
	 */
	private void checkAdapterRequired() {
		if (this.alarmLinesCombo.getSelectionIndex() > 1 || this.tempSensorTypeCombo.getSelectionIndex() == 2
				|| (this.tempSensorTypeCombo.getSelectionIndex() >= 1 && this.pulsPerRevolutionSensorCombo.getSelectionIndex() > 0)) {
			this.sensorAdapterButton.setEnabled(true);
			this.sensorAdapterButton.setBackground(this.application.COLOR_RED);
		}
		else {
			this.sensorAdapterButton.setEnabled(false);
			this.sensorAdapterButton.setBackground(this.application.COLOR_LIGHT_GREY);
		}
	}
}
