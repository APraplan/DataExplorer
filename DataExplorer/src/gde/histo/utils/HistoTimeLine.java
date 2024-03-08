/**************************************************************************************
  	This file is part of GNU DataExplorer.getInstance().

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.getInstance().  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
    					2016,2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.histo.utils;

import static gde.histo.utils.HistoTimeLine.Density.EXTREME;
import static gde.histo.utils.HistoTimeLine.Density.HIGH;
import static gde.histo.utils.HistoTimeLine.Density.LOW;
import static gde.histo.utils.HistoTimeLine.Density.MEDIUM;
import static java.util.logging.Level.FINER;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import gde.GDE;
import gde.config.Settings;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.GraphicsUtils;
import gde.utils.LocalizedDateTime;
import gde.utils.LocalizedDateTime.DateTimePattern;

/**
 * Draw time line with tick marks and numbers.
 * Support logarithmic distances and elaborate x-axis point placement based on box width and number of points.
 * @author Thomas Eickert (USER)
 */
public final class HistoTimeLine {
	private final static String	$CLASS_NAME	= HistoTimeLine.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Maximum distance from an x axis position for snapping the nearest adjacent position (value is a number of pixels).
	 */
	private final static int		X_TOLERANCE	= 20;

	/**
	 * Degree of population on the x -axis.
	 */
	enum Density {
		EXTREME(4), HIGH(8), MEDIUM(10), LOW(16);

		private final Settings			settings					= Settings.getInstance();

		/** box may grow or shrink by this value based on log duration */
		public final int						boxWidthAmplitude	= 2;

		private final int						boxWidth;																		// in pixel

		/** use this to avoid repeatedly cloning actions instead of values() */
		public static final Density	VALUES[]					= values();

		private Density(int boxWidth) {
			this.boxWidth = boxWidth;
		}

		public static Density fromOrdinal(int ordinal) {
			return Density.VALUES[ordinal];
		}

		public static String toString(Density density) {
			return density.name();
		}

		public int getScaledBoxWidth() {
			return (int) (this.boxWidth * (1 + (this.settings.getBoxplotScaleOrdinal() - 1) / 3.));
		}
	}

	/**
	 * X axis positions in reversed order of timestamps.
	 */
	public static final class ScalePositions {
		private final TreeMap<Long, Integer> scalePositions;

		/**
		 * Take the timeline width and calculates the pixel positions of the timestamp values.
		 * Left and right margins are left free for boxplots.
		 * In case of few timestamps the left and right margins will be increased.
		 */
		public static ScalePositions createScalePositions(Density density, int width, TimeScale relativeTimeScale) {
			int leftMargin;
			int rightMargin;
			if (Settings.getInstance().isXAxisLogarithmicDistance() || density == Density.LOW || relativeTimeScale.size() <= 2) {
				// bigger margins present the data in a more harmonic manner
				int remainingWidthPerItem = (width - density.boxWidth) / relativeTimeScale.size();
				if (remainingWidthPerItem > 2 * Density.LOW.boxWidth)
					// in case of few items: placing the items acc to the average distance is more harmonic than using the double box size
					leftMargin = rightMargin = width / (relativeTimeScale.size() + 1);
				else
					leftMargin = rightMargin = (density.getScaledBoxWidth()) / 2; // minimum distance equal to boxWidth assumed
			} else {
				leftMargin = rightMargin = (density.getScaledBoxWidth()) / 2; // minimum distance equal to boxWidth assumed
			}
			int netWidth = width - leftMargin - rightMargin;
			log.finer(() -> String.format("width = %4d|leftMargin = %4d|rightMargin = %4d|netWidth = %4d", //
					width, leftMargin, rightMargin, netWidth));

			TreeMap<Long, Integer> newScalePositions = new TreeMap<>(Collections.reverseOrder());
			for (Entry<Long, Double> entry : relativeTimeScale.entrySet()) {
				newScalePositions.put(entry.getKey(), leftMargin + (int) (entry.getValue() * netWidth));
				log.finest(() -> "timeStamp = " + entry.getKey() + " position = " + newScalePositions.get(entry.getKey()));
			}
			return new ScalePositions(newScalePositions);
		}

		private ScalePositions(TreeMap<Long, Integer> scalePositions) {
			this.scalePositions = scalePositions;
		}

