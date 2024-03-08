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

    Copyright (c) 2017,2018,2019 Thomas Eickert
****************************************************************************************/

package gde.histo.utils;

import static gde.histo.utils.ElementaryQuantile.BoxplotItems.LOWER_WHISKER;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.LQT;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE0;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE1;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE2;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE3;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE4;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.UPPER_WHISKER;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.UQT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

import gde.config.Settings;
import gde.histo.datasources.HistoSet;
import gde.log.Logger;

/**
 * Immutable quantile calculation of a probability distribution.
 * Is based on a mergesort and thus avg O(n log n).<br>
 * NB: a 500k records clone + sort takes 45 ms (T is {@code Number} or {@code Spot<Number>}) on ET's machine.
 * @author Thomas Eickert (USER)
 */
public class ElementaryQuantile<T extends Number & Comparable<T>> {
	private static final String	$CLASS_NAME									= ElementaryQuantile.class.getName();
	private static final Logger	log													= Logger.getLogger($CLASS_NAME);

	interface StatsHelper {

		int getOr();

		double getSum();

		double getAvg();

		double getSigma(boolean isSample);

		int getCount();
	}

	/**
	 * This sigma value for the inner 50% of the population.<br>
	 * Interquartile range <em>IQR = 0.25 < p < 0.75</em>
	 */
	public static final double	INTER_QUARTILE_SIGMA_FACTOR	= 0.674489694;

	private final Settings			settings;

	/**
	 * required for probability calculations from the population
	 */
	protected final boolean			isSample;
	/**
	 * remaining population after removing the elimination members
	 */
	protected final List<T>			trunk;

	protected StatsHelper				statsHelper;

	public enum BoxplotItems {
		QUARTILE0, LOWER_WHISKER, QUARTILE1, QUARTILE2, QUARTILE3, UPPER_WHISKER, QUARTILE4, LQT, UQT
	}

	/**
	 * Implements the Gauss error function.<br>
	 * <em>erf(z) = 2 / &radic;&pi; &int;e<sup>-t&sup2;</sup>, t = 0..z</em>
	 * <p>
	 * Examples:<br>
	 * {@code erf(1.0) = 0.8427007877600067} // actual = 0.84270079294971486934 <br>
	 * {@code Phi(1.0) = 0.8413447386043253} // actual = 0.8413447460 <br>
	 * <br>
	 * {@code erf(-1.0) = -0.8427007877600068} <br>
	 * {@code Phi(-1.0) = 0.15865526139567465} <br>
	 * <br>
	 * {@code erf(3.0) = 0.9999779095015785} // actual = 0.99997790950300141456 <br>
	 * {@code Phi(3.0) = 0.9986501019267444} <br>
	 * <br>
	 * {@code erf(30.0) = 1.0} <br>
	 * {@code Phi(30.0) = 1.0} <br>
	 * <br>
	 * {@code erf(-30.0) = -1.0} <br>
	 * {@code Phi(-30.0) = 0.0} <br>
	 * <br>
	 * {@code erf(1.0E-20)  = -3.0000000483809686E-8} // true answer 1.13E-20 <br>
	 * {@code Phi(1.0E-20)  = 0.49999998499999976} <br>
	 *
	 * @see <a href="https://introcs.cs.princeton.edu/java/21function/ErrorFunction.java.html">Error Function</a>
	 * @author Robert Sedgewick
	 * @author Kevin Wayne
	 * @author Thomas Eickert
	 */
	protected static class ErrorFunction {

