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

import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import gde.GDE;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * Histo graphics window as a sash form of a curve selection table and a drawing canvas.
 * @author Thomas Eickert
 */
public final class HistoGraphicsWindow extends AbstractChartWindow {
	private final static String				$CLASS_NAME	= HistoGraphicsWindow.class.getName();
	final static Logger								log					= Logger.getLogger($CLASS_NAME);

	protected AbstractChartComposite	graphicsComposite;

	private HistoGraphicsWindow(CTabFolder currentDisplayTab, int style, int index) {
		super(currentDisplayTab, style, index);
	}

	public static HistoGraphicsWindow create(CTabFolder dataTab, int style, int position) {
		HistoGraphicsWindow window = new HistoGraphicsWindow(dataTab, style, position);

		window.graphicSashForm = new SashForm(window.tabFolder, SWT.HORIZONTAL);
		window.setControl(window.graphicSashForm);

		window.curveSelectorComposite = new SelectorComposite(window.graphicSashForm, window);
		window.graphicsComposite = new GraphicsComposite(window.graphicSashForm, window);
		try {
			if (GDE.shell.getClientArea().width > SELECTOR_WIDTH) //Linux call this with 0,0
				window.graphicSashForm.setWeights(new int[] { SELECTOR_WIDTH, GDE.shell.getClientArea().width - SELECTOR_WIDTH });
		} catch (IllegalArgumentException e) {
			log.log(WARNING, "graphicSashForm.setWeights(this.sashFormWeights) failed!", e); //$NON-NLS-1$
		}

		window.setFont(SWTResourceManager.getFont(DataExplorer.getInstance(), GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 3 : 1), SWT.NORMAL));
		window.setText(Messages.getString(MessageIds.GDE_MSGT0883));
		return window;
	}

	@Override
	protected GraphicsComposite getGraphicsComposite() {
		return (GraphicsComposite) this.graphicsComposite;
	}

	@Override
	protected Optional<SummaryComposite> getSummaryComposite() {
		return Optional.empty();
	}

	@Override
	public void scrollSummaryComposite() {
		// not required for this chart type
	}

	@Override
	public void redrawGraphics(final boolean redrawCurveSelector) {
		if (windowActor.getTrailRecordSet() == null) return ;

		if (Thread.currentThread().getId() == DataExplorer.getInstance().getThreadId()) {
			if (redrawCurveSelector) this.curveSelectorComposite.doUpdateCurveSelectorTable();

			this.graphicsComposite.doRedrawGraphics();
			this.graphicsComposite.updateCaptions();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (redrawCurveSelector) curveSelectorComposite.doUpdateCurveSelectorTable();

					graphicsComposite.doRedrawGraphics();
					graphicsComposite.updateCaptions();
				}
			});
		}
	}

	/**
	 * Update graphics window header and description.
	 */
	@Override
	@Deprecated
	public void updateCaptions() {
		if (Thread.currentThread().getId() == DataExplorer.getInstance().getThreadId()) {
			this.graphicsComposite.updateCaptions();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					graphicsComposite.updateCaptions();
				}
			});
		}
	}

	@Override
	public void enableRecordSetComment(boolean enabled) {
		this.graphicsComposite.enableRecordSetComment(enabled);
	}

	@Override
	public void clearHeaderAndComment() {
		this.graphicsComposite.clearHeaderAndComment();
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
			Image graphics = this.graphicsComposite.getGraphicsPrintImage();
			if (graphics != null) {
				imageGC.drawImage(graphics, bounds.width - graphics.getBounds().width, 0);
				graphics.dispose();
			}
		}
		imageGC.dispose();

		return tabContentImage;
	}

	@Override
	public void enableGraphicsHeader(boolean enabled) {
		this.graphicsComposite.enableGraphicsHeader(enabled);
	}

	@Override
	public void enableGraphicsScale(boolean enabled) {
		this.graphicsComposite.enableGraphicsScale(enabled);
	}

	@Override
	public void setCurveAreaBackground(Color curveAreaBackground) {
		this.graphicsComposite.curveAreaBackground = curveAreaBackground;
		this.graphicsComposite.graphicCanvas.redraw();
	}

	@Override
	public void setCurveAreaBorderColor(Color borderColor) {
		this.graphicsComposite.curveAreaBorderColor = borderColor;
		this.graphicsComposite.graphicCanvas.redraw();
	}

	@Override
	public void setSurroundingBackground(Color surroundingBackground) {
		this.graphicsComposite.surroundingBackground = surroundingBackground;
		this.graphicsComposite.setBackground(surroundingBackground);
		this.graphicsComposite.graphicsHeader.setBackground(surroundingBackground);
		this.graphicsComposite.recordSetComment.setBackground(surroundingBackground);
		this.graphicsComposite.doRedrawGraphics();
	}

	@Override
	public AbstractChartComposite[] getCharts() {
		return new AbstractChartComposite[] { graphicsComposite };
	}

}
