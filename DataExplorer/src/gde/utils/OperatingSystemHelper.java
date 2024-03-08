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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.device.IDevice;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * Utility class with helpers around operating system
 * @author Winfried Brügmann
 */
public class OperatingSystemHelper {
	private static final Logger	log									= Logger.getLogger(OperatingSystemHelper.class.getName());
	private static final String	STRING_LINK_POINTER	= " -> ";																									//$NON-NLS-1$

	/**
	 * create desktop shortcut to launch the main jar
	 */
	public static boolean createDesktopLink() {
		boolean isCreated = false;
		URL url = FileUtils.class.getProtectionDomain().getCodeSource().getLocation();
		String sourceLaunchFilePath, targetDesktopLaucherFilePath;
		String sourceBasePath = url.getPath(), targetBasePath;

		if (url.getPath().endsWith(GDE.STRING_FILE_SEPARATOR_UNIX)) { // running inside Eclipse
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "started inside Eclipse -> skip creation of shortcut"); //$NON-NLS-1$
		}
		else {
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "started outside with: java -jar *.jar"); //$NON-NLS-1$

			if (GDE.IS_WINDOWS) {
				try {
					String launchFilename = "DataExplorer.exe"; //$NON-NLS-1$
					sourceBasePath = sourceBasePath.substring(1, sourceBasePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX) + 1).replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK); 
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "sourceBasePath = " + sourceBasePath); //$NON-NLS-1$
					sourceLaunchFilePath = (sourceBasePath + launchFilename);
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "sourceLaunchFilePath = " + sourceLaunchFilePath); //$NON-NLS-1$
					targetBasePath = System.getenv("USERPROFILE") + GDE.STRING_FILE_SEPARATOR_UNIX + "Desktop" + GDE.STRING_FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
					targetDesktopLaucherFilePath = targetBasePath + "DataExplorer.lnk"; //$NON-NLS-1$
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "fqShellLinkPath = " + targetDesktopLaucherFilePath); //$NON-NLS-1$
					String fqExecutablePath = sourceLaunchFilePath.replace("/", GDE.FILE_SEPARATOR); //$NON-NLS-1$
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "fqExecutablePath = " + fqExecutablePath); //$NON-NLS-1$
					String executableArguments = GDE.STRING_EMPTY; //exe wrapper dont need arguments - "-jar -Xms40M -Xmx256M \"" + sourceLaunchFilePath + "jar\""; 
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "executableArguments = " + executableArguments); //$NON-NLS-1$
					String workingDirectory = sourceBasePath.replace("/", GDE.FILE_SEPARATOR); //$NON-NLS-1$
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "workingDirectory = " + workingDirectory); //$NON-NLS-1$
					String fqIconPath = fqExecutablePath; // exe wrapper will contain icon - sourceLaunchFilePath + "ico";
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "fqIconPath = " + fqIconPath); //$NON-NLS-1$
					String description = Messages.getString(MessageIds.GDE_MSGT0000);
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "description = " + description); //$NON-NLS-1$

					String[] shellLinkArgs = new String[] { targetDesktopLaucherFilePath, fqExecutablePath, executableArguments, workingDirectory, fqIconPath, description };
					WindowsHelper.createDesktopLink(shellLinkArgs[0], shellLinkArgs[1], shellLinkArgs[2], shellLinkArgs[3], shellLinkArgs[4], 0, shellLinkArgs[5]);

					sourceBasePath = url.getPath();
					launchFilename = "DevicePropertiesEditor.exe"; //$NON-NLS-1$
					sourceBasePath = sourceBasePath.substring(1, sourceBasePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX) + 1).replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK); 
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "sourceBasePath = " + sourceBasePath); //$NON-NLS-1$
					sourceLaunchFilePath = (sourceBasePath + launchFilename);
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "sourceLaunchFilePath = " + sourceLaunchFilePath); //$NON-NLS-1$
					targetBasePath = System.getenv("USERPROFILE") + GDE.STRING_FILE_SEPARATOR_UNIX + "Desktop" + GDE.STRING_FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
					targetDesktopLaucherFilePath = targetBasePath + "DeviceProperties Editor.lnk"; //$NON-NLS-1$
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "fqShellLinkPath = " + targetDesktopLaucherFilePath); //$NON-NLS-1$
					fqExecutablePath = sourceLaunchFilePath.replace("/", GDE.FILE_SEPARATOR); //$NON-NLS-1$
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "fqExecutablePath = " + fqExecutablePath); //$NON-NLS-1$
					executableArguments = GDE.STRING_EMPTY; //exe wrapper dont need arguments - "-jar -Xms40M -Xmx256M \"" + sourceLaunchFilePath + "jar\""; 
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "executableArguments = " + executableArguments); //$NON-NLS-1$
					fqIconPath = fqExecutablePath; // exe wrapper will contain icon - sourceLaunchFilePath + "ico";
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "fqIconPath = " + fqIconPath); //$NON-NLS-1$
					description = Messages.getString(MessageIds.GDE_MSGT0595);
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "description = " + description); //$NON-NLS-1$

					shellLinkArgs = new String[] { targetDesktopLaucherFilePath, fqExecutablePath, executableArguments, workingDirectory, fqIconPath, description };
					WindowsHelper.createDesktopLink(shellLinkArgs[0], shellLinkArgs[1], shellLinkArgs[2], shellLinkArgs[3], shellLinkArgs[4], 0, shellLinkArgs[5]);
					isCreated = true;
				}
				catch (Throwable e) {
					log.log(Level.WARNING, e.getMessage());
				}
			}
			else if (GDE.IS_LINUX) { 
				try {
					sourceBasePath = sourceBasePath.substring(0, sourceBasePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX) + 1);
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "sourceBasePath = " + sourceBasePath); //$NON-NLS-1$

					String desktopFileName = "DataExplorer.desktop"; //$NON-NLS-1$
					String extractTargetFilePath = sourceBasePath + desktopFileName;
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "extractTargetFilePath = " + extractTargetFilePath); //$NON-NLS-1$
					File targetFile = new File(extractTargetFilePath);

					//installation directory must contain DataExplorer.desktop with write permission
					if (targetFile.exists() && targetFile.canWrite()) {
						String jarFilePath = sourceBasePath + "DataExplorer.jar"; //$NON-NLS-1$
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "jarFilePath = " + jarFilePath); //$NON-NLS-1$

						FileUtils.extractWhileReplace("@GDE_DIR@", sourceBasePath, jarFilePath, desktopFileName, extractTargetFilePath, GDE.STRING_UTF_8, GDE.STRING_UTF_8); //$NON-NLS-1$ 

						targetBasePath = System.getenv("HOME") + GDE.STRING_FILE_SEPARATOR_UNIX + "Desktop" + GDE.STRING_FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "targetBasePath = " + targetBasePath); //$NON-NLS-1$
						targetDesktopLaucherFilePath = targetBasePath + desktopFileName;
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "targetDesktopLaucherFilePath = " + targetDesktopLaucherFilePath); //$NON-NLS-1$
						FileUtils.copyFile(new File(extractTargetFilePath), new File(targetDesktopLaucherFilePath));
					}
					else {
						log.log(Level.WARNING, extractTargetFilePath + " does not exist or does not have write (755) pernission, a desktop launcher can not created"); //$NON-NLS-1$
					}

					desktopFileName = "DevicePropertiesEditor.desktop"; //$NON-NLS-1$
					extractTargetFilePath = sourceBasePath + desktopFileName;
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "extractTargetFilePath = " + extractTargetFilePath); //$NON-NLS-1$
					targetFile = new File(extractTargetFilePath);

					//installation directory must contain DevicePropertiesEditor.desktop with write permission
					if (targetFile.exists() && targetFile.canWrite()) {
						String jarFilePath = sourceBasePath + "DataExplorer.jar"; //$NON-NLS-1$
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "jarFilePath = " + jarFilePath); //$NON-NLS-1$

						FileUtils.extractWhileReplace("@GDE_DIR@", sourceBasePath, jarFilePath, desktopFileName, extractTargetFilePath, GDE.STRING_UTF_8, GDE.STRING_UTF_8); //$NON-NLS-1$ 

						targetBasePath = System.getenv("HOME") + GDE.STRING_FILE_SEPARATOR_UNIX + "Desktop" + GDE.STRING_FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "targetBasePath = " + targetBasePath); //$NON-NLS-1$
						targetDesktopLaucherFilePath = targetBasePath + desktopFileName;
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "targetDesktopLaucherFilePath = " + targetDesktopLaucherFilePath); //$NON-NLS-1$
						FileUtils.copyFile(new File(extractTargetFilePath), new File(targetDesktopLaucherFilePath));
					}
					else {
						log.log(Level.WARNING, extractTargetFilePath + " does not exist or does not have write (755) pernission, a desktop launcher can not created"); //$NON-NLS-1$
					}
					isCreated = true;
				}
				catch (UnsupportedEncodingException e) {
					log.log(Level.WARNING, e.getMessage());
				}
				catch (FileNotFoundException e) {
					log.log(Level.WARNING, e.getMessage());
				}
				catch (IOException e) {
					log.log(Level.WARNING, e.getMessage());
				}
				catch (Throwable e) {
					log.log(Level.WARNING, e.getMessage());
				}
			}
			// OPET - start - add
			else if (GDE.IS_MAC) { 
				isCreated = true;
				log.log(Level.INFO, "No desktop link for OS " + System.getProperty(GDE.STRING_OS_NAME)); //$NON-NLS-1$
			}
			// OPET - end
			else {
				log.log(Level.WARNING, "not supported OS"); //$NON-NLS-1$
				// OPET - start - change
				DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGW0032, new Object[] { "createDesktopLink", System.getProperty(GDE.STRING_OS_NAME) }));
				// OPET - end
				isCreated = true;
			}
		}
		if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "DataExplorer desktop created = " + isCreated); //$NON-NLS-1$
		return isCreated;
	}

	/**
	 * remove desktop shortcut to launch the main jar
	 */
	public static boolean removeDesktopLink() {
		boolean isRemoved = false;
		String targetBasePath, targetDesktopLaucherFilePath;

		try {
			if (GDE.IS_WINDOWS) {
				targetBasePath = System.getenv("USERPROFILE") + GDE.STRING_FILE_SEPARATOR_WINDOWS + "Desktop" + GDE.STRING_FILE_SEPARATOR_WINDOWS; //$NON-NLS-1$ //$NON-NLS-2$
				targetDesktopLaucherFilePath = targetBasePath + "DataExplorer.lnk"; //$NON-NLS-1$
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "fqShellLinkPath = " + targetDesktopLaucherFilePath); //$NON-NLS-1$

				Process process = new ProcessBuilder("cmd", "/C", "erase", "/F", targetDesktopLaucherFilePath).start(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				process.waitFor();
				if (process.exitValue() != 0) {
					log.log(Level.WARNING, "failed to remove desktop launcher " + targetBasePath); //$NON-NLS-1$
				}

				targetDesktopLaucherFilePath = targetBasePath + "DeviceProperties*.lnk"; //$NON-NLS-1$
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "fqShellLinkPath = " + targetDesktopLaucherFilePath); //$NON-NLS-1$

				process = new ProcessBuilder("cmd", "/C", "erase", "/F", targetDesktopLaucherFilePath).start(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				process.waitFor();
				if (process.exitValue() != 0) {
					log.log(Level.WARNING, "failed to remove desktop launcher " + targetBasePath); //$NON-NLS-1$
				}

				isRemoved = true;
			}
			else if (GDE.IS_LINUX) {
				String desktopFileName = "DataExplorer.desktop"; //$NON-NLS-1$
				targetBasePath = System.getenv("HOME") + GDE.STRING_FILE_SEPARATOR_UNIX + "Desktop" + GDE.STRING_FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
				log.log(Level.INFO, "targetBasePath = " + targetBasePath); //$NON-NLS-1$
				targetDesktopLaucherFilePath = targetBasePath + desktopFileName;
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "targetDesktopLaucherFilePath = " + targetDesktopLaucherFilePath); //$NON-NLS-1$

				Process process = new ProcessBuilder("rm", "-f", targetDesktopLaucherFilePath).start(); //$NON-NLS-1$ //$NON-NLS-2$
				process.waitFor();
				if (process.exitValue() != 0) {
					log.log(Level.WARNING, "failed to remove desktop launcher " + desktopFileName); //$NON-NLS-1$
				}

				desktopFileName = "DevicePropertiesEditor.desktop"; //$NON-NLS-1$
				targetBasePath = System.getenv("HOME") + GDE.STRING_FILE_SEPARATOR_UNIX + "Desktop" + GDE.STRING_FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "targetBasePath = " + targetBasePath); //$NON-NLS-1$
				targetDesktopLaucherFilePath = targetBasePath + desktopFileName;
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "targetDesktopLaucherFilePath = " + targetDesktopLaucherFilePath); //$NON-NLS-1$

				process = new ProcessBuilder("rm", "-f", targetDesktopLaucherFilePath).start(); //$NON-NLS-1$ //$NON-NLS-2$
				process.waitFor();
				if (process.exitValue() != 0) {
					log.log(Level.WARNING, "failed to remove desktop launcher " + desktopFileName); //$NON-NLS-1$
				}

				isRemoved = true;
			}
			else {
				DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGW0034, new Object[] { System.getProperty(GDE.STRING_OS_NAME) }));
			}
		}
		catch (Throwable e) {
			log.log(Level.WARNING, e.getMessage());
		}
		return isRemoved;
	}

	/**
	 * register the application to the running OS
	 * associate file ending .osd as shell support
	 * @return true for successful registration
	 */
	public static boolean registerApplication() {
		int rc = -1;
		String targetDir = GDE.JAVA_IO_TMPDIR;
		String command = GDE.STRING_BLANK;

		BufferedReader besr = null;
		BufferedReader bisr = null;

		try {
			URL url = FileUtils.class.getProtectionDomain().getCodeSource().getLocation();

			if (url.getPath().endsWith(GDE.STRING_FILE_SEPARATOR_UNIX)) { // running inside Eclipse
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "started inside Eclipse -> skip creation of shortcut"); //$NON-NLS-1$
			}
			else {
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "started outside with: java -jar *.jar"); //$NON-NLS-1$

				String jarBasePath = FileUtils.getJarBasePath();
				String jarFilePath = jarBasePath + "/DataExplorer.jar"; //$NON-NLS-1$

				JarFile jarFile = new JarFile(jarFilePath);

				if (GDE.IS_WINDOWS) {
					// warn user for UAC or fail due to required admin rights accessing registry
					// enable this step to skip 
					if (SWT.OK == DataExplorer.getInstance().openOkCancelMessageDialog(Messages.getString(MessageIds.GDE_MSGI0029))) {
						String regExe = "Register" + GDE.BIT_MODE + ".exe"; //$NON-NLS-1$ //$NON-NLS-2$
						log.log(Level.INFO, "register exe = " + regExe); //$NON-NLS-1$
		
						FileUtils.extract(jarFile, regExe, GDE.STRING_EMPTY, targetDir, "WIN"); //$NON-NLS-1$
						String targetBasePath = jarBasePath.replace(GDE.CHAR_FILE_SEPARATOR_UNIX, GDE.CHAR_FILE_SEPARATOR_WINDOWS);
						targetBasePath = targetBasePath.startsWith(GDE.STRING_FILE_SEPARATOR_WINDOWS) ? targetBasePath.substring(1) : targetBasePath;
						targetBasePath = targetBasePath.endsWith(GDE.STRING_FILE_SEPARATOR_WINDOWS) ? targetBasePath.substring(0, targetBasePath.length() - 1) : targetBasePath;
						command = "cmd /C " + targetDir + regExe + GDE.STRING_BLANK + targetBasePath; //$NON-NLS-1$
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$
						Process process = new ProcessBuilder("cmd", "/C", targetDir + regExe, targetBasePath).start(); //$NON-NLS-1$ //$NON-NLS-2$
						process.waitFor();
						bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
						besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
						String line;
		
						StringBuilder sb = new StringBuilder();
						while ((line = bisr.readLine()) != null) {
							sb.append(line);
						}
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "std.out = " + sb.toString()); //$NON-NLS-1$
						sb = new StringBuilder();
						while ((line = besr.readLine()) != null) {
							sb.append(line);
						}
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "std.err = " + sb.toString()); //$NON-NLS-1$
						if (process.exitValue() != 0) {
							String msg = "failed to execute \"" + command + "\" rc = " + process.exitValue(); //$NON-NLS-1$ //$NON-NLS-2$
							log.log(Level.SEVERE, msg);
							if (msg.contains("740")) //$NON-NLS-1$
								throw new IOException("error=740"); //$NON-NLS-1$
		
							throw new UnsatisfiedLinkError(msg);
						}
						bisr.close();
						besr.close();
		
						//check if registration was successful
						command = "cmd /C assoc .osd"; //$NON-NLS-1$
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "executing \"" + command + "\" to check association"); //$NON-NLS-1$ //$NON-NLS-2$
						process = Runtime.getRuntime().exec(command);
						process.waitFor();
						bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
						besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		
						sb = new StringBuilder();
						while ((line = bisr.readLine()) != null) {
							sb.append(line);
						}
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "std.out = " + sb.toString()); //$NON-NLS-1$
						sb = new StringBuilder();
						while ((line = besr.readLine()) != null) {
							sb.append(line);
						}
						if (log.isLoggable(Level.INFO)) {
							log.log(Level.INFO, "std.err = " + sb.toString()); //$NON-NLS-1$
							log.log(Level.INFO, "\"" + command + "\" rc = " + process.exitValue()); //$NON-NLS-1$ //$NON-NLS-2$
						}
						if (process.exitValue() != 0) {
							log.log(Level.WARNING, "failed to register DataExplorer MIME type rc = " + process.exitValue()); //$NON-NLS-1$
							throw new IOException("error=740"); //$NON-NLS-1$
						}
					}
					rc = 0;
				}
				else if (GDE.IS_LINUX) {
					String desktopFileName = "DataExplorer.desktop"; //$NON-NLS-1$
					String extractTargetFilePath = jarBasePath + desktopFileName;
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "extractTargetFilePath = " + extractTargetFilePath); //$NON-NLS-1$
					File targetFile = new File(extractTargetFilePath);
					//installation directory must contain DataExplorer.desktop with write permission
					if (targetFile.exists() && targetFile.canWrite()) {
						FileUtils.extractWhileReplace("@GDE_DIR@", jarBasePath, jarFilePath, desktopFileName, extractTargetFilePath, "UTF-8", "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$);

						desktopFileName = "DevicePropertiesEditor.desktop"; //$NON-NLS-1$
						extractTargetFilePath = jarBasePath + desktopFileName;
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "extractTargetFilePath = " + extractTargetFilePath); //$NON-NLS-1$
						targetFile = new File(extractTargetFilePath);
						//installation directory must contain DataExplorer.desktop with write permission
						if (targetFile.exists() && targetFile.canWrite()) {
							FileUtils.extractWhileReplace("@GDE_DIR@", jarBasePath, jarFilePath, desktopFileName, extractTargetFilePath, "UTF-8", "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$);
						}
						else {
							// package error, must not occur in a deliverd driver
							log.log(Level.WARNING, extractTargetFilePath + " does not exist or does not have write (755) pernission, the DataExplorer MIME-type can not registered"); //$NON-NLS-1$
						}
					}
					else {
						// package error, must not occur in a deliverd driver
						log.log(Level.WARNING, extractTargetFilePath + " does not exist or does not have write (755) pernission, the DataExplorer MIME-type can not registered"); //$NON-NLS-1$
					}

					//check if xdg-utls are installed, this is the prerequisite for the registration process
					if (Runtime.getRuntime().exec("which xdg-mime").waitFor() != 0) { //$NON-NLS-1$
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "DataExplorer program can not registered until xdg-utils are installed and in path"); //$NON-NLS-1$
						DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0030));
						rc = 0;
					}
					else {
						FileUtils.extract(jarFile, "register.sh", GDE.STRING_EMPTY, targetDir, "555"); //$NON-NLS-1$ //$NON-NLS-2$ 

						// all files extracted, exec register command
						command = "chmod +x " + targetDir + "/register.sh"; //$NON-NLS-1$ //$NON-NLS-2$
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$
						Runtime.getRuntime().exec(command).waitFor();
						command = targetDir + "/register.sh"; //$NON-NLS-1$
						if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$
						rc = Runtime.getRuntime().exec(command).waitFor();

					}
				}
				else if (GDE.IS_MAC) { 
					DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0031));
					rc = 0;
				}
				else {
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Unsupported OS, shell integration, MIME registration NOT IMPLEMENTED"); //$NON-NLS-1$
					DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGW0024, new Object[] { System.getProperty(GDE.STRING_OS_NAME) }));
					rc = 0;
				}
			}
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			if (e.getMessage().contains("error=740") || e instanceof IOException) { //permission access exception //$NON-NLS-1$
				DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGW0023, new Object[] { command }));
				rc = 0;
			}
			else if (e instanceof UnsatisfiedLinkError) {
				DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0037, new Object[] { e.getClass().getSimpleName() }) + Messages.getString(MessageIds.GDE_MSGI0033));
			}
			else {
				DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0039));
			}
		}
		finally {
			try {
				if (bisr != null) bisr.close();
				if (besr != null) besr.close();
			}
			catch (IOException e) {
				// ignore
			}
		}
		if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "DataExplorer MIME registered = " + (rc == 0)); //$NON-NLS-1$
		return rc == 0;
	}

	/**
	 * register the application to the running OS
	 * associate file ending .osd as shell support
	 * @return true for successful registration
	 */
	public static boolean deregisterApplication() {
		int rc = -1;
		String targetDir = GDE.JAVA_IO_TMPDIR;
		String command = GDE.STRING_BLANK;

		String jarBasePath = FileUtils.getJarBasePath();
		String jarFilePath = jarBasePath + "/DataExplorer.jar"; //$NON-NLS-1$

		BufferedReader besr = null;
		BufferedReader bisr = null;

		try {
			JarFile jarFile = new JarFile(jarFilePath);

			if (GDE.IS_WINDOWS) {
				// warn user for UAC or fail due to required admin rights accessing registry
				try {
					DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0029));
				}
				catch (Exception e) {
					//enable suppress this warning while cleaning settings
				}
				String regExe = "Register" + GDE.BIT_MODE + ".exe"; //$NON-NLS-1$ //$NON-NLS-2$
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "register exe = " + regExe); //$NON-NLS-1$

				FileUtils.extract(jarFile, regExe, GDE.STRING_EMPTY, targetDir, "WIN"); //$NON-NLS-1$
				command = "cmd /C " + targetDir + regExe; //$NON-NLS-1$
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$
				Process process = new ProcessBuilder("cmd", "/C", targetDir + regExe).start(); //$NON-NLS-1$ //$NON-NLS-2$
				process.waitFor();
				bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
				besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String line;

				StringBuilder sb = new StringBuilder();
				while ((line = bisr.readLine()) != null) {
					sb.append(line);
				}
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "std.out = " + sb.toString()); //$NON-NLS-1$
				sb = new StringBuilder();
				while ((line = besr.readLine()) != null) {
					sb.append(line);
				}
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "std.err = " + sb.toString()); //$NON-NLS-1$
				if (process.exitValue() != 0) {
					String msg = "failed to execute \"" + command + "\" rc = " + process.exitValue(); //$NON-NLS-1$ //$NON-NLS-2$
					log.log(Level.SEVERE, msg);
					if (msg.contains("740")) //$NON-NLS-1$
						throw new IOException("error=740"); //$NON-NLS-1$

					throw new UnsatisfiedLinkError(msg);
				}
				bisr.close();
				besr.close();

				//check if deregistration was successful
				process = Runtime.getRuntime().exec("cmd /C assoc .osd"); //$NON-NLS-1$
				process.waitFor();
				bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
				besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

				sb = new StringBuilder();
				while ((line = bisr.readLine()) != null) {
					sb.append(line);
				}
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "std.out = " + sb.toString()); //$NON-NLS-1$
				sb = new StringBuilder();
				while ((line = besr.readLine()) != null) {
					sb.append(line);
				}
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "std.err = " + sb.toString()); //$NON-NLS-1$
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "\"cmd /C assoc .osd\" rc = " + process.exitValue()); //$NON-NLS-1$
				if (process.exitValue() == 0) {
					log.log(Level.WARNING, "failed to deregister DataExplorer MIME type to OS rc = " + process.exitValue()); //$NON-NLS-1$
					throw new IOException("error=740"); //$NON-NLS-1$
				}
				rc = 0;
			}
			else if (GDE.IS_LINUX) {
				String desktopFileName = "DataExplorer.desktop"; //$NON-NLS-1$
				String extractTargetFilePath = jarBasePath + desktopFileName;
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "extractTargetFilePath = " + extractTargetFilePath); //$NON-NLS-1$
				File targetFile = new File(extractTargetFilePath);
				//installation directory must contain DataExplorer.desktop with write permission
				if (targetFile.exists() && targetFile.canWrite()) {
					FileUtils.extractWhileReplace("@GDE_DIR@", jarBasePath, jarFilePath, desktopFileName, extractTargetFilePath, "UTF-8", "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$);

					desktopFileName = "DevicePropertiesEditor.desktop"; //$NON-NLS-1$
					extractTargetFilePath = jarBasePath + desktopFileName;
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "extractTargetFilePath = " + extractTargetFilePath); //$NON-NLS-1$
					targetFile = new File(extractTargetFilePath);
					//installation directory must contain DataExplorer.desktop with write permission
					if (targetFile.exists() && targetFile.canWrite()) {
						FileUtils.extractWhileReplace("@GDE_DIR@", jarBasePath, jarFilePath, desktopFileName, extractTargetFilePath, "UTF-8", "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$);
					}
					else {
						// package error, must not occur in a deliverd driver
						log.log(Level.WARNING, extractTargetFilePath + " does not exist or does not have write (755) pernission, the DataExplorer MIME-type can not registered"); //$NON-NLS-1$
					}
				}
				else {
					// package error, must not occur in a deliverd driver
					log.log(Level.WARNING, extractTargetFilePath + " does not exist or does not have write (755) pernission, the DataExplorer MIME-type can not registered"); //$NON-NLS-1$
				}

				//check if xdg-utls are installed, this is the prerequisite for the registration process
				if (Runtime.getRuntime().exec("which xdg-mime").waitFor() != 0) { //$NON-NLS-1$
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "DataExplorer program can not registered until xdg-utils are installed and in path"); //$NON-NLS-1$
					DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0030));
					rc = 0;
				}
				else {
					FileUtils.extract(jarFile, "unregister.sh", GDE.STRING_EMPTY, targetDir, "555"); //$NON-NLS-1$ //$NON-NLS-2$ 

					command = "chmod +x " + targetDir + "/unregister.sh"; //$NON-NLS-1$ //$NON-NLS-2$
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$
					Runtime.getRuntime().exec(command).waitFor();
					command = targetDir + "/unregister.sh"; //$NON-NLS-1$
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$
					rc = Runtime.getRuntime().exec(command).waitFor();
				}
			}
			else {
				DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0032, new Object[] { System.getProperty(GDE.STRING_OS_NAME) }));
			}
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			if (e.getMessage().contains("error=740") || e instanceof IOException) { //permission access exception //$NON-NLS-1$
				DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGW0023, new Object[] { command }));
				rc = 0;
			}
			else if (e instanceof UnsatisfiedLinkError) {
				DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0037, new Object[] { e.getClass().getSimpleName() }) + Messages.getString(MessageIds.GDE_MSGI0033));
			}
			else {
				DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0039));
			}
		}
		finally {
			try {
				if (bisr != null) bisr.close();
				if (besr != null) besr.close();
			}
			catch (IOException e) {
				// ignore
			}
		}
		return rc == 0;
	}

	/**
	 * create a file link
	 */
	public static void createFileLink(String fullQualifiedSourceFilePath, String fullQualifiedTargetFilePath) {

		try {
			//check if a file with this name already exist
			if (FileUtils.checkFileExist(fullQualifiedTargetFilePath)) {
				if (SWT.NO == DataExplorer.getInstance().openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0062, new Object[] { fullQualifiedTargetFilePath })))
					return;
				// check if the target file contained is the same link target
				String existingSourcePath = OperatingSystemHelper.getLinkContainedFilePath(fullQualifiedTargetFilePath).replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
				if (!existingSourcePath.equals(fullQualifiedSourceFilePath)) {
					fullQualifiedTargetFilePath = fullQualifiedTargetFilePath.substring(0, fullQualifiedTargetFilePath.length() - 4) + GDE.STRING_UNDER_BAR + GDE.FILE_ENDING_DOT_OSD;
					DataExplorer.getInstance()
							.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGW0033, new Object[] { fullQualifiedTargetFilePath, existingSourcePath, existingSourcePath, fullQualifiedTargetFilePath }));
				}
			}

			if (GDE.IS_WINDOWS) {
				try {
					fullQualifiedSourceFilePath = fullQualifiedSourceFilePath.replace(GDE.CHAR_FILE_SEPARATOR_UNIX, GDE.CHAR_FILE_SEPARATOR_WINDOWS);
					fullQualifiedTargetFilePath = fullQualifiedTargetFilePath.replace(GDE.CHAR_FILE_SEPARATOR_UNIX, GDE.CHAR_FILE_SEPARATOR_WINDOWS);
					String sourceBasePath = fullQualifiedSourceFilePath.substring(0, fullQualifiedSourceFilePath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_WINDOWS) + 1);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "sourceBasePath = " + sourceBasePath); //$NON-NLS-1$

					String targetFileLinkPath = fullQualifiedTargetFilePath.replace(GDE.CHAR_FILE_SEPARATOR_UNIX, GDE.CHAR_FILE_SEPARATOR_WINDOWS); // + ".lnk"; 
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "targetFileLinkPath = " + targetFileLinkPath); //$NON-NLS-1$

					String[] shellLinkArgs = { targetFileLinkPath, fullQualifiedSourceFilePath, GDE.STRING_EMPTY, sourceBasePath, fullQualifiedSourceFilePath, GDE.STRING_EMPTY };

					WindowsHelper.createDesktopLink(shellLinkArgs[0], shellLinkArgs[1], shellLinkArgs[2], shellLinkArgs[3], shellLinkArgs[4], 0, shellLinkArgs[5]);
				}
				catch (Throwable e) {
					log.log(Level.WARNING, e.getMessage());
				}
			}
			else if (GDE.IS_LINUX) { 
				try {
					String fullQualifiedLinkTargetPath = fullQualifiedSourceFilePath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
					String fullQualifiedLinkPath = fullQualifiedTargetFilePath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
					String command = "ln -s '" + fullQualifiedLinkTargetPath + "' '" + fullQualifiedLinkPath + "'"; //$NON-NLS-1$
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "executing: " + command); //$NON-NLS-1$

					Process process = new ProcessBuilder("ln", "-s", fullQualifiedLinkTargetPath, fullQualifiedLinkPath).start(); //$NON-NLS-1$ //$NON-NLS-2$
					process.waitFor();
					BufferedReader bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
					BufferedReader besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
					String line;
					while ((line = bisr.readLine()) != null) {
						log.log(Level.FINE, "std.out = " + line); //$NON-NLS-1$
					}
					while ((line = besr.readLine()) != null) {
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "std.err = " + line); //$NON-NLS-1$
					}
					if (process.exitValue() != 0) {
						String msg = "failed to execute \"" + command + "\" rc = " + process.exitValue(); //$NON-NLS-1$ //$NON-NLS-2$
						log.log(Level.SEVERE, msg);
					}
					besr.close();
					bisr.close();
				}
				catch (Throwable e) {
					log.log(Level.WARNING, e.getMessage());
				}
			}
			else if (GDE.IS_MAC) { 
				fullQualifiedSourceFilePath = fullQualifiedSourceFilePath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
				fullQualifiedTargetFilePath = fullQualifiedTargetFilePath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "sourceBasePath = " + fullQualifiedSourceFilePath + ", targetFileLinkPath = " + fullQualifiedTargetFilePath); //$NON-NLS-1$

				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fullQualifiedTargetFilePath), "UTF-8")); //$NON-NLS-1$
				writer.write(fullQualifiedSourceFilePath);
				writer.write(GDE.LINE_SEPARATOR);
				writer.close();
			}
			else {
				log.log(Level.WARNING, "not supported OS"); //$NON-NLS-1$
				DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0035, new Object[] { System.getProperty(GDE.STRING_OS_NAME) }));
			}
		}
		catch (IOException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * check if the given file is a file link
	 * @param filePath
	 * @return true if link file, esle false
	 * @throws IOException
	 */
	public static boolean isLinkContained(String filePath) throws IOException {
		filePath = filePath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
		boolean isFileLink = false;
		String line = GDE.STRING_EMPTY;
		if (GDE.IS_WINDOWS) {
			boolean isZippedFile = false;
			try (ZipInputStream zip_input = new ZipInputStream(new FileInputStream(new File(filePath)))) {
				isZippedFile = zip_input.getNextEntry() != null; 
			}
			if (!isZippedFile) {
				char[] tmpChars = new char[25];
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {//$NON-NLS-1$
					if (reader.read(tmpChars) != 25) log.log(Level.WARNING, "failed reading " + filePath);
				}
				line = new String(tmpChars);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "line = " + line); //$NON-NLS-1$
				if (!line.isEmpty() && !line.contains(GDE.DATA_EXPLORER_FILE) && !line.contains(GDE.LEGACY_OSDE_FILE)) {
					isFileLink = true;
				}
			}
		}
		else if (GDE.IS_LINUX) {
			try {
				String command = "ls -al " + filePath; //$NON-NLS-1$
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "executing: " + command); //$NON-NLS-1$
				Process process = new ProcessBuilder("ls", "-al", filePath).start(); //$NON-NLS-1$ //$NON-NLS-2$
				process.waitFor();
				BufferedReader bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				while ((line = bisr.readLine()) != null) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "std.out = " + line); //$NON-NLS-1$
					if (line.contains(OperatingSystemHelper.STRING_LINK_POINTER)) {
						isFileLink = true;
					}
				}
				while ((line = besr.readLine()) != null) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "std.err = " + line); //$NON-NLS-1$
				}
				if (process.exitValue() != 0) {
					String msg = "failed to execute \"" + command + "\" rc = " + process.exitValue(); //$NON-NLS-1$ //$NON-NLS-2$
					log.log(Level.SEVERE, msg);
				}
				besr.close();
				bisr.close();
			}
			catch (Exception e) {
				log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}

		}
		else if (GDE.IS_MAC) {
			ZipInputStream zip_input = new ZipInputStream(new FileInputStream(new File(filePath)));
			if (zip_input.getNextEntry() == null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8")); //$NON-NLS-1$
				char[] tmpChars = new char[25]; // max path length
				if (25 != reader.read(tmpChars)) log.log(Level.WARNING, "failed to read from " + filePath);
				line = new String(tmpChars);
				log.log(Level.FINE, "line = " + line); //$NON-NLS-1$
				if (!line.contains(GDE.DATA_EXPLORER_FILE) && !line.contains(GDE.LEGACY_OSDE_FILE)) {
					line = line + reader.readLine();
					isFileLink = true;
				}
				reader.close();
			}
			zip_input.close();
		}
		else {
			log.log(Level.WARNING, "Operating System implementation not available"); //$NON-NLS-1$
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isFileLink " + isFileLink);
		return isFileLink;
	}

	/**
	 * check if the given file is a file link, if so it returns the contained file path
	 * @param filePath
	 * @return if shell link file the contained file path is returned, else the given file path is returned
	 * @throws IOException
	 */
	public static String getLinkContainedFilePath(String filePath) throws IOException {
		filePath = filePath.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
		String ret = filePath;
		String line = GDE.STRING_EMPTY;
		if (GDE.IS_WINDOWS) {
			boolean isZippedFile = false;
			try (ZipInputStream zip_input = new ZipInputStream(new FileInputStream(new File(filePath)))) {
				isZippedFile = zip_input.getNextEntry() != null; 
			}
			if (!isZippedFile) {
				char[] tmpChars = new char[25];
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {//$NON-NLS-1$
					if (reader.read(tmpChars) != 25) log.log(Level.WARNING, "failed reading " + filePath);
				}
				line = new String(tmpChars);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "line = " + line); //$NON-NLS-1$
				if (!line.isEmpty() && !line.contains(GDE.DATA_EXPLORER_FILE) && !line.contains(GDE.LEGACY_OSDE_FILE)) {
					log.log(Level.FINE, "source filePath = " + filePath); //$NON-NLS-1$
					ret = WindowsHelper.getFilePathFromLink(filePath);
					log.log(Level.FINE, "returned FilePath = " + ret); //$NON-NLS-1$
					if (ret.startsWith("GDE_MSGE")) { //$NON-NLS-1$
						String msgKey = ret.split(GDE.STRING_SEMICOLON)[0];
						String msgValue = ret.split("; ")[1];
						throw new UnsatisfiedLinkError(filePath + GDE.STRING_MESSAGE_CONCAT + Messages.getString(msgKey, new Object[] { msgValue }));
					}
				}
			}
		}
		else if (GDE.IS_LINUX) {
			try {
				String command = "ls -al " + filePath; //$NON-NLS-1$
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "executing: " + command); //$NON-NLS-1$
				Process process = new ProcessBuilder("ls", "-al", filePath).start(); //$NON-NLS-1$ //$NON-NLS-2$
				process.waitFor();
				BufferedReader bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				while ((line = bisr.readLine()) != null) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "std.out = " + line); //$NON-NLS-1$
					if (line.contains(OperatingSystemHelper.STRING_LINK_POINTER)) {
						ret = line.split(OperatingSystemHelper.STRING_LINK_POINTER)[1].trim();
					}
				}
				while ((line = besr.readLine()) != null) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "std.err = " + line); //$NON-NLS-1$
				}
				if (process.exitValue() != 0) {
					String msg = "failed to execute \"" + command + "\" rc = " + process.exitValue(); //$NON-NLS-1$ //$NON-NLS-2$
					log.log(Level.SEVERE, msg);
				}
				besr.close();
				bisr.close();
			}
			catch (Exception e) {
				log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}

		}
		else if (GDE.IS_MAC) {
			ZipInputStream zip_input = new ZipInputStream(new FileInputStream(new File(filePath)));
			if (zip_input.getNextEntry() == null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8")); //$NON-NLS-1$
				char[] tmpChars = new char[25]; // max path length
				if (25 != reader.read(tmpChars)) log.log(Level.WARNING, "failed to read from " + filePath);
				line = new String(tmpChars);
				log.log(Level.FINE, "line = " + line); //$NON-NLS-1$
				if (!line.contains(GDE.DATA_EXPLORER_FILE) && !line.contains(GDE.LEGACY_OSDE_FILE)) {
					line = line + reader.readLine();
					ret = line.trim();
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "returned FilePath = " + ret); //$NON-NLS-1$
				}
				reader.close();
			}
			zip_input.close();
		}
		else {
			log.log(Level.WARNING, "Operating System implementation not available"); //$NON-NLS-1$
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ret);
		return ret;
	}

	/**
	 * print the actual class loader of Java VM
	 */
	public static String getClasspathAsString() {

		//Get the System Classloader
		ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();

		//Get the URLs
		URL[] urls = ((URLClassLoader) sysClassLoader).getURLs();

		StringBuilder sb = new StringBuilder();
		String pathSeparator = System.getProperty("path.separator");
		for (URL url : urls) {
			sb.append(url.getFile()).append(pathSeparator);
		}
		return sb.toString();
	}

	/**
	 * print the actual class loader of Java VM
	 */
	public static void printClassLoader() {

		//Get the System Classloader
		ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();

		//Get the URLs
		URL[] urls = ((URLClassLoader) sysClassLoader).getURLs();

		StringBuilder sb = new StringBuilder().append("SystemClassLoader = ");
		String pathSeparator = System.getProperty("path.separator");
		for (URL url : urls) {
			sb.append(url.getFile()).append(pathSeparator);
		}
		log.log(Level.INFO, sb.toString());
	}

	/**
	 * check for group uucp membership, by calling 'groups'
	 * @return true if the current user is part of uucp group 
	 */
	public static boolean isUucpMember() {
		boolean isMember = false;
		try {
			String command = "groups"; //$NON-NLS-1$
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "executing: " + command); //$NON-NLS-1$
			Process process = new ProcessBuilder(command).start(); 
			process.waitFor();
			BufferedReader bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String line;
			while ((line = bisr.readLine()) != null) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "std.out = " + line); //$NON-NLS-1$
				if (line.contains("uucp")) {
					isMember = true;
				}
			}
			while ((line = besr.readLine()) != null) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "std.err = " + line); //$NON-NLS-1$
			}
			if (process.exitValue() != 0) {
				String msg = "failed to execute \"" + command + "\" rc = " + process.exitValue(); //$NON-NLS-1$ //$NON-NLS-2$
				log.log(Level.SEVERE, msg);
			}
			besr.close();
			bisr.close();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		return isMember;
	}

	/**
	 * dereference symbolic link using grep command
	 * @param grepChunk
	 * @return
	 */
	public static String dereferenceLink(String directory, String grepChunk) {
		String result = grepChunk;
		try {
			String[] command = { "/bin/sh", "-c", "ls -l " + directory + " | grep " + grepChunk }; //$NON-NLS-1$ //$NON-NLS-2$
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "executing: ls -l " + directory + " | grep " + grepChunk);//$NON-NLS-1$ //$NON-NLS-2$
			Process process = new ProcessBuilder(command).start();
			process.waitFor();
			BufferedReader bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String line;
			while ((line = bisr.readLine()) != null) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "std.out = " + line); //$NON-NLS-1$
				if (line.contains("->")) result = line.substring(0, line.lastIndexOf("->") - 1); //$NON-NLS-2$
			}
			while ((line = besr.readLine()) != null) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "std.err = " + line); //$NON-NLS-1$
			}
			besr.close();
			bisr.close();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		return result;
	}

	public static void launchGoogleEarth(IDevice activeDevice) {
		ApplicationLauncher launcher;
		if (GDE.IS_MAC)
			launcher = new ApplicationLauncher(GDE.STRING_MAC_APP_OPEN, new String[] { "Google Earth" }, GDE.STRING_MAC_APP_BASE_PATH + "Google Earth" + GDE.STRING_MAC_DOT_APP); //$NON-NLS-1$ //$NON-NLS-2$
		else if (GDE.IS_LINUX)
			launcher = new ApplicationLauncher(GDE.STRING_LINUX_APP_OPEN, new String[] { "googleearth", "google-earth" }, "which"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else //GDE.IS_WINDOWS
			launcher = new ApplicationLauncher(GDE.STRING_WINDOWS_APP_OPEN, new String[] { "Google Earth.kmzfile", "Google Earth.kmlfile" }, //$NON-NLS-1$
					"Registry - HKEY_CLASSES_ROOT und HKEY_LOCAL_MACHINE\\SOFTWARE\\Classes");

		if (launcher.isLaunchable()) {
			String kmzFilePath = activeDevice.exportFile(GDE.FILE_ENDING_KMZ, true);
			List<String> argumentList = new ArrayList<String>();
			argumentList.add(kmzFilePath);
			launcher.execute(argumentList);
		}
	}

	public static void launchInstallApplication(final String installablePath) {
		ApplicationLauncher launcher;
		if (GDE.IS_MAC)
			launcher = new ApplicationLauncher(GDE.STRING_MAC_APP_OPEN, new String[] {}, GDE.STRING_EMPTY);
		else if (GDE.IS_LINUX) {
			URL url = GDE.class.getProtectionDomain().getCodeSource().getLocation();
			if (url.getFile().endsWith(GDE.FILE_ENDING_DOT_JAR)) {
				String installpath = url.getFile().substring(0, url.getPath().lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX));
				installpath = installpath.substring(0, installpath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX));
				String command = "cd " + installpath + "\nsudo tar -xzf " + installablePath + "\"";
				DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0055, new String[] { command }));
			}
			launcher = new ApplicationLauncher("bash", new String[] {}, GDE.STRING_EMPTY);
		}
		else //GDE.IS_WINDOWS
			launcher = new ApplicationLauncher(GDE.STRING_WINDOWS_APP_OPEN, new String[] {}, GDE.STRING_EMPTY);

		if (launcher.isLaunchable()) {
			List<String> argumentList = new ArrayList<String>();
			argumentList.add(installablePath);
			launcher.execute(argumentList);
		}
	}
}
