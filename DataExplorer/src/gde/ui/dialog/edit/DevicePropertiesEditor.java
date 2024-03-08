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
package gde.ui.dialog.edit;

import java.io.File;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.xml.sax.SAXParseException;

import gde.GDE;
import gde.config.Settings;
import gde.device.CheckSumTypes;
import gde.device.CommaSeparatorTypes;
import gde.device.DataBlockType;
import gde.device.DataTypes;
import gde.device.DesktopPropertyTypes;
import gde.device.DeviceConfiguration;
import gde.device.DeviceTypes;
import gde.device.FormatTypes;
import gde.device.InputTypes;
import gde.device.LineEndingTypes;
import gde.device.MeasurementPropertyTypes;
import gde.device.ObjectFactory;
import gde.device.PropertyType;
import gde.device.TimeUnitTypes;
import gde.log.Level;
import gde.log.LogFormatter;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.dialog.HelpInfoDialog;
import gde.utils.FileUtils;
import gde.utils.StringHelper;

/**
 * Dialog class enable to edit existing and create new device property files
 * @author Winfried Brügmann
 */
public class DevicePropertiesEditor extends Composite {
	final static String 									$CLASS_NAME 														= DevicePropertiesEditor.class.getName();
	final static Logger										log																			= Logger.getLogger(DevicePropertiesEditor.class.getName());
	public static Shell										dialogShell;

	private static DevicePropertiesEditor	devicePropsEditor												= null;

	HelpInfoDialog												helpDialog;
	CTabFolder														tabFolder;
	Label																	devicePropFileNamelabel;
	Text																	deviceFileNameText;
	Button																deviceFileNameSelectionButton, saveButton, closeButton;

	CTabItem															deviceTabItem;
	Menu																	popupMenu;
	ContextMenu														contextMenu;
	String																lastTabItemName;																																										// to dispose menu widget
	Composite															deviceComposite;
	Label																	deviceDescriptionlabel;
	Label																	deviceNameLabel, deviceImplementationLabel, manufacturerLabel, manufURLabel, imageFileNameLabel, usageLabel, groupLabel;
	Button																deviceImplementationButton;
	Text																	nameText, deviceImplementationText, manufacturerText, manufURLText, imageFileNameText;
	Button																usageButton, fileSelectButton;
	CCombo																groupSelectionCombo;
	Composite															deviceLabelComposite, devicePropsComposite;

	String																devicePropertiesFileName								= GDE.STRING_EMPTY;
	String																deviceName															= GDE.STRING_EMPTY;
	String																deviceImplementationClass								= GDE.STRING_EMPTY;
	boolean																isDeviceImplementaionClass							= false;
	String																manufacuturerURL												= GDE.STRING_EMPTY;
	String																imageFileName														= GDE.STRING_EMPTY;
	String																manufacturer														= GDE.STRING_EMPTY;
	boolean																isDeviceUsed														= false;
	DeviceTypes														deviceGroup															= DeviceTypes.LOGGER;

	SeriaPortTypeTabItem									serialPortTabItem;

	CTabItem															timeBaseTabItem;
	Composite															timeBaseComposite;
	Label																	timeBaseDescriptionLabel;
	Label																	timeBaseNameLabel, timeBaseSymbolLabel, timeBaseUnitLabel, timeBaseTimeStepLabel, timeBaseUtcDeltaLabel;
	Text																	timeBaseNameText, timeBaseSymbolText, timeBaseUnitText, timeBaseTimeStepText, timeBaseUtcDeltaText;

	double																timeStep_ms															= 0.0;
	int																		utcDelta																= 0;

	CTabItem															dataBlockTabItem;
	Composite															dataBlockComposite;
	Label																	dataBlockDescriptionLabel;
	Label																	dataBlockInputLabel, dataBlockFormatLabel, dataBlockSizeLabel, dataBlockSeparatorLabel, dataBlockTimeUnitLabel, dataBlockLeaderLabel, dataBlockEndingLabel, dataBlockCheckSumTypeLabel;
	Button																dataBlockCheckSumFormatButton, preferredDataLocationButton, preferredFileExtensionButton;
	CCombo																dataBlockEndingCombo, dataBlockInputCombo1, dataBlockFormatCombo1, dataBlockInputCombo2, dataBlockFormatCombo2, dataBlockTimeUnitCombo, dataBlockcheckSumFormatCombo, dataBlockCheckSumTypeCombo, dataBlockSeparatorCombo;
	Text																	dataBlockSizeText1, dataBlockSizeText2, dataBlockLeaderText, preferredDataLocationText, preferredFileExtensionText;
	Group																	dataBlockRequiredGroup, dataBlockOptionalGroup;

	FormatTypes														dataBlockFormatType1										= FormatTypes.BYTE, dataBlockcheckSumFormat = FormatTypes.BINARY;
	InputTypes														dataBlockInputType1											= InputTypes.SERIAL_IO;
	int																		dataBlockSize1													= 90;
	FormatTypes														dataBlockFormatType2										= FormatTypes.BYTE;
	InputTypes														dataBlockInputType2											= InputTypes.FILE_IO;
	int																		dataBlockSize2													= 30;
	TimeUnitTypes													dataBlockTimeUnit												= TimeUnitTypes.MSEC;
	CommaSeparatorTypes										dataBlockSeparator											= CommaSeparatorTypes.SEMICOLON;
	CheckSumTypes													dataBlockCheckSumType										= CheckSumTypes.ADD;
	String																dataBlockLeader													= GDE.STRING_DOLLAR;
	String																dataBlockEnding													= LineEndingTypes.CRLF.value();
	boolean																isDataBlockOptionalChecksumEnabled			= false;
	boolean																isDataBlockOptionalDataLocationEnabled	= false;
	String																dataBlockOptionalDataLocation						= GDE.STRING_EMPTY;
	boolean																isDataBlockOptionalFileExtentionEnabled	= false;
	String																dataBlockOptionalFileExtention					= GDE.FILE_ENDING_STAR_CSV;

	CTabItem															stateTabItem;
	Composite															stateComposite;
	Label																	stateDescriptionLabel;
	Button																addButton;
	CTabFolder														stateTabFolder;
	PropertyTypeTabItem										modeStateItemComposite;

	CTabItem															channelConfigurationTabItem;
	Composite															channelConfigComposite;
	Label																	channelConfigDescriptionLabel;
	CTabFolder														channelConfigInnerTabFolder;

	CTabItem															destopTabItem;
	DesktopPropertyTypeTabItem						desktopInnerTabItem1, desktopInnerTabItem2, desktopInnerTabItem3, desktopInnerTabItem4, desktopInnerTabItem5, desktopInnerTabItem6;
	Composite															desktopComposite;
	Label																	desktopDescriptionLabel;
	CTabFolder														desktopTabFolder;

	//cross over fields
	DeviceConfiguration										deviceConfig;
	final Settings												settings;
	final DataExplorer 										application;

	public static DevicePropertiesEditor getInstance() {
		if (DevicePropertiesEditor.devicePropsEditor == null) {
			DevicePropertiesEditor.dialogShell = new Shell(GDE.display, SWT.DIALOG_TRIM | (GDE.IS_LINUX ? SWT.MODELESS : SWT.PRIMARY_MODAL));
			DevicePropertiesEditor.devicePropsEditor = new DevicePropertiesEditor(DevicePropertiesEditor.dialogShell, SWT.NULL);
		}
		else if (DevicePropertiesEditor.devicePropsEditor.isDisposed()) {
			DevicePropertiesEditor.dialogShell = new Shell(GDE.display, SWT.DIALOG_TRIM | (GDE.IS_LINUX ? SWT.MODELESS : SWT.PRIMARY_MODAL));
			DevicePropertiesEditor.devicePropsEditor = new DevicePropertiesEditor(DevicePropertiesEditor.dialogShell, SWT.NULL);
		}

		return DevicePropertiesEditor.devicePropsEditor;
	}

	private DevicePropertiesEditor(Shell parent, int style) {
		super(parent, style);
		this.settings = Settings.getInstance();
		this.application = DataExplorer.getInstance();
	}

