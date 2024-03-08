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

    Copyright (c) 2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.config;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import gde.GDE;
import gde.messages.MessageIds;
import gde.messages.Messages;

/**
 * simple structure of exported services as defined for each device implemented in a device plug-in jar manifest
 * @author brueg
 */
public class ExportService {

	public enum DataFeed {

		//TODO this get static initialized, remove to avoid none debug able errors
		NO_DATA_SOURCE(GDE.STRING_MESSAGE_CONCAT), //
		SERIAL_IO(MessageIds.GDE_MSGT0955), //
		FILE(MessageIds.GDE_MSGT0956), //
		NATIVE_USB(MessageIds.GDE_MSGT0957);

		private String messageId;

		private DataFeed(String messageId) {
			this.messageId = messageId;
		}

		public String value() {
			return name();
		}

		public String displayText() {
			return this.messageId.length() > 3 ? Messages.getString(this.messageId) : this.messageId;
		}

		public static DataFeed fromValue(String v) {
			return valueOf(v);
		}

		/**
		 * @param values
		 * @return ordered data sources according list in device XML
		 */
		public static LinkedHashSet<DataFeed> fromStringArray(String[] values) {
			LinkedHashSet<DataFeed> dataFeeds = new LinkedHashSet<DataFeed>();
			for (String value : values) {
				dataFeeds.add(DataFeed.valueOf(value));
			}
			return dataFeeds;
		}
	}

	final String				name;
	final String				manufacturer;
	final Set<DataFeed>	dataFeeds;
	final String				jar;

	/**
	 * Create from the device properties.
	 */
	public ExportService(String name, String manufacturer, Set<DataFeed> dataFeeds, String jarPath) {
		this.name = name;
		this.manufacturer = manufacturer;
		this.dataFeeds = dataFeeds;
		this.jar = jarPath;
	}

	/**
	 * build simple structure of exported services as defined for each device implemented in a device plug-in jar manifest
	 */
	public ExportService(String service, String jarPath) {
		String[] temp = service.split(GDE.STRING_COLON);
		if (temp.length != 3) throw new IllegalArgumentException(String.format("wrong number of arguments in %s", service));
		this.name = temp[0].trim();
		this.manufacturer = temp[1].trim();
		String[] inputTexts = temp[2].trim().split(GDE.STRING_SEMICOLON);
		this.dataFeeds = DataFeed.fromStringArray(inputTexts);
		this.jar = jarPath;
	}

	public String getName() {
		return name;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public String getDataFeed() {
		return dataFeeds.stream().map(DataFeed::displayText).collect(Collectors.joining(GDE.STRING_SEMICOLON));
	}

	public JarFile getJarFile() throws IOException {
		return new JarFile(jar);
	}

	public Path getJarPath() {
		return Paths.get(jar);
	}

	public String getDisplayText() {
		return String.join(GDE.STRING_COLON, name, manufacturer, getDataFeed());
	}

	@Override
	public String toString() {
		String neutralSource = dataFeeds.stream().map(DataFeed::name).collect(Collectors.joining(GDE.STRING_SEMICOLON));
		return String.join(GDE.STRING_COLON, name, manufacturer, neutralSource);
	}

}
