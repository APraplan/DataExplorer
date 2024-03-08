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
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018 Winfried Bruegmann
    							2016,2017,2018 Thomas Eickert
****************************************************************************************/
package gde.device;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.eclipse.swt.custom.CTabItem;

import com.sun.istack.Nullable;

import gde.Analyzer;
import gde.DataAccess;
import gde.DataAccess.LocalAccess;
import gde.GDE;
import gde.comm.IDeviceCommPort;
import gde.config.Settings;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.resource.DeviceXmlResource;
import gde.histo.utils.SecureHash;
import gde.log.Level;
import gde.log.LogFormatter;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.CalculationThread;
import gde.utils.StringHelper;

/**
 * Device Configuration class makes the parsed DeviceProperties XML accessible for the application
 * @author Winfried Brügmann
 */
public class DeviceConfiguration {
	private final static Logger								log												= Logger.getLogger(DeviceConfiguration.class.getName());

	private final Settings										settings;
	final protected DeviceXmlResource					xmlResource								= DeviceXmlResource.getInstance();

	private Path															xmlFile;
	private String														fileSha1Hash							= GDE.STRING_EMPTY;
	// XML JAXB representation
	private JAXBElement<DevicePropertiesType>	elememt;
	private DevicePropertiesType							deviceProps;
	private DeviceType												device;
	private SerialPortType										serialPort;
	private UsbPortType												usbPort;
	private DataBlockType											dataBlock;
	private StateType													state;
	private TimeBaseType											timeBase;
	private DesktopType												desktop;
	private boolean														isChangePropery						= false;

	public final static int										DEVICE_TYPE_UNDEFINED			= 0;
	public final static int										DEVICE_TYPE_CHARGER				= 1;
	public final static int										DEVICE_TYPE_LOGGER				= 2;
	public final static int										DEVICE_TYPE_BALANCER			= 3;
	public final static int										DEVICE_TYPE_CURRENT_SINK	= 4;
	public final static int										DEVICE_TYPE_POWER_SUPPLY	= 5;
	public final static int										DEVICE_TYPE_GPS						= 5;
	public final static int										DEVICE_TYPE_RECEIVER			= 7;
	public final static int										DEVICE_TYPE_MULTIMETER		= 8;
	public final static int										DEVICE_TYPE_CONTROLLER		= 9;

	public final static int										HEIGHT_RELATIVE						= 0;
	public final static int										HEIGHT_ABSOLUTE						= 1;
	public final static int										HEIGHT_CLAMPTOGROUND			= 2;

	public static final String								UNIT_DEGREE_FAHRENHEIT		= "°F";
	public static final String								UNIT_DEGREE_CELSIUS				= "°C";

	protected CalculationThread								calculationThread					= null;																									// universal device
																																																															// calculation thread
																																																															// (slope)
	protected Integer													kmzMeasurementOrdinal			= null;

	/**
	 * holds group index numbers for those channels which have identical measurement names.
	 * the array length is equal to the channel count; the value '-1' denotes a channel without a group assignment.
	 * example: [-1, 0, 0, -1, 1, 1, -1, 1] holds two groups.
	 * the 1st group is identified by the group index '0' and consists of the 2nd and 3rd channel.
	 * the 2nd group is identified by the group index '1' and consists of the 5th, 6th and 8th channel.
	 */
	private int[]															channelGroups;

