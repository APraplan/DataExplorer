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
package gde.ui.tab;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import gde.GDE;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.CurveSelectorContextMenu;
import gde.ui.tab.GraphicsWindow.GraphicsType;

/**
 * This class defines a composite with a header (Curve Selector, ..) and a table with checkable table items
 * The table has a popup menu to manipulate properties of the items behind the table items
 * @author Winfried Brügmann
 */
public class SelectorComposite extends Composite {
	final static Logger							log											= Logger.getLogger(SelectorComposite.class.getName());

	final DataExplorer							application							= DataExplorer.getInstance();
	final Channels									channels								= Channels.getInstance();
	final SashForm									parent;
	final GraphicsType							graphicsType;
	final String										headerText;
	final Menu											popupmenu;
	final CurveSelectorContextMenu	contextMenu;

	int															headerTextExtentFactor	= 9;
	Button													curveSelectorHeader;
	int															initialSelectorHeaderWidth;
	int															selectorColumnWidth;
	Table														curveSelectorTable;
	TableColumn											tableSelectorColumn;

	int															oldSelectorColumnWidth	= 0;
	Point														oldSize									= new Point(0, 0);

	/**
	 * @param useParent
	 * @param useGraphicsType
	 * @param useHeaderText
	 */
	public SelectorComposite(final SashForm useParent, GraphicsType useGraphicsType, final String useHeaderText) {
		super(useParent, SWT.NONE);
		//this = new Composite(this.graphicSashForm, SWT.NONE);
		this.parent = useParent;
		this.graphicsType = useGraphicsType;
		this.headerText = useHeaderText;
		SWTResourceManager.registerResourceUser(this);

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new CurveSelectorContextMenu();
		this.contextMenu.createMenu(SelectorComposite.this.popupmenu);

		initGUI();
	}

	void initGUI() {
		// curveSelector

		FormLayout curveSelectorLayout = new FormLayout();
		this.setLayout(curveSelectorLayout);
		GridData curveSelectorLData = new GridData();
		this.setLayoutData(curveSelectorLData);
		this.setBackground(this.application.COLOR_BACKGROUND);
		this.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent evt) {
				if (SelectorComposite.log.isLoggable(Level.FINEST)) SelectorComposite.log.log(Level.FINEST, "curveSelector.helpRequested " + evt); //$NON-NLS-1$
				SelectorComposite.this.application.openHelpDialog("", "HelpInfo_41.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		{
			this.curveSelectorHeader = new Button(this, SWT.CHECK | SWT.LEFT);
			this.curveSelectorHeader.setText(Messages.getString(MessageIds.GDE_MSGT0254));
			this.curveSelectorHeader.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0671));
			this.curveSelectorHeader.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
			this.curveSelectorHeader.pack();
			this.initialSelectorHeaderWidth = this.curveSelectorHeader.getSize().x + 8;
			FormData curveSelectorHeaderLData = new FormData();
			curveSelectorHeaderLData.width = this.initialSelectorHeaderWidth;
			curveSelectorHeaderLData.height = 25;
			curveSelectorHeaderLData.left = new FormAttachment(0, 1000, GDE.IS_WINDOWS ? 6 : 0);
			curveSelectorHeaderLData.top = new FormAttachment(0, 1000, 0);
			this.curveSelectorHeader.setLayoutData(curveSelectorHeaderLData);
			this.curveSelectorHeader.setBackground(this.application.COLOR_BACKGROUND);
			this.curveSelectorHeader.setForeground(this.application.COLOR_FOREGROUND);
			this.curveSelectorHeader.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (SelectorComposite.log.isLoggable(Level.FINEST)) SelectorComposite.log.log(Level.WARNING, "curveSelectorHeader.widgetSelected, event=" + evt); //$NON-NLS-1$
					SelectorComposite.this.application.clearMeasurementModes();
					if (!SelectorComposite.this.curveSelectorHeader.getSelection()) {
						//use this check button to deselect all selected curves
						for (TableItem tableItem : SelectorComposite.this.curveSelectorTable.getItems()) {
							if (tableItem.getChecked()) toggleRecordSelection(tableItem, false, false);
						}
					}
					else {
						for (TableItem tableItem : SelectorComposite.this.curveSelectorTable.getItems()) {
							if (!tableItem.getChecked()) toggleRecordSelection(tableItem, false, true);
						}
					}
					doUpdateCurveSelectorTable();
					SelectorComposite.this.application.updateAllTabs(true, false);
				}
			});
		}
		{
			this.curveSelectorTable = new Table(this, SWT.FULL_SELECTION | SWT.SINGLE | SWT.CHECK);
			this.curveSelectorTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.curveSelectorTable.setLinesVisible(true);
			FormData curveTableLData = new FormData();
			curveTableLData.width = 82;
			curveTableLData.height = 457;
			curveTableLData.left = new FormAttachment(0, 1000, 0);
			curveTableLData.top = new FormAttachment(0, 1000, 25);
			curveTableLData.bottom = new FormAttachment(1000, 1000, 0);
			curveTableLData.right = new FormAttachment(1000, 1000, 0);
			this.curveSelectorTable.setLayoutData(curveTableLData);
			this.curveSelectorTable.setMenu(this.popupmenu);
			this.curveSelectorTable.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (SelectorComposite.log.isLoggable(Level.FINEST)) SelectorComposite.log.log(Level.FINEST, "curveSelectorTable.widgetSelected, event=" + evt); //$NON-NLS-1$
					SelectorComposite.this.resetMeasuring();
					if (evt != null && evt.item != null && !evt.item.isDisposed()) {
						toggleRecordSelection((TableItem) evt.item, true, false);
						SelectorComposite.this.application.updateAllTabs(true, false);
					}
				}
			});
			{
				synchronized (this) {
					this.tableSelectorColumn = new TableColumn(this.curveSelectorTable, SWT.LEAD);
					this.tableSelectorColumn.setWidth(this.selectorColumnWidth);
				}
			}

