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
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.ui.tab;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channels;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.tab.GraphicsWindow.GraphicsType;
import gde.utils.TimeLine;

/**
 * This class defines the main graphics window as a sash form of a curve selection table and a drawing canvas
 * @author Winfried Brügmann
 */
public class UtilGraphicsWindow extends CTabItem {
	final static Logger						log											= Logger.getLogger(UtilGraphicsWindow.class.getName());

	final CTabFolder							tabFolder;
	
	SashForm											graphicSashForm;
	boolean												isCurveSelectorEnabled	= true;
	int[]													sashFormWeights					= new int[] { 100, 1000 };
	
	// Curve Selector Table with popup menu
	SelectorComposite							curveSelectorComposite;

	// drawing canvas
	GraphicsComposite							graphicsComposite;

	final DataExplorer						application;
	final Channels								channels;
	final Settings								settings;
	final String									tabName;
	final TimeLine								timeLine								= new TimeLine();
	final GraphicsType											graphicsType;
	
	public UtilGraphicsWindow(CTabFolder currentDisplayTab, int style, GraphicsType currentType, String useTabName, int index) {
		super(currentDisplayTab, style, index);
		SWTResourceManager.registerResourceUser(this);
		this.application = DataExplorer.getInstance();
		this.tabFolder = currentDisplayTab;
		this.graphicsType = currentType;
		this.tabName = useTabName;
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
		this.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 3 : 1), SWT.NORMAL));
		this.setText(this.tabName);
	}

	public void create() {
		// graphicSashForm
		this.graphicSashForm = new SashForm(this.tabFolder, SWT.HORIZONTAL);
		this.setControl(this.graphicSashForm);

		{ // curveSelector
			this.curveSelectorComposite = new SelectorComposite(this.graphicSashForm, this.graphicsType, "  " + Messages.getString(MessageIds.GDE_MSGT0254));
		} // curveSelector

		{ // graphics composite
			this.graphicsComposite = new GraphicsComposite(this.graphicSashForm, this.graphicsType);
		} // graphics composite

		// graphicSashForm
	}

	/**
	 * query if this component is visible 
	 * @return true if graphics window is visible
	 */
	public boolean isVisible() {
		return this.graphicsComposite.isVisible();
	}
	
	/**
	 * redraws the graphics canvas as well as the curve selector table
	 */
	public void redrawGraphics() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.graphicsComposite.doRedrawGraphics();
			this.curveSelectorComposite.doUpdateCurveSelectorTable();
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					UtilGraphicsWindow.this.graphicsComposite.doRedrawGraphics();
					UtilGraphicsWindow.this.curveSelectorComposite.doUpdateCurveSelectorTable();
				}
			});
		}
	}

	/**
	 * method to update the curves displayed in the curve selector panel 
	 */
	public void updateCurveSelectorTable() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.curveSelectorComposite.doUpdateCurveSelectorTable();
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					UtilGraphicsWindow.this.curveSelectorComposite.doUpdateCurveSelectorTable();
				}
			});
		}
	}

	/**
	 * set the sashForm weights
	 * @param newSelectorCompositeWidth the changed curve selector width
	 */
	public void setSashFormWeights(int newSelectorCompositeWidth) {
		int tabFolderClientAreaWidth = this.tabFolder.getBounds().width;
		// begin workaround: sometimes tabFolder.getClientArea().width returned values greater than screen size ???? 
		int bestGuessWidth = this.application.getClientArea().width;
		if (tabFolderClientAreaWidth > bestGuessWidth) {
			log.log(Level.WARNING, "tabFolder clientAreaWidth missmatch, tabFolderWidth = " + tabFolderClientAreaWidth + " vs applicationWidth = " + bestGuessWidth);
			tabFolderClientAreaWidth = bestGuessWidth;
			this.tabFolder.setSize(tabFolderClientAreaWidth, this.tabFolder.getBounds().height);
		}
		// end workaround: sometimes tabFolder.getClientArea().width returned values greater than screen size ???? 
		int[] newWeights = new int[] { newSelectorCompositeWidth, tabFolderClientAreaWidth - newSelectorCompositeWidth};
		if (this.sashFormWeights[0] != newWeights[0] || this.sashFormWeights[1] != newWeights[1]) {
			this.sashFormWeights = newWeights;
			this.graphicSashForm.setWeights(this.sashFormWeights);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "sash weight = " + this.sashFormWeights[0] + ", " + this.sashFormWeights[1] + " tabFolderClientAreaWidth = " + tabFolderClientAreaWidth + " graphicsType = " + this.graphicsType);
		}
	}
	
	/**
	 * query the width
	 */
	public int getWidth() {
		return this.graphicSashForm.getSize().x;
	}

	/**
	 * @return the selectorColumnWidth
	 */
	public int[] getSashFormWeights() {
		return this.sashFormWeights;
	}

	/**
	 * @return true if selector is enabled 
	 */
	public boolean isCurveSelectorEnabled() {
		return this.isCurveSelectorEnabled;
	}

	/**
	 * enable curve selector which relect to the sash form weights using the selector colum width
	 * @param enabled
	 */
	public void setCurveSelectorEnabled(boolean enabled) {
		this.isCurveSelectorEnabled = enabled;
		if (enabled)
			setSashFormWeights(this.curveSelectorComposite.getSelectorColumnWidth());
		else 
			setSashFormWeights(0);
	}

	public SashForm getGraphicSashForm() {
		return this.graphicSashForm;
	}
	
	/**
	 * enable display of graphics header
	 */
	public void enableGraphicsHeader(boolean enabled) {
		this.graphicsComposite.enableGraphicsHeader(enabled);
	}

	/**
	 * enable display of record set comment
	 */
	public void enableRecordSetComment(boolean enabled) {
		this.graphicsComposite.enableRecordSetComment(enabled);
	}
	
	public void clearHeaderAndComment() {
		this.graphicsComposite.clearHeaderAndComment();
	}
	
	/**
	 * @return the curveSelectorComposite
	 */
	public SelectorComposite getCurveSelectorComposite() {
		return this.curveSelectorComposite;
	}

	/**
	 * @return the graphicsComposite
	 */
	public GraphicsComposite getGraphicsComposite() {
		return this.graphicsComposite;
	}

	/**
	 * query the context menu activation state
	 * @return state true/false
	 */
	public boolean isActiveCurveSelectorContextMenu() {
		return this.curveSelectorComposite.contextMenu.isActive();
	}
	
	/**
	 * create visible tab window content as image
	 * @return image with content
	 */
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
	
	/**
	 * set the curve graphics background color
	 * @param curveAreaBackground the curveAreaBackground to set
	 */
	public void setCurveAreaBackground(Color curveAreaBackground) {
		this.graphicsComposite.curveAreaBackground = curveAreaBackground;
		this.graphicsComposite.graphicCanvas.redraw();
	}
	
	/**
	 * set the curve graphics background color
	 * @param borderColor the curveAreaBackground to set
	 */
	public void setCurveAreaBorderColor(Color borderColor) {
		this.graphicsComposite.curveAreaBorderColor = borderColor;
		this.graphicsComposite.graphicCanvas.redraw();
	}

	/**
	 * set the curve graphics surrounding background color
	 * @param surroundingBackground the surroundingBackground to set
	 */
	public void setSurroundingBackground(Color surroundingBackground) {
		this.graphicsComposite.surroundingBackground = surroundingBackground;
		this.graphicsComposite.setBackground(surroundingBackground);
		this.graphicsComposite.graphicsHeader.setBackground(surroundingBackground);
		this.graphicsComposite.recordSetComment.setBackground(surroundingBackground);
		this.graphicsComposite.doRedrawGraphics();
	}

	/**
	 * @return the type (TYPE_NORMAL or TYPE_COMPARE)
	 */
	public GraphicsType getGraphicsType() {
		return this.graphicsType;
	}
}
