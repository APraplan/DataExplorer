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
    
    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.util.logging.Logger;

import gde.GDE;
import gde.device.DataTypes;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.StringHelper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * dialog to enable time stamp modification in case of occurrence of miss adjusted device setup data and time  
 */
public class ChangeDateDialog extends org.eclipse.swt.widgets.Dialog {
	final static Logger				log												= Logger.getLogger(ChangeDateDialog.class.getName());

	Shell dialogShell;
	Composite dateComposite;
	Text questionText, dayText, monthText, minutesText, hourText, yearText, capacityText;
	CLabel dayLabel, monthLabel, minuteLabel, hourLabel, yearLabel, capacityLabel;
	Button yesButton, noButton;
	
	int[] date = new int[0];
	int[] changeDate = new int[0];

	public ChangeDateDialog(Shell parent, int style, int[] oldDate) {
		super(parent, style);
		changeDate = oldDate;
	}

	/**
	 * @return an integer array with year,month,day in case of requested change, else empty array
	 */
	public int[] open() {
		try {
			Shell parent = getParent();
			dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

			dialogShell.setLayout(new FormLayout());
			dialogShell.layout();
			dialogShell.pack();			
			dialogShell.setSize(469, 170);
			dialogShell.setText(Messages.getString(MessageIds.GDE_MSGT2323));
			{
				questionText = new Text(dialogShell, SWT.MULTI | SWT.CENTER | SWT.WRAP);
				FormData questionLabelLData = new FormData();
				questionLabelLData.width = 437;
				questionLabelLData.height = 55;
				questionLabelLData.left =  new FormAttachment(0, 1000, 12);
				questionLabelLData.top =  new FormAttachment(0, 1000, 6);
				questionText.setLayoutData(questionLabelLData);
				questionText.setText(Messages.getString(MessageIds.GDE_MSGT2324));
				questionText.setEditable(false);
			}
			{
				dateComposite = new Composite(dialogShell, SWT.NONE);
				RowLayout dateCompositeLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				dateComposite.setLayout(dateCompositeLayout);
				FormData dateCompositeLData = new FormData();
				dateCompositeLData.width = 437;
				dateCompositeLData.height = 30;
				dateCompositeLData.left =  new FormAttachment(0, 1000, 12);
				dateCompositeLData.top =  new FormAttachment(0, 1000, 62);
				dateComposite.setLayoutData(dateCompositeLData);
				{
					yearLabel = new CLabel(dateComposite, SWT.CENTER | SWT.EMBEDDED);
					RowData yearLabelLData = new RowData();
					yearLabelLData.width = 50;
					yearLabelLData.height = 22;
					yearLabel.setLayoutData(yearLabelLData);
					yearLabel.setText(Messages.getString(MessageIds.GDE_MSGT2329));
				}
				{
					yearText = new Text(dateComposite, SWT.CENTER | SWT.BORDER);
					RowData yearTextLData = new RowData();
					yearTextLData.width = 36;
					yearTextLData.height = 16;
					yearText.setLayoutData(yearTextLData);
					yearText.setText(GDE.STRING_EMPTY+changeDate[2]);
					yearText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST, "yearText.verifyText, event="+evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
				{
					monthLabel = new CLabel(dateComposite, SWT.CENTER | SWT.EMBEDDED);
					RowData monthLabelLData = new RowData();
					monthLabelLData.width = 50;
					monthLabelLData.height = 22;
					monthLabel.setLayoutData(monthLabelLData);
					monthLabel.setText(Messages.getString(MessageIds.GDE_MSGT2330));
				}
				{
					monthText = new Text(dateComposite, SWT.CENTER | SWT.BORDER);
					RowData monthTextLData = new RowData();
					monthTextLData.width = 20;
					monthTextLData.height = 16;
					monthText.setLayoutData(monthTextLData);
					monthText.setText(GDE.STRING_EMPTY+changeDate[3]);
					monthText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST, "monthText.verifyText, event="+evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
				{
					dayLabel = new CLabel(dateComposite, SWT.CENTER | SWT.EMBEDDED);
					RowData DayLabelLData = new RowData();
					DayLabelLData.width = 50;
					DayLabelLData.height = 22;
					dayLabel.setLayoutData(DayLabelLData);
					dayLabel.setText(Messages.getString(MessageIds.GDE_MSGT2331));
				}
				{
					dayText = new Text(dateComposite, SWT.CENTER | SWT.BORDER);
					RowData dayTextLData = new RowData();
					dayTextLData.width = 20;
					dayTextLData.height = 16;
					dayText.setLayoutData(dayTextLData);
					dayText.setText(GDE.STRING_EMPTY+changeDate[4]);
					dayText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST, "dayText.verifyText, event="+evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
				{
					hourLabel = new CLabel(dateComposite, SWT.CENTER | SWT.EMBEDDED);
					RowData hourLabelLData = new RowData();
					hourLabelLData.width = 50;
					hourLabelLData.height = 22;
					hourLabel.setLayoutData(hourLabelLData);
					hourLabel.setText(Messages.getString(MessageIds.GDE_MSGT2332));
				}
				{
					hourText = new Text(dateComposite, SWT.CENTER | SWT.BORDER);
					RowData hourTextLData = new RowData();
					hourTextLData.width = 20;
					hourTextLData.height = 16;
					hourText.setLayoutData(hourTextLData);
					hourText.setText(GDE.STRING_EMPTY+changeDate[0]);
					hourText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST, "hourText.verifyText, event="+evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
				{
					minuteLabel = new CLabel(dateComposite, SWT.CENTER | SWT.EMBEDDED);
					RowData minuteLabelLData = new RowData();
					minuteLabelLData.width = 50;
					minuteLabelLData.height = 22;
					minuteLabel.setLayoutData(minuteLabelLData);
					minuteLabel.setText(Messages.getString(MessageIds.GDE_MSGT2325));
				}
				{
					minutesText = new Text(dateComposite, SWT.CENTER | SWT.BORDER);
					RowData minutesTextLData = new RowData();
					minutesTextLData.width = 20;
					minutesTextLData.height = 16;
					minutesText.setLayoutData(minutesTextLData);
					minutesText.setText(GDE.STRING_EMPTY+changeDate[1]);
					minutesText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST, "minutesText.verifyText, event="+evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
			}
			{
				yesButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				yesButton.setText(Messages.getString(MessageIds.GDE_MSGT2326));
				FormData yesButtonLData = new FormData();
				yesButtonLData.width = 110;
				yesButtonLData.height = 30;
				yesButtonLData.left =  new FormAttachment(0, 1000, 20);
				yesButtonLData.top =  new FormAttachment(0, 1000, 104);
				yesButton.setLayoutData(yesButtonLData);
				yesButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "yesButton.widgetSelected, event="+evt); //$NON-NLS-1$
						date = new int[] { Integer.parseInt(hourText.getText()), Integer.parseInt(minutesText.getText()), Integer.parseInt(yearText.getText()), Integer.parseInt(monthText.getText()), Integer.parseInt(dayText.getText()) };
						dialogShell.close();
					}
				});
			}
			{
				capacityLabel = new CLabel(dialogShell, SWT.CENTER | SWT.EMBEDDED);
				FormData capacityLabelLData = new FormData();
				capacityLabelLData.width = 80;
				capacityLabelLData.height = 22;
				capacityLabelLData.left =  new FormAttachment(0, 1000, 140);
				capacityLabelLData.top =  new FormAttachment(0, 1000, 106);
				capacityLabel.setLayoutData(capacityLabelLData);
				capacityLabel.setText(Messages.getString(MessageIds.GDE_MSGT2327));
			}
			{
				capacityText = new Text(dialogShell, SWT.CENTER | SWT.BORDER);
				FormData capacityTextLData1 = new FormData();
				capacityTextLData1.width = 100;
				capacityTextLData1.height = 16;
				capacityTextLData1.left =  new FormAttachment(0, 1000, 220);
				capacityTextLData1.top =  new FormAttachment(0, 1000, 106);
				capacityText.setLayoutData(capacityTextLData1);
				capacityText.setText(changeDate[5]+GDE.FILE_SEPARATOR+changeDate[6]);
				capacityText.setEditable(false);
			}
			{
				noButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				noButton.setText(Messages.getString(MessageIds.GDE_MSGT2328));
				FormData noButtonLData = new FormData();
				noButtonLData.width = 110;
				noButtonLData.height = 30;
				noButtonLData.right =  new FormAttachment(1000, 1000, -20);
				noButtonLData.top =  new FormAttachment(0, 1000, 101);
				noButton.setLayoutData(noButtonLData);
				noButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "noButton.widgetSelected, event="+evt); //$NON-NLS-1$
						dialogShell.close();
					}
				});
			}
			dialogShell.setLocation(getParent().toDisplay(100, 100));
			dialogShell.open();
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return date;
	}
	
}
