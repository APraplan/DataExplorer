/**************************************************************************************
  	This file is part of DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.wstech;

import gde.GDE;
import gde.device.IDevice;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

/**
 * @author brueg
 *
 */
public class VarioToolTabItem extends CTabItem {
	final static Logger						log											= Logger.getLogger(VarioToolTabItem.class.getName());

	private static final String		LINK_VARIO_LINKVARI_HEX	= "LINKVARI.HEX";																		//$NON-NLS-1$
	private static final String		DATA_VARIO_SETUPUPL_HEX	= "SETUPUPL.HEX";																		//$NON-NLS-1$

	final CTabFolder							tabFolder;
	final IDevice									device;
	final DataExplorer						application							= DataExplorer.getInstance();
	final boolean									isDataVarioTool;
	final Menu										popupmenu;
	final ContextMenu							contextMenu;

	ScrolledComposite							mainTabScrolledComposite;
	Composite											innerContentComposite;

	Group													setupGroup0, setupGroup1, setupGroup2, setupGroup3, setupGroup4, setupGroup5, setupGroup6, setupGroup7, setupGroup8, setupGroup9, setupGroup10, setupGroup11,
			setupGroup12, setupGroup13, setupGroup14, setupGroup15, setupGroup16, setupGroup17, setupGroup18, setupGroup19;

	Slider												setupSlider0, setupSlider1, setupSlider2, setupSlider3, setupSlider6, setupSlider7, setupSlider11, setupSlider12, setupSlider13, setupSlider16, setupSlider17,
			setupSlider18, setupSlider19;
	Text													channelText, frequencyText, setupText1, setupText2, setupText3, setupText6, setupText7, setupText11, setupText12, setupText13, setupText16, setupText17, setupText18,
			setupText19;
	Button												setupButton3c, setupButton4a, setupButton4b, setupButton4c, setupButton4d, setupButton4e, setupButton4f, setupButton5a, setupButton5b, setupButton5c, setupButton5d;
	Button												setupButton8a, setupButton8b, setupButton8c, setupButton8d, setupButton8e;
	Button												setupButton9a, setupButton9b, setupButton9c, setupButton9d, setupButton9e, setupButton9f;
	Button												setupButton19a, setupButton19b, setupButton19c, setupButton19d, setupButton19e, setupButton19f, setupButton19g;
	Composite											fillComposite, setupComposite19a, setupComposite19b, setupComposite19c;
	Button												setupButton10a, setupButton10b, setupButton10c, setupButton10d, setupButton10e, setupButton11, setupButton12, setupButton13c;
	Button												setupButton13a, setupButton13b, setupButton14a, setupButton14b, setupButton14c, setupButton15a, setupButton15b, setupButton17, setupButton18;
	Composite											setupComposite4c, setupComposite4b, setupComposite4a, setupComposite5, setupComposite8, setupComposite9, setupComposite10;
	Composite											setupComposite13a, setupComposite13b, setupComposite14;
	CLabel												channelLabel, frequencyLabel, setupLabel1a, setupLabel1b, setupLabel2a, setupLabel2b, setupLabel3a, setupLabel3b, setupLabel4, setupLabel6a, setupLabel6b, setupLabel7a, setupLabel7b;
	CLabel												setupLabel10, setupLabel11a, setupLabel11b, setupLabel12a, setupLabel12b, setupLabel13, setupLabel16a, setupLabel16b, setupLabel17a, setupLabel17b, setupLabel17c, setupLabel18a, setupLabel18b;
	CLabel												setupLabel19a, setupLabel19b;

	int														setupValue0							= 57;																																																																				// transmit channel
	int														setupValue1							= 60;																																																																				// repeat positive height 
	int														setupValue2							= 20;																																																																				// repeat negative height 
	int														setupValue3							= 10;																																																																				// repeat integral height 
	int														setupValue4							= 1;																																																																					// velocity
	int														setupValue5							= 1;																																																																					// vario function
	int														setupValue6							= 48;																																																																				// receiver warn voltage level
	int														setupValue7							= 8;																																																																					// sink ton level
	int														setupValue8							= 1;																																																																					// vario ton mode
	int														setupValue9							= 0;																																																																					// motor battery voltage/current
	int														setupValue10						= 0;																																																																					// sensor interface
	int														setupValue11						= 60;																																																																				// temperature alarm level
	int														setupValue12						= 70;																																																																				// motor battery voltage alarm level
	int														setupValue13						= 3;																																																																					// integral vario config
	int[]													setupValues13						= { 0, 0, 0, 2, 2, 5, 5, 10, 10 };
	int														setupValue14						= 1;																																																																					// current sensor type
	int														setupValue15						= 1;																																																																					// height unit
	int														setupValue16						= 5;																																																																					// current delay
	int														setupValue17						= 60;																																																																				// temperature alarm level
	int														setupValue18						= 0;																																																																					// motor battery capacity alarm level
	int														setupValue19						= 27;																																																																				// link vario SIO config

