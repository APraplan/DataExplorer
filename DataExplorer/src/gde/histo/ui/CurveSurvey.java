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

import static gde.histo.utils.ElementaryQuantile.BoxplotItems.LOWER_WHISKER;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE0;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE1;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE2;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE3;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE4;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.UPPER_WHISKER;
import static java.util.logging.Level.FINEST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import gde.config.Settings;
import gde.histo.recordings.HistoGraphicsMapper;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSection;
import gde.histo.ui.GraphicsComposite.GraphicsLayout;
import gde.histo.utils.HistoTimeLine;
import gde.histo.utils.SingleResponseRegression;
import gde.histo.utils.SingleResponseRegression.RegressionType;
import gde.histo.utils.Spot;
import gde.log.Logger;
import gde.ui.SWTResourceManager;
import gde.utils.ColorUtils;

/**
 * Curve measuring for the graphics window.
 * Supports linear regression and boxplot for delta measurements.
 * @author Thomas Eickert
 */
public final class CurveSurvey {
	private final static String	$CLASS_NAME	= CurveSurvey.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Maximum cursor distance from an x axis position for snapping the timeline position (value is a number of pixels).
	 */
	private static final int X_TOLERANCE = 1;

	public enum LineMark {
		MEASURE_CROSS(1, SWT.COLOR_DARK_GREEN, SWT.LINE_DASH, null), //
		DELTA_CROSS(1, SWT.COLOR_BLUE, SWT.LINE_DASH, null), //
		DIAG_LINE(1, SWT.COLOR_DARK_YELLOW, SWT.LINE_DOT, new int[] { 5, 2 }), //
		AVG_LINE(2, null, SWT.LINE_DASH, null), //
		PARABOLA_LINE(2, null, SWT.LINE_DOT, null), //
		SLOPE_LINE(2, null, SWT.LINE_SOLID, null), //
		BOXPLOT(2, null, SWT.LINE_SOLID, null);

		private final int			lineWidth;
		private final Integer	lineColor;
		private final int			lineStyle;
		private final int[]		lineDash;

		private LineMark(int lineWidth, Integer lineColor, int lineStyle, int[] lineDash) {
			this.lineWidth = lineWidth;
			this.lineColor = lineColor;
			this.lineStyle = lineStyle;
			this.lineDash = lineDash;
		}
	}

	/**
	 * Painting the survey area lines and boxplots.
	 * Includes erasing the existing survey objects.
	 * @author Thomas Eickert (USER)
	 */
	private final class LinePainter {

		private final int	offSetX, offSetY;								// curveAreaBounds x / y for performance
		private final int	width, height;									// for performance

		// survey area axis position borders
		private int				yLowerLimit	= Integer.MIN_VALUE;
		private int				yUpperLimit	= Integer.MAX_VALUE;
		private int				xLeftLimit	= Integer.MAX_VALUE;
		private int				xRightLimit	= Integer.MIN_VALUE;

		public LinePainter(Rectangle curveAreaBounds) {
			super();
			this.offSetX = curveAreaBounds.x;
			this.offSetY = curveAreaBounds.y;
			this.width = curveAreaBounds.width;
			this.height = curveAreaBounds.height;

			// support lines with start or end beyond the y axis drawing area
			CurveSurvey.this.canvasGC.setClipping(curveAreaBounds);
		}

		/**
		 * Draw horizontal line as defined relative to curve draw area, where there is an offset from left and an offset from top
		 * for performance reason specify line width, line style and line color outside
		 * @param posFromLeft
		 * @param posFromTop
		 * @param length
		 */
		private void drawVerticalLine(int posFromLeft, int posFromTop, int length, LineMark lineMark) {
			setLineMark(lineMark);
			CurveSurvey.this.canvasGC.drawLine(posFromLeft + this.offSetX, posFromTop + this.offSetY, posFromLeft + this.offSetX, posFromTop + this.offSetY + length - 1);
		}

