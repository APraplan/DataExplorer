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
package gde.device.r2prototyping;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.MeasurementControlConfigurable;
import gde.ui.SWTResourceManager;

/**
 * Dialog class to enable visualization control
 * @author Winfried Brügmann
 */
public class S32Dialog extends DeviceDialog {
	final static Logger			log									= Logger.getLogger(S32Dialog.class.getName());

	CTabFolder							tabFolder, subTabFolder1, subTabFolder2;
	CTabItem								visualizationTabItem;
	CTabItem								configurationTabItem;
	Composite								configurationMainComposite;
	Label										tabItemLabel;

	Button									saveVisualizationButton, inputFileButton, helpButton, closeButton;

	final IDevice						device;																																								// get device specific things, get serial port, ...
	final Settings					settings;																																							// application configuration settings
	String									selectedSetupFile;
	String									selectedVersionFile;

	RecordSet								lastActiveRecordSet	= null;
	boolean									isVisibilityChanged	= false;
	boolean									isConfigChanged			= false;
	int											measurementsCount		= 0;
	final List<CTabItem>		csonfigurations			= new ArrayList<CTabItem>();
	final List<Composite>		measurementControls	= new ArrayList<Composite>();

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public S32Dialog(Shell parent, S32 useDevice) {
		super(parent);
		this.device = useDevice;
		this.settings = Settings.getInstance();
		this.measurementsCount = this.device.getNumberOfMeasurements(1);
	}

