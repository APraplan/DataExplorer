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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import java.util.zip.Adler32;
import java.util.zip.CRC32;

/**
 * checksum calculation utility class
 * @author Winfried Brügmann
 */
public class Checksum {

	/**
	 * main method to test this class
	 * @param args
	 */
	public static void main(String[] args) {
		byte[] b = new byte[] { (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B,
				(byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C,
				(byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x00 };
		byte[] b1 = new byte[] { (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C,
				(byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C,
				(byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x07 };

		byte[] HOTT_19200_N_MASTER = new byte[] { (byte) 0x80, (byte) 0x8D, (byte) 0x8D, 0x7C, 0x00, 0x00, 0x3E, 0x34, 0x37, 0x3E, 0x64, 0x33, (byte) 0xD0, 0x07, 0x00, 0x41, (byte) 0xC0, 0x00,
				(byte) 0x80, (byte) 0xC3, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2B, 0x2B, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xF4, 0x01, 0x30, 0x75, 0x78, 0x00, 0x00,
				(byte) 0x9C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7D, (byte) 0xDF
		};
		byte[] HOTT_19200_N_MASTER_1 = new byte[] { (byte) 0x80, (byte) 0x8D, 0x7C, 0x00, 0x00, 0x3F, 0x34, 0x37, 0x3F, 0x64, 0x33, (byte) 0xD0, 0x07, 0x00, 0x3E, (byte) 0xC0, 0x00, (byte) 0x80,
				(byte) 0xC3, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2B, 0x2B, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xF4, 0x01, 0x3A, 0x75, 0x78, 0x00, 0x00, (byte) 0x9C,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7D, (byte) 0xE8 };

		byte[] HOTT_19200_N_MASTER_2 = new byte[] { (byte)0x80, (byte)0x89, 0x7C, 0x00, 0x00, 0x3A, 0x35, 0x36, 0x2F, 0x64, 0x35, 0x0A, 0x00, 0x00, 0x2F, (byte)0xC0, 0x00, (byte)0x80, (byte)0xC3, (byte)0xC2, (byte)0xC3, (byte)0xC4, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x31, 0x2B, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xF5, 0x01, 0x30, 0x75, 0x78, 0x00, 0x00, (byte)0x9C, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7D, (byte)0xF8}; 
		byte[] HOTT_19200_N_SLAVE = new byte[] { (byte) 0x80, (byte) 0x8D, 0x7C, 0x00, 0x00, 0x53, 0x34, 0x37, 0x53, 0x64, 0x33, (byte) 0xD0, 0x07, 0x00, 0x55, (byte) 0xC0, 0x00, (byte) 0x80,
				(byte) 0xC3, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2B, 0x2B, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xF4, 0x01, 0x12, 0x75, 0x78, 0x00, 0x00, (byte) 0x9C,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7D, (byte) 0xFF };
		System.out.println("CRC16    	= " + Integer.toHexString(Checksum.CRC16(b, 0))); //$NON-NLS-1$
		System.out.println("CRC16CCITT= " + Integer.toHexString(Checksum.CRC16CCITT(b, 0))); //$NON-NLS-1$
		System.out.println("CRC32 		= " + Integer.toHexString(Checksum.CRC32(b))); //$NON-NLS-1$
		System.out.println("Adler32  	= " + Long.toHexString(Checksum.adler32(b))); //$NON-NLS-1$
		System.out.println("AND      	= " + Integer.toHexString(Checksum.AND(b))); //$NON-NLS-1$
		System.out.println("XOR     	= " + Integer.toHexString(Checksum.XOR(b))); //$NON-NLS-1$
		System.out.println("XOR      	= " + Integer.toHexString(Checksum.XOR(b1))); //$NON-NLS-1$
		
		System.out.println("ADD      	= " + Integer.toHexString(0xFF & Checksum.ADD(HOTT_19200_N_MASTER, 2, HOTT_19200_N_MASTER.length-2)) + " - " + Integer.toHexString(0xFF & HOTT_19200_N_MASTER[HOTT_19200_N_MASTER.length-1])); //$NON-NLS-1$
		System.out.println("ADD      	= " + Integer.toHexString(0xFF & Checksum.ADD(HOTT_19200_N_MASTER_1, 2, HOTT_19200_N_MASTER_1.length-2)) + " - " + Integer.toHexString(0xFF & HOTT_19200_N_MASTER_1[HOTT_19200_N_MASTER_1.length-1])); //$NON-NLS-1$
		System.out.println("ADD      	= " + Integer.toHexString(0xFF & Checksum.ADD(HOTT_19200_N_MASTER_2, 2, HOTT_19200_N_MASTER_2.length-2)) + " - " + Integer.toHexString(0xFF & HOTT_19200_N_MASTER_2[HOTT_19200_N_MASTER_2.length-1])); //$NON-NLS-1$
		System.out.println("ADD      	= " + Integer.toHexString(0xFF & Checksum.ADD(HOTT_19200_N_SLAVE, 2, HOTT_19200_N_SLAVE.length-2)) + " - " + Integer.toHexString(0xFF & HOTT_19200_N_SLAVE[HOTT_19200_N_SLAVE.length-1])); //$NON-NLS-1$
	}
	
	/**
	 * calculate CRC16
	 * @param bytes byte array to compute CRC16
	 * @return 16-bit CRC, unsigned
	 */
	public static short CRC16(byte[] bytes, int initValue) {
		int crc = initValue; // initial value
		int polynomial = 0x1021; // 0001 0000 0010 0001  (0, 5, 12) 
		for (byte b : bytes) {
			for (int i = 0; i < 8; i++) {
				boolean bit = ((b >> (7 - i) & 1) == 1);
				boolean c15 = ((crc >> 15 & 1) == 1);
				crc <<= 1;
				if (c15 ^ bit) crc ^= polynomial;
			}
		}
		return (short) (crc &= 0xffff);
	}


	/**
	 * generator polynomial
	 */
	private static final int	poly			= 0x1021;			/* x16 + x12 + x5 + 1 generator polynomial */
	/* 0x8408 used in European X.25 */
	private static int[] crcTable	= new int[256];
	static {
		// initialize lookup table
		for (int i = 0; i < 256; i++) {
			int fcs = 0;
			int d = i << 8;
			for (int k = 0; k < 8; k++) {
				if (((fcs ^ d) & 0x8000) != 0) {
					fcs = (fcs << 1) ^ poly;
				}
				else {
					fcs = (fcs << 1);
				}
				d <<= 1;
				fcs &= 0xffff;
			}
			crcTable[i] = fcs;
		}
	}

	/**
	 * calculate CRC16 with CCITT method.
	 * @param b byte array to compute CRC on
	 * @return 16-bit CRC, unsigned
	 */
	public static short CRC16CCITT(byte[] b, int initValue) {
		int crc = initValue;
		for (int i = 0; i < b.length; i++) {
			crc = (crcTable[(b[i] ^ (crc >>> 8)) & 0xff] ^ (crc << 8)) & 0xffff;
		}
		return (short) crc;
	}

	/**
	 * calculate CRC16 with CCITT method.
	 * @param c char array to compute CRC on
	 * @return 16-bit CRC, unsigned
	 */
	public static short CRC16CCITT(char[] c) {
		int crc = 0;
		for (int i = 0; i < c.length; i++) {
			crc = (crcTable[(c[i] ^ (crc >>> 8)) & 0xff] ^ (crc << 8)) & 0xffff;
		}
		return (short) crc;
	}

	/**
	 * calculate CRC16 with CCITT method.
	 * @param b byte array to compute CRC on
	 * @return 16-bit CRC, unsigned
	 */
	public static short CRC16CCITT(byte[] b, int startIndex, int length) {
		int crc = 0;
		for (int i = startIndex; i < length+startIndex; i++) {
			crc = (crcTable[(b[i] ^ (crc >>> 8)) & 0xff] ^ (crc << 8)) & 0xffff;
		}
		return (short) crc;
	}
	
	/**
	 * calculate CRC-32 with Java method
	 * @param b byte array to compute CRC on
	 * @return 32-bit CRC, signed
	 */
	public static int CRC32(byte[] b) {
		// create a new CRC-calculating object
		CRC32 crc = new CRC32();
		// loop, calculating CRC for each byte of the string
		for (int i = 0; i < b.length; i++) {
			crc.update(b[i]);
		}
		return (int) crc.getValue();
	}

	/**
	 * calculate Adler32 checksum
	 * @param b byte array to compute
	 * @return 64-bit result
	 */
	public static long adler32(byte[] b) {
		Adler32 digester = new Adler32();
		digester.update(b);
		return digester.getValue();
	}

	/**
	 * calculate OR bit operation
	 * @param b byte array to compute
	 * @return 8-bit result
	 */
	public static byte OR(byte[] b) {
		int value = b[0];
		for (int i = 1; i < b.length; i++) {
			value = value | b[i];
		}
		return (byte) value;
	}

	/**
	 * calculate OR bit operation
	 * @param a byte array to compute
	 * @return 8-bit result
	 */
	public static int OR(int[] a, int offset, int length) {
		int value = a[offset];
		for (int i = 1; i < length; i++) {
			value = value | a[i];
		}
		return value;
	}

	/**
	 * calculate XOR bit operation
	 * @param c character array to compute
	 * @return 8-bit result
	 */
	public static int XOR(char[] c) {
		int value = 0;
		for (int i = 0; i < c.length; i++) {
			value ^= c[i];
		}
		return value;
	}

	/**
	 * calculate XOR bit operation
	 * @param b byte array to compute
	 * @return 8-bit result
	 */
	public static int XOR(byte[] b) {
		int value = 0;
		for (int i = 0; i < b.length; i++) {
			value ^= b[i];
		}
		return value;
	}

	/**
	 * calculate XOR bit operation
	 * @param b byte array to compute
	 * @return 8-bit result
	 */
	public static int XOR(byte[] b, int length) {
		int value = 0;
		for (int i = 0; i < length; i++) {
			value ^= b[i];
		}
		return value;
	}

	/**
	 * calculate XOR bit operation
	 * @param a integer array to compute
	 * @return integer result
	 */
	public static int XOR(int[] a, int offset, int length) {
		int value = a[offset];
		for (int i = offset+1; i < length; i++) {
			value ^= a[i];
		}
		return value;
	}

	/**
	 * calculate AND bit operation
	 * @param b byte array to compute
	 * @return 8-bit result
	 */
	public static byte AND(byte[] b) {
		int value = b[0];
		for (int i = 1; i < b.length; i++) {
			value = value & b[i];
		}
		return (byte) value;
	}

	/**
	 * calculate AND bit operation
	 * @param b byte array to compute
	 * @return integer result
	 */
	public static int AND(byte[] b, int offset, int length) {
		int value = b[offset] &0xFF;
		for (int i = offset+1; i < length; i++) {
			value = value & b[i];
		}
		return value;
	}

	/**
	 * calculate AND bit operation
	 * @param a integer array to compute
	 * @return integer result
	 */
	public static int AND(int[] a, int offset, int length) {
		int value = a[offset];
		for (int i = offset+1; i < length; i++) {
			value = value & a[i];
		}
		return value;
	}
	
	/**
	 * calculate ADD operation
	 * @param b byte array to compute
	 * @param len length of checksum if part of the byte array
	 * @return 16-bit result
	 */
	public static int ADD(byte[] b) {
		int value = b[0] & 0xFF;
		for (int i = 1; i < b.length; i++) {
			value = value  + (b[i] & 0xFF);
		}
		return value;
	}
	
	/**
	 * calculate ADD operation
	 * @param b byte array to compute
	 * @param len length of checksum if part of the byte array
	 * @return 16-bit result
	 */
	public static int ADD(byte[] b, int len) {
		int value = b[0] & 0xFF;
		for (int i = 1; i < b.length - len; i++) {
			value = value  + (b[i] & 0xFF);
		}
		return value;
	}

	/**
	 * calculate ADD operation
	 * @param b byte array to compute
	 * @param len used for calculation, attention checksum if part of the byte array
	 * @return 16-bit result
	 */
	public static int ADD(byte[] b, int start, int len) {
		int value = b[start] & 0xFF;
		for (int i = start+1; i <= len; i++) {
			value = value  + (b[i] & 0xFF);
		}
		return value;
	}

	/**
	 * calculate ADD operation
	 * @param a integer array to compute
	 * @param length length of checksum if part of the byte array
	 * @return integer result
	 */
	public static int ADD(int[] a, int offset, int length) {
		int value = a[offset];
		for (int i = offset+1; i < length; i++) {
			value = value  + a[i];
		}
		return value;
	}

	/**
	 * calculate the CRC16CCITT of the given area and write to end address
	 * @param bytes
	 * @param start address
	 * @param end address
	 */
	public static void calculateAndWrite(byte[] bytes, int start, int end) {
		int checkSum = Checksum.CRC16CCITT(bytes, start, end - start);
		bytes[end] = (byte) (checkSum & 0xFF);
		bytes[end+1] = (byte) ((checkSum & 0xFF00) >> 8);
	}
}
