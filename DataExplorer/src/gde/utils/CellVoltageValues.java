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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * Class to maintain the voltage levels of Lithium batteries used for cell voltage bar graph, ...
 * @author Winfried Brügmann
 */
public class CellVoltageValues {
	final static String					$CLASS_NAME						= CellVoltageValues.class.getName();
	final static Logger					log										= Logger.getLogger(CellVoltageValues.$CLASS_NAME);
	
	public enum CellVoltageTypes { LiPo, LiIo, LiFe, NiMh, Custom }
	
	public final List<String> voltageLimitsArray = new ArrayList<String>();
	
	public final static int[]			liPoLimits					= new int[] {4200, 4200, 4150, 4000, 3300, 3200};
	public final static int[]			liIoLimits					= new int[] {4100, 4100, 4050, 3900, 3200, 3100};
	public final static int[]			liFeLimits					= new int[] {3600, 3600, 3450, 3300, 2700, 2600};
	public final static int[]			niMhLimits					= new int[] {1450, 1450, 1350, 1000, 900, 800};

	// all initial values fit to LiPo akku type LiPo 3.7 V, LiIo 3.6 V LiFe 3.3 V
	static int[]	voltageLimits				= new int[] {4200, 4200, 4150, 4000, 3300, 3200};
	
	private static CellVoltageValues cellVoltageValues = null;
	
	private CellVoltageValues() {
		for (double i = 4.400; i > 0.600; i-=0.005) {
			voltageLimitsArray.add(String.format("%5.3f", i));
		}
	}
	
	public static CellVoltageValues getCellVoltageValues() {
		if (CellVoltageValues.cellVoltageValues == null) {
			CellVoltageValues.cellVoltageValues = new CellVoltageValues();
		}
		return CellVoltageValues.cellVoltageValues;
	}
	
	public static int[] getVoltageLimits(CellVoltageTypes cellVoltageType) {
		switch (cellVoltageType) {
		case LiPo:
			return CellVoltageValues.setVoltageLimits(liPoLimits);
			
		case LiIo:
			return CellVoltageValues.setVoltageLimits(liIoLimits);
			
		case LiFe:
			return CellVoltageValues.setVoltageLimits(liFeLimits);
			
		case NiMh:
			return CellVoltageValues.setVoltageLimits(niMhLimits);
			
		default:
		case Custom:
			return voltageLimits;			
		}
	}

	/**
	 * @return the voltage limits as int array
	 */
	public static int[] getVoltageLimits() {
		return voltageLimits.clone();
	}

	/**
	 * @return the voltage limits as int array
	 */
	public static String[] getVoltageLimitsStringArray() {
		return CellVoltageValues.getCellVoltageValues().voltageLimitsArray.toArray(new String[0]);
	}

	/**
	 * set the voltage limits for the bar graph
	 * @param newUpperLimitVoltage the upperLimitVoltage to set
	 * @param newUpperLimitColorRed the upperLimitColorRed to set
	 * @param newLowerLimitColorRed the lowerLimitColorRed to set
	 * @param newBeginSpreadVoltage the beginSpreadVoltage to set
	 * @param newLowerLimitColorGreen the lowerLimitColorGreen to set
	 * @param newLowerLimitVoltage the lowerLimitVoltage to set
	 */
	public static void setVoltageLimits(int newUpperLimitVoltage, int newUpperLimitColorRed, int newLowerLimitColorGreen, int newBeginSpreadVoltage, int newLowerLimitColorRed, int newLowerLimitVoltage) {
		voltageLimits[0] = newUpperLimitVoltage;
		voltageLimits[1] = newUpperLimitColorRed;
		voltageLimits[2] = newLowerLimitColorGreen;
		voltageLimits[3] = newBeginSpreadVoltage;
		voltageLimits[4] = newLowerLimitColorRed;
		voltageLimits[5] = newLowerLimitVoltage;
	}

	/**
	 * set the voltage limits for the bar graph
	 * @param newVoltageLimits the voltage limits to set
	 */
	public static int[] setVoltageLimits(int[] newVoltageLimits) {
		if (voltageLimits.length <= newVoltageLimits.length)
			System.arraycopy(newVoltageLimits, 0, voltageLimits, 0, voltageLimits.length);
		else if (voltageLimits.length > newVoltageLimits.length)
			System.arraycopy(newVoltageLimits, 0, voltageLimits, 0, newVoltageLimits.length);			
		
		return voltageLimits;
	}

	/**
	 * method to compare voltage limit settings
	 * @return true if all limits match limits2compare values
	 */
	public static boolean compareVoltageLimits(int[] limits2compared) {
		return voltageLimits[0] == limits2compared[0] 
		    && voltageLimits[1] == limits2compared[1] 
		    && voltageLimits[2] == limits2compared[2] 
		    && voltageLimits[3] == limits2compared[3] 
		    && voltageLimits[4] == limits2compared[4] 
		    && voltageLimits[5] == limits2compared[5];
	}

}
