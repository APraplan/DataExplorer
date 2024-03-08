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
    
    Copyright (c) 2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.util.Vector;

/**
 * class to collect and calculate package loss
 * @author brueg
 *
 */
public class PackageLossDeque extends Vector<Integer> {
	private static final long	serialVersionUID	= 1L;
	final int									integrationInterval;
	int												lossCounter;

	public PackageLossDeque(int integrationCount) {
		super(integrationCount);
		this.integrationInterval = integrationCount;
		this.lossCounter = 0;
	}

	@Override
	public synchronized void clear() {
		super.clear();
		this.lossCounter = 0;
	}
	
	@Override
	public synchronized boolean add(Integer value) {
		boolean ret = super.add(value);
		if (value == 0) ++this.lossCounter;
		if (this.size() > this.integrationInterval) {
			if (this.get(0) == 0) --this.lossCounter;
			this.remove(0);
		}
		return ret;
	}

	public int getPercentage() {
		return (int) (this.lossCounter * 100. / this.size());
	}
}