		public int get(long scaleTimeStamp_ms) {
			return scalePositions.get(scaleTimeStamp_ms);
		}

		/**
		 * @return in reversed order
		 */
		public Set<Entry<Long, Integer>> entrySet() {
			return scalePositions.entrySet();
		}

		public int size() {
			return scalePositions.size();
		}
	}

	/**
	 * Map histoset timestamps to x-axis positions.
	 */
	private static final class TimeScale {
		private final TreeMap<Long, Double>							timeScale;
		private final BiFunction<Long, Double, Double>	putter;

		double																					runningSum	= 0.;
		double																					runningMin	= Double.MAX_VALUE;

		public TimeScale() {
			this(true);
		}

		public TimeScale(boolean isXAxisReversed) {
			if (isXAxisReversed) {
				this.timeScale = new TreeMap<Long, Double>(Collections.reverseOrder());
				this.putter = (scaleTimeStamp_ms, position) -> this.timeScale.put(scaleTimeStamp_ms, position);
			} else {
				this.timeScale = new TreeMap<Long, Double>();
				// value is based on descending order and must be transformed (e.g. a lefthand value becomes a righthand value)
				this.putter = (scaleTimeStamp_ms, position) -> this.timeScale.put(scaleTimeStamp_ms, 1. - position);
			}
		}

		public long getLeftMostTimeStamp_ms() {
			return timeScale.firstKey();
		}

		public long getRightMostTimeStamp_ms() {
			return timeScale.lastKey();
		}

		public Double put(long scaleTimeStamp_ms, double distance) {
			runningSum += distance;
			runningMin = Math.min(runningMin, distance);
			return putter.apply(scaleTimeStamp_ms, distance);
		}

		public Double putRunningSum(long scaleTimeStamp_ms, double distance) {
			runningSum += distance;
			return putter.apply(scaleTimeStamp_ms, runningSum);
		}

		/**
		 * @return in reversed order
		 */
		public Set<Entry<Long, Double>> entrySet() {
			return timeScale.entrySet();
		}

		public int size() {
			return timeScale.size();
		}

		@Override
		public String toString() {
			return "TimeScale [timeScaleSize=" + this.timeScale.size() + ", runningSum=" + this.runningSum + ", runningMin=" + this.runningMin + "]";
		}
	}

	/**
	 * Access to timestamps by x axis position (naturalOrder).
	 */
	private static final class ScaleTimeStamps {

		private final TreeMap<Integer, List<Long>> scaleTimeStamps_ms;

		public ScaleTimeStamps(ScalePositions scalePositions) {
			this.scaleTimeStamps_ms = defineScaleTimeStamps_ms(scalePositions);
		}

		/**
		 * Create the map for optimized access to timestamps by x axis position.
		 * Optimized for naturally ordered x positions and timestamps ordered based on the user's x axis order setting.
		 */
		private TreeMap<Integer, List<Long>> defineScaleTimeStamps_ms(ScalePositions scalePositions) {
			TreeMap<Integer, List<Long>> scaleTimeStamps = new TreeMap<Integer, List<Long>>();
			if (Settings.getInstance().isXAxisReversed()) {
				Entry<Integer, List<Long>> previous = null;
				for (Entry<Long, Integer> entry : scalePositions.entrySet()) { // TreeMap is reversed
					if (previous == null || entry.getValue() != previous.getKey()) {
						scaleTimeStamps.put(entry.getValue(), new ArrayList<Long>());
						previous = scaleTimeStamps.lastEntry();
					}
					previous.getValue().add(entry.getKey()); // results in a reverse timeStamp order (decreasing timestamps)
				}
			} else {
				Entry<Integer, List<Long>> previous = null;
				for (Entry<Long, Integer> entry : scalePositions.entrySet()) { // TreeMap is reversed
					if (previous == null || entry.getValue() != previous.getKey()) {
						scaleTimeStamps.put(entry.getValue(), new ArrayList<Long>());
						previous = scaleTimeStamps.firstEntry(); // due to decreasing x axis positions in the scalePositions map
					}
					previous.getValue().add(0, entry.getKey()); // results in a natural timeStamp order
				}
			}
			return scaleTimeStamps;
		}

