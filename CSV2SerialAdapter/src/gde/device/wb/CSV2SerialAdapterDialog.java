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
    
    Copyright (c) 2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.wb;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DeviceDialog;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog class to enable visualization control
 * @author Winfried Brügmann
 */
public class CSV2SerialAdapterDialog extends DeviceDialog {
	final static Logger		log								= Logger.getLogger(CSV2SerialAdapterDialog.class.getName());

	CTabFolder							tabFolder;
	Button									saveButton, closeButton, helpButton;

	final CSV2SerialAdapter	device;																																				// get device specific things, get serial port, ...
	final Settings					settings;																																			// application configuration settings

	int											measurementsCount	= 0;
	final List<CTabItem>		configurations		= new ArrayList<CTabItem>();

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public CSV2SerialAdapterDialog(Shell parent, CSV2SerialAdapter useDevice) {
		super(parent);
		this.device = useDevice;
		this.settings = Settings.getInstance();
		for (int i = 1; i <= this.device.getChannelCount(); i++) {
			int actualMeasurementCount = this.device.getNumberOfMeasurements(i);
			this.measurementsCount = actualMeasurementCount > this.measurementsCount ? actualMeasurementCount : this.measurementsCount;
		}
	}

	@Override
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			log.log(java.util.logging.Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);

