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

import java.util.logging.Logger;

import gde.device.IDevice;
import gde.device.junsi.iChargerUsbPort;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * @author Winfried Brügmann
 */
public class ChargerUsbPort extends iChargerUsbPort {
	final static String					$CLASS_NAME														= ChargerUsbPort.class.getName();
	final static Logger					log																		= Logger.getLogger($CLASS_NAME);

	protected final static byte	MB_HID_PROTOCOL_ID										= 0x30;

	//ModBus HID ADU pack
	//Len(1byte)+Type(1byte)+ModBus PDU
	protected final static int	HID_PACK_MAX													= 64;
	protected final static byte	HID_PACK_CH														= 0;
	protected final static byte	HID_PACK_LEN													= 1;
	protected final static byte	HID_PACK_TYPE													= 2;
	protected final static byte	HID_PACK_MODBUS												= 3;
	protected final static byte	REPORT_ID															= 0;

	protected final static byte	MB_FUNC_NONE													= 0;
	protected final static byte	MB_FUNC_READ_COILS										= 1;
	protected final static byte	MB_FUNC_READ_DISCRETE_INPUTS					= 2;
	protected final static byte	MB_FUNC_WRITE_SINGLE_COIL							= 5;
	protected final static byte	MB_FUNC_WRITE_MULTIPLE_COILS					= 15;
	protected final static byte	MB_FUNC_READ_HOLDING_REGISTER					= 3;
	protected final static byte	MB_FUNC_READ_INPUT_REGISTER						= 4;
	protected final static byte	MB_FUNC_WRITE_REGISTER								= 6;
	protected final static byte	MB_FUNC_WRITE_MULTIPLE_REGISTERS			= 16;
	protected final static byte	MB_FUNC_READWRITE_MULTIPLE_REGISTERS	= 23;
	protected final static byte	MB_FUNC_DIAG_READ_EXCEPTION						= 7;
	protected final static byte	MB_FUNC_DIAG_DIAGNOSTIC								= 8;
	protected final static byte	MB_FUNC_DIAG_GET_COM_EVENT_CNT				= 11;
	protected final static byte	MB_FUNC_DIAG_GET_COM_EVENT_LOG				= 12;
	protected final static byte	MB_FUNC_OTHER_REPORT_SLAVEID					= 17;
	protected final static byte	MB_FUNC_ERROR													= (byte) 128;

	protected final static byte	READ_REG_COUNT_MAX										= ((HID_PACK_MAX - 4) / 2);							//30
	protected final static byte	WRITE_REG_COUNT_MAX										= ((HID_PACK_MAX - 8) / 2);							//28

	enum ModBusErrorCode {
		MB_EOK(0x00), /*!< no error. */
		MB_EX_ILLEGAL_FUNCTION(0x01), 
		MB_EX_ILLEGAL_DATA_ADDRESS(0x02), 
		MB_EX_ILLEGAL_DATA_VALUE(0x03), 
		MB_EX_SLAVE_DEVICE_FAILURE(0x04), 
		MB_EX_ACKNOWLEDGE(0x05), 
		MB_EX_SLAVE_BUSY(0x06), 
		MB_EX_MEMORY_PARITY_ERROR(0x08), 
		MB_EX_GATEWAY_PATH_FAILED(0x0A), 
		MB_EX_GATEWAY_TGT_FAILED(0x0B), 
		MB_ENOREG(0x80), /*!< illegal register address. */
		MB_EILLFUNCTION(0xF0), /*!< illegal function code. */
		MB_EIO(0xF1), /*!< I/O error. */
		MB_ERETURN(0xF2), /*!< protocol stack in illegal state. */
		MB_ELEN(0xF3), /*!< pack length to large error. */
		MB_ETIMEDOUT(0xF4), /*!< timeout error occurred. */
		MB_INVALID(0xF5); /* invalid or unknown error code */

		final int														value;
		public static final ModBusErrorCode	VALUES[]	= values();	// use this to avoid cloning if calling values()

		ModBusErrorCode(int value) {
			this.value = value;
		}

