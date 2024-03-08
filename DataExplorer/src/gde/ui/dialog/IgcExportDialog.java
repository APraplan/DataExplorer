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
package gde.ui.dialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
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
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.config.Settings;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.IDevice;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.GPSHelper;
import gde.utils.StringHelper;

public class IgcExportDialog extends Dialog {
	final static Logger					log													= Logger.getLogger(IgcExportDialog.class.getName());

	public final static String	IGC_PILOT_NAME							= "IGC_PILOT_NAME";																																																																																																																																																		//$NON-NLS-1$
	public final static String	IGC_CO_PILOT_NAME						= "IGC_CO_PILOT_NAME";																																																																																																																																																	//$NON-NLS-1$
	public final static String	IGC_GLIDER_CLASS						= "IGC_GLIDER_CLASS";																																																																																																																																																	//$NON-NLS-1$
	public final static String	IGC_GLIDER_ID								= "IGC_GLIDER_ID";																																																																																																																																																			//$NON-NLS-1$
	public final static String	IGC_GPS_FIRMWARE_VERSION		= "IGC_GPS_FIRMWARE_VERSION";																																																																																																																																													//$NON-NLS-1$
	public final static String	IGC_GPS_HARDWARE_VERSION		= "IGC_GPS_HARDWARE_VERSION";																																																																																																																																													//$NON-NLS-1$
	public final static String	IGC_GPS_TYPE_IDENTIFIER			= "IGC_GPS_TYPE_IDENTIFIER";																																																																																																																																														//$NON-NLS-1$
	public final static String	IGC_COMPETITION_IDENTIFIER	= "IGC_COMPETITION_IDENTIFIER";																																																																																																																																												//$NON-NLS-1$
	public final static String	IGC_COMPETITION_CLASS				= "IGC_COMPETITION_CLASS";																																																																																																																																															//$NON-NLS-1$
	public final static String	IGC_UTC_OFFSET							= "IGC_UTC_OFFSET";																																																																																																																																																		//$NON-NLS-1$

	Shell												dialogShell;
	Group												igcHeaderInfoGroup, extraEntriesGroup, startAltitudeGroup, startTimeGroup;
	CLabel											separatorLabel, headerARecordLabel, headerRecordDateLabel, headerFixAccuracyLabel, headerPilotLabel, headerCoPilotLabel, headerGliderTypeLabel, headerGliderIdLabel, headerGpsDatumLabel,
															headerFirmwareVersionLabel, headerHardwareVersionLabel, headerGpsManufacturerModelLabel, headerCompetitionIdLabel, headerCompetitionClassLabel, headerUtcOffsetLabel;
	Text												headerARecordText, headerRecordDateText, headerFixAccuracyText, headerPilotText, headerCoPilotText, headerGliderIdText, headerGpsDatumText, headerFirmwareVersionText,
															headerHardwareVersionText, headerGpsManufacturerModelText, headerCompetitionIdText, headerCompetitionClassText;
	CCombo											headerGliderTypeText, headerUtcOffsetCombo;
	String											headerARecord, headerRecordDate, headerFixAccuracy, headerPilot, headerCoPilot, headerGliderId, headerGpsDatum, headerFirmwareVersion, headerHardwareVersion,
															headerGpsManufacturerModel, headerCompetitionId, headerCompetitionClass;
	int													headerGliderType, headerUtcOffset;

	CLabel											startAltitudeLabel, startAltitudeUnitLabel, startTimeLabel, startTimeUnitLabel, latitudeLabel, longitudeLabel;
	Text												startAltitudeDescrptionText, startAltitudeText, startTimeDescriptionText, startTimeText, latitudeText, longitudeText;
	String											startAltitude, startTime, latitude, longitude;

	Button											cancelButton, helpButton, saveButton;

	final DataExplorer					application									= DataExplorer.getInstance();
	final Settings							settings										= Settings.getInstance();
	final String[]							gliderTypes									= {
			"0 - 1500 mm", "1501 - 2000 mm", "2001-2500 mm", "2501-3000 mm", "3001-3500 mm", "3501-4000 mm", "4001-4500 mm", "4501-5000 mm", "5001-5500 mm", "5501-6000 mm", "6001-6500 mm", "6501-7000 mm", "7001-7500 mm", "7501-8000 mm", "8001-8500 mm", "8501-9000 mm", "9001-9500 mm", "9501-10000 mm", "10001-10500 mm", "10501-11000mm" };	//$NON-NLS-1$
	final String[]							deltaUTC										= { " -12", " -11", " -10", " -9", " -8", " -7", " -6", " -5", " -4", " -3", " -2", " -1", " 0", " +1", " +2", " +3", " +4", " +5", " +6",
			" +7", " +8", " +9", " +10", " +11", " +12"				};																																																																																																																																																											//$NON-NLS-4$

