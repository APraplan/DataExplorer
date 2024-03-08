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
package gde.ui.dialog.edit;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.device.DataBitsTypes;
import gde.device.DataTypes;
import gde.device.DeviceConfiguration;
import gde.device.FlowControlTypes;
import gde.device.ParityTypes;
import gde.device.StopBitsTypes;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

/**
 * class defining a CTabItem with SerialPortType configuration data
 * @author Winfried Brügmann
 */
public class SeriaPortTypeTabItem extends CTabItem {
	final static Logger						log								= Logger.getLogger(ChannelTypeTabItem.class.getName());

	Composite											serialPortComposite, timeOutComposite;
	Label													serialPortDescriptionLabel, timeOutDescriptionLabel;
	Label													portNameLabel, baudeRateLabel, dataBitsLabel, stopBitsLabel, parityLabel, flowControlLabel, rtsLabel, dtrLabel, timeOutLabel;
	Text													portNameText;
	CCombo												baudeRateCombo, dataBitsCombo, stopBitsCombo, parityCombo, flowControlCombo;
	Button												isRTSButton, isDTRButton, timeOutButton;
	Label													_RTOCharDelayTimeLabel, _RTOExtraDelayTimeLabel, _WTOCharDelayTimeLabel, _WTOExtraDelayTimeLabel;
	Text													_RTOCharDelayTimeText, _RTOExtraDelayTimeText, _WTOCharDelayTimeText, _WTOExtraDelayTimeText;

	String												portName					= GDE.STRING_EMPTY;
	int														baudeRateIndex		= 0;
	int														dataBitsIndex			= 0;
	int														stopBitsIndex			= 0;
	int														parityIndex				= 0;
	int														flowControlIndex	= 0;
	boolean												isRTS							= false;
	boolean												isDTR							= false;
	boolean												useTimeOut				= false;
	int														RTOCharDelayTime	= 0;
	int														RTOExtraDelayTime	= 0;
	int														WTOCharDelayTime	= 0;
	int														WTOExtraDelayTime	= 0;
	DeviceConfiguration						deviceConfig;
	Menu													popupMenu;
	ContextMenu										contextMenu;

	final CTabFolder							tabFolder;
	final DevicePropertiesEditor	propsEditor;

