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

package gde.histo.ui.data;

import static gde.histo.utils.ElementaryQuantile.BoxplotItems.LOWER_WHISKER;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.UPPER_WHISKER;
import static java.util.logging.Level.FINEST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import gde.config.Settings;
import gde.histo.datasources.HistoSet;
import gde.histo.guard.Reminder;
import gde.histo.recordings.TrailRecord;
import gde.histo.ui.Measure;
import gde.histo.ui.SummaryComposite.SummaryLayout;
import gde.log.Logger;
import gde.ui.SWTResourceManager;

/**
 * Graph element data belonging to a record row in the summary graph.
 * xPos/yPos values define the lower left corner of a 2x2 pixel element.
 * @author Thomas Eickert (USER)
 */
public class SummarySpots { // MarkerLine + Boxplot + Warnings
	private static final String	$CLASS_NAME	= SummarySpots.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	public final static Color			COLOR_GREY												= SWTResourceManager.getColor(SWT.COLOR_GRAY);
	public final static Color			COLOR_CANVAS_YELLOW								= SWTResourceManager.getColor(250, 249, 211);
	public final static Color			COLOR_BLUE												= SWTResourceManager.getColor(SWT.COLOR_BLUE);
	public final static Color			COLOR_LIGHT_BLUE									= SWTResourceManager.getColor(239, 239, 255);
	public final static Color			COLOR_DARK_GREEN									= SWTResourceManager.getColor(SWT.COLOR_DARK_GREEN);
	public final static Color			COLOR_BLACK												= SWTResourceManager.getColor(SWT.COLOR_BLACK);
	public final static Color			COLOR_RED													= SWTResourceManager.getColor(SWT.COLOR_RED);

	public enum Density {
		EXTREME(2), HIGH(3), MEDIUM(4), LOW(5);

		private final Settings			settings					= Settings.getInstance();

		/**
		 * Number of pixels for comparison with the average pixel distance.
		 * Corresponds to the extreme density threshold at the medium boxplot scale setting.
		 */
		private final int						distanceThreshold	= 25;											// in pixel

		private final int						markerWidth;																// in pixel

		/** use this to avoid repeatedly cloning actions instead of values() */
		public static final Density	VALUES[]					= values();

		private Density(int markerWidth) {
			this.markerWidth = markerWidth;
		}

		public static Density fromOrdinal(int ordinal) {
			return Density.VALUES[ordinal];
		}

		public static String toString(Density density) {
			return density.name();
		}

		public static Density toDensity(int drawAreaWidth, int markerNumber) {
			Density density;
			int avgDistance = (int) (0. + drawAreaWidth / markerNumber);
			// use the box size as a standard of comparison
			if (avgDistance > Density.LOW.getThresholdDistance())
				density = Density.LOW;
			else if (avgDistance > Density.MEDIUM.getThresholdDistance())
				density = Density.MEDIUM;
			else if (avgDistance > Density.HIGH.getThresholdDistance())
				density = Density.HIGH;
			else
				density = Density.EXTREME;
			log.finer(() -> String.format("density=%s  convenientDistance=%d  thresholdDistance=%d  elementWidth=%d", //$NON-NLS-1$
					density, avgDistance, density.getThresholdDistance(), density.markerWidth));
			return density;
		}

		private int getThresholdDistance() {
			return markerWidth * distanceThreshold / (1 + settings.getBoxplotScaleOrdinal());
		}
	}

	/**
	 * The displayable marker objects at the x position.
	 * Key is the x axis position with a step distance defined by the element size.
	 * A marker object is a list which holds the record indices assigned to this x position.
	 */
	public static final class MarkerPositions {
		private final TreeMap<Integer, PosMarkers> markerPositions = new TreeMap<>();

		public void clear() {
			markerPositions.clear();
		}

		public PosMarkers put(int xDrawer, PosMarkers posMarkers) {
			return markerPositions.put(xDrawer, posMarkers);
		}

		public PosMarkers get(int xDrawer) {
			return markerPositions.get(xDrawer);
		}

		public boolean isEmpty() {
			return markerPositions.isEmpty();
		}

		public Set<Entry<Integer, PosMarkers>> entrySet() {
			return markerPositions.entrySet();
		}

		public void putAll(MarkerPositions newPositions) {
			markerPositions.putAll(newPositions.markerPositions);
		}

		@Override
		public String toString() {
			return "MarkerPositions [markerPositionSize=" + this.markerPositions.size() + "markerPositions=" + this.markerPositions + "]";
		}
	}

