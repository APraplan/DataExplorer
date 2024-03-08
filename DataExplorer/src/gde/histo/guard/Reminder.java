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

    Copyright (c) 2018,2019 Thomas Eickert
****************************************************************************************/

package gde.histo.guard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gde.GDE;
import gde.messages.MessageIds;
import gde.messages.Messages;

/**
 * Reminder data related to a reminder type.
 * Supports an index list pointing to reminder record points.
 * Please note:</br>
 * Vaults may hold an additional set of exceptional outliers beyond the far outlier category.
 * Those outliers are not element of the recordset and of this outliers object.
 */
public final class Reminder {
	private final ReminderType	reminderType;
	private final double				farLimit;
	private final double				closeLimit;
	private final List<Double>	decodedValues	= new ArrayList<>();
	private final List<Integer>	indices				= new ArrayList<>();
	private final int						selectIndex;
	private final String				selectText;

	/**
	 * Type of the warning reminder.
	 */
	public enum ReminderType {
		NONE {
			@Override
			public String localizedText() {
				return GDE.STRING_EMPTY;
			}
		},
		WHISKER {
			@Override
			public String localizedText() {
				return Messages.getString(MessageIds.GDE_MSGT0910);
			}
		},
		CLOSE {
			@Override
			public String localizedText() {
				return Messages.getString(MessageIds.GDE_MSGT0905);
			}
		},
		FAR {
			@Override
			public String localizedText() {
				return Messages.getString(MessageIds.GDE_MSGT0904);
			}
		};

		/**
		 * Use this instead of values() to avoid repeatedly cloning actions.
		 */
		public static final ReminderType VALUES[] = values();

		public static ReminderType fromOrdinal(int ordinal) {
			return ReminderType.VALUES[ordinal];
		}

		public static ReminderType min(ReminderType a, ReminderType b) {
			if (a == ReminderType.NONE) return b;
			if (b == ReminderType.NONE) return a;
			return fromOrdinal(Math.min(a.ordinal(), b.ordinal()));
		}

		public static ReminderType max(ReminderType a, ReminderType b) {
			return fromOrdinal(Math.max(a.ordinal(), b.ordinal()));
		}

		public boolean isIncluded(int reminderLevel) {
			return reminderLevel >= this.ordinal();
		}

		public abstract String localizedText();
	}

	public Reminder(ReminderType reminderType, double farLimit, double closeLimit, int selectIndex, String selectText) {
		this.reminderType = reminderType;
		this.farLimit = farLimit;
		this.closeLimit = closeLimit;
		this.selectIndex = selectIndex;
		this.selectText = selectText;
	}

	public boolean add(double decodedValue, int index) {
		this.indices.add(index);
		return decodedValues.add(decodedValue);
	}

	public ReminderType getReminderType() {
		return this.reminderType;
	}

	public double getFarLimit() {
		return this.farLimit;
	}

	public double getCloseLimit() {
		return this.closeLimit;
	}

	public List<Double> getDecodedValues() {
		return this.decodedValues;
	}

	public List<Integer> getIndices() {
		return this.indices;
	}

	public int getSelectIndex() {
		return this.selectIndex;
	}

	public String getSelectText() {
		return this.selectText;
	}

	@Override
	public String toString() {
		String values = Arrays.toString(this.decodedValues.toArray(new Double[0]));
		return this.reminderType.toString() + " farLimit/closeLimit/values=" + this.farLimit + "/" + this.closeLimit + "/" + values;
	}
}