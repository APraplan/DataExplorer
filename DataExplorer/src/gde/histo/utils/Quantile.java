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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.function.DoubleConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gde.log.Logger;

/**
 * calculates quantiles of a probability distribution after removing outliers.
 * is based on a mergesort and thus avg O(n log n).
 * NB: a 500k records clone + sort takes 55 ms (in the arrayList version / on ET's machine).
 * quickselect should reduce this to 10 ms.
 * this in turn is compensated by the fact that we mostly need 3 quantiles from the same population.
 * @author Thomas Eickert
 */
// replaced by UniversalQuantile --- reason is spaghetti-like code due to integer, double and 2D-points support --- was not deleted for
// JUnit performance comparisons
public class Quantile {
	private final static String					$CLASS_NAME						= Quantile.class.getName();
	private final static Logger					log										= Logger.getLogger($CLASS_NAME);

	/**
	 * Corresponds to the interquartile range (<em>0.25 < p < 0.75</em>)
	 */
	public final static double					boxplotSigmaFactor		= 0.674489694;
	/**
	 * Specifies the outlier distance limit ODL from the interquartile range (<em>ODL = &rho; * TI with &rho; > 0</em>).<br>
	 * Outliers are identified only if they lie beyond this limit.
	 * @see <a href="https://www.google.de/search?q=Tukey+boxplot">Tukey Boxplot</a>
	 */
	public final static double					boxplotOutlierFactor	= 1.5;

	private final List<Integer>					iPopulation;
	private final List<Double>					dPopulation;
	private final EnumSet<Fixings>			fixings;
	private final List<Integer>					iOutliers;
	private final List<Double>					dOutliers;
	private final List<Point2D.Double>	d2Outliers;

	private double											firstFigure;
	private double											lastFigure;
	private Double											maxFigure;
	private Double											minFigure;
	private Double											sumFigure;
	private Double											avgFigure;
	private Double											sigmaFigure;

	public enum Fixings {
		REMOVE_NULLS, REMOVE_ZEROS, REMOVE_MAXMIN, IS_SAMPLE
	};

	/**
	 * Compares two points by y-coordinate.
	 */
	public static final Comparator<Point2D.Double> Y_ORDER = new YOrder();

	// compare points according to their y-coordinate
	private static class YOrder implements Comparator<Point2D.Double> {
		@Override
		public int compare(Point2D.Double p, Point2D.Double q) {
			if (p.y < q.y) return -1;
			if (p.y > q.y) return +1;
			// take the x value for comparison
			if (p.x < q.x) return -1;
			if (p.x > q.x) return +1;
			return 0;
		}
	}

	/**
	 * Implements the Gauss error function.
	 * erf(z) = 2 / sqrt(pi) * integral(exp(-t*t), t = 0..z)
	 * % java ErrorFunction 1.0
	 * erf(1.0) = 0.8427007877600067 // actual = 0.84270079294971486934
	 * Phi(1.0) = 0.8413447386043253 // actual = 0.8413447460
	 * % java ErrorFunction -1.0
	 * erf(-1.0) = -0.8427007877600068
	 * Phi(-1.0) = 0.15865526139567465
	 * % java ErrorFunction 3.0
	 * erf(3.0) = 0.9999779095015785 // actual = 0.99997790950300141456
	 * Phi(3.0) = 0.9986501019267444
	 * % java ErrorFunction 30
	 * erf(30.0) = 1.0
	 * Phi(30.0) = 1.0
	 * % java ErrorFunction -30
	 * erf(-30.0) = -1.0
	 * Phi(-30.0) = 0.0
	 * % java ErrorFunction 1E-20
	 * erf(1.0E-20) = -3.0000000483809686E-8 // true anser 1.13E-20
	 * Phi(1.0E-20) = 0.49999998499999976
	 *
	 * @see <a href="https://introcs.cs.princeton.edu/java/21function/ErrorFunction.java.html">Error Function</a>
	 * @author Thomas Eickert
	 */
	private static class ErrorFunction {

