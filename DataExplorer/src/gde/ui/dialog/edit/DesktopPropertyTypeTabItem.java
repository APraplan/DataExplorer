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

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.device.DesktopPropertyType;
import gde.device.DesktopPropertyTypes;
import gde.device.DeviceConfiguration;
import gde.device.ObjectFactory;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * Composite to wrap XML DesktopPropertyType enable to edit existing and create new device property files
 * @author Winfried Brügmann
 */
public class DesktopPropertyTypeTabItem extends CTabItem {
	final static Logger						log	= Logger.getLogger(DesktopPropertyTypeTabItem.class.getName());

	final CTabFolder							parentTabFolder;
	final MeasurementTypeTabItem	measurementTypeTabItem;

	Composite											propertyTypeComposite;
	Label													nameLabel;
	Label													typeLabel;
	Label													valueLabel;
	Label													descriptionLabel;
	Text													descriptionText;
	Label													attributeLabel;
	CCombo												valueCombo, attributeCombo;
	KeyAdapter										valueKeyListener;
	VerifyListener								valueVerifyListener;

	DeviceConfiguration						deviceConfig;

	Menu													popupMenu;
	MeasurementContextmenu				contextMenu;
	String												tabName;
	DesktopPropertyType						propertyType;

	final DevicePropertiesEditor	propsEditor;

	/**
	 * constructor without any variables input
	 * to display default values a call of update(PropertyType) is required
	 * @param parent must be a CTabFolder
	 * @param style
	 * @param useTabName
	 * @param useMeasurementTypeTabItem2CreatePopupMenu != null signal a popup menu should be initialized
	 */
	public DesktopPropertyTypeTabItem(CTabFolder parent, int style, String useTabName, MeasurementTypeTabItem useMeasurementTypeTabItem2CreatePopupMenu) {
		super(parent, style);
		this.parentTabFolder = parent;
		this.propsEditor = DevicePropertiesEditor.getInstance();
		this.tabName = useTabName;
		this.measurementTypeTabItem = useMeasurementTypeTabItem2CreatePopupMenu;
		this.propertyType = new ObjectFactory().createDesktopPropertyType();
		this.propertyType.setName(DesktopPropertyTypes.values()[0]);
		this.propertyType.setValue(false);
		this.propertyType.setDescription(Messages.getString(MessageIds.GDE_MSGT0474));

		initGUI();
	}

	/**
	 * enable the context menu to create missing tab items
	 * @param enable
	 */
	void enableContextMenu(boolean enable) {
		if (enable && this.measurementTypeTabItem != null) {
			this.popupMenu = new Menu(this.measurementTypeTabItem.channelConfigMeasurementPropertiesTabFolder.getShell(), SWT.POP_UP);
			//this.popupMenu = SWTResourceManager.getMenu("MeasurementContextmenu", this.measurementTypeTabItem.channelConfigMeasurementPropertiesTabFolder.getShell(), SWT.POP_UP);
			this.contextMenu = new MeasurementContextmenu(this.popupMenu, this.measurementTypeTabItem, this.measurementTypeTabItem.channelConfigMeasurementPropertiesTabFolder);
			this.contextMenu.create();
		}
		else if (this.popupMenu != null) {
			this.popupMenu.dispose();
			this.popupMenu = null;
			this.contextMenu = null;
		}
		this.propertyTypeComposite.setMenu(this.popupMenu);
		this.nameLabel.setMenu(this.popupMenu);
		this.typeLabel.setMenu(this.popupMenu);
		this.valueLabel.setMenu(this.popupMenu);
		this.descriptionLabel.setMenu(this.popupMenu);
	}

