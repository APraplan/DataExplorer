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

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.logging.Logger;

import gde.GDE;
import gde.log.Level;

/**
 * CSlass managing graphics visualization, store, restore
 * @author Winfried Brügmann
 */
public class GraphicsTemplate extends Properties {
	static final long			serialVersionUID	= 260357;
	static final Logger		log								= Logger.getLogger(GraphicsTemplate.class.getName());

	boolean				isAvailable				= false;
	boolean				isSaved						= false;																				// indicates if template is saved to file
	String				defaultFileName;
	String				selectedFileName;
	String				currentFileFilePath;
	String				templateFilePath;

	/**
	 * constructor using the application home path and the device signature as initialization parameter
	 * @param deviceSignature - device signature as String (Picolario_K1)
	 */
	public GraphicsTemplate(String deviceSignature) {
		this.defaultFileName = deviceSignature + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4);
		this.templateFilePath = this.defaultFileName;
		log.log(Level.FINE, "graphics template file is " + this.templateFilePath); //$NON-NLS-1$
	}

	/**
	 * @return the isAvailable
	 */
	public boolean isAvailable() {
		return this.isAvailable;
	}

	/**
	 * @return the isSaved
	 */
	public boolean isSaved() {
		return this.isSaved;
	}

	/**
	 * load the properties from file
	 */
	public void load() {
		try {
			this.currentFileFilePath = Settings.getGraphicsTemplatePath() + GDE.STRING_FILE_SEPARATOR_UNIX + ((this.selectedFileName == null) ? this.defaultFileName : this.selectedFileName);
			log.log(Level.FINE, "opening template file " + this.currentFileFilePath); //$NON-NLS-1$
			this.loadFromXML(new FileInputStream(new File(this.currentFileFilePath)));
			this.isAvailable = true;
			log.log(Level.FINE, "template file successful loaded " + this.currentFileFilePath); //$NON-NLS-1$
		}
		catch (InvalidPropertiesFormatException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
		}
	}

	/**
	 * store the properties to file
	 */
	public void store() {
		try {
			// check if templatePath exist, else create directory
			File tmpPath = new File(Settings.getGraphicsTemplatePath());
			if (!tmpPath.exists()) {
				if (!tmpPath.mkdir()) {
					log.log(Level.WARNING, "failed to create " + tmpPath);
				}
			}

			this.currentFileFilePath = Settings.getGraphicsTemplatePath() + GDE.STRING_FILE_SEPARATOR_UNIX + ((this.selectedFileName != null && this.selectedFileName.equals(GDE.STRING_EMPTY) || this.selectedFileName == null) ? this.defaultFileName : this.selectedFileName);
			this.storeToXML(new FileOutputStream(new File(this.currentFileFilePath)), "-- DataExplorer GraphicsTemplate --"); //$NON-NLS-1$
			this.isSaved = true;
			this.selectedFileName = null;
		}
		catch (InvalidPropertiesFormatException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * @param newValue the isSaved to set
	 */
	public void setSaved(boolean newValue) {
		this.isSaved = newValue;
	}

	/**
	 * @return the newFileName
	 */
	public String getNewFileName() {
		return this.selectedFileName;
	}

	/**
	 * @param newFileName the newFileName to set
	 */
	public void setNewFileName(String newFileName) {
		this.selectedFileName = newFileName;
	}

	/**
	 * @return the usedFileName
	 */
	public String getCurrentFilePath() {
		return this.currentFileFilePath;
	}

	/**
	 * @return the default filename for the current device and channel configuration number
	 */
	public String getDefaultFileName() {
		return this.defaultFileName;
	}
}
