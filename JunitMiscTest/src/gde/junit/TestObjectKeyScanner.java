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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.junit;

import java.util.logging.Level;
import java.util.logging.Logger;

import gde.utils.ObjectKeyScanner;

public class TestObjectKeyScanner extends TestSuperClass {
	Logger logger = Logger.getLogger("gde.utils.ObjectKeyScanner");

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.logger.setLevel(Level.FINER);
		this.logger.setUseParentHandlers(true);

		this.setDataPath();
		//System.out.println("this.settings.getDataFilePath() = " + this.settings.getDataFilePath());
	}

	public final void testFindAllObjectKeysAndCreateLinks() {
		ObjectKeyScanner objLnkSearch = new ObjectKeyScanner(false);
		//		objLnkSearch.setSearchForKeys();
		objLnkSearch.start();
		while (objLnkSearch.isAlive()) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				// ignore
			}
		}
	}

	public final void testCreateLinksForObjectKey() {
		ObjectKeyScanner objLnkCrt = new ObjectKeyScanner("ASW-27");
		objLnkCrt.start();
		while (objLnkCrt.isAlive()) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				// ignore
			}
		}
	}

	public final void testDeleteLinks() {
		ObjectKeyScanner.cleanFileLinks();
	}

}