	/**
	 * Graph elements for an x axis position in the summary record row.
	 * The value is the record's data point index in order to get a back reference to the vault data.
	 */
	public static final class PosMarkers implements Iterable<Integer> {
		private final Logger				log								= Logger.getLogger(PosMarkers.class.getName());

		private final List<Integer>	recordIndices			= new ArrayList<>();
		/**
		 * The mid of the drawing strip is 9 pixels and zero is above the mid.
		 */
		private final List<Integer>	yPositions				= new ArrayList<>();

		private final int						elementWidth;
		private final int						yStep;
		private final int						stripHeight;
		private final int						halfDrawingHeight;

		private int									cycleNumber				= 0;																						// number of fully populated element columns
		private int									nextRelativeYPos	= 0;																						// lower corner of a 2x2 element

		PosMarkers(int stripHeight, int elementWidth) {
			this.stripHeight = stripHeight;
			this.elementWidth = elementWidth;
			this.yStep = elementWidth + 1;
			this.halfDrawingHeight = (stripHeight - 1) / 2; // leave at least one pixel at the top of the strip
		}

		/**
		 * @return the trail record index of this element
		 */
		public Integer get(int index) {
			return recordIndices.get(index);
		}

		/**
		 * @param recordIndex is the record's data point index or null for markers which are not drawn
		 */
		public boolean add(Integer recordIndex) {
			int yPosition = recordIndex == null ? null : -nextRelativeYPos + halfDrawingHeight + 0; // add 0 pixel for a little shift to the bottom
			yPositions.add(yPosition);

			// the next position is one step towards the outer border and alternates from the lower half to the upper half
			if ((size() - 1) % 2 == 0 && nextRelativeYPos != 0) {
				nextRelativeYPos = -Math.abs(nextRelativeYPos); // lower half
			} else {
				nextRelativeYPos = Math.abs(nextRelativeYPos) + yStep; // upper half
			}
			// check if the next element exceeds the upper half border
			if (nextRelativeYPos > halfDrawingHeight - elementWidth / 2) {
				cycleNumber += 1;
				// shift the next position downwards from the center line or start at the center line again
				nextRelativeYPos = yStep - cycleNumber % yStep - 1;
			}
			log.finest(() -> String.format("%d cycleNumber=%d nextRelativeYPos=%d", size(), cycleNumber, nextRelativeYPos));
			return recordIndices.add(recordIndex);
		}

		/**
		 * Replace the values at the index with null.
		 */
		public Integer setToNull(int index) {
			yPositions.set(index, null);
			return recordIndices.set(index, null);
		}

		@Override
		public PosMarkers clone() {
			PosMarkers newPosMarkers = new PosMarkers(stripHeight, elementWidth);
			newPosMarkers.cycleNumber = cycleNumber;
			newPosMarkers.nextRelativeYPos = nextRelativeYPos;
			newPosMarkers.recordIndices.addAll(this.recordIndices);
			newPosMarkers.yPositions.addAll(this.yPositions);
			return newPosMarkers;
		}

		public List<Integer> getYPositions() {
			return this.yPositions;
		}

		@Override
		public Iterator<Integer> iterator() {
			return recordIndices.listIterator();
		}

		public int size() {
			return recordIndices.size();
		}

		public Stream<Integer> stream() {
			return recordIndices.stream();
		}

		public List<Integer> toList() {
			return recordIndices;
		}

		@Override
		public String toString() {
			return "PosMarkers [recordIndices=" + this.recordIndices + ", yStep=" + this.yStep + ", stripHeight=" + this.stripHeight + ", cycleNumber=" + this.cycleNumber + ", nextRelativeYPos=" + this.nextRelativeYPos + "]";
		}
	}

	@Override
	public String toString() {
		return "[record=" + this.record.getName() + ", warningMinMax=" + Arrays.toString(this.warningMinMaxValues) + ", xPositions=" + this.xPositions + " ]";
	}

	public List<Integer> defineGrid(boolean innerOnly) {
		List<Integer> grid = new ArrayList<>();
		double xStep = (stripNetWidth + 1) / 10.; // + 1 for a smaller right gap
		if (innerOnly) {
			for (int i = 1; i < 10; i++) {
				grid.add((stripNetX0 + (int) (xStep * i + .5)));
			}
		} else {
			for (int i = 0; i < 11; i++) {
				grid.add((stripNetX0 + (int) (xStep * i + .5)));
			}
		}
		return grid;
	}

