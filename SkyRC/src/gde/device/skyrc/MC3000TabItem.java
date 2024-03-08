/**************************************************************************************
  	This file is part of DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.skyrc;

import gde.GDE;
import gde.data.Channels;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.MeasurementControl;
import gde.ui.SWTResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * This class represents a tab item of a universal record visualization control
 * @author Winfried Brügmann
 */
public class MC3000TabItem extends CTabItem {
	final static Logger							log									= Logger.getLogger(MC3000TabItem.class.getName());

	Composite												measurementComposite;
	Button													measurement;
	Button													inputFileButton;
	Composite												buttonComposite;
	Button													stopCollectDataButton;
	Button													startCollectDataButton;
	Label														measurementUnitLabel;
	Label														measurementSymbolLabel;
	Label														tabItemLabel;
	Composite												mainTabComposite;

	boolean													isVisibilityChanged	= false;

	final CTabFolder								parent;
	final MC3000									device;																																								// get device specific things, get serial port, ...
	final MC3000UsbPort				serialPort;																																						// get device serial port, ...
	final DataExplorer							application;																																					// interaction with application instance
	final Channels									channels;																																							// interaction with channels, source of all records
	final MC3000Dialog						dialog;
	final int												channelConfigNumber;
	final List<MeasurementControl>	measurementTypes		= new ArrayList<MeasurementControl>();

	public MC3000TabItem(CTabFolder parentTabFolder, MC3000Dialog parentDialog, int useChannelConfigNumber, MC3000 useDevice) {
		super(parentTabFolder, SWT.NONE);
		this.parent = parentTabFolder;
		this.dialog = parentDialog;
		this.device = useDevice;
		this.serialPort = useDevice.getCommunicationPort();
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.channelConfigNumber = useChannelConfigNumber;
		this.setText(this.device.getChannelNameReplacement(this.channelConfigNumber));

		create();
	}

	void create() {
		this.mainTabComposite = new Composite(this.parent, SWT.NONE);
		GridLayout mainTabCompositeLayout = new GridLayout();
		mainTabCompositeLayout.makeColumnsEqualWidth = true;
		mainTabCompositeLayout.numColumns = 2;
		this.mainTabComposite.setLayout(mainTabCompositeLayout);
		this.setControl(this.mainTabComposite);
		{
			this.tabItemLabel = new Label(this.mainTabComposite, SWT.CENTER);
			GridData tabItemLabelLData = new GridData();
			tabItemLabelLData.horizontalSpan = 2;
			tabItemLabelLData.grabExcessHorizontalSpace = true;
			tabItemLabelLData.horizontalAlignment = GridData.CENTER;
			tabItemLabelLData.verticalAlignment = GridData.BEGINNING;
			tabItemLabelLData.heightHint = 25;
			tabItemLabelLData.widthHint = 595;
			this.tabItemLabel.setLayoutData(tabItemLabelLData);
			this.tabItemLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE+2, SWT.BOLD));
			this.tabItemLabel.setText(Messages.getString(MessageIds.GDE_MSGT3601));
		}
		{
			//measurements
			for (int i = 0; i < this.device.getChannelMeasuremtsReplacedNames(this.channelConfigNumber).size(); i++) {
				this.measurementTypes.add(new MeasurementControl(this.mainTabComposite, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremtsReplacedNames(this.channelConfigNumber).get(i), this.device, i==10?2:1));
			}
		}
	}
}
