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

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.CalculationThread;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 * Configuration tab for Picolario configration entries
 * @author Winfried Bruegmann
 */
public class PicolarioConfigTab extends Composite {
	{
		SWTResourceManager.registerResourceUser(this);
	}

	final static Logger						log										= Logger.getLogger(PicolarioConfigTab.class.getName());

	Group													heightAdaptionGroup;
	Label													heightLabel;
	Button												noAdaptionButton;
	Button												reduceByDefinedValueButton;
	Button												reduceByFirstValueButton;
	Button												reduceByLastValueButton;
	CCombo												heightOffset;
	CLabel												slopeUnit;
	CLabel												slopeName;
	CLabel												heightUnit;
	CLabel												calculationTypeLabel;
	CCombo												slopeCalculationTypeCombo;
	CCombo												regressionTime;
	Button 												makePersitentButton;

	boolean												isConfigChanged 			= false;
	String												heightDataUnit				= "m";																									// Meter is default //$NON-NLS-1$
	boolean												doNoAdation						= false;
	boolean												doHeightAdaption			= false;																								// indicates to adapt height values
	boolean												doSubtractFirst				= true;																								// indicates to subtract first values from all other
	boolean												doSubtractLast				= false;																								// indicates to subtract last values from all other
	boolean												doOffsetHeight				= false;																								// indicates that the height has to be corrected by an offset
	int														heightOffsetSelection	= 7;																										// represents the offset the measurment should be modified
	double												heightOffsetValue			= 100;																									// represents the offset value
	String												slopeDataUnit					= "m/s";																								// Meter is default //$NON-NLS-1$
	String												slopeTypeSelection		= CalculationThread.REGRESSION_TYPE_CURVE;
	int														slopeTimeSelection;

	final Picolario								device;																																			// get device specific things, get serial port, ...
	final DataExplorer	application;
	final String									configName;																																	// tabName
	final int											configNumber;																																// tabOrdinal + 1 

	/**
	 * panel tab describing a configuration
	 * @param parent
	 * @param useDevice
	 * @param tabName
	 */
	public PicolarioConfigTab(CTabFolder parent, Picolario useDevice, String tabName) {
		super(parent, SWT.NONE);
		this.device = useDevice;
		this.configName = tabName;
		this.configNumber = parent.getItemCount() + 1;
		this.application = DataExplorer.getInstance();
		initGUI();
		initialize();
	}

