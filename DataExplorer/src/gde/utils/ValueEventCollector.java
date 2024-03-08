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

import java.util.Arrays;
import java.util.TreeMap;
import java.util.logging.Logger;

import gde.log.Level;

/**
 * this class collects ValueCollector events sorted by one of the values used as key
 * an average calculation using the previous detected maximal event count can be queried
 * the maximal count can be queried whenever required
 *
 */
public class ValueEventCollector extends TreeMap<Integer, ValueCollector> {
	private static final Logger	log								= Logger.getLogger(ValueEventCollector.class.getName());
	private static final long		serialVersionUID	= 1L;
	public final static String	LF								= System.getProperty("line.separator");

	int													maxCount, minCount;
	int													avgOffsetMaxCount, avgOffsetMinCount;

	public ValueEventCollector() {
		reset();
	}

	/**
	 * Add a pair of values.
	 * Note: The values will be added to other values with the same valueA (entry).
	 *       If none exist a new ValueCollector entry will be created.
	 * @param valueA e.g. the static pressure value times 1000 according GDE data model
	 * @param valueB e.g. the TEC tube pressure value times 1000 according GDE data model
	 */
	public void addValuePair(int valueA, int valueB) {
		if (null == get(valueA))
			put(valueA, new ValueCollector(valueA, valueB));
		else
			get(valueA).add(valueA, valueB);
	}

	/**
	 * @return average offset or difference between the two given values of maximal entry count
	 */
	public int getAvgDiffMaxCount() {
		if (avgOffsetMaxCount == Integer.MIN_VALUE) {
			maxCount = Integer.MIN_VALUE;
			int maxCountKey = 0;
			for (int key : this.keySet()) {
				if (get(key).getCount() > maxCount) {
					maxCount = get(key).getCount();
					maxCountKey = key;
					//log.log(Level.OFF, "pressureOffsetCount = " + maxCount);
				} else if (get(key).getCount() == maxCount) {
					if (get(maxCountKey).getAvgOffset() > get(key).getAvgOffset()) {
						maxCountKey = key;
					}
				}
			}
			if (get(maxCountKey) != null) {
				avgOffsetMaxCount = get(maxCountKey).getAvgOffset();
				//log.log(Level.OFF, String.format("pressureOffsetCount = %3d avgOffset = %.3f hPa", get(maxCountKey).getCount(), avgOffsetMaxCount/1000.));
			}
		}
		return avgOffsetMaxCount;
	}

	/**
	 * @return average offset or difference between the two given values of maximal entry count
	 */
	public int getAvgDiffMinCount() {
		if (avgOffsetMinCount == Integer.MAX_VALUE) {
			minCount = Integer.MAX_VALUE;
			int minCountKey = 0;
			for (int key : this.keySet()) {
				if (get(key).getCount() < minCount) {
					minCount = get(key).getCount();
					minCountKey = key;
					//log.log(Level.OFF, "pressureOffsetCount = " + minCount);
				}
			}
			if (get(minCountKey) != null) {
				avgOffsetMinCount = get(minCountKey).getAvgOffset();
				//log.log(Level.OFF, String.format("pressureOffsetCount = %3d avgOffset = %.3f hPa", get(minCountKey).getCount(), avgOffsetMaxCount/1000.));
			}
		}
		return avgOffsetMinCount;
	}

	/**
	 * @return maximal event count over all entries
	 */
	public int getMaxEntryCount() {
		if (maxCount == Integer.MIN_VALUE) {
			for (int key : this.keySet()) {
				if (get(key).getCount() > maxCount) {
					maxCount = get(key).getCount();
					//log.log(Level.OFF, "valueOffsetCount = " + maxCount);
				}
			}
		}
		return maxCount;
	}

	/**
	 * @return minimal event count over all entries
	 */
	public int getMinEntryCount() {
		if (minCount == Integer.MAX_VALUE) {
			for (int key : this.keySet()) {
				if (get(key).getCount() < minCount) {
					minCount = get(key).getCount();
					//log.log(Level.OFF, "valueOffsetCount = " + minCount);
				}
			}
		}
		return minCount;
	}

	/**
	 * reset initial values to enable previous called maximal count or offset average
	 */
	public void reset() {
		maxCount = Integer.MIN_VALUE;
		minCount = Integer.MAX_VALUE;
		avgOffsetMaxCount = Integer.MIN_VALUE;
		avgOffsetMinCount = Integer.MAX_VALUE;
	}

	@Override
	public String toString() {
		return new StringBuffer(getClass().getSimpleName()).append(": ").append(size()).append(" entries, ")
				.append("max.count=").append(getMaxEntryCount()).append(", min.count=").append(getMinEntryCount())
				.append(", avg.offset@max.count=").append(getAvgDiffMaxCount()).append(", avg.offset@min.count=").append(getAvgDiffMinCount()).toString();
	}

	public String listValues() {
		StringBuffer sb = new StringBuffer("number of value entries=").append(size()).append(LF);
		// sort by static pressure first
		int[] arKeys = new int[size()];
		int i = 0;
		for (int key : this.keySet()) {
			arKeys[i++] = key;
		}
		Arrays.sort(arKeys);
		for (int k = 0; k < arKeys.length; k++) {
			sb.append(get(arKeys[k]).toString()).append(LF);
		}
		return sb.toString();
	}

	public static void main(String[] args) {

		// real samples from before flight.
		double[] arStaticPressureHPa = new double[] { 965.51, 965.52, 965.52, 965.51, 965.49, 965.5, 965.51, 965.49, 965.49, 965.49, 965.5 };
		double[] arTECPressureHPa = new double[] { 965.5, 965.5, 965.51, 965.48, 965.48, 965.49, 965.49, 965.48, 965.47, 965.47, 965.49 };

		// convert to int values by multiplying with 1000
		int[] arStaticPressure = new int[arStaticPressureHPa.length];
		int[] arTECPressure = new int[arTECPressureHPa.length];
		for (int i = 0; i < arStaticPressure.length; i++) {
			arStaticPressure[i] = (int) (arStaticPressureHPa[i] * 1000);
			arTECPressure[i] = (int) (arTECPressureHPa[i] * 1000);
		}
		// let the ValueEventCollector consume the pressures
		ValueEventCollector vec = new ValueEventCollector();
		for (int i = 0; i < arStaticPressure.length; i++) {
			vec.addValuePair(arStaticPressure[i], arTECPressure[i]);
		}
		// dump result
		log.log(Level.OFF, vec.toString());
		log.log(Level.OFF, vec.listValues());
	}
}
