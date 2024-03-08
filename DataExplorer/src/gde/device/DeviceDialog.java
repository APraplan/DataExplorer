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
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018 Winfried Bruegmann
****************************************************************************************/
package gde.device;

import gde.GDE;
import gde.config.Settings;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;

/**
 * DeviceDialog is the abstract class as parent for device dialog implementations
 * @author Winfried Brügmann
 */
public abstract class DeviceDialog extends Dialog {
	final static Logger log = Logger.getLogger(DeviceDialog.class.getName());

	protected Shell			dialogShell;
	
	protected boolean 	isFailedConnectionWarned = false; // if focus adapter opens port, this flag eleminates warning loops in case of none modal dialog
	
	protected	int				shellAlpha; 
	protected boolean		isAlphaEnabled;
	protected boolean 	isFadeOut = false; // false = aplha value is lower 254
	protected boolean		isInDialog = false; // if dialog alpha fading is used this flag is used to switch off mouseExit and mouseEnter inner events
	
	protected boolean 	isClosePossible = true; // use this variable to manage if dialog can be disposed 
	protected String 		disposeDisabledMessage = Messages.getString(MessageIds.GDE_MSGW0007);
	
	protected final DataExplorer application;
	
	public MouseTrackAdapter mouseTrackerEnterFadeOut = new MouseTrackAdapter() {
		@Override
		public void mouseEnter(MouseEvent evt) {
			log.log(Level.FINE, "mouseEnter, event=" + evt); //$NON-NLS-1$
			fadeOutAplhaBlending();
		}
		@Override
		public void mouseHover(MouseEvent evt) {
			log.log(Level.FINEST, "mouseHover, event=" + evt); //$NON-NLS-1$
		}
		@Override
		public void mouseExit(MouseEvent evt) {
			log.log(Level.FINEST, "mouseEnter, event=" + evt); //$NON-NLS-1$
		}
	};	

	/**
	 * default constructor for the dialog, in most cases this dialog should not modal  
	 * @param parent
	 * @param style
	 */
	public DeviceDialog(Shell parent, int style) {
		super(parent, style);
		this.application = DataExplorer.getInstance();
		this.shellAlpha = Settings.getInstance().getDialogAlphaValue(); 
		this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();
	}

	/**
	 * constructor for the dialog, in most cases this dialog should not modal  
	 * @param parent
	 */
	public DeviceDialog(Shell parent) {
		super(parent, SWT.NONE);
		this.application = DataExplorer.getInstance();
		this.shellAlpha = Settings.getInstance().getDialogAlphaValue(); 
		this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	abstract public void open();

	/**
	 * default method to dispose (close) a dialog shell
	 * implement all cleanup operation in a disposeListener method
	 */
	public void dispose() {
		if (this.isClosePossible) {
			this.dialogShell.dispose();
			if (!this.application.isDisposed()) 
				this.application.setStatusMessage(""); //$NON-NLS-1$
			
			this.application.resetShellIcon();
		}
		else if (!this.application.isDisposed()) 
			this.application.setStatusMessage(this.disposeDisabledMessage, SWT.COLOR_RED);
	}

	public void close() {
		this.dispose();
	}

	public void forceDispose() {
		this.dialogShell.dispose();
	}
	
	/**
	 * default method to dispose (close) a dialog shell
	 * implement all cleanup operation in a disposeListener method
	 */
	public boolean isDisposed() {
		return this.dialogShell != null ? this.dialogShell.isDisposed() : true;
	}

	/**
	 * default method to drive visibility of a dialog shell
	 */
	public void setVisible(boolean value) {
		this.dialogShell.setVisible(value);
	}

	/**
	 * default method to set the focus of a dialog shell
	 */
	public boolean setFocus() {
		return this.dialogShell != null ? this.dialogShell.setFocus() : false;
	}

	/**
	 * @return the dialogShell
	 */
	public Shell getDialogShell() {
		Shell shell = GDE.shell;
		try {
			shell = this.dialogShell != null && !this.dialogShell.isDisposed() ? this.dialogShell : shell;
		}
		catch (Throwable e) {
			// return default shell
		}
		return shell;
	}

	/**
	 * @return the isClosePossible
	 */
	public boolean isClosePossible() {
		return this.isClosePossible;
	}

	/**
	 * @param enabled the boolean isClosePossible value to set
	 */
	public void setClosePossible(boolean enabled) {
		this.isClosePossible = enabled;
	}

	public int getShellAlpha() {
		return this.shellAlpha;
	}

	public void setShellAlpha(int newShellAlpha) {
			if (newShellAlpha > this.shellAlpha) {
				//System.out.println("fade-out " + this.shellAlpha);
				for (int i = this.shellAlpha; i < 254; i+=5) {
					this.dialogShell.setAlpha(i);
					//System.out.print(i + " ");
				}
				this.dialogShell.setAlpha(253);
				//System.out.println("fade-out " + this.dialogShell.getAlpha());
				this.isFadeOut = true;
			}
			else {
				//System.out.println("fade-in " + this.dialogShell.getAlpha());
				for (int i = 254; i > this.shellAlpha; i-=5) {
					this.dialogShell.setAlpha(i);
					//System.out.print(i + " ");
				}
				//System.out.println();
				this.dialogShell.setAlpha(this.shellAlpha);
				//System.out.println("fade-in " + this.dialogShell.getAlpha());
				this.isFadeOut = false;
			}
	}

	public boolean isAlphaEnabled() {
		return this.isAlphaEnabled;
	}

	/**
	 * @return the isFailedConnectionWarned
	 */
	public boolean isFailedConnectionWarned() {
		return this.isFailedConnectionWarned;
	}

	/**
	 * @param enabled the enabled to set
	 */
	public void setFailedConnectionWarned(boolean enabled) {
		this.isFailedConnectionWarned = enabled;
	}
		
	/**
	 * fade out alpha blending from 254 to the configured alpha value
	 */
	public void fadeOutAplhaBlending() {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "this.isFadeOut = " + this.isFadeOut); //$NON-NLS-1$
		if (!this.isFadeOut && this.isAlphaEnabled) {
			setShellAlpha(254);
		}
	}

