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

    Copyright (c) 2017,2018,2019 Thomas Eickert
****************************************************************************************/
package gde.histo.ui;

import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;

import java.util.Arrays;
import java.util.Vector;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.histo.datasources.HistoSet;
import gde.histo.recordings.HistoTableMapper;
import gde.histo.recordings.HistoTableMapper.DisplayTag;
import gde.histo.recordings.TrailDataTags.DataTag;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.ui.menu.AbstractTabAreaContextMenu;
import gde.histo.ui.menu.AbstractTabAreaContextMenu.TabMenuOnDemand;
import gde.histo.ui.menu.TableTabAreaContextMenu;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * Display the history in table form.
 * @author Thomas Eickert
 */
public class HistoTableWindow extends CTabItem {
	private static final String								$CLASS_NAME						= HistoTableWindow.class.getName();
	private static final Logger								log										= Logger.getLogger($CLASS_NAME);

	private static final int									TEXT_EXTENT_FACTOR		= 6;

	private final HistoExplorer								presentHistoExplorer	= DataExplorer.getInstance().getPresentHistoExplorer();
	private final Channels										channels							= Channels.getInstance();
	private final Settings										settings							= Settings.getInstance();

	private final CTabFolder									tabFolder;
	private final Menu												popupmenu;
	private final AbstractTabAreaContextMenu	contextMenu;

	private Table															dataTable;
	private TableCursor												cursor;
	private Vector<Integer>										rowVector							= new Vector<Integer>(2);
	private Vector<Integer>										topindexVector				= new Vector<Integer>(2);

	private HistoTableWindow(CTabFolder dataTab, int style, int position) {
		super(dataTab, style, position);
		SWTResourceManager.registerResourceUser(this);
		this.tabFolder = dataTab;

		this.setFont(SWTResourceManager.getFont(DataExplorer.getInstance(), GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 3 : 1), SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.GDE_MSGT0793));

