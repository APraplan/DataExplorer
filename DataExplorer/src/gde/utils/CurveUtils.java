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

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import gde.GDE;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * This class contains utilities to draw curves and vertical scales
 * @author Winfried Brügmann
 */
public class CurveUtils {
	private static Logger			log								= Logger.getLogger(CurveUtils.class.getName());

	/**
	 * draws the data graph scale using gives rectangle for display
	 * all data point are multiplied with factor 1000 to avoid rounding errors for values below 1.0 (0.5 -> 0)
	 * @param record
	 * @param gc
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 * @param scaleWidthSpace
	 * @param isDrawScalesInRecordColor
	 * @param isDrawNameInRecordColor
	 * @param isDrawNumbersInRecordColor
	 */
	public static void drawScale(Record record, GC gc, int x0, int y0, int width, int height, int scaleWidthSpace, boolean isDrawScaleInRecordColor, boolean isDrawNameInRecordColor, boolean isDrawNumbersInRecordColor, boolean isDraw10TicksPerRecord) {
		final IDevice device = record.getDevice(); // defines the link to a device where values may corrected
		RecordSet parent = (record.getParent());
		final boolean isCompareSet = parent.isCompareSet();
		int numberTicks = 10, miniticks = 5;
		if (isDraw10TicksPerRecord)
			record.setRoundOut(true);

		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "x0=" + x0 + " y0=" + y0 + " width=" + width + " height=" + height + " horizontalSpace=" + scaleWidthSpace); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		if (record.isEmpty() && !record.isDisplayable() && !record.isScaleVisible()) return; // nothing to display
		String recordName = isCompareSet ? record.getKeyName() : record.getName();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "drawing record =" + recordName + " isCompareSet = " + isCompareSet); //$NON-NLS-1$ //$NON-NLS-2$

		//Draw the curve
		//(yMaxValue - yMinValue) defines the area to be used for the curve
		double yMaxValue = (record.isScaleSynced() ? record.getSyncMaxValue() : record.getMaxValue()) / 1000.0;
		double yMinValue = (record.isScaleSynced() ? record.getSyncMinValue() : record.getMinValue()) / 1000.0;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "unmodified yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$

		// yMinValueDisplay and yMaxValueDisplay used for scales and adapted values device and measure unit dependent
		double yMinValueDisplay = yMinValue, yMaxValueDisplay = yMaxValue;
		boolean isRaw = parent.isRaw();

		if ( Math.abs(yMaxValue - yMinValue) < .0001 && !isRaw) {
			yMinValueDisplay = yMinValue = Double.valueOf(yMinValue - 1).intValue();
			yMaxValueDisplay = yMaxValue = Double.valueOf(yMaxValue + 1).intValue();
		}
		if (record.isStartEndDefined()) {
			yMinValueDisplay = record.getMinScaleValue();
			yMaxValueDisplay = record.getMaxScaleValue();
			if (isRaw) {
				yMinValue = device.reverseTranslateValue(record, yMinValueDisplay);
				yMaxValue = device.reverseTranslateValue(record, yMaxValueDisplay);
			}
			else {
				yMinValue = yMinValueDisplay;
				yMaxValue = yMaxValueDisplay;
			}

			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "defined yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			//exclude imported data where values don't need correction
			if (device != null && isRaw) { // adapt to device specific range
				yMinValueDisplay = device.translateValue(record, yMinValue);
				yMaxValueDisplay = device.translateValue(record, yMaxValue);
			}

			if (device != null && (record.isRoundOut() || Math.abs(yMaxValue - yMinValue) < .0001)) { // equal value disturbs the scaling algorithm
				double deltaValueDisplay = yMaxValueDisplay - yMinValueDisplay;
				yMaxValueDisplay = MathUtils.roundUp(yMaxValueDisplay, deltaValueDisplay); // max
				yMinValueDisplay = MathUtils.roundDown(yMinValueDisplay, deltaValueDisplay); // min
				Object[] roundResult = MathUtils.adaptRounding(yMinValueDisplay, yMaxValueDisplay, false, isDraw10TicksPerRecord ? 10 : height / 25 >= 3 ? height / 25 : 2);
				yMinValueDisplay = (Double)roundResult[0];
				yMaxValueDisplay = (Double)roundResult[1];
				numberTicks = isDraw10TicksPerRecord ? 10 : (Integer)roundResult[2];
				miniticks = (Integer)roundResult[3];
				if (isRaw) {
					yMinValue = device.reverseTranslateValue(record, yMinValueDisplay);
					yMaxValue = device.reverseTranslateValue(record, yMaxValueDisplay);
				}
				else {
					yMinValue = yMinValueDisplay;
					yMaxValue = yMaxValueDisplay;
				}

				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("rounded yMinValue = %5.3f - yMaxValue = %5.3f", yMinValue, yMaxValue)); //$NON-NLS-1$
			}
			if (record.isStartpointZero()) {
				// check if the main part of the curve is on positive side
				if (record.getAvgValue() >= 0) { // main part of curve is on positive side
					yMinValueDisplay = 0;
					if (isRaw) {
						yMinValue = yMinValueDisplay - record.getOffset();
					}
					else {
						yMinValue = yMinValueDisplay;
					}
				}
				else {// main part of curve is on negative side
					yMaxValueDisplay = 0;
					if (isRaw) {
						yMaxValue = yMaxValueDisplay - record.getOffset();
					}
					else {
						yMaxValue = yMaxValueDisplay;
					}
				}
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "scale starts at 0; yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		record.setMinScaleValue(yMinValueDisplay);
		record.setMaxScaleValue(yMaxValueDisplay);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "scale  -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
		String graphText = (record.isScaleSyncMaster() ? record.getSyncMasterName()	: recordName);
		if (record.getSymbol() != null && record.getSymbol().length() > 0) graphText = graphText + "   " + record.getSymbol();
		if (record.getUnit() != null && record.getUnit().length() > 0) graphText = graphText + "   [" + record.getUnit() + "]"; //$NON-NLS-1$ //$NON-NLS-2$

		// adapt number space calculation to real displayed max number
		//Point pt = gc.textExtent(df.format(yMaxValueDisplay));
		//if (log.isLoggable(Level.FINE)) log.log(Level.FINE, df.format(yMaxValueDisplay) + " gc.textExtent = " + pt.toString());
		Point pt = gc.textExtent("000,00"); //$NON-NLS-1$
		int ticklength = 5;
		int gap = 10;

		// prepare axis position
		gc.setLineWidth(2);
		gc.setLineStyle(SWT.LINE_SOLID);
		boolean isPositionLeft = record.isPositionLeft();
		int positionNumber = isCompareSet ? 0 : parent.getAxisPosition(recordName, isPositionLeft);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordName + " positionNumber = " + positionNumber); //$NON-NLS-1$
		if (isDrawScaleInRecordColor) {
			String rgb = record.isScaleSyncMaster() ? record.getSyncMasterRGB() : record.getRGB();
			gc.setForeground(SWTResourceManager.getColor(rgb)); // draw the main scale line in same color as the curve
		}	else gc.setForeground(DataExplorer.getInstance().COLOR_BLACK);
		if (isPositionLeft) {
			int xPos = x0 - 1 - positionNumber * scaleWidthSpace;
			gc.drawLine(xPos, y0+1, xPos, y0-height-1); //xPos = x0
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "y-Achse = " + xPos + ", " + y0 + ", " + xPos + ", " + (y0 - height)); //yMax //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			GraphicsUtils.drawVerticalTickMarks(record, gc, xPos, y0, height, yMinValueDisplay, yMaxValueDisplay, ticklength, miniticks, gap, isPositionLeft, numberTicks, isDrawNumbersInRecordColor);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "drawText x = " + (xPos - pt.y - 15)); //xPosition Text Spannung [] //$NON-NLS-1$
			if (!isCompareSet) {
				if (isDrawNameInRecordColor) gc.setForeground(SWTResourceManager.getColor(record.getRGB()));
				else {
					DataExplorer.getInstance();
					gc.setForeground(DataExplorer.getInstance().COLOR_BLACK);
				}
				GraphicsUtils.drawTextCentered(graphText, (xPos - scaleWidthSpace + 3), y0 / 2 + (y0 - height), gc, SWT.UP);
			}
		}
		else {
			int xPos = x0 + 1 + width + positionNumber * scaleWidthSpace;
			gc.drawLine(xPos, y0+1, xPos, y0-height-1); //yMax
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "y-Achse = " + xPos + ", " + y0 + ", " + xPos + ", " + (y0 - height)); //yMax //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			GraphicsUtils.drawVerticalTickMarks(record, gc, xPos, y0, height, yMinValueDisplay, yMaxValueDisplay, ticklength, miniticks, gap, isPositionLeft, numberTicks, isDrawNumbersInRecordColor);
			if (!isCompareSet) {
				if (isDrawNameInRecordColor) gc.setForeground(SWTResourceManager.getColor(record.getRGB()));
				else {
					DataExplorer.getInstance();
					gc.setForeground(DataExplorer.getInstance().COLOR_BLACK);
				}
				GraphicsUtils.drawTextCentered(graphText, (xPos + scaleWidthSpace - pt.y - 5), y0 / 2 + (y0 - height), gc, SWT.UP);
			}
		}

		// set the values corresponding to the display area of this curve
		record.setSyncedMinMaxDisplayValues(yMinValue, yMaxValue);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " data limit  -> yMinValue = " + yMinValue + "; yMaxValue = " + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * method draw the curve using the given graphics context (GC)
	 * @param record
	 * @param gc
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 * @param isCompareSet
	 */
	public static void drawCurve(Record record, GC gc, int x0, int y0, int width, int height, boolean isCompareSet) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName()+ String.format(" x0 = %d, y0 = %d, width = %d, height = %d", x0, y0, width, height)); //$NON-NLS-1$
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "curve area bounds = " + record.getParent().getDrawAreaBounds().toString()); //$NON-NLS-1$

		RecordSet parent = (record.getParent());

		// set line properties according adjustment
		gc.setForeground(SWTResourceManager.getColor(record.getRGB()));
		gc.setLineWidth(record.getLineWidth());
		gc.setLineStyle(record.getLineStyle());

		// get the number of data points size to be drawn
		int displayableSize = record.size();

		// calculate time line adaption if record set is compare set, compare set max have different times for each record, (intRecordSize - 1) is number of time deltas for calculation
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "average record time step msec = " + record.getAverageTimeStep_ms()); //$NON-NLS-1$
		double displayableTime_ms = record.getDrawTimeWidth_ms();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "displayableSize = " + displayableSize + " displayableTime_ms = " + displayableTime_ms); //$NON-NLS-1$ //$NON-NLS-2$

		// calculate xScale for curves with much to many data points, it makes no sense to draw all the small lines on the same part of the screen
		int xScaleFactor = (int)(displayableSize / (width * 2.2));
		xScaleFactor = xScaleFactor > 0 ? xScaleFactor : 1; //check for curves with less points than draw area width
		while (displayableSize % xScaleFactor > 3 && xScaleFactor > 1) {
			--xScaleFactor;
		}
		//xScaleFactor+=2;
		// calculate scale factor to fit time into draw bounds display pixel based
		double xTimeFactor = width / displayableTime_ms; // * (xScaleFactor - 0.44);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "xTimeFactor = " + xTimeFactor + " xScaleFactor = " + xScaleFactor + " : " + (xTimeFactor * xScaleFactor)); //$NON-NLS-1$ //$NON-NLS-2$
		record.setDisplayScaleFactorTime(xTimeFactor);
		record.setDisplayScaleFactorValue(height);

		StringBuffer sb = new StringBuffer(); // logging purpose
		Point newPoint, oldPoint = new Point(0, 0);

		try {
			//calculate start point of the curve, which is the first oldPoint
			//draw the first point with possible interpolated values if it does not match a measurement point at time value
			//oldPoint = record.getParent().isScopeMode() ? record.getDisplayPoint(0, x0, y0) : record.getDisplayEndPoint(0, x0);
			oldPoint = record.getDisplayEndPoint(0);
			if (log.isLoggable(Level.FINEST)) sb.append(GDE.LINE_SEPARATOR).append(oldPoint.toString());
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage() + " zoomed compare set ?", e); //$NON-NLS-1$
		}

		try {
			// draw scaled points to draw area - measurements can only be drawn starting with the first measurement point
			if (parent.isCompareSet()) {// compare set might contain records with different size
				boolean isGPSCoordinate = record.getDevice().isGPSCoordinates(record);
				//drawLimit = drawLimit / xScale;
				int drawLimit = record.findBestIndex(record.getCompareSetDrawLimit_ms()) - record.findBestIndex(record.getZoomTimeOffset());
				int j = 0;
				for (; j < displayableSize && displayableSize > 1; j += xScaleFactor) {
					// get the point to be drawn
					if (isGPSCoordinate)
						newPoint = record.getGPSDisplayPoint(j, x0, y0);
					else
						newPoint = record.getDisplayPoint(j, x0, y0);
					if (log.isLoggable(Level.FINEST)) sb.append(GDE.LINE_SEPARATOR).append(newPoint.toString());
					if (j <= drawLimit) {
						gc.drawLine(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y);
					}
					oldPoint = newPoint; //remember the last draw point for next drawLine operation, if point above, do nothing
				}
				if (j <= drawLimit) {
					//draw the last point with possible interpolated values if it does not match a measurement point at time value
					newPoint = record.getDisplayEndPoint(width);
					gc.drawLine(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y);
				}
			}
			else if (record.getDevice().isGPSCoordinates(record)) {
				//int tmpDelta = 0;
				for (int j = 0; j <= displayableSize && displayableSize > 1; j += xScaleFactor) {
					// get the point to be drawn
					newPoint = record.getGPSDisplayPoint(j, x0, y0);
					if (log.isLoggable(Level.FINEST)) sb.append(GDE.LINE_SEPARATOR).append(newPoint.toString());
					gc.drawLine(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y);
					oldPoint = newPoint; // remember the last draw point for next drawLine operation
				}
				//draw the last point with possible interpolated values if it does not match a measurement point at time value
				newPoint = record.getDisplayEndPoint(width);
				gc.drawLine(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y);
			}
			else {
				for (int j = 0; j <= displayableSize && displayableSize > 1; j += xScaleFactor) {
					// get the point to be drawn
					newPoint = record.getDisplayPoint(j, x0, y0);
					if (log.isLoggable(Level.FINEST)) sb.append(GDE.LINE_SEPARATOR).append(newPoint.toString());
					gc.drawLine(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y);
					oldPoint = newPoint; // remember the last draw point for next drawLine operation
				}
				//draw the last point with possible interpolated values if it does not match a measurement point at time value
				newPoint = record.getDisplayEndPoint(width);
				gc.drawLine(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y);
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage() + " zoomed compare set ?", e); //$NON-NLS-1$
		}
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, sb.toString());
	}
}
