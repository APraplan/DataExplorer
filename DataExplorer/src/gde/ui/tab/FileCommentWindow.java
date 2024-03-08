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
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;
import gde.ui.menu.TabAreaContextMenu.TabMenuType;

/**
 * Class to enable a file comment
 * @author Winfried Brügmann
 */
public class FileCommentWindow extends CTabItem {

	final static Logger				log												= Logger.getLogger(FileCommentWindow.class.getName());

	Composite									commentMainComposite;
	CLabel										infoLabel;
	Text											fileCommentText;
	boolean										isFileCommentChanged			= false;
	boolean										isRecordCommentChanged		= false;
	Table											recordCommentTable;
	TableColumn								recordCommentTableColumn1;
	TableColumn								recordCommentTableColumn2;
	TableCursor								cursor;
	ControlEditor							editor;
	Color											innerAreaBackground;
	Color											surroundingBackground;
	Vector<Integer>						rowVector									= new Vector<Integer>(2);
	Vector<Integer>						topindexVector						= new Vector<Integer>(2);

	int												fileCommentCaretPosition	= 0;
	boolean										isFocusGained							= false;
	boolean										isTableUptaded						= true;
	
	final DataExplorer				application;
	final Channels						channels;
	final CTabFolder					displayTab;
	final Menu								popupmenu;
	final TabAreaContextMenu	contextMenu;
	final int									TEXT_MARGIN								= 2;

