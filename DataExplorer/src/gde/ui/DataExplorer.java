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
    					2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.ui;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TaskBar;
import org.eclipse.swt.widgets.TaskItem;

import com.sun.istack.Nullable;

import gde.Analyzer;
import gde.Explorer;
import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.comm.IDeviceCommPort;
import gde.config.Settings;
import gde.data.AbstractRecordSet;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.ObjectData;
import gde.data.RecordSet;
import gde.device.ChannelTypes;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.device.resource.DeviceXmlResource;
import gde.histo.ui.HistoExplorer;
import gde.histo.ui.HistoSummaryWindow;
import gde.io.OsdReaderWriter;
import gde.log.Level;
import gde.log.LogFormatter;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.dialog.AboutDialog;
import gde.ui.dialog.DeviceSelectionDialog;
import gde.ui.dialog.FontSizeDialog;
import gde.ui.dialog.HelpInfoDialog;
import gde.ui.dialog.SettingsDialog;
import gde.ui.dialog.UpdateMessageBox;
import gde.ui.menu.MenuBar;
import gde.ui.menu.MenuToolBar;
import gde.ui.tab.AnalogWindow;
import gde.ui.tab.CellVoltageWindow;
import gde.ui.tab.DataTableWindow;
import gde.ui.tab.DigitalWindow;
import gde.ui.tab.FileCommentWindow;
import gde.ui.tab.GraphicsComposite.GraphicsMode;
import gde.ui.tab.GraphicsWindow;
import gde.ui.tab.GraphicsWindow.GraphicsType;
import gde.ui.tab.ObjectDescriptionWindow;
import gde.ui.tab.StatisticsWindow;
import gde.utils.FileUtils;
import gde.utils.ObjectKeyCompliance;
import gde.utils.OperatingSystemHelper;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;
import gde.utils.WebBrowser;

/**
 * Main application class of DataExplorer
 * @author Winfried Brügmann
 */
public class DataExplorer extends Composite {
	final static String	$CLASS_NAME	= DataExplorer.class.getName();
	final static Logger	log					= Logger.getLogger(DataExplorer.class.getName());

	final HashMap<String, String>	extensionFilterMap								= new HashMap<String, String>();

	public final static String		RECORD_NAME												= "recordName";																							//$NON-NLS-1$
	public final static String		NAME_REPLACEMENT									= "nameReplacement";																				//$NON-NLS-1$
	public final static String		CURVE_SELECTION_ITEM							= "curveSelectedItem";																			//$NON-NLS-1$
	public final static String		OLD_STATE													= "oldState";																								//$NON-NLS-1$

	public final Color			COLOR_WHITE												;
	public final Color			COLOR_LIGHT_GREY									;
	public final Color			COLOR_GREY												;
	public final Color			COLOR_CANVAS_YELLOW								;
	public final Color			COLOR_BLUE												;
	public final Color			COLOR_LIGHT_BLUE									;
	public final Color			COLOR_DARK_GREEN									;
	public final Color			COLOR_BLACK												;
	public final Color			COLOR_RED													;
	public Color						COLOR_BACKGROUND									;
	public Color						COLOR_FOREGROUND									;

	public final static int				TAB_INDEX_GRAPHIC									= 0;
	public final static int				TAB_INDEX_DATA_TABLE							= 1;
	public final static int				TAB_INDEX_DIGITAL									= 2;
	public final static int				TAB_INDEX_ANALOG									= 3;
	public final static int				TAB_INDEX_CELL_VOLTAGE						= 4;
	public final static int				TAB_INDEX_COMPARE									= 5;
	public final static int				TAB_INDEX_COMMENT									= 6;
	public final static int				TAB_INDEX_HISTO_SUMMARY						= 7;
	public final static int				TAB_INDEX_HISTO_TABLE							= 8;

	public final static String		COMPARE_RECORD_SET								= "compare_set";																						//$NON-NLS-1$
	public final static String		UTILITY_RECORD_SET								= "utility_set";																						//$NON-NLS-1$

	private static volatile DataExplorer	application								= null;

	gde.io.FileHandler						fileHandler;
	CTabFolder										displayTab;
	Settings											settings;
	Menu													menu;
	MenuBar												menuBar;
	CoolBar												menuCoolBar;
	int[]													order;
	int[]													wrapIndices;
	Point[]												sizes;

	MenuToolBar										menuToolBar;
	GraphicsWindow								graphicsTabItem;

	GraphicsWindow								compareTabItem;
	DataTableWindow								dataTableTabItem;
	StatisticsWindow							statisticsTabItem;
	DigitalWindow									digitalTabItem;
	AnalogWindow									analogTabItem;
	CellVoltageWindow							cellVoltageTabItem;
	FileCommentWindow							fileCommentTabItem;
	ObjectDescriptionWindow				objectDescriptionTabItem;
	final Vector<CTabItem>				customTabItems										= new Vector<CTabItem>();
	GraphicsWindow								utilGraphicsTabItem;
	Composite											tabComposite;
	Composite											statusComposite;
	StatusBar											statusBar;
	int														progessPercentage									= 0;
	boolean												isDeviceDialogModal;
	int														tabSelectedIndex;																																							// use for identifying the last selected tab

	SettingsDialog								settingsDialog;
	HelpInfoDialog								helpDialog;
	DeviceSelectionDialog					deviceSelectionDialog;

	RecordSet											compareSet;
	RecordSet											utilitySet;
	Optional<HistoExplorer>				histoExplorer											= Optional.empty();
	final long										threadId;
	String												progressBarUser										= null;
	TaskItem											taskBarItem;
	Thread												writeTmpFileThread;
	boolean												isTmpWriteStop										= false;

	boolean												isCurveSelectorEnabled						= true;																											// always enabled during startup - there is no
																																																															// setting. So true is mandatory.
	boolean												isRecordCommentVisible						= false;
	boolean												isGraphicsHeaderVisible						= false;
	boolean												isObjectWindowVisible							= false;

	int														openYesNoMessageDialogAsyncValue	= -1;
	Boolean 											openUpdateMessageDialogSyncValue 	= null;

	DropTarget										target;																																												// = new DropTarget(dropTable, operations);

	final FileTransfer						fileTransfer											= FileTransfer.getInstance();
	Transfer[]										types															= new Transfer[] { this.fileTransfer };

	private Analyzer							analyzer;

	/**
	 * update all visualization windows
	 */
	private boolean								isUpdateAllTabs										= true;