		/**
		 * fractional error in math formula less than 1.2 * 10 ^ -7.
		 * although subject to catastrophic cancellation when z in very close to 0.
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
		 * @param sigmaFactor defines the tolerance interval (<em>TI = &plusmn; z * &sigma; with z >= 0</em>)
		 * @return the probability that a normal deviate lies in the tolerance interval
		 */
		public static double getProbability(double sigmaFactor) {
			return erf(sigmaFactor / Math.sqrt(2.));
		}

	}

	/**
	 * supports standard deviation calculation via parallel streams.
	 */
	class StatsHelper implements DoubleConsumer {
		private double	avg				= 0;
		private double	varTimesN	= 0;
		private int			count			= 0;

		public double getAvg() {
			return count > 0 ? avg : 0;
		}

		public double getSigma(boolean isSample) {
			return count > 0 ? Math.sqrt(varTimesN / (isSample ? count - 1 : count)) : 0;
		}

		public double getCount() {
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
		public void combine(StatsHelper other) {
			double tmpAvg = this.avg * count / (count + other.count) + other.avg * other.count / (count + other.count);
			// the next line is also valid for samples as we work with varTimesN which is in fact var times (N-1) for the Bessel corrected version
			varTimesN += other.varTimesN + count * (avg - tmpAvg) * (avg - tmpAvg) + other.count * (other.avg - tmpAvg) * (other.avg - tmpAvg);
			avg = tmpAvg;
			count += other.count;
		}
	}

	/**
	 * @param population holds the unordered points (<em>n > 0</em>)
	 * @param fixings defines how to proceed with the data
	 * @param sigmaFactor defines the tolerance interval (<em>TI = &plusmn; z * &sigma; with z >= 0</em>)
	 * @param outlierFactor specifies the outlier distance limit ODL from the tolerance interval (<em>ODL = &rho; * TI with &rho; > 0</em>)
	 */
	public Quantile(Vector<Integer> population, EnumSet<Fixings> fixings, double sigmaFactor, double outlierFactor) {
		if (population.isEmpty()) {
			throw new IllegalArgumentException();
		}
		this.dPopulation = null;
		this.fixings = fixings;
		this.iOutliers = new ArrayList<Integer>();
		this.dOutliers = null;
		this.d2Outliers = null;

		List<Integer> excludes = new ArrayList<Integer>();
		if (fixings.contains(Fixings.REMOVE_NULLS)) excludes.add(null);
		if (fixings.contains(Fixings.REMOVE_ZEROS)) excludes.add(0);
		if (fixings.contains(Fixings.REMOVE_MAXMIN)) {
			excludes.add(Integer.MIN_VALUE);
			excludes.add(Integer.MAX_VALUE);
		}
		if (excludes.isEmpty()) {
			this.iPopulation = new ArrayList<>(population);
		} else {
			this.iPopulation = new ArrayList<>();
			for (Integer value : population) {
				if (!excludes.contains(value)) this.iPopulation.add(value);
			}
		}
		this.firstFigure = this.iPopulation.size() > 0 && this.iPopulation.get(0) != null ? this.iPopulation.get(0) : -Double.MAX_VALUE;
		this.lastFigure = this.iPopulation.size() > 0 && this.iPopulation.get(this.iPopulation.size() - 1) != null
				? this.iPopulation.get(this.iPopulation.size() - 1) : -Double.MAX_VALUE;

		Collections.sort(this.iPopulation);

		if (this.iPopulation.size() > 0) {
			// remove outliers except: if all outliers have the same value we expect them to carry a real value (e.g. height 0 m)
			double outlierProbability = (1 - ErrorFunction.getProbability(sigmaFactor)) / 2.;
			double extremumRange = getQuantile(1. - outlierProbability) - getQuantile(outlierProbability);
			while (this.iPopulation.get(0) < getQuantile(outlierProbability) - extremumRange * outlierFactor) {
				this.iOutliers.add(this.iPopulation.get(0));
				this.iPopulation.remove(0);
			}
			while (this.iPopulation.get(this.iPopulation.size() - 1) > getQuantile(1. - outlierProbability) + extremumRange * outlierFactor) {
				this.iOutliers.add(this.iPopulation.get(this.dPopulation.size() - 1));
				this.iPopulation.remove(this.iPopulation.size() - 1);
			}
		}
		log.finest(() -> "" + population.size() + Arrays.toString(population.toArray()));
		log.finest(() -> "" + this.iPopulation.size() + Arrays.toString(this.iPopulation.toArray()));
	}

