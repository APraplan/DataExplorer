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
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.device.CheckSumTypes;
import gde.device.IDevice;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.Checksum;
import gde.utils.StringHelper;
import gde.utils.ValueCollector;
import gde.utils.ValueEventCollector;

/**
 * Class to parse comma separated input line from a comma separated textual line which simulates serial data 
 * one data line consist of eg. $GPRMC,162614,A,5230.5900,N,01322.3900,E,10.0,90.0,131006,1.2,E,A*13
 * where $GPRMC describes a minimum NMEA sentence
 * All properties around the textual data in this line has to be specified in DataBlockType (type=TEXT, size number of values, separator=;, ...), refer to DeviceProperties_XY.XSD
 * @author Winfried Brügmann
 */
public class NMEAParser implements IDataParser {
	private static final Logger	log												= Logger.getLogger(NMEAParser.class.getName());
	private final String				$CLASS_NAME								= "NMEAParser."; //$NON-NLS-1$
	private static final String	STRING_SENTENCE_SPLITTER	= " |:";																				//$NON-NLS-1$

	protected int													time_ms;
	protected int													state											= 1; //default value
	protected long												startTimeStamp						= 0;
	protected long												lastTimeStamp							= 0;
	protected int[]												values;
	protected Date												date;
	protected short												timeOffsetUTC							= 0;
	protected short												dstOffset									= Short.MIN_VALUE;
	protected boolean											isAutoDstOffset						= false;
	protected boolean											isNmeaSentenceTime				= false;
	protected int													checkSum;
	protected String											comment;
	protected int													year, month, day;
	protected int													numGSVsentence 						= 1; //check if GSV sentences are in sync
	protected int													numSattelites							= 0;
	protected Vector<String>							missingImpleWarned				= new Vector<String>();
	protected String 											deviceSerialNumber				= GDE.STRING_EMPTY;
	protected String											firmwareVersion           = GDE.STRING_EMPTY;
	protected ValueEventCollector 				airPressures 							= new ValueEventCollector();
	protected boolean											isOffsetSet								= false;
	protected int													initialPressure						= 0;
	protected double											rho												= 0;
	protected ValueCollector							vcAirSpeed40							= new ValueCollector();
	protected ValueCollector							vcAirSpeedDp1							= new ValueCollector();
	protected ValueCollector							vcAirSpeed70							= new ValueCollector();
	
	protected int													recordSetNumberOffset			= 0;
	protected int													timeResetCounter					= 0;
	protected boolean											isTimeResetEnabled				= false;

	protected final int										dataBlockSize;
	protected final String								separator;
	protected final String								leader;
	protected final CheckSumTypes					checkSumType;
	protected final IDevice								device;
	protected final String								deviceName;
	protected int													channelConfigNumber;
	
	int lineNumber = 0;

	public enum NMEA {
		//NMEA sentences
		GPRMC, GPGSA, GPGGA, GPGNS, GPVTG, GPGSV, GPRMB, GPGLL, GPZDA, 
		GNRMC, GNGSA, GNGGA, GNGNS, GNVTG, GNGSV, GNRMB, GNGLL, GNZDA, 
		//additional SM-Modellbau GPS-Logger NMEA sentences
		GPSSETUP, SETUP, SMGPS, SMGPS2, MLINK, UNILOG, KOMMENTAR, COMMENT,
		//additional SM-Modellbau UniLog2 sentences
		UL2SETUP, UL2, 
		//Multiplex FlightRecorder 
		SETUP1, SETUP2, D
	}

	/**
	 * constructor to construct a NMEA parser
	 * @param useLeaderChar , the leading character $
	 * @param useDeviceQualifier , the device qualifier, normally GP
	 * @param useSeparator , the separator character , 
	 * @param useCheckSum , exclusive OR
	 * @param useDataBlockSize , size of the data points to be filled while parsing
	 */
	public NMEAParser(String useLeaderChar, String useSeparator, CheckSumTypes useCheckSum, int useDataBlockSize, IDevice useDevice, int useChannelConfigNumber, short useTimeOffsetUTC) {
		this.separator = useSeparator;
		this.leader = useLeaderChar;
		this.checkSumType = useCheckSum;
		this.dataBlockSize  = useDataBlockSize;
		this.values = new int[Math.abs(this.dataBlockSize)];
		this.device = useDevice;
		this.deviceName = this.device.getName();
		this.channelConfigNumber = useChannelConfigNumber;
		this.timeOffsetUTC = useTimeOffsetUTC;
	}
	
	/**
	 * @return the channel/config number to locate the parsed data
	 */
	public int getChannelConfigNumber(){
		return this.channelConfigNumber;
	}
	
	/**
	 * @return the actual state number
	 */
	public int getState(){
		return this.state;
	}

	/**
	 * @return the recordSetNumberOffset
	 */
	public int getRecordSetNumberOffset() {
		return this.recordSetNumberOffset;
	}

	/**
	 * @param isTimeResetPrepared the isTimeResetPrepared to set
	 */
	public synchronized void setTimeResetEnabled(boolean isTimeResetPrepared) {
		this.isTimeResetEnabled = isTimeResetPrepared;
	}

