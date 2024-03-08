/**************************************************************************************
  	This file is part of DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.skyrc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import gde.GDE;
import gde.config.Settings;
import gde.device.DeviceDialog;
import gde.device.skyrc.MC3000.SlotSettings;
import gde.device.skyrc.MC3000.SystemSettings;
import gde.device.skyrc.ProgramType.SetupData;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

/**
 * Dialog class to enable visualization control
 * @author Winfried Brügmann
 */
public class MC3000Dialog extends DeviceDialog {

	final static Logger					log											= Logger.getLogger(MC3000Dialog.class.getName());
	static final int						NUMBER_PROGRAM_ENTRIES	= 30;
	static final int						SIZE_PROGRAM_NAME				= 35;
	static final String					NEW_PROG_NAME						= "NEW-PROG-NAME";																	//$NON-NLS-1$
	static final String					STRING_FORMAT_02d_s			= "[%02d] - %s";																		//$NON-NLS-1$
	static final String					STRING_FORMAT_02d				= "[%02d] - ";																			//$NON-NLS-1$
	static final String					STRING_35_BLANK					= "                                   ";						//$NON-NLS-1$
	static final String					DEVICE_JAR_NAME					= "SkyRC";																					//$NON-NLS-1$
	static final int						DIALOG_WIDTH						= 355;

	Composite										tabFolder;
	final List<CTabItem>				configurations					= new ArrayList<CTabItem>();
	CTabItem										chargeTabItem;
	Group												slotsViewGroup;
	Clipboard										clipboard;

	int													parameterSelectHeight		= 25;
	int													chargeSelectHeight			= 17 * this.parameterSelectHeight;
	String[]										programmNames						= new String[MC3000Dialog.NUMBER_PROGRAM_ENTRIES];
	int													lastSelectionIndex[]		= { 0, 0, 0, 0 };
	CCombo[]										programmNameCombos			= new CCombo[4];
	CLabel[]										programLables						= new CLabel[4];

	Schema											schema;
	JAXBContext									jc;
	MC3000Type									mc3000Setup;
	static final String					MC3000_XSD							= "MC3000_V01.xsd";																//$NON-NLS-1$

	Button											saveButton, helpButton, resetAllButton, closeButton;
	MC3000GathererThread				dataGatherThread;

	final MC3000								device;																																		// get device specific things, get serial port, ...
	final MC3000UsbPort					usbPort;																																		// open/close port execute getData()....
	UsbInterface								usbInterface						= null;
	final Settings							settings;																																	// application configuration settings
	SystemSettings							systemSettings;
	SlotSettings								slotSettings_0, slotSettings_1, slotSettings_2, slotSettings_3;
	SlotSettings								slotInitialSettings_0, slotInitialSettings_1, slotInitialSettings_2, slotInitialSettings_3;
	int													measurementsCount				= 0;
	int													tabFolderSelectionIndex	= 0;
	protected static final int	USB_QUERY_DELAY					= GDE.IS_WINDOWS ? 70 : 160;
	boolean											isConnectedByDialog			= false;
	boolean											isConnectedByButton			= false;

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public MC3000Dialog(Shell parent, MC3000 useDevice) {
		super(parent);
		this.device = useDevice;
		this.usbPort = useDevice.getCommunicationPort();
		this.settings = Settings.getInstance();
		for (int i = 0; i < MC3000Dialog.NUMBER_PROGRAM_ENTRIES; i++) {
			this.programmNames[i] = String.format(MC3000Dialog.STRING_FORMAT_02d_s, i + 1, MC3000Dialog.NEW_PROG_NAME);
		}
	}

