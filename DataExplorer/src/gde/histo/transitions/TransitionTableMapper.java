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

package gde.histo.transitions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import gde.Analyzer;
import gde.data.RecordSet;
import gde.device.ChannelType;
import gde.device.SettlementType;
import gde.device.TransitionAmountType;
import gde.device.TransitionCalculusType;
import gde.device.TransitionFigureType;
import gde.device.TransitionGroupType;
import gde.histo.settlements.AmountEvaluator;
import gde.histo.settlements.CalculusEvaluator;
import gde.histo.settlements.FigureEvaluator;
import gde.histo.settlements.SettlementRecord;
import gde.histo.transitions.GroupTransitions.TransitionChronicle;
import gde.log.Logger;

/**
 * Transition data mapping for the table tab.
 * Support synchronization for ongoing data gatherings.
 * @author Thomas Eickert (USER)
 */
public final class TransitionTableMapper {
	private static final String	$CLASS_NAME	= TransitionTableMapper.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	public static final class SettlementTypes {
		private final LinkedHashMap<Integer, SettlementType> settlementTypes;

		public SettlementTypes(LinkedHashMap<Integer, SettlementType> settlements) {
			this.settlementTypes = new LinkedHashMap<>(settlements);
		}

		public SettlementType get(int settlementId) {
			return settlementTypes.get(settlementId);
		}

		public SettlementType put(int settlementId, SettlementType settlementType) {
			return settlementTypes.put(settlementId, settlementType);
		}

		public Set<Entry<Integer, SettlementType>> entrySet() {
			return settlementTypes.entrySet();
		}

		public int size() {
			return settlementTypes.size();
		}

		public Collection<SettlementType> values() {
			return settlementTypes.values();
		}

		@Override
		public String toString() {
			return "SettlementTypes [settlementTypes=" + this.settlementTypes + ", size()=" + this.size() + "]";
		}
	}

	public static final class SettlementRecords {
		private final LinkedHashMap<String, SettlementRecord> settlementRecords = new LinkedHashMap<>();

		public SettlementRecord get(String settlementName) {
			return settlementRecords.get(settlementName);
		}

		public SettlementRecord put(String settlementName, SettlementRecord settlementType) {
			return settlementRecords.put(settlementName, settlementType);
		}

		public Collection<SettlementRecord> values() {
			return settlementRecords.values();
		}

		public Set<Entry<String, SettlementRecord>> entrySet() {
			return settlementRecords.entrySet();
		}

		@Override
		public String toString() {
			return "SettlementRecords [settlementRecords=" + this.settlementRecords + ", size()=" + this.settlementRecords.size() + "]";
		}
	}

	private final RecordSet		recordSet;
	private final Analyzer		analyzer;
	private final ChannelType	channel;

	public TransitionTableMapper(RecordSet recordSet, Analyzer analyzer) {
		this.recordSet = recordSet;
		this.analyzer = analyzer;
		this.channel = analyzer.getActiveDevice().getDeviceConfiguration().getChannel(recordSet.getChannelConfigNumber());
	}

	/**
	 * @param index
	 * @param dataTableRow
	 * @return the row with additional columns for the active settlements with reasonable data
	 */
	public synchronized String[] defineRowWithSettlements(int index, String[] dataTableRow) {
		SettlementTypes settlementTypes = defineActiveAndDisplayableSettlements();
		HashMap<Integer, TransitionGroupType> transitionGroups = channel.getTransitionGroups();
		int tableColumnsSize = recordSet.getVisibleAndDisplayableRecordsForTable().size() + 1;
		String[] tableRow = Arrays.copyOf(dataTableRow, tableColumnsSize + settlementTypes.size() + transitionGroups.size());

		GroupTransitions histoTransitions = recordSet.getHistoTransitions();
		SettlementRecords settlements = determineSettlements(histoTransitions, settlementTypes.values());

		int columnIndex = tableColumnsSize;
		for (SettlementRecord settlementRecord : settlements.values()) {
			Double value = settlementRecord.getTranslatedValue(index);
			if (value != null) {
				tableRow[columnIndex] = String.format("%.3f", value);
			}
			columnIndex++;
		}

		for (Entry<Integer, TransitionGroupType> transitionsGroupsEntry : transitionGroups.entrySet()) {
			TransitionChronicle transitionChronicle = recordSet.getHistoTransitions().get(transitionsGroupsEntry.getKey());
			if (transitionChronicle != null) {
				Transition transition = transitionChronicle.get((long) recordSet.getTime_ms(index));
				if (transition != null) {
					tableRow[columnIndex] = Integer.toString(transition.getTransitionType().getTransitionId());
				}
			}
			columnIndex++;
		}
		return tableRow;
	}

