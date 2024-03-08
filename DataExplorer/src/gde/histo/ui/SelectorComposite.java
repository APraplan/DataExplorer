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

    Copyright (c) 2016,2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.histo.ui;

import static java.util.logging.Level.FINEST;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.sun.istack.Nullable;

import gde.GDE;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.ui.AbstractChartWindow.WindowActor;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.CurveSelectorContextMenu;
import gde.utils.ColorUtils;

/**
 * Composite with a header (Curve Selector, ..) and table rows for the curves.
 * The first column holds checkable items and the 2nd one a curve type combobox.
 * The table has a popup menu to manipulate properties of the items behind the table items.
 * @author Thomas Eickert
 */
public final class SelectorComposite extends Composite {
	private final static String			$CLASS_NAME							= SelectorComposite.class.getName();
	private final static Logger			log											= Logger.getLogger($CLASS_NAME);

	private static final int				TEXT_EXTENT_FACTOR			= 6;
	private static final int				YGAP_CHARTSELECTOR			= GDE.IS_WINDOWS ? 2 : 0;
	private static final int				COMBO_COLUMN_ORDINAL		= 1;

	private final DataExplorer			application;
	private final Menu							popupmenu;
	final CurveSelectorContextMenu	contextMenu;

	private Button									curveSelectorHeader;
	private Button									chartSelector, smartSelector, saveTemplate;
	private int											initialSelectorHeaderWidth;
	private int											selectorColumnWidth;
	private Table										curveSelectorTable;
	private TableColumn							tableSelectorColumn;
	private TableColumn							tableCurveTypeColumn;
	private int											initialCurveTypeColumnWidth;
	private int											curveTypeColumnWidth;
	private int											oldSelectorColumnWidth	= 0;

	private TableEditor[]						editors									= new TableEditor[0];

	private WindowActor							windowActor;


	public SelectorComposite(final SashForm useParent, AbstractChartWindow parentWindow) {
		super(useParent, SWT.NONE);
		SWTResourceManager.registerResourceUser(this);

		this.application = DataExplorer.getInstance();
		this.popupmenu = new Menu(DataExplorer.getInstance().getShell(), SWT.POP_UP);
		this.contextMenu = new CurveSelectorContextMenu();
		this.contextMenu.createMenu(this.popupmenu);

		this.windowActor = parentWindow.windowActor;
		initGUI();
	}

	public Button getCurveSelectorHeader() {
		return this.curveSelectorHeader;
	}

