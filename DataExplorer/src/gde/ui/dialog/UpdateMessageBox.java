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

    Copyright (c) 2024 Winfried Bruegmann
****************************************************************************************/
package gde.ui.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

public class UpdateMessageBox extends Dialog {

	protected Boolean	result = false;
	protected Shell		shell;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public UpdateMessageBox(Shell parent, int style) {
		super(parent, style);
		setText(Messages.getString(MessageIds.GDE_MSGI0052));
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public boolean open() {
		createContents();
		shell.open();
		shell.layout();
		shell.setLocation(500, 300);
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), SWT.BORDER | SWT.TITLE);
		shell.setSize(350, 250);
		shell.setText(getText());
		shell.setLayout(new FormLayout());
		
		Text txttNewUpdateAvail = new Text(shell, SWT.CENTER);
		FormData fd_txttNewUpdateAvail = new FormData();
		fd_txttNewUpdateAvail.right = new FormAttachment(100, -5);
		fd_txttNewUpdateAvail.top = new FormAttachment(0, 15);
		fd_txttNewUpdateAvail.left = new FormAttachment(0, 5);
		fd_txttNewUpdateAvail.height = 25;
		txttNewUpdateAvail.setLayoutData(fd_txttNewUpdateAvail);
		txttNewUpdateAvail.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		txttNewUpdateAvail.setEditable(false);
		txttNewUpdateAvail.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		txttNewUpdateAvail.setText(Messages.getString(MessageIds.GDE_MSGI0072));
		
		Text txtDownloadProblemHint = new Text(shell, SWT.WRAP | SWT.CENTER | SWT.MULTI);
		FormData fd_txtDownloadProblemHint = new FormData();
		fd_txtDownloadProblemHint.right = new FormAttachment(100, -20);
		fd_txtDownloadProblemHint.top = new FormAttachment(0, 50);
		fd_txtDownloadProblemHint.left = new FormAttachment(0, 20);
		fd_txtDownloadProblemHint.height = 30;
		txtDownloadProblemHint.setLayoutData(fd_txtDownloadProblemHint);
		txtDownloadProblemHint.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		txtDownloadProblemHint.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		txtDownloadProblemHint.setText(Messages.getString(MessageIds.GDE_MSGI0073));
		
		Text txtBetaVersionHint = new Text(shell, SWT.WRAP | SWT.CENTER | SWT.MULTI);
		FormData fd_txtBetaVersionHint = new FormData();
		fd_txtBetaVersionHint.right = new FormAttachment(100, -20);
		fd_txtBetaVersionHint.top = new FormAttachment(0, 90);
		fd_txtBetaVersionHint.left = new FormAttachment(0, 20);
		fd_txtBetaVersionHint.height = 30;
		txtBetaVersionHint.setLayoutData(fd_txtBetaVersionHint);
		txtBetaVersionHint.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		txtBetaVersionHint.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		txtBetaVersionHint.setText(Messages.getString(MessageIds.GDE_MSGI0074));
		txtBetaVersionHint.setEditable(false);
		
		Link linkNews = new Link(shell, SWT.NONE);
		FormData fd_linkNews = new FormData();
		fd_linkNews.top = new FormAttachment(0, 140);
		fd_linkNews.left = new FormAttachment(0, 20);
		fd_linkNews.height = 25;
		fd_linkNews.width = 180;
		linkNews.setLayoutData(fd_linkNews);
		linkNews.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		linkNews.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		linkNews.setText("<a>" + Messages.getString(MessageIds.GDE_MSGI0075) + "</a>");
		linkNews.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Program.launch("https://savannah.nongnu.org/news/?group=dataexplorer");
			}
		});
		
		Link linkDonate = new Link(shell, SWT.NONE);
		FormData fd_linkDonate = new FormData();
		fd_linkDonate.right = new FormAttachment(100, -20);
		fd_linkDonate.top = new FormAttachment(0, 140);
		fd_linkDonate.width = 80;
		fd_linkDonate.height = 25;
		linkDonate.setLayoutData(fd_linkDonate);
		linkDonate.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		linkDonate.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		linkDonate.setText("<a>" + Messages.getString(MessageIds.GDE_MSGI0076) + "</a>");
		linkDonate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Program.launch("https://www.paypal.com/paypalme/WinfriedBruegmann");
			}
		});
	
		Composite composite = new Composite(shell, SWT.NONE);
		FormData fd_composite = new FormData();
		fd_composite.right = new FormAttachment(100, -5);
		fd_composite.bottom = new FormAttachment(100, -5);
		fd_composite.left = new FormAttachment(0, 5);
		fd_composite.height = 50;
		composite.setLayoutData(fd_composite);
		composite.setLayout(new FormLayout());
		
		Button btnNo = new Button(composite, SWT.BORDER | SWT.CENTER);
		FormData fd_btnNo = new FormData();
		fd_btnNo.left = new FormAttachment(0, 50);
		fd_btnNo.bottom = new FormAttachment(100, -10);
		fd_btnNo.width = 100;
		btnNo.setLayoutData(fd_btnNo);
		btnNo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		btnNo.setText(Messages.getString(MessageIds.GDE_MSGI0078));
		btnNo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shell.dispose();
			}
		});
		
		Button btnYes = new Button(composite, SWT.BORDER | SWT.CENTER);
		FormData fd_btnYes = new FormData();
		fd_btnYes.right = new FormAttachment(100, -50);
		fd_btnYes.bottom = new FormAttachment(100, -10);
		fd_btnYes.width = 100;
		btnYes.setLayoutData(fd_btnYes);
		btnYes.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		btnYes.setText(Messages.getString(MessageIds.GDE_MSGI0077));
		btnYes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				result = true;
				shell.dispose();
			}
		});
	}
}