	private void initGUI() {
		try {
			FillLayout thisLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
			this.setLayout(thisLayout);
			this.setSize(310, 210);
			{
				this.setLayout(null);
				FillLayout composite1Layout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.setLayout(composite1Layout);
				{ // group 2
					this.heightAdaptionGroup = new Group(this, SWT.NONE);
					this.heightAdaptionGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightAdaptionGroup.setLayout(null);
					this.heightAdaptionGroup.setText(Messages.getString(MessageIds.GDE_MSGT1209));
					{
						this.noAdaptionButton = new Button(this.heightAdaptionGroup, SWT.RADIO | SWT.LEFT);
						this.noAdaptionButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.noAdaptionButton.setText(Messages.getString(MessageIds.GDE_MSGT1210));
						this.noAdaptionButton.setBounds(12, GDE.IS_MAC_COCOA ? 25 : 40, 186, 16);
						this.noAdaptionButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "noAdaptioButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								PicolarioConfigTab.this.noAdaptionButton.setSelection(true);
								PicolarioConfigTab.this.reduceByFirstValueButton.setSelection(false);
								PicolarioConfigTab.this.reduceByLastValueButton.setSelection(false);
								PicolarioConfigTab.this.reduceByDefinedValueButton.setSelection(false);
								PicolarioConfigTab.this.doNoAdation = true;
								PicolarioConfigTab.this.doSubtractFirst = false;
								PicolarioConfigTab.this.doSubtractLast = false;
								PicolarioConfigTab.this.doOffsetHeight = false;
								PicolarioConfigTab.this.heightOffsetValue = 0.0;
								PicolarioConfigTab.this.heightOffset.setText(Double.valueOf(PicolarioConfigTab.this.heightOffsetValue).toString());

								updateRecordProperties();
							}
						});
					}
					{
						this.reduceByFirstValueButton = new Button(this.heightAdaptionGroup, SWT.RADIO | SWT.LEFT);
						this.reduceByFirstValueButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.reduceByFirstValueButton.setText(Messages.getString(MessageIds.GDE_MSGT1211));
						this.reduceByFirstValueButton.setSelection(this.doSubtractFirst);
						this.reduceByFirstValueButton.setBounds(12, GDE.IS_MAC_COCOA ? 46 : 61, 297, 16);
						this.reduceByFirstValueButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "reduceByFirstValueButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								PicolarioConfigTab.this.noAdaptionButton.setSelection(false);
								PicolarioConfigTab.this.reduceByFirstValueButton.setSelection(true);
								PicolarioConfigTab.this.reduceByLastValueButton.setSelection(false);
								PicolarioConfigTab.this.reduceByDefinedValueButton.setSelection(false);
								PicolarioConfigTab.this.doNoAdation = false;
								PicolarioConfigTab.this.doSubtractFirst = true;
								PicolarioConfigTab.this.doSubtractLast = false;
								PicolarioConfigTab.this.doOffsetHeight = false;
								PicolarioConfigTab.this.heightOffsetValue = 0.0;
								PicolarioConfigTab.this.heightOffset.setText(Double.valueOf(PicolarioConfigTab.this.heightOffsetValue).toString());

								updateRecordProperties();
							}
						});
					}
					{
						this.reduceByLastValueButton = new Button(this.heightAdaptionGroup, SWT.RADIO | SWT.LEFT);
						this.reduceByLastValueButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.reduceByLastValueButton.setBounds(12, GDE.IS_MAC_COCOA ? 65 : 80, 293, 18);
						this.reduceByLastValueButton.setText(Messages.getString(MessageIds.GDE_MSGT1212));
						this.reduceByLastValueButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "reduceByLastValueButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								PicolarioConfigTab.this.noAdaptionButton.setSelection(false);
								PicolarioConfigTab.this.reduceByFirstValueButton.setSelection(false);
								PicolarioConfigTab.this.reduceByLastValueButton.setSelection(true);
								PicolarioConfigTab.this.reduceByDefinedValueButton.setSelection(false);
								PicolarioConfigTab.this.doNoAdation = false;
								PicolarioConfigTab.this.doSubtractFirst = false;
								PicolarioConfigTab.this.doSubtractLast = true;
								PicolarioConfigTab.this.doOffsetHeight = false;
								PicolarioConfigTab.this.heightOffsetValue = 0.0;
								PicolarioConfigTab.this.heightOffset.setText(Double.valueOf(PicolarioConfigTab.this.heightOffsetValue).toString());

								updateRecordProperties();
							}
						});
					}
					{
						this.reduceByDefinedValueButton = new Button(this.heightAdaptionGroup, SWT.RADIO | SWT.LEFT);
						this.reduceByDefinedValueButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.reduceByDefinedValueButton.setText(Messages.getString(MessageIds.GDE_MSGT1213));
						this.reduceByDefinedValueButton.setSelection(this.doOffsetHeight);
						this.reduceByDefinedValueButton.setBounds(12, GDE.IS_MAC_COCOA ? 89 : 103, 143, 16);
						this.reduceByDefinedValueButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "reduceByDefinedValueButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								PicolarioConfigTab.this.noAdaptionButton.setSelection(false);
								PicolarioConfigTab.this.reduceByFirstValueButton.setSelection(false);
								PicolarioConfigTab.this.reduceByLastValueButton.setSelection(false);
								PicolarioConfigTab.this.reduceByDefinedValueButton.setSelection(true);
								PicolarioConfigTab.this.doNoAdation = false;
								PicolarioConfigTab.this.doOffsetHeight = true;
								PicolarioConfigTab.this.doSubtractFirst = false;
								PicolarioConfigTab.this.doSubtractLast = false;
								PicolarioConfigTab.this.heightOffsetValue = Double.valueOf(PicolarioConfigTab.this.heightOffset.getText()).doubleValue();

								updateRecordProperties();
							}
						});
					}
					{
						this.heightOffset = new CCombo(this.heightAdaptionGroup, SWT.BORDER);
						this.heightOffset.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						final String[] heightOffsetValues = new String[] { "200", "100", "50", "0", "-50", "-100", "-150", "-200", "-250", "-300", "-400", "-500", "-750", "-1000", "-1500" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$
						this.heightOffset.setItems(heightOffsetValues);
						this.heightOffset.setText(String.format("%.1f",this.heightOffsetValue));
						for (int i = 0; i < heightOffsetValues.length; ++i) { // loop only //$NON-NLS-1$
							if ( Math.abs(Integer.valueOf(heightOffsetValues[i]) - this.heightOffsetValue) < 0.1) this.heightOffset.select(i);
						}
						this.heightOffset.setBounds(184, GDE.IS_MAC_COCOA ? 86 : 101, 116, GDE.IS_LINUX ? 22 : 20);
						this.heightOffset.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "heightOffset.widgetSelected, event=" + evt); //$NON-NLS-1$
								PicolarioConfigTab.this.heightOffsetValue = Double.valueOf(heightOffsetValues[PicolarioConfigTab.this.heightOffset.getSelectionIndex()]).doubleValue();

								Channel activeChannel = Channels.getInstance().getActiveChannel();
								if (activeChannel != null) {
									RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
									if (activeRecordSet != null) {
										Record activeRecord = activeRecordSet.get(1);// height
										activeRecord.setOffset(PicolarioConfigTab.this.heightOffsetValue);
										PicolarioConfigTab.this.application.updateGraphicsWindow();
										activeRecordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
									}
								}
								PicolarioConfigTab.this.isConfigChanged = true;
								PicolarioConfigTab.this.makePersitentButton.setEnabled(PicolarioConfigTab.this.isConfigChanged);
							}
						});
						this.heightOffset.addKeyListener(new KeyAdapter() {
							@Override
							public void keyPressed(KeyEvent evt) {
								log.log(Level.FINEST, "heightOffset.keyPressed, event=" + evt); //$NON-NLS-1$
								if (evt.character == SWT.CR) {
									//heightOffsetSelection 
									try {
										PicolarioConfigTab.this.heightOffsetValue = Double.valueOf(PicolarioConfigTab.this.heightOffset.getText().replace(',', '.')).doubleValue();

										Channel activeChannel = Channels.getInstance().getActiveChannel();
										if (activeChannel != null) {
											RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
											if (activeRecordSet != null) {
												Record activeRecord = activeRecordSet.get(1);// height
												activeRecord.setOffset(PicolarioConfigTab.this.heightOffsetValue);
												PicolarioConfigTab.this.application.updateGraphicsWindow();
												activeRecordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
											}
										}
									}
									catch (NumberFormatException e) {
										log.log(Level.WARNING, e.getMessage(), e);
										PicolarioConfigTab.this.application.openMessageDialog(PicolarioConfigTab.this.getShell(), Messages.getString(MessageIds.GDE_MSGE1200, new Object[] { e.getMessage() } ));
									}
									PicolarioConfigTab.this.isConfigChanged = true;
									PicolarioConfigTab.this.makePersitentButton.setEnabled(PicolarioConfigTab.this.isConfigChanged);
								}
							}
						});
					}
					{
						this.heightLabel = new Label(this.heightAdaptionGroup, SWT.NONE);
						this.heightLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.heightLabel.setBounds(12, GDE.IS_MAC_COCOA ? 5 : 20, 76, 20);
						this.heightLabel.setText(Messages.getString(MessageIds.GDE_MSGT1214));
					}
					{
						this.heightUnit = new CLabel(this.heightAdaptionGroup, SWT.NONE);
						this.heightUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.heightUnit.setBounds(90, GDE.IS_MAC_COCOA ? 3 : 18, 60, 20);
						this.heightUnit.setText("[m]"); //$NON-NLS-1$
					}
					{
						this.slopeName = new CLabel(this.heightAdaptionGroup, SWT.NONE);
						this.slopeName.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.slopeName.setBounds(12, GDE.IS_MAC_COCOA ? 112 : 127, 76, 20);
						this.slopeName.setText(Messages.getString(MessageIds.GDE_MSGT1216));
					}
					{
						this.calculationTypeLabel = new CLabel(this.heightAdaptionGroup, SWT.NONE);
						this.calculationTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.calculationTypeLabel.setBounds(15, GDE.IS_MAC_COCOA ? 136 : 151, 115, 20);
						this.calculationTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT1215));
					}
					{
						this.slopeUnit = new CLabel(this.heightAdaptionGroup, SWT.NONE);
						this.slopeUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.slopeUnit.setBounds(90, GDE.IS_MAC_COCOA ? 112 : 127, 60, 20);
						this.slopeUnit.setText("[m/s]"); //$NON-NLS-1$
					}
					{
						this.slopeCalculationTypeCombo = new CCombo(this.heightAdaptionGroup, SWT.BORDER);
						this.slopeCalculationTypeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.slopeCalculationTypeCombo.setBounds(130, GDE.IS_MAC_COCOA ? 135 : 150, 100, GDE.IS_LINUX ? 22 : 20);
						this.slopeCalculationTypeCombo.setItems(new String[] { " " + Messages.getString(gde.messages.MessageIds.GDE_MSGT0262), " " + Messages.getString(gde.messages.MessageIds.GDE_MSGT0263) }); //$NON-NLS-1$ //$NON-NLS-2$
						this.slopeCalculationTypeCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT1217));
						this.slopeCalculationTypeCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "slopeCalculationTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (PicolarioConfigTab.this.slopeCalculationTypeCombo.getSelectionIndex() == 1)
									PicolarioConfigTab.this.slopeTypeSelection = CalculationThread.REGRESSION_TYPE_CURVE;
								else
									PicolarioConfigTab.this.slopeTypeSelection = CalculationThread.REGRESSION_TYPE_LINEAR;

								Channel activeChannel = Channels.getInstance().getActiveChannel();
								if (activeChannel != null) {
									RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
									if (activeRecordSet != null && activeRecordSet.isRaw()) {
										Record activeRecord = activeRecordSet.get(2);// slope
										activeRecord.getProperty(CalculationThread.REGRESSION_TYPE).setValue(PicolarioConfigTab.this.slopeTypeSelection);
										activeRecord.setDisplayable(false);
										activeRecordSet.setRecalculationRequired();
										PicolarioConfigTab.this.device.makeInActiveDisplayable(activeRecordSet);
										PicolarioConfigTab.this.application.updateStatisticsData();
										PicolarioConfigTab.this.application.updateDataTable(activeRecordSet.getName(), true);
										activeRecordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
									}
								}
								PicolarioConfigTab.this.isConfigChanged = true;
								PicolarioConfigTab.this.makePersitentButton.setEnabled(PicolarioConfigTab.this.isConfigChanged);
							}
						});
					}
					{
						this.regressionTime = new CCombo(this.heightAdaptionGroup, SWT.BORDER);
						this.regressionTime.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.regressionTime.setBounds(232, GDE.IS_MAC_COCOA ? 135 : 150, 70, GDE.IS_LINUX ? 22 : 20);
						this.regressionTime.setItems(new String[] { " 1 s", " 2 s", " 3 s", " 4 s", " 5 s", " 6 s", " 7 s", " 8 s", " 9 s", "10 s", "11 s", "12 s", "13 s", "14 s", "15 s", "16 s", "17 s", "18 s", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$
								"19 s", "20 s", "21 s", "22 s", "23 s", "24 s", "25 s", "26 s", "27 s", "28 s", "29 s", "30 s" }); //$NON-NLS-1$ //$NON-NLS-2$
						this.regressionTime.setToolTipText(Messages.getString(MessageIds.GDE_MSGT1218));
						this.regressionTime.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "regressionTime.widgetSelected, event=" + evt); //$NON-NLS-1$
								PicolarioConfigTab.this.slopeTimeSelection = PicolarioConfigTab.this.regressionTime.getSelectionIndex() + 1;

								Channel activeChannel = Channels.getInstance().getActiveChannel();
								if (activeChannel != null) {
									RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
									if (activeRecordSet != null && activeRecordSet.isRaw()) {
										Record activeRecord = activeRecordSet.get(2);// slope
										activeRecord.getProperty(CalculationThread.REGRESSION_INTERVAL_SEC).setValue("" + PicolarioConfigTab.this.slopeTimeSelection); //$NON-NLS-1$
										activeRecord.setDisplayable(false);
										activeRecordSet.setRecalculationRequired();
										PicolarioConfigTab.this.device.makeInActiveDisplayable(activeRecordSet);
										PicolarioConfigTab.this.application.updateStatisticsData();
										PicolarioConfigTab.this.application.updateDataTable(activeRecordSet.getName(), true);
										activeRecordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
									}
								}
								PicolarioConfigTab.this.isConfigChanged = true;
								PicolarioConfigTab.this.makePersitentButton.setEnabled(PicolarioConfigTab.this.isConfigChanged);
							}
						});
					}
					{
						this.makePersitentButton = new Button(this.heightAdaptionGroup, SWT.PUSH | SWT.CENTER);
						this.makePersitentButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.makePersitentButton.setBounds(10, GDE.IS_MAC_COCOA ? 162 : 177, 295, 25);
						this.makePersitentButton.setText(Messages.getString(MessageIds.GDE_MSGT1219));
						this.makePersitentButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "makePersitentButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								// 1 = height
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configNumber, 1, Picolario.DO_NO_ADAPTION, DataTypes.BOOLEAN,	PicolarioConfigTab.this.doNoAdation);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configNumber, 1, Picolario.DO_SUBTRACT_FIRST, DataTypes.BOOLEAN, PicolarioConfigTab.this.doSubtractFirst);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configNumber, 1, Picolario.DO_SUBTRACT_LAST, DataTypes.BOOLEAN,	PicolarioConfigTab.this.doSubtractLast);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configNumber, 1, Picolario.DO_OFFSET_HEIGHT, DataTypes.BOOLEAN,	PicolarioConfigTab.this.doOffsetHeight);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configNumber, 1, IDevice.OFFSET, DataTypes.DOUBLE, PicolarioConfigTab.this.heightOffsetValue);

								// 2 = slope
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configNumber, 2, CalculationThread.REGRESSION_TYPE, DataTypes.STRING,	PicolarioConfigTab.this.slopeTypeSelection);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configNumber, 2, CalculationThread.REGRESSION_INTERVAL_SEC, DataTypes.INTEGER, PicolarioConfigTab.this.slopeTimeSelection);
								
								PicolarioConfigTab.this.device.setChangePropery(true);
								PicolarioConfigTab.this.device.storeDeviceProperties();
								
								PicolarioConfigTab.this.isConfigChanged = false;
								PicolarioConfigTab.this.makePersitentButton.setEnabled(PicolarioConfigTab.this.isConfigChanged);
							}
						});
					}
				}

			}
			this.layout();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * retrieve initial values from device properties file for editable fields
	 */
	void initEditable() {
		MeasurementType measurement;
		PropertyType property = null;
		Record record = null;
		int channelConfigNumber = Channels.getInstance().getActiveChannel().getNumber();
		RecordSet recordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
		if (recordSet != null) { // load all data from record set
			record = recordSet.get(1); // 1 = height
			this.heightDataUnit = record.getUnit();
			log.log(Level.FINER, "heightDataUnit = " + this.heightDataUnit); //$NON-NLS-1$
	
			property = record.getProperty(Picolario.DO_NO_ADAPTION);
			this.doNoAdation = Boolean.valueOf(property != null ? property.getValue() : "false").booleanValue(); //$NON-NLS-1$
			log.log(Level.FINER, "doHeightAdaption = " + this.doHeightAdaption); //$NON-NLS-1$
	
			property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
			this.doSubtractFirst = Boolean.valueOf(property != null ? property.getValue() : "false").booleanValue(); //$NON-NLS-1$
			log.log(Level.FINER, "doSubtractFirst = " + this.doSubtractFirst); //$NON-NLS-1$
	
			property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
			this.doSubtractLast = Boolean.valueOf(property != null ? property.getValue() : "false").booleanValue(); //$NON-NLS-1$
			log.log(Level.FINER, "doSubtractLast = " + this.doSubtractLast); //$NON-NLS-1$
	
			property = record.getProperty(Picolario.DO_OFFSET_HEIGHT);
			this.doOffsetHeight = this.heightOffsetValue != 0 && this.doHeightAdaption;
			log.log(Level.FINER, "doOffsetHeight = " + this.doOffsetHeight); //$NON-NLS-1$
	
			property = record.getProperty(IDevice.OFFSET);
			this.heightOffsetValue = Double.valueOf(property != null ? property.getValue() : "0.0").doubleValue(); //$NON-NLS-1$
			log.log(Level.FINER, "heightOffsetValue = " + this.heightOffsetValue); //$NON-NLS-1$
	
			record = recordSet.get(2); // 2 = climb
			this.slopeDataUnit = record.getUnit();
			log.log(Level.FINER, "slopeDataUnit = " + this.slopeDataUnit); //$NON-NLS-1$
	
			PropertyType typeSelection = record.getProperty(CalculationThread.REGRESSION_TYPE);
			if (typeSelection == null)
				this.slopeTypeSelection = CalculationThread.REGRESSION_TYPE_CURVE;
			else
				this.slopeTypeSelection = typeSelection.getValue(); // CalculationThread.REGRESSION_TYPE_*
			log.log(Level.FINER, "slopeTypeSelection = " + this.slopeTypeSelection); //$NON-NLS-1$
	
			PropertyType timeSelection = record.getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
			if (timeSelection == null)
				this.slopeTimeSelection = 4;
			else
				this.slopeTimeSelection = Integer.valueOf(timeSelection.getValue());
			log.log(Level.FINER, "slopeTimeSelection = " + this.slopeTimeSelection); //$NON-NLS-1$
		}
		else {		
			measurement = this.device.getMeasurement(channelConfigNumber, 1);// 1 = height
	
			this.heightDataUnit = measurement.getUnit();
			log.log(Level.FINER, "heightDataUnit = " + this.heightDataUnit); //$NON-NLS-1$
	
			property = measurement.getProperty(Picolario.DO_NO_ADAPTION);
			this.doNoAdation = Boolean.valueOf(property != null ? property.getValue() : "false").booleanValue(); //$NON-NLS-1$
			log.log(Level.FINER, "doHeightAdaption = " + this.doHeightAdaption); //$NON-NLS-1$
	
			property = measurement.getProperty(Picolario.DO_SUBTRACT_FIRST);
			this.doSubtractFirst = Boolean.valueOf(property != null ? property.getValue() : "true").booleanValue(); //$NON-NLS-1$
			log.log(Level.FINER, "doSubtractFirst = " + this.doSubtractFirst); //$NON-NLS-1$
	
			property = measurement.getProperty(Picolario.DO_SUBTRACT_LAST);
			this.doSubtractLast = Boolean.valueOf(property != null ? property.getValue() : "false").booleanValue(); //$NON-NLS-1$
			log.log(Level.FINER, "doSubtractLast = " + this.doSubtractLast); //$NON-NLS-1$
	
			property = measurement.getProperty(Picolario.DO_OFFSET_HEIGHT);
			this.doOffsetHeight = this.heightOffsetValue != 0 && this.doHeightAdaption;
			log.log(Level.FINER, "doOffsetHeight = " + this.doOffsetHeight); //$NON-NLS-1$
	
			property = measurement.getProperty(IDevice.OFFSET);
			this.heightOffsetValue = Double.valueOf(property != null ? property.getValue() : "0.0").doubleValue(); //$NON-NLS-1$
			log.log(Level.FINER, "heightOffsetValue = " + this.heightOffsetValue); //$NON-NLS-1$
	
			// 2 = slope
			measurement = this.device.getMeasurement(channelConfigNumber, 2);
			this.slopeDataUnit = measurement.getUnit();
			log.log(Level.FINER, "slopeDataUnit = " + this.slopeDataUnit); //$NON-NLS-1$
	
			PropertyType typeSelection = this.device.getMeasruementProperty(channelConfigNumber, 2, CalculationThread.REGRESSION_TYPE);
			if (typeSelection == null)
				this.slopeTypeSelection = CalculationThread.REGRESSION_TYPE_CURVE;
			else
				this.slopeTypeSelection = typeSelection.getValue(); // CalculationThread.REGRESSION_TYPE_*
			log.log(Level.FINER, "slopeTypeSelection = " + this.slopeTypeSelection); //$NON-NLS-1$
	
			PropertyType timeSelection = this.device.getMeasruementProperty(channelConfigNumber, 2, CalculationThread.REGRESSION_INTERVAL_SEC);
			if (timeSelection == null)
				this.slopeTimeSelection = 4;
			else
				this.slopeTimeSelection = Integer.valueOf(timeSelection.getValue());
			log.log(Level.FINER, "slopeTimeSelection = " + this.slopeTimeSelection); //$NON-NLS-1$
		}
	}

	/**
	 * returns the check info as follow
	 *   1 = nichtAnpassenButton.setSelection(true);
	 *   2 = ertsenWertAbziehenButton.setSelection(true);
	 *   4 = höheVerringernButton.setSelection(true);
	 *   only 1 or 2 or 4 can returned
	 * @return integer of unit type
	 */
	public int getReduceHeightSelectionType() {
		int selection = this.noAdaptionButton.getSelection() ? 1 : 0;
		selection = selection + (this.reduceByFirstValueButton.getSelection() ? 1 : 0);
		selection = selection + (this.reduceByDefinedValueButton.getSelection() ? 1 : 0);
		return selection;
	}

	/**
	 * call this method, if reduceHeightSelectiontYpe is 4
	 * @return integer of dialog selected value
	 */
	public int getReduceHeightSelection() {
		return Integer.valueOf(this.heightOffset.getText()).intValue();
	}

	/**
	 * @return the heightOffsetValue
	 */
	public double getHeightOffsetValue() {
		return this.heightOffsetValue;
	}

	/**
	 * @return the doReduceHeight
	 */
	public boolean isDoReduceHeight() {
		return this.doOffsetHeight;
	}

	/**
	 * @return the doSubtractFirst
	 */
	public boolean isDoSubtractFirst() {
		return this.doSubtractFirst;
	}

	/**
	 * @return the doSubtractLast
	 */
	public boolean isDoSubtractLast() {
		return this.doSubtractLast;
	}

	/**
	 * @return the heightDataUnit
	 */
	public String getHeightDataUnit() {
		return this.heightDataUnit;
	}

	/**
	 * updates record internal properties and redraw the graphics
	 */
	void updateRecordProperties() {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				Record activeRecord = activeRecordSet.get(1); // height
				PropertyType property = activeRecord.getProperty(Picolario.DO_NO_ADAPTION);
				if (property == null) {
					activeRecord.createProperty(Picolario.DO_NO_ADAPTION, DataTypes.BOOLEAN, PicolarioConfigTab.this.doNoAdation);
				}
				else property.setValue(PicolarioConfigTab.this.doNoAdation);
				
				property = activeRecord.getProperty(Picolario.DO_SUBTRACT_FIRST);
				if (property == null) {
					activeRecord.createProperty(Picolario.DO_SUBTRACT_FIRST, DataTypes.BOOLEAN, PicolarioConfigTab.this.doSubtractFirst);
				}
				else property.setValue(PicolarioConfigTab.this.doSubtractFirst);
				
				property = activeRecord.getProperty(Picolario.DO_SUBTRACT_LAST);
				if (property == null) {
					activeRecord.createProperty(Picolario.DO_SUBTRACT_LAST, DataTypes.BOOLEAN, PicolarioConfigTab.this.doSubtractLast);
				}
				else property.setValue(PicolarioConfigTab.this.doSubtractLast);
				
				property = activeRecord.getProperty(Picolario.DO_OFFSET_HEIGHT);
				if (property == null) {
					activeRecord.createProperty(Picolario.DO_OFFSET_HEIGHT, DataTypes.BOOLEAN, PicolarioConfigTab.this.doOffsetHeight);
				}
				else property.setValue(PicolarioConfigTab.this.doOffsetHeight);
				
				property = activeRecord.getProperty(IDevice.OFFSET);
				if (property == null) {
					activeRecord.createProperty(IDevice.OFFSET, DataTypes.DOUBLE, PicolarioConfigTab.this.heightOffsetValue);
				}
				else property.setValue(PicolarioConfigTab.this.heightOffsetValue);
				
				PicolarioConfigTab.this.application.updateGraphicsWindow();
				activeRecordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
			}
		}
		this.isConfigChanged = true;
		this.makePersitentButton.setEnabled(this.isConfigChanged);
	}

	/**
	 * initialize buttons settings
	 */
	private void initialize() {
		initEditable();
		PicolarioConfigTab.this.heightUnit.setText("[" + PicolarioConfigTab.this.heightDataUnit + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		PicolarioConfigTab.this.noAdaptionButton.setSelection(PicolarioConfigTab.this.doNoAdation);
		PicolarioConfigTab.this.reduceByFirstValueButton.setSelection(PicolarioConfigTab.this.doSubtractFirst);
		PicolarioConfigTab.this.reduceByLastValueButton.setSelection(PicolarioConfigTab.this.doSubtractLast);
		PicolarioConfigTab.this.reduceByDefinedValueButton.setSelection(PicolarioConfigTab.this.doOffsetHeight);
		PicolarioConfigTab.this.heightOffset.setText(Double.valueOf(PicolarioConfigTab.this.heightOffsetValue).toString());

		PicolarioConfigTab.this.slopeUnit.setText("[" + PicolarioConfigTab.this.slopeDataUnit + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		PicolarioConfigTab.this.regressionTime.select(PicolarioConfigTab.this.slopeTimeSelection - 1);
		PicolarioConfigTab.this.slopeCalculationTypeCombo.select(PicolarioConfigTab.this.slopeTypeSelection.equals(CalculationThread.REGRESSION_TYPE_CURVE) ? 1 : 0);
		
		PicolarioConfigTab.this.makePersitentButton.setEnabled(PicolarioConfigTab.this.isConfigChanged);
	}

}
