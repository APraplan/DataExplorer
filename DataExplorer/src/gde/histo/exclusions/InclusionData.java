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

import static java.util.logging.Level.SEVERE;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Comparator;
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
 * Including records from summary reminders via user activity.
 * Is based on property files for the current primary folder.
 * Stores the inclusions property files in the user directory.
 * @author Thomas Eickert
 */
public final class InclusionData extends Properties {
	private static final String												$CLASS_NAME				= InclusionData.class.getName();
	private static final Logger												log								= Logger.getLogger($CLASS_NAME);
	private static final long													serialVersionUID	= -2477509505185819765L;

	private static final Cache<String, InclusionData>	memoryCache				=																//
			CacheBuilder.newBuilder().maximumSize(111).recordStats().build();																// key is the file Name

	/**
	 * Criterion the define the properties file name.
	 * Does not define the path for accessing the file.
	 */
	private final Path																activeFolder;
	private final DataAccess													dataAccess;

	/**
	 * Use the singleton getInstance instead for calls by the legacy UI based on the settings (object key and active device name).
	 * @param activeFolder is the current data files folder based on the object key and active device
	 */
	public InclusionData(Path activeFolder, DataAccess dataAccess) {
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

	public boolean isIncluded(String recordName, boolean defaultValue) {
		if (recordName.isEmpty()) throw new UnsupportedOperationException();

		boolean included = defaultValue;
		if (!this.isEmpty()) {
			included = getProperty(recordName) != null;
			if (included) log.fine(() -> String.format("record included by user %s %s", activeFolder.toString(), recordName));
		}
		return included;
	}

	@Override
	public String toString() {
		return this.stringPropertyNames().stream() //
				.sorted(Comparator.comparing(this::getProperty)).map(this::getProperty) //
				.map(k -> k.isEmpty() ? k : k + GDE.STRING_BLANK_COLON_BLANK + k) //
				.collect(Collectors.joining(","));
	}

	@Override
	public InclusionData setProperty(String recordName, String irrelevantValue) {
		if (recordName.isEmpty()) throw new UnsupportedOperationException();

		return (InclusionData) super.setProperty(recordName, irrelevantValue);
	}

	/**
	 * @return the previous value which is a csv string
	 */
	public InclusionData setProperty(String recordName) {
		return (InclusionData) super.setProperty(recordName, GDE.STRING_EMPTY);
	}

	/**
	 * Deletes and purges the file if there is no property left.
	 * @return the property value prior to deletion
	 */
	public String deleteProperty(String recordName) {
		String value = (String) remove(recordName);
		return value;
	}

	private void load() {
		dataAccess.ensureInclusionDirectory();
		String fileName = SecureHash.sha1(this.activeFolder.toString());
		if (dataAccess.existsInclusionFile(fileName)) {
			try (Reader reader = new InputStreamReader(dataAccess.getInclusionsInputStream(fileName))) {
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
			dataAccess.ensureInclusionDirectory();
			String fileName = SecureHash.sha1(this.activeFolder.toString());
			try (Writer writer = new OutputStreamWriter(dataAccess.getInclusionsOutputStream(fileName), "UTF-8")) {
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
		dataAccess.deleteInclusionFile(fileName);
		memoryCache.invalidateAll(); // todo invalidate only the inclusions for the specific data file
	}

	/**
	 * @return the names of the records with reminders activated
	 */
	public String[] getIncludedRecordNames() {
		return stringPropertyNames().toArray(new String[0]);
	}

}