	/**
	 * constructor with CTabFolder parent
	 * @param currentDisplayTab
	 */
	public FileCommentWindow(CTabFolder currentDisplayTab, int style) {
		super(currentDisplayTab, style);
		this.displayTab = currentDisplayTab;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		SWTResourceManager.registerResourceUser(this);
		this.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 3 : 1), SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.GDE_MSGT0239));

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();
		this.innerAreaBackground = Settings.getInstance().getFileCommentInnerAreaBackground();
		this.surroundingBackground = Settings.getInstance().getFileCommentSurroundingAreaBackground();
	}

	/**
	 * method to create the window and register required event listener
	 */
	public void create() {
		this.commentMainComposite = new Composite(this.displayTab, SWT.NONE);
		this.setControl(this.commentMainComposite);
		this.commentMainComposite.setLayout(null);
		this.commentMainComposite.setBackground(this.surroundingBackground);
		this.commentMainComposite.setMenu(this.popupmenu);
		this.commentMainComposite.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent evt) {
				if (FileCommentWindow.log.isLoggable(java.util.logging.Level.FINE)) FileCommentWindow.log.log(java.util.logging.Level.FINE, "commentMainComposite.paintControl, event=" + evt); //$NON-NLS-1$
				if (isFileCommentChanged) setFileComment();
				if (isRecordCommentChanged) updateRecordSetTable();
			}
		});
		this.commentMainComposite.addControlListener(new ControlListener() {
			public void controlResized(ControlEvent evt) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "composite.controlResized evt=" + evt); //$NON-NLS-1$
				updateRecordSetTable();
			}

			public void controlMoved(ControlEvent evt) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "composite.controlMoved evt=" + evt); //$NON-NLS-1$
			}
		});
		this.commentMainComposite.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent evt) {
				if (FileCommentWindow.log.isLoggable(java.util.logging.Level.FINER)) FileCommentWindow.log.log(java.util.logging.Level.FINER, "commentMainComposite.helpRequested " + evt); //$NON-NLS-1$
				DataExplorer.getInstance().openHelpDialog("", "HelpInfo_92.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		this.commentMainComposite.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent evt) {
				if (FileCommentWindow.log.isLoggable(java.util.logging.Level.FINER)) FileCommentWindow.log.log(java.util.logging.Level.FINER, "commentMainComposite.focusLost() , event=" + evt); //$NON-NLS-1$
				setFileComment();
			}

			@Override
			public void focusGained(FocusEvent evt) {
				if (FileCommentWindow.log.isLoggable(java.util.logging.Level.FINER)) FileCommentWindow.log.log(java.util.logging.Level.FINER, "commentMainComposite.focusGained() , event=" + evt); //$NON-NLS-1$
			}
		});
		{
			this.infoLabel = new CLabel(this.commentMainComposite, SWT.LEFT);
			this.infoLabel.setText(Messages.getString(MessageIds.GDE_MSGT0240));
			this.infoLabel.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + 2, SWT.BOLD));
			this.infoLabel.setBackground(this.surroundingBackground);
			this.infoLabel.setMenu(this.popupmenu);
			this.infoLabel.setBounds(50, 10, 500, 26);
			this.infoLabel.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					if (FileCommentWindow.log.isLoggable(java.util.logging.Level.FINER)) FileCommentWindow.log.log(java.util.logging.Level.FINER, "infoLabel.paintControl " + evt); //$NON-NLS-1$
					FileCommentWindow.this.contextMenu.createMenu(FileCommentWindow.this.popupmenu, TabMenuType.SIMPLE);
				}
			});
		}
		{
			this.fileCommentText = new Text(this.commentMainComposite, SWT.WRAP | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
			this.fileCommentText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.fileCommentText.setBackground(this.innerAreaBackground);
			this.fileCommentText.setText(Messages.getString(MessageIds.GDE_MSGT0241));
			this.fileCommentText.setBounds(50, 40, 500, 100);
			this.fileCommentText.setText(this.channels.getActiveChannel() != null ? this.channels.getActiveChannel().getFileDescription() : GDE.STRING_EMPTY);
			this.fileCommentCaretPosition = this.fileCommentText.getText().length();
			this.fileCommentText.setMenu(this.popupmenu);
			this.fileCommentText.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					if (FileCommentWindow.log.isLoggable(java.util.logging.Level.FINER)) FileCommentWindow.log.log(java.util.logging.Level.FINER, "fileCommentText.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_92.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.fileCommentText.addVerifyListener(new VerifyListener() {
				@Override
				public void verifyText(VerifyEvent e) {
					if (FileCommentWindow.log.isLoggable(java.util.logging.Level.FINER)) FileCommentWindow.log.log(java.util.logging.Level.FINER, "fileCommentText.verifyText , event=" + e); //$NON-NLS-1$
					if (FileCommentWindow.this.isFocusGained) {
						e.doit = false;
						FileCommentWindow.this.isFocusGained = false;
					}
					else
						e.doit = true;
					//System.out.println("verify " + FileCommentWindow.this.fileCommentText.getCaretPosition());					
				}
			});
			this.fileCommentText.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (FileCommentWindow.log.isLoggable(java.util.logging.Level.FINER)) FileCommentWindow.log.log(java.util.logging.Level.FINER, "fileCommentText.keyPressed , event=" + e); //$NON-NLS-1$

					if (e.character == '\0' || e.keyCode == SWT.ESC) {
						//ignore when only Shift, Ctrl, etc or ESC was pressed
					}
					else if ((e.stateMask & SWT.MOD1) != 0 && ((char) e.keyCode) == 'a') {
						// select all text on Ctrl+a
						FileCommentWindow.this.fileCommentText.selectAll();
					}
					else {
						FileCommentWindow.this.isFileCommentChanged = true;
					}
				}
			});
			this.fileCommentText.addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent evt) {
					if (FileCommentWindow.log.isLoggable(java.util.logging.Level.FINER)) FileCommentWindow.log.log(java.util.logging.Level.FINER, "fileCommentText.focusLost() , event=" + evt); //$NON-NLS-1$
					setFileComment();
					FileCommentWindow.this.fileCommentCaretPosition = FileCommentWindow.this.fileCommentCaretPosition == 0 && FileCommentWindow.this.fileCommentText.getCaretPosition() == 0 ? FileCommentWindow.this.fileCommentText
							.getText().length() : FileCommentWindow.this.fileCommentText.getCaretPosition();
					//System.out.println("lost " + FileCommentWindow.this.fileCommentText.getCaretPosition() + " " + FileCommentWindow.this.fileCommentCaretPosition);
				}

				@Override
				public void focusGained(FocusEvent evt) {
					if (FileCommentWindow.log.isLoggable(java.util.logging.Level.FINER)) FileCommentWindow.log.log(java.util.logging.Level.FINER, "fileCommentText.focusGained() , event=" + evt); //$NON-NLS-1$
					FileCommentWindow.this.fileCommentText.setSelection(FileCommentWindow.this.fileCommentCaretPosition);
					//System.out.println("gained " + FileCommentWindow.this.fileCommentText.getCaretPosition());
					FileCommentWindow.this.isFocusGained = true;
				}
			});
		}
		{
			this.recordCommentTable = new Table(this.commentMainComposite, SWT.BORDER | SWT.FULL_SELECTION);

			this.recordCommentTableColumn1 = new TableColumn(this.recordCommentTable, SWT.LEFT);
			this.recordCommentTableColumn1.setWidth(250);
			this.recordCommentTableColumn1.setText(Messages.getString(MessageIds.GDE_MSGT0242));

			this.recordCommentTableColumn2 = new TableColumn(this.recordCommentTable, SWT.LEFT);
			this.recordCommentTableColumn2.setWidth(250);
			this.recordCommentTableColumn2.setText(Messages.getString(MessageIds.GDE_MSGT0243));

			this.recordCommentTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.recordCommentTable.setBounds(50, 200, 500, 100);
			//this.table.setControl(this.dataTable);
			this.recordCommentTable.setLinesVisible(true);
			this.recordCommentTable.setHeaderVisible(true);
			this.recordCommentTable.setBackground(this.innerAreaBackground);
			this.recordCommentTable.setMenu(this.popupmenu);
	    Listener paintListener = new Listener() {
	      public void handleEvent(Event event) {
	        switch (event.type) {
	        case SWT.MeasureItem: {
	          TableItem item = (TableItem) event.item;
	          String text = item.getText(event.index);
	          Point size = event.gc.textExtent(text);
	          event.width = size.x;
	          event.height = size.y;	     
	          break;
	        }
	        case SWT.PaintItem: {
	          TableItem item = (TableItem) event.item;
	          String text = item.getText(event.index);
	          Point size = event.gc.textExtent(text);
	          int offset2 = event.index == 0 ? Math.max(0, (event.height - size.y) / 2) : 0;
	          event.gc.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
	          event.gc.drawText(text, event.x, event.y + offset2, true);
	          break;
	        }
	        case SWT.EraseItem: {
	          event.detail &= ~SWT.FOREGROUND;
	          break;
	        }
	        case SWT.Resize: {
	        	recordCommentTable.redraw();
						break;
	        }
	        }
	      }
	    };
	    this.recordCommentTable.addListener(SWT.MeasureItem, paintListener);
	    this.recordCommentTable.addListener(SWT.PaintItem, paintListener);
	    this.recordCommentTable.addListener(SWT.EraseItem, paintListener);
	    this.recordCommentTable.addListener(SWT.Resize, paintListener);
	    
			// create a TableCursor to navigate around the table
			cursor = new TableCursor(this.recordCommentTable, SWT.NONE);
			// create an editor to edit the cell when the user hits "ENTER" while over a cell in the table
			editor = new ControlEditor(cursor);
			editor.grabHorizontal = true;
			editor.grabVertical = true;

			cursor.addSelectionListener(new SelectionAdapter() {
				// when the TableEditor is over a cell, select the corresponding row in the table
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileCommentWindow.this.recordCommentTable.setSelection(new TableItem[] { cursor.getRow() });
				}

				// when the user hits "ENTER" in the TableCursor, pop up a text editor so that 
				// they can change the text of the cell
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					if (cursor.getColumn() == 1) { //don't allow editing record set names 
						final Text text = new Text(cursor, SWT.MULTI);
						text.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						final TableItem row = cursor.getRow();
						text.setText(row.getText(1));
						text.addKeyListener(new KeyAdapter() {
							@Override
							public void keyPressed(KeyEvent evt) {
								// close the text editor and copy the data over 
								// when the user hits "ENTER"
								if (evt.character == SWT.CR) {
									cursor.getRow().setText(1, text.getText());
									if (application.getActiveChannel() != null && application.getActiveChannel().get(row.getText(0)) != null)
										application.getActiveChannel().get(row.getText(0)).setRecordSetDescription(text.getText());
									text.dispose();
								}
								// close the text editor when the user hits "ESC"
								if (evt.character == SWT.ESC) {
									text.dispose();
								}
							}
						});
						// close the text editor when the user tabs away
						text.addFocusListener(new FocusAdapter() {
							@Override
							public void focusLost(FocusEvent evt) {
									try {
										cursor.getRow().setText(1, text.getText());
										if (application.getActiveChannel() != null && application.getActiveChannel().get(row.getText(0)) != null)
											application.getActiveChannel().get(row.getText(0)).setRecordSetDescription(text.getText());
									}
									catch (Exception e) {
										log.log(Level.WARNING, e.getMessage());
									}
								text.dispose();
							}
						});
						editor.setEditor(text);
						text.setFocus();
					}
				}
			});
			// Hide the TableCursor when the user hits the "CTRL" or "SHIFT" key.
			// This allows the user to select multiple items in the table.
			//			cursor.addKeyListener(new KeyAdapter() {
			//				@Override
			//				public void keyPressed(KeyEvent e) {
			//					if (e.keyCode == SWT.CTRL || e.keyCode == SWT.SHIFT || (e.stateMask & SWT.CONTROL) != 0 || (e.stateMask & SWT.SHIFT) != 0) {
			//						cursor.setVisible(false);
			//					}
			//				}
			//			});
			// When the user double clicks in the TableCursor, pop up a text editor so that 
			// they can change the text of the cell.
			cursor.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent e) {
					if (cursor.getColumn() == 1) { //don't allow editing record set names 
						final Text text = new Text(cursor, SWT.MULTI);
						text.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						final TableItem row = cursor.getRow();
						text.setText(row.getText(1));
						text.addKeyListener(new KeyAdapter() {
							@Override
							public void keyPressed(KeyEvent evt) {
								// close the text editor and copy the data over 
								// when the user hits "ENTER"
								if (evt.character == SWT.CR) {
									cursor.getRow().setText(1, text.getText());
									if (application.getActiveChannel() != null && application.getActiveChannel().get(row.getText(0)) != null)
										application.getActiveChannel().get(row.getText(0)).setRecordSetDescription(text.getText());
									text.dispose();
								}
								// close the text editor when the user hits "ESC"
								if (evt.character == SWT.ESC) {
									text.dispose();
								}
							}
						});
						// close the text editor when the user clicks away
						text.addFocusListener(new FocusAdapter() {
							@Override
							public void focusLost(FocusEvent evt) {
									try {
										cursor.getRow().setText(1, text.getText());
									}
									catch (Exception e2) {
										log.log(Level.WARNING, e2.getMessage());
									}
								text.dispose();
							}
						});
						editor.setEditor(text);
						text.setFocus();
					}
				}
			});

			// Show the TableCursor when the user releases the "SHIFT" or "CTRL" key.
			// This signals the end of the multiple selection task.
			this.recordCommentTable.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					if (e.keyCode == SWT.CONTROL && (e.stateMask & SWT.SHIFT) != 0) return;
					if (e.keyCode == SWT.SHIFT && (e.stateMask & SWT.CONTROL) != 0) return;
					if (e.keyCode != SWT.CONTROL && (e.stateMask & SWT.CONTROL) != 0) return;
					if (e.keyCode != SWT.SHIFT && (e.stateMask & SWT.SHIFT) != 0) return;

					TableItem[] selection = FileCommentWindow.this.recordCommentTable.getSelection();
					TableItem row = (selection.length == 0) ? FileCommentWindow.this.recordCommentTable.getItem(FileCommentWindow.this.recordCommentTable.getTopIndex()) : selection[0];
					FileCommentWindow.this.recordCommentTable.showItem(row);
					cursor.setSelection(row, cursor.getColumn());
					cursor.setVisible(true);
					cursor.setFocus();
				}
			});
			this.recordCommentTable.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					if (FileCommentWindow.log.isLoggable(java.util.logging.Level.FINER)) FileCommentWindow.log.log(java.util.logging.Level.FINER, "recordCommentTable.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_92.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
		}
	}

	public synchronized void update() {
		this.fileCommentText.redraw();
		updateRecordSetTable();
	}

	/**
	 * update the record set entry table
	 */
	synchronized void updateRecordSetTable() {
		Point mainSize = FileCommentWindow.this.getParent().getSize();
		//if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "mainSize = " + mainSize.toString());
		Rectangle bounds = new Rectangle(mainSize.x * 5 / 100, mainSize.y * 10 / 100, mainSize.x * 90 / 100, mainSize.y * 40 / 100);
		//if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "cover bounds = " + bounds.toString());
		FileCommentWindow.this.infoLabel.setBounds(50, 10, bounds.width, bounds.y - 10);
		FileCommentWindow.this.fileCommentText.setBounds(bounds);
		if (this.channels.getActiveChannel() != null) {
			this.fileCommentText.setText(this.channels.getActiveChannel().getFileDescription());
		}

		bounds = new Rectangle(mainSize.x * 5 / 100, mainSize.y * 50 / 100, mainSize.x * 90 / 100, mainSize.y * 40 / 100);
		FileCommentWindow.this.recordCommentTable.removeAll();
		FileCommentWindow.this.recordCommentTable.setBounds(bounds);
		FileCommentWindow.this.recordCommentTableColumn2.setWidth(bounds.width - 255);
		Channel channel = Channels.getInstance().getActiveChannel();
		if (channel != null) {
			synchronized (channel) {
				HashMap<String, RecordSet> recordSets = channel.getRecordSets();
				if (recordSets.size() > 0) {
					isTableUptaded = false;
					for (String recordSetKey : channel.getRecordSetNames()) {
						if (recordSetKey != null) {
							TableItem item = new TableItem(FileCommentWindow.this.recordCommentTable, SWT.LEFT);
							RecordSet tmpRecordSet = recordSets.get(recordSetKey);
							if (tmpRecordSet != null) {
								item.setText(new String[] { recordSetKey, tmpRecordSet.getRecordSetDescription() });
							}
						}
					}
//					this.recordCommentTable.setItemCount(channel.getRecordSetNames().length);
//					this.updateTopIndex();
				}
				else { //device switch detected
					if (!isTableUptaded) {
						this.create();
						isTableUptaded = true;
					}
				}
			}
		}
		FileCommentWindow.this.recordCommentTable.redraw();
		this.isRecordCommentChanged = false;
	}


	/**
	 * reset the top index of the data table if required while updating or adding rows
	 */
	public void updateTopIndex() {
		int height = this.recordCommentTable.getClientArea().height;
		int visibleItemCount = height / this.recordCommentTable.getItemHeight() - 1;
		int topIndex = this.recordCommentTable.getTopIndex();
		int itemCount = this.recordCommentTable.getItemCount();
		if (itemCount > visibleItemCount) {
			int newTopIndex = itemCount - visibleItemCount;
			if (topIndex != newTopIndex) 
				this.recordCommentTable.setTopIndex(newTopIndex);
		}
	}

	/**
	 * @return the isFileCommentChanged
	 */
	public boolean isFileCommentChanged() {
		return this.isFileCommentChanged;
	}

	public void setFileComment() {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null && this.isFileCommentChanged) {
			String updateFileDescription = this.fileCommentText.getText();
			activeChannel.setFileDescription(updateFileDescription);
			if (activeChannel.getActiveRecordSet() != null && activeChannel.getActiveRecordSet().getDevice().useChannelWithSyncedDescription())
				activeChannel.syncFileDescription(activeChannel, updateFileDescription);
			this.isFileCommentChanged = false;
		}
	}

	/**
	 * create visible tab window content as image
	 * @return image with content
	 */
	public Image getContentAsImage() {
		Rectangle bounds = this.commentMainComposite.getClientArea();
		Image tabContentImage = new Image(GDE.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.commentMainComposite.print(imageGC);
		if (GDE.IS_MAC)
			this.commentMainComposite.print(imageGC);
		imageGC.dispose();

		return tabContentImage;
	}

	/**
	 * @param newInnerAreaBackground the innerAreaBackground to set
	 */
	public void setInnerAreaBackground(Color newInnerAreaBackground) {
		this.innerAreaBackground = newInnerAreaBackground;
		this.fileCommentText.setBackground(newInnerAreaBackground);
		this.recordCommentTable.setBackground(newInnerAreaBackground);
		this.fileCommentText.redraw();
		this.recordCommentTable.redraw();
	}

	/**
	 * @param newSurroundingBackground the surroundingAreaBackground to set
	 */
	public void setSurroundingAreaBackground(Color newSurroundingBackground) {
		this.commentMainComposite.setBackground(newSurroundingBackground);
		this.infoLabel.setBackground(newSurroundingBackground);
		this.surroundingBackground = newSurroundingBackground;
		this.commentMainComposite.redraw();
	}
}