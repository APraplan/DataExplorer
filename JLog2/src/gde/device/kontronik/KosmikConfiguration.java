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
    
    Copyright (c) 2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.kontronik;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.ChannelPropertyTypes;
import gde.device.DataTypes;
import gde.device.smmodellbau.JLog2Configuration;
import gde.device.smmodellbau.JLog2Dialog;
import gde.device.smmodellbau.jlog2.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

/**
 * composite containing configuration items to adapt displayed values to connected motor 
 * respective motor with gear ratio for rotor or propeller revolutions per minute
 */
public class KosmikConfiguration extends Composite {
	final static Logger	log					= Logger.getLogger(JLog2Configuration.class.getName());

	private CLabel			kosmikHeaderLabel;
	private CLabel			motorRotorRpmLabel;
	private CLabel			numMotorPolsLabel;
	private CLabel			motorPinionLabel;
	private CCombo			motorRotorRpmCombo;
	private CCombo			kosmikVersionCombo;
	private CCombo			temperatureUnitCombo;
	private Slider			mainGearToothCountSlider;
	private Text				mainGearToothCountText;
	private Text				motorPinionText;
	private Slider			motorPinionSlider;
	private CCombo			numMotorPolsCombo;
	private CLabel			kosmikVersionLabel;
	private CLabel			temperatureUnitLabel;
	private CLabel			mainGearToothCountLabel;

	final DataExplorer	application;
	final Channels			channles;
	final JLog2Dialog		dialog;
	final Kosmik				device;
	final String[]			oneTo50			= new String[50];

	boolean							isVersion32	= true;

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
		KosmikConfiguration inst = new KosmikConfiguration(shell, SWT.NULL);
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
	 * default constructor to be used by Kosmik plug-in
	 * @param parent
	 * @param style
	 * @param useDialog
	 * @param useDevice
	 */
	public KosmikConfiguration(Composite parent, int style, JLog2Dialog useDialog, Kosmik useDevice) {
		super(parent, style);
		this.application = DataExplorer.getInstance();
		this.channles = Channels.getInstance();
		this.dialog = useDialog;
		this.device = useDevice;

		for (int i = 0; i < this.oneTo50.length; i++) {
			this.oneTo50[i] = GDE.STRING_EMPTY + (i + 1);
		}
		initGUI();
	}

	/**
	 * constructor to be used by Kosmik plug-in
	 * @param parent
	 * @param style
	 */
	public KosmikConfiguration(Composite parent, int style) {
		super(parent, style);
		for (int i = 0; i < this.oneTo50.length; i++) {
			this.oneTo50[i] = GDE.STRING_EMPTY + (i + 1);
		}
		this.application = null;
		this.channles = Channels.getInstance();
		this.dialog = null;
		this.device = null;
		initGUI();
	}

