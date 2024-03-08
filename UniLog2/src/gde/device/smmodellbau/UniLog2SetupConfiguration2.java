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
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

/**
 * class to implement SM UniLog2 and Multiplex M-Link reverse link configuration panel 2
 */
public class UniLog2SetupConfiguration2 extends org.eclipse.swt.widgets.Composite {
	final static Logger							log											= Logger.getLogger(UniLog2SetupConfiguration2.class.getName());

	final UniLog2Dialog							dialog;
	final DataExplorer							application;

	Composite												fillerComposite;

	Group														unilogTelemtryAlarmsGroup;
	CLabel													addressLabel1UL, addressLabel2UL, addressLabel3UL, addressLabel4UL;
	Text														addressText1UL, addressText2UL, addressText3UL, addressText4UL;
	Slider													addressSlider1UL, addressSlider2UL, addressSlider3UL, addressSlider4UL;
	Button													currentButton, voltageStartButton, voltageButton, capacityButton, heightButton, voltageRxButton, cellVoltageButton, energyButton, rpmMinButton, rpmMaxButton, 
			analogAlarm1Button, analogAlarm1DirectionButton, analogAlarm2Button, analogAlarm2DirectionButton, analogAlarm3Button, analogAlarm3DirectionButton;
	CCombo													currentCombo, voltageStartCombo, voltageCombo, capacityCombo, heightCombo, voltageRxCombo, cellVoltageCombo, energyCombo, rpmMinCombo, rpmMaxCombo, 
			analogAlarm1Combo, analogAlarm2Combo, analogAlarm3Combo;
	CLabel													currentLabel, voltageStartLabel, voltageLabel, capacityLabel, heightLabel, voltageRxLabel, cellVoltageLabel, energyLabel, rpmMinLabel, rpmMaxLabel, 
			analogAlarm1Label, analogAlarm2Label, analogAlarm3Label;
	CLabel													a1Label, a2Label, a3Label, c1Label, c2Label, c3Label, c4Label, c5Label, c6Label;
	CCombo													a1Combo, a2Combo, a3Combo, c1Combo, c2Combo, c3Combo, c4Combo, c5Combo, c6Combo;
	final int												fillerWidth							= 15;
	final int												buttonWidth							= 116;
	final int												comboWidth							= 84;

	Group														jetiExGroup;
	static Group										jetiExGroupStatic;
	Button													measurement1, measurement2, measurement3, measurement4, measurement5, measurement6, measurement7, measurement8, measurement9, measurement10, measurement11,
			measurement12, measurement13, measurement14, measurement15, measurement16, measurement17, measurement18, measurement19, measurement20;
	CLabel 													jetiExSelectionLabel;

	Group														mLinkAddressesGroup;
	static Group										mLinkGroupStatic;
	CLabel													addressVoltageLabel, addressCurrentLabel, addressRevolutionLabel, addressCapacityLabel, addressVarioLabel, addressHeightLabel, addressIntHeightLabel, 
			addressEnergyLabel, addressRemainCapLabel, cellMinimumLabel;
	Text														addressVoltageText, addressCurrentText, addressRevolutionText, addressCapacityText, addressVarioText, addressHeightText, addressHeightGainText, 
			addressEnergyText, addressRemainCapText, cellMinimumText;
	Slider													addressVoltageSlider, addressCurrentSlider, addressRevolutionSlider, addressCapacitySlider, addressVarioSlider, addressHeightSlider, addressHeightGainSlider,
			addressEnergySlider, addressRemainCapSlider, cellMinimumSlider;

	Group														spektrumAdapterGroup;
	static Group										spektrumAdapterGroupStatic;
	CLabel													spektrumAddressLabel;
	CCombo													spektrumAddressCombo;
	final String[]									spektrumAddresses = { " 0 ", " 1 ", " 2 ", " 3 ", " 4 ", " 5 ", " 6 ", " 7 ", " 8 ", " 9 "};
	Button													spektrumEscSensor, spektrumCurrentSensor, spektrumVarioSensor, spektrumLiPoMonitor;

	final int												labelWidth							= 110;
	final int												textWidth								= 25;
	final int												sliderWidth							= 125;
	final int												sliderMinimum						= 0;
	final int												sliderMaximum						= 26;
	final int												sliderIncrement					= 1;
	final String[]									sliderValues						= { "  0", "  1", "  2", "  3", "  4", "  5", "  6", "  7", "  8", "  9", " 10", " 11", " 12", " 13", " 14", " 15", " - -" };

	final String[]									currentValues						= { "    1", "    5", "   10", "   15", "   20", "   30", "   40", "   50", "   60", "   70", "   80", "   90", "  100", "  150", "  200",
			"  250", "  300", "  350", "  400"									};
	final String[]									voltageStartValues			= { "  3.0", "  3.2", "  3.3", "  3.4", "  3.5", "  4.0", "  4.5", "  5.0", "  5.5", "  6.0", "  6.4", "  6.5", "  6.6", "  6.7", "  6.8",
			"  7.0", "  7.5", "  8.0", "  8.5", "  9.0", "  9.5", "  9.6", "  9.7", "  9.8", " 10.0", " 10.5", " 11.0", " 11.5", " 12.0", " 12.2", " 12.3", " 12.4", " 12.5", " 13.0", " 13.5", " 14.0",
			" 14.5", " 15.0", " 15.5", " 15.6", " 15.7", " 15.8", " 15.9", " 16.0", " 16.5", " 17.0", " 17.5", " 18.0", " 18.5", " 19.0", " 19.5", " 20.0", " 20.3", " 20.3", " 20.4", " 20.5", " 20.6",
			" 20.7", " 21.0", " 21.5", " 22.0", " 23.0", " 24.0", " 24.5", " 24.6", " 24.7", " 24.8", " 25.0", " 30.0", " 40.0", " 50.0", " 60.0" };

	final String[]									capacityValues					= { "  250", "  500", "  750", " 1000", " 1250", " 1500", " 1750", " 2000", " 2250", " 2500", " 2750", " 3000", " 3250", " 3500", " 4000",
			" 5000", " 6000"																		};
	final String[]									heightThresholds				= { "   10", "   25", "   50", "   75", "  100", "  150", "  200", "  250", "  300", "  350", "  400", "  450", "  500", "  600", "  700",
			"  800", "  900", " 1000", " 1250", " 1500", " 2000", " 2500", " 3000", " 3500", " 4000" };
	final String[]									voltageRxStartValues		= { "  3.00", "  3.25", "  3.50", "  3.75", "  4.00", "  4.25", "  4.50", "  4.75", "  4.80", "  4.85", "  4.90", "  4.95", "  5.00",
			"  5.05", "  5.10", "  5.15", "  5.20", "  5.25", "  5.50", "  6.00", "  6.25", "  6.50", "  6.75", "  7.00", "  7.25", "  7.50", "  7.75", "  8.00" };
	final String[]									cellVoltageStartValues	= new String[350];
	final String[]									analogAlarmStartValues	= new String[3101];
	final String[]									analogAlarmUnits				= new String[] { "[°C]", "[mV]", "[km/h]", "[km/h]", "[°C]" };																																								//0=Temperature, 1=MilliVolt, 2=SpeedSensor 250, 3=SpeedSensor 450, 4=Temperature PT1000
	final UniLog2SetupReaderWriter	configuration;
	final String[]									jetiValueNames					= Messages.getString(MessageIds.GDE_MSGT2589).split(GDE.STRING_COMMA);
	final String[]									energyValues						= new String[291];
	final String[]									rpmMinValues						= new String[2000];
	final String[]									rpmMaxValues						= new String[2000];

	/**
	 * constructor configuration panel 2
	 * @param parent
	* @param dialog
	* @param configuration
	*/
	public UniLog2SetupConfiguration2(Composite parent, int style, UniLog2Dialog useDialog, UniLog2SetupReaderWriter useConfiguration) {
		super(parent, style);
		SWTResourceManager.registerResourceUser(this);
		this.dialog = useDialog;
		this.configuration = useConfiguration;
		this.application = DataExplorer.getInstance();
		for (int i = 0, value = 100; i < 350; i++) {
			this.cellVoltageStartValues[i] = String.format("    %.2f", ((value += 1) / 100.0)); //$NON-NLS-1$
		}
		for (int i = 0, value = -100; i < 3101; i++) {
			this.analogAlarmStartValues[i] = String.format("  %4d", (value++)); //$NON-NLS-1$
		}
		for (int i = 0, value = 900; i < this.energyValues.length; i++) {
			this.energyValues[i] = String.format(" %5d", (value += 100)); //$NON-NLS-1$
		}
		for (int i = 0, value = 0; i < 2000; i++) {
			this.rpmMinValues[i] = String.format(" %5d", (value += 100)); //$NON-NLS-1$
		}
		for (int i = 0, value = 0; i < 2000; i++) {
			this.rpmMaxValues[i] = String.format(" %5d", (value += 100)); //$NON-NLS-1$
		}
		initGUI();
	}