	public final SummaryLayout		summary;
	public final TrailRecord			record;

	private Rectangle							drawStripBounds;
	private int										elementWidth;

	private int										stripNetX0;																	// start pos for the first marker
	private int										stripNetWidth;															// relative start pos for the LAST marker

	private double								xValueScaleFactor;
	private double								xValueOffset;
	private double								xPointScaleFactor;
	private double								xPointOffset;

	/**
	 * The displayable marker objects at the x position.
	 * Key is the x axis position with a step distance defined by the element size.
	 * A marker object is a list which holds the record indices assigned to this x position.
	 */
	private final MarkerPositions	xPositions					= new MarkerPositions();
	/**
	 * Levels for the min warning and the max warning.
	 * Null means no warning.
	 */
	public Reminder[]							warningMinMaxValues	= null;

	public SummarySpots(SummaryLayout summary) {
		this.summary = summary;
		this.record = summary.getTrailRecord();
	}

	public void initialize(Rectangle newDrawStripBounds, Density newDensity) {
		xPositions.clear();
		warningMinMaxValues = null;

		this.drawStripBounds = newDrawStripBounds;
		elementWidth = newDensity.markerWidth;

		// elements
		stripNetX0 = newDrawStripBounds.x + elementWidth / 2;
		int tmpWidth = newDrawStripBounds.width - elementWidth; // half left and right gap for overlapping elements
		stripNetWidth = tmpWidth - tmpWidth % elementWidth; // additional right gap because of x position delta (is the elements size)

		double[] decodedScaleMinMax = summary.getScaleMinMax();
		xValueScaleFactor = stripNetWidth / (decodedScaleMinMax[1] - decodedScaleMinMax[0]);
		xValueOffset = decodedScaleMinMax[0] * xValueScaleFactor - .5;

		xPointScaleFactor = HistoSet.decodeDeltaValue(record.getChannelItem(), 1. / 1000.) / ((decodedScaleMinMax[1] - decodedScaleMinMax[0]) / stripNetWidth);
		xPointOffset = HistoSet.encodeVaultValue(record, decodedScaleMinMax[0]) * 1000. * xPointScaleFactor - .5;

		int positionsLimit = Settings.getInstance().isSummarySpotsVisible() ? -1 : Settings.getInstance().getReminderCount();
		xPositions.putAll(defineXPositions(positionsLimit));
	}

	/**
	 * Rebuild from record or suite master record data and take the upmost timestamps only.
	 * @return the markers at the x position.<br/>
	 *         A marker object is a list which holds the record indices assigned to this xAxisPosition.
	 */
	private MarkerPositions defineXPositions(int limit) {
		log.finest(() -> record.getName() + "  limit=" + limit);
		MarkerPositions resultXPositions = new MarkerPositions();

		final Vector<Integer> tmpRecord;
		if (record.getTrailSelector().isTrailSuite()) {
			tmpRecord = record.getSuiteRecords().get(record.getTrailSelector().getSuiteMasterIndex());
		} else {
			tmpRecord = record;
		}

		int actualLimit = limit > 0 && limit < tmpRecord.size() ? limit : tmpRecord.size();
		for (int i = 0; i < actualLimit; i++) {
			Integer point = tmpRecord.get(i);
			if (point != null) {
				int xPos = getXPos(point);
				int xDrawer = xPos - xPos % elementWidth;
				log.finest(() -> "xPos=" + xPos);
				PosMarkers posMarkers = resultXPositions.get(xDrawer);
				if (posMarkers == null) {
					posMarkers = new PosMarkers(drawStripBounds.height, elementWidth);
					resultXPositions.put(xDrawer, posMarkers);
				}
				posMarkers.add(i);
			}
		}
		return resultXPositions;
	}

