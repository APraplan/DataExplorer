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

    Copyright (c) 2016,2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.histo.ui;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.WARNING;

import java.util.Optional;
import java.util.logging.Level;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

import com.sun.istack.Nullable;

import gde.GDE;
import gde.data.Channels;
import gde.histo.datasources.HistoSet;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * Histo chart window base class.
 * @author Thomas Eickert
 */
public abstract class AbstractChartWindow extends CTabItem {
	private final static String	$CLASS_NAME				= AbstractChartWindow.class.getName();
	private final static Logger	log								= Logger.getLogger($CLASS_NAME);

	protected static final int	SELECTOR_WIDTH		= 117;
	/**
	 * y position of the first table row and graphics chart gap.
	 */
	protected static final int	HEADER_ROW_HEIGHT	= 33;

	/**
	 * Mediator class for access by window composites.
	 */
	class WindowActor {

		void clearMeasuring() {
			measure.ifPresent(mm -> {
				for (AbstractChartComposite c : getCharts()) {
					c.cleanMeasuring();
					if (c instanceof SummaryComposite) { // cleaning the summary composite is not implemented
						log.log(Level.FINE, "started");
						c.doRedrawGraphics(); // todo check if erasing lines like in CurveSurvey is appropriate
					}
				}
				setStatusMessage(GDE.STRING_EMPTY);
				measure = Optional.empty();
			});
		}

		void setMeasuringActive(String recordKey, boolean enabled, boolean isDeltaMeasuring) {
			if (Channels.getInstance().getActiveChannel() == null) return;
			if (!DataExplorer.getInstance().getPresentHistoExplorer().hasRecords()) return;

			TrailRecordSet trailRecordSet = DataExplorer.getInstance().getPresentHistoExplorer().getTrailRecordSet();
			if (windowActor.getTrailRecordSet().containsKey(recordKey)) {
				windowActor.clearMeasuring();
				if (enabled) {
					TrailRecord trailRecord = trailRecordSet.get(recordKey);
					curveSelectorComposite.setRecordSelection(trailRecord);
					Measure tmpMeasuring = new Measure(isDeltaMeasuring, trailRecord);
					measure = Optional.of(tmpMeasuring);
					for (AbstractChartComposite c : getCharts()) {
						c.setMeasuringActive(tmpMeasuring);
					}
				}
			}
		}

		void drawMeasuring(GC canvasGC) {
			measure.ifPresent(mm -> {
				for (AbstractChartComposite c : getCharts()) {
					c.getMeasuring().ifPresent(m -> m.drawMeasuring(canvasGC));
				}
			});
		}

		void processMouseDownMove(long adjacentTimestamp_ms) {
			measure.ifPresent(mm -> getGraphicsComposite().getMeasuring().ifPresent(m -> m.processMouseDownMove(adjacentTimestamp_ms)));
			getSummaryComposite().ifPresent(AbstractChartComposite::doRedrawGraphics);
		}

		void processMouseUpMove(Point point) {
			measure.ifPresent(mm -> getGraphicsComposite().getMeasuring().ifPresent(m -> m.processMouseUpMove(point)));
		}

		void processMouseDownAction(Point point) {
			measure.ifPresent(mm -> getGraphicsComposite().getMeasuring().ifPresent(m -> m.processMouseDownAction(point)));
		}

		void processMouseUpAction(Point point) {
			measure.ifPresent(mm -> {
				getGraphicsComposite().getMeasuring().ifPresent(m -> m.processMouseUpAction(point));
				getSummaryComposite().ifPresent(AbstractChartComposite::doRedrawGraphics);
			});
		}

		/**
		 * Set the message if it complies with message priorities.
		 */
		void setStatusMessage(String message) {
			if (message.isEmpty() && !measure.isPresent() && getTrailRecordSet() != null && !getTrailRecordSet().getTemplate().getFilePathMessage().isEmpty()) {
				// prioritize template data
				DataExplorer.getInstance().setStatusMessage(getTrailRecordSet().getTemplate().getFilePathMessage());
			} else if (!message.isEmpty() && measure.isPresent()) {
				// prioritize measure data
				DataExplorer.getInstance().setStatusMessage(message);
			} else if (DataExplorer.getInstance().getPresentHistoExplorer().getVolatileStatusMessage() == null) {
				DataExplorer.getInstance().setStatusMessage(message);
			}
		}

		/**
		 * Update any visible histo tab.
		 * @param createRecordSet true creates the recordset from the histo vaults; false uses the existing recordset
		 * @param rebuildTrails true refills the recordset and keeps the selector settings; false only rebuilds the UI
		 */
		void updateHistoTabs(boolean createRecordSet, boolean rebuildTrails, boolean updateSelector) {
			DataExplorer.getInstance().getPresentHistoExplorer().updateHistoTabs(createRecordSet, rebuildTrails, updateSelector);
		}

