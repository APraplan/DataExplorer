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

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.config.Settings;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.ui.AbstractChartWindow.WindowActor;
import gde.histo.ui.menu.AbstractTabAreaContextMenu;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.GraphicsUtils;

/**
 * Histo chart drawing area base class.
 * @author Thomas Eickert (USER)
 */
public abstract class AbstractChartComposite extends Composite {
	private final static String	$CLASS_NAME					= AbstractChartComposite.class.getName();
	private final static Logger	log									= Logger.getLogger($CLASS_NAME);

	protected final static int	DEFAULT_TOP_GAP			= 5;																			// on top of the curves
	protected final static int	DEFAULT_SIDE_GAP		= 10;																			// at the leftmost and rightmost graphics
	protected final static int	DEFAULT_BOTTOM_GAP	= 20;																			// at the bottom of the plots for the scale
	protected final static int	DEFAULT_HEADER_GAP	= 5;
	protected final static int	DEFAULT_COMMENT_GAP	= 5;

	protected final static int	ZERO_CANVAS_HEIGHT	= 11;																			// minimize if smart statistics is not active

	/**
	 * Layout data for the life cycle of a chart.
	 * Members of the chartData classes.
	 */
	protected static class AbstractChartLayout {
	}

	/**
	 * Map holding layout data assigned to records.
	 */
	public static class AbstractChartData {

		private final Map<String, AbstractChartLayout> chartData = new LinkedHashMap<>();

		public void clear() {
			chartData.clear();
		}

		public AbstractChartLayout get(String recordName) {
			return chartData.get(recordName);
		}

		public AbstractChartLayout put(String recordName, AbstractChartLayout chartLayout) {
			return chartData.put(recordName, chartLayout);
		}

		@Override
		public String toString() {
			final int maxLen = 10;
			return "AbstractChartData [chartData=" + (this.chartData != null ? this.toString(this.chartData.entrySet(), maxLen) : null) + "]";
		}

