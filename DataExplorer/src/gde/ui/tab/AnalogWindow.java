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
package gde.ui.tab;

import java.util.HashMap;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;
import gde.ui.menu.TabAreaContextMenu.TabMenuType;

/**
 * Display window parent of analog displays
 * @author Winfried Brügmann
 */
public class AnalogWindow extends CTabItem {
	final static Logger							log	= Logger.getLogger(AnalogWindow.class.getName());

	Composite												analogMainComposite;
	HashMap<String, AnalogDisplay>	displays;
	GridLayout 											analogMainCompositeLayout;
	String 													info = Messages.getString(MessageIds.GDE_MSGT0230);

	Color														surroundingBackground;

	final DataExplorer							application;
	final Channels									channels;
	final CTabFolder								displayTab;
	final Menu											popupmenu;
	final TabAreaContextMenu				contextMenu;
	RecordSet												oldRecordSet;
	Channel													oldChannel;
	String[] 												oldRecordsToDisplay;

	public AnalogWindow(CTabFolder currentDisplayTab, int style, int position) {
		super(currentDisplayTab, style, position);
		SWTResourceManager.registerResourceUser(this);
		this.displayTab = currentDisplayTab;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 3 : 1), SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.GDE_MSGT0231));
		
		this.surroundingBackground = Settings.getInstance().getAnalogSurroundingAreaBackground();
		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();
		
		this.displays = new HashMap<String, AnalogDisplay>(3);
	}

	public void create() {
		{
			this.analogMainComposite = new Composite(this.displayTab, SWT.NONE);
			this.analogMainCompositeLayout = new GridLayout();
			this.analogMainCompositeLayout.makeColumnsEqualWidth = true;
			this.analogMainCompositeLayout.numColumns = 2;
			this.setControl(this.analogMainComposite);
			this.analogMainComposite.setLayout(this.analogMainCompositeLayout);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "digitalMainComposite " + this.analogMainComposite.getBounds().toString()); //$NON-NLS-1$
			this.analogMainComposite.setBackground(this.surroundingBackground);
			this.analogMainComposite.setMenu(this.popupmenu);
			this.analogMainComposite.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "analogMainComposite.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_8.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.analogMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "analogMainComposite.paintControl, event=" + evt); //$NON-NLS-1$
					AnalogWindow.this.contextMenu.createMenu(AnalogWindow.this.popupmenu, TabMenuType.SIMPLE);
					update(false);
				}
			});
			this.analogMainComposite.layout();
		}
	}

	/**
	 * method to update the window with its children
	 */
	public synchronized void updateChilds() {
		RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
		if (recordSet != null) { // channel does not have a record set yet
			String[] activeRecordKeys = recordSet.getDisplayableAndVisibleRecordNames();
			if (activeRecordKeys.length != this.displays.size())
				this.update(true);
			else
				for (String recordKey : activeRecordKeys) {
					AnalogDisplay display = this.displays.get(recordKey);
					if (display != null) {
						display.checkTachoNeedlePosition();
					}
				}
		}
	}

	/**
	 * method to update digital window
	 */
	public synchronized void update(boolean forceUpdate) {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.STRING_BLANK);
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.getDevice().isAnalogTabRequested()) {
				recordSet.updateSyncRecordScale();
				String[] recordsToDisplay = recordSet.getDisplayableAndVisibleRecordNames();
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, activeChannel.getName());
				// if recordSet name signature changed new displays need to be created
				boolean isUpdateRequired = forceUpdate || this.oldRecordSet == null || !recordSet.getName().equals(this.oldRecordSet.getName())
				|| this.oldChannel == null  || !this.oldChannel.getName().equals(activeChannel.getName())
						|| (recordsToDisplay.length != this.oldRecordsToDisplay.length);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isUpdateRequired = " + isUpdateRequired); //$NON-NLS-1$
				if (isUpdateRequired) {
					// set the grid layout
					this.analogMainComposite.setLayout(this.analogMainCompositeLayout);
					// clean possible outdated displays
					for (String recordKey : this.displays.keySet().toArray(new String[0])) {
						AnalogDisplay display = this.displays.get(recordKey);
						if (display != null) {
							if (!display.isDisposed()) display.dispose();
							this.displays.remove(recordKey);
						}
					}
					// add new displays
					for (String recordKey : recordSet.getDisplayableAndVisibleRecordNames()) {
						AnalogDisplay display = new AnalogDisplay(this.analogMainComposite, recordKey, DataExplorer.getInstance().getActiveDevice());
						display.create();
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "created analog display for " + recordKey); //$NON-NLS-1$
						this.displays.put(recordKey, display);
					}
					this.oldRecordSet = recordSet;
					this.oldRecordsToDisplay = recordsToDisplay;
				}
			}
			else { // clean up after device switched
				for (String recordKey : this.displays.keySet().toArray(new String[0])) {
					AnalogDisplay display = this.displays.get(recordKey);
					if (display != null) {
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "clean child " + recordKey); //$NON-NLS-1$
						if (!display.isDisposed()) display.dispose();
						this.displays.remove(recordKey);
					}
				}
			}
			this.oldChannel = activeChannel;
			this.analogMainComposite.layout();
		}
	}
	
	/**
	 * create visible tab window content as image
	 * @return image with content
	 */
	public Image getContentAsImage() {
		Rectangle bounds = this.analogMainComposite.getClientArea();
		Image tabContentImage = new Image(GDE.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.analogMainComposite.print(imageGC);
		if (GDE.IS_MAC)
			this.analogMainComposite.print(imageGC);
		imageGC.dispose();

		return tabContentImage;
	}

	/**
	 * @param newInnerAreaBackground the innerAreaBackground to set
	 */
	public void setInnerAreaBackground(Color newInnerAreaBackground) {
		this.update(true);
	}

	/**
	 * @param newSurroundingBackground the surroundingAreaBackground to set
	 */
	public void setSurroundingAreaBackground(Color newSurroundingBackground) {
		this.analogMainComposite.setBackground(newSurroundingBackground);
		this.surroundingBackground = newSurroundingBackground;
		this.analogMainComposite.redraw();
	}
}
