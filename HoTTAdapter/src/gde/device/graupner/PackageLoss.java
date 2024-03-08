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

import java.util.Locale;
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;

/**
 * Running statistics about the reverse channel lost packages.
 * @author brueg
 */
public class PackageLoss extends Vector<Integer> {
	private static final long	serialVersionUID			= -1434896150654661385L;
	final static Logger														log													= Logger.getLogger(PackageLoss.class.getSimpleName());

	int												lossTotal							= 0;	//total number of lost packages (is summed up while reading the log
	int												minValue							= 0;
	int												maxValue							= 0;
	double										avgValue							= 0;
	double										sigmaValue						= 0;

	int												numberTrackedSamples	= 0;	//number of total packages used to track package loss
	double										percentage						= 100.;

	/**
	 * add value and detect min/max value
	 */
	@Override
	public synchronized boolean add(Integer value) {

		if (super.size() == 0) {
			this.minValue = this.maxValue = value;
		}
		else {
			if (value > this.maxValue) {
				this.maxValue = value;
				//log.log(Level.OFF, "this.maxValue = " + value);
			}
			else if (value < this.minValue) this.minValue = value;
		}
		
		this.lossTotal += value;
		return super.add(value);
	}

	/**
	 * @return the lossTotal
	 */
	public int getLossTotal() {
		return lossTotal;
	}

	/**
	 * calculates the avgValue
	 */
	public synchronized double getAvgValue() {
		synchronized (this) {
			if (this.size() >= 2) {
				long sum = 0;
				int zeroCount = 0;
				for (Integer xi : this) {
					if (xi != 0) {
						sum += xi;
					}
					else {
						zeroCount++;
					}
				}
				this.avgValue = (this.size() - zeroCount) > 0 ? Long.valueOf(sum / (this.size() - zeroCount)).intValue() : 0;
			}
		}
		return this.avgValue;
	}

	/**
	 * calculates the sigmaValue
	 */
	public synchronized double getSigmaValue() {
		synchronized (this) {
			if (super.size() >= 2) {
				double average = this.getAvgValue() / 1000.0;
				double sumPoweredValues = 0;
				for (Integer xi : this) {
					sumPoweredValues += Math.pow(xi / 1000.0 - average, 2);
				}
				this.sigmaValue = Math.sqrt(sumPoweredValues / (this.size() - 1)) * 1000;
			}
		}
		return this.sigmaValue;
	}

	/**
	 * @return the minValue
	 */
	public synchronized int getMinValue() {
		return minValue;
	}

	/**
	 * @return the maxValue
	 */
	public synchronized int getMaxValue() {
		return maxValue;
	}

	/**
	 * @return the statistics as formated string
	 */
	public String getStatistics() {
		if (this.getMinValue() == this.getMaxValue())
			return GDE.STRING_MESSAGE_CONCAT;
		return String.format(Locale.getDefault(), "min=%.2f sec; max=%.2f sec; avg=%.2f sec; sigma=%.2f sec", this.getMinValue()/100.0, this.getMaxValue()/100.0, this.getAvgValue()/100.0, this.getSigmaValue()/100.0);
	}
}
