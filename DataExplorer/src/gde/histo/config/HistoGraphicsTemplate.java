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

		Copyright 2017 Google Inc. (Guava Cache)
    Copyright (c) 2016,2017,2018,2019 Thomas Eickert
****************************************************************************************/
/*
 * Copyright (C) 2017 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package gde.histo.config;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.sun.istack.Nullable;

import gde.Analyzer;
import gde.DataAccess.LocalAccess;
import gde.GDE;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.device.MeasurementType;
import gde.histo.innercache.Cache;
import gde.histo.innercache.CacheBuilder;
import gde.histo.utils.SecureHash;
import gde.log.Level;
import gde.log.Logger;

/**
 * Histo graphics visualization, store, restore.
 * Values related to records are stored like this:</br>
 * {@code <entry key="Tx_isPositionLeft">false</entry>}
 * @author Thomas Eickert
 */
public abstract class HistoGraphicsTemplate extends Properties {
	static final String																				$CLASS_NAME				= HistoGraphicsTemplate.class.getName();
	static final Logger																				log								= Logger.getLogger($CLASS_NAME);
	static final long																					serialVersionUID	= 2088159376716311896L;

	private static final Cache<String, HistoGraphicsTemplate>	memoryCache				=																				//
			CacheBuilder.newBuilder().maximumSize(444).recordStats().build();																								// key is the file Name

	// constants to make the histo template consumer classes independent from the RecordSet / Record classes.

	/**
	 * This measurement can be read from the device, otherwise it is calculated.
	 * Replaces the Record class constant.
	 */
	public final static String																IS_ACTIVE					= "_isActive";
	/**
	 * Quantiles.
	 * Replaces the AbstractRecordSet class constant.
	 */
	public static final String																SMART_STATISTICS	= "RecordSet_smartStatistics";

	/**
	 * Conversion into a skeleton histo template.
	 */
	protected static final class ConvertedLegacyTemplate extends Properties {
		private static final long						serialVersionUID	= -4459032385846145309L;

		private final Path									legacyFileSubPath;

		private final HistoGraphicsTemplate	convertedTemplate;

		/**
		 * Access to the default graphics template file used by the legacy kernel.
		 * @param objectKey
		 */
		ConvertedLegacyTemplate(Analyzer analyzer) {
			String deviceSignature = analyzer.getActiveDevice().getName() + GDE.STRING_UNDER_BAR + analyzer.getActiveChannel().getNumber();
			this.legacyFileSubPath = Paths.get(deviceSignature + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4));
			log.log(Level.FINE, "from signature ", this);

