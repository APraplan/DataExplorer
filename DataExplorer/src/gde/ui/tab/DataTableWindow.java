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

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.SettlementType;
import gde.device.TransitionGroupType;
import gde.device.resource.DeviceXmlResource;
import gde.histo.transitions.TransitionTableMapper;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;
import gde.ui.menu.TabAreaContextMenu.TabMenuType;
import gde.utils.StringHelper;

/**
 * Table display class, displays the data in table form
 * @author Winfried Brügmann
 */
public class DataTableWindow extends CTabItem {
	final static Logger					log												= Logger.getLogger(DataTableWindow.class.getName());

	final public static String	TABLE_TIME_STAMP_ABSOLUTE	= "table_time_stamp_absolute";

	Table												dataTable;
	TableColumn									timeColumn;
	TableCursor									cursor;
	Vector<Integer>							rowVector									= new Vector<Integer>(2);
	Vector<Integer>							topindexVector						= new Vector<Integer>(2);

	final DataExplorer					application;
	final Channels							channels;
	final Settings							settings;
	final DeviceXmlResource			xmlResource								= DeviceXmlResource.getInstance();
	final CTabFolder						tabFolder;
	final Menu									popupmenu;
	final TabAreaContextMenu		contextMenu;
	boolean											isAbsoluteDateTime				= false;

