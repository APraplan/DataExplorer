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
package gde.ui.dialog;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import gde.log.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import gde.GDE;
import gde.config.Settings;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.FileUtils;

/**
 * simple HTTP browser to display help info material
 * @author Winfried Brügmann
 */
public class HelpInfoDialog extends Dialog {
	final static Logger	log	= Logger.getLogger(HelpInfoDialog.class.getName());

	Shell		dialogShell;
	Browser	textBrowser;
	final Rectangle primaryMonitorBounds;
	final Settings	settings;


	public HelpInfoDialog(Shell parent, int style) {
		super(parent, style);
		this.primaryMonitorBounds = parent.getDisplay().getPrimaryMonitor().getBounds();
		this.settings = Settings.getInstance();
	}
	
	/**
	 * opens a fileURL of help HTML of the given device and pageName, 
	 * preferred style is SWT.MOZILLA which supports xulrunner on all platforms 
	 * @param deviceName
	 * @param fileName
	 * @param style
	 */
	public void open(String deviceName, String fileName, int style, boolean extractBase) {
		log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.dialogShell == null || this.dialogShell.isDisposed()) {
			this.dialogShell = new Shell(new Shell(Display.getDefault()), SWT.SHELL_TRIM);
			FillLayout dialogShellLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
			this.dialogShell.setLayout(dialogShellLayout);
			this.dialogShell.setText(GDE.NAME_LONG + Messages.getString(MessageIds.GDE_MSGT0192));
			this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/DataExplorer.png")); //$NON-NLS-1$

			this.textBrowser = new Browser(this.dialogShell, style);
			openURL(deviceName, fileName, extractBase);

			this.dialogShell.layout();
			this.dialogShell.pack();
			int width = this.primaryMonitorBounds.width / 4 * 3;
			this.dialogShell.setSize(width, (this.primaryMonitorBounds.height * 95 / 100));
			this.dialogShell.setLocation(this.primaryMonitorBounds.width - width, 0);

			this.dialogShell.open();
		}
		else {
			this.textBrowser = this.textBrowser == null ? new Browser(this.dialogShell, style) : this.textBrowser;
			openURL(deviceName, fileName, extractBase);
			
			int width = this.primaryMonitorBounds.width / 4 * 3;
			this.dialogShell.setSize(width, (this.primaryMonitorBounds.height * 95 / 100));
			this.dialogShell.setLocation(this.primaryMonitorBounds.width - width, 0);
			this.dialogShell.setMinimized(false);
			this.dialogShell.setVisible(true);
			this.dialogShell.forceActive();
		}
		this.dialogShell.addListener(SWT.Traverse, new Listener() {
      public void handleEvent(Event event) {
        switch (event.detail) {
        case SWT.TRAVERSE_ESCAPE:
        	HelpInfoDialog.this.dialogShell.close();
          event.detail = SWT.TRAVERSE_NONE;
          event.doit = false;
          break;
        }
      }
    });
		Display display = this.dialogShell.getDisplay();
		while (!this.dialogShell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
	}

	/**
	 * @param deviceName
	 * @param fileName
	 * @param extractBase
	 */
	private void openURL(String deviceName, String fileName, boolean extractBase) {
		String jarBasePath = FileUtils.getJarBasePath() + GDE.STRING_FILE_SEPARATOR_UNIX;
		String jarName = GDE.NAME_LONG + GDE.FILE_ENDING_DOT_JAR;
		final String lang = this.settings.getLocale().getLanguage().contains("de") || this.settings.getLocale().getLanguage().contains("en") 
				? this.settings.getLocale().getLanguage() : "en";

		String helpDir = extractBase ? "help" + GDE.FILE_SEPARATOR : "help" + GDE.FILE_SEPARATOR + lang + GDE.FILE_SEPARATOR;
		String helpFileDir = "help" + GDE.FILE_SEPARATOR + lang + GDE.FILE_SEPARATOR;
		String targetDir = GDE.JAVA_IO_TMPDIR + (GDE.IS_WINDOWS ? "" : GDE.STRING_FILE_SEPARATOR_UNIX) + "GDE" + GDE.STRING_FILE_SEPARATOR_UNIX;
		
		try {
			if (!(new File(targetDir)).exists()) {
				log.log(Level.FINE, "jarBasePath = " + jarBasePath + " jarName = " + jarName + " helpDir = " + helpDir); //$NON-NLS-1$
				//extract DataExplorer base help content each time if needed to allow references
				FileUtils.extractDir(new JarFile(jarBasePath + jarName), helpDir, targetDir, "555");
			}
			
			if (deviceName.length() >= 1) { // devices/<deviceName>.jar
				jarBasePath = jarBasePath + "devices" + GDE.STRING_FILE_SEPARATOR_UNIX;
				jarName = deviceName + GDE.FILE_ENDING_DOT_JAR;
				targetDir = targetDir + deviceName + GDE.STRING_FILE_SEPARATOR_UNIX;

				FileUtils.extractDir(new JarFile(jarBasePath + jarName), helpDir, targetDir, "555");
			}
		
		
			String stringUrl = (targetDir + helpFileDir + fileName).replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
			log.log(Level.FINE, "stringUrl = " + "file:///" + stringUrl); //$NON-NLS-1$ //$NON-NLS-2$
			
			this.textBrowser.setUrl("file:///" + stringUrl); //$NON-NLS-1$
		}
		catch (IOException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialog(this.dialogShell, 
					Messages.getString(MessageIds.GDE_MSGE0018, new Object[] { e.getLocalizedMessage() } )); //$NON-NLS-1$
		}
	}
	
	public boolean isDisposed() {
		return this.dialogShell != null ? this.dialogShell.isDisposed() : true;
	}
	
	public void dispose() {
		if (this.dialogShell != null) this.dialogShell.dispose();
	}
}
