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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.ui.dialog.edit;

import gde.GDE;
import gde.device.ChannelType;
import gde.device.ChannelTypes;
import gde.device.DeviceConfiguration;
import gde.device.MeasurementType;
import gde.device.ObjectFactory;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * class defining a CTabItem with ChannelType configuration data
 * @author Winfried Brügmann
 */
public class ChannelTypeTabItem extends CTabItem implements Cloneable {
	final static Logger						log								= Logger.getLogger(ChannelTypeTabItem.class.getName());

	Composite											channelConfigComposite;
	Button												channelConfigAddButton;
	Label													channelConfigLabel;
	CCombo												channelConfigTypeCombo;
	Text													channelConfigText;
	CTabFolder										measurementsTabFolder;

	ChannelTypes									channelConfigType	= ChannelTypes.TYPE_OUTLET;
	String												channelConfigName	= Messages.getString(MessageIds.GDE_MSGT0527);

	final CTabFolder							channelConfigInnerTabFolder;
	final DevicePropertiesEditor	propsEditor;
	final String									tabName;
	DeviceConfiguration						deviceConfig;
	int														channelConfigNumber;
	ChannelType										channelType;

	public ChannelTypeTabItem(CTabFolder parent, int style, int index) {
		super(parent, style);
		this.channelConfigInnerTabFolder = parent;
		this.propsEditor = DevicePropertiesEditor.getInstance();
		this.tabName = GDE.STRING_BLANK + (index + 1) + GDE.STRING_BLANK;
		log.log(java.util.logging.Level.FINE, "ChannelTypeTabItem " + this.tabName); //$NON-NLS-1$
		initGUI();
	}

	/**
	 * @param useDeviceConfig the deviceConfig to set
	 * @param useChannelType the ChannelType to set
	 */
	public void setChannelType(DeviceConfiguration useDeviceConfig, ChannelType useChannelType, int useChannelConfigNumber) {
		log.log(java.util.logging.Level.FINE, "ChannelTypeTabItem.setChannelType"); //$NON-NLS-1$
		this.deviceConfig = useDeviceConfig;
		this.channelType = useChannelType;
		this.channelConfigNumber = useChannelConfigNumber;
		this.channelConfigComposite.redraw();
		this.setText(GDE.STRING_BLANK + ChannelTypeTabItem.this.channelConfigNumber + GDE.STRING_BLANK + this.channelType.getName());

		if (this.channelConfigNumber == 1) setupMeasurementItems();

		initialize();
		this.channelConfigInnerTabFolder.setSelection(0);
	}

	/**
	 * setup the measurement items for the actual channel tab item
	 */
	public void setupMeasurementItems() {
		//MeasurementType begin
		int measurementTypeCount = this.channelType.getMeasurement().size();
		int actualTabItemCount = this.measurementsTabFolder.getItemCount();
		if (measurementTypeCount < actualTabItemCount) {
			for (int i = measurementTypeCount; i < actualTabItemCount; i++) {
				MeasurementTypeTabItem tmpMeasurementTypeTabItem = (MeasurementTypeTabItem) this.measurementsTabFolder.getItem(measurementTypeCount);
				if (tmpMeasurementTypeTabItem.measurementPropertiesTabFolder != null) { // dispose PropertyTypes
					for (CTabItem tmpPropertyTypeTabItem : tmpMeasurementTypeTabItem.measurementPropertiesTabFolder.getItems()) {
						((PropertyTypeTabItem) tmpPropertyTypeTabItem).dispose();
					}
					tmpMeasurementTypeTabItem.measurementPropertiesTabItem.dispose();
				}
				if (tmpMeasurementTypeTabItem.statisticsTypeTabItem != null) { // dispose StatisticsType
					tmpMeasurementTypeTabItem.statisticsTypeTabItem.dispose();
				}
				tmpMeasurementTypeTabItem.dispose();
			}
		}
		else if (measurementTypeCount > actualTabItemCount) {
			for (int i = actualTabItemCount; i < measurementTypeCount; i++) {
				new MeasurementTypeTabItem(this.measurementsTabFolder, SWT.NONE, i);
			}
		}
		this.measurementsTabFolder.getItem(measurementTypeCount - 1).setShowClose(true);
		((MeasurementTypeTabItem) this.measurementsTabFolder.getItem(0)).setMeasurementType(this.deviceConfig, this.channelType.getMeasurement().get(0), this.channelConfigNumber);
		for (int i = 1; i < measurementTypeCount; i++) {
			MeasurementTypeTabItem measurementTabItem = (MeasurementTypeTabItem) this.measurementsTabFolder.getItem(i);
			measurementTabItem.setMeasurementTypeName(this.deviceConfig, this.channelType.getMeasurement().get(i), this.channelConfigNumber);
		}
		((MeasurementTypeTabItem) this.measurementsTabFolder.getSelection()).enableContextMenu(true);
		//MeasurementType end
	}

