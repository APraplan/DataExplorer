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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import gde.GDE;
import gde.config.Settings;
import gde.histo.datasources.HistoSet;
import gde.log.Logger;

/**
 * Removes outliers (defined by the tolerance interval) and outcasts (determined in advance).
 * @author Thomas Eickert
 */
public class UniversalQuantile<T extends Number & Comparable<T>> extends ElementaryQuantile<T> {
	private static final String	$CLASS_NAME					= UniversalQuantile.class.getName();
	private static final Logger	log									= Logger.getLogger($CLASS_NAME);

	/**
	 * Values beyond this limit are outliers.<br>
	 * Maximum whisker length <em>MWL = c * IQR</em>.
	 */
	public static final double	CLOSE_OUTLIER_LIMIT	= 1.5;
	/**
	 * Outliers below this limit are close outliers, those beyond this limit are far outliers.<br>
	 * Outlier distance limit <em>ODL = c * IQR</em>.
	 */
	public static final int			FAR_OUTLIER_LIMIT		= 3;

	/**
	 * values to be eliminated from the population
	 */
	private final Set<T>				outcasts;
	/**
	 * outlier and outcast members removed from the trunk
	 */
	private final List<T>				castaways						= new ArrayList<>();
	/**
	 * most frequent outlier candidates value within the ODL which are removed from in the trunk
	 */
	private final List<T>				constantScraps			= new ArrayList<>();

	private T										firstValidElement;
	private T										lastValidElement;

	/**
	 * Is used for samples, eliminates standard outliers (no constant outliers) and supports complex objects.
	 * @param population holds the y value which is taken as a sample (for the standard deviation)
	 * @param sigmaFactor specifies the confidence interval <em>CI = &plusmn; z * &sigma; with z >= 0</em>
	 * @param outlierFactor specifies the outlier distance limit ODL from the confidence interval (<em>ODL = &rho; * CI with &rho; > 0</em>)
	 */
	public static UniversalQuantile<Double> createUniversalSpotQuantile(Collection<Spot<Double>> population, double sigmaFactor, double outlierFactor, Settings settings) {
		List<Double> trunk = new ArrayList<>();
		for (Spot<Double> spot : population) {
			trunk.add(spot.y());
		}
		boolean isSample = true;
		return new UniversalQuantile<Double>(trunk, isSample, sigmaFactor, outlierFactor, settings);
	}

	/**
	 * Used for channel item values from records during vault creation.
	 * @param population
	 * @param isSample true calculates the sample standard deviation
	 * @param removeCastaways true removes castaways which lie beyond 9 * CI (confidence interval)
	 * @param removeConstantOutliers true removes constant outliers lie beyond 3 * CI, are the outmost value and occur with the highest
	 *          frequency in this range
	 */
	public UniversalQuantile(Collection<T> population, boolean isSample, boolean removeCastaways, boolean removeConstantOutliers, Settings settings) {
		this(population, isSample, INTER_QUARTILE_SIGMA_FACTOR, removeCastaways ? settings.getOutlierToleranceSpread() : 9999., //
				removeConstantOutliers ? FAR_OUTLIER_LIMIT : removeCastaways ? settings.getOutlierToleranceSpread() : 9999., new HashSet<>(), settings);
	}

	/**
	 * Eliminates standard outliers (no constant outliers).
	 * @param population
	 * @param isSample true calculates the sample standard deviation
	 * @param sigmaFactor specifies the confidence interval <em>CI = &plusmn; z * &sigma; with z >= 0</em>
	 * @param outlierFactor specifies the outlier distance limit ODL from the confidence interval (<em>ODL = &rho; * CI with &rho; > 0</em>)
	 */
	public UniversalQuantile(List<T> population, boolean isSample, double sigmaFactor, double outlierFactor, Settings settings) {
		this(population, isSample, sigmaFactor, outlierFactor, outlierFactor, new HashSet<>(), settings);
	}

