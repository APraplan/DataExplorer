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

import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channels;
import gde.device.ChannelPropertyTypes;
import gde.device.DataTypes;
import gde.device.DeviceDialog;
import gde.device.graupner.hott.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

/**
 * Dialog class to enable visualization control
 * @author Winfried Brügmann
 */
public class HoTTAdapterDialog extends DeviceDialog {
	final static Logger					log										= Logger.getLogger(HoTTAdapterDialog.class.getName());

	CTabFolder									tabFolder;
	CTabItem										serialComTabItem;
	Composite										configMainComosite;
	Button											saveButton, closeButton, helpButton;
	CLabel											protocolTypesLabel, protocolTypesUnitLabel;
	CCombo											protocolTypesCombo;
	Button											inputFileButton;
	Button											startLifeDataCapturing, stopLifeDataCapturing;
	Button											enableChannelRecords, enableTextModusFilter, enableChannelPercent, enableFilter;
	CLabel											altitudeClimbSelectLabel;
	CCombo											altitudeClimbSelectCombo;
	CLabel											absorptionLevelLabel, filterStartTimeLabel, filterMaxTimeLabel, filterLapMinTimeLabel, filterMinDeltaRxDbmLabel, filterMinDeltaDistLabel;
	CCombo											absorptionLevelCombo, filterStartTimeCombo, filterMaxTimeCombo, filterLapMinTimeCombo, filterMinDeltaRxDbmCombo, filterMinDeltaDistCombo;

	final HoTTAdapter						device;																																			// get device specific things, get serial port, ...
	final Settings							settings;																																		// application configuration settings
	final HoTTAdapterSerialPort	serialPort;																																	// open/close port execute getData()....
	final Vector<HoTTAdapterDialogTabItem> dialogTabItems = new Vector<HoTTAdapterDialogTabItem>();

	HoTTAdapterLiveGatherer			lifeGatherer;
	boolean											isVisibilityChanged		= false;
	boolean											isHoTTAdapter 				= false;
	boolean											isHoTTAdapter2 				= false;
	boolean											isHoTTAdapterX 				= false;
	boolean											isHoTTAdapterD 				= false;

	int													measurementsCount			= 0;
	int 												protocolTypeOrdinal		= 0;
	String[]										altitudeClimbSensors = new String[] {"auto", "VARIO", "GPS", "GAM", "EAM"};
	String[]										filterItems = new String[] {"0", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55", "60", "65", "70", "75", "80", "85", "90", "95", "100", "105", "110", "115", "120", "130", "140", "150", "160", "170", "180", "190", "200"};
	String[]										filterMinItems = new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "25", "30", "40", "50", "60", "70", "80", "90", "100", "150"};
	String[]										filterMaxItems = new String[] {"50", "60", "70", "80", "90", "100", "110", "120", "130", "140", "150", "160", "170", "180", "190", "200", "210", "300", "360", "420", "480", "540", "600"};

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public HoTTAdapterDialog(Shell parent, HoTTAdapter useDevice) {
		super(parent);
		this.device = useDevice;
		this.serialPort = useDevice.getCommunicationPort();
		this.settings = Settings.getInstance();
		this.measurementsCount = 30;
//		for (int i = 1; i <= this.device.getChannelCount(); i++) {
//			int actualMeasurementCount = this.device.getNumberOfMeasurements(i);
//			this.measurementsCount = actualMeasurementCount > this.measurementsCount ? actualMeasurementCount : this.measurementsCount;
//		}
		this.protocolTypeOrdinal = this.device.getBaudeRate() == 115200 ? HoTTAdapter.Protocol.TYPE_115200.ordinal() : HoTTAdapter.Protocol.TYPE_19200_V4.ordinal();
		this.serialPort.setProtocolType(HoTTAdapter.Protocol.values()[this.protocolTypeOrdinal]);
	}

	@Override
	public void open() {
		try {
			dialogTabItems.clear();
			this.isHoTTAdapter = this.device != null && (this.device.getName().equals("HoTTAdapter") || this.device.getName().equals("HoTTAdapterM")); //$NON-NLS-1$
			this.isHoTTAdapter2 = this.device != null && this.device.getName().startsWith("HoTTAdapter2"); //$NON-NLS-1$
			this.isHoTTAdapterX = this.device != null && this.device.getName().equals("HoTTAdapterX"); //$NON-NLS-1$
			this.isHoTTAdapterD = this.device != null && this.device.getName().startsWith("HoTTAdapterD"); //$NON-NLS-1$

			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			log.log(java.util.logging.Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);

				FormLayout dialogShellLayout = new FormLayout();
				this.dialogShell.setLayout(dialogShellLayout);
				this.dialogShell.layout();
				//dialogShell.pack();
				this.dialogShell.setSize(620, this.isHoTTAdapterD || (this.isHoTTAdapter2 && application.getActiveChannelNumber() == 4) ? 622 : 582); //header + tab + label + this.measurementsCount * 28 + loadButton + save/close buttons
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
		      @Override
					public void handleEvent(Event event) {
		        switch (event.detail) {
		        case SWT.TRAVERSE_ESCAPE:
		        	HoTTAdapterDialog.this.dialogShell.close();
		          event.detail = SWT.TRAVERSE_NONE;
		          event.doit = false;
		          break;
		        }
		      }
		    });
				this.dialogShell.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent evt) {
						log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (HoTTAdapterDialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { HoTTAdapterDialog.this.device.getPropertiesFileName() });
							if (HoTTAdapterDialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
								HoTTAdapterDialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
						HoTTAdapterDialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					@Override
					public void helpRequested(HelpEvent evt) {
						log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						HoTTAdapterDialog.this.application.openHelpDialog("HoTTAdapter", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				{
					this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
					for (int i = 0; i < this.device.getChannelCount(); i++) {
						dialogTabItems.add(new HoTTAdapterDialogTabItem(this.tabFolder, this, (i + 1), this.device));
					}
					FormData tabFolderLData = new FormData();
					tabFolderLData.top = new FormAttachment(0, 1000, 0);
					tabFolderLData.left = new FormAttachment(0, 1000, 0);
					tabFolderLData.right = new FormAttachment(1000, 1000, 0);
					tabFolderLData.bottom = new FormAttachment(1000, 1000, -102);
					//tabFolderLData.bottom = new FormAttachment(1000, 1000, this.isHoTTAdapterD || (this.isHoTTAdapter2 && application.getActiveChannelNumber() == 6) ? -142 : -102);
					this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tabFolder.setLayoutData(tabFolderLData);
					this.tabFolder.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "configTabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
							int channelNumber = HoTTAdapterDialog.this.tabFolder.getSelectionIndex() + 1;
							//disable moving curves between configurations
							if (channelNumber > 0 && channelNumber <= HoTTAdapterDialog.this.device.getChannelCount()) { // enable other tabs for future use
								String configKey = channelNumber + " : " + ((CTabItem) evt.item).getText(); //$NON-NLS-1$
								Channels.getInstance().switchChannel(configKey);
								//PERFORMANCE: create measurement controls on the fly and not at dialog creation time
								if (!dialogTabItems.get(channelNumber - 1).isMeasurementTypesCreated()) {
									dialogTabItems.get(channelNumber - 1).createMeasurementsControls(channelNumber);
								}
							}
							if (isHoTTAdapterX) {
								HoTTAdapterDialog.this.enableFilter.setVisible(false);
								HoTTAdapterDialog.this.enableTextModusFilter.setVisible(false);

								HoTTAdapterDialog.this.startLifeDataCapturing.setVisible(false);
								HoTTAdapterDialog.this.stopLifeDataCapturing.setVisible(false);
								HoTTAdapterDialog.this.protocolTypesLabel.setVisible(false);
								HoTTAdapterDialog.this.protocolTypesCombo.setVisible(false);
								HoTTAdapterDialog.this.protocolTypesUnitLabel.setVisible(false);
							}
						}
					});
				}
				{
					if (this.isHoTTAdapter) {
						this.enableChannelRecords = new Button(this.dialogShell, SWT.CHECK);
						FormData enableFilterLData = new FormData();
						enableFilterLData.height = GDE.IS_MAC ? 22 : 20;
						enableFilterLData.left = new FormAttachment(0, 1000, 5);
						enableFilterLData.width = 60;
						enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -78 : -80);
						this.enableChannelRecords.setLayoutData(enableFilterLData);
						this.enableChannelRecords.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.enableChannelRecords.setText(Messages.getString(MessageIds.GDE_MSGT2424));
						this.enableChannelRecords.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2425));
						this.enableChannelRecords.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "enableChannelRecords.widgetSelected, event=" + evt); //$NON-NLS-1$
								HoTTAdapterDialog.this.device.setChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL, DataTypes.BOOLEAN, GDE.STRING_EMPTY + HoTTAdapterDialog.this.enableChannelRecords.getSelection());
								HoTTAdapterDialog.this.device.setChannelEnabledProperty(HoTTAdapterDialog.this.enableChannelRecords.getSelection());
								HoTTAdapterDialog.this.enableSaveButton(true);
							}
						});
					}
				}
