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

    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.graupner.HoTTAdapter.PickerParameters;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

/**
 * Class to read Graupner HoTT binary data as saved on SD-Cards
 * @author Winfried Brügmann
 */
public class HoTTbinReaderD extends HoTTbinReader2 {
	final static Logger							logger						= Logger.getLogger(HoTTbinReaderD.class.getName());
	

	public static class ChnBinParser extends BinParser {
		protected final byte[] _buf;

		protected ChnBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
			throw new UnsupportedOperationException("use in situ parsing");
		}

		protected ChnBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.CHANNEL);
			_buf = buffers[0];
			if (buffers.length != 1) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=Ch17 ... 118=Ch32 119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
			this.points[4] = (this._buf[3] & 0xFF) * -1000;
			this.points[5] = (this._buf[4] & 0xFF) * -1000;

			this.points[87] = (DataParser.parse2UnsignedShort(this._buf, 8) / 2) * 1000;
			this.points[88] = (DataParser.parse2UnsignedShort(this._buf, 10) / 2) * 1000;
			this.points[89] = (DataParser.parse2UnsignedShort(this._buf, 12) / 2) * 1000;
			this.points[90] = (DataParser.parse2UnsignedShort(this._buf, 14) / 2) * 1000;
			this.points[91] = (DataParser.parse2UnsignedShort(this._buf, 16) / 2) * 1000;
			this.points[92] = (DataParser.parse2UnsignedShort(this._buf, 18) / 2) * 1000;
			this.points[93] = (DataParser.parse2UnsignedShort(this._buf, 20) / 2) * 1000;
			this.points[94] = (DataParser.parse2UnsignedShort(this._buf, 22) / 2) * 1000;

			if (this._buf[5] == 0x00) { // channel 9-12
				this.points[95] = (DataParser.parse2UnsignedShort(this._buf, 24) / 2) * 1000;
				this.points[96] = (DataParser.parse2UnsignedShort(this._buf, 26) / 2) * 1000;
				this.points[97] = (DataParser.parse2UnsignedShort(this._buf, 28) / 2) * 1000;
				this.points[98] = (DataParser.parse2UnsignedShort(this._buf, 30) / 2) * 1000;
				if (this.points[99] == 0) {
					this.points[99] = 1500 * 1000;
					this.points[100] = 1500 * 1000;
					this.points[101] = 1500 * 1000;
					this.points[102] = 1500 * 1000;
				}
			} else { // channel 13-16
				this.points[99] = (DataParser.parse2UnsignedShort(this._buf, 24) / 2) * 1000;
				this.points[100] = (DataParser.parse2UnsignedShort(this._buf, 26) / 2) * 1000;
				this.points[101] = (DataParser.parse2UnsignedShort(this._buf, 28) / 2) * 1000;
				this.points[102] = (DataParser.parse2UnsignedShort(this._buf, 30) / 2) * 1000;
				if (this.points[95] == 0) {
					this.points[95] = 1500 * 1000;
					this.points[96] = 1500 * 1000;
					this.points[97] = 1500 * 1000;
					this.points[98] = 1500 * 1000;
				}
			}
			// events 119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
			this.points[119] = (this._buf[50] & 0x01) * 100000;
			this.points[120] = (this._buf[50] & 0x02) * 50000;
			this.points[121] = (this._buf[50] & 0x04) * 25000;
			if (this._buf[37] > 0 && this._buf[37] < 27)
				this.points[122] = this._buf[32] * 1000; // warning
			else
				this.points[122] = 0;
			return true;
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			for (int j = 4; j < 7; j++) {
				targetPoints[j] = this.points[j];
			}
			// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 
			for (int j = 87; j < 87+16; j++) {
				targetPoints[j] = this.points[j];
			}
			// points.length = 136 -> 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
			// points.length = 152 -> 119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
			throw new UnsupportedOperationException("use in situ parsing");
		}
	}
	
	public static class VarBinParserD extends HoTTbinReader2.VarBinParser {
		private int	tmpHeight		= 0;
		private int	tmpClimb10	= 0;

		protected VarBinParserD(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
		}

		protected VarBinParserD(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.VARIO);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
			points[2] = 100000;
		}

		@Override
		protected boolean parse() {
			//  0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
			// 223=Test 00 224=Test 01.. 235=Test 12
			this.tmpHeight = DataParser.parse2Short(this._buf1, 2) - 500;
			this.tmpClimb10 = DataParser.parse2UnsignedShort(this._buf2, 2) - 30000;
			if (isPointsValid()) {
				if (this.pickerParameters.altitudeClimbSensorSelection == 1) { //sensor selection GPS (auto, Vario, GPS, GAM, EAM)
					this.points[10] = this.tmpHeight * 1000;
					// pointsVarioMax = DataParser.parse2Short(buf1, 4) * 1000;
					// pointsVarioMin = DataParser.parse2Short(buf1, 6) * 1000;
					this.points[11] = (DataParser.parse2UnsignedShort(this._buf1, 8) - 30000) * 10;
					this.points[12] = (DataParser.parse2UnsignedShort(this._buf2, 0) - 30000) * 10;
					this.points[13] = this.tmpClimb10 * 10;
				}
				this.points[14] = (this._buf1[1] & 0x3F) * 1000; // inverse event
				
				if ((_buf4[9] & 0xFF) > 100 && (_buf4[9] & 0xFF) < 120) { //SM MicroVario starts with FW version 1.00 -> 100
					try {
						this.points[15] = Integer.parseInt(String.format(Locale.ENGLISH, "%c%c%c%c%c0", _buf2[4], _buf2[5], _buf2[6], _buf2[8], _buf2[9]).trim());
						this.points[16] = Integer.parseInt(String.format(Locale.ENGLISH, "%c%c%c%c%c0", _buf3[1], _buf3[2], _buf3[3], _buf3[5], _buf3[6]).trim());
						this.points[17] = Integer.parseInt(String.format(Locale.ENGLISH, "%c%c%c%c%c0", _buf3[8], _buf3[9], _buf4[0], _buf4[2], _buf4[3]).trim());
					}
					catch (NumberFormatException e) {
						byte[] tmpArray = new byte[21];
						System.arraycopy(_buf2, 4, tmpArray, 0, 6);
						System.arraycopy(_buf3, 0, tmpArray, 6, 10);
						System.arraycopy(_buf4, 0, tmpArray, 16, 5);
						log.log(Level.WARNING, "'" + new String(tmpArray) + "'");
					}
					this.points[18] = (_buf4[8] & 0xFF) * 1000; //AirSpeed/2
					this.points[19] = (_buf4[9] & 0xFF) * 1000; //SM MicroVario starts with FW version 1.00 -> 100
				} 
				else {
					// 239=Test 00 240=Test 01.. 251=Test 12
					for (int i = 0, j = 0; i < 3; i++, j += 2) {
						HoTTbinReaderD.points[i + 239] = DataParser.parse2Short(_buf2, 4 + j) * 1000;
					}
					for (int i = 0, j = 0; i < 5; i++, j += 2) {
						HoTTbinReaderD.points[i + 242] = DataParser.parse2Short(_buf3, 0 + j) * 1000;
					}
					for (int i = 0, j = 0; i < 5; i++, j += 2) {
						HoTTbinReaderD.points[i + 247] = DataParser.parse2Short(_buf4, 0 + j) * 1000;
					}					
				}
				return true;
			}
			else 
				System.out.println();
			this.points[14] = (this._buf1[1] & 0x3F) * 1000; // inverse event
			return isPointsValid();
		}

		private boolean isPointsValid() {
			return !this.pickerParameters.isFilterEnabled || (this.tmpHeight >= -490 && this.tmpHeight < 5000);
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if (this.points[10] != 0 || this.points[11] != 0 || this.points[12] != 0 || this.points[13] != 0) {
				//10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
				for (int j = 10; j < 20; j++) {
					targetPoints[j] = this.points[j];
				}
				//223=Test 00 224=Test 01.. 235=Test 12
				for (int j = 223; j < 236; j++) {
					targetPoints[j] = this.points[j];
				}
			}
		}
	}
	

	public static class EscBinParser extends BinParser {
		private final boolean	isChannelsChannel;

		private int						tmpTemperatureFet	= 0;
		private int						tmpVoltage				= 0;
		private int						tmpCurrent				= 0;
		private int						tmpRevolution			= 0;
		private int						tmpCapacity				= 0;

		private int						parseCount				= 0;
		
		protected boolean isChannelsChannel() { return this.isChannelsChannel; }

		protected EscBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
		}

		protected EscBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.ESC);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
			this.isChannelsChannel = this.pickerParameters.analyzer.getActiveChannel().getNumber() == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER;
		}

		@Override
		protected boolean parse() {
			this.tmpVoltage = DataParser.parse2Short(this._buf1, 3);
			this.tmpCurrent = DataParser.parse2Short(this._buf2, 1);
			this.tmpCapacity = DataParser.parse2Short(this._buf1, 7);
			this.tmpRevolution = DataParser.parse2UnsignedShort(this._buf2, 5);
			this.tmpTemperatureFet = (this._buf1[9] & 0xFF) - 20;

			// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
			// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
			// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
			// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
			if (isPointsValid()) {
				this.points[123] = this.tmpVoltage * 1000;
				this.points[124] = this.tmpCurrent * 1000;
				this.points[126] = Double.valueOf(this.points[107] / 1000.0 * this.points[108]).intValue();
				if (!this.pickerParameters.isFilterEnabled || this.parseCount <= 20
						|| (this.tmpCapacity != 0 && Math.abs(this.tmpCapacity) <= (this.points[109] / 1000 + this.tmpVoltage * this.tmpCurrent / 2500 + 2))) {
					this.points[125] = this.tmpCapacity * 1000;
				} else {
					if (this.tmpCapacity != 0 && HoTTbinReader2.log.isLoggable(Level.FINE))
						HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", this.getTimeStep_ms()) + " - " + this.tmpCapacity + " - " + (this.points[104] / 1000) + " + " + (this.tmpVoltage * this.tmpCurrent / 2500 + 2));
				}
				this.points[127] = this.tmpRevolution * 1000;
				this.points[128] = this.tmpTemperatureFet * 1000;

				this.points[129] = ((this._buf2[9] & 0xFF) - 20) * 1000;
				this.points[130] = DataParser.parse2Short(this._buf1, 5) * 1000;
				this.points[131] = DataParser.parse2Short(this._buf2, 3) * 1000;
				this.points[132] = DataParser.parse2UnsignedShort(this._buf2, 7) * 1000;
				this.points[133] = ((this._buf2[0] & 0xFF) - 20) * 1000;
				this.points[134] = ((this._buf3[0] & 0xFF) - 20) * 1000;
				this.points[135] = (this._buf1[1] & 0xFF) * 1000; // inverse event
				
				if ((_buf4[9] & 0xFF) == 3) { //Extended YGE protocol 				
					// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
					// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
					this.points[136] = DataParser.parse2Short(_buf3, 1) * 1000; //Speed
					this.points[137] = DataParser.parse2Short(_buf3, 3) * 1000; //Speed max
					this.points[138] = (_buf3[5] & 0xFF) * 1000; 								//PWM
					this.points[139] = (_buf3[6] & 0xFF) * 1000; 								//Throttle
					this.points[140] = (_buf3[7] & 0xFF) * 1000; 								//BEC Voltage
					this.points[141] = (_buf3[8] & 0xFF) * 1000; 								//BEC Voltage min
					this.points[142] = DataParser.parse2UnsignedShort(_buf3[9], _buf4[0]) * 1000; 	//BEC Current
					this.points[143] = ((_buf4[1] & 0xFF) - 20) * 1000; 				//BEC Temperature
					this.points[144] = ((_buf4[2] & 0xFF) - 20) * 1000; 				//Capacity Temperature
					this.points[145] = (_buf4[3] & 0xFF) * 1000; 								//Timing
					this.points[146] = ((_buf4[4] & 0xFF) - 20) * 1000; 				//Aux Temperature
					this.points[147] = DataParser.parse2Short(_buf4, 5) * 1000; //Gear
					this.points[146] = (_buf4[7] & 0xFF) * 1000; 								//YGEGenExt
					this.points[149] = (_buf4[8] & 0xFF) * 1000; 								//MotStatEscNr
					this.points[150] = 0; 																			//spare
					this.points[151] = (_buf4[9] & 0xFF) * 1000; 								//Version ESC
				}
				else if ((_buf4[9] & 0xFF) >= 128) { //Extended CS-Electronics
					//136=AirSpeed 137=AirSpeed_max 138=PWM 139=Throttle 140=VoltagePump 141=VoltagePump_min 142=Flow 143=Fuel 144=Power 
					//145=Thrust 146=TemperaturePump 147=EngineStat 148=spare 149=spare 150=spare 151=version
					this.points[136] = DataParser.parse2Short(_buf3, 1) * 1000; 	//AirSpeed
					this.points[137] = DataParser.parse2Short(_buf3, 3) * 1000; 	//AirSpeed max
					this.points[138] = (_buf3[5] & 0xFF) * 1000; 									//PWM
					this.points[139] = (_buf3[6] & 0xFF) * 1000; 									//Throttle
					this.points[140] = (_buf3[7] & 0xFF) * 1000; 									//Pump Voltage
					this.points[141] = (_buf3[8] & 0xFF) * 1000; 									//Pump Voltage min
					this.points[142] = DataParser.parse2UnsignedShort(_buf3[9], _buf4[0]) * 1000;	//Flow
					this.points[143] = DataParser.parse2UnsignedShort(_buf4, 1) * 1000;						//Fuel ml
					this.points[144] = DataParser.parse2UnsignedShort(_buf4, 3) * 1000; 					//Power Wh
					this.points[145] = DataParser.parse2UnsignedShort(_buf4, 5) * 1000; 					//Thrust
					this.points[146] = ((_buf4[7] & 0xFF) - 20) * 1000; 					//Pump Temperature
					this.points[147] = (_buf4[8] & 0xFF) * 1000; 									//Engine run
					this.points[148] = 0; 																				//spare
					this.points[149] = 0; 																				//spare
					this.points[150] = 0; 																				//spare
					this.points[151] = (_buf4[9] & 0xFF) * 1000; 									//Version ESC			
				}
				return true;
			}
			this.points[135] = (this._buf1[1] & 0xFF) * 1000; // inverse event
			return false;
		}

		private boolean isPointsValid() {
			return !this.pickerParameters.isFilterEnabled
					|| this.tmpVoltage > 0 && this.tmpVoltage < 1000 && this.tmpCurrent < 4000 && this.tmpCurrent > -10 && this.tmpRevolution > -1
					&& this.tmpRevolution < 20000 && !(this.points[112] != 0 && this.points[112] / 1000 - this.tmpTemperatureFet > 20);
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
			// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
			// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
			// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
			for (int j = 123; j < 123+29; j++) {
				targetPoints[j] = this.points[j];
			}
		}
	}

	
	/**
	 * Migrate sensor measurement values in the correct priority and add to record set.
	 * Receiver data are always updated.
	 */
	public static void migrateAddPoints(RecordSet tmpRecordSet, EnumSet<Sensor> migrationJobs, long timeStep_ms, boolean[] isResetMinMax) throws DataInconsitsentException {
		if (migrationJobs.contains(Sensor.EAM)) {
			HoTTbinReaderD.eamBinParser.migratePoints(HoTTbinReaderD.points);
			// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
			// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
			if (!isResetMinMax[1] && HoTTbinReaderD.points[60] != 0) {
				for (int i=60; i<87; ++i) {
					tmpRecordSet.get(i).setMinMax(HoTTbinReaderD.points[i], HoTTbinReaderD.points[i]);
				}
				isResetMinMax[1] = true;
			}
		}
		if (migrationJobs.contains(Sensor.GAM)) {
			HoTTbinReaderD.gamBinParser.migratePoints(HoTTbinReaderD.points);
			// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
			// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
			// 57=LowestCellNumber, 58=Pressure, 59=Event G
			if (!isResetMinMax[2] && HoTTbinReaderD.points[38] != 0) {
				for (int i=38; i<59; ++i) {
					tmpRecordSet.get(i).setMinMax(HoTTbinReaderD.points[i], HoTTbinReaderD.points[i]);
				}
				isResetMinMax[2] = true;
			}
		}
		if (migrationJobs.contains(Sensor.GPS)) {
			HoTTbinReaderD.gpsBinParser.migratePoints(HoTTbinReaderD.points);
			// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
			// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
			if (!isResetMinMax[3] && HoTTbinReaderD.points[27] >= 3000  && HoTTbinReaderD.points[20] != 0 && HoTTbinReaderD.points[21] != 0) {
				for (int i=20; i<38; ++i) {
					tmpRecordSet.get(i).setMinMax(HoTTbinReaderD.points[i], HoTTbinReaderD.points[i]);
				}
				isResetMinMax[3] = true;
			}
		}
		if (migrationJobs.contains(Sensor.VARIO)) {
			HoTTbinReaderD.varBinParser.migratePoints(HoTTbinReaderD.points);
		}
		if (migrationJobs.contains(Sensor.ESC)) {
			HoTTbinReaderD.escBinParser.migratePoints(HoTTbinReaderD.points);
			// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
			// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
			// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
			// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
			if (!isResetMinMax[0] && HoTTbinReaderD.points[107] != 0) {
				for (int i = 123; i < 123+29; i++) {
					tmpRecordSet.get(i).setMinMax(HoTTbinReaderD.points[i], HoTTbinReaderD.points[i]);
				}
				isResetMinMax[0] = true;
			}
		}
		migrationJobs.clear();
		
		if(HoTTbinReaderD.points[219] != 0 || HoTTbinReaderD.points[222] != 0)
			System.out.println();

		HoTTbinReaderD.recordSet.addPoints(HoTTbinReaderD.points, timeStep_ms);
	}

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception
	 */
	public static synchronized void read(String filePath, PickerParameters newPickerParameters) throws Exception {
		HoTTbinReaderD.pickerParameters = newPickerParameters;
		HashMap<String, String> header = getFileInfo(new File(filePath), newPickerParameters);
		HoTTbinReaderD.detectedSensors = Sensor.getSetFromDetected(header.get(HoTTAdapter.DETECTED_SENSOR));
		
		//set picker parameter setting sensor for altitude/climb usage (0=auto, 1=VARIO, 2=GPS, 3=GAM, 4=EAM)
		HoTTbinReaderD.setAltitudeClimbPickeParameter(HoTTbinReaderD.pickerParameters, HoTTbinReaderD.detectedSensors);

		if (HoTTbinReaderD.detectedSensors.size() <= 2) {
			HoTTbinReaderD.isReceiverOnly = HoTTbinReaderD.detectedSensors.size() == 1;
			readSingle(new File(header.get(HoTTAdapter.FILE_PATH)), header);
		} else
			readMultiple(new File(header.get(HoTTAdapter.FILE_PATH)), header);
	}

	/**
	 * read log data according to version 0
	 * @param file
	 * @param data_in
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	static void readSingle(File file, HashMap<String, String> header) throws IOException, DataInconsitsentException {
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		IDevice device = HoTTbinReaderD.application.getActiveDevice();
		int recordSetNumber = HoTTbinReaderD.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		int channelNumber = HoTTbinReaderD.pickerParameters.analyzer.getActiveChannel().getNumber();
		device.getMeasurementFactor(channelNumber, 12);
		boolean isSensorData = false;
		boolean isVarioDetected = false;
		boolean isGPSdetected = false;
		boolean isESCdetected = false;
		boolean[] isResetMinMax = new boolean[] {false, false, false, false, false}; //ESC, EAM, GAM, GPS, Vario
		HoTTbinReaderD.recordSet = null;
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=Ch17 ... 118=Ch32, 119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
		//ESC1
		// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
		// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
		// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
		// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
		//ESC2
		// 152=VoltageM, 153=CurrentM, 154=CapacityM, 155=PowerM, 156=RevolutionM, 157=TemperatureM 1, 158=TemperatureM 2 159=Voltage_min, 160=Current_max,
		// 161=Revolution_max, 162=Temperature1_max, 163=Temperature2_max 164=Event M
		// 165=Speed 166=Speed_max 167=PWM 168=Throttle 169=VoltageBEC 170=VoltageBEC_min 171=CurrentBEC 172=TemperatureBEC 173=TemperatureCap 
		// 174=Timing(empty) 175=Temperature_aux 176=Gear 177=YGEGenExt 178=MotStatEscNr 179=misc ESC_15 180=VersionESC
		//ESC3
		// 181=VoltageM2, 182=CurrentM, 183=CapacityM, 184=PowerM, 185=RevolutionM, 186=TemperatureM 1, 187=TemperatureM 2 188=Voltage_min, 189=Current_max,
		// 190=Revolution_max, 191=Temperature1_max, 192=Temperature2_max 193=Event M
		// 194=Speed 195=Speed_max 196=PWM 197=Throttle 198=VoltageBEC 199=VoltageBEC_min 200=CurrentBEC 201=TemperatureBEC 202=TemperatureCap 
		// 203=Timing(empty) 204=Temperature_aux 205=Gear 206=YGEGenExt 207=MotStatEscNr 208=misc ESC_15 209=VersionESC
		//ESC4
		// 210=VoltageM3, 211=CurrentM, 212=CapacityM, 213=PowerM, 214=RevolutionM, 215=TemperatureM 1, 216=TemperatureM 2 217=Voltage_min, 218=Current_max,
		// 219=Revolution_max, 220=Temperature1_max, 221=Temperature2_max 222=Event M
		// 223=Speed 224=Speed_max 225=PWM 226=Throttle 227=VoltageBEC 228=VoltageBEC_min 229=CurrentBEC 230=TemperatureBEC 231=TemperatureCap 
		// 232=Timing(empty) 233=Temperature_aux 234=Gear 235=YGEGenExt 236=MotStatEscNr 237=misc ESC_15 238=VersionESC

		// 239=Test 00 240=Test 01.. 251=Test 12
		HoTTbinReaderD.points = new int[device.getNumberOfMeasurements(channelNumber)];
		HoTTbinReaderD.pointsGAM = HoTTbinReaderD.pointsEAM = HoTTbinReaderD.pointsESC = HoTTbinReaderD.pointsVario = HoTTbinReaderD.pointsGPS = HoTTbinReaderD.points;
		HoTTbinReaderD.dataBlockSize = 64;
		HoTTbinReaderD.buf = new byte[HoTTbinReaderD.dataBlockSize];
		HoTTbinReaderD.buf0 = new byte[30];
		HoTTbinReaderD.buf1 = new byte[30];
		HoTTbinReaderD.buf2 = new byte[30];
		HoTTbinReaderD.buf3 = new byte[30];
		HoTTbinReaderD.buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		long[] timeSteps_ms = new long[] { 0 };
		HoTTbinReaderD.rcvBinParser = Sensor.RECEIVER.createBinParserD(HoTTbinReaderD.pickerParameters, HoTTbinReaderD.points, timeSteps_ms, new byte[][] { buf });
		HoTTbinReaderD.chnBinParser = Sensor.CHANNEL.createBinParserD(HoTTbinReaderD.pickerParameters, HoTTbinReaderD.points, timeSteps_ms, new byte[][] { buf });
		HoTTbinReaderD.varBinParser = (VarBinParserD) Sensor.VARIO.createBinParserD(HoTTbinReaderD.pickerParameters, HoTTbinReaderD.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReaderD.gpsBinParser = Sensor.GPS.createBinParserD(HoTTbinReaderD.pickerParameters, HoTTbinReaderD.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReaderD.gamBinParser = Sensor.GAM.createBinParserD(HoTTbinReaderD.pickerParameters, HoTTbinReaderD.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReaderD.eamBinParser = Sensor.EAM.createBinParserD(HoTTbinReaderD.pickerParameters, HoTTbinReaderD.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReaderD.escBinParser = Sensor.ESC.createBinParserD(HoTTbinReaderD.pickerParameters, HoTTbinReaderD.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReaderD.isTextModusSignaled = false;
		boolean isSdLogFormat = Boolean.parseBoolean(header.get(HoTTAdapter.SD_FORMAT));
		long numberDatablocks = isSdLogFormat ? fileSize - HoTTbinReaderX.headerSize - HoTTbinReaderX.footerSize : fileSize / HoTTbinReaderD.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReaderD.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTbinReaderD.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);
		if (isSdLogFormat) data_in.skip(HoTTbinReaderX.headerSize);

		try {
			// check if recordSet initialized, transmitter and receiver data always present, but not in the same data rate and signals
			channel = HoTTbinReaderD.channels.get(channelNumber);
			String newFileDescription = HoTTbinReaderD.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReaderD.application.getObjectKey()	: date;
			if (channel.getFileDescription().length() <= newFileDescription.length() || (HoTTbinReaderD.application.isObjectoriented() && !channel.getFileDescription().contains(HoTTbinReaderD.application.getObjectKey())))
				channel.setFileDescription(newFileDescription);
			recordSetName = recordSetNumber + device.getRecordSetStemNameReplacement() + recordSetNameExtend;
			HoTTbinReaderD.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
			channel.put(recordSetName, HoTTbinReaderD.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			// recordSet initialized and ready to add data

			// read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReaderD.buf);
				if (HoTTbinReaderD.log.isLoggable(Level.FINE) && i % 10 == 0) {
					HoTTbinReaderD.log.log(Level.FINE, StringHelper.fourDigitsRunningNumber(HoTTbinReaderD.buf.length));
					HoTTbinReaderD.log.log(Level.FINE, StringHelper.byte2Hex4CharString(HoTTbinReaderD.buf, HoTTbinReaderD.buf.length));
				}

				if (!HoTTbinReaderD.pickerParameters.isFilterTextModus || (HoTTbinReaderD.buf[6] & 0x01) == 0) { // switch into text modus
					if (HoTTbinReaderD.buf[33] >= 0 && HoTTbinReaderD.buf[33] <= 4 && HoTTbinReaderD.buf[3] != 0 && HoTTbinReaderD.buf[4] != 0) { // buf 3, 4, tx,rx
						if (HoTTbinReaderD.log.isLoggable(Level.INFO))
							HoTTbinReaderD.log.log(Level.INFO, String.format("Sensor %x Blocknummer : %d", HoTTbinReaderD.buf[7], HoTTbinReaderD.buf[33]));

						((RcvBinParser) HoTTbinReaderD.rcvBinParser).trackPackageLoss(true);
						
						if (HoTTbinReaderD.log.isLoggable(Level.FINER)) HoTTbinReaderD.log.log(Level.FINER, StringHelper.byte2Hex2CharString(new byte[] {
								HoTTbinReaderD.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReaderD.buf[7], false));

						// fill receiver data
						if (HoTTbinReaderD.buf[33] == 0 && (HoTTbinReaderD.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReaderD.buf, 40) >= 0) {
							HoTTbinReaderD.rcvBinParser.parse();
						}
						HoTTbinReaderD.chnBinParser.parse(); // Channels

						// fill data block 0 receiver voltage an temperature
						if (buf[33] == 0) {
							bufCopier.copyToBuffer();
						}

						// create and fill sensor specific data record sets
						switch ((byte) (HoTTbinReaderD.buf[7] & 0xFF)) {
						case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
						case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
							if (detectedSensors.contains(Sensor.VARIO)) {
								bufCopier.copyToVarioBuffer();
								if (bufCopier.is4BuffersFull()) {
									HoTTbinReaderD.varBinParser.parse();

									if (!isVarioDetected) {
										HoTTAdapter2.updateVarioTypeDependent((HoTTbinReaderD.buf4[9] & 0xFF), device, HoTTbinReaderD.recordSet);
										isVarioDetected = true;								
									}
									
									bufCopier.clearBuffers();
									isSensorData = true;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_GPS_115200:
						case HoTTAdapter.SENSOR_TYPE_GPS_19200:
							if (detectedSensors.contains(Sensor.GPS)) {
								bufCopier.copyToFreeBuffer();
								if (bufCopier.is4BuffersFull()) {
									HoTTbinReaderD.gpsBinParser.parse();
									
									if (!isGPSdetected) {
										if (isReasonableData(buf4) && HoTTbinReaderD.recordSet.get(33).size() > 0 && HoTTbinReaderD.recordSet.get(33).get(HoTTbinReaderD.recordSet.get(33).size()-1) != 0) {
											HoTTAdapter2.updateGpsTypeDependent((buf4[9] & 0xFF), device, HoTTbinReaderD.recordSet, (HoTTbinReaderD.recordSet.get(33).size()-1) * 5);
											isGPSdetected = true;
										}
									}
									
									bufCopier.clearBuffers();
									isSensorData = true;
									// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
									// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
									if (!isResetMinMax[3] && HoTTbinReaderD.points[27] == 3000 && HoTTbinReaderD.points[20] != 0 && HoTTbinReaderD.points[21] != 0) {
										for (int j=20; j<38; ++j) {
											tmpRecordSet.get(j).setMinMax(HoTTbinReaderD.points[j], HoTTbinReaderD.points[j]);
										}
										isResetMinMax[3] = true;
									}
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
						case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
							if (detectedSensors.contains(Sensor.GAM)) {
								bufCopier.copyToFreeBuffer();
								if (bufCopier.is4BuffersFull()) {
									HoTTbinReaderD.gamBinParser.parse();
									bufCopier.clearBuffers();
									isSensorData = true;
									// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
									// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
									// 57=LowestCellNumber, 58=Pressure, 59=Event G
									if (!isResetMinMax[2] && HoTTbinReaderD.points[38] != 0) {
										for (int j=38; j<60; ++j) {
											tmpRecordSet.get(j).setMinMax(HoTTbinReaderD.points[j], HoTTbinReaderD.points[j]);
										}
										isResetMinMax[2] = true;
									}
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
							if (detectedSensors.contains(Sensor.EAM)) {
								bufCopier.copyToFreeBuffer();
								if (bufCopier.is4BuffersFull()) {
									HoTTbinReaderD.eamBinParser.parse();
									bufCopier.clearBuffers();
									isSensorData = true;
									// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
									// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
									if (!isResetMinMax[1] && HoTTbinReaderD.points[60] != 0) {
										for (int j=60; j<87; ++j) {
											tmpRecordSet.get(j).setMinMax(HoTTbinReaderD.points[j], HoTTbinReaderD.points[j]);
										}
										isResetMinMax[1] = true;
									}
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
							if (detectedSensors.contains(Sensor.ESC)) {
								bufCopier.copyToFreeBuffer();
								if (bufCopier.is4BuffersFull()) {
									HoTTbinReaderD.escBinParser.parse();
									
									if (!isESCdetected) {
										HoTTAdapterD.updateEscTypeDependent((HoTTbinReaderD.buf4[9] & 0xFF), device, HoTTbinReaderD.recordSet);
										isESCdetected = true;								
									}

									bufCopier.clearBuffers();
									isSensorData = true;
									// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
									// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
									// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
									// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
									if (!isResetMinMax[0] && HoTTbinReaderD.points[107] != 0) {
										for (int j = 123; j < 123+29; j++) {
											tmpRecordSet.get(j).setMinMax(HoTTbinReaderD.points[j], HoTTbinReaderD.points[j]);
										}
										isResetMinMax[0] = true;
									}
								}
							}
							break;
						}

						if (isSensorData) {
							((RcvBinParser) HoTTbinReaderD.rcvBinParser).updateLossStatistics();
						}

						tmpRecordSet.addPoints(HoTTbinReaderD.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);

						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10; // add default time step from device of 10 msec
						
						if (i % progressIndicator == 0) GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
					} else { // skip empty block, but add time step
						if (HoTTbinReaderD.log.isLoggable(Level.FINE)) HoTTbinReaderD.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

						((RcvBinParser) HoTTbinReaderD.rcvBinParser).trackPackageLoss(false);

						HoTTbinReaderD.chnBinParser.parse(); // Channels
						tmpRecordSet.addPoints(HoTTbinReaderD.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
					}
				} else if (!HoTTbinReaderD.isTextModusSignaled) {
					HoTTbinReaderD.isTextModusSignaled = true;
					HoTTbinReaderD.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			((RcvBinParser) HoTTbinReaderD.rcvBinParser).finalUpdateLossStatistics();
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0
					? String.format("%.1f", ((RcvBinParser) HoTTbinReaderD.rcvBinParser).getLostPackages().percentage)
					: "100";
			HoTTbinReaderD.detectedSensors.add(Sensor.CHANNEL);
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] {	((RcvBinParser) HoTTbinReaderD.rcvBinParser).getLossTotal(), ((RcvBinParser) HoTTbinReaderD.rcvBinParser).getLostPackages().lossTotal, packageLossPercentage, ((RcvBinParser) HoTTbinReaderD.rcvBinParser).getLostPackages().getStatistics() }) 
					+ String.format(" - Sensor: %s", HoTTlogReader.detectedSensors.toString())
					+ (HoTTAdapter2.isAltClimbSensor(HoTTbinReaderD.detectedSensors)
							? String.format(" - %s = %s", Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGT2419), Sensor.fromOrdinal(pickerParameters.altitudeClimbSensorSelection).name())
									: ""));
			HoTTbinReaderD.log.log(Level.WARNING, "skipped number receiver data due to package loss = " + ((RcvBinParser) HoTTbinReaderD.rcvBinParser).getLostPackages().lossTotal); //$NON-NLS-1$
			HoTTbinReaderD.log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (GDE.isWithUi()) {
				GDE.getUiNotification().setProgress(99);
				device.updateVisibilityStatus(tmpRecordSet, true);
				channel.applyTemplate(recordSetName, false);

				// write filename after import to record description
				tmpRecordSet.descriptionAppendFilename(file.getName());

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				GDE.getUiNotification().setProgress(100);
			}
		} finally {
			data_in.close();
			data_in = null;
		}
	}


	/**
	 * read log data according to version 0
	 * @param file
	 * @param data_in
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	static void readMultiple(File file, HashMap<String, String> header) throws IOException, DataInconsitsentException {
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		IDevice device = HoTTbinReaderD.application.getActiveDevice();
		int recordSetNumber = HoTTbinReaderD.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		int channelNumber = HoTTbinReaderD.pickerParameters.analyzer.getActiveChannel().getNumber();
		boolean isReceiverData = false;
		HoTTbinReaderD.recordSet = null;
		boolean isJustMigrated = false;
		boolean isVarioDetected = false;
		boolean isGPSdetected = false;
		boolean isESCdetected = false;
		boolean[] isResetMinMax = new boolean[] {false, false, false, false, false}; //ESC, EAM, GAM, GPS, Vario
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=Ch17 ... 118=Ch32, 119=PowerOff, 120=BatterieLow, 121=Reset, 122=reserve
		//ESC1
		// 123=VoltageM, 124=CurrentM, 125=CapacityM, 126=PowerM, 127=RevolutionM, 128=TemperatureM 1, 129=TemperatureM 2 130=Voltage_min, 131=Current_max,
		// 132=Revolution_max, 133=Temperature1_max, 134=Temperature2_max 135=Event M
		// 136=Speed 137=Speed_max 138=PWM 139=Throttle 140=VoltageBEC 141=VoltageBEC_max 142=CurrentBEC 143=TemperatureBEC 144=TemperatureCap 
		// 145=Timing(empty) 146=Temperature_aux 147=Gear 148=YGEGenExt 149=MotStatEscNr 150=misc ESC_15 151=VersionESC
		//ESC2
		// 152=VoltageM, 153=CurrentM, 154=CapacityM, 155=PowerM, 156=RevolutionM, 157=TemperatureM 1, 158=TemperatureM 2 159=Voltage_min, 160=Current_max,
		// 161=Revolution_max, 162=Temperature1_max, 163=Temperature2_max 164=Event M
		// 165=Speed 166=Speed_max 167=PWM 168=Throttle 169=VoltageBEC 170=VoltageBEC_min 171=CurrentBEC 172=TemperatureBEC 173=TemperatureCap 
		// 174=Timing(empty) 175=Temperature_aux 176=Gear 177=YGEGenExt 178=MotStatEscNr 179=misc ESC_15 180=VersionESC
		//ESC3
		// 181=VoltageM2, 182=CurrentM, 183=CapacityM, 184=PowerM, 185=RevolutionM, 186=TemperatureM 1, 187=TemperatureM 2 188=Voltage_min, 189=Current_max,
		// 190=Revolution_max, 191=Temperature1_max, 192=Temperature2_max 193=Event M
		// 194=Speed 195=Speed_max 196=PWM 197=Throttle 198=VoltageBEC 199=VoltageBEC_min 200=CurrentBEC 201=TemperatureBEC 202=TemperatureCap 
		// 203=Timing(empty) 204=Temperature_aux 205=Gear 206=YGEGenExt 207=MotStatEscNr 208=misc ESC_15 209=VersionESC
		//ESC4
		// 210=VoltageM3, 211=CurrentM, 212=CapacityM, 213=PowerM, 214=RevolutionM, 215=TemperatureM 1, 216=TemperatureM 2 217=Voltage_min, 218=Current_max,
		// 219=Revolution_max, 220=Temperature1_max, 221=Temperature2_max 222=Event M
		// 223=Speed 224=Speed_max 225=PWM 226=Throttle 227=VoltageBEC 228=VoltageBEC_min 229=CurrentBEC 230=TemperatureBEC 231=TemperatureCap 
		// 232=Timing(empty) 233=Temperature_aux 234=Gear 235=YGEGenExt 236=MotStatEscNr 237=misc ESC_15 238=VersionESC

		// 239=Test 00 240=Test 01.. 251=Test 12
		HoTTbinReaderD.points = new int[device.getNumberOfMeasurements(channelNumber)];
		HoTTbinReaderD.pointsGAM = new int[HoTTbinReaderD.points.length];
		HoTTbinReaderD.pointsEAM = new int[HoTTbinReaderD.points.length];
		HoTTbinReaderD.pointsESC = new int[HoTTbinReaderD.points.length];
		HoTTbinReaderD.pointsVario = new int[HoTTbinReaderD.points.length];
		HoTTbinReaderD.pointsVario[2] = 100000;
		HoTTbinReaderD.pointsGPS = new int[HoTTbinReaderD.points.length];
		HoTTbinReaderD.dataBlockSize = 64;
		HoTTbinReaderD.buf = new byte[HoTTbinReaderD.dataBlockSize];
		HoTTbinReaderD.buf0 = new byte[30];
		HoTTbinReaderD.buf1 = new byte[30];
		HoTTbinReaderD.buf2 = new byte[30];
		HoTTbinReaderD.buf3 = new byte[30];
		HoTTbinReaderD.buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		long[] timeSteps_ms = new long[] { 0 };
		// parse in situ for receiver and channel
		HoTTbinReaderD.rcvBinParser = Sensor.RECEIVER.createBinParserD(HoTTbinReaderD.pickerParameters, HoTTbinReaderD.points, timeSteps_ms, new byte[][] { buf });
		HoTTbinReaderD.chnBinParser = Sensor.CHANNEL.createBinParserD(HoTTbinReaderD.pickerParameters, HoTTbinReaderD.points, timeSteps_ms, new byte[][] { buf });
		// use parser points objects
		HoTTbinReaderD.varBinParser = (VarBinParserD) Sensor.VARIO.createBinParserD(HoTTbinReaderD.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReaderD.gpsBinParser = Sensor.GPS.createBinParserD(HoTTbinReaderD.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReaderD.gamBinParser = Sensor.GAM.createBinParserD(HoTTbinReaderD.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReaderD.eamBinParser = Sensor.EAM.createBinParserD(HoTTbinReaderD.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReaderD.escBinParser = Sensor.ESC.createBinParserD(HoTTbinReaderD.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGAM = 0, logCountEAM = 0, logCountESC = 0;
		EnumSet<Sensor> migrationJobs = EnumSet.noneOf(Sensor.class);
		boolean isSdLogFormat = Boolean.parseBoolean(header.get(HoTTAdapter.SD_FORMAT));
		long numberDatablocks = isSdLogFormat ? fileSize - HoTTbinReaderX.headerSize - HoTTbinReaderX.footerSize : fileSize / HoTTbinReaderD.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReaderD.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTbinReaderD.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);
		if (isSdLogFormat) data_in.skip(HoTTbinReaderX.headerSize);

		try {
			// receiver data are always contained
			channel = HoTTbinReaderD.channels.get(channelNumber);
			String newFileDescription = HoTTbinReaderD.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReaderD.application.getObjectKey()	: date;
			if (channel.getFileDescription().length() <= newFileDescription.length() || (HoTTbinReaderD.application.isObjectoriented() && !channel.getFileDescription().contains(HoTTbinReaderD.application.getObjectKey())))
				channel.setFileDescription(newFileDescription);
			recordSetName = recordSetNumber + device.getRecordSetStemNameReplacement() + recordSetNameExtend;
			HoTTbinReaderD.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
			channel.put(recordSetName, HoTTbinReaderD.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			// recordSet initialized and ready to add data

			// read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReaderD.buf);
				if (HoTTbinReaderD.log.isLoggable(Level.FINEST)) {
					HoTTbinReaderD.log.log(Level.FINEST, StringHelper.byte2Hex4CharString(HoTTbinReaderD.buf, HoTTbinReaderD.buf.length));
				}

				if (!HoTTbinReaderD.pickerParameters.isFilterTextModus || (HoTTbinReaderD.buf[6] & 0x01) == 0) { // switch into text modus
					if (HoTTbinReaderD.buf[33] >= 0 && HoTTbinReaderD.buf[33] <= 4 && HoTTbinReaderD.buf[3] != 0 && HoTTbinReaderD.buf[4] != 0) { // buf 3, 4, tx,rx
						if (HoTTbinReaderD.log.isLoggable(Level.INFO))
							HoTTbinReaderD.log.log(Level.INFO, String.format("Sensor %x Blocknummer : %d", HoTTbinReaderD.buf[7], HoTTbinReaderD.buf[33]));

						((RcvBinParser) HoTTbinReaderD.rcvBinParser).trackPackageLoss(true);
						
						if (HoTTbinReaderD.log.isLoggable(Level.FINEST)) HoTTbinReaderD.log.log(Level.FINEST, StringHelper.byte2Hex2CharString(new byte[] {
								HoTTbinReaderD.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReaderD.buf[7], false));

						// fill receiver data
						if (HoTTbinReaderD.buf[33] == 0 && (HoTTbinReaderD.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReaderD.buf, 40) >= 0) {
							HoTTbinReaderD.rcvBinParser.parse();
							isReceiverData = true;
						}
						HoTTbinReaderD.chnBinParser.parse();

						if (actualSensor == -1)
							lastSensor = actualSensor = (byte) (HoTTbinReaderD.buf[7] & 0xFF);
						else
							actualSensor = (byte) (HoTTbinReaderD.buf[7] & 0xFF);

						if (actualSensor != lastSensor) {
							if (logCountVario >= 5 || logCountGPS >= 5 || logCountGAM >= 5 || logCountEAM >= 5 || logCountESC >= 5) {
								switch (lastSensor) {
								case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
								case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
									if (detectedSensors.contains(Sensor.VARIO)) {
										if (migrationJobs.contains(Sensor.VARIO) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReaderD.varBinParser.parse();
										migrationJobs.add(Sensor.VARIO);
										
										if (!isVarioDetected) {
											HoTTAdapter2.updateVarioTypeDependent((HoTTbinReaderD.buf4[9] & 0xFF), device, HoTTbinReaderD.recordSet);
											isVarioDetected = true;								
										}
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GPS_115200:
								case HoTTAdapter.SENSOR_TYPE_GPS_19200:
									if (detectedSensors.contains(Sensor.GPS)) {
										if (migrationJobs.contains(Sensor.GPS) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReaderD.gpsBinParser.parse();
										migrationJobs.add(Sensor.GPS);
										
										if (!isGPSdetected) {
											if (isReasonableData(buf4) && HoTTbinReaderD.recordSet.get(33).size() > 0 && HoTTbinReaderD.recordSet.get(33).get(HoTTbinReaderD.recordSet.get(33).size()-1) != 0) {
												HoTTAdapter2.updateGpsTypeDependent((buf4[9] & 0xFF), device, HoTTbinReaderD.recordSet, (HoTTbinReaderD.recordSet.get(33).size()-1) * 5);
												isGPSdetected = true;
											}
										}
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
								case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
									if (detectedSensors.contains(Sensor.GAM)) {
										if (migrationJobs.contains(Sensor.GAM) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReaderD.gamBinParser.parse();
										migrationJobs.add(Sensor.GAM);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
									if (detectedSensors.contains(Sensor.EAM)) {
										if (migrationJobs.contains(Sensor.EAM) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReaderD.eamBinParser.parse();
										migrationJobs.add(Sensor.EAM);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
									if (detectedSensors.contains(Sensor.ESC)) {
										if (migrationJobs.contains(Sensor.ESC) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReaderD.escBinParser.parse();
										migrationJobs.add(Sensor.ESC);
										
										if (!isESCdetected) {
											HoTTAdapterD.updateEscTypeDependent((HoTTbinReaderD.buf4[9] & 0xFF), device, HoTTbinReaderD.recordSet, 1);
											isESCdetected = true;								
										}
									}
									break;
								}

								if (HoTTbinReaderD.log.isLoggable(Level.FINE)) HoTTbinReaderD.log.log(Level.FINE, "isReceiverData " + isReceiverData + " migrationJobs " + migrationJobs);
							}

							if (HoTTbinReaderD.log.isLoggable(Level.FINE))
								HoTTbinReaderD.log.log(Level.FINE, "logCountVario = " + logCountVario + " logCountGPS = " + logCountGPS + " logCountGeneral = " + logCountGAM + " logCountElectric = " + logCountEAM);
							lastSensor = actualSensor;
							logCountVario = logCountGPS = logCountGAM = logCountEAM = logCountESC = 0;
						}

						switch (lastSensor) {
						case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
						case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
							++logCountVario;
							break;
						case HoTTAdapter.SENSOR_TYPE_GPS_115200:
						case HoTTAdapter.SENSOR_TYPE_GPS_19200:
							++logCountGPS;
							break;
						case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
						case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
							++logCountGAM;
							break;
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
							++logCountEAM;
							break;
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
							++logCountESC;
							break;
						}

						if (isJustMigrated) {
							((RcvBinParser) HoTTbinReaderD.rcvBinParser).updateLossStatistics();
						}

						tmpRecordSet.addPoints(HoTTbinReaderD.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						isReceiverData = false;
						isJustMigrated = false;

						bufCopier.copyToBuffer();
						
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;// add default time step from log record of 10 msec
						
						if (i % progressIndicator == 0) GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
					} else { // skip empty block, but add time step
						if (HoTTbinReaderD.log.isLoggable(Level.FINE)) HoTTbinReaderD.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");
						
						((RcvBinParser) HoTTbinReaderD.rcvBinParser).trackPackageLoss(false);
						
						HoTTbinReaderD.chnBinParser.parse();
						tmpRecordSet.addPoints(HoTTbinReaderD.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
					}
				} else if (!HoTTbinReaderD.isTextModusSignaled) {
					HoTTbinReaderD.isTextModusSignaled = true;
					HoTTbinReaderD.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			// if (HoTTbinReaderD.oldProtocolCount > 2) {
			// application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2405, new Object[] {
			// HoTTbinReaderD.oldProtocolCount }));
			// }
			((RcvBinParser) HoTTbinReaderD.rcvBinParser).finalUpdateLossStatistics();
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0
					? String.format("%.1f", ((RcvBinParser) HoTTbinReaderD.rcvBinParser).getLostPackages().percentage)
					: "100";
			HoTTbinReaderD.detectedSensors.add(Sensor.CHANNEL);
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] {	((RcvBinParser) HoTTbinReaderD.rcvBinParser).getLossTotal(), ((RcvBinParser) HoTTbinReaderD.rcvBinParser).getLostPackages().lossTotal, packageLossPercentage, ((RcvBinParser) HoTTbinReaderD.rcvBinParser).getLostPackages().getStatistics() }) 
					+ String.format(" - Sensor: %s", HoTTlogReader.detectedSensors.toString())
					+ (HoTTAdapter2.isAltClimbSensor(HoTTbinReaderD.detectedSensors)
							? String.format(" - %s = %s", Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGT2419), Sensor.fromOrdinal(pickerParameters.altitudeClimbSensorSelection).name())
									: ""));
			HoTTbinReaderD.log.log(Level.WARNING, "skipped number receiver data due to package loss = " + ((RcvBinParser) HoTTbinReaderD.rcvBinParser).getLostPackages().lossTotal); //$NON-NLS-1$
			HoTTbinReaderD.log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (menuToolBar != null) {
				GDE.getUiNotification().setProgress(99);
				device.makeInActiveDisplayable(tmpRecordSet);
				device.updateVisibilityStatus(tmpRecordSet, true);
				channel.applyTemplate(recordSetName, false);

				// write filename after import to record description
				tmpRecordSet.descriptionAppendFilename(file.getName());

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				GDE.getUiNotification().setProgress(100);
			}
		} finally {
			data_in.close();
			data_in = null;
		}
	}

}