	/**
	 * method to test this class
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		Handler ch = new ConsoleHandler();
		LogFormatter lf = new LogFormatter();
		ch.setFormatter(lf);
		ch.setLevel(Level.ALL);
		Logger.getLogger(GDE.STRING_EMPTY).addHandler(ch);
		Logger.getLogger(GDE.STRING_EMPTY).setLevel(Level.ALL);

		String basePath = "C:/Documents and Settings/brueg/Application Data/DataExplorer/Devices/"; //$NON-NLS-1$

		try (FileInputStream inputStream = new FileInputStream(basePath + "DeviceProperties_V48.xsd")) {
			Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(inputStream));
			JAXBContext jc = JAXBContext.newInstance("gde.device"); //$NON-NLS-1$
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setSchema(schema);
			JAXBElement<DevicePropertiesType> elememt;

			// simple test with Picolario.xml
			try (FileInputStream inputStream2 = new FileInputStream(basePath + "Picolario.xml")) {
				elememt = (JAXBElement<DevicePropertiesType>) unmarshaller.unmarshal(inputStream2); // $NON-NLS-1$
				DevicePropertiesType devProps = elememt.getValue();
				DeviceType device = devProps.getDevice();
				log.log(Level.ALL, "device.getName() = " + device.getName()); //$NON-NLS-1$
				SerialPortType serialPort = devProps.getSerialPort();
				log.log(Level.ALL, "serialPort.getPort() = " + serialPort.getPort()); //$NON-NLS-1$
				serialPort.setPort("COM10"); //$NON-NLS-1$
				log.log(Level.ALL, "serialPort.getPort() = " + serialPort.getPort()); //$NON-NLS-1$
			}
			// store back manipulated XML
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.valueOf(true));
			marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, Settings.DEVICE_PROPERTIES_XSD_NAME);

			try (FileOutputStream outputStream = new FileOutputStream(basePath + "jaxbOutput.xml")) {
				marshaller.marshal(elememt, outputStream);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		} catch (Throwable t) {
			log.log(Level.SEVERE, t.getMessage(), t);
		}
	}

	/**
	 * No roaming data access support.
	 * Not threadsafe due to JAXB un-/marshallers.
	 */
	public DeviceConfiguration(String xmlFileName) throws FileNotFoundException, JAXBException {
		this.xmlFile = Paths.get(xmlFileName);
		LocalAccess localAccess = (LocalAccess) DataAccess.getInstance();
		if (!localAccess.existsDeviceXml(xmlFileName)) throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGE0003) + xmlFileName);

		this.settings = Settings.getInstance();

		this.settings.joinXsdThread();

		this.elememt = this.settings.getDeviceSerialization().getTopElement(this.xmlFile.toString(), localAccess);
		this.deviceProps = this.elememt.getValue();
		this.device = this.deviceProps.getDevice();
		this.serialPort = this.deviceProps.getSerialPort();
		this.usbPort = this.deviceProps.getUsbPort();
		this.dataBlock = this.deviceProps.getDataBlock();
		this.state = this.deviceProps.getState();
		this.timeBase = this.deviceProps.getTimeBase();
		this.desktop = this.deviceProps.getDesktop();
		this.isChangePropery = false;

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, this.toString());
	}

	/**
	 * No roaming data access support.
	 * Not threadsafe due to JAXB un-/marshallers.
	 */
	public DeviceConfiguration(String xmlFileName, Unmarshaller tmpUnmarshaller) throws FileNotFoundException, JAXBException {
		this.xmlFile = Paths.get(xmlFileName);
		LocalAccess localAccess = (LocalAccess) DataAccess.getInstance();
		if (!localAccess.existsDeviceXml(xmlFileName)) throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGE0003) + xmlFileName);

		this.settings = Settings.getInstance();

		this.settings.joinXsdThread();

		this.elememt = this.settings.getDeviceSerialization().getTopElement(this.xmlFile, tmpUnmarshaller, localAccess);
		this.deviceProps = this.elememt.getValue();
		this.device = this.deviceProps.getDevice();
		this.serialPort = this.deviceProps.getSerialPort();
		this.usbPort = this.deviceProps.getUsbPort();
		this.dataBlock = this.deviceProps.getDataBlock();
		this.state = this.deviceProps.getState();
		this.timeBase = this.deviceProps.getTimeBase();
		this.desktop = this.deviceProps.getDesktop();
		this.isChangePropery = false;

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, this.toString());
	}

	/**
	 * Shallow copy constructor.
	 * No data access required.
	 */
	public DeviceConfiguration(DeviceConfiguration deviceConfig) {
		this.settings = deviceConfig.settings;
		this.xmlFile = deviceConfig.xmlFile;
		this.fileSha1Hash = deviceConfig.fileSha1Hash;
		this.elememt = deviceConfig.elememt;
		this.deviceProps = deviceConfig.deviceProps;
		this.device = deviceConfig.device;
		this.serialPort = deviceConfig.serialPort;
		this.usbPort = this.deviceProps.getUsbPort();
		this.dataBlock = deviceProps.dataBlock;
		this.state = deviceProps.state;
		this.timeBase = deviceConfig.timeBase;
		this.desktop = deviceProps.desktop;
		this.isChangePropery = deviceConfig.isChangePropery;

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, this.toString());
	}

	/**
	 * Use this for roaming data sources support via the DataAccess class.
	 * Full data access support.
	 * Not threadsafe due to JAXB un-/marshallers.
	 * @param xmlFileSubPath is a relative path based on the roaming folder
	 */
	public DeviceConfiguration(Path xmlFileSubPath, Unmarshaller tmpUnmarshaller, Analyzer analyzer) throws FileNotFoundException, JAXBException {
		this.xmlFile = Paths.get(GDE.APPL_HOME_PATH).resolve(xmlFileSubPath);
		if (!analyzer.getDataAccess().existsDeviceXml(xmlFileSubPath)) { // ok
			throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGE0003) + xmlFileSubPath.toString());
		}
		this.settings = analyzer.getSettings();

		this.settings.joinXsdThread();

		this.elememt = this.settings.getDeviceSerialization().getTopElement(xmlFileSubPath, tmpUnmarshaller, analyzer.getDataAccess());
		this.deviceProps = this.elememt.getValue();
		this.device = this.deviceProps.getDevice();
		this.serialPort = this.deviceProps.getSerialPort();
		this.usbPort = this.deviceProps.getUsbPort();
		this.dataBlock = this.deviceProps.getDataBlock();
		this.state = this.deviceProps.getState();
		this.timeBase = this.deviceProps.getTimeBase();
		this.desktop = this.deviceProps.getDesktop();
		this.isChangePropery = false;

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, this.toString());
	}

	/**
	 * Use this for roaming data sources support via the DataAccess class.
	 * Full data access support.
	 * Not threadsafe due to JAXB un-/marshallers.
	 * @param xmlFileSubPath is a relative path based on the roaming folder
	 */
	public DeviceConfiguration(Path xmlFileSubPath, Analyzer analyzer) throws FileNotFoundException, JAXBException {
		this.xmlFile = Paths.get(GDE.APPL_HOME_PATH).resolve(xmlFileSubPath);
		if (!analyzer.getDataAccess().existsDeviceXml(xmlFileSubPath)) { // ok
			throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGE0003) + xmlFileSubPath.toString());
		}
		this.settings = analyzer.getSettings();

		this.settings.joinXsdThread();

		this.elememt = this.settings.getDeviceSerialization().getTopElement(xmlFileSubPath, analyzer.getDataAccess());
		this.deviceProps = this.elememt.getValue();
		this.device = this.deviceProps.getDevice();
		this.serialPort = this.deviceProps.getSerialPort();
		this.usbPort = this.deviceProps.getUsbPort();
		this.dataBlock = this.deviceProps.getDataBlock();
		this.state = this.deviceProps.getState();
		this.timeBase = this.deviceProps.getTimeBase();
		this.desktop = this.deviceProps.getDesktop();
		this.isChangePropery = false;

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, this.toString());
	}

	/**
	 * writes updated device properties XML
	 */
	public void storeDeviceProperties() {
		if (this.isChangePropery) {
			try {
				this.fileSha1Hash = GDE.STRING_EMPTY;
				this.settings.getDeviceSerialization().marshall(this.elememt, this.xmlFile.toString(), (LocalAccess) DataAccess.getInstance());
			} catch (Throwable t) {
				log.log(Level.SEVERE, t.getMessage(), t);
			}
			this.isChangePropery = false;
		}
	}

	/**
	 * writes device properties XML to given full qualified file name
	 * @param fullQualifiedFileName
	 */
	public void storeDeviceProperties(String fullQualifiedFileName) {
		try {
			this.fileSha1Hash = GDE.STRING_EMPTY;
			this.settings.getDeviceSerialization().marshall(this.elememt, fullQualifiedFileName, (LocalAccess) DataAccess.getInstance());
		} catch (Throwable t) {
			log.log(Level.SEVERE, t.getMessage(), t);
		}
		this.isChangePropery = false;
	}

	/**
	 * get the active device configuration
	 */
	public DeviceConfiguration getDeviceConfiguration() {
		return this;
	}

	/**
	 * @return the device
	 */
	public DeviceType getDeviceType() {
		return this.device;
	}

	/**
	 * @return the serialPort
	 */
	public SerialPortType getSerialPortType() {
		return this.serialPort;
	}

	/**
	 * remove the serialPort of the active device configuration
	 */
	public void removeSerialPortType() {
		this.isChangePropery = true;
		this.serialPort = this.deviceProps.serialPort = null;
	}

	/**
	 * @return the serialPort
	 */
	public UsbPortType getUsbPortType() {
		return this.usbPort;
	}

	/**
	 * remove the serialPort of the active device configuration
	 */
	public void removeUsbPortType() {
		this.isChangePropery = true;
		this.usbPort = this.deviceProps.usbPort = null;
	}

	/**
	 * @return the timeBase
	 */
	public TimeBaseType getTimeBaseType() {
		return this.timeBase;
	}

	public boolean isUsed() {
		return this.device.isUsage();
	}

	public void setUsed(boolean value) {
		this.isChangePropery = true;
		this.device.setUsage(value);
	}

	public String getPropertiesFileName() {
		return this.xmlFile.toAbsolutePath().toString();
	}

	/**
	 * @return the device name
	 */
	public String getName() {
		return this.device.getName().getValue();
	}

	/**
	 * Special directory handling for MC3000 and Q200 supporting battery sets but store data in normal device folder.
	 * @return the device name stripped by the 'set' extension for devices supporting battery sets
	 */
	public String getPureDeviceName(String deviceName) {
		String pureDeviceName = deviceName;
		if (pureDeviceName.endsWith("-Set")) { // MC3000-Set -> MC3000, Q200-Set -> Q200 //$NON-NLS-1$
			pureDeviceName = pureDeviceName.substring(0, pureDeviceName.length() - 4);
		}
		return pureDeviceName;
	}

	/**
	 * @return the device name
	 */
	public String getDeviceImplName() {
		return this.device.getName().getImplementation() == null ? this.device.getName().getValue() : this.device.getName().getImplementation();
	}

	/**
	 * @return the class name with special characters stripped off
	 */
	public String getClassImplName() {
		String deviceImplName = this.getDeviceImplName().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY).replace(GDE.STRING_DASH, GDE.STRING_EMPTY);
		return deviceImplName.contains(GDE.STRING_DOT) ? deviceImplName // full qualified
				: "gde.device." + getManufacturer().toLowerCase().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY).replace(GDE.STRING_DASH, GDE.STRING_EMPTY) + GDE.STRING_DOT + deviceImplName; //$NON-NLS-1$
	}

	/**
	 * Use to convert this instance into a device object by reloading the device properties file.
	 * @return the device instance by using the class loader
	 */
	public IDevice defineInstanceOfDevice() throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
			InvocationTargetException, NoClassDefFoundError {
		String className = this.getClassImplName();
		// String className = "gde.device.DefaultDeviceDialog";
		// log.log(Level.FINE, "loading Class " + className); //$NON-NLS-1$
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Class<?> c = loader.loadClass(className);
		// Class c = Class.forName(className);
		Constructor<?> constructor = c.getDeclaredConstructor(new Class[] { DeviceConfiguration.class });
		// log.log(Level.FINE, "constructor != null -> " + (constructor != null ? "true" : "false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (constructor != null) {
			try {
				return (IDevice) constructor.newInstance(new Object[] { this });
			}
			catch (IllegalArgumentException e) {
				constructor = c.getDeclaredConstructor(new Class[] { String.class });
				if (constructor != null) {
					return (IDevice) constructor.newInstance(new Object[] { this.getPropertiesFileName() });
				} else
					throw new NoClassDefFoundError(Messages.getString(MessageIds.GDE_MSGE0016));
			}
		} else
			throw new NoClassDefFoundError(Messages.getString(MessageIds.GDE_MSGE0016));
	}

	/**
	 * Use to convert this instance into a device object without reloading from the device properties file.
	 * @return the device instance by using the class loader
	 */
	@Nullable
	public IDevice getAsDevice() {
		IDevice tmpDevice = null;
		try {
			Class<?> currentClass = Thread.currentThread().getContextClassLoader().loadClass(this.getClassImplName());
			Constructor<?> constructor = currentClass.getDeclaredConstructor(new Class[] { DeviceConfiguration.class });
			tmpDevice = constructor != null ? (IDevice) constructor.newInstance(new Object[] { this }) : null;
		} catch (Exception e) {
		}
		return tmpDevice;
	}

	/**
	 * @param newDeviceName set a new device name
	 */
	public void setName(String newDeviceName) {
		this.isChangePropery = true;
		this.device.getName().setValue(newDeviceName);
	}

	/**
	 * set a implementation name if it does not match the device name
	 * @param newDeviceImplClass full qualified or the package will be calculated be manufacturer name
	 */
	public void setDeviceImplName(String newDeviceImplClass) {
		this.isChangePropery = true;
		this.device.getName().setImplementation(newDeviceImplClass);
	}

	/**
	 * @return the device name
	 */
	public String getImageFileName() {
		return this.device.getImage().length() > 0 ? this.device.getImage() : "NoDevicePicture.jpg";
	}

	/**
	 * @param newImageFileName set a new image filename(.jpg|.gif|.png)
	 */
	public void setImageFileName(String newImageFileName) {
		this.isChangePropery = true;
		this.device.setImage(newImageFileName);
	}

	public String getManufacturer() {
		return this.device.getManufacturer();
	}

	/**
	 * @param name set a new device manufacture name
	 */
	public void setManufacturer(String name) {
		this.isChangePropery = true;
		this.device.setManufacturer(name);
	}

	public String getManufacturerURL() {
		return this.device.getManufacturerURL();
	}

	/**
	 * @param name set a new manufacture name
	 */
	public void setManufacturerURL(String name) {
		this.isChangePropery = true;
		this.device.setManufacturerURL(name);
	}

	public DeviceTypes getDeviceGroup() {
		return this.device.getGroup();
	}

	/**
	 * set a device group of a device
	 * @param name set a new manufacture name
	 */
	public void setDeviceGroup(DeviceTypes name) {
		this.isChangePropery = true;
		this.device.setGroup(name);
	}

	/**
	 * @return time step in ms
	 */
	public double getTimeStep_ms() {
		return this.timeBase.getTimeStep();
	}

	/**
	 * set new average time step in ms
	 */
	public void setTimeStep_ms(double newTimeStep_ms) {
		this.isChangePropery = true;
		this.timeBase.setTimeStep(newTimeStep_ms);
	}

	/**
	 * @return average time step in ms (this is an optional element, keep this in mind to have a workaround if it does not exist)
	 */
	public Double getAverageTimeStep_ms() {
		return this.timeBase.getAvarageTimeStep();
	}

	/**
	 * set new time step in ms
	 */
	public void setAverageTimeStep_ms(double newAverageTimeStep_ms) {
		this.isChangePropery = true;
		this.timeBase.setAvarageTimeStep(newAverageTimeStep_ms);
	}

	/**
	 * @return UTC delta time in hours
	 */
	public short getUTCdelta() {
		return this.timeBase.getUTCdelta();
	}

	/**
	 * set new UTC delta time in hours
	 */
	public void setUTCdelta(int newUTCdelta) {
		this.isChangePropery = true;
		this.timeBase.setUTCdelta(newUTCdelta);
	}

	/**
	 * @return the port configured for the device, if SerialPortType is not defined in device specific XML a empty string will returned
	 */
	public String getPort() {
		return this.serialPort != null ? this.serialPort.getPort() : this.getUsbPortType() != null ? "USB" : GDE.STRING_EMPTY;
	}

	/**
	 * @return the port configured in SerialPortType
	 */
	public String getPortString() {
		return this.serialPort.getPort();
	}

	public void setPort(String newPort) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setPort(newPort);
	}

	public Integer getBaudeRate() {
		return this.serialPort != null ? this.serialPort.getBaudeRate() : 9800;
	}

	public void setBaudeRate(Integer value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setBaudeRate(value);
	}

	public DataBitsTypes getDataBits() {
		return this.serialPort != null ? this.serialPort.getDataBits() : DataBitsTypes.DATABITS_8;
	}

	public void setDataBits(DataBitsTypes value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setDataBits(value);
	}

	public StopBitsTypes getStopBits() {
		return this.serialPort != null ? this.serialPort.getStopBits() : StopBitsTypes.STOPBITS_1;
	}

	public void setStopBits(StopBitsTypes enumOrdinal) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setStopBits(enumOrdinal);
	}

	public int getFlowCtrlMode() {
		return this.serialPort != null ? this.serialPort.getFlowControlMode() : 0;
	}

	public int getFlowCtrlModeOrdinal() {
		return this.serialPort != null ? this.serialPort.getFlowControlModeOrdinal() : 0;
	}

	public String getFlowCtrlModeString() {
		return this.serialPort.getFlowControlModeString();
	}

	public void setFlowCtrlMode(FlowControlTypes value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setFlowControlMode(value);
	}

	public ParityTypes getParity() {
		return this.serialPort != null ? this.serialPort.getParity() : ParityTypes.PARITY_NONE;
	}

	/**
	 * create a serial port type with default values
	 */
	private void createSerialPort() {
		this.deviceProps.serialPort = this.serialPort = new ObjectFactory().createSerialPortType();
		this.serialPort.setPort(GDE.STRING_EMPTY);
		this.serialPort.setBaudeRate(9600);
		this.serialPort.setParity(ParityTypes.PARITY_NONE);
		this.serialPort.setDataBits(DataBitsTypes.DATABITS_8);
		this.serialPort.setStopBits(StopBitsTypes.STOPBITS_1);
		this.serialPort.setFlowControlMode(FlowControlTypes.FLOWCONTROL_NONE);
		this.serialPort.setIsDTR(false);
		this.serialPort.setIsRTS(false);
		this.isChangePropery = true;
	}

	public void setParity(ParityTypes value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setParity(value);
	}

	public boolean isDTR() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.isIsDTR();
	}

	public void setIsDTR(boolean value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setIsDTR(value);
	}

	public boolean isRTS() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.isIsRTS();
	}

	public void setIsRTS(boolean value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setIsRTS(value);
	}

	public int getReadTimeOut() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getTimeOut() != null ? this.serialPort.getTimeOut().getReadTimeOut() : 0;
	}

	public void setReadTimeOut(int value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		if (this.serialPort.getTimeOut() == null) {
			this.serialPort.setTimeOut(new ObjectFactory().createTimeOutType());
		}
		this.serialPort.getTimeOut().setReadTimeOut(value);
	}

	public int getReadStableIndex() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getTimeOut() != null ? this.serialPort.getTimeOut().getReadStableIndex() : 0;
	}

	public void setReadStableIndex(int value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		if (this.serialPort.getTimeOut() == null) {
			this.serialPort.setTimeOut(new ObjectFactory().createTimeOutType());
		}
		this.serialPort.getTimeOut().setReadStableIndex(value);
	}

	public int getWriteCharDelayTime() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getTimeOut() != null ? this.serialPort.getTimeOut().getWriteCharDelayTime() : 0;
	}

	public void setWriteCharDelayTime(int value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		if (this.serialPort.getTimeOut() == null) {
			this.serialPort.setTimeOut(new ObjectFactory().createTimeOutType());
		}
		this.serialPort.getTimeOut().setWriteCharDelayTime(value);
	}

	public int getWriteDelayTime() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getTimeOut() != null ? this.serialPort.getTimeOut().getWriteDelayTime() : 0;
	}

	public void setWriteDelayTime(int value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		if (this.serialPort.getTimeOut() == null) {
			this.serialPort.setTimeOut(new ObjectFactory().createTimeOutType());
		}
		this.serialPort.getTimeOut().setWriteDelayTime(value);
	}

	public void removeSerialPortTimeOut() {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		if (this.serialPort.getTimeOut() != null) {
			this.serialPort.setTimeOut(null);
		}
	}
	
	/**
	 * @return true if there is a request byte array defined
	 */
	public boolean isSerialPortRequest() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getRequest() != null && this.serialPort.getRequest().length > 0;
	}
	
	/**
	 * @return byte array to write prior to read from serial port
	 */
	public byte[] getSerialPortRequest() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getRequest();
	}
	
	/**
	 * set a new byte arra as serial port request
	 * @param newRequest
	 */
	public void setSerialPortRequest(final byte[] newRequest) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setRequest(newRequest);
	}

	public UsbInterfaceType getUsbInterfaceType() {
		return this.usbPort.getUsbInterface();
	}

	/**
	 * @return the vendor ID of the USB port to be used for device communication
	 */
	public short getUsbVendorId() {
		return Short.valueOf(this.usbPort.getVendorId().substring(2), 16);
	}

	/**
	 * @return the product ID of the device to be used for communication
	 */
	public short getUsbProductId() {
		return Short.valueOf(this.usbPort.getProductId().substring(2), 16);
	}

	/**
	 * @return the product string of the device to be used for communication
	 */
	public String getUsbProductString() {
		return this.usbPort.getProductString();
	}

	/**
	 * @return the interface address to be used for communication
	 */
	public byte getUsbInterface() {
		return Byte.valueOf(this.usbPort.getUsbInterface().getInterface().getValue().substring(2), 16);
	}

	/**
	 * @return the end point address of the interface to be used for write communication
	 */
	public byte getUsbEndpointIn() {
		return Byte.valueOf(this.usbPort.getUsbInterface().getEndPointIn().substring(2), 16);
	}

	/**
	 * @return the end point address of the interface to be used for read communication
	 */
	public byte getUsbEndpointOut() {
		return Short.valueOf(this.usbPort.getUsbInterface().getEndPointOut().substring(2), 16).byteValue();
	}

	/**
	 * set a new desktop type
	 * @param newDesktopType
	 */
	public void setDesktopType(DesktopType newDesktopType) {
		this.deviceProps.setDesktop(newDesktopType);
		this.desktop = this.deviceProps.desktop;
		this.isChangePropery = true;
	}

	/**
	 * get the desktop type
	 * @return DesktopType
	 */
	public DesktopType getDesktopType() {
		return this.desktop;
	}

	/**
	 * method to query desktop properties, like: table tab switched of, ...
	 * @param dektopType
	 * @return property of the queried type or null if not defined
	 */
	public DesktopPropertyType getDesktopProperty(DesktopPropertyTypes dektopType) {
		DesktopPropertyType property = null;
		if (this.desktop != null) {
			List<DesktopPropertyType> properties = this.desktop.getProperty();
			for (DesktopPropertyType propertyType : properties) {
				if (propertyType.getName().equals(dektopType)) {
					property = propertyType;
					break;
				}
			}
			if (property == null) {
				property = new ObjectFactory().createDesktopPropertyType();
				property.setName(dektopType);
				property.setValue(false);
				property.setDescription(dektopType.name());
				if (dektopType.equals(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB)) {
					property.setTargetReferenceOrdinal(0);
				}
				properties.add(property);
			}
		}
		return property;
	}

	/**
	 * set the desktop type value
	 * @param desktopType
	 * @param newValue
	 */
	public void setDesktopTypeValue(DesktopPropertyTypes desktopType, Boolean newValue) {
		this.getDesktopProperty(desktopType).setValue(newValue);
		this.isChangePropery = true;
	}

	/**
	 * @return size of mode states
	 */
	public int getStateSize() {
		return this.state.property.size();
	}

	/**
	 * @return actual StateType
	 */
	public StateType getStateType() {
		return this.deviceProps.state;
	}

	/**
	 * remove optional mode state
	 */
	public void removeStateType() {
		this.isChangePropery = true;
		this.state = this.deviceProps.state = null;
	}

	/**
	 * append a new mode state type property
	 * @param newStateProperty
	 */
	public void appendStateType(PropertyType newStateProperty) {
		this.isChangePropery = true;
		if (this.deviceProps.state == null) {
			this.deviceProps.state = new ObjectFactory().createStateType();
		}
		this.deviceProps.state.getProperty().add(newStateProperty);
		this.isChangePropery = true;
	}

	/**
	 * remove a mode state type property
	 * @param removeStateProperty
	 */
	public void removeStateType(PropertyType removeStateProperty) {
		this.isChangePropery = true;
		this.deviceProps.state.remove(removeStateProperty);
	}

	/**
	 * set a new mode state name
	 * @param modeStateOrdinal
	 * @param newName
	 */
	public void setStateName(int modeStateOrdinal, String newName) {
		this.isChangePropery = true;
		PropertyType tmpPoperty = this.getStateProperty(modeStateOrdinal);
		if (tmpPoperty != null) {
			tmpPoperty.setName(newName);
		}
	}

	/**
	 * set a new mode state value
	 * @param modeStateOrdinal
	 * @param newValue
	 */
	public void setStateValue(int modeStateOrdinal, String newValue) {
		this.isChangePropery = true;
		PropertyType tmpPoperty = this.getStateProperty(modeStateOrdinal);
		if (tmpPoperty != null) {
			tmpPoperty.setValue(StringHelper.verifyTypedInput(tmpPoperty.getType(), newValue));
		}
	}

	/**
	 * set a new mode state description
	 * @param modeStateOrdinal
	 * @param newDescription
	 */
	public void setStateDescription(int modeStateOrdinal, String newDescription) {
		this.isChangePropery = true;
		PropertyType tmpPoperty = this.getStateProperty(modeStateOrdinal);
		if (tmpPoperty != null) {
			tmpPoperty.setDescription(newDescription);
		}
	}

	/**
	 * method to query desktop properties, like: table tab switched of, ...
	 * @param modeStateOrdinal
	 * @return property of the queried type or null if not defined
	 */
	public PropertyType getStateProperty(int modeStateOrdinal) {
		PropertyType property = null;
		if (this.state != null) {
			List<PropertyType> properties = this.state.getProperty();
			for (PropertyType propertyType : properties) {
				try {
					int propertyValue = Integer.parseInt(propertyType.getValue());
					if (propertyValue == modeStateOrdinal) {
						property = propertyType;
						break;
					}
				} catch (NumberFormatException e) {
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
		return property;
	}

	public DataBlockType getDataBlockType() {
		return this.dataBlock;
	}

	public void removeDataBlockType() {
		this.isChangePropery = true;
		this.dataBlock = this.deviceProps.dataBlock = null;
	}

	public void addDataBlockType() {
		this.isChangePropery = true;
		this.dataBlock = this.deviceProps.dataBlock = new DataBlockType();
	}

	/**
	 * there are two data block format input types,
	 * - FILE_IO, where the data comes in most cases in character form
	 * - SERIAL_IO, where the data are received through serial connection in bytes
	 * @param inputType the input type to query the input size
	 * @return
	 */
	public int getDataBlockSize(InputTypes inputType) {
		int dataBlockSize = -1;
		for (DataBlockType.Format format : this.dataBlock.getFormat()) {
			if (format.getInputType() == inputType) dataBlockSize = format.getSize();
		}
		return dataBlockSize;
	}

	/**
	 * there are two data block format types,
	 * - VALUE, where the data comes in most cases in character form with file I/O and defines the number of values, example CSV file input
	 * - BYTE, where the data are received through serial connection in bytes
	 * @param formatType the format to query the input size
	 * @return
	 */
	public int getDataBlockSize(FormatTypes formatType) {
		int dataBlockSize = -1;
		try {
			for (DataBlockType.Format format : this.dataBlock.getFormat()) {
				if (format.getType() == formatType) dataBlockSize = format.getSize();
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE0052, new String[] {
					this.xmlFile.getFileName().toString() }));
		}
		return dataBlockSize;
	}

	/**
	 * set the size of a data block of given format type and input type
	 * @param useInputType
	 * @param useFormat
	 * @param newSize
	 */
	public void setDataBlockSize(InputTypes useInputType, FormatTypes useFormat, Integer newSize) {
		this.isChangePropery = true;
		if (this.dataBlock == null) {
			this.dataBlock = this.deviceProps.dataBlock = new DataBlockType();
			DataBlockType.Format format = new DataBlockType.Format();
			format.setFormatType(useFormat);
			format.setInputType(useInputType);
			format.setSize(newSize);
			this.dataBlock.getFormat().add(format);
		} else {
			boolean isSizeSet = false;
			for (DataBlockType.Format format : this.dataBlock.getFormat()) {
				if (!isSizeSet && format.getType() == useFormat) {
					format.setInputType(useInputType);
					format.setSize(newSize);
					isSizeSet = true;
					break;
				}
			}
			if (!isSizeSet && this.dataBlock.getFormat().size() == 1) {
				DataBlockType.Format format = new DataBlockType.Format();
				format.setFormatType(useFormat);
				format.setInputType(useInputType);
				format.setSize(newSize);
				this.dataBlock.getFormat().add(format);
			}
		}
	}

	/**
	 * query the FormatTypes type according to the input format type
	 * @param inputType
	 * @return
	 */
	public FormatTypes getDataBlockFormat(InputTypes inputType) {
		FormatTypes dataBlockformat = inputType == InputTypes.FILE_IO ? FormatTypes.VALUE : inputType == InputTypes.SERIAL_IO ? FormatTypes.BYTE
				: FormatTypes.BINARY;
		for (DataBlockType.Format format : this.dataBlock.getFormat()) {
			if (format.getInputType() == inputType) dataBlockformat = format.getType();
		}
		return dataBlockformat;
	}

	/**
	 * set the data block format specifying input type and format type
	 * @param inputType
	 * @param value
	 */
	public void setDataBlockFormat(InputTypes inputType, FormatTypes value) {
		this.isChangePropery = true;
		if (this.dataBlock == null) {
			this.dataBlock = this.deviceProps.dataBlock = new DataBlockType();
			DataBlockType.Format format = new DataBlockType.Format();
			format.setFormatType(value);
			format.setInputType(inputType);
			format.setSize(-1);
			this.dataBlock.getFormat().add(format);
		} else {
			boolean isSizeSet = false;
			for (DataBlockType.Format format : this.dataBlock.getFormat()) {
				if (!isSizeSet && format.getType() == value) {
					format.setInputType(inputType);
					format.setSize(-1);
					isSizeSet = true;
					break;
				}
			}
			if (!isSizeSet && this.dataBlock.getFormat().size() == 1) {
				DataBlockType.Format format = new DataBlockType.Format();
				format.setFormatType(value);
				format.setInputType(inputType);
				format.setSize(-1);
				this.dataBlock.getFormat().add(format);
			}
		}
	}

	public boolean isDataBlockCheckSumDefined() {
		return this.dataBlock != null && this.dataBlock.getCheckSum() != null && (this.dataBlock.getCheckSum() != null && this.dataBlock.getCheckSum().getFormat() != null);
	}

	public CheckSumTypes getDataBlockCheckSumType() {
		return this.dataBlock != null && this.dataBlock.getCheckSum() != null ? this.dataBlock.getCheckSum().getType() : null;
	}

	public void setDataBlockCheckSumType(CheckSumTypes value) {
		this.isChangePropery = true;
		if (this.dataBlock == null) {
			this.deviceProps.dataBlock = this.dataBlock = new ObjectFactory().createDataBlockType();
		}
		if (value == null)
			this.dataBlock.setCheckSum(null);
		else {
			if (this.dataBlock.getCheckSum() == null) this.dataBlock.setCheckSum(new DataBlockType.CheckSum());
			this.dataBlock.getCheckSum().setType(value);
		}
	}

	public FormatTypes getDataBlockCheckSumFormat() {
		return this.dataBlock != null && this.dataBlock.getCheckSum() != null ? this.dataBlock.getCheckSum().getFormat() : FormatTypes.BINARY;
	}

	public void setDataBlockCheckSumFormat(FormatTypes value) {
		this.isChangePropery = true;
		if (this.dataBlock == null) {
			this.deviceProps.dataBlock = this.dataBlock = new ObjectFactory().createDataBlockType();
		}
		if (value == null)
			this.dataBlock.setCheckSum(null);
		else {
			if (this.dataBlock.getCheckSum() == null) this.dataBlock.setCheckSum(new DataBlockType.CheckSum());
			this.dataBlock.getCheckSum().setFormat(value);
		}
	}

	public String getDataBlockLeader() {
		return this.dataBlock != null ? this.dataBlock.getLeader() : "$"; //$NON-NLS-1$
	}

	public void setDataBlockLeader(String value) {
		this.isChangePropery = true;
		if (value == null)
			this.dataBlock.setLeader(null);
		else
			this.dataBlock.setLeader(value);
	}

	public byte[] getDataBlockEnding() {
		return this.dataBlock != null ? this.dataBlock.getTrailer() : new HexBinaryAdapter().unmarshal("0D0A"); //$NON-NLS-1$
	}

	public String getDataBlockEndingLineEndingType() {
		return this.dataBlock != null ? LineEndingTypes.valueFrom(this.dataBlock.getTrailer()) : LineEndingTypes.CRLF.value();
	}

	public void setDataBlockEnding(String value) {
		this.isChangePropery = true;
		if (this.dataBlock == null) {
			this.deviceProps.dataBlock = this.dataBlock = new ObjectFactory().createDataBlockType();
		}
		if (value == null)
			this.dataBlock.setTrailer(null);
		else
			this.dataBlock.setTrailer(LineEndingTypes.bytesFromValue(value));
	}

	public TimeUnitTypes getDataBlockTimeUnit() {
		return this.dataBlock != null ? this.dataBlock.getTimeUnit() : TimeUnitTypes.MSEC;
	}

	public void setDataBlockTimeUnit(TimeUnitTypes value) {
		this.isChangePropery = true;
		if (this.dataBlock == null) {
			this.deviceProps.dataBlock = this.dataBlock = new ObjectFactory().createDataBlockType();
		}
		this.dataBlock.setTimeUnit(value);
	}

	public int getDataBlockTimeUnitFactor() {
		return this.dataBlock != null && this.dataBlock.getTimeUnit() == null ? 1000
				: this.dataBlock != null && this.dataBlock.getTimeUnit().equals(TimeUnitTypes.MSEC) ? 1 : 1000;
	}

	public CommaSeparatorTypes getDataBlockSeparator() {
		return this.dataBlock != null && this.dataBlock.getSeparator() != null ? this.dataBlock.getSeparator() : CommaSeparatorTypes.SEMICOLON;
	}

	public void setDataBlockSeparator(CommaSeparatorTypes value) {
		this.isChangePropery = true;
		if (this.dataBlock == null) {
			this.deviceProps.dataBlock = this.dataBlock = new ObjectFactory().createDataBlockType();
		}
		this.dataBlock.setSeparator(value);
	}

	public String getDataBlockPreferredDataLocation() {
		return this.dataBlock != null ? (this.dataBlock.getPreferredDataLocation() != null && this.dataBlock.getPreferredDataLocation().length() != 0
				? this.dataBlock.getPreferredDataLocation() : this.settings.getDataFilePath()) : this.settings.getDataFilePath();
	}

	public void setDataBlockPreferredDataLocation(String value) {
		this.isChangePropery = true;
		if (this.dataBlock == null) {
			this.deviceProps.dataBlock = this.dataBlock = new ObjectFactory().createDataBlockType();
		}
		this.dataBlock.setPreferredDataLocation(value != null ? value.trim() : GDE.STRING_EMPTY);
	}

	public boolean isDataBlockPreferredFileExtentionDefined() {
		return this.dataBlock != null && this.dataBlock.preferredFileExtention != null && this.dataBlock.preferredFileExtention.length() > 3;
	}

	public String getDataBlockPreferredFileExtention() {
		try {
			return this.dataBlock.getPreferredFileExtention();
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE0052, new String[] {
					this.xmlFile.getFileName().toString() }));
		}
		return "*.*";
	}

	public void setDataBlockPreferredFileExtention(String value) {
		boolean isValidExt = this.isChangePropery = true;
		if (this.dataBlock == null) {
			this.deviceProps.dataBlock = this.dataBlock = new ObjectFactory().createDataBlockType();
		}
		if (value != null) {
			isValidExt = (value = value.replace(GDE.STRING_BLANK, GDE.STRING_EMPTY).replace(GDE.STRING_STAR, GDE.STRING_EMPTY).replace(GDE.STRING_DOT, GDE.STRING_EMPTY).trim()).length() >= 1;
			if (!isValidExt) {
				this.dataBlock.setPreferredFileExtention(null);
			} else {
				value = "*." + value; //$NON-NLS-1$
			}
		}
		this.dataBlock.setPreferredFileExtention(value != null && isValidExt ? value : this.dataBlock.getPreferredFileExtention());
	}

	/**
	 * query if the table tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isTableTabRequested() {
		DesktopPropertyType property = this.getDesktopProperty(DesktopPropertyTypes.TABLE_TAB);
		return Boolean.valueOf(property != null ? property.isValue() : Boolean.FALSE);
	}

	/**
	 * set the DesktopType.TYPE_TABLE_TAB property to the given value
	 * @param enable
	 */
	public void setTableTabRequested(boolean enable) {
		DesktopPropertyType property = this.getDesktopProperty(DesktopPropertyTypes.TABLE_TAB);
		if (property == null) {
			createDesktopProperty(DesktopPropertyTypes.TABLE_TAB.name(), enable);
		} else {
			property.setValue(enable);
		}
		this.isChangePropery = true;
	}

	/**
	 * query if the digital tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isDigitalTabRequested() {
		DesktopPropertyType property = this.getDesktopProperty(DesktopPropertyTypes.DIGITAL_TAB);
		return Boolean.valueOf(property != null ? property.isValue() : Boolean.FALSE);
	}

	/**
	 * set the DesktopType.TYPE_DIGITAL_TAB property to the given value
	 * @param enable
	 */
	public void setDigitalTabRequested(boolean enable) {
		DesktopPropertyType property = this.getDesktopProperty(DesktopPropertyTypes.DIGITAL_TAB);
		if (property == null) {
			createDesktopProperty(DesktopPropertyTypes.DIGITAL_TAB.name(), enable);
		} else {
			property.setValue(enable);
		}
		this.isChangePropery = true;
	}

	/**
	 * query if the analog tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isAnalogTabRequested() {
		DesktopPropertyType property = this.getDesktopProperty(DesktopPropertyTypes.ANALOG_TAB);
		return Boolean.valueOf(property != null ? property.isValue() : Boolean.FALSE);
	}

	/**
	 * set the DesktopType.TYPE_ANALOG_TAB property to the given value
	 * @param enable
	 */
	public void setAnalogTabRequested(boolean enable) {
		DesktopPropertyType property = this.getDesktopProperty(DesktopPropertyTypes.ANALOG_TAB);
		if (property == null) {
			createDesktopProperty(DesktopPropertyTypes.ANALOG_TAB.name(), enable);
		} else {
			property.setValue(enable);
		}
		this.isChangePropery = true;
	}

	/**
	 * query if the voltage per cell tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isVoltagePerCellTabRequested() {
		DesktopPropertyType property = this.getDesktopProperty(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB);
		return Boolean.valueOf(property != null ? property.isValue() : Boolean.FALSE);
	}

	/**
	 * set the DesktopType.TYPE_VOLTAGE_PER_CELL_TAB property to the given value
	 * @param enable
	 */
	@Deprecated
	public void setVoltagePerCellTabRequested(boolean enable) {
		DesktopPropertyType property = this.getDesktopProperty(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB);
		if (property == null) {
			createDesktopProperty(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB.name(), enable);
		} else {
			property.setValue(enable);
		}
		this.isChangePropery = true;
	}

	/**
	 * query if the utility graphics tabulator should be displayed and updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isUtilityGraphicsTabRequested() {
		DesktopPropertyType property = this.getDesktopProperty(DesktopPropertyTypes.UTILITY_GRAPHICS_TAB);
		return Boolean.valueOf(property != null ? property.isValue() : Boolean.FALSE);
	}

	/**
	 * query if the utility device tabulator should be displayed and updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isUtilityDeviceTabRequested() {
		DesktopPropertyType property = this.getDesktopProperty(DesktopPropertyTypes.UTILITY_DEVICE_TAB);
		return Boolean.valueOf(property != null ? property.isValue() : Boolean.FALSE);
	}

	/**
	 * query if the target measurement reference ordinal used by the given desktop type
	 * @return the target measurement reference ordinal, -1 if reference ordinal not set
	 */
	public int getDesktopTargetReferenceOrdinal(DesktopPropertyTypes desktopPropertyType) {
		DesktopPropertyType property = this.getDesktopProperty(desktopPropertyType);
		return property != null ? property.getTargetReferenceOrdinal() != null ? property.getTargetReferenceOrdinal() : -1 : -1;
	}

	/**
	 * set a new desktop type description
	 * @param desktopType
	 * @param newDescription
	 */
	public void setDesktopTypeDesription(DesktopPropertyTypes desktopType, String newDescription) {
		this.getDesktopProperty(desktopType).setDescription(newDescription);
		this.isChangePropery = true;
	}

	/**
	 * @return the channel count
	 */
	public int getChannelCount() {
		return this.deviceProps.getChannels().channel.size();
	}

	/**
	 * @return the channel name
	 */
	public String getChannelName(int channelNumber) {
		return this.deviceProps.getChannels().channel.get(channelNumber - 1).getName();
	}

	/**
	 * @return the channel replacement name
	 */
	public String getChannelNameReplacement(int channelNumber) {
		return xmlResource.getReplacement(this.deviceProps.getChannels().channel.get(channelNumber - 1).getName());
	}

	/**
	 * @param channelConfigName - size should not exceed 15 char length
	 * @param channelNumber
	 */
	public void setChannelName(String channelConfigName, int channelNumber) {
		this.isChangePropery = true;
		this.deviceProps.getChannels().channel.get(channelNumber - 1).setName(channelConfigName);
	}

	/**
	 * @return the channel type by given channel number
	 */
	public ChannelType getChannelType(int channelConfigNumber) {
		return this.deviceProps.getChannels().channel.get(channelConfigNumber - 1);
	}

	/**
	 * @return the channel types by given channel number
	 */
	public ChannelTypes getChannelTypes(int channelConfigNumber) {
		return this.deviceProps.getChannels().channel.get(channelConfigNumber - 1).getType();
	}

	/**
	 * @return the channel types by given channel configuration key (name)
	 */
	@Deprecated
	public ChannelTypes getChannelTypes(String channelConfigKey) {
		return this.getChannel(channelConfigKey).getType();
	}

	/**
	 * set a new channel type using a channel number
	 * @param newChannleType the channel type by given channel number
	 * @param channelNumber
	 */
	public void setChannelTypes(ChannelTypes newChannleType, int channelNumber) {
		this.isChangePropery = true;
		this.deviceProps.getChannels().channel.get(channelNumber - 1).setType(newChannleType);
	}

	/**
	 * add a new channel/config type
	 * @param newChannelType
	 */
	public void addChannelType(ChannelType newChannelType) {
		this.isChangePropery = true;
		this.deviceProps.getChannels().channel.add(newChannelType);
	}

	/**
	 * remove a channel/configuration type at index
	 * @param channelNumber
	 */
	public void removeChannelType(int channelNumber) {
		this.isChangePropery = true;
		this.deviceProps.getChannels().channel.remove(channelNumber - 1);
	}

	/**
	 * @return the channel measurements by given channel configuration key (name)
	 */
	public List<MeasurementType> getChannelMeasuremts(int channelConfigNumber) {
		return this.getChannel(channelConfigNumber).getMeasurement();
	}

	/**
	 * @return the channel measurements by given channel configuration key (name)
	 */
	@Deprecated
	public List<MeasurementType> getChannelMeasuremts(String channelConfigKey) {
		return this.getChannel(channelConfigKey).getMeasurement();
	}

	/**
	 * @return the channel measurements with replaced name by given channel configuration number as clone, real changes needs to be triggered separate
	 */
	public List<MeasurementType> getChannelMeasuremtsReplacedNames(int channelConfigNumber) {
		List<MeasurementType> tmpMeasurements = this.getChannel(channelConfigNumber).getMeasurement();
		List<MeasurementType> cpMeasurements = new ArrayList<MeasurementType>();
		Iterator<MeasurementType> ite = tmpMeasurements.iterator();
		while (ite.hasNext()) {
			MeasurementType measurementType = ite.next().clone();
			measurementType.name = xmlResource.getReplacement(measurementType.name);
			cpMeasurements.add(measurementType);
		}
		return cpMeasurements;
	}

	/**
	 * @return the number of measurements of a channel by given channel number
	 */
	public int getNumberOfMeasurements(int channelConfigNumber) {
		return this.getChannel(channelConfigNumber) != null ? this.getChannel(channelConfigNumber).getMeasurement().size() : 0;
	}

	/**
	 * @return the number of measurements of a channel by given channel configuration key (name)
	 */
	@Deprecated
	public int getNumberOfMeasurements(String channelConfigKey) {
		return this.getChannel(channelConfigKey).getMeasurement().size();
	}

	/**
	 * add (append) a new MeasurementType object to channel with channel number as given
	 * @param channelNumber
	 * @param newMeasurementType
	 */
	public void addMeasurement2Channel(int channelNumber, MeasurementType newMeasurementType) {
		this.isChangePropery = true;
		this.getChannel(channelNumber).getMeasurement().add(newMeasurementType);
	}

	/**
	 * add (append) a new MeasurementType object to channel with channel/config key as given
	 * @param channelConfigKey
	 * @param newMeasurementType
	 */
	@Deprecated
	public void addMeasurement2Channel(String channelConfigKey, MeasurementType newMeasurementType) {
		this.isChangePropery = true;
		this.getChannel(channelConfigKey).getMeasurement().add(newMeasurementType);
	}

	/**
	 * remove a MeasurementType object from channel with channel number as given
	 * @param channelConfigNumber
	 * @param removeMeasurementType
	 */
	public void removeMeasurementFromChannel(int channelConfigNumber, MeasurementType removeMeasurementType) {
		this.isChangePropery = true;
		this.getChannel(channelConfigNumber).getMeasurement().remove(removeMeasurementType);
	}

	/**
	 * remove a MeasurementType object from channel with channel/config key as given
	 * @param channelConfigKey
	 * @param newMeasurementType
	 */
	@Deprecated
	public void removeMeasurementFromChannel(String channelConfigKey, MeasurementType newMeasurementType) {
		this.isChangePropery = true;
		this.getChannel(channelConfigKey).getMeasurement().remove(newMeasurementType);
	}

	/**
	 * get the channel type by given channel configuration key (name)
	 * @param channelConfigNumber
	 * @return the channel type
	 */
	public ChannelType getChannel(int channelConfigNumber) {
		return this.deviceProps.getChannels().channel.size() >= channelConfigNumber ? this.deviceProps.getChannels().channel.get(channelConfigNumber - 1)
				: null;
	}

	/**
	 * get the channel type by given channel configuration key (name)
	 * @param channelConfigKey
	 * @return the channel type
	 */
	@Deprecated
	public ChannelType getChannel(String channelConfigKey) {
		ChannelType channel = null;
		for (ChannelType c : this.deviceProps.getChannels().channel) {
			if (c.getName().trim().startsWith(channelConfigKey)) {
				channel = c;
				break;
			}
		}
		return channel;
	}

	/**
	 * set active status of an measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param isActive
	 */
	public void setMeasurementActive(int channelConfigNumber, int measurementOrdinal, boolean isActive) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "channelConfigNumber = \"" + channelConfigNumber + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigNumber)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigNumber, measurementOrdinal).setActive(isActive);
	}

	/**
	 * set active status of an measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param isActive
	 */
	@Deprecated
	public void setMeasurementActive(String channelConfigKey, int measurementOrdinal, boolean isActive) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementOrdinal).setActive(isActive);
	}

	/**
	 * get the measurement to get/set measurement specific parameter/properties
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return MeasurementType
	 */
	public MeasurementType getMeasurement(int channelConfigNumber, int measurementOrdinal) {
		if (this.deviceProps.getChannels().channel.size() >= channelConfigNumber) {
			try {
				MeasurementType measurement = this.getChannel(channelConfigNumber).getMeasurement().get(measurementOrdinal);
				if (measurement != null) return measurement;
			} catch (IndexOutOfBoundsException e) {
				MeasurementType newMeasurement = this.getChannel(channelConfigNumber).getMeasurement().get(0).clone(); // this will clone statistics and
																																																								// properties as well
				newMeasurement.setName("tmpMeasurement" + measurementOrdinal);
				this.addMeasurement2Channel(channelConfigNumber, newMeasurement);
				this.isChangePropery = true;
			}
		}
		return this.deviceProps.getChannels().channel.size() >= channelConfigNumber
				? this.getChannel(channelConfigNumber).getMeasurement().get(measurementOrdinal) : this.getChannel(1).getMeasurement().get(measurementOrdinal);
	}

	/**
	 * get the measurement to get/set measurement specific parameter/properties
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return MeasurementType
	 */
	@Deprecated
	public MeasurementType getMeasurement(String channelConfigKey, int measurementOrdinal) {
		MeasurementType measurement = null;
		try {
			String tmpMeasurementKey = this.getMeasurementNames(channelConfigKey)[measurementOrdinal];
			for (MeasurementType meas : this.getChannel(channelConfigKey).getMeasurement()) {
				if (meas.getName().equals(tmpMeasurementKey)) {
					measurement = meas;
					break;
				}
			}
		} catch (RuntimeException e) {
			log.log(Level.SEVERE, channelConfigKey + " - " + this.getMeasurementNames(channelConfigKey)[measurementOrdinal], e); //$NON-NLS-1$
		}
		return measurement;
	}

	/**
	 * get the properties from a channel/configuration and record key name
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return List of properties to according measurement
	 */
	public List<PropertyType> getProperties(int channelConfigNumber, int measurementOrdinal) {
		List<PropertyType> list = new ArrayList<PropertyType>();
		MeasurementType measurement = this.getMeasurement(channelConfigNumber, measurementOrdinal);
		if (measurement != null) list = measurement.getProperty();
		return list;
	}

	/**
	 * get the properties from a channel/configuration and record key name
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return List of properties to according measurement
	 */
	@Deprecated
	public List<PropertyType> getProperties(String channelConfigKey, int measurementOrdinal) {
		List<PropertyType> list = new ArrayList<PropertyType>();
		MeasurementType measurement = this.getMeasurement(channelConfigKey, measurementOrdinal);
		if (measurement != null) list = measurement.getProperty();
		return list;
	}

	/**
	 * add a property to a given list of PropertyTypes
	 * @param properties
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	public static void addProperty(List<PropertyType> properties, String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue(GDE.STRING_EMPTY + value);
		properties.add(newProperty);
	}

	/**
	 * get name of specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 */
	public String getMeasurementName(int channelConfigNumber, int measurementOrdinal) {
		return this.getMeasurement(channelConfigNumber, measurementOrdinal).getName();
	}

	/**
	 * get replacement name of specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 */
	public String getMeasurementNameReplacement(int channelConfigNumber, int measurementOrdinal) {
		return xmlResource.getReplacement(this.getMeasurement(channelConfigNumber, measurementOrdinal).getName());
	}

	/**
	 * set new name of specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param name
	 */
	public void setMeasurementName(int channelConfigNumber, int measurementOrdinal, String name) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "channelConfigNumber = \"" + channelConfigNumber + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigNumber)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigNumber, measurementOrdinal).setName(name);
	}

	/**
	 * set new name of specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param name
	 */
	@Deprecated
	public void setMeasurementName(String channelConfigKey, int measurementOrdinal, String name) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementOrdinal).setName(name);
	}

	/**
	 * method to query the unit of measurement data unit by a given record key
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return dataUnit as string
	 */
	public String getMeasurementUnit(int channelConfigNumber, int measurementOrdinal) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "channelConfigNumber = \"" + channelConfigNumber + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigNumber)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return this.getMeasurement(channelConfigNumber, measurementOrdinal).getUnit();
	}

	/**
	 * method to query the unit of measurement data unit by a given record key
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return dataUnit as string
	 */
	@Deprecated
	public String getMeasurementUnit(String channelConfigKey, int measurementOrdinal) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return this.getMeasurement(channelConfigKey, measurementOrdinal).getUnit();
	}

	/**
	 * method to set the unit of measurement by a given measurement key
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param unit
	 */
	public void setMeasurementUnit(int channelConfigNumber, int measurementOrdinal, String unit) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "channelConfigNumber = \"" + channelConfigNumber + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigNumber)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigNumber, measurementOrdinal).setUnit(unit);
	}

	/**
	 * method to set the unit of measurement by a given measurement key
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param unit
	 */
	@Deprecated
	public void setMeasurementUnit(String channelConfigKey, int measurementOrdinal, String unit) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementOrdinal).setUnit(unit);
	}

	/**
	 * get the symbol of specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return the measurement symbol as string
	 */
	public String getMeasurementSymbol(int channelConfigNumber, int measurementOrdinal) {
		return this.getMeasurement(channelConfigNumber, measurementOrdinal).getSymbol();
	}

	/**
	 * get the symbol of specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the measurement symbol as string
	 */
	@Deprecated
	public String getMeasurementSymbol(String channelConfigKey, int measurementOrdinal) {
		return this.getMeasurement(channelConfigKey, measurementOrdinal).getSymbol();
	}

	/**
	 * set new symbol of specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param symbol
	 */
	public void setMeasurementSymbol(int channelConfigNumber, int measurementOrdinal, String symbol) {
		this.isChangePropery = true;
		this.getMeasurement(channelConfigNumber, measurementOrdinal).setSymbol(symbol);
	}

	/**
	 * set new symbol of specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param symbol
	 */
	@Deprecated
	public void setMeasurementSymbol(String channelConfigKey, int measurementOrdinal, String symbol) {
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementOrdinal).setSymbol(symbol);
	}

	/**
	 * get replacement name of specified measurement label
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 */
	public String getMeasurementLabelReplacement(int channelConfigNumber, int measurementOrdinal) {
		final String key = this.getMeasurement(channelConfigNumber, measurementOrdinal).getLabel();
		return key != null ? xmlResource.getReplacement(key) : "";
	}

	/**
	 * get the statistics type of the specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return statistics, if statistics does not exist return null
	 */
	public StatisticsType getMeasurementStatistic(int channelConfigNumber, int measurementOrdinal) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "get statistics type from measurement = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName()); //$NON-NLS-1$
		return this.getMeasurement(channelConfigNumber, measurementOrdinal).getStatistics();
	}

	/**
	 * get the statistics type of the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return statistics, if statistics does not exist return null
	 */
	@Deprecated
	public StatisticsType getMeasurementStatistic(String channelConfigKey, int measurementOrdinal) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "get statistics type from measurement = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName()); //$NON-NLS-1$
		return this.getMeasurement(channelConfigKey, measurementOrdinal).getStatistics();
	}

	/**
	 * remove the statistics type of the specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 */
	public void removeStatisticsTypeFromMeasurement(int channelConfigNumber, int measurementOrdinal) {
		this.isChangePropery = true;
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "remove statistics type from measurement = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName()); //$NON-NLS-1$
		this.getMeasurement(channelConfigNumber, measurementOrdinal).setStatistics(null);
	}

	/**
	 * @return the measurement and settlement and scoregroup names in device configuration order which conforms to the record ordinal sequence.
	 */
	public String[] getMeasurementSettlementScoregroupNames(int channelConfigNumber) {
		StringBuilder sb = new StringBuilder();
		ChannelType channel = this.getChannel(channelConfigNumber);
		if (channel != null) {
			for (MeasurementType measurementType : channel.getMeasurement()) {
				sb.append(measurementType.getName()).append(GDE.STRING_SEMICOLON);
			}
			if (channel.getSettlements() != null) {
				for (SettlementType settlement : channel.getSettlements().values()) {
					sb.append(settlement.getName()).append(GDE.STRING_SEMICOLON);
				}
			}
			if (channel.getScoreGroups() != null) {
				for (ScoreGroupType scoregroup : channel.getScoreGroups().values()) {
					sb.append(scoregroup.getName()).append(GDE.STRING_SEMICOLON);
				}
			}
		}
		return sb.toString().length() > 1 ? sb.toString().split(GDE.STRING_SEMICOLON) : new String[0];
	}

	/**
	 * @return the sorted measurement names
	 */
	public String[] getMeasurementNames(int channelConfigNumber) {
		StringBuilder sb = new StringBuilder();
		ChannelType channel = this.getChannel(channelConfigNumber);
		if (channel != null) {
			List<MeasurementType> measurement = channel.getMeasurement();
			for (MeasurementType measurementType : measurement) {
				sb.append(measurementType.getName()).append(GDE.STRING_SEMICOLON);
			}
		}
		return sb.toString().length() > 1 ? sb.toString().split(GDE.STRING_SEMICOLON) : new String[0];
	}

	/**
	 * @return the sorted measurement names
	 */
	public String[] getMeasurementNamesReplacements(int channelConfigNumber) {
		return xmlResource.getReplacements(this.getMeasurementNames(channelConfigNumber));
	}

	/**
	 * @return the sorted measurement names
	 */
	@Deprecated
	public String[] getMeasurementNames(String channelConfigKey) {
		StringBuilder sb = new StringBuilder();
		ChannelType channel = this.getChannel(channelConfigKey);
		if (channel != null) {
			List<MeasurementType> measurement = channel.getMeasurement();
			for (MeasurementType measurementType : measurement) {
				sb.append(measurementType.getName()).append(GDE.STRING_SEMICOLON);
			}
		}
		return sb.toString().length() > 1 ? xmlResource.getReplacements(sb.toString().split(GDE.STRING_SEMICOLON)) : new String[0];
	}

	/**
	 * get property with given channel configuration key, measurement key and property type key (IDevice.OFFSET, ...)
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return PropertyType
	 */
	public PropertyType getMeasruementProperty(int channelConfigNumber, int measurementOrdinal, String propertyKey) {
		PropertyType property = null;
		try {
			MeasurementType measurementType = this.getMeasurement(channelConfigNumber, measurementOrdinal);
			if (measurementType != null) {
				List<PropertyType> properties = measurementType.getProperty();
				for (PropertyType propertyType : properties) {
					if (propertyType.getName().equals(propertyKey)) {
						property = propertyType;
						break;
					}
				}
			}
		} catch (RuntimeException e) {
			log.log(Level.SEVERE, channelConfigNumber + " - " + this.getMeasurementNames(channelConfigNumber)[measurementOrdinal] + " - " + propertyKey, e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return property;
	}

	/**
	 * remove property with given channel configuration key, measurement key and property type key
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param propertyKey
	 */
	public void removeMeasruementProperty(int channelConfigNumber, int measurementOrdinal, String propertyKey) {
		MeasurementType measurementType = this.getMeasurement(channelConfigNumber, measurementOrdinal);
		if (measurementType != null) {
			List<PropertyType> properties = measurementType.getProperty();
			Iterator<PropertyType> iter = properties.iterator();
			while (iter.hasNext()) {
				PropertyType propertyType = iter.next();
				if (propertyType.getName().equals(propertyKey)) {
					iter.remove();
					break;
				}
			}
		}
		this.isChangePropery = true;
	}

	/**
	 * get property with given channel configuration key, measurement key and property type key (IDevice.OFFSET, ...)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return PropertyType
	 */
	@Deprecated
	public PropertyType getMeasruementProperty(String channelConfigKey, int measurementOrdinal, String propertyKey) {
		PropertyType property = null;
		try {
			MeasurementType measurementType = this.getMeasurement(channelConfigKey, measurementOrdinal);
			if (measurementType != null) {
				List<PropertyType> properties = measurementType.getProperty();
				for (PropertyType propertyType : properties) {
					if (propertyType.getName().equals(propertyKey)) {
						property = propertyType;
						break;
					}
				}
			}
		} catch (RuntimeException e) {
			log.log(Level.SEVERE, channelConfigKey + " - " + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + " - " + propertyKey, e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return property;
	}

	/**
	 * get the offset value of the specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	public double getMeasurementOffset(int channelConfigNumber, int measurementOrdinal) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "get offset from measurement name = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName()); //$NON-NLS-1$
		double value = 0.0;
		PropertyType property = this.getMeasruementProperty(channelConfigNumber, measurementOrdinal, IDevice.OFFSET);
		if (property != null) value = Double.valueOf(property.getValue()).doubleValue();

		return value;
	}

	/**
	 * get the offset value of the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	@Deprecated
	public double getMeasurementOffset(String channelConfigKey, int measurementOrdinal) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "get offset from measurement name = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName()); //$NON-NLS-1$
		double value = 0.0;
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementOrdinal, IDevice.OFFSET);
		if (property != null) value = Double.valueOf(property.getValue()).doubleValue();

		return value;
	}

	/**
	 * set new value for offset at the specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param offset the offset to set
	 */
	public void setMeasurementOffset(int channelConfigNumber, int measurementOrdinal, double offset) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "set offset onto measurement name = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName()); //$NON-NLS-1$
		this.isChangePropery = true;
		PropertyType property = this.getMeasruementProperty(channelConfigNumber, measurementOrdinal, IDevice.OFFSET);
		if (property == null) {
			createProperty(channelConfigNumber, measurementOrdinal, IDevice.OFFSET, DataTypes.DOUBLE, offset);
		} else {
			property.setValue(GDE.STRING_EMPTY + offset);
		}
	}

	/**
	 * set new value for offset at the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param offset the offset to set
	 */
	@Deprecated
	public void setMeasurementOffset(String channelConfigKey, int measurementOrdinal, double offset) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "set offset onto measurement name = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName()); //$NON-NLS-1$
		this.isChangePropery = true;
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementOrdinal, IDevice.OFFSET);
		if (property == null) {
			createProperty(channelConfigKey, measurementOrdinal, IDevice.OFFSET, DataTypes.DOUBLE, offset);
		} else {
			property.setValue(GDE.STRING_EMPTY + offset);
		}
	}

	/**
	 * get the factor value of the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return the factor, if property does not exist return 1.0 as default value
	 */
	public double getMeasurementFactor(int channelConfigNumber, int measurementOrdinal) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "get factor from measurement name = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName()); //$NON-NLS-1$
		double value = 1.0;
		PropertyType property = getMeasruementProperty(channelConfigNumber, measurementOrdinal, IDevice.FACTOR);
		if (property != null) value = Double.valueOf(property.getValue()).doubleValue();

		return value;
	}

	/**
	 * get the factor value of the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the factor, if property does not exist return 1.0 as default value
	 */
	@Deprecated
	public double getMeasurementFactor(String channelConfigKey, int measurementOrdinal) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "get factor from measurement name = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName()); //$NON-NLS-1$
		double value = 1.0;
		PropertyType property = getMeasruementProperty(channelConfigKey, measurementOrdinal, IDevice.FACTOR);
		if (property != null) value = Double.valueOf(property.getValue()).doubleValue();

		return value;
	}

	/**
	 * set new value for factor at the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param factor the offset to set
	 */
	public void setMeasurementFactor(int channelConfigNumber, int measurementOrdinal, double factor) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "set factor onto measurement name = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName()); //$NON-NLS-1$
		this.isChangePropery = true;
		PropertyType property = this.getMeasruementProperty(channelConfigNumber, measurementOrdinal, IDevice.FACTOR);
		if (property == null) {
			createProperty(channelConfigNumber, measurementOrdinal, IDevice.FACTOR, DataTypes.DOUBLE, factor);
		} else {
			property.setValue(GDE.STRING_EMPTY + factor);
		}
	}

	/**
	 * set new value for factor at the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param factor the offset to set
	 */
	@Deprecated
	public void setMeasurementFactor(String channelConfigKey, int measurementOrdinal, double factor) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "set factor onto measurement name = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName()); //$NON-NLS-1$
		this.isChangePropery = true;
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementOrdinal, IDevice.FACTOR);
		if (property == null) {
			createProperty(channelConfigKey, measurementOrdinal, IDevice.FACTOR, DataTypes.DOUBLE, factor);
		} else {
			property.setValue(GDE.STRING_EMPTY + factor);
		}
	}

	/**
	 * get the reduction value of the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return the reduction, if property does not exist return 0.0 as default value
	 */
	public double getMeasurementReduction(int channelConfigNumber, int measurementOrdinal) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "get reduction from measurement name = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName()); //$NON-NLS-1$
		double value = 0.0;
		PropertyType property = getMeasruementProperty(channelConfigNumber, measurementOrdinal, IDevice.REDUCTION);
		if (property != null) value = Double.valueOf(property.getValue()).doubleValue();

		return value;
	}

	/**
	 * get the reduction value of the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the reduction, if property does not exist return 0.0 as default value
	 */
	@Deprecated
	public double getMeasurementReduction(String channelConfigKey, int measurementOrdinal) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "get reduction from measurement name = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName()); //$NON-NLS-1$
		double value = 0.0;
		PropertyType property = getMeasruementProperty(channelConfigKey, measurementOrdinal, IDevice.REDUCTION);
		if (property != null) value = Double.valueOf(property.getValue()).doubleValue();

		return value;
	}

	/**
	 * set new value for factor at the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param reduction of the direct measured value
	 */
	public void setMeasurementReduction(int channelConfigNumber, int measurementOrdinal, double reduction) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "set reduction onto measurement name = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName()); //$NON-NLS-1$
		this.isChangePropery = true;
		PropertyType property = this.getMeasruementProperty(channelConfigNumber, measurementOrdinal, IDevice.REDUCTION);
		if (property == null) {
			createProperty(channelConfigNumber, measurementOrdinal, IDevice.REDUCTION, DataTypes.DOUBLE, reduction);
		} else {
			property.setValue(GDE.STRING_EMPTY + reduction);
		}
	}

	/**
	 * set new value for factor at the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param reduction of the direct measured value
	 */
	@Deprecated
	public void setMeasurementReduction(String channelConfigKey, int measurementOrdinal, double reduction) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "set reduction onto measurement name = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName()); //$NON-NLS-1$
		this.isChangePropery = true;
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementOrdinal, IDevice.REDUCTION);
		if (property == null) {
			createProperty(channelConfigKey, measurementOrdinal, IDevice.REDUCTION, DataTypes.DOUBLE, reduction);
		} else {
			property.setValue(GDE.STRING_EMPTY + reduction);
		}
	}

	/**
	 * get a property of specified measurement, the data type must be known - data conversion is up to implementation
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return the property from measurement defined by key, if property does not exist return 1 as default value
	 */
	public Object getMeasurementPropertyValue(int channelConfigNumber, int measurementOrdinal, String propertyKey) {
		PropertyType property = this.getMeasruementProperty(channelConfigNumber, measurementOrdinal, propertyKey);
		return property != null ? property.getValue() : GDE.STRING_EMPTY;
	}

	/**
	 * get a property of specified measurement, the data type must be known - data conversion is up to implementation
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return the property from measurement defined by key, if property does not exist return 1 as default value
	 */
	@Deprecated
	public Object getMeasurementPropertyValue(String channelConfigKey, int measurementOrdinal, String propertyKey) {
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementOrdinal, propertyKey);
		return property != null ? property.getValue() : GDE.STRING_EMPTY;
	}

	/**
	 * set new property value of specified measurement, if the property does not exist it will be created
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @param type of DataTypes
	 * @param value
	 */
	public void setMeasurementPropertyValue(int channelConfigNumber, int measurementOrdinal, String propertyKey, DataTypes type, Object value) {
		this.isChangePropery = true;
		if (value != null) {
			PropertyType property = this.getMeasruementProperty(channelConfigNumber, measurementOrdinal, propertyKey);
			if (property == null) {
				if (type == DataTypes.STRING)
					createProperty(channelConfigNumber, measurementOrdinal, propertyKey, type, (GDE.STRING_EMPTY + value));
				else
					createProperty(channelConfigNumber, measurementOrdinal, propertyKey, type, (GDE.STRING_EMPTY + value).replace(GDE.CHAR_COMMA, GDE.CHAR_DOT));
			} else {
				if (type == DataTypes.STRING)
					property.setValue((GDE.STRING_EMPTY + value));
				else
					property.setValue((GDE.STRING_EMPTY + value).replace(GDE.CHAR_COMMA, GDE.CHAR_DOT));
			}
		} else
			this.removeMeasruementProperty(channelConfigNumber, measurementOrdinal, propertyKey);
	}

	/**
	 * set new property value of specified measurement, if the property does not exist it will be created
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @param type of DataTypes
	 * @param value
	 */
	@Deprecated
	public void setMeasurementPropertyValue(String channelConfigKey, int measurementOrdinal, String propertyKey, DataTypes type, Object value) {
		this.isChangePropery = true;
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementOrdinal, propertyKey);
		if (property == null) {
			createProperty(channelConfigKey, measurementOrdinal, propertyKey, type, (GDE.STRING_EMPTY + value).replace(GDE.CHAR_COMMA, GDE.CHAR_DOT));
		} else {
			property.setValue((GDE.STRING_EMPTY + value).replace(GDE.CHAR_COMMA, GDE.CHAR_DOT));
		}
	}

	/**
	 * create a measurement property
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	private void createProperty(int channelConfigNumber, int measurementOrdinal, String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue(GDE.STRING_EMPTY + value);
		this.getMeasurement(channelConfigNumber, measurementOrdinal).getProperty().add(newProperty);
		this.isChangePropery = true;
	}

	/**
	 * create a measurement property
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	@Deprecated
	private void createProperty(String channelConfigKey, int measurementOrdinal, String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue(GDE.STRING_EMPTY + value);
		this.getMeasurement(channelConfigKey, measurementOrdinal).getProperty().add(newProperty);
		this.isChangePropery = true;
	}

	/**
	 * create a desktop property
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	private void createDesktopProperty(String propertyKey, Boolean value) {
		ObjectFactory factory = new ObjectFactory();
		DesktopPropertyType newProperty = factory.createDesktopPropertyType();
		newProperty.setName(DesktopPropertyTypes.fromValue(propertyKey));
		newProperty.setValue(value);

		if (this.desktop == null) {
			this.desktop = factory.createDesktopType();
			this.deviceProps.setDesktop(this.desktop);
		}

		this.desktop.getProperty().add(newProperty);
		this.isChangePropery = true;
	}

	/**
	 * @param enabled the isChangePropery to set
	 */
	public void setChangePropery(boolean enabled) {
		this.isChangePropery = enabled;
	}

	/**
	 * @return the isChangePropery
	 */
	public boolean isChangePropery() {
		return this.isChangePropery;
	}

	/**
	 * method to modify open/close serial port menu toolbar button and device menu entry
	 * this enable different naming instead open/close start/stop gathering data from device
	 * and must be called within specific device constructor
	 * @param useIconSet DeviceSerialPort.ICON_SET_OPEN_CLOSE | DeviceSerialPort.ICON_SET_START_STOP | DeviceSerialPort.ICON_SET_IMPORT_CLOSE
	 * @param useToolTipOpen
	 * @param useToolTipClose
	 */
	public void configureSerialPortMenu(int useIconSet, String useToolTipOpen, String useTooTipClose) {
		DataExplorer application = DataExplorer.getInstance();
		if (application.getMenuBar() != null) application.getMenuBar().setSerialPortIconSet(useIconSet);
		if (application.getMenuToolBar() != null) application.getMenuToolBar().setSerialPortIconSet(useIconSet, useToolTipOpen, useTooTipClose);
	}

	/**
	 * @return the calculationThread
	 */
	public CalculationThread getCalculationThread() {
		return this.calculationThread;
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	public int[] getCellVoltageOrdinals() {
		return new int[] { -1, -1 };
	}

	/**
	 * This function allows to register a device specific CTabItem to the main application tab folder to display device
	 * specific curve calculated from point combinations or other specific dialog
	 * As default the function should return null which stands for no device custom tab item.
	 */
	public CTabItem getUtilityGraphicsTabItem() {
		return null;
	}

	/**
	 * This function allows to register a custom CTabItem to the main application tab folder to display device
	 * specific curve calculated from point combinations or other specific dialog
	 * As default the function should return null which stands for no device custom tab item.
	 */
	public CTabItem getUtilityDeviceTabItem() {
		return null;
	}

	/**
	 * query if the actual record set of this device contains GPS data to enable KML export to enable google earth visualization
	 * set value of -1 to suppress this measurement
	 */
	public boolean isActualRecordSetWithGpsData() {
		return false;
	}

	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	public String exportFile(String fileEndingType, boolean isExport2TmpDir) {
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "fileEndingType = " + fileEndingType + ", isExport2TmpDir = " + isExport2TmpDir);
		return GDE.STRING_EMPTY;
	}

	/**
	 * query the jar name of the active device implementation
	 * @return jar name of the active device
	 */
	public String getJarName() {
		return this.getClass().getProtectionDomain().toString();
	}

	/**
	 * set the measurement ordinal to be used for limits as well as the colors which are specified to display in Google Earth
	 */
	public void setGPS2KMZMeasurementOrdinal(final Integer ordinal) {
		this.kmzMeasurementOrdinal = ordinal;
	}

	/**
	 * @return the measurement ordinal to be used for limits as well as the colors which are specified to display in Google Earth
	 */
	public Integer getGPS2KMZMeasurementOrdinal() {
		return -1;
	}

	/**
	 * @return the translated latitude or longitude to IGC latitude {DDMMmmmN/S, DDDMMmmmE/W} for GPS devices only
	 */
	public String translateGPS2IGC(RecordSet recordSet, int index, char fixValidity, int startAltitude, int offsetAltitude) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordSet.getName() + index + fixValidity + startAltitude + offsetAltitude);
		return "DDMMmmmNDDDMMmmm0V000000000000";
	}

	/**
	 * query the default stem used as record set name
	 * @return recordSetStemName
	 */
	public String getRecordSetStemName() {
		return this.getStateType() != null ? this.getStateType().getProperty() != null ? ") " + this.getStateType().getProperty().get(0).getName()
				: Messages.getString(MessageIds.GDE_MSGT0272) : Messages.getString(MessageIds.GDE_MSGT0272);
	}

	/**
	 * query the default stem used as record set replaced name
	 * @return recordSetStemName
	 */
	public String getRecordSetStemNameReplacement() {
		return this.getStateType() != null ? this.getStateType().getProperty() != null
				? ") " + xmlResource.getReplacement(this.getStateType().getProperty().get(0).getName()) : Messages.getString(MessageIds.GDE_MSGT0272)
				: Messages.getString(MessageIds.GDE_MSGT0272);
	}

	/**
	 * query the state name used as record set name
	 * @return getRecordSetStateName
	 */
	public String getRecordSetStateName(final int stateNumber) {
		return this.getStateType() != null ? this.getStateType().getProperty() != null ? this.getStateProperty(stateNumber).getName()
				: "state_data_recording" : "state_data_recording";
	}

	/**
	 * query the state name used as record set name
	 * @return getRecordSetStateName
	 */
	public String getRecordSetStateNameReplacement(final int stateNumber) {
		try {
			return this.getStateType() != null ? this.getStateType().getProperty() != null
					? xmlResource.getReplacement(this.getStateProperty(stateNumber).getName()) : xmlResource.getReplacement("state_data_recording")
					: xmlResource.getReplacement("state_data_recording");
		}
		catch (Exception e) {
			return xmlResource.getReplacement("state_data_recording");
		}
	}

	/**
	 * @return the device communication port
	 */
	public IDeviceCommPort getCommunicationPort() {
		return null;
	}

	/**
	 * get the last used channel number (ordinal + 1 = channel number)
	 * @return the last used channel number
	 */
	public int getLastChannelNumber() {
		int channelNumber = this.settings.getLastUseChannelNumber(this.getName());
		return channelNumber <= this.deviceProps.getChannels().channel.size() ? channelNumber : this.deviceProps.getChannels().channel.size();
	}

	/**
	 * set the last used channel number (ordinal + 1 = channel number)
	 */
	@Deprecated // use settings.addDeviceUse without this deviation method
	public void setLastChannelNumber(int channelNumber) {
		this.settings.addDeviceUse(this.getName(), channelNumber);
	}

	/**
	 * query the channel properties if there are any
	 * @return
	 */
	public List<ChannelPropertyType> getChannelProperties() {
		return this.deviceProps.getChannels().getProperty();
	}

	/**
	 * query the channel property of specified type
	 * @return property if exist or null if not exist
	 */
	public ChannelPropertyType getChannelProperty(ChannelPropertyTypes key) {
		if (getChannelProperties() != null) {
			for (ChannelPropertyType property : getChannelProperties()) {
				if (property.name.equals(key)) return property;
			}
		}
		ChannelPropertyType channelProperty = new ObjectFactory().createChannelPropertyType();
		channelProperty.setName(key);
		return channelProperty;
	}

	/**
	 * set a channel property value
	 * @param key
	 * @param type
	 * @param value
	 */
	public void setChannelProperty(ChannelPropertyTypes key, DataTypes type, String value) {
		ChannelPropertyType channelProperty = getChannelProperty(key);
		channelProperty.setType(type);
		channelProperty.setValue(value);
		List<ChannelPropertyType> properties = getChannelProperties();
		if (!properties.contains(channelProperty)) properties.add(channelProperty);
		this.isChangePropery = true;
	}

	/**
	 * query the channel property of type getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER)
	 * @return true if curve point should be filtered, default implementation returns false
	 */
	public boolean isFilterEnabled() {
		return false;
	}

	/**
	 * get the curve point device individual filtered if required
	 */
	public Integer getFilteredPoint(int channelNumber, Record record, int index) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, channelNumber + record.getName() + index);
		return record.realGet(index);
	}

	/**
	 * @return the device specific dialog instance
	 */
	public DeviceDialog getDialog() {
		return null;
	}

	/**
	 * method to get the sorted active or in active record names as string array
	 * - records which does not have inactive or active flag are calculated from active or inactive
	 * - all records not calculated may have the active status and must be stored
	 * @param channelConfigNumber
	 * @param validMeasurementNames based on the current or any previous configuration
	 * @return String[] containing record names
	 */
	public String[] getNoneCalculationMeasurementNames(int channelConfigNumber, String[] validMeasurementNames) {
		final Vector<String> tmpCalculationRecords = new Vector<String>();
		final String[] deviceMeasurements = this.getMeasurementNames(channelConfigNumber);
		int deviceDataBlockSize = Math.abs(this.getDataBlockSize(FormatTypes.VALUE));
		deviceDataBlockSize = this.getDataBlockSize(FormatTypes.VALUE) <= 0 ? deviceMeasurements.length : deviceDataBlockSize;
		// record names may not match device measurements, but device measurements might be more then existing records
		for (int i = 0; i < deviceMeasurements.length && i < validMeasurementNames.length; ++i) {
			final MeasurementType measurement = this.getMeasurement(channelConfigNumber, i);
			if (!measurement.isCalculation()) { // active or inactive
				tmpCalculationRecords.add(validMeasurementNames[i]);
			}
			// else
			// System.out.println(measurement.getName());
		}
		// assume attached records are calculations like DataVario
		while (tmpCalculationRecords.size() > deviceDataBlockSize) {
			tmpCalculationRecords.remove(deviceDataBlockSize);
		}
		return tmpCalculationRecords.toArray(new String[0]);
	}

	/**
	 * check and adapt stored measurement properties against actual record set records which gets created by device properties XML
	 * - calculated measurements could be later on added to the device properties XML
	 * - devices with battery cell voltage does not need to all the cell curves which does not contain measurement values
	 * @param fileRecordsProperties - all the record describing properties stored in the file
	 * @param recordSet - the record sets with its measurements build up with its measurements from device properties XML
	 * @return string array of measurement names which match the ordinal of the record set requirements to restore file record properties
	 */
	public String[] crossCheckMeasurements(String[] fileRecordsProperties, RecordSet recordSet) {
		// check for device file contained record properties which are not contained in actual configuration
		String[] recordKeys = recordSet.getRecordNames();
		Vector<String> cleanedRecordNames = new Vector<String>();
		if ((recordKeys.length - fileRecordsProperties.length) > 0) { // events ...
			int i = 0;
			for (; i < fileRecordsProperties.length; ++i) {
				cleanedRecordNames.add(recordKeys[i]);
			}
			// cleanup recordSet
			for (; i < recordKeys.length; ++i) {
				recordSet.remove(recordKeys[i]);
			}
			recordKeys = cleanedRecordNames.toArray(new String[1]);
		}
		return recordKeys;
	}

	/**
	 * check and adapt stored measurement specialties properties against actual record set records which gets created by device properties XML
	 * - like GPS type dependent properties
	 * @param fileRecordsProperties - all the record describing properties stored in the file
	 * @param recordSet - the record sets with its measurements build up with its measurements from device properties XML
	 */
	public void applyMeasurementSpecialties(String[] fileRecordsProperties, RecordSet recordSet) {
		return;
	}

	/**
	 * query if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 * @param record
	 * @return
	 */
	public boolean isGPSCoordinates(Record record) {
		return false;
	}

	/**
	 * query the measurement ordinal of the first Lithium cell for cell voltage display
	 * 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 8=Balance
	 * 9=SpannungZelle1 10=SpannungZelle2 11=SpannungZelle3 12=SpannungZelle4 13=SpannungZelle5 14=SpannungZelle6 ..
	 * @return 9 for this example
	 */
	public int getMeasurementOrdinalFirstLithiumCell(RecordSet recordSet) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordSet.getName());
		return 0;
	}

	/**
	 * query the number of Lithium cells if any
	 * @param specificData
	 * @return cell count if any
	 */
	public int getNumberOfLithiumCells(Object specificData) {
		return 0;
	}

	/**
	 * query if the record set numbering should follow channel configuration numbering
	 * @return true where devices does not distinguish between channels (for example Av4ms_FV_762)
	 */
	public boolean recordSetNumberFollowChannel() {
		return this.getChannelTypes(1) == ChannelTypes.TYPE_OUTLET;
	}

	/**
	 * query if the channel in use has dependency with each other to initiate file description synchronization 
	 * @return true for devices with one source of data distributed over channel record sets (HoTTAdapter, Av4ms_FV_762)
	 */
	public boolean useChannelWithSyncedDescription() { return false; }
	
	/**
	 * query device for specific smoothing index
	 * 0 do nothing at all
	 * 1 current drops just a single peak
	 * 2 current drop more or equal than 2 measurements
	 */
	public int getCurrentSmoothIndex() {
		return 0;
	}

	/**
	 * query if the measurements get build up dynamically while reading (import) the data
	 * the implementation must create measurementType while reading the import data,
	 * refer to Weatronic-Telemetry implementation DataHeader
	 * @return true|false, default is false and we have a constant measurement size defined in device XML
	 */
	public boolean isVariableMeasurementSize() {
		return false;
	}

	/**
	 * @param channelConfigNumberI
	 * @param channelConfigNumberJ
	 * @return true if the two channels have identical measurement names
	 */
	public boolean isPairOfChannels(int channelConfigNumberI, int channelConfigNumberJ) {
		if (this.channelGroups == null) {
			initChannelGroups();
		}
		return this.channelGroups[channelConfigNumberI - 1] > -1 && this.channelGroups[channelConfigNumberI - 1] == this.channelGroups[channelConfigNumberJ - 1];
	}

	/**
	 * Do not check the channel mix setting.
	 * @return the 1-based config numbers of those channels which carry identical measurement names compared to the channel identified by the
	 *         param (result size >= 1)
	 */
	public List<Integer> getChannelMixConfigNumbers(int channelNumber) {
		final List<Integer> channelMixConfigNumbers;
		if (getDeviceGroup() == DeviceTypes.CHARGER)
			channelMixConfigNumbers = getChannelBundle(channelNumber);
		else
			channelMixConfigNumbers = Collections.singletonList(channelNumber);
		return channelMixConfigNumbers;
	}

	/**
	 * @param channelConfigNumber is a 1-based number
	 * @return the 1-based config numbers of those channels which carry identical measurement names compared to the channel identified by the
	 *         param (result size >= 1)
	 */
	private List<Integer> getChannelBundle(int channelConfigNumber) {
		List<Integer> channelNumbers = new ArrayList<Integer>();
		if (this.channelGroups == null) {
			initChannelGroups();
		}
		if (this.channelGroups[channelConfigNumber - 1] > -1) {
			for (int i = 0; i < this.channelGroups.length; i++) {
				if (this.channelGroups[i] != -1 && this.channelGroups[i] == this.channelGroups[channelConfigNumber - 1]) channelNumbers.add(i + 1); // 1-based
			}
		} else {
			channelNumbers.add(channelConfigNumber);
		}
		return channelNumbers;
	}

	/**
	 * compare channel measurement names and define channel groups with identical measurement names.
	 */
	private void initChannelGroups() {
		int tmpGroupIndex = -1;
		this.channelGroups = new int[this.getChannelCount()];
		Arrays.fill(this.channelGroups, -1);
		for (int i = 0; i < this.deviceProps.getChannels().channel.size(); i++) {
			String[] measurementNamesI = this.getMeasurementNames(i + 1);
			Arrays.sort(measurementNamesI);
			for (int j = i + 1; j < this.deviceProps.getChannels().channel.size(); j++) {
				String[] measurementNamesJ = this.getMeasurementNames(j + 1);
				Arrays.sort(measurementNamesJ);
				if (Arrays.equals(measurementNamesI, measurementNamesJ)) {
					if (this.channelGroups[i] == -1) {
						this.channelGroups[i] = ++tmpGroupIndex;
					}
					this.channelGroups[j] = this.channelGroups[i];
				}
			}
		}
	}

	/**
	 * function to prepare a row of record set for export while translating available measurement values.
	 * @return pointer to filled data table row with formated values
	 */
	public String[] prepareExportRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				if (isGPSCoordinates(record)) {
					int grad = record.realGet(rowIndex) / 1000000;
					double minuten = record.realGet(rowIndex) % 1000000 / 10000.0;
					dataTableRow[index + 1] = String.format("%02.6f", grad + minuten / 100.); //$NON-NLS-1$
				} else {
					dataTableRow[index + 1] = record.getDecimalFormat().format((record.getOffset() + ((record.realGet(rowIndex) / 1000.0) - record.getReduction()) * record.getFactor()));
				}
				++index;
			}
		} catch (RuntimeException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * function to prepare a row of record set for export while translating available measurement values.
	 * @return pointer to filled data table row with formated values
	 */
	public String[] prepareRawExportRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			String[] recordNames = recordSet.getRecordNames();
			for (int index = 1, j = 0; index < dataTableRow.length && j < recordNames.length; j++) { //do not touch index 0 with time entry
				final Record record = recordSet.get(recordNames[j]);
				MeasurementType  measurement = this.getMeasurement(recordSet.getChannelConfigNumber(), record.getOrdinal());
				if (!measurement.isCalculation()) {	// only use active records for writing raw data
					dataTableRow[index] = String.format("%d", record.realGet(rowIndex)/1000);
					++index;
				}
			}
		} catch (RuntimeException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * default implementation returning device name which is the default directory name to store OSD files as well to search for
	 * @return the preferred directory to search and store for device specific files, this enable for instance MC3000-Set to store all files as
	 *         well in MC3000 directory
	 */
	public String getFileBaseDir() {
		return this.getName();
	}

	/**
	 * Roaming data sources support via the DataAccess class.
	 * @return sha1 key as a unique identifier for the device xml file contents
	 */
	public String getFileSha1Hash() {
		if (this.fileSha1Hash.isEmpty()) setFileSha1Hash();
		return this.fileSha1Hash;
	}

	private void setFileSha1Hash() {
		Path fileSubPath = Paths.get(GDE.APPL_HOME_PATH).relativize(this.xmlFile);
		try (InputStream inputStream = DataAccess.getInstance().getDeviceXmlInputStream(fileSubPath)) { // ok
			this.fileSha1Hash = SecureHash.sha1(inputStream);
		} catch (Exception e) {
			this.fileSha1Hash = GDE.STRING_EMPTY;
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * get the measurement ordinal of altitude, speed and trip length
	 * @return empty integer array if device does not fulfill complete requirement
	 */
	public int[] getAtlitudeTripSpeedOrdinals() { return new int[0]; }  

	/**
	 * @param key string of key to be replaced
	 * @return the replacement
	 */
	public String getMeasurementReplacement(String key) { return xmlResource.getReplacement(key); }
	
	/**
	 * cross check and update given channel configuration to drive correct selection, p.e. if simplified OSD stored with Android version
	 * @param channelConfig "1 : UniLog2"
	 * @param recordSetInfo map containing actual recordSet related information
	 * @return new signature of channelConfiguration
	 */
	public String crossCheckChannelConfig(String channelConfig, HashMap<String, String> recordSetInfo) { return channelConfig; }

}