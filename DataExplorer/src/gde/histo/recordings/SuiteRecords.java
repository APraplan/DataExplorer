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

    Copyright (c) 2017,2018,2019 Thomas Eickert
****************************************************************************************/

package gde.histo.recordings;

import java.util.HashMap;
import java.util.Map;

/**
 * Suite records for trail records.
 * Used if a suite trail type is selected the trail record.
 * @author Thomas Eickert
 */
public final class SuiteRecords {

	Map<Integer, SuiteRecord> suiteRecords = new HashMap<>();

	public void clear() {
		suiteRecords.clear();
	}

	/**
	 * @param memberIndex is 0-based and corresponds to the suite members index of the trail type
	 */
	public SuiteRecord get(int memberIndex) {
		return suiteRecords.get(memberIndex);
	}

	/**
	 * @param memberIndex is 0-based and corresponds to the suite members index of the trail type
	 * @param suiteRecord
	 */
	public SuiteRecord put(int memberIndex, SuiteRecord suiteRecord) {
		return suiteRecords.put(memberIndex, suiteRecord);
	}

	public int getSuiteMaxValue() {
		return suiteRecords.values().parallelStream().mapToInt(SuiteRecord::getMaxRecordValue).max().orElseThrow(UnsupportedOperationException::new);
	}

	public int getSuiteMinValue() {
		return suiteRecords.values().parallelStream().mapToInt(SuiteRecord::getMinRecordValue).min().orElseThrow(UnsupportedOperationException::new);
	}

	public int realSize() {
		int result = 0;
		for (SuiteRecord suiteRecord : suiteRecords.values()) {
			result = suiteRecord.size();
			break;
		}
		return result;
	}

	public int getSuiteLength() {
		return suiteRecords.size();
	}

	/**
	 * @param memberIndex is 0-based and corresponds to the suite members index of the trail type
	 * @param index
	 * @return the point value at the index position of the suite record identified by the suite ordinal
	 */
	public Integer getSuiteValue(int memberIndex, int index) {
		return suiteRecords.get(memberIndex).elementAt(index);
	}

	@Override
	public String toString() {
		return "SuiteRecords [getSuiteMaxValue()=" + this.getSuiteMaxValue() + ", getSuiteMinValue()=" + this.getSuiteMinValue() + ", realSize()=" + this.realSize() + ", getSuiteLength()=" + this.getSuiteLength() + "]";
	}

}
