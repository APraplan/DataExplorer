/**
 * 
 */
package gde.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import gde.log.Level;
import java.util.logging.Logger;

import gde.GDE;
import gde.exception.ApplicationConfigurationException;
import gde.log.LogFormatter;
import gde.utils.FileUtils;

/**
 * @author brueg
 *
 */
public class BuildCompleteHelpHtml {
	static Logger	log	= Logger.getLogger(BuildCompleteHelpHtml.class.getName());
	static Logger	rootLogger;

	static String header_de = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">\n" +
			"<HTML>\n" +
			"<HEAD>\n" +
			"<META HTTP-EQUIV=\"CONTENT-TYPE\" CONTENT=\"text/html; charset=utf-8\">\n" +
			"<TITLE>DataExplorer - Benutzerhandbuch</TITLE>\n" +
			"<META NAME=\"AUTHOR\" CONTENT=\"Winfried Brügmann\">\n" +
			"</HEAD>\n" +
			"<BODY LANG=\"de-DE\" DIR=\"LTR\">\n" +
			"<H0>DataExplorer - Benutzerhandbuch</H0>\n";
	static String header_en = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">\n" +
			"<HTML>\n" +
			"<HEAD>\n" +
			"<META HTTP-EQUIV=\"CONTENT-TYPE\" CONTENT=\"text/html; charset=utf-8\">\n" +
			"<TITLE>DataExplorer - Users Guide</TITLE>\n" +
			"<META NAME=\"AUTHOR\" CONTENT=\"Winfried Brügmann\">\n" +
			"</HEAD>\n" +
			"<BODY LANG=\"en-US\" DIR=\"LTR\">\n" +
			"<H0>DataExplorer - Users Guide</H0>\n";
	static String footer = "</BODY>\n" + "</HTML>";
	
	static String supprtedDevices_de = "Aktuell unterstützte Geräte";
	static String supprtedDevices_en = "Actual Supported Devices";
	
	static String filename_de = GDE.NAME_LONG + " - Information und Hilfe.html";
	static String filename_en = GDE.NAME_LONG + " - Information and Help.html";

	static final String[]	SUPPORTED_LANGUAGES	= new String[] { "de", "en" };
	static final String[]	SUPPORTED_LANGUAGE_HEADERS	= new String[] { header_de, header_en };
	static final String[]	SUPPORTED_LANGUAGE_FILEPATHES	= new String[] { filename_de, filename_en };
	static final String[]	SUPPORTED_LANGUAGE_DEVICES	= new String[] { supprtedDevices_de, supprtedDevices_en };

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ApplicationConfigurationException 
	 * @throws URISyntaxException 
	 */
	public static void main(String[] args) throws IOException, URISyntaxException, ApplicationConfigurationException {
		String baseFilePath;

		String line = GDE.STRING_STAR;
		BufferedReader reader = null;
		BufferedWriter writer = null;

		try {
			initLogger();
			
			URL url = BuildCompleteHelpHtml.class.getProtectionDomain().getCodeSource().getLocation();
			log.log(Level.INFO, "base URL = " + url.toString()); //$NON-NLS-1$
			baseFilePath = url.getFile();
			baseFilePath = baseFilePath.substring(1, baseFilePath.indexOf(GDE.NAME_LONG) + GDE.NAME_LONG.length());
			String targetPath = baseFilePath + GDE.STRING_FILE_SEPARATOR_UNIX + "doc"  + GDE.STRING_FILE_SEPARATOR_UNIX;
			baseFilePath = baseFilePath + GDE.STRING_FILE_SEPARATOR_UNIX + "src" + GDE.STRING_FILE_SEPARATOR_UNIX + "help";
			

			//iterate over de and en directory
			for (int i=0; i<SUPPORTED_LANGUAGES.length; ++i) {

				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetPath + SUPPORTED_LANGUAGE_FILEPATHES[i]), "UTF-8")); //$NON-NLS-1$
				writer.write(SUPPORTED_LANGUAGE_HEADERS[i]);
					
				String langFilePath = baseFilePath + GDE.STRING_FILE_SEPARATOR_UNIX + SUPPORTED_LANGUAGES[i];
				log.log(Level.INFO, "langFilePath = " + langFilePath);
				List<File> files = FileUtils.getFileListing(new File(langFilePath), 4);
				
