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

import gde.log.Level;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * @author Winfried Brügmann
 * Class to load a native library external available or extracted from the jar
 * most of the code is copied from SWT Library.java
 */
public class Library {
	private static final Logger	log			= Logger.getLogger(Library.class.getName());
	
	static final String SEPARATOR;

	static {
		SEPARATOR = System.getProperty("file.separator"); //$NON-NLS-1$
	}

	/**
	 * method to load the shared library 
	 * @param name the name of the library to load
	 */
	public static void loadLibrary (String name) {
		loadLibrary (name, true);
	}

	/**
	 * extract the jar contained load libraries to temporary directory
	 * @param fileName
	 * @param mappedName
	 * @return
	 */
	static boolean extract (String fileName, String mappedName) {
		FileOutputStream os = null;
		InputStream is = null;
		File file = new File(fileName);
		log.log(Level.FINE, fileName + " exist " + file.exists()); //$NON-NLS-1$
		try {
			is = Library.class.getResourceAsStream("/" + mappedName); //$NON-NLS-1$
			//log.log(Level.INFO, "jar in = " + is); //$NON-NLS-1$
			if (is != null) {
				int read;
				byte[] buffer = new byte[4096];
				os = new FileOutputStream(fileName);
				while ((read = is.read(buffer)) != -1) {
					os.write(buffer, 0, read);
				}
				os.close();
				is.close();
				if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) { //$NON-NLS-1$ //$NON-NLS-2$
					try {
						log.log(Level.FINE, "chmod 755 " + fileName); //$NON-NLS-1$
						Runtime.getRuntime().exec(new String[] { "chmod", "755", fileName }).waitFor(); //$NON-NLS-1$ //$NON-NLS-2$
					}
					catch (Throwable e) {
						//ignore
					}
				}
				log.log(Level.FINE, fileName + " exist " + new File(fileName).exists()); //$NON-NLS-1$
				if (load(fileName)) return true;
			}
		} catch (Throwable e) {
			log.log(Level.WARNING, e.getMessage(), e);
			try {
				if (os != null) os.close ();
			} catch (IOException e1) {
				//ignore
			}
			try {
				if (is != null) is.close ();
			} catch (IOException e1) {
				//ignore				
			}
		}
		return false;
	}


	/**
	 * loads the library, using method depending of full qualified library path name
	 * @param libName
	 * @return
	 */
	static boolean load (String libName) {
		try {
			if (libName.indexOf (SEPARATOR) != -1) {
				System.load (libName);
			} else {
				System.loadLibrary (libName);
			}		
			return true;
		} catch (UnsatisfiedLinkError e) {
			//ignore
		}
		return false;
	}


	/**
	 * Loads the shared library that matches the version of the
	 * @param name the name of the library to load
	 * @param mapName true if the name should be mapped, false otherwise
	 */
	public static void loadLibrary (String name, boolean mapName) {
		
		/* Compute the library name and mapped name */
		String libName, mappedName;
		if (mapName) {
			libName = name; 
			mappedName = System.mapLibraryName (libName);
		} else {
			libName = mappedName = name;
		}
		log.log(Level.INFO, "...loading library libname = " + libName + " mappedName = " + mappedName); //$NON-NLS-1$ //$NON-NLS-2$
	
		/* Try loading library from java library path */
		if (load (libName)) return;
		if (mapName && load (libName)) return;
		
		/* Try loading library from the user.dir directory which is the directory where application gets started */
		String path = System.getProperty("user.dir"); //$NON-NLS-1$
		path = new File(path).getAbsolutePath();
		//System.out.println("...loading library from java.io.tmpdir : libname = " + path + SEPARATOR + mappedName);
		if (load(path + SEPARATOR + mappedName)) return;
		
		/* Try loading library from the tmp directory */
		path = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
		path = new File(path).getAbsolutePath();
		//System.out.println("...loading library from java.io.tmpdir : libname = " + path + SEPARATOR + mappedName);
		if (load(path + SEPARATOR + mappedName)) return;
			
		/* Try extracting and loading library from jar */
		//log.log(Level.INFO, "before extract " + path + SEPARATOR + mappedName); //$NON-NLS-1$
		if (path != null) {
			if (extract (path + SEPARATOR + mappedName, mappedName)) return;
		}
		
		/* Failed to find the library */
		throw new UnsatisfiedLinkError ("no " + libName + " in java.library.path or the jar file"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
