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
package gde.device.conrad;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.DeviceDialog;
import gde.exception.ApplicationConfigurationException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

import java.io.IOException;
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
import org.eclipse.swt.widgets.Text;

/**
 * e-Station dialog implementation (902, BC6, BC610, BC8)
 * @author Winfried Brügmann
 */
public class VC800Dialog extends DeviceDialog {
	final static Logger						log									= Logger.getLogger(VC800Dialog.class.getName());
	final static String						DEVICE_NAME					= "VC800"; //$NON-NLS-1$

	Text													infoText;
	Button												closeButton;
	Button												stopCollectDataButton;
	Button												startCollectDataButton;

	Composite											boundsComposite;
	Group													configGroup;
	Composite											labelComposite;
	Composite											composite2;
	Composite											dataComposite;

	CLabel												inputTypeLabel;
	CLabel												inputTypeUnit;
	CLabel												batteryLabel;
	CLabel												batteryCondition;
	boolean												isBatteryOK = true;

	boolean												isConnectionWarned 	= false;
	boolean 											isPortOpenedByMe 		= false;
	Thread												updateConfigTread;

	GathererThread								dataGatherThread;

	final VC800										device;						// get device specific things, get serial port, ...
	final VC800SerialPort					serialPort;				// open/close port execute getData()....
	final Channels								channels;					// interaction with channels, source of all records
	final Settings								settings;					// application configuration settings
	final HashMap<String, String>	configData = new HashMap<String, String>();

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public VC800Dialog(Shell parent, VC800 useDevice) {
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

			VC800Dialog.log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
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
				this.dialogShell.setSize(350, 365);
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
		      public void handleEvent(Event event) {
		        switch (event.detail) {
		        case SWT.TRAVERSE_ESCAPE:
		        	VC800Dialog.this.dialogShell.close();
		          event.detail = SWT.TRAVERSE_NONE;
		          event.doit = false;
		          break;
		        }
		      }
		    });
				this.dialogShell.addFocusListener(new FocusAdapter() {
					@Override
					public void focusGained(FocusEvent evt) {
						VC800Dialog.log.log(Level.FINER, "dialogShell.focusGained, event=" + evt); //$NON-NLS-1$
						if (!VC800Dialog.this.isConnectionWarned) {
							try {
								VC800Dialog.this.updateConfigTread = new Thread("updateConfig") {
									@Override
									public void run() {
										try {
											updateConfig();
										}
										catch (Exception e) {
											VC800Dialog.this.isConnectionWarned = true;
											log.log(Level.WARNING, e.getMessage(), e);
											VC800Dialog.this.application.openMessageDialog(VC800Dialog.this.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0024, new Object[] { e.getMessage() } ));
										}
										finally {
											if (VC800Dialog.this.isPortOpenedByMe) {
												VC800Dialog.this.serialPort.close();
											}
										}
									}
								};

								VC800Dialog.this.updateConfigTread.start();
							}
							catch (RuntimeException e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						VC800Dialog.log.log(Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						VC800Dialog.this.application.openHelpDialog(DEVICE_NAME, "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						VC800Dialog.this.dispose();
					}
				});
				{
					this.boundsComposite = new Composite(this.dialogShell, SWT.SHADOW_IN);
					FormData boundsCompositeLData = new FormData();
					boundsCompositeLData.left = new FormAttachment(0, 1000, 0);
					boundsCompositeLData.right = new FormAttachment(1000, 1000, 0);
					boundsCompositeLData.top = new FormAttachment(0, 1000, 0);
					boundsCompositeLData.bottom = new FormAttachment(1000, 1000, 0);
					this.boundsComposite.setLayoutData(boundsCompositeLData);
					this.boundsComposite.setLayout(new FormLayout());
					this.boundsComposite.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							VC800Dialog.log.log(Level.FINER, "boundsComposite.paintControl() " + evt); //$NON-NLS-1$
							if (VC800Dialog.this.dataGatherThread != null && VC800Dialog.this.dataGatherThread.isAlive()) {
								VC800Dialog.this.startCollectDataButton.setEnabled(false);
								VC800Dialog.this.stopCollectDataButton.setEnabled(true);
							}
							else {
								VC800Dialog.this.startCollectDataButton.setEnabled(true);
								VC800Dialog.this.stopCollectDataButton.setEnabled(false);
							}
						}
					});
					{
						FormData infoTextLData = new FormData();
						infoTextLData.height = 80;
						infoTextLData.left = new FormAttachment(0, 1000, 12);
						infoTextLData.top = new FormAttachment(0, 1000, 12);
						infoTextLData.right = new FormAttachment(1000, 1000, -12);
						this.infoText = new Text(this.boundsComposite, SWT.WRAP | SWT.MULTI );
						this.infoText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.infoText.setLayoutData(infoTextLData);
						this.infoText.setText(Messages.getString(MessageIds.GDE_MSGT1521));
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
						this.startCollectDataButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.startCollectDataButton.setLayoutData(startCollectDataButtonLData);
						this.startCollectDataButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0274));
						this.startCollectDataButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								VC800Dialog.log.log(Level.FINEST, "startCollectDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (!VC800Dialog.this.serialPort.isConnected()) {
									try {
										Channel activChannel = Channels.getInstance().getActiveChannel();
										if (activChannel != null) {
											VC800Dialog.this.dataGatherThread = new GathererThread(VC800Dialog.this.application, VC800Dialog.this.device, VC800Dialog.this.serialPort, activChannel.getNumber(), VC800Dialog.this);
											try {
												VC800Dialog.this.dataGatherThread.start();
											}
											catch (RuntimeException e) {
												log.log(Level.WARNING, e.getMessage(), e);
											}
											VC800Dialog.this.boundsComposite.redraw();
										}
									}
									catch (Exception e) {
										if (VC800Dialog.this.dataGatherThread != null && VC800Dialog.this.dataGatherThread.isCollectDataStopped) {
											VC800Dialog.this.dataGatherThread.stopDataGatheringThread(false);
										}
										VC800Dialog.this.boundsComposite.redraw();
										VC800Dialog.this.application.updateGraphicsWindow();
										VC800Dialog.this.application.openMessageDialog(VC800Dialog.this.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0023, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
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
								VC800Dialog.log.log(Level.FINEST, "stopColletDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (VC800Dialog.this.dataGatherThread != null && VC800Dialog.this.serialPort.isConnected()) {
									VC800Dialog.this.dataGatherThread.stopDataGatheringThread(false);
								}
								VC800Dialog.this.boundsComposite.redraw();
							}
						});
						this.stopCollectDataButton.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
					}
					{
						FormData configGroupLData = new FormData();
						configGroupLData.height = 100;
						configGroupLData.left = new FormAttachment(0, 1000, 12);
						configGroupLData.top = new FormAttachment(0, 1000, 155);
						configGroupLData.right = new FormAttachment(1000, 1000, -12);
						this.configGroup = new Group(this.boundsComposite, SWT.NONE);
						this.configGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						RowLayout configGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.configGroup.setLayout(configGroupLayout);
						this.configGroup.setLayoutData(configGroupLData);
						this.configGroup.setText(Messages.getString(MessageIds.GDE_MSGT1534));
						this.configGroup.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								VC800Dialog.log.log(Level.FINEST, "configGroup.paintControl, event=" + evt); //$NON-NLS-1$
								if (VC800Dialog.this.configData.size() >= 3) {
									VC800Dialog.this.inputTypeUnit.setText(
											VC800Dialog.this.configData.get(VC800.INPUT_TYPE) + "  "	+ VC800Dialog.this.configData.get(VC800.INPUT_SYMBOL) //$NON-NLS-1$
											+ "   [" + VC800Dialog.this.configData.get(VC800.INPUT_UNIT) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
								}
								VC800Dialog.this.batteryCondition.setText(VC800Dialog.this.isBatteryOK ? Messages.getString(MessageIds.GDE_MSGT1535) : Messages.getString(MessageIds.GDE_MSGT1536));
							}
						});
						{
							RowData composite1LData = new RowData();
							composite1LData.width = 150;
							composite1LData.height = 95;
							this.labelComposite = new Composite(this.configGroup, SWT.NONE);
							FillLayout composite1Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
							this.labelComposite.setLayout(composite1Layout);
							this.labelComposite.setLayoutData(composite1LData);
							{
								this.inputTypeLabel = new CLabel(this.labelComposite, SWT.NONE);
								this.inputTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.inputTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT1530));
							}
							{
								this.batteryLabel = new CLabel(this.labelComposite, SWT.NONE);
								this.batteryLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.batteryLabel.setText(Messages.getString(MessageIds.GDE_MSGT1531));
							}
						}
						{
							this.dataComposite = new Composite(this.configGroup, SWT.NONE);
							FillLayout composite3Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
							RowData composite3LData = new RowData();
							composite3LData.width = 150;
							composite3LData.height = 95;
							this.dataComposite.setLayoutData(composite3LData);
							this.dataComposite.setLayout(composite3Layout);
							{
								this.inputTypeUnit = new CLabel(this.dataComposite, SWT.NONE);
								this.inputTypeUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.inputTypeUnit.setText(Messages.getString(MessageIds.GDE_MSGT1532));
							}
							{
								this.batteryCondition = new CLabel(this.dataComposite, SWT.NONE);
								this.batteryCondition.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.batteryCondition.setText(this.isBatteryOK ? Messages.getString(MessageIds.GDE_MSGT1535) : Messages.getString(MessageIds.GDE_MSGT1536));
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
								VC800Dialog.log.log(Level.FINEST, "okButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								close();
							}
						});
						this.closeButton.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
					}
					this.boundsComposite.addMouseTrackListener(new MouseTrackAdapter() {
						@Override
						public void mouseEnter(MouseEvent evt) {
							VC800Dialog.log.log(Level.FINE, "boundsComposite.mouseEnter, event=" + evt); //$NON-NLS-1$
							fadeOutAplhaBlending(evt, VC800Dialog.this.boundsComposite.getSize(), 10, 10, 10, 15);
						}

						@Override
						public void mouseHover(MouseEvent evt) {
							VC800Dialog.log.log(Level.FINEST, "boundsComposite.mouseHover, event=" + evt); //$NON-NLS-1$
						}

						@Override
						public void mouseExit(MouseEvent evt) {
							VC800Dialog.log.log(Level.FINE, "boundsComposite.mouseExit, event=" + evt); //$NON-NLS-1$
							fadeInAlpaBlending(evt, VC800Dialog.this.boundsComposite.getSize(), 10, 10, -10, 15);
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
			VC800Dialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public void resetButtons() {
		if (!this.isDisposed()) {
			this.startCollectDataButton.setEnabled(true);
			this.stopCollectDataButton.setEnabled(false);
		}
	}

	/**
	 * @return the configData
	 */
	public HashMap<String, String> getConfigData() {
		return this.configData;
	}

	/**
	 * @throws ApplicationConfigurationException
	 * @throws SerialPortException
	 * @throws InterruptedException
	 * @throws TimeOutException
	 * @throws IOException
	 * @throws Exception
	 */
	void updateConfig() throws ApplicationConfigurationException, SerialPortException, InterruptedException, TimeOutException, IOException, Exception {
		if (VC800Dialog.this.configData.size() < 3 || !VC800Dialog.this.configData.get(VC800.INPUT_TYPE).equals(Messages.getString(MessageIds.GDE_MSGT1500).split(" ")[0])) {
			if (!VC800Dialog.this.serialPort.isConnected()) {
				VC800Dialog.this.serialPort.open();
				this.isPortOpenedByMe = true;
			}
			else {
				this.isPortOpenedByMe = false;
			}
			do {
				byte[] dataBuffer = VC800Dialog.this.serialPort.getData();
				this.device.getMeasurementInfo(dataBuffer, VC800Dialog.this.configData);
				this.isBatteryOK = this.device.isBatteryLevelLow(dataBuffer);
			} while (VC800Dialog.this.configData.get(VC800.INPUT_TYPE) == null || VC800Dialog.this.configData.get(VC800.INPUT_TYPE).equals(Messages.getString(MessageIds.GDE_MSGT1500).split(" ")[0]));
			if (this.dialogShell != null && !this.dialogShell.isDisposed()) {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						VC800Dialog.this.configGroup.redraw();
					}
				});
			}
		}
	}
}
