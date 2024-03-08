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
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.GDE;
import gde.device.smmodellbau.gpslogger.MessageIds;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.Checksum;
import gde.utils.StringHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * class implementation to read/write SM GPS-Logger configuration in binary format (firmware 1.03)
 */
public class SetupReaderWriter {
	final static Logger	log													= Logger.getLogger(SetupReaderWriter.class.getName());
	final DataExplorer	application									= DataExplorer.getInstance();
	final GPSLogger			device;
	final Shell					parent;

	final static int		TEL_ALARM_HEIGHT						= 0x0001;
	final static int		TEL_ALARM_SPEED_MAX					= 0x0002;
	final static int		TEL_ALARM_DISTANCE_MAX			= 0x0004;
	final static int		TEL_ALARM_TRIP_LENGTH				= 0x0008;
	final static int		TEL_ALARM_VOLTAGE_RX				= 0x0010;

	final static int		TEL_ALARM_CURRENT_UL				= 0x0020;
	final static int		TEL_ALARM_VOLTAGE_START_UL	= 0x0040;
	final static int		TEL_ALARM_VOLTAGE_UL				= 0x0080;
	final static int		TEL_ALARM_CAPACITY_UL				= 0x0100;
	final static int		TEL_ALARM_DISTANCE_MIN			= 0x0200;
	final static int		TEL_ALARM_SPEED_MIN					= 0x0400;

	//$SETUP,192 Byte*
	int									serialNumber								= 357;																									// 1
	short								datarate										= 0;																										// 2 0 = 10Hz, 1 = 5Hz, 2 = 2Hz, 3 = 1Hz
	short								startModus									= 1;																										// 3 0 = „manuell“, 1 = „3D-Fix“, 2 = „ >20 m“, 3 = „>20 km/h“
	short								timeZone										= 2;																										// 4 -12 --> +12 step 1
	short								units												= 0;																										// 5
	short								varioThreshold							= 3;																										// 6  0 --> 50 step 1
	short								varioTon										= 0;																										// 7
	short								stopModus										= 0;																										// 8 0=OFF; 1=no motion
	short								modusDistance								= 0;																										// 9 0=3D; 1=2D
	short								varioThresholdSink					= 8;																										// 10 0 --> 50 step 1
	short								daylightSavingModus					= 1;																										// 11 0=manual; 1=auto
	short								telemetryType								= 0;																										// 12 0=invalid, 1=Futaba, 2=JR DMSS, 3=HoTT, 4=JetiDuplex, 5=M-Link, 6=FrSky, 7=Spektrum, 8=P2Bus
	short								rxControl										= 0;																										// 13 0=OFF, 1=Min/Live/Max 2=StartPoint
	int									jetiExMask									= 0xFFFFFFFF;																						// 14,15 bit0=undefined, bit1=time bit*=refer to converter
	short								varioFactor									= 0;																										// 16 1 + factor/10
	short								frskyAddr 									= 0; 																										// 17 0x00 -> 0x1B
	short								telemetryAlarms							= 0x0013;																								// 18 
	short								heightAlarm									= 200;																									// 19 10m --> 4000m step 50
	short								speedMaxAlarm								= 200;																									// 20 10km/h --> 1000km/h
	short								distanceMaxAlarm						= 500;																									// 21 10m --> 5000m
	short								voltageRxAlarm							= 450;																									// 22 300 --> 800 V/100
	short								tripLengthAlarm							= 50;																										// 23 10km/10 --> 999km/10
	short								currentUlAlarm							= 100;																									// 24 1A --> 400A
	short								voltageStartUlAlarm					= 124;																									// 25 10V/10 --> 600V/10
	short								voltageUlAlarm							= 100;																									// 26 10V/10 --> 600V/10
	short								capacityUlAlarm							= 2000;																									// 27 100mAh --> 30000mAh
	short								distanceMinAlarm						= 0;																										// 28 0 - 500
	byte								serialNumberFix							= 0;																										// 29.. fixe_Seriennummer;
	byte								robbe_T_Box									= 0;																										// ..29 Robbe_T_Box
	short								varioFilter									= 0;																										// 30 Vario filter
	short								speedMinAlarm								= 30;																										// 31 10km/h --> 1000km/h
	byte								language										= 0;																										// 32..
	byte 								hottSpeedType								= 0;																										// ..32 //0=auto, 1=GPS-Speed 2=AirSpeed
	short								jetiExMask_UL								= 0;																										// 33
	//short[] B = new short[4]; // 34-37
	byte								mLinkAddressVarioTec				= 0;																										// 38.. 0 - 15, "--"
	byte								mLinkAddressVoltageRx				= 16;																										// ..38 0 - 15, "--"
	byte								mLinkAddressSpeed						= 1;																										// 39..
	byte								mLinkAddressVario						= 0;																										// ..39
	byte								mLinkAddressDirection				= 3;																										// 40..
	byte								mLinkAddressAirSpeed				= 0;																										// ..40
	byte								mLinkAddressAltitude				= 4;																										// 41..
	byte								mLinkAddressFree41					= 0;																										// ..41
	byte								mLinkAddressDistance				= 7;																										// 42..
	byte								mLinkAddressFree42					= 0;																										// ..42
	byte								mLinkAddressTripLength			= 8;																										// 43..
	byte								mLinkAddressFree43					= 0;																										// ..43
	byte								mLinkAddressSpeedMax				= 2;																										// 44..
	byte								mLinkAddressFree44					= 0;																										// ..44
	byte								mLinkAddressHeightMax				= 5;																										// 45..
	byte								mLinkAddressFree45					= 0;																										// ..45
	byte								mLinkAddressENL							= 9;																										// 46..
	byte								mLinkAddressFree46					= 0;																										// ..46
	byte								mLinkAddressAccX						= 10;																										// 47..
	byte								mLinkAddressFree47					= 0;																										// ..47
	byte								mLinkAddressAccY						= 11;																										// 48..
	byte								mLinkAddressFree48					= 0;																										// ..48
	byte								mLinkAddressAccZ						= 12;																										// 49..
	byte								mLinkAddressFree49					= 0;																										// ..49
	byte								mLinkAddressFlightDirection	= 13;																										// 50..
	byte								mLinkAddressFree50					= 0;																										// ..50
	byte								mLinkAddressDirectionRel		= 14;																										// 51..
	byte								smoothAltitudeNulling				= 0;																										// ..51
	byte								mLinkAddressAltitudeGain			= 6;																										// 52..
	byte								powerBoxP2BusAdresse				= (byte) 188;																						// ..52
	//short[] C = short int[1]; 																																						// 53
	short								firmwareVersion							= 126;																									// 54
	short								modusIGC										= 1;																										// 55
	byte 								startSlotSBUS[] 						= new byte[8];																					// 56-59
	byte								spektrumSensors							= 0;																										// 60..
	byte								spektrumNumber							= 0;																										// ..60
	int									fixPositionLatitude					= 0;																										// 61,62
	int									fixPositionLongitude				= 0;																										// 63,64
	short 							fixPositionAltitude					= 0;																										// 65
	//short[]						unused											= new short[192 - 68 * 2];
	short 							betaVersion									= 0;																										// 94
	short								hardwareVersion							= 0;																										// 95
	short								checkSum;																																						// 96

