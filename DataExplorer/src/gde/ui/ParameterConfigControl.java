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
package gde.ui;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.device.DataTypes;
import gde.utils.StringHelper;

public class ParameterConfigControl {
	final static Logger	log						= Logger.getLogger(ParameterConfigControl.class.getName());
	final Composite			baseComposite;
	final CLabel				nameLabel, descriptionLabel;
	final Text					text;
	final Color					textColor					= SWTResourceManager.getColor(SWT.COLOR_BLACK);
	final Color					textWarningColor	= SWTResourceManager.getColor(SWT.COLOR_RED);
	final Slider				slider;

	final int						controlHeight	= GDE.IS_MAC ? 20 : 18;
	int									sliderMinValue;
	int									sliderMaxValue;
	int									sliderMaxWarningValue;
	int									offset;
	final String				format;

	int									value					= 0;
	String[]						textValues		= new String[0];
	float								devisor				= 1.0f;

	/**
	 * create a parameter configuration control for number values with factor and offset, calculate with total height of 25 to 30
	 * @param parent
	 * @param valueArray
	 * @param valueIndex
	 * @param valueFormat string, if empty no formating like "%d"
	 * @param parameterName
	 * @param nameWidth
	 * @param parameterDescription
	 * @param descriptionWidth
	 * @param isTextValueEditable
	 * @param textFieldWidth
	 * @param sliderWidth
	 * @param sliderMinValue
	 * @param sliderMaxValue
	 * @param sliderOffset
	 * @param isRounding
	 */
	public ParameterConfigControl(final Composite parent, final int[] valueArray, final int valueIndex, final String valueFormat, final String parameterName, final int nameWidth,
			final String parameterDescription, final int descriptionWidth, final boolean isTextValueEditable, final int textFieldWidth, final int sliderWidth, final int sliderMinValue,
			final int sliderMaxValue, final int sliderOffset, final boolean isRounding) {
		this.value = valueArray[valueIndex];
		this.format = valueFormat.equals(GDE.STRING_EMPTY) ? "%d" : valueFormat; //$NON-NLS-1$
		if (this.format.contains(GDE.STRING_DOT)) {
			int startIndex = this.format.indexOf(GDE.CHAR_DOT) + 1;
			int digits = Integer.valueOf(this.format.substring(startIndex, startIndex + 1));
			switch (digits) {
			case 1:
				devisor = 10.0f;
				break;
			case 2:
				devisor = 100.0f;
				break;
			case 3:
				devisor = 1000.0f;
				break;
			default:
				devisor = 1.0f;
				break;
			}
		}
		this.sliderMinValue = sliderMinValue;
		this.sliderMaxValue = sliderMaxValue;
		this.offset = sliderOffset;
		this.baseComposite = new Composite(parent, SWT.NONE);
		RowLayout group1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
		group1Layout.center = true;
		this.baseComposite.setLayout(group1Layout);
		this.baseComposite.setBackground(parent.getBackground());
		{
			this.nameLabel = new CLabel(this.baseComposite, SWT.RIGHT);
			RowData nameLabelLData = new RowData();
			nameLabelLData.width = nameWidth;
			nameLabelLData.height = this.controlHeight;
			this.nameLabel.setLayoutData(nameLabelLData);
			this.nameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.nameLabel.setText(parameterName);
			this.nameLabel.setBackground(parent.getBackground());
		}
		{
			this.text = new Text(this.baseComposite, SWT.CENTER | SWT.BORDER);
			RowData textLData = new RowData();
			textLData.width = textFieldWidth;
			textLData.height = this.controlHeight;
			this.text.setLayoutData(textLData);
			this.text.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.text.setEditable(isTextValueEditable);
			this.text.setBackground(SWTResourceManager.getColor(isTextValueEditable ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
			if (isTextValueEditable) {
				this.text.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						if (ParameterConfigControl.log.isLoggable(Level.FINEST)) ParameterConfigControl.log.log(Level.FINEST, "text.verifyText, event=" + evt); //$NON-NLS-1$
						if (devisor == 1.0)
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						else
							evt.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, evt.text);
					}
				});
				this.text.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						if (ParameterConfigControl.log.isLoggable(Level.FINEST)) ParameterConfigControl.log.log(Level.FINEST, "text.keyReleased, event=" + evt); //$NON-NLS-1$
						ParameterConfigControl.this.value = Integer.parseInt(ParameterConfigControl.this.text.getText().replace(GDE.STRING_DOT, GDE.STRING_EMPTY).replace(GDE.STRING_COMMA, GDE.STRING_EMPTY));
						if (ParameterConfigControl.this.value < ParameterConfigControl.this.sliderMinValue) {
							ParameterConfigControl.this.value = ParameterConfigControl.this.sliderMinValue;
							if (devisor == 1.0)
								ParameterConfigControl.this.text.setText(String.format(Locale.ENGLISH, ParameterConfigControl.this.format, ParameterConfigControl.this.value));
							else
								ParameterConfigControl.this.text.setText(String.format(Locale.ENGLISH, ParameterConfigControl.this.format, ParameterConfigControl.this.value / devisor));
						}
						if (ParameterConfigControl.this.value > ParameterConfigControl.this.sliderMaxValue) {
							ParameterConfigControl.this.value = ParameterConfigControl.this.sliderMaxValue;
							if (devisor == 1.0)
								ParameterConfigControl.this.text.setText(String.format(Locale.ENGLISH, ParameterConfigControl.this.format, ParameterConfigControl.this.value));
							else
								ParameterConfigControl.this.text.setText(String.format(Locale.ENGLISH, ParameterConfigControl.this.format, ParameterConfigControl.this.value / devisor));
						}
						valueArray[valueIndex] = ParameterConfigControl.this.value;
						ParameterConfigControl.this.slider.setSelection(ParameterConfigControl.this.value + ParameterConfigControl.this.offset);
						ParameterConfigControl.this.slider.notifyListeners(SWT.Selection, new Event());
						Event changeEvent = new Event();
						changeEvent.index = valueIndex;
						parent.notifyListeners(SWT.Selection, changeEvent);
					}