		void saveTemplate() {
			if (DataExplorer.getInstance().getPresentHistoExplorer().hasRecords()) {
				TrailRecordSet trailRecordSet = DataExplorer.getInstance().getPresentHistoExplorer().getTrailRecordSet();
				trailRecordSet.getTemplate().setHistoFileName(trailRecordSet.getTemplate().getDefaultHistoFileName());
				trailRecordSet.saveTemplate();
			}
		}

		void setChartSashFormWeights(SelectorComposite composite) {
			int compositeWidth = isCurveSelectorEnabled ? composite.getCompositeWidth() : 0;
			AbstractChartWindow.this.setSashFormWeights(compositeWidth);
		}

		void updateChartWindow(boolean redrawCurveSelector) {
			AbstractChartWindow.this.updateWindow(redrawCurveSelector);
		}

		HistoSet getHistoSet() {
			return DataExplorer.getInstance().getPresentHistoExplorer().getHistoSet();
		}

		@Nullable // i.e. if rebuild thread is not finished
		TrailRecordSet getTrailRecordSet() {
			return Channels.getInstance().getActiveChannel() != null ? DataExplorer.getInstance().getPresentHistoExplorer().getTrailRecordSet() : null;
		}

		boolean isCurveSelectorEnabled() {
			return isCurveSelectorEnabled;
		}

		boolean isMeasureRecord(String recordKeyName) {
			return AbstractChartWindow.this.isMeasureRecord(recordKeyName);
		}

		void scrollSummaryComposite() {
			AbstractChartWindow.this.scrollSummaryComposite();
		}

		public void setTrailVisible(TrailRecord record, int selectIndex) {
			curveSelectorComposite.setRecordSelection(record, selectIndex);
		}

		void setTemplateChart() {
			AbstractChartWindow.this.setTemplateChart();
		}
	}

	protected static ImageData flipHorizontal(ImageData inputImageData) {
		int bytesPerPixel = inputImageData.bytesPerLine / inputImageData.width;
		int outBytesPerLine = inputImageData.width * bytesPerPixel;
		byte[] outDataBytes = new byte[inputImageData.data.length];
		int outX = 0, outY = 0, inIndex = 0, outIndex = 0;

		for (int y = 0; y < inputImageData.height; y++) {
			for (int x = 0; x < inputImageData.width; x++) {
				outX = x;
				outY = inputImageData.height - y - 1;
				inIndex = (y * inputImageData.bytesPerLine) + (x * bytesPerPixel);
				outIndex = (outY * outBytesPerLine) + (outX * bytesPerPixel);
				System.arraycopy(inputImageData.data, inIndex, outDataBytes, outIndex, bytesPerPixel);
			}
		}
		return new ImageData(inputImageData.width, inputImageData.height, inputImageData.depth, inputImageData.palette, outBytesPerLine, outDataBytes);
	}

	protected final WindowActor	windowActor;
	protected final CTabFolder	tabFolder;

	/**
	 * Righthand side of the chart.
	 */
	protected SashForm					graphicSashForm;
	/**
	 * Lefthand side of the chart.
	 */
	protected SelectorComposite	curveSelectorComposite;
	protected boolean						isCurveSelectorEnabled	= true;

	private int[]								sashFormWeights					= new int[] { 100, 1000 };
	protected Optional<Measure>	measure									= Optional.empty();

	protected AbstractChartWindow(CTabFolder parent, int style, int index) {
		super(parent, style, index);
		SWTResourceManager.registerResourceUser(this);
		this.tabFolder = parent;
		this.windowActor = new WindowActor();
	}

	public void resetStatusMessage() {
		windowActor.setStatusMessage("");
	}

	/**
	 * Redraw the graphics canvas as well as the curve selector table.
	 */
	public abstract void redrawGraphics(final boolean redrawCurveSelector);

	/**
	 * Update graphics window header and description.
	 */
	@Deprecated
	public abstract void updateCaptions();

