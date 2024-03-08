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
package gde.device.smmodellbau;

import java.util.Locale;
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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.TaskBar;
import org.eclipse.swt.widgets.TaskItem;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DeviceDialog;
import gde.device.smmodellbau.unilog.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * UniLog device dialog class
 * @author Winfried Brügmann
 */
public class UniLogDialog extends DeviceDialog {
	static final String		DEVICE_NAME								= "UniLog";
	static final Logger						log												= Logger.getLogger(UniLogDialog.class.getName());

	public final static int				WERTESAETZE_MAX						= 25920;
	public final static String[]	TIME_INTERVAL							= { " 1/16 s  (-> 27 min)",
																															"   1/8 s  (-> 54 min)",
																															"   1/4 s  (-> 1:48 h)",
																															"   1/2 s  (-> 3:36 h)",
																															"      1 s   (-> 7:12 h)",
																															"      2 s   (-> 14:24 h)",
																															"      5 s   (-> 36 h)",
																															"    10 s   (->  3 d)",
																															"    30 s   (->  9 d)",
																															"   1 min (-> 18 d)",
																															"   2 min (-> 36 d)",
																															"   5 min (-> 90 d)"};
	public final static String[]	RX_AUTO_START_MS					= { " 1,1 ms", " 1,2 ms", " 1,3 ms", " 1,4 ms", " 1,5 ms", " 1,6 ms", " 1,7 ms", " 1,8 ms", " 1,9 ms", " Rx on" };	//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
	public final static String[]	CURRENT_SENSOR						= { " 40/80A ", "  150A ", "  400A ", "    20A " };	//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	public final static String[]	A1_MODUS_NAMES						= Messages.getString(MessageIds.GDE_MSGT1366).split(GDE.STRING_SEMICOLON);
	public final static String[]	A2_MODUS_NAMES						= Messages.getString(MessageIds.GDE_MSGT1367).split(GDE.STRING_SEMICOLON);
	public final static String[]	A3_MODUS_NAMES						= Messages.getString(MessageIds.GDE_MSGT1368).split(GDE.STRING_SEMICOLON);
	public final static String[]	A1_MODUS_NAMES_COMBO			= Messages.getString(MessageIds.GDE_MSGT1372).split(GDE.STRING_SEMICOLON);
	public final static String[]	A2_MODUS_NAMES_COMBO			= Messages.getString(MessageIds.GDE_MSGT1373).split(GDE.STRING_SEMICOLON);
	public final static String[]	A3_MODUS_NAMES_COMBO			= Messages.getString(MessageIds.GDE_MSGT1374).split(GDE.STRING_SEMICOLON);
	public final static String[]	A1_MODUS_UNITS						= Messages.getString(MessageIds.GDE_MSGT1369).split(GDE.STRING_SEMICOLON);
	public final static String[]	A2_MODUS_UNITS						= Messages.getString(MessageIds.GDE_MSGT1370).split(GDE.STRING_SEMICOLON);
	public final static String[]	A3_MODUS_UNITS						= Messages.getString(MessageIds.GDE_MSGT1371).split(GDE.STRING_SEMICOLON);

	CTabItem											configTabItem1, configTabItem2, configTabItem3, configTabItem4;
	CLabel												currentSensorLabel, a1ModusLabel, a2ModusLabel, a3ModusLabel;
	CCombo												a1ModusCombo, a2ModusCombo, a3ModusCombo;
	CCombo												sensorCurrentCombo;
	Group													sensorGroup;
	CCombo												impulseTriggerCombo;
	CCombo												timeTriggerCombo;
	Button												numberPolsButton;
	CCombo												currentTriggerCombo;
	Button												impulseTriggerButton;
	Button												timeTriggerButton;
	Button												currentTriggerButton;
	Group													autoStartGroup;
	Text													gearFactorCombo;
	CLabel												gearLabel;
	CCombo												numberProbCombo;
	CCombo												motorPoleCombo;
	Button												numberPropButton;
	Group													motorPropGroup;
	Button												storeAdjustmentsButton;

	CCombo												useConfigCombo;
	CLabel												firmwareVersionLabel;
	CLabel												firmwareText;
	CLabel												snLabel;
	CLabel												serialNumberText;
	CLabel												memUsageUnit;
	CLabel												memoryUsageText;
	Slider												gearRatioSlider;
	Button												stopDataButton;
	Group													liveDataCaptureGroup;
	Button												clearMemoryButton;
	Group													clearDataBufferGroup;
	Button												startLiveGatherButton;
	Button												stopLoggingButton;
	Button												startLoggingButton;
	Group													loggingGroup;
	CLabel												numberReadErrorLabel;
	CLabel												readDataErrorLabel;
	CLabel												redDataSetLabel;
	CLabel												readDataLess2Label;
	CLabel												numberLess2Label;
	CLabel												dataSetLabel;
	CLabel												actualDataSetNumberLabel;
	CLabel												actualDataSetNumber;
	ProgressBar										readDataProgressBar;
	Button												readDataButton;
	Group													dataReadGroup;
	Button												closeButton;
	Button												helpButton;
	Composite											dataMainComposite;
	CTabItem											dataTabItem;
	Group													statusGroup;
	CLabel												memUsagePercent;
	CCombo												timeIntervalCombo;
	Group													dataRateGroup;
	Composite											configMainComosite;
	Button												readAdjustmentButton;
	CTabItem											baseConfigTabItem;
	CTabFolder										deviceConfigTabFolder;
	Group													limiterGroup;
	CCombo												limiterEnergyCombo;
	Button												limiterButton;

	Group													channleConfigGroup;
	Button												stopLiveGatherButton;
	Button												editConfigButton;
	Text													memoryDeleteInfo;

	CTabItem											telemetryTabItem;
	UniLogTelemetryTab						telemetryTab;

	final Settings								settings;
	final UniLogSerialPort				serialPort;																																																																	// open/close port execute getData()....
	final UniLog									device;																																																																			// get device specific things, get serial port, ...
	UniLogDataGatherer						gatherThread;
	UniLogLiveGatherer						liveThread;
	UniLogConfigTab								configTab1, configTab2, configTab3, configTab4;
	TaskItem											taskBarItem;
	RecordSet											lastActiveRecordSet 			= null;

	String												statusText								= "";																																																							//$NON-NLS-1$

	String												serialNumber							= "";																																																							//$NON-NLS-1$
	String												unilogVersion							= "";																																																							//$NON-NLS-1$
	int														memoryUsed								= 0;
	String												memoryUsedPercent					= "0";																																																							//$NON-NLS-1$
	int														timeIntervalPosition			= -1;
	boolean												isMotorPole								= false;
	boolean												isPropBlade								= false;
	int														countMotorPole						= 0;
	int														countPropBlade						= 0;
	boolean												isAutoStartCurrent				= false;
	int														currentAutoStart					= 0;
	boolean												isAutStartRx							= false;
	boolean												isRxOn										= false;
	int														rxAutoStartValue					= 0;
	boolean												isImpulseAutoStartTime		= false;
	int														impulseAutoStartTime_sec	= 0;
	int														currentSensorPosition			= 0;
	int														modusA1Position						= 0;
	int														modusA2Position						= 0;
	int														modusA3Position						= 0;
	int 													limiterValue							= 0;
	boolean												isLimiterActive						= false;
	double												gearRatio									= 1.0;

	boolean 											isLiveGathererEnabled			= true;

	int														sliderPosition						= 50;
	String[]											configurationNames				= new String[] { " Konfig 1" };																																										//$NON-NLS-1$
	String												numberRedDataSetsText			= "0";																																																							//$NON-NLS-1$
	String												numberActualDataSetsText	= "0";																																																							//$NON-NLS-1$
	String												numberReadErrorText				= "0";																																																							//$NON-NLS-1$
	String												numberLess2Text						= "0";																																																							//$NON-NLS-1$
	int														channelSelectionIndex			= 0;