	/**
	 * Take the measure borders and determine the marker positions which correspond to the borders of the measuring range.
	 * @return the coordinates for the top timestamp and the lowest timestamp of this section.</br>
	 *         The y value is the yPosition value.</br>
	 *         Returns null if the border marker is not displayed.
	 */
	Point[] defineFirstLastPoints(Measure measure) { // todo check if the processing time requires a better solution
		Point[] result = new Point[2];
		int[] indexFirstLast = measure.getRecordSection().getIndexFirstLast();
		for (Entry<Integer, PosMarkers> entry : xPositions.entrySet()) {
			for (Integer recordIndex : entry.getValue()) {
				if (recordIndex == indexFirstLast[0]) {
					for (int i = 0; i < entry.getValue().size(); i++) {
						Integer recordIndex2 = entry.getValue().get(i);
						if (recordIndex2 == indexFirstLast[0]) result[0] = new Point(entry.getKey(), entry.getValue().yPositions.get(i));
					}
				}
				if (recordIndex == indexFirstLast[1]) {
					for (int i = 0; i < entry.getValue().size(); i++) {
						Integer recordIndex2 = entry.getValue().get(i);
						if (recordIndex2 == indexFirstLast[1]) result[1] = new Point(entry.getKey(), entry.getValue().yPositions.get(i));
					}
				}
				if (result[1] != null && result[0] != null) {
					return result;
				}
			}
		}
		return result;
	}

	/**
	 * Take the measure borders and determine the marker positions which correspond to the borders of the measuring range.
	 * @return a map with one or two x positions lists for the top timestamp and the lowest timestamp of this section.</br>
	 *         The lists hold null values except the border indices. This allows the reference to the summary spot.</br>
	 *         Returns an empty map if at least one of the border markers is not displayed.
	 */
	LinkedHashMap<Integer, PosMarkers> defineXPositionFirstLast(Measure measure) { // todo check if the processing time requires a better solution
		int firstXPosition = -1;
		PosMarkers firstXPosMarkers = null;
		int lastXPosition = -1;
		PosMarkers lastXPosMarkers = null;
		int[] indexFirstLast = measure.getRecordSection().getIndexFirstLast();
		for (Entry<Integer, PosMarkers> entry : xPositions.entrySet()) {
			for (Integer recordIndex : entry.getValue()) {
				int resultCounter = 0;
				if (recordIndex == indexFirstLast[0]) {
					PosMarkers resultMarkers = entry.getValue().clone();
					for (int i = 0; i < resultMarkers.size(); i++) {
						Integer recordIndex2 = resultMarkers.get(i);
						if (!(recordIndex2 == indexFirstLast[0] || recordIndex2 == indexFirstLast[1])) {
							resultMarkers.setToNull(i);
						} else {
							resultCounter++;
						}
					}
					if (resultCounter == 2) { // lucky to find both bounds at a single x axis position
						LinkedHashMap<Integer, PosMarkers> result = new LinkedHashMap<>();
						result.put(firstXPosition, resultMarkers);
						return result;
					}
					firstXPosition = entry.getKey();
					firstXPosMarkers = resultMarkers;
				}
				if (recordIndex == indexFirstLast[1]) {
					PosMarkers resultMarkers = entry.getValue().clone();
					for (int i = 0; i < resultMarkers.size(); i++) {
						Integer recordIndex2 = resultMarkers.get(i);
						if (!(recordIndex2 == indexFirstLast[1])) {
							resultMarkers.setToNull(i);
						}
					}
					lastXPosition = entry.getKey();
					lastXPosMarkers = resultMarkers;
				}
				// let's stop if we found the two border markers
				if (firstXPosMarkers != null && lastXPosMarkers != null) {
					LinkedHashMap<Integer, PosMarkers> result = new LinkedHashMap<>();
					result.put(firstXPosition, firstXPosMarkers);
					result.put(lastXPosition, lastXPosMarkers);
					return result;
				}
			}
		}
		return new LinkedHashMap<>();
	}

	/**
	 * Take the measure borders and determine the marker positions which correspond to the measuring section.
	 * @return a map with x positions lists for the measuring section.</br>
	 *         The lists hold null values for markers which are not section members.</br>
	 *         Returns an empty map if none of the markers is displayed.
	 */
	private TreeMap<Integer, PosMarkers> defineXPositions(Measure measure) {
		TreeMap<Integer, PosMarkers> resultXPositions = new TreeMap<>();
		int[] indexFirstLast = measure.getRecordSection().getIndexFirstLast();
		for (Entry<Integer, PosMarkers> entry : xPositions.entrySet()) {
			for (Integer recordIndex : entry.getValue()) {
				if (recordIndex >= indexFirstLast[0] && recordIndex <= indexFirstLast[1]) {
					PosMarkers resultMarkers = entry.getValue().clone();
					for (int i = 0; i < resultMarkers.size(); i++) {
						Integer recordIndex2 = resultMarkers.get(i);
						if (!(recordIndex2 >= indexFirstLast[0] && recordIndex2 <= indexFirstLast[1])) {
							resultMarkers.setToNull(i);
						}
					}
					resultXPositions.put(entry.getKey(), resultMarkers);
				}
			}
		}
		return resultXPositions;
	}

