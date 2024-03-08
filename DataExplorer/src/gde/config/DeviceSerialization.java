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

package gde.config;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Date;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import gde.Analyzer;
import gde.DataAccess;
import gde.DataAccess.LocalAccess;
import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.device.DevicePropertiesType;
import gde.exception.ThrowableUtils;
import gde.log.Level;
import gde.log.Logger;
import gde.utils.StringHelper;

/**
 * Supports access to device xml files.
 * @author Thomas Eickert (USER)
 */
public class DeviceSerialization {
	private static final String	$CLASS_NAME	= DeviceSerialization.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	// JAXB XML environment
	private JAXBContext					jc;
	private Unmarshaller				unmarshaller;
	private Marshaller					marshaller;

	/**
	 * Supported by the data access instance.
	 * Not threadsafe due to JAXB un-/marshallers.
	 */
	public Thread createXsdThread() {
		Thread xsdThread = new Thread("xsdValidation") {
			@Override
			public void run() {
				DeviceSerialization.log.log(Level.INFO, "xsdThread.run()");
				// device properties context
				try (InputStream inputStream = DataAccess.getInstance().getDeviceXsdInputStream()) {
					Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(inputStream));
					DeviceSerialization.this.jc = JAXBContext.newInstance("gde.device"); //$NON-NLS-1$
					DeviceSerialization.this.unmarshaller = DeviceSerialization.this.jc.createUnmarshaller();
					DeviceSerialization.this.unmarshaller.setSchema(schema);
					DeviceSerialization.this.marshaller = DeviceSerialization.this.jc.createMarshaller();
					DeviceSerialization.this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.valueOf(true));
					DeviceSerialization.this.marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, Settings.DEVICE_PROPERTIES_XSD_NAME);
					DeviceSerialization.log.log(Level.TIME, "schema factory setup time = ", StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (Exception e) {
					DeviceSerialization.log.log(Level.SEVERE, "xsdThread.run()" + e.getMessage(), e);
				}
			}
		};
		return xsdThread;
	}

	/**
	 * Not threadsafe due to JAXB un-/marshallers.
	 */
	public static Thread createMigrationThread() {
		Thread migrationThread = new Thread("migration") {
			@Override
			public void run() {
				log.log(Level.INFO, "start migration thread");
				Analyzer analyzer = Analyzer.getInstance();
				LocalAccess localAccess = (LocalAccess) analyzer.getDataAccess();
				int lastVersion = Integer.valueOf(GDE.DEVICE_PROPERTIES_XSD_VERSION.substring(GDE.DEVICE_PROPERTIES_XSD_VERSION.lastIndexOf("_V") + 2)) - 1;
				for (int i = lastVersion; i >= 10; i--) {
					if (localAccess.existsDeviceMigrationFolder(i)) {
						log.log(Level.INFO, "previous devices exist, migrate from version " + i);
						try (InputStream inputStream = localAccess.getDeviceXsdMigrationStream(i)) {
							Unmarshaller tmpUnmarshaller = JAXBContext.newInstance("gde.device").createUnmarshaller();//$NON-NLS-1$
							tmpUnmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(inputStream)));

							for (Path xmlFileSubPath : localAccess.getDeviceXmlSubPaths(i)) {
								DeviceConfiguration oldConfig = new DeviceConfiguration(xmlFileSubPath, tmpUnmarshaller, analyzer);
								DeviceConfiguration newConfig = analyzer.getDeviceConfigurations().get(oldConfig.getName());
								if (oldConfig.isUsed() && newConfig != null) {
									newConfig.setUsed(true);
									if (oldConfig.getPort().length() > 1 && !oldConfig.getPort().startsWith("USB")) newConfig.setPort(oldConfig.getPort());
									if (oldConfig.getDataBlockPreferredDataLocation().length() > 1)
										newConfig.setDataBlockPreferredDataLocation(oldConfig.getDataBlockPreferredDataLocation());

									newConfig.storeDeviceProperties();
									log.log(Level.INFO, "migrated device configuration " + newConfig.getName());
								} else if (!oldConfig.isUsed()) {
									// WB this is a file system garbage cleaner (rf. to commit 'Fix creating new object graphics templates for criteria never utilized')
									localAccess.deleteDeviceHistoTemplates(oldConfig.getName()); // todo remove
								}
							}
						} catch (Exception e) {
							DeviceSerialization.log.log(Level.SEVERE, "xsdThread.run()" + e.getMessage(), e);
						}
						break;
					}
				}
				log.log(Level.TIME, "finished migration thread");
			}
		};
		return migrationThread;
	}

	@SuppressWarnings({ "unchecked", "static-method" }) // cast to (JAXBElement<DevicePropertiesType>)
	public JAXBElement<DevicePropertiesType> getTopElement(String xmlFilePath, Unmarshaller tmpUnmarshaller, LocalAccess localAccess)
			throws JAXBException {
		try (InputStream stream = localAccess.getDeviceXmlInputStream(xmlFilePath)) {
			return (JAXBElement<DevicePropertiesType>) tmpUnmarshaller.unmarshal(stream);
		} catch (Exception e) {
			throw ThrowableUtils.rethrow(e);
		}
	}

	public JAXBElement<DevicePropertiesType> getTopElement(String xmlFilePath, LocalAccess localAccess) throws JAXBException {
		return getTopElement(xmlFilePath, this.unmarshaller, localAccess);
	}

	/**
	 * Use this for roaming data sources support via the DataAccess class.
	 * @param fileSubPath is a relative path based on the roaming folder
	 * @return the device xml header reference
	 */
	@SuppressWarnings({ "unchecked", "static-method" }) // cast to (JAXBElement<DevicePropertiesType>)
	public JAXBElement<DevicePropertiesType> getTopElement(Path fileSubPath, Unmarshaller tmpUnmarshaller, DataAccess dataAccess) throws JAXBException {
		try (InputStream stream = dataAccess.getDeviceXmlInputStream(fileSubPath)) {
			return (JAXBElement<DevicePropertiesType>) tmpUnmarshaller.unmarshal(stream);
		} catch (Exception e) {
			throw ThrowableUtils.rethrow(e);
		}
	}

	/**
	 * Use this for roaming data sources support via the DataAccess class.
	 * @param fileSubPath is a relative path based on the roaming folder
	 * @return the device xml header reference
	 */
	public JAXBElement<DevicePropertiesType> getTopElement(Path fileSubPath, DataAccess dataAccess) throws JAXBException {
		return getTopElement(fileSubPath, this.unmarshaller, dataAccess);
	}

	/**
	 * Use this for roaming data sources support via the DataAccess class.
	 * @param xmlFile points to a device xml file
	 */
	public void marshall(JAXBElement<DevicePropertiesType> element, String xmlFile, LocalAccess localAccess) throws JAXBException {
		try (OutputStream stream = localAccess.getDeviceXmlOutputStream(xmlFile)) {
			marshaller.marshal(element, stream);
		} catch (Exception e) {
			throw ThrowableUtils.rethrow(e);
		}
	}
}
