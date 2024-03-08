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

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.device.DataTypes;
import gde.device.DeviceConfiguration;
import gde.device.ObjectFactory;
import gde.device.StatisticsType;
import gde.device.TriggerType;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

/**
 * class defining a composite with statistics configuration data
 * @author Winfried Brügmann
*/
public class StatisticsTypeTabItem extends CTabItem implements Cloneable {
	final static Logger						log														= Logger.getLogger(StatisticsTypeTabItem.class.getName());
	final static String[]					STRING_ARRAY_TRIGGER_SECONDS	= new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20" }; //$NON-NLS-1$
	final static String[]					STRING_ARRAY_TRIGGER_VALUES		= new String[] { "250", "500", "1000", "1500", "2000", "2500", "3000", "3500", "4000", "5000", "7500" };													//$NON-NLS-1$

	final MeasurementTypeTabItem	measurementTypeTabItem;
	final CTabFolder							channelConfigMeasurementPropertiesTabFolder;
	final String									tabName;
	final DevicePropertiesEditor	propsEditor;

	Menu													popupMenu;
	MeasurementContextmenu				contextMenu;

	ScrolledComposite							scrolledComposite;
	Composite											statisticsComposite;
	CCombo												triggerLevelCombo;
	Label													triggerLevelLabel;
	CCombo												sumByTriggerRefOrdinalCombo, sumBySecondaryTriggerRefOrdinalCombo;
	Button												isSumByTriggerRefOrdinalButton;
	Button												sumTriggerTimeButton;
	Button												countByTriggerButton;
	Text													triggerRefOrdinalText, sumTriggerText;
	Button												isRatioRefOrdinalButton;
	Button												isIntegrateByTriggerButton;
	Text													triggerCommentText;
	CCombo												minTimeSecCombo;
	Label													minTimeSecLabel;
	Button												isGreaterButton;
	Button												triggerLevelButton;
	Text													ratioText;
	CCombo												ratioRefOrdinalCombo;
	Text													sumTriggerTimeText;
	Text													countByTriggerText;
	CCombo												triggerRefOrdinalCombo;
	Button												triggerRefOrdinalButton;
	Button												statisticsSigmaButton, secondaryTriggerButton;
	Button												statisticsMaxButton;
	Button												statisticsAvgButton;
	Button												statisticsMinButton;

	Boolean												statisticsMin, statisticsMax, statisticsAvg, statisticsSigma, isSecondaryTrigger;
	String												triggerComment, sumTriggerComment, sumTriggerTimeComment, countByTriggerComment, ratioComment, triggerRefOrdinalComment;
	Boolean												isGreater, isSumTriggerTime, isCountByTrigger, isRatioRefOrdinal, isIntegrateByTrigger;
	Integer												triggerLevel, minTimeSec, ratioRefOrdinal;
	Integer												triggerRefOrdinal, triggerSecondaryRefOrdinal, sumByTriggerRefOrdinal, sumBySecondaryTriggerRefOrdinal;																																																											// must be equal !!!
	boolean												isSomeTriggerDefined					= false;
	String[]											measurementReferenceItems;

	DeviceConfiguration						deviceConfig;
	int														channelConfigNumber;
	StatisticsType								statisticsType;
	TriggerType										triggerType;

	public StatisticsTypeTabItem(CTabFolder parent, int style, String name, MeasurementTypeTabItem useMeasurementTypeTabItem) {
		super(parent, style);
		this.channelConfigMeasurementPropertiesTabFolder = parent;
		this.propsEditor = DevicePropertiesEditor.getInstance();
		this.measurementTypeTabItem = useMeasurementTypeTabItem;
		this.tabName = name;

		initGUI();
	}

	@Override
	public synchronized StatisticsTypeTabItem clone() {
		try {
			super.clone();
		}
		catch (CloneNotSupportedException e) {
			// ignore
		}
		return new StatisticsTypeTabItem(this);
	}

	/**
	 * copy constructor
	 * @param copyFrom
	 */
	private StatisticsTypeTabItem(StatisticsTypeTabItem copyFrom) {
		super(copyFrom.channelConfigMeasurementPropertiesTabFolder, SWT.CLOSE);
		this.propsEditor = DevicePropertiesEditor.getInstance();
		this.channelConfigMeasurementPropertiesTabFolder = copyFrom.channelConfigMeasurementPropertiesTabFolder;
		this.measurementTypeTabItem = copyFrom.measurementTypeTabItem;
		this.tabName = copyFrom.tabName;

		this.statisticsMin = copyFrom.statisticsMin;
		this.statisticsMax = copyFrom.statisticsMax;
		this.statisticsAvg = copyFrom.statisticsAvg;
		this.statisticsSigma = copyFrom.statisticsSigma;

		//do not copy trigger attributes, only one trigger permitted per channel/configuration measurements
		//this.triggerLevel = copyFrom.triggerLevel;
		//this.triggerComment = copyFrom.triggerComment;
		//this.isGreater = copyFrom.isGreater;
		//this.minTimeSec = copyFrom.minTimeSec;
		this.isCountByTrigger = copyFrom.isCountByTrigger;
		this.countByTriggerComment = copyFrom.countByTriggerComment;

		this.triggerRefOrdinal = copyFrom.triggerRefOrdinal;
		this.sumByTriggerRefOrdinal = copyFrom.sumByTriggerRefOrdinal;
		this.sumBySecondaryTriggerRefOrdinal = copyFrom.sumBySecondaryTriggerRefOrdinal;
		this.sumTriggerComment = copyFrom.sumTriggerComment;
		this.isRatioRefOrdinal = copyFrom.isRatioRefOrdinal;
		this.ratioRefOrdinal = copyFrom.ratioRefOrdinal;
		this.ratioComment = copyFrom.ratioComment;
		
		this.isIntegrateByTrigger = copyFrom.isIntegrateByTrigger;

		this.deviceConfig = copyFrom.deviceConfig;
		this.channelConfigNumber = copyFrom.channelConfigNumber;

		initGUI();
	}

