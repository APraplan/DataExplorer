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

import gde.GDE;
import gde.device.DataTypes;
import gde.device.smmodellbau.gpslogger.MessageIds;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

import java.util.Locale;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
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
 * class to implement SM GPS-Logger UniLog and Multiplex M-Link reverse link configuration panel 2
 */
public class GPSLoggerSetupConfiguration2 extends org.eclipse.swt.widgets.Composite {
	final static Logger			log									= Logger.getLogger(GPSLoggerSetupConfiguration2.class.getName());

	final GPSLoggerDialog		dialog;
	final DataExplorer		  application;

	Composite								fillerComposite;
	
	final int								fillerWidth					= 15;
	final int								fillerHeight				= 25;
	final int								buttonWidth					= 115;
	final int								comboWidth					= 83;

	Group										mLinkAddressesGroup;
	static Group						mLinkGroupStatic;
	CLabel									addressVarioTecLabel, addressSpeedLabel, addressSpeedMaxLabel, addressAltitudeLabel, addressAltiMaxLabel, addressDistanceLabel, addressDirectionLabel, addressFlightDirectionLabel, addressDirectionRelLabel, addressTripLengthLabel, addressAltitudeGainLabel, addressEnlLabel, addressAccXLabel, addressAccYLabel, addressAccZLabel, addressVoltageRxLabel, addressVarioLabel, addressAirSpeedLabel;
	Text										addressVarioTecText, addressSpeedText, addressSpeedMaxText, addressAltitudeText, addressAltiMaxText, addressDistanceText, addressDirectionText, addressFlightDirectionText, addressDirectionRelText, addressTripLengthText, addressAltitudeGainText, addressEnlText, addressAccXText, addressAccYText, addressAccZText, addressVoltageRxText, addressVarioText, addressAirSpeedText;
	Slider									addressVarioTecSlider, addressSpeedSlider, addressSpeedMaxSlider, addressAltitudeSlider, addressAltiMaxSlider, addressDistanceSlider, addressDirectionSlider, addressFlightDirectionSlider, addressDirectionRelSlider, addressTripLengthSlider, addressHightGainSlider, addressEnlSlider, addressAccXSlider, addressAccYSlider, addressAccZSlider, addressVoltageRxSlider, addressVarioSlider, addressAirSpeedSlider;

	Group										jetiExGroup;
	static Group						jetiExGroupStatic;
	Button									measurement1, measurement2, measurement3, measurement4, measurement5, measurement6, measurement7, measurement8, measurement9, measurement10, 
													measurement11, measurement12, measurement13, measurement14, measurement15, measurement16, measurement17, measurement18, measurement19, measurement20, 
													measurement21, measurement22, measurement23, measurement24, measurement25, measurement26, measurement27, measurement28, measurement29, measurement30,
													measurement31, measurement32, measurement33, measurement34, measurement35, measurement36, measurement37;
	final String[]					jetiValueNames					= Messages.getString(MessageIds.GDE_MSGT2083).split(GDE.STRING_COMMA);
	CLabel									jetiExSelectionLabel;

	Group										unilogTelemtryAlarmsGroup;
	static Group						unilogTelemtryAlarmsGroupStatic;
	Button									currentButton, voltageStartButton, voltageButton, capacityButton;
	CCombo									currentCombo, voltageStartCombo, voltageCombo, capacityCombo;
	CLabel									currentLabel, voltageStartLabel, voltageLabel, capacityLabel, jetiUniLogAlarmsLabel;

	Group										spektrumAdapterGroup;
	static Group						spektrumAdapterGroupStatic;
	CLabel									spektrumAddressLabel;
	CCombo									spektrumAddressCombo;
	final String[]					spektrumAddresses = { " 0 ", " 1 ", " 2 ", " 3 ", " 4 ", " 5 ", " 6 ", " 7 ", " 8 ", " 9 "};
	Button									spektrumGPSSensor, spektrumVarioSensor, spektrumGforceSensor, spektrumUserSensor, spektrumSpeedSensor;
	
	Group										fixGpsStartPositionGroup;
	static Group						fixGpsStartPositionGroupStatic;
	Button									useFixGpsStartPositionButton;
	CLabel									fixLatitudeLabel, fixLongitudeLabel, fixHeightLabel, fixLatitudeUnitLabel, fixLongitudeUnitLabel, fixHeightUnitLabel;
	Text										fixLatitudeText, fixLongitudeText, fixHeightText;

	final int								labelWidth					= 110;
	final int								textWidth						= 25;
	final int								sliderWidth					= 125;
	final int								sliderMinimum				= 0;
	final int								sliderMaximum				= 26;
	final int								sliderIncrement			= 1;
	final String[]					sliderValues 				= {" 0", " 1", " 2", " 3", " 4", " 5", " 6", " 7", " 8", " 9", "10", "11", "12", "13", "14", "15", "- -"};

	final String[]					currentValues				= { "    1", "    5", "   10", "   15", "   20", "   30", "   40", "   50", "   60", "   70", "   80", "   90", "  100", "  150", "  200", "  250",
			"  300", "  350", "  400"							};
	final String[]					voltageStartValues	= { "  3.0", "  3.2", "  3.3", "  3.4", "  3.5", "  4.0", "  4.5", "  5.0", "  5.5", "  6.0", "  6.4", "  6.5", "  6.6", "  6.7", "  6.8", "  7.0",
			"  7.5", "  8.0", "  8.5", "  9.0", "  9.5", "  9.6", "  9.7", "  9.8", " 10.0", " 10.5", " 11.0", " 11.5", " 12.0", " 12.2", " 12.3", " 12.4", " 12.5", " 13.0", " 13.5", " 14.0", " 14.5",
			" 15.0", " 15.5", " 15.6", " 15.7", " 15.8", " 15.9", " 16.0", " 16.5", " 17.0", " 17.5", " 18.0", " 18.5", " 19.0", " 19.5", " 20.0", " 20.3", " 20.3", " 20.4", " 20.5", " 20.6", " 20.7",
			" 21.0", " 21.5", " 22.0", " 23.0", " 24.0", " 24.5", " 24.6", " 24.7", " 24.8", " 25.0", " 30.0", " 40.0", " 50.0", " 60.0" };

	final String[]					capacityValues 			= {"  250", "  500", "  750", " 1000", " 1250", " 1500", " 1750", " 2000", " 2250", " 2500", " 2750", " 3000", " 3250", " 3500", " 4000", " 5000", " 6000"};
	final SetupReaderWriter	configuration;

	/**
	 * constructor configuration panel 2
	 * @param parent
	* @param dialog
	* @param configuration
	*/
	public GPSLoggerSetupConfiguration2(Composite parent, int style, GPSLoggerDialog useDialog, SetupReaderWriter useConfiguration) {
		super(parent, style);
		SWTResourceManager.registerResourceUser(this);
		this.dialog = useDialog;
		this.configuration = useConfiguration;
		this.application = DataExplorer.getInstance();
		initGUI();
	}

