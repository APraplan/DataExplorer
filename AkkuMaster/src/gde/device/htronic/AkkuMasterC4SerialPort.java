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
package gde.device.htronic;

import gde.comm.DeviceCommPort;
import gde.device.IDevice;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * SSerial port implementation for AkkuMaster C4 device
 * @author Winfried Brügmann
 */
public class AkkuMasterC4SerialPort extends DeviceCommPort {
	final static Logger					log															= Logger.getLogger(AkkuMasterC4SerialPort.class.getName());

	public static final String	PROCESS_NAME										= Messages.getString(MessageIds.GDE_MSGT1114);	// "4 ) Laden" = AkkuMaster aktiv Laden
	public static final String	PROCESS_ERROR_NO								= Messages.getString(MessageIds.GDE_MSGT1115);	// "0" = kein Fehler
	public static final String	PROCESS_VOLTAGE									= Messages.getString(MessageIds.GDE_MSGT1116);	// [mV]
	public static final String	PROCESS_CURRENT									= Messages.getString(MessageIds.GDE_MSGT1117);  // [mA] 	(laden/entladen)
	public static final String	PROCESS_CAPACITY								= Messages.getString(MessageIds.GDE_MSGT1118);	// [mAh] (laden/entladen)
	public static final String	PROCESS_POWER										= Messages.getString(MessageIds.GDE_MSGT1119);	// [mW]	
	public static final String	PROCESS_ENERGIE									= Messages.getString(MessageIds.GDE_MSGT1120);	// [mWh]
	public static final String	PROCESS_TIME										= Messages.getString(MessageIds.GDE_MSGT1121);	// [msec]

	public static final String	VERSION_NUMBER									= Messages.getString(MessageIds.GDE_MSGT1122).trim();
	public static final String	VERSION_DATE										= Messages.getString(MessageIds.GDE_MSGT1123).trim();
	public static final String	VERSION_TYPE_CURRENT						= Messages.getString(MessageIds.GDE_MSGT1124).trim();
	public static final String	VERSION_TYPE_FRONT							= Messages.getString(MessageIds.GDE_MSGT1125).trim();

	public static final byte		channel_1[]											= new byte[] { 0x00 };
	public static final byte		channel_2[]											= new byte[] { 0x40 };
	public static final byte		channel_3[]											= new byte[] { (byte) 0x80 };
	public static final byte		channel_4[]											= new byte[] { (byte) 0xC0 };

	// data type command description
	final static byte						readVersion[]										= new byte[] { 0x16 };
	final static byte						readConfiguration[]							= new byte[] { 0x11 };	// Lese eingestellte Werte
	final static byte						readMeasuredValues[]						= new byte[] { 0x12 };	// Lese gemessene Werte
	final static byte						readAdjustedValues[]						= new byte[] { 0x21 };	// Lese eingestellte Werte zus. Parameter
	final static byte						setNewProgramm									= 0x14;									// Schreibe Ladeparameter
	final static byte						setMomoryCycleSleep							= 0x24;									// Schreibe Ladeparameterzusätzliche Parameter 
	final static byte						startProgram										= 0x15;									// Start 
	final static byte						stopProgram											= 0x13;									// Stop 
	final static byte						okStartProgram									= 0x19;									// OK 

	// data type answer description
	byte												version[]												= new byte[11];
	byte												adjustedValues[]								= new byte[5];
	byte												ok															= 0x00;

	// status AkkuMaster C4 device
	final static byte						stateWaiting										= 0x00;					//OOH = warte auf Kommando
	final static byte						stateCharge											= 0x01;					//OIH = Laden
	final static byte						stateDischarge									= 0x02;					//02H = Entladen
	final static byte						stateKeepCharge									= 0x04;					//04H = Erhaltungsladen
	final static byte						stateDeviceActive								= (byte) 0x80;	//80H = Akkumaster aktiv

