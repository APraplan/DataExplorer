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
package gde.ui;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import gde.GDE;
import gde.log.Level;

/**
 * user interface status bar class, device, serial port, port activity, progress bar, messages
 * @author Winfried Brügmann
 */
public class StatusBar {
	final Logger	log	= Logger.getLogger(this.getClass().getName());

	final DataExplorer 			application;
	final Composite					statusComposite;
	Composite								connectionComposite;
	Label										txText;
	Label										rxText;
	Label										conText;
	CLabel									portButton;
	CLabel									txButton;
	CLabel									rxButton;
	Composite								comComposite;
	Label										msgLabel;
	Composite								progressComposite;
	ProgressBar							progressBar;

	public StatusBar(Composite currentStatusComposite) {
		this.application = DataExplorer.getInstance();
		this.statusComposite = currentStatusComposite;
		RowLayout statusCompositeLayout = new RowLayout(SWT.HORIZONTAL);
		statusCompositeLayout.center = true;
		this.statusComposite.setLayout(statusCompositeLayout);
		this.statusComposite.setBackground(this.application.COLOR_BACKGROUND);
		this.statusComposite.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent evt) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "statusComposite.paintControl evt=" + evt); //$NON-NLS-1$
				Point statusCompositeSize = StatusBar.this.statusComposite.getSize();
				Point comCompositeSize = StatusBar.this.comComposite.getSize();
				int offsetX = comCompositeSize.x+280;
				int offsetY = (statusCompositeSize.y - 20) / 2;
				int width = statusCompositeSize.x-offsetX-10;
				StatusBar.this.msgLabel.setBounds(offsetX, offsetY, width, 20);
			}
		});
	}

	public void create() {
		{
			this.connectionComposite = new Composite(this.statusComposite, SWT.NONE);
			this.connectionComposite.setBackground(this.application.COLOR_BACKGROUND);
			GridLayout composite2Layout1 = new GridLayout();
			composite2Layout1.makeColumnsEqualWidth = true;
			this.connectionComposite.setLayout(composite2Layout1);
			this.connectionComposite.setLayoutData(new RowData(170, 24));
			{
				this.comComposite = new Composite(this.connectionComposite, SWT.NONE);
				this.comComposite.setBackground(this.application.COLOR_BACKGROUND);
				FillLayout comCompositeLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
				GridData comCompositeLData = new GridData();
				comCompositeLData.verticalAlignment = GridData.FILL;
				comCompositeLData.horizontalAlignment = GridData.FILL;
				comCompositeLData.grabExcessVerticalSpace = true;
				this.comComposite.setLayoutData(comCompositeLData);
				this.comComposite.setLayout(comCompositeLayout);
				{
					this.portButton = new CLabel(this.comComposite, SWT.NONE);
					this.portButton.setBounds(2,2, 50, 20);
					this.portButton.setForeground(this.application.COLOR_DARK_GREEN);
					this.portButton.setBackground(this.application.COLOR_BACKGROUND);
					this.portButton.setImage(SWTResourceManager.getImage("gde/resource/LEDGreen.gif")); //$NON-NLS-1$
				}
				{
					this.conText = new Label(this.comComposite, SWT.LEFT);
					this.conText.setText("CON"); //$NON-NLS-1$
					this.conText.setFont(SWTResourceManager.getFont(this.statusComposite.getParent(), GDE.WIDGET_FONT_SIZE-2, SWT.NORMAL));
					this.conText.setBackground(this.application.COLOR_BACKGROUND);
					this.conText.setForeground(this.application.COLOR_FOREGROUND);
				}
				{
					this.rxButton = new CLabel(this.comComposite, SWT.CENTER);
					this.rxButton.setBounds(2,2, 50, 20);
					this.rxButton.setForeground(this.application.COLOR_DARK_GREEN);
					this.rxButton.setBackground(this.application.COLOR_BACKGROUND);
					this.rxButton.setImage(SWTResourceManager.getImage("gde/resource/LEDGreen.gif")); //$NON-NLS-1$
				}
				{
					this.rxText = new Label(this.comComposite, SWT.LEFT);
					this.rxText.setBackground(this.application.COLOR_BACKGROUND);
					this.rxText.setForeground(this.application.COLOR_FOREGROUND);
					this.rxText.setFont(SWTResourceManager.getFont(this.statusComposite.getParent(), GDE.WIDGET_FONT_SIZE-2, SWT.NORMAL));
					this.rxText.setText("RX"); //$NON-NLS-1$
				}
				{
					this.txButton = new CLabel(this.comComposite, SWT.CENTER);
					this.txButton.setBounds(2,2, 50, 20);
					this.txButton.setBackground(this.application.COLOR_BACKGROUND);
					this.txButton.setImage(SWTResourceManager.getImage("gde/resource/LEDGreen.gif")); //$NON-NLS-1$
				}
				{
					this.txText = new Label(this.comComposite, SWT.LEFT);
					this.txText.setBackground(this.application.COLOR_BACKGROUND);
					this.txText.setForeground(this.application.COLOR_FOREGROUND);
					this.txText.setFont(SWTResourceManager.getFont(this.statusComposite.getParent(), GDE.WIDGET_FONT_SIZE-2, SWT.NORMAL));
					this.txText.setText("TX"); //$NON-NLS-1$
				}
				this.comComposite.pack();
			}
			{
				this.progressComposite = new Composite(this.statusComposite, SWT.NONE);
				this.progressComposite.setBackground(this.application.COLOR_BACKGROUND);
				FillLayout progressCompositeLayout = new FillLayout(SWT.HORIZONTAL);
				this.progressComposite.setLayout(progressCompositeLayout);
				{
					this.progressBar = new ProgressBar(this.progressComposite, SWT.NONE);
					this.progressBar.setBounds(2, 2, 250, 20);
					this.progressBar.setBackground(this.application.COLOR_BACKGROUND);
					this.progressBar.setMinimum(0);
					this.progressBar.setMaximum(100);
					this.progressBar.setSelection(0);
				}
				this.progressComposite.pack();
			}
			{
				this.msgLabel = new Label(this.statusComposite, SWT.LEFT | SWT.SINGLE);
				this.msgLabel.setBackground(this.application.COLOR_BACKGROUND);
				this.msgLabel.setForeground(this.application.COLOR_FOREGROUND);
				this.msgLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			}
		}
		this.statusComposite.pack();
	}

	/**
	 * method to set a message text to the message label of the status bar
	 */
	public void setMessage(final String text, int swtColor) {
		this.msgLabel.setForeground(SWTResourceManager.getColor(swtColor));
		this.msgLabel.setText(text);
	}

	/**
	 * method to set a message text to the message label of the status bar
	 */
	public void setMessage(final String text) {
		this.msgLabel.setForeground(DataExplorer.getInstance().COLOR_FOREGROUND);
		this.msgLabel.setText(text);
	}

	public void setProgress(final int percentage) {
		this.progressBar.setSelection(percentage);
	}

	public int getProgressPercentage() {
		return this.progressBar.getSelection();
	}

	/**
	 * set the serial com port rx light on
	 */
	public void setSerialRxOn() {
		if (!this.rxButton.isDisposed()) {
			this.rxButton.setImage(SWTResourceManager.getImage("gde/resource/LEDHotGreen.gif")); //$NON-NLS-1$
			this.rxButton.redraw();
		}
	}

	/**
	 * set the serial com port rx light off
	 */
	public void setSerialRxOff() {
		if (!this.rxButton.isDisposed()) {
			this.rxButton.setImage(SWTResourceManager.getImage("gde/resource/LEDGreen.gif")); //$NON-NLS-1$
			this.rxButton.redraw();
		}
	}

	/**
	 * set the serial com port tx light on
	 */
	public void setSerialTxOn() {
		if (!this.txButton.isDisposed()) {
			this.txButton.setImage(SWTResourceManager.getImage("gde/resource/LEDHotGreen.gif")); //$NON-NLS-1$
			this.txButton.redraw();
		}
	}

	/**
	 * set the serial com port tx light off
	 */
	public void setSerialTxOff() {
		if (!this.txButton.isDisposed()) {
			this.txButton.setImage(SWTResourceManager.getImage("gde/resource/LEDGreen.gif")); //$NON-NLS-1$
			this.txButton.redraw();
		}
	}

	/**
	 * set the serial com port light on
	 */
	public void setSerialPortConnected() {
		if (!this.portButton.isDisposed()) {
			this.portButton.setImage(SWTResourceManager.getImage("gde/resource/LEDHotGreen.gif")); //$NON-NLS-1$
			this.portButton.redraw();
		}
	}

	/**
	 * set the serial com port light off
	 */
	public void setSerialPortDisconnected() {
		if (!this.portButton.isDisposed()) {
			this.portButton.setImage(SWTResourceManager.getImage("gde/resource/LEDGreen.gif")); //$NON-NLS-1$
			this.portButton.redraw();
		}
	}

	/**
	 * update background/foreground color of the tool bar
	 */
	public void updateColorSchema() {
		this.statusComposite.setBackground(this.application.COLOR_BACKGROUND);
		this.connectionComposite.setBackground(this.application.COLOR_BACKGROUND);
		this.comComposite.setBackground(this.application.COLOR_BACKGROUND);
		this.portButton.setBackground(this.application.COLOR_BACKGROUND);
		this.conText.setBackground(this.application.COLOR_BACKGROUND);
		this.conText.setForeground(this.application.COLOR_FOREGROUND);
		this.rxButton.setBackground(this.application.COLOR_BACKGROUND);
		this.rxText.setBackground(this.application.COLOR_BACKGROUND);
		this.rxText.setForeground(this.application.COLOR_FOREGROUND);
		this.txButton.setBackground(this.application.COLOR_BACKGROUND);
		this.txText.setBackground(this.application.COLOR_BACKGROUND);
		this.txText.setForeground(this.application.COLOR_FOREGROUND);
		this.progressBar.setBackground(this.application.COLOR_BACKGROUND);
		this.msgLabel.setBackground(this.application.COLOR_BACKGROUND);
	}
}