	/**
	 * clean measurement items from not visible channel tab items 
	 */
	public void cleanMeasurementitems() {
		int measurementTypeCount = this.measurementsTabFolder.getItemCount();
		for (int i = measurementTypeCount; i > 1; i--) {
			MeasurementTypeTabItem tmpMeasurementTypeTabItem = (MeasurementTypeTabItem) this.measurementsTabFolder.getItem(i-1);
			if (tmpMeasurementTypeTabItem.measurementPropertiesTabFolder != null) { // dispose PropertyTypes
				for (CTabItem tmpPropertyTypeTabItem : tmpMeasurementTypeTabItem.measurementPropertiesTabFolder.getItems()) {
					((PropertyTypeTabItem) tmpPropertyTypeTabItem).dispose();
				}
				tmpMeasurementTypeTabItem.measurementPropertiesTabItem.dispose();
			}
			if (tmpMeasurementTypeTabItem.statisticsTypeTabItem != null) { // dispose StatisticsType
				tmpMeasurementTypeTabItem.statisticsTypeTabItem.dispose();
			}
			tmpMeasurementTypeTabItem.dispose();
		}
	}

	@Override
	public synchronized ChannelTypeTabItem clone() {
		try {
			super.clone();
		}
		catch (CloneNotSupportedException e) {
			// ignore
		}
		return new ChannelTypeTabItem(this);
	}

