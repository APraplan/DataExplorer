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
    
    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.DeviceDialog;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.device.smmodellbau.unilog2.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog class to enable visualization control
 * @author Winfried Brügmann
 */
public class UniLog2Dialog extends DeviceDialog {
	final static Logger					log									= Logger.getLogger(UniLog2Dialog.class.getName());
	final static String					PROP_n100W					= MeasurementPropertyTypes.PROP_N_100_W.value();

	CTabFolder									tabFolder;
	CTabItem										configurationTabItem;
	Composite										configurationMainComposite;
	UniLog2SetupConfiguration1	configuration1Composite;
	UniLog2SetupConfiguration2	configuration2Composite;

	Button											saveChangesButton, inputFileButton, helpButton, liveGathererButton, closeButton;

	CTabItem										gpsLoggerTabItem, telemetryTabItem;

	UniLog2LiveGatherer					liveThread;

	final UniLog2								device;																																					// get device specific things, get serial port, ...
	final Settings							settings;																																				// application configuration settings
	final Channels							channels;
	final UniLog2SerialPort			serialPort;																																			// open/close port execute getData()....

	UniLog2SetupReaderWriter		loggerSetup;

	RecordSet										lastActiveRecordSet	= null;
	boolean											isVisibilityChanged	= false;
	int													measurementsCount		= 0;

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public UniLog2Dialog(Shell parent, UniLog2 useDevice) {
		super(parent);
		this.device = useDevice;
		this.serialPort = useDevice.getCommunicationPort();
		this.settings = Settings.getInstance();
		this.channels = Channels.getInstance();
		for (int i = 1; i <= this.device.getChannelCount(); i++) {
			this.measurementsCount = 15; //15 measurements are displayed as maximum per visualization tab
		}
	}

