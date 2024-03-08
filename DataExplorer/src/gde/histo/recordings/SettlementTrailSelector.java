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
import gde.device.IChannelItem;
import gde.device.SettlementType;
import gde.device.TrailDisplayType;
import gde.device.TrailTypes;
import gde.log.Level;

/**
 * Handle the trail type assignment to a trailRecord.
 * @author Thomas Eickert
 */
public final class SettlementTrailSelector extends TrailSelector {
	public SettlementTrailSelector(TrailRecord trailRecord) {
		super(trailRecord);
	}

	/**
	 * @param channelItem is a measurement / settlement / scoregroup in the device channel
	 * @param recordName is the name of the data record which might differ from the device channel item name (e.g. Jeti)
	 * @param smartStatistics true selects the smart trail types
	 */
	public SettlementTrailSelector(Analyzer analyzer, IChannelItem channelItem, String recordName, boolean smartStatistics) {
		super(analyzer, channelItem, recordName, smartStatistics);
	}

	@Override
	protected void setApplicableTrails() {
		if (channelItem.getTrailDisplay().map(TrailDisplayType::getDefaultTrail).map(TrailTypes::isSuite).orElse(false))
			throw new UnsupportedOperationException("suite trail must not be a device settlement default");
		BitSet applicablePrimitiveTrails = getApplicablePrimitiveTrails();

		// build applicable trail type lists for display purposes
		this.applicableTrailsOrdinals = new ArrayList<Integer>();
		this.applicableTrailsTexts = new ArrayList<String>();
		applicablePrimitiveTrails.stream().forEach(i -> {
			this.applicableTrailsOrdinals.add(i);
			this.applicableTrailsTexts.add(TrailTypes.VALUES[i].getDisplayName());
		});

		setApplicableSuiteTrails();
		log.finer(() -> recordName + " texts " + this.applicableTrailsTexts);
		log.finer(() -> recordName + " ordinals " + this.applicableTrailsOrdinals);
	}

	/**
	 * The index is the TrailType ordinal.
	 * @return a set holding true for trails which are visible for the user
	 */
	public BitSet getApplicablePrimitiveTrails() {
		Optional<TrailDisplayType> trailDisplay = channelItem.getTrailDisplay();
		if (((SettlementType) channelItem).getEvaluation().getTransitionAmount() != null) throw new UnsupportedOperationException(
				"TransitionAmount not implemented");

		BitSet trails = new BitSet();
		// triggered values like count/sum are not contained
		TrailTypes.getPrimitives().stream() //
				.filter(t -> !t.isTriggered()).filter(t -> !TrailTypes.OPTIONAL_TRAILS.contains(t)) //
				.filter(t -> t.isSmartStatistics() == smartStatistics) //
				.map(TrailTypes::ordinal) //
				.forEach(idx -> trails.set(idx));

		// do not add any triggered values

		trailDisplay.ifPresent(d -> adaptTrailsToDisplayType(trails, d));

		// set at least one trail if no trail is applicable
		if (trails.isEmpty()) {
			trails.set(TrailTypes.getSubstitute(smartStatistics).ordinal());
		}
		log.finer(() -> recordName + " data " + trails.toString());
		return trails;
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

}