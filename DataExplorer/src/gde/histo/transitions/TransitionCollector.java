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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import gde.data.RecordSet;
import gde.device.ChannelType;
import gde.device.TransitionClassTypes;
import gde.device.TransitionGroupType;
import gde.device.TransitionType;
import gde.histo.transitions.GroupTransitions.TransitionChronicle;
import gde.log.Logger;

/**
 * Collect the transitions from a recordset.
 * @author Thomas Eickert (USER)
 */
public final class TransitionCollector {
	private final static String	$CLASS_NAME	= TransitionCollector.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	private final RecordSet			recordSet;

	public TransitionCollector(RecordSet recordSet) {
		this.recordSet = recordSet;
	}

	/**
	 * Identify all transitions for the recordset and channel.
	 * Take all transition types defined for the channel.
	 * Remove transition duplicates or overlapping transitions in all transition groups.
	 * @return the multimap holding all transitions (key is thresholdStartTimestamp_ms) per transitionGroupId (key)
	 */
	public GroupTransitions defineTransitions(int logChannelNumber) {
		final GroupTransitions groupTransitions = new GroupTransitions(recordSet);

		final ChannelType channelType = recordSet.getDevice().getDeviceConfiguration().getChannel(logChannelNumber);
		for (TransitionType transitionType : channelType.getTransitions().values()) {
			TransitionChronicle transitionsFromRecord = findTransitions(recordSet, transitionType);
			if (!transitionsFromRecord.isEmpty()) {
				log.fine(() -> String.format("%d  transitionCount=%d", transitionType.getTransitionId(), 999)); //$NON-NLS-1$

				// assign the transitions to all transition groups which have a mapping to this transition type
				Iterable<TransitionGroupType> iterable = channelType.getTransitionGroups().values().stream().filter(group -> group.getTransitionMapping().stream().anyMatch(mapping -> mapping.getTransitionId() == transitionType.getTransitionId()))::iterator;
				for (TransitionGroupType transitionGroupType : iterable) {
					if (!groupTransitions.containsKey(transitionGroupType.getTransitionGroupId())) {
						// build the container
						groupTransitions.put(transitionGroupType.getTransitionGroupId(), new TransitionChronicle());
					}
					TransitionChronicle transitionChronicle = groupTransitions.get(transitionGroupType.getTransitionGroupId());

					// merge the new transitions with existing transitions for the current group and class
					transitionChronicle.putAll(getSuperiorTransitions(transitionsFromRecord, transitionChronicle));

					// eliminate duplicate transitions
					List<Long> duplicates = getDuplicates(transitionChronicle);
					for (long timeStamp_ms : duplicates) {
						transitionChronicle.remove(timeStamp_ms);
					}
					if (!duplicates.isEmpty()) {
						log.fine(() -> String.format("%d  removals due to general overlap:  duplicatesSize=%d", transitionType.getTransitionId(), duplicates.size())); //$NON-NLS-1$
					}
				}
			}
		}

		return groupTransitions;
	}

	/**
	 * @param recordSet
	 * @param transitionType
	 * @return the identified transitions with the key thresholdStartTimestamp_ms
	 */
	private TransitionChronicle findTransitions(RecordSet recordSet, TransitionType transitionType) {
		TransitionChronicle transitionsFromRecord;

		if (transitionType.getClassType() == TransitionClassTypes.PEAK) {
			PeakDetector histoTransitions = new PeakDetector(recordSet);
			transitionsFromRecord = histoTransitions.findTransitions(recordSet.get(recordSet.getRecordNames()[transitionType.getRefOrdinal()]), transitionType);
		} else if (transitionType.getClassType() == TransitionClassTypes.PULSE) {
			PulseDetector histoTransitions = new PulseDetector(recordSet);
			transitionsFromRecord = histoTransitions.findTransitions(recordSet.get(recordSet.getRecordNames()[transitionType.getRefOrdinal()]), transitionType);
		} else if (transitionType.getClassType() == TransitionClassTypes.SLOPE) {
			SlopeDetector histoTransitions = new SlopeDetector(recordSet);
			transitionsFromRecord = histoTransitions.findTransitions(recordSet.get(recordSet.getRecordNames()[transitionType.getRefOrdinal()]), transitionType);
		} else {
			throw new UnsupportedOperationException();
		}

		return transitionsFromRecord;
	}