		/**
		 * Draw vertical line as defined relative to curve draw area, where there is an offset from left and an offset from top
		 * for performance reason specify line width, line style and line color outside
		 * @param posFromTop
		 * @param posFromLeft
		 * @param length
		 */
		private void drawHorizontalLine(int posFromTop, int posFromLeft, int length, LineMark lineMark) {
			log.finer(() -> String.format("posFromLeft=%d posFromTop=%d length=%d", posFromLeft, posFromTop, length));
			setLineMark(lineMark);
			CurveSurvey.this.canvasGC.drawLine(posFromLeft + this.offSetX, posFromTop + this.offSetY, posFromLeft + this.offSetX + length - 1, posFromTop + this.offSetY);
		}

		/**
		 * Draw line as defined relative to curve draw area, where there is an offset from left and an offset from top.
		 * Expands the lines drawing area if this line goes beyond the current area.
		 * @param posFromTop1
		 * @param posFromLeft1
		 * @param posFromTop2
		 * @param posFromLeft2
		 */
		private void drawConnectingLine(int posFromLeft1, int posFromTop1, int posFromLeft2, int posFromTop2, LineMark lineMark) {
			log.finer(() -> "posFromLeft1=" + posFromLeft1 + " posFromTop1=" + posFromTop1 + " posFromLeft2=" + posFromLeft2 + " posFromTop2=" + posFromTop2);
			if (posFromLeft1 != posFromLeft2 || posFromTop1 != posFromTop2) {
				setLineMark(lineMark);
				CurveSurvey.this.canvasGC.drawLine(posFromLeft1 + this.offSetX, posFromTop1 + this.offSetY, posFromLeft2 + this.offSetX, posFromTop2 + this.offSetY);
			}
		}

		/**
		 * Draw outlier circle as defined relative to curve draw area, where there is an offset from left and an offset from top.
		 * Expand the lines drawing area if this line goes beyond the current area.
		 * @param posFromTop1
		 * @param posFromLeft1
		 * @param radius
		 * @param lineMark
		 */
		private void drawOutlier(int posFromLeft1, int posFromTop1, int radius, LineMark lineMark) {
			log.finer(() -> "posFromLeft1=" + posFromLeft1 + " posFromTop1=" + posFromTop1);
			log.finer(() -> "yUpperLimit=" + this.yUpperLimit + " yLowerLimit=" + this.yLowerLimit);

			setLineMark(lineMark);

			CurveSurvey.this.canvasGC.drawOval(posFromLeft1 - radius + this.offSetX, posFromTop1 - radius + this.offSetY, radius * 2, radius * 2);
		}

		public void drawAverageLine(int posFromLeft1, int posFromTop1, int length) {
			this.drawHorizontalLine(posFromLeft1, posFromTop1, length, LineMark.AVG_LINE);
		}

		public void drawLinearRegressionLine(int posFromLeft1, int posFromTop1, int posFromLeft2, int posFromTop2) {
			this.drawConnectingLine(posFromLeft1, posFromTop1, posFromLeft2, posFromTop2, LineMark.SLOPE_LINE);

			this.yLowerLimit = Math.max(this.yLowerLimit, Math.max(posFromTop1, posFromTop2));
			this.yUpperLimit = Math.min(this.yUpperLimit, Math.min(posFromTop1, posFromTop2));
			this.xLeftLimit = Math.min(this.xLeftLimit, Math.min(posFromLeft1, posFromLeft2));
			this.xRightLimit = Math.max(this.xRightLimit, Math.max(posFromLeft1, posFromLeft2));
			log.finer(() -> "xLeftLimit=" + this.xLeftLimit + " xRightLimit=" + this.xRightLimit + "yUpperLimit=" + this.yUpperLimit + " yLowerLimit=" + this.yLowerLimit);
		}

