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
package gde.ui.menu;

import gde.log.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.GDE;
import gde.config.Settings;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * @author Winfried Brügmann
 * This class provides a context menu to selecet an image source and other image relevant functions
 */
public class ObjectImageContextMenu {
	final static Logger						log										= Logger.getLogger(ObjectImageContextMenu.class.getName());

	public static final String		OBJECT_IMAGE_PATH			= "OBJECT_IMAGE_PATH"; //$NON-NLS-1$
	public static final String		OBJECT_IMAGE_CHANGED	= "OBJECT_IMAGE_CHANGED"; //$NON-NLS-1$

	Menu													menu;

	final DataExplorer	application;

	public ObjectImageContextMenu() {
		this.application = DataExplorer.getInstance();
	}

	public void createMenu(Menu popupMenu) {
		this.menu = popupMenu;
		MenuItem newItem = new MenuItem(this.menu, SWT.PUSH);
		newItem.setText(Messages.getString(MessageIds.GDE_MSGT0430));
		newItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ObjectImageContextMenu.log.log(Level.FINEST, "newItem action performed! " + e); //$NON-NLS-1$
				chooseImageFile();
			}
		});
		MenuItem deleteItem = new MenuItem(this.menu, SWT.PUSH);
		deleteItem.setText(Messages.getString(MessageIds.GDE_MSGT0432));
		deleteItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				int answer = ObjectImageContextMenu.this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGW0047));
				if (answer == SWT.YES) {
					ObjectImageContextMenu.log.log(Level.FINEST, "deleteItem action performed! " + e); //$NON-NLS-1$
					ObjectImageContextMenu.this.menu.setData(ObjectImageContextMenu.OBJECT_IMAGE_CHANGED, true);
					ObjectImageContextMenu.this.menu.setData(ObjectImageContextMenu.OBJECT_IMAGE_PATH, null);
					ObjectImageContextMenu.this.application.updateObjectImage();
				}
			}
		});
	}

	/**
	 * Opens the FileOpenDialog to let the user choose an image file
	 */
	public void chooseImageFile() {
		FileDialog imgFileDialog = ObjectImageContextMenu.this.application.openFileOpenDialog(Messages.getString(MessageIds.GDE_MSGT0431), new String[] { GDE.FILE_ENDING_STAR_JPG, GDE.FILE_ENDING_STAR_PNG,
				GDE.FILE_ENDING_STAR_GIF }, Settings.getInstance().getObjectImageFilePath(), null, SWT.SINGLE);
		String imgFilePath = imgFileDialog.getFilterPath();
		ObjectImageContextMenu.log.log(Level.FINE, "imgFilePath = " + imgFilePath); //$NON-NLS-1$
		if (imgFileDialog.getFileName().length() > 4) {
			if (!Settings.getInstance().getObjectImageFilePath().equalsIgnoreCase(imgFileDialog.getFilterPath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX)))
				Settings.getInstance().setObjectImageFilePath(imgFileDialog.getFilterPath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX));
			ObjectImageContextMenu.this.menu.setData(ObjectImageContextMenu.OBJECT_IMAGE_CHANGED, true);
			ObjectImageContextMenu.this.menu.setData(ObjectImageContextMenu.OBJECT_IMAGE_PATH, imgFilePath + GDE.STRING_FILE_SEPARATOR_UNIX + imgFileDialog.getFileName());
			ObjectImageContextMenu.this.application.updateObjectImage();
		}
	}
}
