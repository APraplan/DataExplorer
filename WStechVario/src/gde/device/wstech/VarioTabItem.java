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
package gde.device.wstech;

import gde.GDE;
import gde.data.Channels;
import gde.device.IDevice;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * This class represents a tab item of a universal record visualization control
 * @author Winfried Brügmann
 */
public class VarioTabItem extends CTabItem {
	final static Logger							log									= Logger.getLogger(VarioTabItem.class.getName());

	Composite												measurementComposite;
	Button													measurement;
	Button													inputFileButton;
	Composite												buttonComposite;
	Label														measurementUnitLabel;
	Label														measurementSymbolLabel;
	Label														tabItemLabel;
	Composite												mainTabComposite;

	boolean													isVisibilityChanged	= false;

	final CTabFolder								parent;
	final IDevice										device;																																								// get device specific things, get serial port, ...
	final DataExplorer							application;																																						// interaction with application instance
	final Channels									channels;																																							// interaction with channels, source of all records
	final VarioDialog		dialog;
	final int												channelConfigNumber;
	final List<MeasurementControl>	measurementTypes		= new ArrayList<MeasurementControl>();

	public VarioTabItem(CTabFolder parentTabFolder, VarioDialog parentDialog, int useChannelConfigNumber, IDevice useDevice) {
		super(parentTabFolder, SWT.NONE);
		this.parent = parentTabFolder;
		this.dialog = parentDialog;
		this.device = useDevice;
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
		this.mainTabComposite.setLayout(mainTabCompositeLayout);
		this.setControl(this.mainTabComposite);
		{
			this.tabItemLabel = new Label(this.mainTabComposite, SWT.CENTER);
			GridData tabItemLabelLData = new GridData();
			tabItemLabelLData.horizontalAlignment = GridData.BEGINNING;
			tabItemLabelLData.verticalAlignment = GridData.BEGINNING;
			tabItemLabelLData.heightHint = 20;
			tabItemLabelLData.widthHint = 292;
			this.tabItemLabel.setLayoutData(tabItemLabelLData);
			this.tabItemLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE+2, SWT.BOLD));
			this.tabItemLabel.setText(Messages.getString(MessageIds.GDE_MSGT1801));
		}
		{
			// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
			for (int i = 0; i < this.device.getChannelMeasuremtsReplacedNames(this.channelConfigNumber).size(); i++) {
				this.measurementTypes.add(new MeasurementControl(this.mainTabComposite, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremtsReplacedNames(this.channelConfigNumber).get(i), this.device, 1));
			}
		}
		{
			this.buttonComposite = new Composite(this.mainTabComposite, SWT.NONE);
			GridData buttonCompositeLData = new GridData();
			buttonCompositeLData.verticalAlignment = GridData.BEGINNING;
			buttonCompositeLData.horizontalAlignment = GridData.BEGINNING;
			buttonCompositeLData.heightHint = 60;
			buttonCompositeLData.grabExcessHorizontalSpace = true;
			this.buttonComposite.setLayoutData(buttonCompositeLData);
			FormLayout buttonCompositeLayout = new FormLayout();
			this.buttonComposite.setLayout(buttonCompositeLayout);
			{
				this.inputFileButton = new Button(this.buttonComposite, SWT.PUSH | SWT.CENTER);
				FormData inputFileButtonLData = new FormData();
				inputFileButtonLData.width = 188;
				inputFileButtonLData.height = 26;
				inputFileButtonLData.left = new FormAttachment(183, 1000, 0);
				inputFileButtonLData.right = new FormAttachment(827, 1000, 0);
				inputFileButtonLData.top = new FormAttachment(150, 1000, 0);
				inputFileButtonLData.bottom = new FormAttachment(1016, 1000, 0);
				buttonCompositeLData.heightHint = 30;
				buttonCompositeLData.widthHint = 292;
				this.inputFileButton.setLayoutData(inputFileButtonLData);
				this.inputFileButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.inputFileButton.setText(Messages.getString(MessageIds.GDE_MSGT1802));
				this.inputFileButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "inputFileButton.widgetSelected, event=" + evt); 
						if (VarioTabItem.this.isVisibilityChanged) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { VarioTabItem.this.device.getPropertiesFileName() });
							if (VarioTabItem.this.application.openYesNoMessageDialog(VarioTabItem.this.dialog.getDialogShell(), msg) == SWT.YES) {
								log.log(java.util.logging.Level.FINE, "SWT.YES"); 
								VarioTabItem.this.device.storeDeviceProperties();
							}
						}
						VarioTabItem.this.device.open_closeCommPort();
					}
				});
			}
		}
	}
}
