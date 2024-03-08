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
package gde.ui.tab;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;

import gde.GDE;
import gde.config.Settings;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;
import gde.ui.menu.TabAreaContextMenu.TabMenuType;
import gde.utils.CellVoltageValues;

/**
 * Child display class displaying cell voltage display
 * @author Winfried Brügmann
 */
public class CellVoltageDisplay extends Composite {

	{
		//Register as a resource user - SWTResourceManager will
		//handle the obtaining and disposing of resources
		SWTResourceManager.registerResourceUser(this);
	}

	final static Logger	log						= Logger.getLogger(CellVoltageDisplay.class.getName());

	CLabel							cellTextLabel;
	CLabel							cellVoltageDigitalLabel;
	Canvas							cellCanvas;
	Composite						fillRight;
	Composite						fillLeft;
	Composite						cellComposite;
	Label 							upperVoltage, middleVoltage, lowerVoltage;
	
	final CellVoltageWindow				parent;
	final DataExplorer						application;
	final String									displayHeaderText;

	final Color										backgroundColor;
	final Menu										popupmenu;
	final TabAreaContextMenu			contextMenu;

	int									voltage = 0;
	int 								lastTop = 0;
	int 								lastVoltageLevel = 0;
	
	// all initial values fit to LiPo akku type
	final int lowerLimitColorRed;
	final int upperLimitColorRed; 		
	final int lowerLimitColorGreen; 	
	final int deltaSpreadVoltage = 50;
	final int beginSpreadVoltage; 		
	final int upperLimitVoltage; 		
	final int lowerLimitVoltage;


