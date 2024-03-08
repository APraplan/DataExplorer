package test;

import java.io.File;
import java.io.FileOutputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import gde.device.DevicePropertiesType;
import gde.device.DeviceType;
import gde.device.SerialPortType;

public class ExecTest {

	/**
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		String basePath = "C:/Documents and Settings/brueg/Application Data/DataExplorer/Devices/";

		
		try {
      Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new File(basePath + "DeviceProperties_V03.xsd"));
			JAXBContext jc = JAXBContext.newInstance("gde.device");
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setSchema(schema);
			
			Marshaller marshaller = jc.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,  true);
	    marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,  "DeviceProperties_V04.xsd");
			
			JAXBElement<DevicePropertiesType> elememt1 = (JAXBElement<DevicePropertiesType>)unmarshaller.unmarshal(new File (basePath + "Picolario.xml"));
			DevicePropertiesType devProps = elememt1.getValue();
			
			
			DeviceType device = devProps.getDevice();
			System.out.println(device.getName());
			
			
			SerialPortType serialPort = devProps.getSerialPort();
			System.out.println(serialPort.getPort());
			serialPort.setPort("COM10");
			System.out.println(serialPort.getPort());
			
			JAXBElement<DevicePropertiesType> elememt2 = (JAXBElement<DevicePropertiesType>)unmarshaller.unmarshal(new File (basePath + "UniLog.xml"));
			devProps = elememt2.getValue();
			device = devProps.getDevice();
			System.out.println(device.getName());
			
	    marshaller.marshal(elememt1,
	    	   new FileOutputStream(basePath + "jaxbOutput_1.xml"));

	    marshaller.marshal(elememt2,
	    	   new FileOutputStream(basePath + "jaxbOutput_2.xml"));
			
//		devProp.setName("Picolario");
//		devProp.setUsed(false);
//		devProps.setPort("COM2");
//		devProp.setDataBlockSize(32);
//		devProp.store();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

}