	/**
	 * main method to display this dialog inside a shell
	 */
	public static void main(String[] args) {
		try {
			//DeviceData data = new DeviceData();
			//data.tracking = true;
			GDE.display = new Display(); //data);
			//Sleak sleak = new Sleak();
			//sleak.open();
			devicePropsEditor = getInstance();
			devicePropsEditor.initLogger();
			devicePropsEditor.open();
			Point size = devicePropsEditor.getSize();
			DevicePropertiesEditor.dialogShell.setLayout(new FillLayout());
			DevicePropertiesEditor.dialogShell.setText(Messages.getString(MessageIds.GDE_MSGT0440));
			DevicePropertiesEditor.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/EditHot.gif")); //$NON-NLS-1$
			DevicePropertiesEditor.dialogShell.setLocation(100, 100);
			//Rectangle displayBounds = display.getBounds();
			//DevicePropertiesEditor.dialogShell.setLocation(displayBounds.x < 0 ? -size.x : displayBounds.width + displayBounds.x - size.x, displayBounds.height - size.y - 150);
			DevicePropertiesEditor.dialogShell.layout();
			Rectangle shellBounds = DevicePropertiesEditor.dialogShell.computeTrim(0, 0, size.x, size.y);
			DevicePropertiesEditor.dialogShell.setSize(shellBounds.width, shellBounds.height+20+30);
			DevicePropertiesEditor.dialogShell.setMinimumSize(shellBounds.width, shellBounds.height+20+30);
			DevicePropertiesEditor.dialogShell.open();

			if (args.length > 0) {
				String tmpDevFileName = args[0].replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);
				tmpDevFileName = tmpDevFileName.toUpperCase().startsWith(devicePropsEditor.getDevicesPath().toUpperCase()) && tmpDevFileName.length() > devicePropsEditor.getDevicesPath().length() + 6 // "/a.xml"
				? tmpDevFileName.substring(devicePropsEditor.getDevicesPath().length() + 1) : tmpDevFileName;
				devicePropsEditor.openDevicePropertiesFile(tmpDevFileName);
			}

			//Display display = Display.getDefault();
			while (!DevicePropertiesEditor.dialogShell.isDisposed()) {
				if (!GDE.display.readAndDispatch()) GDE.display.sleep();
			}
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void openAsDialog(DeviceConfiguration useDeviceConfiguration) {
		try {
			devicePropsEditor.open();
			Point size = devicePropsEditor.getSize();
			DevicePropertiesEditor.dialogShell.setLayout(new FillLayout());
			DevicePropertiesEditor.dialogShell.setText(Messages.getString(MessageIds.GDE_MSGT0440));
			DevicePropertiesEditor.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/EditHot.gif")); //$NON-NLS-1$
			DevicePropertiesEditor.dialogShell.setLocation(DataExplorer.getInstance().getShell().toDisplay(100, 50));
			DevicePropertiesEditor.dialogShell.layout();
			Rectangle shellBounds = DevicePropertiesEditor.dialogShell.computeTrim(0, 0, size.x, size.y);
			DevicePropertiesEditor.dialogShell.setSize(shellBounds.width, shellBounds.height+20+30);
			DevicePropertiesEditor.dialogShell.setMinimumSize(shellBounds.width, shellBounds.height+20+30);
			DevicePropertiesEditor.dialogShell.open();
			DevicePropertiesEditor.dialogShell.addListener(SWT.Traverse, new Listener() {
	      @Override
				public void handleEvent(Event event) {
	        switch (event.detail) {
	        case SWT.TRAVERSE_ESCAPE:
	        	DevicePropertiesEditor.dialogShell.close();
	          event.detail = SWT.TRAVERSE_NONE;
	          event.doit = false;
	          break;
	        }
	      }
	    });

			this.deviceConfig = useDeviceConfiguration;
			this.devicePropertiesFileName = this.deviceConfig.getPropertiesFileName();
			this.devicePropertiesFileName = this.devicePropertiesFileName.substring(this.getDevicesPath().length() + 1);
			this.deviceFileNameText.setText(this.devicePropertiesFileName);
			this.deviceFileNameSelectionButton.setEnabled(false);
			update();

			Display display = GDE.display; //DevicePropertiesEditor.dialogShell.getDisplay();
			while (!DevicePropertiesEditor.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
			DataExplorer.getInstance().resetShellIcon();
		}
		catch (Throwable e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * initialize logger
	 */
	private void initLogger() {
		Handler logHandler;
		LogFormatter lf = new LogFormatter();
		Logger rootLogger = Logger.getLogger(GDE.STRING_EMPTY);

		// cleanup previous log handler
		rootLogger.removeHandler(GDE.logHandler);

		if (System.getProperty(GDE.ECLIPSE_STRING) == null) { // running outside eclipse
			try {
				logHandler = new FileHandler(Settings.getLogFilePath(), 5000000, 3);
				rootLogger.addHandler(logHandler);
				logHandler.setFormatter(lf);
				logHandler.setLevel(java.util.logging.Level.ALL);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			logHandler = new ConsoleHandler();
			rootLogger.addHandler(logHandler);
			logHandler.setFormatter(lf);
			logHandler.setLevel(java.util.logging.Level.ALL);
		}
		// set logging levels
		Logger logger = Logger.getLogger("gde.ui.dialog.edit"); //$NON-NLS-1$
		logger.setLevel(java.util.logging.Level.INFO);
		logger.setUseParentHandlers(true);
	}

	public int openYesNoMessageDialog(final String message) {
		MessageBox yesNoMessageDialog = new MessageBox(DevicePropertiesEditor.dialogShell, SWT.PRIMARY_MODAL | SWT.YES | SWT.NO | SWT.ICON_QUESTION);
		yesNoMessageDialog.setText(GDE.NAME_LONG);
		yesNoMessageDialog.setMessage(message);
		return yesNoMessageDialog.open();
	}

	public void open() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setLayout(new FormLayout());
			this.setSize(680, 500);
			FormData fd = new FormData();
			this.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "composite.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_A1.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.getShell().addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent evt) {
					log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					if (DevicePropertiesEditor.this.deviceConfig != null && DevicePropertiesEditor.this.deviceConfig.isChangePropery()) {
						String msg = Messages.getString(MessageIds.GDE_MSGI0041, new String[] { DevicePropertiesEditor.this.devicePropertiesFileName });
						if (DevicePropertiesEditor.this.openYesNoMessageDialog(msg) == SWT.YES) {
							DevicePropertiesEditor.this.deviceConfig.storeDeviceProperties();
							DevicePropertiesEditor.this.saveButton.setEnabled(false);

							if (DevicePropertiesEditor.this.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0042)) == SWT.YES) {
								FileUtils.updateFileInDeviceJar(DevicePropertiesEditor.this.deviceConfig, DevicePropertiesEditor.this.deviceConfig.getPropertiesFileName());
							}
						}
					}
					DevicePropertiesEditor.this.deviceConfig = null; //signal properties editor shut down
				}
			});
			{
				this.devicePropFileNamelabel = new Label(this, SWT.RIGHT);
				this.devicePropFileNamelabel.setText(Messages.getString(MessageIds.GDE_MSGT0483));
				this.devicePropFileNamelabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.devicePropFileNamelabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0484));
				fd = new FormData();
				fd.width = 100;
				fd.height = 16;
				fd.left = new FormAttachment(0, 1000, 0);
				fd.top = new FormAttachment(0, 1000, 12);
				this.devicePropFileNamelabel.setLayoutData(fd);
			}
			{
				this.deviceFileNameText = new Text(this, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
				fd = new FormData();
				fd.height = 18;
				fd.top = new FormAttachment(0, 1000, 10);
				fd.left = new FormAttachment(0, 1000, 120);
				fd.right = new FormAttachment(1000, 1000, -120);
				this.deviceFileNameText.setLayoutData(fd);
				this.deviceFileNameText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.deviceFileNameText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "deviceFileNameText.keyReleased, event=" + evt); //$NON-NLS-1$
						if (evt.keyCode == SWT.CR) {
							try {
								DevicePropertiesEditor.this.devicePropertiesFileName = DevicePropertiesEditor.this.deviceFileNameText.getText().trim();
								if (!DevicePropertiesEditor.this.devicePropertiesFileName.endsWith(GDE.FILE_ENDING_DOT_XML)) {
									if (DevicePropertiesEditor.this.devicePropertiesFileName.lastIndexOf(GDE.CHAR_DOT) != -1) {
										DevicePropertiesEditor.this.devicePropertiesFileName = DevicePropertiesEditor.this.devicePropertiesFileName.substring(0, DevicePropertiesEditor.this.devicePropertiesFileName
												.lastIndexOf(GDE.CHAR_DOT));
									}
									DevicePropertiesEditor.this.devicePropertiesFileName = DevicePropertiesEditor.this.devicePropertiesFileName + GDE.FILE_ENDING_DOT_XML;
								}
								log.log(java.util.logging.Level.FINE, "devicePropertiesFileName = " + DevicePropertiesEditor.this.devicePropertiesFileName); //$NON-NLS-1$

								if (!(new File(getDevicesPath() + GDE.STRING_FILE_SEPARATOR_UNIX + DevicePropertiesEditor.this.devicePropertiesFileName)).exists()) {
									MessageBox okCancelMessageDialog = new MessageBox(DevicePropertiesEditor.this.getShell(), SWT.PRIMARY_MODAL | SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);
									okCancelMessageDialog.setText(Messages.getString(MessageIds.GDE_MSGT0440));
									okCancelMessageDialog.setMessage(Messages.getString(MessageIds.GDE_MSGE0003) + DevicePropertiesEditor.this.devicePropertiesFileName + Messages.getString(MessageIds.GDE_MSGT0481));
									if (SWT.OK == okCancelMessageDialog.open()) {
										if (FileUtils
												.extract(
														this.getClass(),
														"DeviceSample_" + DevicePropertiesEditor.this.settings.getLocale() + GDE.FILE_ENDING_DOT_XML, DevicePropertiesEditor.this.devicePropertiesFileName,//$NON-NLS-1$
														"resource/", getDevicesPath(), "555")) { //$NON-NLS-1$ //$NON-NLS-2$
											DevicePropertiesEditor.this.deviceConfig = new DeviceConfiguration(getDevicesPath() + GDE.STRING_FILE_SEPARATOR_UNIX + DevicePropertiesEditor.this.devicePropertiesFileName);
										}
									}
								}
								else {
									DevicePropertiesEditor.this.deviceConfig = new DeviceConfiguration(getDevicesPath() + GDE.STRING_FILE_SEPARATOR_UNIX + DevicePropertiesEditor.this.devicePropertiesFileName);
								}
								update();
							}
							catch (Throwable e) {
								log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
							}
						}
					}
				});
			}
			{
				this.deviceFileNameSelectionButton = new Button(this, SWT.PUSH | SWT.CENTER);
				this.deviceFileNameSelectionButton.setText(" ... "); //$NON-NLS-1$
				this.deviceFileNameSelectionButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.deviceFileNameSelectionButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0484));
				//this.deviceFileNameSelectionButton.setBounds(580, 10, 30, 20);
				fd = new FormData();
				fd.width = GDE.IS_MAC ? 40 : 30;
				fd.height = 22;
				fd.top = new FormAttachment(0, 1000, 12);
				fd.right = new FormAttachment(1000, 1000, -25);
				this.deviceFileNameSelectionButton.setLayoutData(fd);
				this.deviceFileNameSelectionButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "fileSelectionButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						if (DevicePropertiesEditor.this.deviceConfig != null && DevicePropertiesEditor.this.deviceConfig.isChangePropery()) {
							String msg = Messages.getString(MessageIds.GDE_MSGI0041, new String[] { DevicePropertiesEditor.this.devicePropertiesFileName });
							if (DevicePropertiesEditor.this.openYesNoMessageDialog(msg) == SWT.YES) {
								DevicePropertiesEditor.this.deviceConfig.storeDeviceProperties();
								DevicePropertiesEditor.this.saveButton.setEnabled(false);

								if (DevicePropertiesEditor.this.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0042)) == SWT.YES) {
									FileUtils.updateFileInDeviceJar(DevicePropertiesEditor.this.deviceConfig, DevicePropertiesEditor.this.deviceConfig.getPropertiesFileName());
								}
							}
						}
						FileDialog fileSelectionDialog = new FileDialog(DevicePropertiesEditor.this.getShell());
						fileSelectionDialog.setFilterPath(getDevicesPath());
						fileSelectionDialog.setText(Messages.getString(MessageIds.GDE_MSGT0479));
						fileSelectionDialog.setFilterExtensions(new String[] { GDE.FILE_ENDING_STAR_XML });
						fileSelectionDialog.setFilterNames(new String[] { Messages.getString(MessageIds.GDE_MSGT0480) });
						fileSelectionDialog.open();
						String tmpFileName = fileSelectionDialog.getFileName();
						log.log(java.util.logging.Level.FINE, "devicePropertiesFileName = " + tmpFileName); //$NON-NLS-1$

						if (tmpFileName != null && tmpFileName.length() > 4 && !tmpFileName.equals(DevicePropertiesEditor.this.devicePropertiesFileName)) {
							openDevicePropertiesFile(tmpFileName);
						}
					}
				});
			}
			{
				this.saveButton = new Button(this, SWT.PUSH | SWT.CENTER);
				this.saveButton.setText(Messages.getString(MessageIds.GDE_MSGT0486));
				this.saveButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.saveButton.setEnabled(false);
				fd = new FormData();
				fd.width = 250;
				fd.height = 30;
				fd.bottom = new FormAttachment(1000, 1000, -10);
				fd.left = new FormAttachment(0, 1000, 50);
				this.saveButton.setLayoutData(fd);
				this.saveButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.deviceConfig.storeDeviceProperties();
						DevicePropertiesEditor.this.saveButton.setEnabled(false);

					}
				});
			}
			{
				this.closeButton = new Button(this, SWT.PUSH | SWT.CENTER);
				this.closeButton.setText(Messages.getString(MessageIds.GDE_MSGT0485));
				this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				fd = new FormData();
				fd.width = 250;
				fd.height = 30;
				fd.bottom = new FormAttachment(1000, 1000, -10);
				fd.right = new FormAttachment(1000, 1000, -50);
				this.closeButton.setLayoutData(fd);
				this.closeButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.getShell().dispose();
						SWTResourceManager.listResourceStatus("DevicePropertiesEditor.close()");
					}
				});
			}
			{
				this.tabFolder = new CTabFolder(this, SWT.BORDER);
				this.tabFolder.setSimple(false);
				this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				fd = new FormData();
				fd.left = new FormAttachment(0, 1000, 0);
				fd.right = new FormAttachment(1000, 1000, 0);
				fd.bottom = new FormAttachment(1000, 1000, -50);
				fd.top = new FormAttachment(0, 1000, 46);
				this.tabFolder.setLayoutData(fd);
				this.tabFolder.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "tabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.enableContextMenu(DevicePropertiesEditor.this.tabFolder.getSelection().getText(), true);
					}
				});
				{
					this.deviceTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					this.deviceTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0487));
					this.deviceTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					{
						this.deviceComposite = new Composite(this.tabFolder, SWT.NONE);
						this.deviceComposite.setLayout(null);
						this.deviceTabItem.setControl(this.deviceComposite);
						{
							this.deviceDescriptionlabel = new Label(this.deviceComposite, SWT.CENTER | SWT.WRAP);
							this.deviceDescriptionlabel.setText(Messages.getString(MessageIds.GDE_MSGT0488));
							this.deviceDescriptionlabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.deviceDescriptionlabel.setBounds(12, 8, 604, 50);
						}
						{
							this.deviceLabelComposite = new Composite(this.deviceComposite, SWT.NONE);
							this.deviceLabelComposite.setLayout(null);
							this.deviceLabelComposite.setBounds(20, 70, 145, 240);
							{
								this.deviceNameLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.deviceNameLabel.setText(Messages.getString(MessageIds.GDE_MSGT0489));
								this.deviceNameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.deviceNameLabel.setBounds(0, 0, 140, 16);
							}
							{
								this.deviceImplementationLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.deviceImplementationLabel.setText(Messages.getString(MessageIds.GDE_MSGT0477));
								this.deviceImplementationLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.deviceImplementationLabel.setEnabled(false);
								this.deviceImplementationLabel.setBounds(0, 30, 140, 16);
							}
							{
								this.manufacturerLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.manufacturerLabel.setText(Messages.getString(MessageIds.GDE_MSGT0490));
								this.manufacturerLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.manufacturerLabel.setBounds(0, 70, 140, 19);
							}
							{
								this.manufURLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.manufURLabel.setText(Messages.getString(MessageIds.GDE_MSGT0491));
								this.manufURLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.manufURLabel.setBounds(0, 100, 140, 19);
							}
							{
								this.imageFileNameLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.imageFileNameLabel.setText(Messages.getString(MessageIds.GDE_MSGT0492));
								this.imageFileNameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.imageFileNameLabel.setBounds(0, 130, 140, 19);
							}
							{
								this.usageLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.usageLabel.setText(Messages.getString(MessageIds.GDE_MSGT0493));
								this.usageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.usageLabel.setBounds(0, 160, 140, 19);
							}
							{
								this.groupLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.groupLabel.setText(Messages.getString(MessageIds.GDE_MSGT0494));
								this.groupLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.groupLabel.setBounds(0, 190, 140, 16);
							}
						}
						{
							this.devicePropsComposite = new Composite(this.deviceComposite, SWT.NONE);
							this.devicePropsComposite.setLayout(null);
							this.devicePropsComposite.setBounds(170, 70, 450, 240);
							{
								this.nameText = new Text(this.devicePropsComposite, SWT.BORDER);
								this.nameText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.nameText.setBounds(0, 0, 410, 22);
								this.nameText.addKeyListener(new KeyAdapter() {
									@Override
									public void keyReleased(KeyEvent evt) {
										log.log(java.util.logging.Level.FINEST, "nameText.keyReleased, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.deviceConfig.setName(DevicePropertiesEditor.this.deviceName = DevicePropertiesEditor.this.nameText.getText());
										DevicePropertiesEditor.this.enableSaveButton(true);
									}
								});
							}
							{
								this.deviceImplementationButton = new Button(this.devicePropsComposite, SWT.CHECK);
								this.deviceImplementationButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.deviceImplementationButton.setForeground(this.application.COLOR_BLACK);
								this.deviceImplementationButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0478));
								this.deviceImplementationButton.setBounds(0, 30, 22, 22);
								this.deviceImplementationButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(java.util.logging.Level.FINEST, "implementationButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.isDeviceImplementaionClass = DevicePropertiesEditor.this.deviceImplementationButton.getSelection();
										DevicePropertiesEditor.this.deviceImplementationLabel.setEnabled(DevicePropertiesEditor.this.isDeviceImplementaionClass);
										DevicePropertiesEditor.this.deviceImplementationText.setEnabled(DevicePropertiesEditor.this.isDeviceImplementaionClass);
										if (DevicePropertiesEditor.this.deviceConfig != null) {
											DevicePropertiesEditor.this.deviceConfig.setUsed(DevicePropertiesEditor.this.isDeviceUsed = DevicePropertiesEditor.this.usageButton.getSelection());
											DevicePropertiesEditor.this.enableSaveButton(true);
										}
									}
								});
							}
							{
								this.deviceImplementationText = new Text(this.devicePropsComposite, SWT.BORDER);
								this.deviceImplementationText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.deviceImplementationText.setBounds(30, 28, 380, 22);
								this.deviceImplementationText.setEnabled(false);
								this.deviceImplementationText.addKeyListener(new KeyAdapter() {
									@Override
									public void keyReleased(KeyEvent evt) {
										log.log(java.util.logging.Level.FINEST, "nameText.keyReleased, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.deviceConfig.setDeviceImplName(DevicePropertiesEditor.this.deviceImplementationClass = DevicePropertiesEditor.this.deviceImplementationText.getText()
												.trim());
										DevicePropertiesEditor.this.enableSaveButton(true);
									}
								});
							}
							{
								this.manufacturerText = new Text(this.devicePropsComposite, SWT.BORDER);
								this.manufacturerText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.manufacturerText.setBounds(0, 68, 410, 22);
								this.manufacturerText.addKeyListener(new KeyAdapter() {
									@Override
									public void keyReleased(KeyEvent evt) {
										log.log(java.util.logging.Level.FINEST, "manufacturerText.keyReleased, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.deviceConfig.setManufacturer(DevicePropertiesEditor.this.manufacturer = DevicePropertiesEditor.this.manufacturerText.getText());
										DevicePropertiesEditor.this.enableSaveButton(true);
									}
								});
							}
							{
								this.manufURLText = new Text(this.devicePropsComposite, SWT.BORDER);
								this.manufURLText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.manufURLText.setBounds(0, 98, 410, 22);
								this.manufURLText.addKeyListener(new KeyAdapter() {
									@Override
									public void keyReleased(KeyEvent evt) {
										log.log(java.util.logging.Level.FINEST, "manufURLText.keyReleased, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.deviceConfig.setManufacturerURL(DevicePropertiesEditor.this.manufacuturerURL = DevicePropertiesEditor.this.manufURLText.getText());
										DevicePropertiesEditor.this.enableSaveButton(true);
									}
								});
							}
							{
								this.imageFileNameText = new Text(this.devicePropsComposite, SWT.BORDER);
								this.imageFileNameText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.imageFileNameText.setBounds(0, 128, 410, 22);
								this.imageFileNameText.addKeyListener(new KeyAdapter() {
									@Override
									public void keyReleased(KeyEvent evt) {
										log.log(java.util.logging.Level.FINEST, "imageFileNameText.keyReleased, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.deviceConfig.setImageFileName(DevicePropertiesEditor.this.imageFileName = DevicePropertiesEditor.this.imageFileNameText.getText());
										DevicePropertiesEditor.this.enableSaveButton(true);
									}
								});
							}
							{
								this.usageButton = new Button(this.devicePropsComposite, SWT.CHECK);
								this.usageButton.setBounds(3, 158, 22, 22);
								this.usageButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(java.util.logging.Level.FINEST, "usageButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (DevicePropertiesEditor.this.deviceConfig != null) {
											DevicePropertiesEditor.this.deviceConfig.setUsed(DevicePropertiesEditor.this.isDeviceUsed = DevicePropertiesEditor.this.usageButton.getSelection());
											DevicePropertiesEditor.this.enableSaveButton(true);
										}
									}
								});
							}
							{
								this.groupSelectionCombo = new CCombo(this.devicePropsComposite, SWT.BORDER);
								this.groupSelectionCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.groupSelectionCombo.setItems(DeviceTypes.valuesAsStingArray());
								this.groupSelectionCombo.setBounds(0, 188, 410, 22);
								this.groupSelectionCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(java.util.logging.Level.FINEST, "groupSelectionCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (DevicePropertiesEditor.this.deviceConfig != null) {
											DevicePropertiesEditor.this.deviceConfig.setDeviceGroup(DevicePropertiesEditor.this.deviceGroup = DeviceTypes
													.fromValue(DevicePropertiesEditor.this.groupSelectionCombo.getText()));
											DevicePropertiesEditor.this.enableSaveButton(true);
										}
									}
								});
							}
							{
								this.fileSelectButton = new Button(this.devicePropsComposite, SWT.PUSH | SWT.CENTER);
								this.fileSelectButton.setText(" ... "); //$NON-NLS-1$
								this.fileSelectButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0502));
								this.fileSelectButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.fileSelectButton.setBounds(415, 130, 30, 20);
								this.fileSelectButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										FileDialog fileSelectionDialog = new FileDialog(DevicePropertiesEditor.this.getShell());
										fileSelectionDialog.setText("DataExplorer Device Image File"); //$NON-NLS-1$
										fileSelectionDialog.setFilterPath(getDevicesPath());
										fileSelectionDialog.setFilterExtensions(new String[] { GDE.FILE_ENDING_STAR_JPG, GDE.FILE_ENDING_STAR_GIF, GDE.FILE_ENDING_STAR_PNG });
										fileSelectionDialog.setFilterNames(new String[] { Messages.getString(MessageIds.GDE_MSGT0215), Messages.getString(MessageIds.GDE_MSGT0214),
												Messages.getString(MessageIds.GDE_MSGT0213) });
										fileSelectionDialog.open();
										DevicePropertiesEditor.this.imageFileName = fileSelectionDialog.getFileName();
										String fullQualifiedImageSourceName = fileSelectionDialog.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + DevicePropertiesEditor.this.imageFileName;
										if (DevicePropertiesEditor.this.imageFileName != null && DevicePropertiesEditor.this.imageFileName.length() > 5) {
											DevicePropertiesEditor.this.imageFileNameText.setText(DevicePropertiesEditor.this.imageFileName);
											log.log(java.util.logging.Level.FINE, "imageFileName = " + DevicePropertiesEditor.this.imageFileName); //$NON-NLS-1$
											if (DevicePropertiesEditor.this.deviceConfig != null) {
												DevicePropertiesEditor.this.deviceConfig.setImageFileName(DevicePropertiesEditor.this.imageFileName = DevicePropertiesEditor.this.imageFileNameText.getText());
												DevicePropertiesEditor.this.enableSaveButton(true);

												FileUtils.updateImageInDeviceJar(DevicePropertiesEditor.this.deviceConfig, DevicePropertiesEditor.this.imageFileName, new Image(Display.getDefault(), new Image(Display
														.getDefault(), fullQualifiedImageSourceName).getImageData().scaledTo(225, 165)));
											}
										}
									}
								});
							}
						}
					}
				}
				{
					this.serialPortTabItem = new SeriaPortTypeTabItem(this.tabFolder, SWT.CLOSE, 1);
				}
				{
					this.timeBaseTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					SWTResourceManager.registerResourceUser(this.timeBaseTabItem);
					this.timeBaseTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0495));
					this.timeBaseTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					{
						this.timeBaseComposite = new Composite(this.tabFolder, SWT.NONE);
						this.timeBaseTabItem.setControl(this.timeBaseComposite);
						this.timeBaseComposite.setLayout(null);
						{
							this.timeBaseDescriptionLabel = new Label(this.timeBaseComposite, SWT.CENTER | SWT.WRAP);
							this.timeBaseDescriptionLabel.setText(Messages.getString(MessageIds.GDE_MSGT0496));
							this.timeBaseDescriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseDescriptionLabel.setBounds(17, 12, 591, 71);
						}
						{
							this.timeBaseNameLabel = new Label(this.timeBaseComposite, SWT.RIGHT);
							this.timeBaseNameLabel.setText(Messages.getString(MessageIds.GDE_MSGT0497));
							this.timeBaseNameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseNameLabel.setBounds(142, 95, 150, 20);
						}
						{
							this.timeBaseNameText = new Text(this.timeBaseComposite, SWT.CENTER | SWT.BORDER);
							this.timeBaseNameText.setText(Messages.getString(MessageIds.GDE_MSGT0271).split(GDE.STRING_BLANK)[0]);
							this.timeBaseNameText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseNameText.setBounds(322, 94, 60, 20);
							this.timeBaseNameText.setEditable(false);
						}
						{
							this.timeBaseSymbolLabel = new Label(this.timeBaseComposite, SWT.RIGHT);
							this.timeBaseSymbolLabel.setText(Messages.getString(MessageIds.GDE_MSGT0498));
							this.timeBaseSymbolLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseSymbolLabel.setBounds(142, 125, 150, 20);
						}
						{
							this.timeBaseSymbolText = new Text(this.timeBaseComposite, SWT.CENTER | SWT.BORDER);
							this.timeBaseSymbolText.setText(Messages.getString(MessageIds.GDE_MSGT0271).split(GDE.STRING_BLANK)[3]);
							this.timeBaseSymbolText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseSymbolText.setBounds(322, 124, 60, 20);
							this.timeBaseSymbolText.setEditable(false);
						}
						{
							this.timeBaseUnitLabel = new Label(this.timeBaseComposite, SWT.RIGHT);
							this.timeBaseUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT0499));
							this.timeBaseUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseUnitLabel.setBounds(142, 155, 150, 20);
						}
						{
							this.timeBaseUnitText = new Text(this.timeBaseComposite, SWT.CENTER | SWT.BORDER);
							this.timeBaseUnitText.setText(Messages.getString(MessageIds.GDE_MSGT0271).split(GDE.STRING_BLANK)[7]);
							this.timeBaseUnitText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseUnitText.setBounds(322, 154, 60, 20);
							this.timeBaseUnitText.setEditable(false);
						}
						{
							this.timeBaseTimeStepLabel = new Label(this.timeBaseComposite, SWT.RIGHT);
							this.timeBaseTimeStepLabel.setText(Messages.getString(MessageIds.GDE_MSGT0500));
							this.timeBaseTimeStepLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseTimeStepLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0501));
							this.timeBaseTimeStepLabel.setBounds(142, 185, 150, 20);
						}
						{
							this.timeBaseTimeStepText = new Text(this.timeBaseComposite, SWT.RIGHT | SWT.BORDER);
							this.timeBaseTimeStepText.setText("1000.0"); //$NON-NLS-1$
							this.timeBaseTimeStepText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseTimeStepText.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0501));
							this.timeBaseTimeStepText.setBounds(322, 184, 60, 20);
							this.timeBaseTimeStepText.addVerifyListener(new VerifyListener() {
								@Override
								public void verifyText(VerifyEvent evt) {
									log.log(java.util.logging.Level.FINEST, "timeBaseTimeStepText.verifyText, event=" + evt); //$NON-NLS-1$
									evt.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, evt.text);
								}
							});
							this.timeBaseTimeStepText.addKeyListener(new KeyAdapter() {
								@Override
								public void keyReleased(KeyEvent evt) {
									log.log(java.util.logging.Level.FINEST, "timeBaseTimeStepText.keyReleased, event=" + evt); //$NON-NLS-1$
									try {
										DevicePropertiesEditor.this.timeStep_ms = Double.parseDouble(DevicePropertiesEditor.this.timeBaseTimeStepText.getText().replace(GDE.CHAR_COMMA, GDE.CHAR_DOT));
										DevicePropertiesEditor.this.timeStep_ms = Double.parseDouble(String.format(Locale.ENGLISH, "%.1f", DevicePropertiesEditor.this.timeStep_ms)); //$NON-NLS-1$
										if (DevicePropertiesEditor.this.deviceConfig != null) {
											DevicePropertiesEditor.this.deviceConfig.setTimeStep_ms(DevicePropertiesEditor.this.timeStep_ms);
											DevicePropertiesEditor.this.enableSaveButton(true);
										}
									}
									catch (NumberFormatException e) {
										// ignore input
									}
								}
							});
						}
						{
							this.timeBaseUtcDeltaLabel = new Label(this.timeBaseComposite, SWT.RIGHT);
							this.timeBaseUtcDeltaLabel.setText("UCTdelta");
							this.timeBaseUtcDeltaLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseUtcDeltaLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0971));
							this.timeBaseUtcDeltaLabel.setBounds(142, 215, 150, 20);
						}
						{
							this.timeBaseUtcDeltaText = new Text(this.timeBaseComposite, SWT.RIGHT | SWT.BORDER);
							this.timeBaseUtcDeltaText.setText("0"); //$NON-NLS-1$
							this.timeBaseUtcDeltaText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseUtcDeltaText.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0971));
							this.timeBaseUtcDeltaText.setBounds(322, 214, 60, 20);
							this.timeBaseUtcDeltaText.addVerifyListener(new VerifyListener() {
								@Override
								public void verifyText(VerifyEvent evt) {
									log.log(java.util.logging.Level.FINEST, "timeBaseUtcDeltaText.verifyText, event=" + evt); //$NON-NLS-1$
									evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
								}
							});
							this.timeBaseUtcDeltaText.addKeyListener(new KeyAdapter() {
								@Override
								public void keyReleased(KeyEvent evt) {
									log.log(java.util.logging.Level.FINEST, "timeBaseUtcDeltaText.keyReleased, event=" + evt); //$NON-NLS-1$
									try {
										DevicePropertiesEditor.this.utcDelta = Integer.parseInt(DevicePropertiesEditor.this.timeBaseUtcDeltaText.getText());
										DevicePropertiesEditor.this.utcDelta = Integer.parseInt(String.format(Locale.ENGLISH, "%d", DevicePropertiesEditor.this.utcDelta)); //$NON-NLS-1$
										if (DevicePropertiesEditor.this.deviceConfig != null) {
											DevicePropertiesEditor.this.deviceConfig.setUTCdelta(DevicePropertiesEditor.this.utcDelta);
											DevicePropertiesEditor.this.enableSaveButton(true);
										}
									}
									catch (NumberFormatException e) {
										// ignore input
									}
								}
							});
						}
					}
				}
				{
					createDataBlockType();
				}
				{
					createStateTabItem();
				}
				{
					this.channelConfigurationTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					this.channelConfigurationTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0503));
					this.channelConfigurationTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					{
						this.channelConfigComposite = new Composite(this.tabFolder, SWT.NONE);
						this.channelConfigComposite.setLayout(new FormLayout());
						this.channelConfigurationTabItem.setControl(this.channelConfigComposite);
						{
							this.channelConfigDescriptionLabel = new Label(this.channelConfigComposite, SWT.CENTER | SWT.WRAP);
							this.channelConfigDescriptionLabel.setText(Messages.getString(MessageIds.GDE_MSGT0504));
							this.channelConfigDescriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							//this.channelConfigDescriptionLabel.setBounds(12, 5, 602, 38);
							fd = new FormData();
							fd.height = 38;
							fd.top = new FormAttachment(0, 1000, 5);
							fd.left = new FormAttachment(0, 1000, 12);
							fd.right = new FormAttachment(1000, 1000, -12);
							this.channelConfigDescriptionLabel.setLayoutData(fd);
						}
						{
							this.channelConfigInnerTabFolder = new CTabFolder(this.channelConfigComposite, SWT.NONE | SWT.BORDER);
							this.channelConfigInnerTabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.channelConfigInnerTabFolder.setBounds(0, 49, 626, 285);
							fd = new FormData();
							fd.top = new FormAttachment(0, 1000, 50);
							fd.left = new FormAttachment(0, 1000, 0);
							fd.right = new FormAttachment(1000, 1000, 0);
							fd.bottom = new FormAttachment(1000, 1000, 0);
							this.channelConfigInnerTabFolder.setLayoutData(fd);
							{
								//initial channel TabItem
								new ChannelTypeTabItem(this.channelConfigInnerTabFolder, SWT.NONE, 0);
							}
							this.channelConfigInnerTabFolder.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(java.util.logging.Level.FINE, "channelConfigInnerTabFolder selected, event=" + evt); //$NON-NLS-1$
									for (CTabItem item : DevicePropertiesEditor.this.channelConfigInnerTabFolder.getItems()) {
										((ChannelTypeTabItem)item).cleanMeasurementitems();
									}
									ChannelTypeTabItem tabItem = (ChannelTypeTabItem) evt.item;
									tabItem.setupMeasurementItems();
								}
							});
							this.channelConfigInnerTabFolder.setSelection(0);
							this.channelConfigInnerTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
								@Override
								public void restore(CTabFolderEvent evt) {
									log.log(java.util.logging.Level.FINE, "measurementsTabFolder.restore, event=" + evt); //$NON-NLS-1$
								}

								@Override
								public void close(CTabFolderEvent evt) {
									log.log(java.util.logging.Level.FINE, "measurementsTabFolder.close, event=" + evt); //$NON-NLS-1$
									ChannelTypeTabItem tabItem = ((ChannelTypeTabItem) evt.item);
									if (DevicePropertiesEditor.this.deviceConfig != null) {
										DevicePropertiesEditor.this.deviceConfig.removeChannelType(tabItem.channelConfigNumber);
									}
									DevicePropertiesEditor.this.channelConfigInnerTabFolder.setSelection(tabItem.channelConfigNumber - 1);
									tabItem.dispose();
									int itemCount = DevicePropertiesEditor.this.channelConfigInnerTabFolder.getItemCount();
									if (itemCount > 1) DevicePropertiesEditor.this.channelConfigInnerTabFolder.getItem(itemCount - 1).setShowClose(true);

									DevicePropertiesEditor.this.enableSaveButton(true);
								}
							});
						}
					}
				}
				{
					this.destopTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					this.destopTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0505));
					this.destopTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					{
						this.desktopComposite = new Composite(this.tabFolder, SWT.NONE);
						this.desktopComposite.setLayout(null);
						this.destopTabItem.setControl(this.desktopComposite);
						{
							this.desktopDescriptionLabel = new Label(this.desktopComposite, SWT.CENTER | SWT.WRAP);
							this.desktopDescriptionLabel.setText(Messages.getString(MessageIds.GDE_MSGT0506));
							this.desktopDescriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.desktopDescriptionLabel.setBounds(12, 5, 602, 57);
						}
						{
							this.desktopTabFolder = new CTabFolder(this.desktopComposite, SWT.BORDER);
							GridLayout appDesktopTabCompositeLayout = new GridLayout();
							appDesktopTabCompositeLayout.makeColumnsEqualWidth = true;
							this.desktopTabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.desktopTabFolder.setLayout(appDesktopTabCompositeLayout);
							this.desktopTabFolder.setBounds(135, 68, 360, 196);
							{
								this.desktopInnerTabItem1 = new DesktopPropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.TABLE_TAB.value(), null);
								this.desktopInnerTabItem2 = new DesktopPropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.DIGITAL_TAB.value(), null);
								this.desktopInnerTabItem3 = new DesktopPropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.ANALOG_TAB.value(), null);
								this.desktopInnerTabItem4 = new DesktopPropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB.value(), null);
								this.desktopInnerTabItem5 = new DesktopPropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.UTILITY_DEVICE_TAB.value(), null);
								this.desktopInnerTabItem6 = new DesktopPropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.UTILITY_GRAPHICS_TAB.value(), null);
							}
							this.desktopTabFolder.setSelection(0);
						}
					}
				}
				this.tabFolder.setSelection(0);
				this.tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
					@Override
					public void restore(CTabFolderEvent evt) {
						log.log(java.util.logging.Level.FINEST, "tabFolder.restore, event=" + evt); //$NON-NLS-1$
						((CTabItem) evt.item).getControl();
					}

					@Override
					public void close(CTabFolderEvent evt) {
						log.log(java.util.logging.Level.FINE, "tabFolder.close, event=" + evt); //$NON-NLS-1$
						CTabItem tabItem = ((CTabItem) evt.item);
						if (tabItem.getText().equals(Messages.getString(MessageIds.GDE_MSGT0470))) {
							tabItem.dispose();
							DevicePropertiesEditor.this.stateTabItem = null;
							if (DevicePropertiesEditor.this.deviceConfig != null) DevicePropertiesEditor.this.deviceConfig.removeStateType();
						}
						else if (tabItem.getText().equals(Messages.getString(MessageIds.GDE_MSGT0510))) {
							tabItem.dispose();
							DevicePropertiesEditor.this.serialPortTabItem = null;
							if (DevicePropertiesEditor.this.deviceConfig != null) DevicePropertiesEditor.this.deviceConfig.removeSerialPortType();
						}
						else if (tabItem.getText().equals(Messages.getString(MessageIds.GDE_MSGT0515))) {
							tabItem.dispose();
							DevicePropertiesEditor.this.dataBlockTabItem = null;
							if (DevicePropertiesEditor.this.deviceConfig != null) DevicePropertiesEditor.this.deviceConfig.removeDataBlockType();
						}
						if (DevicePropertiesEditor.this.deviceConfig != null) {
							DevicePropertiesEditor.this.enableSaveButton(true);
							update();
						}
					}
				});
			}
			initializeDeviceProperties();
			initializeTimeBase();
			initializeDataBlock();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * enable the context menu to create missing tab items
	 * @param tabItemName to enable the popup menu
	 * @param enable (always true, will be recalled internally for housekeeping)
	 */
	void enableContextMenu(String tabItemName, boolean enable) {
		if (this.lastTabItemName != null && !this.lastTabItemName.equals(tabItemName)) {
			this.enableContextMenu(this.lastTabItemName, false);
		}
		this.lastTabItemName = tabItemName;
		//this.tabFolder.setMenu(this.popupMenu);

		if (tabItemName.equals(Messages.getString(MessageIds.GDE_MSGT0510))) { // Serial port
			this.serialPortTabItem.enableContextmenu(enable);
		}
		else {
			if (enable && (this.popupMenu == null || this.contextMenu == null)) {
				this.popupMenu = new Menu(this.tabFolder.getShell(), SWT.POP_UP);
				//this.popupMenu = SWTResourceManager.getMenu("Contextmenu", this.tabFolder.getShell(), SWT.POP_UP);
				this.contextMenu = new ContextMenu(this.popupMenu, this.tabFolder);
				this.contextMenu.create();
				if (tabItemName.equals(Messages.getString(MessageIds.GDE_MSGT0487))) { //Device
					this.deviceComposite.setMenu(this.popupMenu);
					this.deviceDescriptionlabel.setMenu(this.popupMenu);
					this.deviceLabelComposite.setMenu(this.popupMenu);
					this.deviceNameLabel.setMenu(this.popupMenu);
					this.manufacturerLabel.setMenu(this.popupMenu);
					this.manufURLabel.setMenu(this.popupMenu);
					this.imageFileNameLabel.setMenu(this.popupMenu);
					this.usageLabel.setMenu(this.popupMenu);
					this.groupLabel.setMenu(this.popupMenu);
					this.devicePropsComposite.setMenu(this.popupMenu);
					//this.nameText.setMenu(this.popupMenu);
					//this.manufacturerText.setMenu(this.popupMenu);
					//this.manufURLText.setMenu(this.popupMenu);
					//this.imageFileNameText.setMenu(this.popupMenu);
					this.usageButton.setMenu(this.popupMenu);
					//this.groupSelectionCombo.setMenu(this.popupMenu);
				}
				else if (tabItemName.equals(Messages.getString(MessageIds.GDE_MSGT0495))) { //Time base
					this.timeBaseComposite.setMenu(this.popupMenu);
					this.timeBaseDescriptionLabel.setMenu(this.popupMenu);
					this.timeBaseNameLabel.setMenu(this.popupMenu);
					this.timeBaseNameText.setMenu(this.popupMenu);
					this.timeBaseSymbolLabel.setMenu(this.popupMenu);
					this.timeBaseSymbolText.setMenu(this.popupMenu);
					this.timeBaseUnitLabel.setMenu(this.popupMenu);
					this.timeBaseUnitText.setMenu(this.popupMenu);
					this.timeBaseTimeStepLabel.setMenu(this.popupMenu);
					this.timeBaseTimeStepText.setMenu(this.popupMenu);
					this.timeBaseUtcDeltaLabel.setMenu(this.popupMenu);
					this.timeBaseUtcDeltaText.setMenu(this.popupMenu);
				}
				else if (tabItemName.equals(Messages.getString(MessageIds.GDE_MSGT0515))) { // Data block
					this.dataBlockComposite.setMenu(this.popupMenu);
					this.dataBlockDescriptionLabel.setMenu(this.popupMenu);
					this.dataBlockRequiredGroup.setMenu(this.popupMenu);
					this.dataBlockFormatLabel.setMenu(this.popupMenu);
					this.dataBlockSizeLabel.setMenu(this.popupMenu);
					this.dataBlockOptionalGroup.setMenu(this.popupMenu);
					this.dataBlockCheckSumFormatButton.setMenu(this.popupMenu);
					this.dataBlockCheckSumTypeLabel.setMenu(this.popupMenu);
					this.dataBlockEndingLabel.setMenu(this.popupMenu);
					this.preferredDataLocationButton.setMenu(this.popupMenu);
					this.preferredFileExtensionButton.setMenu(this.popupMenu);
				}
				else if (tabItemName.equals(Messages.getString(MessageIds.GDE_MSGT0470))) { // State
					this.stateComposite.setMenu(this.popupMenu);
					this.stateDescriptionLabel.setMenu(this.popupMenu);
				}
				else if (tabItemName.equals(Messages.getString(MessageIds.GDE_MSGT0503))) { // Channel/Configuration
					this.channelConfigComposite.setMenu(this.popupMenu);
					this.channelConfigDescriptionLabel.setMenu(this.popupMenu);
					this.channelConfigInnerTabFolder.setMenu(this.popupMenu);
				}
				else if (tabItemName.equals(Messages.getString(MessageIds.GDE_MSGT0505))) { // Desktop
					this.desktopComposite.setMenu(this.popupMenu);
					this.desktopDescriptionLabel.setMenu(this.popupMenu);
				}
			}
			else if (this.popupMenu != null) {
				this.popupMenu.dispose();
				this.popupMenu = null;
				this.contextMenu = null;
			}
		}
	}

	/**
	 * open a device properties file
	 * @param useDevicePropertiesFileName
	 */
	private void openDevicePropertiesFile(String useDevicePropertiesFileName) {
		try {
			DevicePropertiesEditor.this.deviceConfig = new DeviceConfiguration(getDevicesPath() + GDE.STRING_FILE_SEPARATOR_UNIX + useDevicePropertiesFileName);

			DevicePropertiesEditor.this.devicePropertiesFileName = useDevicePropertiesFileName;
			DevicePropertiesEditor.this.deviceFileNameText.setText(DevicePropertiesEditor.this.devicePropertiesFileName);
			update();
		}
		catch (JAXBException e) {
			log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			if (e.getLinkedException() instanceof SAXParseException) {
				SAXParseException spe = (SAXParseException) e.getLinkedException();
				openWarningMessageBox(Messages.getString(MessageIds.GDE_MSGW0039, new String[] { spe.getSystemId().replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK), spe.getLocalizedMessage() }));
			}
		}
		catch (Exception e) {
			log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * create a new data block type and place it right after time base
	 */
	public void createDataBlockType() {
		for (int i = 1; i < this.tabFolder.getItemCount(); i++) {
			if (this.tabFolder.getItem(i).getText().equals(Messages.getString(MessageIds.GDE_MSGT0495))) {
				this.dataBlockTabItem = new CTabItem(this.tabFolder, SWT.CLOSE, i + 1);
				SWTResourceManager.registerResourceUser(this.dataBlockTabItem);
				break;
			}
		}
		this.dataBlockTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0515));
		{
			this.dataBlockComposite = new Composite(this.tabFolder, SWT.NONE);
			this.dataBlockComposite.setLayout(null);
			this.dataBlockTabItem.setControl(this.dataBlockComposite);
			this.dataBlockComposite.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "dataBlockComposite.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_A1.html#device_properties_datablock"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			{
				this.dataBlockDescriptionLabel = new Label(this.dataBlockComposite, SWT.CENTER);
				this.dataBlockDescriptionLabel.setText(Messages.getString(MessageIds.GDE_MSGT0516));
				this.dataBlockDescriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.dataBlockDescriptionLabel.setBounds(12, 5, 602, 51);
			}
			{
				this.dataBlockRequiredGroup = new Group(this.dataBlockComposite, SWT.NONE);
				this.dataBlockRequiredGroup.setLayout(null);
				this.dataBlockRequiredGroup.setText(Messages.getString(MessageIds.GDE_MSGT0517));
				this.dataBlockRequiredGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.dataBlockRequiredGroup.setBounds(20, 80, 270, 260);
				{
					this.dataBlockInputLabel = new Label(this.dataBlockRequiredGroup, SWT.RIGHT);
					this.dataBlockInputLabel.setText(Messages.getString(MessageIds.GDE_MSGT0601));
					this.dataBlockInputLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockInputLabel.setBounds(15, GDE.IS_MAC_COCOA ? 15 : 30, 60, 20);
				}
				{
					this.dataBlockInputCombo1 = new CCombo(this.dataBlockRequiredGroup, SWT.BORDER);
					this.dataBlockInputCombo1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockInputCombo1.setBounds(80, GDE.IS_MAC_COCOA ? 15 : 30, 90, 20);
					this.dataBlockInputCombo1.setItems(InputTypes.valuesAsStingArray());
					this.dataBlockInputCombo1.setBackground(this.application.COLOR_WHITE);
					this.dataBlockInputCombo1.setLayout(null);
					this.dataBlockInputCombo1.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBlockFormatCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockFormatType1 = FormatTypes.valueOf(DevicePropertiesEditor.this.dataBlockFormatCombo1.getText());
							DevicePropertiesEditor.this.dataBlockInputType1 = InputTypes.valueOf(DevicePropertiesEditor.this.dataBlockInputCombo1.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockFormat(DevicePropertiesEditor.this.dataBlockInputType1, DevicePropertiesEditor.this.dataBlockFormatType1);
								if (DevicePropertiesEditor.this.dataBlockFormatType1 == FormatTypes.BINARY) {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockSeparator(null);
								}
								else {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockSeparator(DevicePropertiesEditor.this.dataBlockSeparator);
								}
								DevicePropertiesEditor.this.enableSaveButton(true);
							}
						}
					});
				}
				{
					this.dataBlockInputCombo2 = new CCombo(this.dataBlockRequiredGroup, SWT.BORDER);
					this.dataBlockInputCombo2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockInputCombo2.setBounds(175, GDE.IS_MAC_COCOA ? 15 : 30, 90, 20);
					this.dataBlockInputCombo2.setItems(InputTypes.valuesAsStingArray());
					this.dataBlockInputCombo2.setBackground(this.application.COLOR_WHITE);
					this.dataBlockInputCombo2.setLayout(null);
					this.dataBlockInputCombo2.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBlockFormatCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockFormatType2 = FormatTypes.valueOf(DevicePropertiesEditor.this.dataBlockFormatCombo2.getText());
							DevicePropertiesEditor.this.dataBlockInputType2 = InputTypes.valueOf(DevicePropertiesEditor.this.dataBlockInputCombo2.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockFormat(DevicePropertiesEditor.this.dataBlockInputType2, DevicePropertiesEditor.this.dataBlockFormatType2);
								if (DevicePropertiesEditor.this.dataBlockFormatType2 == FormatTypes.BINARY) {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockSeparator(null);
								}
								else {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockSeparator(DevicePropertiesEditor.this.dataBlockSeparator);
								}
								DevicePropertiesEditor.this.enableSaveButton(true);
							}
						}
					});
				}
				{
					this.dataBlockFormatLabel = new Label(this.dataBlockRequiredGroup, SWT.RIGHT);
					this.dataBlockFormatLabel.setText(Messages.getString(MessageIds.GDE_MSGT0518));
					this.dataBlockFormatLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockFormatLabel.setBounds(15, GDE.IS_MAC_COCOA ? 45 : 60, 60, 20);
				}
				{
					this.dataBlockFormatCombo1 = new CCombo(this.dataBlockRequiredGroup, SWT.BORDER);
					this.dataBlockFormatCombo1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockFormatCombo1.setBounds(80, GDE.IS_MAC_COCOA ? 45 : 60, 90, 20);
					this.dataBlockFormatCombo1.setItems(FormatTypes.valuesAsStingArray());
					this.dataBlockFormatCombo1.setBackground(this.application.COLOR_WHITE);
					this.dataBlockFormatCombo1.setLayout(null);
					this.dataBlockFormatCombo1.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBlockFormatCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockFormatType1 = FormatTypes.valueOf(DevicePropertiesEditor.this.dataBlockFormatCombo1.getText());
							DevicePropertiesEditor.this.dataBlockInputType1 = InputTypes.valueOf(DevicePropertiesEditor.this.dataBlockInputCombo1.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockFormat(DevicePropertiesEditor.this.dataBlockInputType1, DevicePropertiesEditor.this.dataBlockFormatType1);
								if (DevicePropertiesEditor.this.dataBlockFormatType1 == FormatTypes.BINARY) {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockSeparator(null);
								}
								else {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockSeparator(DevicePropertiesEditor.this.dataBlockSeparator);
								}
								DevicePropertiesEditor.this.enableSaveButton(true);
							}
						}
					});
				}
				{
					this.dataBlockFormatCombo2 = new CCombo(this.dataBlockRequiredGroup, SWT.BORDER);
					this.dataBlockFormatCombo2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockFormatCombo2.setBounds(175, GDE.IS_MAC_COCOA ? 45 : 60, 90, 20);
					this.dataBlockFormatCombo2.setItems(FormatTypes.valuesAsStingArray());
					this.dataBlockFormatCombo2.setBackground(this.application.COLOR_WHITE);
					this.dataBlockFormatCombo2.setLayout(null);
					this.dataBlockFormatCombo2.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBlockFormatCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockFormatType2 = FormatTypes.valueOf(DevicePropertiesEditor.this.dataBlockFormatCombo2.getText());
							DevicePropertiesEditor.this.dataBlockInputType2 = InputTypes.valueOf(DevicePropertiesEditor.this.dataBlockInputCombo2.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockFormat(DevicePropertiesEditor.this.dataBlockInputType2, DevicePropertiesEditor.this.dataBlockFormatType2);
								if (DevicePropertiesEditor.this.dataBlockFormatType2 == FormatTypes.BINARY) {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockSeparator(null);
								}
								else {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockSeparator(DevicePropertiesEditor.this.dataBlockSeparator);
								}
								DevicePropertiesEditor.this.enableSaveButton(true);
							}
						}
					});
				}
				{
					this.dataBlockSizeLabel = new Label(this.dataBlockRequiredGroup, SWT.RIGHT);
					this.dataBlockSizeLabel.setText(Messages.getString(MessageIds.GDE_MSGT0520));
					this.dataBlockSizeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockSizeLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0521));
					this.dataBlockSizeLabel.setBounds(15, GDE.IS_MAC_COCOA ? 75 : 90, 60, 20);
				}
				{
					this.dataBlockSizeText1 = new Text(this.dataBlockRequiredGroup, SWT.RIGHT | SWT.BORDER);
					this.dataBlockSizeText1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockSizeText1.setBounds(80, GDE.IS_MAC_COCOA ? 75 : 90, 70, 20);
					this.dataBlockSizeText1.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBlockSizeText.keyReleased, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockSize1 = Integer.parseInt(DevicePropertiesEditor.this.dataBlockSizeText1.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.dataBlockFormatType1 = FormatTypes.valueOf(DevicePropertiesEditor.this.dataBlockFormatCombo1.getText());
								DevicePropertiesEditor.this.dataBlockInputType1 = InputTypes.valueOf(DevicePropertiesEditor.this.dataBlockInputCombo1.getText());
								DevicePropertiesEditor.this.deviceConfig.setDataBlockSize(DevicePropertiesEditor.this.dataBlockInputType1, DevicePropertiesEditor.this.dataBlockFormatType1, DevicePropertiesEditor.this.dataBlockSize1);
								DevicePropertiesEditor.this.enableSaveButton(true);
							}
						}
					});
					this.dataBlockSizeText1.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBlockSizeText.verifyText, event=" + evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
				{
					this.dataBlockSizeText2 = new Text(this.dataBlockRequiredGroup, SWT.RIGHT | SWT.BORDER);
					this.dataBlockSizeText2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockSizeText2.setBounds(175, GDE.IS_MAC_COCOA ? 75 : 90, 70, 20);
					this.dataBlockSizeText2.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBlockSizeText.keyReleased, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockSize2 = Integer.parseInt(DevicePropertiesEditor.this.dataBlockSizeText2.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.dataBlockFormatType2 = FormatTypes.valueOf(DevicePropertiesEditor.this.dataBlockFormatCombo2.getText());
								DevicePropertiesEditor.this.dataBlockInputType2 = InputTypes.valueOf(DevicePropertiesEditor.this.dataBlockInputCombo2.getText());
								DevicePropertiesEditor.this.deviceConfig.setDataBlockSize(DevicePropertiesEditor.this.dataBlockInputType2, DevicePropertiesEditor.this.dataBlockFormatType2, DevicePropertiesEditor.this.dataBlockSize2);
								DevicePropertiesEditor.this.enableSaveButton(true);
							}
						}
					});
					this.dataBlockSizeText2.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBlockSizeText.verifyText, event=" + evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
				{
					this.dataBlockTimeUnitLabel = new Label(this.dataBlockRequiredGroup, SWT.RIGHT);
					this.dataBlockTimeUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT0592));
					this.dataBlockTimeUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockTimeUnitLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0593));
					this.dataBlockTimeUnitLabel.setBounds(25, GDE.IS_MAC_COCOA ? 105 : 120, 100, 20);
				}
				{
					this.dataBlockTimeUnitCombo = new CCombo(this.dataBlockRequiredGroup, SWT.BORDER);
					this.dataBlockTimeUnitCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockTimeUnitCombo.setBounds(130, GDE.IS_MAC_COCOA ? 105 : 120, 60, 20);
					this.dataBlockTimeUnitCombo.setItems(TimeUnitTypes.valuesAsStingArray());
					this.dataBlockTimeUnitCombo.setEditable(false);
					this.dataBlockTimeUnitCombo.setBackground(this.application.COLOR_WHITE);
					this.dataBlockTimeUnitCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBlockTimeUnitCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockTimeUnit = TimeUnitTypes.fromValue(DevicePropertiesEditor.this.dataBlockTimeUnitCombo.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockTimeUnit(DevicePropertiesEditor.this.dataBlockTimeUnit);
								DevicePropertiesEditor.this.enableSaveButton(true);
							}
						}
					});
				}
				{
					this.dataBlockSeparatorLabel = new Label(this.dataBlockRequiredGroup, SWT.RIGHT);
					this.dataBlockSeparatorLabel.setText(Messages.getString(MessageIds.GDE_MSGT0476));
					this.dataBlockSeparatorLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockSeparatorLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0329));
					this.dataBlockSeparatorLabel.setBounds(25, GDE.IS_MAC_COCOA ? 135 : 150, 100, 20);
				}
				{
					this.dataBlockSeparatorCombo = new CCombo(this.dataBlockRequiredGroup, SWT.BORDER);
					this.dataBlockSeparatorCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD, false, false));
					this.dataBlockSeparatorCombo.setBounds(130, GDE.IS_MAC_COCOA ? 135 : 150, 40, 20);
					this.dataBlockSeparatorCombo.setItems(CommaSeparatorTypes.valuesAsStingArray());
					this.dataBlockSeparatorCombo.setEditable(false);
					this.dataBlockSeparatorCombo.setBackground(this.application.COLOR_WHITE);
					this.dataBlockSeparatorCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBlockCommaSeparatorCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockSeparator = CommaSeparatorTypes.fromValue(DevicePropertiesEditor.this.dataBlockSeparatorCombo.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockSeparator(DevicePropertiesEditor.this.dataBlockSeparator);
								DevicePropertiesEditor.this.enableSaveButton(true);
							}
						}
					});
				}
			}
			{
				this.dataBlockLeaderLabel = new Label(this.dataBlockRequiredGroup, SWT.RIGHT);
				this.dataBlockLeaderLabel.setText(Messages.getString(MessageIds.GDE_MSGT0469));
				this.dataBlockLeaderLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.dataBlockLeaderLabel.setBounds(25, GDE.IS_MAC_COCOA ? 165 : 180, 100, 20);
			}
			{
				this.dataBlockLeaderText = new Text(this.dataBlockRequiredGroup, SWT.BORDER);
				this.dataBlockLeaderText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.dataBlockLeaderText.setBackground(this.application.COLOR_WHITE);
				this.dataBlockLeaderText.setBounds(130, GDE.IS_MAC_COCOA ? 165 : 180, 30, 20);
				this.dataBlockLeaderText.setText(GDE.STRING_BLANK + DevicePropertiesEditor.this.dataBlockLeader);
				this.dataBlockLeaderText.setEnabled(false);
				this.dataBlockLeaderText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "dataBlockLeaderText.keyReleased, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.dataBlockLeader = DevicePropertiesEditor.this.dataBlockLeaderText.getText().trim();
						if (DevicePropertiesEditor.this.deviceConfig != null) {
							DevicePropertiesEditor.this.deviceConfig.setDataBlockLeader(DevicePropertiesEditor.this.dataBlockLeader);
							DevicePropertiesEditor.this.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.dataBlockEndingLabel = new Label(this.dataBlockRequiredGroup, SWT.RIGHT);
				this.dataBlockEndingLabel.setText(Messages.getString(MessageIds.GDE_MSGT0468));
				this.dataBlockEndingLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0475));
				this.dataBlockEndingLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.dataBlockEndingLabel.setBounds(25, GDE.IS_MAC_COCOA ? 195 : 210, 100, 20);
			}
			{
				this.dataBlockEndingCombo = new CCombo(this.dataBlockRequiredGroup, SWT.BORDER);
				this.dataBlockEndingCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.dataBlockEndingCombo.setBackground(this.application.COLOR_WHITE);
				this.dataBlockEndingCombo.setBounds(130, GDE.IS_MAC_COCOA ? 195 : 210, 90, 20);
				this.dataBlockEndingCombo.setItems(LineEndingTypes.valuesAsStingArray());
				this.dataBlockEndingCombo.setEnabled(false);
				this.dataBlockEndingCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "dataBlockEndingText.selected, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.dataBlockEnding = DevicePropertiesEditor.this.dataBlockEndingCombo.getText();
						if (DevicePropertiesEditor.this.deviceConfig != null) {
							DevicePropertiesEditor.this.deviceConfig.setDataBlockEnding(DevicePropertiesEditor.this.dataBlockEnding);
							DevicePropertiesEditor.this.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.dataBlockOptionalGroup = new Group(this.dataBlockComposite, SWT.NONE);
				this.dataBlockOptionalGroup.setLayout(null);
				this.dataBlockOptionalGroup.setText(Messages.getString(MessageIds.GDE_MSGT0522));
				this.dataBlockOptionalGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.dataBlockOptionalGroup.setBounds(300, 80, 360, 170);
				{
					this.dataBlockCheckSumFormatButton = new Button(this.dataBlockOptionalGroup, SWT.CHECK | SWT.RIGHT);
					this.dataBlockCheckSumFormatButton.setText(Messages.getString(MessageIds.GDE_MSGT0466));
					this.dataBlockCheckSumFormatButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockCheckSumFormatButton.setBounds(10, GDE.IS_MAC_COCOA ? 15 : 30, 120, 20);
					this.dataBlockCheckSumFormatButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBlockCheckSumFormatButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled = DevicePropertiesEditor.this.dataBlockCheckSumFormatButton.getSelection();
							enableDataBlockOptionalChecksumPart(DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled);
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								if (DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled) {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockCheckSumFormat(DevicePropertiesEditor.this.dataBlockcheckSumFormat);
									DevicePropertiesEditor.this.deviceConfig.setDataBlockCheckSumType(DevicePropertiesEditor.this.dataBlockCheckSumType);
								}
								else {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockCheckSumFormat(null);
									DevicePropertiesEditor.this.deviceConfig.setDataBlockCheckSumType(null);
								}
								DevicePropertiesEditor.this.enableSaveButton(true);
							}
						}
					});
				}
				{
					this.dataBlockcheckSumFormatCombo = new CCombo(this.dataBlockOptionalGroup, SWT.BORDER);
					this.dataBlockcheckSumFormatCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockcheckSumFormatCombo.setItems(StringHelper.enumValues2StringArray(FormatTypes.values()));
					this.dataBlockcheckSumFormatCombo.setBounds(140, GDE.IS_MAC_COCOA ? 15 : 30, 90, 20);
					this.dataBlockcheckSumFormatCombo.setEditable(false);
					this.dataBlockcheckSumFormatCombo.setEnabled(false);
					this.dataBlockcheckSumFormatCombo.select(1);
					this.dataBlockcheckSumFormatCombo.setBackground(this.application.COLOR_WHITE);
					this.dataBlockcheckSumFormatCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBlockceckSumFormatCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockcheckSumFormat = FormatTypes.valueOf(DevicePropertiesEditor.this.dataBlockcheckSumFormatCombo.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockCheckSumFormat(DevicePropertiesEditor.this.dataBlockcheckSumFormat);
								DevicePropertiesEditor.this.enableSaveButton(true);
							}
						}
					});
				}
				{
					this.dataBlockCheckSumTypeLabel = new Label(this.dataBlockOptionalGroup, SWT.CHECK | SWT.RIGHT);
					this.dataBlockCheckSumTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT0467));
					this.dataBlockCheckSumTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockCheckSumTypeLabel.setBounds(10, GDE.IS_MAC_COCOA ? 45 : 60, 120, 20);
				}
				{
					this.dataBlockCheckSumTypeCombo = new CCombo(this.dataBlockOptionalGroup, SWT.RIGHT | SWT.BORDER);
					this.dataBlockCheckSumTypeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockCheckSumTypeCombo.setItems(StringHelper.enumValues2StringArray(CheckSumTypes.values()));
					this.dataBlockCheckSumTypeCombo.setBounds(140, GDE.IS_MAC_COCOA ? 45 : 60, 90, 20);
					this.dataBlockCheckSumTypeCombo.setEditable(false);
					this.dataBlockCheckSumTypeCombo.setEnabled(false);
					this.dataBlockCheckSumTypeCombo.select(1);
					this.dataBlockCheckSumTypeCombo.setBackground(this.application.COLOR_WHITE);
					this.dataBlockCheckSumTypeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "dataBlockCheckSumCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockCheckSumType = CheckSumTypes.valueOf(DevicePropertiesEditor.this.dataBlockCheckSumTypeCombo.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockCheckSumType(DevicePropertiesEditor.this.dataBlockCheckSumType);
								DevicePropertiesEditor.this.enableSaveButton(true);
							}
						}
					});
				}
				{
					this.preferredDataLocationButton = new Button(this.dataBlockOptionalGroup, SWT.CHECK | SWT.RIGHT);
					this.preferredDataLocationButton.setText(Messages.getString(MessageIds.GDE_MSGT0523));
					this.preferredDataLocationButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0524));
					this.preferredDataLocationButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.preferredDataLocationButton.setBounds(10, GDE.IS_MAC_COCOA ? 85 : 100, 120, 20);
					this.preferredDataLocationButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "preferredDataLocationButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.isDataBlockOptionalDataLocationEnabled = DevicePropertiesEditor.this.preferredDataLocationButton.getSelection();
							DevicePropertiesEditor.this.preferredDataLocationText.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalDataLocationEnabled);
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								if (DevicePropertiesEditor.this.isDataBlockOptionalDataLocationEnabled) {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockPreferredDataLocation(DevicePropertiesEditor.this.dataBlockOptionalDataLocation);
								}
								else {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockPreferredDataLocation(null);
								}
								DevicePropertiesEditor.this.enableSaveButton(true);
							}
						}
					});
				}
				{
					this.preferredDataLocationText = new Text(this.dataBlockOptionalGroup, SWT.BORDER | SWT.SCROLL_LINE);
					this.preferredDataLocationText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.preferredDataLocationText.setBounds(140, GDE.IS_MAC_COCOA ? 85 : 100, 210, 20);
					this.preferredDataLocationText.setEnabled(false);
					this.preferredDataLocationText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent evt) {
							log.log(java.util.logging.Level.FINEST, "preferredDataLocationText.keyReleased, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockOptionalDataLocation = DevicePropertiesEditor.this.preferredDataLocationText.getText();
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockPreferredDataLocation(DevicePropertiesEditor.this.dataBlockOptionalDataLocation);
								DevicePropertiesEditor.this.enableSaveButton(true);
							}
						}
					});
					this.preferredDataLocationText.addVerifyListener(new VerifyListener() {
						@Override
						public void verifyText(VerifyEvent evt) {
							log.log(java.util.logging.Level.FINEST, "preferredDataLocationText.verifyText, event=" + evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyTypedInput(DataTypes.STRING, evt.text);
						}
					});
				}
			}
			{
				this.preferredFileExtensionButton = new Button(this.dataBlockOptionalGroup, SWT.CHECK | SWT.RIGHT);
				this.preferredFileExtensionButton.setText(Messages.getString(MessageIds.GDE_MSGT0508));
				this.preferredFileExtensionButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0509));
				this.preferredFileExtensionButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.preferredFileExtensionButton.setBounds(10, GDE.IS_MAC_COCOA ? 115 : 130, 120, 20);
				this.preferredFileExtensionButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "preferredFileExtensionButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.isDataBlockOptionalFileExtentionEnabled = DevicePropertiesEditor.this.preferredFileExtensionButton.getSelection();
						DevicePropertiesEditor.this.preferredFileExtensionText.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalFileExtentionEnabled);
						DevicePropertiesEditor.this.dataBlockOptionalFileExtention = DevicePropertiesEditor.this.preferredFileExtensionText.getText();
						if (DevicePropertiesEditor.this.deviceConfig != null) {
							if (DevicePropertiesEditor.this.isDataBlockOptionalFileExtentionEnabled) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockPreferredFileExtention(DevicePropertiesEditor.this.dataBlockOptionalFileExtention);
							}
							else {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockPreferredFileExtention(null);
							}
							DevicePropertiesEditor.this.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.preferredFileExtensionText = new Text(this.dataBlockOptionalGroup, SWT.BORDER);
				this.preferredFileExtensionText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.preferredFileExtensionText.setBounds(140, GDE.IS_MAC_COCOA ? 115 : 130, 90, 20);
				this.preferredFileExtensionText.setEnabled(false);
				this.preferredFileExtensionText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "preferredFileExtensionText.keyReleased, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.dataBlockOptionalFileExtention = DevicePropertiesEditor.this.preferredFileExtensionText.getText();
						if (DevicePropertiesEditor.this.deviceConfig != null) {
							DevicePropertiesEditor.this.deviceConfig.setDataBlockPreferredFileExtention(DevicePropertiesEditor.this.dataBlockOptionalFileExtention);
							DevicePropertiesEditor.this.enableSaveButton(true);
						}
					}
				});
				this.preferredFileExtensionText.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "preferredFileExtensionText.verifyText, event=" + evt); //$NON-NLS-1$
						evt.doit = StringHelper.verifyTypedInput(DataTypes.STRING, evt.text);
					}
				});
			}
		}

		if (this.deviceConfig != null) this.deviceConfig.addDataBlockType();
	}

	/**
	 * create a new mode state tabulator with one mode state entry
	 */
	public void createStateTabItem() {
		int index = 0;
		for (CTabItem tabItem : this.tabFolder.getItems()) {
			if (tabItem.getText().equals(Messages.getString(MessageIds.GDE_MSGT0503))) break;
			++index;
		}
		this.stateTabItem = new CTabItem(this.tabFolder, SWT.CLOSE, index);
		SWTResourceManager.registerResourceUser(this.stateTabItem);
		this.stateTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0470));
		this.stateTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
		{
			this.stateComposite = new Composite(this.tabFolder, SWT.NONE);
			this.stateComposite.setLayout(null);
			this.stateTabItem.setControl(this.stateComposite);
			this.stateComposite.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "stateComposite.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_A1.html#device_properties_state"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			{
				this.stateDescriptionLabel = new Label(this.stateComposite, SWT.LEFT);
				this.stateDescriptionLabel.setText(Messages.getString(MessageIds.GDE_MSGT0471));
				this.stateDescriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.stateDescriptionLabel.setBounds(165, 4, 449, 55);
			}
			{
				this.stateTabFolder = new CTabFolder(this.stateComposite, SWT.BORDER);
				this.stateTabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.stateTabFolder.setBounds(165, 65, 300, 207);
				{
					createStateTypeProperty();
				}
				this.stateTabFolder.setSelection(0);
				this.stateTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
					@Override
					public void close(CTabFolderEvent evt) {
						log.log(java.util.logging.Level.FINEST, "stateTabFolder.close, event=" + evt); //$NON-NLS-1$
						PropertyTypeTabItem tmpTabItem = (PropertyTypeTabItem) evt.item;
						if (DevicePropertiesEditor.this.deviceConfig != null) {
							int childIndex = DevicePropertiesEditor.this.deviceConfig.getStateType().getProperty().indexOf(tmpTabItem.propertyType);
							DevicePropertiesEditor.this.deviceConfig.removeStateType(DevicePropertiesEditor.this.deviceConfig.getStateType().getProperty().get(childIndex));
						}
						tmpTabItem.dispose();
					}
				});
			}
			{
				this.addButton = new Button(this.stateComposite, SWT.PUSH | SWT.CENTER);
				this.addButton.setText(Messages.getString(MessageIds.GDE_MSGT0472));
				this.addButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.addButton.setBounds(165, 284, 300, 30);
				this.addButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "addButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						{
							createStateTypeProperty();
							DevicePropertiesEditor.this.stateTabFolder.setSelection(DevicePropertiesEditor.this.stateTabFolder.getItemCount() - 1);
						}
					}
				});
			}
		}
	}

	/**
	 * creates a new state type property and display it as a PropertyTabItem
	 * appends the PropertyType to device configuration XML if necessary
	 */
	private void createStateTypeProperty() {
		PropertyType property = new ObjectFactory().createPropertyType();
		property.setName(Messages.getString(MessageIds.GDE_MSGT0473));
		property.setType(DataTypes.INTEGER);
		property.setValue(GDE.STRING_EMPTY + (this.stateTabFolder.getItemCount() + 1));
		property.setDescription(Messages.getString(MessageIds.GDE_MSGT0474));
		PropertyTypeTabItem tmpPropertyTypeTabItem = new PropertyTypeTabItem(DevicePropertiesEditor.this.stateTabFolder, SWT.CLOSE, Messages.getString(MessageIds.GDE_MSGT0470), null);
		if (this.deviceConfig != null) {
			this.deviceConfig.appendStateType(property);
		}
		boolean isNoneSpecified = MeasurementPropertyTypes.isNoneSpecified(property.getName());
		tmpPropertyTypeTabItem.setProperty(this.deviceConfig, property, isNoneSpecified, isNoneSpecified ? MeasurementPropertyTypes.valuesAsStingArray() : null, isNoneSpecified ? DataTypes
				.valuesAsStingArray() : null, false);
	}

	/**
	 * create a new serial port tabulator
	 */
	public void createSerialPortTabItem() {
		this.serialPortTabItem = new SeriaPortTypeTabItem(this.tabFolder, SWT.CLOSE, 1);
		this.serialPortTabItem.setDeviceConfig(this.deviceConfig);
	}

	/**
	 * query the Devices path to open, query, save the device properties
	 * @return
	 */
	private String getDevicesPath() {
		String applHomePath = GDE.STRING_EMPTY;
		if (GDE.IS_WINDOWS) {
			applHomePath = (System.getenv("APPDATA") + GDE.STRING_FILE_SEPARATOR_UNIX + GDE.NAME_LONG + GDE.STRING_FILE_SEPARATOR_UNIX).replace("\\", GDE.STRING_FILE_SEPARATOR_UNIX); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else if (GDE.IS_LINUX) {
			applHomePath = System.getProperty("user.home") + GDE.STRING_FILE_SEPARATOR_UNIX + "." + GDE.NAME_LONG + GDE.STRING_FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
		}
		else if (GDE.IS_MAC) {
			applHomePath = System.getProperty("user.home") + GDE.STRING_FILE_SEPARATOR_UNIX + "Library" + GDE.STRING_FILE_SEPARATOR_UNIX + "Application Support" + GDE.STRING_FILE_SEPARATOR_UNIX + GDE.NAME_LONG + GDE.STRING_FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			log.log(Level.SEVERE, Messages.getString(MessageIds.GDE_MSGW0001));
		}
		log.log(Level.FINE, "DevicesPath = " + applHomePath + "Devices");
	return applHomePath + "Devices"; //$NON-NLS-1$
	}

	/**
	 * update internal variables by device properties
	 */
	@Override
	public void update() {
		//SWTResourceManager.listResourceStatus();
		//DeviceType begin
		this.deviceName = this.deviceConfig.getName();
		this.isDeviceImplementaionClass = !this.deviceConfig.getDeviceImplName().equals(this.deviceName);
		this.deviceImplementationClass = this.deviceConfig.getDeviceImplName();
		this.manufacturer = this.deviceConfig.getManufacturer();
		this.manufacuturerURL = this.deviceConfig.getManufacturerURL();
		this.imageFileName = this.deviceConfig.getImageFileName();
		this.isDeviceUsed = this.deviceConfig.isUsed();
		this.deviceGroup = this.deviceConfig.getDeviceGroup();
		initializeDeviceProperties();
		//DeviceType end

		GDE.display.asyncExec( new Runnable() {
			@Override
			public void run() {
				try {
					if (DevicePropertiesEditor.dialogShell != null && !DevicePropertiesEditor.dialogShell.isDisposed()) {
						DevicePropertiesEditor.dialogShell.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));
						//SerialPortType begin
						if (DevicePropertiesEditor.this.deviceConfig.getSerialPortType() == null && DevicePropertiesEditor.this.serialPortTabItem != null
								&& !DevicePropertiesEditor.this.serialPortTabItem.isDisposed()) {
							DevicePropertiesEditor.this.serialPortTabItem.dispose();
							DevicePropertiesEditor.this.serialPortTabItem = null;
						}
						else if (DevicePropertiesEditor.this.deviceConfig.getSerialPortType() != null
								&& (DevicePropertiesEditor.this.serialPortTabItem == null || DevicePropertiesEditor.this.serialPortTabItem.isDisposed())) {
							DevicePropertiesEditor.this.serialPortTabItem = new SeriaPortTypeTabItem(DevicePropertiesEditor.this.tabFolder, SWT.CLOSE, 1);
						}
						if (DevicePropertiesEditor.this.deviceConfig.getSerialPortType() != null && DevicePropertiesEditor.this.serialPortTabItem != null
								&& !DevicePropertiesEditor.this.serialPortTabItem.isDisposed()) {
							DevicePropertiesEditor.this.serialPortTabItem.setDeviceConfig(DevicePropertiesEditor.this.deviceConfig);
						}
						//SerialPortType end
						//TimeBaseType begin
						DevicePropertiesEditor.this.timeStep_ms = DevicePropertiesEditor.this.deviceConfig.getTimeStep_ms();
						DevicePropertiesEditor.this.utcDelta = DevicePropertiesEditor.this.deviceConfig.getUTCdelta();
						initializeTimeBase();
						//TimeBaseType end
						//DataBlockType begin
						if (DevicePropertiesEditor.this.deviceConfig.getDataBlockType() == null && DevicePropertiesEditor.this.dataBlockTabItem != null
								&& !DevicePropertiesEditor.this.dataBlockTabItem.isDisposed()) {
							DevicePropertiesEditor.this.dataBlockTabItem.dispose();
							DevicePropertiesEditor.this.dataBlockTabItem = null;
						}
						else {
							if (DevicePropertiesEditor.this.deviceConfig.getDataBlockType() != null) {
								if (DevicePropertiesEditor.this.dataBlockTabItem != null && DevicePropertiesEditor.this.dataBlockTabItem.isDisposed()) {
									createDataBlockType();
								}
								if (DevicePropertiesEditor.this.deviceConfig.getDataBlockType().getFormat().size() >= 1) {
									DataBlockType.Format dataBLockFormat0 = DevicePropertiesEditor.this.deviceConfig.getDataBlockType().getFormat().get(0);
									DevicePropertiesEditor.this.dataBlockInputType1 = dataBLockFormat0.getInputType();
									DevicePropertiesEditor.this.dataBlockFormatType1 = dataBLockFormat0.getType();
									DevicePropertiesEditor.this.dataBlockSize1 = dataBLockFormat0.getSize();
									if (DevicePropertiesEditor.this.deviceConfig.getDataBlockType().getFormat().size() == 2) {
										DataBlockType.Format dataBLockFormat1 = DevicePropertiesEditor.this.deviceConfig.getDataBlockType().getFormat().get(1);
										DevicePropertiesEditor.this.dataBlockInputType2 = dataBLockFormat1.getInputType();
										DevicePropertiesEditor.this.dataBlockFormatType2 = dataBLockFormat1.getType();
										DevicePropertiesEditor.this.dataBlockSize2 = dataBLockFormat1.getSize();
									}
								}
								DevicePropertiesEditor.this.dataBlockSeparator = DevicePropertiesEditor.this.deviceConfig.getDataBlockSeparator();
								DevicePropertiesEditor.this.dataBlockTimeUnit = DevicePropertiesEditor.this.deviceConfig.getDataBlockTimeUnit();
								DevicePropertiesEditor.this.dataBlockLeader = DevicePropertiesEditor.this.deviceConfig.getDataBlockLeader();
								DevicePropertiesEditor.this.dataBlockEnding = DevicePropertiesEditor.this.deviceConfig.getDataBlockEndingLineEndingType();

								DevicePropertiesEditor.this.dataBlockcheckSumFormat = DevicePropertiesEditor.this.deviceConfig.getDataBlockCheckSumFormat();
								DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled = DevicePropertiesEditor.this.deviceConfig.isDataBlockCheckSumDefined();
								DevicePropertiesEditor.this.dataBlockCheckSumType = DevicePropertiesEditor.this.deviceConfig.getDataBlockCheckSumType();

								DevicePropertiesEditor.this.dataBlockOptionalDataLocation = DevicePropertiesEditor.this.deviceConfig.getDataBlockPreferredDataLocation();
								DevicePropertiesEditor.this.isDataBlockOptionalDataLocationEnabled = DevicePropertiesEditor.this.dataBlockOptionalDataLocation.length() > 1;

								DevicePropertiesEditor.this.dataBlockOptionalFileExtention = DevicePropertiesEditor.this.deviceConfig.getDataBlockPreferredFileExtention();
								DevicePropertiesEditor.this.isDataBlockOptionalFileExtentionEnabled = DevicePropertiesEditor.this.deviceConfig.isDataBlockPreferredFileExtentionDefined();
							}
						}
						initializeDataBlock();
						//DataBlockType end
						//StateType begin
						int stateCount = (DevicePropertiesEditor.this.deviceConfig.getStateType() == null) ? 0 : DevicePropertiesEditor.this.deviceConfig.getStateSize();
						if (DevicePropertiesEditor.this.deviceConfig.getStateType() == null
								|| (stateCount == 0 && (DevicePropertiesEditor.this.deviceConfig.getStateType() != null && !DevicePropertiesEditor.this.stateTabFolder.isDisposed()))) {
							if (DevicePropertiesEditor.this.stateTabItem != null) {
								for (CTabItem tmpPropertyTabItem : DevicePropertiesEditor.this.stateTabFolder.getItems()) {
									((PropertyTypeTabItem) tmpPropertyTabItem).dispose();
								}
								DevicePropertiesEditor.this.stateTabItem.dispose();
								DevicePropertiesEditor.this.stateTabItem = null;
							}
						}
						else {
							if (DevicePropertiesEditor.this.deviceConfig.getStateType() != null && (DevicePropertiesEditor.this.stateTabItem == null || DevicePropertiesEditor.this.stateTabItem.isDisposed())) {
								createStateTabItem();
							}
							if (DevicePropertiesEditor.this.deviceConfig.getStateType() != null && DevicePropertiesEditor.this.stateTabItem != null && !DevicePropertiesEditor.this.stateTabItem.isDisposed()) {
								if (stateCount > DevicePropertiesEditor.this.stateTabFolder.getItemCount()) {
									for (int i = DevicePropertiesEditor.this.stateTabFolder.getItemCount(); i < DevicePropertiesEditor.this.deviceConfig.getStateSize(); i++) {
										new PropertyTypeTabItem(DevicePropertiesEditor.this.stateTabFolder, SWT.CLOSE, Messages.getString(MessageIds.GDE_MSGT0470)
												+ DevicePropertiesEditor.this.stateTabFolder.getItemCount(), null);
									}
								}
								else if (stateCount < DevicePropertiesEditor.this.stateTabFolder.getItemCount()) {
									CTabItem[] childs = DevicePropertiesEditor.this.stateTabFolder.getItems();
									for (int i = stateCount - 1; i < childs.length; i++) {
										((PropertyTypeTabItem) childs[i]).dispose();
									}
								}
								int index = 1;
								for (CTabItem child : DevicePropertiesEditor.this.stateTabFolder.getItems()) {
									if (DevicePropertiesEditor.this.deviceConfig.getStateProperty(index) != null) {
										((PropertyTypeTabItem) child).setProperty(DevicePropertiesEditor.this.deviceConfig, DevicePropertiesEditor.this.deviceConfig.getStateProperty(index++), true, null,
												new String[] { DataTypes.INTEGER.value() }, false);
									}
								}
								DevicePropertiesEditor.this.stateTabFolder.setSelection(0);
							}
						}
						//StateType end
						//ChannelType begin
						int channelTypeCount = DevicePropertiesEditor.this.deviceConfig.getChannelCount();
						int actualTabItemCount = DevicePropertiesEditor.this.channelConfigInnerTabFolder.getItemCount();
						if (channelTypeCount < actualTabItemCount) {
							for (int i = channelTypeCount; i < actualTabItemCount; i++) {
								ChannelTypeTabItem tmpChannelTabItem = (ChannelTypeTabItem) DevicePropertiesEditor.this.channelConfigInnerTabFolder.getItem(channelTypeCount);
								for (CTabItem tmpMeasurementTypeTabItem : tmpChannelTabItem.measurementsTabFolder.getItems()) {
									if (((MeasurementTypeTabItem) tmpMeasurementTypeTabItem).measurementPropertiesTabFolder != null) { // dispose PropertyTypes
										for (CTabItem tmpPropertyTypeTabItem : ((MeasurementTypeTabItem) tmpMeasurementTypeTabItem).measurementPropertiesTabFolder.getItems()) {
											((PropertyTypeTabItem) tmpPropertyTypeTabItem).dispose();
										}
										((MeasurementTypeTabItem) tmpMeasurementTypeTabItem).measurementPropertiesTabItem.dispose();
									}
									if (((MeasurementTypeTabItem) tmpMeasurementTypeTabItem).statisticsTypeTabItem != null) { // dispose StatisticsType
										((MeasurementTypeTabItem) tmpMeasurementTypeTabItem).statisticsTypeTabItem.dispose();
									}
									tmpMeasurementTypeTabItem.dispose();
								}
								tmpChannelTabItem.dispose();
							}
						}
						else if (channelTypeCount > actualTabItemCount) {
							for (int i = actualTabItemCount; i < channelTypeCount; i++) {
								new ChannelTypeTabItem(DevicePropertiesEditor.this.channelConfigInnerTabFolder, SWT.NONE, i);
							}
						}
						int itemCount = DevicePropertiesEditor.this.channelConfigInnerTabFolder.getItemCount();
						if (itemCount > 1) DevicePropertiesEditor.this.channelConfigInnerTabFolder.getItem(itemCount - 1).setShowClose(true);
						for (int i = 0; i < channelTypeCount; i++) {
							ChannelTypeTabItem channelTabItem = (ChannelTypeTabItem) DevicePropertiesEditor.this.channelConfigInnerTabFolder.getItem(i);
							channelTabItem.setChannelType(DevicePropertiesEditor.this.deviceConfig, DevicePropertiesEditor.this.deviceConfig.getChannelType(i + 1), (i + 1));
						}
						//ChannelType end
						//DesktopType begin
						DevicePropertiesEditor.this.desktopInnerTabItem1.setProperty(DevicePropertiesEditor.this.deviceConfig,
								DevicePropertiesEditor.this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.TABLE_TAB));
						DevicePropertiesEditor.this.desktopInnerTabItem2.setProperty(DevicePropertiesEditor.this.deviceConfig,
								DevicePropertiesEditor.this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.DIGITAL_TAB));
						DevicePropertiesEditor.this.desktopInnerTabItem3.setProperty(DevicePropertiesEditor.this.deviceConfig,
								DevicePropertiesEditor.this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.ANALOG_TAB));
						DevicePropertiesEditor.this.desktopInnerTabItem4.setProperty(DevicePropertiesEditor.this.deviceConfig,
								DevicePropertiesEditor.this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB));
						DevicePropertiesEditor.this.desktopInnerTabItem5.setProperty(DevicePropertiesEditor.this.deviceConfig,
								DevicePropertiesEditor.this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.UTILITY_DEVICE_TAB));
						DevicePropertiesEditor.this.desktopInnerTabItem6.setProperty(DevicePropertiesEditor.this.deviceConfig,
								DevicePropertiesEditor.this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.UTILITY_GRAPHICS_TAB));
						//DesktopType end
					}

					//reset possible changes detected by cleanup initial state
					DevicePropertiesEditor.this.deviceConfig.setChangePropery(false);
					DevicePropertiesEditor.this.enableSaveButton(false);
				}
				catch (Throwable e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
				finally {
					if (DevicePropertiesEditor.dialogShell != null && !DevicePropertiesEditor.dialogShell.isDisposed()) {
						DevicePropertiesEditor.this.tabFolder.setSelection(0);
						//SWTResourceManager.listResourceStatus();
						DevicePropertiesEditor.dialogShell.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
						DevicePropertiesEditor.this.enableContextMenu(Messages.getString(MessageIds.GDE_MSGT0487), true); //Device
					}
				}
			}
		});
	}

	/**
	 * enable or disable data block optional checksum properties
	 */
	void enableDataBlockOptionalChecksumPart(boolean enable) {
		this.dataBlockCheckSumFormatButton.setSelection(enable);
		this.dataBlockcheckSumFormatCombo.setEnabled(enable);
		this.dataBlockCheckSumTypeCombo.setEnabled(enable);
	}

	public void openWarningMessageBox(String errorMessage) {
		MessageBox messageDialog = new MessageBox(DevicePropertiesEditor.dialogShell, SWT.OK | SWT.ICON_WARNING);
		if (errorMessage.contains(GDE.STRING_SEMICOLON)) {
			String[] messages = errorMessage.split(GDE.STRING_SEMICOLON);
			messageDialog.setText(messages[0]);
			messageDialog.setMessage(messages[1]);
		}
		else {
			messageDialog.setText(GDE.NAME_LONG);
			messageDialog.setMessage(errorMessage);
		}
		messageDialog.open();

	}

	/**
	 * @param enable = true to set the save button to the enabled state
	 */
	public void enableSaveButton(boolean enable) {
		this.saveButton.setEnabled(enable);
	}

	/**
	 *
	 */
	private void initializeDeviceProperties() {
		DevicePropertiesEditor.this.deviceFileNameText.setText(DevicePropertiesEditor.this.devicePropertiesFileName);
		DevicePropertiesEditor.this.nameText.setText(DevicePropertiesEditor.this.deviceName);
		DevicePropertiesEditor.this.deviceImplementationButton.setSelection(DevicePropertiesEditor.this.isDeviceImplementaionClass);
		DevicePropertiesEditor.this.deviceImplementationText.setText(DevicePropertiesEditor.this.deviceImplementationClass);
		DevicePropertiesEditor.this.deviceImplementationText.setEnabled(DevicePropertiesEditor.this.isDeviceImplementaionClass);
		DevicePropertiesEditor.this.deviceImplementationLabel.setEnabled(DevicePropertiesEditor.this.isDeviceImplementaionClass);
		DevicePropertiesEditor.this.manufURLText.setText(DevicePropertiesEditor.this.manufacuturerURL);
		DevicePropertiesEditor.this.imageFileNameText.setText(DevicePropertiesEditor.this.imageFileName);
		DevicePropertiesEditor.this.manufacturerText.setText(DevicePropertiesEditor.this.manufacturer);
		DevicePropertiesEditor.this.usageButton.setSelection(DevicePropertiesEditor.this.isDeviceUsed);
		DevicePropertiesEditor.this.groupSelectionCombo.select(DevicePropertiesEditor.this.deviceGroup.ordinal());
	}

	/**
	 *
	 */
	private void initializeTimeBase() {
		DevicePropertiesEditor.this.timeBaseTimeStepText.setText(String.format("%.1f", DevicePropertiesEditor.this.timeStep_ms)); //$NON-NLS-1$
		DevicePropertiesEditor.this.timeBaseUtcDeltaText.setText(String.format("%d", DevicePropertiesEditor.this.utcDelta)); //$NON-NLS-1$
	}

	/**
	 *
	 */
	private void initializeDataBlock() {
		DevicePropertiesEditor.this.dataBlockInputCombo1.select(DevicePropertiesEditor.this.dataBlockInputType1 == InputTypes.FILE_IO ? 0 : 1);
		DevicePropertiesEditor.this.dataBlockFormatCombo1.select(DevicePropertiesEditor.this.dataBlockFormatType1 == FormatTypes.BYTE ? 0 : 1);
		DevicePropertiesEditor.this.dataBlockSizeText1.setText(GDE.STRING_EMPTY + DevicePropertiesEditor.this.dataBlockSize1);
		DevicePropertiesEditor.this.dataBlockInputCombo2.select(DevicePropertiesEditor.this.dataBlockInputType2 == InputTypes.FILE_IO ? 0 : 1);
		DevicePropertiesEditor.this.dataBlockFormatCombo2.select(DevicePropertiesEditor.this.dataBlockFormatType2 == FormatTypes.BYTE ? 0 : 1);
		DevicePropertiesEditor.this.dataBlockSizeText2.setText(GDE.STRING_EMPTY + DevicePropertiesEditor.this.dataBlockSize2);

		DevicePropertiesEditor.this.dataBlockSeparatorLabel.setEnabled(true);
		DevicePropertiesEditor.this.dataBlockSeparatorCombo.setEnabled(true);
		DevicePropertiesEditor.this.dataBlockSeparatorCombo.select(DevicePropertiesEditor.this.dataBlockSeparator.ordinal());

		DevicePropertiesEditor.this.dataBlockTimeUnitLabel.setEnabled(true);
		DevicePropertiesEditor.this.dataBlockTimeUnitCombo.setEnabled(true);
		DevicePropertiesEditor.this.dataBlockTimeUnitCombo.select(DevicePropertiesEditor.this.dataBlockTimeUnit.ordinal());

		DevicePropertiesEditor.this.dataBlockLeaderLabel.setEnabled(true);
		DevicePropertiesEditor.this.dataBlockLeaderText.setEnabled(true);
		DevicePropertiesEditor.this.dataBlockLeaderText.setText(GDE.STRING_BLANK + DevicePropertiesEditor.this.dataBlockLeader);

		DevicePropertiesEditor.this.dataBlockEndingLabel.setEnabled(true);
		DevicePropertiesEditor.this.dataBlockEndingCombo.setEnabled(true);
		DevicePropertiesEditor.this.dataBlockEndingCombo.select(LineEndingTypes.fromValue(DevicePropertiesEditor.this.dataBlockEnding).ordinal());

		DevicePropertiesEditor.this.dataBlockCheckSumFormatButton.setSelection(DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled);
		DevicePropertiesEditor.this.dataBlockcheckSumFormatCombo.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled);
		DevicePropertiesEditor.this.dataBlockcheckSumFormatCombo.select(DevicePropertiesEditor.this.dataBlockcheckSumFormat == FormatTypes.BYTE ? 0 : DevicePropertiesEditor.this.dataBlockcheckSumFormat == FormatTypes.VALUE ? 1 : 2);
		DevicePropertiesEditor.this.dataBlockCheckSumTypeCombo.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled);
		DevicePropertiesEditor.this.dataBlockCheckSumTypeCombo.select(DevicePropertiesEditor.this.dataBlockCheckSumType == CheckSumTypes.XOR ? 0 : 1);

		DevicePropertiesEditor.this.preferredDataLocationButton.setSelection(DevicePropertiesEditor.this.isDataBlockOptionalDataLocationEnabled && (DevicePropertiesEditor.this.dataBlockFormatType1 == FormatTypes.VALUE || DevicePropertiesEditor.this.dataBlockFormatType2 == FormatTypes.VALUE));
		DevicePropertiesEditor.this.preferredDataLocationText.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalDataLocationEnabled && (DevicePropertiesEditor.this.dataBlockFormatType1 == FormatTypes.VALUE || DevicePropertiesEditor.this.dataBlockFormatType2 == FormatTypes.VALUE));
		DevicePropertiesEditor.this.preferredDataLocationText.setText(DevicePropertiesEditor.this.dataBlockOptionalDataLocation == null ? GDE.STRING_EMPTY
				: DevicePropertiesEditor.this.dataBlockOptionalDataLocation);
		DevicePropertiesEditor.this.preferredFileExtensionButton.setSelection(DevicePropertiesEditor.this.isDataBlockOptionalFileExtentionEnabled && (DevicePropertiesEditor.this.dataBlockFormatType1 == FormatTypes.VALUE || DevicePropertiesEditor.this.dataBlockFormatType2 == FormatTypes.VALUE));
		DevicePropertiesEditor.this.preferredFileExtensionText.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalFileExtentionEnabled && (DevicePropertiesEditor.this.dataBlockFormatType1 == FormatTypes.VALUE || DevicePropertiesEditor.this.dataBlockFormatType2 == FormatTypes.VALUE));
		DevicePropertiesEditor.this.preferredFileExtensionText.setText(DevicePropertiesEditor.this.dataBlockOptionalFileExtention == null ? GDE.STRING_EMPTY
				: DevicePropertiesEditor.this.dataBlockOptionalFileExtention);
	}

	/**
	 * @return the getMeasurementNames from the given channel/configuration
	 */
	public String[] getMeasurementNames(int channelConfigNumber) {
		return this.deviceConfig != null ? this.deviceConfig.getMeasurementNames(channelConfigNumber): new String[0];
	}
}