	public SeriaPortTypeTabItem(CTabFolder parent, int style, int index) {
		super(parent, style, index);
		this.tabFolder = parent;
		this.propsEditor = DevicePropertiesEditor.getInstance();
		log.log(java.util.logging.Level.FINE, "SeriaPortTypeTabItem "); //$NON-NLS-1$
		initGUI();
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText(Messages.getString(MessageIds.GDE_MSGT0510));
			this.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			{
				this.serialPortComposite = new Composite(this.tabFolder, SWT.NONE);
				this.serialPortComposite.setLayout(null);
				this.setControl(this.serialPortComposite);
				this.serialPortComposite.addHelpListener(new HelpListener() {			
					public void helpRequested(HelpEvent evt) {
						log.log(Level.FINEST, "serialPortComposite.helpRequested " + evt); //$NON-NLS-1$
						DataExplorer.getInstance().openHelpDialog("", "HelpInfo_A1.html#device_properties_serial_port"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				this.serialPortComposite.addFocusListener(new FocusAdapter() {
					@Override
					public void focusLost(FocusEvent focusevent) {
						log.log(java.util.logging.Level.FINEST, "serialPortComposite.focusLost, event=" + focusevent); //$NON-NLS-1$
						SeriaPortTypeTabItem.this.enableContextmenu(false);
					}

					@Override
					public void focusGained(FocusEvent focusevent) {
						log.log(java.util.logging.Level.FINEST, "serialPortComposite.focusGained, event=" + focusevent); //$NON-NLS-1$
						SeriaPortTypeTabItem.this.enableContextmenu(true);
					}
				});
				{
					this.serialPortDescriptionLabel = new Label(this.serialPortComposite, SWT.CENTER | SWT.WRAP);
					this.serialPortDescriptionLabel.setText(Messages.getString(MessageIds.GDE_MSGT0577));
					this.serialPortDescriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.serialPortDescriptionLabel.setBounds(12, 6, 602, 56);
				}
				{
					this.portNameLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.portNameLabel.setText(Messages.getString(MessageIds.GDE_MSGT0578));
					this.portNameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.portNameLabel.setBounds(5, 74, 100, 20);
				}
				{
					this.portNameText = new Text(this.serialPortComposite, SWT.BORDER);
					this.portNameText.setBounds(141, 76, 180, 20);
					this.portNameText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.portNameText.setEditable(false);
				}
				{
					this.baudeRateLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.baudeRateLabel.setText(Messages.getString(MessageIds.GDE_MSGT0579));
					this.baudeRateLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.baudeRateLabel.setBounds(5, 99, 100, 20);
				}
				{
					this.baudeRateCombo = new CCombo(this.serialPortComposite, SWT.BORDER);
					this.baudeRateCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.baudeRateCombo.setItems(DeviceCommPort.STRING_ARRAY_BAUDE_RATES);
					this.baudeRateCombo.setBounds(142, 101, 180, 20);
					this.baudeRateCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "baudeRateCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setBaudeRate(Integer.valueOf(SeriaPortTypeTabItem.this.baudeRateCombo.getText()));
								SeriaPortTypeTabItem.this.propsEditor.enableSaveButton(true);
							}
							SeriaPortTypeTabItem.this.baudeRateIndex = SeriaPortTypeTabItem.this.baudeRateCombo.getSelectionIndex();
						}
					});
				}
				{
					this.dataBitsLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.dataBitsLabel.setText(Messages.getString(MessageIds.GDE_MSGT0580));
					this.dataBitsLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.dataBitsLabel.setBounds(5, 124, 100, 20);
				}
				{
					this.dataBitsCombo = new CCombo(this.serialPortComposite, SWT.BORDER);
					this.dataBitsCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.dataBitsCombo.setItems(DataBitsTypes.valuesAsStingArray());
					this.dataBitsCombo.setBounds(142, 126, 180, 20);
					this.dataBitsCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBitsCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setDataBits(DataBitsTypes.fromValue(SeriaPortTypeTabItem.this.dataBitsCombo.getText()));
							}
							SeriaPortTypeTabItem.this.dataBitsIndex = SeriaPortTypeTabItem.this.dataBitsCombo.getSelectionIndex();
						}
					});
				}
				{
					this.stopBitsLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.stopBitsLabel.setText(Messages.getString(MessageIds.GDE_MSGT0581));
					this.stopBitsLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.stopBitsLabel.setBounds(5, 149, 100, 20);
				}
				{
					this.stopBitsCombo = new CCombo(this.serialPortComposite, SWT.BORDER);
					this.stopBitsCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.stopBitsCombo.setItems(StopBitsTypes.valuesAsStingArray());
					this.stopBitsCombo.setBounds(142, 151, 180, 20);
					this.stopBitsCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "stopBitsCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setStopBits(StopBitsTypes.values()[SeriaPortTypeTabItem.this.stopBitsCombo.getSelectionIndex()]);
								SeriaPortTypeTabItem.this.propsEditor.enableSaveButton(true);
							}
							SeriaPortTypeTabItem.this.stopBitsIndex = SeriaPortTypeTabItem.this.stopBitsCombo.getSelectionIndex();
						}
					});
				}
				{
					this.parityLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.parityLabel.setText(Messages.getString(MessageIds.GDE_MSGT0582));
					this.parityLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.parityLabel.setBounds(5, 174, 100, 20);
				}
				{
					this.parityCombo = new CCombo(this.serialPortComposite, SWT.BORDER);
					this.parityCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.parityCombo.setItems(ParityTypes.valuesAsStingArray());
					this.parityCombo.setBounds(142, 176, 180, 20);
					this.parityCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "parityCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setParity(ParityTypes.values()[SeriaPortTypeTabItem.this.parityCombo.getSelectionIndex()]);
								SeriaPortTypeTabItem.this.propsEditor.enableSaveButton(true);
							}
							SeriaPortTypeTabItem.this.parityIndex = SeriaPortTypeTabItem.this.parityCombo.getSelectionIndex();
						}
					});
				}
				{
					this.flowControlLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.flowControlLabel.setText(Messages.getString(MessageIds.GDE_MSGT0583));
					this.flowControlLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.flowControlLabel.setBounds(5, 199, 100, 20);
				}
				{
					this.flowControlCombo = new CCombo(this.serialPortComposite, SWT.BORDER);
					this.flowControlCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.flowControlCombo.setItems(FlowControlTypes.valuesAsStingArray());
					this.flowControlCombo.setBounds(142, 201, 180, 20);
					this.flowControlCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "flowControlCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setFlowCtrlMode(FlowControlTypes.values()[SeriaPortTypeTabItem.this.flowControlCombo.getSelectionIndex()]);
								SeriaPortTypeTabItem.this.propsEditor.enableSaveButton(true);
							}
							SeriaPortTypeTabItem.this.flowControlIndex = SeriaPortTypeTabItem.this.flowControlCombo.getSelectionIndex();
						}
					});
				}
				{
					this.rtsLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.rtsLabel.setText(Messages.getString(MessageIds.GDE_MSGT0584));
					this.rtsLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.rtsLabel.setBounds(5, 224, 100, 20);
				}
				{
					this.isRTSButton = new Button(this.serialPortComposite, SWT.CHECK);
					this.isRTSButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.isRTSButton.setBounds(142, 224, 180, 20);
					this.isRTSButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "isRTSButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setIsRTS(SeriaPortTypeTabItem.this.isRTSButton.getSelection());
								SeriaPortTypeTabItem.this.propsEditor.enableSaveButton(true);
							}
							SeriaPortTypeTabItem.this.isRTS = SeriaPortTypeTabItem.this.isRTSButton.getSelection();
						}
					});
				}
				{
					this.dtrLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.dtrLabel.setText(Messages.getString(MessageIds.GDE_MSGT0585));
					this.dtrLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.dtrLabel.setBounds(5, 249, 100, 20);
				}
				{
					this.isDTRButton = new Button(this.serialPortComposite, SWT.CHECK);
					this.isDTRButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.isDTRButton.setBounds(142, 249, 180, 20);
					this.isDTRButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "isDTRButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setIsDTR(SeriaPortTypeTabItem.this.isDTRButton.getSelection());
								SeriaPortTypeTabItem.this.propsEditor.enableSaveButton(true);
							}
							SeriaPortTypeTabItem.this.isDTR = SeriaPortTypeTabItem.this.isDTRButton.getSelection();
						}
					});
				}
				{
					this.timeOutComposite = new Composite(this.serialPortComposite, SWT.BORDER);
					this.timeOutComposite.setLayout(null);
					this.timeOutComposite.setBounds(355, 80, 250, 220);
					{
						this.timeOutDescriptionLabel = new Label(this.timeOutComposite, SWT.WRAP);
						this.timeOutDescriptionLabel.setText(Messages.getString(MessageIds.GDE_MSGT0591));
						this.timeOutDescriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.timeOutDescriptionLabel.setBounds(6, 3, 232, 69);
					}
					{
						this.timeOutLabel = new Label(this.timeOutComposite, SWT.RIGHT);
						this.timeOutLabel.setText(Messages.getString(MessageIds.GDE_MSGT0586));
						this.timeOutLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.timeOutLabel.setBounds(6, 70, 140, 20);
					}
					{
						this.timeOutButton = new Button(this.timeOutComposite, SWT.CHECK);
						this.timeOutButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.timeOutButton.setBounds(161, 70, 70, 20);
						this.timeOutButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "timeOutButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								SeriaPortTypeTabItem.this.useTimeOut = SeriaPortTypeTabItem.this.timeOutButton.getSelection();
								if (SeriaPortTypeTabItem.this.useTimeOut) {
									if (SeriaPortTypeTabItem.this.deviceConfig != null) {
										SeriaPortTypeTabItem.this.deviceConfig.setReadTimeOut(SeriaPortTypeTabItem.this.RTOCharDelayTime = SeriaPortTypeTabItem.this.deviceConfig.getReadTimeOut());
										SeriaPortTypeTabItem.this.deviceConfig.setReadStableIndex(SeriaPortTypeTabItem.this.RTOExtraDelayTime = SeriaPortTypeTabItem.this.deviceConfig.getReadStableIndex());
										SeriaPortTypeTabItem.this.deviceConfig.setWriteCharDelayTime(SeriaPortTypeTabItem.this.WTOCharDelayTime = SeriaPortTypeTabItem.this.deviceConfig.getWriteCharDelayTime());
										SeriaPortTypeTabItem.this.deviceConfig.setWriteDelayTime(SeriaPortTypeTabItem.this.WTOExtraDelayTime = SeriaPortTypeTabItem.this.deviceConfig.getWriteDelayTime());
										SeriaPortTypeTabItem.this.propsEditor.enableSaveButton(true);
									}
									else {
										SeriaPortTypeTabItem.this.RTOCharDelayTime = 0;
										SeriaPortTypeTabItem.this.RTOExtraDelayTime = 0;
										SeriaPortTypeTabItem.this.WTOCharDelayTime = 0;
										SeriaPortTypeTabItem.this.WTOExtraDelayTime = 0;
									}
								}
								else {
									if (SeriaPortTypeTabItem.this.deviceConfig != null) {
										SeriaPortTypeTabItem.this.deviceConfig.removeSerialPortTimeOut();
									}
									SeriaPortTypeTabItem.this.RTOCharDelayTime = 0;
									SeriaPortTypeTabItem.this.RTOExtraDelayTime = 0;
									SeriaPortTypeTabItem.this.WTOCharDelayTime = 0;
									SeriaPortTypeTabItem.this.WTOExtraDelayTime = 0;
								}
								SeriaPortTypeTabItem.this.enableTimeout();
							}
						});
					}
					{
						this._RTOCharDelayTimeLabel = new Label(this.timeOutComposite, SWT.RIGHT);
						this._RTOCharDelayTimeLabel.setText(Messages.getString(MessageIds.GDE_MSGT0587));
						this._RTOCharDelayTimeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this._RTOCharDelayTimeLabel.setBounds(6, 100, 140, 20);
					}
					{
						this._RTOCharDelayTimeText = new Text(this.timeOutComposite, SWT.BORDER);
						this._RTOCharDelayTimeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this._RTOCharDelayTimeText.setBounds(162, 100, 70, 20);
						this._RTOCharDelayTimeText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "_RTOCharDelayTimeText.verifyText, event=" + evt); //$NON-NLS-1$
								evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
							}
						});
						this._RTOCharDelayTimeText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "_RTOCharDelayTimeText.keyReleased, event=" + evt); //$NON-NLS-1$
								SeriaPortTypeTabItem.this.RTOCharDelayTime = Integer.parseInt(SeriaPortTypeTabItem.this._RTOCharDelayTimeText.getText());
								if (SeriaPortTypeTabItem.this.deviceConfig != null) {
									SeriaPortTypeTabItem.this.deviceConfig.setReadTimeOut(SeriaPortTypeTabItem.this.RTOCharDelayTime);
									SeriaPortTypeTabItem.this.propsEditor.enableSaveButton(true);
								}
							}
						});
					}
					{
						this._RTOExtraDelayTimeLabel = new Label(this.timeOutComposite, SWT.RIGHT);
						this._RTOExtraDelayTimeLabel.setText(Messages.getString(MessageIds.GDE_MSGT0588));
						this._RTOExtraDelayTimeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this._RTOExtraDelayTimeLabel.setBounds(6, 130, 140, 20);
					}
					{
						this._RTOExtraDelayTimeText = new Text(this.timeOutComposite, SWT.BORDER);
						this._RTOExtraDelayTimeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this._RTOExtraDelayTimeText.setBounds(162, 130, 70, 20);
						this._RTOExtraDelayTimeText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "_RTOExtraDelayTimeText.verifyText, event=" + evt); //$NON-NLS-1$
								evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
							}
						});
						this._RTOExtraDelayTimeText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "_RTOExtraDelayTimeText.keyReleased, event=" + evt); //$NON-NLS-1$
								SeriaPortTypeTabItem.this.RTOExtraDelayTime = Integer.parseInt(SeriaPortTypeTabItem.this._RTOExtraDelayTimeText.getText());
								if (SeriaPortTypeTabItem.this.deviceConfig != null) {
									SeriaPortTypeTabItem.this.deviceConfig.setReadStableIndex(SeriaPortTypeTabItem.this.RTOExtraDelayTime);
									SeriaPortTypeTabItem.this.propsEditor.enableSaveButton(true);
								}
							}
						});
					}
					{
						this._WTOCharDelayTimeLabel = new Label(this.timeOutComposite, SWT.RIGHT);
						this._WTOCharDelayTimeLabel.setText(Messages.getString(MessageIds.GDE_MSGT0589));
						this._WTOCharDelayTimeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this._WTOCharDelayTimeLabel.setBounds(6, 160, 140, 20);
					}
					{
						this._WTOCharDelayTimeText = new Text(this.timeOutComposite, SWT.BORDER);
						this._WTOCharDelayTimeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this._WTOCharDelayTimeText.setBounds(162, 160, 70, 20);
						this._WTOCharDelayTimeText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "_WRTOCharDelayTimeText.verifyText, event=" + evt); //$NON-NLS-1$
								evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
							}
						});
						this._WTOCharDelayTimeText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "_WRTOCharDelayTimeText.keyReleased, event=" + evt); //$NON-NLS-1$
								SeriaPortTypeTabItem.this.WTOCharDelayTime = Integer.parseInt(SeriaPortTypeTabItem.this._WTOCharDelayTimeText.getText());
								if (SeriaPortTypeTabItem.this.deviceConfig != null) {
									SeriaPortTypeTabItem.this.deviceConfig.setWriteCharDelayTime(SeriaPortTypeTabItem.this.WTOCharDelayTime);
									SeriaPortTypeTabItem.this.propsEditor.enableSaveButton(true);
								}
							}
						});
					}
					{
						this._WTOExtraDelayTimeLabel = new Label(this.timeOutComposite, SWT.RIGHT);
						this._WTOExtraDelayTimeLabel.setText(Messages.getString(MessageIds.GDE_MSGT0590));
						this._WTOExtraDelayTimeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this._WTOExtraDelayTimeLabel.setBounds(6, 190, 140, 20);
					}
					{
						this._WTOExtraDelayTimeText = new Text(this.timeOutComposite, SWT.BORDER);
						this._WTOExtraDelayTimeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this._WTOExtraDelayTimeText.setBounds(162, 190, 70, 20);
						this._WTOExtraDelayTimeText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "_WTOExtraDelayTimeText.verifyText, event=" + evt); //$NON-NLS-1$
								evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
							}
						});
						this._WTOExtraDelayTimeText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "_WTOExtraDelayTimeText.keyReleased, event=" + evt); //$NON-NLS-1$
								SeriaPortTypeTabItem.this.WTOExtraDelayTime = Integer.parseInt(SeriaPortTypeTabItem.this._WTOExtraDelayTimeText.getText());
								if (SeriaPortTypeTabItem.this.deviceConfig != null) {
									SeriaPortTypeTabItem.this.deviceConfig.setWriteDelayTime(SeriaPortTypeTabItem.this.WTOExtraDelayTime);
									SeriaPortTypeTabItem.this.propsEditor.enableSaveButton(true);
								}
							}
						});
					}
				}
			}
			initialize();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	void enableContextmenu(boolean enable) {
		if (enable && (this.popupMenu == null || this.contextMenu == null)) {
			this.popupMenu = new Menu(this.tabFolder.getShell(), SWT.POP_UP);
			//this.popupMenu = SWTResourceManager.getMenu("Contextmenu", this.tabFolder.getShell(), SWT.POP_UP);
			this.contextMenu = new ContextMenu(this.popupMenu, this.tabFolder);
			this.contextMenu.create();
		}
		else {
			this.popupMenu = null;
			this.contextMenu = null;
		}
		this.serialPortComposite.setMenu(this.popupMenu);
		this.serialPortDescriptionLabel.setMenu(this.popupMenu);
		this.portNameLabel.setMenu(this.popupMenu);
		this.baudeRateLabel.setMenu(this.popupMenu);
		this.dataBitsLabel.setMenu(this.popupMenu);
		this.stopBitsLabel.setMenu(this.popupMenu);
		this.parityLabel.setMenu(this.popupMenu);
		this.flowControlLabel.setMenu(this.popupMenu);
		this.rtsLabel.setMenu(this.popupMenu);
		this.isRTSButton.setMenu(this.popupMenu);
		this.dtrLabel.setMenu(this.popupMenu);
		this.isDTRButton.setMenu(this.popupMenu);
		this.timeOutComposite.setMenu(this.popupMenu);
		this.timeOutLabel.setMenu(this.popupMenu);
		this.timeOutButton.setMenu(this.popupMenu);
		this._RTOCharDelayTimeLabel.setMenu(this.popupMenu);
		this._RTOExtraDelayTimeLabel.setMenu(this.popupMenu);
		this._WTOCharDelayTimeLabel.setMenu(this.popupMenu);
		this._WTOExtraDelayTimeLabel.setMenu(this.popupMenu);
		this.timeOutDescriptionLabel.setMenu(this.popupMenu);
	}

	/**
	 * @param deviceConfig the deviceConfig to set
	 */
	public void setDeviceConfig(DeviceConfiguration deviceConfig) {
		this.deviceConfig = deviceConfig;

		//String tmpPortString = GDE.IS_WINDOWS ? "COM1" : GDE.IS_LINUX ? "/dev/ttyS0" : GDE.IS_MAC ? "/dev/tty.usbserial" : "COMx";
		//deviceConfig.setPort(tmpPortString);
		this.portName = deviceConfig.getPort();
		this.baudeRateIndex = getSelectionIndex(this.baudeRateCombo, GDE.STRING_EMPTY + deviceConfig.getBaudeRate());
		this.dataBitsIndex = deviceConfig.getDataBits().ordinal();
		this.stopBitsIndex = deviceConfig.getStopBits().ordinal();
		this.parityIndex = deviceConfig.getParity().ordinal();
		this.flowControlIndex = deviceConfig.getFlowCtrlModeOrdinal();
		this.isRTS = deviceConfig.isRTS();
		this.isDTR = deviceConfig.isDTR();

		if (deviceConfig.getSerialPortType().getTimeOut() != null) {
			this.timeOutButton.setSelection(this.useTimeOut = true);
		}
		else {
			this.timeOutButton.setSelection(this.useTimeOut = false);
		}
		this.RTOCharDelayTime = deviceConfig.getReadTimeOut();
		this.RTOExtraDelayTime = deviceConfig.getReadStableIndex();
		this.WTOCharDelayTime = deviceConfig.getWriteCharDelayTime();
		this.WTOExtraDelayTime = deviceConfig.getWriteDelayTime();
		this.timeOutComposite.redraw();

		initialize();
	}

	/**
	 * search the index of a given string within the items of a combo box items
	 * @param useCombo
	 * @param searchString
	 * @return
	 */
	private int getSelectionIndex(CCombo useCombo, String searchString) {
		int searchIndex = 0;
		for (String item : useCombo.getItems()) {
			if (item.equals(searchString)) break;
			++searchIndex;
		}
		return searchIndex;
	}

	/**
	 * initialize widget states
	 */
	private void initialize() {
		SeriaPortTypeTabItem.this.portNameText.setText(SeriaPortTypeTabItem.this.portName);
		SeriaPortTypeTabItem.this.baudeRateCombo.select(SeriaPortTypeTabItem.this.baudeRateIndex);
		SeriaPortTypeTabItem.this.dataBitsCombo.select(SeriaPortTypeTabItem.this.dataBitsIndex);
		SeriaPortTypeTabItem.this.stopBitsCombo.select(SeriaPortTypeTabItem.this.stopBitsIndex);
		SeriaPortTypeTabItem.this.parityCombo.select(SeriaPortTypeTabItem.this.parityIndex);
		SeriaPortTypeTabItem.this.flowControlCombo.select(SeriaPortTypeTabItem.this.flowControlIndex);
		SeriaPortTypeTabItem.this.isRTSButton.setSelection(SeriaPortTypeTabItem.this.isRTS);
		SeriaPortTypeTabItem.this.isDTRButton.setSelection(SeriaPortTypeTabItem.this.isDTR);
		
		SeriaPortTypeTabItem.this._RTOCharDelayTimeText.setText(GDE.STRING_EMPTY + SeriaPortTypeTabItem.this.RTOCharDelayTime);
		SeriaPortTypeTabItem.this._RTOExtraDelayTimeText.setText(GDE.STRING_EMPTY + SeriaPortTypeTabItem.this.RTOExtraDelayTime);
		SeriaPortTypeTabItem.this._WTOCharDelayTimeText.setText(GDE.STRING_EMPTY + SeriaPortTypeTabItem.this.WTOCharDelayTime);
		SeriaPortTypeTabItem.this._WTOExtraDelayTimeText.setText(GDE.STRING_EMPTY + SeriaPortTypeTabItem.this.WTOExtraDelayTime);

		SeriaPortTypeTabItem.this.timeOutButton.setSelection(SeriaPortTypeTabItem.this.useTimeOut);
		enableTimeout();
	}

	private void enableTimeout() {
		if (SeriaPortTypeTabItem.this.timeOutButton.getSelection()) {
			SeriaPortTypeTabItem.this._RTOCharDelayTimeLabel.setEnabled(true);
			SeriaPortTypeTabItem.this._RTOCharDelayTimeText.setEnabled(true);
			SeriaPortTypeTabItem.this._RTOExtraDelayTimeLabel.setEnabled(true);
			SeriaPortTypeTabItem.this._RTOExtraDelayTimeText.setEnabled(true);
			SeriaPortTypeTabItem.this._WTOCharDelayTimeLabel.setEnabled(true);
			SeriaPortTypeTabItem.this._WTOCharDelayTimeText.setEnabled(true);
			SeriaPortTypeTabItem.this._WTOExtraDelayTimeLabel.setEnabled(true);
			SeriaPortTypeTabItem.this._WTOExtraDelayTimeText.setEnabled(true);
		}
		else {
			SeriaPortTypeTabItem.this._RTOCharDelayTimeLabel.setEnabled(false);
			SeriaPortTypeTabItem.this._RTOCharDelayTimeText.setEnabled(false);
			SeriaPortTypeTabItem.this._RTOExtraDelayTimeLabel.setEnabled(false);
			SeriaPortTypeTabItem.this._RTOExtraDelayTimeText.setEnabled(false);
			SeriaPortTypeTabItem.this._WTOCharDelayTimeLabel.setEnabled(false);
			SeriaPortTypeTabItem.this._WTOCharDelayTimeText.setEnabled(false);
			SeriaPortTypeTabItem.this._WTOExtraDelayTimeLabel.setEnabled(false);
			SeriaPortTypeTabItem.this._WTOExtraDelayTimeText.setEnabled(false);
		}
	}
}
