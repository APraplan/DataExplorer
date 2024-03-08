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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;

import gde.Analyzer;
import gde.GDE;
import gde.device.IChannelItem;
import gde.device.MeasurementType;
import gde.device.StatisticsType;
import gde.device.TrailDisplayType;
import gde.device.TrailTypes;
import gde.log.Level;

/**
 * Handle the trail type assignment to a trailRecord.
 * @author Thomas Eickert
 */
public final class MeasurementTrailSelector extends TrailSelector {

	private String	triggerScaleRawText	= GDE.STRING_EMPTY;
	private String	triggerScaleUnit		= GDE.STRING_EMPTY;

	public MeasurementTrailSelector(TrailRecord trailRecord) {
		super(trailRecord);
	}

	/**
	 * @param channelItem is a measurement / settlement / scoregroup in the device channel
	 * @param recordName is the name of the data record which might differ from the device channel item name (e.g. Jeti)
	 * @param smartStatistics true selects the smart trail types
	 */
	public MeasurementTrailSelector(Analyzer analyzer, IChannelItem channelItem, String recordName, boolean smartStatistics) {
		super(analyzer, channelItem, recordName, smartStatistics);
	}

	@Override
	protected void setApplicableTrails() {
		BitSet applicablePrimitiveTrails = getApplicablePrimitiveTrails();

		// build applicable trail type lists for display purposes
		this.applicableTrailsOrdinals = new ArrayList<Integer>();
		this.applicableTrailsTexts = new ArrayList<String>();
		applicablePrimitiveTrails.stream().forEach(i -> {
			this.applicableTrailsOrdinals.add(i);
			this.applicableTrailsTexts.add(TrailTypes.VALUES[i].getDisplayNameWithTriggerText(channelItem));
		});

		setApplicableSuiteTrails();
		log.finer(() -> recordName + " texts " + this.applicableTrailsTexts);
		log.finer(() -> recordName + " ordinals " + this.applicableTrailsOrdinals);
	}

	/**
	 * The index is the TrailType ordinal.
	 * @return a set holding true for trails which are visible for the user
	 */
	private BitSet getApplicablePrimitiveTrails() {
		Optional<TrailDisplayType> trailDisplay = channelItem.getTrailDisplay();
		if (trailDisplay.map(TrailDisplayType::getDefaultTrail).map(TrailTypes::isSuite).orElse(false)) throw new UnsupportedOperationException(
				"suite trail must not be a devices's channel item default");

		BitSet trails = new BitSet();
		// triggered values like count/sum are not contained
		TrailTypes.getPrimitives().stream() //
				.filter(t -> !t.isTriggered()).filter(t -> !TrailTypes.OPTIONAL_TRAILS.contains(t)) //
				.filter(t -> t.isSmartStatistics() == smartStatistics) //
				.map(TrailTypes::ordinal) //
				.forEach(idx -> trails.set(idx));

		trails.or(getApplicablePrimitiveTriggeredTrails());

		trailDisplay.ifPresent(d -> adaptTrailsToDisplayType(trails, d));

		// set at least one trail if no trail is applicable
		if (trails.isEmpty()) {
			trails.set(TrailTypes.getSubstitute(smartStatistics).ordinal());
		}
		log.finer(() -> recordName + " data " + trails.toString());
		return trails;
	}

	/**
	 * The index is the TrailType ordinal.
	 * @return a set holding true for legacy triggered trails in the device channel item
	 */
	private BitSet getApplicablePrimitiveTriggeredTrails() {
		BitSet trails = new BitSet();
		if (((MeasurementType) channelItem).getStatistics() != null) {
			if (!smartStatistics) {
				StatisticsType stats = ((MeasurementType) channelItem).getStatistics();
				if (stats.getSumByTriggerRefOrdinal() != null //
						&& stats.getSumTriggerText() != null && stats.getSumTriggerText().length() > 1) {
					trails.set(TrailTypes.REAL_SUM_TRIGGERED.ordinal());
				}
				if (stats.getRatioRefOrdinal() != null //
						&& stats.getRatioText() != null && stats.getRatioText().length() > 1) {
					trails.set(TrailTypes.REAL_MAX_RATIO_TRIGGERED.ordinal());
				}
				if (stats.getTrigger() != null //
						&& stats.getSumTriggerTimeText() != null && stats.getSumTriggerTimeText().length() > 1) {
					trails.set(TrailTypes.REAL_TIME_SUM_TRIGGERED.ordinal());
				}
				if (stats.isCountByTrigger() != null) {
					trails.set(TrailTypes.REAL_COUNT_TRIGGERED.ordinal());
				}
				// applicableTrails.set(TrailTypes.REAL_SUM.ordinal()) // in settlements only
			} else {
				// todo triggered trails for smart statistics
			}
		}
		return trails;
	}

