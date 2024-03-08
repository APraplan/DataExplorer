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
package gde.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sun.istack.Nullable;

import gde.DataAccess;
import gde.GDE;
import gde.config.ExportService;
import gde.config.ExportService.DataFeed;
import gde.device.InputTypes;
import gde.log.Level;
import gde.log.Logger;

/**
 * Collect data about the device plugin services.
 * Please note: The XML element / attribute names are hard coded in a SAX parser.
 * @author Thomas Eickert (USER)
 */
public class ExportServiceBuilder {
	private static final String	$CLASS_NAME								= ExportServiceBuilder.class.getName();
	private static final Logger	log												= Logger.getLogger($CLASS_NAME);

	private static final String	DEVICE_TAG								= "Device";
	private static final String	DEVICE_NAME_TAG						= "name";
	private static final String	DEVICE_MANUFACTURER_TAG		= "manufacturer";
	private static final String	SERIAL_PORT_TAG						= "SerialPort";
	private static final String	USB_PORT_TAG							= "UsbPort";
	private static final String	DATA_BLOCK_TAG						= "DataBlock";
	private static final String	DATA_BLOCK_FORMAT_TAG			= "format";
	private static final String	DATA_BLOCK_INPUT_TYPE_TAG	= "inputType";
	private static final String	CHANNELS_TAG							= "Channels";

	private final DataAccess		dataAccess;

	public class MySAXTerminatorException extends SAXException {
		private static final long serialVersionUID = -3581286881580383862L;

		public MySAXTerminatorException(String message) {
			super(message);
		}
	}

	public ExportServiceBuilder(DataAccess dataAccess) {
		this.dataAccess = dataAccess;
	}

	/**
	 * @param args deviceProjectPath , hiddenDevicesCsv (e.g. ../AkkuMaster , HoTTAdapterD,HoTTAdapterX )
	 */
	public static void main(String[] args) throws IOException {
		Path deviceProjectPath = Paths.get(args[0]);
		Predicate<ExportService> deviceChecker;
		if (args.length > 1) {
			List<String> hiddenDevices = Arrays.asList(args[1].split(GDE.STRING_COMMA));
			deviceChecker = e -> !hiddenDevices.contains(e.getName());
		} else {
			deviceChecker = e -> true;
		}

		ExportServiceBuilder builder = new ExportServiceBuilder(DataAccess.getInstance());
		List<ExportService> services = builder.getServices(deviceProjectPath.getParent(), deviceProjectPath.getFileName().toString());
		String servicesText = services.stream().filter(deviceChecker).map(ExportService::toString).collect(Collectors.joining(", "));

		System.out.print(servicesText);
	}

	/**
	 * @param basePath points to the development folder (e.g. C:/dataexplorer)
	 * @param projectName is the device project's development folder (e.g. HoTTAdapter)
	 * @return the device plugin services for all device property files
	 */
	public List<ExportService> getServices(Path basePath, String projectName) {
		List<ExportService> services = new ArrayList<>();
		Path devicePropertiesPath = basePath.resolve(projectName).resolve("src/resource");
		try {
			for (String fileName : dataAccess.getDevicePropertyFileNames(devicePropertiesPath)) {
				ExportService service = getService(basePath, projectName, fileName);
				if (service != null) services.add(service);
			}
		} catch (Exception e) {
			log.log(Level.INFO, "not a device project :", projectName);
		}
		return services;
	}

	/**
	 * @param basePath points to the development folder (e.g. C:/dataexplorer)
	 * @param projectName is the device project's development folder (e.g. HoTTAdapter)
	 * @param propertiesFileName the device xml file (e.g. HoTTAdapter2.xml)
	 * @return the device plugin service
	 */
	@Nullable
	public ExportService getService(Path basePath, String projectName, String propertiesFileName) {
		String[] deviceValues = new String[] { "", "" };
		Set<DataFeed> dataFeeds = new LinkedHashSet<ExportService.DataFeed>(); //take order of data source as declared in device XML
		final ExportService exportService;

		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();

			DefaultHandler handler = new DefaultHandler() {

				private boolean	isDevice						= false;
				private boolean	isName							= false;
				private boolean	isManufacturer			= false;
				private boolean	existsSerialPort		= false;
				private boolean	existsUsbPort				= false;
				private boolean	isDataBlock					= false;
				private String	deviceName					= "";
				private String	deviceManufacturer	= "";

				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					switch (qName) {
					case DEVICE_TAG:
						isDevice = true;
						break;

					case DEVICE_NAME_TAG:
						isName = true;
						break;

					case DEVICE_MANUFACTURER_TAG:
						isManufacturer = true;
						break;

					case SERIAL_PORT_TAG:
						existsSerialPort = true;
						break;

					case USB_PORT_TAG:
						existsUsbPort = true;
						break;

					case DATA_BLOCK_TAG:
						isDataBlock = true;
						break;

					case DATA_BLOCK_FORMAT_TAG:
						if (isDataBlock) {
							for (int i = 0; i < attributes.getLength(); i++) {
								String string = attributes.getQName(i);
								if (string.equals(DATA_BLOCK_INPUT_TYPE_TAG)) {
									if (dataFeeds.isEmpty()) {
										deviceValues[0] = deviceName;
										deviceValues[1] = deviceManufacturer;
									}
									String value = attributes.getValue(i);
									if (InputTypes.fromValue(value) == InputTypes.FILE_IO) {
										dataFeeds.add(DataFeed.FILE);
									} else if (InputTypes.fromValue(value) == InputTypes.SERIAL_IO) {
										if (existsSerialPort) {
											dataFeeds.add(DataFeed.SERIAL_IO);
										} else if (existsUsbPort) {
											dataFeeds.add(DataFeed.NATIVE_USB);
										}	else {
												dataFeeds.add(DataFeed.NO_DATA_SOURCE);//device w/o data source
										}
									} else {
										dataFeeds.add(DataFeed.NO_DATA_SOURCE);//device w/o data source
									}
									log.log(Level.FINE, "dataFeed =", value);
								}
							}
						}
						break;

					case CHANNELS_TAG:
						throw new MySAXTerminatorException("the service items do not ly beyond this xml element");

					default:
						break;
					}
				}

				@Override
				public void endElement(String uri, String localName, String qName) throws SAXException {
					switch (qName) {
					case DEVICE_TAG:
						isDevice = false;

					case DATA_BLOCK_TAG:
						isDataBlock = false;
						break;

					default:
						break;
					}
				}

				@Override
				public void characters(char ch[], int start, int length) throws SAXException {
					if (isDevice) {
						if (isName) {
							deviceName = new String(ch, start, length);
							log.log(Level.FINE, "deviceName : ", deviceName);
							isName = false;
						}
						if (isManufacturer) {
							deviceManufacturer = new String(ch, start, length);
							log.log(Level.FINE, "deviceManufacturer : ", deviceManufacturer);
							isManufacturer = false;
						}
					}
				}

			};

			Path devicePropertiesPath = basePath.resolve(projectName).resolve("src/resource").resolve(propertiesFileName);
			saxParser.parse(dataAccess.getDeviceXmlInputStream(devicePropertiesPath.toString()), handler);

		} catch (MySAXTerminatorException e) {
			// correctly terminated
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		exportService = dataFeeds.isEmpty() ? null : new ExportService(deviceValues[0], deviceValues[1], dataFeeds, projectName + ".jar");
		return exportService;
	}

}