			this.convertedTemplate = HistoGraphicsTemplate.createGraphicsTemplate(analyzer);
			this.convertedTemplate.setHistoFileName(deviceSignature + "H" + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4));
		}

		/**
		 * Access to any graphics template file used by the legacy kernel.
		 * @param fileSubPath based on the standard roaming template folder
		 * @param objectKey
		 */
		public ConvertedLegacyTemplate(Path fileSubPath, Analyzer analyzer) {
			this.legacyFileSubPath = fileSubPath;
			log.log(Level.FINE, "from file ", this);

			this.convertedTemplate = HistoGraphicsTemplate.createGraphicsTemplate(analyzer);
			String pureFileName = fileSubPath.getFileName().toString().substring(0, fileSubPath.getFileName().toString().lastIndexOf("."));
			if (pureFileName.endsWith("H")) pureFileName = pureFileName.substring(0, pureFileName.length() - 1); // might result in conversion in situ
			this.convertedTemplate.setHistoFileName(pureFileName + "cnvH" + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4));
		}

		/**
		 * Load the legacy graphics template and convert.
		 * @return the converted histo graphics template
		 */
		HistoGraphicsTemplate convertToHistoTemplate() throws FileNotFoundException, IOException {
			try (InputStream stream = convertedTemplate.analyzer.getDataAccess().getGraphicsTemplateInputStream(legacyFileSubPath)) {
				loadFromXML(stream);
			}

			DeviceConfiguration configuration = convertedTemplate.analyzer.getDeviceConfigurations().get(convertedTemplate.analyzer.getActiveDevice().getName()); // ok
			List<MeasurementType> channelMeasurements = configuration.getChannelMeasuremts(convertedTemplate.analyzer.getActiveChannel().getNumber());
			for (Map.Entry<Object, Object> entry : entrySet()) {
				convertedTemplate.setProperty(getPrefixedName((String) entry.getKey(), channelMeasurements), (String) entry.getValue());
			}
			convertedTemplate.setCommentSuffix(legacyFileSubPath.getFileName().toString() + " " + Instant.ofEpochMilli(convertedTemplate.analyzer.getDataAccess().getGraphicsTemplateLastModified(legacyFileSubPath)));
			return convertedTemplate;
		}

		/**
		 * @param key example {@code 7_isVisible} or {@code RecordSet_horizontalGridColor}
		 * @return the prefixed key.</br>
		 *         Examples: {@code Tx__isVisible} , {@code RecordSet_horizontalGridColor}
		 */
		private String getPrefixedName(String key, List<MeasurementType> channelMeasurements) {
			log.log(FINE, "key=", key);
			String[] splitKey = key.split(GDE.STRING_UNDER_BAR);
			String prefixedName;
			if (splitKey[0].isEmpty()) {
				prefixedName = key;
			} else {
				try {
					MeasurementType measurementType = channelMeasurements.get(Integer.parseInt(splitKey[0]));
					prefixedName = measurementType.getName() + GDE.STRING_UNDER_BAR + GDE.STRING_UNDER_BAR + splitKey[1];
				} catch (NumberFormatException e) {
					prefixedName = key;
				} catch (IndexOutOfBoundsException e) {
					prefixedName = "|Unk|" + key;
				} catch (Exception e) {
					prefixedName = "|Exc|" + key;
				}
			}
			return prefixedName;
		}

		@Override
		public String toString() {
			return "DefaultGraphicsTemplate [legacyFileSubPath=" + this.legacyFileSubPath + "]";
		}
	}

	/**
	 * Use as file name the device signature with suffix "H".
	 * For object related templates use a sub directory.
	 * @return an instance which conforms to the settings and the parameters
	 */
	public static HistoGraphicsTemplate createGraphicsTemplate(Analyzer analyzer) {
		boolean tmpSuppressNewFile = false;
		Settings settings = analyzer.getSettings();
		if (!settings.getActiveObjectKey().isEmpty() && settings.isObjectTemplatesActive()) {
			return new ObjectGraphicsTemplate(analyzer, settings.getActiveObjectKey(), tmpSuppressNewFile);
		} else if (settings.getActiveObjectKey().isEmpty() && settings.isObjectTemplatesActive()) {
			return new ObjectGraphicsTemplate(analyzer, GDE.STRING_DEVICE_ORIENTED_FOLDER, tmpSuppressNewFile);
		} else {
			return new StandardGraphicsTemplate(analyzer, tmpSuppressNewFile);
		}
	}

	/**
	 * Create and populate the template from an existing template file.
	 * Search first for the object template directory a histo template (suffix "H").
	 * Fall back to a pure device template if not available.
	 * @return an instance which conforms to the settings and the parameters
	 */
	public static HistoGraphicsTemplate createTransitoryTemplate(Analyzer analyzer) {
		boolean tmpSuppressNewFile = true;
		Settings settings = analyzer.getSettings();
		if (!settings.getActiveObjectKey().isEmpty() && settings.isObjectTemplatesActive()) {
			return new ObjectGraphicsTemplate(analyzer, settings.getActiveObjectKey(), tmpSuppressNewFile);
		} else if (settings.getActiveObjectKey().isEmpty() && settings.isObjectTemplatesActive()) {
			return new ObjectGraphicsTemplate(analyzer, GDE.STRING_DEVICE_ORIENTED_FOLDER, tmpSuppressNewFile);
		} else {
			return new StandardGraphicsTemplate(analyzer, tmpSuppressNewFile);
		}
	}

	protected final boolean		suppressNewFile;
	protected final Analyzer	analyzer;
	protected final String		defaultHistoFileName;
	protected final String		defaultFileName;

	protected Path						currentFilePathFragment;
	protected String					histoFileName;

	protected boolean					isAvailable		= false;
	protected boolean					isSaved				= false;	// indicates if template is saved to file
	protected String					commentSuffix	= "";

	/**
	 * Create and populate the template from an existing template file.
	 * Search first for a histo template file (suffix "H") in the object template directory.
	 * Create histo template file if not available.
	 * @param suppressNewFile true suppresses the object template file creation
	 */
	protected HistoGraphicsTemplate(Analyzer analyzer, boolean suppressNewFile) {
		this.analyzer = analyzer;
		this.suppressNewFile = suppressNewFile;
		String deviceSignature = analyzer.getActiveDevice().getName() + GDE.STRING_UNDER_BAR + analyzer.getActiveChannel().getNumber();
		this.defaultFileName = deviceSignature + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4);
		this.defaultHistoFileName = deviceSignature + "H" + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4);
		this.setHistoFileName(defaultHistoFileName);
		log.log(FINE, "Histo graphics template file is ", this.defaultFileName);
	}

	public void setCommentSuffix(String commentSuffix) {
		this.commentSuffix = commentSuffix;
	}

	public boolean isAvailable() {
		return this.isAvailable;
	}

	public boolean isSaved() {
		return this.isSaved;
	}

	/**
	 * Load using standard procedures if the file path directory is equal to the target directory (based on object settings).
	 * Otherwise try loading w/o fall back to a default file and w/o conversion from legacy format.
	 * Support file system roaming sources only!
	 * @param filePath is any path but is preferably an xml properties file in the same directory as the target directory
	 */
	public void loadAlien(Path filePath) {
		Path fileSubPath = null;
		try {
			Path tmpSubPath = Paths.get(Settings.getGraphicsTemplatePath()).relativize(filePath);
			fileSubPath = tmpSubPath.toString().startsWith(GDE.STRING_PARENT_DIR) ? null : tmpSubPath;
		} catch (Exception e) {
			fileSubPath = null;
		}
		if (fileSubPath != null && fileSubPath.getNameCount() > 1 && fileSubPath.getParent().equals(getTargetFileSubPath().getParent())) {
			setHistoFileName(filePath.getFileName().toString());
			load();
		} else {
			// might result in template entries which are not usable (this rubbish remains even if the file is saved)
			try (InputStream stream = ((LocalAccess) analyzer.getDataAccess()).getAlienTemplateInputStream(filePath.toFile())) {
				currentFilePathFragment = null;
				clear();
				this.loadFromXML(stream);
				log.log(FINE, "alien file loaded successfully", filePath);
				currentFilePathFragment = filePath;
				setHistoFileName(filePath.getFileName().toString());
				this.isAvailable = true;
			} catch (InvalidPropertiesFormatException e) {
				log.log(SEVERE, e.getMessage(), e);
			} catch (Exception e) {
				log.log(WARNING, e.getMessage());
			}
		}
	}

	/**
	 * Load the properties from the template file.
	 * A template file with a graphics template format is imported.
	 * Take the graphics template file if not available.
	 */
	public void load() {
		try {
			currentFilePathFragment = null;
			Path fileSubPath = getTargetFileSubPath();
			if (!analyzer.getDataAccess().existsGraphicsTemplate(fileSubPath)) {
				if (this.suppressNewFile) {
					fileSubPath = Paths.get(defaultFileName);
					loadFromXml(fileSubPath);
				} else {
					log.log(FINE, "convert legacy default template as a replacement for ", fileSubPath);
					ConvertedLegacyTemplate template = new ConvertedLegacyTemplate(analyzer);
					HistoGraphicsTemplate convertedTemplate = template.convertToHistoTemplate();
					convertedTemplate.store();
					fileSubPath = convertedTemplate.getTargetFileSubPath();

					clear();
					putAll(convertedTemplate);
				}
			} else {
				clear();
				loadFromXml(fileSubPath);
				boolean isHistoTemplate = keySet().stream().map(String.class::cast).anyMatch(k -> k.indexOf(GDE.CHAR_UNDER_BAR + GDE.STRING_UNDER_BAR) >= 0);
				if (!isHistoTemplate) {
					log.log(FINE, "convert template identified as legacy template ", fileSubPath);
					ConvertedLegacyTemplate template = new ConvertedLegacyTemplate(fileSubPath, analyzer);
					HistoGraphicsTemplate convertedTemplate = template.convertToHistoTemplate();

					clear();
					putAll(convertedTemplate);
				}
			}
			currentFilePathFragment = fileSubPath;
			this.isAvailable = true;
		} catch (InvalidPropertiesFormatException e) {
			log.log(SEVERE, e.getMessage(), e);
		} catch (Exception e) {
			log.log(WARNING, e.getMessage());
		}
	}

	protected void loadFromXml(Path fileSubPath) throws IOException, InvalidPropertiesFormatException, FileNotFoundException {
		log.log(FINE, "opening template file ", fileSubPath);
		String fileName = SecureHash.sha1(fileSubPath.toString());
		Properties cachedInstance = memoryCache.getIfPresent(fileName);
		if (cachedInstance == null || cachedInstance == this) { // we work on the cached instance -> reload required
			try (InputStream stream = analyzer.getDataAccess().getGraphicsTemplateInputStream(fileSubPath)) {
				this.loadFromXML(stream);
				memoryCache.put(fileName, this);
				log.log(FINE, "template file successful loaded ", fileSubPath);
			}
		} else {
			this.clear();
			this.putAll(cachedInstance);
		}
	}

	/**
	 * Store the properties to the histo template file.
	 */
	public void store() {
		String deviceSignature = analyzer.getActiveDevice().getName() + GDE.STRING_UNDER_BAR + analyzer.getActiveChannel().getNumber();
		String propertiesComment = "-- DataExplorer Histo GraphicsTemplate " + deviceSignature + " -- " + getTargetFileSubPath().getFileName().toString() + " " + ZonedDateTime.now().toInstant() + " -- " + commentSuffix;
		store(propertiesComment);
	}

	protected void store(String propertiesComment) {
		try {
			currentFilePathFragment = null;
			try (OutputStream stream = analyzer.getDataAccess().getGraphicsTemplateOutputStream(getTargetFileSubPath())) {
				this.storeToXML(stream, propertiesComment);
				String fileName = SecureHash.sha1(getTargetFileSubPath().toString());
				memoryCache.put(fileName, this);
			}
			currentFilePathFragment = getTargetFileSubPath();
			this.isSaved = true;
		} catch (InvalidPropertiesFormatException e) {
			log.log(SEVERE, e.getMessage(), e);
		} catch (Exception e) {
			log.log(WARNING, e.getMessage(), e);
		}
	}

	public void setSaved(boolean newValue) {
		this.isSaved = newValue;
	}

	public void setHistoFileName(String fileName) {
		log.log(FINE, "fileName=", fileName);
		this.histoFileName = fileName;
	}

	public String getHistoFileName() {
		return this.histoFileName;
	}

	public String getDefaultHistoFileName() {
		return this.defaultHistoFileName;
	}

	/**
	 * @return the file subPath based on the standard template directory
	 */
	public abstract Path getTargetFileSubPath();

	public String getFilePathMessage() {
		if (currentFilePathFragment != null && !currentFilePathFragment.getFileName().toString().equals(getDefaultHistoFileName())) {
			return currentFilePathFragment.toString();
		} else {
			return "";
		}
	}

	/**
	 * @return the default filename for the current device and channel configuration number
	 */
	public String getDefaultFileName() {
		return this.histoFileName;
	}

	/**
	 * @param recordName
	 * @param keyPostfix is one of the constant strings for record properties
	 * @param defaultValue is the default value in case the property does not exist
	 * @return the value in this property list with the specified key value.
	 */
	public String getRecordProperty(String recordName, String keyPostfix, String defaultValue) {
		return super.getProperty(recordName + GDE.STRING_UNDER_BAR + keyPostfix, defaultValue);
	}

	/**
	 * @param recordName
	 * @param keyPostfix is one of the constant strings for record properties
	 * @return the value in this property list with the specified key value.
	 */
	public String getRecordProperty(String recordName, String keyPostfix) {
		return super.getProperty(recordName + GDE.STRING_UNDER_BAR + keyPostfix);
	}

	/**
	 * @param recordName
	 * @param keyPostfix is one of the constant strings for record properties
	 * @param value the value corresponding to key.
	 * @return the previous value of the specified key in this property list
	 */
	@Nullable // if there was no previous value
	public String setRecordProperty(String recordName, String keyPostfix, String value) {
		return (String) super.setProperty(recordName + GDE.STRING_UNDER_BAR + keyPostfix, value);
	}

	@Override
	public String toString() {
		return "HistoGraphicsTemplate [defaultHistoFileName=" + this.defaultHistoFileName + ", defaultFileName=" + this.defaultFileName + ", histoFileName=" + this.histoFileName + ", commentSuffix=" + this.commentSuffix + "]";
	}

}
