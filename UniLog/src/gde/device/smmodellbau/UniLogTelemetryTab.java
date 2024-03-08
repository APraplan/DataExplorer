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
import gde.data.Channels;
import gde.device.MeasurementType;
import gde.device.smmodellbau.unilog.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

/**
 * panel tab describing and configuring telemetry down link 
 * @param parent
 * @param useDevice
 */
public class UniLogTelemetryTab extends org.eclipse.swt.widgets.Composite {
	final static Logger			log	= Logger.getLogger(UniLogTelemetryTab.class.getName());

	{
		SWTResourceManager.registerResourceUser(this);
	}

	Button									readTelemetryConfigurationButton;
	Text										headerLabel;
	Button									writeTelemtryConfigurationButton;

	Group										alarmGroup;
	Button									alarmButton1, alarmButton2, alarmButton3, alarmButton4, alarmButton5;
	Text										alarmText1, alarmText2, alarmText3, alarmText4, alarmText5;
	Slider									alarmSlider1, alarmSlider2, alarmSlider3, alarmSlider4, alarmSlider5;
	CLabel									alarmLabel1, alarmLabel2, alarmLabel3, alarmLabel4, alarmLabel5;
	boolean									isAlarmButton1	= false, isAlarmButton2 = true, isAlarmButton3 = false, isAlarmButton4 = false, isAlarmButton5 = true;
	int											alarmValue1			= 60, alarmValue4 = 2600, alarmValue5 = 300;
	double									alarmValue2			= 14.0, alarmValue3 = 12.0;
	String									voltageUnit, voltageStartUnit, currentUnit, capacityUnit, heightUnit;

	Group										mLinkAddressesGroup;
	CLabel									addressLabel1, addressLabel2, addressLabel3, addressLabel4, addressLabel5;
	Text										addressText1, addressText2, addressText3, addressText4, addressText5;
	Slider									addressSlider1, addressSlider2, addressSlider3, addressSlider4, addressSlider5;
	int											addressValue1		= 3, addressValue2 = 2, addressValue3 = 5, addressValue4 = 4, addressValue5 = 6;
	String									addressLabelName1, addressLabelName2, addressLabelName3, addressLabelName4, addressLabelName5;

	final UniLog						device;					// get device specific things, get serial port, ...
	final UniLogDialog			dialog;
	final UniLogSerialPort	serialPort;			// open/close port execute getData()....
	final DataExplorer			application;
	final Channels					channels;

	/**
	 * panel tab describing the telemetry configuration
	 * @param parent
	 * @param useDevice
	 */
	public UniLogTelemetryTab(CTabFolder parent, UniLog useDevice) {
		super(parent, SWT.NONE);
		this.device = useDevice;
		this.dialog = useDevice.getDialog();
		this.serialPort = this.device.getCommunicationPort();
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		initGUI();
	}