	/**
	* main method to test this dialog inside a shell
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			UniLog device = new UniLog("c:\\Documents and Settings\\user\\Application Data\\DataExplorer\\Geraete\\Htronic Akkumaster C4.ini"); //$NON-NLS-1$
			UniLogDialog inst = new UniLogDialog(shell, device);
			inst.open();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public UniLogDialog(Shell parent, UniLog useDevice) {
		super(parent);
		this.serialPort = useDevice.getCommunicationPort();
		this.device = useDevice;
		this.settings = Settings.getInstance();
		UniLogDialog.RX_AUTO_START_MS[UniLogDialog.RX_AUTO_START_MS.length - 1] = Messages.getString(MessageIds.GDE_MSGT1300);
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	@Override
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				this.dialogShell.setText(UniLogDialog.DEVICE_NAME + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);
				this.dialogShell.setLayout(null);
				this.dialogShell.layout();
				this.dialogShell.pack();
				this.dialogShell.setSize(GDE.IS_LINUX ? 655 : 645, 445);
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
		      @Override
					public void handleEvent(Event event) {
		        switch (event.detail) {
		        case SWT.TRAVERSE_ESCAPE:
		        	UniLogDialog.this.dialogShell.close();
		          event.detail = SWT.TRAVERSE_NONE;
		          event.doit = false;
		          break;
		        }
		      }
		    });
				this.dialogShell.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent evt) {
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (UniLogDialog.this.configTab1.getConfigButtonStatus() || UniLogDialog.this.configTab2.getConfigButtonStatus() || UniLogDialog.this.configTab3.getConfigButtonStatus()
								|| UniLogDialog.this.configTab4.getConfigButtonStatus()) {
							String msg = Messages.getString(MessageIds.GDE_MSGI1300);
							if (UniLogDialog.this.application.openYesNoMessageDialog(UniLogDialog.this.getDialogShell(), msg) == SWT.YES) {
								if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "SWT.YES"); //$NON-NLS-1$
								UniLogDialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}

						// check threads before close
						if (UniLogDialog.this.gatherThread != null && UniLogDialog.this.gatherThread.isAlive()) {
							UniLogDialog.this.gatherThread.interrupt();
						}
						if (UniLogDialog.this.liveThread != null) {
							if (UniLogDialog.this.liveThread.isTimerRunning) {
								UniLogDialog.this.liveThread.stopTimerThread();
							}
							if (UniLogDialog.this.liveThread.isAlive()) {
								UniLogDialog.this.liveThread.interrupt();
							}
						}
						UniLogDialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					@Override
					public void helpRequested(HelpEvent evt) {
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						int selection = UniLogDialog.this.deviceConfigTabFolder.getSelectionIndex();
						if (selection == 0)
							UniLogDialog.this.application.openHelpDialog(UniLogDialog.DEVICE_NAME, "HelpInfo.html#adjustment"); //$NON-NLS-1$
						else if (selection == UniLogDialog.this.deviceConfigTabFolder.getItemCount() - 2)
							UniLogDialog.this.application.openHelpDialog(UniLogDialog.DEVICE_NAME, "HelpInfo.html#data_io"); //$NON-NLS-1$
						else if (selection == UniLogDialog.this.deviceConfigTabFolder.getItemCount() - 1)
							UniLogDialog.this.application.openHelpDialog(UniLogDialog.DEVICE_NAME, "HelpInfo.html#telemetry"); //$NON-NLS-1$
						else
							UniLogDialog.this.application.openHelpDialog(UniLogDialog.DEVICE_NAME, "HelpInfo.html#config_tab"); //$NON-NLS-1$
					}
				});
				this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
					@Override
					public void mouseEnter(MouseEvent evt) {
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
						fadeOutAplhaBlending(evt, getDialogShell().getClientArea(), 10, 10, 10, 15);
					}
					@Override
					public void mouseHover(MouseEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
					}
					@Override
					public void mouseExit(MouseEvent evt) {
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
						fadeInAlpaBlending(evt, getDialogShell().getClientArea(), 10, 10, -10, 15);
					}
				});
				this.dialogShell.addPaintListener(new PaintListener() {
					@Override
					public void paintControl(PaintEvent paintevent) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "dialogShell.paintControl, event=" + paintevent); //$NON-NLS-1$
						RecordSet activeRecordSet = UniLogDialog.this.application.getActiveRecordSet();
						if (UniLogDialog.this.lastActiveRecordSet != null && activeRecordSet != null && !UniLogDialog.this.lastActiveRecordSet.getName().equals(activeRecordSet.getName())) {
							int index = Channels.getInstance().getActiveChannelNumber();
							index = index < 1 || index > UniLogDialog.this.deviceConfigTabFolder.getChildren().length - 2 ? 1 : index;
							UniLogDialog.this.deviceConfigTabFolder.setSelection(index);
							(UniLogDialog.this.deviceConfigTabFolder.getChildren()[index]).update();
						}
						UniLogDialog.this.lastActiveRecordSet = UniLogDialog.this.application.getActiveRecordSet();
					}
				});
				{
					this.helpButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					this.helpButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.helpButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0280));
					this.helpButton.setBounds(31, 374, 259, 30);
					this.helpButton.addMouseTrackListener(mouseTrackerEnterFadeOut);
					this.helpButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLogDialog.this.application.openHelpDialog(UniLogDialog.DEVICE_NAME, "HelpInfo.html"); //$NON-NLS-1$
						}
					});
				}
				{
					this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0188));
					this.closeButton.setBounds(342, 374, 260, 30);
					this.closeButton.addMouseTrackListener(mouseTrackerEnterFadeOut);
					this.closeButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							dispose();
						}
					});
				}

				{ // begin tabs
					this.deviceConfigTabFolder = new CTabFolder(this.dialogShell, SWT.NONE);

					{// begin device configuration tab
						this.baseConfigTabItem = new CTabItem(this.deviceConfigTabFolder, SWT.NONE);
						this.baseConfigTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.baseConfigTabItem.setText(Messages.getString(MessageIds.GDE_MSGT1301));
						{
							this.configMainComosite = new Composite(this.deviceConfigTabFolder, SWT.NONE);
							this.baseConfigTabItem.setControl(this.configMainComosite);
							this.configMainComosite.setLayout(null);
							this.configMainComosite.addMouseTrackListener(mouseTrackerEnterFadeOut);
							this.configMainComosite.addPaintListener(new PaintListener() {
								@Override
								public void paintControl(PaintEvent evt) {
									if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "configMainComosite.paintControl " + evt); //$NON-NLS-1$
									if (UniLogDialog.this.timeIntervalPosition == -1) {
										int selection = Double.valueOf(UniLogDialog.this.device.getTimeStep_ms() / 62.5).intValue();
										switch (selection) {
										case 1: // 1/16 sec
											UniLogDialog.this.timeIntervalPosition = 0;
											break;
										case 2: // 1/8 sec
											UniLogDialog.this.timeIntervalPosition = 1;
											break;
										default:
										case 4: // 1/4 sec
											UniLogDialog.this.timeIntervalPosition = 2;
											break;
										case 8: // 1/2 sec
											UniLogDialog.this.timeIntervalPosition = 3;
											break;
										case 16: // 1 sec
											UniLogDialog.this.timeIntervalPosition = 4;
											break;
										case 32: // 2 sec
											UniLogDialog.this.timeIntervalPosition = 5;
											break;
										case 80: // 5 sec
											UniLogDialog.this.timeIntervalPosition = 6;
											break;
										case 160: // 10 sec
											UniLogDialog.this.timeIntervalPosition = 7;
											break;
										}
									}
									UniLogDialog.this.timeIntervalCombo.select(UniLogDialog.this.timeIntervalPosition);
								}
							});
							{
								this.statusGroup = new Group(this.configMainComosite, SWT.NONE);
								this.statusGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.statusGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
								this.statusGroup.setText(Messages.getString(MessageIds.GDE_MSGT1302));
								this.statusGroup.setBounds(12, 17, 602, GDE.IS_MAC ? 55 : 45);
								this.statusGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								{
									new Composite(this.statusGroup, SWT.NONE).setLayoutData(new RowData(10, 22));
								}
								{
									this.firmwareText = new CLabel(this.statusGroup, SWT.NONE);
									this.firmwareText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.firmwareText.setText(Messages.getString(MessageIds.GDE_MSGT1303));
									this.firmwareText.setLayoutData(new RowData(90, 22));
								}
								{
									this.firmwareVersionLabel = new CLabel(this.statusGroup, SWT.NONE);
									this.firmwareVersionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.firmwareVersionLabel.setLayoutData(new RowData(70, 22));
									this.firmwareVersionLabel.setText(this.unilogVersion);
								}
								{
									this.serialNumberText = new CLabel(this.statusGroup, SWT.NONE);
									this.serialNumberText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.serialNumberText.setText(Messages.getString(MessageIds.GDE_MSGT1304));
									this.serialNumberText.setLayoutData(new RowData(45, 22));
								}
								{
									this.snLabel = new CLabel(this.statusGroup, SWT.CENTER | SWT.EMBEDDED);
									this.snLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.snLabel.setLayoutData(new RowData(90, 22));
									this.snLabel.setText(this.serialNumber);
								}
								{
									this.memoryUsageText = new CLabel(this.statusGroup, SWT.NONE);
									this.memoryUsageText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.memoryUsageText.setText(Messages.getString(MessageIds.GDE_MSGT1305));
									this.memoryUsageText.setLayoutData(new RowData(135, 22));
								}
								{
									this.memUsagePercent = new CLabel(this.statusGroup, SWT.NONE);
									this.memUsagePercent.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.memUsagePercent.setLayoutData(new RowData(70, 22));
									this.memUsagePercent.setText(this.memoryUsedPercent);
								}
								{
									this.memUsageUnit = new CLabel(this.statusGroup, SWT.NONE);
									this.memUsageUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.memUsageUnit.setText(Messages.getString(MessageIds.GDE_MSGT1306));
									this.memUsageUnit.setLayoutData(new RowData(25, 22));
								}
								this.statusGroup.layout();
							}
							{
								this.autoStartGroup = new Group(this.configMainComosite, SWT.NONE);
								this.autoStartGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.autoStartGroup.setText(Messages.getString(MessageIds.GDE_MSGT1309));
								this.autoStartGroup.setBounds(12, 226, 300, GDE.IS_MAC ? 105 : 100);
								this.autoStartGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								this.autoStartGroup.setLayout(new GridLayout(3, false));
								{
									new Composite(this.autoStartGroup, SWT.NONE).setLayoutData(new GridData(10, 20));
								}
								{
									this.currentTriggerButton = new Button(this.autoStartGroup, SWT.CHECK | SWT.LEFT);
									this.currentTriggerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.currentTriggerButton.setText(Messages.getString(MessageIds.GDE_MSGT1310));
									this.currentTriggerButton.setLayoutData(new GridData(150, 18));
									this.currentTriggerButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "currentTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.currentTriggerCombo = new CCombo(this.autoStartGroup, SWT.BORDER);
									this.currentTriggerCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.currentTriggerCombo.setLayoutData(new GridData(80, 20));
									this.currentTriggerCombo.setItems(new String[] { "  1", "  2", "  3", "  4", "  5", "  6", "  7", "  8", "  9", " 10", "15", "20", "25", "30", "35", "40", "45", "50" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$
									this.currentTriggerCombo.select(2);
									this.currentTriggerCombo.setEditable(false);
									this.currentTriggerCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									this.currentTriggerCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "currentTriggerCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									new Composite(this.autoStartGroup, SWT.NONE).setLayoutData(new GridData(10, 20));
								}
								{
									this.timeTriggerButton = new Button(this.autoStartGroup, SWT.CHECK | SWT.LEFT);
									this.timeTriggerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.timeTriggerButton.setText(Messages.getString(MessageIds.GDE_MSGT1311));
									this.timeTriggerButton.setLayoutData(new GridData(150, 18));
									this.timeTriggerButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "timeTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.timeTriggerCombo = new CCombo(this.autoStartGroup, SWT.BORDER);
									this.timeTriggerCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.timeTriggerCombo.setLayoutData(new GridData(80, 20));
									this.timeTriggerCombo.setItems(new String[] { "  1", "  2", "  3", "  4", "  5", "  6", "  7", "  8", "  9", " 10", " 11", " 12", " 13", " 14", " 15", " 16", " 17", " 18", " 19", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$ //$NON-NLS-19$
											" 20", " 30", " 60", " 120" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
									this.timeTriggerCombo.select(16);
									this.timeTriggerCombo.setEditable(true);
									this.timeTriggerCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									this.timeTriggerCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "timeTriggerCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									new Composite(this.autoStartGroup, SWT.NONE).setLayoutData(new GridData(10, 20));
								}
								{
									this.impulseTriggerButton = new Button(this.autoStartGroup, SWT.CHECK | SWT.LEFT);
									this.impulseTriggerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.impulseTriggerButton.setText(Messages.getString(MessageIds.GDE_MSGT1312));
									this.impulseTriggerButton.setLayoutData(new GridData(150, 18));
									this.impulseTriggerButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "impulseTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.impulseTriggerCombo = new CCombo(this.autoStartGroup, SWT.BORDER);
									this.impulseTriggerCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.impulseTriggerCombo.setLayoutData(new GridData(80, 20));
									this.impulseTriggerCombo.setEditable(false);
									this.impulseTriggerCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									this.impulseTriggerCombo.setItems(UniLogDialog.RX_AUTO_START_MS);
									this.impulseTriggerCombo.select(4);
									this.impulseTriggerCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "rcTriggerCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								this.autoStartGroup.layout();
							}
							{
								this.motorPropGroup = new Group(this.configMainComosite, SWT.NONE);
								this.motorPropGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.motorPropGroup.setLayout(null);
								this.motorPropGroup.setText(Messages.getString(MessageIds.GDE_MSGT1313));
								this.motorPropGroup.setBounds(12, 117, 300, GDE.IS_MAC ? 105 : 100);
								this.motorPropGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								this.motorPropGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
								{
									new Composite(this.motorPropGroup, SWT.NONE).setLayoutData(new RowData(10, 22));
								}
								{
									this.numberPolsButton = new Button(this.motorPropGroup, SWT.RADIO | SWT.LEFT);
									this.numberPolsButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.numberPolsButton.setText(Messages.getString(MessageIds.GDE_MSGT1314));
									this.numberPolsButton.setLayoutData(new RowData(140, 20));
									this.numberPolsButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "numberPolsButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
											if (UniLogDialog.this.numberPolsButton.getSelection()) {
												UniLogDialog.this.numberProbCombo.setEnabled(false);
												UniLogDialog.this.motorPoleCombo.setEnabled(true);
											}
											else {
												UniLogDialog.this.numberProbCombo.setEnabled(true);
												UniLogDialog.this.motorPoleCombo.setEnabled(false);
												UniLogDialog.this.numberPropButton.setSelection(false);
											}
										}
									});
								}
								{
									this.motorPoleCombo = new CCombo(this.motorPropGroup, SWT.BORDER);
									this.motorPoleCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.motorPoleCombo.setLayoutData(new RowData(60, 20));
									this.motorPoleCombo.setItems(new String[] { "  2", "  4", "  6", "  8", " 10", " 12", " 14", " 16" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
									this.motorPoleCombo.select(6);
									this.motorPoleCombo.setEditable(false);
									this.motorPoleCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									this.motorPoleCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "motorPoleCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.gearLabel = new CLabel(this.motorPropGroup, SWT.LEFT);
									this.gearLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.gearLabel.setLayoutData(new RowData(150, 20));
									this.gearLabel.setText(Messages.getString(MessageIds.GDE_MSGT1315));
								}
								{
									this.gearFactorCombo = new Text(this.motorPropGroup, SWT.LEFT | SWT.BORDER);
									this.gearFactorCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.gearFactorCombo.setLayoutData(new RowData(50, 20));
									this.gearFactorCombo.setText(" 1.0  :  1"); //$NON-NLS-1$
									this.gearFactorCombo.setEditable(false);
									this.gearFactorCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
								}
								{
									this.gearRatioSlider = new Slider(this.motorPropGroup, SWT.HORIZONTAL);
									this.gearRatioSlider.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.gearRatioSlider.setLayoutData(new RowData(70, 20));
									this.gearRatioSlider.setMinimum(0);
									this.gearRatioSlider.setMaximum(100);
									this.gearRatioSlider.setSelection(this.sliderPosition);
									this.gearRatioSlider.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "gearRatioSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
											if (UniLogDialog.this.gearRatioSlider.getSelection() > UniLogDialog.this.sliderPosition) {
												UniLogDialog.this.gearFactorCombo.setText(String.format(" %.1f  :  1", getGearRatio() - 0.1)); //$NON-NLS-1$
												++UniLogDialog.this.sliderPosition;
											}
											else {
												UniLogDialog.this.gearFactorCombo.setText(String.format(" %.1f  :  1", getGearRatio() + 0.1)); //$NON-NLS-1$
												--UniLogDialog.this.sliderPosition;
											}
										}
									});
								}
								{
									new Composite(this.motorPropGroup, SWT.NONE).setLayoutData(new RowData(10, 22));
								}
								{
									this.numberPropButton = new Button(this.motorPropGroup, SWT.RADIO | SWT.LEFT);
									this.numberPropButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.numberPropButton.setText(Messages.getString(MessageIds.GDE_MSGT1316));
									this.numberPropButton.setLayoutData(new RowData(140, 20));
									this.numberPropButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "numberPropButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
											if (UniLogDialog.this.numberPropButton.getSelection()) {
												UniLogDialog.this.numberProbCombo.setEnabled(true);
												UniLogDialog.this.motorPoleCombo.setEnabled(false);
											}
											else {
												UniLogDialog.this.numberProbCombo.setEnabled(false);
												UniLogDialog.this.motorPoleCombo.setEnabled(true);
												UniLogDialog.this.numberPolsButton.setSelection(false);
											}
										}
									});
								}
								{
									this.numberProbCombo = new CCombo(this.motorPropGroup, SWT.BORDER);
									this.numberProbCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.numberProbCombo.setLayoutData(new RowData(60, 20));
									this.numberProbCombo.setItems(new String[] { " 1", " 2", " 3", " 4" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
									this.numberProbCombo.select(1);
									this.numberProbCombo.setEditable(false);
									this.numberProbCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									this.numberProbCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "numbeProbCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								this.motorPropGroup.layout();
							}
							{
								this.dataRateGroup = new Group(this.configMainComosite, SWT.NONE);
								this.dataRateGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.dataRateGroup.setText(Messages.getString(MessageIds.GDE_MSGT1317));
								this.dataRateGroup.setBounds(337, 70, 277, GDE.IS_MAC ? 55 : 45);
								this.dataRateGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								this.dataRateGroup.setLayout(new GridLayout(1, true));
								{
									this.timeIntervalCombo = new CCombo(this.dataRateGroup, SWT.BORDER);
									this.timeIntervalCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.timeIntervalCombo.setItems(UniLogDialog.TIME_INTERVAL);
									GridData timeGridData = new GridData(160, 20);
									timeGridData.grabExcessHorizontalSpace = true;
									timeGridData.horizontalAlignment = SWT.CENTER;
									this.timeIntervalCombo.setLayoutData(timeGridData);
									this.timeIntervalCombo.select(1);
									this.timeIntervalCombo.setEditable(false);
									this.timeIntervalCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									this.timeIntervalCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "timeRateCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
											UniLogDialog.this.timeIntervalPosition = UniLogDialog.this.timeIntervalCombo.getSelectionIndex();
										}
									});
								}
								this.dataRateGroup.layout();
							}
							{
								this.sensorGroup = new Group(this.configMainComosite, SWT.NONE);
								this.sensorGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.sensorGroup.setText(Messages.getString(MessageIds.GDE_MSGT1307));
								this.sensorGroup.setBounds(337, 120, 277, 123);
								this.sensorGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								this.sensorGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
								{
									this.currentSensorLabel = new CLabel(this.sensorGroup, SWT.RIGHT);
									this.currentSensorLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.currentSensorLabel.setText("Current");
									this.currentSensorLabel.setLayoutData(new RowData(90, 20));

									this.sensorCurrentCombo = new CCombo(this.sensorGroup, SWT.BORDER);
									this.sensorCurrentCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.sensorCurrentCombo.setLayoutData(new RowData(135, 20));
									this.sensorCurrentCombo.setItems(UniLogDialog.CURRENT_SENSOR);
									this.sensorCurrentCombo.select(2);
									this.sensorCurrentCombo.setEditable(false);
									this.sensorCurrentCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									this.sensorCurrentCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "sensorCurrentCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.a1ModusLabel = new CLabel(this.sensorGroup, SWT.RIGHT);
									this.a1ModusLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.a1ModusLabel.setText("A1 Modus");
									this.a1ModusLabel.setLayoutData(new RowData(90, 20));

									this.a1ModusCombo = new CCombo(this.sensorGroup, SWT.BORDER);
									this.a1ModusCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.a1ModusCombo.setItems(UniLogDialog.A1_MODUS_NAMES_COMBO);
									this.a1ModusCombo.select(0);
									this.a1ModusCombo.setEditable(false);
									this.a1ModusCombo.setLayoutData(new RowData(135, 20));
									this.a1ModusCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									this.a1ModusCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "a1ModusCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.modusA1Position = UniLogDialog.this.a1ModusCombo.getSelectionIndex();
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.a2ModusLabel = new CLabel(this.sensorGroup, SWT.RIGHT);
									this.a2ModusLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.a2ModusLabel.setText("A2 Modus");
									this.a2ModusLabel.setLayoutData(new RowData(90, 20));

									this.a2ModusCombo = new CCombo(this.sensorGroup, SWT.BORDER);
									this.a2ModusCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.a2ModusCombo.setItems(UniLogDialog.A2_MODUS_NAMES_COMBO);
									this.a2ModusCombo.select(0);
									this.a2ModusCombo.setEditable(false);
									this.a2ModusCombo.setLayoutData(new RowData(135, 20));
									this.a2ModusCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									this.a2ModusCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "a2ModusCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.modusA2Position = UniLogDialog.this.a2ModusCombo.getSelectionIndex();
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.a3ModusLabel = new CLabel(this.sensorGroup, SWT.RIGHT);
									this.a3ModusLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.a3ModusLabel.setText("A3 Modus");
									this.a3ModusLabel.setLayoutData(new RowData(90, 20));

									this.a3ModusCombo = new CCombo(this.sensorGroup, SWT.BORDER);
									this.a3ModusCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.a3ModusCombo.setItems(UniLogDialog.A3_MODUS_NAMES_COMBO);
									this.a3ModusCombo.select(0);
									this.a3ModusCombo.setEditable(false);
									this.a3ModusCombo.setLayoutData(new RowData(135, 20));
									this.a3ModusCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									this.a3ModusCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "a3ModusCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.modusA3Position = UniLogDialog.this.a3ModusCombo.getSelectionIndex();
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								this.sensorGroup.layout();
							}
							{
								this.limiterGroup = new Group(this.configMainComosite, SWT.NONE);
								this.limiterGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.limiterGroup.setText(Messages.getString(MessageIds.GDE_MSGT1308));
								this.limiterGroup.setBounds(337, 243, 277, GDE.IS_MAC ? 55 : 45);
								this.limiterGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								this.limiterGroup.setLayout(new GridLayout(3, false));
								{
									new Composite(this.limiterGroup, SWT.NONE).setLayoutData(new GridData(25, 20));
								}
								{
									this.limiterButton = new Button(this.limiterGroup, SWT.CHECK | SWT.LEFT);
									this.limiterButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.limiterButton.setLayoutData(new GridData(100, 20));
									this.limiterButton.setText("      [ W min ] ");
									this.limiterButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "limiterEnergyButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											if (UniLogDialog.this.limiterButton.getSelection()) {
												if (SWT.CANCEL == UniLogDialog.this.application.openOkCancelMessageDialog(UniLogDialog.this.getDialogShell(), "Da der UniLog im Limiter Modus aktiv in das Servosignal eingreift, darf hier nur der Reglerkanal über den UniLog geleitet werden. Bei erreichtem Limit wird ein Servoimpuls von 900 us an den Regler ausgegeben. Es muss sichergestellt sein, dass damit der Antrieb abgeschaltet ist.\n\nLimiter Modus aktivieren?")) {
													UniLogDialog.this.limiterButton.setSelection(false);
												}
											}
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.limiterEnergyCombo = new CCombo(this.limiterGroup, SWT.BORDER);
									this.limiterEnergyCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.limiterEnergyCombo.setItems(new String[]{" 100", " 250", " 500"," 1000", " 1500", " 2000", " 2500", " 3000"});
									this.limiterEnergyCombo.setLayoutData(new GridData(80, 20));
									this.limiterEnergyCombo.select(1);
									this.limiterEnergyCombo.setEditable(true);
									this.limiterEnergyCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									this.limiterEnergyCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "limiterEnergyCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
											int limiterMaxValue = Integer.parseInt(UniLogDialog.this.limiterEnergyCombo.getText().trim().replace(",", "."));
											if (limiterMaxValue < 100)
												limiterMaxValue = 100;
											else if (limiterMaxValue > 3000)
												limiterMaxValue = 3000;
										}
									});
									this.limiterEnergyCombo.addKeyListener(new KeyAdapter() {
										@Override
										public void keyPressed(KeyEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "recordSelectCombo.keyPressed, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								this.limiterGroup.layout();
							}
							{
								this.readAdjustmentButton = new Button(this.configMainComosite, SWT.PUSH | SWT.FLAT | SWT.CENTER);
								this.readAdjustmentButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.readAdjustmentButton.setText(Messages.getString(MessageIds.GDE_MSGT1318));
								this.readAdjustmentButton.setBounds(12, GDE.IS_MAC ? 83 : 74, 300, 30);
								this.readAdjustmentButton.addMouseTrackListener(mouseTrackerEnterFadeOut);
								this.readAdjustmentButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "readAdjustmentButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										try {
											updateConfigurationValues(UniLogDialog.this.serialPort.readConfiguration());
										}
										catch (ApplicationConfigurationException e) {
											log.log(Level.SEVERE, e.getMessage(), e);
											UniLogDialog.this.application.openMessageDialog(UniLogDialog.this.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
											UniLogDialog.this.application.getDeviceSelectionDialog().open();
										}
										catch (Exception e) {
											UniLogDialog.this.application.openMessageDialog(UniLogDialog.this.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0029, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
										}
									}
								});
							}
							{
								this.storeAdjustmentsButton = new Button(this.configMainComosite, SWT.PUSH | SWT.CENTER);
								this.storeAdjustmentsButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.storeAdjustmentsButton.setText(Messages.getString(MessageIds.GDE_MSGT1319));
								this.storeAdjustmentsButton.setBounds(335, GDE.IS_MAC ? 300 : 296, 281, 30);
								this.storeAdjustmentsButton.setEnabled(false);
								this.storeAdjustmentsButton.addMouseTrackListener(mouseTrackerEnterFadeOut);
								this.storeAdjustmentsButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "storeAdjustmentsButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										try {
											if (UniLogDialog.this.serialPort.setConfiguration(buildUpdateBuffer())) {
												updateTimeStep_ms(UniLogDialog.this.timeIntervalPosition);
												UniLogDialog.this.storeAdjustmentsButton.setEnabled(false);
											}
											else {
												UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
											}
										}
										catch (Exception e) {
											UniLogDialog.this.application.openMessageDialog(UniLogDialog.this.getDialogShell(), e.getMessage());
										}
									}
								});
							}
						}
					} // end device configuration tab

					{ // begin measurement configuration tabs
						if (this.device.getChannelCount() > 0) {
							this.configTabItem1 = new CTabItem(this.deviceConfigTabFolder, SWT.NONE);
							this.configTabItem1.setText(this.device.getChannelNameReplacement(1));
							this.configTab1 = new UniLogConfigTab(this.deviceConfigTabFolder, this.device, this.device.getChannelNameReplacement(1));
							this.configTabItem1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.configTabItem1.setControl(this.configTab1);
						}
						if (this.device.getChannelCount() > 1) {
							this.configTabItem2 = new CTabItem(this.deviceConfigTabFolder, SWT.NONE);
							this.configTabItem2.setText(this.device.getChannelNameReplacement(2));
							this.configTab2 = new UniLogConfigTab(this.deviceConfigTabFolder, this.device, this.device.getChannelNameReplacement(2));
							this.configTabItem2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.configTabItem2.setControl(this.configTab2);
						}
						if (this.device.getChannelCount() > 2) {
							this.configTabItem3 = new CTabItem(this.deviceConfigTabFolder, SWT.NONE);
							this.configTabItem3.setText(this.device.getChannelNameReplacement(3));
							this.configTab3 = new UniLogConfigTab(this.deviceConfigTabFolder, this.device, this.device.getChannelNameReplacement(3));
							this.configTabItem3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.configTabItem3.setControl(this.configTab3);
						}
						if (this.device.getChannelCount() > 3) {
							this.configTabItem4 = new CTabItem(this.deviceConfigTabFolder, SWT.NONE);
							this.configTabItem4.setText(this.device.getChannelNameReplacement(4));
							this.configTab4 = new UniLogConfigTab(this.deviceConfigTabFolder, this.device, this.device.getChannelNameReplacement(4));
							this.configTabItem4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.configTabItem4.setControl(this.configTab4);
						}
					} // end measurement configuration tabs

					{ // begin data tab
						this.dataTabItem = new CTabItem(this.deviceConfigTabFolder, SWT.NONE);
						this.dataTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.dataTabItem.setText(Messages.getString(MessageIds.GDE_MSGT1320));
						{
							this.dataMainComposite = new Composite(this.deviceConfigTabFolder, SWT.NONE);
							this.dataMainComposite.setLayout(null);
							this.dataTabItem.setControl(this.dataMainComposite);
							{
								this.channleConfigGroup = new Group(this.dataMainComposite, SWT.NONE);
								this.channleConfigGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.channleConfigGroup.setBounds(14, 12, 290, 58);
								this.channleConfigGroup.setText(Messages.getString(MessageIds.GDE_MSGT1321));
								this.channleConfigGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								this.channleConfigGroup.setLayout(new GridLayout(2, true));
								{
									this.useConfigCombo = new CCombo(this.channleConfigGroup, SWT.BORDER);
									this.useConfigCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.useConfigCombo.setLayoutData(new GridData(140, 20));
									this.useConfigCombo.setItems(this.configurationNames);
									this.useConfigCombo.select(this.channelSelectionIndex);
									this.useConfigCombo.setEditable(false);
									this.useConfigCombo.setTextLimit(18);
									this.useConfigCombo.setVisibleItemCount(5);
									this.useConfigCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									this.useConfigCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT1322));
									this.useConfigCombo.addKeyListener(new KeyAdapter() {
										@Override
										public void keyReleased(KeyEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "useConfigCombo.keyReleased, event=" + evt); //$NON-NLS-1$
											if (evt.character == SWT.CR) {
												String configName = UniLogDialog.this.useConfigCombo.getText().trim();
												UniLogDialog.this.device.setChannelName(configName, UniLogDialog.this.channelSelectionIndex + 1);
												UniLogDialog.this.configurationNames[UniLogDialog.this.channelSelectionIndex] = configName;
												UniLogDialog.this.useConfigCombo.select(UniLogDialog.this.channelSelectionIndex);
												UniLogDialog.this.dataReadGroup.redraw();
												Channels.getInstance().get(UniLogDialog.this.channelSelectionIndex + 1).setName(" " + (UniLogDialog.this.channelSelectionIndex + 1) + " : " + configName); //$NON-NLS-1$ //$NON-NLS-2$
												UniLogDialog.this.application.getMenuToolBar().updateChannelSelector();
												switch (UniLogDialog.this.channelSelectionIndex) {
												case 0: //configTab1
													UniLogDialog.this.configTabItem1.setText(configName);
													UniLogDialog.this.configTab1.setConfigName(configName);
													break;
												case 1: //configTab2
													UniLogDialog.this.configTabItem2.setText(configName);
													UniLogDialog.this.configTab2.setConfigName(configName);
													break;
												case 2: //configTab3
													UniLogDialog.this.configTabItem3.setText(configName);
													UniLogDialog.this.configTab3.setConfigName(configName);
													break;
												case 3: //configTab4
													UniLogDialog.this.configTabItem4.setText(configName);
													UniLogDialog.this.configTab4.setConfigName(configName);
													break;
												}
												UniLogDialog.this.deviceConfigTabFolder.redraw();
												UniLogDialog.this.editConfigButton.setEnabled(false);
												UniLogDialog.this.device.storeDeviceProperties();
											}
										}
									});
									this.useConfigCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "useConfigCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.readDataButton.setEnabled(true);
											UniLogDialog.this.startLoggingButton.setEnabled(true);
											UniLogDialog.this.startLiveGatherButton.setEnabled(UniLogDialog.this.isLiveGathererEnabled = true);
											UniLogDialog.this.channelSelectionIndex = UniLogDialog.this.useConfigCombo.getSelectionIndex();
											UniLogDialog.this.useConfigCombo.select(UniLogDialog.this.channelSelectionIndex);
											Channels.getInstance().switchChannel(UniLogDialog.this.channelSelectionIndex+1, GDE.STRING_EMPTY);
											UniLogDialog.this.editConfigButton.setEnabled(true);
											resetDataSetsLabel();
										}
									});
								}
								{
									this.editConfigButton = new Button(this.channleConfigGroup, SWT.PUSH | SWT.CENTER);
									this.editConfigButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.editConfigButton.setLayoutData(new GridData(130, 20));
									this.editConfigButton.setText(Messages.getString(MessageIds.GDE_MSGT1323));
									this.editConfigButton.setEnabled(false);
									this.editConfigButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "editConfigButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.useConfigCombo.setEditable(true);
											UniLogDialog.this.editConfigButton.setEnabled(false);
										}
									});
								}
								this.channleConfigGroup.layout();
							}
							{
								this.dataReadGroup = new Group(this.dataMainComposite, SWT.NONE);
								this.dataReadGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.dataReadGroup.setBounds(14, 78, 290, 242);
								this.dataReadGroup.setText(Messages.getString(MessageIds.GDE_MSGT1324));
								this.dataReadGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								this.dataReadGroup.setLayout(new GridLayout(2, false));
								this.dataReadGroup.addPaintListener(new PaintListener() {
									@Override
									public void paintControl(PaintEvent evt) {
										if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "dataReadGroup.paintControl, event=" + evt); //$NON-NLS-1$
										int index = UniLogDialog.this.application.getActiveChannelNumber() - 1;
										UniLogDialog.this.configurationNames = new String[UniLogDialog.this.device.getChannelCount()];
										for (int i = 0; i < UniLogDialog.this.configurationNames.length; i++) {
											UniLogDialog.this.configurationNames[i] = " " + UniLogDialog.this.device.getChannelNameReplacement(i + 1); //$NON-NLS-1$
										}
										UniLogDialog.this.useConfigCombo.setItems(UniLogDialog.this.configurationNames);
										UniLogDialog.this.useConfigCombo.select(index);
									}
								});
								{
									this.readDataButton = new Button(this.dataReadGroup, SWT.PUSH | SWT.CENTER);
									this.readDataButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.readDataButton.setText(Messages.getString(MessageIds.GDE_MSGT1325));
									GridData buttonGridData = new GridData(260, 35);
									buttonGridData.grabExcessHorizontalSpace = true;
									buttonGridData.horizontalAlignment = SWT.CENTER;
									buttonGridData.horizontalSpan = 2;
									this.readDataButton.setLayoutData(buttonGridData);
									this.readDataButton.setEnabled(true);
									this.readDataButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "readDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											String channelName = " " + (UniLogDialog.this.useConfigCombo.getSelectionIndex() + 1) + " : " + UniLogDialog.this.useConfigCombo.getText(); //$NON-NLS-1$ //$NON-NLS-2$
											UniLogDialog.this.gatherThread = new UniLogDataGatherer(UniLogDialog.this.application, UniLogDialog.this.device, UniLogDialog.this.serialPort, channelName);
											try {
												UniLogDialog.this.gatherThread.start();
											}
											catch (RuntimeException e) {
												log.log(Level.WARNING, e.getMessage(), e);
											}
											setClosePossible(false);
											UniLogDialog.this.readAdjustmentButton.setEnabled(false);
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(false);
											UniLogDialog.this.readDataButton.setEnabled(false);
											UniLogDialog.this.editConfigButton.setEnabled(false);
											UniLogDialog.this.stopDataButton.setEnabled(true);
											UniLogDialog.this.startLoggingButton.setEnabled(false);
											UniLogDialog.this.stopLoggingButton.setEnabled(false);
											UniLogDialog.this.useConfigCombo.setEnabled(false);
											UniLogDialog.this.startLiveGatherButton.setEnabled(UniLogDialog.this.isLiveGathererEnabled = false);
											UniLogDialog.this.clearMemoryButton.setEnabled(false);
											UniLogDialog.this.closeButton.setEnabled(false);
										}
									});
								}
								{
									this.dataSetLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.dataSetLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									GridData labelGridData = new GridData(180, 20);
									labelGridData.grabExcessHorizontalSpace = true;
									labelGridData.horizontalAlignment = SWT.CENTER;
									labelGridData.horizontalSpan = 1;
									this.dataSetLabel.setLayoutData(labelGridData);
									this.dataSetLabel.setText(Messages.getString(MessageIds.GDE_MSGT1326));
								}
								{
									this.redDataSetLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.redDataSetLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									GridData labelGridData = new GridData(55, 20);
									labelGridData.grabExcessHorizontalSpace = true;
									labelGridData.horizontalAlignment = SWT.CENTER;
									labelGridData.horizontalSpan = 1;
									this.redDataSetLabel.setLayoutData(labelGridData);
									this.redDataSetLabel.setText("0"); //$NON-NLS-1$
								}
								{
									this.actualDataSetNumberLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.actualDataSetNumberLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									GridData labelGridData = new GridData(180, 20);
									labelGridData.grabExcessHorizontalSpace = true;
									labelGridData.horizontalAlignment = SWT.CENTER;
									labelGridData.horizontalSpan = 1;
									this.actualDataSetNumberLabel.setLayoutData(labelGridData);
									this.actualDataSetNumberLabel.setText(Messages.getString(MessageIds.GDE_MSGT1327));
								}
								{
									this.actualDataSetNumber = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.actualDataSetNumber.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									GridData labelGridData = new GridData(55, 20);
									labelGridData.grabExcessHorizontalSpace = true;
									labelGridData.horizontalAlignment = SWT.CENTER;
									labelGridData.horizontalSpan = 1;
									this.actualDataSetNumber.setLayoutData(labelGridData);
									this.actualDataSetNumber.setText("0"); //$NON-NLS-1$
								}
								{
									this.readDataErrorLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.readDataErrorLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									GridData labelGridData = new GridData(180, 20);
									labelGridData.grabExcessHorizontalSpace = true;
									labelGridData.horizontalAlignment = SWT.CENTER;
									labelGridData.horizontalSpan = 1;
									this.readDataErrorLabel.setLayoutData(labelGridData);
									this.readDataErrorLabel.setText(Messages.getString(MessageIds.GDE_MSGT1328));
								}
								{
									this.numberReadErrorLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.numberReadErrorLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									GridData labelGridData = new GridData(55, 20);
									labelGridData.grabExcessHorizontalSpace = true;
									labelGridData.horizontalAlignment = SWT.CENTER;
									labelGridData.horizontalSpan = 1;
									this.numberReadErrorLabel.setLayoutData(labelGridData);
									this.numberReadErrorLabel.setText("0"); //$NON-NLS-1$
								}
								{
									this.readDataLess2Label = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.readDataLess2Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									GridData labelGridData = new GridData(180, 20);
									labelGridData.grabExcessHorizontalSpace = true;
									labelGridData.horizontalAlignment = SWT.CENTER;
									labelGridData.horizontalSpan = 1;
									this.readDataLess2Label.setLayoutData(labelGridData);
									this.readDataLess2Label.setText(Messages.getString(MessageIds.GDE_MSGT1331));
								}
								{
									this.numberLess2Label = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.numberLess2Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									GridData labelGridData = new GridData(55, 20);
									labelGridData.grabExcessHorizontalSpace = true;
									labelGridData.horizontalAlignment = SWT.CENTER;
									labelGridData.horizontalSpan = 1;
									this.numberLess2Label.setLayoutData(labelGridData);
									this.numberLess2Label.setText("0"); //$NON-NLS-1$
								}
								{
									this.readDataProgressBar = new ProgressBar(this.dataReadGroup, SWT.NONE);
									GridData labelGridData = new GridData(260, 24);
									labelGridData.grabExcessHorizontalSpace = true;
									//labelGridData.grabExcessVerticalSpace = true;
									labelGridData.horizontalAlignment = SWT.CENTER;
									labelGridData.horizontalSpan = 2;
									this.readDataProgressBar.setLayoutData(labelGridData);
									this.readDataProgressBar.setMinimum(0);
									this.readDataProgressBar.setMaximum(100);
								}
								{
									this.stopDataButton = new Button(this.dataReadGroup, SWT.PUSH | SWT.CENTER);
									this.stopDataButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									GridData buttonGridData = new GridData(260, 35);
									buttonGridData.grabExcessHorizontalSpace = true;
									buttonGridData.horizontalAlignment = SWT.CENTER;
									buttonGridData.horizontalSpan = 2;
									this.stopDataButton.setLayoutData(buttonGridData);
									this.stopDataButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0278));
									this.stopDataButton.setEnabled(false);
									this.stopDataButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "stopDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											if (UniLogDialog.this.gatherThread != null && UniLogDialog.this.gatherThread.isAlive()) {
												UniLogDialog.this.gatherThread.setThreadStop(); // end serial communication
											}
											resetButtons();
										}
									});
								}
								this.dataReadGroup.layout();
							}
							{
								this.liveDataCaptureGroup = new Group(this.dataMainComposite, SWT.NONE);
								this.liveDataCaptureGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.liveDataCaptureGroup.setBounds(324, 12, 284, 198);
								this.liveDataCaptureGroup.setText(Messages.getString(MessageIds.GDE_MSGT1329));
								this.liveDataCaptureGroup.setToolTipText(Messages.getString(MessageIds.GDE_MSGT1384));
								this.liveDataCaptureGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								this.liveDataCaptureGroup.setLayout(new GridLayout(1, true));
								{
									this.startLiveGatherButton = new Button(this.liveDataCaptureGroup, SWT.PUSH | SWT.CENTER);
									this.startLiveGatherButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.startLiveGatherButton.setText(Messages.getString(MessageIds.GDE_MSGT1330));
									this.startLiveGatherButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT1384));
									GridData buttonGridData = new GridData(260, 35);
									buttonGridData.grabExcessHorizontalSpace = true;
									buttonGridData.horizontalAlignment = SWT.CENTER;
									this.startLiveGatherButton.setLayoutData(buttonGridData);
									this.startLiveGatherButton.setEnabled(UniLogDialog.this.isLiveGathererEnabled);
									this.startLiveGatherButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "liveViewButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											try {
												int channelNumber = UniLogDialog.this.useConfigCombo.getSelectionIndex() + 1;
												setButtonStateLiveGatherer(UniLogDialog.this.isLiveGathererEnabled = false);
												setClosePossible(false);
												UniLogDialog.this.liveThread = new UniLogLiveGatherer(UniLogDialog.this.application, UniLogDialog.this.device, UniLogDialog.this.serialPort, channelNumber, UniLogDialog.this);
												try {
													UniLogDialog.this.liveThread.start();
												}
												catch (RuntimeException e) {
													log.log(Level.WARNING, e.getMessage(), e);
												}
											}
											catch (Exception e) {
												if (UniLogDialog.this.liveThread != null && UniLogDialog.this.liveThread.isTimerRunning) {
													UniLogDialog.this.liveThread.stopTimerThread();
													UniLogDialog.this.liveThread.interrupt();
												}
												UniLogDialog.this.application.updateGraphicsWindow();
												UniLogDialog.this.application.openMessageDialog(UniLogDialog.this.getDialogShell(), Messages.getString(MessageIds.GDE_MSGE1301, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
												resetButtons();
											}
										}
									});
								}
								{
									this.loggingGroup = new Group(this.liveDataCaptureGroup, SWT.NONE);
									this.loggingGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									GridData groupGridData = new GridData(260, 50);
									groupGridData.grabExcessHorizontalSpace = true;
									groupGridData.horizontalAlignment = SWT.CENTER;
									this.loggingGroup.setLayoutData(groupGridData);
									this.loggingGroup.setText(Messages.getString(MessageIds.GDE_MSGT1332));
									this.loggingGroup.setToolTipText(Messages.getString(MessageIds.GDE_MSGT1383));
									this.loggingGroup.addMouseTrackListener(mouseTrackerEnterFadeOut);
									this.loggingGroup.setLayout(new GridLayout(2, true));
									{
										this.startLoggingButton = new Button(this.loggingGroup, SWT.PUSH | SWT.CENTER);
										this.startLoggingButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.startLoggingButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0274));
										this.startLoggingButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT1383));
										GridData buttonGridData = new GridData(100, 30);
										buttonGridData.grabExcessHorizontalSpace = true;
										buttonGridData.grabExcessVerticalSpace = true;
										buttonGridData.horizontalAlignment = SWT.CENTER;
										this.startLoggingButton.setLayoutData(buttonGridData);
										this.startLoggingButton.setEnabled(true);
										this.startLoggingButton.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "startLoggingButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												try {
													setClosePossible(false);
													UniLogDialog.this.serialPort.startLogging();
													UniLogDialog.this.startLoggingButton.setEnabled(false);
													UniLogDialog.this.stopLoggingButton.setEnabled(true);
												}
												catch (Exception e) {
													log.log(Level.SEVERE, e.getMessage(), e);
													UniLogDialog.this.application.openMessageDialog(UniLogDialog.this.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0029, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
												}
											}
										});
									}
									{
										this.stopLoggingButton = new Button(this.loggingGroup, SWT.PUSH | SWT.CENTER);
										this.stopLoggingButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.stopLoggingButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0275));
										GridData buttonGridData = new GridData(100, 30);
										buttonGridData.grabExcessHorizontalSpace = true;
										buttonGridData.grabExcessVerticalSpace = true;
										buttonGridData.horizontalAlignment = SWT.CENTER;
										this.stopLoggingButton.setLayoutData(buttonGridData);
										this.stopLoggingButton.setEnabled(false);
										this.stopLoggingButton.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "stopLoggingButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												try {
													UniLogDialog.this.serialPort.stopLogging();
													UniLogDialog.this.startLoggingButton.setEnabled(true);
													UniLogDialog.this.stopLoggingButton.setEnabled(false);
													setClosePossible(true);
													if (!UniLogDialog.this.stopLiveGatherButton.getEnabled()) {
														UniLogDialog.this.readDataButton.setEnabled(true);
													}
												}
												catch (Exception e) {
													log.log(Level.SEVERE, e.getMessage(), e);
													UniLogDialog.this.application.openMessageDialog(UniLogDialog.this.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0029, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
												}
											}
										});
									}
									this.loggingGroup.layout();
								}
								{
									this.stopLiveGatherButton = new Button(this.liveDataCaptureGroup, SWT.PUSH | SWT.CENTER);
									this.stopLiveGatherButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									GridData buttonGridData = new GridData(260, 35);
									buttonGridData.grabExcessHorizontalSpace = true;
									buttonGridData.horizontalAlignment = SWT.CENTER;
									this.stopLiveGatherButton.setLayoutData(buttonGridData);
									this.stopLiveGatherButton.setText(Messages.getString(MessageIds.GDE_MSGT1333));
									this.stopLiveGatherButton.setEnabled(!UniLogDialog.this.isLiveGathererEnabled);
									this.stopLiveGatherButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "stopLiveGatherButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											if (UniLogDialog.this.liveThread != null) {
												if (UniLogDialog.this.liveThread.isTimerRunning) {
													UniLogDialog.this.liveThread.stopTimerThread();
												}
												UniLogDialog.this.serialPort.isInterruptedByUser = true;
												UniLogDialog.this.liveThread.interrupt();

												if (Channels.getInstance().getActiveChannel() != null) {
													Channel activeChannle = Channels.getInstance().getActiveChannel();
													if (activeChannle != null) {
														RecordSet activeRecordSet = activeChannle.getActiveRecordSet();
														if (activeRecordSet != null) {
															// active record set name == live gatherer record name
															UniLogDialog.this.liveThread.finalizeRecordSet(activeRecordSet.getName());
														}
													}
												}
											}
											setButtonStateLiveGatherer(UniLogDialog.this.isLiveGathererEnabled = true);
											setClosePossible(true);
											if (!UniLogDialog.this.stopLoggingButton.getEnabled()) {
												UniLogDialog.this.readDataButton.setEnabled(true);
											}
										}
									});
								}
								this.liveDataCaptureGroup.layout();
							}

							{ // begin clearDataBufferGroup
								this.clearDataBufferGroup = new Group(this.dataMainComposite, SWT.NONE);
								this.clearDataBufferGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.clearDataBufferGroup.setBounds(324, 216, 284, 104);
								this.clearDataBufferGroup.setText(Messages.getString(MessageIds.GDE_MSGT1334));
								this.clearDataBufferGroup.setToolTipText(Messages.getString(MessageIds.GDE_MSGT1385));
								this.clearDataBufferGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								this.clearDataBufferGroup.setLayout(new GridLayout(1, true));
								{
									this.memoryDeleteInfo = new Text(this.clearDataBufferGroup, SWT.MULTI | SWT.CENTER | SWT.WRAP);
									this.memoryDeleteInfo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									GridData infoGridData = new GridData(260, 30);
									infoGridData.grabExcessHorizontalSpace = true;
									infoGridData.horizontalAlignment = SWT.CENTER;
									this.memoryDeleteInfo.setLayoutData(infoGridData);
									this.memoryDeleteInfo.setText(Messages.getString(MessageIds.GDE_MSGI1301));
									this.memoryDeleteInfo.setBackground(DataExplorer.getInstance().COLOR_LIGHT_GREY);
								}
								{
									this.clearMemoryButton = new Button(this.clearDataBufferGroup, SWT.PUSH | SWT.CENTER);
									this.clearMemoryButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.clearMemoryButton.setText(Messages.getString(MessageIds.GDE_MSGT1335));
									this.clearMemoryButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT1385));
									GridData buttonGridData = new GridData(260, 35);
									buttonGridData.grabExcessHorizontalSpace = true;
									buttonGridData.horizontalAlignment = SWT.CENTER;
									this.clearMemoryButton.setLayoutData(buttonGridData);
									this.clearMemoryButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "clearMemoryButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											try {
												UniLogDialog.this.clearMemoryButton.setEnabled(false);
												UniLogDialog.this.serialPort.clearMemory();
											}
											catch (Exception e) {
												log.log(Level.SEVERE, e.getMessage(), e);
												UniLogDialog.this.application.openMessageDialog(UniLogDialog.this.getDialogShell(), Messages.getString(MessageIds.GDE_MSGE1300, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
												e.printStackTrace();
											}
											UniLogDialog.this.clearMemoryButton.setEnabled(true);
										}
									});
								}
								this.clearDataBufferGroup.layout();
							} // end clearDataBufferGroup
						}
					} // end data tab

					this.deviceConfigTabFolder.setBounds(0, 0, GDE.IS_LINUX ? 650 : 635, 360);
					this.deviceConfigTabFolder.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "configTabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
							int channelNumber = UniLogDialog.this.deviceConfigTabFolder.getSelectionIndex();
							if (channelNumber >= 1 && channelNumber <= 4) {
								String configKey = channelNumber + " : " + ((CTabItem) evt.item).getText(); //$NON-NLS-1$
								Channels channels = Channels.getInstance();
								Channel activeChannel = channels.getActiveChannel();
								if (activeChannel != null) {
									if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "activeChannel = " + activeChannel.getName() + " configKey = " + configKey); //$NON-NLS-1$ //$NON-NLS-2$
									RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
									if (activeRecordSet != null && !activeChannel.getName().trim().equals(configKey)) {
										int answer = UniLogDialog.this.application.openYesNoMessageDialog(UniLogDialog.this.getDialogShell(), Messages.getString(MessageIds.GDE_MSGI1302));
										if (answer == SWT.YES) {
											String recordSetKey = activeRecordSet.getName();
											if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "move record set " + recordSetKey + " to channel/configuration " + channelNumber + GDE.STRING_BLANK_COLON_BLANK + configKey); //$NON-NLS-1$ //$NON-NLS-2$
											channels.get(channelNumber).put(recordSetKey, activeRecordSet.clone(channelNumber));
											activeChannel.remove(recordSetKey);
											channels.switchChannel(channelNumber, recordSetKey);
											switch (channelNumber) {
											case 1:
												UniLogDialog.this.configTab1.initEditable();
												UniLogDialog.this.configTab1.checkUpdateAnalog();
												break;
											case 2:
												UniLogDialog.this.configTab2.initEditable();
												UniLogDialog.this.configTab2.checkUpdateAnalog();
												break;
											case 3:
												UniLogDialog.this.configTab3.initEditable();
												UniLogDialog.this.configTab3.checkUpdateAnalog();
												break;
											case 4:
												UniLogDialog.this.configTab4.initEditable();
												UniLogDialog.this.configTab4.checkUpdateAnalog();
												break;
											}
											RecordSet newActiveRecordSet = channels.get(channelNumber).getActiveRecordSet();
											if (newActiveRecordSet != null) {
												UniLogDialog.this.device.updateVisibilityStatus(newActiveRecordSet, false);
												UniLogDialog.this.device.makeInActiveDisplayable(newActiveRecordSet);
												UniLogDialog.this.application.updateCurveSelectorTable();
											}
										}
									}
								}
							}
						}
					});
				} // end tabs

				{ // begin telemetry tab
					this.telemetryTabItem = new CTabItem(this.deviceConfigTabFolder, SWT.NONE);
					this.telemetryTabItem.setText(Messages.getString(MessageIds.GDE_MSGT1390));
					this.telemetryTab = new UniLogTelemetryTab(this.deviceConfigTabFolder, this.device);
					this.telemetryTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.telemetryTabItem.setControl(this.telemetryTab);

				} // end tab

				int index = Channels.getInstance().getActiveChannelNumber();
				this.deviceConfigTabFolder.setSelection(index < 1 || index > this.deviceConfigTabFolder.getChildren().length - 2 ? 1 : index);

				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 320, 100));
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
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * update the configuration tab with values red
	 * @param readBuffer
	 */
	public void updateConfigurationValues(byte[] readBuffer) {
		//status field
		this.memoryUsed = ((readBuffer[6] & 0xFF) << 8) + (readBuffer[7] & 0xFF);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "memoryUsed = " + this.memoryUsed); //$NON-NLS-1$