	@Override
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			UniLog2Dialog.log.log(java.util.logging.Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				this.loggerSetup = new UniLog2SetupReaderWriter(this.dialogShell, this.device);

				FormLayout dialogShellLayout = new FormLayout();
				this.dialogShell.setLayout(dialogShellLayout);
				this.dialogShell.layout();
				this.dialogShell.pack();
				this.dialogShell.setSize(650, 100 + 65 + 25 + this.measurementsCount * 32 + 50 + 70); //header + tab + label + this.measurementsCount * 23 + buttons
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
					@Override
					public void handleEvent(Event event) {
						switch (event.detail) {
						case SWT.TRAVERSE_ESCAPE:
							UniLog2Dialog.this.dialogShell.close();
							event.detail = SWT.TRAVERSE_NONE;
							event.doit = false;
							break;
						}
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent evt) {
						UniLog2Dialog.log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (UniLog2Dialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { UniLog2Dialog.this.device.getPropertiesFileName() });
							if (UniLog2Dialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								UniLog2Dialog.log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
								UniLog2Dialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
						UniLog2Dialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					@Override
					public void helpRequested(HelpEvent evt) {
						UniLog2Dialog.log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						UniLog2Dialog.this.application.openHelpDialog(Messages.getString(MessageIds.GDE_MSGT2510), "HelpInfo.html"); //$NON-NLS-1$
					}
				});
				this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
					@Override
					public void mouseEnter(MouseEvent evt) {
						if (UniLog2Dialog.log.isLoggable(java.util.logging.Level.FINER)) UniLog2Dialog.log.log(java.util.logging.Level.FINER, "dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
						fadeOutAplhaBlending(evt, getDialogShell().getClientArea(), 10, 10, 10, 15);
					}

					@Override
					public void mouseHover(MouseEvent evt) {
						if (UniLog2Dialog.log.isLoggable(java.util.logging.Level.FINEST)) UniLog2Dialog.log.log(java.util.logging.Level.FINEST, "dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
					}

					@Override
					public void mouseExit(MouseEvent evt) {
						if (UniLog2Dialog.log.isLoggable(java.util.logging.Level.FINER)) UniLog2Dialog.log.log(java.util.logging.Level.FINER, "dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
						fadeInAlpaBlending(evt, getDialogShell().getClientArea(), 10, 10, -10, 15);
					}
				});
				this.dialogShell.addPaintListener(new PaintListener() {
					@Override
					public void paintControl(PaintEvent paintevent) {
						if (UniLog2Dialog.log.isLoggable(java.util.logging.Level.FINEST)) UniLog2Dialog.log.log(java.util.logging.Level.FINEST, "dialogShell.paintControl, event=" + paintevent); //$NON-NLS-1$
						RecordSet activeRecordSet = UniLog2Dialog.this.application.getActiveRecordSet();
						int index = Channels.getInstance().getActiveChannelNumber();
						if (UniLog2Dialog.this.lastActiveRecordSet == null && activeRecordSet != null
								|| (activeRecordSet != null && !UniLog2Dialog.this.lastActiveRecordSet.getName().equals(activeRecordSet.getName()))) {
							UniLog2Dialog.this.tabFolder.setSelection(index - 1);
						}
						UniLog2Dialog.this.lastActiveRecordSet = UniLog2Dialog.this.application.getActiveRecordSet();
					}
				});
				{
					this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
					this.tabFolder.setSimple(false);
					{
						for (int i = 0; i < this.device.getChannelCount(); i++) {
							createVisualizationTabItem(i + 1);
						}
					}
					{
						this.configurationTabItem = new CTabItem(this.tabFolder, SWT.NONE);
						this.configurationTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.configurationTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2515));

						this.configurationMainComposite = new Composite(this.tabFolder, SWT.NONE);
						FormLayout configurationMainCompositeLayout = new FormLayout();
						this.configurationMainComposite.setLayout(configurationMainCompositeLayout);
						this.configurationTabItem.setControl(this.configurationMainComposite);
						{
							FormData layoutConfig1Data = new FormData();
							layoutConfig1Data.top = new FormAttachment(0, 1000, 0);
							layoutConfig1Data.left = new FormAttachment(0, 1000, 0);
							layoutConfig1Data.right = new FormAttachment(493, 1000, 0);
							layoutConfig1Data.bottom = new FormAttachment(1000, 1000, 0);
							this.configuration1Composite = new UniLog2SetupConfiguration1(this.configurationMainComposite, SWT.NONE, this, this.loggerSetup);
							this.configuration1Composite.setLayoutData(layoutConfig1Data);
						}
						{
							FormData layoutConfig2Data = new FormData();
							layoutConfig2Data.top = new FormAttachment(0, 1000, 0);
							layoutConfig2Data.left = new FormAttachment(493, 1000, 0);
							layoutConfig2Data.right = new FormAttachment(1000, 1000, 0);
							layoutConfig2Data.bottom = new FormAttachment(1000, 1000, 0);
							this.configuration2Composite = new UniLog2SetupConfiguration2(this.configurationMainComposite, SWT.NONE, this, this.loggerSetup);
							this.configuration2Composite.setLayoutData(layoutConfig2Data);
						}
					}
					FormData tabFolderLData = new FormData();
					tabFolderLData.top = new FormAttachment(0, 1000, 0);
					tabFolderLData.left = new FormAttachment(0, 1000, 0);
					tabFolderLData.right = new FormAttachment(1000, 1000, 0);
					tabFolderLData.bottom = new FormAttachment(1000, 1000, -50);
					this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tabFolder.setLayoutData(tabFolderLData);
					this.tabFolder.setSelection(this.channels.getActiveChannel() != null ? this.channels.getActiveChannel().getNumber() - 1 : 0);
					this.tabFolder.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2Dialog.log.log(java.util.logging.Level.FINEST, "configTabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2Dialog.this.tabFolder.getSelectionIndex() == 2) {
								UniLog2Dialog.this.configuration1Composite.changeVisibility();
								UniLog2Dialog.this.saveChangesButton.setText(Messages.getString(MessageIds.GDE_MSGT2518));
								UniLog2Dialog.this.saveChangesButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2524));
								UniLog2Dialog.this.loggerSetup.loadSetup();
								UniLog2Dialog.this.configuration1Composite.updateValues();
								UniLog2Dialog.this.configuration2Composite.updateValues();
							}
							else {
								UniLog2Dialog.this.saveChangesButton.setText(Messages.getString(MessageIds.GDE_MSGT2516));
								UniLog2Dialog.this.saveChangesButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2523));

								int channelNumber = UniLog2Dialog.this.tabFolder.getSelectionIndex();
								if (channelNumber >= 0 && channelNumber <= UniLog2Dialog.this.device.getChannelCount()) { // enable other tabs for future use
									channelNumber += 1;
									String configKey = channelNumber + " : " + ((CTabItem) evt.item).getText(); //$NON-NLS-1$
									Channel activeChannel = UniLog2Dialog.this.channels.getActiveChannel();
									if (activeChannel != null) {
										UniLog2Dialog.log.log(java.util.logging.Level.FINE, "activeChannel = " + activeChannel.getName() + " configKey = " + configKey); //$NON-NLS-1$ //$NON-NLS-2$
										RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
										if (activeRecordSet != null && activeChannel.getNumber() != channelNumber) {
											int answer = UniLog2Dialog.this.application.openYesNoMessageDialog(getDialogShell(), Messages.getString(MessageIds.GDE_MSGI2501));
											if (answer == SWT.YES) {
												String recordSetKey = activeRecordSet.getName();
												Channel tmpChannel = UniLog2Dialog.this.channels.get(channelNumber);
												if (tmpChannel != null) {
													UniLog2Dialog.log.log(java.util.logging.Level.FINE,
															"move record set " + recordSetKey + " to channel/configuration " + channelNumber + GDE.STRING_BLANK_COLON_BLANK + configKey); //$NON-NLS-1$ //$NON-NLS-2$
													tmpChannel.put(recordSetKey, activeRecordSet.clone(channelNumber));
													activeChannel.remove(recordSetKey);
													UniLog2Dialog.this.channels.switchChannel(channelNumber, recordSetKey);
													RecordSet newActiveRecordSet = UniLog2Dialog.this.channels.get(channelNumber).getActiveRecordSet();
													if (newActiveRecordSet != null) {
														UniLog2Dialog.this.device.updateVisibilityStatus(newActiveRecordSet, false);
														UniLog2Dialog.this.device.makeInActiveDisplayable(newActiveRecordSet);
													}
												}
											}
											UniLog2Dialog.this.application.updateCurveSelectorTable();
										}
									}
								}
							}
						}
					});
				}
				{
					this.saveChangesButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData saveButtonLData = new FormData();
					saveButtonLData.width = 130;
					saveButtonLData.height = GDE.IS_MAC ? 33 : 30;
					saveButtonLData.left = new FormAttachment(0, 1000, 15);
					saveButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.saveChangesButton.setLayoutData(saveButtonLData);
					this.saveChangesButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.saveChangesButton.setText(Messages.getString(MessageIds.GDE_MSGT2516));
					this.saveChangesButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2523));
					this.saveChangesButton.setEnabled(false);
					this.saveChangesButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2Dialog.log.log(java.util.logging.Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2Dialog.this.tabFolder.getSelectionIndex() == 2) {
								UniLog2Dialog.this.loggerSetup.saveSetup();
							}
							else {
								UniLog2Dialog.this.device.storeDeviceProperties();
							}
							UniLog2Dialog.this.saveChangesButton.setEnabled(false);
						}
					});
				}
				{
					this.inputFileButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData inputFileButtonLData = new FormData();
					inputFileButtonLData.width = 130;
					inputFileButtonLData.height = GDE.IS_MAC ? 33 : 30;
					inputFileButtonLData.left = new FormAttachment(0, 1000, 155);
					inputFileButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.inputFileButton.setLayoutData(inputFileButtonLData);
					this.inputFileButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.inputFileButton.setText(Messages.getString(MessageIds.GDE_MSGT2517));
					this.inputFileButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2525));
					this.inputFileButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2Dialog.log.log(java.util.logging.Level.FINEST, "inputFileButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2Dialog.this.isVisibilityChanged) {
								String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { UniLog2Dialog.this.device.getPropertiesFileName() });
								if (UniLog2Dialog.this.application.openYesNoMessageDialog(UniLog2Dialog.this.dialogShell, msg) == SWT.YES) {
									UniLog2Dialog.log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
									UniLog2Dialog.this.device.storeDeviceProperties();
								}
							}
							UniLog2Dialog.this.device.open_closeCommPort();
						}
					});
				}
				{
					this.helpButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData helpButtonLData = new FormData();
					helpButtonLData.width = GDE.IS_MAC ? 50 : 40;
					helpButtonLData.height = GDE.IS_MAC ? 33 : 30;
					helpButtonLData.left = new FormAttachment(0, 1000, 302);
					helpButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.helpButton.setLayoutData(helpButtonLData);
					this.helpButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.helpButton.setImage(SWTResourceManager.getImage("gde/resource/QuestionHot.gif")); //$NON-NLS-1$
					this.helpButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2Dialog.log.log(java.util.logging.Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2Dialog.this.application.openHelpDialog(Messages.getString(MessageIds.GDE_MSGT2510), "HelpInfo.html"); //$NON-NLS-1$
						}
					});
				}
				{
					this.liveGathererButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData saveSetupButtonLData = new FormData();
					saveSetupButtonLData.width = 130;
					saveSetupButtonLData.height = GDE.IS_MAC ? 33 : 30;
					saveSetupButtonLData.right = new FormAttachment(1000, 1000, -155);
					saveSetupButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.liveGathererButton.setLayoutData(saveSetupButtonLData);
					this.liveGathererButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.liveGathererButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2578));
					this.liveGathererButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (UniLog2Dialog.log.isLoggable(java.util.logging.Level.FINE)) UniLog2Dialog.log.log(java.util.logging.Level.FINE, "liveGathererButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2Dialog.this.liveThread == null || !UniLog2Dialog.this.serialPort.isConnected()) {
								try {
									UniLog2Dialog.this.liveThread = new UniLog2LiveGatherer(UniLog2Dialog.this.application, UniLog2Dialog.this.device, UniLog2Dialog.this.serialPort, UniLog2Dialog.this);
									try {
										UniLog2Dialog.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT2504), Messages.getString(MessageIds.GDE_MSGT2504));
										UniLog2Dialog.this.liveThread.start();
									}
									catch (RuntimeException e) {
										UniLog2Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
									}
								}
								catch (Exception e) {
									if (UniLog2Dialog.this.liveThread != null && UniLog2Dialog.this.liveThread.isAlive()) {
										UniLog2Dialog.this.liveThread.stopDataGathering();
										UniLog2Dialog.this.liveThread.interrupt();
									}
									UniLog2Dialog.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2504), Messages.getString(MessageIds.GDE_MSGT2504));
									UniLog2Dialog.this.application.updateGraphicsWindow();
									UniLog2Dialog.this.application.openMessageDialog(UniLog2Dialog.this.getDialogShell(),
											Messages.getString(MessageIds.GDE_MSGW2500, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
									UniLog2Dialog.this.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2576));
									UniLog2Dialog.this.liveThread = null;
								}
								UniLog2Dialog.this.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2577));
							}
							else {
								if (UniLog2Dialog.this.liveThread != null && UniLog2Dialog.this.liveThread.isAlive()) {
									UniLog2Dialog.this.liveThread.stopDataGathering();
									UniLog2Dialog.this.liveThread.interrupt();
								}
								UniLog2Dialog.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2504), Messages.getString(MessageIds.GDE_MSGT2504));
								UniLog2Dialog.this.application.updateGraphicsWindow();
								UniLog2Dialog.this.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2576));
								//UniLog2Dialog.this.liveThread = null;
							}
						}
					});
				}
				{
					this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData closeButtonLData = new FormData();
					closeButtonLData.width = 130;
					closeButtonLData.height = GDE.IS_MAC ? 33 : 30;
					closeButtonLData.right = new FormAttachment(1000, 1000, -10);
					closeButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.closeButton.setLayoutData(closeButtonLData);
					this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0485));
					this.closeButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2Dialog.log.log(java.util.logging.Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2Dialog.this.dispose();
						}
					});
				}

				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 375, 10));
				this.dialogShell.open();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}

			if (this.serialPort != null && this.serialPort.isConnected())
				this.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2577));
			else
				this.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2576));

			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			UniLog2Dialog.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * create a visualization control tab item
	 * @param channelNumber
	 */
	private void createVisualizationTabItem(final int channelNumber) {
		CTabItem visualizationTabItem = new CTabItem(this.tabFolder, SWT.NONE);
		visualizationTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		visualizationTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2509) + GDE.STRING_MESSAGE_CONCAT + this.device.getChannelNameReplacement(channelNumber)); //);

		Composite visualizationMainComposite = new Composite(this.tabFolder, SWT.NONE);
		FormLayout visualizationMainCompositeLayout = new FormLayout();
		visualizationMainComposite.setLayout(visualizationMainCompositeLayout);
		visualizationTabItem.setControl(visualizationMainComposite);
		{
			FormData layoutData = new FormData();
			layoutData.top = new FormAttachment(0, 1000, 0);
			layoutData.left = new FormAttachment(0, 1000, 0);
			layoutData.right = new FormAttachment(458, 1000, 0);
			layoutData.bottom = new FormAttachment(1000, 1000, 0);
			new UniLog2VisualizationControl(visualizationMainComposite, layoutData, this, channelNumber, this.device, Messages.getString(MessageIds.GDE_MSGT2510), 0, 15);

			CTabFolder subTabFolder1 = new CTabFolder(visualizationMainComposite, SWT.NONE);
			//this.subTabFolder1.setSimple(false);
			FormData subTabFolder1LData = new FormData();
			subTabFolder1LData.top = new FormAttachment(0, 1000, 0);
			subTabFolder1LData.left = new FormAttachment(460, 1000, 0);
			subTabFolder1LData.right = new FormAttachment(1000, 1000, 0);
			subTabFolder1LData.bottom = new FormAttachment(1000, 1000, 0);
			subTabFolder1.setLayoutData(subTabFolder1LData);

			{
				CTabItem uniLogTabItem = new CTabItem(subTabFolder1, SWT.NONE);
				uniLogTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
				uniLogTabItem.setText("                                                                      ");//Messages.getString(MessageIds.GDE_MSGT2511)); //$NON-NLS-1$
				Composite uniLogVisualization = new Composite(subTabFolder1, SWT.NONE);
				FormLayout compositeLayout = new FormLayout();
				uniLogVisualization.setLayout(compositeLayout);
				uniLogTabItem.setControl(uniLogVisualization);
				FormData layoutUniLogData = new FormData();
				layoutUniLogData.top = new FormAttachment(0, 1000, 0);
				layoutUniLogData.left = new FormAttachment(0, 1000, 0);
				layoutUniLogData.right = new FormAttachment(1000, 1000, 0);
				layoutUniLogData.bottom = new FormAttachment(1000, 1000, -100);
				new UniLog2VisualizationControl(uniLogVisualization, layoutUniLogData, this, channelNumber, this.device, Messages.getString(MessageIds.GDE_MSGT2511), 15, 9);
				{
					Composite filler = new Composite(uniLogVisualization, SWT.NONE);
					FormData composite2LData = new FormData();
					composite2LData.left = new FormAttachment(0, 1000, 0);
					composite2LData.bottom = new FormAttachment(1000, 1000, 0);
					composite2LData.right = new FormAttachment(1000, 1000, 0);
					composite2LData.height = 40;
					//composite2LData.top =  new FormAttachment(0, 1000, 325);
					filler.setLayoutData(composite2LData);
					filler.setLayout(new RowLayout());
					{
						CLabel efficencyLabel = new CLabel(filler, SWT.RIGHT);
						RowData efficencyRowData = new RowData();
						efficencyRowData.height = 20;
						efficencyRowData.width = 150;
						efficencyLabel.setLayoutData(efficencyRowData);
						efficencyLabel.setText(Messages.getString(MessageIds.GDE_MSGT2505));
						efficencyLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2506));
						efficencyLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					}
					{
						final Text propeller_n100W_Text = new Text(filler, SWT.BORDER | SWT.CENTER);
						RowData efficencyRowData = new RowData();
						efficencyRowData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
						efficencyRowData.width = 80;
						propeller_n100W_Text.setLayoutData(efficencyRowData);
						propeller_n100W_Text.setText("3600"); //$NON-NLS-1$
						propeller_n100W_Text.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						propeller_n100W_Text.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								UniLog2Dialog.log.log(java.util.logging.Level.FINEST, "efficencyN100WText.keyReleaded, evt=" + evt); //$NON-NLS-1$
								try {
									int propeller_n100W_Value = Integer.valueOf(propeller_n100W_Text.getText().trim());
									if (UniLog2Dialog.this.channels.getActiveChannel() != null) {
										RecordSet recordSet = UniLog2Dialog.this.channels.getActiveChannel().getActiveRecordSet();
										if (recordSet != null) {
											Record record = recordSet.get(14); //14=Efficiency
											PropertyType property = record.getProperty(UniLog2Dialog.PROP_n100W);
											if (property != null) {
												property.setValue(propeller_n100W_Value);
											}
											else {
												record.createProperty(UniLog2Dialog.PROP_n100W, DataTypes.INTEGER, propeller_n100W_Value);
											}
											recordSet.setRecalculationRequired();
											UniLog2Dialog.this.device.makeInActiveDisplayable(recordSet);
											UniLog2Dialog.this.application.updateGraphicsWindow();
											UniLog2Dialog.this.application.updateStatisticsData();
											UniLog2Dialog.this.application.updateDataTable(recordSet.getName(), true);
											recordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
										}
										UniLog2Dialog.this.device.setMeasurementPropertyValue(channelNumber, 14, MeasurementPropertyTypes.PROP_N_100_W.value(), DataTypes.INTEGER, propeller_n100W_Value);
									}
									UniLog2Dialog.this.saveChangesButton.setEnabled(true);
								}
								catch (Exception e) {
									UniLog2Dialog.this.application.openMessageDialog(UniLog2Dialog.this.getDialogShell(),
											Messages.getString(gde.messages.MessageIds.GDE_MSGE0030, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
								}
							}
						});
						propeller_n100W_Text.addFocusListener(new FocusAdapter() {
							@Override
							public void focusGained(FocusEvent evt) {
								PropertyType property = UniLog2Dialog.this.device.getMeasruementProperty(channelNumber, 14, UniLog2Dialog.PROP_n100W);
								if (property != null)
									propeller_n100W_Text.setText(property.getValue());
								else
									propeller_n100W_Text.setText("3600"); //$NON-NLS-1$
							}
						});
					}
					{
						CLabel efficencyUnit = new CLabel(filler, SWT.NONE);
						RowData efficencyRowData = new RowData();
						efficencyRowData.height = 20;
						efficencyRowData.width = 50;
						efficencyUnit.setLayoutData(efficencyRowData);
						efficencyUnit.setText(Messages.getString(MessageIds.GDE_MSGT2507));
						efficencyUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					}
				}
			}
			{
				CTabItem mLinkTabItem = new CTabItem(subTabFolder1, SWT.NONE);
				mLinkTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
				mLinkTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2512));
				Composite mLinkVisualization = new Composite(subTabFolder1, SWT.NONE);
				FormLayout compositeLayout = new FormLayout();
				mLinkVisualization.setLayout(compositeLayout);
				mLinkTabItem.setControl(mLinkVisualization);
				FormData layoutMLinkData = new FormData();
				layoutMLinkData.top = new FormAttachment(0, 1000, 0);
				layoutMLinkData.left = new FormAttachment(0, 1000, 0);
				layoutMLinkData.right = new FormAttachment(1000, 1000, 0);
				layoutMLinkData.bottom = new FormAttachment(1000, 1000, 0);
				new UniLog2VisualizationControl(mLinkVisualization, layoutMLinkData, this, channelNumber, this.device, Messages.getString(MessageIds.GDE_MSGT2512), 24, 15);
			}
			subTabFolder1.setSelection(0);
		}
	}

	/**
	 * set the save visualization configuration button enabled 
	 */
	@Override
	public void enableSaveButton(boolean enable) {
		this.saveChangesButton.setEnabled(enable);
		this.application.updateAllTabs(true);
	}

	/**
	 * set the save configuration button enabled 
	 */
	public void enableSaveConfigurationButton(boolean enable) {
		this.saveChangesButton.setEnabled(enable);
	}

	/**
	 * @return the tabFolder selection index
	 */
	public Integer getTabFolderSelectionIndex() {
		return this.tabFolder.getSelectionIndex();
	}

	public void resetButtons() {
		if (this.dialogShell != null && !this.dialogShell.isDisposed()) {
			this.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2576));
			this.liveThread = null;
		}
	}

	public void updateAnalogAlarmUnits() {
		this.configuration2Composite.updateAnalogAlarmUnits();
	}
}