	/**
	 * Prioritize shorter transitions because this will increase the probability for additional transitions.
	 * @param transitionChronicle
	 * @return transitions which overlap in the reference and threshold phases
	 */
	private List<Long> getDuplicates(TransitionChronicle transitionChronicle) {
		List<Long> duplicates;
		duplicates = new ArrayList<Long>();
		Entry<Long, Transition> previousTransitionEntry = null;
		for (Entry<Long, Transition> transitionEntry : transitionChronicle.entrySet()) {
			if (previousTransitionEntry != null) {
				Entry<Long, Transition> inferiorTransitionEntry = getInferiorTransition(previousTransitionEntry, transitionEntry).orElse(null);
				if (inferiorTransitionEntry == null)
					previousTransitionEntry = transitionEntry;
				else if (previousTransitionEntry.equals(inferiorTransitionEntry)) {
					duplicates.add(previousTransitionEntry.getKey());
					previousTransitionEntry = transitionEntry;
				} else {
					duplicates.add(transitionEntry.getKey());
					// keep previousTransitionEntry for the next overlap check
				}
			} else {
				previousTransitionEntry = transitionEntry;
			}
		}
		return duplicates;
	}

	/**
	 * @param newChronicle
	 * @param baseChronicle is reduced by inferior transitions compared to new chronicle
	 * @return the merged transitions with existing transitions for the current group and class
	 */
	private TransitionChronicle getSuperiorTransitions(TransitionChronicle newChronicle, TransitionChronicle baseChronicle) {
		TransitionChronicle newTransitions = new TransitionChronicle(newChronicle);
		if (!baseChronicle.isEmpty()) {
			// identify transitions with the same threshold startTimeStamp
			Set<Long> intersection = new HashSet<Long>(newTransitions.keySet()); // use the copy constructor because the keyset is only a view on the map
			intersection.retainAll(baseChronicle.keySet());
			// discard transitions with the same timestamp
			for (long thresholdStartTimeStamp_ms : intersection) {
				// check which one of the conflicting transitions is inferior and remove it from its parent map
				final Entry<Long, Transition> newTransition = newTransitions.ceilingEntry(thresholdStartTimeStamp_ms);
				getInferiorTransition(baseChronicle.ceilingEntry(thresholdStartTimeStamp_ms), newChronicle.ceilingEntry(thresholdStartTimeStamp_ms)).ifPresent(x -> {
					if (newTransition.equals(x))
						newTransitions.remove(thresholdStartTimeStamp_ms);
					else
						baseChronicle.remove(thresholdStartTimeStamp_ms); // todo removal should not be done in a method parameter object
				});
			}
			if (!intersection.isEmpty()) {
				log.fine(() -> String.format("removals due to same timestamp:  intersectionSize=%d", intersection.size())); //$NON-NLS-1$
			}
		}
		return newTransitions;
	}

	/**
	 * Prioritize transitions (peak is top priority, slope is last priority).
	 * The second-order criterion is the reference + threshold duration (shorter is higher priority).
	 * @param entry1
	 * @param entry2
	 * @return the inferior transition in case of overlapping transitions
	 */
	private Optional<Entry<Long, Transition>> getInferiorTransition(Entry<Long, Transition> entry1, Entry<Long, Transition> entry2) {
		Optional<Entry<Long, Transition>> inferiorTransition = Optional.empty();

		final Transition transition2 = entry2.getValue();
		final Transition transition1 = entry1.getValue();
		final boolean isOverlap = transition2.getReferenceStartTimeStamp_ms() < transition1.getThresholdEndTimeStamp_ms() && transition1.getReferenceStartTimeStamp_ms() < transition2.getThresholdEndTimeStamp_ms();
		// !start2.after(end1) && !start1.after(end2)
		final boolean isTransition1Prioritized = (transition1.isPeak() && !transition2.isPeak()) || (transition1.isPulse() && !transition2.isPeak() && !transition2.isPulse());
		final boolean isTransition2Prioritized = (transition2.isPeak() && !transition1.isPeak()) || (transition2.isPulse() && !transition1.isPeak() && !transition1.isPulse());

		if (isOverlap) {
			if (isTransition1Prioritized) {
				inferiorTransition = Optional.of(entry2);
			} else if (isTransition2Prioritized) {
				inferiorTransition = Optional.of(entry1);
			} else {
				final long transition1Duration = transition1.getThresholdEndTimeStamp_ms() - transition1.getReferenceStartTimeStamp_ms();
				final long transition2Duration = transition2.getThresholdEndTimeStamp_ms() - transition2.getReferenceStartTimeStamp_ms();
				if (transition1Duration < transition2Duration) {
					inferiorTransition = Optional.of(entry2);
				} else if (transition2Duration < transition1Duration) {
					inferiorTransition = Optional.of(entry1);
				} else {
					// no criterion found
					inferiorTransition = Optional.of(entry1);
				}
			}
		}

		inferiorTransition.ifPresent(x -> log.finer(() -> ("discarded due to " //
				+ (isTransition1Prioritized || isTransition2Prioritized ? "class:  " : "duration:  ") //
				+ x.getValue())));
		return inferiorTransition;
	}

}
