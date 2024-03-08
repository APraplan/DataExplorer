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

import java.util.Arrays;
import java.util.stream.DoubleStream;

import gde.data.Record.DataType;
import gde.device.IChannelItem;
import gde.device.ScoreGroupType;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.HistoVault;
import gde.histo.guard.Guardian;

/**
 * Trail records containing score values.
 * @author Thomas Eickert (USER)
 */
public final class ScoregroupTrail extends TrailRecord {
	private static final long		serialVersionUID	= 110124007964748556L;

	/**
	 * @param newOrdinal
	 * @param scoreGroupType
	 * @param parent
	 * @param initialCapacity
	 */
	public ScoregroupTrail(int newOrdinal, IChannelItem scoreGroupType, TrailRecordSet parent, int initialCapacity) {
		super(scoreGroupType, newOrdinal, parent, initialCapacity);
		setTrailSelector();
	}

	@Override
	public void setTrailSelector() {
		this.trailSelector = new ScoregroupTrailSelector(this);
	}

	public ScoreGroupType getScoregroup() {
		return (ScoreGroupType) this.channelItem;
	}

	@Override
	protected boolean isAllowedBySetting() {
		return getParent().getAnalyzer().getSettings().isDisplayScores();
	}

	@Override
	public boolean isScaleVisible() {
		return getParent().getAnalyzer().getSettings().isDisplayScores() && super.isScaleVisible();
	}

	@Override
	public Integer getVaultPoint(ExtendedVault vault, int trailOrdinal) {
		if (channelItem.isBits()) {
			return Integer.bitCount(vault.getScorePoint(trailOrdinal));
		} else {
			return vault.getScorePoint(trailOrdinal);
		}
	}

	@Override
	public boolean hasReasonableData() {
		return true;
	}

	@Override
	public double[] defineRecentMinMax(int limit) {
		return Guardian.defineScoreMinMax(Arrays.stream(getParent().getIndexedVaults()).limit(limit), getScoregroup());
	}

	@Override
	public double[] defineExtrema() {
		return Guardian.defineScoreExtrema(Arrays.asList(getParent().getIndexedVaults()), getScoregroup(), getParent().getAnalyzer().getSettings());
	}

	@Override
	protected Integer[] getExtremumTrailPoints(ExtendedVault vault) {
		int[] extremumScoreOrdinals = this.trailSelector.getExtremumTrailsOrdinals();
		if (extremumScoreOrdinals[0] == extremumScoreOrdinals[1]) { // let's spare some processing time
			Integer vaultPoint = getVaultPoint(vault, extremumScoreOrdinals[0]);
			return new Integer[] { vaultPoint, vaultPoint };
		} else {
			return new Integer[] { getVaultPoint(vault, extremumScoreOrdinals[0]), getVaultPoint(vault, extremumScoreOrdinals[1]) };
		}
	}

	@Override
	public boolean hasVaultOutliers(ExtendedVault vault) {
		return false;
	}

	@Override
	public boolean hasVaultScraps(ExtendedVault vault) {
		return false;
	}

	@Override
	public DoubleStream getVaultScraps(ExtendedVault vault) {
		return DoubleStream.empty();
	}

	@Override
	public DoubleStream getVaultOutliers(ExtendedVault vault) {
		return DoubleStream.empty();
	}

	@Override
	public DataType getVaultDataType(HistoVault vault) {
		return DataType.DEFAULT;
	}

}
