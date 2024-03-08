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
    							2016,2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.ui.dialog;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import gde.GDE;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

/**
* class to correct the actual record set time stamp using a dialog
*/
public class TimeSetDialog extends Dialog {
	final static Logger				log	= Logger.getLogger(TimeSetDialog.class.getName());

	private DataExplorer			application;
	private Shell							dialogShell;
	private Composite					compositeYear, compositeMonth, compositeDay, compositeHour, compositeMinute, compositeSecond;
	private CCombo						cComboYear, cComboMonth, cComboDay, cComboHour, cComboMinute, cComboSecond;
	private Composite					compositeButton;
	private Button						buttonOK;
	private Composite					compositeDataTime;
	private CLabel						cLabelYear, cLabelMonth, cLabelDay, cLabelHour, cLabelMinute, cLabelSecond;
	private GregorianCalendar	calendar;

	/**
	* Auto-generated main method to display this
	* org.eclipse.swt.widgets.Dialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			TimeSetDialog inst = new TimeSetDialog(shell, SWT.NULL);
			inst.open(new Date().getTime());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public TimeSetDialog(Shell parent, int style) {
		super(parent, style);
	}

	public void open(long millis) {
		try {
			this.application = DataExplorer.getInstance();
			Shell parent = getParent();
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

			FillLayout dialogShellLayout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
			this.dialogShell.setLayout(dialogShellLayout);
			this.dialogShell.setText(Messages.getString(MessageIds.GDE_MSGT0712));
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(GDE.IS_LINUX ? 410 : 340, 115);
			this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/TimeHot.gif")); //$NON-NLS-1$
			this.dialogShell.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent evt) {
					TimeSetDialog.log.log(Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					RecordSet activeRecordSet = TimeSetDialog.this.application.getActiveRecordSet();
					long timeStamp = TimeSetDialog.this.calendar.getTimeInMillis();
					TimeSetDialog.log.log(Level.FINEST, StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss", timeStamp)); //$NON-NLS-1$
					if (activeRecordSet != null) {
						String description = activeRecordSet.getRecordSetDescription();
						description = description.substring(0, description.indexOf(GDE.CHAR_COLON) + 2) + StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss ", timeStamp) //$NON-NLS-1$
								+ description.substring(description.indexOf(GDE.CHAR_BLANK, description.indexOf(GDE.CHAR_COLON) + 12)+1);
						activeRecordSet.setRecordSetDescription(description);
						activeRecordSet.setStartTimeStamp(timeStamp);
						Channel activeChannel = TimeSetDialog.this.application.getActiveChannel();
						if (activeChannel != null) {
							description = activeChannel.getFileDescription();
							if (description.contains(GDE.STRING_MESSAGE_CONCAT))
								description = String.format("%s %s", StringHelper.getFormatedTime("yyyy-MM-dd", activeRecordSet.getStartTimeStamp()), description.substring(11)); //$NON-NLS-1$ //$NON-NLS-2$
							else
								description = String.format("%s", StringHelper.getFormatedTime("yyyy-MM-dd", activeRecordSet.getStartTimeStamp())); //$NON-NLS-1$ //$NON-NLS-2$
							activeChannel.setFileDescription(description);
						}
						TimeSetDialog.this.application.updateAllTabs(true, true);
						TimeSetDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
					}
				}
			});
			{
				this.compositeDataTime = new Composite(this.dialogShell, SWT.NONE);
				RowLayout compositeDataTimeLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.compositeDataTime.setLayout(compositeDataTimeLayout);
				{
					this.compositeYear = new Composite(this.compositeDataTime, SWT.NONE);
					FillLayout compositeYearLayout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
					RowData compositeYearLData = new RowData(GDE.IS_LINUX ? 75 : 60, 40);
					this.compositeYear.setLayoutData(compositeYearLData);
					this.compositeYear.setLayout(compositeYearLayout);
					{
						this.cLabelYear = new CLabel(this.compositeYear, SWT.CENTER | SWT.EMBEDDED);
						this.cLabelYear.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.cLabelYear.setText(Messages.getString(MessageIds.GDE_MSGT0706));
					}
					{
						this.cComboYear = new CCombo(this.compositeYear, SWT.BORDER);
						this.cComboYear.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.cComboYear.setSize(GDE.IS_LINUX ? 75 : 60, 30);
						this.cComboYear.setEditable(false);
						this.cComboYear.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								TimeSetDialog.log.log(Level.FINEST, "cComboYear.widgetSelected, event=" + evt); //$NON-NLS-1$
								TimeSetDialog.this.calendar.set(Calendar.YEAR, Integer.parseInt(TimeSetDialog.this.cComboYear.getText()));
								TimeSetDialog.this.calendar.set(Calendar.MONTH, Integer.parseInt(TimeSetDialog.this.cComboMonth.getText()) - 1);
								TimeSetDialog.this.calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(TimeSetDialog.this.cComboDay.getText()));
								TimeSetDialog.this.calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(TimeSetDialog.this.cComboHour.getText()));
								TimeSetDialog.this.calendar.set(Calendar.MINUTE, Integer.parseInt(TimeSetDialog.this.cComboMinute.getText()));
								TimeSetDialog.this.calendar.set(Calendar.SECOND, Integer.parseInt(TimeSetDialog.this.cComboSecond.getText()));
							}
						});
					}
				}
				{
					this.compositeMonth = new Composite(this.compositeDataTime, SWT.NONE);
					FillLayout compositeMonthLayout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
					RowData compositeMonth1LData = new RowData(GDE.IS_LINUX ? 60 : 50, 40);
					this.compositeMonth.setLayoutData(compositeMonth1LData);
					this.compositeMonth.setLayout(compositeMonthLayout);
					{
						this.cLabelMonth = new CLabel(this.compositeMonth, SWT.CENTER | SWT.EMBEDDED);
						this.cLabelMonth.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.cLabelMonth.setText(Messages.getString(MessageIds.GDE_MSGT0707));
					}
					{
						this.cComboMonth = new CCombo(this.compositeMonth, SWT.BORDER);
						this.cComboMonth.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.cComboMonth.setItems(new String[] { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$
						this.cComboMonth.setSize(GDE.IS_LINUX ? 60 : 50, 30);
						this.cComboMonth.setEditable(false);
						this.cComboMonth.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								TimeSetDialog.log.log(Level.FINEST, "cComboMonth.widgetSelected, event=" + evt); //$NON-NLS-1$
								TimeSetDialog.this.calendar.set(Calendar.YEAR, Integer.parseInt(TimeSetDialog.this.cComboYear.getText()));
								TimeSetDialog.this.calendar.set(Calendar.MONTH, Integer.parseInt(TimeSetDialog.this.cComboMonth.getText()) - 1);
								TimeSetDialog.this.calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(TimeSetDialog.this.cComboDay.getText()));
								TimeSetDialog.this.calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(TimeSetDialog.this.cComboHour.getText()));
								TimeSetDialog.this.calendar.set(Calendar.MINUTE, Integer.parseInt(TimeSetDialog.this.cComboMinute.getText()));
								TimeSetDialog.this.calendar.set(Calendar.SECOND, Integer.parseInt(TimeSetDialog.this.cComboSecond.getText()));
							}
						});
					}
				}
				{
					this.compositeDay = new Composite(this.compositeDataTime, SWT.NONE);
					FillLayout compositeDayLayout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
					RowData compositeDay2LData = new RowData(GDE.IS_LINUX ? 60 : 50, 40);
					this.compositeDay.setLayoutData(compositeDay2LData);
					this.compositeDay.setLayout(compositeDayLayout);
					{
						this.cLabelDay = new CLabel(this.compositeDay, SWT.CENTER | SWT.EMBEDDED);
						this.cLabelDay.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.cLabelDay.setText(Messages.getString(MessageIds.GDE_MSGT0708));
					}
					{
						this.cComboDay = new CCombo(this.compositeDay, SWT.BORDER);
						this.cComboDay.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.cComboDay
								.setItems(new String[] {
										"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$ //$NON-NLS-19$ //$NON-NLS-20$ //$NON-NLS-21$ //$NON-NLS-22$ //$NON-NLS-23$ //$NON-NLS-24$ //$NON-NLS-25$ //$NON-NLS-26$ //$NON-NLS-27$ //$NON-NLS-28$ //$NON-NLS-29$ //$NON-NLS-30$ //$NON-NLS-31$
						this.cComboDay.setSize(GDE.IS_LINUX ? 60 : 50, 30);
						this.cComboDay.setEditable(false);
						this.cComboDay.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								TimeSetDialog.log.log(Level.FINEST, "cComboDay.widgetSelected, event=" + evt); //$NON-NLS-1$
								TimeSetDialog.this.calendar.set(Calendar.YEAR, Integer.parseInt(TimeSetDialog.this.cComboYear.getText()));
								TimeSetDialog.this.calendar.set(Calendar.MONTH, Integer.parseInt(TimeSetDialog.this.cComboMonth.getText()) - 1);
								TimeSetDialog.this.calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(TimeSetDialog.this.cComboDay.getText()));
								TimeSetDialog.this.calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(TimeSetDialog.this.cComboHour.getText()));
								TimeSetDialog.this.calendar.set(Calendar.MINUTE, Integer.parseInt(TimeSetDialog.this.cComboMinute.getText()));
								TimeSetDialog.this.calendar.set(Calendar.SECOND, Integer.parseInt(TimeSetDialog.this.cComboSecond.getText()));
							}
						});
					}
				}
				{
					this.compositeHour = new Composite(this.compositeDataTime, SWT.NONE);
					FillLayout compositeHourLayout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
					RowData compositeHour3LData = new RowData(GDE.IS_LINUX ? 60 : 50, 40);
					this.compositeHour.setLayoutData(compositeHour3LData);
					this.compositeHour.setLayout(compositeHourLayout);
					{
						this.cLabelHour = new CLabel(this.compositeHour, SWT.CENTER | SWT.EMBEDDED);
						this.cLabelHour.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.cLabelHour.setText(Messages.getString(MessageIds.GDE_MSGT0709));
					}
					{
						this.cComboHour = new CCombo(this.compositeHour, SWT.BORDER);
						this.cComboHour.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.cComboHour.setItems(new String[] { "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$ //$NON-NLS-19$ //$NON-NLS-20$ //$NON-NLS-21$ //$NON-NLS-22$ //$NON-NLS-23$ //$NON-NLS-24$
						this.cComboHour.select(14);
						this.cComboHour.setSize(GDE.IS_LINUX ? 60 : 50, 30);
						this.cComboHour.setEditable(false);
						this.cComboHour.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								TimeSetDialog.log.log(Level.FINEST, "cComboHour.widgetSelected, event=" + evt); //$NON-NLS-1$
								TimeSetDialog.this.calendar.set(Calendar.YEAR, Integer.parseInt(TimeSetDialog.this.cComboYear.getText()));
								TimeSetDialog.this.calendar.set(Calendar.MONTH, Integer.parseInt(TimeSetDialog.this.cComboMonth.getText()) - 1);
								TimeSetDialog.this.calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(TimeSetDialog.this.cComboDay.getText()));
								TimeSetDialog.this.calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(TimeSetDialog.this.cComboHour.getText()));
								TimeSetDialog.this.calendar.set(Calendar.MINUTE, Integer.parseInt(TimeSetDialog.this.cComboMinute.getText()));
								TimeSetDialog.this.calendar.set(Calendar.SECOND, Integer.parseInt(TimeSetDialog.this.cComboSecond.getText()));
							}
						});
					}
				}
				Vector<String> tmp60 = new Vector<String>();
				for (int i = 0; i < 60; i++) {
					tmp60.add(String.format("%02d", i)); //$NON-NLS-1$
				}
				{
					this.compositeMinute = new Composite(this.compositeDataTime, SWT.NONE);
					FillLayout compositeMinuteLayout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
					RowData compositeMinute4LData = new RowData(GDE.IS_LINUX ? 60 : 50, 40);
					this.compositeMinute.setLayoutData(compositeMinute4LData);
					this.compositeMinute.setLayout(compositeMinuteLayout);
					{
						this.cLabelMinute = new CLabel(this.compositeMinute, SWT.CENTER | SWT.EMBEDDED);
						this.cLabelMinute.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.cLabelMinute.setText(Messages.getString(MessageIds.GDE_MSGT0710));
					}
					{
						this.cComboMinute = new CCombo(this.compositeMinute, SWT.BORDER);
						this.cComboMinute.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.cComboMinute.setItems(tmp60.toArray(new String[0]));
						this.cComboMinute.select(32);
						this.cComboMinute.setSize(GDE.IS_LINUX ? 60 : 50, 30);
						this.cComboMinute.setEditable(false);
						this.cComboMinute.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								TimeSetDialog.log.log(Level.FINEST, "cComboMinute.widgetSelected, event=" + evt); //$NON-NLS-1$
								TimeSetDialog.this.calendar.set(Calendar.YEAR, Integer.parseInt(TimeSetDialog.this.cComboYear.getText()));
								TimeSetDialog.this.calendar.set(Calendar.MONTH, Integer.parseInt(TimeSetDialog.this.cComboMonth.getText()) - 1);
								TimeSetDialog.this.calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(TimeSetDialog.this.cComboDay.getText()));
								TimeSetDialog.this.calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(TimeSetDialog.this.cComboHour.getText()));
								TimeSetDialog.this.calendar.set(Calendar.MINUTE, Integer.parseInt(TimeSetDialog.this.cComboMinute.getText()));
								TimeSetDialog.this.calendar.set(Calendar.SECOND, Integer.parseInt(TimeSetDialog.this.cComboSecond.getText()));
							}
						});
					}
				}
				{
					this.compositeSecond = new Composite(this.compositeDataTime, SWT.NONE);
					FillLayout compositeSecondLayout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
					RowData compositeSecondLData = new RowData(GDE.IS_LINUX ? 60 : 50, 40);
					this.compositeSecond.setLayoutData(compositeSecondLData);
					this.compositeSecond.setLayout(compositeSecondLayout);
					{
						this.cLabelSecond = new CLabel(this.compositeSecond, SWT.CENTER | SWT.EMBEDDED);
						this.cLabelSecond.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.cLabelSecond.setText(Messages.getString(MessageIds.GDE_MSGT0711));
					}
					{
						this.cComboSecond = new CCombo(this.compositeSecond, SWT.BORDER);
						this.cComboSecond.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.cComboSecond.setItems(tmp60.toArray(new String[0]));
						this.cComboSecond.select(0);
						this.cComboSecond.setSize(GDE.IS_LINUX ? 60 : 50, 30);
						this.cComboSecond.setEditable(false);
						this.cComboSecond.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								TimeSetDialog.log.log(Level.FINEST, "cComboSecond.widgetSelected, event=" + evt); //$NON-NLS-1$
								TimeSetDialog.this.calendar.set(Calendar.YEAR, Integer.parseInt(TimeSetDialog.this.cComboYear.getText()));
								TimeSetDialog.this.calendar.set(Calendar.MONTH, Integer.parseInt(TimeSetDialog.this.cComboMonth.getText()) - 1);
								TimeSetDialog.this.calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(TimeSetDialog.this.cComboDay.getText()));
								TimeSetDialog.this.calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(TimeSetDialog.this.cComboHour.getText()));
								TimeSetDialog.this.calendar.set(Calendar.MINUTE, Integer.parseInt(TimeSetDialog.this.cComboMinute.getText()));
								TimeSetDialog.this.calendar.set(Calendar.SECOND, Integer.parseInt(TimeSetDialog.this.cComboSecond.getText()));
							}
						});
					}
				}

			}
			{
				this.compositeButton = new Composite(this.dialogShell, SWT.NONE);
				GridLayout compositeButtonLayout = new GridLayout();
				compositeButtonLayout.makeColumnsEqualWidth = true;
				this.compositeButton.setLayout(compositeButtonLayout);
				{
					this.buttonOK = new Button(this.compositeButton, SWT.PUSH | SWT.CENTER);
					GridData buttonOKLData = new GridData();
					buttonOKLData.horizontalAlignment = GridData.CENTER;
					buttonOKLData.grabExcessHorizontalSpace = true;
					buttonOKLData.verticalAlignment = GridData.FILL;
					buttonOKLData.widthHint = 48;
					buttonOKLData.grabExcessVerticalSpace = true;
					this.buttonOK.setLayoutData(buttonOKLData);
					this.buttonOK.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.buttonOK.setText("OK"); //$NON-NLS-1$
					this.buttonOK.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							TimeSetDialog.log.log(Level.FINEST, "buttonOK.widgetSelected, event=" + evt); //$NON-NLS-1$
							TimeSetDialog.this.dialogShell.dispose();
						}
					});
				}
			}

			this.calendar = new GregorianCalendar();
			this.calendar.setTimeInMillis(millis);
			Vector<String> tmpYears = new Vector<String>();
			for (int i = 0; i <= this.calendar.get(Calendar.YEAR) - 2000; i++) {
				tmpYears.add(String.format("%s", (i + 2000))); //$NON-NLS-1$
			}
			this.cComboYear.setItems(tmpYears.toArray(new String[0]));
			this.cComboYear.select(this.calendar.get(Calendar.YEAR) - 2000);
			this.cComboMonth.select(this.calendar.get(Calendar.MONTH));
			this.cComboDay.select(this.calendar.get(Calendar.DAY_OF_MONTH) - 1);
			this.cComboHour.select(this.calendar.get(Calendar.HOUR_OF_DAY));
			this.cComboMinute.select(this.calendar.get(Calendar.MINUTE));

			this.dialogShell.setLocation(getParent().toDisplay(100, 100));
			this.dialogShell.open();
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