	private void initGUI() {
		try {
			FormLayout thisLayout = new FormLayout();
			this.setLayout(thisLayout);
			//this.setSize(629, 340);
			
			MeasurementType measurement = this.device.getMeasurement(1, 1); // 0=Voltage
			this.voltageUnit = measurement.getUnit();
			this.voltageStartUnit = measurement.getUnit() + "start"; //$NON-NLS-1$ 
			this.addressLabelName2 = measurement.getName();
			measurement = this.device.getMeasurement(1, 2); // 2=current
			this.currentUnit = measurement.getUnit();
			this.addressLabelName1 = measurement.getName();
			measurement = this.device.getMeasurement(1, 3); // 3=charge/capacity
			this.capacityUnit = measurement.getUnit();
			this.addressLabelName4 = measurement.getName();
			measurement = this.device.getMeasurement(1, 9); // 9=height
			this.heightUnit = measurement.getUnit();
			this.addressLabelName5 = measurement.getName();
			measurement = this.device.getMeasurement(1, 7); //7=revolution
			this.addressLabelName3 = measurement.getName();

			{
				this.headerLabel = new Text(this, SWT.CENTER | SWT.READ_ONLY | SWT.WRAP);
				FormData headerLabelLData = new FormData();
				headerLabelLData.left = new FormAttachment(0, 1000, 50);
				headerLabelLData.top = new FormAttachment(0, 1000, 10);
				headerLabelLData.right = new FormAttachment(1000, 1000, -50);
				headerLabelLData.bottom = new FormAttachment(1000, 1000, -295);
				this.headerLabel.setLayoutData(headerLabelLData);
				this.headerLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.headerLabel.setText(Messages.getString(MessageIds.GDE_MSGT1386));
				this.headerLabel.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
			}
			{
				this.readTelemetryConfigurationButton = new Button(this, SWT.PUSH | SWT.CENTER);
				FormData readTelemetryConfigurationButtonLData = new FormData();
				readTelemetryConfigurationButtonLData.left = new FormAttachment(0, 1000, 100);
				readTelemetryConfigurationButtonLData.top = new FormAttachment(0, 1000, 56);
				readTelemetryConfigurationButtonLData.right = new FormAttachment(1000, 1000, -100);
				readTelemetryConfigurationButtonLData.bottom = new FormAttachment(1000, 1000, -244);
				readTelemetryConfigurationButtonLData.width = 429;
				readTelemetryConfigurationButtonLData.height = 40;
				this.readTelemetryConfigurationButton.setLayoutData(readTelemetryConfigurationButtonLData);
				this.readTelemetryConfigurationButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.readTelemetryConfigurationButton.setText(Messages.getString(MessageIds.GDE_MSGT1387));
				this.readTelemetryConfigurationButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "readTelemetryConfigurationButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						try {
							updateTelemetryConfigurationValues(UniLogTelemetryTab.this.serialPort.readTelemetryConfiguration());
						}
						catch (Exception e) {
							UniLogTelemetryTab.this.application.openMessageDialog(UniLogTelemetryTab.this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0029, new Object[] {
									e.getClass().getSimpleName(), e.getMessage() }));
						}
					}
				});
			}
			{
				this.alarmGroup = new Group(this, SWT.NONE);
				RowLayout alarmGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.alarmGroup.setLayout(alarmGroupLayout);
				FormData alarmGroupLData = new FormData();
				alarmGroupLData.left = new FormAttachment(0, 1000, 10);
				alarmGroupLData.top = new FormAttachment(0, 1000, 105);
				alarmGroupLData.width = 360;
				alarmGroupLData.height = 131;
				alarmGroupLData.right = new FormAttachment(1000, 1000, -255);
				alarmGroupLData.bottom = new FormAttachment(1000, 1000, -75);
				this.alarmGroup.setLayoutData(alarmGroupLData);
				this.alarmGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.alarmGroup.setText(Messages.getString(MessageIds.GDE_MSGT1394));
				{
					this.alarmButton1 = new Button(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData alarmButton1LData = new RowData();
					alarmButton1LData.width = 140;
					alarmButton1LData.height = 22;
					this.alarmButton1.setLayoutData(alarmButton1LData);
					this.alarmButton1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmButton1.setText(Messages.getString(MessageIds.GDE_MSGT1396));
					this.alarmButton1.setSelection(this.isAlarmButton1);
					this.alarmButton1.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "alarmButton1.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLogTelemetryTab.this.isAlarmButton1 = UniLogTelemetryTab.this.alarmButton1.getSelection();
							UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					this.alarmText1 = new Text(this.alarmGroup, SWT.RIGHT | SWT.READ_ONLY | SWT.BORDER);
					RowData alarmText1LData = new RowData();
					alarmText1LData.width = 40;
					alarmText1LData.height = 14;
					this.alarmText1.setLayoutData(alarmText1LData);
					this.alarmText1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmText1.setText(GDE.STRING_EMPTY + this.alarmValue1);
				}
				{
					RowData alarmSlider1LData = new RowData();
					alarmSlider1LData.width = 100;
					alarmSlider1LData.height = 18;
					this.alarmSlider1 = new Slider(this.alarmGroup, SWT.NONE);
					this.alarmSlider1.setLayoutData(alarmSlider1LData);
					this.alarmSlider1.setMinimum(1);
					this.alarmSlider1.setMaximum(410);
					this.alarmSlider1.setIncrement(1);
					this.alarmSlider1.setSelection(this.alarmValue1);
					this.alarmSlider1.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "alarmSlider1.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLogTelemetryTab.this.alarmValue1 = UniLogTelemetryTab.this.alarmSlider1.getSelection();
							UniLogTelemetryTab.this.alarmText1.setText(GDE.STRING_EMPTY + UniLogTelemetryTab.this.alarmValue1);
							UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					this.alarmLabel1 = new CLabel(this.alarmGroup, SWT.NONE);
					RowData alarmLabel1LData = new RowData();
					alarmLabel1LData.width = 40;
					alarmLabel1LData.height = 22;
					this.alarmLabel1.setLayoutData(alarmLabel1LData);
					this.alarmLabel1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmLabel1.setText(currentUnit);
				}
				{
					this.alarmButton2 = new Button(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData alarmButton2LData = new RowData();
					alarmButton2LData.width = 140;
					alarmButton2LData.height = 22;
					this.alarmButton2.setLayoutData(alarmButton2LData);
					this.alarmButton2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmButton2.setText(Messages.getString(MessageIds.GDE_MSGT1389));
					this.alarmButton2.setSelection(this.isAlarmButton2);
					this.alarmButton2.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "alarmButton2.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLogTelemetryTab.this.isAlarmButton2 = UniLogTelemetryTab.this.alarmButton2.getSelection();
							UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					this.alarmText2 = new Text(this.alarmGroup, SWT.RIGHT | SWT.READ_ONLY | SWT.BORDER);
					RowData alarmText2LData = new RowData();
					alarmText2LData.width = 40;
					alarmText2LData.height = 14;
					this.alarmText2.setLayoutData(alarmText2LData);
					this.alarmText2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmText2.setText(String.format("%.1f", this.alarmValue2)); //$NON-NLS-1$
				}
				{
					RowData alarmSlider2LData = new RowData();
					alarmSlider2LData.width = 100;
					alarmSlider2LData.height = 18;
					this.alarmSlider2 = new Slider(this.alarmGroup, SWT.NONE);
					this.alarmSlider2.setLayoutData(alarmSlider2LData);
					this.alarmSlider2.setMinimum(10);
					this.alarmSlider2.setMaximum(610);
					this.alarmSlider2.setIncrement(1);
					this.alarmSlider2.setSelection((int) this.alarmValue2 * 10);
					this.alarmSlider2.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "alarmSlider2.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLogTelemetryTab.this.alarmValue2 = UniLogTelemetryTab.this.alarmSlider2.getSelection() / 10.0;
							UniLogTelemetryTab.this.alarmText2.setText(String.format("%.1f", UniLogTelemetryTab.this.alarmValue2)); //$NON-NLS-1$
							UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					this.alarmLabel2 = new CLabel(this.alarmGroup, SWT.NONE);
					RowData alarmLabel2LData = new RowData();
					alarmLabel2LData.width = 40;
					alarmLabel2LData.height = 22;
					this.alarmLabel2.setLayoutData(alarmLabel2LData);
					this.alarmLabel2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmLabel2.setText(voltageStartUnit);
				}
				{
					this.alarmButton3 = new Button(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData alarmButton3LData = new RowData();
					alarmButton3LData.width = 140;
					alarmButton3LData.height = 22;
					this.alarmButton3.setLayoutData(alarmButton3LData);
					this.alarmButton3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmButton3.setText(Messages.getString(MessageIds.GDE_MSGT1389));
					this.alarmButton3.setSelection(this.isAlarmButton3);
					this.alarmButton3.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "alarmButton3.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLogTelemetryTab.this.isAlarmButton3 = UniLogTelemetryTab.this.alarmButton3.getSelection();
							UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					this.alarmText3 = new Text(this.alarmGroup, SWT.RIGHT | SWT.READ_ONLY | SWT.BORDER);
					RowData alarmText3LData = new RowData();
					alarmText3LData.width = 40;
					alarmText3LData.height = 14;
					this.alarmText3.setLayoutData(alarmText3LData);
					this.alarmText3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmText3.setText(String.format("%.1f", this.alarmValue3)); //$NON-NLS-1$
				}
				{
					RowData alarmSlider3LData = new RowData();
					alarmSlider3LData.width = 100;
					alarmSlider3LData.height = 18;
					this.alarmSlider3 = new Slider(this.alarmGroup, SWT.NONE);
					this.alarmSlider3.setLayoutData(alarmSlider3LData);
					this.alarmSlider3.setMinimum(10);
					this.alarmSlider3.setMaximum(610);
					this.alarmSlider3.setIncrement(1);
					this.alarmSlider3.setSelection((int) this.alarmValue3 * 10);
					this.alarmSlider3.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "alarmSlider3.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLogTelemetryTab.this.alarmValue3 = UniLogTelemetryTab.this.alarmSlider3.getSelection() / 10.0;
							UniLogTelemetryTab.this.alarmText3.setText(String.format("%.1f", UniLogTelemetryTab.this.alarmValue3)); //$NON-NLS-1$
							UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					this.alarmLabel3 = new CLabel(this.alarmGroup, SWT.NONE);
					RowData alarmLabel3LData = new RowData();
					alarmLabel3LData.width = 40;
					alarmLabel3LData.height = 22;
					this.alarmLabel3.setLayoutData(alarmLabel3LData);
					this.alarmLabel3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmLabel3.setText(voltageUnit);
				}
				{
					this.alarmButton4 = new Button(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData alarmButton4LData = new RowData();
					alarmButton4LData.width = 140;
					alarmButton4LData.height = 22;
					this.alarmButton4.setLayoutData(alarmButton4LData);
					this.alarmButton4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmButton4.setText(Messages.getString(MessageIds.GDE_MSGT1391));
					this.alarmButton4.setSelection(this.isAlarmButton4);
					this.alarmButton4.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "alarmButton4.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLogTelemetryTab.this.isAlarmButton4 = UniLogTelemetryTab.this.alarmButton4.getSelection();
							UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					this.alarmText4 = new Text(this.alarmGroup, SWT.RIGHT | SWT.READ_ONLY | SWT.BORDER);
					RowData alarmText4LData = new RowData();
					alarmText4LData.width = 40;
					alarmText4LData.height = 14;
					this.alarmText4.setLayoutData(alarmText4LData);
					this.alarmText4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmText4.setText(GDE.STRING_EMPTY + this.alarmValue4);
				}
				{
					RowData alarmSlider4LData = new RowData();
					alarmSlider4LData.width = 100;
					alarmSlider4LData.height = 18;
					this.alarmSlider4 = new Slider(this.alarmGroup, SWT.NONE);
					this.alarmSlider4.setLayoutData(alarmSlider4LData);
					this.alarmSlider4.setMinimum(10);
					this.alarmSlider4.setMaximum(1010);
					this.alarmSlider4.setIncrement(5);
					this.alarmSlider4.setSelection(this.alarmValue4 / 10);
					this.alarmSlider4.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "alarmSlider4.widgetSelected, selection=" + UniLogTelemetryTab.this.alarmSlider4.getSelection()); //$NON-NLS-1$
							UniLogTelemetryTab.this.alarmValue4 = UniLogTelemetryTab.this.alarmSlider4.getSelection() * 10;
							UniLogTelemetryTab.this.alarmValue4 = UniLogTelemetryTab.this.alarmValue4 - (UniLogTelemetryTab.this.alarmValue4 % 50);
							UniLogTelemetryTab.this.alarmText4.setText(GDE.STRING_EMPTY + UniLogTelemetryTab.this.alarmValue4);
							UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					this.alarmLabel4 = new CLabel(this.alarmGroup, SWT.NONE);
					RowData alarmLabel4LData = new RowData();
					alarmLabel4LData.width = 40;
					alarmLabel4LData.height = 22;
					this.alarmLabel4.setLayoutData(alarmLabel4LData);
					this.alarmLabel4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmLabel4.setText(capacityUnit);
				}
				{
					this.alarmButton5 = new Button(this.alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData alarmButton5LData = new RowData();
					alarmButton5LData.width = 140;
					alarmButton5LData.height = 22;
					this.alarmButton5.setLayoutData(alarmButton5LData);
					this.alarmButton5.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmButton5.setText(Messages.getString(MessageIds.GDE_MSGT1392));
					this.alarmButton5.setSelection(this.isAlarmButton5);
					this.alarmButton5.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "alarmButton5.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLogTelemetryTab.this.isAlarmButton5 = UniLogTelemetryTab.this.alarmButton5.getSelection();
							UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					this.alarmText5 = new Text(this.alarmGroup, SWT.RIGHT | SWT.READ_ONLY | SWT.BORDER);
					RowData alarmText5LData = new RowData();
					alarmText5LData.width = 40;
					alarmText5LData.height = 14;
					this.alarmText5.setLayoutData(alarmText5LData);
					this.alarmText5.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmText5.setText(GDE.STRING_EMPTY + this.alarmValue5);
				}
				{
					RowData alarmSlider5LData = new RowData();
					alarmSlider5LData.width = 100;
					alarmSlider5LData.height = 18;
					this.alarmSlider5 = new Slider(this.alarmGroup, SWT.NONE);
					this.alarmSlider5.setLayoutData(alarmSlider5LData);
					this.alarmSlider5.setMinimum(10);
					this.alarmSlider5.setMaximum(1010);
					this.alarmSlider5.setIncrement(10);
					this.alarmSlider5.setSelection(this.alarmValue5);
					this.alarmSlider5.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "alarmSlider5.widgetSelected, selection=" + UniLogTelemetryTab.this.alarmSlider5.getSelection()); //$NON-NLS-1$
							UniLogTelemetryTab.this.alarmValue5 = UniLogTelemetryTab.this.alarmSlider5.getSelection();
							UniLogTelemetryTab.this.alarmValue5 = UniLogTelemetryTab.this.alarmValue5 - (UniLogTelemetryTab.this.alarmValue5 % 10);
							UniLogTelemetryTab.this.alarmText5.setText(GDE.STRING_EMPTY + UniLogTelemetryTab.this.alarmValue5);
							UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					this.alarmLabel5 = new CLabel(this.alarmGroup, SWT.NONE);
					RowData alarmLabel5LData = new RowData();
					alarmLabel5LData.width = 40;
					alarmLabel5LData.height = 22;
					this.alarmLabel5.setLayoutData(alarmLabel5LData);
					this.alarmLabel5.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.alarmLabel5.setText(heightUnit);
				}
				{
					this.mLinkAddressesGroup = new Group(this, SWT.NONE);
					RowLayout mLinkAddressesGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					this.mLinkAddressesGroup.setLayout(mLinkAddressesGroupLayout);
					FormData mLinkAddressesGroupLData = new FormData();
					mLinkAddressesGroupLData.left = new FormAttachment(0, 1000, 375);
					mLinkAddressesGroupLData.top = new FormAttachment(0, 1000, 105);
					mLinkAddressesGroupLData.width = 249;
					mLinkAddressesGroupLData.height = 130;
					mLinkAddressesGroupLData.right = new FormAttachment(1000, 1000, 0);
					mLinkAddressesGroupLData.bottom = new FormAttachment(1000, 1000, -75);
					this.mLinkAddressesGroup.setLayoutData(mLinkAddressesGroupLData);
					this.mLinkAddressesGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.mLinkAddressesGroup.setText(Messages.getString(MessageIds.GDE_MSGT1393));
					{
						this.addressLabel1 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
						RowData addressLabel1LData = new RowData();
						addressLabel1LData.width = 80;
						addressLabel1LData.height = 22;
						this.addressLabel1.setLayoutData(addressLabel1LData);
						this.addressLabel1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.addressLabel1.setText(addressLabelName1); 
					}
					{
						this.addressText1 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
						RowData addressText1LData = new RowData();
						addressText1LData.width = 40;
						addressText1LData.height = 14;
						this.addressText1.setLayoutData(addressText1LData);
						this.addressText1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.addressText1.setText(GDE.STRING_EMPTY + this.addressValue1);
					}
					{
						RowData addressSlider1LData = new RowData();
						addressSlider1LData.width = 100;
						addressSlider1LData.height = 18;
						this.addressSlider1 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
						this.addressSlider1.setLayoutData(addressSlider1LData);
						this.addressSlider1.setMinimum(1);
						this.addressSlider1.setMaximum(26);
						this.addressSlider1.setIncrement(1);
						this.addressSlider1.setSelection(this.addressValue1);
						this.addressSlider1.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "addressSlider1.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLogTelemetryTab.this.addressValue1 = UniLogTelemetryTab.this.addressSlider1.getSelection();
								UniLogTelemetryTab.this.addressText1.setText(GDE.STRING_EMPTY + UniLogTelemetryTab.this.addressValue1);
								UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
							}
						});
					}
					{
						this.addressLabel2 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
						RowData addressLabel2LData = new RowData();
						addressLabel2LData.width = 80;
						addressLabel2LData.height = 22;
						this.addressLabel2.setLayoutData(addressLabel2LData);
						this.addressLabel2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.addressLabel2.setText(addressLabelName2);
					}
					{
						this.addressText2 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
						RowData addressText2LData = new RowData();
						addressText2LData.width = 40;
						addressText2LData.height = 14;
						this.addressText2.setLayoutData(addressText2LData);
						this.addressText2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.addressText2.setText(GDE.STRING_EMPTY + this.addressValue2);
					}
					{
						RowData addressSlider2LData = new RowData();
						addressSlider2LData.width = 100;
						addressSlider2LData.height = 18;
						this.addressSlider2 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
						this.addressSlider2.setLayoutData(addressSlider2LData);
						this.addressSlider2.setMinimum(1);
						this.addressSlider2.setMaximum(26);
						this.addressSlider2.setIncrement(1);
						this.addressSlider2.setSelection(this.addressValue2);
						this.addressSlider2.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "addressSlider2.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLogTelemetryTab.this.addressValue2 = UniLogTelemetryTab.this.addressSlider2.getSelection();
								UniLogTelemetryTab.this.addressText2.setText(GDE.STRING_EMPTY + UniLogTelemetryTab.this.addressValue2);
								UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
							}
						});
					}
					{
						this.addressLabel3 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
						RowData addressLabel3LData = new RowData();
						addressLabel3LData.width = 80;
						addressLabel3LData.height = 22;
						this.addressLabel3.setLayoutData(addressLabel3LData);
						this.addressLabel3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.addressLabel3.setText(addressLabelName3);
					}
					{
						this.addressText3 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
						RowData addressText3LData = new RowData();
						addressText3LData.width = 40;
						addressText3LData.height = 14;
						this.addressText3.setLayoutData(addressText3LData);
						this.addressText3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.addressText3.setText(GDE.STRING_EMPTY + this.addressValue3);
					}
					{
						RowData addressSlider3LData = new RowData();
						addressSlider3LData.width = 100;
						addressSlider3LData.height = 18;
						this.addressSlider3 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
						this.addressSlider3.setLayoutData(addressSlider3LData);
						this.addressSlider3.setMinimum(1);
						this.addressSlider3.setMaximum(26);
						this.addressSlider3.setIncrement(1);
						this.addressSlider3.setSelection(this.addressValue3);
						this.addressSlider3.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "addressSlider3.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLogTelemetryTab.this.addressValue3 = UniLogTelemetryTab.this.addressSlider3.getSelection();
								UniLogTelemetryTab.this.addressText3.setText(GDE.STRING_EMPTY + UniLogTelemetryTab.this.addressValue3);
								UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
							}
						});
					}
					{
						this.addressLabel4 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
						RowData addressLabel4LData = new RowData();
						addressLabel4LData.width = 80;
						addressLabel4LData.height = 22;
						this.addressLabel4.setLayoutData(addressLabel4LData);
						this.addressLabel4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.addressLabel4.setText(addressLabelName4);
					}
					{
						this.addressText4 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
						RowData addressText4LData = new RowData();
						addressText4LData.width = 40;
						addressText4LData.height = 14;
						this.addressText4.setLayoutData(addressText4LData);
						this.addressText4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.addressText4.setText(GDE.STRING_EMPTY + this.addressValue4);
					}
					{
						RowData addressSlider4LData = new RowData();
						addressSlider4LData.width = 100;
						addressSlider4LData.height = 18;
						this.addressSlider4 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
						this.addressSlider4.setLayoutData(addressSlider4LData);
						this.addressSlider4.setMinimum(1);
						this.addressSlider4.setMaximum(26);
						this.addressSlider4.setIncrement(1);
						this.addressSlider4.setSelection(this.addressValue4);
						this.addressSlider4.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "addressSlider4.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLogTelemetryTab.this.addressValue4 = UniLogTelemetryTab.this.addressSlider4.getSelection();
								UniLogTelemetryTab.this.addressText4.setText(GDE.STRING_EMPTY + UniLogTelemetryTab.this.addressValue4);
								UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
							}
						});
					}
					{
						this.addressLabel5 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
						RowData addressLabel5LData = new RowData();
						addressLabel5LData.width = 80;
						addressLabel5LData.height = 22;
						this.addressLabel5.setLayoutData(addressLabel5LData);
						this.addressLabel5.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.addressLabel5.setText(addressLabelName5);
					}
					{
						this.addressText5 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
						RowData addressText5LData = new RowData();
						addressText5LData.width = 40;
						addressText5LData.height = 14;
						this.addressText5.setLayoutData(addressText5LData);
						this.addressText5.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.addressText5.setText(GDE.STRING_EMPTY + this.addressValue5);
					}
					{
						RowData addressSlider5LData = new RowData();
						addressSlider5LData.width = 100;
						addressSlider5LData.height = 18;
						this.addressSlider5 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
						this.addressSlider5.setLayoutData(addressSlider5LData);
						this.addressSlider5.setMinimum(1);
						this.addressSlider5.setMaximum(26);
						this.addressSlider5.setIncrement(1);
						this.addressSlider5.setSelection(this.addressValue5);
						this.addressSlider5.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "addressSlider5.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLogTelemetryTab.this.addressValue5 = UniLogTelemetryTab.this.addressSlider5.getSelection();
								UniLogTelemetryTab.this.addressText5.setText(GDE.STRING_EMPTY + UniLogTelemetryTab.this.addressValue5);
								UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
							}
						});
					}
				}
				{
					this.writeTelemtryConfigurationButton = new Button(this, SWT.PUSH | SWT.CENTER);
					FormData writeTelemtryConfigurationButtonLData = new FormData();
					writeTelemtryConfigurationButtonLData.left = new FormAttachment(0, 1000, 100);
					writeTelemtryConfigurationButtonLData.top = new FormAttachment(0, 1000, 280);
					writeTelemtryConfigurationButtonLData.bottom = new FormAttachment(1000, 1000, -20);
					writeTelemtryConfigurationButtonLData.right = new FormAttachment(1000, 1000, -100);
					this.writeTelemtryConfigurationButton.setLayoutData(writeTelemtryConfigurationButtonLData);
					this.writeTelemtryConfigurationButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.writeTelemtryConfigurationButton.setText(Messages.getString(MessageIds.GDE_MSGT1395));
					this.writeTelemtryConfigurationButton.setEnabled(false);
					this.writeTelemtryConfigurationButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							UniLogTelemetryTab.log.log(java.util.logging.Level.FINEST, "writeTelemtryConfigurationButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							try {
								if (UniLogTelemetryTab.this.serialPort.setConfiguration(buildTelemetryConfigurationBuffer())) {
									UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(false);
								}
								else {
									UniLogTelemetryTab.this.writeTelemtryConfigurationButton.setEnabled(true);
								}
							}
							catch (Exception e) {
								UniLogTelemetryTab.this.application.openMessageDialog(UniLogTelemetryTab.this.dialog.getDialogShell(), e.getMessage());
							}
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

	/**
	 * update the telemetry configuration tab with values red 
	 * @param readBuffer
	 */
	public void updateTelemetryConfigurationValues(byte[] readBuffer) {
		this.isAlarmButton1 = (readBuffer[19] & 0x01) == 0x01;
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "isAlarmButton1 = " + this.isAlarmButton1); //$NON-NLS-1$
		this.alarmButton1.setSelection(this.isAlarmButton1);

		this.isAlarmButton2 = (readBuffer[19] & 0x02) == 0x02;
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "isAlarmButton2 = " + this.isAlarmButton2); //$NON-NLS-1$
		this.alarmButton2.setSelection(this.isAlarmButton2);

		this.isAlarmButton3 = (readBuffer[19] & 0x04) == 0x04;
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "isAlarmButton3 = " + this.isAlarmButton3); //$NON-NLS-1$
		this.alarmButton3.setSelection(this.isAlarmButton3);

		this.isAlarmButton4 = (readBuffer[19] & 0x08) == 0x08;
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "isAlarmButton4 = " + this.isAlarmButton4); //$NON-NLS-1$
		this.alarmButton4.setSelection(this.isAlarmButton4);

		this.isAlarmButton5 = (readBuffer[19] & 0x10) == 0x10;
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "isAlarmButton5 = " + this.isAlarmButton5); //$NON-NLS-1$
		this.alarmButton5.setSelection(this.isAlarmButton5);

		this.alarmValue1 = ((readBuffer[5] & 0xFF) << 8) + (readBuffer[4] & 0xFF);
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "alarmValue1 = " + this.alarmValue1); //$NON-NLS-1$
		this.alarmText1.setText(GDE.STRING_EMPTY + this.alarmValue1);
		this.alarmSlider1.setSelection(this.alarmValue1);

		this.alarmValue2 = (((readBuffer[7] & 0xFF) << 8) + (readBuffer[6] & 0xFF)) / 10.0;
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "alarmValue2 = " + this.alarmValue2); //$NON-NLS-1$
		this.alarmText2.setText(String.format("%.1f", this.alarmValue2)); //$NON-NLS-1$
		this.alarmSlider2.setSelection((int) this.alarmValue2 * 10);

		this.alarmValue3 = (((readBuffer[9] & 0xFF) << 8) + (readBuffer[8] & 0xFF)) / 10.0;
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "alarmValue3 = " + this.alarmValue3); //$NON-NLS-1$
		this.alarmText3.setText(String.format("%.1f", this.alarmValue3)); //$NON-NLS-1$
		this.alarmSlider3.setSelection((int) this.alarmValue3 * 10);

		this.alarmValue4 = ((readBuffer[11] & 0xFF) << 8) + (readBuffer[10] & 0xFF);
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "alarmValue4 = " + this.alarmValue4); //$NON-NLS-1$
		this.alarmText4.setText(GDE.STRING_EMPTY + this.alarmValue4);
		this.alarmSlider4.setSelection(this.alarmValue4 / 10);

		this.alarmValue5 = ((readBuffer[13] & 0xFF) << 8) + (readBuffer[12] & 0xFF);
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "alarmValue5 = " + this.alarmValue5); //$NON-NLS-1$
		this.alarmText5.setText(GDE.STRING_EMPTY + this.alarmValue5);
		this.alarmSlider5.setSelection(this.alarmValue5);

		this.addressValue1 = (readBuffer[14] & 0xFF);
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "addressValue1 = " + this.addressValue1); //$NON-NLS-1$
		this.addressSlider1.setSelection(this.addressValue1);
		this.addressText1.setText(GDE.STRING_EMPTY + this.addressValue1);

		this.addressValue2 = (readBuffer[15] & 0xFF);
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "addressValue2 = " + this.addressValue2); //$NON-NLS-1$
		this.addressSlider2.setSelection(this.addressValue2);
		this.addressText2.setText(GDE.STRING_EMPTY + this.addressValue2);

		this.addressValue3 = (readBuffer[16] & 0xFF);
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "addressValue3 = " + this.addressValue3); //$NON-NLS-1$
		this.addressSlider3.setSelection(this.addressValue3);
		this.addressText3.setText(GDE.STRING_EMPTY + this.addressValue3);

		this.addressValue4 = (readBuffer[17] & 0xFF);
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "addressValue4 = " + this.addressValue4); //$NON-NLS-1$
		this.addressSlider4.setSelection(this.addressValue4);
		this.addressText4.setText(GDE.STRING_EMPTY + this.addressValue4);

		this.addressValue5 = (readBuffer[18] & 0xFF);
		UniLogTelemetryTab.log.log(java.util.logging.Level.FINER, "addressValue5 = " + this.addressValue5); //$NON-NLS-1$
		this.addressSlider5.setSelection(this.addressValue5);
		this.addressText5.setText(GDE.STRING_EMPTY + this.addressValue5);

		this.writeTelemtryConfigurationButton.setEnabled(false);
	}

	/**
	 * @return configUpdateBuffer the data buffer containing the byte values to update the UniLog internal telemetry configuration
	 */
	public byte[] buildTelemetryConfigurationBuffer() {
		int checkSum = 0;
		byte[] configUpdateBuffer = new byte[20];
		configUpdateBuffer[0] = (byte) 0xC0;
		configUpdateBuffer[1] = (byte) 0x03;
		checkSum = checkSum + (0xFF & configUpdateBuffer[1]);
		configUpdateBuffer[2] = (byte) 0x05;
		checkSum = checkSum + (0xFF & configUpdateBuffer[2]);

		// begin alarm values as 2 byte
		configUpdateBuffer[3] = (byte) (this.alarmValue1 & 0xFF);
		checkSum = checkSum + (0xFF & configUpdateBuffer[3]);
		configUpdateBuffer[4] = (byte) ((this.alarmValue1 & 0xFF00) >> 8);
		checkSum = checkSum + (0xFF & configUpdateBuffer[4]);

		configUpdateBuffer[5] = (byte) ((int) (this.alarmValue2 * 10) & 0xFF);
		checkSum = checkSum + (0xFF & configUpdateBuffer[5]);
		configUpdateBuffer[6] = (byte) (((int) (this.alarmValue2 * 10) & 0xFF00) >> 8);
		checkSum = checkSum + (0xFF & configUpdateBuffer[6]);

		configUpdateBuffer[7] = (byte) ((int) (this.alarmValue3 * 10) & 0xFF);
		checkSum = checkSum + (0xFF & configUpdateBuffer[7]);
		configUpdateBuffer[8] = (byte) (((int) (this.alarmValue3 * 10) & 0xFF00) >> 8);
		checkSum = checkSum + (0xFF & configUpdateBuffer[8]);

		configUpdateBuffer[9] = (byte) (this.alarmValue4 & 0xFF);
		checkSum = checkSum + (0xFF & configUpdateBuffer[9]);
		configUpdateBuffer[10] = (byte) ((this.alarmValue4 & 0xFF00) >> 8);
		checkSum = checkSum + (0xFF & configUpdateBuffer[10]);

		configUpdateBuffer[11] = (byte) (this.alarmValue5 & 0xFF);
		checkSum = checkSum + (0xFF & configUpdateBuffer[11]);
		configUpdateBuffer[12] = (byte) ((this.alarmValue5 & 0xFF00) >> 8);
		checkSum = checkSum + (0xFF & configUpdateBuffer[12]);

		// begin address values 1 byte
		configUpdateBuffer[13] = (byte) (this.addressValue1 & 0xFF);
		checkSum = checkSum + (0xFF & configUpdateBuffer[13]);

		configUpdateBuffer[14] = (byte) (this.addressValue2 & 0xFF);
		checkSum = checkSum + (0xFF & configUpdateBuffer[14]);

		configUpdateBuffer[15] = (byte) (this.addressValue3 & 0xFF);
		checkSum = checkSum + (0xFF & configUpdateBuffer[15]);

		configUpdateBuffer[16] = (byte) (this.addressValue4 & 0xFF);
		checkSum = checkSum + (0xFF & configUpdateBuffer[16]);

		configUpdateBuffer[17] = (byte) (this.addressValue5 & 0xFF);
		checkSum = checkSum + (0xFF & configUpdateBuffer[17]);

		// begin alarm selections
		configUpdateBuffer[18] = (byte) (this.isAlarmButton1 ? 0x01 : 0x00);
		configUpdateBuffer[18] = (byte) (configUpdateBuffer[18] | (this.isAlarmButton2 ? 0x02 : 0x00));
		configUpdateBuffer[18] = (byte) (configUpdateBuffer[18] | (this.isAlarmButton3 ? 0x04 : 0x00));
		configUpdateBuffer[18] = (byte) (configUpdateBuffer[18] | (this.isAlarmButton4 ? 0x08 : 0x00));
		configUpdateBuffer[18] = (byte) (configUpdateBuffer[18] | (this.isAlarmButton5 ? 0x10 : 0x00));
		checkSum = checkSum + (0xFF & configUpdateBuffer[18]);

		// checksum as 1 byte
		configUpdateBuffer[19] = (byte) (checkSum % 256);

		if (UniLogDialog.log.isLoggable(java.util.logging.Level.FINE)) {
			StringBuilder sb = new StringBuilder();
			sb.append("configUpdateBuffer = ["); //$NON-NLS-1$
			for (int i = 0; i < configUpdateBuffer.length; i++) {
				if (i == configUpdateBuffer.length - 1)
					sb.append(String.format("%02X", configUpdateBuffer[i])); //$NON-NLS-1$
				else
					sb.append(String.format("%02X ", configUpdateBuffer[i])); //$NON-NLS-1$
			}
			sb.append("]"); //$NON-NLS-1$
			UniLogDialog.log.log(java.util.logging.Level.FINE, sb.toString());
		}

		return configUpdateBuffer;
	}
}
