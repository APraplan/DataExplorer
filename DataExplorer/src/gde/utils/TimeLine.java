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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import gde.GDE;
import gde.config.Settings;
import gde.data.RecordSet;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.LocalizedDateTime.DateTimePattern;
import gde.utils.LocalizedDateTime.DurationPattern;

/**
 * Utility class to draw time line with tick marks and numbers
 * @author Winfried Brügmann
 */
public class TimeLine {
	final static Logger						log										= Logger.getLogger(TimeLine.class.getName());

	public final static int				TIME_LINE_MSEC				= 0;
	public final static int				TIME_LINE_SEC					= 1;
	public final static int				TIME_LINE_SEC_MIN			= 2;
	public final static int				TIME_LINE_MIN					= 4;
	public final static int				TIME_LINE_MIN_HRS			= 6;
	public final static int				TIME_LINE_HRS					= 8;
	public final static int				TIME_LINE_HRS_DAYS		= 12;
	public final static int				TIME_LINE_DAYS				= 14;
	public final static int				TIME_LINE_DAYS_MONTH	= 16;
	public final static int				TIME_LINE_MONTH				= 18;
	public final static int				TIME_LINE_MONTH_YEARS	= 20;
	public final static int				TIME_LINE_YEARS				= 22;

	String												timeLineText					= Messages.getString(MessageIds.GDE_MSGT0267);
	boolean												isTimeLinePrepared		= false;
	final static SimpleDateFormat	timeFormatSeconds			= new SimpleDateFormat("ss.SSS");
	final static SimpleDateFormat	timeFormatMinutes			= new SimpleDateFormat("mm:ss.SSS");
	final static SimpleDateFormat	timeFormatHours				= new SimpleDateFormat("HH:mm:ss.SSS");

