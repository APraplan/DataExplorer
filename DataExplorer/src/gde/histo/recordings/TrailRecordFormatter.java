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

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Vector;
import java.util.stream.Collectors;

import gde.GDE;
import gde.device.TrailTypes;
import gde.histo.cache.ExtendedVault;
import gde.histo.datasources.HistoSet;
import gde.histo.guard.Reminder;
import gde.histo.guard.Reminder.ReminderType;
import gde.histo.ui.SummaryComposite.SummaryLayout;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.StringHelper;

/**
 * Output formatting based on trailRecord data.
 * @author Thomas Eickert (USER)
 */
public final class TrailRecordFormatter {
	// private final static String $CLASS_NAME = TrailRecordFormatter.class.getName();
	// private final static Logger log = Logger.getLogger($CLASS_NAME);

	private final int TRAIL_TEXT_MAX_LENGTH = 13;

	/**
	 * Performant replacement for String.format("%.8s", ss)
	 */
	private static String trunc(String ss, int maxLength) {
		return ss.substring(0, Math.min(maxLength, ss.length()));
	}

	/**
	 * Define category based on the magnitude and the delta span of the values.
	 */
	public static DecimalFormat getDecimalFormat(double[] valuePair) {
		return getDecimalFormat(valuePair[0], valuePair[1]);
	}

	/**
	 * Define category based on the magnitude and the delta span of the values.
	 */
	private static DecimalFormat getDecimalFormat(double value1, double value2) {
		DecimalFormat df = new DecimalFormat();
		double rangeValue = Math.abs(value1 - value2);
		if (Math.abs(value1) < 10 && Math.abs(value2) < 10) {
			if (rangeValue < 0.01)
				df.applyPattern("0.0000"); //$NON-NLS-1$
			else if (rangeValue < 0.1)
				df.applyPattern("0.000"); //$NON-NLS-1$
			else
				df.applyPattern("0.00"); //$NON-NLS-1$
		} else if (Math.abs(value1) < 100 && Math.abs(value2) < 100) {
			if (rangeValue < 0.1)
				df.applyPattern("0.000"); //$NON-NLS-1$
			else if (rangeValue < 1.)
				df.applyPattern("0.00"); //$NON-NLS-1$
			else
				df.applyPattern("0.0"); //$NON-NLS-1$
		} else if (Math.abs(value1) < 1000 && Math.abs(value2) < 1000) {
			if (rangeValue < 1.)
				df.applyPattern("0.00"); //$NON-NLS-1$
			else if (rangeValue < 10.)
				df.applyPattern("0.0"); //$NON-NLS-1$
			else
				df.applyPattern("0"); //$NON-NLS-1$
		} else if (Math.abs(value1) < 10000 && Math.abs(value2) < 10000) {
			if (rangeValue < 10.)
				df.applyPattern("0.0"); //$NON-NLS-1$
			else
				df.applyPattern("0"); //$NON-NLS-1$
		} else {
			df.applyPattern("0"); //$NON-NLS-1$
		}
		return df;
	}

	private final TrailRecord record;

	public TrailRecordFormatter(TrailRecord trailRecord) {
		this.record = trailRecord;
	}

	/**
	 * Define category based the number format or on the magnitude and the delta span of the scale values.
	 * @param newNumberFormat holds the number of decimal places or -1 for a range based formatting
	 */
	public DecimalFormat getDecimalFormat(int newNumberFormat) {
		DecimalFormat df = new DecimalFormat();
		switch (newNumberFormat) {
		case -1:
			df = getDecimalFormat(this.record.getMaxScaleValue(), this.record.getMinScaleValue());
			break;
		case 0:
			df.applyPattern("0"); //$NON-NLS-1$
			break;
		case 1:
			df.applyPattern("0.0"); //$NON-NLS-1$
			break;
		case 2:
		default:
			df.applyPattern("0.00"); //$NON-NLS-1$
			break;
		case 3:
			df.applyPattern("0.000"); //$NON-NLS-1$
			break;
		}
		return df;
	}

	/**
	 * @param finalValue is the value to be displayed (without applying a factor or GPS coordinates fraction correction)
	 * @return decimal formatted value
	 */
	public String getSummaryValue(double finalValue, DecimalFormat decimalFormat) {
		if (HistoSet.isGpsCoordinates(this.record)) {
			if (this.record.getUnit().endsWith("'"))
				return StringHelper.getFormatedWithMinutes("%2d %04.1f", finalValue);
			else
				return decimalFormat.format(finalValue);
		} else
			return decimalFormat.format(finalValue);
	}

	/**
	 * @param finalValue is the value to be displayed (without applying a factor or GPS coordinates fraction correction)
	 * @return decimal formatted value based on record's format
	 */
	public String getScaleValue(double finalValue) {
		if (HistoSet.isGpsCoordinates(this.record)) {
			if (this.record.getUnit().endsWith("'")) //$NON-NLS-1$
				return StringHelper.getFormatedWithMinutes("%2d %04.1f", finalValue); //$NON-NLS-1$
			else
				return this.record.getDecimalFormat().format(finalValue);
		} else
			return this.record.getDecimalFormat().format(finalValue);
	}

