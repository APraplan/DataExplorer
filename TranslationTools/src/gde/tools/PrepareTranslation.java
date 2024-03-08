/**
 * 
 */
package gde.tools;

import gde.GDE;
import gde.exception.ApplicationConfigurationException;
import gde.log.Level;
import gde.log.LogFormatter;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * @author brueg
 *
 */
public class PrepareTranslation {
	final static Logger						log															= Logger.getLogger(PrepareTranslation.class.getName());
	static Logger									rootLogger;

	/**
	 * 
	 */
	public PrepareTranslation() {
		// empty default constructor
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String basePath;
		initLogger();
		final Vector<URL> urls = new Vector<URL>();
		URL url = GDE.class.getProtectionDomain().getCodeSource().getLocation();
		log.log(Level.INFO, "base URL = " + url.toString()); //$NON-NLS-1$
		
		if (url.getPath().endsWith(GDE.STRING_FILE_SEPARATOR_UNIX)) { // running inside Eclipse
			log.log(Level.INFO, "started inside Eclipse"); //$NON-NLS-1$
			basePath = url.getFile().substring(0, url.getPath().lastIndexOf(DataExplorer.class.getSimpleName()));
			basePath = basePath.replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK);
			log.log(Level.INFO, "basePath = " + basePath); //$NON-NLS-1$
			List<File> files = FileUtils.getFileListing(new File(basePath), 7, "properties");
			String line;
			BufferedReader reader = null;
			//start to write all German messages
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(GDE.JAVA_IO_TMPDIR+"all_messages_de.po"), "UTF-8")); //$NON-NLS-1$
			for (File path : files) {
				if (path.getAbsolutePath().contains("src") && path.getName().contains("messages_de.")) {
					log.log(Level.INFO, "working with : " + path.getAbsolutePath()); //$NON-NLS-1$
					reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "ISO-8859-1")); //$NON-NLS-1$
					while ((line = reader.readLine()) != null) {
						if (line.startsWith("GDE_MSG")) {
							String[] tokens = line.split("=");
							writer.write(String.format("\n#%s\nmsgid \"%s\"\nmsgstr \" \"\n", tokens[0], line.substring(13)));					
						}
					}
					writer.flush();
				}
			}
			writer.close();
			if (reader != null) reader.close();
			
			//start all over to have message source in english
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(GDE.JAVA_IO_TMPDIR+"all_messages_en.po"), "UTF-8")); //$NON-NLS-1$
			for (File path : files) {
				if (path.getAbsolutePath().contains("src") && path.getName().contains("messages.")) {
					log.log(Level.INFO, "working with : " + path.getAbsolutePath()); //$NON-NLS-1$
					reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "ISO-8859-1")); //$NON-NLS-1$
					while ((line = reader.readLine()) != null) {
						if (line.startsWith("GDE_MSG")) {
							String[] tokens = line.split("=");
							writer.write(String.format("\n#%s\nmsgid \"%s\"\nmsgstr \" \"\n", tokens[0], tokens[1]));					
						}
					}
					writer.flush();
				}
			}
			writer.close();
			if (reader != null) reader.close();
		}
		else { // started outside java -jar *.jar
			log.log(Level.INFO, "started outside with: java -jar *.jar"); //$NON-NLS-1$
			basePath = url.getFile().substring(0, url.getPath().lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX) + 1);
			basePath = basePath.replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK).replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
			log.log(Level.INFO, "basePath = " + basePath); //$NON-NLS-1$
			File file = new File(basePath);
			String[] files = file.list();
			if (files == null) {
				throw new ApplicationConfigurationException(Messages.getString(MessageIds.GDE_MSGE0001, new Object[] { basePath }));
			}
			for (String path : files) {
				if (path.endsWith("properties")) {
					URL fileUrl = new File(basePath + path).toURI().toURL();
					urls.add(fileUrl);
					log.log(Level.INFO, "adding : " + fileUrl.toURI()); //$NON-NLS-1$
				}
			}
		}
	}
	/**
	 * init logger
	 */
	public static void initLogger() {
		LogFormatter lf = new LogFormatter();
		rootLogger = Logger.getLogger(GDE.STRING_EMPTY);

		// clean up all handlers from outside
		Handler[] handlers = rootLogger.getHandlers();
		for (Handler handler : handlers) {
			rootLogger.removeHandler(handler);
		}
		rootLogger.setLevel(Level.ALL);

//		if (System.getProperty(GDE.ECLIPSE_STRING) == null) { // running outside eclipse
//			try {
//				GDE.logHandler = new FileHandler(GDE.JAVA_IO_TMPDIR + GDE.BOOTSTRAP_LOG, 50000, 1);
//				GDE.logHandler.setFormatter(lf);
//				GDE.logHandler.setLevel(Level.INFO);
//				rootLogger.addHandler(GDE.logHandler);
//			}
//			catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		else {
			GDE.logHandler = new ConsoleHandler();
			GDE.logHandler.setFormatter(lf);
			GDE.logHandler.setLevel(Level.INFO);
			rootLogger.addHandler(GDE.logHandler);
//		}
	}
}
