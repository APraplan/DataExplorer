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
package gde.ui.menu;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ArmEvent;
import org.eclipse.swt.events.ArmListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.GraphicsTemplate;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.histo.config.HistoGraphicsTemplate;
import gde.histo.recordings.TrailRecordSet;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.dialog.DeviceSelectionDialog;
import gde.ui.dialog.PrintSelectionDialog;
import gde.ui.dialog.TimeSetDialog;
import gde.ui.dialog.edit.DevicePropertiesEditor;
import gde.ui.tab.GraphicsComposite.GraphicsMode;
import gde.ui.tab.GraphicsWindow;

/**
 * menu bar implementation class for the DataExplorer
 * @author Winfried Brügmann
 */
public class MenuBar {
	private final static String	$CLASS_NAME	= MenuBar.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	private final Settings			settings		= Settings.getInstance();

	MenuItem										fileMenuItem;
	Menu												fileMenu;
	MenuItem										openFileMenuItem;
	MenuItem										historyFileMenuItem;
	MenuItem										toolBoxDeviceMenuItem, portMenuItem;
	MenuItem										aboutMenuItem;
	MenuItem										contentsMenuItem, webCheckMenuItem;
	Menu												helpMenu;
	MenuItem										helpMenuItem;
	private MenuItem						recordCommentMenuItem;
	private MenuItem						graphicsCurveSurveyMenuItem;
	private MenuItem						graphicsHeaderMenuItem;
	MenuItem										prevTabConfigItem;
	MenuItem										nextTabConfigItem;
	private MenuItem						curveSelectionMenuItem;
	MenuItem										prevChannelConfigItem;
	MenuItem										nextChannelConfigItem;
	MenuItem										prevRecordSetItem;
	MenuItem										nextRecordSetItem;
	MenuItem										nextChartItem, refreshItem;
	Menu												viewMenu;
	MenuItem										viewMenuItem;
	private MenuItem						suppressModeItem, partialTableMenuItem;
	Menu												graphicsMenu;
	MenuItem										graphicsMenuItem, saveDefaultGraphicsTemplateItem, restoreDefaultGraphicsTemplateItem, saveAsGraphicsTemplateItem, restoreGraphicsTemplateItem;
	MenuItem										objectTemplatesItem, smartStatisticsItem;
	MenuItem										csvExportMenuItem1, csvExportMenuItem2, csvExportMenuItem3;
	MenuItem										nextDeviceMenuItem;
	MenuItem										prevDeviceMenuItem;
	MenuItem										selectDeviceMenuItem;
	Menu												deviceMenu;
	MenuItem										deviceMenuItem;
	MenuItem										copyTabContentAsImageMenuItem, copyGraphicsPrintImageMenuItem;
	MenuItem										activateZoomGraphicMenuItem, resetZoomGraphicMenuItem, panGraphicMenuItem;
	Menu												editMenu;
	MenuItem										editMenuItem;
	MenuItem										printMenuItem, startTimeMenuItem;
	MenuItem										exitMenuItem;
	MenuItem										preferencesFileMenuItem;
	MenuItem										devicePropertyFileEditMenuItem;
	Menu												exportMenu;
	MenuItem										exportFileMenuItem;
	MenuItem										csvImportMenuItem1, csvImportMenuItem2;
	Menu												importMenu;
	MenuItem										importFileMenuItem;
	Menu												fileHistoryMenu;
	MenuItem										saveAsFileMenuItem;
	MenuItem										saveFileMenuItem;
	MenuItem										newFileMenuItem;
	MenuItem										deleteFileMenuItem;

	int													iconSet			= DeviceCommPort.ICON_SET_OPEN_CLOSE;

	final Menu									parent;
	final DataExplorer					application;
	final Channels							channels;
	final FileHandler						fileHandler;

	public MenuBar(DataExplorer currentApplication, Menu menuParent) {
		this.application = currentApplication;
		this.parent = menuParent;
		this.channels = Channels.getInstance();
		this.fileHandler = new FileHandler();
	}

