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

package gde.histo.recordings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.Point;

import gde.histo.datasources.HistoSet;
import gde.histo.ui.GraphicsComposite.GraphicsLayout;
import gde.histo.utils.HistoTimeLine;
import gde.log.Logger;

/**
 * Record data mapping for the histo graphics tab.
 * @author Thomas Eickert (USER)
 */
public final class HistoGraphicsMapper {
	private final static String	$CLASS_NAME	= TrailSelector.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Data points of multiple measurements or lines or curves.
	 */
	public static final class PointArray {

		private int									x;																					// x value of the curve
		private final Integer[]			y;																					// y values of the curve

		public PointArray(int ySize) {
			this.y = new Integer[ySize];
		}

		public int getX() {
			return this.x;
		}

		public void setX(int x) {
			this.x = x;
		}

		public Integer[] getY() {
			return this.y;
		}

		public void setY(int index, Integer value) {
			this.y[index] = value;
		}

		/**
		 * @param suiteOrdinal
		 * @return the point value of the suite member
		 */
		public Integer getY(int suiteOrdinal) {
			return this.getY()[suiteOrdinal];
		}

		@Override
		public String toString() {
			return "PointArray [x=" + this.x + ", y=" + Arrays.toString(this.y) + "]";
		}

	}

	/**
	 * Query the values for display.
	 * @return the display point x axis value and multiple y axis values; null if the trail record value is null
	 */
	public static List<PointArray> getSuiteDisplayPoints(GraphicsLayout graphicsData, HistoTimeLine timeLine) {
		List<PointArray> suitePoints = new ArrayList<>();
		TrailRecord trailRecord = graphicsData.getTrailRecord();
		int firstOrdinal = trailRecord.getTrailSelector().getSuiteMasterIndex();
		for (int i = 0; i < timeLine.getScalePositions().size(); i++) {
			if (trailRecord.getSuiteRecords().getSuiteValue(firstOrdinal, i) != null)
				suitePoints.add(getSuiteDisplayPoints(graphicsData, timeLine, i));
			else
				suitePoints.add(null);
		}
		return suitePoints;
	}

	private static PointArray getSuiteDisplayPoints(GraphicsLayout graphicsData, HistoTimeLine timeLine, int index) {
		int x0 = timeLine.getCurveAreaBounds().x;
		int y0 = timeLine.getCurveAreaBounds().height + timeLine.getCurveAreaBounds().y;
		TrailRecord trailRecord = graphicsData.getTrailRecord();
		int suiteSize = trailRecord.getTrailSelector().getSuiteMembers().size();
		PointArray pointArray = new PointArray(suiteSize);
		pointArray.setX(x0 + timeLine.getScalePositions().get((long) trailRecord.getParent().getTime_ms(index)));

		double yOffset = getYOffset(graphicsData);
		SuiteRecords suiteRecords = trailRecord.getSuiteRecords();
		for (int j = 0; j < suiteSize; j++) {
			Integer value = suiteRecords.getSuiteValue(j, index);
			if (value != null) {
				double decodedValue = HistoSet.decodeVaultValue(trailRecord.getChannelItem(), value / 1000.0);
				pointArray.setY(j, y0 - (int) ((decodedValue - yOffset) * graphicsData.getDisplayScaleFactorValue()));
			}
		}
		log.finer(() -> pointArray.toString());
		return pointArray;
	}

	/**
	 * Query the values for display.
	 * @param timeLine
	 * @return point time, value; null if the trail record value is null
	 */
	public static Point[] getDisplayPoints(GraphicsLayout graphicsData, HistoTimeLine timeLine) {
		TrailRecord trailRecord = graphicsData.getTrailRecord();
		int x0 = timeLine.getCurveAreaBounds().x;
		int y0 = timeLine.getCurveAreaBounds().height + timeLine.getCurveAreaBounds().y;

		double yOffset = getYOffset(graphicsData);
		Point[] points = new Point[trailRecord.realSize()];
		for (int i = 0; i < trailRecord.realSize(); i++) {
			Integer value = trailRecord.elementAt(i);
			if (value != null) {
				double decodedValue = HistoSet.decodeVaultValue(trailRecord.getChannelItem(), value / 1000.0);
				points[i] = new Point(x0 + timeLine.getScalePositions().get((long) trailRecord.getParent().getTime_ms(i)),
						y0 - (int) ((decodedValue - yOffset) * graphicsData.getDisplayScaleFactorValue()));
			}
		}
		log.finer(() -> Arrays.toString(points));
		return points;
	}

	/**
	 * @param index
	 * @return yPos in pixel (with top@0 and bottom@drawAreaBounds.height) or Integer.MIN_VALUE if the index value is null
	 */
	public static int getVerticalDisplayPos(GraphicsLayout graphicsData, int height, int index) {
		int verticalDisplayPos = Integer.MIN_VALUE;
		TrailRecord record = graphicsData.getTrailRecord();
		Integer value = record.getPoints().elementAt(index);
		if (value != null) {
			double decodedValue = HistoSet.decodeVaultValue(record.getChannelItem(), value / 1000.0);
			verticalDisplayPos = height - (int) ((decodedValue - getYOffset(graphicsData)) * graphicsData.getDisplayScaleFactorValue());
		}
		return verticalDisplayPos;
	}

	public static int getVerticalDisplayPos(GraphicsLayout graphicsData, int height, double decodedValue) {
		int verticalDisplayPos;
		verticalDisplayPos = height - (int) ((decodedValue - getYOffset(graphicsData)) * graphicsData.getDisplayScaleFactorValue());
		log.finer(() -> String.format("translatedValue=%f yPos=%d", decodedValue, verticalDisplayPos)); //$NON-NLS-1$
		return verticalDisplayPos;
	}

	private static double getYOffset(GraphicsLayout graphicsData) {
		return graphicsData.getMinDisplayValue() * 1 / graphicsData.getSyncMasterFactor();
	}

}