	/**
	 * Uses the <em>y<sub>i</sub></em> data points only.
	 * @param population holds the unordered data points <em>x<sub>i</sub></em>, <em>y<sub>i</sub></em> (<em>n > 0</em>)
	 * @param sigmaFactor defines the tolerance interval (<em>TI = &plusmn; z * &sigma; with z >= 0</em>)
	 * @param outlierFactor specifies the outlier distance limit ODL from the tolerance interval (<em>ODL = &rho; * TI with &rho; > 0</em>)
	 */
	public Quantile(List<Point2D.Double> population, double sigmaFactor, double outlierFactor) {
		if (population.isEmpty()) {
			throw new IllegalArgumentException();
		}
		this.iPopulation = null;
		this.fixings = EnumSet.of(Fixings.IS_SAMPLE);
		this.iOutliers = null;
		this.dOutliers = null;
		this.d2Outliers = new ArrayList<Point2D.Double>();

		this.firstFigure = population.get(0).getY();
		this.lastFigure = population.get(population.size() - 1).getY();

		ArrayList<Point2D.Double> d2Population = new ArrayList<>(population);
		Collections.sort(d2Population, Quantile.Y_ORDER);

		this.dPopulation = d2Population.stream().map(p -> p.y).collect(Collectors.toList());

		// remove outliers except: if all outliers have the same value we expect them to carry a real value (e.g. height 0 m)
		double outlierProbability = (1 - ErrorFunction.getProbability(sigmaFactor)) / 2.;
		double extremumRange = getQuantile(1. - outlierProbability) - getQuantile(outlierProbability);
		while (this.dPopulation.get(0) < getQuantile(outlierProbability) - extremumRange * outlierFactor) {
			this.dPopulation.remove(0);
			this.d2Outliers.add(d2Population.get(0));
			d2Population.remove(0);
		}
		while (this.dPopulation.get(this.dPopulation.size() - 1) > getQuantile(1. - outlierProbability) + extremumRange * outlierFactor) {
			this.dPopulation.remove(this.dPopulation.size() - 1);
			this.d2Outliers.add(d2Population.get(d2Population.size() - 1));
			d2Population.remove(d2Population.size() - 1);
		}

		log.finest(() -> "" + population.size() + Arrays.toString(population.toArray()));
		log.finest(() -> "" + this.dPopulation.size() + Arrays.toString(this.dPopulation.toArray()));
		log.finest(() -> String.format("lWhisker=%f q1=%f q2=%f q3=%f uWhisker=%f", getQuantileLowerWhisker(), getQuartile1(), getQuartile2(), getQuartile3(), getQuantileUpperWhisker()));

	}

