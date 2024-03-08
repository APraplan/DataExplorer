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
package gde.device.htronic;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channels;
import gde.device.DeviceDialog;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

import java.util.HashMap;
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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog class for device AkkuMaster C4
 * @author Winfried Brügmann
 */
public class AkkuMasterC4Dialog extends DeviceDialog {
	final static Logger						log										= Logger.getLogger(AkkuMasterC4Dialog.class.getName());
	static final String						DEVICE_NAME						= "Akkumaster C4";
	static final String						AKKU_MASTER_HELP_DIR	= "AkkuMaster";

	CLabel												totalChargeCurrentText,	totalDischargeCurrentText;
	CLabel												totalChargeCurrentLabel, totalDischargeCurrentLabel, totalChargeCurrentUnit, totalDischargeCurrentUnit;
	Composite											statusComposite;
	CLabel												versionFrontplateTypeLabel, versionCurrentTypeLabel, versionDateLabel, versionNumberLabel;
	CLabel												versionFrontplateTypeText,versionCurrentTypeText, versionDateText, versionNumberText;
	Composite											versionComposite;
	CTabItem											versionTabItem;
	CTabFolder										tabFolder;
	Button												closeButton;

	final Settings								settings;
	final AkkuMasterC4						device;
	final AkkuMasterC4SerialPort	serialPort;
	final Channels								channels;
	AkkuMasterChannelTab					channel1Tab, channel2Tab, channel3Tab, channel4Tab;

	int														totalDischargeCurrent	= 0000;			// mA
	int														totalChargeCurrent		= 0000;			// mA
	HashMap<String, String>				version;
	Thread												versionThread;
	final int											numberChannels;
	final int											maxCurrent						= 2000;			// [mA]
	
	boolean 											isWarnedConnectError = false;

	/**
	 * constructor initialize all variables required
	 * @param parent Shell
	 * @param actualDevice AkkuMasterC4 class implementation == IDevice
	 */
	public AkkuMasterC4Dialog(Shell parent, AkkuMasterC4 actualDevice) {
		super(parent);
		this.device = actualDevice;
		this.serialPort = actualDevice.getCommunicationPort();
		this.channels = Channels.getInstance();
		this.numberChannels = actualDevice.getChannelCount();
		this.settings = Settings.getInstance();
	}

