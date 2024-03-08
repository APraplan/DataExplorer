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
package gde.device.smmodellbau;

import java.util.Locale;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.device.DataTypes;
import gde.device.smmodellbau.gpslogger.MessageIds;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

/**
 * class to implement SM GPS-Logger basic configuration panel 1
 */
public class GPSLoggerSetupConfiguration1 extends Composite {

	final static Logger			log							= Logger.getLogger(GPSLoggerSetupConfiguration1.class.getName());
	static final int 				COMBO_WIDTH 		= 66;

	final GPSLoggerDialog		dialog;
	final DataExplorer		  application;

	Composite								fillerComposite, addonComposite;

	Group										gpsLoggerGroup;
	CLabel									serialNumberLabel,firmwareLabel,languageLabel,hottSpeedTypeLabel,dataRateLabel,startModusLabel,stopModusLabel,timeZoneLabel,timeAutoLabel,varioLimitClimbLabel,varioLimitSinkLabel,varioTonLabel,timeZoneUnitLabel,varioLimitUnitLabel,smoothAltitudeLabel,modusIgcLabel,modusDistanceLabel,telemetryTypeLabel,varioFactorLabel,varioFilterLabel,minMaxRxLabel;
	Text										serialNumberText, firmwareText;
	CCombo									languageCombo, hottSpeedTypeCombo, dataRateCombo, startModusCombo, stopModusCombo, timeZoneCombo, timeAutoCombo, varioLimitClimbCombo, varioLimitSinkCombo, varioToneCombo, smoothAltitudeCombo, modusIgcCombo, modusDistanceCombo, telemetryTypeCombo, varioFactorCombo, varioFilterCombo, minMaxRxCombo;

	Group										gpsTelemertieAlarmGroup;
	Button									heightMaxAlarmButton, speedMaxAlarmButton, speedMinAlarmButton, distanceMaxButton, distanceMinButton, tripLengthButton, voltageRxButton;
	CCombo									heightMaxAlarmCombo, speedMaxAlarmCombo, speedMinAlarmCombo, distanceMaxCombo, distanceMinCombo, tripLengthCombo, voltageRxCombo;
	CLabel									heightMaxAlarmLabel, speedAlarmLabel, distanceMaxLabel, distanceMinLabel, tripLengthLabel, voltageRxLabel;

	CLabel									frskyIdLabel;
	Button									fixSerialNumberButton, fixStartPositionButton;
	Button									robbeTBoxButton;
	CCombo									frskyIdCombo;

	final SetupReaderWriter	configuration;
	final String[]					languageValues	= {"Deutsch", "English"};
	final String[]					speedTypeValues	= {" auto", " GPS", "AirSpeed"};
	final String[]					dataRateValues	= Messages.getString(MessageIds.GDE_MSGT2020).split(GDE.STRING_COMMA);
	final String[]					startValues			= Messages.getString(MessageIds.GDE_MSGT2021).split(GDE.STRING_COMMA);
	final String[]					stopValues			= Messages.getString(MessageIds.GDE_MSGT2072).split(GDE.STRING_COMMA);
	final String[]					deltaUTC				= { " -12", " -11", " -10", " -9", " -8", " -7", " -6", " -5", " -4", " -3", " -2", " -1", " 0", " +1", " +2", " +3", " +4", " +5", " +6", " +7", " +8",
			" +9", " +10", " +11", " +12"			};
	final String[]					varioThresholds	= { " 0.0", " 0.1", " 0.2", " 0.3", " 0.4", " 0.5", " 0.6", " 0.7", " 0.8", " 0.9", " 1.0", " 1.1", " 1.2", " 1.3", " 1.4", " 1.5", " 1.6", " 1.7", " 1.8",
			" 1.9", " 2.0", " 2.1", " 2.2", " 2.3", " 2.4", " 2.5", " 2.6", " 2.7", " 2.8", " 2.9", " 3.0", " 3.1", " 3.2", " 3.3", " 3.4", " 3.5", " 3.6", " 3.7", " 3.8", " 3.9", " 4.0", " 4.1", " 4.2",
			" 4.3", " 4.4", " 4.5", " 4.6", " 4.7", " 4.8", " 4.9", " 5.0" };
	final String[]					varioTons				= Messages.getString(MessageIds.GDE_MSGT2022).split(GDE.STRING_COMMA);
	final String[]					heightValues		= {
			"   10", "   25", "   50", "   75", "  100", "  150", "  200", "  250", "  300", "  350", "  400", "  450", "  500", "  600", "  700", "  800", "  900", " 1000", " 1250", " 1500", " 2000", " 2500", " 3000", " 3500", " 4000" };
	final String[]					speedValues			= {
			"   10", "   25", "   50", "   75", "  100", "  150", "  200", "  250", "  300", "  350", "  400", "  450", "  500", "  600", "  700", "  800", "  900", " 1000" };
	final String[]					distanceValues	= {
			"   10", "   25", "   50", "   75", "  100", "  150", "  200", "  250", "  300", "  350", "  400", "  450", "  500", "  600", "  700", "  800", "  900", " 1000", " 1250", " 1500", " 2000", " 2500", " 3000", " 3500", " 4000", " 4500", " 5000" };
	final String[]					tripValues			= {
			"  1.0", "  2.5", "  5.0", "  7.5", " 10.0", " 15.0", " 20.0", " 25.0", " 30.0", " 35.0", " 40.0", " 45.0", " 50.0", " 60.0", " 70.0", " 80.0", " 90.0", " 99.0" };
	final String[]					voltageRxValues	= {
			"  3.00", "  3.25", "  3.50", "  3.75", "  4.00", "  4.25", "  4.50", "  4.75", "  4.80", "  4.85", "  4.90", "  4.95", "  5.00", "  5.05", "  5.10", "  5.15", "  5.20", "  5.25", "  5.50", "  6.00", "  6.25", "  6.50", "  6.75", "  7.00", "  7.25", "  7.50", "  7.75", "  8.00" };
	
	final String[]					smoothAltNull		= Messages.getString(MessageIds.GDE_MSGI2002).split(GDE.STRING_COMMA);
	final String[]					igcModes				= Messages.getString(MessageIds.GDE_MSGT2069).split(GDE.STRING_COMMA);
	final String[]					distanceModes		= Messages.getString(MessageIds.GDE_MSGT2071).split(GDE.STRING_COMMA);
	final String[]					telemetrieTypes	= { " - - - ", " Futaba", " JR DMSS", " HoTT", " JetiDuplex", " M-Link", " FrSky", " Spektrum", " P2Bus" };
	final String[]					varioFactors;
	final String[]					varioFilters		= Messages.getString(MessageIds.GDE_MSGT2019).split(GDE.STRING_COMMA);
	final String[]					frskyIDs				= { " 0x00", " 0xA1", " 0x22", " 0x83", " 0xE4", " 0x45", " 0xC6", " 0x67", " 0x48", " 0xE9", " 0x6A", " 0xCB", " 0xAC", " 0x0D", " 0x8E",
			" 0x2F", " 0xD0", " 0x71", " 0xF2", " 0x53", " 0x34", " 0x95", " 0x16", " 0xB7", " 0x98", " 0x39", " 0xBA", " 0x1B" };

