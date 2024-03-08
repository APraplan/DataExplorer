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
package gde.device.simprop;

import gde.GDE;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.MeasurementControlConfigurable;
import gde.ui.SWTResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Widget;

/**
 * This class represents a tab item of a universal record visualization control
 * @author Winfried Brügmann
 */
public class VisualizationControl extends Composite {
	final static Logger					log									= Logger.getLogger(VisualizationControl.class.getName());

	Composite										measurementComposite;
	Button											measurement;
	Button											inputFileButton;
	Composite										buttonComposite;
	Label												measurementUnitLabel;
	Label												measurementSymbolLabel;
	Label												tabItemLabel;
	Composite										mainTabComposite;

	boolean											isVisibilityChanged	= false;

	final Widget								parent;
	final IDevice								device;																																								// get device specific things, get serial port, ...
	final DataExplorer					application;																																					// interaction with application instance
	final Channels							channels;																																							// interaction with channels, source of all records
	final GigaLoggerDialog			dialog;
	final int										channelConfigNumber;
	final String								typeName;
	final int										measurementCount;
	final int										measurementOffset;
	final List<Composite>				measurementTypes		= new ArrayList<Composite>();

	public VisualizationControl(Composite parentComposite, FormData useLayoutData, GigaLoggerDialog parentDialog, int useChannelConfigNumber, IDevice useDevice, String useName,
			int useMeasurementOffset, int useMeasurementCount) {
		super(parentComposite, SWT.NONE);
		this.parent = parentComposite;
		this.dialog = parentDialog;
		this.device = useDevice;
		this.typeName = useName;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.channelConfigNumber = useChannelConfigNumber;
		this.measurementOffset = useMeasurementOffset;
		this.measurementCount = useMeasurementCount;
		this.setLayoutData(useLayoutData);
		GridLayout mainTabCompositeLayout = new GridLayout();
		mainTabCompositeLayout.makeColumnsEqualWidth = true;
		this.setLayout(mainTabCompositeLayout);

		create();
	}

	void create() {
		{
			if (this.typeName.equals(Messages.getString(MessageIds.GDE_MSGT3509))) {
				this.tabItemLabel = new Label(this, SWT.CENTER);
				GridData tabItemLabelLData = new GridData();
				tabItemLabelLData.horizontalAlignment = GridData.CENTER;
				tabItemLabelLData.verticalAlignment = GridData.BEGINNING;
				tabItemLabelLData.heightHint = 18;
				tabItemLabelLData.widthHint = 600;
				this.tabItemLabel.setLayoutData(tabItemLabelLData);
				this.tabItemLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 2, SWT.BOLD));
				this.tabItemLabel.setText(this.typeName);
			}
		}
		{
			this.mainTabComposite = new Composite(this, SWT.NONE);
			GridLayout mainTabCompositeLayout = new GridLayout();
			mainTabCompositeLayout.makeColumnsEqualWidth = true;
			mainTabCompositeLayout.numColumns = 2;
			this.mainTabComposite.setLayout(mainTabCompositeLayout);
			
			RecordSet activeRecordSet = this.application.getActiveChannel().getActiveRecordSet();
			for (int i = this.measurementOffset; i < this.measurementOffset + this.measurementCount; i++) {
				MeasurementType recordMeasurement = this.device.getChannelMeasuremtsReplacedNames(this.channelConfigNumber).get(i);
				if (activeRecordSet != null)
					recordMeasurement.setUnit(activeRecordSet.get(i).getUnit());
				this.measurementTypes.add(new MeasurementControlConfigurable(this.mainTabComposite, this.dialog, this.channelConfigNumber, i, recordMeasurement,
							this.device, 1, GDE.STRING_BLANK + i, GDE.STRING_EMPTY));
			}
		}
	}
}
