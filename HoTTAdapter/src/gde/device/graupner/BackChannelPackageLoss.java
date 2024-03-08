/* *************************************************************************************
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

 Copyright (c) 2023 Winfried Bruegmann
 ****************************************************************************************/
package gde.device.graupner;

import java.util.logging.Logger;

import gde.log.Level;

public class BackChannelPackageLoss {
	final static Logger							log	= Logger.getLogger(BackChannelPackageLoss.class.getSimpleName());

	final private PackageLoss				lostPackages;
	final private PackageLossDeque	reverseChannelPackageLossCounter;
	private int											consecutiveLossCounter;

	public BackChannelPackageLoss() {
		lostPackages = new PackageLoss();
		reverseChannelPackageLossCounter = new PackageLossDeque(100);
		consecutiveLossCounter = 0;
	}

	/**
	 * @param isAvailable true if the package is not lost
	 */
	public void trackPackageLoss(boolean isAvailable, int[] points) {
		if (isAvailable) {
			this.reverseChannelPackageLossCounter.add(1);
			points[0] = this.reverseChannelPackageLossCounter.getPercentage() * 1000;
		}
		else {
			this.reverseChannelPackageLossCounter.add(0);
			points[0] = this.reverseChannelPackageLossCounter.getPercentage() * 1000;

			++this.consecutiveLossCounter;
		}
		++this.lostPackages.numberTrackedSamples;
	}

	/**
	 * @return true if the lost packages count is transferred into the loss statistics
	 */
	public boolean updateLossStatistics() {
		if (this.consecutiveLossCounter > 0) {
			this.lostPackages.add(this.consecutiveLossCounter);
			this.consecutiveLossCounter = 0;
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * update packets loss statistics before reading statistics values
	 */
	public void finalUpdateLossStatistics() {
		this.lostPackages.percentage = this.lostPackages.lossTotal * 100. / (this.lostPackages.numberTrackedSamples - this.consecutiveLossCounter);
		log.log(Level.INFO, String.format("lostPackages = (%d) %d of %d percentage = %3.1f", this.lostPackages.lossTotal + this.consecutiveLossCounter, this.lostPackages.lossTotal,
				this.lostPackages.numberTrackedSamples, this.lostPackages.percentage));
	}

	/**
	 * @return the total number of lost packages (is summed up while reading the log)
	 */
	public int getLossTotal() {
		return this.lostPackages.lossTotal + this.consecutiveLossCounter;
	}

	public PackageLoss getLostPackages() {
		return this.lostPackages;
	}

	@Override
	public String toString() {
		return super.toString() + "  [lossTotal=" + this.lostPackages.lossTotal + ", consecutiveLossCounter=" + this.consecutiveLossCounter + "]";
	}

}