	/**
	 * Select the trail record (or the suite if applicable) if the selection has changed.
	 * @param value position / index of the trail type in the current list of applicable trails
	 */
	@Override // reason is trigger texts
	public void setTrailTextSelectedIndex(int value) {
		this.trailTextSelectedIndex = value;
		setTriggerScaleTexts(getTrailType());
	}

	private TrailTypes getTrailType() {
		if (this.trailTextSelectedIndex < 0) {
			log.log(Level.SEVERE, "index not defined yet ", this.trailTextSelectedIndex);
			throw new UnsupportedOperationException();
		} else {
			return TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex));
		}
	}

	@Override
	public boolean isTrailSuite() {
		return getTrailType().isSuite();
	}

	@Override
	public boolean isRangePlotSuite() {
		return getTrailType().isRangePlot();
	}

	@Override
	public boolean isBoxPlotSuite() {
		return getTrailType().isBoxPlot();
	}

	@Override
	public boolean isOddRangeTrail() {
		return TrailTypes.ODD_RANGE_TRAILS.contains(getTrailType());
	}

	@Override
	public int getSuiteMasterIndex() {
		return getTrailType().getSuiteMasterIndex();
	}

	@Override
	public List<TrailTypes> getSuiteMembers() {
		return getTrailType().getSuiteMembers();
	}

	public boolean isTriggerTrail() {
		return getTrailType().isTriggered();
	}

	protected void setTriggerScaleTexts(TrailTypes trailType) {
		if (!smartStatistics || ((MeasurementType) channelItem).getStatistics() == null) {
			StatisticsType measurementStatistics = ((MeasurementType) channelItem).getStatistics();
			if (trailType == TrailTypes.REAL_SUM_TRIGGERED //
					&& measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1 && measurementStatistics.getSumByTriggerRefOrdinal() != null) {
				this.triggerScaleRawText = measurementStatistics.getSumTriggerText();
				this.triggerScaleUnit = channelItem.getUnit();
			} else if ((trailType == TrailTypes.REAL_MAX_RATIO_TRIGGERED) //
					&& measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
				this.triggerScaleRawText = measurementStatistics.getRatioText();
				this.triggerScaleUnit = GDE.STRING_SLASH + channelItem.getUnit();
			} else if (trailType == TrailTypes.REAL_TIME_SUM_TRIGGERED //
					&& measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null && measurementStatistics.getSumTriggerTimeText().length() > 1) {
				this.triggerScaleRawText = measurementStatistics.getSumTriggerTimeText();
				this.triggerScaleUnit = "sec";
			} else if (trailType == TrailTypes.REAL_COUNT_TRIGGERED //
					&& measurementStatistics.isCountByTrigger() != null) {
				this.triggerScaleRawText = measurementStatistics.getCountTriggerText();
				this.triggerScaleUnit = "1";
			}
		} else {
			this.triggerScaleRawText = GDE.STRING_EMPTY;
			this.triggerScaleUnit = GDE.STRING_EMPTY;
		}
	}

	/**
	 * Trigger values (e.g. text_trigger_motor_sum) require individual scale texts.
	 * @return the scale text prior to XML replacement
	 */
	public String getTriggerScaleRawText() {
		return this.triggerScaleRawText;
	}

	/**
	 * Trigger values (e.g. text_trigger_count) require individual scale units.
	 * @return the scale unit text or the denominator in case of a trigger ratio (prefixed by '/')
	 */
	public String getTriggerScaleUnit() {
		return this.triggerScaleUnit;
	}

}