	/**
	 * constructor configuration panel 1
	 * @param parent
	 * @param style
	 * @param dialog
	 * @param configuration
	 */
	public GPSLoggerSetupConfiguration1(Composite parent, int style, GPSLoggerDialog useDialog, SetupReaderWriter useConfiguration) {
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

	public void updateValues() {
		this.serialNumberText.setText(GDE.STRING_EMPTY + this.configuration.serialNumber);
		this.firmwareText.setText(String.format(" %.2f", this.configuration.firmwareVersion / 100.0)); //$NON-NLS-1$

		this.languageCombo.select(this.configuration.language);
		this.hottSpeedTypeCombo.select(this.configuration.hottSpeedType);
		this.dataRateCombo.select(this.configuration.datarate);
		this.startModusCombo.select(this.configuration.startModus);
		this.stopModusCombo.select(this.configuration.stopModus);
		this.timeZoneCombo.select(this.configuration.timeZone + 12);
		this.timeAutoCombo.select(this.configuration.daylightSavingModus);
		this.varioLimitClimbCombo.select(this.configuration.varioThreshold);
		this.varioLimitSinkCombo.select(this.configuration.varioThresholdSink);
		this.varioToneCombo.select(this.configuration.varioTon);
		this.varioFactorCombo.select(this.configuration.varioFactor);
		this.varioFilterCombo.select(this.configuration.varioFilter);
		this.modusIgcCombo.select(this.configuration.modusIGC);
		this.modusDistanceCombo.select(this.configuration.modusDistance);
		this.telemetryTypeCombo.select(this.configuration.telemetryType);
		this.minMaxRxCombo.select(this.configuration.rxControl);
		this.frskyIdCombo.select(this.configuration.frskyAddr - 1);
		this.fixSerialNumberButton.setSelection(this.configuration.serialNumberFix == 1 ? true : false);
		this.robbeTBoxButton.setSelection(this.configuration.robbe_T_Box == 1 ? true : false);

		this.heightMaxAlarmButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_HEIGHT) > 0);
		this.heightMaxAlarmCombo.setText(String.format("%5d", this.configuration.heightAlarm)); //$NON-NLS-1$
		this.speedMaxAlarmButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_SPEED_MAX) > 0);
		this.speedMaxAlarmCombo.setText(String.format("%5d", this.configuration.speedMaxAlarm)); //$NON-NLS-1$
		this.speedMinAlarmButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_SPEED_MIN) > 0);
		this.speedMinAlarmCombo.setText(String.format("%5d", this.configuration.speedMinAlarm)); //$NON-NLS-1$
		this.distanceMaxButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE_MAX) > 0);
		this.distanceMinButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE_MIN) > 0);
		this.distanceMaxCombo.setText(String.format("%5d", this.configuration.distanceMaxAlarm)); //$NON-NLS-1$
		this.tripLengthButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_TRIP_LENGTH) > 0);
		this.tripLengthCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.tripLengthAlarm / 10.0)); //$NON-NLS-1$
		this.voltageRxButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_RX) > 0);
		this.voltageRxCombo.setText(String.format(Locale.ENGLISH, "%6.2f", this.configuration.voltageRxAlarm / 100.0)); //$NON-NLS-1$

		this.changeVisibility();
	}

	public void changeVisibility() {
		this.frskyIdLabel.setVisible(false);
		this.frskyIdCombo.setVisible(false);
		this.hottSpeedTypeLabel.setVisible(false);
		this.hottSpeedTypeCombo.setVisible(false);
		this.fixSerialNumberButton.setVisible(false);
		this.fixStartPositionButton.setVisible(false);
		this.fixStartPositionButton.setSelection(false);
		this.robbeTBoxButton.setVisible(false);
		if (GPSLoggerSetupConfiguration2.mLinkGroupStatic != null && !GPSLoggerSetupConfiguration2.mLinkGroupStatic.isDisposed()) {
			GPSLoggerSetupConfiguration2.mLinkGroupStatic.setVisible(false);
		}
		if (GPSLoggerSetupConfiguration2.jetiExGroupStatic != null && !GPSLoggerSetupConfiguration2.jetiExGroupStatic.isDisposed()) {
			GPSLoggerSetupConfiguration2.jetiExGroupStatic.setVisible(false);
		}
		if (GPSLoggerSetupConfiguration2.unilogTelemtryAlarmsGroupStatic != null && !GPSLoggerSetupConfiguration2.unilogTelemtryAlarmsGroupStatic.isDisposed()) {
			GPSLoggerSetupConfiguration2.unilogTelemtryAlarmsGroupStatic.setVisible(false);
		}
		if (GPSLoggerSetupConfiguration2.spektrumAdapterGroupStatic != null && !GPSLoggerSetupConfiguration2.spektrumAdapterGroupStatic.isDisposed()) {
			GPSLoggerSetupConfiguration2.spektrumAdapterGroupStatic.setVisible(false);
		}
		if (GPSLoggerSetupConfiguration2.fixGpsStartPositionGroupStatic != null && !GPSLoggerSetupConfiguration2.fixGpsStartPositionGroupStatic.isDisposed()) {
			GPSLoggerSetupConfiguration2.fixGpsStartPositionGroupStatic.setVisible(true);
		}

		if (this.gpsLoggerGroup != null && !this.gpsLoggerGroup.isDisposed()) {
		//0=invalid, 1=Futaba, 2=JR DMSS, 3=HoTT, 4=JetiDuplex, 5=M-Link, 6=FrSky 7=Spektrum 8=PowerBox
			switch (this.telemetryTypeCombo.getSelectionIndex()) {
			case 3: //HoTT
				this.hottSpeedTypeLabel.setVisible(true);
				this.hottSpeedTypeCombo.setVisible(true);
				break;
			case 7: //Spektrum
				if (GPSLoggerSetupConfiguration2.spektrumAdapterGroupStatic != null && !GPSLoggerSetupConfiguration2.spektrumAdapterGroupStatic.isDisposed()) {
					GPSLoggerSetupConfiguration2.spektrumAdapterGroupStatic.setVisible(true);
				}
				break;
			case 6: //FrSky
				this.frskyIdLabel.setVisible(true);
				this.frskyIdCombo.setVisible(true);
				break;
			case 5: //M-Link
				if (GPSLoggerSetupConfiguration2.mLinkGroupStatic != null) {
					GPSLoggerSetupConfiguration2.mLinkGroupStatic.setVisible(true);
				}
				break;
			case 4: //JetiDuplex
				this.fixSerialNumberButton.setVisible(true);
				this.fixStartPositionButton.setVisible(true);
				if (GPSLoggerSetupConfiguration2.fixGpsStartPositionGroupStatic != null) {
					GPSLoggerSetupConfiguration2.fixGpsStartPositionGroupStatic.setVisible(false);
				}
				if (GPSLoggerSetupConfiguration2.jetiExGroupStatic != null) {
					GPSLoggerSetupConfiguration2.jetiExGroupStatic.setVisible(true);
					GPSLoggerSetupConfiguration2.unilogTelemtryAlarmsGroupStatic.setVisible(true);
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

	void initGUI() {
		try {
			this.setLayout(new FormLayout());
			this.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "GPSLoggerSetupConfiguration1.helpRequested, event=" + evt); //$NON-NLS-1$
					GPSLoggerSetupConfiguration1.this.application.openHelpDialog(Messages.getString(MessageIds.GDE_MSGT2010), "HelpInfo.html#configuration");  //$NON-NLS-1$
				}
			});
			{
				this.gpsLoggerGroup = new Group(this, SWT.NONE);
				FormData gpsLoggerGroupLData = new FormData();
				gpsLoggerGroupLData.left = new FormAttachment(0, 1000, 10);
				gpsLoggerGroupLData.top = new FormAttachment(0, 1000, 5);
				gpsLoggerGroupLData.width = 290;
				gpsLoggerGroupLData.height = GDE.IS_MAC ? 440 : 445;
				RowLayout gpsLoggerGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.gpsLoggerGroup.setLayout(gpsLoggerGroupLayout);
				this.gpsLoggerGroup.setLayoutData(gpsLoggerGroupLData);
				this.gpsLoggerGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.gpsLoggerGroup.setText(Messages.getString(MessageIds.GDE_MSGT2031));
				{
					this.serialNumberLabel = new CLabel(this.gpsLoggerGroup, SWT.NONE);
					RowData serialNumberLabelLData = new RowData();
					serialNumberLabelLData.width = GDE.IS_LINUX ? 90 : 100;
					serialNumberLabelLData.height = 18;
					this.serialNumberLabel.setLayoutData(serialNumberLabelLData);
					this.serialNumberLabel.setText(Messages.getString(MessageIds.GDE_MSGT2032));
					this.serialNumberLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.serialNumberText = new Text(this.gpsLoggerGroup, SWT.RIGHT | SWT.BORDER);
					RowData serialNumberTextLData = new RowData();
					serialNumberTextLData.width = 50;
					serialNumberTextLData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.serialNumberText.setLayoutData(serialNumberTextLData);
					this.serialNumberText.setEditable(false);
					this.serialNumberText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.firmwareLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData firmwareLabelLData = new RowData();
					firmwareLabelLData.width = GDE.IS_LINUX ? 65 : 75;
					firmwareLabelLData.height = 18;
					this.firmwareLabel.setLayoutData(firmwareLabelLData);
					this.firmwareLabel.setText(Messages.getString(MessageIds.GDE_MSGT2033));
					this.firmwareLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.firmwareText = new Text(this.gpsLoggerGroup, SWT.BORDER | SWT.RIGHT);
					RowData firmwareTextLData = new RowData();
					firmwareTextLData.width = GDE.IS_LINUX ? 35 : 40;
					firmwareTextLData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.firmwareText.setLayoutData(firmwareTextLData);
					this.firmwareText.setEditable(false);
					this.firmwareText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.fillerComposite = new Composite(this.gpsLoggerGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 280;
					fillerCompositeRA1LData.height = 5;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.telemetryTypeLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData telemetryTypeLabelLData = new RowData();
					telemetryTypeLabelLData.width = 130;
					telemetryTypeLabelLData.height = 18;
					this.telemetryTypeLabel.setLayoutData(telemetryTypeLabelLData);
					this.telemetryTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.telemetryTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT2075));
				}
				{
					this.telemetryTypeCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData telemetryTypeComboLData = new RowData();
					telemetryTypeComboLData.width = 80;
					telemetryTypeComboLData.height = 17;
					this.telemetryTypeCombo.setLayoutData(telemetryTypeComboLData);
					this.telemetryTypeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.telemetryTypeCombo.setItems(this.telemetrieTypes);
					this.telemetryTypeCombo.select(this.configuration.telemetryType);
					this.telemetryTypeCombo.setEditable(false);
					this.telemetryTypeCombo.setBackground(this.application.COLOR_WHITE);
					this.telemetryTypeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "telemetryTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.telemetryType = (short) GPSLoggerSetupConfiguration1.this.telemetryTypeCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							changeVisibility();
						}
					});
				}
				{
					this.fillerComposite = new Composite(this.gpsLoggerGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 45;
					fillerCompositeRA1LData.height = 2;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.addonComposite = new Composite(this.gpsLoggerGroup, SWT.NONE);
					this.addonComposite.setLayout(new FormLayout());
					RowData addonCompositeLData = new RowData();
					addonCompositeLData.width = 285;
					addonCompositeLData.height = 44;
					this.addonComposite.setLayoutData(addonCompositeLData);
					{
						this.fixSerialNumberButton = new Button(this.addonComposite, SWT.CHECK | SWT.LEFT);
						FormData sensorTypeLabelLData = new FormData();
						sensorTypeLabelLData.width = 280;
						sensorTypeLabelLData.height = 18;
						sensorTypeLabelLData.left = new FormAttachment(0, 1000, 2);
						sensorTypeLabelLData.top = new FormAttachment(0, 1000, 0);
						this.fixSerialNumberButton.setLayoutData(sensorTypeLabelLData);
						this.fixSerialNumberButton.setText(Messages.getString(MessageIds.GDE_MSGT2092));
						this.fixSerialNumberButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.fixSerialNumberButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								GPSLoggerSetupConfiguration1.log.log(Level.FINEST, "fixSerialNumberButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								GPSLoggerSetupConfiguration1.this.configuration.serialNumberFix = (byte) (GPSLoggerSetupConfiguration1.this.fixSerialNumberButton.getSelection() ? 1 : 0);
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
						});
					}
					{
						this.fixStartPositionButton = new Button(this.addonComposite, SWT.CHECK | SWT.LEFT);
						FormData sensorTypeLabelLData = new FormData();
						sensorTypeLabelLData.width = 285;
						sensorTypeLabelLData.height = 18;
						sensorTypeLabelLData.left = new FormAttachment(0, 1000, 2);
						sensorTypeLabelLData.top = new FormAttachment(0, 1000, 22);
						this.fixStartPositionButton.setLayoutData(sensorTypeLabelLData);
						this.fixStartPositionButton.setText(Messages.getString(MessageIds.GDE_MSGT2090)  + "|" + Messages.getString(MessageIds.GDE_MSGT2065));
						this.fixStartPositionButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.fixStartPositionButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								GPSLoggerSetupConfiguration1.log.log(Level.FINEST, "fixStartPositionButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (GPSLoggerSetupConfiguration1.this.fixStartPositionButton.getSelection()) {
									GPSLoggerSetupConfiguration2.unilogTelemtryAlarmsGroupStatic.setVisible(false);
									GPSLoggerSetupConfiguration2.fixGpsStartPositionGroupStatic.setVisible(true);
								}
								else {
									GPSLoggerSetupConfiguration2.unilogTelemtryAlarmsGroupStatic.setVisible(true);
									GPSLoggerSetupConfiguration2.fixGpsStartPositionGroupStatic.setVisible(false);
								}
							}
						});
					}
					{
						this.robbeTBoxButton = new Button(this.addonComposite, SWT.CHECK | SWT.LEFT);
						FormData sensorTypeLabelLData = new FormData();
						sensorTypeLabelLData.width = 130;
						sensorTypeLabelLData.height = 18;
						sensorTypeLabelLData.left = new FormAttachment(0, 1000, 2);
						sensorTypeLabelLData.top = new FormAttachment(0, 1000, 22);
						this.robbeTBoxButton.setLayoutData(sensorTypeLabelLData);
						this.robbeTBoxButton.setText(Messages.getString(MessageIds.GDE_MSGT2093));
						this.robbeTBoxButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.robbeTBoxButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								GPSLoggerSetupConfiguration1.log.log(Level.FINEST, "robbeTBoxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								GPSLoggerSetupConfiguration1.this.configuration.robbe_T_Box = (byte) (GPSLoggerSetupConfiguration1.this.robbeTBoxButton.getSelection() ? 1 : 0);
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
						});
					}
					{
						this.frskyIdLabel = new CLabel(this.addonComposite, SWT.RIGHT);
						FormData frskyIdLabelLData = new FormData();
						frskyIdLabelLData.width = 130;
						frskyIdLabelLData.height = 18;
						frskyIdLabelLData.left = new FormAttachment(0, 1000, 0);
						frskyIdLabelLData.top = new FormAttachment(0, 1000, 0);
						this.frskyIdLabel.setLayoutData(frskyIdLabelLData);
						this.frskyIdLabel.setText(Messages.getString(MessageIds.GDE_MSGT2079));
						this.frskyIdLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					}
					{
						this.frskyIdCombo = new CCombo(this.addonComposite, SWT.BORDER);
						FormData sensorTypeComboLData = new FormData();
						sensorTypeComboLData.width = COMBO_WIDTH;
						sensorTypeComboLData.height = 17;
						sensorTypeComboLData.left = new FormAttachment(0, 1000, 133);
						sensorTypeComboLData.top = new FormAttachment(0, 1000, 0);
						this.frskyIdCombo.setLayoutData(sensorTypeComboLData);
						this.frskyIdCombo.setItems(this.frskyIDs);
						this.frskyIdCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.frskyIdCombo.setEditable(false);
						this.frskyIdCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
						this.frskyIdCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								GPSLoggerSetupConfiguration1.log.log(Level.FINEST, "sensorTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								GPSLoggerSetupConfiguration1.this.configuration.frskyAddr = (short) (GPSLoggerSetupConfiguration1.this.frskyIdCombo.getSelectionIndex() + 1);
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
						});
					}
					this.addonComposite.layout();
				}
				{
					this.languageLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData languageLabelLData = new RowData();
					languageLabelLData.width = 130;
					languageLabelLData.height = 18;
					this.languageLabel.setLayoutData(languageLabelLData);
					this.languageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.languageLabel.setText(Messages.getString(MessageIds.GDE_MSGT2014));
				}
				{
					this.languageCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData dataRateCComboLData = new RowData();
					dataRateCComboLData.width = COMBO_WIDTH;
					dataRateCComboLData.height = 17;
					this.languageCombo.setLayoutData(dataRateCComboLData);
					this.languageCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.languageCombo.setItems(this.languageValues);
					this.languageCombo.select(this.configuration.language);
					this.languageCombo.setEditable(false);
					this.languageCombo.setBackground(this.application.COLOR_WHITE);
					this.languageCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "languageCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.language = (byte) GPSLoggerSetupConfiguration1.this.languageCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.hottSpeedTypeLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData speedTypeLabelLData = new RowData();
					speedTypeLabelLData.width = 130;
					speedTypeLabelLData.height = 18;
					this.hottSpeedTypeLabel.setLayoutData(speedTypeLabelLData);
					this.hottSpeedTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.hottSpeedTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGI2003));
				}
				{
					this.hottSpeedTypeCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData dataRateCComboLData = new RowData();
					dataRateCComboLData.width = COMBO_WIDTH;
					dataRateCComboLData.height = 17;
					this.hottSpeedTypeCombo.setLayoutData(dataRateCComboLData);
					this.hottSpeedTypeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.hottSpeedTypeCombo.setItems(this.speedTypeValues);
					this.hottSpeedTypeCombo.select(this.configuration.hottSpeedType);
					this.hottSpeedTypeCombo.setEditable(false);
					this.hottSpeedTypeCombo.setBackground(this.application.COLOR_WHITE);
					this.hottSpeedTypeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "hottSpeedTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.hottSpeedType = (byte) GPSLoggerSetupConfiguration1.this.hottSpeedTypeCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.dataRateLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData dataRateLabelLData = new RowData();
					dataRateLabelLData.width = 130;
					dataRateLabelLData.height = 18;
					this.dataRateLabel.setLayoutData(dataRateLabelLData);
					this.dataRateLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.dataRateLabel.setText(Messages.getString(MessageIds.GDE_MSGT2034));
				}
				{
					this.dataRateCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData dataRateCComboLData = new RowData();
					dataRateCComboLData.width = COMBO_WIDTH;
					dataRateCComboLData.height = 17;
					this.dataRateCombo.setLayoutData(dataRateCComboLData);
					this.dataRateCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.dataRateCombo.setItems(this.dataRateValues);
					this.dataRateCombo.select(this.configuration.datarate);
					this.dataRateCombo.setEditable(false);
					this.dataRateCombo.setBackground(this.application.COLOR_WHITE);
					this.dataRateCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "dataRateCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.datarate = (short) GPSLoggerSetupConfiguration1.this.dataRateCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.startModusLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData startModusLabelLData = new RowData();
					startModusLabelLData.width = 130;
					startModusLabelLData.height = 18;
					this.startModusLabel.setLayoutData(startModusLabelLData);
					this.startModusLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.startModusLabel.setText(Messages.getString(MessageIds.GDE_MSGT2035));
				}
				{
					this.startModusCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData startModusCComboLData = new RowData();
					startModusCComboLData.width = COMBO_WIDTH;
					startModusCComboLData.height = 17;
					this.startModusCombo.setLayoutData(startModusCComboLData);
					this.startModusCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.startModusCombo.setItems(this.startValues);
					this.startModusCombo.select(this.configuration.startModus);
					this.startModusCombo.setEditable(false);
					this.startModusCombo.setBackground(this.application.COLOR_WHITE);
					this.startModusCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "startModusCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.startModus = (short) GPSLoggerSetupConfiguration1.this.startModusCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.stopModusLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData stopModusLabelLData = new RowData();
					stopModusLabelLData.width = 130;
					stopModusLabelLData.height = 18;
					this.stopModusLabel.setLayoutData(stopModusLabelLData);
					this.stopModusLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.stopModusLabel.setText(Messages.getString(MessageIds.GDE_MSGT2026));
				}
				{
					this.stopModusCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData stopModusCComboLData = new RowData();
					stopModusCComboLData.width = COMBO_WIDTH;
					stopModusCComboLData.height = 17;
					this.stopModusCombo.setLayoutData(stopModusCComboLData);
					this.stopModusCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.stopModusCombo.setItems(this.stopValues);
					this.stopModusCombo.select(this.configuration.stopModus);
					this.stopModusCombo.setEditable(false);
					this.stopModusCombo.setBackground(this.application.COLOR_WHITE);
					this.stopModusCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "stopModusCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.stopModus = (short) GPSLoggerSetupConfiguration1.this.stopModusCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.timeZoneLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData timeZoneLabelLData = new RowData();
					timeZoneLabelLData.width = 130;
					timeZoneLabelLData.height = 18;
					this.timeZoneLabel.setLayoutData(timeZoneLabelLData);
					this.timeZoneLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.timeZoneLabel.setText(Messages.getString(MessageIds.GDE_MSGT2036));
				}
				{
					this.timeZoneCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData timeZoneCComboLData = new RowData();
					timeZoneCComboLData.width = COMBO_WIDTH;
					timeZoneCComboLData.height = 17;
					this.timeZoneCombo.setLayoutData(timeZoneCComboLData);
					this.timeZoneCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.timeZoneCombo.setItems(this.deltaUTC);
					this.timeZoneCombo.select(this.configuration.timeZone + 12);
					this.timeZoneCombo.setEditable(false);
					this.timeZoneCombo.setBackground(this.application.COLOR_WHITE);
					this.timeZoneCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "timeZoneCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.timeZone = (short) (GPSLoggerSetupConfiguration1.this.timeZoneCombo.getSelectionIndex() - 12);
							GPSLoggerSetupConfiguration1.this.dialog.device.setUTCdelta(GPSLoggerSetupConfiguration1.this.configuration.timeZone);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.timeZoneUnitLabel = new CLabel(this.gpsLoggerGroup, SWT.CENTER);
					RowData timeZoneUnitLabelLData = new RowData();
					timeZoneUnitLabelLData.width = 79;
					timeZoneUnitLabelLData.height = 18;
					this.timeZoneUnitLabel.setLayoutData(timeZoneUnitLabelLData);
					this.timeZoneUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.timeZoneUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2037));
				}
				{
					this.timeAutoLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData timeZoneLabelLData = new RowData();
					timeZoneLabelLData.width = 130;
					timeZoneLabelLData.height = 18;
					this.timeAutoLabel.setLayoutData(timeZoneLabelLData);
					this.timeAutoLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.timeAutoLabel.setText(Messages.getString(MessageIds.GDE_MSGT2073));
				}
				{
					this.timeAutoCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData timeZoneCComboLData = new RowData();
					timeZoneCComboLData.width = COMBO_WIDTH;
					timeZoneCComboLData.height = 17;
					this.timeAutoCombo.setLayoutData(timeZoneCComboLData);
					this.timeAutoCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.timeAutoCombo.setItems(Messages.getString(MessageIds.GDE_MSGT2070).split(GDE.STRING_COMMA));
					this.timeAutoCombo.select(this.configuration.daylightSavingModus);
					this.timeAutoCombo.setEditable(false);
					this.timeAutoCombo.setBackground(this.application.COLOR_WHITE);
					this.timeAutoCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "timeAutoCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.timeZone = (short) (GPSLoggerSetupConfiguration1.this.timeAutoCombo.getSelectionIndex());
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioLimitClimbLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData varioLimitLabelLData = new RowData();
					varioLimitLabelLData.width = 130;
					varioLimitLabelLData.height = 18;
					this.varioLimitClimbLabel.setLayoutData(varioLimitLabelLData);
					this.varioLimitClimbLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitClimbLabel.setText(Messages.getString(MessageIds.GDE_MSGT2038));
				}
				{
					this.varioLimitClimbCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER | SWT.RIGHT);
					RowData varioLimitCComboLData = new RowData();
					varioLimitCComboLData.width = COMBO_WIDTH;
					varioLimitCComboLData.height = 17;
					this.varioLimitClimbCombo.setLayoutData(varioLimitCComboLData);
					this.varioLimitClimbCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitClimbCombo.setItems(this.varioThresholds);
					this.varioLimitClimbCombo.select(this.configuration.varioThreshold);
					this.varioLimitClimbCombo.setEditable(false);
					this.varioLimitClimbCombo.setBackground(this.application.COLOR_WHITE);
					this.varioLimitClimbCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "varioLimitCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.varioThreshold = (short) GPSLoggerSetupConfiguration1.this.varioLimitClimbCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioLimitUnitLabel = new CLabel(this.gpsLoggerGroup, SWT.CENTER);
					RowData varioLimitUnitLabelLData = new RowData();
					varioLimitUnitLabelLData.width = 79;
					varioLimitUnitLabelLData.height = 18;
					this.varioLimitUnitLabel.setLayoutData(varioLimitUnitLabelLData);
					this.varioLimitUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2039));
				}
				{
					this.varioLimitSinkLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData varioLimitLabelLData = new RowData();
					varioLimitLabelLData.width = 130;
					varioLimitLabelLData.height = 18;
					this.varioLimitSinkLabel.setLayoutData(varioLimitLabelLData);
					this.varioLimitSinkLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitSinkLabel.setText(Messages.getString(MessageIds.GDE_MSGT2074));
				}
				{
					this.varioLimitSinkCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER | SWT.RIGHT);
					RowData varioLimitCComboLData = new RowData();
					varioLimitCComboLData.width = COMBO_WIDTH;
					varioLimitCComboLData.height = 17;
					this.varioLimitSinkCombo.setLayoutData(varioLimitCComboLData);
					this.varioLimitSinkCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitSinkCombo.setItems(this.varioThresholds);
					this.varioLimitSinkCombo.select(this.configuration.varioThreshold);
					this.varioLimitSinkCombo.setEditable(false);
					this.varioLimitSinkCombo.setBackground(this.application.COLOR_WHITE);
					this.varioLimitSinkCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "varioLimitSinkCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.varioThresholdSink = (short) GPSLoggerSetupConfiguration1.this.varioLimitSinkCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioLimitUnitLabel = new CLabel(this.gpsLoggerGroup, SWT.CENTER);
					RowData varioLimitUnitLabelLData = new RowData();
					varioLimitUnitLabelLData.width = 79;
					varioLimitUnitLabelLData.height = 18;
					this.varioLimitUnitLabel.setLayoutData(varioLimitUnitLabelLData);
					this.varioLimitUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2039));
				}
				{
					this.varioTonLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData varioTonLabelLData = new RowData();
					varioTonLabelLData.width = 130;
					varioTonLabelLData.height = 18;
					this.varioTonLabel.setLayoutData(varioTonLabelLData);
					this.varioTonLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioTonLabel.setText(Messages.getString(MessageIds.GDE_MSGT2040));
				}
				{
					this.varioToneCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData varioToneComboLData = new RowData();
					varioToneComboLData.width = COMBO_WIDTH;
					varioToneComboLData.height = 17;
					this.varioToneCombo.setLayoutData(varioToneComboLData);
					this.varioToneCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioToneCombo.setItems(this.varioTons);
					this.varioToneCombo.select(this.configuration.varioTon);
					this.varioToneCombo.setEditable(false);
					this.varioToneCombo.setBackground(this.application.COLOR_WHITE);
					this.varioToneCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "varioToneCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.varioTon = (short) GPSLoggerSetupConfiguration1.this.varioToneCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioFactorLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData varioFactorLabelLData = new RowData();
					varioFactorLabelLData.width = 130;
					varioFactorLabelLData.height = 18;
					this.varioFactorLabel.setLayoutData(varioFactorLabelLData);
					this.varioFactorLabel.setText(Messages.getString(MessageIds.GDE_MSGT2078));
					this.varioFactorLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.varioFactorCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER | SWT.CENTER);
					RowData varioFactorComboLData = new RowData();
					varioFactorComboLData.width = COMBO_WIDTH;
					varioFactorComboLData.height = 17;
					this.varioFactorCombo.setLayoutData(varioFactorComboLData);
					this.varioFactorCombo.setItems(this.varioFactors);
					this.varioFactorCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioFactorCombo.setEditable(false);
					this.varioFactorCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.varioFactorCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration1.log.log(Level.FINEST, "varioFactorCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.varioFactor = (short) (GPSLoggerSetupConfiguration1.this.varioFactorCombo.getSelectionIndex());
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioFilterLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData varioFilterLabelLData = new RowData();
					varioFilterLabelLData.width = 130;
					varioFilterLabelLData.height = 18;
					this.varioFilterLabel.setLayoutData(varioFilterLabelLData);
					this.varioFilterLabel.setText("Vario Filter");
					this.varioFilterLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.varioFilterCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER | SWT.CENTER);
					RowData varioFilterComboLData = new RowData();
					varioFilterComboLData.width = COMBO_WIDTH;
					varioFilterComboLData.height = 17;
					this.varioFilterCombo.setLayoutData(varioFilterComboLData);
					this.varioFilterCombo.setItems(this.varioFilters);
					this.varioFilterCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioFilterCombo.setEditable(false);
					this.varioFilterCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.varioFilterCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration1.log.log(Level.FINEST, "varioFilterCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.varioFilter = (short) (GPSLoggerSetupConfiguration1.this.varioFilterCombo.getSelectionIndex());
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.smoothAltitudeLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData smoothAltitudeLabelLData = new RowData();
					smoothAltitudeLabelLData.width = 130;
					smoothAltitudeLabelLData.height = 18;
					this.smoothAltitudeLabel.setLayoutData(smoothAltitudeLabelLData);
					this.smoothAltitudeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.smoothAltitudeLabel.setText(Messages.getString(MessageIds.GDE_MSGI2001));
				}
				{
					this.smoothAltitudeCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData smoothAltitudeComboLData = new RowData();
					smoothAltitudeComboLData.width = COMBO_WIDTH;
					smoothAltitudeComboLData.height = 17;
					this.smoothAltitudeCombo.setLayoutData(smoothAltitudeComboLData);
					this.smoothAltitudeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.smoothAltitudeCombo.setItems(this.smoothAltNull);
					this.smoothAltitudeCombo.select(this.configuration.smoothAltitudeNulling);
					this.smoothAltitudeCombo.setEditable(false);
					this.smoothAltitudeCombo.setBackground(this.application.COLOR_WHITE);
					this.smoothAltitudeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "varioToneCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.smoothAltitudeNulling = (byte) GPSLoggerSetupConfiguration1.this.smoothAltitudeCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.modusIgcLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData modusIgcLabelLData = new RowData();
					modusIgcLabelLData.width = 130;
					modusIgcLabelLData.height = 18;
					this.modusIgcLabel.setLayoutData(modusIgcLabelLData);
					this.modusIgcLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.modusIgcLabel.setText(Messages.getString(MessageIds.GDE_MSGT2068));
				}
				{
					this.modusIgcCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData modusIgcComboLData = new RowData();
					modusIgcComboLData.width = COMBO_WIDTH;
					modusIgcComboLData.height = 17;
					this.modusIgcCombo.setLayoutData(modusIgcComboLData);
					this.modusIgcCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.modusIgcCombo.setItems(this.igcModes);
					this.modusIgcCombo.select(this.configuration.modusIGC);
					this.modusIgcCombo.setEditable(false);
					this.modusIgcCombo.setBackground(this.application.COLOR_WHITE);
					this.modusIgcCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "varioToneCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.modusIGC = (short) GPSLoggerSetupConfiguration1.this.modusIgcCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.modusDistanceLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData modusDistanceLabelLData = new RowData();
					modusDistanceLabelLData.width = 130;
					modusDistanceLabelLData.height = 18;
					this.modusDistanceLabel.setLayoutData(modusDistanceLabelLData);
					this.modusDistanceLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.modusDistanceLabel.setText(Messages.getString(MessageIds.GDE_MSGT2027));
				}
				{
					this.modusDistanceCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData modusDistanceComboLData = new RowData();
					modusDistanceComboLData.width = COMBO_WIDTH;
					modusDistanceComboLData.height = 17;
					this.modusDistanceCombo.setLayoutData(modusDistanceComboLData);
					this.modusDistanceCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.modusDistanceCombo.setItems(this.distanceModes);
					this.modusDistanceCombo.select(this.configuration.modusDistance);
					this.modusDistanceCombo.setEditable(false);
					this.modusDistanceCombo.setBackground(this.application.COLOR_WHITE);
					this.modusDistanceCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "modusDistanceCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.modusDistance = (short) GPSLoggerSetupConfiguration1.this.modusDistanceCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.minMaxRxLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData minMaxRxLabelLData = new RowData();
					minMaxRxLabelLData.width = 130;
					minMaxRxLabelLData.height = 18;
					this.minMaxRxLabel.setLayoutData(minMaxRxLabelLData);
					this.minMaxRxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2080));
					this.minMaxRxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.minMaxRxCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData minMaxRxComboLData = new RowData();
					minMaxRxComboLData.width = COMBO_WIDTH;
					minMaxRxComboLData.height = 17;
					this.minMaxRxCombo.setLayoutData(minMaxRxComboLData);
					this.minMaxRxCombo.setItems(Messages.getString(MessageIds.GDE_MSGT2081).split(GDE.STRING_COMMA));
					this.minMaxRxCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.minMaxRxCombo.setEditable(false);
					this.minMaxRxCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.minMaxRxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration1.log.log(Level.FINEST, "minMaxRxCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.rxControl = (short) (GPSLoggerSetupConfiguration1.this.minMaxRxCombo.getSelectionIndex());
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
			}
			{
				this.gpsTelemertieAlarmGroup = new Group(this, SWT.NONE);
				FormData gpsTelemertieGroupLData = new FormData();
				gpsTelemertieGroupLData.left = new FormAttachment(0, 1000, 10);
				gpsTelemertieGroupLData.top = new FormAttachment(0, 1000, 470);
				gpsTelemertieGroupLData.width = 290;
				gpsTelemertieGroupLData.height = 155;
				this.gpsTelemertieAlarmGroup.setLayoutData(gpsTelemertieGroupLData);
				this.gpsTelemertieAlarmGroup.setLayout(new RowLayout(org.eclipse.swt.SWT.HORIZONTAL));
				this.gpsTelemertieAlarmGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.gpsTelemertieAlarmGroup.setText(Messages.getString(MessageIds.GDE_MSGT2041));
				{
					this.fillerComposite = new Composite(this.gpsTelemertieAlarmGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 275;
					fillerCompositeRALData.height = 5;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieAlarmGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 15;
					fillerCompositeRALData.height = 18;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.heightMaxAlarmButton = new Button(this.gpsTelemertieAlarmGroup, SWT.CHECK | SWT.LEFT);
					RowData heightButtonLData = new RowData();
					heightButtonLData.width = 100;
					heightButtonLData.height = 16;
					this.heightMaxAlarmButton.setLayoutData(heightButtonLData);
					this.heightMaxAlarmButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightMaxAlarmButton.setText(Messages.getString(MessageIds.GDE_MSGT2030) + " max");
					this.heightMaxAlarmButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_HEIGHT) > 0);
					this.heightMaxAlarmButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "heightButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.heightMaxAlarmButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_HEIGHT);
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_HEIGHT);
							}
							GPSLoggerSetupConfiguration1.this.heightMaxAlarmButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_HEIGHT) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.heightMaxAlarmCombo = new CCombo(this.gpsTelemertieAlarmGroup, SWT.BORDER);
					RowData heightCComboLData = new RowData();
					heightCComboLData.width = 70;
					heightCComboLData.height = 17;
					this.heightMaxAlarmCombo.setLayoutData(heightCComboLData);
					this.heightMaxAlarmCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightMaxAlarmCombo.setItems(this.heightValues);
					this.heightMaxAlarmCombo.setText(String.format("%5d", this.configuration.heightAlarm)); //$NON-NLS-1$
					this.heightMaxAlarmCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "heightCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.heightAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.heightMaxAlarmCombo.getText().trim());
							GPSLoggerSetupConfiguration1.this.configuration.heightAlarm = GPSLoggerSetupConfiguration1.this.configuration.heightAlarm < 10 ? 10
									: GPSLoggerSetupConfiguration1.this.configuration.heightAlarm > 4000 ? 4000 : GPSLoggerSetupConfiguration1.this.configuration.heightAlarm;
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.heightMaxAlarmCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "heightCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.heightMaxAlarmCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "heightCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration1.this.configuration.heightAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.heightMaxAlarmCombo.getText().trim());
								GPSLoggerSetupConfiguration1.this.configuration.heightAlarm = GPSLoggerSetupConfiguration1.this.configuration.heightAlarm < 10 ? 10
										: GPSLoggerSetupConfiguration1.this.configuration.heightAlarm > 4000 ? 4000 : GPSLoggerSetupConfiguration1.this.configuration.heightAlarm;
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.heightMaxAlarmLabel = new CLabel(this.gpsTelemertieAlarmGroup, SWT.CENTER);
					RowData heightLabelLData = new RowData();
					heightLabelLData.width = 89;
					heightLabelLData.height = 18;
					this.heightMaxAlarmLabel.setLayoutData(heightLabelLData);
					this.heightMaxAlarmLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightMaxAlarmLabel.setText(Messages.getString(MessageIds.GDE_MSGT2042));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieAlarmGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 15;
					fillerCompositeRALData.height = 18;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.speedMaxAlarmButton = new Button(this.gpsTelemertieAlarmGroup, SWT.CHECK | SWT.LEFT);
					RowData velocityButtonLData = new RowData();
					velocityButtonLData.width = 100;
					velocityButtonLData.height = 16;
					this.speedMaxAlarmButton.setLayoutData(velocityButtonLData);
					this.speedMaxAlarmButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.speedMaxAlarmButton.setText(Messages.getString(MessageIds.GDE_MSGT2043) + " max");
					this.speedMaxAlarmButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_SPEED_MAX) > 0);
					this.speedMaxAlarmButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "velocityButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.speedMaxAlarmButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_SPEED_MAX);
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_SPEED_MAX);
							}
							GPSLoggerSetupConfiguration1.this.speedMaxAlarmButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_SPEED_MAX) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.speedMaxAlarmCombo = new CCombo(this.gpsTelemertieAlarmGroup, SWT.BORDER);
					RowData velocityCComboLData = new RowData();
					velocityCComboLData.width = 70;
					velocityCComboLData.height = 17;
					this.speedMaxAlarmCombo.setLayoutData(velocityCComboLData);
					this.speedMaxAlarmCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.speedMaxAlarmCombo.setItems(this.speedValues);
					this.speedMaxAlarmCombo.setText(String.format("%5d", this.configuration.speedMaxAlarm)); //$NON-NLS-1$
					this.speedMaxAlarmCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "velocityCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.speedMaxAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.speedMaxAlarmCombo.getText().trim());
							GPSLoggerSetupConfiguration1.this.configuration.speedMaxAlarm = GPSLoggerSetupConfiguration1.this.configuration.speedMaxAlarm < 10 ? 10
									: GPSLoggerSetupConfiguration1.this.configuration.speedMaxAlarm > 1000 ? 1000 : GPSLoggerSetupConfiguration1.this.configuration.speedMaxAlarm;
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.speedMaxAlarmCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "velocityCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.speedMaxAlarmCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "velocityCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration1.this.configuration.speedMaxAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.speedMaxAlarmCombo.getText().trim());
								GPSLoggerSetupConfiguration1.this.configuration.speedMaxAlarm = GPSLoggerSetupConfiguration1.this.configuration.speedMaxAlarm < 10 ? 10
										: GPSLoggerSetupConfiguration1.this.configuration.speedMaxAlarm > 1000 ? 1000 : GPSLoggerSetupConfiguration1.this.configuration.speedMaxAlarm;
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.speedAlarmLabel = new CLabel(this.gpsTelemertieAlarmGroup, SWT.CENTER);
					RowData velocityLabelLData = new RowData();
					velocityLabelLData.width = 89;
					velocityLabelLData.height = 18;
					this.speedAlarmLabel.setLayoutData(velocityLabelLData);
					this.speedAlarmLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.speedAlarmLabel.setText(Messages.getString(MessageIds.GDE_MSGT2044));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieAlarmGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 15;
					fillerCompositeRALData.height = 18;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.speedMinAlarmButton = new Button(this.gpsTelemertieAlarmGroup, SWT.CHECK | SWT.LEFT);
					RowData velocityButtonLData = new RowData();
					velocityButtonLData.width = 100;
					velocityButtonLData.height = 16;
					this.speedMinAlarmButton.setLayoutData(velocityButtonLData);
					this.speedMinAlarmButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.speedMinAlarmButton.setText(Messages.getString(MessageIds.GDE_MSGT2043) + " min");
					this.speedMinAlarmButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_SPEED_MIN) > 0);
					this.speedMinAlarmButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "velocityButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.speedMinAlarmButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_SPEED_MIN);
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_SPEED_MIN);
							}
							GPSLoggerSetupConfiguration1.this.speedMinAlarmButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_SPEED_MIN) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.speedMinAlarmCombo = new CCombo(this.gpsTelemertieAlarmGroup, SWT.BORDER);
					RowData velocityCComboLData = new RowData();
					velocityCComboLData.width = 70;
					velocityCComboLData.height = 17;
					this.speedMinAlarmCombo.setLayoutData(velocityCComboLData);
					this.speedMinAlarmCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.speedMinAlarmCombo.setItems(this.speedValues);
					this.speedMinAlarmCombo.setText(String.format("%5d", this.configuration.speedMinAlarm)); //$NON-NLS-1$
					this.speedMinAlarmCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "velocityCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.speedMinAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.speedMinAlarmCombo.getText().trim());
							GPSLoggerSetupConfiguration1.this.configuration.speedMinAlarm = GPSLoggerSetupConfiguration1.this.configuration.speedMinAlarm < 10 ? 10
									: GPSLoggerSetupConfiguration1.this.configuration.speedMinAlarm > 1000 ? 1000 : GPSLoggerSetupConfiguration1.this.configuration.speedMinAlarm;
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.speedMinAlarmCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "velocityCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.speedMinAlarmCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "velocityCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration1.this.configuration.speedMinAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.speedMinAlarmCombo.getText().trim());
								GPSLoggerSetupConfiguration1.this.configuration.speedMinAlarm = GPSLoggerSetupConfiguration1.this.configuration.speedMinAlarm < 10 ? 10
										: GPSLoggerSetupConfiguration1.this.configuration.speedMinAlarm > 1000 ? 1000 : GPSLoggerSetupConfiguration1.this.configuration.speedMinAlarm;
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.speedAlarmLabel = new CLabel(this.gpsTelemertieAlarmGroup, SWT.CENTER);
					RowData velocityLabelLData = new RowData();
					velocityLabelLData.width = 89;
					velocityLabelLData.height = 18;
					this.speedAlarmLabel.setLayoutData(velocityLabelLData);
					this.speedAlarmLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.speedAlarmLabel.setText(Messages.getString(MessageIds.GDE_MSGT2044));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieAlarmGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 15;
					fillerCompositeRALData.height = 18;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.distanceMaxButton = new Button(this.gpsTelemertieAlarmGroup, SWT.CHECK | SWT.LEFT);
					RowData distanceButtonLData = new RowData();
					distanceButtonLData.width = 100;
					distanceButtonLData.height = 16;
					this.distanceMaxButton.setLayoutData(distanceButtonLData);
					this.distanceMaxButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceMaxButton.setText(Messages.getString(MessageIds.GDE_MSGT2045));
					this.distanceMaxButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE_MAX) > 0);
					this.distanceMaxButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "distanceButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.distanceMaxButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_DISTANCE_MAX);
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_DISTANCE_MAX);
							}
							GPSLoggerSetupConfiguration1.this.distanceMaxButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE_MAX) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.distanceMaxCombo = new CCombo(this.gpsTelemertieAlarmGroup, SWT.BORDER);
					RowData distanceCComboLData = new RowData();
					distanceCComboLData.width = 70;
					distanceCComboLData.height = 17;
					this.distanceMaxCombo.setLayoutData(distanceCComboLData);
					this.distanceMaxCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceMaxCombo.setItems(this.distanceValues);
					this.distanceMaxCombo.setText(String.format("%5d", this.configuration.distanceMaxAlarm)); //$NON-NLS-1$
					this.distanceMaxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "distanceCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.distanceMaxAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.distanceMaxCombo.getText().trim());
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.distanceMaxCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "distanceCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.distanceMaxCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "distanceCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration1.this.configuration.distanceMaxAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.distanceMaxCombo.getText().trim());
								GPSLoggerSetupConfiguration1.this.configuration.distanceMaxAlarm = GPSLoggerSetupConfiguration1.this.configuration.distanceMaxAlarm < 10 ? 10
										: GPSLoggerSetupConfiguration1.this.configuration.distanceMaxAlarm > 5000 ? 5000 : GPSLoggerSetupConfiguration1.this.configuration.distanceMaxAlarm;
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.distanceMaxLabel = new CLabel(this.gpsTelemertieAlarmGroup, SWT.CENTER);
					RowData distanceLabelLData = new RowData();
					distanceLabelLData.width = 89;
					distanceLabelLData.height = 18;
					this.distanceMaxLabel.setLayoutData(distanceLabelLData);
					this.distanceMaxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceMaxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2046));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieAlarmGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 15;
					fillerCompositeRALData.height = 18;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.distanceMinButton = new Button(this.gpsTelemertieAlarmGroup, SWT.CHECK | SWT.LEFT);
					RowData distanceButtonLData = new RowData();
					distanceButtonLData.width = 100;
					distanceButtonLData.height = 16;
					this.distanceMinButton.setLayoutData(distanceButtonLData);
					this.distanceMinButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceMinButton.setText(Messages.getString(MessageIds.GDE_MSGT2082));
					this.distanceMinButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE_MIN) > 0);
					this.distanceMinButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "distanceMinButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.distanceMinButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_DISTANCE_MIN);
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_DISTANCE_MIN);
							}
							GPSLoggerSetupConfiguration1.this.distanceMinButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE_MIN) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.distanceMinCombo = new CCombo(this.gpsTelemertieAlarmGroup, SWT.BORDER);
					RowData distanceCComboLData = new RowData();
					distanceCComboLData.width = 70;
					distanceCComboLData.height = 17;
					this.distanceMinCombo.setLayoutData(distanceCComboLData);
					this.distanceMinCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceMinCombo.setItems(this.distanceValues);
					this.distanceMinCombo.setText(String.format("%5d", this.configuration.distanceMinAlarm)); //$NON-NLS-1$
					this.distanceMinCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "distanceMinCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.distanceMinAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.distanceMinCombo.getText().trim());
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.distanceMinCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "distanceMinCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.distanceMinCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "distanceMinCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration1.this.configuration.distanceMinAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.distanceMinCombo.getText().trim());
								GPSLoggerSetupConfiguration1.this.configuration.distanceMinAlarm = GPSLoggerSetupConfiguration1.this.configuration.distanceMinAlarm < 0 ? 0
										: GPSLoggerSetupConfiguration1.this.configuration.distanceMinAlarm > 5000 ? 5000 : GPSLoggerSetupConfiguration1.this.configuration.distanceMinAlarm;
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.distanceMinLabel = new CLabel(this.gpsTelemertieAlarmGroup, SWT.CENTER);
					RowData distanceLabelLData = new RowData();
					distanceLabelLData.width = 89;
					distanceLabelLData.height = 18;
					this.distanceMinLabel.setLayoutData(distanceLabelLData);
					this.distanceMinLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceMinLabel.setText(Messages.getString(MessageIds.GDE_MSGT2046));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieAlarmGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 15;
					fillerCompositeRALData.height = 18;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.tripLengthButton = new Button(this.gpsTelemertieAlarmGroup, SWT.CHECK | SWT.LEFT);
					RowData pathLengthButtonLData = new RowData();
					pathLengthButtonLData.width = 100;
					pathLengthButtonLData.height = 16;
					this.tripLengthButton.setLayoutData(pathLengthButtonLData);
					this.tripLengthButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tripLengthButton.setText(Messages.getString(MessageIds.GDE_MSGT2047));
					this.tripLengthButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_TRIP_LENGTH) > 0);
					this.tripLengthButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "pathLengthButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.tripLengthButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_TRIP_LENGTH);
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_TRIP_LENGTH);
							}
							GPSLoggerSetupConfiguration1.this.tripLengthButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_TRIP_LENGTH) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData pathLengthCComboLData = new RowData();
					pathLengthCComboLData.width = 70;
					pathLengthCComboLData.height = 17;
					this.tripLengthCombo = new CCombo(this.gpsTelemertieAlarmGroup, SWT.BORDER);
					this.tripLengthCombo.setLayoutData(pathLengthCComboLData);
					this.tripLengthCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tripLengthCombo.setItems(this.tripValues);
					this.tripLengthCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.tripLengthAlarm / 10.0)); //$NON-NLS-1$
					this.tripLengthCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "tripLengthCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm = (short) (Double.parseDouble(GPSLoggerSetupConfiguration1.this.tripLengthCombo.getText().trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 10);
							GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm = GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm < 1 ? 1
									: GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm > 999 ? 999 : GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm;
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.distanceMaxCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "tripLengthCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.distanceMaxCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "tripLengthCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm = (short) (Double.parseDouble(GPSLoggerSetupConfiguration1.this.tripLengthCombo.getText().trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 10);
								GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm = GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm < 1 ? 1
										: GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm > 999 ? 999 : GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm;
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.tripLengthLabel = new CLabel(this.gpsTelemertieAlarmGroup, SWT.CENTER);
					RowData pathLengthLabelLData = new RowData();
					pathLengthLabelLData.width = 89;
					pathLengthLabelLData.height = 18;
					this.tripLengthLabel.setLayoutData(pathLengthLabelLData);
					this.tripLengthLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tripLengthLabel.setText(Messages.getString(MessageIds.GDE_MSGT2048));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieAlarmGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 15;
					fillerCompositeRALData.height = 18;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.voltageRxButton = new Button(this.gpsTelemertieAlarmGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageRxButtonLData = new RowData();
					voltageRxButtonLData.width = 100;
					voltageRxButtonLData.height = 16;
					this.voltageRxButton.setLayoutData(voltageRxButtonLData);
					this.voltageRxButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageRxButton.setText(Messages.getString(MessageIds.GDE_MSGT2049));
					this.voltageRxButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_RX) > 0);
					this.voltageRxButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageRxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.voltageRxButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_VOLTAGE_RX);
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_VOLTAGE_RX);
							}
							GPSLoggerSetupConfiguration1.this.voltageRxButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_RX) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.voltageRxCombo = new CCombo(this.gpsTelemertieAlarmGroup, SWT.BORDER);
					RowData voltageRxCComboLData = new RowData();
					voltageRxCComboLData.width = 70;
					voltageRxCComboLData.height = 17;
					this.voltageRxCombo.setLayoutData(voltageRxCComboLData);
					this.voltageRxCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageRxCombo.setItems(this.voltageRxValues);
					this.voltageRxCombo.setText(String.format(Locale.ENGLISH, "%6.2f", this.configuration.voltageRxAlarm / 100.0)); //$NON-NLS-1$
					this.voltageRxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageRxCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm = (short) (Double.parseDouble(GPSLoggerSetupConfiguration1.this.voltageRxCombo.getText().trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 100);
							GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm = GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm < 300 ? 300
									: GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm > 800 ? 800 : GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm;
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.voltageRxCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "voltageRxCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.voltageRxCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "voltageRxCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm = (short) (Double.parseDouble(GPSLoggerSetupConfiguration1.this.voltageRxCombo.getText().trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 100);
								GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm = GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm < 300 ? 300
										: GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm > 800 ? 800 : GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm;
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.voltageRxLabel = new CLabel(this.gpsTelemertieAlarmGroup, SWT.CENTER);
					RowData voltageRxLabelLData = new RowData();
					voltageRxLabelLData.width = 89;
					voltageRxLabelLData.height = 18;
					this.voltageRxLabel.setLayoutData(voltageRxLabelLData);
					this.voltageRxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageRxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2050));
				}
			}
			this.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
