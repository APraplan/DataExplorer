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

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.exception.DataInconsitsentException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
  * Implementation for one channel tab, this will be initialized according number of available channels
  * @author Winfried Brügmann
 */
public class AkkuMasterChannelTab {
	final static Logger						log												= Logger.getLogger(AkkuMasterChannelTab.class.getName());

	AkkuMasterC4Dialog						parent;
	String												name;
	byte[]												channelSig;
	String[]											aCapacity;
	String[]											aCellCount;
	String[]											aAkkuTyp;
	String[]											aProgramm;
	String[]											aChargeCurrent_mA;
	String[]											aDischargeCurrent_mA;
	AkkuMasterC4SerialPort				serialPort;
	Channel												channel;
	Timer													timer;
	TimerTask											timerTask;

	CTabItem											channelTab;
	Button												captureOnlyButton;
	Group													programGroup;
	Group													captureOnlyGroup;
	CCombo												memoryNumberCombo;
	CCombo												capacityMilliAh;
	Group													akkuGroup;
	Text													chargeCurrentText;
	Button												stopDataGatheringButton;
	Button												startDataGatheringButton;
	Text													memoryNumberText;
	CCombo												dischargeCurrent;
	Text													dischargeCurrentText;
	CCombo												chargeCurrent;
	CCombo												program;
	Text													programText;
	Group													programTypeGroup;
	Text													akkuTypeText;
	CCombo												akkuType;
	CCombo												countCells;
	Text													countCellsText;
	Text													capacityText;
	Text													captureOnlyText;
	Button												programmButton;
	Composite											channelComposite;
	boolean												isCaptureOnly							= false;
	boolean												isDefinedProgram					= false;
	boolean												isDataGatheringEnabled		= false;
	boolean												isStopButtonEnabled				= false;
	String												capacityMilliAhValue			= "0"; //$NON-NLS-1$
	int														countCellsValue						= 0;
	int														akkuTypeValue							= 0;
	int														programValue							= 0;
	String												chargeCurrentValue				= "0"; //$NON-NLS-1$
	String												dischargeCurrentValue			= "0"; //$NON-NLS-1$
	int														memoryNumberValue					= 1;

