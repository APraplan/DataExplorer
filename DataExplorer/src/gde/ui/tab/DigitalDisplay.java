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

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;
import gde.ui.menu.TabAreaContextMenu.TabMenuType;

/**
 * Child display class displaying digital active measurements
 * @author Winfried Brügmann
 */
public class DigitalDisplay extends Composite {
	final static Logger				log				= Logger.getLogger(DigitalDisplay.class.getName());

	CLabel										textDigitalLabel;
	CLabel										actualDigitalLabel, maxDigitalLabel, minDigitalLabel;
	Composite									minMaxComposite;

	static int								fontSize	= 50;

	final DataExplorer				application;
	final Channels						channels;
	final String							recordKey;
	final IDevice							device;

	final Color								backgroundColor;
	final Menu								popupmenu;
	final TabAreaContextMenu	contextMenu;

	public DigitalDisplay(DataExplorer currentApplication, Composite digitalWindow, String currentRecordKey, IDevice currentDevice) {
		super(digitalWindow, SWT.BORDER);
		FillLayout digitalComposite1Layout = new FillLayout(SWT.VERTICAL);
		this.setLayout(digitalComposite1Layout);
		this.recordKey = currentRecordKey;
		this.device = currentDevice;
		this.application = currentApplication;
		this.channels = Channels.getInstance();

		this.backgroundColor = Settings.getInstance().getDigitalInnerAreaBackground();
		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();
		this.contextMenu.createMenu(this.popupmenu, TabMenuType.DIGITAL);
	}

