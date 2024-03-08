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
									2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.histo.gpslocations;

import static org.junit.Assert.assertNotEquals;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gde.DataAccess;
import gde.config.Settings;
import gde.histo.base.BasicTestCase;
import gde.histo.utils.GpsCoordinate;

class GpsGeoCodesTest extends BasicTestCase {
	private final static String	$CLASS_NAME	= GpsGeoCodesTest.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	@Test
	void testGetRandomLocation() {
		double locationRadius = Settings.getInstance().getGpsLocationRadius();
		DataAccess dataAccess = DataAccess.getInstance();

		Random random = new Random();
		double lat; 
		double lon; 
		String location;
		do {
			lat = random.nextFloat() * 20. + 40.;	// Napoli to Stockholm
			lon = random.nextFloat() * 20.;				// Greenwich to Krakow
			location = GeoCodes.getOrAcquireLocation(new GpsCoordinate(lat, lon), locationRadius, dataAccess);
		}
		while (location.equals(""));
		System.out.println("lat/lon : " + lat + "/" + lon + "   " + location);
		assertNotEquals("lat/lon : " + lat + "/" + lon + "  " + location, "", location);
	}

	@Test
	void testLocationFiles() {
		double locationRadius = Settings.getInstance().getGpsLocationRadius();
		DataAccess dataAccess = DataAccess.getInstance();

		String location;
		location = GeoCodes.getOrAcquireLocation(new GpsCoordinate(0., 179.), locationRadius, dataAccess); // pure ocean
		System.out.println(location);
		assertEquals("  location", "", location);
		location = GeoCodes.getOrAcquireLocation(new GpsCoordinate(48.8696877, 10.3573054), locationRadius, dataAccess); // 73441 Bopfingen, Germany (via
																																																											// Google)
		System.out.println(location);
		assertTrue("  location", location.contains("Ipf, Keltenweg (Ipf), Oberdorf am Ipf, Bopfingen, Verwaltungsgemeinschaft Bopfingen, Ostalbkreis, Baden-Württemberg, 73441, Deutschland"));
		location = GeoCodes.getOrAcquireLocation(new GpsCoordinate(45.0064253, 14.3965299), locationRadius, dataAccess);
		System.out.println(location);
		assertEquals("  location", "D100, Vodice, Grad Cres, Primorsko-goranska županija, 51557, Hrvatska", location);
	}
}