	/**
	* Auto-generated main method to display this
	* org.eclipse.swt.widgets.FileDialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			IgcExportDialog inst = new IgcExportDialog(shell, SWT.PRIMARY_MODAL);
			inst.open(3, 4, 5);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public IgcExportDialog(Shell parent, int style) {
		super(parent, style);
	}

	public IgcExportDialog() {
		super(GDE.shell, SWT.PRIMARY_MODAL);
	}

	public void open(final int ordinalLongitude, final int ordinalLatitude, final int ordinalHeight) {
		try {
			initializeValues(ordinalLongitude, ordinalLatitude, ordinalHeight);

			Shell parent = getParent();
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

			this.dialogShell.setLayout(new FormLayout());
			this.dialogShell.setText(Messages.getString(MessageIds.GDE_MSGT0615));
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(800, 480);
			this.dialogShell.addListener(SWT.Traverse, new Listener() {
	      @Override
				public void handleEvent(Event event) {
	        switch (event.detail) {
	        case SWT.TRAVERSE_ESCAPE:
	        	IgcExportDialog.this.dialogShell.close();
	          event.detail = SWT.TRAVERSE_NONE;
	          event.doit = false;
	          break;
	        }
	      }
	    });
			this.dialogShell.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
					IgcExportDialog.this.application.openHelpDialog("", "HelpInfo_37.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			{
				this.igcHeaderInfoGroup = new Group(this.dialogShell, SWT.NONE);
				RowLayout personalInfoGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.igcHeaderInfoGroup.setLayout(personalInfoGroupLayout);
				FormData personalInfoGroupLData = new FormData();
				personalInfoGroupLData.width = 500;
				personalInfoGroupLData.height = 364;
				personalInfoGroupLData.left = new FormAttachment(0, 1000, 6);
				personalInfoGroupLData.top = new FormAttachment(0, 1000, 7);
				this.igcHeaderInfoGroup.setLayoutData(personalInfoGroupLData);
				this.igcHeaderInfoGroup.setText(Messages.getString(MessageIds.GDE_MSGT0615));
				this.igcHeaderInfoGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE+2, SWT.NORMAL));
				{
					this.separatorLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerARecordLabelLData = new RowData();
					headerARecordLabelLData.width = 400;
					headerARecordLabelLData.height = 5;
					this.separatorLabel.setLayoutData(headerARecordLabelLData);
				}
				{
					this.headerARecordLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerARecordLabelLData = new RowData();
					headerARecordLabelLData.width = 236;
					headerARecordLabelLData.height = 22;
					this.headerARecordLabel.setLayoutData(headerARecordLabelLData);
					this.headerARecordLabel.setText(Messages.getString(MessageIds.GDE_MSGT0616));
					this.headerARecordLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0617));
					this.headerARecordLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.headerARecordText = new Text(this.igcHeaderInfoGroup, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
					RowData headerARecordTextLData = new RowData();
					headerARecordTextLData.width = 236;
					headerARecordTextLData.height = GDE.IS_LINUX ? 14 : 16;
					this.headerARecordText.setLayoutData(headerARecordTextLData);
					this.headerARecordText.setText(this.headerARecord);
					this.headerARecordText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerARecordText.setBackground(this.application.COLOR_LIGHT_GREY);
				}
				{
					this.headerRecordDateLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerRecordDateLabelLData = new RowData();
					headerRecordDateLabelLData.width = 236;
					headerRecordDateLabelLData.height = 22;
					this.headerRecordDateLabel.setLayoutData(headerRecordDateLabelLData);
					this.headerRecordDateLabel.setText(Messages.getString(MessageIds.GDE_MSGT0618));
					this.headerRecordDateLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0619));
					this.headerRecordDateLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.headerRecordDateText = new Text(this.igcHeaderInfoGroup, SWT.BORDER);
					RowData headerRecordDateTextLData = new RowData();
					headerRecordDateTextLData.width = 236;
					headerRecordDateTextLData.height = GDE.IS_LINUX ? 14 : 16;
					this.headerRecordDateText.setLayoutData(headerRecordDateTextLData);
					this.headerRecordDateText.setText(this.headerRecordDate);
					this.headerRecordDateText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerRecordDateText.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent evt) {
							IgcExportDialog.log.log(java.util.logging.Level.FINEST, "headerRecordDateText.verifyText, event=" + evt); //$NON-NLS-1$
							StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
				{
					this.headerFixAccuracyLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerFixAccuracyLabelLData = new RowData();
					headerFixAccuracyLabelLData.width = 236;
					headerFixAccuracyLabelLData.height = 22;
					this.headerFixAccuracyLabel.setLayoutData(headerFixAccuracyLabelLData);
					this.headerFixAccuracyLabel.setText(Messages.getString(MessageIds.GDE_MSGT0620));
					this.headerFixAccuracyLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerFixAccuracyLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0621));
				}
				{
					this.headerFixAccuracyText = new Text(this.igcHeaderInfoGroup, SWT.READ_ONLY | SWT.BORDER);
					RowData headerFixAccuracyTextLData = new RowData();
					headerFixAccuracyTextLData.width = 236;
					headerFixAccuracyTextLData.height = GDE.IS_LINUX ? 14 : 16;
					this.headerFixAccuracyText.setLayoutData(headerFixAccuracyTextLData);
					this.headerFixAccuracyText.setText("035"); //$NON-NLS-1$
					this.headerFixAccuracyText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerFixAccuracyText.setBackground(this.application.COLOR_LIGHT_GREY);
					this.headerFixAccuracyText.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent evt) {
							IgcExportDialog.log.log(java.util.logging.Level.FINEST, "headerFixAccuracyText.verifyText, event=" + evt); //$NON-NLS-1$
							StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
				{
					this.headerPilotLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerPilotLabelLData = new RowData();
					headerPilotLabelLData.width = 236;
					headerPilotLabelLData.height = 22;
					this.headerPilotLabel.setLayoutData(headerPilotLabelLData);
					this.headerPilotLabel.setText(Messages.getString(MessageIds.GDE_MSGT0622));
					this.headerPilotLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerPilotLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0623));
				}
				{
					this.headerPilotText = new Text(this.igcHeaderInfoGroup, SWT.BORDER);
					RowData headerPilotTextLData = new RowData();
					headerPilotTextLData.width = 236;
					headerPilotTextLData.height = GDE.IS_LINUX ? 14 : 16;
					this.headerPilotText.setLayoutData(headerPilotTextLData);
					this.headerPilotText.setText(this.headerPilot);
					this.headerPilotText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.headerCoPilotLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerCoPilotLabelLData = new RowData();
					headerCoPilotLabelLData.width = 236;
					headerCoPilotLabelLData.height = 22;
					this.headerCoPilotLabel.setLayoutData(headerCoPilotLabelLData);
					this.headerCoPilotLabel.setText(Messages.getString(MessageIds.GDE_MSGT0624));
					this.headerCoPilotLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerCoPilotLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0626));
				}
				{
					RowData headerCoPilotTextLData = new RowData();
					headerCoPilotTextLData.width = 236;
					headerCoPilotTextLData.height = GDE.IS_LINUX ? 14 : 16;
					this.headerCoPilotText = new Text(this.igcHeaderInfoGroup, SWT.BORDER);
					this.headerCoPilotText.setLayoutData(headerCoPilotTextLData);
				}
				{
					this.headerGliderTypeLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerGliderTypeLabelLData = new RowData();
					headerGliderTypeLabelLData.width = 236;
					headerGliderTypeLabelLData.height = 22;
					this.headerGliderTypeLabel.setLayoutData(headerGliderTypeLabelLData);
					this.headerGliderTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT0627));
					this.headerGliderTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerGliderTypeLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0628));
				}
				{
					this.headerGliderTypeText = new CCombo(this.igcHeaderInfoGroup, SWT.BORDER);
					RowData headerGliderTypeTextLData = new RowData();
					headerGliderTypeTextLData.width = 236;
					headerGliderTypeTextLData.height = 17;
					this.headerGliderTypeText.setLayoutData(headerGliderTypeTextLData);
					this.headerGliderTypeText.setItems(this.gliderTypes);
					this.headerGliderTypeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerGliderTypeText.select(this.headerGliderType);
				}
				{
					this.headerGliderIdLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerGliderIdLabelLData = new RowData();
					headerGliderIdLabelLData.width = 236;
					headerGliderIdLabelLData.height = 22;
					this.headerGliderIdLabel.setLayoutData(headerGliderIdLabelLData);
					this.headerGliderIdLabel.setText(Messages.getString(MessageIds.GDE_MSGT0629));
					this.headerGliderIdLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerGliderIdLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0630));
				}
				{
					this.headerGliderIdText = new Text(this.igcHeaderInfoGroup, SWT.BORDER);
					RowData headerGliderIdTextLData = new RowData();
					headerGliderIdTextLData.width = 236;
					headerGliderIdTextLData.height = GDE.IS_LINUX ? 14 : 16;
					this.headerGliderIdText.setLayoutData(headerGliderIdTextLData);
					this.headerGliderIdText.setText(this.headerGliderId);
					this.headerGliderIdText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.headerGpsDatumLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerGpsDatumLabelLData = new RowData();
					headerGpsDatumLabelLData.width = 236;
					headerGpsDatumLabelLData.height = 22;
					this.headerGpsDatumLabel.setLayoutData(headerGpsDatumLabelLData);
					this.headerGpsDatumLabel.setText(Messages.getString(MessageIds.GDE_MSGT0631));
					this.headerGpsDatumLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerGpsDatumLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0632));
				}
				{
					this.headerGpsDatumText = new Text(this.igcHeaderInfoGroup, SWT.READ_ONLY | SWT.BORDER);
					RowData headerGpsDatumTextLData = new RowData();
					headerGpsDatumTextLData.width = 236;
					headerGpsDatumTextLData.height = GDE.IS_LINUX ? 14 : 16;
					this.headerGpsDatumText.setLayoutData(headerGpsDatumTextLData);
					this.headerGpsDatumText.setText(this.headerGpsDatum);
					this.headerGpsDatumText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerGpsDatumText.setBackground(this.application.COLOR_LIGHT_GREY);
				}
				{
					this.headerFirmwareVersionLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerFirmwareVersionLabelLData = new RowData();
					headerFirmwareVersionLabelLData.width = 236;
					headerFirmwareVersionLabelLData.height = 22;
					this.headerFirmwareVersionLabel.setLayoutData(headerFirmwareVersionLabelLData);
					this.headerFirmwareVersionLabel.setText(Messages.getString(MessageIds.GDE_MSGT0633));
					this.headerFirmwareVersionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerFirmwareVersionLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0634));
				}
				{
					this.headerFirmwareVersionText = new Text(this.igcHeaderInfoGroup, SWT.BORDER);
					RowData textheaderFirmwareVersionTextLData = new RowData();
					textheaderFirmwareVersionTextLData.width = 236;
					textheaderFirmwareVersionTextLData.height = GDE.IS_LINUX ? 14 : 16;
					this.headerFirmwareVersionText.setLayoutData(textheaderFirmwareVersionTextLData);
					this.headerFirmwareVersionText.setText(this.headerFirmwareVersion);
					this.headerFirmwareVersionText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.headerHardwareVersionLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerHardwareVersionLabelLData = new RowData();
					headerHardwareVersionLabelLData.width = 236;
					headerHardwareVersionLabelLData.height = 22;
					this.headerHardwareVersionLabel.setLayoutData(headerHardwareVersionLabelLData);
					this.headerHardwareVersionLabel.setText(Messages.getString(MessageIds.GDE_MSGT0635));
					this.headerHardwareVersionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerHardwareVersionLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0636));
				}
				{
					this.headerHardwareVersionText = new Text(this.igcHeaderInfoGroup, SWT.BORDER);
					RowData headerHardwareVersionTextLData = new RowData();
					headerHardwareVersionTextLData.width = 236;
					headerHardwareVersionTextLData.height = GDE.IS_LINUX ? 14 : 16;
					this.headerHardwareVersionText.setLayoutData(headerHardwareVersionTextLData);
					this.headerHardwareVersionText.setText(this.headerHardwareVersion);
					this.headerHardwareVersionText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.headerGpsManufacturerModelLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerGpsManufacturerModelLabelLData = new RowData();
					headerGpsManufacturerModelLabelLData.width = 236;
					headerGpsManufacturerModelLabelLData.height = 22;
					this.headerGpsManufacturerModelLabel.setLayoutData(headerGpsManufacturerModelLabelLData);
					this.headerGpsManufacturerModelLabel.setText(Messages.getString(MessageIds.GDE_MSGT0637));
					this.headerGpsManufacturerModelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerGpsManufacturerModelLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0638));
				}
				{
					this.headerGpsManufacturerModelText = new Text(this.igcHeaderInfoGroup, SWT.BORDER);
					RowData headerGpsManufacturerModelTextLData = new RowData();
					headerGpsManufacturerModelTextLData.width = 236;
					headerGpsManufacturerModelTextLData.height = GDE.IS_LINUX ? 14 : 16;
					this.headerGpsManufacturerModelText.setLayoutData(headerGpsManufacturerModelTextLData);
					this.headerGpsManufacturerModelText.setText(this.headerGpsManufacturerModel);
					this.headerGpsManufacturerModelText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.headerCompetitionIdLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerCompetitionIdLabelLData = new RowData();
					headerCompetitionIdLabelLData.width = 236;
					headerCompetitionIdLabelLData.height = 22;
					this.headerCompetitionIdLabel.setLayoutData(headerCompetitionIdLabelLData);
					this.headerCompetitionIdLabel.setText(Messages.getString(MessageIds.GDE_MSGT0639));
					this.headerCompetitionIdLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerCompetitionIdLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0640));
				}
				{
					this.headerCompetitionIdText = new Text(this.igcHeaderInfoGroup, SWT.BORDER);
					RowData headerCompetitionIdTextLData = new RowData();
					headerCompetitionIdTextLData.width = 236;
					headerCompetitionIdTextLData.height = GDE.IS_LINUX ? 14 : 16;
					this.headerCompetitionIdText.setLayoutData(headerCompetitionIdTextLData);
					this.headerCompetitionIdText.setText(this.headerCompetitionId);
					this.headerCompetitionIdText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.headerCompetitionClassLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerCompetitionClassLabelLData = new RowData();
					headerCompetitionClassLabelLData.width = 236;
					headerCompetitionClassLabelLData.height = 22;
					this.headerCompetitionClassLabel.setLayoutData(headerCompetitionClassLabelLData);
					this.headerCompetitionClassLabel.setText(Messages.getString(MessageIds.GDE_MSGT0641));
					this.headerCompetitionClassLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerCompetitionClassLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0642));
				}
				{
					this.headerCompetitionClassText = new Text(this.igcHeaderInfoGroup, SWT.BORDER);
					RowData headerCompetitionClassTextLData = new RowData();
					headerCompetitionClassTextLData.width = 236;
					headerCompetitionClassTextLData.height = GDE.IS_LINUX ? 14 : 16;
					this.headerCompetitionClassText.setLayoutData(headerCompetitionClassTextLData);
					this.headerCompetitionClassText.setText(this.headerCompetitionClass);
					this.headerCompetitionClassText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.headerUtcOffsetLabel = new CLabel(this.igcHeaderInfoGroup, SWT.NONE);
					RowData headerUtcOffsetLabelLData = new RowData();
					headerUtcOffsetLabelLData.width = 236;
					headerUtcOffsetLabelLData.height = 22;
					this.headerUtcOffsetLabel.setLayoutData(headerUtcOffsetLabelLData);
					this.headerUtcOffsetLabel.setText(Messages.getString(MessageIds.GDE_MSGT0643));
					this.headerUtcOffsetLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerUtcOffsetLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0644));
				}
				{
					RowData headerUtcOffsetComboLData = new RowData();
					headerUtcOffsetComboLData.width = 61;
					headerUtcOffsetComboLData.height = 17;
					this.headerUtcOffsetCombo = new CCombo(this.igcHeaderInfoGroup, SWT.BORDER);
					this.headerUtcOffsetCombo.setLayoutData(headerUtcOffsetComboLData);
					this.headerUtcOffsetCombo.setItems(this.deltaUTC);
					this.headerUtcOffsetCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.headerUtcOffsetCombo.select(this.headerUtcOffset);
				}
			}
			{
				this.extraEntriesGroup = new Group(this.dialogShell, SWT.NONE);
				RowLayout extraEntriesGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.extraEntriesGroup.setLayout(extraEntriesGroupLayout);
				FormData extraEntriesGroupLData = new FormData();
				extraEntriesGroupLData.width = 261;
				extraEntriesGroupLData.height = 364;
				extraEntriesGroupLData.top = new FormAttachment(0, 1000, 7);
				extraEntriesGroupLData.right = new FormAttachment(1000, 1000, -7);
				this.extraEntriesGroup.setLayoutData(extraEntriesGroupLData);
				this.extraEntriesGroup.setText(Messages.getString(MessageIds.GDE_MSGT0645));
				this.extraEntriesGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE+2, SWT.NORMAL));
				{
					this.separatorLabel = new CLabel(this.extraEntriesGroup, SWT.NONE);
					RowData headerARecordLabelLData = new RowData();
					headerARecordLabelLData.width = 250;
					headerARecordLabelLData.height = 5;
					this.separatorLabel.setLayoutData(headerARecordLabelLData);
				}
				{
					this.startAltitudeGroup = new Group(this.extraEntriesGroup, SWT.NONE);
					RowLayout startAltitudeGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					this.startAltitudeGroup.setLayout(startAltitudeGroupLayout);
					RowData startAltitudeGroupLData = new RowData();
					startAltitudeGroupLData.width = 247;
					startAltitudeGroupLData.height = 166;
					this.startAltitudeGroup.setLayoutData(startAltitudeGroupLData);
					this.startAltitudeGroup.setText(Messages.getString(MessageIds.GDE_MSGT0646));
					this.startAltitudeGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE+2, SWT.NORMAL));
					{
						this.separatorLabel = new CLabel(this.startAltitudeGroup, SWT.NONE);
						RowData headerARecordLabelLData = new RowData();
						headerARecordLabelLData.width = 240;
						headerARecordLabelLData.height = 5;
						this.separatorLabel.setLayoutData(headerARecordLabelLData);
					}
					{
						this.startAltitudeDescrptionText = new Text(this.startAltitudeGroup, SWT.WRAP | SWT.READ_ONLY);
						RowData startAltitudeLabelLData = new RowData();
						startAltitudeLabelLData.width = 241;
						startAltitudeLabelLData.height = 104;
						this.startAltitudeDescrptionText.setLayoutData(startAltitudeLabelLData);
						this.startAltitudeDescrptionText.setText(Messages.getString(MessageIds.GDE_MSGT0647));
						this.startAltitudeDescrptionText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.startAltitudeDescrptionText.setBackground(this.application.COLOR_LIGHT_GREY);
					}
					{
						this.latitudeLabel = new CLabel(this.startAltitudeGroup, SWT.NONE);
						this.latitudeLabel.setText("lat :");
						this.latitudeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					}
					{
						this.latitudeText = new Text(this.startAltitudeGroup, SWT.READ_ONLY | SWT.BORDER);
						this.latitudeText.setText(this.latitude);
						this.latitudeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.latitudeText.setBackground(this.application.COLOR_LIGHT_GREY);
					}
					{
						this.longitudeLabel = new CLabel(this.startAltitudeGroup, SWT.NONE);
						this.longitudeLabel.setText("long :");
						this.longitudeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					}
					{
						this.longitudeText = new Text(this.startAltitudeGroup, SWT.READ_ONLY | SWT.BORDER);
						this.longitudeText.setText(this.longitude);
						this.longitudeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.longitudeText.setBackground(this.application.COLOR_LIGHT_GREY);
					}
					{
						this.startAltitudeLabel = new CLabel(this.startAltitudeGroup, SWT.NONE);
						RowData startAltitudeLabelLData = new RowData();
						startAltitudeLabelLData.width = 95;
						startAltitudeLabelLData.height = 22;
						this.startAltitudeLabel.setLayoutData(startAltitudeLabelLData);
						this.startAltitudeLabel.setText(Messages.getString(MessageIds.GDE_MSGT0648));
						this.startAltitudeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.startAltitudeLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0649));
					}
					{
						this.startAltitudeText = new Text(this.startAltitudeGroup, SWT.BORDER);
						RowData startAltitudeTextLData = new RowData();
						startAltitudeTextLData.width = 48;
						startAltitudeTextLData.height = GDE.IS_LINUX ? 14 : 16;
						this.startAltitudeText.setLayoutData(startAltitudeTextLData);
						this.startAltitudeText.setText(this.startAltitude);
						this.startAltitudeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.startAltitudeText.addVerifyListener(new VerifyListener() {
							@Override
							public void verifyText(VerifyEvent evt) {
								IgcExportDialog.log.log(java.util.logging.Level.FINEST, "startAltitudeText.verifyText, event=" + evt); //$NON-NLS-1$
								StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
							}
						});
					}
					{
						this.startAltitudeUnitLabel = new CLabel(this.startAltitudeGroup, SWT.NONE);
						this.startAltitudeUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT0613));
						this.startAltitudeUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					}
				}
				{
					this.startTimeGroup = new Group(this.extraEntriesGroup, SWT.NONE);
					RowLayout startTimeGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					this.startTimeGroup.setLayout(startTimeGroupLayout);
					RowData startTimeGroupLData = new RowData();
					startTimeGroupLData.width = 250;
					startTimeGroupLData.height = 140;
					this.startTimeGroup.setLayoutData(startTimeGroupLData);
					this.startTimeGroup.setText(Messages.getString(MessageIds.GDE_MSGT0650));
					this.startTimeGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE+2, SWT.NORMAL));
					{
						this.separatorLabel = new CLabel(this.startTimeGroup, SWT.NONE);
						RowData headerARecordLabelLData = new RowData();
						headerARecordLabelLData.width = 240;
						headerARecordLabelLData.height = 5;
						this.separatorLabel.setLayoutData(headerARecordLabelLData);
					}
					{
						this.startTimeDescriptionText = new Text(this.startTimeGroup, SWT.READ_ONLY | SWT.WRAP);
						RowData startTimeDescriptionTextLData = new RowData();
						startTimeDescriptionTextLData.width = 227;
						startTimeDescriptionTextLData.height = 105;
						this.startTimeDescriptionText.setLayoutData(startTimeDescriptionTextLData);
						this.startTimeDescriptionText.setText(Messages.getString(MessageIds.GDE_MSGT0651));
						this.startTimeDescriptionText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.startTimeDescriptionText.setBackground(this.application.COLOR_LIGHT_GREY);
					}
					{
						this.startTimeLabel = new CLabel(this.startTimeGroup, SWT.NONE);
						RowData startTimeLabelLData = new RowData();
						startTimeLabelLData.width = 95;
						startTimeLabelLData.height = 22;
						this.startTimeLabel.setLayoutData(startTimeLabelLData);
						this.startTimeLabel.setText(Messages.getString(MessageIds.GDE_MSGT0652));
						this.startTimeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.startTimeLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0653));
					}
					{
						this.startTimeText = new Text(this.startTimeGroup, SWT.BORDER);
						this.startTimeText.setText(this.startTime);
						this.startTimeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					}
					{
						this.startTimeUnitLabel = new CLabel(this.startTimeGroup, SWT.NONE);
						this.startTimeUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT0612));
						this.startTimeUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					}
				}
			}
			{
				this.cancelButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.cancelButton.setText(Messages.getString(MessageIds.GDE_MSGT0452));
				this.cancelButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				FormData cancelButtonLData = new FormData();
				cancelButtonLData.width = 172;
				cancelButtonLData.height = 34;
				cancelButtonLData.left = new FormAttachment(0, 1000, 173);
				cancelButtonLData.top = new FormAttachment(0, 1000, 407);
				this.cancelButton.setLayoutData(cancelButtonLData);
				this.cancelButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						IgcExportDialog.log.log(java.util.logging.Level.FINEST, "cancelButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						IgcExportDialog.this.dialogShell.close();
					}
				});
				}
				{
					this.helpButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					this.helpButton.setImage(SWTResourceManager.getImage("gde/resource/QuestionHot.gif")); //$NON-NLS-1$
					FormData helpButtonLData = new FormData();
					helpButtonLData.width = 60;
					helpButtonLData.height = 34;
					helpButtonLData.left = new FormAttachment(0, 1000, 370);
					helpButtonLData.top = new FormAttachment(0, 1000, 407);
					this.helpButton.setLayoutData(helpButtonLData);
					this.helpButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							IgcExportDialog.log.log(java.util.logging.Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							IgcExportDialog.this.application.openHelpDialog("", "HelpInfo_37.html"); //$NON-NLS-1$ //$NON-NLS-2$
						}
					});
				{
					this.saveButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData saveButtonLData = new FormData();
					saveButtonLData.width = 172;
					saveButtonLData.height = 34;
					saveButtonLData.left = new FormAttachment(0, 1000, 449);
					saveButtonLData.top = new FormAttachment(0, 1000, 407);
					this.saveButton.setLayoutData(saveButtonLData);
					this.saveButton.setText(Messages.getString(MessageIds.GDE_MSGT0486));
					this.saveButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.saveButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							IgcExportDialog.log.log(java.util.logging.Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							RecordSet recordSet = IgcExportDialog.this.application.getActiveRecordSet();
							IDevice device = IgcExportDialog.this.application.getActiveDevice();
							if (recordSet != null && device != null) {
//								new FileHandler().exportFileIGC(Messages.getString(MessageIds.GDE_MSGT0654), device, IgcExportDialog.this.getHeader(), ordinalLongitude, ordinalLatitude, ordinalHeight,
//										Integer.parseInt(IgcExportDialog.this.startAltitudeText.getText()));
							}
							IgcExportDialog.this.dialogShell.close();
						}
					});
				}
			}

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

	/**
	 * initialize the values to be displayed
	 */
	public void initializeValues(final int ordinalLongitude, final int ordinalLatitude, final int ordinalHeight) {
		IDevice device = this.application.getActiveDevice();
		RecordSet recordSet = this.application.getActiveRecordSet();

		this.headerARecord = String.format("%s %s", GDE.NAME_LONG, GDE.VERSION); //$NON-NLS-1$
		if (recordSet != null) {
			this.headerRecordDate = String.format("%s", new SimpleDateFormat("ddMMyy").format(recordSet.getStartTimeStamp())); //$NON-NLS-1$ //$NON-NLS-2$

			Record recordLatitude = recordSet.get(ordinalLatitude);
			Record recordLongitude = recordSet.get(ordinalLongitude);
			int startIndex = GPSHelper.getStartIndexGPS(recordSet, ordinalLatitude, ordinalLongitude);
			String latitudeNS = recordLatitude.get(startIndex) > 0 ? "N" : "S"; //$NON-NLS-1$ //$NON-NLS-2$
			String longitudeEW = recordLongitude.get(startIndex) > 0 ? "E" : "W"; //$NON-NLS-1$ //$NON-NLS-2$
			this.latitude = String.format("%08.6f %s", device.translateValue(recordLatitude, recordLatitude.get(startIndex) / 1000.0), latitudeNS);
			this.longitude = String.format("%09.6f %s", device.translateValue(recordLongitude, recordLongitude.get(startIndex) / 1000.0), longitudeEW);

			this.startAltitude = String.format("%.0f", device.translateValue(recordSet.get(ordinalHeight), recordSet.get(ordinalHeight).get(startIndex) / 1000.0)); //$NON-NLS-1$
			this.startTime = new SimpleDateFormat("HH:mm:ss").format(recordSet.getStartTimeStamp()); //$NON-NLS-1$
		}
		else {
			this.headerRecordDate = String.format("%s", new SimpleDateFormat("ddMMyy").format(new Date().getTime())); //$NON-NLS-1$ //$NON-NLS-2$
			this.latitude = "53.147619 N"; //$NON-NLS-1$
			this.longitude = "008.443705 E"; //$NON-NLS-1$
			this.startAltitude = "0"; //$NON-NLS-1$
			this.startTime = new SimpleDateFormat("HH:mm:ss").format(new Date().getTime()); //$NON-NLS-1$
		}

		//constant values
		this.headerFixAccuracy = "035"; //$NON-NLS-1$
		this.headerGpsDatum = "WGS-1984"; //$NON-NLS-1$

		if (this.application.isObjectoriented()) {
			String objectText = this.application.getObject().getStyledText();

			this.headerPilot = Messages.getString(MessageIds.GDE_MSGT0655);
			if (objectText.split(IgcExportDialog.IGC_PILOT_NAME).length >= 2) {
				this.headerPilot = objectText.split(IgcExportDialog.IGC_PILOT_NAME)[1].split(":|\\r|\\n")[1].trim(); //$NON-NLS-1$
			}
			this.headerCoPilot = ""; //$NON-NLS-1$
			if (objectText.split(IgcExportDialog.IGC_CO_PILOT_NAME).length >= 2) {
				this.headerCoPilot = objectText.split(IgcExportDialog.IGC_CO_PILOT_NAME)[1].split(":|\\r|\\n")[1].trim(); //$NON-NLS-1$
			}
			this.headerGliderType = 4;
			if (objectText.split(IgcExportDialog.IGC_GLIDER_CLASS).length >= 2) {
				String tmpGliderType = objectText.split(IgcExportDialog.IGC_GLIDER_CLASS)[1].split(":|\\r|\\n")[1].trim().split("-")[0].trim(); //$NON-NLS-1$ //$NON-NLS-2$
				if (tmpGliderType.startsWith("0"))this.headerGliderType = 0; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("15"))this.headerGliderType = 1; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("20"))this.headerGliderType = 2; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("25"))this.headerGliderType = 3; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("30"))this.headerGliderType = 4; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("35"))this.headerGliderType = 5; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("40"))this.headerGliderType = 6; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("45"))this.headerGliderType = 7; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("50"))this.headerGliderType = 8; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("55"))this.headerGliderType = 9; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("60"))this.headerGliderType = 10; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("65"))this.headerGliderType = 11; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("70"))this.headerGliderType = 12; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("75"))this.headerGliderType = 13; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("80"))this.headerGliderType = 14; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("85"))this.headerGliderType = 15; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("90"))this.headerGliderType = 16; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("95"))this.headerGliderType = 17; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("100"))this.headerGliderType = 18; //$NON-NLS-1$
				else if (tmpGliderType.startsWith("105")) this.headerGliderType = 19; //$NON-NLS-1$
			}
			this.headerGliderId = this.application.getObjectKey();
			if (objectText.split(IgcExportDialog.IGC_GLIDER_ID).length >= 2) {
				this.headerGliderId = objectText.split(IgcExportDialog.IGC_GLIDER_ID)[1].split(":|\\r|\\n")[1].trim(); //$NON-NLS-1$
			}
			this.headerFirmwareVersion = Messages.getString(MessageIds.GDE_MSGT0658);
			if (objectText.split(IgcExportDialog.IGC_GPS_FIRMWARE_VERSION).length >= 2) {
				this.headerFirmwareVersion = objectText.split(IgcExportDialog.IGC_GPS_FIRMWARE_VERSION)[1].split(":|\\r|\\n")[1].trim(); //$NON-NLS-1$
			}
			this.headerHardwareVersion = Messages.getString(MessageIds.GDE_MSGT0659);
			if (objectText.split(IgcExportDialog.IGC_GPS_HARDWARE_VERSION).length >= 2) {
				this.headerHardwareVersion = objectText.split(IgcExportDialog.IGC_GPS_HARDWARE_VERSION)[1].split(":|\\r|\\n")[1].trim(); //$NON-NLS-1$
			}
			this.headerGpsManufacturerModel = Messages.getString(MessageIds.GDE_MSGT0660);
			if (objectText.split(IgcExportDialog.IGC_GPS_TYPE_IDENTIFIER).length >= 2) {
				this.headerGpsManufacturerModel = objectText.split(IgcExportDialog.IGC_GPS_TYPE_IDENTIFIER)[1].split(":|\\r|\\n")[1].trim(); //$NON-NLS-1$
			}
			this.headerCompetitionId = Messages.getString(MessageIds.GDE_MSGT0661);
			if (objectText.split(IgcExportDialog.IGC_COMPETITION_IDENTIFIER).length >= 2) {
				this.headerCompetitionId = objectText.split(IgcExportDialog.IGC_COMPETITION_IDENTIFIER)[1].split(":|\\r|\\n")[1].trim(); //$NON-NLS-1$
			}
			this.headerCompetitionClass = Messages.getString(MessageIds.GDE_MSGT0662);
			if (objectText.split(IgcExportDialog.IGC_COMPETITION_CLASS).length >= 2) {
				this.headerCompetitionClass = objectText.split(IgcExportDialog.IGC_COMPETITION_CLASS)[1].split(":|\\r|\\n")[1].trim(); //$NON-NLS-1$
			}
			this.headerUtcOffset = 12; // 0
			if (objectText.split(IgcExportDialog.IGC_UTC_OFFSET).length >= 2) {
				String tmpUtcOffset = objectText.split(IgcExportDialog.IGC_UTC_OFFSET)[1].split(":|\\r|\\n")[1].trim(); //$NON-NLS-1$
				if (tmpUtcOffset.startsWith("+"))this.headerUtcOffset = 12 + Integer.parseInt(tmpUtcOffset.substring(1)); //$NON-NLS-1$
				else if (tmpUtcOffset.startsWith("-"))this.headerUtcOffset = 12 - Integer.parseInt(tmpUtcOffset.substring(1)); //$NON-NLS-1$
				else
					this.headerUtcOffset = 12 + Integer.parseInt(tmpUtcOffset);
			}
		}
		else {
			this.headerPilot = Messages.getString(MessageIds.GDE_MSGT0655);
			this.headerCoPilot = Messages.getString(MessageIds.GDE_MSGT0656);
			this.headerGliderType = 4;
			this.headerGliderId = Messages.getString(MessageIds.GDE_MSGT0657);
			this.headerFirmwareVersion = Messages.getString(MessageIds.GDE_MSGT0658);
			this.headerHardwareVersion = Messages.getString(MessageIds.GDE_MSGT0659);
			this.headerGpsManufacturerModel = Messages.getString(MessageIds.GDE_MSGT0660);
			this.headerCompetitionId = Messages.getString(MessageIds.GDE_MSGT0661);
			this.headerCompetitionClass = Messages.getString(MessageIds.GDE_MSGT0662);
			this.headerUtcOffset = 12; // 0
		}
	}

	/**
	 * @return
	 */
	public StringBuilder getHeader() {
		StringBuilder header = new StringBuilder();
		if (this.headerRecordDateText != null) {
			header.append(String.format("AXDE %s\r\n", GDE.NAME_LONG, GDE.VERSION.replace(GDE.STRING_DOT, GDE.STRING_EMPTY)));
			header.append(String.format("HFDTE%s\r\n", this.headerRecordDateText.getText())); //$NON-NLS-1$
			header.append(String.format("HFFXA%s\r\n", this.headerFixAccuracyText.getText())); //$NON-NLS-1$
			header.append(String.format("HFPLTPILOT:%s\r\n", this.headerPilotText.getText())); //$NON-NLS-1$
			header.append(String.format("HFCM2CREW2:%s\r\n", this.headerCoPilotText.getText())); //$NON-NLS-1$
			header.append(String.format("HFGTYGLIDERTYPE:%s\r\n", this.headerGliderTypeText.getText().trim())); //$NON-NLS-1$
			header.append(String.format("HFGIDGLIDERID:%s\r\n", this.headerGliderIdText.getText())); //$NON-NLS-1$
			header.append(String.format("HFDTM100GPSDATUM:%s\r\n", this.headerGpsDatumText.getText())); //$NON-NLS-1$
			header.append(String.format("HFRFWFIRMWAREVERSION:%s\r\n", GDE.VERSION)); //$NON-NLS-1$
			header.append(String.format("HFRHWHARDWAREVERSION:%s\r\n", this.headerHardwareVersionText.getText())); //$NON-NLS-1$
			header.append(String.format("HFFTYFRTYPE:%s\r\n", this.headerGpsManufacturerModelText.getText())); //$NON-NLS-1$
			header.append(String.format("HFCIDCOMPETITIONID:%s\r\n", this.headerCompetitionIdText.getText())); //$NON-NLS-1$
			header.append(String.format("HFCCLCOMPETITIONCLASS:%s\r\n", this.headerCompetitionClassText.getText())); //$NON-NLS-1$
			String tmpUtfOffset = this.headerUtcOffsetCombo.getText().trim();
			header.append(String.format("HFTZNTIMEZONE:%s\r\n", tmpUtfOffset.startsWith("+") ? tmpUtfOffset.substring(1) : tmpUtfOffset)); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			header.append(String.format("AXDE %s %s\r\n", GDE.NAME_LONG, GDE.VERSION.replace(GDE.STRING_DOT, GDE.STRING_EMPTY)));
			header.append(String.format("HFDTE%s\r\n", this.headerRecordDate)); //$NON-NLS-1$
			header.append(String.format("HFFXA%s\r\n", this.headerFixAccuracy)); //$NON-NLS-1$
			header.append(String.format("HFPLTPILOT:%s\r\n", this.headerPilot)); //$NON-NLS-1$
			header.append(String.format("HFCM2CREW2:%s\r\n", this.headerCoPilot)); //$NON-NLS-1$
			header.append(String.format("HFGTYGLIDERTYPE:%s\r\n", this.gliderTypes[this.headerGliderType])); //$NON-NLS-1$
			header.append(String.format("HFGIDGLIDERID:%s\r\n", this.headerGliderId)); //$NON-NLS-1$
			header.append(String.format("HFDTM100GPSDATUM:%s\r\n", this.headerGpsDatum)); //$NON-NLS-1$
			header.append(String.format("HFRFWFIRMWAREVERSION:%s\r\n", GDE.VERSION)); //$NON-NLS-1$
			header.append(String.format("HFRHWHARDWAREVERSION:%s\r\n", this.headerHardwareVersion)); //$NON-NLS-1$
			header.append(String.format("HFFTYFRTYPE:%s\r\n", this.headerGpsManufacturerModel)); //$NON-NLS-1$
			header.append(String.format("HFCIDCOMPETITIONID:%s\r\n", this.headerCompetitionId)); //$NON-NLS-1$
			header.append(String.format("HFCCLCOMPETITIONCLASS:%s\r\n", this.headerCompetitionClass)); //$NON-NLS-1$
			String tmpUtfOffset = this.deltaUTC[this.headerUtcOffset];
			header.append(String.format("HFTZNTIMEZONE:%s\r\n", tmpUtfOffset.startsWith("+") ? tmpUtfOffset.substring(1) : tmpUtfOffset)); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return header;
	}
}
