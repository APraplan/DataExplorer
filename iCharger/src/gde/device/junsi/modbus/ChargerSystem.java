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

import gde.io.DataParser;

public class ChargerSystem {
	
	short								tempUnit;
	short								tempStop;
	short								tempFansOn;
	short								tempReduce;
	short								fansSpeed;														//DUO only
	short								usbPdEnable;													//DX only
	short								fansOffDelay;
	short								lcdContraste;
	short								lightValue;
	short								beepMode;															//DUO only
	short[]							beepType						= new short[4];
	short[]							beepEnable					= new short[4];
	short[]							beepVOL							= new short[4];
	short								selectLanguage; 											//0=english, 1=german
	short								selectAdj; 														//calibration
	short								systemVersion;
	short								selInputSource; 											//DUO 0=AC, 1=Bat; X 0-3
	
	//begin DUO only section
	short								dcInputLowVolt;
	short								dcInputOverVolt;
	short								dcInputCurrentLimit;
	short								batInputLowVolt;
	short								batInputOverVolt;
	short								batInputCurrentLimit;
	short								regEnable;
	short								regVoltLimit;
	short								regCurrentLimit;
	int									regCapacityLimit; //0 - 999900
	short[]							chargePower					= new short[2]; 
	short[]							dischargePower			= new short[2];
	short								proPower;
	short[]							monitorLogInterval	= new short[2];
	short[]							monitorLogSaveToSD	= new short[2];
	short								modBusMode;
	short								serialModBusAddr;
	short								serialModBusBaudeRate;
	short								serialModBusParity;
	//end DUO only section
	
	//begin DX/X/S only section
	InputSource[]				xInputSources = new InputSource[4]; //X 0-3
	short								xDischargePower;
	short								xMonitorLogInterval;
	short								xMonitorLogSaveToSD;
	//end DX/X/S only section
	short								servoType;
	short								servoUserCenter;
	short								servoUserRate;
	short								servoUserOpAngle;
	short								servoSpeedVolt; 	//DX only
	
	short								antiSparkEnable; 	//DX only

	short[]							dump;
	
	int									index	= 0;						//iterate in constructor
	
	class InputSource {
		short	inputLowVolt;				
		short	inputCurrentLimit;	
		short	chargePower;				
		short	regEnable;					
		short	regVoltLimit;				  
		short	regCurrentLimit;		
		short	regPowerLimit;			
		int		regCapLimit;				
		
		InputSource(final byte[] readSystemData, int index) {
			this.inputLowVolt = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.inputCurrentLimit = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.chargePower = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.regEnable = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.regVoltLimit = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.regCurrentLimit = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.regPowerLimit = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.regCapLimit = DataParser.parse2Int(readSystemData, index);
		}

		/**
		 * @return the size in bytes
		 */
		public int getSize() {
			return 18;
		}
		/**
		 * @return the inputLowVolt
		 */
		public short getInputLowVolt() {
			return inputLowVolt;
		}

		/**
		 * @param inputLowVolt the inputLowVolt to set
		 */
		public void setInputLowVolt(short inputLowVolt) {
			this.inputLowVolt = inputLowVolt;
		}

		/**
		 * @return the inputCurrentLimit
		 */
		public short getInputCurrentLimit() {
			return inputCurrentLimit;
		}

		/**
		 * @param inputCurrentLimit the inputCurrentLimit to set
		 */
		public void setInputCurrentLimit(short inputCurrentLimit) {
			this.inputCurrentLimit = inputCurrentLimit;
		}

		/**
		 * @return the chargePower
		 */
		public short getChargePower() {
			return chargePower;
		}

		/**
		 * @param chargePower the chargePower to set
		 */
		public void setChargePower(short chargePower) {
			this.chargePower = chargePower;
		}

		/**
		 * @return the regEnable
		 */
		public short getRegEnable() {
			return regEnable;
		}

		/**
		 * @param regEnable the regEnable to set
		 */
		public void setRegEnable(short regEnable) {
			this.regEnable = regEnable;
		}

		/**
		 * @return the regVoltLimit
		 */
		public short getRegVoltLimit() {
			return regVoltLimit;
		}

		/**
		 * @param regVoltLimit the regVoltLimit to set
		 */
		public void setRegVoltLimit(short regVoltLimit) {
			this.regVoltLimit = regVoltLimit;
		}

		/**
		 * @return the regCurrentLimit
		 */
		public short getRegCurrentLimit() {
			return regCurrentLimit;
		}

		/**
		 * @param regCurrentLimit the regCurrentLimit to set
		 */
		public void setRegCurrentLimit(short regCurrentLimit) {
			this.regCurrentLimit = regCurrentLimit;
		}

		/**
		 * @return the regPowerLimit
		 */
		public short getRegPowerLimit() {
			return regPowerLimit;
		}

		/**
		 * @param regPowerLimit the regPowerLimit to set
		 */
		public void setRegPowerLimit(short regPowerLimit) {
			this.regPowerLimit = regPowerLimit;
		}

		/**
		 * @return the regCapLimit
		 */
		public int getRegCapLimit() {
			return regCapLimit;
		}

		/**
		 * @param regCapLimit the regCapLimit to set
		 */
		public void setRegCapLimit(int regCapLimit) {
			this.regCapLimit = regCapLimit;
		}
	}

	//construct System data using received data array
	ChargerSystem(final byte[] readSystemData, final boolean isDuo, final boolean isDx) {
		this.index = 0;
		this.tempUnit = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.tempStop = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.tempFansOn = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.tempReduce = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		
		if (isDuo)
			this.fansSpeed = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		if (isDx)
			this.usbPdEnable = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);

