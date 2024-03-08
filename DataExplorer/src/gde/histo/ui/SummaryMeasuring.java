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
import org.eclipse.swt.graphics.Rectangle;

import gde.config.Settings;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.ui.data.SummarySpots;
import gde.histo.ui.data.SummarySpots.Density;
import gde.log.Logger;
import gde.utils.ColorUtils;

/**
 * Summary measuring activity mapper for mouse movements and other UI actions from menus, checkboxes etc.
 * @author Thomas Eickert (USER)
 */
public final class SummaryMeasuring extends AbstractMeasuring {
	private final static String			$CLASS_NAME					= SummaryMeasuring.class.getName();
	private final static Logger			log									= Logger.getLogger($CLASS_NAME);

	private final Settings					settings						= Settings.getInstance();

	boolean													isLeftMouseMeasure	= false;
	boolean													isRightMouseMeasure	= false;

	private final SummaryComposite	summaryComposite;
	private final CurveSurvey				curveSurvey;

	private GC											canvasGC;

	public SummaryMeasuring(SummaryComposite summaryComposite, Measure measure) {
		super(measure);
		this.summaryComposite = summaryComposite;

		this.curveSurvey = null;
	}

	/**
	 * Draw a refreshed measurement.
	 */
	@Override
	public void drawMeasuring(GC canvasGC) {
		drawMeasuring(canvasGC, measure.getTimestampMeasure_ms(), measure.getTimestampDelta_ms());
	}

	/**
	 * Draw a new measurement based on the timestamp values.
	 */
	private void drawMeasuring(GC canvasGC, long timestampMeasureNew_ms, long timestampDeltaNew_ms) {
		// set the gc properties
		//this.canvasGC = new GC(this.summaryComposite.graphicCanvas);
		canvasGC.setForeground(ColorUtils.getColor(measure.measureRecord.getRGB()));

		measure.setTimestampMeasure_ms(timestampMeasureNew_ms);
		measure.setTimestampDelta_ms(timestampDeltaNew_ms);

		drawModeMeasurement(canvasGC);
		//this.canvasGC.dispose();
	}

	private void drawModeMeasurement(GC canvasImageGC) { // todo move this into a new class analogous to CurveSurvey
		AbstractChartComposite hgc = summaryComposite;
		boolean isSummarySpotsVisible = settings.isSummarySpotsVisible();
		if (!isSummarySpotsVisible) return;

		boolean isPartialDataTable = settings.isPartialDataTable();
		TrailRecordSet trailRecordSet = hgc.retrieveTrailRecordSet();
		if (trailRecordSet.getDisplayRecords() == null || trailRecordSet.getDisplayRecords().isEmpty()) return; // concurrent activity

		final Density density = Density.toDensity(hgc.curveAreaBounds.width, trailRecordSet.getTimeStepSize());
		final int stripHeight = hgc.fixedCanvasHeight / trailRecordSet.getDisplayRecords().size();

		SummarySpots previousSpots = null;
		for (int i = 0; i < trailRecordSet.getDisplayRecords().size(); i++) {
			TrailRecord record = trailRecordSet.getDisplayRecords().get(i);
			Rectangle drawStripBounds = new Rectangle(hgc.curveAreaBounds.x, hgc.curveAreaBounds.y + stripHeight * i + SummaryComposite.UNK_GAP,
					hgc.curveAreaBounds.width, stripHeight);
			SummarySpots summarySpots = summaryComposite.getChartData(record).getSummarySpots();
			summarySpots.initialize(drawStripBounds, density);

			if (record.isVisible() || !isPartialDataTable) {
				if (record.equals(hgc.measuring.measure.measureRecord)) {
					summarySpots.drawMarkers(canvasImageGC, hgc.measuring.measure);
				}
				if (previousSpots != null && settings.isCurveSurvey()) {
					summarySpots.drawConnections(canvasImageGC, previousSpots, hgc.measuring.measure);
				}
				previousSpots = summarySpots;
			}
		}
	}

	/**
	 * Reset the graphic area and comment.
	 */
	@Override
	public void cleanMeasuring() {
		this.isLeftMouseMeasure = false;
		this.isRightMouseMeasure = false;

		log.finer(() -> "canvasGC=" + canvasGC + " curveSurvey=" + curveSurvey + " height=" + summaryComposite.graphicCanvas.getBounds().height);
		// if (this.summaryComposite.graphicCanvas.getBounds().height != 0) { // canvas was not yet drawn
		if (curveSurvey != null) { // required if this chart was not yet displayed
			//this.canvasGC = new GC(this.summaryComposite.graphicCanvas);
			//this.curveSurvey.setCanvasGC(this.canvasGC);

			//this.curveSurvey.cleanMeasurementPointer(this.summaryComposite.canvasImage);
			//this.canvasGC.dispose();
			throw new UnsupportedOperationException();
		}
// }
		this.summaryComposite.setRecordSetCommentStandard();
	}

	/**
	 *
	 */
	@Override
	public void processMouseDownMove(long timestamp_ms) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Perform UI activities at mouse up movements.
	 */
	@Override
	public void processMouseUpMove(Point point) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Determine which vertical line was moved.
	 */
	@Override
	public void processMouseDownAction(Point point) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Reset the identified vertical line.
	 */
	@Override
	public void processMouseUpAction(Point point) {
		throw new UnsupportedOperationException();
	}

}