	public CellVoltageDisplay(DataExplorer currentApplication, Composite cellVoltageMainComposite, int measurementValue, String measurementName, String measurementUnit, CellVoltageWindow useParent) {
		super(cellVoltageMainComposite, SWT.BORDER);
		this.voltage = measurementValue;
		this.displayHeaderText = String.format("%s [%S]", measurementName, measurementUnit); //$NON-NLS-1$
		this.application = currentApplication;
		this.parent = useParent;
		this.backgroundColor = Settings.getInstance().getCellVoltageInnerAreaBackground();
		this.setBackground(this.backgroundColor);
		GridLayout mainCompositeLayout = new GridLayout();
		mainCompositeLayout.makeColumnsEqualWidth = true;
		mainCompositeLayout.verticalSpacing = 0;
		mainCompositeLayout.marginHeight = 0;
		mainCompositeLayout.marginWidth = 0;
		this.setLayout(mainCompositeLayout);
		int[] voltageLimits = CellVoltageValues.getVoltageLimits();
		this.upperLimitVoltage = voltageLimits[0];
		this.upperLimitColorRed = voltageLimits[1];
		this.lowerLimitColorGreen = voltageLimits[2];
		this.beginSpreadVoltage = voltageLimits[3];
		this.lowerLimitColorRed = voltageLimits[4];
		this.lowerLimitVoltage = voltageLimits[5];
		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.setMenu(this.popupmenu);
		this.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent evt) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "CellVoltageDisplay.helpRequested " + evt); //$NON-NLS-1$
				DataExplorer.getInstance().openHelpDialog("", "HelpInfo_9.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		
		this.contextMenu = new TabAreaContextMenu();
		this.contextMenu.createMenu(this.popupmenu, TabMenuType.SIMPLE);
	}

	public void create() {
		{
			this.cellTextLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.cellTextLabel.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.cellTextLabel.setBackground(this.backgroundColor);
			this.cellTextLabel.setText(this.displayHeaderText);
			this.cellTextLabel.setMenu(this.popupmenu);
			GridData text1LData = new GridData();
			text1LData.horizontalAlignment = GridData.FILL;
			text1LData.grabExcessHorizontalSpace = true;
			this.cellTextLabel.setLayoutData(text1LData);
		}
		{
			this.cellVoltageDigitalLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.cellVoltageDigitalLabel.setBackground(this.backgroundColor);
			this.cellVoltageDigitalLabel.setMenu(this.popupmenu);
			this.cellVoltageDigitalLabel.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + 22, SWT.NORMAL));
			this.cellVoltageDigitalLabel.setText("0,00"); //$NON-NLS-1$
			GridData actualDigitalLabelLData = new GridData();
			actualDigitalLabelLData.horizontalAlignment = GridData.FILL;
			actualDigitalLabelLData.grabExcessHorizontalSpace = true;
			this.cellVoltageDigitalLabel.setLayoutData(actualDigitalLabelLData);
			//this.actualDigitalLabel.setBounds(0, 60, 50, 60);
		}
		{
			this.cellComposite = new Composite(this, SWT.NONE);
			GridData canvas1LData = new GridData();
			canvas1LData.horizontalAlignment = GridData.FILL;
			canvas1LData.grabExcessHorizontalSpace = true;
			canvas1LData.grabExcessVerticalSpace = true;
			canvas1LData.verticalAlignment = GridData.FILL;
			this.cellComposite.setLayoutData(canvas1LData);
			FillLayout canvas1Layout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
			canvas1Layout.marginHeight = 0;
			canvas1Layout.marginWidth = 0;
			this.cellComposite.setLayout(canvas1Layout);
			this.cellComposite.setMenu(this.popupmenu);
			{
				this.fillLeft = new Composite(this.cellComposite, SWT.NONE);
				this.fillLeft.setDragDetect(false);
				this.fillLeft.setEnabled(false);
				this.fillLeft.setBackground(this.backgroundColor);
				this.upperVoltage = new Label(this.fillLeft, SWT.NONE);
				this.upperVoltage.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.middleVoltage = new Label(this.fillLeft, SWT.NONE);
				this.middleVoltage.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.lowerVoltage = new Label(this.fillLeft, SWT.NONE);
				this.lowerVoltage.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			}
			{
				this.cellCanvas = new Canvas(this.cellComposite, SWT.NONE);
				this.cellCanvas.setDragDetect(false);
				this.cellCanvas.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "cellCanvas.paintControl, evt = " + evt); //$NON-NLS-1$
						voltagePaintControl(evt.gc);
					}
				});
			}
			{
				this.fillRight = new Composite(this.cellComposite, SWT.NONE);
				this.fillRight.setEnabled(false);
				this.fillRight.setDragDetect(false);
				this.fillRight.setBackground(this.backgroundColor);
			}
		}
		this.layout();
	}

	public int getVoltage() {
		return this.voltage;
	}
	
	/**
	 * @param newVoltage the voltage to set
	 */
	public void setVoltage(int newVoltage) {
		//this.layout(true);
		if (this.voltage != newVoltage) {
			this.voltage = newVoltage;
			String valueText = String.format("%.3f", Double.valueOf(this.voltage / 1000.0)); //$NON-NLS-1$
			this.cellVoltageDigitalLabel.setText(valueText);

			Rectangle rect = this.cellCanvas.getClientArea();
			Point topHeight = calculateBarGraph(rect);
			if (this.lastVoltageLevel != checkVoltageLevel()) {
				this.cellCanvas.redraw();
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, newVoltage + " redraw "+ ", " + topHeight.x + " -> " + topHeight.y); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			else if (this.lastTop < topHeight.x) {
				int top = this.lastTop-1 < 0 ? 0 : this.lastTop-1;
				int height = topHeight.x+1 > rect.height-1 ? rect.height-1 : topHeight.x+1;
				this.cellCanvas.redraw(0, top, rect.width-1, height, false);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, newVoltage + " redraw "+ ", " + top + " -> " + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			else {
				int top = topHeight.x-1 < 0 ? 0 : topHeight.x-1;
				int height = this.lastTop+1 > rect.height-1 ? rect.height-1 : this.lastTop+1;
				this.cellCanvas.redraw(0, top, rect.width-1, height, false); 
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, newVoltage + " redraw "+ ", " + top + " -> " + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}

	/**
	 * 
	 */
	void voltagePaintControl(GC gc) {
		//this.cellTextLabel.setText(this.displayHeaderText);
		String valueText = String.format("%.3f", Double.valueOf(this.voltage / 1000.0)); //$NON-NLS-1$
		this.cellVoltageDigitalLabel.setText(valueText);

		Rectangle rect = this.cellCanvas.getClientArea();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "cellCanvas.getBounds = " + rect); //$NON-NLS-1$
		Point topHeight = calculateBarGraph(rect);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, valueText + " redraw "+ ", " + topHeight.x + " -> " + topHeight.y); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		rect = new Rectangle(0, topHeight.x, rect.width-1, topHeight.y);
		
		//if (this.lastTop != topHeight.x) {
		this.upperVoltage.setLocation(10, rect.x);
		this.upperVoltage.setText(String.format("%.1f", upperLimitVoltage/1000.0));
		this.middleVoltage.setLocation(10, 200);
		this.middleVoltage.setText(String.format("%.1f", (lowerLimitVoltage+(upperLimitVoltage-lowerLimitVoltage)/2)/1000.0));
		this.lowerVoltage.setLocation(10, 300);
		this.lowerVoltage.setText(String.format("%.1f", lowerLimitVoltage/1000.0));
		//GC gc = new GC(this.cellCanvas); 

		this.lastVoltageLevel = checkVoltageLevel();
		switch (this.lastVoltageLevel) {
		case 1: // < 2600 || > 4200
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_RED));
			break;
		default:
		case 2: //2600 - 4200
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_YELLOW));
			break;
		case 3: //== 4200
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_GREEN));
			break;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "fillRectangle = " + rect); //$NON-NLS-1$
		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		gc.drawLine(1, this.lastTop, rect.width-1, this.lastTop);
		this.lastTop = topHeight.x;
		gc.fillRectangle(1, topHeight.x, rect.width-1, topHeight.y);
		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		gc.drawLine(1, topHeight.x, rect.width-1, topHeight.x);
		gc.drawLine(0, 0, rect.width, 0);
		gc.drawLine(rect.width, 0, rect.width, rect.height+topHeight.x);
		gc.drawLine(0, 0, 0, rect.height+topHeight.x);
		//gc.drawRectangle(0, 0, rect.width, rect.height+topHeight.x);
		
		//gc.dispose();
	}

	/**
	 * @return 1 if voltage <2600 | >4210, 2 if voltage between 2600 and 4190, 3 for voltage 4191 <-> 4209
	 */
	int checkVoltageLevel() {
		int voltageLevel;
		if (CellVoltageDisplay.this.voltage < this.lowerLimitColorRed || CellVoltageDisplay.this.voltage > this.upperLimitColorRed) {
			voltageLevel = 1; //red
		}
		else if (CellVoltageDisplay.this.voltage >= this.lowerLimitColorRed && CellVoltageDisplay.this.voltage <= this.lowerLimitColorGreen) {
			voltageLevel = 2; // yellow
		}
		else { // 4191 <-> 4209
			voltageLevel = 3; // green
		}
		return voltageLevel;
	}

	/**
	 * @param cellCanvasBounds
	 * @return top limit and botom line position within the cellCanvas bounds
	 */
	Point calculateBarGraph(Rectangle cellCanvasBounds) {
		Point topHeight = new Point(0,0);
		int baseVoltage = this.lowerLimitVoltage;

		// spread display if voltage average is greater than 4.0 V and delta between cell voltages lower than 0.1 V
		if (CellVoltageDisplay.this.parent.getVoltageDelta() < this.deltaSpreadVoltage && CellVoltageDisplay.this.parent.getVoltageAvg() > this.beginSpreadVoltage) {
			baseVoltage = this.beginSpreadVoltage;
		}

		// 4,2 - 2,5  = 1,7 (max voltage - min voltage)
		Double delta = cellCanvasBounds.height / (1.0 * this.upperLimitVoltage - baseVoltage) * (1.0 * this.upperLimitVoltage - CellVoltageDisplay.this.voltage);
		
		topHeight.x = (int)(delta+0.5);
		topHeight.y = cellCanvasBounds.height-topHeight.x;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, topHeight.toString());
		return topHeight;
	}
}
