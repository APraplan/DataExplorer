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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.device.DataTypes;
import gde.io.DataParser;
import gde.log.Level;

/**
 * @author Winfried Brügmann
 * class with collection of string helper finctions
 */
public class StringHelper {
	final static Logger log = Logger.getLogger(StringHelper.class.getName());

	/**
	 * split the given string at delimiter to hash map where the keys are used for hashes
	 * @param line
	 * @param delimiter
	 * @param hashKeys
	 */
	public static HashMap<String, String> splitString(String line, String delimiter, String[] hashKeys) {
		HashMap<String, String> resultMap = new HashMap<String, String>();
		if (line != null && line.length() > 5) {
			String tmpStr = line;
			int endindex = 0;
			while ((endindex = tmpStr.indexOf(delimiter)) != -1) {
				for (String key : hashKeys) {
					if (tmpStr.startsWith(key)) {
						String value = tmpStr.substring(key.length(), endindex).trim();
						if (value.startsWith(GDE.STRING_EQUAL)) value = value.substring(1).trim();
						resultMap.put(key, value);
						break;
					}
				}
				tmpStr = tmpStr.substring(endindex + delimiter.length(), tmpStr.length());
			}
			for (String key : hashKeys) {
				if (tmpStr.startsWith(key)) {
					String value = tmpStr.substring(key.length()).trim();
					if (value.startsWith(GDE.STRING_EQUAL)) value = value.substring(1).trim();
					resultMap.put(key, value);
					break;
				}
			}
			if (log.isLoggable(Level.FINER)) {
				for (String key : hashKeys) {
					log.log(Level.FINER, key + " = " + resultMap.get(key)); //$NON-NLS-1$
				}
			}
		}
		return resultMap;
	}

	/**
	 * split a string at delimiter into string array, if the array element contains stripString at begin/end, it will be removed
	 * @param line
	 * @param delimiter
	 * @return string array with split string 
	 */
	public static String[] splitString(String line, String delimiter, String stripString) {
		Vector<String> result = new Vector<String>();
		if (line != null && line.length() > 5) {
			String tmpStr = line.endsWith(delimiter) ? line.substring(0, line.lastIndexOf(delimiter)) : line;
			int endindex = 0;
			while ((endindex = tmpStr.indexOf(delimiter)) != -1) {
				String tmp = tmpStr.substring(0, endindex);
				if (tmp.startsWith(stripString)) {
					tmp = tmp.substring(stripString.length());
				}
				else if (tmp.endsWith(stripString)) {
					tmp = tmp.substring(0, tmp.indexOf(stripString));
				}
				result.add(tmp);
				tmpStr = tmpStr.substring(endindex + delimiter.length(), tmpStr.length());
			}
			if (tmpStr.length() > 0) {
				if (tmpStr.startsWith(stripString))
					tmpStr = tmpStr.substring(stripString.length());
				else if (tmpStr.endsWith(stripString)) tmpStr = tmpStr.substring(0, tmpStr.indexOf(stripString));
				result.add(tmpStr);
			}
			if (log.isLoggable(Level.FINER)) {
				for (String string : result) {
					log.log(Level.FINER, stripString + " = " + string); //$NON-NLS-1$
				}
			}
		}
		return result.toArray(new String[0]);
	}

