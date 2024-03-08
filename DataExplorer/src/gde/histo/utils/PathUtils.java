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

package gde.histo.utils;

import java.nio.file.Path;

import gde.GDE;

/**
 * Absolute and relative paths, file and folder names, extensions.
 * @author Thomas Eickert (USER)
 */
public class PathUtils {

	/**
	 * @return the lower case extension including the dot
	 */
	public static String getFileExtention(Path path) {
		if (path.getNameCount() == 0) {
			return GDE.STRING_EMPTY;
		} else {
			return getFileExtention(path.getFileName().toString());
		}
	}

	/**
	 * @return the lower case extension including the dot
	 */
	public static String getFileExtention(String fileName) {
		int index = fileName.lastIndexOf('.');
		String extention;
		if (index < 0) {
			extention = GDE.STRING_EMPTY;
		} else {
			extention = fileName.toString().substring(index).toLowerCase();
			if (extention.equals(fileName)) extention = GDE.STRING_EMPTY;
		}
		return extention;
	}

}