	/**
	 * @param index
	 * @param trailType
	 * @return the translated and formatted value also for GPS coordinates
	 */
	public String getTableSuiteValue(int index, TrailTypes trailType) {
		String cellValue;
		StringBuilder sb = new StringBuilder();
		sb.append(getTruncatedTableValue(trailType.getSuiteLowerIndex(), index, 8));
		String d = sb.length() > 3 ? Character.toString((char) 183) : GDE.STRING_BLANK_COLON_BLANK; // 183: middle dot
		sb.append(d).append(getTruncatedTableValue(trailType.getSuiteMasterIndex(), index, 8));
		sb.append(d).append(getTruncatedTableValue(trailType.getSuiteUpperIndex(), index, 8));
		cellValue = sb.toString();
		return cellValue;
	}

	/**
	 * @param suiteOrdinal
	 * @param index
	 * @return the translated and formatted and truncated value also for GPS coordinates
	 */
	private String getTruncatedTableValue(int suiteOrdinal, int index, int length) {
		return trunc(getTableValue(this.record.getSuiteRecords().get(suiteOrdinal).get(index) / 1000.), length);
	}

	/**
	 * Supports suites and null values.
	 * @param index
	 * @return the translated and decimal formatted value at the given index or a standard string in case of a null value
	 */
	public String getMeasureValue(int index) {
		final Vector<Integer> points = this.record.getPoints();

		if (points.elementAt(index) != null) {
			return getTableValue(points.elementAt(index) / 1000.);
		} else
			return GDE.STRING_STAR;
	}

	/**
	 * @param value is the untranslated value (<em>intValue / 1000.</em>)
	 * @return the formatted value also for GPS coordinates
	 */
	public String getTableValue(double value) {
		final String formattedValue;
		if (HistoSet.isGpsCoordinates(this.record)) {
			// if (this.getDataType() == DataType.GPS_LATITUDE etc ???
			if (this.record.getUnit().endsWith("'")) { //$NON-NLS-1$
				formattedValue = StringHelper.getFormatedWithMinutes("%2d %07.4f", HistoSet.decodeVaultValue(this.record.getChannelItem(), value)).trim(); //$NON-NLS-1$
			} else {
				formattedValue = String.format("%8.6f", HistoSet.decodeVaultValue(this.record.getChannelItem(), value)); //$NON-NLS-1$
			}
		} else {
			formattedValue = this.record.getDecimalFormat().format(HistoSet.decodeVaultValue(this.record.getChannelItem(), value));
		}
		return formattedValue;
	}

	/**
	 * @param index
	 * @return the formatted value also for GPS coordinates
	 */
	public String getTableValue(int index) {
		return getTableValue(this.record.elementAt(index) / 1000.);
	}

	public String defineFormattedMinWarning(SummaryLayout summary) {
		Reminder outliers = summary.getMinMaxWarning()[0];
		if (outliers == null) {
			return new String();
		} else {
			DecimalFormat df = summary.getDecimalFormat();
			if (outliers.getReminderType() == ReminderType.WHISKER) {
				String values = outliers.getDecodedValues().stream() //
						.map(v -> getSummaryValue(v, df)).collect(Collectors.joining(", "));
				String fileNames = outliers.getIndices().stream().map(record.getParent()::getVault) //
						.map(ExtendedVault::getLoadFileAsPath).map(Path::getFileName).map(Path::toString) //
						.distinct().collect(Collectors.joining(", "));
				String outputText = outliers.getSelectText().length() > TRAIL_TEXT_MAX_LENGTH
						? outliers.getSelectText().substring(0, TRAIL_TEXT_MAX_LENGTH - 1) + GDE.STRING_ELLIPSIS : outliers.getSelectText();
				return outputText + " !" //
						+ Messages.getString(MessageIds.GDE_MSGT0906) + getSummaryValue(outliers.getFarLimit(), df) + "/" + getSummaryValue(outliers.getCloseLimit(), df) //
						+ outliers.getReminderType().localizedText() + Messages.getString(MessageIds.GDE_MSGT0911) + values + "\n" + fileNames;
			} else {
				String values = outliers.getDecodedValues().stream() //
						.map(v -> getSummaryValue(v, df)).collect(Collectors.joining(", "));
				String fileNames = outliers.getIndices().stream().map(record.getParent()::getVault) //
						.map(ExtendedVault::getLoadFileAsPath).map(Path::getFileName).map(Path::toString) //
						.distinct().collect(Collectors.joining(", "));
				String outputText = outliers.getSelectText().length() > TRAIL_TEXT_MAX_LENGTH
						? outliers.getSelectText().substring(0, TRAIL_TEXT_MAX_LENGTH - 1) + GDE.STRING_ELLIPSIS : outliers.getSelectText();
				return outputText + " !" //
						+ Messages.getString(MessageIds.GDE_MSGT0906) + getSummaryValue(outliers.getFarLimit(), df) + "/" + getSummaryValue(outliers.getCloseLimit(), df) //
						+ outliers.getReminderType().localizedText() + Messages.getString(MessageIds.GDE_MSGT0907) + values + "\n" + fileNames;
			}
		}
	}

