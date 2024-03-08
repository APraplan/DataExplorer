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
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import gde.Analyzer;
import gde.GDE;
import gde.device.IChannelItem;
import gde.device.TrailDisplayType;
import gde.device.TrailTypes;
import gde.device.TrailVisibilityType;
import gde.device.resource.DeviceXmlResource;
import gde.log.Logger;

/**
 * Handle the trail type assignment to a trailRecord.
 * @author Thomas Eickert (USER)
 */
public abstract class TrailSelector {
	private static final String				$CLASS_NAME							= TrailSelector.class.getName();
	protected static final Logger			log											= Logger.getLogger($CLASS_NAME);

	protected final DeviceXmlResource	xmlResource							= DeviceXmlResource.getInstance();

	protected final String						deviceName;
	protected final IChannelItem			channelItem;
	protected final String						recordName;
	protected final boolean						smartStatistics;
	protected final int								channelConfigNumber;
	protected final Analyzer					analyzer;

	/**
	 * User selection from applicable trails, is saved in the graphics template
	 */
	protected int											trailTextSelectedIndex	= -1;
	/**
	 * The user may select one of these entries
	 */
	protected List<String>						applicableTrailsTexts;
	/**
	 * Maps all applicable trails in order to convert the user selection into a valid trail
	 */
	protected List<Integer>						applicableTrailsOrdinals;
	/**
	 * The selectIndex numbers which hold the extremum values or alternatively the first trail in the channel item
	 */
	protected int[]										extremumIndices					= null;

	protected TrailSelector(TrailRecord trailRecord) {
		this.deviceName = trailRecord.getParent().getDevice().getName();
		this.channelItem = trailRecord.channelItem;
		this.recordName = trailRecord.getName();
		this.smartStatistics = trailRecord.getParent().isSmartStatistics();
		this.channelConfigNumber = trailRecord.getParent().getChannelConfigNumber();
		this.analyzer = trailRecord.getParent().getAnalyzer();
		if (!this.deviceName.equals(analyzer.getActiveDevice().getName())) throw new IllegalArgumentException(
				"deviceName=" + this.deviceName + " != " + analyzer.getActiveDevice().getName()); // todo remove redundancy
		if (this.channelConfigNumber != analyzer.getActiveChannel().getNumber()) throw new IllegalArgumentException(
				"channelNumber=" + this.channelConfigNumber + " != " + analyzer.getActiveChannel().getNumber());
		setApplicableTrails();
	}

	/**
	 * @param recordName is the name of the data record which might differ from the device channel item name (e.g. Jeti)
	 * @param smartStatistics true selects the smart trail types
	 */
	protected TrailSelector(Analyzer analyzer, IChannelItem channelItem, String recordName, boolean smartStatistics) {
		this.deviceName = analyzer.getActiveDevice().getName();
		this.channelItem = channelItem;
		this.recordName = recordName;
		this.smartStatistics = smartStatistics;
		this.channelConfigNumber = analyzer.getActiveChannel().getNumber();
		this.analyzer = analyzer;
		setApplicableTrails();
	}

	/**
	 * Select the most prioritized trail from the applicable trails.
	 */
	public void setMostApplicableTrailTextOrdinal() {
		int displaySequence = Integer.MAX_VALUE;
		for (int i = 0; i < applicableTrailsOrdinals.size(); i++) {
			int tmpDisplaySequence = (TrailTypes.fromOrdinal(applicableTrailsOrdinals.get(i))).getDisplaySequence();
			if (tmpDisplaySequence < displaySequence) {
				displaySequence = tmpDisplaySequence;
				setTrailTextSelectedIndex(i);
			}
		}
	}

	public Integer getTrailTextSelectedIndex() {
		return this.trailTextSelectedIndex;
	}

	/**
	 * Select the trail record (or the suite if applicable) if the selection has changed.
	 * @param value position / index of the trail type in the current list of applicable trails
	 */
	public void setTrailTextSelectedIndex(int value) {
		this.trailTextSelectedIndex = value;
	}

	/**
	 * @return display text for the trail (may have been modified due to special texts for triggers)
	 */
	public String getTrailText() {
		return applicableTrailsTexts.size() == 0 ? GDE.STRING_EMPTY : applicableTrailsTexts.get(trailTextSelectedIndex);
	}

	public List<String> getApplicableTrailsTexts() {
		return this.applicableTrailsTexts;
	}