	/**
	 * @param population holds the unordered values (<em>n > 0</em>)
	 * @param fixings defines how to proceed with the data
	 * @param sigmaFactor defines the tolerance interval (<em>TI = &plusmn; z * &sigma; with z >= 0</em>)
	 * @param outlierFactor specifies the outlier distance limit ODL from the tolerance interval (<em>ODL = &rho; * TI with &rho; > 0</em>)
	 */
	public Quantile(Collection<Double> population, EnumSet<Fixings> fixings, double sigmaFactor, double outlierFactor) {
		if (population.isEmpty()) {
			throw new IllegalArgumentException();
		}
		this.iPopulation = null;
		this.fixings = fixings;
		this.iOutliers = null;
		this.dOutliers = new ArrayList<Double>();
		this.d2Outliers = null;

		List<Double> excludes = new ArrayList<Double>();
		if (fixings.contains(Fixings.REMOVE_NULLS)) excludes.add(null);
		if (fixings.contains(Fixings.REMOVE_ZEROS)) excludes.add(0.);
		if (fixings.contains(Fixings.REMOVE_MAXMIN)) {
			excludes.add(-Double.MAX_VALUE);
			excludes.add(Double.MAX_VALUE);
		}
		if (excludes.isEmpty()) {
			this.dPopulation = new ArrayList<>(population);
		} else {
			this.dPopulation = new ArrayList<>();
			for (Double value : population) {
				if (!excludes.contains(value)) this.dPopulation.add(value);
			}
		}
		this.firstFigure = this.dPopulation.get(0) != null ? this.dPopulation.get(0) : -Double.MAX_VALUE;
		this.lastFigure = this.dPopulation.get(this.dPopulation.size() - 1) != null ? this.dPopulation.get(this.dPopulation.size() - 1)
				: -Double.MAX_VALUE;

		Collections.sort(this.dPopulation);

		// remove outliers except: if all outliers have the same value we expect them to carry a real value (e.g. height 0 m)
		double outlierProbability = (1 - ErrorFunction.getProbability(sigmaFactor)) / 2.;
		double extremumRange = getQuantile(1. - outlierProbability) - getQuantile(outlierProbability);
		while (this.dPopulation.get(0) < getQuantile(outlierProbability) - extremumRange * outlierFactor) {
			this.dOutliers.add(this.dPopulation.get(0));
			this.dPopulation.remove(0);
		}
		while (this.dPopulation.get(this.dPopulation.size() - 1) > getQuantile(1. - outlierProbability) + extremumRange * outlierFactor) {
			this.dOutliers.add(this.dPopulation.get(this.dPopulation.size() - 1));
			this.dPopulation.remove(this.dPopulation.size() - 1);
		}

		log.finest(() -> "" + population.size() + Arrays.toString(population.toArray()));
		log.finest(() -> "" + this.dPopulation.size() + Arrays.toString(this.dPopulation.toArray()));
		log.finest(() -> String.format("lWhisker=%f q1=%f q2=%f q3=%f uWhisker=%f", getQuantileLowerWhisker(), getQuartile1(), getQuartile2(), getQuartile3(), getQuantileUpperWhisker()));
	}

	/**
	 * constructor based on a vector, e.g. the record.
	 * @param iPopulation
	 * @param fixings define how to proceed with the data
	 */
	public Quantile(Vector<Integer> iPopulation, EnumSet<Fixings> fixings) {
		this.dPopulation = null;
		if (iPopulation.isEmpty()) throw new UnsupportedOperationException();
		this.fixings = fixings;
		this.iOutliers = null;
		this.dOutliers = new ArrayList<Double>();
		this.d2Outliers = null;

		Stream<Integer> stream = iPopulation.parallelStream();
		if (fixings.contains(Fixings.REMOVE_NULLS)) stream = stream.filter(Objects::nonNull);
		if (fixings.contains(Fixings.REMOVE_ZEROS)) stream = stream.filter(x -> x != 0);
		if (fixings.contains(Fixings.REMOVE_MAXMIN)) {
			Integer[] excludes = { Integer.MIN_VALUE, Integer.MAX_VALUE };
			stream = stream.filter(x -> Arrays.asList(excludes).contains(x));
		}
		this.iPopulation = stream.sorted().collect(Collectors.toList());

		// do not remove outliers

		log.fine(() -> "i " + iPopulation.size() + " " + this.iPopulation.size()); //$NON-NLS-1$ //$NON-NLS-2$
		log.finest(() -> Arrays.toString(iPopulation.toArray()));
		log.finest(() -> Arrays.toString(this.iPopulation.toArray()));
	}

	/**
	 * constructor based on any collection.
	 * @param dPopulation
	 * @param fixings define how to proceed with the data
	 */
	public Quantile(Collection<Double> dPopulation, EnumSet<Fixings> fixings) {
		this.iPopulation = null;
		if (dPopulation.isEmpty()) throw new UnsupportedOperationException();
		this.fixings = fixings;
		this.iOutliers = null;
		this.dOutliers = new ArrayList<Double>();
		this.d2Outliers = null;

		Stream<Double> stream = dPopulation.parallelStream();
		if (fixings.contains(Fixings.REMOVE_NULLS)) stream = stream.filter(Objects::nonNull);
		if (fixings.contains(Fixings.REMOVE_ZEROS)) stream = stream.filter(x -> x != 0);
		if (fixings.contains(Fixings.REMOVE_MAXMIN)) {
			Double[] excludes = { -Double.MAX_VALUE, Double.MAX_VALUE };
			stream = stream.filter(x -> Arrays.asList(excludes).contains(x));
		}
		this.dPopulation = stream.sorted().collect(Collectors.toList());

		// do not remove outliers

		log.fine(() -> "d " + dPopulation.size() + " " + this.dPopulation.size()); //$NON-NLS-1$ //$NON-NLS-2$
		log.finest(() -> Arrays.toString(dPopulation.toArray()));
		log.finest(() -> Arrays.toString(this.dPopulation.toArray()));
	}