	/**
	 * fade out alpha blending from 254 to the configured alpha value
	 * @param evt
	 * @param outherBoundSize
	 * @param left gap
	 * @param right gap
	 * @param top gap
	 * @param bottom gap
	 */
	public void fadeOutAplhaBlending(MouseEvent evt, Point outherBoundSize, int left, int right, int top, int bottom) {
		boolean isEnterShellEvt = (evt.x < left || evt.x > outherBoundSize.x - right || evt.y < top || evt.y > outherBoundSize.y - bottom) ? true : false;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isEnterShellEvt = " + isEnterShellEvt + " size = " + outherBoundSize); //$NON-NLS-1$ //$NON-NLS-2$
		if (!this.isFadeOut && isEnterShellEvt && this.isAlphaEnabled) {
			setShellAlpha(254);
		}
	}

	/**
	 * fade in alpha blending the configured alpha value to 254
	 * @param evt
	 * @param outherBoundSize
	 * @param left gap
	 * @param right gap
	 * @param top gap
	 * @param bottom gap
	 */
	public void fadeInAlpaBlending(MouseEvent evt, Point outherBoundSize, int left, int right, int top, int bottom) {
		boolean isExitShellEvt = (evt.x < left || evt.x > outherBoundSize.x - right || evt.y < top || evt.y > outherBoundSize.y - bottom) ? true : false;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isExitShellEvt = " + isExitShellEvt + " size = " + outherBoundSize); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.isFadeOut && isExitShellEvt && this.isAlphaEnabled) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isExitShellEvt = " + isExitShellEvt + " size = " + outherBoundSize); //$NON-NLS-1$ //$NON-NLS-2$
			setShellAlpha(getShellAlpha());
		}
	}

	/**
	 * fade out alpha blending from 254 to the configured alpha value
	 * @param evt
	 * @param outherBound
	 * @param left gap
	 * @param right gap
	 * @param top gap
	 * @param bottom gap
	 */
	public void fadeOutAplhaBlending(MouseEvent evt, Rectangle outherBound, int left, int right, int top, int bottom) {
		boolean isEnterShellEvt = (evt.x < left || evt.x > outherBound.width - right || evt.y < top || evt.y > outherBound.height - bottom) ? true : false;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isEnterShellEvt = " + isEnterShellEvt + " size = " + outherBound.width + "," + outherBound.height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (!this.isFadeOut && isEnterShellEvt && this.isAlphaEnabled) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isEnterShellEvt = " + isEnterShellEvt + " size = " + outherBound.width + "," + outherBound.height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			setShellAlpha(254);
		}
	}

	/**
	 * fade in alpha blending the configured alpha value to 254
	 * @param evt
	 * @param outherBound
	 * @param left gap
	 * @param right gap
	 * @param top gap
	 * @param bottom gap
	 */
	public void fadeInAlpaBlending(MouseEvent evt, Rectangle outherBound, int left, int right, int top, int bottom) {
		boolean isExitShellEvt = (evt.x < left || evt.x > outherBound.width - right || evt.y < top || evt.y > outherBound.height - bottom) ? true : false;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isExitShellEvt = " + isExitShellEvt + " size = " + outherBound.width + "," + outherBound.height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (this.isFadeOut && isExitShellEvt && this.isAlphaEnabled) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isExitShellEvt = " + isExitShellEvt + " size = " + outherBound.width + "," + outherBound.height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			setShellAlpha(getShellAlpha());
		}
	}
	
	/**
	 * switch a save button to enabled state or vice versa
	 * @param enable
	 */
	public void enableSaveButton(boolean enable) {
		//noop - whenever a dialog has a save button relating to device property changes it must implement this method 
	}
}
