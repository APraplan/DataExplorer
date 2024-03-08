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
    
    Copyright (c) 2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.log.Level;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * @author Winfried Brügmann
 * This class provides a context menu to tabulator area, curve graphics, compare window, etc. and enable selection of background color, ...
 */
public class ConvertContextMenu {
	final static Logger						log	= Logger.getLogger(ConvertContextMenu.class.getName());
	
	MenuItem											convert2mc32;
	MenuItem											convert2mc28;
	MenuItem											convert2mc26;
	MenuItem											convert2mc20;
	MenuItem											convert2mx20;
	MenuItem											convert2mc16;
	MenuItem											convert2mx16;
	MenuItem											convert2mx12;
	boolean												isCreated = false;
	String sourceFilePath;

	public void createMenu(Menu popupMenu, final Transmitter transmiterCode, final String filePath) {
		sourceFilePath = filePath;
		popupMenu.addMenuListener(new MenuListener() {
			public void menuShown(MenuEvent e) {
				switch (transmiterCode) {
				case MC_32:
					ConvertContextMenu.this.convert2mc32.setEnabled(false);
					ConvertContextMenu.this.convert2mc28.setEnabled(true);
					ConvertContextMenu.this.convert2mc26.setEnabled(true);
					ConvertContextMenu.this.convert2mx20.setEnabled(true);
					ConvertContextMenu.this.convert2mc16.setEnabled(true);
					ConvertContextMenu.this.convert2mx16.setEnabled(false);
					ConvertContextMenu.this.convert2mx12.setEnabled(false);
					break;
				case MC_28:
					ConvertContextMenu.this.convert2mc32.setEnabled(true);
					ConvertContextMenu.this.convert2mc28.setEnabled(false);
					ConvertContextMenu.this.convert2mc26.setEnabled(true);
					ConvertContextMenu.this.convert2mc20.setEnabled(true);
					ConvertContextMenu.this.convert2mx20.setEnabled(true);
					ConvertContextMenu.this.convert2mc16.setEnabled(true);
					ConvertContextMenu.this.convert2mx16.setEnabled(false);
					ConvertContextMenu.this.convert2mx12.setEnabled(false);
					break;
				case MC_26:
					ConvertContextMenu.this.convert2mc32.setEnabled(true);
					ConvertContextMenu.this.convert2mc28.setEnabled(true);
					ConvertContextMenu.this.convert2mc26.setEnabled(false);
					ConvertContextMenu.this.convert2mc20.setEnabled(true);
					ConvertContextMenu.this.convert2mx20.setEnabled(true);
					ConvertContextMenu.this.convert2mc16.setEnabled(true);
					ConvertContextMenu.this.convert2mx16.setEnabled(false);
					ConvertContextMenu.this.convert2mx12.setEnabled(false);
					break;
				case MX_20:
					ConvertContextMenu.this.convert2mc32.setEnabled(true);
					ConvertContextMenu.this.convert2mc28.setEnabled(true);
					ConvertContextMenu.this.convert2mc26.setEnabled(true);
					ConvertContextMenu.this.convert2mc20.setEnabled(true);
					ConvertContextMenu.this.convert2mx20.setEnabled(false);
					ConvertContextMenu.this.convert2mc16.setEnabled(true);
					ConvertContextMenu.this.convert2mx16.setEnabled(false);
					ConvertContextMenu.this.convert2mx12.setEnabled(false);
					break;
				case MC_16:
					ConvertContextMenu.this.convert2mc32.setEnabled(true);
					ConvertContextMenu.this.convert2mc28.setEnabled(true);
					ConvertContextMenu.this.convert2mc26.setEnabled(true);
					ConvertContextMenu.this.convert2mc20.setEnabled(true);
					ConvertContextMenu.this.convert2mx20.setEnabled(true);
					ConvertContextMenu.this.convert2mc16.setEnabled(false);
					ConvertContextMenu.this.convert2mx16.setEnabled(false);
					ConvertContextMenu.this.convert2mx12.setEnabled(false);
					break;
				case MX_16:
					ConvertContextMenu.this.convert2mc32.setEnabled(false);
					ConvertContextMenu.this.convert2mc28.setEnabled(false);
					ConvertContextMenu.this.convert2mc26.setEnabled(false);
					ConvertContextMenu.this.convert2mc20.setEnabled(false);
					ConvertContextMenu.this.convert2mx20.setEnabled(false);
					ConvertContextMenu.this.convert2mc16.setEnabled(false);
					ConvertContextMenu.this.convert2mx16.setEnabled(false);
					ConvertContextMenu.this.convert2mx12.setEnabled(true);
					break;
				case MX_12:
					ConvertContextMenu.this.convert2mc32.setEnabled(false);
					ConvertContextMenu.this.convert2mc28.setEnabled(false);
					ConvertContextMenu.this.convert2mc26.setEnabled(false);
					ConvertContextMenu.this.convert2mc20.setEnabled(false);
					ConvertContextMenu.this.convert2mx20.setEnabled(false);
					ConvertContextMenu.this.convert2mc16.setEnabled(false);
					ConvertContextMenu.this.convert2mx16.setEnabled(true);
					ConvertContextMenu.this.convert2mx12.setEnabled(false);
					break;
				default:
					ConvertContextMenu.this.convert2mc32.setEnabled(false);
					ConvertContextMenu.this.convert2mc28.setEnabled(false);
					ConvertContextMenu.this.convert2mc26.setEnabled(false);
					ConvertContextMenu.this.convert2mc20.setEnabled(false);
					ConvertContextMenu.this.convert2mx20.setEnabled(false);
					ConvertContextMenu.this.convert2mx16.setEnabled(false);
					ConvertContextMenu.this.convert2mx12.setEnabled(false);
					break;
				}
			}
			public void menuHidden(MenuEvent e) {
				//ignore
			}
		});
		if (!isCreated) {

			this.convert2mc32 = new MenuItem(popupMenu, SWT.NONE);
			this.convert2mc32.setText("-> " + Transmitter.MC_32);
			this.convert2mc32.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ConvertContextMenu.log.log(Level.FINEST, "convert2mc32 action performed! " + e); //$NON-NLS-1$
					Transmitter.convert2target(sourceFilePath, Transmitter.MC_32);
				}
			});
			this.convert2mc28 = new MenuItem(popupMenu, SWT.NONE);
			this.convert2mc28.setText("-> " + Transmitter.MC_28);
			this.convert2mc28.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ConvertContextMenu.log.log(Level.FINEST, "convert2mc28 action performed! " + e); //$NON-NLS-1$
					Transmitter.convert2target(sourceFilePath, Transmitter.MC_28);
				}
			});
			this.convert2mc26 = new MenuItem(popupMenu, SWT.NONE);
			this.convert2mc26.setText("-> " + Transmitter.MC_26);
			this.convert2mc26.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ConvertContextMenu.log.log(Level.FINEST, "convert2mc26 action performed! " + e); //$NON-NLS-1$
					Transmitter.convert2target(sourceFilePath, Transmitter.MC_26);
				}
			});
			this.convert2mc20 = new MenuItem(popupMenu, SWT.NONE);
			this.convert2mc20.setText("-> " + Transmitter.MC_20);
			this.convert2mc20.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ConvertContextMenu.log.log(Level.FINEST, "convert2mc20 action performed! " + e); //$NON-NLS-1$
					Transmitter.convert2target(sourceFilePath, Transmitter.MC_20);
				}
			});
			this.convert2mx20 = new MenuItem(popupMenu, SWT.NONE);
			this.convert2mx20.setText("-> " + Transmitter.MX_20);
			this.convert2mx20.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ConvertContextMenu.log.log(Level.FINEST, "convert2mx20 action performed! " + e); //$NON-NLS-1$
					Transmitter.convert2target(sourceFilePath, Transmitter.MX_20);
				}
			});
			this.convert2mc16 = new MenuItem(popupMenu, SWT.NONE);
			this.convert2mc16.setText("-> " + Transmitter.MC_16);
			this.convert2mc16.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ConvertContextMenu.log.log(Level.FINEST, "convert2mc16 action performed! " + e); //$NON-NLS-1$
					Transmitter.convert2target(sourceFilePath, Transmitter.MC_16);
				}
			});
			this.convert2mx16 = new MenuItem(popupMenu, SWT.NONE);
			this.convert2mx16.setText("-> " + Transmitter.MX_16);
			this.convert2mx16.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ConvertContextMenu.log.log(Level.FINEST, "convert2mx16 action performed! " + e); //$NON-NLS-1$
					Transmitter.convert2target(sourceFilePath, Transmitter.MX_16);
				}
			});
			this.convert2mx12 = new MenuItem(popupMenu, SWT.NONE);
			this.convert2mx12.setText("-> " + Transmitter.MX_12);
			this.convert2mx12.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ConvertContextMenu.log.log(Level.FINEST, "convert2mx12 action performed! " + e); //$NON-NLS-1$
					Transmitter.convert2target(sourceFilePath, Transmitter.MX_12);
				}
			});
			isCreated = true;
		}
	}
}