	/**
	 * @param displayTabFolder
	 * @param style
	 */
	public VarioToolTabItem(CTabFolder displayTabFolder, int style, IDevice useDevice, boolean isVarioTool) {
		super(displayTabFolder, style);
		this.tabFolder = displayTabFolder;
		this.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 3 : 1), SWT.NORMAL));
		this.device = useDevice;
		this.isDataVarioTool = isVarioTool;
		this.setText(isVarioTool ? Messages.getString(MessageIds.GDE_MSGT1803) : Messages.getString(MessageIds.GDE_MSGT1804));
		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new ContextMenu(this);
		this.create();
	}

	/**
	 * @param displayTabFolder
	 * @param style
	 * @param position
	 */
	public VarioToolTabItem(CTabFolder displayTabFolder, int style, int position, IDevice useDevice, boolean isVarioTool) {
		super(displayTabFolder, style, position);
		this.tabFolder = displayTabFolder;
		SWTResourceManager.registerResourceUser(this);
		this.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 3 : 1), SWT.NORMAL));
		this.device = useDevice;
		this.isDataVarioTool = isVarioTool;
		this.setText(isVarioTool ? Messages.getString(MessageIds.GDE_MSGT1803) : Messages.getString(MessageIds.GDE_MSGT1804));
		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new ContextMenu(this);
		this.create();
	}

	void create() {
		try {
			{ // begin scrolled composite
				this.mainTabScrolledComposite = new ScrolledComposite(this.tabFolder, SWT.H_SCROLL | SWT.V_SCROLL);
				this.setControl(this.mainTabScrolledComposite);
				{
					this.innerContentComposite = new Composite(this.mainTabScrolledComposite, SWT.NONE);
					this.innerContentComposite.setLayout(null);
					this.innerContentComposite.setBounds(0, 0, 1000, this.isDataVarioTool ? (GDE.IS_MAC ? 780 : 760) : (GDE.IS_MAC ? 890 : 870));
					if (this.isDataVarioTool) {
						this.setupGroup0 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout group1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup0.setLayout(group1Layout);
						this.setupGroup0.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup0.setText(Messages.getString(MessageIds.GDE_MSGT1817));
						this.setupGroup0.setBounds(5, 6, 490, GDE.IS_MAC ? 55 : 45);
						{
							new CLabel(this.setupGroup0, SWT.NONE).setSize(5, 18);
						}
						{
							RowData channelSliderLData = new RowData();
							channelSliderLData.width = 160;
							channelSliderLData.height = 18;
							this.setupSlider0 = new Slider(this.setupGroup0, SWT.BORDER);
							this.setupSlider0.setLayoutData(channelSliderLData);
							this.setupSlider0.setMinimum(1);
							this.setupSlider0.setMaximum(79); //max 69
							this.setupSlider0.setIncrement(1);
							this.setupSlider0.setSelection(this.setupValue0);
							this.setupSlider0.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupSlider0.widgetSelected, event=" + evt); //$NON-NLS-1$
									VarioToolTabItem.this.setupValue0 = VarioToolTabItem.this.setupSlider0.getSelection();
									VarioToolTabItem.this.channelText.setText(GDE.STRING_EMPTY + VarioToolTabItem.this.setupValue0);
									VarioToolTabItem.this.frequencyText.setText(String.format("%.3f", (433.050 + VarioToolTabItem.this.setupValue0 * 0.025))); //$NON-NLS-1$
								}
							});
						}
						{
							this.channelLabel = new CLabel(this.setupGroup0, SWT.RIGHT);
							RowData channelLabelLData = new RowData();
							channelLabelLData.width = 60;
							channelLabelLData.height = 18;
							this.channelLabel.setLayoutData(channelLabelLData);
							this.channelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.channelLabel.setText(Messages.getString(MessageIds.GDE_MSGT1806));
						}
						{
							this.channelText = new Text(this.setupGroup0, SWT.CENTER | SWT.BORDER);
							RowData channelTextLData = new RowData();
							channelTextLData.width = 35;
							channelTextLData.height = 12;
							this.channelText.setLayoutData(channelTextLData);
							this.channelText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
							this.channelText.setText(GDE.STRING_EMPTY + this.setupValue0);
							this.channelText.setEditable(false);
							this.channelText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
						}
						{
							this.frequencyLabel = new CLabel(this.setupGroup0, SWT.RIGHT);
							RowData frequencyLabelLData = new RowData();
							frequencyLabelLData.width = 75;
							frequencyLabelLData.height = 18;
							this.frequencyLabel.setLayoutData(frequencyLabelLData);
							this.frequencyLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.frequencyLabel.setText(Messages.getString(MessageIds.GDE_MSGT1807));
						}
						{
							this.frequencyText = new Text(this.setupGroup0, SWT.CENTER | SWT.BORDER);
							RowData frequencyTextLData = new RowData();
							frequencyTextLData.width = 50;
							frequencyTextLData.height = 12;
							this.frequencyText.setLayoutData(frequencyTextLData);
							this.frequencyText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
							this.frequencyText.setText(String.format("%.3f", (433.050 + this.setupValue0 * 0.025))); //$NON-NLS-1$
							this.frequencyText.setEditable(false);
							this.frequencyText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
						}
						this.setupGroup0.layout();
						this.setupGroup0.setMenu(this.popupmenu);
					}
					{
						this.setupGroup1 = new Group(this.innerContentComposite, SWT.NONE);
						this.setupGroup1.setBounds(this.isDataVarioTool ? 505 : 5, 6, this.isDataVarioTool ? 490 : 990, GDE.IS_MAC ? 55 : 45);
						this.setupGroup1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup1.setText(Messages.getString(MessageIds.GDE_MSGT1805));
						RowLayout group1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup1.setLayout(group1Layout);
						{
							new CLabel(this.setupGroup1, SWT.NONE).setSize(5, 18);
						}
						{
							RowData setupSlider1LData = new RowData();
							setupSlider1LData.width = 160;
							setupSlider1LData.height = 18;
							this.setupSlider1 = new Slider(this.setupGroup1, SWT.BORDER);
							this.setupSlider1.setMinimum(10);
							this.setupSlider1.setMaximum(130); // max 120
							this.setupSlider1.setIncrement(10);
							this.setupSlider1.setSelection(this.setupValue1);
							this.setupSlider1.setLayoutData(setupSlider1LData);
							this.setupSlider1.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupSlider1.widgetSelected, event=" + evt); //$NON-NLS-1$
									int tmpValue = VarioToolTabItem.this.setupSlider1.getSelection();
									if (tmpValue % 10 == 0) {
										if (tmpValue < 10)
											VarioToolTabItem.this.setupValue1 = 10;
										else if (tmpValue > 120)
											VarioToolTabItem.this.setupValue1 = 120;
										else
											VarioToolTabItem.this.setupValue1 = tmpValue;
										VarioToolTabItem.this.setupText1.setText(GDE.STRING_EMPTY + VarioToolTabItem.this.setupValue1);
									}
								}
							});
						}
						{
							this.setupLabel1a = new CLabel(this.setupGroup1, SWT.RIGHT);
							RowData setupLabel1aLData = new RowData();
							setupLabel1aLData.width = 60;
							setupLabel1aLData.height = 18;
							this.setupLabel1a.setLayoutData(setupLabel1aLData);
							this.setupLabel1a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel1a.setText(Messages.getString(MessageIds.GDE_MSGT1809));
						}
						{
							this.setupText1 = new Text(this.setupGroup1, SWT.CENTER | SWT.BORDER);
							RowData setupText1LData = new RowData();
							setupText1LData.width = 35;
							setupText1LData.height = 12;
							this.setupText1.setLayoutData(setupText1LData);
							this.setupText1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
							this.setupText1.setText(GDE.STRING_EMPTY + this.setupValue1);
							this.setupText1.setEditable(false);
							this.setupText1.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
						}
						{
							this.setupLabel1b = new CLabel(this.setupGroup1, SWT.LEFT);
							RowData setupLabel1bLData = new RowData();
							setupLabel1bLData.width = 70;
							setupLabel1bLData.height = 18;
							this.setupLabel1b.setLayoutData(setupLabel1bLData);
							this.setupLabel1b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel1b.setText(Messages.getString(MessageIds.GDE_MSGT1810));
						}
						this.setupGroup1.layout();
						this.setupGroup1.setMenu(this.popupmenu);
					}
					{
						this.setupGroup2 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout group2Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup2.setLayout(group2Layout);
						this.setupGroup2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup2.setText(Messages.getString(MessageIds.GDE_MSGT1808));
						this.setupGroup2.setBounds(5, 53, 490, GDE.IS_MAC ? 55 : 45);
						{
							new CLabel(this.setupGroup2, SWT.NONE).setSize(5, 18);
						}
						{
							RowData setupSlider2LData = new RowData();
							setupSlider2LData.width = 160;
							setupSlider2LData.height = 18;
							this.setupSlider2 = new Slider(this.setupGroup2, SWT.BORDER);
							this.setupSlider2.setMinimum(10);
							this.setupSlider2.setMaximum(70); // max 60
							this.setupSlider2.setIncrement(10);
							this.setupSlider2.setSelection(this.setupValue2);
							this.setupSlider2.setLayoutData(setupSlider2LData);
							this.setupSlider2.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupSlider2.widgetSelected, event=" + evt); //$NON-NLS-1$
									int tmpValue = VarioToolTabItem.this.setupSlider2.getSelection();
									if (tmpValue % 10 == 0) {
										if (tmpValue < 10)
											VarioToolTabItem.this.setupValue2 = 10;
										else if (tmpValue > 60)
											VarioToolTabItem.this.setupValue2 = 60;
										else
											VarioToolTabItem.this.setupValue2 = tmpValue;
										VarioToolTabItem.this.setupText2.setText(GDE.STRING_EMPTY + VarioToolTabItem.this.setupValue2);
									}
								}
							});
						}
						{
							this.setupLabel2a = new CLabel(this.setupGroup2, SWT.RIGHT);
							RowData setupLabel2aLData = new RowData();
							setupLabel2aLData.width = 60;
							setupLabel2aLData.height = 18;
							this.setupLabel2a.setLayoutData(setupLabel2aLData);
							this.setupLabel2a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel2a.setText(Messages.getString(MessageIds.GDE_MSGT1809));
						}
						{
							this.setupText2 = new Text(this.setupGroup2, SWT.CENTER | SWT.BORDER);
							RowData setupText2LData = new RowData();
							setupText2LData.width = 35;
							setupText2LData.height = 12;
							this.setupText2.setLayoutData(setupText2LData);
							this.setupText2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
							this.setupText2.setText(GDE.STRING_EMPTY + this.setupValue2);
							this.setupText2.setEditable(false);
							this.setupText2.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
						}
						{
							this.setupLabel2b = new CLabel(this.setupGroup2, SWT.LEFT);
							RowData setupLabel2bLData = new RowData();
							setupLabel2bLData.width = 70;
							setupLabel2bLData.height = 18;
							this.setupLabel2b.setLayoutData(setupLabel2bLData);
							this.setupLabel2b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel2b.setText(Messages.getString(MessageIds.GDE_MSGT1810));
						}
						this.setupGroup2.layout();
						this.setupGroup2.setMenu(this.popupmenu);
					}
					{
						this.setupGroup3 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout group3Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup3.setLayout(group3Layout);
						this.setupGroup3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup3.setText(Messages.getString(MessageIds.GDE_MSGT1812));
						this.setupGroup3.setBounds(505, 53, 490, GDE.IS_MAC ? 55 : 45);
						{
							new CLabel(this.setupGroup3, SWT.NONE).setSize(5, 18);
						}
						{
							RowData setupSlider3LData = new RowData();
							setupSlider3LData.width = 160;
							setupSlider3LData.height = 18;
							this.setupSlider3 = new Slider(this.setupGroup3, SWT.BORDER);
							this.setupSlider3.setMinimum(5);
							this.setupSlider3.setMaximum(40); // max 30
							this.setupSlider3.setIncrement(5);
							this.setupSlider3.setSelection(this.setupValue3);
							this.setupSlider3.setEnabled(this.setupValue3 != 60);
							this.setupSlider3.setLayoutData(setupSlider3LData);
							this.setupSlider3.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupSlider3.widgetSelected, event=" + evt); //$NON-NLS-1$
									int tmpValue = VarioToolTabItem.this.setupSlider3.getSelection();
									if (tmpValue % 5 == 0) {
										if (tmpValue < 10)
											VarioToolTabItem.this.setupValue3 = 5;
										else if (tmpValue > 30)
											VarioToolTabItem.this.setupValue3 = 30;
										else
											VarioToolTabItem.this.setupValue3 = tmpValue;
										VarioToolTabItem.this.setupText3.setText(GDE.STRING_EMPTY + VarioToolTabItem.this.setupValue3);
									}
								}
							});
						}
						{
							this.setupLabel3a = new CLabel(this.setupGroup3, SWT.RIGHT);
							RowData setupLabel3aLData = new RowData();
							setupLabel3aLData.width = 60;
							setupLabel3aLData.height = 18;
							this.setupLabel3a.setLayoutData(setupLabel3aLData);
							this.setupLabel3a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel3a.setText(Messages.getString(MessageIds.GDE_MSGT1809));
						}
						{
							this.setupText3 = new Text(this.setupGroup3, SWT.CENTER | SWT.BORDER);
							RowData setupText3LData = new RowData();
							setupText3LData.width = 35;
							setupText3LData.height = 12;
							this.setupText3.setLayoutData(setupText3LData);
							this.setupText3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
							this.setupText3.setText((this.setupValue3 != 60) ? GDE.STRING_EMPTY + this.setupValue3 : GDE.STRING_EMPTY);
							this.setupText3.setEditable(false);
							this.setupText3.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
						}
						{
							this.setupLabel3b = new CLabel(this.setupGroup3, SWT.LEFT);
							RowData setupLabel3bLData = new RowData();
							setupLabel3bLData.width = 70;
							setupLabel3bLData.height = 18;
							this.setupLabel3b.setLayoutData(setupLabel3bLData);
							this.setupLabel3b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel3b.setText(Messages.getString(MessageIds.GDE_MSGT1810));
						}
						{
							this.setupButton3c = new Button(this.setupGroup3, SWT.CHECK | SWT.LEFT);
							RowData setupButton3LData = new RowData();
							setupButton3LData.width = 105;
							setupButton3LData.height = 18;
							this.setupButton3c.setLayoutData(setupButton3LData);
							this.setupButton3c.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupButton3c.setText(Messages.getString(MessageIds.GDE_MSGT1811));
							this.setupButton3c.setSelection(this.setupValue3 == 60);
							this.setupButton3c.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton3c.widgetSelected, event=" + evt); //$NON-NLS-1$
									if (VarioToolTabItem.this.setupButton3c.getSelection()) {
										VarioToolTabItem.this.setupText3.setText(GDE.STRING_EMPTY);
										VarioToolTabItem.this.setupSlider3.setEnabled(false);
										VarioToolTabItem.this.setupValue3 = 60;
									}
									else {
										VarioToolTabItem.this.setupValue3 = 20;
										VarioToolTabItem.this.setupText3.setText(GDE.STRING_EMPTY + VarioToolTabItem.this.setupValue3);
										VarioToolTabItem.this.setupSlider3.setEnabled(true);
										VarioToolTabItem.this.setupSlider3.setSelection(VarioToolTabItem.this.setupValue3);
									}
								}
							});
						}
						this.setupGroup3.layout();
						this.setupGroup3.setMenu(this.popupmenu);
					}
					{
						this.setupGroup4 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout group4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup4.setLayout(group4Layout);
						this.setupGroup4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup4.setText(Messages.getString(MessageIds.GDE_MSGT1813));
						this.setupGroup4.setBounds(5, 100, 490, GDE.IS_MAC ? 105 : 95);
						{
							new CLabel(this.setupGroup4, SWT.NONE).setSize(5, 72);
						}
						{
							RowData composite1LData = new RowData();
							composite1LData.width = 435;
							composite1LData.height = 72;
							this.setupComposite4a = new Composite(this.setupGroup4, SWT.NONE);
							FillLayout composite1Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
							this.setupComposite4a.setLayout(composite1Layout);
							this.setupComposite4a.setLayoutData(composite1LData);
							{
								this.setupButton4a = new Button(this.setupComposite4a, SWT.RADIO | SWT.LEFT);
								this.setupButton4a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton4a.setText(Messages.getString(MessageIds.GDE_MSGT1814));
								this.setupButton4a.setSelection(this.setupValue4 == 1);
								this.setupButton4a.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton4a.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton4a.getSelection()) {
											VarioToolTabItem.this.setupValue4 = 1;
											VarioToolTabItem.this.setupButton4b.setSelection(false);
											VarioToolTabItem.this.setupButton4c.setSelection(false);
											VarioToolTabItem.this.setupButton4d.setSelection(false);
											VarioToolTabItem.this.setupButton4e.setSelection(false);
											VarioToolTabItem.this.setupButton4f.setSelection(false);
										}
									}
								});
							}
							{
								this.setupLabel4 = new CLabel(this.setupComposite4a, SWT.NONE);
								this.setupLabel4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupLabel4.setText(Messages.getString(MessageIds.GDE_MSGT1815));
							}
							{
								this.setupComposite4b = new Composite(this.setupComposite4a, SWT.NONE);
								FillLayout additionalHeightCompositeLayout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
								this.setupComposite4b.setLayout(additionalHeightCompositeLayout);
								{
									this.setupComposite4c = new Composite(this.setupComposite4b, SWT.NONE);
									RowLayout additionalHeightButtonsCompositeLayout = new RowLayout(org.eclipse.swt.SWT.VERTICAL);
									this.setupComposite4c.setLayout(additionalHeightButtonsCompositeLayout);
									{
										this.setupButton4b = new Button(this.setupComposite4c, SWT.RADIO | SWT.LEFT);
										this.setupButton4b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.setupButton4b.setText(Messages.getString(MessageIds.GDE_MSGT1816));
										this.setupButton4b.setSelection(this.setupValue4 == 3);
										this.setupButton4b.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "addHeightButton70.widgetSelected, event=" + evt); //$NON-NLS-1$
												if (VarioToolTabItem.this.setupButton4b.getSelection()) {
													VarioToolTabItem.this.setupValue4 = 3;
													VarioToolTabItem.this.setupButton4a.setSelection(false);
													VarioToolTabItem.this.setupButton4c.setSelection(false);
													VarioToolTabItem.this.setupButton4d.setSelection(false);
													VarioToolTabItem.this.setupButton4e.setSelection(false);
													VarioToolTabItem.this.setupButton4f.setSelection(false);
												}
											}
										});
									}
									{
										this.setupButton4c = new Button(this.setupComposite4c, SWT.RADIO | SWT.LEFT);
										this.setupButton4c.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.setupButton4c.setText(Messages.getString(MessageIds.GDE_MSGT1818));
										this.setupButton4c.setSelection(this.setupValue4 == 4);
										this.setupButton4c.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "addHeightButton80.widgetSelected, event=" + evt); //$NON-NLS-1$
												if (VarioToolTabItem.this.setupButton4c.getSelection()) {
													VarioToolTabItem.this.setupValue4 = 4;
													VarioToolTabItem.this.setupButton4a.setSelection(false);
													VarioToolTabItem.this.setupButton4b.setSelection(false);
													VarioToolTabItem.this.setupButton4d.setSelection(false);
													VarioToolTabItem.this.setupButton4e.setSelection(false);
													VarioToolTabItem.this.setupButton4f.setSelection(false);
												}
											}
										});
									}
									{
										this.setupButton4d = new Button(this.setupComposite4c, SWT.RADIO | SWT.LEFT);
										this.setupButton4d.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.setupButton4d.setText(Messages.getString(MessageIds.GDE_MSGT1819));
										this.setupButton4d.setSelection(this.setupValue4 == 5);
										this.setupButton4d.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "addHeightButton100.widgetSelected, event=" + evt); //$NON-NLS-1$
												if (VarioToolTabItem.this.setupButton4d.getSelection()) {
													VarioToolTabItem.this.setupValue4 = 5;
													VarioToolTabItem.this.setupButton4a.setSelection(false);
													VarioToolTabItem.this.setupButton4b.setSelection(false);
													VarioToolTabItem.this.setupButton4c.setSelection(false);
													VarioToolTabItem.this.setupButton4e.setSelection(false);
													VarioToolTabItem.this.setupButton4f.setSelection(false);
												}
											}
										});
									}
									{
										this.setupButton4e = new Button(this.setupComposite4c, SWT.RADIO | SWT.LEFT);
										this.setupButton4e.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.setupButton4e.setText(Messages.getString(MessageIds.GDE_MSGT1820));
										this.setupButton4e.setSelection(this.setupValue4 == 6);
										this.setupButton4e.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton4e.widgetSelected, event=" + evt); //$NON-NLS-1$
												if (VarioToolTabItem.this.setupButton4e.getSelection()) {
													VarioToolTabItem.this.setupValue4 = 6;
													VarioToolTabItem.this.setupButton4a.setSelection(false);
													VarioToolTabItem.this.setupButton4b.setSelection(false);
													VarioToolTabItem.this.setupButton4c.setSelection(false);
													VarioToolTabItem.this.setupButton4d.setSelection(false);
													VarioToolTabItem.this.setupButton4f.setSelection(false);
												}
											}
										});
									}
								}
							}
							{
								this.setupButton4f = new Button(this.setupComposite4a, SWT.RADIO | SWT.LEFT);
								this.setupButton4f.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton4f.setText(Messages.getString(MessageIds.GDE_MSGT1821));
								this.setupButton4f.setSelection(this.setupValue4 == 2);
								this.setupButton4f.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "maxVelocitiyButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton4f.getSelection()) {
											VarioToolTabItem.this.setupValue4 = 2;
											VarioToolTabItem.this.setupButton4a.setSelection(false);
											VarioToolTabItem.this.setupButton4b.setSelection(false);
											VarioToolTabItem.this.setupButton4c.setSelection(false);
											VarioToolTabItem.this.setupButton4d.setSelection(false);
											VarioToolTabItem.this.setupButton4e.setSelection(false);
										}
									}
								});
							}
						}
						this.setupGroup4.layout();
						this.setupGroup4.setMenu(this.popupmenu);
					}
					{
						this.setupGroup5 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout group5Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup5.setLayout(group5Layout);
						this.setupGroup5.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup5.setText(Messages.getString(MessageIds.GDE_MSGT1823));
						this.setupGroup5.setBounds(505, 100, 490, GDE.IS_MAC ? 105 : 95);
						{
							new CLabel(this.setupGroup5, SWT.NONE).setSize(5, 90);
						}
						{
							RowData composite2LData = new RowData();
							composite2LData.width = 426;
							composite2LData.height = 72;
							this.setupComposite5 = new Composite(this.setupGroup5, SWT.NONE);
							FillLayout composite2Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
							this.setupComposite5.setLayout(composite2Layout);
							this.setupComposite5.setLayoutData(composite2LData);
							{
								this.setupButton5a = new Button(this.setupComposite5, SWT.RADIO | SWT.LEFT);
								this.setupButton5a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton5a.setText(Messages.getString(MessageIds.GDE_MSGT1824));
								this.setupButton5a.setSelection(this.setupValue5 == 1);
								this.setupButton5a.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton5a.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton5a.getSelection()) {
											VarioToolTabItem.this.setupValue5 = 1;
											VarioToolTabItem.this.setupButton5b.setSelection(false);
											VarioToolTabItem.this.setupButton5c.setSelection(false);
											VarioToolTabItem.this.setupButton5d.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton5b = new Button(this.setupComposite5, SWT.RADIO | SWT.LEFT);
								this.setupButton5b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton5b.setText(Messages.getString(MessageIds.GDE_MSGT1825));
								this.setupButton5b.setSelection(this.setupValue5 == 2);
								this.setupButton5b.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton5b.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton5b.getSelection()) {
											VarioToolTabItem.this.setupValue5 = 2;
											VarioToolTabItem.this.setupButton5a.setSelection(false);
											VarioToolTabItem.this.setupButton5c.setSelection(false);
											VarioToolTabItem.this.setupButton5d.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton5c = new Button(this.setupComposite5, SWT.RADIO | SWT.LEFT);
								this.setupButton5c.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton5c.setText(Messages.getString(MessageIds.GDE_MSGT1826));
								this.setupButton5c.setSelection(this.setupValue5 == 3);
								this.setupButton5c.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton5c.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton5c.getSelection()) {
											VarioToolTabItem.this.setupValue5 = 3;
											VarioToolTabItem.this.setupButton5a.setSelection(false);
											VarioToolTabItem.this.setupButton5b.setSelection(false);
											VarioToolTabItem.this.setupButton5d.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton5d = new Button(this.setupComposite5, SWT.RADIO | SWT.LEFT);
								this.setupButton5d.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton5d.setText(Messages.getString(MessageIds.GDE_MSGT1827));
								this.setupButton5d.setSelection(this.setupValue5 == 4);
								this.setupButton5d.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton5d.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton5d.getSelection()) {
											VarioToolTabItem.this.setupValue5 = 4;
											VarioToolTabItem.this.setupButton5a.setSelection(false);
											VarioToolTabItem.this.setupButton5b.setSelection(false);
											VarioToolTabItem.this.setupButton5c.setSelection(false);
										}
									}
								});
							}
						}
						this.setupGroup5.layout();
						this.setupGroup5.setMenu(this.popupmenu);
					}
					{
						this.setupGroup6 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout group6Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup6.setLayout(group6Layout);
						this.setupGroup6.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup6.setText(Messages.getString(MessageIds.GDE_MSGT1828));
						this.setupGroup6.setBounds(5, 197, 490, GDE.IS_MAC ? 55 : 45);
						{
							new CLabel(this.setupGroup6, SWT.NONE).setSize(5, 18);
						}
						{
							RowData setupSlider6LData = new RowData();
							setupSlider6LData.width = 160;
							setupSlider6LData.height = 18;
							this.setupSlider6 = new Slider(this.setupGroup6, SWT.BORDER);
							this.setupSlider6.setMinimum(44);
							this.setupSlider6.setMaximum(109);
							this.setupSlider6.setIncrement(1);
							this.setupSlider6.setSelection(this.setupValue6);
							this.setupSlider6.setLayoutData(setupSlider6LData);
							this.setupSlider6.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupSlider6.widgetSelected, event=" + evt); //$NON-NLS-1$
									int tmpValue = VarioToolTabItem.this.setupSlider6.getSelection();
									if (tmpValue < 44)
										VarioToolTabItem.this.setupValue6 = 44;
									else if (tmpValue > 99)
										VarioToolTabItem.this.setupValue6 = 99;
									else
										VarioToolTabItem.this.setupValue6 = tmpValue;
									VarioToolTabItem.this.setupText6.setText(String.format("%.1f", VarioToolTabItem.this.setupValue6 / 10.0)); //$NON-NLS-1$
								}
							});
						}
						{
							this.setupLabel6a = new CLabel(this.setupGroup6, SWT.RIGHT);
							RowData setupLabel6aLData = new RowData();
							setupLabel6aLData.width = 85;
							setupLabel6aLData.height = 18;
							this.setupLabel6a.setLayoutData(setupLabel6aLData);
							this.setupLabel6a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel6a.setText(Messages.getString(MessageIds.GDE_MSGT1829));
						}
						{
							this.setupText6 = new Text(this.setupGroup6, SWT.CENTER | SWT.BORDER);
							RowData setupText6LData = new RowData();
							setupText6LData.width = 35;
							setupText6LData.height = 12;
							this.setupText6.setLayoutData(setupText6LData);
							this.setupText6.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
							this.setupText6.setText(String.format("%.1f", this.setupValue6 / 10.0)); //$NON-NLS-1$
							this.setupText6.setEditable(false);
							this.setupText6.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
						}
						{
							this.setupLabel6b = new CLabel(this.setupGroup6, SWT.LEFT);
							RowData setupLabel6bLData = new RowData();
							setupLabel6bLData.width = 70;
							setupLabel6bLData.height = 18;
							this.setupLabel6b.setLayoutData(setupLabel6bLData);
							this.setupLabel6b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel6b.setText(Messages.getString(MessageIds.GDE_MSGT1830));
						}
						this.setupGroup6.layout();
						this.setupGroup6.setMenu(this.popupmenu);
					}
					{
						this.setupGroup7 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout setupGroup7Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup7.setLayout(setupGroup7Layout);
						this.setupGroup7.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup7.setText(Messages.getString(MessageIds.GDE_MSGT1831));
						this.setupGroup7.setBounds(505, 197, 490, GDE.IS_MAC ? 55 : 45);
						{
							new CLabel(this.setupGroup7, SWT.NONE).setSize(5, 18);
						}
						{
							RowData setupSlider7LData = new RowData();
							setupSlider7LData.width = 160;
							setupSlider7LData.height = 18;
							this.setupSlider7 = new Slider(this.setupGroup7, SWT.BORDER);
							this.setupSlider7.setMinimum(0);
							this.setupSlider7.setMaximum(30);
							this.setupSlider7.setIncrement(1);
							this.setupSlider7.setSelection(this.setupValue7);
							this.setupSlider7.setLayoutData(setupSlider7LData);
							this.setupSlider7.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupSlider7.widgetSelected, event=" + evt); //$NON-NLS-1$
									int tmpValue = VarioToolTabItem.this.setupSlider7.getSelection();
									if (tmpValue < 0)
										VarioToolTabItem.this.setupValue7 = 0;
									else if (tmpValue > 20)
										VarioToolTabItem.this.setupValue7 = 20;
									else
										VarioToolTabItem.this.setupValue7 = tmpValue;
									VarioToolTabItem.this.setupText7.setText(String.format("%.1f", VarioToolTabItem.this.setupValue7 / -10.0)); //$NON-NLS-1$
								}
							});
						}
						{
							this.setupLabel7a = new CLabel(this.setupGroup7, SWT.RIGHT);
							RowData setupLabel7aLData = new RowData();
							setupLabel7aLData.width = 60;
							setupLabel7aLData.height = 18;
							this.setupLabel7a.setLayoutData(setupLabel7aLData);
							this.setupLabel7a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel7a.setText(Messages.getString(MessageIds.GDE_MSGT1832));
						}
						{
							this.setupText7 = new Text(this.setupGroup7, SWT.CENTER | SWT.BORDER);
							RowData setupText7LData = new RowData();
							setupText7LData.width = 35;
							setupText7LData.height = 12;
							this.setupText7.setLayoutData(setupText7LData);
							this.setupText7.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
							this.setupText7.setText(String.format("%.1f", this.setupValue7 / -10.0)); //$NON-NLS-1$
							this.setupText7.setEditable(false);
							this.setupText7.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
						}
						{
							this.setupLabel7b = new CLabel(this.setupGroup7, SWT.LEFT);
							RowData setupLabel7bLData = new RowData();
							setupLabel7bLData.width = 70;
							setupLabel7bLData.height = 18;
							this.setupLabel7b.setLayoutData(setupLabel7bLData);
							this.setupLabel7b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel7b.setText(Messages.getString(MessageIds.GDE_MSGT1833));
						}
						this.setupGroup7.layout();
						this.setupGroup7.setMenu(this.popupmenu);
					}
					{
						this.setupGroup8 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout setupGroup8Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup8.setLayout(setupGroup8Layout);
						this.setupGroup8.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup8.setText(Messages.getString(MessageIds.GDE_MSGT1834));
						this.setupGroup8.setBounds(5, 244, 990, GDE.IS_MAC ? 125 : 115);
						{
							new CLabel(this.setupGroup8, SWT.NONE).setSize(5, 50);
						}
						{
							this.setupComposite8 = new Composite(this.setupGroup8, SWT.NONE);
							FillLayout setupComposite8Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
							RowData setupComposite8LData = new RowData();
							setupComposite8LData.width = 940;
							setupComposite8LData.height = 90;
							this.setupComposite8.setLayoutData(setupComposite8LData);
							this.setupComposite8.setLayout(setupComposite8Layout);
							{
								this.setupButton8a = new Button(this.setupComposite8, SWT.RADIO | SWT.LEFT);
								this.setupButton8a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton8a.setText(Messages.getString(MessageIds.GDE_MSGT1835));
								this.setupButton8a.setSelection(this.setupValue8 == 0);
								this.setupButton8a.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton8a.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton8a.getSelection()) {
											VarioToolTabItem.this.setupValue8 = 0;
											VarioToolTabItem.this.setupButton8b.setSelection(false);
											VarioToolTabItem.this.setupButton8c.setSelection(false);
											VarioToolTabItem.this.setupButton8d.setSelection(false);
											VarioToolTabItem.this.setupButton8e.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton8b = new Button(this.setupComposite8, SWT.RADIO | SWT.LEFT);
								this.setupButton8b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton8b.setText(Messages.getString(MessageIds.GDE_MSGT1836));
								this.setupButton8b.setSelection(this.setupValue8 == 1);
								this.setupButton8b.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton8b.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton8b.getSelection()) {
											VarioToolTabItem.this.setupValue8 = 1;
											VarioToolTabItem.this.setupButton8a.setSelection(false);
											VarioToolTabItem.this.setupButton8c.setSelection(false);
											VarioToolTabItem.this.setupButton8d.setSelection(false);
											VarioToolTabItem.this.setupButton8e.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton8c = new Button(this.setupComposite8, SWT.RADIO | SWT.LEFT);
								this.setupButton8c.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton8c.setText(Messages.getString(MessageIds.GDE_MSGT1837));
								this.setupButton8c.setSelection(this.setupValue8 == 2);
								this.setupButton8c.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton8c.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton8c.getSelection()) {
											VarioToolTabItem.this.setupValue8 = 2;
											VarioToolTabItem.this.setupButton8a.setSelection(false);
											VarioToolTabItem.this.setupButton8b.setSelection(false);
											VarioToolTabItem.this.setupButton8d.setSelection(false);
											VarioToolTabItem.this.setupButton8e.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton8d = new Button(this.setupComposite8, SWT.RADIO | SWT.LEFT);
								this.setupButton8d.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton8d.setText(Messages.getString(MessageIds.GDE_MSGT1838));
								this.setupButton8d.setSelection(this.setupValue8 == 3);
								this.setupButton8d.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton8d.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton8d.getSelection()) {
											VarioToolTabItem.this.setupValue8 = 3;
											VarioToolTabItem.this.setupButton8a.setSelection(false);
											VarioToolTabItem.this.setupButton8b.setSelection(false);
											VarioToolTabItem.this.setupButton8c.setSelection(false);
											VarioToolTabItem.this.setupButton8e.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton8e = new Button(this.setupComposite8, SWT.RADIO | SWT.LEFT);
								this.setupButton8e.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton8e.setText(Messages.getString(MessageIds.GDE_MSGT1839));
								this.setupButton8e.setSelection(this.setupValue8 == 4);
								this.setupButton8e.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton8e.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton8e.getSelection()) {
											VarioToolTabItem.this.setupValue8 = 4;
											VarioToolTabItem.this.setupButton8a.setSelection(false);
											VarioToolTabItem.this.setupButton8b.setSelection(false);
											VarioToolTabItem.this.setupButton8c.setSelection(false);
											VarioToolTabItem.this.setupButton8d.setSelection(false);
										}
									}
								});
							}
						}
						this.setupGroup8.layout();
						this.setupGroup8.setMenu(this.popupmenu);
					}
					{
						this.setupGroup9 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout setupGroup9Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup9.setLayout(setupGroup9Layout);
						this.setupGroup9.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup9.setText(Messages.getString(MessageIds.GDE_MSGT1840));
						this.setupGroup9.setBounds(5, 361, 490, GDE.IS_MAC ? 145 : 135);
						{
							new CLabel(this.setupGroup9, SWT.NONE).setSize(5, 108);
						}
						{
							this.setupComposite9 = new Composite(this.setupGroup9, SWT.NONE);
							FillLayout setupComposite9Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
							RowData setupComposite9LData = new RowData();
							setupComposite9LData.width = 460;
							setupComposite9LData.height = 108;
							this.setupComposite9.setLayoutData(setupComposite9LData);
							this.setupComposite9.setLayout(setupComposite9Layout);
							{
								this.setupButton9a = new Button(this.setupComposite9, SWT.RADIO | SWT.LEFT);
								this.setupButton9a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton9a.setText(Messages.getString(MessageIds.GDE_MSGT1841));
								this.setupButton9a.setSelection(this.setupValue9 == 0);
								this.setupButton9a.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton9a.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton9a.getSelection()) {
											VarioToolTabItem.this.setupValue9 = 0;
											VarioToolTabItem.this.setupButton9b.setSelection(false);
											VarioToolTabItem.this.setupButton9c.setSelection(false);
											VarioToolTabItem.this.setupButton9d.setSelection(false);
											VarioToolTabItem.this.setupButton9e.setSelection(false);
											VarioToolTabItem.this.setupButton9f.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton9b = new Button(this.setupComposite9, SWT.RADIO | SWT.LEFT);
								this.setupButton9b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton9b.setText(Messages.getString(MessageIds.GDE_MSGT1842));
								this.setupButton9b.setSelection(this.setupValue9 == 1);
								this.setupButton9b.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton9b.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton9b.getSelection()) {
											VarioToolTabItem.this.setupValue9 = 1;
											VarioToolTabItem.this.setupButton9a.setSelection(false);
											VarioToolTabItem.this.setupButton9c.setSelection(false);
											VarioToolTabItem.this.setupButton9d.setSelection(false);
											VarioToolTabItem.this.setupButton9e.setSelection(false);
											VarioToolTabItem.this.setupButton9f.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton9c = new Button(this.setupComposite9, SWT.RADIO | SWT.LEFT);
								this.setupButton9c.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton9c.setText(Messages.getString(MessageIds.GDE_MSGT1843));
								this.setupButton9c.setSelection(this.setupValue9 == 2);
								this.setupButton9c.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton9c.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton9c.getSelection()) {
											VarioToolTabItem.this.setupValue9 = 2;
											VarioToolTabItem.this.setupButton9a.setSelection(false);
											VarioToolTabItem.this.setupButton9b.setSelection(false);
											VarioToolTabItem.this.setupButton9d.setSelection(false);
											VarioToolTabItem.this.setupButton9e.setSelection(false);
											VarioToolTabItem.this.setupButton9f.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton9d = new Button(this.setupComposite9, SWT.RADIO | SWT.LEFT);
								this.setupButton9d.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton9d.setText(Messages.getString(MessageIds.GDE_MSGT1844));
								this.setupButton9d.setSelection(this.setupValue9 == 3);
								this.setupButton9d.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton9d.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton9d.getSelection()) {
											VarioToolTabItem.this.setupValue9 = 3;
											VarioToolTabItem.this.setupButton9a.setSelection(false);
											VarioToolTabItem.this.setupButton9b.setSelection(false);
											VarioToolTabItem.this.setupButton9c.setSelection(false);
											VarioToolTabItem.this.setupButton9e.setSelection(false);
											VarioToolTabItem.this.setupButton9f.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton9e = new Button(this.setupComposite9, SWT.RADIO | SWT.LEFT);
								this.setupButton9e.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton9e.setText(Messages.getString(MessageIds.GDE_MSGT1845));
								this.setupButton9e.setSelection(this.setupValue9 == 4);
								this.setupButton9e.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton9e.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton9e.getSelection()) {
											VarioToolTabItem.this.setupValue9 = 4;
											VarioToolTabItem.this.setupButton9a.setSelection(false);
											VarioToolTabItem.this.setupButton9b.setSelection(false);
											VarioToolTabItem.this.setupButton9c.setSelection(false);
											VarioToolTabItem.this.setupButton9d.setSelection(false);
											VarioToolTabItem.this.setupButton9f.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton9f = new Button(this.setupComposite9, SWT.RADIO | SWT.LEFT);
								this.setupButton9f.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton9f.setText(Messages.getString(MessageIds.GDE_MSGT1846));
								this.setupButton9f.setSelection(this.setupValue9 == 5);
								this.setupButton9f.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton9f.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton9f.getSelection()) {
											VarioToolTabItem.this.setupValue9 = 5;
											VarioToolTabItem.this.setupButton9a.setSelection(false);
											VarioToolTabItem.this.setupButton9b.setSelection(false);
											VarioToolTabItem.this.setupButton9c.setSelection(false);
											VarioToolTabItem.this.setupButton9d.setSelection(false);
											VarioToolTabItem.this.setupButton9e.setSelection(false);
										}
									}
								});
							}
						}
						this.setupGroup9.layout();
						this.setupGroup9.setMenu(this.popupmenu);
					}
					{
						this.setupGroup10 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout setupGroup10Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup10.setLayout(setupGroup10Layout);
						this.setupGroup10.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup10.setText(Messages.getString(MessageIds.GDE_MSGT1847));
						this.setupGroup10.setBounds(505, 361, 490, GDE.IS_MAC ? 145 : 135);
						{
							new CLabel(this.setupGroup10, SWT.NONE).setSize(5, 109);
						}
						{
							this.setupComposite10 = new Composite(this.setupGroup10, SWT.NONE);
							FillLayout setupComposite10Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
							RowData setupComposite10LData = new RowData();
							setupComposite10LData.width = 460;
							setupComposite10LData.height = 109;
							this.setupComposite10.setLayoutData(setupComposite10LData);
							this.setupComposite10.setLayout(setupComposite10Layout);
							{
								this.setupButton10a = new Button(this.setupComposite10, SWT.RADIO | SWT.LEFT);
								this.setupButton10a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton10a.setText(Messages.getString(MessageIds.GDE_MSGT1848));
								this.setupButton10a.setSelection(this.setupValue10 == 0);
								this.setupButton10a.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton10a.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton10a.getSelection()) {
											VarioToolTabItem.this.setupValue10 = 0;
											VarioToolTabItem.this.setupButton10b.setSelection(false);
											VarioToolTabItem.this.setupButton10c.setSelection(false);
											VarioToolTabItem.this.setupButton10d.setSelection(false);
											VarioToolTabItem.this.setupButton10e.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton10b = new Button(this.setupComposite10, SWT.RADIO | SWT.LEFT);
								this.setupButton10b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton10b.setText(Messages.getString(MessageIds.GDE_MSGT1849));
								this.setupButton10b.setSelection(this.setupValue10 == 1);
								this.setupButton10b.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton10b.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton10b.getSelection()) {
											VarioToolTabItem.this.setupValue10 = 1;
											VarioToolTabItem.this.setupButton10a.setSelection(false);
											VarioToolTabItem.this.setupButton10c.setSelection(false);
											VarioToolTabItem.this.setupButton10d.setSelection(false);
											VarioToolTabItem.this.setupButton10e.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton10c = new Button(this.setupComposite10, SWT.RADIO | SWT.LEFT);
								this.setupButton10c.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton10c.setText(Messages.getString(MessageIds.GDE_MSGT1850));
								this.setupButton10c.setSelection(this.setupValue10 == 2);
								this.setupButton10c.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton10c.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton10c.getSelection()) {
											VarioToolTabItem.this.setupValue10 = 2;
											VarioToolTabItem.this.setupButton10a.setSelection(false);
											VarioToolTabItem.this.setupButton10b.setSelection(false);
											VarioToolTabItem.this.setupButton10d.setSelection(false);
											VarioToolTabItem.this.setupButton10e.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton10d = new Button(this.setupComposite10, SWT.RADIO | SWT.LEFT);
								this.setupButton10d.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton10d.setText(Messages.getString(MessageIds.GDE_MSGT1852));
								this.setupButton10d.setSelection(this.setupValue10 == 3);
								this.setupButton10d.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton10d.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton10d.getSelection()) {
											VarioToolTabItem.this.setupValue10 = 3;
											VarioToolTabItem.this.setupButton10a.setSelection(false);
											VarioToolTabItem.this.setupButton10b.setSelection(false);
											VarioToolTabItem.this.setupButton10c.setSelection(false);
											VarioToolTabItem.this.setupButton10e.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton10e = new Button(this.setupComposite10, SWT.RADIO | SWT.LEFT);
								this.setupButton10e.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton10e.setText(Messages.getString(MessageIds.GDE_MSGT1853));
								this.setupButton10e.setSelection(this.setupValue10 == 4);
								this.setupButton10e.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton10e.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton10e.getSelection()) {
											VarioToolTabItem.this.setupValue10 = 4;
											VarioToolTabItem.this.setupButton10a.setSelection(false);
											VarioToolTabItem.this.setupButton10b.setSelection(false);
											VarioToolTabItem.this.setupButton10c.setSelection(false);
											VarioToolTabItem.this.setupButton10d.setSelection(false);
										}
									}
								});
							}
							{
								this.setupLabel10 = new CLabel(this.setupComposite10, SWT.NONE);
							}
						}
						this.setupGroup10.layout();
						this.setupGroup10.setMenu(this.popupmenu);
					}
					{
						this.setupGroup11 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout setupGroup11Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup11.setLayout(setupGroup11Layout);
						this.setupGroup11.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup11.setText(Messages.getString(MessageIds.GDE_MSGT1854));
						this.setupGroup11.setBounds(5, 497, 490, GDE.IS_MAC ? 55 : 45);
						{
							new CLabel(this.setupGroup3, SWT.NONE).setSize(5, 18);
						}
						{
							RowData setupSlider11LData = new RowData();
							setupSlider11LData.width = 160;
							setupSlider11LData.height = 18;
							this.setupSlider11 = new Slider(this.setupGroup11, SWT.BORDER);
							this.setupSlider11.setLayoutData(setupSlider11LData);
							this.setupSlider11.setMinimum(0);
							this.setupSlider11.setMaximum(135); // max 125
							this.setupSlider11.setIncrement(5);
							this.setupSlider11.setSelection(this.setupValue11);
							this.setupSlider11.setEnabled(this.setupValue11 != 0);
							this.setupSlider11.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupSlider11.widgetSelected, event=" + evt); //$NON-NLS-1$
									int tmpValue = VarioToolTabItem.this.setupSlider11.getSelection();
									if (tmpValue % 5 == 0) {
										if (tmpValue < 0)
											VarioToolTabItem.this.setupValue11 = 0;
										else if (tmpValue > 125)
											VarioToolTabItem.this.setupValue11 = 125;
										else
											VarioToolTabItem.this.setupValue11 = tmpValue;
										VarioToolTabItem.this.setupText11.setText(GDE.STRING_EMPTY + VarioToolTabItem.this.setupValue11);
									}
								}
							});
						}
						{
							this.setupLabel11a = new CLabel(this.setupGroup11, SWT.RIGHT);
							RowData setupLabel11aLData = new RowData();
							setupLabel11aLData.width = 91;
							setupLabel11aLData.height = 18;
							this.setupLabel11a.setLayoutData(setupLabel11aLData);
							this.setupLabel11a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel11a.setText(Messages.getString(MessageIds.GDE_MSGT1855));
						}
						{
							this.setupText11 = new Text(this.setupGroup11, SWT.CENTER | SWT.BORDER);
							RowData setupText11LData = new RowData();
							setupText11LData.width = 35;
							setupText11LData.height = 12;
							this.setupText11.setLayoutData(setupText11LData);
							this.setupText11.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
							this.setupText11.setText((this.setupValue11 == 0) ? GDE.STRING_EMPTY : GDE.STRING_EMPTY + this.setupValue11);
							this.setupText11.setEditable(false);
							this.setupText11.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
						}
						{
							this.setupLabel11b = new CLabel(this.setupGroup11, SWT.LEFT);
							RowData setupLabel11bLData = new RowData();
							setupLabel11bLData.width = 51;
							setupLabel11bLData.height = 18;
							this.setupLabel11b.setLayoutData(setupLabel11bLData);
							this.setupLabel11b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel11b.setText(Messages.getString(MessageIds.GDE_MSGT1856));
						}
						{
							this.setupButton11 = new Button(this.setupGroup11, SWT.CHECK | SWT.LEFT);
							RowData setupButton11LData = new RowData();
							setupButton11LData.width = 105;
							setupButton11LData.height = 18;
							this.setupButton11.setLayoutData(setupButton11LData);
							this.setupButton11.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupButton11.setText(Messages.getString(MessageIds.GDE_MSGT1811));
							this.setupButton11.setSelection(this.setupValue11 == 0);
							this.setupButton11.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton3c.widgetSelected, event=" + evt); //$NON-NLS-1$
									if (VarioToolTabItem.this.setupButton11.getSelection()) {
										VarioToolTabItem.this.setupValue11 = 0;
										VarioToolTabItem.this.setupText11.setText(GDE.STRING_EMPTY);
										VarioToolTabItem.this.setupSlider11.setEnabled(false);
									}
									else {
										VarioToolTabItem.this.setupValue11 = 60;
										VarioToolTabItem.this.setupText11.setText(GDE.STRING_EMPTY + VarioToolTabItem.this.setupValue11);
										VarioToolTabItem.this.setupSlider11.setEnabled(true);
										VarioToolTabItem.this.setupSlider11.setSelection(VarioToolTabItem.this.setupValue11);
									}
								}
							});
						}
						this.setupGroup11.layout();
						this.setupGroup11.setMenu(this.popupmenu);
					}
					{
						this.setupGroup12 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout setupGroup12Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup12.setLayout(setupGroup12Layout);
						this.setupGroup12.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup12.setText(Messages.getString(MessageIds.GDE_MSGT1857));
						this.setupGroup12.setBounds(505, 497, 490, GDE.IS_MAC ? 55 : 45);
						{
							new CLabel(this.setupGroup12, SWT.NONE).setSize(5, 18);
						}
						{
							RowData setupSlider12LData = new RowData();
							setupSlider12LData.width = 160;
							setupSlider12LData.height = 18;
							this.setupSlider12 = new Slider(this.setupGroup12, SWT.BORDER);
							this.setupSlider12.setLayoutData(setupSlider12LData);
							this.setupSlider12.setMinimum(50);
							this.setupSlider12.setMaximum(510); // max 50.0 * 10
							this.setupSlider12.setIncrement(5);
							this.setupSlider12.setSelection(this.setupValue12 / 2 * 10);
							this.setupSlider12.setEnabled(this.setupValue12 != 0);
							this.setupSlider12.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupSlider12.widgetSelected, event=" + evt); //$NON-NLS-1$
									int tmpValue = VarioToolTabItem.this.setupSlider12.getSelection();
									if (tmpValue % 5 == 0) {
										if (tmpValue < 50)
											tmpValue = 50;
										else if (tmpValue > 500) tmpValue = 500;

										VarioToolTabItem.this.setupText12.setText((tmpValue == 0) ? GDE.STRING_EMPTY : String.format("%.1f", tmpValue / 10.0)); //$NON-NLS-1$
										VarioToolTabItem.this.setupValue12 = tmpValue * 2 / 10;
									}
								}
							});
						}
						{
							this.setupLabel12a = new CLabel(this.setupGroup12, SWT.RIGHT);
							RowData setupLabel12aLData = new RowData();
							setupLabel12aLData.width = 85;
							setupLabel12aLData.height = 18;
							this.setupLabel12a.setLayoutData(setupLabel12aLData);
							this.setupLabel12a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel12a.setText(Messages.getString(MessageIds.GDE_MSGT1829));
						}
						{
							this.setupText12 = new Text(this.setupGroup12, SWT.CENTER | SWT.BORDER);
							RowData setupText12LData = new RowData();
							setupText12LData.width = 35;
							setupText12LData.height = 12;
							this.setupText12.setLayoutData(setupText12LData);
							this.setupText12.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
							this.setupText12.setText((this.setupValue12 == 0) ? GDE.STRING_EMPTY : String.format("%.1f", this.setupValue12 * 5 / 10.0)); //$NON-NLS-1$
							this.setupText12.setEditable(false);
							this.setupText12.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
						}
						{
							this.setupLabel12b = new CLabel(this.setupGroup12, SWT.LEFT);
							RowData setupLabel12bLData = new RowData();
							setupLabel12bLData.width = 51;
							setupLabel12bLData.height = 18;
							this.setupLabel12b.setLayoutData(setupLabel12bLData);
							this.setupLabel12b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel12b.setText(Messages.getString(MessageIds.GDE_MSGT1830));
						}
						{
							this.setupButton12 = new Button(this.setupGroup12, SWT.CHECK | SWT.LEFT);
							RowData setupButton12LData = new RowData();
							setupButton12LData.width = 105;
							setupButton12LData.height = 18;
							this.setupButton12.setLayoutData(setupButton12LData);
							this.setupButton12.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupButton12.setText(Messages.getString(MessageIds.GDE_MSGT1811));
							this.setupButton12.setSelection(this.setupValue12 == 0);
							this.setupButton12.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton12.widgetSelected, event=" + evt); //$NON-NLS-1$
									if (VarioToolTabItem.this.setupButton12.getSelection()) {
										VarioToolTabItem.this.setupValue12 = 0;
										VarioToolTabItem.this.setupText12.setText(GDE.STRING_EMPTY);
										VarioToolTabItem.this.setupSlider12.setEnabled(false);
									}
									else {
										VarioToolTabItem.this.setupValue12 = 150;
										VarioToolTabItem.this.setupText12.setText((VarioToolTabItem.this.setupValue12 == 0) ? GDE.STRING_EMPTY : String.format("%.1f", VarioToolTabItem.this.setupValue12 * 5 / 10.0)); //$NON-NLS-1$
										VarioToolTabItem.this.setupSlider12.setEnabled(true);
										VarioToolTabItem.this.setupSlider12.setSelection(VarioToolTabItem.this.setupValue12);
									}
								}
							});
						}
						this.setupGroup12.layout();
						this.setupGroup12.setMenu(this.popupmenu);
					}
					{
						this.setupGroup13 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout setupGroup13Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup13.setLayout(setupGroup13Layout);
						this.setupGroup13.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup13.setText(Messages.getString(MessageIds.GDE_MSGT1860));
						this.setupGroup13.setBounds(5, 545, 490, GDE.IS_MAC ? 100 : 90);
						{
							this.setupComposite13a = new Composite(this.setupGroup13, SWT.NONE);
							RowLayout setupComposite13Layout = new RowLayout(org.eclipse.swt.SWT.VERTICAL);
							RowData setupComposite13LData = new RowData();
							setupComposite13LData.width = 464;
							setupComposite13LData.height = 43;
							this.setupComposite13a.setLayoutData(setupComposite13LData);
							this.setupComposite13a.setLayout(setupComposite13Layout);
							{
								this.setupButton13a = new Button(this.setupComposite13a, SWT.RADIO | SWT.LEFT);
								RowData setupButton13aLData = new RowData();
								setupButton13aLData.width = 462;
								setupButton13aLData.height = 18;
								this.setupButton13a.setLayoutData(setupButton13aLData);
								this.setupButton13a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton13a.setText(Messages.getString(MessageIds.GDE_MSGT1861));
								this.setupButton13a.setSelection((Math.abs(this.setupValue13) % 2) == 1);
								this.setupButton13a.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton13a.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton13a.getSelection()) {
											VarioToolTabItem.this.setupValue13 = getSetupSlider13Value(VarioToolTabItem.this.setupSlider13.getSelection());
											VarioToolTabItem.this.setupSlider13.setEnabled(VarioToolTabItem.this.setupValue13 > 2);
											VarioToolTabItem.this.setupText13.setText(GDE.STRING_EMPTY + VarioToolTabItem.this.setupValue13);
											VarioToolTabItem.this.setupButton13b.setSelection(false);
											VarioToolTabItem.this.setupButton13c.setSelection(VarioToolTabItem.this.setupValue13 > 2);
										}
									}
								});
							}
							{
								this.setupButton13b = new Button(this.setupComposite13a, SWT.RADIO | SWT.LEFT);
								RowData setupButton13LData = new RowData();
								setupButton13LData.width = 462;
								setupButton13LData.height = 18;
								this.setupButton13b.setLayoutData(setupButton13LData);
								this.setupButton13b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton13b.setText(Messages.getString(MessageIds.GDE_MSGT1862));
								this.setupButton13b.setSelection((this.setupValue13 % 2) == 0);
								this.setupButton13b.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton13.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton13b.getSelection()) {
											VarioToolTabItem.this.setupValue13 = getSetupSlider13Value(VarioToolTabItem.this.setupSlider13.getSelection());
											VarioToolTabItem.this.setupSlider13.setEnabled(VarioToolTabItem.this.setupValue13 > 2);
											VarioToolTabItem.this.setupText13.setText(GDE.STRING_EMPTY + VarioToolTabItem.this.setupValue13);
											VarioToolTabItem.this.setupButton13a.setSelection(false);
											VarioToolTabItem.this.setupButton13c.setSelection(VarioToolTabItem.this.setupValue13 > 2);
										}
									}
								});
							}
						}
						{
							this.setupComposite13b = new Composite(this.setupGroup13, SWT.NONE);
							RowLayout composite1Layout1 = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
							this.setupComposite13b.setLayout(composite1Layout1);
							{
								this.setupButton13c = new Button(this.setupComposite13b, SWT.CHECK | SWT.LEFT);
								this.setupButton13c.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton13c.setText(Messages.getString(MessageIds.GDE_MSGT1863));
								this.setupButton13c.setSelection(this.setupValue13 > 2);
								this.setupButton13c.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton13c.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton13c.getSelection()) {
											VarioToolTabItem.this.setupValue13 = getSetupSlider13Value(VarioToolTabItem.this.setupSlider13.getSelection()) + (VarioToolTabItem.this.setupButton13a.getSelection() ? 1 : 2);
											VarioToolTabItem.this.setupSlider13.setEnabled(true);
											VarioToolTabItem.this.setupText13.setText(GDE.STRING_EMPTY + VarioToolTabItem.this.setupValue13);
										}
										else {
											VarioToolTabItem.this.setupSlider13.setEnabled(false);
											VarioToolTabItem.this.setupText13.setText(GDE.STRING_EMPTY);
										}
									}
								});
							}
							{
								RowData setupSlider13LData = new RowData();
								setupSlider13LData.width = 92;
								setupSlider13LData.height = 18;
								this.setupSlider13 = new Slider(this.setupComposite13b, SWT.BORDER);
								this.setupSlider13.setLayoutData(setupSlider13LData);
								this.setupSlider13.setMinimum(20);
								this.setupSlider13.setMaximum(110); // max 10
								this.setupSlider13.setIncrement(40);
								this.setupSlider13.setSelection((this.setupValue13 < 4) ? 20 : (this.setupValue13 > 7) ? 100 : 50);
								this.setupSlider13.setEnabled(this.setupValue13 > 2);
								this.setupSlider13.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupSlider13.widgetSelected, event=" + evt); //$NON-NLS-1$
										int tmpValue = VarioToolTabItem.this.setupSlider13.getSelection();
										if (tmpValue % 10 == 0) {
											tmpValue = getSetupSlider13Value(tmpValue);

											VarioToolTabItem.this.setupText13.setText(GDE.STRING_EMPTY + tmpValue);
											VarioToolTabItem.this.setupValue13 = tmpValue + (VarioToolTabItem.this.setupButton13a.getSelection() ? 1 : 2);
										}
									}
								});
							}
							{
								this.setupText13 = new Text(this.setupComposite13b, SWT.CENTER | SWT.BORDER);
								RowData setupText13LData = new RowData();
								setupText13LData.width = 20;
								setupText13LData.height = 12;
								this.setupText13.setLayoutData(setupText13LData);
								this.setupText13.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
								this.setupText13.setText(this.setupValue13 <= 2 ? GDE.STRING_EMPTY : GDE.STRING_EMPTY + this.setupValues13[this.setupValue13]);
								this.setupText13.setEditable(false);
								this.setupText13.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
							}
							{
								this.setupLabel13 = new CLabel(this.setupComposite13b, SWT.NONE);
								this.setupLabel13.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupLabel13.setText(Messages.getString(MessageIds.GDE_MSGT1864));
								RowData setupText13LData = new RowData();
								setupText13LData.width = 159;
								setupText13LData.height = 19;
								this.setupLabel13.setLayoutData(setupText13LData);
							}
						}
						this.setupGroup13.layout();
						this.setupGroup13.setMenu(this.popupmenu);
					}
					{
						this.setupGroup14 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout group1Layout1 = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup14.setLayout(group1Layout1);
						this.setupGroup14.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup14.setText(Messages.getString(MessageIds.GDE_MSGT1865));
						this.setupGroup14.setBounds(505, 545, 490, GDE.IS_MAC ? 100 : 90);
						{
							new CLabel(this.setupGroup14, SWT.NONE).setSize(5, 37);
						}
						{
							this.setupComposite14 = new Composite(this.setupGroup14, SWT.NONE);
							RowLayout setupComposite14Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
							RowData setupComposite14LData = new RowData();
							setupComposite14LData.width = 455;
							setupComposite14LData.height = 65;
							this.setupComposite14.setLayoutData(setupComposite14LData);
							this.setupComposite14.setLayout(setupComposite14Layout);
							{
								this.setupButton14a = new Button(this.setupComposite14, SWT.RADIO | SWT.LEFT);
								RowData setupButton14aLData = new RowData();
								setupButton14aLData.width = 322;
								setupButton14aLData.height = 18;
								this.setupButton14a.setLayoutData(setupButton14aLData);
								this.setupButton14a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton14a.setText(Messages.getString(MessageIds.GDE_MSGT1866));
								this.setupButton14a.setSelection(this.setupValue14 == 1);
								this.setupButton14a.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton14a.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton14a.getSelection()) {
											VarioToolTabItem.this.setupValue14 = 1;
											VarioToolTabItem.this.setupButton14b.setSelection(false);
											VarioToolTabItem.this.setupButton14c.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton14b = new Button(this.setupComposite14, SWT.RADIO | SWT.LEFT);
								RowData setupButton14bLData = new RowData();
								setupButton14bLData.width = 322;
								setupButton14bLData.height = 18;
								this.setupButton14b.setLayoutData(setupButton14bLData);
								this.setupButton14b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton14b.setText(Messages.getString(MessageIds.GDE_MSGT1867));
								this.setupButton14b.setSelection(this.setupValue14 == 2);
								this.setupButton14b.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton14b.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton14b.getSelection()) {
											VarioToolTabItem.this.setupValue14 = 2;
											VarioToolTabItem.this.setupButton14a.setSelection(false);
											VarioToolTabItem.this.setupButton14c.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton14c = new Button(this.setupComposite14, SWT.RADIO | SWT.LEFT);
								RowData setupButton14cLData = new RowData();
								setupButton14cLData.width = 322;
								setupButton14cLData.height = 18;
								this.setupButton14c.setLayoutData(setupButton14cLData);
								this.setupButton14c.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton14c.setText(Messages.getString(MessageIds.GDE_MSGT1868));
								this.setupButton14c.setSelection(this.setupValue14 == 3);
								this.setupButton14c.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton14c.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton14c.getSelection()) {
											VarioToolTabItem.this.setupValue14 = 3;
											VarioToolTabItem.this.setupButton14a.setSelection(false);
											VarioToolTabItem.this.setupButton14b.setSelection(false);
										}
									}
								});
							}
						}
						this.setupGroup14.layout();
						this.setupGroup14.setMenu(this.popupmenu);
					}
					{
						this.setupGroup15 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout setupGroup15Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup15.setLayout(setupGroup15Layout);
						this.setupGroup15.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup15.setText(Messages.getString(MessageIds.GDE_MSGT1869));
						this.setupGroup15.setBounds(5, 639, 490, GDE.IS_MAC ? 55 : 45);
						{
							new CLabel(this.setupGroup15, SWT.NONE);
						}
						{
							this.setupButton15a = new Button(this.setupGroup15, SWT.RADIO | SWT.LEFT);
							this.setupButton15a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupButton15a.setText(Messages.getString(MessageIds.GDE_MSGT1870));
							this.setupButton15a.setSelection(this.setupValue15 == 1);
							this.setupButton15a.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton15a.widgetSelected, event=" + evt); //$NON-NLS-1$
									if (VarioToolTabItem.this.setupButton15a.getSelection()) {
										VarioToolTabItem.this.setupValue15 = 1;
										VarioToolTabItem.this.setupButton15b.setSelection(false);
									}
								}
							});
						}
						{
							this.setupButton15b = new Button(this.setupGroup15, SWT.RADIO | SWT.LEFT);
							this.setupButton15b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupButton15b.setText(Messages.getString(MessageIds.GDE_MSGT1871));
							this.setupButton15b.setSelection(this.setupValue15 == 0);
							this.setupButton15b.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(java.util.logging.Level.FINEST, "setupButton15b.widgetSelected, event=" + evt); //$NON-NLS-1$
									if (VarioToolTabItem.this.setupButton15b.getSelection()) {
										VarioToolTabItem.this.setupValue15 = 0;
										VarioToolTabItem.this.setupButton15a.setSelection(false);
									}
								}
							});
						}
						this.setupGroup15.layout();
						this.setupGroup15.setMenu(this.popupmenu);
					}
					{
						this.setupGroup16 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout setupGroup16Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup16.setLayout(setupGroup16Layout);
						this.setupGroup16.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup16.setText(Messages.getString(MessageIds.GDE_MSGT1872));
						this.setupGroup16.setBounds(505, 639, 490, GDE.IS_MAC ? 55 : 45);
						{
							new CLabel(this.setupGroup16, SWT.NONE).setSize(5, 18);
						}
						{
							RowData setupSlider16LData = new RowData();
							setupSlider16LData.width = 160;
							setupSlider16LData.height = 18;
							this.setupSlider16 = new Slider(this.setupGroup16, SWT.BORDER);
							this.setupSlider16.setLayoutData(setupSlider16LData);
							this.setupSlider16.setMinimum(3);
							this.setupSlider16.setMaximum(30); // max 20
							this.setupSlider16.setIncrement(1);
							this.setupSlider16.setSelection(this.setupValue16);
							this.setupSlider16.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(Level.FINEST, "setupSlider16.widgetSelected, event=" + evt); //$NON-NLS-1$
									int tmpValue = VarioToolTabItem.this.setupSlider16.getSelection();
									if (tmpValue <= 3)
										VarioToolTabItem.this.setupValue16 = 3;
									else if (tmpValue > 20)
										VarioToolTabItem.this.setupValue16 = 20;
									else
										VarioToolTabItem.this.setupValue16 = tmpValue;
									VarioToolTabItem.this.setupText16.setText(GDE.STRING_EMPTY + VarioToolTabItem.this.setupValue16);
								}
							});
						}
						{
							this.setupLabel16a = new CLabel(this.setupGroup16, SWT.RIGHT);
							RowData setupLabel16aLData = new RowData();
							setupLabel16aLData.width = 85;
							setupLabel16aLData.height = 18;
							this.setupLabel16a.setLayoutData(setupLabel16aLData);
							this.setupLabel16a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel16a.setText(Messages.getString(MessageIds.GDE_MSGT1851));
						}
						{
							this.setupText16 = new Text(this.setupGroup16, SWT.CENTER | SWT.BORDER);
							RowData setupText16LData = new RowData();
							setupText16LData.width = 35;
							setupText16LData.height = 12;
							this.setupText16.setLayoutData(setupText16LData);
							this.setupText16.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
							this.setupText16.setText(GDE.STRING_EMPTY + this.setupValue16);
						}
						{
							this.setupLabel16b = new CLabel(this.setupGroup16, SWT.LEFT);
							RowData setupLabel16bLData = new RowData();
							setupLabel16bLData.width = 70;
							setupLabel16bLData.height = 18;
							this.setupLabel16b.setLayoutData(setupLabel16bLData);
							this.setupLabel16b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel16b.setText(Messages.getString(MessageIds.GDE_MSGT1810));
						}
						this.setupGroup16.layout();
						this.setupGroup16.setMenu(this.popupmenu);
					}
					{
						this.setupGroup17 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout setupGroup17Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup17.setLayout(setupGroup17Layout);
						this.setupGroup17.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup17.setText(Messages.getString(MessageIds.GDE_MSGT1873));
						this.setupGroup17.setBounds(5, 687, 490, GDE.IS_MAC ? 80 : 70);
						{
							new CLabel(this.setupGroup17, SWT.NONE).setSize(5, 18);
						}
						{
							this.setupButton17 = new Button(this.setupGroup17, SWT.CHECK | SWT.LEFT);
							this.setupButton17.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupButton17.setText(Messages.getString(MessageIds.GDE_MSGT1874));
							RowData setupButton17LData = new RowData();
							setupButton17LData.width = GDE.IS_MAC ? 460 : 465;
							setupButton17LData.height = 18;
							this.setupButton17.setLayoutData(setupButton17LData);
							this.setupButton17.setSelection((this.setupValue17 & 0x80) == 0x80);
							this.setupButton17.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(Level.FINEST, "setupButton17.widgetSelected, event=" + evt); //$NON-NLS-1$
									if (VarioToolTabItem.this.setupButton17.getSelection()) {
										VarioToolTabItem.this.setupValue17 = VarioToolTabItem.this.setupValue17 + 0x80;
									}
									else {
										VarioToolTabItem.this.setupValue17 = VarioToolTabItem.this.setupValue17 - 0x80;
									}
								}
							});
						}
						{
							new CLabel(this.setupGroup17, SWT.NONE).setSize(5, 18);
						}
						{
							RowData setupSlider17LData = new RowData();
							setupSlider17LData.width = 160;
							setupSlider17LData.height = 18;
							this.setupSlider17 = new Slider(this.setupGroup17, SWT.BORDER);
							this.setupSlider17.setLayoutData(setupSlider17LData);
							this.setupSlider17.setMinimum(5);
							this.setupSlider17.setMaximum(109); // max 99
							this.setupSlider17.setIncrement(5);
							this.setupSlider17.setSelection(this.setupValue17);
							this.setupSlider17.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(Level.FINEST, "setupSlider17.widgetSelected, event=" + evt); //$NON-NLS-1$
									int tmpValue = VarioToolTabItem.this.setupSlider17.getSelection();
									if (tmpValue % 5 == 0) {
										if (tmpValue <= 5)
											VarioToolTabItem.this.setupValue17 = 5;
										else if (tmpValue >= 99)
											VarioToolTabItem.this.setupValue17 = 99;
										else
											VarioToolTabItem.this.setupValue17 = tmpValue;
										VarioToolTabItem.this.setupText17.setText(String.format("%.1f", (VarioToolTabItem.this.setupValue17 & 0x7f) / 10.0)); //$NON-NLS-1$
									}
								}
							});
						}
						{
							this.setupLabel17a = new CLabel(this.setupGroup17, SWT.RIGHT);
							RowData setupLabel17aLData = new RowData();
							setupLabel17aLData.width = 25;
							setupLabel17aLData.height = 18;
							this.setupLabel17a.setLayoutData(setupLabel17aLData);
							this.setupLabel17a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel17a.setText(Messages.getString(MessageIds.GDE_MSGT1875));
						}
						{
							this.setupText17 = new Text(this.setupGroup17, SWT.CENTER | SWT.BORDER);
							RowData setupText17LData = new RowData();
							setupText17LData.width = 25;
							setupText17LData.height = 12;
							this.setupText17.setLayoutData(setupText17LData);
							this.setupText17.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
							this.setupText17.setText(String.format("%.1f", (this.setupValue17 & 0x7f) / 10.0)); //$NON-NLS-1$
						}
						{
							this.setupLabel17b = new CLabel(this.setupGroup17, SWT.LEFT);
							RowData setupLabel17bLData = new RowData();
							setupLabel17bLData.width = 240;
							setupLabel17bLData.height = 18;
							this.setupLabel17b.setLayoutData(setupLabel17bLData);
							this.setupLabel17b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel17b.setText(Messages.getString(MessageIds.GDE_MSGT1876));
						}
						this.setupGroup17.layout();
						this.setupGroup17.setMenu(this.popupmenu);
					}
					{
						this.setupGroup18 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout setupGroup18Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup18.setLayout(setupGroup18Layout);
						this.setupGroup18.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup18.setText(Messages.getString(MessageIds.GDE_MSGT1878));
						this.setupGroup18.setBounds(505, 687, 490, GDE.IS_MAC ? 80 : 70);
						{
							new CLabel(this.setupGroup18, SWT.NONE).setSize(5, 18);
						}
						{
							this.setupButton18 = new Button(this.setupGroup18, SWT.CHECK | SWT.LEFT);
							this.setupButton18.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupButton18.setText(Messages.getString(MessageIds.GDE_MSGT1881));
							RowData setupButton18LData = new RowData();
							setupButton18LData.width = GDE.IS_MAC ? 460 : 465;
							setupButton18LData.height = 18;
							this.setupButton18.setLayoutData(setupButton18LData);
							this.setupButton18.setSelection(this.setupValue18 == 0);
							this.setupButton18.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(Level.FINEST, "setupButton18.widgetSelected, event=" + evt); //$NON-NLS-1$
									if (VarioToolTabItem.this.setupButton18.getSelection()) {
										VarioToolTabItem.this.setupValue18 = 0;
										VarioToolTabItem.this.setupText18.setText(GDE.STRING_EMPTY);
										VarioToolTabItem.this.setupSlider18.setEnabled(false);
									}
									else {
										VarioToolTabItem.this.setupValue18 = 20;
										VarioToolTabItem.this.setupText18.setText(GDE.STRING_EMPTY + VarioToolTabItem.this.setupValue18);
										VarioToolTabItem.this.setupSlider18.setEnabled(true);
										VarioToolTabItem.this.setupSlider18.setSelection(VarioToolTabItem.this.setupValue18);
									}
								}
							});
						}
						{
							new CLabel(this.setupGroup18, SWT.NONE).setSize(5, 18);
						}
						{
							RowData setupSlider18LData = new RowData();
							setupSlider18LData.width = 160;
							setupSlider18LData.height = 18;
							this.setupSlider18 = new Slider(this.setupGroup18, SWT.BORDER);
							this.setupSlider18.setLayoutData(setupSlider18LData);
							this.setupSlider18.setMinimum(20);
							this.setupSlider18.setMaximum(110); // max 100
							this.setupSlider18.setIncrement(10);
							this.setupSlider18.setSelection(this.setupValue18);
							this.setupSlider18.setEnabled(this.setupValue18 != 0);
							this.setupSlider18.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									VarioToolTabItem.log.log(Level.FINEST, "setupSlider18.widgetSelected, event=" + evt); //$NON-NLS-1$
									int tmpValue = VarioToolTabItem.this.setupSlider18.getSelection();
									if (tmpValue % 10 == 0) {
										if (tmpValue < 20)
											VarioToolTabItem.this.setupValue18 = 20;
										else if (tmpValue > 100)
											VarioToolTabItem.this.setupValue18 = 100;
										else
											VarioToolTabItem.this.setupValue18 = tmpValue;
										VarioToolTabItem.this.setupText18.setText(GDE.STRING_EMPTY + VarioToolTabItem.this.setupValue18);
									}
								}
							});
						}
						{
							this.setupLabel18a = new CLabel(this.setupGroup18, SWT.RIGHT);
							RowData setupLabel18aLData = new RowData();
							setupLabel18aLData.width = 25;
							setupLabel18aLData.height = 18;
							this.setupLabel18a.setLayoutData(setupLabel18aLData);
							this.setupLabel18a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel18a.setText(Messages.getString(MessageIds.GDE_MSGT1879));
						}
						{
							this.setupText18 = new Text(this.setupGroup18, SWT.CENTER | SWT.BORDER);
							RowData setupText18LData = new RowData();
							setupText18LData.width = 25;
							setupText18LData.height = 12;
							this.setupText18.setLayoutData(setupText18LData);
							this.setupText18.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
							this.setupText18.setText(this.setupValue18 == 0 ? GDE.STRING_EMPTY : GDE.STRING_EMPTY + this.setupValue18);
							this.setupText18.setEditable(false);
							this.setupText18.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
						}
						{
							this.setupLabel18b = new CLabel(this.setupGroup18, SWT.LEFT);
							RowData setupLabel18bLData = new RowData();
							setupLabel18bLData.width = 240;
							setupLabel18bLData.height = 16;
							this.setupLabel18b.setLayoutData(setupLabel18bLData);
							this.setupLabel18b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupLabel18b.setText(Messages.getString(MessageIds.GDE_MSGT1880));
						}
						this.setupGroup18.layout();
						this.setupGroup18.setMenu(this.popupmenu);
					}
					if (!this.isDataVarioTool) {
						this.setupGroup19 = new Group(this.innerContentComposite, SWT.NONE);
						RowLayout setupGroup19Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.setupGroup19.setLayout(setupGroup19Layout);
						this.setupGroup19.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.setupGroup19.setText(Messages.getString(MessageIds.GDE_MSGT1882));
						this.setupGroup19.setBounds(5, 760, 990, GDE.IS_MAC ? 115 : 105);
						{
							new CLabel(this.setupGroup19, SWT.NONE).setSize(5, 85);
						}
						{
							this.setupComposite19a = new Composite(this.setupGroup19, SWT.NONE);
							RowLayout setupComposite19aLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
							RowData setupComposite19aLData = new RowData();
							setupComposite19aLData.width = 207;
							setupComposite19aLData.height = 78;
							this.setupComposite19a.setLayoutData(setupComposite19aLData);
							this.setupComposite19a.setLayout(setupComposite19aLayout);
							{
								RowData setupSlider19LData = new RowData();
								setupSlider19LData.width = 136;
								setupSlider19LData.height = 17;
								this.setupSlider19 = new Slider(this.setupComposite19a, SWT.NONE);
								this.setupSlider19.setLayoutData(setupSlider19LData);
								this.setupSlider19.setMinimum(0);
								this.setupSlider19.setMaximum(40); // max 3
								this.setupSlider19.setIncrement(10);
								this.setupSlider19.setSelection((this.setupValue19 & 0x03) * 10);
								this.setupSlider19.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(Level.FINEST, "setupSlider19.widgetSelected, event=" + evt); //$NON-NLS-1$
										int tmpValue = VarioToolTabItem.this.setupSlider19.getSelection();
										if (tmpValue % 10 == 0) {
											if (tmpValue <= 0)
												VarioToolTabItem.this.setupValue19 = 0;
											else if (tmpValue >= 30)
												VarioToolTabItem.this.setupValue19 = 3;
											else
												VarioToolTabItem.this.setupValue19 = tmpValue / 10;
											VarioToolTabItem.this.setupValue19 = VarioToolTabItem.this.setupButton19a.getSelection() ? (VarioToolTabItem.this.setupValue19 & 0x07) : VarioToolTabItem.this.setupButton19b
													.getSelection() ? (VarioToolTabItem.this.setupValue19 & 0x07) | 0x08
													: VarioToolTabItem.this.setupButton19c.getSelection() ? (VarioToolTabItem.this.setupValue19 & 0x07) | 0x10
															: VarioToolTabItem.this.setupButton19c.getSelection() ? (VarioToolTabItem.this.setupValue19 & 0x07) | 0x20 : (VarioToolTabItem.this.setupValue19 & 0x07);

											VarioToolTabItem.this.setupText19.setText(GDE.STRING_EMPTY + (VarioToolTabItem.this.setupValue19 & 0x03));
										}
									}
								});
							}
							{
								this.setupLabel19a = new CLabel(this.setupComposite19a, SWT.NONE);
								this.setupLabel19a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupLabel19a.setText(Messages.getString(MessageIds.GDE_MSGT1883));
							}
							{
								this.setupText19 = new Text(this.setupComposite19a, SWT.CENTER | SWT.BORDER);
								RowData setupText19LData = new RowData();
								setupText19LData.width = 30;
								setupText19LData.height = 12;
								this.setupText19.setLayoutData(setupText19LData);
								this.setupText19.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
								this.setupText19.setText(GDE.STRING_EMPTY + (this.setupValue19 & 0x03));
							}
							{
								this.setupLabel19b = new CLabel(this.setupComposite19a, SWT.NONE);
								this.setupLabel19b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupLabel19b.setText(Messages.getString(MessageIds.GDE_MSGT1884));
							}
						}
						{
							this.setupComposite19b = new Composite(this.setupGroup19, SWT.NONE);
							RowLayout setupComposite19bLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
							RowData setupComposite19bLData = new RowData();
							setupComposite19bLData.width = 308;
							setupComposite19bLData.height = GDE.IS_MAC ? 85 : 78;
							this.setupComposite19b.setLayoutData(setupComposite19bLData);
							this.setupComposite19b.setLayout(setupComposite19bLayout);
							{
								this.setupButton19a = new Button(this.setupComposite19b, SWT.RADIO | SWT.LEFT);
								this.setupButton19a.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton19a.setText(Messages.getString(MessageIds.GDE_MSGT1885));
								this.setupButton19a.setSelection((this.setupValue19 & 0x38) == 0x00); // 000000
								this.setupButton19a.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(Level.FINEST, "setupButton19a.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton19a.getSelection()) {
											VarioToolTabItem.this.setupValue19 = VarioToolTabItem.this.setupValue19 & 0x07;
											VarioToolTabItem.this.setupButton19b.setSelection(false);
											VarioToolTabItem.this.setupButton19c.setSelection(false);
											VarioToolTabItem.this.setupButton19d.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton19b = new Button(this.setupComposite19b, SWT.RADIO | SWT.LEFT);
								this.setupButton19b.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton19b.setText(Messages.getString(MessageIds.GDE_MSGT1886));
								this.setupButton19b.setSelection((this.setupValue19 & 0x38) == 0x08); // 001000
								this.setupButton19b.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(Level.FINEST, "setupButton19b.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton19b.getSelection()) {
											VarioToolTabItem.this.setupValue19 = (VarioToolTabItem.this.setupValue19 & 0x07) | 0x08;
											VarioToolTabItem.this.setupButton19a.setSelection(false);
											VarioToolTabItem.this.setupButton19c.setSelection(false);
											VarioToolTabItem.this.setupButton19d.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton19c = new Button(this.setupComposite19b, SWT.RADIO | SWT.LEFT);
								this.setupButton19c.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton19c.setText(Messages.getString(MessageIds.GDE_MSGT1887));
								this.setupButton19c.setSelection((this.setupValue19 & 0x38) == 0x10); // 010000
								this.setupButton19c.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(Level.FINEST, "setupButton19c.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton19c.getSelection()) {
											VarioToolTabItem.this.setupValue19 = (VarioToolTabItem.this.setupValue19 & 0x07) | 0x10;
											VarioToolTabItem.this.setupButton19a.setSelection(false);
											VarioToolTabItem.this.setupButton19b.setSelection(false);
											VarioToolTabItem.this.setupButton19d.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton19d = new Button(this.setupComposite19b, SWT.RADIO | SWT.LEFT);
								this.setupButton19d.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton19d.setText(Messages.getString(MessageIds.GDE_MSGT1888));
								this.setupButton19d.setSelection((this.setupValue19 & 0x38) == 0x20); // 100000
								this.setupButton19d.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(Level.FINEST, "setupButton19d.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton19d.getSelection()) {
											VarioToolTabItem.this.setupValue19 = (VarioToolTabItem.this.setupValue19 & 0x07) | 0x20;
											VarioToolTabItem.this.setupButton19a.setSelection(false);
											VarioToolTabItem.this.setupButton19b.setSelection(false);
											VarioToolTabItem.this.setupButton19c.setSelection(false);
										}
									}
								});
							}
						}
						{
							this.setupComposite19c = new Composite(this.setupGroup19, SWT.NONE);
							RowLayout setupComposite19cLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
							RowData setupComposite19cLData = new RowData();
							setupComposite19cLData.width = 439;
							setupComposite19cLData.height = 78;
							this.setupComposite19c.setLayoutData(setupComposite19cLData);
							this.setupComposite19c.setLayout(setupComposite19cLayout);
							{
								this.setupButton19e = new Button(this.setupComposite19c, SWT.RADIO | SWT.LEFT);
								this.setupButton19e.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton19e.setText(Messages.getString(MessageIds.GDE_MSGT1889));
								this.setupButton19e.setSelection(true);
								this.setupButton19e.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(Level.FINEST, "setupButton19e.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton19e.getSelection()) {
											VarioToolTabItem.this.setupButton19f.setSelection(false);
											VarioToolTabItem.this.setupButton19g.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton19f = new Button(this.setupComposite19c, SWT.RADIO | SWT.LEFT);
								this.setupButton19f.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton19f.setText(Messages.getString(MessageIds.GDE_MSGT1890));
								this.setupButton19f.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(Level.FINEST, "setupButton19f.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton19f.getSelection()) {
											VarioToolTabItem.this.setupButton19e.setSelection(false);
											VarioToolTabItem.this.setupButton19g.setSelection(false);
										}
									}
								});
							}
							{
								this.setupButton19g = new Button(this.setupComposite19c, SWT.RADIO | SWT.LEFT);
								this.setupButton19g.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.setupButton19g.setText(Messages.getString(MessageIds.GDE_MSGT1891));
								this.setupButton19g.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										VarioToolTabItem.log.log(Level.FINEST, "setupButton19g.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (VarioToolTabItem.this.setupButton19g.getSelection()) {
											VarioToolTabItem.this.setupButton19e.setSelection(false);
											VarioToolTabItem.this.setupButton19f.setSelection(false);
										}
									}
								});
							}
						}
						this.setupGroup19.layout();
						this.setupGroup19.setMenu(this.popupmenu);
					}
					this.mainTabScrolledComposite.setContent(this.innerContentComposite);
					this.innerContentComposite.setMenu(this.popupmenu);
					this.mainTabScrolledComposite.setMenu(this.popupmenu);

				}
			} // end scrolled composite
			this.contextMenu.createMenu(this.popupmenu);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	void loadSetup() {
		FileDialog fd = this.application.openFileOpenDialog(Messages.getString(MessageIds.GDE_MSGT1892), new String[] { GDE.FILE_ENDING_STAR_HEX, GDE.FILE_ENDING_STAR }, this.device.getDataBlockPreferredDataLocation(), this.getDefaultFileName(), SWT.SINGLE);
		String selectedSetupFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + fd.getFileName();
		VarioToolTabItem.log.log(Level.FINE, "selectedSetupFile = " + selectedSetupFile); //$NON-NLS-1$

		if (fd.getFileName().length() > 4) {

			try {
				FileInputStream file_input = new FileInputStream(new File(selectedSetupFile));
				DataInputStream data_in = new DataInputStream(file_input);
				byte[] buffer = new byte[20];
				int size = data_in.read(buffer);
				data_in.close();

				VarioToolTabItem.log.log(Level.FINEST, "red bytes = " + size); //$NON-NLS-1$
				if (this.isDataVarioTool) {
					this.setupValue0 = buffer[0]; // transmit channel
					this.setupSlider0.setSelection(this.setupValue0);
					this.channelText.setText(GDE.STRING_EMPTY + this.setupValue0);
					this.frequencyText.setText(String.format("%.3f", (433.050 + this.setupValue0 * 0.025))); //$NON-NLS-1$
				}
				this.setupValue1 = buffer[1]; // repeat positive height 
				this.setupSlider1.setSelection(this.setupValue1);
				this.setupText1.setText(GDE.STRING_EMPTY + this.setupValue1);

				this.setupValue2 = buffer[2]; // repeat negative height 
				this.setupSlider2.setSelection(this.setupValue2);
				this.setupText2.setText(GDE.STRING_EMPTY + this.setupValue2);

				this.setupValue3 = buffer[3]; // repeat integral height 
				this.setupSlider3.setSelection(this.setupValue3);
				this.setupSlider3.setEnabled(this.setupValue3 != 60);
				this.setupText3.setText((this.setupValue3 != 60) ? GDE.STRING_EMPTY + this.setupValue3 : GDE.STRING_EMPTY);
				this.setupButton3c.setSelection(this.setupValue3 == 60);

				this.setupValue4 = buffer[4]; // velocity
				this.setupButton4a.setSelection(this.setupValue4 == 1);
				this.setupButton4b.setSelection(this.setupValue4 == 3);
				this.setupButton4c.setSelection(this.setupValue4 == 4);
				this.setupButton4d.setSelection(this.setupValue4 == 5);
				this.setupButton4e.setSelection(this.setupValue4 == 6);
				this.setupButton4f.setSelection(this.setupValue4 == 2);

				this.setupValue5 = buffer[5]; // vario function
				this.setupButton5a.setSelection(this.setupValue5 == 1);
				this.setupButton5b.setSelection(this.setupValue5 == 2);
				this.setupButton5c.setSelection(this.setupValue5 == 3);
				this.setupButton5d.setSelection(this.setupValue5 == 4);

				this.setupValue6 = buffer[6]; // receiver warn voltage level
				this.setupSlider6.setSelection(this.setupValue6);
				this.setupText6.setText(String.format("%.1f", this.setupValue6 / 10.0)); //$NON-NLS-1$

				this.setupValue7 = buffer[7]; // sink ton level
				this.setupSlider7.setSelection(this.setupValue7);
				this.setupText7.setText(String.format("%.1f", this.setupValue7 / -10.0)); //$NON-NLS-1$

				this.setupValue8 = buffer[8]; // vario ton mode
				this.setupButton8a.setSelection(this.setupValue8 == 0);
				this.setupButton8b.setSelection(this.setupValue8 == 1);
				this.setupButton8c.setSelection(this.setupValue8 == 2);
				this.setupButton8d.setSelection(this.setupValue8 == 3);
				this.setupButton8e.setSelection(this.setupValue8 == 4);

				this.setupValue9 = buffer[9]; // motor battery voltage/current
				this.setupButton9a.setSelection(this.setupValue9 == 0);
				this.setupButton9b.setSelection(this.setupValue9 == 1);
				this.setupButton9c.setSelection(this.setupValue9 == 2);
				this.setupButton9d.setSelection(this.setupValue9 == 3);
				this.setupButton9e.setSelection(this.setupValue9 == 4);
				this.setupButton9f.setSelection(this.setupValue9 == 5);

				this.setupValue10 = buffer[10]; // sensor interface
				this.setupButton10a.setSelection(this.setupValue10 == 0);
				this.setupButton10b.setSelection(this.setupValue10 == 1);
				this.setupButton10c.setSelection(this.setupValue10 == 2);
				this.setupButton10d.setSelection(this.setupValue10 == 3);
				this.setupButton10e.setSelection(this.setupValue10 == 4);

				this.setupValue11 = buffer[11]; // temperature alarm level
				this.setupSlider11.setSelection(this.setupValue11);
				this.setupSlider11.setEnabled(this.setupValue11 != 0);
				this.setupText11.setText((this.setupValue11 == 0) ? GDE.STRING_EMPTY : GDE.STRING_EMPTY + this.setupValue11);
				this.setupButton11.setSelection(this.setupValue11 == 0);

				this.setupValue12 = buffer[12]; // motor battery voltage alarm level
				this.setupSlider12.setSelection(this.setupValue12 / 2 * 10);
				this.setupSlider12.setEnabled(this.setupValue12 != 0);
				this.setupText12.setText((this.setupValue12 == 0) ? GDE.STRING_EMPTY : String.format("%.1f", this.setupValue12 * 5.0 / 10)); //$NON-NLS-1$
				this.setupButton12.setSelection(this.setupValue12 == 0);

				this.setupValue13 = buffer[13]; // integral vario config
				this.setupButton13a.setSelection((Math.abs(this.setupValue13) % 2) == 1);
				this.setupButton13b.setSelection((Math.abs(this.setupValue13) % 2) == 0);
				this.setupButton13c.setSelection(this.setupValue13 > 2);
				this.setupSlider13.setEnabled(this.setupValue13 > 2);
				this.setupSlider13.setSelection((this.setupValue13 < 4) ? 20 : (this.setupValue13 > 7) ? 100 : 50);
				this.setupText13.setText(this.setupValue13 <= 2 ? GDE.STRING_EMPTY : GDE.STRING_EMPTY + this.setupValues13[this.setupValue13]);

				this.setupValue14 = buffer[14]; // current sensor type
				this.setupButton14a.setSelection(this.setupValue14 == 1);
				this.setupButton14b.setSelection(this.setupValue14 == 2);
				this.setupButton14c.setSelection(this.setupValue14 == 3);

				this.setupValue15 = buffer[15]; // height unit
				this.setupButton15a.setSelection(this.setupValue15 == 1);
				this.setupButton15b.setSelection(this.setupValue15 == 0);

				this.setupValue16 = buffer[16]; // current delay
				this.setupSlider16.setSelection(this.setupValue16);
				this.setupText16.setText(GDE.STRING_EMPTY + this.setupValue16);

				this.setupValue17 = buffer[17]; // temperature alarm level
				this.setupButton17.setSelection((this.setupValue17 & 0x80) == 0x80);
				this.setupSlider17.setSelection(this.setupValue17);
				this.setupText17.setText(String.format("%.1f", (this.setupValue17 & 0x7f) / 10.0)); //$NON-NLS-1$

				this.setupValue18 = buffer[18]; // motor battery capacity alarm level
				this.setupSlider18.setSelection(this.setupValue18);
				this.setupSlider18.setEnabled(this.setupValue18 != 0);
				this.setupText18.setText(this.setupValue18 == 0 ? GDE.STRING_EMPTY : GDE.STRING_EMPTY + this.setupValue18);
				this.setupButton18.setSelection(this.setupValue18 == 0);

				if (!this.isDataVarioTool) {
					this.setupValue19 = buffer[0]; // link vario SIO config
					this.setupSlider19.setSelection((this.setupValue19 & 0x03) * 10);
					this.setupText19.setText(GDE.STRING_EMPTY + (this.setupValue19 & 0x03));
					this.setupButton19a.setSelection((this.setupValue19 & 0x38) == 0x00); // 000000
					this.setupButton19b.setSelection((this.setupValue19 & 0x38) == 0x08); // 001000
					this.setupButton19c.setSelection((this.setupValue19 & 0x38) == 0x10); // 010000
					this.setupButton19d.setSelection((this.setupValue19 & 0x38) == 0x20); // 100000
				}
			}
			catch (Throwable e) {
				VarioToolTabItem.log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	void saveSetup() {
		FileDialog fileDialog = this.application.prepareFileSaveDialog(Messages.getString(MessageIds.GDE_MSGT1893), new String[] { GDE.FILE_ENDING_STAR_HEX, GDE.FILE_ENDING_STAR }, this.device
				.getDataBlockPreferredDataLocation(), this.getDefaultFileName());
		VarioToolTabItem.log.log(Level.FINE, "selectedSetupFile = " + fileDialog.getFileName()); //$NON-NLS-1$
		String setupFilePath = fileDialog.open();
		if (setupFilePath != null && setupFilePath.length() > 4) {
			File setupFile = new File(setupFilePath);
			byte[] buffer = new byte[20];
			try {
				if (this.isDataVarioTool) {
					buffer[0] = (byte) (this.setupValue0 & 0xFF);
				}
				else {
					buffer[0] = (byte) (this.setupValue19 & 0xFF);
				}
				buffer[1] = (byte) (this.setupValue1 & 0xFF);
				buffer[2] = (byte) (this.setupValue2 & 0xFF);
				buffer[3] = (byte) (this.setupValue3 & 0xFF);
				buffer[4] = (byte) (this.setupValue4 & 0xFF);
				buffer[5] = (byte) (this.setupValue5 & 0xFF);
				buffer[6] = (byte) (this.setupValue6 & 0xFF);
				buffer[7] = (byte) (this.setupValue7 & 0xFF);
				buffer[8] = (byte) (this.setupValue8 & 0xFF);
				buffer[9] = (byte) (this.setupValue9 & 0xFF);
				buffer[10] = (byte) (this.setupValue10 & 0xFF);
				buffer[11] = (byte) (this.setupValue11 & 0xFF);
				buffer[12] = (byte) (this.setupValue12 & 0xFF);
				buffer[13] = (byte) (this.setupValue13 & 0xFF);
				buffer[14] = (byte) (this.setupValue14 & 0xFF);
				buffer[15] = (byte) (this.setupValue15 & 0xFF);
				buffer[16] = (byte) (this.setupValue16 & 0xFF);
				buffer[17] = (byte) (this.setupValue17 & 0xFF);
				buffer[18] = (byte) (this.setupValue18 & 0xFF);
				FileOutputStream file_out = new FileOutputStream(setupFile);
				DataOutputStream data_out = new DataOutputStream(file_out);
				data_out.write(buffer);
				data_out.close();
			}
			catch (Throwable e) {
				VarioToolTabItem.log.log(Level.WARNING, "Error writing setupfile = " + fileDialog.getFileName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage()); //$NON-NLS-1$
			}
		}
	}

	String getDefaultFileName() {
		return this.isDataVarioTool ? VarioToolTabItem.DATA_VARIO_SETUPUPL_HEX : VarioToolTabItem.LINK_VARIO_LINKVARI_HEX;
	}

	/**
	 * @param tmpValue
	 * @return
	 */
	private int getSetupSlider13Value(int tmpValue) {
		if (tmpValue < 30)
			tmpValue = 2;
		else if (tmpValue > 80)
			tmpValue = 10;
		else
			tmpValue = 5;
		return tmpValue;
	}
	
	/**
	 * query the supported firmware version
	 */
	public String getFirmwareVersion() {
		return this.getText().startsWith("Link") ? "00x" : "50x"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
