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

import gde.log.Level;
import java.util.logging.Logger;

import gde.data.Record;
import gde.data.RecordSet;

/**
 * This thread implementation calculates the slop of the height curve using linear regression
 * a time interval may be modified, intervals < 2 seconds may result in phase shifts near points of slope = 0
 * a point interval may be modified, but point interval must be modulo 2 and < interval
 * good for calculating average slope between selected curve points
 * @author Winfried Brügmann
 */
public class LinearRegression extends CalculationThread {
	private final static Logger	log	= Logger.getLogger(LinearRegression.class.getName());

	/**
	 * constructor where calculation required parameters are given as parameter
	 * @param currentRecordSet
	 * @param inRecordKey
	 * @param outRecordKey
	 * @param calcIntervalSec
	 */
	public LinearRegression(RecordSet currentRecordSet, String inRecordKey, String outRecordKey, int calcIntervalSec) {
		super(currentRecordSet, inRecordKey, outRecordKey, calcIntervalSec);
	}

	/**
	 * method which do the slope calculation on base of linear regression
	 */
	@Override
	public void run() {
		if (this.recordSet == null || this.sourceRecordKey == null || this.targetRecordKey == null) {
			log.log(Level.WARNING, "Slope can not be calculated -> recordSet == null || sourceRecordKey == null || targetRecordKey == null"); //$NON-NLS-1$
			return;
		}
		synchronized (CalculationThread.REGRESSION_INTERVAL_SEC) {
			log.log(Level.FINE, "start data calculation for record = " + this.targetRecordKey); //$NON-NLS-1$

			Record recordHeight = this.recordSet.get(this.sourceRecordKey);
			Record record = this.recordSet.get(this.targetRecordKey);
			if (record != null && !this.threadStop && recordHeight.getMaxTime_ms()*1000 > this.calcInterval_sec) {
				record.clear(); // make sure to clean the target record before calculate new data points
				double timeStep_sec = recordHeight.getAverageTimeStep_ms() / 1000;
				int timeStepsPerInterval = Double.valueOf(this.calcInterval_sec / timeStep_sec).intValue(); // 4000ms/50ms/point -> 80 points per interval
				timeStepsPerInterval = timeStepsPerInterval <= 4 ? 4 : timeStepsPerInterval;
				int pointsPerInterval = timeStepsPerInterval + 1;
				log.log(Level.FINE, "calcInterval_sec = " + this.calcInterval_sec + " pointsPerInterval = " + pointsPerInterval); //$NON-NLS-1$ //$NON-NLS-2$
				int pointInterval = 3; // fix number of points where the calculation will result in slope values, rest is overlap
				int numberDataPoints = recordHeight.realSize();
				int startPosition = 0; // start position for interval calculation
				int frontPadding = (pointsPerInterval - pointInterval) / 5 * 4; // |-----..-----|		
				//int modCounter = ((numberDataPoints - (numberDataPoints % pointsPerInterval)) - (pointsPerInterval - pointInterval)) / pointInterval;
				int modCounter = (numberDataPoints - (pointsPerInterval - pointInterval)) / pointInterval;
				log.log(Level.FINE, "numberDataPoints = " + numberDataPoints + " modCounter = " + modCounter + " frontPadding = " + frontPadding); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				for (int i = 0; i < frontPadding; i++) { // padding data points which does not fit into interval, begin and end
					record.add(0);
				}
				// calculate avg x == time step in msec
				double avgX = timeStep_sec; // timeStepsPerInterval * time_ms / 1000.0 / timeStepsPerInterval; 
				// (xi - avgX)*(xi - avgX)
				double ssXX = 0.0;
				for (int i = 1; i <= timeStepsPerInterval; i++) {
					ssXX = ssXX + (((timeStep_sec * i) - avgX) * ((timeStep_sec * i) - avgX));
				}
				ssXX = ssXX / timeStepsPerInterval;
				log.log(Level.FINE, "avgX = " + avgX + " ssXX = " + ssXX); //$NON-NLS-1$ //$NON-NLS-2$
				--modCounter;
				while (modCounter > 0 && !this.threadStop) {
					// calculate avg y
					double avgY = 0.0;
					for (int i = 1; i <= timeStepsPerInterval; i++) {
						avgY = avgY + (recordHeight.realGet(i + startPosition));
					}
					avgY = avgY / timeStepsPerInterval;

					// (yi - avgY)
					double sumYi_avgY = 0.0;
					for (int i = 1; i <= timeStepsPerInterval; i++) {
						sumYi_avgY = sumYi_avgY + ((recordHeight.realGet(i + startPosition)) - avgY);
					}
					sumYi_avgY = sumYi_avgY / timeStepsPerInterval;

					// (xi - avgX)*(yi - avgY)
					double ssXY = 0.0;
					for (int i = 1; i <= timeStepsPerInterval; i++) {
						ssXY = ssXY + (((timeStep_sec * i) - avgX) * ((recordHeight.realGet(i + startPosition)) - avgY));
					}
					ssXY = ssXY / timeStepsPerInterval;

					int slope = Double.valueOf(ssXY / ssXX * 4).intValue(); // slope = ssXY / ssXX;
					// add point over pointInterval
					for (int i = 0; i < pointInterval; i++) {
						record.add(slope);
					}
					startPosition = startPosition + pointInterval;

					log.log(Level.FINE, "slope = " + slope + " startPosition = " + startPosition + " modCounter = " + modCounter); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					--modCounter;
				}
				// fill the rest of the curve to make equal lenght
				for (int i = record.realSize(); i < numberDataPoints; i++) {
					record.add(0);
				}
				log.log(Level.FINE, "startPosition = " + startPosition + " modCounter = " + modCounter); //$NON-NLS-1$ //$NON-NLS-2$

				if (this.recordSet.get(this.sourceRecordKey) != null && this.recordSet.get(this.sourceRecordKey).isDisplayable()) record.setDisplayable(true); // depending record influence
				if (this.application.getActiveRecordSet() != null && this.recordSet.getName().equals(this.application.getActiveRecordSet().getName()) && record.isVisible()) {
					this.application.updateGraphicsWindow();
				}
			}

			this.application.updateCurveSelectorTable();
			this.application.updateStatisticsData();
			this.application.updateDataTable(this.recordSet.getName(), true);
			log.log(Level.FINE, "finished data calculation for record = " + this.targetRecordKey); //$NON-NLS-1$
		}
	}

}
