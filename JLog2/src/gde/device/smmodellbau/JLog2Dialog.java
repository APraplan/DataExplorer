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
    
    Copyright (c) 2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.device.kontronik.Kosmik;
import gde.device.kontronik.KosmikConfiguration;
import gde.device.smmodellbau.jlog2.MessageIds;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog class to enable visualization control
 * @author Winfried Brügmann
 */
public class JLog2Dialog extends DeviceDialog {
	final static Logger			log									= Logger.getLogger(JLog2Dialog.class.getName());

	CTabFolder							tabFolder, subTabFolder1, subTabFolder2;
	CTabItem								visualizationTabItem;
	CTabItem								configurationTabItem;
	Composite								visualizationMainComposite, uniLogVisualization, mLinkVisualization;
	Composite								configurationMainComposite;

	Button									saveVisualizationButton, inputFileButton, helpButton, liveGathererButton, closeButton;

	JLog2LiveGathererThread	liveThread;

	final IDevice						device;																																								// get device specific things, get serial port, ...
	final Settings					settings;																																							// application configuration settings
	final JLog2SerialPort		serialPort;																																						// open/close port execute getData()....
	String									selectedSetupFile;
	String									selectedVersionFile;

	RecordSet								lastActiveRecordSet	= null;
	boolean									isVisibilityChanged	= false;
	boolean									isConfigChanged			= false;
	int											measurementsCount		= 0;
	final List<CTabItem>		configurations			= new ArrayList<CTabItem>();

	boolean									isJLog2							= true;

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public JLog2Dialog(Shell parent, JLog2 useDevice) {
		super(parent);
		this.device = useDevice;
		this.serialPort = useDevice.getCommunicationPort();
		this.settings = Settings.getInstance();
		this.isJLog2 = this.device.getClass().getSimpleName().equals("JLog2");
		this.measurementsCount = this.isJLog2 ? 40 : this.device.getNumberOfMeasurements(this.device.getLastChannelNumber());
	}