		this.fansOffDelay = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.lcdContraste = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.lightValue = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		
		if (isDuo)
			this.beepMode = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		
		this.beepType[0] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.beepType[1] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.beepType[2] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.beepType[3] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.beepEnable[0] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.beepEnable[1] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.beepEnable[2] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.beepEnable[3] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.beepVOL[0] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.beepVOL[1] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.beepVOL[2] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.beepVOL[3] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.selectLanguage = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.selectAdj = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.systemVersion = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		this.selInputSource = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		
		if (isDuo) { //duo devices allow channel individual log setting		
			this.dcInputLowVolt = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dcInputOverVolt = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dcInputCurrentLimit = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.batInputLowVolt = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.batInputOverVolt = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.batInputCurrentLimit = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.regEnable = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.regVoltLimit = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.regCurrentLimit = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.chargePower[0] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.chargePower[1] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dischargePower[0] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dischargePower[1] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.proPower = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.monitorLogInterval[0] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.monitorLogInterval[1] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.monitorLogSaveToSD[0] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.monitorLogSaveToSD[1] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
	
			this.servoType = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.servoUserCenter = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.servoUserRate = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.servoUserOpAngle = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
	
			this.modBusMode = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.serialModBusAddr = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.serialModBusBaudeRate = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.serialModBusParity = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
	
			this.regCapacityLimit = DataParser.parse2Int(readSystemData, index);
			index += 4;

			this.dump	= new short[7];
			this.dump[0] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[1] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[2] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[3] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[4] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[5] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[6] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
		}
		else {			
			this.xInputSources[0] = new InputSource(readSystemData, index);
			index += this.xInputSources[0].getSize();
			this.xInputSources[1] = new InputSource(readSystemData, index);
			index += this.xInputSources[0].getSize();
			this.xInputSources[2] = new InputSource(readSystemData, index);
			index += this.xInputSources[0].getSize();
			this.xInputSources[3] = new InputSource(readSystemData, index);
			index += this.xInputSources[0].getSize();
			if (isDx) {
				this.chargePower[0] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
				this.chargePower[1] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
				this.dischargePower[0] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
				this.dischargePower[1] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
				this.proPower = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
				this.monitorLogInterval[0] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
				this.monitorLogInterval[1] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
				this.monitorLogSaveToSD[0] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
				this.monitorLogSaveToSD[1] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);				
			}
			else {
				this.xDischargePower = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
				this.xMonitorLogInterval = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
				this.xMonitorLogSaveToSD = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			}
	
			this.servoType = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.servoUserCenter = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.servoUserRate = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.servoUserOpAngle = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			if (isDx) {
				this.servoSpeedVolt = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
				this.antiSparkEnable = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			}
	
			if (isDx) 
				this.dump	= new short[15];
			else
				this.dump	= new short[12];

