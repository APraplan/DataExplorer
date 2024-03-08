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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import java.io.IOException;
import java.util.jar.JarFile;
import gde.log.Level;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * This is a simple web browser launcher utility, where help pages can be displayed
 * @author Winfried Brügmann
 */
public class WebBrowser {
	private static Logger				log			= Logger.getLogger(WebBrowser.class.getName());

	public static void openURL(String deviceName, String fileName) {
		String basePath = FileUtils.getJarBasePath() + "/";
		String jarName = "DataExplorer.jar";
		
		if (deviceName.length() >= 1) { // devices/<deviceName>.jar
			basePath = basePath + "devices/";
			jarName = deviceName + ".jar";
		}
		
		log.log(Level.FINE, "basePath = " + basePath + " jarName = " + jarName); //$NON-NLS-1$
		
		try {
			String targetDir = GDE.JAVA_IO_TMPDIR + GDE.NAME_SHORT + GDE.FILE_SEPARATOR;
			String helpDir = "help" + GDE.FILE_SEPARATOR + Settings.getInstance().getLocale().getLanguage() + GDE.FILE_SEPARATOR;
			FileUtils.extractDir(new JarFile(basePath + jarName), helpDir, targetDir, "555");
			
			String stringUrl = targetDir + helpDir + fileName;
			log.log(Level.FINE, "stringUrl = " + stringUrl); //$NON-NLS-1$

			openBrowser(stringUrl);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialog(
					Messages.getString(MessageIds.GDE_MSGE0018, new Object[] { e.getLocalizedMessage() } )); //$NON-NLS-1$
		}
	}

	/**
	 * @param stringUrl
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws Exception
	 */
	public static void openBrowser(String stringUrl) {
		try {
			if (GDE.IS_WINDOWS) {
				Runtime.getRuntime().exec("rundll32.exe url.dll,FileProtocolHandler " + stringUrl); //$NON-NLS-1$
			}
			else if (GDE.IS_LINUX){				
				Runtime.getRuntime().exec("xdg-open" + GDE.STRING_BLANK + stringUrl);
			}
			else if (GDE.IS_MAC) {
		 		Runtime.getRuntime().exec("open" + GDE.STRING_BLANK + stringUrl);
		 }
			else {
				throw new Exception(Messages.getString(MessageIds.GDE_MSGE0020, new Object[] {System.getProperty(GDE.STRING_OS_NAME)} )); 
			}

		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE0021, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
		}
	}
}