	/**
	 * update the tab item internal widgets by property content
	 * @param useDeviceConfig of DeviceConfiguration
	 * @param useProperty of PropertyType
	 * @param enableEditName
	 * @param nameSelectionItems String[] used to fill a selection combo box
	 * @param typeSelectionItems String[] used to fill a selection combo box
	 * @param enableEditValue
	 */
	public void setProperty(DeviceConfiguration useDeviceConfig, DesktopPropertyType useProperty) {
		this.deviceConfig = useDeviceConfig;
		this.propertyType = useProperty;
		this.setText(this.tabName = this.propertyType.getLocalizedName());

		this.valueCombo.setVisible(true);
		this.valueCombo.select(this.propertyType.isValue() == true ? 0 : 1);

		this.descriptionText.setText(this.propertyType.getDescription() != null ? this.propertyType.getDescription() : GDE.STRING_EMPTY);
		this.enableContextMenu(true);
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText(this.tabName);
			this.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent disposeevent) {
					log.log(java.util.logging.Level.FINEST, "statisticsTypeTabItem.widgetDisposed, event=" + disposeevent); //$NON-NLS-1$
					DesktopPropertyTypeTabItem.this.enableContextMenu(false);
				}
			});
			this.propertyTypeComposite = new Composite(this.parentTabFolder, SWT.NONE);
			this.setControl(this.propertyTypeComposite);
			this.propertyTypeComposite.setLayout(null);
			this.propertyTypeComposite.setSize(300, 160);
			this.propertyTypeComposite.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "propertyTypeComposite.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_A.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			{
				this.nameLabel = new Label(this.propertyTypeComposite, SWT.RIGHT);
				this.nameLabel.setText(Messages.getString(MessageIds.GDE_MSGT0549));
				this.nameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.nameLabel.setBounds(5, 12, 80, 20);
			}
			{
				this.typeLabel = new Label(this.propertyTypeComposite, SWT.RIGHT);
				this.typeLabel.setText(Messages.getString(MessageIds.GDE_MSGT0552));
				this.typeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.typeLabel.setBounds(5, 38, 80, 20);
			}
			{
				this.valueLabel = new Label(this.propertyTypeComposite, SWT.RIGHT);
				this.valueLabel.setText(Messages.getString(MessageIds.GDE_MSGT0553));
				this.valueLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.valueLabel.setBounds(5, 65, 80, 20);
			}
			{
				this.descriptionLabel = new Label(this.propertyTypeComposite, SWT.RIGHT);
				this.descriptionLabel.setText(Messages.getString(MessageIds.GDE_MSGT0554));
				this.descriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.descriptionLabel.setBounds(5, 92, 80, 20);
			}
			{
				if (this.tabName.equals(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB.value())) {
					this.attributeLabel = new Label(this.propertyTypeComposite, SWT.RIGHT);
					this.attributeLabel.setText(Messages.getString(MessageIds.GDE_MSGT0598));
					this.attributeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.attributeLabel.setBounds(5, 135, 80, 40);
				}
			}
			{
				this.valueCombo = new CCombo(this.propertyTypeComposite, SWT.BORDER);
				this.valueCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.valueCombo.setBounds(90, 65, 120, 20);
				this.valueCombo.setEditable(false);
				this.valueCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
				this.valueCombo.setItems(GDE.STRING_ARRAY_TRUE_FALSE);
				this.valueCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "valueCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						if (DesktopPropertyTypeTabItem.this.deviceConfig != null) {
							DesktopPropertyTypeTabItem.this.deviceConfig.setTableTabRequested(Boolean.parseBoolean(DesktopPropertyTypeTabItem.this.valueCombo.getText()));

							DesktopPropertyTypeTabItem.this.deviceConfig.setChangePropery(true);
							DesktopPropertyTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
						else {
							DesktopPropertyTypeTabItem.this.propertyType.setValue(Boolean.parseBoolean(DesktopPropertyTypeTabItem.this.valueCombo.getText()));
						}
					}
				});
			}
			{
				this.descriptionText = new Text(this.propertyTypeComposite, SWT.LEFT | SWT.WRAP | SWT.BORDER);
				this.descriptionText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.descriptionText.setBounds(90, 92, 200, 45);
				this.descriptionText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "descriptionText.keyReleased, event=" + evt); //$NON-NLS-1$
						DesktopPropertyTypeTabItem.this.propertyType.setDescription(DesktopPropertyTypeTabItem.this.descriptionText.getText());
						if (DesktopPropertyTypeTabItem.this.deviceConfig != null) {
							DesktopPropertyTypeTabItem.this.deviceConfig.setChangePropery(true);
							DesktopPropertyTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
					@Override
					public void keyPressed(KeyEvent e) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "descriptionText.keyPressed , event=" + e); //$NON-NLS-1$

						// select all text on ctrl+a
						if (e.keyCode=='a' && ((e.stateMask & SWT.MOD1) != 0)) {
							descriptionText.selectAll();
						}
					}
				});
			}
			{
				if (this.tabName.equals(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB.value())) {
					this.attributeCombo = new CCombo(this.propertyTypeComposite, SWT.BORDER);
					this.attributeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.attributeCombo.setBounds(90, 145, 120, 20);
					this.attributeCombo.setEditable(false);
					this.attributeCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
					this.attributeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "attributeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (DesktopPropertyTypeTabItem.this.deviceConfig != null) {
								DesktopPropertyTypeTabItem.this.propertyType.setTargetReferenceOrdinal(DesktopPropertyTypeTabItem.this.attributeCombo.getSelectionIndex());
							}
							if (DesktopPropertyTypeTabItem.this.deviceConfig != null) {
								DesktopPropertyTypeTabItem.this.deviceConfig.setChangePropery(true);
								DesktopPropertyTypeTabItem.this.propsEditor.enableSaveButton(true);
							}
						}
					});
				}
			}
			this.propertyTypeComposite.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
