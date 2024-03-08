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

import static java.util.logging.Level.FINE;

import gde.Analyzer;
import gde.GDE;
import gde.data.Record;
import gde.device.AmountTypes;
import gde.device.IDevice;
import gde.device.TransitionAmountType;
import gde.histo.transitions.Transition;
import gde.log.Logger;

/**
 * Collect settlement data for the trail recordset and subordinate objects.
 * @author Thomas Eickert (USER)
 */
public final class AmountEvaluator {
	private final static String			$CLASS_NAME	= AmountEvaluator.class.getName();
	private final static Logger			log					= Logger.getLogger($CLASS_NAME);

	private final SettlementRecord	histoSettlement;
	private final Analyzer					analyzer;

	public AmountEvaluator(SettlementRecord newHistoSettlement, Analyzer analyzer) {
		this.histoSettlement = newHistoSettlement;
		this.analyzer = analyzer;
	}

	/**
	 * Walk forward from the time step when the trigger has fired and amount the extremum values for the calculation.
	 * Add single result value.
	 * @param transition
	 */
	public void addFromTransition(Transition transition) {
		IDevice device = analyzer.getActiveDevice();
		TransitionAmountType transitionAmount = this.histoSettlement.getSettlement().getEvaluation().getTransitionAmount();
		log.log(FINE, GDE.STRING_GREATER, transitionAmount);
		final Record record = this.histoSettlement.getParent().get(this.histoSettlement.getParent().getRecordNames()[transitionAmount.getRefOrdinal()]);
		log.fine(() -> record.getName() + " values   " + record.subList(transition.getReferenceStartIndex(), transition.getThresholdEndIndex() + 1)); //$NON-NLS-1$
		final int reverseTranslatedResult;
		if (transitionAmount.getAmountType() == AmountTypes.MIN) {
			double min = Double.MAX_VALUE;
			for (int j = transition.getThresholdStartIndex(); j < transition.getThresholdEndIndex() + 1; j++)
				if (record.elementAt(j) != null) min = Math.min(min, transitionAmount.isUnsigned()
						? Math.abs(device.translateValue(record, record.elementAt(j) / 1000.)) : device.translateValue(record, record.elementAt(j) / 1000.));
			reverseTranslatedResult = (int) (this.histoSettlement.reverseTranslateValue(min) * 1000.);
		} else if (transitionAmount.getAmountType() == AmountTypes.MAX) {
			double max = transitionAmount.isUnsigned() ? 0. : -Double.MAX_VALUE;
			for (int j = transition.getThresholdStartIndex(); j < transition.getThresholdEndIndex() + 1; j++)
				if (record.elementAt(j) != null) max = Math.max(max, transitionAmount.isUnsigned()
						? Math.abs(device.translateValue(record, record.elementAt(j) / 1000.)) : device.translateValue(record, record.elementAt(j) / 1000.));
			reverseTranslatedResult = (int) (this.histoSettlement.reverseTranslateValue(max) * 1000.);
		} else if (transitionAmount.getAmountType() == AmountTypes.AVG) {
			double avg = 0., value = 0.;
			int skipCount = 0;
			for (int j = transition.getThresholdStartIndex(); j < transition.getThresholdEndIndex() + 1; j++)
				if (record.elementAt(j) != null) {
					value = transitionAmount.isUnsigned() ? Math.abs(device.translateValue(record, record.elementAt(j) / 1000.))
							: device.translateValue(record, record.elementAt(j) / 1000.);
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.getThresholdStartIndex() - skipCount + 1);
				} else
					skipCount++;
			reverseTranslatedResult = (int) (this.histoSettlement.reverseTranslateValue(avg) * 1000.);
		} else if (transitionAmount.getAmountType() == AmountTypes.SIGMA) {
			// https://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods
			double avg = 0., q = 0., value = 0.;
			int skipCount = 0;
			for (int j = transition.getThresholdStartIndex(); j < transition.getThresholdEndIndex() + 1; j++)
				if (record.elementAt(j) != null) {
					value = transitionAmount.isUnsigned() ? Math.abs(device.translateValue(record, record.elementAt(j) / 1000.))
							: device.translateValue(record, record.elementAt(j) / 1000.);
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.getThresholdStartIndex() - skipCount + 1);
					q += deltaAvg * (value - avg);
					if (log.isLoggable(FINE)) log.log(FINE, String.format("value=%f  deltaAvg=%f  q=%f", value, deltaAvg, q)); //$NON-NLS-1$
				} else
					skipCount++;
			reverseTranslatedResult = (int) (this.histoSettlement.reverseTranslateValue(Math.sqrt(q / (transition.getThresholdSize() - skipCount - 1))) * 1000.);
		} else {
			reverseTranslatedResult = 0;
			throw new UnsupportedOperationException();
		}
		// add to settlement record
		this.histoSettlement.add(reverseTranslatedResult, transition);
		if (log.isLoggable(FINE)) log.log(FINE, String.format("%s: timeStamp_ms=%d  reverseTranslatedResult=%d  amountType=%s", //$NON-NLS-1$
				this.histoSettlement.getName(), (int) this.histoSettlement.getParent().getTime_ms(transition.getThresholdEndIndex() + 1), reverseTranslatedResult, transitionAmount.getAmountType()));
	}

}
