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
package gde.ui.dialog;

import java.util.Locale;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import gde.GDE;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.CellVoltageValues;
import gde.utils.CellVoltageValues.CellVoltageTypes;

/**
 * Dialog class to adjust the voltage levels of the bar graph
 * @author Winfried Brügmann
 */
public class CellVoltageValuesDialog extends org.eclipse.swt.widgets.Dialog {
	final static String					$CLASS_NAME						= CellVoltageValuesDialog.class.getName();
	final public static Logger	log										= Logger.getLogger(CellVoltageValuesDialog.$CLASS_NAME);

	Shell												dialogShell;
	Group												presetsGroup;
	CLabel											upperLimitVoltageLabel;
	CCombo											upperLimitVoltageCombo;
	CLabel											lowerLimitVoltageLabel;
	CCombo											lowerLimitVoltageCombo;
	CLabel											lowerLimitRedLabel;
	Composite										lableComboComposite;
	Composite										lowerSpacer;
	Composite										upperSpacer;
	CCombo											beginSpreadVoltageCombo;
	CLabel											beginSpreadVoltageLabel;
	CCombo											lowerLimitColorGreenCombo;
	CLabel											lowerLimitColorGreenLabel;
	CCombo											lowerLimitColorRedCombo;
	Group												individualGroup;
	Button											buttonLiPo;
	Button											buttonLiFe;
	Button											buttonNiMh;
	CLabel											upperLimitColorRedLabel;
	CCombo											upperLimitColorRedCombo;
	Composite										lowerLimitRed;
	Composite										yellowField;
	Composite										green;
	Composite										upperLimitRed;
	Composite										coverComposite;
	Button											buttonLiIo;
	Button											okButton;

	int[]												voltageLimits;

	final DataExplorer 					application;

	public CellVoltageValuesDialog(DataExplorer currentApplication, int style) {
		super(currentApplication.getShell(), style);
		this.application = currentApplication;
		this.voltageLimits = this.application.getActiveRecordSet() != null 
				? this.application.getActiveRecordSet().getVoltageLimits() : CellVoltageValues.getVoltageLimits();
	}

