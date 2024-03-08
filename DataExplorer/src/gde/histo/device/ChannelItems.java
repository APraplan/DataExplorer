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

    Copyright (c) 2018 Thomas Eickert
****************************************************************************************/

package gde.histo.device;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;

import gde.Analyzer;
import gde.device.ChannelType;
import gde.device.DeviceConfiguration;
import gde.device.IChannelItem;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.ScoreGroupType;
import gde.device.SettlementType;

/**
 *
 * @author Thomas Eickert (USER)
 */
public final class ChannelItems {

	private final DeviceConfiguration	deviceConfiguration;
	private final ChannelType					channelType;

	public ChannelItems(Analyzer analyzer) {
		this.deviceConfiguration = analyzer.getActiveDevice().getDeviceConfiguration();
		this.channelType = deviceConfiguration.getChannel(analyzer.getActiveChannel().getNumber());
	}

	/**
	 * Perform the actions for all the channel {@code IChannelItem}s in the display sequence.
	 * Use the {@code Integer} number as 0-based channel item ordinal.
	 */
	public void processItems(BiConsumer<Integer, IChannelItem> measurementAction, BiConsumer<Integer, IChannelItem> settlementAction,
			BiConsumer<Integer, IChannelItem> scoreGroupAction) {
		List<MeasurementType> channelMeasurements = channelType.getMeasurement();
		LinkedHashMap<Integer, SettlementType> channelSettlements = channelType.getSettlements();
		LinkedHashMap<Integer, ScoreGroupType> channelScoreGroups = channelType.getScoreGroups();

		{// section 0: look for scores at the top - scores' ordinals start after measurements + settlements due to GraphicsTemplate compatibility
			int myIndex = channelMeasurements.size() + channelSettlements.size();
			for (IChannelItem scoreGroup : channelScoreGroups.values()) {
				PropertyType topPlacementProperty = scoreGroup.getProperty("histo_top_placement");
				if (topPlacementProperty != null ? Boolean.parseBoolean(topPlacementProperty.getValue()) : false) {
					scoreGroupAction.accept(myIndex, scoreGroup);
				}
				myIndex++;
			}
		}
		{// section 1: look for settlements at the top - settlements' ordinals start after measurements due to GraphicsTemplate compatibility
			int myIndex = channelMeasurements.size(); // myIndex is used as recordOrdinal
			for (SettlementType settlement : channelSettlements.values()) {
				PropertyType topPlacementProperty = settlement.getProperty("histo_top_placement");
				if (topPlacementProperty != null ? Boolean.parseBoolean(topPlacementProperty.getValue()) : false) {
					settlementAction.accept(myIndex, settlement);
				}
				myIndex++;
			}
		}
		{// section 2: all measurements
			for (int i = 0; i < channelMeasurements.size(); i++) {
				MeasurementType measurement = channelMeasurements.get(i);
				measurementAction.accept(i, measurement);
			}
		}
		{// section 3: take remaining settlements
			int myIndex = channelMeasurements.size();
			for (SettlementType settlement : channelSettlements.values()) {
				PropertyType topPlacementProperty = settlement.getProperty("histo_top_placement");
				if (!(topPlacementProperty != null ? Boolean.parseBoolean(topPlacementProperty.getValue()) : false)) {
					settlementAction.accept(myIndex, settlement);
				}
				myIndex++; //
			}
		}
		{// section 4: take remaining scores
			int myIndex = channelMeasurements.size() + channelSettlements.size();
			for (ScoreGroupType scoreGroup : channelScoreGroups.values()) {
				PropertyType topPlacementProperty = scoreGroup.getProperty("histo_top_placement");
				if (!(topPlacementProperty != null ? Boolean.parseBoolean(topPlacementProperty.getValue()) : false)) {
					scoreGroupAction.accept(myIndex, scoreGroup);
				}
				myIndex++; //
			}
		}

	}
}
