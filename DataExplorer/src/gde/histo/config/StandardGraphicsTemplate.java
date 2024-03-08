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

    Copyright (c) 2016,2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.histo.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import gde.Analyzer;
import gde.GDE;

/**
 * Standard template in the standard template path.
 * @see gde.histo.config.HistoGraphicsTemplate
 * @author Thomas Eickert
 */
public class StandardGraphicsTemplate extends HistoGraphicsTemplate {
	private static final long serialVersionUID = -2725393260740286884L;

	/**
	 * @see gde.histo.config.HistoGraphicsTemplate
	 */
	protected StandardGraphicsTemplate(Analyzer analyzer, boolean suppressNewFile) {
		super(analyzer, suppressNewFile);
	}

	@Override
	public Path getTargetFileSubPath() {
		String fileName = histoFileName == null || histoFileName.equals(GDE.STRING_EMPTY) ? defaultFileName : histoFileName;
		return Paths.get(fileName);
	}

}