	public double getMaxFigure() {
		if (this.maxFigure == null) {
			if (this.dPopulation == null)
				this.maxFigure = (double) this.iPopulation.parallelStream().mapToInt(x -> x).max().getAsInt();
			else
				this.maxFigure = this.dPopulation.parallelStream().mapToDouble(x -> x).max().getAsDouble();
		}
		return this.maxFigure;
	}

	public double getMinFigure() {
		if (this.minFigure == null) {
			if (this.dPopulation == null)
				this.minFigure = (double) this.iPopulation.parallelStream().mapToInt(x -> x).min().getAsInt();
			else
				this.minFigure = this.dPopulation.parallelStream().mapToDouble(x -> x).min().getAsDouble();
		}
		return this.minFigure;
	}

	public double getSumFigure() {
		if (this.sumFigure == null) {
			if (this.dPopulation == null)
				this.sumFigure = (double) this.iPopulation.parallelStream().mapToInt(x -> x).sum(); // do not extend to long as the vault only holds integer
																																														// values
			else
				this.sumFigure = this.dPopulation.parallelStream().mapToDouble(x -> x).sum();
		}
		return this.sumFigure;
	}

	public double getAvgFigure() {
		if (this.avgFigure == null) {
			if (this.dPopulation == null)
				this.avgFigure = this.iPopulation.parallelStream().mapToInt(x -> x).average().getAsDouble();
			else
				this.avgFigure = this.dPopulation.parallelStream().mapToDouble(x -> x).average().getAsDouble();
		}
		return this.avgFigure;
	}

	public double getAvgOBS() {
		if (this.dPopulation == null) {
			double avg = 0;
			for (int i = 0; i < this.iPopulation.size(); i++) {
				double value = this.iPopulation.get(i).doubleValue();
				avg += (value - avg) / (i + 1);
			}
			return avg;
		} else {
			double avg = 0;
			for (int i = 0; i < this.dPopulation.size(); i++) {
				double value = this.dPopulation.get(i);
				avg += (value - avg) / (i + 1);
			}
			return avg;
		}
	}

	public double getAvgSlow() {
		if (this.dPopulation == null)
			return this.iPopulation.parallelStream().collect(StatsHelper::new, StatsHelper::accept, StatsHelper::combine).getAvg();
		else
			return this.dPopulation.parallelStream().collect(StatsHelper::new, StatsHelper::accept, StatsHelper::combine).getAvg();
	}

	public double getSigmaOBS() {
		if (this.dPopulation == null) {
			double avg = getAvgOBS();
			double varTimesN = 0;
			for (int i = 0; i < this.iPopulation.size(); i++) {
				double value = this.iPopulation.get(i).doubleValue();
				varTimesN += (value - avg) * (value - avg);
			}
			return this.iPopulation.size() > 0 ? Math.sqrt(varTimesN / (this.fixings.contains(Fixings.IS_SAMPLE) ? this.iPopulation.size() - 1
					: this.iPopulation.size())) : 0;
		} else {
			double avg = getAvgOBS();
			double varTimesN = 0;
			for (int i = 0; i < this.dPopulation.size(); i++) {
				double value = this.dPopulation.get(i);
				varTimesN += (value - avg) * (value - avg);
			}
			return this.dPopulation.size() > 0 ? Math.sqrt(varTimesN / (this.fixings.contains(Fixings.IS_SAMPLE) ? this.dPopulation.size() - 1
					: this.dPopulation.size())) : 0;
		}
	}