	public void create() {
		{
			this.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(final PaintEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "digitalLabel.paintControl, event=" + evt); //$NON-NLS-1$
					Channel activeChannel = DigitalDisplay.this.channels.getActiveChannel();
					if (activeChannel != null) {
						RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
						if (activeRecordSet != null) {
							Record record = activeRecordSet.get(DigitalDisplay.this.recordKey);
							if (record != null && record.size() > 0) {
								if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "update label for " + DigitalDisplay.this.recordKey); //$NON-NLS-1$
								DigitalDisplay.this.textDigitalLabel
										.setText(activeRecordSet.get(DigitalDisplay.this.recordKey).getName() + " [ " + activeRecordSet.get(DigitalDisplay.this.recordKey).getUnit() + " ]"); //$NON-NLS-1$//$NON-NLS-2$
								String actualValue = record.getFormattedStatisticsValue(record.lastElement() / 1000.0);
								String maxValue = Messages.getString(MessageIds.GDE_MSGT0236) + record.getFormattedStatisticsValue(record.getMaxValue() / 1000.0);
								String minValue = Messages.getString(MessageIds.GDE_MSGT0237) + record.getFormattedStatisticsValue(record.getMinValue() / 1000.0);
								if (log.isLoggable(Level.FINE)) log.log(Level.FINE, DigitalDisplay.this.recordKey + " actualValue=" + actualValue + " maxValue=" + maxValue + " minValue=" + minValue); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
								DigitalDisplay.this.actualDigitalLabel.setForeground(SWTResourceManager.getColor(record.getRGB()));
								DigitalDisplay.this.actualDigitalLabel.setText(actualValue);
								DigitalDisplay.this.maxDigitalLabel.setText(maxValue);
								DigitalDisplay.this.minDigitalLabel.setText(minValue);
							}
						}
					}
				}
			});
			this.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "DigitalDisplay.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_7.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.textDigitalLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.textDigitalLabel.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + 4, SWT.BOLD));
			this.textDigitalLabel.setBackground(this.backgroundColor);
			this.textDigitalLabel.setMenu(this.popupmenu);
			this.textDigitalLabel.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(final PaintEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "digitalLabel.paintControl, event=" + evt); //$NON-NLS-1$
					Channel activeChannel = DigitalDisplay.this.channels.getActiveChannel();
					if (activeChannel != null) {
						RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
						if (activeRecordSet != null) {
							Record record = activeRecordSet.get(DigitalDisplay.this.recordKey);
							if (record != null) {
								if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "update label for " + DigitalDisplay.this.recordKey); //$NON-NLS-1$
								DigitalDisplay.this.textDigitalLabel.setText(activeRecordSet.get(DigitalDisplay.this.recordKey).getName() + " [ " + activeRecordSet.get(DigitalDisplay.this.recordKey).getUnit() + " ]");
							}
						}
					}
				}
			});
		}
		{
			this.actualDigitalLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.actualDigitalLabel.setBackground(this.backgroundColor);
			this.actualDigitalLabel.setText("00,00"); //$NON-NLS-1$
			this.actualDigitalLabel.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + fontSize, SWT.NORMAL));
			this.actualDigitalLabel.setMenu(this.popupmenu);
			this.actualDigitalLabel.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(final PaintEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "digitalLabel.paintControl, event=" + evt); //$NON-NLS-1$
					Channel activeChannel = DigitalDisplay.this.channels.getActiveChannel();
					if (activeChannel != null) {
						RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
						if (activeRecordSet != null) {
							Record record = activeRecordSet.get(DigitalDisplay.this.recordKey);
							if (record != null && record.size() > 0) {
								String actualValue = record.getFormattedStatisticsValue(record.lastElement() / 1000.0);
								if (log.isLoggable(Level.FINE)) log.log(Level.FINE, DigitalDisplay.this.recordKey + " actualValue=" + actualValue); //$NON-NLS-1$
								DigitalDisplay.this.actualDigitalLabel.setForeground(SWTResourceManager.getColor(record.getRGB()));
								DigitalDisplay.this.actualDigitalLabel.setText(actualValue);
							}
						}
					}
				}
			});
		}
		{
			this.minMaxComposite = new Composite(this, SWT.NONE);
			FillLayout digitalComposite1Layout = new FillLayout(SWT.HORIZONTAL);
			this.minMaxComposite.setLayout(digitalComposite1Layout);

			this.minDigitalLabel = new CLabel(this.minMaxComposite, SWT.CENTER | SWT.EMBEDDED);
			this.minDigitalLabel.setText("MIN : 00,00"); //$NON-NLS-1$
			this.minDigitalLabel.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + 2, SWT.BOLD));
			this.minDigitalLabel.setBackground(this.backgroundColor);
			this.minDigitalLabel.setMenu(this.popupmenu);
			this.minDigitalLabel.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(final PaintEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "digitalLabel.paintControl, event=" + evt); //$NON-NLS-1$
					Channel activeChannel = DigitalDisplay.this.channels.getActiveChannel();
					if (activeChannel != null) {
						RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
						if (activeRecordSet != null) {
							Record record = activeRecordSet.get(DigitalDisplay.this.recordKey);
							if (record != null && record.size() > 0) {
								String minValue = Messages.getString(MessageIds.GDE_MSGT0237) + record.getFormattedStatisticsValue(record.getMinValue() / 1000.0);
								if (log.isLoggable(Level.FINE)) log.log(Level.FINE, DigitalDisplay.this.recordKey + " minValue=" + minValue); //$NON-NLS-1$
								DigitalDisplay.this.minDigitalLabel.setText(minValue);
							}
						}
					}
				}
			});

			this.maxDigitalLabel = new CLabel(this.minMaxComposite, SWT.CENTER | SWT.EMBEDDED);
			this.maxDigitalLabel.setText("MAX : 00,00"); //$NON-NLS-1$
			this.maxDigitalLabel.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + 2, SWT.BOLD));
			this.maxDigitalLabel.setBackground(this.backgroundColor);
			this.maxDigitalLabel.setMenu(this.popupmenu);
			this.maxDigitalLabel.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(final PaintEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "digitalLabel.paintControl, event=" + evt); //$NON-NLS-1$
					Channel activeChannel = DigitalDisplay.this.channels.getActiveChannel();
					if (activeChannel != null) {
						RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
						if (activeRecordSet != null) {
							Record record = activeRecordSet.get(DigitalDisplay.this.recordKey);
							if (record != null && record.size() > 0) {
								String maxValue = Messages.getString(MessageIds.GDE_MSGT0236) + record.getFormattedStatisticsValue(record.getMaxValue() / 1000.0);
								if (log.isLoggable(Level.FINE)) log.log(Level.FINE, DigitalDisplay.this.recordKey + " maxValue=" + maxValue); //$NON-NLS-1$
								DigitalDisplay.this.maxDigitalLabel.setText(maxValue);
							}
						}
					}
				}
			});
		}
	}

	/**
	 * call redraw method to update values of this display
	 */
	public void redawDisplay() {
		this.actualDigitalLabel.redraw();
		this.minDigitalLabel.redraw();
		this.maxDigitalLabel.redraw();
	}

	/**
	 * enable font size adaption
	 * @param fontSize
	 */
	public void setFontSize(int newFontSize) {
		DigitalDisplay.fontSize = newFontSize;
	}
}