	private void initGUI() {
		try {
			this.setLayout(new FormLayout());
			this.setSize(665, 334);
			{
				this.kosmikHeaderLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
				FormData kosmikHeaderLabelLData = new FormData();
				kosmikHeaderLabelLData.left = new FormAttachment(0, 1000, 10);
				kosmikHeaderLabelLData.top = new FormAttachment(0, 1000, 10);
				kosmikHeaderLabelLData.width = this.getClientArea().width - 20;
				kosmikHeaderLabelLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				this.kosmikHeaderLabel.setLayoutData(kosmikHeaderLabelLData);
				this.kosmikHeaderLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.kosmikHeaderLabel.setText(Messages.getString(MessageIds.GDE_MSGW2804));
			}
			{
				this.motorRotorRpmLabel = new CLabel(this, SWT.NONE);
				this.motorRotorRpmLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.motorRotorRpmLabel.setText(Messages.getString(MessageIds.GDE_MSGW2805));
				FormData rpmLabelLData = new FormData();
				rpmLabelLData.width = 272;
				rpmLabelLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				rpmLabelLData.left = new FormAttachment(0, 1000, 125);
				rpmLabelLData.top = new FormAttachment(0, 1000, 40);
				this.motorRotorRpmLabel.setLayoutData(rpmLabelLData);
			}
			{
				this.motorRotorRpmCombo = new CCombo(this, SWT.BORDER);
				this.motorRotorRpmCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.motorRotorRpmCombo.setItems(new String[] { "  Motor", "Propeller/Rotor" }); //$NON-NLS-1$ //$NON-NLS-2$
				FormData motorRotorRpmComboLData = new FormData();
				motorRotorRpmComboLData.width = 120;
				motorRotorRpmComboLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				motorRotorRpmComboLData.left = new FormAttachment(0, 1000, 400);
				motorRotorRpmComboLData.top = new FormAttachment(0, 1000, 40);
				this.motorRotorRpmCombo.setLayoutData(motorRotorRpmComboLData);
				this.motorRotorRpmCombo.select(0);
				this.motorRotorRpmCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (KosmikConfiguration.log.isLoggable(Level.FINEST)) KosmikConfiguration.log.log(Level.FINEST, "motorRotorRpmCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						updateMotorRotor(KosmikConfiguration.this.motorRotorRpmCombo.getSelectionIndex());
						switch (KosmikConfiguration.this.motorRotorRpmCombo.getSelectionIndex()) {
						case 1:
							KosmikConfiguration.this.setGearFactor();
							break;

						default:
							KosmikConfiguration.this.setPoleFactor();
							break;
						}
						KosmikConfiguration.this.device.setChannelProperty(ChannelPropertyTypes.NUMBER_POLES, DataTypes.INTEGER, GDE.STRING_EMPTY + (KosmikConfiguration.this.numMotorPolsCombo.getSelectionIndex()+1)*2);
					}
				});
			}
			{
				this.numMotorPolsLabel = new CLabel(this, SWT.NONE);
				this.numMotorPolsLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.numMotorPolsLabel.setText(Messages.getString(MessageIds.GDE_MSGW2808));
				FormData numPolsLabelLData = new FormData();
				numPolsLabelLData.width = 272;
				numPolsLabelLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				numPolsLabelLData.left = new FormAttachment(0, 1000, 125);
				numPolsLabelLData.top = new FormAttachment(0, 1000, 75);
				this.numMotorPolsLabel.setLayoutData(numPolsLabelLData);
			}
			{
				this.numMotorPolsCombo = new CCombo(this, SWT.BORDER);
				this.numMotorPolsCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.numMotorPolsCombo.setItems(new String[] { "    2", "    4", "    6", "    8", "   10", "   12", "   14", "   16", "   18" });
				FormData numMotorPolsComboLData = new FormData();
				numMotorPolsComboLData.width = 80;
				numMotorPolsComboLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				numMotorPolsComboLData.left = new FormAttachment(0, 1000, 400);
				numMotorPolsComboLData.top = new FormAttachment(0, 1000, 75);
				this.numMotorPolsCombo.setLayoutData(numMotorPolsComboLData);
				this.numMotorPolsCombo.select(6);
				this.numMotorPolsCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (KosmikConfiguration.log.isLoggable(Level.FINEST)) KosmikConfiguration.log.log(Level.FINEST, "numMotorPolsCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						if (KosmikConfiguration.this.motorRotorRpmCombo.getSelectionIndex() == 0) { //motor RPM
							KosmikConfiguration.this.setPoleFactor();
						}
						else { //Propeller/Rotor
							KosmikConfiguration.this.setGearFactor();
						}
						KosmikConfiguration.this.device.setChannelProperty(ChannelPropertyTypes.NUMBER_POLES, DataTypes.INTEGER, GDE.STRING_EMPTY + (KosmikConfiguration.this.numMotorPolsCombo.getSelectionIndex()+1)*2);
						KosmikConfiguration.this.dialog.enableSaveButton(true);
					}
				});
			}
			{
				this.motorPinionLabel = new CLabel(this, SWT.NONE);
				this.motorPinionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.motorPinionLabel.setText(Messages.getString(MessageIds.GDE_MSGW2806));
				FormData motorPinionLabelLData = new FormData();
				motorPinionLabelLData.width = 272;
				motorPinionLabelLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				motorPinionLabelLData.left = new FormAttachment(0, 1000, 125);
				motorPinionLabelLData.top = new FormAttachment(0, 1000, 110);
				this.motorPinionLabel.setLayoutData(motorPinionLabelLData);
			}
			{
				this.motorPinionText = new Text(this, SWT.CENTER | SWT.BORDER);
				this.motorPinionText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.motorPinionText.setText("17"); //$NON-NLS-1$
				FormData motorPinionTextLData = new FormData();
				motorPinionTextLData.width = 30;
				motorPinionTextLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				motorPinionTextLData.left = new FormAttachment(0, 1000, 400);
				motorPinionTextLData.top = new FormAttachment(0, 1000, 110);
				this.motorPinionText.setLayoutData(motorPinionTextLData);
				this.motorPinionText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent evt) {
						if (KosmikConfiguration.log.isLoggable(Level.FINEST)) KosmikConfiguration.log.log(Level.FINEST, "motorPinionText.keyPressed, event=" + evt); //$NON-NLS-1$
						KosmikConfiguration.this.motorPinionSlider.setSelection(Integer.valueOf(KosmikConfiguration.this.motorPinionText.getText().trim())
								+ KosmikConfiguration.this.motorPinionSlider.getMinimum());
						KosmikConfiguration.this.setGearFactor();
						KosmikConfiguration.this.dialog.enableSaveButton(true);
					}
				});
				this.motorPinionText.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						if (KosmikConfiguration.log.isLoggable(Level.FINEST)) KosmikConfiguration.log.log(Level.FINEST, "motorPinionText.verifyText, event=" + evt); //$NON-NLS-1$
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
			}
			{
				this.motorPinionSlider = new Slider(this, SWT.HORIZONTAL | SWT.BORDER);
				FormData motorPinionSliderLData = new FormData();
				motorPinionSliderLData.width = 100;
				motorPinionSliderLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				motorPinionSliderLData.left = new FormAttachment(0, 1000, 450);
				motorPinionSliderLData.top = new FormAttachment(0, 1000, 110);
				this.motorPinionSlider.setLayoutData(motorPinionSliderLData);
				this.motorPinionSlider.setMaximum(50);
				this.motorPinionSlider.setMinimum(10);
				this.motorPinionSlider.setSelection(16);
				this.motorPinionSlider.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (KosmikConfiguration.log.isLoggable(Level.FINEST)) KosmikConfiguration.log.log(Level.FINEST, "motorPinionSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
						KosmikConfiguration.this.motorPinionText.setText(GDE.STRING_EMPTY + KosmikConfiguration.this.motorPinionSlider.getSelection());
						KosmikConfiguration.this.setGearFactor();
						KosmikConfiguration.this.dialog.enableSaveButton(true);
					}
				});
				this.motorPinionText.setText(GDE.STRING_EMPTY + this.motorPinionSlider.getSelection());
			}
			{
				this.mainGearToothCountLabel = new CLabel(this, SWT.NONE);
				this.mainGearToothCountLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.mainGearToothCountLabel.setText(Messages.getString(MessageIds.GDE_MSGW2807));
				FormData mainGearToothCountLabelLData = new FormData();
				mainGearToothCountLabelLData.width = 272;
				mainGearToothCountLabelLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				mainGearToothCountLabelLData.left = new FormAttachment(0, 1000, 125);
				mainGearToothCountLabelLData.top = new FormAttachment(0, 1000, 145);
				this.mainGearToothCountLabel.setLayoutData(mainGearToothCountLabelLData);
			}
			{
				this.mainGearToothCountText = new Text(this, SWT.CENTER | SWT.BORDER);
				this.mainGearToothCountText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.mainGearToothCountText.setText("87");
				FormData mainGearToothCountTextLData = new FormData();
				mainGearToothCountTextLData.width = 30;
				mainGearToothCountTextLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				mainGearToothCountTextLData.left = new FormAttachment(0, 1000, 400);
				mainGearToothCountTextLData.top = new FormAttachment(0, 1000, 145);
				this.mainGearToothCountText.setLayoutData(mainGearToothCountTextLData);
				this.mainGearToothCountText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent evt) {
						if (KosmikConfiguration.log.isLoggable(Level.FINEST)) KosmikConfiguration.log.log(Level.FINEST, "mainGearToothCountText.keyPressed, event=" + evt); //$NON-NLS-1$
						KosmikConfiguration.this.mainGearToothCountSlider.setSelection(Integer.valueOf(KosmikConfiguration.this.mainGearToothCountText.getText().trim())
								+ KosmikConfiguration.this.mainGearToothCountSlider.getMinimum());
						KosmikConfiguration.this.setGearFactor();
						KosmikConfiguration.this.dialog.enableSaveButton(true);
					}
				});
				this.mainGearToothCountText.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						if (KosmikConfiguration.log.isLoggable(Level.FINEST)) KosmikConfiguration.log.log(Level.FINEST, "mainGearToothCountText.verifyText, event=" + evt); //$NON-NLS-1$
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
			}
			{
				this.mainGearToothCountSlider = new Slider(this, SWT.HORIZONTAL | SWT.BORDER);
				FormData mainGearToothCountSliderLData = new FormData();
				mainGearToothCountSliderLData.width = 100;
				mainGearToothCountSliderLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				mainGearToothCountSliderLData.left = new FormAttachment(0, 1000, 450);
				mainGearToothCountSliderLData.top = new FormAttachment(0, 1000, 145);
				this.mainGearToothCountSlider.setLayoutData(mainGearToothCountSliderLData);
				this.mainGearToothCountSlider.setMaximum(300);
				this.mainGearToothCountSlider.setMinimum(30);
				this.mainGearToothCountSlider.setSelection(70);
				this.mainGearToothCountSlider.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (KosmikConfiguration.log.isLoggable(Level.FINEST)) KosmikConfiguration.log.log(Level.FINEST, "mainGearToothCountSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
						KosmikConfiguration.this.mainGearToothCountText.setText(GDE.STRING_EMPTY + KosmikConfiguration.this.mainGearToothCountSlider.getSelection());
						KosmikConfiguration.this.setGearFactor();
						KosmikConfiguration.this.dialog.enableSaveButton(true);
					}
				});
				this.mainGearToothCountText.setText(GDE.STRING_EMPTY + this.mainGearToothCountSlider.getSelection());
			}
			{
				this.temperatureUnitLabel = new CLabel(this, SWT.NONE);
				this.temperatureUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.temperatureUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGW2810));
				FormData temperatureUnitLabelLData = new FormData();
				temperatureUnitLabelLData.width = 272;
				temperatureUnitLabelLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				temperatureUnitLabelLData.left = new FormAttachment(0, 1000, 125);
				temperatureUnitLabelLData.top = new FormAttachment(0, 1000, 180);
				this.temperatureUnitLabel.setLayoutData(temperatureUnitLabelLData);
			}
			{
				this.temperatureUnitCombo = new CCombo(this, SWT.BORDER);
				this.temperatureUnitCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.temperatureUnitCombo.setItems(new String[] { "  Celsius", " Fahrenheit" });
				FormData temperatureUnitComboLData = new FormData();
				temperatureUnitComboLData.width = 100;
				temperatureUnitComboLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				temperatureUnitComboLData.left = new FormAttachment(0, 1000, 400);
				temperatureUnitComboLData.top = new FormAttachment(0, 1000, 180);
				this.temperatureUnitCombo.setLayoutData(temperatureUnitComboLData);
				this.temperatureUnitCombo.select(0);
				this.temperatureUnitCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (KosmikConfiguration.log.isLoggable(Level.FINEST)) KosmikConfiguration.log.log(Level.FINEST, "temperatureUnitCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						Channel activeChannel = Channels.getInstance().getActiveChannel();
						if (activeChannel != null) {
							double factor = 1.0;
							double offset = 0.0;
							int indexTempFET = KosmikConfiguration.this.isVersion32 ? 6 : 5;
							int indexTempBEC = KosmikConfiguration.this.isVersion32 ? 10 : 9;
							RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
							if (activeRecordSet != null) {
								if (KosmikConfiguration.this.temperatureUnitCombo.getSelectionIndex() == 1) {
									factor = 1.8018;
									offset = 32.0;
									activeRecordSet.get(indexTempFET).setUnit("°F"); //$NON-NLS-1$
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), indexTempFET, "°F"); //$NON-NLS-1$
									activeRecordSet.get(indexTempBEC).setUnit("°F"); //$NON-NLS-1$
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), indexTempBEC, "°F"); //$NON-NLS-1$
								}
								else {
									activeRecordSet.get(indexTempFET).setUnit("°C"); //$NON-NLS-1$
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), indexTempFET, "°C"); //$NON-NLS-1$
									activeRecordSet.get(indexTempBEC).setUnit("°C"); //$NON-NLS-1$
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), indexTempBEC, "°C"); //$NON-NLS-1$
								}
								activeRecordSet.get(indexTempFET).setFactor(factor);
								KosmikConfiguration.this.device.setMeasurementFactor(activeChannel.getNumber(), indexTempFET, factor);
								activeRecordSet.get(indexTempFET).setOffset(offset);
								KosmikConfiguration.this.device.setMeasurementOffset(activeChannel.getNumber(), indexTempFET, offset);
								activeRecordSet.get(indexTempBEC).setFactor(factor);
								KosmikConfiguration.this.device.setMeasurementFactor(activeChannel.getNumber(), indexTempBEC, factor);
								activeRecordSet.get(indexTempBEC).setOffset(offset);
								KosmikConfiguration.this.device.setMeasurementOffset(activeChannel.getNumber(), indexTempBEC, offset);

								KosmikConfiguration.this.device.updateVisibilityStatus(activeRecordSet, false);
								KosmikConfiguration.this.dialog.enableSaveButton(true);
							}
							else {
								if (KosmikConfiguration.this.temperatureUnitCombo.getSelectionIndex() == 1) {
									factor = 1.8018;
									offset = 32.0;
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), indexTempFET, "°F"); //$NON-NLS-1$
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), indexTempBEC, "°F"); //$NON-NLS-1$
								}
								else {
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), indexTempFET, "°C"); //$NON-NLS-1$
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), indexTempBEC, "°C"); //$NON-NLS-1$
								}
								KosmikConfiguration.this.dialog.enableSaveButton(true);
							}
						}
					}
				});
			}
			{
				this.kosmikVersionLabel = new CLabel(this, SWT.NONE);
				this.kosmikVersionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.kosmikVersionLabel.setText("Kosmik Version:"); //$NON-NLS-1$
				FormData kosmikVersionLabelLData = new FormData();
				kosmikVersionLabelLData.width = 272;
				kosmikVersionLabelLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				kosmikVersionLabelLData.left = new FormAttachment(0, 1000, 125);
				kosmikVersionLabelLData.top = new FormAttachment(0, 1000, 225);
				this.kosmikVersionLabel.setLayoutData(kosmikVersionLabelLData);
			}
			{
				this.kosmikVersionCombo = new CCombo(this, SWT.BORDER);
				this.kosmikVersionCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.kosmikVersionCombo.setItems(new String[] { "  <= 3.1", "  >= 3.2" }); //$NON-NLS-1$ //$NON-NLS-2$
				FormData kosmikVersionComboLData = new FormData();
				kosmikVersionComboLData.width = 80;
				kosmikVersionComboLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
				kosmikVersionComboLData.left = new FormAttachment(0, 1000, 400);
				kosmikVersionComboLData.top = new FormAttachment(0, 1000, 225);
				this.kosmikVersionCombo.setLayoutData(kosmikVersionComboLData);
				this.kosmikVersionCombo.select(this.channles.getActiveChannelNumber() - 1);
				this.kosmikVersionCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (KosmikConfiguration.log.isLoggable(Level.FINEST)) KosmikConfiguration.log.log(Level.FINEST, "kosmikVersionCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						KosmikConfiguration.this.channles.switchChannel(KosmikConfiguration.this.channles.getChannelNames()[KosmikConfiguration.this.kosmikVersionCombo.getSelectionIndex()]);
					}
				});
			}
			this.isVersion32 = (this.channles.getActiveChannelNumber() - 1) == 1;
			this.updateGUI();
			this.layout();
			this.addFocusListener(new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent evt) {
					KosmikConfiguration.this.kosmikVersionCombo.select(KosmikConfiguration.this.channles.getActiveChannelNumber() - 1);

				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * switch enable/disable fields depending on motor rotor configuration
	 */
	protected void updateMotorRotor(int selectionIndex) {
		this.motorPinionLabel.setEnabled(selectionIndex == 1);
		this.motorPinionText.setEnabled(selectionIndex == 1);
		this.motorPinionSlider.setEnabled(selectionIndex == 1);
		this.mainGearToothCountLabel.setEnabled(selectionIndex == 1);
		this.mainGearToothCountText.setEnabled(selectionIndex == 1);
		this.mainGearToothCountSlider.setEnabled(selectionIndex == 1);
	}

	protected void updateGUI() {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			//detect if gear configuration
			if (this.device.getMeasurementFactor(activeChannel.getNumber(), 0) > 0.10) { // motor poles only
				this.motorRotorRpmCombo.select(0);
				this.numMotorPolsCombo.select((int) (2 / (this.device.getMeasurementFactor(activeChannel.getNumber(), 0) * 2) - 1));
				this.motorPinionText.setText("10"); //$NON-NLS-1$
				this.motorPinionSlider.setSelection(10);
				double factor = 2.0 / ((KosmikConfiguration.this.numMotorPolsCombo.getSelectionIndex() + 1) * 2);
				int selectIndex = (int) (2 / factor * 2);
				this.mainGearToothCountText.setText(GDE.STRING_EMPTY + (selectIndex * 10));
				this.mainGearToothCountSlider.setSelection(selectIndex);
			}
			else {
				this.motorRotorRpmCombo.select(1);
				int numMotorPoles;
				try {
					numMotorPoles = Integer.valueOf(KosmikConfiguration.this.device.getChannelProperty(ChannelPropertyTypes.NUMBER_POLES).getValue());
				}
				catch (NumberFormatException e) {
					numMotorPoles = 2;
				}
				this.numMotorPolsCombo.select((numMotorPoles / 2) - 1);
				Integer pinion;
				try {
					pinion = Integer.valueOf(KosmikConfiguration.this.device.getChannelProperty(ChannelPropertyTypes.PINION_GEAR).getValue());
				}
				catch (NumberFormatException e) {
					pinion = 10;
				}
				this.motorPinionText.setText(GDE.STRING_EMPTY + pinion); //$NON-NLS-1$
				this.motorPinionSlider.setSelection(pinion - this.motorPinionSlider.getMinimum());
				Integer main;
				try {
					main = Integer.valueOf(KosmikConfiguration.this.device.getChannelProperty(ChannelPropertyTypes.MAIN_GEAR).getValue());
				}
				catch (NumberFormatException e) {
					main = 40;
				}
				this.mainGearToothCountText.setText(GDE.STRING_EMPTY + main);
				this.mainGearToothCountSlider.setSelection(main - this.mainGearToothCountSlider.getMinimum());				
			}
			this.updateMotorRotor(this.motorRotorRpmCombo.getSelectionIndex());

			this.temperatureUnitCombo.select(this.device.getMeasurementUnit(activeChannel.getNumber(), KosmikConfiguration.this.isVersion32 ? 6 : 5).contains("C") ? 0 : 1);
		}
		this.kosmikVersionCombo.select(this.channles.getActiveChannelNumber() - 1);
	}

	/**
	 * set the factor to be used to calculate RPM based on motor poles and related gear tooth counts
	 */
	private void setGearFactor() {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			double factor = 2.0 / ((KosmikConfiguration.this.numMotorPolsCombo.getSelectionIndex() + 1) * 2);
			this.device.setChannelProperty(ChannelPropertyTypes.MAIN_GEAR, DataTypes.INTEGER, GDE.STRING_EMPTY + KosmikConfiguration.this.mainGearToothCountText.getText().trim());
			this.device.setChannelProperty(ChannelPropertyTypes.PINION_GEAR, DataTypes.INTEGER, GDE.STRING_EMPTY + KosmikConfiguration.this.motorPinionText.getText().trim());
			double gearFactor = 1.0 * Integer.valueOf(KosmikConfiguration.this.mainGearToothCountText.getText().trim()) / Integer.valueOf(KosmikConfiguration.this.motorPinionText.getText().trim());
			factor /= gearFactor;
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				activeRecordSet.get(0).setFactor(factor);
				KosmikConfiguration.this.device.setMeasurementFactor(activeChannel.getNumber(), 0, factor);
				KosmikConfiguration.this.device.updateVisibilityStatus(activeRecordSet, false);
			}
			KosmikConfiguration.this.device.setMeasurementFactor(activeChannel.getNumber(), 0, factor);
		}
	}

	/**
	 * set the factor to be used to calculate RPM by selected motor poles only
	 */
	private void setPoleFactor() {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			double factor = 2.0 / ((KosmikConfiguration.this.numMotorPolsCombo.getSelectionIndex() + 1) * 2);
			if (activeRecordSet != null) {
				activeRecordSet.get(0).setFactor(factor);
				KosmikConfiguration.this.device.setMeasurementFactor(activeChannel.getNumber(), 0, factor);
				KosmikConfiguration.this.device.updateVisibilityStatus(activeRecordSet, false);
			}
			KosmikConfiguration.this.device.setMeasurementFactor(activeChannel.getNumber(), 0, factor);
		}
	}
}
