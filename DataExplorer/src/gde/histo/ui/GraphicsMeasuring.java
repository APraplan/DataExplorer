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

import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.histo.recordings.HistoGraphicsMapper;
import gde.histo.recordings.TrailRecordSetFormatter;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.ColorUtils;
import gde.utils.StringHelper;

/**
 * Graphics measuring activity mapper for mouse movements and other UI actions from menus, checkboxes etc.
 * @author Thomas Eickert (USER)
 */
public final class GraphicsMeasuring extends AbstractMeasuring {
	private final static String	$CLASS_NAME	= GraphicsMeasuring.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	public enum MeasuringMode {

		/** Single measurement mode is active */
		MEASURE {
			@Override
			void setMode(GraphicsMeasuring graphicsMeasuring) {
				graphicsMeasuring.isLeftMouseMeasure = true;
				graphicsMeasuring.isRightMouseMeasure = false;
			}

			@Override
			void drawMeasurement(GC canvasGC, GraphicsMeasuring hgm) {
				AbstractChartComposite hgc = hgm.graphicsComposite;

				String statusMessage = hgm.curveSurvey.drawMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, hgm.measure.getTimestampMeasure_ms()));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
			}

			@Override
			void drawLeftMeasurement(GC canvasGC, GraphicsMeasuring hgm) {
				AbstractChartComposite hgc = hgm.graphicsComposite;
				long startTime = new Date().getTime();
				String statusMessage = hgm.curveSurvey.drawMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, hgm.measure.getTimestampMeasure_ms()));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
				log.time(() -> "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}

			@Override
			void drawRightMeasurement(GC canvasGC, GraphicsMeasuring hgm) {
				// simple measure has only a left measurement
			}
		},

		/** delta measurement mode is active */
		MEASURE_DELTA {
			@Override
			void setMode(GraphicsMeasuring graphicsMeasuring) {
				graphicsMeasuring.isLeftMouseMeasure = false;
				graphicsMeasuring.isRightMouseMeasure = true;
			}

			@Override
			void drawMeasurement(GC canvasGC, GraphicsMeasuring hgm) {
				AbstractChartComposite hgc = hgm.graphicsComposite;

				String statusMessage = hgm.curveSurvey.drawDeltaMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, hgm.measure.getTimestampMeasure_ms()));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
			}

			@Override
			void drawLeftMeasurement(GC canvasGC, GraphicsMeasuring hgm) {
				AbstractChartComposite hgc = hgm.graphicsComposite;
				long startTime = new Date().getTime();
				String statusMessage = hgm.curveSurvey.drawDeltaMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, hgm.measure.getTimestampMeasure_ms()));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
				log.time(() -> "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}

			@Override
			void drawRightMeasurement(GC canvasGC, GraphicsMeasuring hgm) {
				AbstractChartComposite hgc = hgm.graphicsComposite;
				long startTime = new Date().getTime();
				String statusMessage = hgm.curveSurvey.drawDeltaMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, hgm.measure.getTimestampMeasure_ms()));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
				log.time(() -> "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}
		};

		/**
		 * Set graphics window measurement mode.
		 */
		abstract void setMode(GraphicsMeasuring graphicsMeasuring);

		/**
		 * Perform the UI activities required for a measurement.
		 */
		abstract void drawMeasurement(GC canvasGC, GraphicsMeasuring hgm);

		/**
		 * Perform the UI activities for the first measurement timestamp.
		 * In case of delta measurement: the timestamp which is initially on the left.
		 */
		abstract void drawLeftMeasurement(GC canvasGC, GraphicsMeasuring hgm);

		/**
		 * Perform the UI activities for the second measurement timestamp used for delta measurement.
		 */
		abstract void drawRightMeasurement(GC canvasGC, GraphicsMeasuring hgm);

		private static String getSelectedMeasurementsAsTable(Text recordSetComment, long timestamp_ms) {
			recordSetComment.setFont(SWTResourceManager.getFont("Courier New", GDE.WIDGET_FONT_SIZE - 1, SWT.BOLD));
			return TrailRecordSetFormatter.getSelectedMeasurementsAsTable(timestamp_ms);
		}

		static MeasuringMode getMode(boolean isDeltaMeasuring) {
			if (isDeltaMeasuring) {
				return MeasuringMode.MEASURE_DELTA;
			} else {
				return MeasuringMode.MEASURE;
			}
		}
	};

	boolean													isLeftMouseMeasure	= false;
	boolean													isRightMouseMeasure	= false;

	private final GraphicsComposite	graphicsComposite;
	private final CurveSurvey				curveSurvey;

	private GC											canvasGC;
	private MeasuringMode						mode;
	
	public static int							xPosMeasure	= Integer.MIN_VALUE;
	public static int							yPosMeasure	= Integer.MIN_VALUE;
	public static int							xPosDelta		= Integer.MIN_VALUE;
	public static int							yPosDelta		= Integer.MIN_VALUE;


	public GraphicsMeasuring(GraphicsComposite graphicsComposite, Measure measure) {
		super(measure);
		this.graphicsComposite = graphicsComposite;
		this.mode = MeasuringMode.getMode(measure.isDeltaMeasure);
		this.mode.setMode(this);

		this.curveSurvey = new CurveSurvey(graphicsComposite, measure);
		//this.curveSurvey.initialize();
		this.curveSurvey.setMeasurePosition();
		this.curveSurvey.setDeltaPosition();
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
		//Image canvasImage = this.graphicsComposite.canvasImage;
		// set the gc properties
		canvasGC.setForeground(ColorUtils.getColor(measure.measureRecord.getRGB()));

		curveSurvey.setCanvasGC(canvasGC);
		// all obsolete lines are cleaned up now draw new position marker
		//this.curveSurvey.cleanMeasurementPointer(canvasImage);
		measure.setTimestampMeasure_ms(timestampMeasureNew_ms);
		measure.setTimestampDelta_ms(timestampDeltaNew_ms);

		//curveSurvey.initialize();
		curveSurvey.setMeasurePosition();
		curveSurvey.setDeltaPosition();
		mode.drawMeasurement(canvasGC, this);
		//this.canvasGC.dispose();
	}

	/**
	 * Reset the graphic area and comment.
	 */
	@Override
	public void cleanMeasuring() {
		this.isLeftMouseMeasure = false;
		this.isRightMouseMeasure = false;

		if (curveSurvey != null) { // required if this chart was not yet displayed
			this.curveSurvey.setCanvasGC(this.canvasGC);

			//this.curveSurvey.cleanMeasurementPointer(this.graphicsComposite.canvasImage);
			//this.canvasGC.dispose();
		}
		this.graphicsComposite.setRecordSetCommentStandard();
	}

	/**
	 * Draw the survey graphics while moving the vertical line.
	 */
	@Override
	public void processMouseDownMove(long timestamp_ms) {
		//this.curveSurvey.setCanvasGC(this.canvasGC);
		//this.canvasGC.setForeground(ColorUtils.getColor(measure.measureRecord.getRGB()));

		if (this.isLeftMouseMeasure) {
			int height = graphicsComposite.curveAreaBounds.height;
			int yPosMeasureNew = HistoGraphicsMapper.getVerticalDisplayPos(graphicsComposite.getChartData(measure.measureRecord), height, measure.measureRecord.getParent().getIndex(timestamp_ms));
			if (this.curveSurvey.isNewMeasureSpot(timestamp_ms, yPosMeasureNew)) {
			//	curveSurvey.cleanMeasurementPointer(graphicsComposite.canvasImage);

				measure.setTimestampMeasure_ms(timestamp_ms);
				curveSurvey.setMeasurePosition();
				//mode.drawLeftMeasurement(this);
			}
		} else if (this.isRightMouseMeasure) {
			int height = graphicsComposite.curveAreaBounds.height;
			int yPosDeltaNew = HistoGraphicsMapper.getVerticalDisplayPos(graphicsComposite.getChartData(measure.measureRecord), height, measure.measureRecord.getParent().getIndex(timestamp_ms));
			if (this.curveSurvey.isNewDeltaSpot(timestamp_ms, yPosDeltaNew)) {
				//curveSurvey.cleanMeasurementPointer(graphicsComposite.canvasImage);

				measure.setTimestampDelta_ms(timestamp_ms);
				curveSurvey.setDeltaPosition();
				//mode.drawRightMeasurement(this);
			}
		}
		//this.canvasGC.dispose();
		this.graphicsComposite.graphicCanvas.redraw();
	}

	/**
	 * Perform UI activities at mouse up movements.
	 */
	@Override
	public void processMouseUpMove(Point point) {
		if (this.curveSurvey.isOverVerticalLine(point.x)) {
			this.graphicsComposite.graphicCanvas.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_SIZEWE));
		} else {
			this.graphicsComposite.graphicCanvas.setCursor(DataExplorer.getInstance().getCursor());
		}
	}

	/**
	 * Determine which vertical line was moved.
	 */
	@Override
	public void processMouseDownAction(Point point) {
		if (this.curveSurvey.isOverVerticalLine(point.x)) {
			if (this.curveSurvey.isNearMeasureLine(point.x)) {
				this.isLeftMouseMeasure = true;
				this.isRightMouseMeasure = false;
			} else if (this.curveSurvey.isNearDeltaLine(point.x)) {
				this.isRightMouseMeasure = true;
				this.isLeftMouseMeasure = false;
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}

	/**
	 * Reset the identified vertical line.
	 */
	@Override
	public void processMouseUpAction(Point point) {
		if (this.isLeftMouseMeasure) {
			this.isLeftMouseMeasure = false;
			// application.setStatusMessage(GDE.STRING_EMPTY);
		} else if (this.isRightMouseMeasure) {
			this.isRightMouseMeasure = false;
			// application.setStatusMessage(GDE.STRING_EMPTY);
		}
		log.time(() -> "isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure);
	}

}
