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
    Copyright (c) 2017,2018,2019 Thomas Eickert
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

package gde.histo.exclusions;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import gde.DataAccess;
import gde.GDE;
import gde.histo.innercache.Cache;
import gde.histo.innercache.CacheBuilder;
import gde.histo.innercache.CacheStats;
import gde.histo.utils.SecureHash;
import gde.log.Logger;

/**
 * Excluding files from history analysis via user activity.
 * Is based on property files for the current primary folder.
 * Stores the exclusions property files in the user directory.
 * @author Thomas Eickert
 */
public final class ExclusionData extends Properties { // todo solution for files with the same name - all of them are excluded!
	private static final String												$CLASS_NAME				= ExclusionData.class.getName();
	private static final Logger												log								= Logger.getLogger($CLASS_NAME);
	private static final long													serialVersionUID	= -2477509505185819765L;

	private static final Cache<String, ExclusionData>	memoryCache				=																//
			CacheBuilder.newBuilder().maximumSize(111).recordStats().build();																// key is the file Name

	/**
	 * Criterion defining the exclusion file name.
	 * Does not define the path for accessing the file.
	 */
	private final Path																activeFolder;
	private final DataAccess													dataAccess;

	/**
	 * @return true if an exclusion for all recordsets in the data file is active
	 */
	public boolean isExcluded(String fileName) {
		String exclusionValue = getProperty(fileName);
		boolean isExcluded = exclusionValue != null && exclusionValue.isEmpty();
		if (isExcluded) log.log(FINE, "file excluded ", fileName);
		return isExcluded;
	}

	/**
	 * @return true if an exclusion for the recordset in the data file is active
	 */
	public boolean isExcluded(String fileName, String recordsetBaseName) {
		if (recordsetBaseName.isEmpty()) throw new UnsupportedOperationException();

		String exclusionValue = getProperty(fileName);
		boolean isExcluded = exclusionValue != null && (exclusionValue.isEmpty() || exclusionValue.contains(recordsetBaseName));
		if (isExcluded) log.fine(() -> String.format("recordset excluded %s %s", fileName, recordsetBaseName));
		return isExcluded;
	}

	/**
	 * Determine the exclusions which are active for the current channel.
	 * @return the exclusion information for the trusses excluded from the history
	 */
	public String[] getExcludedTrusses(List<Path> excludedPaths) {
		return excludedPaths.stream() //
				.map(p -> {
					String key = p.getFileName().toString();
					String property = getProperty(key);
					return (property.isEmpty() ? key : key + GDE.STRING_BLANK_COLON_BLANK + property);
				}) //
				.distinct().sorted(Collections.reverseOrder()) //
				.toArray(String[]::new);
	}

	/**
	 * Use the singleton getInstance instead for calls by the legacy UI based on the settings (object key and active device name).
	 * @param activeFolder is the current data files folder based on the object key and active device
	 */
	public ExclusionData(Path activeFolder, DataAccess dataAccess) {
		super();
		this.activeFolder = activeFolder;
		this.dataAccess = dataAccess;

		String fileName = SecureHash.sha1(this.activeFolder.toString());
		Properties cachedInstance = memoryCache.getIfPresent(fileName);
		log.finer(() -> {
			CacheStats stats = memoryCache.stats();
			return String.format("evictionCount=%d  hitCount=%d  missCount=%d hitRate=%f missRate=%f", stats.evictionCount(), stats.hitCount(), stats.missCount(), stats.hitRate(), stats.missRate());
		});
		if (cachedInstance == null) {
			load();
		} else {
			this.putAll(cachedInstance);
		}
	}

	@Override
	public String toString() {
		return this.stringPropertyNames().stream().sorted().map(this::getProperty) //
				.map(k -> k.isEmpty() ? k : k + GDE.STRING_BLANK_COLON_BLANK + getProperty(k)) //
				.collect(Collectors.joining(GDE.STRING_NEW_LINE));
	}

	@Override
	public Object setProperty(String dataFileName, String recordsetBaseName) {
		if (recordsetBaseName.isEmpty()) throw new UnsupportedOperationException();

		return super.setProperty(dataFileName, recordsetBaseName);
	}

	/**
	 * Set the dataFileName as excluded for all recordSets.
	 * @param dataFileName
	 * @return the previous value which is a csv string
	 */
	public Object setProperty(String dataFileName) {
		return super.setProperty(dataFileName, GDE.STRING_EMPTY);
	}

	/**
	 * @param dataFileName
	 * @param recordsetBaseName
	 * @return the previous value
	 */
	public Object addToProperty(String dataFileName, String recordsetBaseName) {
		if (recordsetBaseName.isEmpty()) throw new UnsupportedOperationException();

		if (this.getProperty(dataFileName) == null)
			return super.setProperty(dataFileName, recordsetBaseName);
		else {
			if (this.getProperty(dataFileName).isEmpty() || this.getProperty(dataFileName).contains(recordsetBaseName))
				return this.getProperty(dataFileName);
			else
				return super.setProperty(dataFileName, this.getProperty(dataFileName) + GDE.STRING_BLANK_PLUS_BLANK + recordsetBaseName);
		}
	}

	private void load() {
		dataAccess.ensureExclusionDirectory();
		String fileName = SecureHash.sha1(this.activeFolder.toString());
		if (dataAccess.existsExclusionFile(fileName)) {
			try (Reader reader = new InputStreamReader(dataAccess.getExclusionsInputStream(fileName))) {
				this.load(reader);
				memoryCache.put(fileName, this);
			} catch (Throwable e) {
				log.log(SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * Write the file if excludes are defined, else delete the file.
	 */
	public void store() {
		if (this.size() > 0) {
			dataAccess.ensureExclusionDirectory();
			String fileName = SecureHash.sha1(this.activeFolder.toString());
			try (Writer writer = new OutputStreamWriter(dataAccess.getExclusionsOutputStream(fileName), "UTF-8")) {
				this.store(writer, this.activeFolder.toString());
				memoryCache.put(fileName, this);
			} catch (Throwable e) {
				log.log(SEVERE, e.getMessage(), e);
			}
		} else {
			delete();
			String fileName = SecureHash.sha1(this.activeFolder.toString());
			memoryCache.invalidate(fileName);
		}
	}

	public void delete() {
		String fileName = SecureHash.sha1(this.activeFolder.toString());
		dataAccess.deleteExclusionFile(fileName);
		memoryCache.invalidateAll();
	}

}