	public int[] defineSpreadXPositions() {
		double avg = record.getQuantile().getAvgFigure();
		double sigma = record.getQuantile().getSigmaFigure();
		int[] result = new int[] { getXPos(avg - 2 * sigma), getXPos(avg), getXPos(avg + 2 * sigma) };
		log.log(Level.FINEST, "left,avg,right=", Arrays.toString(result));
		return result;
	}

	public int[] defineTukeyXPositions() {
		double[] tukeyBoxPlot = record.getQuantile().getTukeyBoxPlot();
		int[] resultXPositions = new int[tukeyBoxPlot.length];
		for (int i = 0; i < tukeyBoxPlot.length; i++) {
			int xPos = getXPos(tukeyBoxPlot[i]);
			resultXPositions[i] = xPos;
		}
		log.finer(() -> Arrays.toString(resultXPositions));
		return resultXPositions;
	}

	/**
	 * @param value is a real measurement value
	 * @return the x axis position (not restricted to the drawing area)
	 */
	private int getXPos(double value) {
		return (int) (value * xValueScaleFactor - xValueOffset);
	}

	/**
	 * @param point is the record point value
	 * @return the x axis position
	 */
	private int getXPos(Integer point) {
		return (int) (point * xPointScaleFactor - xPointOffset);
	}

	public void drawMarkers(GC gc, Measure measure) {
		drawMarkers(gc, defineXPositions(measure), COLOR_BLUE);
	}

	public void drawRecentMarkers(GC gc) {
		drawScalableMarkers(gc, defineXPositions(Settings.getInstance().getReminderCount()), COLOR_RED);
	}

	/**
	 * Draw the summary marker points using given rectangle for display.
	 */
	public void drawMarkers(GC gc) {
		drawScalableMarkers(gc, xPositions, COLOR_GREY);
	}

	/**
	 * Draw markers and skip PosMarkers elements with null values.
	 */
	private void drawMarkers(GC gc, TreeMap<Integer, PosMarkers> tmpXPositions, Color color) {
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(color);
		gc.setBackground(color);

		for (Map.Entry<Integer, PosMarkers> xEntry : tmpXPositions.entrySet()) {
			int actualWidth = elementWidth;

			int xPosOffset = stripNetX0 - elementWidth / 2 - actualWidth / 2 + 1;
			int yPosOffset = drawStripBounds.y - actualWidth / 2 + 1;
			for (Integer yPos : xEntry.getValue().getYPositions()) {
				if (yPos == null) continue;

				gc.fillRectangle(xEntry.getKey() + xPosOffset, yPos + yPosOffset, actualWidth, actualWidth);
				if (log.isLoggable(FINEST)) log.log(FINEST, String.format("x=%d y=%d", xEntry.getKey() + xPosOffset, yPos + yPosOffset));
			}
		}
	}

	/**
	 * Draw markers and skip PosMarkers elements with null values.
	 * Enlarged outliers.
	 */
	private void drawScalableMarkers(GC gc, MarkerPositions markerPositions, Color color) {
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(color);
		gc.setBackground(color);

		int[] tukeyXPositions = defineTukeyXPositions();
		markerPositions.entrySet().stream().filter(x -> x.getKey() >= 0 && x.getKey() <= stripNetWidth).forEach(xEntry -> {
			int lowerLimit = tukeyXPositions[LOWER_WHISKER.ordinal()] / elementWidth * elementWidth; // floor
			int upperLimit = (tukeyXPositions[UPPER_WHISKER.ordinal()] / elementWidth + 1) * elementWidth; // ceiling
			int actualWidth = xEntry.getKey() < lowerLimit || xEntry.getKey() > upperLimit ? elementWidth * 2 : elementWidth;

			int xPosOffset = stripNetX0 - elementWidth / 2 - actualWidth / 2 + 1;
			int yPosOffset = drawStripBounds.y - actualWidth / 2 + 1;
			for (Integer yPos : xEntry.getValue().getYPositions()) {
				if (yPos == null) continue;

				gc.fillRectangle(xEntry.getKey() + xPosOffset, yPos + yPosOffset, actualWidth, actualWidth);
				if (log.isLoggable(FINEST)) log.log(FINEST, String.format("x=%d y=%d", xEntry.getKey() + xPosOffset, yPos + yPosOffset));
			}
		});
	}