		this.popupmenu = new Menu(DataExplorer.getInstance().getShell(), SWT.POP_UP);
		this.contextMenu = new TableTabAreaContextMenu();
	}

	public static HistoTableWindow create(CTabFolder dataTab, int style, int position) {
		HistoTableWindow window = new HistoTableWindow(dataTab, style, position);

		window.dataTable = new Table(window.tabFolder, SWT.VIRTUAL | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		window.setControl(window.dataTable);
		window.dataTable.setLinesVisible(true);
		window.dataTable.setHeaderVisible(true);

		window.initGui();
		return window;
	}

	private void initGui() {
		final TableEditor editor = new TableEditor(this.dataTable);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		this.cursor = new TableCursor(this.dataTable, SWT.NONE);
		this.cursor.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent event) {
				log.finest(() -> "cursor.keyReleased, keycode: " + event.keyCode); //$NON-NLS-1$

				if (event.stateMask == SWT.MOD1) {
					switch (event.keyCode) {
					case SWT.HOME:
						// Ctrl+Home: go to first row and first column
						HistoTableWindow.this.cursor.setSelection(0, 0);
						break;
					case SWT.END:
						// Ctrl+End: go to last row and last column
						HistoTableWindow.this.cursor.setSelection(HistoTableWindow.this.dataTable.getItemCount() - 1, HistoTableWindow.this.dataTable.getColumnCount() - 1);
						break;
					case 0x63:
						// Ctrl+c: copy selection into clip board
						HistoTableWindow.this.dataTable.getSelectionCount();
						HistoTableWindow.this.dataTable.getSelectionIndices();
						StringBuilder sb = new StringBuilder();
						int columns = HistoTableWindow.this.dataTable.getColumnCount();
						for (int i = 0; i < columns; i++) {
							sb.append(HistoTableWindow.this.dataTable.getColumn(i).getText().trim()).append('\t');
						}
						sb.deleteCharAt(sb.length() - 1).append('\n');
						for (TableItem tmpItem : HistoTableWindow.this.dataTable.getSelection()) {
							for (int i = 0; i < columns; i++) {
								sb.append(tmpItem.getText(i).trim()).append('\t');
							}
							sb.deleteCharAt(sb.length() - 1).append('\n');
						}
						Clipboard cb = new Clipboard(GDE.display);
						String textData = sb.toString();
						Transfer textTransfer = TextTransfer.getInstance();
						cb.setContents(new Object[] { textData }, new Transfer[] { textTransfer });
						break;
					}
				}
				// else if (event.stateMask == SWT.MOD2) {
				// selection of multiple rows (Shift+Arrow Up/Down) require DataTableWindow.this.cursor.setVisible(false);
				// afterwards dataTable.keyReleased handles selection movement
				// }
				else if (((event.stateMask & SWT.MOD2) != 0) && ((event.stateMask & SWT.MOD1) != 0)) {
					switch (event.keyCode) {
					case SWT.HOME:
						// Ctrl+Shift+Home: go to first row
						HistoTableWindow.this.cursor.setSelection(0, HistoTableWindow.this.cursor.getColumn());
						break;
					case SWT.END:
						// Ctrl+Shift+End: go to last row
						HistoTableWindow.this.cursor.setSelection(HistoTableWindow.this.dataTable.getItemCount() - 1, HistoTableWindow.this.cursor.getColumn());
						break;
					}
				} else if (event.stateMask == 0) {
					switch (event.keyCode) {
					case SWT.HOME:
						// Home: go to first column
						workaroundTableCursor(0);
						break;
					case SWT.END:
						// End: go to last column
						workaroundTableCursor(HistoTableWindow.this.dataTable.getColumnCount() - 1);
						break;
					}
				}

				// setSelection() doesn't fire a widgetSelected() event, so we need manually update the vector
				// System.out.println("cursor.keyReleased " + (event.stateMask & SWT.MOD1) + " - " + event.keyCode );
				if (HistoTableWindow.this.cursor.getRow() != null && (((event.stateMask & SWT.MOD1) == 0 && event.character != 'c') || ((event.stateMask & SWT.MOD1) == 0 && event.character != 'a'))) {
					updateVector(HistoTableWindow.this.dataTable.indexOf(HistoTableWindow.this.cursor.getRow()), HistoTableWindow.this.dataTable.getTopIndex());

					// select the table row, after repositioning cursor (HOME/END)
					HistoTableWindow.this.dataTable.setSelection(new TableItem[] { HistoTableWindow.this.cursor.getRow() });
				}
			}

			/**
			 * Problem: When our code for the TableCursor keyReleased() is reached,
			 * the TableCursor has already internally processed the home/end keyevent and changed the row/column.
			 * Therefore, we need a Vector that stores the last cell positions, so we can access the position before the current position.
			 * @param col
			 * @see <a href="https://www.javadocexamples.com/org/eclipse/swt/custom/org.eclipse.swt.custom.TableCursor-source.html">TableCursor
			 *      source</a>
			 */
			private void workaroundTableCursor(int col) {
				/*
				 * Get the second element from the vector.
				 * This represents the row that was active before the keyevent
				 * fired and the TableCursor automatically changed the row.
				 */
				int row = HistoTableWindow.this.rowVector.get(0);
				log.finer(() -> "Setting selection to row: " + row); //$NON-NLS-1$
				HistoTableWindow.this.cursor.setSelection(row, col);

				/*
				 * As the TableCursor automatically changes the rows and we go back, the item that was on top of the list changes.
				 * We fix that here to get the original item at the top of the list again.
				 */
				log.finer(() -> "Setting top index: " + HistoTableWindow.this.topindexVector.get(0)); //$NON-NLS-1$
				HistoTableWindow.this.dataTable.setTopIndex(HistoTableWindow.this.topindexVector.get(0));

				/*
				 * Workaround: When Home or End is pressed, the cursor is misplaced in such way,
				 * that the active cell seems to be another one. However, when you press cursor down,
				 * the cell cursor magically appears one cell below the former expected cell.
				 * Calling setVisible fixes this problem.
				 */
				HistoTableWindow.this.cursor.setVisible(true);
			}

			@Override
			public void keyPressed(KeyEvent event) {
				log.log(FINEST, "cursor.keyPressed ", event); //$NON-NLS-1$
				// System.out.println("cursor.keyPressed " + (event.stateMask & SWT.MOD1) + " - " + event.keyCode );
				if (HistoTableWindow.this.cursor.getRow() != null && !(event.stateMask == SWT.MOD1 && event.keyCode != 0x99) && !(event.stateMask == SWT.MOD1 && event.keyCode != 0x97) && event.keyCode != SWT.MOD1) {
					// select the table row where the cursor get moved to
					HistoTableWindow.this.dataTable.setSelection(new TableItem[] { HistoTableWindow.this.cursor.getRow() });
				} else if (HistoTableWindow.this.cursor.getRow() != null && (event.stateMask & SWT.MOD1) == SWT.MOD1 && event.keyCode == 0x61) {
					// System.out.println("select all");
					HistoTableWindow.this.dataTable.setSelection(HistoTableWindow.this.dataTable.getItems());
				}
				// to enable multi selection the cursor needs to be set invisible, if the cursor is invisible the dataTable key listener takes effect
				// the cursor should be set to invisible only when shift key is pressed, not while shift is pressed in combination with others
				if (event.keyCode == SWT.MOD2 && ((event.stateMask & SWT.MOD2) == 0) && ((event.stateMask & SWT.MOD1) == 0) && ((event.stateMask & SWT.MOD3) == 0)) {
					HistoTableWindow.this.cursor.setVisible(false);
					if (HistoTableWindow.this.cursor.getRow() != null) {
						// reset previous (multiple) selection table row(s)
						HistoTableWindow.this.dataTable.setSelection(new TableItem[] { HistoTableWindow.this.cursor.getRow() });
					}
				}
			}
		});

		// Add the event handling
		this.cursor.addSelectionListener(new SelectionAdapter() {
			// This is called as the user navigates around the table
			@Override
			public void widgetSelected(SelectionEvent event) {
				log.log(FINEST, "cursor.widgetSelected ", event); //$NON-NLS-1$
				if (HistoTableWindow.this.cursor.getRow() != null) {
					updateVector(HistoTableWindow.this.dataTable.indexOf(HistoTableWindow.this.cursor.getRow()), HistoTableWindow.this.dataTable.getTopIndex());
				}

				TrailRecordSet trailRecordSet = Channels.getInstance().getActiveChannel() != null //
						? trailRecordSet = presentHistoExplorer.getTrailRecordSet() //
						: null;
				int rowNumber = HistoTableWindow.this.dataTable.indexOf(HistoTableWindow.this.cursor.getRow()); // 0-based
				int columnNumber = HistoTableWindow.this.cursor.getColumn(); // 0-based
				log.finer(() -> "row=" + rowNumber + "  column=" + columnNumber); //$NON-NLS-1$ //$NON-NLS-2$
				if (HistoTableWindow.this.dataTable.getColumnCount() <= 2 || columnNumber < 2 || trailRecordSet == null || trailRecordSet.isEmpty()) {
					HistoTableWindow.this.popupmenu.setData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.name(), null);
					HistoTableWindow.this.popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), GDE.STRING_EMPTY);
					HistoTableWindow.this.popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), GDE.STRING_EMPTY);
					HistoTableWindow.this.popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), GDE.STRING_EMPTY);
				} else {
					int index = HistoTableWindow.this.settings.isXAxisReversed() ? columnNumber - 2 : HistoTableWindow.this.dataTable.getColumnCount() - columnNumber - 1;
					HistoTableWindow.this.popupmenu.setData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.name(), GDE.STRING_TRUE);
					HistoTableWindow.this.popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), trailRecordSet.getDataTagText(index, DataTag.LINK_PATH));
					HistoTableWindow.this.popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), trailRecordSet.getDataTagText(index, DataTag.FILE_PATH));
					HistoTableWindow.this.popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), trailRecordSet.getDataTagText(index, DataTag.RECORDSET_BASE_NAME));
				}
				HistoTableWindow.this.popupmenu.setData(TabMenuOnDemand.EXCLUDED_LIST.name(), Arrays.stream(ExclusionActivity.getExcludedTrusses()).collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR)));
				log.finer(() -> "DataTag.FILE_PATH=" + HistoTableWindow.this.popupmenu.getData(TabMenuOnDemand.DATA_FILE_PATH.name())); //$NON-NLS-1$
			}
		});

		this.dataTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.dataTable.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent evt) {
				log.log(FINER, "dataTable.helpRequested ", evt); //$NON-NLS-1$
				DataExplorer.getInstance().openHelpDialog("", "HelpInfo_95.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		this.dataTable.addListener(SWT.SetData, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (presentHistoExplorer.hasRecords()) {
					TrailRecordSet trailRecordSet = presentHistoExplorer.getTrailRecordSet();
					TableItem item = (TableItem) event.item;
					Vector<TrailRecord> currentRecords = trailRecordSet.getVisibleAndDisplayableRecordsForTable();
					try {
						if (HistoTableWindow.this.dataTable.indexOf(item) < currentRecords.size()) {
							int index = HistoTableWindow.this.dataTable.indexOf(item);
							TrailRecord trailRecord = currentRecords.get(index);
							item.setText(HistoTableMapper.getTableRow(trailRecord));
						} else if (HistoTableWindow.this.settings.isDisplayTags()) {
							int index = HistoTableWindow.this.dataTable.indexOf(item) - currentRecords.size();
							int channelNumber = Analyzer.getInstance().getActiveChannel().getNumber();
							DisplayTag[] activeDisplayTags = trailRecordSet.getDataTags().getActiveDisplayTags(channelNumber).toArray(new DisplayTag[] {});
							item.setText(HistoTableMapper.getTableTagRow(trailRecordSet, activeDisplayTags[index]));
						}
					}
					catch (Exception e) {
						log.warning(() -> "index of item outside data table array bounds");
					}
				}
			}
		});

		this.dataTable.addKeyListener(new KeyListener() {
			int selectionFlowIndex = 0;

			private void workaroundTableCursor(int col) {
				/*
				 * Get the second element from the vector.
				 * This represents the row that was active before the keyevent
				 * fired and the TableCursor automatically changed the row.
				 */
				int row = HistoTableWindow.this.rowVector.get(HistoTableWindow.this.rowVector.size() - 1);
				log.log(FINER, "Setting selection to row: ", row); //$NON-NLS-1$
				HistoTableWindow.this.cursor.setSelection(row, col);

				/*
				 * As the TableCursor automatically changes the rows and we go back, the item that was on top of the list changes.
				 * We fix that here to get the original item at the top of the list again.
				 */
				log.finer(() -> "Setting top index: " + HistoTableWindow.this.topindexVector.get(0)); //$NON-NLS-1$
				HistoTableWindow.this.dataTable.setTopIndex(HistoTableWindow.this.topindexVector.get(HistoTableWindow.this.topindexVector.size() - 1));

				HistoTableWindow.this.cursor.setVisible(true);

				// calling setFocus(releases table key listener and take over to cursor key listener again
				HistoTableWindow.this.cursor.setFocus();
			}

			@Override
			public void keyReleased(KeyEvent event) {
				log.finest(() -> "dataTable.keyReleased, keycode: " + event.keyCode); //$NON-NLS-1$
				if (event.keyCode == SWT.MOD2 && HistoTableWindow.this.rowVector.size() > 0) { // ET 20.06.2017 2nd condition added due to rowVector.get index
																																												// violation
					int rowIndex = HistoTableWindow.this.rowVector.get(HistoTableWindow.this.rowVector.size() - 1) + this.selectionFlowIndex;
					// check table bounds reached
					rowIndex = rowIndex < 0 ? 0 : rowIndex > HistoTableWindow.this.dataTable.getItems().length - 1
							? HistoTableWindow.this.dataTable.getItems().length - 1 : rowIndex;

					updateVector(rowIndex, HistoTableWindow.this.dataTable.getTopIndex());
					workaroundTableCursor(HistoTableWindow.this.cursor.getColumn());
					this.selectionFlowIndex = 0;
					switch (event.keyCode) {
					case 1:
						break;
					}
				}
			}

			@Override
			public void keyPressed(KeyEvent event) {
				log.finest(() -> "dataTable.keyPressed, keycode: " + event.keyCode); //$NON-NLS-1$
				if (event.stateMask == SWT.MOD2 && ((event.stateMask & SWT.MOD1) == 0) && ((event.stateMask & SWT.MOD3) == 0)) {
					switch (event.keyCode) {
					case SWT.ARROW_UP:
						--this.selectionFlowIndex;
						break;
					case SWT.ARROW_DOWN:
						++this.selectionFlowIndex;
						break;
					}
				}
			}
		});
		this.contextMenu.createMenu(this.popupmenu);
		this.cursor.setMenu(this.popupmenu);
	}

	/**
	 * @return true if table window is visible
	 */
	public boolean isVisible() {
		return this.dataTable.isVisible();
	}

	/**
	 * Helper function for workaroundTableCursor().
	 * @param row
	 * @param topindex
	 */
	private void updateVector(int row, int topindex) {

		if (this.rowVector.size() == 2) {
			// we only need the last and the current value, so remove the old entries
			this.rowVector.remove(0);
			this.topindexVector.remove(0);
		}

		// now add the new row
		this.rowVector.addElement(row);
		this.topindexVector.addElement(topindex);
	}

	public boolean isHeaderTextValid() {
		if (presentHistoExplorer.hasRecords()) {
			String[] tableHeaderRow = HistoTableMapper.getTableHeaderRow(presentHistoExplorer.getTrailRecordSet());
			if (tableHeaderRow.length == this.dataTable.getColumnCount() - 2) {
				boolean isValid = true;
				for (int i = 0; i < tableHeaderRow.length; i++) {
					isValid = tableHeaderRow[i].equals(this.dataTable.getColumn(i + 2).getText());
					if (!isValid) //
						break;
				}
				return isValid;
			}
		}
		return false;
	}

	public boolean isRowTextAndTrailValid() {
		boolean isValid = false;
		if (presentHistoExplorer.hasRecords()) {
			TrailRecordSet trailRecordSet = presentHistoExplorer.getTrailRecordSet();
			for (int j = 0; j < this.dataTable.getItems().length; j++) {
				TableItem tableItem = this.dataTable.getItems()[j];
				int index = HistoTableWindow.this.dataTable.indexOf(tableItem);
				if (HistoTableWindow.this.dataTable.indexOf(tableItem) < trailRecordSet.getVisibleAndDisplayableRecordsForTable().size()) {
					TrailRecord trailRecord = trailRecordSet.getVisibleAndDisplayableRecordsForTable().get(index);
					isValid = tableItem.getText().equals(trailRecord.getTableRowHeader()) && tableItem.getText(1).equals(trailRecord.getTrailSelector().getTrailText());
				} else {
					isValid = tableItem.getText().isEmpty();
				}
				if (!isValid) //
					break;
			}
		}
		return isValid;
	}

	/**
	 * Set up two header columns and columns for the trail recordsets of the histoSet timestamp range.
	 */
	public void setHeader() {
		// clean old header
		this.dataTable.removeAll(); // prevents display flickering (to some extent only)
		for (TableColumn tableColumn : this.dataTable.getColumns()) {
			tableColumn.dispose();
		}
		this.dataTable.setItemCount(0); // ET required for Listener firing on setRowCount(xyz)

		String recordTitle = Messages.getString(MessageIds.GDE_MSGT0749);
		TableColumn recordsColumn = new TableColumn(this.dataTable, SWT.CENTER);
		recordsColumn.setWidth((int) (recordTitle.length() * TEXT_EXTENT_FACTOR * 15 / 10.));
		recordsColumn.setText(recordTitle);

		String curveTypeHeader = Messages.getString(MessageIds.GDE_MSGT0828);
		recordsColumn = new TableColumn(this.dataTable, SWT.LEFT);
		recordsColumn.setWidth((int) (curveTypeHeader.length() * TEXT_EXTENT_FACTOR * 15 / 10.));
		recordsColumn.setText(curveTypeHeader);

		// set the data columns of the new header line
		Channel activeChannel = this.channels.getActiveChannel();
		HistoSet histoSet = presentHistoExplorer.getHistoSet();
		if (activeChannel != null && histoSet != null && histoSet.getTrailRecordSet() != null) {
			String[] tableHeaderRow = HistoTableMapper.getTableHeaderRow(histoSet.getTrailRecordSet());
			if (tableHeaderRow.length > 0) {
				for (String headerString : tableHeaderRow) {
					TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
					column.setWidth((int) (headerString.length() * TEXT_EXTENT_FACTOR * 21 / 20.));
					column.setText(headerString.intern());
				}
			} else {
				if (System.getProperty("os.name", "").toLowerCase().startsWith("linux")) { //$NON-NLS-1$ //$NON-NLS-2$
					// Linux SWT need additional header field for padding
					TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
					column.setWidth(100);
				}
			}
		}
		log.log(FINER, "dataTable.getColumnCount() ", this.dataTable.getColumnCount()); //$NON-NLS-1$
	}

	/**
	 * Trigger the table listener SWT.SetData based on the visible and displayable records.
	 * @param count of visible (!) record entries
	 */
	public void setRowCount(int count) {
		DataExplorer.getInstance().getPresentHistoExplorer().paintVolatileStatusMessage();
		this.dataTable.setItemCount(count);
	}

	/**
	 * Clean the table, not the header.
	 */
	public synchronized void cleanTable() {
		if (this.dataTable != null && !this.dataTable.isDisposed()) {
			this.dataTable.removeAll();
			// ET added (empty columns remain after removeAll)
			for (TableColumn tableColumn : this.dataTable.getColumns()) {
				tableColumn.dispose();
			}
		}
	}

	/**
	 * Create visible tab window content as image.
	 * @return image with content
	 */
	public Image getContentAsImage() {
		Rectangle bounds = this.dataTable.getClientArea();
		Image tabContentImage = new Image(GDE.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.dataTable.print(imageGC);
		if (GDE.IS_MAC)
			this.dataTable.print(imageGC);
		imageGC.dispose();

		return tabContentImage;
	}

	public int getTableLength() {
		return this.dataTable.getItems().length;
	}

}