	@Override
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			S32Dialog.log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
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
				this.dialogShell.pack();
				this.dialogShell.setSize(GDE.IS_LINUX ? 740 : 675, 500);
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
					public void handleEvent(Event event) {
						switch (event.detail) {
						case SWT.TRAVERSE_ESCAPE:
							S32Dialog.this.dialogShell.close();
							event.detail = SWT.TRAVERSE_NONE;
							event.doit = false;
							break;
						}
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						S32Dialog.log.log(Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (S32Dialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { S32Dialog.this.device.getPropertiesFileName() });
							if (S32Dialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								S32Dialog.log.log(Level.FINE, "SWT.YES"); //$NON-NLS-1$
								S32Dialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
						S32Dialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						S32Dialog.log.log(Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						S32Dialog.this.application.openHelpDialog("JLog3", "HelpInfo.html", true); //$NON-NLS-1$
					}
				});
				this.dialogShell.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent paintevent) {
						if (S32Dialog.log.isLoggable(Level.FINEST)) S32Dialog.log.log(Level.FINEST, "dialogShell.paintControl, event=" + paintevent); //$NON-NLS-1$
						RecordSet activeRecordSet = S32Dialog.this.application.getActiveRecordSet();
						if (S32Dialog.this.lastActiveRecordSet == null && activeRecordSet != null
								|| (activeRecordSet != null && !S32Dialog.this.lastActiveRecordSet.getName().equals(activeRecordSet.getName()))) {
							S32Dialog.this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
						}
						S32Dialog.this.lastActiveRecordSet = S32Dialog.this.application.getActiveRecordSet();
					}
				});
				{
					this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
					this.tabFolder.setSimple(false);
					FormData tabFolderLData = new FormData();
					tabFolderLData.top = new FormAttachment(0, 1000, 0);
					tabFolderLData.left = new FormAttachment(0, 1000, 0);
					tabFolderLData.right = new FormAttachment(1000, 1000, 0);
					tabFolderLData.bottom = new FormAttachment(1000, 1000, -50);
					this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tabFolder.setLayoutData(tabFolderLData);
					{
						for (int i = 1; i <= this.device.getChannelCount(); i++) {
							createVisualizationTabItem(i, this.device.getNumberOfMeasurements(i));
						}
					}
					this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
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
					this.saveVisualizationButton.setText(Messages.getString(MessageIds.GDE_MSGT3810));
					this.saveVisualizationButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3811));
					this.saveVisualizationButton.setEnabled(false);
					this.saveVisualizationButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							S32Dialog.log.log(Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							S32Dialog.this.device.storeDeviceProperties();
							S32Dialog.this.saveVisualizationButton.setEnabled(false);
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
					this.inputFileButton.setText(Messages.getString(MessageIds.GDE_MSGT3812));
					this.inputFileButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3813));
					this.inputFileButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							S32Dialog.log.log(Level.FINEST, "inputFileButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (S32Dialog.this.isVisibilityChanged) {
								String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { S32Dialog.this.device.getPropertiesFileName() });
								if (S32Dialog.this.application.openYesNoMessageDialog(S32Dialog.this.dialogShell, msg) == SWT.YES) {
									S32Dialog.log.log(Level.FINE, "SWT.YES"); //$NON-NLS-1$
									S32Dialog.this.device.storeDeviceProperties();
								}
							}
							S32Dialog.this.device.open_closeCommPort();
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
							S32Dialog.log.log(Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							S32Dialog.this.application.openHelpDialog("JLog3", "HelpInfo.html", true); //$NON-NLS-1$
						}
					});
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
							S32Dialog.log.log(Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							S32Dialog.this.dispose();
						}
					});
				}

				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 375, 10));
				this.dialogShell.open();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}

			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			S32Dialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * create a visualization control tab item
	 * @param channelNumber
	 */
	private void createVisualizationTabItem(int channelNumber, int numMeasurements) {
		this.visualizationTabItem = new CTabItem(this.tabFolder, SWT.NONE);
		this.visualizationTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 1 : 0), SWT.NORMAL));
		this.visualizationTabItem.setText(this.device.getChannelNameReplacement(channelNumber));

		ScrolledComposite scolledComposite = new ScrolledComposite(this.tabFolder, SWT.V_SCROLL);
		scolledComposite.setLayout(new FillLayout());
		this.visualizationTabItem.setControl(scolledComposite);

		Composite mainTabComposite = new Composite(scolledComposite, SWT.None);
		GridLayout mainTabCompositeLayout = new GridLayout();
		mainTabCompositeLayout.makeColumnsEqualWidth = true;
		mainTabCompositeLayout.numColumns = 2;
		mainTabComposite.setLayout(mainTabCompositeLayout);
		mainTabComposite.setSize(610, 350);
		scolledComposite.setContent(mainTabComposite);

		this.tabItemLabel = new Label(mainTabComposite, SWT.CENTER);
		GridData tabItemLabelLData = new GridData();
		tabItemLabelLData.grabExcessHorizontalSpace = true;
		tabItemLabelLData.horizontalAlignment = GridData.CENTER;
		tabItemLabelLData.verticalAlignment = GridData.BEGINNING;
		tabItemLabelLData.heightHint = 18;
		tabItemLabelLData.widthHint = 600;
		tabItemLabelLData.horizontalSpan = 2;
		this.tabItemLabel.setLayoutData(tabItemLabelLData);
		this.tabItemLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 2, SWT.BOLD));
		this.tabItemLabel.setText(Messages.getString(MessageIds.GDE_MSGT3809));
	
		List<MeasurementType> measurementTypes = this.device.getChannelMeasuremtsReplacedNames(channelNumber);
		for (int i = 0; i < numMeasurements; i++) { // display actual only the native 31 measurements of JLog2
			//allow all measurement names, symbols and units to be correctable
			this.measurementControls.add(new MeasurementControlConfigurable(mainTabComposite, this, channelNumber, i,
					measurementTypes.get(i), this.device, 1, GDE.STRING_BLANK + i, GDE.STRING_EMPTY));
		}

		scolledComposite.addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent evt) {
				log.log(java.util.logging.Level.FINEST, "scolledComposite.controlResized, event=" + evt); //$NON-NLS-1$
				int height = 35 + S32Dialog.this.device.getChannelMeasuremtsReplacedNames(S32Dialog.this.tabFolder.getSelectionIndex() + 1).size() * 28 / 2;
				Channel channel = Channels.getInstance().get(S32Dialog.this.tabFolder.getSelectionIndex() + 1);
				if (channel != null)
					if (channel.getActiveRecordSet() != null)
						height = 35 + (channel.getActiveRecordSet().size() + 1) * 28 / 2;
				mainTabComposite.setSize(scolledComposite.getClientArea().width, height);
			}

			@Override
			public void controlMoved(ControlEvent evt) {
				log.log(java.util.logging.Level.FINEST, "scolledComposite.controlMoved, event=" + evt); //$NON-NLS-1$
				int height = 35 + S32Dialog.this.device.getChannelMeasuremtsReplacedNames(S32Dialog.this.tabFolder.getSelectionIndex() + 1).size() * 28 / 2;
				Channel channel = Channels.getInstance().get(S32Dialog.this.tabFolder.getSelectionIndex() + 1);
				if (channel != null)
					if (channel.getActiveRecordSet() != null)
						height = 35 + (channel.getActiveRecordSet().size() + 1) * 28 / 2;
				mainTabComposite.setSize(scolledComposite.getClientArea().width, height);
			}
		});
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
}
