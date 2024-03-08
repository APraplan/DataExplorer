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
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.opentx;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DeviceDialog;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
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
public class OpenTxAdapterDialog extends DeviceDialog {
	final static Logger	log									= Logger.getLogger(OpenTxAdapterDialog.class.getName());

	CTabFolder					tabFolder, subTabFolder1, subTabFolder2;
	CTabItem						visualizationTabItem;
	Button							saveVisualizationButton, inputFileButton, helpButton, closeButton;

	CTabItem						gpsLoggerTabItem, telemetryTabItem;

	final OpenTxAdapter	device;																																	// get device specific things, get serial port, ...
	final Settings			settings;																																// application configuration settings

	RecordSet						lastActiveRecordSet	= null;
	boolean							isVisibilityChanged	= false;
	int									measurementsCount		= 0;

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public OpenTxAdapterDialog(Shell parent, OpenTxAdapter useDevice) {
		super(parent);
		this.device = useDevice;
		this.settings = Settings.getInstance();
		this.measurementsCount = this.device.getNumberOfMeasurements(1);
	}

	@Override
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			OpenTxAdapterDialog.log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);

				FormLayout dialogShellLayout = new FormLayout();
				this.dialogShell.setLayout(dialogShellLayout);
				this.dialogShell.layout();
				this.dialogShell.pack();
				this.dialogShell.setSize(GDE.IS_LINUX ? 750 : 685, 30 + 25 + 25 + (this.measurementsCount / 4) * 26 + 50 + 42); //header + tab + label + this./2 * 26 + buttons
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
					@Override
					public void handleEvent(Event event) {
						switch (event.detail) {
						case SWT.TRAVERSE_ESCAPE:
							OpenTxAdapterDialog.this.dialogShell.close();
							event.detail = SWT.TRAVERSE_NONE;
							event.doit = false;
							break;
						}
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent evt) {
						OpenTxAdapterDialog.log.log(Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (OpenTxAdapterDialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { OpenTxAdapterDialog.this.device.getPropertiesFileName() });
							if (OpenTxAdapterDialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								OpenTxAdapterDialog.log.log(Level.FINE, "SWT.YES"); //$NON-NLS-1$
								OpenTxAdapterDialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
						OpenTxAdapterDialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					@Override
					public void helpRequested(HelpEvent evt) {
						OpenTxAdapterDialog.log.log(Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						OpenTxAdapterDialog.this.application.openHelpDialog(OpenTxAdapterDialog.this.device.getName(), "HelpInfo.html"); //$NON-NLS-1$
					}
				});
				this.dialogShell.addPaintListener(new PaintListener() {
					@Override
					public void paintControl(PaintEvent paintevent) {
						if (OpenTxAdapterDialog.log.isLoggable(Level.FINEST)) OpenTxAdapterDialog.log.log(Level.FINEST, "dialogShell.paintControl, event=" + paintevent); //$NON-NLS-1$
						RecordSet activeRecordSet = OpenTxAdapterDialog.this.application.getActiveRecordSet();
						if (OpenTxAdapterDialog.this.lastActiveRecordSet == null && activeRecordSet != null
								|| (activeRecordSet != null && !OpenTxAdapterDialog.this.lastActiveRecordSet.getName().equals(activeRecordSet.getName()))) {
							OpenTxAdapterDialog.this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
						}
						OpenTxAdapterDialog.this.lastActiveRecordSet = OpenTxAdapterDialog.this.application.getActiveRecordSet();
					}
				});
				{
					this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
					this.tabFolder.setSimple(false);
					{
						for (int i = 1; i <= this.device.getChannelCount(); i++) {
							new OpenTxAdapterDialogTabItem(this.tabFolder, this, i, this.device);
						}
					}
					FormData tabFolderLData = new FormData();
					tabFolderLData.top = new FormAttachment(0, 1000, 0);
					tabFolderLData.left = new FormAttachment(0, 1000, 0);
					tabFolderLData.right = new FormAttachment(1000, 1000, 0);
					tabFolderLData.bottom = new FormAttachment(1000, 1000, -50);
					this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tabFolder.setLayoutData(tabFolderLData);
					this.tabFolder.setSelection(0);
				}
				{
					this.saveVisualizationButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData saveButtonLData = new FormData();
					saveButtonLData.width = 130;
					saveButtonLData.height = GDE.IS_MAC ? 33 : 30;
					saveButtonLData.left = new FormAttachment(0, 1000, 15);
					saveButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.saveVisualizationButton.setLayoutData(saveButtonLData);
					this.saveVisualizationButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.saveVisualizationButton.setText(Messages.getString(MessageIds.GDE_MSGT3315));
					this.saveVisualizationButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3316));
					this.saveVisualizationButton.setEnabled(false);
					this.saveVisualizationButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							OpenTxAdapterDialog.log.log(Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							OpenTxAdapterDialog.this.device.storeDeviceProperties();
							OpenTxAdapterDialog.this.saveVisualizationButton.setEnabled(false);
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
					this.inputFileButton.setText(Messages.getString(MessageIds.GDE_MSGT3302));
					this.inputFileButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3314));
					this.inputFileButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							OpenTxAdapterDialog.log.log(Level.FINEST, "inputFileButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (OpenTxAdapterDialog.this.isVisibilityChanged) {
								String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { OpenTxAdapterDialog.this.device.getPropertiesFileName() });
								if (OpenTxAdapterDialog.this.application.openYesNoMessageDialog(OpenTxAdapterDialog.this.dialogShell, msg) == SWT.YES) {
									OpenTxAdapterDialog.log.log(Level.FINE, "SWT.YES"); //$NON-NLS-1$
									OpenTxAdapterDialog.this.device.storeDeviceProperties();
								}
							}
							OpenTxAdapterDialog.this.device.open_closeCommPort();
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
							OpenTxAdapterDialog.log.log(Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							OpenTxAdapterDialog.this.application.openHelpDialog(OpenTxAdapterDialog.this.device.getName().replace(GDE.STRING_DASH, GDE.STRING_EMPTY), "HelpInfo.html"); //$NON-NLS-1$
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
							OpenTxAdapterDialog.log.log(Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							OpenTxAdapterDialog.this.dispose();
						}
					});
				}
				try {
					this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
				}
				catch (RuntimeException e) {
					this.tabFolder.setSelection(0);
				}
				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 375, 10));
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
			OpenTxAdapterDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * set the save visualization configuration button enabled 
	 */
	@Override
	public void enableSaveButton(boolean enable) {
		this.saveVisualizationButton.setEnabled(enable);
		this.application.updateAllTabs(true);
	}

	/**
	 * @return the tabFolder selection index
	 */
	public Integer getTabFolderSelectionIndex() {
		return this.tabFolder.getSelectionIndex();
	}
}