	@Override
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			JLog2Dialog.log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);

				FormLayout dialogShellLayout = new FormLayout();
				this.dialogShell.setLayout(dialogShellLayout);
				this.dialogShell.layout();
				this.dialogShell.pack();
				this.dialogShell.setSize(GDE.IS_LINUX ? 740 : 700, 30 + 25 + 25 + (this.measurementsCount + 1) / 2 * 26 + 50 + 45); //header + tab + label + this.measurementsCount * 26 + buttons
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
					public void handleEvent(Event event) {
						switch (event.detail) {
						case SWT.TRAVERSE_ESCAPE:
							JLog2Dialog.this.dialogShell.close();
							event.detail = SWT.TRAVERSE_NONE;
							event.doit = false;
							break;
						}
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						JLog2Dialog.log.log(Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (JLog2Dialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { JLog2Dialog.this.device.getPropertiesFileName() });
							if (JLog2Dialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								JLog2Dialog.log.log(Level.FINE, "SWT.YES"); //$NON-NLS-1$
								JLog2Dialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
						JLog2Dialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						JLog2Dialog.log.log(Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						JLog2Dialog.this.application.openHelpDialog("JLog2", JLog2Dialog.this.isJLog2 ? "HelpInfo.html" : "HelpInfo2.html", true); //$NON-NLS-1$
					}
				});
				this.dialogShell.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent paintevent) {
						if (JLog2Dialog.log.isLoggable(Level.FINEST)) JLog2Dialog.log.log(Level.FINEST, "dialogShell.paintControl, event=" + paintevent); //$NON-NLS-1$
						RecordSet activeRecordSet = JLog2Dialog.this.application.getActiveRecordSet();
						if (JLog2Dialog.this.lastActiveRecordSet == null && activeRecordSet != null
								|| (activeRecordSet != null && !JLog2Dialog.this.lastActiveRecordSet.getName().equals(activeRecordSet.getName()))) {
							JLog2Dialog.this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
						}
						JLog2Dialog.this.lastActiveRecordSet = JLog2Dialog.this.application.getActiveRecordSet();
					}
				});
				{
					this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
					this.tabFolder.setSimple(false);
					{
						for (int i = 1; i <= this.device.getChannelCount(); i++) {
							createVisualizationTabItem(i, this.isJLog2 ? 31 : this.device.getNumberOfMeasurements(i));
						}
					}
					{
						this.configurationTabItem = new CTabItem(this.tabFolder, SWT.NONE);
						this.configurationTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 1 : 0), SWT.NORMAL));
						this.configurationTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2814));
						this.configurationTabItem.setControl((this.isJLog2 ? new JLog2Configuration(this.tabFolder, SWT.NONE, this, (JLog2) this.device) : new KosmikConfiguration(this.tabFolder, SWT.NONE, this,
								(Kosmik) this.device)));
					}
					FormData tabFolderLData = new FormData();
					tabFolderLData.top = new FormAttachment(0, 1000, 0);
					tabFolderLData.left = new FormAttachment(0, 1000, 0);
					tabFolderLData.right = new FormAttachment(1000, 1000, 0);
					tabFolderLData.bottom = new FormAttachment(1000, 1000, -50);
					this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tabFolder.setLayoutData(tabFolderLData);
					this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
					this.tabFolder.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							if (JLog2Dialog.this.isJLog2) {
								if (JLog2Dialog.this.tabFolder.getSelectionIndex() == JLog2Dialog.this.tabFolder.getItemCount() - 1) {
									loadSetup();
									JLog2Dialog.this.liveGathererButton.setEnabled(false);
									JLog2Dialog.this.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2803));
									JLog2Dialog.this.liveGathererButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2815));
								}
								else {
									if (JLog2Dialog.this.isConfigChanged) {
										if (SWT.YES == JLog2Dialog.this.application.openYesNoMessageDialog(JLog2Dialog.this.getDialogShell(), Messages.getString(MessageIds.GDE_MSGW2802))) {
											saveSetup();
										}
										JLog2Dialog.this.isConfigChanged = false;
									}
									JLog2Dialog.this.liveGathererButton.setEnabled(true);
									JLog2Dialog.this.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2805));
									JLog2Dialog.this.liveGathererButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2807));
								}
							}
						}
					});
				}
				{
					this.saveVisualizationButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData saveButtonLData = new FormData();
					saveButtonLData.width = 130;
					saveButtonLData.height = GDE.IS_MAC ? 33 : 30;
					saveButtonLData.left = new FormAttachment(0, 1000, 15);
					saveButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.saveVisualizationButton.setLayoutData(saveButtonLData);
					this.saveVisualizationButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.saveVisualizationButton.setText(Messages.getString(MessageIds.GDE_MSGT2810));
					this.saveVisualizationButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2811));
					this.saveVisualizationButton.setEnabled(false);
					this.saveVisualizationButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Dialog.log.log(Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Dialog.this.device.storeDeviceProperties();
							JLog2Dialog.this.saveVisualizationButton.setEnabled(false);
						}
					});
				}
				{
					this.inputFileButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData inputFileButtonLData = new FormData();
					inputFileButtonLData.width = 130;
					inputFileButtonLData.height = GDE.IS_MAC ? 33 : 30;
					inputFileButtonLData.left = new FormAttachment(0, 1000, 155);
					inputFileButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.inputFileButton.setLayoutData(inputFileButtonLData);
					this.inputFileButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.inputFileButton.setText(Messages.getString(MessageIds.GDE_MSGT2812));
					this.inputFileButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2813));
					this.inputFileButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Dialog.log.log(Level.FINEST, "inputFileButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (JLog2Dialog.this.isVisibilityChanged) {
								String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { JLog2Dialog.this.device.getPropertiesFileName() });
								if (JLog2Dialog.this.application.openYesNoMessageDialog(JLog2Dialog.this.dialogShell, msg) == SWT.YES) {
									JLog2Dialog.log.log(Level.FINE, "SWT.YES"); //$NON-NLS-1$
									JLog2Dialog.this.device.storeDeviceProperties();
								}
							}
							JLog2Dialog.this.device.open_closeCommPort();
						}
					});
				}
				{
					this.helpButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData helpButtonLData = new FormData();
					helpButtonLData.width = GDE.IS_LINUX ? 70 : 65;
					helpButtonLData.height = GDE.IS_MAC ? 33 : 30;
					helpButtonLData.left = new FormAttachment(0, 1000, GDE.IS_LINUX ? 332 : 302);
					helpButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.helpButton.setLayoutData(helpButtonLData);
					this.helpButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.helpButton.setImage(SWTResourceManager.getImage("gde/resource/QuestionHot.gif")); //$NON-NLS-1$
					this.helpButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Dialog.log.log(Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Dialog.this.application.openHelpDialog("JLog2", JLog2Dialog.this.isJLog2 ? "HelpInfo.html" : "HelpInfo2.html", true); //$NON-NLS-1$
						}
					});
				}
				{
					this.liveGathererButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData saveSetupButtonLData = new FormData();
					saveSetupButtonLData.width = 130;
					saveSetupButtonLData.height = GDE.IS_MAC ? 33 : 30;
					saveSetupButtonLData.right = new FormAttachment(1000, 1000, -155);
					saveSetupButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.liveGathererButton.setLayoutData(saveSetupButtonLData);
					this.liveGathererButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.liveGathererButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2807));
					if (this.isJLog2) {
						this.liveGathererButton.setEnabled(true);
						this.liveGathererButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								if (JLog2Dialog.log.isLoggable(Level.FINE)) JLog2Dialog.log.log(Level.FINE, "liveGathererButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (!JLog2Dialog.this.configurationTabItem.getControl().isVisible()) {
									if (JLog2Dialog.this.liveThread == null || !JLog2Dialog.this.serialPort.isConnected()) {
										try {
											JLog2Dialog.this.liveThread = new JLog2LiveGathererThread(JLog2Dialog.this.application, (JLog2) JLog2Dialog.this.device, JLog2Dialog.this.serialPort,
													JLog2Dialog.this.application.getActiveChannelNumber(), JLog2Dialog.this);
											try {
												JLog2Dialog.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT2806), Messages.getString(MessageIds.GDE_MSGT2806));
												JLog2Dialog.this.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2806));
												JLog2Dialog.this.liveThread.start();
											}
											catch (RuntimeException e) {
												JLog2Dialog.log.log(Level.WARNING, e.getMessage(), e);
											}
										}
										catch (Exception e) {
											if (JLog2Dialog.this.liveThread != null && JLog2Dialog.this.liveThread.isAlive()) {
												JLog2Dialog.this.liveThread.stopDataGathering();
												JLog2Dialog.this.liveThread.interrupt();
											}
											JLog2Dialog.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2804), Messages.getString(MessageIds.GDE_MSGT2804));
											JLog2Dialog.this.application.updateGraphicsWindow();
											JLog2Dialog.this.application.openMessageDialog(JLog2Dialog.this.getDialogShell(),
													Messages.getString(MessageIds.GDE_MSGW2801, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
											JLog2Dialog.this.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2805));
											JLog2Dialog.this.liveThread = null;
										}
									}
									else {
										if (JLog2Dialog.this.liveThread != null && JLog2Dialog.this.liveThread.isAlive()) {
											JLog2Dialog.this.liveThread.stopDataGathering();
											JLog2Dialog.this.liveThread.interrupt();
										}
										JLog2Dialog.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2804), Messages.getString(MessageIds.GDE_MSGT2804));
										JLog2Dialog.this.application.updateGraphicsWindow();
										JLog2Dialog.this.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2805));
										//JLog2Dialog.this.liveThread = null;
									}
								}
								else {
									//configuration
									saveSetup();
								}
							}
						});
					}
					else {
						this.liveGathererButton.setEnabled(false);
					}
				}
				{
					this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData closeButtonLData = new FormData();
					closeButtonLData.width = 130;
					closeButtonLData.height = GDE.IS_MAC ? 33 : 30;
					closeButtonLData.right = new FormAttachment(1000, 1000, -10);
					closeButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.closeButton.setLayoutData(closeButtonLData);
					this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0485));
					this.closeButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							JLog2Dialog.log.log(Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							JLog2Dialog.this.dispose();
						}
					});
				}

				try {
					this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
				}
				catch (RuntimeException e) {
					this.tabFolder.setSelection(0);
				}
				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 375, 10));
				this.dialogShell.open();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}
			
			if (this.serialPort != null && this.serialPort.isConnected())
				this.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2806));
			else
				this.liveGathererButton.setText(Messages.getString(MessageIds.GDE_MSGT2805));

			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			JLog2Dialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * create a visualization control tab item
	 * @param channelNumber
	 */
	private void createVisualizationTabItem(int channelNumber, int numMeasurements) {
		this.visualizationTabItem = new CTabItem(this.tabFolder, SWT.NONE);
		this.visualizationTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 1 : 0), SWT.NORMAL));
		this.visualizationTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2809) + GDE.STRING_MESSAGE_CONCAT + this.device.getChannelNameReplacement(channelNumber));

		this.visualizationMainComposite = new Composite(this.tabFolder, SWT.NONE);
		FormLayout visualizationMainCompositeLayout = new FormLayout();
		this.visualizationMainComposite.setLayout(visualizationMainCompositeLayout);
		this.visualizationTabItem.setControl(this.visualizationMainComposite);
		{
			FormData layoutData = new FormData();
			layoutData.top = new FormAttachment(0, 1000, 0);
			layoutData.left = new FormAttachment(0, 1000, 0);
			layoutData.right = new FormAttachment(1000, 1000, 0);
			layoutData.bottom = new FormAttachment(1000, 1000, 0);
			new JLog2VisualizationControl(this.visualizationMainComposite, layoutData, this, channelNumber, this.device, Messages.getString(MessageIds.GDE_MSGT2809), 0, numMeasurements);

		}
	}

	/**
	 * set the save visualization configuration button enabled 
	 */
	@Override
	public void enableSaveButton(boolean enable) {
		this.saveVisualizationButton.setEnabled(enable);
		this.application.updateAllTabs(true);
	}

	/**
	 * @return the tabFolder selection index
	 */
	public Integer getTabFolderSelectionIndex() {
		return this.tabFolder.getItemCount() == this.tabFolder.getSelectionIndex() + 1 ? this.tabFolder.getSelectionIndex() - 1 : this.tabFolder.getSelectionIndex();
	}

	void loadSetup() {
		FileDialog fd = this.application.openFileOpenDialog(this.dialogShell, Messages.getString(MessageIds.GDE_MSGT2801), new String[] { GDE.FILE_ENDING_STAR_TXT, GDE.FILE_ENDING_STAR },
				((JLog2) this.device).getConfigurationFileDirecotry(), JLog2.SM_JLOG2_CONFIG_TXT, SWT.SINGLE);
		this.selectedSetupFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + fd.getFileName();
		this.selectedVersionFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + "version.txt";
		JLog2Dialog.log.log(Level.FINE, "selectedSetupFile = " + this.selectedSetupFile); //$NON-NLS-1$

		if (fd.getFileName().length() > 4) {
			String searchDirectory = device.getDeviceConfiguration().getDataBlockPreferredDataLocation();
			if (!Settings.getInstance().isDeviceImportDirectoryObjectRelated() && !searchDirectory.contains(fd.getFilterPath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX)))
				device.getDeviceConfiguration().setDataBlockPreferredDataLocation(fd.getFilterPath());
			int version = Integer.valueOf("400");
			try {
				File versionFile = new File(this.selectedVersionFile);
				if (versionFile.exists()) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(versionFile), "ISO-8859-1")); //$NON-NLS-1$
					String line = reader.readLine();
					try {
						version = Integer.valueOf(line.substring(0, 5).replace(GDE.STRING_DOT, GDE.STRING_EMPTY));
					}
					catch (Exception e) {
						// ignore
					}
					reader.close();
				}
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.selectedSetupFile), "ISO-8859-1")); //$NON-NLS-1$
				String line = reader.readLine();
				((JLog2Configuration) this.configurationTabItem.getControl()).loadConfiuration(line, version);
				reader.close();
			}
			catch (Exception e) {
				JLog2Dialog.log.log(Level.SEVERE, e.getMessage(), e);
				((JLog2Configuration) this.configurationTabItem.getControl()).loadConfiuration(JLog2Configuration.Configuration400.noneSelected, version);
			}
		}
	}

	void saveSetup() {
		FileDialog fileDialog = this.application.prepareFileSaveDialog(this.dialogShell, Messages.getString(MessageIds.GDE_MSGT2802), new String[] { GDE.FILE_ENDING_STAR_TXT, GDE.FILE_ENDING_STAR },
				((JLog2) this.device).getConfigurationFileDirecotry(), JLog2.SM_JLOG2_CONFIG_TXT);
		String setupFilePath = fileDialog.open();
		JLog2Dialog.log.log(Level.FINE, "selectedSetupFile = " + setupFilePath); //$NON-NLS-1$

		if (setupFilePath != null && setupFilePath.length() > 4) {
			String searchDirectory = device.getDeviceConfiguration().getDataBlockPreferredDataLocation();
			if (!Settings.getInstance().isDeviceImportDirectoryObjectRelated() && !searchDirectory.contains(fileDialog.getFilterPath().replace(GDE.CHAR_FILE_SEPARATOR_WINDOWS, GDE.CHAR_FILE_SEPARATOR_UNIX)))
				device.getDeviceConfiguration().setDataBlockPreferredDataLocation(fileDialog.getFilterPath());
			try {
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(setupFilePath), "ISO-8859-1")); //$NON-NLS-1$
				writer.write(((JLog2Configuration) this.configurationTabItem.getControl()).configuration.getConfiguration());
				writer.close();
				this.liveGathererButton.setEnabled(false);
				this.isConfigChanged = false;
			}
			catch (Exception e) {
				JLog2Dialog.log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
}
