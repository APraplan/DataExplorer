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
import gde.device.DataTypes;
import gde.device.smmodellbau.unilog2.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

/**
 * class to implement SM UniLog2 basic configuration panel 1
 */
public class UniLog2SetupConfiguration1 extends org.eclipse.swt.widgets.Composite {
	final static Logger							log										= Logger.getLogger(UniLog2SetupConfiguration1.class.getName());

	final UniLog2Dialog							dialog;
	final DataExplorer							application;
	final UniLog2SetupReaderWriter	configuration;
	
	CLabel													serialNumberLabel, firmwareLabel;
	Text														serialNumberText, firmwareText;

	Group														commonAdjustmentsGroup, logStartStopGroup;
	CLabel													dataRateLabel, currentSensorTypeLabel, propellerBladesLabel, motorPolsLabel, gearFactorLabel, varioTriggerLevelLabel, varioTriggerSinkLevelLabel, varioToneLabel,
			varioFactorLabel, varioFilterLabel;
	CLabel													limiterModusLabel, energyLimitLabel, autoStartCurrentUnitLabel, autoStartRxUnitLabel, autoStartTimeUnitLabel, a1ModusLabel, a2ModusLabel, a3ModusLabel,
			minMaxRxLabel, capacityResetLabel, currentOffsetLabel, telemetrieTypeLabel, autoStopLabel;
	Text														gearFactorText, varioTriggerLevelText, varioTriggerSinkLevelText, energyLimitText;
	Slider													gearFactorSlider, varioTriggerLevelSlider, varioTriggerSinkLevelSlider, energyLimitSlider;
	CCombo													dataRateCombo, currentSensorCombo, propBladesCombo, varioToneCombo, limiterModusCombo, minMaxRxCombo, capacityResetCombo, currentOffsetCombo, autoStopCombo,
			autoStartCurrentCombo, autoStartRxCombo, autoStartTimeCombo;
	CCombo													a1ModusCombo, a2ModusCombo, a3ModusCombo, varioFactorCombo, varioFilterCombo;
	CCombo													telemetrieTypeCombo;
	Button													autoStartCurrentButton, autoStartRxButton, autoStartTimeButton;
	Composite												fillerComposite;
	Composite												addonComposite;
	CLabel													frskyIdLabel;
	Button													fixSerialNumberButton;
	Button													robbeTBoxButton;
	CCombo													frskyIdCombo;

	final String[]									dataRateValues				= { " 20 Hz", " 10 Hz", "  5 Hz", "  2 Hz", "  1 Hz" };
	final String[]									currentSensorTypes		= { "  20 A", " 40/80 A", " 150 A", " 400 A" };
	final String[]									analogModi						= Messages.getString(MessageIds.GDE_MSGT2549).split(GDE.STRING_COMMA);
	final String[]									numberProbMotorPoles	= { " 1 / 2", " 2 / 4", " 3 / 6", " 4 / 8", " 5 / 10", " 6 / 12", " 7 / 14" };
	final String[]									currentStartValues		= { "  1", "  2", "  3", "  4", "  5", "  6", "  7", "  8", "  9", " 10" };
	final String[]									rxStartValues					= { "  1.1", "  1.2", "  1.3", "  1.4", "  1.5", "  1.6", "  1.7", "  1.8", "  1.9", Messages.getString(MessageIds.GDE_MSGT2508) };
	final String[]									timeStartValues				= { "  5", " 10", " 15", " 20", " 25", " 30", " 35", " 40", " 45", " 50", " 55", " 60", " 65", " 70", " 75", " 80", " 85", " 90" };
	final String[]									voltageRxValues				= { "  3.00", "  3.25", "  3.50", "  3.75", "  4.00", "  4.25", "  4.50", "  4.75", "  4.80", "  4.85", "  4.90", "  4.95", "  5.00", "  5.05",
			"  5.10", "  5.15", "  5.20", "  5.25", "  5.50", "  6.00", "  6.25", "  6.50", "  6.75", "  7.00", "  7.25", "  7.50", "  7.75", "  8.00" };
	final String[]									frskyIDs							= { " 0x00", " 0xA1", " 0x22", " 0x83", " 0xE4", " 0x45", " 0xC6", " 0x67", " 0x48", " 0xE9", " 0x6A", " 0xCB", " 0xAC", " 0x0D", " 0x8E",
			" 0x2F", " 0xD0", " 0x71", " 0xF2", " 0x53", " 0x34", " 0x95", " 0x16", " 0xB7", " 0x98", " 0x39", " 0xBA", " 0x1B" };

	final String[]									telemetrieTypes				= { " - - - ", " Futaba", " JR DMSS", " HoTT GAM", " HoTT EAM", " HoTT ESC", " JetiDuplex", " M-Link", " FrSky", " HoTT Vario", " Spektrum" };
	final String[]									capacityResets				= Messages.getString(MessageIds.GDE_MSGT2581).split(GDE.STRING_COMMA);
	final String[]									currentOffsets				= Messages.getString(MessageIds.GDE_MSGT2582).split(GDE.STRING_COMMA);
	final String[]									varioFactors;
	final String[]									varioFilters					= Messages.getString(MessageIds.GDE_MSGT2588).split(GDE.STRING_COMMA);

	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public static void main(String[] args) {
		showGUI();
	}