	public String defineFormattedMaxWarning(SummaryLayout summary) {
		Reminder outliers = summary.getMinMaxWarning()[1];
		if (outliers == null) {
			return new String();
		} else {
			DecimalFormat df = summary.getDecimalFormat();
			if (outliers.getReminderType() == ReminderType.WHISKER) {
				String values = outliers.getDecodedValues().stream() //
						.map(v -> getSummaryValue(v, df)).collect(Collectors.joining(", "));
				String fileNames = outliers.getIndices().stream().map(record.getParent()::getVault) //
						.map(ExtendedVault::getLoadFileAsPath).map(Path::getFileName).map(Path::toString) //
						.distinct().collect(Collectors.joining(", "));
				String outputText = outliers.getSelectText().length() > TRAIL_TEXT_MAX_LENGTH
						? outliers.getSelectText().substring(0, TRAIL_TEXT_MAX_LENGTH - 1) + GDE.STRING_ELLIPSIS : outliers.getSelectText();
				return outputText + " !" //
						+ Messages.getString(MessageIds.GDE_MSGT0906) + getSummaryValue(outliers.getCloseLimit(), df) + "/" + getSummaryValue(outliers.getFarLimit(), df) //
						+ outliers.getReminderType().localizedText() + Messages.getString(MessageIds.GDE_MSGT0911) + values + "\n" + fileNames;
			} else {
				String values = outliers.getDecodedValues().stream() //
						.map(v -> getSummaryValue(v, df)).collect(Collectors.joining(", "));
				String fileNames = outliers.getIndices().stream().map(record.getParent()::getVault) //
						.map(ExtendedVault::getLoadFileAsPath).map(Path::getFileName).map(Path::toString) //
						.distinct().collect(Collectors.joining(", "));
				String outputText = outliers.getSelectText().length() > TRAIL_TEXT_MAX_LENGTH
						? outliers.getSelectText().substring(0, TRAIL_TEXT_MAX_LENGTH - 1) + GDE.STRING_ELLIPSIS : outliers.getSelectText();
				return outputText + " !" //
						+ Messages.getString(MessageIds.GDE_MSGT0906) + getSummaryValue(outliers.getCloseLimit(), df) + "/" + getSummaryValue(outliers.getFarLimit(), df) //
						+ outliers.getReminderType().localizedText() + Messages.getString(MessageIds.GDE_MSGT0907) + values + "\n" + fileNames;
			}
		}
	}

	/**
	 * @return the text information about all warnings for the record
	 */
	public String defineMinMaxWarningText(SummaryLayout summary) {
		String textLine1 = "", textLine2 = "";
		final String fileNameInitializer = Messages.getString(MessageIds.GDE_MSGT0908);
		final String minMaxSeparator = "   >---<   ";
		String lineInitializer = record.getNameReplacement().length() > TRAIL_TEXT_MAX_LENGTH
				? record.getNameReplacement().substring(0, TRAIL_TEXT_MAX_LENGTH - 1) + GDE.STRING_ELLIPSIS + " > " : record.getNameReplacement() + " > ";
		if (summary.getMinMaxWarning()[0] != null) { // left scale warnings
			if (summary.getMinMaxWarning()[0].getReminderType() == ReminderType.FAR)
				textLine1 = lineInitializer + defineFormattedMinWarning(summary).replace("\n", fileNameInitializer);
			else
				textLine2 = lineInitializer + defineFormattedMinWarning(summary).replace("\n", fileNameInitializer);
		}
		if (summary.getMinMaxWarning()[1] != null) { // right
			lineInitializer = "                    " + lineInitializer;
			if (summary.getMinMaxWarning()[1].getReminderType() == ReminderType.FAR) {
				textLine1 = textLine1.isEmpty() ? lineInitializer + defineFormattedMaxWarning(summary).replace("\n", fileNameInitializer)
						: textLine1 + minMaxSeparator + defineFormattedMaxWarning(summary).replace("\n", fileNameInitializer);
			} else {
				textLine2 = textLine2.isEmpty() ? lineInitializer + defineFormattedMaxWarning(summary).replace("\n", fileNameInitializer)
						: textLine2 + minMaxSeparator + defineFormattedMaxWarning(summary).replace("\n", fileNameInitializer);
			}
		}
		return textLine1 + "\n" + textLine2;
	}

}
