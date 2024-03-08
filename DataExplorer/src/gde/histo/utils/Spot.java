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

/**
 *  Immutable data type to encapsulate a two-dimensional point. <br>
 *  Comparisons are based on the y coordinate value.
 *  <p>
 *  Note: copes with the special behavior of floating point numbers with respect to -0.0 and +0.0.
 * 	@author Thomas Eickert
 * 	@param <T> supports numbers
 */
public final class Spot<T extends Number> implements Comparable<Spot<T>> {

	private final T	x;	// x coordinate
	private final T	y;	// y coordinate

	/**
	 * @param x the x coordinate
	 * @param y the y coordinate
	 */
	@SuppressWarnings("unchecked") // ET there is no simple solution to create an instance of T; see https://stackoverflow.com/questions/75175/create-instance-of-generic-type-in-java
	public Spot(T x, T y) {
		if (x instanceof Double) {
			if (Double.isInfinite(x.doubleValue()) || Double.isInfinite(y.doubleValue())) throw new IllegalArgumentException("infinite");
			if (Double.isNaN(x.doubleValue()) || Double.isNaN(y.doubleValue())) throw new IllegalArgumentException("NaN");
			// convert -0.0 to +0.0
			this.x = x.doubleValue() == 0.0 ? (T) Double.valueOf(0.0) : x;
			this.y = y.doubleValue() == 0.0 ? (T) Double.valueOf(0.0) : y;
		} else if (x instanceof Float) {
			if (Float.isInfinite(x.floatValue()) || Float.isInfinite(y.floatValue())) throw new IllegalArgumentException("infinite");
			if (Float.isNaN(x.floatValue()) || Float.isNaN(y.floatValue())) throw new IllegalArgumentException("NaN");
			// convert -0.0 to +0.0
			this.x = x.floatValue() == 0.0 ? (T) Float.valueOf(0.0f) : x;
			this.y = y.floatValue() == 0.0 ? (T) Float.valueOf(0.0f) : y;
		} else {
			this.x = x;
			this.y = y;
		}
	}

	/**
	 * @return the x coordinate
	 */
	public T x() {
		return this.x;
	}

	/**
	 * @return the y coordinate
	 */
	public T y() {
		return this.y;
	}

	/**
	 * @return the comparison result based on the y value (and on the x value in case of equal y values)
	 */
	@Override
	public int compareTo(Spot<T> that) {
		if (this.y.doubleValue() < that.y.doubleValue()) return -1;
		if (this.y.doubleValue() > that.y.doubleValue()) return +1;
		// take the x value for comparison
		if (this.x.doubleValue() < that.x.doubleValue()) return -1;
		if (this.x.doubleValue() > that.x.doubleValue()) return +1;
		return 0;
	}

	/**
	 * @return a string representation of this point in the format (x, y)
	 */
	@Override
	public String toString() {
		return "(" + this.x + ", " + this.y + ")";
	}
}