	/**
	 * update the memory parameter according to system properties
	 */
	private void updateSystemParameter(final SystemSettings sysSettings) {
		if (sysSettings != null) {
			int actualFirmware = sysSettings.getFirmwareVersionAsInt();
			if (actualFirmware < 104) {
				this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGW3600, new String[] { sysSettings.getFirmwareVersion() }));
			}
			else if (sysSettings.getFirmwareVersionAsInt() == 111) {
				this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGT3650, new String[] { sysSettings.getFirmwareVersion() }));
			}
			//firmware > 1.12
		}
	}

	@Override
	public void open() {
		if (!this.usbPort.isConnected()) {
			try {
				this.jc = JAXBContext.newInstance("gde.device.skyrc"); //$NON-NLS-1$
				this.schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
						new StreamSource(MC3000Dialog.class.getClassLoader().getResourceAsStream("resource/" + MC3000Dialog.MC3000_XSD))); //$NON-NLS-1$

				Unmarshaller unmarshaller = this.jc.createUnmarshaller();
				unmarshaller.setSchema(this.schema);
				this.mc3000Setup = (MC3000Type) unmarshaller.unmarshal(new File(Settings.getApplHomePath() + "/MC3000_Slot_Programs" + GDE.FILE_ENDING_DOT_XML)); //$NON-NLS-1$
				int index = 0;
				for (ProgramType prog : this.mc3000Setup.program) {
					if (!prog.getName().contains(MC3000Dialog.NEW_PROG_NAME))
						this.programmNames[index++] = String.format(MC3000Dialog.STRING_FORMAT_02d_s, index, prog.getName());
					else
						this.programmNames[index++] = prog.getName();
				}
			}
			catch (UnmarshalException e) {
				MC3000Dialog.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
				createMC3000Setup();
			}
			catch (Exception e) {
				if (e.getCause() instanceof FileNotFoundException) {
					createMC3000Setup();
				}
			}

			String busyMessage = readInitialSlotPrograms();
			if (busyMessage != null) {
				this.application.openMessageDialog(busyMessage);
				return;
			}
		}
		else {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGW3601));
			return;
		}
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			MC3000Dialog.log.log(java.util.logging.Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
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
				this.dialogShell.pack();
				this.dialogShell.setSize(MC3000Dialog.DIALOG_WIDTH, (GDE.IS_WINDOWS ? 85 : 75) + this.chargeSelectHeight);
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
					@Override
					public void handleEvent(Event event) {
						switch (event.detail) {
						case SWT.TRAVERSE_ESCAPE:
							MC3000Dialog.this.dialogShell.close();
							event.detail = SWT.TRAVERSE_NONE;
							event.doit = false;
							break;
						}
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent evt) {
						MC3000Dialog.log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (MC3000Dialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { MC3000Dialog.this.device.getPropertiesFileName() });
							if (MC3000Dialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								MC3000Dialog.log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
								MC3000Dialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
						MC3000Dialog.this.clipboard.dispose();
						MC3000Dialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					@Override
					public void helpRequested(HelpEvent evt) {
						MC3000Dialog.log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						MC3000Dialog.this.application.openHelpDialog(MC3000Dialog.DEVICE_JAR_NAME, "HelpInfo.html"); //$NON-NLS-1$
					}
				});
				this.clipboard = new Clipboard(this.dialogShell.getDisplay());
				{
					this.tabFolder = new Composite(this.dialogShell, SWT.NONE);
					this.tabFolder.setLayout(new FillLayout(SWT.VERTICAL));
					this.tabFolder.setSize(this.dialogShell.getClientArea().width, this.chargeSelectHeight);
					this.tabFolder.layout();
					{
						this.slotsViewGroup = new Group(this.tabFolder, SWT.NONE);
						this.slotsViewGroup.setLayout(new FillLayout(SWT.VERTICAL));
						for (int i = 0; i < 4; i++) {
							createSlotGroup(i);
						}
					}
					{
						this.saveButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData saveButtonLData = new FormData();
						saveButtonLData.width = 90;
						saveButtonLData.height = GDE.IS_MAC ? 33 : 30;
						saveButtonLData.bottom = new FormAttachment(1000, 1000, -10);
						saveButtonLData.left = new FormAttachment(0, 1000, 10);
						this.saveButton.setLayoutData(saveButtonLData);
						this.saveButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.saveButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0486));
						this.saveButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3655));
						this.saveButton.setEnabled(false);
						this.saveButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								MC3000Dialog.log.log(java.util.logging.Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								saveMc3000SetupData();
							}
						});
					}
					{
						this.helpButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						this.helpButton.setImage(SWTResourceManager.getImage("gde/resource/QuestionHot.gif")); //$NON-NLS-1$
						FormData LoadButtonLData = new FormData();
						LoadButtonLData.width = 40;
						LoadButtonLData.height = GDE.IS_MAC ? 33 : 30;
						LoadButtonLData.bottom = new FormAttachment(1000, 1000, -10);
						LoadButtonLData.left = new FormAttachment(0, 1000, 107);
						this.helpButton.setLayoutData(LoadButtonLData);
						this.helpButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								MC3000Dialog.log.log(java.util.logging.Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								MC3000Dialog.this.application.openHelpDialog(MC3000Dialog.DEVICE_JAR_NAME, "HelpInfo.html"); //$NON-NLS-1$
							}
						});
					}
					{
						this.resetAllButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData reloadButtonLData = new FormData();
						reloadButtonLData.width = 90;
						reloadButtonLData.height = GDE.IS_MAC ? 33 : 30;
						reloadButtonLData.right = new FormAttachment(1000, 1000, -110);
						reloadButtonLData.bottom = new FormAttachment(1000, 1000, -10);
						this.resetAllButton.setLayoutData(reloadButtonLData);
						this.resetAllButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.resetAllButton.setText(Messages.getString(MessageIds.GDE_MSGT3679));
						this.resetAllButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3680));
						this.resetAllButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								MC3000Dialog.log.log(java.util.logging.Level.FINEST, "reloadButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								try {
									if (!MC3000Dialog.this.usbPort.isConnected()) {
										MC3000Dialog.this.usbInterface = MC3000Dialog.this.usbPort.openUsbPort(MC3000Dialog.this.device);
										MC3000Dialog.this.isConnectedByButton = true;
									}
									int firmware = MC3000Dialog.this.systemSettings != null ? MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt() : 0;
									byte systemTemperatureUnit = MC3000Dialog.this.systemSettings != null ? MC3000Dialog.this.systemSettings.getTemperatureUnit() : 0;
									byte[] tmpProgramBuffer;
									for (int i = 0; i < 4; i++) {
										switch (i) {
										case 0:
											//programLables[i].setText(MC3000Dialog.this.slotInitialSettings_0 != null ? MC3000Dialog.this.slotInitialSettings_0.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
											//programLables[i].setToolTipText(MC3000Dialog.this.slotInitialSettings_0 != null ? MC3000Dialog.this.slotInitialSettings_0.toString4Tip()
											//		: Messages.getString(MessageIds.GDE_MSGT3654));
											if (MC3000Dialog.this.slotInitialSettings_0 != null) {
												tmpProgramBuffer = MC3000Dialog.this.slotInitialSettings_0.getBuffer((byte) 0, firmware, systemTemperatureUnit);
												MC3000Dialog.this.usbPort.setSlotProgram(MC3000Dialog.this.usbInterface, tmpProgramBuffer);
												MC3000Dialog.this.slotSettings_0 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface,
														MC3000UsbPort.QuerySlotData.SLOT_0.value()), firmware);
												MC3000Dialog.this.programLables[i].setText(MC3000Dialog.this.slotSettings_0 != null ? MC3000Dialog.this.slotSettings_0.toString4View() : Messages
														.getString(MessageIds.GDE_MSGT3654));
												MC3000Dialog.this.programLables[i].setToolTipText(MC3000Dialog.this.slotSettings_0 != null ? MC3000Dialog.this.slotSettings_0.toString4Tip(true, systemTemperatureUnit) : Messages
														.getString(MessageIds.GDE_MSGT3654));
											}
											break;
										case 1:
											MC3000Dialog.this.programLables[i].setText(MC3000Dialog.this.slotInitialSettings_1 != null ? MC3000Dialog.this.slotInitialSettings_1.toString4View() : Messages
													.getString(MessageIds.GDE_MSGT3654));
											MC3000Dialog.this.programLables[i].setToolTipText(MC3000Dialog.this.slotInitialSettings_1 != null ? MC3000Dialog.this.slotInitialSettings_1.toString4Tip(true, systemTemperatureUnit) : Messages
													.getString(MessageIds.GDE_MSGT3654));
											if (MC3000Dialog.this.slotInitialSettings_1 != null) {
												tmpProgramBuffer = MC3000Dialog.this.slotInitialSettings_1.getBuffer((byte) 1, firmware, systemTemperatureUnit);
												MC3000Dialog.this.usbPort.setSlotProgram(MC3000Dialog.this.usbInterface, tmpProgramBuffer);
												MC3000Dialog.this.slotSettings_1 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface,
														MC3000UsbPort.QuerySlotData.SLOT_1.value()), firmware);
												MC3000Dialog.this.programLables[i].setText(MC3000Dialog.this.slotSettings_1 != null ? MC3000Dialog.this.slotSettings_1.toString4View() : Messages
														.getString(MessageIds.GDE_MSGT3654));
												MC3000Dialog.this.programLables[i].setToolTipText(MC3000Dialog.this.slotSettings_1 != null ? MC3000Dialog.this.slotSettings_1.toString4Tip(true, systemTemperatureUnit) : Messages
														.getString(MessageIds.GDE_MSGT3654));
											}
											break;
										case 2:
											MC3000Dialog.this.programLables[i].setText(MC3000Dialog.this.slotInitialSettings_2 != null ? MC3000Dialog.this.slotInitialSettings_2.toString4View() : Messages
													.getString(MessageIds.GDE_MSGT3654));
											MC3000Dialog.this.programLables[i].setToolTipText(MC3000Dialog.this.slotInitialSettings_2 != null ? MC3000Dialog.this.slotInitialSettings_2.toString4Tip(true, systemTemperatureUnit) : Messages
													.getString(MessageIds.GDE_MSGT3654));
											if (MC3000Dialog.this.slotInitialSettings_2 != null) {
												tmpProgramBuffer = MC3000Dialog.this.slotInitialSettings_2.getBuffer((byte) 2, firmware, systemTemperatureUnit);
												MC3000Dialog.this.usbPort.setSlotProgram(MC3000Dialog.this.usbInterface, tmpProgramBuffer);
												MC3000Dialog.this.slotSettings_2 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface,
														MC3000UsbPort.QuerySlotData.SLOT_2.value()), firmware);
												MC3000Dialog.this.programLables[i].setText(MC3000Dialog.this.slotSettings_2 != null ? MC3000Dialog.this.slotSettings_2.toString4View() : Messages
														.getString(MessageIds.GDE_MSGT3654));
												MC3000Dialog.this.programLables[i].setToolTipText(MC3000Dialog.this.slotSettings_2 != null ? MC3000Dialog.this.slotSettings_2.toString4Tip(true, systemTemperatureUnit) : Messages
														.getString(MessageIds.GDE_MSGT3654));
											}
											break;
										case 3:
											MC3000Dialog.this.programLables[i].setText(MC3000Dialog.this.slotInitialSettings_3 != null ? MC3000Dialog.this.slotInitialSettings_3.toString4View() : Messages
													.getString(MessageIds.GDE_MSGT3654));
											MC3000Dialog.this.programLables[i].setToolTipText(MC3000Dialog.this.slotInitialSettings_3 != null ? MC3000Dialog.this.slotInitialSettings_3.toString4Tip(true, systemTemperatureUnit) : Messages
													.getString(MessageIds.GDE_MSGT3654));
											if (MC3000Dialog.this.slotInitialSettings_3 != null) {
												tmpProgramBuffer = MC3000Dialog.this.slotInitialSettings_3.getBuffer((byte) 3, firmware, systemTemperatureUnit);
												MC3000Dialog.this.usbPort.setSlotProgram(MC3000Dialog.this.usbInterface, tmpProgramBuffer);
												MC3000Dialog.this.slotSettings_3 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface,
														MC3000UsbPort.QuerySlotData.SLOT_3.value()), firmware);
												MC3000Dialog.this.programLables[i].setText(MC3000Dialog.this.slotSettings_3 != null ? MC3000Dialog.this.slotSettings_3.toString4View() : Messages
														.getString(MessageIds.GDE_MSGT3654));
												MC3000Dialog.this.programLables[i].setToolTipText(MC3000Dialog.this.slotSettings_3 != null ? MC3000Dialog.this.slotSettings_3.toString4Tip(true, systemTemperatureUnit) : Messages
														.getString(MessageIds.GDE_MSGT3654));
											}
											break;

										default:
											break;
										}
									}
								}
								catch (Exception e) {
									MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
									MC3000Dialog.this.application.openMessageDialogAsync(e.getMessage());
								}
								finally {
									if (MC3000Dialog.this.isConnectedByButton && MC3000Dialog.this.usbPort != null && MC3000Dialog.this.usbPort.isConnected()) {
										try {
											MC3000Dialog.this.usbPort.closeUsbPort(MC3000Dialog.this.usbInterface != null ? MC3000Dialog.this.usbInterface : null);
										}
										catch (UsbException e) {
											MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
										}
									}
								}
							}
						});
					}
					{
						this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData closeButtonLData = new FormData();
						closeButtonLData.width = 90;
						closeButtonLData.height = GDE.IS_MAC ? 33 : 30;
						closeButtonLData.right = new FormAttachment(1000, 1000, -10);
						closeButtonLData.bottom = new FormAttachment(1000, 1000, -10);
						this.closeButton.setLayoutData(closeButtonLData);
						this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0485));
						this.closeButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								MC3000Dialog.log.log(java.util.logging.Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (!MC3000Dialog.this.saveButton.isEnabled()
										|| (MC3000Dialog.this.saveButton.isEnabled() && SWT.OK == MC3000Dialog.this.application.openOkCancelMessageDialog(MC3000Dialog.this.dialogShell,
												Messages.getString(MessageIds.GDE_MSGT3681)))) {
									MC3000Dialog.this.dialogShell.dispose();
								}
							}
						});
					}
					FormData tabFolderLData = new FormData();
					tabFolderLData.top = new FormAttachment(0, 1000, 0);
					tabFolderLData.left = new FormAttachment(0, 1000, 0);
					tabFolderLData.right = new FormAttachment(1000, 1000, 0);
					tabFolderLData.bottom = new FormAttachment(1000, 1000, -50);
					//this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tabFolder.setLayoutData(tabFolderLData);
				}

				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 175, 100));
				this.dialogShell.open();
				//this.checkPortStatus();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}
			updateSystemParameter(this.systemSettings);

			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			MC3000Dialog.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
	}

	private String readInitialSlotPrograms() {
		boolean isBusy = false;
		String isBusyString = null;
		try {
			if (!this.usbPort.isConnected()) {
				this.usbInterface = this.usbPort.openUsbPort(this.device);
				this.isConnectedByDialog = true;
			}

			if (this.usbPort.isConnected()) this.systemSettings = this.device.new SystemSettings(this.usbPort.getSystemSettings(this.usbInterface));
			WaitTimer.delay(MC3000Dialog.USB_QUERY_DELAY);
			if (this.usbPort.isConnected()) {
				this.slotSettings_0 = this.device.new SlotSettings(this.usbPort.getSlotData(this.usbInterface, MC3000UsbPort.QuerySlotData.SLOT_0.value()), this.systemSettings.getFirmwareVersionAsInt());
				this.slotInitialSettings_0 = this.device.new SlotSettings(this.slotSettings_0);
				WaitTimer.delay(MC3000Dialog.USB_QUERY_DELAY);
				isBusy = this.slotInitialSettings_0.isBusy();
				isBusyString = isBusy ? Messages.getString(MessageIds.GDE_MSGW3601) : null;
			}
			if (this.usbPort.isConnected() && !isBusy) {
				this.slotSettings_1 = this.device.new SlotSettings(this.usbPort.getSlotData(this.usbInterface, MC3000UsbPort.QuerySlotData.SLOT_1.value()), this.systemSettings.getFirmwareVersionAsInt());
				this.slotInitialSettings_1 = this.device.new SlotSettings(this.slotSettings_1);
				WaitTimer.delay(MC3000Dialog.USB_QUERY_DELAY);
				isBusy = this.slotInitialSettings_0.isBusy();
				isBusyString = isBusy ? Messages.getString(MessageIds.GDE_MSGW3601) : null;
			}
			if (this.usbPort.isConnected() && !isBusy) {
				this.slotSettings_2 = this.device.new SlotSettings(this.usbPort.getSlotData(this.usbInterface, MC3000UsbPort.QuerySlotData.SLOT_2.value()), this.systemSettings.getFirmwareVersionAsInt());
				this.slotInitialSettings_2 = this.device.new SlotSettings(this.slotSettings_2);
				WaitTimer.delay(MC3000Dialog.USB_QUERY_DELAY);
				isBusy = this.slotInitialSettings_0.isBusy();
				isBusyString = isBusy ? Messages.getString(MessageIds.GDE_MSGW3601) : null;
			}
			if (this.usbPort.isConnected() && !isBusy) {
				this.slotSettings_3 = this.device.new SlotSettings(this.usbPort.getSlotData(this.usbInterface, MC3000UsbPort.QuerySlotData.SLOT_3.value()), this.systemSettings.getFirmwareVersionAsInt());
				this.slotInitialSettings_3 = this.device.new SlotSettings(this.slotSettings_3);
				isBusy = this.slotInitialSettings_0.isBusy();
				isBusyString = isBusy ? Messages.getString(MessageIds.GDE_MSGW3601) : null;
			}
		}
		catch (Exception e) {
			MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			if (e instanceof UsbException)
				isBusyString = e.getMessage();
			else
				isBusyString = Messages.getString(MessageIds.GDE_MSGW3601);
		}
		finally {
			if (this.isConnectedByDialog && this.usbPort != null && this.usbPort.isConnected()) {
				try {
					this.usbPort.closeUsbPort(this.usbInterface != null ? this.usbInterface : null);
				}
				catch (UsbException e) {
					MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
				}
			}
		}
		return isBusyString;
	}

	private void createSlotGroup(final int index) {
		Group slot = new Group(this.slotsViewGroup, SWT.NONE);
		slot.setLayout(new RowLayout(SWT.HORIZONTAL));
		slot.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		slot.setText(Messages.getString(MessageIds.GDE_MSGT3651, new Object[] { index + 1 }));
		CLabel nameLabel = new CLabel(slot, SWT.None);
		nameLabel.setLayoutData(new RowData(GDE.IS_WINDOWS ? 115 : 120, 20));
		nameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		nameLabel.setText(Messages.getString(MessageIds.GDE_MSGT3652));
		final CCombo programmNameCombo = new CCombo(slot, SWT.BORDER);
		this.programmNameCombos[index] = programmNameCombo;
		final CLabel label0 = new CLabel(slot, SWT.None);
		final Button writeButton = new Button(slot, SWT.PUSH | SWT.CENTER);
		this.programLables[index] = label0;
		programmNameCombo.setItems(this.programmNames);
		programmNameCombo.setVisibleItemCount(20);
		programmNameCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		programmNameCombo.setTextLimit(5 + MC3000Dialog.SIZE_PROGRAM_NAME);
		programmNameCombo.setLayoutData(new RowData(187, GDE.IS_WINDOWS ? 16 : 18));
		programmNameCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3653));
		programmNameCombo.select(0);
		programmNameCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		programmNameCombo.setEditable(true);
		programmNameCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				MC3000Dialog.log.log(java.util.logging.Level.FINEST, "programmNameCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
				MC3000Dialog.this.lastSelectionIndex[index] = programmNameCombo.getSelectionIndex();
				//load saved values to slot if selected program name does not contain "NEW-PROG-NAME"
				if (MC3000Dialog.this.mc3000Setup == null) createMC3000Setup();
				List<ProgramType> devicePrograms = MC3000Dialog.this.mc3000Setup.getProgram();
				if (!devicePrograms.get(programmNameCombo.getSelectionIndex()).getName().contains(MC3000Dialog.NEW_PROG_NAME)) {
					try {
						byte systemTemperatureUnit = MC3000Dialog.this.systemSettings != null ? MC3000Dialog.this.systemSettings.getTemperatureUnit() : 0;
						switch (index) {
						case 0:
							MC3000Dialog.this.slotSettings_0 = MC3000Dialog.this.device.new SlotSettings(devicePrograms.get(programmNameCombo.getSelectionIndex()).getSetupData().getValue(),
									MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt());
							MC3000Dialog.this.slotSettings_0.setSlotNumber((byte) index);
							label0.setText(MC3000Dialog.this.slotSettings_0.toString4View());
							label0.setToolTipText(MC3000Dialog.this.slotSettings_0.toString4Tip(true, systemTemperatureUnit));
							break;
						case 1:
							MC3000Dialog.this.slotSettings_1 = MC3000Dialog.this.device.new SlotSettings(devicePrograms.get(programmNameCombo.getSelectionIndex()).getSetupData().getValue(),
									MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt());
							MC3000Dialog.this.slotSettings_1.setSlotNumber((byte) index);
							label0.setText(MC3000Dialog.this.slotSettings_1.toString4View());
							label0.setToolTipText(MC3000Dialog.this.slotSettings_1.toString4Tip(true, systemTemperatureUnit));
							break;
						case 2:
							MC3000Dialog.this.slotSettings_2 = MC3000Dialog.this.device.new SlotSettings(devicePrograms.get(programmNameCombo.getSelectionIndex()).getSetupData().getValue(),
									MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt());
							MC3000Dialog.this.slotSettings_2.setSlotNumber((byte) index);
							label0.setText(MC3000Dialog.this.slotSettings_2.toString4View());
							label0.setToolTipText(MC3000Dialog.this.slotSettings_2.toString4Tip(true, systemTemperatureUnit));
							break;
						case 3:
							MC3000Dialog.this.slotSettings_3 = MC3000Dialog.this.device.new SlotSettings(devicePrograms.get(programmNameCombo.getSelectionIndex()).getSetupData().getValue(),
									MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt());
							MC3000Dialog.this.slotSettings_3.setSlotNumber((byte) index);
							label0.setText(MC3000Dialog.this.slotSettings_3.toString4View());
							label0.setToolTipText(MC3000Dialog.this.slotSettings_3.toString4Tip(true, systemTemperatureUnit));
							break;

						default:
							break;
						}
						if (programmNameCombo.getText().contains(MC3000Dialog.NEW_PROG_NAME))
							writeButton.setEnabled(false);
						else
							writeButton.setEnabled(true);
					}
					catch (Exception e) {
						// ignore checksum error detected
					}
				}
			}
		});
		programmNameCombo.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent evt) {
				MC3000Dialog.log.log(java.util.logging.Level.FINEST, "memoryCombo.keyReleased, event=" + evt); //$NON-NLS-1$
			}

			@Override
			public void keyPressed(KeyEvent evt) {
				MC3000Dialog.log.log(java.util.logging.Level.FINEST, "memoryCombo.keyPressed, event=" + evt); //$NON-NLS-1$
				if (evt.character == SWT.CR) {
					try {
						if (MC3000Dialog.this.mc3000Setup == null) createMC3000Setup();
						MC3000Dialog.log.log(java.util.logging.Level.FINE, String.format("slot # %d selection index = %d", index, MC3000Dialog.this.lastSelectionIndex[index])); //$NON-NLS-1$
						String nameLeader = String.format(MC3000Dialog.STRING_FORMAT_02d, MC3000Dialog.this.lastSelectionIndex[index] + 1);
						String tmpName = programmNameCombo.getText() + MC3000Dialog.STRING_35_BLANK;
						tmpName = tmpName.startsWith(nameLeader) ? tmpName : (nameLeader + tmpName);
						String newSlotProgramName = String.format(MC3000Dialog.STRING_FORMAT_02d_s, MC3000Dialog.this.lastSelectionIndex[index] + 1, tmpName.substring(7, MC3000Dialog.SIZE_PROGRAM_NAME + 5)
								.trim());
						MC3000Dialog.this.programmNames[MC3000Dialog.this.lastSelectionIndex[index]] = newSlotProgramName;
						programmNameCombo.setText(newSlotProgramName);
						programmNameCombo.setItem(MC3000Dialog.this.lastSelectionIndex[index], newSlotProgramName);

						MC3000Dialog.this.mc3000Setup.getProgram().get(MC3000Dialog.this.lastSelectionIndex[index])
								.setName(MC3000Dialog.this.programmNames[MC3000Dialog.this.lastSelectionIndex[index]].substring(7).trim());
						switch (index) {
						case 0:
							MC3000Dialog.this.mc3000Setup.getProgram().get(MC3000Dialog.this.lastSelectionIndex[index]).getSetupData().setValue(MC3000Dialog.this.slotSettings_0.getBuffer());
							break;
						case 1:
							MC3000Dialog.this.mc3000Setup.getProgram().get(MC3000Dialog.this.lastSelectionIndex[index]).getSetupData().setValue(MC3000Dialog.this.slotSettings_1.getBuffer());
							break;
						case 2:
							MC3000Dialog.this.mc3000Setup.getProgram().get(MC3000Dialog.this.lastSelectionIndex[index]).getSetupData().setValue(MC3000Dialog.this.slotSettings_2.getBuffer());
							break;
						case 3:
							MC3000Dialog.this.mc3000Setup.getProgram().get(MC3000Dialog.this.lastSelectionIndex[index]).getSetupData().setValue(MC3000Dialog.this.slotSettings_3.getBuffer());
							break;

						default:
							break;
						}
						for (CCombo combo : MC3000Dialog.this.programmNameCombos) {
							combo.setItem(MC3000Dialog.this.lastSelectionIndex[index], newSlotProgramName);
							if (combo.getSelectionIndex() == MC3000Dialog.this.lastSelectionIndex[index]) combo.setText(newSlotProgramName);
						}
						MC3000Dialog.this.saveButton.setEnabled(true);
					}
					catch (Exception e) {
						MC3000Dialog.this.application.openMessageDialog(MC3000Dialog.this.dialogShell, Messages.getString(gde.messages.MessageIds.GDE_MSGE0007, new String[] { e.getMessage() }));
					}
				}
			}
		});
		label0.setLayoutData(new RowData(350, 25));
		label0.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		byte sysTemperatureUnit = MC3000Dialog.this.systemSettings != null ? MC3000Dialog.this.systemSettings.getTemperatureUnit() : 0;
		switch (index) {
		case 0:
			label0.setText(this.slotSettings_0 != null ? this.slotSettings_0.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
			label0.setToolTipText(this.slotSettings_0 != null ? this.slotSettings_0.toString4Tip(true, sysTemperatureUnit) : Messages.getString(MessageIds.GDE_MSGT3654));
			break;
		case 1:
			label0.setText(this.slotSettings_1 != null ? this.slotSettings_1.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
			label0.setToolTipText(this.slotSettings_1 != null ? this.slotSettings_1.toString4Tip(true, sysTemperatureUnit) : Messages.getString(MessageIds.GDE_MSGT3654));
			break;
		case 2:
			label0.setText(this.slotSettings_2 != null ? this.slotSettings_2.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
			label0.setToolTipText(this.slotSettings_2 != null ? this.slotSettings_2.toString4Tip(true, sysTemperatureUnit) : Messages.getString(MessageIds.GDE_MSGT3654));
			break;
		case 3:
			label0.setText(this.slotSettings_3 != null ? this.slotSettings_3.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
			label0.setToolTipText(this.slotSettings_3 != null ? this.slotSettings_3.toString4Tip(true, sysTemperatureUnit) : Messages.getString(MessageIds.GDE_MSGT3654));
			break;

		default:
			break;
		}
		writeButton.setLayoutData(new RowData(110, 22));
		writeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		writeButton.setText(Messages.getString(MessageIds.GDE_MSGT3656));
		writeButton.setEnabled(false);
		writeButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3657));
		writeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				MC3000Dialog.log.log(java.util.logging.Level.FINEST, "writeButton.widgetSelected, event=" + evt); //$NON-NLS-1$);
				try {
					if (!MC3000Dialog.this.usbPort.isConnected()) {
						MC3000Dialog.this.usbInterface = MC3000Dialog.this.usbPort.openUsbPort(MC3000Dialog.this.device);
						MC3000Dialog.this.isConnectedByButton = true;
					}
					int firmware = MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt();
					byte systemTemperatureUnit = MC3000Dialog.this.systemSettings != null ? MC3000Dialog.this.systemSettings.getTemperatureUnit() : 0;
					byte[] newProgramBuffer;
					switch (index) {
					case 0:
						newProgramBuffer = MC3000Dialog.this.slotSettings_0.getBuffer((byte) 0, firmware, systemTemperatureUnit);
						MC3000Dialog.this.usbPort.setSlotProgram(MC3000Dialog.this.usbInterface, newProgramBuffer);
						MC3000Dialog.this.slotSettings_0 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface,
								MC3000UsbPort.QuerySlotData.SLOT_0.value()), firmware);
						MC3000Dialog.this.programLables[index].setText(MC3000Dialog.this.slotSettings_0 != null ? MC3000Dialog.this.slotSettings_0.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
						MC3000Dialog.this.programLables[index].setToolTipText(MC3000Dialog.this.slotSettings_0 != null ? MC3000Dialog.this.slotSettings_0.toString4Tip(true, systemTemperatureUnit) : Messages
								.getString(MessageIds.GDE_MSGT3654));
						break;
					case 1:
						newProgramBuffer = MC3000Dialog.this.slotSettings_1.getBuffer((byte) 1, firmware, systemTemperatureUnit);
						MC3000Dialog.this.usbPort.setSlotProgram(MC3000Dialog.this.usbInterface, newProgramBuffer);
						MC3000Dialog.this.slotSettings_1 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface,
								MC3000UsbPort.QuerySlotData.SLOT_1.value()), firmware);
						MC3000Dialog.this.programLables[index].setText(MC3000Dialog.this.slotSettings_1 != null ? MC3000Dialog.this.slotSettings_1.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
						MC3000Dialog.this.programLables[index].setToolTipText(MC3000Dialog.this.slotSettings_1 != null ? MC3000Dialog.this.slotSettings_1.toString4Tip(true, systemTemperatureUnit) : Messages
								.getString(MessageIds.GDE_MSGT3654));
						break;
					case 2:
						newProgramBuffer = MC3000Dialog.this.slotSettings_2.getBuffer((byte) 2, firmware, systemTemperatureUnit);
						MC3000Dialog.this.usbPort.setSlotProgram(MC3000Dialog.this.usbInterface, newProgramBuffer);
						MC3000Dialog.this.slotSettings_2 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface,
								MC3000UsbPort.QuerySlotData.SLOT_2.value()), firmware);
						MC3000Dialog.this.programLables[index].setText(MC3000Dialog.this.slotSettings_2 != null ? MC3000Dialog.this.slotSettings_2.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
						MC3000Dialog.this.programLables[index].setToolTipText(MC3000Dialog.this.slotSettings_2 != null ? MC3000Dialog.this.slotSettings_2.toString4Tip(true, systemTemperatureUnit) : Messages
								.getString(MessageIds.GDE_MSGT3654));
						break;
					case 3:
						newProgramBuffer = MC3000Dialog.this.slotSettings_3.getBuffer((byte) 3, firmware, systemTemperatureUnit);
						MC3000Dialog.this.usbPort.setSlotProgram(MC3000Dialog.this.usbInterface, newProgramBuffer);
						MC3000Dialog.this.slotSettings_3 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface,
								MC3000UsbPort.QuerySlotData.SLOT_3.value()), firmware);
						MC3000Dialog.this.programLables[index].setText(MC3000Dialog.this.slotSettings_3 != null ? MC3000Dialog.this.slotSettings_3.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
						MC3000Dialog.this.programLables[index].setToolTipText(MC3000Dialog.this.slotSettings_3 != null ? MC3000Dialog.this.slotSettings_3.toString4Tip(true, systemTemperatureUnit) : Messages
								.getString(MessageIds.GDE_MSGT3654));
						break;

					default:
						break;
					}
				}
				catch (Exception e) {
					MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
					MC3000Dialog.this.application.openMessageDialogAsync(e.getMessage());
				}
				finally {
					writeButton.setEnabled(false);
					if (MC3000Dialog.this.isConnectedByButton && MC3000Dialog.this.usbPort != null && MC3000Dialog.this.usbPort.isConnected()) {
						try {
							MC3000Dialog.this.usbPort.closeUsbPort(MC3000Dialog.this.usbInterface != null ? MC3000Dialog.this.usbInterface : null);
						}
						catch (UsbException e) {
							MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
						}
					}
				}
			}
		});
		Button copyClipBoardButton = new Button(slot, SWT.PUSH | SWT.CENTER);
		copyClipBoardButton.setLayoutData(new RowData(90, 22));
		copyClipBoardButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		copyClipBoardButton.setText(Messages.getString(MessageIds.GDE_MSGT3677));
		copyClipBoardButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3678));
		copyClipBoardButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				byte systemTemperatureUnit = MC3000Dialog.this.systemSettings != null ? MC3000Dialog.this.systemSettings.getTemperatureUnit() : 0;
				String data = new String();
				switch (index) {
				case 0:
					data = MC3000Dialog.this.slotSettings_0 != null ? MC3000Dialog.this.slotSettings_0.toString4Tip(false, systemTemperatureUnit) : Messages.getString(MessageIds.GDE_MSGT3654);
					break;
				case 1:
					data = MC3000Dialog.this.slotSettings_1 != null ? MC3000Dialog.this.slotSettings_1.toString4Tip(false, systemTemperatureUnit) : Messages.getString(MessageIds.GDE_MSGT3654);
					break;
				case 2:
					data = MC3000Dialog.this.slotSettings_2 != null ? MC3000Dialog.this.slotSettings_2.toString4Tip(false, systemTemperatureUnit) : Messages.getString(MessageIds.GDE_MSGT3654);
					break;
				case 3:
					data = MC3000Dialog.this.slotSettings_3 != null ? MC3000Dialog.this.slotSettings_3.toString4Tip(false, systemTemperatureUnit) : Messages.getString(MessageIds.GDE_MSGT3654);
					break;
				default:
					break;
				}
				if (data.length() > 0) {
					MC3000Dialog.this.clipboard.setContents(new Object[] { data }, new Transfer[] { TextTransfer.getInstance() });
				}
			}
		});
		Button loadButton = new Button(slot, SWT.PUSH | SWT.CENTER);
		loadButton.setLayoutData(new RowData(110, 22));
		loadButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		loadButton.setText(Messages.getString(MessageIds.GDE_MSGT3658));
		loadButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3659));
		loadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				MC3000Dialog.log.log(java.util.logging.Level.FINEST, "loadButton.widgetSelected, event=" + evt); //$NON-NLS-1$);
				try {
					if (!MC3000Dialog.this.usbPort.isConnected()) {
						MC3000Dialog.this.usbInterface = MC3000Dialog.this.usbPort.openUsbPort(MC3000Dialog.this.device);
						MC3000Dialog.this.isConnectedByButton = true;
					}

					byte systemTemperatureUnit = MC3000Dialog.this.systemSettings != null ? MC3000Dialog.this.systemSettings.getTemperatureUnit() : 0;
					switch (index) {
					case 0:
						if (MC3000Dialog.this.usbPort.isConnected())
							MC3000Dialog.this.slotSettings_0 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface,
									MC3000UsbPort.QuerySlotData.SLOT_0.value()), MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt());
						label0.setText(MC3000Dialog.this.slotSettings_0 != null ? MC3000Dialog.this.slotSettings_0.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
						label0.setToolTipText(MC3000Dialog.this.slotSettings_0 != null ? MC3000Dialog.this.slotSettings_0.toString4Tip(true, systemTemperatureUnit) : Messages.getString(MessageIds.GDE_MSGT3654));
						break;
					case 1:
						if (MC3000Dialog.this.usbPort.isConnected())
							MC3000Dialog.this.slotSettings_1 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface,
									MC3000UsbPort.QuerySlotData.SLOT_1.value()), MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt());
						label0.setText(MC3000Dialog.this.slotSettings_1 != null ? MC3000Dialog.this.slotSettings_1.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
						label0.setToolTipText(MC3000Dialog.this.slotSettings_1 != null ? MC3000Dialog.this.slotSettings_1.toString4Tip(true, systemTemperatureUnit) : Messages.getString(MessageIds.GDE_MSGT3654));
						break;
					case 2:
						if (MC3000Dialog.this.usbPort.isConnected())
							MC3000Dialog.this.slotSettings_2 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface,
									MC3000UsbPort.QuerySlotData.SLOT_2.value()), MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt());
						label0.setText(MC3000Dialog.this.slotSettings_2 != null ? MC3000Dialog.this.slotSettings_2.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
						label0.setToolTipText(MC3000Dialog.this.slotSettings_2 != null ? MC3000Dialog.this.slotSettings_2.toString4Tip(true, systemTemperatureUnit) : Messages.getString(MessageIds.GDE_MSGT3654));
						break;
					case 3:
						if (MC3000Dialog.this.usbPort.isConnected())
							MC3000Dialog.this.slotSettings_3 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface,
									MC3000UsbPort.QuerySlotData.SLOT_3.value()), MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt());
						label0.setText(MC3000Dialog.this.slotSettings_3 != null ? MC3000Dialog.this.slotSettings_3.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
						label0.setToolTipText(MC3000Dialog.this.slotSettings_3 != null ? MC3000Dialog.this.slotSettings_3.toString4Tip(true, systemTemperatureUnit) : Messages.getString(MessageIds.GDE_MSGT3654));
						break;

					default:
						break;
					}
				}
				catch (Exception e) {
					MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
					MC3000Dialog.this.application.openMessageDialogAsync(e.getMessage());
				}
				finally {
					if (MC3000Dialog.this.isConnectedByButton && MC3000Dialog.this.usbPort != null && MC3000Dialog.this.usbPort.isConnected()) {
						try {
							MC3000Dialog.this.usbPort.closeUsbPort(MC3000Dialog.this.usbInterface != null ? MC3000Dialog.this.usbInterface : null);
						}
						catch (UsbException e) {
							MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
						}
					}
				}
			}
		});
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
		return this.tabFolderSelectionIndex;
	}

	//	/**
	//	 * toggle the text of start configuration button according comm port state
	//	 */
	//	void checkPortStatus() {
	//		GDE.display.asyncExec(new Runnable() {
	//			public void run() {
	//				if (!MC3000Dialog.this.startConfiguration.isDisposed()) {
	//					if (MC3000Dialog.this.device.usbPort.isConnected()) {
	//						MC3000Dialog.this.startConfiguration.setText("stop configurartion");
	//					}
	//					else {
	//						MC3000Dialog.this.startConfiguration.setText("start configurartion");
	//					}
	//				}
	//			}
	//		});
	//	}

	/**
	 * create minimal MC3000 XML data
	 */
	private void createMC3000Setup() {
		this.mc3000Setup = new ObjectFactory().createMC3000Type();
		List<ProgramType> devicePrograms = this.mc3000Setup.getProgram();
		if (devicePrograms.size() < MC3000Dialog.NUMBER_PROGRAM_ENTRIES) { // initially create only base setup data
			for (int i = 0; i < MC3000Dialog.NUMBER_PROGRAM_ENTRIES; i++) {
				ProgramType program = new ObjectFactory().createProgramType();
				SetupData setupData = new ObjectFactory().createProgramTypeSetupData();
				setupData.setValue(new byte[64]);
				program.setSetupData(setupData);
				program.setName(this.programmNames[i]);
				devicePrograms.add(program);
			}
		}
		saveMc3000SetupData();
	}

	private void saveMc3000SetupData() {
		try {
			// store back manipulated XML
			long startTime = new Date().getTime();
			Marshaller marshaller = this.jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.valueOf(true));
			marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, MC3000Dialog.MC3000_XSD);
			marshaller.marshal(this.mc3000Setup, new FileOutputStream(new File(Settings.getApplHomePath() + "/MC3000_Slot_Programs" + GDE.FILE_ENDING_DOT_XML))); //$NON-NLS-1$
			MC3000Dialog.log.log(Level.TIME, "write program setup XML time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.saveButton != null && !this.saveButton.isDisposed()) this.saveButton.setEnabled(false);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}
