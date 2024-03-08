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
import gde.data.Channels;
import gde.device.IDevice;
import gde.device.smmodellbau.unilog2.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.MeasurementControl;
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
public class UniLog2VisualizationControl extends Composite {
	private static final String	MLINK_EXTEND_ML			= "_ML";																													//$NON-NLS-1$

	final static Logger					log									= Logger.getLogger(UniLog2VisualizationControl.class.getName());

	Composite										measurementComposite;
	Button											measurement;
	Button											inputFileButton;
	Composite										buttonComposite;
	Label												measurementUnitLabel;
	Label												measurementSymbolLabel;
	Label												tabItemLabel;

	boolean											isVisibilityChanged	= false;

	final Widget								parent;
	final IDevice								device;																																							// get device specific things, get serial port, ...
	final DataExplorer					application;																																					// interaction with application instance
	final Channels							channels;																																						// interaction with channels, source of all records
	final UniLog2Dialog					dialog;
	final int										channelConfigNumber;
	final String								typeName;
	final int										measurementCount;
	final int										measurementOffset;
	final List<Composite>				measurementTypes		= new ArrayList<Composite>();

	public UniLog2VisualizationControl(Composite parentComposite, FormData useLayoutData, UniLog2Dialog parentDialog, int useChannelConfigNumber, IDevice useDevice, String useName,
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
			if (this.typeName.equals(Messages.getString(MessageIds.GDE_MSGT2510))) {
				this.tabItemLabel = new Label(this, SWT.CENTER);
				GridData tabItemLabelLData = new GridData();
				tabItemLabelLData.horizontalAlignment = GridData.CENTER;
				tabItemLabelLData.verticalAlignment = GridData.BEGINNING;
				tabItemLabelLData.heightHint = 18;
				tabItemLabelLData.widthHint = 250;
				this.tabItemLabel.setLayoutData(tabItemLabelLData);
				this.tabItemLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 2, SWT.BOLD));
				this.tabItemLabel.setText(this.typeName);
			}
		}
		{
			//0=VoltageRx, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Energy, 6=CellBalance, 7=CellVoltage1, 8=CellVoltage2, 9=CellVoltage3, 
			//10=CellVoltage4, 11=CellVoltage5, 12=CellVoltage6, 13=Revolution, 14=Efficiency, 15=Altitude, 16=Climb, 17=ValueA1, 18=ValueA2, 19=ValueA3,
			//20=AirPressure, 21=InternTemperature, 22=ServoImpuls In, 23=ServoImpuls Out, 
			//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14 39=valAdd15;
			for (int i = this.measurementOffset; i < this.measurementOffset + this.measurementCount; i++) {
				if (this.typeName.startsWith(Messages.getString(MessageIds.GDE_MSGT2512))) {
					this.measurementTypes.add(new MeasurementControlConfigurable(this, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremtsReplacedNames(this.channelConfigNumber).get(i), this.device,
							1, GDE.STRING_BLANK + (i - this.measurementOffset), UniLog2VisualizationControl.MLINK_EXTEND_ML));
				}
				else if (this.typeName.startsWith(Messages.getString(MessageIds.GDE_MSGT2511)) && i > 16 && i < 20) {
					this.measurementTypes.add(new MeasurementControlConfigurable(this, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremtsReplacedNames(this.channelConfigNumber).get(i), this.device,
							1, "A" + (i - 16), GDE.STRING_EMPTY)); //$NON-NLS-1$
				}
				else {
					this.measurementTypes.add(new MeasurementControl(this, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremtsReplacedNames(this.channelConfigNumber).get(i), this.device, 1));
				}
			}
		}
	}
}
