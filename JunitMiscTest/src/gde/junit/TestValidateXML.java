package gde.junit;

import gde.GDE;
import gde.config.Settings;
import gde.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import junit.framework.TestCase;

import org.xml.sax.SAXParseException;

public class TestValidateXML extends TestCase {
	static Logger	log = Logger.getLogger(TestValidateXML.class.getName());
	
	// JAXB XML environment
	Schema											schema;
	JAXBContext									jc;
	Unmarshaller								unmarshaller;
	Marshaller									marshaller;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
    log.setLevel(Level.INFO);
    log.setUseParentHandlers(true);
	}

	/**
	 * check all device XML files in workspace to be valid and well-formed
	 */
	public void testValidateWorkspaceXMLs() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();
		String basePath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
		basePath = basePath.substring(GDE.IS_WINDOWS?1:0, basePath.indexOf("JunitMiscTest"));
		log.log(Level.INFO, "basePath = " + basePath);
		String xsdBasePath = basePath + GDE.STRING_FILE_SEPARATOR_UNIX + "DataExplorer/src/resource/";
		log.log(Level.INFO, "xsdBasePath = " + xsdBasePath);


		try {
			loadXSD(xsdBasePath);
			List<File> files = FileUtils.getFileListing(new File(basePath), 2);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(GDE.FILE_ENDING_DOT_XML) && file.getPath().contains("src") && file.getPath().contains("resource") 
						&& !file.getPath().contains("template") && !file.getPath().contains("DataExplorer_Sample")) {
					log.log(Level.INFO, "working with : " + file);

					try {
						this.unmarshaller.unmarshal(file);
					}
					catch (JAXBException e) {
						log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
						if (e.getLinkedException() instanceof SAXParseException) {
							SAXParseException spe = (SAXParseException) e.getLinkedException();
							failures.put(file.getAbsolutePath(), spe);
						}
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}
	
//	/**
//	 * check all device XML files in application configuration directory to be valid and well-formed
//	 */
//	public void testValidateApplicationConfigXMLs() {
//		HashMap<String, Exception> failures = new HashMap<String, Exception>();
//
//		try {
//			List<File> files = FileUtils.getFileListing(new File(this.getDevicesPath()));
//
//			for (File file : files) {
//				if (file.getAbsolutePath().toLowerCase().endsWith(GDE.FILE_ENDING_DOT_XML)) {
//					log.log(Level.INFO, "working with : " + file);
//
//					try {
//						
//					}
//					catch (Exception e) {
//						e.printStackTrace();
//						failures.put(file.getAbsolutePath(), e);
//					}
//				}
//			}
//		}
//		catch (FileNotFoundException e) {
//			e.printStackTrace();
//			fail(e.toString());
//		}
//
//		StringBuilder sb = new StringBuilder();
//		for (String key : failures.keySet()) {
//			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
//		}
//		if (failures.size() > 0) fail(sb.toString());
//	}
	
	/**
	 * load the XSD to instantiate the marshaller/unmarshaller
	 * @param xmlBasePath
	 */
	void loadXSD(String xmlBasePath) {
		try {
			this.schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new File(xmlBasePath + Settings.DEVICE_PROPERTIES_XSD_NAME));
			this.jc = JAXBContext.newInstance("gde.device"); //$NON-NLS-1$
			this.unmarshaller = this.jc.createUnmarshaller();
			this.unmarshaller.setSchema(this.schema);
			this.marshaller = this.jc.createMarshaller();
			this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.valueOf(true));
			this.marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, Settings.DEVICE_PROPERTIES_XSD_NAME);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}
