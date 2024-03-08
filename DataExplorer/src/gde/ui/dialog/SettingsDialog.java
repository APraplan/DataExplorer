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

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;
import java.util.Vector;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import gde.DataAccess;
import gde.DataAccess.LocalAccess;
import gde.GDE;
import gde.config.DeviceConfigurations;
import gde.config.Settings;
import gde.device.CommaSeparatorTypes;
import gde.device.DecimalSeparatorTypes;
import gde.histo.ui.HistoExplorer;
import gde.histo.ui.datasources.SupplementObjectFolder;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.ParameterConfigControl;
import gde.ui.SWTResourceManager;
import gde.ui.menu.LogLevelSelectionContextMenu;
import gde.utils.ObjectKeyCompliance;
import gde.utils.ObjectKeyScanner;
import gde.utils.OperatingSystemHelper;
import gde.utils.StringHelper;

/**
 * Dialog class to adjust application wide properties
 * @author Winfried Brügmann
 */
public class SettingsDialog extends Dialog {
	final static Logger									log											= Logger.getLogger(SettingsDialog.class.getName());
	static final String									DEFAULT_LOG_LEVEL				= "WARNING";																																															//$NON-NLS-1$

	public static final String					LOGGER_NAME							= "logger_name";																																													//$NON-NLS-1$
	public static final String					LOG_LEVEL								= "log_level";																																														//$NON-NLS-1$

	CCombo															configLevelCombo;
	CLabel															utilsLevelLabel;
	CCombo															utilsLevelCombo;
	CLabel															serialIOLevelLabel;
	CCombo															serialIOLevelCombo;
	CLabel															configLevelLabel;
	Button															okButton;
	Button															globalLogLevel;
	CLabel															commonLevelLabel;
	CCombo															commonLevelCombo;
	CLabel															deviceLevelLabel;
	CCombo															deviceLevelCombo;
	CCombo															uiLevelCombo;
	CLabel															uiLevelLabel;
	Composite														individualLoggingComosite;
	Composite														globalLoggingComposite;
	Shell																dialogShell;
	CLabel															defaultDataPathLabel;
	Group																defaultDataPathGroup;
	CLabel															localLabel, timeFormatLabel;
	CCombo															localCombo, timeFormatCombo;
	Group																groupLocale, groupTimeFormat;
	Button															skipBluetoothDevices, doPortAvailabilityCheck;
	Button															enableBlackListButton, enableWhiteListButton;
	Text																serialPortBlackList, serialPortWhiteList;
	Button															suggestObjectKey, writeTmpFiles;
	Composite														generalTabComposite;
	Composite														analysisComposite;
	CTabItem														generalTabItem;
	CTabItem														testTabItem;
	CTabItem														histoTabItem;
	Button															histoActive;
	Group																histoScreening;
	Group																histoDisplay;
	Button															histoDisplaySettlements;
	Button															histoDisplayScores;
	Button															histoDisplayTags;
	Group																histoBoxplot;
	CLabel															histoBoxplotScaleLabel;
	CCombo															histoBoxplotScale;
	CLabel															histoBoxplotSizeAdaptationLabel;
	CCombo															histoBoxplotSizeAdaptation;
	CLabel															histoRetrospectLabel;
	Text																histoRetrospectMonths;
	Button															histoSearchDataPathImports;
	Button															histoChannelMix;
	Group																histoDisplayOption;
	Group																histoXAxis;
	CLabel															histoSpreadLabel;
	CCombo															histoSpreadGrade;
	Button															histoReversed;
	Button															histoLogarithmicDistance;
	Group																histoFileContents;
	CLabel															histoMaxDurationLabel;
	Text																histoMaxDuration;
	CLabel															histoSamplingLabel;
	CCombo															histoSamplingTimespan_ms;
	Group																histoForceObject;
	Button															histoIgnoreLogObjectKey;
	CCombo															histoSubdirectoryLevel;
	CLabel															histoSubdirectoryLevelLabel;
	Group																timeZone;
	Button															dateTimeUtcButton;
	Group																fileOpenGroup;
	Button															firstRecordSet, histoObjectQuery;
	CTabItem														analysisTabItem;
	CTabFolder													settingsTabFolder;
	Slider															alphaSlider;
	Button															suggestDate;
	Group																fileOpenSaveDialogGroup;
	Group																objectKeyGroup;
	Button															scanObjectKeysButton;
	Button															cleanObjectReferecesButton;
	Button															removeMimeAssocButton;
	Button															createObjectsFromDirectoriesButton, importLogsButton;
	Group																histoToolsGroup;
	Button															clearHistoCacheButton, clearSupplementFoldersButton;
	Group																miscDiagGroup;
	Button															resourceConsumptionButton, cleanSettingsButton;
	Button															assocMimeTypeButton;
	Button															removeLauncherButton;
	Button															createLauncherButton;
	Button															partialDataTableButton, blankChargeDischargeButton, continiousRecordSetButton, startCommunicationAfterStartupButton;
	Button															drawScaleInRecordColorButton, drawNameInRecordColorButton, drawNumbersInRecordColorButton, draw10TicksPerRecordButton, addChannelConfigNameCurveCompareButton;
	ParameterConfigControl							fontSizeCorrectionSlider, kmzExportTimeStepSlider;
	int[]																fontCorrection					= new int[1];
	int[]																exportTimeStep_ms				= new int[1];
	Composite														osMiscComposite;
	Composite														histoComposite, miscComposite;
	Group																shellMimeType;
	Group																desktopLauncher;
	Group																fontSizeGroup, kmzExportGroup, dataTableGroup, chargerSpecials, graphicsView;
	CTabItem														osMiscTabItem;
	CTabItem														miscTabItem;
	CLabel															fileIOLevelLabel;
	CCombo															fileIOLevelCombo;
	Button															deviceDialogModalButton;
	Button															deviceDialogOnTopButton;
	Button															deviceDialogAlphaButton;
	Button															deviceImportDialogButton;
	Group																deviceDialogGroup;
	Group																serialPortGroup;
	Group																separatorGroup;
	CCombo															listSeparator;
	CLabel															listSeparatorLabel;
	CCombo															decimalSeparator;
	CLabel															decimalSeparatorLabel;
	Button															defaultDataPathAdjustButton;
	Text																defaultDataPath;
	CCombo															globalLoggingCombo;
	Group																loggingGroup;
	Group																classSelectionGroup;
	Label																classBasedLabel;
	Tree																tree;
	Group 															skinColorSchemaGroup;
	Button															osSchema, lightSchema, darkSchema;

	Thread															listPortsThread;
	Vector<String>											availablePorts					= new Vector<String>();
	final Settings											settings;
	final DataExplorer									application;
	final String[]											supportedLocals					= { "en", "de" };																																													//$NON-NLS-1$ //$NON-NLS-2$
	boolean															isLocaleLanguageChanged	= false;

	final LogLevelSelectionContextMenu	logLevelMenu						= new LogLevelSelectionContextMenu();
	Menu																popupmenu;
	
	public SettingsDialog(Shell parent, int style) {
		super(parent, style);
		this.application = DataExplorer.getInstance();
		this.settings = Settings.getInstance();
	}

