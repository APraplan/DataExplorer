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

/**
 * simple thread implementation to wait during a main thread without using existing locks
 * use -> WaitTimer.delay(500); 
 */
public class WaitTimer extends Thread {

	/**
	 * synchronous wait for the given time
	 * @param milliSeconds
	 */
	public static void delay(long milliSeconds) {
		try {
			if (milliSeconds > 0)
				Thread.sleep(milliSeconds);
		}
		catch (InterruptedException e) {
			//ignore
		}
	}
}
