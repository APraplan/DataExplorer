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
import gde.device.MeasurementType;
import gde.histo.cache.DataTypes;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.HistoVault;
import gde.histo.datasources.HistoSet;

/**
 * Trail records containing measurement values.
 * @author Thomas Eickert (USER)
 */
public final class MeasurementTrail extends TrailRecord {
	private static final long serialVersionUID = 110124007964748556L;

	/**
	 * @param newOrdinal
	 * @param measurementType
	 * @param parent
	 * @param initialCapacity
	 */
	public MeasurementTrail(int newOrdinal, IChannelItem measurementType, TrailRecordSet parent, int initialCapacity) {
		super(measurementType, newOrdinal, parent, initialCapacity);
		setTrailSelector();
	}

	@Override
	public void setTrailSelector() {
		this.trailSelector = new MeasurementTrailSelector(this);
	}

	public MeasurementType getMeasurement() {
		return (MeasurementType) this.channelItem;
	}

	@Override
	protected boolean isAllowedBySetting() {
		return true;
	}

	@Override // for clarity only
	public boolean isScaleVisible() {
		return super.isScaleVisible();
	}

	@Override
	public Integer getVaultPoint(ExtendedVault vault, int trailOrdinal) {
		return vault.getMeasurementPoint(this.getOrdinal(), trailOrdinal);
	}

	@Override
	public boolean hasVaultOutliers(ExtendedVault vault) {
		return vault.hasMeasurementOutliers(this.getOrdinal());
	}

	@Override
	public boolean hasVaultScraps(ExtendedVault vault) {
		return vault.hasMeasurementScraps(this.getOrdinal());
	}

	@Override
	public DoubleStream getVaultOutliers(ExtendedVault vault) {
		return vault.getMeasurementOutliers(this.getOrdinal()) //
				.mapToDouble(p -> HistoSet.decodeVaultValue(this.getChannelItem(), p / 1000.));
	}

	@Override
	public DoubleStream getVaultScraps(ExtendedVault vault) {
		return vault.getMeasurementScraps(this.getOrdinal()) //
				.mapToDouble(p -> HistoSet.decodeVaultValue(this.getChannelItem(), p / 1000.));
	}

	@Override
	public DataType getVaultDataType(HistoVault vault) {
		DataTypes dataType = vault.getMeasurementDataType(this.getOrdinal());
		return dataType != null ? DataTypes.toDataType(dataType) : null;
	}

}