	/**
	 * main application class constructor
	 */
	private DataExplorer() {
		super(GDE.shell, SWT.NONE);
		this.threadId = Thread.currentThread().getId();

		SWTResourceManager.registerResourceUser(this);
COLOR_WHITE												= SWTResourceManager.getColor(SWT.COLOR_WHITE);
COLOR_LIGHT_GREY									= SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND);
COLOR_GREY												= SWTResourceManager.getColor(SWT.COLOR_GRAY);
COLOR_CANVAS_YELLOW								= SWTResourceManager.getColor(250, 249, 211);
COLOR_BLUE												= SWTResourceManager.getColor(SWT.COLOR_BLUE);
COLOR_LIGHT_BLUE									= SWTResourceManager.getColor(239, 239, 255);
COLOR_DARK_GREEN									= SWTResourceManager.getColor(SWT.COLOR_DARK_GREEN);
COLOR_BLACK												= SWTResourceManager.getColor(SWT.COLOR_BLACK);
COLOR_RED													= SWTResourceManager.getColor(SWT.COLOR_RED);
COLOR_BACKGROUND									= SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND);
COLOR_FOREGROUND									= SWTResourceManager.getColor(SWT.COLOR_WIDGET_FOREGROUND);





	}

	/**
	 * main application class constructor
	 */
	private void initialize() {
		analyzer = Analyzer.getInstance();

		this.extensionFilterMap.put(GDE.FILE_ENDING_OSD, Messages.getString(MessageIds.GDE_MSGT0139));
		this.extensionFilterMap.put(GDE.FILE_ENDING_LOV, Messages.getString(MessageIds.GDE_MSGT0140));
		this.extensionFilterMap.put(GDE.FILE_ENDING_CSV, Messages.getString(MessageIds.GDE_MSGT0141));
		this.extensionFilterMap.put(GDE.FILE_ENDING_XML, Messages.getString(MessageIds.GDE_MSGT0142));
		this.extensionFilterMap.put(GDE.FILE_ENDING_PNG, Messages.getString(MessageIds.GDE_MSGT0213));
		this.extensionFilterMap.put(GDE.FILE_ENDING_GIF, Messages.getString(MessageIds.GDE_MSGT0214));
		this.extensionFilterMap.put(GDE.FILE_ENDING_JPG, Messages.getString(MessageIds.GDE_MSGT0215));
		this.extensionFilterMap.put(GDE.FILE_ENDING_KMZ, Messages.getString(MessageIds.GDE_MSGT0222));
		this.extensionFilterMap.put(GDE.FILE_ENDING_GPX, Messages.getString(MessageIds.GDE_MSGT0677));
		this.extensionFilterMap.put(GDE.FILE_ENDING_STAR, Messages.getString(GDE.IS_WINDOWS ? MessageIds.GDE_MSGT0216 : MessageIds.GDE_MSGT0676));
		this.extensionFilterMap.put(GDE.FILE_ENDING_INI, Messages.getString(MessageIds.GDE_MSGT0368));
		this.extensionFilterMap.put(GDE.FILE_ENDING_LOG, Messages.getString(MessageIds.GDE_MSGT0672));
		this.extensionFilterMap.put(GDE.FILE_ENDING_JML, Messages.getString(MessageIds.GDE_MSGT0673));
		this.extensionFilterMap.put(GDE.FILE_ENDING_BIN, Messages.getString(MessageIds.GDE_MSGT0950));
		this.extensionFilterMap.put(GDE.FILE_ENDING_TXT, Messages.getString(MessageIds.GDE_MSGT0951));
		this.extensionFilterMap.put(GDE.FILE_ENDING_BIN_LOG, Messages.getString(MessageIds.GDE_MSGT0952));
	}

	/**
	 * get the instance of singleton DataExplorer
	 */
	public static DataExplorer getInstance() {
		if (DataExplorer.application == null) {
			DataExplorer.application = new DataExplorer();
			// synchronize now to avoid a performance penalty in case of frequent getInstance calls
			synchronized (DataExplorer.application) {
				DataExplorer.application.initialize();
			}
		}
		return DataExplorer.application;
	}

	public static boolean isInitialized() {
		return DataExplorer.application != null;
	}

	/**
	 * Initializes the GUI.
	 */
	private void initGUI() {
		// final String $METHOD_NAME = "initGUI"; //$NON-NLS-1$
		try {
			this.setColorSchemaColors(this.settings.getSkinColorSchema());
			this.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
			this.setBackground(this.COLOR_BACKGROUND);

			GridLayout thisLayout = new GridLayout(1, true);
			thisLayout.marginWidth = 0;
			thisLayout.marginHeight = 0;
			thisLayout.numColumns = 1;
			thisLayout.makeColumnsEqualWidth = true;
			thisLayout.horizontalSpacing = 0;
			thisLayout.verticalSpacing = 0;
			this.setLayout(thisLayout);
			{
				this.menu = new Menu(GDE.shell, SWT.BAR);
				this.menuBar = new MenuBar(this, this.menu);
				this.menuBar.create();
				GDE.shell.setMenuBar(this.menu);
			}
			{
				this.menuCoolBar = new CoolBar(this, SWT.FLAT);
				this.menuCoolBar.setBackground(this.COLOR_BACKGROUND);
				GridData menuCoolBarLData = new GridData();
				menuCoolBarLData.horizontalAlignment = GridData.FILL;
				menuCoolBarLData.verticalAlignment = GridData.BEGINNING;
				menuCoolBarLData.grabExcessHorizontalSpace = true;
				this.menuCoolBar.setLayoutData(menuCoolBarLData);
				// this.menuCoolBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				this.menuToolBar = new MenuToolBar(this, this.menuCoolBar);
				this.menuToolBar.create();
				// restore cool bar items position and wrap
				this.menuCoolBar.setItemLayout(this.settings.getCoolBarOrder(DataExplorer.getInstance().getMenuToolBar().getCoolBarSizes()), this.settings.getCoolBarWraps(), this.settings.getCoolBarSizes(DataExplorer.getInstance().getMenuToolBar().getCoolBarSizes()));
			}
			{ // begin main tab display
				this.displayTab = new CTabFolder(this, SWT.FLAT);
				this.displayTab.setBackground(new Color[]{this.COLOR_BACKGROUND, this.settings.getGraphicsSurroundingBackground()}, new int[]{100}, true);
				this.displayTab.setForeground(this.COLOR_FOREGROUND);
				GridData tabCompositeLData = new GridData();
				tabCompositeLData.verticalAlignment = GridData.FILL;
				tabCompositeLData.horizontalAlignment = GridData.FILL;
				tabCompositeLData.grabExcessHorizontalSpace = true;
				tabCompositeLData.grabExcessVerticalSpace = true;
				this.displayTab.setLayoutData(tabCompositeLData);
				this.displayTab.setSimple(false);
				{
					this.graphicsTabItem = new GraphicsWindow(this.displayTab, SWT.NONE, GraphicsType.NORMAL, Messages.getString(MessageIds.GDE_MSGT0143), 0);
					this.graphicsTabItem.create();
				}
				this.displayTab.setSelection(0);
			} // end main tab display
			{
				this.statusComposite = new Composite(this, SWT.NONE);
				GridData statusCompositeLData = new GridData();
				statusCompositeLData.grabExcessHorizontalSpace = true;
				statusCompositeLData.horizontalAlignment = GridData.FILL;
				statusCompositeLData.verticalAlignment = GridData.END;
				this.statusComposite.setLayoutData(statusCompositeLData);
				this.statusBar = new StatusBar(this.statusComposite);
				this.statusBar.create();
				log.info(() -> "Statusbar created");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * execute DataExplorer
	 */
	public void execute(final String inputFilePath) {
		final String $METHOD_NAME = "execute"; //$NON-NLS-1$
		try {
			// cleanup possible old files and native libraries
			FileUtils.cleanupPre();

			// init settings
			this.settings = Settings.getInstance();
			log.logp(Level.INFO, $CLASS_NAME, $METHOD_NAME, this.settings.toString());

			new Thread("updateAvailablePorts") {
				@Override
				public void run() {
					try {
						if (DataExplorer.this.getActiveDevice() != null && DataExplorer.this.getActiveDevice().getDeviceConfiguration() != null
								&& DataExplorer.this.getActiveDevice().getDeviceConfiguration().getSerialPortType() != null) {
							//RXTXcomm usage: DeviceSerialPortImpl.listConfiguredSerialPorts(...);
							DeviceCommPort.listConfiguredSerialPorts(DataExplorer.this.settings.doPortAvailabilityCheck(),
									DataExplorer.this.settings.isSerialPortBlackListEnabled() ? DataExplorer.this.settings.getSerialPortBlackList() : GDE.STRING_EMPTY,
									DataExplorer.this.settings.isSerialPortWhiteListEnabled() ? DataExplorer.this.settings.getSerialPortWhiteList() : new Vector<String>());
						}
					} catch (Throwable t) {
						log.log(java.util.logging.Level.WARNING, t.getMessage(), t);
					}
					log.log(Level.TIME, "updateAvailablePortsThread time =", StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime)));
				}
			}.start();

			this.isDeviceDialogModal = this.settings.isDeviceDialogsModal();

			if (this.settings.getWindow().width < 600) this.settings.setWindow(new Point(this.settings.getWindow().x,
					this.settings.getWindow().y), new Point(600, this.settings.getWindow().height));
			if (this.settings.getWindow().height < 400) this.settings.setWindow(new Point(this.settings.getWindow().x,
					this.settings.getWindow().y), new Point(this.settings.getWindow().width, 400));
			if (this.settings.isWindowMaximized()) {
				GDE.shell.setLocation(this.settings.getWindow().x, this.settings.getWindow().y);
				GDE.shell.setSize(this.settings.getWindow().width, this.settings.getWindow().height);
				GDE.shell.setMaximized(true);
			} else {
				Rectangle displayBounds = GDE.display.getBounds();
				if (this.settings.getWindow().x < displayBounds.x || this.settings.getWindow().x > (displayBounds.width + displayBounds.x) // check location
																																																																		// x,y inside
																																																																		// display bounds
						|| this.settings.getWindow().y < displayBounds.y || this.settings.getWindow().y > (displayBounds.height + displayBounds.y)) {
					GDE.shell.setLocation(50, 50);
					GDE.shell.setSize(this.settings.getWindow().width, this.settings.getWindow().height);
				} else {
					GDE.shell.setBounds(this.settings.getWindow());
				}
			}

			this.fileHandler = new gde.io.FileHandler();
			this.initGUI();

			((Explorer) this.analyzer).setChannels();
			// this.compareSet = new RecordSet(null, GDE.STRING_EMPTY, DataExplorer.COMPARE_RECORD_SET, 1);
			// this.utilitySet = new RecordSet(null, GDE.STRING_EMPTY, DataExplorer.UTILITY_RECORD_SET, 1);

			GDE.shell.setLayout(new FillLayout());
			GDE.shell.setImage(SWTResourceManager.getImage(GDE.IS_MAC ? "gde/resource/DataExplorer_MAC.png" : "gde/resource/DataExplorer.png")); //$NON-NLS-1$ //$NON-NLS-2$
			GDE.shell.setText(GDE.NAME_LONG);

			if (GDE.splash != null) GDE.splash.dispose();
			TaskBar taskBar = GDE.display.getSystemTaskBar();
			if (taskBar == null)
				this.taskBarItem = null;
			else {
				this.taskBarItem = taskBar.getItem(GDE.shell) != null ? taskBar.getItem(GDE.shell) : taskBar.getItem(null);
			}

			StringBuilder sb = new StringBuilder();
			if (this.settings.isDevicePropertiesUpdated()) sb.append(Messages.getString(MessageIds.GDE_MSGI0016)).append(GDE.STRING_NEW_LINE);
			if (this.settings.isGraphicsTemplateUpdated()) sb.append(Messages.getString(MessageIds.GDE_MSGI0017)).append(GDE.STRING_NEW_LINE);
			if (this.settings.isHistoCacheTemplateUpdated() && sb.length() == 0) //
				; // shut up in this case --- sb.append(Messages.getString(MessageIds.GDE_MSGI0068)).append(GDE.STRING_NEW_LINE);
			if (sb.length() > 0) application.openMessageDialog(GDE.shell, sb.toString());

			GDE.shell.addControlListener(new ControlListener() {
				@Override
				public void controlResized(ControlEvent controlevent) {
					if (log.isLoggable(Level.FINEST))
						log.logp(Level.FINEST, $CLASS_NAME, "shell.controlResized", GDE.shell.getLocation().toString() + "event = " + controlevent); //$NON-NLS-1$ //$NON-NLS-2$
					DataExplorer.application.settings.setWindowMaximized(GDE.shell.getMaximized());
					if (!DataExplorer.application.settings.isWindowMaximized()) {
						DataExplorer.application.settings.setWindow(GDE.shell.getLocation(), GDE.shell.getSize());
					}
				}

				@Override
				public void controlMoved(ControlEvent controlevent) {
					if (log.isLoggable(Level.FINEST))
						log.logp(Level.FINEST, $CLASS_NAME, "shell.controlResized", GDE.shell.getLocation().toString() + "event = " + controlevent); //$NON-NLS-1$ //$NON-NLS-2$
					if (!GDE.shell.getMaximized()) DataExplorer.application.settings.setWindow(GDE.shell.getLocation(), GDE.shell.getSize());
				}
			});

			GDE.shell.open();

			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					DataExplorer.this.postInitGUI(inputFilePath);

					for (String errorMessage : GDE.getInitErrors()) {
						MessageBox messageDialog = new MessageBox(GDE.shell, SWT.OK | SWT.ICON_WARNING);
						if (errorMessage.contains(GDE.STRING_SEMICOLON)) {
							String[] messages = errorMessage.split(GDE.STRING_SEMICOLON);
							messageDialog.setText(messages[0]);
							messageDialog.setMessage(messages[1]);
						} else {
							messageDialog.setText(GDE.NAME_LONG);
							messageDialog.setMessage(errorMessage);
						}
						messageDialog.open();
					}
				}
			});

			if (!this.settings.isUpdateChecked()) {
				//use extra thread since GDE.display.asyncExec seam blocking UI
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(10000);
						}
						catch (InterruptedException e) {
							log.log(Level.WARNING, e.getMessage(), e);
						}
						long startTime = new Date().getTime();
						check4update();
						if (log.isLoggable(Level.TIME)) log.log(Level.TIME, "check4update time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
					}
				}, "check4update").start();
			}
			this.enableWritingTmpFiles(this.settings.getUsageWritingTmpFiles());
			log.logp(Level.TIME, DataExplorer.$CLASS_NAME, $METHOD_NAME, "total init time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$

			while (!GDE.shell.isDisposed() && !this.isDisposed()) {
				if (!GDE.display.readAndDispatch()) GDE.display.sleep();
			}
		} catch (Throwable t) {
			log.log(Level.SEVERE, t.getMessage(), t);
			t.printStackTrace(System.err);
		}
		// make writeTmpFile thread
		this.isTmpWriteStop = true;

		// cleanup out dated resources
		FileUtils.cleanupPost();

		this.histoExplorer.ifPresent(HistoExplorer::cleanup);
	}

	/**
	 * init/update logger
	 */
	private void updateLogger() {
		Handler logHandler;
		LogFormatter lf = new LogFormatter();
		Logger rootLogger = Logger.getLogger(GDE.STRING_EMPTY);

		// cleanup previous log handler
		rootLogger.removeHandler(GDE.logHandler);

		if (System.getProperty(GDE.ECLIPSE_STRING) == null) { // running outside eclipse
			try {
				logHandler = new java.util.logging.FileHandler(Settings.getLogFilePath(), 5000000, 3);
				rootLogger.addHandler(logHandler);
				logHandler.setFormatter(lf);
				logHandler.setLevel(Level.ALL);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			logHandler = new ConsoleHandler();
			rootLogger.addHandler(logHandler);
			logHandler.setFormatter(lf);
			logHandler.setLevel(Level.ALL);
		}
		// set logging levels
		this.settings.updateLogLevel();
	}

	/**
	 * Add your post-init code in here
	 */
	private void postInitGUI(final String inputFilePath) {
		final String $METHOD_NAME = "postInitGUI"; //$NON-NLS-1$
		try {
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "init tabs"); //$NON-NLS-1$
			this.statisticsTabItem = new StatisticsWindow(this.displayTab, SWT.NONE);
			this.statisticsTabItem.create();

			// initialization of table, digital, analog and cell voltage are done while initializing the device

			this.fileCommentTabItem = new FileCommentWindow(this.displayTab, SWT.NONE);
			this.fileCommentTabItem.create();

			// createCompareWindowTabItem();

			this.setObjectDescriptionTabVisible(this.menuToolBar.isObjectoriented());

			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "init listener"); //$NON-NLS-1$
			GDE.shell.addListener(SWT.Close, new Listener() {
				@Override
				public void handleEvent(Event evt) {
					if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, GDE.shell.getLocation().toString() + "event = " + evt); //$NON-NLS-1$

					// check all data saved - prevent closing application
					evt.doit = getDeviceSelectionDialog().checkDataSaved();
					
					//check for device initiated running thread like data gathering, skip application shutdown
					for (Thread thread : Thread.getAllStackTraces().keySet()) {
						if (thread != null && !thread.isDaemon() && thread.isAlive() && thread.getClass().getName().startsWith("gde.device")) {
							DataExplorer.this.openMessageDialog(Messages.getString(MessageIds.GDE_MSGW0048));
							evt.doit = false;
						}
					}
				}
			});
			this.addDisposeListener(new DisposeListener() {
				@SuppressWarnings("deprecation")
				@Override
				public void widgetDisposed(DisposeEvent evt) {
					if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, "widgetDisposed", GDE.shell.getLocation().toString() + "event = " + evt); //$NON-NLS-1$ //$NON-NLS-2$
					if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, "widgetDisposed", GDE.shell.getSize().toString()); //$NON-NLS-1$

					//kill long running thread initiated by used device, warning skipped by user
					for (Thread thread : Thread.getAllStackTraces().keySet()) {
						if (thread != null && !thread.isDaemon() && thread.isAlive() && thread.getClass().getName().startsWith("gde.device")) {
							try {
								thread.stop();
							}
							catch (Throwable e) {
								log.log(Level.SEVERE, "thread.getClass().getName() killed brute force while shutdown");
							}
						}
					}
					
					// cleanup if help browser is open, dispose it
					if (DataExplorer.this.helpDialog != null && !DataExplorer.this.helpDialog.isDisposed()) {
						DataExplorer.this.helpDialog.dispose();
					}
					if (DataExplorer.application.getActiveDevice() != null) {
						DataExplorer.application.getActiveDevice().storeDeviceProperties();

						// close open communication ports
						IDeviceCommPort port = DataExplorer.application.getActiveDevice().getCommunicationPort();
						if (port != null) {// if communication port still open, close it
							try {
								if (port.getClass().getName().toLowerCase().contains("usb")) // USB port
									port.closeUsbPort(null);
								else
									port.close(); // serial port
							} catch (Exception e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
							DataExplorer.application.getActiveDevice().storeDeviceProperties();
						}
					}

					if (DataExplorer.application.getDeviceDialog() != null && !DataExplorer.application.getDeviceDialog().isDisposed()) {// if a device tool box
																																																																// is open, dispose it
						DataExplorer.application.getDeviceDialog().forceDispose();
					}

					// query the item definition to save it for restore option
					DataExplorer.this.order = DataExplorer.this.menuCoolBar.getItemOrder();
					DataExplorer.this.wrapIndices = DataExplorer.this.menuCoolBar.getWrapIndices();
					if (DataExplorer.this.wrapIndices.length > 0) {
						if (DataExplorer.this.wrapIndices[0] != 0) {
							int[] newWraps = new int[DataExplorer.this.wrapIndices.length + 1];
							for (int i = 0; i < DataExplorer.this.wrapIndices.length; i++) {
								newWraps[i + 1] = DataExplorer.this.wrapIndices[i];
							}
							DataExplorer.this.wrapIndices = newWraps;
						}
					}
					DataExplorer.this.sizes = DataExplorer.this.menuCoolBar.getItemSizes();
					DataExplorer.application.settings.setCoolBarStates(DataExplorer.this.order, DataExplorer.this.wrapIndices, DataExplorer.this.sizes);

					if (DataExplorer.this.objectDescriptionTabItem != null && !DataExplorer.this.objectDescriptionTabItem.isDisposed()) {
						DataExplorer.this.objectDescriptionTabItem.checkSaveObjectData(); // check if data needs to be saved
					}

					// finally save application settings
					DataExplorer.application.settings.store();
				}
			});
			this.menuCoolBar.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, "menuCoolBar.controlResized, event=" + evt); //$NON-NLS-1$
					// menuCoolBar.controlResized signals collBar item moved
					if (DataExplorer.this.displayTab != null && getSize().y != 0) {
						Point menuCoolBarSize = DataExplorer.this.menuCoolBar.getSize();
						if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, "menuCoolBar.controlResized", "menuCoolBarSize = " + menuCoolBarSize); //$NON-NLS-1$
						Point shellSize = new Point(getClientArea().width, getClientArea().height);
						if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, "menuCoolBar.controlResized", "shellClientSize = " + shellSize); //$NON-NLS-1$
						Point statusBarSize = DataExplorer.this.statusComposite.getSize();
						if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, "menuCoolBar.controlResized", "statusBarSize = " + statusBarSize); //$NON-NLS-1$
						DataExplorer.this.displayTab.setBounds(0, menuCoolBarSize.y, shellSize.x, shellSize.y - menuCoolBarSize.y - statusBarSize.y);
						if (log.isLoggable(Level.FINE))
							log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "displayTab.bounds = " + DataExplorer.this.displayTab.getBounds()); //$NON-NLS-1$
					}
				}
			});
			this.displayTab.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, "displayTab.controlResized, event=" + evt); //$NON-NLS-1$
					if (DataExplorer.this.displayTab != null && getSize().y != 0) {
						DataExplorer.this.menuCoolBar.pack();
						Point menuCoolBarSize = DataExplorer.this.menuCoolBar.getSize();
				    Point shellSize = new Point(GDE.shell.getClientArea().width, GDE.shell.getClientArea().height);
						Point statusBarSize = DataExplorer.this.statusComposite.getSize();
						if (log.isLoggable(Level.FINE))
							log.logp(Level.FINE, $CLASS_NAME, "displayTab.controlRezised", "displayTab.bounds = " + DataExplorer.this.displayTab.getBounds()); //$NON-NLS-1$
				    DataExplorer.this.displayTab.setBounds(0, menuCoolBarSize.y, shellSize.x, shellSize.y - menuCoolBarSize.y - statusBarSize.y);
					}
				}
			});
			this.displayTab.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER) && DataExplorer.this.displayTab.getSelectionIndex() >= 0)
						log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "displayTab.paintControl " + DataExplorer.this.displayTab.getItems()[DataExplorer.this.displayTab.getSelectionIndex()].getText() //$NON-NLS-1$
								+ GDE.STRING_MESSAGE_CONCAT + DataExplorer.this.displayTab.getSelectionIndex() + GDE.STRING_MESSAGE_CONCAT + evt);
					if (isRecordSetVisible(GraphicsType.NORMAL)) {
						if (DataExplorer.this.graphicsTabItem.isCurveSelectorEnabled())
							DataExplorer.this.graphicsTabItem.setSashFormWeights(DataExplorer.this.graphicsTabItem.getCurveSelectorComposite().getSelectorColumnWidth());
						else
							DataExplorer.this.graphicsTabItem.setSashFormWeights(0);
					} else if (isRecordSetVisible(GraphicsType.COMPARE) && DataExplorer.this.compareTabItem != null) {
						DataExplorer.this.compareTabItem.setSashFormWeights(DataExplorer.this.compareTabItem.getCurveSelectorComposite().getSelectorColumnWidth());
					} else if (isRecordSetVisible(GraphicsType.UTIL) && DataExplorer.this.utilGraphicsTabItem != null) {
						DataExplorer.this.utilGraphicsTabItem.setSashFormWeights(DataExplorer.this.utilGraphicsTabItem.getCurveSelectorComposite().getSelectorColumnWidth());
					}
					if (DataExplorer.this.objectDescriptionTabItem != null) {
						if (DataExplorer.this.objectDescriptionTabItem.isVisible()) {
							if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "displayTab.focusGained " + evt); //$NON-NLS-1$
							DataExplorer.this.isObjectWindowVisible = true;
						} else if (DataExplorer.this.isObjectWindowVisible) {
							if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "displayTab.focusLost " + evt); //$NON-NLS-1$
							DataExplorer.this.checkSaveObjectData();
							DataExplorer.this.isObjectWindowVisible = false;
						}
					}
					if (log.isLoggable(Level.FINE) && DataExplorer.this.displayTab != null && DataExplorer.this.menuCoolBar != null && DataExplorer.this.statusComposite != null && getSize().y != 0) {
						log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "menuCoolBar.size = " + DataExplorer.this.menuCoolBar.getSize()); //$NON-NLS-1$
						log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "shellClient.size = " + new Point(getClientArea().width, getClientArea().height)); //$NON-NLS-1$
						log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "statusBar.size = " + DataExplorer.this.statusComposite.getSize()); //$NON-NLS-1$
						log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "displayTab.bounds = " + DataExplorer.this.displayTab.getBounds()); //$NON-NLS-1$
					}
				}
			});
			this.displayTab.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					CTabFolder tabFolder = (CTabFolder) evt.widget;
					int tabPreviousIndex = DataExplorer.this.tabSelectedIndex;
					int tabSelectionIndex = tabFolder.getSelectionIndex();
					DataExplorer.this.tabSelectedIndex = tabFolder.getSelectionIndex();
					if (DataExplorer.this.histoExplorer.isPresent()) {
						try {
							if (log.isLoggable(Level.FINER))
								log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "old=" + tabPreviousIndex + " new=" + tabSelectionIndex + " addSelectionListener, event=" + evt); //$NON-NLS-1$
							if (tabFolder.getItem(tabPreviousIndex) instanceof HistoSummaryWindow) {
								DataExplorer.this.setStatusMessage(GDE.STRING_EMPTY);
							}
						} catch (Exception e) {
							log.log(Level.WARNING, e.getMessage(), e);
						}
					}

					if (tabSelectionIndex == 0) {
						DataExplorer.this.menuToolBar.enableScopePointsCombo(true);
						DataExplorer.this.enableZoomMenuButtons(true);
						DataExplorer.this.updateGraphicsWindow();
					} else if (tabSelectionIndex > 0) {
						if ((DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) && DataExplorer.this.isRecordSetVisible(GraphicsType.COMPARE)) {
							DataExplorer.this.menuToolBar.enableScopePointsCombo(false);
							DataExplorer.this.enableZoomMenuButtons(true);
							DataExplorer.this.updateGraphicsWindow();
						} else if ((DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) && DataExplorer.this.isRecordSetVisible(GraphicsType.UTIL)) {
							DataExplorer.this.menuToolBar.enableScopePointsCombo(false);
							DataExplorer.this.enableZoomMenuButtons(false);
							DataExplorer.this.updateGraphicsWindow();
						} else if ((DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof DataTableWindow)) {
							final Channel activeChannel = Analyzer.getInstance().getActiveChannel();
							final RecordSet activeRecordSet = activeChannel != null ? activeChannel.getActiveRecordSet() : null;
							if (activeRecordSet != null) {
								DataExplorer.this.dataTableTabItem.cleanTable();
								DataExplorer.this.dataTableTabItem.setRowCount(activeRecordSet.getRecordDataSize(false));
							}
						} else {
							DataExplorer.this.histoExplorer.ifPresent(h -> h.updateVisibleTab(evt));
						}
					}
				}
			});
			// drag filePath support
			this.target = new DropTarget(this, DND.DROP_COPY | DND.DROP_DEFAULT);
			this.target.setTransfer(this.types);
			this.target.addDropListener(new DropTargetAdapter() {
				@Override
				public void dragEnter(DropTargetEvent event) {
					if (event.detail == DND.DROP_DEFAULT) {
						if ((event.operations & DND.DROP_COPY) != 0) {
							event.detail = DND.DROP_COPY;
						} else {
							event.detail = DND.DROP_NONE;
						}
					}
					for (TransferData element : event.dataTypes) {
						if (DataExplorer.this.fileTransfer.isSupportedType(element)) {
							event.currentDataType = element;
							if (event.detail != DND.DROP_COPY) {
								event.detail = DND.DROP_NONE;
							}
							break;
						}
					}
				}

				@Override
				public void dragOperationChanged(DropTargetEvent event) {
					if (event.detail == DND.DROP_DEFAULT) {
						if ((event.operations & DND.DROP_COPY) != 0) {
							event.detail = DND.DROP_COPY;
						} else {
							event.detail = DND.DROP_NONE;
						}
					}
					if (DataExplorer.this.fileTransfer.isSupportedType(event.currentDataType)) {
						if (event.detail != DND.DROP_COPY) {
							event.detail = DND.DROP_NONE;
						}
					}
				}

				@Override
				public void drop(DropTargetEvent event) {
					if (DataExplorer.this.fileTransfer.isSupportedType(event.currentDataType)) {
						String[] files = (String[]) event.data;
						for (String filePath : files) {
							if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "dropped file = " + filePath); //$NON-NLS-1$
							if (filePath.toLowerCase().endsWith(GDE.FILE_ENDING_OSD)) {
								String directoryName = ObjectKeyCompliance.getUpcomingObjectKey(Paths.get(filePath));
								if (!directoryName.isEmpty()) ObjectKeyCompliance.createObjectKey(directoryName);
								DataExplorer.this.fileHandler.openOsdFile(filePath);
							} else if (filePath.toLowerCase().endsWith(GDE.FILE_ENDING_LOV)) {
								String directoryName = ObjectKeyCompliance.getUpcomingObjectKey(Paths.get(filePath));
								if (!directoryName.isEmpty()) ObjectKeyCompliance.createObjectKey(directoryName);
								DataExplorer.this.fileHandler.openLovFile(filePath);
							} else {
								application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0022));
							}
						}
					}
				}
			});

			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "init help listener"); //$NON-NLS-1$
			this.menuCoolBar.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "this.helpRequested, event=" + evt); //$NON-NLS-1$
					DataExplorer.application.openHelpDialog(GDE.STRING_EMPTY, "HelpInfo_3.html"); //$NON-NLS-1$
				}
			});
			this.menu.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "this.helpRequested, event=" + evt); //$NON-NLS-1$
					DataExplorer.application.openHelpDialog(GDE.STRING_EMPTY, "HelpInfo_3.html"); //$NON-NLS-1$
				}
			});

			// restore window settings
			this.isRecordCommentVisible = this.settings.isRecordCommentVisible();
			if (this.isRecordCommentVisible) {
				this.menuBar.setRecordCommentMenuItemSelection(this.isRecordCommentVisible);
				this.enableRecordSetComment(this.isRecordCommentVisible);
			}
			this.isGraphicsHeaderVisible = this.settings.isGraphicsHeaderVisible();
			if (this.isGraphicsHeaderVisible) {
				this.menuBar.setGraphicsHeaderMenuItemSelection(this.isGraphicsHeaderVisible);
				this.enableGraphicsHeader(this.isGraphicsHeaderVisible);
			}

			if (!this.settings.isDesktopShortcutCreated()) {
				this.settings.setProperty(Settings.IS_DESKTOP_SHORTCUT_CREATED, GDE.STRING_EMPTY + OperatingSystemHelper.createDesktopLink());
			}

			if (!this.settings.isApplicationRegistered()) {
				this.settings.setProperty(Settings.IS_APPL_REGISTERED, GDE.STRING_EMPTY + OperatingSystemHelper.registerApplication());
			}

			if ((GDE.IS_MAC || GDE.IS_LINUX) && !this.settings.isLockUucpHinted()) {
				if (GDE.IS_MAC && !OperatingSystemHelper.isUucpMember())
					this.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0046));
				else if (GDE.IS_LINUX && !OperatingSystemHelper.isUucpMember()) this.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0045));
				this.settings.setProperty(Settings.IS_LOCK_UUCP_HINTED, "true"); //$NON-NLS-1$
			}

			// wait for possible migration and delay opening for migration
			if (this.settings.isDevicePropertiesUpdated()) {
				this.analyzer.joinDeviceConfigurationsThread(); // todo check if stopping the thread is appropriate
				this.settings.startMigationThread();
				this.settings.joinMigationThread();
				this.analyzer.startDeviceConfigurationsThread();
				this.analyzer.joinDeviceConfigurationsThread();
			}

			// check initial application settings
			if (!this.settings.isOK()) {
				this.openSettingsDialog();
			}

			// check configured device
			this.deviceSelectionDialog = new DeviceSelectionDialog(GDE.shell, SWT.PRIMARY_MODAL, this); //re-initialize to handle updates due to migration
			if (this.settings.getActiveDevice().equals(Settings.EMPTY)) {
				this.deviceSelectionDialog.open();
			} else {
				// channels HashMap will filled with empty records matching the active device, the dummy content is replaced
				this.deviceSelectionDialog.setupDevice();
			}

			if (inputFilePath.length() > 5) {
				if (inputFilePath.endsWith(GDE.FILE_ENDING_OSD))
					this.fileHandler.openOsdFile(inputFilePath);
				else if (inputFilePath.endsWith(GDE.FILE_ENDING_LOV)) this.fileHandler.openLovFile(inputFilePath);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0007) + e.getMessage());
		}
		if (log.isLoggable(Level.TIME)) log.logp(Level.TIME, $CLASS_NAME, $METHOD_NAME, String.format("layout time = %s", StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime)))); //$NON-NLS-1$
		this.setHisto(Settings.getInstance().isHistoActive());
		if (log.isLoggable(Level.TIME)) log.logp(Level.TIME, $CLASS_NAME, $METHOD_NAME, String.format("done time = %s", StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime)))); //$NON-NLS-1$
		GDE.shell.layout(); //fix Ubuntu update problem
		this.updateLogger();
		
		if (DataExplorer.application.getActiveDevice() != null && Settings.getInstance().isStartDeviceCommunicationAfterStartup()) {
			DataExplorer.application.getActiveDevice().open_closeCommPort();
		}
	}

	/**
	 * De-/Activates the history windows.
	 */
	public synchronized void setHisto(boolean isActive) {
		if (isActive) {
			HistoExplorer tmpHistoExplorer = new HistoExplorer(displayTab);
			this.histoExplorer = Optional.of(tmpHistoExplorer);
			if (displayTab != null) { // JUnit
				tmpHistoExplorer.initHisto();
				tmpHistoExplorer.enableCurveSelector(this.isCurveSelectorEnabled);
				tmpHistoExplorer.enableGraphicsHeader(this.isGraphicsHeaderVisible);
				tmpHistoExplorer.enableRecordSetComment(this.isRecordCommentVisible);
			}
		} else {
			this.histoExplorer.ifPresent(h -> h.disposeHisto());
			this.histoExplorer = Optional.empty();
		}
	}

	/**
	 * updates the statistics window using current record set data
	 */
	public void updateStatisticsData() {
		if (this.statisticsTabItem != null && !this.statisticsTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.statisticsTabItem.updateStatisticsData(true);
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.statisticsTabItem.updateStatisticsData(true);
					}
				});
			}
		}
	}

	/**
	 * setup the data table header with current record set data
	 */
	public void setupDataTableHeader() {
		if (this.dataTableTabItem != null && !this.dataTableTabItem.isDisposed()) this.dataTableTabItem.setHeader();
	}

	/**
	 * updates the data table with current record set data
	 * @param requestingRecordSetName name of the active record set
	 * @param forceClean true|false clean the table
	 */
	public synchronized void updateDataTable(String requestingRecordSetName, final boolean forceClean) {
		final Channel activeChannel = this.analyzer.getActiveChannel();
		final RecordSet activeRecordSet = activeChannel != null ? activeChannel.getActiveRecordSet() : null;

		if (activeRecordSet != null && activeRecordSet.getRecordDataSize(true) > 0 && this.dataTableTabItem != null && !this.dataTableTabItem.isDisposed() && activeRecordSet.getName().equals(requestingRecordSetName) && activeRecordSet.getDevice().isTableTabRequested()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				if (forceClean) {
					// DataExplorer.this.dataTableTabItem.setAbsoluteDateTime(false);
					DataExplorer.this.dataTableTabItem.setHeader();
				}
				DataExplorer.this.dataTableTabItem.setRowCount(activeRecordSet.getRecordDataSize(false));
				DataExplorer.this.dataTableTabItem.updateTopIndex();
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						if (forceClean) {
							// DataExplorer.this.dataTableTabItem.setAbsoluteDateTime(false);
							DataExplorer.this.dataTableTabItem.setHeader();
						}
						DataExplorer.this.dataTableTabItem.setRowCount(activeRecordSet.getRecordDataSize(false));
						DataExplorer.this.dataTableTabItem.updateTopIndex();
					}
				});
			}
		} else {
			if (activeRecordSet == null || requestingRecordSetName.equals(GDE.STRING_EMPTY)) {
				if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
					if (this.dataTableTabItem != null) {
						// this.dataTableTabItem.setHeader();
						this.dataTableTabItem.cleanTable();
					}
				} else {
					GDE.display.asyncExec(new Runnable() {

						@Override
						public void run() {
							if (DataExplorer.this.dataTableTabItem != null) {
								// DataExplorer.this.dataTableTabItem.setHeader();
								DataExplorer.this.dataTableTabItem.cleanTable();
							}
						}

					});
				}
			}
		}
	}

	/**
	 * updates the digital window children displays with current record set data
	 */
	public void updateDigitalWindow() {
		if (this.digitalTabItem != null && !this.digitalTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.digitalTabItem.update(true);
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.digitalTabItem.update(true);
					}
				});
			}
		}
	}

	/**
	 * updates the digital window children displays with current record set data
	 */
	public void updateDigitalWindowChilds() {
		if (this.digitalTabItem != null && !this.digitalTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.digitalTabItem.updateChilds();
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.digitalTabItem.updateChilds();
					}
				});
			}
		}
	}

	/**
	 * updates the analog window children displays with current record set data
	 */
	public void updateAnalogWindow() {
		if (this.analogTabItem != null && !this.analogTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.analogTabItem.update(true);
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.analogTabItem.update(true);
					}
				});
			}
		}
	}

	/**
	 * updates the analog window children displays with current record set data
	 */
	public void updateAnalogWindowChilds() {
		if (this.analogTabItem != null && !this.analogTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.analogTabItem.updateChilds();
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.analogTabItem.updateChilds();
					}
				});
			}
		}
	}

	/**
	 * updates the cell voltage window display measurement ordinal
	 * @param measurementOrdinals {firstMeasurementOrdinal, secondMeasurementOrdinal}
	 */
	public void setCellVoltageWindowOrdinal(int[] measurementOrdinals) {
		if (this.cellVoltageTabItem != null) this.cellVoltageTabItem.setMeasurements(measurementOrdinals[0], measurementOrdinals[1]);
	}

	/**
	 * updates the cell voltage window
	 */
	public void updateCellVoltageWindow() {
		if (this.cellVoltageTabItem != null && !this.cellVoltageTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.cellVoltageTabItem.getCellVoltageMainComposite().redraw();
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.cellVoltageTabItem.getCellVoltageMainComposite().redraw();
					}
				});
			}
		}
	}

	/**
	 * updates the cell voltage window children displays according to current record set data
	 */
	public void updateCellVoltageChilds() {
		if (this.cellVoltageTabItem != null && !this.cellVoltageTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.cellVoltageTabItem.updateChilds();
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.cellVoltageTabItem.updateChilds();
					}
				});
			}
		}
	}

	/**
	 * updates the cell voltage limits selector group
	 */
	public void updateCellVoltageLimitsSelector() {
		if (this.cellVoltageTabItem != null && !this.cellVoltageTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.cellVoltageTabItem.updateVoltageLimitsSelection();
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.cellVoltageTabItem.updateVoltageLimitsSelection();
					}
				});
			}
		}
	}

	/**
	 * updates the analog window children displays with current record set data
	 */
	public void updateFileCommentWindow() {
		if (this.fileCommentTabItem != null) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.fileCommentTabItem.update();
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.fileCommentTabItem.update();
					}
				});
			}
		}
	}

	/**
	 * updates the object description window with current object data
	 */
	public void updateObjectDescriptionWindow() {
		if (this.objectDescriptionTabItem != null && !this.objectDescriptionTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.objectDescriptionTabItem.update();
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.objectDescriptionTabItem.update();
					}
				});
			}
		}
	}

	/**
	 * updates the analog window children displays with current record set data
	 */
	public void cleanHeaderAndCommentInGraphicsWindow() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.graphicsTabItem.clearHeaderAndComment();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					DataExplorer.this.graphicsTabItem.clearHeaderAndComment();
				}
			});
		}
	}

	public DeviceSelectionDialog getDeviceSelectionDialog() {
		return this.deviceSelectionDialog;
	}

	public void setStatusMessage(final String message, final int swtColor) {
		if (this.statusBar != null) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.statusBar.setMessage(message, swtColor);
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.statusBar.setMessage(message, swtColor);
					}
				});
			}
		}
	}

	public void setStatusMessage(final String message) {
		if (this.statusBar != null) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.statusBar.setMessage(message);
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.statusBar.setMessage(message);
					}
				});
			}
		}
	}

	/**
	 * set the progress bar percentage, only one process can use the progress bar at one cycle to show its progress - therefore
	 * if a percentage value of 0 or >99 is specified the user will be reset to enable usage of progress bar for another process user
	 * else the progress bar will check user for user equality, not equal will skip processing
	 * @param percentage
	 * @param user
	 */
	public void setProgress(final int percentage, final String user) {
		if (this.statusBar != null) {
			if (this.progressBarUser == null || user == null || this.progressBarUser.equals(user)) {
				if (percentage > 99 | percentage == 0)
					this.progressBarUser = null;
				else
					this.progressBarUser = user;

				if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
					this.statusBar.setProgress(percentage);
					if (this.taskBarItem != null) {
						if (user == null)
							this.taskBarItem.setProgressState(SWT.DEFAULT);
						else
							this.taskBarItem.setProgressState(GDE.IS_MAC ? SWT.PAUSED : SWT.NORMAL);

						this.taskBarItem.setProgress(percentage);
					}
				} else {
					GDE.display.asyncExec(new Runnable() {
						@Override
						public void run() {
							DataExplorer.this.statusBar.setProgress(percentage);
							if (DataExplorer.this.taskBarItem != null) {
								if (user == null)
									DataExplorer.this.taskBarItem.setProgressState(SWT.DEFAULT);
								else
									DataExplorer.this.taskBarItem.setProgressState(GDE.IS_MAC ? SWT.PAUSED : SWT.NORMAL);

								DataExplorer.this.taskBarItem.setProgress(percentage);
							}
						}
					});
				}
				this.progessPercentage = percentage;
				if (percentage >= 100) DataExplorer.this.resetProgressBar();
			}
		}
	}

	private void resetProgressBar() {
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				DataExplorer.this.statusBar.setProgress(0);
				if (DataExplorer.this.taskBarItem != null) {
					DataExplorer.this.taskBarItem.setProgress(0);
				}
			}
		});

	}

	public int getProgressPercentage() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.progessPercentage = this.statusBar.getProgressPercentage();
		} else { // if the percentage is not up to date it will updated later
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					DataExplorer.this.progessPercentage = DataExplorer.this.statusBar.getProgressPercentage();
				}
			});
		}
		return this.progessPercentage == 100 ? 0 : this.progessPercentage;
	}

	final boolean[]	isTxOn	= new boolean[] { true };
	Runnable				txOn		= new Runnable() {
														@Override
														public void run() {
															DataExplorer.this.statusBar.setSerialTxOn();
															isTxOn[0] = true;
														}
													};

	public void setSerialTxOn() {
		if (isTxOn[0]) {
			isTxOn[0] = false;
			GDE.display.asyncExec(txOn);
		}
	}

	final boolean[]	isTxOff	= new boolean[] { true };
	Runnable				txOff		= new Runnable() {
														@Override
														public void run() {
															DataExplorer.this.statusBar.setSerialTxOff();
															isTxOff[0] = true;
														}
													};

	public void setSerialTxOff() {
		if (isTxOff[0]) {
			isTxOff[0] = false;
			GDE.display.asyncExec(txOff);
		}
	}

	final boolean[]	isRxOn	= new boolean[] { true };
	Runnable				rxOn		= new Runnable() {
														@Override
														public void run() {
															DataExplorer.this.statusBar.setSerialRxOn();
															isRxOn[0] = true;
														}
													};

	public void setSerialRxOn() {
		if (isRxOn[0]) {
			isRxOn[0] = false;
			GDE.display.asyncExec(rxOn);
		}
	}

	final boolean[]	isRxOff	= new boolean[] { true };
	Runnable				rxOff		= new Runnable() {
														@Override
														public void run() {
															DataExplorer.this.statusBar.setSerialRxOff();
															isRxOff[0] = true;
														}
													};

	public void setSerialRxOff() {
		if (isRxOff[0]) {
			isRxOff[0] = false;
			GDE.display.asyncExec(rxOff);
		}
	}

	public IDevice getActiveDevice() {
		return this.analyzer.getActiveDevice();
	}

	public void openDeviceDialog() {
		if (DataExplorer.this.getDeviceDialog() != null) {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					DataExplorer.this.getDeviceDialog().open();
				}
			});
		}
	}

	/**
	 * set the active device in main settings
	 * @param device
	 */
	public void setActiveDevice(IDevice device) {
		if (device != null) {
			if (this.getActiveDevice() == null || !this.getActiveDevice().getName().equals(device.getName())) {
				this.analyzer.setActiveDevice(device);
			}
			// do always update, the port might be changed
			this.settings.setActiveDevice(device.getName() + GDE.STRING_SEMICOLON + device.getManufacturer() + GDE.STRING_SEMICOLON + device.getPort());
			this.updateTitleBar(this.getObjectKey(), device.getName(), device.getPort());
			if (this.deviceSelectionDialog.getNumberOfActiveDevices() > 1)
				this.enableDeviceSwitchButtons(true);
			else
				this.enableDeviceSwitchButtons(false);
		}
		else { // no device
			this.settings.setActiveDevice(Settings.EMPTY_SIGNATURE);
			this.updateTitleBar(this.getObjectKey(), Messages.getString(MessageIds.GDE_MSGI0023), GDE.STRING_EMPTY);
			IDevice nullDevice = null;
			this.analyzer.setActiveDevice(nullDevice);
			this.analyzer.getChannels().cleanup();
			this.enableDeviceSwitchButtons(false);
			// remove Histo tabs at this place because setupDevice is not called if all devices are removed
			this.setHisto(false);
		}

		// cleanup device specific utility graphics tab item
		if (this.utilGraphicsTabItem != null) {
			this.utilGraphicsTabItem.dispose();
			this.utilGraphicsTabItem = null;
		}
		// cleanup device specific custom tab item
		for (CTabItem tab : this.customTabItems) {
			tab.dispose();
		}
		this.customTabItems.clear();

	}

	public void updateTitleBar(final String objectName, final String deviceName, final String devicePort) {
		StringBuilder sb = new StringBuilder().append(GDE.NAME_LONG);
		String separator = "  -  "; //$NON-NLS-1$
		String actualFileName = (this.analyzer.getActiveChannel() != null) ? this.analyzer.getActiveChannel().getFileName()
				: null;
		if (actualFileName != null && actualFileName.length() > 4) sb.append(separator).append(actualFileName);
		if (objectName != null && objectName.length() > 0 && !(actualFileName != null && actualFileName.length() > 4 && actualFileName.contains(objectName)) && !objectName.startsWith(Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0]))
			sb.append(separator).append(objectName);
		if (deviceName != null && deviceName.length() > 0) sb.append(separator).append(deviceName);
		if (devicePort != null && devicePort.length() > 0) sb.append(separator).append(devicePort);
		final String headerText = sb.toString();

		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			GDE.shell.setText(headerText);
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					GDE.shell.setText(headerText);
				}
			});
		}
	}

	public void updateTitleBar() {
		final IDevice actualDevice = this.getActiveDevice();
		if (actualDevice != null) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				updateTitleBar(this.getObjectKey(), actualDevice.getName(), actualDevice.getPort());
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						updateTitleBar(DataExplorer.this.getObjectKey(), actualDevice.getName(), actualDevice.getPort());
					}
				});
			}
		}
	}

	public void openMessageDialog(final String message) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			MessageBox messageDialog = new MessageBox(GDE.shell, SWT.OK | SWT.ICON_WARNING);
			messageDialog.setText(GDE.NAME_LONG);
			messageDialog.setMessage(message);
			messageDialog.open();
		} else {
			GDE.display.syncExec(new Runnable() {
				@Override
				public void run() {
					MessageBox messageDialog = new MessageBox(GDE.shell, SWT.OK | SWT.ICON_WARNING);
					messageDialog.setText(GDE.NAME_LONG);
					messageDialog.setMessage(message);
					messageDialog.open();
				}
			});
		}
	}

	public void openMessageDialog(final Shell parent, final String message) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			Shell useParent = (parent != null && !parent.isDisposed()) ? parent : GDE.shell;
			MessageBox messageDialog = new MessageBox(useParent, SWT.OK | SWT.ICON_WARNING);
			messageDialog.setText(GDE.NAME_LONG);
			messageDialog.setMessage(message);
			messageDialog.open();
		} else {
			GDE.display.syncExec(new Runnable() {
				@Override
				public void run() {
					// parent might be disposed ??
					Shell useParent = (parent != null && !parent.isDisposed()) ? parent : GDE.shell;
					MessageBox messageDialog = new MessageBox(useParent, SWT.OK | SWT.ICON_WARNING);
					messageDialog.setText(GDE.NAME_LONG);
					messageDialog.setMessage(message);
					messageDialog.open();
				}
			});
		}
	}

	public void openMessageDialogAsync(final String message) {
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				MessageBox messageDialog = new MessageBox(GDE.shell, SWT.OK | SWT.ICON_WARNING);
				messageDialog.setText(GDE.NAME_LONG);
				messageDialog.setMessage(message);
				messageDialog.open();
			}
		});
	}

	public void openMessageDialogAsync(Shell parent, final String message) {
		final Shell useParent = (parent != null && !parent.isDisposed()) ? parent : GDE.shell;
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				MessageBox messageDialog = new MessageBox(useParent, SWT.OK | SWT.ICON_WARNING | SWT.MODELESS);
				messageDialog.setText(GDE.NAME_LONG);
				messageDialog.setMessage(message);
				messageDialog.open();
			}
		});
	}

	public int openOkCancelMessageDialog(Shell parent, final String message) {
		final Shell useParent = (parent != null && !parent.isDisposed()) ? parent : GDE.shell;
		MessageBox okCancelMessageDialog = new MessageBox(useParent, SWT.PRIMARY_MODAL | SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);
		okCancelMessageDialog.setText(GDE.NAME_LONG);
		okCancelMessageDialog.setMessage(message);
		return okCancelMessageDialog.open();
	}

	public int openOkCancelMessageDialog(final String message) {
		MessageBox okCancelMessageDialog = new MessageBox(this.getShell(), SWT.PRIMARY_MODAL | SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);
		okCancelMessageDialog.setText(GDE.NAME_LONG);
		okCancelMessageDialog.setMessage(message);
		return okCancelMessageDialog.open();
	}

	public int openYesNoMessageDialog(Shell parent, final String message) {
		final Shell useParent = (parent != null && !parent.isDisposed()) ? parent : GDE.shell;
		MessageBox yesNoMessageDialog = new MessageBox(useParent, SWT.PRIMARY_MODAL | SWT.YES | SWT.NO | SWT.ICON_QUESTION);
		yesNoMessageDialog.setText(GDE.NAME_LONG);
		yesNoMessageDialog.setMessage(message);
		return yesNoMessageDialog.open();
	}

	public int openYesNoMessageDialog(final String message) {
		MessageBox yesNoMessageDialog = new MessageBox(GDE.shell, SWT.PRIMARY_MODAL | SWT.YES | SWT.NO | SWT.ICON_QUESTION);
		yesNoMessageDialog.setText(GDE.NAME_LONG);
		yesNoMessageDialog.setMessage(message);
		return yesNoMessageDialog.open();
	}

	public int openYesNoMessageDialogSync(final String message) {
		this.openYesNoMessageDialogAsyncValue = -1;
		GDE.display.syncExec(new Runnable() {
			@Override
			public void run() {
				MessageBox yesNoMessageDialog = new MessageBox(GDE.shell, SWT.PRIMARY_MODAL | SWT.YES | SWT.NO | SWT.ICON_QUESTION);
				yesNoMessageDialog.setText(GDE.NAME_LONG);
				yesNoMessageDialog.setMessage(message);
				DataExplorer.this.openYesNoMessageDialogAsyncValue = yesNoMessageDialog.open();
			}
		});
		int counter = 5000;
		while (this.openYesNoMessageDialogAsyncValue == -1 && counter-- > 0) {
			WaitTimer.delay(100);
		}
		return this.openYesNoMessageDialogAsyncValue;
	}

	public int openYesNoCancelMessageDialog(Shell parent, final String message) {
		final Shell useParent = (parent != null && !parent.isDisposed()) ? parent : GDE.shell;
		MessageBox yesNoCancelMessageDialog = new MessageBox(useParent, SWT.PRIMARY_MODAL | SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_QUESTION);
		yesNoCancelMessageDialog.setText(GDE.NAME_LONG);
		yesNoCancelMessageDialog.setMessage(message);
		return yesNoCancelMessageDialog.open();
	}

	public boolean openUpdateMessageDialogSync() {
		this.openUpdateMessageDialogSyncValue = false;
		GDE.display.syncExec(new Runnable() {
			@Override
			public void run() {
				UpdateMessageBox updateMessageBox = new UpdateMessageBox(GDE.shell, SWT.PRIMARY_MODAL | SWT.BORDER | SWT.TITLE);
				DataExplorer.this.openUpdateMessageDialogSyncValue = updateMessageBox.open();
			}
		});
		int counter = 10000;
		while (this.openUpdateMessageDialogSyncValue == null && counter-- > 0) {
			WaitTimer.delay(100);
		}
		return this.openUpdateMessageDialogSyncValue;
	}


	public void openAboutDialog() {
		new AboutDialog(GDE.shell, SWT.PRIMARY_MODAL).open();
	}

	public DeviceDialog getDeviceDialog() {
		return this.getActiveDevice() != null ? this.getActiveDevice().getDialog() : null;
	}

	public MenuBar getMenuBar() {
		return this.menuBar;
	}

	public void updateSubHistoryMenuItem(String addFilePath) {
		this.menuBar.updateSubHistoryMenuItem(addFilePath);
	}

	public MenuToolBar getMenuToolBar() {
		return this.menuToolBar;
	}

	/**
	 * @return the isObjectoriented
	 */
	public boolean isObjectoriented() {
		return this.menuToolBar != null ? this.menuToolBar.isObjectoriented() : false;
	}

	/**
	 * @return the object key, if device oriented an empty string will be returned
	 */
	public String getObjectKey() {
		return this.menuToolBar != null ? this.menuToolBar.isObjectoriented() ? this.menuToolBar.getActiveObjectKey() : GDE.STRING_EMPTY
				: GDE.STRING_EMPTY;
	}

	/**
	 * @return the object keys, if there are no object key defined return an empty string array
	 */
	public String[] getObjectKeys() {
		return this.settings.getObjectList();
	}

	/**
	 * Set the active channel's object key (does not affect the settings).
	 * {@code newObjectKey} must occur in the object key list.
	 */
	public void selectObjectKey(final String newObjectKey) {
		if (this.settings != null && !this.analyzer.getChannels().getActiveChannel().getObjectKey().equals(newObjectKey)) {
			String[] objectKeys = this.settings.getObjectList();
			for (int searchSelectionIndex = 0; searchSelectionIndex < objectKeys.length; ++searchSelectionIndex) {
				if (newObjectKey.equals(objectKeys[searchSelectionIndex])) {
					log.fine(() -> String.format("channel number %d - actual object key %s", this.analyzer.getChannels().getActiveChannel().getNumber(), this.analyzer.getChannels().getActiveChannel().getObjectKey()));
					if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
						this.menuToolBar.selectObjectKey(searchSelectionIndex);
						this.analyzer.getActiveChannel().setObjectKey(newObjectKey);
					} else {
						final int selectionIndex = searchSelectionIndex;
						GDE.display.syncExec(new Runnable() {
							@Override
							public void run() {
								DataExplorer.this.menuToolBar.selectObjectKey(selectionIndex);
								DataExplorer.this.analyzer.getActiveChannel().setObjectKey(newObjectKey);
							}
						});
					}
					break;
				}
			}
		}
	}

	/**
	 * check if some the object data needs to be saved
	 */
	public void checkSaveObjectData() {
		if (this.objectDescriptionTabItem != null) this.objectDescriptionTabItem.checkSaveObjectData();
	}

	/**
	 * @return the full qualified object file path
	 */
	public String getObjectFilePath() {
		String objectkey = this.menuToolBar.getActiveObjectKey();
		FileUtils.checkDirectoryAndCreate(Settings.getInstance().getDataFilePath() + GDE.STRING_FILE_SEPARATOR_UNIX + objectkey);
		return this.settings.getDataFilePath() + GDE.STRING_FILE_SEPARATOR_UNIX + objectkey + GDE.STRING_FILE_SEPARATOR_UNIX;
	}

	/**
	 * set a new object key list from outside (object key scanner)
	 */
	public void setObjectList(final String[] newObjectKeyList, final String newObjectKey) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.menuToolBar.setObjectList(newObjectKeyList, newObjectKey);
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					DataExplorer.this.menuToolBar.setObjectList(newObjectKeyList, newObjectKey);
				}
			});
		}
	}

	/**
	 * set the object list elements synchronously
	 */
	public void setObjectListElements() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.menuToolBar.setObjectListElements();
		} else {
			GDE.display.syncExec(new Runnable() {
				@Override
				public void run() {
					DataExplorer.this.menuToolBar.setObjectListElements();
				}
			});
		}
	}

	public boolean isObjectSelectorEditable() {
		boolean objectSelectorEditable = true;
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			objectSelectorEditable = DataExplorer.getInstance().getMenuToolBar().isObjectSelectorEditable();
		} else { // if the percentage is not up to date it will updated later
			boolean[] isObjectSelectorEditable = new boolean[] { true };
			GDE.display.syncExec(new Runnable() {
				@Override
				public void run() {
					isObjectSelectorEditable[0] = DataExplorer.getInstance().getMenuToolBar().isObjectSelectorEditable();
				}
			});
			objectSelectorEditable = isObjectSelectorEditable[0];
		}
		return objectSelectorEditable;
	}

	/**
	 * enable / disable the zoom menu buttons
	 * @param enabled
	 */
	public void enableZoomMenuButtons(boolean enabled) {
		this.menuToolBar.enableZoomToolBar(enabled);
		this.menuBar.enableZoomMenuButtons(enabled);
	}

	/**
	 * enable / disable the device switch buttons
	 * @param enabled
	 */
	public void enableDeviceSwitchButtons(boolean enabled) {
		this.menuToolBar.enableDeviceSwitchButtons(enabled);
		this.menuBar.enableDeviceSwitchButtons(enabled);
	}

	/**
	 * enable/disable some menu action (buttons) to avoid exceptions
	 * sample: while loading file content, disable device switch or record set deletion
	 */
	public void enableMenuActions(final boolean enabled) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.menuToolBar.enableDeviceSwitchButtons(enabled);
			this.menuBar.enableDeviceSwitchButtons(enabled);

			this.menuToolBar.enableChannelActions(enabled);
			this.menuToolBar.enableRecordSetActions(enabled);
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					DataExplorer.this.menuToolBar.enableDeviceSwitchButtons(enabled);
					DataExplorer.this.menuBar.enableDeviceSwitchButtons(enabled);

					DataExplorer.this.menuToolBar.enableChannelActions(enabled);
					DataExplorer.this.menuToolBar.enableRecordSetActions(enabled);
				}
			});
		}
	}

	/**
	 * method to update the channel selector of the data tool bar
	 * @param activeChannel to be set
	 */
	public void updateChannelSelector(int activeChannel) {
		String[] channelNames = new String[this.analyzer.getChannels().size()];
		for (int i = 0; i < channelNames.length; i++) {
			channelNames[i] = this.analyzer.getChannels().get(i + 1).getName();
		}
		CCombo channelSelect = this.menuToolBar.getChannelSelectCombo();
		channelSelect.setItems(channelNames); // "K1: Kanal 1", "K2: Kanal 2", "K3: Kanal 3", "K4: Kanal 4"
		channelSelect.select(activeChannel); // kanalCombo.setText("K1: Kanal 1");
	}

	/**
	 * method to update the channel selector of the data tool bar
	 * @param activeRecord to be set
	 */
	public void updateRecordSelector(int activeRecord) {
		Channel activeChannel = this.analyzer.getActiveChannel();
		if (activeChannel != null) {
			String[] recordNames = activeChannel.getRecordSetNames();
			if (recordNames != null && recordNames.length > 0 && recordNames[0] != null) {
				CCombo recordSelect = this.menuToolBar.getRecordSelectCombo();
				recordSelect.setItems(recordNames); // 1) Datensatz
				recordSelect.select(activeRecord); // kanalCombo.setText("K1: Kanal 1");
			}
		}
	}

	/**
	 * update menu items and tool bar items according GPS data availability
	 */
	public void updateMenusRegardingGPSData() {
		this.menuBar.updateAdditionalGPSMenuItems();
		this.menuToolBar.updateGoogleEarthToolItem();
	}

	public FileDialog prepareFileOpenDialog(String name, String[] extensions, String path, String fileName, int addStyle) {
		final String $METHOD_NAME = "fileOpenDialogPath"; //$NON-NLS-1$
		FileDialog fileOpenDialog = new FileDialog(GDE.shell, SWT.PRIMARY_MODAL | SWT.OPEN | addStyle);
		if (path != null) {
			path = path.replace(GDE.STRING_FILE_SEPARATOR_UNIX, GDE.FILE_SEPARATOR);
			path = !path.endsWith(GDE.FILE_SEPARATOR) ? path + GDE.FILE_SEPARATOR : path;
		}
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "dialogName = " + name + " path = " + path); //$NON-NLS-1$ //$NON-NLS-2$
		fileOpenDialog.setText(name);
		fileOpenDialog.setFileName(fileName == null ? GDE.STRING_EMPTY : fileName);
		if (extensions != null) {
			adaptFilter(fileOpenDialog, extensions);
		}
		if (path != null) fileOpenDialog.setFilterPath(path);
		return fileOpenDialog;
	}

	public FileDialog openFileOpenDialog(String name, String[] extensions, String path, String fileName, int addStyle) {
		final String $METHOD_NAME = "openFileOpenDialog"; //$NON-NLS-1$
		FileDialog fileOpenDialog = new FileDialog(GDE.shell, SWT.PRIMARY_MODAL | SWT.OPEN | addStyle);
		if (path != null) {
			path = path.replace(GDE.STRING_FILE_SEPARATOR_UNIX, GDE.FILE_SEPARATOR);
			path = !path.endsWith(GDE.FILE_SEPARATOR) ? path + GDE.FILE_SEPARATOR : path;
		}
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "dialogName = " + name + " path = " + path); //$NON-NLS-1$ //$NON-NLS-2$
		fileOpenDialog.setText(name);
		fileOpenDialog.setFileName(fileName == null ? GDE.STRING_EMPTY : fileName);
		if (extensions != null) {
			adaptFilter(fileOpenDialog, extensions);
		}
		if (path != null) fileOpenDialog.setFilterPath(path);
		fileOpenDialog.open();
		return fileOpenDialog;
	}

	/**
	 * adapt extensions list to upper and lower case if OS distinguish
	 * @param fileOpenDialog
	 * @param extensions
	 */
	private void adaptFilter(FileDialog fileOpenDialog, String[] extensions) {
		if (!GDE.IS_WINDOWS) { // Apples MAC OS seams to reply with case insensitive file names
			Vector<String> tmpExt = new Vector<String>();
			for (String extension : extensions) {
				if (!extension.equals(GDE.FILE_ENDING_STAR_STAR)) {
					tmpExt.add(extension); // lower case is default
					tmpExt.add(extension.toUpperCase());
				} else
					tmpExt.add(GDE.FILE_ENDING_STAR);
			}
			extensions = tmpExt.toArray(new String[1]);
		}
		fileOpenDialog.setFilterExtensions(extensions);
		fileOpenDialog.setFilterNames(getExtensionDescription(extensions));
	}

	public FileDialog openFileOpenDialog(Shell parent, String name, String[] extensions, String path, String fileName, int addStyle) {
		final String $METHOD_NAME = "openFileOpenDialog"; //$NON-NLS-1$
		FileDialog fileOpenDialog = new FileDialog(parent, SWT.PRIMARY_MODAL | SWT.OPEN | addStyle);
		if (path != null) {
			path = path.replace(GDE.STRING_FILE_SEPARATOR_UNIX, GDE.FILE_SEPARATOR);
			path = !path.endsWith(GDE.FILE_SEPARATOR) ? path + GDE.FILE_SEPARATOR : path;
		}
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "dialogName = " + name + " path = " + path); //$NON-NLS-1$ //$NON-NLS-2$
		fileOpenDialog.setText(name);
		fileOpenDialog.setFileName(fileName == null ? GDE.STRING_EMPTY : fileName);
		if (extensions != null) {
			adaptFilter(fileOpenDialog, extensions);
		}
		if (path != null) fileOpenDialog.setFilterPath(path);
		fileOpenDialog.open();
		return fileOpenDialog;
	}

	public FileDialog prepareFileSaveDialog(String name, String[] extensions, String path, String fileName) {
		final String $METHOD_NAME = "openFileSaveDialog"; //$NON-NLS-1$
		FileDialog fileSaveDialog = new FileDialog(GDE.shell, SWT.PRIMARY_MODAL | SWT.SAVE);
		if (path != null) {
			path = path.replace(GDE.STRING_FILE_SEPARATOR_UNIX, GDE.FILE_SEPARATOR);
			path = !path.endsWith(GDE.FILE_SEPARATOR) ? path + GDE.FILE_SEPARATOR : path;
		}
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "dialogName = " + name + " path = " + path); //$NON-NLS-1$ //$NON-NLS-2$
		fileSaveDialog.setText(name);
		if (extensions != null) {
			adaptFilter(fileSaveDialog, extensions);
		}
		if (path != null) fileSaveDialog.setFilterPath(path);
		fileSaveDialog.setFileName(fileName != null && fileName.length() > 5 ? fileName : GDE.STRING_EMPTY);
		return fileSaveDialog;
	}

	public FileDialog prepareFileSaveDialog(Shell parent, String name, String[] extensions, String path, String fileName) {
		final String $METHOD_NAME = "openFileSaveDialog"; //$NON-NLS-1$
		FileDialog fileSaveDialog = new FileDialog(parent, SWT.PRIMARY_MODAL | SWT.SAVE | SWT.ON_TOP);
		if (path != null) {
			path = path.replace(GDE.STRING_FILE_SEPARATOR_UNIX, GDE.FILE_SEPARATOR);
			path = !path.endsWith(GDE.FILE_SEPARATOR) ? path + GDE.FILE_SEPARATOR : path;
		}
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "dialogName = " + name + " path = " + path); //$NON-NLS-1$ //$NON-NLS-2$
		fileSaveDialog.setText(name);
		if (extensions != null) {
			adaptFilter(fileSaveDialog, extensions);
		}
		if (path != null) fileSaveDialog.setFilterPath(path);
		fileSaveDialog.setFileName(fileName != null && fileName.length() > 5 ? fileName : GDE.STRING_EMPTY);
		return fileSaveDialog;
	}

	/**
	 * @param extensions
	 * @return the mapped extension description *.osd -> DataExplorer files
	 */
	public String[] getExtensionDescription(String[] extensions) {
		String[] filterNames = new String[extensions.length];
		for (int i = 0; i < filterNames.length; i++) {
			int beginIndex = extensions[i].indexOf(GDE.CHAR_DOT);
			String tmpExt = (beginIndex != -1 ? extensions[i].substring(beginIndex + 1) : extensions[i]);
			filterNames[i] = this.extensionFilterMap.get(tmpExt.toLowerCase());

			if (filterNames[i] == null)
				filterNames[i] = extensions[i];
			else {
				beginIndex = filterNames[i].indexOf(GDE.CHAR_DOT);
				if (beginIndex > 0) { // replace extension case
					String tmpFilterExt = filterNames[i].substring(filterNames[i].indexOf(GDE.CHAR_DOT) + 1, filterNames[i].length() - 1);
					filterNames[i] = tmpExt.equals(tmpFilterExt) ? filterNames[i] : filterNames[i].replace(tmpFilterExt, tmpExt);
				}
			}
		}
		return filterNames;
	}

	public String openDirFileDialog(String name, String path) {
		final String $METHOD_NAME = "openDirFileDialog"; //$NON-NLS-1$
		DirectoryDialog fileDirDialog = new DirectoryDialog(GDE.shell, SWT.PRIMARY_MODAL | SWT.NONE);
		if (path != null) {
			path = path.replace(GDE.STRING_FILE_SEPARATOR_UNIX, GDE.FILE_SEPARATOR);
			path = !path.endsWith(GDE.FILE_SEPARATOR) ? path + GDE.FILE_SEPARATOR : path;
		}
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "dialogName = " + name + " path = " + path); //$NON-NLS-1$ //$NON-NLS-2$
		fileDirDialog.setText(name);
		if (path != null) fileDirDialog.setFilterPath(path);
		return fileDirDialog.open();
	}

	public RGB openColorDialog() {
		ColorDialog colorDialog = new ColorDialog(GDE.shell);
		colorDialog.setText(this.getClass().getSimpleName() + Messages.getString(MessageIds.GDE_MSGT0145));
		return colorDialog.open();
	}

	public int openFontSizeDialog() {
		FontSizeDialog fontSizeDialog = new FontSizeDialog(GDE.shell, SWT.NORMAL);
		fontSizeDialog.setText(this.getClass().getSimpleName() + Messages.getString(MessageIds.GDE_MSGT0145));
		return fontSizeDialog.open(new String[] { "20", "25", "30", "35", "40", "45", "50", "55", "60", "65", "70" }, 6);
	}

	/**
	 * update all visualization windows
	 */
	public void updateAllTabs(final boolean force) {
		updateAllTabs(force, true);
	}

	public void updateAllTabs(final boolean force, final boolean redrawCurveSelector) {
		// log.log(Level.OFF, "updateAllTabs entry " + this.isUpdateAllTabs[0]);
		if (this.isUpdateAllTabs) {
			this.isUpdateAllTabs = false;
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.doUpdateAllTabs(force, redrawCurveSelector);
			} else {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.doUpdateAllTabs(force, redrawCurveSelector);
					}
				});
			}
		}
	}

	/**
	 * update all visualization windows
	 */
	private void doUpdateAllTabs(final boolean force, final boolean redrawCurveSelector) {
		this.updateGraphicsWindow(redrawCurveSelector);
		this.updateStatisticsData();
		if (force) {
			this.updateDigitalWindow();
			this.updateAnalogWindow();
		} else {
			this.updateDigitalWindowChilds();
			this.updateAnalogWindowChilds();
		}
		this.updateCellVoltageWindow();
		this.updateFileCommentWindow();
		if (this.getActiveRecordSet() != null) {
			this.updateDataTable(this.getActiveRecordSet().getName(), force);
		} else {
			DataExplorer.this.updateDataTable(GDE.STRING_EMPTY, force);
		}
		this.isUpdateAllTabs = true;
	}

	/**
	 * update the graphicsWindow
	 */
	public void updateGraphicsWindow() {
		if (this.graphicsTabItem != null) // testing without UI
			updateGraphicsWindow(true);
	}

	/**
	 * update the graphicsWindow
	 */
	public void updateGraphicsWindow(final boolean refreshCurveSelector) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			if (!this.graphicsTabItem.isActiveCurveSelectorContextMenu()) {
				int tabSelectionIndex = this.displayTab.getSelectionIndex();
				if (tabSelectionIndex == 0) { // graphics tab is always the first one
					this.graphicsTabItem.redrawGraphics(refreshCurveSelector);
				} else if (tabSelectionIndex > 0) {
					if ((DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) && DataExplorer.this.isRecordSetVisible(GraphicsType.COMPARE)) {
						this.compareTabItem.redrawGraphics(refreshCurveSelector);
					} else if ((DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) && DataExplorer.this.isRecordSetVisible(GraphicsType.UTIL)) {
						this.utilGraphicsTabItem.redrawGraphics(refreshCurveSelector);
					} else {
						this.histoExplorer.ifPresent(h -> h.updateGraphicsWindow(refreshCurveSelector));
					}
				}
			}
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!DataExplorer.this.graphicsTabItem.isActiveCurveSelectorContextMenu()) {
						int tabSelectionIndex = DataExplorer.this.displayTab.getSelectionIndex();
						if (tabSelectionIndex == 0) {
							DataExplorer.this.graphicsTabItem.redrawGraphics(refreshCurveSelector);
						} else if ((DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) && DataExplorer.this.isRecordSetVisible(GraphicsType.COMPARE)) {
							DataExplorer.this.compareTabItem.redrawGraphics(refreshCurveSelector);
						} else if ((DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) && DataExplorer.this.isRecordSetVisible(GraphicsType.UTIL)) {
							DataExplorer.this.utilGraphicsTabItem.redrawGraphics(refreshCurveSelector);
						} else {
							DataExplorer.this.histoExplorer.ifPresent(h -> h.updateGraphicsWindow(refreshCurveSelector));
						}
					}
				}
			});
		}
	}

	/**
	 * update graphics captions (header and description)
	 */
	public void updateGraphicsCaptions() {
		if (this.graphicsTabItem != null) this.graphicsTabItem.updateCaptions();
	}

	/**
	 * update the graphics window curve selector table only (after calculating values to make records displayable)
	 */
	public void updateCurveSelectorTable() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.graphicsTabItem.updateCurveSelectorTable();
			if (this.compareTabItem != null && !this.compareTabItem.isDisposed()) this.compareTabItem.updateCurveSelectorTable();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					DataExplorer.this.graphicsTabItem.updateCurveSelectorTable();
					if (DataExplorer.this.compareTabItem != null && !DataExplorer.this.compareTabItem.isDisposed())
						DataExplorer.this.compareTabItem.updateCurveSelectorTable();
				}
			});
		}
	}

	/**
	 * method to make record comment window visible
	 */
	@Deprecated // ET seems to be replaced by enableRecordSetComment(boolean) in the meanwhile
	public void setRecordCommentEnabled(boolean enabled) {
		this.settings.setRecordCommentVisible(enabled);
		this.isRecordCommentVisible = enabled;
	}

	/**
	 * set the curve selector visible
	 */
	public void enableCurveSelector(boolean value) {
		this.graphicsTabItem.setCurveSelectorEnabled(value);
		this.histoExplorer.ifPresent(h -> h.enableCurveSelector(value));
		this.isCurveSelectorEnabled = value;
	}

	/**
	 * set the graphics window sashForm weights
	 */
	public void setGraphicsSashFormWeights(int newSelectorCopositeWidth, GraphicsType graphicsType) {
		switch (graphicsType) {
		case COMPARE:
			this.compareTabItem.setSashFormWeights(newSelectorCopositeWidth);
			break;

		case UTIL:
			this.utilGraphicsTabItem.setSashFormWeights(newSelectorCopositeWidth);
			break;

		default:
			this.graphicsTabItem.setSashFormWeights(newSelectorCopositeWidth);
			break;
		}
	}

	/**
	 * opens the settingsDialog
	 */
	public void openSettingsDialog() {
		this.settingsDialog = new SettingsDialog(GDE.shell, SWT.PRIMARY_MODAL);
		this.settingsDialog.open();
	}

	public void updateDisplayTab() {
		this.displayTab.redraw();
	}

	public void updateCompareWindow() {
			this.compareTabItem.redrawGraphics(true);
	}

	public boolean isWithCompareSet() {
		return this.compareSet != null && !this.compareSet.isEmpty();
	}

	public boolean isWithUtilitySet() {
		return this.utilitySet != null && !this.utilitySet.isEmpty();
	}

	public RecordSet getCompareSet() {
		return this.compareSet == null ? this.compareSet = new RecordSet(analyzer, GDE.STRING_EMPTY, DataExplorer.COMPARE_RECORD_SET, 1, GraphicsType.COMPARE)
				: this.compareSet;
	}

	public RecordSet getUtilitySet() {
		return this.utilitySet == null ? this.utilitySet = new RecordSet(analyzer, GDE.STRING_EMPTY, DataExplorer.UTILITY_RECORD_SET, 1, GraphicsType.UTIL)
				: this.utilitySet;
	}

	public GraphicsWindow getUtilGraphicsWindow(String tabName) {
		return this.setUtilGraphicsWindowVisible(true, tabName);
	}

	/**
	 * check the given graphics window type is visible to decide which record set to be used.
	 * Does not support histo windows.
	 * @param type (GraphicsWindow.TYPE_NORMAL/GraphicsWindow.TYPE_COMPARE)
	 * @return true if the the record set in it window is visible
	 */
	public boolean isRecordSetVisible(GraphicsType type) {
		boolean result = false;
		switch (type) {
		case COMPARE:
			result = this.compareTabItem != null && !this.compareTabItem.isDisposed() && this.compareTabItem.isVisible();
			break;

		case UTIL:
			result = this.utilGraphicsTabItem != null && !this.utilGraphicsTabItem.isDisposed() && this.utilGraphicsTabItem.isVisible();
			break;

		case NORMAL:
		default:
			result = this.graphicsTabItem.isVisible();
			break;
		}
		return result;
	}

	/**
	 * @return the associated recordset - might be a compare / util / histo recordset
	 */
	@Nullable
	public AbstractRecordSet getRecordSetOfVisibleTab() {
		if (this.isRecordSetVisible(GraphicsType.NORMAL))
			return this.getActiveRecordSet();
		else if (this.isRecordSetVisible(GraphicsType.COMPARE))
			return this.compareSet;
		else if (this.isRecordSetVisible(GraphicsType.UTIL))
			return this.utilitySet;
		else if (this.histoExplorer.map(h -> h.isHistoChartWindowVisible()).orElse(false))
			return this.getPresentHistoExplorer().getHistoSet().getTrailRecordSet();

		return this.getActiveRecordSet();
	}

	/**
	 * reset the graphicsWindow zoom mode and measurement pointer
	 */
	public void resetGraphicsWindowZoomAndMeasurement() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.graphicsTabItem.setModeState(GraphicsMode.RESET);
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					DataExplorer.this.graphicsTabItem.setModeState(GraphicsMode.RESET);
				}
			});
		}
	}

	/**
	 * set the graphics window mode to the just visible window
	 * @param graphicsMode (GraphicsWindow.MODE_ZOOM, GraphicsWindow.MODE_PAN)
	 * @param enabled
	 */
	public void setGraphicsMode(GraphicsMode graphicsMode, boolean enabled) {
		final String $METHOD_NAME = "setGraphicsMode"; //$NON-NLS-1$
		if (isRecordSetVisible(GraphicsType.NORMAL)) {
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "graphicsWindow.getGraphicCanvas().isVisible() == true"); //$NON-NLS-1$
			setGraphicsWindowMode(graphicsMode, enabled);
		} else if (isRecordSetVisible(GraphicsType.COMPARE) && graphicsMode != GraphicsMode.SCOPE) {
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "compareWindow.getGraphicCanvas().isVisible() == true"); //$NON-NLS-1$
			setCompareWindowMode(graphicsMode, enabled);
		} else if (isRecordSetVisible(GraphicsType.UTIL)) {
			if (log.isLoggable(Level.FINE))
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "utilityWindow.getGraphicCanvas().isVisible() == true, it does not have a supported graphics mode"); //$NON-NLS-1$
		}
	}

	/**
	 * set the graphics window mode to the graphics window
	 * @param graphicsMode
	 * @param enabled
	 */
	public void setGraphicsWindowMode(GraphicsMode graphicsMode, boolean enabled) {
		RecordSet recordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
		if (recordSet != null) {
			switch (graphicsMode) {
			case ZOOM:
				recordSet.resetMeasurement();
				recordSet.setZoomMode(enabled);
				this.graphicsTabItem.setModeState(graphicsMode);
				break;
			case SCOPE:
				recordSet.resetZoomAndMeasurement();
				recordSet.setScopeSizeRecordPoints(this.getMenuToolBar().getScopeModeLevelValue());
				this.graphicsTabItem.setModeState(graphicsMode);
				this.updateGraphicsWindow();
				break;
			case PAN:
				if (!recordSet.isPanMode())
					this.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0007));
				else {
					recordSet.resetMeasurement();
					this.graphicsTabItem.setModeState(graphicsMode);
				}
				break;
			default:
			case RESET:
				recordSet.resetZoomAndMeasurement();
				this.graphicsTabItem.setModeState(graphicsMode);
 				this.updateGraphicsWindow();
			}
		}
	}

	/**
	 * set the graphics window mode to the compare window
	 * @param graphicsMode
	 * @param enabled
	 */
	public void setCompareWindowMode(GraphicsMode graphicsMode, boolean enabled) {
		if (DataExplorer.application.isWithCompareSet()) {
			RecordSet recordSet = DataExplorer.application.getCompareSet();
			switch (graphicsMode) {
			case ZOOM:
				recordSet.resetMeasurement();
				recordSet.setZoomMode(enabled);
				if (this.compareTabItem != null && !this.compareTabItem.isDisposed()) this.compareTabItem.setModeState(graphicsMode);
				break;
			case PAN:
				if (!recordSet.isPanMode())
					this.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0007));
				else {
					recordSet.resetMeasurement();
					if (this.compareTabItem != null && !this.compareTabItem.isDisposed()) this.compareTabItem.setModeState(graphicsMode);
				}
				break;
			default:
			case RESET:
				recordSet.resetZoomAndMeasurement();
				if (this.compareTabItem != null && !this.compareTabItem.isDisposed()) this.compareTabItem.setModeState(graphicsMode);
				this.updateGraphicsWindow();
			}
		}
	}

	/**
	 * clear measurement pointer of visible tab window.
	 */
	public void clearMeasurementModes() {
		boolean isGraphicsTypeNormal = isRecordSetVisible(GraphicsType.NORMAL);
		RecordSet recordSet = isGraphicsTypeNormal ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : this.compareSet;
		if (recordSet != null) {
			if (isGraphicsTypeNormal) {
				recordSet.clearMeasurementModes();
				this.graphicsTabItem.getGraphicsComposite().cleanMeasurementPointer();
			} else if (this.compareTabItem != null && !this.compareTabItem.isDisposed() && this.isWithCompareSet()) {
				recordSet = DataExplorer.application.getCompareSet();
				recordSet.clearMeasurementModes();
				this.compareTabItem.getGraphicsComposite().cleanMeasurementPointer();
			}
		}
	}

	/**
	 * switch application into measurement mode for the visible record set using selected record
	 * @param recordKey
	 * @param enabled
	 */
	public void setMeasurementActive(String recordKey, boolean enabled) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordKey);
		boolean isGraphicsTypeNormal = isRecordSetVisible(GraphicsType.NORMAL);
		RecordSet recordSet = isGraphicsTypeNormal ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : this.compareSet;
		if (recordSet != null && recordSet.containsKey(recordKey)) {
			if (isGraphicsTypeNormal) {
				recordSet.setMeasurementMode(recordKey, enabled);
				this.graphicsTabItem.getGraphicsComposite().cleanMeasurementPointer();
				if (enabled)
					this.graphicsTabItem.getGraphicsComposite().drawMeasurePointer(null, recordSet, GraphicsMode.MEASURE, false);
			} else if (this.compareTabItem != null && !this.compareTabItem.isDisposed()) {
				recordSet = DataExplorer.application.getCompareSet();
				if (recordSet != null && recordSet.containsKey(recordKey)) {
					recordSet.setMeasurementMode(recordKey, enabled);
					if (enabled)
						this.compareTabItem.getGraphicsComposite().drawMeasurePointer(null, recordSet, GraphicsMode.MEASURE, false);
					else
						this.compareTabItem.getGraphicsComposite().cleanMeasurementPointer();
				}
			}
		}
	}

	/**
	 * switch application into delta measurement mode for visible record set using selected record
	 * @param recordKey
	 * @param enabled
	 */
	public void setDeltaMeasurementActive(String recordKey, boolean enabled) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordKey);
		boolean isGraphicsTypeNormal = isRecordSetVisible(GraphicsType.NORMAL);
		RecordSet recordSet = isGraphicsTypeNormal ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : this.compareSet;
		if (recordSet != null && recordSet.containsKey(recordKey)) {
			if (isGraphicsTypeNormal) {
				recordSet.setDeltaMeasurementMode(recordKey, enabled);
				this.graphicsTabItem.getGraphicsComposite().cleanMeasurementPointer();
				if (enabled)
					this.graphicsTabItem.getGraphicsComposite().drawMeasurePointer(null, recordSet, GraphicsMode.MEASURE_DELTA, false);
			} else if (this.compareTabItem != null && !this.compareTabItem.isDisposed()) {
				recordSet = DataExplorer.application.getCompareSet();
				if (recordSet != null && recordSet.containsKey(recordKey)) {
					recordSet.setDeltaMeasurementMode(recordKey, enabled);
					if (enabled)
						this.compareTabItem.getGraphicsComposite().drawMeasurePointer(null, recordSet, GraphicsMode.MEASURE_DELTA, false);
					else
						this.compareTabItem.getGraphicsComposite().cleanMeasurementPointer();
				}
			}
		}
	}

	/**
	 * switch application into delta measurement mode for visible record set using selected record
	 * @param recordKey
	 * @param enabled
	 */
	public void setAvgMedianMeasurementActive(String recordKey, boolean enabled) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordKey);
		boolean isGraphicsTypeNormal = isRecordSetVisible(GraphicsType.NORMAL);
		RecordSet recordSet = isGraphicsTypeNormal ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : this.compareSet;
		if (recordSet != null && recordSet.containsKey(recordKey)) {
			if (isGraphicsTypeNormal) {
				recordSet.setAvgMedianMeasurementMode(recordKey, enabled);
				this.graphicsTabItem.getGraphicsComposite().cleanMeasurementPointer();
				if (enabled)
					this.graphicsTabItem.getGraphicsComposite().drawMeasurePointer(null, recordSet, GraphicsMode.MEASURE_DELTA, false);
			} else if (this.compareTabItem != null && !this.compareTabItem.isDisposed()) {
				recordSet = DataExplorer.application.getCompareSet();
				if (recordSet != null && recordSet.containsKey(recordKey)) {
					recordSet.setAvgMedianMeasurementMode(recordKey, enabled);
					if (enabled)
						this.compareTabItem.getGraphicsComposite().drawMeasurePointer(null, recordSet, GraphicsMode.MEASURE_DELTA, false);
					else
						this.compareTabItem.getGraphicsComposite().cleanMeasurementPointer();
				}
			}
		}
	}

	/**
	 * switch application graphics window into cut mode
	 * @param leftEnabled
	 * @param rightEnabled
	 */
	public void setCutModeActive(boolean leftEnabled, boolean rightEnabled) {
		if (leftEnabled || rightEnabled)
			if (leftEnabled)
				this.graphicsTabItem.getGraphicsComposite().drawCutPointer(null, GraphicsMode.CUT_LEFT, leftEnabled, rightEnabled);
			else if (rightEnabled)
				this.graphicsTabItem.getGraphicsComposite().drawCutPointer(null, GraphicsMode.CUT_RIGHT, leftEnabled, rightEnabled);
			else
				this.graphicsTabItem.getGraphicsComposite().drawCutPointer(null, GraphicsMode.RESET, false, false);
		else
			this.graphicsTabItem.getGraphicsComposite().drawCutPointer(null, GraphicsMode.RESET, false, false);
	}

	@Override
	public void setCursor(final Cursor newCursor) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			DataExplorer.application.getParent().setCursor(newCursor);
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					DataExplorer.application.getParent().setCursor(newCursor);
				}
			});
		}
	}

	public long getThreadId() {
		return this.threadId;
	}

	public void setPortConnected(final boolean isOpenStatus) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.menuBar.setPortConnected(isOpenStatus);
			this.menuToolBar.setPortConnected(isOpenStatus);
			if (this.statusBar != null) {
				if (isOpenStatus) {
					this.statusBar.setSerialPortConnected();
				} else {
					this.statusBar.setSerialPortDisconnected();
					this.statusBar.setSerialRxOff();
					this.statusBar.setSerialTxOff();
				}
			}
		} else {
			if (DataExplorer.this.statusBar != null) {
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						DataExplorer.this.menuBar.setPortConnected(isOpenStatus);
						DataExplorer.this.menuToolBar.setPortConnected(isOpenStatus);
						if (isOpenStatus) {
							DataExplorer.this.statusBar.setSerialPortConnected();
						} else {
							DataExplorer.this.statusBar.setSerialPortDisconnected();
							DataExplorer.this.statusBar.setSerialRxOff();
							DataExplorer.this.statusBar.setSerialTxOff();
						}
					}
				});
			}
		}
	}

	/**
	 * open the dialog and displays content of given HTML file
	 * @param deviceName
	 * @param fileName the help HTML file
	 */
	public void openHelpDialog(String deviceName, String fileName) {
		final String $METHOD_NAME = "openHelpDialog"; //$NON-NLS-1$
		try {
			if (this.helpDialog == null || this.helpDialog.isDisposed()) {
				this.helpDialog = new HelpInfoDialog(GDE.shell, SWT.NONE);
			}
			this.helpDialog.open(deviceName, fileName, SWT.NONE, false);
		} catch (Error e) {
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "using OS registered web browser"); //$NON-NLS-1$
			WebBrowser.openURL(deviceName, fileName);
			application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI0025));
		} catch (Throwable t) {
			application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0007) + t.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + t.getMessage());
		}
	}

	/**
	 * open the dialog and displays content of given HTML file
	 * @param deviceName
	 * @param fileName the help HTML file
	 */
	public void openHelpDialog(String deviceName, String fileName, boolean extractBase) {
		final String $METHOD_NAME = "openHelpDialog"; //$NON-NLS-1$
		try {
			if (this.helpDialog == null || this.helpDialog.isDisposed()) {
				this.helpDialog = new HelpInfoDialog(GDE.shell, SWT.NONE);
			}
			this.helpDialog.open(deviceName, fileName, SWT.NONE, extractBase);
		} catch (Error e) {
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "using OS registered web browser"); //$NON-NLS-1$
			WebBrowser.openURL(deviceName, fileName);
			application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI0025));
		} catch (Throwable t) {
			application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0007) + t.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + t.getMessage());
		}
	}

	/**
	 * open the dialog and displays content of given HTML file
	 * @param stringURL of the help HTML file
	 */
	public void openWebBrowser(String stringURL) {
		WebBrowser.openBrowser(stringURL);
	}

	/**
	 * @return true for application modal device dialogs
	 */
	public boolean isDeviceDialogModal() {
		// return this.settings.isDeviceDialogsModal();
		return this.isDeviceDialogModal;
	}

	/**
	 * enable display of graphics header
	 */
	public void enableGraphicsHeader(boolean enabled) {
		this.graphicsTabItem.enableGraphicsHeader(enabled);
		this.histoExplorer.ifPresent(h -> h.enableGraphicsHeader(enabled));
		this.settings.setGraphicsHeaderVisible(enabled);
		this.isGraphicsHeaderVisible = enabled;
	}

	/**
	 * enable display of record set comment
	 */
	public void enableRecordSetComment(boolean enabled) {
		this.graphicsTabItem.enableRecordSetComment(enabled);
		this.histoExplorer.ifPresent(h -> h.enableRecordSetComment(enabled));
		this.settings.setRecordCommentVisible(enabled);
		this.isRecordCommentVisible = enabled;
	}

	/**
	 * @return the statusBar
	 */
	public StatusBar getStatusBar() {
		return this.statusBar;
	}

	/**
	 * update the object image
	 */
	public void updateObjectImage() {
		if (this.objectDescriptionTabItem != null) this.objectDescriptionTabItem.redrawImageCanvas();
	}

	/**
	 * check if file comment has pending change and update if required
	 */
	public void checkUpdateFileComment() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			if (this.fileCommentTabItem != null && this.fileCommentTabItem.isFileCommentChanged()) this.fileCommentTabItem.setFileComment();
		} else { // if the percentage is not up to date it will updated later
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (DataExplorer.this.fileCommentTabItem != null && DataExplorer.this.fileCommentTabItem.isFileCommentChanged())
						DataExplorer.this.fileCommentTabItem.setFileComment();
				}
			});
		}
	}

	/**
	 * @return the isRecordCommentChanged
	 */
	public boolean isFileCommentChanged() {
		return this.fileCommentTabItem.isFileCommentChanged();
	}

	/**
	 * check if file comment has pending change and update if required
	 */
	public void checkUpdateRecordSetComment() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			if (this.graphicsTabItem != null && this.graphicsTabItem.getGraphicsComposite().isRecordCommentChanged())
				this.graphicsTabItem.getGraphicsComposite().updateRecordSetComment();
		} else { // if the percentage is not up to date it will updated later
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (DataExplorer.this.graphicsTabItem.getGraphicsComposite().isRecordCommentChanged())
						DataExplorer.this.graphicsTabItem.getGraphicsComposite().updateRecordSetComment();
				}
			});
		}
	}

	/**
	 * @return the isRecordCommentChanged
	 */
	public boolean isRecordCommentChanged() {
		return this.graphicsTabItem.getGraphicsComposite().isRecordCommentChanged();
	}

	/**
	 * @return the canvasImage alias graphics window for the visible tab.
	 */
	public Image getGraphicsPrintImage() {
		Optional<Image> histoImage = this.histoExplorer.flatMap(h -> h.getGraphicsPrintImage());
		return this.isRecordSetVisible(GraphicsType.COMPARE) ? this.compareTabItem.getGraphicsComposite().getGraphicsPrintImage()
				: this.isRecordSetVisible(GraphicsType.UTIL) ? this.utilGraphicsTabItem.getGraphicsComposite().getGraphicsPrintImage()
						: histoImage.isPresent() ? histoImage.get() : this.graphicsTabItem.getGraphicsComposite().getGraphicsPrintImage();
	}

	/**
	 * return statistics window content as image
	 */
	public Image getGraphicsTabContentAsImage() {
		return this.isRecordSetVisible(GraphicsType.COMPARE) ? this.compareTabItem.getContentAsImage() : this.isRecordSetVisible(GraphicsType.COMPARE)
				? this.utilGraphicsTabItem.getContentAsImage() : this.graphicsTabItem.getContentAsImage();
	}

	/**
	 * return statistics window content as image
	 */
	public Image getStatisticsTabContentAsImage() {
		return this.statisticsTabItem.getContentAsImage();
	}

	/**
	 * return statistics window content as formated string
	 */
	public String getStatisticsAsText() {
		return this.statisticsTabItem.getContentAsText();
	}

	/**
	 * return table tab window content as image
	 */
	public Image getTableTabContentAsImage() {
		return this.dataTableTabItem.getContentAsImage();
	}

	/**
	 * return digital tab window content as image
	 */
	public Image getDigitalTabContentAsImage() {
		return this.digitalTabItem.getContentAsImage();
	}

	/**
	 * return analog tab window content as image
	 */
	public Image getAnalogTabContentAsImage() {
		return this.analogTabItem.getContentAsImage();
	}

	/**
	 * return cell voltage tab window content as image
	 */
	public Image getCellVoltageTabContentAsImage() {
		return this.cellVoltageTabItem.getContentAsImage();
	}

	/**
	 * return file description tab window content as image
	 */
	public Image getFileDescriptionTabContentAsImage() {
		return this.fileCommentTabItem.getContentAsImage();
	}

	/**
	 * get object description window as image for printing purpose
	 * @return object description window as image
	 */
	public Image getObjectTabContentAsImage() {
		return this.objectDescriptionTabItem.getContentAsImage();
	}

	/**
	 * copy tabulator content as image into clipboard
	 */
	public void copyTabContentAsImage() {
		Image graphicsImage = null;
		int tabSelectionIndex = this.displayTab.getSelectionIndex();
		if (tabSelectionIndex == 0) {
			graphicsImage = this.graphicsTabItem.getContentAsImage();
		} else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof StatisticsWindow) {
			graphicsImage = this.statisticsTabItem.getContentAsImage();
		} else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof DataTableWindow) {
			graphicsImage = this.dataTableTabItem.getContentAsImage();
		} else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof DigitalWindow) {
			graphicsImage = this.digitalTabItem.getContentAsImage();
		} else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof AnalogWindow) {
			graphicsImage = this.analogTabItem.getContentAsImage();
		} else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof CellVoltageWindow) {
			graphicsImage = this.cellVoltageTabItem.getContentAsImage();
		} else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof FileCommentWindow) {
			graphicsImage = this.fileCommentTabItem.getContentAsImage();
		} else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof ObjectDescriptionWindow) {
			graphicsImage = this.objectDescriptionTabItem.getContentAsImage();
		} else if ((DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) && DataExplorer.this.isRecordSetVisible(GraphicsType.COMPARE)) {
			graphicsImage = this.compareTabItem.getContentAsImage();
		} else if ((DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) && DataExplorer.this.isRecordSetVisible(GraphicsType.UTIL)) {
			graphicsImage = this.utilGraphicsTabItem.getContentAsImage();
		} else {
			Optional<Image> histoImage = this.histoExplorer.flatMap(h -> h.getContentAsImage());
			if (histoImage.isPresent()) {
				graphicsImage = histoImage.get();
			} else {
				graphicsImage = this.graphicsTabItem.getContentAsImage();
			}
		}
		Clipboard clipboard = new Clipboard(GDE.display);
		clipboard.setContents(new Object[] { graphicsImage.getImageData() }, new Transfer[] { ImageTransfer.getInstance() });
		clipboard.dispose();
		graphicsImage.dispose();
	}

	/**
	 *
	 */
	public void copyGraphicsPrintImage() {
		Image graphicsImage = this.getGraphicsPrintImage();
		Clipboard clipboard = new Clipboard(GDE.display);
		clipboard.setContents(new Object[] { graphicsImage.getImageData() }, new Transfer[] { ImageTransfer.getInstance() });
		clipboard.dispose();
		graphicsImage.dispose();
	}

	/**
	 * switch tab to the first tab item found by applying the filter predicate to the current tabs.
	 * @param tabFilter is filter predicate for one or more tab items
	 */
	public void selectTab(Predicate<? super CTabItem> tabFilter) {
		this.displayTab.setSelection(Arrays.stream(this.getTabFolder().getItems()).filter(tabFilter).findFirst().orElseThrow(UnsupportedOperationException::new));
		this.displayTab.showSelection();
	}

	/**
	 * switch tab by selection index
	 * @param index
	 */
	public void selectTab(int index) {
		this.displayTab.setSelection(index);
		this.displayTab.showSelection();
	}

	/**
	 * @return the tab selection index
	 */
	public int getTabSelectionIndex() {
		return this.displayTab.getSelectionIndex();
	}

	/**
	 * set the inner area background color (for curve graphics the curve area, for statistics ...)
	 * @param tabSelectionIndex the current tabulator item index
	 * @param innerAreaBackground the innerAreaBackground to set
	 */
	public void setInnerAreaBackground(int tabSelectionIndex, Color innerAreaBackground) {
		if (tabSelectionIndex == 0) {
			this.settings.setGraphicsCurveAreaBackground(innerAreaBackground);
			this.graphicsTabItem.setCurveAreaBackground(innerAreaBackground);
		} else if (tabSelectionIndex > 0) if (this.displayTab.getItem(tabSelectionIndex) instanceof StatisticsWindow) {
			this.settings.setSatisticsInnerAreaBackground(innerAreaBackground);
			this.statisticsTabItem.setInnerAreaBackground(innerAreaBackground);
		} else if (this.displayTab.getItem(tabSelectionIndex) instanceof DigitalWindow) {
			this.settings.setDigitalInnerAreaBackground(innerAreaBackground);
			this.digitalTabItem.setInnerAreaBackground(innerAreaBackground);
		} else if (this.displayTab.getItem(tabSelectionIndex) instanceof AnalogWindow) {
			this.settings.setAnalogInnerAreaBackground(innerAreaBackground);
			this.analogTabItem.setInnerAreaBackground(innerAreaBackground);
		} else if (this.displayTab.getItem(tabSelectionIndex) instanceof CellVoltageWindow) {
			this.settings.setCellVoltageInnerAreaBackground(innerAreaBackground);
			this.cellVoltageTabItem.setInnerAreaBackground(innerAreaBackground);
		} else if (this.displayTab.getItem(tabSelectionIndex) instanceof FileCommentWindow) {
			this.settings.setFileCommentInnerAreaBackground(innerAreaBackground);
			this.fileCommentTabItem.setInnerAreaBackground(innerAreaBackground);
		} else if (this.displayTab.getItem(tabSelectionIndex) instanceof ObjectDescriptionWindow) {
			this.settings.setObjectDescriptionInnerAreaBackground(innerAreaBackground);
			this.objectDescriptionTabItem.setInnerAreaBackground(innerAreaBackground);
		} else if ((this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) && this.isRecordSetVisible(GraphicsType.COMPARE)) {
			this.settings.setCompareCurveAreaBackground(innerAreaBackground);
			this.compareTabItem.setCurveAreaBackground(innerAreaBackground);
		} else if ((this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) && this.isRecordSetVisible(GraphicsType.UTIL)) {
			this.settings.setUtilityCurveAreaBackground(innerAreaBackground);
			this.utilGraphicsTabItem.setCurveAreaBackground(innerAreaBackground);
		} else {
			this.histoExplorer.ifPresent(h -> h.setInnerAreaBackground(innerAreaBackground));
		}
	}

	/**
	 * set the border color (for curve graphics the curve area border color, ...)
	 * @param tabItemIndex the current tabulator item index
	 * @param borderColor the borderColor to set
	 */
	public void setBorderColor(int tabItemIndex, Color borderColor) {
		if (tabItemIndex == 0) {
			this.settings.setCurveGraphicsBorderColor(borderColor);
			this.graphicsTabItem.setCurveAreaBorderColor(borderColor);
		} else if (tabItemIndex > 0)
			if ((this.displayTab.getItem(tabItemIndex) instanceof GraphicsWindow) && this.isRecordSetVisible(GraphicsType.COMPARE)) {
			this.settings.setCurveCompareBorderColor(borderColor);
			this.compareTabItem.setCurveAreaBorderColor(borderColor);
			} else if ((this.displayTab.getItem(tabItemIndex) instanceof GraphicsWindow) && this.isRecordSetVisible(GraphicsType.UTIL)) {
			this.settings.setUtilityCurvesBorderColor(borderColor);
			this.utilGraphicsTabItem.setCurveAreaBorderColor(borderColor);
			} else {
			this.histoExplorer.ifPresent(h -> h.setBorderColor(borderColor));
			}
	}

	/**
	 * set the tabulator specific font size, actually enabled for digital tab only
	 * @param newFonSize in dots added to standard widget font size
	 */
	public void setTabFontSize(int tabSelectionIndex, int newFonSize) {
		if (tabSelectionIndex == 0) {
			// actual no special font size adaption enabled
		} else if (tabSelectionIndex > 0) {
			if (this.displayTab.getItem(tabSelectionIndex) instanceof DigitalWindow) {
				// this.settings.setDigitalDisplayFontSize(newFonSize); actual no pesistens enabled
				this.digitalTabItem.setDigitalDisplayFontSize(newFonSize);
				this.updateDigitalWindowChilds();
			}
		}
	}

	/**
	 * set the surrounding area background color (for curve graphics, the area surrounding the curve area, ...)
	 * @param tabSelectionIndex the current tabulator item index
	 * @param surroundingBackground the surroundingBackground to set
	 */
	public void setSurroundingBackground(int tabSelectionIndex, Color surroundingBackground) {
		if (tabSelectionIndex == 0) {
			this.settings.setGraphicsSurroundingBackground(surroundingBackground);
			this.graphicsTabItem.setSurroundingBackground(surroundingBackground);
		} else if (tabSelectionIndex > 0) if (this.displayTab.getItem(tabSelectionIndex) instanceof StatisticsWindow) {
			this.settings.setSatisticsSurroundingAreaBackground(surroundingBackground);
			this.statisticsTabItem.setSurroundingAreaBackground(surroundingBackground);
		} else if (this.displayTab.getItem(tabSelectionIndex) instanceof DigitalWindow) {
			this.settings.setDigitalSurroundingAreaBackground(surroundingBackground);
			this.digitalTabItem.setSurroundingAreaBackground(surroundingBackground);
		} else if (this.displayTab.getItem(tabSelectionIndex) instanceof AnalogWindow) {
			this.settings.setAnalogSurroundingAreaBackground(surroundingBackground);
			this.analogTabItem.setSurroundingAreaBackground(surroundingBackground);
		} else if (this.displayTab.getItem(tabSelectionIndex) instanceof CellVoltageWindow) {
			this.settings.setCellVoltageSurroundingAreaBackground(surroundingBackground);
			this.cellVoltageTabItem.setSurroundingAreaBackground(surroundingBackground);
		} else if (this.displayTab.getItem(tabSelectionIndex) instanceof FileCommentWindow) {
			this.settings.setFileCommentSurroundingAreaBackground(surroundingBackground);
			this.fileCommentTabItem.setSurroundingAreaBackground(surroundingBackground);
		} else if (this.displayTab.getItem(tabSelectionIndex) instanceof ObjectDescriptionWindow) {
			this.settings.setObjectDescriptionSurroundingAreaBackground(surroundingBackground);
			this.objectDescriptionTabItem.setSurroundingAreaBackground(surroundingBackground);
		} else if ((this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) && this.isRecordSetVisible(GraphicsType.COMPARE)) {
			this.settings.setCompareSurroundingBackground(surroundingBackground);
			this.compareTabItem.setSurroundingBackground(surroundingBackground);
		} else if ((this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) && this.isRecordSetVisible(GraphicsType.UTIL)) {
			this.settings.setUtilitySurroundingBackground(surroundingBackground);
			this.utilGraphicsTabItem.setSurroundingBackground(surroundingBackground);
		} else {
			this.histoExplorer.ifPresent(h -> h.setSurroundingBackground(surroundingBackground));
		}
	}

	public void setAbsoluteDateTime(boolean enable) {
		this.dataTableTabItem.setAbsoluteDateTime(enable);
		this.dataTableTabItem.setHeader();
		Channel activeChannel = this.analyzer.getActiveChannel();
		RecordSet activeRecordSet = activeChannel != null ? activeChannel.getActiveRecordSet() : null;
		if (activeRecordSet != null) {
			this.dataTableTabItem.setRowCount(activeRecordSet.getRecordDataSize(false));
		}
	}

	/**
	 * set data table tab item visible or invisible
	 * @param visible
	 */
	public void setDataTableTabItemVisible(boolean visible) {
		if (visible) {
			if (this.dataTableTabItem == null || this.dataTableTabItem.isDisposed()) {
				this.dataTableTabItem = new DataTableWindow(this.displayTab, SWT.NONE, 2);
				this.dataTableTabItem.create();
			}
		} else {
			if (this.dataTableTabItem != null && !this.dataTableTabItem.isDisposed()) {
				this.dataTableTabItem.dispose();
				this.dataTableTabItem = null;
			}
		}
	}

	/**
	 * set digital tab item visible or invisible
	 * @param visible
	 */
	public void setDigitalTabItemVisible(boolean visible) {
		if (visible) {
			if (this.digitalTabItem == null || this.digitalTabItem.isDisposed()) {
				int position = (this.displayTab.getItem(2) instanceof DataTableWindow) ? 3 : 2;
				this.digitalTabItem = new DigitalWindow(this.displayTab, SWT.NONE, position);
				this.digitalTabItem.create();
			}
		} else {
			if (this.digitalTabItem != null && !this.digitalTabItem.isDisposed()) {
				this.digitalTabItem.dispose();
				this.digitalTabItem = null;
			}
		}
	}

	/**
	 * set analog tab item visible or invisible
	 * @param visible
	 */
	public void setAnalogTabItemVisible(boolean visible) {
		if (visible) {
			if (this.analogTabItem == null || this.analogTabItem.isDisposed()) {
				int position = (this.displayTab.getItem(2) instanceof DataTableWindow) && (this.displayTab.getItem(3) instanceof DigitalWindow) ? 4
						: (this.displayTab.getItem(2) instanceof DataTableWindow) || (this.displayTab.getItem(2) instanceof DigitalWindow) ? 3 : 2;
				this.analogTabItem = new AnalogWindow(this.displayTab, SWT.NONE, position);
				this.analogTabItem.create();
			}
		} else {
			if (this.analogTabItem != null && !this.analogTabItem.isDisposed()) {
				this.analogTabItem.dispose();
				this.analogTabItem = null;
			}
		}
	}

	/**
	 * set data table tab item visible or invisible
	 * @param visible
	 */
	public void setCellVoltageTabItemVisible(boolean visible) {
		if (visible) {
			if (this.cellVoltageTabItem == null || this.cellVoltageTabItem.isDisposed()) {
				int position = 2;
				CTabItem[] tabItems = this.displayTab.getItems();
				for (int i = 1; i < tabItems.length; i++) {
					if (tabItems[i] instanceof GraphicsWindow || tabItems[i] instanceof FileCommentWindow) position = i;
				}
				this.cellVoltageTabItem = new CellVoltageWindow(this.displayTab, SWT.NONE, position);
				this.cellVoltageTabItem.create();
			}
		} else {
			if (this.cellVoltageTabItem != null && !this.cellVoltageTabItem.isDisposed()) {
				this.cellVoltageTabItem.dispose();
				this.cellVoltageTabItem = null;
			}
		}
	}

	/**
	 * create a compare window tab item
	 */
	public void createCompareWindowTabItem() {
		if (this.compareTabItem == null || this.compareTabItem.isDisposed()) {
			for (int i = 0; i < this.displayTab.getItemCount(); ++i) {
				CTabItem tabItem = this.displayTab.getItems()[i];
				if (tabItem instanceof FileCommentWindow) {
					this.compareTabItem = new GraphicsWindow(this.displayTab, SWT.NONE, GraphicsType.COMPARE, Messages.getString(MessageIds.GDE_MSGT0144), i);
					this.compareTabItem.create();
					break;
				}
			}
		}
	}

	/**
	 * @param visible boolean value to set the object description tabulator visible
	 */
	public void setObjectDescriptionTabVisible(boolean visible) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			if (visible) {
				if (this.objectDescriptionTabItem == null || this.objectDescriptionTabItem.isDisposed()) {
					for (int i = 0; i < this.displayTab.getItemCount(); ++i) {
						CTabItem tabItem = this.displayTab.getItems()[i];
						if (tabItem instanceof FileCommentWindow) {
							this.objectDescriptionTabItem = new ObjectDescriptionWindow(this.displayTab, SWT.NONE, i + 1);
							this.objectDescriptionTabItem.create();
							break;
						}
					}
				}
			} else {
				if (this.objectDescriptionTabItem != null) {
					this.objectDescriptionTabItem.dispose();
					this.objectDescriptionTabItem = null;
				}
			}
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (visible) {
						if (objectDescriptionTabItem == null || objectDescriptionTabItem.isDisposed()) {
							for (int i = 0; i < displayTab.getItemCount(); ++i) {
								CTabItem tabItem = displayTab.getItems()[i];
								if (tabItem instanceof FileCommentWindow) {
									objectDescriptionTabItem = new ObjectDescriptionWindow(displayTab, SWT.NONE, i + 1);
									objectDescriptionTabItem.create();
									break;
								}
							}
						}
					} else {
						if (objectDescriptionTabItem != null) {
							objectDescriptionTabItem.dispose();
							objectDescriptionTabItem = null;
						}
					}
				}
			});
		}
	}

	/**
	 * @return the current object data object if object oriented, else null wil be returned
	 */
	public void updateCurrentObjectData(String newObjectKey) {
		if (this.objectDescriptionTabItem != null) {
			this.objectDescriptionTabItem.setObject(this.objectDescriptionTabItem.getObject(), newObjectKey);
			this.updateObjectDescriptionWindow();
		}
	}

	/**
	 * register a utility graphics tab item for device specific purpose
	 */
	public GraphicsWindow setUtilGraphicsWindowVisible(boolean visible, String tabName) {
		if (visible) {
			if (this.utilGraphicsTabItem == null || this.utilGraphicsTabItem.isDisposed()) {
				this.utilGraphicsTabItem = new GraphicsWindow(this.displayTab, SWT.NONE, GraphicsType.UTIL, tabName.length() < 3
						? Messages.getString(MessageIds.GDE_MSGT0282) : tabName, this.displayTab.getItemCount());
				this.utilGraphicsTabItem.create();
			}
		} else {
			if (this.utilGraphicsTabItem != null) {
				this.utilGraphicsTabItem.dispose();
				this.utilGraphicsTabItem = null;
			}
		}
		return this.utilGraphicsTabItem;
	}

	/**
	 * register a custom tab item for device specific purpose
	 */
	public void registerCustomTabItem(CTabItem customDeviceTabItem) {
		if (customDeviceTabItem == null) {
			for (CTabItem tab : this.customTabItems) {
				tab.dispose();
			}
			this.customTabItems.clear();
		} else {
			this.customTabItems.add(customDeviceTabItem);
		}
	}

	/**
	 * @return the display tab folder which is required to create and register a new custom tabItem * @return
	 */
	public CTabFolder getTabFolder() {
		return this.displayTab;
	}

	/**
	 * set the main shell icon to its default
	 */
	public void resetShellIcon() {
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				GDE.shell.setImage(SWTResourceManager.getImage(GDE.IS_MAC ? "gde/resource/DataExplorer_MAC.png" : "gde/resource/DataExplorer.png")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
	}

	/**
	 * @return the object data from the objectDescriptionTabItem
	 */
	public ObjectData getObject() {
		return this.objectDescriptionTabItem.getObject();
	}

	/**
	 * @return the object data from the objectDescriptionTabItem
	 */
	public ObjectData getActiveObject() {
		if (this.objectDescriptionTabItem != null && !this.objectDescriptionTabItem.isDisposed()) {
			return this.objectDescriptionTabItem.getObject();
		} 
		return null;
	}

	/**
	 * @return the active record set or null
	 */
	public RecordSet getActiveRecordSet() {
		RecordSet activeRecordSet = null;
		Channel activeChannnel = this.analyzer.getActiveChannel();
		if (activeChannnel != null) activeRecordSet = activeChannnel.getActiveRecordSet();

		return activeRecordSet;
	}

	/**
	 * @return the number of the active channel or 1
	 */
	public int getActiveChannelNumber() {
		int activeChannelNumber = 1;
		if (this.analyzer != null && this.analyzer.getActiveChannel() != null) activeChannelNumber = this.analyzer.getActiveChannel().getNumber();
		return activeChannelNumber;
	}

	/**
	 * @return the active channel or null
	 */
	public Channel getActiveChannel() {
		return this.analyzer.getActiveChannel();
	}

	/**
	 * switch around to previous tabulator
	 */
	public void switchPreviousTabulator() {
		CTabItem[] tabItems = this.displayTab.getItems();
		for (int i = 0; i < tabItems.length; i++) {
			CTabItem tabItem = tabItems[i];
			if (tabItem.getControl().isVisible()) {
				if (i - 1 >= 0)
					tabItem.getParent().setSelection(i - 1);
				else
					tabItem.getParent().setSelection(tabItems.length - 1);
				break;
			}
		}
	}

	/**
	 * switch around to next tabulator
	 */
	public void switchNextTabulator() {
		CTabItem[] tabItems = this.displayTab.getItems();
		for (int i = 0; i < tabItems.length; i++) {
			CTabItem tabItem = tabItems[i];
			if (tabItem.getControl().isVisible()) {
				if (i + 1 <= tabItems.length - 1)
					tabItem.getParent().setSelection(i + 1);
				else
					tabItem.getParent().setSelection(0);
				break;
			}
		}
	}

	/**
	 * updates the extensionFilterMap to be used for opening a file selection dialog
	 */
	public synchronized void updateExtensionFilterMap(String key, String value) {
		this.extensionFilterMap.put(key, value);
	}

	public void enableWritingTmpFiles(boolean enable) {
		if (enable && (this.writeTmpFileThread == null || !this.writeTmpFileThread.isAlive())) {
			this.isTmpWriteStop = false;
			this.writeTmpFileThread = new Thread("write_tmp") {
				@Override
				public void run() {
					while (!DataExplorer.this.isTmpWriteStop) {
						try {
							// cycle for 5 minutes in steps of1 second to enable thread destroy while exiting
							int cycleCount = 0;
							while (!DataExplorer.this.isTmpWriteStop && cycleCount++ < 60 * 5)
								Thread.sleep(1000);

							if (DataExplorer.this.isTmpWriteStop) break;

							String tmpFilePath = Settings.getApplHomePath() + GDE.STRING_FILE_SEPARATOR_UNIX + GDE.TEMP_FILE_STEM;
							if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "attempt to save a temporary file(s)");
							if (DataExplorer.this.analyzer.getActiveChannel() != null && DataExplorer.this.analyzer.getActiveChannel().getType() == ChannelTypes.TYPE_CONFIG) {
								if (DataExplorer.this.analyzer.getActiveChannel().getActiveRecordSet() != null)
									OsdReaderWriter.write(tmpFilePath + GDE.FILE_ENDING_DOT_OSD, DataExplorer.this.analyzer.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
							} else
								for (int i = 1; i <= DataExplorer.this.analyzer.getChannels().size() && DataExplorer.this.analyzer.getActiveChannel().getActiveRecordSet() != null;
										++i) {
									if (DataExplorer.this.analyzer.getChannels().get(i).size() > 0)
										OsdReaderWriter.write(tmpFilePath + "_" + i + GDE.FILE_ENDING_DOT_OSD, DataExplorer.this.analyzer.getChannels().get(i), GDE.DATA_EXPLORER_FILE_VERSION_INT);
								}
						} catch (Exception e) {
							log.log(Level.WARNING, e.getMessage(), e);
						}
					}
				}
			};
			this.writeTmpFileThread.start();
		} else {
			this.isTmpWriteStop = true;
		}
	}

	/**
	 * checks for update to installed version and enable open download page if a newer version is available
	 */
	public void check4update() {
		final String[] versionCheck = FileUtils.isUpdateAvailable();
		if (Boolean.valueOf(versionCheck[0])) {
			if (openUpdateMessageDialogSync()) {
				new Thread("Download") {
					@Override
					public void run() {
						try {
							String downloadUrl = "https://download.savannah.gnu.org/releases/dataexplorer/";
							String arch = System.getProperty("sun.arch.data.model");
							String version = versionCheck[1];
							String filename = GDE.STRING_EMPTY;
							if (GDE.IS_WINDOWS) // DataExplorer_Setup_3.0.8_win64.exe
								filename = "DataExplorer_Setup_" + version + "_win" + arch + GDE.FILE_ENDING_DOT_EXE;
							else if (GDE.IS_LINUX && GDE.IS_OS_ARCH_ARM) // dataexplorer-3.0.8-bin_GNULinux_x86_64.tar.gz
								filename = "dataexplorer-" + version + "-bin_RaspbianLinux_ARM_" + arch + ".tar.gz";
							else if (GDE.IS_LINUX && !GDE.IS_OS_ARCH_ARM) // dataexplorer-3.0.8-bin_GNULinux_x86_64.tar.gz
								filename = "dataexplorer-" + version + "-bin_GNULinux_x86_" + arch + ".tar.gz";
							else if (GDE.IS_MAC) // DataExplorer-3.0.8_Mac_64.dmg
								filename = "DataExplorer-" + version + "_Mac_" + arch + ".dmg";

							final String targetFilePath = GDE.JAVA_IO_TMPDIR + GDE.STRING_FILE_SEPARATOR_UNIX + filename;

							if (!new File(targetFilePath).exists()) FileUtils.downloadFile(new URL(downloadUrl + filename), targetFilePath);

							GDE.display.syncExec(new Runnable() {
								@Override
								public void run() {
									if (GDE.IS_LINUX) {
										URL url = GDE.class.getProtectionDomain().getCodeSource().getLocation();
										if (url.getFile().endsWith(GDE.FILE_ENDING_DOT_JAR)) {
											String installpath = url.getFile().substring(0, url.getPath().lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX));
											installpath = installpath.substring(0, installpath.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX));
											String command = "cd " + installpath + "; sudo tar -xzf " + targetFilePath + "\"";
											log.log(Level.FINE, "command = " + command);
											MessageBox message = new MessageBox(GDE.shell, SWT.ICON_INFORMATION);
											message.setText(GDE.NAME_LONG);
											message.setMessage(Messages.getString(MessageIds.GDE_MSGI0055, new String[] { command }));
											message.open();
										}
									} else {
										MessageBox message = new MessageBox(GDE.shell, SWT.YES | SWT.NO | SWT.ICON_INFORMATION);
										message.setText(GDE.NAME_LONG);
										message.setMessage(Messages.getString(MessageIds.GDE_MSGI0053));
										if (SWT.YES == message.open()) {
											OperatingSystemHelper.launchInstallApplication(targetFilePath);
											GDE.shell.dispose();
										}
									}
								}
							});
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}.start();
			}
		}
	}

	/**
	 * reload all resource which are language related
	 */
	public void reloadLanguageRelatedResources() {
		Messages.reloadResources();
		DeviceXmlResource.reloadResources();
	}

	public Optional<HistoExplorer> getHistoExplorer() {
		return this.histoExplorer;
	}

	public HistoExplorer getPresentHistoExplorer() {
		return this.histoExplorer.orElseThrow(UnsupportedOperationException::new);
	}

	/**
	 * set color schema to be used
	 * @param schema
	 */
	public void setColorSchemaColors(String schema) {
		switch (schema) {
		default:
		case Settings.COLOR_SCHEMA_SYSTEM:
			this.COLOR_BACKGROUND	= SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND);
			this.COLOR_FOREGROUND	= SWTResourceManager.getColor(SWT.COLOR_WIDGET_FOREGROUND);
			break;

		case Settings.COLOR_SCHEMA_LIGHT:
			this.COLOR_BACKGROUND	= SWTResourceManager.getColor(SWT.COLOR_GRAY);
			this.COLOR_FOREGROUND	= SWTResourceManager.getColor(SWT.COLOR_BLACK);
			break;
		case Settings.COLOR_SCHEMA_DARK:
			this.COLOR_BACKGROUND	= SWTResourceManager.getColor(80, 80, 80);
			this.COLOR_FOREGROUND	= SWTResourceManager.getColor(SWT.COLOR_WHITE);
			break;
		}
		//updating colors of a widgets
		if (this.menuCoolBar != null) {
			this.setBackground(this.COLOR_BACKGROUND);
			this.menuCoolBar.setBackground(this.COLOR_BACKGROUND);
			this.menuToolBar.updateColorSchema();
			this.displayTab.setBackground(new Color[]{this.COLOR_BACKGROUND, this.settings.getGraphicsSurroundingBackground()}, new int[]{100}, true);
			this.displayTab.setForeground(this.COLOR_FOREGROUND);
			this.graphicsTabItem.updateColorSchema();
			this.statusBar.updateColorSchema();
		}
		this.histoExplorer.ifPresent(h -> h.updateColorSchema());
	}
}