	public double getSigmaRunningOBS() {
		if (this.dPopulation == null) {
			double avg = 0;
			double varTimesN = 0;
			int count = 0;
			for (int i = 0; i < this.iPopulation.size(); i++) {
				double value = this.iPopulation.get(i).doubleValue();
				varTimesN += (value - avg) * (value - avg) * count / ++count; // pls note the counter increment
				avg += (value - avg) / count;
			}
			return this.iPopulation.size() > 0 ? Math.sqrt(varTimesN / (this.fixings.contains(Fixings.IS_SAMPLE) ? this.iPopulation.size() - 1
					: this.iPopulation.size())) : 0;
		} else {
			double avg = 0;
			double varTimesN = 0;
			double count = 0; // double count improves the performance in this case (not in the integer based branch)
			for (int i = 0; i < this.dPopulation.size(); i++) {
				double value = this.dPopulation.get(i);
				varTimesN += (value - avg) * (value - avg) * count / ++count; // pls note the counter increment
				avg += (value - avg) / count;
			}
			return this.dPopulation.size() > 0 ? Math.sqrt(varTimesN / (this.fixings.contains(Fixings.IS_SAMPLE) ? this.dPopulation.size() - 1
					: this.dPopulation.size())) : 0;
		}
	}

	public double getSigmaFigure() {
		// takes about 310 ms for 100 iPopulations with 500k members on ET's machine (compared to 680/500 ms for sigmaOBS/sigmaRunningOBS )
		if (this.sigmaFigure == null) {
			if (this.dPopulation == null) {
				this.sigmaFigure = this.iPopulation.parallelStream().collect(StatsHelper::new, StatsHelper::accept, StatsHelper::combine).getSigma(this.fixings.contains(Fixings.IS_SAMPLE));
			} else
				this.sigmaFigure = this.dPopulation.parallelStream().collect(StatsHelper::new, StatsHelper::accept, StatsHelper::combine).getSigma(this.fixings.contains(Fixings.IS_SAMPLE));
		}
		return this.sigmaFigure;
	}

	/**
	 * The sample quantile is calculated according to R-6, SAS-4, SciPy-(0,0), Maple-5 which is piecewise linear and symmetric.
	 * @param probabilityCutPoint as value between 0 and 1
	 * @return the quantile for the population or for a sample
	 */
	public double getQuantile(double probabilityCutPoint) {
		if (this.dPopulation == null) {
			int pSize = this.iPopulation.size();
			if (this.fixings.contains(Fixings.IS_SAMPLE)) {
				if (probabilityCutPoint >= 1. / (pSize + 1) && probabilityCutPoint < (double) pSize / (pSize + 1)) {
					double position = (pSize + 1) * probabilityCutPoint;
					return this.iPopulation.get((int) position - 1) + (position - (int) position) * (this.iPopulation.get((int) position) - this.iPopulation.get((int) position - 1));
				} else if (probabilityCutPoint < 1. / (pSize + 1))
					return this.iPopulation.get(0);
				else
					return this.iPopulation.get(pSize - 1);
			} else {
				if (probabilityCutPoint > 0. && probabilityCutPoint < 1.) {
					double position = pSize * probabilityCutPoint;
					if (position % 2 == 0)
						// take elements p-1 and p due to zerobased index
						return (this.iPopulation.get((int) position) + this.iPopulation.get((int) (position + 1))) / 2.;
					else
						// take element p due to zerobased index in combination with upper bound operation in the calculation rule
						return this.iPopulation.get((int) (position));
				} else if (probabilityCutPoint == 0.)
					return this.iPopulation.get(0);
				else
					return this.iPopulation.get(pSize - 1);
			}
		} else {
			int pSize = this.dPopulation.size();
			if (this.fixings.contains(Fixings.IS_SAMPLE)) {
				if (probabilityCutPoint >= 1. / (pSize + 1) && probabilityCutPoint < (double) pSize / (pSize + 1)) {
					double position = (pSize + 1) * probabilityCutPoint;
					return this.dPopulation.get((int) position - 1) + (position - (int) position) * (this.dPopulation.get((int) position) - this.dPopulation.get((int) position - 1));
				} else if (probabilityCutPoint < 1. / (pSize + 1))
					return this.dPopulation.get(0);
				else
					return this.dPopulation.get(pSize - 1);
			} else {
				if (probabilityCutPoint > 0. && probabilityCutPoint < 1.) {
					double position = pSize * probabilityCutPoint;
					if (position % 2 == 0)
						// take elements p-1 and p due to zerobased index
						return (this.dPopulation.get((int) position) + this.dPopulation.get((int) (position + 1))) / 2.;
					else
						// take element p due to zerobased index in combination with upper bound operation in the calculation rule
						return this.dPopulation.get((int) (position));
				} else if (probabilityCutPoint == 0.)
					return this.dPopulation.get(0);
				else
					return this.dPopulation.get(pSize - 1);
			}
		}
	}

