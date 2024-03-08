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
    
    Copyright (c) 2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.elprog;

import java.util.logging.Logger;

import gde.GDE;
import gde.device.CheckSumTypes;
import gde.device.FormatTypes;
import gde.device.InputTypes;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.io.DataParser;

/**
 * @author brueg
 *
 */
public class PulsarDataParser extends DataParser {
	static Logger			log										= Logger.getLogger(DataParser.class.getName());

	protected double integratedEnergy = 0;
	protected final int offset;
	protected int intermediateState;
	
	/**
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType
	 * @param useDataSize
	 */
	public PulsarDataParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, int useDataSize, int offset) {
		super(useTimeFactor, useLeaderChar, useSeparator, useCheckSumType, useDataSize);
		this.offset = offset;
	}

	/**
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType
	 * @param useCheckSumFormatType
	 * @param useDataSize
	 * @param useDataFormatType
	 * @param doMultiply1000
	 */
	public PulsarDataParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, FormatTypes useCheckSumFormatType, int useDataSize, FormatTypes useDataFormatType,
			boolean doMultiply1000, int offset) {
		super(useTimeFactor, useLeaderChar, useSeparator, useCheckSumType, useCheckSumFormatType, useDataSize, useDataFormatType, doMultiply1000);
		this.offset = offset;
	}
	/**
	 * constructor to initialize required configuration parameter
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType 
	 * @param useCheckSumFormatType if null, no checksum calculation will occur
	 * @param useDataSize
	 * @param useDataFormatType FormatTypes.TEXT where dataBlockSize specifies the number of contained values, FormatTypes.BINARY where dataBlockSize specifies the number of bytes received via seral connection
	 * @param doMultiply1000 some transferred values are already in a format required to enable 3 digits 
	 */
	public PulsarDataParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, FormatTypes useCheckSumFormatType, int useDataSize, FormatTypes useDataFormatType, boolean doMultiply1000) {
		super(useTimeFactor, useLeaderChar, useSeparator, useCheckSumType, useCheckSumFormatType, useDataSize, useDataFormatType, doMultiply1000);
		this.offset = 2;
	}


	/**
	 * default parse method for #03D11____B2,00000,11302,00000,000,00000,00000,3772,0,3768,0,3770,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0 like lines
	 * @param inputLine
	 * @param strValues
	 * @throws DevicePropertiesInconsistenceException
	 */
	@Override
	public void parse(String inputLine, int lineNum) throws DevicePropertiesInconsistenceException {
		String[] strValues = inputLine.split(this.separator);
		String strValue = strValues[0].trim();
		
		switch (inputLine.charAt(0)) {
		case '#':
		default:
			//#03C05____B4,00070,11625,06572,000,00064,00037,3879,0,3887,0,3880,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0
			this.state = strValue.charAt(3);
			this.valueSize = 40;
			this.values = new int[Math.abs(this.device.getDataBlockSize(InputTypes.FILE_IO))];
			int[] tmpValues = new int[this.valueSize];

			for (int i = 0; i < this.valueSize-this.offset-1; i++) {
				strValue = strValues[i + this.offset].trim();
				try {
					tmpValues[i] = strValue.length() > 0 ? Integer.parseInt(strValue) : 0;
					if (i >= 5 ) {
						++i;
	 					strValue = strValues[i + this.offset].trim();
						tmpValues[i] = strValue.length() > 0 && !strValue.equals(GDE.STRING_COLON) ? Integer.parseInt(strValue) : 100;
					}
				}
				catch (NumberFormatException e) {
					tmpValues[i] = 0;
				}
			}
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			
			//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie
			values[0] = tmpValues[0];
			values[1] = tmpValues[1];
			values[2] = tmpValues[3] * 1000;			
			values[3] = Double.valueOf((values[0] / 1000.0) * (values[1] / 1000.0) * 1000).intValue();							// power U*I [W]
			values[4] = Double.valueOf(integratedEnergy += ((values[0] / 1000.0 * values[1] / 1000.0) / 3600.0 + 0.5) * 1000.).intValue();
			//values[4] = Double.valueOf((values[0] / 1000.0) * (values[2] / 1000.0)).intValue();											// energy U*C [mWh]
			//5=Temperature 6=Ri
			values[5] = tmpValues[2];
			values[6] = tmpValues[4];
			//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
			for (int i = 0,j=0; i < 16; ++i,j+=2) {
				values[i+8] = tmpValues[j+5];
				if (values[i + 8] > 0) {
					maxVotage = values[i + 8] > maxVotage ? values[i + 8] : maxVotage;
					minVotage = values[i + 8] < minVotage ? values[i + 8] : minVotage;
				}
				//System.out.println(String.format("zelle %d = %d", i,values[i+8]));
			}
			//7=Balance
			values[7] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 1000;
			//24=BalancerZelle1 25=BalancerZelle2 26=BalancerZelle3 27=BalancerZelle4 28=BalancerZelle5 29=BalancerZelle6 ... 39=BalancerZelle16
			for (int i = 0, j = 0; i < 16; ++i, j+=2) {
				values[i+40] = tmpValues[j+6];
				//System.out.println(String.format("power %d = %d", i,values[i+24]));
			}
			//detect program ending
			//#03D05__M_B4 -> manual ending
			//#03C05__EPB4 -> program finished
			//#04D01__EE_0 -> program cycle ended
			if ((strValue = strValues[0].trim()).charAt(8) != '_')
				this.intermediateState = strValue.charAt(8);
			break;
			
		case '!': //cell resistance
			//!060,057,059,000,000,000,000,000,000,000,000,000,000,000,000,000
			strValues = inputLine.substring(1).split(this.separator);
			for (int i = 0; i < 16; ++i) {
				try {
					values[i+24] = strValues[i].length() > 0 ? Integer.parseInt(strValues[i]) * 100 : 0;
				}
				catch (NumberFormatException e) {
					values[i+24] = 0;
				}
			}			
			break;
		}
	}

	/**
	 * @return the values
	 */
	public int[] getValues(int[] existingPoints) {
		for (int i=0; i<existingPoints.length && i<this.values.length; ++i) {
			if (existingPoints[i] != 0)
				this.values[i] = existingPoints[i];
		}
		return this.values;
	}

}
