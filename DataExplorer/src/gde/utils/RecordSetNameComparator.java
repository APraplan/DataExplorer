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

import java.io.Serializable;
import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * To sort by starting number or alphabetically
 * @author Winfried Brügmann
 */
public class RecordSetNameComparator implements Comparator<String>, Serializable {
	static final long serialVersionUID = 26031957; 

	public static void main(String[] args) {
		RecordSetNameComparator rc = new RecordSetNameComparator();
		rc.compare("1) asdfer", "12) 12121"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	// Comparator interface requires defining compare method.
	public int compare(String nameA, String nameB) {
		try {
			Pattern p = Pattern.compile("^[ ]*[0-9]+[^0-9]"); //$NON-NLS-1$
			if (p.matcher(nameA).find() && p.matcher(nameB).find() ) {
				Integer intA = Integer.valueOf(nameA.trim().split("\\D")[0]); //$NON-NLS-1$
				Integer intB = Integer.valueOf(nameB.trim().split("\\D")[0]); //$NON-NLS-1$
				if (intA > intB)
					return 1;
				else if (intA < intB)
					return -1;
				else 
					return 0;
			}

			//... If no number, sort alphabetically.
			return nameA.compareToIgnoreCase(nameB);
		}
		catch (Exception e) {
			// NumberFormatException, sort alphabetically.
			return nameA.compareToIgnoreCase(nameB);
		}
	}
}
