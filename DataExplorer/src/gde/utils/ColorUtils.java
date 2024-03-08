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

    Copyright (c) 2018 Thomas Eickert
****************************************************************************************/

package gde.utils;

import java.util.Random;

import org.eclipse.swt.graphics.Color;

import gde.GDE;
import gde.ui.SWTResourceManager;

/**
 * Simple helper methods related to colors.
 * No input value checks.
 * @author Thomas Eickert (USER)
 */
public final class ColorUtils {

	/**
	 * @param ordinal is a zero based positive integer (e.g. a record ordinal)
	 * @return the CSV value mapped to the ordinal (e.g. 0,0,0 for black)
	 */
	public static String getDefaultRgb(int ordinal) {
		final String rgb;
		switch (ordinal) {
		case 0: // erste Kurve
			rgb = "0,0,255"; // (SWT.COLOR_BLUE));
			break;
		case 1: // zweite Kurve
			rgb = "0,128,0"; // SWT.COLOR_DARK_GREEN));
			break;
		case 2: // dritte Kurve
			rgb = "128,0,0"; // (SWT.COLOR_DARK_RED));
			break;
		case 3: // vierte Kurve
			rgb = "255,0,255"; // (SWT.COLOR_MAGENTA));
			break;
		case 4: // fünfte Kurve
			rgb = "64,0,64"; // (SWT.COLOR_CYAN));
			break;
		case 5: // sechste Kurve
			rgb = "0,128,128"; // (SWT.COLOR_DARK_YELLOW));
			break;
		case 6: // Kurve
			rgb = "128,128,0";
			break;
		case 7: // Kurve
			rgb = "128,0,128";
			break;
		case 8: // Kurve
			rgb = "0,128,255";
			break;
		case 9: // Kurve
			rgb = "128,255,0";
			break;
		case 10: // Kurve
			rgb = "255,0,128";
			break;
		case 11: // Kurve
			rgb = "0,64,128";
			break;
		case 12: // Kurve
			rgb = "64,128,0";
			break;
		case 13: // Kurve
			rgb = "128,0,64";
			break;
		case 14: // Kurve
			rgb = "128,64,0";
			break;
		case 15: // Kurve
			rgb = "0,128,64";
			break;
		default:
			Random rand = new Random();
			rgb = toRGB(rand.nextInt() & 0xff, rand.nextInt() & 0xff, rand.nextInt() & 0xff);
			break;
		}
		return rgb;
	}

	/**
	 * @param red between 0 to 255
	 * @param green between 0 to 255
	 * @param blue between 0 to 255
	 * @return the CSV value (e.g. 0,0,0 for black)
	 */
	public static String toRGB(int red, int green, int blue) {
		return red + GDE.STRING_COMMA + green + GDE.STRING_COMMA + blue;
	}

	/**
	 * @return the int value (e.g. 0 for black, 2^24 -1 for white)
	 */
	public static int toRGB(String rgb) {
		int r, g, b;
		r = Integer.parseInt(rgb.split(GDE.STRING_COMMA)[0]);
		g = Integer.parseInt(rgb.split(GDE.STRING_COMMA)[1]);
		b = Integer.parseInt(rgb.split(GDE.STRING_COMMA)[2]);
		return toIntRGB(r, g, b);
	}

	/**
	 * @param rgb is the composite rgb value between 0 and 2^24 - 1 (i.e. 16777215)
	 * @return the CSV value (e.g. 0,0,0 for black)
	 */
	public static String toRGB(int rgb) {
		return toRGB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF);
	}

	/**
	 * @return the int value (e.g. 0 for black, 255 for blue)
	 */
	public static int toIntRGB(int red, int green, int blue) {
		return red << 16 + green << 8 + blue;
	}

	public static Color getColor(String rgb) {
		int r, g, b;
		r = Integer.parseInt(rgb.split(GDE.STRING_COMMA)[0]);
		g = Integer.parseInt(rgb.split(GDE.STRING_COMMA)[1]);
		b = Integer.parseInt(rgb.split(GDE.STRING_COMMA)[2]);
		Color color = SWTResourceManager.getColor(r, g, b);
		return color;
	}
}
