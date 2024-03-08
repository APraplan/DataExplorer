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
package gde.device.renschler;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DeviceDialog;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

/**
 * Dialog class for the Picolariolog device of Uwe Renschler
 * @author Winfried Brügmann
 */
public class PicolarioDialog extends DeviceDialog {
	final static Logger				log								= Logger.getLogger(PicolarioDialog.class.getName());
	private static final String	DEVICE_NAME	= "Picolario";

	Group											numberAvailableRecorsSetsGroup1;
	Button										queryAvailableRecordSetButton;
	CLabel										numberAvailableRecordSetsLabel;
	String										numberAvailable		= GDE.STRING_EMPTY;

	CTabFolder								configTabFolder;
	CTabItem									configTabItem1, configTabItem2;
	PicolarioConfigTab				configTab1, configTab2;

	Group											readDataGroup3;
	Button										readSingle;
	Button										closeButton;
	Button										stopButton;
	CLabel										alreadyRedLabel;
	CLabel										alreadyRedDataSetsLabel;
	CLabel										redDataSets;
	Button										switchRecordSetButton;
	Button										readAllRecords;
	CLabel										numberRedTelegramLabel;
	CCombo										recordSetSelectCombo;
	String										redDatagrams			= "0"; //$NON-NLS-1$
	String										redDataSetsText		= "0"; //$NON-NLS-1$
	boolean										doSwtichRecordSet	= false;

	final Settings						settings;
	final Picolario						device;
	final PicolarioSerialPort	serialPort;
	DataGathererThread				gatherThread;

	/**
	 * constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice Picolario class implementation == IDevice
	 */
	public PicolarioDialog(Shell parent, Picolario useDevice) {
		super(parent);
		this.device = useDevice;
		this.serialPort = useDevice.getCommunicationPort();
		this.settings = Settings.getInstance();
	}

