/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.ui.dialog;

import java.util.Properties;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.config.Settings;
import gde.data.ObjectData;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

/**
 * this dialog enable to modify speed limits and associated colors of the KML track
 */
public class GoogleEarthCustomizingDialog extends org.eclipse.swt.widgets.Dialog {
	final static Logger log = Logger.getLogger(GoogleEarthCustomizingDialog.class.getName());
	final DataExplorer  application;
	final Settings			settings;
	final IDevice				device;

	Shell dialogShell;
	Composite valueSelectorComposite;
	Composite colorComposite;
	Text upperLimitText;
	CLabel upperLimitLabel;
	Text avgFactorText;
	CLabel averageFactorLabel;
	Text avgText;
	CLabel averageLabel;
	private Label valueSelectionLabel;
	private Button randomColorButton;
	private Button extrudeButton;
	private Combo valueSelectionCombo;
	Composite fillerComposite;
	Text lowerLimitText;
	CLabel lowerLimitLabel;
	Button upperLimitButton;
	Button withinLimitsButton;
	Button lowerLimitButton;
	Composite limitComposite;
	Button closeButton;
	Composite compositeUpper;
	Composite compositeWithin;
	Composite compositeLower;

	int upperLimitVelocity = 100;
	int lowerLimitVelocity = 20;
	double avgLimitFactor = 2.0;
	RGB withinLimitsColor = SWTResourceManager.getColor(0, 255, 0).getRGB();
	RGB lowerLimitColor = SWTResourceManager.getColor(255, 0, 0).getRGB();
	RGB upperLimitColor = SWTResourceManager.getColor(128, 128, 0).getRGB();