		public static ModBusErrorCode fromErrorByte(byte errorCodeValue) {
			for (ModBusErrorCode errorCode : ModBusErrorCode.VALUES) {
				if (errorCode.value == errorCodeValue) return errorCode;
			}
			return MB_INVALID;
		}

	};
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public ChargerUsbPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
	}

	ModBusErrorCode masterRead(byte readType, short regStart, short regCount, byte[] pOut) throws IllegalStateException, TimeOutException {
		ModBusErrorCode ret = ModBusErrorCode.MB_EOK;
		short i;
		byte[] inBuf = new byte[16];
		byte funCode;
		int indexOut = 0;

		if (readType == 0)
			funCode = MB_FUNC_READ_HOLDING_REGISTER;
		else
			funCode = MB_FUNC_READ_INPUT_REGISTER;

		for (i = 0; i < regCount / READ_REG_COUNT_MAX; i++) {
			inBuf[0] = (byte) (regStart >> 8);
			inBuf[1] = (byte) (regStart & 0xff);
			inBuf[2] = 0;
			inBuf[3] = READ_REG_COUNT_MAX;
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("transfer data length = %d indexOut = %d", inBuf[3], indexOut));
			ret = masterModBus(funCode, inBuf, pOut, indexOut, TIMEOUT_MS);
			if (ret != ModBusErrorCode.MB_EOK) 
				return ret;
			regStart += READ_REG_COUNT_MAX;
			indexOut += (2 * READ_REG_COUNT_MAX);
		}

		if (regCount % READ_REG_COUNT_MAX != 0) {
			inBuf[0] = (byte) (regStart >> 8);
			inBuf[1] = (byte) (regStart & 0xff);
			inBuf[2] = 0;
			inBuf[3] = (byte) (regCount % READ_REG_COUNT_MAX);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("transfer data length = %d indexOut = %d", inBuf[3], indexOut));
			ret = masterModBus(funCode, inBuf, pOut, indexOut, TIMEOUT_MS);
			if (ret != ModBusErrorCode.MB_EOK) return ret;
		}
		return ret;
	}

	ModBusErrorCode masterWrite(short regStart, short regCount, byte[] pIn) throws IllegalStateException, TimeOutException {
		ModBusErrorCode ret = ModBusErrorCode.MB_EOK;
		short i, j;
		byte[] inBuf = new byte[80];
		int indexIn = 0;

		for (i = 0; i < regCount / WRITE_REG_COUNT_MAX; i++) {
			inBuf[0] = (byte) (regStart >> 8);
			inBuf[1] = (byte) (regStart & 0xff);
			inBuf[2] = 0;
			inBuf[3] = WRITE_REG_COUNT_MAX;
			inBuf[4] = 2 * WRITE_REG_COUNT_MAX;
			for (j = 0; j < inBuf[4]; j += 2) {
				inBuf[5 + j] = pIn[indexIn + j + 1];
				inBuf[5 + j + 1] = pIn[indexIn + j];
			}
			ret = masterModBus(MB_FUNC_WRITE_MULTIPLE_REGISTERS, inBuf, null, 0, TIMEOUT_MS);
			if (ret != ModBusErrorCode.MB_EOK) return ret;
			regStart += WRITE_REG_COUNT_MAX;
			indexIn += (2 * WRITE_REG_COUNT_MAX);
		}

		if (regCount % WRITE_REG_COUNT_MAX != 0) {
			inBuf[0] = (byte) (regStart >> 8);
			inBuf[1] = (byte) (regStart & 0xff);
			inBuf[2] = 0;
			inBuf[3] = (byte) (regCount % WRITE_REG_COUNT_MAX);
			inBuf[4] = (byte) (2 * inBuf[3]);
			for (j = 0; j < inBuf[4]; j += 2) {
				inBuf[5 + j] = pIn[indexIn + j + 1];
				inBuf[5 + j + 1] = pIn[indexIn + j];
			}
			ret = masterModBus(MB_FUNC_WRITE_MULTIPLE_REGISTERS, inBuf, null, 0, TIMEOUT_MS);
			if (ret != ModBusErrorCode.MB_EOK) return ret;
		}
		return ret;
	}

	ModBusErrorCode masterModBus(byte funCode, byte[] pIn, byte[] pOut, int outIndex, long timeOut_ms) throws IllegalStateException, TimeOutException {
		int i;
		byte[] hidBuf = new byte[HID_PACK_MAX];
		//hidBuf[HID_PACK_CH] = REPORT_ID;
		hidBuf[HID_PACK_TYPE-1] = MB_HID_PROTOCOL_ID;
		hidBuf[HID_PACK_MODBUS-1] = funCode;
		switch (funCode) {
		case MB_FUNC_READ_INPUT_REGISTER: //Modbus function 0x04 Read Input Registers
			//			if(0) //·¶Î§¼ì²é
			//				return ModBusErrorCode.MB_ENOREG;
			hidBuf[HID_PACK_LEN-1] = 7;
			break;
		case MB_FUNC_READ_HOLDING_REGISTER: //Modbus function 0x03 Read Holding Registers
			//			if(0) //·¶Î§¼ì²é
			//				return ModBusErrorCode.MB_ENOREG;
			hidBuf[HID_PACK_LEN-1] = 7;
			break;
		case MB_FUNC_WRITE_MULTIPLE_REGISTERS: //Modbus function 0x10 Write Multiple Registers
			//			if(0) //·¶Î§¼ì²é
			//				return ModBusErrorCode.MB_ENOREG;
			hidBuf[HID_PACK_LEN-1] = (byte) (7 + (pIn[4] + 1));
			if (hidBuf[HID_PACK_LEN-1] > HID_PACK_MAX) 
				return ModBusErrorCode.MB_ELEN;
			break;
		default:
			return ModBusErrorCode.MB_EILLFUNCTION;
		}
		//copy from pIn
		for (i = 0; i < hidBuf[HID_PACK_LEN-1] - 3; i++)
			hidBuf[HID_PACK_MODBUS + i] = pIn[i];

		//read system junk 1: [64] 07 30 03 84 00 00 1E 45 DC 4E 77 24 F5 19 00 00 00 00 00 C8 EF 42 00 60 C7 68 00 0C F5 19 00 00 00 00 00 00 00 00 00 04 03 00 00 0E 3B 40 00 00 00 00 00 01 00 00 00 01 00 00 00 BB 80 42 00 14
		//read system junk 2: [64] 07 30 03 84 1E 00 1D 20 01 F4 00 64 00 02 00 10 00 10 00 0A 00 00 00 00 00 00 00 01 00 01 00 01 00 01 00 05 00 05 00 05 00 05 00 00 00 00 03 E8 00 00 00 5A 01 C2 04 4C 00 00 00 91 00 64 04 4C
		//read memory junk 1: [64] 07 30 03 8C 00 00 1E
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "write " + StringHelper.byte2Hex2CharString(hidBuf, hidBuf.length));
		this.write(libUsbHandle, this.endpointIn, hidBuf, timeOut_ms);
		//if(JsHID.Write(hidBuf,HID_PACK_MAX+1)==FALSE)return ModBusErrorCode.MB_EIO;
		//rece
		//hidBuf[HID_PACK_CH] = REPORT_ID;
		this.read(libUsbHandle, this.endpointOut, hidBuf, timeOut_ms);
		//read system junk 1: [64] 40 30 03 3C 00 00 03 20 01 F4 00 64 00 02 00 10 00 10 00 0A 00 00 00 00 00 00 00 01 00 01 00 01 00 01 00 05 00 05 00 05 00 05 00 00 00 00 03 E8 00 00 00 5A 01 C2 04 4C 00 00 00 91 00 64 04 4C
		//read system junk 2: [64] 3E 30 03 3A 00 00 00 00 00 5A 01 C2 04 4C 00 00 00 91 00 64 04 4C 00 00 00 00 00 5A 01 C2 04 4C 00 00 00 91 00 64 04 4C 00 00 00 00 00 5A 01 C2 04 4C 00 00 00 91 00 64 04 4C 00 00 00 00 04 4C
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "read  " + StringHelper.byte2Hex2CharString(hidBuf, hidBuf.length));
		//if(JsHID.Read(hidBuf,HID_PACK_MAX+1,ms)==FALSE)return ModBusErrorCode.MB_ETIMEDOUT;
		if (hidBuf[HID_PACK_LEN-1] > HID_PACK_MAX) 
			return ModBusErrorCode.MB_ELEN;

		if (hidBuf[HID_PACK_MODBUS-1] == funCode) {
			switch (funCode) {
			case MB_FUNC_READ_INPUT_REGISTER: //Modbus function 0x04 Read Input Registers
			case MB_FUNC_READ_HOLDING_REGISTER: //Modbus function 0x03 Read Holding Registers
				if ((hidBuf[HID_PACK_LEN-1] != hidBuf[HID_PACK_MODBUS] + 4) || (hidBuf[HID_PACK_LEN-1] & 0x01) != 0) 
					return ModBusErrorCode.MB_ELEN;
				//copy to pOut
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("copy to pOut data length = %d", hidBuf[HID_PACK_MODBUS]));
				for (i = 0; i < hidBuf[HID_PACK_MODBUS] && i + outIndex < pOut.length; i += 2) {
					pOut[i + outIndex] = hidBuf[HID_PACK_MODBUS + 1 + i + 1];
					pOut[i + 1 + outIndex] = hidBuf[HID_PACK_MODBUS + 1 + i];
				}
				break;
			case MB_FUNC_WRITE_MULTIPLE_REGISTERS: //Modbus function 0x10 Write Multiple Registers
				hidBuf[HID_PACK_LEN-1] = (byte) (0 + 5 + (hidBuf[HID_PACK_MODBUS + 4] * 2 + 1));
				break;
			}
		}
		else if (hidBuf[HID_PACK_MODBUS-1] == (funCode | 0x80))
			return ModBusErrorCode.fromErrorByte(hidBuf[HID_PACK_MODBUS]);
		else
			return ModBusErrorCode.MB_ERETURN;

//		if (funCode != MB_FUNC_WRITE_MULTIPLE_REGISTERS)
//			log.log(Level.INFO, StringHelper.byte2Hex2CharString(pOut, pOut.length));
		
		return ModBusErrorCode.MB_EOK;
	}
	
}