	/**
	 * copy constructor
	 * @param copyFrom
	 */
	private ChannelTypeTabItem(ChannelTypeTabItem copyFrom) {
		super(copyFrom.channelConfigInnerTabFolder, SWT.CLOSE);
		this.propsEditor = DevicePropertiesEditor.getInstance();
		this.channelConfigInnerTabFolder = copyFrom.channelConfigInnerTabFolder;
		this.deviceConfig = copyFrom.deviceConfig;
		this.channelConfigNumber = this.channelConfigInnerTabFolder.getItemCount();
		this.channelType = new ObjectFactory().createChannelType();
		this.channelConfigType = copyFrom.channelConfigType;
		this.channelConfigName = copyFrom.channelConfigName;
		this.channelType.setName(this.channelConfigType == ChannelTypes.TYPE_OUTLET ? this.channelConfigName : Messages.getString(MessageIds.GDE_MSGT0507));
		this.channelType.setType(this.channelConfigType);
		this.tabName = GDE.STRING_BLANK + this.channelConfigNumber + GDE.STRING_BLANK + (this.deviceConfig != null ? this.channelType.getName() : GDE.STRING_EMPTY);
		initGUI();

		//MeasurementType begin - fix number tab items
		int measurementTypeCount = copyFrom.channelType.getMeasurement().size();
		int actualTabItemCount = this.measurementsTabFolder.getItemCount();
		if (measurementTypeCount < actualTabItemCount) {
			for (int i = measurementTypeCount; i < actualTabItemCount; i++) {
				MeasurementTypeTabItem measurementTabItem = (MeasurementTypeTabItem) this.measurementsTabFolder.getItem(measurementTypeCount);
				measurementTabItem.dispose();
			}
		}
		else if (measurementTypeCount > actualTabItemCount) {
			for (int i = actualTabItemCount; i < measurementTypeCount; i++) {
				new MeasurementTypeTabItem(this.measurementsTabFolder, SWT.NONE, i);
			}
		}
		this.measurementsTabFolder.getItem(measurementTypeCount - 1).setShowClose(true);
		//MeasurementType end
		if (this.deviceConfig != null) {
			this.deviceConfig.addChannelType(this.channelType);
			//copy existing measurements
			for (int i = 0; i < copyFrom.channelType.getMeasurement().size(); ++i) {
				MeasurementTypeTabItem measurementTabItem = (MeasurementTypeTabItem) this.measurementsTabFolder.getItem(i);
				MeasurementType tmpMeasurementType = copyFrom.channelType.getMeasurement().get(i).clone();
				measurementTabItem.setMeasurementType(this.deviceConfig, tmpMeasurementType, this.channelConfigNumber);
				this.channelType.getMeasurement().add(tmpMeasurementType);
			}
		}
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText(this.tabName);
			this.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			FormData fd;
			{
				this.channelConfigComposite = new Composite(this.channelConfigInnerTabFolder, SWT.NONE);
				this.setControl(this.channelConfigComposite);
				this.channelConfigComposite.setLayout(new FormLayout());
				this.channelConfigComposite.addHelpListener(new HelpListener() {			
					public void helpRequested(HelpEvent evt) {
						log.log(Level.FINEST, "channelConfigComposite.helpRequested " + evt); //$NON-NLS-1$
						DataExplorer.getInstance().openHelpDialog("", "HelpInfo_A1.html#device_properties_channelConfig"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				{
					this.channelConfigTypeCombo = new CCombo(this.channelConfigComposite, SWT.BORDER);
					this.channelConfigTypeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					//this.channelConfigTypeCombo.setBounds(6, 9, 121, 20);
					fd = new FormData();
					fd.left = new FormAttachment(0, 1000, 9);
					fd.top = new FormAttachment(0, 1000, 7);
					fd.width = 120;
					fd.height = 18;
					this.channelConfigTypeCombo.setLayoutData(fd);
					this.channelConfigTypeCombo.setItems(StringHelper.enumValues2StringArray(ChannelTypes.values()));
					this.channelConfigTypeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "channelConfigTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							ChannelTypeTabItem.this.channelConfigType = ChannelTypes.valueOf(ChannelTypeTabItem.this.channelConfigTypeCombo.getText());
							if (ChannelTypeTabItem.this.channelType != null) {
								ChannelTypeTabItem.this.channelType.setType(ChannelTypeTabItem.this.channelConfigType);
								ChannelTypeTabItem.this.deviceConfig.setChangePropery(true);
								ChannelTypeTabItem.this.propsEditor.enableSaveButton(true);
							}
						}
					});
				}
				{
					this.channelConfigText = new Text(this.channelConfigComposite, SWT.BORDER | SWT.LEFT);
					this.channelConfigText.setText(Messages.getString(MessageIds.GDE_MSGT0527));
					this.channelConfigText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					//this.channelConfigText.setBounds(147, 9, 128, 20);
					fd = new FormData();
					fd.left = new FormAttachment(0, 1000, 150);
					fd.top = new FormAttachment(0, 1000, 7);
					fd.width = 130;
					fd.height = 16;
					this.channelConfigText.setLayoutData(fd);
					this.channelConfigText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent evt) {
							log.log(java.util.logging.Level.FINEST, "channelConfigText.keyReleased, event=" + evt); //$NON-NLS-1$
							ChannelTypeTabItem.this.channelConfigName = ChannelTypeTabItem.this.channelConfigText.getText().trim();
							if (ChannelTypeTabItem.this.channelType != null) {
								ChannelTypeTabItem.this.channelType.setName(ChannelTypeTabItem.this.channelConfigName);
								ChannelTypeTabItem.this.deviceConfig.setChangePropery(true);
								ChannelTypeTabItem.this.propsEditor.enableSaveButton(true);
								ChannelTypeTabItem.this.setText(GDE.STRING_BLANK + ChannelTypeTabItem.this.channelConfigNumber + GDE.STRING_BLANK + ChannelTypeTabItem.this.channelConfigName);
							}
						}
					});
				}
				{
					this.channelConfigLabel = new Label(this.channelConfigComposite, SWT.CENTER);
					this.channelConfigLabel.setText(Messages.getString(MessageIds.GDE_MSGT0528));
					this.channelConfigLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					//this.channelConfigLabel.setBounds(289, 9, 279, 20);
					fd = new FormData();
					fd.top = new FormAttachment(0, 1000, 9);
					fd.right = new FormAttachment(1000, 1000, -60);
					fd.width = 280;
					fd.height = 18;
					this.channelConfigLabel.setLayoutData(fd);
				}
				{
					this.measurementsTabFolder = new CTabFolder(this.channelConfigComposite, SWT.NONE | SWT.BORDER);
					//this.measurementsTabFolder.setBounds(0, 35, 622, 225);
					fd = new FormData();
					fd.top = new FormAttachment(0, 1000, 35);
					fd.left = new FormAttachment(0, 1000, 0);
					fd.right = new FormAttachment(1000, 1000, 0);
					fd.bottom = new FormAttachment(1000, 1000, 0);
					this.measurementsTabFolder.setLayoutData(fd);
					{
						//create initial measurement type
						new MeasurementTypeTabItem(this.measurementsTabFolder, SWT.NONE, 0);
					}
					this.measurementsTabFolder.setSelection(0);
					this.measurementsTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
						@Override
						public void restore(CTabFolderEvent evt) {
							log.log(java.util.logging.Level.FINEST, "measurementsTabFolder.restore, event=" + evt); //$NON-NLS-1$
							((CTabItem) evt.item).getControl();
						}

						@Override
						public void close(CTabFolderEvent evt) {
							log.log(java.util.logging.Level.FINEST, "measurementsTabFolder.close, event=" + evt); //$NON-NLS-1$
							MeasurementTypeTabItem tabItem = ((MeasurementTypeTabItem) evt.item);
							if (ChannelTypeTabItem.this.deviceConfig != null) {
								ChannelTypeTabItem.this.deviceConfig.removeMeasurementFromChannel(ChannelTypeTabItem.this.channelConfigNumber, tabItem.measurementType);
							}
							ChannelTypeTabItem.this.measurementsTabFolder.setSelection(ChannelTypeTabItem.this.measurementsTabFolder.getSelectionIndex() - 1);
							tabItem.dispose();

							int itemCount = ChannelTypeTabItem.this.measurementsTabFolder.getItemCount();
							if (itemCount > 1) ChannelTypeTabItem.this.measurementsTabFolder.getItem(itemCount - 1).setShowClose(true);
						}
					});
					this.measurementsTabFolder.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "measurementsTabFolder.close, event=" + evt); //$NON-NLS-1$
							GDE.display.asyncExec(new Runnable() {
								public void run() {
									MeasurementTypeTabItem tmpMeasurementTypeTabItem = (MeasurementTypeTabItem) ChannelTypeTabItem.this.measurementsTabFolder.getSelection();
									tmpMeasurementTypeTabItem.setMeasurementType(tmpMeasurementTypeTabItem.deviceConfig, tmpMeasurementTypeTabItem.measurementType, tmpMeasurementTypeTabItem.channelConfigNumber);
									tmpMeasurementTypeTabItem.enableContextMenu(true);
								}
							});
						}
					});
				}
				{
					this.channelConfigAddButton = new Button(this.channelConfigComposite, SWT.PUSH | SWT.CENTER);
					this.channelConfigAddButton.setText(GDE.STRING_PLUS);
					this.channelConfigAddButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.channelConfigAddButton.setBounds(574, 9, 42, 19);
					fd = new FormData();
					fd.top = new FormAttachment(0, 1000, 7);
					fd.width = 42;
					fd.height = 20;
					fd.right = new FormAttachment(1000, 1000, -10);
					this.channelConfigAddButton.setLayoutData(fd);
					this.channelConfigAddButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0526));
					this.channelConfigAddButton.setSize(40, 20);
					this.channelConfigAddButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "channelConfigAddButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							ChannelTypeTabItem.this.channelConfigInnerTabFolder.getItem(ChannelTypeTabItem.this.channelConfigInnerTabFolder.getItemCount() - 1).setShowClose(false);
							ChannelTypeTabItem.this.clone();
						}
					});
				}
			}
			initialize();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * initialize states of widgests
	 */
	private void initialize() {
		if (ChannelTypeTabItem.this.channelType != null) {
			ChannelTypeTabItem.this.channelConfigType = ChannelTypeTabItem.this.channelType.getType();
			ChannelTypeTabItem.this.channelConfigTypeCombo.select(ChannelTypeTabItem.this.channelConfigType.ordinal());
			ChannelTypeTabItem.this.channelConfigName = ChannelTypeTabItem.this.channelType.getName();
			ChannelTypeTabItem.this.channelConfigText.setText(ChannelTypeTabItem.this.channelConfigName);
		}
	}

	/**
	 * @return the measurementsTabFolder
	 */
	public CTabFolder getMeasurementsTabFolder() {
		return measurementsTabFolder;
	}
}