		public Long getSnappedTimeStamp_ms(int xPos) {
			Long timeStamp_ms = null;

			final Entry<Integer, List<Long>> lowerEntry = scaleTimeStamps_ms.lowerEntry(xPos);
			if (lowerEntry == null) {
				Entry<Integer, List<Long>> ceilingEntry = scaleTimeStamps_ms.ceilingEntry(xPos);
				if (ceilingEntry == null) throw new UnsupportedOperationException("an empty scale is not allowed");
				timeStamp_ms = ceilingEntry.getValue().get(0);
			} else {
				if (xPos == lowerEntry.getKey())
					timeStamp_ms = lowerEntry.getValue().get(0); // take the first timeStamp for simplicity reasons
				else {
					Entry<Integer, List<Long>> ceilingEntry = scaleTimeStamps_ms.ceilingEntry(xPos);
					if (ceilingEntry == null) {
						; // one single entry but grabbing it makes no sense
					} else {
						if (xPos <= lowerEntry.getKey() + X_TOLERANCE || xPos >= ceilingEntry.getKey() - X_TOLERANCE) {
							timeStamp_ms = xPos <= (ceilingEntry.getKey() + lowerEntry.getKey()) / 2 //
									? lowerEntry.getValue().get(lowerEntry.getValue().size() - 1) //
									: ceilingEntry.getValue().get(0);
						} else
							; // midst in the distance but too far away
					}
				}
			}
			return timeStamp_ms;
		}

		public long getAdjacentTimeStamp_ms(int xPos) {
			final long timeStamp_ms;

			final Entry<Integer, List<Long>> lowerEntry = this.scaleTimeStamps_ms.lowerEntry(xPos);
			if (lowerEntry == null) {
				Entry<Integer, List<Long>> ceilingEntry = this.scaleTimeStamps_ms.ceilingEntry(xPos);
				if (ceilingEntry == null) throw new UnsupportedOperationException(" an empty scale is not allowed");
				timeStamp_ms = ceilingEntry.getValue().get(0);
			} else {
				if (xPos == lowerEntry.getKey()) {
					timeStamp_ms = lowerEntry.getValue().get(0); // take the first timeStamp for simplicity reasons
				} else {
					Entry<Integer, List<Long>> ceilingEntry = this.scaleTimeStamps_ms.ceilingEntry(xPos);
					if (ceilingEntry == null) {
						timeStamp_ms = lowerEntry.getValue().get(lowerEntry.getValue().size() - 1);
					} else {
						timeStamp_ms = xPos <= (ceilingEntry.getKey() + lowerEntry.getKey()) / 2 //
								? lowerEntry.getValue().get(lowerEntry.getValue().size() - 1) //
								: ceilingEntry.getValue().get(0);
					}
				}
			}
			return timeStamp_ms;
		}

		public int getScaleTimeStampsSum_ms() {
			return scaleTimeStamps_ms.entrySet().parallelStream().mapToInt(c -> c.getValue().size()).sum();
		}

		public int size() {
			return scaleTimeStamps_ms.size();
		}

	}

	private final Settings	settings	= Settings.getInstance();

	private TrailRecordSet	trailRecordSet;
	/**
	 * Chart region defined by the full timescale length and the y axis height
	 */
	private Rectangle				curveAreaBounds;
	/**
	 * number of pixels for the timescale length (includes left/right margins and the chart region)
	 */
	private int							width;

	private TimeScale				relativeTimeScale;
	private Density					density;
	private ScalePositions	scalePositions;
	private ScaleTimeStamps	scaleTimeStamps;

	/**
	 * Take the timeline width and calculates the x-axis pixel positions for the timestamp values.
	 * @param newTrailRecordSet any recordset object (no trail recordset required)
	 * @param newCurveAreaBounds number of pixels for the timescale length including left/right margins for boxplots
	 */
	public synchronized void initialize(TrailRecordSet newTrailRecordSet, Rectangle newCurveAreaBounds) {
		this.trailRecordSet = newTrailRecordSet;
		this.curveAreaBounds = newCurveAreaBounds;
		this.width = newCurveAreaBounds.width;
		this.relativeTimeScale = defineRelativeScale();
		this.density = defineDensity();
		this.scalePositions = ScalePositions.createScalePositions(this.density, this.width, this.relativeTimeScale);
		this.scaleTimeStamps = null;
	}