		/**
		 * Draw a parabola based on bounded trailrecord values as defined relative to curve draw area.
		 * Note the offsets from left and from top.
		 * @param points is a list of the display points approximated by the parabola (one point per log timestamp)
		 */
		public void drawRegressionParabolaLine(List<Spot<Integer>> points) {
			log.finer(() -> "xPos0=" + points.get(0).x() + " xPosN=" + points.get(points.size() - 1).x() + " yPos0=" + points.get(0).y() + " yPosN=" + points.get(points.size() - 1).y());

			// determine the display points for all x axis pixels within the bounded survey range
			SingleResponseRegression<Integer> singleResponseRegression = new SingleResponseRegression<>(points, RegressionType.QUADRATIC);

			int xPosStart = Math.min(points.get(0).x(), points.get(points.size() - 1).x());
			int xPosEnd = Math.max(points.get(0).x(), points.get(points.size() - 1).x());
			int[] pointArray;
			if (singleResponseRegression.getGamma() == 0.) {
				int yPosStart = (int) (singleResponseRegression.getResponse(xPosStart) + .5);
				int yPosEnd = (int) (singleResponseRegression.getResponse(xPosEnd) + .5);
				drawConnectingLine(xPosStart, yPosStart, xPosEnd, yPosEnd, LineMark.PARABOLA_LINE);
			} else {
				pointArray = new int[(xPosEnd - xPosStart) * 2];
				for (int i = 0; i < xPosEnd - xPosStart; i++) {
					pointArray[i * 2] = this.offSetX + xPosStart + i;
					pointArray[i * 2 + 1] = this.offSetY + (int) (singleResponseRegression.getResponse(xPosStart + i) + .5);
				}
				setLineMark(LineMark.PARABOLA_LINE);
				CurveSurvey.this.canvasGC.drawPolyline(pointArray);
			}

			{ // calculate the extremum values
				int yLowerLimitTmp;
				int yUpperLimitTmp;
				{ // calculate the display parabola's left and right value
					int leftExtremum = (int) singleResponseRegression.getResponse(points.get(0).x());
					int rightExtremum = (int) singleResponseRegression.getResponse(points.get(points.size() - 1).x());
					yLowerLimitTmp = Math.max(this.yLowerLimit, Math.max(leftExtremum, rightExtremum));
					yUpperLimitTmp = Math.min(this.yUpperLimit, Math.min(leftExtremum, rightExtremum));
					// check the parabola extremum
					if (singleResponseRegression.getGamma() != 0.) {
						Double xPosExtremum = singleResponseRegression.getParabolaExtremum();
						double mid = (points.get(0).x() + points.get(points.size() - 1).x()) / 2.;
						if (Math.abs(xPosExtremum - mid) <= (Math.abs(points.get(0).x() - mid))) { // extremum is between
							int absoluteExtremum = (int) singleResponseRegression.getResponse(xPosExtremum);
							yLowerLimitTmp = Math.max(this.yLowerLimit, absoluteExtremum);
							yUpperLimitTmp = Math.min(this.yUpperLimit, absoluteExtremum);
						}
					}
				}
				// add 3 pixels to compensate truncation errors
				this.yLowerLimit = Math.min(this.height, yLowerLimitTmp + 3);
				this.yUpperLimit = Math.max(0, yUpperLimitTmp - 3);
			}
			log.finer(() -> "yUpperLimit=" + this.yUpperLimit + " yLowerLimit=" + this.yLowerLimit);
		}

		public void drawCross(int posFromLeft1, int posFromTop1) {
			drawVerticalLine(posFromLeft1, 0, this.height, LineMark.MEASURE_CROSS);
			drawHorizontalLine(posFromTop1, 0, this.width, LineMark.MEASURE_CROSS);
		}