	@Override
	public void open() {
		this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
		this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

		log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.dialogShell == null || this.dialogShell.isDisposed()) {
			if (this.settings.isDeviceDialogsModal())
				this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
			else if (this.settings.isDeviceDialogsOnTop())
				this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
			else
				this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

			SWTResourceManager.registerResourceUser(this.dialogShell);
			if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);
			this.dialogShell.setLayout(null);
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(345, 590);
			this.dialogShell.setText(DEVICE_NAME + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
			this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
			this.dialogShell.addListener(SWT.Traverse, new Listener() {
	      @Override
				public void handleEvent(Event event) {
	        switch (event.detail) {
	        case SWT.TRAVERSE_ESCAPE:
	        	PicolarioDialog.this.dialogShell.close();
	          event.detail = SWT.TRAVERSE_NONE;
	          event.doit = false;
	          break;
	        }
	      }
	    });
			this.dialogShell.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent evt) {
					log.log(Level.FINE, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					if (PicolarioDialog.this.gatherThread != null && PicolarioDialog.this.gatherThread.isAlive()) PicolarioDialog.this.gatherThread.setThreadStop(true);
					PicolarioDialog.this.dispose();
				}
			});
			this.dialogShell.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
					PicolarioDialog.this.application.openHelpDialog(DEVICE_NAME, "HelpInfo.html");
				}
			});
			this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
				@Override
				public void mouseEnter(MouseEvent evt) {
					log.log(Level.FINER, "dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
					fadeOutAplhaBlending(evt, PicolarioDialog.this.getDialogShell().getClientArea(), 10, 10, 10, 15);
				}
				@Override
				public void mouseHover(MouseEvent evt) {
					log.log(Level.FINEST, "dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
				}
				@Override
				public void mouseExit(MouseEvent evt) {
					log.log(Level.FINER, "dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
					fadeInAlpaBlending(evt, PicolarioDialog.this.getDialogShell().getClientArea(), 10, 10, -10, 15);
				}
			});

			{ // group 1
				this.numberAvailableRecorsSetsGroup1 = new Group(this.dialogShell, SWT.NONE);
				this.numberAvailableRecorsSetsGroup1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.numberAvailableRecorsSetsGroup1.setLayout(new GridLayout(2, false));
				this.numberAvailableRecorsSetsGroup1.setText(Messages.getString(MessageIds.GDE_MSGT1200));
				this.numberAvailableRecorsSetsGroup1.setBounds(10, 5, 320, 55);
				this.numberAvailableRecorsSetsGroup1.addMouseTrackListener(PicolarioDialog.this.mouseTrackerEnterFadeOut);
				{
					this.queryAvailableRecordSetButton = new Button(this.numberAvailableRecorsSetsGroup1, SWT.PUSH | SWT.CENTER);
					this.queryAvailableRecordSetButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.queryAvailableRecordSetButton.setText(Messages.getString(MessageIds.GDE_MSGT1201));
					this.queryAvailableRecordSetButton.setLayoutData(new GridData(250, 25));
					this.queryAvailableRecordSetButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "anzahlAufzeichnungenButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							try {
								if (PicolarioDialog.this.serialPort != null) {
									PicolarioDialog.this.setClosePossible(false);
									int availableRecords = PicolarioDialog.this.serialPort.readNumberAvailableRecordSets();
									PicolarioDialog.this.numberAvailable = Integer.valueOf(availableRecords).toString();
									PicolarioDialog.this.numberAvailableRecordSetsLabel.setText(PicolarioDialog.this.numberAvailable);
									setRecordSetSelection(availableRecords, 0);
									PicolarioDialog.this.readSingle.setEnabled(true);
									PicolarioDialog.this.readAllRecords.setEnabled(true);
									resetTelegramLabel();
									resetDataSetsLabel();
									PicolarioDialog.this.setClosePossible(true);
								}
							}
							catch (Exception e) {
								PicolarioDialog.this.setClosePossible(true);
								PicolarioDialog.this.serialPort.close();
								PicolarioDialog.this.application.openMessageDialog(PicolarioDialog.this.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0024, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
								PicolarioDialog.this.application.getDeviceSelectionDialog().open();
							}
						}
					});
				}
				{
					this.numberAvailableRecordSetsLabel = new CLabel(this.numberAvailableRecorsSetsGroup1, SWT.RIGHT | SWT.BORDER);
					this.numberAvailableRecordSetsLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.numberAvailableRecordSetsLabel.setBackground(DataExplorer.getInstance().COLOR_WHITE);
					this.numberAvailableRecordSetsLabel.setLayoutData(new GridData(30, 24));
					this.numberAvailableRecordSetsLabel.setText(this.numberAvailable);
				}
				this.numberAvailableRecorsSetsGroup1.layout();
			} // end group1

			{ // config tab 2
				this.configTabFolder = new CTabFolder(this.dialogShell, SWT.BORDER);

				if (this.device.getChannelCount() > 0) {
					this.configTabItem1 = new CTabItem(this.configTabFolder, SWT.NONE);
					this.configTabItem1.setText(this.device.getChannelNameReplacement(1));
					this.configTab1 = new PicolarioConfigTab(this.configTabFolder, this.device, this.device.getChannelNameReplacement(1));
					this.configTabItem1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.configTabItem1.setControl(this.configTab1);
				}
				if (this.device.getChannelCount() > 1) {
					this.configTabItem2 = new CTabItem(this.configTabFolder, SWT.NONE);
					this.configTabItem2.setText(this.device.getChannelNameReplacement(2));
					this.configTab2 = new PicolarioConfigTab(this.configTabFolder, this.device, this.device.getChannelNameReplacement(2));
					this.configTabItem2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.configTabItem2.setControl(this.configTab2);
				}

				this.configTabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
				this.configTabFolder.setBounds(10, 65, 320, 240);
				this.configTabFolder.addMouseTrackListener(PicolarioDialog.this.mouseTrackerEnterFadeOut);
				this.configTabFolder.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "configTabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
						int channelNumber = PicolarioDialog.this.configTabFolder.getSelectionIndex() + 1;
						String configKey = channelNumber + " : " + ((CTabItem) evt.item).getText();
						Channels channels = Channels.getInstance();
						Channel activeChannel = channels.getActiveChannel();
						if (activeChannel != null) {
							log.log(Level.FINE, "activeChannel = " + activeChannel.getName() + " configKey = " + configKey); //$NON-NLS-1$ //$NON-NLS-2$
							RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
							if (activeRecordSet != null && !activeChannel.getName().trim().equals(configKey)) {
								int answer = PicolarioDialog.this.application.openYesNoMessageDialog(PicolarioDialog.this.getDialogShell(), Messages.getString(MessageIds.GDE_MSGT1202));
								if (answer == SWT.YES) {
									String recordSetKey = activeRecordSet.getName();
									log.log(Level.FINE, "move record set " + recordSetKey + " to configuration " + configKey); //$NON-NLS-1$ //$NON-NLS-2$
									channels.get(channelNumber).put(recordSetKey, activeRecordSet.clone(channelNumber));
									activeChannel.remove(recordSetKey);
									channels.switchChannel(channelNumber, recordSetKey);
									PicolarioDialog.this.getDialogShell().redraw();
								}
							}
						}
					}
				});
			} // config tab 2

			{ // group 3
				this.readDataGroup3 = new Group(this.dialogShell, SWT.NONE);
				this.readDataGroup3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.readDataGroup3.setLayout(null);
				this.readDataGroup3.setText(Messages.getString(MessageIds.GDE_MSGT1203));
				this.readDataGroup3.setBounds(10, 310, 320, 200);
				this.readDataGroup3.addMouseTrackListener(PicolarioDialog.this.mouseTrackerEnterFadeOut);
				{
					this.switchRecordSetButton = new Button(this.readDataGroup3, SWT.CHECK | SWT.CENTER);
					this.switchRecordSetButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.switchRecordSetButton.setBounds(15, GDE.IS_MAC_COCOA ? 5 : 20, 290, 17);
					this.switchRecordSetButton.setText(Messages.getString(MessageIds.GDE_MSGT1204));
					this.switchRecordSetButton.setSelection(this.doSwtichRecordSet);
					this.switchRecordSetButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "switchRecordSetButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							PicolarioDialog.this.doSwtichRecordSet = PicolarioDialog.this.switchRecordSetButton.getSelection();
						}
					});
				}
				{
					this.readSingle = new Button(this.readDataGroup3, SWT.PUSH | SWT.CENTER);
					this.readSingle.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.readSingle.setText(Messages.getString(MessageIds.GDE_MSGT1205));
					this.readSingle.setBounds(10, GDE.IS_MAC_COCOA ? 30 : 45, 240, 25);
					this.readSingle.setEnabled(false);
					this.readSingle.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "ausleseButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							PicolarioDialog.this.setClosePossible(false);
							PicolarioDialog.this.queryAvailableRecordSetButton.setEnabled(false);
							PicolarioDialog.this.readSingle.setEnabled(false);
							PicolarioDialog.this.readAllRecords.setEnabled(false);
							PicolarioDialog.this.stopButton.setEnabled(true);
							PicolarioDialog.this.gatherThread = new DataGathererThread(PicolarioDialog.this.application, PicolarioDialog.this.device, PicolarioDialog.this.serialPort,
									new String[] { PicolarioDialog.this.recordSetSelectCombo.getText() });
							try {
								PicolarioDialog.this.gatherThread.start();
							}
							catch (RuntimeException e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
							log.log(Level.FINE, "gatherThread.run() - executing"); //$NON-NLS-1$
						} // end widget selected
					}); // end selection adapter
				}
				{
					this.recordSetSelectCombo = new CCombo(this.readDataGroup3, SWT.BORDER | SWT.RIGHT);
					this.recordSetSelectCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.recordSetSelectCombo.setItems(this.numberAvailable.length() >= 1 ? StringHelper.int2Array(Integer.parseInt(this.numberAvailable)) : new String[]{"0"}); //$NON-NLS-1$
					this.recordSetSelectCombo.setBounds(260, GDE.IS_MAC_COCOA ? 32 : 47, 45, GDE.IS_LINUX ? 22 : 20);
				}
				{
					this.numberRedTelegramLabel = new CLabel(this.readDataGroup3, SWT.RIGHT);
					this.numberRedTelegramLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.numberRedTelegramLabel.setBounds(10, GDE.IS_MAC_COCOA ? 60 : 75, 234, 24);
					this.numberRedTelegramLabel.setText(Messages.getString(MessageIds.GDE_MSGT1206));
					this.numberRedTelegramLabel.setForeground(SWTResourceManager.getColor(64, 128, 128));
				}
				{
					this.alreadyRedLabel = new CLabel(this.readDataGroup3, SWT.RIGHT);
					this.alreadyRedLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alreadyRedLabel.setBounds(244, GDE.IS_MAC_COCOA ? 60 : 75, 56, 24);
					this.alreadyRedLabel.setText(this.redDatagrams);
				}
				{
					this.readAllRecords = new Button(this.readDataGroup3, SWT.PUSH | SWT.CENTER);
					this.readAllRecords.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.readAllRecords.setBounds(8, GDE.IS_MAC_COCOA ? 85 : 100, 300, 25);
					this.readAllRecords.setText(Messages.getString(MessageIds.GDE_MSGT1207));
					this.readAllRecords.setEnabled(false);
					this.readAllRecords.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "readAllRecords.widgetSelected, event=" + evt); //$NON-NLS-1$
							PicolarioDialog.this.setClosePossible(false);
							PicolarioDialog.this.queryAvailableRecordSetButton.setEnabled(false);
							PicolarioDialog.this.readAllRecords.setEnabled(false);
							PicolarioDialog.this.readSingle.setEnabled(false);
							PicolarioDialog.this.stopButton.setEnabled(true);
							String[] itemNames = PicolarioDialog.this.recordSetSelectCombo.getItems();
							PicolarioDialog.this.gatherThread = new DataGathererThread(PicolarioDialog.this.application, PicolarioDialog.this.device, PicolarioDialog.this.serialPort, itemNames);
							try {
								PicolarioDialog.this.gatherThread.start();
								log.log(Level.FINE, "gatherThread.run() - executing"); //$NON-NLS-1$
							}
							catch (RuntimeException e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					});
				}
				{
					this.alreadyRedDataSetsLabel = new CLabel(this.readDataGroup3, SWT.RIGHT);
					this.alreadyRedDataSetsLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alreadyRedDataSetsLabel.setBounds(10, GDE.IS_MAC_COCOA ? 110 : 125, 234, 24);
					this.alreadyRedDataSetsLabel.setForeground(SWTResourceManager.getColor(64, 128, 128));
					this.alreadyRedDataSetsLabel.setText(Messages.getString(MessageIds.GDE_MSGT1208));
				}
				{
					this.redDataSets = new CLabel(this.readDataGroup3, SWT.RIGHT);
					this.redDataSets.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.redDataSets.setBounds(244, GDE.IS_MAC_COCOA ? 110 : 125, 56, 24);
					this.redDataSets.setText(this.redDataSetsText);
				}
				{
					this.stopButton = new Button(this.readDataGroup3, SWT.PUSH | SWT.CENTER);
					this.stopButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.stopButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0278));
					this.stopButton.setEnabled(false);
					this.stopButton.setBounds(80, GDE.IS_MAC_COCOA ? 140 : 155, 150, 25);
					this.stopButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "stopButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							PicolarioDialog.this.gatherThread.setThreadStop(true);
							PicolarioDialog.this.setClosePossible(true);
						}
					});
				}
			} // end group 3

			{
				this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0188));
				this.closeButton.setBounds(70, 520, 200, 25);
				this.closeButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINE, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						dispose();
					}
				});
			}

			this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x/2-175, 100));
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

	public void setAvailableRecordSets(int number) {
		this.numberAvailableRecordSetsLabel.setText(Integer.valueOf(number).toString());
	}

	/**
	 * method to enable setting of selectable values for record set selection
	 * @param items
	 * @param index
	 */
	public void setRecordSetSelection(int items, int index) {
		this.recordSetSelectCombo.setItems(StringHelper.int2Array(items));
		this.recordSetSelectCombo.select(index);
		this.recordSetSelectCombo.setVisibleItemCount(items);
	}

	/**
	 * function to reset counter labels fro red and calculated
	 */
	public void resetTelegramLabel() {
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (!PicolarioDialog.this.application.getDeviceDialog().isDisposed()) {
					if (!PicolarioDialog.this.application.getDeviceDialog().isDisposed()) {
						PicolarioDialog.this.redDatagrams = "0"; //$NON-NLS-1$
						PicolarioDialog.this.alreadyRedLabel.setText(PicolarioDialog.this.redDatagrams);
					}
				}
			}
		});
	}

	/**
	 * use this method to set displayed text during data gathering
	 * @param newValue
	 */
	public void setAlreadyRedText(final int newValue) {
		this.redDatagrams = Integer.valueOf(newValue).toString();
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (!PicolarioDialog.this.application.getDeviceDialog().isDisposed()) PicolarioDialog.this.alreadyRedLabel.setText(PicolarioDialog.this.redDatagrams);
			}
		});
	}

	/**
	 * function to reset counter labels fro red and calculated
	 */
	public void resetDataSetsLabel() {
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (!PicolarioDialog.this.application.getDeviceDialog().isDisposed()) {
					PicolarioDialog.this.redDataSetsText = "0"; //$NON-NLS-1$
					PicolarioDialog.this.redDataSets.setText(PicolarioDialog.this.redDataSetsText);
				}
			}
		});
	}

	/**
	 * use this method to set displayed text during data gathering
	 * @param newValue
	 */
	public void setAlreadyRedDataSets(final String newValue) {
		this.redDataSetsText = newValue;
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (!PicolarioDialog.this.application.getDeviceDialog().isDisposed()) {
					PicolarioDialog.this.redDataSets.setText(newValue);
				}
			}
		});
	}

	/**
	 * function to enable all the read data read buttons, normally called after data gathering finished
	 */
	public void enableReadButtons() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.queryAvailableRecordSetButton.setEnabled(true);
			this.readSingle.setEnabled(true);
			this.readAllRecords.setEnabled(true);
			this.stopButton.setEnabled(false);
			this.isClosePossible = true;
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!PicolarioDialog.this.application.getDeviceDialog().isDisposed()) {
						PicolarioDialog.this.queryAvailableRecordSetButton.setEnabled(true);
						PicolarioDialog.this.readSingle.setEnabled(true);
						PicolarioDialog.this.readAllRecords.setEnabled(true);
						PicolarioDialog.this.stopButton.setEnabled(false);
						PicolarioDialog.this.setClosePossible(true);
					}
				}
			});
		}
	}

	/**
	 * @return the doSwtichRecordSet
	 */
	public boolean isDoSwtichRecordSet() {
		return this.doSwtichRecordSet;
	}

	/**
	 * reset the button states to default
	 */
	public void resetButtons() {
		if (this.dialogShell != null && !this.dialogShell.isDisposed()) {
			if (Thread.currentThread().getId() == this.application.getThreadId()) {
				PicolarioDialog.this.setClosePossible(true);
				PicolarioDialog.this.queryAvailableRecordSetButton.setEnabled(true);
				PicolarioDialog.this.readSingle.setEnabled(true);
				PicolarioDialog.this.readAllRecords.setEnabled(true);
				PicolarioDialog.this.stopButton.setEnabled(false);
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						PicolarioDialog.this.setClosePossible(true);
						PicolarioDialog.this.queryAvailableRecordSetButton.setEnabled(true);
						PicolarioDialog.this.readSingle.setEnabled(true);
						PicolarioDialog.this.readAllRecords.setEnabled(true);
						PicolarioDialog.this.stopButton.setEnabled(false);
					}
				});
			}
		}
	}

	/**
	 * @return the queryAvailableRecordSetButton
	 */
	public synchronized Button getQueryAvailableRecordSetButton() {
		return queryAvailableRecordSetButton;
	}

	/**
	 * @return the readAllRecords
	 */
	public synchronized Button getReadAllRecords() {
		return readAllRecords;
	}
}