//WB 2018-02-28 remove listener displaying measurement descriptions, its only used with HoTTAdapter* and disturbs several usage cases
//			final Listener labelListener = new Listener() {
//				public void handleEvent(Event event) {
//					Label label = (Label) event.widget;
//					Shell shell = label.getShell();
//					switch (event.type) {
//					case SWT.MouseDown:
//						Event e = new Event();
//						e.item = (TableItem) label.getData("_TABLEITEM");
//						// Assuming table is single select, set the selection as if
//						// the mouse down event went through to the table
//						curveSelectorTable.setSelection(new TableItem[] { (TableItem) e.item });
//						curveSelectorTable.notifyListeners(SWT.Selection, e);
//						shell.dispose();
//						curveSelectorTable.setFocus();
//						break;
//					case SWT.MouseExit:
//						shell.dispose();
//						break;
//					}
//				}
//			};
//
//			Listener tableListener = new Listener() {
//				Shell	toolTip		= null;
//				Label	label	= null;
//
//				public void handleEvent(Event event) {
//					switch (event.type) {
//					case SWT.Dispose:
//					case SWT.KeyDown:
//					case SWT.MouseMove: {
//						if (toolTip == null)
//							break;
//						toolTip.dispose();
//						toolTip = null;
//						label = null;
//						break;
//					}
//
//					case SWT.MouseHover: {
//						TableItem item = curveSelectorTable.getItem(new Point(event.x, event.y));
//
//						if (item != null) {
//							if (toolTip != null && !toolTip.isDisposed()) toolTip.dispose();
//							Record record  = application.getActiveRecordSet().get(item.getText());
//							if (record != null && record.getDevice().getMeasurement(record.getParent().getChannelConfigNumber(), record.getOrdinal()).getLabel() != null) {
//								toolTip = new Shell(curveSelectorTable.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
//								FillLayout layout = new FillLayout();
//								layout.marginWidth = 2;
//								toolTip.setLayout(layout);
//								label = new Label(toolTip, SWT.NONE);
//								label.setData("_TABLEITEM", item);
//								label.setText(record.getDevice().getMeasurementLabelReplacement(record.getParent().getChannelConfigNumber(), record.getOrdinal()));
//								label.addListener(SWT.MouseExit, labelListener);
//								label.addListener(SWT.MouseDown, labelListener);
//								Point size = toolTip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
//								Rectangle rect = item.getBounds(0);
//								Point pt = curveSelectorTable.toDisplay(rect.x, rect.y);
//								toolTip.setBounds(pt.x, pt.y, size.x, size.y);
//								toolTip.setVisible(true);
//							}
//						}
//					}
//					}
//				}
//			};
//
//			this.curveSelectorTable.addListener(SWT.MouseMove, tableListener);
//			this.curveSelectorTable.addListener(SWT.MouseHover, tableListener);
		}
	}

	/**
	 * executes the update of the curve selector table
	 */
	public synchronized void doUpdateCurveSelectorTable() {
		this.application.updateAllTabs(true, false);
		int itemWidth = this.initialSelectorHeaderWidth;
		RecordSet recordSet = null;
		switch (this.graphicsType) {
		case COMPARE:
			recordSet = this.application.getCompareSet();
			break;

		case UTIL:
			recordSet = this.application.getUtilitySet();
			break;

		case NORMAL:
		default:
			recordSet = this.channels.getActiveChannel() != null ? this.channels.getActiveChannel().getActiveRecordSet() : null;
			break;
		}
		if (recordSet != null && recordSet.size() > 0) {
			if (SelectorComposite.log.isLoggable(Level.FINE)) SelectorComposite.log.log(Level.FINE, recordSet.getName());
			this.curveSelectorTable.removeAll();
			int checkBoxWidth = 20;
			int textSize = 10;
			boolean isOneVisible = false;
			for (int i = 0; i < recordSet.size(); ++i) {
				Record record = recordSet.get(i);
				if (record != null) {
					if (SelectorComposite.log.isLoggable(Level.FINER)) SelectorComposite.log.log(Level.FINER, record.getName());
					textSize = record.getName().length() * 8;
					if (itemWidth < (textSize + checkBoxWidth)) itemWidth = textSize + checkBoxWidth;
					//if (log.isLoggable(Level.FINE)) log.log(Level.FINE, item.getText() + " " + itemWidth);
					if (record.isDisplayable()) {
						TableItem item = new TableItem(this.curveSelectorTable, SWT.NULL);
						item.setForeground(SWTResourceManager.getColor(record.getRGB()));
						item.setText(record.getName());
						if (record.isVisible()) {
							isOneVisible = true;
							item.setChecked(true);
							item.setData(DataExplorer.OLD_STATE, true);
							item.setData(GraphicsWindow.GRAPHICS_TYPE, this.graphicsType);
						}
						else {
							item.setChecked(false);
							item.setData(DataExplorer.OLD_STATE, false);
							item.setData(GraphicsWindow.GRAPHICS_TYPE, this.graphicsType);
						}
						setHeaderSelection(isOneVisible);
					}
				}
			}
			this.selectorColumnWidth = itemWidth;
			if (SelectorComposite.log.isLoggable(Level.FINE)) SelectorComposite.log.log(Level.FINE, "*curveSelectorTable width = " + this.selectorColumnWidth); //$NON-NLS-1$
			recordSet.updateVisibleAndDisplayableRecordsForTable();
		}
		else {
			this.curveSelectorTable.removeAll();
			this.selectorColumnWidth = this.initialSelectorHeaderWidth;
			setHeaderSelection(false);
		}

		if (this.oldSelectorColumnWidth != this.selectorColumnWidth) {
			this.curveSelectorHeader.setSize(this.selectorColumnWidth - 1, this.curveSelectorHeader.getSize().y);
			this.tableSelectorColumn.setWidth(this.selectorColumnWidth - 2);
			this.oldSelectorColumnWidth = this.selectorColumnWidth;
			this.application.setGraphicsSashFormWeights(this.selectorColumnWidth, this.graphicsType);
		}
		this.curveSelectorTable.redraw();
		if (SelectorComposite.log.isLoggable(Level.FINER)) SelectorComposite.log.log(Level.FINER, "curveSelectorTable width = " + this.selectorColumnWidth); //$NON-NLS-1$
	}

	/**
	 * @param enable
	 */
	public void setHeaderSelection(boolean enable) {
		this.curveSelectorHeader.setSelection(enable);
	}

	/**
	 * @return the selectorColumnWidth
	 */
	public int getSelectorColumnWidth() {
		synchronized (this) {
			return this.selectorColumnWidth;
		}
	}

	/**
	 * toggles selection state of a record
	 * @param item table were selection need toggle state
	 */
	public void toggleRecordSelection(TableItem item, boolean isTableSelection, boolean forceVisible) {
		String recordName = item.getText();
		if (SelectorComposite.log.isLoggable(Level.FINE)) SelectorComposite.log.log(Level.FINE, "selected = " + recordName); //$NON-NLS-1$
		SelectorComposite.this.popupmenu.setData(DataExplorer.RECORD_NAME, recordName);
		SelectorComposite.this.popupmenu.setData(DataExplorer.CURVE_SELECTION_ITEM, item);
		if (!isTableSelection || item.getChecked() != (Boolean) item.getData(DataExplorer.OLD_STATE)) {
			if (SelectorComposite.log.isLoggable(Level.FINE)) SelectorComposite.log.log(Level.FINE, "selection state changed = " + recordName); //$NON-NLS-1$
			Record activeRecord;
			switch (SelectorComposite.this.graphicsType) {
			case COMPARE:
				activeRecord = SelectorComposite.this.application.getCompareSet().get(recordName);
				break;

			case UTIL:
				activeRecord = SelectorComposite.this.application.getUtilitySet().get(recordName);
				break;

			default:
				RecordSet activeRecordSet = SelectorComposite.this.channels.getActiveChannel().getActiveRecordSet();
				activeRecord = activeRecordSet != null ? activeRecordSet.get(recordName) : null;
				break;
			}
			if (activeRecord != null) {
				//activeRecord.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
				if (isTableSelection && item.getChecked() || forceVisible) {
					activeRecord.setVisible(true);
					SelectorComposite.this.popupmenu.getItem(0).setSelection(true);
					item.setData(DataExplorer.OLD_STATE, true);
					item.setData(GraphicsWindow.GRAPHICS_TYPE, SelectorComposite.this.graphicsType);
					setHeaderSelection(true);
				}
				else {
					activeRecord.setVisible(false);
					SelectorComposite.this.popupmenu.getItem(0).setSelection(false);
					item.setData(DataExplorer.OLD_STATE, false);
					item.setData(GraphicsWindow.GRAPHICS_TYPE, SelectorComposite.this.graphicsType);
				}
				activeRecord.getParent().syncScaleOfSyncableRecords();
				activeRecord.getParent().updateVisibleAndDisplayableRecordsForTable();
				if (activeRecord.getParent().getVisibleAndDisplayableRecords().size() == 0) SelectorComposite.this.application.clearMeasurementModes();
			}
		}
	}

	/**
	 * update background/foreground color of the tool bar
	 */
	public void updateColorSchema() {
		this.setBackground(this.application.COLOR_BACKGROUND);
		this.curveSelectorHeader.setBackground(this.application.COLOR_BACKGROUND);
		this.curveSelectorHeader.setForeground(this.application.COLOR_FOREGROUND);

	}

	/**
	 * reset active measuring modes 
	 */
	private void resetMeasuring() {
		if (this.contextMenu != null) contextMenu.resetMeasuring();
		this.application.clearMeasurementModes();
		this.application.resetGraphicsWindowZoomAndMeasurement();
	}

}
