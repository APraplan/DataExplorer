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

import org.eclipse.swt.SWT;

/**
 * @author Winfried Brügmann
 * helper class to analyse events
 */
public class EventUtils {


  static String stateMask(int stateMask) {
    String string = "";
    if ((stateMask & SWT.CTRL) != 0)
      string += " CTRL";
    if ((stateMask & SWT.ALT) != 0)
      string += " ALT";
    if ((stateMask & SWT.SHIFT) != 0)
      string += " SHIFT";
    if ((stateMask & SWT.COMMAND) != 0)
      string += " COMMAND";
    return string;
  }

	
  static String character(char character) {
    switch (character) {
    case 0:
      return "'\\0'";
    case SWT.BS:
      return "'\\b'";
    case SWT.CR:
      return "'\\r'";
    case SWT.DEL:
      return "DEL";
    case SWT.ESC:
      return "ESC";
    case SWT.LF:
      return "'\\n'";
    case SWT.TAB:
      return "'\\t'";
    }
    return "'" + character + "'";
  }

  static String keyCode(int keyCode) {
    switch (keyCode) {

    /* Keyboard and Mouse Masks */
    case SWT.ALT:
      return "ALT";
    case SWT.SHIFT:
      return "SHIFT";
    case SWT.CONTROL:
      return "CONTROL";
    case SWT.COMMAND:
      return "COMMAND";

    /* Non-Numeric Keypad Keys */
    case SWT.ARROW_UP:
      return "ARROW_UP";
    case SWT.ARROW_DOWN:
      return "ARROW_DOWN";
    case SWT.ARROW_LEFT:
      return "ARROW_LEFT";
    case SWT.ARROW_RIGHT:
      return "ARROW_RIGHT";
    case SWT.PAGE_UP:
      return "PAGE_UP";
    case SWT.PAGE_DOWN:
      return "PAGE_DOWN";
    case SWT.HOME:
      return "HOME";
    case SWT.END:
      return "END";
    case SWT.INSERT:
      return "INSERT";

    /* Virtual and Ascii Keys */
    case SWT.BS:
      return "BS";
    case SWT.CR:
      return "CR";
    case SWT.DEL:
      return "DEL";
    case SWT.ESC:
      return "ESC";
    case SWT.LF:
      return "LF";
    case SWT.TAB:
      return "TAB";

    /* Functions Keys */
    case SWT.F1:
      return "F1";
    case SWT.F2:
      return "F2";
    case SWT.F3:
      return "F3";
    case SWT.F4:
      return "F4";
    case SWT.F5:
      return "F5";
    case SWT.F6:
      return "F6";
    case SWT.F7:
      return "F7";
    case SWT.F8:
      return "F8";
    case SWT.F9:
      return "F9";
    case SWT.F10:
      return "F10";
    case SWT.F11:
      return "F11";
    case SWT.F12:
      return "F12";
    case SWT.F13:
      return "F13";
    case SWT.F14:
      return "F14";
    case SWT.F15:
      return "F15";

    /* Numeric Keypad Keys */
    case SWT.KEYPAD_ADD:
      return "KEYPAD_ADD";
    case SWT.KEYPAD_SUBTRACT:
      return "KEYPAD_SUBTRACT";
    case SWT.KEYPAD_MULTIPLY:
      return "KEYPAD_MULTIPLY";
    case SWT.KEYPAD_DIVIDE:
      return "KEYPAD_DIVIDE";
    case SWT.KEYPAD_DECIMAL:
      return "KEYPAD_DECIMAL";
    case SWT.KEYPAD_CR:
      return "KEYPAD_CR";
    case SWT.KEYPAD_0:
      return "KEYPAD_0";
    case SWT.KEYPAD_1:
      return "KEYPAD_1";
    case SWT.KEYPAD_2:
      return "KEYPAD_2";
    case SWT.KEYPAD_3:
      return "KEYPAD_3";
    case SWT.KEYPAD_4:
      return "KEYPAD_4";
    case SWT.KEYPAD_5:
      return "KEYPAD_5";
    case SWT.KEYPAD_6:
      return "KEYPAD_6";
    case SWT.KEYPAD_7:
      return "KEYPAD_7";
    case SWT.KEYPAD_8:
      return "KEYPAD_8";
    case SWT.KEYPAD_9:
      return "KEYPAD_9";
    case SWT.KEYPAD_EQUAL:
      return "KEYPAD_EQUAL";

    /* Other keys */
    case SWT.CAPS_LOCK:
      return "CAPS_LOCK";
    case SWT.NUM_LOCK:
      return "NUM_LOCK";
    case SWT.SCROLL_LOCK:
      return "SCROLL_LOCK";
    case SWT.PAUSE:
      return "PAUSE";
    case SWT.BREAK:
      return "BREAK";
    case SWT.PRINT_SCREEN:
      return "PRINT_SCREEN";
    case SWT.HELP:
      return "HELP";
    }
    return character((char) keyCode);
  }

}