	/**
	 * Method to update the curves displayed in the curve selector panel.
	 */
	@Deprecated
	public void updateCurveSelectorTable() {
		if (Thread.currentThread().getId() == DataExplorer.getInstance().getThreadId()) {
			this.curveSelectorComposite.doUpdateCurveSelectorTable();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					AbstractChartWindow.this.curveSelectorComposite.doUpdateCurveSelectorTable();
				}
			});
		}
	}

	/**
	 * @param newSelectorCompositeWidth the changed curve selector width
	 */
	public void setSashFormWeights(int newSelectorCompositeWidth) {
		log.log(FINER, "newSelectorCompositeWidth= ", newSelectorCompositeWidth); //$NON-NLS-1$
		int tabFolderClientAreaWidth = this.tabFolder.getBounds().width;
		// begin workaround: sometimes tabFolder.getClientArea().width returned values greater than screen size ????
		int bestGuessWidth = DataExplorer.getInstance().getClientArea().width;
		if (tabFolderClientAreaWidth > bestGuessWidth) {
			log.log(WARNING, "tabFolder clientAreaWidth missmatch, tabFolderWidth = " + tabFolderClientAreaWidth + " vs applicationWidth = " + bestGuessWidth); //$NON-NLS-1$ //$NON-NLS-2$
			tabFolderClientAreaWidth = bestGuessWidth;
			this.tabFolder.setSize(tabFolderClientAreaWidth, this.tabFolder.getBounds().height);
		}
		// end workaround: sometimes tabFolder.getClientArea().width returned values greater than screen size ????
		int selectorCompositeWidth = newSelectorCompositeWidth > tabFolderClientAreaWidth / 2 ? tabFolderClientAreaWidth / 2 : newSelectorCompositeWidth;
		int[] newWeights = new int[] { selectorCompositeWidth, tabFolderClientAreaWidth - selectorCompositeWidth };
		log.log(FINER, "newSelectorCompositeWidth= ", selectorCompositeWidth); //$NON-NLS-1$
		if (this.sashFormWeights[0] != newWeights[0] || this.sashFormWeights[1] != newWeights[1]) {
			this.sashFormWeights = newWeights;
			try {
				if (this.sashFormWeights.length == 2 && this.sashFormWeights[1] != 0) //Linux call this with 0,0
					this.graphicSashForm.setWeights(this.sashFormWeights);
			} catch (IllegalArgumentException e) {
				log.log(WARNING, "graphicSashForm.setWeights(this.sashFormWeights) failed!", e); //$NON-NLS-1$
			}
		}
		if (log.isLoggable(FINE))
			log.log(FINE, "sash weight = " + this.sashFormWeights[0] + ", " + this.sashFormWeights[1] + " tabFolderClientAreaWidth = " + tabFolderClientAreaWidth); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public int getWidth() {
		return this.graphicSashForm.getSize().x;
	}

	public int[] getSashFormWeights() {
		return this.sashFormWeights;
	}

	public SashForm getGraphicSashForm() {
		return this.graphicSashForm;
	}

	public abstract void enableRecordSetComment(boolean enabled);

	public abstract void clearHeaderAndComment();

	protected abstract AbstractChartComposite getGraphicsComposite();

	protected abstract Optional<? extends AbstractChartComposite> getSummaryComposite();

	public boolean isActiveCurveSelectorContextMenu() {
		return this.curveSelectorComposite.contextMenu.isActive();
	}

	/**
	 * Create visible tab window content as image.
	 * @return image with content
	 */
	public abstract Image getContentAsImage();

	public abstract void enableGraphicsHeader(boolean enabled);

	public abstract void enableGraphicsScale(boolean enabled);

	public abstract void setCurveAreaBackground(Color curveAreaBackground);

	public abstract void setCurveAreaBorderColor(Color borderColor);

	public abstract void setSurroundingBackground(Color surroundingBackground);

	/**
	 * @return true if selector is enabled
	 */
	public boolean isCurveSelectorEnabled() {
		return this.isCurveSelectorEnabled;
	}

	/**
	 * Enable curve selector which relect to the sash form weights using the column widths.
	 */
	public void enableCurveSelector(boolean enabled) {
		this.isCurveSelectorEnabled = enabled;
		if (enabled) {
			setSashFormWeights(this.curveSelectorComposite.getCompositeWidth());
		} else {
			setSashFormWeights(0);
		}
	}

	/**
	 * Select the next graph or alternatively restore to full vertical size.
	 */
	public abstract void scrollSummaryComposite();

	public void scrollComposite() {
		windowActor.scrollSummaryComposite();
	}

	/**
	 * Switch window into measurement mode for the selected record.
	 */
	public void setMeasurementActive(String recordKey, boolean enabled, boolean isDeltaMeasuring) {
		windowActor.setMeasuringActive(recordKey, enabled, isDeltaMeasuring);
	}

	/**
	 * Clean everything related to the measurement.
	 */
	public void cleanMeasuring() {
		curveSelectorComposite.resetContextMenuMeasuring();
		windowActor.clearMeasuring();
	}

	public Optional<TrailRecord> getMeasureRecord() {
		return measure.map(m -> m.measureRecord);
	}

	/**
	 * @return true if there is a measuring record with this name
	 */
	public boolean isMeasureRecord(String recordKeyName) {
		return getMeasureRecord().map(TrailRecord::getName).map(r -> r.equals(recordKeyName)).orElse(false);
	}

	public void updateWindow(boolean redrawCurveSelector) {
		if (!isActiveCurveSelectorContextMenu()) {
			redrawGraphics(redrawCurveSelector);
		}
	}

	public abstract AbstractChartComposite[] getCharts();

	/**
	 * @return the graphic window content as image
	 */
	public Image getGraphicsPrintImage() {
		return getGraphicsComposite().getGraphicsPrintImage();
	}

	/**
	 * Set the graphics chart into the window without redrawing.
	 */
	protected void setTemplateChart() {
		// overwrite if function required
	}
	
	/**
	 * update background/foreground color selector table header
	 */
	public void updateColorSchema() {
		if (curveSelectorComposite != null) curveSelectorComposite.updateColorSchema();
	}

}
