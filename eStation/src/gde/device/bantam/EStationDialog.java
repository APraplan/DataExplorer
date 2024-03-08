/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.bantam;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.DeviceDialog;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

import java.util.HashMap;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * e-Station dialog implementation (902, BC6, BC610, BC8)
 * @author Winfried Brügmann
 */
public class EStationDialog extends DeviceDialog {
	final static Logger						log									= Logger.getLogger(EStationDialog.class.getName());
	static final String						DEVICE_NAME					= "eStation";

	CLabel												infoText;
	Button												closeButton;
	Button												stopCollectDataButton;
	Button												startCollectDataButton;

	Composite											boundsComposite;
	Group													configGroup;
	Composite											composite1;
	Composite											composite2;
	Composite											composite3;

	CLabel												inputPowerLowCutOffLabel;
	CLabel												capacityCutOffLabel;
	CLabel												safetyTimerLabel;
	CLabel												tempCutOffLabel;
	CLabel												waitTimeLabel;
	CLabel												cellTypeLabel;

	CLabel												inputLowPowerCutOffText;
	CLabel												capacityCutOffText;
	CLabel												safetyTimerText;
	CLabel												tempCutOffText;
	CLabel												waitTimeText;
	CLabel												cellTypeText;

	CLabel												inputLowPowerCutOffUnit;
	CLabel												capacityCutOffUnit;
	CLabel												safetyTimerUnit;
	CLabel												tempCutOffUnit;
	CLabel												waitTimeUnit;
	CLabel												cellTypeUnit;

	boolean												isConnectionWarned 	= false;
	String												inputLowPowerCutOff	= "?";				//$NON-NLS-1$
	String												capacityCutOff			= "?";				//$NON-NLS-1$
	String												safetyTimer					= "?";				//$NON-NLS-1$
	String												tempCutOff					= "?";				//$NON-NLS-1$
	String												waitTime						= "?";				//$NON-NLS-1$
	String												cellType						= "?";				//$NON-NLS-1$

	HashMap<String, String>				configData					= new HashMap<String, String>();
	GathererThread								dataGatherThread;
	Thread												updateConfigTread;

