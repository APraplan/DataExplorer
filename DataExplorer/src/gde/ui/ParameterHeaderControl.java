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

import gde.GDE;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;

public class ParameterHeaderControl {
	final static Logger	log						= Logger.getLogger(ParameterHeaderControl.class.getName());
	final Composite			controlComposite;
	final CLabel				nameLabel, valueLabel, descriptionLabel;
	Composite						separator;

	int									controlHeight	= 20;

	/**
	 * create a header control with parameter name, paameter value, parameter description
	 * @param parent
	 * @param parameterName
	 * @param nameWidth
	 * @param valueDescription
	 * @param valueWidth
	 * @param parameterDescription
	 * @param descriptionWidth
	 * @param height
	 */
	public ParameterHeaderControl(Composite parent, final String parameterName, final int nameWidth, final String valueDescription, final int valueWidth, final String parameterDescription,
			final int descriptionWidth, final int height) {
		RowData separatorLData = new RowData();
		separatorLData.width = 5;
		separatorLData.height = height;
		controlHeight = height;

		controlComposite = new Composite(parent, SWT.NONE);
		RowLayout group1Layout = new RowLayout(SWT.HORIZONTAL);
		controlComposite.setLayout(group1Layout);
		controlComposite.setBackground(SWTResourceManager.getColor(GDE.IS_MAC ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
		{
			Composite filler = new Composite(controlComposite, SWT.NONE);
			filler.setLayoutData(separatorLData);
			filler.setBackground(SWTResourceManager.getColor(GDE.IS_MAC ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
		}
		{
			nameLabel = new CLabel(controlComposite, SWT.CENTER);
			RowData nameLabelLData = new RowData();
			nameLabelLData.width = nameWidth;
			nameLabelLData.height = controlHeight;
			nameLabel.setLayoutData(nameLabelLData);
			nameLabel.setBackground(SWTResourceManager.getColor(GDE.IS_MAC ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
			nameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			nameLabel.setText(parameterName);
		}
		{
			valueLabel = new CLabel(controlComposite, SWT.CENTER);
			RowData valueLabelLData = new RowData();
			valueLabelLData.width = valueWidth;
			valueLabelLData.height = controlHeight;
			valueLabel.setLayoutData(valueLabelLData);
			valueLabel.setBackground(SWTResourceManager.getColor(GDE.IS_MAC ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
			valueLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			valueLabel.setText(valueDescription);
		}
		{
			Composite filler = new Composite(controlComposite, SWT.NONE);
			filler.setLayoutData(separatorLData);
			filler.setBackground(SWTResourceManager.getColor(GDE.IS_MAC ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
		}
		{
			descriptionLabel = new CLabel(controlComposite, SWT.CENTER);
			RowData descriptionLabelLData = new RowData();
			descriptionLabelLData.width = descriptionWidth;
			descriptionLabelLData.height = controlHeight;
			descriptionLabel.setLayoutData(descriptionLabelLData);
			descriptionLabel.setBackground(SWTResourceManager.getColor(GDE.IS_MAC ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
			descriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			descriptionLabel.setText(parameterDescription);
		}
	}
}
