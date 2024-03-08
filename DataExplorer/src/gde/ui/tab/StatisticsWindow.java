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
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.ui.tab;

import java.text.DecimalFormat;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.StatisticsType;
import gde.device.resource.DeviceXmlResource;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;
import gde.ui.menu.TabAreaContextMenu.TabMenuType;
import gde.utils.TimeLine;

/**
 * class to represent statistics data according configuration in device properties XML file
 */
public class StatisticsWindow extends CTabItem {
	final static Logger			log													= Logger.getLogger(StatisticsWindow.class.getName());

	static final String			DELIMITER										= "!";																								//$NON-NLS-1$
	static final String			NO_VALUE										= "    ---    ";																			//$NON-NLS-1$

	Composite								composite;
	Composite								filler;
	Group										descriptionGroup;
	Text										descriptionTextLabel;
	CLabel									minLabel;
	CLabel									maxLabel;
	CLabel									avgLabel;
	Table										dataTable;
	TableColumn							measurementTableColumn;
	TableColumn							unitTableColumn;
	TableColumn							sigmaTableColumn;
	TableColumn							customTableColumn;
	int											customTableColumnWidth			= 0;
	CLabel									sigmaLabel;
	CLabel									extraLabel;
	TableColumn							avgTableColumn;
	TableColumn							maxTableColumn;
	TableColumn							minTableColumn;
	TableColumn							maxTimeTableColumn;
	TableColumn							minTimeTableColumn;

	Menu										popupmenu;
	TabAreaContextMenu			contextMenu;
	Color										innerAreaBackground;
	Color										surroundingBackground;

	// internal display variables
	String									descriptionText							= "";																									//$NON-NLS-1$
	Vector<String>					tabelItemText								= new Vector<String>();

	final boolean						isWindows										= System.getProperty("os.name").startsWith("Windows");
	final int								extentFactor								= 8;																									// factor to calculate column width
	RecordSet								oldRecordSet								= null;
	int											oldNumberDisplayableRecords	= 0;
	final DataExplorer			application;
	final Channels					channels;
	final Settings					settings;
	final CTabFolder				tabFolder;

	final DeviceXmlResource	xmlResource									= DeviceXmlResource.getInstance();

