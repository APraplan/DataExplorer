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

package gde.histo.ui;

import java.nio.file.Path;
import java.util.List;

import gde.Analyzer;
import gde.histo.datasources.DirectoryScanner;
import gde.histo.exclusions.ExclusionData;
import gde.ui.DataExplorer;

/**
 * Exclusion activity processing.
 * Use this for UI activities only because it relies on the active device.
 * @author Thomas Eickert (USER)
 */
public final class ExclusionActivity {

	/**
	 * Delete the exclusion file belonging to the primary directory.
	 * The exclusion information is deleted in any case, e.g. if the suppress mode is currently OFF.
	 */
	public static void clearExcludeLists() {
		if (!DataExplorer.getInstance().getHistoExplorer().isPresent()) throw new UnsupportedOperationException();

		ExclusionData exclusionData = new ExclusionData(DirectoryScanner.getActiveFolder4Ui(), Analyzer.getInstance().getDataAccess());
		exclusionData.delete();
	}

	/**
	 * @param filePath
	 * @param recordsetBaseName empty string sets ignore to the file in total
	 */
	public static void setExcludeRecordSet(Path filePath, String recordsetBaseName) {
		ExclusionData exclusionData = new ExclusionData(DirectoryScanner.getActiveFolder4Ui(), Analyzer.getInstance().getDataAccess());
		if (recordsetBaseName.isEmpty())
			exclusionData.setProperty((filePath.getFileName().toString()));
		else
			exclusionData.addToProperty(filePath.getFileName().toString(), recordsetBaseName);
		exclusionData.store();
	}

	/**
	 * Determine the exclusions which are active for the current channel.
	 * @return the exclusion information for the trusses excluded from the history
	 */
	public static String[] getExcludedTrusses() {
		ExclusionData exclusionData = new ExclusionData(DirectoryScanner.getActiveFolder4Ui(), Analyzer.getInstance().getDataAccess());
		List<Path> excludedPaths = DataExplorer.getInstance().getPresentHistoExplorer().getHistoSet().getExcludedPaths();
		return exclusionData.getExcludedTrusses(excludedPaths);
	}
}