	@Override
	public void open() {
		this.shellAlpha = Settings.getInstance().getDialogAlphaValue(); 
		this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

		AkkuMasterC4Dialog.log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
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
			this.dialogShell.setSize(440, 555);
			this.dialogShell.setText(DEVICE_NAME + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
			this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif"));
			this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
				@Override
				public void mouseEnter(MouseEvent evt) {
					log.log(Level.FINER, "dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
					fadeOutAplhaBlending(evt, AkkuMasterC4Dialog.this.getDialogShell().getClientArea(), 10, 10, 10, 15);
				}
				@Override
				public void mouseHover(MouseEvent evt) {
					log.log(Level.FINEST, "dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
				}
				@Override
				public void mouseExit(MouseEvent evt) {
					log.log(Level.FINER, "dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
					fadeInAlpaBlending(evt, AkkuMasterC4Dialog.this.getDialogShell().getClientArea(), 10, 10, -10, 15);
				}
			});
			{
				this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
				this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.tabFolder.setBounds(0, 0, 430, 425);
				this.tabFolder.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						AkkuMasterC4Dialog.log.log(Level.FINEST, "tabFolder.widgetDisposed, event=" + evt); //$NON-NLS-1$
						AkkuMasterC4Dialog.this.statusComposite.redraw();
					}
				});

				@SuppressWarnings("nls")
				String[] aCapacity = new String[] { "100", "250", "500", "600", "800", "1000", "1250", "1500", "1750", "2000", "2500", "3000", "4000", "5000" };
				String[] aCellCount = new String[] { "1"+Messages.getString(MessageIds.GDE_MSGT1185), "2"+Messages.getString(MessageIds.GDE_MSGT1148), "3"+Messages.getString(MessageIds.GDE_MSGT1148), "4"+Messages.getString(MessageIds.GDE_MSGT1148), "5"+Messages.getString(MessageIds.GDE_MSGT1148), 
						"6"+Messages.getString(MessageIds.GDE_MSGT1148), "7"+Messages.getString(MessageIds.GDE_MSGT1148), "8"+Messages.getString(MessageIds.GDE_MSGT1148), "9"+Messages.getString(MessageIds.GDE_MSGT1148), "10"+Messages.getString(MessageIds.GDE_MSGT1148), 
						"11"+Messages.getString(MessageIds.GDE_MSGT1148), "12"+Messages.getString(MessageIds.GDE_MSGT1148),	"13"+Messages.getString(MessageIds.GDE_MSGT1148), "14"+Messages.getString(MessageIds.GDE_MSGT1148) };
				String[] aAkkuType = new String[] { "0 NiCa", "1 NiMh", "2 Pb" };
				String[] aProgramm = new String[] { Messages.getString(MessageIds.GDE_MSGT1100), Messages.getString(MessageIds.GDE_MSGT1101), Messages.getString(MessageIds.GDE_MSGT1102), Messages.getString(MessageIds.GDE_MSGT1103), Messages.getString(MessageIds.GDE_MSGT1104), Messages.getString(MessageIds.GDE_MSGT1105), Messages.getString(MessageIds.GDE_MSGT1106), Messages.getString(MessageIds.GDE_MSGT1107),
						Messages.getString(MessageIds.GDE_MSGT1108) }; 
				String[] aChargeCurrent_mA = new String[] { "50", "100", "150", "200", "250", "300", "400", "500", "750", "900", "1000", "1500", "2000" };
				String[] aDischargeCurrent_mA = aChargeCurrent_mA;

				///////////////////////////////////////////////////				
				if (this.numberChannels > 0) {
					this.channel1Tab = new AkkuMasterChannelTab(this,
							(" 1 " + this.device.getChannelNameReplacement(1)), AkkuMasterC4SerialPort.channel_1, AkkuMasterC4Dialog.this.serialPort, this.channels.get(1), aCapacity, aCellCount, //$NON-NLS-1$
							aAkkuType, aProgramm, aChargeCurrent_mA, aDischargeCurrent_mA);
					this.channel1Tab.addChannelTab(this.tabFolder);
				}
				if (this.numberChannels > 1) {
					this.channel2Tab = new AkkuMasterChannelTab(this,
							(" 2 " + this.device.getChannelNameReplacement(2)), AkkuMasterC4SerialPort.channel_2, AkkuMasterC4Dialog.this.serialPort, this.channels.get(2), aCapacity, aCellCount, //$NON-NLS-1$
							aAkkuType, aProgramm, aChargeCurrent_mA, aDischargeCurrent_mA);
					this.channel2Tab.addChannelTab(this.tabFolder);
				}
				if (this.numberChannels > 2) {
					this.channel3Tab = new AkkuMasterChannelTab(this,
							(" 3 " + this.device.getChannelNameReplacement(3)), AkkuMasterC4SerialPort.channel_3, AkkuMasterC4Dialog.this.serialPort, this.channels.get(3), aCapacity, aCellCount, //$NON-NLS-1$
							aAkkuType, aProgramm, aChargeCurrent_mA, aDischargeCurrent_mA);
					this.channel3Tab.addChannelTab(this.tabFolder);
				}
				if (this.numberChannels > 3) {
					this.channel4Tab = new AkkuMasterChannelTab(this,
							(" 4 " + this.device.getChannelNameReplacement(4)), AkkuMasterC4SerialPort.channel_4, AkkuMasterC4Dialog.this.serialPort, this.channels.get(4), aCapacity, aCellCount, //$NON-NLS-1$
							aAkkuType, aProgramm, aChargeCurrent_mA, aDischargeCurrent_mA);
					this.channel4Tab.addChannelTab(this.tabFolder);
				}
				///////////////////////////////////////////////////		

				{
					this.versionTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					this.versionTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.versionTabItem.setText(Messages.getString(MessageIds.GDE_MSGT1109));
					{
						this.versionComposite = new Composite(this.tabFolder, SWT.NONE);
						this.versionComposite.setLayout(null);
						this.versionTabItem.setControl(this.versionComposite);
						this.versionComposite.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								AkkuMasterC4Dialog.log.log(Level.FINEST, "versionComposite.paintControl, event=" + evt); //$NON-NLS-1$
								if (AkkuMasterC4Dialog.this.version != null) {
									updateVersionText(
											String.format(":    %s", AkkuMasterC4Dialog.this.version.get(AkkuMasterC4SerialPort.VERSION_NUMBER)), //$NON-NLS-1$
											String.format(":    %s", AkkuMasterC4Dialog.this.version.get(AkkuMasterC4SerialPort.VERSION_DATE)), //$NON-NLS-1$
											String.format(":    %s", AkkuMasterC4Dialog.this.version.get(AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT)), //$NON-NLS-1$
											String.format(":    %s", AkkuMasterC4Dialog.this.version.get(AkkuMasterC4SerialPort.VERSION_TYPE_FRONT))); //$NON-NLS-1$

								}
								else {
									updateVersionText(
											String.format(":    %s", Messages.getString(gde.messages.MessageIds.GDE_MSGT0276)), //$NON-NLS-1$
											String.format(":    %s", Messages.getString(gde.messages.MessageIds.GDE_MSGT0276)), //$NON-NLS-1$
											String.format(":    %s", Messages.getString(gde.messages.MessageIds.GDE_MSGT0276)), //$NON-NLS-1$
											String.format(":    %s", Messages.getString(gde.messages.MessageIds.GDE_MSGT0276))); //$NON-NLS-1$
									startUpdateVersionThread();
								}
							}
						});
						{
							this.versionNumberLabel = new CLabel(this.versionComposite, SWT.LEFT);
							this.versionNumberLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionNumberLabel.setBounds(25, 60, 150, 20);
							this.versionNumberLabel.setText(Messages.getString(MessageIds.GDE_MSGT1122));
						}
						{
							this.versionNumberText = new CLabel(this.versionComposite, SWT.NONE);
							this.versionNumberText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionNumberText.setBounds(230, 63, 100, 20);
						}
						{
							this.versionDateLabel = new CLabel(this.versionComposite, SWT.LEFT);
							this.versionDateLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionDateLabel.setBounds(25, 110, 150, 20);
							this.versionDateLabel.setText(Messages.getString(MessageIds.GDE_MSGT1123));
						}
						{
							this.versionDateText = new CLabel(this.versionComposite, SWT.NONE);
							this.versionDateText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionDateText.setBounds(230, 113, 100, 20);
						}
						{
							this.versionCurrentTypeLabel = new CLabel(this.versionComposite, SWT.LEFT);
							this.versionCurrentTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionCurrentTypeLabel.setBounds(25, 160, 150, 20);
							this.versionCurrentTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT1124));
						}
						{
							this.versionCurrentTypeText = new CLabel(this.versionComposite, SWT.NONE);
							this.versionCurrentTypeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionCurrentTypeText.setBounds(230, 163, 100, 20);
						}
						{
							this.versionFrontplateTypeLabel = new CLabel(this.versionComposite, SWT.LEFT);
							this.versionFrontplateTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionFrontplateTypeLabel.setBounds(25, 210, 150, 20);
							this.versionFrontplateTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT1125));
						}
						{
							this.versionFrontplateTypeText = new CLabel(this.versionComposite, SWT.NONE);
							this.versionFrontplateTypeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionFrontplateTypeText.setBounds(230, 213, 100, 20);
						}
					}
				}
				try {
					this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
				}
				catch (RuntimeException e) {
					this.tabFolder.setSelection(0);
				}
			}
			{
				this.statusComposite = new Composite(this.dialogShell, SWT.NONE);
				this.statusComposite.setLayout(null);
				this.statusComposite.setBounds(0, 430, 430, 45);
				this.statusComposite.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						AkkuMasterC4Dialog.log.log(Level.FINE, "statusComposite.paintCOntrol, event=" + evt); //$NON-NLS-1$
						AkkuMasterC4Dialog.this.totalDischargeCurrentText.setText(String.format("%4d", AkkuMasterC4Dialog.this.totalDischargeCurrent));
						AkkuMasterC4Dialog.this.totalChargeCurrentText.setText(String.format("%4d", AkkuMasterC4Dialog.this.totalChargeCurrent));
					}
				});
				this.statusComposite.addFocusListener(new FocusAdapter() {
					@Override
					public void focusGained(FocusEvent evt) {
						AkkuMasterC4Dialog.log.log(Level.WARNING, "statusComposite.focusGained, event=" + evt); //$NON-NLS-1$
						AkkuMasterC4Dialog.this.totalDischargeCurrentText.setText(String.format("%4d", AkkuMasterC4Dialog.this.totalDischargeCurrent));
						AkkuMasterC4Dialog.this.totalChargeCurrentText.setText(String.format("%4d", AkkuMasterC4Dialog.this.totalChargeCurrent));
					}
				});
				{
					this.totalChargeCurrentLabel = new CLabel(this.statusComposite, SWT.RIGHT);
					this.totalChargeCurrentLabel.setText(Messages.getString(MessageIds.GDE_MSGT1110));
					this.totalChargeCurrentLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.totalChargeCurrentLabel.setBounds(20, 3, 190, 20);
				}
				{
					this.totalChargeCurrentText = new CLabel(this.statusComposite, SWT.RIGHT | SWT.EMBEDDED);
					this.totalChargeCurrentText.setBounds(235, 3, 50, 20);
					this.totalChargeCurrentText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.totalChargeCurrentText.setText(String.format("%4d", this.totalChargeCurrent));
				}
				{
					this.totalChargeCurrentUnit = new CLabel(this.statusComposite, SWT.LEFT);
					this.totalChargeCurrentUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.totalChargeCurrentUnit.setText(Messages.getString(MessageIds.GDE_MSGT1113));
					this.totalChargeCurrentUnit.setBounds(300, 3, 119, 20);
				}
				{
					this.totalDischargeCurrentLabel = new CLabel(this.statusComposite, SWT.RIGHT);
					this.totalDischargeCurrentLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.totalDischargeCurrentLabel.setText(Messages.getString(MessageIds.GDE_MSGT1111));
					this.totalDischargeCurrentLabel.setBounds(20, 25, 190, 20);
				}
				{
					this.totalDischargeCurrentText = new CLabel(this.statusComposite, SWT.RIGHT | SWT.EMBEDDED);
					this.totalDischargeCurrentText.setBounds(235, 25, 50, 20);
					this.totalDischargeCurrentText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.totalDischargeCurrentText.setText(String.format("%4d", this.totalDischargeCurrent));
				}
				{
					this.totalDischargeCurrentUnit = new CLabel(this.statusComposite, SWT.LEFT);
					this.totalDischargeCurrentUnit.setText(Messages.getString(MessageIds.GDE_MSGT1112));
					this.totalDischargeCurrentUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.totalDischargeCurrentUnit.setBounds(300, 25, 119, 20);
				}
			}
			{
				this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0188));
				this.closeButton.setBounds(87, 485, 260, 30);
				this.closeButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						AkkuMasterC4Dialog.log.log(Level.FINEST, "closeButton.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (getChannelTab(1).isDataColletionActive() || getChannelTab(2).isDataColletionActive() || getChannelTab(3).isDataColletionActive() || getChannelTab(4).isDataColletionActive())
							setClosePossible(false);
						else
							setClosePossible(true);

						dispose();
					}
				});
			}
			this.dialogShell.getShell().addListener (SWT.Close, new Listener() {			
				public void handleEvent (Event evt) {		
					AkkuMasterC4Dialog.log.log(Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					if (!(getChannelTab(1).isDataColletionActive() || getChannelTab(2).isDataColletionActive() || getChannelTab(3).isDataColletionActive() || getChannelTab(4).isDataColletionActive())) {
						//all gatherer thread are stopped
						if (AkkuMasterC4Dialog.this.serialPort != null && AkkuMasterC4Dialog.this.serialPort.isConnected()) {
							AkkuMasterC4Dialog.this.serialPort.close();
						}

						Thread thread = AkkuMasterC4Dialog.this.versionThread;
						if (thread != null && thread.isAlive()) thread.interrupt();
						AkkuMasterC4Dialog.this.dispose();
					}
					else {
						evt.doit = false;
					}
				}
			});
			this.dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					if (AkkuMasterC4Dialog.this.isDataColletionActive()) {
						AkkuMasterC4Dialog.this.channel1Tab.stopTimer();
						AkkuMasterC4Dialog.this.channel2Tab.stopTimer();
						AkkuMasterC4Dialog.this.channel3Tab.stopTimer();
						AkkuMasterC4Dialog.this.channel4Tab.stopTimer();
					}
					AkkuMasterC4Dialog.this.serialPort.close();
					AkkuMasterC4Dialog.this.dispose();
				}
			});
			this.dialogShell.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					AkkuMasterC4Dialog.log.log(Level.FINEST, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
					AkkuMasterC4Dialog.this.application.openHelpDialog(AKKU_MASTER_HELP_DIR, "HelpInfo.html"); //$NON-NLS-1$
				}
			});
			this.dialogShell.addListener(SWT.Traverse, new Listener() {
				public void handleEvent(Event event) {
					switch (event.detail) {
					case SWT.TRAVERSE_ESCAPE:
						AkkuMasterC4Dialog.this.dialogShell.close();
						event.detail = SWT.TRAVERSE_NONE;
						event.doit = false;
						break;
					}
				}
			});
			this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x/2-220, 70));
			this.dialogShell.open();
			startUpdateVersionThread();
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

	@Override
	public void close() {
		if (this.dialogShell != null && !this.dialogShell.isDisposed()) {
			this.channel1Tab.stopTimer();
			this.channel2Tab.stopTimer();
			this.channel3Tab.stopTimer();
			this.channel4Tab.stopTimer();
			this.dialogShell.dispose();
		}
	}

	public boolean isDataColletionActive() {
		return this.channel1Tab.isCollectData() && this.channel1Tab.isCollectDataStopped() && this.channel2Tab.isCollectData() && this.channel2Tab.isCollectDataStopped()
				&& this.channel3Tab.isCollectData() && this.channel3Tab.isCollectDataStopped() && this.channel4Tab.isCollectData() && this.channel4Tab.isCollectDataStopped();
	}

	/**
	 * add current to discharge current sum
	 * @param newDeltaDischargeCurrent
	 */
	public void addTotalDischargeCurrent(int newDeltaDischargeCurrent) {
		this.totalDischargeCurrent = this.totalDischargeCurrent + newDeltaDischargeCurrent;
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				AkkuMasterC4Dialog.this.totalDischargeCurrentText.setText(String.format("%4d", AkkuMasterC4Dialog.this.totalDischargeCurrent));
				AkkuMasterC4Dialog.this.totalDischargeCurrentText.update();
				AkkuMasterC4Dialog.this.statusComposite.redraw();
				log.log(Level.FINE, "charge = " + AkkuMasterC4Dialog.this.totalChargeCurrent + ", discharge = " + AkkuMasterC4Dialog.this.totalDischargeCurrent);
			}
		});
	}

	/**
	 * subtract current to discharge current sum
	 * @param newDeltaDischargeCurrent
	 */
	public void subtractTotalDischargeCurrent(int newDeltaDischargeCurrent) {
		this.totalDischargeCurrent = this.totalDischargeCurrent - newDeltaDischargeCurrent;
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				AkkuMasterC4Dialog.this.totalDischargeCurrentText.setText(String.format("%4d", AkkuMasterC4Dialog.this.totalDischargeCurrent));
				AkkuMasterC4Dialog.this.totalDischargeCurrentText.update();
				AkkuMasterC4Dialog.this.statusComposite.redraw();
				log.log(Level.FINE, "charge = " + AkkuMasterC4Dialog.this.totalChargeCurrent + ", discharge = " + AkkuMasterC4Dialog.this.totalDischargeCurrent);
			}
		});
	}

	/**
	 * add current to charge current sum
	 * @param newDeltaChargeCurrent
	 */
	public void addTotalChargeCurrent(int newDeltaChargeCurrent) {
		this.totalChargeCurrent = this.totalChargeCurrent + newDeltaChargeCurrent;
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				AkkuMasterC4Dialog.this.totalChargeCurrentText.setText(String.format("%4d", AkkuMasterC4Dialog.this.totalChargeCurrent));
				AkkuMasterC4Dialog.this.totalChargeCurrentText.update();
				AkkuMasterC4Dialog.this.statusComposite.redraw();
				log.log(Level.FINE, "charge = " + AkkuMasterC4Dialog.this.totalChargeCurrent + ", discharge = " + AkkuMasterC4Dialog.this.totalDischargeCurrent);
			}
		});
	}

	/**
	 * subtract current to charge current sum
	 * @param newDeltaChargeCurrent
	 */
	public void subtractTotalChargeCurrent(int newDeltaChargeCurrent) {
		this.totalChargeCurrent = this.totalChargeCurrent - newDeltaChargeCurrent;
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				AkkuMasterC4Dialog.this.totalChargeCurrentText.setText(String.format("%4d", AkkuMasterC4Dialog.this.totalChargeCurrent));
				AkkuMasterC4Dialog.this.totalChargeCurrentText.update();
				AkkuMasterC4Dialog.this.statusComposite.redraw();
				log.log(Level.FINE, "charge = " + AkkuMasterC4Dialog.this.totalChargeCurrent + ", discharge = " + AkkuMasterC4Dialog.this.totalDischargeCurrent);
			}
		});
	}

	/**
	 * method to query sum of charge and discharge current in mA to enable overload check
	 */
	public int getActiveCurrent() {
		return this.totalChargeCurrent + this.totalDischargeCurrent;
	}

	public int getMaxCurrent() {
		return this.maxCurrent;
	}

	/**
	 * update version string 
	 */
	void startUpdateVersionThread() {
		try {
			if (this.serialPort != null && !this.isWarnedConnectError) {
				if (this.versionThread == null || !this.versionThread.isAlive()) {
					this.versionThread = new Thread() {
						
						@Override
						public void run() {
							try {
								if (!AkkuMasterC4Dialog.this.serialPort.isConnected()) {
									AkkuMasterC4Dialog.this.serialPort.open();
								}
								AkkuMasterC4Dialog.this.version = AkkuMasterC4Dialog.this.serialPort.getVersion();
								getDialogShell().getDisplay().asyncExec(new Runnable() {
									public void run() {
										AkkuMasterC4Dialog.this.versionComposite.redraw();
									}
								});
							}
							catch (Throwable e) {
								AkkuMasterC4Dialog.this.application.openMessageDialog(AkkuMasterC4Dialog.this.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0024, new Object[] {e.getClass().getSimpleName(), e.getMessage() } ));
								GDE.display.syncExec(new Runnable() {
									public void run() {
										AkkuMasterC4Dialog.this.dispose();
										AkkuMasterC4Dialog.this.application.getDeviceSelectionDialog().open();
									}
								});
								return;
							}
						}
					};
					try {
						this.versionThread.start();
					}
					catch (RuntimeException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}
			}
		}
		catch (Exception e) {
			AkkuMasterC4Dialog.log.log(Level.WARNING, e.getMessage(), e);
			AkkuMasterC4Dialog.this.application.openMessageDialog(AkkuMasterC4Dialog.this.dialogShell, Messages.getString(gde.messages.MessageIds.GDE_MSGE0025, new Object[] {e.getClass().getSimpleName(), e.getMessage() } ));
			this.isWarnedConnectError = true;
		}
	}

	/**
	 * @return the channel1Tab
	 */
	public AkkuMasterChannelTab getChannelTab(int number) {
		AkkuMasterChannelTab channelTab;
		switch (number) {
		case 4:
			channelTab = this.channel4Tab;
			break;
		case 3:
			channelTab = this.channel3Tab;
			break;
		case 2:
			channelTab = this.channel2Tab;
			break;
		default:
		case 1:
			channelTab = this.channel1Tab;
			break;
		}
		return channelTab;
	}

	/**
	 * update the version text displayed in version tab
	 */
	void updateVersionText(String useVersionNumberText, String useVersionDateText, String useVersionTypeText, String useVersionPanelText) {
		this.versionNumberText.setText(useVersionNumberText);
		this.versionDateText.setText(useVersionDateText);
		this.versionCurrentTypeText.setText(useVersionTypeText);
		this.versionFrontplateTypeText.setText(useVersionPanelText);
	}

	/**
	 * @return the device
	 */
	public AkkuMasterC4 getDevice() {
		return this.device;
	}
}