//				{
//					if (this.isHoTTAdapterD || (this.isHoTTAdapter2 && application.getActiveChannelNumber() == 6)) {
//						{
//							this.filterStartTimeLabel = new CLabel(this.dialogShell, SWT.RIGHT);
//							FormData enableFilterLData = new FormData();
//							enableFilterLData.height = GDE.IS_MAC ? 22: 20;
//							enableFilterLData.left = new FormAttachment(0, 1000, 15);
//							enableFilterLData.width = 120;
//							enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -118 : -120);
//							this.filterStartTimeLabel.setLayoutData(enableFilterLData);
//							this.filterStartTimeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//							this.filterStartTimeLabel.setText("Start-Wartezeit [s]");
//							this.filterStartTimeLabel.setToolTipText("Zeit bevor das Rundenzählen startet");
//						}
//						{
//							this.filterStartTimeCombo = new CCombo(this.dialogShell, SWT.BORDER);
//							FormData enableFilterLData = new FormData();
//							enableFilterLData.height = GDE.IS_MAC ? 18 : 16;
//							enableFilterLData.left = new FormAttachment(0, 1000, 140);
//							enableFilterLData.width = 42;
//							enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -120 : -120);
//							this.filterStartTimeCombo.setLayoutData(enableFilterLData);
//							this.filterStartTimeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//							this.filterStartTimeCombo.setItems(filterMinItems);
//							//this.filterStartTimeCombo.select(10);
//							this.filterStartTimeCombo.setToolTipText("Größerer Wert -> späterer Start Rundenzeiten");
//							this.filterStartTimeCombo.setEditable(false);
//							this.filterStartTimeCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
//							this.filterStartTimeCombo.addSelectionListener(new SelectionAdapter() {
//								@Override
//								public void widgetSelected(SelectionEvent evt) {
//									log.log(java.util.logging.Level.FINEST, "filterStartTimeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
//									if (isHoTTAdapter2 && application.getActiveChannelNumber() == 6) {
//										//5=Rx_dbm, 18=Distance, 86=SmoothedRx_dbm, 87=DiffRx_dbm, 88=LapsRx_dbm 89=DiffDistance, 90=LapsDistance
//										HoTTAdapterDialog.this.device.setMeasurementPropertyValue(6, 87, MeasurementPropertyTypes.FILTER_FACTOR.value(), DataTypes.STRING, HoTTAdapterDialog.this.filterStartTimeCombo.getText().trim());
//										HoTTAdapterDialog.this.device.makeInActiveDisplayable(application.getActiveRecordSet());
//										HoTTAdapterDialog.this.enableSaveButton(true);
//									}
//									else if (isHoTTAdapterD) {
//										//5=Rx_dbm, 109=SmoothedRx_dbm, 110=DiffRx_dbm, 111=LapsRx_dbm
//										//15=Distance, 112=DiffDistance, 113=LapsDistance
//										HoTTAdapterDialog.this.device.setMeasurementPropertyValue(1, 110, MeasurementPropertyTypes.FILTER_FACTOR.value(), DataTypes.STRING, HoTTAdapterDialog.this.filterStartTimeCombo.getText().trim());
//										HoTTAdapterDialog.this.device.makeInActiveDisplayable(application.getActiveRecordSet());
//										HoTTAdapterDialog.this.enableSaveButton(true);
//									}
//								}
//							});
//						}
//						{
//							this.filterMaxTimeLabel = new CLabel(this.dialogShell, SWT.RIGHT);
//							FormData enableFilterLData = new FormData();
//							enableFilterLData.height = GDE.IS_MAC ? 22: 20;
//							enableFilterLData.left = new FormAttachment(0, 1000, 190);
//							enableFilterLData.width = 160;
//							enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -118 : -120);
//							this.filterMaxTimeLabel.setLayoutData(enableFilterLData);
//							this.filterMaxTimeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//							this.filterMaxTimeLabel.setText("Rundenzeit-Fenster [s]");
//							this.filterMaxTimeLabel.setToolTipText("Zeitfenster ab Startwartezeit in der das Rundenzählen statt findet");
//						}
//						{
//							this.filterMaxTimeCombo = new CCombo(this.dialogShell, SWT.BORDER);
//							FormData enableFilterLData = new FormData();
//							enableFilterLData.height = GDE.IS_MAC ? 18 : 16;
//							enableFilterLData.left = new FormAttachment(0, 1000, 360);
//							enableFilterLData.width = 42;
//							enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -120 : -120);
//							this.filterMaxTimeCombo.setLayoutData(enableFilterLData);
//							this.filterMaxTimeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//							this.filterMaxTimeCombo.setItems(filterMaxItems);
//							//this.filterMaxTimeCombo.select(10);
//							this.filterMaxTimeCombo.setToolTipText("Größerer Wert -> länger Rundenzeiten-Messung");
//							this.filterMaxTimeCombo.setEditable(false);
//							this.filterMaxTimeCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
//							this.filterMaxTimeCombo.addSelectionListener(new SelectionAdapter() {
//								@Override
//								public void widgetSelected(SelectionEvent evt) {
//									log.log(java.util.logging.Level.FINEST, "filterMaxTimeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
//									if (isHoTTAdapter2 && application.getActiveChannelNumber() == 6) {
//											//5=Rx_dbm, 18=Distance, 86=SmoothedRx_dbm, 87=DiffRx_dbm, 88=LapsRx_dbm 89=DiffDistance, 90=LapsDistance
//										HoTTAdapterDialog.this.device.setMeasurementPropertyValue(6, 5, MeasurementPropertyTypes.FILTER_FACTOR.value(), DataTypes.STRING, HoTTAdapterDialog.this.filterMaxTimeCombo.getText().trim());
//										HoTTAdapterDialog.this.device.makeInActiveDisplayable(application.getActiveRecordSet());
//										HoTTAdapterDialog.this.enableSaveButton(true);
//									}
//									else if (isHoTTAdapterD) {
//										//5=Rx_dbm, 109=SmoothedRx_dbm, 110=DiffRx_dbm, 111=LapsRx_dbm
//										//15=Distance, 112=DiffDistance, 113=LapsDistance
//										HoTTAdapterDialog.this.device.setMeasurementPropertyValue(1, 5, MeasurementPropertyTypes.FILTER_FACTOR.value(), DataTypes.STRING, HoTTAdapterDialog.this.filterMaxTimeCombo.getText().trim());
//										HoTTAdapterDialog.this.device.makeInActiveDisplayable(application.getActiveRecordSet());
//										HoTTAdapterDialog.this.enableSaveButton(true);
//									}
//								}
//							});
//						}
//						{
//							this.filterLapMinTimeLabel = new CLabel(this.dialogShell, SWT.RIGHT);
//							FormData enableFilterLData = new FormData();
//							enableFilterLData.height = GDE.IS_MAC ? 22: 20;
//							enableFilterLData.left = new FormAttachment(0, 1000, 410);
//							enableFilterLData.width = 150;
//							enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -118 : -120);
//							this.filterLapMinTimeLabel.setLayoutData(enableFilterLData);
//							this.filterLapMinTimeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//							this.filterLapMinTimeLabel.setText("Minimale Rundenzeit [s]");
//							this.filterLapMinTimeLabel.setToolTipText("Etwas mehr, wie die halbe Rundenzeit einstellen");
//						}
//						{
//							this.filterLapMinTimeCombo = new CCombo(this.dialogShell, SWT.BORDER);
//							FormData enableFilterLData = new FormData();
//							enableFilterLData.height = GDE.IS_MAC ? 18 : 16;
//							enableFilterLData.left = new FormAttachment(0, 1000, 565);
//							enableFilterLData.width = 42;
//							enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -120 : -120);
//							this.filterLapMinTimeCombo.setLayoutData(enableFilterLData);
//							this.filterLapMinTimeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//							this.filterLapMinTimeCombo.setItems(filterMinItems);
//							//this.filterLapMinTimeCombo.select(0);
//							this.filterLapMinTimeCombo.setToolTipText("Filter Rundenzeitfehlmessung");
//							this.filterLapMinTimeCombo.setEditable(false);
//							this.filterLapMinTimeCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
//							this.filterLapMinTimeCombo.addSelectionListener(new SelectionAdapter() {
//								@Override
//								public void widgetSelected(SelectionEvent evt) {
//									log.log(java.util.logging.Level.FINEST, "filterLapMinTimeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
//									if (isHoTTAdapter2 && application.getActiveChannelNumber() == 6) {
//										//5=Rx_dbm, 18=Distance, 86=SmoothedRx_dbm, 87=DiffRx_dbm, 88=LapsRx_dbm 89=DiffDistance, 90=LapsDistance
//										HoTTAdapterDialog.this.device.setMeasurementPropertyValue(6, 88, MeasurementPropertyTypes.FILTER_FACTOR.value(), DataTypes.STRING, HoTTAdapterDialog.this.filterLapMinTimeCombo.getText().trim());
//										HoTTAdapterDialog.this.device.makeInActiveDisplayable(application.getActiveRecordSet());
//										HoTTAdapterDialog.this.enableSaveButton(true);
//									}
//									else if (isHoTTAdapterD) {
//										//5=Rx_dbm, 109=SmoothedRx_dbm, 110=DiffRx_dbm, 111=LapsRx_dbm
//										//15=Distance, 112=DiffDistance, 113=LapsDistance
//										HoTTAdapterDialog.this.device.setMeasurementPropertyValue(1, 111, MeasurementPropertyTypes.FILTER_FACTOR.value(), DataTypes.STRING, HoTTAdapterDialog.this.filterLapMinTimeCombo.getText().trim());
//										HoTTAdapterDialog.this.device.makeInActiveDisplayable(application.getActiveRecordSet());
//										HoTTAdapterDialog.this.enableSaveButton(true);
//									}
//								}
//							});
//						}
//						{
//							this.absorptionLevelLabel = new CLabel(this.dialogShell, SWT.RIGHT);
//							FormData enableFilterLData = new FormData();
//							enableFilterLData.height = GDE.IS_MAC ? 22: 20;
//							enableFilterLData.left = new FormAttachment(0, 1000, 15);
//							enableFilterLData.width = 140;
//							enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -98 : -100);
//							this.absorptionLevelLabel.setLayoutData(enableFilterLData);
//							this.absorptionLevelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//							this.absorptionLevelLabel.setText("Rx dbm Glättungsgrad");
//							this.absorptionLevelLabel.setToolTipText("Glättung von Rx dbm");
//						}
//						{
//							this.absorptionLevelCombo = new CCombo(this.dialogShell, SWT.BORDER);
//							FormData enableFilterLData = new FormData();
//							enableFilterLData.height = GDE.IS_MAC ? 18 : 16;
//							enableFilterLData.left = new FormAttachment(0, 1000, 160);
//							enableFilterLData.width = 42;
//							enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -100 : -100);
//							this.absorptionLevelCombo.setLayoutData(enableFilterLData);
//							this.absorptionLevelCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//							this.absorptionLevelCombo.setItems(filterItems);
//							this.absorptionLevelCombo.setToolTipText("Größerer Wert -> größere Glättung, 0 == aus");
//							this.absorptionLevelCombo.setEditable(false);
//							this.absorptionLevelCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
//							this.absorptionLevelCombo.addSelectionListener(new SelectionAdapter() {
//								@Override
//								public void widgetSelected(SelectionEvent evt) {
//									log.log(java.util.logging.Level.FINEST, "absorptionLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
//									if (isHoTTAdapter2 && application.getActiveChannelNumber() == 6) {
//										//5=Rx_dbm, 18=Distance, 86=SmoothedRx_dbm, 87=DiffRx_dbm, 88=LapsRx_dbm 89=DiffDistance, 90=LapsDistance
//										HoTTAdapterDialog.this.device.setMeasurementPropertyValue(6, 86, MeasurementPropertyTypes.FILTER_FACTOR.value(), DataTypes.STRING, HoTTAdapterDialog.this.absorptionLevelCombo.getText().trim());
//										HoTTAdapterDialog.this.device.makeInActiveDisplayable(application.getActiveRecordSet());
//										HoTTAdapterDialog.this.enableSaveButton(true);
//									}
//									else if (isHoTTAdapterD) {
//										//5=Rx_dbm, 109=SmoothedRx_dbm, 110=DiffRx_dbm, 111=LapsRx_dbm
//										//15=Distance, 112=DiffDistance, 113=LapsDistance
//										HoTTAdapterDialog.this.device.setMeasurementPropertyValue(1, 109, MeasurementPropertyTypes.FILTER_FACTOR.value(), DataTypes.STRING, HoTTAdapterDialog.this.absorptionLevelCombo.getText().trim());
//										HoTTAdapterDialog.this.device.makeInActiveDisplayable(application.getActiveRecordSet());
//										HoTTAdapterDialog.this.enableSaveButton(true);
//									}
//								}
//							});
//						}
//						{
//							this.filterMinDeltaRxDbmLabel = new CLabel(this.dialogShell, SWT.RIGHT);
//							FormData enableFilterLData = new FormData();
//							enableFilterLData.height = GDE.IS_MAC ? 22: 20;
//							enableFilterLData.left = new FormAttachment(0, 1000, 190);
//							enableFilterLData.width = 160;
//							enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -98 : -100);
//							this.filterMinDeltaRxDbmLabel.setLayoutData(enableFilterLData);
//							this.filterMinDeltaRxDbmLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//							this.filterMinDeltaRxDbmLabel.setText("Min-Delta Rx [dbm]");
//							this.filterMinDeltaRxDbmLabel.setToolTipText("Etwas mehr, wie die halbe maximale Differenz einstellen");
//						}
//						{
//							this.filterMinDeltaRxDbmCombo = new CCombo(this.dialogShell, SWT.BORDER);
//							FormData enableFilterLData = new FormData();
//							enableFilterLData.height = GDE.IS_MAC ? 18 : 16;
//							enableFilterLData.left = new FormAttachment(0, 1000, 360);
//							enableFilterLData.width = 42;
//							enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -100 : -100);
//							this.filterMinDeltaRxDbmCombo.setLayoutData(enableFilterLData);
//							this.filterMinDeltaRxDbmCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//							this.filterMinDeltaRxDbmCombo.setItems(filterMinItems);
//							//this.filterMinDeltaRxDbmCombo.select(10);
//							this.filterMinDeltaRxDbmCombo.setToolTipText("Größerer Wert -> längere Rundenzeiten-Messung");
//							this.filterMinDeltaRxDbmCombo.setEditable(false);
//							this.filterMinDeltaRxDbmCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
//							this.filterMinDeltaRxDbmCombo.addSelectionListener(new SelectionAdapter() {
//								@Override
//								public void widgetSelected(SelectionEvent evt) {
//									log.log(java.util.logging.Level.FINEST, "filterMinDeltaRxDbmCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
//									if (isHoTTAdapter2 && application.getActiveChannelNumber() == 6) {
//										//5=Rx_dbm, 18=Distance, 86=SmoothedRx_dbm, 87=DiffRx_dbm, 88=LapsRx_dbm 89=DiffDistance, 90=LapsDistance
//										HoTTAdapterDialog.this.device.setMeasurementPropertyValue(6, 87, MeasurementPropertyTypes.NONE_SPECIFIED.value(), DataTypes.STRING, HoTTAdapterDialog.this.filterMinDeltaRxDbmCombo.getText().trim());
//										HoTTAdapterDialog.this.device.makeInActiveDisplayable(application.getActiveRecordSet());
//										HoTTAdapterDialog.this.enableSaveButton(true);
//									}
//									else if (isHoTTAdapterD) {
//										//5=Rx_dbm, 109=SmoothedRx_dbm, 110=DiffRx_dbm, 111=LapsRx_dbm
//										//15=Distance, 112=DiffDistance, 113=LapsDistance
//										HoTTAdapterDialog.this.device.setMeasurementPropertyValue(1, 110, MeasurementPropertyTypes.NONE_SPECIFIED.value(), DataTypes.STRING, HoTTAdapterDialog.this.filterMinDeltaRxDbmCombo.getText().trim());
//										HoTTAdapterDialog.this.device.makeInActiveDisplayable(application.getActiveRecordSet());
//										HoTTAdapterDialog.this.enableSaveButton(true);
//									}
//								}
//							});
//						}
//						{
//							this.filterMinDeltaDistLabel = new CLabel(this.dialogShell, SWT.RIGHT);
//							FormData enableFilterLData = new FormData();
//							enableFilterLData.height = GDE.IS_MAC ? 22: 20;
//							enableFilterLData.left = new FormAttachment(0, 1000, 410);
//							enableFilterLData.width = 150;
//							enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -98 : -100);
//							this.filterMinDeltaDistLabel.setLayoutData(enableFilterLData);
//							this.filterMinDeltaDistLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//							this.filterMinDeltaDistLabel.setText("Min-Delta Distance [m]");
//							this.filterMinDeltaDistLabel.setToolTipText("Etwas mehr, wie die halbe maximale Differenz einstellen");
//						}
//						{
//							this.filterMinDeltaDistCombo = new CCombo(this.dialogShell, SWT.BORDER);
//							FormData enableFilterLData = new FormData();
//							enableFilterLData.height = GDE.IS_MAC ? 18 : 16;
//							enableFilterLData.left = new FormAttachment(0, 1000, 565);
//							enableFilterLData.width = 42;
//							enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -100 : -100);
//							this.filterMinDeltaDistCombo.setLayoutData(enableFilterLData);
//							this.filterMinDeltaDistCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//							this.filterMinDeltaDistCombo.setItems(filterMinItems);
//							//this.filterMinDistDeltaCombo.select(0);
//							this.filterMinDeltaDistCombo.setToolTipText("Filter Rundenzeitfehlmessung durch Einschränken auf eine Differenz");
//							this.filterMinDeltaDistCombo.setEditable(false);
//							this.filterMinDeltaDistCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
//							this.filterMinDeltaDistCombo.addSelectionListener(new SelectionAdapter() {
//								@Override
//								public void widgetSelected(SelectionEvent evt) {
//									log.log(java.util.logging.Level.FINEST, "filterMinDistDeltaCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
//									if (isHoTTAdapter2 && application.getActiveChannelNumber() == 6) {
//										//5=Rx_dbm, 18=Distance, 86=SmoothedRx_dbm, 87=DiffRx_dbm, 88=LapsRx_dbm 89=DiffDistance, 90=LapsDistance
//										HoTTAdapterDialog.this.device.setMeasurementPropertyValue(6, 89, MeasurementPropertyTypes.NONE_SPECIFIED.value(), DataTypes.STRING, HoTTAdapterDialog.this.filterMinDeltaDistCombo.getText().trim());
//										HoTTAdapterDialog.this.device.makeInActiveDisplayable(application.getActiveRecordSet());
//										HoTTAdapterDialog.this.enableSaveButton(true);
//									}
//									else if (isHoTTAdapterD) {
//										//5=Rx_dbm, 109=SmoothedRx_dbm, 110=DiffRx_dbm, 111=LapsRx_dbm
//										//15=Distance, 112=DiffDistance, 113=LapsDistance
//										HoTTAdapterDialog.this.device.setMeasurementPropertyValue(1, 112, MeasurementPropertyTypes.NONE_SPECIFIED.value(), DataTypes.STRING, HoTTAdapterDialog.this.filterMinDeltaDistCombo.getText().trim());
//										HoTTAdapterDialog.this.device.makeInActiveDisplayable(application.getActiveRecordSet());
//										HoTTAdapterDialog.this.enableSaveButton(true);
//									}
//								}
//							});
//						}
//					}
//				}
				{
					this.enableFilter = new Button(this.dialogShell, SWT.CHECK);
					FormData enableFilterLData = new FormData();
					enableFilterLData.height = GDE.IS_MAC ? 22 : 20;
					enableFilterLData.left = new FormAttachment(0, 1000, this.isHoTTAdapter ? 75 : 15);
					enableFilterLData.width = this.isHoTTAdapter ? 55 : 70;
					enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -78 : -80);
					this.enableFilter.setLayoutData(enableFilterLData);
					this.enableFilter.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.enableFilter.setText(Messages.getString(MessageIds.GDE_MSGT2417));
					this.enableFilter.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2418));
					this.enableFilter.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "enableFilter.widgetSelected, event=" + evt); //$NON-NLS-1$
							HoTTAdapterDialog.this.device.setChannelProperty(ChannelPropertyTypes.ENABLE_FILTER, DataTypes.BOOLEAN, GDE.STRING_EMPTY + HoTTAdapterDialog.this.enableFilter.getSelection());
							HoTTAdapterDialog.this.device.setFilterProperties(HoTTAdapterDialog.this.enableFilter.getSelection());
							HoTTAdapterDialog.this.enableSaveButton(true);
						}
					});
				}
				{
					this.enableTextModusFilter = new Button(this.dialogShell, SWT.CHECK);
					FormData enableTextModusFilterLData = new FormData();
					enableTextModusFilterLData.height = GDE.IS_MAC ? 22 : 20;
					enableTextModusFilterLData.left = new FormAttachment(0, 1000, this.isHoTTAdapter ? 132 : 100);
					enableTextModusFilterLData.width = this.isHoTTAdapter ? 95 : 95;
					enableTextModusFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -78 : -80);
					this.enableTextModusFilter.setLayoutData(enableTextModusFilterLData);
					this.enableTextModusFilter.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.enableTextModusFilter.setText(Messages.getString(MessageIds.GDE_MSGT2438));
					this.enableTextModusFilter.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2439));
					this.enableTextModusFilter.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "enableTextModusFilter.widgetSelected, event=" + evt); //$NON-NLS-1$
							HoTTAdapterDialog.this.device.setChannelProperty(ChannelPropertyTypes.TEXT_MODE, DataTypes.BOOLEAN, GDE.STRING_EMPTY + HoTTAdapterDialog.this.enableTextModusFilter.getSelection());
							HoTTAdapterDialog.this.device.setTextModusFilter(HoTTAdapterDialog.this.enableTextModusFilter.getSelection());
							HoTTAdapterDialog.this.enableSaveButton(true);
						}
					});
				}
				{
					this.enableChannelPercent = new Button(this.dialogShell, SWT.CHECK);
					FormData enableTextModusFilterLData = new FormData();
					enableTextModusFilterLData.height = GDE.IS_MAC ? 22 : 20;
					enableTextModusFilterLData.left = new FormAttachment(0, 1000, this.isHoTTAdapter ? 250 : 225);
					enableTextModusFilterLData.width = this.isHoTTAdapter ? 105 : 140;
					enableTextModusFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -78 : -80);
					this.enableChannelPercent.setLayoutData(enableTextModusFilterLData);
					this.enableChannelPercent.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.enableChannelPercent.setText(Messages.getString(MessageIds.GDE_MSGT2451));
					this.enableChannelPercent.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2452));
					this.enableChannelPercent.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "enableChannelPercent.widgetSelected, event=" + evt); //$NON-NLS-1$
							HoTTAdapterDialog.this.device.setChannelProperty(ChannelPropertyTypes.CHANNEL_PERCENTAGE, DataTypes.BOOLEAN, GDE.STRING_EMPTY + HoTTAdapterDialog.this.enableChannelPercent.getSelection());
							HoTTAdapterDialog.this.device.setChannelPercent(HoTTAdapterDialog.this.enableChannelPercent.getSelection());
							HoTTAdapterDialog.this.enableSaveButton(true);
						}
					});
				}
				{
					this.altitudeClimbSelectLabel = new CLabel(this.dialogShell, SWT.RIGHT);
					FormData enableFilterLData = new FormData();
					enableFilterLData.height = GDE.IS_MAC ? 22: 20;
					enableFilterLData.left = new FormAttachment(0, 1000, 380); //not used for HoTTAdapter
					enableFilterLData.width = this.isHoTTAdapter ? 135 : 140;
					enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -78 : -80);
					this.altitudeClimbSelectLabel.setLayoutData(enableFilterLData);
					this.altitudeClimbSelectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.altitudeClimbSelectLabel.setText(Messages.getString(MessageIds.GDE_MSGT2419));
					this.altitudeClimbSelectLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2420));
				}
				{
					this.altitudeClimbSelectCombo = new CCombo(this.dialogShell, SWT.BORDER);
					FormData enableFilterLData = new FormData();
					enableFilterLData.height = GDE.IS_MAC ? 18 : 16;
					enableFilterLData.left = new FormAttachment(0, 1000, 520); //not used for HoTTAdapter
					enableFilterLData.width = 62;
					enableFilterLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -80 : -80);
					this.altitudeClimbSelectCombo.setLayoutData(enableFilterLData);
					this.altitudeClimbSelectCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.altitudeClimbSelectCombo.setItems(altitudeClimbSensors);
					this.altitudeClimbSelectCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2420));
					this.altitudeClimbSelectCombo.setEditable(false);
					this.altitudeClimbSelectCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.altitudeClimbSelectCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "filterFactorLatitudeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							HoTTAdapterDialog.this.device.setChannelProperty(ChannelPropertyTypes.SENSOR_ALT_CLIMB, DataTypes.INTEGER, GDE.STRING_EMPTY + HoTTAdapterDialog.this.altitudeClimbSelectCombo.getSelectionIndex());
							HoTTAdapterDialog.this.device.setAltitudeClimbSelectionProperties(HoTTAdapterDialog.this.altitudeClimbSelectCombo.getSelectionIndex());
							HoTTAdapterDialog.this.enableSaveButton(true);
						}
					});
				}

				{
					this.startLifeDataCapturing = new Button(this.dialogShell, SWT.None);
					FormData startCapturingButtonLData = new FormData();
					startCapturingButtonLData.height = GDE.IS_MAC ? 33 : 30;
					startCapturingButtonLData.left = new FormAttachment(0, 1000, 210);
					startCapturingButtonLData.width = 200;
					startCapturingButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -43 : -45);
					this.startLifeDataCapturing.setLayoutData(startCapturingButtonLData);
					this.startLifeDataCapturing.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.startLifeDataCapturing.setText(Messages.getString(MessageIds.GDE_MSGT2413));
					this.startLifeDataCapturing.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "startLifeDataCapturing.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (HoTTAdapterDialog.this.device != null) {
								try {
									String toolTipText = HoTTAdapter.getImportToolTip();
									HoTTAdapterDialog.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, toolTipText, toolTipText);
									HoTTAdapterDialog.this.lifeGatherer = HoTTAdapterDialog.this.device.getName().equals("HoTTAdapter") //$NON-NLS-1$
									? new HoTTAdapterLiveGatherer(HoTTAdapterDialog.this.application, HoTTAdapterDialog.this.device, HoTTAdapterDialog.this.serialPort, HoTTAdapterDialog.this)
											: new HoTTAdapter2LiveGatherer(HoTTAdapterDialog.this.application, HoTTAdapterDialog.this.device, HoTTAdapterDialog.this.serialPort, HoTTAdapterDialog.this);
									HoTTAdapterDialog.this.lifeGatherer.start();
									HoTTAdapterDialog.this.startLifeDataCapturing.setEnabled(false);
									HoTTAdapterDialog.this.stopLifeDataCapturing.setEnabled(true);
									HoTTAdapterDialog.this.protocolTypesCombo.setEnabled(false);
									HoTTAdapterDialog.this.inputFileButton.setEnabled(false);
								}
								catch (Exception e) {
									log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
									String toolTipText = HoTTAdapter.getImportToolTip();
									HoTTAdapterDialog.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
									HoTTAdapterDialog.this.serialPort.close();
									HoTTAdapterDialog.this.startLifeDataCapturing.setEnabled(true);
									HoTTAdapterDialog.this.stopLifeDataCapturing.setEnabled(false);
									HoTTAdapterDialog.this.protocolTypesCombo.setEnabled(true);
									HoTTAdapterDialog.this.inputFileButton.setEnabled(true);
								}
							}
						}
					});
				}
				{
					this.stopLifeDataCapturing = new Button(this.dialogShell, SWT.None);
					FormData stopCapturingButtonLData = new FormData();
					stopCapturingButtonLData.height = GDE.IS_MAC ? 33 : 30;
					stopCapturingButtonLData.left = new FormAttachment(0, 1000, 210);
					stopCapturingButtonLData.width = 200;
					stopCapturingButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.stopLifeDataCapturing.setLayoutData(stopCapturingButtonLData);
					this.stopLifeDataCapturing.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.stopLifeDataCapturing.setText(Messages.getString(MessageIds.GDE_MSGT2414));
					this.stopLifeDataCapturing.setEnabled(false);
					this.stopLifeDataCapturing.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "stopLifeDataCapturing.widgetSelected, event=" + evt); //$NON-NLS-1$
							HoTTAdapterDialog.this.serialPort.isInterruptedByUser = true;
						}
					});
				}
				{
					this.inputFileButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData inputFileButtonLData = new FormData();
					inputFileButtonLData.height = GDE.IS_MAC ? 33 : 30;
					inputFileButtonLData.left = new FormAttachment(0, 1000, 10);
					inputFileButtonLData.width = 160;
					inputFileButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -43 : -45);
					this.inputFileButton.setLayoutData(inputFileButtonLData);
					this.inputFileButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.inputFileButton.setText(Messages.getString(MessageIds.GDE_MSGT2402));
					this.inputFileButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2410));
					this.inputFileButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "inputFileButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (HoTTAdapterDialog.this.isVisibilityChanged) {
								String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { HoTTAdapterDialog.this.device.getPropertiesFileName() });
								if (HoTTAdapterDialog.this.application.openYesNoMessageDialog(HoTTAdapterDialog.this.getDialogShell(), msg) == SWT.YES) {
									log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
									HoTTAdapterDialog.this.device.storeDeviceProperties();
								}
							}
							HoTTAdapterDialog.this.device.open_closeCommPort();
						}
					});
				}
				{
					this.protocolTypesLabel = new CLabel(this.dialogShell, SWT.RIGHT);
					this.protocolTypesLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.protocolTypesLabel.setText(Messages.getString(MessageIds.GDE_MSGT2411));
					this.protocolTypesLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2415));
					FormData timeZoneOffsetUTCLabelLData = new FormData();
					timeZoneOffsetUTCLabelLData.width = 70;
					timeZoneOffsetUTCLabelLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					timeZoneOffsetUTCLabelLData.bottom = new FormAttachment(1000, 1000, -50);
					timeZoneOffsetUTCLabelLData.right = new FormAttachment(1000, 1000, -130);
					this.protocolTypesLabel.setLayoutData(timeZoneOffsetUTCLabelLData);
				}
				{
					this.protocolTypesCombo = new CCombo(this.dialogShell, SWT.RIGHT | SWT.BORDER);
					this.protocolTypesCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.protocolTypesCombo.setItems(HoTTAdapter.Protocol.valuesAsStingArray());
					FormData timeZoneOffsetUTCComboLData = new FormData();
					timeZoneOffsetUTCComboLData.width = 85;
					timeZoneOffsetUTCComboLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					timeZoneOffsetUTCComboLData.bottom = new FormAttachment(1000, 1000, -50);
					timeZoneOffsetUTCComboLData.right = new FormAttachment(1000, 1000, -40);
					this.protocolTypesCombo.setLayoutData(timeZoneOffsetUTCComboLData);
					this.protocolTypesCombo.select(this.protocolTypeOrdinal);
					this.protocolTypesCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2415));
					this.protocolTypesCombo.setEditable(false);
					this.protocolTypesCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.protocolTypesCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "timeZoneOffsetUTCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							HoTTAdapterDialog.this.device.setBaudeRate(Integer.parseInt(HoTTAdapterDialog.this.protocolTypesCombo.getText().trim().split(GDE.STRING_BLANK)[0]));
							HoTTAdapterDialog.this.protocolTypeOrdinal = HoTTAdapterDialog.this.protocolTypesCombo.getSelectionIndex();
							HoTTAdapterDialog.this.serialPort.setProtocolType(HoTTAdapter.Protocol.values()[HoTTAdapterDialog.this.protocolTypeOrdinal]);
							HoTTAdapterDialog.this.saveButton.setEnabled(true);
						}
					});
				}
				{
					this.protocolTypesUnitLabel = new CLabel(this.dialogShell, SWT.RIGHT);
					this.protocolTypesUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.protocolTypesUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2412));
					this.protocolTypesUnitLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2415));
					FormData timeZoneOffsetUTCUnitLData = new FormData();
					timeZoneOffsetUTCUnitLData.width = 30;
					timeZoneOffsetUTCUnitLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
					timeZoneOffsetUTCUnitLData.bottom = new FormAttachment(1000, 1000, -50);
					timeZoneOffsetUTCUnitLData.right = new FormAttachment(1000, 1000, -3);
					this.protocolTypesUnitLabel.setLayoutData(timeZoneOffsetUTCUnitLData);
				}
				{
					this.saveButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData saveButtonLData = new FormData();
					saveButtonLData.width = 105;
					saveButtonLData.height = GDE.IS_MAC ? 33 : 30;
					saveButtonLData.left = new FormAttachment(0, 1000, 10);
					saveButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.saveButton.setLayoutData(saveButtonLData);
					this.saveButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.saveButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0486));
					this.saveButton.setEnabled(false);
					this.saveButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							HoTTAdapterDialog.this.device.storeDeviceProperties();
							HoTTAdapterDialog.this.saveButton.setEnabled(false);
						}
					});
				}
				{
					this.helpButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData helpButtonLData = new FormData();
					helpButtonLData.width = GDE.IS_MAC ? 50 : 40;
					helpButtonLData.height = GDE.IS_MAC ? 33 : 30;
					helpButtonLData.left = new FormAttachment(0, 1000, GDE.IS_MAC ? 129 : 132);
					helpButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.helpButton.setLayoutData(helpButtonLData);
					this.helpButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.helpButton.setImage(SWTResourceManager.getImage("gde/resource/QuestionHot.gif")); //$NON-NLS-1$
					this.helpButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							HoTTAdapterDialog.this.application.openHelpDialog("HoTTAdapter", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
						}
					});
				}
				{
					this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData closeButtonLData = new FormData();
					closeButtonLData.width = 160;
					closeButtonLData.height = GDE.IS_MAC ? 33 : 30;
					closeButtonLData.right = new FormAttachment(1000, 1000, -10);
					closeButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.closeButton.setLayoutData(closeButtonLData);
					this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0485));
					this.closeButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							HoTTAdapterDialog.this.dialogShell.dispose();
						}
					});
				}

				try {
					this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
				}
				catch (RuntimeException e) {
					this.tabFolder.setSelection(0);
				}

				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - this.dialogShell.getSize().x/2, 0));
				this.dialogShell.open();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}

			if (this.device.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL).getValue() != null && this.enableChannelRecords != null)
				this.enableChannelRecords.setSelection(Boolean.parseBoolean(this.device.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL).getValue()));
			this.enableFilter.setSelection(Boolean.parseBoolean(this.device.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER).getValue()));
			this.enableTextModusFilter.setSelection(Boolean.parseBoolean(this.device.getChannelProperty(ChannelPropertyTypes.TEXT_MODE).getValue() != null ? this.device.getChannelProperty(ChannelPropertyTypes.TEXT_MODE).getValue() : "true"));
			this.enableChannelPercent.setSelection(Boolean.parseBoolean(this.device.getChannelProperty(ChannelPropertyTypes.CHANNEL_PERCENTAGE).getValue() != null ? this.device.getChannelProperty(ChannelPropertyTypes.CHANNEL_PERCENTAGE).getValue() : "true"));
			
			if (isHoTTAdapter) { //altitudeClimbSelection usable for HoTTAdapter2, HoTTAdapterD only
				HoTTAdapterDialog.this.altitudeClimbSelectLabel.setVisible(false);
				HoTTAdapterDialog.this.altitudeClimbSelectCombo.setVisible(false);
			}
			else if (isHoTTAdapter2 || isHoTTAdapterD) {
				this.altitudeClimbSelectCombo.select(Integer.parseInt(this.device.getChannelProperty(ChannelPropertyTypes.SENSOR_ALT_CLIMB).getValue() != null ? this.device.getChannelProperty(ChannelPropertyTypes.SENSOR_ALT_CLIMB).getValue() : "0"));
//				int activeChannelNumber = this.application.getActiveChannelNumber();
//				if (activeChannelNumber == 6 && this.device.getNumberOfMeasurements(activeChannelNumber) > 86  && this.device.getMeasurementPropertyValue(activeChannelNumber, 88, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().length() > 0) {
//					this.filterStartTimeCombo.select(findPosition(filterMinItems, this.device.getMeasurementPropertyValue(activeChannelNumber, 87, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim(), 10));
//					this.filterLapMinTimeCombo.select(findPosition(filterMinItems, this.device.getMeasurementPropertyValue(activeChannelNumber, 88, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim(), 0));
//					this.filterMaxTimeCombo.select(findPosition(filterMaxItems, this.device.getMeasurementPropertyValue(activeChannelNumber, 5, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim(), 10));
//					//5=Rx_dbm, 18=Distance, 86=SmoothedRx_dbm, 87=DiffRx_dbm, 88=LapsRx_dbm 89=DiffDistance, 90=LapsDistance
//					this.absorptionLevelCombo.select(findPosition(filterItems, this.device.getMeasurementPropertyValue(activeChannelNumber, 72, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim(), 0));
//					this.filterMinDeltaRxDbmCombo.select(findPosition(filterMinItems, this.device.getMeasurementPropertyValue(activeChannelNumber, 86, MeasurementPropertyTypes.NONE_SPECIFIED.value()).toString().trim(), 10));
//					this.filterMinDeltaDistCombo.select(findPosition(filterMinItems, this.device.getMeasurementPropertyValue(activeChannelNumber, 89, MeasurementPropertyTypes.NONE_SPECIFIED.value()).toString().trim(), 0));
//				}
//			}
//			else if (isHoTTAdapterD) {
//				this.altitudeClimbSelectCombo.select(Integer.parseInt(this.device.getChannelProperty(ChannelPropertyTypes.SENSOR_ALT_CLIMB).getValue() != null ? this.device.getChannelProperty(ChannelPropertyTypes.SENSOR_ALT_CLIMB).getValue() : "0"));
//				if (this.device.getMeasurementPropertyValue(1, 111, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().length() > 0) {
//					this.filterStartTimeCombo.select(findPosition(filterMinItems, this.device.getMeasurementPropertyValue(1, 110, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim(), 10));
//					this.filterLapMinTimeCombo.select(findPosition(filterMinItems, this.device.getMeasurementPropertyValue(1, 111, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim(), 0));
//					this.filterMaxTimeCombo.select(findPosition(filterMaxItems, this.device.getMeasurementPropertyValue(1, 5, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim(), 10));
//					//5=Rx_dbm, 109=SmoothedRx_dbm, 110=DiffRx_dbm, 111=LapsRx_dbm
//					this.absorptionLevelCombo.select(findPosition(filterItems, this.device.getMeasurementPropertyValue(1, 109, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim(), 0));
//					//15=Distance, 112=DiffDistance, 113=LapsDistance
//					this.filterMinDeltaRxDbmCombo.select(findPosition(filterMinItems, this.device.getMeasurementPropertyValue(1, 110, MeasurementPropertyTypes.NONE_SPECIFIED.value()).toString().trim(), 10));
//					this.filterMinDeltaDistCombo.select(findPosition(filterMinItems, this.device.getMeasurementPropertyValue(1, 112, MeasurementPropertyTypes.NONE_SPECIFIED.value()).toString().trim(), 0));
//				}
			}

			if (this.serialPort.isConnected()) { // check serial port state, if user has closed the dialog while gathering data
				this.startLifeDataCapturing.setEnabled(false);
				this.stopLifeDataCapturing.setEnabled(true);
				this.protocolTypesCombo.setEnabled(false);
				this.inputFileButton.setEnabled(false);
			}
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * find position of a string value in an array
	 * @param valueArray
	 * @param value
	 * @param defaultPosition
	 * @return
	 */
	@SuppressWarnings("unused")
	private int findPosition(final String[] valueArray, final String value, final int defaultPosition) {
		for (int i = 0; i < valueArray.length; i++) {
			if (value.equals(valueArray[i])) {
				return i;
			}
		}
		return defaultPosition;
	}
	/**
	 * implementation of noop method from base dialog class
	 */
	@Override
	public void enableSaveButton(boolean enable) {
		this.saveButton.setEnabled(enable);
	}

	/**
	 * @return the tabFolder selection index
	 */
	public Integer getTabFolderSelectionIndex() {
		return this.tabFolder.getSelectionIndex();
	}

	/**
	 * reset the button states
	 */
	public void resetButtons() {
		if (this.dialogShell != null && !this.dialogShell.isDisposed()) {
			this.dialogShell.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					String toolTipText = HoTTAdapter.getImportToolTip();
					HoTTAdapterDialog.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
					if (!HoTTAdapterDialog.this.isDisposed()) {
						HoTTAdapterDialog.this.startLifeDataCapturing.setEnabled(true);
						HoTTAdapterDialog.this.stopLifeDataCapturing.setEnabled(false);
						HoTTAdapterDialog.this.protocolTypesCombo.setEnabled(true);
						HoTTAdapterDialog.this.inputFileButton.setEnabled(true);
					}
				}
			});
		}
	}

	/**
	 * switch to the tab when sensor is detected
	 * @param index
	 */
	public void selectTab(final int index) {
		if (HoTTAdapterDialog.this.tabFolder != null && !HoTTAdapterDialog.this.tabFolder.isDisposed()) {
			this.dialogShell.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					HoTTAdapterDialog.this.tabFolder.setSelection(index - 1);
				}
			});
		}
	}

//	private void setFilterProperties() {
//		this.device.setChannelProperty(ChannelPropertyTypes.ENABLE_FILTER, DataTypes.BOOLEAN, ""+HoTTAdapter.isFilterEnabled);
//		this.device.setMeasurementPropertyValue(3, 1, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value(), DataTypes.BOOLEAN, HoTTAdapter.isTolerateSignChangeLatitude);
//		this.device.setMeasurementPropertyValue(3, 2, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value(), DataTypes.BOOLEAN, HoTTAdapter.isTolerateSignChangeLongitude);
//		this.device.setMeasurementPropertyValue(3, 1, MeasurementPropertyTypes.FILTER_FACTOR.value(), DataTypes.INTEGER, HoTTAdapter.latitudeToleranceFactor);
//		this.device.setMeasurementPropertyValue(3, 2, MeasurementPropertyTypes.FILTER_FACTOR.value(), DataTypes.INTEGER, HoTTAdapter.longitudeToleranceFactor);
//	}
}