	/**
	 * @return the settlementTypes with reasonable data (key is settlementId)
	 */
	public SettlementTypes defineActiveAndDisplayableSettlements() {
		SettlementTypes channelSettlements = new SettlementTypes(channel.getSettlements());
		for (Iterator<Entry<Integer, SettlementType>> iterator = channelSettlements.entrySet().iterator(); iterator.hasNext();) {
			Entry<Integer, SettlementType> entry = iterator.next();
			if (entry.getValue().isActive() && hasTransitions(entry.getValue())) {

			} else {
				iterator.remove();
			}
		}
		log.finer(() -> "settlementTypes.size=" + channel.getSettlements().size() + " size=" + channelSettlements.size());
		return channelSettlements;
	}

	private boolean hasTransitions(SettlementType settlementType) {
		GroupTransitions histoTransitions = recordSet.getHistoTransitions();
		if (histoTransitions.isEmpty()) {
			return false;
		} else {
			TransitionFigureType transitionFigureType = settlementType.getEvaluation().getTransitionFigure();
			TransitionAmountType transitionAmountType = settlementType.getEvaluation().getTransitionAmount();
			TransitionCalculusType calculationType = settlementType.getEvaluation().getTransitionCalculus();
			final int transitionGroupId;
			if (transitionFigureType != null) {
				transitionGroupId = transitionFigureType.getTransitionGroupId();
			} else if (transitionAmountType != null) {
				transitionGroupId = transitionAmountType.getTransitionGroupId();
			} else if (calculationType != null) {
				transitionGroupId = calculationType.getTransitionGroupId();
			} else {
				throw new UnsupportedOperationException();
			}
			TransitionChronicle transitionChronicle = histoTransitions.get(transitionGroupId);
			if (transitionChronicle != null) {
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * @return the calculated settlements calculated from transitions (key is the name of the settlementType)
	 */
	private SettlementRecords determineSettlements(GroupTransitions transitions, Collection<SettlementType> settlementTypes) {
		SettlementRecords histoSettlements = new SettlementRecords();
		for (SettlementType settlementType : settlementTypes) {
			if (settlementType.getEvaluation() != null) {
				SettlementRecord record = new SettlementRecord(settlementType, recordSet, analyzer.getActiveChannel().getNumber());
				histoSettlements.put(settlementType.getName(), record);
				if (transitions.isEmpty()) continue;

				TransitionFigureType transitionFigureType = settlementType.getEvaluation().getTransitionFigure();
				TransitionAmountType transitionAmountType = settlementType.getEvaluation().getTransitionAmount();
				TransitionCalculusType calculationType = settlementType.getEvaluation().getTransitionCalculus();
				if (transitionFigureType != null) {
					FigureEvaluator evaluator = new FigureEvaluator(record);
					for (Transition transition : transitions.get(transitionFigureType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
				} else if (transitionAmountType != null) {
					AmountEvaluator evaluator = new AmountEvaluator(record, analyzer);
					for (Transition transition : transitions.get(transitionAmountType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
				} else if (calculationType != null) {
					CalculusEvaluator evaluator = new CalculusEvaluator(record, analyzer);
					for (Transition transition : transitions.get(calculationType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
				} else {
					throw new UnsupportedOperationException();
				}
			}
		}
		return histoSettlements;
	}

}
