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
									2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.histo.utils;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import gde.config.Settings;
import gde.histo.base.BasicTestCase;
import gde.histo.utils.Quantile.Fixings;

class QuantileTest extends BasicTestCase {
	private final static String	$CLASS_NAME									= QuantileTest.class.getName();
	private final static Logger	log													= Logger.getLogger($CLASS_NAME);

	/**
	 * value 100; reduce to 0 to shorten the elapsed Junit test time
	 */
	private final static int		performanceTestLoops				= 100;

	private Quantile						quantile;
	private final Integer[]			recordArray									= { 99999, -99999, null, 0, null, 8, 10, 25, 49, 50, 51, 75, 99, 100, 101, 134, 175, -5 };	// Size
																																																																											// =
																																																																											// 18
	private final double				q0Zeros2Null								= -99999.;																																									// minimum
	private final double				q1Zeros2Null								= 10.;																																											// 25%
	private final double				q2Zeros2Null								= 51.;																																											// median
	private final double				q3Zeros2Null								= 101.;																																											// 75%
	private final double				q4Zeros2Null								= 99999.;																																										// maximum
	private final double				q33PerCentZeros2Null				= 25.;
	private final double				qLowerWhiskerZeros2Null			= -5;
	private final double				qUpperWhiskerZeros2Null			= 175.;
	private final double				q0SampleZeros2Null					= -99999.;																																									// minimum
	private final double				q1SampleZeros2Null					= 10.;																																											// 25%
	private final double				q2SampleZeros2Null					= 51.;																																											// median
	private final double				q3SampleZeros2Null					= 101.;																																											// 75%
	private final double				q4SampleZeros2Null					= 99999.;																																										// maximum
	private final double				q33PerCentSampleZeros2Null	= 25 + .28 * (49 - 25);
	private final Integer[]			size1Array									= { -99999 };
	private final double				qxSize1Array								= -99999.;
	private final Integer[]			size1ArrayNull							= { null };

	private static final double	DELTA												= 1e-9;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	@Test
	void testSigma() {
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
		final double avg = this.quantile.getAvgFigure();
		final double avgSlow = this.quantile.getAvgSlow();
		final double sigmaOBS = this.quantile.getSigmaOBS();
		final double sigmaRunningOBS = this.quantile.getSigmaRunningOBS();
		final double sigma = this.quantile.getSigmaFigure();
		assertEquals("getAvgFigure=" + avg, avg, avgSlow, DELTA);
		assertEquals("getSigmaOBS=" + sigmaOBS, sigmaOBS, sigmaRunningOBS, DELTA);
		assertEquals("getSigmaFigure=" + sigma, sigma, sigmaRunningOBS, DELTA);
	}