		public void drawDoubleCross(int posFromLeft1, int posFromTop1, int posFromLeft2, int posFromTop2) {
			drawVerticalLine(posFromLeft1, 0, this.height, LineMark.MEASURE_CROSS);
			drawHorizontalLine(posFromTop1, 0, this.width, LineMark.MEASURE_CROSS);

			drawVerticalLine(posFromLeft2, 0, this.height, LineMark.DELTA_CROSS);
			drawHorizontalLine(posFromTop2, 0, this.width, LineMark.DELTA_CROSS);

			this.drawConnectingLine(posFromLeft1, posFromTop1, posFromLeft2, posFromTop2, LineMark.DIAG_LINE);

			this.yLowerLimit = Math.max(this.yLowerLimit, Math.max(posFromTop1, posFromTop2));
			this.yUpperLimit = Math.min(this.yUpperLimit, Math.min(posFromTop1, posFromTop2));
			this.xLeftLimit = Math.min(this.xLeftLimit, Math.min(posFromLeft1, posFromLeft2));
			this.xRightLimit = Math.max(this.xRightLimit, Math.max(posFromLeft1, posFromLeft2));
			log.finer(() -> "xLeftLimit=" + this.xLeftLimit + " xRightLimit=" + this.xRightLimit + "yUpperLimit=" + this.yUpperLimit + " yLowerLimit=" + this.yLowerLimit);
		}

		public void drawBoxplot(int xPos, int[] yPosBoxplot) {
			int halfBoxWidth = getBoxWidth() / 2;
			int xPosLeft = xPos - halfBoxWidth;
			int xPosRight = xPos + halfBoxWidth;
			int radius = halfBoxWidth / 4;
			{
				// x axis limits are defined by the boxplot frame
				this.xLeftLimit = Math.min(xPosLeft, this.xLeftLimit);
				this.xRightLimit = Math.max(xPosRight, this.xRightLimit);

				int yPosQuartile1 = yPosBoxplot[QUARTILE1.ordinal()];
				drawHorizontalLine(yPosQuartile1, xPosLeft, halfBoxWidth * 2, LineMark.BOXPLOT);
				drawHorizontalLine(yPosBoxplot[QUARTILE2.ordinal()], xPosLeft, halfBoxWidth * 2, LineMark.BOXPLOT);
				int yPosQuartile3 = yPosBoxplot[QUARTILE3.ordinal()];
				drawHorizontalLine(yPosQuartile3, xPosLeft, halfBoxWidth * 2, LineMark.BOXPLOT);
				drawVerticalLine(xPosLeft, yPosQuartile3, yPosQuartile1 - yPosQuartile3, LineMark.BOXPLOT);
				drawVerticalLine(xPosRight, yPosQuartile3, yPosQuartile1 - yPosQuartile3, LineMark.BOXPLOT);

				int yPosLowerWhisker = yPosBoxplot[LOWER_WHISKER.ordinal()];
				drawHorizontalLine(yPosLowerWhisker, xPos - halfBoxWidth / 2, halfBoxWidth, LineMark.BOXPLOT);
				drawVerticalLine(xPos, yPosQuartile1, yPosLowerWhisker - yPosQuartile1, LineMark.BOXPLOT);

				int yPosUpperWhisker = yPosBoxplot[UPPER_WHISKER.ordinal()];
				drawHorizontalLine(yPosUpperWhisker, xPos - halfBoxWidth / 2, halfBoxWidth, LineMark.BOXPLOT);
				drawVerticalLine(xPos, yPosUpperWhisker, yPosQuartile3 - yPosUpperWhisker, LineMark.BOXPLOT);

				log.finer(() -> String.format("xPos=%d  LW=%d Q1=%d Q2=%d Q3=%d UW=%d ", xPos, yPosLowerWhisker, yPosQuartile1, yPosBoxplot[QUARTILE2.ordinal()], yPosQuartile3, yPosUpperWhisker));
			}
			{
				int yPosBottom = yPosBoxplot[QUARTILE0.ordinal()];
				if (yPosBottom != yPosBoxplot[LOWER_WHISKER.ordinal()]) drawOutlier(xPos, yPosBottom, radius, LineMark.BOXPLOT);
				int yPosTop = yPosBoxplot[QUARTILE4.ordinal()];
				if (yPosTop != yPosBoxplot[UPPER_WHISKER.ordinal()]) drawOutlier(xPos, yPosTop, radius, LineMark.BOXPLOT);

				// y axis limits may cross the top or bottom defined by the curve values
				this.yLowerLimit = Math.max(this.yLowerLimit, yPosBottom + radius);
				this.yUpperLimit = Math.min(this.yUpperLimit, yPosTop - radius);
			}
			log.finer(() -> "xLeftLimit=" + this.xLeftLimit + " xRightLimit=" + this.xRightLimit + " yUpperLimit=" + this.yUpperLimit + " yLowerLimit=" + this.yLowerLimit);
		}