	// program numbers
	final static byte						programChargeOnly								= 0x01;					//OIH = Nur laden
	final static byte						programDischargeOnly						= 0x02;					//02H = Nur entladen
	final static byte						programDischargeCharge					= 0x03;					//03H = entladen / laden
	final static byte						programChargeDischargeCharge		= 0x04;					//04H = laden / entladen / laden
	final static byte						programDischargeChargeTwoTimes	= 0x05;					//05H = 2 * entladen / laden
	final static byte						programFormUp										= 0x06;					//06H = Formieren
	final static byte						programOverWinter								= 0x07;					//07H = Überwintern
	final static byte						programRefresh									= 0x08;					//08H = Auffrischen
	final static byte						programDiagnostic								= 0x09;					//09H = Akkudiagnose

	// supportzed accu type
	final static byte						typeNC													= 0x00;					//OOH = NC
	final static byte						typeNiMh												= 0x01;					//OIH = NMH
	final static byte						typePB													= 0x02;					//02H = PB

	//	// Zellenzahl:
	//	private byte							numberCells[]										= new byte[1];								//OIH = 1 Zellen . . . OCH = 12 Zellen
	//	// nominale Akku Kapazität:
	//	private byte							capacitiy[]											= new byte[2];								//1 bit=ImAh [400 . . 16000] (2A Variante)
	//	// Entladestrom / Ladestrom:
	//	private byte							current[]												= new byte[2];								//1 bit = 1 mA [40 . . 20001 (2A Variante)
	//
	//	// Wartezeit:
	//	private byte							latencyTime[]										= new byte[2];								//1 bit = 1 Minute
	//
	//	// Aktuelle Entladekapazität des Akkus
	//	private byte							dischargeCapacity[]							= new byte[2];								//1 bit= 1 mAh	
	//	// Aktuelle Ladekapazität des Akkus
	//	private byte							chargeCapacity[]								= new byte[2];								//1 bit= 1 mAh
	//	// Aktuelle Akkuspannung
	//	private byte							voltage[]												= new byte[2];								//1 bit = 0.2 mV
	//
	//	// Entladezeit Stunden
	//	private byte							dischargeTimeHours[]						= new byte[2];								//1 bit = 1 Stunde
	//	// Entladezeit Stunden
	//	private byte							dischargeTimeMinutes[]					= new byte[2];								//1 bit = 1 Minute
	//	// Entladezeit Stunden
	//	private byte							dischargeTimeSeconds[]					= new byte[2];								//1 bit = 1 Sekunde
	//
	//	// Entladezeit Stunden
	//	private byte							chargeTimeHours[]								= new byte[2];								//1 bit = 1 Stunde
	//	// Entladezeit Stunden
	//	private byte							chargeTimeMinutes[]							= new byte[2];								//1 bit = 1 Minute
	//	// Entladezeit Stunden
	//	private byte							chargeTimeSeconds[]							= new byte[2];								//1 bit = 1 Sekunde
	//
	//	// Anzahl Ladezyklen
	//	private byte							numberChargeCycles[]						= new byte[2];								//1 = 1. Ladezyklus
	//	// Verbleibende Wartezeit bis Formieren wiederholt wird
	//	private byte							formUpLatencyTime[]							= new byte[2];								//1 bit = 1 Minute