		/**
		 * Fractional error in math formula less than {@code 1.2 * 10 ^ -7}.
		 * Although subject to catastrophic cancellation when z in very close to 0.
		 * @param z
		 * @return from Chebyshev fitting formula for erf(z) from Numerical Recipes, 6.2
		 */
		public static double erf(double z) {
			double t = 1.0 / (1.0 + 0.5 * Math.abs(z));
			// use Horner's method
			double ans = 1 - t * Math.exp(-z * z - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 + t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 + t * (-0.82215223 + t * (0.17087277))))))))));
			if (z >= 0)
				return ans;
			else
				return -ans;
		}

		/**
		 * fractional error less than <em>x.xx * 10<sup>-4</sup> </em>.
		 * @param z
		 * @return erf(z) acc. to Algorithm 26.2.17 in Abromowitz and Stegun, Handbook of Mathematical
		 */
		public static double erf2(double z) {
			double t = 1.0 / (1.0 + 0.47047 * Math.abs(z));
			double poly = t * (0.3480242 + t * (-0.0958798 + t * (0.7478556)));
			double ans = 1.0 - poly * Math.exp(-z * z);
			if (z >= 0)
				return ans;
			else
				return -ans;
		}

		/**
		 * <em>&Phi;(x) = &frac12; erfc(-x/&radic;2) </em><br>
		 * See Gaussia.java for a better way to compute <em>&Phi;(x)</em>.
		 * @param z
		 * @return the cumulative normal distribution
		 */
		public static double Phi(double z) {
			return 0.5 * (1.0 + erf(z / Math.sqrt(2.0)));
		}

		/**
		 * @param sigmaFactor defines the confidence interval (<em>CI = &plusmn; z * &sigma; with z >= 0</em>)
		 * @return the probability that a normal deviate lies in the confidence interval
		 */
		public static double getProbability(double sigmaFactor) {
			return erf(sigmaFactor / Math.sqrt(2.));
		}

		/**
		 * Test client
		 * @param args
		 */
		public static void simpleTest(String[] args) {
			double x = Double.parseDouble(args[0]);

			System.out.println(String.format("erf(%f)  = %f", x, ErrorFunction.erf(x))); //$NON-NLS-1$
			System.out.println(String.format("erf2(%f)  = %f", x, ErrorFunction.erf2(x))); //$NON-NLS-1$
			System.out.println(String.format("Phi(%f)  = %f", x, ErrorFunction.Phi(x))); //$NON-NLS-1$
			System.out.println();
		}
	}

	/**
	 * Support standard deviation calculation via parallel streams.
	 */
	class StatsDoubleHelper implements DoubleConsumer, StatsHelper {
		private double	avg				= 0;
		private double	varTimesN	= 0;
		private int			count			= 0;

		@Override
		public int getOr() {
			return 0;
		}

		@Override
		public double getSum() {
			return avg * count;
		}

		@Override
		public double getAvg() {
			return count > 0 ? avg : 0;
		}

		@Override
		public double getSigma(@SuppressWarnings("hiding") boolean isSample) {
			if (isSample) {
				return count > 1 ? Math.sqrt(varTimesN / (isSample ? count - 1 : count)) : 0;
			} else {
				return count > 0 ? Math.sqrt(varTimesN / (isSample ? count - 1 : count)) : 0;
			}
		}

		@Override
		public int getCount() {
			return count;
		}

		@Override
		public void accept(double value) {
			varTimesN += (value - avg) * (value - avg) * count / ++count; // pls note the counter increment
			avg += (value - avg) / count;
		}

		/**
		 * @param other
		 * @see <a href="https://stats.stackexchange.com/a/56000">Formulae</a>
		 */
		public void combine(StatsDoubleHelper other) {
			double tmpAvg = this.avg * count / (count + other.count) + other.avg * other.count / (count + other.count);
			// the next line is also valid for samples as we work with varTimesN which is in fact var times (N-1) for the Bessel corrected version
			varTimesN += other.varTimesN + count * (avg - tmpAvg) * (avg - tmpAvg) + other.count * (other.avg - tmpAvg) * (other.avg - tmpAvg);
			avg = tmpAvg;
			count += other.count;
		}
	}

	/**
	 * Support standard deviation calculation via parallel streams.
	 */
	class StatsIntHelper implements IntConsumer, StatsHelper {
		private int			or 				= 0;
		private double	avg				= 0;
		private double	varTimesN	= 0;
		private int			count			= 0;

		@Override
		public int getOr() {
			return or;
		}

		@Override
		public double getSum() {
			return avg * count;
		}

		@Override
		public double getAvg() {
			return count > 0 ? avg : 0;
		}

		@Override
		public double getSigma(@SuppressWarnings("hiding") boolean isSample) {
			if (isSample) {
				return count > 1 ? Math.sqrt(varTimesN / (isSample ? count - 1 : count)) : 0;
			} else {
				return count > 0 ? Math.sqrt(varTimesN / (isSample ? count - 1 : count)) : 0;
			}
		}

		@Override
		public int getCount() {
			return count;
		}

		@Override
		public void accept(int value) {
			or |= value;
			varTimesN += (value - avg) * (value - avg) * count / ++count; // pls note the counter increment
			avg += (value - avg) / count;
		}

		/**
		 * @param other
		 * @see <a href="https://stats.stackexchange.com/a/56000">Formulae</a>
		 */
		public void combine(StatsIntHelper other) {
			or |= other.or;
			double tmpAvg = this.avg * count / (count + other.count) + other.avg * other.count / (count + other.count);
			// the next line is also valid for samples as we work with varTimesN which is in fact var times (N-1) for the Bessel corrected version
			varTimesN += other.varTimesN + count * (avg - tmpAvg) * (avg - tmpAvg) + other.count * (other.avg - tmpAvg) * (other.avg - tmpAvg);
			avg = tmpAvg;
			count += other.count;
		}
	}

	public static ElementaryQuantile<Double> createElementarySpotQuantile(Collection<Spot<Double>> population, Settings settings) {
		List<Double> trunk = new ArrayList<>();
		for (Spot<Double> spot : population) {
			trunk.add(spot.y());
		}
		boolean isSample = true;
		return new ElementaryQuantile<Double>(trunk, isSample, settings);
	}

	/**
	 * @param population
	 * @param isSample true calculates the sample standard deviation
	 */
	public ElementaryQuantile(List<T> population, boolean isSample, Settings settings) {
		this.settings = settings;
		this.trunk = population;
		this.isSample = isSample;

		Collections.sort(this.trunk);
		log.finest(() -> "" + population.size() + Arrays.toString(population.toArray()));
		log.finest(() -> "" + this.trunk.size() + Arrays.toString(this.trunk.toArray()));
	}

	public int getOrFigure() {
		if (statsHelper == null) setStats();
		return statsHelper.getOr();
	}

	public double getSumFigure() {
		if (statsHelper == null) setStats();
		return statsHelper.getSum();
	}

	public double getAvgOBS() {
		double avg = 0;
		for (int i = 0; i < trunk.size(); i++) {
			double value = trunk.get(i).doubleValue();
			avg += (value - avg) / (i + 1);
		}
		return avg;
	}

	public double getAvgFigure() {
		if (statsHelper == null) setStats();
		return statsHelper.getAvg();
	}

	public double getSigmaOBS() {
		double avg = getAvgFigure();
		double varTimesN = 0;
		for (int i = 0; i < trunk.size(); i++) {
			double value = trunk.get(i).doubleValue();
			varTimesN += (value - avg) * (value - avg);
		}
		return trunk.size() > 0 ? Math.sqrt(varTimesN / (isSample ? trunk.size() - 1 : trunk.size())) : 0;
	}

	public double getSigmaRunningOBS() {
		double avg = 0;
		double varTimesN = 0;
		int count = 0;
		for (int i = 0; i < trunk.size(); i++) {
			double value = trunk.get(i).doubleValue();
			varTimesN += (value - avg) * (value - avg) * count / ++count; // pls note the counter increment
			avg += (value - avg) / count;
		}
		return trunk.size() > 0 ? Math.sqrt(varTimesN / (isSample ? trunk.size() - 1 : trunk.size())) : 0;
	}

	public double getSigmaFigure() {
		if (statsHelper == null) setStats();
		return statsHelper.getSigma(isSample);
	}

	protected void setStats() {
		if (!trunk.isEmpty() && (trunk.get(0) instanceof Integer || trunk.get(0) instanceof Short)) {
			statsHelper = trunk.parallelStream().mapToInt(T::intValue).collect(StatsIntHelper::new, StatsIntHelper::accept, StatsIntHelper::combine);
		} else {
			statsHelper = trunk.parallelStream().mapToDouble(T::doubleValue).collect(StatsDoubleHelper::new, StatsDoubleHelper::accept, StatsDoubleHelper::combine);
		}
	}

	/**
	 * The sample quantile is calculated according to R-6, SAS-4, SciPy-(0,0), Maple-5 which is piecewise linear and symmetric.
	 * @param probabilityCutPoint as value between 0 and 1
	 * @return the quantile for the population or for a sample
	 */
	public double getQuantile(double probabilityCutPoint) {
		int pSize = trunk.size();
		if (pSize > 0) {
			if (isSample) {
				if (probabilityCutPoint >= 1. / (pSize + 1) && probabilityCutPoint < (double) pSize / (pSize + 1)) {
					double position = (pSize + 1) * probabilityCutPoint;
					return trunk.get((int) position - 1).doubleValue() + (position - (int) position) * (trunk.get((int) position).doubleValue() - trunk.get((int) position - 1).doubleValue());
				}
				else if (probabilityCutPoint < 1. / (pSize + 1))
					return trunk.get(0).doubleValue();
				else
					return trunk.get(pSize - 1).doubleValue();
			}
			else {
				if (probabilityCutPoint > 0. && probabilityCutPoint < 1.) {
					double position = pSize * probabilityCutPoint;
					if (position % 2 == 0)
						return (trunk.get((int) position).doubleValue() + trunk.get((int) (position + 1)).doubleValue()) / 2.;
					else
						return trunk.get((int) (position)).doubleValue();
				}
				else if (probabilityCutPoint == 0.)
					return trunk.get(0).doubleValue();
				else
					return trunk.get(pSize - 1).doubleValue();
			} 
		}
		return 1.0;
	}

	public double getQuartile0() {
		if (trunk != null && trunk.size() >= 1)
			return trunk.get(0).doubleValue();
		return 0.0;
	}

	public double getQuartile1() {
		return getQuantile(.25);
	}

	public double getQuartile2() {
		return getQuantile(.5);
	}

	public double getQuartile3() {
		return getQuantile(.75);
	}

	public double getQuartile4() {
		if (trunk != null && trunk.size() >= 2)
			return trunk.get(trunk.size() - 1).doubleValue();
		return 0.0;
	}

	public double getInterQuartileRangeOBS() {
		return getQuantile(.75) - getQuantile(.25);
	}

	/**
	 * @return the interquantile ranges or equivalent intervals based on a normal distribution
	 */
	public double[] getQuantileToleranceLowerUpper(double sigmaFactor) {
		final double outlierProbability1 = (1 - ErrorFunction.getProbability(sigmaFactor)) / 2.;
		final double q1 = getQuantile(outlierProbability1);
		final double q2 = getQuartile2();
		final double q3 = getQuantile(1. - outlierProbability1);
		if (settings.isCanonicalQuantiles()) {
			double halfTolerance = (q3 - q1) / 2.;
			return new double[] { halfTolerance, halfTolerance };
		} else if (settings.isSymmetricToleranceInterval()) {
			if (HistoSet.fuzzyEquals(q3 - q1, 0.)
					// next line for keeping all event values
					&& !HistoSet.fuzzyEquals(q3, 0.) && !HistoSet.fuzzyEquals(q1, 0.)) {
				// take the more expensive avg +- sigma solution
				double halfTolerance = getSigmaFigure() * sigmaFactor;
				log.finer(() -> "avg=" + getAvgFigure() + "  sigma=" + getSigmaFigure());
				return new double[] { halfTolerance, halfTolerance };
			} else {
				double halfTolerance = (q3 - q1) / 2.;
				return new double[] { halfTolerance, halfTolerance };
			}
		} else {
			if ((HistoSet.fuzzyEquals(q3 - q1, 0.) || HistoSet.fuzzyEquals(q2 - q1, 0.) || HistoSet.fuzzyEquals(q3 - q2, 0.))
					// next line for keeping all event values
					&& !HistoSet.fuzzyEquals(q3, 0.) && !HistoSet.fuzzyEquals(q1, 0.)) {
				double halfTolerance = getSigmaFigure() * sigmaFactor;
				log.finer(() -> "avg=" + getAvgFigure() + "  sigma=" + getSigmaFigure());
				return new double[] { halfTolerance, halfTolerance };
			} else {
				return new double[] { q2 - q1, q3 - q2 };
			}
		}
	}

	/**
	 * @return the interquartile ranges or equivalent intervals based on a normal distribution
	 */
	public double[] getQuartileToleranceLowerUpper() {
		return getQuantileToleranceLowerUpper(INTER_QUARTILE_SIGMA_FACTOR);
	}

	/**
	 * @param outlierFactor defines the positive or negative range starting at the Tolerance Interval (TI)
	 * @return the outmost range value based on the population reduced by castaways and outliers
	 */
	public double getExtremumFromRange(double sigmaFactor, double outlierFactor) {
		if (outlierFactor == 0.) {
			throw new IllegalArgumentException();
		} else {
			double[] toleranceLowerUpper = getQuantileToleranceLowerUpper(sigmaFactor);
			if (HistoSet.fuzzyEquals(toleranceLowerUpper[0] + toleranceLowerUpper[1], 0.)) {
				return getQuartile2();
			} else if (outlierFactor < 0) {
				double minLimit = getQuartile1() + outlierFactor * 2. * toleranceLowerUpper[0];
				ListIterator<T> iterator = trunk.listIterator();
				while (iterator.hasNext()) {
					double trunkValue = iterator.next().doubleValue();
					if (trunkValue >= minLimit) {
						return trunkValue;
					}
				}
				throw new UnsupportedOperationException("outlier factor < 0");
			} else {
				double maxLimit = getQuartile3() + outlierFactor * 2. * toleranceLowerUpper[1];
				ListIterator<T> iterator = trunk.listIterator(trunk.size());
				while (iterator.hasPrevious()) {
					double trunkValue = iterator.previous().doubleValue();
					if (trunkValue <= maxLimit) {
						return trunkValue;
					}
				}
				throw new UnsupportedOperationException();
			}
		}
	}

	public double getQuantileLowerWhisker() {
		final double probabilityCutPoint = .25;
		final double whiskerStartValue = getQuantile(probabilityCutPoint);
		final double whiskerLimitValue = whiskerStartValue - getQuartileToleranceLowerUpper()[0] * 2. * 1.5;
		double value = whiskerStartValue;
		for (int i = 0; i < trunk.size() * probabilityCutPoint; i++) {
			if (trunk.get(i).doubleValue() >= whiskerLimitValue) {
				// get the corrected value which is crucial for samples
				value = getQuantile((.5 + i) / trunk.size()); // add .5 due to zerobased index and rule 0<p<1 which implies an index average value
				// take the whisker limit value if the interpolation / estimation value is beyond the limit
				value = value < whiskerLimitValue ? whiskerLimitValue : value;
				break;
			}
		}
		return value;
	}

	public double getQuantileUpperWhisker() {
		final double probabilityCutPoint = .75;
		final double whiskerStartValue = getQuantile(probabilityCutPoint);
		final double whiskerLimitValue = whiskerStartValue + getQuartileToleranceLowerUpper()[1] * 2. * 1.5;
		double value = whiskerStartValue;
		for (int i = trunk.size() - 1; i >= trunk.size() * probabilityCutPoint; i--) {
			if (trunk.get(i).doubleValue() <= whiskerLimitValue) {
				// get the corrected value which is crucial for samples
				value = getQuantile((.5 + i) / trunk.size()); // add .5 due to zerobased index and rule 0<p<1 which implies an index average value
				// take the whisker limit value if the interpolation / estimation value is beyond the limit
				value = value > whiskerLimitValue ? whiskerLimitValue : value;
				break;
			}
		}
		return value;
	}

	public double[] getTukeyBoxPlot() {
		double[] values = new double[7];
		values[QUARTILE0.ordinal()] = getQuartile0();
		values[LOWER_WHISKER.ordinal()] = getQuantileLowerWhisker();
		values[QUARTILE1.ordinal()] = getQuartile1();
		values[QUARTILE2.ordinal()] = getQuartile2();
		values[QUARTILE3.ordinal()] = getQuartile3();
		values[UPPER_WHISKER.ordinal()] = getQuantileUpperWhisker();
		values[QUARTILE4.ordinal()] = getQuartile4();
		return values;
	}

	public double[] getTukeyWithQuartileTolerances() {
		double[] values = new double[9];
		values[QUARTILE0.ordinal()] = getQuartile0();
		values[LOWER_WHISKER.ordinal()] = getQuantileLowerWhisker();
		values[QUARTILE1.ordinal()] = getQuartile1();
		values[QUARTILE2.ordinal()] = getQuartile2();
		values[QUARTILE3.ordinal()] = getQuartile3();
		values[UPPER_WHISKER.ordinal()] = getQuantileUpperWhisker();
		values[QUARTILE4.ordinal()] = getQuartile4();
		double[] toleranceInterval = getQuartileToleranceLowerUpper();
		values[LQT.ordinal()] = toleranceInterval[0];
		values[UQT.ordinal()] = toleranceInterval[1];
		return values;
	}

	/**
	 * @return the population size after eliminations and removing outliers
	 */
	public int getSize() {
		return trunk.size();
	}

	@Override
	public String toString() {
		return "isSample=" + isSample + ", size=" + getSize() //
				+ ", sumFigure=" + getSumFigure() + ", avgFigure=" + getAvgFigure() + ", sigmaFigure=" + getSigmaFigure() + "";
	}

}