	@Override
	public String toString() {
		return String.format("width=%d  leftmostTimeStamp = %,d  rightmostTimeStamp = %,d  density=%s scalePositionsSize=%d scaleTimeStamps_ms=%d", //
				this.width, this.relativeTimeScale.getLeftMostTimeStamp_ms(), this.relativeTimeScale.getRightMostTimeStamp_ms(), this.density.toString(), this.scaleTimeStamps.size(), scaleTimeStamps.getScaleTimeStampsSum_ms());
	}

	/**
	 * Draw the time line for the standard x-axis and the logarithmic distance axis.
	 * Respect left/right margin, i.e. uses the chart region only.
	 * @param gc graphics context
	 */
	public void drawTimeLine(GC gc) {
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(DataExplorer.getInstance().COLOR_BLACK);

		int x0 = curveAreaBounds.x;
		int y0 = curveAreaBounds.y + curveAreaBounds.height;
		gc.drawLine(x0 - 1, y0, x0 + this.width + 1, y0);
		log.fine(() -> String.format("time line - x0=%d y0=%d - width=%d", x0, y0, this.width)); //$NON-NLS-1$

		// calculate the maximum time to be displayed and define the corresponding label format
		final DateTimePattern timeFormat = getScaleFormat(this.trailRecordSet.getTopTimeStamp_ms() - this.trailRecordSet.getLowestTimeStamp_ms());

		String timeLineDescription;
		Point pt; // to calculate the space required to draw the time values
		if (timeFormat == DateTimePattern.MMdd) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0745);
		} else if (timeFormat == DateTimePattern.MMdd_HH) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0744);
		} else if (timeFormat == DateTimePattern.yyyyMM) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0743);
		} else if (timeFormat == DateTimePattern.yyyyMMdd) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0742);
		} else if (timeFormat == DateTimePattern.yyyyMMdd_HHmm) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0741);
		} else {
			throw new UnsupportedOperationException();
		}
		long sampleTimeStamp_ms = trailRecordSet.getTimeStepSize() == 0 ? 11 : trailRecordSet.getTopTimeStamp_ms();
		pt = gc.textExtent(LocalizedDateTime.getFormatedTime(timeFormat, sampleTimeStamp_ms)); // $NON-NLS-1$
		GraphicsUtils.drawTimeLineText(timeLineDescription, (x0 + this.width / 2), y0 + pt.y * 5 / 2 + 2, gc, SWT.HORIZONTAL);
		drawTickMarks(gc, x0, y0, pt, timeFormat);
	}

	/**
	 * Calculate the maximum time to be displayed and defines the corresponding label format.
	 * @param totalDisplayTime_ms timespan
	 * @return format (e.g. TimeLine.TIME_LINE_DATE_TIME)
	 */
	private DateTimePattern getScaleFormat(long totalDisplayTime_ms) {
		final DateTimePattern timeFormat;
		log.finer(() -> "totalDisplayTime_ms = " + totalDisplayTime_ms); //$NON-NLS-1$

		long totalTime_month = TimeUnit.DAYS.convert(totalDisplayTime_ms, TimeUnit.MILLISECONDS) / 30;
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(this.relativeTimeScale.getLeftMostTimeStamp_ms()); // start year
		if (totalTime_month < 12 && Calendar.getInstance().get(Calendar.YEAR) == cal.get(Calendar.YEAR)) {
			// starts in the current year
			if (this.density == EXTREME)
				timeFormat = DateTimePattern.MMdd;
			else if (this.density == HIGH)
				timeFormat = DateTimePattern.MMdd_HH;
			else if (this.density == MEDIUM)
				timeFormat = DateTimePattern.yyyyMMdd;
			else
				timeFormat = DateTimePattern.yyyyMMdd_HHmm;
		} else {
			if (this.density == EXTREME)
				timeFormat = DateTimePattern.yyyyMM;
			else if (this.density == HIGH)
				timeFormat = DateTimePattern.yyyyMM;
			else if (this.density == MEDIUM)
				timeFormat = DateTimePattern.yyyyMMdd;
			else
				timeFormat = DateTimePattern.yyyyMMdd_HHmm;
		}
		log.finer(() -> "timeLineText = " + Messages.getString(MessageIds.GDE_MSGT0267)); //$NON-NLS-1$
		return timeFormat;
	}

	/**
	 * @return list holding the timestamps and pixel positions (leftmost: 0, rightmost: width)
	 */
	public ScalePositions getScalePositions() {
		return this.scalePositions;
	}

	/**
	 * Calculate the relative position on the timescale based on isReversed and isTimeframeAdjustment.
	 * (leftmost: 0, rightmost: 1)
	 */
	@SuppressWarnings({ "unused" })
	private TimeScale defineRelativeScale() {
		TimeScale result;
		long maxVerifiedTimeStamp = -1; // first timeStamp maximumTimeStamp which is the first one due to descending order

		TimeScale applicableDistances = new TimeScale();
		{// pass 1: build distances list and distances sum for all timestamps in the timestamp sliding window
			long lastTimeStamp = 0;
			for (int i = 0; i < this.trailRecordSet.getTimeStepSize(); i++) {
				long currentTimeStamp = ((long) this.trailRecordSet.getTime_ms(i));
				if (lastTimeStamp != 0) {
					double currentDistance = lastTimeStamp - currentTimeStamp;
					applicableDistances.put(currentTimeStamp, currentDistance);
				} else {
					maxVerifiedTimeStamp = currentTimeStamp;
				}
				lastTimeStamp = currentTimeStamp;
			}
			log.finer(() -> "applicableDistancesSum=" + applicableDistances.runningSum + " number of distances=" + applicableDistances.size());
		}
		{// pass 2: build the relative timeScale list from relative distances
			result = new TimeScale(this.settings.isXAxisReversed());
			result.put(maxVerifiedTimeStamp, 0.); // highest timestamp is at the leftmost or rightmost position
			@SuppressWarnings("unused")
			final boolean isNaturalTimescale = false;
			if (isNaturalTimescale == true) {
				// true is not used anymore:
				// use 'non-logarithmic' with a spread ordinal of zero which results in values very close to the natural timescale
				// build final relative TimeScale List --- this a simple version illustrating the calculation baseline
				for (Entry<Long, Double> entry : applicableDistances.entrySet()) {
					double relativeDistance = entry.getValue() / applicableDistances.runningSum;
					result.putRunningSum(entry.getKey(), relativeDistance);
				}
				log.log(FINER, "relativeTimeScaleSum=", result.runningSum);
			} else {
				// pass 2.1: build normalized distances List
				TimeScale normalizedDistances = new TimeScale();
				for (Entry<Long, Double> entry : applicableDistances.entrySet()) {
					double normalizedDistance = this.settings.isXAxisLogarithmicDistance() //
							? entry.getValue() != 0 ? Math.log(entry.getValue() / applicableDistances.runningSum) : 0 //
							: entry.getValue() / applicableDistances.runningSum;
					normalizedDistances.put(entry.getKey(), normalizedDistance);
				}
				log.log(FINER, "normalizedDistancesSum=", normalizedDistances.runningSum);

				// pass 2.2: take logDistanceMin as a reference and scale to the final position range which is 0 to 1
				// high spread values result in a rather equidistant placement of timesteps (distant timestamps move close together which
				// results in more space for timesteps with small distances)
				// so we get reasonable adjustments for spreadValues from 0 to 5
				double logBaseDivisor = Math.log(1 + Math.pow(10, this.settings.getXAxisSpreadOrdinal() - 3));
				double scaleConstant = normalizedDistances.runningSum / logBaseDivisor + (1 - normalizedDistances.runningMin / logBaseDivisor) * normalizedDistances.size();
				for (Entry<Long, Double> entry2 : normalizedDistances.entrySet()) {
					double relativeDistance = (1. + (entry2.getValue() - normalizedDistances.runningMin) / logBaseDivisor) / scaleConstant;
					result.putRunningSum(entry2.getKey(), relativeDistance);
				}
				log.log(FINER, "relativeTimeScaleSum=", result.runningSum);
			}
		}
		return result;
	}

	/**
	 * Calculate a density indicator based on the current width and the boxplot element scaled sizes
	 * without width amplitude settings.
	 * Does not take elements which have a distance less than one pixel and will be thus positioned at the same place.
	 * @return
	 */
	private Density defineDensity() {
		Density result;
		List<Double> relativeDistances = new ArrayList<>();
		Entry<Long, Double> lastEntry = null;
		for (Entry<Long, Double> entry : this.relativeTimeScale.entrySet()) {
			if (lastEntry != null) {
				double relativeDistance = Math.abs(lastEntry.getValue() - entry.getValue());
				double absoluteDistance = relativeDistance * this.width;
				if (absoluteDistance > 0.5)
					relativeDistances.add(Math.abs(lastEntry.getValue() - entry.getValue()));
				else
					log.finer(() -> String.format("subpixel distance discarded at %d    absoluteDistance = %f", //$NON-NLS-1$
							entry.getKey(), absoluteDistance));

			}
			lastEntry = entry;
		}
		if (relativeDistances.size() < 3)
			result = LOW;
		else {
			Collections.sort(relativeDistances);
			// a high distanceQuantile value indicates that there is plenty of space between the x-axis points
			// use relative distance and calculate lower or higher quantile according to user's priorities
			double distanceQuantile = relativeDistances.get(relativeDistances.size() * (2 + 2 * (this.settings.getBoxplotScaleOrdinal() + 1)) / 10);
			int absoluteDistance = (int) (this.width * distanceQuantile + .5);
			// use the box size as a standard of comparison
			if (absoluteDistance > LOW.boxWidth)
				result = LOW;
			else if (absoluteDistance > MEDIUM.boxWidth)
				result = MEDIUM;
			else if (absoluteDistance > HIGH.boxWidth)
				result = HIGH;
			else
				result = EXTREME;
			log.fine(() -> String.format("absoluteDistance=%d  boxType=%s   relativeDistancesMin=%f relativeDistancesMax=%f", //$NON-NLS-1$
					absoluteDistance, this.density.toString(), relativeDistances.get(0), relativeDistances.get(relativeDistances.size() - 1)));
		}
		log.log(FINER, "", this); //$NON-NLS-1$
		return result;
	}

	private void drawTickMarks(GC gc, int x0, int y0, Point pt, DateTimePattern timeFormat) {
		int tickLength = pt.y / 2 + 1;
		int gap = pt.y / 3;
		int labelGap = 4 + pt.x;
		int lastXPosLabel = -Integer.MAX_VALUE;
		String lastText = GDE.STRING_BLANK;
		for (Entry<Long, Integer> entry : this.getScalePositions().entrySet()) {
			int xPos = x0 + entry.getValue();
			String text = LocalizedDateTime.getFormatedTime(timeFormat, entry.getKey());
			if (!lastText.equals(text) && (Math.abs(lastXPosLabel - xPos) > labelGap)) { // abs supports asc / desc scale
				gc.drawLine(xPos, y0 + 1, xPos, y0 + tickLength);
				GraphicsUtils.drawTextCentered(text, xPos, y0 + tickLength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
				lastText = text;
				lastXPosLabel = xPos;
			} else {
				gc.drawLine(xPos, y0 + 1, xPos, y0 + tickLength / 2);
			}
		}
	}

	/**
	 * @param timestamp_ms
	 * @return the timestamp position in the drawing area (relative to x0 which is the left position of the drawing canvas)
	 */
	public int getXPosTimestamp(long timestamp_ms) {
		log.finest(() -> "scalePositionsSize=" + this.scalePositions.size() + "  timestamp_ms=  " + timestamp_ms); //$NON-NLS-1$
		return this.scalePositions.get(timestamp_ms);
	}

	/**
	 * @param xPos current position in the drawing area (relative to x0 which is the left position of the drawing canvas)
	 * @return the time stamp of the trail recordset which is the closest one to xPos --- or null if xPos is too far away
	 */
	public Long getSnappedTimestamp(int xPos) {
		if (this.scaleTimeStamps == null) this.scaleTimeStamps = new ScaleTimeStamps(this.scalePositions);
		return this.scaleTimeStamps.getSnappedTimeStamp_ms(xPos);
	}

	/**
	 * @param xPos current position in the drawing area (relative to x0 which is the left position of the drawing canvas)
	 * @return the time stamp of the trail recordset which is the closest one to xPos
	 */
	public long getAdjacentTimestamp(int xPos) {
		if (this.scaleTimeStamps == null) this.scaleTimeStamps = new ScaleTimeStamps(this.scalePositions);
		return scaleTimeStamps.getAdjacentTimeStamp_ms(xPos);
	}

	public Density getDensity() {
		return this.density;
	}

	public Rectangle getCurveAreaBounds() {
		return this.curveAreaBounds;
	}

}