	@Test
	void testIqrNull() {
		Settings settings = Settings.getInstance();

		System.out.println(" >>> 1st pass : Prevent outlier by tolerance interval based on SD in case of zero IQR");
		{
			Double[] values = new Double[] { 1., 1., 1., 1., 1., 1., 1., 7. };
			{
				settings.setCanonicalQuantiles(true);
				settings.setSymmetricToleranceInterval(true);
				UniversalQuantile<Double> dQuantile = new UniversalQuantile<>(new Vector<>(Arrays.asList(values)), true,
						ElementaryQuantile.INTER_QUARTILE_SIGMA_FACTOR, 9., 9., new HashSet<>(), settings);
				assertEquals("outlierSize", 0, dQuantile.getOutliers().size());
				double[] toleranceLowerUpper = dQuantile.getQuartileToleranceLowerUpper();
				double iQR = toleranceLowerUpper[0] + toleranceLowerUpper[1];
				assertEquals("Canonical IQR", 0., iQR);
				System.out.println("Canonical Outliers : " + dQuantile.getOutliersCsv());
			}
			{
				settings.setCanonicalQuantiles(false);
				settings.setSymmetricToleranceInterval(true);
				UniversalQuantile<Double> dQuantile = new UniversalQuantile<>(new Vector<>(Arrays.asList(values)), true,
						ElementaryQuantile.INTER_QUARTILE_SIGMA_FACTOR, 9., 9., new HashSet<>(), settings);
				assertEquals("outlierSize", 0, dQuantile.getOutliers().size());
				double[] toleranceLowerUpper = dQuantile.getQuartileToleranceLowerUpper();
				double iQR = toleranceLowerUpper[0] + toleranceLowerUpper[1];
				assertEquals("Zero-optimized IQR", 2.86, iQR, .01);
				System.out.println("Zero-optimized Outliers : " + dQuantile.getOutliersCsv());
			}
			{
				settings.setCanonicalQuantiles(false);
				settings.setSymmetricToleranceInterval(true);
				UniversalQuantile<Double> dQuantile = new UniversalQuantile<>(new Vector<>(Arrays.asList(values)), true,
						ElementaryQuantile.INTER_QUARTILE_SIGMA_FACTOR, 99., 99., new HashSet<>(), settings);
				assertEquals("outlierSize", 0, dQuantile.getOutliers().size());
				double[] toleranceLowerUpper = dQuantile.getQuartileToleranceLowerUpper();
				double iQR = toleranceLowerUpper[0] + toleranceLowerUpper[1];
				assertEquals("Asymmetric tolerance interval" + iQR, 2.86, iQR, .01);
				System.out.println("Asymmetric tolerance interval Outliers : " + dQuantile.getOutliersCsv());
			}
		}
		System.out.println(" >>> 2nd pass : Prevent outlier by asymmetric tolerance interval");
		{
			Double[] values = new Double[] { .9, .9, 1., 1., 1., 1., 2., 15. };
			{
				settings.setCanonicalQuantiles(true);
				settings.setSymmetricToleranceInterval(true);
				UniversalQuantile<Double> dQuantile = new UniversalQuantile<>(new Vector<>(Arrays.asList(values)), true,
						ElementaryQuantile.INTER_QUARTILE_SIGMA_FACTOR, 9., 9., new HashSet<>(), settings);
				assertEquals("outlierSize", 1, dQuantile.getOutliers().size());
				double[] toleranceLowerUpper = dQuantile.getQuartileToleranceLowerUpper();
				double iQR = toleranceLowerUpper[0] + toleranceLowerUpper[1];
				assertEquals("Canonical IQR", 0.1, iQR, .00001);
				System.out.println("Canonical Outliers : " + dQuantile.getOutliersCsv());
			}
			{
				settings.setCanonicalQuantiles(false);
				settings.setSymmetricToleranceInterval(true);
				UniversalQuantile<Double> dQuantile = new UniversalQuantile<>(new Vector<>(Arrays.asList(values)), true,
						ElementaryQuantile.INTER_QUARTILE_SIGMA_FACTOR, 9., 9., new HashSet<>(), settings);
				assertEquals("outlierSize", 1, dQuantile.getOutliers().size());
				double[] toleranceLowerUpper = dQuantile.getQuartileToleranceLowerUpper();
				double iQR = toleranceLowerUpper[0] + toleranceLowerUpper[1];
				assertEquals("Zero-optimized IQR", 0.1, iQR, .00001);
				System.out.println("Zero-optimized Outliers : " + dQuantile.getOutliersCsv());
			}
			{
				settings.setCanonicalQuantiles(false);
				settings.setSymmetricToleranceInterval(true);
				UniversalQuantile<Double> dQuantile = new UniversalQuantile<>(new Vector<>(Arrays.asList(values)), true,
						ElementaryQuantile.INTER_QUARTILE_SIGMA_FACTOR, 99., 99., new HashSet<>(), settings);
				assertEquals("outlierSize", 0, dQuantile.getOutliers().size());
				double[] toleranceLowerUpper = dQuantile.getQuartileToleranceLowerUpper();
				double iQR = toleranceLowerUpper[0] + toleranceLowerUpper[1];
				assertEquals("Asymmetric tolerance interval", .825, iQR, .00001);
				System.out.println("Asymmetric tolerance interval Outliers : " + dQuantile.getOutliersCsv());
			}
		}
	}

