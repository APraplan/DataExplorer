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

package gde.histo.settlements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gde.Analyzer;
import gde.data.Record;
import gde.device.MeasurementMappingType;
import gde.device.ReferenceGroupType;
import gde.device.ReferenceRuleTypes;
import gde.histo.utils.Spot;
import gde.log.Logger;

/**
 * Perform the aggregation of translated record values.
 * The aggregation is based on the reference rule.
 * @author Thomas Eickert
 */
public final class RecordGroup {
	private final static String				$CLASS_NAME	= RecordGroup.class.getName();
	private final static Logger				log					= Logger.getLogger($CLASS_NAME);

	private final ReferenceGroupType	referenceGroupType;
	private final SettlementRecord		settlementRecord;
	private final Analyzer						analyzer;
	private final Record[]						records;

	public RecordGroup(SettlementRecord settlementRecord, ReferenceGroupType referenceGroupType, Analyzer analyzer) {
		this.settlementRecord = settlementRecord;
		this.analyzer = analyzer;
		this.referenceGroupType = referenceGroupType;
		this.records = new Record[referenceGroupType.getMeasurementMapping().size() + referenceGroupType.getSettlementMapping().size()];

		int i = 0;
		for (MeasurementMappingType measurementMappingType : referenceGroupType.getMeasurementMapping()) {
			this.records[i] = settlementRecord.getParent().get(settlementRecord.getParent().getRecordNames()[measurementMappingType.getMeasurementOrdinal()]);
			i++;
		}
		if (!referenceGroupType.getSettlementMapping().isEmpty()) throw new UnsupportedOperationException(
				"settlements based on settlements not supported");
	}

	/**
	 * @return true if at least one of the records contains reasonable data which can be displayed
	 */
	public boolean hasReasonableData() {
		for (Record record : this.records) {
			if (record.hasReasonableData()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the aggregated translated maximum value
	 */
	public double getRealMax() {
		double result = 0;
		for (int i = 0; i < this.records.length; i++) {
			Record record = this.records[i];
			final double translatedValue = analyzer.getActiveDevice().translateValue(record, record.getRealMaxValue() / 1000.);
			result = calculateAggregate(result, i, translatedValue);
		}
		return result;
	}

	/**
	 * @return the aggregated translated minimum value
	 */
	public double getRealMin() {
		double result = 0;
		for (int i = 0; i < this.records.length; i++) {
			Record record = this.records[i];
			final double translatedValue = analyzer.getActiveDevice().translateValue(record, record.getRealMinValue() / 1000.);
			result = calculateAggregate(result, i, translatedValue);
		}
		return result;
	}

	/**
	 * @param index
	 * @return the aggregated translated value at this real index position (irrespective of zoom / scope)
	 */
	public Double getReal(int index) {
		Double result = 0.;
		for (int i = 0; i < this.records.length; i++) {
			Record record = this.records[i];
			if (record.elementAt(index) == null) {
				result = null;
				break;
			} else {
				final double translatedValue = analyzer.getActiveDevice().translateValue(record, record.elementAt(index) / 1000.);
				result = calculateAggregate(result, i, translatedValue);
			}
		}
		return result;
	}

	/**
	 * @param tmpResult from the previous aggregation step
	 * @param aggregationStepIndex is the 0-based number of the current aggregation step
	 * @param translatedValue
	 * @return the recurrentResult aggregated based on the translated value
	 */
	private double calculateAggregate(double tmpResult, int aggregationStepIndex, double translatedValue) {
		double recurrentResult = tmpResult;
		if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.AVG) {
			recurrentResult += (translatedValue - recurrentResult) / (aggregationStepIndex + 1);
		} else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.MAX) {
			if (aggregationStepIndex != 0)
				recurrentResult = Math.max(recurrentResult, translatedValue);
			else
				recurrentResult = translatedValue;
		} else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.MIN) {
			if (aggregationStepIndex == 0)
				recurrentResult = translatedValue;
			else
				recurrentResult = Math.max(recurrentResult, translatedValue);
		} else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.PRODUCT) {
			if (aggregationStepIndex == 0)
				recurrentResult = translatedValue;
			else
				recurrentResult *= translatedValue;
		} else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.QUOTIENT) {
			if (aggregationStepIndex == 0)
				recurrentResult = translatedValue;
			else
				recurrentResult /= translatedValue;
		} else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.SPREAD) {
			if (aggregationStepIndex == 0)
				recurrentResult = translatedValue;
			else
				recurrentResult -= translatedValue;
		} else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.SUM) {
			recurrentResult += translatedValue;
		} else
			throw new UnsupportedOperationException();
		return recurrentResult;
	}

	/**
	 * @param fromIndex
	 * @param toIndex
	 * @return the portion of the timestamps_ms and aggregated translated values between fromIndex, inclusive, and toIndex, exclusive. (If
	 *         fromIndex and toIndex are equal, the returned List is empty.)
	 */
	public List<Spot<Double>> getSubPoints(int fromIndex, int toIndex) {
		int recordSize = toIndex - fromIndex;
		List<Spot<Double>> result = new ArrayList<>(recordSize);
		for (int i = fromIndex; i < toIndex; i++) {
			if (getReal(i) != null) result.add(new Spot<Double>(this.settlementRecord.getParent().getTime_ms(i), getReal(i)));
		}
		log.finer(() -> Arrays.toString(result.toArray()));
		return result;
	}

	public String getComment() {
		return this.referenceGroupType.getComment();
	}

	public int getSize() {
		return this.records.length;
	}

}