				FormLayout dialogShellLayout = new FormLayout();
				this.dialogShell.setLayout(dialogShellLayout);
				this.dialogShell.layout();
				//dialogShell.pack();
				this.dialogShell.setSize(GDE.IS_LINUX ? 380 : 350, 600); 
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
					public void handleEvent(Event event) {
						switch (event.detail) {
						case SWT.TRAVERSE_ESCAPE:
							CSV2SerialAdapterDialog.this.dialogShell.close();
							event.detail = SWT.TRAVERSE_NONE;
							event.doit = false;
							break;
						}
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (CSV2SerialAdapterDialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] {CSV2SerialAdapterDialog.this.device.getPropertiesFileName()});
							if (CSV2SerialAdapterDialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
								CSV2SerialAdapterDialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
						CSV2SerialAdapterDialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						CSV2SerialAdapterDialog.this.application.openHelpDialog("CSV2SerialAdapter", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				// enable fade in/out alpha blending (do not fade-in on top)
//				this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
//					@Override
//					public void mouseEnter(MouseEvent evt) {
//						log.log(java.util.logging.Level.FINER, "dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
//						fadeOutAplhaBlending(evt, getDialogShell().getClientArea(), 20, 20, 20, 25);
//					}
//
//					@Override
//					public void mouseHover(MouseEvent evt) {
//						log.log(java.util.logging.Level.FINEST, "dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
//					}
//
//					@Override
//					public void mouseExit(MouseEvent evt) {
//						log.log(java.util.logging.Level.FINER, "dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
//						fadeInAlpaBlending(evt, getDialogShell().getClientArea(), 20, 20, -20, 25);
//					}
//				});
				{
					this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
					FormData tabFolderLData = new FormData();
					tabFolderLData.top = new FormAttachment(0, 1000, 0);
					tabFolderLData.left = new FormAttachment(0, 1000, 0);
					tabFolderLData.right = new FormAttachment(1000, 1000, 0);
					tabFolderLData.bottom = new FormAttachment(1000, 1000, -50);
					this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tabFolder.setLayoutData(tabFolderLData);
					this.tabFolder.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "configTabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
							int channelNumber = CSV2SerialAdapterDialog.this.tabFolder.getSelectionIndex();
							if (channelNumber >= 0 && channelNumber <= CSV2SerialAdapterDialog.this.device.getChannelCount()) { // enable other tabs for future use
								channelNumber += 1;
								String configKey = channelNumber + " : " + ((CTabItem) evt.item).getText(); //$NON-NLS-1$
								Channels channels = Channels.getInstance();
								Channel activeChannel = channels.getActiveChannel();
								if (activeChannel != null) {
									log.log(java.util.logging.Level.FINE, "activeChannel = " + activeChannel.getName() + " configKey = " + configKey); //$NON-NLS-1$ //$NON-NLS-2$
									RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
									if (activeRecordSet != null && activeChannel.getNumber() != channelNumber) {
										int answer = CSV2SerialAdapterDialog.this.application.openYesNoMessageDialog(getDialogShell(), Messages.getString(MessageIds.GDE_MSGI1701));
										if (answer == SWT.YES) {
											String recordSetKey = activeRecordSet.getName();
											Channel tmpChannel = channels.get(channelNumber);
											if (tmpChannel != null) {
												log.log(java.util.logging.Level.FINE,
														"move record set " + recordSetKey + " to channel/configuration " + channelNumber + GDE.STRING_BLANK_COLON_BLANK + configKey); //$NON-NLS-1$ //$NON-NLS-2$
												tmpChannel.put(recordSetKey, activeRecordSet.clone(channelNumber));
												activeChannel.remove(recordSetKey);
												channels.switchChannel(channelNumber, recordSetKey);
												RecordSet newActiveRecordSet = channels.get(channelNumber).getActiveRecordSet();
												if (newActiveRecordSet != null) {
													CSV2SerialAdapterDialog.this.device.updateVisibilityStatus(newActiveRecordSet, false);
													CSV2SerialAdapterDialog.this.device.makeInActiveDisplayable(newActiveRecordSet);
												}
											}
										}
										CSV2SerialAdapterDialog.this.application.updateCurveSelectorTable();
									}
								}
							}
						}
					});
					{
						for (int i = 0; i < this.device.getChannelCount(); i++) {
							this.configurations.add(new CSV2SerialAdapterDialogTabItem(this.tabFolder, this, (i + 1), this.device));
						}
					}
					try {
						this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
					}
					catch (RuntimeException e) {
						this.tabFolder.setSelection(0);
					}
					{
						this.saveButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData saveButtonLData = new FormData();
						saveButtonLData.width = 105;
						saveButtonLData.height = GDE.IS_MAC ? 33 : 30;
						saveButtonLData.left = new FormAttachment(0, 1000, 10);
						saveButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
						this.saveButton.setLayoutData(saveButtonLData);
						this.saveButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.saveButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0486));
						this.saveButton.setEnabled(false);
						this.saveButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								CSV2SerialAdapterDialog.this.device.storeDeviceProperties();
								CSV2SerialAdapterDialog.this.saveButton.setEnabled(false);
							}
						});
					}
					{
						this.helpButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData helpButtonLData = new FormData();
						helpButtonLData.width = GDE.IS_MAC ? 50 : 40;
						helpButtonLData.height = GDE.IS_MAC ? 33 : 30;
						helpButtonLData.left = new FormAttachment(0, 1000, GDE.IS_MAC ? 129 : 132);
						helpButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
						this.helpButton.setLayoutData(helpButtonLData);
						this.helpButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.helpButton.setImage(SWTResourceManager.getImage("gde/resource/QuestionHot.gif")); //$NON-NLS-1$
						this.helpButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								CSV2SerialAdapterDialog.this.application.openHelpDialog("CSV2SerialAdapter", "HelpInfo.html");  //$NON-NLS-1$
							}
						});
					}
					{
						this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData closeButtonLData = new FormData();
						closeButtonLData.width = 105;
						closeButtonLData.height = GDE.IS_MAC ? 33 : 30;
						closeButtonLData.right = new FormAttachment(1000, 1000, -10);
						closeButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
						this.closeButton.setLayoutData(closeButtonLData);
						this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0485));
						this.closeButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								CSV2SerialAdapterDialog.this.dialogShell.dispose();
							}
						});
					}
				}
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
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * implementation of noop method from base dialog class
	 */
	@Override
	public void enableSaveButton(boolean enable) {
		this.saveButton.setEnabled(enable);
	}

	/**
	 * @return the tabFolder selection index
	 */
	public Integer getTabFolderSelectionIndex() {
		return this.tabFolder.getSelectionIndex();
	}
}