	@Tag("performance")
	@Test
	void testSortPerformance() {
		Settings settings = Settings.getInstance();

		// special double example : -zero
		final Double d3 = -0d; // try this code with d3 = 0d; for comparison
		if (d3 < 0d)
			System.out.println("is never printed");
		else
			System.out.println("Double -zero is equal to +zero");
		if (Double.compare(d3, 0d) < 0)
			System.out.println("Double -zero is less compared to +zero");
		else
			System.out.println("is never printed");

		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		for (int i = 0; i < 15; i++) {
			record.addAll(record);
		}
		{
			this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
			ArrayList<Double> arrayList = new ArrayList<Double>();
			for (Integer value : record) {
				arrayList.add(value != null ? value.doubleValue() : null);
			}
			Quantile quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
			log.log(Level.INFO, ">>> Class Quantile <<<");
			log.log(Level.INFO, "Avg   " + this.quantile.getAvgFigure() + " bisher " + this.quantile.getAvgOBS());
			log.log(Level.INFO, "Sigma " + this.quantile.getSigmaFigure() + " bisher " + this.quantile.getSigmaRunningOBS());
			log.log(Level.INFO, "Avg   " + quantileArray.getAvgFigure() + " bisher " + quantileArray.getAvgOBS());
			log.log(Level.INFO, "Sigma " + quantileArray.getSigmaFigure() + " bisher " + quantileArray.getSigmaRunningOBS());

			for (int j = 0; j < 4; j++) {
				long nanoTime = System.nanoTime(), nanoTimeSigmaInt = 0, nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
					quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
					nanoTimeSigmaInt -= System.nanoTime();
					this.quantile.getSigmaRunningOBS();
					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					quantileArray.getSigmaRunningOBS();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms noStream> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
				nanoTime = System.nanoTime();
				nanoTimeSigmaInt = 0;
				nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS));
					quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS));
					nanoTimeSigmaInt -= System.nanoTime();
					this.quantile.getSigmaFigure();
					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					quantileArray.getSigmaFigure();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms Stream -> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
				nanoTime = System.nanoTime();
				nanoTimeSigmaInt = 0;
				nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
					quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
					nanoTimeSigmaInt -= System.nanoTime();
					this.quantile.getSigmaFigure();
					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					quantileArray.getSigmaFigure();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms opt  ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
			}
		}
		{
			ArrayList<Point> recordPoints = new ArrayList<>();
			ArrayList<Point2D.Double> arrayList = new ArrayList<>();
			int counter = 0;
			for (Integer value : record) {
				if (value != null) {
					recordPoints.add(new Point(counter, value));
					arrayList.add(new Point2D.Double(counter, value));
				}
				counter++;
			}
			// this.quantile = new Quantile(recordPoints, 6, 9);
			Quantile quantileArray = new Quantile(arrayList, 6, 9);
			log.log(Level.INFO, ">>> Class Quantile with Point2D <<<");
			// log.log(Level.INFO, "Avg " + this.quantile.getAvgFigure() + " bisher " + this.quantile.getAvgOBS());
			// log.log(Level.INFO, "Sigma " + this.quantile.getSigmaFigure() + " bisher " + this.quantile.getSigmaRunningOBS());
			log.log(Level.INFO, "Avg   " + quantileArray.getAvgFigure() + " bisher " + quantileArray.getAvgOBS());
			log.log(Level.INFO, "Sigma " + quantileArray.getSigmaFigure() + " bisher " + quantileArray.getSigmaRunningOBS());

			for (int j = 0; j < 4; j++) {
				long nanoTime = System.nanoTime(), nanoTimeSigmaInt = 0, nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					// genericQuantile = new GenericQuantile(recordList, false, 6, 9, exclusions);
					quantileArray = new Quantile(arrayList, 6, 9);
					// nanoTimeSigmaInt -= System.nanoTime();
					// genericQuantile.getSigmaRunningOBS();
					// nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					quantileArray.getSigmaRunningOBS();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms noStream> " + TimeUnit.NANOSECONDS.toMillis(2 * (System.nanoTime() - nanoTime)) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
				nanoTime = System.nanoTime();
				nanoTimeSigmaInt = 0;
				nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					// genericQuantile = new GenericQuantile(recordList, false, 6, 9, exclusions);
					quantileArray = new Quantile(arrayList, 6, 9);
					// nanoTimeSigmaInt -= System.nanoTime();
					// genericQuantile.getSigmaFigure();
					// nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					quantileArray.getSigmaFigure();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms opt  ---> " + TimeUnit.NANOSECONDS.toMillis(2 * (System.nanoTime() - nanoTime)) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
			}
		}
		{
			List<Integer> recordList = new ArrayList<>(record);
			ArrayList<Double> arrayList = new ArrayList<>();
			for (Integer value : record) {
				arrayList.add(value != null ? value.doubleValue() : null);
			}
			Set<Integer> iExclusions = new HashSet<>();
			iExclusions.add(null);
			Set<Double> dExclusions = new HashSet<>();
			dExclusions.add(null);
			UniversalQuantile<Integer> genericQuantile = new UniversalQuantile<>(recordList, false, 6., 9., 9., iExclusions, settings);
			UniversalQuantile<Double> genericArray = new UniversalQuantile<>(arrayList, false, 6., 9., 9., dExclusions, settings);
			log.log(Level.INFO, ">>> Class UniversalQuantile with Number <<<");
			log.log(Level.INFO, "Avg   " + genericQuantile.getAvgFigure() + " bisher " + genericQuantile.getAvgOBS());
			log.log(Level.INFO, "Sigma " + genericQuantile.getSigmaFigure() + " bisher " + genericQuantile.getSigmaRunningOBS());
			log.log(Level.INFO, "Avg   " + genericArray.getAvgFigure() + " bisher " + genericArray.getAvgOBS());
			log.log(Level.INFO, "Sigma " + genericArray.getSigmaFigure() + " bisher " + genericArray.getSigmaRunningOBS());

			for (int j = 0; j < 4; j++) {
				long nanoTime = System.nanoTime(), nanoTimeSigmaInt = 0, nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					genericQuantile = new UniversalQuantile<>(recordList, false, 6., 9., 9., iExclusions, settings);
					genericArray = new UniversalQuantile<>(arrayList, false, 6., 9., 9., dExclusions, settings);
					nanoTimeSigmaInt -= System.nanoTime();
					genericQuantile.getSigmaRunningOBS();
					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					genericArray.getSigmaRunningOBS();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms noStream> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
				nanoTime = System.nanoTime();
				nanoTimeSigmaInt = 0;
				nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					genericQuantile = new UniversalQuantile<>(recordList, false, 6., 9., 9., iExclusions, settings);
					genericArray = new UniversalQuantile<>(arrayList, false, 6., 9., 9., dExclusions, settings);
					nanoTimeSigmaInt -= System.nanoTime();
					genericQuantile.getSigmaFigure();
					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					genericArray.getSigmaFigure();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms opt  ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
			}
		}
		{
			List<Spot<Integer>> recordPoints = new ArrayList<>();
			List<Spot<Integer>> iExclusions = new ArrayList<>();
			iExclusions.add(null);
			List<Spot<Double>> arrayPoints = new ArrayList<>();
			List<Spot<Double>> dExclusions = new ArrayList<>();
			dExclusions.add(null);
			int counter = 0;
			for (Integer value : record) {
				if (value != null) {
					recordPoints.add(new Spot<Integer>(counter, value));
					arrayPoints.add(new Spot<Double>((double) counter, (double) value));
				}
				counter++;
			}
			UniversalQuantile<Double> genericArray = UniversalQuantile.createUniversalSpotQuantile(arrayPoints, 6., 9., settings);
			log.log(Level.INFO, ">>> Class UniversalQuantile with Spot<Double> <<<");
			log.log(Level.INFO, "Avg   " + genericArray.getAvgFigure() + " bisher " + genericArray.getAvgOBS());
			log.log(Level.INFO, "Sigma " + genericArray.getSigmaFigure() + " bisher " + genericArray.getSigmaRunningOBS());

			for (int j = 0; j < 4; j++) {
				long nanoTime = System.nanoTime(), nanoTimeSigmaInt = 0, nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					genericArray = UniversalQuantile.createUniversalSpotQuantile(arrayPoints, 6., 9., settings);
					nanoTimeSigmaDouble -= System.nanoTime();
					genericArray.getSigmaRunningOBS();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms noStream> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
				nanoTime = System.nanoTime();
				nanoTimeSigmaInt = 0;
				nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					genericArray = UniversalQuantile.createUniversalSpotQuantile(arrayPoints, 6., 9., settings);
					nanoTimeSigmaDouble -= System.nanoTime();
					genericArray.getSigmaFigure();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms opt  ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
			}
		}
	}

	@Test
	void testSampleZero2Null() {
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS, Fixings.IS_SAMPLE), 6, 9);
		assertEquals("q0SampleZeros2Null=" + q0SampleZeros2Null, q0SampleZeros2Null, this.quantile.getQuartile0(), DELTA);
		assertEquals("q1SampleZeros2Null=" + q1SampleZeros2Null, q1SampleZeros2Null, this.quantile.getQuartile1(), DELTA);
		assertEquals("q2SampleZeros2Null=" + q2SampleZeros2Null, q2SampleZeros2Null, this.quantile.getQuartile2(), DELTA);
		assertEquals("q3SampleZeros2Null=" + q3SampleZeros2Null, q3SampleZeros2Null, this.quantile.getQuartile3(), DELTA);
		assertEquals("q4SampleZeros2Null=" + q4SampleZeros2Null, q4SampleZeros2Null, this.quantile.getQuartile4(), DELTA);
		assertEquals("q33PerCentSampleZeros2Null=" + q33PerCentSampleZeros2Null, q33PerCentSampleZeros2Null, this.quantile.getQuantile(.33), DELTA);
		log.log(Level.INFO, " ---> " + this.quantile.getQuartile3());
	}

	@Test
	void testPopulationWithZerosForbiddenNulls() {
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		try {
			this.quantile = new Quantile(record, EnumSet.noneOf(Fixings.class), 6, 9);
			fail("Should throw an exception");
		} catch (Exception e) {
			//
		}
	}

	@Test
	void testPopulationZero2Null() {
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS), 6, 9);
		assertEquals("q0Zeros2Null=" + q0Zeros2Null, q0Zeros2Null, this.quantile.getQuartile0());
		assertEquals("q1Zeros2Null=" + q1Zeros2Null, q1Zeros2Null, this.quantile.getQuartile1());
		assertEquals("q2Zeros2Null=" + q2Zeros2Null, q2Zeros2Null, this.quantile.getQuartile2());
		assertEquals("q3Zeros2Null=" + q3Zeros2Null, q3Zeros2Null, this.quantile.getQuartile3());
		assertEquals("q4Zeros2Null=" + q4Zeros2Null, q4Zeros2Null, this.quantile.getQuartile4());
		assertEquals("q33PerCentZeros2Null=" + q33PerCentZeros2Null, q33PerCentZeros2Null, this.quantile.getQuantile(.33));
		log.log(Level.INFO, " ---> " + this.quantile.getQuartile3());
	}

	@Test
	void testQuantilesWhiskerZero2Null() {
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS), 6, 9);
		assertEquals("qLowerWhiskerZeros2Null=" + qLowerWhiskerZeros2Null, qLowerWhiskerZeros2Null, this.quantile.getQuantileLowerWhisker(), DELTA);
		assertEquals("qUpperWhiskerZeros2Null=" + qUpperWhiskerZeros2Null, qUpperWhiskerZeros2Null, this.quantile.getQuantileUpperWhisker(), DELTA);
		log.log(Level.INFO, " ---> " + this.quantile.getQuantileLowerWhisker());
		log.log(Level.INFO, " ---> " + this.quantile.getQuantileUpperWhisker());
	}

	@Test
	void testQuantilesAtSize1() {
		Vector<Integer> record = new Vector<>(Arrays.asList(size1Array));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS), 6, 9);
		assertEquals("q0Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile0());
		assertEquals("q1Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile1());
		assertEquals("q2Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile2());
		assertEquals("q3Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile3());
		assertEquals("q4Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile4());
		assertEquals("q33PerCentZeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuantile(.33));
	}

	@Test
	void testQuantilesAtSize1ArrayNull() {
		Vector<Integer> record = new Vector<>(Arrays.asList(size1ArrayNull));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS), 6, 9);
		try {
			this.quantile.getQuartile2();
			fail("Should throw an exception");
		} catch (Exception e) {
			//
		}
	}
}