	public void open() {
		int shellWidth =  500;
		int shellHeight =  GDE.IS_LINUX ? 620 : 610;
		try {
			Shell parent = getParent();
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
			SWTResourceManager.registerResourceUser(this.dialogShell);
			this.dialogShell.setLayout(new FormLayout());
			this.dialogShell.setSize(shellWidth, shellHeight);
			this.dialogShell.layout();
			this.dialogShell.setText(GDE.NAME_LONG + Messages.getString(MessageIds.GDE_MSGT0300));
			this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/DataExplorer.png")); //$NON-NLS-1$
			this.dialogShell.addListener(SWT.Traverse, new Listener() {
				@Override
				public void handleEvent(Event event) {
					switch (event.detail) {
					case SWT.TRAVERSE_ESCAPE:
						SettingsDialog.this.dialogShell.close();
						event.detail = SWT.TRAVERSE_NONE;
						event.doit = false;
						break;
					}
				}
			});
			{ // begin tab folder
				this.settingsTabFolder = new CTabFolder(this.dialogShell, SWT.FLAT | SWT.BORDER);
				this.settingsTabFolder.setSimple(false);
				FormData cTabFolder1LData = new FormData();
				cTabFolder1LData.height = GDE.IS_WINDOWS ? 480 : 515;
				cTabFolder1LData.left = new FormAttachment(0, 1000, 0);
				cTabFolder1LData.right = new FormAttachment(1000, 1000, 0);
				cTabFolder1LData.top = new FormAttachment(0, 1000, 0);
				this.settingsTabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.settingsTabFolder.setLayoutData(cTabFolder1LData);
				{ // begin general tab item
					this.generalTabItem = new CTabItem(this.settingsTabFolder, SWT.NONE);
					this.generalTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.generalTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0301));
					{
						this.generalTabComposite = new Composite(this.settingsTabFolder, SWT.NONE);
						this.generalTabItem.setControl(this.generalTabComposite);
						GridLayout generalTabCompositeLayout = new GridLayout(2, true);
						this.generalTabComposite.setLayout(generalTabCompositeLayout);
						{
							this.groupLocale = new Group(this.generalTabComposite, SWT.NONE);
							this.groupLocale.setLayout(null);
							this.groupLocale.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							GridData groupLocaleLData = new GridData(SWT.HORIZONTAL);
							groupLocaleLData.horizontalSpan = 1;
							groupLocaleLData.widthHint = shellWidth/2 - 24;
							this.groupLocale.setLayoutData(groupLocaleLData);
							this.groupLocale.setText(Messages.getString(MessageIds.GDE_MSGT0305));
							{
								this.localLabel = new CLabel(this.groupLocale, SWT.NONE);
								this.localLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.localLabel.setBounds(10, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 20, 120, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.localLabel.setText(Messages.getString(MessageIds.GDE_MSGT0307));
								this.localLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0308));
							}
							{
								this.localCombo = new CCombo(this.groupLocale, SWT.BORDER);
								this.localCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.localCombo.setItems(this.supportedLocals);
								this.localCombo.select(getLocalLanguageIndex());
								this.localCombo.setBounds(138, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 20, 65, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.localCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0306));
								this.localCombo.setEditable(false);
								this.localCombo.setBackground(SWTResourceManager.getColor(255, 255, 255));
								this.localCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "localCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										String newLanguage = SettingsDialog.this.supportedLocals[SettingsDialog.this.localCombo.getSelectionIndex()];
										SettingsDialog.this.isLocaleLanguageChanged = !SettingsDialog.this.settings.getLocale().getLanguage().equals(newLanguage);
										SettingsDialog.this.settings.setLocaleLanguage(newLanguage);
									}
								});
							}
						}
						{
							this.groupTimeFormat = new Group(this.generalTabComposite, SWT.NONE);
							this.groupTimeFormat.setLayout(null);
							this.groupTimeFormat.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							GridData groupTimeFormatLData = new GridData(SWT.HORIZONTAL);
							groupTimeFormatLData.horizontalSpan = 1;
							groupTimeFormatLData.widthHint = shellWidth/2 - 24;
							this.groupTimeFormat.setLayoutData(groupTimeFormatLData);
							this.groupTimeFormat.setText(Messages.getString(MessageIds.GDE_MSGT0682));
							{
								this.timeFormatLabel = new CLabel(this.groupTimeFormat, SWT.NONE);
								this.timeFormatLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.timeFormatLabel.setBounds(10, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 20, 80, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.timeFormatLabel.setText(Messages.getString(MessageIds.GDE_MSGT0682));
								this.timeFormatLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0683));
							}
							{
								this.timeFormatCombo = new CCombo(this.groupTimeFormat, SWT.BORDER);
								this.timeFormatCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.timeFormatCombo.setItems(new String[] { Messages.getString(MessageIds.GDE_MSGT0684), Messages.getString(MessageIds.GDE_MSGT0359) });
								this.timeFormatCombo.select(getTimeFormatIndex());
								this.timeFormatCombo.setBounds(90, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 20, 140, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.timeFormatCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0683));
								this.timeFormatCombo.setEditable(false);
								this.timeFormatCombo.setBackground(SWTResourceManager.getColor(255, 255, 255));
								this.timeFormatCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "timeFormatCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setTimeFormat(SettingsDialog.this.timeFormatCombo.getItems()[SettingsDialog.this.timeFormatCombo.getSelectionIndex()]);
									}
								});
							}
						}
						{ // begin default data path group
							this.defaultDataPathGroup = new Group(this.generalTabComposite, SWT.NONE);
							GridData defaultDataPathGroupLData = new GridData(SWT.HORIZONTAL);
							defaultDataPathGroupLData.horizontalSpan = 2;
							defaultDataPathGroupLData.widthHint = shellWidth - 25;
							defaultDataPathGroupLData.grabExcessHorizontalSpace = true; 
							this.defaultDataPathGroup.setLayoutData(defaultDataPathGroupLData);
							this.defaultDataPathGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.defaultDataPathGroup.setText(Messages.getString(MessageIds.GDE_MSGT0310));
							{
								this.defaultDataPathLabel = new CLabel(this.defaultDataPathGroup, SWT.NONE);
								this.defaultDataPathLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.defaultDataPathLabel.setText(Messages.getString(MessageIds.GDE_MSGT0311));
								this.defaultDataPathLabel.setBounds(15, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 20, 90, 18);
							}
							{
								this.defaultDataPath = new Text(this.defaultDataPathGroup, SWT.BORDER);
								this.defaultDataPath.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.defaultDataPath.setBounds(107, GDE.IS_MAC ? 6 : GDE.IS_LINUX ? 8 : 18, 295, GDE.IS_MAC ? 20 : GDE.IS_LINUX ? 22 : 18);
							}
							{
								this.defaultDataPathAdjustButton = new Button(this.defaultDataPathGroup, SWT.PUSH | SWT.CENTER);
								this.defaultDataPathAdjustButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.defaultDataPathAdjustButton.setText(". . . "); //$NON-NLS-1$
								this.defaultDataPathAdjustButton.setBounds(405, GDE.IS_WINDOWS ? 18 : 8, GDE.IS_WINDOWS ? 30 : 50, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.defaultDataPathAdjustButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "defaultDataPathAdjustButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										String defaultDataDirectory = SettingsDialog.this.application.openDirFileDialog(Messages.getString(MessageIds.GDE_MSGT0312), SettingsDialog.this.settings.getDataFilePath());
										if (defaultDataDirectory != null && defaultDataDirectory.length() > 5) {
											SettingsDialog.log.log(Level.FINE, "default directory from directoy dialog = " + defaultDataDirectory); //$NON-NLS-1$
											SettingsDialog.this.settings.setDataFilePath(defaultDataDirectory);
											SettingsDialog.this.defaultDataPath.setText(defaultDataDirectory);
											SettingsDialog.this.application.getHistoExplorer().ifPresent(HistoExplorer::resetHisto);
										}
									}
								});
							}
						} // end default data path group
						{ // begin file save dialog filename leader
							this.fileOpenSaveDialogGroup = new Group(this.generalTabComposite, SWT.NONE);
							this.fileOpenSaveDialogGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.fileOpenSaveDialogGroup.setLayout(null);
							GridData fileOpenSaveDialogGroupLData = new GridData(SWT.HORIZONTAL);
							fileOpenSaveDialogGroupLData.horizontalSpan = 2;
							fileOpenSaveDialogGroupLData.widthHint = shellWidth - 25;
							fileOpenSaveDialogGroupLData.grabExcessHorizontalSpace = true; 
							this.fileOpenSaveDialogGroup.setLayoutData(fileOpenSaveDialogGroupLData);
							this.fileOpenSaveDialogGroup.setText(Messages.getString(MessageIds.GDE_MSGT0315));
							{
								this.suggestDate = new Button(this.fileOpenSaveDialogGroup, SWT.CHECK | SWT.LEFT);
								this.suggestDate.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.suggestDate.setText(Messages.getString(MessageIds.GDE_MSGT0316));
								this.suggestDate.setBounds(10, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 20, 170, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.suggestDate.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "suggestDate.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setUsageDateAsFileNameLeader(SettingsDialog.this.suggestDate.getSelection());
									}
								});
							}
							{
								this.suggestObjectKey = new Button(this.fileOpenSaveDialogGroup, SWT.CHECK | SWT.LEFT);
								this.suggestObjectKey.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.suggestObjectKey.setText(Messages.getString(MessageIds.GDE_MSGT0317));
								this.suggestObjectKey.setBounds(180, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 20, 190, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.suggestObjectKey.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "suggestObjectKey.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setUsageObjectKeyInFileName(SettingsDialog.this.suggestObjectKey.getSelection());
									}
								});
							}
							{
								this.writeTmpFiles = new Button(this.fileOpenSaveDialogGroup, SWT.CHECK | SWT.LEFT);
								this.writeTmpFiles.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.writeTmpFiles.setText(Messages.getString(MessageIds.GDE_MSGT0674));
								this.writeTmpFiles.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0675, new String[] { Settings.getApplHomePath() }));
								this.writeTmpFiles.setBounds(370, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 20, 95, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.writeTmpFiles.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "writeTmpFiles.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setUsageWritingTmpFiles(SettingsDialog.this.writeTmpFiles.getSelection());
										SettingsDialog.this.application.enableWritingTmpFiles(SettingsDialog.this.writeTmpFiles.getSelection());
									}
								});
							}
						} // end file save dialog filename leader
						{ // begin skin color
							this.skinColorSchemaGroup = new Group(this.generalTabComposite, SWT.NONE);
							this.skinColorSchemaGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.skinColorSchemaGroup.setLayout(null);
							GridData skinColorSchemaGroupLData = new GridData(SWT.HORIZONTAL);
							skinColorSchemaGroupLData.horizontalSpan = 2;
							skinColorSchemaGroupLData.widthHint = shellWidth - 25;
							skinColorSchemaGroupLData.grabExcessHorizontalSpace = true; 
							this.skinColorSchemaGroup.setLayoutData(skinColorSchemaGroupLData);
							this.skinColorSchemaGroup.setText(Messages.getString(MessageIds.GDE_MSGT0954));
							{
								this.osSchema = new Button(this.skinColorSchemaGroup, SWT.CHECK | SWT.LEFT);
								this.osSchema.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.osSchema.setText("Standard");
								this.osSchema.setBounds(10, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 20, 170, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.osSchema.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "osShema.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (SettingsDialog.this.osSchema.getSelection()) {
											SettingsDialog.this.settings.setSkinColorSchema(Settings.COLOR_SCHEMA_SYSTEM);
											SettingsDialog.this.lightSchema.setSelection(false);
											SettingsDialog.this.darkSchema.setSelection(false);
										}
									}
								});
							}
							{
								this.lightSchema = new Button(this.skinColorSchemaGroup, SWT.CHECK | SWT.LEFT);
								this.lightSchema.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.lightSchema.setText(Messages.getString(MessageIds.GDE_MSGT0333));
								this.lightSchema.setBounds(180, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 20, 190, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.lightSchema.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "lightSchema.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (SettingsDialog.this.lightSchema.getSelection()) {
											SettingsDialog.this.settings.setSkinColorSchema(Settings.COLOR_SCHEMA_LIGHT);
											SettingsDialog.this.osSchema.setSelection(false);
											SettingsDialog.this.darkSchema.setSelection(false);
										}
									}
								});
							}
							{
								this.darkSchema = new Button(this.skinColorSchemaGroup, SWT.CHECK | SWT.LEFT);
								this.darkSchema.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.darkSchema.setText(Messages.getString(MessageIds.GDE_MSGT0334));
								this.darkSchema.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0675, new String[] { Settings.getApplHomePath() }));
								this.darkSchema.setBounds(370, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 20, 95, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.darkSchema.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "darkSchema.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (SettingsDialog.this.darkSchema.getSelection()) {
											SettingsDialog.this.settings.setSkinColorSchema(Settings.COLOR_SCHEMA_DARK);
											SettingsDialog.this.lightSchema.setSelection(false);
											SettingsDialog.this.osSchema.setSelection(false);
										}
								}
								});
							}
						} // end skin color
						{ // begin device dialog settings
							this.deviceDialogGroup = new Group(this.generalTabComposite, SWT.NONE);
							this.deviceDialogGroup.setLayout(null);
							GridData deviceDialogLData = new GridData(SWT.HORIZONTAL);
							deviceDialogLData.horizontalSpan = 2;
							deviceDialogLData.widthHint = shellWidth - 25;
							deviceDialogLData.grabExcessHorizontalSpace = true; 
							this.deviceDialogGroup.setLayoutData(deviceDialogLData);
							this.deviceDialogGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.deviceDialogGroup.setText(Messages.getString(MessageIds.GDE_MSGT0318));
							{
								this.deviceDialogModalButton = new Button(this.deviceDialogGroup, SWT.CHECK | SWT.LEFT);
								this.deviceDialogModalButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.deviceDialogModalButton.setText(Messages.getString(MessageIds.GDE_MSGT0319));
								this.deviceDialogModalButton.setBounds(16, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 20, 254, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.deviceDialogModalButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0320));
								this.deviceDialogModalButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "deviceDialogModalButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.enabelModalDeviceDialogs(SettingsDialog.this.deviceDialogModalButton.getSelection());
										SettingsDialog.this.deviceDialogOnTopButton.setEnabled(!SettingsDialog.this.deviceDialogModalButton.getSelection());
									}
								});
							}
							{
								this.deviceDialogOnTopButton = new Button(this.deviceDialogGroup, SWT.CHECK | SWT.LEFT);
								this.deviceDialogOnTopButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.deviceDialogOnTopButton.setText(Messages.getString(MessageIds.GDE_MSGT0309));
								this.deviceDialogOnTopButton.setBounds(282, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 20, 165, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.deviceDialogOnTopButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0320));
								this.deviceDialogOnTopButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "deviceDialogOnTopButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.enabelDeviceDialogsOnTop(SettingsDialog.this.deviceDialogOnTopButton.getSelection());
									}
								});
							}
							{
								this.deviceDialogAlphaButton = new Button(this.deviceDialogGroup, SWT.CHECK | SWT.LEFT);
								this.deviceDialogAlphaButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.deviceDialogAlphaButton.setText(Messages.getString(MessageIds.GDE_MSGT0321));
								this.deviceDialogAlphaButton.setBounds(16, GDE.IS_MAC ? 30 : GDE.IS_LINUX ? 36 : 45, 254, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.deviceDialogAlphaButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0322));
								this.deviceDialogAlphaButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "deviceDialogButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setDeviceDialogAlphaEnabled(SettingsDialog.this.deviceDialogAlphaButton.getSelection());
										SettingsDialog.this.alphaSlider.setEnabled(SettingsDialog.this.deviceDialogAlphaButton.getSelection());
									}
								});
							}
							{
								this.alphaSlider = new Slider(this.deviceDialogGroup, SWT.NONE);
								this.alphaSlider.setBounds(282, GDE.IS_MAC ? 25 : GDE.IS_LINUX ? 32 : 45, 163, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.alphaSlider.setIncrement(5);
								this.alphaSlider.setMinimum(10);
								this.alphaSlider.setMaximum(180);
								this.alphaSlider.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINER, "alphaSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
										switch (evt.detail) {
										case SWT.DRAG:
											SettingsDialog.this.dialogShell.setAlpha(SettingsDialog.this.alphaSlider.getSelection());
											break;
										default:
										case SWT.NONE:
											SettingsDialog.this.settings.setDialogAlphaValue(SettingsDialog.this.alphaSlider.getSelection());
											SettingsDialog.this.dialogShell.setAlpha(254);
											break;
										}
										SettingsDialog.this.settings.setDialogAlphaValue(SettingsDialog.this.alphaSlider.getSelection());
									}
								});
							}
							{
								this.deviceImportDialogButton = new Button(this.deviceDialogGroup, SWT.CHECK | SWT.LEFT);
								this.deviceImportDialogButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.deviceImportDialogButton.setText(Messages.getString(MessageIds.GDE_MSGT0409));
								this.deviceImportDialogButton.setBounds(16, GDE.IS_MAC ? 52 : GDE.IS_LINUX ? 62 : 72, 435, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.deviceImportDialogButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0437));
								this.deviceImportDialogButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "deviceDialogButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setDeviceImportDirectoryObjectRelated(SettingsDialog.this.deviceImportDialogButton.getSelection());
									}
								});
							}
						} // end device dialog settings
						{ // begin CSV separator group
							this.separatorGroup = new Group(this.generalTabComposite, SWT.NONE);
							this.separatorGroup.setLayout(null);
							this.separatorGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							GridData separatorGroupLData = new GridData(SWT.HORIZONTAL);
							separatorGroupLData.horizontalSpan = 2;
							separatorGroupLData.widthHint = shellWidth - 25;
							separatorGroupLData.grabExcessHorizontalSpace = true; 
							this.separatorGroup.setLayoutData(separatorGroupLData);
							this.separatorGroup.setText(Messages.getString(MessageIds.GDE_MSGT0325));
							{
								this.decimalSeparatorLabel = new CLabel(this.separatorGroup, SWT.RIGHT);
								this.decimalSeparatorLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.decimalSeparatorLabel.setText(Messages.getString(MessageIds.GDE_MSGT0326));
								this.decimalSeparatorLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0327));
								this.decimalSeparatorLabel.setBounds(10, GDE.IS_MAC || GDE.IS_LINUX ? 12 : 24, 140, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
							}
							{
								this.decimalSeparator = new CCombo(this.separatorGroup, SWT.BORDER);
								this.decimalSeparator.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
								this.decimalSeparator.setItems(DecimalSeparatorTypes.valuesAsStingArray());
								this.decimalSeparator.setBounds(153, GDE.IS_MAC || GDE.IS_LINUX ? 12 : 24, 65, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.decimalSeparator.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "decimalSeparator.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setDecimalSeparator(SettingsDialog.this.decimalSeparator.getText().trim());
										SettingsDialog.this.decimalSeparator.setText(GDE.STRING_BLANK + SettingsDialog.this.decimalSeparator.getText().trim());
									}
								});
							}
							{
								this.listSeparatorLabel = new CLabel(this.separatorGroup, SWT.RIGHT);
								this.listSeparatorLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.listSeparatorLabel.setText(Messages.getString(MessageIds.GDE_MSGT0328));
								this.listSeparatorLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0329));
								this.listSeparatorLabel.setBounds(228, GDE.IS_MAC || GDE.IS_LINUX ? 12 : 24, 140, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
							}
							{
								this.listSeparator = new CCombo(this.separatorGroup, SWT.BORDER);
								this.listSeparator.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
								this.listSeparator.setItems(CommaSeparatorTypes.valuesAsStingArray());
								this.listSeparator.setBounds(370, GDE.IS_MAC || GDE.IS_LINUX ? 12 : 24, 65, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.listSeparator.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "listSeparator.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setListSeparator(SettingsDialog.this.listSeparator.getText().trim());
										SettingsDialog.this.listSeparator.setText(GDE.STRING_BLANK + SettingsDialog.this.listSeparator.getText().trim());
									}
								});
							}
						} // end CSV separator group
						{ // begin serial port group
							this.serialPortGroup = new Group(this.generalTabComposite, SWT.NONE);
							this.serialPortGroup.setLayout(null);
							GridData serialPortGroupLData = new GridData(SWT.HORIZONTAL);
							serialPortGroupLData.horizontalSpan = 2;
							serialPortGroupLData.widthHint = shellWidth - 25;
							serialPortGroupLData.grabExcessHorizontalSpace = true; 
							this.serialPortGroup.setLayoutData(serialPortGroupLData);
							this.serialPortGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.serialPortGroup.setText(Messages.getString(MessageIds.GDE_MSGT0330));
							{
								this.enableBlackListButton = new Button(this.serialPortGroup, SWT.CHECK | SWT.LEFT);
								this.enableBlackListButton.setText(Messages.getString(MessageIds.GDE_MSGT0336));
								this.enableBlackListButton.setBounds(15, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 18, 243, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.enableBlackListButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.enableBlackListButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0337));
								this.enableBlackListButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "enableBlackListButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										System.clearProperty("gnu.io.rxtx.SerialPorts"); //$NON-NLS-1$
										if (SettingsDialog.this.enableBlackListButton.getSelection()) {
											SettingsDialog.this.settings.setSerialPortBlackListEnabled(true);
											SettingsDialog.this.serialPortBlackList.setEditable(true);
											SettingsDialog.this.serialPortBlackList.setEnabled(true);
											SettingsDialog.this.settings.setSerialPortWhiteListEnabled(false);
											SettingsDialog.this.enableWhiteListButton.setSelection(false);
											SettingsDialog.this.serialPortWhiteList.setEditable(false);
											SettingsDialog.this.serialPortWhiteList.setEnabled(false);
										}
										else {
											SettingsDialog.this.settings.setSerialPortBlackListEnabled(false);
											SettingsDialog.this.serialPortBlackList.setEditable(false);
											SettingsDialog.this.serialPortBlackList.setEnabled(false);
										}
									}
								});
							}
							{
								this.serialPortBlackList = new Text(this.serialPortGroup, SWT.BORDER);
								this.serialPortBlackList.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.serialPortBlackList.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0337));
								this.serialPortBlackList.setBounds(260, GDE.IS_MAC ? 8 : GDE.IS_LINUX ? 12 : 18, 181, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.serialPortBlackList.addVerifyListener(new VerifyListener() {
									@Override
									public void verifyText(VerifyEvent e) {
										log.log(Level.FINEST, GDE.STRING_EMPTY + StringHelper.verifyPortInput(e.text.trim()));
										e.doit = StringHelper.verifyPortInput(e.text.trim());
									}
								});
								this.serialPortBlackList.addKeyListener(new KeyAdapter() {
									@Override
									public void keyReleased(KeyEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "serialPortBlackList.keyReleased, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setSerialPortBlackList(SettingsDialog.this.serialPortBlackList.getText());
									}
								});
							}
							{
								this.enableWhiteListButton = new Button(this.serialPortGroup, SWT.CHECK | SWT.LEFT);
								this.enableWhiteListButton.setText(Messages.getString(MessageIds.GDE_MSGT0338));
								this.enableWhiteListButton.setBounds(15, GDE.IS_MAC ? 30 : GDE.IS_LINUX ? 36 : 45, 243, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.enableWhiteListButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.enableWhiteListButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0339));
								this.enableWhiteListButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "enableWhiteListButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (SettingsDialog.this.enableWhiteListButton.getSelection()) {
											SettingsDialog.this.settings.setSerialPortWhiteListEnabled(true);
											SettingsDialog.this.serialPortWhiteList.setEditable(true);
											SettingsDialog.this.serialPortWhiteList.setEnabled(true);
											SettingsDialog.this.settings.setSerialPortBlackListEnabled(false);
											SettingsDialog.this.enableBlackListButton.setSelection(false);
											SettingsDialog.this.serialPortBlackList.setEditable(false);
											SettingsDialog.this.serialPortBlackList.setEnabled(false);
											SettingsDialog.this.settings.setSerialPortWhiteList(SettingsDialog.this.serialPortWhiteList.getText());
										}
										else {
											SettingsDialog.this.settings.setSerialPortWhiteListEnabled(false);
											SettingsDialog.this.serialPortWhiteList.setEditable(false);
											SettingsDialog.this.serialPortWhiteList.setEnabled(false);
											System.clearProperty("gnu.io.rxtx.SerialPorts"); //$NON-NLS-1$
										}
									}
								});
							}
							{
								this.serialPortWhiteList = new Text(this.serialPortGroup, SWT.BORDER);
								this.serialPortWhiteList.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.serialPortWhiteList.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0339));
								this.serialPortWhiteList.setBounds(260, GDE.IS_MAC ? 30 : GDE.IS_LINUX ? 32 : 45, 181, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.serialPortWhiteList.addVerifyListener(new VerifyListener() {
									@Override
									public void verifyText(VerifyEvent e) {
										//log.log(Level.FINEST, GDE.STRING_EMPTY+StringHelper.verifyPortInput(e.text));
										//e.doit = StringHelper.verifyPortInput(e.text);
									}
								});
								this.serialPortWhiteList.addKeyListener(new KeyAdapter() {
									@Override
									public void keyReleased(KeyEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "serialPortWhiteList.keyReleased, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setSerialPortWhiteList(SettingsDialog.this.serialPortWhiteList.getText());
									}
								});
							}
							{
								this.skipBluetoothDevices = new Button(this.serialPortGroup, SWT.CHECK | SWT.LEFT);
								this.skipBluetoothDevices.setBounds(260, GDE.IS_MAC ? 52 : GDE.IS_LINUX ? 62 : 72, 243, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.skipBluetoothDevices.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.skipBluetoothDevices.setText(Messages.getString(MessageIds.GDE_MSGT0434));
								this.skipBluetoothDevices.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0435));
								this.skipBluetoothDevices.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINE, "skipBluetoothDevices.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setSkipBluetoothDevices(SettingsDialog.this.skipBluetoothDevices.getSelection());
										if (!SettingsDialog.this.skipBluetoothDevices.getSelection()) {
											SettingsDialog.this.application.openMessageDialog(SettingsDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGI0049));
										}
									}
								});
							}
							{
								this.doPortAvailabilityCheck = new Button(this.serialPortGroup, SWT.CHECK | SWT.LEFT);
								this.doPortAvailabilityCheck.setBounds(15, GDE.IS_MAC ? 52 : GDE.IS_LINUX ? 62 : 72, 243, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.doPortAvailabilityCheck.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.doPortAvailabilityCheck.setText(Messages.getString(MessageIds.GDE_MSGT0331));
								this.doPortAvailabilityCheck.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0332));
								this.doPortAvailabilityCheck.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINE, "doPortAvailabilityCheck.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setPortAvailabilityCheck(SettingsDialog.this.doPortAvailabilityCheck.getSelection());
										if (SettingsDialog.this.doPortAvailabilityCheck.getSelection()) {
											SettingsDialog.this.application.openMessageDialog(SettingsDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGI0036));
										}
									}
								});
							}
						} // end serial port group
					} // end generalTabComposite
					this.generalTabComposite.layout();
				} // end general tab item
				{ // begin histo tab item
					this.histoTabItem = new CTabItem(this.settingsTabFolder, SWT.NONE);
					this.histoTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.histoTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0738));
					{
						this.histoComposite = new Composite(this.settingsTabFolder, SWT.NONE);
						this.histoTabItem.setControl(this.histoComposite);
						RowLayout compositeHistoLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						compositeHistoLayout.marginLeft = GDE.IS_MAC ? 2 : 5;
						compositeHistoLayout.spacing = GDE.IS_MAC ? 2 : 5;
						compositeHistoLayout.marginTop = GDE.IS_MAC ? 3 : 11;
						this.histoComposite.setLayout(compositeHistoLayout);
						{
							this.histoActive = new Button(this.histoComposite, SWT.CHECK);
							this.histoActive.setLayoutData(new RowData(333, 15));
							this.histoActive.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.histoActive.setText(Messages.getString(MessageIds.GDE_MSGT0739));
							this.histoActive.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0740));
							this.histoActive.setSelection(this.settings.isHistoActive());
							this.histoActive.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									SettingsDialog.log.log(Level.FINEST, "histoActive.widgetSelected, event=" + evt); //$NON-NLS-1$
									SettingsDialog.this.settings.setHistoActive(SettingsDialog.this.histoActive.getSelection());
									SettingsDialog.this.application.setHisto(SettingsDialog.this.histoActive.getSelection());
								}
							});
						}
						{
							this.histoDisplayOption = new Group(this.histoComposite, SWT.NONE);
							// kommt von histoXAxisGroup
							RowData histoDisplayOptionRData = new RowData();
							histoDisplayOptionRData.width = GDE.IS_MAC ? 485 : 477;
							this.histoDisplayOption.setLayoutData(histoDisplayOptionRData);

							// kommt von histoForceObjectGroup
							RowLayout histoDisplayOptionRLayout = new RowLayout(SWT.HORIZONTAL);
							histoDisplayOptionRLayout.marginTop = histoDisplayOptionRLayout.marginLeft = histoDisplayOptionRLayout.marginRight = histoDisplayOptionRLayout.marginHeight = 2;
							this.histoDisplayOption.setLayout(histoDisplayOptionRLayout);
							this.histoDisplayOption.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.histoDisplayOption.setText(Messages.getString(MessageIds.GDE_MSGT0806));
							{
								this.histoXAxis = new Group(this.histoDisplayOption, SWT.NONE);
								RowData histoXAxisGroupLData = new RowData();
								histoXAxisGroupLData.width = 223;
								this.histoXAxis.setLayoutData(histoXAxisGroupLData);
								FormLayout formLayout = new FormLayout();
								formLayout.marginTop = 7;
								formLayout.marginLeft = formLayout.marginRight = formLayout.marginHeight = 0;
								this.histoXAxis.setLayout(formLayout);
								this.histoXAxis.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.histoXAxis.setText(Messages.getString(MessageIds.GDE_MSGT0816));
								{
									this.histoReversed = new Button(this.histoXAxis, SWT.CHECK);
									FormData formData = new FormData();
									formData.left = new FormAttachment(0, 5);
									this.histoReversed.setLayoutData(formData);
									this.histoReversed.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.histoReversed.setText(Messages.getString(MessageIds.GDE_MSGT0821));
									this.histoReversed.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0822));
									this.histoReversed.setSelection(this.settings.isXAxisReversed());
									this.histoReversed.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.log(Level.FINEST, "histoReversed.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setXAxisReversed(SettingsDialog.this.histoReversed.getSelection());
											SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
										}
									});
								}
								{
									this.histoSpreadLabel = new CLabel(this.histoXAxis, SWT.NONE);
									FormData formData = new FormData();
									formData.top = new FormAttachment(this.histoReversed, 5);
									this.histoSpreadLabel.setLayoutData(formData);
									this.histoSpreadLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.histoSpreadLabel.setBounds(10, GDE.IS_MAC ? 8 : 20, 120, 20);
									this.histoSpreadLabel.setText(Messages.getString(MessageIds.GDE_MSGT0817));
									this.histoSpreadLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0818));
								}
								{
									this.histoSpreadGrade = new CCombo(this.histoXAxis, SWT.BORDER);
									FormData formData = new FormData();
									formData.top = new FormAttachment(this.histoReversed, 5);
									formData.right = new FormAttachment(100, -5);
									formData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
									formData.width = GDE.IS_LINUX ? 80 : 70;
									this.histoSpreadGrade.setLayoutData(formData);
									this.histoSpreadGrade.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									//this.histoSpreadGrade.setBounds(370, GDE.IS_MAC ? 14 : 24, 47, GDE.IS_LINUX ? 22 : 20);
									this.histoSpreadGrade.setItems(Settings.getXAxisSpreadGradeNomenclatures());
									this.histoSpreadGrade.setText(GDE.STRING_BLANK + SettingsDialog.this.settings.getXAxisSpreadGrade());
									this.histoSpreadGrade.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0818));
									this.histoSpreadGrade.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.log(Level.FINEST, "histoSpreadGrade.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setXAxisSpreadGrade(SettingsDialog.this.histoSpreadGrade.getText().trim());
											SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
										}
									});
								}
								{
									this.histoLogarithmicDistance = new Button(this.histoXAxis, SWT.CHECK);
									FormData formData = new FormData();
									formData.left = new FormAttachment(0, 5);
									formData.top = new FormAttachment(this.histoSpreadLabel, 10);
									this.histoLogarithmicDistance.setLayoutData(formData);
									this.histoLogarithmicDistance.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.histoLogarithmicDistance.setText(Messages.getString(MessageIds.GDE_MSGT0819));
									this.histoLogarithmicDistance.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0820));
									this.histoLogarithmicDistance.setSelection(this.settings.isXAxisLogarithmicDistance());
									this.histoLogarithmicDistance.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.log(Level.FINEST, "histoLogarithmicDistance.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setXAxisLogarithmicDistance(SettingsDialog.this.histoLogarithmicDistance.getSelection());
											SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
										}
									});
								}
							} // end histoXAxis group
							{
								this.histoBoxplot = new Group(this.histoDisplayOption, SWT.NONE);
								RowData histoXAxisGroupRData = new RowData();
								histoXAxisGroupRData.width = 223;
								this.histoBoxplot.setLayoutData(histoXAxisGroupRData);
								FormLayout formLayout = new FormLayout();
								formLayout.marginTop = formLayout.marginLeft = formLayout.marginRight = formLayout.marginHeight = 2;
								this.histoBoxplot.setLayout(formLayout);
								this.histoBoxplot.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.histoBoxplot.setText(Messages.getString(MessageIds.GDE_MSGT0795));
								{
									this.histoBoxplotScaleLabel = new CLabel(this.histoBoxplot, SWT.NONE);
									FormData formData = new FormData();
									this.histoBoxplotScaleLabel.setLayoutData(formData);
									this.histoBoxplotScaleLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.histoBoxplotScaleLabel.setBounds(10, GDE.IS_MAC ? 8 : 20, 120, 20);
									this.histoBoxplotScaleLabel.setText(Messages.getString(MessageIds.GDE_MSGT0812));
									this.histoBoxplotScaleLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0813));
								}
								{
									this.histoBoxplotScale = new CCombo(this.histoBoxplot, SWT.BORDER);
									FormData formData = new FormData();
									formData.right = new FormAttachment(100, -5);
									formData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
									formData.width = GDE.IS_LINUX ? 80 : 70;
									this.histoBoxplotScale.setLayoutData(formData);
									this.histoBoxplotScale.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									//this.histoBoxplotScale.setBounds(370, GDE.IS_MAC ? 14 : 24, 47, GDE.IS_LINUX ? 22 : 20);
									this.histoBoxplotScale.setItems(Settings.getBoxplotScaleNomenclatures());
									this.histoBoxplotScale.setText(GDE.STRING_BLANK + SettingsDialog.this.settings.getBoxplotScale());
									this.histoBoxplotScale.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0813));
									this.histoBoxplotScale.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.log(Level.FINEST, "histoBoxplotScale.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setBoxplotScale(SettingsDialog.this.histoBoxplotScale.getText().trim());
											SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
										}
									});
								}
								{
									this.histoBoxplotSizeAdaptationLabel = new CLabel(this.histoBoxplot, SWT.NONE);
									FormData formData = new FormData();
									formData.top = new FormAttachment(this.histoBoxplotScaleLabel, 5);
									this.histoBoxplotSizeAdaptationLabel.setLayoutData(formData);
									this.histoBoxplotSizeAdaptationLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.histoBoxplotSizeAdaptationLabel.setBounds(10, GDE.IS_MAC ? 8 : 20, 120, 20);
									this.histoBoxplotSizeAdaptationLabel.setText(Messages.getString(MessageIds.GDE_MSGT0814));
									this.histoBoxplotSizeAdaptationLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0815));
								}
								{
									this.histoBoxplotSizeAdaptation = new CCombo(this.histoBoxplot, SWT.BORDER);
									FormData formData = new FormData();
									formData.top = new FormAttachment(this.histoBoxplotScaleLabel, 5);
									formData.right = new FormAttachment(100, -5);
									formData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
									formData.width = GDE.IS_LINUX ? 80 : 70;
									this.histoBoxplotSizeAdaptation.setLayoutData(formData);
									this.histoBoxplotSizeAdaptation.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									//this.histoBoxplotSizeAdaptation.setBounds(370, GDE.IS_MAC ? 20 : 24, 47, GDE.IS_LINUX ? 22 : 20);
									this.histoBoxplotSizeAdaptation.setItems(Settings.getBoxplotSizeAdaptationNomenclatures());
									this.histoBoxplotSizeAdaptation.setText(GDE.STRING_BLANK + SettingsDialog.this.settings.getBoxplotSizeAdaptation());
									this.histoBoxplotSizeAdaptation.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0815));
									this.histoBoxplotSizeAdaptation.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.log(Level.FINEST, "histoBoxplotSizeAdaptation.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setBoxplotSizeAdaptation(SettingsDialog.this.histoBoxplotSizeAdaptation.getText().trim());
											SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
										}
									});
								}
							} // end histoBoxplot group
							{
								this.fileOpenGroup = new Group(this.histoDisplayOption, SWT.NONE);
								RowData groupLData = new RowData();
								groupLData.width = 223;
								this.fileOpenGroup.setLayoutData(groupLData);
								FormLayout formLayout = new FormLayout();
								formLayout.marginTop = 2;
								formLayout.marginBottom = 2;
								formLayout.marginLeft = formLayout.marginRight = formLayout.marginHeight = 2;
								this.fileOpenGroup.setLayout(formLayout);
								this.fileOpenGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.fileOpenGroup.setText(Messages.getString(MessageIds.GDE_MSGT0880));
								{
									this.firstRecordSet = new Button(this.fileOpenGroup, SWT.CHECK);
									FormData formData = new FormData();
									formData.left = new FormAttachment(0, 5);
									this.firstRecordSet.setLayoutData(formData);
									this.firstRecordSet.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.firstRecordSet.setText(Messages.getString(MessageIds.GDE_MSGT0881));
									this.firstRecordSet.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0882));
									this.firstRecordSet.setSelection(this.settings.isFirstRecordSetChoice());
									this.firstRecordSet.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.log(Level.FINEST, "firstRecordSetButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setFirstRecordSetChoice(SettingsDialog.this.firstRecordSet.getSelection());
										}
									});
								}
								{
									this.histoObjectQuery = new Button(this.fileOpenGroup, SWT.CHECK);
									FormData formData = new FormData();
									formData.left = new FormAttachment(0, 5);
									formData.top = new FormAttachment(this.firstRecordSet, 5);
									this.histoObjectQuery.setLayoutData(formData);
									this.histoObjectQuery.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.histoObjectQuery.setText(Messages.getString(MessageIds.GDE_MSGT0927));
									this.histoObjectQuery.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0928));
									this.histoObjectQuery.setSelection(this.settings.isObjectQueryActive());
									this.histoObjectQuery.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.log(Level.FINEST, "histoObjectQueryButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setObjectQueryActive(SettingsDialog.this.histoObjectQuery.getSelection());
										}
									});
								}
							} // end fileOpenGroup group
							{
								this.timeZone= new Group(this.histoDisplayOption, SWT.NONE);
								RowData groupLData = new RowData();
								groupLData.width = 223;
								this.timeZone.setLayoutData(groupLData);
								FormLayout formLayout = new FormLayout();
								formLayout.marginTop = 2;
								formLayout.marginBottom = 2;
								formLayout.marginLeft = formLayout.marginRight = formLayout.marginHeight = 2;
								this.timeZone.setLayout(formLayout);
								this.timeZone.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.timeZone.setText(Messages.getString(MessageIds.GDE_MSGT0805));
								{
									this.dateTimeUtcButton = new Button(this.timeZone, SWT.CHECK);
									FormData formData = new FormData();
									formData.left = new FormAttachment(0, 5);
									this.dateTimeUtcButton.setLayoutData(formData);
									this.dateTimeUtcButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.dateTimeUtcButton.setText(Messages.getString(MessageIds.GDE_MSGT0810));
									this.dateTimeUtcButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0811));
									this.dateTimeUtcButton.setSelection(this.settings.isDateTimeUtc());
									this.dateTimeUtcButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.log(Level.FINEST, "dateTimeUtcButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setDateTimeUtc(SettingsDialog.this.dateTimeUtcButton.getSelection());
											SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
										}
									});
								}
							} // end timeZoneGroup group
							{
								this.histoDisplay = new Group(this.histoDisplayOption, SWT.NONE);
								RowData groupRData = new RowData();
								groupRData.width = 464;
								this.histoDisplay.setLayoutData(groupRData);
								FormLayout formLayout = new FormLayout();
								formLayout.marginTop = 2;
								formLayout.marginBottom = 2;
								formLayout.marginLeft = formLayout.marginRight = formLayout.marginHeight = 2;
								this.histoDisplay.setLayout(formLayout);
								this.histoDisplay.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.histoDisplay.setText(Messages.getString(MessageIds.GDE_MSGT0847));
								{
									this.histoDisplaySettlements = new Button(this.histoDisplay, SWT.CHECK);
									FormData formData = new FormData();
									formData.left = new FormAttachment(0, 7);
									this.histoDisplaySettlements.setLayoutData(formData);
									this.histoDisplaySettlements.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.histoDisplaySettlements.setText(Messages.getString(MessageIds.GDE_MSGT0788));
									this.histoDisplaySettlements.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0789));
									this.histoDisplaySettlements.setSelection(this.settings.isDisplaySettlements());
									this.histoDisplaySettlements.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.log(Level.FINEST, "histoDisplaySettlements.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setDisplaySettlements(SettingsDialog.this.histoDisplaySettlements.getSelection());
											SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
										}
									});
								}
								{
									this.histoDisplayScores = new Button(this.histoDisplay, SWT.CHECK);
									FormData formData = new FormData();
									formData.left = new FormAttachment(this.histoDisplaySettlements, 55);
									this.histoDisplayScores.setLayoutData(formData);
									this.histoDisplayScores.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.histoDisplayScores.setText(Messages.getString(MessageIds.GDE_MSGT0790));
									this.histoDisplayScores.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0791));
									this.histoDisplayScores.setSelection(this.settings.isDisplayScores());
									this.histoDisplayScores.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.log(Level.FINEST, "histoDisplayScores.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setDisplayScores(SettingsDialog.this.histoDisplayScores.getSelection());
											SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
										}
									});
								}
								{
									this.histoDisplayTags = new Button(this.histoDisplay, SWT.CHECK);
									FormData formData = new FormData();
									formData.left = new FormAttachment(this.histoDisplayScores, 55);
									this.histoDisplayTags.setLayoutData(formData);
									this.histoDisplayTags.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.histoDisplayTags.setText(Messages.getString(MessageIds.GDE_MSGT0746));
									this.histoDisplayTags.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0747));
									this.histoDisplayTags.setSelection(this.settings.isDisplayTags());
									this.histoDisplayTags.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.log(Level.FINEST, "histoDisplayTags.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setDisplayTags(SettingsDialog.this.histoDisplayTags.getSelection());
											SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
										}
									});
								}
							} // end histoDisplay group
						} // end histoDisplayOptionGroup
						{
							this.histoScreening = new Group(this.histoComposite, SWT.NONE);
							RowData histoScreeningGroupRData = new RowData();
							histoScreeningGroupRData.width = 233;
							this.histoScreening.setLayoutData(histoScreeningGroupRData);
							FormLayout formLayout = new FormLayout();
							formLayout.marginTop = formLayout.marginLeft = formLayout.marginRight = formLayout.marginHeight = 2;
							this.histoScreening.setLayout(formLayout);
							this.histoScreening.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.histoScreening.setText(Messages.getString(MessageIds.GDE_MSGT0809));
							{
								this.histoRetrospectLabel = new CLabel(this.histoScreening, SWT.NONE);
								this.histoRetrospectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.histoRetrospectLabel.setText(Messages.getString(MessageIds.GDE_MSGT0832));
								this.histoRetrospectLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0833));
							}
							{
								this.histoRetrospectMonths = new Text(this.histoScreening, SWT.RIGHT | SWT.BORDER);
								FormData formData = new FormData();
								formData.right = new FormAttachment(100, -5);
								this.histoRetrospectMonths.setLayoutData(formData);
								this.histoRetrospectMonths.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.histoRetrospectMonths.setText(String.format("  %9d", SettingsDialog.this.settings.getRetrospectMonths()));
								this.histoRetrospectMonths.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0833));
								this.histoRetrospectMonths.addVerifyListener(new VerifyListener() {
									@Override
									public void verifyText(VerifyEvent e) {
										log.log(Level.FINEST, GDE.STRING_EMPTY + StringHelper.verifyPortInput(e.text.trim()));
										e.doit = StringHelper.verifyPortInput(e.text.trim());
									}
								});
								this.histoRetrospectMonths.addKeyListener(new KeyAdapter() {
									@Override
									public void keyReleased(KeyEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "histoRetrospectMonths.keyReleased, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setRetrospectMonths(SettingsDialog.this.histoRetrospectMonths.getText());
									}
								});
								this.histoRetrospectMonths.addVerifyListener(new VerifyListener() {
									@Override
									public void verifyText(VerifyEvent e) {
										if (!(Character.isDigit(e.character) || e.keyCode == 8 || e.keyCode == 127)) e.doit = false;
									}
								});
								this.histoRetrospectMonths.addFocusListener(new FocusListener() {
									private String trimmedInitialText;

									@Override
									public void focusLost(FocusEvent e) {
										SettingsDialog.log.log(Level.FINEST, "histoRetrospectMonths.focusLost, event=" + e); //$NON-NLS-1$
										if (!this.trimmedInitialText.equals(SettingsDialog.this.histoRetrospectMonths.getText().trim())) {
											SettingsDialog.log.log(Level.FINEST, "setText=" + SettingsDialog.this.settings.getRetrospectMonths()); //$NON-NLS-1$
											SettingsDialog.this.histoRetrospectMonths.setText(String.format("  %9d", SettingsDialog.this.settings.getRetrospectMonths()));
											SettingsDialog.this.application.getHistoExplorer().ifPresent(HistoExplorer::resetHisto);
										}
									}

									@Override
									public void focusGained(FocusEvent e) {
										SettingsDialog.this.histoRetrospectMonths.setText(String.format("  %9d", SettingsDialog.this.settings.getRetrospectMonths()));
										this.trimmedInitialText = SettingsDialog.this.histoRetrospectMonths.getText().trim();
										SettingsDialog.log.log(Level.FINEST, "histoRetrospectMonths.focusGained, text=" + this.trimmedInitialText); //$NON-NLS-1$
									}
								});
							}
							{
								this.histoSearchDataPathImports = new Button(this.histoScreening, SWT.CHECK);
								FormData formData = new FormData();
								formData.left = new FormAttachment(0, 5);
								formData.top = new FormAttachment(this.histoRetrospectMonths, 7);
								this.histoSearchDataPathImports.setLayoutData(formData);
								this.histoSearchDataPathImports.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.histoSearchDataPathImports.setText(Messages.getString(MessageIds.GDE_MSGT0834));
								this.histoSearchDataPathImports.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0835));
								this.histoSearchDataPathImports.setSelection(this.settings.getSearchDataPathImports());
								this.histoSearchDataPathImports.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "histoSearchDataPathImports.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setSearchDataPathImports(SettingsDialog.this.histoSearchDataPathImports.getSelection());
										SettingsDialog.this.application.getHistoExplorer().ifPresent(HistoExplorer::resetHisto);
									}
								});
							}
							{
								this.histoForceObject = new Group(this.histoScreening, SWT.NONE);
								RowLayout histoForceObjectGroupRLayout = new RowLayout(SWT.HORIZONTAL);
								histoForceObjectGroupRLayout.marginTop = 2;
								histoForceObjectGroupRLayout.marginLeft = 2;
								this.histoForceObject.setLayout(histoForceObjectGroupRLayout);
								FormData formData = new FormData();
								formData.top = new FormAttachment(this.histoSearchDataPathImports, 7);
								formData.width = 223;
								this.histoForceObject.setLayoutData(formData);
								this.histoForceObject.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.histoForceObject.setText(Messages.getString(MessageIds.GDE_MSGT0794));
								{
									this.histoIgnoreLogObjectKey = new Button(this.histoForceObject, SWT.CHECK | SWT.LEFT);
									this.histoIgnoreLogObjectKey.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.histoIgnoreLogObjectKey.setText(Messages.getString(MessageIds.GDE_MSGT0796));
									this.histoIgnoreLogObjectKey.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0797));
									this.histoIgnoreLogObjectKey.setSelection(this.settings.getIgnoreLogObjectKey());
									this.histoIgnoreLogObjectKey.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.log(Level.FINEST, "histoIgnoreLogObjectKey.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setIgnoreLogObjectKey(SettingsDialog.this.histoIgnoreLogObjectKey.getSelection());
											SettingsDialog.this.application.getHistoExplorer().ifPresent(HistoExplorer::resetHisto);
										}
									});
								}
							} // end histoForceObjectGroup
							{
								this.histoSubdirectoryLevelLabel = new CLabel(this.histoScreening, SWT.NONE);
								FormData formData = new FormData();
								formData.top = new FormAttachment(this.histoForceObject, 7);
								this.histoSubdirectoryLevelLabel.setLayoutData(formData);
								this.histoSubdirectoryLevelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.histoSubdirectoryLevelLabel.setBounds(10, GDE.IS_MAC ? 8 : 20, 120, 20);
								this.histoSubdirectoryLevelLabel.setText(Messages.getString(MessageIds.GDE_MSGT0871));
								this.histoSubdirectoryLevelLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0872));
							}
							{
								this.histoSubdirectoryLevel = new CCombo(this.histoScreening, SWT.BORDER);
								FormData formData = new FormData();
								formData.top = new FormAttachment(this.histoForceObject, 5);
								formData.right = new FormAttachment(100, -5);
								formData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
								formData.width = GDE.IS_LINUX ? 60 : 40;
								this.histoSubdirectoryLevel.setLayoutData(formData);
								this.histoSubdirectoryLevel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								//this.histoSamplingTimespan_ms.setBounds(370, GDE.IS_MAC ? 14 : 24, 47, GDE.IS_LINUX ? 22 : 20);
								this.histoSubdirectoryLevel.setItems(new String[] { " 0", " 1", " 2", " 3", " 4", " 5" });
								this.histoSubdirectoryLevel.setText(GDE.STRING_BLANK + String.valueOf(SettingsDialog.this.settings.getSubDirectoryLevelMax()));
								this.histoSubdirectoryLevel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0872));
								this.histoSubdirectoryLevel.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "histoSubdirectoryLevel.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setSubDirectoryLevelMax(SettingsDialog.this.histoSubdirectoryLevel.getText().trim());
										SettingsDialog.this.application.getHistoExplorer().ifPresent(HistoExplorer::resetHisto);
									}
								});
							}
						} // end histoScreening group
						{
							this.histoFileContents = new Group(this.histoComposite, SWT.NONE);
							RowData histoFileContentsGroupRData = new RowData();
							histoFileContentsGroupRData.width = 223;
							this.histoFileContents.setLayoutData(histoFileContentsGroupRData);
							FormLayout formLayout1 = new FormLayout();
							formLayout1.marginTop = formLayout1.marginLeft = formLayout1.marginRight = formLayout1.marginHeight = 2;
							this.histoFileContents.setLayout(formLayout1);
							this.histoFileContents.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.histoFileContents.setText(Messages.getString(MessageIds.GDE_MSGT0804));
							{
								this.histoSamplingLabel = new CLabel(this.histoFileContents, SWT.NONE);
								FormData formData = new FormData();
								this.histoSamplingLabel.setLayoutData(formData);
								this.histoSamplingLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.histoSamplingLabel.setBounds(10, GDE.IS_MAC ? 8 : 20, 120, 20);
								this.histoSamplingLabel.setText(Messages.getString(MessageIds.GDE_MSGT0807));
								this.histoSamplingLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0808));
							}
							{
								this.histoSamplingTimespan_ms = new CCombo(this.histoFileContents, SWT.BORDER);
								FormData formData = new FormData();
								formData.top = new FormAttachment(this.histoMaxDurationLabel, 5);
								formData.right = new FormAttachment(100, -5);
								formData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
								formData.width = GDE.IS_LINUX ? 80 : 70;
								this.histoSamplingTimespan_ms.setLayoutData(formData);
								this.histoSamplingTimespan_ms.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								//this.histoSamplingTimespan_ms.setBounds(370, GDE.IS_MAC ? 14 : 24, 47, GDE.IS_LINUX ? 22 : 20);
								this.histoSamplingTimespan_ms.setItems(Settings.getSamplingTimespanValues());
								this.histoSamplingTimespan_ms.setText(String.valueOf(SettingsDialog.this.settings.getSamplingTimespan_ms() / 1000.));
								this.histoSamplingTimespan_ms.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0808));
								this.histoSamplingTimespan_ms.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "histoSamplingTimespan_ms.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setSamplingTimespan_ms(SettingsDialog.this.histoSamplingTimespan_ms.getText().trim());
										SettingsDialog.this.application.getHistoExplorer().ifPresent(HistoExplorer::resetHisto);
									}
								});
							}
							{
								this.histoChannelMix = new Button(this.histoFileContents, SWT.CHECK);
								FormData formData = new FormData();
								formData.left = new FormAttachment(0, 5);
								formData.top = new FormAttachment(this.histoSamplingLabel, 7);
								this.histoChannelMix.setLayoutData(formData);
								this.histoChannelMix.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.histoChannelMix.setText(Messages.getString(MessageIds.GDE_MSGT0826));
								this.histoChannelMix.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0827));
								this.histoChannelMix.setSelection(this.settings.isChannelMix());
								this.histoChannelMix.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "histoChannelMix.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setChannelMix(SettingsDialog.this.histoChannelMix.getSelection());
										SettingsDialog.this.application.getHistoExplorer().ifPresent(HistoExplorer::resetHisto);
									}
								});
							}
						} // end histoFileContentsGroup
					} // end histoComposite
				} // end histo tab item
				{
					this.miscTabItem = new CTabItem(this.settingsTabFolder, SWT.NONE);
					this.miscTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.miscTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0691));
					{
						this.miscComposite = new Composite(this.settingsTabFolder, SWT.NONE);
						this.miscTabItem.setControl(this.miscComposite);
						RowLayout composite1Layout = new RowLayout(org.eclipse.swt.SWT.VERTICAL);
						this.miscComposite.setLayout(composite1Layout);
						{
							this.fontSizeGroup = new Group(this.miscComposite, SWT.NONE);
							RowLayout fontSizeGroupLayout = new RowLayout(SWT.HORIZONTAL);
							fontSizeGroupLayout.center = true;
							fontSizeGroupLayout.marginTop = 2;
							fontSizeGroupLayout.marginWidth = 20;
							fontSizeGroupLayout.spacing = 5;
							this.fontSizeGroup.setLayout(fontSizeGroupLayout);
							RowData fontSizeGroupLData = new RowData();
							fontSizeGroupLData.width = 478;
							//fontSizeGroupLData.height = 70;
							this.fontSizeGroup.setLayoutData(fontSizeGroupLData);
							this.fontSizeGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.fontSizeGroup.setText(Messages.getString(MessageIds.GDE_MSGT0727));
							this.fontSizeGroup.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0953));
							{
								this.fontSizeCorrectionSlider = new ParameterConfigControl(this.fontSizeGroup, this.fontCorrection, 0, GDE.STRING_EMPTY, String.format("%s / 10", Messages.getString(MessageIds.GDE_MSGT0276)), 140, GDE.STRING_EMPTY, 0, true, 
										30,	250, 10, GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18);
								this.fontSizeCorrectionSlider.setSliderSelection((int) (this.settings.getFontDisplayDensityAdaptionFactor() * 10));
								this.fontSizeGroup.addListener(SWT.Selection, new Listener() {
									@Override
									public void handleEvent(Event evt) {
										SettingsDialog.this.settings.setFontDisplayDensityAdaptionFactor(SettingsDialog.this.fontCorrection[0] / 10.0);
									}
								});
							}
							this.fontSizeGroup.layout();
						}
						{
							this.graphicsView = new Group(this.miscComposite, SWT.NONE);
							RowLayout chargerSpecialsLayout = new RowLayout(SWT.HORIZONTAL);
							chargerSpecialsLayout.center = true;
							chargerSpecialsLayout.marginTop = 2;
							chargerSpecialsLayout.marginWidth = 20;
							chargerSpecialsLayout.spacing = 5;
							this.graphicsView.setLayout(chargerSpecialsLayout);
							RowData graphicsViewLData = new RowData();
							graphicsViewLData.width = 478;
							//graphicsViewLData.height = 160;
							this.graphicsView.setLayoutData(graphicsViewLData);
							this.graphicsView.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.graphicsView.setText(Messages.getString(MessageIds.GDE_MSGT0692));
							{
								Label label = new Label(this.graphicsView, SWT.LEFT);
								RowData labelLData = new RowData();
								labelLData.width = 460;
								labelLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
								label.setLayoutData(labelLData);
								label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
								label.setText(Messages.getString(MessageIds.GDE_MSGT0693));
							}
							{
								this.drawScaleInRecordColorButton = new Button(this.graphicsView, SWT.CHECK);
								this.drawScaleInRecordColorButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.drawScaleInRecordColorButton.setText(Messages.getString(MessageIds.GDE_MSGT0695));
								this.drawScaleInRecordColorButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0694));
								this.drawScaleInRecordColorButton.setSelection(this.settings.isDrawScaleInRecordColor());
								RowData createLauncerButtonLData = new RowData();
								createLauncerButtonLData.width = 460;
								createLauncerButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
								this.drawScaleInRecordColorButton.setLayoutData(createLauncerButtonLData);
								this.drawScaleInRecordColorButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "drawScaleInRecordColorButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setDrawScaleInRecordColor(SettingsDialog.this.drawScaleInRecordColorButton.getSelection());
										SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
									}
								});
							}
							{
								this.drawNameInRecordColorButton = new Button(this.graphicsView, SWT.CHECK);
								this.drawNameInRecordColorButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.drawNameInRecordColorButton.setText(Messages.getString(MessageIds.GDE_MSGT0696));
								this.drawNameInRecordColorButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0694));
								this.drawNameInRecordColorButton.setSelection(this.settings.isDrawNameInRecordColor());
								RowData createLauncerButtonLData = new RowData();
								createLauncerButtonLData.width = 460;
								createLauncerButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
								this.drawNameInRecordColorButton.setLayoutData(createLauncerButtonLData);
								this.drawNameInRecordColorButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "drawNameInRecordColorButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setDrawNameInRecordColor(SettingsDialog.this.drawNameInRecordColorButton.getSelection());
										SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
									}
								});
							}
							{
								this.drawNumbersInRecordColorButton = new Button(this.graphicsView, SWT.CHECK);
								this.drawNumbersInRecordColorButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.drawNumbersInRecordColorButton.setText(Messages.getString(MessageIds.GDE_MSGT0697));
								this.drawNumbersInRecordColorButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0694));
								this.drawNumbersInRecordColorButton.setSelection(this.settings.isDrawNumbersInRecordColor());
								RowData createLauncerButtonLData = new RowData();
								createLauncerButtonLData.width = 460;
								createLauncerButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
								this.drawNumbersInRecordColorButton.setLayoutData(createLauncerButtonLData);
								this.drawNumbersInRecordColorButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "drawNumbersInRecordColorButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setDrawNumbersInRecordColor(SettingsDialog.this.drawNumbersInRecordColorButton.getSelection());
										SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
									}
								});
							}
							{
								this.draw10TicksPerRecordButton = new Button(this.graphicsView, SWT.CHECK);
								this.draw10TicksPerRecordButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.draw10TicksPerRecordButton.setText(Messages.getString(MessageIds.GDE_MSGT0968));
								this.draw10TicksPerRecordButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0969));
								this.draw10TicksPerRecordButton.setSelection(this.settings.isDraw10TicksPerRecord());
								RowData createLauncerButtonLData = new RowData();
								createLauncerButtonLData.width = 460;
								createLauncerButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
								this.draw10TicksPerRecordButton.setLayoutData(createLauncerButtonLData);
								this.draw10TicksPerRecordButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "draw10TicksPerRecord.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setDraw10TicksPerRecord(SettingsDialog.this.draw10TicksPerRecordButton.getSelection());
										if (SettingsDialog.this.draw10TicksPerRecordButton.getSelection())
											SettingsDialog.this.application.updateAllTabs(false);											
										else {
											if (SettingsDialog.this.application.getActiveRecordSet() != null)
												SettingsDialog.this.application.getActiveChannel().applyTemplateScaleEndpoints(SettingsDialog.this.application.getActiveRecordSet().getName());
										}
											
										SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
									}
								});
							}
							{
								Label label = new Label(this.graphicsView, SWT.LEFT);
								RowData labelLData = new RowData();
								labelLData.width = 460;
								labelLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
								label.setLayoutData(labelLData);
								label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
								label.setText(Messages.getString(MessageIds.GDE_MSGT0699));
							}
							{
								this.addChannelConfigNameCurveCompareButton = new Button(this.graphicsView, SWT.CHECK);
								this.addChannelConfigNameCurveCompareButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.addChannelConfigNameCurveCompareButton.setText(Messages.getString(MessageIds.GDE_MSGT0700));
								this.addChannelConfigNameCurveCompareButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0701));
								this.addChannelConfigNameCurveCompareButton.setSelection(this.settings.isCurveCompareChannelConfigName());
								RowData createLauncerButtonLData = new RowData();
								createLauncerButtonLData.width = 460;
								createLauncerButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
								this.addChannelConfigNameCurveCompareButton.setLayoutData(createLauncerButtonLData);
								this.addChannelConfigNameCurveCompareButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "addChannelConfigNameCurveCompareButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setCurveCompareChannelConfigName(SettingsDialog.this.addChannelConfigNameCurveCompareButton.getSelection());
									}
								});
							}
							this.graphicsView.layout();
						}
						{
							this.dataTableGroup = new Group(this.miscComposite, SWT.NONE);
							RowLayout chargerSpecialsLayout = new RowLayout(SWT.HORIZONTAL);
							chargerSpecialsLayout.center = true;
							chargerSpecialsLayout.marginTop = 2;
							chargerSpecialsLayout.marginWidth = 20;
							chargerSpecialsLayout.spacing = 5;
							this.dataTableGroup.setLayout(chargerSpecialsLayout);
							RowData dataTableGroupLData = new RowData();
							dataTableGroupLData.width = 478;
							//dataTableGroupLData.height = 70;
							this.dataTableGroup.setLayoutData(dataTableGroupLData);
							this.dataTableGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.dataTableGroup.setText(Messages.getString(MessageIds.GDE_MSGT0703));
							{
								this.partialDataTableButton = new Button(this.dataTableGroup, SWT.CHECK);
								this.partialDataTableButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.partialDataTableButton.setText(Messages.getString(MessageIds.GDE_MSGT0704));
								this.partialDataTableButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0705));
								this.partialDataTableButton.setSelection(this.settings.isPartialDataTable());
								RowData createLauncerButtonLData = new RowData();
								createLauncerButtonLData.width = 400;
								createLauncerButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
								this.partialDataTableButton.setLayoutData(createLauncerButtonLData);
								this.partialDataTableButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "partialDataTableButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setPartialDataTable(SettingsDialog.this.partialDataTableButton.getSelection());
										SettingsDialog.this.application.updateAllTabs(true, false);
										SettingsDialog.this.application.getHistoExplorer().ifPresent(h -> h.updateHistoTabs(false, false, true));
									}
								});
							}
							this.dataTableGroup.layout();
						}
						{
							this.kmzExportGroup = new Group(this.miscComposite, SWT.NONE);
							RowLayout kmzExportGroupLayout = new RowLayout(SWT.HORIZONTAL);
							kmzExportGroupLayout.center = true;
							kmzExportGroupLayout.marginTop = 2;
							kmzExportGroupLayout.marginWidth = 20;
							kmzExportGroupLayout.spacing = 5;
							this.kmzExportGroup.setLayout(kmzExportGroupLayout);
							RowData kmzExportGroupLData = new RowData();
							kmzExportGroupLData.width = 478;
							//kmzExportGroupLData.height = 70;
							this.kmzExportGroup.setLayoutData(kmzExportGroupLData);
							this.kmzExportGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.kmzExportGroup.setText("KML/KMZ Export");
							{
								this.kmzExportTimeStepSlider = new ParameterConfigControl(this.kmzExportGroup, this.exportTimeStep_ms, 0, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT0702), 80, "[ms]", 40, true, 
										40,	250, 10, 1000);
								this.kmzExportTimeStepSlider.setSliderSelection(this.settings.getKmzExportTimeStep_ms());
								this.kmzExportGroup.addListener(SWT.Selection, new Listener() {
									@Override
									public void handleEvent(Event evt) {
										SettingsDialog.this.settings.setKmzExportTimeStep_ms(SettingsDialog.this.exportTimeStep_ms[0]);
									}
								});
							}
							this.kmzExportGroup.layout();
						}

						{
							this.chargerSpecials = new Group(this.miscComposite, SWT.NONE);
							RowLayout chargerSpecialsLayout = new RowLayout(SWT.HORIZONTAL);
							chargerSpecialsLayout.center = true;
							chargerSpecialsLayout.marginTop = 2;
							chargerSpecialsLayout.marginWidth = 20;
							chargerSpecialsLayout.spacing = 5;
							this.chargerSpecials.setLayout(chargerSpecialsLayout);
							RowData chargerSpecialsLData = new RowData();
							chargerSpecialsLData.width = 478;
							//chargerSpecialsLData.height = 80;
							this.chargerSpecials.setLayoutData(chargerSpecialsLData);
							this.chargerSpecials.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.chargerSpecials.setText(Messages.getString(MessageIds.GDE_MSGT0698));
							{
								this.blankChargeDischargeButton = new Button(this.chargerSpecials, SWT.CHECK);
								this.blankChargeDischargeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.blankChargeDischargeButton.setText(Messages.getString(MessageIds.GDE_MSGT0688));
								this.blankChargeDischargeButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0689));
								this.blankChargeDischargeButton.setSelection(this.settings.isReduceChargeDischarge());
								RowData createLauncerButtonLData = new RowData();
								createLauncerButtonLData.width = 400;
								createLauncerButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
								this.blankChargeDischargeButton.setLayoutData(createLauncerButtonLData);
								this.blankChargeDischargeButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "blankChargeDischargeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setReduceChargeDischarge(SettingsDialog.this.blankChargeDischargeButton.getSelection());
										if (SettingsDialog.this.settings.isContinuousRecordSet() && SettingsDialog.this.settings.isReduceChargeDischarge()
												&& SettingsDialog.this.application.getActiveDevice().getName().startsWith("MC3000") && SettingsDialog.this.application.getActiveDevice().getName().startsWith("Q200")) {
											// continuous recording without breaks like pauses or termination phase may lead in combination record sets to
											// time gaps which can not be realized, so charger specific implementation take place here
											SettingsDialog.this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI0059));
										}
									}
								});
							}
							{
								this.continiousRecordSetButton = new Button(this.chargerSpecials, SWT.CHECK);
								this.continiousRecordSetButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.continiousRecordSetButton.setText(Messages.getString(MessageIds.GDE_MSGT0686));
								this.continiousRecordSetButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0687));
								this.continiousRecordSetButton.setSelection(this.settings.isContinuousRecordSet());
								RowData createLauncerButtonLData = new RowData();
								createLauncerButtonLData.width = 400;
								createLauncerButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
								this.continiousRecordSetButton.setLayoutData(createLauncerButtonLData);
								this.continiousRecordSetButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "continiousRecordSetButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setContinuousRecordSet(SettingsDialog.this.continiousRecordSetButton.getSelection());
										if (SettingsDialog.this.settings.isContinuousRecordSet() && SettingsDialog.this.settings.isReduceChargeDischarge()
												&& SettingsDialog.this.application.getActiveDevice().getName().startsWith("MC3000") && SettingsDialog.this.application.getActiveDevice().getName().startsWith("Q200")) {
											// continuous recording without breaks like pauses or termination phase may lead in combination record sets to
											// time gaps which can not be realized, so charger specific implementation take place here
											SettingsDialog.this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI0059));
										}
									}
								});
							}
							{
								this.startCommunicationAfterStartupButton = new Button(this.chargerSpecials, SWT.CHECK);
								this.startCommunicationAfterStartupButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.startCommunicationAfterStartupButton.setText(Messages.getString(MessageIds.GDE_MSGT0690));
								this.startCommunicationAfterStartupButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0970));
								this.startCommunicationAfterStartupButton.setSelection(this.settings.isStartDeviceCommunicationAfterStartup());
								RowData createLauncerButtonLData = new RowData();
								createLauncerButtonLData.width = 400;
								createLauncerButtonLData.height = GDE.IS_LINUX ? 22 : GDE.IS_MAC ? 20 : 18;
								this.startCommunicationAfterStartupButton.setLayoutData(createLauncerButtonLData);
								this.startCommunicationAfterStartupButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "startCommunicationAfterStartupButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setStartDeviceCommunicationAfterStartup(SettingsDialog.this.startCommunicationAfterStartupButton.getSelection());
									}
								});
							}
							this.chargerSpecials.layout();
						}
					}
				}
				{
					this.osMiscTabItem = new CTabItem(this.settingsTabFolder, SWT.NONE);
					this.osMiscTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.osMiscTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0303));
					{
						this.osMiscComposite = new Composite(this.settingsTabFolder, SWT.NONE);
						this.osMiscTabItem.setControl(this.osMiscComposite);
						FillLayout composite1Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
						this.osMiscComposite.setLayout(composite1Layout);
						{
							this.desktopLauncher = new Group(this.osMiscComposite, SWT.NONE);
							RowLayout desktopLauncherLayout = new RowLayout(SWT.HORIZONTAL);
							desktopLauncherLayout.center = true;
							desktopLauncherLayout.marginTop = 20;
							desktopLauncherLayout.marginLeft = 55;
							desktopLauncherLayout.spacing = 10;
							this.desktopLauncher.setLayout(desktopLauncherLayout);
							this.desktopLauncher.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.desktopLauncher.setText(Messages.getString(MessageIds.GDE_MSGT0362));
							{
								this.createLauncherButton = new Button(this.desktopLauncher, SWT.PUSH | SWT.CENTER);
								this.createLauncherButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.createLauncherButton.setText(Messages.getString(MessageIds.GDE_MSGT0363));
								RowData createLauncerButtonLData = new RowData();
								createLauncerButtonLData.width = 180;
								createLauncerButtonLData.height = 30;
								this.createLauncherButton.setLayoutData(createLauncerButtonLData);
								this.createLauncherButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "createLauncerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										OperatingSystemHelper.createDesktopLink();
									}
								});
							}
							{
								this.removeLauncherButton = new Button(this.desktopLauncher, SWT.PUSH | SWT.CENTER);
								this.removeLauncherButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.removeLauncherButton.setText(Messages.getString(MessageIds.GDE_MSGT0364));
								RowData removeLauncherButtonLData = new RowData();
								removeLauncherButtonLData.width = 180;
								removeLauncherButtonLData.height = 30;
								this.removeLauncherButton.setLayoutData(removeLauncherButtonLData);
								this.removeLauncherButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "removeLauncherButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										OperatingSystemHelper.removeDesktopLink();
									}
								});
							}
							if (GDE.IS_MAC) {
								this.createLauncherButton.setEnabled(false);
								this.removeLauncherButton.setEnabled(false);
							}
						}
						{
							this.shellMimeType = new Group(this.osMiscComposite, SWT.NONE);
							RowLayout shellMimeTypeLayout = new RowLayout(SWT.HORIZONTAL);
							shellMimeTypeLayout.center = true;
							shellMimeTypeLayout.marginTop = 20;
							shellMimeTypeLayout.marginLeft = 55;
							shellMimeTypeLayout.spacing = 10;
							this.shellMimeType.setLayout(shellMimeTypeLayout);
							this.shellMimeType.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.shellMimeType.setText(Messages.getString(MessageIds.GDE_MSGT0365));
							{
								this.assocMimeTypeButton = new Button(this.shellMimeType, SWT.PUSH | SWT.CENTER);
								this.assocMimeTypeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.assocMimeTypeButton.setText(Messages.getString(MessageIds.GDE_MSGT0366));
								RowData assocMimeTypeButtonLData = new RowData();
								assocMimeTypeButtonLData.width = 180;
								assocMimeTypeButtonLData.height = 30;
								this.assocMimeTypeButton.setLayoutData(assocMimeTypeButtonLData);
								this.assocMimeTypeButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "assocMimeTypeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										OperatingSystemHelper.registerApplication();
									}
								});
							}
							{
								this.removeMimeAssocButton = new Button(this.shellMimeType, SWT.PUSH | SWT.CENTER);
								this.removeMimeAssocButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.removeMimeAssocButton.setText(Messages.getString(MessageIds.GDE_MSGT0367));
								RowData removeMimeAssocButtonLData = new RowData();
								removeMimeAssocButtonLData.width = 180;
								removeMimeAssocButtonLData.height = 30;
								this.removeMimeAssocButton.setLayoutData(removeMimeAssocButtonLData);
								this.removeMimeAssocButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "removeMimeAssocButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										OperatingSystemHelper.deregisterApplication();
									}
								});
							}
							if (GDE.IS_MAC) {
								this.assocMimeTypeButton.setEnabled(false);
								this.removeMimeAssocButton.setEnabled(false);
							}
						}
						{
							this.objectKeyGroup = new Group(this.osMiscComposite, SWT.NONE);
							RowLayout objectKeyGroupLayout = new RowLayout(SWT.HORIZONTAL);
							objectKeyGroupLayout.center = true;
							objectKeyGroupLayout.marginTop = 5;
							objectKeyGroupLayout.marginLeft = 55;
							objectKeyGroupLayout.spacing = 10;
							this.objectKeyGroup.setLayout(objectKeyGroupLayout);
							this.objectKeyGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.objectKeyGroup.setText(Messages.getString(MessageIds.GDE_MSGT0206));
							{
								this.scanObjectKeysButton = new Button(this.objectKeyGroup, SWT.PUSH | SWT.CENTER);
								this.scanObjectKeysButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.scanObjectKeysButton.setText(Messages.getString(MessageIds.GDE_MSGT0207));
								this.scanObjectKeysButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0208, new Object[] { this.settings.getDataFilePath() }));
								RowData scanObjectKeysButtonLData = new RowData();
								scanObjectKeysButtonLData.width = 180;
								scanObjectKeysButtonLData.height = 30;
								this.scanObjectKeysButton.setLayoutData(scanObjectKeysButtonLData);
								this.scanObjectKeysButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "scanObjectKeysButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										ObjectKeyCompliance.rebuildObjectKeys();
									}
								});
							}
							{
								this.cleanObjectReferecesButton = new Button(this.objectKeyGroup, SWT.PUSH | SWT.CENTER);
								this.cleanObjectReferecesButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.cleanObjectReferecesButton.setText(Messages.getString(MessageIds.GDE_MSGT0220));
								this.cleanObjectReferecesButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0221, new Object[] { this.settings.getDataFilePath() }));
								RowData cleanObjectReferecesButtonLData = new RowData();
								cleanObjectReferecesButtonLData.width = 180;
								cleanObjectReferecesButtonLData.height = 30;
								this.cleanObjectReferecesButton.setLayoutData(cleanObjectReferecesButtonLData);
								this.cleanObjectReferecesButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "cleanObjectReferecesButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										new Thread() {
											@Override
											public void run() {
												ObjectKeyScanner.cleanFileLinks();
												if (getParent().isDisposed())
													SettingsDialog.this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI0039));
												else
													SettingsDialog.this.application.openMessageDialogAsync(getParent(), Messages.getString(MessageIds.GDE_MSGI0039));
											}
										}.start();
									}
								});
							}
							{
								this.createObjectsFromDirectoriesButton = new Button(this.objectKeyGroup, SWT.PUSH | SWT.CENTER);
								this.createObjectsFromDirectoriesButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.createObjectsFromDirectoriesButton.setText(Messages.getString(MessageIds.GDE_MSGT0836));
								String toolTipExtension = String.format("\n%s \"%s\"", Messages.getString(MessageIds.GDE_MSGT0523), this.settings.getDataFilePath());
								String dataPathLines = Arrays.stream(Settings.getInstance().getDataFoldersCsv().split(GDE.STRING_CSV_SEPARATOR)) //
										.map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining("\",\n  \""));
								if (!dataPathLines.isEmpty()) {
									toolTipExtension += String.format("\n%s \"%s\"", Messages.getString(MessageIds.GDE_MSGT0824), dataPathLines);
								}
								String importPathLines = Arrays.stream(Settings.getInstance().getImportFoldersCsv().split(GDE.STRING_CSV_SEPARATOR)) //
										.map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining("\",\n  \""));
								if (!importPathLines.isEmpty()) {
									toolTipExtension += String.format("\n%s \"%s\"", Messages.getString(MessageIds.GDE_MSGT0825), importPathLines);
								}
								this.createObjectsFromDirectoriesButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0837, new Object[] {
										toolTipExtension }));
								RowData createObjectsFromDirectoriesButtonLData = new RowData();
								createObjectsFromDirectoriesButtonLData.width = 180;
								createObjectsFromDirectoriesButtonLData.height = 30;
								this.createObjectsFromDirectoriesButton.setLayoutData(createObjectsFromDirectoriesButtonLData);
								this.createObjectsFromDirectoriesButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "createObjectsFromDirectoriesButton.widgetSelected, event=", evt); //$NON-NLS-1$
										// find directories which might represent object keys
										DeviceConfigurations devices = DataExplorer.getInstance().getDeviceSelectionDialog().getDeviceConfigurations();
										Set<String> objectCandidates = ObjectKeyCompliance.defineObjectKeyNovelties(devices.getAllConfigurations());
										if (objectCandidates.size() > 0) {
											if (SWT.OK == SettingsDialog.this.application.openOkCancelMessageDialog(Messages.getString(MessageIds.GDE_MSGI0069, //
													new Object[] { objectCandidates.stream().sorted().collect(Collectors.toList()).toString() }))) {
												ObjectKeyCompliance.addObjectKeys(objectCandidates);
											}
										}
										else {
											SettingsDialog.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0070));
										}
									}
								});
							}
						}
						{
							this.histoToolsGroup = new Group(this.osMiscComposite, SWT.NONE);
							RowLayout histoToolsGroupLayout = new RowLayout(SWT.HORIZONTAL);
							histoToolsGroupLayout.center = true;
							histoToolsGroupLayout.marginTop = 20;
							histoToolsGroupLayout.marginLeft = 55;
							histoToolsGroupLayout.spacing = 10;
							this.histoToolsGroup.setLayout(histoToolsGroupLayout);
							this.histoToolsGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.histoToolsGroup.setText(Messages.getString(MessageIds.GDE_MSGT0738));
							{
								this.clearHistoCacheButton = new Button(this.histoToolsGroup, SWT.PUSH | SWT.CENTER);
								this.clearHistoCacheButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.clearHistoCacheButton.setText(Messages.getString(MessageIds.GDE_MSGT0829));
								this.clearHistoCacheButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0830, new Object[] { Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME).toString() }));
								RowData clearHistoCacheButtonLData = new RowData();
								clearHistoCacheButtonLData.width = 180;
								clearHistoCacheButtonLData.height = 30;
								this.clearHistoCacheButton.setLayoutData(clearHistoCacheButtonLData);
								this.clearHistoCacheButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "clearHistoCacheButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										((LocalAccess) DataAccess.getInstance()).resetHistolocations();
										SettingsDialog.this.application.openMessageDialog(((LocalAccess) DataAccess.getInstance()).resetHistoCache());
									}
								});
							}
							{
								this.clearSupplementFoldersButton = new Button(this.histoToolsGroup, SWT.PUSH | SWT.CENTER);
								this.clearSupplementFoldersButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.clearSupplementFoldersButton.setText(Messages.getString(MessageIds.GDE_MSGT0922));
								this.clearSupplementFoldersButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0923, new Object[] { SupplementObjectFolder.getSupplementObjectsPath().toString() }));
								RowData clearSupplementFoldersButtonLData = new RowData();
								clearSupplementFoldersButtonLData.width = 180;
								clearSupplementFoldersButtonLData.height = 30;
								this.clearSupplementFoldersButton.setLayoutData(clearSupplementFoldersButtonLData);
								this.clearSupplementFoldersButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "clearSupplementFoldersButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.application.openMessageDialog(SupplementObjectFolder.resetFolders());
									}
								});
							}
						}
						{
							this.miscDiagGroup = new Group(this.osMiscComposite, SWT.NONE);
							RowLayout miscDiagGroupLayout = new RowLayout(SWT.HORIZONTAL);
							miscDiagGroupLayout.center = true;
							miscDiagGroupLayout.marginTop = 20;
							miscDiagGroupLayout.marginLeft = 55;
							miscDiagGroupLayout.spacing = 10;
							this.miscDiagGroup.setLayout(miscDiagGroupLayout);
							this.miscDiagGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.miscDiagGroup.setText(Messages.getString(MessageIds.GDE_MSGT0209));
							{
								this.resourceConsumptionButton = new Button(this.miscDiagGroup, SWT.PUSH | SWT.CENTER);
								this.resourceConsumptionButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.resourceConsumptionButton.setText(Messages.getString(MessageIds.GDE_MSGT0210));
								this.resourceConsumptionButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0211));
								RowData resourceConsumptionButtonLData = new RowData();
								resourceConsumptionButtonLData.width = 180;
								resourceConsumptionButtonLData.height = 30;
								this.resourceConsumptionButton.setLayoutData(resourceConsumptionButtonLData);
								this.resourceConsumptionButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "resourceConsumptionButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SWTResourceManager.listResourceStatus(this.getClass().getSimpleName());
										SettingsDialog.log.log(Level.WARNING, String.format("Max Memory=%,11d   Total Memory=%,11d   Free Memory=%,11d", Runtime.getRuntime().maxMemory(),
												Runtime.getRuntime().totalMemory(), Runtime.getRuntime().freeMemory()));
									}
								});
							}
							{
								this.cleanSettingsButton = new Button(this.miscDiagGroup, SWT.PUSH | SWT.CENTER);
								this.cleanSettingsButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.cleanSettingsButton.setText(Messages.getString(MessageIds.GDE_MSGT0378));
								this.cleanSettingsButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0379));
								RowData resourceConsumptionButtonLData = new RowData();
								resourceConsumptionButtonLData.width = 180;
								resourceConsumptionButtonLData.height = 30;
								this.cleanSettingsButton.setLayoutData(resourceConsumptionButtonLData);
								this.cleanSettingsButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "cleanSettingsButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (SWT.YES == SettingsDialog.this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGW0544))) {
											System.setProperty(GDE.CLEAN_SETTINGS_WHILE_SHUTDOWN, GDE.STRING_TRUE);
										}
									}
								});
							}
						}
					}
				}
				{ // begin analysis tab item
					this.analysisTabItem = new CTabItem(this.settingsTabFolder, SWT.NONE);
					this.analysisTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.analysisTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0302));
					{ // begin logging group
						this.loggingGroup = new Group(this.settingsTabFolder, SWT.NONE);
						this.loggingGroup.setLayout(null);
						this.loggingGroup.setBounds(13, 8, 456, 399);
						this.loggingGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.loggingGroup.setText(Messages.getString(MessageIds.GDE_MSGT0340));
						this.analysisTabItem.setControl(this.loggingGroup);
						{ // begin gloabal logging settings
							this.globalLoggingComposite = new Composite(this.loggingGroup, SWT.NONE);
							this.globalLoggingComposite.setLayout(null);
							this.globalLoggingComposite.setBounds(12, 26, 466, 41);
							{
								this.globalLogLevel = new Button(this.globalLoggingComposite, SWT.CHECK | SWT.LEFT);
								this.globalLogLevel.setBounds(12, 12, 190, 22);
								this.globalLogLevel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.globalLogLevel.setText(Messages.getString(MessageIds.GDE_MSGT0341));
								this.globalLogLevel.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "globalLogLevel.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (SettingsDialog.this.globalLogLevel.getSelection()) {
											enableIndividualLogging(false);
											SettingsDialog.this.globalLoggingCombo.setEnabled(true);
											SettingsDialog.this.settings.setIsGlobalLogLevel("true"); //$NON-NLS-1$
										}
										else {
											enableIndividualLogging(true);
											SettingsDialog.this.globalLoggingCombo.setEnabled(false);
											SettingsDialog.this.settings.setIsGlobalLogLevel("false"); //$NON-NLS-1$
										}

									}
								});
							}
							{
								this.globalLoggingCombo = new CCombo(this.globalLoggingComposite, SWT.BORDER);
								this.globalLoggingCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.globalLoggingCombo.setBounds(214, 12, 212, GDE.IS_LINUX ? 22 : 20);
								this.globalLoggingCombo.setItems(Settings.LOGGING_LEVEL);
								this.globalLoggingCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "globalLoggingCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.GLOBAL_LOG_LEVEL, SettingsDialog.this.globalLoggingCombo.getText());
										SettingsDialog.this.globalLoggingCombo.setText(SettingsDialog.this.globalLoggingCombo.getText());
									}
								});
							}
						} // end gloabal logging settings
						{ // begin individual package based logging settings
							this.individualLoggingComosite = new Composite(this.loggingGroup, SWT.NONE);
							this.individualLoggingComosite.setLayout(null);
							this.individualLoggingComosite.setBounds(11, 90, 263, 317);
							{
								this.uiLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								RowLayout uiLevelLabelLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.uiLevelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.uiLevelLabel.setLayout(uiLevelLabelLayout);
								this.uiLevelLabel.setText(Messages.getString(MessageIds.GDE_MSGT0342));
								this.uiLevelLabel.setBounds(3, 3, 170, 20);
							}
							{
								this.uiLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.uiLevelCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.uiLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.uiLevelCombo.setBounds(183, 3, 79, GDE.IS_LINUX ? 22 : 20);
								this.uiLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "uiLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.UI_LOG_LEVEL, SettingsDialog.this.uiLevelCombo.getText());
									}
								});
							}
							{
								this.deviceLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								RowLayout cLabel1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.deviceLevelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.deviceLevelLabel.setLayout(cLabel1Layout);
								this.deviceLevelLabel.setText(Messages.getString(MessageIds.GDE_MSGT0343));
								this.deviceLevelLabel.setBounds(3, 27, 170, 20);
							}
							{
								this.deviceLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.deviceLevelCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.deviceLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.deviceLevelCombo.setBounds(183, 27, 79, GDE.IS_LINUX ? 22 : 20);
								this.deviceLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "deviceLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.DEVICE_LOG_LEVEL, SettingsDialog.this.deviceLevelCombo.getText());
									}
								});
							}
							{
								this.commonLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								RowLayout cLabel2Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.commonLevelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.commonLevelLabel.setLayout(cLabel2Layout);
								this.commonLevelLabel.setText(Messages.getString(MessageIds.GDE_MSGT0344));
								this.commonLevelLabel.setBounds(3, 51, 170, 20);
							}
							{
								this.commonLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.commonLevelCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.commonLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.commonLevelCombo.setBounds(183, 51, 79, GDE.IS_LINUX ? 22 : 20);
								this.commonLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "commonLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.DATA_LOG_LEVEL, SettingsDialog.this.commonLevelCombo.getText());
									}
								});
							}
							{
								this.configLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								RowLayout cLabel3Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.configLevelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.configLevelLabel.setLayout(cLabel3Layout);
								this.configLevelLabel.setText(Messages.getString(MessageIds.GDE_MSGT0345));
								this.configLevelLabel.setBounds(3, 75, 170, 20);
							}
							{
								this.configLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.configLevelCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.configLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.configLevelCombo.setBounds(183, 75, 79, GDE.IS_LINUX ? 22 : 20);
								this.configLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "configLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.CONFIG_LOG_LEVEL, SettingsDialog.this.configLevelCombo.getText());
									}
								});
							}
							{
								this.utilsLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.utilsLevelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.utilsLevelLabel.setLayout(cLabel4Layout);
								this.utilsLevelLabel.setText(Messages.getString(MessageIds.GDE_MSGT0346));
								this.utilsLevelLabel.setBounds(3, 99, 170, 20);
							}
							{
								this.utilsLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.utilsLevelCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.utilsLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.utilsLevelCombo.setBounds(183, 99, 79, GDE.IS_LINUX ? 22 : 20);
								this.utilsLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "utilsLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.UTILS_LOG_LEVEL, SettingsDialog.this.utilsLevelCombo.getText());
									}
								});
							}
							{
								this.fileIOLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								this.fileIOLevelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.fileIOLevelLabel.setLayout(cLabel4Layout);
								this.fileIOLevelLabel.setBounds(3, 124, 170, 20);
								this.fileIOLevelLabel.setText(Messages.getString(MessageIds.GDE_MSGT0347));
							}
							{
								this.fileIOLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.fileIOLevelCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.fileIOLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.fileIOLevelCombo.setBounds(183, 124, 79, GDE.IS_LINUX ? 22 : 20);
								this.fileIOLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "fileIOLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.FILE_IO_LOG_LEVEL, SettingsDialog.this.fileIOLevelCombo.getText());
									}
								});
							}
							{
								this.serialIOLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								this.serialIOLevelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.serialIOLevelLabel.setLayout(cLabel4Layout);
								this.serialIOLevelLabel.setText(Messages.getString(MessageIds.GDE_MSGT0348));
								this.serialIOLevelLabel.setBounds(3, 149, 170, 20);
							}
							{
								this.serialIOLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.serialIOLevelCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.serialIOLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.serialIOLevelCombo.setBounds(183, 149, 79, GDE.IS_LINUX ? 22 : 20);
								this.serialIOLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "serialIOLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.SERIAL_IO_LOG_LEVEL, SettingsDialog.this.serialIOLevelCombo.getText());
									}
								});
							}

						} // end individual package based logging settings
						{
							this.classBasedLabel = new Label(this.loggingGroup, SWT.CENTER);
							this.classBasedLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.classBasedLabel.setText(Messages.getString(MessageIds.GDE_MSGT0349));
							this.classBasedLabel.setBounds(286, 95, 192, 18);

							this.popupmenu = new Menu(this.dialogShell, SWT.POP_UP);
							this.logLevelMenu.createMenu(this.popupmenu);

							this.tree = new Tree(this.loggingGroup, SWT.NONE);
							this.tree.setLayout(null);
							this.tree.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.tree.setBounds(286, 120, 192, 290);
							this.tree.setMenu(this.popupmenu);
							this.tree.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									SettingsDialog.log.log(Level.FINEST, "tree.widgetSelected, event=" + evt); //$NON-NLS-1$
									TreeItem tmpItem = (TreeItem) evt.item;
									if (tmpItem != null && tmpItem.getParentItem() != null) {
										StringBuilder sb = new StringBuilder();
										TreeItem tmpParent = tmpItem;
										while (null != (tmpParent = tmpParent.getParentItem())) {
											sb.append(tmpParent.getText()).append("."); //$NON-NLS-1$
										}
										sb.append(tmpItem.getText());
										String loggerName = sb.toString();
										SettingsDialog.this.popupmenu.setData(SettingsDialog.LOGGER_NAME, loggerName);
									}
									else {
										SettingsDialog.this.popupmenu.setData(SettingsDialog.LOGGER_NAME, tmpItem.getText() + GDE.STRING_DOT);
									}
								}
							});
						}
					} // end logging group
					//} // end analysis composite
				} // end analysis tab item
				this.settingsTabFolder.setSelection(0);
				initialize();
			} // end tab folder

			this.dialogShell.addHelpListener(new HelpListener() {

				@Override
				public void helpRequested(HelpEvent evt) {
					SettingsDialog.log.log(Level.FINE, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
					SettingsDialog.this.application.openHelpDialog(GDE.STRING_EMPTY, "HelpInfo_1.html"); //$NON-NLS-1$
				}
			});
			this.dialogShell.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent evt) {
					SettingsDialog.log.log(Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					if (SettingsDialog.this.settings.getActiveDevice().startsWith(Settings.EMPTY)) SettingsDialog.this.settings.setActiveDevice(Settings.EMPTY_SIGNATURE);
					SettingsDialog.this.settings.store();
					// set logging levels
					SettingsDialog.this.settings.updateLogLevel();
					// check if black or white list enabled and some chars typed but not accepted
					if (SettingsDialog.this.settings.isSerialPortBlackListEnabled()) {
						SettingsDialog.this.settings.setSerialPortBlackList(SettingsDialog.this.serialPortBlackList.getText());
						System.clearProperty("gnu.io.rxtx.SerialPorts"); //$NON-NLS-1$
					}
					else if (SettingsDialog.this.settings.isSerialPortWhiteListEnabled()) {
						SettingsDialog.this.settings.setSerialPortWhiteList(SettingsDialog.this.serialPortWhiteList.getText());
						//System.setProperty("gnu.io.rxtx.SerialPorts", "COMx"); set by setSerialPortWhiteList()
					}
					else {
						System.clearProperty("gnu.io.rxtx.SerialPorts"); //$NON-NLS-1$
					}
					// check for changed local
					if (SettingsDialog.this.isLocaleLanguageChanged) {
						SettingsDialog.this.application.openMessageDialog(SettingsDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGT0304));
						SettingsDialog.this.application.reloadLanguageRelatedResources(); //this may cause several exceptions while a text compare is used
					}
				}
			});
			{ // begin ok button
				FormData okButtonLData = new FormData();
				okButtonLData.width = 250;
				okButtonLData.height = 30;
				okButtonLData.left = new FormAttachment(0, 1000, 116);
				okButtonLData.bottom = new FormAttachment(1000, 1000, -7);
				okButtonLData.right = new FormAttachment(1000, 1000, -122);
				this.okButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.okButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.okButton.setLayoutData(okButtonLData);
				this.okButton.setText(Messages.getString(MessageIds.GDE_MSGT0188));
				this.okButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						SettingsDialog.log.log(Level.FINEST, "okButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							SettingsDialog.this.dialogShell.dispose();
						}
				});
			} // end ok button

			this.dialogShell.setLocation(getParent().toDisplay(100, 10));
			this.dialogShell.open();

			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * get the index of preferences locale
	 */
	private int getLocalLanguageIndex() {
		int index = 0; // en
		String language = this.settings.getLocale().toString();
		for (; index < this.supportedLocals.length; index++) {
			if (this.supportedLocals[index].equals(language)) return index;
		}
		return index;
	}

	/**
	 * get the index of preferences locale
	 */
	private int getTimeFormatIndex() {
		int index = 0; // relativ
		String format = this.settings.getTimeFormat().toString().trim().substring(0, 3);
		for (; index < this.timeFormatCombo.getItemCount(); index++) {
			if (this.timeFormatCombo.getItems()[index].trim().startsWith(format))
				return index;
		}
		return index;
	}

	/**
	 * method to enable / disable log level group
	 */
	void enableIndividualLogging(boolean value) {
		this.uiLevelLabel.setEnabled(value);
		this.uiLevelCombo.setEnabled(value);
		this.commonLevelLabel.setEnabled(value);
		this.commonLevelCombo.setEnabled(value);
		this.deviceLevelLabel.setEnabled(value);
		this.deviceLevelCombo.setEnabled(value);
		this.configLevelLabel.setEnabled(value);
		this.configLevelCombo.setEnabled(value);
		this.utilsLevelLabel.setEnabled(value);
		this.utilsLevelCombo.setEnabled(value);
		this.fileIOLevelLabel.setEnabled(value);
		this.fileIOLevelCombo.setEnabled(value);
		this.serialIOLevelLabel.setEnabled(value);
		this.serialIOLevelCombo.setEnabled(value);
		this.tree.setEnabled(value);
	}

	/**
	 * updates the logging levels in dialog
	 */
	void updateLoggingLevels() {
		if (this.settings.getProperty(Settings.GLOBAL_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.GLOBAL_LOG_LEVEL, SettingsDialog.DEFAULT_LOG_LEVEL);
		}
		this.globalLoggingCombo.setText(this.settings.getProperty(Settings.GLOBAL_LOG_LEVEL));

		if (this.settings.getProperty(Settings.UI_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.UI_LOG_LEVEL, SettingsDialog.DEFAULT_LOG_LEVEL);
		}
		this.uiLevelCombo.setText(this.settings.getProperty(Settings.UI_LOG_LEVEL));

		if (this.settings.getProperty(Settings.DATA_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.DATA_LOG_LEVEL, SettingsDialog.DEFAULT_LOG_LEVEL);
		}
		this.commonLevelCombo.setText(this.settings.getProperty(Settings.DATA_LOG_LEVEL));

		if (this.settings.getProperty(Settings.DEVICE_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.DEVICE_LOG_LEVEL, SettingsDialog.DEFAULT_LOG_LEVEL);
		}
		this.deviceLevelCombo.setText(this.settings.getProperty(Settings.DEVICE_LOG_LEVEL));

		if (this.settings.getProperty(Settings.CONFIG_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.CONFIG_LOG_LEVEL, SettingsDialog.DEFAULT_LOG_LEVEL);
		}
		this.configLevelCombo.setText(this.settings.getProperty(Settings.CONFIG_LOG_LEVEL));

		if (this.settings.getProperty(Settings.UTILS_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.UTILS_LOG_LEVEL, SettingsDialog.DEFAULT_LOG_LEVEL);
		}
		this.utilsLevelCombo.setText(this.settings.getProperty(Settings.UTILS_LOG_LEVEL));

		if (this.settings.getProperty(Settings.FILE_IO_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.FILE_IO_LOG_LEVEL, SettingsDialog.DEFAULT_LOG_LEVEL);
		}
		this.fileIOLevelCombo.setText(this.settings.getProperty(Settings.FILE_IO_LOG_LEVEL));

		if (this.settings.getProperty(Settings.SERIAL_IO_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.SERIAL_IO_LOG_LEVEL, SettingsDialog.DEFAULT_LOG_LEVEL);
		}
		this.serialIOLevelCombo.setText(this.settings.getProperty(Settings.SERIAL_IO_LOG_LEVEL));
	}

	/**
	 * initialize buttons, logging according to settings
	 */
	private void initialize() {
		SettingsDialog.this.defaultDataPath.setText(new File(SettingsDialog.this.settings.getDataFilePath()).getAbsolutePath());

		SettingsDialog.this.suggestDate.setSelection(SettingsDialog.this.settings.getUsageDateAsFileNameLeader());
		SettingsDialog.this.suggestObjectKey.setSelection(SettingsDialog.this.settings.getUsageObjectKeyInFileName());
		SettingsDialog.this.writeTmpFiles.setSelection(SettingsDialog.this.settings.getUsageWritingTmpFiles());

		SettingsDialog.this.osSchema.setSelection(SettingsDialog.this.settings.getSkinColorSchema().equals(Settings.COLOR_SCHEMA_SYSTEM));
		SettingsDialog.this.lightSchema.setSelection(SettingsDialog.this.settings.getSkinColorSchema().equals(Settings.COLOR_SCHEMA_LIGHT));
		SettingsDialog.this.darkSchema.setSelection(SettingsDialog.this.settings.getSkinColorSchema().equals(Settings.COLOR_SCHEMA_DARK));

		SettingsDialog.this.deviceDialogModalButton.setSelection(SettingsDialog.this.settings.isDeviceDialogsModal());
		SettingsDialog.this.deviceDialogOnTopButton.setEnabled(!SettingsDialog.this.settings.isDeviceDialogsModal());
		SettingsDialog.this.deviceDialogOnTopButton.setSelection(SettingsDialog.this.settings.isDeviceDialogsOnTop());
		SettingsDialog.this.deviceDialogAlphaButton.setSelection(SettingsDialog.this.settings.isDeviceDialogAlphaEnabled());
		SettingsDialog.this.alphaSlider.setEnabled(SettingsDialog.this.settings.isDeviceDialogAlphaEnabled());
		SettingsDialog.this.alphaSlider.setSelection(SettingsDialog.this.settings.getDialogAlphaValue());
		SettingsDialog.this.deviceImportDialogButton.setSelection(SettingsDialog.this.settings.isDeviceImportDirectoryObjectRelated());

		SettingsDialog.this.decimalSeparator.setText(GDE.STRING_BLANK + SettingsDialog.this.settings.getDecimalSeparator());
		SettingsDialog.this.listSeparator.setText(GDE.STRING_BLANK + SettingsDialog.this.settings.getListSeparator());

		SettingsDialog.this.skipBluetoothDevices.setSelection(SettingsDialog.this.settings.isSkipBluetoothDevices());
		SettingsDialog.this.doPortAvailabilityCheck.setSelection(SettingsDialog.this.settings.doPortAvailabilityCheck());

		SettingsDialog.this.serialPortBlackList.setText(GDE.STRING_BLANK + SettingsDialog.this.settings.getSerialPortBlackList());
		boolean isBlacklistEnabled = SettingsDialog.this.settings.isSerialPortBlackListEnabled();
		SettingsDialog.this.enableBlackListButton.setSelection(isBlacklistEnabled);
		SettingsDialog.this.serialPortBlackList.setEditable(isBlacklistEnabled);
		SettingsDialog.this.serialPortBlackList.setEnabled(isBlacklistEnabled);

		SettingsDialog.this.serialPortWhiteList.setText(GDE.STRING_BLANK + SettingsDialog.this.settings.getSerialPortWhiteListString());
		boolean isWhitelistEnabled = SettingsDialog.this.settings.isSerialPortWhiteListEnabled();
		SettingsDialog.this.enableWhiteListButton.setSelection(isWhitelistEnabled);
		SettingsDialog.this.serialPortWhiteList.setEditable(isWhitelistEnabled);
		SettingsDialog.this.serialPortWhiteList.setEnabled(isWhitelistEnabled);

		SettingsDialog.this.globalLogLevel.setSelection(SettingsDialog.this.settings.isGlobalLogLevel());
		if (SettingsDialog.this.settings.isGlobalLogLevel()) {
			enableIndividualLogging(false);
			SettingsDialog.this.globalLoggingCombo.setEnabled(true);
		}
		else {
			enableIndividualLogging(true);
			SettingsDialog.this.globalLoggingCombo.setEnabled(false);
			SettingsDialog.this.globalLogLevel.setSelection(false);
		}
		updateLoggingLevels();

		SettingsDialog.this.tree.clearAll(true);
		LogManager manager = LogManager.getLogManager();
		Enumeration<String> loggerNames = manager.getLoggerNames();
		StringBuilder sb = new StringBuilder();
		while (loggerNames.hasMoreElements()) {
			String loggerName = loggerNames.nextElement();
			if (loggerName.startsWith(GDE.STRING_BASE_PACKAGE) && loggerName.replace(GDE.CHAR_DOT, GDE.CHAR_COLON).split(GDE.STRING_COLON).length >= 3) {
				sb.append(loggerName).append(GDE.STRING_SEMICOLON);
			}
		}
		String[] loggers = sb.toString().split(GDE.STRING_SEMICOLON);
		Arrays.sort(loggers);
		if (SettingsDialog.log.isLoggable(Level.FINER)) {
			for (String string : loggers) {
				SettingsDialog.log.log(Level.FINER, string);
			}
		}
		SettingsDialog.this.tree.removeAll();
		String root = GDE.STRING_EMPTY;
		TreeItem treeItemRoot = null;
		TreeItem treeItemNode;
		for (String string : loggers) {
			String[] tmp = string.replace(GDE.CHAR_DOT, GDE.CHAR_COLON).split(GDE.STRING_COLON);
			switch (tmp.length) {
			case 3:
				if (!root.equals(tmp[0] + GDE.STRING_DOT + tmp[1])) {
					root = tmp[0] + GDE.STRING_DOT + tmp[1];
					treeItemRoot = new TreeItem(SettingsDialog.this.tree, SWT.SINGLE);
					treeItemRoot.setText(root);
				}
				treeItemNode = new TreeItem(treeItemRoot, SWT.NULL);
				treeItemNode.setText(tmp[2]);
				break;
			case 4:
				if (!root.equals(tmp[0] + GDE.STRING_DOT + tmp[1] + GDE.STRING_DOT + tmp[2])) {
					root = tmp[0] + GDE.STRING_DOT + tmp[1] + GDE.STRING_DOT + tmp[2];
					treeItemRoot = new TreeItem(SettingsDialog.this.tree, SWT.SINGLE);
					treeItemRoot.setText(root);
				}
				treeItemNode = new TreeItem(treeItemRoot, SWT.NULL);
				treeItemNode.setText(tmp[3]);
				break;
			case 5:
				if (!root.equals(tmp[0] + GDE.STRING_DOT + tmp[1] + GDE.STRING_DOT + tmp[2] + GDE.STRING_DOT + tmp[3])) {
					root = tmp[0] + GDE.STRING_DOT + tmp[1] + GDE.STRING_DOT + tmp[2] + GDE.STRING_DOT + tmp[3];
					treeItemRoot = new TreeItem(SettingsDialog.this.tree, SWT.SINGLE);
					treeItemRoot.setText(root);
				}
				treeItemNode = new TreeItem(treeItemRoot, SWT.NULL);
				treeItemNode.setText(tmp[4]);
				break;
			}
		}
	}

}
