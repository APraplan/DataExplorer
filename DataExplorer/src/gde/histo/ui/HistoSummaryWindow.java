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

import static java.util.logging.Level.WARNING;

import java.util.Arrays;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import gde.GDE;
import gde.log.Level;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * Histo summary window as a sash form of a curve selection table and a drawing canvas.
 * The drawing canvas holds multiple charts.
 * @author Thomas Eickert
 */
public final class HistoSummaryWindow extends AbstractChartWindow {
	private static final String	$CLASS_NAME						= HistoSummaryWindow.class.getName();
	private static final Logger	log										= Logger.getLogger($CLASS_NAME);

	public static final int[]		DEFAULT_CHART_WEIGHTS	= new int[] { 0, 1000 };							// 2nd chart is the default chart

	protected SashForm					compositeSashForm;

	protected HistoSummaryWindow(CTabFolder currentDisplayTab, int style, int index) {
		super(currentDisplayTab, style, index);
	}

	public static AbstractChartWindow create(CTabFolder dataTab, int style, int position) {
		HistoSummaryWindow window = new HistoSummaryWindow(dataTab, style, position);

		window.graphicSashForm = new SashForm(window.tabFolder, SWT.HORIZONTAL);
		window.setControl(window.graphicSashForm);
		window.curveSelectorComposite = new SelectorComposite(window.graphicSashForm, window);
		window.compositeSashForm = new SashForm(window.graphicSashForm, SWT.VERTICAL);
		try {
			if (GDE.shell.getClientArea().width > SELECTOR_WIDTH) //Linux call this with 0,0
				window.graphicSashForm.setWeights(new int[] { SELECTOR_WIDTH, GDE.shell.getClientArea().width - SELECTOR_WIDTH });
		} catch (IllegalArgumentException e) {
			log.log(WARNING, "graphicSashForm.setWeights(this.sashFormWeights) failed!", e); //$NON-NLS-1$
		}

		SummaryComposite summaryComposite = new SummaryComposite(window.compositeSashForm, window); // at the top
		new GraphicsComposite(window.compositeSashForm, window);
		window.compositeSashForm.setWeights(DEFAULT_CHART_WEIGHTS.clone());
		summaryComposite.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				HistoSummaryWindow.log.log(Level.FINEST, "height=", summaryComposite.getBounds().height);
				window.curveSelectorComposite.setVerticalBarVisible(window.compositeSashForm.getWeights()[0] == 0);
			}
		});

		window.setFont(SWTResourceManager.getFont(DataExplorer.getInstance(), GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 3 : 1), SWT.NORMAL));
		window.setText(Messages.getString(MessageIds.GDE_MSGT0792));
		return window;
	}

	@Override
	protected GraphicsComposite getGraphicsComposite() {
		return Arrays.stream(this.compositeSashForm.getChildren()).filter(c -> c instanceof GraphicsComposite).map(c -> (GraphicsComposite) c).findFirst().orElseThrow(UnsupportedOperationException::new);
	}

	@Override
	protected Optional<SummaryComposite> getSummaryComposite() {
		return Arrays.stream(this.compositeSashForm.getChildren()).filter(c -> c instanceof SummaryComposite).map(c -> (SummaryComposite) c).findFirst();
	}

	@Override
	public void scrollSummaryComposite() {
		if (windowActor.getTrailRecordSet() == null) return ;
		if (windowActor.getTrailRecordSet().isSmartStatistics()) {
			int fixedTotalHeight = getSummaryComposite().orElseGet(null).getFixedTotalHeight();
			int actualHeight = Arrays.stream(this.compositeSashForm.getChildren()).filter(c -> c instanceof AbstractChartComposite) //
					.mapToInt(c -> c.getBounds().height).sum();
			int summaryWeight = fixedTotalHeight < actualHeight ? fixedTotalHeight * 1000 / actualHeight : 1000;
			if (compositeSashForm.getWeights()[0] == summaryWeight) {
				resizeCompositeSashForm(new int[] { 0, 1000 });
			} else if (compositeSashForm.getWeights()[0] == 0) {
				resizeCompositeSashForm(new int[] { 1000, 0 });
			} else {
				resizeCompositeSashForm(new int[] { summaryWeight, 1000 - summaryWeight });
			}
		} else {
			setTemplateChart();
		}
	}

	/**
	 * Set the graphics chart into the window without redrawing.
	 */
	@Override
	protected void setTemplateChart() {
		if (windowActor.getTrailRecordSet() == null) return ;
		resizeCompositeSashForm(windowActor.getTrailRecordSet().getChartWeights());
	}

	public int[] getChartWeights() {
		return compositeSashForm.getWeights();
	}

	public void setChartWeights(int[] chartWeights) {
		if (Thread.currentThread().getId() == DataExplorer.getInstance().getThreadId()) {
			resizeCompositeSashForm(chartWeights);
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					HistoSummaryWindow.this.resizeCompositeSashForm(chartWeights);
				}
			});
		}
	}

	private void resizeCompositeSashForm(int[] chartWeights) {
		compositeSashForm.setWeights(chartWeights);
		curveSelectorComposite.setVerticalBarVisible(chartWeights[0] == 0);
	}

	/**
	 * Redraw the graphics canvas as well as the curve selector table.
	 */
	@Override
	public void redrawGraphics(final boolean redrawCurveSelector) {

		if (Thread.currentThread().getId() == DataExplorer.getInstance().getThreadId()) {
			if (redrawCurveSelector) curveSelectorComposite.doUpdateCurveSelectorTable();
			if (windowActor.getTrailRecordSet() == null) return ;
			if (!windowActor.getTrailRecordSet().isSmartStatistics()) setTemplateChart();

			for (AbstractChartComposite c : getCharts()) {
				c.setFixedGraphicCanvas(curveSelectorComposite.getRealBounds());
				c.doRedrawGraphics();
				c.updateCaptions();
			}
			DataExplorer.getInstance().getPresentHistoExplorer().paintVolatileStatusMessage();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (redrawCurveSelector) curveSelectorComposite.doUpdateCurveSelectorTable();
					if (windowActor.getTrailRecordSet() == null) return ;
					if (!windowActor.getTrailRecordSet().isSmartStatistics()) setTemplateChart();

					for (AbstractChartComposite c : getCharts()) {
						c.setFixedGraphicCanvas(curveSelectorComposite.getRealBounds());
						c.doRedrawGraphics();
						c.updateCaptions();
					}
					DataExplorer.getInstance().getPresentHistoExplorer().paintVolatileStatusMessage();
				}
			});
		}
	}

	@Override
	public void enableGraphicsHeader(boolean enabled) {
		for (AbstractChartComposite c : getCharts()) {
			c.enableGraphicsHeader(enabled);
		}
	}

	@Override
	public void enableRecordSetComment(boolean enabled) {
		for (AbstractChartComposite c : getCharts()) {
			c.enableRecordSetComment(enabled);
		}
	}

	@Override
	public void clearHeaderAndComment() {
		for (AbstractChartComposite c : getCharts()) {
			c.clearHeaderAndComment();
		}
	}

	@Override
	public void enableGraphicsScale(boolean enabled) {
		for (AbstractChartComposite c : getCharts()) {
			c.enableGraphicsScale(enabled);
		}
	}

	@Override
	public void setCurveAreaBackground(Color curveAreaBackground) {
		for (AbstractChartComposite c : getCharts()) {
			c.curveAreaBackground = curveAreaBackground;
			c.graphicCanvas.redraw();
		}
	}

	@Override
	public void setCurveAreaBorderColor(Color borderColor) {
		for (AbstractChartComposite c : getCharts()) {
			c.curveAreaBorderColor = borderColor;
			c.graphicCanvas.redraw();
		}
	}

	@Override
	public void setSurroundingBackground(Color surroundingBackground) {
		for (AbstractChartComposite c : getCharts()) {
			c.surroundingBackground = surroundingBackground;
			c.setBackground(surroundingBackground);
			c.graphicsHeader.setBackground(surroundingBackground);
			c.recordSetComment.setBackground(surroundingBackground);
			c.doRedrawGraphics();
		}
	}

	/**
	 * Enable curve selector which relect to the sash form weights using the column widths.
	 */
	@Override
	public void enableCurveSelector(boolean enabled) {
		this.isCurveSelectorEnabled = enabled;
		if (enabled) {
			setSashFormWeights(this.curveSelectorComposite.getCompositeWidth());
		} else {
			setSashFormWeights(0);
		}
	}

	@Override
	public AbstractChartComposite[] getCharts() {
		return Arrays.stream(this.compositeSashForm.getChildren()).filter(c -> c instanceof AbstractChartComposite).map(c -> (AbstractChartComposite) c) //
				.toArray(AbstractChartComposite[]::new);
	}

	/**
	 * Update graphics window header and description.
	 */
	@Override
	@Deprecated
	public void updateCaptions() {
		if (Thread.currentThread().getId() == DataExplorer.getInstance().getThreadId()) {
			for (AbstractChartComposite c : getCharts()) {
				c.updateCaptions();
			}
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					for (AbstractChartComposite c : getCharts()) {
						c.updateCaptions();
					}
				}
			});
		}
	}

	/**
	 * Create visible tab window content as image.
	 * @return image with content
	 */
	@Override
	public Image getContentAsImage() {
		Rectangle bounds = this.graphicSashForm.getClientArea();
		Image tabContentImage = new Image(GDE.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.graphicSashForm.print(imageGC);
		if (GDE.IS_MAC) {
			this.graphicSashForm.print(imageGC);
			Image graphics = this.getGraphicsComposite().getGraphicsPrintImage();
			if (graphics != null) {
				imageGC.drawImage(graphics, bounds.width - graphics.getBounds().width, 0);
				graphics.dispose();
			}
		}
		imageGC.dispose();

		return tabContentImage;
	}

}
