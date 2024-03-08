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
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;

import gde.device.MeasurementPropertyTypes;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

/**
 * Class to represent the context menu to enable adding statistics and property tab items to measurement
 * @author Winfried Brügmann
 */
public class MeasurementContextmenu {
	final static Logger						log	= Logger.getLogger(ContextMenu.class.getName());

	final Menu										menu;
	final MeasurementTypeTabItem	measurementTypeTabItem;
	final CTabFolder							channelConfigMeasurementPropertiesTabFolder;
	CTabFolder										measurementPropertiesTabFolder;

	MenuItem											addStatisticsTypeMenuItem, addPropertyTypeMenuItem;
	Menu													addPropertyTypeMenu;
	MenuItem											defaultPropertyMenuItem;
	MenuItem											offsetPropertyMenuItem, factorPropertyMenuItem, reductionPropertyMenuItem, doSubtractFirstPropertyMenuItem, doSubtractLastPropertyMenuItem;
	MenuItem											regressionIntervalPropertyMenuItem, regressionTypeCurvePropertyMenuItem, regressionTypeLinearPropertyMenuItem;
	MenuItem											numberMotorPropertyMenuItem, revolutionFactorPropertyMenuItem, prop100WPropertyMenuItem;
	MenuItem											numberCellsPropertyMenuItem, invertCurrentPropertyMenuItem;
	MenuItem 											scaleSyncRefOrdinal, googleEarthVelocityAvgLimitFactor, googleEarthVelocityLowerLimit, googleEarthVelocityUpperLimit;
	MenuItem 											filterFactor, recordDataType;


	public MeasurementContextmenu(Menu useMenu, MeasurementTypeTabItem parent, CTabFolder useChannelConfigMeasurementPropertiesTabFolder) {
		this.menu = useMenu;
		this.measurementTypeTabItem = parent;
		this.channelConfigMeasurementPropertiesTabFolder = useChannelConfigMeasurementPropertiesTabFolder;
	}

