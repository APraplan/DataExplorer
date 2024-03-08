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
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import gde.GDE;
import gde.log.Level;
import gde.ui.DataExplorer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * class to execute a installed and registered application
 */
public class ApplicationLauncher {
	private final static Logger	log	= Logger.getLogger(ApplicationLauncher.class.getName());
	
	String fqExecPath = GDE.STRING_EMPTY;

	/**
	 * constructor where the path to the executable will be figured out if not given
	 */
	public ApplicationLauncher(String executable, String[] searchExecutableKeys, String searchLocationInfo) {
		try {
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				for (String searchExecutableKey : searchExecutableKeys) {
					sb.append(searchExecutableKey).append(GDE.STRING_COMMA);
				}
				log.log(Level.FINE, "searchExecutableKeys = '" + sb.toString() + GDE.STRING_SINGLE_QUOAT); //$NON-NLS-1$
			}

			//prepare search keys
			Vector<String> searchKeyVector = new Vector<String>();
			for (String tmpSearchExecutableKey : searchExecutableKeys) {
				if (tmpSearchExecutableKey != null && tmpSearchExecutableKey.length() > 4) {
					tmpSearchExecutableKey = tmpSearchExecutableKey.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
					if (tmpSearchExecutableKey.contains(GDE.STRING_FILE_SEPARATOR_UNIX)) {
						tmpSearchExecutableKey = tmpSearchExecutableKey.substring(tmpSearchExecutableKey.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX) + 1);
					}
					searchKeyVector.add(tmpSearchExecutableKey);
				}
			}

			if (GDE.IS_WINDOWS) {
				for (String tmpSearchExecutableKey : searchKeyVector) {
					String path = WindowsHelper.findApplicationPath(tmpSearchExecutableKey);
					if (path.length() > 4) {
						path = path.substring(0, path.toLowerCase().indexOf(GDE.FILE_ENDING_EXE) + 4);
						if (path.startsWith("\"")) { //$NON-NLS-1$
							path = path.substring(1);
						}
						log.log(Level.FINE, "executable = " + path);
						break;
					}
					log.log(Level.WARNING, "failed find executable according key = " + tmpSearchExecutableKey + " method: " + searchLocationInfo);
				}
			}
			else if (GDE.IS_LINUX) {
				//String[] browsers = { "firefox", "konqueror", "opera", "epiphany", "mozilla", "netscape" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				//String browser = null;
				//for (int count = 0; count < browsers.length && browser == null; count++) {
				//	if (Runtime.getRuntime().exec(new String[] { "which", browsers[count] }).waitFor() == 0) {
				//		browser = browsers[count]; //$NON-NLS-1$
				//		break;
				//	}
				//}
				//if (browser == null)
				//	throw new Exception(Messages.getString(MessageIds.GDE_MSGE0019, new Object[]
				//			{ "firefox, konqueror, opera, epiphany, mozilla, netscape" } )); //$NON-NLS-1$

				for (String tmpSearchExecutableKey : searchKeyVector) {
					List<String> command = new ArrayList<String>();
					command.add("which");//$NON-NLS-1$
					command.add(tmpSearchExecutableKey);
					Process process = new ProcessBuilder(command).start(); //$NON-NLS-1$ //$NON-NLS-2$
					process.waitFor();
					BufferedReader bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
					BufferedReader besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = bisr.readLine()) != null) {
						log.log(Level.FINEST, "std.out = " + line); //$NON-NLS-1$
						sb.append(line);
					}
					while ((line = besr.readLine()) != null) {
						log.log(Level.FINEST, "std.err = " + line); //$NON-NLS-1$
					}
					if (process.exitValue() != 0) {
						log.log(Level.WARNING, "failed find executable according key = " + tmpSearchExecutableKey + " method: " + searchLocationInfo);
					}
					besr.close();
					bisr.close();

					if (process.exitValue() == 0 && sb.length() > 4) {
						log.log(Level.FINE, "executable = " + sb.toString());
						break;
					}
				}
			}
			else if (GDE.IS_MAC) {
				for (String tmpSearchExecutableKey : searchKeyVector) {
					if (tmpSearchExecutableKey.toLowerCase().endsWith(GDE.STRING_MAC_DOT_APP)) {
						tmpSearchExecutableKey = tmpSearchExecutableKey.substring(0, tmpSearchExecutableKey.indexOf(GDE.STRING_MAC_DOT_APP));
					}
					String appDirectory = searchLocationInfo;
					if ((new File(appDirectory)).exists()) {
						String macExecPath = appDirectory + GDE.STRING_MAC_APP_EXE_PATH + tmpSearchExecutableKey;
						if (FileUtils.checkFileExist(macExecPath)) {
							log.log(Level.FINE, "executable = " + macExecPath);
							break;
						}
					}
					else
						log.log(Level.WARNING, "failed find executable according key = " + tmpSearchExecutableKey + " method: " + searchLocationInfo);
				}
			}
			fqExecPath = executable;
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialogAsync(e.getMessage());
		}
	}
	
	/**
	 * execute the initialized executable with the given arguments
	 * @param arguments
	 */
	public void execute(final List<String> arguments) {
		if (this.isLaunchable()) {
			arguments.add(0, this.fqExecPath);
			if (GDE.IS_WINDOWS) {
				arguments.add(1, "url.dll,FileProtocolHandler");
			}
			for (String string : arguments) {
				log.log(Level.INFO, GDE.STRING_SINGLE_QUOAT + string + GDE.STRING_SINGLE_QUOAT);
			}

			try {
				StringBuilder sb = new StringBuilder();
				boolean isBeginParam = true;
				for (String argument : arguments) {
					sb.append(argument).append(GDE.STRING_BLANK);
					if (isBeginParam) {
						sb.append(GDE.STRING_SINGLE_QUOAT);
						isBeginParam = false;
					}
				}
				sb.deleteCharAt(sb.length()-1).append(GDE.STRING_SINGLE_QUOAT);
				log.log(Level.FINE, sb.toString());
				
				new ProcessBuilder(arguments.toArray(new String[1])).start();
			}
			catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
				DataExplorer.getInstance().openMessageDialogAsync(e.getMessage());
			}
		}
	}

	/**
	 * @return true if full qualified file path length >= 4
	 */
	public boolean isLaunchable() {
		return this.fqExecPath.length() >= 4;
	}
}