	/**
	 * method to receive formated data and time in format string like "yyyy-MM-dd, HH:mm:ss"
	 */
	public static String getDateAndTime() {
		return new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new Date().getTime()); //$NON-NLS-1$
	}

	/**
	 * method to receive formated data and time with given format string like "yyyy-MM-dd, HH:mm:ss"
	 */
	public static String getDateAndTime(String format) {
		return new SimpleDateFormat(format).format(new Date().getTime());
	}

	/**
	 * method to get current date
	 */
	public static String getDate() {
		return new SimpleDateFormat("yyyy-MM-dd").format(new Date()); //$NON-NLS-1$
	}

	/**
	 * method to get formated time by given format string and time in millis seconds
	 */
	public static String getFormatedTime(String format, long millisec) {
		return new SimpleDateFormat(format).format(millisec);
	}

	/**
	 * method to get formated duration by given format string and time in millis seconds
	 */
	public static String getFormatedDuration(String format, long millisec) {
		if (millisec > GDE.ONE_HOUR_MS * 24 * 365 * 11) throw new UnsupportedOperationException("use getFormatedTime instead"); //$NON-NLS-1$
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		// avoid wrong number of hours due to a locale setting other than UTC/GMT
		sdf.getTimeZone().setRawOffset(0);
		return sdf.format(millisec);
	}

	/**
	 * @param format for degrees and minutes, e.g. "%2d %07.4f"
	 * @param value is the latitude / longitude value with the fraction range x.0000 to x.5999, e.g. 48,599,999
	 * @return the degrees and minutes, e.g. 48 59.99
	 */
	@Deprecated // currently not used -> was replaced by StringHelper.getFormatedWithMinutes(String, double) for better code readability
	public static String getFormatedLatLonWithMinutes(String format, int value) {
		return String.format(format, value / 1000000, value % 1000000 / 10000.0);
	}

	/**
	 * @param format for degrees (hours) and minutes, e.g. "%2d %09.6f"
	 * @param translatedValue is the decimal latitude / longitude (or hours) value, e.g. 48.999998
	 * @return the degrees (hours) and minutes, e.g. 48 59.999880
	 */
	public static String getFormatedWithMinutes(String format, double translatedValue) {
		String sign = translatedValue < 0. && (int) translatedValue == 0 ? GDE.STRING_MINUS : GDE.STRING_EMPTY; 
		return String.format(sign + format, (int) translatedValue, Math.abs(translatedValue % 1 * 60.0));
	}

	/**
	 * remove blanks within a string
	 * @param inputString
	 * @return cleaned string
	 */
	public static String removeBlanks(String inputString) {
		String[] tmpDev = inputString.split(GDE.STRING_BLANK);
		StringBuilder sb = new StringBuilder();
		for (String tmp : tmpDev) {
			sb.append(tmp);
		}
		return sb.toString();
	}

	public static String intArrayToString(Integer[] values) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; values != null && i < values.length; i++) {
			sb.append(values[i]);
			if (i != values.length - 1) sb.append(';');
		}
		return sb.toString();
	}

	public static String intArrayToString(int[] values) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; values != null && i < values.length; i++) {
			sb.append(values[i]);
			if (i != values.length - 1) sb.append(';');
		}
		return sb.toString();
	}

	public static int[] stringToIntArray(String values) {
		String[] stringValues = values.split(";"); //$NON-NLS-1$
		int[] array = new int[stringValues.length];
		for (int i = 0; i < stringValues.length && stringValues.length > 1; i++) {
			array[i] = Integer.valueOf(stringValues[i].trim()).intValue();
		}
		return array;
	}

	public static String pointArrayToString(Point[] points) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; points != null && i < points.length; i++) {
			sb.append(points[i].x).append(':').append(points[i].y);
			if (i != points.length - 1) sb.append(';');
		}
		return sb.toString();
	}

	public static Point[] stringToPointArray(String values) {
		String[] stringValues = values.split(";"); //$NON-NLS-1$
		Point[] points = new Point[stringValues.length];
		for (int i = 0; i < points.length && points.length > 1; i++) {
			String[] xy = stringValues[i].split(":"); //$NON-NLS-1$
			points[i] = new Point(Integer.valueOf(xy[0].trim()).intValue(), Integer.valueOf(xy[1].trim()).intValue());
		}
		return points;
	}

	/**
	 * print the SWT key code by log info
	 * @param evt
	 */
	public static void printSWTKeyCode(KeyEvent evt) {
		String keyCode = "" + evt.character;
		switch (evt.keyCode) {
		/* Keyboard and Mouse Masks */
		case SWT.ALT:
			keyCode = "ALT";
			break;
		case SWT.SHIFT:
			keyCode = "SHIFT";
			break;
		case SWT.CONTROL:
			keyCode = "CONTROL";
			break;
		case SWT.COMMAND:
			keyCode = "COMMAND";
			break;

		/* Non-Numeric Keypad Keys */
		case SWT.ARROW_UP:
			keyCode = "ARROW_UP";
			break;
		case SWT.ARROW_DOWN:
			keyCode = "ARROW_DOWN";
			break;
		case SWT.ARROW_LEFT:
			keyCode = "ARROW_LEFT";
			break;
		case SWT.ARROW_RIGHT:
			keyCode = "ARROW_RIGHT";
			break;
		case SWT.PAGE_UP:
			keyCode = "PAGE_UP";
			break;
		case SWT.PAGE_DOWN:
			keyCode = "PAGE_DOWN";
			break;
		case SWT.HOME:
			keyCode = "HOME";
			break;
		case SWT.END:
			keyCode = "END";
			break;
		case SWT.INSERT:
			keyCode = "INSERT";
			break;

		/* Virtual and Ascii Keys */
		case SWT.BS:
			keyCode = "BS";
			break;
		case SWT.CR:
			keyCode = "CR";
			break;
		case SWT.DEL:
			keyCode = "DEL";
			break;
		case SWT.ESC:
			keyCode = "ESC";
			break;
		case SWT.LF:
			keyCode = "LF";
			break;
		case SWT.TAB:
			keyCode = "TAB";
			break;

		/* Functions Keys */
		case SWT.F1:
			keyCode = "F1";
			break;
		case SWT.F2:
			keyCode = "F2";
			break;
		case SWT.F3:
			keyCode = "F3";
			break;
		case SWT.F4:
			keyCode = "F4";
			break;
		case SWT.F5:
			keyCode = "F5";
			break;
		case SWT.F6:
			keyCode = "F6";
			break;
		case SWT.F7:
			keyCode = "F7";
			break;
		case SWT.F8:
			keyCode = "F8";
			break;
		case SWT.F9:
			keyCode = "F9";
			break;
		case SWT.F10:
			keyCode = "F10";
			break;
		case SWT.F11:
			keyCode = "F11";
			break;
		case SWT.F12:
			keyCode = "F12";
			break;
		case SWT.F13:
			keyCode = "F13";
			break;
		case SWT.F14:
			keyCode = "F14";
			break;
		case SWT.F15:
			keyCode = "F15";
			break;

		/* Numeric Keypad Keys */
		case SWT.KEYPAD_ADD:
			keyCode = "KEYPAD_ADD";
			break;
		case SWT.KEYPAD_SUBTRACT:
			keyCode = "KEYPAD_SUBTRACT";
			break;
		case SWT.KEYPAD_MULTIPLY:
			keyCode = "KEYPAD_MULTIPLY";
			break;
		case SWT.KEYPAD_DIVIDE:
			keyCode = "KEYPAD_DIVIDE";
			break;
		case SWT.KEYPAD_DECIMAL:
			keyCode = "KEYPAD_DECIMAL";
			break;
		case SWT.KEYPAD_CR:
			keyCode = "KEYPAD_CR";
			break;
		case SWT.KEYPAD_0:
			keyCode = "KEYPAD_0";
			break;
		case SWT.KEYPAD_1:
			keyCode = "KEYPAD_1";
			break;
		case SWT.KEYPAD_2:
			keyCode = "KEYPAD_2";
			break;
		case SWT.KEYPAD_3:
			keyCode = "KEYPAD_3";
			break;
		case SWT.KEYPAD_4:
			keyCode = "KEYPAD_4";
			break;
		case SWT.KEYPAD_5:
			keyCode = "KEYPAD_5";
			break;
		case SWT.KEYPAD_6:
			keyCode = "KEYPAD_6";
			break;
		case SWT.KEYPAD_7:
			keyCode = "KEYPAD_7";
			break;
		case SWT.KEYPAD_8:
			keyCode = "KEYPAD_8";
			break;
		case SWT.KEYPAD_9:
			keyCode = "KEYPAD_9";
			break;
		case SWT.KEYPAD_EQUAL:
			keyCode = "KEYPAD_EQUAL";
			break;

		/* Other keys */
		case SWT.CAPS_LOCK:
			keyCode = "CAPS_LOCK";
			break;
		case SWT.NUM_LOCK:
			keyCode = "NUM_LOCK";
			break;
		case SWT.SCROLL_LOCK:
			keyCode = "SCROLL_LOCK";
			break;
		case SWT.PAUSE:
			keyCode = "PAUSE";
			break;
		case SWT.BREAK:
			keyCode = "BREAK";
			break;
		case SWT.PRINT_SCREEN:
			keyCode = "PRINT_SCREEN";
			break;
		case SWT.HELP:
			keyCode = "HELP";
			break;
		default:
		}
		log.log(Level.INFO, "keyCode = SWT." + keyCode);
	}

	/**
	 * verify the user input while typing port names
	 * @param eventText of the VerifyEvent test containing the char(s) to be verified
	 */
	public static boolean verifyPortInput(String eventText) {
		char[] chars = new char[eventText.length()];
		eventText.getChars(0, chars.length, chars, 0);
		for (int i = 0; i < chars.length; i++) {
			log.log(Level.FINER, "\"" + chars[i] + "\"");
			if (GDE.IS_WINDOWS) {
				if (!('0' <= chars[i] && chars[i] <= '9' || 'c' == chars[i] || 'C' == chars[i] || 'o' == chars[i] || 'O' == chars[i] || 'm' == chars[i] || 'M' == chars[i] || ' ' == chars[i])) {
					return false;
				}
			}
			else if (GDE.IS_LINUX) {
				if (!('0' <= chars[i] && chars[i] <= '9' || '/' == chars[i] || 'd' == chars[i] || 'e' == chars[i] || 'v' == chars[i] || 't' == chars[i] || 'y' == chars[i] || ' ' == chars[i] || 'U' == chars[i]
						|| 'S' == chars[i] || 'B' == chars[i])) {
					return false;
				}
			}
			else if (GDE.IS_MAC) {
				//				if (!(('0' <= chars[i] && chars[i] <= '9') || '/' == chars[i] || '.' == chars[i] || ('a' <= chars[i] && 'z' <= chars[i]) || ('A' <= chars[i] && 'Z' <= chars[i]))) {
				//					return false;
				//				}
			}
		}
		return true;
	}

	/**
	 * verify the user input while typing DataTypes typed input text
	 * @param eventText of the VerifyEvent test containing the char(s) to be verified
	 */
	public static boolean verifyTypedInput(DataTypes useType, String eventText) {
		boolean doIt = true;
		switch (useType) {
		case INTEGER:
			try {
				if (eventText.equals("-"))
					return doIt;
				else if (eventText.equals(GDE.STRING_EMPTY)) eventText = "0";
				Integer.parseInt(eventText.trim());
			}
			catch (Exception e) {
				doIt = false;
			}
			break;
		case DOUBLE:
			try {
				if (eventText.equals("-") || eventText.equals(",") || eventText.equals(".") || eventText.equals(GDE.STRING_EMPTY))
					doIt = true;
				else
					Double.parseDouble(eventText.replace(",", ".").trim());
			}
			catch (Exception e) {
				doIt = false;
			}
			break;
		case BOOLEAN:
			try {
				Boolean.parseBoolean(eventText.trim());
			}
			catch (Exception e) {
				doIt = false;
			}
			break;
		case STRING:
		default:
			doIt = true;
			break;
		}
		return doIt;
	}

	/**
	 * verify the user input while typing DataTypes typed input text
	 * @param eventText of the VerifyEvent test containing the char(s) to be verified
	 */
	public static String verifyTypedString(DataTypes useType, String eventText) {
		String result = eventText;
		while (result.startsWith("0")) {
			result = result.substring(1);
		}
		switch (useType) {
		case INTEGER:
			try {
				if (eventText.replace(',', '.').contains(".")) {
					result = eventText.substring(0, eventText.indexOf('.'));
				}
				Integer.parseInt(result);
			}
			catch (Exception e) {
				result = "0";
			}
			break;
		case DOUBLE:
			try {
				if (!eventText.replace(',', '.').contains(".")) {
					result = eventText + ".0";
				}
				if (result.startsWith(".")) {
					result = "0" + result;
				}
				Double.parseDouble(result);
			}
			catch (Exception e) {
				result = "1.0";
			}
			break;
		case BOOLEAN:
			if (!(result.equals("true") || result.equals("false"))) {
				result = "true";
			}
			break;
		case STRING:
		default:
			break;
		}
		return result;
	}

	/**
	 * return a byte in 8 digits binary representation
	 * @param inByte
	 * @param newLine true appends a new line at the end, false will append a blank
	 */
	public static String printBinary(byte inByte, boolean newLine) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 8; i++) {
			sb.append(((0x80 & inByte) >> 7));
			inByte = (byte) (inByte << 1);
		}
		if (newLine) {
			sb.append(GDE.LINE_SEPARATOR);
		}
		return sb.toString();
	}

	/**
	 * verify the user input while typing hex input '0a0d'
	 * @param eventText of the VerifyEvent test containing the char(s) to be verified
	 */
	public static boolean verifyHexAsString(String eventText) {
		char[] chars = new char[eventText.length()];
		eventText.getChars(0, chars.length, chars, 0);
		for (int i = 0; i < chars.length; i++) {
			log.log(Level.FINER, "\"" + chars[i] + "\"");
			if (!('0' <= chars[i] && chars[i] <= '9' || 'a' == chars[i] || 'A' == chars[i] || 'b' == chars[i] || 'B' == chars[i] || 'c' == chars[i] || 'C' == chars[i] || 'd' == chars[i] || 'D' == chars[i]
					|| 'e' == chars[i] || 'E' == chars[i] || 'f' == chars[i] || 'F' == chars[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * convert a byte array into a 2 hex character string representation
	 * @param bytes
	 * @param size
	 * @return string with converted characters
	 */
	public static String byte2CharString(final byte[] bytes, final int size) {
		StringBuffer sb = new StringBuffer().append(GDE.STRING_LEFT_BRACKET).append(size).append(GDE.STRING_RIGHT_BRACKET_COMMA);
		for (int i = 0; i < size; i++) {
			sb.append(String.format("%c", (char) bytes[i])); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * convert a byte array into a 2 hex character string representation
	 * @param bytes
	 * @param start
	 * @param size
	 * @return string with converted characters
	 */
	public static String byte2CharString(final byte[] bytes, final int start, final int size) {
		StringBuffer sb = new StringBuffer().append(GDE.STRING_LEFT_BRACKET).append(start).append(GDE.STRING_SEMICOLON).append(size).append(GDE.STRING_RIGHT_BRACKET_COMMA);
		for (int i = start >= 0 ? start : 0; i < size; i++) {
			sb.append(String.format("%c", (char) bytes[i])); //$NON-NLS-1$
		}
		return sb.toString();
	}

	public static int byte2hex2int(byte[] bytes, int start, int size) {
		StringBuffer sb = new StringBuffer();
		for (int i = start; i < size + start; i++) {
			sb.append(String.format("%c", (char) bytes[i])); //$NON-NLS-1$
		}
		return Integer.parseInt(sb.toString(), 16);
	}

	public static String fourDigitsRunningNumber(int size) {
		StringBuffer sb = new StringBuffer().append(GDE.STRING_LEFT_BRACKET).append(String.format("%04d", size)).append(GDE.STRING_RIGHT_BRACKET_COMMA);
		for (int i = 0; i < size; i++) {
			sb.append(String.format("%4d", i));
		}
		return sb.toString();
	}

	/**
	 * convert a byte array into a 4 digits 2 hex character string representation
	 * @param bytes
	 * @param size
	 * @return string with converted characters
	 */
	public static String byte2Hex4CharString(byte[] bytes, int size) {
		StringBuffer sb = new StringBuffer().append(GDE.STRING_LEFT_BRACKET).append(String.format("%04d", size)).append(GDE.STRING_RIGHT_BRACKET_COMMA);
		for (int i = 0; i < size; i++) {
			sb.append(String.format("  %02X", bytes[i])); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * convert a byte array into a 2 hex character string representation
	 * @param bytes
	 * @return string with converted characters
	 */
	public static String byte2Hex2CharString(byte[] bytes) {
		final StringBuffer sb = new StringBuffer();
		for (final byte b : bytes) {
			sb.append(String.format("%02X", b)); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * convert a byte array into a 2 hex character string representation
	 * @param bytes
	 * @param size
	 * @return string with converted characters
	 */
	public static String byte2Hex2CharString(byte[] bytes, int size) {
		StringBuffer sb = new StringBuffer().append(GDE.STRING_LEFT_BRACKET).append(String.format("%3d", size)).append(GDE.STRING_RIGHT_BRACKET_COMMA);
		for (int i = 0; i < size; i++) {
			sb.append(String.format(" %02X", bytes[i])); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * convert a byte array into a 2 hex character string representation
	 * @param bytes
	 * @param size
	 * @return string with converted characters
	 */
	public static String byte2Hex2CharString(byte[] bytes, int start, int size) {
		StringBuffer sb = new StringBuffer().append(GDE.STRING_LEFT_BRACKET).append(String.format("%3d", size-start)).append(GDE.STRING_RIGHT_BRACKET_COMMA);
		for (int i = start; i < size; i++) {
			sb.append(String.format(" %02X", bytes[i])); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * convert a byte array inverted into BCD character string representation
	 * @param bytes array of bytes
	 * @param start start position  (start + size - 1) 
	 * @param size iteration length
	 * @return string with converted characters
	 */
	public static String bcdEncodeInverted(byte[] bytes, int start, int size) {
		StringBuffer sb = new StringBuffer();
		for (int i = start+size-1; i >= start; --i) {
			sb.append(String.format("%02X", bytes[i])); //$NON-NLS-1$
		}
		if (Pattern.compile(".*[ABCDEF]").matcher(sb.toString()).find()) {
			log.log(Level.WARNING, "invalid input " + sb);
			return "0";
		}
		return sb.toString();
	}

	/**
	 * convert a byte array into a decimal string representation
	 * @param bytes
	 * @return string with converted characters
	 */
	public static String byte2FourDigitsIntegerString(byte[] bytes) {
		StringBuffer sb = new StringBuffer().append(GDE.STRING_LEFT_BRACKET).append(String.format("%3d", bytes.length)).append(GDE.STRING_RIGHT_BRACKET_COMMA);
		for (int i = 0; i < bytes.length; i++) {
			sb.append(String.format("%4d", (bytes[i] & 0xFF))); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * convert a byte array into a decimal string representation
	 * @param bytes
	 * @return string with converted characters
	 */
	public static String byte2FourDigitsIntegerString(byte[] bytes, byte subtract, int offset, int length) {
		StringBuffer sb = new StringBuffer().append(GDE.STRING_LEFT_BRACKET).append(String.format("%3d", bytes.length)).append(GDE.STRING_RIGHT_BRACKET_COMMA);
		for (int i = offset; i < length; i++) {
			sb.append(String.format("%4d", ((bytes[i] & 0xFF) + subtract))); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * convert a integer array to a 4 hex character string representation
	 * @param values
	 * @return
	 */
	public static String integer2Hex4ByteString(int[] values) {
		StringBuilder sb = new StringBuilder();
		for (int value : values) {
			sb.append(String.format("%04X", value));
		}
		if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, sb.toString());
		return sb.toString();
	}

	/**
	 * convert a string two char per byte input into byte array 
	 * @param twoCharsPerByte
	 * @return byte array with converted characters
	 */
	public static byte[] convert2ByteArray(String twoCharsPerByte) {
		int length = twoCharsPerByte.length() / 2;
		byte[] buffer = new byte[length];
		twoCharsPerByte = twoCharsPerByte + "0";
		for (int i = 0, j = 0; i < length; i++, j += 2) {
			buffer[i] = (byte) Integer.parseInt(twoCharsPerByte.substring(j, j + 2), 16);
		}
		return buffer;
	}

	/**
	 * @param buffer
	 * @return a string were special characters are converted to readable, all others to character
	 */
	public static String convert2CharString(byte[] buffer) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < buffer.length; ++i) {
			if (buffer[i] == DeviceCommPort.FF)
				sb.append(DeviceCommPort.STRING_FF);
			else if (buffer[i] == DeviceCommPort.CR)
				sb.append(DeviceCommPort.STRING_CR);
			else if (buffer[i] == DeviceCommPort.ACK)
				sb.append(DeviceCommPort.STRING_ACK);
			else if (buffer[i] == DeviceCommPort.NAK)
				sb.append(DeviceCommPort.STRING_NAK);
			else if (buffer[i] == -1)
				sb.append('|');
			else if (i == buffer.length - 5)
				sb.append(GDE.STRING_OR).append((char) buffer[i]);
			else
				sb.append((char) buffer[i]);
			//sb.append(String.format("%X", buffer[i]));

			//if (i%4 == 0) sb.append(" ");
		}
		return sb.toString();
	}

	/**
	 * build a sting array from enumeration (combo.setItems(String[]))
	 * @param enumValues
	 * @return string array of the enumeration
	 */
	public static String[] enumValues2StringArray(Object[] enumValues) {
		Vector<String> tmpVec = new Vector<String>();
		for (Object element : enumValues) {
			tmpVec.add(element.toString());
		}
		return tmpVec.toArray(new String[0]);
	}

	/**
	 * convert string list to a string, list elements separated by separator character
	 * @param list
	 * @param separator
	 * @return
	 */
	public static String listToString(List<String> list, char separator) {
		Iterator<String> iter = list.iterator();
		StringBuffer measurements = new StringBuffer(iter.next());
		while (iter.hasNext())
			measurements.append(separator).append(iter.next());
		return measurements.toString();
	}

	/**
	 * convert a separator character separated string to a string list
	 * @param string
	 * @param separator
	 * @return
	 */
	public static List<String> stringToList(String string, char separator) {
		return Arrays.asList(string.split("" + separator));
	}

	/**
	 * integer to string array ( 3 -> {"0", "1", "2"} )
	 * @param items
	 * @return
	 */
	public static String[] int2Array(int items) {
		String[] itemNames = new String[items];
		for (int i = 0; i < items; i++) {
			itemNames[i] = Integer.valueOf(i + 1).toString();
		}
		return itemNames;
	}

	/**
	 * @return 8 bit binary representation
	 */
	public static String byte2bin_8(byte value) {
		StringBuilder sb = new StringBuilder().append("binary : ");
		for (int i = 0, j = 0x80; i < 8; i++, j /= 2) {
			if ((value & j) > 0)
				sb.append("1");
			else
				sb.append("0");
		}
		return sb.toString();
	}

	/**
	 * @return 16 bit binary representation
	 */
	public static String int2bin_16(int value) {
		StringBuilder sb = new StringBuilder().append("binary : ");
		for (int i = 0, j = 0x8000; i < 16; i++, j /= 2) {
			if ((value & j) > 0)
				sb.append("1");
			else
				sb.append("0");
		}
		return sb.toString();
	}

	/**
	 * @return 32 bit binary representation
	 */
	public static String int2bin_32(long value) {
		StringBuilder sb = new StringBuilder().append("binary : ");
		for (int i = 0, j = 0x80000000; i < 32; i++, j /= 2) {
			if ((value & j) > 0)
				sb.append("1");
			else
				sb.append("0");
		}
		return sb.toString();
	}

	/**
	 * print memory area as integer representation
	 * @param name
	 * @param buffer
	 * @param startIndex
	 * @param _byte
	 * @param width
	 * @param count
	 */
	public static void printMemInt(String name, byte[] buffer, int startIndex, int _byte, int width, int count) {
		System.out.println(String.format("%s 0x%04X", name, startIndex));
		for (int i = 0; i < count; ++i) {
			switch (_byte) {
			case 1:
				for (int j = 0; j < width; j++) {
					System.out.print(String.format("%02d", buffer[startIndex]));
					++startIndex;
				}
				break;
			case 2:
				for (int j = 0; j < width; j++) {
					System.out.print(DataParser.parse2Short(buffer, startIndex));
					startIndex += 2;
				}
				break;
			}
			System.out.print("; ");
		}
		System.out.println();
	}

	/**
	 * print memory area in hex representation
	 * @param name
	 * @param buffer
	 * @param startIndex
	 * @param _byte
	 * @param width
	 * @param count
	 */
	public static void printMemHex(String name, byte[] buffer, int startIndex, int _byte, int width, int count) {
		System.out.println(String.format("%s 0x%04X", name, startIndex));
		for (int i = 0; i < count; ++i) {
			switch (_byte) {
			case 1:
				for (int j = 0; j < width; j++) {
					System.out.print(String.format("%02x", buffer[startIndex]));
					++startIndex;
				}
				break;
			case 2:
				for (int j = 0; j < width; j++) {
					System.out.print(String.format("%02x%02x", buffer[startIndex], buffer[startIndex + 1]));
					startIndex += 2;
				}
				break;
			}
			System.out.print("; ");
		}
		System.out.println();
	}

	/**
	 * print memory area in character representation
	 * @param name
	 * @param buffer
	 * @param startIndex
	 * @param _byte
	 * @param width
	 * @param count
	 */
	public static void printMemChar(String name, byte[] buffer, int startIndex, int _byte, int width, int count) {
		System.out.println(String.format("%s 0x%04X", name, startIndex));
		for (int i = 0; i < count; ++i) {
			switch (_byte) {
			case 1:
				for (int j = 0; j < width; j++) {
					System.out.print(String.format("%c", buffer[startIndex]));
					++startIndex;
				}
				break;
			case 2:
				for (int j = 0; j < width; j++) {
					System.out.print(String.format("%c%c", buffer[startIndex], buffer[startIndex + 1]));
					startIndex += 2;
				}
				break;
			}
			System.out.print("; ");
		}
		System.out.println();
	}

	/**
	 * transfer from char array to string (new String(name.getBytes("ISO-8859-1"), "UTF-8"))
	 * @param array
	 * @return
	 */
	public static String transfer(String array) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length(); i++) {
			sb.append(array.charAt(i));
		}
		return sb.toString();
	}

	public static String center(String s, int size) {
		return center(s, size, ' ');
	}

	public static String center(String s, int size, char pad) {
		if (s == null || size <= s.length()) return s;

		StringBuilder sb = new StringBuilder(size);
		for (int i = 0; i < (size - s.length()) / 2; i++) {
			sb.append(pad);
		}
		sb.append(s);
		while (sb.length() < size) {
			sb.append(pad);
		}
		return sb.toString();
	}
	
	public static String arrayToString(Object[] inputArray) {
		StringBuffer sb = new StringBuffer();
		for (Object object : inputArray) {
			sb.append(object).append(GDE.STRING_COMMA_BLANK);
		}
		return sb.toString();
	}
	
	public static String arrayToString(int[] inputArray) {
		StringBuffer sb = new StringBuffer();
		for (Object object : inputArray) {
			sb.append(object).append(GDE.STRING_COMMA_BLANK);
		}
		return sb.toString();
	}

	public static Object arrayToString(byte[] inputArray) {
		StringBuffer sb = new StringBuffer();
		for (Object object : inputArray) {
			sb.append(object).append(GDE.STRING_COMMA_BLANK);
		}
		return sb.toString();
	}
}
