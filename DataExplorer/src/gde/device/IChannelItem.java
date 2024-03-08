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

package gde.device;

import java.util.List;
import java.util.Optional;

import gde.Analyzer;
import gde.data.Record.DataType;
import gde.histo.cache.HistoVault;
import gde.histo.recordings.TrailSelector;

/**
 *
 * @author Thomas Eickert (USER)
 */
public interface IChannelItem {

	String getChannelItemId();

	String getName();

	String getSymbol();

	String getUnit();

	boolean isActive();

	String getLabel();

	double getOffset();

	double getReduction();

	double getFactor();

	boolean isBits();

	boolean isTokens();

	int getSyncMasterRecordOrdinal();

	List<PropertyType> getProperty();

	Optional<TrailDisplayType> getTrailDisplay();

	PropertyType getProperty(String propertyKey);

	void setDataType(DataType dataType);

	DataType getDataType();

	/**
	 * Set the most specific datatype.
	 */
	default DataType getUnifiedDataType() {
		if (getDataType() == null || getDataType() == DataType.DEFAULT) {
			DataType guess = DataType.guess(getName());
			if (guess != null)
				return guess;
			else
				return DataType.DEFAULT;
		}
		return getDataType();
	}

	/**
	 * @param vault
	 * @param trailOrdinal is the requested trail ordinal number which may differ from the selected trail type (e.g. suites)
	 * @return the point value
	 */
	Integer getVaultPoint(HistoVault vault, int trailOrdinal);

	/**
	 * @param analyzer defines the the requested device, channel, object
	 * @param recordName
	 * @param smartStatistics true if quantiles are active
	 * @return the trail selector
	 */
	TrailSelector createTrailSelector(Analyzer analyzer, String recordName, boolean smartStatistics);
}
