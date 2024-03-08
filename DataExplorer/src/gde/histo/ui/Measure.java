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

import org.eclipse.swt.graphics.Rectangle;

import gde.GDE;
import gde.device.resource.DeviceXmlResource;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordFormatter;
import gde.histo.recordings.TrailRecordSection;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.utils.HistoTimeLine;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.LocalizedDateTime;
import gde.utils.LocalizedDateTime.DateTimePattern;

/**
 * Measuring data object.
 * For data exchange between the window, the window composites and measuring objects.
 * Supports delta measuring with two timestamps or simple measuring with identical timestamps.
 */
public class Measure {
	private final static String							$CLASS_NAME	= Measure.class.getName();
	private final static Logger							log					= Logger.getLogger($CLASS_NAME);

	private static final DeviceXmlResource	xmlResource	= DeviceXmlResource.getInstance();

	/**
	 * Standard drawing area for initializing the measuring timestamps.
	 */
	private final static Rectangle					BOUNDS			= new Rectangle(0, 0, 999, 99);

	final boolean														isDeltaMeasure;
	final TrailRecord												measureRecord;
	private long														timestampMeasure_ms;
	private long														timestampDelta_ms;
	private TrailRecordSection							recordSection;

	Measure(boolean isDeltaMeasuring, TrailRecord measuringRecord) {
		this.isDeltaMeasure = isDeltaMeasuring;
		this.measureRecord = measuringRecord;
		long[] timestamps_ms = defineInitialTimestamps_ms();
		if (timestamps_ms.length == 0) throw new IllegalArgumentException();
		setTimestampMeasure_ms(timestamps_ms[0]);
		setTimestampDelta_ms(isDeltaMeasuring ? timestamps_ms[1] : timestamps_ms[0]);
	}

	/**
	 * Selects indices with nonNull values.
	 * @return the upper and the lower initial timestamp
	 */
	private long[] defineInitialTimestamps_ms() { // todo find a cheaper solution than initializing a dummy time line
		HistoTimeLine timeLine = new HistoTimeLine();
		TrailRecordSet recordSet = measureRecord.getParent();
		timeLine.initialize(recordSet, BOUNDS);
		if (timeLine.getScalePositions().size() > 0) {
			int margin = BOUNDS.width / (timeLine.getScalePositions().size() + 1);
			long timestampMeasureNew_ms = timeLine.getAdjacentTimestamp(margin);
			// long timestampDeltaNew_ms = hgc.timeLine.getAdjacentTimestamp(hgc.curveAreaBounds.width - margin);
			long timestampDeltaNew_ms = timeLine.getAdjacentTimestamp(BOUNDS.width * 2 / 3);
			return new long[] { timestampMeasureNew_ms, timestampDeltaNew_ms };
		} else {
			return new long[0];
		}
	}

	/**
	 * Set the timestamp of the first non null value by searching in the proposed and earlier timestamps
	 */
	void setValidMeasureTimeStamp_ms(long proposedTimeStamp_ms) {
		long timestampMeasureNew_ms = proposedTimeStamp_ms;
		int index = measureRecord.getParent().getIndex(proposedTimeStamp_ms);
		Integer value = measureRecord.getPoints().elementAt(index);
		if (value == null) {
			log.fine(() -> String.format("timestampMeasure_ms=%d search first non-null value from the left", proposedTimeStamp_ms));
			int i = -1;
			while (value == null) {
				value = measureRecord.getPoints().elementAt(++i);
			}
			timestampMeasureNew_ms = measureRecord.getParent().getDisplayTimeStamp_ms(i);
		}
		setTimestampMeasure_ms(timestampMeasureNew_ms);
	}

	/**
	 * Set the timestamp of the first non null value by searching in the proposed and later timestamps
	 */
	void setValidDeltaTimeStamp_ms(long proposedTimeStamp_ms) {
		long timestampDeltaNew_ms = proposedTimeStamp_ms;
		int index = measureRecord.getParent().getIndex(proposedTimeStamp_ms);
		Integer value = measureRecord.getPoints().elementAt(index);
		if (value == null) {
			log.fine(() -> String.format("timestampDelta_ms=%d search first non-null value from the right", proposedTimeStamp_ms));
			int i = measureRecord.getParent().getTimeStepSize();
			while (value == null) {
				value = measureRecord.getPoints().elementAt(--i);
			}
			timestampDeltaNew_ms = measureRecord.getParent().getDisplayTimeStamp_ms(i);
		}
		setTimestampDelta_ms(timestampDeltaNew_ms);
	}