	public void create() {
		SWTResourceManager.registerResourceUser(this.menu);
		this.menu.addMenuListener(new MenuListener() {
			public void menuShown(MenuEvent e) {
				log.log(java.util.logging.Level.FINEST, "menuShown action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.addStatisticsTypeMenuItem.setEnabled(true);
				MeasurementContextmenu.this.addPropertyTypeMenuItem.setEnabled(true);
				for (CTabItem tabItem : MeasurementContextmenu.this.channelConfigMeasurementPropertiesTabFolder.getItems()) {
					if (tabItem.getText().equals(Messages.getString(MessageIds.GDE_MSGT0350))) {
						MeasurementContextmenu.this.addStatisticsTypeMenuItem.setEnabled(false);
					}
				}
				//refresh tab folder it could be destroyed and re-created
				MeasurementContextmenu.this.measurementPropertiesTabFolder = MeasurementContextmenu.this.measurementTypeTabItem.measurementPropertiesTabFolder;
			}

			public void menuHidden(MenuEvent e) {
				//ignore
			}
		});
		this.addStatisticsTypeMenuItem = new MenuItem(this.menu, SWT.PUSH);
		this.addStatisticsTypeMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0533));
		this.addStatisticsTypeMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "addStatisticsTypeMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createStatisticsTabItem();
			}
		});
		new MenuItem(this.menu, SWT.SEPARATOR);
		this.addPropertyTypeMenuItem = new MenuItem(this.menu, SWT.CASCADE);
		this.addPropertyTypeMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0534));
		this.addPropertyTypeMenu = new Menu(this.addPropertyTypeMenuItem);
		this.addPropertyTypeMenuItem.setMenu(this.addPropertyTypeMenu);
		this.addPropertyTypeMenu.addMenuListener(new MenuListener() {
			public void menuShown(MenuEvent e) {
				log.log(java.util.logging.Level.FINEST, "addPropertyTypeMenu.menuShown action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.offsetPropertyMenuItem.setEnabled(true);
				MeasurementContextmenu.this.factorPropertyMenuItem.setEnabled(true);
				MeasurementContextmenu.this.reductionPropertyMenuItem.setEnabled(true);
				MeasurementContextmenu.this.doSubtractFirstPropertyMenuItem.setEnabled(true);
				MeasurementContextmenu.this.doSubtractLastPropertyMenuItem.setEnabled(true);
				MeasurementContextmenu.this.regressionIntervalPropertyMenuItem.setEnabled(true);
				MeasurementContextmenu.this.regressionTypeCurvePropertyMenuItem.setEnabled(true);
				MeasurementContextmenu.this.regressionTypeLinearPropertyMenuItem.setEnabled(true);
				MeasurementContextmenu.this.numberMotorPropertyMenuItem.setEnabled(true);
				MeasurementContextmenu.this.revolutionFactorPropertyMenuItem.setEnabled(true);
				MeasurementContextmenu.this.prop100WPropertyMenuItem.setEnabled(true);
				MeasurementContextmenu.this.numberCellsPropertyMenuItem.setEnabled(true);
				MeasurementContextmenu.this.invertCurrentPropertyMenuItem.setEnabled(true);
				MeasurementContextmenu.this.scaleSyncRefOrdinal.setEnabled(true);
				MeasurementContextmenu.this.googleEarthVelocityAvgLimitFactor.setEnabled(true);
				MeasurementContextmenu.this.googleEarthVelocityLowerLimit.setEnabled(true);
				MeasurementContextmenu.this.googleEarthVelocityUpperLimit.setEnabled(true);
				MeasurementContextmenu.this.filterFactor.setEnabled(true);
				MeasurementContextmenu.this.recordDataType.setEnabled(true);
				MeasurementContextmenu.this.defaultPropertyMenuItem.setEnabled(true);
				if (MeasurementContextmenu.this.measurementPropertiesTabFolder != null) {
					for (CTabItem tabItem : MeasurementContextmenu.this.measurementPropertiesTabFolder.getItems()) {
						switch (MeasurementPropertyTypes.fromValue(tabItem.getText())) {
						case OFFSET:
							MeasurementContextmenu.this.offsetPropertyMenuItem.setEnabled(false);
							break;
						case FACTOR:
							MeasurementContextmenu.this.factorPropertyMenuItem.setEnabled(false);
							break;
						case REDUCTION:
							MeasurementContextmenu.this.reductionPropertyMenuItem.setEnabled(false);
							break;
						case DO_SUBTRACT_FIRST:
							MeasurementContextmenu.this.doSubtractFirstPropertyMenuItem.setEnabled(false);
							break;
						case DO_SUBTRACT_LAST:
							MeasurementContextmenu.this.doSubtractLastPropertyMenuItem.setEnabled(false);
							break;
						case REGRESSION_INTERVAL_SEC:
							MeasurementContextmenu.this.regressionIntervalPropertyMenuItem.setEnabled(false);
							break;
						case REGRESSION_TYPE_CURVE:
							MeasurementContextmenu.this.regressionTypeCurvePropertyMenuItem.setEnabled(false);
							break;
						case REGRESSION_TYPE_LINEAR:
							MeasurementContextmenu.this.regressionTypeLinearPropertyMenuItem.setEnabled(false);
							break;
						case NUMBER_MOTOR:
							MeasurementContextmenu.this.numberMotorPropertyMenuItem.setEnabled(false);
							break;
						case REVOLUTION_FACTOR:
							MeasurementContextmenu.this.revolutionFactorPropertyMenuItem.setEnabled(false);
							break;
						case NUMBER_CELLS:
							MeasurementContextmenu.this.numberCellsPropertyMenuItem.setEnabled(false);
							break;
						case PROP_N_100_W:
							MeasurementContextmenu.this.prop100WPropertyMenuItem.setEnabled(false);
							break;
						case IS_INVERT_CURRENT:
							MeasurementContextmenu.this.invertCurrentPropertyMenuItem.setEnabled(false);
							break;
						case SCALE_SYNC_REF_ORDINAL:
							MeasurementContextmenu.this.scaleSyncRefOrdinal.setEnabled(false);
							break;
						case GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR:
							MeasurementContextmenu.this.googleEarthVelocityAvgLimitFactor.setEnabled(false);
							MeasurementContextmenu.this.googleEarthVelocityLowerLimit.setEnabled(false);
							MeasurementContextmenu.this.googleEarthVelocityUpperLimit.setEnabled(false);
							break;
						case GOOGLE_EARTH_VELOCITY_LOWER_LIMIT:
							MeasurementContextmenu.this.googleEarthVelocityAvgLimitFactor.setEnabled(false);
							MeasurementContextmenu.this.googleEarthVelocityLowerLimit.setEnabled(false);
							break;
						case GOOGLE_EARTH_VELOCITY_UPPER_LIMIT:
							MeasurementContextmenu.this.googleEarthVelocityAvgLimitFactor.setEnabled(false);
							MeasurementContextmenu.this.googleEarthVelocityUpperLimit.setEnabled(false);
							break;
						case FILTER_FACTOR:
							MeasurementContextmenu.this.filterFactor.setEnabled(false);
							break;
						case DATA_TYPE:
							MeasurementContextmenu.this.recordDataType.setEnabled(false);
							break;
						default:
						case NONE_SPECIFIED:
							MessageBox mb = new MessageBox(MeasurementContextmenu.this.menu.getShell(), SWT.OK);
							mb.setText(Messages.getString(MessageIds.GDE_MSGW0540));
							mb.setMessage(Messages.getString(MessageIds.GDE_MSGW0541));
							mb.open();
							break;
						}
					}
				}
			}
			public void menuHidden(MenuEvent e) {
				//ignore
			}
		});
		this.offsetPropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.offsetPropertyMenuItem.setText(MeasurementPropertyTypes.OFFSET.value());
		this.offsetPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "defaultPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.OFFSET.value());
			}
		});
		this.factorPropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.factorPropertyMenuItem.setText(MeasurementPropertyTypes.FACTOR.value());
		this.factorPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "factorPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.FACTOR.value());
			}
		});
		this.reductionPropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.reductionPropertyMenuItem.setText(MeasurementPropertyTypes.REDUCTION.value());
		this.reductionPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "reductionPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.REDUCTION.value());
			}
		});
		this.doSubtractFirstPropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.doSubtractFirstPropertyMenuItem.setText(MeasurementPropertyTypes.DO_SUBTRACT_FIRST.value());
		this.doSubtractFirstPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "doSubtractFirstPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.DO_SUBTRACT_FIRST.value());
			}
		});
		this.doSubtractLastPropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.doSubtractLastPropertyMenuItem.setText(MeasurementPropertyTypes.DO_SUBTRACT_LAST.value());
		this.doSubtractLastPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "doSubtractLastPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.DO_SUBTRACT_LAST.value());
			}
		});
		this.regressionIntervalPropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.regressionIntervalPropertyMenuItem.setText(MeasurementPropertyTypes.REGRESSION_INTERVAL_SEC.value());
		this.regressionIntervalPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "regressionIntervalPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.REGRESSION_INTERVAL_SEC.value());
			}
		});
		this.regressionTypeCurvePropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.regressionTypeCurvePropertyMenuItem.setText(MeasurementPropertyTypes.REGRESSION_TYPE_CURVE.value());
		this.regressionTypeCurvePropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "regressionTypeCurvePropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.REGRESSION_TYPE_CURVE.value());
			}
		});
		this.regressionTypeLinearPropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.regressionTypeLinearPropertyMenuItem.setText(MeasurementPropertyTypes.REGRESSION_TYPE_LINEAR.value());
		this.regressionTypeLinearPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "regressionTypeLinearPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.REGRESSION_TYPE_LINEAR.value());
			}
		});
		this.numberMotorPropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.numberMotorPropertyMenuItem.setText(MeasurementPropertyTypes.NUMBER_MOTOR.value());
		this.numberMotorPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "numberMotorPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.NUMBER_MOTOR.value());
			}
		});
		this.revolutionFactorPropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.revolutionFactorPropertyMenuItem.setText(MeasurementPropertyTypes.REVOLUTION_FACTOR.value());
		this.revolutionFactorPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "revolutionFactorPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.REVOLUTION_FACTOR.value());
			}
		});
		this.prop100WPropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.prop100WPropertyMenuItem.setText(MeasurementPropertyTypes.PROP_N_100_W.value());
		this.prop100WPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "prop100WPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.PROP_N_100_W.value());
			}
		});
		this.numberCellsPropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.numberCellsPropertyMenuItem.setText(MeasurementPropertyTypes.NUMBER_CELLS.value());
		this.numberCellsPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "numberCellsPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.NUMBER_CELLS.value());
			}
		});
		this.invertCurrentPropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.invertCurrentPropertyMenuItem.setText(MeasurementPropertyTypes.IS_INVERT_CURRENT.value());
		this.invertCurrentPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "invertCurrentPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.IS_INVERT_CURRENT.value());
			}
		});
		this.scaleSyncRefOrdinal = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.scaleSyncRefOrdinal.setText(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
		this.scaleSyncRefOrdinal.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "scaleSyncRefOrdinal action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
			}
		});
		this.googleEarthVelocityAvgLimitFactor = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.googleEarthVelocityAvgLimitFactor.setText(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value());
		this.googleEarthVelocityAvgLimitFactor.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "googleEarthVelocityAvgLimitFactor action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value());
			}
		});
		this.googleEarthVelocityLowerLimit = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.googleEarthVelocityLowerLimit.setText(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value());
		this.googleEarthVelocityLowerLimit.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "googleEarthVelocityLowerLimit action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value());
			}
		});
		this.googleEarthVelocityUpperLimit = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.googleEarthVelocityUpperLimit.setText(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value());
		this.googleEarthVelocityUpperLimit.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "googleEarthVelocityUpperLimit action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value());
			}
		});	
		this.filterFactor = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.filterFactor.setText(MeasurementPropertyTypes.FILTER_FACTOR.value());
		this.filterFactor.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "filterFactor action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.FILTER_FACTOR.value());
			}
		});	
		this.recordDataType = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.recordDataType.setText(MeasurementPropertyTypes.DATA_TYPE.value());
		this.recordDataType.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "recordDataType action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.DATA_TYPE.value());
			}
		});	
		this.defaultPropertyMenuItem = new MenuItem(this.addPropertyTypeMenu, SWT.PUSH);
		this.defaultPropertyMenuItem.setText(MeasurementPropertyTypes.NONE_SPECIFIED.value());
		this.defaultPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "defaultPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				MeasurementContextmenu.this.measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.NONE_SPECIFIED.value());
			}
		});
	}

}