		this.unilogVersion = String.format(Locale.ENGLISH, "v%.2f", Double.valueOf(readBuffer[8] & 0xFF) / 100); //$NON-NLS-1$
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "unilogVersion = " + this.unilogVersion); //$NON-NLS-1$

		int memoryDeleted = readBuffer[9] & 0xFF;
		int tmpMemoryUsed = 0;
		if (memoryDeleted > 0)
			tmpMemoryUsed = 0;
		else
			tmpMemoryUsed = this.memoryUsed;
		this.memoryUsedPercent = String.format("%.2f", tmpMemoryUsed * 100.0 / UniLogDialog.WERTESAETZE_MAX); //$NON-NLS-1$
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "memoryUsedPercent = " + this.memoryUsedPercent + " (" + tmpMemoryUsed + "/" + UniLogDialog.WERTESAETZE_MAX + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		// timer interval
		this.timeIntervalPosition = readBuffer[10] & 0xFF;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "timeIntervalPosition = " + this.timeIntervalPosition); //$NON-NLS-1$
		updateTimeStep_ms(this.timeIntervalPosition);

		// motor/prop
		this.isMotorPole = false;
		this.isPropBlade = false;
		this.countMotorPole = 0;
		this.countPropBlade = 0;
		if ((readBuffer[11] & 0x80) == 0) {
			this.isPropBlade = true;
		}
		else {
			this.isMotorPole = true;
		}
		this.countPropBlade = readBuffer[11] & 0x7F;
		this.countMotorPole = (readBuffer[11] & 0x7F) * 2;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "isPropBlade = " + this.isPropBlade + " countPropBlade = " + this.countPropBlade); //$NON-NLS-1$ //$NON-NLS-2$
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "isMotorPole = " + this.isMotorPole + " countMotorPole = " + this.countMotorPole); //$NON-NLS-1$ //$NON-NLS-2$

		this.isAutoStartCurrent = false;
		this.currentAutoStart = 0;
		if ((readBuffer[12] & 0x80) != 0) {
			this.isAutoStartCurrent = true;
		}
		this.currentAutoStart = readBuffer[12] & 0x7F;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "isAutoStartCurrent = " + this.isAutoStartCurrent + " currentAutoStart = " + this.currentAutoStart); //$NON-NLS-1$ //$NON-NLS-2$

		this.isAutStartRx = false;
		this.isRxOn = false;
		this.rxAutoStartValue = 0;
		if ((readBuffer[13] & 0x80) != 0) {
			this.isAutStartRx = true;
		}
		this.rxAutoStartValue = (readBuffer[13] & 0x7F); // 16 = 1.6 ms (value - 11 = position in RX_AUTO_START_MS)
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "isAutStartRx = " + this.isAutStartRx + " isRxOn = " + this.isRxOn + " rxAutoStartValue = " + this.rxAutoStartValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		this.isImpulseAutoStartTime = false;
		this.impulseAutoStartTime_sec = 0;
		if ((readBuffer[14] & 0x80) != 0) {
			this.isImpulseAutoStartTime = true;
		}
		this.impulseAutoStartTime_sec = readBuffer[14] & 0x7F;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "isAutoStartTime = " + this.isImpulseAutoStartTime + " timeAutoStart_sec = " + this.impulseAutoStartTime_sec); //$NON-NLS-1$ //$NON-NLS-2$

		this.currentSensorPosition = readBuffer[15] & 0xFF;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "currentSensor = " + this.currentSensorPosition); //$NON-NLS-1$

		this.serialNumber = "" + (((readBuffer[16] & 0xFF) << 8) + (readBuffer[17] & 0xFF)); //$NON-NLS-1$
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "serialNumber = " + this.serialNumber); //$NON-NLS-1$

		this.modusA1Position = (readBuffer[18] & 0xFF) <= 3 ? (readBuffer[18] & 0xFF) : 0;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "modusA1 = " + this.modusA1Position); //$NON-NLS-1$

		this.modusA2Position = (readBuffer[4] & 0xFF);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "modusA2 = " + this.modusA2Position); //$NON-NLS-1$
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "select A2 combo = " + (this.modusA2Position >= 1 ? this.modusA2Position - 1 : this.modusA2Position)); //$NON-NLS-1$

		this.modusA3Position = (readBuffer[5] & 0xFF);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "modusA3 = " + this.modusA3Position); //$NON-NLS-1$
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "select A3 combo = " + (this.modusA3Position >= 1 ? this.modusA3Position - 1 : this.modusA3Position)); //$NON-NLS-1$

		this.isLimiterActive = (readBuffer[19] & 0x80) > 1;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "limiter active = " + this.isLimiterActive); //$NON-NLS-1$

		this.limiterValue = ((readBuffer[19] & 0x7F) << 8) | (readBuffer[20] & 0xFF);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "limiterValue = " + this.limiterValue); //$NON-NLS-1$

		this.gearRatio = (readBuffer[21] & 0xFF) / 10.0;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("gearRatio = %.1f", this.gearRatio)); //$NON-NLS-1$

		if (this.dialogShell != null && !this.dialogShell.isDisposed()) { //update UI if opened
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					UniLogDialog.this.firmwareVersionLabel.setText(UniLogDialog.this.unilogVersion);
					UniLogDialog.this.memUsagePercent.setText(UniLogDialog.this.memoryUsedPercent);
					UniLogDialog.this.timeIntervalCombo.select(UniLogDialog.this.timeIntervalPosition);
					UniLogDialog.this.numberPolsButton.setSelection(UniLogDialog.this.isMotorPole);
					UniLogDialog.this.numberPropButton.setSelection(UniLogDialog.this.isPropBlade);
					//motorPoleCombo.setItems(new String[] {"2", "4", "6", "8", "10", "12", "14", "16"});
					UniLogDialog.this.motorPoleCombo.select(UniLogDialog.this.countMotorPole / 2 - 1);
					//numbeProbCombo.setItems(new String[] {"1", "2", "3", "4"});
					UniLogDialog.this.numberProbCombo.select(UniLogDialog.this.countPropBlade - 1);
					if (UniLogDialog.this.isMotorPole) {
						UniLogDialog.this.numberProbCombo.setEnabled(false);
						UniLogDialog.this.motorPoleCombo.setEnabled(true);
					}
					else {
						UniLogDialog.this.numberProbCombo.setEnabled(true);
						UniLogDialog.this.motorPoleCombo.setEnabled(false);
					}
					UniLogDialog.this.currentTriggerButton.setSelection(UniLogDialog.this.isAutoStartCurrent);
					UniLogDialog.this.currentTriggerCombo.select(UniLogDialog.this.currentAutoStart - 1);
					UniLogDialog.this.impulseTriggerCombo.select(UniLogDialog.this.rxAutoStartValue - 11);
					UniLogDialog.this.impulseTriggerButton.setSelection(UniLogDialog.this.isAutStartRx);
					UniLogDialog.this.timeTriggerButton.setSelection(UniLogDialog.this.isImpulseAutoStartTime);
					UniLogDialog.this.timeTriggerCombo.select(UniLogDialog.this.impulseAutoStartTime_sec + 1);
					UniLogDialog.this.timeTriggerCombo.setText(String.format("%4s", UniLogDialog.this.impulseAutoStartTime_sec)); //$NON-NLS-1$
					UniLogDialog.this.sensorCurrentCombo.select(UniLogDialog.this.currentSensorPosition);
					UniLogDialog.this.snLabel.setText(UniLogDialog.this.serialNumber);
					UniLogDialog.this.a1ModusCombo.select(UniLogDialog.this.modusA1Position);
					UniLogDialog.this.a2ModusCombo.select(UniLogDialog.this.modusA2Position >= 1 ? UniLogDialog.this.modusA2Position - 1 : UniLogDialog.this.modusA2Position);
					UniLogDialog.this.a3ModusCombo.select(UniLogDialog.this.modusA3Position >= 1 ? UniLogDialog.this.modusA3Position - 1 : UniLogDialog.this.modusA3Position);
					UniLogDialog.this.limiterButton.setSelection(UniLogDialog.this.isLimiterActive);
					if (UniLogDialog.this.limiterValue < 1000) {
						UniLogDialog.this.limiterEnergyCombo.select(UniLogDialog.this.limiterValue / 250);
					}
					else {
						UniLogDialog.this.limiterEnergyCombo.select(UniLogDialog.this.limiterValue / 500 + 1);
					}
					UniLogDialog.this.limiterEnergyCombo.setText("" + UniLogDialog.this.limiterValue);
					UniLogDialog.this.gearFactorCombo.setText(String.format(" %.1f  :  1", UniLogDialog.this.gearRatio)); //$NON-NLS-1$
					if (UniLogDialog.this.configTab1 != null) UniLogDialog.this.configTab1.setA1ModusAvailable(true);
					if (UniLogDialog.this.configTab2 != null) UniLogDialog.this.configTab2.setA1ModusAvailable(true);
					if (UniLogDialog.this.configTab3 != null) UniLogDialog.this.configTab3.setA1ModusAvailable(true);
					if (UniLogDialog.this.configTab4 != null) UniLogDialog.this.configTab4.setA1ModusAvailable(true);

					TaskBar taskBar = UniLogDialog.this.dialogShell.getDisplay().getSystemTaskBar();
					if (taskBar == null)
						UniLogDialog.this.taskBarItem = null;
					else {
						UniLogDialog.this.taskBarItem = taskBar.getItem(UniLogDialog.this.dialogShell) != null ? taskBar.getItem(UniLogDialog.this.dialogShell) : taskBar.getItem(null);

						if (!UniLogDialog.this.readDataButton.getEnabled() && UniLogDialog.this.taskBarItem != null)
							UniLogDialog.this.taskBarItem.setProgressState(GDE.IS_MAC ? SWT.ERROR : SWT.NORMAL);
					}
				}
			});
		}
	}

	public byte[] buildUpdateBuffer() {
		int checkSum = 0;
		byte[] updateBuffer = new byte[15];
		updateBuffer[0] = (byte) 0xC0;
		updateBuffer[1] = (byte) 0x03;
		checkSum = checkSum + (0xFF & updateBuffer[1]);
		updateBuffer[2] = (byte) 0x02;
		checkSum = checkSum + (0xFF & updateBuffer[2]);

		updateBuffer[3] = (byte) this.timeIntervalCombo.getSelectionIndex();
		checkSum = checkSum + (0xFF & updateBuffer[3]);

		if (this.numberPolsButton.getSelection()) // isMotorPole
			updateBuffer[4] = (byte) ((this.motorPoleCombo.getSelectionIndex() + 1) | 0x80);
		else
			updateBuffer[4] = (byte) ((this.numberProbCombo.getSelectionIndex() + 1));
		checkSum = checkSum + (0xFF & updateBuffer[4]);

		if (this.currentTriggerButton.getSelection()) // isCurrentTriggerAutoStart
			updateBuffer[5] = (byte) ((this.currentTriggerCombo.getSelectionIndex() + 1) | 0x80);
		else
			updateBuffer[5] = (byte) (this.currentTriggerCombo.getSelectionIndex() + 1);
		checkSum = checkSum + (0xFF & updateBuffer[5]);

		if (this.impulseTriggerButton.getSelection())
			if ((this.impulseTriggerCombo.getSelectionIndex() + 1) == UniLogDialog.RX_AUTO_START_MS.length) // "RX an"
				updateBuffer[6] = (byte) 0x80;
			else
				updateBuffer[6] = (byte) ((this.impulseTriggerCombo.getSelectionIndex() | 0x80) + 11);
		else if ((this.impulseTriggerCombo.getSelectionIndex() + 1) == UniLogDialog.RX_AUTO_START_MS.length) // "RX an"
			updateBuffer[6] = (byte) 0x00;
		else
			updateBuffer[6] = (byte) (this.impulseTriggerCombo.getSelectionIndex() + 11);
		checkSum = checkSum + (0xFF & updateBuffer[6]);

		if (this.timeTriggerButton.getSelection())
			updateBuffer[7] = (byte) (Byte.parseByte(this.timeTriggerCombo.getText().trim()) | 0x80);
		else
			updateBuffer[7] = Byte.parseByte(this.timeTriggerCombo.getText().trim());
		checkSum = checkSum + (0xFF & updateBuffer[7]);

		updateBuffer[8] = (byte) this.sensorCurrentCombo.getSelectionIndex();
		checkSum = checkSum + (0xFF & updateBuffer[8]);

		updateBuffer[9] = (byte) this.a1ModusCombo.getSelectionIndex();
		checkSum = checkSum + (0xFF & updateBuffer[9]);

		updateBuffer[10] = (byte) ((Integer.parseInt(this.limiterEnergyCombo.getText().trim()) & 0xFF00) >> 8);
		if (this.limiterButton.getSelection()) {
			updateBuffer[10] = (byte) (updateBuffer[10] | 0x80);
		}
		checkSum = checkSum + (0xFF & updateBuffer[10]);
		updateBuffer[11] = (byte) (Integer.parseInt(this.limiterEnergyCombo.getText().trim()) & 0x00FF);
		checkSum = checkSum + (0xFF & updateBuffer[11]);

		double tempGearRatio = Double.valueOf(this.gearFactorCombo.getText().split(":")[0].trim().replace(',', '.')).doubleValue() * 10; //$NON-NLS-1$
		updateBuffer[12] = (byte) tempGearRatio;
		checkSum = checkSum + (0xFF & updateBuffer[12]);

		int tmpA2Modus = this.a2ModusCombo.getSelectionIndex();
		tmpA2Modus = tmpA2Modus >= 1 ? tmpA2Modus + 1 : tmpA2Modus;
		int tmpA3Modus = this.a3ModusCombo.getSelectionIndex();
		tmpA3Modus = tmpA3Modus >= 1 ? tmpA3Modus + 1 : tmpA3Modus;
		updateBuffer[13] = (byte) (((tmpA3Modus & 0x0F) << 4) | (tmpA2Modus & 0x0F));
		checkSum = checkSum + (0xFF & updateBuffer[13]);

		updateBuffer[14] = (byte) (checkSum % 256);

		if (log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder();
			sb.append("updateBuffer = ["); //$NON-NLS-1$
			for (int i = 0; i < updateBuffer.length; i++) {
				if (i == updateBuffer.length - 1)
					sb.append(String.format("%02X", updateBuffer[i])); //$NON-NLS-1$
				else
					sb.append(String.format("%02X ", updateBuffer[i])); //$NON-NLS-1$
			}
			sb.append("]"); //$NON-NLS-1$
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, sb.toString());
		}

		return updateBuffer;
	}

	/**
	 * @param newStatus the status text to set
	 */
	public void setStatusText(String newStatus) {
		this.statusText = newStatus;
		this.memUsagePercent.setText(this.statusText);
	}

	/**
	 * @return the readDataProgressBar
	 */
	public ProgressBar getReadDataProgressBar() {
		return this.readDataProgressBar;
	}

	/**
	 * set progress bar to value between 0 to 100, called out of thread
	 * @param value
	 */
	public void setReadDataProgressBar(final int value) {
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				int tmpValue = value < 0 ? 0 : value;
				tmpValue = value > 100 ? 100 : value;
				UniLogDialog.this.readDataProgressBar.setSelection(tmpValue);
				if (UniLogDialog.this.taskBarItem != null) UniLogDialog.this.taskBarItem.setProgress(tmpValue);
			}
		});
	}

	/**
	 * query the rear ratio
	 * @return
	 */
	double getGearRatio() {
		return Double.valueOf(this.gearFactorCombo.getText().split(":")[0].trim().replace(',', '.')).doubleValue(); //$NON-NLS-1$
	}

	/**
	 * update the counter number in the dialog, called out of thread
	 * @param redTelegrams
	 * @param numberRecordSet
	 * @param numReadErrors
	 * @param numLess2Measurements
	 * @param memoryUsedValue
	 */
	public void updateDataGatherProgress(final int redTelegrams, final int numberRecordSet, final int numReadErrors, final int numLess2Measurements, final int memoryUsedValue) {
		this.numberRedDataSetsText = "" + redTelegrams; //$NON-NLS-1$
		this.numberActualDataSetsText = "" + numberRecordSet; //$NON-NLS-1$
		this.numberReadErrorText = "" + numReadErrors; //$NON-NLS-1$
		this.numberLess2Text = "" + numLess2Measurements; //$NON-NLS-1$
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				int progress = memoryUsedValue > 0 ? redTelegrams * 100 / memoryUsedValue : 100;
				int tmpValue = progress < 0 ? 0 : progress;
				tmpValue = progress > 100 ? 100 : progress;
				UniLogDialog.this.readDataProgressBar.setSelection(tmpValue);
				if (UniLogDialog.this.taskBarItem != null) UniLogDialog.this.taskBarItem.setProgress(tmpValue);
				UniLogDialog.this.redDataSetLabel.setText(UniLogDialog.this.numberRedDataSetsText);
				UniLogDialog.this.actualDataSetNumber.setText(UniLogDialog.this.numberActualDataSetsText);
				UniLogDialog.this.numberReadErrorLabel.setText(UniLogDialog.this.numberReadErrorText);
				UniLogDialog.this.numberLess2Label.setText(UniLogDialog.this.numberLess2Text);
			}
		});
	}

	/**
	 * function to reset counter labels
	 */
	public void resetDataSetsLabel() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.numberRedDataSetsText = "0"; //$NON-NLS-1$
			this.numberActualDataSetsText = "0"; //$NON-NLS-1$
			this.numberReadErrorText = "0"; //$NON-NLS-1$
			this.numberLess2Text = "0"; //$NON-NLS-1$
			this.redDataSetLabel.setText(this.numberRedDataSetsText);
			this.actualDataSetNumber.setText(this.numberActualDataSetsText);
			this.numberReadErrorLabel.setText(this.numberReadErrorText);
			this.numberLess2Label.setText(this.numberLess2Text);
			this.readDataProgressBar.setSelection(0);
			if (this.taskBarItem != null)this.taskBarItem.setProgress(0);
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					UniLogDialog.this.numberRedDataSetsText = "0"; //$NON-NLS-1$
					UniLogDialog.this.numberActualDataSetsText = "0"; //$NON-NLS-1$
					UniLogDialog.this.numberReadErrorText = "0"; //$NON-NLS-1$
					UniLogDialog.this.redDataSetLabel.setText(UniLogDialog.this.numberRedDataSetsText);
					UniLogDialog.this.actualDataSetNumber.setText(UniLogDialog.this.numberActualDataSetsText);
					UniLogDialog.this.numberReadErrorLabel.setText(UniLogDialog.this.numberReadErrorText);
					UniLogDialog.this.numberLess2Label.setText(UniLogDialog.this.numberLess2Text);
					UniLogDialog.this.readDataProgressBar.setSelection(0);
					if (UniLogDialog.this.taskBarItem != null) UniLogDialog.this.taskBarItem.setProgress(0);
				}
			});
		}
	}

	/**
	 * function to reset all the buttons, normally called after data gathering finished
	 */
	public void resetButtons() {
		if (this.dialogShell != null && !this.dialogShell.isDisposed()) {
			if (Thread.currentThread().getId() == this.application.getThreadId()) {
				this.setButtonStateLiveGatherer(true);
				this.startLoggingButton.setEnabled(true);
				this.stopLoggingButton.setEnabled(false);
				this.closeButton.setEnabled(true);
				setClosePossible(true);
				if (this.taskBarItem != null)
					this.taskBarItem.setProgressState(SWT.DEFAULT);
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						UniLogDialog.this.setButtonStateLiveGatherer(true);
						UniLogDialog.this.readAdjustmentButton.setEnabled(true);
						UniLogDialog.this.startLoggingButton.setEnabled(true);
						UniLogDialog.this.stopLoggingButton.setEnabled(false);
						UniLogDialog.this.closeButton.setEnabled(true);
						setClosePossible(true);
						if (UniLogDialog.this.taskBarItem != null)
							UniLogDialog.this.taskBarItem.setProgressState(SWT.DEFAULT);
					}
				});
			}
		}
	}

	/**
	 * enable the button to store the configuration
	 * @param enabled
	 */
	public void enableStoreAdjustmentsButton(boolean enabled) {
		this.storeAdjustmentsButton.setEnabled(enabled);
	}

	/**
	 * query the selection index of the A1,A2,A3 modus type
	 * @return index position of the A1 modus array
	 */
	public int[] getIndexAnalogModus() {
		return new int[] {this.modusA1Position, this.modusA2Position, this.modusA3Position};
	}

	/**
	 * update the used timeStep_ms, this is used for all type of calculations
	 * @param timeIntervalIndex the index of the TIME_INTERVAL array
	 */
	void updateTimeStep_ms(int timeIntervalIndex) {
		switch (timeIntervalIndex) {
		case 0: // 1/16 sec
			this.device.setTimeStep_ms(1000.0 / 16);
			break;
		case 1: // 1/8 sec
			this.device.setTimeStep_ms(1000.0 / 8);
			break;
		default:
		case 2: // 1/4 sec
			this.device.setTimeStep_ms(1000.0 / 4);
			break;
		case 3: // 1/2 sec
			this.device.setTimeStep_ms(1000.0 / 2);
			break;
		case 4: // 1 sec
			this.device.setTimeStep_ms(1000.0 * 1);
			break;
		case 5: // 2 sec
			this.device.setTimeStep_ms(1000.0 * 2);
			break;
		case 6: // 5 sec
			this.device.setTimeStep_ms(1000.0 * 5);
			break;
		case 7: // 10 sec
			this.device.setTimeStep_ms(1000.0 * 10);
			break;
		case 8: // 30 sec
			this.device.setTimeStep_ms(1000.0 * 30);
			break;
		case 9: // 60 sec
			this.device.setTimeStep_ms(1000.0 * 60);
			break;
		case 10: // 120 sec
			this.device.setTimeStep_ms(1000.0 * 120);
			break;
		case 11: // 300 sec
			this.device.setTimeStep_ms(1000.0 * 300);
			break;
		}
		this.device.storeDeviceProperties();
	}

	/**
	 * check and update analog modus name, unit according device configuration
	 * @param configTabIndex the index of the configuration tabulator item
	 */
	public void updateActualConfigTabItemAnalogModi(int configTabIndex) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "updating configTab" + configTabIndex);
		if (this.dialogShell != null && !this.dialogShell.isDisposed()) { // dialog opened
			switch (configTabIndex) {
			case 1:
				this.configTab1.checkUpdateAnalog();
				break;
			case 2:
				this.configTab2.checkUpdateAnalog();
				break;
			case 3:
				this.configTab3.checkUpdateAnalog();
				break;
			case 4:
				this.configTab4.checkUpdateAnalog();
				break;
			}
		}
	}

	/**
	 * update buttons live gatherer dependent
	 */
	void setButtonStateLiveGatherer(boolean liveGathererEnabled) {
		this.isLiveGathererEnabled = liveGathererEnabled;
		if (this.dialogShell != null && !this.dialogShell.isDisposed()) {
			this.readAdjustmentButton.setEnabled(liveGathererEnabled);
			this.storeAdjustmentsButton.setEnabled(false);
			this.editConfigButton.setEnabled(false);
			this.readDataButton.setEnabled(liveGathererEnabled);
			this.stopDataButton.setEnabled(false);
			this.startLiveGatherButton.setEnabled(liveGathererEnabled);
			this.stopLiveGatherButton.setEnabled(!liveGathererEnabled);
			this.useConfigCombo.setEnabled(liveGathererEnabled);
			this.clearMemoryButton.setEnabled(liveGathererEnabled);
			this.closeButton.setEnabled(liveGathererEnabled);
		}
	}
}