			this.dump[0] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[1] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[2] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[3] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[4] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[5] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[6] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[7] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[8] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[9] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[10] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			this.dump[11] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			if (isDx) {
				this.dump[12] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
				this.dump[13] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);		
				this.dump[14] = DataParser.parse2Short(readSystemData[index++], readSystemData[index++]);
			}
		}
	}

	public byte[] getAsByteArray(final boolean isDuo, final boolean isDx) {
		byte[] memoryBuffer = new byte[(ChargerSystem.getSize(isDuo, isDx) + 1) / 2 * 2];

		index = 0;
		if (isDuo) {
			memoryBuffer[index++] = (byte) (this.tempUnit & 0xFF);
			memoryBuffer[index++] = (byte) (this.tempUnit >> 8);
			memoryBuffer[index++] = (byte) (this.tempStop & 0xFF);
			memoryBuffer[index++] = (byte) (this.tempStop >> 8);
			memoryBuffer[index++] = (byte) (this.tempFansOn & 0xFF);
			memoryBuffer[index++] = (byte) (this.tempFansOn >> 8);
			memoryBuffer[index++] = (byte) (this.tempReduce & 0xFF);
			memoryBuffer[index++] = (byte) (this.tempReduce >> 8);
			memoryBuffer[index++] = (byte) (this.fansSpeed & 0xFF);
			memoryBuffer[index++] = (byte) (this.fansSpeed >> 8);
			memoryBuffer[index++] = (byte) (this.fansOffDelay & 0xFF);
			memoryBuffer[index++] = (byte) (this.fansOffDelay >> 8);
	
			memoryBuffer[index++] = (byte) (this.lcdContraste & 0xFF);
			memoryBuffer[index++] = (byte) (this.lcdContraste >> 8);
			memoryBuffer[index++] = (byte) (this.lightValue & 0xFF);
			memoryBuffer[index++] = (byte) (this.lightValue >> 8);
	
			memoryBuffer[index++] = (byte) (this.beepMode & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepMode >> 8);
	
			memoryBuffer[index++] = (byte) (this.beepType[0] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepType[0] >> 8);
			memoryBuffer[index++] = (byte) (this.beepType[1] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepType[1] >> 8);
			memoryBuffer[index++] = (byte) (this.beepType[2] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepType[2] >> 8);
			memoryBuffer[index++] = (byte) (this.beepType[3] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepType[3] >> 8);
	
			memoryBuffer[index++] = (byte) (this.beepEnable[0] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepEnable[0] >> 8);
			memoryBuffer[index++] = (byte) (this.beepEnable[1] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepEnable[1] >> 8);
			memoryBuffer[index++] = (byte) (this.beepEnable[2] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepEnable[2] >> 8);
			memoryBuffer[index++] = (byte) (this.beepEnable[3] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepEnable[3] >> 8);
	
			memoryBuffer[index++] = (byte) (this.beepVOL[0] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepVOL[0] >> 8);
			memoryBuffer[index++] = (byte) (this.beepVOL[1] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepVOL[1] >> 8);
			memoryBuffer[index++] = (byte) (this.beepVOL[2] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepVOL[2] >> 8);
			memoryBuffer[index++] = (byte) (this.beepVOL[3] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepVOL[3] >> 8);
	
			memoryBuffer[index++] = (byte) (this.selectLanguage & 0xFF);
			memoryBuffer[index++] = (byte) (this.selectLanguage >> 8);
	
			memoryBuffer[index++] = (byte) (this.selectAdj & 0xFF);
			memoryBuffer[index++] = (byte) (this.selectAdj >> 8);
			memoryBuffer[index++] = (byte) (this.systemVersion & 0xFF);
			memoryBuffer[index++] = (byte) (this.systemVersion >> 8);
	
			memoryBuffer[index++] = (byte) (this.selInputSource & 0xFF);
			memoryBuffer[index++] = (byte) (this.selInputSource >> 8);
	
			memoryBuffer[index++] = (byte) (this.dcInputLowVolt & 0xFF);
			memoryBuffer[index++] = (byte) (this.dcInputLowVolt >> 8);
			memoryBuffer[index++] = (byte) (this.dcInputOverVolt & 0xFF);
			memoryBuffer[index++] = (byte) (this.dcInputOverVolt >> 8);
			memoryBuffer[index++] = (byte) (this.dcInputCurrentLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.dcInputCurrentLimit >> 8);
	
			memoryBuffer[index++] = (byte) (this.batInputLowVolt & 0xFF);
			memoryBuffer[index++] = (byte) (this.batInputLowVolt >> 8);
			memoryBuffer[index++] = (byte) (this.batInputOverVolt & 0xFF);
			memoryBuffer[index++] = (byte) (this.batInputOverVolt >> 8);
			memoryBuffer[index++] = (byte) (this.batInputCurrentLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.batInputCurrentLimit >> 8);
	
			memoryBuffer[index++] = (byte) (this.regEnable & 0xFF);
			memoryBuffer[index++] = (byte) (this.regEnable >> 8);
			memoryBuffer[index++] = (byte) (this.regVoltLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.regVoltLimit >> 8);
			memoryBuffer[index++] = (byte) (this.regCurrentLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.regCurrentLimit >> 8);

			memoryBuffer[index++] = (byte) (this.chargePower[0] & 0xFF);
			memoryBuffer[index++] = (byte) (this.chargePower[0] >> 8);
			memoryBuffer[index++] = (byte) (this.chargePower[1] & 0xFF);
			memoryBuffer[index++] = (byte) (this.chargePower[1] >> 8);
			
			memoryBuffer[index++] = (byte) (this.dischargePower[0] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dischargePower[0] >> 8);
			memoryBuffer[index++] = (byte) (this.dischargePower[1] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dischargePower[1] >> 8);
			
			memoryBuffer[index++] = (byte) (this.proPower & 0xFF);
			memoryBuffer[index++] = (byte) (this.proPower >> 8);

			memoryBuffer[index++] = (byte) (this.monitorLogInterval[0] & 0xFF);
			memoryBuffer[index++] = (byte) (this.monitorLogInterval[0] >> 8);
			memoryBuffer[index++] = (byte) (this.monitorLogInterval[1] & 0xFF);
			memoryBuffer[index++] = (byte) (this.monitorLogInterval[1] >> 8);

			memoryBuffer[index++] = (byte) (this.monitorLogSaveToSD[0] & 0xFF);
			memoryBuffer[index++] = (byte) (this.monitorLogSaveToSD[0] >> 8);
			memoryBuffer[index++] = (byte) (this.monitorLogSaveToSD[1] & 0xFF);
			memoryBuffer[index++] = (byte) (this.monitorLogSaveToSD[1] >> 8);

			memoryBuffer[index++] = (byte) (this.servoType & 0xFF);
			memoryBuffer[index++] = (byte) (this.servoType >> 8);
			memoryBuffer[index++] = (byte) (this.servoUserCenter & 0xFF);
			memoryBuffer[index++] = (byte) (this.servoUserCenter >> 8);
			memoryBuffer[index++] = (byte) (this.servoUserRate & 0xFF);
			memoryBuffer[index++] = (byte) (this.servoUserRate >> 8);
			memoryBuffer[index++] = (byte) (this.servoUserOpAngle & 0xFF);
			memoryBuffer[index++] = (byte) (this.servoUserOpAngle >> 8);

			memoryBuffer[index++] = (byte) (this.modBusMode & 0xFF);
			memoryBuffer[index++] = (byte) (this.modBusMode >> 8);
			memoryBuffer[index++] = (byte) (this.serialModBusAddr & 0xFF);
			memoryBuffer[index++] = (byte) (this.serialModBusAddr >> 8);
			memoryBuffer[index++] = (byte) (this.serialModBusBaudeRate & 0xFF);
			memoryBuffer[index++] = (byte) (this.serialModBusBaudeRate >> 8);
			memoryBuffer[index++] = (byte) (this.serialModBusParity & 0xFF);
			memoryBuffer[index++] = (byte) (this.serialModBusParity >> 8);

			memoryBuffer[index++] = (byte) (this.regCapacityLimit & 0xFF);
			memoryBuffer[index++] = (byte) ((this.regCapacityLimit & 0x0000FF00) >> 8);
			memoryBuffer[index++] = (byte) ((this.regCapacityLimit & 0x00FF0000) >> 16);
			memoryBuffer[index++] = (byte) (this.regCapacityLimit >> 24);
			
			memoryBuffer[index++] = (byte) (this.dump[0] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[0] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[1] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[1] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[2] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[2] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[3] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[3] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[4] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[4] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[5] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[5] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[6] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[6] >> 8);
		}
		else {
			memoryBuffer[index++] = (byte) (this.tempUnit & 0xFF);
			memoryBuffer[index++] = (byte) (this.tempUnit >> 8);
			memoryBuffer[index++] = (byte) (this.tempStop & 0xFF);
			memoryBuffer[index++] = (byte) (this.tempStop >> 8);
			memoryBuffer[index++] = (byte) (this.tempFansOn & 0xFF);
			memoryBuffer[index++] = (byte) (this.tempFansOn >> 8);
			memoryBuffer[index++] = (byte) (this.tempReduce & 0xFF);
			memoryBuffer[index++] = (byte) (this.tempReduce >> 8);
			
			if (isDx) {
				memoryBuffer[index++] = (byte) (this.usbPdEnable & 0xFF);
				memoryBuffer[index++] = (byte) (this.usbPdEnable >> 8);
			}
			
			memoryBuffer[index++] = (byte) (this.fansOffDelay & 0xFF);
			memoryBuffer[index++] = (byte) (this.fansOffDelay >> 8);
	
			memoryBuffer[index++] = (byte) (this.lcdContraste & 0xFF);
			memoryBuffer[index++] = (byte) (this.lcdContraste >> 8);
			memoryBuffer[index++] = (byte) (this.lightValue & 0xFF);
			memoryBuffer[index++] = (byte) (this.lightValue >> 8);
	
			memoryBuffer[index++] = (byte) (this.beepType[0] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepType[0] >> 8);
			memoryBuffer[index++] = (byte) (this.beepType[1] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepType[1] >> 8);
			memoryBuffer[index++] = (byte) (this.beepType[2] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepType[2] >> 8);
			memoryBuffer[index++] = (byte) (this.beepType[3] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepType[3] >> 8);
	
			memoryBuffer[index++] = (byte) (this.beepEnable[0] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepEnable[0] >> 8);
			memoryBuffer[index++] = (byte) (this.beepEnable[1] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepEnable[1] >> 8);
			memoryBuffer[index++] = (byte) (this.beepEnable[2] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepEnable[2] >> 8);
			memoryBuffer[index++] = (byte) (this.beepEnable[3] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepEnable[3] >> 8);
	
			memoryBuffer[index++] = (byte) (this.beepVOL[0] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepVOL[0] >> 8);
			memoryBuffer[index++] = (byte) (this.beepVOL[1] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepVOL[1] >> 8);
			memoryBuffer[index++] = (byte) (this.beepVOL[2] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepVOL[2] >> 8);
			memoryBuffer[index++] = (byte) (this.beepVOL[3] & 0xFF);
			memoryBuffer[index++] = (byte) (this.beepVOL[3] >> 8);
	
			memoryBuffer[index++] = (byte) (this.selectLanguage & 0xFF);
			memoryBuffer[index++] = (byte) (this.selectLanguage >> 8);
			
			memoryBuffer[index++] = (byte) (this.selectAdj & 0xFF);
			memoryBuffer[index++] = (byte) (this.selectAdj >> 8);
			memoryBuffer[index++] = (byte) (this.systemVersion & 0xFF);
			memoryBuffer[index++] = (byte) (this.systemVersion >> 8);
	
			memoryBuffer[index++] = (byte) (this.selInputSource & 0xFF);
			memoryBuffer[index++] = (byte) (this.selInputSource >> 8);

			memoryBuffer[index++] = (byte) (this.xInputSources[0].inputLowVolt & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[0].inputLowVolt >> 8);			
			memoryBuffer[index++] = (byte) (this.xInputSources[0].inputCurrentLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[0].inputCurrentLimit >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[0].chargePower & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[0].chargePower >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[0].regEnable & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[0].regEnable >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[0].regVoltLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[0].regVoltLimit >> 8);			
			memoryBuffer[index++] = (byte) (this.xInputSources[0].regCurrentLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[0].regCurrentLimit >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[0].regPowerLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[0].regPowerLimit >> 8);		
			memoryBuffer[index++] = (byte) (this.xInputSources[0].regCapLimit & 0xFF);
			memoryBuffer[index++] = (byte) ((this.xInputSources[0].regCapLimit & 0x0000FF00) >> 8);
			memoryBuffer[index++] = (byte) ((this.xInputSources[0].regCapLimit & 0x00FF0000) >> 16);
			memoryBuffer[index++] = (byte) (this.xInputSources[0].regCapLimit >> 24);

			memoryBuffer[index++] = (byte) (this.xInputSources[1].inputLowVolt & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[1].inputLowVolt >> 8);			
			memoryBuffer[index++] = (byte) (this.xInputSources[1].inputCurrentLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[1].inputCurrentLimit >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[1].chargePower & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[1].chargePower >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[1].regEnable & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[1].regEnable >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[1].regVoltLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[1].regVoltLimit >> 8);			
			memoryBuffer[index++] = (byte) (this.xInputSources[1].regCurrentLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[1].regCurrentLimit >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[1].regPowerLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[1].regPowerLimit >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[1].regCapLimit & 0xFF);
			memoryBuffer[index++] = (byte) ((this.xInputSources[1].regCapLimit & 0x0000FF00) >> 8);
			memoryBuffer[index++] = (byte) ((this.xInputSources[1].regCapLimit & 0x00FF0000) >> 16);
			memoryBuffer[index++] = (byte) (this.xInputSources[1].regCapLimit >> 24);

			memoryBuffer[index++] = (byte) (this.xInputSources[2].inputLowVolt & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[2].inputLowVolt >> 8);			
			memoryBuffer[index++] = (byte) (this.xInputSources[2].inputCurrentLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[2].inputCurrentLimit >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[2].chargePower & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[2].chargePower >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[2].regEnable & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[2].regEnable >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[2].regVoltLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[2].regVoltLimit >> 8);			
			memoryBuffer[index++] = (byte) (this.xInputSources[2].regCurrentLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[2].regCurrentLimit >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[2].regPowerLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[2].regPowerLimit >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[2].regCapLimit & 0xFF);
			memoryBuffer[index++] = (byte) ((this.xInputSources[2].regCapLimit & 0x0000FF00) >> 8);
			memoryBuffer[index++] = (byte) ((this.xInputSources[2].regCapLimit & 0x00FF0000) >> 16);
			memoryBuffer[index++] = (byte) (this.xInputSources[2].regCapLimit >> 24);

			memoryBuffer[index++] = (byte) (this.xInputSources[3].inputLowVolt & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[3].inputLowVolt >> 8);			
			memoryBuffer[index++] = (byte) (this.xInputSources[3].inputCurrentLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[3].inputCurrentLimit >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[3].chargePower & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[3].chargePower >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[3].regEnable & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[3].regEnable >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[3].regVoltLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[3].regVoltLimit >> 8);			
			memoryBuffer[index++] = (byte) (this.xInputSources[3].regCurrentLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[3].regCurrentLimit >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[3].regPowerLimit & 0xFF);
			memoryBuffer[index++] = (byte) (this.xInputSources[3].regPowerLimit >> 8);
			memoryBuffer[index++] = (byte) (this.xInputSources[3].regCapLimit & 0xFF);
			memoryBuffer[index++] = (byte) ((this.xInputSources[3].regCapLimit & 0x0000FF00) >> 8);
			memoryBuffer[index++] = (byte) ((this.xInputSources[3].regCapLimit & 0x00FF0000) >> 16);
			memoryBuffer[index++] = (byte) (this.xInputSources[3].regCapLimit >> 24);
			
			if (isDx) {
				memoryBuffer[index++] = (byte) (this.chargePower[0] & 0xFF);
				memoryBuffer[index++] = (byte) (this.chargePower[0] >> 8);
				memoryBuffer[index++] = (byte) (this.chargePower[1] & 0xFF);
				memoryBuffer[index++] = (byte) (this.chargePower[1] >> 8);
				
				memoryBuffer[index++] = (byte) (this.dischargePower[0] & 0xFF);
				memoryBuffer[index++] = (byte) (this.dischargePower[0] >> 8);
				memoryBuffer[index++] = (byte) (this.dischargePower[1] & 0xFF);
				memoryBuffer[index++] = (byte) (this.dischargePower[1] >> 8);
				
				memoryBuffer[index++] = (byte) (this.proPower & 0xFF);
				memoryBuffer[index++] = (byte) (this.proPower >> 8);

				memoryBuffer[index++] = (byte) (this.monitorLogInterval[0] & 0xFF);
				memoryBuffer[index++] = (byte) (this.monitorLogInterval[0] >> 8);
				memoryBuffer[index++] = (byte) (this.monitorLogInterval[1] & 0xFF);
				memoryBuffer[index++] = (byte) (this.monitorLogInterval[1] >> 8);

				memoryBuffer[index++] = (byte) (this.monitorLogSaveToSD[0] & 0xFF);
				memoryBuffer[index++] = (byte) (this.monitorLogSaveToSD[0] >> 8);
				memoryBuffer[index++] = (byte) (this.monitorLogSaveToSD[1] & 0xFF);
				memoryBuffer[index++] = (byte) (this.monitorLogSaveToSD[1] >> 8);
			}
			else {
				memoryBuffer[index++] = (byte) (this.xDischargePower & 0xFF);
				memoryBuffer[index++] = (byte) (this.xDischargePower >> 8);
				memoryBuffer[index++] = (byte) (this.xMonitorLogInterval & 0xFF);
				memoryBuffer[index++] = (byte) (this.xMonitorLogInterval >> 8);
				memoryBuffer[index++] = (byte) (this.xMonitorLogSaveToSD & 0xFF);
				memoryBuffer[index++] = (byte) (this.xMonitorLogSaveToSD >> 8);
			}
			
			memoryBuffer[index++] = (byte) (this.servoType & 0xFF);
			memoryBuffer[index++] = (byte) (this.servoType >> 8);
			memoryBuffer[index++] = (byte) (this.servoUserCenter & 0xFF);
			memoryBuffer[index++] = (byte) (this.servoUserCenter >> 8);
			memoryBuffer[index++] = (byte) (this.servoUserRate & 0xFF);
			memoryBuffer[index++] = (byte) (this.servoUserRate >> 8);
			memoryBuffer[index++] = (byte) (this.servoUserOpAngle & 0xFF);
			memoryBuffer[index++] = (byte) (this.servoUserOpAngle >> 8);
			
			if (isDx) {
				memoryBuffer[index++] = (byte) (this.servoSpeedVolt & 0xFF);
				memoryBuffer[index++] = (byte) (this.servoSpeedVolt >> 8);
				memoryBuffer[index++] = (byte) (this.antiSparkEnable & 0xFF);
				memoryBuffer[index++] = (byte) (this.antiSparkEnable >> 8);
			}

			memoryBuffer[index++] = (byte) (this.dump[0] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[0] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[1] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[1] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[2] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[2] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[3] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[3] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[4] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[4] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[5] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[5] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[6] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[6] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[7] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[7] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[8] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[8] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[9] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[9] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[10] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[10] >> 8);
			memoryBuffer[index++] = (byte) (this.dump[11] & 0xFF);
			memoryBuffer[index++] = (byte) (this.dump[11] >> 8);
			if (isDx) {
				memoryBuffer[index++] = (byte) (this.dump[12] & 0xFF);
				memoryBuffer[index++] = (byte) (this.dump[12] >> 8);
				memoryBuffer[index++] = (byte) (this.dump[13] & 0xFF);
				memoryBuffer[index++] = (byte) (this.dump[13] >> 8);
				memoryBuffer[index++] = (byte) (this.dump[14] & 0xFF);
				memoryBuffer[index++] = (byte) (this.dump[14] >> 8);
			}
		}

		return memoryBuffer;
	}

	public String toString(final boolean isDuo, final boolean isDx) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName()).append(":\n");
		sb.append(String.format("TempUnit \t\t= %d", this.tempUnit)).append("\n");
		sb.append(String.format("TempStop \t\t= %d", this.tempStop)).append("\n");
		sb.append(String.format("TempFansOn	\t= %d", this.tempFansOn)).append("\n");
		sb.append(String.format("TempReduce	\t= %d", this.tempReduce)).append("\n");
		//DUO reserved	sb.append(String.format("FansSpeed \t\t= %d", this.fansSpeed)).append("\n");
		if (isDx)
			sb.append(String.format("UsbPdEnable \t\t= %d", this.usbPdEnable)).append("\n");
		sb.append(String.format("FansOffDelay \t\t= %d", this.fansOffDelay)).append("\n");
		sb.append(String.format("LcdContraste \t\t= %d", this.lcdContraste)).append("\n");
		sb.append(String.format("LightValue \t\t= %d", this.lightValue)).append("\n");
		//DUO reserved	sb.append(String.format("BeepMode \t\t= %d", this.beepMode)).append("\n");
		sb.append(String.format("BeepType \t\t= [%d, %d, %d, %d]", this.beepType[0], this.beepType[1], this.beepType[2], this.beepType[3])).append("\n");
		sb.append(String.format("BeepEnable \t\t= [%d, %d, %d, %d]", this.beepEnable[0], this.beepEnable[1], this.beepEnable[2], this.beepEnable[3])).append("\n");
		sb.append(String.format("BeepVOL \t\t= [%d, %d, %d, %d]", this.beepVOL[0], this.beepVOL[1], this.beepVOL[2], this.beepVOL[3])).append("\n");
		sb.append(String.format("SelectLanguage \t\t= %d", this.selectLanguage)).append("\n");
		sb.append(String.format("SelectAdj \t\t= %d", this.selectAdj)).append("\n");
		sb.append(String.format("SystemVersion \t\t= %d", this.systemVersion)).append("\n");
		sb.append(String.format("SelInputSource	\t= %d", this.selInputSource)).append("\n").append("\n");
		if (isDuo) {
			sb.append(String.format("DCInputLowVolt	\t= %d", this.dcInputLowVolt)).append("\n");
			sb.append(String.format("DCInputOverVolt \t= %d", this.dcInputOverVolt)).append("\n");
			sb.append(String.format("DCInputCurrentLimit \t= %d", this.dcInputCurrentLimit)).append("\n");
			sb.append(String.format("BatInputLowVolt \t= %d", this.batInputLowVolt)).append("\n");
			sb.append(String.format("BatInputOverVolt	= %d", this.batInputOverVolt)).append("\n");
			sb.append(String.format("BatInputCurrentLimit	= %d", this.batInputCurrentLimit)).append("\n");
			sb.append(String.format("RegEnable \t\t= %d", this.regEnable)).append("\n");
			sb.append(String.format("RegVoltLimit	\t= %d", this.regVoltLimit)).append("\n");
			sb.append(String.format("RegCurrentLimit \t= %d", this.regCurrentLimit)).append("\n");
			sb.append(String.format("RegCapacityLimit \t= %d", this.regCapacityLimit)).append("\n");
			sb.append(String.format("ChargePower \t\t= [%d, %d]", this.chargePower[0], this.chargePower[1])).append("\n");
			sb.append(String.format("DischargePower \t\t= [%d, %d]", this.dischargePower[0], this.dischargePower[1])).append("\n");
			sb.append(String.format("ProPower \t\t= %d", this.batInputLowVolt)).append("\n");
			sb.append(String.format("MonitorLogInterval \t= [%d, %d]", this.monitorLogInterval[0], this.monitorLogInterval[1])).append("\n");
			sb.append(String.format("MonitorLogSaveToSD \t= [%d, %d]", this.monitorLogSaveToSD[0], this.monitorLogSaveToSD[1])).append("\n");
			sb.append("\n");
			sb.append(String.format("ServoType \t\t= %d", this.servoType)).append("\n");
			sb.append(String.format("ServoUserCenter \t= %d", this.servoUserCenter)).append("\n");
			sb.append(String.format("ServoUserRate \t\t= %d", this.servoUserRate)).append("\n");
			sb.append(String.format("ServoUserOpAngle \t= %d", this.servoUserOpAngle)).append("\n");
	
			sb.append(String.format("ModBusMode \t\t= %d", this.modBusMode)).append("\n");
			sb.append(String.format("SerialModBusAddr \t= %d", this.serialModBusAddr)).append("\n");
			sb.append(String.format("SerialModBusBaudeRate \t= %d", this.serialModBusBaudeRate)).append("\n");
			sb.append(String.format("SerialModBusParity \t= %d", this.serialModBusParity)).append("\n");
	
			sb.append(String.format("Dump \t\t\t= [%d, %d, %d, %d ,%d, %d, %d]", this.dump[0], this.dump[1], this.dump[2], this.dump[3], this.dump[4], this.dump[5], this.dump[6])).append("\n");
		}
		else {
			sb.append(String.format("[0]inputLowVolt \t= %d", xInputSources[0].inputLowVolt)).append("\n");
			sb.append(String.format("[0]inputCurrentLimit \t= %d", xInputSources[0].inputCurrentLimit)).append("\n");
			sb.append(String.format("[0]chargePower \t\t= %d", xInputSources[0].chargePower)).append("\n");
			sb.append(String.format("[0]regEnable \t\t= %d", xInputSources[0].regEnable)).append("\n");
			sb.append(String.format("[0]regVoltLimit \t= %d", xInputSources[0].regVoltLimit)).append("\n");
			sb.append(String.format("[0]regCurrentLimit \t= %d", xInputSources[0].regCurrentLimit)).append("\n");
			sb.append(String.format("[0]regPowerLimit \t= %d", xInputSources[0].regPowerLimit)).append("\n");
			sb.append(String.format("[0]regCapLimit \t\t= %d", xInputSources[0].regCapLimit)).append("\n");
			sb.append("\n");

			sb.append(String.format("[1]inputLowVolt \t= %d", xInputSources[1].inputLowVolt)).append("\n");
			sb.append(String.format("[1]inputCurrentLimit \t= %d", xInputSources[1].inputCurrentLimit)).append("\n");
			sb.append(String.format("[1]chargePower \t\t= %d", xInputSources[1].chargePower)).append("\n");
			sb.append(String.format("[1]regEnable \t\t= %d", xInputSources[1].regEnable)).append("\n");
			sb.append(String.format("[1]regVoltLimit \t= %d", xInputSources[1].regVoltLimit)).append("\n");
			sb.append(String.format("[1]regCurrentLimit \t= %d", xInputSources[1].regCurrentLimit)).append("\n");
			sb.append(String.format("[1]regPowerLimit \t= %d", xInputSources[1].regPowerLimit)).append("\n");
			sb.append(String.format("[1]regCapLimit \t\t= %d", xInputSources[1].regCapLimit)).append("\n");
			sb.append("\n");

			sb.append(String.format("[2]inputLowVolt \t= %d", xInputSources[2].inputLowVolt)).append("\n");
			sb.append(String.format("[2]inputCurrentLimit \t= %d", xInputSources[2].inputCurrentLimit)).append("\n");
			sb.append(String.format("[2]chargePower \t\t= %d", xInputSources[2].chargePower)).append("\n");
			sb.append(String.format("[2]regEnable \t\t= %d", xInputSources[2].regEnable)).append("\n");
			sb.append(String.format("[2]regVoltLimit \t= %d", xInputSources[2].regVoltLimit)).append("\n");
			sb.append(String.format("[2]regCurrentLimit \t= %d", xInputSources[2].regCurrentLimit)).append("\n");
			sb.append(String.format("[2]regPowerLimit \t= %d", xInputSources[2].regPowerLimit)).append("\n");
			sb.append(String.format("[2]regCapLimit \t\t= %d", xInputSources[2].regCapLimit)).append("\n");
			sb.append("\n");

			sb.append(String.format("[3]inputLowVolt \t= %d", xInputSources[3].inputLowVolt)).append("\n");
			sb.append(String.format("[3]inputCurrentLimit \t= %d", xInputSources[3].inputCurrentLimit)).append("\n");
			sb.append(String.format("[3]chargePower \t\t= %d", xInputSources[3].chargePower)).append("\n");
			sb.append(String.format("[3]regEnable \t\t= %d", xInputSources[3].regEnable)).append("\n");
			sb.append(String.format("[3]regVoltLimit \t= %d", xInputSources[3].regVoltLimit)).append("\n");
			sb.append(String.format("[3]regCurrentLimit \t= %d", xInputSources[3].regCurrentLimit)).append("\n");
			sb.append(String.format("[3]regPowerLimit \t= %d", xInputSources[3].regPowerLimit)).append("\n");
			sb.append(String.format("[3]regCapLimit \t\t= %d", xInputSources[3].regCapLimit)).append("\n");
			sb.append("\n");

			if (isDx) {
				sb.append(String.format("ChargePower \t\t= [%d, %d]", this.chargePower[0], this.chargePower[1])).append("\n");
				sb.append(String.format("DischargePower \t\t= [%d, %d]", this.dischargePower[0], this.dischargePower[1])).append("\n");
				sb.append(String.format("ProPower \t\t= %d", this.batInputLowVolt)).append("\n");
				sb.append(String.format("MonitorLogInterval \t= [%d, %d]", this.monitorLogInterval[0], this.monitorLogInterval[1])).append("\n");
				sb.append(String.format("MonitorLogSaveToSD \t= [%d, %d]", this.monitorLogSaveToSD[0], this.monitorLogSaveToSD[1])).append("\n");
				sb.append("\n");
			}
			else {
				sb.append(String.format("DischargePower \t\t= %d", this.xDischargePower)).append("\n");
				sb.append(String.format("MonitorLogInterval \t= %d", this.xMonitorLogInterval)).append("\n");
				sb.append(String.format("MonitorLogSaveToSD \t= %d", this.xMonitorLogSaveToSD)).append("\n");
				sb.append("\n");
			}
	
			sb.append(String.format("ServoType \t\t= %d", this.servoType)).append("\n");
			sb.append(String.format("ServoUserCenter \t= %d", this.servoUserCenter)).append("\n");
			sb.append(String.format("ServoUserRate \t\t= %d", this.servoUserRate)).append("\n");
			sb.append(String.format("ServoUserOpAngle \t= %d", this.servoUserOpAngle)).append("\n");
			if (isDx) {
				sb.append(String.format("ServoSpeedVolt \t\t= %d", this.servoSpeedVolt)).append("\n");
				sb.append(String.format("AntiSparkEnable \t= %d", this.antiSparkEnable)).append("\n");
			}
	
			if (isDx)
				sb.append(String.format("Dump \t\t\t= [%d, %d, %d, %d ,%d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d]", this.dump[0], this.dump[1], this.dump[2], this.dump[3], this.dump[4], this.dump[5], this.dump[6], this.dump[7],
						this.dump[8], this.dump[9], this.dump[10], this.dump[11], this.dump[12], this.dump[13], this.dump[14])).append("\n");
			else
				sb.append(String.format("Dump \t\t\t= [%d, %d, %d, %d ,%d, %d, %d, %d, %d, %d, %d, %d]", this.dump[0], this.dump[1], this.dump[2], this.dump[3], this.dump[4], this.dump[5], this.dump[6], this.dump[7],
						this.dump[8], this.dump[9], this.dump[10], this.dump[11])).append("\n");

		}
		return sb.toString();
	}

	final static int size = 60 * 2; //size in byte

	public static int getSize(final boolean isDuo, final boolean isDx) {
		return isDuo ? ChargerSystem.size : isDx ? 180 : 156; //x=76x2=152, Dx=81x2=162
	}

	public short getTempUnit() {
		return this.tempUnit;
	}

	public void setTempUnit(short tempUnit) {
		this.tempUnit = tempUnit;
	}

	public short getTempStop() {
		return this.tempStop;
	}

	public void setTempStop(short tempStop) {
		this.tempStop = tempStop;
	}

	public short getTempFansOn() {
		return this.tempFansOn;
	}

	public void setTempFansOn(short tempFansOn) {
		this.tempFansOn = tempFansOn;
	}

	public short getTempReduce() {
		return this.tempReduce;
	}

	public void setTempReduce(short tempReduce) {
		this.tempReduce = tempReduce;
	}

	public short getFansSpeed() {
		return this.fansSpeed;
	}

	public void setFansSpeed(short fansSpeed) {
		this.fansSpeed = fansSpeed;
	}

	public short getUsbPdEnable() {
		return usbPdEnable;
	}

	public void setUsbPdEnable(short usbPdEnable) {
		this.usbPdEnable = usbPdEnable;
	}

	public short getFansOffDelay() {
		return this.fansOffDelay;
	}

	public void setFansOffDelay(short fansOffDelay) {
		this.fansOffDelay = fansOffDelay;
	}

	public short getLcdContraste() {
		return this.lcdContraste;
	}

	/**
	 * @return the xDischargePower
	 */
	public short getxDischargePower() {
		return xDischargePower;
	}

	/**
	 * @param xDischargePower the xDischargePower to set
	 */
	public void setxDischargePower(short xDischargePower) {
		this.xDischargePower = xDischargePower;
	}

	public void setLcdContraste(short lcdContraste) {
		this.lcdContraste = lcdContraste;
	}

	public short getLightValue() {
		return this.lightValue;
	}

	public void setLightValue(short lightValue) {
		this.lightValue = lightValue;
	}

	public short getBeepMode() {
		return this.beepMode;
	}

	public void setBeepMode(short beepMode) {
		this.beepMode = beepMode;
	}

	public short[] getBeepType() {
		return this.beepType;
	}

	public void setBeepType(short[] beepType) {
		this.beepType = beepType;
	}

	public short[] getBeepEnable() {
		return this.beepEnable;
	}

	public void setBeepEnable(short[] beepEnable) {
		this.beepEnable = beepEnable;
	}

	public short[] getBeepVOL() {
		return this.beepVOL;
	}

	public void setBeepVOL(short[] beepVOL) {
		this.beepVOL = beepVOL;
	}

	public short getSelectAdj() {
		return this.selectAdj;
	}

	public void setSelectAdj(short selectAdj) {
		this.selectAdj = selectAdj;
	}

	public short getVer() {
		return this.systemVersion;
	}

	public void setVer(short ver) {
		this.systemVersion = ver;
	}

	public short getSelInputSource() {
		return this.selInputSource;
	}

	public void setSelInputSource(short selInputSource) {
		this.selInputSource = selInputSource;
	}

	public short getDCInputLowVolt() {
		return this.dcInputLowVolt;
	}

	public void setDCInputLowVolt(short dCInputLowVolt) {
		this.dcInputLowVolt = dCInputLowVolt;
	}

	public short getDCInputOverVolt() {
		return this.dcInputOverVolt;
	}

	public void setDCInputOverVolt(short dCInputOverVolt) {
		this.dcInputOverVolt = dCInputOverVolt;
	}

	public short getDCInputCurrentLimit() {
		return this.dcInputCurrentLimit;
	}

	public void setDCInputCurrentLimit(short dCInputCurrentLimit) {
		this.dcInputCurrentLimit = dCInputCurrentLimit;
	}

	public short getBatInputLowVolt() {
		return this.batInputLowVolt;
	}

	public void setBatInputLowVolt(short batInputLowVolt) {
		this.batInputLowVolt = batInputLowVolt;
	}

	public short getBatInputOverVolt() {
		return this.batInputOverVolt;
	}

	public void setBatInputOverVolt(short batInputOverVolt) {
		this.batInputOverVolt = batInputOverVolt;
	}

	public short getBatInputCurrentLimit() {
		return this.batInputCurrentLimit;
	}

	public void setBatInputCurrentLimit(short batInputCurrentLimit) {
		this.batInputCurrentLimit = batInputCurrentLimit;
	}

	public short getRegEnable() {
		return this.regEnable;
	}

	public void setRegEnable(short regEnable) {
		this.regEnable = regEnable;
	}

	public short getRegVoltLimit() {
		return this.regVoltLimit;
	}

	public void setRegVoltLimit(short regVoltLimit) {
		this.regVoltLimit = regVoltLimit;
	}

	public short getRegCurrentLimit() {
		return this.regCurrentLimit;
	}

	public void setRegCurrentLimit(short regCurrentLimit) {
		this.regCurrentLimit = regCurrentLimit;
	}

	public int getRegCapacityLimit() {
		return regCapacityLimit;
	}

	public void setRegCapacityLimit(int regCapacityLimit) {
		this.regCapacityLimit = regCapacityLimit;
	}

	public short[] getChargePower() {
		return this.chargePower;
	}

	public void setChargePower(short[] chargePower) {
		this.chargePower = chargePower;
	}

	public short[] getDischargePower() {
		return this.dischargePower;
	}

	public void setDischargePower(short[] dischargePower) {
		this.dischargePower = dischargePower;
	}

	public short getProPower() {
		return this.proPower;
	}

	public void setProPower(short proPower) {
		this.proPower = proPower;
	}

	public short[] getMonitorLogInterval() {
		return this.monitorLogInterval;
	}

	public void setMonitorLogInterval(short[] monitorLogInterval) {
		this.monitorLogInterval = monitorLogInterval;
	}

	public short[] getMonitorLogSaveToSD() {
		return this.monitorLogSaveToSD;
	}

	public void setMonitorLogSaveToSD(short[] monitorLogSaveToSD) {
		this.monitorLogSaveToSD = monitorLogSaveToSD;
	}

	public short getServoType() {
		return this.servoType;
	}

	public void setServoType(short servoType) {
		this.servoType = servoType;
	}

	public short getServoUserCenter() {
		return this.servoUserCenter;
	}

	public void setServoUserCenter(short servoUserCenter) {
		this.servoUserCenter = servoUserCenter;
	}

	public short getServoUserRate() {
		return this.servoUserRate;
	}

	public void setServoUserRate(short servoUserRate) {
		this.servoUserRate = servoUserRate;
	}

	public short getServoUserOpAngle() {
		return this.servoUserOpAngle;
	}

	public void setServoUserOpAngle(short servoUserOpAngle) {
		this.servoUserOpAngle = servoUserOpAngle;
	}

	public short getServoSpeedVolt() {
		return servoSpeedVolt;
	}

	public void setServoSpeedVolt(short servoSpeedVolt) {
		this.servoSpeedVolt = servoSpeedVolt;
	}

	public short getAntiSparkEnable() {
		return antiSparkEnable;
	}

	public void setAntiSparkEnabl(short antiSparkEnable) {
		this.antiSparkEnable = antiSparkEnable;
	}

	public short getModBusMode() {
		return this.modBusMode;
	}

	public void setModBusMode(short modBusMode) {
		this.modBusMode = modBusMode;
	}

	public short getModBusAddr() {
		return this.serialModBusAddr;
	}

	public void setModBusAddr(short modBusAddr) {
		this.serialModBusAddr = modBusAddr;
	}

	public short[] getDump() {
		return this.dump;
	}

	public void setDump(short[] dump) {
		this.dump = dump;
	}

	public int[] getSystemValues(final int[] values) {

		values[0] = this.getTempUnit(); //0 temperature unit
		values[1] = this.getTempStop(); //1 shut down temperature
		values[2] = this.getTempReduce(); //2 power reduce delta
		values[3] = this.getTempFansOn(); //3 Cooling fan on temperature
		values[4] = this.getFansOffDelay(); //4 Cooling fan off delay time

		values[5] = this.getBeepEnable()[0] != 0 ? this.getBeepVOL()[0] : 0; //5 Beep volume buttons
		values[6] = this.getBeepEnable()[1] != 0 ? this.getBeepVOL()[1] : 0; //6 Beep volume tip
		values[7] = this.getBeepEnable()[2] != 0 ? this.getBeepVOL()[2] : 0; //7 Beep volume alarm
		values[8] = this.getBeepEnable()[3] != 0 ? this.getBeepVOL()[3] : 0; //8 Beep volume end
		values[9] = this.getBeepType()[3]; //9 Beep type

		values[10] = this.getLightValue(); //10 LCD brightness
		values[11] = this.getLcdContraste(); //11 LCD contrast

		values[12] = this.getChargePower()[0];//12 Charge power channel 1
		values[13] = this.getChargePower()[1];//13 Charge power channel 2
		values[14] = this.getDischargePower()[0];//14 Charge power channel 1
		values[15] = this.getDischargePower()[1];//15 Charge power channel 2
		values[16] = this.getProPower(); //16 Power distribution

		values[17] = this.getSelInputSource(); //17 input select

		values[18] = this.getxDischargePower(); //18 discharge power Limit
		if (this.xInputSources[values[17]] != null) {
			values[19] = this.xInputSources[values[17]].getInputLowVolt(); //19 inputLowVolt
			values[20] = this.xInputSources[values[17]].getInputCurrentLimit(); //20 inputCurrentLimit
			values[21] = this.xInputSources[values[17]].getChargePower(); //21 Charge power 
			values[22] = this.xInputSources[values[17]].getRegEnable(); //22 regEnable
			values[23] = this.xInputSources[values[17]].getRegVoltLimit(); //23 regVoltLimit
			values[24] = this.xInputSources[values[17]].getRegCurrentLimit(); //24 regCurrentLimit
			values[25] = this.xInputSources[values[17]].getRegPowerLimit(); //25 regPowerLimit
			values[26] = this.xInputSources[values[17]].getRegCapLimit(); //26 regCapLimit
		}
		values[27] = this.getSelectLanguage(); //27 language 0=en 1=de
		
		//duo input source configuration
		values[28] = this.getDcInputLowVolt();
		values[29] = this.getDcInputCurrentLimit();
		values[30] = this.getBatInputLowVolt();
		values[31] = this.getBatInputCurrentLimit();
		values[32] = this.getRegEnable();
		values[33] = this.getRegVoltLimit();
		values[34] = this.getRegCurrentLimit();
		values[35] = this.getRegCapacityLimit() / 100;
		
		//DX input anti spark
		values[36] = this.getAntiSparkEnable();

		return values;
	}

	/**
	 * @return the selectLanguage
	 */
	public short getSelectLanguage() {
		return selectLanguage;
	}

	/**
	 * @param selectLanguage the selectLanguage to set
	 */
	public void setSelectLanguage(short selectLanguage) {
		this.selectLanguage = selectLanguage;
	}

	/**
	 * @return the dcInputLowVolt
	 */
	public short getDcInputLowVolt() {
		return dcInputLowVolt;
	}

	/**
	 * @param dcInputLowVolt the dcInputLowVolt to set
	 */
	public void setDcInputLowVolt(short dcInputLowVolt) {
		this.dcInputLowVolt = dcInputLowVolt;
	}

	/**
	 * @return the dcInputOverVolt
	 */
	public short getDcInputOverVolt() {
		return dcInputOverVolt;
	}

	/**
	 * @param dcInputOverVolt the dcInputOverVolt to set
	 */
	public void setDcInputOverVolt(short dcInputOverVolt) {
		this.dcInputOverVolt = dcInputOverVolt;
	}

	/**
	 * @return the dcInputCurrentLimit
	 */
	public short getDcInputCurrentLimit() {
		return dcInputCurrentLimit;
	}

	/**
	 * @param dcInputCurrentLimit the dcInputCurrentLimit to set
	 */
	public void setDcInputCurrentLimit(short dcInputCurrentLimit) {
		this.dcInputCurrentLimit = dcInputCurrentLimit;
	}

}
