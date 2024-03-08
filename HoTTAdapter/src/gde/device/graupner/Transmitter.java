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
    
    Copyright (c) 2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import gde.GDE;
import gde.device.graupner.hott.MessageIds;
import gde.io.DataParser;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.Checksum;
import gde.utils.FileUtils;
import gde.utils.StringHelper;


public enum Transmitter {
	MC_32("mc-32"), MC_28("mc-28"), MC_26("mc-26"), MC_20("mc-20"), MX_20("mx-20"), MC_16("mc-16"), MX_16("mx-16"), MX_12("mx-12"), UNSPECIFIED("unspecified");

	final static Logger						log	= Logger.getLogger(Transmitter.class.getName());

	public final static byte[] mc_32_PROD_CODE 		=  new byte[] {(byte) 0x04, 0x34, (byte) 0xf4, 0x00, (byte) 0x53, 0x04, 0x00, 0x00};
	public final static byte[] mc_28_PROD_CODE 		=  new byte[] {(byte) 0xbc, 0x3f, (byte) 0xf4, 0x00, (byte) 0xef, 0x03, 0x00, 0x00};
	public final static byte[] mc_26_PROD_CODE 		=  new byte[] {(byte) 0x14, 0x42, (byte) 0xf4, 0x00, (byte) 0xea, 0x03, 0x00, 0x00};
	public final static byte[] mc_20_PROD_CODE 		=  new byte[] {(byte) 0xcc, 0x34, (byte) 0xf4, 0x00, (byte) 0xe8, 0x03, 0x00, 0x00};
	public final static byte[] mx_20_PROD_CODE 		=  new byte[] {(byte) 0x74, 0x32, (byte) 0xf4, 0x00, (byte) 0x5f, 0x04, 0x00, 0x00};
	public final static byte[] mc_16_PROD_CODE 		=  new byte[] {(byte) 0xF8, 0x35, (byte) 0xF4, 0x00, (byte) 0x09, 0x03, 0x00, 0x00};
	public final static byte[] mx_16_PROD_CODE		=  new byte[] {(byte) 0xe4, 0x30, (byte) 0xf4, 0x00, (byte) 0xb3, 0x06, 0x00, 0x00};
	public final static byte[] mx_12_PROD_CODE 		=  new byte[] {(byte) 0x10, 0x32, (byte) 0xf4, 0x00, (byte) 0x73, 0x06, 0x00, 0x00};
	
	public final static byte[] mc_32_APP_VERSION 	=  new byte[] {(byte) 0xe8, 0x03, 0x00, 0x00};
	public final static byte[] mc_28_APP_VERSION 	=  new byte[] {(byte) 0xea, 0x03, 0x00, 0x00};
	public final static byte[] mc_26_APP_VERSION 	=  new byte[] {(byte) 0xea, 0x03, 0x00, 0x00};
	public final static byte[] mc_20_APP_VERSION 	=  new byte[] {(byte) 0xea, 0x03, 0x00, 0x00};
	public final static byte[] mx_20_APP_VERSION 	=  new byte[] {(byte) 0xea, 0x03, 0x00, 0x00};
	public final static byte[] mc_16_APP_VERSION 	=  new byte[] {(byte) 0xea, 0x03, 0x00, 0x00};
	public final static byte[] mx_16_APP_VERSION 	=  new byte[] {(byte) 0xe9, 0x03, 0x00, 0x00};
	public final static byte[] mx_12_APP_VERSION 	=  new byte[] {(byte) 0xe9, 0x03, 0x00, 0x00};

