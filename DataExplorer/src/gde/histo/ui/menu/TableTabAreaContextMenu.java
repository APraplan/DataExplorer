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

    Copyright (c) 2017,2018,2019 Thomas Eickert
****************************************************************************************/

package gde.histo.ui.menu;

import java.nio.file.Paths;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.log.Level;

/**
 * Provides a context menu to the histo table tab and enable selection of background color, ...
 * @author Thomas Eickert (USER)
 */
public final class TableTabAreaContextMenu extends AbstractTabAreaContextMenu {
	private final static String	$CLASS_NAME	= TableTabAreaContextMenu.class.getName();
	@SuppressWarnings("hiding")
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	@Override
	public void createMenu(Menu popupMenu) {
		popupMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuShown(MenuEvent e) {
				log.log(Level.FINEST, "menuShown action " + e); //$NON-NLS-1$
				setCommonItems(popupMenu);

				if (popupMenu.getData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.toString()) == null) { // not within the curve area canvas bounds
					setCoreEnabled(false);
					suppressModeItem.setEnabled(true);
					hideItem.setEnabled(true);
					hideMenuRevokeItem.setEnabled(true);
					copyTabItem.setEnabled(true);
				} else {
					setCoreEnabled(true);
					curveSelectionItem.setEnabled(false);
					displayGraphicsHeaderItem.setEnabled(false);
					displayGraphicsCommentItem.setEnabled(false);
					displayGraphicsCurveSurvey.setEnabled(false);
					copyPrintImageItem.setEnabled(false);

					String dataFilePath = (String) popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString());
					setDataPathItems(popupMenu, Paths.get(dataFilePath));
				}

				// clear consumed menu type selector
				popupMenu.setData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.toString(), null);
			}

			@Override
			public void menuHidden(MenuEvent e) {
				// ignore
			}
		});

		if (!isCreated) {
			createFileItems(popupMenu);
			new MenuItem(popupMenu, SWT.SEPARATOR);
			createCheckItems(popupMenu);
			new MenuItem(popupMenu, SWT.SEPARATOR);
			createCopyItems(popupMenu);

			isCreated = true;
		}

	}

}