	/**
	 *
	 */
	public void create() {
		{
			this.fileMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.fileMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0001));
			this.fileMenuItem.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.log(Level.FINE, "fileMenuItem.helpRequested, event=" + evt); //$NON-NLS-1$
					MenuBar.this.application.openHelpDialog(GDE.STRING_EMPTY, "HelpInfo_3.html"); //$NON-NLS-1$
				}
			});
			{
				this.fileMenu = new Menu(this.fileMenuItem);
				this.fileMenu.addMenuListener(new MenuListener() {
					@Override
					public void menuShown(MenuEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "fileMenu.handleEvent, event=" + evt); //$NON-NLS-1$
						MenuBar.this.updateSubHistoryMenuItem(GDE.STRING_EMPTY);

						// check if the deleteFileMenuItem should be enabled
						boolean fileIsLoaded = false;
						Channel activeChannel = MenuBar.this.channels.getActiveChannel();
						if (activeChannel != null) {
							String filename = activeChannel.getFullQualifiedFileName();
							if (filename != null && !filename.isEmpty()) {
								fileIsLoaded = true;
							}
						}
						MenuBar.this.deleteFileMenuItem.setEnabled(fileIsLoaded);

					}

					@Override
					public void menuHidden(MenuEvent evt) {
						log.log(Level.FINEST, "fileMenu.menuHidden " + evt); //$NON-NLS-1$
					}
				});
				{
					this.newFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.newFileMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0002, GDE.MOD1));
					this.newFileMenuItem.setImage(SWTResourceManager.getImage("gde/resource/NewHot.gif")); //$NON-NLS-1$
					this.newFileMenuItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0002));
					this.newFileMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "newFileMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.application.getDeviceSelectionDialog().checkDataSaved()) {
								MenuBar.this.application.getDeviceSelectionDialog().setupDataChannels(MenuBar.this.application.getActiveDevice());
							}
						}
					});
				}
				{
					this.openFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.openFileMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0003, GDE.MOD1));
					this.openFileMenuItem.setImage(SWTResourceManager.getImage("gde/resource/OpenHot.gif")); //$NON-NLS-1$
					this.openFileMenuItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0003));
					this.openFileMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "openFileMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.fileHandler.openFileDialog(Messages.getString(MessageIds.GDE_MSGT0004));
						}
					});
				}
				{
					this.saveFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.saveFileMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0005, GDE.MOD1));
					this.saveFileMenuItem.setImage(SWTResourceManager.getImage("gde/resource/SaveHot.gif")); //$NON-NLS-1$
					this.saveFileMenuItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0005));
					this.saveFileMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "saveFileMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							Channel activeChannel = MenuBar.this.channels.getActiveChannel();
							if (activeChannel != null) {
								if (!activeChannel.isSaved())
									MenuBar.this.fileHandler.saveOsdFile(Messages.getString(MessageIds.GDE_MSGT0006), GDE.STRING_EMPTY);
								else
									MenuBar.this.fileHandler.saveOsdFile(Messages.getString(MessageIds.GDE_MSGT0007), activeChannel.getFileName());
							}
						}
					});
				}
				{
					this.saveAsFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.saveAsFileMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0008));
					this.saveAsFileMenuItem.setImage(SWTResourceManager.getImage("gde/resource/SaveAsHot.gif")); //$NON-NLS-1$
					this.saveAsFileMenuItem.setAccelerator(SWT.F12);
					this.saveAsFileMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "saveAsFileMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.fileHandler.saveOsdFile(Messages.getString(MessageIds.GDE_MSGT0006), GDE.STRING_EMPTY);
						}
					});
				}
				{
					this.deleteFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.deleteFileMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0667));
					this.deleteFileMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "deleteFileMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.fileHandler.deleteOsdFile()) {
								// initialize new data
								MenuBar.this.application.getDeviceSelectionDialog().setupDataChannels(MenuBar.this.application.getActiveDevice());
							}
						}
					});
				}
				{
					this.historyFileMenuItem = new MenuItem(this.fileMenu, SWT.CASCADE);
					this.historyFileMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0009));
					{
						this.fileHistoryMenu = new Menu(this.historyFileMenuItem);
						this.historyFileMenuItem.setMenu(this.fileHistoryMenu);
					}
				}
				{
					new MenuItem(this.fileMenu, SWT.SEPARATOR);
				}
				{
					this.importFileMenuItem = new MenuItem(this.fileMenu, SWT.CASCADE);
					this.importFileMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0010));
					{
						this.importMenu = new Menu(this.importFileMenuItem);
						this.importFileMenuItem.setMenu(this.importMenu);
						{
							this.csvImportMenuItem1 = new MenuItem(this.importMenu, SWT.PUSH);
							this.csvImportMenuItem1.setText(Messages.getString(MessageIds.GDE_MSGT0011));
							this.csvImportMenuItem1.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "csvImportMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
									MenuBar.this.fileHandler.importFileCSV(Messages.getString(MessageIds.GDE_MSGT0012), false);
								}
							});
						}
						{
							this.csvImportMenuItem2 = new MenuItem(this.importMenu, SWT.PUSH);
							this.csvImportMenuItem2.setText(Messages.getString(MessageIds.GDE_MSGT0013));
							this.csvImportMenuItem2.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "csvImportMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
									MenuBar.this.fileHandler.importFileCSV(Messages.getString(MessageIds.GDE_MSGT0014), true);
								}
							});
						}
					}
				}
				{
					this.exportFileMenuItem = new MenuItem(this.fileMenu, SWT.CASCADE);
					this.exportFileMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0015));
					{
						this.exportMenu = new Menu(this.exportFileMenuItem);
						this.exportFileMenuItem.setMenu(this.exportMenu);
						{
							this.csvExportMenuItem1 = new MenuItem(this.exportMenu, SWT.CASCADE);
							this.csvExportMenuItem1.setText(Messages.getString(MessageIds.GDE_MSGT0016));
							this.csvExportMenuItem1.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "csvExportMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
									MenuBar.this.fileHandler.exportFileCSV(Messages.getString(MessageIds.GDE_MSGT0017), false, "ISO-8859-1");
								}
							});
						}
						{
							this.csvExportMenuItem2 = new MenuItem(this.exportMenu, SWT.CASCADE);
							this.csvExportMenuItem2.setText(Messages.getString(MessageIds.GDE_MSGT0018));
							this.csvExportMenuItem2.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "csvExportMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
									MenuBar.this.fileHandler.exportFileCSV(Messages.getString(MessageIds.GDE_MSGT0019), true, "ISO-8859-1");
								}
							});
						}
						{
							this.csvExportMenuItem3 = new MenuItem(this.exportMenu, SWT.CASCADE);
							this.csvExportMenuItem3.setText(Messages.getString(MessageIds.GDE_MSGT0732));
							this.csvExportMenuItem3.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "csvExportMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
									MenuBar.this.fileHandler.exportFileCSV(Messages.getString(MessageIds.GDE_MSGT0733), false, "UTF-8");
								}
							});
						}
					}
				}
				if (!GDE.IS_MAC) {
					{
						new MenuItem(this.fileMenu, SWT.SEPARATOR);
					}
					{
						this.preferencesFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
						this.preferencesFileMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0020, GDE.MOD3));
						this.preferencesFileMenuItem.setAccelerator(SWT.MOD3 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0052));
						this.preferencesFileMenuItem.setImage(SWTResourceManager.getImage("gde/resource/SettingsHot.gif")); //$NON-NLS-1$
						this.preferencesFileMenuItem.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								openPreferencesDialog(evt);
							}
						});
					}
				}
				{
					new MenuItem(this.fileMenu, SWT.SEPARATOR);
				}
				{
					this.printMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.printMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0052, GDE.MOD1));
					this.printMenuItem.setImage(SWTResourceManager.getImage("gde/resource/PrintHot.gif")); //$NON-NLS-1$
					this.printMenuItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0052));
					this.printMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "exitMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							new PrintSelectionDialog(GDE.shell, SWT.NULL).open();
						}
					});
				}
				{
					this.startTimeMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.startTimeMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0713));
					this.startTimeMenuItem.setImage(SWTResourceManager.getImage("gde/resource/TimeHot.gif")); //$NON-NLS-1$
					this.startTimeMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "exitMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							new TimeSetDialog(GDE.shell, SWT.NULL).open(new Date().getTime());
						}
					});
				}
				if (!GDE.IS_MAC) {
					{
						new MenuItem(this.fileMenu, SWT.SEPARATOR);
					}
					{
						this.exitMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
						this.exitMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0021, GDE.MOD1));
						this.exitMenuItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0021));
						this.exitMenuItem.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								closeApplication(evt);
							}
						});
					}
				}
				this.fileMenuItem.setMenu(this.fileMenu);
			}
		}
		{
			this.editMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.editMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0022));
			this.editMenuItem.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.log(Level.FINE, "editMenuItem.helpRequested, event=" + evt); //$NON-NLS-1$
					MenuBar.this.application.openHelpDialog(GDE.STRING_EMPTY, "HelpInfo_31.html"); //$NON-NLS-1$
				}
			});
			{
				this.editMenu = new Menu(this.editMenuItem);
				this.editMenuItem.setMenu(this.editMenu);
				this.editMenu.addMenuListener(new MenuListener() {
					@Override
					public void menuShown(MenuEvent e) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "editMenu.menuShown, event=" + e); //$NON-NLS-1$
						Channel activeChannel = MenuBar.this.channels.getActiveChannel();
						boolean isRecordSetRelatedCopyable = false;
						if (activeChannel != null) {
							RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
							if (activeRecordSet != null) isRecordSetRelatedCopyable = activeRecordSet.size() > 0;
						}
						boolean isCompareSetCopyable = MenuBar.this.application.isWithCompareSet() && MenuBar.this.application.getTabSelectionIndex() == 6;

						MenuBar.this.copyTabContentAsImageMenuItem.setEnabled(isRecordSetRelatedCopyable);
						MenuBar.this.copyGraphicsPrintImageMenuItem.setEnabled((isRecordSetRelatedCopyable && MenuBar.this.application.getTabSelectionIndex() == 0) || isCompareSetCopyable);
					}

					@Override
					public void menuHidden(MenuEvent e) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "editMenu.menuHidden, event=" + e); //$NON-NLS-1$
					}
				});
				{
					this.activateZoomGraphicMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.activateZoomGraphicMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0023));
					this.activateZoomGraphicMenuItem.setImage(SWTResourceManager.getImage("gde/resource/ZoomHot.gif")); //$NON-NLS-1$
					this.activateZoomGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "activateZoomGraphicMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.setGraphicsMode(GraphicsMode.ZOOM, true);
						}
					});
				}
				{
					this.resetZoomGraphicMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.resetZoomGraphicMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0024));
					this.resetZoomGraphicMenuItem.setImage(SWTResourceManager.getImage("gde/resource/ExpandHot.gif")); //$NON-NLS-1$
					this.resetZoomGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "resetZoomGraphicMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.setGraphicsMode(GraphicsMode.RESET, false);
						}
					});
				}
				{
					this.panGraphicMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.panGraphicMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0025));
					this.panGraphicMenuItem.setImage(SWTResourceManager.getImage("gde/resource/PanHot.gif")); //$NON-NLS-1$
					this.panGraphicMenuItem.setEnabled(false);
					this.panGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "panGraphicMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.setGraphicsMode(GraphicsMode.PAN, true);
						}
					});
				}
				{
					new MenuItem(this.editMenu, SWT.SEPARATOR);
				}
				{
					this.copyTabContentAsImageMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.copyTabContentAsImageMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0026, GDE.MOD1));
					this.copyTabContentAsImageMenuItem.setAccelerator(SWT.MOD1 + SWT.SHIFT + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0026));
					this.copyTabContentAsImageMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "copyTabContentAsImageMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.copyTabContentAsImage();
						}
					});
				}
				{
					this.copyGraphicsPrintImageMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.copyGraphicsPrintImageMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0027, GDE.MOD1));
					this.copyGraphicsPrintImageMenuItem.setAccelerator(SWT.MOD1 + SWT.SHIFT + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0027));
					this.copyGraphicsPrintImageMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "copyGraphicsPrintImageMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.copyGraphicsPrintImage();
						}
					});
				}
			}
		}
		{
			this.deviceMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.deviceMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0028));
			this.deviceMenuItem.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.log(Level.FINE, "deviceMenuItem.helpRequested, event=" + evt); //$NON-NLS-1$
					MenuBar.this.application.openHelpDialog(GDE.STRING_EMPTY, "HelpInfo_32.html"); //$NON-NLS-1$
				}
			});
			{
				this.deviceMenu = new Menu(this.deviceMenuItem);
				this.deviceMenuItem.setMenu(this.deviceMenu);
				{
					this.portMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.portMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0049));
					this.portMenuItem.setImage(SWTResourceManager.getImage("gde/resource/BulletHotRed.gif")); //$NON-NLS-1$
					this.portMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "selectDeviceMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							IDevice activeDevice = MenuBar.this.application.getActiveDevice();
							if (activeDevice != null) activeDevice.open_closeCommPort();
						}
					});
				}
				{
					new MenuItem(this.deviceMenu, SWT.SEPARATOR);
				}
				{
					this.selectDeviceMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.selectDeviceMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0030, GDE.MOD1));
					this.selectDeviceMenuItem.setImage(SWTResourceManager.getImage("gde/resource/DeviceSelectionHot.gif")); //$NON-NLS-1$
					this.selectDeviceMenuItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0030));
					this.selectDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "selectDeviceMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							DeviceSelectionDialog deviceSelection = MenuBar.this.application.getDeviceSelectionDialog();
							if (deviceSelection.checkDataSaved()) {
								deviceSelection.open();
							}
						}
					});
				}
				{
					this.prevDeviceMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.prevDeviceMenuItem.setImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldLefHot.gif")); //$NON-NLS-1$
					this.prevDeviceMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0031, GDE.MOD3));
					this.prevDeviceMenuItem.setAccelerator(SWT.MOD3 + SWT.PAGE_UP);
					this.prevDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "prevDeviceMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.application.getActiveDevice().getCommunicationPort() == null || !MenuBar.this.application.getActiveDevice().getCommunicationPort().isConnected()) { // allow device switch only if port noct connected
								DeviceConfiguration deviceConfig;
								DeviceSelectionDialog deviceSelect = MenuBar.this.application.getDeviceSelectionDialog();
								if (deviceSelect.checkDataSaved()) {
									int selection = deviceSelect.getDeviceConfigurations().indexOf(deviceSelect.getActiveConfig().getName());
									int size = deviceSelect.getDeviceConfigurations().size();
									if (selection > 0 && selection <= size) {
										deviceConfig = deviceSelect.getDeviceConfigurations().get(deviceSelect.getDeviceConfigurations().get(selection - 1));
									}
									else
										deviceConfig = deviceSelect.getDeviceConfigurations().get(deviceSelect.getDeviceConfigurations().get(size - 1));

									// if a device tool box is open, dispose it
									if (MenuBar.this.application.getDeviceDialog() != null && !MenuBar.this.application.getDeviceDialog().isDisposed()) {
										MenuBar.this.application.getDeviceDialog().dispose();
									}

									deviceSelect.setActiveConfig(deviceConfig);
									deviceSelect.setupDevice();
								}
							}
							else {
								MenuBar.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0002));
							}
						}
					});
				}
				{
					this.nextDeviceMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.nextDeviceMenuItem.setImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldRightHot.gif")); //$NON-NLS-1$
					this.nextDeviceMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0032, GDE.MOD3));
					this.nextDeviceMenuItem.setAccelerator(SWT.MOD3 + SWT.PAGE_DOWN);
					this.nextDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "nextDeviceMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.application.getActiveDevice().getCommunicationPort() == null || !MenuBar.this.application.getActiveDevice().getCommunicationPort().isConnected()) { // allow device switch only if port noct connected
								DeviceConfiguration deviceConfig;
								DeviceSelectionDialog deviceSelect = MenuBar.this.application.getDeviceSelectionDialog();
								if (deviceSelect.checkDataSaved()) {
									int selection = deviceSelect.getDeviceConfigurations().indexOf(deviceSelect.getActiveConfig().getName());
									int size = deviceSelect.getDeviceConfigurations().size() - 1;
									if (selection >= 0 && selection < size)
										deviceConfig = deviceSelect.getDeviceConfigurations().get(deviceSelect.getDeviceConfigurations().get(selection + 1));
									else
										deviceConfig = deviceSelect.getDeviceConfigurations().get(deviceSelect.getDeviceConfigurations().get(0));

									// if a device tool box is open, dispose it
									if (MenuBar.this.application.getDeviceDialog() != null && !MenuBar.this.application.getDeviceDialog().isDisposed()) {
										MenuBar.this.application.getDeviceDialog().dispose();
									}

									deviceSelect.setActiveConfig(deviceConfig);
									deviceSelect.setupDevice();
								}
							}
							else {
								MenuBar.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0002));
							}
						}
					});
				}
				{
					new MenuItem(this.deviceMenu, SWT.SEPARATOR);
				}
				{
					this.toolBoxDeviceMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.toolBoxDeviceMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0029, GDE.MOD1));
					this.toolBoxDeviceMenuItem.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
					this.toolBoxDeviceMenuItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0029));
					this.toolBoxDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "toolBoxDeviceMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.openDeviceDialog();
						}
					});
				}
				{
					this.devicePropertyFileEditMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.devicePropertyFileEditMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0465, GDE.MOD1));
					this.devicePropertyFileEditMenuItem.setImage(SWTResourceManager.getImage("gde/resource/EditHot.gif")); //$NON-NLS-1$
					this.devicePropertyFileEditMenuItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0465));
					this.devicePropertyFileEditMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "devicePropertyFileEditMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.getInstance().openAsDialog(MenuBar.this.application.getActiveDevice().getDeviceConfiguration());
						}
					});
				}
			}
		}
		{
			this.graphicsMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.graphicsMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0033));
			this.graphicsMenuItem.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.log(Level.FINE, "graphicsMenuItem.helpRequested, event=" + evt); //$NON-NLS-1$
					MenuBar.this.application.openHelpDialog(GDE.STRING_EMPTY, "HelpInfo_33.html"); //$NON-NLS-1$
				}
			});
			{
				this.graphicsMenu = new Menu(this.graphicsMenuItem);
				this.graphicsMenuItem.setMenu(this.graphicsMenu);
				{
					this.saveDefaultGraphicsTemplateItem = new MenuItem(this.graphicsMenu, SWT.PUSH);
					this.saveDefaultGraphicsTemplateItem.setText(Messages.getString(MessageIds.GDE_MSGT0034, GDE.MOD1));
					this.saveDefaultGraphicsTemplateItem.setAccelerator(SWT.MOD1 + SWT.SHIFT + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0034));
					this.saveDefaultGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "saveGraphicsTemplateItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.application.getHistoExplorer().map(h -> h.isHistoWindowVisible()).orElse(false) //
									&& MenuBar.this.application.getPresentHistoExplorer().hasRecords()) {
								TrailRecordSet trailRecordSet = MenuBar.this.application.getPresentHistoExplorer().getTrailRecordSet();
								trailRecordSet.getTemplate().setHistoFileName(trailRecordSet.getTemplate().getDefaultHistoFileName());
								trailRecordSet.saveTemplate();
							}
							else {
								MenuBar.this.channels.getActiveChannel().saveTemplate();
							}
						}
					});
				}
				{
					this.restoreDefaultGraphicsTemplateItem = new MenuItem(this.graphicsMenu, SWT.PUSH);
					this.restoreDefaultGraphicsTemplateItem.setText(Messages.getString(MessageIds.GDE_MSGT0195, GDE.MOD1));
					this.restoreDefaultGraphicsTemplateItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0195));
					if (!GDE.IS_OS_ARCH_ARM) this.restoreDefaultGraphicsTemplateItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0884));
					this.restoreDefaultGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "restoreDefaultGraphicsTemplateItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.application.getHistoExplorer().map(h -> h.isHistoWindowVisible()).orElse(false) //
									&& MenuBar.this.application.getPresentHistoExplorer().hasRecords()) {
								TrailRecordSet trailRecordSet = MenuBar.this.application.getPresentHistoExplorer().getTrailRecordSet();
								HistoGraphicsTemplate template = trailRecordSet.getTemplate();
								template.setHistoFileName(template.getDefaultHistoFileName());
								template.load();
								trailRecordSet.applyTemplate(true);
								MenuBar.this.application.getPresentHistoExplorer().updateHistoTabs(false, true, true);;
							}
							else {
								Channel activeChannel = MenuBar.this.channels.getActiveChannel();
								GraphicsTemplate template = activeChannel.getTemplate();
								template.setNewFileName(template.getDefaultFileName());
								MenuBar.log.log(Level.FINE, "templateFilePath = " + template.getDefaultFileName()); //$NON-NLS-1$
								template.load();
								if (activeChannel.getActiveRecordSet() != null) {
									activeChannel.applyTemplate(activeChannel.getActiveRecordSet().getName(), true);
									activeChannel.getActiveRecordSet().setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
								}
							}
						}
					});
				}
				{
					this.saveAsGraphicsTemplateItem = new MenuItem(this.graphicsMenu, SWT.PUSH);
					this.saveAsGraphicsTemplateItem.setText(Messages.getString(MessageIds.GDE_MSGT0035));
					this.saveAsGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "saveGraphicsTemplateItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							Settings.getInstance();
							MenuBar.log.log(Level.FINE, "templatePath = " + Settings.getGraphicsTemplatePath()); //$NON-NLS-1$
							if (MenuBar.this.application.getHistoExplorer().map(h -> h.isHistoWindowVisible()).orElse(false) //
									&& MenuBar.this.application.getPresentHistoExplorer().hasRecords()) {
								TrailRecordSet trailRecordSet = application.getPresentHistoExplorer().getTrailRecordSet();
								HistoGraphicsTemplate template = trailRecordSet.getTemplate();
								FileDialog fileDialog = MenuBar.this.application.prepareFileSaveDialog(Messages.getString(MessageIds.GDE_MSGT0036), new String[] {
										Settings.GRAPHICS_TEMPLATES_EXTENSION }, template.getTargetFileSubPath().getParent().toString(), template.getTargetFileSubPath().getFileName().toString());
								if (fileDialog.open() != null) {
									String templateFileName = fileDialog.getFileName();
									if (templateFileName != null && templateFileName.length() > 4) {
										MenuBar.log.log(Level.FINE, "templateFilePath = " + templateFileName); //$NON-NLS-1$
										trailRecordSet.getTemplate().setHistoFileName(templateFileName);
										trailRecordSet.saveTemplate();
										if (MenuBar.this.application.getPresentHistoExplorer().isHistoChartWindowVisible()) {
											MenuBar.this.application.getPresentHistoExplorer().getActiveHistoChartTabItem().resetStatusMessage();
										}
									}
								}
							} else {
								Channel activeChannel = MenuBar.this.channels.getActiveChannel();
								if (activeChannel != null) {
									GraphicsTemplate template = activeChannel.getTemplate();
									Settings.getInstance();
									FileDialog fileDialog = MenuBar.this.application.prepareFileSaveDialog(Messages.getString(MessageIds.GDE_MSGT0036), new String[] { Settings.GRAPHICS_TEMPLATES_EXTENSION },
											Settings.getGraphicsTemplatePath(), template.getDefaultFileName());
									fileDialog.open();
									String templateFileName = fileDialog.getFileName();
									if (templateFileName != null && templateFileName.length() > 4) {
										MenuBar.log.log(Level.FINE, "templateFilePath = " + templateFileName); //$NON-NLS-1$
										template.setNewFileName(templateFileName);
										activeChannel.saveTemplate();
									}
								}
							}
						}
					});
				}
				{
					this.restoreGraphicsTemplateItem = new MenuItem(this.graphicsMenu, SWT.PUSH);
					this.restoreGraphicsTemplateItem.setText(Messages.getString(MessageIds.GDE_MSGT0037));
					this.restoreGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "restoreGraphicsTemplateItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.application.getHistoExplorer().map(h -> h.isHistoWindowVisible()).orElse(false) //
									&& MenuBar.this.application.getPresentHistoExplorer().hasRecords()) {
								TrailRecordSet trailRecordSet = MenuBar.this.application.getPresentHistoExplorer().getTrailRecordSet();
								HistoGraphicsTemplate template = trailRecordSet.getTemplate();
								Path templatesPath = settings.isObjectTemplatesActive() //
										? settings.getActiveObjectKey().isEmpty() //
												? Paths.get(Settings.getGraphicsTemplatePath(), GDE.STRING_DEVICE_ORIENTED_FOLDER) //
												: Paths.get(Settings.getGraphicsTemplatePath(), settings.getActiveObjectKey()) //
										: Paths.get(Settings.getGraphicsTemplatePath());
								String filePath = MenuBar.this.application.prepareFileOpenDialog(Messages.getString(MessageIds.GDE_MSGT0038), new String[] {
										Settings.GRAPHICS_TEMPLATES_EXTENSION }, templatesPath.toString(), template.getTargetFileSubPath().getFileName().toString(), SWT.SINGLE) //
										.open();
								if (filePath != null) {
									Path templateFilePath = Paths.get(filePath);
									if (templateFilePath.getFileName().toString().length() > 4) {
										// allow loading whatever file the user requests
										template.loadAlien(templateFilePath);
										trailRecordSet.applyTemplate(true);
										MenuBar.this.application.getPresentHistoExplorer().updateHistoTabs(false, true, true);
									}
								}
							} else {
								FileDialog fileDialog = MenuBar.this.application.openFileOpenDialog(Messages.getString(MessageIds.GDE_MSGT0038), new String[] {
										Settings.GRAPHICS_TEMPLATES_EXTENSION }, Settings.getGraphicsTemplatePath(), null, SWT.SINGLE);
								String templateFileName = fileDialog.getFileName();
								if (templateFileName != null && templateFileName.length() > 4) {
									MenuBar.log.log(Level.FINE, "templateFilePath = " + templateFileName); //$NON-NLS-1$
									Channel activeChannel = MenuBar.this.channels.getActiveChannel();
									GraphicsTemplate template = activeChannel.getTemplate();
									template.setNewFileName(templateFileName);
									template.load();
									if (activeChannel.getActiveRecordSet() != null) {
										activeChannel.applyTemplate(activeChannel.getActiveRecordSet().getName(), true);
										activeChannel.getActiveRecordSet().setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
									}
								}
							}
						}
					});
				}
				{
					new MenuItem(this.graphicsMenu, SWT.SEPARATOR);
				}
				{
					this.objectTemplatesItem = new MenuItem(this.graphicsMenu, SWT.CHECK);
					this.objectTemplatesItem.setText(Messages.getString(MessageIds.GDE_MSGT0920));
					if (!GDE.IS_OS_ARCH_ARM) this.objectTemplatesItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0921));
					this.objectTemplatesItem.setSelection(MenuBar.this.settings.isObjectTemplatesActive());
					this.objectTemplatesItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "objectTemplatesItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.settings.setObjectTemplatesActive(MenuBar.this.objectTemplatesItem.getSelection());
							if (MenuBar.this.application.getHistoExplorer().map(h -> h.isHistoWindowVisible()).orElse(false)) {
								MenuBar.this.application.getPresentHistoExplorer().resetHisto();
							} else {
								// not implemented for kernel graphics
							}
						}
					});
				}
				{
					this.smartStatisticsItem = new MenuItem(this.graphicsMenu, SWT.CHECK);
					this.smartStatisticsItem.setText(Messages.getString(MessageIds.GDE_MSGT0885, GDE.MOD1));
					if (!GDE.IS_OS_ARCH_ARM) this.smartStatisticsItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0886, GDE.MOD1));
					this.smartStatisticsItem.setAccelerator(SWT.MOD1 + SWT.SHIFT + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0885));
					this.smartStatisticsItem.setSelection(false);
					this.smartStatisticsItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "smartStatisticsItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.application.getHistoExplorer().map(h -> h.isHistoWindowVisible()).orElse(false) //
									&& MenuBar.this.application.getPresentHistoExplorer().hasRecords()) {
								TrailRecordSet trailRecordSet = MenuBar.this.application.getPresentHistoExplorer().getTrailRecordSet();
								trailRecordSet.setSmartStatistics(MenuBar.this.smartStatisticsItem.getSelection());
								trailRecordSet.getTemplate().store();
								MenuBar.this.application.getPresentHistoExplorer().updateHistoTabs(false, true, true);
							} else {
								// not implemented for kernel graphics
								MenuBar.this.objectTemplatesItem.setSelection(false);
							}
						}
					});
				}
			}
		}
		{
			this.viewMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.viewMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0039));
			this.viewMenuItem.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.log(Level.FINE, "viewMenuItem.helpRequested, event=" + evt); //$NON-NLS-1$
					MenuBar.this.application.openHelpDialog(GDE.STRING_EMPTY, "HelpInfo_34.html"); //$NON-NLS-1$
				}
			});
			{
				this.viewMenu = new Menu(this.viewMenuItem);
				this.viewMenuItem.setMenu(this.viewMenu);
				{
					this.suppressModeItem = new MenuItem(this.viewMenu, SWT.CHECK);
					this.suppressModeItem.setText(Messages.getString(MessageIds.GDE_MSGT0859));
					this.suppressModeItem.setSelection(MenuBar.this.settings.isSuppressMode());
					this.suppressModeItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "suppressModeItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.settings.setSuppressMode(MenuBar.this.suppressModeItem.getSelection());

							MenuBar.this.application.getHistoExplorer().ifPresent(h -> h.resetHisto());
						}
					});
				}
				{
					this.curveSelectionMenuItem = new MenuItem(this.viewMenu, SWT.CHECK);
					this.curveSelectionMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0040));
					this.curveSelectionMenuItem.setSelection(true);
					this.curveSelectionMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "kurveSelectionMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.enableCurveSelector(MenuBar.this.curveSelectionMenuItem.getSelection());
						}
					});
				}
				{
					this.graphicsHeaderMenuItem = new MenuItem(this.viewMenu, SWT.CHECK);
					this.graphicsHeaderMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0041));
					this.graphicsHeaderMenuItem.setSelection(false);
					this.graphicsHeaderMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicsHeaderMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.graphicsHeaderMenuItem.getSelection()) {
								MenuBar.this.application.enableGraphicsHeader(true);
								MenuBar.this.application.updateDisplayTab();
							}
							else {
								MenuBar.this.application.enableGraphicsHeader(false);
								MenuBar.this.application.updateDisplayTab();
							}
						}
					});
				}
				{
					this.recordCommentMenuItem = new MenuItem(this.viewMenu, SWT.CHECK);
					this.recordCommentMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0042));
					if (!GDE.IS_OS_ARCH_ARM) this.recordCommentMenuItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0878));
					this.recordCommentMenuItem.setSelection(false);
					this.recordCommentMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "recordCommentMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.recordCommentMenuItem.getSelection()) {
								MenuBar.this.application.enableRecordSetComment(true);
								MenuBar.this.application.updateDisplayTab();
							}
							else {
								MenuBar.this.application.enableRecordSetComment(false);
								MenuBar.this.application.updateDisplayTab();
							}
						}
					});
				}
				{
					this.graphicsCurveSurveyMenuItem = new MenuItem(this.viewMenu, SWT.CHECK);
					this.graphicsCurveSurveyMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0876));
					if (!GDE.IS_OS_ARCH_ARM) this.graphicsCurveSurveyMenuItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0877));
					this.graphicsCurveSurveyMenuItem.setSelection(settings.isCurveSurvey());
					this.graphicsCurveSurveyMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicsCurveSurvey.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.getHistoExplorer().ifPresent(h -> {
								h.enableCurveSurvey(MenuBar.this.graphicsCurveSurveyMenuItem.getSelection());
								h.updateHistoChartWindow(MenuBar.this.graphicsCurveSurveyMenuItem.getSelection());
							});
						}
					});
				}
				{
					this.partialTableMenuItem = new MenuItem(this.viewMenu, SWT.CHECK);
					this.partialTableMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0704));
					if (!GDE.IS_OS_ARCH_ARM) this.partialTableMenuItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0705));
					this.partialTableMenuItem.setSelection(settings.isPartialDataTable());
					this.partialTableMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "partialTableMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.getHistoExplorer().ifPresent(h -> {
								h.enablePartialDataTable(MenuBar.this.partialTableMenuItem.getSelection());
								h.updateHistoChartWindow(MenuBar.this.graphicsCurveSurveyMenuItem.getSelection());
								h.updateHistoTableWindow(true);
							});
						}
					});
				}
				{
					new MenuItem(this.viewMenu, SWT.SEPARATOR);
				}
				{
					this.prevTabConfigItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.prevTabConfigItem.setText(Messages.getString(MessageIds.GDE_MSGT0438, GDE.MOD1));
					this.prevTabConfigItem.setAccelerator(SWT.MOD1 + SWT.PAGE_UP);
					this.prevTabConfigItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "prevTabConfigItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.switchPreviousTabulator();
						}
					});
				}
				{
					this.nextTabConfigItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.nextTabConfigItem.setText(Messages.getString(MessageIds.GDE_MSGT0439, GDE.MOD1));
					this.nextTabConfigItem.setAccelerator(SWT.MOD1 + SWT.PAGE_DOWN);
					this.nextTabConfigItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "nextTabConfigItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.switchNextTabulator();
						}
					});
				}
				{
					new MenuItem(this.viewMenu, SWT.SEPARATOR);
				}
				{
					this.prevChannelConfigItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.prevChannelConfigItem.setText(Messages.getString(MessageIds.GDE_MSGT0663, GDE.MOD1));
					this.prevChannelConfigItem.setAccelerator(SWT.MOD1 + SWT.ARROW_LEFT);
					this.prevChannelConfigItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "prevChannelConfigItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.getMenuToolBar().prevChannel.notifyListeners(SWT.Selection, new Event());
						}
					});
				}
				{
					this.nextChannelConfigItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.nextChannelConfigItem.setText(Messages.getString(MessageIds.GDE_MSGT0664, GDE.MOD1));
					this.nextChannelConfigItem.setAccelerator(SWT.MOD1 + SWT.ARROW_RIGHT);
					this.nextChannelConfigItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "nextChannelConfigItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.getMenuToolBar().nextChannel.notifyListeners(SWT.Selection, new Event());
						}
					});
				}
				{
					new MenuItem(this.viewMenu, SWT.SEPARATOR);
				}
				{
					this.prevRecordSetItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.prevRecordSetItem.setText(Messages.getString(MessageIds.GDE_MSGT0665, GDE.MOD3));
					this.prevRecordSetItem.setAccelerator(SWT.MOD3 + SWT.ARROW_LEFT);
					this.prevRecordSetItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "prevRecordSetItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.getMenuToolBar().prevRecord.notifyListeners(SWT.Selection, new Event());
						}
					});
				}
				{
					this.nextRecordSetItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.nextRecordSetItem.setText(Messages.getString(MessageIds.GDE_MSGT0666, GDE.MOD3));
					this.nextRecordSetItem.setAccelerator(SWT.MOD3 + SWT.ARROW_RIGHT);
					this.nextRecordSetItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "nextRecordSetItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.getMenuToolBar().nextRecord.notifyListeners(SWT.Selection, new Event());
						}
					});
				}
				{
					new MenuItem(this.viewMenu, SWT.SEPARATOR);
				}
				{
					this.nextChartItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.nextChartItem.setText(Messages.getString(MessageIds.GDE_MSGT0900, GDE.MOD1));
					if (!GDE.IS_OS_ARCH_ARM) this.nextChartItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0901));
					this.nextChartItem.setAccelerator(SWT.MOD1 + SWT.SHIFT + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0900));
					this.nextChartItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "nextChartItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.getHistoExplorer().ifPresent(h -> {
								h.scrollSummaryComposite();
								h.updateHistoChartWindow(false);
							});
						}
					});
				}
				{
					this.refreshItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.refreshItem.setText(Messages.getString(MessageIds.GDE_MSGT0925, GDE.MOD1));
					if (!GDE.IS_OS_ARCH_ARM) this.refreshItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0926));
					this.refreshItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT0925));
					this.refreshItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "refreshItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.getHistoExplorer().ifPresent(h -> {
								h.resetHisto();
							});
						}
					});
				}
			}
		}
		{
			this.helpMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.helpMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0043));
			{
				this.helpMenu = new Menu(this.helpMenuItem);
				this.helpMenuItem.setMenu(this.helpMenu);
				{
					this.contentsMenuItem = new MenuItem(this.helpMenu, SWT.PUSH);
					this.contentsMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0044));
					this.contentsMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "contentsMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.application.getActiveDevice() != null && MenuBar.this.application.getActiveDevice().getDialog() != null
									&& !MenuBar.this.application.getActiveDevice().getDialog().isDisposed()) {
								MenuBar.this.application.getActiveDevice().getDialog().getDialogShell().getShell().notifyListeners(SWT.Help, new Event());
							}
							else {
								for (CTabItem tabItem : MenuBar.this.application.getTabFolder().getItems()) {
									if (!tabItem.isDisposed() && tabItem.getControl().isVisible()) {
										if (tabItem.getControl().isListening(SWT.Help)) {
											tabItem.getControl().notifyListeners(SWT.Help, new Event());
											break;
										}
										else if (tabItem instanceof GraphicsWindow) {
											((GraphicsWindow) tabItem).getGraphicsComposite().notifyListeners(SWT.Help, new Event());
										}
										else if (tabItem.getText().endsWith("Tool")) { //DataVarioTool, LinkVarioTool //$NON-NLS-1$
											if (MenuBar.this.application.getActiveDevice() != null && MenuBar.this.application.getActiveDevice().isUtilityDeviceTabRequested()) {
												MenuBar.this.application.openHelpDialog("WStechVario", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
											}
										}
									}
								}
							}
						}
					});
				}
				{
					this.webCheckMenuItem = new MenuItem(this.helpMenu, SWT.PUSH);
					this.webCheckMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0045));
					this.webCheckMenuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "webCheckMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.openWebBrowser(Messages.getString(MessageIds.GDE_MSGT0046));
						}
					});
				}
				if (!GDE.IS_MAC) {
					{
						this.aboutMenuItem = new MenuItem(this.helpMenu, SWT.PUSH);
						this.aboutMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0047));
						this.aboutMenuItem.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "aboutMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
								MenuBar.this.application.openAboutDialog();
							}
						});
					}
				}
			}
		}

		Menu systemMenu = GDE.display.getSystemMenu();
		if (systemMenu != null) {
			/* remove comment for further logging
			systemMenu.addMenuListener(new MenuListener() {
				@Override
				public void menuHidden(MenuEvent e) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "App menu closed"); //$NON-NLS-1$
				}

				@Override
				public void menuShown(MenuEvent e) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "App menu opened"); //$NON-NLS-1$
				}
			});
			*/

			MenuItem sysItem = getItem(systemMenu, SWT.ID_QUIT);
			// sysItem.addArmListener(armListener);
			sysItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					closeApplication(e);
				};
			});
			sysItem = getItem(systemMenu, SWT.ID_HIDE_OTHERS);
			// sysItem.addArmListener(armListener);
			sysItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "Hide others selected -- and blocked!"); //$NON-NLS-1$
					e.doit = false;
				};
			});
			sysItem = getItem(systemMenu, SWT.ID_HIDE);
			// sysItem.addArmListener(armListener);
			sysItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "Hide selected -- and blocked!"); //$NON-NLS-1$
					e.doit = false;
				};
			});
			sysItem = getItem(systemMenu, SWT.ID_PREFERENCES);
			// sysItem.addArmListener(armListener);
			sysItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					openPreferencesDialog(e);
				};
			});
			sysItem = getItem(systemMenu, SWT.ID_ABOUT);
			// sysItem.addArmListener(armListener);
			sysItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "aboutMenuItem.widgetSelected, event=" + e); //$NON-NLS-1$
					MenuBar.this.application.openAboutDialog();
				};
			});
		}
	}

	/**
	 * update file history while add history file to history menu
	 * @param fullQualifiedFileName (/home/device/filename.osd)
	 */
	public void updateSubHistoryMenuItem(String fullQualifiedFileName) {
		List<String> refFileHistory = Settings.getInstance().getFileHistory();
		if (fullQualifiedFileName != null && fullQualifiedFileName.length() > 4) {
			final String newhistoryEntry = fullQualifiedFileName.replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX);

			if (refFileHistory.indexOf(newhistoryEntry) > -1) { // fileName already exist
				refFileHistory.remove(newhistoryEntry);
			}
			refFileHistory.add(0, newhistoryEntry);
		}
		// clean up the menu entries
		MenuItem[] menuItems = this.fileHistoryMenu.getItems();
		for (MenuItem menuItem : menuItems) {
			menuItem.dispose();
		}
		// fill with refreshed data
		int i = 0;
		for (String fullQualifiedFileReference : refFileHistory) {
			i++;
			// add number in front of history item for selection with keyboard
			String shortFileReference = "&" + i + ": " + fullQualifiedFileReference.substring(fullQualifiedFileReference.lastIndexOf('/') + 1);
			final MenuItem historyImportMenuItem = new MenuItem(this.fileHistoryMenu, SWT.PUSH);
			historyImportMenuItem.setText(shortFileReference);
			historyImportMenuItem.setData(shortFileReference, fullQualifiedFileReference);
			historyImportMenuItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "historyImportMenuItem.widgetSelected, event=" + evt);//$NON-NLS-1$
					String fileName = (String) historyImportMenuItem.getData(historyImportMenuItem.getText());
					String fileType = fileName.substring(fileName.lastIndexOf('.') + 1);
					if (fileType != null && fileType.length() > 2) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "opening file = " + fileName);//$NON-NLS-1$

						// check if the file still exists
						File file = new File(fileName);
						if (!file.exists()) {
							int answer = MenuBar.this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGW0046, new Object[] { file.getAbsolutePath() }));
							if (answer == SWT.YES) {
								// remove from history
								Settings.getInstance().getFileHistory().remove(fileName);
								updateSubHistoryMenuItem(null);
							}
						}
						else {
							// open file from history
							MenuBar.log.log(Level.FINE, "opening file = " + fileName);//$NON-NLS-1$
							if (fileType.equalsIgnoreCase(GDE.FILE_ENDING_OSD)) {
								MenuBar.this.fileHandler.openOsdFile(fileName);
							}
							else if (fileType.equalsIgnoreCase(GDE.FILE_ENDING_LOV)) {
								MenuBar.this.fileHandler.openLovFile(fileName);
							}
							else {
								MenuBar.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0003));
							}
						}
					}
				}
			});
		}
	}

	/**
	 * this function must only called by application which make secure to choose the right thread
	 * @param isOpenStatus
	 */
	public void setPortConnected(final boolean isOpenStatus) {
		if (!this.application.isDisposed() && !this.portMenuItem.isDisposed()) {
			switch (this.iconSet) {
			case 0: // DeviceSerialPort.ICON_SET_OPEN_CLOSE
			default:
				if (isOpenStatus) {
					this.portMenuItem.setImage(SWTResourceManager.getImage("gde/resource/BulletHotGreen.gif")); //$NON-NLS-1$
					this.portMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0048));
				}
				else {
					this.portMenuItem.setImage(SWTResourceManager.getImage("gde/resource/BulletHotRed.gif")); //$NON-NLS-1$
					this.portMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0049));
				}
				break;
			case 1: // DeviceSerialPort.ICON_SET_START_STOP
				if (isOpenStatus) {
					this.portMenuItem.setImage(SWTResourceManager.getImage("gde/resource/RectangleHotRed.gif")); //$NON-NLS-1$
					this.portMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0071));
				}
				else {
					this.portMenuItem.setImage(SWTResourceManager.getImage("gde/resource/TriangleGreen.gif")); //$NON-NLS-1$
					this.portMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0070));
				}
				break;
			case 2: // DeviceSerialPort.ICON_SET_IMPORT_CLOSE
				if (isOpenStatus) {
					this.portMenuItem.setImage(SWTResourceManager.getImage("gde/resource/RectangleHotRed.gif")); //$NON-NLS-1$
					this.portMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0219));
				}
				else {
					this.portMenuItem.setImage(SWTResourceManager.getImage("gde/resource/OpenHot.gif")); //$NON-NLS-1$
					this.portMenuItem.setText(Messages.getString(MessageIds.GDE_MSGT0218));
				}
				break;
			}
		}
	}

	/**
	 * method to switch icon set by active device
	 * @param newIconSet
	 */
	public void setSerialPortIconSet(int newIconSet) {
		this.iconSet = newIconSet;
		this.setPortConnected(false);
	}

	/**
	 * method to query icon set by active device
	 */
	public int getSerialPortIconSet() {
		return this.iconSet;
	}

	/**
	 * set selection of record comment window
	 * @param selected
	 */
	public void setRecordCommentMenuItemSelection(boolean selected) {
		this.recordCommentMenuItem.setSelection(selected);
	}

	/**
	 * set histo graphics CurveSurvey display
	 * @param selected
	 */
	public void setCurveSurveyMenuItemSelection(boolean selected) {
		this.graphicsCurveSurveyMenuItem.setSelection(selected);
	}

	/**
	 * set selection of record comment window
	 * @param selected
	 */
	public void setGraphicsHeaderMenuItemSelection(boolean selected) {
		this.graphicsHeaderMenuItem.setSelection(selected);
	}

	/**
	 * set the state of device switch menu
	 * @param enabled
	 */
	public void enableDeviceSwitchButtons(boolean enabled) {
		this.prevDeviceMenuItem.setEnabled(enabled);
		this.nextDeviceMenuItem.setEnabled(enabled);
	}

	/**
	 * set the state of zoom menu buttons
	 * @param enabled
	 */
	public void enableZoomMenuButtons(boolean enabled) {
		this.activateZoomGraphicMenuItem.setEnabled(enabled);
		this.resetZoomGraphicMenuItem.setEnabled(enabled);
	}

	/**
	 * @return the file menu for update purpose
	 */
	public Menu getExportMenu() {
		return this.exportMenu;
	}

	/**
	 * @return the file menu for update purpose
	 */
	public Menu getImportMenu() {
		return this.importMenu;
	}

	/**
	 * remove menu entries not any longer required
	 */
	public void cleanup() {
		// cleanup exportMenu for device specific entries
		for (int i = this.exportMenu.getItemCount() - 1; this.exportMenu.getItemCount() > 3; i--) {
			this.exportMenu.getItem(i).dispose();
		}
		for (int i = this.importMenu.getItemCount() - 1; this.importMenu.getItemCount() > 2; i--) {
			this.importMenu.getItem(i).dispose();
		}
	}

	/**
	 * enable pan button in zoomed mode
	 * @param enable
	 */
	public void enablePanButton(boolean enable) {
		this.panGraphicMenuItem.setEnabled(enable);
	}

	/**
	 * toggle enabling additional export menu items in dependency of device data capability
	 */
	public void updateAdditionalGPSMenuItems() {
		final boolean isGPSData = this.application.getActiveDevice().isActualRecordSetWithGpsData();
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			boolean isAdditionalExportItem = false;
			for (MenuItem menuItem : this.exportMenu.getItems()) {
				if (menuItem.getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
					isAdditionalExportItem = true;
				}
				else {
					if (isAdditionalExportItem) menuItem.setEnabled(isGPSData);
				}
			}
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					boolean isAdditionalExportItem = false;
					for (MenuItem menuItem : MenuBar.this.exportMenu.getItems()) {
						if (menuItem.getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
							isAdditionalExportItem = true;
						}
						else {
							if (isAdditionalExportItem) menuItem.setEnabled(isGPSData);
						}
					}
				}
			});
		}
	}

	private void openPreferencesDialog(SelectionEvent evt) {
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "preferencesFileMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
		// check if other none modal dialog is open
		DeviceDialog deviceDialog = MenuBar.this.application.getDeviceDialog();
		if (deviceDialog == null || deviceDialog.isDisposed()) {
			MenuBar.this.application.openSettingsDialog();
			MenuBar.this.application.setStatusMessage(GDE.STRING_EMPTY);
		}
		else
			MenuBar.this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI0001), SWT.COLOR_RED);
	}

	private void closeApplication(SelectionEvent evt) {
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "exitMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
		
		// check all data saved - prevent closing application
		evt.doit = MenuBar.this.application.getDeviceSelectionDialog().checkDataSaved();
		
		//check for device initiated running thread like data gathering, skip application shutdown
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread != null && !thread.isDaemon() && thread.isAlive() && thread.getClass().getName().startsWith("gde.device")) {
				MenuBar.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGW0048));
				evt.doit = false;
			}
		}

		if (evt.doit)
			MenuBar.this.parent.getParent().dispose();
	}

	ArmListener armListener = new ArmListener() {
		@Override
		public void widgetArmed(ArmEvent e) {
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "widgetArmed, event=" + e); //$NON-NLS-1$
		}
	};
	static MenuItem getItem(Menu menu, int id) {
		MenuItem[] items = menu.getItems();
		for (MenuItem item : items) {
			if (item.getID() == id) return item;
		}
		return null;
	}

	public MenuItem getSuppressModeItem() {
		return suppressModeItem;
	}

	public void setSuppressModeItem(MenuItem suppressModeItem) {
		this.suppressModeItem = suppressModeItem;
	}

	public MenuItem getCurveSelectionMenuItem() {
		return curveSelectionMenuItem;
	}

	public void setCurveSelectionMenuItem(MenuItem curveSelectionMenuItem) {
		this.curveSelectionMenuItem = curveSelectionMenuItem;
	}

	public MenuItem getGraphicsHeaderMenuItem() {
		return graphicsHeaderMenuItem;
	}

	public void setGraphicsHeaderMenuItem(MenuItem graphicsHeaderMenuItem) {
		this.graphicsHeaderMenuItem = graphicsHeaderMenuItem;
	}

	public MenuItem getRecordCommentMenuItem() {
		return recordCommentMenuItem;
	}

	public void setRecordCommentMenuItem(MenuItem recordCommentMenuItem) {
		this.recordCommentMenuItem = recordCommentMenuItem;
	}

	public MenuItem getGraphicsCurveSurveyMenuItem() {
		return graphicsCurveSurveyMenuItem;
	}

	public void setGraphicsCurveSurveyMenuItem(MenuItem graphicsCurveSurveyMenuItem) {
		this.graphicsCurveSurveyMenuItem = graphicsCurveSurveyMenuItem;
	}

	public MenuItem getPartialTableMenuItem() {
		return partialTableMenuItem;
	}

	public void setPartialTableMenuItem(MenuItem partialTableMenuItem) {
		this.partialTableMenuItem = partialTableMenuItem;
	}

	public MenuItem getSmartStatisticsItem() {
		return this.smartStatisticsItem;
	}
}