	public String getCurveSurveyStatusMessage() {
		String deltaText = getRecordSection().getFormattedBoundsDelta();
		String unitText = measureRecord.getUnit();
		String avgText = getRecordSection().getFormattedBoundsAvg();
		String slopeText = getRecordSection().getFormattedBoundsSlope();
		String recordName = xmlResource.getReplacement(measureRecord.getName());
		return Messages.getString(MessageIds.GDE_MSGT0848, new Object[] { recordName, unitText, deltaText,
				LocalizedDateTime.getFormatedDistance(timestampMeasure_ms, timestampDelta_ms) }) + Messages.getString(MessageIds.GDE_MSGT0879, new Object[] {
						unitText, avgText, unitText, slopeText });
	}

	public String getDeltaStandardStatusMessage() {
		String deltaText = getRecordSection().getFormattedBoundsDelta();
		String unitText = measureRecord.getUnit();
		String recordName = xmlResource.getReplacement(measureRecord.getName());
		return Messages.getString(MessageIds.GDE_MSGT0848, new Object[] { recordName, unitText, deltaText,
				LocalizedDateTime.getFormatedDistance(timestampMeasure_ms, timestampDelta_ms) });
	}

	public String getNoDeltaCurveSurveyStatusMessage() {
		String deltaText = GDE.STRING_STAR;
		String unitText = measureRecord.getUnit();
		String avgText = getRecordSection().getFormattedBoundsAvg();
		String slopeText = getRecordSection().getFormattedBoundsSlope();
		String recordName = xmlResource.getReplacement(measureRecord.getName());
		return Messages.getString(MessageIds.GDE_MSGT0848, new Object[] { recordName, deltaText, unitText,
				LocalizedDateTime.getFormatedDistance(timestampMeasure_ms, timestampDelta_ms) }) + Messages.getString(MessageIds.GDE_MSGT0879, new Object[] {
						unitText, avgText, unitText, slopeText });
	}

	public String getNoRecordsStatusMessage() {
		String recordName = xmlResource.getReplacement(measureRecord.getName());
		return Messages.getString(MessageIds.GDE_MSGT0848, new Object[] { recordName, GDE.STRING_STAR });
	}

	public String getMeasureStatusMessage() {
		String recordName = xmlResource.getReplacement(measureRecord.getName());
		return Messages.getString(MessageIds.GDE_MSGT0256, new Object[] { recordName, new TrailRecordFormatter(
				measureRecord).getMeasureValue(measureRecord.getParent().getIndex(timestampMeasure_ms)), measureRecord.getUnit(),
				LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmss, timestampMeasure_ms) });
	}

	public TrailRecordSection getRecordSection() {
		if (recordSection == null) setRecordSection();
		return recordSection;
	}

	private void setRecordSection() {
		recordSection = new TrailRecordSection(measureRecord, timestampMeasure_ms, timestampDelta_ms);
	}

	public long getTimestampMeasure_ms() {
		return timestampMeasure_ms;
	}

	public void setTimestampMeasure_ms(long timestampMeasure_ms) {
		recordSection = null;
		this.timestampMeasure_ms = timestampMeasure_ms;
		if (!isDeltaMeasure) this.timestampDelta_ms = timestampMeasure_ms;
	}

	public long getTimestampDelta_ms() {
		return timestampDelta_ms;
	}

	public void setTimestampDelta_ms(long timestampDelta_ms) {
		recordSection = null;
		this.timestampDelta_ms = timestampDelta_ms;
	}

	@Override
	public String toString() {
		return "[isDeltaMeasure=" + isDeltaMeasure + ", timestampMeasure_ms=" + timestampMeasure_ms + ", timestampDelta_ms=" + timestampDelta_ms + ", recordSection=" + getRecordSection() + ", measureRecord=" + measureRecord + " ]";
	}

	public boolean isDeltaMeasure() {
		return this.isDeltaMeasure;
	}

}