	/**
	 * Eliminates all types of outliers and outcasts.
	 * @param population
	 * @param isSample true calculates the sample standard deviation
	 * @param sigmaFactor specifies the confidence interval <em>CI = &plusmn; z * &sigma; with z >= 0</em>
	 * @param outlierFactor specifies the outlier distance limit ODL from the confidence interval (<em>ODL = &rho; * CI with &rho; > 0</em>)
	 * @param constantOutlierFactor defines the inner limits for constant value elimination after outlier elimination
	 * @param outcasts holds list members which are eliminated before the quantiles calculation
	 */
	public UniversalQuantile(Collection<T> population, boolean isSample, double sigmaFactor, double outlierFactor, double constantOutlierFactor,
			Set<T> outcasts, Settings settings) {
		super(!outcasts.isEmpty() ? new ArrayList<T>() : new ArrayList<T>(population), isSample, settings);
		this.outcasts = outcasts;
		if (population == null || population.isEmpty()) throw new IllegalArgumentException("empty population");

		if (!outcasts.isEmpty()) {
			// build trunk from scratch on
			// this.trunk = new ArrayList<T>();
			for (T element : population) {
				if (outcasts.contains(element))
					this.castaways.add(element);
				else {
					this.trunk.add(element);
				}
			}
			Collections.sort(this.trunk);
		}

		removeOutliers(sigmaFactor, outlierFactor, constantOutlierFactor);

		List<T> populationList = population instanceof List<?> ? (List<T>) population : new ArrayList<T>(population);
		this.firstValidElement = null;
		// walk forward and get the first element which is not in the outcast / outlier lists
		for (ListIterator<T> listIterator = populationList.listIterator(); listIterator.hasNext();) {
			T t = listIterator.next();
			if (!this.castaways.contains(t)) {
				this.firstValidElement = t;
				break;
			}
		}
		this.lastValidElement = null;
		// walk backward and get the first element which is not in the outcast / outlier lists
		for (ListIterator<T> listIterator = populationList.listIterator(populationList.size()); listIterator.hasPrevious();) {
			T t = listIterator.previous();
			if (!this.castaways.contains(t)) {
				this.lastValidElement = t;
				break;
			}
		}

		log.finest(() -> "" + populationList.size() + Arrays.toString(populationList.toArray()));
		log.finest(() -> "" + this.trunk.size() + Arrays.toString(this.trunk.toArray()));
	}

	/**
	 * Removes at first the outliers beyond the outlier distance limit.
	 * Also removes constant values beyond the far outlier range.
	 * They are suspected to originate from a technical reason.
	 * @param sigmaFactor
	 * @param outlierFactor defines the inner limits for outlier elimination
	 * @param constantOutlierFactor defines the inner limits for constant value elimination after outlier elimination
	 */
	private void removeOutliers(double sigmaFactor, double outlierFactor, double constantOutlierFactor) {
		if (constantOutlierFactor > outlierFactor) throw new IllegalArgumentException();

		final double outlierProbability = (1 - ErrorFunction.getProbability(sigmaFactor)) / 2.;
		double[] toleranceLowerUpper = getQuantileToleranceLowerUpper(sigmaFactor);
		if (HistoSet.fuzzyEquals(toleranceLowerUpper[0], 0.) || HistoSet.fuzzyEquals(toleranceLowerUpper[1], 0.)) return;

		{
			List<T> candidates = new ArrayList<>();  // allows specimen counts in contrast to a set
			double nonOutlierRange = toleranceLowerUpper[0] * 2. * outlierFactor;
			double nonConstantOutlierRange = toleranceLowerUpper[0] * 2. * constantOutlierFactor;
			double q1WithOutliers = getQuantile(outlierProbability);
			ListIterator<T> iterator = trunk.listIterator();
			while (iterator.hasNext()) {
				T value = iterator.next();
				if (value.doubleValue() < q1WithOutliers - nonOutlierRange) {
					castaways.add(value);
					iterator.remove();
				} else if (value.doubleValue() < q1WithOutliers - nonConstantOutlierRange) {
					candidates.add(value);
				} else
					break;
			}
			if (castaways.isEmpty() && candidates.size() > 2) { // remove only if a significant amount
				if (removePrevalentValues(candidates.get(0), candidates)) constantScraps.add(candidates.get(0));
			}
		}
		{
			List<T> candidates = new ArrayList<>(); // allows specimen counts in contrast to a set
			double nonOutlierRange = toleranceLowerUpper[1] * 2. * outlierFactor;
			double nonConstantOutlierRange = toleranceLowerUpper[1] * 2. * constantOutlierFactor;
			double q3WithOutliers = getQuantile(1. - outlierProbability);
			ListIterator<T> iterator = trunk.listIterator(trunk.size());
			while (iterator.hasPrevious()) {
				T value = iterator.previous();
				if (value.doubleValue() > q3WithOutliers + nonOutlierRange) {
					castaways.add(value);
					iterator.remove();
				} else if (value.doubleValue() > q3WithOutliers + nonConstantOutlierRange) {
					candidates.add(value);
				} else
					break;
			}
			if (castaways.isEmpty() && candidates.size() > 2) { // remove only if a significant amount
				if (removePrevalentValues(candidates.get(0), candidates)) constantScraps.add(candidates.get(0));
			}
		}
		if (trunk.isEmpty()) throw new UnsupportedOperationException("empty trunk");
	}

