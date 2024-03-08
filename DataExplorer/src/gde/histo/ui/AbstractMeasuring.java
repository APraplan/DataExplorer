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

package gde.histo.ui;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

/**
 * Measure one single timestep or a time delta interactively.
 * @author Thomas Eickert (USER)
 */
public abstract class AbstractMeasuring {

	protected final Measure measure;

	public AbstractMeasuring(Measure measure) {
		this.measure = measure;
	}

	/**
	 * Reset the graphic area and comment.
	 */
	public abstract void cleanMeasuring();

	/**
	 * Draw a refreshed measurement.
	 */
	public abstract void drawMeasuring(GC canvasGC);

	/**
	 * Draw the survey graphics while moving the vertical line.
	 */
	public abstract void processMouseDownMove(long timestamp_ms);

	/**
	 * Perform UI activities at mouse up movements.
	 */
	public abstract void processMouseUpMove(Point point);

	/**
	 * Determine which vertical line was moved.
	 */
	public abstract void processMouseDownAction(Point point);

	/**
	 * Reset the identified vertical line.
	 */
	public abstract void processMouseUpAction(Point point);

}