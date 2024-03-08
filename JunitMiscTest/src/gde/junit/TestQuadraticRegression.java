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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gde.histo.utils.SingleResponseRegression;
import gde.histo.utils.Spot;
import gde.histo.utils.SingleResponseRegression.RegressionType;

public class TestQuadraticRegression extends TestSuperClass {
	static Logger								log					= Logger.getLogger(TestQuadraticRegression.class.getName());

	private static final double	DELTA				= 1e-8;

	private final int						xPoints[]		= { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
	private final int						yPoints[]		= { 11757, 11757, 11539, 11529, 11521, 11515,								//
			11609, 11715, 11724, 11733, 11744, 11755, 11757 };

	private final double				x1Points[]	= { 1, 2, 3, 4, 5, 6, 7 };
	private final double				y1Points[]	= { 0.38, 1.15, 2.71, 3.92, 5.93, 8.56, 11.24 };

	private final double				gamma1			= 0.19642857;
	private final double				beta1				= 0.23642857;
	private final double				alpha1			= -0.03285714;
	private final double				r2Coeff1		= 0.99870690;

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
		{
			List<Spot<Integer>> points = new ArrayList<>();
			for (int i = 0; i < this.yPoints.length; i++) {
				points.add(new Spot<>(this.xPoints[i], this.yPoints[i]));
			}
			SingleResponseRegression<Integer> singlePredictorRegression = new SingleResponseRegression<>(points, RegressionType.QUADRATIC);
			log.log(Level.OFF, "result for concave : ", singlePredictorRegression);
			log.log(Level.OFF, "determination : ", singlePredictorRegression.getR2());
		}
		{
			List<Spot<Integer>> points = new ArrayList<>();
			for (int i = 0; i < this.yPoints.length; i++) {
				points.add(new Spot<>(this.xPoints[i], -this.yPoints[i]));
			}
			SingleResponseRegression<Integer> singlePredictorRegression  = new SingleResponseRegression<>(points, RegressionType.QUADRATIC);
			log.log(Level.OFF, "result for convex : ", singlePredictorRegression);
			log.log(Level.OFF, "determination : ", singlePredictorRegression.getR2());
		}
		{
			List<Spot<Integer>> points = new ArrayList<>();
			for (int i = 0; i < this.yPoints.length; i++) {
				points.add(new Spot<>(this.xPoints[i], this.xPoints[i]));
			}
			SingleResponseRegression<Integer> singlePredictorRegression  = new SingleResponseRegression<>(points, RegressionType.QUADRATIC);
			log.log(Level.OFF, "result for linear : ", singlePredictorRegression);
			log.log(Level.OFF, "determination : ", singlePredictorRegression.getR2());
		}
	}

	public void testRoundUpAutoAlgorithmWithDeltaval() {
		List<Spot<Double>> points = new ArrayList<>();
		for (int i = 0; i < this.y1Points.length; i++) {
			points.add(new Spot<>(this.x1Points[i], this.y1Points[i]));
		}

		SingleResponseRegression<Double> singlePredictorRegression = new SingleResponseRegression<>(points, RegressionType.QUADRATIC);
		log.log(Level.OFF, "result : ", singlePredictorRegression);
		assertEquals("alpha ", singlePredictorRegression.getAlpha(), this.alpha1, DELTA);
		assertEquals("beta ", singlePredictorRegression.getBeta(), this.beta1, DELTA);
		assertEquals("gamma ", singlePredictorRegression.getGamma(), this.gamma1, DELTA);
		assertEquals("R2 ", singlePredictorRegression.getR2(), this.r2Coeff1, DELTA);
	}

}
