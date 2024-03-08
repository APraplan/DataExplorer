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
    
    Copyright (c) 2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.weatronic;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channels;
import gde.device.ChannelPropertyTypes;
import gde.device.DataTypes;
import gde.device.DeviceDialog;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog class to enable visualization control
 * @author Winfried Brügmann
 */
public class WeatronicAdapterDialog extends DeviceDialog {
	final static Logger			log									= Logger.getLogger(WeatronicAdapterDialog.class.getName());

	CTabFolder							tabFolder;
	CTabItem								serialComTabItem;
	Composite								configMainComosite;
	Button									saveButton, closeButton, helpButton;
	Button									inputFileButton;
	Button									enableChannelRecords, enableStatusFilter, enableUtcTimeFilter;

	final WeatronicAdapter	device;																																				// get device specific things, get serial port, ...
	final Settings					settings;																																			// application configuration settings
	boolean									isVisibilityChanged	= false;

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public WeatronicAdapterDialog(Shell parent, WeatronicAdapter useDevice) {
		super(parent);
		this.device = useDevice;
		this.settings = Settings.getInstance();
	}

	@Override
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			WeatronicAdapterDialog.log.log(java.util.logging.Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
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
				this.dialogShell.setSize(620, 582); //header + tab + label + this.measurementsCount * 28 + loadButton + save/close buttons
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
					@Override
					public void handleEvent(Event event) {
						switch (event.detail) {
						case SWT.TRAVERSE_ESCAPE:
							WeatronicAdapterDialog.this.dialogShell.close();
							event.detail = SWT.TRAVERSE_NONE;
							event.doit = false;
							break;
						}
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent evt) {
						WeatronicAdapterDialog.log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (WeatronicAdapterDialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { WeatronicAdapterDialog.this.device.getPropertiesFileName() });
							if (WeatronicAdapterDialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								WeatronicAdapterDialog.log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
								WeatronicAdapterDialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
						WeatronicAdapterDialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					@Override
					public void helpRequested(HelpEvent evt) {
						WeatronicAdapterDialog.log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						WeatronicAdapterDialog.this.application.openHelpDialog("WeatronicAdapter", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				{
					this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);

					{
						for (int i = 0; i < this.device.getChannelCount(); i++) {
							new WeatronicAdapterDialogTabItem(this.tabFolder, this, (i + 1), this.device);
						}
					}
					FormData tabFolderLData = new FormData();
					tabFolderLData.top = new FormAttachment(0, 1000, 0);
					tabFolderLData.left = new FormAttachment(0, 1000, 0);
					tabFolderLData.right = new FormAttachment(1000, 1000, 0);
					tabFolderLData.bottom = new FormAttachment(1000, 1000, -100);
					this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tabFolder.setLayoutData(tabFolderLData);
					this.tabFolder.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							WeatronicAdapterDialog.log.log(java.util.logging.Level.FINEST, "configTabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
							int channelNumber = WeatronicAdapterDialog.this.tabFolder.getSelectionIndex() + 1;
							//disable moving curves between configurations
							if (channelNumber > 0 && channelNumber <= WeatronicAdapterDialog.this.device.getChannelCount()) { // enable other tabs for future use
								String configKey = channelNumber + " : " + ((CTabItem) evt.item).getText(); //$NON-NLS-1$
								Channels.getInstance().switchChannel(configKey);
							}
						}
					});
				}
				{
					this.enableChannelRecords = new Button(this.dialogShell, SWT.CHECK);
					FormData enableFilterLData = new FormData();
					enableFilterLData.height = GDE.IS_MAC ? 22 : 20;
					enableFilterLData.left = new FormAttachment(0, 1000, 10);
					enableFilterLData.width = 150;
					enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -48 : -50);
					this.enableChannelRecords.setLayoutData(enableFilterLData);
					this.enableChannelRecords.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.enableChannelRecords.setText(Messages.getString(MessageIds.GDE_MSGT3724));
					this.enableChannelRecords.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3725));
					this.enableChannelRecords.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							WeatronicAdapterDialog.log.log(java.util.logging.Level.FINEST, "enableChannelRecords.widgetSelected, event=" + evt); //$NON-NLS-1$
							WeatronicAdapterDialog.this.device.setChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL, DataTypes.BOOLEAN,
									GDE.STRING_EMPTY + WeatronicAdapterDialog.this.enableChannelRecords.getSelection());
							WeatronicAdapter.setChannelFilter(WeatronicAdapterDialog.this.enableChannelRecords.getSelection());
							WeatronicAdapterDialog.this.enableSaveButton(true);
						}
					});
				}
				{
					this.enableStatusFilter = new Button(this.dialogShell, SWT.CHECK);
					FormData enableFilterLData = new FormData();
					enableFilterLData.height = GDE.IS_MAC ? 22 : 20;
					enableFilterLData.left = new FormAttachment(0, 1000, 250);
					enableFilterLData.width = 120;
					enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -48 : -50);
					this.enableStatusFilter.setLayoutData(enableFilterLData);
					this.enableStatusFilter.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.enableStatusFilter.setText(Messages.getString(MessageIds.GDE_MSGT3717));
					this.enableStatusFilter.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3718));
					this.enableStatusFilter.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							WeatronicAdapterDialog.log.log(java.util.logging.Level.FINEST, "enableFilter.widgetSelected, event=" + evt); //$NON-NLS-1$
							WeatronicAdapterDialog.this.device.setChannelProperty(ChannelPropertyTypes.ENABLE_FILTER, DataTypes.BOOLEAN,
									GDE.STRING_EMPTY + WeatronicAdapterDialog.this.enableStatusFilter.getSelection());
							WeatronicAdapter.setStatusFilter(WeatronicAdapterDialog.this.enableStatusFilter.getSelection());
							WeatronicAdapterDialog.this.enableSaveButton(true);
						}
					});
				}
				{
					this.enableUtcTimeFilter = new Button(this.dialogShell, SWT.CHECK);
					FormData enableTextModusFilterLData = new FormData();
					enableTextModusFilterLData.height = GDE.IS_MAC ? 22 : 20;
					enableTextModusFilterLData.left = new FormAttachment(0, 1000, 480);
					enableTextModusFilterLData.width = 100;
					enableTextModusFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -48 : -50);
					this.enableUtcTimeFilter.setLayoutData(enableTextModusFilterLData);
					this.enableUtcTimeFilter.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.enableUtcTimeFilter.setText(Messages.getString(MessageIds.GDE_MSGT3715));
					this.enableUtcTimeFilter.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3716));
					this.enableUtcTimeFilter.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							WeatronicAdapterDialog.log.log(java.util.logging.Level.FINEST, "enableTextModusFilter.widgetSelected, event=" + evt); //$NON-NLS-1$
							WeatronicAdapterDialog.this.device.setChannelProperty(ChannelPropertyTypes.TEXT_MODE, DataTypes.BOOLEAN,
									GDE.STRING_EMPTY + WeatronicAdapterDialog.this.enableUtcTimeFilter.getSelection());
							WeatronicAdapter.setUtcFilter(WeatronicAdapterDialog.this.enableUtcTimeFilter.getSelection());
							WeatronicAdapterDialog.this.enableSaveButton(true);
						}
					});
				}
				{
					this.inputFileButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData inputFileButtonLData = new FormData();
					inputFileButtonLData.height = GDE.IS_MAC ? 33 : 30;
					inputFileButtonLData.left = new FormAttachment(0, 1000, 10);
					inputFileButtonLData.width = 160;
					inputFileButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.inputFileButton.setLayoutData(inputFileButtonLData);
					this.inputFileButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.inputFileButton.setText(Messages.getString(MessageIds.GDE_MSGT3702));
					this.inputFileButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3701));
					this.inputFileButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							WeatronicAdapterDialog.log.log(java.util.logging.Level.FINEST, "inputFileButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (WeatronicAdapterDialog.this.isVisibilityChanged) {
								String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { WeatronicAdapterDialog.this.device.getPropertiesFileName() });
								if (WeatronicAdapterDialog.this.application.openYesNoMessageDialog(WeatronicAdapterDialog.this.getDialogShell(), msg) == SWT.YES) {
									WeatronicAdapterDialog.log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
									WeatronicAdapterDialog.this.device.storeDeviceProperties();
								}
							}
							WeatronicAdapterDialog.this.device.open_closeCommPort();
						}
					});
				}
				{
					this.saveButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData saveButtonLData = new FormData();
					saveButtonLData.width = 105;
					saveButtonLData.height = GDE.IS_MAC ? 33 : 30;
					saveButtonLData.left = new FormAttachment(0, 1000, 190);
					saveButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.saveButton.setLayoutData(saveButtonLData);
					this.saveButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.saveButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0486));
					this.saveButton.setEnabled(false);
					this.saveButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							WeatronicAdapterDialog.log.log(java.util.logging.Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							WeatronicAdapterDialog.this.device.storeDeviceProperties();
							WeatronicAdapterDialog.this.saveButton.setEnabled(false);
						}
					});
				}
				{
					this.helpButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData helpButtonLData = new FormData();
					helpButtonLData.width = 105;
					helpButtonLData.height = GDE.IS_MAC ? 33 : 30;
					helpButtonLData.right = new FormAttachment(1000, 1000, -190);
					helpButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.helpButton.setLayoutData(helpButtonLData);
					this.helpButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.helpButton.setImage(SWTResourceManager.getImage("gde/resource/QuestionHot.gif")); //$NON-NLS-1$
					this.helpButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							WeatronicAdapterDialog.log.log(java.util.logging.Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							WeatronicAdapterDialog.this.application.openHelpDialog("WeatronicAdapter", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
						}
					});
				}
				{
					this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData closeButtonLData = new FormData();
					closeButtonLData.width = 160;
					closeButtonLData.height = GDE.IS_MAC ? 33 : 30;
					closeButtonLData.right = new FormAttachment(1000, 1000, -10);
					closeButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.closeButton.setLayoutData(closeButtonLData);
					this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0485));
					this.closeButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							WeatronicAdapterDialog.log.log(java.util.logging.Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							WeatronicAdapterDialog.this.dialogShell.dispose();
						}
					});
				}

				try {
					this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
				}
				catch (RuntimeException e) {
					this.tabFolder.setSelection(0);
				}

				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - this.dialogShell.getSize().x / 2, 0));
				this.dialogShell.open();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}

			this.enableChannelRecords.setSelection(Boolean.parseBoolean(this.device.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL).getValue() != null ? this.device.getChannelProperty(
					ChannelPropertyTypes.ENABLE_CHANNEL).getValue() : "true"));
			this.enableStatusFilter.setSelection(Boolean.parseBoolean(this.device.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER).getValue() != null ? this.device.getChannelProperty(
					ChannelPropertyTypes.ENABLE_FILTER).getValue() : "true"));
			this.enableUtcTimeFilter.setSelection(Boolean.parseBoolean(this.device.getChannelProperty(ChannelPropertyTypes.TEXT_MODE).getValue() != null ? this.device.getChannelProperty(
					ChannelPropertyTypes.TEXT_MODE).getValue() : "true"));

			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			WeatronicAdapterDialog.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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

	/**
	 * switch to the tab when sensor is detected
	 * @param index
	 */
	public void selectTab(final int index) {
		if (WeatronicAdapterDialog.this.tabFolder != null && !WeatronicAdapterDialog.this.tabFolder.isDisposed()) {
			this.dialogShell.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					WeatronicAdapterDialog.this.tabFolder.setSelection(index - 1);
				}
			});
		}
	}
}