	public void updateValues() {
		this.addressVarioTecSlider.setSelection(this.configuration.mLinkAddressVarioTec);
		this.addressVarioTecText.setText(sliderValues[this.configuration.mLinkAddressVarioTec]);
		this.addressSpeedSlider.setSelection(this.configuration.mLinkAddressSpeed);
		this.addressSpeedText.setText(sliderValues[this.configuration.mLinkAddressSpeed]);
		this.addressSpeedMaxSlider.setSelection(this.configuration.mLinkAddressSpeedMax);
		this.addressSpeedMaxText.setText(sliderValues[this.configuration.mLinkAddressSpeedMax]);
		this.addressAltitudeSlider.setSelection(this.configuration.mLinkAddressAltitude);
		this.addressAltitudeText.setText(sliderValues[this.configuration.mLinkAddressAltitude]);
		this.addressAltiMaxSlider.setSelection(this.configuration.mLinkAddressHeightMax);
		this.addressAltiMaxText.setText(sliderValues[this.configuration.mLinkAddressHeightMax]);
		this.addressDistanceSlider.setSelection(this.configuration.mLinkAddressDistance);
		this.addressDistanceText.setText(sliderValues[this.configuration.mLinkAddressDistance]);
		this.addressDirectionSlider.setSelection(this.configuration.mLinkAddressDirection);
		this.addressDirectionText.setText(sliderValues[this.configuration.mLinkAddressDirection]);	
		
		this.addressFlightDirectionSlider.setSelection(this.configuration.mLinkAddressFlightDirection);
		this.addressFlightDirectionText.setText(sliderValues[this.configuration.mLinkAddressFlightDirection]);
		this.addressDirectionRelSlider.setSelection(this.configuration.mLinkAddressDirectionRel);
		this.addressDirectionRelText.setText(sliderValues[this.configuration.mLinkAddressDirectionRel]);
		
		this.addressTripLengthSlider.setSelection(this.configuration.mLinkAddressTripLength);
		this.addressTripLengthText.setText(sliderValues[this.configuration.mLinkAddressTripLength]);

		this.addressHightGainSlider.setSelection(this.configuration.mLinkAddressAltitudeGain);
		this.addressAltitudeGainText.setText(sliderValues[this.configuration.mLinkAddressAltitudeGain]);

		this.addressVoltageRxSlider.setSelection(this.configuration.mLinkAddressVoltageRx);
		this.addressVoltageRxText.setText(sliderValues[this.configuration.mLinkAddressVoltageRx]);

		this.addressEnlSlider.setSelection(this.configuration.mLinkAddressENL);
		this.addressEnlText.setText(sliderValues[this.configuration.mLinkAddressENL]);
		this.addressAccXSlider.setSelection(this.configuration.mLinkAddressAccX);
		this.addressAccXText.setText(sliderValues[this.configuration.mLinkAddressAccX]);
		this.addressAccYSlider.setSelection(this.configuration.mLinkAddressAccY);
		this.addressAccYText.setText(sliderValues[this.configuration.mLinkAddressAccY]);
		this.addressAccZSlider.setSelection(this.configuration.mLinkAddressAccZ);
		this.addressAccZText.setText(sliderValues[this.configuration.mLinkAddressAccZ]);
		
		this.addressVarioSlider.setSelection(this.configuration.mLinkAddressVario);
		this.addressVarioText.setText(sliderValues[this.configuration.mLinkAddressVario]);
		this.addressAirSpeedSlider.setSelection(this.configuration.mLinkAddressAirSpeed);
		this.addressAirSpeedText.setText(sliderValues[this.configuration.mLinkAddressAirSpeed]);

		this.currentButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CURRENT_UL) > 0);
		this.currentCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.currentUlAlarm)); //$NON-NLS-1$
		this.voltageStartButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_START_UL) > 0);
		this.voltageStartCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageStartUlAlarm / 10.0)); //$NON-NLS-1$
		this.voltageButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_UL) > 0);
		this.voltageCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageUlAlarm / 10.0)); //$NON-NLS-1$
		this.capacityButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CAPACITY_UL) > 0);
		this.capacityCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.capacityUlAlarm)); //$NON-NLS-1$
		
		this.measurement1.setSelection((this.configuration.jetiExMask  & 0x00000002) == 0);
		this.measurement2.setSelection((this.configuration.jetiExMask  & 0x00000004) == 0);
		this.measurement3.setSelection((this.configuration.jetiExMask  & 0x00000008) == 0);
		this.measurement4.setSelection((this.configuration.jetiExMask  & 0x00000010) == 0);
		this.measurement5.setSelection((this.configuration.jetiExMask  & 0x00000020) == 0);
		this.measurement6.setSelection((this.configuration.jetiExMask  & 0x00000040) == 0);
		this.measurement7.setSelection((this.configuration.jetiExMask  & 0x00000080) == 0);
		this.measurement8.setSelection((this.configuration.jetiExMask  & 0x00000100) == 0);
		this.measurement9.setSelection((this.configuration.jetiExMask  & 0x00000200) == 0);
		this.measurement10.setSelection((this.configuration.jetiExMask & 0x00000400) == 0);
		this.measurement11.setSelection((this.configuration.jetiExMask & 0x00000800) == 0);
		this.measurement12.setSelection((this.configuration.jetiExMask & 0x00001000) == 0);
		this.measurement13.setSelection((this.configuration.jetiExMask & 0x00002000) == 0);
		this.measurement14.setSelection((this.configuration.jetiExMask & 0x00004000) == 0);
		this.measurement15.setSelection((this.configuration.jetiExMask & 0x00008000) == 0);
		this.measurement16.setSelection((this.configuration.jetiExMask & 0x00010000) == 0);
		this.measurement17.setSelection((this.configuration.jetiExMask & 0x00020000) == 0);
		this.measurement18.setSelection((this.configuration.jetiExMask & 0x00040000) == 0);
		this.measurement19.setSelection((this.configuration.jetiExMask & 0x00080000) == 0);
		this.measurement20.setSelection((this.configuration.jetiExMask & 0x00100000) == 0);
		this.measurement21.setSelection((this.configuration.jetiExMask & 0x00200000) == 0);
		this.measurement22.setSelection((this.configuration.jetiExMask & 0x00400000) == 0);
		this.measurement23.setSelection((this.configuration.jetiExMask & 0x00800000) == 0);
		this.measurement24.setSelection((this.configuration.jetiExMask & 0x01000000) == 0);
		this.measurement25.setSelection((this.configuration.jetiExMask & 0x02000000) == 0);
		this.measurement26.setSelection((this.configuration.jetiExMask & 0x04000000) == 0);
		
		this.measurement27.setSelection((this.configuration.jetiExMask_UL & 0x0001) == 0);
		this.measurement28.setSelection((this.configuration.jetiExMask_UL & 0x0002) == 0);
		this.measurement29.setSelection((this.configuration.jetiExMask_UL & 0x0004) == 0);
		this.measurement30.setSelection((this.configuration.jetiExMask_UL & 0x0008) == 0);
		this.measurement31.setSelection((this.configuration.jetiExMask_UL & 0x0010) == 0);
		this.measurement32.setSelection((this.configuration.jetiExMask_UL & 0x0020) == 0);
		this.measurement33.setSelection((this.configuration.jetiExMask_UL & 0x0040) == 0);
		this.measurement34.setSelection((this.configuration.jetiExMask_UL & 0x0080) == 0);
		this.measurement35.setSelection((this.configuration.jetiExMask_UL & 0x0100) == 0);
		this.measurement36.setSelection((this.configuration.jetiExMask_UL & 0x0200) == 0);
		this.measurement37.setSelection((this.configuration.jetiExMask_UL & 0x0400) == 0);
		this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {this.configuration.getJetiMeasurementCount()}));

		this.spektrumAddressCombo.select(this.configuration.spektrumNumber);
		this.spektrumGPSSensor.setSelection((this.configuration.spektrumSensors & 0x01) == 0);
		this.spektrumVarioSensor.setSelection((this.configuration.spektrumSensors & 0x02) == 0);
		this.spektrumGforceSensor.setSelection((this.configuration.spektrumSensors & 0x04) == 0);
		this.spektrumUserSensor.setSelection((this.configuration.spektrumSensors & 0x08) == 0);
		this.spektrumSpeedSensor.setSelection((this.configuration.spektrumSensors & 0x10) == 0);
		
		this.useFixGpsStartPositionButton.setSelection((GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude >> 24 & 0x80) > 0);
		double tempLatitudeValue = ((GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude & 0x3FFFFFFF) / 10000.0);
		char tempLatDir = (GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude >> 24 & 0x40) > 0 ? 'S' : 'N';
		this.fixLatitudeText.setText(String.format(Locale.ENGLISH, "%09.4f %c", tempLatitudeValue, tempLatDir)); //$NON-NLS-1$
		double tempLongitudeValue = ((this.configuration.fixPositionLongitude & 0x3FFFFFFF) / 10000.0);
		char tempLonDir = (this.configuration.fixPositionLatitude >> 24 & 0x40) > 0 ? 'W' : 'O';
		this.fixLongitudeText.setText(String.format(Locale.ENGLISH, "%010.4f %c", tempLongitudeValue, tempLonDir)); //$NON-NLS-1$
		this.fixHeightText.setText(String.format(Locale.ENGLISH, "%d", this.configuration.fixPositionAltitude)); //$NON-NLS-1$
	}

	void initGUI() {
		try {
			this.setLayout(new FormLayout());
			this.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "GPSLoggerSetupConfiguration2.helpRequested, event=" + evt); //$NON-NLS-1$
					GPSLoggerSetupConfiguration2.this.application.openHelpDialog(Messages.getString(MessageIds.GDE_MSGT2010), "HelpInfo.html#configuration");  //$NON-NLS-1$
				}
			});
			{
				this.mLinkAddressesGroup = new Group(this, SWT.NONE);
				GPSLoggerSetupConfiguration2.mLinkGroupStatic = this.mLinkAddressesGroup;
				RowLayout mLinkAddressesGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.mLinkAddressesGroup.setLayout(mLinkAddressesGroupLayout);
				FormData mLinkAddressesGroupLData = new FormData();
				mLinkAddressesGroupLData.top = new FormAttachment(0, 1000, 2);
				mLinkAddressesGroupLData.left = new FormAttachment(0, 1000, 15);
				mLinkAddressesGroupLData.width = 290;
				mLinkAddressesGroupLData.height = GDE.IS_MAC ? 440 : 445;
				this.mLinkAddressesGroup.setLayoutData(mLinkAddressesGroupLData);
				this.mLinkAddressesGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.mLinkAddressesGroup.setText(Messages.getString(MessageIds.GDE_MSGT2058));
				{
					this.addressVarioTecLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressVarioTecLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1LData = new RowData();
					addressLabel1LData.width = labelWidth;
					addressLabel1LData.height = 18;
					this.addressVarioTecLabel.setLayoutData(addressLabel1LData);
					this.addressVarioTecLabel.setText(Messages.getString(MessageIds.GDE_MSGT2059));
				}
				{
					this.addressVarioTecText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressVarioTecText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText1LData = new RowData();
					addressText1LData.width = textWidth;
					addressText1LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressVarioTecText.setLayoutData(addressText1LData);
					this.addressVarioTecText.setText(sliderValues[this.configuration.mLinkAddressVarioTec]);
					this.addressVarioTecText.setEditable(false);
					this.addressVarioTecText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressVarioTecSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressVarioTecSlider.setMinimum(sliderMinimum);
					this.addressVarioTecSlider.setMaximum(sliderMaximum);
					this.addressVarioTecSlider.setIncrement(sliderIncrement);
					RowData addressSlider1LData = new RowData();
					addressSlider1LData.width = sliderWidth;
					addressSlider1LData.height = 17;
					this.addressVarioTecSlider.setLayoutData(addressSlider1LData);
					this.addressVarioTecSlider.setSelection(this.configuration.mLinkAddressVarioTec);
					this.addressVarioTecSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressVarioSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressVarioTec = (byte) GPSLoggerSetupConfiguration2.this.addressVarioTecSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressVarioTecText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressVarioTec]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressSpeedLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressSpeedLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel2LData = new RowData();
					addressLabel2LData.width = labelWidth;
					addressLabel2LData.height = 18;
					this.addressSpeedLabel.setLayoutData(addressLabel2LData);
					this.addressSpeedLabel.setText(Messages.getString(MessageIds.GDE_MSGT2060));
				}
				{
					this.addressSpeedText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressSpeedText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText2LData = new RowData();
					addressText2LData.width = textWidth;
					addressText2LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressSpeedText.setLayoutData(addressText2LData);
					this.addressSpeedText.setText(sliderValues[this.configuration.mLinkAddressSpeed]);
					this.addressSpeedText.setEditable(false);
					this.addressSpeedText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressSpeedSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSpeedSlider.setMinimum(sliderMinimum);
					this.addressSpeedSlider.setMaximum(sliderMaximum);
					this.addressSpeedSlider.setIncrement(sliderIncrement);
					RowData addressSlider2LData = new RowData();
					addressSlider2LData.width = sliderWidth;
					addressSlider2LData.height = 17;
					this.addressSpeedSlider.setLayoutData(addressSlider2LData);
					this.addressSpeedSlider.setSelection(this.configuration.mLinkAddressSpeed);
					this.addressSpeedSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSpeedSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressSpeed = (byte) GPSLoggerSetupConfiguration2.this.addressSpeedSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressSpeedText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressSpeed]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressSpeedMaxLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressSpeedMaxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel2maxLData = new RowData();
					addressLabel2maxLData.width = labelWidth;
					addressLabel2maxLData.height = 18;
					this.addressSpeedMaxLabel.setLayoutData(addressLabel2maxLData);
					this.addressSpeedMaxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2060) + "_max");
				}
				{
					this.addressSpeedMaxText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressSpeedMaxText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText2maxLData = new RowData();
					addressText2maxLData.width = textWidth;
					addressText2maxLData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressSpeedMaxText.setLayoutData(addressText2maxLData);
					this.addressSpeedMaxText.setText(sliderValues[this.configuration.mLinkAddressSpeedMax]);
					this.addressSpeedMaxText.setEditable(false);
					this.addressSpeedMaxText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressSpeedMaxSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSpeedMaxSlider.setMinimum(sliderMinimum);
					this.addressSpeedMaxSlider.setMaximum(sliderMaximum);
					this.addressSpeedMaxSlider.setIncrement(sliderIncrement);
					RowData addressSlider2maxLData = new RowData();
					addressSlider2maxLData.width = sliderWidth;
					addressSlider2maxLData.height = 17;
					this.addressSpeedMaxSlider.setLayoutData(addressSlider2maxLData);
					this.addressSpeedMaxSlider.setSelection(this.configuration.mLinkAddressSpeedMax);
					this.addressSpeedMaxSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSpeedMaxSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressSpeedMax = (byte) GPSLoggerSetupConfiguration2.this.addressSpeedMaxSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressSpeedMaxText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressSpeedMax]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressAltitudeLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressAltitudeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3LData = new RowData();
					addressLabel3LData.width = labelWidth;
					addressLabel3LData.height = 18;
					this.addressAltitudeLabel.setLayoutData(addressLabel3LData);
					this.addressAltitudeLabel.setText(Messages.getString(MessageIds.GDE_MSGT2061));
				}
				{
					this.addressAltitudeText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressAltitudeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3LData = new RowData();
					addressText3LData.width = textWidth;
					addressText3LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressAltitudeText.setLayoutData(addressText3LData);
					this.addressAltitudeText.setText(sliderValues[this.configuration.mLinkAddressAltitude]);
					this.addressAltitudeText.setEditable(false);
					this.addressAltitudeText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressAltitudeSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressAltitudeSlider.setMinimum(sliderMinimum);
					this.addressAltitudeSlider.setMaximum(sliderMaximum);
					this.addressAltitudeSlider.setIncrement(sliderIncrement);
					RowData addressSlider3LData = new RowData();
					addressSlider3LData.width = sliderWidth;
					addressSlider3LData.height = 17;
					this.addressAltitudeSlider.setLayoutData(addressSlider3LData);
					this.addressAltitudeSlider.setSelection(this.configuration.mLinkAddressAltitude);
					this.addressAltitudeSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressAltitudeSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAltitude = (byte) GPSLoggerSetupConfiguration2.this.addressAltitudeSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressAltitudeText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAltitude]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressAltiMaxLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressAltiMaxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3maxLData = new RowData();
					addressLabel3maxLData.width = labelWidth;
					addressLabel3maxLData.height = 18;
					this.addressAltiMaxLabel.setLayoutData(addressLabel3maxLData);
					this.addressAltiMaxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2061) + "_max");
				}
				{
					this.addressAltiMaxText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressAltiMaxText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3maxLData = new RowData();
					addressText3maxLData.width = textWidth;
					addressText3maxLData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressAltiMaxText.setLayoutData(addressText3maxLData);
					this.addressAltiMaxText.setText(sliderValues[this.configuration.mLinkAddressHeightMax]);
					this.addressAltiMaxText.setEditable(false);
					this.addressAltiMaxText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressAltiMaxSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressAltiMaxSlider.setMinimum(sliderMinimum);
					this.addressAltiMaxSlider.setMaximum(sliderMaximum);
					this.addressAltiMaxSlider.setIncrement(sliderIncrement);
					RowData addressSlider3maxLData = new RowData();
					addressSlider3maxLData.width = sliderWidth;
					addressSlider3maxLData.height = 17;
					this.addressAltiMaxSlider.setLayoutData(addressSlider3maxLData);
					this.addressAltiMaxSlider.setSelection(this.configuration.mLinkAddressHeightMax);
					this.addressAltiMaxSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressAltiMaxSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressHeightMax = (byte) GPSLoggerSetupConfiguration2.this.addressAltiMaxSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressAltiMaxText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressHeightMax]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressDistanceLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressDistanceLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel4LData = new RowData();
					addressLabel4LData.width = labelWidth;
					addressLabel4LData.height = 18;
					this.addressDistanceLabel.setLayoutData(addressLabel4LData);
					this.addressDistanceLabel.setText(Messages.getString(MessageIds.GDE_MSGT2062));
				}
				{
					this.addressDistanceText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressDistanceText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText4LData = new RowData();
					addressText4LData.width = textWidth;
					addressText4LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressDistanceText.setLayoutData(addressText4LData);
					this.addressDistanceText.setText(sliderValues[this.configuration.mLinkAddressDistance]);
					this.addressDistanceText.setEditable(false);
					this.addressDistanceText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressDistanceSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressDistanceSlider.setMinimum(sliderMinimum);
					this.addressDistanceSlider.setMaximum(sliderMaximum);
					this.addressDistanceSlider.setIncrement(sliderIncrement);
					RowData addressSlider4LData = new RowData();
					addressSlider4LData.width = sliderWidth;
					addressSlider4LData.height = 17;
					this.addressDistanceSlider.setLayoutData(addressSlider4LData);
					this.addressDistanceSlider.setSelection(this.configuration.mLinkAddressDistance);
					this.addressDistanceSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressDistanceSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDistance = (byte) GPSLoggerSetupConfiguration2.this.addressDistanceSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressDistanceText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDistance]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressDirectionLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressDirectionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel5LData = new RowData();
					addressLabel5LData.width = labelWidth;
					addressLabel5LData.height = 18;
					this.addressDirectionLabel.setLayoutData(addressLabel5LData);
					this.addressDirectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2063));
				}
				{
					this.addressDirectionText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressDirectionText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText5LData = new RowData();
					addressText5LData.width = textWidth;
					addressText5LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressDirectionText.setLayoutData(addressText5LData);
					this.addressDirectionText.setText(sliderValues[this.configuration.mLinkAddressDirection]);
					this.addressDirectionText.setEditable(false);
					this.addressDirectionText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressDirectionSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressDirectionSlider.setMinimum(sliderMinimum);
					this.addressDirectionSlider.setMaximum(sliderMaximum);
					this.addressDirectionSlider.setIncrement(sliderIncrement);
					RowData addressSlider5LData = new RowData();
					addressSlider5LData.width = sliderWidth;
					addressSlider5LData.height = 17;
					this.addressDirectionSlider.setLayoutData(addressSlider5LData);
					this.addressDirectionSlider.setSelection(this.configuration.mLinkAddressDirection);
					this.addressDirectionSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressDirectionSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDirection = (byte) GPSLoggerSetupConfiguration2.this.addressDirectionSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressDirectionText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDirection]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressFlightDirectionLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressFlightDirectionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel6LData = new RowData();
					addressLabel6LData.width = labelWidth;
					addressLabel6LData.height = 18;
					this.addressFlightDirectionLabel.setLayoutData(addressLabel6LData);
					this.addressFlightDirectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2028));
				}
				{
					this.addressFlightDirectionText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressFlightDirectionText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText6LData = new RowData();
					addressText6LData.width = textWidth;
					addressText6LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressFlightDirectionText.setLayoutData(addressText6LData);
					this.addressFlightDirectionText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressFlightDirection]);
					this.addressFlightDirectionText.setEditable(false);
					this.addressFlightDirectionText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressFlightDirectionSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressFlightDirectionSlider.setMinimum(sliderMinimum);
					this.addressFlightDirectionSlider.setMaximum(sliderMaximum);
					this.addressFlightDirectionSlider.setIncrement(sliderIncrement);
					RowData addressSlider6LData = new RowData();
					addressSlider6LData.width = sliderWidth;
					addressSlider6LData.height = 17;
					this.addressFlightDirectionSlider.setLayoutData(addressSlider6LData);
					this.addressFlightDirectionSlider.setSelection(this.configuration.mLinkAddressFlightDirection);
					this.addressFlightDirectionSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressFlightDirectionSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressFlightDirection = (byte) GPSLoggerSetupConfiguration2.this.addressFlightDirectionSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressFlightDirectionText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressFlightDirection]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressDirectionRelLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressDirectionRelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel7LData = new RowData();
					addressLabel7LData.width = labelWidth;
					addressLabel7LData.height = 18;
					this.addressDirectionRelLabel.setLayoutData(addressLabel7LData);
					this.addressDirectionRelLabel.setText(Messages.getString(MessageIds.GDE_MSGT2029));
				}
				{
					this.addressDirectionRelText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressDirectionRelText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText7LData = new RowData();
					addressText7LData.width = textWidth;
					addressText7LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressDirectionRelText.setLayoutData(addressText7LData);
					this.addressDirectionRelText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDirectionRel]);
					this.addressDirectionRelText.setEditable(false);
					this.addressDirectionRelText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressDirectionRelSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressDirectionRelSlider.setMinimum(sliderMinimum);
					this.addressDirectionRelSlider.setMaximum(sliderMaximum);
					this.addressDirectionRelSlider.setIncrement(sliderIncrement);
					RowData addressSlider7LData = new RowData();
					addressSlider7LData.width = sliderWidth;
					addressSlider7LData.height = 17;
					this.addressDirectionRelSlider.setLayoutData(addressSlider7LData);
					this.addressDirectionRelSlider.setSelection(this.configuration.mLinkAddressDirectionRel);
					this.addressDirectionRelSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressDirectionRelSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDirectionRel = (byte) GPSLoggerSetupConfiguration2.this.addressDirectionRelSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressDirectionRelText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDirectionRel]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressTripLengthLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressTripLengthLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel8LData = new RowData();
					addressLabel8LData.width = labelWidth;
					addressLabel8LData.height = 18;
					this.addressTripLengthLabel.setLayoutData(addressLabel8LData);
					this.addressTripLengthLabel.setText(Messages.getString(MessageIds.GDE_MSGT2064));
				}
				{
					this.addressTripLengthText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressTripLengthText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText8LData = new RowData();
					addressText8LData.width = textWidth;
					addressText8LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressTripLengthText.setLayoutData(addressText8LData);
					this.addressTripLengthText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength]);
					this.addressTripLengthText.setEditable(false);
					this.addressTripLengthText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressTripLengthSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressTripLengthSlider.setMinimum(sliderMinimum);
					this.addressTripLengthSlider.setMaximum(sliderMaximum);
					this.addressTripLengthSlider.setIncrement(sliderIncrement);
					RowData addressSlider8LData = new RowData();
					addressSlider8LData.width = sliderWidth;
					addressSlider8LData.height = 17;
					this.addressTripLengthSlider.setLayoutData(addressSlider8LData);
					this.addressTripLengthSlider.setSelection(this.configuration.mLinkAddressTripLength);
					this.addressTripLengthSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressTripLengthSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength = (byte) GPSLoggerSetupConfiguration2.this.addressTripLengthSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressTripLengthText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressAltitudeGainLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressAltitudeGainLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel9LData = new RowData();
					addressLabel9LData.width = labelWidth;
					addressLabel9LData.height = 18;
					this.addressAltitudeGainLabel.setLayoutData(addressLabel9LData);
					this.addressAltitudeGainLabel.setText(Messages.getString(MessageIds.GDE_MSGT2076));
				}
				{
					this.addressAltitudeGainText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressAltitudeGainText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText9LData = new RowData();
					addressText9LData.width = textWidth;
					addressText9LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressAltitudeGainText.setLayoutData(addressText9LData);
					this.addressAltitudeGainText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAltitudeGain]);
					this.addressAltitudeGainText.setEditable(false);
					this.addressAltitudeGainText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressHightGainSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressHightGainSlider.setMinimum(sliderMinimum);
					this.addressHightGainSlider.setMaximum(sliderMaximum);
					this.addressHightGainSlider.setIncrement(sliderIncrement);
					RowData addressSlider9LData = new RowData();
					addressSlider9LData.width = sliderWidth;
					addressSlider9LData.height = 17;
					this.addressHightGainSlider.setLayoutData(addressSlider9LData);
					this.addressHightGainSlider.setSelection(this.configuration.mLinkAddressTripLength);
					this.addressHightGainSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressHightGainSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAltitudeGain = (byte) GPSLoggerSetupConfiguration2.this.addressHightGainSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressAltitudeGainText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAltitudeGain]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressVoltageRxLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressVoltageRxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel14LData = new RowData();
					addressLabel14LData.width = labelWidth;
					addressLabel14LData.height = 18;
					this.addressVoltageRxLabel.setLayoutData(addressLabel14LData);
					this.addressVoltageRxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2049));
				}
				{
					this.addressVoltageRxText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressVoltageRxText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText14LData = new RowData();
					addressText14LData.width = textWidth;
					addressText14LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressVoltageRxText.setLayoutData(addressText14LData);
					this.addressVoltageRxText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressVoltageRx]);
					this.addressVoltageRxText.setEditable(false);
					this.addressVoltageRxText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressVoltageRxSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressVoltageRxSlider.setMinimum(sliderMinimum);
					this.addressVoltageRxSlider.setMaximum(sliderMaximum);
					this.addressVoltageRxSlider.setIncrement(sliderIncrement);
					RowData addressSlider14LData = new RowData();
					addressSlider14LData.width = sliderWidth;
					addressSlider14LData.height = 17;
					this.addressVoltageRxSlider.setLayoutData(addressSlider14LData);
					this.addressVoltageRxSlider.setSelection(this.configuration.mLinkAddressTripLength);
					this.addressVoltageRxSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressVoltageRxSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressVoltageRx = (byte) GPSLoggerSetupConfiguration2.this.addressVoltageRxSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressVoltageRxText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressVoltageRx]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressEnlLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressEnlLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel10LData = new RowData();
					addressLabel10LData.width = labelWidth;
					addressLabel10LData.height = 18;
					this.addressEnlLabel.setLayoutData(addressLabel10LData);
					this.addressEnlLabel.setText(Messages.getString(MessageIds.GDE_MSGT2084));
				}
				{
					this.addressEnlText =new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressEnlText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText10LData = new RowData();
					addressText10LData.width = textWidth;
					addressText10LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressEnlText.setLayoutData(addressText10LData);
					this.addressEnlText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressENL]);
					this.addressEnlText.setEditable(false);
					this.addressEnlText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressEnlSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressEnlSlider.setMinimum(sliderMinimum);
					this.addressEnlSlider.setMaximum(sliderMaximum);
					this.addressEnlSlider.setIncrement(sliderIncrement);
					RowData addressSlider10LData = new RowData();
					addressSlider10LData.width = sliderWidth;
					addressSlider10LData.height = 17;
					this.addressEnlSlider.setLayoutData(addressSlider10LData);
					this.addressEnlSlider.setSelection(this.configuration.mLinkAddressENL);
					this.addressEnlSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressEnlSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressENL = (byte) GPSLoggerSetupConfiguration2.this.addressEnlSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressEnlText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressENL]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressAccXLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressAccXLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel11LData = new RowData();
					addressLabel11LData.width = labelWidth;
					addressLabel11LData.height = 18;
					this.addressAccXLabel.setLayoutData(addressLabel11LData);
					this.addressAccXLabel.setText(Messages.getString(MessageIds.GDE_MSGT2085));
				}
				{
					this.addressAccXText =new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressAccXText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText11LData = new RowData();
					addressText11LData.width = textWidth;
					addressText11LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressAccXText.setLayoutData(addressText11LData);
					this.addressAccXText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAccX]);
					this.addressAccXText.setEditable(false);
					this.addressAccXText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressAccXSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressAccXSlider.setMinimum(sliderMinimum);
					this.addressAccXSlider.setMaximum(sliderMaximum);
					this.addressAccXSlider.setIncrement(sliderIncrement);
					RowData addressSlider11LData = new RowData();
					addressSlider11LData.width = sliderWidth;
					addressSlider11LData.height = 17;
					this.addressAccXSlider.setLayoutData(addressSlider11LData);
					this.addressAccXSlider.setSelection(this.configuration.mLinkAddressAccX);
					this.addressAccXSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider11.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAccX = (byte) GPSLoggerSetupConfiguration2.this.addressAccXSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressAccXText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAccX]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressAccYLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressAccYLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel12LData = new RowData();
					addressLabel12LData.width = labelWidth;
					addressLabel12LData.height = 18;
					this.addressAccYLabel.setLayoutData(addressLabel12LData);
					this.addressAccYLabel.setText(Messages.getString(MessageIds.GDE_MSGT2086));
				}
				{
					this.addressAccYText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressAccYText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText12LData = new RowData();
					addressText12LData.width = textWidth;
					addressText12LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressAccYText.setLayoutData(addressText12LData);
					this.addressAccYText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAccY]);
					this.addressAccYText.setEditable(false);
					this.addressAccYText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressAccYSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressAccYSlider.setMinimum(sliderMinimum);
					this.addressAccYSlider.setMaximum(sliderMaximum);
					this.addressAccYSlider.setIncrement(sliderIncrement);
					RowData addressSlider12LData = new RowData();
					addressSlider12LData.width = sliderWidth;
					addressSlider12LData.height = 17;
					this.addressAccYSlider.setLayoutData(addressSlider12LData);
					this.addressAccYSlider.setSelection(this.configuration.mLinkAddressAccY);
					this.addressAccYSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressAccYSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAccY = (byte) GPSLoggerSetupConfiguration2.this.addressAccYSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressAccYText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAccY]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressAccZLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressAccZLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel13LData = new RowData();
					addressLabel13LData.width = labelWidth;
					addressLabel13LData.height = 18;
					this.addressAccZLabel.setLayoutData(addressLabel13LData);
					this.addressAccZLabel.setText(Messages.getString(MessageIds.GDE_MSGT2087));
				}
				{
					this.addressAccZText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressAccZText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText13LData = new RowData();
					addressText13LData.width = textWidth;
					addressText13LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressAccZText.setLayoutData(addressText13LData);
					this.addressAccZText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAccZ]);
					this.addressAccZText.setEditable(false);
					this.addressAccZText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressAccZSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressAccZSlider.setMinimum(sliderMinimum);
					this.addressAccZSlider.setMaximum(sliderMaximum);
					this.addressAccZSlider.setIncrement(sliderIncrement);
					RowData addressSlider13LData = new RowData();
					addressSlider13LData.width = sliderWidth;
					addressSlider13LData.height = 17;
					this.addressAccZSlider.setLayoutData(addressSlider13LData);
					this.addressAccZSlider.setSelection(this.configuration.mLinkAddressAccZ);
					this.addressAccZSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressAccZSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAccZ = (byte) GPSLoggerSetupConfiguration2.this.addressAccZSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressAccZText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAccZ]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressVarioLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressVarioLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel13LData = new RowData();
					addressLabel13LData.width = labelWidth;
					addressLabel13LData.height = 18;
					this.addressVarioLabel.setLayoutData(addressLabel13LData);
					this.addressVarioLabel.setText("Vario");
				}
				{
					this.addressVarioText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressVarioText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText13LData = new RowData();
					addressText13LData.width = textWidth;
					addressText13LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressVarioText.setLayoutData(addressText13LData);
					this.addressVarioText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAccZ]);
					this.addressVarioText.setEditable(false);
					this.addressVarioText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressVarioSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressVarioSlider.setMinimum(sliderMinimum);
					this.addressVarioSlider.setMaximum(sliderMaximum);
					this.addressVarioSlider.setIncrement(sliderIncrement);
					RowData addressSlider13LData = new RowData();
					addressSlider13LData.width = sliderWidth;
					addressSlider13LData.height = 17;
					this.addressVarioSlider.setLayoutData(addressSlider13LData);
					this.addressVarioSlider.setSelection(this.configuration.mLinkAddressVario);
					this.addressVarioSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressVarioSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressVario = (byte) GPSLoggerSetupConfiguration2.this.addressVarioSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressVarioText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressVario]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressAirSpeedLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressAirSpeedLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel13LData = new RowData();
					addressLabel13LData.width = labelWidth;
					addressLabel13LData.height = 18;
					this.addressAirSpeedLabel.setLayoutData(addressLabel13LData);
					this.addressAirSpeedLabel.setText("AirSpeed");
				}
				{
					this.addressAirSpeedText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressAirSpeedText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText13LData = new RowData();
					addressText13LData.width = textWidth;
					addressText13LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressAirSpeedText.setLayoutData(addressText13LData);
					this.addressAirSpeedText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAirSpeed]);
					this.addressAirSpeedText.setEditable(false);
					this.addressAirSpeedText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressAirSpeedSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressAirSpeedSlider.setMinimum(sliderMinimum);
					this.addressAirSpeedSlider.setMaximum(sliderMaximum);
					this.addressAirSpeedSlider.setIncrement(sliderIncrement);
					RowData addressSlider13LData = new RowData();
					addressSlider13LData.width = sliderWidth;
					addressSlider13LData.height = 17;
					this.addressAirSpeedSlider.setLayoutData(addressSlider13LData);
					this.addressAirSpeedSlider.setSelection(this.configuration.mLinkAddressAirSpeed);
					this.addressAirSpeedSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressAirSpeedSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAirSpeed = (byte) GPSLoggerSetupConfiguration2.this.addressAirSpeedSlider.getSelection();
							GPSLoggerSetupConfiguration2.this.addressAirSpeedText.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressAirSpeed]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
			}
			{
				this.jetiExGroup = new Group(this, SWT.NONE);
				GPSLoggerSetupConfiguration2.jetiExGroupStatic = this.jetiExGroup;
				RowLayout mLinkAddressesGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.jetiExGroup.setLayout(mLinkAddressesGroupLayout);
				FormData mLinkAddressesGroupLData = new FormData();
				mLinkAddressesGroupLData.top = new FormAttachment(0, 1000, 5);
				mLinkAddressesGroupLData.left = new FormAttachment(0, 1000, 15);
				mLinkAddressesGroupLData.width = 290;
				mLinkAddressesGroupLData.height = GDE.IS_MAC ? 440 : 445;
				this.jetiExGroup.setLayoutData(mLinkAddressesGroupLData);
				this.jetiExGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.jetiExGroup.setText("Jeti Duplex EX ");
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement2.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement2.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFFFB
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000004;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement3.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement3.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFFF7
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000008;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement4.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement4.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFFEF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000010;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement5.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement5.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFFDF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000020;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement6.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement6.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFFBF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000040;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement7.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement7.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFF7F
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000080;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement8.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement8.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFEFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000100;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement9.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement9.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFDFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000200;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement10.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement10.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFBFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000400;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement11.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement11.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFF7FF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000800;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement12.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement12.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFEFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00001000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement13.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement13.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFDFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00002000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement14.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement14.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFBFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00004000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement15.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement15.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFF7FFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00008000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement16.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement16.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFEFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00010000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement17.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement17.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFDFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00020000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement18.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement18.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFBFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00040000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement19.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement19.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFF7FFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00080000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement20.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement20.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFEFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00100000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement21 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement21.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement21.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement21.setText(this.jetiValueNames[21]);
					this.measurement21.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement21.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement21.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFDFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00200000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement22 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement22.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement22.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement22.setText(this.jetiValueNames[22]);
					this.measurement22.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement22.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement22.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFBFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00400000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement23 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement23.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement23.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement23.setText(this.jetiValueNames[23]);
					this.measurement23.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement23.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement23.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFF7FFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00800000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement24 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement24.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement24.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement24.setText(this.jetiValueNames[24]);
					this.measurement24.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement24.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement24.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFEFFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x01000000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement25 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement25.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement25.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement25.setText(this.jetiValueNames[25]);
					this.measurement25.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement25.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement25.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFDFFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x02000000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement26 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement26.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement26.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement26.setText(this.jetiValueNames[26]);
					this.measurement26.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement26.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement26.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFBFFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x04000000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement27 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement27.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement27.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement27.setText(this.jetiValueNames[27]);
					this.measurement27.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement27.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL = (short) (GPSLoggerSetupConfiguration2.this.measurement27.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL & 0xFFFE
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL + 0x0001);
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement28 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement28.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement28.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement28.setText(this.jetiValueNames[28]);
					this.measurement28.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement28.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL = (short) (GPSLoggerSetupConfiguration2.this.measurement28.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL & 0xFFFFD
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL + 0x0002);
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement29 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement29.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement29.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement29.setText(this.jetiValueNames[29]);
					this.measurement29.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement29.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL = (short) (GPSLoggerSetupConfiguration2.this.measurement29.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL & 0xFFFB
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL + 0x0004);
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement30 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement30.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement30.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement30.setText(this.jetiValueNames[30]);
					this.measurement30.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement30.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL = (short) (GPSLoggerSetupConfiguration2.this.measurement30.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL & 0xFFF7
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL + 0x0008);
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement31 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement31.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement31.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement31.setText(this.jetiValueNames[31]);
					this.measurement31.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement31.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL = (short) (GPSLoggerSetupConfiguration2.this.measurement31.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL & 0xFFEF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL + 0x0010);
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement32 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement32.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement32.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement32.setText(this.jetiValueNames[32]);
					this.measurement32.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement32.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL = (short) (GPSLoggerSetupConfiguration2.this.measurement32.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL & 0xFFDF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL + 0x0020);
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement33 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement33.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement33.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement33.setText(this.jetiValueNames[33]);
					this.measurement33.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement33.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL = (short) (GPSLoggerSetupConfiguration2.this.measurement33.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL & 0xFFBF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL + 0x0040);
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement34 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement34.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement34.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement34.setText(this.jetiValueNames[34]);
					this.measurement34.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement34.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL = (short) (GPSLoggerSetupConfiguration2.this.measurement34.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL & 0xFF7F
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL + 0x0080);
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement35 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement35.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement35.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement35.setText(this.jetiValueNames[35]);
					this.measurement35.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement35.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL = (short) (GPSLoggerSetupConfiguration2.this.measurement35.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL & 0xFEFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL + 0x0100);
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement36 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement36.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement36.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement36.setText(this.jetiValueNames[36]);
					this.measurement36.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement36.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL = (short) (GPSLoggerSetupConfiguration2.this.measurement36.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL & 0xFDFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL + 0x0200);
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement37 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement37.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement37.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement37.setText(this.jetiValueNames[37]);
					this.measurement37.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement37.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL = (short) (GPSLoggerSetupConfiguration2.this.measurement30.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL & 0xFBFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask_UL + 0x0400);
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
					this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
				}
			}
			{
				this.unilogTelemtryAlarmsGroup = new Group(this, SWT.NONE);
				GPSLoggerSetupConfiguration2.unilogTelemtryAlarmsGroupStatic = this.unilogTelemtryAlarmsGroup;
				RowLayout unilogTelemtryAlarmsGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.unilogTelemtryAlarmsGroup.setLayout(unilogTelemtryAlarmsGroupLayout);
				FormData unilogTelemtryAlarmsGroupLData = new FormData();
				unilogTelemtryAlarmsGroupLData.width = 290;
				unilogTelemtryAlarmsGroupLData.height = 143;
				unilogTelemtryAlarmsGroupLData.top = new FormAttachment(0, 1000, 470);
				unilogTelemtryAlarmsGroupLData.left = new FormAttachment(0, 1000, 15);
				this.unilogTelemtryAlarmsGroup.setLayoutData(unilogTelemtryAlarmsGroupLData);
				this.unilogTelemtryAlarmsGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.unilogTelemtryAlarmsGroup.setText(Messages.getString(MessageIds.GDE_MSGT2065));
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = fillerWidth;
					fillerCompositeRA1LData.height = fillerHeight;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.currentButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData currentButtonLData = new RowData();
					currentButtonLData.width = buttonWidth;
					currentButtonLData.height = 18;
					this.currentButton.setLayoutData(currentButtonLData);
					this.currentButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentButton.setText(Messages.getString(MessageIds.GDE_MSGT2066));
					this.currentButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CURRENT_UL) > 0);
					this.currentButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "currentButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration2.this.currentButton.getSelection()) {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_CURRENT_UL);
							}
							else {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_CURRENT_UL);
							}
							GPSLoggerSetupConfiguration2.this.currentButton.setSelection((GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CURRENT_UL) > 0);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData currentCComboLData = new RowData();
					currentCComboLData.width = comboWidth;
					currentCComboLData.height = 17;
					this.currentCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.currentCombo.setLayoutData(currentCComboLData);
					this.currentCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentCombo.setItems(this.currentValues);
					this.currentCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.currentUlAlarm)); //$NON-NLS-1$
					this.currentCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "currentCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration2.this.currentCombo.getText().trim());
							GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm < 1 ? 1
									: GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm > 400 ? 400 : GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm; 
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.currentCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(java.util.logging.Level.FINEST, "currentCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.currentCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(java.util.logging.Level.FINEST, "currentCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration2.this.currentCombo.getText().trim());
								GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm < 1 ? 1
										: GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm > 400 ? 400 : GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm; 
								GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.currentLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER);
					RowData currentLabelLData = new RowData();
					currentLabelLData.width = 50;
					currentLabelLData.height = 18;
					this.currentLabel.setLayoutData(currentLabelLData);
					this.currentLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentLabel.setText(Messages.getString(MessageIds.GDE_MSGT2051));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = fillerWidth;
					fillerCompositeRA1LData.height = fillerHeight;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.voltageStartButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageStartButtonLData = new RowData();
					voltageStartButtonLData.width = buttonWidth;
					voltageStartButtonLData.height = 18;
					this.voltageStartButton.setLayoutData(voltageStartButtonLData);
					this.voltageStartButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageStartButton.setText(Messages.getString(MessageIds.GDE_MSGT2052));
					this.voltageStartButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_START_UL) > 0);
					this.voltageStartButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageStartButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration2.this.voltageStartButton.getSelection()) {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_VOLTAGE_START_UL);
							}
							else {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_VOLTAGE_START_UL);
							}
							GPSLoggerSetupConfiguration2.this.voltageStartButton.setSelection((GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_START_UL) > 0);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData voltageStartCComboLData = new RowData();
					voltageStartCComboLData.width = comboWidth;
					voltageStartCComboLData.height = 17;
					this.voltageStartCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.voltageStartCombo.setLayoutData(voltageStartCComboLData);
					this.voltageStartCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageStartCombo.setItems(this.voltageStartValues);
					this.voltageStartCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageStartUlAlarm / 10.0)); //$NON-NLS-1$
					this.voltageStartCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageStartCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm = (short) (Double.parseDouble(GPSLoggerSetupConfiguration2.this.voltageStartCombo.getText().trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 10);
							GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm < 10 ? 10 
									: GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm > 600 ? 600 : GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm; 
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.voltageStartCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "voltageStartCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.voltageStartCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "voltageStartCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm = (short) (Double.parseDouble(GPSLoggerSetupConfiguration2.this.voltageStartCombo.getText().trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 10);
								GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm < 10 ? 10 
										: GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm > 600 ? 600 : GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm; 
								GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.voltageStartLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData voltageStartLabelLData = new RowData();
					voltageStartLabelLData.width = 50;
					voltageStartLabelLData.height = 18;
					this.voltageStartLabel.setLayoutData(voltageStartLabelLData);
					this.voltageStartLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageStartLabel.setText(Messages.getString(MessageIds.GDE_MSGT2053));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = fillerWidth;
					fillerCompositeRA1LData.height = fillerHeight;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.voltageButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageButtonLData = new RowData();
					voltageButtonLData.width = buttonWidth;
					voltageButtonLData.height = 18;
					this.voltageButton.setLayoutData(voltageButtonLData);
					this.voltageButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageButton.setText(Messages.getString(MessageIds.GDE_MSGT2054));
					this.voltageButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_UL) > 0);
					this.voltageButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration2.this.voltageButton.getSelection()) {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_VOLTAGE_UL);
							}
							else {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_VOLTAGE_UL);
							}
							GPSLoggerSetupConfiguration2.this.voltageButton.setSelection((GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_UL) > 0);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData voltageCComboLData = new RowData();
					voltageCComboLData.width = comboWidth;
					voltageCComboLData.height = 17;
					this.voltageCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.voltageCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageCombo.setLayoutData(voltageCComboLData);
					this.voltageCombo.setItems(this.voltageStartValues);
					this.voltageCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageUlAlarm / 10.0)); //$NON-NLS-1$
					this.voltageCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm = (short) (Double.parseDouble(GPSLoggerSetupConfiguration2.this.voltageCombo.getText().trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 10);
							GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm < 10 ? 10 
									: GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm > 600 ? 600 : GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm; 
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.voltageCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "voltageCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.voltageCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "voltageCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm = (short) (Double.parseDouble(GPSLoggerSetupConfiguration2.this.voltageCombo.getText().trim().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT)) * 10);
								GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm < 10 ? 10 
										: GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm > 600 ? 600 : GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm; 
								GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					RowData voltageLabelLData = new RowData();
					voltageLabelLData.width = 50;
					voltageLabelLData.height = 18;
					this.voltageLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					this.voltageLabel.setLayoutData(voltageLabelLData);
					this.voltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageLabel.setText(Messages.getString(MessageIds.GDE_MSGT2055));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = fillerWidth;
					fillerCompositeRA1LData.height = fillerHeight;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.capacityButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageRxULButtonLData = new RowData();
					voltageRxULButtonLData.width = buttonWidth;
					voltageRxULButtonLData.height = 18;
					this.capacityButton.setLayoutData(voltageRxULButtonLData);
					this.capacityButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityButton.setText(Messages.getString(MessageIds.GDE_MSGT2056));
					this.capacityButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CAPACITY_UL) > 0);
					this.capacityButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageRxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration2.this.capacityButton.getSelection()) {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_CAPACITY_UL);
							}
							else {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = (short) (GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_CAPACITY_UL);
							}
							GPSLoggerSetupConfiguration2.this.capacityButton.setSelection((GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CAPACITY_UL) > 0);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData CCombo1LData = new RowData();
					CCombo1LData.width = comboWidth;
					CCombo1LData.height = 17;
					this.capacityCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.capacityCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityCombo.setLayoutData(CCombo1LData);
					this.capacityCombo.setItems(this.capacityValues);
					this.capacityCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.capacityUlAlarm)); //$NON-NLS-1$
					this.capacityCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "capacityCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration2.this.capacityCombo.getText().trim());
							GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm < 100 ? 100 
									: GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm > 30000 ? 30000 : GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm; 
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.capacityCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "capacityCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.capacityCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "capacityCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration2.this.capacityCombo.getText().trim());
								GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm < 100 ? 100 
										: GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm > 30000 ? 30000 : GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm; 
								GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// illegal char
							}
						}
					});
				}
				{
					this.capacityLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData voltageRxULLabelLData = new RowData();
					voltageRxULLabelLData.width = 50;
					voltageRxULLabelLData.height = 18;
					this.capacityLabel.setLayoutData(voltageRxULLabelLData);
					this.capacityLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityLabel.setText(Messages.getString(MessageIds.GDE_MSGT2057));
				}
				{
					this.jetiUniLogAlarmsLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER);
					this.jetiUniLogAlarmsLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData jetiUniLogAlarmsLData = new RowData();
					jetiUniLogAlarmsLData.width = 285;
					jetiUniLogAlarmsLData.height = 30;
					this.jetiUniLogAlarmsLabel.setLayoutData(jetiUniLogAlarmsLData);
					this.jetiUniLogAlarmsLabel.setText(Messages.getString(MessageIds.GDE_MSGT2089));
				}
			}
			{
				this.spektrumAdapterGroup = new Group(this, SWT.NONE);
				GPSLoggerSetupConfiguration2.spektrumAdapterGroupStatic = this.spektrumAdapterGroup;
				RowLayout mLinkAddressesGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.spektrumAdapterGroup.setLayout(mLinkAddressesGroupLayout);
				FormData mLinkAddressesGroupLData = new FormData();
				mLinkAddressesGroupLData.top = new FormAttachment(0, 1000, 5);
				mLinkAddressesGroupLData.left = new FormAttachment(0, 1000, 15);
				mLinkAddressesGroupLData.width = 290;
				mLinkAddressesGroupLData.height = 165;
				this.spektrumAdapterGroup.setLayoutData(mLinkAddressesGroupLData);
				this.spektrumAdapterGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.spektrumAdapterGroup.setText(Messages.getString(MessageIds.GDE_MSGT2094));
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
					this.spektrumAddressLabel.setText(Messages.getString(MessageIds.GDE_MSGT2095));
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
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "spektrumAddrssCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.spektrumNumber = (byte) Integer.parseInt(GPSLoggerSetupConfiguration2.this.spektrumAddressCombo.getText().trim());
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
					fillerCompositeRA1LData.height = 18;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.spektrumGPSSensor = new Button(this.spektrumAdapterGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 280 - this.fillerWidth;
					analogAlarm1DirectionButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					this.spektrumGPSSensor.setLayoutData(analogAlarm1DirectionButtonLData);
					this.spektrumGPSSensor.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.spektrumGPSSensor.setText(Messages.getString(MessageIds.GDE_MSGT2096));
					this.spektrumGPSSensor.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "spektrumGPSSensor.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors = (byte) (GPSLoggerSetupConfiguration2.this.spektrumGPSSensor.getSelection() 
									? GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors & 0xFE
									: GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors | 0x01);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.fillerComposite = new Composite(this.spektrumAdapterGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 18;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.spektrumVarioSensor = new Button(this.spektrumAdapterGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 280 - this.fillerWidth;
					analogAlarm1DirectionButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					this.spektrumVarioSensor.setLayoutData(analogAlarm1DirectionButtonLData);
					this.spektrumVarioSensor.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.spektrumVarioSensor.setText(Messages.getString(MessageIds.GDE_MSGT2097));
					this.spektrumVarioSensor.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "spektrumVarioSensor.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors = (byte) (GPSLoggerSetupConfiguration2.this.spektrumVarioSensor.getSelection() 
									? GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors & 0xFD
									: GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors | 0x02);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.fillerComposite = new Composite(this.spektrumAdapterGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 18;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.spektrumGforceSensor = new Button(this.spektrumAdapterGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 280 - this.fillerWidth;
					analogAlarm1DirectionButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					this.spektrumGforceSensor.setLayoutData(analogAlarm1DirectionButtonLData);
					this.spektrumGforceSensor.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.spektrumGforceSensor.setText(Messages.getString(MessageIds.GDE_MSGT2098));
					this.spektrumGforceSensor.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "spektrumGforceSensor.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors = (byte) (GPSLoggerSetupConfiguration2.this.spektrumGforceSensor.getSelection() 
									? GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors & 0xFB
									: GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors | 0x04);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.fillerComposite = new Composite(this.spektrumAdapterGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 18;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.spektrumUserSensor = new Button(this.spektrumAdapterGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 280 - this.fillerWidth;
					analogAlarm1DirectionButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					this.spektrumUserSensor.setLayoutData(analogAlarm1DirectionButtonLData);
					this.spektrumUserSensor.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.spektrumUserSensor.setText(Messages.getString(MessageIds.GDE_MSGT2099));
					this.spektrumUserSensor.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "spektrumUserSensor.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors = (byte) (GPSLoggerSetupConfiguration2.this.spektrumUserSensor.getSelection() 
									? GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors & 0xF7
									: GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors | 0x08);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.fillerComposite = new Composite(this.spektrumAdapterGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 18;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.spektrumSpeedSensor = new Button(this.spektrumAdapterGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 280 - this.fillerWidth;
					analogAlarm1DirectionButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					this.spektrumSpeedSensor.setLayoutData(analogAlarm1DirectionButtonLData);
					this.spektrumSpeedSensor.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.spektrumSpeedSensor.setText(Messages.getString(MessageIds.GDE_MSGT2013));
					this.spektrumSpeedSensor.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "spektrumSpeedSensor.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors = (byte) (GPSLoggerSetupConfiguration2.this.spektrumSpeedSensor.getSelection() 
									? GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors & 0xEF
									: GPSLoggerSetupConfiguration2.this.configuration.spektrumSensors | 0x10);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
			}
			{
				this.fixGpsStartPositionGroup = new Group(this, SWT.NONE);
				GPSLoggerSetupConfiguration2.fixGpsStartPositionGroupStatic = this.fixGpsStartPositionGroup;
				this.fixGpsStartPositionGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
				FormData fixGpsStartPositionGroupLData = new FormData();
				fixGpsStartPositionGroupLData.width = 290;
				fixGpsStartPositionGroupLData.height = 143;
				fixGpsStartPositionGroupLData.top = new FormAttachment(0, 1000, 470);
				fixGpsStartPositionGroupLData.left = new FormAttachment(0, 1000, 15);
				this.fixGpsStartPositionGroup.setLayoutData(fixGpsStartPositionGroupLData);
				this.fixGpsStartPositionGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.fixGpsStartPositionGroup.setText(Messages.getString(MessageIds.GDE_MSGT2090));
				{
					this.fillerComposite = new Composite(this.fixGpsStartPositionGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = fillerWidth;
					fillerCompositeRA1LData.height = fillerHeight;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.useFixGpsStartPositionButton = new Button(this.fixGpsStartPositionGroup, SWT.CHECK | SWT.LEFT);
					RowData currentButtonLData = new RowData();
					currentButtonLData.width = 250;
					currentButtonLData.height = 18;
					this.useFixGpsStartPositionButton.setLayoutData(currentButtonLData);
					this.useFixGpsStartPositionButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.useFixGpsStartPositionButton.setText(Messages.getString(MessageIds.GDE_MSGT2091));
					this.useFixGpsStartPositionButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CURRENT_UL) > 0);
					this.useFixGpsStartPositionButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "useFixGpsStartPositionButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration2.this.useFixGpsStartPositionButton.getSelection()) {
								GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude = GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude | 0x80000000;
							}
							else {
								GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude = GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude ^ 0x80000000;
							}
							System.out.println(
							String.format("%s %s %s %s", StringHelper.printBinary((byte) (GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude >> 24 & 0xFF), false),
									StringHelper.printBinary((byte) (GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude >> 16 & 0xFF), false),
									StringHelper.printBinary((byte) (GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude >> 8 & 0xFF), false),
									StringHelper.printBinary((byte) (GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude & 0xFF), false))
									);
							System.out.println((GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude >> 24 & 0x80));
							GPSLoggerSetupConfiguration2.this.useFixGpsStartPositionButton.setSelection((GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude >> 24 & 0x80) > 0);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.fixLatitudeLabel = new CLabel(this.fixGpsStartPositionGroup, SWT.RIGHT);
					this.fixLatitudeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1LData = new RowData();
					addressLabel1LData.width = labelWidth;
					addressLabel1LData.height = 18;
					this.fixLatitudeLabel.setLayoutData(addressLabel1LData);
					this.fixLatitudeLabel.setText(Messages.getString(MessageIds.GDE_MSGT2083).split(GDE.STRING_COMMA)[3].split(GDE.STRING_BLANK)[1]);
				}
				{
					this.fixLatitudeText = new Text(this.fixGpsStartPositionGroup, SWT.RIGHT | SWT.BORDER);
					this.fixLatitudeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText1LData = new RowData();
					addressText1LData.width = GDE.IS_LINUX ? 80 : 90;
					addressText1LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.fixLatitudeText.setLayoutData(addressText1LData);
					this.fixLatitudeText.setText("0000.0000 N");
					this.fixLatitudeText.setEditable(true);
					this.fixLatitudeText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.fixLatitudeText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							GPSLoggerSetupConfiguration2.log.log(Level.FINEST, "fixHeightText.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude = (int) (Double.parseDouble(GPSLoggerSetupConfiguration2.this.fixLatitudeText.getText()
										.substring(1, GPSLoggerSetupConfiguration2.this.fixLatitudeText.getText().length()-2).trim().replace(GDE.STRING_COMMA, GDE.STRING_EMPTY).replace(GDE.STRING_DOT, GDE.STRING_EMPTY)));
								if (GPSLoggerSetupConfiguration2.this.fixLatitudeText.getText().contains("S")) {
									GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude = GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude | 0x40000000;
								}
								else { //GPSLoggerSetupConfiguration2.this.fixLatitudeText.getText().contains("N"))
									GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude = GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude ^ 0x40000000;
								}
								double tempLatitudeValue = ((GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude & 0x3FFFFFFF) / 10000.0);
								char tempLatDir = (GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude >> 24 & 0x40) > 0 ? 'S' : 'N';
								GPSLoggerSetupConfiguration2.this.fixLatitudeText.setText(String.format(Locale.ENGLISH, "%09.4f %c", tempLatitudeValue, tempLatDir)); //$NON-NLS-1$
								GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore -
							}
						}
					});
					this.fixLatitudeText.addFocusListener(new FocusAdapter() {
						@Override
						public void focusLost(FocusEvent evt) {
							double tempLatitudeValue = ((GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude & 0x3FFFFFFF) / 10000.0);
							char tempLatDir = (GPSLoggerSetupConfiguration2.this.configuration.fixPositionLatitude >> 24 & 0x40) > 0 ? 'S' : 'N';
							GPSLoggerSetupConfiguration2.this.fixLatitudeText.setText(String.format(Locale.ENGLISH, "%09.4f %c", tempLatitudeValue, tempLatDir)); //$NON-NLS-1$
						}
					});
				}
				{
					this.fixLatitudeUnitLabel = new CLabel(this.fixGpsStartPositionGroup, SWT.LEFT);
					this.fixLatitudeUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1LData = new RowData();
					addressLabel1LData.width = 70;
					addressLabel1LData.height = 18;
					this.fixLatitudeUnitLabel.setLayoutData(addressLabel1LData);
					this.fixLatitudeUnitLabel.setText("[° ']   S|N");
				}
				{
					this.fixLongitudeLabel = new CLabel(this.fixGpsStartPositionGroup, SWT.RIGHT);
					this.fixLongitudeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1LData = new RowData();
					addressLabel1LData.width = labelWidth;
					addressLabel1LData.height = 18;
					this.fixLongitudeLabel.setLayoutData(addressLabel1LData);
					this.fixLongitudeLabel.setText(Messages.getString(MessageIds.GDE_MSGT2083).split(GDE.STRING_COMMA)[4].split(GDE.STRING_BLANK)[1]);
				}
				{
					this.fixLongitudeText = new Text(this.fixGpsStartPositionGroup, SWT.RIGHT | SWT.BORDER);
					this.fixLongitudeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText1LData = new RowData();
					addressText1LData.width = GDE.IS_LINUX ? 80 : 90;
					addressText1LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.fixLongitudeText.setLayoutData(addressText1LData);
					this.fixLongitudeText.setText("00000.0000 E");
					this.fixLongitudeText.setEditable(true);
					this.fixLongitudeText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.fixLongitudeText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							GPSLoggerSetupConfiguration2.log.log(Level.FINEST, "fixHeightText.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration2.this.configuration.fixPositionLongitude = (int) (Double.parseDouble(GPSLoggerSetupConfiguration2.this.fixLongitudeText.getText()
										.substring(1, GPSLoggerSetupConfiguration2.this.fixLongitudeText.getText().length()-2).trim().replace(GDE.STRING_COMMA, GDE.STRING_EMPTY).replace(GDE.STRING_DOT, GDE.STRING_EMPTY)));
								if (GPSLoggerSetupConfiguration2.this.fixLongitudeText.getText().contains("W")) {
									GPSLoggerSetupConfiguration2.this.configuration.fixPositionLongitude = GPSLoggerSetupConfiguration2.this.configuration.fixPositionLongitude | 0x40000000;
								}
								else { //GPSLoggerSetupConfiguration2.this.fixLongitudeText.getText().contains("E"))
									GPSLoggerSetupConfiguration2.this.configuration.fixPositionLongitude = GPSLoggerSetupConfiguration2.this.configuration.fixPositionLongitude ^ 0x40000000;
								}
								double tempLongitudeValue = ((GPSLoggerSetupConfiguration2.this.configuration.fixPositionLongitude & 0x3FFFFFFF) / 10000.0);
								char tempLonDir = (GPSLoggerSetupConfiguration2.this.configuration.fixPositionLongitude >> 24 & 0x40) > 0 ? 'W' : 'E';
								GPSLoggerSetupConfiguration2.this.fixLongitudeText.setText(String.format(Locale.ENGLISH, "%010.4f %c", tempLongitudeValue, tempLonDir)); //$NON-NLS-1$
								GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore -
							}
						}
					});
					this.fixLongitudeText.addFocusListener(new FocusAdapter() {
						@Override
						public void focusLost(FocusEvent evt) {
							double tempLongitudeValue = ((GPSLoggerSetupConfiguration2.this.configuration.fixPositionLongitude & 0x3FFFFFFF) / 10000.0);
							char tempLonDir = (GPSLoggerSetupConfiguration2.this.configuration.fixPositionLongitude >> 24 & 0x40) > 0 ? 'W' : 'O';
							GPSLoggerSetupConfiguration2.this.fixLongitudeText.setText(String.format(Locale.ENGLISH, "%010.4f %c", tempLongitudeValue, tempLonDir)); //$NON-NLS-1$
						}
					});
				}
				{
					this.fixLongitudeUnitLabel = new CLabel(this.fixGpsStartPositionGroup, SWT.LEFT);
					this.fixLongitudeUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1LData = new RowData();
					addressLabel1LData.width = 70;
					addressLabel1LData.height = 18;
					this.fixLongitudeUnitLabel.setLayoutData(addressLabel1LData);
					this.fixLongitudeUnitLabel.setText("[° ']   W|O");
				}
				{
					this.fixHeightLabel = new CLabel(this.fixGpsStartPositionGroup, SWT.RIGHT);
					this.fixHeightLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1LData = new RowData();
					addressLabel1LData.width = labelWidth;
					addressLabel1LData.height = 18;
					this.fixHeightLabel.setLayoutData(addressLabel1LData);
					this.fixHeightLabel.setText(Messages.getString(MessageIds.GDE_MSGT2083).split(GDE.STRING_COMMA)[7].substring(3));
				}
				{
					this.fixHeightText = new Text(this.fixGpsStartPositionGroup, SWT.RIGHT | SWT.BORDER);
					this.fixHeightText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText1LData = new RowData();
					addressText1LData.width = GDE.IS_LINUX ? 80 : 90;
					addressText1LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.fixHeightText.setLayoutData(addressText1LData);
					this.fixHeightText.setText("000");
					this.fixHeightText.setEditable(true);
					this.fixHeightText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.fixHeightText.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent verifyevent) {
							GPSLoggerSetupConfiguration2.log.log(Level.FINEST, "fixHeightText.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.fixHeightText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							GPSLoggerSetupConfiguration2.log.log(Level.FINEST, "fixHeightText.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration2.this.configuration.fixPositionAltitude = (short) (Integer.parseInt(GPSLoggerSetupConfiguration2.this.fixHeightText.getText().trim()));
								GPSLoggerSetupConfiguration2.this.fixHeightText.setText(String.format(Locale.ENGLISH, "%d", GPSLoggerSetupConfiguration2.this.configuration.fixPositionAltitude)); //$NON-NLS-1$
								GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore -
							}
						}
					});
					this.fixHeightText.addFocusListener(new FocusAdapter() {
						@Override
						public void focusLost(FocusEvent evt) {
							GPSLoggerSetupConfiguration2.this.fixHeightText.setText(String.format(Locale.ENGLISH, "%d", GPSLoggerSetupConfiguration2.this.configuration.fixPositionAltitude)); //$NON-NLS-1$
						}
					});
				}
				{
					this.fixHeightUnitLabel = new CLabel(this.fixGpsStartPositionGroup, SWT.LEFT);
					this.fixHeightUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1LData = new RowData();
					addressLabel1LData.width = 70;
					addressLabel1LData.height = 18;
					this.fixHeightUnitLabel.setLayoutData(addressLabel1LData);
					this.fixHeightUnitLabel.setText("[m]");
				}
			}
			this.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
