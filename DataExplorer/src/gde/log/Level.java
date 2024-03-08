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
    
    Copyright (c) 2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.log;

/**
 * class shadows the simple name of the superclass java.util.logging.Level and add TIME level
 */
public class Level extends java.util.logging.Level {

  public static final java.util.logging.Level TIME = new Level("TIME", 850, "sun.util.logging.resources.logging");
  private static final long serialVersionUID = -8176160795706313070L;

	/**
	 * @param s
	 * @param i
	 */
	public Level(String s, int i) {
		super(s, i);
	}

	/**
	 * @param s
	 * @param i
	 * @param s1
	 */
	public Level(String s, int i, String s1) {
		super(s, i, s1);
	}

	
}