	short[]							setupData										= new short[192];

	public SetupReaderWriter(Shell useParent, GPSLogger useDevice) {
		this.parent = useParent;
		this.device = useDevice;
	}

	void loadSetup() {
		FileDialog fd = this.application.openFileOpenDialog(this.parent, Messages.getString(MessageIds.GDE_MSGT2001), new String[] { GDE.FILE_ENDING_STAR_INI, GDE.FILE_ENDING_STAR },
				this.device.getConfigurationFileDirecotry(), this.device.getDefaultConfigurationFileName(), SWT.SINGLE);
		GPSLogger.selectedSetupFilePath = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + fd.getFileName();
		log.log(Level.FINE, "selectedSetupFile = " + GPSLogger.selectedSetupFilePath); //$NON-NLS-1$

		if (fd.getFileName().length() > 4) {
			try {
				FileInputStream file_input = new FileInputStream(new File(GPSLogger.selectedSetupFilePath));
				DataInputStream data_in = new DataInputStream(file_input);
				byte[] buffer = new byte[192];
				int size = data_in.read(buffer);
				data_in.close();

				if (size != 192) {
					log.log(Level.SEVERE, "error reading configuration file, data size != 192 Bytes!"); //$NON-NLS-1$
				}
				this.serialNumber 					= DataParser.parse2UnsignedShort(buffer, 0);
				this.datarate 							= DataParser.parse2Short(buffer, 2);
				this.startModus 						= DataParser.parse2Short(buffer, 4);
				this.timeZone 							= DataParser.parse2Short(buffer, 6);
				//units
				this.varioThreshold 				= DataParser.parse2Short(buffer, 10);
				this.varioTon 							= DataParser.parse2Short(buffer, 12);
				this.stopModus 							= DataParser.parse2Short(buffer, 14);
				this.modusDistance					= DataParser.parse2Short(buffer, 16);
				this.varioThresholdSink			= DataParser.parse2Short(buffer, 18);
				this.daylightSavingModus		= DataParser.parse2Short(buffer, 20);
				this.telemetryType					= DataParser.parse2Short(buffer, 22);
				this.rxControl							= DataParser.parse2Short(buffer, 24);
				this.jetiExMask							= DataParser.parse2Int(buffer, 26);
				this.varioFactor 						= DataParser.parse2Short(buffer, 30);
				this.frskyAddr							= DataParser.parse2Short(buffer, 32);
				this.telemetryAlarms 				= DataParser.parse2Short(buffer, 34);
				this.heightAlarm 						= DataParser.parse2Short(buffer, 36);
				this.speedMaxAlarm 					= DataParser.parse2Short(buffer, 38);
				this.distanceMaxAlarm 			= DataParser.parse2Short(buffer, 40);
				this.voltageRxAlarm 				= DataParser.parse2Short(buffer, 42);
				this.tripLengthAlarm 				= DataParser.parse2Short(buffer, 44);
				this.currentUlAlarm 				= DataParser.parse2Short(buffer, 46);
				this.voltageStartUlAlarm 		= DataParser.parse2Short(buffer, 48);
				this.voltageUlAlarm 				= DataParser.parse2Short(buffer, 50);
				this.capacityUlAlarm 				= DataParser.parse2Short(buffer, 52);
				this.distanceMinAlarm				= DataParser.parse2Short(buffer, 54);
				this.serialNumberFix 				= buffer[56]; 												// 29.. fixe_Seriennummer;
				this.robbe_T_Box 						= buffer[57]; 												// ..29 Robbe_T_Box;
				this.varioFilter 						= DataParser.parse2Short(buffer, 58); // 30 Vario filter
				this.speedMinAlarm					= DataParser.parse2Short(buffer, 60);	// 31 10km/h --> 1000km/h
				this.language								= buffer[62];													// 32.. 0= german, 1=english
				this.hottSpeedType					= buffer[63];													// ..32 speed type 0=auto, 1=GPS, 2=AirSpeed
				this.jetiExMask_UL					= DataParser.parse2Short(buffer, 64);	// 33
				//B[4]
				this.mLinkAddressVarioTec			= buffer[74];		// 38..
				this.mLinkAddressVoltageRx		= buffer[75];		// ..38
				this.mLinkAddressSpeed 				= buffer[76];		// 39..
				this.mLinkAddressVario   			= buffer[77];		// ..39
				this.mLinkAddressDirection 		= buffer[78];		// 40..
				this.mLinkAddressAirSpeed 		= buffer[79];		// ..40
				this.mLinkAddressAltitude 		= buffer[80];		// 41..
				this.mLinkAddressDistance 		= buffer[82];		// 42..
				this.mLinkAddressTripLength 	= buffer[84];		// 43..
				this.mLinkAddressSpeedMax			= buffer[86];		// 44..
				this.mLinkAddressHeightMax		= buffer[88];		// 45..
				this.mLinkAddressENL					= buffer[90];		// 46..
				this.mLinkAddressAccX					= buffer[92];		// 47..
				this.mLinkAddressAccY					= buffer[94];		// 48..
				this.mLinkAddressAccZ					= buffer[96];		// 49..		
				this.mLinkAddressFlightDirection	= buffer[98];// 50..
				this.mLinkAddressDirectionRel	= buffer[100];	// 51..
				this.smoothAltitudeNulling		= buffer[101];	// ..51
				this.mLinkAddressAltitudeGain	= buffer[102];	// 52..
				this.powerBoxP2BusAdresse			= buffer[103];	// ..52
				//short[] C 									= short int[1]; // 53..
				this.firmwareVersion 					= DataParser.parse2Short(buffer, 106);	// 54
				this.modusIGC 								= DataParser.parse2Short(buffer, 108);	// 55
				System.arraycopy(buffer, 110, this.startSlotSBUS, 0, 8);
				this.spektrumSensors 					= buffer[118];	// 60..
				this.spektrumNumber 					= buffer[119];	// ..60
				this.fixPositionLatitude			= DataParser.parse2Int(buffer, 120);		// 61,62
				this.fixPositionLongitude			= DataParser.parse2Int(buffer, 124);		// 63,64
				this.fixPositionAltitude			= DataParser.parse2Short(buffer, 128);	// 65
				//unused
				this.betaVersion 							= DataParser.parse2Short(buffer, 186);	// 94
				this.hardwareVersion 					= DataParser.parse2Short(buffer, 188);	// 95
				this.checkSum = (short) (((buffer[191] & 0x00FF) << 8) + (buffer[190] & 0x00FF));// 96

				byte[] chkBuffer = new byte[192 - 2];
				System.arraycopy(buffer, 0, chkBuffer, 0, chkBuffer.length);
				short checkCRC = Checksum.CRC16(chkBuffer, 0);
				if (this.checkSum != checkCRC) {
					log.log(Level.WARNING, "Checksum missmatch!"); //$NON-NLS-1$
				}
			}
			catch (Throwable e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	void saveSetup() {
		FileDialog fileDialog = this.application.prepareFileSaveDialog(this.parent, Messages.getString(MessageIds.GDE_MSGT2002), new String[] { GDE.FILE_ENDING_STAR_INI, GDE.FILE_ENDING_STAR },
				this.device.getConfigurationFileDirecotry(), this.device.getDefaultConfigurationFileName());
		log.log(Level.FINE, "selectedSetupFile = " + fileDialog.getFileName()); //$NON-NLS-1$
		String setupFilePath = fileDialog.open();
		
		if (setupFilePath != null && setupFilePath.length() > 4) {
			File setupFile = new File(setupFilePath);
			byte[] buffer = new byte[192];
			int tmpCheckSum = 0;

			try {
				buffer[0] = (byte) (this.serialNumber & 0x00FF);
				buffer[1] = (byte) ((this.serialNumber & 0xFF00) >> 8);
				buffer[2] = (byte) (this.datarate & 0x00FF);
				buffer[3] = (byte) ((this.datarate & 0xFF00) >> 8);
				buffer[4] = (byte) (this.startModus & 0x00FF);
				buffer[5] = (byte) ((this.startModus & 0xFF00) >> 8);
				buffer[6] = (byte) (this.timeZone & 0x00FF);
				buffer[7] = (byte) ((this.timeZone & 0xFF00) >> 8);
				//units
				buffer[10] = (byte) (this.varioThreshold & 0x00FF);
				buffer[11] = (byte) ((this.varioThreshold & 0xFF00) >> 8);
				buffer[12] = (byte) (this.varioTon & 0x00FF);
				buffer[13] = (byte) ((this.varioTon & 0xFF00) >> 8);
				buffer[14] = (byte) (this.stopModus & 0x00FF);
				buffer[15] = (byte) ((this.stopModus & 0xFF00) >> 8);
				buffer[16] = (byte) (this.modusDistance & 0x00FF);
				buffer[17] = (byte) ((this.modusDistance & 0xFF00) >> 8);
				buffer[18] = (byte) (this.varioThresholdSink & 0x00FF);
				buffer[19] = (byte) ((this.varioThresholdSink & 0xFF00) >> 8);
				buffer[20] = (byte) (this.daylightSavingModus & 0x00FF);
				buffer[21] = (byte) ((this.daylightSavingModus & 0xFF00) >> 8);
				buffer[22] = (byte) (this.telemetryType & 0x00FF);
				buffer[23] = (byte) ((this.telemetryType & 0xFF00) >> 8);
				buffer[24] = (byte) (this.rxControl & 0x00FF);
				buffer[25] = (byte) ((this.rxControl & 0xFF00) >> 8);
				buffer[26] = (byte) (this.jetiExMask & 0x000000FF); 
				buffer[27] = (byte) ((this.jetiExMask & 0x0000FF00) >> 8);
				buffer[28] = (byte) ((this.jetiExMask & 0x00FF0000) >> 16);
				buffer[29] = (byte) ((this.jetiExMask & 0xFF000000) >> 24);
				buffer[30] = (byte) (this.varioFactor & 0x00FF);
				buffer[31] = (byte) ((this.varioFactor & 0xFF00) >> 8);
				buffer[32] = (byte) (this.frskyAddr & 0x00FF);									// 17 0x00 -> 0x1B
				buffer[33] = (byte) ((this.frskyAddr & 0xFF00) >> 8);
				buffer[34] = (byte) (this.telemetryAlarms & 0x00FF);
				buffer[35] = (byte) ((this.telemetryAlarms & 0xFF00) >> 8);
				buffer[36] = (byte) (this.heightAlarm & 0x00FF);
				buffer[37] = (byte) ((this.heightAlarm & 0xFF00) >> 8);
				buffer[38] = (byte) (this.speedMaxAlarm & 0x00FF);
				buffer[39] = (byte) ((this.speedMaxAlarm & 0xFF00) >> 8);
				buffer[40] = (byte) (this.distanceMaxAlarm & 0x00FF);
				buffer[41] = (byte) ((this.distanceMaxAlarm & 0xFF00) >> 8);
				buffer[42] = (byte) (this.voltageRxAlarm & 0x00FF);
				buffer[43] = (byte) ((this.voltageRxAlarm & 0xFF00) >> 8);
				buffer[44] = (byte) (this.tripLengthAlarm & 0x00FF);
				buffer[45] = (byte) ((this.tripLengthAlarm & 0xFF00) >> 8);

				buffer[46] = (byte) (this.currentUlAlarm & 0x00FF);
				buffer[47] = (byte) ((this.currentUlAlarm & 0xFF00) >> 8);
				buffer[48] = (byte) (this.voltageStartUlAlarm & 0x00FF);
				buffer[49] = (byte) ((this.voltageStartUlAlarm & 0xFF00) >> 8);
				buffer[50] = (byte) (this.voltageUlAlarm & 0x00FF);
				buffer[51] = (byte) ((this.voltageUlAlarm & 0xFF00) >> 8);
				buffer[52] = (byte) (this.capacityUlAlarm & 0x00FF);
				buffer[53] = (byte) ((this.capacityUlAlarm & 0xFF00) >> 8);
				buffer[54] = (byte) (this.distanceMinAlarm & 0x00FF);
				buffer[55] = (byte) ((this.distanceMinAlarm & 0xFF00) >> 8);
				buffer[56] = this.serialNumberFix; 																// 29.. fixe_Seriennummer;
				buffer[57] = this.robbe_T_Box; 																		// ..29 Robbe_T_Box;
				buffer[58] = (byte) (this.varioFilter & 0x00FF);									// 30 Vario filter
				buffer[59] = (byte) ((this.varioFilter & 0xFF00) >> 8);
				buffer[60] = (byte) (this.speedMinAlarm & 0x00FF);								// 31 10km/h --> 1000km/h
				buffer[61] = (byte) ((this.speedMinAlarm & 0xFF00) >> 8);
				buffer[62] = (byte) (this.language & 0x00FF);											// 32.. 0= german, 1=english
				buffer[63] = (byte) (this.hottSpeedType	& 0x00FF);								// ..32 0=auto, 1=GPS, 2=AirSpeed
				buffer[64] = (byte) (this.jetiExMask_UL & 0x00FF);								// 33
				buffer[65] = (byte) ((this.jetiExMask_UL & 0xFF00) >> 8);
				//B[4]
				buffer[74] = this.mLinkAddressVarioTec;														// 38..
				buffer[75] = this.mLinkAddressVoltageRx;													// ..38
				buffer[76] = (byte) (this.mLinkAddressSpeed & 0x00FF);						// 39..
				buffer[77] = this.mLinkAddressVario;															// ..39
				buffer[78] = (byte) (this.mLinkAddressDirection & 0x00FF);				// 40..
				buffer[79] = (byte) (this.mLinkAddressAirSpeed & 0x00FF);  				//..40
				buffer[80] = (byte) (this.mLinkAddressAltitude & 0x00FF);					// 41..
				buffer[81] = 0;
				buffer[82] = (byte) (this.mLinkAddressDistance & 0x00FF);					// 42..
				buffer[83] = 0;
				buffer[84] = (byte) (this.mLinkAddressTripLength & 0x00FF);				// 43..
				buffer[85] = 0;
				buffer[86] = (byte) (this.mLinkAddressSpeedMax & 0x00FF);					// 44..
				buffer[87] = 0;
				buffer[88] = (byte) (this.mLinkAddressHeightMax & 0x00FF);				// 45..
				buffer[89] = 0;
				buffer[90] = (byte) (this.mLinkAddressENL & 0x00FF);							// 46..
				buffer[91] = 0;
				buffer[92] = (byte) (this.mLinkAddressAccX & 0x00FF);							// 47..
				buffer[93] = 0;
				buffer[94] = (byte) (this.mLinkAddressAccY & 0x00FF);							// 48..
				buffer[95] = 0;
				buffer[96] = (byte) (this.mLinkAddressAccZ & 0x00FF);							// 49..
				buffer[97] = 0;
				buffer[98] = (byte) (this.mLinkAddressFlightDirection & 0x00FF);	// 50..
				buffer[99] = 0;
				buffer[100] = (byte) (this.mLinkAddressDirectionRel & 0x00FF);		// 51..
				buffer[101] = this.smoothAltitudeNulling;													// ..51
				buffer[102] = this.mLinkAddressAltitudeGain;											// 52..
				buffer[103] = this.powerBoxP2BusAdresse;													// ..52
				//C [1]																														// 53
				buffer[106] = (byte) (this.firmwareVersion & 0x00FF);							// 54
				buffer[107] = (byte) ((this.firmwareVersion & 0xFF00) >> 8);			// 54
				buffer[108] = (byte) (this.modusIGC & 0x00FF);										// 55
				buffer[109] = (byte) ((this.modusIGC & 0xFF00) >> 8);							// 55
				System.arraycopy(this.startSlotSBUS, 0, buffer, 110, 8);					// 56-59 
				buffer[118] = this.spektrumSensors;																// 60..
				buffer[119] = this.spektrumNumber;																// ..60
				buffer[120] = (byte) (this.fixPositionLatitude & 0x000000FF); 		// 61,62
				buffer[121] = (byte) ((this.fixPositionLatitude & 0x0000FF00) >> 8);
				buffer[122] = (byte) ((this.fixPositionLatitude & 0x00FF0000) >> 16);
				buffer[123] = (byte) ((this.fixPositionLatitude & 0xFF000000) >> 24);
				buffer[124] = (byte) (this.fixPositionLongitude & 0x000000FF); 		// 63,64
				buffer[125] = (byte) ((this.fixPositionLongitude & 0x0000FF00) >> 8);
				buffer[126] = (byte) ((this.fixPositionLongitude & 0x00FF0000) >> 16);
				buffer[127] = (byte) ((this.fixPositionLongitude & 0xFF000000) >> 24);
				buffer[128] = (byte) (this.fixPositionAltitude & 0x00FF);					// 65
				buffer[129] = (byte) ((this.fixPositionAltitude & 0xFF00) >> 8);	
				//unused 
				buffer[186] = (byte) (this.betaVersion & 0x00FF);									// 94..
				buffer[187] = (byte) ((this.betaVersion & 0xFF00) >> 8);					// ..94
				buffer[188] = (byte) (this.hardwareVersion & 0x00FF);							// 95..
				buffer[189] = (byte) ((this.hardwareVersion & 0xFF00) >> 8);			// ..95
				byte[] chkBuffer = new byte[192 - 2];
				System.arraycopy(buffer, 0, chkBuffer, 0, chkBuffer.length);
				tmpCheckSum = Checksum.CRC16(chkBuffer, 0);
				buffer[190] = (byte) (tmpCheckSum & 0x00FF);											// 96..
				buffer[191] = (byte) ((tmpCheckSum & 0xFF00) >> 8);								// ..96

				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "$SETUP," + StringHelper.byte2Hex2CharString(buffer, buffer.length));
				FileOutputStream file_out = new FileOutputStream(setupFile);
				DataOutputStream data_out = new DataOutputStream(file_out);
				data_out.write(buffer);
				data_out.close();
			}
			catch (Throwable e) {
				log.log(Level.WARNING, "Error writing setupfile = " + fileDialog.getFileName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage()); //$NON-NLS-1$
			}
		}
	}
	
	public int getJetiMeasurementCount() {
		int count = 37;
		for (int i = 0; i < GDE.SIZE_BYTES_INTEGER * 8; i++) {
			count -= ((this.jetiExMask & 0xFFFFFFFF) >> i) & 0x00000001;
		}
		for (int i = 0; i < GDE.SIZE_BYTES_INTEGER * 4; i++) {
			count -= ((this.jetiExMask_UL & 0xFFFF) >> i) & 0x0001;
		}
		return count;
	}
}