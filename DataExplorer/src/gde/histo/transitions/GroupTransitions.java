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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import gde.data.RecordSet;

/**
 * Collect transitions per transition group.
 * Multimap holding all transitions (key is thresholdStartTimestamp_ms) per transitionGroupId (key).
 * @author Thomas Eickert (USER)
 */
public final class GroupTransitions {

	/**
	 * Timeline of identified transitions.
	 */
	public static final class TransitionChronicle {
		private final TreeMap<Long, Transition> transitionChronicle;

		public TransitionChronicle() {
			this.transitionChronicle = new TreeMap<>();
		}

		public TransitionChronicle(TransitionChronicle transitionChronicle) {
			this.transitionChronicle = new TreeMap<>(transitionChronicle.transitionChronicle);
		}

		public Transition get(long thresholdStartTimestamp_ms) {
			return transitionChronicle.get(thresholdStartTimestamp_ms);
		}

		public Transition put(long thresholdStartTimestamp_ms, Transition transition) {
			return transitionChronicle.put(thresholdStartTimestamp_ms, transition);
		}

		public boolean isEmpty() {
			return transitionChronicle.isEmpty();
		}

		public Collection<? extends Long> keySet() {
			return transitionChronicle.keySet();
		}

		public Entry<Long, Transition> ceilingEntry(long thresholdStartTimeStamp_ms) {
			return transitionChronicle.ceilingEntry(thresholdStartTimeStamp_ms);
		}

		public Transition remove(long thresholdStartTimeStamp_ms) {
			return transitionChronicle.remove(thresholdStartTimeStamp_ms);
		}

		public Set<Entry<Long, Transition>> entrySet() {
			return transitionChronicle.entrySet();
		}

		public Collection<Transition> values() {
			return transitionChronicle.values();
		}

		public void putAll(TransitionChronicle newTransitions) {
			transitionChronicle.putAll(newTransitions.transitionChronicle);
		}

		@Override
		public String toString() {
			return "TransitionChronicle [transitionChronicleSize=" + this.transitionChronicle.size() + ", transitionChronicle=" + this.transitionChronicle + "]";
		}
	}

	private final Map<Integer, TransitionChronicle>	groupTransitions	= new HashMap<>();

	private final RecordSet													recordSet;
	private final int																recordDataSize;

	public GroupTransitions(RecordSet recordSet) {
		this.recordSet = recordSet;
		this.recordDataSize = recordSet.getRecordDataSize(true);
	}

	public boolean isGatheringMode(RecordSet currentRecordSet) {
		if (!currentRecordSet.equals(this.recordSet)) throw new IllegalArgumentException();
		return this.recordDataSize != currentRecordSet.getRecordDataSize(true);
	}

	public boolean isEmpty() {
		return groupTransitions.isEmpty();
	}

	public TransitionChronicle get(int transitionGroupId) {
		return groupTransitions.get(transitionGroupId);
	}

	public TransitionChronicle put(int transitionGroupId, TransitionChronicle transitions) {
		return groupTransitions.put(transitionGroupId, transitions);
	}

	public boolean containsKey(int transitionGroupId) {
		return groupTransitions.containsKey(transitionGroupId);
	}
}