				for (File file : files) {
					if (file.getAbsolutePath().toLowerCase().endsWith(".html") || file.getAbsolutePath().toLowerCase().endsWith(".htm")) {
						log.log(Level.INFO, "working with : " + file);
						
						reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")); //$NON-NLS-1$
						while ((line = reader.readLine()) != null) {
							//log.log(Level.INFO, line);
							if (line.toUpperCase().contains("<DIV ID=\"CONTENT\"")) {
								while ((line = reader.readLine()) != null) {
									if (line.toUpperCase().contains("</DIV>")) break;
									if (line.toUpperCase().contains("<IMG ")) {
										//log.log(Level.INFO, line);
										line = line.substring(0, line.toUpperCase().indexOf("SRC=\"")+5) + "../src/help/" + SUPPORTED_LANGUAGES[i] + "/" + line.substring(line.toUpperCase().indexOf("SRC=\"")+5);
									}
									if (!line.contains("&lt;&lt;==") && !line.contains("==&gt;&gt;")) {
										//log.log(Level.INFO, line);
										writer.write(line+"\n");
									}
								}
							}
						}
						reader.close();
					}
				}

				writer.write("<H1>" + SUPPORTED_LANGUAGE_DEVICES[i] + "</H1>\n");
				URL[] urls = ((URLClassLoader)GDE.getClassLoader()).getURLs();
				
				for (int j = 0; j < urls.length; ++j) {
					String pluginBaseFilePath = urls[j].getPath().substring(1, urls[j].getPath().indexOf("bin"));
					pluginBaseFilePath = pluginBaseFilePath + "src/help";
					
					if (new File(pluginBaseFilePath).exists() && !pluginBaseFilePath.contains(GDE.NAME_LONG) && !pluginBaseFilePath.contains("Sample")) {
						String pluginLangFilePath = pluginBaseFilePath + GDE.STRING_FILE_SEPARATOR_UNIX + SUPPORTED_LANGUAGES[i];
						log.log(Level.INFO, "pluginLangFilePath = " + pluginLangFilePath);
						
						files = FileUtils.getFileListing(new File(pluginLangFilePath), 5);
						for (File file : files) {
							if (file.getAbsolutePath().toLowerCase().endsWith(".html") || file.getAbsolutePath().toLowerCase().endsWith(".htm")) {
								log.log(Level.INFO, "working with : " + file);

								String deviceName = pluginBaseFilePath.substring(0, pluginBaseFilePath.indexOf("/src/help"));
								deviceName = deviceName.substring(deviceName.lastIndexOf('/')+1);
								writer.write("<H2>" + deviceName + "</H2>\n");

								reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")); //$NON-NLS-1$
								while ((line = reader.readLine()) != null) {
									//log.log(Level.INFO, line);
									if (line.toUpperCase().contains("<BODY")) {
										while ((line = reader.readLine()) != null) {
											if (line.toUpperCase().contains("</BODY>")) break;
											if (line.toUpperCase().contains("<IMG ")) {
												//log.log(Level.INFO, line);
												line = line.substring(0, line.toUpperCase().indexOf("SRC=\"")+5) + "../../" + deviceName + "/src/help/" + SUPPORTED_LANGUAGES[i] + "/" + line.substring(line.toUpperCase().indexOf("SRC=\"")+5);
											}
											if (!line.contains("&lt;&lt;==") && !line.contains("==&gt;&gt;")) {
												//log.log(Level.INFO, line);
												writer.write(line + "\n");
											}
										}
									}
								}
								reader.close();
							}
						}
					}
				}
				writer.close();
			}


		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		finally {
			if (reader != null) reader.close();
			if (writer != null) writer.close();
		}

	}

	/**
	 * init logger
	 */
	private static void initLogger() {
		Handler ch = new ConsoleHandler();
		LogFormatter lf = new LogFormatter();
		rootLogger = Logger.getLogger(GDE.STRING_EMPTY);

		// clean up all handlers from outside
		Handler[] handlers = rootLogger.getHandlers();
		for (int index = 0; index < handlers.length; index++) {
			rootLogger.removeHandler(handlers[index]);
		}
		rootLogger.setLevel(Level.ALL);

		rootLogger.addHandler(ch);
		ch.setFormatter(lf);
		ch.setLevel(Level.INFO);
	}

}