	/**
	 * parsing method where the incoming vector contains a cycle of NMEA sentences
	 * @param inputLines
	 * @param lastLineNumber
	 * @throws Exception
	 */
	public void parse(Vector<String> inputLines, int lastLineNumber) throws Exception {
		final String $METHOD_NAME = "parse()"; //$NON-NLS-1$
		try {
			int indexRMC = 0;
			for (; indexRMC < inputLines.size(); ++indexRMC) {
				if (inputLines.elementAt(indexRMC).indexOf("RMC", 1) > -1) { //$NON-NLS-1$
					this.lineNumber = lastLineNumber - inputLines.size() + indexRMC + 1;
					parse(inputLines.elementAt(indexRMC), this.lineNumber);
					inputLines.remove(indexRMC);
					break;
				}
			}

			for (int i = 0; i < inputLines.size(); ++i) {
				String inputLine = inputLines.elementAt(i);
				this.lineNumber = lastLineNumber - inputLines.size() + (i<indexRMC ? i : i+1);
				parse(inputLine, this.lineNumber);
			}
		}
		catch (NumberFormatException e) {
			log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "line number " + this.lineNumber + GDE.STRING_MESSAGE_CONCAT + e.getMessage(), e); //$NON-NLS-1$
			//do not re-throw and skip sentence set
		}
		catch (Exception e) {
			log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "line number " + this.lineNumber + GDE.STRING_MESSAGE_CONCAT + e.getMessage(), e); //$NON-NLS-1$
			throw e;
		}
	}

	/**
	 * parse the input line string
	 * @param inputLine
	 * @throws DevicePropertiesInconsistenceException
	 * @throws Exception
	 */
	public void parse(String inputLine, int lineNum) throws DevicePropertiesInconsistenceException, Exception {
		final String $METHOD_NAME = "parse()"; //$NON-NLS-1$
		log.log(Level.FINER, "parser inputLine = " + inputLine); //$NON-NLS-1$
		
		if (!inputLine.startsWith(this.leader)) 
			throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0046, new Object[] { this.leader, lineNum }));

		//add special for MPX FlightRecorder
		if (inputLine.startsWith("$SETUP1;") || inputLine.startsWith("$SETUP2;") || inputLine.startsWith("$D;")) {
			//$SETUP1;Time;;; A:02;;;;;;; A:09; A:10; A:11;
			//$SETUP2;sec ;;;   °C;;;;;;; km/h;    m;    m;
			//$D;0000,95;;;8,9;;;;;;;0,0;-14;0;*33
			inputLine = inputLine.replace(',', '.').replace(";", this.separator);
		}
		else if (!isChecksumOK(inputLine)) {
			return;
		}
		
		if(!inputLine.contains(separator)) 
			throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0047, new String[] {inputLine, separator})); 

		String[] strValues = inputLine.split(this.separator); // {$GPRMC,162614,A,5230.5900,N,01322.3900,E,10.0,90.0,131006,1.2,E,A*13}

		try {
			NMEA sentence = NMEA.valueOf(strValues[0].substring(1));
			switch (sentence) {
			case GPRMC: //Recommended Minimum Sentence C (RMC)
			case GNRMC: //Recommended Minimum Sentence C (RMC)
				parseRMC(strValues);
				break;
			case GPGGA: //Global Positioning System Fix Data (GGA)				
			case GNGGA: //Global Positioning System Fix Data (GGA)			
				parseGGA(strValues);
				break;
			case GPGNS: //Global Navigation System Fix Data			
			case GNGNS: //Global Navigation System Fix Data
				parseGNS(strValues);
				break;
			case GPGSA: //Satellite status (GSA)
			case GNGSA: //Satellite status (GSA)
				parseGSA(strValues);
				break;
			case GPVTG: // Velocity made good (VTG)
			case GNVTG: // Velocity made good (VTG)
				if(!deviceName.startsWith("GPS-Logger")) parseVTG(strValues);
				break;
			case GPGSV: // Satellites in view (GSV)
			case GNGSV: // Satellites in view (GSV)
				parseGSV(strValues);
				break;
			case GPRMB: // Recommended minimum navigation information (RMB)
			case GNRMB: // Recommended minimum navigation information (RMB)
				parseRMB(strValues);
				break;
			case GPGLL: // Geographic Latitude and Longitude (GLL)
			case GNGLL: // Geographic Latitude and Longitude (GLL)
				parseGLL(strValues);
				break;
			case GPZDA: // Data and Time (ZDA)
			case GNZDA: // Data and Time (ZDA)
				parseZDA(strValues);
				break;
			case SMGPS:
				if (this.values.length >=15) parseSMGPS(strValues);
				break;
			case SMGPS2:
				if (this.values.length >=19) parseSMGPS2(strValues);
				break;
			case UNILOG:
				if (this.values.length >=24) parseUNILOG(strValues);
				break;
			case MLINK:
				if (this.values.length >=39) parseMLINK(strValues);
				break;
			case COMMENT:
			case KOMMENTAR:
				//$KOMMENTAR,Extra 300. Kuban Acht. Mit UniLog Daten.*
				//$KOMMENTAR,Trojan. Ein paar liegende Figuren. Volle M-Link Bestückung.*
				this.comment = strValues[1].trim();
				this.comment = this.comment.endsWith(GDE.STRING_STAR) ? this.comment.substring(0, this.comment.length() - 1) : this.comment;

				break;
			case GPSSETUP:// setup SM GPS-Logger firmware >= 1.01
				//$GPSSETUP,2F5A,1,1,2,0,5,0,0,0,0,0,0,0,0,0,0,0,17,12C,96,3E8,1EA,1F4,64,7C,64,7D0,0,0,0,0,0,0,0,0,0,0,0,1,3,4,6,7,2,5,8,9,A,B,0,0,0,0,67,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9A67*09
				this.deviceSerialNumber = String.format("%d", Integer.parseInt(strValues[1].trim(), 16)); //$NON-NLS-1$
				this.timeOffsetUTC = (short) Integer.parseInt(strValues[4].trim(), 16);
				if (log.isLoggable(Level.TIME)) 
					log.log(Level.TIME, String.format("timeOffsetUTC = %d", Integer.parseInt(strValues[4].trim(), 16)));
				if (Integer.parseInt(strValues[11].trim(), 16) == 1) { //automatic summer time
					this.isAutoDstOffset = true;
				}
				this.firmwareVersion = String.format("%.2f", Integer.parseInt(strValues[54].trim(), 16)/100.0); //$NON-NLS-1$

				break;
			case SETUP: // setup SM GPS-Logger firmware 1.00
//							try {
//								byte[] buffer = StringHelper.convert2ByteArray(strValues[1]);
//								this.timeOffsetUTC = (short) ((buffer[7] << 8) + (buffer[6] & 0x00FF));
//								this.timeOffsetUTC = this.timeOffsetUTC > 12 ? 12 : this.timeOffsetUTC < -12 ? -12 : this.timeOffsetUTC;
//							}
//							catch (Exception e) {
//								log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "line number " + this.lineNumber + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
//							}
				break;
			case UL2SETUP: // UniLog2 setup
				this.deviceSerialNumber = String.format("%d", Integer.parseInt(strValues[1].trim(), 16)); //$NON-NLS-1$
				this.firmwareVersion = String.format("%.2f", Integer.parseInt(strValues[2].trim(), 16)/100.0); //$NON-NLS-1$
				//0 = „Temperatur“, 1 = „Millivolt“; 2 =„Speed-Sensor 250 kph“, 3 =̈„Speed-Sensor 450 kph“, 4 =„Temperatur PT1000“
				byte A1 = (byte) (Integer.parseInt(strValues[9].trim(), 16) & 0xFF);
				byte A2 = (byte) (Integer.parseInt(strValues[10].trim(), 16) & 0xFF);
				byte A3 = (byte) (Integer.parseInt(strValues[11].trim(), 16) & 0xFF);
				String tempName = Settings.getInstance().getLocale().getLanguage().equalsIgnoreCase("de") ? "Temperatur" : "Temperature";
				this.device.setMeasurementName(this.channelConfigNumber, 17, A1 == 0x00 ? tempName+" A1" : A1 == 0x02 ? "Speed_250 A1" : A1 == 0x03 ? "Speed_450 A1" : A1 == 0x04 ? "PT1000 A1" : "Millivolt A1");
				this.device.setMeasurementName(this.channelConfigNumber, 18, A2 == 0x00 ? tempName+" A2" : A2 == 0x02 ? "Speed_250 A2" : A2 == 0x03 ? "Speed_450 A2" : A2 == 0x04 ? "PT1000 A2" : "Millivolt A2");
				this.device.setMeasurementName(this.channelConfigNumber, 19, A3 == 0x00 ? tempName+" A3" : A3 == 0x02 ? "Speed_250 A3" : A3 == 0x03 ? "Speed_450 A3" : A3 == 0x04 ? "PT1000 A3" : "Millivolt A3");
				this.device.setMeasurementUnit(this.channelConfigNumber, 17, A1 == 0x00 || A1 == 0x04 ? "°C" : A1 == 0x02 || A1 == 0x03 ? "km/h" : "mV");
				this.device.setMeasurementUnit(this.channelConfigNumber, 18, A2 == 0x00 || A2 == 0x04 ? "°C" : A2 == 0x02 || A2 == 0x03 ? "km/h" : "mV");
				this.device.setMeasurementUnit(this.channelConfigNumber, 19, A3 == 0x00 || A2 == 0x04 ? "°C" : A3 == 0x02 || A3 == 0x03 ? "km/h" : "mV");
				break;
			case UL2:
				if (this.values.length >=25){
					if (this.deviceName.equals("UniLog2")) {
						//0=VoltageRx, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Energy, 6=CellBalance, 7=CellVoltage1, 8=CellVoltage2, 9=CellVoltage3, 
						//10=CellVoltage4, 11=CellVoltage5, 12=CellVoltage6, 13=Revolution, 14=Efficiency, 15=Altitude, 16=Climb, 17=ValueA1, 18=ValueA2, 19=ValueA3,
						//20=ValueCollector, 21=InternTemperature, 22=ServoImpuls In, 23=ServoImpuls Out, 
						//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
						//inOutMapping  000, 001, 002, 003, 004, 005, 006, 007, 008, 009, 010, 011, 012, 013, 014, 015, 016, 017, 018, 019, 020, 021, 022, 023, 024, 025
						int[] in2out = { -1,  -1,  -1,  -1,   1,   2,  15,  16,   4,  13,   0,   3,   5,  17,  18,  19,   7,   8,   9,  10,  11,  12,  20,  21,  22,  23};
						parseUNILOG2(strValues, in2out, 6, true);
					}
					else if (this.deviceName.equals("GPS-Logger")) {
						if (this.channelConfigNumber == 2) {
							//UL2 4:voltage, 5:current, 6:height, 7:climb, 8:power, 9:revolution, 11:capacity, 12:energy, 13:valueA1, 14:valueA2, 15:valueA3, 
							//UL2 16:cellvoltage1, 17:cellvoltage2, 18:cellvoltage3, 19:cellvoltage4, 20:cellvoltage5, 21:cellvoltage6, 23:temperature intern
							//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
							//begin GDE 3.4.9
							//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
							//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
							//CH2-UniLog2
							//Unilog2 17=Voltage, 18=Current, 19=Capacity, 20=Power, 21=Energy, 222=CellBalance, 23=CellVoltage1, 24=CellVoltage2, 25=CellVoltage3, 
							//Unilog2 26=CellVoltage4, 27=CellVoltage5, 28=CellVoltage6, 29=Revolution, 30=ValueA1, 31=ValueA2, 32=ValueA3, 33=InternTemperature
							//M-LINK  34=valAdd00 35=valAdd01 36=valAdd02 37=valAdd03 38=valAdd04 39=valAdd05 40=valAdd06 41=valAdd07 42=valAdd08 43=valAdd09 44=valAdd10 45=valAdd11 46=valAdd12 47=valAdd13 48=valAdd14;
							//inOutMapping  000, 001, 002, 003, 004, 005, 006, 007, 008, 009, 010, 011, 012, 013, 014, 015, 016, 017, 018, 019, 020, 021, 022, 023, 024, 025
						//int[] in2out = { -1,  -1,  -1,  -1,  15,  16,  -1,  -1,  18,  27,  -1,  17,  19,  28,  29,  30,  21,  22,  23,  24,  25,  26,  -1,  31,  -1,  -1};
							int[] in2out = { -1,  -1,  -1,  -1,  17,  18,  -1,  -1,  20,  29,  -1,  19,  21,  30,  31,  32,  23,  24,  25,  26,  27,  28,  -1,  33,  -1,  -1};
							parseUNILOG2(strValues, in2out, 20, false);								
						}
					}
					else if (this.deviceName.equals("GPS-Logger2") || this.deviceName.equals("GPS-Logger3")) {
						if (this.channelConfigNumber == 2) {
							//UL2 4:voltage, 5:current, 6:height, 7:climb, 8:power, 9:revolution, 11:capacity, 12:energy, 13:valueA1, 14:valueA2, 15:valueA3, 
							//UL2 16:cellvoltage1, 17:cellvoltage2, 18:cellvoltage3, 19:cellvoltage4, 20:cellvoltage5, 21:cellvoltage6, 23:temperature intern
							//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
							//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=direction;
							//SMGPS2 	15=AccelerationX 16=AccelerationY 17=AccelerationZ 18=ENL 19=Impuls
							//CH2-UniLog2
							//Unilog2 	20=Voltage, 21=Current, 22=Capacity, 23=Power, 24=Energy, 25=CellBalance, 26=CellVoltage1, 27=CellVoltage2, 28=CellVoltage3, 
							//Unilog2 	29=CellVoltage4, 30=CellVoltage5, 31=CellVoltage6, 32=Revolution, 33=ValueA1, 34=ValueA2, 35=ValueA3, 36=InternTemperature
							//M-LINK  	37=valAdd00 38=valAdd01 39=valAdd02 40=valAdd03 41=valAdd04 42=valAdd05 43=valAdd06 44=valAdd07 45=valAdd08 46=valAdd09 47=valAdd10 48=valAdd11 49=valAdd12 50=valAdd13 51=valAdd14;
							//inOutMapping  000, 001, 002, 003, 004, 005, 006, 007, 008, 009, 010, 011, 012, 013, 014, 015, 016, 017, 018, 019, 020, 021, 022, 023, 024, 025
							//int[] in2out = { -1,  -1,  -1,  -1,  20,  21,  -1,  -1,  23,  32,  -1,  22,  24,  33,  34,  35,  26,  27,  28,  29,  30,  31,  -1,  36,  -1,  -1};
							//begin FW1.26
							//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
							//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
							//SMGPS2 17=AccelerationX 18=AccelerationY 19=AccelerationZ 20=ENL 21=Impulse 22=AirSpeed 23=pressure static 24=pressure TEK 25=climb TEK
							//CH2-UniLog2
							//Unilog2 26=voltage_UL 27=current_UL2 28=capacity_UL2 29=power_UL2 30=energy_UL2 31=balance_UL 32=cellVoltage1 33=cellVolt2_ul 34=cellVolltage3_UL 35=cellVoltage4_UL 36=cellVoltage5_UL 37=cellVoltage6_UL 38=revolution_UL 39=a1_UL 40=a2_UL 41=a3_UL 42=temp_UL;
							//M-LINK 43=valAdd00 44=valAdd01 45=valAdd02 46=valAdd03 47=valAdd04 48=valAdd05 49=valAdd06 50=valAdd07 51=valAdd08 52=valAdd09 53=valAdd10 54=valAdd11 55=valAdd12 56=valAdd13 57=valAdd14;
							//inOutMapping  000, 001, 002, 003, 004, 005, 006, 007, 008, 009, 010, 011, 012, 013, 014, 015, 016, 017, 018, 019, 020, 021, 022, 023, 024, 025
						//int[] in2out = { -1,  -1,  -1,  -1,  20,  21,  -1,  -1,  23,  32,  -1,  22,  24,  33,  34,  35,  26,  27,  28,  29,  30,  31,  -1,  36,  -1,  -1};
							int[] in2out = { -1,  -1,  -1,  -1,  26,  27,  -1,  -1,  29,  38,  -1,  28,  30,  39,  40,  41,  32,  33,  34,  35,  36,  37,  -1,  42,  -1,  -1};
							if (log.isLoggable(Level.FINE)) 
								log.log(Level.FINE, String.format("isNmeaSentenceTime = %b", this.isNmeaSentenceTime));
							parseUNILOG2(strValues, in2out, 31, !this.isNmeaSentenceTime);								
						}
					}
				}
				break;
			case SETUP1:// setup Multiplex FlightRecorder - time and addresses
				//$SETUP1;Time;;; A:02;;;;;;; A:09; A:10; A:11;
				//GPGGA	0=latitude 1=longitude 2=altitudeGPS 3=numSatelites
				for (int i = 4, j = 2; i < this.device.getNumberOfMeasurements(channelConfigNumber); i++, j++) {
					if (j < strValues.length && strValues[j].trim().length() > 0) {
						String name = strValues[j].trim();//$NON-NLS-1$
						this.device.setMeasurementName(channelConfigNumber, i, StringHelper.transfer(new String(name.getBytes("ISO-8859-1"), "UTF-8")));
					}						
					else {
						this.device.setMeasurementName(channelConfigNumber, i, String.format("%d????", i));
					}
				}
				break;
			case SETUP2:// setup Multiplex FlightRecorder
				//$SETUP2;sec ;;;   °C;;;;;;; km/h;    m;    m;
				//GPGGA	0=latitude 1=longitude 2=altitudeGPS 3=numSatelites
				for (int i = 4, j = 2; i < this.device.getNumberOfMeasurements(channelConfigNumber); i++, j++) {
					if (j < strValues.length &&  strValues[j].trim().length() > 0) {
						String unit = strValues[j].trim();
						this.device.setMeasurementUnit(channelConfigNumber, i, StringHelper.transfer(new String(unit.getBytes("ISO-8859-1"), "UTF-8")));
					}
					else {
						this.device.setMeasurementUnit(channelConfigNumber, i, GDE.STRING_EMPTY);
					}
				}
				if (log.isLoggable(Level.FINE)) {
					StringBuilder sb = new StringBuilder();
					String [] names = this.device.getMeasurementNamesReplacements(channelConfigNumber);
					for (int i = 0; i < this.device.getNumberOfMeasurements(channelConfigNumber); i++) {
						sb.append(String.format("\n%s %s", names[i], this.device.getMeasurementUnit(channelConfigNumber, i)));
					}
					log.log(Level.OFF, sb.toString());
				}
				break;
			case D:// data Multiplex FlightRecorder
				//$D;0000,95;;;8,9;;;;;;;0,0;-14;0;*33
				parseMpxD(strValues);
				break;
			}
		}
		catch (Exception e) {
			if (e instanceof IllegalArgumentException && e.getMessage().contains("No enum")) { //$NON-NLS-1$
				if (!missingImpleWarned.contains(strValues[0].substring(1))) {
					log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "line number " + this.lineNumber + " - NMEA sentence = " + strValues[0].substring(1) + " actually not implemented!"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					missingImpleWarned.add(strValues[0].substring(1));
				}
			}
			else {
				throw e;
			}
		}
	}

	/**
	 * check sentence checkSum against last two bytes hex value
	 * checksum is build of exclusive or between $ and *
	 * @param strValues
	 */
	boolean isChecksumOK(String sentence) {
		boolean isOK = true;
		try {
			String hexCheckSum = sentence.trim().substring(sentence.indexOf(GDE.CHAR_STAR) + 1);
			if (hexCheckSum.length() == 2) {
				int tmpCheckSum = Integer.parseInt(hexCheckSum, 16);
				String subSentence = sentence.substring(1, sentence.indexOf(GDE.CHAR_STAR));
				isOK = tmpCheckSum == Checksum.XOR(subSentence.toCharArray());
				if (!isOK) 
					log.logp(Level.WARNING, $CLASS_NAME, "parse()", String.format("line number %d : checkSum 0x%s missmatch 0x%02X in %s!", this.lineNumber, hexCheckSum, Checksum.XOR(subSentence.getBytes()), subSentence)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		catch (Exception e) {
			log.logp(Level.WARNING, $CLASS_NAME, "isChecksumOK()", "line number " + this.lineNumber + GDE.STRING_BLANK + e.getClass().getSimpleName() + GDE.STRING_BLANK + e.getMessage() + " in " + sentence); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return isOK;
	}

	/**
	 * check and correct time offset for GPS-Logger which allow to configure automatically DST adjustment for file time stamps
	 * @param calendar
	 * @param hour
	 * @param minute
	 * @param second
	 * @return corrected calendar
	 */
	private void correctDstOffset(GregorianCalendar calendar) {
		this.dstOffset = (short) (calendar.get(Calendar.DST_OFFSET)/3600000);
		this.timeOffsetUTC += this.dstOffset;
		log.log(Level.TIME, String.format("DST_OFFSET+timeOffsetUTC = %d", this.timeOffsetUTC));
		calendar.add(Calendar.HOUR, this.dstOffset);
	}

	/**
	 * parse the recommended minimum sentence RMC
	 * if a NumberFormatExeption in date and time parsing occurs, skip sentence set
	 * <ul>
	 * $GPRMC,HHMMSS.ss,A,BBBB.BBBB,b,LLLLL.LLLL,l,GG.G,RR.R,DDMMYY,M.M,m,F*PP
	 * $GPRMC,132045.100,A,4752.4904,N,01106.7063,E,1.29,267.55,170910,,,A*60
	 * <li> RMC          Recommended Minimum sentence C						</li>
	 * <li> 123519       Fix taken at 12:35:19 UTC								</li>
	 * <li> A            Status A=active or V=invalid							</li> 		
	 * <li> 4807.038,N   Latitude 48 deg 07.038' N								</li>
	 * <li> 01131.000,E  Longitude 11 deg 31.000' E								</li>
	 * <li> 022.4        Speed over the ground in knots						</li>
	 * <li> 084.4        Track angle in degrees True							</li>
	 * <li> 230394       Date - 23rd of March 1994								</li>
	 * <li> 003.1,W      Magnetic Variation												</li>
	 * <li> *6A          The checksum data, always begins with *	</li>
	 * </ul>
	 * @param strValues
	 */
	void parseRMC(String[] strValues) {
		if (strValues[2].equals("A")) { //$NON-NLS-1$ //$NON-NLS-2$
			if (this.date == null) {
				String strValueDate = strValues[9].trim();
				if (strValueDate.length() < 6) return; //invalid values in RMC sentence
				this.year = Integer.parseInt(strValueDate.substring(4));
				this.year = this.year > 50 ? this.year + 1900 : this.year + 2000;
				this.month = Integer.parseInt(strValueDate.substring(2, 4));
				this.day = Integer.parseInt(strValueDate.substring(0, 2));
			}
			String strValueTime = strValues[1].trim();
			if (strValueTime.length() < 9) return; //invalid values in RMC sentence
			
			int hour = Integer.parseInt(strValueTime.substring(0, 2)) + this.timeOffsetUTC;
			int minute = Integer.parseInt(strValueTime.substring(2, 4));
			int second = Integer.parseInt(strValueTime.substring(4, 6));
			GregorianCalendar calendar = new GregorianCalendar(this.year, this.month - 1, this.day, hour, minute, second);
			this.isNmeaSentenceTime = true;
			if (this.isAutoDstOffset && this.dstOffset == Short.MIN_VALUE) {
				this.correctDstOffset(calendar);
			}
			int indexAfterDot = strValueTime.indexOf(GDE.CHAR_DOT) + 1;
			long timeStamp = calendar.getTimeInMillis() + (indexAfterDot > 0  && strValueTime.length() >= indexAfterDot + 2 ? Long.parseLong(strValueTime.substring(indexAfterDot, indexAfterDot + 2)) * 10 : 0);
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, "RMC " + Integer.parseInt(strValueTime.substring(indexAfterDot, indexAfterDot + 2)) * 10); //$NON-NLS-1$);

			if (this.lastTimeStamp < timeStamp) {
				this.time_ms = (int) (this.lastTimeStamp == 0 ? 0 : this.time_ms + (timeStamp - this.lastTimeStamp));
				this.lastTimeStamp = timeStamp;
				this.date = calendar.getTime();
				if (this.startTimeStamp == 0) this.startTimeStamp = timeStamp;
				
				if (log.isLoggable(Level.FINER)) 
					log.log(Level.FINER, "RMC " + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss.SSS").format(timeStamp)); //$NON-NLS-1$);

				int latitude, longitude, velocity, magneticVariation;
				try {
					latitude = (int) (Double.valueOf(strValues[3].trim())*10000);
					latitude = strValues[4].trim().equalsIgnoreCase("N") ? latitude : -1 * latitude;  //$NON-NLS-1$
				}
				catch (Exception e) {
					latitude = this.values[0];
				}
				try {
					longitude = (int) (Double.valueOf(strValues[5].trim())*10000);
					longitude = strValues[6].trim().equalsIgnoreCase("E") ? longitude : -1 * longitude;  //$NON-NLS-1$
				}
				catch (Exception e) {
					longitude = this.values[1];
				}
				try {
					velocity = (int) (Double.parseDouble(strValues[7].trim()) * 1852.0);
				}
				catch (Exception e) {
					velocity = this.values[7];
				}
				try {
					magneticVariation = strValues[10].trim().length() > 0 ? (int) (Double.parseDouble(strValues[10].trim()) * 1000.0) : this.values[10];
				}
				catch (Exception e) {
					magneticVariation = this.values[8];
				}

				//GPS 
				this.values[0] = latitude;
				this.values[1] = longitude;
				//this.values[2]  = altitudeGPS;
				//this.values[3]  = numSatelites;
				//this.values[4]  = PDOP (dilution of precision) 
				//this.values[5]  = HDOP (horizontal dilution of precision) 
				//this.values[6]  = VDOP (vertical dilution of precision)
				this.values[7] = velocity;
				this.values[8] = magneticVariation; // SM GPS-Logger -> altitudeRel;
			}
		}
	}

	/**
	 * parse the Global Positioning System Fix Data (GGA)
	 * if a NumberFormatExeption in time parsing occurs, skip sentence set
	 * <ul>
	 * $GPGGA,HHMMSS.ss,BBBB.BBBB,b,LLLLL.LLLL,l,Q,NN,D.D,H.H,h,G.G,g,A.A,RRRR*PP
	 * $GPGGA,132045.100,4752.4904,N,01106.7063,E,1,10,0.89,543.4,M,47.8,M,,*68
	 * <li>
	 * <li> GGA          Global Positioning System Fix Data
	 * <li> 123519       Fix taken at 12:35:19 UTC
	 * <li> 4807.038,N   Latitude 48 deg 07.038' N
	 * <li> 01131.000,E  Longitude 11 deg 31.000' E
	 * <li> 1            Fix quality: 0 = invalid
	 * <li>                           1 = GPS fix (SPS)
	 * <li>                           2 = DGPS fix
	 * <li>                           3 = PPS fix
	 * <li> 													4 = Real Time Kinematic
	 * <li> 													5 = Float RTK
	 * <li> 													6 = estimated (dead reckoning) (2.3 feature)
	 * <li> 													7 = Manual input mode
	 * <li> 													8 = Simulation mode
	 * <li> 08           Number of satellites being tracked
	 * <li> 0.9          Horizontal dilution of position
	 * <li> 545.4,M      Altitude, Meters, above mean sea level
	 * <li> 46.9,M       Height of geoid (mean sea level) above WGS84
	 * <li>                  ellipsoid
	 * <li> (empty field) time in seconds since last DGPS update
	 * <li> (empty field) DGPS station ID number
	 * <li> *47          the checksum data, always begins with *
	 * </ul>
	 * @param strValues
	 */
	void parseGGA(String[] strValues) {
		if (strValues[6].trim().length() == 0 || Integer.parseInt(strValues[6].trim()) > 0) { //fix quality 
			String strValueTime = strValues[1].trim();
			long timeStamp = 0l;
			if (strValueTime.length() < 9) return;
			
			int hour = Integer.parseInt(strValueTime.substring(0, 2)) + this.timeOffsetUTC;
			int minute = Integer.parseInt(strValueTime.substring(2, 4));
			int second = Integer.parseInt(strValueTime.substring(4, 6));
			GregorianCalendar calendar = new GregorianCalendar(this.year, this.month - 1, this.day, hour, minute, second);
			this.isNmeaSentenceTime = true;
			if (this.isAutoDstOffset && this.dstOffset == Short.MIN_VALUE) {
				this.correctDstOffset(calendar);
			}
			int indexAfterDot = strValueTime.indexOf(GDE.CHAR_DOT) + 1;
			timeStamp = calendar.getTimeInMillis() + (indexAfterDot > 0  && strValueTime.length() >= indexAfterDot + 2 ? Integer.parseInt(strValueTime.substring(indexAfterDot, indexAfterDot + 2)) * 10 : 0);
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, "GGA " + Integer.parseInt(strValueTime.substring(indexAfterDot, indexAfterDot + 2)) * 10); //$NON-NLS-1$);
			
			if (log.isLoggable(Level.FINE)) 
				log.log(Level.FINE, "GGA " + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss.SSS").format(timeStamp)); //$NON-NLS-1$);
			

			int latitude, longitude, numSatelites, altitudeGPS;
			if (this.lastTimeStamp == timeStamp) { // validate sentence  depends to same sentence set
				try {
					if (this.values[0] == 0) {
						latitude = (int) (Double.valueOf(strValues[2].trim())*10000);
						latitude = strValues[3].trim().equalsIgnoreCase("N") ? latitude : -1 * latitude; //$NON-NLS-1$
					} 
					else
						latitude = this.values[0];
				}
				catch (Exception e) {
					latitude = this.values[0];
				}
				try {
					if (this.values[1] == 0) {
						longitude = (int) (Double.valueOf(strValues[4].trim())*10000);
						longitude = strValues[5].trim().equalsIgnoreCase("E") ? longitude : -1 * longitude; //$NON-NLS-1$
					} 
					else
						longitude = this.values[1];
				}
				catch (Exception e) {
					longitude = this.values[1];
				}
				try {
					numSatelites = Integer.parseInt(strValues[7].trim()) * 1000;
				}
				catch (Exception e) {
					numSatelites = this.values[3];
				}
				try {
					altitudeGPS = (int) (Double.parseDouble(strValues[9].trim()) * 1000.0);
				}
				catch (Exception e) {
					altitudeGPS = this.values[2];
				}
			}
			else {
				try {
						latitude = (int) (Double.valueOf(strValues[2].trim())*10000);
						latitude = strValues[3].trim().equalsIgnoreCase("N") ? latitude : -1 * latitude; //$NON-NLS-1$
				}
				catch (Exception e) {
					latitude = this.values[0];
				}
				try {
						longitude = (int) (Double.valueOf(strValues[4].trim())*10000);
						longitude = strValues[5].trim().equalsIgnoreCase("E") ? longitude : -1 * longitude; //$NON-NLS-1$
				}
				catch (Exception e) {
					longitude = this.values[1];
				}
				try {
					numSatelites = Integer.parseInt(strValues[7].trim()) * 1000;
				}
				catch (Exception e) {
					numSatelites = this.values[3];
				}
				try {
					altitudeGPS = (int) (Double.parseDouble(strValues[9].trim()) * 1000.0);
				}
				catch (Exception e) {
					altitudeGPS = this.values[2];
				}
			}

			//GPS 
			this.values[0] = latitude;
			this.values[1] = longitude;
			this.values[2] = altitudeGPS;
			this.values[3] = numSatelites;
			//this.values[4]  = PDOP (dilution of precision) 
			//this.values[5]  = HDOP (horizontal dilution of precision) 
			//this.values[6]  = VDOP (vertical dilution of precision)
			//this.values[7]  = velocity;
			//this.values[8]  = magneticVariation; // SM GPS-Logger -> altitudeRel;
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, String.format("lat %9.6f, long %9.6f, alt %d, sats %d", latitude/1000000., longitude/1000000., altitudeGPS/10000, numSatelites/1000));
		}
	}

	/**
	 * parse the Global Navigation System Fix Data (GNS), use variable number of satellites systems
	 * <ul>
	 * $GNGNS,HHMMSS.ss,BBBB.BBBB,b,LLLLL.LLLL,l,c--c,NN,D.D,H.H,h,G.G,g,A.A,RRRR*PP
	 * $GPGGA,132045.100,4752.4904,N,01106.7063,E,1,10,0.89,543.4,M,47.8,M,,*68
	 * <li>
	 * <li> GNS          Global Navigation System Fix Data
	 * <li> 123519       Fix taken at 12:35:19 UTC
	 * <li> 4807.038,N   Latitude 48 deg 07.038' N
	 * <li> 01131.000,E  Longitude 11 deg 31.000' E
	 * <li> ADDN         Mode indicator with variable  length. Characters  currently  defined GPS, GLONASS, Galileo other satellite systems in the future.  
	 * <li>  						 N = No fix
	 * <li>                           A = Autonomous mode
	 * <li>                           D = Differential mode
	 * <li>                           P = Precise mode
	 * <li> 													R = Real Time Kinematic
	 * <li> 													F = Float RTK
	 * <li> 													E = Estimated (dead reckoning) mode
	 * <li> 													M = Manual input mode
	 * <li> 													S = Simulator mode
	 * <li> 08           Number of satellites in use
	 * <li> 0.9          Horizontal dilution of position
	 * <li> 545.4,M      Altitude, Meters, above mean sea level
	 * <li> 46.9,M       Geoidal Separation: the difference between the WGS-84 earth ellipsoid surface and mean-sea-level (geoid) surface; “-” = mean-sea-level surface below ellipsoid.
	 * <li> A:A					 Age of differential data and Differential Reference Station ID
	 * <li> RRRR  			 DGPS station ID number
	 * <li> *47          the checksum data, always begins with *
	 * </ul>
	 * @param strValues
	 */
	void parseGNS(String[] strValues) {
		String modeIndicator = strValues[6].trim();
		if (modeIndicator.length() == 0 || modeIndicator.contains("A") || modeIndicator.contains("D") || modeIndicator.contains("P")) { //fix quality 
			String strValueTime = strValues[1].trim();
			long timeStamp = 0l;
			if (strValueTime.length() < 9) return;
			
			int hour = Integer.parseInt(strValueTime.substring(0, 2)) + this.timeOffsetUTC;
			int minute = Integer.parseInt(strValueTime.substring(2, 4));
			int second = Integer.parseInt(strValueTime.substring(4, 6));
			GregorianCalendar calendar = new GregorianCalendar(this.year, this.month - 1, this.day, hour, minute, second);
			this.isNmeaSentenceTime = true;
			if (this.isAutoDstOffset && this.dstOffset == Short.MIN_VALUE) {
				this.correctDstOffset(calendar);
			}
			int indexAfterDot = strValueTime.indexOf(GDE.CHAR_DOT) + 1;
			timeStamp = calendar.getTimeInMillis() + (indexAfterDot > 0  && strValueTime.length() >= indexAfterDot + 2 ? Integer.parseInt(strValueTime.substring(indexAfterDot, indexAfterDot + 2)) * 10 : 0);
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, "GNS " + Integer.parseInt(strValueTime.substring(indexAfterDot, indexAfterDot + 2)) * 10); //$NON-NLS-1$);
			
			if (log.isLoggable(Level.FINE)) 
				log.log(Level.FINE, "GNS " + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss.SSS").format(timeStamp)); //$NON-NLS-1$);
			

			int latitude, longitude, numSatelites, altitudeGPS;
			if (this.lastTimeStamp == timeStamp) { // validate sentence  depends to same sentence set
				try {
					if (this.values[0] == 0) {
						latitude = (int) (Double.valueOf(strValues[2].trim())*10000);
						latitude = strValues[3].trim().equalsIgnoreCase("N") ? latitude : -1 * latitude; //$NON-NLS-1$
					} 
					else
						latitude = this.values[0];
				}
				catch (Exception e) {
					latitude = this.values[0];
				}
				try {
					if (this.values[1] == 0) {
						longitude = (int) (Double.valueOf(strValues[4].trim())*10000);
						longitude = strValues[5].trim().equalsIgnoreCase("E") ? longitude : -1 * longitude; //$NON-NLS-1$
					} 
					else
						longitude = this.values[1];
				}
				catch (Exception e) {
					longitude = this.values[1];
				}
				try {
					numSatelites = Integer.parseInt(strValues[7].trim()) * 1000;
				}
				catch (Exception e) {
					numSatelites = this.values[3];
				}
				try {
					altitudeGPS = (int) (Double.parseDouble(strValues[9].trim()) * 1000.0);
				}
				catch (Exception e) {
					altitudeGPS = this.values[2];
				}
			}
			else {
				try {
						latitude = (int) (Double.valueOf(strValues[2].trim())*10000);
						latitude = strValues[3].trim().equalsIgnoreCase("N") ? latitude : -1 * latitude; //$NON-NLS-1$
				}
				catch (Exception e) {
					latitude = this.values[0];
				}
				try {
						longitude = (int) (Double.valueOf(strValues[4].trim())*10000);
						longitude = strValues[5].trim().equalsIgnoreCase("E") ? longitude : -1 * longitude; //$NON-NLS-1$
				}
				catch (Exception e) {
					longitude = this.values[1];
				}
				try {
					numSatelites = Integer.parseInt(strValues[7].trim()) * 1000;
				}
				catch (Exception e) {
					numSatelites = this.values[3];
				}
				try {
					altitudeGPS = (int) (Double.parseDouble(strValues[9].trim()) * 1000.0);
				}
				catch (Exception e) {
					altitudeGPS = this.values[2];
				}
			}

			//GPS 
			this.values[0] = latitude;
			this.values[1] = longitude;
			this.values[2] = altitudeGPS;
			this.values[3] = numSatelites;
			//this.values[4]  = PDOP (dilution of precision) 
			//this.values[5]  = HDOP (horizontal dilution of precision) 
			//this.values[6]  = VDOP (vertical dilution of precision)
			//this.values[7]  = velocity;
			//this.values[8]  = magneticVariation; // SM GPS-Logger -> altitudeRel;
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, String.format("lat %9.6f, long %9.6f, alt %d, sats %d", latitude/1000000., longitude/1000000., altitudeGPS/10000, numSatelites/1000));
		}
	}

	/**
	 * parse the satellites status sentence GSA
	 * <ul>
	 * $GPGSA,A,3,31,30,23,25,05,02,29,21,10,12,,,1.20,0.89,0.81*03
	 * <li> GSA      Satellite status
	 * <li> A        Auto selection of 2D or 3D fix (M = manual) 
	 * <li> 3        3D fix - values include: 1 = no fix
	 * <li>                                   2 = 2D fix
	 * <li>                                   3 = 3D fix
	 * <li> 04,05... PRNs of satellites used for fix (space for 12) 
	 * <li> 2.5      PDOP (dilution of precision) 
	 * <li> 1.3      Horizontal dilution of precision (HDOP) 
	 * <li> 2.1      Vertical dilution of precision (VDOP)
	 * <li> *39      the checksum data, always begins with *
	 * </ul>
	 * @param strValues
	 */
	void parseGSA(String[] strValues) {
		if (strValues[1].equals("A") || strValues[1].equals("M")) { //$NON-NLS-1$ //$NON-NLS-2$
			int PDOP = this.values[4], HDOP = this.values[5], VDOP = this.values[6];
			try {
				PDOP = (int) (Double.parseDouble(strValues[strValues.length - 3].trim()) * 1000.0);
			}
			catch (Exception e) {
				//ignore and leave value unchanged
			}
			try {
				HDOP = (int) (Double.parseDouble(strValues[strValues.length - 2].trim()) * 1000.0);
			}
			catch (Exception e) {
				//ignore and leave value unchanged
			}
			try {
				String value = strValues[strValues.length - 1].trim();
				value = value.contains(GDE.STRING_STAR) ? value.substring(0, value.indexOf(GDE.CHAR_STAR)) : value;
				VDOP = (int) (Double.parseDouble(value) * 1000.0);
			}
			catch (Exception e) {
				//ignore and leave value unchanged
			}

			//GPS 
			//this.values[0]  = latitude;
			//this.values[1]  = longitude;
			//this.values[2]  = altitudeGPS;
			//this.values[3]  = numSatelites;
			this.values[4] = PDOP; // (dilution of precision) 
			this.values[5] = HDOP; // (horizontal dilution of precision) 
			this.values[6] = VDOP; // (vertical dilution of precision)
			//this.values[7]  = velocity;
			//this.values[8]  = magneticVariation; // SM GPS-Logger -> altitudeRel;
		}
	}

	/**
	 * parse the GSV - Satellites in view
	 * <ul>
	 * $GPGSV,2,1,08,01,40,083,46,02,17,308,41,12,07,344,39,14,22,228,45*75
	 * <li> GSV          Satellites in view
	 * <li> 2            Number of sentences for full data
	 * <li> 1            sentence 1 of 2
	 * <li> 08           Number of satellites in view
	 * <li> 01           Satellite PRN number
	 * <li> 40           Elevation, degrees
	 * <li> 083          Azimuth, degrees
	 * <li> 46           SNR - higher is better   
	 * <li> 			for up to 4 satellites per sentence
	 * <li> *75      the checksum data, always begins with *
	 * </ul>
	 * @param strValues
	 */
	void parseGSV(String[] strValues) {
		if (!(GDE.STRING_EMPTY+numGSVsentence).equals(strValues[1]) || (GDE.STRING_EMPTY+numGSVsentence).equals(strValues[2])) { 
			int numSentence = 1;
			int actualSentence = 0;
			int actualNumSattelites = 0;
			try {
				numSentence = Integer.parseInt(strValues[1]);
				actualSentence = Integer.parseInt(strValues[2]);
				actualNumSattelites = Integer.parseInt(strValues[3]) * 1000;
				if (numSentence >= numGSVsentence && actualSentence <= numGSVsentence && (numSattelites == 0 || numSattelites == actualNumSattelites)){ // in synch
					numGSVsentence = actualSentence == numSentence ? 1 : numSentence; // reset after reading last sentence of set
					numSattelites = actualNumSattelites;
					this.values[3]  = actualNumSattelites;
				}
				else {
					numGSVsentence = 1;
					log.log(Level.WARNING, "GSV sentences out of sync, skip and reset!"); //$NON-NLS-1$
					return;
				}
			}
			catch (Exception e) {
				//ignore and leave value unchanged
			}
			//passed sentence sync check
			try {
				for (int i = 0; i < 4 && (7 + 4*i) < strValues.length; i++) { //up to 4 satellites per sentence
					int numSattelite = Integer.parseInt(strValues[4 + 4*i]);
					int elevationDegrees = Integer.parseInt(strValues[5 + 4*i]);
					int azimuthDegrees = Integer.parseInt(strValues[6 + 4*i]);
					int signalNoiseRation;
					if (strValues[7 + 4*i].contains(GDE.STRING_STAR)) {
						String tmpValue = strValues[7 + 4*i].substring(0, strValues[7 + 4*i].indexOf(GDE.CHAR_STAR));
						signalNoiseRation = tmpValue.length() > 0 ? Integer.parseInt(tmpValue) : 0;
					}
					else 
						signalNoiseRation = Integer.parseInt(strValues[7 + 4*i]);
					
					if (log.isLoggable(Level.FINE)) 
						log.log(Level.FINE, "numSattelite = " + numSattelite + " elevation = " + elevationDegrees + " azimuth = " + azimuthDegrees + " signalNoiseRation = " + signalNoiseRation); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
			}
			catch (Exception e) {
				//ignore and leave value unchanged
			}
			

			//GPS 
			//this.values[0]  = latitude;
			//this.values[1]  = longitude;
			//this.values[2]  = altitudeGPS;
			//this.values[3]  = numSatelites;
			//this.values[4] = PDOP; // (dilution of precision) 
			//this.values[5] = HDOP; // (horizontal dilution of precision) 
			//this.values[6] = VDOP; // (vertical dilution of precision)
			//this.values[7]  = velocity;

			//this.values[8]  = altitudeRel;
			//this.values[9]  = climb;
			//this.values[10] = magneticVariation; // SM GPS-Logger -> voltageRx;
			//this.values[11] = distanceTotal;
			//this.values[12] = distanceStart;
			//this.values[13] = directionStart;
			//this.values[14] = direction/azimuth/track;
		}
	}

	/**
	 * parse the velocity made good sentence VTG
	 * <ul>
	 * $GPVTG,054.7,T,034.4,M,005.5,N,010.2,K*48
	 * $GPVTG,267.55,T,,M,1.29,N,2.38,K,A*3D
	 * <li>         VTG          Track made good and ground speed
	 * <li>         054.7,T      True track made good (degrees)
	 * <li>         034.4,M      Magnetic track made good
	 * <li>         005.5,N      Ground speed, knots
	 * <li>         010.2,K      Ground speed, Kilometers per hour
	 * <li>         *48          Checksum
	 * </ul>
	 * @param strValues
	 */
	void parseVTG(String[] strValues) {
			int velocity;
			try {
				velocity = (int) (Double.parseDouble(strValues[7].trim()) * 1000.0);
			}
			catch (Exception e) {
				try {
					velocity = (int) (Double.parseDouble(strValues[5].trim()) * 1852.0);
				}
				catch (Exception e1) {
					velocity = this.values[7];
				}
			}

			//GPS 
			//this.values[0]  = latitude;
			//this.values[1]  = longitude;
			//this.values[2]  = altitudeGPS;
			//this.values[3]  = numSatelites;
			//this.values[4]  = PDOP (dilution of precision) 
			//this.values[5]  = HDOP (horizontal dilution of precision) 
			//this.values[6]  = VDOP (vertical dilution of precision)
			this.values[7] = velocity;
			//this.values[8]  = magneticVariation; // SM GPS-Logger -> altitudeRel;
	}

	/**
	 	 * parse the GLL - Geographic Latitude and Longitude
	 	 * <ul>
	 	 * $GPGLL,4916.45,N,12311.12,W,225444,A,*1D
	 	 * <li> GLL          Geographic position, Latitude and Longitude
	 	 * <li> 4916.46,N    Latitude 49 deg. 16.45 min. North
	 	 * <li> 12311.12,W   Longitude 123 deg. 11.12 min. West
	 	 * <li> 225444       Fix taken at 22:54:44 UTC
	 	 * <li> A            Data Active or V (void)
	 	 * <li> *iD          checksum data
	 	 * </ul>
	 	 * @param strValues
	 	 */
	void parseGLL(String[] strValues) {
		if (strValues[6].equals("A")) { //$NON-NLS-1$ //$NON-NLS-2$
			if (this.date == null) {
				Calendar calendar = new GregorianCalendar();
				this.year = calendar.get(Calendar.YEAR);
				this.month = calendar.get(Calendar.MONTH)+1;
				this.day = calendar.get(Calendar.DATE);
			}
			String strValueTime = strValues[5].trim();
			if (strValueTime.length() < 9) return;
			
			int hour = Integer.parseInt(strValueTime.substring(0, 2)) + this.timeOffsetUTC;
			int minute = Integer.parseInt(strValueTime.substring(2, 4));
			int second = Integer.parseInt(strValueTime.substring(4, 6));
			GregorianCalendar calendar = new GregorianCalendar(this.year, this.month - 1, this.day, hour, minute, second);
			this.isNmeaSentenceTime = true;
			if (this.isAutoDstOffset && this.dstOffset == Short.MIN_VALUE) {
				this.correctDstOffset(calendar);
			}
			int indexAfterDot = strValueTime.indexOf(GDE.CHAR_DOT) + 1;
			long timeStamp = calendar.getTimeInMillis() + (indexAfterDot > 0  && strValueTime.length() >= indexAfterDot + 2 ? Long.parseLong(strValueTime.substring(indexAfterDot, indexAfterDot + 2)) * 10 : 0);
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, "GLL " + Integer.parseInt(strValueTime.substring(indexAfterDot, indexAfterDot + 2)) * 10); //$NON-NLS-1$);
			
			if (log.isLoggable(Level.FINE)) 
				log.log(Level.FINE, "GLL " + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss.SSS").format(timeStamp)); //$NON-NLS-1$);

			if (this.lastTimeStamp < timeStamp) {
				this.time_ms = (int) (this.lastTimeStamp == 0 ? 0 : this.time_ms + (timeStamp - this.lastTimeStamp));
				this.lastTimeStamp = timeStamp;
				this.date = calendar.getTime();
				log.log(Level.FINE, new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(this.date)); //$NON-NLS-1$);
				if (this.startTimeStamp == 0) this.startTimeStamp = timeStamp;

				int latitude, longitude;
				try {
					if (this.values[0] == 0) {
						latitude = (int) (Double.valueOf(strValues[2].trim())*10000);
						latitude = strValues[3].trim().equalsIgnoreCase("N") ? latitude : -1 * latitude; //$NON-NLS-1$
					} 
					else
						latitude = this.values[0];
				}
				catch (Exception e) {
					latitude = this.values[0];
				}
				try {
					if (this.values[1] == 0) {
						longitude = (int) (Double.valueOf(strValues[4].trim())*10000);
						longitude = strValues[5].trim().equalsIgnoreCase("E") ? longitude : -1 * longitude; //$NON-NLS-1$
					} 
					else
						longitude = this.values[1];
				}
				catch (Exception e) {
					longitude = this.values[1];
				}

				//GPS 
				this.values[0] = latitude;
				this.values[1] = longitude;
				//this.values[2] = altitudeGPS;
				//this.values[3] = numSatelites;
				//this.values[4] = PDOP; // (dilution of precision) 
				//this.values[5] = HDOP; // (horizontal dilution of precision) 
				//this.values[6] = VDOP; // (vertical dilution of precision)
				//this.values[7] = velocity;
				//this.values[8] = magneticVariation; // SM GPS-Logger -> altitudeRel;
			}
		}
	}

	/**
	 * parse ZDA - Data and Time
	 * $GPZDA,hhmmss.ss,dd,mm,yyyy,xx,yy*CC
	 * $GPZDA,201530.00,04,07,2002,00,00*60
	 * <ul>
	 * <li>         ZDA					Data and Time
	 * <li>         hhmmss    	HrMinSec(UTC)
	 * <li>         dd,mm,yyy 	Day,Month,Year
	 * <li>         xx        	local zone hours -13..13
	 * <li>         yy        	local zone minutes 0..59    	 								
	 * <li>         *CC       	checksum
	 * </ul>
	 * @param strValues
	 */
	void parseZDA(String[] strValues) {
		if (this.date == null) {
			String strValueDate = strValues[9].trim();
			this.year = Integer.parseInt(strValueDate.substring(4));
			this.year = this.year > 50 ? this.year + 1900 : this.year + 2000;
			this.month = Integer.parseInt(strValueDate.substring(2, 4));
			this.day = Integer.parseInt(strValueDate.substring(0, 2));
		}
		String strValueTime = strValues[1].trim();
		if (strValueTime.length() < 9) return;
		
		int hour = Integer.parseInt(strValueTime.substring(0, 2)) + this.timeOffsetUTC;
		int minute = Integer.parseInt(strValueTime.substring(2, 4));
		int second = Integer.parseInt(strValueTime.substring(4, 6));
		GregorianCalendar calendar = new GregorianCalendar(this.year, this.month - 1, this.day, hour, minute, second);
		this.isNmeaSentenceTime = true;
		if (this.isAutoDstOffset && this.dstOffset == Short.MIN_VALUE) {
			this.correctDstOffset(calendar);
		}
		int indexAfterDot = strValueTime.indexOf(GDE.CHAR_DOT) + 1;
		long timeStamp = calendar.getTimeInMillis() + (indexAfterDot > 0  && strValueTime.length() >= indexAfterDot + 2 ? Long.parseLong(strValueTime.substring(indexAfterDot, indexAfterDot + 2)) * 10 : 0);
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "ZDA " + Integer.parseInt(strValueTime.substring(indexAfterDot, indexAfterDot + 2)) * 10); //$NON-NLS-1$);
		
		if (log.isLoggable(Level.FINE)) 
			log.log(Level.FINE, "ZDA " + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss.SSS").format(timeStamp)); //$NON-NLS-1$);

		if (this.lastTimeStamp < timeStamp) {
			this.time_ms = (int) (this.lastTimeStamp == 0 ? 0 : this.time_ms + (timeStamp - this.lastTimeStamp));
			this.lastTimeStamp = timeStamp;
			this.date = calendar.getTime();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(this.date)); //$NON-NLS-1$);
			if (this.startTimeStamp == 0) this.startTimeStamp = timeStamp;
		}
	}

	/**
	 * parse RMB - Recommended minimum navigation information
	 * $GPZDA,hhmmss.ss,dd,mm,yyyy,xx,yy*CC
	 * $GPRMB,A,0.66,L,003,004,4917.24,N,12309.57,W,001.3,052.5,000.5,V*20
	 * <ul>
	 * <li>         RMB          Recommended minimum navigation information
	 * <li>         A            Data status A = OK, V = Void (warning)
	 * <li>         0.66,L       Cross-track error (nautical miles, 9.99 max), steer Left to correct (or R = right)
	 * <li>         003          Origin waypoint ID
	 * <li>         004          Destination waypoint ID  	 	
	 * <li>         4917.24,N    Destination waypoint latitude 49 deg. 17.24 min. N
	 * <li>         12309.57,W   Destination waypoint longitude 123 deg. 09.57 min. W		
	 * <li>         001.3        Range to destination, nautical miles (999.9 max)	    
	 * <li>         052.5        True bearing to destination		
	 * <li>         000.5        Velocity towards destination, knots	
	 * <li>         V            Arrival alarm  A = arrived, V = not arrived	
	 * <li>         *20          checksum
	 * </ul>
	 * @param strValues
	 */
	void parseRMB(String[] strValues) {
		if (strValues[1].equals("A")) { //$NON-NLS-1$
			int velocity;
			try {
				velocity = this.values[7] == 0 ? (int) (Double.parseDouble(strValues[12].trim()) * 1852.0) : this.values[7];
			}
			catch (Exception e) {
				velocity = this.values[7];
			}

			//GPS 
			//this.values[0]  = latitude;
			//this.values[1]  = longitude;
			//this.values[2]  = altitudeGPS;
			//this.values[3]  = numSatelites;
			//this.values[4] = PDOP; // (dilution of precision) 
			//this.values[5] = HDOP; // (horizontal dilution of precision) 
			//this.values[6] = VDOP; // (vertical dilution of precision)
			this.values[7]  = velocity;
			//this.values[8]  = magneticVariation; // SM GPS-Logger -> altitudeRel;
		}
	}

	/**
	 * parse SM GPS sentence
	 * $SMGPS,-1.0 m,+0.21 m/s,4.85 VRx,0.00 km,0 m,270.0 °,1:-- (  0.0 km/h)*CE
	 * 1: Höhe relativ
	 * 2: Steigen
	 * 3: Rx Spannung
	 * 4: Strecke (gesamt zurückgelegt)
	 * 5: Entfernung vom Startpunkt Luftlinie
	 * 6: Richtung vom Startpunkt
	 * 7: Gleitzahl, in der Form „1:xx (yy.y km/h)“, dabei ist 1:xx die Gleitzahl, kein gültiger Wert ist „1:--“
	 * 8: SpeedGleitzahl, in der Form „1:xx (yy.y km/h)“
	 * 9: Höhe GPS, in der Form 498.3 m
	 * @param strValues
	 */
	void parseSMGPS(String[] strValues) {
		for (int i = 0; i < strValues.length - 1 && i < 8; i++) {
			try {
				String[] tmpValues = strValues[i + 1].trim().split(NMEAParser.STRING_SENTENCE_SPLITTER);
				if (i < 6) {
					this.values[8 + i] = (int) (Double.parseDouble(tmpValues[0]) * 1000.0);
					if (!this.device.getMeasurement(this.channelConfigNumber, 8 + i).getUnit().startsWith(tmpValues[1].substring(0, 1))) {
						this.device.getMeasurement(this.channelConfigNumber, 8 + i).setUnit(tmpValues[1].contains(GDE.STRING_STAR) ? tmpValues[1].substring(0, tmpValues[1].indexOf(GDE.CHAR_STAR)) : tmpValues[1]);
					}
				}
				else if (i == 6) { //Gleitzahl + SpeedGleitzahl
					tmpValues = strValues[i + 1].replace("  ", " ").trim().split(NMEAParser.STRING_SENTENCE_SPLITTER);
					if (!tmpValues[1].startsWith("-")) {
						this.values[8 + i + 1] = (int) (Double.parseDouble(tmpValues[1]) * 1000.0);
						if (!this.device.getMeasurement(this.channelConfigNumber, 8 + i + 1).getUnit().equals("m/1")) {
							this.device.getMeasurement(this.channelConfigNumber, 8 + i + 1).setUnit("m/1");
						}
						if (tmpValues[2].equals("(")) {
							this.values[8 + i + 2] = (int) (Double.parseDouble(tmpValues[3]) * 1000.0);
						}
						else if (tmpValues[2].startsWith("(")) {
							tmpValues[2] = tmpValues[2].substring(1);
							this.values[8 + i + 2] = (int) (Double.parseDouble(tmpValues[2]) * 1000.0);
						}					
						if (!this.device.getMeasurement(this.channelConfigNumber, 8 + i + 2).getUnit().equals("km/h")) {
							this.device.getMeasurement(this.channelConfigNumber, 8 + i + 2).setUnit("km/h");
						} 
					}
					else {
						this.values[8 + i + 1] = 0;
						this.values[8 + i + 2] = 0;
					}
				}
				else if(i == 7) {
					try {
						this.values[2]  = (int) (Double.parseDouble(tmpValues[0].split(GDE.STRING_COMMA)[0]) * 1000.0);
					}
					catch (Exception e) {
						// ignore
					}
				}
			}
			catch (Exception e) {
				log.log(Level.WARNING, e.getMessage(), e);
				// ignore and leave value unchanged
			}
		}

		//GPS 
		//this.values[0]  = latitude;
		//this.values[1]  = longitude;
		//this.values[2]  = altitudeGPS;
		//this.values[3]  = numSatelites;
		//this.values[4]  = PDOP (dilution of precision) 
		//this.values[5]  = HDOP (horizontal dilution of precision) 
		//this.values[6]  = VDOP (vertical dilution of precision)		
		//this.values[7]  = velocity;
		//SMGPS
		//this.values[8] = altitudeRel;
		//this.values[9] = climb;
		//this.values[10] = voltageRx;
		//this.values[11] = distanceTotal;
		//this.values[12] = distanceStart;
		//this.values[13] = directionStart;
		//this.values[14] = trackAngle;
		//this.values[15] = glideRatio;
		//this.values[16] = speedGlideRatio;
	}

	/**
	 * parse SM GPS2 sentence
	 * $SMGPS2,+0.31 X,+0.58 Y,+0.79 Z,0 ENL,0 us*4A
	 * 1: Acceleration X
	 * 2: Acceleration Y
	 * 3: Acceleration Z
	 * 4: ENL
	 * 5: Impulse
	 * 6: Airspeed in [km/h]
	 * 7: statischer Luftdruck in [hPa]
	 * 8: Luftdruck TEK in [hPa]
	 * 9: Vario TEK in [m/s]
	 * @param strValues
	 */
	void parseSMGPS2(String[] strValues) {
		for (int i = 0; i < strValues.length - 1 && i < 9; i++) {
			try {
				String[] tmpValues = strValues[i + 1].trim().split(NMEAParser.STRING_SENTENCE_SPLITTER);
				this.values[17 + i] = (int) (Double.parseDouble(tmpValues[0]) * 1000.0);
				if (!this.device.getMeasurement(this.channelConfigNumber, 17 + i).getUnit().startsWith(tmpValues[1].substring(0, 1))) {
					this.device.getMeasurement(this.channelConfigNumber, 17 + i).setUnit(tmpValues[1].contains(GDE.STRING_STAR) ? tmpValues[1].substring(0, tmpValues[1].indexOf(GDE.CHAR_STAR)) : tmpValues[1]);
				}
			}
			catch (Exception e) {
				// ignore and leave value unchanged
			}
		}

		//GPS 
		//this.values[0]  = latitude;
		//this.values[1]  = longitude;
		//this.values[2]  = altitudeGPS;
		//this.values[3]  = numSatelites;
		//this.values[4]  = PDOP (dilution of precision) 
		//this.values[5]  = HDOP (horizontal dilution of precision) 
		//this.values[6]  = VDOP (vertical dilution of precision)		
		//this.values[7]  = velocity;
		//SMGPS
		//this.values[8] = altitudeRel;
		//this.values[9] = climb;
		//this.values[10] = voltageRx;
		//this.values[11] = distanceTotal;
		//this.values[12] = distanceStart;
		//this.values[13] = directionStart;
		//this.values[14] = trackAngle;
		//this.values[15] = glideRatio;
		//this.values[16] = speedGlideRatio;
		//SMGPS2
		//this.values[17] = acceleration x;
		//this.values[18] = acceleration y;
		//this.values[19] = acceleration z;
		//this.values[20] = noise level;
		//this.values[21] = Impulse;
		
		if (strValues.length > 6 && device.getName().endsWith("Logger3")) { //GPS-Logger3
			//this.values[22] = Airspeed [km/h];
			//this.values[23] = static Airpressure [hPa];
			//this.values[24] = Airpressure TEK [hPa];
			if (!isOffsetSet //&& this.values[22] >= 0 && this.values[22] <= 1000
					&& (initialPressure == 0 || (this.values[23] <= initialPressure + 20 && this.values[23] >= initialPressure - 20))) {
				airPressures.addValuePair(this.values[23], this.values[24]);

				this.values[24] = 0;
				if (initialPressure == 0) this.initialPressure = this.values[23];

				//uncomment if own airspeed should be calculated and replace Impulse values
				//			this.values[21] = this.values[7]; 
			}
			else {
				this.values[24] = this.values[23] - this.values[24] - airPressures.getAvgDiffMaxCount();
				isOffsetSet = true;

				//uncomment if own airspeed should be calculated and replace Impulse values
				//			if (this.values[7] > 10000) {
				//				rho = this.values[23] / (287.058 * (20 + 273.15)) / 10;
				//				this.values[21] = (int) (Math.sqrt( 2.0 * this.values[24]/1000 / rho ) * 36000);
				//			}
				//			else 
				//				this.values[21] = this.values[7];
			}
			//build sum of AirSpeed values between 39,5 and 40,5 km/h as well as sum of matching ∆TEC pressure
			if (this.values[22] >= 39500 && this.values[22] <= 40500) {
				vcAirSpeed40.add(this.values[22], this.values[24]);
			}
			//build sum of ∆TEC pressure values between 0,95 and 1,05 hPa as well as sum of matching ∆TEC pressure
			if (this.values[24] >= 950 && this.values[24] <= 1050) {
				vcAirSpeedDp1.add(this.values[22], this.values[24]);
			}
			//build sum of AirSpeed values between 69,5 and 70,5 km/h as well as sum of matching ∆TEC pressure
			if (this.values[22] >= 69500 && this.values[22] <= 70500) {
				vcAirSpeed70.add(this.values[22], this.values[24]);
			}
			//this.values[25] = Vario TEK [m/s];
		}
	}

	/**
	 * parse SM UNILOG sentence
	 * $UNILOG,11.31 V,0.00 A,0.0 W,0 rpm,0.00 VRx,0.9 m,---- °C (A1),1303 mAh (A2),13.9 °C (int)*30
	 * 1: Spannung
	 * 2: Strom
	 * 3: Leistung
	 * 4: Drehzahl
	 * 5: Rx Spannung
	 * 6: Höhe
	 * 7: Wert A1
	 * 8: Wert A2
	 * 9: Wert A3
	 * @param strValues
	 */
	void parseUNILOG(String[] strValues) {
		if (this.deviceName.equals("GPS-Logger")) {
			if (this.channelConfigNumber == 1) {
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=direction;
				//Unilog 	15=Voltage, 16=Current, 17=Power, 18=Revolution, 19=VoltageRx, 20=Altitude, 21=ValueA1, 22=ValueA2, 23=ValueA3
				//M-LINK  24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
				//begin GDE 3.4.9
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
				//CH1-UniLog
				//Unilog 17=voltage_UL 18=current_UL 19=power_UL 20=revolution_UL 21=voltageRx_UL 22=altitude_UL 23=a1_UL 24=a2_UL 25=a3_UL;
				//M-LINK 26=valAdd00 27=valAdd01 28=valAdd02 29=valAdd03 30=valAdd04 31=valAdd05 32=valAdd06 34=valAdd07 35=valAdd08 36=valAdd09 37=valAdd10 38=valAdd11 39=valAdd12 40=valAdd13 41=valAdd14;
				for (int i = 0; i < strValues.length && i < 9; i++) {
					try {
						String[] tmpValues = strValues[i + 1].trim().split(GDE.STRING_BLANK);
						this.values[17 + i] = (int) (Double.parseDouble(tmpValues[0]) * 1000.0);
						if (!this.device.getMeasurement(this.channelConfigNumber, 17 + i).getUnit().equals(tmpValues[1])) {
							this.device.getMeasurement(this.channelConfigNumber, 17 + i).setUnit(tmpValues[1].contains(GDE.STRING_STAR) ? tmpValues[1].substring(0, tmpValues[1].indexOf(GDE.CHAR_STAR)) : tmpValues[1]);
						}
					}
					catch (Exception e) {
						// ignore and leave value unchanged
					}
				}
			}
		}
		else if (this.deviceName.equals("GPS-Logger2") || this.deviceName.equals("GPS-Logger3")) {
			if (this.channelConfigNumber == 1) {
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=direction;
				//SMGPS2 	15=AccelerationX 16=AccelerationY 17=AccelerationZ 18=ENL 19=Impuls
				//CH1-UniLog
				//Unilog 	20=Voltage, 21=Current, 22=Power, 32=Revolution, 24=VoltageRx, 25=Altitude, 26=ValueA1, 27=ValueA2, 28=ValueA3
				//M-LINK  29=valAdd00 30=valAdd01 31=valAdd02 32=valAdd03 33=valAdd04 34=valAdd05 35=valAdd06 36=valAdd07 37=valAdd08 38=valAdd09 39=valAdd10 40=valAdd11 41=valAdd12 42=valAdd13 43=valAdd14;
				//begin FW1.26 / GDE 3.4.8
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
				//SMGPS2 17=AccelerationX 18=AccelerationY 19=AccelerationZ 20=ENL 21=Impulse 22=AirSpeed 23=pressure static 24=pressure TEK 25=climb TEK
				//CH1-UniLog
				//Unilog 26=voltage_UL 27=current_UL 28=power_UL 29=revolution_UL 30=voltageRx_UL 31=altitude_UL 32=a1_UL 33=a2_UL 34=a3_UL;
				//M-LINK 35=valAdd00 36=valAdd01 37=valAdd02 38=valAdd03 39=valAdd04 40=valAdd05 41=valAdd06 42=valAdd07 43=valAdd08 44=valAdd09 45=valAdd10 46=valAdd11 47=valAdd12 48=valAdd13 49=valAdd14;
				for (int i = 0; i < strValues.length && i < 9; i++) {
					try {
						String[] tmpValues = strValues[i + 1].trim().split(GDE.STRING_BLANK);
						this.values[26 + i] = (int) (Double.parseDouble(tmpValues[0]) * 1000.0);
						if (!this.device.getMeasurement(this.channelConfigNumber, 26 + i).getUnit().equals(tmpValues[1])) {
							this.device.getMeasurement(this.channelConfigNumber, 26 + i).setUnit(tmpValues[1].contains(GDE.STRING_STAR) ? tmpValues[1].substring(0, tmpValues[1].indexOf(GDE.CHAR_STAR)) : tmpValues[1]);
						}
					}
					catch (Exception e) {
						// ignore and leave value unchanged
					}
				}
			}
		}
	}

	/**
	 * parse SM UniLog 2 sentence within GPS-Logger data, switch to channel/configuration UniLog2
	 * $UNILOG,11.31 V,0.00 A,0.0 W,0 rpm,0.00 VRx,0.9 m,---- °C (A1),1303 mAh (A2),13.9 °C (int)*30
	 * 1: Spannung
	 * 2: Strom
	 * 3: Leistung
	 * 4: Drehzahl
	 * 5: Rx Spannung
	 * 6: Höhe
	 * 7: Wert A1
	 * 8: Wert A2
	 * 9: Wert A3
	 * 
	 * $UL2,2011-07-03,16:01:31.30,0.00, 12.37,1.89,-1.5,0.0,23.4,1868.5,4.96,3.6,2.7,18.0,,4.6,4.095,4.085,4.086,0.000,0.000,0.000,949.94,24.8,0.0,0.0*0D
	 * 1: date
	 * 2: time stamp absolute with dot hundreds
	 * 3: time stamp relative with dot hundreds
	 * 4: voltage [V]
	 * 5: current [A]
	 * 6: height (relative) [m]
	 * 7: climb [m/sec]
	 * 8: power [W]
	 * 9: revolution [1/min]
	 * 10: voltageRx [V]
	 * 11: capacity [mAh]
	 * 12: energy [Wmin]
	 * 13: value A1
	 * 14: value A2
	 * 15: value A3
	 * 16: cell voltage 1 [V]
	 * 17: cell voltage 2 [V]
	 * 18: cell voltage 3 [V]
	 * 19: cell voltage 4 [V]
	 * 20: cell voltage 5 [V]
	 * 21: cell voltage 6 [V]
	 * 22: air pressure [hPa]
	 * 23: temperature intern [°C]
	 * 24: servo impuls in [us]
	 * 25: servo impuls out [us]
	 * @param strValues
	 */
	void parseUL2(String[] strValues) {
		for (int i = 4; i < 2+4; i++) {
			//this.values[15] = 4: voltage [V]
			//this.values[16] = 5: current [A]
			try {
				String tmpValue = strValues[i].trim();
				this.values[i + 15 - 4] = (int) (tmpValue.indexOf(GDE.CHAR_STAR) > 1 
						? Double.parseDouble(tmpValue.substring(0, tmpValue.indexOf(GDE.CHAR_STAR))) * 1000.0 
						: Double.parseDouble(tmpValue) * 1000.0);
			}
			catch (Exception e) {
				// ignore and leave value unchanged
			}
		}
		try { //this.values[20] = 6: height (relative) [m]
			String tmpValue = strValues[6].trim();
			this.values[20] = (int) (tmpValue.indexOf(GDE.CHAR_STAR) > 1 
					? Double.parseDouble(tmpValue.substring(0, tmpValue.indexOf(GDE.CHAR_STAR))) * 1000.0 
					: Double.parseDouble(tmpValue) * 1000.0);
		}
		catch (Exception e) {
			// ignore and leave value unchanged
		}
		try {	//this.values[17] = 8: power [W]

			String tmpValue = strValues[8].trim();
			this.values[17] = (int) (tmpValue.indexOf(GDE.CHAR_STAR) > 1 
					? Double.parseDouble(tmpValue.substring(0, tmpValue.indexOf(GDE.CHAR_STAR))) * 1000.0 
					: Double.parseDouble(tmpValue) * 1000.0);
		}
		catch (Exception e) {
			// ignore and leave value unchanged
		}
		for (int i = 9; i < 2+9; i++) {
			//this.values[18] = 9: revolution [1/min]
			//this.values[19] = 10: voltageRx [V]
			try {
				String tmpValue = strValues[i].trim();
				this.values[i + 18 - 9] = (int) (tmpValue.indexOf(GDE.CHAR_STAR) > 1 
						? Double.parseDouble(tmpValue.substring(0, tmpValue.indexOf(GDE.CHAR_STAR))) * 1000.0 
						: Double.parseDouble(tmpValue) * 1000.0);
			}
			catch (Exception e) {
				// ignore and leave value unchanged
			}
		}
		for (int i = 13; i < 3+13; i++) {
			//this.values[21] = 13: value A1
			//this.values[22] = 14: value A2
			//this.values[23] = 15: value A3
			try {
				String tmpValue = strValues[i].trim();
				this.values[i + 21 - 13] = (int) (tmpValue.indexOf(GDE.CHAR_STAR) > 1 
						? Double.parseDouble(tmpValue.substring(0, tmpValue.indexOf(GDE.CHAR_STAR))) * 1000.0 
						: Double.parseDouble(tmpValue) * 1000.0);
			}
			catch (Exception e) {
				// ignore and leave value unchanged
			}
		}

		if (log.isLoggable(Level.FINE)) {
			StringBuilder s = new StringBuilder();
			for (int value : this.values) {
				s.append(value).append("; ");
			}
			log.log(Level.FINE, s.toString());
		}
	}

	/**
	 * parse SM UNILOG2 sentence used for UniLog2 only
	 * $UL2,2011-07-03,16:01:31.30,0.00, 12.37,1.89,-1.5,0.0,23.4,1868.5,4.96,3.6,2.7,18.0,,4.6,4.095,4.085,4.086,0.000,0.000,0.000,949.94,24.8,0.0,0.0*0D
	 * 1: date
	 * 2: time stamp absolute with dot hundreds
	 * 3: time stamp relative with dot hundreds
	 * 4: voltage [V]
	 * 5: current [A]
	 * 6: height (relative) [m]
	 * 7: climb [m/sec]
	 * 8: power [W]
	 * 9: revolution [1/min]
	 * 10: voltageRx [V]
	 * 11: capacity [mAh]
	 * 12: energy [Wmin]
	 * 13: value A1
	 * 14: value A2
	 * 15: value A3
	 * 16: cell voltage 1 [V]
	 * 17: cell voltage 2 [V]
	 * 18: cell voltage 3 [V]
	 * 19: cell voltage 4 [V]
	 * 20: cell voltage 5 [V]
	 * 21: cell voltage 6 [V]
	 * 22: air pressure [hPa]
	 * 23: temperature intern [°C]
	 * 24: servo impuls in [us]
	 * 25: servo impuls out [us]
	 * @param strValues
	 * @param inOutMapping
	 * @param indexBalance
	 * @param checkTime true will check if actual sentence has newer time compared to the one worked with before
	 */
	void parseUNILOG2(String[] strValues, int[] inOutMapping, int indexBalance, boolean checkTime) {
		if (checkTime) {
			String[] strValueDate = strValues[1].trim().split(GDE.STRING_DASH);
			if (this.date == null || Integer.parseInt(strValueDate[2]) > this.day) {
				this.year = Integer.parseInt(strValueDate[0]);
				this.month = Integer.parseInt(strValueDate[1]);
				this.day = Integer.parseInt(strValueDate[2]);
			}
			String[] strValueTime = strValues[2].trim().split(GDE.STRING_COLON);
			int hour = Integer.parseInt(strValueTime[0]) + this.timeOffsetUTC;
			int minute = Integer.parseInt(strValueTime[1]);
			int second = 0;
			if (strValueTime[2].contains(GDE.STRING_DOT)) {
				second = Integer.parseInt(strValueTime[2].substring(0, strValueTime[2].indexOf(GDE.CHAR_DOT)));
			}
			else {
				second = Integer.parseInt(strValueTime[2]);
			}
			GregorianCalendar calendar = new GregorianCalendar(this.year, this.month - 1, this.day, hour, minute, second);
			if (this.isAutoDstOffset && this.dstOffset == Short.MIN_VALUE) {
				this.correctDstOffset(calendar);
			}
			int indexAfterDot = strValueTime[2].indexOf(GDE.CHAR_DOT) + 1;
			long timeStamp = calendar.getTimeInMillis() + (indexAfterDot > 0  && strValueTime[2].length() >= indexAfterDot + 2 ? Long.parseLong(strValueTime[2].substring(indexAfterDot, indexAfterDot + 2)) * 10 : 0);
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, "UNILOG2 " + Integer.parseInt(strValueTime[2].substring(indexAfterDot, indexAfterDot + 2)) * 10); //$NON-NLS-1$);
			if (this.lastTimeStamp < timeStamp) {
				this.time_ms = (int) (this.lastTimeStamp == 0 ? 0 : this.time_ms + (timeStamp - this.lastTimeStamp));
				this.lastTimeStamp = timeStamp;
				this.date = calendar.getTime();
				if (log.isLoggable(Level.FINE)) 
					log.log(Level.FINE, new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss.SS").format(timeStamp)); //$NON-NLS-1$);
				if (this.startTimeStamp == 0) this.startTimeStamp = timeStamp;
			}
			else {
				return;
			}
		}
		
		//now start real sentence value parsing
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		//inOutMapping  000, 001, 002, 003, 004, 005, 006, 007, 008, 009, 010, 011, 012, 013, 014, 015, 016, 017, 018, 019, 020, 021, 022, 023, 024, 025
		//UniLog2      { -1,  -1,  -1,  -1, 	1, 		2, 15,  16,   4,  13,   0,   3,   5,  18,  19,  20,   7,   8,   9,  10,  11,  12,  20,  21,  22,  23};
		//GPS-Logger   { -1,  -1,  -1,  -1,  17,  18,  -1,  -1,  20,  29,  -1,  19,  21,  30,  31,  32,  23,  24,  25,  26,  27,  28,  -1,  32,  -1,  -1};
		//GPS-Logger2/3{ -1,  -1,  -1,  -1,  26,  27,  -1,  -1,  29,  38,  -1,  28,  30,  39,  40,  41,  32,  33,  34,  35,  36,  37,  -1,  42,  -1,  -1};
		for (int i = 4; i < strValues.length; i++) {
			try {
				if (inOutMapping[i] >= 0) {
					String tmpValue = strValues[i].trim();
					this.values[inOutMapping[i]] = (int) (tmpValue.indexOf(GDE.CHAR_STAR) > 1 ? Double.parseDouble(tmpValue.substring(0, tmpValue.indexOf(GDE.CHAR_STAR))) * 1000.0 : Double.parseDouble(tmpValue) * 1000.0);
					if (i >= 16 && i <= 21 && this.values[inOutMapping[i]] > 0) {
						maxVotage = this.values[inOutMapping[i]] > maxVotage ? this.values[inOutMapping[i]] : maxVotage;
						minVotage = this.values[inOutMapping[i]] < minVotage ? this.values[inOutMapping[i]] : minVotage;
					}
				}
			}
			catch (Exception e) {
				// ignore and leave value unchanged
			}
		}
		this.values[indexBalance] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 1000;

		if (log.isLoggable(Level.FINE)) {
			StringBuilder s = new StringBuilder();
			for (int value : this.values) {
				s.append(value).append("; ");
			}
			log.log(Level.FINE, s.toString());
		}
	}

	/**
	 * parse SM MLINK sentence
	 * $MLINK,0: 4.9 V,1: 100 % LQI,3: 0.2 m/s,4: 14.7 °C,5: 5.4 km/h,6: 261.4 °,7: 0 m,8: 2 m,9: 0.0 km,10: 0.0 A,11: 11.1 V,12: 0 rpm,13: 681 mAh,14: 1 m*19
	 * sentence declares only address correlation to measurement values
	 * @param strValues
	 */
	void parseMLINK(String[] strValues) {
		if (this.deviceName.equals("GPS-Logger")) {
			if (this.channelConfigNumber == 1) { //UniLog
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=direction;
				//Unilog 	15=Voltage, 16=Current, 17=Power, 18=Revolution, 19=VoltageRx, 20=Altitude, 21=ValueA1, 22=ValueA2, 23=ValueA3
				//M-LINK  24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
				for (int i = 1; i < strValues.length && i <= 15; i++) {
					try {
						String[] tmpValues = strValues[i].trim().split(NMEAParser.STRING_SENTENCE_SPLITTER);
						int address = Integer.parseInt(tmpValues[0]);
						this.values[24 + address] = (int) (Double.parseDouble(tmpValues[2]) * 1000.0);
						if (!this.device.getMeasurement(this.channelConfigNumber, 24 + address).getUnit().equals(tmpValues[3])) {
							this.device.getMeasurement(this.channelConfigNumber, 24 + address).setUnit(tmpValues[3].contains(GDE.STRING_STAR) ? tmpValues[3].substring(0, tmpValues[3].indexOf(GDE.CHAR_STAR)) : tmpValues[3]);
						}
					}
					catch (Exception e) {
						// ignore and leave value unchanged
					}
				}
			}
			if (this.channelConfigNumber == 2) { //UniLog2
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=direction;
				//Unilog2 15=Voltage, 16=Current, 17=Capacity, 18=Power, 19=Energy, 20=CellBalance, 21=CellVoltage1, 21=CellVoltage2, 23=CellVoltage3, 
				//Unilog2 24=CellVoltage4, 25=CellVoltage5, 26=CellVoltage6, 27=Revolution, 28=ValueA1, 29=ValueA2, 30=ValueA3, 31=InternTemperature
				//M-LINK  32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
				for (int i = 1; i < strValues.length && i <= 15; i++) {
					try {
						String[] tmpValues = strValues[i].trim().split(NMEAParser.STRING_SENTENCE_SPLITTER);
						int address = Integer.parseInt(tmpValues[0]);
						this.values[32 + address] = (int) (Double.parseDouble(tmpValues[2]) * 1000.0);
						if (!this.device.getMeasurement(this.channelConfigNumber, 32 + address).getUnit().equals(tmpValues[3])) {
							this.device.getMeasurement(this.channelConfigNumber, 32 + address).setUnit(tmpValues[3].contains(GDE.STRING_STAR) ? tmpValues[3].substring(0, tmpValues[3].indexOf(GDE.CHAR_STAR)) : tmpValues[3]);
						}
					}
					catch (Exception e) {
						// ignore and leave value unchanged
					}
				}
			}
		}
		else if (this.deviceName.equals("GPS-Logger2") || this.deviceName.equals("GPS-Logger3")) {
			if (this.channelConfigNumber == 1) { //UniLog
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=direction;
				//SMGPS2 	15=AccelerationX 16=AccelerationY 17=AccelerationZ 18=ENL 19=Impuls
				//CH1-UniLog
				//Unilog 	20=Voltage, 21=Current, 22=Power, 32=Revolution, 24=VoltageRx, 25=Altitude, 26=ValueA1, 27=ValueA2, 28=ValueA3
				//M-LINK  29=valAdd00 30=valAdd01 31=valAdd02 32=valAdd03 33=valAdd04 34=valAdd05 35=valAdd06 36=valAdd07 37=valAdd08 38=valAdd09 39=valAdd10 40=valAdd11 41=valAdd12 42=valAdd13 43=valAdd14;
				//begin FW1.26
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
				//SMGPS2 17=AccelerationX 18=AccelerationY 19=AccelerationZ 20=ENL 21=Impulse 22=AirSpeed 23=pressure static 24=pressure TEK 25=climb TEK
				//CH1-UniLog
				//Unilog 26=voltage_UL 27=current_UL 28=power_UL 29=revolution_UL 30=voltageRx_UL 31=altitude_UL 32=a1_UL 33=a2_UL 34=a3_UL;
				//M-LINK 35=valAdd00 36=valAdd01 37=valAdd02 38=valAdd03 39=valAdd04 40=valAdd05 41=valAdd06 42=valAdd07 43=valAdd08 44=valAdd09 45=valAdd10 46=valAdd11 47=valAdd12 48=valAdd13 49=valAdd14;
				for (int i = 1; i < strValues.length && i <= 15; i++) {
					try {
						String[] tmpValues = strValues[i].trim().split(NMEAParser.STRING_SENTENCE_SPLITTER);
						int address = Integer.parseInt(tmpValues[0]);
						this.values[35 + address] = (int) (Double.parseDouble(tmpValues[2]) * 1000.0);
						if (!this.device.getMeasurement(this.channelConfigNumber, 35 + address).getUnit().equals(tmpValues[3])) {
							this.device.getMeasurement(this.channelConfigNumber, 35 + address).setUnit(tmpValues[3].contains(GDE.STRING_STAR) ? tmpValues[3].substring(0, tmpValues[3].indexOf(GDE.CHAR_STAR)) : tmpValues[3]);
						}
					}
					catch (Exception e) {
						// ignore and leave value unchanged
					}
				}
			}
			if (this.channelConfigNumber == 2) { //UniLog2
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=trackAngle;
				//SMGPS2 	15=AccelerationX 16=AccelerationY 17=AccelerationZ 18=ENL 19=Impuls
				//CH2-UniLog2
				//Unilog2 	20=Voltage, 21=Current, 22=Capacity, 23=Power, 24=Energy, 25=CellBalance, 26=CellVoltage1, 27=CellVoltage2, 28=CellVoltage3, 
				//Unilog2 	29=CellVoltage4, 30=CellVoltage5, 31=CellVoltage6, 32=Revolution, 33=ValueA1, 34=ValueA2, 35=ValueA3, 36=InternTemperature
				//M-LINK  	37=valAdd00 38=valAdd01 39=valAdd02 40=valAdd03 41=valAdd04 42=valAdd05 43=valAdd06 44=valAdd07 45=valAdd08 46=valAdd09 47=valAdd10 48=valAdd11 49=valAdd12 50=valAdd13 51=valAdd14;
				//begin FW1.26
				//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
				//SMGPS2 17=AccelerationX 18=AccelerationY 19=AccelerationZ 20=ENL 21=Impulse 22=AirSpeed 23=pressure static 24=pressure TEK 25=climb TEK
				//CH2-UniLog2
				//Unilog2 26=voltage_UL 27=current_UL2 28=capacity_UL2 29=power_UL2 30=energy_UL2 31=balance_UL 32=cellVoltage1 33=cellVolt2_ul 34=cellVolltage3_UL 35=cellVoltage4_UL 36=cellVoltage5_UL 37=cellVoltage6_UL 38=revolution_UL 39=a1_UL 40=a2_UL 41=a3_UL 42=temp_UL;
				//M-LINK 43=valAdd00 44=valAdd01 45=valAdd02 46=valAdd03 47=valAdd04 48=valAdd05 49=valAdd06 50=valAdd07 51=valAdd08 52=valAdd09 53=valAdd10 54=valAdd11 55=valAdd12 56=valAdd13 57=valAdd14;
				for (int i = 1; i < strValues.length && i <= 15; i++) {
					try {
						String[] tmpValues = strValues[i].trim().split(NMEAParser.STRING_SENTENCE_SPLITTER);
						int address = Integer.parseInt(tmpValues[0]);
						this.values[43 + address] = (int) (Double.parseDouble(tmpValues[2]) * 1000.0);
						if (!this.device.getMeasurement(this.channelConfigNumber, 43 + address).getUnit().equals(tmpValues[3])) {
							this.device.getMeasurement(this.channelConfigNumber, 43 + address).setUnit(tmpValues[3].contains(GDE.STRING_STAR) ? tmpValues[3].substring(0, tmpValues[3].indexOf(GDE.CHAR_STAR)) : tmpValues[3]);
						}
					}
					catch (Exception e) {
						// ignore and leave value unchanged
					}
				}
			}
		}
		else if (this.deviceName.equals("UniLog2")) {
			//0=VoltageRx, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Energy, 6=CellBalance, 7=CellVoltage1, 8=CellVoltage2, 9=CellVoltage3, 
			//10=CellVoltage4, 11=CellVoltage5, 12=CellVoltage6, 13=Revolution, 14=Efficiency, 15=Altitude, 16=Climb, 17=ValueA1, 18=ValueA2, 19=ValueA3,
			//20=ValueCollector, 21=InternTemperature, 22=ServoImpuls In, 23=ServoImpuls Out, 
			//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
			for (int i = 1; i < strValues.length && i <= 15; i++) {
				try {
					String[] tmpValues = strValues[i].trim().split(NMEAParser.STRING_SENTENCE_SPLITTER);
					int address = Integer.parseInt(tmpValues[0]);
					this.values[24 + address] = (int) (Double.parseDouble(tmpValues[2]) * 1000.0);
					if (!this.device.getMeasurement(this.channelConfigNumber, 24 + address).getUnit().equals(tmpValues[3])) {
						this.device.getMeasurement(this.channelConfigNumber, 24 + address).setUnit(tmpValues[3].contains(GDE.STRING_STAR) ? tmpValues[3].substring(0, tmpValues[3].indexOf(GDE.CHAR_STAR)) : tmpValues[3]);
					}
				}
				catch (Exception e) {
					// ignore and leave value unchanged
				}
			}
		}
		//GPS 
		//this.values[0]  = latitude;
		//this.values[1]  = longitude;
		//this.values[2]  = altitudeGPS;
		//this.values[3]  = numSatelites;
		//this.values[4]  = PDOP (dilution of precision) 
		//this.values[5]  = HDOP (horizontal dilution of precision) 
		//this.values[6]  = VDOP (vertical dilution of precision)
		//this.values[7]  = velocity;
		//SMGPS
		//this.values[8]  = altitudeRel;
		//this.values[9]  = climb;
		//this.values[10] = voltageRx;
		//this.values[11] = distanceTotal;
		//this.values[12] = distanceStart;
		//this.values[13] = directionStart;
		//this.values[14] = trackAngle;
		//Unilog
		//this.values[15] = voltageUniLog;
		//this.values[16] = currentUniLog;
		//this.values[17] = powerUniLog;
		//this.values[18] = revolutionUniLog;
		//this.values[19] = voltageRxUniLog;
		//this.values[20] = heightUniLog;
		//this.values[21] = a1UniLog;
		//this.values[22] = a2UniLog;
		//this.values[23] = a3UniLog;
		//M-LINK
		//this.values[24] = add00;
		//this.values[25] = add01;
		//this.values[26] = add02;
		//this.values[27] = add03;
		//this.values[28] = add04;
		//this.values[29] = add05;
		//this.values[30] = add06;
		//this.values[31] = add07;
		//this.values[32] = add08;
		//this.values[33] = add09;
		//this.values[34] = add10;
		//this.values[35] = add11;
		//this.values[36] = add12;
		//this.values[37] = add13;
		//this.values[38] = add14;
	}

	/**
	 * parse Multiplex FlightRecorder data - floating values
	 * $D;0000,95;;;8,9;;;;;;;0,0;-14;0;*33
	 * 1: time stamp relative seconds
	 * @param strValues
	 */
	void parseMpxD(String[] strValues) {
		try {
			long timeStamp = (long) (Double.parseDouble(strValues[1].replace(',', '.'))*1000); //two decimal digits
			if (this.lastTimeStamp < timeStamp) {
				this.time_ms = (int) (this.lastTimeStamp == 0 ? 0 : this.time_ms + (timeStamp - this.lastTimeStamp));
				this.lastTimeStamp = timeStamp;
				log.log(Level.FINE, "timeStamp = " + timeStamp); //$NON-NLS-1$);
				if (this.startTimeStamp == 0) this.startTimeStamp = timeStamp;
			}
			else {
				return;
			}
			String strValue;
			for (int i = 2; i < strValues.length-1; i++) {
				try {
					strValue = strValues[i].trim();
					if (strValue.length() > 0) 
						this.values[i+2] = (int) (Double.parseDouble(strValue) * 1000.0);	
				}
				catch (Exception e) {
					e.printStackTrace();
					// ignore and leave value unchanged
				}
			}
		}
		catch (NumberFormatException e) {
			// ignore and leave value unchanged
		}
	}

	/**
	 * @return the time
	 */
	public long getTime_ms() {
		return this.time_ms;
	}

	/**
	 * @return the values
	 */
	public int[] getValues() {
		return this.values;
	}

	/**
	 * @return the date
	 */
	public Date getDate() {
		return this.date;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return this.comment != null ? this.comment : GDE.STRING_EMPTY;
	}

	/**
	 * @return the startTimeStamp
	 */
	public synchronized long getStartTimeStamp() {
		return startTimeStamp;
	}

	/**
	 * @return the lastTimeStamp
	 */
	public synchronized long getLastTimeStamp() {
		return lastTimeStamp;
	}
	

	/**
	 * default parse method for $1, 1, 0, 14780, 0,598, 1,000, 8,838, 22 like lines
	 * @param inputLine
	 * @param strValues
	 * @throws DevicePropertiesInconsistenceException
	 */
	public void parse(String inputLine, String[] strValues) throws DevicePropertiesInconsistenceException {
		//to be implemented by extending DataParser
	}

	public boolean isSupportedSentence(final String sentenceSignature) {
		boolean isSupported = false;
		try {
			NMEA.valueOf(sentenceSignature);
			isSupported = true;
		}
		catch(Throwable t) {
			//ignore
		}
		return isSupported;
	}

	/**
	 * @return true if all $2, $3 should be redirected to channel 1
	 */
	@Override
	public boolean isRedirectChannel1() {
		return false;
	}
	
}