	public AkkuMasterC4SerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
	}

	/**
	 * OK start program
	 * @throws IOException 
	 * @throws TimeOutException 
	 */
	public synchronized void ok(byte[] channel) throws IOException, TimeOutException {
		byte[] command = new byte[1];
		command[0] = Integer.valueOf(okStartProgram + channel[0]).byteValue();
		this.write(command);
		
		byte[] answer = new byte[2];
		answer = this.read(answer, 2000);
		if (answer[0] != command[0]) throw new IOException(Messages.getString(MessageIds.GDE_MSGE1100));
		if (answer[1] != this.ok) throw new IOException(Messages.getString(MessageIds.GDE_MSGE1101));
	}

	/**
	 * start loaded program
	 * @throws IOException 
	 * @throws TimeOutException 
	 */
	public synchronized void start(byte[] channel) throws IOException, TimeOutException {
		byte[] command = new byte[1];
		command[0] = Integer.valueOf(startProgram + channel[0]).byteValue();
		this.write(command);
		
		byte[] answer = new byte[2];
		answer = this.read(answer, 2000);
		if (answer[0] != command[0]) throw new IOException(Messages.getString(MessageIds.GDE_MSGE1100));
		if (answer[1] != this.ok) throw new IOException(Messages.getString(MessageIds.GDE_MSGE1102));
	}

	/**
	 * stop loaded program
	 * @throws IOException 
	 * @throws TimeOutException 
	 */
	public synchronized void stop(byte[] channel) throws IOException, TimeOutException {
		byte[] command = new byte[1];
		command[0] = Integer.valueOf(stopProgram + channel[0]).byteValue();
		this.write(command);
		
		byte[] answer = new byte[2];
		answer = this.read(answer, 2000);
		if (answer[0] != command[0]) throw new IOException(Messages.getString(MessageIds.GDE_MSGE1100));
		if (answer[1] != this.ok) throw new IOException(Messages.getString(MessageIds.GDE_MSGE1103));
	}

	/**
	 * write new program to given memory number
	 * Komando, Programm-Nummer, Wartezeit bis Formieren wiederholt wird, Akku Typ, Zellenzahl, Nominale Kapazität des Akkus, Entladestrom, Ladestrom
	 * @throws IOException 
	 * @throws TimeOutException 
	 */
	public synchronized void writeNewProgram(byte[] channel, int programNumber, int waitTime_days, int akkuTyp, int cellCount, int akkuCapacity, int dischargeCurrent_mA, int chargeCurrent_mA)
			throws IOException, TimeOutException {
		byte[] command = new byte[11];
		command[0] = Integer.valueOf(setNewProgramm + channel[0]).byteValue();
		command[1] = Integer.valueOf(programNumber).byteValue();
		command[2] = Integer.valueOf(waitTime_days).byteValue();
		command[3] = Integer.valueOf(akkuTyp).byteValue();
		command[4] = Integer.valueOf(cellCount).byteValue();

		command[5] = Integer.valueOf(((akkuCapacity >> 8) & 0xFF)).byteValue();
		command[6] = Integer.valueOf(akkuCapacity & 0xFF).byteValue();

		command[7] = Integer.valueOf(((dischargeCurrent_mA >> 8) & 0xFF)).byteValue();
		command[8] = Integer.valueOf(dischargeCurrent_mA & 0xFF).byteValue();

		command[9] = Integer.valueOf(((chargeCurrent_mA >> 8) & 0xFF)).byteValue();
		command[10] = Integer.valueOf(chargeCurrent_mA & 0xFF).byteValue();
		this.write(command);
		
		byte[] answer = new byte[2];
		answer = this.read(answer, 2000);

		if (answer[0] != command[0]) throw new IOException(Messages.getString(MessageIds.GDE_MSGE1100));
		if (answer[1] != this.ok) throw new IOException(Messages.getString(MessageIds.GDE_MSGE1104));
	}

	/**
	 * set memory number, cycle counter, sleep time
	 * 24H we Kommando: Schreibe Ladeparameterzusätzliche Parameter
	 * YYH byte Speicher-Nummer (OOH = Speicher 0 . . .  07H = Speicher 7)
	 * YYH byte Anzahl Wiederholungen (1 bit = 1 Wiederholung [2 . . 9])
	 * YYYYH word Wartezeit (1 bit = 1 Minute [0 . . 43200])
	 * @throws IOException 
	 * @throws TimeOutException 
	 */
	public synchronized void setMemoryNumberCycleCountSleepTime(byte[] channel, int memoryNumber, int cycleCount, int sleepTime_min) throws IOException, TimeOutException {
		byte[] command = new byte[5];
		command[0] = Integer.valueOf(setMomoryCycleSleep + channel[0]).byteValue();
		command[1] = Integer.valueOf(memoryNumber).byteValue();
		command[2] = Integer.valueOf(cycleCount).byteValue();
		command[3] = Integer.valueOf(((sleepTime_min >> 8) & 0xFF)).byteValue();
		command[4] = Integer.valueOf(sleepTime_min & 0xFF).byteValue();
		this.write(command);
		
		byte[] answer = new byte[2];
		answer = this.read(answer, 2000);
		if (answer[0] != command[0]) throw new IOException(Messages.getString(MessageIds.GDE_MSGE1100));
		if (answer[1] != this.ok) throw new IOException(Messages.getString(MessageIds.GDE_MSGE1105));
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * [0] String Aktueller Prozessname 			"4 ) Laden" = AkkuMaster aktiv Laden
	 * [1] int 		Aktuelle Fehlernummer				"0" = kein Fehler
	 * [2] int		Aktuelle Akkuspannung 			[mV]
	 * [3] int 		Aktueller Prozesssstrom 		[mA] 	(laden/entladen)
	 * [4] int 		Aktuelle Prozesskapazität		[mAh] (laden/entladen)
	 * [5] int 		Errechnete Leistung					[mW]			
	 * [6] int		Errechnete Energie					[mWh]			
	 * [7] int		Prozesszeit									[msec]			
	 * @param channelSignature signature if device has more than one or required by device
	 * @return map containing gathered data - this can individual specified per device
	 * @throws Exception 
	 */
	public synchronized HashMap<String, Object> getData(byte[] channelSignature) throws Exception {
		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}
			HashMap<String, Object> values = new HashMap<String, Object>(7);
			return getConvertedValues(values, getConfiguration(channelSignature), getMeasuredValues(channelSignature));
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			if (isPortOpenedByMe) this.close();
		}
	}

	/**
	 * convert the input string array of configuration and measurements into a hash map 
	 * which contains all information for live view and building record data points
	 * @param values
	 * @param currentConfiguration
	 * @param currentMeasurements
	 * @return map with configuration key value pair
	 */
	public static HashMap<String, Object> getConvertedValues(HashMap<String, Object> values, String[] currentConfiguration, String[] currentMeasurements) {
		boolean isActive = true;
		values.put(AkkuMasterC4SerialPort.PROCESS_NAME, currentConfiguration[0].split(" ")[0]); // AkkuMaster aktiv //$NON-NLS-1$
		values.put(AkkuMasterC4SerialPort.PROCESS_ERROR_NO, Integer.valueOf(currentConfiguration[1].split(" ")[0])); // Aktuelle Fehlernummer //$NON-NLS-1$
		values.put(AkkuMasterC4SerialPort.PROCESS_VOLTAGE, Integer.valueOf(currentMeasurements[2].split(" ")[0])); // Aktuelle Akkuspannung //$NON-NLS-1$

		switch (Integer.valueOf(currentConfiguration[0].split(" ")[0])) { //$NON-NLS-1$
		case 1:
			values.put(AkkuMasterC4SerialPort.PROCESS_NAME, currentConfiguration[0].split(" ")[0] + Messages.getString(MessageIds.GDE_MSGT1126)); //$NON-NLS-1$ 
			values.put(AkkuMasterC4SerialPort.PROCESS_CURRENT, Integer.valueOf(currentConfiguration[7].split(" ")[0])); // eingestellter Ladestrom //$NON-NLS-1$
			values.put(AkkuMasterC4SerialPort.PROCESS_CAPACITY, Integer.valueOf(currentMeasurements[1].split(" ")[0])); // Aktuelle Ladekapazität //$NON-NLS-1$
			break;
		case 2:
			values.put(AkkuMasterC4SerialPort.PROCESS_NAME, currentConfiguration[0].split(" ")[0] + Messages.getString(MessageIds.GDE_MSGT1127)); //$NON-NLS-1$ 
			values.put(AkkuMasterC4SerialPort.PROCESS_CURRENT, Integer.valueOf(currentConfiguration[6].split(" ")[0])); // eingestellter Entladestrom //$NON-NLS-1$
			values.put(AkkuMasterC4SerialPort.PROCESS_CAPACITY, Integer.valueOf(currentMeasurements[0].split(" ")[0])); // Aktuelle Entladekapazität //$NON-NLS-1$
			break;
		case 3:
			values.put(AkkuMasterC4SerialPort.PROCESS_NAME, currentConfiguration[0].split(" ")[0] + Messages.getString(MessageIds.GDE_MSGT1128)); //$NON-NLS-1$ 
			values.put(AkkuMasterC4SerialPort.PROCESS_CAPACITY, Integer.valueOf("0")); //$NON-NLS-1$
			break;
		default:
			isActive = false;
			values.put(AkkuMasterC4SerialPort.PROCESS_NAME, currentConfiguration[0].split(" ")[0] + Messages.getString(MessageIds.GDE_MSGT1129)); //$NON-NLS-1$ 
			values.put(AkkuMasterC4SerialPort.PROCESS_CAPACITY, 0);
			break;
		}

		if (isActive) {
			int voltage = (Integer) values.get(AkkuMasterC4SerialPort.PROCESS_VOLTAGE);
			values.put(AkkuMasterC4SerialPort.PROCESS_POWER, Integer.valueOf(voltage * (Integer) values.get(AkkuMasterC4SerialPort.PROCESS_CURRENT))); // Errechnete Leistung	[mW]
			values.put(AkkuMasterC4SerialPort.PROCESS_ENERGIE, Integer.valueOf(voltage * (Integer) values.get(AkkuMasterC4SerialPort.PROCESS_CAPACITY))); // Errechnete Energie	[mWh]
		}

		return values;
	}

	/**
	 * 	Antwort: Lese eingestellte Werte
	 *	[0] Status des Akku-Master
	 *	[1] Fehlernummer
	 *  [2] Programm-Nummer
	 *  [3] Akku Typ
	 *	[4] Zellenzahl
	 *	[5] Nominale Kapazität des Akkus
	 *	[6] eingestellter Entladestrom
	 *	[7] eingestellter Ladestrom
	 *	[8] eingestellte Wartezeit bis Formieren wiederholt wird
	 * @throws IOException 
	 * @return String[] containing described values
	 * @throws TimeOutException 
	 */
	public synchronized String[] getConfiguration(byte[] channel) throws IOException, TimeOutException {
		byte readConfigOfChannel[] = new byte[1];

		int a = readConfiguration[0];
		int b = channel[0];
		readConfigOfChannel[0] = Integer.valueOf(a + b).byteValue();
		this.write(readConfigOfChannel);
		
		byte[] configuration	= new byte[14];
		configuration = this.read(configuration, 2000);
		if (configuration[0] != readConfigOfChannel[0]) throw new IOException(Messages.getString(MessageIds.GDE_MSGE1100));

		return convertConfigurationAnswer(configuration);
	}

	/**
	 * convert the recceived data bytes into parseable string array
	 * @param configurationDataBytes - the data byte array
	 * @return string array with device configuration data
	 */
	public static String[] convertConfigurationAnswer(byte[] configurationDataBytes) {
		String[] configStrings = new String[9];
		// status
		byte state = (byte) (configurationDataBytes[1] - stateDeviceActive);
		if (state == stateWaiting)
			configStrings[0] = Messages.getString(MessageIds.GDE_MSGT1130); // warte auf Kommando 
		else if (state == stateCharge)
			configStrings[0] = Messages.getString(MessageIds.GDE_MSGT1131);
		else if (state == stateDischarge)
			configStrings[0] = Messages.getString(MessageIds.GDE_MSGT1132);
		else if (state == stateKeepCharge)
			configStrings[0] = Messages.getString(MessageIds.GDE_MSGT1133);
		else
			configStrings[0] = Messages.getString(MessageIds.GDE_MSGT1134); // Pausentimer ? 

		// error number 
		configStrings[1] = Messages.getString(MessageIds.GDE_MSGT1135, new Object[] {configurationDataBytes[2]});

		// program number
		byte program = configurationDataBytes[3];
		if (program == programChargeOnly)
			configStrings[2] = Messages.getString(MessageIds.GDE_MSGT1136); 
		else if (program == programDischargeOnly)
			configStrings[2] = Messages.getString(MessageIds.GDE_MSGT1137);
		else if (program == programDischargeCharge)
			configStrings[2] = Messages.getString(MessageIds.GDE_MSGT1138);
		else if (program == programChargeDischargeCharge)
			configStrings[2] = Messages.getString(MessageIds.GDE_MSGT1139);
		else if (program == programDischargeChargeTwoTimes)
			configStrings[2] = Messages.getString(MessageIds.GDE_MSGT1140); 
		else if (program == programFormUp)
			configStrings[2] = Messages.getString(MessageIds.GDE_MSGT1141); 
		else if (program == programOverWinter)
			configStrings[2] = Messages.getString(MessageIds.GDE_MSGT1142);
		else if (program == programRefresh)
			configStrings[2] = Messages.getString(MessageIds.GDE_MSGT1143);
		else if (program == programDiagnostic)
			configStrings[2] = Messages.getString(MessageIds.GDE_MSGT1144);
		else
			configStrings[2] = Messages.getString(gde.messages.MessageIds.GDE_MSGT0279);

		// Akku-Typ:
		byte accuTyp = configurationDataBytes[4];
		if (accuTyp == typeNC)
			configStrings[3] = Messages.getString(MessageIds.GDE_MSGT1145);
		else if (accuTyp == typeNiMh)
			configStrings[3] = Messages.getString(MessageIds.GDE_MSGT1146); 
		else if (accuTyp == typePB)
			configStrings[3] = Messages.getString(MessageIds.GDE_MSGT1147); 
		else
			configStrings[3] = Messages.getString(gde.messages.MessageIds.GDE_MSGT0279);

		// Zellenzahl:
		configStrings[4] = configurationDataBytes[5] + Messages.getString(MessageIds.GDE_MSGT1148); 

		// nominale Akku Kapazität:
		int accuCapacity = (configurationDataBytes[6] & 0xFF) << 8;
		accuCapacity += (configurationDataBytes[7] & 0xFF) << 0;
		configStrings[5] = accuCapacity + Messages.getString(MessageIds.GDE_MSGT1149); 

		// Entladestrom 
		int current = (configurationDataBytes[8] & 0xFF) << 8;
		current += (configurationDataBytes[9] & 0xFF) << 0;
		configStrings[6] = current + Messages.getString(MessageIds.GDE_MSGT1150); // (2A Variante)

		// Ladestrom
		current = (configurationDataBytes[10] & 0xFF) << 8;
		current += (configurationDataBytes[11] & 0xFF) << 0;
		configStrings[7] = current + Messages.getString(MessageIds.GDE_MSGT1151); // (2A Variante)

		// Wartezeit:
		int latencyTime = (configurationDataBytes[12] & 0xFF) << 8;
		latencyTime += (configurationDataBytes[13] & 0xFF) << 0;
		configStrings[8] = latencyTime + Messages.getString(MessageIds.GDE_MSGT1152); //1 bit = 1 Minute

		return configStrings;
	}

	/**
	 * 	Antwort: Lese eingestellte Werte zus. Parameter
	 *	[0] ausgewählte Speichernummer
	 *	[1] Anzahl einaestellter Wiederholunaen
	 *  [2] tatsächlicher Ladestrom
	 * @throws IOException 
	 * @return String[] containing described values
	 * @throws TimeOutException 
	 */
	public synchronized String[] getAdjustedValues(byte[] channel) throws IOException, TimeOutException {
		String[] adjustments = new String[3];
		byte readAdjustmentsOfChannel[] = new byte[1];

		int a = readAdjustedValues[0];
		int b = channel[0];
		readAdjustmentsOfChannel[0] = Integer.valueOf(a + b).byteValue();
		this.write(readAdjustmentsOfChannel);
		
		this.adjustedValues = this.read(this.adjustedValues, 2000);
		if (this.adjustedValues[0] != readAdjustmentsOfChannel[0]) throw new IOException(Messages.getString(MessageIds.GDE_MSGE1100));

		// ausgewählte Speichernummer
		adjustments[0] = this.adjustedValues[1] + Messages.getString(MessageIds.GDE_MSGT1153);

		// Anzahl einaestellter Wiederholungen
		adjustments[1] = this.adjustedValues[2] + Messages.getString(MessageIds.GDE_MSGT1154);

		// tatsächlicher Ladestrom
		int current = (this.adjustedValues[3] & 0xFF) << 8;
		current += (this.adjustedValues[4] & 0xFF) << 0;
		adjustments[2] = current + Messages.getString(MessageIds.GDE_MSGT1155);

		return adjustments;
	}

	/**
	 * Antwort: Lese gemessene Werte
	 * [0] Aktuelle Entladekapazität des Akkus
	 * [1] Aktuelle Ladekapazität des Akkus
	 * [2] Aktuelle Akkuspannung
	 * [3] Entladezeit Stunden
	 * [4] Entladezeit Minuten
	 * [5] Entladezeit Sekunden 
	 * [6] Ladezeit Stunden
	 * [7] Ladezeit Minuten
	 * [8] Ladezeit Sekunden
	 * [9] Anzahl Ladezyklen
	 * [10] Verbleibende Wartezeit bis Formieren wiederholt wird
	 * @return String[]
	 * @throws IOException 
	 * @throws TimeOutException 
	 */
	public synchronized String[] getMeasuredValues(byte[] channel) throws IOException, TimeOutException {
		byte readValuesOfChannel[] = new byte[1];

		int a = readMeasuredValues[0];
		int b = channel[0];
		readValuesOfChannel[0] = Integer.valueOf(a + b).byteValue();
		this.write(readValuesOfChannel);
		
		byte[]	measuredValues	= new byte[16];
		measuredValues = this.read(measuredValues, 2000);
		if (measuredValues[0] != readValuesOfChannel[0]) throw new IOException(Messages.getString(MessageIds.GDE_MSGE1100));

		return convertMeasurementValues(measuredValues);
	}

	/**
	 * convert measurement data bytes into parseable string array
	 * @param measuredValuesDataBytes
	 * @return sting array with measurement values
	 */
	public static String[] convertMeasurementValues(byte[] measuredValuesDataBytes) {
		String[] measurements = new String[11];
		// Aktuelle Entladekapazität des Akkus
		int current = (measuredValuesDataBytes[1] & 0xFF) << 8;
		current += (measuredValuesDataBytes[2] & 0xFF) << 0;
		measurements[0] = current + Messages.getString(MessageIds.GDE_MSGT1156); 
		//Aktuelle Ladekapazität des Akkus
		current = (measuredValuesDataBytes[3] & 0xFF) << 8;
		current += (measuredValuesDataBytes[4] & 0xFF) << 0;
		measurements[1] = current + Messages.getString(MessageIds.GDE_MSGT1157);
		// Aktuelle Akkuspannung
		int voltage = (measuredValuesDataBytes[5] & 0xFF) << 8;
		voltage += (measuredValuesDataBytes[6] & 0xFF) << 0;
		measurements[2] = voltage * 10 / 2 + Messages.getString(MessageIds.GDE_MSGT1158);

		// Entladezeit Stunden
		measurements[3] = measuredValuesDataBytes[7] + Messages.getString(MessageIds.GDE_MSGT1159);
		// Entladezeit Minuten
		measurements[4] = measuredValuesDataBytes[8] + Messages.getString(MessageIds.GDE_MSGT1160);
		// Entladezeit Sekunden
		measurements[5] = measuredValuesDataBytes[9] + Messages.getString(MessageIds.GDE_MSGT1161);
		// Ladezeit Stunden
		measurements[6] = measuredValuesDataBytes[10] + Messages.getString(MessageIds.GDE_MSGT1162);
		// Ladezeit Minuten
		measurements[7] = measuredValuesDataBytes[11] + Messages.getString(MessageIds.GDE_MSGT1163);
		// Ladezeit Sekunden
		measurements[8] = measuredValuesDataBytes[12] + Messages.getString(MessageIds.GDE_MSGT1164);

		// Anzahl Ladezyklen
		measurements[9] = measuredValuesDataBytes[13] + Messages.getString(MessageIds.GDE_MSGT1165); 

		// Verbleibende Wartezeit bis Formieren wiederholt wird
		int latencyCycleTime = (measuredValuesDataBytes[14] & 0xFF) << 8;
		latencyCycleTime += (measuredValuesDataBytes[15] & 0xFF) << 0;
		measurements[10] = latencyCycleTime + Messages.getString(MessageIds.GDE_MSGT1166);

		return measurements;
	}

	/**
	 * Antwort: Lese Version
	 * [0] Versionsnummer
	 * [1] Datum 
	 * [2] Stromvariante
	 * [3] Frontplattenversion
	 * @return String[] containing described values
	 * @throws Throwable 
	 */
	public synchronized HashMap<String, String> getVersion() throws Throwable {
		HashMap<String, String> result = new HashMap<String, String>(4);
		try {
			if (!this.isConnected()) {
				this.open();
			}

			this.write(readVersion);
			this.version = this.read(this.version, 2000);

			// Versionsnummer der Software
			String versionsNummer = Integer.valueOf(this.version[1]).toString();
			// Versionsindex der Software
			String versionsIndex = Integer.valueOf(this.version[2]).toString();
			result.put(AkkuMasterC4SerialPort.VERSION_NUMBER, versionsNummer + "." + versionsIndex); //$NON-NLS-1$

			// Datum der Software Tag
			String day = Integer.valueOf(this.version[3]).toString();
			// Datum der Software Monat
			String month = Integer.valueOf(this.version[4]).toString();
			int iYear = (this.version[5] & 0xFF) << 8;
			iYear += (this.version[6] & 0xFF) << 0;
			// Datum der Software Jahr
			String year = Integer.valueOf(iYear).toString();
			result.put(AkkuMasterC4SerialPort.VERSION_DATE, day + "." + month + "." + year); //$NON-NLS-1$ //$NON-NLS-2$

			// Stromvariante OOH = 0,5A Variante; 01 H = 2A Variante
			if (this.version[7] == 0x00)
				result.put(AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT, Messages.getString(MessageIds.GDE_MSGT1167));
			else if (this.version[7] == 0x01)
				result.put(AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT, Messages.getString(MessageIds.GDE_MSGT1168));
			else
				result.put(AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT, Messages.getString(MessageIds.GDE_MSGT1169));

			// Frontplattenvariante 0OH = 6 Tasten; 01 H = 4 Tasten (turned around ?)
			if (this.version[8] == 0x00)
				result.put(AkkuMasterC4SerialPort.VERSION_TYPE_FRONT, Messages.getString(MessageIds.GDE_MSGT1170));
			else if (this.version[8] == 0x01)
				result.put(AkkuMasterC4SerialPort.VERSION_TYPE_FRONT, Messages.getString(MessageIds.GDE_MSGT1171));
			else
				result.put(AkkuMasterC4SerialPort.VERSION_TYPE_FRONT, Messages.getString(MessageIds.GDE_MSGT1172)); 

		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			// could not secure all timer threads are stopped
			//if (isPortOpenedByMe) this.close();
		}
		return result;
	}

	/**
	 * log string array
	 * @param array
	 */
	public void print(String[] array) {
		for (String string : array) {
			if (string != null)
				log.log(Level.FINE, string);
			else
				log.log(Level.FINE, "no data"); //$NON-NLS-1$
		}
	}

}