	public StatisticsWindow(CTabFolder currentDisplayTab, int style) {
		super(currentDisplayTab, style);
		SWTResourceManager.registerResourceUser(this);
		this.application = DataExplorer.getInstance();
		this.tabFolder = currentDisplayTab;
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
		this.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 3 : 1), SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.GDE_MSGT0350));

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();
		this.innerAreaBackground = this.settings.getStatisticsInnerAreaBackground();
		this.surroundingBackground = this.settings.getStatisticsSurroundingAreaBackground();
	}

	public void create() {
		try {
			this.composite = new Composite(this.tabFolder, SWT.NONE);
			this.setControl(this.composite);
			this.composite.setLayout(null);
			this.composite.setBackground(this.surroundingBackground);
			this.composite.setMenu(this.popupmenu);
			this.composite.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "composite.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_5.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.composite.addControlListener(new ControlListener() {
				public void controlResized(ControlEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "composite.controlResized evt=" + evt); //$NON-NLS-1$
					StatisticsWindow.this.descriptionGroup.setSize(StatisticsWindow.this.composite.getClientArea().width - 20, 110);
					StatisticsWindow.this.descriptionTextLabel.setSize(StatisticsWindow.this.descriptionGroup.getClientArea().width - 15, StatisticsWindow.this.descriptionGroup.getClientArea().height - 10);
					adaptTableSize();
				}

				public void controlMoved(ControlEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "composite.controlMoved evt=" + evt); //$NON-NLS-1$
				}
			});
			{
				this.descriptionGroup = new Group(this.composite, SWT.NONE);
				this.descriptionGroup.setLayout(null);
				this.descriptionGroup.setBounds(10, 10, 300, 110); // set top,left and maintain the rest by control listener
				this.descriptionGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.descriptionGroup.setText(Messages.getString(MessageIds.GDE_MSGT0351));
				if (!GDE.IS_MAC) this.descriptionGroup.setBackground(this.innerAreaBackground);
				this.descriptionGroup.setMenu(this.popupmenu);
				this.descriptionGroup.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "descriptionGroup.helpRequested " + evt); //$NON-NLS-1$
						DataExplorer.getInstance().openHelpDialog("", "HelpInfo_5.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				this.descriptionGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "descriptionGroup.paintControl, event=" + evt); //$NON-NLS-1$
						StatisticsWindow.this.contextMenu.createMenu(StatisticsWindow.this.popupmenu, TabMenuType.SIMPLE);
						Channel activeChannel = StatisticsWindow.this.channels.getActiveChannel();
						if (activeChannel != null) {
							RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
							if (activeRecordSet != null) {
								String tmpDescriptionText = StatisticsWindow.this.descriptionText = activeChannel.getFileDescription() + "\n--------------------------\n" //$NON-NLS-1$
										+ activeRecordSet.getName() + " :  " + activeRecordSet.getRecordSetDescription(); //$NON-NLS-1$
								if (StatisticsWindow.this.descriptionTextLabel != null && !tmpDescriptionText.equals(StatisticsWindow.this.descriptionTextLabel.getText())) {
									StatisticsWindow.this.descriptionTextLabel.setText(StatisticsWindow.this.descriptionText = tmpDescriptionText);
								}
							}
							else {
								String tmpDescriptionText = GDE.STRING_EMPTY;
								if (StatisticsWindow.this.descriptionTextLabel != null && !tmpDescriptionText.equals(StatisticsWindow.this.descriptionTextLabel.getText())) {
									StatisticsWindow.this.descriptionTextLabel.setText(StatisticsWindow.this.descriptionText = tmpDescriptionText);
								}
							}
						}
					}
				});
				{
					this.descriptionTextLabel = new Text(this.descriptionGroup, SWT.LEFT | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
					this.descriptionTextLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.descriptionTextLabel.setText("recordSetName, (fileDescription), recordSetDescription"); //$NON-NLS-1$
					this.descriptionTextLabel.setBackground(this.innerAreaBackground);
					if (GDE.IS_WINDOWS)	this.descriptionTextLabel.setBounds(10, 20, this.descriptionGroup.getClientArea().width-15, this.descriptionGroup.getClientArea().height-10);
					else							  this.descriptionTextLabel.setBounds( 5, 3 , this.descriptionGroup.getClientArea().width-10, this.descriptionGroup.getClientArea().height-10);
					this.descriptionTextLabel.setEditable(false);
					this.descriptionTextLabel.setMenu(this.popupmenu);
				}
			}
			{
				this.dataTable = new Table(this.composite, SWT.MULTI | SWT.BORDER);
				this.dataTable.setLinesVisible(!this.isWindows);
				this.dataTable.setHeaderVisible(true);
				this.dataTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dataTable.setBounds(10, 150, 300, 100); // set top,left and maintain the rest by control listener
				this.dataTable.setBackground(this.innerAreaBackground);
				this.dataTable.setMenu(this.popupmenu);
				this.dataTable.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "dataTable.helpRequested " + evt); //$NON-NLS-1$
						DataExplorer.getInstance().openHelpDialog("", "HelpInfo_5.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				{
					this.measurementTableColumn = new TableColumn(this.dataTable, SWT.LEFT);
					this.measurementTableColumn.setWidth(180);
					this.measurementTableColumn.setText(Messages.getString(MessageIds.GDE_MSGT0352));
				}
				{
					this.unitTableColumn = new TableColumn(this.dataTable, SWT.LEFT);
					this.unitTableColumn.setWidth(120);
					this.unitTableColumn.setText(Messages.getString(MessageIds.GDE_MSGT0353));
					this.unitTableColumn.setAlignment(SWT.LEFT);
				}
				{
					this.minTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.minTableColumn.setWidth(90);
					this.minTableColumn.setText(Messages.getString(MessageIds.GDE_MSGT0354));
				}
				{
					this.minTimeTableColumn = new TableColumn(this.dataTable, SWT.RIGHT);
					this.minTimeTableColumn.setWidth(110);
					this.minTimeTableColumn.setText(Messages.getString(MessageIds.GDE_MSGT0359) + GDE.STRING_BLANK);
				}
				{
					this.avgTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.avgTableColumn.setWidth(90);
					this.avgTableColumn.setText(Messages.getString(MessageIds.GDE_MSGT0355));
				}
				{
					this.maxTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.maxTableColumn.setWidth(90);
					this.maxTableColumn.setText(Messages.getString(MessageIds.GDE_MSGT0356));
				}
				{
					this.maxTimeTableColumn = new TableColumn(this.dataTable, SWT.RIGHT);
					this.maxTimeTableColumn.setWidth(110);
					this.maxTimeTableColumn.setText(Messages.getString(MessageIds.GDE_MSGT0359) + GDE.STRING_BLANK);
				}
				{
					this.sigmaTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					String sigmaText = Messages.getString(MessageIds.GDE_MSGT0357);
					this.sigmaTableColumn.setText(sigmaText);
					this.sigmaTableColumn.setWidth((sigmaText.length() * this.extentFactor > 90) ? sigmaText.length() * this.extentFactor : 80);
				}
				{
					this.customTableColumn = new TableColumn(this.dataTable, SWT.LEFT);
					this.customTableColumn.setWidth(300);
					this.customTableColumn.setText(Messages.getString(MessageIds.GDE_MSGT0358));
				}
			}
			this.composite.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * update statistics window display data
	 */
	public synchronized void updateStatisticsData(boolean forceUpdate) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "entry updateStatisticsData"); //$NON-NLS-1$
		this.descriptionGroup.redraw();

		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && !activeRecordSet.isZoomMode() && activeRecordSet.getRecordDataSize(true) > 0 && (forceUpdate || !activeRecordSet.equals(oldRecordSet))) {
				// cleanup old data table
				this.dataTable.removeAll();

				this.customTableColumnWidth = 0;
				try {
					String[] displayableRecords = activeRecordSet.getDisplayableRecordNames();
					this.oldNumberDisplayableRecords = displayableRecords.length;

					StringBuilder sb = new StringBuilder();
					this.tabelItemText = new Vector<String>();

					// time
					String time = Messages.getString(MessageIds.GDE_MSGT0234);
					sb.append(time.split(" ")[0]).append(DELIMITER); //$NON-NLS-1$
					sb.append(Messages.getString(MessageIds.GDE_MSGT0359)).append(DELIMITER);
					sb.append("     0      ").append(DELIMITER); //$NON-NLS-1$
					sb.append(NO_VALUE).append(DELIMITER);
					sb.append(NO_VALUE).append(DELIMITER);
					sb.append(TimeLine.getFomatedTime(activeRecordSet.getMaxTime_ms())).append(" ").append(DELIMITER); //$NON-NLS-1$
					sb.append(TimeLine.getFomatedTime(activeRecordSet.getMaxTime_ms())).append(" ").append(DELIMITER); //$NON-NLS-1$
					sb.append(NO_VALUE).append(DELIMITER);
					if (activeRecordSet.isTimeStepConstant()) {
						sb.append(Messages.getString(MessageIds.GDE_MSGT0360)).append(String.format("%6.1f", activeRecordSet.getTime_ms(1))).append(Messages.getString(MessageIds.GDE_MSGT0361)); //$NON-NLS-1$
					}
					this.tabelItemText.add(sb.toString());

					for (String recordName : displayableRecords) {
						Record record = activeRecordSet.get(recordName);
						IDevice device = activeRecordSet.getDevice();
						StatisticsType measurementStatistics = device.getMeasurementStatistic(activeChannel.getNumber(), activeRecordSet.get(recordName).getOrdinal());
						if (measurementStatistics != null) {
							sb = new StringBuilder();
							int triggerRefOrdinal = getTriggerReferenceOrdinal(activeRecordSet, measurementStatistics);
							int triggerSecondaryRefOrdinal = getTriggerSecondaryReferenceOrdinal(activeRecordSet, measurementStatistics);
							if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("updating record = %s trigger ref ordinal = %d trigger secondary ref ordinal = %d", recordName, triggerRefOrdinal, triggerSecondaryRefOrdinal));
							sb.append(record.getName()).append(DELIMITER);
							sb.append("[").append(record.getUnit()).append("]").append(DELIMITER); //$NON-NLS-1$ //$NON-NLS-2$

							if (measurementStatistics.isMin()) {
								if (triggerRefOrdinal >= 0 && triggerSecondaryRefOrdinal >= 0)
									sb.append(formatOutput(record.getFormattedStatisticsValue(record.getMinValueTriggered(triggerRefOrdinal, triggerSecondaryRefOrdinal) / 1000.)));
								else if (triggerRefOrdinal < 0 || record.getMinValueTriggered(triggerRefOrdinal) != Integer.MAX_VALUE)
									sb.append(formatOutput(record.getFormattedStatisticsValue((triggerRefOrdinal < 0 ? record.getRealMinValue() : record.getMinValueTriggered(triggerRefOrdinal)) / 1000.0)));
								else
									sb.append(NO_VALUE).append(DELIMITER).append(NO_VALUE);
								
								if (!sb.toString().endsWith(NO_VALUE)) 
									sb.append(DELIMITER).append(TimeLine.getFomatedTime(record.getRealMinValueTime_ms())).append(GDE.STRING_BLANK);
							}
							else
								sb.append(NO_VALUE).append(DELIMITER).append(NO_VALUE);
							sb.append(DELIMITER);

							if (measurementStatistics.isAvg())
								if (triggerRefOrdinal >= 0 && triggerSecondaryRefOrdinal >= 0)
									sb.append(formatOutput(record.getFormattedStatisticsValue(record.getAvgValueTriggered(triggerRefOrdinal, triggerSecondaryRefOrdinal) / 1000.)));
								else if (triggerRefOrdinal < 0 || record.getAvgValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE)
									sb.append(formatOutput(record.getFormattedStatisticsValue((triggerRefOrdinal < 0 ? record.getAvgValue() : record.getAvgValueTriggered(triggerRefOrdinal)) / 1000.0)));
							else
								sb.append(NO_VALUE);
							sb.append(DELIMITER);

							if (measurementStatistics.isMax()) {
								if (triggerRefOrdinal >= 0 && triggerSecondaryRefOrdinal >= 0)
									sb.append(formatOutput(record.getFormattedStatisticsValue(record.getMaxValueTriggered(triggerRefOrdinal, triggerSecondaryRefOrdinal) / 1000.)));
								else if (triggerRefOrdinal < 0 || record.getMaxValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE)
									sb.append(formatOutput(record.getFormattedStatisticsValue((triggerRefOrdinal < 0 ? record.getRealMaxValue() : record.getMaxValueTriggered(triggerRefOrdinal)) / 1000.0)));
								else
									sb.append(NO_VALUE).append(DELIMITER).append(NO_VALUE);
								
								if (!sb.toString().endsWith(NO_VALUE)) 
									sb.append(DELIMITER).append(TimeLine.getFomatedTime(record.getRealMaxValueTime_ms())).append(GDE.STRING_BLANK);
							}
							else
								sb.append(NO_VALUE).append(DELIMITER).append(NO_VALUE);
							sb.append(DELIMITER);

							if (measurementStatistics.isSigma()) {
								DecimalFormat cdf = new DecimalFormat("0.000"); //$NON-NLS-1$
								if (triggerRefOrdinal >= 0 && triggerSecondaryRefOrdinal >= 0)
									sb.append(formatOutput(record.getFormattedStatisticsValue(record.getSigmaValueTriggered(triggerRefOrdinal, triggerSecondaryRefOrdinal) / 1000.)));
								else if (triggerRefOrdinal < 0 || record.getSigmaValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE)
									sb.append(formatOutput(cdf.format(device.translateValue(record, (triggerRefOrdinal < 0 ? record.getSigmaValue() : record.getSigmaValueTriggered(triggerRefOrdinal)) / 1000.0))));
								else
									sb.append(NO_VALUE).append(DELIMITER).append(NO_VALUE);
							}
							else
								sb.append(NO_VALUE);
							sb.append(DELIMITER);

							// counted trigger events fulfilling the level and time constrains 
							if (measurementStatistics.isCountByTrigger() != null) {
								sb.append(xmlResource.getReplacement(measurementStatistics.getCountTriggerText())).append(" = ") //$NON-NLS-1$
										.append(record.getTriggerRanges() != null ? record.getTriggerRanges().size() : 0).append("; "); //$NON-NLS-1$
							}

							// evaluate sum value within trigger range
							if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
								if (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1) {
									sb.append(xmlResource.getReplacement(measurementStatistics.getSumTriggerText())).append(" = "); //$NON-NLS-1$
									boolean isTriggerLevel = measurementStatistics.getTrigger() != null;
									if (isTriggerLevel)
										sb.append(String.format("%.1f", device.translateDeltaValue(record, record.getSumTriggeredRange() / 1000.)));
									else 
										if (measurementStatistics.getSumBySecondaryTriggerRefOrdinal() != null)
											sb.append(String.format("%.1f", device.translateDeltaValue(record, record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal().intValue(), measurementStatistics.getSumBySecondaryTriggerRefOrdinal().intValue()) / 1000.)));
										else 
											sb.append(String.format("%.1f", device.translateDeltaValue(record, record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal().intValue()) / 1000.)));
									
									sb.append(" [").append(record.getUnit()).append("]; "); //$NON-NLS-1$ //$NON-NLS-2$
								}

								// append ratio text + ratio value
								if (measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
									Integer ratioRefOrdinal = measurementStatistics.getRatioRefOrdinal();
									if (ratioRefOrdinal != null) {
										Record referencedRecord = activeRecordSet.get(ratioRefOrdinal.intValue());
										if (referencedRecord != null) {
											sb.append(xmlResource.getReplacement(measurementStatistics.getRatioText())).append(" = "); //$NON-NLS-1$
											double summarizedValue = device.translateDeltaValue(record, record.getSumTriggeredRange(ratioRefOrdinal) / 1000.0);
											double summarizedValueReferenced = device.translateDeltaValue(referencedRecord, referencedRecord.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal().intValue()) / 1000.0);											
											if (summarizedValueReferenced > 0. && summarizedValue > 0.) {
												double ratio = (summarizedValueReferenced / 1000.0) / summarizedValue;
												sb.append(String.format("%.2f", (ratio < 1.0 ? ratio * 1000 : ratio)));
												sb.append(" [").append(referencedRecord.getUnit()).append("/").append(record.getUnit()).append("]; "); //$NON-NLS-1$ //$NON-NLS-2$											
											}
											else {
												sb.append("0 [").append(referencedRecord.getUnit()).append("/").append(record.getUnit()).append("]; "); //$NON-NLS-1$ //$NON-NLS-2$
											}
										}
									}
								}
							}

							// append global comment
							if (measurementStatistics.getComment() != null && measurementStatistics.getComment().length() > 1)
								if (triggerRefOrdinal >= 0 && triggerSecondaryRefOrdinal >= 0)
									sb.append(String.format("(%s %s+%s) ", xmlResource.getReplacement(measurementStatistics.getComment()), activeRecordSet.get(triggerRefOrdinal).getName(), activeRecordSet.get(triggerSecondaryRefOrdinal).getName()));
								else if (triggerRefOrdinal >= 0)
									sb.append(String.format("(%s %s) ", xmlResource.getReplacement(measurementStatistics.getComment()), activeRecordSet.get(triggerRefOrdinal).getName()));
								else
									sb.append(String.format("%s) ", xmlResource.getReplacement(measurementStatistics.getComment()))); //$NON-NLS-1$ //$NON-NLS-2$
							
							//evaluate integrated value by trigger range
							if (triggerRefOrdinal >= 0 && measurementStatistics.isIntegrateByTrigger()) {
								DecimalFormat cdf = new DecimalFormat("0.00"); //$NON-NLS-1$
								sb.append(String.format(Messages.getString(MessageIds.GDE_MSGT0958), cdf.format(record.getIntegratedValue(triggerRefOrdinal)), record.getUnit()+"min"));
							}

							// append trigger + comment
							if (measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null && measurementStatistics.getSumTriggerTimeText().length() > 1) {
								sb.append("(").append(String.format(xmlResource.getReplacement(measurementStatistics.getTrigger().getComment()), recordName, measurementStatistics.getTrigger().getLevel()/1000.0, activeRecordSet.get(recordName).getUnit(), measurementStatistics.getTrigger().getMinTimeSec())).append(") "); //$NON-NLS-1$ //$NON-NLS-2$
								if (!measurementStatistics.getTrigger().isSecondary())
									this.tabelItemText.set(0, this.tabelItemText.get(0) + (activeRecordSet.isTimeStepConstant() ? ", " : GDE.STRING_EMPTY) + xmlResource.getReplacement(xmlResource.getReplacement(measurementStatistics.getSumTriggerTimeText())) + " = " + record.getTimeSumTriggeredRange());
							}
							else if (measurementStatistics.getSumTriggerTimeText() == null && measurementStatistics.getTrigger() != null && measurementStatistics.getTrigger().getComment() != null && measurementStatistics.getTrigger().getComment().length() > 1 ) {
								sb.append("(").append(String.format(xmlResource.getReplacement(measurementStatistics.getTrigger().getComment()), recordName, measurementStatistics.getTrigger().getLevel()/1000.0, activeRecordSet.get(recordName).getUnit(), measurementStatistics.getTrigger().getMinTimeSec())).append(") "); //$NON-NLS-1$ //$NON-NLS-2$								
							}

							GC displayGC = new GC(this.dataTable.getDisplay());
							int customColumnTextExtent = 15 + displayGC.textExtent(sb.substring(sb.lastIndexOf(DELIMITER) + 1)).x;
							displayGC.dispose();
							this.customTableColumnWidth = customColumnTextExtent > this.customTableColumnWidth ? customColumnTextExtent : this.customTableColumnWidth;

							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, sb.toString());
							this.tabelItemText.add(sb.toString());
						}
					}
				}
				catch (RuntimeException e) {
					log.log(Level.WARNING, e.getMessage(), e);
				}

				// set items (rows) of data table
				TableItem row;
				if (!this.isWindows) this.dataTable.setItemCount(this.dataTable.getItemCount() + 1); // add spacer between header and table enties only
				for (String itemsText : this.tabelItemText) {
					if (this.isWindows) this.dataTable.setItemCount(this.dataTable.getItemCount() + 1); // add spacer line for windows
					row = new TableItem(this.dataTable, SWT.NONE);
					row.setText(itemsText.split(DELIMITER));
				}
				if (oldRecordSet != null && !activeRecordSet.getName().equals(oldRecordSet.getName())) {
					this.descriptionGroup.redraw();
				}
				this.oldRecordSet = activeRecordSet;
			}
			else if (activeRecordSet == null) {
				if (oldRecordSet != null && this.tabelItemText.size() > 0) {
					// cleanup old data table
					this.dataTable.removeAll();
				}
				this.tabelItemText = new Vector<String>();
				this.oldRecordSet = null;
			}
		}
		else {
			if (oldRecordSet != null && this.tabelItemText.size() > 0) {
				// cleanup old data table
				this.dataTable.removeAll();
			}
			this.tabelItemText = new Vector<String>();
			this.oldRecordSet = null;
		}
		adaptTableSize();
		this.dataTable.redraw();
	}

	/**
	 * get the trigger reference ordinal value while checking the referenced record is in state displayable
	 * @param recordSet
	 * @param measurementStatistics
	 * @return -1 if referenced record does not fulfill the criteria required, else the ordinal of the referenced record
	 */
	int getTriggerReferenceOrdinal(RecordSet recordSet, StatisticsType measurementStatistics) {
		int triggerRefOrdinal = -1;
		if (measurementStatistics.getTriggerRefOrdinal() != null && recordSet != null) {
			int tmpOrdinal = measurementStatistics.getTriggerRefOrdinal().intValue();
			Record record = recordSet.get(tmpOrdinal);
			if (record != null && record.isDisplayable()) {
				triggerRefOrdinal = tmpOrdinal;
			}
		}
		return triggerRefOrdinal;
	}

	/**
	 * get the trigger secondary reference ordinal value while checking the referenced record is in state displayable
	 * @param recordSet
	 * @param measurementStatistics
	 * @return -1 if referenced record does not fulfill the criteria required, else the ordinal of the referenced record
	 */
	int getTriggerSecondaryReferenceOrdinal(RecordSet recordSet, StatisticsType measurementStatistics) {
		int triggerSecondaryRefOrdinal = -1;
		if (measurementStatistics.getTriggerSecondaryRefOrdinal() != null && recordSet != null) {
			int tmpOrdinal = measurementStatistics.getTriggerSecondaryRefOrdinal().intValue();
			Record record = recordSet.get(tmpOrdinal);
			if (record != null && record.isDisplayable()) {
				triggerSecondaryRefOrdinal = tmpOrdinal;
			}
		}
		return triggerSecondaryRefOrdinal;
	}

	/**
	 * format value string
	 * @param inDecimalString
	 */
	String formatOutput(String inDecimalString) {
		String[] tmp = inDecimalString.replace('.', ';').replace(',', ';').split(";"); //$NON-NLS-1$
		return tmp.length > 1 ? String.format("%6s.%-5s", tmp[0], tmp[1]) : String.format("%6s%-6s", tmp[0], ".0"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * method to update the data table
	 */
	void updateDataTable() {

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "entry data table update"); //$NON-NLS-1$

		// cleanup old data table
		this.dataTable.removeAll();

		// set items (rows) of data table
		TableItem row;
		if (!this.isWindows) this.dataTable.setItemCount(this.dataTable.getItemCount() + 1); // add spacer between header and table entries only
		for (String itemsText : this.tabelItemText) {
			if (this.isWindows) this.dataTable.setItemCount(this.dataTable.getItemCount() + 1); // add spacer line for windows
			row = new TableItem(this.dataTable, SWT.NONE);
			row.setText(itemsText.split(DELIMITER));
		}
		this.dataTable.redraw();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "exit data table update"); //$NON-NLS-1$
	}

	/**
	 * adapt the size of the data table
	 */
	void adaptTableSize() {
		// adapt custom part width
		int columsWidth = 0;
		for (int i = 0; i < this.dataTable.getColumnCount() - 1; i++) {
			columsWidth += this.dataTable.getColumn(i).getWidth();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "ColumWidth = " + this.dataTable.getColumn(i).getWidth()); //$NON-NLS-1$
		}
		Point tableSize = this.dataTable.computeSize(StatisticsWindow.this.composite.getClientArea().width - 20, SWT.DEFAULT, true);
		//if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "computed size = " + tableSize);
		//tableHeight = tableHeight+150 < this.composite.getClientArea().height ? tableHeight : this.composite.getClientArea().height-150;
		int tableHeight = tableSize.y + 150 < this.composite.getClientArea().height ? tableSize.y : this.composite.getClientArea().height - 150;
		tableHeight = tableHeight > 0 ? tableHeight : 0;
		//if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "tableHeight = " + tableHeight + "/" + (this.composite.getClientArea().height-150));
		this.dataTable.setSize(StatisticsWindow.this.composite.getClientArea().width - 20, tableHeight);

		int customWidthFill = this.dataTable.getClientArea().width - columsWidth;
		this.customTableColumn.setWidth(this.customTableColumnWidth > customWidthFill ? this.customTableColumnWidth : customWidthFill);
		//if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "table width = " + (columsWidth + this.customTableColumn.getWidth()));
	}

	/**
	 * create statistics window content as formated string
	 * 
	 */
	public String getContentAsText() {
		StringBuilder sb = new StringBuilder();
		//header
		sb.append(GDE.NAME_LONG).append(GDE.STRING_MESSAGE_CONCAT).append(Messages.getString(MessageIds.GDE_MSGT0350)).append(GDE.LINE_SEPARATOR).append(GDE.LINE_SEPARATOR);
		//description
		sb.append(Messages.getString(MessageIds.GDE_MSGT0351)).append(GDE.LINE_SEPARATOR);
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			this.descriptionText = activeChannel.getFileDescription() + "\n--------------------------\n"; //$NON-NLS-1$
		}
		else {
			this.descriptionText = Messages.getString(MessageIds.GDE_MSGW0036) + "\n--------------------------\n"; //$NON-NLS-1$
		}
		RecordSet activeRecordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
		if (activeRecordSet != null) {
			this.descriptionText = this.descriptionText + activeRecordSet.getName() + " :  " + activeRecordSet.getRecordSetDescription(); //$NON-NLS-1$
		}
		sb.append(this.descriptionText).append(GDE.LINE_SEPARATOR).append(GDE.LINE_SEPARATOR);
		//table header
		sb.append(String.format("%-18s %-15s %10s  %12s  %10s %-18s  %s", 
				Messages.getString(MessageIds.GDE_MSGT0352), Messages.getString(MessageIds.GDE_MSGT0353), Messages.getString(MessageIds.GDE_MSGT0354), 
				Messages.getString(MessageIds.GDE_MSGT0355), Messages.getString(MessageIds.GDE_MSGT0356), Messages.getString(MessageIds.GDE_MSGT0357), 
				Messages.getString(MessageIds.GDE_MSGT0358))).append(GDE.LINE_SEPARATOR);
		//table data
		for (String tableText : this.tabelItemText) {
			String[] itemsText = tableText.split(DELIMITER);
			sb.append(String.format("%-18s %-15s %12s %12s %12s %12s      ", itemsText[0],itemsText[1],itemsText[2],itemsText[3],itemsText[4],itemsText[5]));
			if (itemsText.length > 6)
				sb.append(itemsText[6]);
			sb.append(GDE.LINE_SEPARATOR);
		}
		return sb.toString();
	}

	/**
	 * create statistics window content as image
	 * @return image with content
	 */
	public Image getContentAsImage() {
		Rectangle bounds = this.tabFolder.getClientArea();
		Image objectImage = new Image(GDE.display, bounds.width, bounds.height);
		GC imageGC = new GC(objectImage);
		this.composite.print(imageGC);
		if (GDE.IS_MAC)
			this.composite.print(imageGC);
		imageGC.dispose();

		return objectImage;
	}

	/**
	 * @param newInnerAreaBackground the innerAreaBackground to set
	 */
	public void setInnerAreaBackground(Color newInnerAreaBackground) {
		this.innerAreaBackground = newInnerAreaBackground;
		this.descriptionGroup.setBackground(newInnerAreaBackground);
		this.descriptionTextLabel.setBackground(newInnerAreaBackground);
		this.descriptionGroup.redraw();
		this.dataTable.setBackground(newInnerAreaBackground);
		this.dataTable.redraw();
	}

	/**
	 * @param newSurroundingBackground the surroundingAreaBackground to set
	 */
	public void setSurroundingAreaBackground(Color newSurroundingBackground) {
		this.composite.setBackground(newSurroundingBackground);
		this.surroundingBackground = newSurroundingBackground;
		this.composite.redraw();
	}
}
