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

import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * @author Winfried Brügmann
 * This class provides a context menu to tabulator area, curve graphics, compare window, etc. and enable selection of background color, ...
 */
public class ContextMenu {
	final static Logger						log						= Logger.getLogger(ContextMenu.class.getName());

	final DataExplorer						application;
	final VarioToolTabItem 				varioToolTabItem;

	MenuItem											headerItem;
	MenuItem											setupLoadItem;
	MenuItem											setupSaveItem;
	MenuItem											separatorView;

	public ContextMenu(VarioToolTabItem useVarioToolTabItem) {
		this.application = DataExplorer.getInstance();
		this.varioToolTabItem = useVarioToolTabItem;
	}

	public void createMenu(Menu popupMenu) {
		this.headerItem = new MenuItem(popupMenu, SWT.NONE);
		this.headerItem.setText(Messages.getString(MessageIds.GDE_MSGT1894) + varioToolTabItem.getFirmwareVersion());

		this.separatorView = new MenuItem(popupMenu, SWT.SEPARATOR);

		this.setupLoadItem = new MenuItem(popupMenu, SWT.PUSH);
		this.setupLoadItem.setText(Messages.getString(MessageIds.GDE_MSGT1892));
		this.setupLoadItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ContextMenu.log.log(Level.FINEST, "setupLoadItem action performed! " + e); //$NON-NLS-1$
				varioToolTabItem.loadSetup();
			}
		});
		this.setupSaveItem = new MenuItem(popupMenu, SWT.PUSH);
		this.setupSaveItem.setText(Messages.getString(MessageIds.GDE_MSGT1893));
		this.setupSaveItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ContextMenu.log.log(Level.FINEST, "setupSaveItem action performed! " + e); //$NON-NLS-1$
				varioToolTabItem.saveSetup();
			}
		});
	}
}