	public int getTrailOrdinal() {
		return this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex);
	}

	public abstract boolean isTrailSuite();

	public abstract boolean isRangePlotSuite();

	public abstract boolean isBoxPlotSuite();

	/**
	 * @return true if an own scale max/min is defined (do not use the value's / synchronized value's graphics scale)
	 */
	public abstract boolean isOddRangeTrail();

	public abstract int getSuiteMasterIndex();

	/**
	 * @return the members of the suite in the logical order
	 */
	public abstract List<TrailTypes> getSuiteMembers();

	/**
	 * @param replacementKey
	 * @return the replacement name of the specified key or an empty string if there is no key entry
	 */
	protected String getDeviceXmlReplacement(String replacementKey) {
		return replacementKey != null ? this.xmlResource.getReplacement(replacementKey) : GDE.STRING_EMPTY;
	}

	/**
	 * @return the ordinal numbers of the trail types resp. score labels which hold the extremum values after outlier elimination
	 */
	public int[] getExtremumTrailsOrdinals() {
		if (extremumIndices == null) setExtremumIndices();
		return new int[] { applicableTrailsOrdinals.get(extremumIndices[0]), applicableTrailsOrdinals.get(extremumIndices[1]) };
	}

	/**
	 * @return the trail texts resp. score label texts which hold the extremum values after outlier elimination
	 */
	public String[] getExtremumTrailsTexts() {
		if (extremumIndices == null) setExtremumIndices();
		return new String[] { applicableTrailsTexts.get(extremumIndices[0]), applicableTrailsTexts.get(extremumIndices[1]) };
	}

	/**
	 * @return the select index numbers of the trail texts which hold the extremum values after outlier elimination
	 */
	public int[] getExtremumTrailsIndices() {
		if (extremumIndices == null) setExtremumIndices();
		return extremumIndices;
	}

	/**
	 * Take Q0/Q4 if available to support outlier elimination.
	 */
	protected void setExtremumIndices() {
		int indexMin = -1, indexMax = -1;
		for (int i = 0; i < applicableTrailsOrdinals.size(); i++) {
			if (applicableTrailsOrdinals.get(i) == TrailTypes.Q0.ordinal()) {
				indexMin = i;
			} else if (applicableTrailsOrdinals.get(i) == TrailTypes.Q4.ordinal()) {
				indexMax = i;
			}
		}
		if (indexMin == -1 || indexMax == -1) {
			for (int i = 0; i < applicableTrailsOrdinals.size(); i++) {
				if (applicableTrailsOrdinals.get(i) == TrailTypes.MIN.ordinal()) {
					indexMin = i;
				} else if (applicableTrailsOrdinals.get(i) == TrailTypes.MAX.ordinal()) {
					indexMax = i;
				}
			}
		}
		if (indexMin == -1 || indexMax == -1) {
			extremumIndices = new int[] { 0, 0 };
		} else {
			extremumIndices = new int[] { indexMin, indexMax };
		}
	}

	protected abstract void setApplicableTrails();

	protected void setApplicableSuiteTrails() {
		Optional<TrailDisplayType> trailDisplay = channelItem.getTrailDisplay();
		if (trailDisplay.isPresent()) {
			final List<TrailTypes> displayTrails;
			boolean hideAllTrails = trailDisplay.map(TrailDisplayType::isDiscloseAll).orElse(false);
			if (hideAllTrails) {
				displayTrails = trailDisplay.map(x -> x.getExposed().stream().map(TrailVisibilityType::getTrail) //
						.filter(TrailTypes::isSuite).filter(t -> t.isSmartStatistics() == smartStatistics) //
						.collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
			} else {
				List<TrailTypes> disclosedTrails = trailDisplay.map(x -> x.getDisclosed().stream().map(TrailVisibilityType::getTrail) //
						.filter(TrailTypes::isSuite).filter(t -> t.isSmartStatistics() == smartStatistics) //
						.collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
				displayTrails = TrailTypes.getSuites().stream() //
						.filter(t -> !disclosedTrails.contains(t)).filter(t -> t.isSmartStatistics() == smartStatistics) //
						.collect(Collectors.toList());
			}
			for (TrailTypes suiteTrailType : displayTrails) {
				applicableTrailsOrdinals.add(suiteTrailType.ordinal());
				applicableTrailsTexts.add(suiteTrailType.getDisplayName().intern());
			}
		} else {
			for (TrailTypes suiteTrailType : TrailTypes.getSuites()) {
				if (suiteTrailType.isSmartStatistics() == smartStatistics) {
					applicableTrailsOrdinals.add(suiteTrailType.ordinal());
					applicableTrailsTexts.add(suiteTrailType.getDisplayName().intern());
				}
			}
		}
	}

	protected void adaptTrailsToDisplayType(BitSet trails, TrailDisplayType displayType) {
		if (displayType.isDiscloseAll()) {
			trails.clear();
		}

		BitSet exposedTrails = new BitSet();
		displayType.getExposed().stream().map(TrailVisibilityType::getTrail) //
				.filter(t -> !t.isSuite()).filter(t -> t.isSmartStatistics() == smartStatistics) //
				.map(TrailTypes::ordinal) //
				.forEach(idx -> exposedTrails.set(idx));
		trails.or(exposedTrails);

		BitSet disclosedTrails = new BitSet();
		displayType.getDisclosed().stream().map(TrailVisibilityType::getTrail) //
				.filter(t -> !t.isSuite()).filter(t -> t.isSmartStatistics() == smartStatistics) //
				.map(TrailTypes::ordinal) //
				.forEach(idx -> disclosedTrails.set(idx));
		trails.andNot(disclosedTrails);

		if (trails.isEmpty() && displayType.getDefaultTrail() != null) {
			trails.set(displayType.getDefaultTrail().ordinal());
		}
}

	@Override
	public String toString() {
		return "TrailSelector [" + recordName + " trailTextSelectedIndex=" + this.trailTextSelectedIndex + " " + this.getTrailText() + ", applicableTrailsTexts=" + this.applicableTrailsTexts + ", applicableTrailsOrdinals=" + this.applicableTrailsOrdinals + ", extremumIndices=" + Arrays.toString(this.extremumIndices) + "]";
	}

}