					@Override
					public void keyPressed(KeyEvent evt) {
						if (ParameterConfigControl.log.isLoggable(Level.FINEST)) ParameterConfigControl.log.log(Level.FINEST, "text.keyPressed, event=" + evt); //$NON-NLS-1$
					}
				});
			}
		}
		{
			this.descriptionLabel = new CLabel(this.baseComposite, SWT.LEFT);
			RowData descriptionLabelLData = new RowData();
			descriptionLabelLData.width = descriptionWidth;
			descriptionLabelLData.height = this.controlHeight;
			this.descriptionLabel.setLayoutData(descriptionLabelLData);
			this.descriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.descriptionLabel.setText(parameterDescription);
			this.descriptionLabel.setBackground(parent.getBackground());
		}
		{
			RowData sliderLData = new RowData();
			sliderLData.width = sliderWidth;
			sliderLData.height = this.controlHeight;
			this.slider = new Slider(this.baseComposite, SWT.NONE);
			this.slider.setLayoutData(sliderLData);
			this.slider.setMinimum(this.sliderMinValue + this.offset);
			this.slider.setMaximum(this.sliderMaxValue + this.offset + 10);
			this.slider.setIncrement(1);
			this.slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (ParameterConfigControl.log.isLoggable(Level.FINEST)) ParameterConfigControl.log.log(Level.FINEST, "slider.widgetSelected, event=" + evt); //$NON-NLS-1$
					ParameterConfigControl.this.value = ParameterConfigControl.this.slider.getSelection() - ParameterConfigControl.this.offset;
					if (isRounding)
						valueArray[valueIndex] = ParameterConfigControl.this.value > 1000
								? (valueArray[valueIndex] <= ParameterConfigControl.this.value ? ParameterConfigControl.this.value + 49 : ParameterConfigControl.this.value - 40) / 50 * 50
								: (valueArray[valueIndex] <= ParameterConfigControl.this.value ? ParameterConfigControl.this.value + 9 : ParameterConfigControl.this.value - 5) / 10 * 10;
					else
						valueArray[valueIndex] = ParameterConfigControl.this.value;

					if (devisor == 1.0)
						ParameterConfigControl.this.text.setText(String.format(Locale.ENGLISH, ParameterConfigControl.this.format, ParameterConfigControl.this.value));
					else
						ParameterConfigControl.this.text.setText(String.format(Locale.ENGLISH, ParameterConfigControl.this.format, ParameterConfigControl.this.value / devisor));

					if (evt.data == null) {
						Event changeEvent = new Event();
						changeEvent.index = valueIndex;
						parent.notifyListeners(SWT.Selection, changeEvent);
					}
				}
			});
		}
	}

	/**
	 * create a parameter configuration control for number values with factor and offset, calculate with total height of 25 to 30
	 * @param parent
	 * @param valueArray
	 * @param valueIndex
	 * @param valueFormat string, if empty no formating like "%d"
	 * @param parameterName
	 * @param nameWidth
	 * @param parameterDescription
	 * @param descriptionWidth
	 * @param isTextValueEditable
	 * @param textFieldWidth
	 * @param sliderWidth
	 * @param sliderMinValue
	 * @param sliderMaxValue
	 * @param sliderMaxWarningValue
	 * @param sliderOffset
	 * @param isRounding
	 */
	public ParameterConfigControl(final Composite parent, final int[] valueArray, final int valueIndex, final String valueFormat, final String parameterName, final int nameWidth,
			final String parameterDescription, final int descriptionWidth, final boolean isTextValueEditable, final int textFieldWidth, final int sliderWidth, final int sliderMinValue,
			final int sliderMaxValue, final int sliderMaxWarningValue, final int sliderOffset, final boolean isRounding) {
		this.value = valueArray[valueIndex];
		this.format = valueFormat.equals(GDE.STRING_EMPTY) ? "%d" : valueFormat; //$NON-NLS-1$
		if (this.format.contains(GDE.STRING_DOT)) {
			int startIndex = this.format.indexOf(GDE.CHAR_DOT) + 1;
			int digits = Integer.valueOf(this.format.substring(startIndex, startIndex + 1));
			switch (digits) {
			case 1:
				devisor = 10.0f;
				break;
			case 2:
				devisor = 100.0f;
				break;
			case 3:
				devisor = 1000.0f;
				break;
			default:
				devisor = 1.0f;
				break;
			}
		}
		this.sliderMinValue = sliderMinValue;
		this.sliderMaxValue = sliderMaxValue;
		this.sliderMaxWarningValue = sliderMaxWarningValue;
		this.offset = sliderOffset;
		this.baseComposite = new Composite(parent, SWT.NONE);
		RowLayout group1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
		group1Layout.center = true;
		this.baseComposite.setLayout(group1Layout);
		this.baseComposite.setBackground(parent.getBackground());
		{
			this.nameLabel = new CLabel(this.baseComposite, SWT.RIGHT);
			RowData nameLabelLData = new RowData();
			nameLabelLData.width = nameWidth;
			nameLabelLData.height = this.controlHeight;
			this.nameLabel.setLayoutData(nameLabelLData);
			this.nameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.nameLabel.setText(parameterName);
			this.nameLabel.setBackground(parent.getBackground());
		}
		{
			this.text = new Text(this.baseComposite, SWT.CENTER | SWT.BORDER);
			RowData textLData = new RowData();
			textLData.width = textFieldWidth;
			textLData.height = this.controlHeight;
			this.text.setLayoutData(textLData);
			this.text.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.text.setEditable(isTextValueEditable);
			this.text.setBackground(SWTResourceManager.getColor(isTextValueEditable ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
			this.text.setForeground(textColor);
			if (isTextValueEditable) {
				this.text.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						if (ParameterConfigControl.log.isLoggable(Level.FINEST)) ParameterConfigControl.log.log(Level.FINEST, "text.verifyText, event=" + evt); //$NON-NLS-1$
						if (devisor == 1.0)
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						else
							evt.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, evt.text);
					}
				});
				this.text.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						if (ParameterConfigControl.log.isLoggable(Level.FINEST)) ParameterConfigControl.log.log(Level.FINEST, "text.keyReleased, event=" + evt); //$NON-NLS-1$
						ParameterConfigControl.this.value = Integer.parseInt(ParameterConfigControl.this.text.getText().replace(GDE.STRING_DOT, GDE.STRING_EMPTY).replace(GDE.STRING_COMMA, GDE.STRING_EMPTY));
						if (ParameterConfigControl.this.value < ParameterConfigControl.this.sliderMinValue) {
							ParameterConfigControl.this.value = ParameterConfigControl.this.sliderMinValue;
							if (devisor == 1.0)
								ParameterConfigControl.this.text.setText(String.format(Locale.ENGLISH, ParameterConfigControl.this.format, ParameterConfigControl.this.value));
							else
								ParameterConfigControl.this.text.setText(String.format(Locale.ENGLISH, ParameterConfigControl.this.format, ParameterConfigControl.this.value / devisor));
						}
						if (ParameterConfigControl.this.value > ParameterConfigControl.this.sliderMaxValue) {
							ParameterConfigControl.this.value = ParameterConfigControl.this.sliderMaxValue;
							if (devisor == 1.0)
								ParameterConfigControl.this.text.setText(String.format(Locale.ENGLISH, ParameterConfigControl.this.format, ParameterConfigControl.this.value));
							else
								ParameterConfigControl.this.text.setText(String.format(Locale.ENGLISH, ParameterConfigControl.this.format, ParameterConfigControl.this.value / devisor));
						}
						if (ParameterConfigControl.this.value > ParameterConfigControl.this.sliderMaxWarningValue)
							ParameterConfigControl.this.text.setForeground(textWarningColor);
						else 
							ParameterConfigControl.this.text.setForeground(textColor);
				
						valueArray[valueIndex] = ParameterConfigControl.this.value;
						ParameterConfigControl.this.slider.setSelection(ParameterConfigControl.this.value + ParameterConfigControl.this.offset);
						ParameterConfigControl.this.slider.notifyListeners(SWT.Selection, new Event());
						Event changeEvent = new Event();
						changeEvent.index = valueIndex;
						parent.notifyListeners(SWT.Selection, changeEvent);
					}

					@Override
					public void keyPressed(KeyEvent evt) {
						if (ParameterConfigControl.log.isLoggable(Level.FINEST)) ParameterConfigControl.log.log(Level.FINEST, "text.keyPressed, event=" + evt); //$NON-NLS-1$
					}
				});
			}
		}
		{
			this.descriptionLabel = new CLabel(this.baseComposite, SWT.LEFT);
			RowData descriptionLabelLData = new RowData();
			descriptionLabelLData.width = descriptionWidth;
			descriptionLabelLData.height = this.controlHeight;
			this.descriptionLabel.setLayoutData(descriptionLabelLData);
			this.descriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.descriptionLabel.setText(parameterDescription);
			this.descriptionLabel.setBackground(parent.getBackground());
		}
		{
			RowData sliderLData = new RowData();
			sliderLData.width = sliderWidth;
			sliderLData.height = this.controlHeight;
			this.slider = new Slider(this.baseComposite, SWT.NONE);
			this.slider.setLayoutData(sliderLData);
			this.slider.setMinimum(this.sliderMinValue + this.offset);
			this.slider.setMaximum(this.sliderMaxValue + this.offset + 10);
			this.slider.setIncrement(1);
			this.slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (ParameterConfigControl.log.isLoggable(Level.FINEST)) ParameterConfigControl.log.log(Level.FINEST, "slider.widgetSelected, event=" + evt); //$NON-NLS-1$
					ParameterConfigControl.this.value = ParameterConfigControl.this.slider.getSelection() - ParameterConfigControl.this.offset;
					if (isRounding)
						valueArray[valueIndex] = ParameterConfigControl.this.value > 1000
								? (valueArray[valueIndex] <= ParameterConfigControl.this.value ? ParameterConfigControl.this.value + 49 : ParameterConfigControl.this.value - 40) / 50 * 50
								: (valueArray[valueIndex] <= ParameterConfigControl.this.value ? ParameterConfigControl.this.value + 9 : ParameterConfigControl.this.value - 5) / 10 * 10;
					else
						valueArray[valueIndex] = ParameterConfigControl.this.value;

					if (devisor == 1.0)
						ParameterConfigControl.this.text.setText(String.format(Locale.ENGLISH, ParameterConfigControl.this.format, ParameterConfigControl.this.value));
					else
						ParameterConfigControl.this.text.setText(String.format(Locale.ENGLISH, ParameterConfigControl.this.format, ParameterConfigControl.this.value / devisor));

					if (ParameterConfigControl.this.value > ParameterConfigControl.this.sliderMaxWarningValue)
						ParameterConfigControl.this.text.setForeground(textWarningColor);
					else 
						ParameterConfigControl.this.text.setForeground(textColor);

					if (evt.data == null) {
						Event changeEvent = new Event();
						changeEvent.index = valueIndex;
						parent.notifyListeners(SWT.Selection, changeEvent);
					}
				}
			});
		}
	}

	/**
	 * create a parameter configuration control for number values with factor and without offset, calculate with total height of 25 to 30
	 * @param parent
	 * @param valueArray
	 * @param valueIndex
	 * @param valueFormat string, if empty no formating like "%d"
	 * @param parameterName
	 * @param nameWidth
	 * @param parameterDescription
	 * @param descriptionWidth
	 * @param isTextValueEditable
	 * @param textFieldWidth
	 * @param sliderWidth
	 * @param sliderMinValue
	 * @param sliderMaxValue
	 */
	public ParameterConfigControl(final Composite parent, final int[] valueArray, final int valueIndex, final String valueFormat, final String parameterName, final int nameWidth,
			final String parameterDescription, final int descriptionWidth, final boolean isTextValueEditable, final int textFieldWidth, final int sliderWidth, final int sliderMinValue,
			final int sliderMaxValue) {
		this.value = valueArray[valueIndex];
		this.format = valueFormat.equals(GDE.STRING_EMPTY) ? "%d" : valueFormat; //$NON-NLS-1$
		this.sliderMinValue = sliderMinValue;
		this.sliderMaxValue = sliderMaxValue;
		this.offset = 0;
		this.baseComposite = new Composite(parent, SWT.NONE);
		RowLayout group1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
		group1Layout.center = true;
		this.baseComposite.setLayout(group1Layout);
		this.baseComposite.setBackground(parent.getBackground());
		{
			this.nameLabel = new CLabel(this.baseComposite, SWT.RIGHT);
			RowData nameLabelLData = new RowData();
			nameLabelLData.width = nameWidth;
			nameLabelLData.height = this.controlHeight;
			this.nameLabel.setLayoutData(nameLabelLData);
			this.nameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.nameLabel.setText(parameterName);
			this.nameLabel.setBackground(parent.getBackground());
		}
		{
			this.text = new Text(this.baseComposite, SWT.CENTER | SWT.BORDER);
			RowData textLData = new RowData();
			textLData.width = textFieldWidth;
			textLData.height = this.controlHeight;
			this.text.setLayoutData(textLData);
			this.text.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.text.setEditable(isTextValueEditable);
			this.text.setBackground(SWTResourceManager.getColor(isTextValueEditable ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
			if (isTextValueEditable) {
				this.text.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						if (ParameterConfigControl.log.isLoggable(Level.FINEST)) ParameterConfigControl.log.log(Level.FINEST, "text.verifyText, event=" + evt); //$NON-NLS-1$
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
				this.text.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						if (ParameterConfigControl.log.isLoggable(Level.FINEST)) ParameterConfigControl.log.log(Level.FINEST, "text.keyReleased, event=" + evt); //$NON-NLS-1$
						ParameterConfigControl.this.value = Integer.parseInt(ParameterConfigControl.this.text.getText());
						if (ParameterConfigControl.this.value < ParameterConfigControl.this.sliderMinValue) {
							ParameterConfigControl.this.value = ParameterConfigControl.this.sliderMinValue;
							ParameterConfigControl.this.text.setText(String.format(ParameterConfigControl.this.format, ParameterConfigControl.this.value));
						}
						if (ParameterConfigControl.this.value > ParameterConfigControl.this.sliderMaxValue) {
							ParameterConfigControl.this.value = ParameterConfigControl.this.sliderMaxValue;
							ParameterConfigControl.this.text.setText(String.format(ParameterConfigControl.this.format, ParameterConfigControl.this.value));
						}
						valueArray[valueIndex] = ParameterConfigControl.this.value;
						ParameterConfigControl.this.slider.setSelection(ParameterConfigControl.this.value);
						ParameterConfigControl.this.slider.notifyListeners(SWT.Selection, new Event());
						Event changeEvent = new Event();
						changeEvent.index = valueIndex;
						parent.notifyListeners(SWT.Selection, changeEvent);
					}

					@Override
					public void keyPressed(KeyEvent evt) {
						if (ParameterConfigControl.log.isLoggable(Level.FINEST)) ParameterConfigControl.log.log(Level.FINEST, "text.keyPressed, event=" + evt); //$NON-NLS-1$
					}
				});
			}
		}
		{
			this.descriptionLabel = new CLabel(this.baseComposite, SWT.LEFT);
			RowData descriptionLabelLData = new RowData();
			descriptionLabelLData.width = descriptionWidth;
			descriptionLabelLData.height = this.controlHeight;
			this.descriptionLabel.setLayoutData(descriptionLabelLData);
			this.descriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.descriptionLabel.setText(parameterDescription);
			this.descriptionLabel.setBackground(parent.getBackground());
		}
		{
			RowData sliderLData = new RowData();
			sliderLData.width = sliderWidth;
			sliderLData.height = this.controlHeight;
			this.slider = new Slider(this.baseComposite, SWT.NONE);
			this.slider.setLayoutData(sliderLData);
			this.slider.setMinimum(this.sliderMinValue);
			this.slider.setMaximum(this.sliderMaxValue + 10);
			this.slider.setIncrement(1);
			this.slider.setSelection(this.value);
			this.slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (ParameterConfigControl.log.isLoggable(Level.FINEST)) ParameterConfigControl.log.log(Level.FINEST, "slider.widgetSelected, event=" + evt); //$NON-NLS-1$
					ParameterConfigControl.this.value = ParameterConfigControl.this.slider.getSelection();
					ParameterConfigControl.this.text.setText(String.format(ParameterConfigControl.this.format, ParameterConfigControl.this.value));
					valueArray[valueIndex] = ParameterConfigControl.this.value;
					if (evt.data == null) {
						Event changeEvent = new Event();
						changeEvent.index = valueIndex;
						parent.notifyListeners(SWT.Selection, changeEvent);
					}
				}
			});
		}
	}

	/**
	 * create a parameter configuration control for values array, where the array size drives the slider, calculate with total height of 25 to 30
	 * @param parent
	 * @param valueArray
	 * @param valueIndex
	 * @param nameWidth
	 * @param parameterDescription
	 * @param descriptionWidth
	 * @param textFiledValues
	 * @param textFieldWidth
	 * @param sliderWidth
	 */
	public ParameterConfigControl(final Composite parent, final int[] valueArray, final int valueIndex, final String parameterName, final int nameWidth, final String parameterDescription,
			final int descriptionWidth, final String[] textFiledValues, final int textFieldWidth, final int sliderWidth) {
		this.value = valueArray[valueIndex];
		this.textValues = textFiledValues;
		this.format = GDE.STRING_EMPTY;
		this.offset = 0;
		this.baseComposite = new Composite(parent, SWT.NONE);
		RowLayout group1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
		group1Layout.center = true;
		this.baseComposite.setLayout(group1Layout);
		this.baseComposite.setBackground(parent.getBackground());
		{
			this.nameLabel = new CLabel(this.baseComposite, SWT.RIGHT);
			RowData nameLabelLData = new RowData();
			nameLabelLData.width = nameWidth;
			nameLabelLData.height = this.controlHeight;
			this.nameLabel.setLayoutData(nameLabelLData);
			this.nameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.nameLabel.setText(parameterName);
			this.nameLabel.setBackground(parent.getBackground());
		}
		{
			this.text = new Text(this.baseComposite, SWT.CENTER | SWT.BORDER);
			RowData textLData = new RowData();
			textLData.width = textFieldWidth;
			textLData.height = this.controlHeight;
			this.text.setLayoutData(textLData);
			this.text.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.text.setEditable(false);
			this.text.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
		}
		{
			this.descriptionLabel = new CLabel(this.baseComposite, SWT.LEFT);
			RowData descriptionLabelLData = new RowData();
			descriptionLabelLData.width = descriptionWidth;
			descriptionLabelLData.height = this.controlHeight;
			this.descriptionLabel.setLayoutData(descriptionLabelLData);
			this.descriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.descriptionLabel.setText(parameterDescription);
			this.descriptionLabel.setBackground(parent.getBackground());
		}
		{
			RowData sliderLData = new RowData();
			sliderLData.width = sliderWidth;
			sliderLData.height = this.controlHeight;
			this.slider = new Slider(this.baseComposite, SWT.NONE);
			this.slider.setLayoutData(sliderLData);
			this.slider.setMinimum(0);
			this.slider.setMaximum(this.textValues.length < 15 ? 10 + this.textValues.length - 1 : this.textValues.length + 1);
			this.slider.setSelection(this.value);
			this.slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (ParameterConfigControl.log.isLoggable(Level.FINEST)) ParameterConfigControl.log.log(Level.FINEST, "slider.widgetSelected, event=" + evt); //$NON-NLS-1$
					ParameterConfigControl.this.value = ParameterConfigControl.this.slider.getSelection();
					ParameterConfigControl.this.text.setText(ParameterConfigControl.this.textValues[ParameterConfigControl.this.value]);
					valueArray[valueIndex] = ParameterConfigControl.this.value;
					if (evt.data == null) {
						Event changeEvent = new Event();
						changeEvent.index = valueIndex;
						parent.notifyListeners(SWT.Selection, changeEvent);
					}
				}
			});
		}
	}

	public ParameterConfigControl dispose() {
		this.baseComposite.dispose();
		return null;
	}

	public void setEnabled(final boolean enable) {
		this.nameLabel.setForeground(enable ? DataExplorer.getInstance().COLOR_BLACK : DataExplorer.getInstance().COLOR_GREY);
		this.text.setEnabled(enable);
		this.descriptionLabel.setForeground(enable ? DataExplorer.getInstance().COLOR_BLACK : DataExplorer.getInstance().COLOR_GREY);
		this.slider.setEnabled(enable);
	}

	/**
	 * set the slider selection index
	 * @param useValue the value to be set in dependency of factor and offset if applicable
	 */
	public void setSliderSelection(int useValue) {
		this.value = useValue;
		if (!this.slider.isDisposed()) {
			this.slider.setSelection(this.value + this.offset);
			if (ParameterConfigControl.log.isLoggable(java.util.logging.Level.FINE))
				ParameterConfigControl.log.log(java.util.logging.Level.FINE, "slider value = " + this.value + " offset = " + this.offset);
			Event updateEvent = new Event();
			updateEvent.data = new Object();
			this.slider.notifyListeners(SWT.Selection, updateEvent);
		}
	}

	/**
	 * @return the slider
	 */
	public Slider getSlider() {
		return this.slider;
	}

	/**
	 * redraw the control widget
	 */
	public void redraw() {
		this.baseComposite.redraw();
	}

	/**
	 * update control description and slider range (cell type change -> charge max capacity)
	 * @param newParameterDescription
	 * @param newMinSliderValue
	 * @param newMaxSliderValue
	 */
	public void updateValueRange(String newParameterDescription, int newMinSliderValue, int newMaxSliderValue) {
		this.descriptionLabel.setText(newParameterDescription);
		this.sliderMinValue = newMinSliderValue;
		this.slider.setMinimum(newMinSliderValue);
		this.sliderMaxValue = newMaxSliderValue;
		this.slider.setMaximum(newMaxSliderValue);
	}

	/**
	 * update control description and slider range (cell type change -> charge max capacity)
	 * @param newParameterDescription
	 * @param newMinSliderValue
	 * @param newMaxSliderValue
	 */
	public void updateValueRange(String newParameterDescription, int newMinSliderValue, int newMaxSliderValue, int newOffset) {
		this.descriptionLabel.setText(newParameterDescription);
		this.offset = newOffset;
		this.sliderMinValue = newMinSliderValue;
		this.slider.setMinimum(newMinSliderValue + this.offset);
		this.sliderMaxValue = newMaxSliderValue;
		this.slider.setMaximum(newMaxSliderValue + this.offset + 10);
	}

	/**
	 * update control description and slider range (cell type change -> charge max capacity)
	 * @param newParameterDescription
	 * @param newMinSliderValue
	 * @param newMaxSliderValue
	 */
	public void updateValueRange(String newParameterDescription, int newMinSliderValue, int newMaxSliderValue, int newSliderMaxWarningValue, int newOffset) {
		this.descriptionLabel.setText(newParameterDescription);
		this.offset = newOffset;
		this.sliderMinValue = newMinSliderValue;
		this.slider.setMinimum(newMinSliderValue + this.offset);
		this.sliderMaxValue = newMaxSliderValue;
		this.slider.setMaximum(newMaxSliderValue + this.offset + 10);
		this.sliderMaxWarningValue = newSliderMaxWarningValue;
	}

	public void updateTextFieldValues(final String[] textFiledValues) {
		this.descriptionLabel.setText(String.join(",", textFiledValues));
		this.textValues = textFiledValues;
		this.sliderMaxValue = this.textValues.length < 10 ? 10 + this.textValues.length - 1 : this.textValues.length + 1;
		this.slider.setMaximum(this.sliderMaxValue);
		this.value = ParameterConfigControl.this.slider.getSelection();
		this.text.setText(this.textValues[ParameterConfigControl.this.value]);
	}

	public void updateNameLabel(final String newNameText) {
		this.nameLabel.setText(newNameText);
	}
}