	/**
	* Auto-generated method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public static void showGUI() {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		UniLog2SetupConfiguration1 inst = new UniLog2SetupConfiguration1(shell, SWT.NULL);
		Point size = inst.getSize();
		shell.setLayout(new FillLayout());
		shell.layout();
		if (size.x == 0 && size.y == 0) {
			inst.pack();
			shell.pack();
		}
		else {
			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
			shell.setSize(shellBounds.width, shellBounds.height);
		}
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
	}

	/**
	* Constructor matching the auto-generated method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public UniLog2SetupConfiguration1(Composite parent, int style) {
		super(parent, style);
		this.dialog = null;
		this.configuration = null;
		this.application = null;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 41; i++) {
			sb.append(String.format(Locale.ENGLISH, " %2.1f ,", 1 + i / 10.0));
		}
		this.varioFactors = sb.toString().split(GDE.STRING_COMMA);
		initGUI();
	}

	/**
	 * constructor configuration panel 1
	 * @param parent
	 * @param style
	 * @param dialog
	 * @param configuration
	 */
	public UniLog2SetupConfiguration1(Composite parent, int style, UniLog2Dialog useDialog, UniLog2SetupReaderWriter useConfiguration) {
		super(parent, style);
		SWTResourceManager.registerResourceUser(this);
		this.dialog = useDialog;
		this.configuration = useConfiguration;
		this.application = DataExplorer.getInstance();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 41; i++) {
			sb.append(String.format(Locale.ENGLISH, " %2.1f ,", 1 + i / 10.0));
		}
		this.varioFactors = sb.toString().split(GDE.STRING_COMMA);
		initGUI();
		updateValues();
	}

	/**
	 * update values according to red configuration values
	 */
	public void updateValues() {
		this.serialNumberText.setText(GDE.STRING_EMPTY + this.configuration.serialNumber);
		this.firmwareText.setText(String.format(" %.2f", this.configuration.firmwareVersion / 100.0)); //$NON-NLS-1$

		this.dataRateCombo.select(this.configuration.dataRate - 1); //remove 50 Hz
		this.currentSensorCombo.select(this.configuration.currentSensorType);
		this.a1ModusCombo.select(this.configuration.modusA1);
		this.a2ModusCombo.select(this.configuration.modusA2);
		this.a3ModusCombo.select(this.configuration.modusA3);
		this.propBladesCombo.select(this.configuration.numberProb_MotorPole - 1);
		this.gearFactorSlider.setSelection(this.configuration.gearFactor - 100);
		this.gearFactorText.setText(String.format(Locale.ENGLISH, " %.2f", this.configuration.gearFactor / 100.0)); //$NON-NLS-1$
		this.varioTriggerLevelSlider.setSelection(this.configuration.varioThreshold);
		this.varioTriggerLevelText.setText(String.format(Locale.ENGLISH, "+%.1f", this.configuration.varioThreshold / 10.0)); //$NON-NLS-1$
		this.varioTriggerSinkLevelSlider.setSelection(this.configuration.varioThresholdSink);
		this.varioTriggerSinkLevelText.setText(String.format(Locale.ENGLISH, "-%.1f", this.configuration.varioThresholdSink / 10.0)); //$NON-NLS-1$
		this.varioToneCombo.select(this.configuration.varioTon);
		this.varioFactorCombo.select(this.configuration.varioFactor);
		this.varioFilterCombo.select(this.configuration.varioFilter);
		this.limiterModusCombo.select(this.configuration.limiterModus);
		this.energyLimitSlider.setSelection(this.configuration.energyLimit);
		this.energyLimitText.setText(GDE.STRING_BLANK + this.configuration.energyLimit);
		this.minMaxRxCombo.select(this.configuration.minMaxRx);
		this.capacityResetCombo.select(this.configuration.capacityReset);
		this.currentOffsetCombo.select(this.configuration.currentOffset);
		this.telemetrieTypeCombo.select(this.configuration.telemetrieType);
		this.frskyIdCombo.select(this.configuration.frskyAddr - 1);
		this.fixSerialNumberButton.setSelection(this.configuration.serialNumberFix == 1 ? true : false);
		this.robbeTBoxButton.setSelection(this.configuration.robbe_T_Box == 1 ? true : false);

		this.autoStartCurrentButton.setSelection((this.configuration.startModus & 0x0001) > 0);
		this.autoStartRxButton.setSelection((this.configuration.startModus & 0x0002) > 0);
		this.autoStartTimeButton.setSelection((this.configuration.startModus & 0x0004) > 0);
		this.autoStartCurrentCombo.select(this.configuration.startCurrent - 1);
		this.autoStartRxCombo.select(this.configuration.startRx - 11);
		this.autoStartTimeCombo.select(this.configuration.startTime / 5 - 1);
		this.autoStopCombo.select(this.configuration.stopModus);

		changeVisibility();
	}

	void initGUI() {
		SWTResourceManager.registerResourceUser(this);
		try {
			this.setLayout(new FormLayout());
			//this.setSize(320, 467); // required for form editor
			{
				this.commonAdjustmentsGroup = new Group(this, SWT.NONE);
				RowLayout commonAdjustmentsGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.commonAdjustmentsGroup.setLayout(commonAdjustmentsGroupLayout);
				FormData commonAdjustmentsGroupLData = new FormData();
				commonAdjustmentsGroupLData.width = 290;
				commonAdjustmentsGroupLData.height = 515;
				commonAdjustmentsGroupLData.left = new FormAttachment(0, 1000, 12);
				commonAdjustmentsGroupLData.top = new FormAttachment(0, 1000, 5);
				this.commonAdjustmentsGroup.setLayoutData(commonAdjustmentsGroupLData);
				this.commonAdjustmentsGroup.setText(Messages.getString(MessageIds.GDE_MSGT2533));
				this.commonAdjustmentsGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				{
					this.serialNumberLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData serialNumberLabelLData = new RowData();
					serialNumberLabelLData.width = GDE.IS_LINUX ? 90 : 100;
					serialNumberLabelLData.height = 20;
					this.serialNumberLabel.setLayoutData(serialNumberLabelLData);
					this.serialNumberLabel.setText(Messages.getString(MessageIds.GDE_MSGT2534));
					this.serialNumberLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.serialNumberText = new Text(this.commonAdjustmentsGroup, SWT.RIGHT | SWT.BORDER);
					RowData serialNumberTextLData = new RowData();
					serialNumberTextLData.width = 50;
					serialNumberTextLData.height = 16;
					this.serialNumberText.setLayoutData(serialNumberTextLData);
					this.serialNumberText.setEditable(false);
					this.serialNumberText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.firmwareLabel = new CLabel(this.commonAdjustmentsGroup, SWT.RIGHT);
					RowData firmwareLabelLData = new RowData();
					firmwareLabelLData.width = GDE.IS_LINUX ? 65 : 75;
					firmwareLabelLData.height = 20;
					this.firmwareLabel.setLayoutData(firmwareLabelLData);
					this.firmwareLabel.setText(Messages.getString(MessageIds.GDE_MSGT2535));
					this.firmwareLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.firmwareText = new Text(this.commonAdjustmentsGroup, SWT.BORDER | SWT.RIGHT);
					RowData firmwareTextLData = new RowData();
					firmwareTextLData.width = GDE.IS_LINUX ? 35 : 40;
					firmwareTextLData.height = 16;
					this.firmwareText.setLayoutData(firmwareTextLData);
					this.firmwareText.setEditable(false);
					this.firmwareText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.fillerComposite = new Composite(this.commonAdjustmentsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 280;
					fillerCompositeRA1LData.height = 5;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.telemetrieTypeLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData telemetrieTypeLabelLData = new RowData();
					telemetrieTypeLabelLData.width = 130;
					telemetrieTypeLabelLData.height = 20;
					this.telemetrieTypeLabel.setLayoutData(telemetrieTypeLabelLData);
					this.telemetrieTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT2580));
					this.telemetrieTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.telemetrieTypeCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					RowData telemetrieTypeComboLData = new RowData();
					telemetrieTypeComboLData.width = 130;
					telemetrieTypeComboLData.height = 17;
					this.telemetrieTypeCombo.setLayoutData(telemetrieTypeComboLData);
					this.telemetrieTypeCombo.setItems(this.telemetrieTypes);
					this.telemetrieTypeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.telemetrieTypeCombo.setEditable(false);
					this.telemetrieTypeCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.telemetrieTypeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "telemetrieTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.telemetrieType = (short) (UniLog2SetupConfiguration1.this.telemetrieTypeCombo.getSelectionIndex());
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							changeVisibility();
						}
					});
				}
				{
					this.addonComposite = new Composite(this.commonAdjustmentsGroup, SWT.NONE);
					this.addonComposite.setLayout(new FormLayout());
					RowData addonCompositeLData = new RowData();
					addonCompositeLData.width = 285;
					addonCompositeLData.height = 44;
					this.addonComposite.setLayoutData(addonCompositeLData);
					{
						this.frskyIdLabel = new CLabel(this.addonComposite, SWT.NONE);
						FormData frskyIdLabelLData = new FormData();
						frskyIdLabelLData.width = 130;
						frskyIdLabelLData.height = 20;
						frskyIdLabelLData.left = new FormAttachment(0, 1000, 2);
						frskyIdLabelLData.top = new FormAttachment(0, 1000, 0);
						this.frskyIdLabel.setLayoutData(frskyIdLabelLData);
						this.frskyIdLabel.setText(Messages.getString(MessageIds.GDE_MSGT2579));
						this.frskyIdLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					}
					{
						this.fixSerialNumberButton = new Button(this.addonComposite, SWT.CHECK | SWT.LEFT);
						FormData sensorTypeLabelLData = new FormData();
						sensorTypeLabelLData.width = 130;
						sensorTypeLabelLData.height = 20;
						sensorTypeLabelLData.left = new FormAttachment(0, 1000, 2);
						sensorTypeLabelLData.top = new FormAttachment(0, 1000, 0);
						this.fixSerialNumberButton.setLayoutData(sensorTypeLabelLData);
						this.fixSerialNumberButton.setText(Messages.getString(MessageIds.GDE_MSGT2592));
						this.fixSerialNumberButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.fixSerialNumberButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								UniLog2SetupConfiguration1.log.log(Level.FINEST, "fixSerialNumberButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLog2SetupConfiguration1.this.configuration.serialNumberFix = (byte) (UniLog2SetupConfiguration1.this.fixSerialNumberButton.getSelection() ? 1 : 0);
								UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
						});
					}
					{
						this.robbeTBoxButton = new Button(this.addonComposite, SWT.CHECK | SWT.LEFT);
						FormData sensorTypeLabelLData = new FormData();
						sensorTypeLabelLData.width = 115;
						sensorTypeLabelLData.height = 20;
						sensorTypeLabelLData.left = new FormAttachment(0, 1000, 2);
						sensorTypeLabelLData.top = new FormAttachment(0, 1000, 22);
						this.robbeTBoxButton.setLayoutData(sensorTypeLabelLData);
						this.robbeTBoxButton.setText(Messages.getString(MessageIds.GDE_MSGT2593));
						this.robbeTBoxButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.robbeTBoxButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								UniLog2SetupConfiguration1.log.log(Level.FINEST, "robbeTBoxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLog2SetupConfiguration1.this.configuration.robbe_T_Box = (byte) (UniLog2SetupConfiguration1.this.robbeTBoxButton.getSelection() ? 1 : 0);
								UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
						});
					}
					{
						this.frskyIdCombo = new CCombo(this.addonComposite, SWT.BORDER);
						FormData sensorTypeComboLData = new FormData();
						sensorTypeComboLData.width = 80;
						sensorTypeComboLData.height = 20;
						sensorTypeComboLData.left = new FormAttachment(0, 1000, 137);
						sensorTypeComboLData.top = new FormAttachment(0, 1000, 0);
						this.frskyIdCombo.setLayoutData(sensorTypeComboLData);
						this.frskyIdCombo.setItems(this.frskyIDs);
						this.frskyIdCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.frskyIdCombo.setEditable(false);
						this.frskyIdCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
						this.frskyIdCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								UniLog2SetupConfiguration1.log.log(Level.FINEST, "sensorTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLog2SetupConfiguration1.this.configuration.frskyAddr = (short) (UniLog2SetupConfiguration1.this.frskyIdCombo.getSelectionIndex() + 1);
								UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
						});
					}
					this.addonComposite.layout();
				}
				{
					this.dataRateLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData dataRateLabelLData = new RowData();
					dataRateLabelLData.width = 130;
					dataRateLabelLData.height = 20;
					this.dataRateLabel.setLayoutData(dataRateLabelLData);
					this.dataRateLabel.setText(Messages.getString(MessageIds.GDE_MSGT2536));
					this.dataRateLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.dataRateCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					this.dataRateCombo.setItems(this.dataRateValues);
					this.dataRateCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData dataRateComboLData = new RowData();
					dataRateComboLData.width = 84;
					dataRateComboLData.height = 17;
					this.dataRateCombo.setLayoutData(dataRateComboLData);
					this.dataRateCombo.setEditable(false);
					this.dataRateCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.dataRateCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "dataRateCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.dataRate = (short) (UniLog2SetupConfiguration1.this.dataRateCombo.getSelectionIndex() + 1); //remove 50 Hz
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.currentSensorTypeLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData currentSensorTypeLabelLData = new RowData();
					currentSensorTypeLabelLData.width = 130;
					currentSensorTypeLabelLData.height = 20;
					this.currentSensorTypeLabel.setLayoutData(currentSensorTypeLabelLData);
					this.currentSensorTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT2537));
					this.currentSensorTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.currentSensorCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					RowData currentSensorComboLData = new RowData();
					currentSensorComboLData.width = 84;
					currentSensorComboLData.height = 17;
					this.currentSensorCombo.setLayoutData(currentSensorComboLData);
					this.currentSensorCombo.setItems(this.currentSensorTypes);
					this.currentSensorCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentSensorCombo.setEditable(false);
					this.currentSensorCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.currentSensorCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "currentSensorCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.currentSensorType = (short) UniLog2SetupConfiguration1.this.currentSensorCombo.getSelectionIndex();
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.a1ModusLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData a1ModusLabelLData = new RowData();
					a1ModusLabelLData.width = 130;
					a1ModusLabelLData.height = 20;
					this.a1ModusLabel.setLayoutData(a1ModusLabelLData);
					this.a1ModusLabel.setText(Messages.getString(MessageIds.GDE_MSGT2538));
					this.a1ModusLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.a1ModusCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					this.a1ModusCombo.setItems(this.analogModi);
					this.a1ModusCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData a1ModusComboLData = new RowData();
					a1ModusComboLData.width = 105;
					a1ModusComboLData.height = 17;
					this.a1ModusCombo.setLayoutData(a1ModusComboLData);
					this.a1ModusCombo.setEditable(false);
					this.a1ModusCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.a1ModusCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "a1ModusCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.modusA1 = (short) UniLog2SetupConfiguration1.this.a1ModusCombo.getSelectionIndex();
							UniLog2SetupConfiguration1.this.dialog.updateAnalogAlarmUnits();
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.a2ModusLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData a2ModusLabelLData = new RowData();
					a2ModusLabelLData.width = 130;
					a2ModusLabelLData.height = 20;
					this.a2ModusLabel.setLayoutData(a2ModusLabelLData);
					this.a2ModusLabel.setText(Messages.getString(MessageIds.GDE_MSGT2539));
					this.a2ModusLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.a2ModusCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					this.a2ModusCombo.setItems(this.analogModi);
					this.a2ModusCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData a2ModusComboLData = new RowData();
					a2ModusComboLData.width = 105;
					a2ModusComboLData.height = 17;
					this.a2ModusCombo.setLayoutData(a2ModusComboLData);
					this.a2ModusCombo.setEditable(false);
					this.a2ModusCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.a2ModusCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "a2ModusCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.modusA2 = (short) UniLog2SetupConfiguration1.this.a2ModusCombo.getSelectionIndex();
							UniLog2SetupConfiguration1.this.dialog.updateAnalogAlarmUnits();
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.a3ModusLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData a3ModusLabelLData = new RowData();
					a3ModusLabelLData.width = 130;
					a3ModusLabelLData.height = 20;
					this.a3ModusLabel.setLayoutData(a3ModusLabelLData);
					this.a3ModusLabel.setText(Messages.getString(MessageIds.GDE_MSGT2540));
					this.a3ModusLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.a3ModusCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					this.a3ModusCombo.setItems(this.analogModi);
					this.a3ModusCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData a3ModusComboLData = new RowData();
					a3ModusComboLData.width = 105;
					a3ModusComboLData.height = 17;
					this.a3ModusCombo.setLayoutData(a3ModusComboLData);
					this.a3ModusCombo.setEditable(false);
					this.a3ModusCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.a3ModusCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "a3ModusCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.modusA3 = (short) UniLog2SetupConfiguration1.this.a3ModusCombo.getSelectionIndex();
							UniLog2SetupConfiguration1.this.dialog.updateAnalogAlarmUnits();
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.propellerBladesLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData propellerBladesLabelLData = new RowData();
					propellerBladesLabelLData.width = 130;
					propellerBladesLabelLData.height = 20;
					this.propellerBladesLabel.setLayoutData(propellerBladesLabelLData);
					this.propellerBladesLabel.setText(Messages.getString(MessageIds.GDE_MSGT2541));
					this.propellerBladesLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.propBladesCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					RowData propBladesComboLData = new RowData();
					propBladesComboLData.width = 65;
					propBladesComboLData.height = 17;
					this.propBladesCombo.setLayoutData(propBladesComboLData);
					this.propBladesCombo.setItems(this.numberProbMotorPoles);
					this.propBladesCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.propBladesCombo.setEditable(false);
					this.propBladesCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.propBladesCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "propBladesCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.numberProb_MotorPole = (short) (UniLog2SetupConfiguration1.this.propBladesCombo.getSelectionIndex() + 1);
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.motorPolsLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData motorPolsLabelLData = new RowData();
					motorPolsLabelLData.width = 76;
					motorPolsLabelLData.height = 20;
					this.motorPolsLabel.setLayoutData(motorPolsLabelLData);
					this.motorPolsLabel.setText(Messages.getString(MessageIds.GDE_MSGT2542));
					this.motorPolsLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.gearFactorLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData gearFactorLabelLData = new RowData();
					gearFactorLabelLData.width = 130;
					gearFactorLabelLData.height = 20;
					this.gearFactorLabel.setLayoutData(gearFactorLabelLData);
					this.gearFactorLabel.setText(Messages.getString(MessageIds.GDE_MSGT2543));
					this.gearFactorLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.gearFactorText = new Text(this.commonAdjustmentsGroup, SWT.CENTER | SWT.BORDER);
					RowData gearFactorTextLData = new RowData();
					gearFactorTextLData.width = GDE.IS_LINUX ? 30 : 35;
					gearFactorTextLData.height = 16;
					this.gearFactorText.setLayoutData(gearFactorTextLData);
					this.gearFactorText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.gearFactorText.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "gearFactorText.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.gearFactorText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "gearFactorText.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration1.this.configuration.gearFactor = (short) (Double.parseDouble(UniLog2SetupConfiguration1.this.gearFactorText.getText().trim()
										.replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 100);
								if (UniLog2SetupConfiguration1.this.configuration.gearFactor < 100) {
									UniLog2SetupConfiguration1.this.configuration.gearFactor = 100;
									UniLog2SetupConfiguration1.this.gearFactorText.setText(String.format(Locale.ENGLISH, " %.2f", (UniLog2SetupConfiguration1.this.configuration.gearFactor / 100.0))); //$NON-NLS-1$
									UniLog2SetupConfiguration1.this.gearFactorSlider.setSelection(UniLog2SetupConfiguration1.this.gearFactorSlider.getMaximum());
								}
								else if (UniLog2SetupConfiguration1.this.configuration.gearFactor > 2000) {
									UniLog2SetupConfiguration1.this.configuration.gearFactor = 2000;
									UniLog2SetupConfiguration1.this.gearFactorText.setText(String.format(Locale.ENGLISH, " %.2f", (UniLog2SetupConfiguration1.this.configuration.gearFactor / 100.0))); //$NON-NLS-1$
									UniLog2SetupConfiguration1.this.gearFactorSlider.setSelection(UniLog2SetupConfiguration1.this.gearFactorSlider.getMinimum());
								}
								else {
									UniLog2SetupConfiguration1.this.gearFactorSlider.setSelection(UniLog2SetupConfiguration1.this.configuration.gearFactor - 100);
								}
								UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore -
							}
						}
					});
					this.gearFactorText.addFocusListener(new FocusAdapter() {
						@Override
						public void focusLost(FocusEvent evt) {
							UniLog2SetupConfiguration1.this.gearFactorText.setText(String.format(Locale.ENGLISH, " %.2f", (UniLog2SetupConfiguration1.this.configuration.gearFactor / 100.0))); //$NON-NLS-1$
						}
					});
				}
				{
					RowData gearFactorSliderLData = new RowData();
					gearFactorSliderLData.width = 101;
					gearFactorSliderLData.height = 18;
					this.gearFactorSlider = new Slider(this.commonAdjustmentsGroup, SWT.NONE);
					this.gearFactorSlider.setLayoutData(gearFactorSliderLData);
					this.gearFactorSlider.setMinimum(0);
					this.gearFactorSlider.setMaximum(1910);
					this.gearFactorSlider.setIncrement(1);
					this.gearFactorSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "gearFactorSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.gearFactor = (short) (UniLog2SetupConfiguration1.this.gearFactorSlider.getSelection() + 100);
							UniLog2SetupConfiguration1.this.gearFactorText.setText(String.format(Locale.ENGLISH, " %.2f", (UniLog2SetupConfiguration1.this.configuration.gearFactor / 100.0))); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioTriggerLevelLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData varioTriggerLevelLabelLData = new RowData();
					varioTriggerLevelLabelLData.width = 130;
					varioTriggerLevelLabelLData.height = 20;
					this.varioTriggerLevelLabel.setLayoutData(varioTriggerLevelLabelLData);
					this.varioTriggerLevelLabel.setText(Messages.getString(MessageIds.GDE_MSGT2544));
					this.varioTriggerLevelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.varioTriggerLevelText = new Text(this.commonAdjustmentsGroup, SWT.CENTER | SWT.BORDER);
					RowData varioTriggerLevelTextLData = new RowData();
					varioTriggerLevelTextLData.width = GDE.IS_LINUX ? 30 : 35;
					varioTriggerLevelTextLData.height = 16;
					this.varioTriggerLevelText.setLayoutData(varioTriggerLevelTextLData);
					this.varioTriggerLevelText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioTriggerLevelText.setEditable(false);
					this.varioTriggerLevelText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					RowData varioTriggerLevelSliderLData = new RowData();
					varioTriggerLevelSliderLData.width = 101;
					varioTriggerLevelSliderLData.height = 18;
					this.varioTriggerLevelSlider = new Slider(this.commonAdjustmentsGroup, SWT.NONE);
					this.varioTriggerLevelSlider.setLayoutData(varioTriggerLevelSliderLData);
					this.varioTriggerLevelSlider.setMinimum(0);
					this.varioTriggerLevelSlider.setMaximum(60);
					this.varioTriggerLevelSlider.setIncrement(1);
					this.varioTriggerLevelSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "varioTriggerLevelSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.varioThreshold = (short) UniLog2SetupConfiguration1.this.varioTriggerLevelSlider.getSelection();
							UniLog2SetupConfiguration1.this.varioTriggerLevelText.setText(String.format(Locale.ENGLISH, "+%.1f", UniLog2SetupConfiguration1.this.configuration.varioThreshold / 10.0));
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioTriggerSinkLevelLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData varioTriggerSinkLevelLabelLData = new RowData();
					varioTriggerSinkLevelLabelLData.width = 130;
					varioTriggerSinkLevelLabelLData.height = 20;
					this.varioTriggerSinkLevelLabel.setLayoutData(varioTriggerSinkLevelLabelLData);
					this.varioTriggerSinkLevelLabel.setText(Messages.getString(MessageIds.GDE_MSGT2513));
					this.varioTriggerSinkLevelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.varioTriggerSinkLevelText = new Text(this.commonAdjustmentsGroup, SWT.CENTER | SWT.BORDER);
					RowData varioTriggerSinkLevelTextLData = new RowData();
					varioTriggerSinkLevelTextLData.width = GDE.IS_LINUX ? 30 : 35;
					varioTriggerSinkLevelTextLData.height = 16;
					this.varioTriggerSinkLevelText.setLayoutData(varioTriggerSinkLevelTextLData);
					this.varioTriggerSinkLevelText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioTriggerSinkLevelText.setEditable(false);
					this.varioTriggerSinkLevelText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					RowData varioTriggerSinkLevelSliderLData = new RowData();
					varioTriggerSinkLevelSliderLData.width = 101;
					varioTriggerSinkLevelSliderLData.height = 18;
					this.varioTriggerSinkLevelSlider = new Slider(this.commonAdjustmentsGroup, SWT.NONE);
					this.varioTriggerSinkLevelSlider.setLayoutData(varioTriggerSinkLevelSliderLData);
					this.varioTriggerSinkLevelSlider.setMinimum(0);
					this.varioTriggerSinkLevelSlider.setMaximum(60);
					this.varioTriggerSinkLevelSlider.setIncrement(1);
					this.varioTriggerSinkLevelSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "varioTriggerSinkLevelSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.varioThresholdSink = (short) UniLog2SetupConfiguration1.this.varioTriggerSinkLevelSlider.getSelection();
							UniLog2SetupConfiguration1.this.varioTriggerSinkLevelText.setText(String.format(Locale.ENGLISH, "-%.1f", UniLog2SetupConfiguration1.this.configuration.varioThresholdSink / 10.0)); //$NON-NLS-1$);
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioToneLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData varioToneLabelLData = new RowData();
					varioToneLabelLData.width = 130;
					varioToneLabelLData.height = 20;
					this.varioToneLabel.setLayoutData(varioToneLabelLData);
					this.varioToneLabel.setText(Messages.getString(MessageIds.GDE_MSGT2545));
					this.varioToneLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.varioToneCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER | SWT.CENTER);
					RowData varioToneComboLData = new RowData();
					varioToneComboLData.width = 84;
					varioToneComboLData.height = 17;
					this.varioToneCombo.setLayoutData(varioToneComboLData);
					this.varioToneCombo.setItems(Messages.getString(MessageIds.GDE_MSGT2522).split(GDE.STRING_COMMA));
					this.varioToneCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioToneCombo.setEditable(false);
					this.varioToneCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.varioToneCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "varioToneCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.varioTon = (short) (UniLog2SetupConfiguration1.this.varioToneCombo.getSelectionIndex());
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioFactorLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData varioFactorLabelLData = new RowData();
					varioFactorLabelLData.width = 130;
					varioFactorLabelLData.height = 20;
					this.varioFactorLabel.setLayoutData(varioFactorLabelLData);
					this.varioFactorLabel.setText(Messages.getString(MessageIds.GDE_MSGT2586));
					this.varioFactorLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.varioFactorCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER | SWT.CENTER);
					RowData varioFactorComboLData = new RowData();
					varioFactorComboLData.width = 84;
					varioFactorComboLData.height = 17;
					this.varioFactorCombo.setLayoutData(varioFactorComboLData);
					this.varioFactorCombo.setItems(this.varioFactors);
					this.varioFactorCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioFactorCombo.setEditable(false);
					this.varioFactorCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.varioFactorCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "varioFactorCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.varioFactor = (short) (UniLog2SetupConfiguration1.this.varioFactorCombo.getSelectionIndex());
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioFilterLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData varioFilterLabelLData = new RowData();
					varioFilterLabelLData.width = 130;
					varioFilterLabelLData.height = 20;
					this.varioFilterLabel.setLayoutData(varioFilterLabelLData);
					this.varioFilterLabel.setText(Messages.getString(MessageIds.GDE_MSGT2587));
					this.varioFilterLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.varioFilterCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER | SWT.CENTER);
					RowData varioFilterComboLData = new RowData();
					varioFilterComboLData.width = 84;
					varioFilterComboLData.height = 17;
					this.varioFilterCombo.setLayoutData(varioFilterComboLData);
					this.varioFilterCombo.setItems(this.varioFilters);
					this.varioFilterCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioFilterCombo.setEditable(false);
					this.varioFilterCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.varioFilterCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "varioFilterCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.varioFilter = (short) (UniLog2SetupConfiguration1.this.varioFilterCombo.getSelectionIndex());
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.limiterModusLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData limiterModusLabelLData = new RowData();
					limiterModusLabelLData.width = 130;
					limiterModusLabelLData.height = 20;
					this.limiterModusLabel.setLayoutData(limiterModusLabelLData);
					this.limiterModusLabel.setText(Messages.getString(MessageIds.GDE_MSGT2546));
					this.limiterModusLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.limiterModusCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER | SWT.CENTER);
					RowData limiterModusComboLData = new RowData();
					limiterModusComboLData.width = 84;
					limiterModusComboLData.height = 17;
					this.limiterModusCombo.setLayoutData(limiterModusComboLData);
					this.limiterModusCombo.setItems(Messages.getString(MessageIds.GDE_MSGT2521).split(GDE.STRING_COMMA));
					this.limiterModusCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.limiterModusCombo.setEditable(false);
					this.limiterModusCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.limiterModusCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "limiterModusCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.limiterModus = (short) (UniLog2SetupConfiguration1.this.limiterModusCombo.getSelectionIndex());
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.energyLimitLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData energyLimitLabelLData = new RowData();
					energyLimitLabelLData.width = 130;
					energyLimitLabelLData.height = 20;
					this.energyLimitLabel.setLayoutData(energyLimitLabelLData);
					this.energyLimitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2547));
					this.energyLimitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.energyLimitText = new Text(this.commonAdjustmentsGroup, SWT.CENTER | SWT.BORDER);
					RowData energyLimitTextLData = new RowData();
					energyLimitTextLData.width = GDE.IS_MAC ? 39 : GDE.IS_LINUX ? 30 : 35;
					energyLimitTextLData.height = 16;
					this.energyLimitText.setLayoutData(energyLimitTextLData);
					this.energyLimitText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.energyLimitText.setEditable(false);
					this.energyLimitText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					RowData energyLimitSliderLData = new RowData();
					energyLimitSliderLData.width = 101;
					energyLimitSliderLData.height = 18;
					this.energyLimitSlider = new Slider(this.commonAdjustmentsGroup, SWT.BORDER);
					this.energyLimitSlider.setLayoutData(energyLimitSliderLData);
					this.energyLimitSlider.setMinimum(0);
					this.energyLimitSlider.setMaximum(2010);
					this.energyLimitSlider.setIncrement(1);
					this.energyLimitSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "energyLimitSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.energyLimit = (short) UniLog2SetupConfiguration1.this.energyLimitSlider.getSelection();
							UniLog2SetupConfiguration1.this.energyLimitText.setText(GDE.STRING_EMPTY + UniLog2SetupConfiguration1.this.configuration.energyLimit);
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.minMaxRxLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData minMaxRxLabelLData = new RowData();
					minMaxRxLabelLData.width = 130;
					minMaxRxLabelLData.height = 20;
					this.minMaxRxLabel.setLayoutData(minMaxRxLabelLData);
					this.minMaxRxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2548));
					this.minMaxRxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.minMaxRxCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					RowData minMaxRxComboLData = new RowData();
					minMaxRxComboLData.width = 84;
					minMaxRxComboLData.height = 17;
					this.minMaxRxCombo.setLayoutData(minMaxRxComboLData);
					this.minMaxRxCombo.setItems(Messages.getString(MessageIds.GDE_MSGT2519).split(GDE.STRING_COMMA));
					this.minMaxRxCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.minMaxRxCombo.setEditable(false);
					this.minMaxRxCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.minMaxRxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "minMaxRxCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.minMaxRx = (short) (UniLog2SetupConfiguration1.this.minMaxRxCombo.getSelectionIndex());
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.capacityResetLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData capacityResetLabelLData = new RowData();
					capacityResetLabelLData.width = 130;
					capacityResetLabelLData.height = 20;
					this.capacityResetLabel.setLayoutData(capacityResetLabelLData);
					this.capacityResetLabel.setText(Messages.getString(MessageIds.GDE_MSGT2583));
					this.capacityResetLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.capacityResetCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					RowData capacityResetComboLData = new RowData();
					capacityResetComboLData.width = 130;
					capacityResetComboLData.height = 17;
					this.capacityResetCombo.setLayoutData(capacityResetComboLData);
					this.capacityResetCombo.setItems(this.capacityResets);
					this.capacityResetCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityResetCombo.setEditable(false);
					this.capacityResetCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.capacityResetCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "capacityResetCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.capacityReset = (short) (UniLog2SetupConfiguration1.this.capacityResetCombo.getSelectionIndex());
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.currentOffsetLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData currentOffsetLabelLData = new RowData();
					currentOffsetLabelLData.width = 130;
					currentOffsetLabelLData.height = 20;
					this.currentOffsetLabel.setLayoutData(currentOffsetLabelLData);
					this.currentOffsetLabel.setText(Messages.getString(MessageIds.GDE_MSGT2584));
					this.currentOffsetLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.currentOffsetCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					RowData currentOffsetComboLData = new RowData();
					currentOffsetComboLData.width = 84;
					currentOffsetComboLData.height = 17;
					this.currentOffsetCombo.setLayoutData(currentOffsetComboLData);
					this.currentOffsetCombo.setItems(this.currentOffsets);
					this.currentOffsetCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentOffsetCombo.setEditable(false);
					this.currentOffsetCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.currentOffsetCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "currentOffsetCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.currentOffset = (short) (UniLog2SetupConfiguration1.this.currentOffsetCombo.getSelectionIndex());
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
			}
			{
				this.logStartStopGroup = new Group(this, SWT.NONE);
				RowLayout logStartStopGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.logStartStopGroup.setLayout(logStartStopGroupLayout);
				FormData logStartStopGroupLData = new FormData();
				logStartStopGroupLData.width = 290;
				logStartStopGroupLData.height = 105;
				logStartStopGroupLData.left = new FormAttachment(0, 1000, 12);
				logStartStopGroupLData.top = new FormAttachment(0, 1000, 540);
				this.logStartStopGroup.setLayoutData(logStartStopGroupLData);
				this.logStartStopGroup.setText(Messages.getString(MessageIds.GDE_MSGT2526));
				this.logStartStopGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				{
					this.fillerComposite = new Composite(this.logStartStopGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 280;
					fillerCompositeRA1LData.height = 5;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.autoStartCurrentButton = new Button(this.logStartStopGroup, SWT.CHECK);
					RowData startByCurrentButtonLData = new RowData();
					startByCurrentButtonLData.width = 130;
					startByCurrentButtonLData.height = 19;
					this.autoStartCurrentButton.setLayoutData(startByCurrentButtonLData);
					this.autoStartCurrentButton.setText(Messages.getString(MessageIds.GDE_MSGT2527));
					this.autoStartCurrentButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.autoStartCurrentButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "autoStartCurrentButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration1.this.autoStartCurrentButton.getSelection()) {
								UniLog2SetupConfiguration1.this.configuration.startModus = (short) (UniLog2SetupConfiguration1.this.configuration.startModus | UniLog2SetupReaderWriter.AUTO_START_CURRENT);
							}
							else {
								UniLog2SetupConfiguration1.this.configuration.startModus = (short) (UniLog2SetupConfiguration1.this.configuration.startModus ^ UniLog2SetupReaderWriter.AUTO_START_CURRENT);
							}
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.autoStartCurrentCombo = new CCombo(this.logStartStopGroup, SWT.BORDER);
					RowData currentTriggerComboLData = new RowData();
					currentTriggerComboLData.width = 84;
					currentTriggerComboLData.height = 17;
					this.autoStartCurrentCombo.setLayoutData(currentTriggerComboLData);
					this.autoStartCurrentCombo.setItems(this.currentStartValues);
					this.autoStartCurrentCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.autoStartCurrentCombo.setEditable(false);
					this.autoStartCurrentCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.autoStartCurrentCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "autoStartCurrentCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.startCurrent = (short) (UniLog2SetupConfiguration1.this.autoStartCurrentCombo.getSelectionIndex() + 1);
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.autoStartCurrentUnitLabel = new CLabel(this.logStartStopGroup, SWT.NONE);
					RowData currentTriggerUnitLabelLData = new RowData();
					currentTriggerUnitLabelLData.width = 49;
					currentTriggerUnitLabelLData.height = 20;
					this.autoStartCurrentUnitLabel.setLayoutData(currentTriggerUnitLabelLData);
					this.autoStartCurrentUnitLabel.setText("[A]"); //$NON-NLS-1$
					this.autoStartCurrentUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.autoStartRxButton = new Button(this.logStartStopGroup, SWT.CHECK | SWT.LEFT);
					RowData rxTriggerButtonLData = new RowData();
					rxTriggerButtonLData.width = 130;
					rxTriggerButtonLData.height = 16;
					this.autoStartRxButton.setLayoutData(rxTriggerButtonLData);
					this.autoStartRxButton.setText(Messages.getString(MessageIds.GDE_MSGT2528));
					this.autoStartRxButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.autoStartRxButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "autoStartRxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration1.this.autoStartRxButton.getSelection()) {
								UniLog2SetupConfiguration1.this.configuration.startModus = (short) (UniLog2SetupConfiguration1.this.configuration.startModus | UniLog2SetupReaderWriter.AUTO_START_RX);
							}
							else {
								UniLog2SetupConfiguration1.this.configuration.startModus = (short) (UniLog2SetupConfiguration1.this.configuration.startModus ^ UniLog2SetupReaderWriter.AUTO_START_RX);
							}
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.autoStartRxCombo = new CCombo(this.logStartStopGroup, SWT.BORDER);
					RowData rxTriggerComboLData = new RowData();
					rxTriggerComboLData.width = 84;
					rxTriggerComboLData.height = 17;
					this.autoStartRxCombo.setLayoutData(rxTriggerComboLData);
					this.autoStartRxCombo.setItems(this.rxStartValues);
					this.autoStartRxCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.autoStartRxCombo.setEditable(false);
					this.autoStartRxCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.autoStartRxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "autoStartRxCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.startRx = (short) (UniLog2SetupConfiguration1.this.autoStartRxCombo.getSelectionIndex() + 11);
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.autoStartRxUnitLabel = new CLabel(this.logStartStopGroup, SWT.NONE);
					RowData rxTriggerUnitLabelLData = new RowData();
					rxTriggerUnitLabelLData.width = 49;
					rxTriggerUnitLabelLData.height = 20;
					this.autoStartRxUnitLabel.setLayoutData(rxTriggerUnitLabelLData);
					this.autoStartRxUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2529));
					this.autoStartRxUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.autoStartTimeButton = new Button(this.logStartStopGroup, SWT.CHECK | SWT.LEFT);
					RowData timeTriggerButtonLData = new RowData();
					timeTriggerButtonLData.width = 130;
					timeTriggerButtonLData.height = 16;
					this.autoStartTimeButton.setLayoutData(timeTriggerButtonLData);
					this.autoStartTimeButton.setText(Messages.getString(MessageIds.GDE_MSGT2530));
					this.autoStartTimeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.autoStartTimeButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "autoStartTimeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration1.this.autoStartTimeButton.getSelection()) {
								UniLog2SetupConfiguration1.this.configuration.startModus = (short) (UniLog2SetupConfiguration1.this.configuration.startModus | UniLog2SetupReaderWriter.AUTO_START_TIME);
							}
							else {
								UniLog2SetupConfiguration1.this.configuration.startModus = (short) (UniLog2SetupConfiguration1.this.configuration.startModus ^ UniLog2SetupReaderWriter.AUTO_START_TIME);
							}
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.autoStartTimeCombo = new CCombo(this.logStartStopGroup, SWT.BORDER);
					RowData timeTriggerComboLData = new RowData();
					timeTriggerComboLData.width = 84;
					timeTriggerComboLData.height = 17;
					this.autoStartTimeCombo.setLayoutData(timeTriggerComboLData);
					this.autoStartTimeCombo.setItems(this.timeStartValues);
					this.autoStartTimeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.autoStartTimeCombo.setEditable(false);
					this.autoStartTimeCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.autoStartTimeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "autoStartTimeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.startTime = (short) ((UniLog2SetupConfiguration1.this.autoStartTimeCombo.getSelectionIndex() + 1) * 5);
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.autoStartTimeUnitLabel = new CLabel(this.logStartStopGroup, SWT.NONE);
					RowData timeTriggerUnitLabelLData = new RowData();
					timeTriggerUnitLabelLData.width = 49;
					timeTriggerUnitLabelLData.height = 20;
					this.autoStartTimeUnitLabel.setLayoutData(timeTriggerUnitLabelLData);
					this.autoStartTimeUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2531));
					this.autoStartTimeUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.autoStopLabel = new CLabel(this.logStartStopGroup, SWT.NONE);
					RowData autoStopLabelLData = new RowData();
					autoStopLabelLData.width = 130;
					autoStopLabelLData.height = 20;
					this.autoStopLabel.setLayoutData(autoStopLabelLData);
					this.autoStopLabel.setText(Messages.getString(MessageIds.GDE_MSGT2532));
					this.autoStopLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.autoStopCombo = new CCombo(this.logStartStopGroup, SWT.BORDER);
					this.autoStopCombo.setItems(Messages.getString(MessageIds.GDE_MSGT2519).split(GDE.STRING_COMMA));
					this.autoStopCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData autoStopComboLData = new RowData();
					autoStopComboLData.width = 84;
					autoStopComboLData.height = 17;
					this.autoStopCombo.setLayoutData(autoStopComboLData);
					this.autoStopCombo.setEditable(false);
					this.autoStopCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.autoStopCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration1.log.log(Level.FINEST, "autoStopCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.stopModus = (short) (UniLog2SetupConfiguration1.this.autoStopCombo.getSelectionIndex());
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
			}
			this.layout();
		}
		catch (Exception e) {
			UniLog2SetupConfiguration1.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public void changeVisibility() {
		this.frskyIdLabel.setVisible(false);
		this.frskyIdCombo.setVisible(false);
		this.fixSerialNumberButton.setVisible(false);
		this.robbeTBoxButton.setVisible(false);
		if (UniLog2SetupConfiguration2.mLinkGroupStatic != null && !UniLog2SetupConfiguration2.mLinkGroupStatic.isDisposed()) {
			UniLog2SetupConfiguration2.mLinkGroupStatic.setVisible(false);
		}
		if (UniLog2SetupConfiguration2.jetiExGroupStatic != null && !UniLog2SetupConfiguration2.jetiExGroupStatic.isDisposed()) {
			UniLog2SetupConfiguration2.jetiExGroupStatic.setVisible(false);
		}
		if (UniLog2SetupConfiguration2.spektrumAdapterGroupStatic != null && !UniLog2SetupConfiguration2.spektrumAdapterGroupStatic.isDisposed()) {
			UniLog2SetupConfiguration2.spektrumAdapterGroupStatic.setVisible(false);
		}

		if (this.telemetrieTypeCombo != null && !this.telemetrieTypeCombo.isDisposed()) {
			switch (this.telemetrieTypeCombo.getSelectionIndex()) {
			case 10: //Spektrum
				if (UniLog2SetupConfiguration2.spektrumAdapterGroupStatic != null && !UniLog2SetupConfiguration2.spektrumAdapterGroupStatic.isDisposed()) {
					UniLog2SetupConfiguration2.spektrumAdapterGroupStatic.setVisible(true);
				}
				break;
			case 8: // FrSky
				this.frskyIdLabel.setVisible(true);
				this.frskyIdCombo.setVisible(true);
				break;
			case 7: //M-Link
				if (UniLog2SetupConfiguration2.mLinkGroupStatic != null) {
					UniLog2SetupConfiguration2.mLinkGroupStatic.setVisible(true);
				}
				break;
			case 6: //Jeti
				this.fixSerialNumberButton.setVisible(true);
				if (UniLog2SetupConfiguration2.jetiExGroupStatic != null) {
					UniLog2SetupConfiguration2.jetiExGroupStatic.setVisible(true);
				}
				break;
			case 1: //Futaba
				this.fixSerialNumberButton.setVisible(true);
				this.robbeTBoxButton.setVisible(true);
				break;
			default:
				break;
			}
		}
	}

}
