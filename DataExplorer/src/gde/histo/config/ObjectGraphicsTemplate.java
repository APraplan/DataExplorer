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

package gde.histo.config;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.InvalidPropertiesFormatException;

import gde.Analyzer;
import gde.GDE;

/**
 * Supports individual device channel templates for objects in sub directories.
 * @author Thomas Eickert (USER)
 */
public final class ObjectGraphicsTemplate extends HistoGraphicsTemplate {
	private static final long	serialVersionUID	= -4197176848694996415L;

	private final String			objectFolderName;

	/**
	 * Constructor using the application home path and the device signature as initialization parameter.
	 * @param suppressNewFile true suppresses the object template file creation
	 */
	protected ObjectGraphicsTemplate(Analyzer analyzer, String objectFolderName, boolean suppressNewFile) {
		super(analyzer, suppressNewFile);
		this.objectFolderName = objectFolderName;
	}

	@Override
	public Path getTargetFileSubPath() {
		String fileName = histoFileName == null || histoFileName.equals(GDE.STRING_EMPTY) ? defaultHistoFileName : histoFileName;
		return Paths.get(objectFolderName, fileName);
	}

	/**
	 * Load the properties from the object template file.
	 * Alternatively browse the default path for a valid file and
	 * copy this file in the object template directory (except the readonly attribute is set).
	 */
	@Override
	public void load() {
		try {
			if (!analyzer.getDataAccess().existsGraphicsTemplate(getTargetFileSubPath())) {
				super.load();
			} else {
				currentFilePathFragment = null;
				loadFromXml(getTargetFileSubPath());
				currentFilePathFragment = getTargetFileSubPath();
			}
			this.isAvailable = true;
		} catch (InvalidPropertiesFormatException e) {
			log.log(SEVERE, e.getMessage(), e);
		} catch (Exception e) {
			log.log(WARNING, e.getMessage());
		}
	}

	/**
	 * Store the properties to the object template file.
	 */
	@Override
	public void store() {
		String propertiesComment = "-- DataExplorer ObjectGraphicsTemplate " + objectFolderName + "/" + getTargetFileSubPath().getFileName().toString() + " " + ZonedDateTime.now().toInstant() + " -- " + commentSuffix;
		super.store(propertiesComment);
	}

}
