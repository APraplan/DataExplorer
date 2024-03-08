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

import gde.data.RecordSet;
import gde.device.MeasurementPropertyTypes;
import gde.ui.DataExplorer;

/**
 * This abstract thread implementation calculates the slop of the height curve using several algorithm
 * @author Winfried Brügmann
 */
public abstract class CalculationThread extends Thread {
	private final static Logger											log					= Logger.getLogger(CalculationThread.class.getName());

	public static final String REGRESSION_TYPE 					= MeasurementPropertyTypes.REGRESSION_TYPE.value();
	public static final String REGRESSION_TYPE_LINEAR 	= MeasurementPropertyTypes.REGRESSION_TYPE_LINEAR.value();
	public static final String REGRESSION_TYPE_CURVE 		= MeasurementPropertyTypes.REGRESSION_TYPE_CURVE.value();
	public static final String REGRESSION_INTERVAL_SEC 	= MeasurementPropertyTypes.REGRESSION_INTERVAL_SEC.value();

	protected RecordSet											recordSet;
	protected String												sourceRecordKey, targetRecordKey;
	protected int														calcInterval_sec = 10;
	protected final DataExplorer	application;
	protected boolean												threadStop	= false;

	/**
	 * constructor where calculation required parameters are given as parameter
	 * @param currentRecordSet
	 * @param inRecordKey
	 * @param outRecordKey
	 * @param calcIntervalSec
	 */
	public CalculationThread(RecordSet currentRecordSet, String inRecordKey, String outRecordKey, int calcIntervalSec) {
		super("calculation");
		this.recordSet = currentRecordSet;
		this.sourceRecordKey = inRecordKey;
		this.targetRecordKey = outRecordKey;
		this.calcInterval_sec = calcIntervalSec;
		this.application = DataExplorer.getInstance();
		log.log(Level.FINER, this.getClass().getSimpleName() + " instanciated");
		this.setPriority(Thread.MAX_PRIORITY);
	}

	/**
	 * method which do the calculation
	 */
	@Override
	public abstract void run();

	/**
	 * @param newRecordSet the recordSet to set
	 */
	public void setRecordSet(RecordSet newRecordSet) {
		this.recordSet = newRecordSet;
	}

	public void setThreadStop(boolean doStop) {
		this.threadStop = doStop;
	}

	/**
	 * @param inRecordKey the sourceRecordKey to set
	 */
	public void setSourceRecordKey(String inRecordKey) {
		this.sourceRecordKey = inRecordKey;
	}

	/**
	 * @param outRecordKey the targetRecordKey to set
	 */
	public void setTargetRecordKey(String outRecordKey) {
		this.targetRecordKey = outRecordKey;
	}

	/**
	 * @return the calcInterval_sec
	 */
	public int getCalcInterval_sec() {
		return this.calcInterval_sec;
	}

	/**
	 * @param calcIntervalSec the calcInterval_sec to set
	 */
	public void setCalcInterval_sec(int calcIntervalSec) {
		this.calcInterval_sec = calcIntervalSec;
	}
}