	/**
	* Auto-generated main method to display this
	* org.eclipse.swt.widgets.Dialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			GoogleEarthCustomizingDialog inst = new GoogleEarthCustomizingDialog(shell, SWT.NULL);
			inst.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public GoogleEarthCustomizingDialog(Shell parent, int style) {
		super(parent, style);
		this.application = DataExplorer.getInstance();
		this.settings = Settings.getInstance();
		this.device = this.application.getActiveDevice();
	}

	public void open() {
		try {
			Shell parent = getParent();
			dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			SWTResourceManager.registerResourceUser(dialogShell);
			dialogShell.setText(Messages.getString(MessageIds.GDE_MSGT0283));
			dialogShell.setLayout( new FormLayout());
			dialogShell.layout();
			dialogShell.pack();
			dialogShell.setSize(360, 265);
			this.dialogShell.addListener(SWT.Traverse, new Listener() {
	      @Override
				public void handleEvent(Event event) {
	        switch (event.detail) {
	        case SWT.TRAVERSE_ESCAPE:
	        	GoogleEarthCustomizingDialog.this.dialogShell.close();
	          event.detail = SWT.TRAVERSE_NONE;
	          event.doit = false;
	          break;
	        }
	      }
	    });
			this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/EarthConfigHot.gif")); //$NON-NLS-1$
			dialogShell.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent evt) {
					log.log(Level.FINEST,  "dialogShell.widgetDisposed, event="+evt); //$NON-NLS-1$
					makePersistent();
					application.resetShellIcon();
				}
			});
			{
				valueSelectorComposite = new Composite(dialogShell, SWT.NONE);
				FormData colorCompositeLData = new FormData();
				colorCompositeLData.top =  new FormAttachment(0, 1000, 0);
				colorCompositeLData.left =  new FormAttachment(0, 1000, 5);
				colorCompositeLData.right =  new FormAttachment(1000, 1000, -5);
				colorCompositeLData.height = 55;
				valueSelectorComposite.setLayoutData(colorCompositeLData);
				RowLayout compositeLayout = new RowLayout(SWT.VERTICAL);
				valueSelectorComposite.setLayout(compositeLayout);
				{
					valueSelectionLabel = new Label(valueSelectorComposite, SWT.NONE);
					RowData valueSelectionLabelLData = new RowData(GDE.IS_LINUX ? 200 : 185, 18);
					valueSelectionLabel.setLayoutData(valueSelectionLabelLData);
					valueSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT0875));
				}
				{
					valueSelectionCombo = new Combo(valueSelectorComposite, SWT.NONE);
					RowData valueSelectionComboLData = new RowData(GDE.IS_WINDOWS ? 155 : 170, GDE.IS_MAC ? 26 : 18);
					valueSelectionCombo.setLayoutData(valueSelectionComboLData);
					valueSelectionCombo.setItems(application.getActiveRecordSet().getRecordNames());
					valueSelectionCombo.select(device.getGPS2KMZMeasurementOrdinal());
					valueSelectionCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "valueSelectionCombo.widgetSelected, event="+evt); //$NON-NLS-1$
							device.setGPS2KMZMeasurementOrdinal(valueSelectionCombo.getSelectionIndex());
							avgFactorText.notifyListeners(SWT.KeyUp, new Event());
						}
					});
				}
				{
					extrudeButton = new Button(valueSelectorComposite, SWT.CHECK | SWT.LEFT);
					RowData isExtrudeButtonLData = new RowData(150, 18);
					extrudeButton.setLayoutData(isExtrudeButtonLData);
					extrudeButton.setText(Messages.getString(MessageIds.GDE_MSGT0734));
					extrudeButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0735));
					extrudeButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "extrudeButton.widgetSelected, event="+evt);
							//no extra code needed
						}
					});
				}
				{
					randomColorButton = new Button(valueSelectorComposite, SWT.CHECK | SWT.LEFT);
					RowData randomColorButtonLData = new RowData(150, 18);
					randomColorButton.setLayoutData(randomColorButtonLData);
					randomColorButton.setText(Messages.getString(MessageIds.GDE_MSGT0736));
					randomColorButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0737));
					randomColorButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "randomColorButton.widgetSelected, event="+evt);
							//no extra code needed
						}
					});
				}
			}
			{
				colorComposite = new Composite(dialogShell, SWT.NONE);
				RowLayout compositeLayout = new RowLayout(org.eclipse.swt.SWT.VERTICAL);
				colorComposite.setLayout(compositeLayout);
				FormData colorCompositeLData = new FormData();
				colorCompositeLData.right =  new FormAttachment(450, 1000, 0);
				colorCompositeLData.left =  new FormAttachment(0, 1000, 0);
				colorCompositeLData.width = 157;
				colorCompositeLData.height = 118;
				colorCompositeLData.top =  new FormAttachment(240, 1000, 0);
				colorCompositeLData.bottom =  new FormAttachment(1000, 1000, -45);
				colorComposite.setLayoutData(colorCompositeLData);
				{
					compositeLower = new Composite(colorComposite, SWT.BORDER);
					RowData composite1LData = new RowData();
					composite1LData.width = 145;
					composite1LData.height = 30;
					GridLayout composite1Layout = new GridLayout();
					composite1Layout.makeColumnsEqualWidth = true;
					compositeLower.setLayout(composite1Layout);
					compositeLower.setLayoutData(composite1LData);
					{
						lowerLimitButton = new Button(compositeLower, SWT.PUSH | SWT.CENTER);
						GridData lowerLimitButtonLData = new GridData();
						lowerLimitButtonLData.widthHint = 130;
						lowerLimitButtonLData.heightHint = 20;
						lowerLimitButtonLData.horizontalAlignment = GridData.CENTER;
						lowerLimitButton.setLayoutData(lowerLimitButtonLData);
						lowerLimitButton.setText(Messages.getString(MessageIds.GDE_MSGT0289));
						lowerLimitButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0293));
						lowerLimitButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST,  "lowerLimitButton.widgetSelected, event="+evt); //$NON-NLS-1$
								lowerLimitColor = application.openColorDialog();
								compositeLower.setBackground(SWTResourceManager.getColor(lowerLimitColor.red, lowerLimitColor.green, lowerLimitColor.blue));
							}
						});
					}
				}
				{
					fillerComposite = new Composite(colorComposite, SWT.NONE);
					RowData fillerComposite_RALData = new RowData();
					fillerComposite_RALData.width = 145;
					fillerComposite_RALData.height = 2;
					fillerComposite.setLayoutData(fillerComposite_RALData);
				}
				{
					compositeWithin = new Composite(colorComposite, SWT.BORDER);
					RowData composite2LData = new RowData();
					composite2LData.width = 145;
					composite2LData.height = 30;
					GridLayout composite2Layout = new GridLayout();
					composite2Layout.makeColumnsEqualWidth = true;
					compositeWithin.setLayout(composite2Layout);
					compositeWithin.setLayoutData(composite2LData);
					{
						withinLimitsButton = new Button(compositeWithin, SWT.PUSH | SWT.CENTER);
						withinLimitsButton.setText(Messages.getString(MessageIds.GDE_MSGT0294));
						GridData withinLimitsButtonLData = new GridData();
						withinLimitsButtonLData.horizontalAlignment = GridData.CENTER;
						withinLimitsButtonLData.widthHint = 130;
						withinLimitsButtonLData.heightHint = 20;
						withinLimitsButton.setLayoutData(withinLimitsButtonLData);
						withinLimitsButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0295));
						withinLimitsButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST,  "withinLimitsButton.widgetSelected, event="+evt); //$NON-NLS-1$
								withinLimitsColor = application.openColorDialog();
								compositeWithin.setBackground(SWTResourceManager.getColor(withinLimitsColor.red, withinLimitsColor.green, withinLimitsColor.blue));
							}
						});
					}
				}
				{
					fillerComposite = new Composite(colorComposite, SWT.NONE);
					RowData fillerComposite_RA1LData = new RowData();
					fillerComposite_RA1LData.width = 145;
					fillerComposite_RA1LData.height = 2;
					fillerComposite.setLayoutData(fillerComposite_RA1LData);
				}
				{
					compositeUpper = new Composite(colorComposite, SWT.BORDER);
					RowData composite3LData = new RowData();
					composite3LData.width = 145;
					composite3LData.height = 30;
					GridLayout composite3Layout = new GridLayout();
					composite3Layout.makeColumnsEqualWidth = true;
					compositeUpper.setLayout(composite3Layout);
					compositeUpper.setLayoutData(composite3LData);
					{
						upperLimitButton = new Button(compositeUpper, SWT.PUSH | SWT.CENTER);
						GridData upperLimitButtonLData = new GridData();
						upperLimitButtonLData.widthHint = 130;
						upperLimitButtonLData.heightHint = 20;
						upperLimitButtonLData.horizontalAlignment = GridData.CENTER;
						upperLimitButton.setLayoutData(upperLimitButtonLData);
						upperLimitButton.setText(Messages.getString(MessageIds.GDE_MSGT0296));
						upperLimitButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0297));
						upperLimitButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST,  "upperLimitButton.widgetSelected, event="+evt); //$NON-NLS-1$
								upperLimitColor = application.openColorDialog();
								compositeUpper.setBackground(SWTResourceManager.getColor(upperLimitColor.red, upperLimitColor.green, upperLimitColor.blue));
							}
						});
					}
				}
				colorComposite.layout();
			}
			{
				limitComposite = new Composite(dialogShell, SWT.NONE);
				RowLayout limitCompositeLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				limitComposite.setLayout(limitCompositeLayout);
				FormData limitCompositeLData = new FormData();
				limitCompositeLData.left =  new FormAttachment(461, 1000, 0);
				limitCompositeLData.right =  new FormAttachment(1000, 1000, 0);
				limitCompositeLData.top =  new FormAttachment(240, 1000, 0);
				limitCompositeLData.width = 189;
				limitCompositeLData.height = 118;
				limitCompositeLData.bottom =  new FormAttachment(1000, 1000, -45);
				limitComposite.setLayoutData(limitCompositeLData);
//				{
//					fillerComposite = new Composite(limitComposite, SWT.NONE);
//					RowData composite_IL2LData = new RowData();
//					composite_IL2LData.width = 145;
//					composite_IL2LData.height = 20;
//					fillerComposite.setLayoutData(composite_IL2LData);
//				}
				{
					lowerLimitLabel = new CLabel(limitComposite, SWT.RIGHT);
					RowData lowerLimitLabelLData = new RowData();
					lowerLimitLabelLData.width = 115;
					lowerLimitLabelLData.height = 22;
					lowerLimitLabel.setLayoutData(lowerLimitLabelLData);
					lowerLimitLabel.setText(Messages.getString(MessageIds.GDE_MSGT0284));
					lowerLimitLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0285));
				}
				{
					lowerLimitText = new Text(limitComposite, SWT.SINGLE | SWT.RIGHT | SWT.BORDER);
					RowData lowerLimitTextLData = new RowData();
					lowerLimitTextLData.width = 35;
					lowerLimitTextLData.height = 16;
					lowerLimitText.setLayoutData(lowerLimitTextLData);
					lowerLimitText.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST,  "lowerLimitText.verifyText, event="+evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
				{
					averageFactorLabel = new CLabel(limitComposite, SWT.RIGHT);
					RowData averageFactorLabelLData = new RowData();
					averageFactorLabelLData.width = 115;
					averageFactorLabelLData.height = 22;
					averageFactorLabel.setLayoutData(averageFactorLabelLData);
					averageFactorLabel.setText(Messages.getString(MessageIds.GDE_MSGT0286));
					averageFactorLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0287));
				}
				{
					avgFactorText = new Text(limitComposite, SWT.SINGLE | SWT.RIGHT | SWT.BORDER);
					RowData avgFactorTextLData = new RowData();
					avgFactorTextLData.width = 35;
					avgFactorTextLData.height = 16;
					avgFactorText.setLayoutData(avgFactorTextLData);
					avgFactorText.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST,  "avgFactorText.verifyText, event="+evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, evt.text);
						}
					});
					avgFactorText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent arg0) {
							Integer measurementOrdinal = device.getGPS2KMZMeasurementOrdinal();
							RecordSet activeRecordSet = application.getActiveRecordSet();
							if (activeRecordSet != null && measurementOrdinal != null && measurementOrdinal >= 0) {
								int avgValue = (int) device.translateValue(activeRecordSet.get(measurementOrdinal.intValue()), activeRecordSet.get(measurementOrdinal.intValue()).getAvgValue()/1000.0);
								avgText.setText(GDE.STRING_BLANK + avgValue);
								try {
									double factor = Double.parseDouble(avgFactorText.getText().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT));
									if (factor >= 1) {
										lowerLimitText.setText(String.format("%d", (int) (avgValue / factor))); //$NON-NLS-1$
										upperLimitText.setText(String.format("%d", (int) (avgValue * factor))); //$NON-NLS-1$
										lowerLimitText.setBackground(DataExplorer.getInstance().COLOR_LIGHT_GREY);
									  avgFactorText.setBackground(DataExplorer.getInstance().COLOR_WHITE);
										upperLimitText.setBackground(DataExplorer.getInstance().COLOR_LIGHT_GREY);
									}
									else {
										lowerLimitText.setText(GDE.STRING_EMPTY + lowerLimitVelocity);
										upperLimitText.setText(GDE.STRING_EMPTY + upperLimitVelocity);
										lowerLimitText.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									  avgFactorText.setBackground(DataExplorer.getInstance().COLOR_LIGHT_GREY);
										upperLimitText.setBackground(DataExplorer.getInstance().COLOR_WHITE);
									}
								}
								catch (Exception e) {
									// ignore
								}
							}
							else avgText.setText("0");
						}
					});
				}
				{
					upperLimitLabel = new CLabel(limitComposite, SWT.RIGHT);
					RowData upperLimitLabelLData = new RowData();
					upperLimitLabelLData.width = 115;
					upperLimitLabelLData.height = 22;
					upperLimitLabel.setLayoutData(upperLimitLabelLData);
					upperLimitLabel.setText(Messages.getString(MessageIds.GDE_MSGT0288));
					upperLimitLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0290));
				}
				{
					upperLimitText = new Text(limitComposite, SWT.SINGLE | SWT.RIGHT | SWT.BORDER);
					RowData upperLimitTextLData = new RowData();
					upperLimitTextLData.width = 35;
					upperLimitTextLData.height = 16;
					upperLimitText.setLayoutData(upperLimitTextLData);
					upperLimitText.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST,  "upperLimitText.verifyText, event="+evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
				{
					averageLabel = new CLabel(limitComposite, SWT.RIGHT);
					RowData averageLabelLData = new RowData();
					averageLabelLData.width = 115;
					averageLabelLData.height = 22;
					averageLabel.setLayoutData(averageLabelLData);
					averageLabel.setText(Messages.getString(MessageIds.GDE_MSGT0678));
					averageLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0679));
				}
				{
					avgText = new Text(limitComposite, SWT.SINGLE | SWT.RIGHT | SWT.BORDER);
					RowData avgTextLData = new RowData();
					avgTextLData.width = 35;
					avgTextLData.height = 16;
					avgText.setLayoutData(avgTextLData);
					avgText.setEditable(false);
					Integer measurementOrdinal = device.getGPS2KMZMeasurementOrdinal();
					RecordSet activeRecordSet = application.getActiveRecordSet();
					if (activeRecordSet != null && measurementOrdinal != null && measurementOrdinal >= 0) {
						avgText.setText(String.format(" %d", (int) device.translateValue(activeRecordSet.get(measurementOrdinal.intValue()), activeRecordSet.get(measurementOrdinal.intValue()).getAvgValue()/1000.0)));
					}
					avgText.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0679));
				}
				limitComposite.layout();
			}
			{
				closeButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				closeButton.setText(Messages.getString(MessageIds.GDE_MSGT0291));
				closeButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0292));
				FormData closeButtonLData = new FormData();
				closeButtonLData.height = 25;
				closeButtonLData.left =  new FormAttachment(0, 1000, 70);
				closeButtonLData.right =  new FormAttachment(1000, 1000, -70);
				closeButtonLData.bottom =  new FormAttachment(1000, 1000, -12);
				closeButton.setLayoutData(closeButtonLData);
				closeButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "closeButton.widgetSelected, event="+evt); //$NON-NLS-1$
						dialogShell.dispose();
					}
				});
			}
			initialize();
			this.dialogShell.setLocation(getParent().toDisplay(350, 50));
			dialogShell.open();
			avgFactorText.notifyListeners(SWT.KeyUp, new Event()); //update average value according to selected measurement
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void initialize() {
		lowerLimitVelocity = 20;
		avgLimitFactor = 0.0;
		upperLimitVelocity = 100;
		withinLimitsColor = SWTResourceManager.getColor(0, 255, 0).getRGB();
		lowerLimitColor = SWTResourceManager.getColor(255, 0, 0).getRGB();
		upperLimitColor = SWTResourceManager.getColor(255, 255, 0).getRGB();

		if (application.isObjectoriented()) {
			ObjectData object = application.getObject();
			Properties properties = object.getProperties();
			if (properties != null) {
				try {
					avgLimitFactor = Double.parseDouble(((String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value())).trim());
				}
				catch (Exception e) {
					// ignore
				}
				try {
					lowerLimitVelocity = Integer.parseInt(((String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value())).trim());
				}
				catch (Exception e) {
					// ignore
				}
				try {
					upperLimitVelocity = Integer.parseInt(((String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value())).trim());
				}
				catch (Exception e) {
					// ignore
				}
				int r, g, b;
				try {
					String color = (String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_WITHIN_LIMITS_COLOR.value());
					r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
					g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
					b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
					withinLimitsColor = SWTResourceManager.getColor(r, g, b).getRGB();
				}
				catch (Exception e) {
					// ignore
				}
				try {
					String color = (String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_LOWER_LIMIT_COLOR.value());
					r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
					g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
					b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
					lowerLimitColor = SWTResourceManager.getColor(r, g, b).getRGB();
				}
				catch (Exception e) {
					// ignore
				}
				try {
					String color = (String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_UPPER_LIMIT_COLOR.value());
					r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
					g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
					b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
					upperLimitColor = SWTResourceManager.getColor(r, g, b).getRGB();
				}
				catch (Exception e) {
					// ignore
				}
				if (properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_IS_EXTRUDE.value()) != null)
					extrudeButton.setSelection(Boolean.parseBoolean(properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_IS_EXTRUDE.value()).toString()));
				if (properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_IS_RANDOM_COLOR.value()) != null)
					randomColorButton.setSelection(Boolean.parseBoolean(properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_IS_RANDOM_COLOR.value()).toString()));
			}
		}
		else { //device oriented
			Integer activeChannelNumber = application.getActiveChannelNumber();
			Integer measurementOrdinal = device.getGPS2KMZMeasurementOrdinal();
			if(activeChannelNumber != null && measurementOrdinal != null) {
				PropertyType property = device.getMeasruementProperty(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value());
				if (property != null) {
					try {
						avgLimitFactor = Double.parseDouble(property.getValue());
					}
					catch (NumberFormatException e) {
						// ignore
					}
				}
				property = device.getMeasruementProperty(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value());
				if (property != null) {
					try {
						lowerLimitVelocity = Integer.parseInt(property.getValue());
					}
					catch (Exception e) {
						// ignore
					}
				}
				property = device.getMeasruementProperty(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value());
				if (property != null) {
					try {
						upperLimitVelocity = Integer.parseInt(property.getValue());
					}
					catch (Exception e) {
						// ignore
					}
				}
				int r, g, b;
				property = device.getMeasruementProperty(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_WITHIN_LIMITS_COLOR.value());
				if (property != null) {
					try {
						String color = property.getValue();
						r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
						g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
						b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
						withinLimitsColor = SWTResourceManager.getColor(r, g, b).getRGB();
					}
					catch (Exception e) {
						// ignore
					}
				}
				property = device.getMeasruementProperty(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_LOWER_LIMIT_COLOR.value());
				if (property != null) {
					try {
						String color = property.getValue();
						r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
						g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
						b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
						lowerLimitColor = SWTResourceManager.getColor(r, g, b).getRGB();
					}
					catch (Exception e) {
						// ignore
					}
				}
				property = device.getMeasruementProperty(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_UPPER_LIMIT_COLOR.value());
				if (property != null) {
					try {
						String color = property.getValue();
						r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
						g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
						b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
						upperLimitColor = SWTResourceManager.getColor(r, g, b).getRGB();
					}
					catch (Exception e) {
						// ignore
					}
				}
				property = device.getMeasruementProperty(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_IS_EXTRUDE.value());
				if (property != null) {
					try {
						extrudeButton.setSelection(Boolean.parseBoolean(property.getValue()));
					}
					catch (Exception e) {
						// ignore
					}
				}
				property = device.getMeasruementProperty(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_IS_RANDOM_COLOR.value());
				if (property != null) {
					try {
						randomColorButton.setSelection(Boolean.parseBoolean(property.getValue()));
					}
					catch (Exception e) {
						// ignore
					}
				}
			}
		}

		lowerLimitText.setText(GDE.STRING_EMPTY + lowerLimitVelocity);
	  avgFactorText.setText(String.format("%.1f", avgLimitFactor)); //$NON-NLS-1$
		upperLimitText.setText(GDE.STRING_EMPTY + upperLimitVelocity);
		compositeLower.setBackground(SWTResourceManager.getColor(lowerLimitColor.red,lowerLimitColor.green,lowerLimitColor.blue));
		compositeWithin.setBackground(SWTResourceManager.getColor(withinLimitsColor.red,withinLimitsColor.green,withinLimitsColor.blue));
		compositeUpper.setBackground(SWTResourceManager.getColor(upperLimitColor.red,upperLimitColor.green,upperLimitColor.blue));

		if(avgLimitFactor >= 1) {
			Integer measurementOrdinal = device.getGPS2KMZMeasurementOrdinal();
			RecordSet activeRecordSet = application.getActiveRecordSet();
			if (activeRecordSet != null && measurementOrdinal != null && measurementOrdinal >= 0) {
				int avgValue = (int) device.translateValue(activeRecordSet.get(measurementOrdinal.intValue()), activeRecordSet.get(measurementOrdinal.intValue()).getAvgValue()/1000.0);
				try {
					lowerLimitText.setText(String.format("%d", (int)(avgValue/Double.parseDouble(avgFactorText.getText().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT))))); //$NON-NLS-1$
					upperLimitText.setText(String.format("%d", (int)(avgValue*Double.parseDouble(avgFactorText.getText().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT))))); //$NON-NLS-1$
				}
				catch (Exception e) {
					// ignore
				}
			}
			lowerLimitText.setBackground(this.application.COLOR_LIGHT_GREY);
		  avgFactorText.setBackground(this.application.COLOR_WHITE);
			upperLimitText.setBackground(this.application.COLOR_LIGHT_GREY);
		}
		else {
			lowerLimitText.setBackground(this.application.COLOR_WHITE);
		  avgFactorText.setBackground(this.application.COLOR_LIGHT_GREY);
			upperLimitText.setBackground(this.application.COLOR_WHITE);
		}
	}

	void makePersistent() {
		lowerLimitVelocity = Integer.parseInt(lowerLimitText.getText());
		avgLimitFactor = Double.parseDouble(avgFactorText.getText().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT));
		upperLimitVelocity = Integer.parseInt(upperLimitText.getText());

		if (application.isObjectoriented()) {
			ObjectData object = application.getObject();
			if (object != null) {
				Properties properties = object.getProperties();
				if (properties == null) {
					properties = new Properties();
					object.setProperties(properties);
				}
				if(avgLimitFactor > 0) {
					properties.setProperty(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value(), GDE.STRING_BLANK + avgLimitFactor);
					if (properties.containsKey(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value())) properties.remove(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value());
					if (properties.containsKey(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value())) properties.remove(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value());
				}
				else {
					if (properties.containsKey(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value())) properties.remove(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value());
					properties.setProperty(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value(), GDE.STRING_BLANK + lowerLimitVelocity);
					properties.setProperty(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value(), GDE.STRING_BLANK + upperLimitVelocity);
				}
				properties.setProperty(MeasurementPropertyTypes.GOOGLE_EARTH_WITHIN_LIMITS_COLOR.value(), withinLimitsColor.red + GDE.STRING_COMMA + withinLimitsColor.green + GDE.STRING_COMMA	+ withinLimitsColor.blue);
				properties.setProperty(MeasurementPropertyTypes.GOOGLE_EARTH_LOWER_LIMIT_COLOR.value(), lowerLimitColor.red + GDE.STRING_COMMA + lowerLimitColor.green + GDE.STRING_COMMA	+ lowerLimitColor.blue);
				properties.setProperty(MeasurementPropertyTypes.GOOGLE_EARTH_UPPER_LIMIT_COLOR.value(), upperLimitColor.red + GDE.STRING_COMMA + upperLimitColor.green + GDE.STRING_COMMA	+ upperLimitColor.blue);
				properties.setProperty(MeasurementPropertyTypes.GOOGLE_EARTH_IS_EXTRUDE.value(), extrudeButton.getSelection() ? "true" : "false");
				properties.setProperty(MeasurementPropertyTypes.GOOGLE_EARTH_IS_RANDOM_COLOR.value(), randomColorButton.getSelection() ? "true" : "false");

				object.save();
			}
		}
		else { //device oriented
			Integer activeChannelNumber = application.getActiveChannelNumber();
			Integer measurementOrdinal = device.getGPS2KMZMeasurementOrdinal();
			if(activeChannelNumber != null && measurementOrdinal != null) {
				if(avgLimitFactor >= 1) {
					device.setMeasurementPropertyValue(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value(), DataTypes.DOUBLE, avgLimitFactor);
					device.setMeasurementPropertyValue(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value(), DataTypes.INTEGER, null);
					device.setMeasurementPropertyValue(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value(), DataTypes.INTEGER, null);
				}
				else {
					device.setMeasurementPropertyValue(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value(), DataTypes.DOUBLE, null);
					device.setMeasurementPropertyValue(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value(), DataTypes.INTEGER, lowerLimitVelocity);
					device.setMeasurementPropertyValue(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value(), DataTypes.INTEGER, upperLimitVelocity);
				}
				device.setMeasurementPropertyValue(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_WITHIN_LIMITS_COLOR.value(), DataTypes.STRING, withinLimitsColor.red + GDE.STRING_COMMA + withinLimitsColor.green + GDE.STRING_COMMA + withinLimitsColor.blue);
				device.setMeasurementPropertyValue(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_LOWER_LIMIT_COLOR.value(), DataTypes.STRING, lowerLimitColor.red + GDE.STRING_COMMA + lowerLimitColor.green + GDE.STRING_COMMA + lowerLimitColor.blue);
				device.setMeasurementPropertyValue(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_UPPER_LIMIT_COLOR.value(), DataTypes.STRING, upperLimitColor.red + GDE.STRING_COMMA + upperLimitColor.green + GDE.STRING_COMMA + upperLimitColor.blue);

				device.setMeasurementPropertyValue(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_IS_EXTRUDE.value(), DataTypes.BOOLEAN, extrudeButton.getSelection() ? "true" : "false");
				device.setMeasurementPropertyValue(activeChannelNumber.intValue(), measurementOrdinal.intValue(), MeasurementPropertyTypes.GOOGLE_EARTH_IS_RANDOM_COLOR.value(), DataTypes.BOOLEAN, randomColorButton.getSelection() ? "true" : "false");
			}
			device.storeDeviceProperties();
		}
	}
}