	public void updateValues() {
		this.currentButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_CURRENT) > 0);
		this.currentCombo.setText(String.format(Locale.ENGLISH, " %5d", this.configuration.currentAlarm)); //$NON-NLS-1$
		this.voltageStartButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_START) > 0);
		this.voltageStartCombo.setText(String.format(Locale.ENGLISH, " %5.1f", this.configuration.voltageStartAlarm / 10.0)); //$NON-NLS-1$
		this.voltageButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
		this.voltageCombo.setText(String.format(Locale.ENGLISH, " %5.1f", this.configuration.voltageAlarm / 10.0)); //$NON-NLS-1$
		this.capacityButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_CAPACITY) > 0);
		this.capacityCombo.setText(String.format(Locale.ENGLISH, " %5d", this.configuration.capacityAlarm)); //$NON-NLS-1$
		this.energyButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_ENERGY) > 0);
		this.energyCombo.setText(String.format(Locale.ENGLISH, " %5d", this.configuration.energyAlarm)); //$NON-NLS-1$
		this.heightButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_HEIGHT) > 0);
		this.heightCombo.setText(String.format(Locale.ENGLISH, " %5d", this.configuration.heightAlarm)); //$NON-NLS-1$
		this.voltageRxButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_RX) > 0);
		this.voltageRxCombo.setText(String.format(Locale.ENGLISH, "  %5.2f", (this.configuration.voltageRxAlarm / 100.0))); //$NON-NLS-1$
		this.cellVoltageButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_CELL) > 0);
		this.cellVoltageCombo.setText(String.format(Locale.ENGLISH, "  %5.2f", (this.configuration.cellVoltageAlarm / 100.0))); //$NON-NLS-1$
		this.rpmMinButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_RPM_MIN) > 0);
		this.rpmMinCombo.setText(String.format(Locale.ENGLISH, "  %5d", (this.configuration.rpmMinAlarm * 100))); //$NON-NLS-1$
		this.rpmMaxButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_RPM_MAX) > 0);
		this.rpmMaxCombo.setText(String.format(Locale.ENGLISH, "  %5d", (this.configuration.rpmMaxAlarm * 100))); //$NON-NLS-1$

		//0=Temperature, 1=MilliVolt, 2=SpeedSensor 250, 3=SpeedSensor 450, 4=Temperature PT1000
		this.analogAlarm1Button.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_1) > 0);
		this.analogAlarm1DirectionButton.setSelection(this.configuration.analogAlarm1Direct == 1);
		this.analogAlarm1Combo.select(this.configuration.analogAlarm1 + 100);
		this.analogAlarm1Label.setText(this.analogAlarmUnits[this.configuration.modusA1]);
		this.analogAlarm2Button.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_2) > 0);
		this.analogAlarm2DirectionButton.setSelection(this.configuration.analogAlarm2Direct == 1);
		this.analogAlarm2Combo.select(this.configuration.analogAlarm2 + 100);
		this.analogAlarm2Label.setText(this.analogAlarmUnits[this.configuration.modusA2]);
		this.analogAlarm3Button.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_3) > 0);
		this.analogAlarm3DirectionButton.setSelection(this.configuration.analogAlarm3Direct == 1);
		this.analogAlarm3Combo.select(this.configuration.analogAlarm3 + 100);
		this.analogAlarm3Label.setText(this.analogAlarmUnits[this.configuration.modusA3]);

		this.addressVoltageSlider.setSelection(this.configuration.mLinkAddressVoltage);
		this.addressVoltageText.setText(this.sliderValues[this.configuration.mLinkAddressVoltage]);
		this.addressCurrentSlider.setSelection(this.configuration.mLinkAddressCurrent);
		this.addressCurrentText.setText(this.sliderValues[this.configuration.mLinkAddressCurrent]);
		this.addressRevolutionSlider.setSelection(this.configuration.mLinkAddressRevolution);
		this.addressRevolutionText.setText(this.sliderValues[this.configuration.mLinkAddressRevolution]);
		this.addressCapacitySlider.setSelection(this.configuration.mLinkAddressCapacity);
		this.addressCapacityText.setText(this.sliderValues[this.configuration.mLinkAddressCapacity]);
		this.addressVarioSlider.setSelection(this.configuration.mLinkAddressVario);
		this.addressVarioText.setText(this.sliderValues[this.configuration.mLinkAddressVario]);
		this.addressHeightText.setText(this.sliderValues[this.configuration.mLinkAddressHeight]);
		this.addressHeightSlider.setSelection(this.configuration.mLinkAddressHeight);
		this.addressHeightGainText.setText(this.sliderValues[this.configuration.mLinkAddressHeightGain]);
		this.addressHeightGainSlider.setSelection(this.configuration.mLinkAddressHeightGain);
		this.addressEnergyText.setText(this.sliderValues[this.configuration.mLinkAddressEnergy]);
		this.addressEnergySlider.setSelection(this.configuration.mLinkAddressEnergy);
		this.addressRemainCapText.setText(this.sliderValues[this.configuration.mLinkAddressRemainCap]);
		this.addressRemainCapSlider.setSelection(this.configuration.mLinkAddressRemainCap);
		this.cellMinimumSlider.setSelection(this.configuration.mLinkAddressCellMinimum);
		this.cellMinimumText.setText(this.sliderValues[this.configuration.mLinkAddressCellMinimum]);

		this.a1Combo.select(this.configuration.mLinkAddressA1);
		this.a2Combo.select(this.configuration.mLinkAddressA2);
		this.a3Combo.select(this.configuration.mLinkAddressA3);

		this.c1Combo.select(this.configuration.mLinkAddressCell1);
		this.c2Combo.select(this.configuration.mLinkAddressCell2);
		this.c3Combo.select(this.configuration.mLinkAddressCell3);
		this.c4Combo.select(this.configuration.mLinkAddressCell4);
		this.c5Combo.select(this.configuration.mLinkAddressCell5);
		this.c6Combo.select(this.configuration.mLinkAddressCell6);

		this.measurement1.setSelection((this.configuration.jetiValueVisibility & 0x0002) == 0);
		this.measurement2.setSelection((this.configuration.jetiValueVisibility & 0x0004) == 0);
		this.measurement3.setSelection((this.configuration.jetiValueVisibility & 0x0008) == 0);
		this.measurement4.setSelection((this.configuration.jetiValueVisibility & 0x0010) == 0);
		this.measurement5.setSelection((this.configuration.jetiValueVisibility & 0x0020) == 0);
		this.measurement6.setSelection((this.configuration.jetiValueVisibility & 0x0040) == 0);
		this.measurement7.setSelection((this.configuration.jetiValueVisibility & 0x0080) == 0);
		this.measurement8.setSelection((this.configuration.jetiValueVisibility & 0x0100) == 0);
		this.measurement9.setSelection((this.configuration.jetiValueVisibility & 0x0200) == 0);
		this.measurement10.setSelection((this.configuration.jetiValueVisibility & 0x0400) == 0);
		this.measurement11.setSelection((this.configuration.jetiValueVisibility & 0x0800) == 0);
		this.measurement12.setSelection((this.configuration.jetiValueVisibility & 0x1000) == 0);
		this.measurement13.setSelection((this.configuration.jetiValueVisibility & 0x2000) == 0);
		this.measurement14.setSelection((this.configuration.jetiValueVisibility & 0x4000) == 0);
		this.measurement15.setSelection((this.configuration.jetiValueVisibility & 0x8000) == 0);
		this.measurement16.setSelection((this.configuration.jetiValueVisibility & 0x10000) == 0);
		this.measurement17.setSelection((this.configuration.jetiValueVisibility & 0x20000) == 0);
		this.measurement18.setSelection((this.configuration.jetiValueVisibility & 0x40000) == 0);
		this.measurement19.setSelection((this.configuration.jetiValueVisibility & 0x80000) == 0);
		this.measurement20.setSelection((this.configuration.jetiValueVisibility & 0x100000) == 0);
		this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {this.configuration.getJetiMeasurementCount()}));

		this.spektrumAddressCombo.select(this.configuration.spektrumNumber);
		this.spektrumEscSensor.setSelection((this.configuration.spektrumSensors & 0x01) == 0);
		this.spektrumCurrentSensor.setSelection((this.configuration.spektrumSensors & 0x02) == 0);
		this.spektrumVarioSensor.setSelection((this.configuration.spektrumSensors & 0x04) == 0);
		this.spektrumLiPoMonitor.setSelection((this.configuration.spektrumSensors & 0x08) == 0);
	}

	public void updateAnalogAlarmUnits() {
		this.analogAlarm1Label.setText(this.analogAlarmUnits[this.configuration.modusA1]);
		this.analogAlarm2Label.setText(this.analogAlarmUnits[this.configuration.modusA2]);
		this.analogAlarm3Label.setText(this.analogAlarmUnits[this.configuration.modusA3]);
	}

	void initGUI() {
		try {
			this.setLayout(new FormLayout());
			this.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "GPSLoggerSetupConfiguration2.helpRequested, event=" + evt); //$NON-NLS-1$
					UniLog2SetupConfiguration2.this.application.openHelpDialog(Messages.getString(MessageIds.GDE_MSGT2510), "HelpInfo.html#configuration"); //$NON-NLS-1$
				}
			});
			{
				this.unilogTelemtryAlarmsGroup = new Group(this, SWT.NONE);
				RowLayout unilogTelemtryAlarmsGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.unilogTelemtryAlarmsGroup.setLayout(unilogTelemtryAlarmsGroupLayout);
				FormData unilogTelemtryAlarmsGroupLData = new FormData();
				unilogTelemtryAlarmsGroupLData.top = new FormAttachment(0, 1000, 5);
				unilogTelemtryAlarmsGroupLData.left = new FormAttachment(0, 1000, 12);
				unilogTelemtryAlarmsGroupLData.width = 290;
				unilogTelemtryAlarmsGroupLData.height = 315;
				this.unilogTelemtryAlarmsGroup.setLayoutData(unilogTelemtryAlarmsGroupLData);
				this.unilogTelemtryAlarmsGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.unilogTelemtryAlarmsGroup.setText(Messages.getString(MessageIds.GDE_MSGT2565));
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.currentButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData currentButtonLData = new RowData();
					currentButtonLData.width = this.buttonWidth;
					currentButtonLData.height = 19;
					this.currentButton.setLayoutData(currentButtonLData);
					this.currentButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentButton.setText(Messages.getString(MessageIds.GDE_MSGT2566));
					this.currentButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_CURRENT) > 0);
					this.currentButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "currentButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.currentButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_CURRENT);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_CURRENT);
							}
							UniLog2SetupConfiguration2.this.currentButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_CURRENT) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData currentCComboLData = new RowData();
					currentCComboLData.width = this.comboWidth;
					currentCComboLData.height = 17;
					this.currentCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.currentCombo.setLayoutData(currentCComboLData);
					this.currentCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentCombo.setItems(this.currentValues);
					this.currentCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.currentAlarm)); //$NON-NLS-1$
					this.currentCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "currentCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.currentAlarm = (short) Integer.parseInt(UniLog2SetupConfiguration2.this.currentCombo.getText().trim());
							UniLog2SetupConfiguration2.this.configuration.currentAlarm = UniLog2SetupConfiguration2.this.configuration.currentAlarm < 1 ? 1
									: UniLog2SetupConfiguration2.this.configuration.currentAlarm > 400 ? 400 : UniLog2SetupConfiguration2.this.configuration.currentAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.currentCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "currentCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.currentCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "currentCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration2.this.configuration.currentAlarm = (short) Integer.parseInt(UniLog2SetupConfiguration2.this.currentCombo.getText().trim());
								UniLog2SetupConfiguration2.this.configuration.currentAlarm = UniLog2SetupConfiguration2.this.configuration.currentAlarm < 1 ? 1
										: UniLog2SetupConfiguration2.this.configuration.currentAlarm > 400 ? 400 : UniLog2SetupConfiguration2.this.configuration.currentAlarm;
								UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore -
							}
						}
					});
				}
				{
					this.currentLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER);
					RowData currentLabelLData = new RowData();
					currentLabelLData.width = 50;
					currentLabelLData.height = 19;
					this.currentLabel.setLayoutData(currentLabelLData);
					this.currentLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentLabel.setText(Messages.getString(MessageIds.GDE_MSGT2551));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.voltageStartButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageStartButtonLData = new RowData();
					voltageStartButtonLData.width = this.buttonWidth;
					voltageStartButtonLData.height = 19;
					this.voltageStartButton.setLayoutData(voltageStartButtonLData);
					this.voltageStartButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageStartButton.setText(Messages.getString(MessageIds.GDE_MSGT2552));
					this.voltageStartButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_START) > 0);
					this.voltageStartButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "voltageStartButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.voltageStartButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_START);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_START);
							}
							UniLog2SetupConfiguration2.this.voltageStartButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_START) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData voltageStartCComboLData = new RowData();
					voltageStartCComboLData.width = this.comboWidth;
					voltageStartCComboLData.height = 17;
					this.voltageStartCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.voltageStartCombo.setLayoutData(voltageStartCComboLData);
					this.voltageStartCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageStartCombo.setItems(this.voltageStartValues);
					this.voltageStartCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageStartAlarm / 10.0)); //$NON-NLS-1$
					this.voltageStartCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "voltageStartCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.voltageStartCombo.getText().trim()
									.replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 10);
							UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm = UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.voltageStartCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "voltageStartCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.voltageStartCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "voltageStartCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.voltageStartCombo.getText().trim()
										.replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 10);
								UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm = UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm < 10 ? 10
										: UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm;
								UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore -
							}
						}
					});
				}
				{
					this.voltageStartLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData voltageStartLabelLData = new RowData();
					voltageStartLabelLData.width = 50;
					voltageStartLabelLData.height = 19;
					this.voltageStartLabel.setLayoutData(voltageStartLabelLData);
					this.voltageStartLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageStartLabel.setText(Messages.getString(MessageIds.GDE_MSGT2553));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.voltageButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageButtonLData = new RowData();
					voltageButtonLData.width = this.buttonWidth;
					voltageButtonLData.height = 19;
					this.voltageButton.setLayoutData(voltageButtonLData);
					this.voltageButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageButton.setText(Messages.getString(MessageIds.GDE_MSGT2554));
					this.voltageButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
					this.voltageButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "voltageButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.voltageButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							UniLog2SetupConfiguration2.this.voltageButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData voltageCComboLData = new RowData();
					voltageCComboLData.width = this.comboWidth;
					voltageCComboLData.height = 17;
					this.voltageCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.voltageCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageCombo.setLayoutData(voltageCComboLData);
					this.voltageCombo.setItems(this.voltageStartValues);
					this.voltageCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageAlarm / 10.0)); //$NON-NLS-1$
					this.voltageCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "voltageCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.voltageAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.voltageCombo.getText().trim()
									.replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 10);
							UniLog2SetupConfiguration2.this.configuration.voltageAlarm = UniLog2SetupConfiguration2.this.configuration.voltageAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.voltageAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.voltageAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.voltageCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "voltageCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.voltageCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "voltageCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration2.this.configuration.voltageAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.voltageCombo.getText().trim()
										.replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 10);
								UniLog2SetupConfiguration2.this.configuration.voltageAlarm = UniLog2SetupConfiguration2.this.configuration.voltageAlarm < 10 ? 10
										: UniLog2SetupConfiguration2.this.configuration.voltageAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.voltageAlarm;
								UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								//ignore -
							}
						}
					});
				}
				{
					RowData voltageLabelLData = new RowData();
					voltageLabelLData.width = 50;
					voltageLabelLData.height = 19;
					this.voltageLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					this.voltageLabel.setLayoutData(voltageLabelLData);
					this.voltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageLabel.setText(Messages.getString(MessageIds.GDE_MSGT2555));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.capacityButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageRxULButtonLData = new RowData();
					voltageRxULButtonLData.width = this.buttonWidth;
					voltageRxULButtonLData.height = 19;
					this.capacityButton.setLayoutData(voltageRxULButtonLData);
					this.capacityButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityButton.setText(Messages.getString(MessageIds.GDE_MSGT2556));
					this.capacityButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_CAPACITY) > 0);
					this.capacityButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "voltageRxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.capacityButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_CAPACITY);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_CAPACITY);
							}
							UniLog2SetupConfiguration2.this.capacityButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_CAPACITY) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.capacityCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.capacityCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData capacityComboLData = new RowData();
					capacityComboLData.width = this.comboWidth;
					capacityComboLData.height = 17;
					this.capacityCombo.setLayoutData(capacityComboLData);
					this.capacityCombo.setItems(this.capacityValues);
					this.capacityCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.capacityAlarm)); //$NON-NLS-1$
					this.capacityCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "capacityCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.capacityAlarm = (short) Integer.parseInt(UniLog2SetupConfiguration2.this.capacityCombo.getText().trim());
							UniLog2SetupConfiguration2.this.configuration.capacityAlarm = UniLog2SetupConfiguration2.this.configuration.capacityAlarm < 100 ? 100
									: UniLog2SetupConfiguration2.this.configuration.capacityAlarm > 30000 ? 30000 : UniLog2SetupConfiguration2.this.configuration.capacityAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.capacityCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "capacityCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.capacityCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "capacityCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration2.this.configuration.capacityAlarm = (short) Integer.parseInt(UniLog2SetupConfiguration2.this.capacityCombo.getText().trim());
								UniLog2SetupConfiguration2.this.configuration.capacityAlarm = UniLog2SetupConfiguration2.this.configuration.capacityAlarm < 100 ? 100
										: UniLog2SetupConfiguration2.this.configuration.capacityAlarm > 30000 ? 30000 : UniLog2SetupConfiguration2.this.configuration.capacityAlarm;
								UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								//ignore -
							}
						}
					});
				}
				{
					this.capacityLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData voltageRxULLabelLData = new RowData();
					voltageRxULLabelLData.width = 50;
					voltageRxULLabelLData.height = 19;
					this.capacityLabel.setLayoutData(voltageRxULLabelLData);
					this.capacityLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityLabel.setText(Messages.getString(MessageIds.GDE_MSGT2557));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.energyButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData energyButtonLData = new RowData();
					energyButtonLData.width = this.buttonWidth;
					energyButtonLData.height = 19;
					this.energyButton.setLayoutData(energyButtonLData);
					this.energyButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.energyButton.setText(Messages.getString(MessageIds.GDE_MSGT2547).split(GDE.STRING_BLANK)[0]);
					this.energyButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
					this.energyButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "energyButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.energyButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_ENERGY);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_ENERGY);
							}
							UniLog2SetupConfiguration2.this.energyButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_ENERGY) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData energyCComboLData = new RowData();
					energyCComboLData.width = this.comboWidth;
					energyCComboLData.height = 17;
					this.energyCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.energyCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.energyCombo.setLayoutData(energyCComboLData);
					this.energyCombo.setItems(this.energyValues);
					this.energyCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.energyAlarm / 10.0)); //$NON-NLS-1$
					this.energyCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "energyCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.energyAlarm = (short) (Integer.parseInt(UniLog2SetupConfiguration2.this.energyCombo.getText().trim()));
							UniLog2SetupConfiguration2.this.configuration.energyAlarm = UniLog2SetupConfiguration2.this.configuration.energyAlarm < 100
									? 100
									: UniLog2SetupConfiguration2.this.configuration.energyAlarm > 30000 
										? 30000 
										: UniLog2SetupConfiguration2.this.configuration.energyAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.energyCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "energyCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.energyCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "energyCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration2.this.configuration.energyAlarm = (short) (Integer.parseInt(UniLog2SetupConfiguration2.this.energyCombo.getText().trim()));
								UniLog2SetupConfiguration2.this.configuration.energyAlarm = UniLog2SetupConfiguration2.this.configuration.energyAlarm < 100
										? 100
										: UniLog2SetupConfiguration2.this.configuration.energyAlarm > 30000 
											? 30000 
											: UniLog2SetupConfiguration2.this.configuration.energyAlarm;
								UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore -
							}
						}
					});
				}
				{
					RowData energyLabelLData = new RowData();
					energyLabelLData.width = 50;
					energyLabelLData.height = 19;
					this.energyLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					this.energyLabel.setLayoutData(energyLabelLData);
					this.energyLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.energyLabel.setText(Messages.getString(MessageIds.GDE_MSGT2547).split(GDE.STRING_BLANK)[2]);
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.heightButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData heightButtonLData = new RowData();
					heightButtonLData.width = this.buttonWidth;
					heightButtonLData.height = 19;
					this.heightButton.setLayoutData(heightButtonLData);
					this.heightButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightButton.setText(Messages.getString(MessageIds.GDE_MSGT2567));
					this.heightButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
					this.heightButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "heightButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.heightButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							UniLog2SetupConfiguration2.this.heightButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData heightCComboLData = new RowData();
					heightCComboLData.width = this.comboWidth;
					heightCComboLData.height = 17;
					this.heightCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.heightCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightCombo.setLayoutData(heightCComboLData);
					this.heightCombo.setItems(this.heightThresholds);
					this.heightCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "heightCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.heightAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.heightCombo.getText().trim()
									.replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)));
							UniLog2SetupConfiguration2.this.configuration.heightAlarm = UniLog2SetupConfiguration2.this.configuration.heightAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.heightAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.heightAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.heightCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "heightCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.heightCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "heightCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration2.this.configuration.heightAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.heightCombo.getText().trim()
										.replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 10);
								UniLog2SetupConfiguration2.this.configuration.heightAlarm = UniLog2SetupConfiguration2.this.configuration.heightAlarm < 10 ? 10
										: UniLog2SetupConfiguration2.this.configuration.heightAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.heightAlarm;
								UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore -
							}
						}
					});
				}
				{
					this.heightLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData heightLabelLData = new RowData();
					heightLabelLData.width = 50;
					heightLabelLData.height = 19;
					this.heightLabel.setLayoutData(heightLabelLData);
					this.heightLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightLabel.setText(Messages.getString(MessageIds.GDE_MSGT2555));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.voltageRxButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageRxButtonLData = new RowData();
					voltageRxButtonLData.width = this.buttonWidth;
					voltageRxButtonLData.height = 19;
					this.voltageRxButton.setLayoutData(voltageRxButtonLData);
					this.voltageRxButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageRxButton.setText(Messages.getString(MessageIds.GDE_MSGT2568));
					this.voltageRxButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
					this.voltageRxButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "voltageRxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.voltageRxButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							UniLog2SetupConfiguration2.this.voltageRxButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData voltageRxCComboLData = new RowData();
					voltageRxCComboLData.width = this.comboWidth;
					voltageRxCComboLData.height = 17;
					this.voltageRxCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.voltageRxCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageRxCombo.setLayoutData(voltageRxCComboLData);
					this.voltageRxCombo.setItems(this.voltageRxStartValues);
					this.voltageRxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "voltageRxCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.voltageRxCombo.getText().trim()
									.replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 100);
							UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm = UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.voltageRxCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "voltageRxCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.voltageRxCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "voltageRxCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.voltageRxCombo.getText().trim()
										.replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 100);
								UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm = UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm < 10 ? 10
										: UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm;
								UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore -
							}
						}
					});
				}
				{
					RowData voltageRxLabelLData = new RowData();
					voltageRxLabelLData.width = 50;
					voltageRxLabelLData.height = 19;
					this.voltageRxLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					this.voltageRxLabel.setLayoutData(voltageRxLabelLData);
					this.voltageRxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageRxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2555));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.cellVoltageButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData cellVoltageButtonLData = new RowData();
					cellVoltageButtonLData.width = this.buttonWidth;
					cellVoltageButtonLData.height = 19;
					this.cellVoltageButton.setLayoutData(cellVoltageButtonLData);
					this.cellVoltageButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.cellVoltageButton.setText(Messages.getString(MessageIds.GDE_MSGT2569));
					this.cellVoltageButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
					this.cellVoltageButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "cellVoltageButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.cellVoltageButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							UniLog2SetupConfiguration2.this.cellVoltageButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData cellVoltageCComboLData = new RowData();
					cellVoltageCComboLData.width = this.comboWidth;
					cellVoltageCComboLData.height = 17;
					this.cellVoltageCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.cellVoltageCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.cellVoltageCombo.setLayoutData(cellVoltageCComboLData);
					this.cellVoltageCombo.setItems(this.cellVoltageStartValues);
					this.cellVoltageCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.cellVoltageAlarm / 10.0)); //$NON-NLS-1$
					this.cellVoltageCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "cellVoltageCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.cellVoltageCombo.getText().trim()
									.replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 100);
							UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm = UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.cellVoltageCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "cellVoltageCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.cellVoltageCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "cellVoltageCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.cellVoltageCombo.getText().trim()
										.replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 100);
								UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm = UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm < 10 ? 10
										: UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm;
								UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore -
							}
						}
					});
				}
				{
					RowData cellVoltageLabelLData = new RowData();
					cellVoltageLabelLData.width = 50;
					cellVoltageLabelLData.height = 19;
					this.cellVoltageLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					this.cellVoltageLabel.setLayoutData(cellVoltageLabelLData);
					this.cellVoltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.cellVoltageLabel.setText(Messages.getString(MessageIds.GDE_MSGT2555));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.analogAlarm1Button = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1ButtonLData = new RowData();
					analogAlarm1ButtonLData.width = this.buttonWidth / 4 * 3 - 3;
					analogAlarm1ButtonLData.height = 19;
					this.analogAlarm1Button.setLayoutData(analogAlarm1ButtonLData);
					this.analogAlarm1Button.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.analogAlarm1Button.setText("  A1"); //$NON-NLS-1$
					this.analogAlarm1Button.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_1) > 0);
					this.analogAlarm1Button.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm1Button.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.analogAlarm1Button.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_1);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_1);
							}
							UniLog2SetupConfiguration2.this.analogAlarm1Button.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_1) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.analogAlarm1DirectionButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = this.buttonWidth / 4;
					analogAlarm1DirectionButtonLData.height = 19;
					this.analogAlarm1DirectionButton.setLayoutData(analogAlarm1DirectionButtonLData);
					this.analogAlarm1DirectionButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.analogAlarm1DirectionButton.setText("<"); //$NON-NLS-1$
					this.analogAlarm1DirectionButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_1) > 0);
					this.analogAlarm1DirectionButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm1DirectionButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.analogAlarm1Direct = (short) (UniLog2SetupConfiguration2.this.analogAlarm1DirectionButton.getSelection() ? 1 : 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.analogAlarm1Combo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.analogAlarm1Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData analogAlarm1CComboLData = new RowData();
					analogAlarm1CComboLData.width = this.comboWidth;
					analogAlarm1CComboLData.height = 17;
					this.analogAlarm1Combo.setLayoutData(analogAlarm1CComboLData);
					this.analogAlarm1Combo.setItems(this.analogAlarmStartValues);
					this.analogAlarm1Combo.setText(String.format(Locale.ENGLISH, "  %4df", this.configuration.analogAlarm1)); //$NON-NLS-1$
					this.analogAlarm1Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.analogAlarm1 = (short) (Integer.parseInt(UniLog2SetupConfiguration2.this.analogAlarm1Combo.getText().trim()));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.analogAlarm1Combo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm1Combo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.analogAlarm1Combo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm1Combo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration2.this.configuration.analogAlarm1 = (short) (Integer.parseInt(UniLog2SetupConfiguration2.this.analogAlarm1Combo.getText().trim()));
								UniLog2SetupConfiguration2.this.configuration.analogAlarm1 = UniLog2SetupConfiguration2.this.configuration.analogAlarm1 > 3000 ? 3000
										: UniLog2SetupConfiguration2.this.configuration.analogAlarm1 < -100 ? -100 : UniLog2SetupConfiguration2.this.configuration.analogAlarm1;
								UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore for -
							}
						}
					});
				}
				{
					this.analogAlarm1Label = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData analogAlarm1LabelLData = new RowData();
					analogAlarm1LabelLData.width = 50;
					analogAlarm1LabelLData.height = 19;
					this.analogAlarm1Label.setLayoutData(analogAlarm1LabelLData);
					this.analogAlarm1Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.analogAlarm2Button = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm2ButtonLData = new RowData();
					analogAlarm2ButtonLData.width = this.buttonWidth / 4 * 3 - 3;
					analogAlarm2ButtonLData.height = 19;
					this.analogAlarm2Button.setLayoutData(analogAlarm2ButtonLData);
					this.analogAlarm2Button.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.analogAlarm2Button.setText("  A2"); //$NON-NLS-1$
					this.analogAlarm2Button.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm2Button.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.analogAlarm2Button.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_2);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_2);
							}
							UniLog2SetupConfiguration2.this.analogAlarm2Button.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_2) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.analogAlarm2DirectionButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm2DirectionButtonLData = new RowData();
					analogAlarm2DirectionButtonLData.width = this.buttonWidth / 4;
					analogAlarm2DirectionButtonLData.height = 19;
					this.analogAlarm2DirectionButton.setLayoutData(analogAlarm2DirectionButtonLData);
					this.analogAlarm2DirectionButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.analogAlarm2DirectionButton.setText("<"); //$NON-NLS-1$
					this.analogAlarm2DirectionButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm2DirectionButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.analogAlarm2Direct = (short) (UniLog2SetupConfiguration2.this.analogAlarm2DirectionButton.getSelection() ? 1 : 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.analogAlarm2Combo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.analogAlarm2Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData analogAlarm2CComboLData = new RowData();
					analogAlarm2CComboLData.width = this.comboWidth;
					analogAlarm2CComboLData.height = 17;
					this.analogAlarm2Combo.setLayoutData(analogAlarm2CComboLData);
					this.analogAlarm2Combo.setItems(this.analogAlarmStartValues);
					this.analogAlarm2Combo.setText(String.format(Locale.ENGLISH, "  %4df", this.configuration.analogAlarm2)); //$NON-NLS-1$
					this.analogAlarm2Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm2Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.analogAlarm2 = (short) (Integer.parseInt(UniLog2SetupConfiguration2.this.analogAlarm2Combo.getText().trim()));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.analogAlarm2Combo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm2Combo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.analogAlarm2Combo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm2Combo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration2.this.configuration.analogAlarm2 = (short) (Integer.parseInt(UniLog2SetupConfiguration2.this.analogAlarm2Combo.getText().trim()));
								UniLog2SetupConfiguration2.this.configuration.analogAlarm2 = UniLog2SetupConfiguration2.this.configuration.analogAlarm2 > 3000 ? 3000
										: UniLog2SetupConfiguration2.this.configuration.analogAlarm2 < -100 ? -100 : UniLog2SetupConfiguration2.this.configuration.analogAlarm2;
								UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore for -
							}
						}
					});
				}
				{
					this.analogAlarm2Label = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData analogAlarm2LabelLData = new RowData();
					analogAlarm2LabelLData.width = 50;
					analogAlarm2LabelLData.height = 19;
					this.analogAlarm2Label.setLayoutData(analogAlarm2LabelLData);
					this.analogAlarm2Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.analogAlarm3Button = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm3ButtonLData = new RowData();
					analogAlarm3ButtonLData.width = this.buttonWidth / 4 * 3 - 3;
					analogAlarm3ButtonLData.height = 19;
					this.analogAlarm3Button.setLayoutData(analogAlarm3ButtonLData);
					this.analogAlarm3Button.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.analogAlarm3Button.setText("  A3"); //$NON-NLS-1$
					this.analogAlarm3Button.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm3Button.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.analogAlarm3Button.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_3);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_3);
							}
							UniLog2SetupConfiguration2.this.analogAlarm3Button.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_ANALOG_3) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.analogAlarm3DirectionButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm3DirectionButtonLData = new RowData();
					analogAlarm3DirectionButtonLData.width = this.buttonWidth / 4;
					analogAlarm3DirectionButtonLData.height = 19;
					this.analogAlarm3DirectionButton.setLayoutData(analogAlarm3DirectionButtonLData);
					this.analogAlarm3DirectionButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.analogAlarm3DirectionButton.setText("<"); //$NON-NLS-1$
					this.analogAlarm3DirectionButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm3DirectionButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.analogAlarm3Direct = (short) (UniLog2SetupConfiguration2.this.analogAlarm3DirectionButton.getSelection() ? 1 : 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.analogAlarm3Combo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.analogAlarm3Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData analogAlarm3CComboLData = new RowData();
					analogAlarm3CComboLData.width = this.comboWidth;
					analogAlarm3CComboLData.height = 17;
					this.analogAlarm3Combo.setLayoutData(analogAlarm3CComboLData);
					this.analogAlarm3Combo.setItems(this.analogAlarmStartValues);
					this.analogAlarm3Combo.setText(String.format(Locale.ENGLISH, "  %4df", this.configuration.analogAlarm3)); //$NON-NLS-1$
					this.analogAlarm3Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm3Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.analogAlarm3 = (short) (Integer.parseInt(UniLog2SetupConfiguration2.this.analogAlarm3Combo.getText().trim()));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.analogAlarm3Combo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm3Combo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.analogAlarm3Combo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "analogAlarm3Combo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration2.this.configuration.analogAlarm3 = (short) (Integer.parseInt(UniLog2SetupConfiguration2.this.analogAlarm3Combo.getText().trim()));
								UniLog2SetupConfiguration2.this.configuration.analogAlarm3 = UniLog2SetupConfiguration2.this.configuration.analogAlarm3 > 3000 ? 3000
										: UniLog2SetupConfiguration2.this.configuration.analogAlarm3 < -100 ? -100 : UniLog2SetupConfiguration2.this.configuration.analogAlarm3;
							}
							catch (NumberFormatException e) {
								// ignore for -
							}
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.analogAlarm3Label = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData analogAlarm3LabelLData = new RowData();
					analogAlarm3LabelLData.width = 50;
					analogAlarm3LabelLData.height = 19;
					this.analogAlarm3Label.setLayoutData(analogAlarm3LabelLData);
					this.analogAlarm3Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.rpmMinButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData rpmMinButtonLData = new RowData();
					rpmMinButtonLData.width = this.buttonWidth;
					rpmMinButtonLData.height = 19;
					this.rpmMinButton.setLayoutData(rpmMinButtonLData);
					this.rpmMinButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.rpmMinButton.setText(Messages.getString(MessageIds.GDE_MSGT2561) + " min");
					this.rpmMinButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
					this.rpmMinButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "rpmMinButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.rpmMinButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_RPM_MIN);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_RPM_MIN);
							}
							UniLog2SetupConfiguration2.this.rpmMinButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_RPM_MIN) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData rpmMinCComboLData = new RowData();
					rpmMinCComboLData.width = this.comboWidth;
					rpmMinCComboLData.height = 17;
					this.rpmMinCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.rpmMinCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.rpmMinCombo.setLayoutData(rpmMinCComboLData);
					this.rpmMinCombo.setItems(this.rpmMinValues);
					this.rpmMinCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.rpmMinAlarm * 100)); //$NON-NLS-1$
					this.rpmMinCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "rpmMinCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.rpmMinAlarm = (short) (Integer.parseInt(UniLog2SetupConfiguration2.this.rpmMinCombo.getText().trim()) / 100);
							UniLog2SetupConfiguration2.this.configuration.rpmMinAlarm = UniLog2SetupConfiguration2.this.configuration.rpmMinAlarm < 1 ? 1
									: UniLog2SetupConfiguration2.this.configuration.rpmMinAlarm > 2000 ? 2000 : UniLog2SetupConfiguration2.this.configuration.rpmMinAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.rpmMinCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "rpmMinCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.rpmMinCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "rpmMinCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration2.this.configuration.rpmMinAlarm = (short) (Integer.parseInt(UniLog2SetupConfiguration2.this.rpmMinCombo.getText().trim()) / 100);
								UniLog2SetupConfiguration2.this.configuration.rpmMinAlarm = UniLog2SetupConfiguration2.this.configuration.rpmMinAlarm < 1 ? 1
										: UniLog2SetupConfiguration2.this.configuration.rpmMinAlarm > 2000 ? 2000 : UniLog2SetupConfiguration2.this.configuration.rpmMinAlarm;
								UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore -
							}
						}
					});
				}
				{
					RowData rpmMinLabelLData = new RowData();
					rpmMinLabelLData.width = 50;
					rpmMinLabelLData.height = 19;
					this.rpmMinLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					this.rpmMinLabel.setLayoutData(rpmMinLabelLData);
					this.rpmMinLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.rpmMinLabel.setText(Messages.getString(MessageIds.GDE_MSGT2507));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.rpmMaxButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData rpmMaxButtonLData = new RowData();
					rpmMaxButtonLData.width = this.buttonWidth;
					rpmMaxButtonLData.height = 19;
					this.rpmMaxButton.setLayoutData(rpmMaxButtonLData);
					this.rpmMaxButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.rpmMaxButton.setText(Messages.getString(MessageIds.GDE_MSGT2561) + " max");
					this.rpmMaxButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
					this.rpmMaxButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "rpmMaxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.rpmMaxButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_RPM_MAX);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_RPM_MAX);
							}
							UniLog2SetupConfiguration2.this.rpmMaxButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_RPM_MAX) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData rpmMaxCComboLData = new RowData();
					rpmMaxCComboLData.width = this.comboWidth;
					rpmMaxCComboLData.height = 17;
					this.rpmMaxCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.rpmMaxCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.rpmMaxCombo.setLayoutData(rpmMaxCComboLData);
					this.rpmMaxCombo.setItems(this.rpmMaxValues);
					this.rpmMaxCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.rpmMaxAlarm / 10.0)); //$NON-NLS-1$
					this.rpmMaxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "rpmMaxCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.rpmMaxAlarm = (short) (Integer.parseInt(UniLog2SetupConfiguration2.this.rpmMaxCombo.getText().trim()) / 100);
							UniLog2SetupConfiguration2.this.configuration.rpmMaxAlarm = UniLog2SetupConfiguration2.this.configuration.rpmMaxAlarm < 1 ? 1
									: UniLog2SetupConfiguration2.this.configuration.rpmMaxAlarm > 2000 ? 2000 : UniLog2SetupConfiguration2.this.configuration.rpmMaxAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.rpmMaxCombo.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "rpmMaxCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.rpmMaxCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "rpmMaxCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								UniLog2SetupConfiguration2.this.configuration.rpmMaxAlarm = (short) (Integer.parseInt(UniLog2SetupConfiguration2.this.rpmMaxCombo.getText().trim()) / 100);
								UniLog2SetupConfiguration2.this.configuration.rpmMaxAlarm = UniLog2SetupConfiguration2.this.configuration.rpmMaxAlarm < 1 ? 1
										: UniLog2SetupConfiguration2.this.configuration.rpmMaxAlarm > 2000 ? 2000 : UniLog2SetupConfiguration2.this.configuration.rpmMaxAlarm;
								UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore -
							}
						}
					});
				}
				{
					RowData rpmMaxLabelLData = new RowData();
					rpmMaxLabelLData.width = 50;
					rpmMaxLabelLData.height = 19;
					this.rpmMaxLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					this.rpmMaxLabel.setLayoutData(rpmMaxLabelLData);
					this.rpmMaxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.rpmMaxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2507));
				}
			}
			{
				this.mLinkAddressesGroup = new Group(this, SWT.NONE);
				UniLog2SetupConfiguration2.mLinkGroupStatic = this.mLinkAddressesGroup;
				RowLayout mLinkAddressesGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.mLinkAddressesGroup.setLayout(mLinkAddressesGroupLayout);
				FormData mLinkAddressesGroupLData = new FormData();
				mLinkAddressesGroupLData.top = new FormAttachment(0, 1000, 335);
				mLinkAddressesGroupLData.left = new FormAttachment(0, 1000, 12);
				mLinkAddressesGroupLData.width = 290;
				mLinkAddressesGroupLData.height = 325;
				this.mLinkAddressesGroup.setLayoutData(mLinkAddressesGroupLData);
				this.mLinkAddressesGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.mLinkAddressesGroup.setText(Messages.getString(MessageIds.GDE_MSGT2558));
				{
					this.addressVoltageLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressVoltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1LData = new RowData();
					addressLabel1LData.width = this.labelWidth;
					addressLabel1LData.height = 21;
					this.addressVoltageLabel.setLayoutData(addressLabel1LData);
					this.addressVoltageLabel.setText(Messages.getString(MessageIds.GDE_MSGT2559));
				}
				{
					this.addressVoltageText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressVoltageText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText1LData = new RowData();
					addressText1LData.width = this.textWidth;
					addressText1LData.height = 16;
					this.addressVoltageText.setLayoutData(addressText1LData);
					this.addressVoltageText.setText(this.sliderValues[this.configuration.mLinkAddressVario]);
					this.addressVoltageText.setEditable(false);
					this.addressVoltageText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressVoltageSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressVoltageSlider.setMinimum(this.sliderMinimum);
					this.addressVoltageSlider.setMaximum(this.sliderMaximum);
					this.addressVoltageSlider.setIncrement(this.sliderIncrement);
					RowData addressSlider1LData = new RowData();
					addressSlider1LData.width = this.sliderWidth;
					addressSlider1LData.height = 17;
					this.addressVoltageSlider.setLayoutData(addressSlider1LData);
					this.addressVoltageSlider.setSelection(this.configuration.mLinkAddressVario);
					this.addressVoltageSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "addressSlider1.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressVoltage = (byte) UniLog2SetupConfiguration2.this.addressVoltageSlider.getSelection();
							UniLog2SetupConfiguration2.this.addressVoltageText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressVoltage]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressCurrentLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressCurrentLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel2LData = new RowData();
					addressLabel2LData.width = this.labelWidth;
					addressLabel2LData.height = 21;
					this.addressCurrentLabel.setLayoutData(addressLabel2LData);
					this.addressCurrentLabel.setText(Messages.getString(MessageIds.GDE_MSGT2560));
				}
				{
					this.addressCurrentText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressCurrentText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText2LData = new RowData();
					addressText2LData.width = this.textWidth;
					addressText2LData.height = 16;
					this.addressCurrentText.setLayoutData(addressText2LData);
					this.addressCurrentText.setText(this.sliderValues[this.configuration.mLinkAddressA1]);
					this.addressCurrentText.setEditable(false);
					this.addressCurrentText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressCurrentSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressCurrentSlider.setMinimum(this.sliderMinimum);
					this.addressCurrentSlider.setMaximum(this.sliderMaximum);
					this.addressCurrentSlider.setIncrement(this.sliderIncrement);
					RowData addressSlider2LData = new RowData();
					addressSlider2LData.width = this.sliderWidth;
					addressSlider2LData.height = 17;
					this.addressCurrentSlider.setLayoutData(addressSlider2LData);
					this.addressCurrentSlider.setSelection(this.configuration.mLinkAddressA1);
					this.addressCurrentSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "addressSlider2.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCurrent = (byte) UniLog2SetupConfiguration2.this.addressCurrentSlider.getSelection();
							UniLog2SetupConfiguration2.this.addressCurrentText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressCurrent]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressRevolutionLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressRevolutionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel2maxLData = new RowData();
					addressLabel2maxLData.width = this.labelWidth;
					addressLabel2maxLData.height = 21;
					this.addressRevolutionLabel.setLayoutData(addressLabel2maxLData);
					this.addressRevolutionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2561));
				}
				{
					this.addressRevolutionText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressRevolutionText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText2maxLData = new RowData();
					addressText2maxLData.width = this.textWidth;
					addressText2maxLData.height = 16;
					this.addressRevolutionText.setLayoutData(addressText2maxLData);
					this.addressRevolutionText.setEditable(false);
					this.addressRevolutionText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressRevolutionSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressRevolutionSlider.setMinimum(this.sliderMinimum);
					this.addressRevolutionSlider.setMaximum(this.sliderMaximum);
					this.addressRevolutionSlider.setIncrement(this.sliderIncrement);
					RowData addressSlider2maxLData = new RowData();
					addressSlider2maxLData.width = this.sliderWidth;
					addressSlider2maxLData.height = 17;
					this.addressRevolutionSlider.setLayoutData(addressSlider2maxLData);
					//this.addressSlider2max.setSelection(this.configuration.mLinkAddressZ);
					this.addressRevolutionSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "addressSlider3.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressRevolution = (byte) UniLog2SetupConfiguration2.this.addressRevolutionSlider.getSelection();
							UniLog2SetupConfiguration2.this.addressRevolutionText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressRevolution]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressCapacityLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressCapacityLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3LData = new RowData();
					addressLabel3LData.width = this.labelWidth;
					addressLabel3LData.height = 21;
					this.addressCapacityLabel.setLayoutData(addressLabel3LData);
					this.addressCapacityLabel.setText(Messages.getString(MessageIds.GDE_MSGT2562));
				}
				{
					this.addressCapacityText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressCapacityText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3LData = new RowData();
					addressText3LData.width = this.textWidth;
					addressText3LData.height = 16;
					this.addressCapacityText.setLayoutData(addressText3LData);
					this.addressCapacityText.setText(this.sliderValues[this.configuration.mLinkAddressCapacity]);
					this.addressCapacityText.setEditable(false);
					this.addressCapacityText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressCapacitySlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressCapacitySlider.setMinimum(this.sliderMinimum);
					this.addressCapacitySlider.setMaximum(this.sliderMaximum);
					this.addressCapacitySlider.setIncrement(this.sliderIncrement);
					RowData addressSlider3LData = new RowData();
					addressSlider3LData.width = this.sliderWidth;
					addressSlider3LData.height = 17;
					this.addressCapacitySlider.setLayoutData(addressSlider3LData);
					this.addressCapacitySlider.setSelection(this.configuration.mLinkAddressCapacity);
					this.addressCapacitySlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "addressSlider3.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCapacity = (byte) UniLog2SetupConfiguration2.this.addressCapacitySlider.getSelection();
							UniLog2SetupConfiguration2.this.addressCapacityText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressCapacity]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressVarioLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressVarioLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3maxLData = new RowData();
					addressLabel3maxLData.width = this.labelWidth;
					addressLabel3maxLData.height = 21;
					this.addressVarioLabel.setLayoutData(addressLabel3maxLData);
					this.addressVarioLabel.setText(Messages.getString(MessageIds.GDE_MSGT2563));
				}
				{
					this.addressVarioText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressVarioText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3maxLData = new RowData();
					addressText3maxLData.width = this.textWidth;
					addressText3maxLData.height = 16;
					this.addressVarioText.setLayoutData(addressText3maxLData);
					this.addressVarioText.setEditable(false);
					this.addressVarioText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressVarioSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressVarioSlider.setMinimum(this.sliderMinimum);
					this.addressVarioSlider.setMaximum(this.sliderMaximum);
					this.addressVarioSlider.setIncrement(this.sliderIncrement);
					RowData addressSlider3maxLData = new RowData();
					addressSlider3maxLData.width = this.sliderWidth;
					addressSlider3maxLData.height = 17;
					this.addressVarioSlider.setLayoutData(addressSlider3maxLData);
					//this.addressSlider3max.setSelection(this.configuration.mLinkAddressHeightMax);
					this.addressVarioSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "addressSlider3max.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressVario = (byte) UniLog2SetupConfiguration2.this.addressVarioSlider.getSelection();
							UniLog2SetupConfiguration2.this.addressVarioText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressVario]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressHeightLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressHeightLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3maxLData = new RowData();
					addressLabel3maxLData.width = this.labelWidth;
					addressLabel3maxLData.height = 21;
					this.addressHeightLabel.setLayoutData(addressLabel3maxLData);
					this.addressHeightLabel.setText(Messages.getString(MessageIds.GDE_MSGT2564));
				}
				{
					this.addressHeightText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressHeightText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3maxLData = new RowData();
					addressText3maxLData.width = this.textWidth;
					addressText3maxLData.height = 16;
					this.addressHeightText.setLayoutData(addressText3maxLData);
					this.addressHeightText.setEditable(false);
					this.addressHeightText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressHeightSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressHeightSlider.setMinimum(this.sliderMinimum);
					this.addressHeightSlider.setMaximum(this.sliderMaximum);
					this.addressHeightSlider.setIncrement(this.sliderIncrement);
					RowData addressSlider3maxLData = new RowData();
					addressSlider3maxLData.width = this.sliderWidth;
					addressSlider3maxLData.height = 17;
					this.addressHeightSlider.setLayoutData(addressSlider3maxLData);
					//this.addressSlider3max.setSelection(this.configuration.mLinkAddressHeightMax);
					this.addressHeightSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "addressSlider3max.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressHeight = (byte) UniLog2SetupConfiguration2.this.addressHeightSlider.getSelection();
							UniLog2SetupConfiguration2.this.addressHeightText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressHeight]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressIntHeightLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressIntHeightLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3maxLData = new RowData();
					addressLabel3maxLData.width = this.labelWidth;
					addressLabel3maxLData.height = 21;
					this.addressIntHeightLabel.setLayoutData(addressLabel3maxLData);
					this.addressIntHeightLabel.setText(Messages.getString(MessageIds.GDE_MSGT2585));
				}
				{
					this.addressHeightGainText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressHeightGainText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3maxLData = new RowData();
					addressText3maxLData.width = this.textWidth;
					addressText3maxLData.height = 16;
					this.addressHeightGainText.setLayoutData(addressText3maxLData);
					this.addressHeightGainText.setEditable(false);
					this.addressHeightGainText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressHeightGainSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressHeightGainSlider.setMinimum(this.sliderMinimum);
					this.addressHeightGainSlider.setMaximum(this.sliderMaximum);
					this.addressHeightGainSlider.setIncrement(this.sliderIncrement);
					RowData addressSlider3maxLData = new RowData();
					addressSlider3maxLData.width = this.sliderWidth;
					addressSlider3maxLData.height = 17;
					this.addressHeightGainSlider.setLayoutData(addressSlider3maxLData);
					//this.addressSlider3max.setSelection(this.configuration.mLinkaddressIntHeightMax);
					this.addressHeightGainSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "addressSlider3max.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressHeightGain = (byte) UniLog2SetupConfiguration2.this.addressHeightGainSlider.getSelection();
							UniLog2SetupConfiguration2.this.addressHeightGainText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressHeightGain]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressEnergyLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressEnergyLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3maxLData = new RowData();
					addressLabel3maxLData.width = this.labelWidth;
					addressLabel3maxLData.height = 21;
					this.addressEnergyLabel.setLayoutData(addressLabel3maxLData);
					this.addressEnergyLabel.setText(Messages.getString(MessageIds.GDE_MSGT2547).split(GDE.STRING_BLANK)[0]);
				}
				{
					this.addressEnergyText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressEnergyText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3maxLData = new RowData();
					addressText3maxLData.width = this.textWidth;
					addressText3maxLData.height = 16;
					this.addressEnergyText.setLayoutData(addressText3maxLData);
					this.addressEnergyText.setEditable(false);
					this.addressEnergyText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressEnergySlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressEnergySlider.setMinimum(this.sliderMinimum);
					this.addressEnergySlider.setMaximum(this.sliderMaximum);
					this.addressEnergySlider.setIncrement(this.sliderIncrement);
					RowData addressSlider3maxLData = new RowData();
					addressSlider3maxLData.width = this.sliderWidth;
					addressSlider3maxLData.height = 17;
					this.addressEnergySlider.setLayoutData(addressSlider3maxLData);
					this.addressEnergySlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "addressSlider3max.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressEnergy = (byte) UniLog2SetupConfiguration2.this.addressEnergySlider.getSelection();
							UniLog2SetupConfiguration2.this.addressEnergyText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressEnergy]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressRemainCapLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressRemainCapLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3maxLData = new RowData();
					addressLabel3maxLData.width = this.labelWidth;
					addressLabel3maxLData.height = 21;
					this.addressRemainCapLabel.setLayoutData(addressLabel3maxLData);
					this.addressRemainCapLabel.setText(Messages.getString(MessageIds.GDE_MSGT2591));
				}
				{
					this.addressRemainCapText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressRemainCapText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3maxLData = new RowData();
					addressText3maxLData.width = this.textWidth;
					addressText3maxLData.height = 16;
					this.addressRemainCapText.setLayoutData(addressText3maxLData);
					this.addressRemainCapText.setEditable(false);
					this.addressRemainCapText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressRemainCapSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressRemainCapSlider.setMinimum(this.sliderMinimum);
					this.addressRemainCapSlider.setMaximum(this.sliderMaximum);
					this.addressRemainCapSlider.setIncrement(this.sliderIncrement);
					RowData addressSlider3maxLData = new RowData();
					addressSlider3maxLData.width = this.sliderWidth;
					addressSlider3maxLData.height = 17;
					this.addressRemainCapSlider.setLayoutData(addressSlider3maxLData);
					this.addressRemainCapSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "addressRemainCapSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressRemainCap = (byte) UniLog2SetupConfiguration2.this.addressRemainCapSlider.getSelection();
							UniLog2SetupConfiguration2.this.addressRemainCapText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressRemainCap]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.cellMinimumLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.cellMinimumLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3maxLData = new RowData();
					addressLabel3maxLData.width = this.labelWidth;
					addressLabel3maxLData.height = 21;
					this.cellMinimumLabel.setLayoutData(addressLabel3maxLData);
					this.cellMinimumLabel.setText(Messages.getString(MessageIds.GDE_MSGT2514));
				}
				{
					this.cellMinimumText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.cellMinimumText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3maxLData = new RowData();
					addressText3maxLData.width = this.textWidth;
					addressText3maxLData.height = 16;
					this.cellMinimumText.setLayoutData(addressText3maxLData);
					this.cellMinimumText.setEditable(false);
					this.cellMinimumText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.cellMinimumSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.cellMinimumSlider.setMinimum(this.sliderMinimum);
					this.cellMinimumSlider.setMaximum(this.sliderMaximum);
					this.cellMinimumSlider.setIncrement(this.sliderIncrement);
					RowData addressSlider3maxLData = new RowData();
					addressSlider3maxLData.width = this.sliderWidth;
					addressSlider3maxLData.height = 17;
					this.cellMinimumSlider.setLayoutData(addressSlider3maxLData);
					//this.addressSlider3max.setSelection(this.configuration.mLinkcellMinimumMax);
					this.cellMinimumSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "addressSlider3max.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCellMinimum = (byte) UniLog2SetupConfiguration2.this.cellMinimumSlider.getSelection();
							UniLog2SetupConfiguration2.this.cellMinimumText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressCellMinimum]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.fillerComposite = new Composite(this.mLinkAddressesGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 280;
					fillerCompositeRA1LData.height = 4;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.a1Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData a1LabelLData = new RowData();
					a1LabelLData.width = GDE.IS_LINUX ? 40 : 43;
					a1LabelLData.height = 19;
					this.a1Label.setLayoutData(a1LabelLData);
					this.a1Label.setText("A1"); //$NON-NLS-1$
					this.a1Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.a1Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData a1ComboLData = new RowData();
					a1ComboLData.width = GDE.IS_LINUX ? 45 : 40;
					a1ComboLData.height = 17;
					this.a1Combo.setLayoutData(a1ComboLData);
					this.a1Combo.setItems(this.sliderValues);
					this.a1Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.a1Combo.setEditable(false);
					this.a1Combo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.a1Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "a1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressA1 = (byte) UniLog2SetupConfiguration2.this.a1Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.a2Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData a2LabelLData = new RowData();
					a2LabelLData.width = GDE.IS_LINUX ? 40 : 43;
					a2LabelLData.height = 19;
					this.a2Label.setLayoutData(a2LabelLData);
					this.a2Label.setText("A2"); //$NON-NLS-1$
					this.a2Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.a2Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData a2ComboLData = new RowData();
					a2ComboLData.width = GDE.IS_LINUX ? 45 : 40;
					a2ComboLData.height = 17;
					this.a2Combo.setLayoutData(a2ComboLData);
					this.a2Combo.setItems(this.sliderValues);
					this.a2Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.a2Combo.setEditable(false);
					this.a2Combo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.a2Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "a1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressA2 = (byte) UniLog2SetupConfiguration2.this.a2Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.a3Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData a3LabelLData = new RowData();
					a3LabelLData.width = GDE.IS_LINUX ? 40 : 43;
					a3LabelLData.height = 19;
					this.a3Label.setLayoutData(a3LabelLData);
					this.a3Label.setText("A3"); //$NON-NLS-1$
					this.a3Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.a3Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData a3ComboLData = new RowData();
					a3ComboLData.width = GDE.IS_LINUX ? 45 : 40;
					a3ComboLData.height = 17;
					this.a3Combo.setLayoutData(a3ComboLData);
					this.a3Combo.setItems(this.sliderValues);
					this.a3Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.a3Combo.setEditable(false);
					this.a3Combo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.a3Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "a1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressA3 = (byte) UniLog2SetupConfiguration2.this.a3Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.c1Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData c1LabelLData = new RowData();
					c1LabelLData.width = GDE.IS_LINUX ? 40 : 43;
					c1LabelLData.height = 19;
					this.c1Label.setLayoutData(c1LabelLData);
					this.c1Label.setText(Messages.getString(MessageIds.GDE_MSGT2570));
					this.c1Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.c1Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData c1ComboLData = new RowData();
					c1ComboLData.width = GDE.IS_LINUX ? 45 : 40;
					c1ComboLData.height = 17;
					this.c1Combo.setLayoutData(c1ComboLData);
					this.c1Combo.setItems(this.sliderValues);
					this.c1Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.c1Combo.setEditable(false);
					this.c1Combo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.c1Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "c1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCell1 = (byte) UniLog2SetupConfiguration2.this.c1Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.c2Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData c2LabelLData = new RowData();
					c2LabelLData.width = GDE.IS_LINUX ? 40 : 43;
					c2LabelLData.height = 19;
					this.c2Label.setLayoutData(c2LabelLData);
					this.c2Label.setText(Messages.getString(MessageIds.GDE_MSGT2571));
					this.c2Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.c2Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData c2ComboLData = new RowData();
					c2ComboLData.width = GDE.IS_LINUX ? 45 : 40;
					c2ComboLData.height = 17;
					this.c2Combo.setLayoutData(c2ComboLData);
					this.c2Combo.setItems(this.sliderValues);
					this.c2Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.c2Combo.setEditable(false);
					this.c2Combo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.c2Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "a1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCell2 = (byte) UniLog2SetupConfiguration2.this.c2Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.c3Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData c3LabelLData = new RowData();
					c3LabelLData.width = GDE.IS_LINUX ? 40 : 43;
					c3LabelLData.height = 19;
					this.c3Label.setLayoutData(c3LabelLData);
					this.c3Label.setText(Messages.getString(MessageIds.GDE_MSGT2572));
					this.c3Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.c3Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData c3ComboLData = new RowData();
					c3ComboLData.width = GDE.IS_LINUX ? 45 : 40;
					c3ComboLData.height = 17;
					this.c3Combo.setLayoutData(c3ComboLData);
					this.c3Combo.setItems(this.sliderValues);
					this.c3Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.c3Combo.setEditable(false);
					this.c3Combo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.c3Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "c3Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCell3 = (byte) UniLog2SetupConfiguration2.this.c3Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.c4Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData c4LabelLData = new RowData();
					c4LabelLData.width = GDE.IS_LINUX ? 40 : 43;
					c4LabelLData.height = 19;
					this.c4Label.setLayoutData(c4LabelLData);
					this.c4Label.setText(Messages.getString(MessageIds.GDE_MSGT2573));
					this.c4Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.c4Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData c4ComboLData = new RowData();
					c4ComboLData.width = GDE.IS_LINUX ? 45 : 40;
					c4ComboLData.height = 17;
					this.c4Combo.setLayoutData(c4ComboLData);
					this.c4Combo.setItems(this.sliderValues);
					this.c4Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.c4Combo.setEditable(false);
					this.c4Combo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.c4Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "c4Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCell4 = (byte) UniLog2SetupConfiguration2.this.c4Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.c5Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData c5LabelLData = new RowData();
					c5LabelLData.width = GDE.IS_LINUX ? 40 : 43;
					c5LabelLData.height = 19;
					this.c5Label.setLayoutData(c5LabelLData);
					this.c5Label.setText(Messages.getString(MessageIds.GDE_MSGT2574));
					this.c5Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.c5Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData c5ComboLData = new RowData();
					c5ComboLData.width = GDE.IS_LINUX ? 45 : 40;
					c5ComboLData.height = 17;
					this.c5Combo.setLayoutData(c5ComboLData);
					this.c5Combo.setItems(this.sliderValues);
					this.c5Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.c5Combo.setEditable(false);
					this.c5Combo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.c5Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "a1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCell5 = (byte) UniLog2SetupConfiguration2.this.c5Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.c6Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData c6LabelLData = new RowData();
					c6LabelLData.width = GDE.IS_LINUX ? 40 : 43;
					c6LabelLData.height = 19;
					this.c6Label.setLayoutData(c6LabelLData);
					this.c6Label.setText(Messages.getString(MessageIds.GDE_MSGT2575));
					this.c6Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.c6Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData c6ComboLData = new RowData();
					c6ComboLData.width = GDE.IS_LINUX ? 45 : 40;
					c6ComboLData.height = 17;
					this.c6Combo.setLayoutData(c6ComboLData);
					this.c6Combo.setItems(this.sliderValues);
					this.c6Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.c6Combo.setEditable(false);
					this.c6Combo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.c6Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "a1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCell6 = (byte) UniLog2SetupConfiguration2.this.c6Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
			}
			{
				this.jetiExGroup = new Group(this, SWT.NONE);
				UniLog2SetupConfiguration2.jetiExGroupStatic = this.jetiExGroup;
				RowLayout mLinkAddressesGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.jetiExGroup.setLayout(mLinkAddressesGroupLayout);
				FormData mLinkAddressesGroupLData = new FormData();
				mLinkAddressesGroupLData.top = new FormAttachment(0, 1000, 335);
				mLinkAddressesGroupLData.left = new FormAttachment(0, 1000, 12);
				mLinkAddressesGroupLData.width = 290;
				mLinkAddressesGroupLData.height = 270;
				this.jetiExGroup.setLayoutData(mLinkAddressesGroupLData);
				this.jetiExGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.jetiExGroup.setText("Jeti EX ");
				{
					this.fillerComposite = new Composite(this.mLinkAddressesGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 280;
					fillerCompositeRA1LData.height = 2;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.measurement1 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement1.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement1.setText(this.jetiValueNames[1]);
					this.measurement1.setEnabled(false);
					this.measurement1.setSelection(true);
					//this.measurement1.addSelectionListener(new SelectionAdapter() {
					//	@Override
					//	public void widgetSelected(SelectionEvent evt) {
					//		log.log(Level.FINEST, "measurement1.widgetSelected, event=" + evt); //$NON-NLS-1$
					//		UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = measurement1.getSelection() ? configuration.jetiValueVisibility & 0xFFFFFFFD : configuration.jetiValueVisibility + 0x00000002;
					//		UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
					//	}
					//});
				}
				{
					this.measurement2 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement2.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement2.setText(this.jetiValueNames[2]);
					this.measurement2.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement2.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement2.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFFFFFB
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00000004;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement3 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement3.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement3.setText(this.jetiValueNames[3]);
					this.measurement3.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement3.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement3.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFFFFF7
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00000008;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement4 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement4.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement4.setText(this.jetiValueNames[4]);
					this.measurement4.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement4.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement4.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFFFFEF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00000010;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				//Höhe, Vario, Drehzahl, Energie, Leistung, Lufdruck, Impuls Ein, Impuls Aus, Zelle Min, Zelle Min#, A1, A2, A3, Temp int, Höhengeweinn
				{
					this.measurement5 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement5.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement5.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement5.setText(this.jetiValueNames[5]);
					this.measurement5.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement5.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement5.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFFFFDF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00000020;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement6 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement6.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement6.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement6.setText(this.jetiValueNames[6]);
					this.measurement6.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement6.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement6.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFFFFBF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00000040;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement7 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement7.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement7.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement7.setText(this.jetiValueNames[7]);
					this.measurement7.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement7.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement7.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFFFF7F
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00000080;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement8 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement8.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement8.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement8.setText(this.jetiValueNames[8]);
					this.measurement8.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement8.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement8.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFFFEFF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00000100;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement9 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement9.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement9.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement9.setText(this.jetiValueNames[9]);
					this.measurement9.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement9.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement9.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFFFDFF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00000200;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement10 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement10.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement10.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement10.setText(this.jetiValueNames[10]);
					this.measurement10.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement10.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement10.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFFFBFF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00000400;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement11 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement11.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement11.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement11.setText(this.jetiValueNames[11]);
					this.measurement11.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement11.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement11.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFFF7FF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00000800;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement12 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement12.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement12.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement12.setText(this.jetiValueNames[12]);
					this.measurement12.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement12.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement12.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFFEFFF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00001000;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement13 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement13.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement13.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement13.setText(this.jetiValueNames[13]);
					this.measurement13.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement13.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement13.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFFDFFF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00002000;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement14 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement14.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement14.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement14.setText(this.jetiValueNames[14]);
					this.measurement14.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement14.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement14.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFFBFFF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00004000;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement15 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement15.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement15.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement15.setText(this.jetiValueNames[15]);
					this.measurement15.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement15.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement15.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFF7FFF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00008000;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement16 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement16.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement16.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement16.setText(this.jetiValueNames[16]);
					this.measurement16.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement16.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement16.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFEFFFF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00010000;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement17 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement17.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement17.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement17.setText(this.jetiValueNames[17]);
					this.measurement17.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement17.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement17.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFDFFFF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00020000;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement18 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement18.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement18.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement18.setText(this.jetiValueNames[18]);
					this.measurement18.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement18.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement18.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFFBFFFF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00040000;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement19 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement19.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement19.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement19.setText(this.jetiValueNames[19]);
					this.measurement19.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement19.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement19.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFF7FFFF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00080000;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement20 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement20.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement20.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement20.setText(this.jetiValueNames[20]);
					this.measurement20.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement20.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility = UniLog2SetupConfiguration2.this.measurement20.getSelection() ? UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility & 0xFFEFFFFF
									: UniLog2SetupConfiguration2.this.configuration.jetiValueVisibility + 0x00100000;
							UniLog2SetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.jetiExSelectionLabel = new CLabel(this.jetiExGroup, SWT.CENTER);
					this.jetiExSelectionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData jetiExSelectionLData = new RowData();
					jetiExSelectionLData.width = 285;
					jetiExSelectionLData.height = 25;
					this.jetiExSelectionLabel.setLayoutData(jetiExSelectionLData);
					this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2590, new Object[] {UniLog2SetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
				}
			}
			{
				this.spektrumAdapterGroup = new Group(this, SWT.NONE);
				UniLog2SetupConfiguration2.spektrumAdapterGroupStatic = this.spektrumAdapterGroup;
				RowLayout mLinkAddressesGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.spektrumAdapterGroup.setLayout(mLinkAddressesGroupLayout);
				FormData mLinkAddressesGroupLData = new FormData();
				mLinkAddressesGroupLData.top = new FormAttachment(0, 1000, 335);
				mLinkAddressesGroupLData.left = new FormAttachment(0, 1000, 12);
				mLinkAddressesGroupLData.width = 290;
				mLinkAddressesGroupLData.height = 145;
				this.spektrumAdapterGroup.setLayoutData(mLinkAddressesGroupLData);
				this.spektrumAdapterGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.spektrumAdapterGroup.setText(Messages.getString(MessageIds.GDE_MSGT2594));
				{
					this.fillerComposite = new Composite(this.spektrumAdapterGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 280;
					fillerCompositeRA1LData.height = 2;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.spektrumAddressLabel = new CLabel(this.spektrumAdapterGroup, SWT.RIGHT);
					this.spektrumAddressLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1LData = new RowData();
					addressLabel1LData.width = 140;
					addressLabel1LData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					this.spektrumAddressLabel.setLayoutData(addressLabel1LData);
					this.spektrumAddressLabel.setText(Messages.getString(MessageIds.GDE_MSGT2595));
				}
				{
					this.spektrumAddressCombo = new CCombo(this.spektrumAdapterGroup, SWT.BORDER);
					RowData currentCComboLData = new RowData();
					currentCComboLData.width = 60;
					currentCComboLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					this.spektrumAddressCombo.setLayoutData(currentCComboLData);
					this.spektrumAddressCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.spektrumAddressCombo.setItems(this.spektrumAddresses);
					this.spektrumAddressCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					//this.spektrumAddressCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.spektrumNumbers)); //$NON-NLS-1$
					this.spektrumAddressCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "spektrumAddrssCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.spektrumNumber = (byte) Integer.parseInt(UniLog2SetupConfiguration2.this.spektrumAddressCombo.getText().trim());
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.fillerComposite = new Composite(this.spektrumAdapterGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 280;
					fillerCompositeRA1LData.height = 2;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.fillerComposite = new Composite(this.spektrumAdapterGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.spektrumEscSensor = new Button(this.spektrumAdapterGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 280 - this.fillerWidth;
					analogAlarm1DirectionButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					this.spektrumEscSensor.setLayoutData(analogAlarm1DirectionButtonLData);
					this.spektrumEscSensor.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.spektrumEscSensor.setText(Messages.getString(MessageIds.GDE_MSGT2596));
					this.spektrumEscSensor.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "spektrumEscSensor.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.spektrumSensors = (byte) (UniLog2SetupConfiguration2.this.spektrumEscSensor.getSelection() 
									? UniLog2SetupConfiguration2.this.configuration.spektrumSensors & 0xFE
									: UniLog2SetupConfiguration2.this.configuration.spektrumSensors | 0x01);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.fillerComposite = new Composite(this.spektrumAdapterGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.spektrumCurrentSensor = new Button(this.spektrumAdapterGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 280 - this.fillerWidth;
					analogAlarm1DirectionButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					this.spektrumCurrentSensor.setLayoutData(analogAlarm1DirectionButtonLData);
					this.spektrumCurrentSensor.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.spektrumCurrentSensor.setText(Messages.getString(MessageIds.GDE_MSGT2597));
					this.spektrumCurrentSensor.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "spektrumCurrentSensor.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.spektrumSensors = (byte) (UniLog2SetupConfiguration2.this.spektrumCurrentSensor.getSelection() 
									? UniLog2SetupConfiguration2.this.configuration.spektrumSensors & 0xFD
									: UniLog2SetupConfiguration2.this.configuration.spektrumSensors | 0x02);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.fillerComposite = new Composite(this.spektrumAdapterGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.spektrumVarioSensor = new Button(this.spektrumAdapterGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 280 - this.fillerWidth;
					analogAlarm1DirectionButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					this.spektrumVarioSensor.setLayoutData(analogAlarm1DirectionButtonLData);
					this.spektrumVarioSensor.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.spektrumVarioSensor.setText(Messages.getString(MessageIds.GDE_MSGT2598));
					this.spektrumVarioSensor.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "spektrumVarioSensor.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.spektrumSensors = (byte) (UniLog2SetupConfiguration2.this.spektrumVarioSensor.getSelection() 
									? UniLog2SetupConfiguration2.this.configuration.spektrumSensors & 0xFB
									: UniLog2SetupConfiguration2.this.configuration.spektrumSensors | 0x04);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.fillerComposite = new Composite(this.spektrumAdapterGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.spektrumLiPoMonitor = new Button(this.spektrumAdapterGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 280 - this.fillerWidth;
					analogAlarm1DirectionButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					this.spektrumLiPoMonitor.setLayoutData(analogAlarm1DirectionButtonLData);
					this.spektrumLiPoMonitor.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.spektrumLiPoMonitor.setText(Messages.getString(MessageIds.GDE_MSGT2599));
					this.spektrumLiPoMonitor.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLog2SetupConfiguration2.log.log(java.util.logging.Level.FINEST, "spektrumLiPoMonitor.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.spektrumSensors = (byte) (UniLog2SetupConfiguration2.this.spektrumLiPoMonitor.getSelection() 
									? UniLog2SetupConfiguration2.this.configuration.spektrumSensors & 0xF7
									: UniLog2SetupConfiguration2.this.configuration.spektrumSensors | 0x08);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
			}
			this.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