		/**
		 * Set the line properties for the next line draw operations.
		 * @param lineMark
		 * @param defaultColor in case of a null color in the lineMark
		 */
		private void setLineMark(LineMark lineMark) {
			CurveSurvey.this.canvasGC.setLineWidth(lineMark.lineWidth);
			CurveSurvey.this.canvasGC.setLineDash(lineMark.lineDash);
			CurveSurvey.this.canvasGC.setLineStyle(lineMark.lineStyle);
			if (lineMark.lineColor == null)
				CurveSurvey.this.canvasGC.setForeground(ColorUtils.getColor(CurveSurvey.this.trailRecord.getRGB()));
			else
				CurveSurvey.this.canvasGC.setForeground(SWTResourceManager.getColor(lineMark.lineColor));
		}

		private int getBoxWidth() {
			int boxWidth = 15;
			if (Math.abs(GraphicsMeasuring.xPosDelta - GraphicsMeasuring.xPosMeasure) > boxWidth) {
				boxWidth += ((int) Math.log(Math.abs(GraphicsMeasuring.xPosDelta - GraphicsMeasuring.xPosMeasure))) * 4 - 10;
				log.log(FINEST, "boxwidth=", boxWidth);
			}
			return boxWidth + boxWidth * (Settings.getInstance().getBoxplotScaleOrdinal() - 1) / 2;
		}

		@Override
		public String toString() {
			return "LinePainter [offSetX=" + this.offSetX + ", offSetY=" + this.offSetY + ", width=" + this.width + ", height=" + this.height + ", yLowerLimit=" + this.yLowerLimit + ", yUpperLimit=" + this.yUpperLimit + ", xLeftLimit=" + this.xLeftLimit + ", xRightLimit=" + this.xRightLimit + ", getBoxWidth()=" + this.getBoxWidth() + "]";
		}

	}

	private final Settings					settings		= Settings.getInstance();

	private final GraphicsComposite	graphicsComposite;
	private final HistoTimeLine			timeLine;
	private final TrailRecord				trailRecord;
	private final Measure						measure;

	private LinePainter							linePainter;
	private GC											canvasGC;

	public CurveSurvey(GraphicsComposite graphicsComposite, Measure measure) {
		this.trailRecord = measure.measureRecord;
		this.graphicsComposite = graphicsComposite;
		this.timeLine = graphicsComposite.getTimeLine();
		this.measure = measure;
	}

	public void initialize() {
		GraphicsMeasuring.xPosMeasure = Integer.MIN_VALUE;
		GraphicsMeasuring.yPosMeasure = Integer.MIN_VALUE;
		GraphicsMeasuring.xPosDelta = Integer.MIN_VALUE;
		GraphicsMeasuring.yPosDelta = Integer.MIN_VALUE;
	}