		private String toString(Collection<?> collection, int maxLen) {
			StringBuilder builder = new StringBuilder();
			builder.append("[");
			int i = 0;
			for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
				if (i > 0) builder.append(", ");
				builder.append(iterator.next());
			}
			builder.append("]");
			return builder.toString();
		}
	}

	protected final DataExplorer					application				= DataExplorer.getInstance();
	protected final Settings							settings					= Settings.getInstance();

	protected final WindowActor						windowActor;

	protected Menu												popupmenu;
	protected AbstractTabAreaContextMenu	contextMenu;

	protected Color												curveAreaBackground;
	protected Color												surroundingBackground;
	protected Color												curveAreaBorderColor;

	protected Text												graphicsHeader;
	protected Text												recordSetComment;
	protected Text												xScale;
	protected Canvas											graphicCanvas;
	int																		headerHeight			= 0;
	int																		headerGap					= 0;
	int																		commentHeight			= 0;
	int																		commentGap				= 0;
	int																		xScaleHeight			= 0;
	protected String											graphicsHeaderText;

	/**
	 * Holds multiple sash children.
	 */
	protected Rectangle										canvasBounds;
	protected Image												canvasImage;
	protected GC													canvasImageGC;
	protected Rectangle										curveAreaBounds		= new Rectangle(0, 0, 1, 1);
	protected int													fixedCanvasY			= -1;
	protected int													fixedCanvasHeight	= -1;

	/** composite size - control resized */
	protected Point												oldSize						= new Point(0, 0);
	protected AbstractMeasuring						measuring;

	/**
	 * Returns the value nearest to {@code value} which is within the closed range {@code [min..max]}.
	 *
	 * <p>
	 * If {@code value} is within the range {@code [min..max]}, {@code value} is returned
	 * unchanged. If {@code value} is less than {@code min}, {@code min} is returned, and if
	 * {@code value} is greater than {@code max}, {@code max} is returned.
	 *
	 * @param value the {@code int} value to constrain
	 * @param min the lower bound (inclusive) of the range to constrain {@code value} to
	 * @param max the upper bound (inclusive) of the range to constrain {@code value} to
	 * @throws IllegalArgumentException if {@code min > max}
	 * @see <a href="https://www.google.de/search?q=Guava 22.0">Guava 22.0</a>
	 */
	public static int constrainToRange(int value, int min, int max) { // todo move into gde.utils.MathUtils
		if (min > max) throw new IllegalArgumentException(value + "  " + min + "/" + max);
		return Math.min(Math.max(value, min), max);
	}

	public AbstractChartComposite(Composite parent, CTabItem parentWindow, int style) {
		super(parent, style);
		this.windowActor = ((AbstractChartWindow) parentWindow).windowActor;
	}

	protected TrailRecordSet retrieveTrailRecordSet() {
		return windowActor.getTrailRecordSet();
	}

	/**
	 * Redraw the graphics canvas as well as the curve selector table.
	 */
	public void redrawGraphics() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			doRedrawGraphics();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					doRedrawGraphics();
				}
			});
		}
	}

	/**
	 * Update the graphics canvas, while repeatable redraw calls it optimized to the required area.
	 */
	protected synchronized void doRedrawGraphics() {
		log.fine(() -> "refresch graphics");
		this.drawCurves();
		this.graphicCanvas.redraw(); // do full update where required
		this.recordSetComment.redraw();
	}

	public void notifySelected() {
		this.recordSetComment.notifyListeners(SWT.FocusOut, new Event());
	}

	protected void setRecordSetCommentStandard() {
		this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.recordSetComment.setText(this.application.getPresentHistoExplorer().getHistoSet().getDirectoryScanStatistics());
		this.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0896));
	}

	public void enableGraphicsHeader(boolean enabled) {
		if (enabled) {
			this.headerGap = DEFAULT_HEADER_GAP;
			this.headerHeight = AbstractChartWindow.HEADER_ROW_HEIGHT - DEFAULT_TOP_GAP;
		} else {
			this.headerGap = DEFAULT_HEADER_GAP;
			this.headerHeight = 0;
		}
		setComponentBounds();
	}

	public void enableGraphicsScale(boolean enabled) {
		if (enabled) {
			GC gc = new GC(this.xScale);
			Point pt = gc.textExtent("0"); //$NON-NLS-1$
			this.xScaleHeight = 3 * pt.y;
			gc.dispose();
		} else {
			this.xScaleHeight = 0;
		}
		setComponentBounds();
	}

	public void enableRecordSetComment(boolean enabled) {
		if (enabled) {
			this.commentGap = DEFAULT_COMMENT_GAP;
			GC gc = new GC(this.recordSetComment);
			int stringHeight = gc.stringExtent(this.recordSetComment.getText()).y;
			this.commentHeight = stringHeight * 2 + 8;
			gc.dispose();
		} else {
			this.commentGap = 0;
			this.commentHeight = 0;
		}
		setComponentBounds();
	}

	public void clearHeaderAndComment() {
		this.recordSetComment.setText(GDE.STRING_EMPTY);
		this.recordSetComment.setToolTipText(GDE.STRING_EMPTY);
		this.graphicsHeader.setText(GDE.STRING_EMPTY);
		this.graphicsHeaderText = null;
		updateCaptions();
	}

	public synchronized void updateCaptions() {
		this.recordSetComment.redraw();
		this.graphicsHeader.redraw();
	}

	/**
	 * Resize the three areas: header, curve, comment.
	 */
	protected void setComponentBounds() {
		Rectangle graphicsBounds = this.getClientArea();
		// this.application.setGraphicsSashFormWeights(this.graphicSashForm.getSize().x - graphicsBounds.width);
		int x = 0;
		int y = this.headerGap;
		int width = graphicsBounds.width;
		int height = this.headerHeight;
		this.graphicsHeader.setBounds(x, y, width, height);
		log.log(Level.FINER, "recordSetHeader.setBounds " + this.graphicsHeader.getBounds()); //$NON-NLS-1$

		y = this.fixedCanvasY < 0 ? this.headerGap + this.headerHeight : this.fixedCanvasY;
		height = this.fixedCanvasHeight < 0 ? graphicsBounds.height - (this.headerGap + this.commentGap + this.commentHeight + this.headerHeight)
				: this.fixedCanvasHeight + this.headerGap + DEFAULT_BOTTOM_GAP;
		this.graphicCanvas.setBounds(x, y, width, height);
		log.log(Level.FINER, "graphicCanvas.setBounds ", this.graphicCanvas.getBounds()); //$NON-NLS-1$

		y += height + this.commentGap;
		height = this.commentHeight;
		this.recordSetComment.setBounds(20, y, width - 40, height - 5);
		log.log(Level.FINER, "recordSetComment.setBounds " + this.recordSetComment.getBounds()); //$NON-NLS-1$
	}

	/**
	 * Option to set a graphics area with position and size not depending on the header and the comment (e.g. summary graphics)
	 */
	protected abstract void setFixedGraphicCanvas(Rectangle realBounds);

	/**
	 * @param fixedY is the constant left upper position or -1 in case of a flexible value
	 * @param fixedHeight is the constant height or -1 in case of a flexible height
	 */
	public void setFixedGraphicCanvas(int fixedY, int fixedHeight) {
		this.fixedCanvasY = fixedY;
		this.fixedCanvasHeight = fixedHeight;
		log.finer(() -> "y = " + fixedY + "  height = " + fixedHeight); //$NON-NLS-1$
		setComponentBounds();
	}

	/**
	 * @return the graphic window content as image
	 */
	public Image getGraphicsPrintImage() {
		Image graphicsImage = null;
		int graphicsHeight = 30 + this.canvasBounds.height + 40;
		TrailRecordSet trailRecordSet = windowActor.getTrailRecordSet();
		if (trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
			if (this.canvasImage != null) this.canvasImage.dispose();
			this.canvasImage = new Image(GDE.display, this.canvasBounds);
			this.canvasImageGC = new GC(this.canvasImage); // SWTResourceManager.getGC(this.canvasImage);
			this.canvasImageGC.setBackground(this.surroundingBackground);
			this.canvasImageGC.fillRectangle(this.canvasBounds);
			this.canvasImageGC.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			drawCurves();
			graphicsImage = new Image(GDE.display, this.canvasBounds.width, graphicsHeight);
			GC graphicsGC = new GC(graphicsImage);
			graphicsGC.setForeground(this.graphicsHeader.getForeground());
			graphicsGC.setBackground(this.surroundingBackground);
			graphicsGC.setFont(this.graphicsHeader.getFont());
			graphicsGC.fillRectangle(0, 0, this.canvasBounds.width, graphicsHeight);
			if (this.graphicsHeader.getText().length() > 1) {
				GraphicsUtils.drawTextCentered(this.graphicsHeader.getText(), this.canvasBounds.width / 2, 20, graphicsGC, SWT.HORIZONTAL);
			}
			graphicsGC.setFont(this.recordSetComment.getFont());
			if (this.recordSetComment.getText().length() > 1) {
				GraphicsUtils.drawText(this.recordSetComment.getText(), 20, graphicsHeight - 40, graphicsGC, SWT.HORIZONTAL);
			}
			graphicsGC.drawImage(this.canvasImage, 0, 30);
			graphicsGC.dispose();
			this.canvasImageGC.dispose();
		}
		return graphicsImage;
	}

	/**
	 * Draw the record graphs with their scales and define the curve area.
	 */
	protected void drawCurves() {
		if (windowActor.getTrailRecordSet() != null) {
			// Get the canvas and its dimensions draw image outside paint event
			this.canvasBounds = this.graphicCanvas.getClientArea();
			log.finer(() -> "canvasBounds" + this.canvasBounds);
			if (this.canvasBounds.height <= 0 || this.canvasBounds.width <= 0) return;
	
			if (this.canvasImage != null) this.canvasImage.dispose();
				this.canvasImage = new Image(GDE.display, this.canvasBounds);
			this.canvasImageGC = new GC(this.canvasImage); // SWTResourceManager.getGC(this.canvasImage);
			this.canvasImageGC.setBackground(this.surroundingBackground);
			this.canvasImageGC.fillRectangle(this.canvasBounds);
			this.canvasImageGC.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
	
			// changed curve selection may change the scale end values
			windowActor.getTrailRecordSet().syncScaleOfSyncableRecords();
	
			int dataScaleWidth = defineDataScaleWidth();
			curveAreaBounds = defineCurveAreaBounds(dataScaleWidth, defineNumberLeftRightScales());
			defineLayoutParams(); // initialize early in order to avoid problems in mouse move events
	
			drawCurveArea(dataScaleWidth);
			
			this.canvasImageGC.dispose();
		}
	}

	/**
	 * @return the space width to be used for any of the scales
	 */
	private int defineDataScaleWidth() {
		// calculate the space width to be used for the scales
		int dataScaleWidth; // horizontal space used for text and scales, numbers and caption
		{
			Point pt = canvasImageGC.textExtent("-000,00"); //$NON-NLS-1$
			int horizontalGap = pt.x / 5;
			int horizontalNumberExtend = pt.x;
			int horizontalCaptionExtend = pt.y;
			dataScaleWidth = horizontalNumberExtend + horizontalCaptionExtend + horizontalGap;
		}
		return dataScaleWidth;
	}

	/**
	 * @param dataScaleWidth is the space width to be used for any of the scales
	 * @param numberLeftRightScales holds the numbers of scales {left, right}
	 */
	private Rectangle defineCurveAreaBounds(int dataScaleWidth, int[] numberLeftRightScales) {
		int spaceLeft = numberLeftRightScales[0] * dataScaleWidth;
		int spaceRight = numberLeftRightScales[1] * dataScaleWidth;

		int x0, y0; // the lower left corner of the curve area
		int xMax, yMax; // the upper right corner of the curve area
		int width; // x coordinate width
		int height; // y coordinate - make modulo 10 ??

		// calculate the horizontal area available for plotting graphs
		int gapSide = DEFAULT_SIDE_GAP; // free gap left or right side of the curves
		x0 = spaceLeft + (numberLeftRightScales[0] > 0 ? gapSide / 2 : gapSide);// enable a small gap if no axis is shown
		xMax = canvasBounds.width - spaceRight - (numberLeftRightScales[1] > 0 ? gapSide / 2 : gapSide);
		width = Math.max(1, xMax - x0);

		// calculate the vertical area available for plotting graphs
		yMax = DEFAULT_TOP_GAP; // free gap on top of the curves
		y0 = canvasBounds.height - yMax - getXScaleHeight();
		height = Math.max(1, y0 - yMax); // WB ?? recalculate due to modulo 10 ??
		log.finer(() -> "draw area x0=" + x0 + ", y0=" + y0 + ", xMax=" + xMax + ", yMax=" + yMax + ", width=" + width + ", height=" + height); //$NON-NLS-6$

		Rectangle result = new Rectangle(x0, y0 - height, width, height);
		log.log(FINE, "curve bounds=", result); //$NON-NLS-1$
		return result;
	}

	/**
	 * @return the height for a full view or -1 if there is no fixed height
	 */
	public int getFixedTotalHeight() {
		return DEFAULT_TOP_GAP + headerHeight + DEFAULT_HEADER_GAP + fixedCanvasHeight + DEFAULT_COMMENT_GAP + commentHeight + DEFAULT_BOTTOM_GAP;
	}

	/**
	 * @return the height of the scales with description or legend and scale text
	 */
	public int getXScaleHeight() {
		return this.xScaleHeight;
	}

	/**
	 * @return numberLeftRightScales holds the numbers of scales {left, right}
	 */
	protected abstract int[] defineNumberLeftRightScales();

	/**
	 * Draw the curves into the curve area.
	 * @param dataScaleWidth is the width of one single scale
	 */
	protected abstract void drawCurveArea(int dataScaleWidth);

	/**
	 * Check input x,y value against curve are bounds and correct to bound if required.
	 * @return Point containing corrected x,y position value
	 */
	protected Point checkCurveBounds(int xPos, int yPos) {
		log.finer(() -> "in  xPos = " + xPos + " yPos = " + yPos); //$NON-NLS-1$ //$NON-NLS-2$
		int tmpxPos = xPos - this.curveAreaBounds.x;
		int tmpyPos = yPos - this.curveAreaBounds.y;
		tmpxPos = constrainToRange(tmpxPos, 0, this.curveAreaBounds.width);
		tmpyPos = constrainToRange(tmpyPos, 0, this.curveAreaBounds.height);
		if (log.isLoggable(FINER)) log.log(FINER, "out xPos = " + tmpxPos + " yPos = " + tmpyPos); //$NON-NLS-1$ //$NON-NLS-2$
		return new Point(tmpxPos, tmpyPos);
	}

	/**
	 * @return true if the x position is on the left border
	 */
	protected boolean isBeyondLeftBounds(int xPos) {
		return xPos < this.curveAreaBounds.x;
	}

	/**
	 * @return true if the x position is on the right border
	 */
	protected boolean isBeyondRightBounds(int xPos) {
		return xPos > this.curveAreaBounds.x + this.curveAreaBounds.width;
	}

	public Optional<AbstractMeasuring> getMeasuring() {
		return Optional.ofNullable(this.measuring);
	}

	public abstract void setMeasuringActive(Measure measure);

	/**
	 * Reset the graphic area and comment.
	 */
	public void cleanMeasuring() {
		//measuring.cleanMeasuring();
		measuring = null;
	}

	/**
	 * Draw the containing records and sets the comment.
	 */
	public void abstractDrawAreaPaintControl(GC canvasGC) {
		if (windowActor.getTrailRecordSet() == null) return;


		setRecordSetCommentStandard();
		windowActor.setStatusMessage(GDE.STRING_EMPTY);

		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
			canvasGC.drawImage(this.canvasImage, 0, 0);

			if (measuring != null) 
				measuring.drawMeasuring(canvasGC);
		} else {
			canvasGC.drawImage(this.canvasImage, 0, 0);
		}
	}

	/**
	 * Make drawing independent from layout data creation.
	 */
	protected abstract void defineLayoutParams();

}
