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
package gde.device.tttronix;

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
import java.util.logging.Level;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
public class QcCopterDialog extends DeviceDialog {
	final static Logger				log								= Logger.getLogger(QcCopterDialog.class.getName());

	CTabFolder								tabFolder;
	final List<CTabItem>			configurations		= new ArrayList<CTabItem>();
	Text											terminalText;
	Composite									terminalComposite;
	CTabItem									terminalTabItem;

	Button										saveButton, startConfiguration, closeButton;
	GathererThread						dataGatherThread;

	final QcCopter						device;																															// get device specific things, get serial port, ...
	final QcCopterSerialPort	serialPort;																													// open/close port execute getData()....
	final Settings						settings;																														// application configuration settings

	int												measurementsCount	= 0;
	int												tabFolderSelectionIndex = 0;

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public QcCopterDialog(Shell parent, QcCopter useDevice) {
		super(parent);
		this.device = useDevice;
		this.serialPort = useDevice.getCommunicationPort();
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

			QcCopterDialog.log.log(java.util.logging.Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
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
				this.dialogShell.pack();
				this.dialogShell.setSize(600, 10 + 90 + this.measurementsCount * 30 / 2 + 55);
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
		      public void handleEvent(Event event) {
		        switch (event.detail) {
		        case SWT.TRAVERSE_ESCAPE:
		        	QcCopterDialog.this.dialogShell.close();
		          event.detail = SWT.TRAVERSE_NONE;
		          event.doit = false;
		          break;
		        }
		      }
		    });
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						QcCopterDialog.log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (QcCopterDialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { QcCopterDialog.this.device.getPropertiesFileName() });
							if (QcCopterDialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								QcCopterDialog.log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
								QcCopterDialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
						QcCopterDialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						QcCopterDialog.log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						QcCopterDialog.this.application.openHelpDialog("QC-Copter", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				{
					this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
					{
						this.terminalTabItem = new CTabItem(this.tabFolder, SWT.NONE);
						this.terminalTabItem.setText("Terminal");
						{
							this.terminalComposite = new Composite(this.tabFolder, SWT.NONE);
							GridLayout terminalCompositeLayout = new GridLayout();
							this.terminalComposite.setLayout(terminalCompositeLayout);
							this.terminalTabItem.setControl(this.terminalComposite);
							//terminalComposite.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
							{
								this.terminalText = new Text(this.terminalComposite, SWT.MULTI | SWT.LEFT | SWT.WRAP | SWT.BORDER);
								GridData terminalTextLData = new GridData();
								terminalTextLData.widthHint = 410;
								terminalTextLData.heightHint = 300;
								terminalTextLData.grabExcessHorizontalSpace = true;
								terminalTextLData.grabExcessVerticalSpace = true;
								terminalTextLData.verticalAlignment = GridData.CENTER;
								terminalTextLData.horizontalAlignment = GridData.CENTER;
								this.terminalText.setLayoutData(terminalTextLData);
								this.terminalText.setText("012345678901234567890123456789012345678901234567890\n1\n2\n3\n4\n5\n6\n7\n8\n9\n0\n1\n2\n3\n4\n5\n6\n7\n8\n9");
								this.terminalText.setFont(SWTResourceManager.getFont(GDE.IS_LINUX?"Monospace":"Courier", GDE.WIDGET_FONT_SIZE-1, SWT.NORMAL));
							}
						}
					}
					{
						for (int i = 0; i < this.device.getChannelCount(); i++) {
							this.configurations.add(new QcCopterTabItem(this.tabFolder, this, (i + 1), this.device));
						}
					}
					{
						this.saveButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData saveButtonLData = new FormData();
						saveButtonLData.width = 120;
						saveButtonLData.height = 30;
						saveButtonLData.bottom = new FormAttachment(1000, 1000, -10);
						saveButtonLData.left = new FormAttachment(0, 1000, 55);
						this.saveButton.setLayoutData(saveButtonLData);
						this.saveButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.saveButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0486));
						this.saveButton.setEnabled(false);
						this.saveButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								QcCopterDialog.log.log(java.util.logging.Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								QcCopterDialog.this.device.storeDeviceProperties();
								QcCopterDialog.this.saveButton.setEnabled(false);
							}
						});
					}
					{
						this.startConfiguration = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData startConfigurationLData = new FormData();
						startConfigurationLData.height = 30;
						startConfigurationLData.left = new FormAttachment(0, 1000, 200);
						startConfigurationLData.right = new FormAttachment(1000, 1000, -200);
						startConfigurationLData.bottom = new FormAttachment(1000, 1000, -10);
						this.startConfiguration.setLayoutData(startConfigurationLData);
						this.startConfiguration.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.startConfiguration.setText(this.device.serialPort.isConnected() ? "stop configurartion" : "start configurartion");//QcCopterDialog.java
						this.startConfiguration.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								QcCopterDialog.log.log(java.util.logging.Level.FINEST, "startConfiguration.widgetSelected, event=" + evt); //$NON-NLS-1$
								QcCopterDialog.this.device.open_closeCommPort();
								checkPortStatus();
							}
						});
					}
					{
						this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData closeButtonLData = new FormData();
						closeButtonLData.width = 120;
						closeButtonLData.height = 30;
						closeButtonLData.right = new FormAttachment(1000, 1000, -55);
						closeButtonLData.bottom = new FormAttachment(1000, 1000, -10);
						this.closeButton.setLayoutData(closeButtonLData);
						this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0485));
						this.closeButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								QcCopterDialog.log.log(java.util.logging.Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								QcCopterDialog.this.dialogShell.dispose();
							}
						});
					}
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
							QcCopterDialog.log.log(java.util.logging.Level.FINEST, "configTabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (QcCopterDialog.this.tabFolder.getSelectionIndex() > 0) {
								int channelNumber = QcCopterDialog.this.tabFolder.getSelectionIndex() - 1;
								if (channelNumber >= 0 && channelNumber <= QcCopterDialog.this.device.getChannelCount()) { // enable other tabs for future use
									channelNumber += 1;
									String configKey = channelNumber + " : " + ((CTabItem) evt.item).getText(); //$NON-NLS-1$
									Channels channels = Channels.getInstance();
									Channel activeChannel = channels.getActiveChannel();
									if (activeChannel != null) {
										QcCopterDialog.log.log(java.util.logging.Level.FINE, "activeChannel = " + activeChannel.getName() + " configKey = " + configKey); //$NON-NLS-1$ //$NON-NLS-2$
										RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
										if (activeRecordSet != null && activeChannel.getNumber() != channelNumber) {
											int answer = QcCopterDialog.this.application.openYesNoMessageDialog(getDialogShell(), Messages.getString(MessageIds.GDE_MSGI1901));
											if (answer == SWT.YES) {
												String recordSetKey = activeRecordSet.getName();
												Channel tmpChannel = channels.get(channelNumber);
												if (tmpChannel != null) {
													QcCopterDialog.log.log(java.util.logging.Level.FINE,
															"move record set " + recordSetKey + " to channel/configuration " + channelNumber + GDE.STRING_BLANK_COLON_BLANK + configKey); //$NON-NLS-1$ //$NON-NLS-2$
													tmpChannel.put(recordSetKey, activeRecordSet.clone(channelNumber));
													activeChannel.remove(recordSetKey);
													channels.switchChannel(channelNumber, recordSetKey);
													RecordSet newActiveRecordSet = channels.get(channelNumber).getActiveRecordSet();
													if (newActiveRecordSet != null) {
														QcCopterDialog.this.device.updateVisibilityStatus(newActiveRecordSet, true);
														QcCopterDialog.this.device.makeInActiveDisplayable(newActiveRecordSet);
													}
												}
											}
											QcCopterDialog.this.application.updateCurveSelectorTable();
										}
									}
								}
							}
							tabFolderSelectionIndex = tabFolder.getSelectionIndex();
						}
					});
				}

				Channel activChannel = Channels.getInstance().getActiveChannel();
				if (activChannel != null) {
					RecordSet activeRecordSet = activChannel.getActiveRecordSet();
					if (activeRecordSet != null) {
						this.tabFolder.setSelection(activeRecordSet.getChannelConfigNumber());
						tabFolderSelectionIndex = tabFolder.getSelectionIndex();
					}
					else {
						if (this.device.serialPort.isMatchAvailablePorts(this.device.getPort()) && !this.device.serialPort.isConnected())
							this.device.open_closeCommPort();
					}
				}

				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 175, 100));
				this.dialogShell.open();
				this.checkPortStatus();
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
			QcCopterDialog.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
		return this.tabFolderSelectionIndex;
	}

	public void setTerminalText(final String newText) {
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				if (!QcCopterDialog.this.dialogShell.isDisposed()) {
					QcCopterDialog.this.terminalText.setText(newText);
					QcCopterDialog.this.terminalText.update();
					log.logp(Level.FINER, "QCCopterDialog", "setTerminalText()", newText);
				}
			}
		});
	}

	/**
	 * toggle the text of start configuration button according comm port state
	 */
	void checkPortStatus() {
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				if (!QcCopterDialog.this.startConfiguration.isDisposed()) {
					if (QcCopterDialog.this.device.serialPort.isConnected()) {
						QcCopterDialog.this.startConfiguration.setText("stop configurartion");
					}
					else {
						QcCopterDialog.this.startConfiguration.setText("start configurartion");
					}
				}
			}
		});
	}
}