	public final static byte[] MEMORY_VERSION 		=  new byte[] {(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff};

	public final static byte[] mc_32_MEM_INFO 		=  new byte[] {0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, (byte) 0xFF,(byte) 0xFF,(byte) 0xFF};
	public final static byte[] mc_28_MEM_INFO 		=  new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,(byte) 0xff,(byte) 0xff};
	public final static byte[] mc_26_MEM_INFO 		=  new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,(byte) 0xff,(byte) 0xff};
	public final static byte[] mc_20_MEM_INFO 		=  new byte[] {0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, (byte) 0x20,(byte) 0x20,(byte) 0x20};
	public final static byte[] mx_20_MEM_INFO 		=  new byte[] {0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, (byte) 0x20,(byte) 0x20,(byte) 0x20};
	public final static byte[] mc_16_MEM_INFO 		=  new byte[] {0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, (byte) 0x20,(byte) 0x20,(byte) 0xFF};
	
	public final static int[]		mc_32_20_0x1840_0x1988	= new int[] { 0x1840, 0x1988 };
	public final static int[]		mc_32_20_0x1840					= new int[] { 0x1848, 0x1870, 0x1898, 0x18C0, 0x18E8, 0x1910, 0x1938, 0x1960 };
	public final static int[]		mc_32_20_0x198A_0x1AB6	= new int[] { 0x198A, 0x1AB6 };
	public final static int[]		mc_32_20_0x198A					= new int[] { 0x19D2, 0x19FC, 0x1A26, 0x1A50 };
	public final static int[]		mc_32_20_0x1AC5_0x1EDF	= new int[] { 0x1AC5, 0x1EDF };
	public final static int[]		mc_32_20_0x1AC5					= new int[] { 0x1B1F, 0x1B47, 0x1B6F, 0x1B97, 0x1BBF, 0x1BE7, 0x1C0F, 0x1C37, 0x1C5F, 0x1C87, 0x1CAF, 0x1CFF, 0x1D27, 0x1D4F, 0x1D77, 0x1D9F,	0x1DC7, 0x1DEF, 0x1E17, 0x1E67, 0x1E8F, 0x1EB7	};
	public final static int[]		mc_32_20_0x21C2_0x238A	= new int[] { 0x21C2, 0x238A };
	public final static int[]		mc_32_20_0x21C2					= new int[] { 0x224A, 0x2272, 0x229A, 0x22C2, 0x22EA, 0x2312, 0x233A, 0x2362 };

	private final String	value;
	
	private Transmitter(String v) {
		this.value = v;
	}

	public String value() {
		return this.value;
	}

  public static Transmitter fromValue(String v) {
    for (Transmitter c: Transmitter.values()) {
        if (c.value.equals(v)) {
            return c;
        }
    }
    throw new IllegalArgumentException(v);
}

	/**
	 * detect transmitter type using a few identification items
	 * @param fileName
	 * @return
	 */
	public static Transmitter detectTransmitter(String fileName, String fqPathName) {
		byte[] inBytes = new byte[0x141];
		DataInputStream in = null;
		
		try {
			in = new DataInputStream( new FileInputStream(new File(fqPathName)));
			in.read(inBytes);
			in.close();
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		
		return detectSourceTransmitter(inBytes);
	}

	/**
	 * @param result
	 * @param inBytes
	 * @return
	 */
	public static Transmitter detectSourceTransmitter(byte[] inBytes) {
		Transmitter result = Transmitter.UNSPECIFIED;
		if (log.isLoggable(Level.FINE)) 
			log.log(Level.FINE, StringHelper.byte2Hex2CharString(inBytes, inBytes.length));
		if ((inBytes[0x08]&0xFF) == 0xE8) {
			boolean isMC32 = true;
			for (int i = 0; i < 2; i++) {
				if (inBytes[i] != Transmitter.mc_32_PROD_CODE[i]) {
					isMC32 = false;
					break;
				}
			}
			result = isMC32 ? Transmitter.MC_32 : Transmitter.UNSPECIFIED;
		}
		else if ((inBytes[0x08]&0xFF) == 0xEA) {
			boolean isMC28 = true, isMC26 = true, isMC20 = true, isMX20 = true, isMC16 = true;
			for (int i = 0; i < 2; i++) {
				if (inBytes[i] != Transmitter.mc_28_PROD_CODE[i]) {
					isMC28 = false;
					break;
				}
			}
			for (int i = 0; i < 2; i++) {
				if (inBytes[i] != Transmitter.mc_26_PROD_CODE[i]) {
					isMC26 = false;
					break;
				}
			}
			for (int i = 0; i < 2; i++) {
				if (inBytes[i] != Transmitter.mc_20_PROD_CODE[i]) {
					isMC20 = false;
					break;
				}
			}
			for (int i = 0; i < 2; i++) {
				if ((inBytes[0x00 + i]&0xFF) != (Transmitter.mx_20_PROD_CODE[i]&0xFF)){
					isMX20 = false;
					break;
				}
			}
			for (int i = 0; i < 2; i++) {
				if ((inBytes[0x00 + i]&0xFF) != (Transmitter.mc_16_PROD_CODE[i]&0xFF)){
					isMC16 = false;
					break;
				}
			}
			result = isMC28 ? Transmitter.MC_28 : isMC26 ? Transmitter.MC_26 : isMC20 ? Transmitter.MC_20 : isMX20 ? Transmitter.MX_20 : isMC16 ? Transmitter.MC_16 : Transmitter.UNSPECIFIED;
		}
		else if ((inBytes[0x08]&0xFF) == 0xE9) {
			boolean isMX16 = true, isMX12 = true;
			for (int i = 0; i < 2; i++) {
				if (inBytes[i] != Transmitter.mx_16_PROD_CODE[i]) {
					isMX16 = false;
					break;
				}
			}
			for (int i = 0; i < 2; i++) {
				if (inBytes[i] != Transmitter.mx_12_PROD_CODE[i]){
					isMX12 = false;
					break;
				}
			}
			result = isMX16 ? Transmitter.MX_16 : isMX12 ? Transmitter.MX_12 : Transmitter.UNSPECIFIED;
		}
		return result;
	}

	/**
	 * simple model configuration convert function based on exchanged product code and application code
	 * transmitter ID seams to be not required to replace and will be updated during use
	 * @param filepath
	 * @param targetTransmitter
	 */
	public static void convert2target(String filepath, Transmitter targetTransmitter) {
		DataInputStream in = null;
		DataOutputStream out = null;
		byte[] bytes = new byte[targetTransmitter.ordinal() <= 5 ? 12288 : 8192];

		try {
			filepath = filepath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
			File inputFile = new File(filepath);
			in = new DataInputStream( new FileInputStream(inputFile));
			String outFilePath = filepath.substring(0, filepath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX));
			outFilePath = outFilePath.substring(0, outFilePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX)+1) + targetTransmitter.value() + GDE.STRING_FILE_SEPARATOR_UNIX;
			FileUtils.checkDirectoryAndCreate(outFilePath);
			outFilePath = outFilePath+ inputFile.getName();
			File outputFile = new File(outFilePath);
			
			out = new DataOutputStream( new FileOutputStream(outputFile));
			in.read(bytes);

			switch (targetTransmitter) {
			case MC_32:
				if (log.isLoggable(Level.FINE)) System.out.println("to " + MC_32.value());
				cleanReceiverBinding(bytes);
				
				switch (detectSourceTransmitter(bytes)) {
				case MC_26:
				case MC_28:
					migrateFromFirstSector(bytes); 			
					migrateFromSecondSector(bytes); 		
					migrateFromSetPos2PhaseAnouncement(bytes);
					break;
				default:
					break;
				}
				
				cleanSwAssignements(bytes);
				cleanControlAdustSw(bytes);
				
				System.arraycopy(Transmitter.mc_32_PROD_CODE, 0, bytes, 0x00, Transmitter.mc_32_PROD_CODE.length);
				bytes[0x08] = (byte) 0xE8;
				bytes[0x108] = (byte) 0xE8;
				System.arraycopy(Transmitter.mc_32_MEM_INFO, 0, bytes, 0x140, Transmitter.mc_32_MEM_INFO.length);
				bytes[0x160] = (byte) 0xFF; // bind information
				
				convertCurves(bytes, MC_20.ordinal(), MC_32.ordinal());
				break;
				
			case MC_28:
				if (log.isLoggable(Level.FINE)) System.out.println("to " + MC_28.value());
				cleanReceiverBinding(bytes);

				switch (detectSourceTransmitter(bytes)) {
				case MC_32:
					cleanSwAssignements(bytes); 		
					cleanControlAdustSw(bytes);			
					cleanPhaseSetting(bytes); 			
					migrateDualMixerChannel(bytes); 
					migrateChannelMapping(bytes); 	
					migrateFreeMixerChannel(bytes); 
					convertCurves(bytes, MC_32.ordinal(), MC_20.ordinal()); 
					
					migrateToFirstSector(bytes); 			
					migrateToSecondSector(bytes); 		
					migrateToSetPos2PhaseAnouncement(bytes);
					break;

				case MC_20:
				case MC_16:
				case MX_20:
				default:
					cleanSwAssignements(bytes); 		
					cleanControlAdustSw(bytes);			
					cleanPhaseSetting(bytes); 			
					migrateDualMixerChannel(bytes); 
					migrateChannelMapping(bytes); 	
					migrateFreeMixerChannel(bytes); 
					
					migrateToFirstSector(bytes); 			
					migrateToSecondSector(bytes); 		
					migrateToMarkerKey2PhaseAnouncement(bytes);
					break;
				}
				
				System.arraycopy(Transmitter.mc_28_PROD_CODE, 0, bytes, 0x00, Transmitter.mc_28_PROD_CODE.length);
				System.arraycopy(Transmitter.mc_28_MEM_INFO, 0, bytes, 0x140, Transmitter.mc_28_MEM_INFO.length);
				bytes[0x160] = (byte) 0x05;	// bind information	
				break;
				
			case MC_26:
				if (log.isLoggable(Level.FINE)) System.out.println("to " + MC_26.value());
				cleanReceiverBinding(bytes);


				switch (detectSourceTransmitter(bytes)) {
				case MC_32:
					cleanSwAssignements(bytes); 		
					cleanControlAdustSw(bytes);			
					cleanPhaseSetting(bytes); 			
					migrateDualMixerChannel(bytes); 
					migrateChannelMapping(bytes); 	
					migrateFreeMixerChannel(bytes); 
					convertCurves(bytes, MC_32.ordinal(), MC_20.ordinal()); 
					
					migrateToSecondSector(bytes); 		
					migrateToFirstSector(bytes); 			
					migrateToSetPos2PhaseAnouncement(bytes);
					break;

				case MC_20:
				case MC_16:
				case MX_20:
				default:
					cleanSwAssignements(bytes); 		
					cleanControlAdustSw(bytes);			
					cleanPhaseSetting(bytes); 			
					migrateDualMixerChannel(bytes); 
					migrateChannelMapping(bytes); 	
					migrateFreeMixerChannel(bytes); 
					
					migrateToSecondSector(bytes); 		
					migrateToFirstSector(bytes); 			
					migrateToMarkerKey2PhaseAnouncement(bytes);
					break;
				}
				
				System.arraycopy(Transmitter.mc_26_PROD_CODE, 0, bytes, 0x00, Transmitter.mc_26_PROD_CODE.length);
				System.arraycopy(Transmitter.mc_26_MEM_INFO, 0, bytes, 0x140, Transmitter.mc_26_MEM_INFO.length);
				bytes[0x160] = (byte) 0x05;	// bind information	
				break;
				
			case MX_20:
				if (log.isLoggable(Level.FINE)) System.out.println("to " + MX_20.value());			
				cleanReceiverBinding(bytes);
								
				switch (detectSourceTransmitter(bytes)) {
				case MC_26:
				case MC_28:
					migrateFromFirstSector(bytes); 			
					migrateFromSecondSector(bytes); 		
					migrateFromMarkerKey2PhaseAnouncement(bytes);
					cleanPhaseSetting(bytes);
					cleanPhaseSetting(bytes);
					migrateDualMixerChannel(bytes);
					migrateChannelMapping(bytes);
					migrateFreeMixerChannel(bytes);
					cleanSwAssignements(bytes);
					cleanControlAdustSw(bytes);
					break;

				case MC_32:
					cleanPhaseSetting(bytes);
					cleanPhaseSetting(bytes);
					migrateDualMixerChannel(bytes);
					migrateChannelMapping(bytes);
					migrateFreeMixerChannel(bytes);
					cleanSwAssignements(bytes);
					cleanControlAdustSw(bytes);
					convertCurves(bytes, MC_32.ordinal(), MX_20.ordinal());
					break;

				default:
					break;
				}
				
				System.arraycopy(Transmitter.mx_20_PROD_CODE, 0, bytes, 0x00, Transmitter.mx_20_PROD_CODE.length);
				bytes[0x008] = (byte) 0xEA;
				bytes[0x108] = (byte) 0xEA;
				System.arraycopy(Transmitter.mc_20_MEM_INFO, 0, bytes, 0x140, Transmitter.mc_20_MEM_INFO.length);
				bytes[0x160] = (byte) 0xFF; // bind information
				break;
				
			case MC_20:
				if (log.isLoggable(Level.FINE)) System.out.println("to " + MC_20.value());
				cleanReceiverBinding(bytes);

				switch (detectSourceTransmitter(bytes)) {
				case MC_26:
				case MC_28:
					migrateFromFirstSector(bytes); 			
					migrateFromSecondSector(bytes); 		
					migrateFromMarkerKey2PhaseAnouncement(bytes);
					cleanPhaseSetting(bytes);
					cleanPhaseSetting(bytes);
					migrateDualMixerChannel(bytes);
					migrateChannelMapping(bytes);
					migrateFreeMixerChannel(bytes);
					cleanSwAssignements(bytes);
					cleanControlAdustSw(bytes);
					break;

				case MC_32:
					cleanPhaseSetting(bytes);
					cleanPhaseSetting(bytes);
					migrateDualMixerChannel(bytes);
					migrateChannelMapping(bytes);
					migrateFreeMixerChannel(bytes);
					cleanSwAssignements(bytes);
					cleanControlAdustSw(bytes);
					convertCurves(bytes, MC_32.ordinal(), MX_20.ordinal());
					break;

				default:
					break;
				}
				
				System.arraycopy(Transmitter.mc_20_PROD_CODE, 0, bytes, 0x00, Transmitter.mc_20_PROD_CODE.length);
				bytes[0x008] = (byte) 0xEA;
				bytes[0x108] = (byte) 0xEA;
				System.arraycopy(Transmitter.mc_20_MEM_INFO, 0, bytes, 0x140, Transmitter.mc_20_MEM_INFO.length);
				bytes[0x160] = (byte) 0x05; // bind information
				break;

			case MC_16:
				if (log.isLoggable(Level.FINE)) System.out.println("to " + MC_16.value());
				cleanReceiverBinding(bytes);

				switch (detectSourceTransmitter(bytes)) {
				case MC_26:
				case MC_28:
					migrateFromFirstSector(bytes); 			
					migrateFromSecondSector(bytes); 		
					migrateFromMarkerKey2PhaseAnouncement(bytes);
					cleanPhaseSetting(bytes);
					cleanPhaseSetting(bytes);
					migrateDualMixerChannel(bytes);
					migrateChannelMapping(bytes);
					migrateFreeMixerChannel(bytes);
					cleanSwAssignements(bytes);
					cleanControlAdustSw(bytes);
					break;

				case MC_32:
					cleanPhaseSetting(bytes);
					cleanPhaseSetting(bytes);
					migrateDualMixerChannel(bytes);
					migrateChannelMapping(bytes);
					migrateFreeMixerChannel(bytes);
					cleanSwAssignements(bytes);
					cleanControlAdustSw(bytes);
					convertCurves(bytes, MC_32.ordinal(), MX_20.ordinal());
					break;

				default:
					break;
				}
				
				System.arraycopy(Transmitter.mc_16_PROD_CODE, 0, bytes, 0x00, Transmitter.mc_16_PROD_CODE.length);
				bytes[0x008] = (byte) 0xEA;
				bytes[0x108] = (byte) 0xEA;
				System.arraycopy(Transmitter.mc_16_MEM_INFO, 0, bytes, 0x140, Transmitter.mc_16_MEM_INFO.length);
				bytes[0x160] = (byte) 0x05; // bind information
				break;
				
			case MX_16:
				System.arraycopy(Transmitter.mx_16_PROD_CODE, 0, bytes, 0x00, Transmitter.mx_16_PROD_CODE.length);				
				bytes[0x11db] = (byte) 0xfd;
				//bytes[0x11ef] = (byte) 0x82;
				//bytes[0x11f0] = (byte) 0x81; 
				calculateAndWriteCRC(bytes, 0x11db, 0x11ef);
				calculateAndWriteCRC(bytes, 0x11f1, 0x11f2);
				calculateAndWriteCRC(bytes, 0x11f4, 0x11f5);
				break;
			case MX_12:
				System.arraycopy(Transmitter.mx_12_PROD_CODE, 0, bytes, 0x00, Transmitter.mx_12_PROD_CODE.length);
				bytes[0x11db] = (byte) 0xdd;
				//bytes[0x11ef] = (byte) 0x5d;
				//bytes[0x11f0] = (byte) 0x74; 
				calculateAndWriteCRC(bytes, 0x11db, 0x11ef);
				calculateAndWriteCRC(bytes, 0x11f1, 0x11f2);
				calculateAndWriteCRC(bytes, 0x11f4, 0x11f5);
				break;
			default:
				break;
			}

			in.close();
			in = null;
			out.write(bytes);
			out.close();
			out = null;
			
			if (DataExplorer.getInstance() != null)
				switch (targetTransmitter) {
				case MC_32:
					DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI2403));
					break;

				case MC_28:
				case MC_26:
					if (detectSourceTransmitter(bytes) == Transmitter.MC_32) {
						DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI2402));
					}
					else {
						DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI2403));
					}
					break;

				case MC_20:
				case MX_20:
				case MC_16:
					if (detectSourceTransmitter(bytes) == Transmitter.MC_32) {
						DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI2402));
					}
					else {
						DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI2403));
					}
					break;
					
				case MX_16:
				case MX_12:
					DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI2401));
					break;

				default:
					break;
				}
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE2401));
		}
		finally {
			try {
				if (in != null) in.close();
				if (out != null) out.close();
			}
			catch (IOException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	public static void cleanReceiverBinding(byte[] bytes) {
		for (int i = 0; i < 4; i++) {
			bytes[0x101E] = (byte) 0x00; //bind receiver 4=none
		}
		for (int i = 0; i < 4*16; i++) {
			bytes[0x1022+i] = (byte) 0x00;
		}
	}
	
	public static void migrateDualMixerChannel(byte[] bytes) {
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x17CD", bytes, 0x17CD, 1, 1, 12); //from
		for (int i = 0; i < 4; i++) { //20 max = 12; 32 max = 16
			if (bytes[0x17CD + i] > 12) bytes[0x1996 + i] = (byte) 0x0D;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x17D1", bytes, 0x17D1, 1, 1, 12); //to
		for (int i = 0; i < 4; i++) { //20 max = 12; 32 max = 16
			if (bytes[0x17D1 + i] > 12) bytes[0x17D1 + i] = (byte) 0x0D;
		}
		calculateAndWriteCRC(bytes, 0x17CD, 0x17D9);
	}
	
	public static void migrateChannelMapping(byte[] bytes) {
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x182E", bytes, 0x182E, 1, 1, 16); 
		for (int i = 0; i < 12; i++) { //20 max = 12; 32 max = 16
			if (bytes[0x182E + i] > 12) bytes[0x182E + i] = (byte) 0x0D;
		}
		calculateAndWriteCRC(bytes, 0x182E, 0x183E);
	}
	
	public static void migrateFreeMixerChannel(byte[] bytes) {
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x1996", bytes, 0x1996, 1, 1, 12); //from
		for (int i = 0; i < 12; i++) { //20 max = 12; 32 max = 16
			if (bytes[0x1996 + i] > 12) bytes[0x1996 + i] = (byte) 0x0D;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x19A2", bytes, 0x19A2, 1, 1, 12); //to
		for (int i = 0; i < 12; i++) { //20 max = 13; 32 max = 17
			if (bytes[0x19A2 + i] > 13) bytes[0x19A2 + i] = (byte) 0x0E;
		}
	}

	public static void cleanSwAssignements(byte[] bytes) {
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x1062", bytes, 0x1062, 1, 2, 9);
		for (int i = 0; i < 2*9; i++) {
			bytes[0x1062 + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x1062", bytes, 0x1062, 1, 2, 9);
		calculateAndWriteCRC(bytes, 0x1000, 0x1074);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x1518", bytes, 0x1518, 1, 6, 16);
		for (int i = 0; i < 16*6; i++) {
			bytes[0x1518 + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x1518", bytes, 0x1518, 1, 6, 16);
		calculateAndWriteCRC(bytes, 0x14B8, 0x1578);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x157A", bytes, 0x157A, 1, 2, 8);
		for (int i = 0; i < 2*8; i++) {
			bytes[0x157A + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x157A", bytes, 0x157A, 1, 2, 8);
		
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x159A", bytes, 0x159A, 1, 2, 8);
		for (int i = 0; i < 2*8; i++) {
			bytes[0x159A + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x159A", bytes, 0x159A, 1, 2, 8);
		calculateAndWriteCRC(bytes, 0x157A, 0x15B2);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x15B4", bytes, 0x15B4, 1, 4, 8);
		for (int i = 0; i < 4*8; i++) {
			bytes[0x15B4 + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x15B4", bytes, 0x15B4, 1, 4, 8);
		calculateAndWriteCRC(bytes, 0x15B4, 0x15E4);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x1640", bytes, 0x1640, 1, 2, 6);
		for (int i = 0; i < 2*6; i++) {
			bytes[0x1640 + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x1640", bytes, 0x1640, 1, 2, 6);
		calculateAndWriteCRC(bytes, 0x1640, 0x165E);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x173C", bytes, 0x173C, 1, 2, 3);
		for (int i = 0; i < 2*3; i++) {
			bytes[0x173C + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x173C", bytes, 0x173C, 1, 2, 3);
		calculateAndWriteCRC(bytes, 0x1734, 0x1742);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x174D", bytes, 0x174D, 1, 2, 5);
		for (int i = 0; i < 2*5; i++) {
			bytes[0x174D + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x174D", bytes, 0x174D, 1, 2, 5);
		calculateAndWriteCRC(bytes, 0x1744, 0x1757);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x1811", bytes, 0x1811, 1, 1, 12);
		for (int i = 0; i < 1*12; i++) {
			bytes[0x1811 + i] = (byte) 0x00;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x1811", bytes, 0x182A, 1, 1, 12);
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x182A", bytes, 0x182A, 1, 2, 1);
		for (int i = 0; i < 2*1; i++) {
			bytes[0x182A + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x182A", bytes, 0x182A, 1, 2, 1);
		calculateAndWriteCRC(bytes, 0x1811, 0x182C);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x19AE", bytes, 0x19AE, 1, 2, 12);
		for (int i = 0; i < 2*12; i++) {
			bytes[0x19AE + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x19AE", bytes, 0x19AE, 1, 2, 12);
		calculateAndWriteCRC(bytes, 0x198A, 0x1AB6);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x1F59", bytes, 0x1F59, 1, 4, 16);
		for (int i = 0; i < 16*4; i++) {
			bytes[0x1F59 + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x1F59", bytes, 0x1F59, 1, 4, 16);
		calculateAndWriteCRC(bytes, 0x1EE9, 0x1F99);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x2482", bytes, 0x2482, 1, 2, 1);
		for (int i = 0; i < 2*1; i++) {
			bytes[0x2482 + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x2482", bytes, 0x2482, 1, 2, 1);
		calculateAndWriteCRC(bytes, 0x246E, 0x2484);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x24D3", bytes, 0x24D3, 1, 2, 1);
		for (int i = 0; i < 2*1; i++) {
			bytes[0x24D3 + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x24D3", bytes, 0x24D3, 1, 2, 1);
		calculateAndWriteCRC(bytes, 0x249C, 0x24D6);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x258F", bytes, 0x258F, 1, 2, 8);
		for (int i = 0; i < 2*8; i++) {
			bytes[0x258F + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x258F", bytes, 0x258F, 1, 2, 8);
		calculateAndWriteCRC(bytes, 0x2587, 0x259F);
	}
	
	public static void cleanControlAdustSw(byte[] bytes) {
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x12CA", bytes, 0x12CA, 1, 8, 60);
		for (int i = 0; i < 8*60; i++) {
			bytes[0x12CA + i] = (byte) 0xFF;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x12CA", bytes, 0x12CA, 1, 8, 60);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x14AA", bytes, 0x14AA, 1, 1, 12);
		for (int i = 0; i < 2*5; i++) {
			bytes[0x14AA + i] = (byte) 0x01;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x14AA", bytes, 0x14AA, 1, 1, 12);	
		calculateAndWriteCRC(bytes, 0x10EA, 0x14B6);
	}

	public static void cleanPhaseSetting(byte[] bytes) {
		if (log.isLoggable(Level.FINE)) StringHelper.printMemChar("0x15F6", bytes, 0x15F6, 1, 7, 8);
		byte[] name = new byte[] {0x20,0x20,0x20,0x20,0x20,0x20,0x20};
		for (int i = 0; i < 8; i++) {
			System.arraycopy(name, 0, bytes, 0x15F6+i*name.length, name.length);
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemChar("0x15F6", bytes, 0x15F6, 1, 7, 8);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x162E", bytes, 0x162E, 1, 8, 1);
		for (int i = 0; i < 8; i++) {
			bytes[0x162E + i] = (byte) 0x00;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x162E", bytes, 0x162E, 1, 8, 1);

		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x1636", bytes, 0x1636, 1, 8, 1);
		for (int i = 0; i < 8; i++) {
			bytes[0x1636 + i] = (byte) 0x00;
		}
		if (log.isLoggable(Level.FINE)) StringHelper.printMemHex("0x1636", bytes, 0x1636, 1, 8, 1);
		
		calculateAndWriteCRC(bytes, 0x15E6, 0x163E);
	}

	/**
	 * convert all curves from 8 to 5 points and vice versa depending on source/target transmitter
	 * @param bytes
	 */
	public static void convertCurves(byte[] bytes, int from, int to) {
		for (int i = 0; i < mc_32_20_0x1840.length; i++) {
			convertCurve(bytes, mc_32_20_0x1840[i], from, to);
		}
		calculateAndWriteCRC(bytes, mc_32_20_0x1840_0x1988[0], mc_32_20_0x1840_0x1988[1]);

		for (int i = 0; i < mc_32_20_0x198A.length; i++) {
			convertCurve(bytes, mc_32_20_0x198A[i], from, to);
		}
		calculateAndWriteCRC(bytes, mc_32_20_0x198A_0x1AB6[0], mc_32_20_0x198A_0x1AB6[1]);

		for (int i = 0; i < mc_32_20_0x1AC5.length; i++) {
			convertCurve(bytes, mc_32_20_0x1AC5[i], from, to);
		}
		calculateAndWriteCRC(bytes, mc_32_20_0x1AC5_0x1EDF[0], mc_32_20_0x1AC5_0x1EDF[1]);

		for (int i = 0; i < mc_32_20_0x21C2.length; i++) {
			convertCurve(bytes, mc_32_20_0x21C2[i], from, to);
		}
		calculateAndWriteCRC(bytes, mc_32_20_0x21C2_0x238A[0], mc_32_20_0x21C2_0x238A[1]);
	}

	/**
	 * calculate the CRC16CCITT of the given area and write to end address
	 * @param bytes
	 * @param start address
	 * @param end address
	 */
	public static void calculateAndWriteCRC(byte[] bytes, int start, int end) {
		int checkSum = Checksum.CRC16CCITT(bytes, start, end - start);
		bytes[end] = (byte) (checkSum & 0xFF);
		bytes[end+1] = (byte) ((checkSum & 0xFF00) >> 8);
	}

	/**
	 * convert curves points from 8 to 5 points, assign warning byte if more than convertible points used
	 * @param bytes pointer to byte array
	 * @param index of byte array
	 * @param ptCount point count
	 * @param from transmitter ordinal
	 * @param to transmitter ordinal
	 * @return
	 */
	public static boolean convertCurve(byte[] bytes, int index, int from, int to) {
		boolean isCheckNeeded = false;
		if (log.isLoggable(Level.FINE)) printCurveData(bytes, index);
		// check number of used curve points
		if (from == MC_32.ordinal()) {
			int ptCount = 0;
			for (int i = index; i < index+8; i++) {
				if (bytes[32 + i] == 1) ++ptCount;
				if (ptCount > 6) {
					isCheckNeeded = true;
					break;
				}
			}
			bytes[(5*2) + index] = bytes[(7*2) + index];
			bytes[(5*2) + index + 1] = bytes[(7*2) + index + 1];
			bytes[16 + (5*2) + index] = bytes[16 + (7*2) + index];
			bytes[16 + (5*2) + index + 1] = bytes[16 + (7*2) + index + 1];
			bytes[32 + 5 + index] = 1;
			
			if (index < 0x224A) {
				bytes[(6*2) + index] = (byte) 0x4A;
				bytes[(6*2) + index + 1] = (byte) 0x00;
				bytes[(7*2) + index] = (byte) 0x64;
				bytes[(7*2) + index + 1] = (byte) 0x00;
				if ((index >= 0x19D2 && index < 0x1B1F) || index == 0x1B6F || index == 0x1BE7 || index == 0x1C5F || index == 0x1D4F || index == 0x1DC7 || index == 0x1EB7) {
					bytes[16 + (6*2) + index] = (byte) 0x00;
					bytes[16 + (6*2) + index + 1] = (byte) 0x00;
					bytes[16 + (7*2) + index] = (byte) 0x00;
					bytes[16 + (7*2) + index + 1] = (byte) 0x00;
				}
				else {
					bytes[16 + (6*2) + index] = (byte) 0x4A;
					bytes[16 + (6*2) + index + 1] = (byte) 0x00;
					bytes[16 + (7*2) + index] = (byte) 0x64;
					bytes[16 + (7*2) + index + 1] = (byte) 0x00;
				}
				bytes[32 + 6 + index] = 0;
				bytes[32 + 7 + index] = 1;
			}
			else {
				bytes[(6*2) + index] = (byte) 0xff;
				bytes[(6*2) + index + 1] = (byte) 0xff;
				bytes[(7*2) + index] = (byte) 0xff;
				bytes[(7*2) + index + 1] = (byte) 0xff;
				bytes[16 + (6*2) + index] = (byte) 0xff;
				bytes[16 + (6*2) + index + 1] = (byte) 0xff;
				bytes[16 + (7*2) + index] = (byte) 0xff;
				bytes[16 + (7*2) + index + 1] = (byte) 0xff;
				bytes[32 + 6 + index] = -1;
				bytes[32 + 7 + index] = -1;
			}
			if (index >= 0x1B97 && index <= 0x1EB7) {
				bytes[32 + 0 + index] = 0;
			}
		}
		else if (to == MC_32.ordinal()) {
			bytes[(7*2) + index] = bytes[(5*2) + index];
			bytes[(7*2) + index + 1] = bytes[(5*2) + index + 1];
			bytes[16 + (7*2) + index] = bytes[16 + (5*2) + index];
			bytes[16 + (7*2) + index + 1] = bytes[16 + (5*2) + index + 1];
			bytes[32 + 5 + index] = 0;
			bytes[32 + 6 + index] = 0;
			bytes[32 + 7 + index] = 1;
			
			if (index < 0x224A) {
				bytes[(5*2) + index] = (byte) 0x46;
				bytes[(5*2) + index + 1] =(byte) 0x00;
				bytes[(6*2) + index] = (byte) 0x55;
				bytes[(6*2) + index + 1] =(byte) 0x00;
				
				if ((index >= 0x19D2 && index < 0x1B1F) || index == 0x1B6F || index == 0x1BE7 || index == 0x1C5F || index == 0x1D4F || index == 0x1DC7 || index == 0x1EB7) {
					bytes[16 + (5*2) + index] = (byte) 0x00;
					bytes[16 + (5*2) + index + 1] = (byte) 0x00;
					bytes[16 + (6*2) + index] = (byte) 0x00;
					bytes[16 + (6*2) + index + 1] = (byte) 0x00;
				}
				else {
					bytes[16 + (5*2) + index] = (byte) 0x46;
					bytes[16 + (5*2) + index + 1] = (byte) 0x00;
					bytes[16 + (6*2) + index] = (byte) 0x55;
					bytes[16 + (6*2) + index + 1] = (byte) 0x00;
				}
			}
			else {
				bytes[(5*2) + index] = (byte) 0x46;
				bytes[(5*2) + index + 1] =(byte) 0x00;
				bytes[(6*2) + index] = (byte) 0x55;
				bytes[(6*2) + index + 1] =(byte) 0x00;
				bytes[16 + (5*2) + index] = (byte) 0x00;
				bytes[16 + (5*2) + index + 1] =(byte) 0x00;
				bytes[16 + (6*2) + index] = (byte) 0x00;
				bytes[16 + (6*2) + index + 1] =(byte) 0x00;
			}
			
			if (index >= 0x1B97 && index <= 0x1EB7) {
				bytes[32 + 0 + index] = 1;
			}
		}
		if (log.isLoggable(Level.FINE)) printCurveData(bytes, index);
		
		return isCheckNeeded;
	}

	/**
	 * debug print curve data points
	 * @param bytes
	 * @param index
	 * @return
	 */
	public static int printCurveData(byte[] bytes, int _index) {
		int index = _index;
		for (int i = 0; i < 3; i++) {
			switch (i) {
			default:// 32(7) -> 20(5)
				for (int j = 0; j < 8*2; j+=2) {
					System.out.print(DataParser.parse2Short(bytes, index));
					System.out.print("; ");
					index+=2;
				}
				if (log.isLoggable(Level.FINE)) System.out.println();
				break;
			case 2:
				for (int j = 0; j < 8; j++) {
					System.out.print(bytes[index++]);
					System.out.print("; ");
				}
				if (log.isLoggable(Level.FINE)) System.out.println();
				break;
			}
		}
		return index;
	}
	
	private static void migrateToSetPos2PhaseAnouncement(final byte[] bytes) {
		byte[] buffer = new byte[0x2D18 - 0x260A];
		InputStream is = Transmitter.class.getClassLoader().getResourceAsStream("resource/MC-28_Dummy.mdl");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		int bytesRead = 0;
		int offset = 0;
    
    try {
    	is.skip(0x260A + offset);
			while ((bytesRead += is.read(buffer)) != -1) {
			    System.arraycopy(buffer, 0, bytes, 0x260A + offset, bytesRead);
			    if (bytesRead == buffer.length)
			    	break;
			    offset = bytesRead;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
    finally {
    	try {
				br.close();
	      is.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void migrateFromSetPos2PhaseAnouncement(final byte[] bytes) {
		for (int i = 0; i < 0x2D18 - 0x260A; i++) {
			bytes[0x260A + i] = (byte) 0xFF;
		}
	}

	private static void migrateToMarkerKey2PhaseAnouncement(final byte[] bytes) {
		byte[] buffer = new byte[0x2D18 - 0x25c9];
		InputStream is = Transmitter.class.getClassLoader().getResourceAsStream("resource/MC-28_Dummy.mdl");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		int bytesRead = 0;
		int offset = 0;
    
    try {
    	is.skip(0x25C9 + offset);
			while ((bytesRead += is.read(buffer)) != -1) {
			    System.arraycopy(buffer, 0, bytes, 0x25C9 + offset, bytesRead);
			    if (bytesRead == buffer.length)
			    	break;
			    offset = bytesRead;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
    finally {
    	try {
				br.close();
	      is.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void migrateFromMarkerKey2PhaseAnouncement(final byte[] bytes) {
		for (int i = 0; i < 0x2D18 - 0x25C9; i++) {
			bytes[0x25c9 + i] = (byte) 0xFF;
		}
	}
	
	private static void migrateToFirstSector(final byte[] bytes) {
		System.arraycopy(bytes, 0x182E, bytes, 0x1832, 0x1F9B - 0x182E);
		for (int i = 0; i < 0x1830 - 0x1811; i++) {
			bytes[0x1811 + i] = 0;			
		}
		calculateAndWriteCRC(bytes, 0x1811, 0x1830);
	}
	
	private static void migrateFromFirstSector(final byte[] bytes) {
		System.arraycopy(bytes, 0x1815, bytes, 0x1811, 0x1F9B - 0x1815);
		for (int i = 0; i < 0x182C - 0x1811; i++) {
			bytes[0x1811 + i] = 0;			
		}
		calculateAndWriteCRC(bytes, 0x1811, 0x182C);
	}

	private static void migrateToSecondSector(final byte[] bytes) {
		System.arraycopy(bytes, 0x24DE, bytes, 0x24E5, 0x25C3 - 0x24DE);
		for (int i = 0; i < 0x24E4 - 0x24DC; i++) {
			bytes[0x24DC + i] = (byte) 0xFF;			
		}
		calculateAndWriteCRC(bytes, 0x24D8, 0x24E3);
	}

	private static void migrateFromSecondSector(final byte[] bytes) {
		System.arraycopy(bytes, 0x24E3, bytes, 0x24DC, 0x25C9 - 0x24E3);
		calculateAndWriteCRC(bytes, 0x24D8, 0x24DC);
	}
}