	/**
	 * @return the message text for the status message
	 */
	public String drawMeasurementGraphics() {
		String statusMessage;

		linePainter = new LinePainter(timeLine.getCurveAreaBounds());
		this.linePainter.drawCross(GraphicsMeasuring.xPosMeasure, GraphicsMeasuring.yPosMeasure);

		if (GraphicsMeasuring.yPosMeasure >= Integer.MIN_VALUE) {
			statusMessage = measure.getMeasureStatusMessage();
		} else {
			statusMessage = measure.getNoRecordsStatusMessage();
		}
		return statusMessage;
	}

	/**
	 * @return the message text for the status message
	 */
	public String drawDeltaMeasurementGraphics() {
		String statusMessage;

		linePainter = new LinePainter(timeLine.getCurveAreaBounds());
		this.linePainter.drawDoubleCross(GraphicsMeasuring.xPosMeasure, GraphicsMeasuring.yPosMeasure, GraphicsMeasuring.xPosDelta, GraphicsMeasuring.yPosDelta);

		if (GraphicsMeasuring.yPosMeasure != Integer.MIN_VALUE || GraphicsMeasuring.yPosDelta != Integer.MIN_VALUE) {
			if (this.settings.isCurveSurvey()) {
				drawCurveSurvey();
				statusMessage = measure.getCurveSurveyStatusMessage();
			} else {
				statusMessage = measure.getDeltaStandardStatusMessage();
			}
		} else {
			if (measure.getRecordSection().isValidBounds() && this.settings.isCurveSurvey()) {
				drawCurveSurvey();
				statusMessage = measure.getNoDeltaCurveSurveyStatusMessage();
			} else {
				statusMessage = measure.getNoRecordsStatusMessage();
			}
		}
		return statusMessage;
	}

