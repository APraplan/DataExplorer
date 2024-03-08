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

import gde.GDE;
import gde.io.LogViewReader;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogViewReaderTester extends TestSuperClass {
	Logger	logger = Logger.getLogger(LogViewReaderTester.class.getSimpleName());
	
	String				fileRootDir	= null;

	String[] files_1_13 = {
	// 1.13
	"LogView/Picolario/2006_05_12.lov",
	"LogView/Picolario/2006_06_07.lov",
	"LogView/Htronic Akkumaster C4/2007-06-02SenderSubC7-K4.lov",
	};
	
	String[] files_1_15 = {
	// 1.15
	"LogView/UniLog/Starling electro 20070604.lov",
	"LogView/UniLog/Starling electro 20071010 UniLog.lov",
	"LogView/Picolario/2007_07_20.lov"
	};
	
	String[] files_1_50_ALPHA = {
			"LogView/Htronic Akkumaster C4/2007-05-23-FlugakkuA(1.5 ALPHA).lov",
			"LogView/Htronic Akkumaster C4/2007-05-23-FlugakkuB(1.5 ALPHA).lov",
			"LogView/Htronic Akkumaster C4/2007-05-23-FlugakkuB_1(1.5 ALPHA).lov",
			"LogView/Htronic Akkumaster C4/2007-07-21-FlugakkuA(1.5 ALPHA).lov",
			"LogView/Htronic Akkumaster C4/2007-07-21-FlugakkuB(1.5 ALPHA).lov" 
	};
	
	String[] files_1_50_PreBETA = {
			"LogView/Htronic Akkumaster C4/2007-09-06-Empfänger1300-K1(1.5 PreBeta).lov",
			"LogView/Htronic Akkumaster C4/akkumaster(1.5 PreBeta).lov", 
	};

	
	String[] files_2_0_BETA = {
			"LogView/Htronic Akkumaster C4/2007-06-01-EmpfängerAA4-K2(2 BETA).lov",
			"LogView/Htronic Akkumaster C4/Beta2.0_9VoltBlockTest(2 BETA).lov"
	};

	String[] files_2_0_BETA2 = {
			"LogView/UniLog/1_Empfaenger(2 BETA2).lov",
			"LogView/Picolario/Gunnar_pico_test(2 BETA2).lov"
	};

	String[] files_2_0 = {
	// 2.0
	"LogView/UniLog/Auto-Geschwindigkeit.lov",
	"LogView/Picolario/2006_05_12(2).lov",
	"LogView/Picolario/2006_06_07(2).lov",
	"LogView/UniLog/2008_13_04_ASW27_LiPo_Westhang.lov",
	"LogViewOBJ/ASW-27/ASW-27 23.5.2008 19.32.lov",
	"LogViewOBJ/ASW-27/ASW-27 24.5.2008 15.44.lov",
	};

	String[] files_mixed = {
			"LogView/Htronic Akkumaster C4/2007-05-23-FlugakkuA.lov", 
			"LogView/Htronic Akkumaster C4/2007-05-23-FlugakkuB.lov",
			"LogView/Htronic Akkumaster C4/2007-05-24-EmpfängerAA5.lov", 
			"LogView/Htronic Akkumaster C4/2007-05-24-FlugakkuA.lov",
			"LogView/Htronic Akkumaster C4/2007-05-24-FlugakkuB.lov", 
			"LogView/Htronic Akkumaster C4/2007-06-01-EmpfängerAA4-K2.lov",
			"LogView/Htronic Akkumaster C4/2007-06-01-EmpfängerSubCMPX-K1.lov", 
			"LogView/Htronic Akkumaster C4/2007-06-01-EmpfängerSubCPan-K3.lov",
			"LogView/Htronic Akkumaster C4/2007-06-02-AntriebAA8-K2.lov", 
			"LogView/Htronic Akkumaster C4/2007-06-02-AntriebTPLiPo2s2p-K2.lov",
			"LogView/Htronic Akkumaster C4/2007-06-02SenderSubC7-K4.lov", 
			"LogView/Htronic Akkumaster C4/2007-09-06-Empfänger1300-K1.lov",
			"LogView/Htronic Akkumaster C4/akkumaster.lov", 
			"LogView/Htronic Akkumaster C4/Beta2.0_9VoltBlockTest.lov",
			
			"LogView/Picolario/2006_05_12(2).lov",
			"LogView/Picolario/2006_05_12.lov",
			"LogView/Picolario/2006_06_07(2).lov",
			"LogView/Picolario/2006_06_07.lov",	
			"LogView/Picolario/2006_06_10.lov", 
			"LogView/Picolario/2006_06_30.lov",
			"LogView/Picolario/2006_07_15.lov", 
			"LogView/Picolario/2006_07_29.lov",
			"LogView/Picolario/2007_04_04.lov",
			"LogView/Picolario/2007_04_15.lov",
			"LogView/Picolario/2007_04_28.lov", 
			"LogView/Picolario/2007_06_30.lov",
			"LogView/Picolario/2007_07_20.lov",
			"LogView/Picolario/Gunnar_pico_test.lov",
			"LogView/Picolario/Starling e pico 20080426.lov", 
			"LogView/Picolario/test_8_4.lov",
			
			"LogView/UniLog/1_Empfaenger.lov",
			"LogView/UniLog/2008_13_04_ASW27_LiPo_Westhang.lov",
			"LogView/UniLog/2008_13_04_ASW27_LiPo_Westhang_.lov",
			"LogView/UniLog/2008_24_02_ASW27_Westang.lov", 
			"LogView/UniLog/2008_24_02_ASW27_Westang_.lov",
			"LogView/UniLog/Auto-Geschwindigkeit.lov",
			"LogView/UniLog/Auto-Geschwindigkeit_1.lov",
			"LogView/UniLog/BigExcel_20071014.lov",
			"LogView/UniLog/Capu 20080427.lov",
			"LogView/UniLog/Starling electro 20070604.lov",
			"LogView/UniLog/Starling electro 20071010 UniLog.lov",
			"LogView/UniLog/Starling electro 20071014 UniLog.lov",
			
			"LogViewOBJ/ASW-27/ASW-27 23.5.2008 19.32.lov",
			"LogViewOBJ/ASW-27/ASW-27 24.5.2008 15.44.lov",
			
			"LogView/Htronic Akkumaster C4/2007-05-23-FlugakkuA(1.5 ALPHA).lov",
			"LogView/Htronic Akkumaster C4/2007-05-23-FlugakkuB(1.5 ALPHA).lov",
			"LogView/Htronic Akkumaster C4/2007-07-21-FlugakkuA(1.5 ALPHA).lov",
			"LogView/Htronic Akkumaster C4/2007-07-21-FlugakkuB(1.5 ALPHA).lov",

			"LogView/Htronic Akkumaster C4/2007-09-06-Empfänger1300-K1(1.5 PreBeta).lov",
			"LogView/Htronic Akkumaster C4/akkumaster(1.5 PreBeta).lov", 

			"LogView/Htronic Akkumaster C4/2007-06-01-EmpfängerAA4-K2(2 BETA).lov",
			"LogView/Htronic Akkumaster C4/Beta2.0_9VoltBlockTest(2 BETA).lov",

			"LogView/UniLog/1_Empfaenger(2 BETA2).lov",
			"LogView/Picolario/Gunnar_pico_test(2 BETA2).lov"
	};
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.rootLogger = Logger.getLogger("");
		
		// clean up all handlers from outside
		Handler[] handlers = this.rootLogger.getHandlers();
	  for ( int index = 0; index < handlers.length; index++ ) {
	  	this.rootLogger.removeHandler(handlers[index]);
	  }
		this.rootLogger.setLevel(Level.ALL);
		this.rootLogger.addHandler(ch);
		ch.setFormatter(lf);
		ch.setLevel(Level.INFO);
		
		this.setDataPath();
		this.fileRootDir = this.dataPath.getAbsolutePath();
		this.fileRootDir = this.fileRootDir.substring(0, this.fileRootDir.lastIndexOf(GDE.FILE_SEPARATOR) + 1);
		System.out.println("this.fileRootDir = " + this.fileRootDir);
	}
	
	/**
	 * Test method for {@link gde.io.LogViewReader#getHeader(de.device.IDevice, java.lang.String)}.
	 */
	public final void testGetHeader_1_13() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();
		for (String filePath : this.files_1_13) {
			try {
				if (new File(this.fileRootDir + filePath).exists()) {
					logger.log(Level.INFO, "working with = " + this.fileRootDir + filePath);
					LogViewReader.getHeader(this.fileRootDir + filePath);
				}
			}
			catch (Exception e) {
				failures.put(this.fileRootDir + filePath, e);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}
	
	/**
	 * Test method for {@link gde.io.LogViewReader#getHeader(de.device.IDevice, java.lang.String)}.
	 */
	public final void testGetHeader_1_15() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();
		for (String filePath : this.files_1_15) {
			try {
				if (new File(this.fileRootDir + filePath).exists()) {
					logger.log(Level.INFO, "working with = " + this.fileRootDir + filePath);
					LogViewReader.getHeader(this.fileRootDir + filePath);
				}
			}
			catch (Exception e) {
				failures.put(this.fileRootDir + filePath, e);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}
	
	/**
	 * Test method for {@link gde.io.LogViewReader#getHeader(de.device.IDevice, java.lang.String)}.
	 */
	public final void testGetHeader_1_50_ALPHA() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();
		for (String filePath : this.files_1_50_ALPHA) {
			try {
				if (new File(this.fileRootDir + filePath).exists()) {
					logger.log(Level.INFO, "working with = " + this.fileRootDir + filePath);
					LogViewReader.getHeader(this.fileRootDir + filePath);
				}
			}
			catch (Exception e) {
				failures.put(this.fileRootDir + filePath, e);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}
	
	/**
	 * Test method for {@link gde.io.LogViewReader#getHeader(de.device.IDevice, java.lang.String)}.
	 */
	public final void testGetHeader_1_50_PreBETA() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();
		for (String filePath : this.files_1_50_PreBETA) {
			try {
				if (new File(this.fileRootDir + filePath).exists()) {
					logger.log(Level.INFO, "working with = " + this.fileRootDir + filePath);
					LogViewReader.getHeader(this.fileRootDir + filePath);
				}
			}
			catch (Exception e) {
				failures.put(this.fileRootDir + filePath, e);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}
	
	/**
	 * Test method for {@link gde.io.LogViewReader#getHeader(de.device.IDevice, java.lang.String)}.
	 */
	public final void testGetHeader_2_0_BETA() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();
		for (String filePath : this.files_2_0_BETA) {
			try {
				if (new File(this.fileRootDir + filePath).exists()) {
					logger.log(Level.INFO, "working with = " + this.fileRootDir + filePath);
					LogViewReader.getHeader(this.fileRootDir + filePath);
				}
			}
			catch (Exception e) {
				failures.put(this.fileRootDir + filePath, e);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}
	
	/**
	 * Test method for {@link gde.io.LogViewReader#getHeader(de.device.IDevice, java.lang.String)}.
	 */
	public final void testGetHeader_2_0_BETA2() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();
		for (String filePath : this.files_2_0_BETA2) {
			try {
				if (new File(this.fileRootDir + filePath).exists()) {
					logger.log(Level.INFO, "working with = " + this.fileRootDir + filePath);
					LogViewReader.getHeader(this.fileRootDir + filePath);
				}
			}
			catch (Exception e) {
				failures.put(this.fileRootDir + filePath, e);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}
	
	/**
	 * Test method for {@link gde.io.LogViewReader#getHeader(de.device.IDevice, java.lang.String)}.
	 */
	public final void testGetHeader_2_0() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();
		for (String filePath : this.files_2_0) {
			try {
				if (new File(this.fileRootDir + filePath).exists()) {
					logger.log(Level.INFO, "working with = " + this.fileRootDir + filePath);
					LogViewReader.getHeader(this.fileRootDir + filePath);
				}
			}
			catch (Exception e) {
				failures.put(this.fileRootDir + filePath, e);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}
	
	/**
	 * Test method for {@link gde.io.LogViewReader#getHeader(de.device.IDevice, java.lang.String)}.
	 */
	public final void testGetHeader_mixed() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();
		for (String filePath : this.files_mixed) {
			try {
				if (new File(this.fileRootDir + filePath).exists()) {
					logger.log(Level.INFO, "working with = " + this.fileRootDir + filePath);
					LogViewReader.getHeader(this.fileRootDir + filePath);
				}
			}
			catch (Exception e) {
				failures.put(this.fileRootDir + filePath, e);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

}
