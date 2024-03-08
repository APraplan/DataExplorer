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
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.stream.Collectors;

import gde.GDE;
import gde.device.resource.DeviceXmlResource;
import gde.histo.recordings.TrailDataTags.DataTag;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.LocalizedDateTime;
import gde.utils.LocalizedDateTime.DateTimePattern;
import gde.utils.StringHelper;

/**
 * Formatting methods.
 * @author Thomas Eickert (USER)
 */
public final class TrailRecordSetFormatter {
	private static final int			MAX_TOOLTIP_LINES	= 33;

	public static String getSelectedMeasurementsAsTable(long timestamp_ms) {
		TrailRecordSet trailRecordSet = DataExplorer.getInstance().getPresentHistoExplorer().getTrailRecordSet();
		if (trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
			Properties displayProps = trailRecordSet.getAnalyzer().getSettings().getMeasurementDisplayProperties();
			Vector<TrailRecord> records = trailRecordSet.getVisibleAndDisplayableRecords();

			StringBuilder sb = new StringBuilder().append(String.format("%-11.11s", Messages.getString(MessageIds.GDE_MSGT0799))); //$NON-NLS-1$
			sb.append(GDE.STRING_OR).append(String.format("%-16s", Messages.getString(MessageIds.GDE_MSGT0652))); //$NON-NLS-1$
			DeviceXmlResource xmlResource = DeviceXmlResource.getInstance();
			for (int i = 0; i < records.size(); i++) {
				TrailRecord record = records.get(i);
				if (displayProps.getProperty(record.getName()) != null)
					sb.append(GDE.STRING_OR).append(String.format("%-10s", displayProps.getProperty(record.getName()))); //$NON-NLS-1$
				else {
					String unit = GDE.STRING_LEFT_BRACKET + record.getUnit() + GDE.STRING_RIGHT_BRACKET;
					String replacedName = xmlResource.getReplacement(record.getName());
					String name = replacedName.substring(0, replacedName.length() >= 10 - unit.length() ? 10 - unit.length() : replacedName.length());
					String format = "%-" + (10 - unit.length()) + "s%" + unit.length() + "s"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					sb.append(GDE.STRING_OR).append(String.format(format, name, unit));
				}
			}
			sb.append(GDE.STRING_OR).append(GDE.LINE_SEPARATOR);

			final int index = trailRecordSet.getIndex(timestamp_ms);
			sb.append(String.format("%-11.11s", trailRecordSet.getDataTagText(index, DataTag.RECORDSET_BASE_NAME)));
			sb.append(GDE.STRING_OR).append(String.format("%-16s", LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmm, timestamp_ms)).substring(0, 16)); //$NON-NLS-1$
			for (int i = 0; i < records.size(); i++) {
				TrailRecord record = records.get(i);
				sb.append(GDE.STRING_OR).append(String.format("%.10s", StringHelper.center(new TrailRecordFormatter(record).getMeasureValue(index), 10))); //$NON-NLS-1$
			}
			return sb.append(GDE.STRING_OR).toString();
		} else {
			return GDE.STRING_EMPTY;
		}
	}

	public static String getFileNameLines(List<Integer> indices) {
		TrailRecordSet trailRecordSet = DataExplorer.getInstance().getPresentHistoExplorer().getTrailRecordSet();
		if (trailRecordSet != null) {
			String fileNameLines = indices.stream().map(i -> trailRecordSet.getDataTagText(i, DataTag.FILE_PATH)) //
					.map(Paths::get).map(Path::getFileName).map(Path::toString) //
					.sorted(Comparator.reverseOrder()).limit(MAX_TOOLTIP_LINES).collect(Collectors.joining(GDE.STRING_NEW_LINE));
			String suffix = indices.size() > MAX_TOOLTIP_LINES ? GDE.STRING_NEW_LINE + GDE.STRING_ELLIPSIS : "";
			return fileNameLines + suffix;
		} else {
			return "";
		}
	}

}