	/**
	 * set the statistics type and update all internal variables
	 * @param useDeviceConfig the device configuration used
	 * @param useStatisticsType the statisticsType to set
	 * @param useChannelConfigNumber the channel configuration number
	 */
	public void setStatisticsType(DeviceConfiguration useDeviceConfig, StatisticsType useStatisticsType, int useChannelConfigNumber) {
		this.deviceConfig = useDeviceConfig;
		this.statisticsType = useStatisticsType;
		this.channelConfigNumber = useChannelConfigNumber;
		this.triggerType = this.statisticsType == null ? null : this.statisticsType.getTrigger();

		if (this.statisticsType != null) {
			this.statisticsMinButton.setSelection(this.statisticsMin = this.statisticsType.isMin());
			this.statisticsMaxButton.setSelection(this.statisticsMax = this.statisticsType.isMax());
			this.statisticsAvgButton.setSelection(this.statisticsAvg = this.statisticsType.isAvg());
			this.statisticsSigmaButton.setSelection(this.statisticsSigma = this.statisticsType.isSigma());
		}
		this.measurementReferenceItems = this.deviceConfig.getMeasurementNames(this.channelConfigNumber);
		this.triggerRefOrdinalCombo.setItems(this.measurementReferenceItems);
		this.sumByTriggerRefOrdinalCombo.setItems(this.measurementReferenceItems);
		this.sumBySecondaryTriggerRefOrdinalCombo.setItems(this.measurementReferenceItems);
		this.ratioRefOrdinalCombo.setItems(this.measurementReferenceItems);

		updateTriggerDependent(this.isSomeTriggerDefined = isSomeTriggerDefined());

		if (this.triggerRefOrdinal != null && this.sumByTriggerRefOrdinal != null && this.triggerRefOrdinal.intValue() != this.sumByTriggerRefOrdinal.intValue()) {
			MessageBox mb = new MessageBox(this.channelConfigMeasurementPropertiesTabFolder.getShell(), SWT.OK);
			mb.setText(Messages.getString(MessageIds.GDE_MSGW0540));
			mb.setMessage(Messages.getString(MessageIds.GDE_MSGW0542));
		}
		initialize();
	}

	/**
	 * Update trigger dependent attributes and fields
	 * A trigger reference can not be set if no trigger is defined for this measurement types!
	 * This method will remove trigger referencing or depending configurations if no trigger is defined.
	 */
	private void updateTriggerDependent(boolean isTriggerDefined) {
		this.triggerLevelButton.setEnabled(!isTriggerDefined || this.triggerType != null);
		this.secondaryTriggerButton.setEnabled(!isTriggerDefined || this.triggerType != null);

		this.triggerLevelButton.setSelection(isTriggerDefined && this.triggerType != null);
		this.secondaryTriggerButton.setSelection(isTriggerDefined && this.triggerType != null && this.triggerType.isSecondary());
		this.triggerLevelLabel.setEnabled(isTriggerDefined && this.triggerType != null);
		this.triggerLevelCombo.setEnabled(isTriggerDefined && this.triggerType != null);
		this.triggerCommentText.setEnabled(isTriggerDefined && this.triggerType != null);
		this.isGreaterButton.setEnabled(isTriggerDefined && this.triggerType != null);
		this.minTimeSecLabel.setEnabled(isTriggerDefined && this.triggerType != null);
		this.minTimeSecCombo.setEnabled(isTriggerDefined && this.triggerType != null);

		if (isTriggerDefined && this.triggerType != null) {
			this.triggerLevelCombo.setText(GDE.STRING_EMPTY + (this.triggerLevel = this.triggerType.getLevel()));
			this.triggerCommentText.setText(this.triggerComment = this.triggerType.getComment() == null ? GDE.STRING_EMPTY : this.triggerType.getComment());
			this.isGreaterButton.setSelection(this.isGreater = this.triggerType.isGreater());
			this.minTimeSecCombo.select((this.minTimeSec = this.triggerType.getMinTimeSec()) - 1);
		}
		else if (this.statisticsType != null && this.statisticsType.getTrigger() != null) {
			this.statisticsType.removeTrigger();
			this.deviceConfig.setChangePropery(true);
			this.propsEditor.enableSaveButton(true);
		}

		this.sumTriggerTimeButton.setEnabled(isTriggerDefined);
		this.countByTriggerButton.setEnabled(isTriggerDefined);
		this.triggerRefOrdinalButton.setEnabled(isTriggerDefined && this.triggerType == null);
		this.triggerRefOrdinalText.setEnabled(isTriggerDefined && this.triggerType == null);
		this.isSumByTriggerRefOrdinalButton.setEnabled(isTriggerDefined && this.triggerType == null);
		this.sumTriggerText.setEnabled(isTriggerDefined && this.triggerType == null);
		this.isRatioRefOrdinalButton.setEnabled(isTriggerDefined && this.triggerType == null);
		this.ratioRefOrdinalCombo.setEnabled(isTriggerDefined && this.triggerType == null);
		this.ratioText.setEnabled(isTriggerDefined && this.triggerType == null);
		this.isIntegrateByTriggerButton.setEnabled(isTriggerDefined && this.triggerType == null);

		if (this.statisticsType != null) {
			if (isTriggerDefined) {
				if (this.isSumTriggerTime = (this.statisticsType.getSumTriggerTimeText() != null)) {
					this.countByTriggerButton.setSelection(this.isSumTriggerTime);
					if (this.isSumTriggerTime) {
						this.sumTriggerTimeText.setEnabled(true);
						this.sumTriggerTimeText.setText(this.sumTriggerTimeComment = this.statisticsType.getSumTriggerTimeText() == null ? GDE.STRING_EMPTY : this.statisticsType.getSumTriggerTimeText());
					}
					else {
						this.sumTriggerTimeText.setEnabled(false);
					}
				}
				else {
					this.countByTriggerText.setEnabled(false);
				}
				if ((this.isCountByTrigger = this.statisticsType.isCountByTrigger()) != null) {
					this.countByTriggerButton.setSelection(this.isCountByTrigger);
					if (this.isCountByTrigger) {
						this.countByTriggerText.setEnabled(true);
						this.countByTriggerText.setText(this.countByTriggerComment = this.statisticsType.getCountTriggerText() == null ? GDE.STRING_EMPTY : this.statisticsType.getCountTriggerText());
					}
					else {
						this.countByTriggerText.setEnabled(false);
					}
				}
				else {
					this.countByTriggerText.setEnabled(false);
				}
				if ((this.triggerRefOrdinal = this.statisticsType.getTriggerRefOrdinal()) != null) {
					this.triggerRefOrdinalButton.setSelection(true);
					this.triggerRefOrdinalText.setText(this.triggerRefOrdinalComment = this.statisticsType.getComment() == null ? GDE.STRING_EMPTY : this.statisticsType.getComment());
					this.triggerRefOrdinalCombo.select(this.triggerRefOrdinal);
				}
				else {
					//this.triggerRefOrdinalCombo.setEnabled(false);
				}
				if ((this.sumByTriggerRefOrdinal = this.statisticsType.getSumByTriggerRefOrdinal()) != null) {
					this.isSumByTriggerRefOrdinalButton.setEnabled(true);
					this.isSumByTriggerRefOrdinalButton.setSelection(true);
					//this.sumByTriggerRefOrdinalCombo.setEnabled(true);
					this.sumTriggerText.setEnabled(true);
					this.sumByTriggerRefOrdinalCombo.select(this.sumByTriggerRefOrdinal);
					this.sumTriggerText.setText((this.sumTriggerComment = this.statisticsType.getSumTriggerText()) != null ? this.sumTriggerComment : GDE.STRING_EMPTY);
				}
				else {
					this.isSumByTriggerRefOrdinalButton.setSelection(false);
					//this.sumByTriggerRefOrdinalCombo.setEnabled(false);
					this.sumTriggerText.setEnabled(false);
				}
				if (this.sumByTriggerRefOrdinal != null && (this.sumBySecondaryTriggerRefOrdinal = this.statisticsType.getSumBySecondaryTriggerRefOrdinal()) != null) {
					this.sumBySecondaryTriggerRefOrdinalCombo.select(this.sumBySecondaryTriggerRefOrdinal);
				}
				if ((this.ratioRefOrdinal = this.statisticsType.getRatioRefOrdinal()) != null) {
					this.isRatioRefOrdinalButton.setEnabled(true);
					this.isRatioRefOrdinalButton.setSelection(this.isRatioRefOrdinal = true);
					this.ratioRefOrdinalCombo.setEnabled(true);
					this.ratioRefOrdinalCombo.select(this.ratioRefOrdinal);
					this.ratioText.setEnabled(true);
					this.ratioText.setText((this.ratioComment = this.statisticsType.getRatioText()) != null ? this.ratioComment : GDE.STRING_EMPTY);
				}
				else {
					this.isRatioRefOrdinalButton.setSelection(this.isRatioRefOrdinal = false);
					this.ratioRefOrdinalCombo.setEnabled(false);
					this.ratioText.setEnabled(false);
				}

				if ((this.isIntegrateByTrigger = this.statisticsType.isIntegrateByTrigger()) != null) {
					this.isIntegrateByTriggerButton.setSelection(this.isIntegrateByTrigger);
				}
				else {
					this.isIntegrateByTriggerButton.setSelection(false);
				}
			}
			else { // no measurement defines a trigger, as reult no measurement can reference to it
				this.isCountByTrigger = null;
				if (this.statisticsType.isCountByTrigger() != null) {
					this.statisticsType.setCountByTrigger(null);
					this.deviceConfig.setChangePropery(true);
					this.propsEditor.enableSaveButton(true);
				}
				this.countByTriggerButton.setSelection(false);
				this.countByTriggerText.setEnabled(false);
				this.countByTriggerText.setText(GDE.STRING_EMPTY);
				this.countByTriggerComment = null;
				if (this.statisticsType.getCountTriggerText() != null) {
					this.statisticsType.setCountTriggerText(null);
					this.deviceConfig.setChangePropery(true);
					this.propsEditor.enableSaveButton(true);
				}

				this.triggerRefOrdinal = null;
				if (this.statisticsType.getTriggerRefOrdinal() != null) {
					this.statisticsType.setTriggerRefOrdinal(null);
					this.deviceConfig.setChangePropery(true);
					this.propsEditor.enableSaveButton(true);
				}
				this.triggerRefOrdinalButton.setSelection(false);
				this.triggerRefOrdinalCombo.select(0);

				this.sumByTriggerRefOrdinal = null;
				if (this.statisticsType.getSumByTriggerRefOrdinal() != null) {
					this.statisticsType.setSumByTriggerRefOrdinal(null);
					this.deviceConfig.setChangePropery(true);
					this.propsEditor.enableSaveButton(true);
				}
				this.isSumByTriggerRefOrdinalButton.setSelection(false);
				this.sumByTriggerRefOrdinalCombo.select(0);
				this.sumBySecondaryTriggerRefOrdinalCombo.select(0);
				this.sumTriggerComment = null;
				if (this.statisticsType.getSumTriggerText() != null) {
					this.statisticsType.setSumTriggerText(null);
					this.deviceConfig.setChangePropery(true);
					this.propsEditor.enableSaveButton(true);
				}
				this.sumTriggerText.setEnabled(false);
				this.sumTriggerText.setText(GDE.STRING_EMPTY);

				this.ratioRefOrdinal = null;
				if (this.statisticsType.getRatioRefOrdinal() != null) {
					this.statisticsType.setRatioRefOrdinal(null);
					this.deviceConfig.setChangePropery(true);
					this.propsEditor.enableSaveButton(true);
				}
				this.isRatioRefOrdinalButton.setEnabled(false);
				this.isRatioRefOrdinalButton.setSelection(false);
				this.ratioRefOrdinalCombo.setEnabled(false);
				this.ratioRefOrdinalCombo.select(0);
				this.ratioComment = null;
				if (this.statisticsType.getRatioText() != null) {
					this.statisticsType.setRatioText(null);
					this.deviceConfig.setChangePropery(true);
					this.propsEditor.enableSaveButton(true);
				}
				this.ratioText.setEnabled(false);
				this.ratioText.setText(GDE.STRING_EMPTY);
			}
		}
	}

