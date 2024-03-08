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

import java.util.stream.DoubleStream;

import gde.data.Record.DataType;
import gde.device.IChannelItem;
import gde.device.SettlementType;
import gde.histo.cache.DataTypes;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.HistoVault;
import gde.histo.datasources.HistoSet;

/**
 * Trail records containing settlement values.
 * @author Thomas Eickert (USER)
 */
public final class SettlementTrail extends TrailRecord {
	private static final long serialVersionUID = 110124007964748556L;

	/**
	 * @param newOrdinal
	 * @param settlementType
	 * @param parent
	 * @param initialCapacity
	 */
	public SettlementTrail(int newOrdinal, IChannelItem settlementType, TrailRecordSet parent, int initialCapacity) {
		super(settlementType, newOrdinal, parent, initialCapacity);
		setTrailSelector();
	}

	@Override
	public void setTrailSelector() {
		this.trailSelector = new SettlementTrailSelector(this);
	}

	@Override
	protected boolean isAllowedBySetting() {
		return getParent().getAnalyzer().getSettings().isDisplaySettlements();
	}

	@Override
	public boolean isScaleVisible() {
		return getParent().getAnalyzer().getSettings().isDisplaySettlements() && super.isScaleVisible();
	}

	@Override
	public Integer getVaultPoint(ExtendedVault vault, int trailOrdinal) {
		return vault.getSettlementPoint(((SettlementType) this.channelItem).getSettlementId(), trailOrdinal);
	}

	@Override
	public boolean hasVaultOutliers(ExtendedVault vault) {
		return vault.hasSettlementOutliers(((SettlementType) this.channelItem).getSettlementId());
	}

	@Override
	public boolean hasVaultScraps(ExtendedVault vault) {
		return vault.hasSettlementScraps(((SettlementType) this.channelItem).getSettlementId());
	}

	@Override
	public DoubleStream getVaultOutliers(ExtendedVault vault) {
		return vault.getSettlementOutliers(((SettlementType) this.channelItem).getSettlementId()) //
				.mapToDouble(p -> HistoSet.decodeVaultValue(this.getChannelItem(), p / 1000.));
	}

	@Override
	public DoubleStream getVaultScraps(ExtendedVault vault) {
		return vault.getSettlementScraps(((SettlementType) this.channelItem).getSettlementId()) //
				.mapToDouble(p -> HistoSet.decodeVaultValue(this.getChannelItem(), p / 1000.));
	}

	@Override
	public DataType getVaultDataType(HistoVault vault) {
		DataTypes dataType = vault.getSettlementDataType(((SettlementType) this.channelItem).getSettlementId());
		return dataType != null ? DataTypes.toDataType(dataType) : null;
	}

}