	final eStation								device;						// get device specific things, get serial port, ...
	final EStationSerialPort			serialPort;				// open/close port execute getData()....
	final Channels								channels;					// interaction with channels, source of all records
	final Settings								settings;					// application configuration settings

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public EStationDialog(Shell parent, eStation useDevice) {
		super(parent);
		this.serialPort = useDevice.getCommunicationPort();
		this.device = useDevice;
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	@Override
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue(); 
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			EStationDialog.log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);
				this.dialogShell.setLayout(new FormLayout());
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.layout();
				this.dialogShell.pack();
				this.dialogShell.setSize(350, 465);
				this.dialogShell.addFocusListener(new FocusAdapter() {
					@Override
					public void focusGained(FocusEvent evt) {
						EStationDialog.log.log(Level.FINER, "dialogShell.focusGained, event=" + evt); //$NON-NLS-1$
						// if port is already connected, do not read the data update will be done by gathere thread
						if (!EStationDialog.this.isConnectionWarned && !EStationDialog.this.serialPort.isConnected()) {
							EStationDialog.this.updateConfigTread = new Thread("updateConfig") {
								@Override
								public void run() {
									try {
										EStationDialog.this.configData = new HashMap<String, String>();
										EStationDialog.this.serialPort.open();
										EStationDialog.this.serialPort.wait4Bytes(2000);
										EStationDialog.this.device.getConfigurationValues(EStationDialog.this.configData, EStationDialog.this.serialPort.getData());
										getDialogShell().getDisplay().asyncExec(new Runnable() {
											public void run() {
												updateGlobalConfigData(EStationDialog.this.configData);
											}
										});
									}
									catch (Exception e) {
										EStationDialog.this.isConnectionWarned = true;
										EStationDialog.this.application.openMessageDialog(EStationDialog.this.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0024, new Object[] { e.getMessage() } ));
									}
									EStationDialog.this.serialPort.close();
								}
							};
							try {
								EStationDialog.this.updateConfigTread.start();
							}
							catch (RuntimeException e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				});
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
		      public void handleEvent(Event event) {
		        switch (event.detail) {
		        case SWT.TRAVERSE_ESCAPE:
		        	EStationDialog.this.dialogShell.close();
		          event.detail = SWT.TRAVERSE_NONE;
		          event.doit = false;
		          break;
		        }
		      }
		    });
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						EStationDialog.log.log(Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						EStationDialog.this.application.openHelpDialog(DEVICE_NAME, "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						EStationDialog.this.dispose();
					}
				});
				{
					this.boundsComposite = new Composite(this.dialogShell, SWT.NONE);
					FormData boundsCompositeLData = new FormData();
					boundsCompositeLData.left = new FormAttachment(0, 1000, 0);
					boundsCompositeLData.right = new FormAttachment(1000, 1000, 0);
					boundsCompositeLData.top = new FormAttachment(0, 1000, 0);
					boundsCompositeLData.bottom = new FormAttachment(1000, 1000, 0);
					this.boundsComposite.setLayoutData(boundsCompositeLData);
					this.boundsComposite.setLayout(new FormLayout());
					this.boundsComposite.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							EStationDialog.log.log(Level.FINER, "boundsComposite.paintControl() " + evt); //$NON-NLS-1$
							if (EStationDialog.this.dataGatherThread != null && EStationDialog.this.dataGatherThread.isAlive()) {
								EStationDialog.this.startCollectDataButton.setEnabled(false);
								EStationDialog.this.stopCollectDataButton.setEnabled(true);
							}
							else {
								EStationDialog.this.startCollectDataButton.setEnabled(true);
								EStationDialog.this.stopCollectDataButton.setEnabled(false);
							}
						}
					});
					{
						FormData infoTextLData = new FormData();
						infoTextLData.height = 80;
						infoTextLData.left = new FormAttachment(0, 1000, 12);
						infoTextLData.top = new FormAttachment(0, 1000, 20);
						infoTextLData.right = new FormAttachment(1000, 1000, -12);
						this.infoText = new CLabel(this.boundsComposite, SWT.SHADOW_IN | SWT.CENTER | SWT.EMBEDDED);
						this.infoText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.infoText.setLayoutData(infoTextLData);
						this.infoText.setText(Messages.getString(MessageIds.GDE_MSGT1410));
						this.infoText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
						this.infoText.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
					}
					{
						FormData startCollectDataButtonLData = new FormData();
						startCollectDataButtonLData.height = 30;
						startCollectDataButtonLData.left = new FormAttachment(0, 1000, 12);
						startCollectDataButtonLData.top = new FormAttachment(0, 1000, 110);
						startCollectDataButtonLData.right = new FormAttachment(1000, 1000, -180);
						this.startCollectDataButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						this.startCollectDataButton.setLayoutData(startCollectDataButtonLData);
						this.startCollectDataButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0274));
						this.startCollectDataButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								EStationDialog.log.log(Level.FINEST, "startCollectDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (!EStationDialog.this.serialPort.isConnected()) {
									try {
										Channel activChannel = Channels.getInstance().getActiveChannel();
										if (activChannel != null) {
											EStationDialog.this.dataGatherThread = new GathererThread(EStationDialog.this.application, EStationDialog.this.device, EStationDialog.this.serialPort, activChannel.getNumber(), EStationDialog.this);
											try {
												EStationDialog.this.dataGatherThread.start();
											}
											catch (RuntimeException e) {
												log.log(Level.WARNING, e.getMessage(), e);
											}
											EStationDialog.this.boundsComposite.redraw();
										}
									}
									catch (Exception e) {
										if (EStationDialog.this.dataGatherThread != null && EStationDialog.this.dataGatherThread.isCollectDataStopped) {
											EStationDialog.this.dataGatherThread.stopDataGatheringThread(false, e);
										}
										EStationDialog.this.boundsComposite.redraw();
										EStationDialog.this.application.updateGraphicsWindow();
										EStationDialog.this.application.openMessageDialog(EStationDialog.this.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0023, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
									}
								}
							}
						});
						this.startCollectDataButton.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
					}
					{
						FormData stopColletDataButtonLData = new FormData();
						stopColletDataButtonLData.height = 30;
						stopColletDataButtonLData.left = new FormAttachment(0, 1000, 170);
						stopColletDataButtonLData.top = new FormAttachment(0, 1000, 110);
						stopColletDataButtonLData.right = new FormAttachment(1000, 1000, -12);
						this.stopCollectDataButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						this.stopCollectDataButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.stopCollectDataButton.setLayoutData(stopColletDataButtonLData);
						this.stopCollectDataButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0275));
						this.stopCollectDataButton.setEnabled(false);
						this.stopCollectDataButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								EStationDialog.log.log(Level.FINEST, "stopColletDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (EStationDialog.this.dataGatherThread != null && EStationDialog.this.serialPort.isConnected()) {
									EStationDialog.this.dataGatherThread.stopDataGatheringThread(false, null);
								}
								EStationDialog.this.boundsComposite.redraw();
							}
						});
						this.stopCollectDataButton.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
					}
					{
						FormData configGroupLData = new FormData();
						configGroupLData.height = 200;
						configGroupLData.left = new FormAttachment(0, 1000, 12);
						configGroupLData.top = new FormAttachment(0, 1000, 155);
						configGroupLData.right = new FormAttachment(1000, 1000, -12);
						this.configGroup = new Group(this.boundsComposite, SWT.NONE);
						RowLayout configGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.configGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.configGroup.setLayout(configGroupLayout);
						this.configGroup.setLayoutData(configGroupLData);
						this.configGroup.setText(Messages.getString(MessageIds.GDE_MSGT1407));
						this.configGroup.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								EStationDialog.log.log(Level.FINEST, "configGroup.paintControl, event=" + evt); //$NON-NLS-1$
								EStationDialog.this.inputLowPowerCutOffText.setText(EStationDialog.this.inputLowPowerCutOff);
								EStationDialog.this.capacityCutOffText.setText(EStationDialog.this.capacityCutOff);
								EStationDialog.this.safetyTimerText.setText(EStationDialog.this.safetyTimer);
								EStationDialog.this.tempCutOffText.setText(EStationDialog.this.tempCutOff);
								EStationDialog.this.waitTimeText.setText(EStationDialog.this.waitTime);
								EStationDialog.this.cellTypeText.setText(EStationDialog.this.cellType);
							}
						});
						{
							RowData composite1LData = new RowData();
							composite1LData.width = 190;
							composite1LData.height = 195;
							this.composite1 = new Composite(this.configGroup, SWT.NONE);
							FillLayout composite1Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
							this.composite1.setLayout(composite1Layout);
							this.composite1.setLayoutData(composite1LData);
							{
								this.inputPowerLowCutOffLabel = new CLabel(this.composite1, SWT.NONE);
								this.inputPowerLowCutOffLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.inputPowerLowCutOffLabel.setText(Messages.getString(MessageIds.GDE_MSGT1414));
							}
							{
								this.capacityCutOffLabel = new CLabel(this.composite1, SWT.NONE);
								this.capacityCutOffLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.capacityCutOffLabel.setText(Messages.getString(MessageIds.GDE_MSGT1415));
							}
							{
								this.safetyTimerLabel = new CLabel(this.composite1, SWT.NONE);
								this.safetyTimerLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.safetyTimerLabel.setText(Messages.getString(MessageIds.GDE_MSGT1416));
							}
							{
								this.tempCutOffLabel = new CLabel(this.composite1, SWT.NONE);
								this.tempCutOffLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.tempCutOffLabel.setText(Messages.getString(MessageIds.GDE_MSGT1417));
							}
							{
								this.waitTimeLabel = new CLabel(this.composite1, SWT.NONE);
								this.waitTimeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.waitTimeLabel.setText(Messages.getString(MessageIds.GDE_MSGT1418));
							}
							{
								this.cellTypeLabel = new CLabel(this.composite1, SWT.NONE);
								this.cellTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.cellTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT1420));
							}
						}
						{
							RowData composite2LData = new RowData();
							composite2LData.width = 45;
							composite2LData.height = 195;
							this.composite2 = new Composite(this.configGroup, SWT.NONE);
							FillLayout composite2Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
							this.composite2.setLayout(composite2Layout);
							this.composite2.setLayoutData(composite2LData);
							{
								this.inputLowPowerCutOffText = new CLabel(this.composite2, SWT.NONE);
								this.inputLowPowerCutOffText.setText(this.inputLowPowerCutOff);
							}
							{
								this.capacityCutOffText = new CLabel(this.composite2, SWT.NONE);
								this.capacityCutOffText.setText(this.capacityCutOff);
							}
							{
								this.safetyTimerText = new CLabel(this.composite2, SWT.NONE);
								this.safetyTimerText.setText(this.safetyTimer);
							}
							{
								this.tempCutOffText = new CLabel(this.composite2, SWT.NONE);
								this.tempCutOffText.setText(this.tempCutOff);
							}
							{
								this.waitTimeText = new CLabel(this.composite2, SWT.NONE);
								this.waitTimeText.setText(this.waitTime);
							}
							{
								this.cellTypeText = new CLabel(this.composite2, SWT.NONE);
								this.cellTypeText.setText(this.cellType);
							}
						}
						{
							this.composite3 = new Composite(this.configGroup, SWT.NONE);
							FillLayout composite3Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
							RowData composite3LData = new RowData();
							composite3LData.width = 60;
							composite3LData.height = 195;
							this.composite3.setLayoutData(composite3LData);
							this.composite3.setLayout(composite3Layout);
							{
								this.inputLowPowerCutOffUnit = new CLabel(this.composite3, SWT.NONE);
								this.inputLowPowerCutOffUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.inputLowPowerCutOffUnit.setText(Messages.getString(MessageIds.GDE_MSGT1421));
							}
							{
								this.capacityCutOffUnit = new CLabel(this.composite3, SWT.NONE);
								this.capacityCutOffUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.capacityCutOffUnit.setText(Messages.getString(MessageIds.GDE_MSGT1422));
							}
							{
								this.safetyTimerUnit = new CLabel(this.composite3, SWT.NONE);
								this.safetyTimerUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.safetyTimerUnit.setText(Messages.getString(MessageIds.GDE_MSGT1423));
							}
							{
								this.tempCutOffUnit = new CLabel(this.composite3, SWT.NONE);
								this.tempCutOffUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.tempCutOffUnit.setText(Messages.getString(MessageIds.GDE_MSGT1424));
							}
							{
								this.waitTimeUnit = new CLabel(this.composite3, SWT.NONE);
								this.waitTimeUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.waitTimeUnit.setText(Messages.getString(MessageIds.GDE_MSGT1425));
							}
							{
								this.cellTypeUnit = new CLabel(this.composite3, SWT.NONE);
								this.cellTypeUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.cellTypeUnit.setText(""); //$NON-NLS-1$
							}
						}
						this.configGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
					}
					{
						FormData closeButtonLData = new FormData();
						closeButtonLData.height = 30;
						closeButtonLData.bottom = new FormAttachment(1000, 1000, -12);
						closeButtonLData.left = new FormAttachment(0, 1000, 12);
						closeButtonLData.right = new FormAttachment(1000, 1000, -12);
						this.closeButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.closeButton.setLayoutData(closeButtonLData);
						this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0188));
						this.closeButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								EStationDialog.log.log(Level.FINEST, "okButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								close();
							}
						});
						this.closeButton.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
					}
					this.boundsComposite.addMouseTrackListener(new MouseTrackAdapter() {
						@Override
						public void mouseEnter(MouseEvent evt) {
							EStationDialog.log.log(Level.FINE, "boundsComposite.mouseEnter, event=" + evt); //$NON-NLS-1$
							fadeOutAplhaBlending(evt, EStationDialog.this.boundsComposite.getSize(), 10, 10, 10, 15);
						}

						@Override
						public void mouseHover(MouseEvent evt) {
							EStationDialog.log.log(Level.FINEST, "boundsComposite.mouseHover, event=" + evt); //$NON-NLS-1$
						}

						@Override
						public void mouseExit(MouseEvent evt) {
							EStationDialog.log.log(Level.FINE, "boundsComposite.mouseExit, event=" + evt); //$NON-NLS-1$
							fadeInAlpaBlending(evt, EStationDialog.this.boundsComposite.getSize(), 10, 10, -10, 15);
						}
					});
				} // end boundsComposite
				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 175, 100));
				this.dialogShell.open();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			EStationDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public void resetButtons() {
		if (!this.isDisposed()) {
			this.startCollectDataButton.setEnabled(true);
			this.stopCollectDataButton.setEnabled(false);
		}
	}

	/**
	 * update the global conguration data in dialog
	 */
	public void updateGlobalConfigData(HashMap<String, String> newConfigData) {
		this.configData = newConfigData;
		if (this.dialogShell != null && !this.dialogShell.isDisposed()) {
			if (Thread.currentThread().getId() == this.application.getThreadId()) {
				this.inputLowPowerCutOffText.setText(this.inputLowPowerCutOff = this.configData.get(eStation.CONFIG_IN_VOLTAGE_CUT_OFF)); //$NON-NLS-1$
				this.capacityCutOffText.setText(this.capacityCutOff = this.configData.get(eStation.CONFIG_SET_CAPASITY)); //$NON-NLS-1$
				this.safetyTimerText.setText(this.safetyTimer = this.configData.get(eStation.CONFIG_SAFETY_TIME)); //$NON-NLS-1$
				this.tempCutOffText.setText(this.tempCutOff = this.configData.get(eStation.CONFIG_EXT_TEMP_CUT_OFF)); //$NON-NLS-1$
				this.waitTimeText.setText(this.waitTime = this.configData.get(eStation.CONFIG_WAIT_TIME)); //$NON-NLS-1$
				if (this.configData.get(eStation.CONFIG_BATTERY_TYPE) != null)
					this.cellTypeText.setText(this.cellType = this.configData.get(eStation.CONFIG_BATTERY_TYPE));
				this.configGroup.redraw();
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						EStationDialog.this.inputLowPowerCutOffText.setText(EStationDialog.this.inputLowPowerCutOff = EStationDialog.this.configData.get(eStation.CONFIG_IN_VOLTAGE_CUT_OFF));
						EStationDialog.this.capacityCutOffText.setText(EStationDialog.this.capacityCutOff = EStationDialog.this.configData.get(eStation.CONFIG_SET_CAPASITY));
						EStationDialog.this.safetyTimerText.setText(EStationDialog.this.safetyTimer = EStationDialog.this.configData.get(eStation.CONFIG_SAFETY_TIME));
						EStationDialog.this.tempCutOffText.setText(EStationDialog.this.tempCutOff = EStationDialog.this.configData.get(eStation.CONFIG_EXT_TEMP_CUT_OFF));
						EStationDialog.this.waitTimeText.setText(EStationDialog.this.waitTime = EStationDialog.this.configData.get(eStation.CONFIG_WAIT_TIME));
						if (EStationDialog.this.configData.get(eStation.CONFIG_BATTERY_TYPE) != null)
							EStationDialog.this.cellTypeText.setText(EStationDialog.this.cellType = EStationDialog.this.configData.get(eStation.CONFIG_BATTERY_TYPE));
						EStationDialog.this.configGroup.redraw();
					}
				});
			}
		}
	}
}
