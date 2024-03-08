/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.
    
    Copyright (c) 2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.skyrc;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.comm.IDeviceCommPort;
import gde.config.Settings;
import gde.data.Channels;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

/**
 * simple dialog implementation to display only minor information
 * @author Winfried Bruegmann
 */
public class ChargerDialog extends DeviceDialog {
	final static Logger						log									= Logger.getLogger(ChargerDialog.class.getName());
	static       String						DEVICE_NAME					= "Q200";

	Button												closeButton;

	Composite											boundsComposite;
	Text													infoText;
	Text 													infoText2;

	final IDevice									device;						// get device specific things, get serial port, ...
	final IDeviceCommPort					comPort;					// open/close port execute getData()....
	final Channels								channels;					// interaction with channels, source of all records
	final Settings								settings;					// application configuration settings

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public ChargerDialog(Shell parent, IDevice useDevice) {
		super(parent);
		this.comPort = useDevice.getCommunicationPort();
		this.device = useDevice;
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	@Override
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue(); 
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			ChargerDialog.log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);
				this.dialogShell.setLayout(new FormLayout());
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.layout();
				this.dialogShell.pack();
				this.dialogShell.setSize(350, 250);
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						ChargerDialog.log.log(Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						ChargerDialog.this.application.openHelpDialog(DEVICE_NAME, "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						ChargerDialog.this.dispose();
					}
				});
				{
					this.boundsComposite = new Composite(this.dialogShell, SWT.NONE);
					FormData boundsCompositeLData = new FormData();
					boundsCompositeLData.left = new FormAttachment(0, 1000, 0);
					boundsCompositeLData.right = new FormAttachment(1000, 1000, 0);
					boundsCompositeLData.top = new FormAttachment(0, 1000, 0);
					boundsCompositeLData.bottom = new FormAttachment(1000, 1000, 0);
					this.boundsComposite.setLayoutData(boundsCompositeLData);
					this.boundsComposite.setLayout(new FormLayout());
					this.boundsComposite.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							ChargerDialog.log.log(Level.FINER, "boundsComposite.paintControl() " + evt); //$NON-NLS-1$
						}
					});
					{
						FormData infoTextLData = new FormData();
						infoTextLData.height = 120;
						infoTextLData.left = new FormAttachment(0, 1000, 5);
						infoTextLData.top = new FormAttachment(0, 1000, 10);
						infoTextLData.right = new FormAttachment(1000, 1000, -5);
						this.infoText = new Text(this.boundsComposite, SWT.MULTI | SWT.WRAP | SWT.LEFT);
						this.infoText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.infoText.setLayoutData(infoTextLData);
						this.infoText.setText(Messages.getString(MessageIds.GDE_MSGT3690));
						this.infoText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
						this.infoText.setEditable(false);
					}
					{
						FormData configGroupLData = new FormData();
						configGroupLData.height = 40;
						configGroupLData.left = new FormAttachment(0, 1000, 5);
						configGroupLData.top = new FormAttachment(0, 1000, 140);
						configGroupLData.right = new FormAttachment(1000, 1000, -5);
						this.infoText2 = new Text(this.boundsComposite, SWT.MULTI | SWT.WRAP | SWT.LEFT);
						this.infoText2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.infoText2.setLayoutData(configGroupLData);
						this.infoText2.setText(Messages.getString(MessageIds.GDE_MSGT3691));
						this.infoText2.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
						this.infoText2.setEditable(false);
					}
					{
						FormData closeButtonLData = new FormData();
						closeButtonLData.height = 30;
						closeButtonLData.bottom = new FormAttachment(1000, 1000, -12);
						closeButtonLData.left = new FormAttachment(0, 1000, 12);
						closeButtonLData.right = new FormAttachment(1000, 1000, -12);
						this.closeButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.closeButton.setLayoutData(closeButtonLData);
						this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0188));
						this.closeButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								ChargerDialog.log.log(Level.FINEST, "okButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								close();
							}
						});
						this.closeButton.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
					}
				} // end boundsComposite
				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 175, 100));
				this.dialogShell.open();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			ChargerDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}