	/**
	 * Search the two border markers in the startSpots and this spots object.
	 * Draw connecting lines for the markers corresponding to the measure timestamp and to the delta timestamp.
	 * @param startSpots is the previous summary spot in the summary chart
	 */
	public void drawConnections(GC gc, SummarySpots startSpots, Measure measure) {
		Point[] startFirstLastPoints = startSpots.defineFirstLastPoints(measure);
		Point[] endFirstLastPoints = defineFirstLastPoints(measure);

		int xPosOffset = stripNetX0 - elementWidth / 2 + 1;
		int startYPosOffset = startSpots.drawStripBounds.y + 1;
		int endYPosOffset = drawStripBounds.y + 1;

		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_DASH);
		gc.setForeground(COLOR_DARK_GREEN);
		gc.setBackground(COLOR_DARK_GREEN);
		if (startFirstLastPoints[0] != null && endFirstLastPoints[0] != null) {
			gc.drawLine(endFirstLastPoints[0].x + xPosOffset, endFirstLastPoints[0].y + endYPosOffset //
					, startFirstLastPoints[0].x + xPosOffset, startFirstLastPoints[0].y + startYPosOffset);
		}

		if (measure.isDeltaMeasure()) {
			gc.setForeground(COLOR_BLUE);
			gc.setBackground(COLOR_BLUE);
			if (startFirstLastPoints[1] != null && endFirstLastPoints[1] != null) {
				gc.drawLine(endFirstLastPoints[1].x + xPosOffset, endFirstLastPoints[1].y + endYPosOffset //
						, startFirstLastPoints[1].x + xPosOffset, startFirstLastPoints[1].y + startYPosOffset);
			}
		}
	}

	public Rectangle getDrawStripBounds() {
		return drawStripBounds;
	}

	public int getElementWidth() {
		return elementWidth;
	}

	private List<Integer> getSnappedIndexes(int stripXPos) {
		int xDrawer = stripXPos - stripXPos % elementWidth;
		PosMarkers posMarkers = xPositions.get(xDrawer);
		log.log(Level.FINER, "stripXPos=", stripXPos);
		return posMarkers != null ? posMarkers.toList() : new ArrayList<>();
	}

	/**
	 * @param stripXPos is the 0-based x axis position on the record drawing strip area
	 * @param stripYPos is the 0-based x axis position on the record drawing strip area (top is zero)
	 * @return the recordset timestamp indices of the log data identified by the mouse position or an empty list
	 */
	public List<Integer> getSnappedIndexes(int stripXPos, int stripYPos) {
		if (drawStripBounds == null) return new ArrayList<>(); // graphics not yet drawn
		if (stripYPos < 0 || stripYPos > drawStripBounds.height) return new ArrayList<>();
		if (xPositions.isEmpty()) return new ArrayList<>(); // either spots not initialized or no markers

		List<Integer> indexes = getSnappedIndexes(stripXPos);
		if (indexes.size() <= 1) {
			return indexes;
		} else {
			int maxAdditionalMarker = (drawStripBounds.height - elementWidth) / 2 / (elementWidth + 1) * 2;
			if (maxAdditionalMarker == 0 || indexes.size() > maxAdditionalMarker + 1) {
				return indexes;
			} else {
				// transform stripYPos into a scale relative to the markers area
				int markersHeight = (elementWidth + 1) * (maxAdditionalMarker + 1);
				int markersYPos = stripYPos - ((drawStripBounds.height - markersHeight) / 2);
				if (markersYPos >= 0 && markersYPos < markersHeight) {
					// determine the markers index which the cursor is pointing to (relative position is 0-based)
					int relativePosition = markersYPos / (elementWidth + 1);
					int alternatingPosition = maxAdditionalMarker / 2 - relativePosition;
					log.finest(() -> "stripYPos=" + stripYPos + "  markersYPos=" + markersYPos + "  elementWidth=" + elementWidth + "  relativePosition=" + relativePosition + "  alternatingPosition=" + alternatingPosition);
					int index = alternatingPosition > 0 ? alternatingPosition * 2 - 1 : -alternatingPosition * 2;
					if (index >= indexes.size()) { // the vertical space is not filled with markers
						return new ArrayList<>();
					} else {
						log.finest(() -> "alternatingPosition=" + alternatingPosition + "  index=" + index);
						return new ArrayList<>(Arrays.asList(new Integer[] { indexes.get(index) }));
					}
				} else {
					return new ArrayList<>();
				}
			}
		}
	}

}