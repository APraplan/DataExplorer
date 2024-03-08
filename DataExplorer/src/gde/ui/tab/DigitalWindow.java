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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Display window parent of digital displays
 * @author Winfried Brügmann
 */
public class DigitalWindow extends CTabItem {
	final static Logger							log	= Logger.getLogger(DigitalWindow.class.getName());

	Composite												digitalMainComposite;
	HashMap<String, DigitalDisplay>	displays;
	FillLayout 											digitalMainCompositeLayout = new FillLayout(SWT.HORIZONTAL);
	String 													info = Messages.getString(MessageIds.GDE_MSGT0230);

	Color														surroundingBackground;

	final DataExplorer							application;
	final Channels									channels;
	final CTabFolder								displayTab;
	
	RecordSet												oldRecordSet;
	Channel													oldChannel;
	String[] 												oldRecordsToDisplay;

	public DigitalWindow(CTabFolder currentDisplayTab, int style, int position) {
		super(currentDisplayTab, style, position);
		SWTResourceManager.registerResourceUser(this);
		this.displayTab = currentDisplayTab;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 3 : 1), SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.GDE_MSGT0238));
			
		this.surroundingBackground = Settings.getInstance().getDigitalSurroundingAreaBackground();
		
		this.displays = new HashMap<String, DigitalDisplay>(3);
		
		this.displayTab.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent evt) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "digitalMainComposite.paintControl, event=" + evt); //$NON-NLS-1$
				update(false);
			}
		});
	}

	public void create() {
		{
			this.digitalMainComposite = new Composite(this.displayTab, SWT.NONE);
			this.setControl(this.digitalMainComposite);
			this.digitalMainComposite.setBackground(this.surroundingBackground);
			this.digitalMainComposite.setLayout(null);
			this.digitalMainComposite.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "digitalMainComposite.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_7.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.digitalMainComposite.layout();
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
					DigitalDisplay display = this.displays.get(recordKey);
					if (display != null) display.redawDisplay();
				}
		}
	}

	/**
	 * method to update digital window by adding removing digital displays
	 */
	public synchronized void update(boolean forceUpdate) {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.STRING_BLANK);
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.getDevice().isDigitalTabRequested()) {
				String[] recordsToDisplay = recordSet.getDisplayableAndVisibleRecordNames();
		
				// if recordSet name signature changed new displays need to be created
				boolean isUpdateRequired = forceUpdate || this.oldRecordSet == null || !recordSet.getName().equals(this.oldRecordSet.getName())
						|| this.oldChannel == null  || !this.oldChannel.getName().equals(activeChannel.getName())
						|| (recordsToDisplay.length != this.oldRecordsToDisplay.length);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isUpdateRequired = " + isUpdateRequired); //$NON-NLS-1$
				if (isUpdateRequired) {
					// set layout 
					this.digitalMainComposite.setLayout(this.digitalMainCompositeLayout);
					// cleanup
					for (String recordKey : this.displays.keySet().toArray(new String[0])) {
						DigitalDisplay display = this.displays.get(recordKey);
						if (display != null) {
							if (!display.isDisposed()) display.dispose();
							this.displays.remove(recordKey);
						}
					}
					// add new
					for (String recordKey : recordSet.getDisplayableAndVisibleRecordNames()) {
						DigitalDisplay display = new DigitalDisplay(this.application, this.digitalMainComposite, recordKey, DataExplorer.getInstance().getActiveDevice());
						display.create();
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "created digital display for " + recordKey); //$NON-NLS-1$
						this.displays.put(recordKey, display);
					}
					this.oldRecordSet = recordSet;
					this.oldRecordsToDisplay = recordsToDisplay;
				}
			}
			else { // clean up after device switched
				for (String recordKey : this.displays.keySet().toArray(new String[0])) {
					DigitalDisplay display = this.displays.get(recordKey);
					if (display != null) {
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "clean child " + recordKey); //$NON-NLS-1$
						if (!display.isDisposed()) display.dispose();
						this.displays.remove(recordKey);
					}
				}
			}
			this.oldChannel = activeChannel;
			this.digitalMainComposite.layout();
		}
	}
	
	/**
	 * create visible tab window content as image
	 * @return image with content
	 */
	public Image getContentAsImage() {
		Rectangle bounds = this.digitalMainComposite.getClientArea();
		Image tabContentImage = new Image(GDE.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.digitalMainComposite.print(imageGC);
		if (GDE.IS_MAC)
			this.digitalMainComposite.print(imageGC);
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
	 * @param newInnerAreaBackground the innerAreaBackground to set
	 */
	public void setDigitalDisplayFontSize(int newSize) {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null && this.digitalMainComposite.isVisible()) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.STRING_BLANK);
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.getDevice().isDigitalTabRequested()) {
				String[] recordsToDisplay = recordSet.getDisplayableAndVisibleRecordNames();
					for (String recordKey : recordsToDisplay) {
						this.displays.get(recordKey).setFontSize(newSize);
					}
				}
			}
		this.update(true);
	}

	/**
	 * @param newSurroundingBackground the surroundingAreaBackground to set
	 */
	public void setSurroundingAreaBackground(Color newSurroundingBackground) {
		this.digitalMainComposite.setBackground(newSurroundingBackground);
		this.surroundingBackground = newSurroundingBackground;
		this.digitalMainComposite.redraw();
	}
}