	/**
	 * calculates the maximum time to be displayed and the scale number factor
	 * @param totalDisplayTime_ms
	 * @return maxTimeNumber, scale number factor
	 */
	public int[] getScaleMaxTimeNumber(double totalDisplayTime_ms) {
		int factor = 10; // for the most cases (make factor 10 based to enable 0.5 by factor 5)
		int format = TimeLine.TIME_LINE_MSEC; // the time format type

		if (TimeLine.log.isLoggable(Level.FINE)) TimeLine.log.log(Level.FINE, "totalDisplayTime_ms = " + totalDisplayTime_ms); //$NON-NLS-1$

		long totalTime_msec = (long) totalDisplayTime_ms;
		long totalTime_sec = (long) (totalDisplayTime_ms / 1000.0);
		long totalTime_min = TimeUnit.MINUTES.convert(totalTime_sec, TimeUnit.SECONDS);
		long totalTime_std = TimeUnit.HOURS.convert(totalTime_sec, TimeUnit.SECONDS);
		long totalTime_days = TimeUnit.DAYS.convert(totalTime_sec, TimeUnit.SECONDS);
		long totalTime_month = TimeUnit.DAYS.convert(totalTime_sec, TimeUnit.SECONDS) / 30;
		long totalTime_year = TimeUnit.DAYS.convert(totalTime_sec, TimeUnit.SECONDS) / 365;
		if (TimeLine.log.isLoggable(Level.FINE))
			TimeLine.log.log(Level.FINE,
					"totalTime_std = " + totalTime_std + "; totalTime_min = " + totalTime_min + "; totalTime_sec = " + totalTime_sec + "; totalTime_ms = " + totalTime_msec + " - " + Integer.MAX_VALUE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		int maxTimeNumberFormated; // the biggest number in the scale to be displayed

		boolean isTimeFormatAbsolute = DataExplorer.getInstance().getActiveRecordSet() != null && Settings.getInstance().isTimeFormatAbsolute()
				&& !DataExplorer.getInstance().getActiveRecordSet().isCompareSet()
				&& !DataExplorer.getInstance().getActiveRecordSet().isZoomMode();

		if (totalTime_year > 5) {
			maxTimeNumberFormated = (int) totalTime_year;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0714) : Messages.getString(MessageIds.GDE_MSGT0392);
			format = TimeLine.TIME_LINE_YEARS;
		}
		else if (totalTime_month >= 12) {
			maxTimeNumberFormated = (int) totalTime_month;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0715) : Messages.getString(MessageIds.GDE_MSGT0393);
			format = TimeLine.TIME_LINE_MONTH_YEARS;
		}
		else if (totalTime_month > 3) {
			maxTimeNumberFormated = (int) totalTime_month;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0716) : Messages.getString(MessageIds.GDE_MSGT0394);
			format = TimeLine.TIME_LINE_MONTH;
		}
		else if (totalTime_days > 30) {
			maxTimeNumberFormated = (int) totalTime_days;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0717) : Messages.getString(MessageIds.GDE_MSGT0395);
			format = TimeLine.TIME_LINE_DAYS_MONTH;
		}
		else if (totalTime_days > 7) {
			maxTimeNumberFormated = (int) totalTime_days;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0718) : Messages.getString(MessageIds.GDE_MSGT0396);
			format = TimeLine.TIME_LINE_DAYS;
		}
		else if (totalTime_std >= 72) {
			maxTimeNumberFormated = (int) totalTime_std;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0719) : Messages.getString(MessageIds.GDE_MSGT0397);
			format = TimeLine.TIME_LINE_HRS_DAYS;
		}
		else if (totalTime_std > 5) {
			maxTimeNumberFormated = (int) totalTime_std;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0720) : Messages.getString(MessageIds.GDE_MSGT0265);
			format = TimeLine.TIME_LINE_HRS;
		}
		else if (totalTime_min >= 60) {
			maxTimeNumberFormated = (int) totalTime_min;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0721) : Messages.getString(MessageIds.GDE_MSGT0266);
			format = TimeLine.TIME_LINE_MIN_HRS;
		}
		else if (totalTime_min > 10) {
			maxTimeNumberFormated = (int) totalTime_min;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0722) : Messages.getString(MessageIds.GDE_MSGT0267);
			format = TimeLine.TIME_LINE_MIN;
		}
		else if (totalTime_sec >= 60) {
			maxTimeNumberFormated = (int) totalTime_sec;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0723) : Messages.getString(MessageIds.GDE_MSGT0268);
			format = TimeLine.TIME_LINE_SEC_MIN;
		}
		else if (totalTime_sec > 10) {
			maxTimeNumberFormated = (int) totalTime_sec;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0724) : Messages.getString(MessageIds.GDE_MSGT0269);
			format = TimeLine.TIME_LINE_SEC;
		}
		else if (totalTime_sec > 1) {
			maxTimeNumberFormated = (int) totalTime_msec;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0724) : Messages.getString(MessageIds.GDE_MSGT0269);
			factor = 1000; // 2900 -> 2,9 sec
			format = TimeLine.TIME_LINE_SEC;
		}
		else if (totalTime_msec > 0) {
			maxTimeNumberFormated = (int) totalTime_msec;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0725) : Messages.getString(MessageIds.GDE_MSGT0271);
			factor = 1;
			format = TimeLine.TIME_LINE_MSEC;
		}
		else {
			maxTimeNumberFormated = (int) totalTime_msec;
			this.timeLineText = isTimeFormatAbsolute ? Messages.getString(MessageIds.GDE_MSGT0725) : Messages.getString(MessageIds.GDE_MSGT0264);
			factor = 1;
			format = TimeLine.TIME_LINE_MSEC;
		}
		if (TimeLine.log.isLoggable(Level.FINER)) TimeLine.log.log(Level.FINER, this.timeLineText + "  " + maxTimeNumberFormated); //$NON-NLS-1$

		this.isTimeLinePrepared = true;

		if (TimeLine.log.isLoggable(Level.FINE))
			TimeLine.log.log(Level.FINE, "timeLineText = " + this.timeLineText + " maxTimeNumber = " + maxTimeNumberFormated + " factor = " + factor); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return new int[] { maxTimeNumberFormated, factor, format, (int) totalTime_msec };
	}

	/**
	 * draws the time line - requires to call preparation steps
	 * - x0, y0 defines the start point of the scale, y0, width the nd point
	 * @param recordSet
	 * @param gc graphics context
	 * @param y0 start point in y horizontal direction
	 * @param x0 start point in x vertical direction
	 * @param width in points where the ticks should be drawn
	 * @param startTimeValue  the time value where the scale should start to count
	 * @param endTimeValue depends on the time gap and the unit calculated from the gap
	 * @param timeFormat - TIME_LINE_MSEC, TIME_LINE_SEC, TIME_LINE_SEC_MIN, ..
	 * @param deltaTime_ms the difference in ms between start and end time
	 * @param color
	 */
	public void drawTimeLine(RecordSet recordSet, GC gc, int x0, int y0, int width, int startTimeValue, int endTimeValue, int scaleFactor, int timeFormat, long deltaTime_ms, Color color) {
		if (this.isTimeLinePrepared == false) {
			TimeLine.log.log(Level.WARNING, "isTimeLinePrepared == false -> getScaleMaxTimeNumber(RecordSet recordSet) needs to be called first"); //$NON-NLS-1$
			return;
		}

		// set the line color and draw a horizontal time line
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(color);
		gc.drawLine(x0 - 1, y0, x0 + width + 1, y0);
		if (TimeLine.log.isLoggable(Level.FINER))
			TimeLine.log.log(Level.FINER, String.format("time line - x0=%d y0=%d - width=%d - maxNumber=%d - scaleFactor=%d", x0, y0, width, endTimeValue, timeFormat)); //$NON-NLS-1$

		Point pt = gc.textExtent(this.timeLineText);
		int ticklength = pt.y / 2;
		int gap = pt.y / 3;
		int miniTicks = 4;

		drawTickMarks(recordSet, gc, x0, y0, width, startTimeValue, endTimeValue, scaleFactor, timeFormat, deltaTime_ms, ticklength, miniTicks, gap);

		// draw the scale description centered
		GraphicsUtils.drawTimeLineText(this.timeLineText, (x0 + width / 2), y0 + ticklength + pt.y * 2, gc, SWT.HORIZONTAL);
	}

	/**
	 * draws tick marks to a scale in horizontal direction
	 * @param recordSet
	 * @param gc graphics context
	 * @param y0 start point in y horizontal direction
	 * @param x0 start point in x vertical direction
	 * @param width in points where the ticks should be drawn
	 * @param startTimeValue the time value where the scale should start to count
	 * @param endTimeValue the number where the time scale should end, endTimeValue - startTimeValue -> number of ticks drawn
	 * @param scaleFactor (100 -> deltaTime of 2900 = 2.9)
	 * @param timeFormat - TIME_LINE_MSEC, TIME_LINE_SEC, TIME_LINE_SEC_MIN, ..
	 * @param deltaTime_ms the difference in ms between start and end time
	 * @param ticklength of the main ticks
	 * @param miniticks number of mini ticks drawn between the main ticks
	 * @param gap distance between ticks and the number scale
	 */
	private void drawTickMarks(RecordSet recordSet, GC gc, int x0, int y0, int width, int startTimeValue, int endTimeValue, double scaleFactor, int timeFormat, long deltaTime_ms, int ticklength,
			int miniticks, int gap) {
		Double numberTicks, timeDelta;
		boolean isAbsoluteTime = Settings.getInstance().isTimeFormatAbsolute() && !recordSet.isCompareSet() && !recordSet.isZoomMode();
		long startTimeStamp = recordSet.getStartTimeStamp();
		long offset = 0;
		int timeDeltaValue = endTimeValue - startTimeValue;
		if (TimeLine.log.isLoggable(Level.FINER))
			TimeLine.log.log(Level.FINER, String.format("timeDelta = %s startTime = %s endTime = %s", timeDeltaValue, startTimeValue, endTimeValue)); //$NON-NLS-1$

		if (TimeLine.log.isLoggable(Level.FINER))
			TimeLine.log.log(Level.FINER, String.format("formatedDate = %s", StringHelper.getFormatedTime("YYYY-MM-dd HH:mm:ss.SSS", startTimeStamp))); //$NON-NLS-1$
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(startTimeStamp);
		int totalTime_msec = c.get(Calendar.MILLISECOND);
		int totalTime_sec = c.get(Calendar.SECOND);
		int totalTime_min = c.get(Calendar.MINUTE);
		int totalTime_std = c.get(Calendar.HOUR_OF_DAY);
		int totalTime_days = c.get(Calendar.DAY_OF_MONTH);
		int totalTime_month = c.get(Calendar.MONTH);

		// calculate a scale factor, a big time difference would have to much ticks
		if (timeDeltaValue > 0) {
			switch (timeFormat) {
			case TimeLine.TIME_LINE_SEC:
			case TimeLine.TIME_LINE_SEC_MIN:
				if (isAbsoluteTime) {
					if (totalTime_msec != 0) c.add(Calendar.MILLISECOND, 1000 - totalTime_msec);
					if (TimeLine.log.isLoggable(Level.FINER))
						TimeLine.log.log(Level.FINER, String.format("offsetDate = %s", StringHelper.getFormatedTime("YYYY-MM-dd HH:mm:ss.SSS", (c.getTimeInMillis() - startTimeStamp)))); //$NON-NLS-1$
					offset = c.getTimeInMillis() - startTimeStamp;
				}
				timeDelta = deltaTime_ms / 1000.0;
				break;
			case TimeLine.TIME_LINE_MIN:
			case TimeLine.TIME_LINE_MIN_HRS:
				if (isAbsoluteTime) {
					if (totalTime_msec != 0 && totalTime_sec != 0) {
						c.add(Calendar.MILLISECOND, 1000 - totalTime_msec);
						++totalTime_sec;
					}
					if (totalTime_sec != 0) {
						c.add(Calendar.SECOND, 60 - totalTime_sec);
						++totalTime_min;
					}
					if (TimeLine.log.isLoggable(Level.FINER))
						TimeLine.log.log(Level.FINER, String.format("offsetDate = %s", StringHelper.getFormatedTime("YYYY-MM-dd HH:mm:ss.SSS", (c.getTimeInMillis() - startTimeStamp)))); //$NON-NLS-1$
					offset = c.getTimeInMillis() - startTimeStamp;
				}
				timeDelta = deltaTime_ms / 60000.0;
				break;
			case TimeLine.TIME_LINE_HRS:
			case TimeLine.TIME_LINE_HRS_DAYS:
				if (isAbsoluteTime) {
					if (totalTime_msec != 0 && totalTime_sec != 0) {
						c.add(Calendar.MILLISECOND, 1000 - totalTime_msec);
						++totalTime_sec;
					}
					if (totalTime_sec != 0 && totalTime_min != 0) {
						c.add(Calendar.SECOND, 60 - totalTime_sec);
						++totalTime_min;
					}
					if (totalTime_min != 0) c.add(Calendar.MINUTE, 60 - totalTime_min);
					if (TimeLine.log.isLoggable(Level.FINER))
						TimeLine.log.log(Level.FINER, String.format("offsetDate = %s", StringHelper.getFormatedTime("YYYY-MM-dd HH:mm:ss.SSS", (c.getTimeInMillis())))); //$NON-NLS-1$
					offset = c.getTimeInMillis() - startTimeStamp;
				}
				timeDelta = deltaTime_ms / 3600000.0;
				break;
			case TimeLine.TIME_LINE_DAYS:
			case TimeLine.TIME_LINE_DAYS_MONTH:
				if (isAbsoluteTime) {
					if (totalTime_msec != 0 && totalTime_sec != 0) {
						c.add(Calendar.MILLISECOND, 1000 - totalTime_msec);
						++totalTime_sec;
					}
					if (totalTime_sec != 0 && totalTime_min != 0) {
						c.add(Calendar.SECOND, 60 - totalTime_sec);
						++totalTime_min;
					}
					if (totalTime_min != 0 && totalTime_std != 0) {
						c.add(Calendar.MINUTE, 60 - totalTime_min);
						++totalTime_std;
					}
					if (totalTime_std != 0) c.add(Calendar.HOUR_OF_DAY, 24 - totalTime_std);
					offset = c.getTimeInMillis() - startTimeStamp;
				}
				timeDelta = deltaTime_ms / 3600000.0 / 24;
				break;
			case TimeLine.TIME_LINE_MONTH:
			case TimeLine.TIME_LINE_MONTH_YEARS:
				if (isAbsoluteTime) {
					if (totalTime_msec != 0 && totalTime_sec != 0) {
						c.add(Calendar.MILLISECOND, 1000 - totalTime_msec);
						++totalTime_sec;
					}
					if (totalTime_sec != 0 && totalTime_min != 0) {
						c.add(Calendar.SECOND, 60 - totalTime_sec);
						++totalTime_min;
					}
					if (totalTime_min != 0 && totalTime_std != 0) {
						c.add(Calendar.MINUTE, 60 - totalTime_min);
						++totalTime_std;
					}
					if (totalTime_std != 0) {
						c.add(Calendar.HOUR_OF_DAY, 24 - totalTime_std);
						++totalTime_days;
					}
					if (totalTime_days != 0) c.add(Calendar.DAY_OF_MONTH, 30 - totalTime_days);
					offset = c.getTimeInMillis() - startTimeStamp;
				}
				timeDelta = deltaTime_ms / 3600000.0 / 24 / 30;
				break;
			case TimeLine.TIME_LINE_YEARS:
				if (isAbsoluteTime) {
					if (totalTime_msec != 0) {
						c.add(Calendar.MILLISECOND, 1000 - totalTime_msec);
						++totalTime_sec;
					}
					if (totalTime_sec != 0) {
						c.add(Calendar.SECOND, 60 - totalTime_sec);
						++totalTime_min;
					}
					if (totalTime_min != 0) {
						c.add(Calendar.MINUTE, 60 - totalTime_min);
						++totalTime_std;
					}
					if (totalTime_std != 0) {
						c.add(Calendar.HOUR_OF_DAY, 24 - totalTime_std);
						++totalTime_days;
					}
					if (totalTime_days != 0) {
						c.add(Calendar.DAY_OF_MONTH, 30 - totalTime_days);
						++totalTime_month;
					}
					if (totalTime_month != 0) c.add(Calendar.MONTH, 12 - totalTime_month);
					offset = c.getTimeInMillis() - startTimeStamp;
				}
				timeDelta = deltaTime_ms / 3600000.0 / 24 / 30 / 365;
				break;
			case TimeLine.TIME_LINE_MSEC:
			default:
				timeDelta = deltaTime_ms * 1.0;
				break;
			}

			if (TimeLine.log.isLoggable(Level.FINE)) TimeLine.log.log(Level.FINE, "width/timeDeltaValue = " + width / timeDeltaValue);
			switch (timeFormat * (int) scaleFactor) { // TIME_LINE_MSEC, TIME_LINE_SEC, TIME_LINE_SEC_MIN, ..
			case TimeLine.TIME_LINE_MSEC * 1:
				if (timeDeltaValue <= width * 2) {
					numberTicks = timeDelta / 100.0; // every 1'th units one tick
					scaleFactor = 1;
				}
				else {
					numberTicks = timeDelta / 200.0; // every 2'th units one tick
					scaleFactor = 0.5;
				}
				break;

			case TimeLine.TIME_LINE_SEC * 100:
				if (timeDeltaValue <= width * 10) {
					numberTicks = timeDelta / 1000.0; // every 1'th units one tick
					scaleFactor = 100;
				}
				else {
					numberTicks = timeDelta / 500.0; // every 2'th units one tick
					scaleFactor = 200;
				}
				break;

			case TimeLine.TIME_LINE_SEC * 1000:
				if (timeDeltaValue <= width * 10) {
					numberTicks = timeDelta / 0.5; // every 1'th units one tick
					scaleFactor = 200;
				}
				else {
					numberTicks = isAbsoluteTime ? timeDelta / 2.0 : timeDelta / 1.0; // every 2'th units one tick
					scaleFactor = 100;
				}
				break;

			default:
			case TimeLine.TIME_LINE_SEC * 10:
			case TimeLine.TIME_LINE_MIN * 10:
			case TimeLine.TIME_LINE_HRS * 10:
			case TimeLine.TIME_LINE_DAYS * 10:
			case TimeLine.TIME_LINE_MONTH * 10:
				if (timeDeltaValue > 0 && timeDeltaValue <= width / 100) {
					numberTicks = isAbsoluteTime ? timeDelta : timeDelta * 2.0; // every 0.5'th units one tick
					scaleFactor = scaleFactor * 20.0;
				}
				else if (timeDeltaValue > width / 100 && timeDeltaValue <= width / 70) {
					numberTicks = isAbsoluteTime ? timeDelta / 2.0 : timeDelta; // every 1'th units one tick
					scaleFactor = scaleFactor * 10.0;
				}
				else if (timeDeltaValue > width / 70 && timeDeltaValue <= width / 25) {
					numberTicks = isAbsoluteTime ? timeDelta / 3.0 : timeDelta / 2.5; // every 2.5 th units one tick
					scaleFactor = scaleFactor * 4.0;
				}
				else if (timeDeltaValue > width / 25 && timeDeltaValue < width / 10) {
					numberTicks = isAbsoluteTime ? timeDelta / 6.0 : timeDelta / 5.0; // every 5 th units one tick
					scaleFactor = scaleFactor * 2.0;
				}
				else {
					numberTicks = isAbsoluteTime ? timeDelta / 12.0 : timeDelta / 10.0; // every 10th units one tick
				}
				break;

			case TimeLine.TIME_LINE_SEC_MIN * 10:
			case TimeLine.TIME_LINE_MIN_HRS * 10:
				if (timeDeltaValue >= 0 && timeDeltaValue <= 30) {
					numberTicks = isAbsoluteTime ? timeDelta / 3.0 : timeDelta / 1.0; // every 2.5 th units one tick
					scaleFactor = scaleFactor * 4.0;
				}
				else if (timeDeltaValue > width / 50 && timeDeltaValue <= width / 25) {
					numberTicks = isAbsoluteTime ? timeDelta / 6.0 : timeDelta / 2.5; // every 5 th units one tick
					scaleFactor = scaleFactor * 4.0;
				}
				else if (timeDeltaValue > width / 25 && timeDeltaValue <= width / 8) {
					numberTicks = isAbsoluteTime ? timeDelta / 12.0 : timeDelta / 5.0; // every 5 th units one tick
					scaleFactor = scaleFactor * 2.0;
				}
				else if (timeDeltaValue >= width / 8 && timeDeltaValue <= width / 4) {
					numberTicks = isAbsoluteTime ? timeDelta / 25.0 : timeDelta / 10.0; // every 10 th units one tick
					scaleFactor = scaleFactor * 1.0;
				}
				else if (timeDeltaValue >= width / 4 && timeDeltaValue <= width / 2) {
					numberTicks = isAbsoluteTime ? timeDelta / 50.0 : timeDelta / 20.0; // every 20 th units one tick
					scaleFactor = scaleFactor / 2.0;
				}
				else if (timeDeltaValue >= width / 2 && timeDeltaValue <= width) {
					numberTicks = isAbsoluteTime ? timeDelta / 100.0 : timeDelta / 40.0; // every 20 th units one tick
					scaleFactor = scaleFactor / 4.0;
				}
				else {
					numberTicks = isAbsoluteTime ? timeDelta / 150.0 : timeDelta / 80.0; // every 20 th units one tick
					scaleFactor = scaleFactor / 8.0;
				}
				break;

			case TimeLine.TIME_LINE_HRS_DAYS * 10:
				if (timeDeltaValue >= 0 && timeDeltaValue <= 30) {
					numberTicks = isAbsoluteTime ? timeDelta / 5.0 : timeDelta / 2.5; // every 2.5 th units one tick
					scaleFactor = scaleFactor * 4.0;
				}
				else if (timeDeltaValue > width / 25 && timeDeltaValue <= width / 7) {
					numberTicks = isAbsoluteTime ? timeDelta / 7.0 : timeDelta / 5.0; // every 5 th units one tick
					scaleFactor = scaleFactor * 2.0;
				}
				else if (timeDeltaValue >= width / 7 && timeDeltaValue <= width / 1.5) {
					numberTicks = isAbsoluteTime ? timeDelta / 14.0 : timeDelta / 10.0; // every 10 th units one tick
					scaleFactor = scaleFactor * 1.0;
				}
				else {
					numberTicks = isAbsoluteTime ? timeDelta / 30.0 : timeDelta / 20.0; // every 20 th units one tick
					scaleFactor = scaleFactor / 2.0;
				}
				break;

			case TimeLine.TIME_LINE_DAYS_MONTH * 10:
				if (width / timeDeltaValue >= 20) {
					numberTicks = isAbsoluteTime ? timeDelta / 3.0 : timeDelta / 2.0; // every 2.5 th units one tick
					scaleFactor = scaleFactor * 5.0;
				}
				else if (width / timeDeltaValue >= 7) {
					numberTicks = isAbsoluteTime ? timeDelta / 6.0 : timeDelta / 5.0; // every 5 th units one tick
					scaleFactor = scaleFactor * 2.0;
				}
				else if (width / timeDeltaValue >= 3) {
					numberTicks = isAbsoluteTime ? timeDelta / 12.0 : timeDelta / 10.0; // every 10 th units one tick
					scaleFactor = scaleFactor * 1.0;
				}
				else {
					numberTicks = isAbsoluteTime ? timeDelta / 60.0 : timeDelta / 30.0; // every 20 th units one tick
					scaleFactor = scaleFactor / 3.0;
				}
				break;

			case TimeLine.TIME_LINE_MONTH_YEARS * 10:
				if (timeDeltaValue >= width / 6) {
					numberTicks = isAbsoluteTime ? timeDelta / 6.0 : timeDelta / 4.0; // every 2'th units one tick
					scaleFactor = scaleFactor * 2.5;
				}
				else if (timeDeltaValue >= width / 15) {
					numberTicks = isAbsoluteTime ? timeDelta / 3.0 : timeDelta / 2.0; // every 2'th units one tick
					scaleFactor = scaleFactor * 5.0;
				}
				else if (timeDeltaValue > width / 60) {
					numberTicks = isAbsoluteTime ? timeDelta / 2.0 : timeDelta; // 1 ticks every unit
					scaleFactor = scaleFactor * 10.0;
				}
				else {
					numberTicks = isAbsoluteTime ? timeDelta : timeDelta * 2; // 2 ticks every units
					scaleFactor = scaleFactor * 20.0;
				}
				break;

			case TimeLine.TIME_LINE_YEARS * 10:
				if (timeDeltaValue >= width / 6) {
					numberTicks = isAbsoluteTime ? timeDelta / 4.0 : timeDelta * 3.75; // every 2'th units one tick
					scaleFactor = scaleFactor * 1.25;
				}
				else if (timeDeltaValue >= width / 15) {
					numberTicks = isAbsoluteTime ? timeDelta / 12.0 : timeDelta * 7.5; // every 3'th units one tick
					scaleFactor = scaleFactor * 2.5;
				}
				else if (timeDeltaValue > width / 60) {
					numberTicks = isAbsoluteTime ? timeDelta / 24.0 : timeDelta * 15; // every 2'th units one tick
					scaleFactor = scaleFactor * 5.0;
				}
				else {
					numberTicks = isAbsoluteTime ? timeDelta / 48.0 : timeDelta * 30; // 1 ticks every unit
					scaleFactor = scaleFactor * 10.0;
				}
				break;

			}
			if (TimeLine.log.isLoggable(Level.FINER))
				TimeLine.log.log(Level.FINER, "timeFormat = " + timeFormat + " numberTicks = " + numberTicks + " startTimeValue = " + startTimeValue + " endTimeValue = " + endTimeValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			double deltaTick = width / numberTicks;
			if (!isAbsoluteTime) miniticks++;

			// calculate the space required to draw the time values
			Point pt = gc.textExtent("00"); //$NON-NLS-1$

			// prepare grid vector
			Vector<Integer> timeGrid = new Vector<Integer>();
			boolean isBuildGridVector = recordSet.getTimeGridType() == RecordSet.TIME_GRID_MAIN;

			for (int i = 0; isAbsoluteTime ? i < numberTicks : i <= numberTicks; i++) { // <= end of time scale tick
				//draw the main scale ticks, length = 5 and gap to scale = 2
				double xTickPosition = offset != 0 ? 1.0 * width * offset / deltaTime_ms + x0 + i * deltaTick : x0 + i * deltaTick;
				int intXTickPosition = (int) (xTickPosition + 0.5);
				if (intXTickPosition > x0 + width) break;
				if (isBuildGridVector) timeGrid.add(intXTickPosition);
				gc.drawLine(intXTickPosition, y0, intXTickPosition, y0 + ticklength);

				//draw the sub ticks to the scale according number of miniTicks
				double deltaPosMini = deltaTick / miniticks;
				for (int j = 1; j < miniticks && i < numberTicks; j++) {
					double xMiniTickPos = (xTickPosition + j * deltaPosMini);
					int intMiniTickPos = (int) xMiniTickPos;
					if (TimeLine.log.isLoggable(Level.FINEST)) TimeLine.log.log(Level.FINEST, "intXTickPosition=" + intXTickPosition + ", width=" + width); //$NON-NLS-1$ //$NON-NLS-2$
					if (intMiniTickPos < (x0 + width)) {
						gc.drawLine(intMiniTickPos, y0, intMiniTickPos, y0 + ticklength / 2);
					}
				}
				//draw values to time line
				if (isAbsoluteTime)
					drawValues2Scale(gc, x0, y0, width, timeFormat, deltaTime_ms, ticklength, gap, pt, timeGrid, startTimeStamp, i, xTickPosition, intXTickPosition);
				else
					drawValues2Scale(recordSet, gc, y0, scaleFactor, timeFormat, ticklength, gap, pt, timeGrid, i, intXTickPosition);
			}
			recordSet.setTimeGrid(timeGrid);
		}
	}

	/**
	 * draw time line values with relative time (0, 1, 2, ...)
	 * @param recordSet
	 * @param gc
	 * @param y0
	 * @param scaleFactor
	 * @param timeFormat
	 * @param ticklength
	 * @param gap
	 * @param pt
	 * @param timeGrid
	 * @param i
	 * @param intXTickPosition
	 */
	private void drawValues2Scale(RecordSet recordSet, GC gc, int y0, double scaleFactor, int timeFormat, int ticklength, int gap, Point pt, Vector<Integer> timeGrid, int i, int intXTickPosition) {
		final double timeValue = i * 100.0 / scaleFactor;
		if (TimeLine.log.isLoggable(Level.FINER)) TimeLine.log.log(Level.FINER, "timeValue = " + timeValue); //$NON-NLS-1$
		// prepare to make every minute or hour to bold
		String numberStr;
		switch (timeFormat) {
		case TimeLine.TIME_LINE_SEC_MIN: // 60 sec/min
		case TimeLine.TIME_LINE_MIN_HRS: // 60 min/hrs
			boolean isMod60 = (timeValue % 60) == 0;
			double timeValue60 = isMod60 ? timeValue / 60 : timeValue % 60; // minute, hour
			if (TimeLine.log.isLoggable(Level.FINER)) TimeLine.log.log(Level.FINER, "timeValue = " + timeValue + ", timeValue60 = " + timeValue60); //$NON-NLS-1$ //$NON-NLS-2$
			numberStr = (timeValue60 % 1 == 0 || isMod60) ? String.format("%.0f", timeValue60) : String.format("%.1f", timeValue60); //$NON-NLS-1$ //$NON-NLS-2$
			if (isMod60 && timeValue > 0.0) {
				gc.setFont(SWTResourceManager.getFont(gc, SWT.BOLD));
				if (i != 0 && recordSet.getTimeGridType() == RecordSet.TIME_GRID_MOD60) timeGrid.add(intXTickPosition);
				GraphicsUtils.drawTextCentered(numberStr, intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
				gc.setFont(SWTResourceManager.getFont(gc, SWT.NORMAL));
			}
			else {
				GraphicsUtils.drawTextCentered(numberStr, intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
			}
			break;

		case TimeLine.TIME_LINE_MONTH_YEARS: // 12 month/year
			boolean isMod12 = (timeValue % 12) == 0;
			double timeValue12 = isMod12 ? timeValue / 12 : timeValue % 12; // minute, hour
			if (TimeLine.log.isLoggable(Level.FINER)) TimeLine.log.log(Level.FINER, "timeValue = " + timeValue + ", timeValue12 = " + timeValue12); //$NON-NLS-1$ //$NON-NLS-2$
			numberStr = (timeValue12 % 1 == 0 || isMod12) ? String.format("%.0f", timeValue12) : String.format("%.1f", timeValue12); //$NON-NLS-1$ //$NON-NLS-2$
			if (isMod12 && timeValue > 0.0) {
				gc.setFont(SWTResourceManager.getFont(gc, SWT.BOLD));
				if (i != 0 && recordSet.getTimeGridType() == RecordSet.TIME_GRID_MOD60) timeGrid.add(intXTickPosition);
				GraphicsUtils.drawTextCentered(numberStr, intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
				gc.setFont(SWTResourceManager.getFont(gc, SWT.NORMAL));
			}
			else {
				GraphicsUtils.drawTextCentered(numberStr, intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
			}
			break;

		case TimeLine.TIME_LINE_DAYS_MONTH: // 30 days/month
			boolean isMod30 = (timeValue % 30) == 0;
			double timeValue30 = isMod30 ? timeValue / 30 : timeValue % 30; // day, hour
			if (TimeLine.log.isLoggable(Level.FINER)) TimeLine.log.log(Level.FINER, "timeValue = " + timeValue + ", timeValue30 = " + timeValue30); //$NON-NLS-1$ //$NON-NLS-2$
			numberStr = (timeValue30 % 1 == 0 || isMod30) ? String.format("%.0f", timeValue30) : String.format("%.1f", timeValue30); //$NON-NLS-1$ //$NON-NLS-2$
			if (isMod30 && timeValue > 0.0) {
				gc.setFont(SWTResourceManager.getFont(gc, SWT.BOLD));
				if (i != 0 && recordSet.getTimeGridType() == RecordSet.TIME_GRID_MOD60) timeGrid.add(intXTickPosition);
				GraphicsUtils.drawTextCentered(numberStr, intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
				gc.setFont(SWTResourceManager.getFont(gc, SWT.NORMAL));
			}
			else {
				GraphicsUtils.drawTextCentered(numberStr, intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
			}
			break;

		case TimeLine.TIME_LINE_HRS_DAYS: // 24 hrs/day
			boolean isMod24 = (timeValue % 24) == 0;
			double timeValue24 = isMod24 ? timeValue / 24 : timeValue % 24; // 24 hours/day
			if (TimeLine.log.isLoggable(Level.FINER)) TimeLine.log.log(Level.FINER, "timeValue = " + timeValue + ", timeValue24 = " + timeValue24); //$NON-NLS-1$ //$NON-NLS-2$
			numberStr = (timeValue24 % 1 == 0 || isMod24) ? String.format("%.0f", timeValue24) : String.format("%.1f", timeValue24); //$NON-NLS-1$ //$NON-NLS-2$
			if (isMod24 && timeValue > 0.0) {
				gc.setFont(SWTResourceManager.getFont(gc, SWT.BOLD));
				if (i != 0 && recordSet.getTimeGridType() == RecordSet.TIME_GRID_MOD60) timeGrid.add(intXTickPosition);
				GraphicsUtils.drawTextCentered(numberStr, intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
				gc.setFont(SWTResourceManager.getFont(gc, SWT.NORMAL));
			}
			else {
				GraphicsUtils.drawTextCentered(numberStr, intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
			}
			break;

		default:
			GraphicsUtils.drawTextCentered(("" + timeValue), intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL); //$NON-NLS-1$
			break;
		}
	}

	/**
	 * draw time line value with absolute time (10:20, 10:40; ..)
	 * @param gc
	 * @param x0
	 * @param y0
	 * @param width
	 * @param timeFormat
	 * @param deltaTime_ms
	 * @param ticklength
	 * @param gap
	 * @param numberTicks
	 * @param pt
	 * @param timeGrid
	 * @param startTimeStamp
	 * @param i
	 * @param xTickPosition
	 * @param intXTickPosition
	 */
	private void drawValues2Scale(GC gc, int x0, int y0, int width, int timeFormat, long deltaTime_ms, int ticklength, int gap, Point pt, Vector<Integer> timeGrid, long startTimeStamp, int i,
			double xTickPosition, int intXTickPosition) {
		final Double timeValue = deltaTime_ms * (xTickPosition - x0) / width + startTimeStamp;
		if (TimeLine.log.isLoggable(Level.FINER))
			TimeLine.log.log(Level.FINER, String.format("formatedDate = %s ", StringHelper.getFormatedTime("YYYY-MM-dd HH:mm:ss.SSS", timeValue.longValue()))); //$NON-NLS-1$
		// prepare to make every minute or hour to bold
		switch (timeFormat) {
		case TimeLine.TIME_LINE_MSEC: // 1000 sec/min
			GraphicsUtils.drawTextCentered(LocalizedDateTime.getFormatedTime(DateTimePattern.ssSSS, timeValue.longValue()), intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
			break;

		case TimeLine.TIME_LINE_SEC: // 60 sec/min
			GraphicsUtils.drawTextCentered(LocalizedDateTime.getFormatedTime(DateTimePattern.mmssSSS, timeValue.longValue()), intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
			break;

		default:
		case TimeLine.TIME_LINE_SEC_MIN:
		case TimeLine.TIME_LINE_MIN:
		case TimeLine.TIME_LINE_MIN_HRS:
			GraphicsUtils.drawTextCentered(LocalizedDateTime.getFormatedTime(DateTimePattern.HHmmss, timeValue.longValue()), intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
			break;

		case TimeLine.TIME_LINE_HRS:
			GraphicsUtils.drawTextCentered(LocalizedDateTime.getFormatedTime(DateTimePattern.dd_HHmm, timeValue.longValue()), intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
			break;

		case TimeLine.TIME_LINE_DAYS: // 30 days/month
		case TimeLine.TIME_LINE_DAYS_MONTH: // 30 days/month
			GraphicsUtils.drawTextCentered(LocalizedDateTime.getFormatedTime(DateTimePattern.MMdd_HH, timeValue.longValue()), intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
			break;

		case TimeLine.TIME_LINE_MONTH: // 30 days/month
		case TimeLine.TIME_LINE_MONTH_YEARS: // 12 month/year
			GraphicsUtils.drawTextCentered(LocalizedDateTime.getFormatedTime(DateTimePattern.yyMMdd, timeValue.longValue()), intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
			break;
		}
	}

	/**
	 * converts a given time in m_sec into time format used for time scale
	 * @return converted time value
	 */
	public static int convertTimeInFormatNumber(double time_ms, int timeFormat) {
		int time_sec = (int) (time_ms / 1000.0);
		int time_min = (int) TimeUnit.MINUTES.convert(time_sec, TimeUnit.SECONDS);
		int time_std = (int) TimeUnit.HOURS.convert(time_sec, TimeUnit.SECONDS);
		int time_days = (int) TimeUnit.DAYS.convert(time_sec, TimeUnit.SECONDS);
		int time_month = (int) TimeUnit.DAYS.convert(time_sec, TimeUnit.SECONDS) / 30;
		int time_year = (int) TimeUnit.DAYS.convert(time_sec, TimeUnit.SECONDS) / 365;
		if (TimeLine.log.isLoggable(Level.FINE))
			TimeLine.log.log(Level.FINE, "time_std = " + time_std + "; time_min = " + time_min + "; time_sec = " + time_sec + "; time_ms = " + time_ms); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		int result;

		switch (timeFormat) {
		case TIME_LINE_YEARS:
			result = time_year;
			break;
		case TIME_LINE_MONTH_YEARS:
		case TIME_LINE_MONTH:
			result = time_month;
			break;
		case TIME_LINE_DAYS_MONTH:
		case TIME_LINE_DAYS:
			result = time_days;
			break;
		case TIME_LINE_HRS_DAYS:
		case TIME_LINE_HRS:
			result = time_std;
			break;
		case TIME_LINE_MIN_HRS:
		case TIME_LINE_MIN:
			result = time_min;
			break;
		case TIME_LINE_SEC_MIN:
		case TIME_LINE_SEC:
			result = time_sec;
			break;
		default: // TIME_LINE_MSEC
			result = (int) time_ms;
			break;
		}
		return result;
	}

	/**
	 * get the formatted time of time value in m_sec, if hours are 0 or minutes are 0 the string will be cut of
	 * unit string [HH:mm:ss:SSS] will be appended accordingly
	 * @param milliSeconds
	 * @return string of time value in simple date format HH:mm:ss:SSS
	 */
	public static String getFomatedTimeWithUnit(final double milliSeconds) {
		String time = "0"; //$NON-NLS-1$
		if (milliSeconds >= 0) {
			long lSeconds = (long) (milliSeconds / 1000.0);
			long lMinutes = lSeconds / 60;
			lSeconds %= 60;
			long lHours = lMinutes / 60;
			lMinutes %= 60;

			if (lMinutes == 0 && lHours == 0)
				time = String.format("%s [ss:SSS]", LocalizedDateTime.getFormatedTime(DateTimePattern.ssSSS, (long) milliSeconds)); //$NON-NLS-1$
			else if (lHours == 0)
				time = String.format("%s [mm:ss:SSS]", LocalizedDateTime.getFormatedTime(DateTimePattern.mmssSSS, (long) milliSeconds)); //$NON-NLS-1$
			else
				time = String.format("%s [HH:mm:ss:SSS]", LocalizedDateTime.getFormatedTime(DateTimePattern.HHmmssSSS, (long) milliSeconds)); //$NON-NLS-1$
		}
		return time;
	}

	/**
	 * get the formatted time of time value in m_sec, if hours are 0 or minutes are 0 the string will be cut of
	 * no unit string [HH:mm:ss:SSS] will be appended
	 * @param milliSeconds
	 * @return string of time value in simple date format HH:mm:ss:SSS
	 */
	public static String getFomatedTime(double milliSeconds) {
		String time = "0"; //$NON-NLS-1$
		if (milliSeconds >= 0) {
			long lSeconds = (long) (milliSeconds / 1000.0);
			//milliSeconds %= 1000;
			long lMinutes = lSeconds / 60;
			lSeconds %= 60;
			long lHours = lMinutes / 60;
			lMinutes %= 60;

			if (lMinutes == 0 && lHours == 0)
				time = LocalizedDateTime.getFormatedDuration(DurationPattern.ss_SSS, (long) milliSeconds);
			else if (lHours == 0)
				time = LocalizedDateTime.getFormatedDuration(DurationPattern.mm_ss_SSS, (long) milliSeconds);
			else {
				time = LocalizedDateTime.getFormatedDuration(DurationPattern.HH_mm_ss_SSS, (long) milliSeconds);
				if (Integer.parseInt(time.substring(0, time.indexOf(GDE.CHAR_COLON))) != lHours) time = String.format("%02d%s", lHours, time.substring(time.indexOf(GDE.CHAR_COLON)));
			}
		}
		return time;
	}

}