	void initGUI() {
		FormLayout curveSelectorLayout = new FormLayout();
		this.setBackground(this.application.COLOR_BACKGROUND);
		this.setLayout(curveSelectorLayout);
		GridData curveSelectorLData = new GridData();
		this.setLayoutData(curveSelectorLData);
		this.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent evt) {
				log.log(FINEST, "helpRequested ", evt);
				DataExplorer.getInstance().openHelpDialog("", "HelpInfo_41.html");
			}
		});
		{
			this.curveSelectorHeader = new Button(this, SWT.CHECK | SWT.LEFT);
			this.curveSelectorHeader.setText(Messages.getString(MessageIds.GDE_MSGT0254));
			this.curveSelectorHeader.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0671));
			this.curveSelectorHeader.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
			this.curveSelectorHeader.pack();
			this.initialSelectorHeaderWidth = this.curveSelectorHeader.getSize().x + 8; // ET +8 don't know why
			FormData curveSelectorHeaderLData = new FormData();
			// ET don't know why curveSelectorHeaderLData.width = this.initialSelectorHeaderWidth;
			curveSelectorHeaderLData.height = AbstractChartWindow.HEADER_ROW_HEIGHT;
			curveSelectorHeaderLData.left = new FormAttachment(0, 1000, GDE.IS_WINDOWS ? 6 : 0);
			curveSelectorHeaderLData.top = new FormAttachment(0, 1000, 0);
			this.curveSelectorHeader.setLayoutData(curveSelectorHeaderLData);
			this.curveSelectorHeader.setBackground(this.application.COLOR_BACKGROUND);
			this.curveSelectorHeader.setForeground(this.application.COLOR_FOREGROUND);
			this.curveSelectorHeader.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.fine(() -> "curveSelectorHeader.widgetSelected, event=" + evt);
					if (!SelectorComposite.this.curveSelectorHeader.getSelection()) {
						resetContextMenuMeasuring();
						windowActor.clearMeasuring();
						// use this check button to deselect all selected curves
						for (TableItem tableItem : SelectorComposite.this.curveSelectorTable.getItems()) {
							if (tableItem.getChecked()) {
								toggleRecordSelection(tableItem, false, false);
							}
						}
					} else {
						for (TableItem tableItem : SelectorComposite.this.curveSelectorTable.getItems()) {
							if (!tableItem.getChecked()) {
								toggleRecordSelection(tableItem, false, true);
							}
						}
					}
					windowActor.updateChartWindow(true);
				}
			});
		}
		{
			this.chartSelector = new Button(this, SWT.PUSH | SWT.LEFT | SWT.TRANSPARENT);
			FormData chartSelectorLData = new FormData();
			chartSelectorLData.width = GDE.IS_WINDOWS ? 26 : 33;
			chartSelectorLData.height = GDE.IS_WINDOWS ? 26 : 33;
			chartSelectorLData.left = new FormAttachment(curveSelectorHeader);
			chartSelectorLData.top = new FormAttachment(0, 1000, YGAP_CHARTSELECTOR);
			this.chartSelector.setLayoutData(chartSelectorLData);
			this.chartSelector.setImage(SWTResourceManager.getImage("gde/resource/moveToNext.png"));
			this.chartSelector.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0899, GDE.MOD1));
			this.chartSelector.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.fine(() -> "chartSelector.widgetSelected, event=" + evt);
					windowActor.scrollSummaryComposite();
					// windowActor.updateChartWindow(false);
				}
			});
		}
		{
			this.smartSelector = new Button(this, SWT.TOGGLE | SWT.LEFT | SWT.TRANSPARENT);
			FormData smartSelectorLData = new FormData();
			smartSelectorLData.width = GDE.IS_WINDOWS ? 26 : 33;
			smartSelectorLData.height = GDE.IS_WINDOWS ? 26 : 33;
			smartSelectorLData.left = new FormAttachment(chartSelector, 26 / 2);
			smartSelectorLData.top = new FormAttachment(0, 1000, YGAP_CHARTSELECTOR);
			this.smartSelector.setLayoutData(smartSelectorLData);
			this.smartSelector.setImage(SWTResourceManager.getImage("gde/resource/smartSetting.png"));
			this.smartSelector.setSelection(true);
			this.smartSelector.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.fine(() -> "smartSelector.widgetSelected, event=" + evt);
					resetContextMenuMeasuring();
					windowActor.clearMeasuring();
					windowActor.getTrailRecordSet().setSmartStatistics(smartSelector.getSelection());
					windowActor.setTemplateChart();
					windowActor.updateHistoTabs(false, true, true);
				}
			});
		}
		{
			this.saveTemplate = new Button(this, SWT.PUSH | SWT.LEFT | SWT.TRANSPARENT);
			FormData saveTemplateLData = new FormData();
			saveTemplateLData.width = GDE.IS_WINDOWS ? 26 : 33;
			saveTemplateLData.height = GDE.IS_WINDOWS ? 26 : 33;
			saveTemplateLData.left = new FormAttachment(smartSelector, -1);
			saveTemplateLData.top = new FormAttachment(0, 1000, YGAP_CHARTSELECTOR);
			this.saveTemplate.setLayoutData(saveTemplateLData);
			this.saveTemplate.setImage(SWTResourceManager.getImage("gde/resource/saveTemplate.png"));
			this.saveTemplate.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0884, GDE.MOD1));
			this.saveTemplate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.fine(() -> "saveTemplate.widgetSelected, event=" + evt);
					windowActor.saveTemplate();
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
			curveTableLData.top = new FormAttachment(0, 1000, AbstractChartWindow.HEADER_ROW_HEIGHT);
			curveTableLData.bottom = new FormAttachment(1000, 1000, 0);
			curveTableLData.right = new FormAttachment(1000, 1000, 0);
			this.curveSelectorTable.setLayoutData(curveTableLData);
			this.curveSelectorTable.setMenu(this.popupmenu);
			this.curveSelectorTable.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.fine(() -> "curveSelectorTable.widgetSelected, event=" + evt);
					if (evt != null && evt.item != null) {
						final TableItem eventItem = (TableItem) evt.item;
						// avoid phantom measurements with invisible curves
						log.finer(() -> "checked/Old=" + eventItem.getChecked() + eventItem.getData(DataExplorer.OLD_STATE));
						if ( getTableItemRecord(eventItem) != null) {
							String recordName = getTableItemRecord(eventItem).getName();
							if (!eventItem.getChecked() && (Boolean) eventItem.getData(DataExplorer.OLD_STATE) //
									&& windowActor.isMeasureRecord(recordName)) {
								if (windowActor.isMeasureRecord(recordName)) {
									contextMenu.setMeasurement(recordName, false);
									contextMenu.setDeltaMeasurement(recordName, false);
								}
								windowActor.clearMeasuring();
							}
							SelectorComposite.this.popupmenu.setData(DataExplorer.RECORD_NAME, eventItem.getData(DataExplorer.RECORD_NAME));
							SelectorComposite.this.popupmenu.setData(DataExplorer.NAME_REPLACEMENT, eventItem.getData(DataExplorer.NAME_REPLACEMENT));
							SelectorComposite.this.popupmenu.setData(DataExplorer.CURVE_SELECTION_ITEM, eventItem);
							if (toggleRecordSelection(eventItem, true, false)) {
								windowActor.updateChartWindow(false);
							}
						}
					}
				}
			});
			{
				synchronized (this) {
					this.tableSelectorColumn = new TableColumn(this.curveSelectorTable, SWT.LEAD);
					this.tableSelectorColumn.setWidth(this.selectorColumnWidth);
					this.tableCurveTypeColumn = new TableColumn(this.curveSelectorTable, SWT.LEAD);
					this.tableCurveTypeColumn.setWidth(this.curveTypeColumnWidth);
				}
			}
		}
	}

	/**
	 * Update of the curve selector table.
	 */
	public synchronized void doUpdateCurveSelectorTable() {
		if (windowActor.getTrailRecordSet() == null) return ;

		{
			boolean smartStatistics = windowActor.getTrailRecordSet().isSmartStatistics();
			this.chartSelector.setEnabled(smartStatistics);
			this.smartSelector.setToolTipText(Messages.getString(smartStatistics ? MessageIds.GDE_MSGT0887 : MessageIds.GDE_MSGT0898, GDE.MOD1));
			this.smartSelector.setSelection(smartStatistics);
		}
		this.curveSelectorTable.removeAll();
		for (TableEditor editor : this.editors) {
			if (editor != null) { // non displayable records
				if (editor.getEditor() != null) editor.getEditor().dispose();
				editor.dispose();
			}
		}
		int itemWidth = this.initialSelectorHeaderWidth;
		int checkBoxWidth = 20;
		int textSize = 10;
		int itemWidth2 = this.initialCurveTypeColumnWidth;
		int textSize2 = 10;
		boolean isOneVisible = false;
		TrailRecordSet recordSet = windowActor.getTrailRecordSet();
		if (recordSet != null) {
			Combo[] selectorCombos = new Combo[recordSet.size()];
			this.editors = new TableEditor[recordSet.size()];
			for (int i = 0; i < recordSet.getDisplayRecords().size(); i++) {
				TrailRecord record = recordSet.getDisplayRecords().get(i);
				textSize = record.getName().length() * TEXT_EXTENT_FACTOR;
				if (itemWidth < textSize + checkBoxWidth) itemWidth = textSize + checkBoxWidth;
				textSize2 = (int) (record.getTrailSelector().getApplicableTrailsTexts().stream().mapToInt(w -> w.length()).max().orElse(10) * TEXT_EXTENT_FACTOR * 15 / 20.);
				if (itemWidth2 < textSize2 + checkBoxWidth) itemWidth2 = textSize2 + checkBoxWidth;
				// log.fine(() -> item.getText() + " " + itemWidth);
				{
					TableItem item = new TableItem(this.curveSelectorTable, SWT.NULL);
					item.setForeground(ColorUtils.getColor(record.getRGB()));
					item.setData(DataExplorer.RECORD_NAME, record.getName());
					item.setData(DataExplorer.NAME_REPLACEMENT, record.getNameReplacement());
					item.setText(record.getNameReplacement().intern());

					this.editors[i] = new TableEditor(this.curveSelectorTable);
					selectorCombos[i] = new Combo(this.curveSelectorTable, SWT.READ_ONLY);
					this.editors[i].grabHorizontal = true;
					this.editors[i].setEditor(selectorCombos[i], item, COMBO_COLUMN_ORDINAL);
					selectorCombos[i].setItems(record.getTrailSelector().getApplicableTrailsTexts().toArray(new String[0]));
					selectorCombos[i].setText(record.getTrailSelector().getTrailText());
					selectorCombos[i].setToolTipText(!record.getLabel().isEmpty() ? record.getLabel() : Messages.getString(MessageIds.GDE_MSGT0748));
					selectorCombos[i].addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent event) {
							log.fine(() -> "selectorCombos.SelectionListener, event=" + event);
							Combo combo = (Combo) event.getSource();
							recordSet.refillRecord(record, combo.getSelectionIndex());
							windowActor.updateHistoTabs(false, false, false);
						}
					});
					if (record.isVisible()) {
						isOneVisible = true;
						item.setChecked(true);
						item.setData(DataExplorer.OLD_STATE, true);
					} else {
						item.setChecked(false);
						item.setData(DataExplorer.OLD_STATE, false);
					}
					setHeaderSelection(isOneVisible);
				}
			}
			this.selectorColumnWidth = itemWidth;
			this.curveTypeColumnWidth = itemWidth2;
			log.fine(() -> "curveSelectorTable width = " + this.selectorColumnWidth);
		}
		this.tableCurveTypeColumn.setWidth(this.curveTypeColumnWidth);
		if (this.oldSelectorColumnWidth != this.selectorColumnWidth) {
			this.curveSelectorHeader.setSize(this.selectorColumnWidth - 1, this.curveSelectorHeader.getSize().y);
			int xOverlap = 2;
			this.tableSelectorColumn.setWidth(this.selectorColumnWidth - xOverlap);
			this.oldSelectorColumnWidth = this.selectorColumnWidth;
			FormData layoutData2 = (FormData) this.chartSelector.getLayoutData();
			layoutData2.left = new FormAttachment(0, 1000, itemWidth - xOverlap);
		}
		windowActor.setChartSashFormWeights(this);

		log.fine(() -> "curveSelectorTable width = " + this.selectorColumnWidth);
		this.curveSelectorTable.redraw();
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
	 * @return the total width of all columns
	 */
	public int getCompositeWidth() {
		synchronized (this) {
			return this.selectorColumnWidth + this.curveTypeColumnWidth;
		}
	}

	/**
	 * Scan all visible records and clear the context menu entries related with measuring.
	 */
	public void resetContextMenuMeasuring() {
		for (TableItem tableItem : SelectorComposite.this.curveSelectorTable.getItems()) {
			if (tableItem.getChecked()) {
				TrailRecord record = getTableItemRecord(tableItem);
				if (record != null) {
					// avoid phantom measurements with invisible curves
					String recordName = record.getName();
					if (windowActor.isMeasureRecord(recordName)) {
						contextMenu.resetMeasuring();
					}
				}
			}
		}
	}

	public void setRecordSelection(TrailRecord activeRecord) {
		final TableItem tableItem = Arrays.stream(this.curveSelectorTable.getItems()) //
				.filter(c -> ((String) c.getData(DataExplorer.RECORD_NAME)).equals(activeRecord.getName())).findFirst().orElseThrow(UnsupportedOperationException::new);
		tableItem.setChecked(true);
		setRecordSelection(activeRecord, true, tableItem);
	}

	public void setRecordSelection(TrailRecord activeRecord, int selectIndex) {
		if (windowActor.getTrailRecordSet() == null) return ;

		int displayIndex = -1;
		for (int i = 0; i < this.curveSelectorTable.getItems().length; i++) {
			TableItem tableItem2 = this.curveSelectorTable.getItems()[i];
			if (((String) tableItem2.getData(DataExplorer.RECORD_NAME)).equals(activeRecord.getName())) {
				displayIndex = i;
				break;
			}
		}
		TableItem tableItem = this.curveSelectorTable.getItems()[displayIndex];
		tableItem.setChecked(true);
		setRecordSelection(activeRecord, true, tableItem);

		Combo selectorCombo = (Combo) this.editors[displayIndex].getEditor();
		selectorCombo.select(selectIndex);
		windowActor.getTrailRecordSet().refillRecord(activeRecord, selectorCombo.getSelectionIndex());
	}

	private void setRecordSelection(TrailRecord activeRecord, boolean isVisible, final TableItem tableItem) {
		// activeRecord.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
		log.fine(() -> "isVisible old= " + activeRecord.isVisible());
		if (isVisible) {
			activeRecord.setVisible(true);
			// activeRecord.setDisplayable(true);
			SelectorComposite.this.popupmenu.getItem(0).setSelection(true);
			tableItem.setData(DataExplorer.OLD_STATE, true);
			setHeaderSelection(true);
		} else {
			activeRecord.setVisible(false);
			// activeRecord.setDisplayable(false);
			SelectorComposite.this.popupmenu.getItem(0).setSelection(false);
			tableItem.setData(DataExplorer.OLD_STATE, false);
		}
		activeRecord.getParent().syncScaleOfSyncableRecords();
		activeRecord.getParent().setDisplayable();
		activeRecord.getParent().updateVisibleAndDisplayableRecordsForTable();
	}

	/**
	 * Toggle selection state of a record.
	 * @param item table were selection need toggle state
	 * @param isTableSelection
	 * @param forceVisible
	 * @return true if the state was actually changed
	 */
	private boolean toggleRecordSelection(TableItem item, boolean isTableSelection, boolean forceVisible) {
		boolean isToggled = false;
		TrailRecord activeRecord = getTableItemRecord(item);
		if (!isTableSelection || item.getChecked() != (Boolean) item.getData(DataExplorer.OLD_STATE)) {
			isToggled = true;
			log.fine(() -> "selection state changed= " + activeRecord.getName());
			// get newest timestamp and newest recordSet within this entry (both collections are in descending order)
			if (activeRecord != null) {
				setRecordSelection(activeRecord, isTableSelection && item.getChecked() || forceVisible, item);
				log.fine(() -> "isVisible= " + activeRecord.isVisible());
			}
		}
		return isToggled;
	}

	/**
	 * @param item
	 * @return the record of the record represented by the table item
	 */
	@Nullable
	private TrailRecord getTableItemRecord(TableItem item) {
		if (windowActor.getTrailRecordSet() == null) return null;

		TrailRecordSet trailRecordSet = windowActor.getTrailRecordSet();
		return trailRecordSet != null ? trailRecordSet.get(item.getData(DataExplorer.RECORD_NAME)) : null;
	}

	/**
	 * @return the bounds of the table rows relative to the top left header pixel
	 */
	public Rectangle getRealBounds() {
		if (this.curveSelectorTable.getItemCount() > 0) {
			Rectangle firstRowBounds = this.curveSelectorTable.getItem(0).getBounds();
			Rectangle lastRowBounds = this.curveSelectorTable.getItem(this.curveSelectorTable.getItemCount() - 1).getBounds();
			int headerHeight = this.curveSelectorTable.getLocation().y;

			return new Rectangle(getBounds().x, headerHeight, getBounds().width, lastRowBounds.y + lastRowBounds.height - firstRowBounds.y);
		} else {
			return new Rectangle(1, 1, 1, 1);
		}
	}

	public void setVerticalBarVisible(boolean visible) {
		if (!visible) curveSelectorTable.setTopIndex(0);
		this.curveSelectorTable.getVerticalBar().setVisible(visible);
	}

	/**
	 * update background/foreground color of the tool bar
	 */
	public void updateColorSchema() {
		this.setBackground(this.application.COLOR_BACKGROUND);
		this.curveSelectorHeader.setBackground(this.application.COLOR_BACKGROUND);
		this.curveSelectorHeader.setForeground(this.application.COLOR_FOREGROUND);

	}
}