	boolean												isCollectData							= false;
	boolean												isGatheredRecordSetVisible	= true;
	RecordSet											recordSet;
	int														retryCounter							= 10;
	long													timeStamp;
	boolean												isChargeCurrentAdded			= false;
	boolean												isDischargeCurrentAdded		= false;
	boolean												isCollectDataStopped			= true;
	boolean												isMemorySelectionChanged	= false;
	String												recordSetKey							= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272);

	int 													actualCurrent = 0;
	String 												processName = GDE.STRING_EMPTY;


	final Channels								channels;
	final DataExplorer	application;

	/**
	 * constructor initialization of one channel tab
	 * @param newName
	 * @param useChannel byte signature
	 * @param useSerialPort
	 * @param arrayCapacity
	 * @param arrayCellCount
	 * @param arrayAkkuTyp
	 * @param arrayProgramm
	 * @param arrayChargeCurrent_mA
	 * @param arrayDischargeCurrent_mA
	 */
	public AkkuMasterChannelTab(AkkuMasterC4Dialog useParent, String newName, byte[] useChannelSig, AkkuMasterC4SerialPort useSerialPort, Channel useChannel, String[] arrayCapacity,
			String[] arrayCellCount, String[] arrayAkkuTyp, String[] arrayProgramm, String[] arrayChargeCurrent_mA, String[] arrayDischargeCurrent_mA) {
		this.parent = useParent;
		this.name = newName;
		this.channelSig = useChannelSig;
		this.serialPort = useSerialPort;
		this.channel = useChannel;
		this.aCapacity = arrayCapacity;
		this.aCellCount = arrayCellCount;
		this.aAkkuTyp = arrayAkkuTyp;
		this.aProgramm = arrayProgramm;
		this.aChargeCurrent_mA = arrayChargeCurrent_mA;
		this.aDischargeCurrent_mA = arrayDischargeCurrent_mA;
		this.channels = Channels.getInstance();
		this.application = DataExplorer.getInstance();
	}

	/**
	 * add the tab to the dialog
	 */
	public void addChannelTab(CTabFolder tabFolder) {
		{
			this.channelTab = new CTabItem(tabFolder, SWT.NONE);
			this.channelTab.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.channelTab.setText(this.name);
			{ // begin channel composite
				this.channelComposite = new Composite(tabFolder, SWT.NONE);
				this.channelTab.setControl(this.channelComposite);
				this.channelComposite.setLayout(null);
				this.channelComposite.addMouseTrackListener(this.parent.getDevice().getDialog().mouseTrackerEnterFadeOut);
				this.channelComposite.addPaintListener(new PaintListener() {
					@Override
					public void paintControl(PaintEvent evt) {
						log.log(Level.FINEST, "channelComposite.widgetSelected, event=" + evt); //$NON-NLS-1$
						updateStartDataGatheringButton();
						updateStopDataGatheringButton();
					}
				});

				{ // begin capture only group
					this.captureOnlyGroup = new Group(this.channelComposite, SWT.NONE);
					this.captureOnlyGroup.setLayout(null);
					this.captureOnlyGroup.setBounds(12, 8, 400, 80);
					this.captureOnlyGroup.addMouseTrackListener(this.parent.getDevice().getDialog().mouseTrackerEnterFadeOut);
					this.captureOnlyGroup.addPaintListener(new PaintListener() {
						@Override
						public void paintControl(PaintEvent evt) {
							log.log(Level.FINEST, "captureOnlyGroup.widgetSelected, event=" + evt); //$NON-NLS-1$
							updateCaptureOnlyButton();
						}
					});
					{
						this.captureOnlyText = new Text(this.captureOnlyGroup, SWT.MULTI | SWT.WRAP);
						this.captureOnlyText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.captureOnlyText.setText(Messages.getString(MessageIds.GDE_MSGT1173));
						this.captureOnlyText.setBackground(this.application.COLOR_LIGHT_GREY);
						this.captureOnlyText.setBounds(51, GDE.IS_MAC_COCOA ? 25 : 40, 315, 37);
					}
					{
						this.captureOnlyButton = new Button(this.captureOnlyGroup, SWT.RADIO | SWT.LEFT);
						this.captureOnlyButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
						this.captureOnlyButton.setText(Messages.getString(MessageIds.GDE_MSGT1174));
						this.captureOnlyButton.setBounds(12, GDE.IS_MAC_COCOA ? 0 : 15, 310, 22);
						this.captureOnlyButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "captureOnlyButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (AkkuMasterChannelTab.this.captureOnlyButton.getSelection()) {
									try {
										AkkuMasterChannelTab.this.isCaptureOnly = true;
										AkkuMasterChannelTab.this.isDefinedProgram = false;
										updateAdjustedValues();
										AkkuMasterChannelTab.this.isDataGatheringEnabled = true;
										updateCaptureOnlyButton();
										updateProgramButton();
										updateStartDataGatheringButton();
									}
									catch (Exception e) {
										AkkuMasterChannelTab.this.captureOnlyButton.setSelection(false);
										AkkuMasterChannelTab.this.stopTimer();
										AkkuMasterChannelTab.this.application.openMessageDialog(AkkuMasterChannelTab.this.parent.getDialogShell(),
												Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
									}
								}
							}
						});
					}
				} // end capture only group

				{ // begin program group
					this.programGroup = new Group(this.channelComposite, SWT.NONE);
					this.programGroup.setLayout(null);
					this.programGroup.setBounds(12, 95, 400, 250);
					this.programGroup.addMouseTrackListener(this.parent.getDevice().getDialog().mouseTrackerEnterFadeOut);
					this.programGroup.addPaintListener(new PaintListener() {
						@Override
						public void paintControl(PaintEvent evt) {
							log.log(Level.FINEST, "programGroup.widgetSelected, event=" + evt); //$NON-NLS-1$
							updateProgramButton();
						}
					});
					{
						this.programmButton = new Button(this.programGroup, SWT.RADIO | SWT.LEFT);
						this.programmButton.setText(Messages.getString(MessageIds.GDE_MSGT1175));
						this.programmButton.setBounds(12, GDE.IS_MAC_COCOA ? 0 : 15, 295, 21);
						this.programmButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
						this.programmButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "programmButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (AkkuMasterChannelTab.this.programmButton.getSelection()) {
									try {
										AkkuMasterChannelTab.this.isCaptureOnly = false;
										AkkuMasterChannelTab.this.isDefinedProgram = true;
										updateAdjustedValues();
										AkkuMasterChannelTab.this.isDataGatheringEnabled = true;
										updateCaptureOnlyButton();
										updateProgramButton();
										updateStartDataGatheringButton();
									}
									catch (Exception e) {
										AkkuMasterChannelTab.this.programmButton.setSelection(false);
										AkkuMasterChannelTab.this.stopTimer();
										AkkuMasterChannelTab.this.application.openMessageDialog(AkkuMasterChannelTab.this.parent.getDialogShell(),
												Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
									}
								}
							}
						});
					}
					{
						this.akkuGroup = new Group(this.programGroup, SWT.NONE);
						RowLayout akkuRowLayout = new RowLayout(SWT.VERTICAL);
						akkuRowLayout.spacing = 3;
						akkuRowLayout.center = true;
						akkuRowLayout.justify = true;
						this.akkuGroup.setLayout(akkuRowLayout);
						this.akkuGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.akkuGroup.setText(Messages.getString(MessageIds.GDE_MSGT1176));
						this.akkuGroup.setBounds(15, GDE.IS_MAC_COCOA ? 25 : 40, 369, 75);
						this.akkuGroup.addPaintListener(new PaintListener() {
							@Override
							public void paintControl(PaintEvent evt) {
								log.log(Level.FINEST, "akkuGroup.widgetSelected, event=" + evt); //$NON-NLS-1$
								updateCapacityMilliAhText();
								updateCountCellSelection();
								updateAkkuType();
							}
						});
						{
							this.capacityText = new Text(this.akkuGroup, SWT.CENTER);
							this.capacityText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.capacityText.setLayoutData(new RowData(112, 18));
							this.capacityText.setText(Messages.getString(MessageIds.GDE_MSGT1177));
							this.capacityText.setBackground(this.application.COLOR_LIGHT_GREY);
							this.capacityText.setEditable(false);
						}
						{
							this.capacityMilliAh = new CCombo(this.akkuGroup, SWT.BORDER);
							this.capacityMilliAh.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.capacityMilliAh.setItems(this.aCapacity);
							this.capacityMilliAh.setText(this.aCapacity[5]);
							this.capacityMilliAh.setLayoutData(new RowData(112, GDE.IS_LINUX ? 22 : 20));
							this.capacityMilliAh.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "capacityMilliAh.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateCapacityMilliAhValue();
								}
							});
						}
						{
							this.countCellsText = new Text(this.akkuGroup, SWT.CENTER);
							this.countCellsText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.countCellsText.setBackground(this.application.COLOR_LIGHT_GREY);
							this.countCellsText.setLayoutData(new RowData(112, 18));
							this.countCellsText.setText(Messages.getString(MessageIds.GDE_MSGT1178));
							this.countCellsText.setEditable(false);
						}
						{
							this.countCells = new CCombo(this.akkuGroup, SWT.BORDER);
							this.countCells.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.countCells.setLayoutData(new RowData(112, GDE.IS_LINUX ? 22 : 20));
							this.countCells.setItems(this.aCellCount);
							this.countCells.setText(this.aCellCount[3]);
							this.countCells.setEditable(false);
							this.countCells.setBackground(this.application.COLOR_WHITE);
							this.countCells.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "countCells.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateCellCountValue();
								}
							});
						}
						{
							this.akkuTypeText = new Text(this.akkuGroup, SWT.CENTER);
							this.akkuTypeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.akkuTypeText.setBackground(this.application.COLOR_LIGHT_GREY);
							this.akkuTypeText.setLayoutData(new RowData(112, 18));
							this.akkuTypeText.setText(Messages.getString(MessageIds.GDE_MSGT1179));
							this.akkuTypeText.setDoubleClickEnabled(false);
							this.akkuTypeText.setDragDetect(false);
							this.akkuTypeText.setEditable(false);
						}
						{
							this.akkuType = new CCombo(this.akkuGroup, SWT.BORDER);
							this.akkuType.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.akkuType.setLayoutData(new RowData(112, GDE.IS_LINUX ? 22 : 20));
							this.akkuType.setItems(this.aAkkuTyp);
							this.akkuType.setText(this.aAkkuTyp[0]);
							this.akkuType.setEditable(false);
							this.akkuType.setBackground(this.application.COLOR_WHITE);
							this.akkuType.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "akkuType.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateAkkuTypeValue();
								}
							});
						}
						this.akkuGroup.layout();
					}
					{
						this.programTypeGroup = new Group(this.programGroup, SWT.NONE);
						this.programTypeGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.programTypeGroup.setBounds(15, GDE.IS_MAC_COCOA ? 95 : 115, 369, 130);
						this.programTypeGroup.setText(Messages.getString(MessageIds.GDE_MSGT1180));
						GridLayout programGrpLayout = new GridLayout();
						programGrpLayout.numColumns = 3;
						programGrpLayout.makeColumnsEqualWidth = true;
						this.programTypeGroup.setLayout(programGrpLayout);
						this.programTypeGroup.addPaintListener(new PaintListener() {
							@Override
							public void paintControl(PaintEvent evt) {
								log.log(Level.FINEST, "programTypeGroup.widgetSelected, event=" + evt); //$NON-NLS-1$
								updateProgramText();
								updateChargeCurrentText();
								updateDichargeCurrentText();
								updateMemoryNumberSelection();
							}
						});
						{
							this.programText = new Text(this.programTypeGroup, SWT.CENTER);
							this.programText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.programText.setBackground(this.application.COLOR_LIGHT_GREY);
							GridData gridDataText = new GridData(347, 18);
							gridDataText.horizontalAlignment = SWT.CENTER;
							gridDataText.horizontalSpan = 3;
							this.programText.setLayoutData(gridDataText);
							this.programText.setText(Messages.getString(MessageIds.GDE_MSGT1181));
						}
						{
							this.program = new CCombo(this.programTypeGroup, SWT.BORDER);
							this.program.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							GridData gridDataCombo = new GridData(347, GDE.IS_LINUX ? 22 : 20);
							gridDataCombo.horizontalAlignment = SWT.CENTER;
							gridDataCombo.horizontalSpan = 3;
							this.program.setLayoutData(gridDataCombo);
							this.program.setItems(this.aProgramm);
							this.program.select(2);
							this.program.setEditable(false);
							this.program.setBackground(this.application.COLOR_WHITE);
							this.program.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "program.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateProgramSelectionValue();
								}
							});
						}
						{
							this.chargeCurrentText = new Text(this.programTypeGroup, SWT.CENTER);
							this.chargeCurrentText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.chargeCurrentText.setBackground(this.application.COLOR_LIGHT_GREY);
							this.chargeCurrentText.setLayoutData(new GridData(112, 18));
							this.chargeCurrentText.setText(Messages.getString(MessageIds.GDE_MSGT1182));
							this.chargeCurrentText.setEditable(false);
						}
						{
							this.dischargeCurrentText = new Text(this.programTypeGroup, SWT.CENTER);
							this.dischargeCurrentText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.dischargeCurrentText.setBackground(this.application.COLOR_LIGHT_GREY);
							this.dischargeCurrentText.setLayoutData(new GridData(112, 18));
							this.dischargeCurrentText.setDragDetect(false);
							this.dischargeCurrentText.setDoubleClickEnabled(false);
							this.dischargeCurrentText.setText(Messages.getString(MessageIds.GDE_MSGT1183));
							this.dischargeCurrentText.setEditable(false);
						}
						{
							this.memoryNumberText = new Text(this.programTypeGroup, SWT.CENTER);
							this.memoryNumberText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.memoryNumberText.setBackground(this.application.COLOR_LIGHT_GREY);
							this.memoryNumberText.setLayoutData(new GridData(112, 18));
							this.memoryNumberText.setText(Messages.getString(MessageIds.GDE_MSGT1184));
							this.memoryNumberText.setEditable(false);
						}
						{
							this.chargeCurrent = new CCombo(this.programTypeGroup, SWT.BORDER);
							this.chargeCurrent.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.chargeCurrent.setLayoutData(new GridData(112, GDE.IS_LINUX ? 22 : 20));
							this.chargeCurrent.setItems(this.aChargeCurrent_mA);
							this.chargeCurrent.setText(this.aChargeCurrent_mA[5]);
							this.chargeCurrent.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "chargeCurrent.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateChargeCurrentValue();
								}
							});
						}
						{
							this.dischargeCurrent = new CCombo(this.programTypeGroup, SWT.BORDER);
							this.dischargeCurrent.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.dischargeCurrent.setLayoutData(new GridData(112, GDE.IS_LINUX ? 22 : 20));
							this.dischargeCurrent.setItems(this.aDischargeCurrent_mA);
							this.dischargeCurrent.setText(this.aDischargeCurrent_mA[5]);
							this.dischargeCurrent.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "dischargeCurrent.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateDischargeCurrentValue();
								}
							});
						}
						{
							this.memoryNumberCombo = new CCombo(this.programTypeGroup, SWT.BORDER);
							this.memoryNumberCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.memoryNumberCombo.setLayoutData(new GridData(112, GDE.IS_LINUX ? 22 : 20));
							this.memoryNumberCombo.setItems(new String[] { "0", "1", "2", "3", "4", "5", "6", "7" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
							this.memoryNumberCombo.select(1);
							this.memoryNumberCombo.setEditable(false);
							this.memoryNumberCombo.setBackground(this.application.COLOR_WHITE);
							this.memoryNumberCombo.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "memoryNumberCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateMemoryNumberValue();
									setMemorySelectionChanged(true);
								}
							});
						}
					}
					this.programTypeGroup.layout();
				} // end program group

				{
					this.startDataGatheringButton = new Button(this.channelComposite, SWT.PUSH | SWT.CENTER);
					this.startDataGatheringButton.setBounds(12, 360, 190, 28);
					this.startDataGatheringButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0277));
					this.startDataGatheringButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.startDataGatheringButton.setSelection(this.isCollectData);
					this.startDataGatheringButton.setEnabled(false);
					this.startDataGatheringButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "startAufzeichnungButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (!isCollectData()) {
								setCollectData(true);

								try {
									if (isCaptureOnly()) {
										updateAdjustedValues();
									}
									else {
										int programNumber = getProgramNumber();
										int waitTime_days = 1; // Integer.valueOf(warteZeitTage.getText()).intValue();
										int accuTyp = getAkkuType();
										int cellCount = getCellCount();
										int akkuCapacity = getAkkuCapacity();
										int dischargeCurrent_mA = getDischargeCurrent();
										int chargeCurrent_mA = getChargeCurrent();
										log.log(Level.FINE, " programNumber = " + programNumber + " waitTime_days = " + waitTime_days + " accuTyp = " + accuTyp + " cellCount = " + cellCount //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
												+ " akkuCapacity = " + akkuCapacity + " dischargeCurrent_mA = " + dischargeCurrent_mA + " chargeCurrent_mA = " + chargeCurrent_mA); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
										AkkuMasterChannelTab.this.serialPort.writeNewProgram(AkkuMasterChannelTab.this.channelSig, programNumber, waitTime_days, accuTyp, cellCount, akkuCapacity, dischargeCurrent_mA, chargeCurrent_mA);

										if (isMemorySelectionChanged()) {
											int memoryNumber = getMemoryNumberSelectionIndex();
											log.log(Level.FINE, "memoryNumber =" + memoryNumber); //$NON-NLS-1$
											AkkuMasterChannelTab.this.serialPort.setMemoryNumberCycleCountSleepTime(AkkuMasterChannelTab.this.channelSig, memoryNumber, 2, 2000);
										}

										if (AkkuMasterChannelTab.this.parent.getMaxCurrent() < AkkuMasterChannelTab.this.parent.getActiveCurrent() + dischargeCurrent_mA || AkkuMasterChannelTab.this.parent.getMaxCurrent() < AkkuMasterChannelTab.this.parent.getActiveCurrent() + chargeCurrent_mA) {
											AkkuMasterChannelTab.this.application.openMessageDialog(AkkuMasterChannelTab.this.parent.getDialogShell(), Messages.getString(MessageIds.GDE_MSGW1100, new Object[] { dischargeCurrent_mA, chargeCurrent_mA } ));
											setCollectData(false);
											AkkuMasterChannelTab.this.startDataGatheringButton.setSelection(false);
											return;
										}

										AkkuMasterChannelTab.this.serialPort.start(AkkuMasterChannelTab.this.channelSig);
										AkkuMasterChannelTab.this.serialPort.ok(AkkuMasterChannelTab.this.channelSig);
									}
									AkkuMasterChannelTab.this.isDataGatheringEnabled = false;
									updateStartDataGatheringButton();
									AkkuMasterChannelTab.this.isStopButtonEnabled = true;
									updateStopDataGatheringButton();

									AkkuMasterChannelTab.this.channels.switchChannel(AkkuMasterChannelTab.this.channel.getName());
									// prepare timed data gatherer thread
									int delay = 0;
									int period = (int)AkkuMasterChannelTab.this.application.getActiveDevice().getTimeStep_ms(); // repeat every 10 sec.
									setTimer(new Timer());
									setTimerTask(new TimerTask() {
										HashMap<String, Object>	data; // [8]

										@Override
										public void run() {
											/* [0] String Aktueller Prozessname 			"4 ) Laden" = AkkuMaster aktiv Laden
											 * [1] int 		Aktuelle Fehlernummer				"0" = kein Fehler
											 * [2] int		Aktuelle Akkuspannung 			[mV]
											 * [3] int 		Aktueller Prozesssstrom 		[mA] 	(laden/entladen)
											 * [4] int 		Aktuelle Prozesskapazität		[mAh] (laden/entladen)
											 * [5] int 		Errechnete Leistung					[mW]
											 * [6] int		Errechnete Energie					[mWh]
											 * [7] int		Prozesszeit									[msec]
											 */
											try {
												this.data = AkkuMasterChannelTab.this.serialPort.getData(AkkuMasterChannelTab.this.channelSig);
												// check for no error state
												log.log(Level.FINE, "error state = " + this.data.get(AkkuMasterC4SerialPort.PROCESS_ERROR_NO)); //$NON-NLS-1$
												if (0 == (Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_ERROR_NO)) {
													AkkuMasterChannelTab.this.processName = ((String) this.data.get(AkkuMasterC4SerialPort.PROCESS_NAME)).split(" ")[1].trim(); //$NON-NLS-1$
													log.log(Level.FINE, AkkuMasterChannelTab.this.name + ": processName = " + processName); //$NON-NLS-1$
													AkkuMasterChannelTab.this.application.setStatusMessage(GDE.STRING_EMPTY);

													// check if device is ready for data capturing
													int processNumber = Integer.valueOf(((String) this.data.get(AkkuMasterC4SerialPort.PROCESS_NAME)).split(" ")[0]).intValue(); //$NON-NLS-1$
													if (processNumber == 1 || processNumber == 2) { // 1=Laden; 2=Entladen - AkkuMaster activ
														// check state change waiting to discharge to charge
														// check if a record set matching for re-use is available and prepare a new if required
														log.log(Level.FINE, AkkuMasterChannelTab.this.channel.getName() + "=" + AkkuMasterChannelTab.this.channel.size()); //$NON-NLS-1$
														if (AkkuMasterChannelTab.this.channel.size() == 0
																|| !AkkuMasterChannelTab.this.channel.getRecordSetNames()[AkkuMasterChannelTab.this.channel.getRecordSetNames().length - 1].endsWith(" " + processName) //$NON-NLS-1$
																|| (new Date().getTime() - getTimeStamp()) > 30000 || isCollectDataStopped()) {
															setCollectDataStopped(false);
															// record set does not exist or is outdated, build a new name and create
															AkkuMasterChannelTab.this.recordSetKey = AkkuMasterChannelTab.this.channel.getNextRecordSetNumber() + ") " + processName; //$NON-NLS-1$
															AkkuMasterChannelTab.this.channel.put(AkkuMasterChannelTab.this.recordSetKey, RecordSet.createRecordSet(AkkuMasterChannelTab.this.recordSetKey, AkkuMasterChannelTab.this.application.getActiveDevice(), AkkuMasterChannelTab.this.channel.getNumber(), true, false, true));
															AkkuMasterChannelTab.this.channel.applyTemplateBasics(AkkuMasterChannelTab.this.recordSetKey);
															log.log(Level.FINE, AkkuMasterChannelTab.this.recordSetKey + " created for channel " + AkkuMasterChannelTab.this.channel.getName()); //$NON-NLS-1$
															if (AkkuMasterChannelTab.this.channel.getActiveRecordSet() == null) AkkuMasterChannelTab.this.channel.setActiveRecordSet(AkkuMasterChannelTab.this.recordSetKey);
															AkkuMasterChannelTab.this.recordSet = AkkuMasterChannelTab.this.channel.get(AkkuMasterChannelTab.this.recordSetKey);
															AkkuMasterChannelTab.this.recordSet.setAllDisplayable();
															// switch the active record set if the current record set is child of active channel
															if (AkkuMasterChannelTab.this.channel.getName().equals(AkkuMasterChannelTab.this.channels.getActiveChannel().getName())) {
																AkkuMasterChannelTab.this.application.getMenuToolBar().addRecordSetName(AkkuMasterChannelTab.this.recordSetKey);
																AkkuMasterChannelTab.this.channels.getActiveChannel().switchRecordSet(AkkuMasterChannelTab.this.recordSetKey);
															}
															// update discharge / charge current display
															AkkuMasterChannelTab.this.actualCurrent = ((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_CURRENT)).intValue();
															updateCurrentStatus();
														}
														else {
															log.log(Level.FINE, "re-using " + AkkuMasterChannelTab.this.recordSetKey); //$NON-NLS-1$
														}
														setTimeStamp();

														// prepare the data for adding to record set
														// build the point array according curves from record set
														int[] points = new int[AkkuMasterChannelTab.this.recordSet.size()];

														points[0] = Integer.valueOf((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_VOLTAGE)).intValue(); //Spannung 	[mV]
														points[1] = Integer.valueOf((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_CURRENT)).intValue(); //Strom 			[mA]
														// display adaption * 1000  -  / 1000
														points[2] = Integer.valueOf((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_CAPACITY)).intValue() * 1000; //Kapazität	[mAh]
														points[3] = Integer.valueOf((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_POWER)).intValue() / 1000; //Leistung		[mW]
														points[4] = Integer.valueOf((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_ENERGIE)).intValue() / 1000; //Energie		[mWh]
														log.log(Level.FINE, points[0] + " mV; " + points[1] + " mA; " + points[2] + " mAh; " + points[3] + " mW; " + points[4] + " mWh"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

														AkkuMasterChannelTab.this.recordSet.addPoints(points);

														if (AkkuMasterChannelTab.this.recordSet.isChildOfActiveChannel() && AkkuMasterChannelTab.this.recordSet.equals(AkkuMasterChannelTab.this.channels.getActiveChannel().getActiveRecordSet())) {
															AkkuMasterChannelTab.this.application.updateAllTabs(false);														}
													}
													else {
														// enable switching records sets
														if (0 == (setRetryCounter(getRetryCounter() - 1))) {
															stopTimer();
															log.log(Level.WARNING, "Timer stopped AkkuMaster inactiv - " + AkkuMasterChannelTab.this.name); //$NON-NLS-1$
															AkkuMasterChannelTab.this.application.setStatusMessage("=====> Timer stopped AkkuMaster inactiv - " + AkkuMasterChannelTab.this.name, SWT.COLOR_RED); //$NON-NLS-1$
															setRetryCounter(10);
															updateCurrentStatus();
															AkkuMasterChannelTab.this.isChargeCurrentAdded = false;
															AkkuMasterChannelTab.this.isDischargeCurrentAdded = false;
														}
														else {
															log.log(Level.FINE, "Waiting for device activation - " + AkkuMasterChannelTab.this.name); //$NON-NLS-1$
															AkkuMasterChannelTab.this.application.setStatusMessage("=====> Waiting for device activation - " + AkkuMasterChannelTab.this.name); //$NON-NLS-1$
														}
													}
												}
												else { // some error state
													log.log(Level.FINE, "canceling timer due to error"); //$NON-NLS-1$
													updateCurrentStatus();
													AkkuMasterChannelTab.this.isChargeCurrentAdded = false;
													AkkuMasterChannelTab.this.isDischargeCurrentAdded = false;
													if (!isCaptureOnly()) try {
														AkkuMasterChannelTab.this.serialPort.stop(AkkuMasterChannelTab.this.channelSig);
													}
													catch (IOException e) {
														log.log(Level.SEVERE, e.getMessage(), e);
													}
													setCollectData(false);
													stopTimer();
													AkkuMasterChannelTab.this.application.openMessageDialog(AkkuMasterChannelTab.this.parent.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0027));
												}
											}
											catch (DataInconsitsentException e) {
												updateCurrentStatus();
												AkkuMasterChannelTab.this.isChargeCurrentAdded = false;
												AkkuMasterChannelTab.this.isDischargeCurrentAdded = false;
												// exception is logged where it is thrown first
												log.log(Level.SEVERE, e.getMessage(), e);
												setCollectData(false);
												stopTimer();
												if (!AkkuMasterChannelTab.this.parent.isDisposed()) AkkuMasterChannelTab.this.application.openMessageDialog(AkkuMasterChannelTab.this.parent.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0028, new Object[] {e.getClass().getSimpleName(), e.getMessage()} ));
											}
											catch (Exception e) {
												updateCurrentStatus();
												AkkuMasterChannelTab.this.isChargeCurrentAdded = false;
												AkkuMasterChannelTab.this.isDischargeCurrentAdded = false;

												// exception is logged where it is thrown first
												log.log(Level.SEVERE, e.getMessage(), e);
												setCollectData(false);
												stopTimer();
												if (!AkkuMasterChannelTab.this.parent.isDisposed()) AkkuMasterChannelTab.this.application.openMessageDialog(AkkuMasterChannelTab.this.parent.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0029, new Object[] {e.getClass().getSimpleName(), e.getMessage()} ));
											}
										}
									});
									getTimer().scheduleAtFixedRate(getTimerTask(), delay, period);

								}
								catch (Throwable e1) {
									updateCurrentStatus();
									AkkuMasterChannelTab.this.isChargeCurrentAdded = false;
									AkkuMasterChannelTab.this.isDischargeCurrentAdded = false;

									AkkuMasterChannelTab.this.isStopButtonEnabled = false;
									AkkuMasterChannelTab.this.startDataGatheringButton.setSelection(true);
									AkkuMasterChannelTab.this.application.openMessageDialog(AkkuMasterChannelTab.this.parent.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0026));
								}
							}
						}
					});
				}
				{
					this.stopDataGatheringButton = new Button(this.channelComposite, SWT.PUSH | SWT.CENTER);
					this.stopDataGatheringButton.setBounds(225, 360, 190, 28);
					this.stopDataGatheringButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0278));
					this.stopDataGatheringButton.setEnabled(false);
					this.stopDataGatheringButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.stopDataGatheringButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "stopAuzeichnungButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							updateCurrentStatus();
							AkkuMasterChannelTab.this.isChargeCurrentAdded = false;
							AkkuMasterChannelTab.this.isDischargeCurrentAdded = false;

							updateDialogAfterStop();
							if (!isCaptureOnly()) try {
								AkkuMasterChannelTab.this.serialPort.stop(AkkuMasterChannelTab.this.channelSig);
							}
							catch (Exception e) {
								e.printStackTrace();
							}
							stopTimer();
							// hope this is the right record set
							AkkuMasterChannelTab.this.recordSet = AkkuMasterChannelTab.this.channels.getActiveChannel().get(AkkuMasterChannelTab.this.recordSetKey);
							AkkuMasterChannelTab.this.application.updateStatisticsData();
							//AkkuMasterChannelTab.this.application.updateDataTable(AkkuMasterChannelTab.this.recordSet.getName());
						}
					});
				}
			} // end channel composite
		}
	}

	/**
	 * @param actualCurrent
	 * @param processName
	 */
	private void updateCurrentStatus() {
		log.log(Level.FINE, "'" + AkkuMasterChannelTab.this.processName.trim() + "' - " + AkkuMasterChannelTab.this.isChargeCurrentAdded + ", " + AkkuMasterChannelTab.this.isDischargeCurrentAdded);
		//charge
		if (AkkuMasterChannelTab.this.processName.trim().equalsIgnoreCase(Messages.getString(MessageIds.GDE_MSGT1126).trim())) {
			if(this.isChargeCurrentAdded == false) {
				AkkuMasterChannelTab.this.parent.addTotalChargeCurrent(AkkuMasterChannelTab.this.actualCurrent);
				this.isChargeCurrentAdded = true;
			}
			else {
				AkkuMasterChannelTab.this.parent.subtractTotalChargeCurrent(AkkuMasterChannelTab.this.actualCurrent);
				this.isChargeCurrentAdded = false;
			}
			if(this.isDischargeCurrentAdded == true) { //discharge -> charge
				AkkuMasterChannelTab.this.parent.subtractTotalDischargeCurrent(AkkuMasterChannelTab.this.actualCurrent);
				this.isDischargeCurrentAdded = false;
			}
		}
		//discharge
		else if (AkkuMasterChannelTab.this.processName.trim().equalsIgnoreCase(Messages.getString(MessageIds.GDE_MSGT1127).trim())) {
			if (this.isDischargeCurrentAdded == false) {
				AkkuMasterChannelTab.this.parent.addTotalDischargeCurrent(AkkuMasterChannelTab.this.actualCurrent);
				this.isDischargeCurrentAdded = true;
			}
			else {
				AkkuMasterChannelTab.this.parent.subtractTotalDischargeCurrent(AkkuMasterChannelTab.this.actualCurrent);
				this.isDischargeCurrentAdded = false;
			}
			if (this.isChargeCurrentAdded == true) { //charge -> discharge
				AkkuMasterChannelTab.this.parent.subtractTotalChargeCurrent(AkkuMasterChannelTab.this.actualCurrent);
				this.isChargeCurrentAdded = false;
			}
		}
		else { // timer stopped
			if (this.isChargeCurrentAdded == true) {
				AkkuMasterChannelTab.this.parent.subtractTotalChargeCurrent(AkkuMasterChannelTab.this.actualCurrent);
				this.isChargeCurrentAdded = false;
			}
			if (this.isDischargeCurrentAdded == true) {
				AkkuMasterChannelTab.this.parent.subtractTotalDischargeCurrent(AkkuMasterChannelTab.this.actualCurrent);
				this.isDischargeCurrentAdded = false;
			}
		}
		log.log(Level.FINE, "'" + AkkuMasterChannelTab.this.processName.trim() + "' - " + AkkuMasterChannelTab.this.isChargeCurrentAdded + ", " + AkkuMasterChannelTab.this.isDischargeCurrentAdded);
	}

	/**
	 * @throws IOException
	 */
	void updateAdjustedValues() throws Exception {
		// update channel tab with values red from device
		if (!this.serialPort.isConnected()) {
			this.serialPort.open();
		}
		String[] configuration = this.serialPort.getConfiguration(this.channelSig);
		if (log.isLoggable(Level.FINER)) this.serialPort.print(configuration);
		if (!configuration[0].equals("0")) { // AkkuMaster somehow active			 //$NON-NLS-1$
			this.programValue = Integer.valueOf(configuration[2].split(" ")[0]).intValue() - 1; //$NON-NLS-1$
			this.program.setText(this.aProgramm[this.programValue]);

			this.akkuTypeValue = Integer.valueOf(configuration[3].split(" ")[0]).intValue(); //$NON-NLS-1$
			this.akkuType.setText(this.aAkkuTyp[this.akkuTypeValue]);

			this.countCellsValue = Integer.valueOf(configuration[4].split(" ")[0]).intValue() - 1; //$NON-NLS-1$
			this.countCells.select(this.countCellsValue);

			this.capacityMilliAhValue = configuration[5].split(" ")[0]; //$NON-NLS-1$
			this.capacityMilliAh.setText(this.capacityMilliAhValue);

			this.chargeCurrentValue = configuration[7].split(" ")[0]; //$NON-NLS-1$
			this.chargeCurrent.setText(this.chargeCurrentValue);

			this.dischargeCurrentValue = configuration[6].split(" ")[0]; //$NON-NLS-1$
			this.dischargeCurrent.setText(this.dischargeCurrentValue);

			String[] adjustments = this.serialPort.getAdjustedValues(this.channelSig);
			this.memoryNumberValue = Integer.valueOf(adjustments[0].split(" ")[0]).intValue(); //$NON-NLS-1$
			this.memoryNumberCombo.select(this.memoryNumberValue);
			if (log.isLoggable(Level.FINER)) this.serialPort.print(adjustments);
		}
	}

	/**
	 * stop the timer task thread, this tops data capturing
	 */
	public void stopTimer() {
		if (this.timerTask != null) this.timerTask.cancel();
		if (this.timer != null) {
			this.timer.cancel();
			this.timer.purge();
		}

		this.isCollectData = false;
		this.isCollectDataStopped = true;

		if (Thread.currentThread().getId() == AkkuMasterChannelTab.this.application.getThreadId()) {
			updateDialogAfterStop();
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					updateDialogAfterStop();
				}
			});
		}
	}

	/**
	 * updates dialog UI after stop timer operation
	 */
	void updateDialogAfterStop() {
		this.isDataGatheringEnabled = false;
		this.isStopButtonEnabled = false;
		this.isCaptureOnly = false;
		this.isDefinedProgram = false;
		this.isMemorySelectionChanged = false;
		this.startDataGatheringButton.setEnabled(this.isDataGatheringEnabled);
		this.stopDataGatheringButton.setEnabled(this.isStopButtonEnabled);

		this.captureOnlyButton.setSelection(this.isCaptureOnly);
		this.programmButton.setSelection(this.isDefinedProgram);
	}

	public boolean isDataColletionActive() {
		return isCollectData() || !isCollectDataStopped();
	}

	/**
	 * @return the isCollectData
	 */
	public boolean isCollectData() {
		return this.isCollectData;
	}

	/**
	 * @return the isCollectDataStopped
	 */
	public boolean isCollectDataStopped() {
		return this.isCollectDataStopped;
	}

	/**
	 * update the StartDataGatheringButton with the actual value of isDataGatheringEnabled
	 */
	void updateStartDataGatheringButton() {
		this.startDataGatheringButton.setEnabled(this.isDataGatheringEnabled);
	}

	/**
	 * update the StopDataGatheringButton with the actual value of isDataGatheringEnabled
	 */
	void updateStopDataGatheringButton() {
		this.stopDataGatheringButton.setEnabled(this.isStopButtonEnabled);
	}

	/**
	 * update the CaptureOnlyButton with the actual value of isCaptureOnly
	 */
	void updateCaptureOnlyButton() {
		this.captureOnlyButton.setSelection(this.isCaptureOnly);
	}

	/**
	 *
	 */
	void updateProgramButton() {
		this.programmButton.setSelection(this.isDefinedProgram);
	}

	/**
	 *
	 */
	void updateCapacityMilliAhText() {
		this.capacityMilliAh.setText(this.capacityMilliAhValue);
	}

	/**
	 *
	 */
	void updateCountCellSelection() {
		this.countCells.select(this.countCellsValue);
	}

	/**
	 *
	 */
	void updateAkkuType() {
		this.akkuType.setText(this.aAkkuTyp[this.akkuTypeValue]);
	}

	/**
	 *
	 */
	void updateCapacityMilliAhValue() {
		this.capacityMilliAhValue = this.capacityMilliAh.getText();
	}

	/**
	 *
	 */
	void updateCellCountValue() {
		this.countCellsValue = this.countCells.getSelectionIndex();
	}

	/**
	 *
	 */
	void updateAkkuTypeValue() {
		this.akkuTypeValue = this.akkuType.getSelectionIndex();
	}

	/**
	 *
	 */
	void updateProgramText() {
		this.program.setText(this.aProgramm[this.programValue]);
	}

	/**
	 *
	 */
	void updateChargeCurrentText() {
		this.chargeCurrent.setText(this.chargeCurrentValue);
	}

	/**
	 *
	 */
	void updateDichargeCurrentText() {
		this.dischargeCurrent.setText(this.dischargeCurrentValue);
	}

	/**
	 *
	 */
	void updateMemoryNumberSelection() {
		this.memoryNumberCombo.select(this.memoryNumberValue);
	}

	/**
	 *
	 */
	void updateProgramSelectionValue() {
		this.programValue = this.program.getSelectionIndex() + 1;
	}

	/**
	 *
	 */
	void updateChargeCurrentValue() {
		this.chargeCurrentValue = this.chargeCurrent.getText();
	}

	/**
	 *
	 */
	void updateDischargeCurrentValue() {
		this.dischargeCurrentValue = this.dischargeCurrent.getText();
	}

	/**
	 *
	 */
	void updateMemoryNumberValue() {
		this.memoryNumberValue = this.memoryNumberCombo.getSelectionIndex();
		this.memoryNumberCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_CYAN));
	}

	/**
	 * @param enabled the isMemorySelectionChanged to set
	 */
	void setMemorySelectionChanged(boolean enabled) {
		this.isMemorySelectionChanged = enabled;
	}

	/**
	 * @param enabled the isCollectData to set
	 */
	void setCollectData(boolean enabled) {
		this.isCollectData = enabled;
	}

	/**
	 * @return the isCaptureOnly
	 */
	boolean isCaptureOnly() {
		return this.isCaptureOnly;
	}

	/**
	 * @return
	 */
	int getProgramNumber() {
		return Integer.valueOf(this.program.getText().split(" ")[0]).intValue(); //$NON-NLS-1$
	}

	/**
	 * @return
	 */
	int getAkkuType() {
		return Integer.valueOf(this.akkuType.getText().split(" ")[0]).intValue(); //$NON-NLS-1$
	}

	/**
	 * @return
	 */
	int getCellCount() {
		return Integer.valueOf(this.countCells.getText().split(" ")[0]).intValue(); //$NON-NLS-1$
	}

	/**
	 * @return
	 */
	int getAkkuCapacity() {
		return Integer.valueOf(this.capacityMilliAh.getText()).intValue();
	}

	/**
	 * @return
	 */
	int getDischargeCurrent() {
		return Integer.valueOf(this.dischargeCurrent.getText()).intValue();
	}

	/**
	 * @return
	 */
	int getChargeCurrent() {
		return Integer.valueOf(this.chargeCurrent.getText()).intValue();
	}

	/**
	 * @return the isMemorySelectionChanged
	 */
	boolean isMemorySelectionChanged() {
		return this.isMemorySelectionChanged;
	}

	/**
	 * @return
	 */
	int getMemoryNumberSelectionIndex() {
		return this.memoryNumberCombo.getSelectionIndex();
	}

	/**
	 * @param newTimer the timer to set
	 */
	public void setTimer(Timer newTimer) {
		this.timer = newTimer;
	}

	/**
	 * @return the timer
	 */
	public Timer getTimer() {
		return this.timer;
	}

	/**
	 * @param newTimerTask the timerTask to set
	 */
	void setTimerTask(TimerTask newTimerTask) {
		this.timerTask = newTimerTask;
	}

	/**
	 * set timeStamp using Date().getTime()
	 */
	public void setTimeStamp() {
		this.timeStamp = new Date().getTime();
	}

	/**
	 * @return the timeStamp
	 */
	public long getTimeStamp() {
		return this.timeStamp;
	}

	/**
	 * @param enabled the isCollectDataStopped to set
	 */
	void setCollectDataStopped(boolean enabled) {
		this.isCollectDataStopped = enabled;
	}

	/**
	 * @return the name
	 */
	String getName() {
		return this.name;
	}

	/**
	 * @return the timerTask
	 */
	public TimerTask getTimerTask() {
		return this.timerTask;
	}

	/**
	 * @return the isChargeCurrentAdded
	 */
	public boolean isChargeCurrentAdded() {
		return this.isChargeCurrentAdded;
	}

	/**
	 * @return the isDischargeCurrentAdded
	 */
	public boolean isDischargeCurrentAdded() {
		return this.isDischargeCurrentAdded;
	}

	/**
	 * @param newRetryCounter the retryCounter to set
	 */
	public int setRetryCounter(int newRetryCounter) {
		return this.retryCounter = newRetryCounter;
	}

	/**
	 * @return the retryCounter
	 */
	public int getRetryCounter() {
		return this.retryCounter;
	}

	/**
	 * @return the actualCurrent
	 */
	public int getActualCurrent() {
		return actualCurrent;
	}
}
