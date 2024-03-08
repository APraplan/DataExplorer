/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.junit;

import gde.GDE;
import gde.utils.FileUtils;
import gde.utils.ObjectKeyScanner;

import java.io.File;

import junit.framework.TestCase;

public class CleanupTestTemp extends TestCase {
	
	final String 		tmpDir 			= System.getProperty("java.io.tmpdir").endsWith(GDE.FILE_SEPARATOR) 
		? System.getProperty("java.io.tmpdir") 
				: System.getProperty("java.io.tmpdir") + GDE.FILE_SEPARATOR ;

	/**
	 * cleans up the temporary created files during junit execution
	 * comment out from AllTests to keep this files for further analysis
	 */
	public void testCleanupTempData() {
		File file = new File(tmpDir);
		if (file.exists()) {
			String[] files = file.list();
			for (String fileName : files) {
				File filesAndDirs = new File(tmpDir + fileName);
				if (filesAndDirs.isFile() 
						&& (fileName.endsWith(GDE.FILE_ENDING_DOT_XML) || fileName.endsWith(GDE.FILE_ENDING_DOT_JPG) || fileName.endsWith(GDE.FILE_ENDING_DOT_PNG) )) {
					filesAndDirs.delete();
					System.out.println("delete " + filesAndDirs.getAbsolutePath());
				}
				else if (filesAndDirs.isDirectory() && fileName.startsWith("Write_") ) {
					FileUtils.deleteDirectory(filesAndDirs.getAbsolutePath());
					System.out.println("delete " + filesAndDirs.getAbsolutePath());
				}
			}
		}
	}
	
	public final void testDeleteLinks() {
		ObjectKeyScanner.cleanFileLinks();
	}
}
