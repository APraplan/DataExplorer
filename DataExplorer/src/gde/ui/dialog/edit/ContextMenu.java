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

import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

/**
 * Class to represent the context menu to enable adding different tab items
 * @author Winfried Brügmann
 */
public class ContextMenu {
	final static Logger	log	= Logger.getLogger(ContextMenu.class.getName());

	final Menu					menu;
	final CTabFolder		parentTabFolder;

	MenuItem						addSerialPortTypeMenuItem, addDataBlockTypeMenuItem, addStateTypeMenuItem;

	public ContextMenu(Menu useMenu, CTabFolder parent) {
		this.menu = useMenu;
		this.parentTabFolder = parent;
	}

	public void create() {
		SWTResourceManager.registerResourceUser(this.menu);
		this.menu.addMenuListener(new MenuListener() {
			public void menuShown(MenuEvent e) {
				log.log(java.util.logging.Level.FINEST, "menuShown action performed! " + e); //$NON-NLS-1$
				ContextMenu.this.addSerialPortTypeMenuItem.setEnabled(true);
				ContextMenu.this.addDataBlockTypeMenuItem.setEnabled(true);
				ContextMenu.this.addStateTypeMenuItem.setEnabled(true);
				for (CTabItem tabItem : ContextMenu.this.parentTabFolder.getItems()) {
					if (tabItem.getText().equals(Messages.getString(MessageIds.GDE_MSGT0510))) {
						ContextMenu.this.addSerialPortTypeMenuItem.setEnabled(false);
					}
					else if (tabItem.getText().equals(Messages.getString(MessageIds.GDE_MSGT0515))) {
						ContextMenu.this.addDataBlockTypeMenuItem.setEnabled(false);
					}
					else if (tabItem.getText().equals(Messages.getString(MessageIds.GDE_MSGT0470))) {
						ContextMenu.this.addStateTypeMenuItem.setEnabled(false);
					}
				}
			}
			public void menuHidden(MenuEvent e) {// no implementation here 
			}
		});
		this.addSerialPortTypeMenuItem = new MenuItem(this.menu, SWT.PUSH);
		this.addSerialPortTypeMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0512));
		this.addSerialPortTypeMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "addSerialPortTypeMenuItem action performed! " + e); //$NON-NLS-1$
				DevicePropertiesEditor.getInstance().createSerialPortTabItem();
			}
		});
		new MenuItem(this.menu, SWT.SEPARATOR);
		this.addDataBlockTypeMenuItem = new MenuItem(this.menu, SWT.PUSH);
		this.addDataBlockTypeMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0513));
		this.addDataBlockTypeMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "addSerialPortTypeMenuItem action performed! " + e); //$NON-NLS-1$
				DevicePropertiesEditor.getInstance().createDataBlockType();
			}
		});
		new MenuItem(this.menu, SWT.SEPARATOR);
		this.addStateTypeMenuItem = new MenuItem(this.menu, SWT.PUSH);
		this.addStateTypeMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0514));
		this.addStateTypeMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(java.util.logging.Level.FINEST, "addStateTypeMenuItem action performed! " + e); //$NON-NLS-1$
				DevicePropertiesEditor.getInstance().createStateTabItem();
			}
		});
	}

}
