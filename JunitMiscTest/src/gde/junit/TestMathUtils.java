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

import java.util.logging.Level;
import java.util.logging.Logger;

import gde.utils.MathUtils;

public class TestMathUtils  extends TestSuperClass {
	static Logger	log = Logger.getLogger(TestMathUtils.class.getName());
	
	double cat[] = {0,0, 1.0, 2.0, 4.0, 5.0, 8.0, 10.0, 25, 49, 50, 51, 75, 99, 100, 101, 134, 175, 300, 500, 740, 1000, 1345, 2000, 2569, 5000} ;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();		
    log.setLevel(Level.INFO);
    log.setUseParentHandlers(true);
	}

	public void testRoundUpAlgorithmWithDeltaval() {

		for (double d : this.cat) {
			checkRoundUp(d,d);
		}
		for (double d : this.cat) {
			checkRoundUp(d * -1,d);
		}
	}

	/**
	 * @param category
	 */
	private void checkRoundUp(double val, double category) {
		double value;
		if (category > 0) {
			log.log(Level.INFO, category + " ---> " + MathUtils.roundUp(category, category));
			value = val + 0.17;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUp(value, category));
			value = val + 0.27;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUp(value, category));
			value = val + 0.42;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUp(value, category));
			value = val + 0.57;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUp(value, category));
			value = val + 0.77;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUp(value, category));
		}
		else {
			log.log(Level.INFO, category + " ---> " + MathUtils.roundUp(category, category));
			value = val - 0.17;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUp(value, category));
			value = val - 0.27;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUp(value, category));
			value = val - 0.42;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUp(value, category));
			value = val - 0.57;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUp(value, category));
			value = val - 0.77;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUp(value, category));
		}
	}

	public void testRoundDownAlgorithmWithDeltaVal() {

		for (double d : this.cat) {
			checkRoundDown(d, d);
		}
		for (double d : this.cat) {
			checkRoundDown(d * -1, d);
		}
	}

	/**
	 * @param category
	 */
	private void checkRoundDown(double val, double category) {
		double value;
		if (category > 0) {
			log.log(Level.INFO, category + " ---> " + MathUtils.roundDown(category, category));
			value = val + 0.17;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDown(value, category));
			value = val + 0.27;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDown(value, category));
			value = val + 0.42;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDown(value, category));
			value = val + 0.57;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDown(value, category));
			value = val + 0.77;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDown(value, category));
		}
		else {
			log.log(Level.INFO, category + " ---> " + MathUtils.roundDown(category, category));
			value = val - 0.17;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDown(value, category));
			value = val - 0.27;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDown(value, category));
			value = val - 0.42;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDown(value, category));
			value = val - 0.57;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDown(value, category));
			value = val - 0.77;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDown(value, category));
		}
	}

	public void testRoundUpAutoAlgorithmWithDeltaval() {

		for (double d : this.cat) {
			checkRoundUpAuto(d,d);
		}
		for (double d : this.cat) {
			checkRoundUpAuto(d * -1,d);
		}
	}

	/**
	 * @param category
	 */
	private void checkRoundUpAuto(double val, double category) {
		double value;
		if (category > 0) {
			log.log(Level.INFO, category + " ---> " + MathUtils.roundUpAuto(category, category));
			value = val + 0.17;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUpAuto(value, category));
			value = val + 0.27;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUpAuto(value, category));
			value = val + 0.42;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUpAuto(value, category));
			value = val + 0.57;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUpAuto(value, category));
			value = val + 0.77;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUpAuto(value, category));
		}
		else {
			log.log(Level.INFO, category + " ---> " + MathUtils.roundUpAuto(category, category));
			value = val - 0.17;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUpAuto(value, category));
			value = val - 0.27;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUpAuto(value, category));
			value = val - 0.42;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUpAuto(value, category));
			value = val - 0.57;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUpAuto(value, category));
			value = val - 0.77;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundUpAuto(value, category));
		}
	}

	public void testRoundDownAutoAlgorithmWithDeltaVal() {

		for (double d : this.cat) {
			checkRoundDownAuto(d, d);
		}
		for (double d : this.cat) {
			checkRoundDownAuto(d * -1, d);
		}
	}

	/**
	 * @param category
	 */
	private void checkRoundDownAuto(double val, double category) {
		double value;
		if (category > 0) {
			log.log(Level.INFO, category + " ---> " + MathUtils.roundDownAuto(category, category));
			value = val + 0.17;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDownAuto(value, category));
			value = val + 0.27;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDownAuto(value, category));
			value = val + 0.42;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDownAuto(value, category));
			value = val + 0.57;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDownAuto(value, category));
			value = val + 0.77;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDownAuto(value, category));
		}
		else {
			log.log(Level.INFO, category + " ---> " + MathUtils.roundDown(category, category));
			value = val - 0.17;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDownAuto(value, category));
			value = val - 0.27;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDownAuto(value, category));
			value = val - 0.42;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDownAuto(value, category));
			value = val - 0.57;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDownAuto(value, category));
			value = val - 0.77;
			log.log(Level.INFO, value + " ---> " + MathUtils.roundDownAuto(value, category));
		}
	}
	
}