	/**
	 * Constant outliers are supposed to originate from a technical origin.
	 * The {@code scrappableCandidate} is a prevalent value if it is the most common in {@code candidates}.
	 * @param scrappableCandidate is the value for removal
	 * @param candidates holds a sublist from the trunk
	 * @return true if at least one {@code scrappableCandidate} value was removed from the trunk
	 */
	private boolean removePrevalentValues(T scrappableCandidate, List<T> candidates) {
		boolean[] toRemove = new boolean[] { false };

		T halfWayValue = candidates.get(candidates.size() / 2);
		toRemove[0] = scrappableCandidate.equals(halfWayValue);

		if (!toRemove[0]) { // go the hard way
			log.finest(() -> candidates.stream().collect(Collectors.groupingBy(t -> t, Collectors.counting())).entrySet().toString());
			Optional<Entry<T, Long>> mostFrequentValueGroup = candidates.stream() //
					.collect(Collectors.groupingBy(t -> t, Collectors.counting())).entrySet() //
					.stream().max(Comparator.comparing(Entry::getValue));
			mostFrequentValueGroup.ifPresent(e -> {
				T mostFrequentValue = e.getKey();
				toRemove[0] = scrappableCandidate.equals(mostFrequentValue) && e.getValue() > 1;
			});
		}

		if (toRemove[0]) {
			// remove all entries with this value
			return trunk.removeAll(Collections.singleton(scrappableCandidate));
		} else
			return false;
	}

	/**
	 * @return the value of the maximum element before outcast elimination and removing the outliers
	 */
	public double getPopulationMaxFigure() {
		double realMax = getQuartile4();
		for (T t : castaways) {
			realMax = Math.max(realMax, t.doubleValue());
		}
		return realMax;
	}

	/**
	 * @return the value of the minimum element before outcast elimination and removing the outliers
	 */
	public double getPopulationMinFigure() {
		double realMin = getQuartile0();
		for (T t : castaways) {
			realMin = Math.min(realMin, t.doubleValue());
		}
		return realMin;
	}

	/**
	 * @return the outliers based on the sigmaFactor and the outlierFactor
	 */
	public List<T> getOutliers() {
		List<T> outliers = new ArrayList<>(castaways);
		outliers.removeAll(outcasts);
		return outliers;
	}

	public String getOutliersCsv() {
		return getOutliers().stream().map(T::doubleValue).map(String::valueOf).collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR));
	}

	public List<T> getConstantScraps() {
		return constantScraps;
	}

	public String getConstantScrapsCsv() {
		return constantScraps.stream().map(T::doubleValue).map(String::valueOf).collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR));
	}

	@Override
	public String toString() {
		return "isSample=" + isSample + ", size=" + getSize() + ", castawaysSize=" + castaways.size() + ", constantScraps=" + getConstantScrapsCsv() //
				+ ", sumFigure=" + getSumFigure() + ", avgFigure=" + getAvgFigure() + ", sigmaFigure=" + getSigmaFigure() + "";
	}

	/**
	 * @return the value of the first element after outcast elimination and removing the outliers
	 */
	public double getFirstFigure() {
		return firstValidElement.doubleValue();
	}

	/**
	 * @return the value of the last element after outcast elimination and removing the outliers
	 */
	public double getLastFigure() {
		return lastValidElement.doubleValue();
	}

}