	public double getQuartile0() {
		return this.dPopulation == null ? this.iPopulation.get(0) : this.dPopulation.get(0);
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
		return this.dPopulation == null ? this.iPopulation.get(this.iPopulation.size() - 1) : this.dPopulation.get(this.dPopulation.size() - 1);
	}

	public double getInterQuartileRange() {
		return getQuantile(.75) - getQuantile(.25);
	}

	public double getQuantileLowerWhisker() {
		final double probabilityCutPoint = .25;
		final double whiskerStartValue = getQuantile(probabilityCutPoint);
		final double whiskerLimitValue = whiskerStartValue - getInterQuartileRange() * 1.5;
		double value = whiskerStartValue;
		if (this.dPopulation == null) {
			for (int i = 0; i < this.iPopulation.size() * probabilityCutPoint; i++) {
				if (this.iPopulation.get(i) >= whiskerLimitValue) {
					// get the corrected value which is crucial for samples
					// add .5 due to zerobased index and rule 0<p<1 which implies an index average value
					value = getQuantile((.5 + i) / this.iPopulation.size());
					// take the whisker limit value if the interpolation / estimation value is beyond the limit
					value = value < whiskerLimitValue ? whiskerLimitValue : value;
					break;
				}
			}
		} else {
			for (int i = 0; i < this.dPopulation.size() * probabilityCutPoint; i++) {
				if (this.dPopulation.get(i) >= whiskerLimitValue) {
					// get the corrected value which is crucial for samples
					// add .5 due to zerobased index and rule 0<p<1 which implies an index average value
					value = getQuantile((.5 + i) / this.dPopulation.size());
					// take the whisker limit value if the interpolation / estimation value is beyond the limit
					value = value < whiskerLimitValue ? whiskerLimitValue : value;
					break;
				}
			}
		}
		return value;
	}

	public double getQuantileUpperWhisker() {
		final double probabilityCutPoint = .75;
		final double whiskerStartValue = getQuantile(probabilityCutPoint);
		final double whiskerLimitValue = whiskerStartValue + getInterQuartileRange() * 1.5;
		double value = whiskerStartValue;
		if (this.dPopulation == null) {
			for (int i = this.iPopulation.size() - 1; i > this.iPopulation.size() * probabilityCutPoint; i--) {
				if (this.iPopulation.get(i) <= whiskerLimitValue) {
					// get the corrected value which is crucial for samples
					// add .5 due to zerobased index and rule 0<p<1 which implies an index average value
					value = getQuantile((.5 + i) / this.iPopulation.size());
					// take the whisker limit value if the interpolation / estimation value is beyond the limit
					value = value > whiskerLimitValue ? whiskerLimitValue : value;
					break;
				}
			}
		} else {
			for (int i = this.dPopulation.size() - 1; i > this.dPopulation.size() * probabilityCutPoint; i--) {
				if (this.dPopulation.get(i) <= whiskerLimitValue) {
					// get the corrected value which is crucial for samples
					// add .5 due to zerobased index and rule 0<p<1 which implies an index average value
					value = getQuantile((.5 + i) / this.dPopulation.size());
					// take the whisker limit value if the interpolation / estimation value is beyond the limit
					value = value > whiskerLimitValue ? whiskerLimitValue : value;
					break;
				}
			}
		}
		return value;
	}

	/**
	 * @return the first value after the fixing actions
	 */
	public double getFirstFigure() {
		return this.firstFigure;
	}

	/**
	 * @return the last value after the fixing actions
	 */
	public double getLastFigure() {
		return this.lastFigure;
	}

	/**
	 * @return the population size after the fixing actions
	 */
	public int getSize() {
		return this.dPopulation == null ? this.iPopulation.size() : this.dPopulation.size();
	}

}