	/**
	 * Draw regression lines and the boxplot element.
	 * @param trailRecord
	 */
	private void drawCurveSurvey() {
		int height = timeLine.getCurveAreaBounds().height;
		TrailRecordSection recordSection = measure.getRecordSection();

		GraphicsLayout graphicsData = graphicsComposite.getChartData(trailRecord);
		if (!recordSection.isBoundedParabola()) { // hide these curves for better overview whenever a parabola is shown
			int yBoundedAvg = HistoGraphicsMapper.getVerticalDisplayPos(graphicsData, height, recordSection.getBoundedAvgValue());
			this.linePainter.drawAverageLine(yBoundedAvg, GraphicsMeasuring.xPosMeasure, GraphicsMeasuring.xPosDelta - GraphicsMeasuring.xPosMeasure);

			int yRegressionPosMeasure = HistoGraphicsMapper.getVerticalDisplayPos(graphicsData, height, recordSection.getBoundedSlopeValue(measure.getTimestampMeasure_ms()));
			int yRegressionPosDelta = HistoGraphicsMapper.getVerticalDisplayPos(graphicsData, height, recordSection.getBoundedSlopeValue(measure.getTimestampDelta_ms()));
			this.linePainter.drawLinearRegressionLine(GraphicsMeasuring.xPosMeasure, yRegressionPosMeasure, GraphicsMeasuring.xPosDelta, yRegressionPosDelta);
		} else {
			List<Spot<Integer>> points = new ArrayList<>();
			for (Spot<Double> entry : recordSection.getBoundedParabolaValues()) {
				points.add(new Spot<Integer>(this.timeLine.getXPosTimestamp((long) entry.x().doubleValue()),
						HistoGraphicsMapper.getVerticalDisplayPos(graphicsData, height, entry.y())));
			}
			log.finer(() -> "values " + Arrays.toString(points.toArray()));
			this.linePainter.drawRegressionParabolaLine(points);
		}
		{
			int xPosMidBounds = (GraphicsMeasuring.xPosDelta + GraphicsMeasuring.xPosMeasure) / 2;
			double[] values = recordSection.getBoundedBoxplotValues();
			int[] yPosBoxplot = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				yPosBoxplot[i] = HistoGraphicsMapper.getVerticalDisplayPos(graphicsData, height, values[i]);
			}
			this.linePainter.drawBoxplot(xPosMidBounds, yPosBoxplot);
		}
	}

	/**
	 * Define the measure canvas positions.
	 */
	public void setMeasurePosition() {
		int height = timeLine.getCurveAreaBounds().height;
		GraphicsMeasuring.xPosMeasure = timeLine.getXPosTimestamp(measure.getTimestampMeasure_ms());
		GraphicsLayout graphicsData = graphicsComposite.getChartData(trailRecord);
		GraphicsMeasuring.yPosMeasure = HistoGraphicsMapper.getVerticalDisplayPos(graphicsData, height, trailRecord.getParent().getIndex(measure.getTimestampMeasure_ms()));
		log.finer(() -> String.format("timestampMeasure_ms=%d xPosMeasure=%d yPosMeasure=%d", measure.getTimestampMeasure_ms(), GraphicsMeasuring.xPosMeasure, GraphicsMeasuring.yPosMeasure));
	}

	/**
	 * Define the delta canvas positions.
	 */
	public void setDeltaPosition() {
		int height = timeLine.getCurveAreaBounds().height;
		GraphicsMeasuring.xPosDelta = timeLine.getXPosTimestamp(measure.getTimestampDelta_ms());
		GraphicsLayout graphicsData = graphicsComposite.getChartData(trailRecord);
		GraphicsMeasuring.yPosDelta = HistoGraphicsMapper.getVerticalDisplayPos(graphicsData, height, trailRecord.getParent().getIndex(measure.getTimestampDelta_ms()));
		log.fine(() -> String.format("timestampDelta_ms=%d xPosDelta=%d yPosDelta=%d", measure.getTimestampDelta_ms(), GraphicsMeasuring.xPosDelta, GraphicsMeasuring.yPosDelta));
	}

	/**
	 * @param xPos
	 * @return true if the x position is close to the vertical line
	 */
	public boolean isNearDeltaLine(int xPos) {
		return GraphicsMeasuring.xPosDelta + 1 >= xPos && GraphicsMeasuring.xPosDelta - 1 <= xPos;
	}

	/**
	 * @param xPos
	 * @return true if the x position is close to the vertical line
	 */
	public boolean isNearMeasureLine(int xPos) {
		return GraphicsMeasuring.xPosMeasure + X_TOLERANCE >= xPos && GraphicsMeasuring.xPosMeasure - X_TOLERANCE <= xPos;
	}

	/**
	 * @param xPos
	 * @return true if the x position is exactly over the measurement or delta vertical line
	 */
	public boolean isOverVerticalLine(int xPos) {
		return xPos > Integer.MIN_VALUE && GraphicsMeasuring.xPosMeasure > Integer.MIN_VALUE && isNearMeasureLine(xPos) //
				|| xPos > Integer.MIN_VALUE && GraphicsMeasuring.xPosDelta > Integer.MIN_VALUE && isNearDeltaLine(xPos);
	}

	public boolean isNewMeasureSpot(long timestampNew_ms, int yPosNew) {
		return yPosNew != Integer.MIN_VALUE && (timestampNew_ms != measure.getTimestampMeasure_ms() || yPosNew != GraphicsMeasuring.yPosMeasure);
	}

	public boolean isNewDeltaSpot(long timestampNew_ms, int yPosNew) {
		return yPosNew != Integer.MIN_VALUE && (timestampNew_ms != measure.getTimestampDelta_ms() || yPosNew != GraphicsMeasuring.yPosDelta);
	}

	public void setCanvasGC(GC canvasGC) {
		this.canvasGC = canvasGC;
	}

	@Override
	public String toString() {
		return "CurveSurvey [xPosMeasure=" + GraphicsMeasuring.xPosMeasure + ", yPosMeasure=" + GraphicsMeasuring.yPosMeasure + ", xPosDelta=" + GraphicsMeasuring.xPosDelta + ", yPosDelta=" + GraphicsMeasuring.yPosDelta + "]";
	}

}