	public DataTableWindow(CTabFolder dataTab, int style, int position) {
		super(dataTab, style, position);
		SWTResourceManager.registerResourceUser(this);
		this.tabFolder = dataTab;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
		this.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 3 : 1), SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.GDE_MSGT0233));

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();
	}

	public void create() {
		this.isAbsoluteDateTime = Settings.getInstance().isTimeFormatAbsolute();
		this.dataTable = new Table(this.tabFolder, SWT.VIRTUAL | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		this.setControl(this.dataTable);
		this.dataTable.setLinesVisible(true);
		this.dataTable.setHeaderVisible(true);

		final TableEditor editor = new TableEditor(this.dataTable);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		this.cursor = new TableCursor(this.dataTable, SWT.NONE);
		this.cursor.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(final KeyEvent event) {
				if (DataTableWindow.log.isLoggable(java.util.logging.Level.FINEST)) DataTableWindow.log.log(java.util.logging.Level.FINEST, ("cursor.keyReleased, keycode: " + event.keyCode));

				if (DataTableWindow.this.settings.isDataTableEditable()) {
					// Clean up any previous editor control
					Control oldEditor = editor.getEditor();
					if (oldEditor != null) oldEditor.dispose();

					// Identify the selected row
					TableItem item = DataTableWindow.this.cursor.getRow();
					if (item == null) return;

					final String origText = item.getText(DataTableWindow.this.cursor.getColumn());
					// The control that will be the editor must be a child of the Table
					final Text text = new Text(DataTableWindow.this.dataTable, SWT.NONE);
					text.setText(String.valueOf(event.character));
					final TableItem row = DataTableWindow.this.cursor.getRow();
					final int column = DataTableWindow.this.cursor.getColumn();
					text.addModifyListener(new ModifyListener() {
						@Override
						public void modifyText(ModifyEvent me) {
							Text modifyText = (Text) editor.getEditor();
							editor.getItem().setText(DataTableWindow.this.cursor.getColumn(), modifyText.getText());
						}
					});
					text.addVerifyListener(new VerifyListener() {

						@Override
						public void verifyText(VerifyEvent ve) {
							ve.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, text.getText());
						}
					});
					text.addFocusListener(new FocusListener() {
						@Override
						public void focusLost(FocusEvent fe) {
							// System.out.println("focus lost");
							DataTableWindow.this.settings.setDataTableEditable(false);
							if (!setEditedRecordPoint(row, column)) DataTableWindow.this.cursor.getRow().setText(column, origText);
							text.dispose();
						}

						@Override
						public void focusGained(FocusEvent arg0) {
							// System.out.println("focus gained");
						}
					});
					text.addKeyListener(new KeyListener() {

						@Override
						public void keyReleased(KeyEvent ke) {
							// System.out.println("key released");
						}

						@Override
						public void keyPressed(KeyEvent ke) {
							// System.out.println("key pressed");
							if (ke.character == SWT.CR) {
								row.setText(column, ((Text) editor.getEditor()).getText());
								DataTableWindow.this.settings.setDataTableEditable(false);
								if (!setEditedRecordPoint(row, column)) DataTableWindow.this.cursor.getRow().setText(column, origText);
								text.dispose();
							}
							// close the text editor when the user hits "ESC"
							else if (ke.character == SWT.ESC) {
								DataTableWindow.this.cursor.getRow().setText(column, origText);
								text.dispose();
								DataTableWindow.this.settings.setDataTableEditable(false);
							}
						}
					});
					text.selectAll();
					text.setFocus();
					editor.setEditor(text, item, DataTableWindow.this.cursor.getColumn());
				}

				if (event.stateMask == SWT.MOD1) {
					switch (event.keyCode) {
					case SWT.HOME:
						// Ctrl+Home: go to first row and first column
						DataTableWindow.this.cursor.setSelection(0, 0);
						break;
					case SWT.END:
						// Ctrl+End: go to last row and last column
						DataTableWindow.this.cursor.setSelection(DataTableWindow.this.dataTable.getItemCount() - 1, DataTableWindow.this.dataTable.getColumnCount() - 1);
						break;
					case 0x63:
						// Ctrl+c: copy selection into clip board
						DataTableWindow.this.dataTable.getSelectionCount();
						DataTableWindow.this.dataTable.getSelectionIndices();
						StringBuilder sb = new StringBuilder();
						int columns = DataTableWindow.this.dataTable.getColumnCount();
						for (int i = 0; i < columns; i++) {
							sb.append(DataTableWindow.this.dataTable.getColumn(i).getText().trim()).append('\t');
						}
						sb.deleteCharAt(sb.length() - 1).append('\n');
						for (TableItem tmpItem : DataTableWindow.this.dataTable.getSelection()) {
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
						DataTableWindow.this.cursor.setSelection(0, DataTableWindow.this.cursor.getColumn());
						break;
					case SWT.END:
						// Ctrl+Shift+End: go to last row
						DataTableWindow.this.cursor.setSelection(DataTableWindow.this.dataTable.getItemCount() - 1, DataTableWindow.this.cursor.getColumn());
						break;
					}
				}
				else if (event.stateMask == 0) {
					switch (event.keyCode) {
					case SWT.HOME:
						// Home: go to first column
						workaroundTableCursor(0);
						break;
					case SWT.END:
						// End: go to last column
						workaroundTableCursor(DataTableWindow.this.dataTable.getColumnCount() - 1);
						break;
					}
				}

				// setSelection() doesn't fire a widgetSelected() event, so we need manually update the vector
				// System.out.println("cursor.keyReleased " + (event.stateMask & SWT.MOD1) + " - " + event.keyCode );
				if (DataTableWindow.this.cursor.getRow() != null && (((event.stateMask & SWT.MOD1) == 0 && event.character != 'c') || ((event.stateMask & SWT.MOD1) == 0 && event.character != 'a'))) {
					updateVector(DataTableWindow.this.dataTable.indexOf(DataTableWindow.this.cursor.getRow()), DataTableWindow.this.dataTable.getTopIndex());

					// select the table row, after repositioning cursor (HOME/END)
					DataTableWindow.this.dataTable.setSelection(new TableItem[] { DataTableWindow.this.cursor.getRow() });
				}
			}

			/**
			 * Problem: When our code for the TableCursor keyReleased() is reached,
			 * the TableCursor has already internally processed the home/end keyevent
			 * and changed the row/column, see:
			 * https://www.javadocexamples.com/org/eclipse/swt/custom/org.eclipse.swt.custom.TableCursor-source.html
			 *
			 * Therefore, we need a Vector that stores the last cell positions, so we can
			 * access the position before the current position.
			 *
			 * @param col
			 */
			private void workaroundTableCursor(int col) {
				/* Get the second element from the vector.
				 * This represents the row that was active before the keyevent
				 * fired and the TableCursor automatically changed the row.
				*/
				int row = DataTableWindow.this.rowVector.get(0);
				if (DataTableWindow.log.isLoggable(java.util.logging.Level.FINER)) DataTableWindow.log.log(java.util.logging.Level.FINER, ("Setting selection to row: " + row));
				DataTableWindow.this.cursor.setSelection(row, col);

				/* As the TableCursor automatically changes the rows and we go back, the item that was on top of the list changes.
				 * We fix that here to get the original item at the top of the list again.
				 */
				if (DataTableWindow.log.isLoggable(java.util.logging.Level.FINER)) DataTableWindow.log.log(java.util.logging.Level.FINER, ("Setting top index: " + DataTableWindow.this.topindexVector.get(0)));
				DataTableWindow.this.dataTable.setTopIndex(DataTableWindow.this.topindexVector.get(0));

				/* Workaround: When Home or End is pressed, the cursor is misplaced in such way,
				 * that the active cell seems to be another one. However, when you press cursor down,
				 * the cell cursor magically appears one cell below the former expected cell.
				 * Calling setVisible fixes this problem.
				 */
				DataTableWindow.this.cursor.setVisible(true);
			}

			@Override
			public void keyPressed(KeyEvent event) {
				if (DataTableWindow.log.isLoggable(java.util.logging.Level.FINEST)) DataTableWindow.log.log(java.util.logging.Level.FINEST, "cursor.keyPressed " + event); //$NON-NLS-1$
				// System.out.println("cursor.keyPressed " + (event.stateMask & SWT.MOD1) + " - " + event.keyCode );
				if (DataTableWindow.this.cursor.getRow() != null && !(event.stateMask == SWT.MOD1 && event.keyCode != 0x99) && !(event.stateMask == SWT.MOD1 && event.keyCode != 0x97)
						&& event.keyCode != SWT.MOD1) {
					// select the table row where the cursor get moved to
					DataTableWindow.this.dataTable.setSelection(new TableItem[] { DataTableWindow.this.cursor.getRow() });
				}
				else if (DataTableWindow.this.cursor.getRow() != null && (event.stateMask & SWT.MOD1) == SWT.MOD1 && event.keyCode == 0x61) {
					// System.out.println("select all");
					DataTableWindow.this.dataTable.setSelection(DataTableWindow.this.dataTable.getItems());
				}
				// to enable multi selection the cursor needs to be set invisible, if the cursor is invisible the dataTable key listener takes effect
				// the cursor should be set to invisible only when shift key is pressed, not while shift is pressed in combination with others
				if (event.keyCode == SWT.MOD2 && ((event.stateMask & SWT.MOD2) == 0) && ((event.stateMask & SWT.MOD1) == 0) && ((event.stateMask & SWT.MOD3) == 0)) {
					DataTableWindow.this.cursor.setVisible(false);
					if (DataTableWindow.this.cursor.getRow() != null) {
						// reset previous (multiple) selection table row(s)
						DataTableWindow.this.dataTable.setSelection(new TableItem[] { DataTableWindow.this.cursor.getRow() });
					}
				}
			}
		});

		// Add the event handling
		this.cursor.addSelectionListener(new SelectionAdapter() {
			// This is called as the user navigates around the table
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (DataTableWindow.log.isLoggable(java.util.logging.Level.FINEST)) DataTableWindow.log.log(java.util.logging.Level.FINEST, "cursor.widgetSelected " + event); //$NON-NLS-1$
				if (DataTableWindow.this.cursor.getRow() != null) {
					updateVector(DataTableWindow.this.dataTable.indexOf(DataTableWindow.this.cursor.getRow()), DataTableWindow.this.dataTable.getTopIndex());
				}
			}
		});

		this.dataTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.dataTable.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent evt) {
				if (DataTableWindow.log.isLoggable(java.util.logging.Level.FINER)) DataTableWindow.log.log(java.util.logging.Level.FINER, "dataTable.helpRequested " + evt); //$NON-NLS-1$
				DataExplorer.getInstance().openHelpDialog("", "HelpInfo_6.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		this.dataTable.addListener(SWT.SetData, new Listener() {
			@Override
			public void handleEvent(Event event) {
				Channel activeChannel = DataTableWindow.this.channels.getActiveChannel();
				if (activeChannel != null) {
					RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
					if (activeRecordSet != null && activeRecordSet.getRecordDataSize(false) > 0) {
						TableItem item = (TableItem) event.item;
						int index = DataTableWindow.this.dataTable.indexOf(item);
						item.setText(activeRecordSet.getDataTableRow(index, DataTableWindow.this.isAbsoluteDateTime));
					}
				}
			}
		});
		this.dataTable.addKeyListener(new KeyListener() {
			int selectionFlowIndex = 0;

			private void workaroundTableCursor(int col) {
				/* Get the second element from the vector.
				 * This represents the row that was active before the keyevent
				 * fired and the TableCursor automatically changed the row.
				*/
				int row = DataTableWindow.this.rowVector.get(DataTableWindow.this.rowVector.size() - 1);
				if (DataTableWindow.log.isLoggable(java.util.logging.Level.FINER)) DataTableWindow.log.log(java.util.logging.Level.FINER, ("Setting selection to row: " + row));
				DataTableWindow.this.cursor.setSelection(row, col);

				/* As the TableCursor automatically changes the rows and we go back, the item that was on top of the list changes.
				 * We fix that here to get the original item at the top of the list again.
				 */
				if (DataTableWindow.log.isLoggable(java.util.logging.Level.FINER)) DataTableWindow.log.log(java.util.logging.Level.FINER, ("Setting top index: " + DataTableWindow.this.topindexVector.get(0)));
				DataTableWindow.this.dataTable.setTopIndex(DataTableWindow.this.topindexVector.get(DataTableWindow.this.topindexVector.size() - 1));

				DataTableWindow.this.cursor.setVisible(true);

				// calling setFocus(releases table key listener and take over to cursor key listener again
				DataTableWindow.this.cursor.setFocus();
			}

			@Override
			public void keyReleased(KeyEvent event) {
				if (DataTableWindow.log.isLoggable(java.util.logging.Level.FINEST)) DataTableWindow.log.log(java.util.logging.Level.FINEST, ("dataTable.keyReleased, keycode: " + event.keyCode));
				if (event.keyCode == SWT.MOD2 && DataTableWindow.this.rowVector.size() > 0) {
					int rowIndex = DataTableWindow.this.rowVector.get(DataTableWindow.this.rowVector.size() - 1) + this.selectionFlowIndex;
					// check table bounds reached
					rowIndex = rowIndex < 0 ? 0 : rowIndex > DataTableWindow.this.dataTable.getItems().length - 1 ? DataTableWindow.this.dataTable.getItems().length - 1 : rowIndex;

					updateVector(rowIndex, DataTableWindow.this.dataTable.getTopIndex());
					workaroundTableCursor(DataTableWindow.this.cursor.getColumn());
					this.selectionFlowIndex = 0;
					switch (event.keyCode) {
					case 1:
						break;
					}
				}
			}

			@Override
			public void keyPressed(KeyEvent event) {
				if (DataTableWindow.log.isLoggable(java.util.logging.Level.FINEST)) DataTableWindow.log.log(java.util.logging.Level.FINEST, ("dataTable.keyPressed, keycode: " + event.keyCode));
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
		this.contextMenu.createMenu(this.popupmenu, TabMenuType.TABLE);
		this.cursor.setMenu(this.popupmenu);
	}

	/**
	 * Helper function for workaroundTableCursor()
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

	/**
	 * method to set up new header according device properties
	 */
	public void setHeader() {
		// clean old header
		this.dataTable.removeAll();
		TableColumn[] columns = this.dataTable.getColumns();
		for (TableColumn tableColumn : columns) {
			tableColumn.dispose();
		}

		int extentFactor = 9;
		String time = this.isAbsoluteDateTime ? Messages.getString(MessageIds.GDE_MSGT0436) : Messages.getString(MessageIds.GDE_MSGT0234);
		this.timeColumn = new TableColumn(this.dataTable, SWT.CENTER);
		this.timeColumn.setWidth(time.length() * 7);
		this.timeColumn.setText(time);

		// set the new header line
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				synchronized (activeRecordSet) {

					if (this.settings.isPartialDataTable()) {
						for (final Record record : activeRecordSet.getVisibleAndDisplayableRecordsForTable()) {
							StringBuilder sb = new StringBuilder();
							sb.append(record.getName()).append(GDE.STRING_BLANK_LEFT_BRACKET).append(record.getUnit()).append(GDE.STRING_RIGHT_BRACKET);
							TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
							column.setWidth(sb.length() * extentFactor);
							column.setText(sb.toString());
						}
					}
					else {
						for (int i = 0; i < activeRecordSet.size(); i++) {
							Record record = activeRecordSet.get(i);
							StringBuilder sb = new StringBuilder();
							sb.append(record.getName()).append(GDE.STRING_BLANK_LEFT_BRACKET).append(record.getUnit()).append(GDE.STRING_RIGHT_BRACKET);
							TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
							column.setWidth(sb.length() * extentFactor);
							column.setText(sb.toString());
						}
					}
					if (this.settings.isHistoActive() && this.settings.isDataTableTransitions()) {
						TransitionTableMapper mapper = new TransitionTableMapper(DataTableWindow.this.application.getActiveRecordSet(), Analyzer.getInstance());
						for (SettlementType settlementType : mapper.defineActiveAndDisplayableSettlements().values()) {
							TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
							String recordName = this.xmlResource.getReplacement(settlementType.getName());
							StringBuilder sb = new StringBuilder();
							sb.append(recordName).append(GDE.STRING_BLANK_LEFT_BRACKET).append(settlementType.getUnit()).append(GDE.STRING_RIGHT_BRACKET);
							column.setWidth(sb.length() > 11 ? 11 * extentFactor : sb.length() * extentFactor);
							column.setText(sb.toString());
						}
						IDevice device = this.application.getActiveDevice();
						HashMap<Integer, TransitionGroupType> transitionGroups = device.getDeviceConfiguration().getChannel(this.channels.getActiveChannelNumber()).getTransitionGroups();
						for (Entry<Integer, TransitionGroupType> transitionGroupEntry : transitionGroups.entrySet()) {
							TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
							column.setWidth(6 * extentFactor);
							if (transitionGroupEntry.getValue().getComment() != null)
								column.setText(String.format("TG%d: %-22s", transitionGroupEntry.getKey(), transitionGroupEntry.getValue().getComment()));
							else
								column.setText(String.format("TG%d", transitionGroupEntry.getKey()));
						}
					}
				}
			}
			else {
				IDevice device = this.application.getActiveDevice();
				for (int i = 0; i < device.getNumberOfMeasurements(activeChannel.getNumber()); i++) {
					MeasurementType measurement = device.getMeasurement(activeChannel.getNumber(), i);
					StringBuilder sb = new StringBuilder();
					sb.append(xmlResource.getReplacement(measurement.getName().trim())).append(GDE.STRING_BLANK).append(GDE.STRING_LEFT_BRACKET).append(measurement.getUnit()).append(GDE.STRING_RIGHT_BRACKET);
					TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
					column.setWidth(sb.length() * extentFactor);
					column.setText(sb.toString());
				}
				if (System.getProperty("os.name", "").toLowerCase().startsWith("linux")) { // add additional header field for padding //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
					column.setWidth(100);
				}
			}
		}
	}

	/**
	 * @param count of record entries
	 */
	public void setRowCount(int count) {
		this.dataTable.setItemCount(count);
	}

	/**
	 * clean the table, not the header
	 */
	public synchronized void cleanTable() {
		if (this.dataTable != null && !this.dataTable.isDisposed()) this.dataTable.removeAll();
	}

	/**
	 * create visible tab window content as image
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

	/**
	 * reset the top index of the data table if required while updating or adding rows
	 */
	public void updateTopIndex() {
		int height = this.dataTable.getClientArea().height;
		int visibleItemCount = height / this.dataTable.getItemHeight() - 1;
		int topIndex = this.dataTable.getTopIndex();
		int itemCount = this.dataTable.getItemCount();
		if (itemCount > visibleItemCount) {
			int newTopIndex = itemCount - visibleItemCount;
			if (topIndex != newTopIndex) this.dataTable.setTopIndex(newTopIndex);
		}
	}

	/**
	 * @param isAbsoluteDateTime the isAbsoluteDateTime to set
	 */
	public void setAbsoluteDateTime(boolean isAbsoluteDateTime) {
		this.isAbsoluteDateTime = isAbsoluteDateTime;
	}

	/**
	 * set an edited entry to the record at the given index
	 * @param row
	 * @param column
	 */
	private synchronized boolean setEditedRecordPoint(final TableItem row, final int column) {
		String recordName = DataTableWindow.this.dataTable.getColumn(column).getText();
		recordName = recordName.substring(0, recordName.lastIndexOf(GDE.CHAR_BLANK));
		Record editRecord = DataTableWindow.this.application.getActiveRecordSet().get(recordName);
		if (editRecord != null && StringHelper.verifyTypedInput(DataTypes.DOUBLE, row.getText(column))) {
			editRecord.set(DataTableWindow.this.dataTable.indexOf(DataTableWindow.this.cursor.getRow()), (int) (Double.valueOf(row.getText(column).replace(',', '.')) * 1000));
			return true;
		}
		return false;
	}

}
