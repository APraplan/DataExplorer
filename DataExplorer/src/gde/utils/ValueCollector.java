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

    Copyright (c) 2022 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

/**
 * this class collect values to enable average calculation of the value difference (offset) or average of both collected values
 */
public class ValueCollector {
	int count = 0;
	int sumValueA = 0;
	int sumValueB = 0;
	
	public ValueCollector() {};

	/**
	 * initialize class while initializing count and the two pressure values
	 * @param valueA a value times 1000 according GDE data model, as example static pressure
	 * @param valueB a value times 1000 according GDE data model, as example TEC pressure
	 */
	public ValueCollector(int valueA, int valueB) {
		count = 1;
		sumValueA = valueA;
		sumValueB = valueB;			
	}

	/**
	 * add up count and the two given pressure values
	 * @param valueA as example static pressure value times 1000 according GDE data model
	 * @param valueB as example TEC tube pressure value times 1000 according GDE data model
	 */
	public void add(int valueA, int valueB) {
		count += 1;
		sumValueA += valueA;
		sumValueB += valueB;			
	}

	/**
	 * @return actual count of entries
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @return offset average of collected values A - values B divided by count
	 */
	public int getAvgOffset() {
		return count > 0 ? (sumValueA - sumValueB) / count : 0;
	}

	/**
	 * @return average of counted values A
	 */
	public int getAvgValuesA() {
		return count > 0 ? sumValueA / count : 0;
	}

	/**
	 * @return average of counted values B
	 */
	public int getAvgValuesB() {
		return count > 0 ? sumValueB / count : 0;
	}

	@Override
	public String toString() {
		return new StringBuffer(getClass().getSimpleName()).append(": ")
		  .append( count ).append(" value pairs, ")
		  .append("avg.valueA=").append(getAvgValuesA()).append(" ,avg.valueB=").append(getAvgValuesB())
		  .append(", avg.offset=").append(getAvgOffset()).toString();
	}
}