	/**
	 * find if some measurement defines a trigger to enable reference to it,
	 * loop through all measurement of the channel configuration
	 * @return true if trigger is defined
	 */
	private boolean isSomeTriggerDefined() {
		for (int i = 0; this.deviceConfig != null && i < this.deviceConfig.getMeasurementNames(this.channelConfigNumber).length; i++) {
			if (this.deviceConfig.getMeasurementStatistic(this.channelConfigNumber, i) != null && this.deviceConfig.getMeasurementStatistic(this.channelConfigNumber, i).getTrigger() != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * find reference ordinal of a defined trigger,
	 * loop through all measurement of the channel configuration
	 * @return the reference ordinal or -1 if no trigger is set
	 */
	private int getTriggerReferenceOrdinal() {
		for (int i = 0; this.deviceConfig != null && i < this.deviceConfig.getMeasurementNames(this.channelConfigNumber).length; i++) {
			if (this.deviceConfig.getMeasurementStatistic(this.channelConfigNumber, i) != null && this.deviceConfig.getMeasurementStatistic(this.channelConfigNumber, i).getTrigger() != null) {
				return i;
			}
		}
		return -1;
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText(this.tabName);
			this.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.scrolledComposite = new ScrolledComposite(this.channelConfigMeasurementPropertiesTabFolder, SWT.H_SCROLL);
			this.setControl(this.scrolledComposite);
			this.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent disposeevent) {
					log.log(java.util.logging.Level.FINEST, "statisticsTypeTabItem.widgetDisposed, event=" + disposeevent); //$NON-NLS-1$
					//StatisticsTypeTabItem.this.enableContextMenu(false);
				}
			});
			this.statisticsComposite = new Composite(this.scrolledComposite, SWT.NONE);
			this.statisticsComposite.setLayout(null);
			this.statisticsComposite.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "statisticsComposite.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_A1.html#device_properties_statistics"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			{
				this.statisticsMinButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.statisticsMinButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.statisticsMinButton.setText(Messages.getString(MessageIds.GDE_MSGT0556));
				this.statisticsMinButton.setBounds(10, 5, 90, 20);
				this.statisticsMinButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "staisticsMinButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.statisticsMin = StatisticsTypeTabItem.this.statisticsMinButton.getSelection();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setMin(StatisticsTypeTabItem.this.statisticsMin);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.statisticsAvgButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.statisticsAvgButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.statisticsAvgButton.setText(Messages.getString(MessageIds.GDE_MSGT0557));
				this.statisticsAvgButton.setBounds(10, 30, 90, 20);
				this.statisticsAvgButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "statisticsAvgButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.statisticsAvg = StatisticsTypeTabItem.this.statisticsAvgButton.getSelection();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setAvg(StatisticsTypeTabItem.this.statisticsAvg);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.statisticsMaxButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.statisticsMaxButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.statisticsMaxButton.setText(Messages.getString(MessageIds.GDE_MSGT0558));
				this.statisticsMaxButton.setBounds(10, 55, 90, 20);
				this.statisticsMaxButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "statisticsMaxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.statisticsMax = StatisticsTypeTabItem.this.statisticsMaxButton.getSelection();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setMax(StatisticsTypeTabItem.this.statisticsMax);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.statisticsSigmaButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.statisticsSigmaButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.statisticsSigmaButton.setText(Messages.getString(MessageIds.GDE_MSGT0559));
				this.statisticsSigmaButton.setBounds(10, 80, 90, 20);
				this.statisticsSigmaButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "statisticsSigmaButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.statisticsSigma = StatisticsTypeTabItem.this.statisticsSigmaButton.getSelection();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setSigma(StatisticsTypeTabItem.this.statisticsSigma);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.secondaryTriggerButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.secondaryTriggerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.secondaryTriggerButton.setText("secondary");
				this.secondaryTriggerButton.setToolTipText("secondary Trigger");
				this.secondaryTriggerButton.setBounds(10, 105, 90, 20);
				this.secondaryTriggerButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "secondaryTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.isSecondaryTrigger = StatisticsTypeTabItem.this.secondaryTriggerButton.getSelection();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							if (StatisticsTypeTabItem.this.triggerType != null) {
								StatisticsTypeTabItem.this.triggerType.setSecondary(StatisticsTypeTabItem.this.isSecondaryTrigger);
								StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
								StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
							}
						}
					}
				});
			}
			{
				this.triggerLevelButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.triggerLevelButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.triggerLevelButton.setText(Messages.getString(MessageIds.GDE_MSGT0560));
				this.triggerLevelButton.setBounds(125, 5, 63, 20);
				this.triggerLevelButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0555));
				this.triggerLevelButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "triggerLevelButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							if (StatisticsTypeTabItem.this.triggerLevelButton.getSelection()) {
								if (StatisticsTypeTabItem.this.statisticsType.getTrigger() == null) {
									StatisticsTypeTabItem.this.statisticsType.setTrigger(StatisticsTypeTabItem.this.triggerType = new ObjectFactory().createTriggerType());
									StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
									StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);

									StatisticsTypeTabItem.this.triggerType.setLevel(StatisticsTypeTabItem.this.triggerLevel == null ? StatisticsTypeTabItem.this.triggerLevel = 0
											: StatisticsTypeTabItem.this.triggerLevel);
									StatisticsTypeTabItem.this.triggerType.setComment(StatisticsTypeTabItem.this.triggerComment);
									StatisticsTypeTabItem.this.triggerType.setIsGreater(StatisticsTypeTabItem.this.isGreater == null ? (StatisticsTypeTabItem.this.isGreater = true)
											: StatisticsTypeTabItem.this.isGreater);
									StatisticsTypeTabItem.this.triggerType.setMinTimeSec(StatisticsTypeTabItem.this.minTimeSec == null ? StatisticsTypeTabItem.this.minTimeSec = 1
											: StatisticsTypeTabItem.this.minTimeSec);
									StatisticsTypeTabItem.this.statisticsType.setCountByTrigger(StatisticsTypeTabItem.this.isCountByTrigger);
									StatisticsTypeTabItem.this.statisticsType.setCountTriggerText(StatisticsTypeTabItem.this.countByTriggerComment);

									StatisticsTypeTabItem.this.statisticsType.setTriggerRefOrdinal(null);
									StatisticsTypeTabItem.this.statisticsType.setTriggerSecondaryRefOrdinal(null);
									StatisticsTypeTabItem.this.statisticsType.setSumByTriggerRefOrdinal(null);
									StatisticsTypeTabItem.this.statisticsType.setSumTriggerText(null);
									StatisticsTypeTabItem.this.statisticsType.setRatioRefOrdinal(null);
									StatisticsTypeTabItem.this.statisticsType.setRatioText(null);

									StatisticsTypeTabItem.this.statisticsComposite.redraw();
								}

								StatisticsTypeTabItem.this.triggerRefOrdinalButton.setEnabled(false);
								StatisticsTypeTabItem.this.triggerRefOrdinalText.setEnabled(false);
								StatisticsTypeTabItem.this.isSumByTriggerRefOrdinalButton.setEnabled(false);
								StatisticsTypeTabItem.this.sumTriggerText.setEnabled(false);
								StatisticsTypeTabItem.this.isRatioRefOrdinalButton.setEnabled(false);
								StatisticsTypeTabItem.this.ratioRefOrdinalCombo.setEnabled(false);
								StatisticsTypeTabItem.this.ratioText.setEnabled(false);
								StatisticsTypeTabItem.this.isIntegrateByTriggerButton.setEnabled(false);
							}
							else {
								StatisticsTypeTabItem.this.statisticsType.removeTrigger();
								StatisticsTypeTabItem.this.triggerType = null;
								StatisticsTypeTabItem.this.triggerLevel = null;
								StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
								StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);

								StatisticsTypeTabItem.this.statisticsType.setCountByTrigger(StatisticsTypeTabItem.this.isCountByTrigger);
								StatisticsTypeTabItem.this.statisticsType.setTriggerRefOrdinal(StatisticsTypeTabItem.this.triggerRefOrdinal);
								StatisticsTypeTabItem.this.statisticsType.setTriggerSecondaryRefOrdinal(StatisticsTypeTabItem.this.triggerSecondaryRefOrdinal);
								StatisticsTypeTabItem.this.statisticsType.setSumByTriggerRefOrdinal(StatisticsTypeTabItem.this.sumByTriggerRefOrdinal);
								StatisticsTypeTabItem.this.statisticsType.setSumTriggerText(StatisticsTypeTabItem.this.sumTriggerComment);
								StatisticsTypeTabItem.this.statisticsType.setRatioRefOrdinal(StatisticsTypeTabItem.this.ratioRefOrdinal);
								StatisticsTypeTabItem.this.statisticsType.setRatioText(StatisticsTypeTabItem.this.ratioComment);

								StatisticsTypeTabItem.this.triggerCommentText.setEnabled(false);
								StatisticsTypeTabItem.this.triggerLevelLabel.setEnabled(false);
								StatisticsTypeTabItem.this.triggerLevelCombo.setEnabled(false);
								StatisticsTypeTabItem.this.isGreaterButton.setEnabled(false);
								StatisticsTypeTabItem.this.minTimeSecLabel.setEnabled(false);
								StatisticsTypeTabItem.this.minTimeSecCombo.setEnabled(false);
								StatisticsTypeTabItem.this.sumTriggerTimeButton.setEnabled(false);
								StatisticsTypeTabItem.this.sumTriggerTimeText.setEnabled(false);
								StatisticsTypeTabItem.this.countByTriggerButton.setEnabled(false);
								StatisticsTypeTabItem.this.countByTriggerText.setEnabled(false);

								StatisticsTypeTabItem.this.triggerRefOrdinalButton.setEnabled(true);
								StatisticsTypeTabItem.this.triggerRefOrdinalText.setEnabled(true);
								StatisticsTypeTabItem.this.isSumByTriggerRefOrdinalButton.setEnabled(true);
								StatisticsTypeTabItem.this.sumTriggerText.setEnabled(true);
								StatisticsTypeTabItem.this.isRatioRefOrdinalButton.setEnabled(true);
								StatisticsTypeTabItem.this.ratioRefOrdinalCombo.setEnabled(true);
								StatisticsTypeTabItem.this.ratioText.setEnabled(true);
								StatisticsTypeTabItem.this.isIntegrateByTriggerButton.setEnabled(true);

								StatisticsTypeTabItem.this.statisticsComposite.redraw();
							}
							StatisticsTypeTabItem.this.updateTriggerDependent(StatisticsTypeTabItem.this.triggerLevelButton.getSelection());
						}
					}
				});
			}
			{
				this.triggerLevelLabel = new Label(this.statisticsComposite, SWT.RIGHT);
				this.triggerLevelLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.triggerLevelLabel.setText(Messages.getString(MessageIds.GDE_MSGT0561));
				this.triggerLevelLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0562));
				this.triggerLevelLabel.setBounds(190, 7, 85, 20);
			}
			{
				this.triggerLevelCombo = new CCombo(this.statisticsComposite, SWT.BORDER);
				this.triggerLevelCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.triggerLevelCombo.setBounds(280, 5, 75, 20);
				this.triggerLevelCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0562));
				this.triggerLevelCombo.setItems(StatisticsTypeTabItem.STRING_ARRAY_TRIGGER_VALUES);
				this.triggerLevelCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "triggerLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.triggerLevel = Integer.valueOf(StatisticsTypeTabItem.this.triggerLevelCombo.getText());
						if (StatisticsTypeTabItem.this.triggerType != null) {
							StatisticsTypeTabItem.this.triggerType.setLevel(StatisticsTypeTabItem.this.triggerLevel);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
				this.triggerLevelCombo.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "triggerLevelCombo.keyReleased, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.triggerLevel = Integer.valueOf(StatisticsTypeTabItem.this.triggerLevelCombo.getText());
						if (StatisticsTypeTabItem.this.triggerType != null) {
							StatisticsTypeTabItem.this.triggerType.setLevel(StatisticsTypeTabItem.this.triggerLevel);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
				this.triggerLevelCombo.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "triggerLevelCombo.verifyText, event=" + evt); //$NON-NLS-1$
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
			}
			{
				this.triggerCommentText = new Text(this.statisticsComposite, SWT.BORDER);
				this.triggerCommentText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.triggerCommentText.setBounds(360, 5, 325, 20);
				this.triggerCommentText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "triggerCommentText.keyReleased, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.triggerComment = StatisticsTypeTabItem.this.triggerCommentText.getText();
						if (StatisticsTypeTabItem.this.triggerType != null) {
							StatisticsTypeTabItem.this.triggerType.setComment(StatisticsTypeTabItem.this.triggerComment);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.isGreaterButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.isGreaterButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.isGreaterButton.setText(Messages.getString(MessageIds.GDE_MSGT0563));
				this.isGreaterButton.setBounds(125, 30, 75, 20);
				this.isGreaterButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0564));
				this.isGreaterButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "isGreaterButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.isGreater = StatisticsTypeTabItem.this.isGreaterButton.getSelection();
						if (StatisticsTypeTabItem.this.triggerType != null) {
							StatisticsTypeTabItem.this.triggerType.setIsGreater(StatisticsTypeTabItem.this.isGreater);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.minTimeSecLabel = new Label(this.statisticsComposite, SWT.RIGHT);
				this.minTimeSecLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.minTimeSecLabel.setText(Messages.getString(MessageIds.GDE_MSGT0565));
				this.minTimeSecLabel.setBounds(200, 32, 75, 20);
				this.minTimeSecLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0566));
			}
			{
				this.minTimeSecCombo = new CCombo(this.statisticsComposite, SWT.BORDER);
				this.minTimeSecCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.minTimeSecCombo.setBounds(280, 30, 75, 20);
				this.minTimeSecCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0566));
				this.minTimeSecCombo.setItems(StatisticsTypeTabItem.STRING_ARRAY_TRIGGER_SECONDS);
				this.minTimeSecCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "minTimeSecCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.minTimeSec = Integer.valueOf(StatisticsTypeTabItem.this.minTimeSecCombo.getText());
						if (StatisticsTypeTabItem.this.triggerType != null) {
							StatisticsTypeTabItem.this.triggerType.setMinTimeSec(StatisticsTypeTabItem.this.minTimeSec);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
				this.minTimeSecCombo.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "minTimeSecCombo.keyReleased, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.minTimeSec = Integer.valueOf(StatisticsTypeTabItem.this.minTimeSecCombo.getText());
						if (StatisticsTypeTabItem.this.triggerType != null) {
							StatisticsTypeTabItem.this.triggerType.setMinTimeSec(StatisticsTypeTabItem.this.minTimeSec);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
				this.minTimeSecCombo.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "minTimeSecCombo.verifyText, event=" + evt); //$NON-NLS-1$
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
			}
			{
				this.sumTriggerTimeButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.sumTriggerTimeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.sumTriggerTimeButton.setText(Messages.getString(MessageIds.GDE_MSGT0511));
				this.sumTriggerTimeButton.setBounds(125, 55, 170, 20);
				this.sumTriggerTimeButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0525));
				this.sumTriggerTimeButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "sumTriggerTimeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.isSumTriggerTime = StatisticsTypeTabItem.this.sumTriggerTimeButton.getSelection();
						StatisticsTypeTabItem.this.sumTriggerTimeText.setEnabled(StatisticsTypeTabItem.this.isSumTriggerTime);
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setSumTriggerTimeText(StatisticsTypeTabItem.this.isSumTriggerTime ? StatisticsTypeTabItem.this.sumTriggerTimeComment : null);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.sumTriggerTimeText = new Text(this.statisticsComposite, SWT.BORDER);
				this.sumTriggerTimeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.sumTriggerTimeText.setBounds(360, 55, 325, 20);
				this.sumTriggerTimeText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "sumTriggerTimeText.keyReleased, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.sumTriggerTimeComment = StatisticsTypeTabItem.this.sumTriggerTimeText.getText().trim();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setSumTriggerTimeText(StatisticsTypeTabItem.this.sumTriggerTimeComment);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.countByTriggerButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.countByTriggerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.countByTriggerButton.setText(Messages.getString(MessageIds.GDE_MSGT0567));
				this.countByTriggerButton.setBounds(125, 80, 170, 20);
				this.countByTriggerButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0568));
				this.countByTriggerButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "countByTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.isCountByTrigger = StatisticsTypeTabItem.this.countByTriggerButton.getSelection() == false ? null : true;
						StatisticsTypeTabItem.this.countByTriggerText.setEnabled(StatisticsTypeTabItem.this.isCountByTrigger != null);
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setCountByTrigger(StatisticsTypeTabItem.this.isCountByTrigger);
							StatisticsTypeTabItem.this.triggerRefOrdinalText.setEnabled(StatisticsTypeTabItem.this.countByTriggerButton.getSelection());
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
							if (StatisticsTypeTabItem.this.isCountByTrigger == null) {
								StatisticsTypeTabItem.this.statisticsType.setCountTriggerText(null);
							}
							else {
								if (StatisticsTypeTabItem.this.statisticsType.getTrigger() == null) {
									StatisticsTypeTabItem.this.triggerRefOrdinalButton.setSelection(true);
									handleTriggerRefOrdinalSelectionEvent();
								}
							}
						}
					}
				});
			}
			{
				this.countByTriggerText = new Text(this.statisticsComposite, SWT.BORDER);
				this.countByTriggerText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.countByTriggerText.setBounds(360, 80, 325, 20);
				this.countByTriggerText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "countByTriggerText.keyReleased, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.countByTriggerComment = StatisticsTypeTabItem.this.countByTriggerText.getText().trim();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setCountTriggerText(StatisticsTypeTabItem.this.countByTriggerComment);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.triggerRefOrdinalButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.triggerRefOrdinalButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.triggerRefOrdinalButton.setText(Messages.getString(MessageIds.GDE_MSGT0569));
				this.triggerRefOrdinalButton.setBounds(125, 105, 118, 20);
				this.triggerRefOrdinalButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0570));
				this.triggerRefOrdinalButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "isTriggerRefOrdinalButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						handleTriggerRefOrdinalSelectionEvent();
					}
				});
			}
			{
				this.triggerRefOrdinalCombo = new CCombo(this.statisticsComposite, SWT.BORDER);
				this.triggerRefOrdinalCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.triggerRefOrdinalCombo.setBounds(245, 105, 110, 20);
				this.triggerRefOrdinalCombo.setEnabled(false);
				this.triggerRefOrdinalCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
				this.triggerRefOrdinalCombo.setForeground(DataExplorer.getInstance().COLOR_BLACK);
			}
			{
				this.triggerRefOrdinalText = new Text(this.statisticsComposite, SWT.BORDER);
				this.triggerRefOrdinalText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.triggerRefOrdinalText.setBounds(360, 105, 325, 20);
				this.triggerRefOrdinalText.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0573));
				this.triggerRefOrdinalText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "triggerRefOrdinalText.keyReleased, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.triggerRefOrdinalComment = StatisticsTypeTabItem.this.triggerRefOrdinalText.getText();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setComment(StatisticsTypeTabItem.this.triggerRefOrdinalComment);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.isSumByTriggerRefOrdinalButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.isSumByTriggerRefOrdinalButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.isSumByTriggerRefOrdinalButton.setText(Messages.getString(MessageIds.GDE_MSGT0571));
				this.isSumByTriggerRefOrdinalButton.setBounds(125, 130, 118, 20);
				this.isSumByTriggerRefOrdinalButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0572));
				this.isSumByTriggerRefOrdinalButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "isSumByTriggerRefOrdinalButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						if (StatisticsTypeTabItem.this.isSumByTriggerRefOrdinalButton.getSelection()) {
							StatisticsTypeTabItem.this.triggerRefOrdinal = StatisticsTypeTabItem.this.sumByTriggerRefOrdinal = StatisticsTypeTabItem.this.getTriggerReferenceOrdinal();
							if (StatisticsTypeTabItem.this.statisticsType != null) {
								StatisticsTypeTabItem.this.statisticsType.setSumByTriggerRefOrdinal(StatisticsTypeTabItem.this.sumByTriggerRefOrdinal);
								StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
								StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
							}
							StatisticsTypeTabItem.this.sumTriggerText.setEnabled(true);
						}
						else {
							StatisticsTypeTabItem.this.sumByTriggerRefOrdinal = null;
							if (StatisticsTypeTabItem.this.statisticsType != null) {
								StatisticsTypeTabItem.this.statisticsType.setSumByTriggerRefOrdinal(StatisticsTypeTabItem.this.triggerRefOrdinal);
								StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
								StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
							}
							StatisticsTypeTabItem.this.sumTriggerText.setEnabled(false);
						}
					}
				});
			}
			{
				this.sumByTriggerRefOrdinalCombo = new CCombo(this.statisticsComposite, SWT.BORDER);
				this.sumByTriggerRefOrdinalCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.sumByTriggerRefOrdinalCombo.setBounds(245, 130, 110, 20);
				this.sumByTriggerRefOrdinalCombo.setEnabled(false);
				this.sumByTriggerRefOrdinalCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
				this.sumByTriggerRefOrdinalCombo.setForeground(DataExplorer.getInstance().COLOR_BLACK);
			}
			{
				this.sumTriggerText = new Text(this.statisticsComposite, SWT.BORDER);
				this.sumTriggerText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.sumTriggerText.setBounds(360, 130, 325, 20);
				this.sumTriggerText.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0573));
				this.sumTriggerText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "sumTriggerText.keyReleased, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.sumTriggerComment = StatisticsTypeTabItem.this.sumTriggerText.getText();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setSumTriggerText(StatisticsTypeTabItem.this.sumTriggerComment);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.sumBySecondaryTriggerRefOrdinalCombo = new CCombo(this.statisticsComposite, SWT.BORDER);
				this.sumBySecondaryTriggerRefOrdinalCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.sumBySecondaryTriggerRefOrdinalCombo.setBounds(245, 155, 110, 20);
				this.sumBySecondaryTriggerRefOrdinalCombo.setEnabled(false);
				this.sumBySecondaryTriggerRefOrdinalCombo.setBackground(DataExplorer.getInstance().COLOR_WHITE);
				this.sumBySecondaryTriggerRefOrdinalCombo.setForeground(DataExplorer.getInstance().COLOR_BLACK);
			}
			{
				this.isRatioRefOrdinalButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.isRatioRefOrdinalButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.isRatioRefOrdinalButton.setText(Messages.getString(MessageIds.GDE_MSGT0574));
				this.isRatioRefOrdinalButton.setBounds(125, 180, 118, 20);
				this.isRatioRefOrdinalButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0575));
				this.isRatioRefOrdinalButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "isRatioRefOrdinalButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.isRatioRefOrdinal = StatisticsTypeTabItem.this.isRatioRefOrdinalButton.getSelection();
						StatisticsTypeTabItem.this.ratioRefOrdinalCombo.setEnabled(StatisticsTypeTabItem.this.isRatioRefOrdinal);
						StatisticsTypeTabItem.this.ratioText.setEnabled(StatisticsTypeTabItem.this.isRatioRefOrdinal);
						if (StatisticsTypeTabItem.this.statisticsType != null && !StatisticsTypeTabItem.this.isRatioRefOrdinal) {
							StatisticsTypeTabItem.this.statisticsType.setRatioRefOrdinal(StatisticsTypeTabItem.this.ratioRefOrdinalCombo.getSelectionIndex());
							StatisticsTypeTabItem.this.ratioText.setText(StatisticsTypeTabItem.this.ratioText.getText());
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
						else {
							if (StatisticsTypeTabItem.this.statisticsType != null) {
								StatisticsTypeTabItem.this.statisticsType.setRatioRefOrdinal(null);
							}
							StatisticsTypeTabItem.this.ratioText.setText(GDE.STRING_EMPTY);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.ratioRefOrdinalCombo = new CCombo(this.statisticsComposite, SWT.BORDER);
				this.ratioRefOrdinalCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.ratioRefOrdinalCombo.setBounds(245, 180, 110, 20);
				this.ratioRefOrdinalCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "ratioRefOrdinalCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.ratioRefOrdinal = StatisticsTypeTabItem.this.ratioRefOrdinalCombo.getSelectionIndex();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setRatioRefOrdinal(StatisticsTypeTabItem.this.ratioRefOrdinal);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.ratioText = new Text(this.statisticsComposite, SWT.BORDER);
				this.ratioText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.ratioText.setBounds(360, 180, 325, 20);
				this.ratioText.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0576));
				this.ratioText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "ratioText.keyReleased, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.ratioComment = StatisticsTypeTabItem.this.ratioText.getText();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setRatioText(StatisticsTypeTabItem.this.ratioComment);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
						}
					}
				});
			}
			{
				this.isIntegrateByTriggerButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.isIntegrateByTriggerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.isIntegrateByTriggerButton.setText(Messages.getString(MessageIds.GDE_MSGT0959));
				this.isIntegrateByTriggerButton.setBounds(125, 205, 118, 20);
				this.isIntegrateByTriggerButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0960));
				this.isIntegrateByTriggerButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "isIntegrateByTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						StatisticsTypeTabItem.this.isIntegrateByTrigger = StatisticsTypeTabItem.this.isIntegrateByTriggerButton.getSelection();
						if (StatisticsTypeTabItem.this.statisticsType != null && StatisticsTypeTabItem.this.isIntegrateByTrigger) {
							StatisticsTypeTabItem.this.statisticsType.setIntegrateByTrigger(StatisticsTypeTabItem.this.isIntegrateByTrigger);
						}
						else {
							if (StatisticsTypeTabItem.this.statisticsType != null) {
								StatisticsTypeTabItem.this.statisticsType.setIntegrateByTrigger(null);
							}
						}
						StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
					}
				});
			}
			this.scrolledComposite.setContent(this.statisticsComposite);
			this.statisticsComposite.setSize(700, 235);
			this.statisticsComposite.layout();

			initialize();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * enable the context menu to create missing tab items
	 * @param enable
	 */
	public void enableContextMenu(boolean enable) {
		if (enable && (this.popupMenu == null || this.contextMenu == null)) {
			this.popupMenu = new Menu(this.channelConfigMeasurementPropertiesTabFolder.getShell(), SWT.POP_UP);
			//this.popupMenu = SWTResourceManager.getMenu("MeasurementContextmenu", this.channelConfigMeasurementPropertiesTabFolder.getShell(), SWT.POP_UP);
			this.contextMenu = new MeasurementContextmenu(this.popupMenu, this.measurementTypeTabItem, this.channelConfigMeasurementPropertiesTabFolder);
			this.contextMenu.create();
		}
		else if (this.popupMenu != null) {
			this.popupMenu.dispose();
			this.popupMenu = null;
			this.contextMenu = null;
		}
		this.statisticsComposite.setMenu(this.popupMenu);
		this.statisticsMinButton.setMenu(this.popupMenu);
		this.statisticsAvgButton.setMenu(this.popupMenu);
		this.statisticsMaxButton.setMenu(this.popupMenu);
		this.statisticsSigmaButton.setMenu(this.popupMenu);
		this.secondaryTriggerButton.setMenu(this.popupMenu);
		this.triggerLevelButton.setMenu(this.popupMenu);
		this.triggerLevelLabel.setMenu(this.popupMenu);
		this.isGreaterButton.setMenu(this.popupMenu);
		this.minTimeSecLabel.setMenu(this.popupMenu);
		this.countByTriggerButton.setMenu(this.popupMenu);
		this.triggerRefOrdinalButton.setMenu(this.popupMenu);
		this.isSumByTriggerRefOrdinalButton.setMenu(this.popupMenu);
		this.isRatioRefOrdinalButton.setMenu(this.popupMenu);
	}

	/**
	 * Handle the event while isTriggerRefOrdinalButton is selected
	 * If trigger of this measurement is not defined a reference to the measurement defining the trigger is required
	 */
	private void handleTriggerRefOrdinalSelectionEvent() {
		if (StatisticsTypeTabItem.this.triggerRefOrdinalButton.getSelection()) {
			//StatisticsTypeTabItem.this.triggerRefOrdinalCombo.setEnabled(true);
			StatisticsTypeTabItem.this.triggerRefOrdinal = StatisticsTypeTabItem.this.getTriggerReferenceOrdinal();
			if (StatisticsTypeTabItem.this.statisticsType != null) {
				StatisticsTypeTabItem.this.statisticsType.setTriggerRefOrdinal(StatisticsTypeTabItem.this.triggerRefOrdinal);
			}
			StatisticsTypeTabItem.this.isIntegrateByTriggerButton.setEnabled(true);
		}
		else {
			//StatisticsTypeTabItem.this.triggerRefOrdinalCombo.setEnabled(false);
			if (StatisticsTypeTabItem.this.statisticsType != null) {
				StatisticsTypeTabItem.this.statisticsType.setTriggerRefOrdinal(StatisticsTypeTabItem.this.triggerRefOrdinal = null);
				StatisticsTypeTabItem.this.isIntegrateByTriggerButton.setSelection(false);
				StatisticsTypeTabItem.this.isIntegrateByTriggerButton.setEnabled(false);
				StatisticsTypeTabItem.this.statisticsType.setIntegrateByTrigger(null);
			}
		}
		StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
		StatisticsTypeTabItem.this.propsEditor.enableSaveButton(true);
	}

	/**
	 * initialize widget states
	 */
	private void initialize() {
		StatisticsTypeTabItem.this.statisticsMinButton.setSelection(StatisticsTypeTabItem.this.statisticsMin == null ? StatisticsTypeTabItem.this.statisticsMin = false
				: StatisticsTypeTabItem.this.statisticsMin);
		StatisticsTypeTabItem.this.statisticsAvgButton.setSelection(StatisticsTypeTabItem.this.statisticsAvg == null ? StatisticsTypeTabItem.this.statisticsAvg = false
				: StatisticsTypeTabItem.this.statisticsAvg);
		StatisticsTypeTabItem.this.statisticsMaxButton.setSelection(StatisticsTypeTabItem.this.statisticsMax == null ? StatisticsTypeTabItem.this.statisticsMax = false
				: StatisticsTypeTabItem.this.statisticsMax);
		StatisticsTypeTabItem.this.statisticsSigmaButton.setSelection(StatisticsTypeTabItem.this.statisticsSigma == null ? StatisticsTypeTabItem.this.statisticsSigma = false
				: StatisticsTypeTabItem.this.statisticsSigma);

		if (StatisticsTypeTabItem.this.statisticsType != null) {
			StatisticsTypeTabItem.this.updateTriggerDependent(StatisticsTypeTabItem.this.isSomeTriggerDefined = StatisticsTypeTabItem.this.isSomeTriggerDefined());
		}
		if (StatisticsTypeTabItem.this.triggerLevel != null) {
			StatisticsTypeTabItem.this.triggerLevelButton.setSelection(true);
			StatisticsTypeTabItem.this.triggerLevelCombo.select(StatisticsTypeTabItem.this.triggerLevel);
			StatisticsTypeTabItem.this.triggerCommentText.setText(StatisticsTypeTabItem.this.triggerComment == null ? GDE.STRING_EMPTY : StatisticsTypeTabItem.this.triggerComment);
			StatisticsTypeTabItem.this.isGreaterButton
					.setSelection(StatisticsTypeTabItem.this.isGreater == null ? StatisticsTypeTabItem.this.isGreater = true : StatisticsTypeTabItem.this.isGreater);
			StatisticsTypeTabItem.this.minTimeSecCombo
					.select((StatisticsTypeTabItem.this.minTimeSec == null ? StatisticsTypeTabItem.this.minTimeSec = 1 : StatisticsTypeTabItem.this.minTimeSec) - 1);
		}
		StatisticsTypeTabItem.this.sumTriggerTimeButton.setSelection(StatisticsTypeTabItem.this.isSumTriggerTime == null ? false : StatisticsTypeTabItem.this.isSumTriggerTime);
		StatisticsTypeTabItem.this.sumTriggerTimeText.setText(StatisticsTypeTabItem.this.sumTriggerTimeComment == null ? GDE.STRING_EMPTY : StatisticsTypeTabItem.this.sumTriggerTimeComment);
		StatisticsTypeTabItem.this.countByTriggerButton.setSelection(StatisticsTypeTabItem.this.isCountByTrigger == null ? false : StatisticsTypeTabItem.this.isCountByTrigger);
		StatisticsTypeTabItem.this.countByTriggerText.setText(StatisticsTypeTabItem.this.countByTriggerComment == null ? GDE.STRING_EMPTY : StatisticsTypeTabItem.this.countByTriggerComment);
		StatisticsTypeTabItem.this.triggerRefOrdinalButton.setSelection(StatisticsTypeTabItem.this.triggerRefOrdinal != null);
		StatisticsTypeTabItem.this.triggerRefOrdinalCombo.select(StatisticsTypeTabItem.this.triggerRefOrdinal == null ? StatisticsTypeTabItem.this.getTriggerReferenceOrdinal()
				: StatisticsTypeTabItem.this.triggerRefOrdinal);
		StatisticsTypeTabItem.this.triggerRefOrdinalText.setText(StatisticsTypeTabItem.this.triggerRefOrdinalComment != null ? StatisticsTypeTabItem.this.triggerRefOrdinalComment
				: GDE.STRING_EMPTY);
		StatisticsTypeTabItem.this.isSumByTriggerRefOrdinalButton.setSelection(StatisticsTypeTabItem.this.sumByTriggerRefOrdinal != null);
		StatisticsTypeTabItem.this.sumByTriggerRefOrdinalCombo.select(StatisticsTypeTabItem.this.sumByTriggerRefOrdinal == null ? StatisticsTypeTabItem.this.getTriggerReferenceOrdinal()
				: StatisticsTypeTabItem.this.sumByTriggerRefOrdinal);
		StatisticsTypeTabItem.this.sumBySecondaryTriggerRefOrdinalCombo.select(StatisticsTypeTabItem.this.sumBySecondaryTriggerRefOrdinal == null ? StatisticsTypeTabItem.this.getTriggerReferenceOrdinal()
				: StatisticsTypeTabItem.this.sumBySecondaryTriggerRefOrdinal);
		StatisticsTypeTabItem.this.sumTriggerText.setText(StatisticsTypeTabItem.this.sumTriggerComment == null ? GDE.STRING_EMPTY : StatisticsTypeTabItem.this.sumTriggerComment);
		StatisticsTypeTabItem.this.isRatioRefOrdinalButton.setSelection(StatisticsTypeTabItem.this.ratioRefOrdinal != null);
		StatisticsTypeTabItem.this.ratioRefOrdinalCombo.select(StatisticsTypeTabItem.this.ratioRefOrdinal == null ? 0 : StatisticsTypeTabItem.this.ratioRefOrdinal);
		StatisticsTypeTabItem.this.ratioText.setText(StatisticsTypeTabItem.this.ratioComment == null ? GDE.STRING_EMPTY : StatisticsTypeTabItem.this.ratioComment);
		StatisticsTypeTabItem.this.isIntegrateByTriggerButton.setSelection(StatisticsTypeTabItem.this.isIntegrateByTrigger != null ? StatisticsTypeTabItem.this.isIntegrateByTrigger : false);
		StatisticsTypeTabItem.this.isIntegrateByTriggerButton.setEnabled(StatisticsTypeTabItem.this.triggerRefOrdinal != null);

		//StatisticsTypeTabItem.this.enableContextMenu(true);
	}

}