	public int[] open() {
		final String $METHOD_NAME = "open"; //$NON-NLS-1$
		try {
			Shell parent = getParent();
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			SWTResourceManager.registerResourceUser(this.dialogShell);
			FormLayout dialogShellLayout = new FormLayout();
			this.dialogShell.setLayout(dialogShellLayout);
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(380, 380);
			this.dialogShell.setText(Messages.getString(MessageIds.GDE_MSGT0376));
			this.dialogShell.setBackground(this.application.COLOR_CANVAS_YELLOW);
			this.dialogShell.addListener(SWT.Traverse, new Listener() {
	      @Override
				public void handleEvent(Event event) {
	        switch (event.detail) {
	        case SWT.TRAVERSE_ESCAPE:
	        	CellVoltageValuesDialog.this.dialogShell.close();
	          event.detail = SWT.TRAVERSE_NONE;
	          event.doit = false;
	          break;
	        }
	      }
	    });
			this.dialogShell.addDisposeListener( new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent event) {
					CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "dialogShell.disposeListener, event=" + event); //$NON-NLS-1$
					if (CellVoltageValuesDialog.this.voltageLimits[5] < CellVoltageValuesDialog.this.voltageLimits[0] && (CellVoltageValuesDialog.this.voltageLimits[0] - CellVoltageValuesDialog.this.voltageLimits[5]) >= 2
							&& CellVoltageValuesDialog.this.voltageLimits[4] > 0 && CellVoltageValuesDialog.this.voltageLimits[1] > CellVoltageValuesDialog.this.voltageLimits[4]
							&& CellVoltageValuesDialog.this.voltageLimits[2] > CellVoltageValuesDialog.this.voltageLimits[5]
							&& CellVoltageValuesDialog.this.voltageLimits[2] < CellVoltageValuesDialog.this.voltageLimits[0] && CellVoltageValuesDialog.this.voltageLimits[3] < CellVoltageValuesDialog.this.voltageLimits[0]
							&& CellVoltageValuesDialog.this.voltageLimits[3] > CellVoltageValuesDialog.this.voltageLimits[5]) {
						CellVoltageValues.setVoltageLimits(
								CellVoltageValuesDialog.this.voltageLimits[0],
								CellVoltageValuesDialog.this.voltageLimits[1],
								CellVoltageValuesDialog.this.voltageLimits[2],
								CellVoltageValuesDialog.this.voltageLimits[3],
								CellVoltageValuesDialog.this.voltageLimits[4],
								CellVoltageValuesDialog.this.voltageLimits[5]);
						//{upperLimitVoltage=0,  upperLimitColorRed=1, lowerLimitColorGreen=2, beginSpreadVoltage=3, lowerLimitColorRed=4, lowerLimitVoltage=5};
					}
					CellVoltageValuesDialog.this.application.updateCellVoltageLimitsSelector();
				}
			});
			{
				this.presetsGroup = new Group(this.dialogShell, SWT.BORDER);
				this.presetsGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				GridLayout defaultSelectionCompositeLayout1 = new GridLayout();
				defaultSelectionCompositeLayout1.makeColumnsEqualWidth = true;
				this.presetsGroup.setLayout(defaultSelectionCompositeLayout1);
				GridLayout defaultSelectionCompositeLayout = new GridLayout();
				defaultSelectionCompositeLayout.makeColumnsEqualWidth = true;
				FormData defaultSelectionCompositeLData = new FormData();
				defaultSelectionCompositeLData.right = new FormAttachment(300, 1000, 0);
				defaultSelectionCompositeLData.left = new FormAttachment(0, 1000, 0);
				defaultSelectionCompositeLData.top = new FormAttachment(0, 1000, 0);
				defaultSelectionCompositeLData.bottom = new FormAttachment(1000, 1000, -35);
				this.presetsGroup.setLayoutData(defaultSelectionCompositeLData);
				this.presetsGroup.setBackground(this.application.COLOR_CANVAS_YELLOW);
				this.presetsGroup.setText(Messages.getString(MessageIds.GDE_MSGT0374));
				{
					this.upperSpacer = new Composite(this.presetsGroup, SWT.NONE);
					GridLayout upperSpacerLayout = new GridLayout();
					upperSpacerLayout.makeColumnsEqualWidth = true;
					GridData upperSpacerLData = new GridData();
					upperSpacerLData.horizontalAlignment = GridData.FILL;
					upperSpacerLData.grabExcessHorizontalSpace = true;
					upperSpacerLData.verticalAlignment = GridData.BEGINNING;
					this.upperSpacer.setLayoutData(upperSpacerLData);
					this.upperSpacer.setLayout(upperSpacerLayout);
					this.upperSpacer.setBackground(this.application.COLOR_CANVAS_YELLOW);
				}
				{
					this.buttonLiPo = new Button(this.presetsGroup, SWT.TOGGLE | SWT.CENTER);
					GridData buttonLiPoLData = new GridData();
					buttonLiPoLData.grabExcessHorizontalSpace = true;
					buttonLiPoLData.horizontalAlignment = GridData.FILL;
					buttonLiPoLData.verticalAlignment = GridData.FILL;
					buttonLiPoLData.grabExcessVerticalSpace = true;
					this.buttonLiPo.setLayoutData(buttonLiPoLData);
					this.buttonLiPo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.buttonLiPo.setText(Messages.getString(MessageIds.GDE_MSGT0371));
					this.buttonLiPo.setSelection(this.voltageLimits[0] == CellVoltageValues.liPoLimits[0]);
					this.buttonLiPo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "buttonLiPo.widgetSelected, event=" + evt); //$NON-NLS-1$
							CellVoltageValuesDialog.this.buttonNiMh.setSelection(false);
							CellVoltageValuesDialog.this.buttonLiPo.setSelection(true);
							CellVoltageValuesDialog.this.buttonLiIo.setSelection(false);
							CellVoltageValuesDialog.this.buttonLiFe.setSelection(false);

							CellVoltageValuesDialog.this.voltageLimits = CellVoltageValues.getVoltageLimits(CellVoltageTypes.LiPo);
							CellVoltageValuesDialog.this.individualGroup.notifyListeners(SWT.Paint, new Event());
						}
					});
				}
				{
					this.buttonLiIo = new Button(this.presetsGroup, SWT.TOGGLE | SWT.CENTER);
					GridData buttonLiIoLData = new GridData();
					buttonLiIoLData.horizontalAlignment = GridData.FILL;
					buttonLiIoLData.verticalAlignment = GridData.FILL;
					buttonLiIoLData.grabExcessVerticalSpace = true;
					this.buttonLiIo.setLayoutData(buttonLiIoLData);
					this.buttonLiIo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.buttonLiIo.setText(Messages.getString(MessageIds.GDE_MSGT0372));
					this.buttonLiIo.setSelection(this.voltageLimits[0] == CellVoltageValues.liIoLimits[0]);
					this.buttonLiIo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "buttonLiIo.widgetSelected, event=" + evt); //$NON-NLS-1$
							CellVoltageValuesDialog.this.buttonNiMh.setSelection(false);
							CellVoltageValuesDialog.this.buttonLiIo.setSelection(true);
							CellVoltageValuesDialog.this.buttonLiPo.setSelection(false);
							CellVoltageValuesDialog.this.buttonLiFe.setSelection(false);

							CellVoltageValuesDialog.this.voltageLimits = CellVoltageValues.getVoltageLimits(CellVoltageTypes.LiIo);
							CellVoltageValuesDialog.this.individualGroup.notifyListeners(SWT.Paint, new Event());
						}
					});
				}
				{
					this.buttonLiFe = new Button(this.presetsGroup, SWT.TOGGLE | SWT.CENTER);
					GridData buttonLiFeLData = new GridData();
					buttonLiFeLData.horizontalAlignment = GridData.FILL;
					buttonLiFeLData.verticalAlignment = GridData.FILL;
					buttonLiFeLData.grabExcessVerticalSpace = true;
					buttonLiFeLData.grabExcessHorizontalSpace = true;
					this.buttonLiFe.setLayoutData(buttonLiFeLData);
					this.buttonLiFe.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.buttonLiFe.setText(Messages.getString(MessageIds.GDE_MSGT0373));
					this.buttonLiFe.setSelection(this.voltageLimits[0] == CellVoltageValues.liFeLimits[0]);
					this.buttonLiFe.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "buttonLiFe.widgetSelected, event=" + evt); //$NON-NLS-1$
							CellVoltageValuesDialog.this.buttonNiMh.setSelection(false);
							CellVoltageValuesDialog.this.buttonLiFe.setSelection(true);
							CellVoltageValuesDialog.this.buttonLiIo.setSelection(false);
							CellVoltageValuesDialog.this.buttonLiPo.setSelection(false);

							CellVoltageValuesDialog.this.voltageLimits = CellVoltageValues.getVoltageLimits(CellVoltageTypes.LiFe);
							CellVoltageValuesDialog.this.individualGroup.notifyListeners(SWT.Paint, new Event());
						}
					});
				}
				{
					this.buttonNiMh = new Button(this.presetsGroup, SWT.TOGGLE | SWT.CENTER);
					GridData buttonLiFeLData = new GridData();
					buttonLiFeLData.horizontalAlignment = GridData.FILL;
					buttonLiFeLData.verticalAlignment = GridData.FILL;
					buttonLiFeLData.grabExcessVerticalSpace = true;
					buttonLiFeLData.grabExcessHorizontalSpace = true;
					this.buttonNiMh.setLayoutData(buttonLiFeLData);
					this.buttonNiMh.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.buttonNiMh.setText(Messages.getString(MessageIds.GDE_MSGT0377));
					this.buttonNiMh.setSelection(this.voltageLimits[0] == CellVoltageValues.niMhLimits[0]);
					this.buttonNiMh.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "buttonLiFe.widgetSelected, event=" + evt); //$NON-NLS-1$
							CellVoltageValuesDialog.this.buttonNiMh.setSelection(true);
							CellVoltageValuesDialog.this.buttonLiFe.setSelection(false);
							CellVoltageValuesDialog.this.buttonLiIo.setSelection(false);
							CellVoltageValuesDialog.this.buttonLiPo.setSelection(false);

							CellVoltageValuesDialog.this.voltageLimits = CellVoltageValues.getVoltageLimits(CellVoltageTypes.NiMh);
							CellVoltageValuesDialog.this.individualGroup.notifyListeners(SWT.Paint, new Event());
						}
					});
				}
				{
					this.lowerSpacer = new Composite(this.presetsGroup, SWT.NONE);
					GridLayout lowerSpacerLayout = new GridLayout();
					lowerSpacerLayout.makeColumnsEqualWidth = true;
					GridData lowerSpacerLData = new GridData();
					lowerSpacerLData.horizontalAlignment = GridData.FILL;
					lowerSpacerLData.grabExcessHorizontalSpace = true;
					lowerSpacerLData.verticalAlignment = GridData.END;
					this.lowerSpacer.setLayoutData(lowerSpacerLData);
					this.lowerSpacer.setLayout(lowerSpacerLayout);
					this.lowerSpacer.setBackground(this.application.COLOR_CANVAS_YELLOW);
				}
			}
			{
				this.individualGroup = new Group(this.dialogShell, SWT.BORDER);
				FormData valueSelectionCompositeLData = new FormData();
				valueSelectionCompositeLData.top = new FormAttachment(0, 1000, 0);
				valueSelectionCompositeLData.right = new FormAttachment(1000, 1000, 0);
				valueSelectionCompositeLData.left = new FormAttachment(this.presetsGroup);
				valueSelectionCompositeLData.bottom = new FormAttachment(1000, 1000, -35);
				this.individualGroup.setLayoutData(valueSelectionCompositeLData);
				this.individualGroup.setBackground(this.application.COLOR_CANVAS_YELLOW);
				this.individualGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.individualGroup.setText(Messages.getString(MessageIds.GDE_MSGT0375));
				this.individualGroup.addHelpListener(new HelpListener() {
					@Override
					public void helpRequested(HelpEvent evt) {
						CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "individualGroup.helpRequested, event=" + evt); //$NON-NLS-1$
					}
				});
				this.individualGroup.addPaintListener(new PaintListener() {
					@Override
					public void paintControl(PaintEvent evt) {
						CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "lableComboComposite.paintControl, event=" + evt); //$NON-NLS-1$
						if (evt.gc == null)
							evt.gc = new GC(CellVoltageValuesDialog.this.individualGroup);
						evt.gc.drawLine(100, 40, 145, 35);
						evt.gc.drawLine(100, 61, 145, 83);
						evt.gc.drawLine(100, 92, 145, 132);
						evt.gc.drawLine(100, 143, 145, 180);
						evt.gc.drawLine(100, 252, 145, 227);
						evt.gc.drawLine(100, 278, 145, 275);
						CellVoltageValuesDialog.this.upperLimitVoltageCombo.select(matchValueToSelection(CellVoltageValuesDialog.this.upperLimitVoltageCombo, CellVoltageValuesDialog.this.voltageLimits[0]));
						CellVoltageValuesDialog.this.upperLimitColorRedCombo.select(matchValueToSelection(CellVoltageValuesDialog.this.upperLimitColorRedCombo, CellVoltageValuesDialog.this.voltageLimits[1]));
						CellVoltageValuesDialog.this.lowerLimitColorGreenCombo.select(matchValueToSelection(CellVoltageValuesDialog.this.lowerLimitColorGreenCombo, CellVoltageValuesDialog.this.voltageLimits[2]));
						CellVoltageValuesDialog.this.beginSpreadVoltageCombo.select(matchValueToSelection(CellVoltageValuesDialog.this.beginSpreadVoltageCombo, CellVoltageValuesDialog.this.voltageLimits[3]));
						CellVoltageValuesDialog.this.lowerLimitColorRedCombo.select(matchValueToSelection(CellVoltageValuesDialog.this.lowerLimitColorRedCombo, CellVoltageValuesDialog.this.voltageLimits[4]));
						CellVoltageValuesDialog.this.lowerLimitVoltageCombo.select(matchValueToSelection(CellVoltageValuesDialog.this.lowerLimitVoltageCombo, CellVoltageValuesDialog.this.voltageLimits[5]));
					}
				});
				{
					this.coverComposite = new Composite(this.individualGroup, SWT.BORDER);
					this.coverComposite.setLayout(null);
					this.coverComposite.setBounds(20, 40, 80, 240);
					{
						this.upperLimitRed = new Composite(this.coverComposite, SWT.NONE);
						GridLayout upperLimitRedLayout = new GridLayout();
						upperLimitRedLayout.makeColumnsEqualWidth = true;
						this.upperLimitRed.setLayout(upperLimitRedLayout);
						this.upperLimitRed.setBounds(0, 0, 80, 20);
						this.upperLimitRed.setBackground(SWTResourceManager.getColor(255, 0, 0));
					}
					{
						this.green = new Composite(this.coverComposite, SWT.NONE);
						GridLayout greenLayout = new GridLayout();
						greenLayout.makeColumnsEqualWidth = true;
						this.green.setLayout(greenLayout);
						this.green.setBounds(0, 20, 80, 30);
						this.green.setBackground(SWTResourceManager.getColor(0, 255, 0));
					}
					{
						this.yellowField = new Composite(this.coverComposite, SWT.NONE);
						GridLayout yellowFieldLayout = new GridLayout();
						yellowFieldLayout.makeColumnsEqualWidth = true;
						this.yellowField.setLayout(yellowFieldLayout);
						this.yellowField.setBounds(0, 50, 80, 160);
						this.yellowField.setBackground(SWTResourceManager.getColor(255, 255, 0));
						this.yellowField.addPaintListener(new PaintListener() {
							@Override
							public void paintControl(PaintEvent evt) {
								CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "lableComboComposite.paintControl, event=" + evt); //$NON-NLS-1$
								evt.gc.setLineStyle(SWT.LINE_DASH);
								evt.gc.drawLine(0, 50, 100, 50);
							}
						});
					}
					{
						this.lowerLimitRed = new Composite(this.coverComposite, SWT.NONE);
						GridLayout lowerLimitRedLayout = new GridLayout();
						lowerLimitRedLayout.makeColumnsEqualWidth = true;
						this.lowerLimitRed.setLayout(lowerLimitRedLayout);
						this.lowerLimitRed.setBounds(0, 210, 80, 33);
						this.lowerLimitRed.setBackground(SWTResourceManager.getColor(255, 0, 0));
					}
				}
				{
					this.lableComboComposite = new Composite(this.individualGroup, SWT.NONE);
					FillLayout lableComboCompositeLayout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
					this.lableComboComposite.setLayout(lableComboCompositeLayout);
					this.lableComboComposite.setBounds(145, 10, 100, 290);
					this.lableComboComposite.setBackground(this.application.COLOR_CANVAS_YELLOW);
					{
						this.upperLimitVoltageLabel = new CLabel(this.lableComboComposite, SWT.LEFT | SWT.EMBEDDED);
						this.upperLimitVoltageLabel.setBackground(SWTResourceManager.getColor(255, 255, 128));
						this.upperLimitVoltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.upperLimitVoltageLabel.setText(Messages.getString(MessageIds.GDE_MSGT0380));
						this.upperLimitVoltageLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0381));
						this.upperLimitVoltageLabel.setBounds(0, 0, 115, 25);
					}
					{
						this.upperLimitVoltageCombo = new CCombo(this.lableComboComposite, SWT.BORDER);
						this.upperLimitVoltageCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.upperLimitVoltageCombo.setItems(CellVoltageValues.getVoltageLimitsStringArray());
						this.upperLimitVoltageCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0381));
						this.upperLimitVoltageCombo.setBounds(0, 25, 95, GDE.IS_LINUX ? 22 : 20);
						this.upperLimitVoltageCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "upperLimitVoltageCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								CellVoltageValuesDialog.this.voltageLimits[0] = Integer.valueOf(CellVoltageValuesDialog.this.upperLimitVoltageCombo.getText().replace(".", "").replace(",", "")); //$NON-NLS-1$ //$NON-NLS-2$
								CellVoltageValuesDialog.log.info("upperLimitVoltage = " + CellVoltageValuesDialog.this.voltageLimits[0]); //$NON-NLS-1$
							}
						});
					}
					{
						this.upperLimitColorRedLabel = new CLabel(this.lableComboComposite, SWT.LEFT | SWT.EMBEDDED);
						this.upperLimitColorRedLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.upperLimitColorRedLabel.setText(Messages.getString(MessageIds.GDE_MSGT0382));
						this.upperLimitColorRedLabel.setBackground(SWTResourceManager.getColor(255, 255, 128));
						this.upperLimitColorRedLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0383));
						this.upperLimitColorRedLabel.setBounds(0, 49, 115, 24);
					}
					{
						this.upperLimitColorRedCombo = new CCombo(this.lableComboComposite, SWT.BORDER);
						this.upperLimitColorRedCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.upperLimitColorRedCombo.setItems(CellVoltageValues.getVoltageLimitsStringArray());
						this.upperLimitColorRedCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0383));
						this.upperLimitColorRedCombo.setBounds(0, 73, 95, GDE.IS_LINUX ? 22 : 20);
						this.upperLimitColorRedCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "upperLimitColorRedCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								CellVoltageValuesDialog.this.voltageLimits[1] = Integer.valueOf(CellVoltageValuesDialog.this.upperLimitColorRedCombo.getText().replace(".", "").replace(",", "")); //$NON-NLS-1$ //$NON-NLS-2$
								CellVoltageValuesDialog.log.info("upperLimitColorRed = " + CellVoltageValuesDialog.this.voltageLimits[1]); //$NON-NLS-1$
							}
						});
					}
					{
						this.lowerLimitColorGreenLabel = new CLabel(this.lableComboComposite, SWT.LEFT | SWT.EMBEDDED);
						this.lowerLimitColorGreenLabel.setBackground(SWTResourceManager.getColor(255, 255, 128));
						this.lowerLimitColorGreenLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.lowerLimitColorGreenLabel.setText(Messages.getString(MessageIds.GDE_MSGT0384));
						this.lowerLimitColorGreenLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0385));
						this.lowerLimitColorGreenLabel.setBounds(0, 97, 115, 24);
					}
					{
						this.lowerLimitColorGreenCombo = new CCombo(this.lableComboComposite, SWT.BORDER);
						this.lowerLimitColorGreenCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.lowerLimitColorGreenCombo.setItems(CellVoltageValues.getVoltageLimitsStringArray());
						this.lowerLimitColorGreenCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0385));
						this.lowerLimitColorGreenCombo.setBounds(0, 121, 95, GDE.IS_LINUX ? 22 : 20);
						this.lowerLimitColorGreenCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "lowerLimitColorGreenCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								CellVoltageValuesDialog.this.voltageLimits[2] = Integer.valueOf(CellVoltageValuesDialog.this.lowerLimitColorGreenCombo.getText().replace(".", "").replace(",", "")); //$NON-NLS-1$ //$NON-NLS-2$
								CellVoltageValuesDialog.log.info("lowerLimitColorGreen = " + CellVoltageValuesDialog.this.voltageLimits[2]); //$NON-NLS-1$
							}
						});
					}
					{
						this.beginSpreadVoltageLabel = new CLabel(this.lableComboComposite, SWT.LEFT | SWT.EMBEDDED);
						this.beginSpreadVoltageLabel.setBackground(SWTResourceManager.getColor(255, 255, 128));
						this.beginSpreadVoltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.beginSpreadVoltageLabel.setText(Messages.getString(MessageIds.GDE_MSGT0386));
						this.beginSpreadVoltageLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0387));
						this.beginSpreadVoltageLabel.setBounds(0, 145, 115, 24);
					}
					{
						this.beginSpreadVoltageCombo = new CCombo(this.lableComboComposite, SWT.BORDER);
						this.beginSpreadVoltageCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.beginSpreadVoltageCombo.setItems(CellVoltageValues.getVoltageLimitsStringArray());
						this.beginSpreadVoltageCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0387));
						this.beginSpreadVoltageCombo.setBounds(0, 169, 95, GDE.IS_LINUX ? 22 : 20);
						this.beginSpreadVoltageCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "beginSpreadVoltageCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								CellVoltageValuesDialog.this.voltageLimits[3] = Integer.valueOf(CellVoltageValuesDialog.this.beginSpreadVoltageCombo.getText().replace(".", "").replace(",", "")); //$NON-NLS-1$ //$NON-NLS-2$
								CellVoltageValuesDialog.log.info("beginSpreadVoltage = " + CellVoltageValuesDialog.this.voltageLimits[3]); //$NON-NLS-1$
							}
						});
					}
					{
						this.lowerLimitRedLabel = new CLabel(this.lableComboComposite, SWT.LEFT | SWT.EMBEDDED);
						this.lowerLimitRedLabel.setBackground(SWTResourceManager.getColor(255, 255, 128));
						this.lowerLimitRedLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.lowerLimitRedLabel.setText(Messages.getString(MessageIds.GDE_MSGT0388));
						this.lowerLimitRedLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0389));
						this.lowerLimitRedLabel.setBounds(0, 193, 115, GDE.IS_LINUX ? 22 : 20);
					}
					{
						this.lowerLimitColorRedCombo = new CCombo(this.lableComboComposite, SWT.BORDER);
						this.lowerLimitColorRedCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.lowerLimitColorRedCombo.setItems(CellVoltageValues.getVoltageLimitsStringArray());
						this.lowerLimitColorRedCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0389));
						this.lowerLimitColorRedCombo.setBounds(0, 217, 95, GDE.IS_LINUX ? 22 : 20);
						this.lowerLimitColorRedCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "lowerLimitRedColorCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								CellVoltageValuesDialog.this.voltageLimits[4] = Integer.valueOf(CellVoltageValuesDialog.this.lowerLimitColorRedCombo.getText().replace(".", "").replace(",", "")); //$NON-NLS-1$ //$NON-NLS-2$
								CellVoltageValuesDialog.log.info("lowerLimitColorRed = " + CellVoltageValuesDialog.this.voltageLimits[4]); //$NON-NLS-1$
							}
						});
					}
					{
						this.lowerLimitVoltageLabel = new CLabel(this.lableComboComposite, SWT.LEFT | SWT.EMBEDDED);
						this.lowerLimitVoltageLabel.setBackground(SWTResourceManager.getColor(255, 255, 128));
						this.lowerLimitVoltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.lowerLimitVoltageLabel.setText(Messages.getString(MessageIds.GDE_MSGT0390));
						this.lowerLimitVoltageLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0391));
						this.lowerLimitVoltageLabel.setBounds(0, 241, 115, 24);
					}
					{
						this.lowerLimitVoltageCombo = new CCombo(this.lableComboComposite, SWT.BORDER);
						this.lowerLimitVoltageCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.lowerLimitVoltageCombo.setItems(CellVoltageValues.getVoltageLimitsStringArray());
						this.lowerLimitVoltageCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0391));
						this.lowerLimitVoltageCombo.setBounds(0, 265, 95, GDE.IS_LINUX ? 22 : 20);
						this.lowerLimitVoltageCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								CellVoltageValuesDialog.log.logp(Level.FINEST, CellVoltageValuesDialog.$CLASS_NAME, $METHOD_NAME, "lowerLimitVoltageCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								CellVoltageValuesDialog.this.voltageLimits[5] = Integer.valueOf(CellVoltageValuesDialog.this.lowerLimitVoltageCombo.getText().replace(".", "").replace(",", "")); //$NON-NLS-1$ //$NON-NLS-2$
								CellVoltageValuesDialog.log.info("lowerLimitVoltage = " + CellVoltageValuesDialog.this.voltageLimits[5]); //$NON-NLS-1$
							}
						});
					}
				}
			}
			{ // begin ok button
				FormData okButtonLData = new FormData();
				okButtonLData.width = 150;
				okButtonLData.height = 25;
				okButtonLData.left = new FormAttachment(0, 1000, 115);
				okButtonLData.bottom = new FormAttachment(1000, 1000, -5);
				this.okButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.okButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.okButton.setLayoutData(okButtonLData);
				this.okButton.setText(Messages.getString(MessageIds.GDE_MSGT0188));
				this.okButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						SettingsDialog.log.log(Level.FINEST, "okButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						CellVoltageValuesDialog.this.dialogShell.dispose();
					}
				});
			} // end ok button
			this.dialogShell.setLocation(getParent().toDisplay(100, 100));
			this.dialogShell.open();
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return CellVoltageValues.getVoltageLimits();
	}

	/**
	 * find a matching entry from value within combo items
	 */
	int matchValueToSelection(CCombo combo, int value) {
		String[] comboValues = combo.getItems();
		String strValue = String.format(Locale.ENGLISH, "%04d", value);
		int index = 0;
		for (; index < comboValues.length; ++index) {
			if (comboValues[index].replace(".", "").replace(",", "").equals(strValue)) break;
		}
		return index;
	}

}
